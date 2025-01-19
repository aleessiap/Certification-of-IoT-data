package org.hyperledger.fabric.samples.assettransfer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import org.hyperledger.fabric.contract.annotation.Property;

public class IDToken {
    // la tripla identificativa del device di un utente in un gruppo 

    @Property()
    public final String id;

    @Property()
    public final String groupid;

    @Property()
    public final String userid;

    @Property()
    public final String deviceid;
    
    public IDToken(String groupid, String userid, String deviceid) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((groupid + userid + deviceid).getBytes());
        this.id = UUID.nameUUIDFromBytes(hash).toString();
        this.groupid = groupid;
        this.userid = userid;
        this.deviceid = deviceid;
    }

    public String getId() {
        return id;
    }

    public String getGroupid() {
        return groupid;
    }

    public String getUserid() {
        return userid;
    }

    public String getDeviceid() {
        return deviceid;
    }

    @Override
    public String toString() {
        return "IDToken [id=" + id + ", groupid=" + groupid + ", userid=" + userid + ", deviceid=" + deviceid + "]";
    }

    @Override
    public int hashCode() {
        return id.hashCode() + groupid.hashCode() + userid.hashCode() + deviceid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        IDToken that = (IDToken) o;
        return id.equals(that.id) && groupid.equals(that.groupid) && userid.equals(that.userid) && deviceid.equals(that.deviceid);
    }
}
