package repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "smoodifydb";
    private static final String USER = "root";
    private static final String PASS = "Sockymahmut63";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL + DB_NAME, USER, PASS);
    }

    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            System.out.println("Database checked/created successfully.");

            stmt.execute("USE " + DB_NAME);
            String createTableSQL = "CREATE TABLE IF NOT EXISTS tracks (" +
                    "track_id VARCHAR(255) PRIMARY KEY, " +
                    "track_name TEXT, " +
                    "artists TEXT, " +
                    "album_name TEXT, " +
                    "track_genre VARCHAR(255), " +
                    "popularity INT, " +
                    "danceability FLOAT, " +
                    "energy FLOAT, " +
                    "valence FLOAT, " +
                    "tempo FLOAT, " +
                    "instrumentalness FLOAT, " +
                    "acousticness FLOAT, " +
                    "mode TINYINT)";
            stmt.executeUpdate(createTableSQL);
            System.out.println("Table 'tracks' checked/created successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
