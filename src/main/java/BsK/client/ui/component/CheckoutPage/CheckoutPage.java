package BsK.client.ui.component.CheckoutPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
public class CheckoutPage extends JPanel {
    private MainFrame mainFrame;
    
    // Top Panel Components
    private JTextField searchPatientField;
    private JLabel patientInfoLabel;
    
    // Left Panel - Prescription Checklist (TO-DO)
    private JTable prescriptionChecklistTable;
    private DefaultTableModel prescriptionChecklistModel;
    private JTextArea doctorNotesArea;
    
    // Middle Panel - Dispensed Items (DONE)
    private JTable dispensedItemsTable;
    private DefaultTableModel dispensedItemsModel;
    
    // Right Panel - Payment Summary
    private JLabel medicineSubtotalLabel;
    private JLabel serviceSubtotalLabel;
    private JLabel grandTotalLabel;
    private JTextField cashReceivedField;
    private JLabel changeLabel;
    private JButton cancelButton;
    private JButton payAndPrintButton;
    
    // Dispensing workflow components
    private JDialog configureItemDialog;
    private JComboBox<String> unitComboBox;
    private JLabel unitPriceLabel;
    private JSpinner quantitySpinner;
    private JLabel lineTotalLabel;
    private JTable batchTable;
    private DefaultTableModel batchTableModel;
    private JButton confirmAddButton;
    private JButton cancelDialogButton;
    
    // Current dispensing data
    private String currentDispensingDrug = "";
    private double currentUnitPrice = 0.0;
    
    // Current patient data
    private String currentPatientId = "";
    private String currentPatientName = "";
    private String currentDoctorName = "";
    private double medicineTotal = 0.0;
    private double serviceTotal = 0.0;
    
    // Number formatter for currency
    private NumberFormat currencyFormatter;

    public CheckoutPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // --- Navigation Bar ---
        NavBar navBar = new NavBar(mainFrame, "Thanh Toán & Cấp Phát Thuốc");
        add(navBar, BorderLayout.NORTH);

        initializeComponents();
        layoutComponents();
        setupEventListeners();
        setupKeyboardShortcuts();
        
