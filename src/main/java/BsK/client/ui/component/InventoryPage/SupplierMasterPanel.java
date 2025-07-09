package BsK.client.ui.component.InventoryPage;

import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;

@Slf4j
public class SupplierMasterPanel extends JPanel {
    private InventoryPage parentPage;
    
    // Supplier list
    private JTextField searchField;
    private JTable supplierTable;
    private DefaultTableModel supplierTableModel;
    
    // Supplier form
    private JTextField companyNameField;
    private JTextField contactPersonField;
    private JTextField phoneField;
    private JTextField emailField;
    private JTextField addressField;
    private JTextField taxCodeField;
    private JTextArea notesArea;
    private JComboBox<String> statusCombo;
    
    // Actions
    private JButton addButton;
    private JButton editButton;
    private JButton saveButton;
    private JButton cancelButton;
    private JButton deleteButton;
    private JButton viewHistoryButton;
    
    private boolean isEditing = false;
    private int editingRow = -1;
    private SimpleDateFormat dateFormatter;

    public SupplierMasterPanel(InventoryPage parentPage) {
        this.parentPage = parentPage;
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        // Search field
        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Supplier table
        String[] columns = {"Tên Công Ty", "Người Liên Hệ", "Điện Thoại", "Email", "Trạng Thái"};
        supplierTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        supplierTable = new JTable(supplierTableModel);
        setupSupplierTable();
        
        // Form fields
        companyNameField = createFormField();
        contactPersonField = createFormField();
        phoneField = createFormField();
        emailField = createFormField();
        addressField = createFormField();
        taxCodeField = createFormField();
        
        notesArea = new JTextArea(3, 30);
        notesArea.setFont(new Font("Arial", Font.PLAIN, 14));
        notesArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setEnabled(false);
        
        statusCombo = new JComboBox<>(new String[]{"Hoạt động", "Tạm dừng", "Ngừng hợp tác"});
        statusCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        statusCombo.setEnabled(false);
        
        // Action buttons
        addButton = createActionButton("➕ Thêm NCC", new Color(76, 175, 80));
        editButton = createActionButton("✏️ Sửa", new Color(255, 152, 0));
        saveButton = createActionButton("💾 Lưu", new Color(33, 150, 243));
        cancelButton = createActionButton("❌ Hủy", new Color(158, 158, 158));
        deleteButton = createActionButton("🗑️ Xóa", new Color(244, 67, 54));
        viewHistoryButton = createActionButton("📋 Lịch Sử", new Color(156, 39, 176));
        
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        viewHistoryButton.setEnabled(false);
    }

