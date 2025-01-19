package org.hyperledger.fabric.samples.assettransfer;

import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Set;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;


@DataType()
public class Group { 
    
    @Property()
    private final String ID;

    @Property()
    private final String name;

    @Property()
    private final String location;

    @Property()
    private Policy policy;

    @Property()
    private Boolean voteEnded; // questo potrebbe essere ridondante, controllare se viene usato male

    @Property()
    private final Map<String, Role> members; 

    @Property()
    private final List<String> joining_requests;  

    @Property()
    private Map<Policy, Integer> proposed_policies; // proposed_policies (and votes)

    @Property()
    private LocalTime group_timestamp; // il timestamp da quando il gruppo è attivo/inizia la fase di proposal proposal_started
    //non è così in realtà perchè questo timestamp è modificato nel tempo 

    @Property()
    private LocalTime warning_reset_timestamp; 

    @Property()
    private GroupState state; // lo stato del gruppo 

    @Property()
    private final Integer proposal_duration; // la durata della fase di proposal  

    @Property()
    private final Integer voting_duration; // la durata della fase di voting 

    @Property()
    private Map<String, Map<String, List<String>>> warnings;   // le violazioni sono <ID_Utente, <ID_Regola, Numero Violazioni alla regola>>  
                                                          // potrebbe essere <ID_Utente, <ID_Regola, List<ID_misurazioni>>>  
                                                          // le lista sarà ordinata, quindi è facile poi controllare se ci sono misurazioni "violanti" troppo vecchie e possibilmente eliminabili


    public Group(
        @JsonProperty("ID") final String ID, 
        @JsonProperty("name") final String name,
        @JsonProperty("location") final String location, 
        @JsonProperty("proposal_duration") final Integer proposal_duration, 
        @JsonProperty("voting_duration") final Integer voting_duration) {
        this.state = GroupState.NEW;
        this.ID = ID;
        this.name = name;
        this.location = location;
        this.proposal_duration = proposal_duration;
        this.voting_duration = voting_duration;

        this.voteEnded = false;

        this.policy = new Policy();

        this.warnings = new HashMap<>();
        this.members = new HashMap<String, Role>();
        this.joining_requests = new ArrayList<String>();
        this.proposed_policies =  new HashMap<Policy, Integer>();  

    }

     public String getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Policy getPolicy() {
        return policy;
    }

    public Boolean getVoteEnded() {
        return voteEnded;
    }

    public Map<String, Role> getMembers() {
        return members;
    }

    public List<String> getJoining_requests() {
        return joining_requests;
    }

    public Map<Policy, Integer> getProposed_policies() {
        return proposed_policies;
    }

    public LocalTime getGroup_timestamp() {
        return group_timestamp;
    }

    public GroupState getState() {
        return state;
    }

    public Integer getProposal_duration() {
        return proposal_duration;
    }

    public Integer getVoting_duration() {
        return voting_duration;
    }

    public LocalTime getWarning_reset_timestamp(){
        return warning_reset_timestamp;
    }

    public Map<String, Map<String, List<String>>> getWarnings() {
        return warnings;
    }


    // Method to get the integer value for a given string

    public Map<String, List<String>> getUsersWarnings(String userID) {
        if (warnings.containsKey(userID)) {
            return warnings.get(userID);
        } else {
            throw new IllegalArgumentException("Key not found: " + userID);
        }
    }

    // Method to get the integer value for a given string
    public void setUsersWarnings(String userID, String rule, String measureID) {
        if (warnings.containsKey(userID)) {
            if (warnings.get(userID).containsKey(rule)){
                List <String> rule_warning = warnings.get(userID).get(rule);
                rule_warning.add(measureID);
                warnings.get(userID).put(rule, rule_warning);
            }
            else{
                throw new IllegalArgumentException("Rule not found: " + rule + " in user " + userID);
            }
        } else {
            throw new IllegalArgumentException("Key not found: " + userID);
        }
    }

    //resetWarnings
    public void resetWarnings(){
        List<String> id_rules = this.getPolicy().getRulesIDs();
        Map<String, Role> users = this.getMembers();

        for(String userID: users.keySet()){
            for (String ruleID: id_rules) {
                warnings.get(userID).put(ruleID, new ArrayList<String>()); 
            }
        }
    }


