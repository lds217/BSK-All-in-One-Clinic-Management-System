package BsK.client.ui.component.DashboardPage;

import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;

import javax.swing.*;
import java.awt.*;

public class DashboardPage extends JPanel {
    public DashboardPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        // Sidebar panel
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(63, 81, 181);
                Color color2 = new Color(33, 150, 243);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        sidebar.setBackground(new Color(63, 81, 181));
        sidebar.setLayout(new GridLayout(15, 1));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] sidebarItems = {"Thống kê","Thăm khám", "Dữ liệu bệnh nhân", "Kho", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage","CheckUpPage", "PatientDataPage", "InventoryPage", "UserPage", "InfoPage"};
        for(int i = 0; i < sidebarItems.length; i++) {
            String item = sidebarItems[i];
            String dest = destination[i];
            JLabel label = new JLabel(item);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Change cursor to hand
            label.setFont(new Font("Arial", Font.BOLD, 14));

            // Add a mouse listener to handle click events
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Handle the click event
                    mainFrame.showPage(dest);

                  //  JOptionPane.showMessageDialog(null, item + " clicked!");
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    label.setForeground(new Color(200, 230, 255)); // Highlight on hover
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    label.setForeground(Color.WHITE); // Restore original color
                }
            });

            sidebar.add(label);
        }

// Make the sidebar scrollable
        JScrollPane sidebarScrollPane = new JScrollPane(sidebar);
        sidebarScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        sidebarScrollPane.getViewport().setOpaque(false);
        sidebarScrollPane.setOpaque(false);



        // Topbar panel
        JPanel topbar = new JPanel();
        topbar.setLayout(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Dashboard / Home");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        topbar.add(title, BorderLayout.WEST);

        JLabel userInfo = new JLabel("User Name (Admin)");
        userInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        topbar.add(userInfo, BorderLayout.EAST);

        // Main content panel
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top cards
        JPanel topCards = new JPanel();
        topCards.setLayout(new GridLayout(1, 4, 20, 20));
        String[] cardTitles = {"Add Check", "Income", "Expense", "Other Income"};
        Color[] cardColors = {Color.PINK, Color.CYAN, Color.ORANGE, Color.GREEN};

        for (int i = 0; i < cardTitles.length; i++) {
            JPanel card = new RoundedPanel(20, cardColors[i], true);
            card.setBackground(cardColors[i]);
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

            JLabel cardTitle = new JLabel(cardTitles[i]);
            cardTitle.setForeground(Color.WHITE);
            cardTitle.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(cardTitle, BorderLayout.NORTH);

            JLabel cardValue = new JLabel("Value", SwingConstants.CENTER);
            cardValue.setForeground(Color.WHITE);
            cardValue.setFont(new Font("Arial", Font.BOLD, 16));
            card.add(cardValue, BorderLayout.CENTER);

            topCards.add(card);
        }

        mainContent.add(topCards, BorderLayout.NORTH);

        // Wrapper panel for spacing
        JPanel wrapperPanel = new JPanel();
        wrapperPanel.setLayout(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0)); // Add spacing above
        wrapperPanel.setOpaque(false); // Ensure transparency for background compatibility

// Data table inside a RoundedPanel
        RoundedPanel dataTable = new RoundedPanel(20, Color.WHITE, false);
        dataTable.setLayout(new BorderLayout());
        dataTable.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Adjust padding for better appearance

        String[] columns = {"Name", "Gender", "Course", "Fees", "Action"};
        String[][] data = {
                {"John", "Male", "Java", "$300", "Edit/Delete"},
                {"Dara", "Male", "C++", "$300", "Edit/Delete"},
                {"Bora", "Male", "C#", "$300", "Edit/Delete"},
        };
        JTable table = new JTable(data, columns);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        dataTable.add(tableScroll, BorderLayout.CENTER);

// Add RoundedPanel to the wrapper panel
        wrapperPanel.add(dataTable, BorderLayout.CENTER);

// Add the wrapper panel to the main content
        mainContent.add(wrapperPanel, BorderLayout.CENTER);



        // Notice board
        JPanel noticeBoard = new RoundedPanel(20, Color.WHITE, false);
        noticeBoard.setLayout(new BorderLayout());
        noticeBoard.setBorder(BorderFactory.createTitledBorder("Notice Board"));
        noticeBoard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea noticeText = new JTextArea("04/10/2021\nHidemode Now\n\n03/10/2021\nFurther Reading\n");
        noticeText.setEditable(false);
        noticeBoard.add(new JScrollPane(noticeText), BorderLayout.CENTER);

        //mainContent.add(noticeBoard, BorderLayout.EAST);

        // Adding panels to the frame
        add(sidebarScrollPane, BorderLayout.WEST);
        add(topbar, BorderLayout.NORTH);
        add(mainContent, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
//        JFrame frame = new JFrame("Dashboard");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(800, 600);
//        frame.add(new DashboardPage());
//        frame.setVisible(true);
    }
}