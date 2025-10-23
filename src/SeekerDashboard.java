import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.Timer;

public class SeekerDashboard extends JFrame {
    private int seekerId;

    // nav
    private JButton searchButton, sendRequestButton, trackStatusButton, refreshButton;
    private JPanel cards;
    private final String CARD_SEARCH = "card_search";
    private final String CARD_SEND = "card_send";
    private final String CARD_TRACK = "card_track";

    // components
    private JTextField searchLocationField;
    private JTextField searchBloodField;
    private JTextArea searchResultsArea;

    private JTextField sendHospitalField;
    private JTextArea sendDetailsArea;

    private JTextArea trackArea;
    private JButton logoutButton;
    
    private final Font baseFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font mono = new Font("Consolas", Font.PLAIN, 15);

    // auto refresh for tracking
    private Timer trackRefreshTimer;
    private String currentCard = CARD_SEARCH;

    public SeekerDashboard(int seekerId) {
        this.seekerId = seekerId;
        setTitle("DROP4LIFE — Seeker Dashboard");
        setSize(980, 720);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // root panel - main white window
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Top stacked area: slim info bar + nav bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        // slim top info bar
        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setBackground(new Color(150, 20, 40));
        topInfo.setPreferredSize(new Dimension(0, 36));
        JLabel appLabel = new JLabel("  DROP4LIFE");
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JLabel seekerLabel = new JLabel("Seeker: " + getSeekerName() + "  (ID: " + seekerId + ")", SwingConstants.RIGHT);
        seekerLabel.setForeground(new Color(255, 230, 230));
        seekerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topInfo.add(appLabel, BorderLayout.WEST);
        topInfo.add(seekerLabel, BorderLayout.EAST);
        topPanel.add(topInfo, BorderLayout.NORTH);

        // top nav bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        navBar.setBackground(new Color(220, 20, 60));
        navBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        searchButton = navButton("Search Hospitals");
        searchButton.addActionListener(e -> showCard(CARD_SEARCH));
        navBar.add(searchButton);

        sendRequestButton = navButton("Send Request");
        sendRequestButton.addActionListener(e -> showCard(CARD_SEND));
        navBar.add(sendRequestButton);

        trackStatusButton = navButton("Track Status");
        trackStatusButton.addActionListener(e -> showCard(CARD_TRACK));
        navBar.add(trackStatusButton);

        refreshButton = navButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (CARD_SEARCH.equals(currentCard)) doSearch();
            if (CARD_TRACK.equals(currentCard)) loadTrackStatus();
        });
        navBar.add(refreshButton);
        
        // Logout button
        logoutButton = navButton("Logout");
        logoutButton.setPreferredSize(new Dimension(110, 34));
        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame());
            }
        });
        navBar.add(logoutButton);
        topPanel.add(navBar, BorderLayout.SOUTH);
        root.add(topPanel, BorderLayout.NORTH);

        // cards (main white content)
        cards = new JPanel(new CardLayout());
        cards.setBackground(Color.WHITE);
        cards.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // SEARCH CARD
        JPanel searchCard = new JPanel(new BorderLayout());
        searchCard.setBackground(Color.WHITE);
        JLabel sTitle = new JLabel("Search Hospitals", SwingConstants.LEFT);
        sTitle.setFont(titleFont);
        sTitle.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        searchCard.add(sTitle, BorderLayout.NORTH);

        JPanel searchForm = new JPanel(new GridBagLayout());
        searchForm.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel locLbl = new JLabel("Location:");
        locLbl.setFont(baseFont);
        searchForm.add(locLbl, gbc);
        gbc.gridx = 1;
        searchLocationField = new JTextField();
        searchLocationField.setFont(baseFont);
        searchForm.add(searchLocationField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel btLbl = new JLabel("Blood Type:");
        btLbl.setFont(baseFont);
        searchForm.add(btLbl, gbc);
        gbc.gridx = 1;
        searchBloodField = new JTextField();
        searchBloodField.setFont(baseFont);
        searchForm.add(searchBloodField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton sBtn = new JButton("Search");
        stylePrimary(sBtn);
        sBtn.addActionListener(e -> doSearch());
        searchForm.add(sBtn, gbc);

        searchCard.add(searchForm, BorderLayout.WEST);

        searchResultsArea = new JTextArea();
        searchResultsArea.setFont(mono);
        searchResultsArea.setEditable(false);
        searchResultsArea.setBackground(Color.WHITE);
        searchResultsArea.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        JScrollPane sScroll = new JScrollPane(searchResultsArea);
        searchCard.add(sScroll, BorderLayout.CENTER);

        cards.add(searchCard, CARD_SEARCH);

        // SEND CARD
        JPanel sendCard = new JPanel(new GridBagLayout());
        sendCard.setBackground(Color.WHITE);
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(8,8,8,8);
        g2.fill = GridBagConstraints.HORIZONTAL;

        g2.gridx = 0; g2.gridy = 0;
        JLabel hospLbl = new JLabel("Hospital Name:");
        hospLbl.setFont(baseFont);
        sendCard.add(hospLbl, g2);
        g2.gridx = 1;
        sendHospitalField = new JTextField();
        sendHospitalField.setFont(baseFont);
        sendCard.add(sendHospitalField, g2);

        g2.gridx = 0; g2.gridy = 1;
        JLabel detailsLbl = new JLabel("Details:");
        detailsLbl.setFont(baseFont);
        sendCard.add(detailsLbl, g2);
        g2.gridx = 1;
        sendDetailsArea = new JTextArea(6, 32);
        sendDetailsArea.setFont(baseFont);
        JScrollPane detailsScroll = new JScrollPane(sendDetailsArea);
        sendCard.add(detailsScroll, g2);

        g2.gridx = 0; g2.gridy = 2; g2.gridwidth = 2;
        JButton sendBtn = new JButton("Send Request");
        stylePrimary(sendBtn);
        sendBtn.addActionListener(e -> doSendRequest());
        sendCard.add(sendBtn, g2);

        cards.add(sendCard, CARD_SEND);

        // TRACK CARD
        JPanel trackCard = new JPanel(new BorderLayout());
        trackCard.setBackground(Color.WHITE);
        JLabel tTitle = new JLabel("Your Requests", SwingConstants.LEFT);
        tTitle.setFont(titleFont);
        tTitle.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        trackCard.add(tTitle, BorderLayout.NORTH);

        trackArea = new JTextArea();
        trackArea.setFont(mono);
        trackArea.setEditable(false);
        trackArea.setBackground(Color.WHITE);
        trackArea.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        JScrollPane tScroll = new JScrollPane(trackArea);
        trackCard.add(tScroll, BorderLayout.CENTER);

        cards.add(trackCard, CARD_TRACK);

        root.add(cards, BorderLayout.CENTER);

        setContentPane(root);
        setLocationRelativeTo(null);
        setVisible(true);

        // auto-refresh tracker
        trackRefreshTimer = new Timer(8_000, e -> {
            if (CARD_TRACK.equals(currentCard)) loadTrackStatus();
        });
        trackRefreshTimer.setInitialDelay(0);
        trackRefreshTimer.start();

        // initial card
        showCard(CARD_SEARCH);
    }

    private JButton navButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(178, 34, 34));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(160, 34));
        return b;
    }

    private void stylePrimary(JButton b) {
        b.setBackground(new Color(178, 34, 34));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(180, 36));
    }

    private void showCard(String card) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, card);
        currentCard = card;
        if (CARD_TRACK.equals(card)) loadTrackStatus();
    }

    // Search hospitals and display results in searchResultsArea
    private void doSearch() {
        String location = searchLocationField.getText().trim();
        String blood = searchBloodField.getText().trim();
        if (location.isEmpty() || blood.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter location and blood type.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            String query = "SELECT h.name, bs.units, h.location FROM hospitals h JOIN blood_stocks bs ON h.id = bs.hospital_id " +
                           "WHERE h.location LIKE ? AND bs.blood_type = ? AND bs.units > 0 ORDER BY bs.units DESC";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(query)) {
                pst.setString(1, "%" + location + "%");
                pst.setString(2, blood);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    sb.append("Hospital: ").append(rs.getString("name"))
                      .append(" | Location: ").append(rs.getString("location"))
                      .append(" | Units: ").append(rs.getInt("units"))
                      .append("\n");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                sb.append("Error searching hospitals: ").append(ex.getMessage()).append("\n");
            }
            if (sb.length() == 0) sb.append("No available hospitals found.");
            searchResultsArea.setText(sb.toString());
            showCard(CARD_SEARCH);
        });
    }

    // Send request to hospital
    private void doSendRequest() {
        String hospitalName = sendHospitalField.getText().trim();
        String details = sendDetailsArea.getText().trim();
        if (hospitalName.isEmpty() || details.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter hospital name and details.");
            return;
        }
        int hospitalId = getHospitalId(hospitalName);
        if (hospitalId == -1) {
            JOptionPane.showMessageDialog(this, "Hospital not found.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            String q = "INSERT INTO requests (seeker_id, hospital_id, details, status, request_date) VALUES (?, ?, ?, 'Pending', NOW())";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(q)) {
                pst.setInt(1, seekerId);
                pst.setInt(2, hospitalId);
                pst.setString(3, details);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Request sent.");
                sendHospitalField.setText("");
                sendDetailsArea.setText("");
                // switch to track card to show the new request
                showCard(CARD_TRACK);
                loadTrackStatus();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error sending request: " + ex.getMessage());
            }
        });
    }

    // Load track status for this seeker
    private void loadTrackStatus() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            String q = "SELECT r.id, h.name AS hospital_name, r.status, r.details, r.request_date " +
                       "FROM requests r LEFT JOIN hospitals h ON r.hospital_id = h.id " +
                       "WHERE r.seeker_id = ? ORDER BY r.request_date DESC";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(q)) {
                pst.setInt(1, seekerId);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    sb.append("Request ID: ").append(rs.getInt("id"))
                      .append(" | Hospital: ").append(rs.getString("hospital_name"))
                      .append(" | Status: ").append(rs.getString("status"))
                      .append(" | Date: ").append(rs.getTimestamp("request_date"))
                      .append("\nDetails: ").append(rs.getString("details"))
                      .append("\n\n");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                sb.append("Error loading requests: ").append(ex.getMessage());
            }
            if (sb.length() == 0) sb.append("No requests found.");
            trackArea.setText(sb.toString());
        });
    }

    private int getHospitalId(String name) {
        String q = "SELECT id FROM hospitals WHERE name = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q)) {
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private String getSeekerName() {
        String q = "SELECT name FROM seekers WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q)) {
            pst.setInt(1, seekerId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException ex) {
            // ignore
        }
        return "Unknown";
    }

    // quick main for testing
    public static void main(String[] args) {
        new SeekerDashboard(1);
    }
}