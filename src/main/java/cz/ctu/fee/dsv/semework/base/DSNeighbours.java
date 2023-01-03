package cz.ctu.fee.dsv.semework.base;

import java.io.Serializable;

// stara se o udrzovani toho, co node vi o celem systemu
// drzi stav a umi se to vypsat
public class DSNeighbours implements Serializable {
    public Address next;
    public Address nextNext;
    public Address prev;
    public Address prevPrev;
    public Address leader;
    public MissingNodeCounter missingNodeCounter;

    public DSNeighbours (Address me) {
        this.next = me;
        this.nextNext = me;
        this.prev = me;
        this.leader = null;
        this.prevPrev = me;
    }

    public DSNeighbours (Address next, Address nextNext, Address prev, Address prevPrev, Address leader) {
        this.next = next;
        this.nextNext = nextNext;
        this.prev = prev;
        this.prevPrev = prevPrev;
        this.leader = null;
    }

    @Override
    public String toString() {
        return "DSNeighbours{\n" +
                "next=" + next + "\n" +
                ", nextNext=" + nextNext +"\n" +
                ", prev=" + prev +"\n" +
                ", prevPrev=" + prevPrev +"\n" +
                ", leader=" + leader +"\n" +
                '}';
    }

    public Address getNext() {
        return next;
    }

    public void setNext(Address next) {
        this.next = next;
    }

    public Address getNextNext() {
        return nextNext;
    }

    public void setNextNext(Address nextNext) {
        this.nextNext = nextNext;
    }

    public Address getPrev() {
        return prev;
    }

    public void setPrev(Address prev) {
        this.prev = prev;
    }

    public Address getPrevPrev() {
        return prevPrev;
    }

    public void setPrevPrev(Address prevPrev) {
        this.prevPrev = prevPrev;
    }

    public Address getLeader() {
        return leader;
    }

    public void setLeader(Address leader) {
        this.leader = leader;
    }

    public MissingNodeCounter getMissingNodeCounter() {
        return missingNodeCounter;
    }

    public void setMissingNodeCounter(MissingNodeCounter missingNodeCounter) {
        this.missingNodeCounter = missingNodeCounter;
    }
}