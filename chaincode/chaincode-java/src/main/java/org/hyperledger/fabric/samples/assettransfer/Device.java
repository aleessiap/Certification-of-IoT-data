package org.hyperledger.fabric.samples.assettransfer;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public class Device {
    
    @Property()
    private final String ID;

    @Property()
    private final String unit; // unità di misura che viene misurata 

    // in futuro aggiungere anche le misurazioni possibili dal device 

    @Property()
    private final String owner;
    
    public Device(@JsonProperty("ID") final String ID, @JsonProperty("unit") final String unit, @JsonProperty("owner") final String owner) {
        this.ID = ID;
        this.unit = unit;
        this.owner = owner;
    }

    public String getID() {
        return ID;
    }

    public String getUnit() {
        return unit;
    }

    public String getOwner() {
        return owner;
    }

}
