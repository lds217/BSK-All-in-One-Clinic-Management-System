package BsK.client.ui.component.CheckUpPage;

import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;

import javax.swing.*;
import java.awt.*;

public class CheckUpPage extends JPanel {
    private boolean debugMode = false; // Enable debug mode

    public CheckUpPage(MainFrame mainFrame) {
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

                if (debugMode) {
                    drawBoundingBox(g2d, this);
                }
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
        JPanel topbar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (debugMode) {
                    drawBoundingBox((Graphics2D) g, this);
                }
            }
        };
        topbar.setLayout(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Check Up");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        topbar.add(title, BorderLayout.WEST);

        JLabel userInfo = new JLabel("User Name (Admin)");
        userInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        topbar.add(userInfo, BorderLayout.EAST);

        // Main content panel
        JPanel mainContent = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (debugMode) {
                    drawBoundingBox((Graphics2D) g, this);
                }
            }
        };
        mainContent.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Data table inside a RoundedPanel
        RoundedPanel dataTable1 = new RoundedPanel(20, Color.WHITE, false) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (debugMode) {
                    drawBoundingBox((Graphics2D) g, this);
                }
            }
        };

        RoundedPanel dataTable2 = new RoundedPanel(20, Color.WHITE, false) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (debugMode) {
                    drawBoundingBox((Graphics2D) g, this);
                }
            }
        };

        JLabel titleText1 = new JLabel();
        titleText1.setText("Check Up Queue 1");
        titleText1.setFont(new Font("Arial", Font.BOLD, 16));
        titleText1.setBackground(Color.WHITE);
        titleText1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        JLabel titleText2 = new JLabel();
        titleText2.setText("Check Up Queue 2");
        titleText2.setFont(new Font("Arial", Font.BOLD, 16));
        titleText2.setBackground(Color.WHITE);
        titleText2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        dataTable1.setLayout(new BorderLayout());
        dataTable1.add(titleText1, BorderLayout.NORTH);
        dataTable1.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10)); // Adjust padding for better appearance

        dataTable2.setLayout(new BorderLayout());
        dataTable2.add(titleText2, BorderLayout.NORTH);
        dataTable2.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10)); // Adjust padding for better appearance

        String[] columns = {"Name", "Gender", "Course"};
        String[][] data = {
                {"John", "Male", "Java"},
                {"Dara", "Male", "C++"},
                {"Bora", "Male", "C#"},
        };
        JTable table1 = new JTable(data, columns);
        JScrollPane tableScroll1 = new JScrollPane(table1);
        tableScroll1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        dataTable1.add(tableScroll1, BorderLayout.CENTER);

        JTable table2 = new JTable(data, columns);
        JScrollPane tableScroll2 = new JScrollPane(table2);
        tableScroll2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        dataTable2.add(tableScroll2, BorderLayout.CENTER);

        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder()); // Remove border
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dataTable1, dataTable2);
        splitPane.setResizeWeight(0.5); // Split

        // Add the split pane to the main content
        mainContent.add(splitPane, BorderLayout.CENTER);
        // Notice board
        JPanel noticeBoard = new RoundedPanel(20, Color.WHITE, false) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (debugMode) {
                    drawBoundingBox((Graphics2D) g, this);
                }
            }
        };
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

    private void drawBoundingBox(Graphics2D g2d, JComponent component) {
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(0, 0, component.getWidth() - 1, component.getHeight() - 1);
    }

    public static void main(String[] args) {
//        JFrame frame = new JFrame("Dashboard");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(800, 600);
//        frame.add(new CheckUpPage();
//        frame.setVisible(true);
    }
}