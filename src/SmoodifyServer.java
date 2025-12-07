import repository.DatabaseManager;

import java.io.*;
import java.net.*;
import java.util.List;

public class SmoodifyServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        System.out.println("Smoodify Server is running on port " + PORT + "...");
        MoodLogic logic = new MoodLogic();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                new ClientHandler(clientSocket, logic).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private MoodLogic logic;

        public ClientHandler(Socket socket, MoodLogic logic) {
            this.socket = socket;
            this.logic = logic;
        }

        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String request = in.readLine();
                System.out.println("Received request: " + request);

                if (request == null) return;

                List<String> responseList;


                if (request.startsWith("ADD_FAV:")) {
                    String songInfo = request.substring(8); //
                    String resultMessage = logic.addToFavorites(songInfo);
                    responseList = List.of(resultMessage);

                } else if (request.startsWith("REMOVE_FAV:")) {
                    String songInfo = request.substring(11);
                    boolean success = logic.removeFromFavorites(songInfo);
                    responseList = List.of(success ? "Song removed from favorites." : "Failed to remove.");

                } else if (request.equals("GET_FAVS")) {
                    responseList = logic.getFavorites();

                } else {
                    String mood = request.startsWith("MOOD:") ? request.substring(5) : request;
                    responseList = logic.getRecommendations(mood);
                }

                out.println(responseList.size());
                for (String line : responseList) {
                    out.println(line);
                }

            } catch (IOException e) {
                System.out.println("Client disconnected.");
            }
        }
    }
}