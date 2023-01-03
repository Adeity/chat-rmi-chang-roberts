package cz.ctu.fee.dsv.semework;

import cz.ctu.fee.dsv.semework.base.Address;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.RemoteException;

// spusti vlakno, ktere ceka navstupu a parsuje to, co po nem chci
public class ConsoleHandler implements Runnable {

    private boolean reading = true;
    private BufferedReader reader = null;
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private Node myNode;


    public ConsoleHandler(Node myNode) {
        this.myNode = myNode;
        reader = new BufferedReader(new InputStreamReader(System.in));
    }


    private void parse_commandline(String commandline) throws IOException {
        String[] split = commandline.split(" ");
        if (split[0].equals("hn")) {
            myNode.sendHelloToNext();
        } else if (split[0].equals("hp")) {
            myNode.sendHelloToPrevious();
        } else if (split[0].equals("j")) {
            String hostname = split[1];
            int port = Integer.parseInt(split[2]);
            myNode.tryToJoinTopology(new Address(hostname, port));
        } else if (split[0].equals("s")) {
            myNode.printStatus();
        } else if (split[0].equals("sm")) {
            String to = split[1];
            String message;
            try {
                message = reader.readLine();
                CurrentTimeLogger.printTimeWithMessage("[to: " + to + "] " + message);
                myNode.sendMessageThroughLeader(to, myNode.getNickname(), message);
            } catch (IOException e) {
                System.err.println(e);
            }
        } else if (split[0].equals("le")) {
            int msDelay = 0;
            try {
                msDelay = Integer.parseInt(split[1]);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("delay is 0");
            }
            try {
                Thread.sleep(msDelay);
            } catch (InterruptedException e) {
                System.err.println(e);
            }
            try {
                myNode.initiateElection();
            } catch (RemoteException e) {
                System.err.println("initiate election bummer");
            }
        } else if (split[0].equals("?")) {
            CurrentTimeLogger.printTimeWithMessage("? - this help");
            CurrentTimeLogger.printTimeWithMessage("hn - send Hello message to Next neighbour");
            CurrentTimeLogger.printTimeWithMessage("hp - send Hello message to Previous neighbour");
            CurrentTimeLogger.printTimeWithMessage("j - send Join request");
            CurrentTimeLogger.printTimeWithMessage("s - print node status");
            CurrentTimeLogger.printTimeWithMessage("sm [to] ... pak message");
            CurrentTimeLogger.printTimeWithMessage("le - initiate leader election");
        } else {
            // do nothing
            System.out.print("Unrecognized command.");
        }
    }


    // dokud ma cist, tak vypise command line ac eka
    @Override
    public void run() {
        String commandline = "";
        while (reading == true) {
            commandline = "";
            System.out.print("\ncmd > ");
            try {
                commandline = reader.readLine();
                parse_commandline(commandline);
            } catch (IOException e) {
                err.println("ConsoleHandler - error in rading console input.");
                e.printStackTrace();
                reading = false;
            }
        }
        CurrentTimeLogger.printTimeWithMessage("Closing ConsoleHandler.");
    }
}