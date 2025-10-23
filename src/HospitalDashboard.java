import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.Timer;

public class HospitalDashboard extends JFrame {
    private int hospitalId;

    // nav buttons (moved to top)
    private JButton viewStockButton, addStockButton, viewRequestsButton, requestBloodFromDonorButton, refreshNowButton;
    // card panels and controls
    private JPanel cards; // CardLayout container
    private final String CARD_STOCK = "card_stock";
    private final String CARD_ADD = "card_add";
    private final String CARD_REQUESTS = "card_requests";
    private final String CARD_REQUEST_DONOR = "card_request_donor";

    // components inside cards
    private JTextArea stockArea;
    private JTextArea requestsArea;
    private JTextField addBloodTypeField;
    private JSpinner addUnitsSpinner;
    private JPanel reqDonorCard; // Reference to donor card for refresh
    private JButton logoutButton;
    
    // auto-refresh timer for incoming requests
    private Timer requestsRefreshTimer;

    private Mode currentMode = Mode.NONE;
    private enum Mode { NONE, ADD_STOCK, REQUEST_DONOR, RESPOND_REQUEST }

    // track current visible card
    private String currentCard = CARD_STOCK;

    // shared fonts
    private final Font baseFont = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 18);
    private final Font navFont = new Font("Segoe UI", Font.BOLD, 13);
    private final Font monoLarge = new Font("Consolas", Font.PLAIN, 15);

    public HospitalDashboard(int hospitalId) {
        this.hospitalId = hospitalId;
        setTitle("DROP4LIFE — Hospital Dashboard");
        setSize(980, 720);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // root layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Top panel: info bar + nav bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        // Slim top info bar
        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setBackground(new Color(150, 20, 40));
        topInfo.setPreferredSize(new Dimension(0, 36));
        String hospName = getHospitalName();
        JLabel appLabel = new JLabel("  DROP4LIFE", SwingConstants.LEFT);
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JLabel hospLabel = new JLabel("Hospital: " + hospName + "  (ID: " + hospitalId + ")", SwingConstants.RIGHT);
        hospLabel.setForeground(new Color(255, 230, 230));
        hospLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topInfo.add(appLabel, BorderLayout.WEST);
        topInfo.add(hospLabel, BorderLayout.EAST);
        topPanel.add(topInfo, BorderLayout.NORTH);

        // Navigation bar (tighter spacing so buttons fit on one row)
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        navBar.setBackground(new Color(220, 20, 60));
        navBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        viewStockButton = createNavButton("View Stock");
        viewStockButton.addActionListener(e -> showCard(CARD_STOCK));
        navBar.add(viewStockButton);

        addStockButton = createNavButton("Add Blood Stock");
        addStockButton.addActionListener(e -> showCard(CARD_ADD));
        navBar.add(addStockButton);

        viewRequestsButton = createNavButton("Incoming Requests");
        viewRequestsButton.addActionListener(e -> showCard(CARD_REQUESTS));
        navBar.add(viewRequestsButton);

        requestBloodFromDonorButton = createNavButton("Request Donor");
        requestBloodFromDonorButton.addActionListener(e -> showCard(CARD_REQUEST_DONOR));
        navBar.add(requestBloodFromDonorButton);

        refreshNowButton = createNavButton("Refresh Now");
        refreshNowButton.addActionListener(e -> {
            if (isShowingCard(CARD_REQUESTS)) loadRequests();
            if (isShowingCard(CARD_STOCK)) loadStock();
        });
        navBar.add(refreshNowButton);

        // Logout button
        logoutButton = createNavButton("Logout");
        logoutButton.setPreferredSize(new Dimension(110, 36));
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

        // Main cards area
        cards = new JPanel(new CardLayout());
        cards.setBackground(Color.WHITE);
        cards.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // STOCK CARD
        JPanel stockCard = new JPanel(new BorderLayout());
        stockCard.setBackground(Color.WHITE);
        JLabel stockTitle = new JLabel("Current Blood Stock", SwingConstants.LEFT);
        stockTitle.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        stockTitle.setFont(titleFont);
        stockCard.add(stockTitle, BorderLayout.NORTH);

        stockArea = new JTextArea();
        stockArea.setEditable(false);
        stockArea.setFont(monoLarge);
        stockArea.setBackground(Color.WHITE);
        stockArea.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        JScrollPane stockScroll = new JScrollPane(stockArea);
        stockCard.add(stockScroll, BorderLayout.CENTER);
        cards.add(stockCard, CARD_STOCK);

        // ADD STOCK CARD
        JPanel addCard = new JPanel(new GridBagLayout());
        addCard.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblBt = new JLabel("Blood Type:");
        lblBt.setFont(baseFont);
        gbc.gridx = 0; gbc.gridy = 0;
        addCard.add(lblBt, gbc);
        gbc.gridx = 1;
        addBloodTypeField = new JTextField();
        addBloodTypeField.setFont(baseFont);
        addCard.add(addBloodTypeField, gbc);

        JLabel lblUnits = new JLabel("Units:");
        lblUnits.setFont(baseFont);
        gbc.gridx = 0; gbc.gridy = 1;
        addCard.add(lblUnits, gbc);
        gbc.gridx = 1;
        addUnitsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        ((JSpinner.DefaultEditor)addUnitsSpinner.getEditor()).getTextField().setFont(baseFont);
        addCard.add(addUnitsSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton addSubmit = new JButton("Add Stock");
        stylePrimaryButton(addSubmit);
        addSubmit.addActionListener(e -> {
            String bt = addBloodTypeField.getText().trim().toUpperCase();
            int units = (Integer) addUnitsSpinner.getValue();
            if (bt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter blood type (e.g. A+).");
                return;
            }
            updateBloodStock(bt, units);
            loadStock();
            JOptionPane.showMessageDialog(this, "Added " + units + " units of " + bt + ".");
            addBloodTypeField.setText("");
            addUnitsSpinner.setValue(1);
        });
        addCard.add(addSubmit, gbc);
        cards.add(addCard, CARD_ADD);

        // REQUESTS CARD
        JPanel reqCard = new JPanel(new BorderLayout());
        reqCard.setBackground(Color.WHITE);
        JLabel reqTitle = new JLabel("Incoming Requests", SwingConstants.LEFT);
        reqTitle.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        reqTitle.setFont(titleFont);
        reqCard.add(reqTitle, BorderLayout.NORTH);

        requestsArea = new JTextArea();
        requestsArea.setEditable(false);
        requestsArea.setFont(monoLarge);
        requestsArea.setBackground(Color.WHITE);
        requestsArea.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        JScrollPane reqScroll = new JScrollPane(requestsArea);
        reqCard.add(reqScroll, BorderLayout.CENTER);
        cards.add(reqCard, CARD_REQUESTS);

        // REQUEST DONOR CARD (FINAL FIXED VERSION)
        reqDonorCard = new JPanel(new BorderLayout());
        reqDonorCard.setBackground(Color.WHITE);
        reqDonorCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel reqDonorTitle = new JLabel("Request Blood from Donors", SwingConstants.LEFT);
        reqDonorTitle.setFont(titleFont);
        reqDonorTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        reqDonorCard.add(reqDonorTitle, BorderLayout.NORTH);

        JPanel donorListPanel = new JPanel();
        donorListPanel.setLayout(new BoxLayout(donorListPanel, BoxLayout.Y_AXIS));
        donorListPanel.setBackground(Color.WHITE);
        JScrollPane donorScroll = new JScrollPane(donorListPanel);
        donorScroll.setPreferredSize(new Dimension(900, 500));
        donorScroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        reqDonorCard.add(donorScroll, BorderLayout.CENTER);
        cards.add(reqDonorCard, CARD_REQUEST_DONOR);

        root.add(cards, BorderLayout.CENTER);
        setContentPane(root);
        setLocationRelativeTo(null);
        setVisible(true);

        // Auto-refresh timer
        requestsRefreshTimer = new Timer(10_000, e -> {
            if (isShowingCard(CARD_REQUESTS)) loadRequests();
        });
        requestsRefreshTimer.setInitialDelay(0);
        requestsRefreshTimer.start();

        showCard(CARD_STOCK);
        loadStock();
    }

    private JButton createNavButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(178, 34, 34));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(navFont);
        // reduce preferred size so more buttons fit in single row
        b.setPreferredSize(new Dimension(120, 32));
        return b;
    }
 
    private void stylePrimaryButton(JButton b) {
        b.setBackground(new Color(178, 34, 34));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(120, 32));
    }

    private void showCard(String cardName) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, cardName);
        currentCard = cardName;

        if (CARD_REQUESTS.equals(cardName)) {
            loadRequests();
        } else if (CARD_STOCK.equals(cardName)) {
            loadStock();
        } else if (CARD_REQUEST_DONOR.equals(cardName)) {
            loadAvailableDonors(); 
        }
    }

    private boolean isShowingCard(String cardName) {
        return cardName.equals(currentCard);
    }

    // FINAL FIXED METHOD: Uses ONLY id, name, blood_type columns
    private void loadAvailableDonors() {
        JScrollPane donorScroll = (JScrollPane) reqDonorCard.getComponent(1);
        JPanel donorPanel = (JPanel) donorScroll.getViewport().getView();
        donorPanel.removeAll();
        
        // Query now also fetches donor location
        String query = "SELECT id, name, blood_type, location FROM donors ORDER BY name";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            
            ResultSet rs = pst.executeQuery();
            
            if (!rs.isBeforeFirst()) {
                JLabel noDonors = new JLabel("No donors registered.");
                noDonors.setFont(baseFont);
                noDonors.setForeground(new Color(100, 100, 100));
                noDonors.setHorizontalAlignment(SwingConstants.CENTER);
                donorPanel.add(noDonors);
            }
            
            while (rs.next()) {
                int donorId = rs.getInt("id");
                String name = rs.getString("name");
                String bloodType = rs.getString("blood_type");
                String location = rs.getString("location"); // new
                
                JPanel donorRow = new JPanel(new BorderLayout(10, 8));
                donorRow.setBackground(Color.WHITE);
                donorRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
                donorRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                
                // Donor info (NAME + BLOOD TYPE + LOCATION)
                JPanel infoPanel = new JPanel(new BorderLayout());
                infoPanel.setBackground(Color.WHITE);
                infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
                
                // Use simple HTML to show name/blood type on top and location smaller below
                String locText = (location == null || location.trim().isEmpty()) ? "Location: Unknown" : "Location: " + location;
                JLabel detailsLabel = new JLabel("<html><b>" + escapeHtml(name) + "</b> (" + escapeHtml(bloodType) + ")<br>"
                        + "<span style='font-size:11px;color:#555;'>" + escapeHtml(locText) + "</span></html>");
                detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                
                infoPanel.add(detailsLabel, BorderLayout.CENTER);
                
                // Send Request button
                JButton sendButton = new JButton("Send Request");
                stylePrimaryButton(sendButton);
                sendButton.addActionListener(e -> sendRequestToDonor(donorId, name, bloodType));
                
                donorRow.add(infoPanel, BorderLayout.CENTER);
                donorRow.add(sendButton, BorderLayout.EAST);
                donorPanel.add(donorRow);
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            JLabel errorLabel = new JLabel("Error loading donors: " + ex.getMessage());
            errorLabel.setForeground(Color.RED);
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            donorPanel.add(errorLabel);
        }
        
        donorPanel.revalidate();
        donorPanel.repaint();
    }

    // Send request to donor
    private void sendRequestToDonor(int donorId, String donorName, String bloodType) {
        JTextArea detailsArea = new JTextArea(4, 30);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(detailsArea);
        
        JPanel dialogPanel = new JPanel(new BorderLayout(10, 10));
        dialogPanel.add(new JLabel("Request Details for " + donorName + " (" + bloodType + "):"), BorderLayout.NORTH);
        dialogPanel.add(scroll, BorderLayout.CENTER);
        
        int result = JOptionPane.showConfirmDialog(
            this, dialogPanel, "Send Blood Request", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String details = detailsArea.getText().trim();
            if (details.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter request details.");
                return;
            }
            
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO donor_requests (hospital_id, donor_id, details, request_date) VALUES (?, ?, ?, NOW())")) {
                
                pst.setInt(1, hospitalId);
                pst.setInt(2, donorId);
                pst.setString(3, details);
                pst.executeUpdate();
                
                JOptionPane.showMessageDialog(
                    this, "Request sent successfully to " + donorName + "!"
                );
                
                loadAvailableDonors();
                
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error sending request: " + ex.getMessage());
            }
        }
    }

    private void loadStock() {
        SwingUtilities.invokeLater(() -> {
            String query = "SELECT blood_type, units FROM blood_stocks WHERE hospital_id = ? ORDER BY blood_type";
            StringBuilder sb = new StringBuilder();
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(query)) {
                pst.setInt(1, hospitalId);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    sb.append(String.format("%-4s : %4d units%n", rs.getString("blood_type"), rs.getInt("units")));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                sb.append("Error reading stock: ").append(ex.getMessage());
            }
            stockArea.setText(sb.length() == 0 ? "No stock available." : sb.toString());
        });
    }

    private void loadRequests() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder sb = new StringBuilder();
            String query =
                "SELECT r.id, s.name AS seeker_name, s.blood_type_needed, r.details, " +
                "r.status, r.request_date " +
                "FROM requests r JOIN seekers s ON r.seeker_id = s.id " +
                "WHERE r.hospital_id = ? ORDER BY r.request_date DESC";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(query)) {
                pst.setInt(1, hospitalId);
                ResultSet rs = pst.executeQuery();
                while (rs.next()) {
                    sb.append("Request ID: ").append(rs.getInt("id"))
                      .append(", Seeker: ").append(rs.getString("seeker_name"))
                      .append(", Blood Needed: ").append(rs.getString("blood_type_needed"))
                      .append(", Status: ").append(rs.getString("status"))
                      .append(", Date: ").append(rs.getTimestamp("request_date"))
                      .append("\nDetails: ").append(rs.getString("details"))
                      .append("\n\n");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                sb.append("Error reading requests: ").append(ex.getMessage());
            }
            if (sb.length() == 0) sb.append("No requests found.");
            requestsArea.setText(sb.toString());
        });
    }

    private void updateBloodStock(String bloodType, int units) {
        SwingUtilities.invokeLater(() -> {
            String updateQuery = "UPDATE blood_stocks SET units = units + ? WHERE hospital_id = ? AND blood_type = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(updateQuery)) {
                pst.setInt(1, units);
                pst.setInt(2, hospitalId);
                pst.setString(3, bloodType);
                int updated = pst.executeUpdate();
                if (updated == 0 && units > 0) {
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
        });
    }

    private String getHospitalName() {
        String query = "SELECT name FROM hospitals WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, hospitalId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException ignored) {}
        return "Unknown";
    }

    // small helper to avoid HTML injection if names contain special chars
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}