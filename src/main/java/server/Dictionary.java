package server;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import common.Protocol;
import common.DictionaryResult;

public class Dictionary {
    //define dictionary structure
    private Map<String, List<String>> words;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Dictionary() {
        words = new HashMap<>();
    }

    // load dic from file
    public void loadFromFile(String filePath) throws IOException {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String word = parts[0].trim().toLowerCase();
                    String meaning = parts[1].trim();

                    if (!word.isEmpty() && !meaning.isEmpty()) {
                        List<String> meanings = words.getOrDefault(word, new ArrayList<>());
                        meanings.add(meaning);
                        words.put(word, meanings);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new IOException("Error loading dictionary file: " + e.getMessage(), e);
        }
    }

    // save to file
    public void saveToFile(String filePath) throws IOException {
        try {
            // read lock before write in
            lock.readLock().lock();
            //create writer
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            for (Map.Entry<String, List<String>> entry : words.entrySet()) {
                String word = entry.getKey();
                List<String> meanings = entry.getValue();

                for (String meaning : meanings) {
                    writer.write(word + ": " + meaning);
                    writer.newLine();
                }
            }

            writer.close();
        } catch (IOException e) {
            throw new IOException("Error saving dictionary file: " + e.getMessage(), e);
        } finally {
            // unlock read lock
            lock.readLock().unlock();
        }
    }

    // get word meanings
    public List<String> getMeanings(String word) throws DictionaryException {
        // check if word is empty or null
        if (word == null || word.trim().isEmpty()) {
            throw new DictionaryException("Word cannot be empty", "INVALID_INPUT");
        }

        try {
            lock.readLock().lock();
            String normalizedWord = word.toLowerCase().trim();
            List<String> meanings = words.get(normalizedWord);

            return new ArrayList<>(words.getOrDefault(word.toLowerCase().trim(), new ArrayList<>())); // return empty list if word not found
        } finally {
            lock.readLock().unlock();
        }
    }

    // add new word
    public DictionaryResult addWord(String word, String meaning) throws DictionaryException {
        if (word == null || word.trim().isEmpty()) {
            throw new DictionaryException("Word cannot be empty", "INVALID_INPUT");
        }
        if (meaning == null || meaning.trim().isEmpty()) {
            throw new DictionaryException("Meaning cannot be empty", "INVALID_INPUT");
        }

        word = word.toLowerCase().trim();
        meaning = meaning.trim();

        try {
            lock.writeLock().lock();

            List<String> meanings = words.get(word);
            if (meanings != null) {
                return DictionaryResult.failure(Protocol.DUPLICATE); // word already exists
            }

            meanings = new ArrayList<>();
            meanings.add(meaning);
            words.put(word, meanings);
            return DictionaryResult.success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // delete word
    public DictionaryResult removeWord(String word) throws DictionaryException {
        if (word == null || word.trim().isEmpty()) {
            throw new DictionaryException("Word cannot be empty", "INVALID_INPUT");
        }

        word = word.toLowerCase().trim();

        try {
            lock.writeLock().lock();

            if (!words.containsKey(word)) {
                return DictionaryResult.failure(Protocol.WORD_NOT_FOUND);  // word does not exist
            }

            words.remove(word);
            return DictionaryResult.success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // add meaning to existing word
    public DictionaryResult addMeaning(String word, String meaning) throws DictionaryException {
        if (word == null || word.trim().isEmpty() ) {
            throw new DictionaryException("Word cannot be empty", "INVALID_INPUT");
        } else if ( meaning == null || meaning.trim().isEmpty()){
            throw new DictionaryException("Meaning cannot be empty", "INVALID_INPUT");
        }

        word = word.toLowerCase().trim();
        meaning = meaning.trim();

        try {
            lock.writeLock().lock();

            List<String> meanings = words.get(word);
            if (meanings == null) {
                return DictionaryResult.failure(Protocol.WORD_NOT_FOUND); // word does not exist
            }

            if (meanings.contains(meaning)) {
                return DictionaryResult.failure(Protocol.MEANING_NOT_FOUND); // meaning already exists
            }

            meanings.add(meaning);
            return DictionaryResult.success();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // update meaning of existing word
    public DictionaryResult updateMeaning(String word, String oldMeaning, String newMeaning) throws DictionaryException {
        if (word == null || word.trim().isEmpty() ) {
            throw new DictionaryException("Word cannot be empty", "INVALID_INPUT");
        } else if ( oldMeaning == null || oldMeaning.trim().isEmpty()){
            throw new DictionaryException("OldMeaning cannot be empty", "INVALID_INPUT");
        } else if ( newMeaning == null || newMeaning.trim().isEmpty()){
            throw new DictionaryException("NewMeaning cannot be empty", "INVALID_INPUT");
        }

        word = word.toLowerCase().trim();
        oldMeaning = oldMeaning.trim();
        newMeaning = newMeaning.trim();

        try {
            lock.writeLock().lock();

            List<String> meanings = words.get(word);
            if (meanings == null) {
                return DictionaryResult.failure(Protocol.WORD_NOT_FOUND); // word does not exist
            }

            int index = meanings.indexOf(oldMeaning);
            if (index == -1) {
                return DictionaryResult.failure(Protocol.MEANING_NOT_FOUND); // old meaning does not exist
            }

            meanings.set(index, newMeaning);
            return DictionaryResult.success();
        } finally {
            lock.writeLock().unlock();
        }
    }
}