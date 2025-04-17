package client;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class DictionaryClient extends JFrame {
    // Network components
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // GUI components
    private JTextField wordField;
    private JTextArea resultArea;
    private JTextArea meaningArea;
    private JButton searchButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton addMeaningButton;
    private JButton updateMeaningButton;
    private JTextField oldMeaningField;
    private JTextField newMeaningField;

    public DictionaryClient(String serverAddress, int serverPort) {
        try {
            // Initialize network connection
            initializeConnection(serverAddress, serverPort);

            // Set up the GUI
            setupGUI();

            // Set up event listeners
            setupEventListeners();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error connecting to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void initializeConnection(String serverAddress, int serverPort) throws IOException {
        try {
            // Create socket and connect to server
            socket = new Socket(serverAddress, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
        } catch (IOException e) {
            throw new IOException("Could not connect to server: " + e.getMessage(), e);
        }
    }

    private void setupGUI() {
        // Set window properties
        setTitle("Dictionary Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center on screen

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top panel for word input and search
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JLabel wordLabel = new JLabel("Word:");
        wordField = new JTextField(20);
        searchButton = new JButton("Search");

        JPanel wordInputPanel = new JPanel(new BorderLayout(5, 0));
        wordInputPanel.add(wordLabel, BorderLayout.WEST);
        wordInputPanel.add(wordField, BorderLayout.CENTER);

        topPanel.add(wordInputPanel, BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.EAST);

        // Center panel with results area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder("Results"));

        // Bottom panel for meaning input and operations
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));

        // Meaning input panel
        JPanel meaningPanel = new JPanel(new BorderLayout(5, 5));
        meaningPanel.setBorder(BorderFactory.createTitledBorder("Meaning"));

        meaningArea = new JTextArea(4, 20);
        meaningArea.setLineWrap(true);
        meaningArea.setWrapStyleWord(true);
        JScrollPane meaningScrollPane = new JScrollPane(meaningArea);
        meaningPanel.add(meaningScrollPane, BorderLayout.CENTER);

        // Update meaning panel
        JPanel updatePanel = new JPanel(new BorderLayout(5, 5));
        updatePanel.setBorder(BorderFactory.createTitledBorder("Update Meaning"));

        JPanel oldNewPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        oldNewPanel.add(new JLabel("Old Meaning:"));
        oldMeaningField = new JTextField();
        oldNewPanel.add(oldMeaningField);
        oldNewPanel.add(new JLabel("New Meaning:"));
        newMeaningField = new JTextField();
        oldNewPanel.add(newMeaningField);

        updatePanel.add(oldNewPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        addButton = new JButton("Add Word");
        removeButton = new JButton("Remove Word");
        addMeaningButton = new JButton("Add Meaning");
        updateMeaningButton = new JButton("Update Meaning");

        buttonsPanel.add(addButton);
        buttonsPanel.add(removeButton);
        buttonsPanel.add(addMeaningButton);
        buttonsPanel.add(updateMeaningButton);

        // Combine bottom panels
        JPanel operationsPanel = new JPanel(new BorderLayout(10, 10));
        operationsPanel.add(meaningPanel, BorderLayout.CENTER);
        operationsPanel.add(updatePanel, BorderLayout.SOUTH);

        bottomPanel.add(operationsPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add all components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(resultScrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        setContentPane(mainPanel);
    }

    private void setupEventListeners() {
        // Search button action
        searchButton.addActionListener(e -> searchWord());

        // Add word button action
        addButton.addActionListener(e -> addWord());

        // Remove word button action
        removeButton.addActionListener(e -> removeWord());

        // Add meaning button action
        addMeaningButton.addActionListener(e -> addMeaning());

        // Update meaning button action
        updateMeaningButton.addActionListener(e -> updateMeaning());

        // Enter key in word field triggers search
        wordField.addActionListener(e -> searchWord());

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });
    }

    private void searchWord() {
        String word = wordField.getText().trim();
        if (word.isEmpty()) {
            showError("Please enter a word to search");
            return;
        }

        try {
            // Create and send search request
            Protocol.Message request = Protocol.createSearchRequest(word);
            writer.println(Protocol.toJson(request));

            // Get response
            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            // Process and display results
            displaySearchResults(response);

        } catch (IOException e) {
            showError("Error communicating with server: " + e.getMessage());
        }
    }

    private void addWord() {
        String word = wordField.getText().trim();
        String meaning = meaningArea.getText().trim();

        if (word.isEmpty() || meaning.isEmpty()) {
            showError("Both word and meaning must be provided");
            return;
        }

        try {
            // Create and send add request
            Protocol.Message request = Protocol.createAddRequest(word, meaning);
            writer.println(Protocol.toJson(request));

            // Get response
            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            // Process response
            if (Protocol.SUCCESS.equals(response.getStatus())) {
                resultArea.setText("Word '" + word + "' added successfully.");
                meaningArea.setText("");
            } else if (Protocol.DUPLICATE.equals(response.getStatus())) {
                resultArea.setText("Word '" + word + "' already exists in the dictionary.");
            } else {
                resultArea.setText("Error: " + response.getErrorMessage());
            }

        } catch (IOException e) {
            showError("Error communicating with server: " + e.getMessage());
        }
    }

    private void removeWord() {
        String word = wordField.getText().trim();

        if (word.isEmpty()) {
            showError("Please enter a word to remove");
            return;
        }

        try {
            // Create and send remove request
            Protocol.Message request = Protocol.createRemoveRequest(word);
            writer.println(Protocol.toJson(request));

            // Get response
            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            // Process response
            if (Protocol.SUCCESS.equals(response.getStatus())) {
                resultArea.setText("Word '" + word + "' removed successfully.");
                meaningArea.setText("");
            } else if (Protocol.WORD_NOT_FOUND.equals(response.getStatus())) {
                resultArea.setText("Word '" + word + "' not found in the dictionary.");
            } else {
                resultArea.setText("Error: " + response.getErrorMessage());
            }

        } catch (IOException e) {
            showError("Error communicating with server: " + e.getMessage());
        }
    }

    private void addMeaning() {
        String word = wordField.getText().trim();
        String meaning = meaningArea.getText().trim();

        if (word.isEmpty() || meaning.isEmpty()) {
            showError("Both word and meaning must be provided");
            return;
        }

        try {
            // Create and send add meaning request
            Protocol.Message request = Protocol.createAddMeaningRequest(word, meaning);
            writer.println(Protocol.toJson(request));

            // Get response
            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            // Process response
            if (Protocol.SUCCESS.equals(response.getStatus())) {
                resultArea.setText("New meaning for '" + word + "' added successfully.");
                meaningArea.setText("");
            } else if (Protocol.WORD_NOT_FOUND.equals(response.getStatus())) {
                resultArea.setText("Word '" + word + "' not found in the dictionary.");
            } else {
                resultArea.setText("Error: " + response.getErrorMessage());
            }

        } catch (IOException e) {
            showError("Error communicating with server: " + e.getMessage());
        }
    }

    private void updateMeaning() {
        String word = wordField.getText().trim();
        String oldMeaning = oldMeaningField.getText().trim();
        String newMeaning = newMeaningField.getText().trim();

        if (word.isEmpty() || oldMeaning.isEmpty() || newMeaning.isEmpty()) {
            showError("Word, old meaning, and new meaning must all be provided");
            return;
        }

        try {
            // Create and send update meaning request
            Protocol.Message request = Protocol.createUpdateMeaningRequest(word, oldMeaning, newMeaning);
            writer.println(Protocol.toJson(request));

            // Get response
            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            // Process response
            if (Protocol.SUCCESS.equals(response.getStatus())) {
                resultArea.setText("Meaning for '" + word + "' updated successfully.");
                oldMeaningField.setText("");
                newMeaningField.setText("");
            } else if (Protocol.WORD_NOT_FOUND.equals(response.getStatus())) {
                resultArea.setText("Word '" + word + "' not found in the dictionary.");
            } else if (Protocol.MEANING_NOT_FOUND.equals(response.getStatus())) {
                resultArea.setText("Old meaning for '" + word + "' not found.");
            } else {
                resultArea.setText("Error: " + response.getErrorMessage());
            }

        } catch (IOException e) {
            showError("Error communicating with server: " + e.getMessage());
        }
    }

    private void displaySearchResults(Protocol.Message response) {
        if (Protocol.SUCCESS.equals(response.getStatus())) {
            List<String> meanings = response.getResults();
            StringBuilder sb = new StringBuilder();
            sb.append("Meanings for '").append(response.getWord()).append("':\n\n");

            for (int i = 0; i < meanings.size(); i++) {
                sb.append(i + 1).append(". ").append(meanings.get(i)).append("\n");
            }

            resultArea.setText(sb.toString());
        } else if (Protocol.MEANING_NOT_FOUND.equals(response.getStatus()) ||
                Protocol.WORD_NOT_FOUND.equals(response.getStatus())) {
            resultArea.setText("Word '" + response.getWord() + "' not found in the dictionary.");
        } else {
            resultArea.setText("Error: " + response.getErrorMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Check command-line arguments
        if (args.length != 2) {
            System.out.println("Usage: java -jar DictionaryClient.jar <server-address> <server-port>");
            return;
        }

        String serverAddress = args[0];
        int serverPort;

        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            return;
        }

        // Start the client application
        SwingUtilities.invokeLater(() -> {
            DictionaryClient client = new DictionaryClient(serverAddress, serverPort);
            client.setVisible(true);
        });
    }
}