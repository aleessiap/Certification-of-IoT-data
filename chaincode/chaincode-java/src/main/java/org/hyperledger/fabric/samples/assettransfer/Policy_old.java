package org.hyperledger.fabric.samples.assettransfer;

import java.util.List;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public class Policy_old {

    @Property()
    private final String ID;
    
    @Property()
    private List<String> samplingIntervals;

    @Property()
    private final PolicyType policyType;

    @Property()
    private List<String> valueNames;

    @Property()
    private List<String> valueThresholds;

    @Property()
    private List<String> operatorThresholds;

    //Policy constructor with samplingInterval, valueThresholds and operatorThresholds
    public Policy(@JsonProperty("ID") final String ID, 
                @JsonProperty("samplingIntervals") final List<String> samplingIntervals,
                @JsonProperty("policyType") final PolicyType policyType,
                @JsonProperty("valueNames") final List<String> valueNames,
                @JsonProperty("valueThresholds") final List<String> valueThresholds,
                @JsonProperty("operatorThresholds") final List<String> operatorThresholds){

        this.ID = ID;
        this.policyType = policyType;
        this.valueNames = valueNames;
        
        if (policyType == PolicyType.TIMEVALUE){
            this.samplingIntervals = samplingIntervals;
            this.valueThresholds = valueThresholds;
            this.operatorThresholds = operatorThresholds;
        } else if (policyType == PolicyType.TIME){
            this.samplingIntervals = samplingIntervals;
            this.valueThresholds = null;
            this.operatorThresholds = null;
        } else if (policyType == PolicyType.VALUE){
            this.valueThresholds = valueThresholds;
            this.operatorThresholds = operatorThresholds;
            this.samplingIntervals = null;
        }
    }

    public String getSamplingInterval() {
        return samplingIntervals;
    }

    public PolicyType getpolicyType() {
        return policyType;
    }

    public String getID() {
        return ID;
    }

    public List<String> getValueNames() {
        return valueNames;
    }

    public List<String> getValueThresholds() {
        return valueThresholds;
    }

    public List<String> getOperatorThresholds() {
        return operatorThresholds;
    }

}
