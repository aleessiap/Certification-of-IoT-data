package org.hyperledger.fabric.samples.assettransfer;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

import jdk.vm.ci.meta.Local;

@DataType()
public class Violation {

    @Property()
    private final String ID;

    @Property()
    private final String userID;
    
    @Property()
    private final String deviceID;

    @Property()
    private final LocalTime timestamp;

    @Property()
    private final String groupID;

    @Property
    private final String measureID;

    @Property()
    private final String value;

    @Property()
    private final String type;
    
    public Violation(@JsonProperty("ID") final String ID, @JsonProperty("measureID") final String measureID, @JsonProperty("userID") final String userID, @JsonProperty("groupID") final String groupID, @JsonProperty("deviceID") final String deviceID, @JsonProperty("timestamp") final LocalTime timestamp,
    @JsonProperty("value") final String value,@JsonProperty("type") final String type) {
        this.ID = ID;
        this.measureID = measureID;
        this.userID = userID;
        this.groupID = groupID;
        this.deviceID = deviceID;
        this.timestamp = timestamp;
        this.value = value;
        this.type = type;
    }

    public Violation(){
        this.ID = "0";
        this.measureID = "0";
        this.userID = "0";
        this.groupID = "0";
        this.deviceID = "0";
        this.timestamp = LocalTime.now();
        this.value = "0";
        this.type = "0";
    }

    public String getID() {
        return ID;
    }

    public String getdeviceID() {
        return deviceID;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "Violation [IDdevice=" + deviceID + ", timestamp=" + timestamp + ", type=" + type + "]";
    }

}
