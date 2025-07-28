package BsK.client.ui.component;

import BsK.client.ui.component.CheckUpPage.CheckUpPage;
import BsK.client.ui.component.DashboardPage.DashboardPage;
import BsK.client.ui.component.LandingPage.LandingPage;
import BsK.client.ui.component.LoginPage.LoginPage;
import BsK.client.ui.component.RegisterPage.RegisterPage;
import BsK.client.network.handler.ClientHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel currentPage;

    public MainFrame() {
        setTitle("BSK");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add pages to the main panel
        mainPanel.add(new LandingPage(this), "LandingPage");
        currentPage = (JPanel) mainPanel.getComponent(0);

        // Add window listener for proper cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performFullCleanup();
                System.exit(0);
            }
        });

        add(mainPanel);
    }

    public void showPage(String pageName) {
        try {
            // Perform fast cleanup on current page if it's CheckUpPage
            if (currentPage instanceof CheckUpPage) {
                ((CheckUpPage) currentPage).fastCleanup();
            }

            //  clear all listeners on current page
            ClientHandler.clearListeners();

            // Construct the fully qualified class name
            String className = "BsK.client.ui.component." + pageName + "." + pageName;
            Class<?> pageClass = Class.forName(className);
            JPanel newPage = (JPanel) pageClass.getConstructor(MainFrame.class).newInstance(this);

            mainPanel.add(newPage, pageName);
            mainPanel.remove(currentPage);
            currentPage = newPage;
            cardLayout.show(mainPanel, pageName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to load page: " + pageName, e);
        }
    }

    public JPanel getCurrentPage() {
        return currentPage;
    }

    private void performFullCleanup() {
        // Perform full cleanup on current page if it's CheckUpPage
        if (currentPage instanceof CheckUpPage) {
            ((CheckUpPage) currentPage).fullCleanup();
        }
        // Clear all listeners
        ClientHandler.clearListeners();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}