        // Focus on search field by default
        SwingUtilities.invokeLater(() -> searchPatientField.requestFocusInWindow());
    }

    private void initializeComponents() {
        // Top Panel Components
        searchPatientField = new JTextField(35);
        searchPatientField.setFont(new Font("Arial", Font.PLAIN, 18)); // Increased font size
        searchPatientField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        patientInfoLabel = new JLabel("Chưa chọn bệnh nhân");
        patientInfoLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size
        patientInfoLabel.setForeground(new Color(63, 81, 181));

        // Left Panel - Prescription Checklist (NO SELECT ALL CHECKBOX)
        String[] prescriptionColumns = {"✓", "Tên thuốc/Dịch vụ", "SL Cần", "Thao tác"};
        prescriptionChecklistModel = new DefaultTableModel(prescriptionColumns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                if (column == 3) return JButton.class; // Button column
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 3; // Checkbox and button are editable
            }
        };
        prescriptionChecklistTable = new JTable(prescriptionChecklistModel);
        setupPrescriptionTable();

        doctorNotesArea = new JTextArea(3, 20);
        doctorNotesArea.setEditable(false);
        doctorNotesArea.setBackground(new Color(250, 250, 250));
        doctorNotesArea.setFont(new Font("Arial", Font.ITALIC, 14)); // Increased font size
        doctorNotesArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Ghi chú của Bác sĩ"),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        doctorNotesArea.setLineWrap(true);
        doctorNotesArea.setWrapStyleWord(true);

        // Middle Panel - Dispensed Items
        String[] dispensedColumns = {"Tên thuốc/Dịch vụ", "Lô", "HSD", "SL Đã lấy", "Đơn giá", "Thành tiền"};
        dispensedItemsModel = new DefaultTableModel(dispensedColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        dispensedItemsTable = new JTable(dispensedItemsModel);
        setupDispensedTable();

        // Right Panel - Payment Summary (RIGHT-ALIGNED VALUES)
        medicineSubtotalLabel = new JLabel("0 VNĐ", SwingConstants.RIGHT);
        medicineSubtotalLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        serviceSubtotalLabel = new JLabel("0 VNĐ", SwingConstants.RIGHT);
        serviceSubtotalLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        grandTotalLabel = new JLabel("0 VNĐ", SwingConstants.RIGHT);
        grandTotalLabel.setFont(new Font("Arial", Font.BOLD, 22)); // Increased font size
        grandTotalLabel.setForeground(new Color(244, 67, 54));

        cashReceivedField = new JTextField(15);
        cashReceivedField.setFont(new Font("Arial", Font.PLAIN, 18)); // Increased font size
        cashReceivedField.setHorizontalAlignment(JTextField.RIGHT);
        
        changeLabel = new JLabel("0 VNĐ", SwingConstants.RIGHT);
        changeLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Increased font size
        changeLabel.setForeground(new Color(76, 175, 80));

        // DE-EMPHASIZED CANCEL BUTTON (Grey outline style)
        cancelButton = new JButton("HỦY BỎ");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 14)); // Not bold
        cancelButton.setBackground(Color.WHITE);
        cancelButton.setForeground(new Color(120, 120, 120));
        cancelButton.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 1));
        cancelButton.setFocusPainted(false);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        payAndPrintButton = new JButton("THANH TOÁN & IN HÓA ĐƠN (F9)");
        payAndPrintButton.setFont(new Font("Arial", Font.BOLD, 16));
        payAndPrintButton.setBackground(new Color(76, 175, 80));
        payAndPrintButton.setForeground(Color.WHITE);
        payAndPrintButton.setFocusPainted(false);
        payAndPrintButton.setPreferredSize(new Dimension(300, 55));
        payAndPrintButton.setEnabled(false); // Initially disabled
        payAndPrintButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void setupPrescriptionTable() {
        prescriptionChecklistTable.setRowHeight(35); // Increased row height
        prescriptionChecklistTable.setFont(new Font("Arial", Font.PLAIN, 15)); // Increased font size
        prescriptionChecklistTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        prescriptionChecklistTable.getTableHeader().setBackground(new Color(63, 81, 181));
        prescriptionChecklistTable.getTableHeader().setForeground(Color.WHITE);
        
        // Set column widths - adjusted for Dispense button column
        prescriptionChecklistTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        prescriptionChecklistTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        prescriptionChecklistTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        prescriptionChecklistTable.getColumnModel().getColumn(3).setPreferredWidth(80); // Thao tác column
        
        // Button renderer and editor for Dispense column (column 3)
        prescriptionChecklistTable.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JButton button = new JButton("Cấp thuốc");
                button.setFont(new Font("Arial", Font.PLAIN, 11));
                button.setBackground(new Color(33, 150, 243));
                button.setForeground(Color.WHITE);
                button.setFocusPainted(false);
                
                // Disable button if item is already checked
                Boolean checked = (Boolean) table.getValueAt(row, 0);
                if (checked != null && checked) {
                    button.setText("Đã cấp");
                    button.setBackground(new Color(158, 158, 158));
                    button.setEnabled(false);
                }
                
                return button;
            }
        });
        
        prescriptionChecklistTable.getColumnModel().getColumn(3).setCellEditor(new javax.swing.DefaultCellEditor(new JCheckBox()) {
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JButton button = new JButton("Cấp thuốc");
                button.addActionListener(e -> {
                    String drugName = table.getValueAt(row, 1).toString();
                    CheckoutPage.this.openConfigureItemDialog(drugName, row);
                    fireEditingStopped();
                });
                return button;
            }
        });
        
        // COLOR-CODED STATUS RENDERER (highlight entire row)
        prescriptionChecklistTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                    boolean hasFocus, int row, int column) {
                
                // Skip button column
                if (column == 3) {
                    return prescriptionChecklistTable.getColumnModel().getColumn(3).getCellRenderer()
                            .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
                
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Get the status from the hidden status field (we'll store it in the model)
                String itemName = table.getValueAt(row, 1).toString();
                String status = getItemStatus(itemName); // Helper method to get status
                
                if (status.contains("Hết hàng") || status.contains("Out of Stock")) {
                    c.setBackground(new Color(255, 230, 230)); // Light red
                    if (column == 1) { // Add warning icon to item name
                        setText("⚠️ " + value.toString());
                    }
                } else if (status.contains("Sắp hết") || status.contains("Low Stock")) {
                    c.setBackground(new Color(255, 250, 220)); // Light yellow
                    if (column == 1) { // Add warning icon to item name
                        setText("⚠️ " + value.toString());
                    }
                } else {
                    if (isSelected) {
                        c.setBackground(table.getSelectionBackground());
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                    if (column == 1 && !value.toString().startsWith("⚠️")) {
                        setText(value.toString());
                    }
                }
                
                c.setForeground(isSelected ? table.getSelectionForeground() : Color.BLACK);
                return c;
            }
        });
    }

    private void setupDispensedTable() {
        dispensedItemsTable.setRowHeight(30); // Increased row height
        dispensedItemsTable.setFont(new Font("Arial", Font.PLAIN, 14)); // Increased font size
        dispensedItemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        dispensedItemsTable.getTableHeader().setBackground(new Color(76, 175, 80));
        dispensedItemsTable.getTableHeader().setForeground(Color.WHITE);
        
        // Set column widths
        dispensedItemsTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        dispensedItemsTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        dispensedItemsTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        dispensedItemsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        dispensedItemsTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        dispensedItemsTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        
        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        dispensedItemsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); // SL
        dispensedItemsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer); // Đơn giá
        dispensedItemsTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); // Thành tiền
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top Panel - Search and Patient Info
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Main Content - Three Panel Layout
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Left Panel - Prescription Checklist
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35; gbc.weighty = 1.0;
        contentPanel.add(createPrescriptionPanel(), gbc);

        // Middle Panel - Dispensed Items
        gbc.gridx = 1; gbc.weightx = 0.4;
        contentPanel.add(createDispensedPanel(), gbc);

        // Right Panel - Payment Summary
        gbc.gridx = 2; gbc.weightx = 0.25;
        contentPanel.add(createPaymentPanel(), gbc);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(15, 15));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 224, 224)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Tìm bệnh nhân (ID/Tên) hoặc quét mã (F2):");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Increased font size
        searchPanel.add(searchLabel);
        searchPanel.add(Box.createHorizontalStrut(15));
        searchPanel.add(searchPatientField);

        topPanel.add(searchPanel, BorderLayout.WEST);
        topPanel.add(patientInfoLabel, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createPrescriptionPanel() {
        RoundedPanel panel = new RoundedPanel(12, Color.WHITE, true);
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("ĐƠN THUỐC CẦN XUẤT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Increased font size
        titleLabel.setForeground(new Color(63, 81, 181));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0,15, 0));

        JScrollPane tableScroll = new JScrollPane(prescriptionChecklistTable);
        tableScroll.setPreferredSize(new Dimension(420, 320));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(doctorNotesArea, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDispensedPanel() {
        RoundedPanel panel = new RoundedPanel(12, Color.WHITE, true);
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("THUỐC ĐÃ LẤY", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Increased font size
        titleLabel.setForeground(new Color(76, 175, 80));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JScrollPane tableScroll = new JScrollPane(dispensedItemsTable);
        tableScroll.setPreferredSize(new Dimension(480, 380));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPaymentPanel() {
        RoundedPanel panel = new RoundedPanel(12, Color.WHITE, true);
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(244, 67, 54), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("THANH TOÁN", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Increased font size
        titleLabel.setForeground(new Color(244, 67, 54));

        JPanel summaryPanel = new JPanel(new GridBagLayout());
        summaryPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 8, 10, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Cost breakdown with RIGHT-ALIGNED values
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.6;
        JLabel medicineLabel = new JLabel("Tổng tiền thuốc:");
        medicineLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        summaryPanel.add(medicineLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.4;
        summaryPanel.add(medicineSubtotalLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.6;
        JLabel serviceLabel = new JLabel("Tổng tiền dịch vụ:");
        serviceLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        summaryPanel.add(serviceLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.4;
        summaryPanel.add(serviceSubtotalLabel, gbc);

        // Separator line
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.insets = new Insets(15, 0, 15, 0);
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);
        summaryPanel.add(separator, gbc);

        // Grand total
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.insets = new Insets(10, 8, 10, 8);
        gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.6;
        JLabel totalLabel = new JLabel("TỔNG CỘNG:");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        summaryPanel.add(totalLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.4;
        summaryPanel.add(grandTotalLabel, gbc);

        // Payment input
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.6;
        JLabel cashLabel = new JLabel("Tiền khách đưa:");
        cashLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        summaryPanel.add(cashLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.4;
        summaryPanel.add(cashReceivedField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 0.6;
        JLabel changeTextLabel = new JLabel("Tiền thối lại:");
        changeTextLabel.setFont(new Font("Arial", Font.BOLD, 16));
        summaryPanel.add(changeTextLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.4;
        summaryPanel.add(changeLabel, gbc);

        // Action buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        buttonPanel.add(payAndPrintButton);
        buttonPanel.add(cancelButton);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(summaryPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void setupEventListeners() {
        // Search field - Enter key or barcode scan
        searchPatientField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchAndLoadPatient();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });

        // Cash received field - calculate change
        cashReceivedField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                calculateChange();
            }
        });

        // Prescription checklist - monitor completion
        prescriptionChecklistModel.addTableModelListener(e -> {
            updatePayButtonState();
        });

        // Cancel button
        cancelButton.addActionListener(e -> clearCurrentTransaction());

        // Pay and print button
        payAndPrintButton.addActionListener(e -> processPaymentAndPrint());
    }

    // KEYBOARD SHORTCUTS IMPLEMENTATION
    private void setupKeyboardShortcuts() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        // F2 or Ctrl+F: Focus search field
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "focusSearch");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        actionMap.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchPatientField.requestFocusInWindow();
                searchPatientField.selectAll();
            }
        });

        // F9 or Ctrl+P: Activate payment button
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "processPayment");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK), "processPayment");
        actionMap.put("processPayment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (payAndPrintButton.isEnabled()) {
                    processPaymentAndPrint();
                }
            }
        });

        // ESC: Cancel current transaction
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelTransaction");
        actionMap.put("cancelTransaction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearCurrentTransaction();
            }
        });
    }

    private void searchAndLoadPatient() {
        String searchTerm = searchPatientField.getText().trim();
        if (searchTerm.isEmpty()) {
            playErrorSound();
            JOptionPane.showMessageDialog(this, "Vui lòng nhập ID bệnh nhân hoặc quét mã.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // BARCODE SCAN DETECTION: If term looks like a drug barcode and we have a patient loaded
        if (searchTerm.length() > 8 && searchTerm.matches("\\d+") && !currentPatientId.isEmpty()) {
            handleBarcodeScanned(searchTerm);
            return;
        }

        // TODO: Network request to search patient by ID/name/barcode
        log.info("Searching for patient: " + searchTerm);
        
        // Mock data for demonstration
        loadMockPatientData(searchTerm);
        playSuccessSound();
    }

    private void handleBarcodeScanned(String barcode) {
        log.info("Barcode scanned: " + barcode);
        
        // TODO: Network request to identify drug from barcode
        String drugName = identifyDrugFromBarcode(barcode);
        
        if (drugName == null) {
            playErrorSound();
            JOptionPane.showMessageDialog(this, 
                "Không tìm thấy thuốc với mã vạch: " + barcode, 
                "Mã vạch không hợp lệ", 
                JOptionPane.ERROR_MESSAGE);
            searchPatientField.setText("");
            return;
        }
        
        // SAFETY CHECK: Verify drug exists in prescription
        int prescriptionRow = findDrugInPrescription(drugName);
        if (prescriptionRow == -1) {
            playErrorSound();
            JOptionPane.showMessageDialog(this, 
                "CẢNH BÁO: Thuốc '" + drugName + "' KHÔNG có trong đơn thuốc của bệnh nhân!\n\n" +
                "Vui lòng kiểm tra lại hoặc liên hệ bác sĩ.", 
                "Thuốc không có trong đơn", 
                JOptionPane.ERROR_MESSAGE);
            searchPatientField.setText("");
            return;
        }
        
        // Check if already dispensed
        Boolean isChecked = (Boolean) prescriptionChecklistModel.getValueAt(prescriptionRow, 0);
        if (isChecked != null && isChecked) {
            playErrorSound();
            JOptionPane.showMessageDialog(this, 
                "Thuốc '" + drugName + "' đã được cấp rồi!", 
                "Thuốc đã cấp", 
                JOptionPane.WARNING_MESSAGE);
            searchPatientField.setText("");
            return;
        }
        
        // Valid drug - open configure dialog
        playSuccessSound();
        searchPatientField.setText("");
        openConfigureItemDialog(drugName, prescriptionRow);
    }

    private String identifyDrugFromBarcode(String barcode) {
        // TODO: Implement actual barcode lookup
        // Mock implementation
        switch (barcode) {
            case "1234567890": return "Paracetamol 500mg";
            case "0987654321": return "Amoxicillin 250mg";
            default: return null;
        }
    }

    private int findDrugInPrescription(String drugName) {
        for (int i = 0; i < prescriptionChecklistModel.getRowCount(); i++) {
            String prescriptionDrug = prescriptionChecklistModel.getValueAt(i, 1).toString();
            // Remove warning icons for comparison
            prescriptionDrug = prescriptionDrug.replace("⚠️ ", "");
            if (prescriptionDrug.equals(drugName)) {
                return i;
            }
        }
        return -1;
    }

    private void openConfigureItemDialog(String drugName, int prescriptionRow) {
        currentDispensingDrug = drugName;
        
        if (configureItemDialog == null) {
            createConfigureItemDialog();
        }
        
        // Populate dialog with drug data
        populateConfigureDialog(drugName);
        
        // Center dialog on parent
        configureItemDialog.setLocationRelativeTo(this);
        configureItemDialog.setVisible(true);
    }

    private void createConfigureItemDialog() {
        configureItemDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                                        "Xác nhận chi tiết thuốc", true);
        configureItemDialog.setSize(500, 400);
        configureItemDialog.setLayout(new BorderLayout(10, 10));
        
        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(new Color(63, 81, 181));
        JLabel titleLabel = new JLabel("Xác nhận chi tiết: ");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        configureItemDialog.add(titlePanel, BorderLayout.NORTH);
        
        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Part 1: Unit & Quantity Selection
        JPanel part1Panel = new JPanel(new GridBagLayout());
        part1Panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(63, 81, 181)),
            "PHẦN 1: CHỌN ĐƠN VỊ & SỐ LƯỢNG",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(63, 81, 181)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Unit selection
        gbc.gridx = 0; gbc.gridy = 0;
        part1Panel.add(new JLabel("Đơn vị:"), gbc);
        gbc.gridx = 1;
        unitComboBox = new JComboBox<>(new String[]{"Hộp (10 vỉ)", "Vỉ (10 viên)", "Viên"});
        unitComboBox.setPreferredSize(new Dimension(150, 25));
        unitComboBox.addActionListener(e -> updateUnitPrice());
        part1Panel.add(unitComboBox, gbc);
        
        gbc.gridx = 2;
        part1Panel.add(new JLabel("Đơn giá:"), gbc);
        gbc.gridx = 3;
        unitPriceLabel = new JLabel("100,000 VNĐ");
        unitPriceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        part1Panel.add(unitPriceLabel, gbc);
        
        // Quantity selection
        gbc.gridx = 0; gbc.gridy = 1;
        part1Panel.add(new JLabel("Số lượng:"), gbc);
        gbc.gridx = 1;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        quantitySpinner.setPreferredSize(new Dimension(80, 25));
        quantitySpinner.addChangeListener(e -> updateLineTotal());
        part1Panel.add(quantitySpinner, gbc);
        
        gbc.gridx = 2;
        part1Panel.add(new JLabel("Thành tiền:"), gbc);
        gbc.gridx = 3;
        lineTotalLabel = new JLabel("100,000 VNĐ");
        lineTotalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        lineTotalLabel.setForeground(new Color(244, 67, 54));
        part1Panel.add(lineTotalLabel, gbc);
        
        // Part 2: Batch Selection
        JPanel part2Panel = new JPanel(new BorderLayout(5, 5));
        part2Panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80)),
            "PHẦN 2: CHỌN LÔ SẢN XUẤT (Hệ thống đề xuất lô sắp hết hạn trước - FEFO)",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(76, 175, 80)
        ));
        
        String[] batchColumns = {"Chọn", "Lô", "HSD", "Tồn kho"};
        batchTableModel = new DefaultTableModel(batchColumns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only radio button is editable
            }
        };
        
        batchTable = new JTable(batchTableModel);
        batchTable.setRowHeight(25);
        batchTable.setFont(new Font("Arial", Font.PLAIN, 13));
        batchTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        batchTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        batchTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        batchTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        batchTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        // Single selection for batches
        batchTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane batchScroll = new JScrollPane(batchTable);
        batchScroll.setPreferredSize(new Dimension(400, 120));
        part2Panel.add(batchScroll, BorderLayout.CENTER);
        
        // Combine parts
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(part1Panel, BorderLayout.NORTH);
        contentPanel.add(part2Panel, BorderLayout.CENTER);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        cancelDialogButton = new JButton("Hủy Bỏ");
        cancelDialogButton.setFont(new Font("Arial", Font.PLAIN, 14));
        cancelDialogButton.addActionListener(e -> configureItemDialog.setVisible(false));
        
        confirmAddButton = new JButton("Xác Nhận & Thêm");
        confirmAddButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmAddButton.setBackground(new Color(76, 175, 80));
        confirmAddButton.setForeground(Color.WHITE);
        confirmAddButton.setFocusPainted(false);
        confirmAddButton.addActionListener(e -> confirmAndAddItem());
        
        buttonPanel.add(cancelDialogButton);
        buttonPanel.add(confirmAddButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        configureItemDialog.add(mainPanel, BorderLayout.CENTER);
    }

    private void populateConfigureDialog(String drugName) {
        // Update title
        JLabel titleLabel = (JLabel) ((JPanel) configureItemDialog.getContentPane()
                .getComponent(0)).getComponent(0);
        titleLabel.setText("Xác nhận chi tiết: " + drugName);
        
        // TODO: Load actual unit options and prices from database
        // Mock data for demonstration
        currentUnitPrice = 100000.0; // Default price for first unit
        updateUnitPrice();
        updateLineTotal();
        
        // Load available batches with FEFO logic
        loadAvailableBatches(drugName);
    }

    private void loadAvailableBatches(String drugName) {
        // Clear existing data
        batchTableModel.setRowCount(0);
        
        // TODO: Load actual batches from inventory database
        // Mock data - sorted by expiry date (FEFO)
        if (drugName.contains("Paracetamol")) {
            batchTableModel.addRow(new Object[]{true, "PA202408", "15/08/2025", "45 hộp"}); // FEFO selected
            batchTableModel.addRow(new Object[]{false, "PA202411", "30/11/2025", "18 hộp"});
        } else if (drugName.contains("Amoxicillin")) {
            batchTableModel.addRow(new Object[]{true, "AM202407", "20/07/2025", "12 hộp"}); // FEFO selected
            batchTableModel.addRow(new Object[]{false, "AM202410", "15/10/2025", "35 hộp"});
        }
        
        // Ensure only one batch is selected (radio button behavior)
        setupSingleBatchSelection();
    }

    private void setupSingleBatchSelection() {
        batchTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) { // Selection column
                int changedRow = e.getFirstRow();
                Boolean newValue = (Boolean) batchTableModel.getValueAt(changedRow, 0);
                
                if (newValue != null && newValue) {
                    // Uncheck all other rows
                    for (int i = 0; i < batchTableModel.getRowCount(); i++) {
                        if (i != changedRow) {
                            batchTableModel.setValueAt(false, i, 0);
                        }
                    }
                }
            }
        });
    }

    private void updateUnitPrice() {
        // TODO: Get actual price from database based on selected unit
        String selectedUnit = (String) unitComboBox.getSelectedItem();
        if (selectedUnit.contains("Hộp")) {
            currentUnitPrice = 100000.0;
        } else if (selectedUnit.contains("Vỉ")) {
            currentUnitPrice = 10000.0;
        } else {
            currentUnitPrice = 1000.0;
        }
        
        unitPriceLabel.setText(currencyFormatter.format(currentUnitPrice) + " VNĐ");
        updateLineTotal();
    }

    private void updateLineTotal() {
        int quantity = (Integer) quantitySpinner.getValue();
        double lineTotal = currentUnitPrice * quantity;
        lineTotalLabel.setText(currencyFormatter.format(lineTotal) + " VNĐ");
    }

    private void confirmAndAddItem() {
        // Validate batch selection
        int selectedBatchRow = -1;
        for (int i = 0; i < batchTableModel.getRowCount(); i++) {
            Boolean selected = (Boolean) batchTableModel.getValueAt(i, 0);
            if (selected != null && selected) {
                selectedBatchRow = i;
                break;
            }
        }
        
        if (selectedBatchRow == -1) {
            JOptionPane.showMessageDialog(configureItemDialog, 
                "Vui lòng chọn lô sản xuất!", "Chưa chọn lô", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Get selected data
        String drugName = currentDispensingDrug;
        String unit = (String) unitComboBox.getSelectedItem();
        int quantity = (Integer) quantitySpinner.getValue();
        String batch = batchTableModel.getValueAt(selectedBatchRow, 1).toString();
        String expiry = batchTableModel.getValueAt(selectedBatchRow, 2).toString();
        double unitPrice = currentUnitPrice;
        double lineTotal = unitPrice * quantity;
        
        // Add to dispensed items table
        dispensedItemsModel.addRow(new Object[]{
            drugName,
            batch,
            expiry,
            quantity + " " + unit.split(" ")[0], // Extract unit name
            currencyFormatter.format(unitPrice) + " VNĐ",
            currencyFormatter.format(lineTotal) + " VNĐ"
        });
        
        // Mark prescription item as completed
        int prescriptionRow = findDrugInPrescription(drugName);
        if (prescriptionRow != -1) {
            prescriptionChecklistModel.setValueAt(true, prescriptionRow, 0);
        }
        
        // Update totals
        if (drugName.contains("Dịch vụ")) {
            serviceTotal += lineTotal;
        } else {
            medicineTotal += lineTotal;
        }
        updateTotals();
        updatePayButtonState();
        
        // Close dialog
        configureItemDialog.setVisible(false);
        
        // Success feedback
        playSuccessSound();
        
        // Refresh prescription table to update button states
        prescriptionChecklistTable.repaint();
        
        log.info("Added to dispensed items: " + drugName + " (Batch: " + batch + ", Qty: " + quantity + ")");
    }

    private void loadMockPatientData(String patientId) {
        // Mock patient data
        currentPatientId = patientId;
        currentPatientName = "Nguyễn Văn A";
        currentDoctorName = "BS. Trần Thị Mai";
        
        // IMPROVED PATIENT INFO DISPLAY
        patientInfoLabel.setText(String.format(
            "Bệnh nhân: %s  |  Mã BN: %s  |  Bác sĩ: %s", 
            currentPatientName, currentPatientId, currentDoctorName
        ));

        // Clear previous data
        prescriptionChecklistModel.setRowCount(0);
        dispensedItemsModel.setRowCount(0);
        doctorNotesArea.setText("");

        // Mock prescription data (some automatically marked for services)
        prescriptionChecklistModel.addRow(new Object[]{false, "Paracetamol 500mg", "2 hộp", "Cấp thuốc"});
        prescriptionChecklistModel.addRow(new Object[]{false, "Amoxicillin 250mg", "1 hộp", "Cấp thuốc"});
        prescriptionChecklistModel.addRow(new Object[]{true, "Dịch vụ khám tổng quát", "1 lần", "Đã cấp"}); // Auto-completed

        doctorNotesArea.setText("Uống thuốc sau ăn. Tránh uống cùng với sữa. Nếu có triệu chứng dị ứng, ngừng thuốc ngay. Tái khám sau 1 tuần.");

        updateTotals();
        updatePayButtonState();
    }

    private void calculateChange() {
        try {
            String cashText = cashReceivedField.getText().replace(",", "").trim();
            if (cashText.isEmpty()) {
                changeLabel.setText("0 VNĐ");
                return;
            }

            double cashReceived = Double.parseDouble(cashText);
            double grandTotal = medicineTotal + serviceTotal;
            double change = cashReceived - grandTotal;

            if (change >= 0) {
                changeLabel.setText(currencyFormatter.format(change) + " VNĐ");
                changeLabel.setForeground(new Color(76, 175, 80));
            } else {
                changeLabel.setText("THIẾU " + currencyFormatter.format(Math.abs(change)) + " VNĐ");
                changeLabel.setForeground(new Color(244, 67, 54));
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("Số tiền không hợp lệ");
            changeLabel.setForeground(new Color(244, 67, 54));
        }
    }

    private void updateTotals() {
        medicineTotal = 0.0;
        serviceTotal = 0.0;

        for (int i = 0; i < dispensedItemsModel.getRowCount(); i++) {
            String drugName = dispensedItemsModel.getValueAt(i, 0).toString();
            double lineTotal = Double.parseDouble(dispensedItemsModel.getValueAt(i, 5).toString().replace(" VNĐ", "").replace(",", ""));

            if (drugName.contains("Dịch vụ")) {
                serviceTotal += lineTotal;
            } else {
                medicineTotal += lineTotal;
            }
        }

        medicineSubtotalLabel.setText(currencyFormatter.format(medicineTotal) + " VNĐ");
        serviceSubtotalLabel.setText(currencyFormatter.format(serviceTotal) + " VNĐ");
        grandTotalLabel.setText(currencyFormatter.format(medicineTotal + serviceTotal) + " VNĐ");

        calculateChange();
    }

    private void updatePayButtonState() {
        // Check if all prescription items are completed
        boolean allCompleted = true;
        for (int i = 0; i < prescriptionChecklistModel.getRowCount(); i++) {
            Boolean checked = (Boolean) prescriptionChecklistModel.getValueAt(i, 0);
            if (checked == null || !checked) {
                allCompleted = false;
                break;
            }
        }

        payAndPrintButton.setEnabled(allCompleted && !currentPatientId.isEmpty());
    }

    private void clearCurrentTransaction() {
        currentPatientId = "";
        currentPatientName = "";
        currentDoctorName = "";
        patientInfoLabel.setText("Chưa chọn bệnh nhân");
        searchPatientField.setText("");
        
        prescriptionChecklistModel.setRowCount(0);
        dispensedItemsModel.setRowCount(0);
        doctorNotesArea.setText("");
        
        cashReceivedField.setText("");
        changeLabel.setText("0 VNĐ");
        
        medicineTotal = 0.0;
        serviceTotal = 0.0;
        updateTotals();
        updatePayButtonState();
        
        searchPatientField.requestFocusInWindow();
    }

    private void processPaymentAndPrint() {
        // TODO: Implement payment processing and receipt printing
        log.info("Processing payment for patient: " + currentPatientName + " (ID: " + currentPatientId + ")");
        log.info("Total amount: " + currencyFormatter.format(medicineTotal + serviceTotal) + " VNĐ");
        
        playSuccessSound();
        
        JOptionPane.showMessageDialog(this, 
            "THANH TOÁN THÀNH CÔNG!\n\n" +
            "Bệnh nhân: " + currentPatientName + "\n" +
            "Tổng tiền: " + currencyFormatter.format(medicineTotal + serviceTotal) + " VNĐ\n\n" +
            "TODO: Lưu hóa đơn và cập nhật tồn kho...", 
            "Xác nhận thanh toán", 
            JOptionPane.INFORMATION_MESSAGE);
        
        // Clear transaction after successful payment
        clearCurrentTransaction();
    }

    // HELPER METHODS
    private String getItemStatus(String itemName) {
        // TODO: Get actual status from inventory system
        if (itemName.contains("Amoxicillin")) {
            return "Sắp hết hàng";
        }
        return "Còn hàng";
    }

    // AUDIBLE FEEDBACK PLACEHOLDERS
    private void playSuccessSound() {
        // TODO: Implement pleasant beep sound
        log.debug("Playing success sound");
        Toolkit.getDefaultToolkit().beep(); // Basic system beep for now
    }

    private void playErrorSound() {
        // TODO: Implement distinct error sound
        log.debug("Playing error sound");
        for (int i = 0; i < 2; i++) {
            Toolkit.getDefaultToolkit().beep();
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
    }
} 