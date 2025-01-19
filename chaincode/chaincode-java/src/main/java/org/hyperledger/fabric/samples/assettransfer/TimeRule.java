package org.hyperledger.fabric.samples.assettransfer;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

import jdk.internal.javac.ParticipatesInPreview;


@DataType()
public class TimeRule {

    @Property()
    private final String ID;

    @Property()
    private final String sampling;

    @Property()
    private final Integer tolerance;

    @Property
    private final String dataMonitored;

    public TimeRule(String ID, String dataMonitored, String sampling, Integer tolerance) {
        this.ID = ID;
        this.dataMonitored = dataMonitored;
        this.sampling = sampling;
        this.tolerance = tolerance;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TimeRule{");
        sb.append("ID=").append(ID);
        sb.append(", sampling=").append(sampling);
        sb.append(", tolerance=").append(tolerance);
        sb.append(", dataMonitored=").append(dataMonitored);
        sb.append('}');
        return sb.toString();
    }

    public String getID() {
        return ID;
    }

    public String getSampling() {
        return sampling;
    }

    public Integer getTolerance() {
        return tolerance;
    }

    public String getDataMonitored() {
        return dataMonitored;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataMonitored(), getSampling(), getTolerance());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        TimeRule other = (TimeRule) obj;
        return Objects.deepEquals(new Object[] {getDataMonitored(), getSampling(), getTolerance()},
        new Object[] {other.getDataMonitored(), other.getSampling(), other.getTolerance()});
    }

    

}