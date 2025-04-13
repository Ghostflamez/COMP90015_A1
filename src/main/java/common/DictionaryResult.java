package common;

public class DictionaryResult {
    private boolean success;
    private String statusCode;

    // factory methods for success and failure
    public static DictionaryResult success() {
        return new DictionaryResult(true, Protocol.SUCCESS);
    }

    public static DictionaryResult failure(String statusCode) {
        return new DictionaryResult(false, statusCode);
    }

    // Constructor for success
    private DictionaryResult(boolean success, String statusCode) {
        this.success = success;
        this.statusCode = statusCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getStatusCode() {
        return statusCode;
    }
}