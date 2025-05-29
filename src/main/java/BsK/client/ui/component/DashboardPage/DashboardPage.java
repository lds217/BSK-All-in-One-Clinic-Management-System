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

@Slf4j
public class DashboardPage extends JPanel {
    private JLabel activeNavItem = null; // To keep track of the active navigation item

    public DashboardPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240)); // A light background for the page

        // --- Navigation Bar (copied and adapted from CheckUpPage) ---
        JPanel navBar = new JPanel();
        navBar.setBackground(new Color(63, 81, 181));
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS));

        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(new Color(63, 81, 181));
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 15)); 
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Kho", "Thanh toán", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "CheckoutPage", "UserPage", "InfoPage"};
        String[] iconFiles = {"dashboard.png", "health-check.png", "database.png", "warehouse.png", "cashier-machine.png", "user.png", "info.png"};

        final Color defaultNavColor = new Color(63, 81, 181); 
        final Color hoverNavColor = new Color(50, 70, 170); 
        final Color activeNavColor = new Color(33, 150, 243); 

        final Border defaultNavItemBorder = BorderFactory.createEmptyBorder(12, 15, 12, 15);
        // Specific border for the active "Thống kê" page
        final Border activeDashboardSpecificBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, activeNavColor),
                BorderFactory.createEmptyBorder(12, 15, 9, 15) 
        );

        for (int i = 0; i < navBarItems.length; i++) {
            final String itemText = navBarItems[i];
            final String dest = destination[i];
            String iconFileName = iconFiles[i];

            final JLabel label = new JLabel(itemText);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setBorder(defaultNavItemBorder);
            label.setOpaque(false);

            try {
                String iconPath = "src/main/java/BsK/client/ui/assets/icon/" + iconFileName;
                ImageIcon originalIcon = new ImageIcon(iconPath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                label.setIcon(scaledIcon);
            } catch (Exception e) {
                log.error("Error loading icon: {} for nav item: {}", iconFileName, itemText, e);
            }

            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setHorizontalTextPosition(SwingConstants.CENTER);

            if (itemText.equals("Thống kê")) { // Highlight "Thống kê" as active
                label.setBorder(activeDashboardSpecificBorder);
                label.setBackground(activeNavColor);
                label.setOpaque(true);
                activeNavItem = label;
            }

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (activeNavItem != null && activeNavItem != label) {
                        activeNavItem.setBackground(defaultNavColor);
                        activeNavItem.setOpaque(false);
                        activeNavItem.setForeground(Color.WHITE);
                        activeNavItem.setBorder(defaultNavItemBorder);
                    }
                    activeNavItem = label;
                    activeNavItem.setBackground(activeNavColor);
                    activeNavItem.setOpaque(true);
                    activeNavItem.setForeground(Color.WHITE);
                    // Apply specific border only if it's the designated active page for this class
                    if (itemText.equals("Thống kê")) { 
                        activeNavItem.setBorder(activeDashboardSpecificBorder);
                    } else {
                        activeNavItem.setBorder(defaultNavItemBorder); 
                    }
                    mainFrame.showPage(dest);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (label != activeNavItem) {
                        label.setForeground(new Color(200, 230, 255));
                        label.setBackground(hoverNavColor);
                        label.setOpaque(true);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (label != activeNavItem) {
                        label.setForeground(Color.WHITE);
                        label.setBackground(defaultNavColor);
                        label.setOpaque(false);
                    }
                }
            });
            navItemsPanel.add(label);
        }

        navBar.add(navItemsPanel);
        navBar.add(Box.createHorizontalGlue());

        JLabel welcomeLabel = new JLabel("Chào, " + LocalStorage.username + "            ");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) { 
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem profileItem = new JMenuItem("Hồ sơ cá nhân");
                    JMenuItem settingsItem = new JMenuItem("Cài đặt");
                    JMenuItem logoutItem = new JMenuItem("Đăng xuất");

                    profileItem.addActionListener(event -> {
                        JOptionPane.showMessageDialog(mainFrame, "Tính năng Hồ sơ cá nhân sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    });
                    settingsItem.addActionListener(event -> {
                        JOptionPane.showMessageDialog(mainFrame, "Tính năng Cài đặt sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    });
                    // For Logout, you'll need ClientHandler and LogoutRequest from CheckUpPage context if not already available
                    // For now, just a placeholder action
                    logoutItem.addActionListener(event -> {
                        // NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new LogoutRequest()); // Requires imports
                        LocalStorage.username = null;
                        LocalStorage.userId = -1; 
                        mainFrame.showPage("LandingPage");
                    });
                    popupMenu.add(profileItem);
                    popupMenu.add(settingsItem);
                    popupMenu.addSeparator(); 
                    popupMenu.add(logoutItem);
                    popupMenu.setPreferredSize(new Dimension(150, popupMenu.getPreferredSize().height));
                    popupMenu.show(welcomeLabel, 0, welcomeLabel.getHeight());
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                welcomeLabel.setForeground(new Color(200, 230, 255)); 
            }
            @Override
            public void mouseExited(MouseEvent e) {
                welcomeLabel.setForeground(Color.WHITE); 
            }
        });
        navBar.add(welcomeLabel);
        navBar.setPreferredSize(new Dimension(1200, 85));

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