package server;

import java.io.*;
import java.net.*;

public class DictionaryServer {
    private int port;
    private String dictionaryFile;
    private Dictionary dictionary;

    public DictionaryServer(int port, String dictionaryFile) {
        this.port = port;
        this.dictionaryFile = dictionaryFile;
        this.dictionary = new Dictionary();
    }

    public void start() {
        try {
            // load dictionary
            dictionary.loadFromFile(dictionaryFile);

            // create server socket
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // accept client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                // 创建并启动客户端处理线程
                // 将在后续实现
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java DictionaryServer <port> <dictionary-file>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String dictionaryFile = args[1];

        DictionaryServer server = new DictionaryServer(port, dictionaryFile);
        server.start();
    }
}