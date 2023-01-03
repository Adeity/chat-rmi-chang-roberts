package cz.ctu.fee.dsv.semework.base;

public enum PassTo {
    NEXT, PREVIOUS;

    public static PassTo getOppossite(PassTo passTo) {
        if (passTo == PassTo.NEXT) {
            return PassTo.PREVIOUS;
        }
         return PassTo.NEXT;
    }
}
