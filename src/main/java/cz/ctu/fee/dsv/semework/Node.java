package cz.ctu.fee.dsv.semework;

import cz.ctu.fee.dsv.semework.base.Address;
import cz.ctu.fee.dsv.semework.base.DSNeighbours;
import cz.ctu.fee.dsv.semework.base.ElectionStateEnum;
import cz.ctu.fee.dsv.semework.base.NodeCommands;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Node implements Runnable {
    // Using logger is strongly recommended (log4j, ...)

    // Name of our RMI "service"
    // kazdy node ma u sebe RMI registry. Adresa nodu je adresa RMI registry, ve kterem budu hledat objekt DSVNode
    //
    public static final String COMM_INTERFACE_NAME = "DSVNode";

    // This Node
    // instance toho nodu
    public static Node thisNode = null;

    // Initial configuration from commandline
    // vychozi hodnoty
    private String nickname = "Unknown";
    private String myIP = "127.0.0.1";
    private int myPort = 2010;

    // Node Id
    // nastaveni objektu, ktere budu pouzivat.
    private long nodeId = 0;
    private Address myAddress;
    private DSNeighbours neighbours;
    private NodeCommands myMessageReceiver;
    private CommunicationHub communicationHub;
    private ConsoleHandler myConsoleHandler;

    // opravuju topologii, nebo ne
    boolean repairInProgress = false;
    // probiha leader election, nebo ne
    boolean ongoingElection = false;
    ElectionStateEnum electionState = ElectionStateEnum.NOT_INVOLVED;
    boolean responseOk = true;
    int numberOfResponses = 0;
    int iMax = 1;
    int i = 0;

    public Map<String, Address> nicknames;


    // nastaveni meho objektu
    // otvira se z prikazove radky
    public Node (String[] args) {
        // handle commandline arguments
        //prvni uzel
        if (args.length == 3) {
            nickname = args[0];
            myIP = args[1];
            myPort = Integer.parseInt(args[2]);
        } else {
            // something is wrong - use default values
            System.err.println("Wrong number of commandline parameters - using default values.");
        }
    }


    // funcke na vytvoreni idcka
    private long generateId(String address, int port) {
        // generates  <port><IPv4_dec1><IPv4_dec2><IPv4_dec3><IPv4_dec4>
        String[] array = myIP.split("\\.");
        long id = 0;
        long shift = 0, temp = 0;
        for(int i = 0 ; i < array.length; i++){
            temp = Long.parseLong(array[i]);
            id = (long) (id * 1000);
            id += temp;
        }
        if (id == 0) {
            // TODO problem with parsing address - handle it
            id = 666000666000l;
        }
        id = id + port*1000000000000l;
        return id;
    }


    private NodeCommands startMessageReceiver() {
        System.setProperty("java.rmi.server.hostname", myAddress.hostname);

        NodeCommands msgReceiver = null;
        try {
            msgReceiver = new MessageReceiver(this);

            // Create instance of remote object and its skeleton
            NodeCommands skeleton = (NodeCommands) UnicastRemoteObject.exportObject(msgReceiver, 40000+myAddress.port);

            // Create registry and (re)register object name and skeleton in it
            Registry registry = LocateRegistry.createRegistry(myAddress.port);
            registry.rebind(COMM_INTERFACE_NAME, skeleton);
        } catch (Exception e) {
            // Something is wrong ...
            System.err.println("Message listener - something is wrong: " + e.getMessage());
        }
        CurrentTimeLogger.printTimeWithMessage("Message listener is started ...");

        return msgReceiver;
    }


    @Override
    public String toString() {
        return "Node[id:'"+nodeId+"', \n" +
                "nick:'"+nickname+"', \n" +
                "myIP:'"+myIP+"', \n" +
                "myPort:'"+myPort+"', \n" +
                "']";
    }


    public void printStatus() {
        CurrentTimeLogger.printTimeWithMessage("Status: " + this + "\n");
        CurrentTimeLogger.printTimeWithMessage("  With neighbours " + neighbours);
    }

    public void initiateElection() throws RemoteException{
        myMessageReceiver.initiateElection();
    }


    @Override
    public void run() {
        nodeId = generateId(myIP, myPort);
        myAddress = new Address(myIP, myPort);
        neighbours = new DSNeighbours(myAddress);
        printStatus();
        myMessageReceiver = startMessageReceiver();     // TODO null -> exit
        communicationHub = new CommunicationHub(this);   // TODO null -> exit
        myConsoleHandler = new ConsoleHandler(this);

        new Thread(myConsoleHandler).run();
    }


    public static void main(String[] args) {
        thisNode = new Node(args);
        thisNode.run();
    }


    // co se ma stat, kdyz zprava umre
    public void repairTolopogy(Address missingNode) throws RemoteException{
        if (repairInProgress == false) {
            repairInProgress = true;
            {
                try {
                    myMessageReceiver.nodeMissing(missingNode);
                } catch (RemoteException e) {
                    // this should not happen
                    // muze se to stat, kdyz umre next next (myslimm)
                    e.printStackTrace();
                }
                CurrentTimeLogger.printTimeWithMessage("Topology was repaired " + neighbours);
            }
            repairInProgress = false;

            // test leader
            this.testLeader();

        }
    }

    public void testLeader() throws RemoteException{
        try {
            communicationHub.getLeader().hello();
            CurrentTimeLogger.printTimeWithMessage("There is no leader.");
        } catch (RemoteException | NullPointerException e) {
            CurrentTimeLogger.printTimeWithMessage("Leader is down or null. Initating election.");
            myMessageReceiver.initiateElection();
        }
    }

    public void sendMessageThroughLeader(String to, String from, String message) throws RemoteException{
        try {
            this.getCommunicationHub().getLeader().sendMessage(to, from, message);
        } catch (RemoteException e) {
            repairTolopogy(this.neighbours.getLeader());
        }
    }

    public void sendHelloToNext() throws RemoteException{
        CurrentTimeLogger.printTimeWithMessage("Sending Hello to my Next ...");
        try {
            communicationHub.getNextNode().hello();
        } catch (RemoteException e) {
            repairTolopogy(neighbours.next);
        }
    }

    public void sendHelloToPrevious() throws RemoteException{
        CurrentTimeLogger.printTimeWithMessage("Sending Hello to my Previous ...");
        try {
            communicationHub.getPrevNode().hello();
        } catch (RemoteException e) {
            repairTolopogy(neighbours.prev);
        }
    }

    public void tryToJoinTopology(Address address) {
        CurrentTimeLogger.printTimeWithMessage("Trying to join topology.");
        try {
            NodeCommands s = communicationHub.getRMIProxy(address);
            DSNeighbours neighbours = s.join(this.getAddress());
            this.setNeighbours(neighbours);
            CurrentTimeLogger.printTimeWithMessage("Joined topology");
        } catch (RemoteException e) {
            System.err.println("Something went wrong!");
        }
    }


    public Address getAddress() {
        return myAddress;
    }


    public DSNeighbours getNeighbours() {
        return neighbours;
    }

    public void setNeighbours(DSNeighbours neighbours) {
        this.neighbours = neighbours;
    }


    public NodeCommands getMessageReceiver() {
        return myMessageReceiver;
    }


    public CommunicationHub getCommunicationHub() {
        return communicationHub;
    }


    public long getNodeId() {
        return nodeId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Map<String, Address> getNicknames() {
        if (nicknames == null) {
            nicknames = new HashMap<>();
        }
        return nicknames;
    }

    public void setNicknames(Map<String, Address> nicknames) {
        this.nicknames = nicknames;
    }

    public boolean isOngoingElection() {
        return ongoingElection;
    }

    public void setOngoingElection(boolean ongoingElection) {
        this.ongoingElection = ongoingElection;
    }
}
