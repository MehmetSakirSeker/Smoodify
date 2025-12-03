import repository.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MoodLogic {

    public List<String> getRecommendations(String mood) {
        List<String> songs = new ArrayList<>();
        String sql = "";



        switch (mood.toLowerCase()) {
            case "focus":
                // Focus: Instrumental, low energy
                sql = "SELECT track_name, artists FROM tracks " +
                        "WHERE instrumentalness > 0.5 AND energy < 0.5 AND popularity > 30 " +
                        "ORDER BY RAND() LIMIT 10";
                break;
            case "chill":
                // Chill: High acousticness, moderate energy
                sql = "SELECT track_name, artists FROM tracks " +
                        "WHERE acousticness > 0.6 AND energy < 0.4 AND popularity > 30 " +
                        "ORDER BY RAND() LIMIT 10";
                break;
            case "energetic":
                // Energetic: High energy, high danceability
                sql = "SELECT track_name, artists FROM tracks " +
                        "WHERE energy > 0.7 AND danceability > 0.6 AND popularity > 30 " +
                        "ORDER BY RAND() LIMIT 10";
                break;
            case "sad":
                // Sad: Low valence, low energy
                sql = "SELECT track_name, artists FROM tracks " +
                        "WHERE valence < 0.3 AND energy < 0.4 AND popularity > 30 " +
                        "ORDER BY RAND() LIMIT 10";
                break;
            default:
                return List.of("Invalid Mood Selected");
        }

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String title = rs.getString("track_name");
                String artist = rs.getString("artists");
                songs.add(title + " - " + artist);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            songs.add("Error fetching data from database.");
        }
        return songs;
    }
}