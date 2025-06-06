package BsK.client.ui.component.RegisterPage;

import BsK.client.ui.component.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterPage extends JPanel {
    public RegisterPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        // Panel with background image and tint
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                ImageIcon icon = new ImageIcon("src/main/java/BsK/client/ui/assets/img/landingpagebg.jpeg");
                Image image = icon.getImage();
                g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                Color tint = new Color(0, 0, 0, 100); // Black tint with transparency
                g2d.setColor(tint);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel promptLabel = new JLabel("Please enter your details:");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridwidth = 2;
        backgroundPanel.add(promptLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        backgroundPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        backgroundPanel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(Color.WHITE);
        backgroundPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        backgroundPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        backgroundPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        backgroundPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel rePasswordLabel = new JLabel("Re-enter your password:");
        rePasswordLabel.setForeground(Color.WHITE);
        backgroundPanel.add(rePasswordLabel, gbc);

        gbc.gridx = 1;
        JPasswordField rePasswordField = new JPasswordField(20);
        backgroundPanel.add(rePasswordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setForeground(Color.WHITE);
        backgroundPanel.add(roleLabel, gbc);

        gbc.gridx = 1;
        String[] roles = {"user", "admin", "doctor", "nurse"};
        JComboBox<String> roleComboBox = new JComboBox<>(roles);
        backgroundPanel.add(roleComboBox, gbc);

        gbc.gridy++;
        JButton loginButton = new JButton("Register");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.showPage("LandingPage");
            }
        });
        backgroundPanel.add(loginButton, gbc);
        add(backgroundPanel, BorderLayout.CENTER);

        // Back button to return to the landing page
        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.showPage("LandingPage");
            }
        });
        add(backButton, BorderLayout.SOUTH);
    }
    public static void main(String[] args) {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
    }
}