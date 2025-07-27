package BsK.client.ui.component.SettingsPage;

import BsK.client.Client;
import BsK.client.LocalStorage;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

@Slf4j
public class SettingsDialog extends JDialog {
    private JTextField mediaDirectoryField;
    private JTextField ultrasoundFolderField;
    private JCheckBox autoChangeStatusCheckBox;
    private Properties originalProps;
    private boolean hasChanges = false;
    
    public SettingsDialog(JFrame parent) {
        super(parent, "Cài đặt hệ thống", true); // Modal dialog
        setSize(700, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        // Create main content panel that will be scrollable
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Create two main sections
        JPanel normalSettingsPanel = createSection("Cài đặt", new Color(240, 248, 255)); // Light blue background
        JPanel restartSettingsPanel = createSection("Cài đặt cần khởi động lại", new Color(255, 248, 240)); // Light orange background
        
        // Add normal settings (no restart required)
        addNormalSettings(normalSettingsPanel);
        
        // Add restart-required settings
        addRestartSettings(restartSettingsPanel);
        
        // Load current settings
        loadSettings();
        
        // Add sections to content panel
        contentPanel.add(normalSettingsPanel);
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(restartSettingsPanel);
        contentPanel.add(Box.createVerticalGlue());
        
        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton cancelButton = new JButton("Hủy");
        cancelButton.setPreferredSize(new Dimension(80, 35));
        cancelButton.addActionListener(e -> {
            if (hasChanges) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn hủy các thay đổi không?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    dispose(); // Close dialog without saving
                }
            } else {
                dispose(); // No changes, just close
            }
        });
        
        JButton saveButton = new JButton("Lưu");
        saveButton.setPreferredSize(new Dimension(80, 35));
        saveButton.setBackground(new Color(70, 130, 180));
        saveButton.setForeground(Color.WHITE);
        saveButton.addActionListener(e -> saveSettings());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(saveButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void addNormalSettings(JPanel parent) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Auto change status setting
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        autoChangeStatusCheckBox = new JCheckBox("Tự động chuyển trạng thái thành 'Hoàn thành' khi in");
        autoChangeStatusCheckBox.setFont(new Font("Arial", Font.PLAIN, 12));
        autoChangeStatusCheckBox.setSelected(LocalStorage.autoChangeStatusToFinished);
        autoChangeStatusCheckBox.addActionListener(e -> hasChanges = true);
        parent.add(autoChangeStatusCheckBox, gbc);
        
        gbc.gridy = 1;
        gbc.weighty = 0;
        JLabel autoStatusDescription = new JLabel("<html><i>Khi bật, trạng thái khám bệnh sẽ tự động chuyển thành 'Hoàn thành' sau khi in hóa đơn</i></html>");
        autoStatusDescription.setFont(new Font("Arial", Font.PLAIN, 10));
        autoStatusDescription.setForeground(Color.GRAY);
        parent.add(autoStatusDescription, gbc);
        
        // Add vertical glue to push content to top
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        parent.add(Box.createVerticalGlue(), gbc);
    }
    
