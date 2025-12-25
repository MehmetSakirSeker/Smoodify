import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class SmoodifyClientGUI extends JFrame {

    // Server connection settings
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    // Interface Components
    private JList<String> songList;
    private DefaultListModel<String> listModel;
    private JLabel statusLabel;
    private JLabel subtitleLabel;

    public SmoodifyClientGUI() {
        // Basic window configuration
        setTitle("Smoodify - Mood Based Music Recommender");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Header Section: Title and Mood Subtitle ---
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(new Color(30, 215, 96));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel titleLabel = new JLabel("Smoodify");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        subtitleLabel = new JLabel("Select your current mood:");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setForeground(Color.WHITE);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(subtitleLabel);

        add(headerPanel, BorderLayout.NORTH);

        // --- Center Section: Song List ---
        listModel = new DefaultListModel<>();
        songList = new JList<>(listModel);
        songList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songList.setFixedCellHeight(30);

        // Open YouTube search on double-click
        songList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = songList.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        String rawText = listModel.getElementAt(index);
                        String cleanText = rawText.replace(" - ", "").trim();
                        openInBrowser(cleanText);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(songList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Recommended Songs (Double click to play)"));

        add(scrollPane, BorderLayout.CENTER);

        // --- Right Click Menu (POPUP) ---
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem addToFavItem = new JMenuItem("Add to Favorites");
        popupMenu.add(addToFavItem);
        songList.setComponentPopupMenu(popupMenu);

        addToFavItem.addActionListener(e -> {
            String selectedSong = songList.getSelectedValue();
            if (selectedSong != null) {
                String cleanName = selectedSong.replace(" - ", "").trim();
                sendRequestToServer("ADD_FAV:" + cleanName);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a song first!");
            }
        });

        // --- Bottom Section: Control Buttons and Status Bar ---
        JPanel bottomContainer = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15));
        buttonPanel.setBackground(Color.DARK_GRAY);

        // Mood buttons
        JButton btnFocus = createMoodButton("Focus", new Color(100, 149, 237));
        JButton btnChill = createMoodButton("Chill", new Color(255, 182, 193));
        JButton btnEnergetic = createMoodButton("Energetic", new Color(255, 140, 0));
        JButton btnSad = createMoodButton("Sad", new Color(70, 130, 180));

        // Custom Slider Dialog Button
        JButton btnCustom = new JButton("Custom Mix");
        btnCustom.setFont(new Font("Arial", Font.BOLD, 14));
        btnCustom.setBackground(new Color(138, 43, 226));
        btnCustom.setForeground(Color.WHITE);
        btnCustom.setFocusPainted(false);
        btnCustom.addActionListener(e -> showCustomMoodDialog());

        // Favorites  actions
        JButton btnFavorites = new JButton("My Favorites");
        btnFavorites.setFont(new Font("Arial", Font.BOLD, 14));
        btnFavorites.setBackground(new Color(255, 215, 0)); // Gold
        btnFavorites.setForeground(Color.WHITE);
        btnFavorites.setFocusPainted(false);
        btnFavorites.addActionListener(e -> sendRequestToServer("GET_FAVS"));

        // Weather-based recommendation button
        JButton btnWeather = new JButton("Weather Vibe");
        btnWeather.setFont(new Font("Arial", Font.BOLD, 14));
        btnWeather.setBackground(new Color(0, 191, 255));
        btnWeather.setForeground(Color.WHITE);
        btnWeather.setFocusPainted(false);
        btnWeather.addActionListener(e -> {
            new Thread(() -> {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Detecting location..."));
                String location = getCurrentLocation();

                if (location != null) {
                    sendRequestToServer("WEATHER:" + location);
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Location detection failed.");
                        JOptionPane.showMessageDialog(this, "Could not detect location.\nCheck your internet connection.");
                    });
                }
            }).start();
        });

        JButton btnExport = new JButton("Export Playlist");
        btnExport.setFont(new Font("Arial", Font.BOLD, 14));
        btnExport.setBackground(new Color(148, 0, 211));
        btnExport.setForeground(Color.WHITE);
        btnExport.setFocusPainted(false);
        btnExport.addActionListener(e -> exportFavoritesToFile());

        // Add Buttons to buttonPanel
        buttonPanel.add(btnFocus);
        buttonPanel.add(btnChill);
        buttonPanel.add(btnEnergetic);
        buttonPanel.add(btnSad);
        buttonPanel.add(btnCustom);
        buttonPanel.add(btnWeather);
        buttonPanel.add(btnFavorites);
        buttonPanel.add(btnExport);

        // -- Status Bar --
        statusLabel = new JLabel("Ready to connect...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        bottomContainer.add(buttonPanel, BorderLayout.CENTER);
        bottomContainer.add(statusLabel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);
    }

    // Displays a dialog with sliders to manually adjust energy and happiness
    private void showCustomMoodDialog() {
        JDialog dialog = new JDialog(this, "Create Your Mood", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 1));

        // -- Energy Slider --
        JPanel energyPanel = new JPanel(new BorderLayout());
        energyPanel.setBorder(BorderFactory.createTitledBorder("Energy Level (Low to High)"));
        JSlider energySlider = new JSlider(0, 100, 50);
        energySlider.setMajorTickSpacing(20);
        energySlider.setMinorTickSpacing(5);
        energySlider.setPaintTicks(true);
        energySlider.setPaintLabels(true);
        energyPanel.add(energySlider);

        // -- Valence (Happiness) Slider --
        JPanel valencePanel = new JPanel(new BorderLayout());
        valencePanel.setBorder(BorderFactory.createTitledBorder("Vibe (Sad to Happy)"));
        JSlider valenceSlider = new JSlider(0, 100, 50);
        valenceSlider.setMajorTickSpacing(20);
        valenceSlider.setMinorTickSpacing(5);
        valenceSlider.setPaintTicks(true);
        valenceSlider.setPaintLabels(true);
        valencePanel.add(valenceSlider);

        // -- Confirmation Button Panel --
        JPanel actionPanel = new JPanel(new FlowLayout());
        JButton btnApply = new JButton("Find Songs");
        btnApply.setFont(new Font("Arial", Font.BOLD, 16));
        btnApply.setBackground(new Color(30, 215, 96));
        btnApply.setForeground(Color.WHITE);

        btnApply.addActionListener(e -> {
            // Convert a value between 0-100 to a value between 0.0-1.0.
            double energyVal = energySlider.getValue() / 100.0;
            double valenceVal = valenceSlider.getValue() / 100.0;

            dialog.dispose(); // Close the window after clicking the find songs button on the slider panel

            // UI update and server request
            subtitleLabel.setText(String.format("Custom Mix: Energy %.1f | Vibe %.1f", energyVal, valenceVal));
            sendRequestToServer("CUSTOM:" + energyVal + "," + valenceVal);
        });

        actionPanel.add(btnApply);

        dialog.add(energyPanel);
        dialog.add(valencePanel);
        dialog.add(actionPanel);

        dialog.setVisible(true);
    }

    // Encodes query and opens the default browser to YouTube
    private void openInBrowser(String query) {
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://www.youtube.com/results?search_query=" + encodedQuery;
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not open browser.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Uses ip-api.com to get latitude and longitude based on the user's IP
    private String getCurrentLocation() {
        try {
            URL url = new URL("http://ip-api.com/json/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);

            if (conn.getResponseCode() != 200) return null;

            StringBuilder jsonResult = new StringBuilder();
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNext()) {
                jsonResult.append(scanner.nextLine());
            }
            scanner.close();

            String json = jsonResult.toString();
            double lat = extractJsonValue(json, "\"lat\":");
            double lon = extractJsonValue(json, "\"lon\":");

            return lat + "," + lon;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fetches favorites from server and saves them to a local .txt file
    private double extractJsonValue(String json, String key) {
        int startIndex = json.indexOf(key);
        if (startIndex == -1) return 0.0;

        startIndex += key.length();
        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1) endIndex = json.indexOf("}", startIndex);

        String valueStr = json.substring(startIndex, endIndex).trim();
        return Double.parseDouble(valueStr);
    }

    private void exportFavoritesToFile() {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Exporting playlist..."));

            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("GET_FAVS");

                String countLine = in.readLine();
                if (countLine == null) return;
                int responseCount = Integer.parseInt(countLine);

                File folder = new File("playlists");
                if (!folder.exists()) {
                    folder.mkdir();
                }

                File file = new File(folder, "favorites.txt");

                try (PrintWriter fileWriter = new PrintWriter(new FileWriter(file))) {
                    for (int i = 0; i < responseCount; i++) {
                        String song = in.readLine();
                        fileWriter.println(song);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Playlist exported successfully!");
                    JOptionPane.showMessageDialog(this,
                            "Playlist saved to:\n" + file.getAbsolutePath(),
                            "Export Success",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Export failed!");
                    JOptionPane.showMessageDialog(this, "Error exporting playlist: " + ex.getMessage());
                });
                ex.printStackTrace();
            }
        }).start();
    }

    private JButton createMoodButton(String mood, Color color) {
        JButton btn = new JButton(mood);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> sendRequestToServer("MOOD:" + mood));
        return btn;
    }

    // Handles communication with the Server in a background thread to keep UI responsive
    private void sendRequestToServer(String command) {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Processing: " + command + "...");

                if (!command.startsWith("ADD_FAV:") && !command.startsWith("REMOVE_FAV:")) {
                    listModel.clear();

                    if (command.equals("GET_FAVS")) {
                        subtitleLabel.setText("Your Favorite Songs");
                    } else if (command.startsWith("MOOD:")) {
                        String moodName = command.substring(5);
                        subtitleLabel.setText("Selected Mood: " + moodName);
                    } else if (command.startsWith("WEATHER:")) {
                        subtitleLabel.setText("Analyzing Weather...");
                    }
                }
            });

            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(command);

                String countLine = in.readLine();
                if (countLine == null) return;

                AtomicInteger responseCount = new AtomicInteger(Integer.parseInt(countLine));
                ArrayList<String> responses = new ArrayList<>();
                for (int i = 0; i < responseCount.get(); i++) {
                    responses.add(in.readLine());
                }

                // Update UI components
                SwingUtilities.invokeLater(() -> {
                    if (command.startsWith("ADD_FAV:")) {
                        String msg = responses.isEmpty() ? "No response" : responses.get(0);
                        JOptionPane.showMessageDialog(this, msg);
                        statusLabel.setText(msg);

                    } else if (command.startsWith("REMOVE_FAV:")) {
                        String msg = responses.isEmpty() ? "No response" : responses.get(0);
                        statusLabel.setText(msg);
                        sendRequestToServer("GET_FAVS");

                    } else {
                        if (command.startsWith("WEATHER:") && !responses.isEmpty() && responses.get(0).startsWith("[Weather Detected:")) {
                            String infoLine = responses.get(0);
                            String detectedMood = infoLine.substring(19, infoLine.indexOf(" Mode"));
                            String coordinates = command.substring(8);
                            subtitleLabel.setText("Selected Mood: " + detectedMood + " | Location: " + coordinates);
                            responses.remove(0);
                            responseCount.getAndDecrement();
                        }

                        for (String item : responses) {
                            listModel.addElement(" - " + item);
                        }

                        statusLabel.setText("Loaded " + responseCount + " songs.");

                        boolean isFavoritesView = command.equals("GET_FAVS");
                        updateContextMenu(isFavoritesView);
                    }
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Connection Error!");
                });
                ex.printStackTrace();
            }
        }).start();
    }

    // Switches popup menu items between "Add" and "Remove" based on the current view
    private void updateContextMenu(boolean isFavoritesView) {
        JPopupMenu popupMenu = new JPopupMenu();

        if (isFavoritesView) {
            JMenuItem removeItem = new JMenuItem("Remove from Favorites");
            removeItem.addActionListener(e -> {
                String selectedSong = songList.getSelectedValue();
                if (selectedSong != null) {
                    String cleanName = selectedSong.replace(" - ", "").trim();
                    sendRequestToServer("REMOVE_FAV:" + cleanName);
                }
            });
            popupMenu.add(removeItem);

        } else {
            JMenuItem addItem = new JMenuItem("Add to Favorites");
            addItem.addActionListener(e -> {
                String selectedSong = songList.getSelectedValue();
                if (selectedSong != null) {
                    String cleanName = selectedSong.replace(" - ", "").trim();
                    sendRequestToServer("ADD_FAV:" + cleanName);
                }
            });
            popupMenu.add(addItem);
        }

        songList.setComponentPopupMenu(popupMenu);
    }

    public static void main(String[] args) {
        // UIManager ile başlayan satırları sildik.
        // Artık renkler %100 görünecek.

        SwingUtilities.invokeLater(() -> {
            new SmoodifyClientGUI().setVisible(true);
        });
    }
}