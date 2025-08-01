package BsK.server;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import BsK.server.network.entity.ClientConnection;
import BsK.server.network.manager.SessionManager;
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
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private Timer statsTimer;
    private Timer networkStatsTimer;
    private boolean isAutoScrollEnabled = true;
    private JTable networkTable;
    private DefaultTableModel networkTableModel;

    // --- NEW ---
    private JButton backupDbButton;

    public static ServerDashboard getInstance() {
        if (instance == null) {
            instance = new ServerDashboard();
        }
        return instance;
    }

    private ServerDashboard() {
        setTitle("BSK Server Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main container panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add main components
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createMainContentPanel(), BorderLayout.CENTER);

        add(mainPanel);

        // Start timers
        startSystemStatsTimer();
        startNetworkStatsTimer();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.weighty = 1.0;

        // Server Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Server Status"));
        statusLabel = new JLabel("Status: Starting...", SwingConstants.CENTER);
        portLabel = new JLabel("Port: --", SwingConstants.CENTER);
        clientsLabel = new JLabel("Connected Clients: 0", SwingConstants.CENTER);
        statusPanel.add(statusLabel);
        statusPanel.add(portLabel);
        statusPanel.add(clientsLabel);
        gbc.gridx = 0;
        gbc.weightx = 0.35; // MODIFIED: Adjusted weight
        headerPanel.add(statusPanel, gbc);

        // System Info Panel
        JPanel systemPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        systemPanel.setBorder(BorderFactory.createTitledBorder("System Information"));
        memoryLabel = new JLabel("Memory Usage: --", SwingConstants.CENTER);
        cpuLabel = new JLabel("CPU Usage: --", SwingConstants.CENTER);
        JLabel osLabel = new JLabel("OS: " + System.getProperty("os.name"), SwingConstants.CENTER);
        systemPanel.add(memoryLabel);
        systemPanel.add(cpuLabel);
        systemPanel.add(osLabel);
        gbc.gridx = 1;
        gbc.weightx = 0.35; // MODIFIED: Adjusted weight
        headerPanel.add(systemPanel, gbc);

        // Google Drive Panel
        JPanel drivePanel = new JPanel(new BorderLayout(10, 0));
        drivePanel.setBorder(BorderFactory.createTitledBorder("Google Drive"));
        googleDriveLabel = new JLabel("Status: Checking...", SwingConstants.CENTER);
        googleDriveButton = new JButton("Retry");
        googleDriveButton.addActionListener(e -> retryGoogleDriveConnection());
        drivePanel.add(googleDriveLabel, BorderLayout.CENTER);
        drivePanel.add(googleDriveButton, BorderLayout.EAST);
        gbc.gridx = 2;
        gbc.weightx = 0.2; // MODIFIED: Adjusted weight
        headerPanel.add(drivePanel, gbc);
        
        // --- NEW: Actions Panel ---
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        backupDbButton = new JButton("Backup DB to Drive");
        backupDbButton.setToolTipText("Upload a timestamped copy of the database to Google Drive.");
        backupDbButton.addActionListener(e -> performDatabaseBackup());
        actionsPanel.add(backupDbButton, BorderLayout.CENTER);
        gbc.gridx = 3;
        gbc.weightx = 0.1; // MODIFIED: Adjusted weight
        headerPanel.add(actionsPanel, gbc);


        return headerPanel;
    }
    
    // --- NEW: Method to handle the backup action ---
    private void performDatabaseBackup() {
        // Disable button to prevent multiple clicks
        backupDbButton.setEnabled(false);
        addLog("▶️ Starting database backup to Google Drive...");

        // Run the backup in a background thread to not freeze the UI
        new Thread(() -> {
            try {
                // Call the static method in the Server class
                BsK.server.Server.backupDatabaseToDrive();

                // Update UI on success
                SwingUtilities.invokeLater(() -> {
                    addLog("✅ Database backup completed successfully.");
                    JOptionPane.showMessageDialog(this,
                            "Database backup was successfully uploaded to Google Drive.",
                            "Backup Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                log.error("Database backup failed", e);
                // Update UI on failure
                SwingUtilities.invokeLater(() -> {
                    addLog("❌ Database backup failed: " + e.getMessage());
                     JOptionPane.showMessageDialog(this,
                            "Failed to backup the database.\nError: " + e.getMessage() + "\n\nCheck logs for more details.",
                            "Backup Failed",
                            JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                // Always re-enable the button on the UI thread
                SwingUtilities.invokeLater(() -> backupDbButton.setEnabled(true));
            }
        }).start();
    }

    private JSplitPane createMainContentPanel() {
        // ... (This method remains unchanged)
        // --- Log Panel ---
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logPanel.add(createLogControlsPanel(), BorderLayout.NORTH);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // --- Network Panel ---
        JPanel networkPanel = new JPanel(new BorderLayout());
        networkPanel.setBorder(BorderFactory.createTitledBorder("Network Information"));
        String[] columnNames = {"Session ID", "IP Address", "Port", "Role", "Connected Time", "Last Activity"};
        networkTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        networkTable = new JTable(networkTableModel);
        networkTable.setFillsViewportHeight(true);
        networkPanel.add(new JScrollPane(networkTable), BorderLayout.CENTER);

        // --- Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logPanel, networkPanel);
        splitPane.setResizeWeight(0.80); // Give 80% of space to the logs
        
        return splitPane;
    }

    private JPanel createLogControlsPanel() {
        // ... (This method remains unchanged)
        JPanel logControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchInLogs());
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> clearLogs());
        autoScrollToggle = new JToggleButton("Auto-scroll", true);
        autoScrollToggle.addActionListener(e -> isAutoScrollEnabled = autoScrollToggle.isSelected());

        logControlsPanel.add(new JLabel("Search: "));
        logControlsPanel.add(searchField);
        logControlsPanel.add(searchButton);
        logControlsPanel.add(Box.createHorizontalStrut(20));
        logControlsPanel.add(clearButton);
        logControlsPanel.add(Box.createHorizontalStrut(20));
        logControlsPanel.add(autoScrollToggle);
        return logControlsPanel;
    }

    private void startSystemStatsTimer() {
        // ... (This method remains unchanged)
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        statsTimer = new Timer(2000, e -> {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
            SwingUtilities.invokeLater(() ->
                memoryLabel.setText(String.format("Memory: %d MB / %d MB", usedMemory, maxMemory))
            );

            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
                SwingUtilities.invokeLater(() ->
                    cpuLabel.setText(String.format("CPU: %.1f%%", cpuLoad))
                );
            }
        });
        statsTimer.start();
    }

    private void startNetworkStatsTimer() {
        // ... (This method remains unchanged)
        networkStatsTimer = new Timer(1000, e -> refreshNetworkTable());
        networkStatsTimer.start();
    }

    public void refreshNetworkTable() {
        // ... (This method remains unchanged)
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
        // ... (This method remains unchanged)
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
        // ... (This method remains unchanged)
        SwingUtilities.invokeLater(() -> {
            try {
                logArea.getDocument().remove(0, logArea.getDocument().getLength());
                addLog("Logs cleared");
            } catch (BadLocationException e) {
                log.error("Error clearing logs", e);
            }
        });
    }

    public void updateStatus(String status, Color color) {
        // ... (This method remains unchanged)
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + status);
            statusLabel.setForeground(color);
        });
    }

    public void updatePort(int port) {
        // ... (This method remains unchanged)
        SwingUtilities.invokeLater(() ->
            portLabel.setText("Port: " + port)
        );
    }

    public static void incrementClients() {
        // ... (This method remains unchanged)
        connectedClients++;
        updateClientCount();
    }

    public static void decrementClients() {
        // ... (This method remains unchanged)
        connectedClients = Math.max(0, connectedClients - 1);
        updateClientCount();
    }

    private static void updateClientCount() {
        // ... (This method remains unchanged)
        if (instance != null) {
            SwingUtilities.invokeLater(() -> {
                instance.clientsLabel.setText("Connected Clients: " + connectedClients);
                instance.refreshNetworkTable();
            });
        }
    }

    public void addLog(String message) {
        // ... (This method remains unchanged)
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
        // ... (This method remains unchanged)
        SwingUtilities.invokeLater(() -> {
            if (connected) {
                googleDriveLabel.setText("✅ " + statusMessage);
                googleDriveLabel.setForeground(new Color(34, 139, 34)); // Forest Green
                googleDriveButton.setText("Test");
                googleDriveButton.setToolTipText("Test Google Drive connection");
            } else {
                googleDriveLabel.setText("❌ " + statusMessage);
                googleDriveLabel.setForeground(Color.RED);
                googleDriveButton.setText("Retry");
                googleDriveButton.setToolTipText("Retry Google Drive connection");
            }
        });
    }

    private void retryGoogleDriveConnection() {
        // ... (This method remains unchanged)
        SwingUtilities.invokeLater(() -> {
            googleDriveLabel.setText("🔄 Connecting...");
            googleDriveLabel.setForeground(Color.ORANGE);
            googleDriveButton.setEnabled(false);
            addLog("Testing Google Drive connection...");
        });

        // Run connection attempt in a background thread
        new Thread(() -> {
            try {
                // This method should attempt the connection and update the server's internal state.
                BsK.server.Server.retryGoogleDriveConnection();

                // After the attempt, get the result from the server's state
                boolean isConnected = BsK.server.Server.isGoogleDriveConnected();
                String message = isConnected ? "Connection test successful" : "Connection test failed";

                // *** THIS IS THE FIX ***
                // Call the standard UI update method with the result.
                updateGoogleDriveStatus(isConnected, message);

            } catch (Exception e) {
                log.error("Google Drive connection test failed with an exception", e);
                // Also update the UI in case of an exception
                updateGoogleDriveStatus(false, "Connection error");
                addLog("Google Drive connection error: " + e.getMessage());
            } finally {
                // ALWAYS re-enable the button on the UI thread
                SwingUtilities.invokeLater(() -> googleDriveButton.setEnabled(true));
            }
        }).start();
    }

    @Override
    public void dispose() {
        // ... (This method remains unchanged)
        if (statsTimer != null) {
            statsTimer.stop();
        }
        if (networkStatsTimer != null) {
            networkStatsTimer.stop();
        }
        super.dispose();
    }
}