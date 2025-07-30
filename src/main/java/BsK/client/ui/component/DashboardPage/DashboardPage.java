package BsK.client.ui.component.DashboardPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.common.AddDialog.AddDialog;
import BsK.client.ui.component.DashboardPage.RecheckUpDialog.RecheckUpDialog;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Patient;
import BsK.common.packet.req.GetCheckUpQueueUpdateRequest;
import BsK.common.packet.res.GetCheckUpQueueUpdateResponse;
import BsK.common.packet.res.TodayPatientCountResponse;
import BsK.common.packet.res.RecheckCountResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DashboardPage extends JPanel {
    private final MainFrame mainFrame;

    private JLabel todayPatientsLabel;
    private JLabel waitingPatientsLabel;
    private JLabel recheckPatientsLabel;
    private JLabel dateTimeLabel;
    private JTable currentQueueTable;
    private JLabel lastUpdateLabel;
    private Timer realTimeClockTimer;

    private final List<Patient> patientQueue = new ArrayList<>();

    // --- LISTENERS ---
    private final ResponseListener<GetCheckUpQueueUpdateResponse> checkUpQueueUpdateListener = this::handleGetCheckUpQueueUpdateResponse;
    private final ResponseListener<TodayPatientCountResponse> todayPatientCountListener = this::handleTodayPatientCountResponse;
    private final ResponseListener<RecheckCountResponse> recheckCountListener = this::handleRecheckCountResponse;

    private void handleGetCheckUpQueueUpdateResponse(GetCheckUpQueueUpdateResponse response) {
        SwingUtilities.invokeLater(() -> {
            log.info("Received dashboard queue update from server.");
            this.patientQueue.clear();
            if (response.getQueue() != null) {
                for (String[] patientData : response.getQueue()) {
                    try {
                        this.patientQueue.add(new Patient(patientData));
                    } catch (IllegalArgumentException e) {
                        log.error("Received malformed patient data for queue: {}", (Object) patientData, e);
                    }
                }
            }
            updateQueueTable();
            lastUpdateLabel.setText("Cập nhật lần cuối: " + getCurrentTime());
            flashComponent(lastUpdateLabel, new Color(16, 185, 129));
        });
    }

    private void handleTodayPatientCountResponse(TodayPatientCountResponse response) {
        SwingUtilities.invokeLater(() -> {
            log.info("Received today's patient count: {}", response.getTotalPatientsToday());
            if (todayPatientsLabel != null) {
                todayPatientsLabel.setText(String.valueOf(response.getTotalPatientsToday()));
            } else {
                log.warn("todayPatientsLabel is null, cannot update UI.");
            }
        });
    }

    private void handleRecheckCountResponse(RecheckCountResponse response) {
        SwingUtilities.invokeLater(() -> {
            log.info("Received recheck count: {}", response.getRecheckCount());
            if (recheckPatientsLabel != null) {
                recheckPatientsLabel.setText(String.valueOf(response.getRecheckCount()));
            } else {
                log.warn("recheckPatientsLabel is null, cannot update UI.");
            }
        });
    }

    private void flashComponent(JComponent component, Color flashColor) {
        final Color originalColor = component.getForeground();
        component.setForeground(flashColor);
        Timer flashTimer = new Timer(1000, e -> component.setForeground(originalColor));
        flashTimer.setRepeats(false);
        flashTimer.start();
    }

    public DashboardPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        NavBar navBar = new NavBar(mainFrame, "Thống kê");
        add(navBar, BorderLayout.NORTH);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainContent.setOpaque(false);

        JPanel topSection = createTopSection();
        JPanel centerSection = createCenterSection();

        mainContent.add(topSection, BorderLayout.NORTH);
        mainContent.add(centerSection, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        loadDashboardData();
        startRealTimeClock();

        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                ClientHandler.addResponseListener(GetCheckUpQueueUpdateResponse.class, checkUpQueueUpdateListener);
                ClientHandler.addResponseListener(TodayPatientCountResponse.class, todayPatientCountListener);
                ClientHandler.addResponseListener(RecheckCountResponse.class, recheckCountListener);
                log.info("DashboardPage listeners registered. Requesting initial data.");
                NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueUpdateRequest());
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                ClientHandler.deleteListener(GetCheckUpQueueUpdateResponse.class, checkUpQueueUpdateListener);
                ClientHandler.deleteListener(TodayPatientCountResponse.class, todayPatientCountListener);
                log.info("DashboardPage listeners removed.");
            }
            
            @Override
            public void ancestorMoved(AncestorEvent event) { /* Do nothing */ }
        });
    }

    private void startRealTimeClock() {
        realTimeClockTimer = new Timer(1000, e -> updateDateTime());
        realTimeClockTimer.start();
    }

    private JPanel createTopSection() {
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        topSection.add(createHeaderPanel(), BorderLayout.NORTH);
        topSection.add(createMetricsPanel(), BorderLayout.CENTER);
        return topSection;
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        JLabel welcomeLabel = new JLabel("Xin chào, " + LocalStorage.username);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcomeLabel.setForeground(new Color(44, 82, 130));
        lastUpdateLabel = new JLabel("Cập nhật lần cuối: " + getCurrentTime());
        lastUpdateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lastUpdateLabel.setForeground(new Color(107, 114, 128));
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(welcomeLabel, BorderLayout.NORTH);
        leftPanel.add(lastUpdateLabel, BorderLayout.SOUTH);
        headerPanel.add(leftPanel, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createMetricsPanel() {
        JPanel metricsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        metricsPanel.setOpaque(false);
        String[] cardTitles = {"Bệnh nhân trong ngày", "Đang đợi khám", "Cần tái khám hôm nay", "Ngày giờ hiện tại"};
        Color[] cardColors = {
                new Color(59, 130, 246), new Color(245, 158, 11),
                new Color(139, 92, 246), new Color(16, 185, 129)
        };
        String[] cardIcons = {"👥", "⏳", "🔄", "📅"};
        JLabel[] valueLabels = new JLabel[3];

        for (int i = 0; i < cardTitles.length; i++) {
            if (i == 3) {
                metricsPanel.add(createDateTimeCard(cardTitles[i], cardColors[i], cardIcons[i]));
            } else {
                RoundedPanel card = createMetricCard(cardTitles[i], cardColors[i], cardIcons[i]);
                
                // This logic correctly finds the value label, which is a direct child of the card.
                for (Component comp : card.getComponents()) {
                    if (comp instanceof JLabel && ((JLabel) comp).getFont().getSize() == 32) {
                        valueLabels[i] = (JLabel) comp;
                        break; // Found the label, exit the inner loop
                    }
                }
                
                metricsPanel.add(card);
            }
        }

        todayPatientsLabel = valueLabels[0];
        waitingPatientsLabel = valueLabels[1];
        recheckPatientsLabel = valueLabels[2];
        return metricsPanel;
    }

    private RoundedPanel createDateTimeCard(String title, Color color, String icon) {
        RoundedPanel card = new RoundedPanel(15, color, true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(iconLabel, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        dateTimeLabel = new JLabel();
        dateTimeLabel.setForeground(Color.WHITE);
        dateTimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        dateTimeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(topPanel, BorderLayout.NORTH);
        card.add(dateTimeLabel, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { card.setBackground(color); }
        });
        return card;
    }

    private RoundedPanel createMetricCard(String title, Color color, String icon) {
        RoundedPanel card = new RoundedPanel(15, color, true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(iconLabel, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        JLabel valueLabel = new JLabel("--");
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(topPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER); // valueLabel is a direct child
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { card.setBackground(color); }
        });
        return card;
    }

    private JPanel createCenterSection() {
        JPanel centerSection = new JPanel(new GridLayout(1, 2, 20, 0));
        centerSection.setOpaque(false);
        centerSection.add(createCurrentQueuePanel());
        centerSection.add(createActionButtonsPanel());
        return centerSection;
    }

    private JPanel createCurrentQueuePanel() {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel titleLabel = new JLabel("Hàng đợi hiện tại");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(31, 41, 55));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // --- MODIFIED SECTION START ---
        String[] columns = {"STT", "Mã Khám", "Họ và Tên", "Năm sinh", "Bác sĩ", "Loại khám", "Trạng thái"};
        // --- MODIFIED SECTION END ---
        
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        currentQueueTable = new JTable(tableModel);
        currentQueueTable.setRowHeight(35);
        currentQueueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        currentQueueTable.setFont(new Font("Segoe UI", Font.BOLD, 14));
        currentQueueTable.setSelectionBackground(new Color(219, 234, 254));
        currentQueueTable.setGridColor(new Color(229, 231, 235));
        JScrollPane tableScroll = new JScrollPane(currentQueueTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    // --- MODIFIED SECTION START ---
    private void setQueueTableColumnWidths() {
        currentQueueTable.getColumnModel().getColumn(0).setPreferredWidth(40);    // STT: small
        currentQueueTable.getColumnModel().getColumn(1).setPreferredWidth(80);    // Mã Khám: small
        currentQueueTable.getColumnModel().getColumn(2).setPreferredWidth(180);   // Họ và Tên: biggest
        currentQueueTable.getColumnModel().getColumn(3).setPreferredWidth(70);    // Năm sinh: small
        currentQueueTable.getColumnModel().getColumn(4).setPreferredWidth(180);   // Bác sĩ: large
        currentQueueTable.getColumnModel().getColumn(5).setPreferredWidth(100);   // Loại khám: small
        currentQueueTable.getColumnModel().getColumn(6).setPreferredWidth(120);   // Trạng thái: small but bigger
    }
    // --- MODIFIED SECTION END ---

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
        String[] actionTitles = {"Thêm bệnh nhân", "Gọi bệnh nhân tái khám", "Báo cáo ngày", "Quản lý dịch vụ (thuốc)"};
        String[] actionIcons = {"➕", "📞", "📊", "📋"};
        Color[] actionColors = {
                new Color(34, 197, 94), new Color(59, 130, 246),
                new Color(168, 85, 247), new Color(24, 68, 68)
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
        button.setUI(new RoundedButtonUI(color, Color.WHITE, 10));
        button.setLayout(new BorderLayout());
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        button.add(iconLabel, BorderLayout.CENTER);
        button.add(titleLabel, BorderLayout.SOUTH);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { button.setBackground(color); }
        });
        return button;
    }
    
    private void loadDashboardData() {
        updateMetrics(3);
        updateDateTime();
    }
    
    private void updateMetrics(int recheckPatients) {
        if (recheckPatientsLabel != null) recheckPatientsLabel.setText(String.valueOf(recheckPatients));
    }

    private void updateDateTime() {
        if (dateTimeLabel != null) {
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            dateTimeLabel.setText("<html><div style='text-align: center;'>" + date + "<br>" + time + "</div></html>");
        }
    }

    private void updateQueueTable() {
        DefaultTableModel model = (DefaultTableModel) currentQueueTable.getModel();
        model.setRowCount(0);

        if (patientQueue != null) {
            for (Patient patient : patientQueue) {
                // --- MODIFIED SECTION START ---
                Object[] rowData = {
                        patient.getQueueNumber(),
                        patient.getCheckupId(),
                        patient.getCustomerLastName() + " " + patient.getCustomerFirstName(),
                        DateUtils.extractYearFromTimestamp(patient.getCustomerDob()),
                        patient.getDoctorName(), // Added doctor name
                        patient.getCheckupType(),
                        patient.getStatus()
                };
                // --- MODIFIED SECTION END ---
                model.addRow(rowData);
            }
        }
        
        long waitingCount = patientQueue.stream().filter(p -> "CHỜ KHÁM".equals(p.getStatus())).count() ;
        if (waitingPatientsLabel != null) {
            waitingPatientsLabel.setText(String.valueOf(waitingCount));
        }
        
        setQueueTableColumnWidths();
    }


    private void handleAction(int actionIndex) {
        switch (actionIndex) {
            case 0: // Add Patient
                AddDialog addDialog = new AddDialog(mainFrame);
                addDialog.setVisible(true);
                break;
            case 1: // Call recheck patient
                RecheckUpDialog recheckUpDialog = new RecheckUpDialog(mainFrame);
                recheckUpDialog.setVisible(true);
                break;
            case 2: // Daily report
                JOptionPane.showMessageDialog(this, 
                        "Chức năng báo cáo ngày đang được phát triển", 
                        "Thông báo", 
                        JOptionPane.INFORMATION_MESSAGE);
                break;
            case 3:
                JOptionPane.showMessageDialog(this, "Chế độ khẩn cấp được kích hoạt!", "DỊCH VỤ", JOptionPane.WARNING_MESSAGE);
                break;
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }
}