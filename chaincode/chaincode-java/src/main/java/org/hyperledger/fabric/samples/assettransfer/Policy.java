package org.hyperledger.fabric.samples.assettransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public class Policy {

    @Property()
    private String ID;
    
    @Property()
    private PolicyType policyType;

    @Property()
    private List<TimeRule> time_rules;

    @Property()
    private List<ValueRule> value_rules;

    @Property()
    private long reset_time;

    public Policy(){
        this.ID = "0";
        this.policyType = PolicyType.TIME;
        this.time_rules = new ArrayList<TimeRule>();
        this.value_rules = new ArrayList<ValueRule>();
        this.reset_time = 43800; //a month
    }

    public Policy(String ID, PolicyType policyType, List<String> rules){
        this.ID = ID;
        this.policyType = policyType;
        this.reset_time = 43800;
        Integer counter = 0;
        if (null == policyType) {
           throw new IllegalArgumentException("Unknown policy type.");
        } else switch (policyType) {
            case TIME:
                {
                    int ruleSize = 3; // Each TimeRule requires 3 elements
                    if (rules.size() % ruleSize != 0) {
                        throw new IllegalArgumentException("Invalid number of arguments for TIME policy.");
                    }       
                    for (int i = 0; i < rules.size(); i += ruleSize) {
                        TimeRule timeRule = new TimeRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                rules.get(i + 1),
                                Integer.valueOf(rules.get(i + 2))
                        );
                        this.time_rules.add(timeRule);
                        counter = counter + 1;
                    } 
                    
                }
                break;
            case VALUE:
                {
                    int ruleSize = 6; // Each ValueRule requires 6 elements
                    if (rules.size() % ruleSize != 0) {
                        throw new IllegalArgumentException("Invalid number of arguments for VALUE policy.");
                    }       
                    for (int i = 0; i < rules.size(); i += ruleSize) {
                        ValueRule valueRule = new ValueRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                Integer.valueOf(rules.get(i + 1)),
                                rules.get(i+2),
                                Float.valueOf(rules.get(i + 3)),
                                rules.get(i+4),
                                Float.valueOf(rules.get(i + 5))
                        );
                        this.value_rules.add(valueRule);
                        counter = counter + 1;
                    }       
                }
                break;

            case TIMEVALUE:
                {
                    int ruleSize = 8; // Each TimeValue group requires 8 elements (3 for TimeRule, 6 for ValueRule)
                    if (rules.size() % ruleSize != 0) {
                        throw new IllegalArgumentException("Invalid number of arguments for TIMEVALUE policy.");
                    }       
                    for (int i = 0; i < rules.size(); i += ruleSize) {
                        TimeRule timeRule = new TimeRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                rules.get(i + 1),
                                 Integer.valueOf(rules.get(i + 2))
                        );
                        counter = counter + 1;
                        ValueRule valueRule = new ValueRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                Integer.valueOf(rules.get(i + 3)),
                                rules.get(i+4),
                                Float.valueOf(rules.get(i + 5)),
                                rules.get(i+6),
                                Float.valueOf(rules.get(i + 7))
                        );
                        counter = counter + 1;
                        this.time_rules.add(timeRule);
                        this.value_rules.add(valueRule);
                    }       
                    break;
                }
            default:
                throw new IllegalArgumentException("Unknown policy type.");
        }    
    }

    public Policy(String ID, PolicyType policyType, List<String> rules, Integer reset_time) {
        this.ID = ID;
        this.policyType = policyType;
        this.reset_time = reset_time;
        Integer counter = 0;
        if (null == policyType) {
           throw new IllegalArgumentException("Unknown policy type.");
        } else switch (policyType) {
            case TIME:
                {
                    int ruleSize = 3; // Each TimeRule requires 3 elements
                    if (rules.size() % ruleSize != 0) {
                        throw new IllegalArgumentException("Invalid number of arguments for TIME policy.");
                    }       
                    for (int i = 0; i < rules.size(); i += ruleSize) {
                        TimeRule timeRule = new TimeRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                rules.get(i + 1),
                                Integer.valueOf(rules.get(i + 2))
                        );
                        this.time_rules.add(timeRule);
                        counter = counter + 1;
                    } 
                    
                }
                break;
            case VALUE:
                {
                    int ruleSize = 6; // Each ValueRule requires 6 elements
                    if (rules.size() % ruleSize != 0) {
                        throw new IllegalArgumentException("Invalid number of arguments for VALUE policy.");
                    }       
                    for (int i = 0; i < rules.size(); i += ruleSize) {
                        ValueRule valueRule = new ValueRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                Integer.valueOf(rules.get(i + 1)),
                                rules.get(i+2),
                                Float.valueOf(rules.get(i + 3)),
                                rules.get(i+4),
                                Float.valueOf(rules.get(i + 5))
                        );
                        this.value_rules.add(valueRule);
                        counter = counter + 1;
                    }       
                }
                break;

            case TIMEVALUE:
                {
                    int ruleSize = 8; // Each TimeValue group requires 8 elements (3 for TimeRule, 6 for ValueRule)
                    if (rules.size() % ruleSize != 0) {
                        throw new IllegalArgumentException("Invalid number of arguments for TIMEVALUE policy.");
                    }       
                    for (int i = 0; i < rules.size(); i += ruleSize) {
                        TimeRule timeRule = new TimeRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                rules.get(i + 1),
                                 Integer.valueOf(rules.get(i + 2))
                        );
                        counter = counter + 1;
                        ValueRule valueRule = new ValueRule(
                                ID + "_" + Integer.toString(counter),
                                rules.get(i),
                                Integer.valueOf(rules.get(i + 3)),
                                rules.get(i+4),
                                Float.valueOf(rules.get(i + 5)),
                                rules.get(i+6),
                                Float.valueOf(rules.get(i + 7))
                        );
                        counter = counter + 1;
                        this.time_rules.add(timeRule);
                        this.value_rules.add(valueRule);
                    }       
                    break;
                }
            default:
                throw new IllegalArgumentException("Unknown policy type.");
        }

    }


    public String getID() {
        return ID;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public List<TimeRule> getTimeRules() {
        return time_rules;
    }

    public List<ValueRule> getValueRules() {
        return value_rules;
    }

    public long getReset_Time(){
        return reset_time;
    }

    public List<String> getRulesIDs(){
        List<String> list = new ArrayList();
        for (TimeRule tr : this.getTimeRules()){
            list.add(tr.getID());
        }
        for (ValueRule vr : this.getValueRules()){
            list.add(vr.getID());
        }
        return list;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Policy{");
        sb.append("ID=").append(ID);
        sb.append(", policyType=").append(policyType);
        sb.append(", resetTime=").append(reset_time);
        sb.append(", time_rules=").append(time_rules);
        sb.append(", value_rules=").append(value_rules);
        sb.append('}');
        return sb.toString();
    }
 

    @Override
    public int hashCode() {
        return Objects.hash(getPolicyType(), getReset_Time(), getTimeRules(), getValueRules());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        Policy other = (Policy) obj;
        return getPolicyType() == other.getPolicyType() && getReset_Time() == other.getReset_Time() &&
               Objects.equals(this.getTimeRules(), other.getTimeRules()) &&
               Objects.equals(this.getValueRules(), other.getValueRules());
    }

}