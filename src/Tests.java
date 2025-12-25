import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import repository.DatabaseManager;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Tests {

    private MoodLogic moodLogic;

    @BeforeAll
    void setup() {
        // Ensure the database schema and tables are ready before starting tests
        DatabaseManager.initializeDatabase();
        moodLogic = new MoodLogic();
    }

    @Nested
    @DisplayName("Recommendation Tests")
    class RecommendationTests {

        @ParameterizedTest(name = "Mood: {0}")
        @ValueSource(strings = {"focus", "chill", "energetic", "sad"})
        @DisplayName("Should retrieve data for all primary mood categories")
        void testAllValidMoods(String mood) {
            List<String> results = moodLogic.getRecommendations(mood);

            assertNotNull(results, "The result list should not be null for " + mood);

            // Validate that the SQL LIMIT 10 is respected
            assertTrue(results.size() <= 10, "Song count should not exceed 10");

            // Verify the string concatenation format defined in MoodLogic.java
            if (!results.isEmpty()) {
                assertTrue(results.get(0).contains(" - "), "Song format should be 'Title - Artist'");
            }
        }

        @Test
        @DisplayName("Should return an error message for undefined moods")
        void testInvalidMoodResponse() {
            List<String> results = moodLogic.getRecommendations("unknown_genre_123");
            assertEquals(1, results.size());
            assertEquals("Invalid Mood Selected", results.get(0));
        }
    }

    @Nested
    @DisplayName("Favorites Tests")
    class FavoriteTests {

        private final String MOCK_SONG = "JUnit Test Song - Developer";

        @Test
        @DisplayName("Adding, Duplicate Checking, and Removing a Favorite")
        void testFavoriteLifecycle() {
            // Step 1: Clean start (ensure the mock song doesn't exist)
            moodLogic.removeFromFavorites(MOCK_SONG);

            // Step 2: Test successful insertion
            String firstAdd = moodLogic.addToFavorites(MOCK_SONG);
            assertEquals("Song added to favorites successfully!", firstAdd);

            // Step 3: Test Duplicate Prevention (Logic check for isSongInFavorites)
            String secondAdd = moodLogic.addToFavorites(MOCK_SONG);
            assertEquals("This song is already in your favorites!", secondAdd);

            // Step 4: Verify retrieval
            List<String> favorites = moodLogic.getFavorites();
            assertTrue(favorites.contains(MOCK_SONG), "The song should appear in the favorites list");

            // Step 5: Test successful deletion
            boolean removed = moodLogic.removeFromFavorites(MOCK_SONG);
            assertTrue(removed, "Existing song should be removable");

            // Step 6: Verify deletion impact
            assertFalse(moodLogic.getFavorites().contains(MOCK_SONG), "The song should be gone from the list");
        }

        @Test
        @DisplayName("Attempting to remove a non-existent song")
        void testRemoveNonExistentSong() {
            boolean result = moodLogic.removeFromFavorites("Non-Existent-999");
            assertFalse(result, "Should return false when the song does not exist in the database");
        }
    }

    @Nested
    @DisplayName("Custom Mix & Boundary Value Tests")
    class CustomMixTests {

        @Test
        @DisplayName("Should handle boundary slider values (0.0 and 1.0)")
        void testCustomBoundaryValues() {
            // Minimum bounds
            assertDoesNotThrow(() -> moodLogic.getCustomRecommendations(0.0, 0.0));

            // Maximum bounds
            List<String> results = moodLogic.getCustomRecommendations(1.0, 1.0);
            assertNotNull(results);
        }

        @Test
        @DisplayName("Validate custom search result messages")
        void testCustomNoResultHandling() {
            // Use extreme values that are unlikely to return matches in most datasets
            List<String> results = moodLogic.getCustomRecommendations(0.99, 0.01);
            if (results.size() == 1) {
                String msg = results.get(0);
                // Should either find a song or provide the "No songs found" informative message
                assertTrue(msg.contains("No songs found") || msg.contains(" - "),
                        "System should either return a formatted song or an informative 'No results' message");
            }
        }
    }

    @Nested
    @DisplayName("Weather Integration & API Tests")
    class WeatherIntegrationTests {

        @Test
        @DisplayName("Should validate API response and list structure for a location")
        void testWeatherFlow() {
            // Coordinates for Izmir, Turkey
            double lat = 38.4192;
            double lon = 27.1287;

            List<String> results = moodLogic.getRecommendationsByWeather(lat, lon);

            assertNotNull(results, "Weather results should not be null");
            assertFalse(results.isEmpty(), "Result list should not be empty");

            // Verify the protocol: The first line must be the detected mood info for the GUI
            assertTrue(results.get(0).startsWith("[Weather Detected:"),
                    "Header line is incorrect or missing: " + results.get(0));
        }

        @Test
        @DisplayName("Should handle invalid coordinates gracefully")
        void testInvalidLocationHandling() {
            // Invalid coordinates fall back to 'chill' or return an error list
            List<String> results = moodLogic.getRecommendationsByWeather(999.0, 999.0);
            assertNotNull(results);
            assertTrue(results.size() >= 1, "Even on failure, the system must return at least one message line");
        }
    }
}