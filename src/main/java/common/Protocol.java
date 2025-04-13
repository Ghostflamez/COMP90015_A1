// src/main/java/common/Protocol.java
package common;

import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// define protocol for client-server communication
public class Protocol {
    // operation codes
    public static final String SEARCH = "SEARCH";
    public static final String ADD = "ADD";
    public static final String REMOVE = "REMOVE";
    public static final String ADD_MEANING = "ADD_MEANING";
    public static final String UPDATE_MEANING = "UPDATE_MEANING";

    // status codes
    public static final String SUCCESS = "SUCCESS";
    public static final String WORD_NOT_FOUND = "WORD_NOT_FOUND";
    public static final String MEANING_NOT_FOUND = "MEANING_NOT_FOUND";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String ERROR = "ERROR";

    private static final Gson gson = new GsonBuilder().create();

    // message format
    public static class Message {
        private String operation;  // operation type
        private String word;       // key
        private String status;     // response status

        // parameters for operations
        private List<String> params = new ArrayList<>();

        // results for operations
        private List<String> results = new ArrayList<>();
        private String errorMessage;

        // default constructor
        public Message() {
        }

        // Getters and setters
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }

        public String getWord() { return word; }
        public void setWord(String word) { this.word = word; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<String> getParams() { return params; }
        public void setParams(List<String> params) { this.params = params; }

        public List<String> getResults() { return results; }
        public void setResults(List<String> results) { this.results = results; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        // helper methods for adding parameters and results
        public void addParam(String param) {
            if (param != null) {
                this.params.add(param);
            }
        }

        public String getParamAt(int index) {
            if (params != null && index >= 0 && index < params.size()) {
                return params.get(index);
            }
            return null;
        }

        public void addResult(String result) {
            if (result != null) {
                this.results.add(result);
            }
        }

        // helper methods for getting specific parameters
        public String getMeaning() {
            return getParamAt(0);
        }

        public String getOldMeaning() {
            return getParamAt(0);
        }

        public String getNewMeaning() {
            return getParamAt(1);
        }
    }

    // convert message object to JSON string -- for sending
    public static String toJson(Message message) {
        return gson.toJson(message);
    }

    // reconvert JSON string to message object -- for receiving
    public static Message fromJson(String json) {
        return gson.fromJson(json, Message.class);
    }

    // package message creation methods
    public static Message createSearchRequest(String word) {
        Message message = new Message();
        message.setOperation(SEARCH);
        message.setWord(word);
        return message;
    }

    public static Message createAddRequest(String word, String meaning) {
        Message message = new Message();
        message.setOperation(ADD);
        message.setWord(word);
        message.addParam(meaning);
        return message;
    }

    public static Message createRemoveRequest(String word) {
        Message message = new Message();
        message.setOperation(REMOVE);
        message.setWord(word);
        return message;
    }

    public static Message createAddMeaningRequest(String word, String meaning) {
        Message message = new Message();
        message.setOperation(ADD_MEANING);
        message.setWord(word);
        message.addParam(meaning);
        return message;
    }

    public static Message createUpdateMeaningRequest(String word, String oldMeaning, String newMeaning) {
        Message message = new Message();
        message.setOperation(UPDATE_MEANING);
        message.setWord(word);
        message.addParam(oldMeaning);
        message.addParam(newMeaning);
        return message;
    }
}