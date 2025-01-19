package org.hyperledger.fabric.samples.assettransfer;

public enum PolicyType {
    TIME(Type.TIME),
    VALUE(Type.VALUE),
    TIMEVALUE(Type.TIMEVALUE);

    public class Type {
        public static final String TIME = "TIME";
        public static final String VALUE = "VALUE";
        public static final String TIMEVALUE = "TIMEVALUE";
    }

    private final String label;

    private PolicyType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }

}