    private JTextField createFormField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setEnabled(false);
        return field;
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void setupSupplierTable() {
        supplierTable.setFont(new Font("Arial", Font.PLAIN, 14));
        supplierTable.setRowHeight(35);
        supplierTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        supplierTable.setBackground(Color.WHITE);
        supplierTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        supplierTable.getTableHeader().setBackground(new Color(37, 47, 63));
        supplierTable.getTableHeader().setForeground(Color.WHITE);
        
        // Status column with color coding
        supplierTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    String status = (String) value;
                    if ("Hoạt động".equals(status)) {
                        c.setBackground(new Color(232, 245, 233));
                        setForeground(new Color(76, 175, 80));
                    } else if ("Tạm dừng".equals(status)) {
                        c.setBackground(new Color(255, 248, 225));
                        setForeground(new Color(255, 152, 0));
                    } else {
                        c.setBackground(new Color(255, 235, 238));
                        setForeground(new Color(244, 67, 54));
                    }
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("QUẢN LÝ NHÀ CUNG CẤP", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(37, 47, 63));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Split pane: list + form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        splitPane.setResizeWeight(0.65);

        // Left: Supplier list
        RoundedPanel listPanel = createSupplierListPanel();
        splitPane.setLeftComponent(listPanel);

        // Right: Supplier form
        RoundedPanel formPanel = createSupplierFormPanel();
        splitPane.setRightComponent(formPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private RoundedPanel createSupplierListPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Header with search
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        JLabel headerLabel = new JLabel("DANH SÁCH NHÀ CUNG CẤP");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("🔍"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setPreferredSize(new Dimension(250, 35));
        
        headerPanel.add(headerLabel, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        JScrollPane scrollPane = new JScrollPane(supplierTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private RoundedPanel createSupplierFormPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Header
        JLabel headerLabel = new JLabel("THÔNG TIN NHÀ CUNG CẤP");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Company info
        addFormRow(formPanel, gbc, 0, "Tên công ty:", companyNameField);
        addFormRow(formPanel, gbc, 1, "Người liên hệ:", contactPersonField);
        addFormRow(formPanel, gbc, 2, "Điện thoại:", phoneField);
        addFormRow(formPanel, gbc, 3, "Email:", emailField);
        addFormRow(formPanel, gbc, 4, "Mã số thuế:", taxCodeField);
        addFormRow(formPanel, gbc, 5, "Trạng thái:", statusCombo);

        // Address
        gbc.gridx = 0; gbc.gridy = 6; gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Địa chỉ:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        addressField.setPreferredSize(new Dimension(250, 60));
        formPanel.add(addressField, gbc);

        // Notes
        gbc.gridx = 0; gbc.gridy = 7; gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Ghi chú:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH;
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setPreferredSize(new Dimension(250, 80));
        formPanel.add(notesScroll, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewHistoryButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addFormRow(JPanel parent, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        parent.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        component.setPreferredSize(new Dimension(250, 30));
        parent.add(component, gbc);
    }

    private void setupEventListeners() {
        // Table selection
        supplierTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onSupplierSelected();
            }
        });

        // Action buttons
        addButton.addActionListener(e -> startAdd());
        editButton.addActionListener(e -> startEdit());
        saveButton.addActionListener(e -> saveSupplier());
        cancelButton.addActionListener(e -> cancelOperation());
        deleteButton.addActionListener(e -> deleteSupplier());
        viewHistoryButton.addActionListener(e -> viewPurchaseHistory());
    }

    private void onSupplierSelected() {
        int row = supplierTable.getSelectedRow();
        boolean hasSelection = row >= 0;
        
        editButton.setEnabled(hasSelection && !isEditing);
        deleteButton.setEnabled(hasSelection && !isEditing);
        viewHistoryButton.setEnabled(hasSelection && !isEditing);
        
        if (hasSelection) {
            loadSupplierToForm(row);
        }
    }

    private void loadSupplierToForm(int row) {
        // TODO: Load actual supplier data
        companyNameField.setText((String) supplierTableModel.getValueAt(row, 0));
        contactPersonField.setText((String) supplierTableModel.getValueAt(row, 1));
        phoneField.setText((String) supplierTableModel.getValueAt(row, 2));
        emailField.setText((String) supplierTableModel.getValueAt(row, 3));
        statusCombo.setSelectedItem(supplierTableModel.getValueAt(row, 4));
        
        // Mock additional data
        taxCodeField.setText("0123456789");
        addressField.setText("123 Đường ABC, Phường XYZ, Quận 1, TP.HCM");
        notesArea.setText("Nhà cung cấp uy tín, giao hàng đúng hẹn.");
    }

    private void startAdd() {
        isEditing = true;
        editingRow = -1;
        enableFormFields(true);
        clearForm();
        
        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        viewHistoryButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    private void startEdit() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) return;
        
        isEditing = true;
        editingRow = row;
        enableFormFields(true);
        
        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        viewHistoryButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    private void saveSupplier() {
        if (!validateForm()) return;
        
        Object[] rowData = {
            companyNameField.getText(),
            contactPersonField.getText(),
            phoneField.getText(),
            emailField.getText(),
            statusCombo.getSelectedItem()
        };
        
        if (editingRow >= 0) {
            // Update existing
            for (int i = 0; i < rowData.length; i++) {
                supplierTableModel.setValueAt(rowData[i], editingRow, i);
            }
            JOptionPane.showMessageDialog(this, "Cập nhật nhà cung cấp thành công!", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Add new
            supplierTableModel.addRow(rowData);
            JOptionPane.showMessageDialog(this, "Thêm nhà cung cấp mới thành công!", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        }
        
        // TODO: Save to database
        cancelOperation();
    }

    private void cancelOperation() {
        isEditing = false;
        editingRow = -1;
        enableFormFields(false);
        
        addButton.setEnabled(true);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        onSupplierSelected(); // Refresh button states
    }

    private void deleteSupplier() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) return;
        
        String companyName = (String) supplierTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa nhà cung cấp '" + companyName + "'?",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            supplierTableModel.removeRow(row);
            clearForm();
            JOptionPane.showMessageDialog(this, "Xóa nhà cung cấp thành công!", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
            // TODO: Delete from database
        }
    }

    private void viewPurchaseHistory() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) return;
        
        String companyName = (String) supplierTableModel.getValueAt(row, 0);
        
        // Create history dialog
        JDialog historyDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
            "Lịch Sử Mua Hàng - " + companyName, true);
        historyDialog.setSize(800, 500);
        historyDialog.setLocationRelativeTo(this);
        
        // History table
        String[] historyColumns = {"Ngày", "Số HĐ", "Tổng Tiền", "Trạng Thái", "Ghi Chú"};
        DefaultTableModel historyModel = new DefaultTableModel(historyColumns, 0);
        JTable historyTable = new JTable(historyModel);
        historyTable.setRowHeight(30);
        
        // Mock history data
        Object[][] historyData = {
            {"20/06/2024", "HD001", "15.650.000", "Đã thanh toán", "Giao đúng hẹn"},
            {"15/06/2024", "HD002", "8.900.000", "Đã thanh toán", ""},
            {"10/06/2024", "HD003", "22.300.000", "Chưa thanh toán", "Chờ duyệt"},
            {"05/06/2024", "HD004", "12.800.000", "Đã thanh toán", "Có chiết khấu"}
        };
        
        for (Object[] row : historyData) {
            historyModel.addRow(row);
        }
        
        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyDialog.add(historyScroll, BorderLayout.CENTER);
        
        // Summary panel
        JPanel summaryPanel = new JPanel(new FlowLayout());
        summaryPanel.add(new JLabel("Tổng số đơn hàng: 4 | Tổng giá trị: 59.650.000 VNĐ"));
        historyDialog.add(summaryPanel, BorderLayout.SOUTH);
        
        historyDialog.setVisible(true);
    }

