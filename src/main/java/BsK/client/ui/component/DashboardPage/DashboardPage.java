package BsK.client.ui.component.DashboardPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class DashboardPage extends JPanel {
    private final MainFrame mainFrame;
    
    // TODO: Add backend request handlers
    private JLabel todayPatientsLabel;
    private JLabel recheckPatientsLabel;
    private JLabel totalPatientsLabel;
    private JLabel pendingResultsLabel;
    private JTable recentActivitiesTable;
    private JLabel lastUpdateLabel;

    public DashboardPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // --- Navigation Bar ---
        NavBar navBar = new NavBar(mainFrame, "Thống kê");
        add(navBar, BorderLayout.NORTH);

        // --- Main Content Panel ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainContent.setOpaque(false);

        // Create main sections
        JPanel topSection = createTopSection();
        JPanel centerSection = createCenterSection();
        JPanel bottomSection = createBottomSection();

        mainContent.add(topSection, BorderLayout.NORTH);
        mainContent.add(centerSection, BorderLayout.CENTER);
        mainContent.add(bottomSection, BorderLayout.SOUTH);

        add(mainContent, BorderLayout.CENTER);

        // TODO: Initialize with real data from backend
        loadDashboardData();
    }

    private JPanel createTopSection() {
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Welcome header
        JPanel headerPanel = createHeaderPanel();
        
        // Patient metrics cards
        JPanel metricsPanel = createMetricsPanel();

        topSection.add(headerPanel, BorderLayout.NORTH);
        topSection.add(metricsPanel, BorderLayout.CENTER);

        return topSection;
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel welcomeLabel = new JLabel("Chào mừng, " + LocalStorage.username);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcomeLabel.setForeground(new Color(44, 82, 130));

        lastUpdateLabel = new JLabel("Cập nhật lần cuối: " + getCurrentTime());
        lastUpdateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lastUpdateLabel.setForeground(new Color(107, 114, 128));

        JButton refreshButton = new JButton("Làm mới");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setBackground(new Color(59, 130, 246));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> {
            // TODO: Implement refresh data from backend
            refreshDashboardData();
        });

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(welcomeLabel, BorderLayout.NORTH);
        leftPanel.add(lastUpdateLabel, BorderLayout.SOUTH);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(refreshButton, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createMetricsPanel() {
        JPanel metricsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        metricsPanel.setOpaque(false);

        // Patient metrics cards
        String[] cardTitles = {"Bệnh nhân hôm nay", "Cần tái khám", "Tổng bệnh nhân", "Chờ kết quả"};
        Color[] cardColors = {
            new Color(59, 130, 246),   // Blue - Today's patients
            new Color(245, 158, 11),   // Orange - Recheck patients  
            new Color(16, 185, 129),   // Green - Total patients
            new Color(139, 92, 246)    // Purple - Pending results
        };
        String[] cardIcons = {"👥", "🔄", "📊", "⏳"};

        JLabel[] valueLabels = new JLabel[4];

        for (int i = 0; i < cardTitles.length; i++) {
            RoundedPanel card = createMetricCard(cardTitles[i], cardColors[i], cardIcons[i]);
            
            // Store references to value labels for updating
            Component[] components = card.getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    Component[] subComps = ((JPanel) comp).getComponents();
                    for (Component subComp : subComps) {
                        if (subComp instanceof JLabel && ((JLabel) subComp).getFont().getSize() == 32) {
                            valueLabels[i] = (JLabel) subComp;
                            break;
                        }
                    }
                }
            }
            
            metricsPanel.add(card);
        }

        // Store references for updating data
        todayPatientsLabel = valueLabels[0];
        recheckPatientsLabel = valueLabels[1];
        totalPatientsLabel = valueLabels[2];
        pendingResultsLabel = valueLabels[3];

        return metricsPanel;
    }

    private RoundedPanel createMetricCard(String title, Color color, String icon) {
        RoundedPanel card = new RoundedPanel(15, color, true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Icon and title panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        topPanel.add(iconLabel, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        // Value label
        JLabel valueLabel = new JLabel("--");
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        // Add hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(color.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(color);
            }
        });

        return card;
    }

    private JPanel createCenterSection() {
        JPanel centerSection = new JPanel(new GridLayout(1, 2, 20, 0));
        centerSection.setOpaque(false);
        centerSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Recent activities panel
        JPanel activitiesPanel = createRecentActivitiesPanel();
        
        // Quick actions panel
        JPanel actionsPanel = createQuickActionsPanel();

        centerSection.add(activitiesPanel);
        centerSection.add(actionsPanel);

        return centerSection;
    }

    private JPanel createRecentActivitiesPanel() {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Hoạt động gần đây");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));

        // Table for recent activities
        String[] columns = {"Thời gian", "Bệnh nhân", "Hoạt động", "Trạng thái"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentActivitiesTable = new JTable(tableModel);
        recentActivitiesTable.setRowHeight(35);
        recentActivitiesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        recentActivitiesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recentActivitiesTable.setSelectionBackground(new Color(219, 234, 254));
        recentActivitiesTable.setGridColor(new Color(229, 231, 235));

        JScrollPane tableScroll = new JScrollPane(recentActivitiesTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(15), BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createQuickActionsPanel() {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Thao tác nhanh");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));

        JPanel actionsGrid = new JPanel(new GridLayout(3, 2, 15, 15));
        actionsGrid.setOpaque(false);
        actionsGrid.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // Quick action buttons
        String[] actionTitles = {"Thêm bệnh nhân", "Xem hàng đợi", "Báo cáo ngày", "Tìm bệnh nhân", "Cài đặt", "Khẩn cấp"};
        String[] actionIcons = {"➕", "📋", "📊", "🔍", "⚙️", "🚨"};
        Color[] actionColors = {
            new Color(34, 197, 94),   // Green - Add patient
            new Color(59, 130, 246),  // Blue - View queue
            new Color(168, 85, 247),  // Purple - Reports
            new Color(245, 158, 11),  // Orange - Search
            new Color(107, 114, 128), // Gray - Settings
            new Color(239, 68, 68)    // Red - Emergency
        };

        for (int i = 0; i < actionTitles.length; i++) {
            JButton actionButton = createQuickActionButton(actionTitles[i], actionIcons[i], actionColors[i]);
            final int index = i;
            actionButton.addActionListener(e -> handleQuickAction(index));
            actionsGrid.add(actionButton);
        }

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(actionsGrid, BorderLayout.CENTER);

        return panel;
    }

    private JButton createQuickActionButton(String title, String icon, Color color) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        button.add(iconLabel, BorderLayout.CENTER);
        button.add(titleLabel, BorderLayout.SOUTH);

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private JPanel createBottomSection() {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Lịch hẹn hôm nay");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));

        // TODO: Create appointment calendar widget
        JPanel appointmentPanel = new JPanel(new BorderLayout());
        appointmentPanel.setOpaque(false);
        appointmentPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JLabel placeholderLabel = new JLabel("Tính năng lịch hẹn đang được phát triển...");
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        placeholderLabel.setForeground(new Color(107, 114, 128));
        placeholderLabel.setHorizontalAlignment(SwingConstants.CENTER);

        appointmentPanel.add(placeholderLabel, BorderLayout.CENTER);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(appointmentPanel, BorderLayout.CENTER);

        return panel;
    }

    // TODO: Implement backend data loading
    private void loadDashboardData() {
        // Placeholder data - replace with real backend calls
        updateMetrics(12, 3, 156, 5);
        loadRecentActivities();
    }

    private void updateMetrics(int todayPatients, int recheckPatients, int totalPatients, int pendingResults) {
        if (todayPatientsLabel != null) todayPatientsLabel.setText(String.valueOf(todayPatients));
        if (recheckPatientsLabel != null) recheckPatientsLabel.setText(String.valueOf(recheckPatients));
        if (totalPatientsLabel != null) totalPatientsLabel.setText(String.valueOf(totalPatients));
        if (pendingResultsLabel != null) pendingResultsLabel.setText(String.valueOf(pendingResults));
    }

    private void loadRecentActivities() {
        DefaultTableModel model = (DefaultTableModel) recentActivitiesTable.getModel();
        model.setRowCount(0); // Clear existing data

        // TODO: Replace with real data from backend
        String[][] sampleData = {
            {"10:30", "Nguyễn Văn A", "Khám tổng quát", "Hoàn thành"},
            {"10:15", "Trần Thị B", "Siêu âm", "Đang thực hiện"},
            {"09:45", "Lê Văn C", "Tái khám", "Chờ kết quả"},
            {"09:30", "Phạm Thị D", "Khám thai", "Hoàn thành"},
            {"09:00", "Hoàng Văn E", "Khám tổng quát", "Hoàn thành"}
        };

        for (String[] row : sampleData) {
            model.addRow(row);
        }
    }

    private void refreshDashboardData() {
        lastUpdateLabel.setText("Cập nhật lần cuối: " + getCurrentTime());
        // TODO: Implement real data refresh from backend
        loadDashboardData();
        
        JOptionPane.showMessageDialog(this, 
            "Đã làm mới dữ liệu thành công!", 
            "Thông báo", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleQuickAction(int actionIndex) {
        String[] actions = {"AddPatient", "CheckUpPage", "Reports", "SearchPatient", "SettingsPage", "Emergency"};
        String[] messages = {
            "Chuyển đến trang thêm bệnh nhân...",
            "Chuyển đến trang xem hàng đợi...", 
            "Mở báo cáo ngày...",
            "Mở tìm kiếm bệnh nhân...",
            "Chuyển đến trang cài đặt...",
            "Kích hoạt chế độ khẩn cấp!"
        };

        // TODO: Implement real navigation and actions
        switch (actionIndex) {
            case 0: // Add Patient - Navigate to patient data page
                mainFrame.showPage("PatientDataPage");
                break;
            case 1: // View Queue - Navigate to checkup page
                mainFrame.showPage("CheckUpPage");
                break;
            case 4: // Settings - Navigate to settings page
                mainFrame.showPage("SettingsPage");
                break;
            default:
                JOptionPane.showMessageDialog(this, 
                    messages[actionIndex], 
                    "Thông báo", 
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public static void main(String[] args) {
        // Test frame for development
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dashboard Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            
            LocalStorage.username = "Test User"; // Set test username
            DashboardPage dashboard = new DashboardPage(null);
            frame.add(dashboard);
            frame.setVisible(true);
        });
    }
}