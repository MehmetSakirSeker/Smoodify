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

    public String addToFavorites(String songInfo) {
        if (isSongInFavorites(songInfo)) {
            return "This song is already in your favorites!";
        }

        String sql = "INSERT INTO favorites (song_info) VALUES (?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, songInfo);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                return "Song added to favorites successfully!";
            } else {
                return "Failed to add song.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Database Error: " + e.getMessage();
        }
    }

    private boolean isSongInFavorites(String songInfo) {
        String sql = "SELECT COUNT(*) FROM favorites WHERE song_info = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, songInfo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getFavorites() {
        List<String> favs = new ArrayList<>();
        String sql = "SELECT song_info FROM favorites ORDER BY fav_id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                favs.add(rs.getString("song_info"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            favs.add("Error fetching favorites.");
        }
        return favs;
    }

    public boolean removeFromFavorites(String songInfo) {
        String sql = "DELETE FROM favorites WHERE song_info = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, songInfo);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}