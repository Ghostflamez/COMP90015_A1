package common;

import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// define protocol for client-server communication
public class Protocol {
    // operation code
    public static final String SEARCH = "SEARCH";
    public static final String ADD = "ADD";
    public static final String REMOVE = "REMOVE";
    public static final String ADD_MEANING = "ADD_MEANING";
    public static final String UPDATE_MEANING = "UPDATE_MEANING";

    // status code
    public static final String SUCCESS = "SUCCESS";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String ERROR = "ERROR";

    private static final Gson gson = new GsonBuilder().create();

    // message format
    public static class Message {
        private String operation;
        private String word;
        private String meaning;
        private String newMeaning;
        private String status;
        private List<String> meanings;
        private String errorMessage;

        // getters and setters
        public String getOperation() {
            return operation;
        }
        public void setOperation(String operation) {
            this.operation = operation;
        }
        public String getWord() {
            return word;
        }
        public void setWord(String word) {
            this.word = word;
        }
        public String getMeaning() {
            return meaning;
        }
        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }
        public String getNewMeaning() {
            return newMeaning;
        }
        public void setNewMeaning(String newMeaning) {
            this.newMeaning = newMeaning;
        }
        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }
        public List<String> getMeanings() {
            return meanings;
        }
        public void setMeanings(List<String> meanings) {
            this.meanings = meanings;
        }
        public String getErrorMessage() {
            return errorMessage;
        }
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        // constructor
        public Message(String operation, String word, String meaning, String newMeaning, String status, List<String> meanings, String errorMessage) {
            this.operation = operation;
            this.word = word;
            this.meaning = meaning;
            this.newMeaning = newMeaning;
            this.status = status;
            this.meanings = meanings;
            this.errorMessage = errorMessage;
        }
    }

    // JSON序列化和反序列化方法 (可以使用内置库或第三方库如Gson或Jackson)
}