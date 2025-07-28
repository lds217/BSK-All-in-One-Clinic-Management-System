package BsK.client.ui.component.common;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.InfoPage.InfoDialog;
import BsK.client.ui.component.SettingsPage.SettingsDialog;
import BsK.common.packet.req.LogoutRequest;
import BsK.common.util.network.NetworkUtil;
import BsK.client.network.handler.ClientHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;

public class NavBar extends JPanel {

    private JLabel activeNavItem = null;
    private final MainFrame mainFrame;

    public NavBar(MainFrame mainFrame, String activePageTitle) {
        this.mainFrame = mainFrame;

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBackground(new Color(240, 240, 240));
        this.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 25));

        // Left section with navigation items
        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(this.getBackground());
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        navItemsPanel.setPreferredSize(new Dimension(800, 60));

        // Center section with current page title
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setBackground(this.getBackground());
        titlePanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(activePageTitle);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 60, 60));
        titlePanel.add(titleLabel, new GridBagConstraints());

        // Right section with user info
        JPanel userPanel = new JPanel(new GridBagLayout());
        userPanel.setBackground(this.getBackground());
        userPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        userPanel.setPreferredSize(new Dimension(200, 70));

        // Add welcome label with user info
        JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        welcomePanel.setBackground(this.getBackground());
        JLabel welcomeLabel = new JLabel("Chào, " + LocalStorage.username);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        welcomeLabel.setForeground(new Color(60, 60, 60));
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserMenu(welcomeLabel);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                welcomeLabel.setForeground(new Color(30, 30, 30));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                welcomeLabel.setForeground(new Color(60, 60, 60));
            }
        });
        welcomePanel.add(welcomeLabel);
        userPanel.add(welcomePanel, new GridBagConstraints());

        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Cài đặt", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "DataPage", "SettingsPage", "InfoPage"};
        String[] iconFiles = {"dashboard.png", "health-check.png", "database.png","settings.png", "info.png"};
        Color[] navItemColors = {
            new Color(51, 135, 204),    // Darker Blue
            new Color(66, 157, 21),     // Darker Green
            new Color(200, 138, 16),    // Darker Orange
            new Color(91, 37, 167),     // Darker Purple
            new Color(196, 27, 36)      // Darker Red-Orange
        };

        final Color defaultTextColor = new Color(50, 50, 50);
        final Color activeTextColor = Color.WHITE;

        for (int i = 0; i < navBarItems.length; i++) {
            final String itemText = navBarItems[i];
            final String dest = destination[i];
            String iconFileName = iconFiles[i];
            final Color itemColor = navItemColors[i];

            RoundedPanel itemPanel = new RoundedPanel(15, itemColor.brighter(), true);
            itemPanel.setLayout(new BorderLayout(2, 2));
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            itemPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            itemPanel.setPreferredSize(new Dimension(100, 55));

            final JLabel label = new JLabel(itemText);
            label.setForeground(defaultTextColor);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setOpaque(false);

            try {
                String iconPath = "src/main/java/BsK/client/ui/assets/icon/" + iconFileName;
                ImageIcon originalIcon = new ImageIcon(iconPath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                label.setIcon(scaledIcon);
            } catch (Exception e) {
                // log is not available here, print to stderr
                System.err.println("Error loading icon: " + iconFileName + " for nav item: " + itemText);
                e.printStackTrace();
            }

            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            itemPanel.add(label, BorderLayout.CENTER);

            if (itemText.equals(activePageTitle)) {
                itemPanel.setBackground(itemColor);
                label.setForeground(activeTextColor);
                activeNavItem = label;
            }

            itemPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if ("InfoPage".equals(dest)) {
                        InfoDialog infoDialog = new InfoDialog();
                        infoDialog.setVisible(true);
                        return;
                    }
                    
                    if ("SettingsPage".equals(dest)) {
                        // Open settings dialog instead of navigating to a page
                        SettingsDialog settingsDialog = new SettingsDialog(mainFrame);
                        settingsDialog.setVisible(true);
                        return;
                    }

                    if (activeNavItem != null && activeNavItem != label) {
                        JPanel prevPanel = (JPanel) activeNavItem.getParent();
                        prevPanel.setBackground(navItemColors[findNavItemIndex(activeNavItem.getText(), navBarItems)].brighter());
                        activeNavItem.setForeground(defaultTextColor);
                    }
                    activeNavItem = label;
                    itemPanel.setBackground(itemColor);
                    label.setForeground(activeTextColor);
                    mainFrame.showPage(dest);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor); // Darken background on hover
                        label.setForeground(activeTextColor); // Set text to active color
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor.brighter()); // Revert to default inactive background
                        label.setForeground(defaultTextColor); // Revert text color
                    }
                }
            });
            navItemsPanel.add(itemPanel);
        }

        this.add(navItemsPanel);
        this.add(Box.createHorizontalGlue());
        this.add(titlePanel);
        this.add(Box.createHorizontalGlue());
        this.add(userPanel);
        this.setPreferredSize(new Dimension(1200, 70));
    }

    private int findNavItemIndex(String text, String[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(text)) return i;
        }
        return 0;
    }

    private void showUserMenu(JLabel welcomeLabel) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createEmptyBorder());

        JMenuItem profileItem = createMenuItem("Hồ sơ cá nhân", e ->
            JOptionPane.showMessageDialog(mainFrame, "Tính năng Hồ sơ cá nhân sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE));

        JMenuItem settingsItem = createMenuItem("Cài đặt", e -> {
            SettingsDialog settingsDialog = new SettingsDialog(mainFrame);
            settingsDialog.setVisible(true);
        });

        JMenuItem logoutItem = createMenuItem("Đăng xuất", e -> {
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new LogoutRequest());
            LocalStorage.username = null;
            LocalStorage.userId = -1;
            mainFrame.showPage("LandingPage");
        });

        popupMenu.add(profileItem);
        popupMenu.add(settingsItem);
        popupMenu.addSeparator();
        popupMenu.add(logoutItem);

        popupMenu.setPreferredSize(new Dimension(180, popupMenu.getPreferredSize().height));
        popupMenu.show(welcomeLabel, 0, welcomeLabel.getHeight());
    }

    private JMenuItem createMenuItem(String text, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Arial", Font.PLAIN, 12));
        item.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        item.addActionListener(action);
        return item;
    }
} 