package BsK.client.ui.component.LoginPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.common.packet.req.ClinicInfoRequest;
import BsK.common.packet.req.GetDoctorGeneralInfo;
import BsK.common.packet.req.GetProvinceRequest;
import BsK.common.packet.req.LoginRequest;
import BsK.common.packet.res.ErrorResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

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
                ImageIcon icon = new ImageIcon("src/main/java/BsK/client/ui/assets/img/landingpagebg.jpeg");
                Image image = icon.getImage();
                g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
                Color tint = new Color(0, 0, 0, 100); // Black tint with transparency
                g2d.setColor(tint);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        backgroundPanel.setLayout(new GridBagLayout());
        add(backgroundPanel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 4, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel promptLabel = new JLabel("Đăng nhập");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Arial", Font.BOLD, 32));
        formPanel.add(promptLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel usernameLabel = new JLabel("Tên đăng nhập:");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JTextField loginField = new JTextField(20);
        formPanel.add(loginField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setForeground(Color.WHITE);
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JPasswordField passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // --- BUTTON PANEL START ---
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10); // Add top margin

        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonContainer.setOpaque(false);

        // --- Back Button (Secondary Style) ---
        JButton backButton = new JButton("Trở về");
        backButton.setForeground(Color.WHITE);
        // Style: Transparent background with a white border
        backButton.setUI(new RoundedButtonUI(new Color(0, 0, 0, 0), Color.WHITE, Color.WHITE, 10));
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.addActionListener(e -> mainFrame.showPage("LandingPage"));

        // --- Login Button (Primary Style, copied from LandingPage) ---
        Color primaryBlueText = new Color(13, 110, 253);
        JButton loginButton = new JButton("Đăng nhập");
        loginButton.setForeground(primaryBlueText);
        loginButton.setUI(new RoundedButtonUI(Color.WHITE, primaryBlueText, 10)); // White background
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

        Dimension buttonSize = new Dimension(120, 40);
        loginButton.setPreferredSize(buttonSize);
        backButton.setPreferredSize(buttonSize);

        buttonContainer.add(backButton);
        buttonContainer.add(loginButton);

        formPanel.add(buttonContainer, gbc);
        // --- BUTTON PANEL END ---

        backgroundPanel.add(formPanel, new GridBagConstraints());


        passwordField.addActionListener(e -> loginButton.doClick());

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
            LocalStorage.userId = response.getUserId();
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new ClinicInfoRequest());
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDoctorGeneralInfo());
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetProvinceRequest());
            ClientHandler.deleteListener(ErrorResponse.class);
            mainFrame.showPage("DashboardPage");
        });

        ClientHandler.addResponseListener(ErrorResponse.class, response -> {
            log.error("Login error: {}", response.getError());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Đăng nhập thất bại: " + response.getError(), "Lỗi", JOptionPane.ERROR_MESSAGE));
        });
    }
}