package BsK.client.ui.component.LandingPage;

import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.RoundedButtonUI;

import javax.swing.*;
import java.awt.*;

public class LandingPage extends JPanel {
    public LandingPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        // Left and middle panel with background image and tint
        JPanel leftMiddlePanel = new JPanel() {
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
        leftMiddlePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel titleLabel = new JLabel("BSK");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 50));
        leftMiddlePanel.add(titleLabel, gbc);

        gbc.gridy++;
        JLabel authorLabel = new JLabel("by lds217 and miti99");
        authorLabel.setForeground(Color.WHITE);
        authorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        leftMiddlePanel.add(authorLabel, gbc);

        // Right panel with blue gradient and buttons
        JPanel rightPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(0, 0, 255);
                Color color2 = new Color(173, 216, 230);
                GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        rightPanel.setLayout(new GridBagLayout());
        rightPanel.setPreferredSize(new Dimension(300, getHeight())); // Set preferred size for the right panel
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;

        // --- BUTTONS UPDATED ---
        Color primaryBlueText = new Color(13, 110, 253);

        JButton loginButton = new JButton("Đăng nhập");
        loginButton.setForeground(primaryBlueText); // Set text color
        loginButton.setUI(new RoundedButtonUI(Color.WHITE, primaryBlueText, 10)); // White background
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        JButton registerButton = new JButton("Đăng ký");
        registerButton.setForeground(primaryBlueText); // Set text color
        registerButton.setUI(new RoundedButtonUI(Color.WHITE, primaryBlueText, 10)); // White background
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));

        // Set the same preferred size for both buttons
        Dimension buttonSize = new Dimension(150, 40);
        loginButton.setPreferredSize(buttonSize);
        registerButton.setPreferredSize(buttonSize);

        rightPanel.add(loginButton, gbc);
        gbc.gridy++;
        rightPanel.add(registerButton, gbc);

        add(leftMiddlePanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Action listeners for buttons
        loginButton.addActionListener(e -> mainFrame.showPage("LoginPage"));
        registerButton.addActionListener(e -> mainFrame.showPage("RegisterPage"));
    }
}