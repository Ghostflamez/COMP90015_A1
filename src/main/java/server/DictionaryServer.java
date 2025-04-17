package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DictionaryServer {
    private int port;
    private String dictionaryFile;
    private Dictionary dictionary;
    private ExecutorService threadPool;
    private final int AUTOSAVE_INTERVAL = 30; // seconds
    private AtomicBoolean running;
    private ScheduledExecutorService scheduler;

    public DictionaryServer(int port, String dictionaryFile) {
        this.port = port;
        this.dictionaryFile = dictionaryFile;
        this.dictionary = new Dictionary();
        this.running = new AtomicBoolean(true);

        // Create thread pool for handling client connections
        int processors = Runtime.getRuntime().availableProcessors();
        this.threadPool = Executors.newFixedThreadPool(processors * 2);

        // Create scheduler for auto-saving
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        try {
            // Load dictionary
            System.out.println("Loading dictionary from " + dictionaryFile);
            dictionary.loadFromFile(dictionaryFile);
            System.out.println("Dictionary loaded successfully");

            // Set up auto-save
            setupAutoSave();

            // Set up shutdown hook
            setupShutdownHook();

            // Create server socket
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // Accept client connections
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    // Create and submit client handler to thread pool
                    ClientHandler clientHandler = new ClientHandler(clientSocket, dictionary);
                    threadPool.submit(clientHandler);
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }

            // Clean up
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void setupAutoSave() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Auto-saving dictionary to " + dictionaryFile);
                dictionary.saveToFile(dictionaryFile);
                System.out.println("Dictionary auto-saved successfully");
            } catch (IOException e) {
                System.err.println("Error auto-saving dictionary: " + e.getMessage());
            }
        }, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL, TimeUnit.SECONDS);
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            shutdown();
        }));
    }

    private void shutdown() {
        if (running.getAndSet(false)) {
            try {
                // Save dictionary before shutting down
                System.out.println("Saving dictionary before shutdown...");
                dictionary.saveToFile(dictionaryFile);
                System.out.println("Dictionary saved successfully");

                // Shutdown thread pool and scheduler
                scheduler.shutdown();
                threadPool.shutdown();

                // Wait for all tasks to complete
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Forcing thread pool shutdown...");
                    threadPool.shutdownNow();
                }

                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Forcing scheduler shutdown...");
                    scheduler.shutdownNow();
                }

                System.out.println("Server shutdown complete");
            } catch (IOException | InterruptedException e) {
                System.err.println("Error during shutdown: " + e.getMessage());
                threadPool.shutdownNow();
                scheduler.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java -jar DictionaryServer.jar <port> <dictionary-file>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            return;
        }

        String dictionaryFile = args[1];

        // Create and start server
        DictionaryServer server = new DictionaryServer(port, dictionaryFile);
        server.start();
    }
}