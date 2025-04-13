package server;

public class DictionaryException extends Exception {
    private String errorCode;

    public DictionaryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}