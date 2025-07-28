package BsK.client.ui.component.DashboardPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.common.AddDialog.AddDialog;
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
    
    private JLabel todayPatientsLabel;
    private JLabel waitingPatientsLabel;
    private JLabel recheckPatientsLabel;
    private JLabel totalPatientsLabel;
    private JTable currentQueueTable;
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

        mainContent.add(topSection, BorderLayout.NORTH);
        mainContent.add(centerSection, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        // Load initial data
        loadDashboardData();
    }

    private JPanel createTopSection() {
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

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
        refreshButton.addActionListener(e -> refreshDashboardData());

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

        // Patient metrics cards with specific titles
        String[] cardTitles = {"Bệnh nhân trong ngày", "Đang đợi khám", "Cần tái khám", "Tổng bệnh nhân"};
        Color[] cardColors = {
            new Color(59, 130, 246),   // Blue - Today's patients
            new Color(245, 158, 11),   // Orange - Waiting patients  
            new Color(139, 92, 246),   // Purple - Recheck patients
            new Color(16, 185, 129)    // Green - Total patients
        };
        String[] cardIcons = {"👥", "⏳", "🔄", "📊"};

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
        waitingPatientsLabel = valueLabels[1];
        recheckPatientsLabel = valueLabels[2];
        totalPatientsLabel = valueLabels[3];

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

        // Current queue panel (left side)
        JPanel queuePanel = createCurrentQueuePanel();
        
        // Action buttons panel (right side)
        JPanel actionsPanel = createActionButtonsPanel();

        centerSection.add(queuePanel);
        centerSection.add(actionsPanel);

        return centerSection;
    }

    private JPanel createCurrentQueuePanel() {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Hàng đợi hiện tại");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));

        // Table for current queue
        String[] columns = {"STT", "Mã BN", "Họ và Tên", "Loại khám", "Trạng thái"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        currentQueueTable = new JTable(tableModel);
        currentQueueTable.setRowHeight(35);
        currentQueueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        currentQueueTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        currentQueueTable.setSelectionBackground(new Color(219, 234, 254));
        currentQueueTable.setGridColor(new Color(229, 231, 235));

        JScrollPane tableScroll = new JScrollPane(currentQueueTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(15), BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActionButtonsPanel() {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Thao tác nhanh");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));

        JPanel actionsGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        actionsGrid.setOpaque(false);
        actionsGrid.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        // 4 specific action buttons
        String[] actionTitles = {"Thêm bệnh nhân", "Gọi bệnh nhân tái khám", "Báo cáo ngày", "Khẩn cấp"};
        String[] actionIcons = {"➕", "📞", "📊", "🚨"};
        Color[] actionColors = {
            new Color(34, 197, 94),   // Green - Add patient
            new Color(59, 130, 246),  // Blue - Call recheck patient
            new Color(168, 85, 247),  // Purple - Daily report
            new Color(239, 68, 68)    // Red - Emergency
        };

        for (int i = 0; i < actionTitles.length; i++) {
            JButton actionButton = createActionButton(actionTitles[i], actionIcons[i], actionColors[i]);
            final int index = i;
            actionButton.addActionListener(e -> handleAction(index));
            actionsGrid.add(actionButton);
        }

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(actionsGrid, BorderLayout.CENTER);

        return panel;
    }

    private JButton createActionButton(String title, String icon, Color color) {
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

    private void loadDashboardData() {
        // Load metrics with sample data
        updateMetrics(12, 5, 3, 156);
        loadCurrentQueue();
    }

    private void updateMetrics(int todayPatients, int waitingPatients, int recheckPatients, int totalPatients) {
        if (todayPatientsLabel != null) todayPatientsLabel.setText(String.valueOf(todayPatients));
        if (waitingPatientsLabel != null) waitingPatientsLabel.setText(String.valueOf(waitingPatients));
        if (recheckPatientsLabel != null) recheckPatientsLabel.setText(String.valueOf(recheckPatients));
        if (totalPatientsLabel != null) totalPatientsLabel.setText(String.valueOf(totalPatients));
    }

    private void loadCurrentQueue() {
        DefaultTableModel model = (DefaultTableModel) currentQueueTable.getModel();
        model.setRowCount(0); // Clear existing data

        // Sample queue data
        String[][] queueData = {
            {"1", "BN-00123", "Nguyễn Văn An", "Khám tổng quát", "Đang chờ"},
            {"2", "BN-00124", "Trần Thị Bình", "Siêu âm", "Đang khám"},
            {"3", "BN-00125", "Lê Hoàng Cường", "Tái khám", "Đang chờ"},
            {"4", "BN-00126", "Phạm Thị Dung", "Khám thai", "Đang chờ"},
            {"5", "BN-00127", "Hoàng Văn Em", "Khám tổng quát", "Đang chờ"}
        };

        for (String[] row : queueData) {
            model.addRow(row);
        }
    }

    private void refreshDashboardData() {
        lastUpdateLabel.setText("Cập nhật lần cuối: " + getCurrentTime());
        loadDashboardData();
        
        JOptionPane.showMessageDialog(this, 
            "Đã làm mới dữ liệu thành công!", 
            "Thông báo", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleAction(int actionIndex) {
        String[] messages = {
            "Chuyển đến trang thêm bệnh nhân...",
            "Gọi bệnh nhân tái khám...", 
            "Mở báo cáo ngày...",
            "Kích hoạt chế độ khẩn cấp!"
        };

        switch (actionIndex) {
            case 0: // Add Patient
                AddDialog addDialog = new AddDialog(mainFrame);
                addDialog.setVisible(true);
                break;
            case 1: // Call recheck patient
                JOptionPane.showMessageDialog(this, 
                    "Chức năng gọi bệnh nhân tái khám đang được phát triển", 
                    "Thông báo", 
                    JOptionPane.INFORMATION_MESSAGE);
                break;
            case 2: // Daily report
                JOptionPane.showMessageDialog(this, 
                    "Chức năng báo cáo ngày đang được phát triển", 
                    "Thông báo", 
                    JOptionPane.INFORMATION_MESSAGE);
                break;
            case 3: // Emergency
                JOptionPane.showMessageDialog(this, 
                    "Chế độ khẩn cấp được kích hoạt!", 
                    "KHẨN CẤP", 
                    JOptionPane.WARNING_MESSAGE);
                break;
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}