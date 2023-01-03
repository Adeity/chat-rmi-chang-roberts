package cz.ctu.fee.dsv.semework.base;

public class MissingNodeCounter {
    private Address missingNodeAddress;
    private int missingNodeMessageCounter;

    public MissingNodeCounter(Address missingNodeAddress) {
        this.missingNodeAddress = missingNodeAddress;
        this.missingNodeMessageCounter = 1;
    }

    public void incementCounter() {
        this.missingNodeMessageCounter++;
    }

    public Address getMissingNodeAddress() {
        return missingNodeAddress;
    }

    public void setMissingNodeAddress(Address missingNodeAddress) {
        this.missingNodeAddress = missingNodeAddress;
    }

    public int getMissingNodeMessageCounter() {
        return missingNodeMessageCounter;
    }

    public void setMissingNodeMessageCounter(int missingNodeMessageCounter) {
        this.missingNodeMessageCounter = missingNodeMessageCounter;
    }
}
