package cz.ctu.fee.dsv.semework.base;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeCommands extends Remote {
        // mel by mit navratovou zpravu, ale protoze mame rmi tak vraci DSNeighbours strukturu.
        public DSNeighbours join(Address addr) throws RemoteException;
        // change n next
        public void changeNextNext(Address addr) throws RemoteException;
        // change n prev
        public Address changePrev(Address addr) throws RemoteException;
        public Address changeNext(Address addr) throws RemoteException;
        public void changePrevPrev(Address addr) throws RemoteException;
        public void nodeMissing(Address addr) throws RemoteException;
        public void initiateElection() throws RemoteException;
        public void candidature(long candidateId, int i, int iMax, PassTo passToDirection) throws RemoteException;
        public void response(boolean responseOk, long candidateId, PassTo passTo) throws RemoteException;
        public void elected(long id, Address leaderAddr) throws RemoteException;
        public void sendMessage(String toNickName, String fromNickName, String message) throws RemoteException;
        public void receiveMessage(String fromNickName, String message) throws RemoteException;
        public void register(String nickName, Address addr) throws RemoteException;
        // keep alive zalezitost, nepovinna
        public void hello() throws RemoteException;
}