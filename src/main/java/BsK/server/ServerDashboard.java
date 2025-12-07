package BsK.server;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import BsK.server.network.entity.ClientConnection;
import BsK.server.network.manager.SessionManager;
import BsK.common.util.date.DateUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerDashboard extends JFrame {
    private JTextPane logArea;
    private JLabel statusLabel;
    private JLabel portLabel;
    private JLabel clientsLabel;
    private JLabel memoryLabel;
    private JLabel cpuLabel;
    private JLabel googleDriveLabel;
    private JButton googleDriveButton;
    private JToggleButton autoScrollToggle;
    private JTextField searchField;
    private static int connectedClients = 0;
    private static ServerDashboard instance;
    // Use Vietnam timezone (UTC+7) for consistent time display
    private final SimpleDateFormat timeFormat = DateUtils.createVietnamDateFormat("HH:mm:ss");
    private Timer statsTimer;
    private Timer networkStatsTimer;
    private boolean isAutoScrollEnabled = true;
    private JTable networkTable;
    private DefaultTableModel networkTableModel;
    private JButton backupDbButton;
    
    // New monitoring fields
    private JLabel uptimeLabel;
    private JLabel diskSpaceLabel;
    private JLabel dbSizeLabel;
    private JLabel peakConnectionsLabel;
    private final long serverStartTime = System.currentTimeMillis();
    private int peakConnections = 0;

    public static ServerDashboard getInstance() {
        if (instance == null) {
            instance = new ServerDashboard();
        }
        return instance;
    }

    private ServerDashboard() {
        String version = getServerVersion();
        String title = "Bảng Điều Khiển Máy Chủ BSK";
        if (!"N/A".equals(version)) {
            title += " - Phiên bản " + version;
        }
        setTitle(title);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // Add custom close logic
        addCustomCloseListener();

        // Main container panel
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Add main components
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        topPanel.add(createMonitoringPanel(), BorderLayout.CENTER);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(createMainContentPanel(), BorderLayout.CENTER);

        add(mainPanel);

        // Start timers
        startSystemStatsTimer();
        startNetworkStatsTimer();
    }
    
    // Unchanged methods from your original code are here...
    // createHeaderPanel(), performDatabaseBackup(), createMainContentPanel(), etc.
    // I've omitted them for brevity but they should remain in your file.
    
    // --- START OF UNCHANGED METHODS (for context) ---
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        
        // Top row - Status and Clients
        JPanel topRow = new JPanel(new GridLayout(1, 4, 10, 0));
        Font headerFont = new Font("Arial", Font.BOLD, 16);
        
        statusLabel = new JLabel("Trạng thái: Đang khởi động...", SwingConstants.CENTER);
        statusLabel.setFont(headerFont);
        statusLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Trạng thái máy chủ", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        portLabel = new JLabel("Cổng: --", SwingConstants.CENTER);
        portLabel.setFont(headerFont);
        portLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Cổng kết nối", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        clientsLabel = new JLabel("Máy con: 0", SwingConstants.CENTER);
        clientsLabel.setFont(headerFont);
        clientsLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Số máy kết nối", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        googleDriveLabel = new JLabel("Đang kiểm tra...", SwingConstants.CENTER);
        googleDriveLabel.setFont(headerFont);
        googleDriveLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Google Drive", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        topRow.add(statusLabel);
        topRow.add(portLabel);
        topRow.add(clientsLabel);
        topRow.add(googleDriveLabel);
        
        // Bottom row - System info and actions
        JPanel bottomRow = new JPanel(new GridLayout(1, 4, 10, 0));
        
        memoryLabel = new JLabel("Bộ nhớ: --", SwingConstants.CENTER);
        memoryLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        memoryLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY), 
            "RAM", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 13)));
        
        cpuLabel = new JLabel("CPU: --", SwingConstants.CENTER);
        cpuLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        cpuLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY), 
            "CPU", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 13)));
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 1));
        googleDriveButton = new JButton("Thử lại kết nối");
        googleDriveButton.setFont(new Font("Arial", Font.BOLD, 14));
        googleDriveButton.addActionListener(e -> retryGoogleDriveConnection());
        buttonPanel.add(googleDriveButton);
        buttonPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY), 
            "Hành động Drive", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 13)));
        
        backupDbButton = new JButton("Sao lưu DB lên Drive");
        backupDbButton.setFont(new Font("Arial", Font.BOLD, 14));
        backupDbButton.setToolTipText("Tải bản sao cơ sở dữ liệu lên Google Drive");
        backupDbButton.addActionListener(e -> performDatabaseBackup());
        JPanel backupPanel = new JPanel(new GridLayout(1, 1));
        backupPanel.add(backupDbButton);
        backupPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY), 
            "Sao lưu dữ liệu", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 13)));
        
        bottomRow.add(memoryLabel);
        bottomRow.add(cpuLabel);
        bottomRow.add(buttonPanel);
        bottomRow.add(backupPanel);
        
        headerPanel.add(topRow, BorderLayout.NORTH);
        headerPanel.add(bottomRow, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    private JPanel createMonitoringPanel() {
        JPanel monitoringPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        Font monitorFont = new Font("Arial", Font.PLAIN, 15);
        
        uptimeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        uptimeLabel.setFont(monitorFont);
        uptimeLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Thời gian hoạt động", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        diskSpaceLabel = new JLabel("-- GB", SwingConstants.CENTER);
        diskSpaceLabel.setFont(monitorFont);
        diskSpaceLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Dung lượng còn lại", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        dbSizeLabel = new JLabel("-- MB", SwingConstants.CENTER);
        dbSizeLabel.setFont(monitorFont);
        dbSizeLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Kích thước CSDL", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        peakConnectionsLabel = new JLabel("Đỉnh: 0", SwingConstants.CENTER);
        peakConnectionsLabel.setFont(monitorFont);
        peakConnectionsLabel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Kết nối đồng thời", 
            TitledBorder.CENTER, 
            TitledBorder.TOP, 
            new Font("Arial", Font.PLAIN, 14)));
        
        monitoringPanel.add(uptimeLabel);
        monitoringPanel.add(diskSpaceLabel);
        monitoringPanel.add(dbSizeLabel);
        monitoringPanel.add(peakConnectionsLabel);
        
        return monitoringPanel;
    }
    
    private void performDatabaseBackup() {
        // Disable button to prevent multiple clicks
        backupDbButton.setEnabled(false);
        addLog("Bắt đầu sao lưu cơ sở dữ liệu lên Google Drive...");

        // Run the backup in a background thread to not freeze the UI
        new Thread(() -> {
            try {
                // Call the static method in the Server class
                BsK.server.Server.backupDatabaseToDrive();

                // Update UI on success
                SwingUtilities.invokeLater(() -> {
                    addLog("Sao lưu cơ sở dữ liệu thành công.");
                    JOptionPane.showMessageDialog(this,
                            "Cơ sở dữ liệu đã được sao lưu thành công lên Google Drive.",
                            "Sao lưu thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                log.error("Database backup failed", e);
                // Update UI on failure
                SwingUtilities.invokeLater(() -> {
                    addLog("Sao lưu cơ sở dữ liệu thất bại: " + e.getMessage());
                        JOptionPane.showMessageDialog(this,
                                "Không thể sao lưu cơ sở dữ liệu.\nLỗi: " + e.getMessage() + "\n\nKiểm tra nhật ký để biết thêm chi tiết.",
                                "Sao lưu thất bại",
                                JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                // Always re-enable the button on the UI thread
                SwingUtilities.invokeLater(() -> backupDbButton.setEnabled(true));
            }
        }).start();
    }

    private JSplitPane createMainContentPanel() {
        // --- Log Panel ---
        JPanel logPanel = new JPanel(new BorderLayout());
        TitledBorder logBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2), 
            "Nhật ký máy chủ", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            new Font("Arial", Font.BOLD, 16));
        logPanel.setBorder(logBorder);
        
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logPanel.add(createLogControlsPanel(), BorderLayout.NORTH);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // --- Network Panel ---
        JPanel networkPanel = new JPanel(new BorderLayout());
        TitledBorder networkBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.DARK_GRAY, 2), 
            "Thông tin kết nối", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            new Font("Arial", Font.BOLD, 16));
        networkPanel.setBorder(networkBorder);
        
        String[] columnNames = {"ID Phiên", "Địa chỉ IP", "Cổng", "Vai trò", "Thời gian kết nối", "Hoạt động cuối"};
        networkTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        networkTable = new JTable(networkTableModel);
        networkTable.setFont(new Font("Arial", Font.PLAIN, 14));
        networkTable.setRowHeight(28);
        networkTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        networkTable.setFillsViewportHeight(true);
        networkPanel.add(new JScrollPane(networkTable), BorderLayout.CENTER);

        // --- Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logPanel, networkPanel);
        splitPane.setResizeWeight(0.75); // Give 75% of space to the logs, 25% to network table
        splitPane.setDividerSize(8);
        
        return splitPane;
    }

    private JPanel createLogControlsPanel() {
        JPanel logControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        Font controlFont = new Font("Arial", Font.PLAIN, 14);
        
        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(controlFont);
        
        searchField = new JTextField(20);
        searchField.setFont(controlFont);
        
        JButton searchButton = new JButton("Tìm");
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.addActionListener(e -> searchInLogs());
        
        JButton clearButton = new JButton("Xóa nhật ký");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.addActionListener(e -> clearLogs());
        
        autoScrollToggle = new JToggleButton("Tự động cuộn", true);
        autoScrollToggle.setFont(new Font("Arial", Font.BOLD, 14));
        autoScrollToggle.addActionListener(e -> isAutoScrollEnabled = autoScrollToggle.isSelected());

        logControlsPanel.add(searchLabel);
        logControlsPanel.add(searchField);
        logControlsPanel.add(searchButton);
        logControlsPanel.add(Box.createHorizontalStrut(20));
        logControlsPanel.add(clearButton);
        logControlsPanel.add(Box.createHorizontalStrut(20));
        logControlsPanel.add(autoScrollToggle);
        return logControlsPanel;
    }

    private void startSystemStatsTimer() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        statsTimer = new Timer(2000, e -> {
            // Memory stats
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
            SwingUtilities.invokeLater(() ->
                memoryLabel.setText(String.format("Bộ nhớ: %d MB / %d MB", usedMemory, maxMemory))
            );

            // CPU stats
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
                SwingUtilities.invokeLater(() ->
                    cpuLabel.setText(String.format("CPU: %.1f%%", cpuLoad))
                );
            }
            
            // Uptime
            long uptimeSeconds = (System.currentTimeMillis() - serverStartTime) / 1000;
            long hours = uptimeSeconds / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;
            long seconds = uptimeSeconds % 60;
            String uptime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            SwingUtilities.invokeLater(() -> uptimeLabel.setText(uptime));
            
            // Disk space
            try {
                java.io.File root = new java.io.File(".");
                long freeSpace = root.getFreeSpace() / (1024 * 1024 * 1024); // GB
                long totalSpace = root.getTotalSpace() / (1024 * 1024 * 1024); // GB
                String diskInfo = String.format("%d GB / %d GB", freeSpace, totalSpace);
                
                // Alert if low disk space
                Color diskColor = Color.BLACK;
                if (freeSpace < 5) {
                    diskColor = Color.RED;
                    if (uptimeSeconds % 60 == 0) { // Log warning every minute
                        addLog("CẢNH BÁO: Dung lượng đĩa thấp - Chỉ còn " + freeSpace + " GB!");
                    }
                } else if (freeSpace < 10) {
                    diskColor = Color.ORANGE;
                }
                
                Color finalDiskColor = diskColor;
                SwingUtilities.invokeLater(() -> {
                    diskSpaceLabel.setText(diskInfo);
                    diskSpaceLabel.setForeground(finalDiskColor);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> diskSpaceLabel.setText("N/A"));
            }
            
            // Database size
            try {
                java.io.File dbFile = new java.io.File("database/BSK.db");
                if (dbFile.exists()) {
                    long dbSize = dbFile.length() / (1024 * 1024); // MB
                    SwingUtilities.invokeLater(() -> 
                        dbSizeLabel.setText(String.format("%d MB", dbSize))
                    );
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> dbSizeLabel.setText("N/A"));
            }
            
            // Peak connections
            if (connectedClients > peakConnections) {
                peakConnections = connectedClients;
                SwingUtilities.invokeLater(() -> 
                    peakConnectionsLabel.setText(String.format("Đỉnh: %d", peakConnections))
                );
            }
            
            // Memory alert
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            if (memoryUsage > 90 && uptimeSeconds % 60 == 0) {
                addLog("CẢNH BÁO: Bộ nhớ cao - " + String.format("%.1f%%", memoryUsage));
            }
        });
        statsTimer.start();
    }

    private void startNetworkStatsTimer() {
        networkStatsTimer = new Timer(1000, e -> refreshNetworkTable());
        networkStatsTimer.start();
    }

    public void refreshNetworkTable() {
        SwingUtilities.invokeLater(() -> {
            networkTableModel.setRowCount(0);
            for (ClientConnection conn : SessionManager.getAllConnections()) {
                Vector<Object> row = new Vector<>();
                row.add(conn.getSessionId());
                row.add(conn.getIpAddress());
                row.add(conn.getPort());
                row.add(conn.getUserRole());
                row.add(conn.getConnectionDuration());
                row.add(conn.getLastActivityDuration());
                networkTableModel.addRow(row);
            }
        });
    }

    private void searchInLogs() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            return;
        }

        try {
            String content = logArea.getDocument().getText(0, logArea.getDocument().getLength());
            logArea.getHighlighter().removeAllHighlights();

            int index = content.toLowerCase().indexOf(searchText);
            while (index >= 0) {
                logArea.getHighlighter().addHighlight(
                        index,
                        index + searchText.length(),
                        new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW)
                );
                index = content.toLowerCase().indexOf(searchText, index + 1);
            }
        } catch (BadLocationException e) {
            log.error("Error searching logs", e);
        }
    }

    private void clearLogs() {
        SwingUtilities.invokeLater(() -> {
            try {
                logArea.getDocument().remove(0, logArea.getDocument().getLength());
                addLog("Đã xóa nhật ký");
            } catch (BadLocationException e) {
                log.error("Error clearing logs", e);
            }
        });
    }

    public void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setForeground(color);
        });
    }

    public void updatePort(int port) {
        SwingUtilities.invokeLater(() ->
            portLabel.setText("Cổng: " + port)
        );
    }

    public static void incrementClients() {
        connectedClients++;
        updateClientCount();
    }

    public static void decrementClients() {
        connectedClients = Math.max(0, connectedClients - 1);
        updateClientCount();
    }

    private static void updateClientCount() {
        if (instance != null) {
            SwingUtilities.invokeLater(() -> {
                instance.clientsLabel.setText("Máy con: " + connectedClients);
                instance.refreshNetworkTable();
            });
        }
    }

    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                Document doc = logArea.getDocument();
                String timestamp = timeFormat.format(new Date());
                String formattedMessage = String.format("[%s] %s%n", timestamp, message);

                Style style = logArea.addStyle("Log Style", null);
                StyleConstants.setFontFamily(style, "Monospace");
                StyleConstants.setFontSize(style, 12);

                doc.insertString(doc.getLength(), formattedMessage, style);

                if (isAutoScrollEnabled) {
                    logArea.setCaretPosition(doc.getLength());
                }
            } catch (BadLocationException e) {
                log.error("Error adding log message", e);
            }
        });
    }

    public void updateGoogleDriveStatus(boolean connected, String statusMessage) {
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                googleDriveLabel.setText("Đã kết nối");
                googleDriveLabel.setForeground(new Color(34, 139, 34)); // Forest Green
                googleDriveButton.setText("Kiểm tra");
                googleDriveButton.setToolTipText("Kiểm tra kết nối Google Drive");
            } else {
                googleDriveLabel.setText("Chưa kết nối");
                googleDriveLabel.setForeground(Color.RED);
                googleDriveButton.setText("Thử lại");
                googleDriveButton.setToolTipText("Thử kết nối lại Google Drive");
            }
        });
    }

    private void retryGoogleDriveConnection() {
        SwingUtilities.invokeLater(() -> {
            googleDriveLabel.setText("Đang kết nối...");
            googleDriveLabel.setForeground(Color.ORANGE);
            googleDriveButton.setEnabled(false);
            addLog("Đang kiểm tra kết nối Google Drive...");
        });

        // Run connection attempt in a background thread
        new Thread(() -> {
            try {
                // This method should attempt the connection and update the server's internal state.
                BsK.server.Server.retryGoogleDriveConnection();

                // After the attempt, get the result from the server's state
                boolean isConnected = BsK.server.Server.isGoogleDriveConnected();
                String message = isConnected ? "Kết nối thành công" : "Kết nối thất bại";

                // Call the standard UI update method with the result.
                updateGoogleDriveStatus(isConnected, message);

            } catch (Exception e) {
                log.error("Google Drive connection test failed with an exception", e);
                // Also update the UI in case of an exception
                updateGoogleDriveStatus(false, "Lỗi kết nối");
                addLog("Lỗi kết nối Google Drive: " + e.getMessage());
            } finally {
                // ALWAYS re-enable the button on the UI thread
                SwingUtilities.invokeLater(() -> googleDriveButton.setEnabled(true));
            }
        }).start();
    }

    @Override
    public void dispose() {
        if (statsTimer != null) {
            statsTimer.stop();
        }
        if (networkStatsTimer != null) {
            networkStatsTimer.stop();
        }
        super.dispose();
    }
    // --- END OF UNCHANGED METHODS ---

    // --- MODIFICATION START: Add new methods for close confirmation ---
    /**
     * Overrides the default close operation to show a confirmation dialog.
     */
    private void addCustomCloseListener() {
        // This tells the frame to do nothing automatically when the 'X' is clicked.
        // We will handle the closing manually.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showExitConfirmationDialog();
            }
        });
    }

    /**
     * Displays the confirmation dialog and exits the application if confirmed.
     */
    private void showExitConfirmationDialog() {
        String title = "Xác nhận thoát";
        String message = "Nếu tắt bạn sẽ đóng tất cả các máy con. Bạn có chắc chắn không?";

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // User confirmed, so exit the entire application.
            // This will trigger the graceful shutdown logic in your Server's main() method.
            System.exit(0);
        }
        // If the user chooses "No", the dialog closes and nothing else happens.
    }

    private String getServerVersion() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("version_server.json")));
            Pattern pattern = Pattern.compile("\"currentVersion\":\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            log.error("Error reading server version file", e);
        }
        return "N/A";
    }
    // --- MODIFICATION END ---
}