import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginFrame extends JFrame implements ActionListener {
    private JComboBox<String> roleCombo;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton;
    private JLabel registerHintLabel; // message shown below login when user not registered

    public LoginFrame() {
        setTitle("DROP4LIFE - Login");
        setSize(620, 420);               // bigger approximate size
        setMinimumSize(new Dimension(620, 420));
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header (large app name)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(178, 34, 34)); // dark red
        header.setBorder(new EmptyBorder(18, 20, 18, 20));
        JLabel title = new JLabel("DROP4LIFE", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        header.add(title, BorderLayout.CENTER);
        // small subtitle
        JLabel subtitle = new JLabel("Donate. Save lives.", SwingConstants.CENTER);
        subtitle.setForeground(Color.WHITE);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Form panel with padding
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 40, 20, 40));
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        formPanel.add(roleLabel, gbc);

        gbc.gridx = 1;
        roleCombo = new JComboBox<>(new String[]{"Donor", "Hospital", "Seeker", "Admin"});
        roleCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        formPanel.add(roleCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel userLabel = new JLabel("Username/ID:");
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField();
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        formPanel.add(usernameField, gbc);

        // show registration hint when user clicks into the username field (or focuses it)
        usernameField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showRegisterHint();
            }
        });
        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                showRegisterHint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                checkRegistrationHint(); // keep existing behavior on blur
            }
        });

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        formPanel.add(passwordField, gbc);

        // register hint label (hidden by default) shown below login fields
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        registerHintLabel = new JLabel("", SwingConstants.CENTER);
        registerHintLabel.setForeground(new Color(0, 102, 204)); // blue link-like color
        registerHintLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerHintLabel.setVisible(false);
        // click opens register frame for currently selected role
        registerHintLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String role = (String) roleCombo.getSelectedItem();
                if (role == null || role.equals("Admin")) return;
                new RegisterFrame(role);
            }
        });
        formPanel.add(registerHintLabel, gbc);

        // Buttons row
        gbc.gridy++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        buttons.setBackground(Color.WHITE);

        loginButton = new JButton("Login");
        stylePrimaryButton(loginButton);
        loginButton.addActionListener(this);
        buttons.add(loginButton);

        registerButton = new JButton("Register (Donor/Seeker/Hospital)");
        stylePrimaryButton(registerButton);
        registerButton.addActionListener(this);
        buttons.add(registerButton);

        formPanel.add(buttons, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Footer / info
        JLabel footer = new JLabel("Developed for DROP4LIFE", SwingConstants.CENTER);
        footer.setBorder(new EmptyBorder(8, 0, 12, 0));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setForeground(new Color(120, 120, 120));
        add(footer, BorderLayout.SOUTH);

        // listeners: check registration status when username field loses focus or role changes
        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                checkRegistrationHint();
            }
        });
        roleCombo.addActionListener(e -> checkRegistrationHint());

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void stylePrimaryButton(JButton b) {
        b.setBackground(new Color(178, 34, 34)); // red
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(240, 36));
    }

    // show register hint below login if username not found for selected role (excluding Admin)
    private void checkRegistrationHint() {
        String role = (String) roleCombo.getSelectedItem();
        if (role == null || role.equals("Admin")) {
            registerHintLabel.setVisible(false);
            return;
        }
        String username = usernameField.getText();
        if (username == null || username.trim().isEmpty()) {
            registerHintLabel.setVisible(false);
            return;
        }
        boolean exists = isUserRegistered(role, username.trim());
        if (!exists) {
            registerHintLabel.setText("<html><u>Not registered? Click here to create a " + role + " account.</u></html>");
            registerHintLabel.setVisible(true);
        } else {
            registerHintLabel.setVisible(false);
        }
    }

    private boolean isUserRegistered(String role, String username) {
        String table = getTableForRole(role);
        if (table == null) return false;
        String column = getUsernameColumn(role);
        String query = "SELECT 1 FROM " + table + " WHERE " + column + " = ? LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loginButton) {
            String role = (String) roleCombo.getSelectedItem();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (authenticate(role, username, password)) {
                dispose();
                openDashboard(role, username);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials!");
                checkRegistrationHint(); // update hint after failed login
            }
        } else if (e.getSource() == registerButton) {
            String role = (String) roleCombo.getSelectedItem();
            if (role.equals("Admin")) {
                JOptionPane.showMessageDialog(this, "Admins cannot register here.");
                return;
            }
            new RegisterFrame(role);
        }
    }

    private boolean authenticate(String role, String username, String password) {
        String table = getTableForRole(role);
        if (table == null) return false;

        String query = "SELECT * FROM " + table + " WHERE " + getUsernameColumn(role) + " = ? AND password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private String getTableForRole(String role) {
        switch (role) {
            case "Admin": return "admins";
            case "Donor": return "donors";
            case "Hospital": return "hospitals";
            case "Seeker": return "seekers";
            default: return null;
        }
    }

    private String getUsernameColumn(String role) {
        return role.equals("Admin") ? "admin_id" : "name";
    }

    private void openDashboard(String role, String username) {
        switch (role) {
            case "Admin": new AdminDashboard(); break;
            case "Donor": new DonorDashboard(getUserId("donors", username)); break;
            case "Hospital": new HospitalDashboard(getUserId("hospitals", username)); break;
            case "Seeker": new SeekerDashboard(getUserId("seekers", username)); break;
        }
    }

    private int getUserId(String table, String username) {
        String query = "SELECT id FROM " + table + " WHERE name = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    // add this helper method near other private methods in the same class
    private void showRegisterHint() {
        String role = (String) roleCombo.getSelectedItem();
        if (role == null || role.equals("Admin")) {
            registerHintLabel.setVisible(false);
            return;
        }
        registerHintLabel.setText("<html><u>Not registered? Click here to create a " + role + " account.</u></html>");
        registerHintLabel.setVisible(true);
    }
}