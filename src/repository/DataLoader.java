package repository;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataLoader {

    private static final String CSV_FILE_PATH = "datasets/smoodify_cleaned_data.csv";

    public static void main(String[] args) {
        // Initialize DB schema then trigger the CSV import
        DatabaseManager.initializeDatabase();
        loadCsvToDatabase();
    }

    public static void loadCsvToDatabase() {
        String insertSQL = "INSERT INTO tracks (track_id, track_name, artists, album_name, track_genre, " +
                "popularity, danceability, energy, valence, tempo, instrumentalness, acousticness, mode) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH));
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            conn.setAutoCommit(false); // Disable auto-commit for speed better performing

            String line;
            int count = 0;
            br.readLine(); // Skip the header

            System.out.println("Starting data import...");

            while ((line = br.readLine()) != null) {
                String[] data = parseCsvLine(line);

                // Basic validation to ensure row matches expected column count
                if (data.length < 13) {
                    System.out.println("Skipping malformed row: " + line);
                    continue;
                }

                // Map CSV columns to PreparedStatement parameters
                pstmt.setString(1, data[0]); // track_id
                pstmt.setString(2, data[1]); // track_name
                pstmt.setString(3, data[2]); // artists
                pstmt.setString(4, data[3]); // album_name
                pstmt.setString(5, data[4]); // track_genre

                pstmt.setInt(6, parseIntOrZero(data[5]));      // popularity
                pstmt.setFloat(7, parseFloatOrZero(data[6]));  // danceability
                pstmt.setFloat(8, parseFloatOrZero(data[7]));  // energy
                pstmt.setFloat(9, parseFloatOrZero(data[8]));  // valence
                pstmt.setFloat(10, parseFloatOrZero(data[9])); // tempo
                pstmt.setFloat(11, parseFloatOrZero(data[10]));// instrumentalness
                pstmt.setFloat(12, parseFloatOrZero(data[11]));// acousticness
                pstmt.setInt(13, parseIntOrZero(data[12]));    // mode

                pstmt.addBatch();
                count++;

                // Execute final batch and commit the transaction
                if (count % 5000 == 0) {
                    pstmt.executeBatch();
                    System.out.println("Imported " + count + " rows...");
                }
            }

            // Insert remaining rows
            pstmt.executeBatch();
            conn.commit();
            System.out.println("Success! Total rows imported: " + count);

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }


    // Handles CSV splitting while preserving commas located inside quotes
    private static String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    // Helper methods to safely parse numbers and prevent crashes on null/empty strings
    private static int parseIntOrZero(String value) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static float parseFloatOrZero(String value) {
        try { return Float.parseFloat(value.trim()); } catch (NumberFormatException e) { return 0.0f; }
    }
}