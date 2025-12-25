import repository.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

public class MoodLogic {

    // Retrieves 10 random songs based on predefined mood parameters (energy, valence, etc.)
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
    // Saves a song string to the favorites table after checking for duplicates
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
    // Helper to check if a song already exists in the favorites table
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
    // Retrieves all songs saved in the favorites table
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
    // Removes a song from the favorites table
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

    // Connects to Open-Meteo API to fetch local weather and map it to a music mood
    public List<String> getRecommendationsByWeather(double lat, double lon) {
        String weatherMood = "chill"; // default mod

        try {
            String urlString = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current_weather=true",
                    lat, lon);

            System.out.println("API Request URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("API Response Code: " + responseCode);
                return List.of("Weather API Error: " + responseCode);
            }

            // Parse response to find the weathercode
            StringBuilder jsonResult = new StringBuilder();
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNext()) {
                jsonResult.append(scanner.nextLine());
            }
            scanner.close();

            String json = jsonResult.toString();

            int currentWeatherIndex = json.indexOf("\"current_weather\"");

            if (currentWeatherIndex != -1) {
                int codeIndex = json.indexOf("\"weathercode\":", currentWeatherIndex);

                if (codeIndex != -1) {
                    // Basic string parsing to extract the code value
                    String sub = json.substring(codeIndex + 14);

                    int commaIndex = sub.indexOf(",");
                    int braceIndex = sub.indexOf("}");

                    int endIndex;
                    if (commaIndex == -1) endIndex = braceIndex;
                    else if (braceIndex == -1) endIndex = commaIndex;
                    else endIndex = Math.min(commaIndex, braceIndex);

                    String codeStr = sub.substring(0, endIndex).trim();

                    int weatherCode = Integer.parseInt(codeStr);
                    weatherMood = mapWeatherToMood(weatherCode);

                    System.out.println("SERVER: Algılanan Hava Kodu: " + weatherCode + " -> Mod: " + weatherMood);
                }
            }

        } catch (Exception e) {
            System.err.println("--- Weather API Error Details ---");
            e.printStackTrace();
            return List.of("Weather Connection Failed. Defaulting to Chill.");
        }

        List<String> songs = getRecommendations(weatherMood);
        songs.add(0, "[Weather Detected: " + weatherMood + " Mode Activated]");
        return songs;
    }

    // Maps WMO Weather interpretation codes to our app's mood categories
    private String mapWeatherToMood(int code) {
        // 0: Clear weather -> Energetic
        // 1, 2, 3: Partly cloudy -> Chill
        // 45, 48: Foggy -> Focus
        // 51, 53, 55, 61, 63, 65: Rain -> Sad
        // 71, 73, 75: Snow -> Chill
        // 95, 96, 99: Storm -> Focus (or Energetic)

        if (code == 0) return "energetic"; // Clear Sky
        if (code >= 1 && code <= 3) return "chill"; // Cloudy
        if (code == 45 || code == 48) return "focus"; // Fog
        if (code >= 51 && code <= 67) return "sad"; // Rain
        if (code >= 80 && code <= 82) return "sad"; // Drizzle
        if (code >= 71 && code <= 77) return "chill"; // Snow
        if (code >= 95) return "focus"; // Storm

        return "chill";
    }

    // Fetches songs based on precise sliders for energy and valence
    public List<String> getCustomRecommendations(double targetEnergy, double targetValence) {
        List<String> songs = new ArrayList<>();

        // Tolerance range: Slightly below and slightly above what the user has selected.
        double range = 0.15;

        String sql = "SELECT track_name, artists FROM tracks " +
                "WHERE energy BETWEEN ? AND ? " +
                "AND valence BETWEEN ? AND ? " +
                "ORDER BY RAND() LIMIT 10";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Adjust the energy range.
            pstmt.setDouble(1, Math.max(0, targetEnergy - range));
            pstmt.setDouble(2, Math.min(1, targetEnergy + range));

            // Adjust the Valence (Happiness) range.
            pstmt.setDouble(3, Math.max(0, targetValence - range));
            pstmt.setDouble(4, Math.min(1, targetValence + range));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("track_name");
                String artist = rs.getString("artists");
                songs.add(title + " - " + artist);
            }

            if (songs.isEmpty()) {
                songs.add("No songs found for this specific combination. Try different values!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            songs.add("Database Error during custom search.");
        }
        return songs;
    }
}