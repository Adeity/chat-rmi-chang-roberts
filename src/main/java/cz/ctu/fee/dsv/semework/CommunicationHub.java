package cz.ctu.fee.dsv.semework;

import cz.ctu.fee.dsv.semework.base.Address;
import cz.ctu.fee.dsv.semework.base.NodeCommands;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// vyuzivam pro komunikaci s ostatnimi nody.
// classa, kterou nejak nastavim. dam ji informaci o mem nodu (classa Node)

// je dobre mit mechanismus, ktery dokaze zpozdit prijmuti nebo odeslani zpravy
public class CommunicationHub {
    private Node node;

    public CommunicationHub (Node node) {
        this.node = node;
    }

    // dostanu objekt, ktery reprezentuje komunikacni element toho nextu
    public NodeCommands getNextNode() throws RemoteException {
        return getRMIProxy(node.getNeighbours().next);
    }


    public NodeCommands getNextNextNode() throws RemoteException {
        return getRMIProxy(node.getNeighbours().nextNext);
    }


    public NodeCommands getPrevNode() throws RemoteException {
        return getRMIProxy(node.getNeighbours().prev);
    }


    public NodeCommands getLeader() throws RemoteException {
        return getRMIProxy(node.getNeighbours().leader);
    }


    // pripoji se na rmi registry daneho uzlu a vytahne z nej objekt
    public NodeCommands getRMIProxy(Address address) throws RemoteException {
        if (address.compareTo(node.getAddress()) == 0 ) return node.getMessageReceiver();
        else {
            try {
                Registry registry = LocateRegistry.getRegistry(address.hostname, address.port);
                return (NodeCommands) registry.lookup(Node.COMM_INTERFACE_NAME);
            } catch (NotBoundException nbe) {
                // transitive RM exception
                throw new RemoteException(nbe.getMessage());
            }
        }
    }
}