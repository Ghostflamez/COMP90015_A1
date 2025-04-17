package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DictionaryServer {
    private static final Logger LOGGER = Logger.getLogger(DictionaryServer.class.getName());

    private int port;
    private String dictionaryFile;
    private Dictionary dictionary;
    private CustomThreadPool threadPool;
    private final int AUTOSAVE_INTERVAL = 30; // seconds
    private AtomicBoolean running;
    private ScheduledExecutorService scheduler;

    // Thread pool configuration
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final long KEEP_ALIVE_TIME = 60;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final int WORK_QUEUE_CAPACITY = 100;

    public DictionaryServer(int port, String dictionaryFile) {
        this.port = port;
        this.dictionaryFile = dictionaryFile;
        this.dictionary = new Dictionary();
        this.running = new AtomicBoolean(true);

        // Create custom thread pool for handling client connections
        this.threadPool = new CustomThreadPool(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TIME_UNIT,
            WORK_QUEUE_CAPACITY
        );

        // Create scheduler for auto-saving
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        try {
            // Load dictionary
            LOGGER.info("Loading dictionary from " + dictionaryFile);
            dictionary.loadFromFile(dictionaryFile);
            LOGGER.info("Dictionary loaded successfully");

            // Set up auto-save
            setupAutoSave();

            // Set up shutdown hook
            setupShutdownHook();

            // Create server socket
            ServerSocket serverSocket = new ServerSocket(port);
            LOGGER.info("Server started on port " + port);

            // Accept client connections
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    LOGGER.info("New client connected: " + clientAddress);

                    // Create client handler and submit to our custom thread pool
                    Runnable clientHandler = new ClientHandler(clientSocket, dictionary);
                    if (!threadPool.execute(clientHandler)) {
                        LOGGER.severe("Could not process client " + clientAddress + " - thread pool full");

                        // Send a friendly message to the client before closing
                        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
                            writer.println("Server is currently at maximum capacity. Please try again later.");
                        }

                        clientSocket.close();
                    }

                    // Log thread pool stats periodically
                    if (threadPool.getPoolSize() % 5 == 0) {
                        logThreadPoolStats();
                    }

                } catch (IOException e) {
                    if (running.get()) {
                        LOGGER.log(Level.SEVERE, "Error accepting client connection: " + e.getMessage(), e);
                    }
                }
            }

            // Clean up
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server error: " + e.getMessage(), e);
        } finally {
            shutdown();
        }
    }

    private void logThreadPoolStats() {
        LOGGER.info(String.format(
            "Thread pool stats - Size: %d, Active: %d, Queue: %d, Completed: %d, Rejected: %d",
            threadPool.getPoolSize(),
            threadPool.getActiveCount(),
            threadPool.getQueueSize(),
            threadPool.getCompletedTaskCount(),
            threadPool.getRejectedTaskCount()
        ));
    }

    private void setupAutoSave() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("Auto-saving dictionary to " + dictionaryFile);
                dictionary.saveToFile(dictionaryFile);
                LOGGER.info("Dictionary auto-saved successfully");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error auto-saving dictionary: " + e.getMessage(), e);
            }
        }, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL, TimeUnit.SECONDS);
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down server...");
            shutdown();
        }));
    }

    private void shutdown() {
        if (running.getAndSet(false)) {
            try {
                // Save dictionary before shutting down
                LOGGER.info("Saving dictionary before shutdown...");
                dictionary.saveToFile(dictionaryFile);
                LOGGER.info("Dictionary saved successfully");

                // Shutdown thread pool and scheduler
                scheduler.shutdown();
                threadPool.shutdown();

                // Wait for all tasks to complete
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.info("Forcing thread pool shutdown...");
                }

                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.info("Forcing scheduler shutdown...");
                    scheduler.shutdownNow();
                }

                LOGGER.info("Server shutdown complete");
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Error during shutdown: " + e.getMessage(), e);
                scheduler.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        // Configure basic logging
        configureLogging();

        if (args.length != 2) {
            System.out.println("Usage: java -jar DictionaryServer.jar <port> <dictionary-file>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            LOGGER.severe("Invalid port number: " + args[0]);
            return;
        }

        String dictionaryFile = args[1];

        // Create and start server
        DictionaryServer server = new DictionaryServer(port, dictionaryFile);
        server.start();
    }

    private static void configureLogging() {
        // This is a simple logging setup - in a real application, you might use a properties file
        // or a more sophisticated logging framework
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    }
}