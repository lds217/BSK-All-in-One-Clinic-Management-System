package BsK.client.ui.component.common;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.InfoPage.InfoDialog;
import BsK.client.ui.component.SettingsPage.SettingsDialog;
import BsK.client.ui.component.DataDialog.DataDialog;
import BsK.client.ui.component.CheckUpPage.CheckUpPage;
import BsK.common.Error;
import BsK.common.packet.req.EmergencyRequest;
import BsK.common.packet.req.LogoutRequest;
import BsK.common.util.network.NetworkUtil;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.res.EmergencyResponse;
import BsK.common.packet.res.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import javax.swing.Icon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;

@Slf4j
public class NavBar extends JPanel {

    private JLabel activeNavItem = null;
    private final MainFrame mainFrame;

    // Listener for incoming emergency alerts
    private final ResponseListener<EmergencyResponse> emergencyResponseListener = this::handleEmergencyResponse;
    private final ResponseListener<ErrorResponse> errorResponseListener = this::handleErrorResponse;

    public NavBar(MainFrame mainFrame, String activePageTitle) {
        this.mainFrame = mainFrame;

        // Register the response listener for emergency alerts.
        ClientHandler.addResponseListener(EmergencyResponse.class, emergencyResponseListener);
        // add error response listener
        ClientHandler.addResponseListener(ErrorResponse.class, errorResponseListener);
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setBackground(new Color(240, 240, 240));
        this.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 25));

        // Left section with navigation items
        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(this.getBackground());
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Center section with current page title
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setBackground(this.getBackground());
        titlePanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(activePageTitle);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 60, 60));
        titlePanel.add(titleLabel, new GridBagConstraints());

        // Right section with user info and emergency button
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userPanel.setBackground(this.getBackground());
        userPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        // Add the Emergency button first
        Component emergencyButton = createEmergencyButton();
        userPanel.add(emergencyButton);

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
        userPanel.add(welcomePanel);

        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Cài đặt", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "CheckUpPage", "SettingsPage", "InfoPage"};
        String[] iconFiles = {"dashboard.png", "health-check.png", "database.png","settings.png", "info.png"};
        Color[] navItemColors = {
                new Color(51, 135, 204),       // Darker Blue
                new Color(66, 157, 21),        // Darker Green
                new Color(200, 138, 16),       // Darker Orange
                new Color(91, 37, 167),        // Darker Purple
                new Color(196, 27, 36)         // Darker Red-Orange
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
                        SettingsDialog settingsDialog = new SettingsDialog(mainFrame);
                        settingsDialog.setVisible(true);
                        return;
                    }

                    if ("Dữ liệu".equals(itemText)) {
                        if (!(mainFrame.getCurrentPage() instanceof CheckUpPage)) {
                            mainFrame.showPage("CheckUpPage");
                            if (activeNavItem != null) {
                                JPanel prevPanel = (JPanel) activeNavItem.getParent();
                                prevPanel.setBackground(navItemColors[findNavItemIndex(activeNavItem.getText(), navBarItems)].brighter());
                                activeNavItem.setForeground(defaultTextColor);
                            }
                            for (int j = 0; j < navBarItems.length; j++) {
                                if ("Thăm khám".equals(navBarItems[j])) {
                                    Component[] components = navItemsPanel.getComponents();
                                    for(Component comp : components) {
                                        if (comp instanceof JPanel && ((JPanel) comp).getComponentCount() > 0 && ((JPanel) comp).getComponent(0) instanceof JLabel) {
                                            JLabel potentialLabel = (JLabel) ((JPanel) comp).getComponent(0);
                                            if ("Thăm khám".equals(potentialLabel.getText())) {
                                                activeNavItem = potentialLabel;
                                                ((JPanel) comp).setBackground(navItemColors[j]);
                                                potentialLabel.setForeground(activeTextColor);
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            // Get the current CheckUpPage instance from the main frame
                            CheckUpPage checkUpPage = (CheckUpPage) mainFrame.getCurrentPage();
                            
                            // Pass this instance to the DataDialog
                            DataDialog dataDialog = new DataDialog(mainFrame, checkUpPage); // <-- MODIFIED CONSTRUCTOR
                            dataDialog.setVisible(true);
                        });
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
                        itemPanel.setBackground(itemColor);
                        label.setForeground(activeTextColor);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor.brighter());
                        label.setForeground(defaultTextColor);
                    }
                }
            });
            navItemsPanel.add(itemPanel);

            // --- MODIFIED SEPARATOR ---
            if (i == 1) {
                // Create the separator with a darker color
                JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
                separator.setForeground(new Color(150, 150, 150));

                // Set a fixed size to make it thicker and taller
                separator.setPreferredSize(new Dimension(2, 45));

                // Create a panel to hold the separator and add some horizontal padding for spacing
                JPanel separatorPanel = new JPanel();
                separatorPanel.setBackground(this.getBackground()); // Match the navbar background
                separatorPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add 5px padding left and right
                separatorPanel.add(separator);

                navItemsPanel.add(separatorPanel);
            }
            // --- END MODIFIED SEPARATOR ---
        }

        this.add(navItemsPanel);
        this.add(Box.createHorizontalGlue());
        this.add(titlePanel);
        this.add(Box.createHorizontalGlue());
        this.add(userPanel);
    }

    private int findNavItemIndex(String text, String[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(text)) return i;
        }
        return 0;
    }

    private Component createEmergencyButton() {
        Color emergencyColor = new Color(220, 38, 38);
        RoundedPanel buttonPanel = new RoundedPanel(15, emergencyColor, true);
        buttonPanel.setLayout(new BorderLayout(2, 2));
        buttonPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        buttonPanel.setPreferredSize(new Dimension(100, 55));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JLabel buttonLabel = new JLabel("Khẩn cấp");
        buttonLabel.setForeground(Color.WHITE);
        buttonLabel.setFont(new Font("Arial", Font.BOLD, 12));
        buttonLabel.setHorizontalAlignment(SwingConstants.CENTER);
        buttonLabel.setOpaque(false);

        try {
            String iconPath = "src/main/java/BsK/client/ui/assets/icon/emergency.png";
            ImageIcon originalIcon = new ImageIcon(iconPath);
            Image scaledImage = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            buttonLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            System.err.println("Error loading icon: emergency.png. Using text fallback.");
            buttonLabel.setText("Khẩn cấp");
        }

        buttonLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        buttonLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        buttonPanel.add(buttonLabel, BorderLayout.CENTER);

        buttonPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int response = JOptionPane.showConfirmDialog(
                        mainFrame,
                        "Bạn có chắc chắn muốn kích hoạt chế độ khẩn cấp không?",
                        "XÁC NHẬN KHẨN CẤP",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (response == JOptionPane.YES_OPTION) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new EmergencyRequest());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                buttonPanel.setBackground(emergencyColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonPanel.setBackground(emergencyColor);
            }
        });

        return buttonPanel;
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

    /**
     * Handles the incoming EmergencyResponse. This version is silent and uses a manual
     * dialog creation to ensure the custom close button works correctly.
     * @param response The emergency response packet from the server.
     */
    private void handleEmergencyResponse(EmergencyResponse response) {
        SwingUtilities.invokeLater(() -> {
            log.warn("Received EMERGENCY alert from: {}", response.getSenderName());

            // Create enhanced visual components for the dialog
            String htmlMessage = "<html><body style='width: 350px; text-align: center;'>"
                    + "<h1><font color='#B22222'>CẢNH BÁO KHẨN CẤP!</font></h1>"
                    + "<p style='font-size: 14px;'>Nhận được tín hiệu khẩn cấp từ người dùng:</p>"
                    + "<h2 style='font-size: 24px; color: #8B0000;'>" + response.getSenderName() + "</h2>"
                    + "<p style='font-size: 14px;'>Vui lòng kiểm tra và hỗ trợ ngay lập tức!</p>"
                    + "</body></html>";
            JLabel messageLabel = new JLabel(htmlMessage);

            JCheckBox acknowledgementCheck = new JCheckBox("Tôi xác nhận đã đọc và hiểu rõ cảnh báo này.");
            acknowledgementCheck.setFont(new Font("Arial", Font.ITALIC, 12));
            acknowledgementCheck.setOpaque(false);

            JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
            contentPanel.setOpaque(false);
            contentPanel.add(messageLabel, BorderLayout.CENTER);
            contentPanel.add(acknowledgementCheck, BorderLayout.SOUTH);

            JPanel dialogPanel = new JPanel(new BorderLayout());
            dialogPanel.setBackground(new Color(255, 220, 220));
            dialogPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
            dialogPanel.add(contentPanel, BorderLayout.CENTER);

            JButton confirmButton = new JButton("ĐÃ XỬ LÝ");
            confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
            confirmButton.setEnabled(false);

            acknowledgementCheck.addActionListener(e -> confirmButton.setEnabled(acknowledgementCheck.isSelected()));

            // --- CORRECTED DIALOG CREATION ---
            // 1. Create a JOptionPane instance with our custom panel and button.
            JOptionPane optionPane = new JOptionPane(
                    dialogPanel,
                    JOptionPane.ERROR_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,
                    new Object[]{confirmButton}, // Array of options
                    confirmButton);

            // 2. Create a JDialog from the JOptionPane.
            final JDialog dialog = optionPane.createDialog(mainFrame, "🚨🚨 TÍN HIỆU KHẨN CẤP 🚨🚨");

            // 3. Add an ActionListener to our custom button to close the dialog.
            confirmButton.addActionListener(e -> dialog.dispose());

            // 4. Show the dialog.
            dialog.setVisible(true);
        });
    }

    private void handleErrorResponse(ErrorResponse response) {
        log.error("Error response: {}", response.getError());
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame,
                response.getError().toString(), "Lỗi", JOptionPane.ERROR_MESSAGE));
    }

    /**
     * Cleans up resources, specifically unregistering network listeners.
     * This should be called when the NavBar is no longer needed (e.g., on logout).
     */
    public void cleanup() {
        ClientHandler.deleteListener(EmergencyResponse.class, emergencyResponseListener);
        ClientHandler.deleteListener(ErrorResponse.class, errorResponseListener);
        log.info("EmergencyResponse listener removed from NavBar.");
    }
}