    private void addRestartSettings(JPanel parent) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Media directory setting
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        
        JLabel mediaLabel = new JLabel("Thư mục lưu trữ ảnh khám bệnh:");
        mediaLabel.setFont(new Font("Arial", Font.BOLD, 12));
        parent.add(mediaLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JPanel mediaInputPanel = new JPanel(new BorderLayout(5, 0));
        mediaDirectoryField = new JTextField();
        mediaDirectoryField.setText(LocalStorage.checkupMediaBaseDir);
        mediaDirectoryField.setPreferredSize(new Dimension(300, 25));
        mediaDirectoryField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
        });
        
        JButton mediaBrowseButton = new JButton("Chọn thư mục");
        mediaBrowseButton.setPreferredSize(new Dimension(120, 25));
        mediaBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(mediaDirectoryField.getText()));
            
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                mediaDirectoryField.setText(chooser.getSelectedFile().getAbsolutePath());
                hasChanges = true;
            }
        });
        
        mediaInputPanel.add(mediaDirectoryField, BorderLayout.CENTER);
        mediaInputPanel.add(mediaBrowseButton, BorderLayout.EAST);
        parent.add(mediaInputPanel, gbc);
        
        gbc.gridy = 2;
        gbc.weighty = 0;
        JLabel mediaDescription = new JLabel("<html><i>Thư mục chứa ảnh được chụp trong quá trình khám bệnh</i></html>");
        mediaDescription.setFont(new Font("Arial", Font.PLAIN, 10));
        mediaDescription.setForeground(Color.GRAY);
        parent.add(mediaDescription, gbc);
        
        // Ultrasound folder setting
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(15, 5, 5, 5); // Add extra top margin
        
        JLabel ultrasoundLabel = new JLabel("Thư mục theo dõi ảnh siêu âm:");
        ultrasoundLabel.setFont(new Font("Arial", Font.BOLD, 12));
        parent.add(ultrasoundLabel, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5); // Reset insets
        
        JPanel ultrasoundInputPanel = new JPanel(new BorderLayout(5, 0));
        ultrasoundFolderField = new JTextField();
        ultrasoundFolderField.setText(LocalStorage.ULTRASOUND_FOLDER_PATH);
        ultrasoundFolderField.setPreferredSize(new Dimension(300, 25));
        ultrasoundFolderField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
        });
        
        JButton ultrasoundBrowseButton = new JButton("Chọn thư mục");
        ultrasoundBrowseButton.setPreferredSize(new Dimension(120, 25));
        ultrasoundBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(ultrasoundFolderField.getText()));
            
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                ultrasoundFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
                hasChanges = true;
            }
        });
        
        ultrasoundInputPanel.add(ultrasoundFolderField, BorderLayout.CENTER);
        ultrasoundInputPanel.add(ultrasoundBrowseButton, BorderLayout.EAST);
        parent.add(ultrasoundInputPanel, gbc);
        
        gbc.gridy = 5;
        gbc.weighty = 0;
        JLabel ultrasoundDescription = new JLabel("<html><i>Thư mục được hệ thống theo dõi để tự động nhận ảnh siêu âm mới</i></html>");
        ultrasoundDescription.setFont(new Font("Arial", Font.PLAIN, 10));
        ultrasoundDescription.setForeground(Color.GRAY);
        parent.add(ultrasoundDescription, gbc);
        
        // Add vertical glue to push content to top
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        parent.add(Box.createVerticalGlue(), gbc);
    }
    
    private JPanel createSection(String title, Color backgroundColor) {
        JPanel panel = new JPanel(new GridBagLayout()); // Changed to GridBagLayout
        panel.setBackground(backgroundColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(60, 60, 60)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    
    private void loadSettings() {
        try {
            originalProps = new Properties();
            File configFile = new File("config/config.properties");
            if (configFile.exists()) {
                try (var input = Files.newInputStream(configFile.toPath())) {
                    originalProps.load(input);
                }
            }
            hasChanges = false;
        } catch (IOException e) {
            log.error("Error loading config", e);
            JOptionPane.showMessageDialog(this,
                "Lỗi khi tải cài đặt: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveSettings() {
        // Check if there are any changes
        boolean mediaChanged = !mediaDirectoryField.getText().equals(LocalStorage.checkupMediaBaseDir);
        boolean ultrasoundChanged = !ultrasoundFolderField.getText().equals(LocalStorage.ULTRASOUND_FOLDER_PATH);
        boolean autoStatusChanged = autoChangeStatusCheckBox.isSelected() != LocalStorage.autoChangeStatusToFinished;
        
        if (!hasChanges && !mediaChanged && !ultrasoundChanged && !autoStatusChanged) {
            JOptionPane.showMessageDialog(this,
                "Không có thay đổi nào để lưu.",
                "Thông báo",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            Properties props = new Properties();
            if (originalProps != null) {
                props.putAll(originalProps); // Preserve other settings
            }
            
            // Update settings that require restart
            String newMediaDir = mediaDirectoryField.getText().trim();
            String newUltrasoundFolder = ultrasoundFolderField.getText().trim();
            props.setProperty("storage.checkup_media_base_dir", newMediaDir);
            props.setProperty("storage.ultrasound_folder_path", newUltrasoundFolder);
            
            // Update settings that don't require restart
            boolean newAutoStatus = autoChangeStatusCheckBox.isSelected();
            props.setProperty("app.auto_change_status_to_finished", String.valueOf(newAutoStatus));
            LocalStorage.autoChangeStatusToFinished = newAutoStatus; // Update immediately
            
            // Ensure config directory exists
            new File("config").mkdirs();
            
            // Save properties
            File configFile = new File("config/config.properties");
            try (var output = Files.newOutputStream(configFile.toPath())) {
                props.store(output, "BSK Clinic Management System Configuration - Updated via Settings Dialog");
            }
            
            // Check if restart is required
            boolean restartRequired = mediaChanged || ultrasoundChanged;
            
            if (restartRequired) {
                // Show success message with restart required
                int choice = JOptionPane.showConfirmDialog(this,
                    "Cài đặt đã được lưu. Một số thay đổi cần khởi động lại ứng dụng để có hiệu lực.\n\nBạn có muốn khởi động lại ngay bây giờ không?",
                    "Cần khởi động lại",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    // Exit the application
                    System.exit(0);
                } else {
                    dispose(); // Close dialog after saving
                }
            } else {
                // No restart required
                JOptionPane.showMessageDialog(this,
                    "Cài đặt đã được lưu thành công.",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
            
            hasChanges = false;
                
        } catch (IOException ex) {
            log.error("Error saving settings", ex);
            JOptionPane.showMessageDialog(this,
                "Lỗi khi lưu cài đặt: " + ex.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }
} 