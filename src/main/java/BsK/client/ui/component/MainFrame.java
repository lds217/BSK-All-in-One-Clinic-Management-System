package BsK.client.ui.component;

import BsK.client.ui.component.CheckUpPage.CheckUpPage;
import BsK.client.ui.component.DashboardPage.DashboardPage;
import BsK.client.ui.component.LandingPage.LandingPage;
import BsK.client.ui.component.LoginPage.LoginPage;
import BsK.client.ui.component.RegisterPage.RegisterPage;
import BsK.client.network.handler.ClientHandler;

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
//        mainPanel.add(new CheckUpPage(this), "CheckUpPage");

        add(mainPanel);
    }

//    public void showPage(String pageName) {
//        cardLayout.show(mainPanel, pageName);
//    }

    public void showPage(String pageName) {
        try {
            //  clear all listeners on current page
            ClientHandler.clearListeners();

            // Construct the fully qualified class name
            String className = "BsK.client.ui.component." + pageName + "." + pageName;
            Class<?> pageClass = Class.forName(className);
            JPanel newPage = (JPanel) pageClass.getConstructor(MainFrame.class).newInstance(this);
            // clear all listeners on new page

            mainPanel.add(newPage, pageName);
            mainPanel.remove(0);
            cardLayout.show(mainPanel, pageName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to load page: " + pageName, e);
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}