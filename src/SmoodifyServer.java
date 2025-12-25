import repository.DatabaseManager;

import java.io.*;
import java.net.*;
import java.util.List;

public class SmoodifyServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        // Ensure database and tables are ready before accepting connections
        DatabaseManager.initializeDatabase();

        System.out.println("Smoodify Server is running on port " + PORT + "...");
        MoodLogic logic = new MoodLogic();

        // Listen for incoming client connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                // Spawn a new thread for each client to handle requests concurrently
                new ClientHandler(clientSocket, logic).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class to handle individual client communication logic
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


                // --- Command Routing ---
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

                } else if (request.startsWith("WEATHER:")) {
                    try {
                        // Parses "lat,lon" string from client
                        String[] parts = request.substring(8).split(",");
                        double lat = Double.parseDouble(parts[0]);
                        double lon = Double.parseDouble(parts[1]);

                        responseList = logic.getRecommendationsByWeather(lat, lon);
                    } catch (Exception e) {
                        responseList = List.of("Invalid Coordinates sent.");
                    }

                } else if (request.startsWith("CUSTOM:")) {
                    try {
                        // Parses "energy,valence" values from client sliders
                        String[] parts = request.substring(7).split(",");
                        double energy = Double.parseDouble(parts[0]);
                        double valence = Double.parseDouble(parts[1]);

                        responseList = logic.getCustomRecommendations(energy, valence);
                    } catch (Exception e) {
                        responseList = List.of("Invalid Custom Data.");
                    }

                } else {
                    // Default to standard mood-based search
                    String mood = request.startsWith("MOOD:") ? request.substring(5) : request;
                    responseList = logic.getRecommendations(mood);
                }

                // --- Response Protocol ---
                // First line: Total number of items (for client loop control)
                // Subsequent lines: The actual data
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