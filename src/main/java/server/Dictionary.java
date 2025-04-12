package server;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    public List<String> getMeanings(String word) {
        try {
            lock.readLock().lock();
            return new ArrayList<>(words.getOrDefault(word.toLowerCase(), new ArrayList<>()));
        } finally {
            lock.readLock().unlock();
        }
    }

    // add new word
    public boolean addWord(String word, String meaning) {
        if (word == null || word.trim().isEmpty() || meaning == null || meaning.trim().isEmpty()) {
            return false;
        }

        word = word.toLowerCase().trim();
        meaning = meaning.trim();

        try {
            lock.writeLock().lock();

            List<String> meanings = words.get(word);
            if (meanings != null) {
                return false; // 单词已存在
            }

            meanings = new ArrayList<>();
            meanings.add(meaning);
            words.put(word, meanings);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // delete word
    public boolean removeWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        word = word.toLowerCase().trim();

        try {
            lock.writeLock().lock();

            if (!words.containsKey(word)) {
                return false; // word does not exist
            }

            words.remove(word);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // add meaning to existing word
    public boolean addMeaning(String word, String meaning) {
        if (word == null || word.trim().isEmpty() || meaning == null || meaning.trim().isEmpty()) {
            return false;
        }

        word = word.toLowerCase().trim();
        meaning = meaning.trim();

        try {
            lock.writeLock().lock();

            List<String> meanings = words.get(word);
            if (meanings == null) {
                return false; // word does not exist
            }

            if (meanings.contains(meaning)) {
                return false; // meaning already exists
            }

            meanings.add(meaning);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // update meaning of existing word
    public boolean updateMeaning(String word, String oldMeaning, String newMeaning) {
        if (word == null || word.trim().isEmpty() ||
                oldMeaning == null || oldMeaning.trim().isEmpty() ||
                newMeaning == null || newMeaning.trim().isEmpty()) {
            return false;
        }

        word = word.toLowerCase().trim();
        oldMeaning = oldMeaning.trim();
        newMeaning = newMeaning.trim();

        try {
            lock.writeLock().lock();

            List<String> meanings = words.get(word);
            if (meanings == null) {
                return false; // word does not exist
            }

            int index = meanings.indexOf(oldMeaning);
            if (index == -1) {
                return false; // old meaning does not exist
            }

            meanings.set(index, newMeaning);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}