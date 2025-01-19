package Schemas;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;

import java.util.Map;

/*
{
    "measureStream": "uuid",
    "valueTime": "datetime",
    "processingTime": "datetime",
    "data": {
        "name1": "value1",
        "name2": "value2",
        ...
    }
}
*/

public class SendMeasure {
    
    @NotNull
    private UUID measureStream;

    @NotNull
    private LocalDateTime valueTime;

    @NotNull
    private LocalDateTime processingTime;

    @NotNull
    private Map<String, String> data;

    //Getters
    public UUID getMeasureStream() {
        return measureStream;
    }

    public LocalDateTime getValueTime() {
        return valueTime;
    }

    public LocalDateTime getProcessingTime() {
        return processingTime;
    }

    public Map<String, String> getData() {
        return data;
    }

    //Generate UUID
    public void setMeasureStream(String groupId, String userID, String deviceId) {
        String concatenatedData = groupId + "|" + userID + "|" + deviceId;
        String base64EncodedData = Base64.getUrlEncoder().withoutPadding().encodeToString(concatenatedData.getBytes());
        UUID uuid = UUID.nameUUIDFromBytes(base64EncodedData.getBytes());
        this.measureStream = uuid;
    }

    public static String[] decodeFromUUID(UUID uuid) {
        String base64EncodedData = new String(Base64.getUrlDecoder().decode(uuid.toString().getBytes()));
        return base64EncodedData.split("\\|");
    }

}