    public void addUser(String user, Role role) {
        members.put(user, role);
        warnings.put(user, new HashMap<String, List<String>>());
    }

    
    public void setTimer(LocalTime timer) {
        this.group_timestamp = timer;
    }

    public void setState(GroupState state) {
        this.state = state;
    }


    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public void setWarning_reset_timestamp(LocalTime time){
        this.warning_reset_timestamp = time;
    }

    
    public String getAdmin() {
        for (Map.Entry<String, Role> entry : members.entrySet()) {
            if (entry.getValue().equals(Role.ADMIN)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void addJoiningRequest(String userID) {
        joining_requests.add(userID);
    }


    public void removeJoiningRequest(String userID) {
        joining_requests.remove(userID);
    }


    //si potrebbe fare un controllo e vedere se esiste già una policy uguale a quella proposta e deve restituire "POLICY GIA' PROPOSTA"

    public void addProposals(List<Policy> policies) {
        for (Policy policy : policies) {
           this.addProposal(policy);
        }
    }

    public void addProposal(Policy policy) {
        Set<Policy> policies = getProposed_policies().keySet();
        for (Policy p : policies){
            if (p.getID().equals(policy.getID())){
                throw new IllegalArgumentException("The policy ID was already assigned!");
            }
            if (p.hashCode() == policy.hashCode()) {
                if (p.equals(policy)) {
                    throw new IllegalArgumentException("The policy already exists!");
                } 
            } 
        }
        if(!proposed_policies.containsKey(policy)){
            proposed_policies.put(policy, 0);
            System.out.println("Policy " + policy.getID() + " added to the vote");
        }
    }

    public String endPolicyVote() {
        this.voteEnded = true;
        String mostVotedPolicyId = null;
        int maxVotes = 0;

        for (Map.Entry<Policy, Integer> entry : proposed_policies.entrySet()) {
            if (entry.getValue() > maxVotes) {
                mostVotedPolicyId = entry.getKey().getID();
                maxVotes = entry.getValue();
            }
        }

        //proposed_policies.clear();
        LocalTime time = LocalTime.now();
        this.group_timestamp = time.withNano(0);

        return mostVotedPolicyId;

    }

    //Reset Measurements, Violations and Timer
    public void resetMVT() { 
        LocalTime time = LocalTime.now();
        this.group_timestamp = time.withNano(0);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Group other = (Group) obj;
        return Objects.deepEquals(new Object[] {getID(), getName(), getLocation(), getPolicy(), getVoteEnded(), getMembers(), getJoining_requests(), getProposed_policies(), getGroup_timestamp(), getState(), getProposal_duration(), getVoting_duration(), getWarnings(), getWarning_reset_timestamp()},
        new Object[] {other.getID(), other.getName(), other.getLocation(), other.getPolicy(), other.getVoteEnded(), other.getMembers(), other.getJoining_requests(), other.getProposed_policies(), other.getGroup_timestamp(), other.getState(), other.getProposal_duration(), other.getVoting_duration(), other.getWarnings(), this.getWarning_reset_timestamp()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), getName(), getLocation(), getPolicy(), getVoteEnded(), getMembers(), getJoining_requests(), getProposed_policies(), getGroup_timestamp(), getState(), getProposal_duration(), getVoting_duration(), getWarnings(), getWarning_reset_timestamp());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Group{");
        sb.append("ID=").append(ID);
        sb.append(", name=").append(name);
        sb.append(", location=").append(location);
        sb.append(", policy=").append(policy);
        sb.append(", voteEnded=").append(voteEnded);
        sb.append(", members=").append(members);
        sb.append(", joining_requests=").append(joining_requests);
        sb.append(", proposed_policies=").append(proposed_policies);
        sb.append(", group_timestamp=").append(group_timestamp);
        sb.append(", state=").append(state);
        sb.append(", proposal_duration=").append(proposal_duration);
        sb.append(", voting_duration=").append(voting_duration);
        sb.append(", warnings=").append(warnings);
        sb.append(", warning_reset_timestamp=").append(warning_reset_timestamp);
        sb.append('}');
        return sb.toString();
    }

   

}
