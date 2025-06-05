package BsK.client.ui.component.DashboardPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;

@Slf4j
public class DashboardPage extends JPanel {
    private JLabel activeNavItem = null; // To keep track of the active navigation item
    private final MainFrame mainFrame;

    public DashboardPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240)); // A light background for the page

        // --- Navigation Bar ---
        JPanel navBar = new JPanel();
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS));
        navBar.setBackground(new Color(240, 240, 240)); // Bright gray background
        navBar.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Left section with navigation items
        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(navBar.getBackground());
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        navItemsPanel.setPreferredSize(new Dimension(750, 80));

        // Center section with current page title
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(navBar.getBackground());
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titlePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        JLabel titleLabel = new JLabel("Thống kê");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 60, 60));
        titlePanel.add(titleLabel);

        // Right section with user info
        JPanel userPanel = new JPanel();
        userPanel.setBackground(navBar.getBackground());
        userPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        userPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        userPanel.setPreferredSize(new Dimension(200, 80));

        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Kho", "Thanh toán", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "CheckoutPage", "UserPage", "InfoPage"};
        String[] iconFiles = {"dashboard.png", "health-check.png", "database.png", "warehouse.png", "cashier-machine.png", "user.png", "info.png"};
        Color[] navItemColors = {
            new Color(64, 169, 255),    // Blue
            new Color(82, 196, 26),     // Green
            new Color(250, 173, 20),    // Orange
            new Color(255, 77, 79),     // Red
            new Color(114, 46, 209),    // Purple
            new Color(19, 194, 194),    // Cyan
            new Color(245, 34, 45)      // Red-Orange
        };

        final Color defaultTextColor = new Color(255, 255, 255);
        final Color shadowColor = new Color(0, 0, 0, 50);

        for (int i = 0; i < navBarItems.length; i++) {
            final String itemText = navBarItems[i];
            final String dest = destination[i];
            String iconFileName = iconFiles[i];
            final Color itemColor = navItemColors[i];
            
            // Create panel with the item's color as default background
            RoundedPanel itemPanel = new RoundedPanel(15, itemColor.brighter(), true);
            itemPanel.setLayout(new BorderLayout(5, 5));
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 4, 2),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
            ));
            itemPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            itemPanel.setPreferredSize(new Dimension(130, 80));

            final JLabel label = new JLabel(itemText);
            label.setForeground(defaultTextColor);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setOpaque(false);

            try {
                String iconPath = "src/main/java/BsK/client/ui/assets/icon/" + iconFileName;
                ImageIcon originalIcon = new ImageIcon(iconPath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                label.setIcon(scaledIcon);
            } catch (Exception e) {
                log.error("Error loading icon: {} for nav item: {}", iconFileName, itemText, e);
            }

            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            itemPanel.add(label, BorderLayout.CENTER);

            if (itemText.equals("Thống kê")) {
                itemPanel.setBackground(itemColor);
                // Add a glow effect for selected item
                itemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 4, 2),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 2),
                        BorderFactory.createEmptyBorder(10, 18, 10, 18)
                    )
                ));
                activeNavItem = label;
            }

            final int index = i;
            itemPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (activeNavItem != null && activeNavItem != label) {
                        JPanel prevPanel = (JPanel) activeNavItem.getParent();
                        prevPanel.setBackground(navItemColors[findNavItemIndex(activeNavItem.getText(), navBarItems)].brighter());
                        prevPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 4, 2),
                            BorderFactory.createEmptyBorder(12, 20, 12, 20)
                        ));
                    }
                    activeNavItem = label;
                    itemPanel.setBackground(itemColor);
                    // Add selection effect with white border
                    itemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(2, 2, 4, 2),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.WHITE, 2),
                            BorderFactory.createEmptyBorder(10, 18, 10, 18)
                        )
                    ));
                    mainFrame.showPage(dest);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor);
                        // Add hover glow effect
                        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 4, 2),
                            BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.WHITE, 1),
                                BorderFactory.createEmptyBorder(11, 19, 11, 19)
                            )
                        ));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor.brighter());
                        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 4, 2),
                            BorderFactory.createEmptyBorder(12, 20, 12, 20)
                        ));
                    }
                }
            });
            navItemsPanel.add(itemPanel);
        }

        // Welcome Label with updated styling
        RoundedPanel welcomePanel = new RoundedPanel(15, new Color(52, 73, 94), true);
        welcomePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(2, 2, 4, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15) // Smaller padding
        ));
        welcomePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        welcomePanel.setPreferredSize(new Dimension(120, 40)); // Smaller size

        JLabel welcomeLabel = new JLabel("Chào, " + LocalStorage.username);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Smaller font
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showUserMenu(welcomeLabel);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                welcomePanel.setBackground(new Color(44, 62, 80));
                welcomePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 4, 2),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(70, 90, 120), 1),
                        BorderFactory.createEmptyBorder(7, 14, 7, 14) // Smaller padding
                    )
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                welcomePanel.setBackground(new Color(52, 73, 94));
                welcomePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 4, 2),
                    BorderFactory.createEmptyBorder(8, 15, 8, 15) // Smaller padding
                ));
            }
        });
        welcomePanel.add(welcomeLabel);
        userPanel.add(welcomePanel);

        navBar.add(navItemsPanel);
        navBar.add(Box.createHorizontalGlue());
        navBar.add(titlePanel);
        navBar.add(Box.createHorizontalGlue());
        navBar.add(userPanel);
        navBar.setPreferredSize(new Dimension(1200, 120)); // Maintain height

        add(navBar, BorderLayout.NORTH);

        // --- Main Content Panel (from existing DashboardPage) ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainContent.setOpaque(false); // Make it transparent to see page background

        // Top cards
        JPanel topCards = new JPanel();
        topCards.setOpaque(false); // Make transparent
        topCards.setLayout(new GridLayout(1, 4, 20, 20));
        String[] cardTitles = {"Add Check", "Income", "Expense", "Other Income"};
        // Using slightly more professional/subtle colors
        Color[] cardColors = {new Color(100, 181, 246), new Color(77, 208, 225), new Color(255, 183, 77), new Color(129, 199, 132)};

        for (int i = 0; i < cardTitles.length; i++) {
            RoundedPanel card = new RoundedPanel(15, cardColors[i], true); // Slightly smaller radius
            card.setBackground(cardColors[i]); // Ensure background is set
            card.setLayout(new BorderLayout(10,10));
            card.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15)); // Adjusted padding

            JLabel cardTitleLabel = new JLabel(cardTitles[i]);
            cardTitleLabel.setForeground(Color.WHITE);
            cardTitleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Larger title
            cardTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(cardTitleLabel, BorderLayout.NORTH);

            JLabel cardValueLabel = new JLabel("N/A"); // Placeholder value
            cardValueLabel.setForeground(Color.WHITE);
            cardValueLabel.setFont(new Font("Arial", Font.PLAIN, 24)); // Larger value text
            cardValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(cardValueLabel, BorderLayout.CENTER);

            topCards.add(card);
        }
        mainContent.add(topCards, BorderLayout.NORTH);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        wrapperPanel.setOpaque(false);

        RoundedPanel dataTablePanel = new RoundedPanel(15, Color.WHITE, false);
        dataTablePanel.setLayout(new BorderLayout());
        dataTablePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {"Name", "Gender", "Course", "Fees", "Action"};
        String[][] data = {
                {"John", "Male", "Java", "$300", "Edit/Delete"},
                {"Dara", "Male", "C++", "$300", "Edit/Delete"},
                {"Bora", "Male", "C#", "$300", "Edit/Delete"},
        };
        JTable table = new JTable(data, columns);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(10,0,0,0)); // Top padding for header
        dataTablePanel.add(tableScroll, BorderLayout.CENTER);

        wrapperPanel.add(dataTablePanel, BorderLayout.CENTER);
        mainContent.add(wrapperPanel, BorderLayout.CENTER);

        // Notice board (keeping it commented as in original)
        /*
        JPanel noticeBoard = new RoundedPanel(20, Color.WHITE, false);
        noticeBoard.setLayout(new BorderLayout());
        noticeBoard.setBorder(BorderFactory.createTitledBorder("Notice Board"));
        noticeBoard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea noticeText = new JTextArea("04/10/2021\nHidemode Now\n\n03/10/2021\nFurther Reading\n");
        noticeText.setEditable(false);
        noticeBoard.add(new JScrollPane(noticeText), BorderLayout.CENTER);
        mainContent.add(noticeBoard, BorderLayout.EAST);
        */

        add(mainContent, BorderLayout.CENTER);
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
        
        JMenuItem settingsItem = createMenuItem("Cài đặt", e -> 
            JOptionPane.showMessageDialog(mainFrame, "Tính năng Cài đặt sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        
        JMenuItem logoutItem = createMenuItem("Đăng xuất", e -> {
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

    // Main method (usually for testing, can be kept or removed)
    public static void main(String[] args) {
//        JFrame frame = new JFrame("Dashboard");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(1200, 800);
//        DashboardPage d = new DashboardPage(null); // Pass null or a mock MainFrame for testing
//        frame.add(d);
//        frame.setVisible(true);
    }
}