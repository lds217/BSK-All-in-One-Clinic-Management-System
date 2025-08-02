package BsK.client.ui.component.SettingsPage;

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
    // Existing fields
    private JTextField mediaDirectoryField;
    private JTextField ultrasoundFolderField;
    private JCheckBox autoChangeStatusCheckBox;

    // Fields for additional settings
    private JTextField clinicNameField;
    private JTextField clinicAddressField;
    private JTextField clinicPhoneField;
    private JTextField clinicPrefixField;
    private JTextField serverAddressField;
    private JTextField serverPortField;
    
    // <<< REMOVED: Google Drive UI fields are gone

    private Properties originalProps;
    private boolean hasChanges = false;
    
    public SettingsDialog(JFrame parent) {
        super(parent, "Cài đặt hệ thống", true); // Modal dialog
        setSize(700, 700); // Adjusted height
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
                    dispose();
                }
            } else {
                dispose();
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
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        parent.add(Box.createVerticalGlue(), gbc);
    }
    
    private void addRestartSettings(JPanel parent) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        int y = 0;

        javax.swing.event.DocumentListener changeListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { hasChanges = true; }
        };
        
        // --- Clinic Info ---
        clinicNameField = addTextFieldSetting(parent, gbc, y++, "Tên phòng khám:", LocalStorage.ClinicName, "Tên sẽ hiển thị trên các phiếu in.", changeListener);
        clinicAddressField = addTextFieldSetting(parent, gbc, y++, "Địa chỉ phòng khám:", LocalStorage.ClinicAddress, "Địa chỉ sẽ hiển thị trên các phiếu in.", changeListener);
        clinicPhoneField = addTextFieldSetting(parent, gbc, y++, "SĐT phòng khám:", LocalStorage.ClinicPhone, "Số điện thoại sẽ hiển thị trên các phiếu in.", changeListener);
        clinicPrefixField = addTextFieldSetting(parent, gbc, y++, "Tiền tố mã:", LocalStorage.ClinicPrefix, "Tiền tố cho mã bệnh nhân và các mã khác (ví dụ: BSK).", changeListener);

        // --- Server Connection ---
        serverAddressField = addTextFieldSetting(parent, gbc, y++, "Địa chỉ máy chủ:", LocalStorage.serverAddress, "Địa chỉ IP hoặc tên miền của máy chủ.", changeListener);
        serverPortField = addTextFieldSetting(parent, gbc, y++, "Cổng máy chủ (Port):", LocalStorage.serverPort, "Cổng kết nối tới máy chủ.", changeListener);

        // --- Storage Settings ---
        mediaDirectoryField = addTextFieldSettingWithBrowse(parent, gbc, y++, "Thư mục lưu trữ ảnh khám bệnh:", LocalStorage.checkupMediaBaseDir, "Thư mục chứa ảnh được chụp trong quá trình khám bệnh.", changeListener);
        ultrasoundFolderField = addTextFieldSettingWithBrowse(parent, gbc, y++, "Thư mục theo dõi ảnh siêu âm:", LocalStorage.ULTRASOUND_FOLDER_PATH, "Thư mục được hệ thống theo dõi để tự động nhận ảnh siêu âm mới.", changeListener);

        // <<< REMOVED: Google Drive Integration UI is gone.

        // Vertical glue to push all content up
        gbc.gridy = y * 3; // Adjust glue position
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        parent.add(Box.createVerticalGlue(), gbc);
    }

    private JTextField addTextFieldSetting(JPanel parent, GridBagConstraints gbc, int yPos, String labelText, String initialValue, String description, javax.swing.event.DocumentListener listener) {
        gbc.gridx = 0; gbc.gridy = yPos * 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.insets = (yPos == 0) ? new Insets(5, 5, 5, 5) : new Insets(15, 5, 5, 5);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        parent.add(label, gbc);

        gbc.gridy = yPos * 3 + 1; gbc.insets = new Insets(5, 5, 5, 5);
        JTextField textField = new JTextField(initialValue);
        textField.getDocument().addDocumentListener(listener);
        parent.add(textField, gbc);

        gbc.gridy = yPos * 3 + 2;
        JLabel descLabel = new JLabel("<html><i>" + description + "</i></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        descLabel.setForeground(Color.GRAY);
        parent.add(descLabel, gbc);
        
        return textField;
    }

    private JTextField addTextFieldSettingWithBrowse(JPanel parent, GridBagConstraints gbc, int yPos, String labelText, String initialValue, String description, javax.swing.event.DocumentListener listener) {
        JTextField textField = addTextFieldSetting(parent, gbc, yPos, labelText, initialValue, description, listener);
        parent.remove(textField);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(textField, BorderLayout.CENTER);
        JButton browseButton = new JButton("Chọn...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            try {
                File currentDir = new File(textField.getText());
                if (currentDir.exists()) {
                    chooser.setCurrentDirectory(currentDir);
                }
            } catch (Exception ex) { /* Ignore */ }

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                textField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        inputPanel.add(browseButton, BorderLayout.EAST);

        gbc.gridy = yPos * 3 + 1;
        parent.add(inputPanel, gbc);

        return textField;
    }
    
    private JPanel createSection(String title, Color backgroundColor) {
        JPanel panel = new JPanel(new GridBagLayout());
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
            JOptionPane.showMessageDialog(this, "Lỗi khi tải cài đặt: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveSettings() {
        // <<< MODIFIED: Removed Google Drive fields from change detection
        boolean restartRequiredChanges = 
            !mediaDirectoryField.getText().trim().equals(LocalStorage.checkupMediaBaseDir) ||
            !ultrasoundFolderField.getText().trim().equals(LocalStorage.ULTRASOUND_FOLDER_PATH) ||
            !clinicNameField.getText().trim().equals(LocalStorage.ClinicName) ||
            !clinicAddressField.getText().trim().equals(LocalStorage.ClinicAddress) ||
            !clinicPhoneField.getText().trim().equals(LocalStorage.ClinicPhone) ||
            !clinicPrefixField.getText().trim().equals(LocalStorage.ClinicPrefix) ||
            !serverAddressField.getText().trim().equals(LocalStorage.serverAddress) ||
            !serverPortField.getText().trim().equals(LocalStorage.serverPort);
            
        boolean noRestartChanges = autoChangeStatusCheckBox.isSelected() != LocalStorage.autoChangeStatusToFinished;

        if (!restartRequiredChanges && !noRestartChanges) {
            JOptionPane.showMessageDialog(this, "Không có thay đổi nào để lưu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            Properties props = new Properties();
            if (originalProps != null) {
                props.putAll(originalProps);
            }
            
            // Clinic Info
            props.setProperty("clinic.name", clinicNameField.getText().trim());
            props.setProperty("clinic.address", clinicAddressField.getText().trim());
            props.setProperty("clinic.phone", clinicPhoneField.getText().trim());
            props.setProperty("clinic.prefix", clinicPrefixField.getText().trim());
            
            // Server
            props.setProperty("server.address", serverAddressField.getText().trim());
            props.setProperty("server.port", serverPortField.getText().trim());

            // Storage
            props.setProperty("storage.checkup_media_base_dir", mediaDirectoryField.getText().trim());
            props.setProperty("storage.ultrasound_folder_path", ultrasoundFolderField.getText().trim());

            // <<< REMOVED: Saving of Google Drive properties
            
            // App settings (no restart)
            boolean newAutoStatus = autoChangeStatusCheckBox.isSelected();
            props.setProperty("app.auto_change_status_to_finished", String.valueOf(newAutoStatus));
            LocalStorage.autoChangeStatusToFinished = newAutoStatus;
            
            // Save properties to file
            new File("config").mkdirs();
            File configFile = new File("config/config.properties");
            try (var output = Files.newOutputStream(configFile.toPath())) {
                props.store(output, "BSK Clinic Management System Configuration - Updated via Settings Dialog");
            }
            
            if (restartRequiredChanges) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "Cài đặt đã được lưu. Một số thay đổi cần khởi động lại ứng dụng để có hiệu lực.\n\nBạn có muốn khởi động lại ngay bây giờ không?",
                    "Cần khởi động lại",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                    
                if (choice == JOptionPane.YES_OPTION) {
                    System.exit(0);
                } else {
                    dispose();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Cài đặt đã được lưu thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
            
            hasChanges = false;
            
        } catch (IOException ex) {
            log.error("Error saving settings", ex);
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu cài đặt: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}