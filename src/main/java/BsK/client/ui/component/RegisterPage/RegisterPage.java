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
        
        JLabel announcementLabel = new JLabel("Hãy liên hệ với Admin để tạo tài khoản");
        announcementLabel.setForeground(Color.WHITE);
        announcementLabel.setFont(new Font("Arial", Font.BOLD, 20));
        backgroundPanel.add(announcementLabel, new GridBagConstraints());

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
}
