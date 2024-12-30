package BsK.client.ui.component;

import BsK.client.ui.component.CheckUpPage.CheckUpPage;
import BsK.client.ui.component.DashboardPage.DashboardPage;
import BsK.client.ui.component.LandingPage.LandingPage;
import BsK.client.ui.component.LoginPage.LoginPage;
import BsK.client.ui.component.RegisterPage.RegisterPage;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    public MainFrame() {
        setTitle("BSK");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add pages to the main panel
        mainPanel.add(new LandingPage(this), "LandingPage");
        mainPanel.add(new LoginPage(this), "LoginPage");
        mainPanel.add(new RegisterPage(this), "RegisterPage");
        mainPanel.add(new DashboardPage(this), "DashboardPage");
        mainPanel.add(new CheckUpPage(this), "CheckUpPage");

        add(mainPanel);
    }

    public void showPage(String pageName) {
        cardLayout.show(mainPanel, pageName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}