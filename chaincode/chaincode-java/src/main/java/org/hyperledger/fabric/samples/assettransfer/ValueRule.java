package org.hyperledger.fabric.samples.assettransfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

import jdk.internal.javac.ParticipatesInPreview;


@DataType()
public class ValueRule {

    @Property()
    private final String ID;
    
    @Property
    private final String dataMonitored;

    @Property()
    private final Integer tolerance;

    @Property()
    private final String operator1;

    @Property()
    private final Float threshold1;

    @Property()
    private final String operator2;

    @Property()
    private final Float threshold2;


    public ValueRule(String ID, String dataMonitored,  Integer tolerance, String operator1, Float threshold1, String operator2, Float threshold2) {
        this.ID = ID;
        this.dataMonitored = dataMonitored;
        this.tolerance = tolerance;
        this.operator1 = operator1;
        this.threshold1 = threshold1;
        this.operator2 = operator2;
        this.threshold2 = threshold2;
    }

    public String getID() {
        return ID;
    }

    public Float getThreshold1() {
        return threshold1;
    }

    public String getOperator1() {
        return operator1;
    }

    public Float getThreshold2() {
        return threshold2;
    }

    public String getOperator2() {
        return operator2;
    }

    public Integer getTolerance() {
        return tolerance;
    }

    public String getDataMonitored() {
        return dataMonitored;
    }

    

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValueRule{");
        sb.append("ID=").append(ID);
        sb.append(", dataMonitored=").append(dataMonitored);
        sb.append(", tolerance=").append(tolerance);
        sb.append(", operator1=").append(operator1);
        sb.append(", threshold1=").append(threshold1);
        sb.append(", operator2=").append(operator2);
        sb.append(", threshold2=").append(threshold2);
        sb.append('}');
        return sb.toString();
    }


    @Override
    public int hashCode() {
        return Objects.hash(getDataMonitored(), getTolerance(), getOperator1(), getThreshold1(), getOperator2(), getThreshold2());
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        ValueRule other = (ValueRule) obj;
        return Objects.deepEquals(new Object[] {getDataMonitored(), getTolerance(), getOperator1(), getThreshold1(), getOperator2(), getThreshold2()},
        new Object[] {other.getDataMonitored(), other.getTolerance(), other.getOperator1(), other.getThreshold1(), other.getOperator2(), other.getThreshold2()});
    }
    

}