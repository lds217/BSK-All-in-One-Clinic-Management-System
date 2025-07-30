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

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton loginButton = new JButton("Đăng nhập");
        loginButton.setUI(new RoundedButtonUI(new Color(13, 110, 253), Color.WHITE, 10));
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));

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
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new ClinicInfoRequest()); // Request clinic info
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDoctorGeneralInfo()); // Request doctor info
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetProvinceRequest()); // Request province info
            ClientHandler.deleteListener(ErrorResponse.class);
            mainFrame.showPage("DashboardPage");
        });

        ClientHandler.addResponseListener(ErrorResponse.class, response -> {
            log.error("Login error: {}", response.getError());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "Đăng nhập thất bại: " + response.getError(), "Lỗi", JOptionPane.ERROR_MESSAGE));
        });


        formPanel.add(loginButton, gbc);
        backgroundPanel.add(formPanel, new GridBagConstraints());


        JButton backButton = new JButton("Trở về");
        backButton.setUI(new RoundedButtonUI(new Color(108, 117, 125), Color.WHITE, 10));
        backButton.setFont(new Font("Arial", Font.BOLD, 14));

        backButton.addActionListener(e -> mainFrame.showPage("LandingPage"));

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setOpaque(false);
        southPanel.add(backButton);
        add(southPanel, BorderLayout.SOUTH);
    }
}
