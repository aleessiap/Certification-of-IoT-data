package Schemas;

public class ResponseSchema {
    private String message;
    private String status;
    private Object data;

    public ResponseSchema(String message, String status, Object data) {
        this.message = message;
        this.status = status;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }
}
