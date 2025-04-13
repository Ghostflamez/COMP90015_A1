package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Dictionary dictionary;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(Socket clientSocket, Dictionary dictionary) {
        this.clientSocket = clientSocket;
        this.dictionary = dictionary;
    }

    @Override
    public void run() {
        try {
            // initialize I/O streams
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

            String requestJson;
            // listen for requests until client disconnects
            while ((requestJson = reader.readLine()) != null) {
                // parse request
                Protocol.Message request = Protocol.fromJson(requestJson);
                Protocol.Message response = processRequest(request);

                // send response
                writer.println(Protocol.toJson(response));
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private Protocol.Message processRequest(Protocol.Message request) {
        Protocol.Message response = new Protocol.Message();
        response.setOperation(request.getOperation());
        response.setWord(request.getWord());

        try {
            switch (request.getOperation()) {
                case Protocol.SEARCH:
                    handleSearch(request, response);
                    break;
                case Protocol.ADD:
                    handleAdd(request, response);
                    break;
                case Protocol.REMOVE:
                    handleRemove(request, response);
                    break;
                case Protocol.ADD_MEANING:
                    handleAddMeaning(request, response);
                    break;
                case Protocol.UPDATE_MEANING:
                    handleUpdateMeaning(request, response);
                    break;
                default:
                    response.setStatus(Protocol.ERROR);
                    response.setErrorMessage("Unknown operation");
            }
        } catch (Exception e) {
            response.setStatus(Protocol.ERROR);
            response.setErrorMessage("Server error: " + e.getMessage());
        }

        return response;
    }

    private void handleSearch(Protocol.Message request, Protocol.Message response) {
        List<String> meanings = dictionary.getMeanings(request.getWord());

        if (meanings == null || meanings.isEmpty()) {
            response.setStatus(Protocol.MEANING_NOT_FOUND);
        } else {
            response.setStatus(Protocol.SUCCESS);
            response.setResults(meanings);
        }
    }

    private void handleAdd(Protocol.Message request, Protocol.Message response) {
        String meaning = request.getMeaning();

        if (meaning == null || meaning.trim().isEmpty()) {
            response.setStatus(Protocol.ERROR);
            response.setErrorMessage("Meaning cannot be empty");
            return;
        }

        boolean success = dictionary.addWord(request.getWord(), meaning);

        if (success) {
            response.setStatus(Protocol.SUCCESS);
        } else {
            response.setStatus(Protocol.DUPLICATE);
        }
    }

    private void handleRemove(Protocol.Message request, Protocol.Message response) {
        boolean success = dictionary.removeWord(request.getWord());

        if (success) {
            response.setStatus(Protocol.SUCCESS);
        } else {
            response.setStatus(Protocol.WORD_NOT_FOUND);
        }
    }

    private void handleAddMeaning(Protocol.Message request, Protocol.Message response) {
        String meaning = request.getMeaning();

        if (meaning == null || meaning.trim().isEmpty()) {
            response.setStatus(Protocol.ERROR);
            response.setErrorMessage("Meaning cannot be empty");
            return;
        }

        boolean success = dictionary.addMeaning(request.getWord(), meaning);

        if (success) {
            response.setStatus(Protocol.SUCCESS);
        } else {
            response.setStatus(Protocol.WORD_NOT_FOUND);
        }
    }

    private void handleUpdateMeaning(Protocol.Message request, Protocol.Message response) {
        String oldMeaning = request.getOldMeaning();
        String newMeaning = request.getNewMeaning();

        if (oldMeaning == null || oldMeaning.trim().isEmpty() ||
                newMeaning == null || newMeaning.trim().isEmpty()) {
            response.setStatus(Protocol.ERROR);
            response.setErrorMessage("Old and new meanings cannot be empty");
            return;
        }

        boolean success = dictionary.updateMeaning(request.getWord(), oldMeaning, newMeaning);

        if (success) {
            response.setStatus(Protocol.SUCCESS);
        } else {
            response.setStatus(Protocol.NOT_FOUND);
        }
    }

    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();

            System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress());
        } catch (IOException e) {
            System.err.println("Error closing client connection: " + e.getMessage());
        }
    }
}