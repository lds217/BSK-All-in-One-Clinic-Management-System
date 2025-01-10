package BsK.client.ui.component.CheckUpPage;

import javax.swing.*;
import java.awt.*;

public class SidebarAnimationDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sidebar Animation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 500);
            frame.setLayout(new BorderLayout());

            // Sidebar with gradient
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
            sidebar.setLayout(new GridLayout(15, 1));
            sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            sidebar.setPreferredSize(new Dimension(0, 500)); // Initially hidden

            // Sidebar items
            String[] sidebarItems = {"Thống kê", "Thăm khám", "Dữ liệu bệnh nhân", "Kho", "Người dùng", "Thông tin"};
            String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "UserPage", "InfoPage"};
            for (int i = 0; i < sidebarItems.length; i++) {
                String item = sidebarItems[i];
                String dest = destination[i];
                JLabel label = new JLabel(item);
                label.setForeground(Color.WHITE);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.setFont(new Font("Arial", Font.BOLD, 14));

                // Add hover and click effects
                label.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        System.out.println("Navigating to: " + dest);
                        // mainFrame.showPage(dest); // Example navigation call
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

            frame.add(sidebar, BorderLayout.WEST);

            // Main content with toggle button
            JPanel mainContent = new JPanel(new BorderLayout());
            JLabel contentLabel = new JLabel("Main Content Area");
            contentLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainContent.add(contentLabel, BorderLayout.CENTER);

            JButton toggleButton = new JButton("Toggle Sidebar");
            mainContent.add(toggleButton, BorderLayout.SOUTH);

            frame.add(mainContent, BorderLayout.CENTER);

            // Sidebar animation variables
            boolean[] isSidebarOpen = {false}; // Track the sidebar state

            // Toggle button action
            toggleButton.addActionListener(e -> {
                new Timer(10, event -> {
                    int currentWidth = sidebar.getWidth();
                    if (!isSidebarOpen[0]) {
                        if (currentWidth < 200) { // Expand to target width
                            sidebar.setPreferredSize(new Dimension(currentWidth + 10, 500));
                            sidebar.revalidate();
                        } else {
                            isSidebarOpen[0] = true;
                            ((Timer) event.getSource()).stop();
                        }
                    } else {
                        if (currentWidth > 0) { // Retract to hidden
                            sidebar.setPreferredSize(new Dimension(currentWidth - 10, 500));
                            sidebar.revalidate();
                        } else {
                            isSidebarOpen[0] = false;
                            ((Timer) event.getSource()).stop();
                        }
                    }
                }).start();
            });

            frame.setVisible(true);

            // Animate sidebar on startup
            Timer timer = new Timer(10, event -> {
                int currentWidth = sidebar.getWidth();
                if (currentWidth < 200) { // Target width
                    sidebar.setPreferredSize(new Dimension(currentWidth + 10, 500));
                    sidebar.revalidate();
                } else {
                    ((Timer) event.getSource()).stop(); // Stop animation
                }
            });
            timer.start();
        });
    }
}
