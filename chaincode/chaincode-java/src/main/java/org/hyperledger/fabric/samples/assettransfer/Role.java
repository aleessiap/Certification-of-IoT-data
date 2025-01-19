package org.hyperledger.fabric.samples.assettransfer;

public enum Role {
    MEMBER(Type.MEMBER),
    ADMIN(Type.ADMIN);

    public class Type{
        public static final String MEMBER = "Member";
        public static final String ADMIN = "Admin";
    }

    private final String label;

    private Role(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
