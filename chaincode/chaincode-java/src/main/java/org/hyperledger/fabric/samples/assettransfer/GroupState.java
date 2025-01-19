package org.hyperledger.fabric.samples.assettransfer;

public enum GroupState {
    NEW(State.NEW),
    VOTEOPEN(State.VOTEOPEN),
    MONITORING(State.MONITORING),
    POLICYPROPOSAL(State.POLICYPROPOSAL);

    public class State {
        public static final String NEW = "New";
        public static final String VOTEOPEN = "VoteOpen";
        public static final String MONITORING = "Monitoring";
        public static final String POLICYPROPOSAL = "PolicyProposal";
    }

    private final String label;

    private GroupState(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
    
}
