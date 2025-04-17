package client;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private String serverAddress;
    private int serverPort;
    private boolean connected = false;

    // Common GUI components
    private JTextField wordField;
    private JTextArea resultArea;
    private JScrollPane resultScrollPane;

    // Search mode components
    private JPanel searchPanel;
    private JButton searchButton;

    // Edit mode components
    private JPanel editPanel;
    private JTextArea meaningArea;
    private JRadioButton addRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton replaceRadioButton;
    private JTextField newMeaningField;
    private JButton updateButton;

    // Navigation and utility components
    private JButton switchModeButton;
    private JButton reconnectButton;

    // Current mode
    private boolean inEditMode = false;

    public DictionaryClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        // Set window properties
        setTitle("Dictionary Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center on screen

        try {
            // Initialize network connection
            initializeConnection(serverAddress, serverPort);

            // Set up the GUI
            initComponents();
            setupLayout();
            setupEventListeners();

            // Show the initial mode
            showSearchMode();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error connecting to server: " + e.getMessage() + "\nYou can try reconnecting once the server is available.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            connected = false;
            updateConnectionStatus();
        }
    }

    private void initializeConnection(String serverAddress, int serverPort) throws IOException {
        try {
            // Create socket and connect to server
            socket = new Socket(serverAddress, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);
            connected = true;
        } catch (IOException e) {
            connected = false;
            throw new IOException("Could not connect to server: " + e.getMessage(), e);
        }
    }

    private void reconnect() {
        try {
            // Close existing connection if any
            closeConnection();

            // Attempt to reconnect
            initializeConnection(serverAddress, serverPort);
            JOptionPane.showMessageDialog(this,
                    "Successfully reconnected to the server!",
                    "Connection Success",
                    JOptionPane.INFORMATION_MESSAGE);
            updateConnectionStatus();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to reconnect: " + e.getMessage(),
                    "Reconnection Error",
                    JOptionPane.ERROR_MESSAGE);
            connected = false;
            updateConnectionStatus();
        }
    }

    private void updateConnectionStatus() {
        searchButton.setEnabled(connected);
        updateButton.setEnabled(connected);
        reconnectButton.setText(connected ? "Connected" : "Reconnect");
        reconnectButton.setBackground(connected ? new Color(100, 180, 100) : new Color(200, 100, 100));
    }

    private void initComponents() {
        // Initialize common components
        wordField = new JTextField(20);
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder("Results"));

        // Initialize search mode components
        searchPanel = new JPanel();
        searchButton = new JButton("Search");

        // Initialize edit mode components
        editPanel = new JPanel();
        meaningArea = new JTextArea(3, 20);
        meaningArea.setLineWrap(true);
        meaningArea.setWrapStyleWord(true);

        ButtonGroup radioGroup = new ButtonGroup();
        addRadioButton = new JRadioButton("Add");
        removeRadioButton = new JRadioButton("Remove");
        replaceRadioButton = new JRadioButton("Replace");

        radioGroup.add(addRadioButton);
        radioGroup.add(removeRadioButton);
        radioGroup.add(replaceRadioButton);
        addRadioButton.setSelected(true);

        newMeaningField = new JTextField(20);
        newMeaningField.setEnabled(false);

        updateButton = new JButton("Update");

        // Initialize navigation and utility components
        switchModeButton = new JButton("Switch to Edit");
        reconnectButton = new JButton("Connected");
        reconnectButton.setBackground(new Color(100, 180, 100));
        reconnectButton.setPreferredSize(new Dimension(120, 30));
    }

    private void setupLayout() {
        // Set up main content pane
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(contentPane);

        // Set up top panel (word input, search, switch mode)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // Word input panel
        JPanel wordInputPanel = new JPanel(new BorderLayout(5, 0));
        JLabel wordLabel = new JLabel("Word:");
        wordInputPanel.add(wordLabel, BorderLayout.WEST);
        wordInputPanel.add(wordField, BorderLayout.CENTER);

        // Control buttons panel
        JPanel controlPanel = new JPanel(new BorderLayout(5, 0));
        JPanel rightButtonPanel = new JPanel(new BorderLayout(5, 0));
        rightButtonPanel.add(reconnectButton, BorderLayout.WEST);
        rightButtonPanel.add(switchModeButton, BorderLayout.EAST);

        controlPanel.add(searchButton, BorderLayout.WEST);
        controlPanel.add(rightButtonPanel, BorderLayout.EAST);

        topPanel.add(wordInputPanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.SOUTH);

        // Set up search panel
        searchPanel.setLayout(new BorderLayout());

        // Set up edit panel
        editPanel.setLayout(new BorderLayout(10, 10));
        editPanel.setBorder(BorderFactory.createTitledBorder("Editing"));

        // Meaning input
        JPanel meaningPanel = new JPanel(new BorderLayout(5, 5));
        JLabel meaningLabel = new JLabel("Meaning:");
        meaningPanel.add(meaningLabel, BorderLayout.NORTH);
        meaningPanel.add(new JScrollPane(meaningArea), BorderLayout.CENTER);

        // Radio buttons for operations
        JPanel operationsPanel = new JPanel(new BorderLayout(10, 10));
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        radioPanel.add(addRadioButton);
        radioPanel.add(removeRadioButton);
        radioPanel.add(replaceRadioButton);

        // New meaning field
        JPanel newMeaningPanel = new JPanel(new BorderLayout(5, 5));
        JLabel newMeaningLabel = new JLabel("New Meaning:");
        newMeaningPanel.add(newMeaningLabel, BorderLayout.WEST);
        newMeaningPanel.add(newMeaningField, BorderLayout.CENTER);

        operationsPanel.add(radioPanel, BorderLayout.NORTH);
        operationsPanel.add(newMeaningPanel, BorderLayout.CENTER);
        operationsPanel.add(updateButton, BorderLayout.SOUTH);

        editPanel.add(meaningPanel, BorderLayout.NORTH);
        editPanel.add(operationsPanel, BorderLayout.CENTER);

        // Add the components to content pane
        contentPane.add(topPanel, BorderLayout.NORTH);
        contentPane.add(resultScrollPane, BorderLayout.CENTER);

        // Initially don't add any mode panel
        // It will be added in showSearchMode() or showEditMode()
    }

    private void setupEventListeners() {
        // Switch mode button
        switchModeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (inEditMode) {
                    showSearchMode();
                } else {
                    showEditMode();
                }
            }
        });

        // Search button action
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchWord();
            }
        });

        // Reconnect button
        reconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reconnect();
            }
        });

        // Radio button listeners
        replaceRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newMeaningField.setEnabled(true);
            }
        });

        addRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newMeaningField.setEnabled(false);
            }
        });

        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newMeaningField.setEnabled(false);
            }
        });

        // Update button action
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // First search the word to get latest status
                String word = wordField.getText().trim();
                if (word.isEmpty()) {
                    showError("Please enter a word");
                    return;
                }

                searchWord();

                // Determine which operation to perform
                if (addRadioButton.isSelected()) {
                    handleAddOperation();
                } else if (removeRadioButton.isSelected()) {
                    handleRemoveOperation();
                } else if (replaceRadioButton.isSelected()) {
                    handleReplaceOperation();
                }
            }
        });

        // Enter key in word field triggers search
        wordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchWord();
            }
        });

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });
    }

    private void showSearchMode() {
        Container contentPane = getContentPane();
        // Remove edit panel if it's there
        if (inEditMode) {
            contentPane.remove(editPanel);
        }
        inEditMode = false;
        switchModeButton.setText("Switch to Edit");

        // Repaint to reflect changes
        revalidate();
        repaint();
    }

    private void showEditMode() {
        Container contentPane = getContentPane();
        // Add edit panel
        contentPane.add(editPanel, BorderLayout.SOUTH);
        inEditMode = true;
        switchModeButton.setText("Switch to Search");

        // If we have results, append "Editing..." text
        String currentText = resultArea.getText();
        if (!currentText.isEmpty() && !currentText.endsWith("Editing...")) {
            resultArea.append("\n\nEditing...");
        }

        // Repaint to reflect changes
        revalidate();
        repaint();
    }

    private void searchWord() {
        if (!connected) {
            showError("Not connected to server. Please reconnect.");
            return;
        }

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

            // Add "Editing..." text in edit mode
            if (inEditMode) {
                resultArea.append("\n\nEditing...");
            }

        } catch (IOException e) {
            connected = false;
            updateConnectionStatus();
            showError("Error communicating with server: " + e.getMessage() + "\nPlease reconnect.");
        }
    }

    private void handleAddOperation() {
        String word = wordField.getText().trim();
        String meaning = meaningArea.getText().trim();

        // Validate inputs based on specifications
        if (word.isEmpty()) {
            showError("Word cannot be empty");
            return;
        }

        // Get user confirmation
        String operation = meaning.isEmpty() ?
                "Add new empty word: \"" + word + "\"" :
                "Add new meaning to word \"" + word + "\":\n\n\"" + meaning + "\"";

        boolean confirmed = showConfirmDialog("Confirm Add Operation", operation);
        if (!confirmed) return;

        try {
            // If meaning is empty and word exists, show error
            if (meaning.isEmpty()) {
                // First check if word exists
                Protocol.Message checkRequest = Protocol.createSearchRequest(word);
                writer.println(Protocol.toJson(checkRequest));

                String responseJson = reader.readLine();
                Protocol.Message response = Protocol.fromJson(responseJson);

                if (Protocol.SUCCESS.equals(response.getStatus())) {
                    showError("Word already exists and meaning is empty. Please provide a meaning.");
                    return;
                }

                // Word doesn't exist, create with empty meaning
                Protocol.Message request = Protocol.createAddRequest(word, "");
                writer.println(Protocol.toJson(request));

                responseJson = reader.readLine();
                response = Protocol.fromJson(responseJson);

                if (Protocol.SUCCESS.equals(response.getStatus())) {
                    resultArea.setText("Word '" + word + "' added successfully (with no meaning).");
                } else {
                    resultArea.setText("Error: " + response.getErrorMessage());
                }
            } else {
                // First check if word exists
                Protocol.Message checkRequest = Protocol.createSearchRequest(word);
                writer.println(Protocol.toJson(checkRequest));

                String responseJson = reader.readLine();
                Protocol.Message response = Protocol.fromJson(responseJson);

                if (Protocol.SUCCESS.equals(response.getStatus())) {
                    // Word exists, check if meaning exists
                    List<String> meanings = response.getResults();
                    if (meanings.contains(meaning)) {
                        showError("This meaning already exists for the word.");
                        return;
                    }

                    // Add meaning to existing word
                    Protocol.Message request = Protocol.createAddMeaningRequest(word, meaning);
                    writer.println(Protocol.toJson(request));

                    responseJson = reader.readLine();
                    response = Protocol.fromJson(responseJson);

                    if (Protocol.SUCCESS.equals(response.getStatus())) {
                        resultArea.setText("New meaning for '" + word + "' added successfully.");
                        meaningArea.setText("");
                    } else {
                        resultArea.setText("Error: " + response.getErrorMessage());
                    }
                } else {
                    // Word doesn't exist, create new word with meaning
                    Protocol.Message request = Protocol.createAddRequest(word, meaning);
                    writer.println(Protocol.toJson(request));

                    responseJson = reader.readLine();
                    response = Protocol.fromJson(responseJson);

                    if (Protocol.SUCCESS.equals(response.getStatus())) {
                        resultArea.setText("Word '" + word + "' with meaning added successfully.");
                        meaningArea.setText("");
                    } else {
                        resultArea.setText("Error: " + response.getErrorMessage());
                    }
                }
            }
        } catch (IOException e) {
            connected = false;
            updateConnectionStatus();
            showError("Error communicating with server: " + e.getMessage() + "\nPlease reconnect.");
        }
    }

    private void handleRemoveOperation() {
        String word = wordField.getText().trim();
        String meaning = meaningArea.getText().trim();

        if (word.isEmpty()) {
            showError("Word cannot be empty");
            return;
        }

        // Get user confirmation
        String operation = meaning.isEmpty() ?
                "Remove entire word: \"" + word + "\"" :
                "Remove specific meaning from word \"" + word + "\":\n\n\"" + meaning + "\"";

        boolean confirmed = showConfirmDialog("Confirm Remove Operation", operation);
        if (!confirmed) return;

        try {
            // First check if word exists
            Protocol.Message checkRequest = Protocol.createSearchRequest(word);
            writer.println(Protocol.toJson(checkRequest));

            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            if (!Protocol.SUCCESS.equals(response.getStatus())) {
                showError("Word '" + word + "' not found in the dictionary.");
                return;
            }

            // Word exists
            List<String> meanings = response.getResults();

            if (meaning.isEmpty()) {
                // Remove entire word
                Protocol.Message request = Protocol.createRemoveRequest(word);
                writer.println(Protocol.toJson(request));

                responseJson = reader.readLine();
                response = Protocol.fromJson(responseJson);

                if (Protocol.SUCCESS.equals(response.getStatus())) {
                    resultArea.setText("Word '" + word + "' removed successfully.");
                    meaningArea.setText("");
                } else {
                    resultArea.setText("Error: " + response.getErrorMessage());
                }
            } else {
                // Remove specific meaning
                if (!meanings.contains(meaning)) {
                    showError("The specified meaning does not exist for this word.");
                    return;
                }

                // Use updateMeaning with special "<delete>" marker to remove the meaning
                Protocol.Message request = Protocol.createUpdateMeaningRequest(word, meaning, "<delete>");
                writer.println(Protocol.toJson(request));

                responseJson = reader.readLine();
                response = Protocol.fromJson(responseJson);

                if (Protocol.SUCCESS.equals(response.getStatus())) {
                    resultArea.setText("Meaning removed from word '" + word + "' successfully.");
                    meaningArea.setText("");
                } else {
                    resultArea.setText("Error: " + response.getErrorMessage());
                }
            }
        } catch (IOException e) {
            connected = false;
            updateConnectionStatus();
            showError("Error communicating with server: " + e.getMessage() + "\nPlease reconnect.");
        }
    }

    private void handleReplaceOperation() {
        String word = wordField.getText().trim();
        String oldMeaning = meaningArea.getText().trim();
        String newMeaning = newMeaningField.getText().trim();

        if (word.isEmpty()) {
            showError("Word cannot be empty");
            return;
        }

        if (oldMeaning.isEmpty() || newMeaning.isEmpty()) {
            showError("Meaning and New Meaning should both be filled");
            return;
        }

        // Get user confirmation
        String operation = "Replace meaning for word \"" + word + "\":\n\n" +
                "From: \"" + oldMeaning + "\"\n" +
                "To: \"" + newMeaning + "\"";

        boolean confirmed = showConfirmDialog("Confirm Replace Operation", operation);
        if (!confirmed) return;

        try {
            // First check if word exists
            Protocol.Message checkRequest = Protocol.createSearchRequest(word);
            writer.println(Protocol.toJson(checkRequest));

            String responseJson = reader.readLine();
            Protocol.Message response = Protocol.fromJson(responseJson);

            if (!Protocol.SUCCESS.equals(response.getStatus())) {
                showError("Word '" + word + "' not found in the dictionary.");
                return;
            }

            // Word exists, check if meaning exists
            List<String> meanings = response.getResults();
            if (!meanings.contains(oldMeaning)) {
                showError("The specified meaning does not exist for this word.");
                return;
            }

            // Update the meaning
            Protocol.Message request = Protocol.createUpdateMeaningRequest(word, oldMeaning, newMeaning);
            writer.println(Protocol.toJson(request));

            responseJson = reader.readLine();
            response = Protocol.fromJson(responseJson);

            if (Protocol.SUCCESS.equals(response.getStatus())) {
                resultArea.setText("Meaning for '" + word + "' updated successfully.");
                meaningArea.setText("");
                newMeaningField.setText("");
            } else {
                resultArea.setText("Error: " + response.getErrorMessage());
            }

        } catch (IOException e) {
            connected = false;
            updateConnectionStatus();
            showError("Error communicating with server: " + e.getMessage() + "\nPlease reconnect.");
        }
    }

    private boolean showConfirmDialog(String title, String message) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        return choice == JOptionPane.OK_OPTION;
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DictionaryClient client = new DictionaryClient(serverAddress, serverPort);
                client.setVisible(true);
            }
        });
    }
}