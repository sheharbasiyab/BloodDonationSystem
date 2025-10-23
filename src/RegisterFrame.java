import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class RegisterFrame extends JFrame implements ActionListener {
    private String role;
    private JTextField nameField, ageField, bloodTypeField, contactField, locationField;
    private JPasswordField passwordField;
    private JButton registerButton;
    private JButton backButton;

    public RegisterFrame(String role) {
        this.role = role;
        setTitle("Register as " + role);
        setSize(520, 520);                    // larger window
        setMinimumSize(new Dimension(520, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(178, 34, 34));
        header.setPreferredSize(new Dimension(0, 70));
        JLabel hdr = new JLabel("Create " + role + " Account", SwingConstants.CENTER);
        hdr.setForeground(Color.WHITE);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.add(hdr, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // form area
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        form.setLayout(new GridLayout(role.equals("Hospital") ? 6 : 8, 2, 8, 8));

        form.add(new JLabel("Name:"));
        nameField = new JTextField();
        form.add(nameField);

        if (!role.equals("Hospital")) {
            form.add(new JLabel("Age:"));
            ageField = new JTextField();
            form.add(ageField);
        }

        // add blood type field only for roles that need it (e.g. Donor, Seeker)
        if (!"Hospital".equalsIgnoreCase(role)) {
            form.add(new JLabel("Blood Type:"));
            bloodTypeField = new JTextField();
            form.add(bloodTypeField);
        }

        form.add(new JLabel("Contact Info:"));
        contactField = new JTextField();
        form.add(contactField);

        form.add(new JLabel("Location:"));
        locationField = new JTextField();
        form.add(locationField);

        form.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        form.add(passwordField);

        add(form, BorderLayout.CENTER);

        // buttons panel
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        buttons.setBackground(Color.WHITE);

        registerButton = new JButton("Create Account");
        stylePrimaryButton(registerButton);
        registerButton.addActionListener(this);
        buttons.add(registerButton);

        backButton = new JButton("Back to Login");
        styleSecondaryButton(backButton);
        backButton.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        buttons.add(backButton);

        add(buttons, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void stylePrimaryButton(JButton b) {
        b.setBackground(new Color(178, 34, 34)); // red
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(180, 36));
    }

    private void styleSecondaryButton(JButton b) {
        b.setBackground(new Color(240, 240, 240));
        b.setForeground(new Color(178, 34, 34));
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(140, 34));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == registerButton) {
            try {
                String name = nameField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();
                String contact = (contactField != null) ? contactField.getText().trim() : "";
                String location = (locationField != null) ? locationField.getText().trim() : "";

                // only parse age and blood type for non-hospital roles
                int age = 0;
                String bloodType = null;
                if (!"Hospital".equalsIgnoreCase(role)) {
                    String ageStr = (ageField != null) ? ageField.getText().trim() : "";
                    if (ageStr.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Please enter age.");
                        return;
                    }
                    age = Integer.parseInt(ageStr);

                    bloodType = (bloodTypeField != null) ? bloodTypeField.getText().trim() : "";
                }

                if (name.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill required fields (name, password).");
                    return;
                }

                if (!"Hospital".equalsIgnoreCase(role)) {
                    if (bloodType == null || bloodType.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Please enter blood type.");
                        return;
                    }
                }

                if (registerUser(name, age, bloodType, contact, location, password)) {
                    JOptionPane.showMessageDialog(this, "Registration successful!");
                    dispose();
                    new LoginFrame();
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed! Check console for errors.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid age.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage());
            }
        }
    }

    private boolean registerUser(String name, int age, String bloodType, String contact, String location, String password) {
        String table = role.toLowerCase() + "s";
        String query;

        if (role.equals("Hospital")) {
            query = "INSERT INTO " + table + " (name, location, password) VALUES (?, ?, ?)";
        } else {
            query = "INSERT INTO " + table + " (name, age, "
                    + (role.equals("Seeker") ? "blood_type_needed" : "blood_type")
                    + ", contact_info, location, password) VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pst.setString(1, name);
            if (role.equals("Hospital")) {
                pst.setString(2, location);
                pst.setString(3, password);
            } else {
                pst.setInt(2, age);
                pst.setString(3, bloodType);
                pst.setString(4, contact);
                pst.setString(5, location);
                pst.setString(6, password);
            }

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                return false;
            }

            // Get generated ID for Hospital to initialize blood stocks
            int userId = -1;
            if (role.equals("Hospital")) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId = rs.getInt(1);
                        initializeBloodStocks(con, userId);
                    }
                }
            }

            logActivity("New " + role + " registered: " + name);
            return true;

        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void initializeBloodStocks(Connection con, int hospitalId) throws SQLException {
        String[] bloodTypes = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        String query = "INSERT INTO blood_stocks (hospital_id, blood_type, units) VALUES (?, ?, 0)";
        try (PreparedStatement pst = con.prepareStatement(query)) {
            for (String type : bloodTypes) {
                pst.setInt(1, hospitalId);
                pst.setString(2, type);
                pst.executeUpdate();
            }
        }
    }

    private void logActivity(String description) {
        String query = "INSERT INTO activity_logs (description) VALUES (?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, description);
            pst.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Main method for quick testing
    public static void main(String[] args) {
        new RegisterFrame("Donor");
    }
}