    private void enableFormFields(boolean enabled) {
        companyNameField.setEnabled(enabled);
        contactPersonField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        addressField.setEnabled(enabled);
        taxCodeField.setEnabled(enabled);
        notesArea.setEnabled(enabled);
        statusCombo.setEnabled(enabled);
    }

    private void clearForm() {
        companyNameField.setText("");
        contactPersonField.setText("");
        phoneField.setText("");
        emailField.setText("");
        addressField.setText("");
        taxCodeField.setText("");
        notesArea.setText("");
        statusCombo.setSelectedIndex(0);
    }

    private boolean validateForm() {
        if (companyNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên công ty!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            companyNameField.requestFocus();
            return false;
        }
        
        if (contactPersonField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập người liên hệ!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            contactPersonField.requestFocus();
            return false;
        }
        
        if (phoneField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số điện thoại!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            phoneField.requestFocus();
            return false;
        }
        
        return true;
    }

    public void refreshData() {
        // TODO: Load actual data from database
        loadMockSupplierData();
    }

    private void loadMockSupplierData() {
        supplierTableModel.setRowCount(0);
        Object[][] mockData = {
            {"Công ty TNHH Dược phẩm ABC", "Nguyễn Văn A", "0901234567", "contact@abc.com", "Hoạt động"},
            {"Công ty CP Dược Hậu Giang", "Trần Thị B", "0902345678", "info@haugiangpharma.com", "Hoạt động"},
            {"Công ty TNHH Dược phẩm Traphaco", "Lê Văn C", "0903456789", "sales@traphaco.com", "Hoạt động"},
            {"Công ty CP Dược phẩm OPC", "Phạm Thị D", "0904567890", "business@opc.vn", "Tạm dừng"},
            {"Công ty TNHH Imexpharm", "Hoàng Văn E", "0905678901", "order@imexpharm.com", "Hoạt động"}
        };
        
        for (Object[] row : mockData) {
            supplierTableModel.addRow(row);
        }
        
        log.info("Supplier master data loaded - " + mockData.length + " suppliers");
    }
} 