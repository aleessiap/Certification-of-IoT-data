package org.hyperledger.fabric.samples.assettransfer;

import java.util.Objects;
import java.util.HashMap;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class User {
    
    @Property()
    private final String userID;

    @Property()
    private final String name;

    @Property()
    private final String surname;

    @Property()
    private final String email;

    @Property()
    private Boolean hasVoted = false;

    @Property()
    private final List<String> subscription = new ArrayList<String>();

    @Property()
    private final List<Policy> policies = new ArrayList<Policy>();

    @Property()
    private List<String> devices = new ArrayList<String>();

    @Property()
    private Map<String, Measure> lastMeasure; // dovrebbe essere Map<Gruppo, Map<Variabile, Measure>> 

    public User(@JsonProperty("userID") final String ID, @JsonProperty("name") final String name,
    @JsonProperty("surname") final String surname, @JsonProperty("email") final String email) {
        this.userID = ID;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.lastMeasure = new HashMap<String, Measure>();
    }

    public String getID() {
        return userID;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public Boolean getHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(Boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public List<String> getSubscription() {
        return subscription;
    }

    public void addDevice(String device) {
        devices.add(device);
    }

    public boolean hasDevice(String device) {
        return devices.contains(device);
    }

    public void setPolicy(Group group, Policy policy) {
        //if Role.ADMIN
            group.setPolicy(policy);
    }

    public void acceptRequest(Group group, User user) {
        //if Role.ADMIN
            String userID = user.getID();
            if (group.getJoining_requests().contains(userID)) {
                group.addUser(userID, Role.MEMBER);
                group.removeJoiningRequest(userID);
                user.getSubscription().add(group.getID());
            }
    }

    
    //public Policy createPolicy(String ID, List<String> samplingIntervals, PolicyType policyType, List<String> valueNames, List<String> valueThresholds, List<String> operatorThresholds) {
    //    Policy policy = new Policy(ID, samplingIntervals, policyType, valueNames, valueThresholds, operatorThresholds);
    //    policies.add(policy);
    //    return policy;
    //}
    
    public Policy createPolicy(String ID,  PolicyType policyType, List<String> rules) {
        Policy policy = new Policy(ID, policyType, rules);
        policies.add(policy);
        return policy;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public Policy getPolicy(String ID) {
        for (Policy policy : policies) {
            if (policy.getID().equals(ID)) {
                return policy;
            }
        }
        return null;
    }

    public void vote(Group group, Policy policy) {
        Map<Policy, Integer> votes = group.getProposed_policies();
        this.hasVoted = true;

        if(null == group.getState()) {
            System.out.println("Vote phase ended");
        } else switch (group.getState()) {
            case POLICYPROPOSAL:
                if (Duration.between(group.getGroup_timestamp(), LocalTime.now().withNano(0)).toMinutes() > 1) {
                    group.setState(GroupState.VOTEOPEN);
                    group.setTimer(LocalTime.now().withNano(0));

                    votes.put(policy, 1);
                }   break;
            case VOTEOPEN:
                votes.put(policy, votes.get(policy) + 1);
                break;
            default:
                System.out.println("Vote phase ended");
                break;
        }
    }

    public void setLastMeasure(String sensor, Measure measure) {
        this.lastMeasure.put(sensor, measure);
    }

    public Map<String,Measure> getLastMeasures() {
        return lastMeasure;
    }

    public Measure getLastMeasure(String sensor) {
        if (lastMeasure.containsKey(sensor)) {
            return lastMeasure.get(sensor);
        } else {
            return null;
        }    
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        User other = (User) obj;
        return Objects.deepEquals(new Object[] {getID(), getName(), getSurname(), getEmail()},
                new Object[] {other.getID(), other.getName(), other.getSurname(), other.getEmail()});
    }

    @Override
    public int hashCode() {
        return Objects.hash(getID(), getName(), getSurname(), getEmail());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " [ID=" + userID + ", name=" + name + ", surname=" + surname + ", email=" + email + "]";
    }

}
