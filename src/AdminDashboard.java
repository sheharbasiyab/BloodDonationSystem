import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {
    private JButton manageUsersButton, viewLogsButton;

    public AdminDashboard() {
        setTitle("Admin Dashboard");
        setSize(700, 480);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Top bar (logo / app name) — matches other dashboards
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(150, 20, 40));
        topBar.setPreferredSize(new Dimension(0, 48));
        JLabel appLabel = new JLabel("  DROP4LIFE");
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        topBar.add(appLabel, BorderLayout.WEST);

        // Main button panel (tighter spacing so buttons fit on one row)
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        btnPanel.setBackground(Color.WHITE);

        manageUsersButton = new JButton("Manage Users");
        styleNavButton(manageUsersButton);
        manageUsersButton.addActionListener(e -> manageUsers());
        btnPanel.add(manageUsersButton);

        viewLogsButton = new JButton("View Activity Logs");
        styleNavButton(viewLogsButton);
        viewLogsButton.addActionListener(e -> viewLogs());
        btnPanel.add(viewLogsButton);

        // add components to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(btnPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void manageUsers() {
        String role = JOptionPane.showInputDialog("Enter role to manage (donors/hospitals/seekers):");
        String table = role;
        String action = JOptionPane.showInputDialog("Action (view/add/remove):");
        if (action.equals("view")) {
            String query = "SELECT * FROM " + table;
            try (Connection con = DBConnection.getConnection();
                 Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    sb.append("ID: ").append(rs.getInt("id")).append(", Name: ").append(rs.getString("name")).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } // Add more for add/remove
        // Implement add/remove similarly using PreparedStatements
    }

    private void viewLogs() {
        String query = "SELECT * FROM activity_logs ORDER BY log_date DESC";
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getTimestamp("log_date")).append(": ").append(rs.getString("description")).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // small helper to style nav buttons consistent with other dashboards
    private void styleNavButton(JButton b) {
        b.setBackground(new Color(178, 34, 34)); // button color
        b.setForeground(Color.WHITE);            // text color
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(140, 34));
    }
}