import javax.swing.*;
import java.awt.*;
import java.io.*; // Dosya işlemleri için gerekli
import java.net.Socket;
import java.util.ArrayList;

public class SmoodifyClientGUI extends JFrame {

    // Sunucu Ayarları
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    // Arayüz Bileşenleri
    private JList<String> songList;
    private DefaultListModel<String> listModel;
    private JLabel statusLabel;
    private JLabel subtitleLabel;

    public SmoodifyClientGUI() {
        setTitle("Smoodify - Mood Based Music Recommender");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- ÜST PANEL ---
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

        // --- ORTA PANEL (LİSTE) ---
        listModel = new DefaultListModel<>();
        songList = new JList<>(listModel);
        songList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songList.setFixedCellHeight(30);

        JScrollPane scrollPane = new JScrollPane(songList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Recommended Songs"));

        add(scrollPane, BorderLayout.CENTER);

        // --- SAĞ TIK MENÜSÜ (POPUP) ---
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

        // --- ALT PANEL ---
        JPanel bottomContainer = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15));
        buttonPanel.setBackground(Color.DARK_GRAY);

        JButton btnFocus = createMoodButton("Focus", new Color(100, 149, 237));
        JButton btnChill = createMoodButton("Chill", new Color(255, 182, 193));
        JButton btnEnergetic = createMoodButton("Energetic", new Color(255, 140, 0));
        JButton btnSad = createMoodButton("Sad", new Color(70, 130, 180));

        JButton btnFavorites = new JButton("My Favorites");
        btnFavorites.setFont(new Font("Arial", Font.BOLD, 14));
        btnFavorites.setBackground(new Color(255, 215, 0)); // Altın Sarısı
        btnFavorites.setForeground(Color.WHITE);
        btnFavorites.setFocusPainted(false);
        btnFavorites.addActionListener(e -> sendRequestToServer("GET_FAVS"));

        // --- EXPORT ---
        JButton btnExport = new JButton("Export Playlist");
        btnExport.setFont(new Font("Arial", Font.BOLD, 14));
        btnExport.setBackground(new Color(148, 0, 211)); // Mor renk
        btnExport.setForeground(Color.WHITE);
        btnExport.setFocusPainted(false);

        btnExport.addActionListener(e -> exportFavoritesToFile());

        buttonPanel.add(btnFocus);
        buttonPanel.add(btnChill);
        buttonPanel.add(btnEnergetic);
        buttonPanel.add(btnSad);
        buttonPanel.add(btnFavorites);
        buttonPanel.add(btnExport);

        statusLabel = new JLabel("Ready to connect...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        bottomContainer.add(buttonPanel, BorderLayout.CENTER);
        bottomContainer.add(statusLabel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);
    }

    // --- Favorileri Dosyaya Aktar ---
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

    private void sendRequestToServer(String command) {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Processing: " + command + "...");

                if (!command.startsWith("ADD_FAV:") && !command.startsWith("REMOVE_FAV:")) {
                    listModel.clear();

                    if (command.equals("GET_FAVS")) {
                        subtitleLabel.setText("Your Favorite Songs");
                    } else {
                        String moodName = command.startsWith("MOOD:") ? command.substring(5) : command;
                        subtitleLabel.setText("Selected Mood: " + moodName);
                    }
                }
            });

            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(command);

                String countLine = in.readLine();
                if (countLine == null) return;

                int responseCount = Integer.parseInt(countLine);
                ArrayList<String> responses = new ArrayList<>();
                for (int i = 0; i < responseCount; i++) {
                    responses.add(in.readLine());
                }

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
                        for (String item : responses) {
                            listModel.addElement(" - " + item);
                        }
                        statusLabel.setText("Listed " + responseCount + " items.");

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
        SwingUtilities.invokeLater(() -> {
            new SmoodifyClientGUI().setVisible(true);
        });
    }
}