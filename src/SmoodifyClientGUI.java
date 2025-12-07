import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
    private JLabel subtitleLabel; // Bunu sınıf seviyesine taşıdık ki değiştirebilelim

    public SmoodifyClientGUI() {
        setTitle("Smoodify - Mood Based Music Recommender");
        setSize(700, 500); // Genişliği biraz artırdım şarkı isimleri sığsın diye
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

        // Dinamik değişecek olan alt başlık
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

        // Satır aralığını biraz açalım daha şık dursun
        songList.setFixedCellHeight(30);

        JScrollPane scrollPane = new JScrollPane(songList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Recommended Songs"));

        add(scrollPane, BorderLayout.CENTER);

        // --- ALT PANEL ---
        JPanel bottomContainer = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.DARK_GRAY);

        JButton btnFocus = createMoodButton("Focus", new Color(100, 149, 237));
        JButton btnChill = createMoodButton("Chill", new Color(255, 182, 193));
        JButton btnEnergetic = createMoodButton("Energetic", new Color(255, 140, 0));
        JButton btnSad = createMoodButton("Sad", new Color(70, 130, 180));

        buttonPanel.add(btnFocus);
        buttonPanel.add(btnChill);
        buttonPanel.add(btnEnergetic);
        buttonPanel.add(btnSad);

        statusLabel = new JLabel("Ready to connect...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);

        bottomContainer.add(buttonPanel, BorderLayout.CENTER);
        bottomContainer.add(statusLabel, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);
    }

    private JButton createMoodButton(String mood, Color color) {
        JButton btn = new JButton(mood);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> fetchSongsFromServer(mood));
        return btn;
    }

    private void fetchSongsFromServer(String mood) {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Connecting to server for " + mood + " songs...");

                // İSTEK 1: Başlığı güncelle (Select your current mood: Focus)
                subtitleLabel.setText("Selected Mood: " + mood);

                listModel.clear();
            });

            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(mood);

                String countLine = in.readLine();
                if (countLine == null) return;

                int songCount = Integer.parseInt(countLine);
                ArrayList<String> tempSongs = new ArrayList<>();
                for (int i = 0; i < songCount; i++) {
                    String song = in.readLine();
                    tempSongs.add(song);
                }

                SwingUtilities.invokeLater(() -> {
                    for (String song : tempSongs) {
                        // İSTEK 2: Başındaki kareyi (emojiyi) kaldırdık.
                        // Sadece temiz bir tire işareti koydum.
                        listModel.addElement(" - " + song);
                    }
                    statusLabel.setText("Found " + songCount + " songs for " + mood + ".");
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: Could not connect to server!");
                    subtitleLabel.setText("Connection Error!"); // Hata olursa başlıkta da belli edelim
                });
                ex.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SmoodifyClientGUI().setVisible(true);
        });
    }
}