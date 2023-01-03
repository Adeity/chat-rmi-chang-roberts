package cz.ctu.fee.dsv.semework;

import cz.ctu.fee.dsv.semework.base.*;

import java.rmi.RemoteException;

// neco co implementuje NodeCommands, tento objekt vystavuju ven. Tudy do me vstupuji zpravy od ostatnich uzlu, zprava = rpc goal/call?
public class MessageReceiver implements NodeCommands {
    private Node myNode = null;

    public MessageReceiver(Node node) {
        this.myNode = node;
    }

    //nekdo se pridava do topologie
    @Override
    public DSNeighbours join(Address addr) throws RemoteException {
        CurrentTimeLogger.printTimeWithMessage("JOIN was called ...");
        if (addr.compareTo(myNode.getAddress()) == 0) {
            CurrentTimeLogger.printTimeWithMessage("I am the first and leader");
            return myNode.getNeighbours();
        } else {
            //nastavim novackovi nastaveni
            CurrentTimeLogger.printTimeWithMessage("Someone is joining ...");
            DSNeighbours myNeighbours = myNode.getNeighbours();
            Address myInitialNext = new Address(myNeighbours.next);     // because of 2 nodes config
            Address myInitialPrev = new Address(myNeighbours.prev);     // because of 2 nodes config

            boolean toBeTwoInTopology = (myNeighbours.prev.compareTo(this.myNode.getAddress())  == 0)
                    && myNeighbours.next.compareTo(this.myNode.getAddress()) == 0;
            Address prevForNewGuy = myNode.getAddress();
            Address prevPrevForNewGuy = toBeTwoInTopology ? addr : myNeighbours.prev;
            Address nextForNewGuy = myNeighbours.next;
            Address nextNextForNewGuy = toBeTwoInTopology ? addr : myNeighbours.nextNext;

            DSNeighbours newNodeNeighbours = new DSNeighbours(nextForNewGuy,
                                                            nextNextForNewGuy,
                                                            prevForNewGuy,
                                                            prevPrevForNewGuy,
                                                            myNeighbours.leader);
            //zbytku topologie zmenim info o topologii
            // to my next send msg ChPrev to addr
            myNode.getCommunicationHub().getNextNode().changePrev(addr);
            myNode.getCommunicationHub().getNextNode().changePrevPrev(prevForNewGuy);
            // to my prev send msg ChNNext addr
            myNode.getCommunicationHub().getPrevNode().changeNextNext(addr);

            // handle myself
            myNeighbours.setNextNext(nextForNewGuy);
            myNeighbours.setNext(addr);
            if (toBeTwoInTopology) {
                CurrentTimeLogger.printTimeWithMessage("To be two in topology");
                myNeighbours.setPrev(addr);
                myNeighbours.setPrevPrev(myNode.getAddress());
            }
//            myNeighbours.nextNext = myInitialNext;
//            myNeighbours.next = addr;
            return newNodeNeighbours;
        }
    }

    public void initiateElection() throws RemoteException{
        CurrentTimeLogger.printTimeWithMessage("Initiating election, responseOk: " + myNode.responseOk + " election state: " + myNode.electionState + " number of responses: " + myNode.numberOfResponses);
        myNode.electionState = ElectionStateEnum.CANDIDATE;
        myNode.responseOk = true;
        myNode.iMax = 1;
        myNode.getNicknames().clear();
        myNode.setOngoingElection(true);
        electionCycle();
    }

