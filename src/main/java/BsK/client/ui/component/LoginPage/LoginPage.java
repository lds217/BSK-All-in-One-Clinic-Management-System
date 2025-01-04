package BsK.client.ui.component.LoginPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.ui.component.MainFrame;
import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.LoginRequest;
import BsK.common.packet.res.ErrorResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
public class LoginPage extends JPanel {
    public LoginPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        // Panel with background image and tint
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                ImageIcon icon = new ImageIcon("src/main/java/BsK/client/ui/image/landingpagebg.jpeg");
                Image image = icon.getImage();
                g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                Color tint = new Color(0, 0, 0, 100); // Black tint with transparency
                g2d.setColor(tint);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span across two columns

        JLabel promptLabel = new JLabel("Please enter your details:");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Arial", Font.BOLD, 20));
        backgroundPanel.add(promptLabel, gbc);

        gbc.gridwidth = 1; // Reset to default
        gbc.gridy++;
        JLabel usernameLabel = new JLabel("Enter Username:");
        usernameLabel.setForeground(Color.WHITE);
        backgroundPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        JTextField loginField = new JTextField(20);
        backgroundPanel.add(loginField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel passwordLabel = new JLabel("Enter Password:");
        passwordLabel.setForeground(Color.WHITE);
        backgroundPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        backgroundPanel.add(passwordField, gbc);

        gbc.gridy++;
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);
            String username = loginField.getText();
            var loginRequest = new LoginRequest(username, password);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), loginRequest);
        });


        ClientHandler.addResponseListener(LoginSuccessResponse.class, response -> {
            log.info("Login successful, UserId: {}, Role: {}", response.getUserId(), response.getRole());
            LocalStorage.username = loginField.getText();
            mainFrame.showPage("DashboardPage");
        });

        ClientHandler.addResponseListener(ErrorResponse.class, response -> {
            log.error("Login error: {}", response.getError());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Login failed: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE));
        });


        backgroundPanel.add(loginButton, gbc);
        add(backgroundPanel, BorderLayout.CENTER);


        JButton backButton = new JButton("Back");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.showPage("LandingPage");
            }
        });
        add(backButton, BorderLayout.SOUTH);
    }
}