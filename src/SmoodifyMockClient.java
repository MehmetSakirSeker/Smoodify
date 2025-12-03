import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class SmoodifyMockClient {

    public static void main(String[] args) {
        testRequest("Focus");
        System.out.println("\n-------------------\n");
        testRequest("Energetic");
        System.out.println("\n-------------------\n");
        testRequest("Focus");
    }

    public static void testRequest(String mood) {
        String hostname = "localhost";
        int port = 5000;

        System.out.println(">>> CLIENT: Sending mood request: " + mood);

        try (Socket socket = new Socket(hostname, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            out.println(mood);


            String countLine = in.readLine();
            int songCount = Integer.parseInt(countLine);

            System.out.println(">>> CLIENT: Server found " + songCount + " songs for mode: " + mood);

            //Print each song
            for (int i = 0; i < songCount; i++) {
                String song = in.readLine();
                System.out.println("   " + (i + 1) + ". " + song);
            }

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O Error: " + ex.getMessage());
        }
    }
}