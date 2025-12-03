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
                //Read mood from Client
                String moodRequest = in.readLine();
                System.out.println("Received request: " + moodRequest);

                //Get Recommendations from DB
                List<String> recommendations = logic.getRecommendations(moodRequest);

                //Send response back to Client
                //We send the size first, then the lines
                out.println(recommendations.size());
                for (String song : recommendations) {
                    out.println(song);
                }

            } catch (IOException e) {
                System.out.println("Client disconnected.");
            }
        }
    }
}