    public void electionCycle() throws RemoteException{
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("interrputed");
        }
        CurrentTimeLogger.printTimeWithMessage("starting election cycle, i: " + myNode.i + ", imax: " + myNode.iMax);
        if (myNode.responseOk && myNode.electionState == ElectionStateEnum.CANDIDATE) {
            myNode.numberOfResponses = 0;
            try {
                myNode.getCommunicationHub().getNextNode().candidature(myNode.getNodeId(), 0, myNode.iMax, PassTo.NEXT);
            } catch (RemoteException e) {
                myNode.repairTolopogy(myNode.getNeighbours().getNext());
            }
            try {
                myNode.getCommunicationHub().getPrevNode().candidature(myNode.getNodeId(), 0, myNode.iMax, PassTo.PREVIOUS);
            } catch (RemoteException e) {
                myNode.repairTolopogy(myNode.getNeighbours().getPrev());
            }
        }
    }

    public void candidature(long candidateId, int i, int iMax, PassTo passTo) throws RemoteException{
        CurrentTimeLogger.printTimeWithMessage("Received candidature, candidateId: " + candidateId + " i: " + i + ", iMax: " + iMax + " passTo: " + passTo);
        myNode.setOngoingElection(true);
        long myId = myNode.getNodeId();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("interrputed");
        }
        if (candidateId < myId) {
            // respond response
            PassTo responsePassTo = PassTo.getOppossite(passTo);
            if (responsePassTo == PassTo.NEXT) {
                try {
                    myNode.getCommunicationHub().getNextNode().response(false, candidateId, responsePassTo);
                } catch (RemoteException e) {
                    myNode.repairTolopogy(myNode.getNeighbours().getNext());
                }
            } else if (responsePassTo == PassTo.PREVIOUS) {
                try {
                    myNode.getCommunicationHub().getPrevNode().response(false, candidateId, responsePassTo);
                } catch (RemoteException e) {
                    myNode.repairTolopogy(myNode.getNeighbours().getPrev());
                }
            }

            CurrentTimeLogger.printTimeWithMessage("I've sent response. My state is: " + myNode.electionState);
            if (myNode.electionState == ElectionStateEnum.NOT_INVOLVED) {
                myNode.iMax = 1;
                CurrentTimeLogger.printTimeWithMessage("This node has bigger id and is getting involved in the election");
                initiateElection();
            } else {
                CurrentTimeLogger.printTimeWithMessage("This node has bigger id and is already involved in election");
            }
        } else if (candidateId > myId) {
            myNode.electionState = ElectionStateEnum.LOST;
            i++;
            if (i < iMax) {
                CurrentTimeLogger.printTimeWithMessage("Candidate has bigger id. Passing candidature message.");
                if (passTo.equals(PassTo.NEXT)) {
                    try {
                        myNode.getCommunicationHub().getNextNode().candidature(candidateId, i, iMax, passTo);
                    } catch (RemoteException e) {
                        myNode.repairTolopogy(myNode.getNeighbours().getNext());
                    }
                } else if (passTo.equals(PassTo.PREVIOUS)) {
                    try {
                        myNode.getCommunicationHub().getPrevNode().candidature(candidateId, i, iMax, passTo);
                    } catch (RemoteException e) {
                        myNode.repairTolopogy(myNode.getNeighbours().getPrev());
                    }
                }
            } else {
                PassTo responsePassTo = PassTo.getOppossite(passTo);
                CurrentTimeLogger.printTimeWithMessage("Candidate has bigger id. Sending response back to the candidate.");
                if (responsePassTo == PassTo.NEXT) {
                    try {
                        myNode.getCommunicationHub().getNextNode().response(true, candidateId, responsePassTo);
                    } catch (RemoteException e) {
                        myNode.repairTolopogy(myNode.getNeighbours().getNext());
                    }
                } else if (responsePassTo == PassTo.PREVIOUS) {
                    try {
                        myNode.getCommunicationHub().getPrevNode().response(true, candidateId, responsePassTo);
                    } catch (RemoteException e) {
                        myNode.repairTolopogy(myNode.getNeighbours().getPrev());
                    }
                }
            }
        } else { // it's this node
            if (myNode.electionState != ElectionStateEnum.ELECTED) {
                CurrentTimeLogger.printTimeWithMessage("I am the candidate. I just got elected");
                myNode.electionState = ElectionStateEnum.ELECTED;
                myNode.getNeighbours().setLeader(myNode.getAddress());
                try {
                    myNode.getCommunicationHub().getNextNode().elected(myNode.getNodeId(), myNode.getAddress());
                } catch (RemoteException e) {
                    myNode.repairTolopogy(myNode.getNeighbours().getNext());
                    try {
                        myNode.getCommunicationHub().getNextNode().elected(myNode.getNodeId(), myNode.getAddress());
                    } catch (RemoteException e2) {
                        myNode.repairTolopogy(myNode.getNeighbours().getNext());
                    }
                }
            } else {
                CurrentTimeLogger.printTimeWithMessage("I am the candidate. Though I already was elected.");
            }
        }
    }

    public void response(boolean responseOk, long candidateId, PassTo passTo) throws RemoteException{
        if (!myNode.isOngoingElection()) {
            return;
        }
        if (myNode.getNodeId() == candidateId) {
            myNode.numberOfResponses++;
            myNode.responseOk = myNode.responseOk && responseOk;
            if (!myNode.responseOk) {
                CurrentTimeLogger.printTimeWithMessage("Received response. I am out of the race.");
                myNode.electionState = ElectionStateEnum.LOST;
            } else {
                CurrentTimeLogger.printTimeWithMessage("Received response. I am still candidating.");
            }
            if (myNode.numberOfResponses == 2) {
                myNode.i++;
                myNode.iMax *= 2;
                electionCycle();
            }
        } else {
            CurrentTimeLogger.printTimeWithMessage("Received response. Forwarding.");
            CurrentTimeLogger.printTimeWithMessage("Resonspe ok: " + responseOk + " candidateId: " + candidateId + " passTo: " + passTo);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("sleep interrupted");
            }
            if (passTo.equals(PassTo.NEXT)) {
                try {
                    myNode.getCommunicationHub().getNextNode().response(responseOk, candidateId, passTo);
                } catch (RemoteException e2) {
                    myNode.repairTolopogy(myNode.getNeighbours().getNext());
                }
            } else if (passTo.equals(PassTo.PREVIOUS)) {
                try {
                    myNode.getCommunicationHub().getPrevNode().response(responseOk, candidateId, passTo);
                } catch (RemoteException e2) {
                    myNode.repairTolopogy(myNode.getNeighbours().getPrev());
                }
            }
        }
    }

    // zkontroluje, jestli adresa, ktera chybi, je moje next/previous. jestli ne, tak to posila dal.
    @Override
    public void nodeMissing(Address addr) throws RemoteException {
        if (myNode.getNeighbours().getMissingNodeCounter() != null) {
            if (myNode.getNeighbours().getMissingNodeCounter().getMissingNodeAddress().compareTo(addr) == 0) {
                myNode.getNeighbours().getMissingNodeCounter().incementCounter();
                if (myNode.getNeighbours().getMissingNodeCounter().getMissingNodeMessageCounter() > 1) {
                    CurrentTimeLogger.printTimeWithMessage("Received another cycle of node missing with the same address. Throwing message away.");
                    if (myNode.getNeighbours().getLeader().compareTo(addr) == 0) {
                        CurrentTimeLogger.printTimeWithMessage("The missing node is my leader. Going to test leader.");
                        myNode.testLeader();
                    }
                }
            } else {
                myNode.getNeighbours().setMissingNodeCounter(new MissingNodeCounter(addr));
            }
        } else {
            myNode.getNeighbours().setMissingNodeCounter(new MissingNodeCounter(addr));
        }
        CurrentTimeLogger.printTimeWithMessage("NodeMissing was called with " + addr);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("interrputed sleep");
        }
        if (addr.compareTo(myNode.getNeighbours().next) == 0) {
            // chybi next
            myNode.getNeighbours().setNext(myNode.getNeighbours().getNextNext());
            myNode.getCommunicationHub().getNextNode().changePrevPrev(myNode.getNeighbours().getPrev());
            myNode.getNeighbours().setNextNext(myNode.getCommunicationHub().getNextNode().changePrev(myNode.getAddress())); // protoze navratova hodnota je tvuj next next
            myNode.getCommunicationHub().getPrevNode().changeNextNext(myNode.getNeighbours().getNext());
            CurrentTimeLogger.printTimeWithMessage("NodeMissing DONE");
//        } else if (addr.compareTo(myNode.getNeighbours().prev) == 0) {
//            // chybi previous
//            myNode.getNeighbours().setPrev(myNode.getNeighbours().getPrevPrev());
//            myNode.getCommunicationHub().getPrevNode().changeNextNext(myNode.getNeighbours().getNext());
//            myNode.getNeighbours().setPrevPrev(myNode.getCommunicationHub().getPrevNode().changeNext(myNode.getAddress())); // protoze navratova hodnota je tvuj next next
//            myNode.getCommunicationHub().getNextNode().changePrevPrev(myNode.getNeighbours().getPrev());
//            CurrentTimeLogger.printTimeWithMessage("NodeMissing DONE");
        } else {
            // send to next node
            myNode.getCommunicationHub().getNextNode().nodeMissing(addr);
        }
    }

    // zaloguju ze bylo volanu, nastavim si leadera. jestli ze to nejsem ja, tak posilam dal, jinak to konci.
    //
    @Override
    public void elected(long id, Address leaderAddr) throws RemoteException{
        CurrentTimeLogger.printTimeWithMessage("Elected was called with id " + id);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("sleep interrupted");
        }
        myNode.electionState = ElectionStateEnum.NOT_INVOLVED;
        myNode.iMax = 1;
        myNode.getNeighbours().leader = leaderAddr;
        if (myNode.getNodeId() != id) {
            try {
                myNode.getCommunicationHub().getLeader().register(myNode.getNickname(), myNode.getAddress());
            } catch (RemoteException e2) {
                myNode.repairTolopogy(myNode.getNeighbours().getLeader());
            }
            try {
                myNode.getCommunicationHub().getNextNode().elected(id, leaderAddr);
            } catch (RemoteException e2) {
                myNode.repairTolopogy(myNode.getNeighbours().getNext());
            }
        } else {
        }
    }

    @Override
    public void sendMessage(String toNickName, String fromNickName, String message) throws RemoteException {
        if (toNickName.equals(myNode.getNickname())) {
            this.receiveMessage(fromNickName, message);
            return;
        }
        Address address = myNode.getNicknames().get(toNickName);
        if (address == null) {
            sendMessageFailed(fromNickName, toNickName);
            return;
        }
        try {
            myNode.getCommunicationHub().getRMIProxy(address).receiveMessage(fromNickName, message);
        } catch (RemoteException e) {
            CurrentTimeLogger.printTimeWithMessage("Couldn't send message to " + toNickName);
            sendMessageFailed(fromNickName, toNickName);
            myNode.repairTolopogy(address);
        }
    }

    private void sendMessageFailed(String fromNickName, String toNickName) throws RemoteException {
        Address senderAddress = myNode.getNicknames().get(fromNickName);
        if (senderAddress != null) {
            myNode.getCommunicationHub().getRMIProxy(senderAddress).receiveMessage("admin", "Message to + " + toNickName + " could not be delivered.");
        }
        return;
    }

    @Override
    public void receiveMessage(String fromNickName, String message) throws RemoteException {
        CurrentTimeLogger.printTimeWithMessage("[from: "+fromNickName+"] " + message);
    }

    @Override
    public void register(String nickName, Address addr) throws RemoteException {
        myNode.getNicknames().put(nickName, addr);
    }


    @Override
    public void hello() throws RemoteException {
        CurrentTimeLogger.printTimeWithMessage("Hello was called ...");
    }


    @Override
    public Address changeNext(Address addr) throws RemoteException {
        myNode.setOngoingElection(false);
        myNode.getNeighbours().next = addr;
        myNode.getCommunicationHub().getPrevNode().changeNextNext(addr);
        return myNode.getNeighbours().prev; // vracim tomu nodu jeho prevPrev
    }

    @Override
    public void changeNextNext(Address addr) throws RemoteException {
        myNode.setOngoingElection(false);
        myNode.getNeighbours().nextNext = addr;
    }


    @Override
    public Address changePrev(Address addr) throws RemoteException {
        myNode.setOngoingElection(false);
        myNode.getNeighbours().prev = addr;
        myNode.getCommunicationHub().getNextNode().changePrevPrev(addr);
        return myNode.getNeighbours().next; // vracim tomu nodu jeho nextNext
    }

    @Override
    public void changePrevPrev(Address addr) throws RemoteException {
        myNode.setOngoingElection(false);
        myNode.getNeighbours().prevPrev = addr;
    }

}
