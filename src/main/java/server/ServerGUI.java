package server;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    // Components for displaying server information
    private JLabel statusLabel;
    private JLabel portLabel;
    private JLabel dictionaryFileLabel;
    private JLabel lastSaveTimeLabel;

    // Components for displaying thread pool information
    private JLabel poolSizeLabel;
    private JLabel activeThreadsLabel;
    private JLabel queueSizeLabel;
    private JLabel completedTasksLabel;
    private JLabel rejectedTasksLabel;

    // Components for displaying client information
    private JLabel clientCountLabel;
    private JTextArea logArea;

    private Timer refreshTimer;
    private DictionaryServer server;
    private Date lastSaveTime;

    public ServerGUI(DictionaryServer server) {
        this.server = server;
        this.lastSaveTime = new Date();

        // Set up the JFrame
        setTitle("Dictionary Server Monitor");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);

        // Set up the content pane with a border layout
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // Set up the main panel with BoxLayout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Set up the server information panel
        JPanel serverInfoPanel = createServerInfoPanel();
        serverInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120)); // limit height
        mainPanel.add(serverInfoPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); // add space

        // Set up the thread pool information panel
        JPanel threadPoolPanel = createThreadPoolPanel();
        threadPoolPanel.setPreferredSize(new Dimension(600, 200)); // set preferred height
        mainPanel.add(threadPoolPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); // add space

        // Set up the log panel
        JPanel logPanel = createLogPanel();
        logPanel.setPreferredSize(new Dimension(600, 200)); // set preferred height
        mainPanel.add(logPanel);

        contentPane.add(mainPanel, BorderLayout.CENTER);

        // Add a window listener to handle window closing event
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                int result = JOptionPane.showConfirmDialog(
                        ServerGUI.this,
                        "Are you sure you want to close the server monitor?\nNote: This will NOT stop the server.",
                        "Confirm Close",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });

        // Start timer to refresh GUI every second
        refreshTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStats();
            }
        });
        refreshTimer.start();
    }

    private JPanel createServerInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Server Information"));

        statusLabel = new JLabel("Status: Running");
        statusLabel.setForeground(Color.GREEN.darker());
        portLabel = new JLabel("Port: " + server.getPort());
        dictionaryFileLabel = new JLabel("Dictionary File: " + server.getDictionaryFile());
        lastSaveTimeLabel = new JLabel("Last Save: " + formatDate(lastSaveTime));

        panel.add(statusLabel);
        panel.add(portLabel);
        panel.add(dictionaryFileLabel);
        panel.add(lastSaveTimeLabel);

        return panel;
    }

    private JPanel createThreadPoolPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Thread Pool Information"));

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // create labels for thread pool information
        Font labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

        poolSizeLabel = new JLabel("Pool Size: 0");
        activeThreadsLabel = new JLabel("Active Threads: 0");
        queueSizeLabel = new JLabel("Queue Size: 0");
        completedTasksLabel = new JLabel("Completed Tasks: 0");
        rejectedTasksLabel = new JLabel("Rejected Tasks: 0");

        // set font and alignment
        poolSizeLabel.setFont(labelFont);
        activeThreadsLabel.setFont(labelFont);
        queueSizeLabel.setFont(labelFont);
        completedTasksLabel.setFont(labelFont);
        rejectedTasksLabel.setFont(labelFont);

        poolSizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        activeThreadsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        queueSizeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        completedTasksLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rejectedTasksLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // make sure the labels are not too wide
        statsPanel.add(poolSizeLabel);
        statsPanel.add(Box.createRigidArea(new Dimension(0, 15))); // 增加垂直间距
        statsPanel.add(activeThreadsLabel);
        statsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        statsPanel.add(queueSizeLabel);
        statsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        statsPanel.add(completedTasksLabel);
        statsPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        statsPanel.add(rejectedTasksLabel);

        panel.add(statsPanel, BorderLayout.CENTER);

        panel.setMinimumSize(new Dimension(600, 180));
        panel.setPreferredSize(new Dimension(600, 180));

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Server Log"));

        clientCountLabel = new JLabel("Connected Clients: 0");
        panel.add(clientCountLabel, BorderLayout.NORTH);

        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton clearButton = new JButton("Clear Log");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText("");
            }
        });
        panel.add(clearButton, BorderLayout.SOUTH);

        return panel;
    }

    private void updateStats() {
        // Update thread pool information
        poolSizeLabel.setText("Pool Size: " + server.getThreadPool().getPoolSize());
        activeThreadsLabel.setText("Active Threads: " + server.getThreadPool().getActiveCount());
        queueSizeLabel.setText("Queue Size: " + server.getThreadPool().getQueueSize());
        completedTasksLabel.setText("Completed Tasks: " + server.getThreadPool().getCompletedTaskCount());
        rejectedTasksLabel.setText("Rejected Tasks: " + server.getThreadPool().getRejectedTaskCount());

        // Update client count
        clientCountLabel.setText("Connected Clients: " + (server.getThreadPool().getActiveCount()));

        // If the server has a new save time, update it
        if (server.getLastSaveTime() != null && !server.getLastSaveTime().equals(lastSaveTime)) {
            lastSaveTime = server.getLastSaveTime();
            lastSaveTimeLabel.setText("Last Save: " + formatDate(lastSaveTime));
        }
    }

    public void addLogMessage(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String timestamp = sdf.format(new Date());
                logArea.append("[" + timestamp + "] " + message + "\n");
                // Scroll to the bottom
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    private String formatDate(Date date) {
        if (date == null) return "Never";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public void shutdown() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        dispose();
    }
}