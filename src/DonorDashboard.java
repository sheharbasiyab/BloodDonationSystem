import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DonorDashboard extends JFrame {
    private int donorId;

    // nav
    private JButton profileButton, donationsButton, requestsButton, refreshButton;
    private JPanel cards;
    private final String CARD_PROFILE = "card_profile";
    private final String CARD_DONATIONS = "card_donations";
    private final String CARD_REQUESTS = "card_requests";

    // components
    private JTextArea profileArea;
    private JButton editProfileButton;
    private JTextArea donationsArea;
    private JPanel requestsListPanel; // new requests container panel
    private JButton logoutButton;

    // layout / fonts
    private final Font baseFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font mono = new Font("Consolas", Font.PLAIN, 15);

    // state
    private String currentCard = CARD_PROFILE;

    // auto-refresh for requests view
    private Timer requestsRefreshTimer;

    public DonorDashboard(int donorId) {
        this.donorId = donorId;
        setTitle("DROP4LIFE — Donor Dashboard");
        setSize(980, 720);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ===== Top Info + Nav Bar =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setBackground(new Color(150, 20, 40));
        topInfo.setPreferredSize(new Dimension(0, 36));
        JLabel appLabel = new JLabel("  DROP4LIFE");
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JLabel donorLabel = new JLabel("Donor: " + getDonorName() + "  (ID: " + donorId + ")", SwingConstants.RIGHT);
        donorLabel.setForeground(new Color(255, 230, 230));
        donorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topInfo.add(appLabel, BorderLayout.WEST);
        topInfo.add(donorLabel, BorderLayout.EAST);
        topPanel.add(topInfo, BorderLayout.NORTH);

        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        navBar.setBackground(new Color(220, 20, 60));
        navBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        profileButton = createNavButton("Profile");
        profileButton.addActionListener(e -> showCard(CARD_PROFILE));
        navBar.add(profileButton);

        donationsButton = createNavButton("My Donations");
        donationsButton.addActionListener(e -> showCard(CARD_DONATIONS));
        navBar.add(donationsButton);

        requestsButton = createNavButton("Requests");
        requestsButton.addActionListener(e -> showCard(CARD_REQUESTS));
        navBar.add(requestsButton);

        refreshButton = createNavButton("Refresh");
        refreshButton.addActionListener(e -> {
            if (CARD_PROFILE.equals(currentCard)) loadProfile();
            if (CARD_DONATIONS.equals(currentCard)) loadDonations();
            if (CARD_REQUESTS.equals(currentCard)) loadRequests();
        });
        navBar.add(refreshButton);

        // Logout button
        logoutButton = createNavButton("Logout");
        // make logout a bit smaller than main nav buttons
        logoutButton.setPreferredSize(new Dimension(110, 36));
        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?", "Confirm Logout",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                // close dashboard and return to login
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame());
            }
        });
        navBar.add(logoutButton);

        topPanel.add(navBar, BorderLayout.SOUTH);
        root.add(topPanel, BorderLayout.NORTH);

        // ===== Main Cards =====
        cards = new JPanel(new CardLayout());
        cards.setBackground(Color.WHITE);
        cards.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- Profile Card ----
        JPanel profileCard = new JPanel(new BorderLayout());
        profileCard.setBackground(Color.WHITE);
        JLabel pTitle = new JLabel("Your Profile", SwingConstants.LEFT);
        pTitle.setFont(titleFont);
        pTitle.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        profileCard.add(pTitle, BorderLayout.NORTH);

        profileArea = new JTextArea();
        profileArea.setEditable(false);
        profileArea.setFont(mono);
        profileArea.setBackground(Color.WHITE);
        profileArea.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        profileCard.add(new JScrollPane(profileArea), BorderLayout.CENTER);

        JPanel profileBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        profileBtnBar.setBackground(Color.WHITE);
        editProfileButton = new JButton("Edit Profile");
        stylePrimaryButton(editProfileButton);
        editProfileButton.setPreferredSize(new Dimension(140, 34));
        editProfileButton.addActionListener(e -> showEditProfileDialog());
        profileBtnBar.add(editProfileButton);
        profileCard.add(profileBtnBar, BorderLayout.SOUTH);

        cards.add(profileCard, CARD_PROFILE);

        // ---- Donations Card ----
        JPanel donationsCard = new JPanel(new BorderLayout());
        donationsCard.setBackground(Color.WHITE);
        JLabel dTitle = new JLabel("Donation History", SwingConstants.LEFT);
        dTitle.setFont(titleFont);
        dTitle.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        donationsCard.add(dTitle, BorderLayout.NORTH);

        donationsArea = new JTextArea();
        donationsArea.setEditable(false);
        donationsArea.setFont(mono);
        donationsArea.setBackground(Color.WHITE);
        donationsArea.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        donationsCard.add(new JScrollPane(donationsArea), BorderLayout.CENTER);

        cards.add(donationsCard, CARD_DONATIONS);

        // ---- Requests Card ----
        JPanel reqCard = new JPanel(new BorderLayout());
        reqCard.setBackground(Color.WHITE);
        JLabel rTitle = new JLabel("Requests For You", SwingConstants.LEFT);
        rTitle.setFont(titleFont);
        rTitle.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        reqCard.add(rTitle, BorderLayout.NORTH);

        requestsListPanel = new JPanel();
        requestsListPanel.setLayout(new BoxLayout(requestsListPanel, BoxLayout.Y_AXIS));
        requestsListPanel.setBackground(Color.WHITE);
        JScrollPane reqScroll = new JScrollPane(requestsListPanel);
        reqScroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        reqCard.add(reqScroll, BorderLayout.CENTER);

        cards.add(reqCard, CARD_REQUESTS);

        root.add(cards, BorderLayout.CENTER);
        setContentPane(root);
        setLocationRelativeTo(null);
        setVisible(true);

        // Auto-refresh requests every 10 seconds
        requestsRefreshTimer = new Timer(10_000, e -> {
            if (CARD_REQUESTS.equals(currentCard)) loadRequests();
        });
        requestsRefreshTimer.setInitialDelay(0);
        requestsRefreshTimer.start();

        // Initial load
        showCard(CARD_PROFILE);
        loadProfile();
    }

    private JButton createNavButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(178, 34, 34));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(160, 36));
        return b;
    }

    private void stylePrimaryButton(JButton b) {
        b.setBackground(new Color(178, 34, 34));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(200, 36));
    }

    private void showCard(String card) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, card);
        currentCard = card;
        if (CARD_PROFILE.equals(card)) loadProfile();
        if (CARD_DONATIONS.equals(card)) loadDonations();
        if (CARD_REQUESTS.equals(card)) loadRequests();
    }

    private void loadProfile() {
        SwingUtilities.invokeLater(() -> {
            String q = "SELECT name, age, blood_type, contact_info, location FROM donors WHERE id = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(q)) {
                pst.setInt(1, donorId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Name: ").append(rs.getString("name")).append("\n");
                    sb.append("Age: ").append(rs.getInt("age")).append("\n");
                    sb.append("Blood Type: ").append(rs.getString("blood_type")).append("\n");
                    sb.append("Contact: ").append(rs.getString("contact_info")).append("\n");
                    sb.append("Location: ").append(rs.getString("location")).append("\n");
                    profileArea.setText(sb.toString());
                } else {
                    profileArea.setText("Profile not found.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                profileArea.setText("Error loading profile: " + ex.getMessage());
            }
        });
    }

    private void loadDonations() {
        SwingUtilities.invokeLater(() -> {
            String q = "SELECT dh.id, h.name AS hospital_name, dh.details, dh.donation_date " +
                       "FROM donation_history dh LEFT JOIN hospitals h ON dh.hospital_id = h.id " +
                       "WHERE dh.donor_id = ? ORDER BY dh.donation_date DESC";
            StringBuilder sb = new StringBuilder();
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(q)) {
                pst.setInt(1, donorId);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id"))
                      .append(" | Hospital: ").append(rs.getString("hospital_name"))
                      .append(" | Date: ").append(rs.getTimestamp("donation_date"))
                      .append("\nDetails: ").append(rs.getString("details"))
                      .append("\n\n");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                sb.append("Error loading donations: ").append(ex.getMessage());
            }
            if (sb.length() == 0) sb.append("No donations found.");
            donationsArea.setText(sb.toString());
        });
    }

    private void loadRequests() {
        SwingUtilities.invokeLater(() -> {
            requestsListPanel.removeAll();

            try (Connection con = DBConnection.getConnection()) {
                // discover columns available in donor_requests
                java.util.Set<String> cols = new java.util.HashSet<>();
                try (ResultSet rsCols = con.getMetaData().getColumns(null, null, "donor_requests", null)) {
                    while (rsCols.next()) {
                        cols.add(rsCols.getString("COLUMN_NAME").toLowerCase());
                    }
                }

                // pick a timestamp column if present
                String tsCol = null;
                for (String candidate : new String[] { "request_date", "created_at", "created", "timestamp", "createdon", "createdat" }) {
                    if (cols.contains(candidate)) { tsCol = candidate; break; }
                }
                boolean hasStatus = cols.contains("status");

                // build safe query (only reference columns that exist)
                StringBuilder q = new StringBuilder();
                q.append("SELECT dr.id, h.name AS hospital_name, dr.details");
                if (tsCol != null) q.append(", dr.").append(tsCol).append(" AS request_date");
                else q.append(", NULL AS request_date");
                q.append(" FROM donor_requests dr LEFT JOIN hospitals h ON dr.hospital_id = h.id ");
                q.append("WHERE dr.donor_id = ? ");
                if (hasStatus) q.append("AND (dr.status IS NULL OR LOWER(dr.status) <> 'accepted') ");
                if (tsCol != null) q.append("ORDER BY request_date DESC");
                else q.append("ORDER BY dr.id DESC");

                try (PreparedStatement pst = con.prepareStatement(q.toString())) {
                    pst.setInt(1, donorId);
                    try (ResultSet rs = pst.executeQuery()) {
                        boolean any = false;
                        while (rs.next()) {
                            any = true;
                            int reqId = rs.getInt("id");
                            String hospital = rs.getString("hospital_name");
                            String details = rs.getString("details");
                            java.sql.Timestamp date = rs.getTimestamp("request_date"); // may be null when column missing

                            JPanel card = new JPanel(new BorderLayout(6, 6));
                            card.setBackground(Color.WHITE);
                            card.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(230, 230, 230)),
                                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                            ));

                            JLabel header = new JLabel("Request #" + reqId + " | " + hospital + " | " + (date != null ? date.toString() : ""));
                            header.setFont(baseFont);

                            JTextArea detailArea = new JTextArea(details != null ? details : "");
                            detailArea.setEditable(false);
                            detailArea.setBackground(Color.WHITE);
                            detailArea.setFont(mono);
                            detailArea.setWrapStyleWord(true);
                            detailArea.setLineWrap(true);

                            // build action buttons
                            JButton acceptBtn = new JButton("Accept");
                            stylePrimaryButton(acceptBtn);
                            acceptBtn.setPreferredSize(new Dimension(120, 32));
                            acceptBtn.addActionListener(e -> acceptRequest(reqId, hospital));

                            JButton declineBtn = new JButton("Decline");
                            declineBtn.setBackground(new Color(200, 200, 200));
                            declineBtn.setForeground(Color.BLACK);
                            declineBtn.setFocusPainted(false);
                            declineBtn.setPreferredSize(new Dimension(120, 32));
                            declineBtn.addActionListener(e -> declineRequest(reqId, hospital));

                            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                            bottom.setBackground(Color.WHITE);
                            bottom.add(declineBtn);
                            bottom.add(Box.createHorizontalStrut(8));
                            bottom.add(acceptBtn);

                            card.add(header, BorderLayout.NORTH);
                            card.add(new JScrollPane(detailArea), BorderLayout.CENTER);
                            card.add(bottom, BorderLayout.SOUTH);

                            requestsListPanel.add(card);
                            requestsListPanel.add(Box.createVerticalStrut(10));
                        }

                        if (!any) {
                            JLabel empty = new JLabel("No requests found.");
                            empty.setFont(baseFont);
                            empty.setForeground(Color.GRAY);
                            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                            requestsListPanel.add(empty);
                        }
                    }
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JLabel err = new JLabel("Error loading requests: " + ex.getMessage());
                err.setForeground(Color.RED);
                requestsListPanel.add(err);
            }

            requestsListPanel.revalidate();
            requestsListPanel.repaint();
        });
    }

    private void acceptRequest(int requestId, String hospitalName) {
        // provide a clear reason for ineligibility (underage vs recent donation)
        String reason = getIneligibilityReason();
        if (reason != null) {
            switch (reason) {
                case "underage":
                    JOptionPane.showMessageDialog(this,
                            "Not eligible to donate: donor must be at least 18 years old.",
                            "Not eligible", JOptionPane.INFORMATION_MESSAGE);
                    return;
                case "recent_donation":
                    JOptionPane.showMessageDialog(this,
                            "Not eligible to donate yet. Please wait at least 3 months since your last donation.",
                            "Not eligible", JOptionPane.INFORMATION_MESSAGE);
                    return;
                default:
                    JOptionPane.showMessageDialog(this, "Not eligible to donate.", "Not eligible", JOptionPane.INFORMATION_MESSAGE);
                    return;
            }
        }

        int hospitalId = getHospitalId(hospitalName);
        if (hospitalId == -1) {
            JOptionPane.showMessageDialog(this, "Hospital not found: " + hospitalName);
            return;
        }

        performDonate(hospitalName);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement("UPDATE donor_requests SET status = 'accepted' WHERE id = ?")) {
            pst.setInt(1, requestId);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        JOptionPane.showMessageDialog(this, "Request accepted and donation recorded.");
        loadRequests();
        loadDonations();
    }

    // returns null when eligible, otherwise a reason code: "underage", "recent_donation", "db_error", etc.
    private String getIneligibilityReason() {
        final int MIN_AGE = 18;
        try (Connection con = DBConnection.getConnection()) {
            // check age
            String qAge = "SELECT age FROM donors WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(qAge)) {
                pst.setInt(1, donorId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int age = rs.getInt("age");
                        if (age < MIN_AGE) return "underage";
                    } else {
                        return "db_error";
                    }
                }
            }

            // check last donation date
            String qLast = "SELECT MAX(donation_date) AS last_donation FROM donation_history WHERE donor_id = ?";
            try (PreparedStatement pst2 = con.prepareStatement(qLast)) {
                pst2.setInt(1, donorId);
                try (ResultSet rs2 = pst2.executeQuery()) {
                    if (rs2.next()) {
                        java.sql.Timestamp ts = rs2.getTimestamp("last_donation");
                        if (ts == null) return null; // never donated => eligible by date
                        java.time.LocalDate last = ts.toLocalDateTime().toLocalDate();
                        long months = ChronoUnit.MONTHS.between(last, LocalDate.now());
                        if (months < 3) return "recent_donation";
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return "db_error";
        }
        return null;
    }

    private void declineRequest(int requestId, String hospitalName) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(
                     "UPDATE donor_requests SET status = 'declined' WHERE id = ?")) {
            pst.setInt(1, requestId);
            int n = pst.executeUpdate();
            if (n > 0) {
                JOptionPane.showMessageDialog(this, "You declined the request from " + hospitalName + ".");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to decline request (no rows updated).");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error declining request: " + ex.getMessage());
        } finally {
            loadRequests();
        }
    }

    private void performDonate(String hospitalName) {
        int hospitalId = getHospitalId(hospitalName);
        if (hospitalId == -1) {
            JOptionPane.showMessageDialog(this, "Hospital not found: " + hospitalName);
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(
                     "INSERT INTO donation_history (donor_id, hospital_id, details, donation_date) VALUES (?, ?, ?, NOW())")) {
            pst.setInt(1, donorId);
            pst.setInt(2, hospitalId);
            pst.setString(3, "Donation to " + hospitalName);
            pst.executeUpdate();

            String bt = getDonorBloodType();
            updateBloodStock(hospitalId, bt, 1);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error recording donation: " + ex.getMessage());
        }
    }

    private boolean isEligible() {
        final int MIN_AGE = 18;
        try (Connection con = DBConnection.getConnection()) {
            String qAge = "SELECT age FROM donors WHERE id = ?";
            try (PreparedStatement pst = con.prepareStatement(qAge)) {
                pst.setInt(1, donorId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        int age = rs.getInt("age");
                        if (age < MIN_AGE) return false;
                    } else return false;
                }
            }

            String qLast = "SELECT MAX(donation_date) AS last_donation FROM donation_history WHERE donor_id = ?";
            try (PreparedStatement pst2 = con.prepareStatement(qLast)) {
                pst2.setInt(1, donorId);
                try (ResultSet rs2 = pst2.executeQuery()) {
                    if (rs2.next()) {
                        Timestamp ts = rs2.getTimestamp("last_donation");
                        if (ts == null) return true;
                        LocalDate last = ts.toLocalDateTime().toLocalDate();
                        long months = ChronoUnit.MONTHS.between(last, LocalDate.now());
                        return months >= 3;
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private String getDonorBloodType() {
        String query = "SELECT blood_type FROM donors WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, donorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String bt = rs.getString("blood_type");
                if (bt != null && !bt.trim().isEmpty()) return bt.trim();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "A+";
    }

    private void updateBloodStock(int hospitalId, String bloodType, int units) {
        String updateQuery = "UPDATE blood_stocks SET units = units + ? WHERE hospital_id = ? AND blood_type = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(updateQuery)) {
            pst.setInt(1, units);
            pst.setInt(2, hospitalId);
            pst.setString(3, bloodType);
            int updated = pst.executeUpdate();
            if (updated == 0) {
                String insertQuery = "INSERT INTO blood_stocks (hospital_id, blood_type, units) VALUES (?, ?, ?)";
                try (PreparedStatement insertPst = con.prepareStatement(insertQuery)) {
                    insertPst.setInt(1, hospitalId);
                    insertPst.setString(2, bloodType);
                    insertPst.setInt(3, units);
                    insertPst.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating stock: " + ex.getMessage());
        }
    }

    private int getHospitalId(String name) {
        if (name == null || name.trim().isEmpty()) return -1;
        String query = "SELECT id FROM hospitals WHERE name = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, name.trim());
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    private String getDonorName() {
        String q = "SELECT name FROM donors WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q)) {
            pst.setInt(1, donorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException ignored) {}
        return "Unknown";
    }

    // called by editProfileButton
    private void showEditProfileDialog() {
        String q = "SELECT name, age, blood_type, contact_info, location FROM donors WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q)) {
            pst.setInt(1, donorId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Profile not found.");
                    return;
                }
                String name = rs.getString("name");
                String age = String.valueOf(rs.getInt("age"));
                String bt = rs.getString("blood_type");
                String contact = rs.getString("contact_info");
                String location = rs.getString("location");

                JDialog dlg = new JDialog(this, "Edit Profile", true);
                JPanel p = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(6,6,6,6);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                gbc.gridx = 0; gbc.gridy = 0; p.add(new JLabel("Name:"), gbc);
                gbc.gridx = 1; JTextField nameF = new JTextField(name, 20); p.add(nameF, gbc);

                gbc.gridx = 0; gbc.gridy = 1; p.add(new JLabel("Age:"), gbc);
                gbc.gridx = 1; JTextField ageF = new JTextField(age, 6); p.add(ageF, gbc);

                gbc.gridx = 0; gbc.gridy = 2; p.add(new JLabel("Blood Type:"), gbc);
                gbc.gridx = 1; JTextField btF = new JTextField(bt != null ? bt : "", 8); p.add(btF, gbc);

                gbc.gridx = 0; gbc.gridy = 3; p.add(new JLabel("Contact:"), gbc);
                gbc.gridx = 1; JTextField contactF = new JTextField(contact != null ? contact : "", 20); p.add(contactF, gbc);

                gbc.gridx = 0; gbc.gridy = 4; p.add(new JLabel("Location:"), gbc);
                gbc.gridx = 1; JTextField locF = new JTextField(location != null ? location : "", 20); p.add(locF, gbc);

                JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton save = new JButton("Save");
                JButton cancel = new JButton("Cancel");
                btns.add(cancel); btns.add(save);
                gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; p.add(btns, gbc);

                cancel.addActionListener(a -> dlg.dispose());
                save.addActionListener(a -> {
                    String newName = nameF.getText().trim();
                    String ageTxt = ageF.getText().trim();
                    String newBt = btF.getText().trim();
                    String newContact = contactF.getText().trim();
                    String newLoc = locF.getText().trim();
                    if (newName.isEmpty()) {
                        JOptionPane.showMessageDialog(dlg, "Name required.");
                        return;
                    }
                    int newAge;
                    try { newAge = Integer.parseInt(ageTxt); }
                    catch (NumberFormatException ex) { JOptionPane.showMessageDialog(dlg, "Invalid age."); return; }

                    if (updateProfileInDB(newName, newAge, newBt, newContact, newLoc)) {
                        dlg.dispose();
                        loadProfile();
                        JOptionPane.showMessageDialog(this, "Profile updated.");
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Update failed. See console.");
                    }
                });

                dlg.setContentPane(p);
                dlg.pack();
                dlg.setLocationRelativeTo(this);
                dlg.setVisible(true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading profile: " + ex.getMessage());
        }
    }

    private boolean updateProfileInDB(String name, int age, String bloodType, String contact, String location) {
        String q = "UPDATE donors SET name = ?, age = ?, blood_type = ?, contact_info = ?, location = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(q)) {
            pst.setString(1, name);
            pst.setInt(2, age);
            pst.setString(3, (bloodType == null || bloodType.isEmpty()) ? null : bloodType);
            pst.setString(4, contact);
            pst.setString(5, location);
            pst.setInt(6, donorId);
            return pst.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // quick main for testing (optional)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DonorDashboard(1));
    }
}
