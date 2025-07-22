package BsK.server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
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

        // Create main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status Panel (North)
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        
        // Server Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Server Status"));

        statusLabel = new JLabel("Status: Starting...", SwingConstants.CENTER);
        portLabel = new JLabel("Port: --", SwingConstants.CENTER);
        clientsLabel = new JLabel("Connected Clients: 0", SwingConstants.CENTER);
        googleDriveLabel = new JLabel("Google Drive: Checking...", SwingConstants.CENTER);

        statusPanel.add(statusLabel);
        statusPanel.add(portLabel);
        statusPanel.add(clientsLabel);
        statusPanel.add(googleDriveLabel);

        // System Info Panel
        JPanel systemPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        systemPanel.setBorder(BorderFactory.createTitledBorder("System Information"));

        memoryLabel = new JLabel("Memory Usage: --", SwingConstants.CENTER);
        cpuLabel = new JLabel("CPU Usage: --", SwingConstants.CENTER);
        JLabel osLabel = new JLabel("OS: " + System.getProperty("os.name"), SwingConstants.CENTER);
        
        // Google Drive settings button
        googleDriveButton = new JButton("Retry Drive");
        googleDriveButton.addActionListener(e -> retryGoogleDriveConnection());
        googleDriveButton.setToolTipText("Retry Google Drive connection");

        systemPanel.add(memoryLabel);
        systemPanel.add(cpuLabel);
        systemPanel.add(osLabel);
        systemPanel.add(googleDriveButton);

        // Google Drive Settings Panel
        JPanel driveSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        driveSettingsPanel.setBorder(BorderFactory.createTitledBorder("Google Drive Settings"));
        
        JLabel folderNameLabel = new JLabel("Root Folder Name:");
        JTextField driveRootFolderField = new JTextField(getCurrentDriveRootFolder(), 20);
        JButton applyDriveSettingsButton = new JButton("Apply");
        applyDriveSettingsButton.addActionListener(e -> applyDriveSettings(driveRootFolderField.getText().trim()));
        
        driveSettingsPanel.add(folderNameLabel);
        driveSettingsPanel.add(driveRootFolderField);
        driveSettingsPanel.add(applyDriveSettingsButton);

        topPanel.add(statusPanel);
        topPanel.add(systemPanel);
        topPanel.add(driveSettingsPanel);

        // Create split pane for logs and network info
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6); // Give more space to the top component

        // Network Information Panel
        JPanel networkPanel = new JPanel(new BorderLayout());
        networkPanel.setBorder(BorderFactory.createTitledBorder("Network Information"));
        
        // Create table model with columns
        String[] columnNames = {"Session ID", "IP Address", "Port", "Role", "Connected Time", "Last Activity", "Bytes Sent", "Bytes Received"};
        networkTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        networkTable = new JTable(networkTableModel);
        networkTable.setFillsViewportHeight(true);
        JScrollPane networkScrollPane = new JScrollPane(networkTable);
        networkPanel.add(networkScrollPane, BorderLayout.CENTER);

        // Log Controls Panel
        JPanel logControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Search field
        searchField = new JTextField(20);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchInLogs());
        
        // Clear logs button
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> clearLogs());
        
        // Auto-scroll toggle
        autoScrollToggle = new JToggleButton("Auto-scroll", true);
        autoScrollToggle.addActionListener(e -> isAutoScrollEnabled = autoScrollToggle.isSelected());

        logControlsPanel.add(new JLabel("Search: "));
        logControlsPanel.add(searchField);
        logControlsPanel.add(searchButton);
        logControlsPanel.add(Box.createHorizontalStrut(20));
        logControlsPanel.add(clearButton);
        logControlsPanel.add(Box.createHorizontalStrut(20));
        logControlsPanel.add(autoScrollToggle);

        // Log Area Panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);

        logPanel.add(logControlsPanel, BorderLayout.NORTH);
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        // Add components to split pane
        splitPane.setTopComponent(logPanel);
        splitPane.setBottomComponent(networkPanel);

        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);

        // Start system stats update timer
        startSystemStatsTimer();
        // Start network stats update timer
        startNetworkStatsTimer();
    }

    private void startSystemStatsTimer() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        statsTimer = new Timer(2000, e -> {
            // Update memory usage
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
            SwingUtilities.invokeLater(() -> 
                memoryLabel.setText(String.format("Memory Usage: %d MB / %d MB", usedMemory, maxMemory))
            );

            // Update CPU usage if available
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
                SwingUtilities.invokeLater(() -> 
                    cpuLabel.setText(String.format("CPU Usage: %.1f%%", cpuLoad))
                );
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
            // Clear existing rows
            networkTableModel.setRowCount(0);
            
            // Add current connections
            for (ClientConnection conn : SessionManager.getAllConnections()) {
                Vector<Object> row = new Vector<>();
                row.add(conn.getSessionId());
                row.add(conn.getIpAddress());
                row.add(conn.getPort());
                row.add(conn.getUserRole());
                row.add(conn.getConnectionDuration());
                row.add(conn.getLastActivityDuration());
                row.add(formatBytes(conn.getBytesSent()));
                row.add(formatBytes(conn.getBytesReceived()));
                networkTableModel.addRow(row);
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
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
                addLog("Logs cleared");
            } catch (BadLocationException e) {
                log.error("Error clearing logs", e);
            }
        });
    }

    public void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + status);
            statusLabel.setForeground(color);
        });
    }

    public void updatePort(int port) {
        SwingUtilities.invokeLater(() -> 
            portLabel.setText("Port: " + port)
        );
    }

    public static void incrementClients() {
        connectedClients++;
        updateClientCount();
    }

    public static void decrementClients() {
        connectedClients = Math.max(0, connectedClients - 1); // Ensure we don't go below 0
        updateClientCount();
    }

    private static void updateClientCount() {
        if (instance != null) {
            SwingUtilities.invokeLater(() -> {
                instance.clientsLabel.setText("Connected Clients: " + connectedClients);
                // Also update the network table since client count changed
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
                
                // Create style for new text
                Style style = logArea.addStyle("Log Style", null);
                StyleConstants.setFontFamily(style, "Monospace");
                StyleConstants.setFontSize(style, 12);
                
                // Insert the text at the end
                doc.insertString(doc.getLength(), formattedMessage, style);
                
                // Scroll to bottom if auto-scroll is enabled
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
                googleDriveLabel.setText("Google Drive: ✅ " + statusMessage);
                googleDriveLabel.setForeground(Color.GREEN);
                googleDriveButton.setText("Test Drive");
                googleDriveButton.setToolTipText("Test Google Drive connection");
            } else {
                googleDriveLabel.setText("Google Drive: ❌ " + statusMessage);
                googleDriveLabel.setForeground(Color.RED);
                googleDriveButton.setText("Retry Drive");
                googleDriveButton.setToolTipText("Retry Google Drive connection");
            }
        });
    }

    private void retryGoogleDriveConnection() {
        SwingUtilities.invokeLater(() -> {
            googleDriveLabel.setText("Google Drive: 🔄 Connecting...");
            googleDriveLabel.setForeground(Color.ORANGE);
            googleDriveButton.setEnabled(false);
            addLog("Retrying Google Drive connection...");
        });

        // Run connection attempt in background thread
        new Thread(() -> {
            try {
                BsK.server.Server.retryGoogleDriveConnection();
                SwingUtilities.invokeLater(() -> {
                    googleDriveButton.setEnabled(true);
                    if (BsK.server.Server.isGoogleDriveConnected()) {
                        addLog("Google Drive connection successful ✅");
                    } else {
                        addLog("Google Drive connection failed ❌");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    googleDriveButton.setEnabled(true);
                    addLog("Google Drive connection error: " + e.getMessage());
                });
                         }
         }).start();
     }

     private String getCurrentDriveRootFolder() {
         // Get current root folder name from Server or return default
         try {
             if (BsK.server.Server.getGoogleDriveService() != null) {
                 return BsK.server.Server.getGoogleDriveRootFolderName();
             }
         } catch (Exception e) {
             log.warn("Could not get current drive root folder name", e);
         }
         return "BSK_Clinic_Patient_Files"; // Default value
     }

     private void applyDriveSettings(String newRootFolderName) {
         if (newRootFolderName == null || newRootFolderName.trim().isEmpty()) {
             addLog("❌ Root folder name cannot be empty");
             return;
         }

         // Sanitize folder name
         String sanitizedName = newRootFolderName.replaceAll("[^a-zA-Z0-9._-]", "_");
         if (!sanitizedName.equals(newRootFolderName)) {
             addLog("⚠️  Folder name sanitized to: " + sanitizedName);
         }

         addLog("🔄 Updating Google Drive root folder to: " + sanitizedName);
         
         new Thread(() -> {
             try {
                 // Update the root folder name in server
                 BsK.server.Server.updateGoogleDriveRootFolder(sanitizedName);
                 
                 SwingUtilities.invokeLater(() -> {
                     addLog("✅ Google Drive root folder updated successfully");
                     addLog("💡 New patient folders will be created under: " + sanitizedName);
                 });
             } catch (Exception e) {
                 SwingUtilities.invokeLater(() -> {
                     addLog("❌ Failed to update Google Drive root folder: " + e.getMessage());
                 });
                 log.error("Error updating Google Drive root folder", e);
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
} 