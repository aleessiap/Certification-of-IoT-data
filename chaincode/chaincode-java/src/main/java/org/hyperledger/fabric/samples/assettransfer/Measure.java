package org.hyperledger.fabric.samples.assettransfer;

import java.time.LocalTime;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import java.util.List;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public class Measure {
    
    @Property()
    private final String ID;

    @Property()
    private final String userID;

    @Property()
    private final LocalTime timestamp;

    @Property()
    private LocalTime timestampFromUser;

    @Property()
    private final String IDdevice;

    @Property()
    private final String groupID;


    // possono essere una lista di coppie (name, value)
    @Property()
    private final List<String> valueNames;

    @Property()
    private final List<String> values;



    public Measure(@JsonProperty("userID") final String userID, @JsonProperty("ID") final String ID, @JsonProperty("timestamp") final LocalTime timestamp,
    @JsonProperty("value") final List<String> values, @JsonProperty("IDdevice") final String IDdevice, @JsonProperty("groupID") final String groupID,
    @JsonProperty("valueNames") final List<String> valueNames) {
        this.userID = userID;
        this.ID = ID;
        this.timestamp = timestamp;
        this.values = values;
        this.IDdevice = IDdevice;
        this.groupID = groupID;
        this.valueNames = valueNames;
    }

    public String getID() {
        return ID;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public List<String> getValues() {
        return values;
    }

    public List<String> getValueNames() {
        return valueNames;
    }

    public String getIDdevice() {
        return IDdevice;
    }

    public String getUserID() {
        return userID;
    }

    public String getGroupID() {
        return groupID;
    }

}
