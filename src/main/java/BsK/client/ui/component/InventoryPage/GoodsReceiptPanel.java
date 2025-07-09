package BsK.client.ui.component.InventoryPage;

import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.InventoryPage.common.InventoryButtonFactory;
import BsK.client.ui.component.InventoryPage.common.InventoryColorScheme;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
public class GoodsReceiptPanel extends JPanel {
    private InventoryPage parentPage;
    
    // Header information
    private JComboBox<String> supplierCombo;
    private JTextField invoiceNumberField;
    private JTextField receiptDateField;
    
    // Medicine search and add
    private JTextField medicineSearchField;
    private JButton addMedicineButton;
    
    // Receipt items table
    private JTable receiptTable;
    private DefaultTableModel receiptTableModel;
    
    // Total and actions
    private JLabel totalAmountLabel;
    private JButton saveReceiptButton;
    private JButton cancelButton;
    private JButton printButton;
    
    // Formatters
    private SimpleDateFormat dateFormatter;
    private NumberFormat currencyFormatter;
    
    // Current receipt data
    private double totalAmount = 0.0;

    public GoodsReceiptPanel(InventoryPage parentPage) {
        this.parentPage = parentPage;
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
        this.currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        // Supplier combo
        supplierCombo = new JComboBox<>(new String[]{
            "Chọn nhà cung cấp...",
            "Công ty TNHH Dược phẩm ABC",
            "Công ty Cổ phần Dược Hậu Giang",
            "Công ty TNHH Dược phẩm Traphaco",
            "Công ty Cổ phần Dược phẩm OPC",
            "Công ty TNHH Dược phẩm Imexpharm"
        });
        supplierCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        supplierCombo.setPreferredSize(new Dimension(300, 35));
        
        // Invoice number field
        invoiceNumberField = new JTextField();
        invoiceNumberField.setFont(new Font("Arial", Font.PLAIN, 14));
        invoiceNumberField.setPreferredSize(new Dimension(200, 35));
        invoiceNumberField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Receipt date field (auto-filled with today)
        receiptDateField = new JTextField(dateFormatter.format(new Date()));
        receiptDateField.setFont(new Font("Arial", Font.PLAIN, 14));
        receiptDateField.setPreferredSize(new Dimension(150, 35));
        receiptDateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Medicine search field with placeholder
        medicineSearchField = new JTextField("Tìm thuốc theo tên hoặc quét mã vạch...");
        medicineSearchField.setFont(new Font("Arial", Font.ITALIC, 14));
        medicineSearchField.setForeground(InventoryColorScheme.TEXT_DISABLED);
        medicineSearchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(InventoryColorScheme.BORDER_LIGHT, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        
        // Add focus listener for placeholder behavior
        medicineSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (medicineSearchField.getText().equals("Tìm thuốc theo tên hoặc quét mã vạch...")) {
                    medicineSearchField.setText("");
                    medicineSearchField.setFont(new Font("Arial", Font.PLAIN, 14));
                    medicineSearchField.setForeground(InventoryColorScheme.TEXT_PRIMARY);
                    medicineSearchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(InventoryColorScheme.PRIMARY_BLUE, 2),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                    ));
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (medicineSearchField.getText().trim().isEmpty()) {
                    medicineSearchField.setText("Tìm thuốc theo tên hoặc quét mã vạch...");
                    medicineSearchField.setFont(new Font("Arial", Font.ITALIC, 14));
                    medicineSearchField.setForeground(InventoryColorScheme.TEXT_DISABLED);
                    medicineSearchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(InventoryColorScheme.BORDER_LIGHT, 2),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                    ));
                }
            }
        });
        
        // Receipt table
        String[] columns = {"Tên Thuốc", "Lô", "Hạn Sử Dụng", "Số Lượng", "Đơn Giá Nhập", "Thành Tiền", "Xóa"};
        receiptTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only delete column is editable
            }
        };
        receiptTable = new JTable(receiptTableModel);
        setupReceiptTable();
        
        // Total amount label
        totalAmountLabel = new JLabel("0 VNĐ");
        totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 24));
        totalAmountLabel.setForeground(new Color(76, 175, 80));
        totalAmountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Action buttons using standardized factory
        saveReceiptButton = InventoryButtonFactory.createPrimaryButton("💾 Lưu Phiếu Nhập", InventoryButtonFactory.LARGE_BUTTON_SIZE);
        cancelButton = InventoryButtonFactory.createSecondaryButton("❌ Hủy Bỏ", InventoryButtonFactory.LARGE_BUTTON_SIZE);
        printButton = InventoryButtonFactory.createSpecialButton("🖨️ In Phiếu", InventoryButtonFactory.LARGE_BUTTON_SIZE);
        printButton.setEnabled(false);
    }

    private void setupReceiptTable() {
        receiptTable.setFont(new Font("Arial", Font.PLAIN, 14));
        receiptTable.setRowHeight(40);
        receiptTable.setBackground(Color.WHITE);
        receiptTable.setGridColor(new Color(230, 230, 230));
        receiptTable.setShowVerticalLines(true);
        receiptTable.setShowHorizontalLines(true);
        receiptTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        receiptTable.getTableHeader().setBackground(InventoryColorScheme.TABLE_HEADER_BG);
        receiptTable.getTableHeader().setForeground(InventoryColorScheme.TABLE_HEADER_TEXT);
        receiptTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        TableColumn col0 = receiptTable.getColumnModel().getColumn(0); // Tên Thuốc
        col0.setPreferredWidth(200);
        TableColumn col1 = receiptTable.getColumnModel().getColumn(1); // Lô
        col1.setPreferredWidth(100);
        TableColumn col2 = receiptTable.getColumnModel().getColumn(2); // HSD
        col2.setPreferredWidth(100);
        TableColumn col3 = receiptTable.getColumnModel().getColumn(3); // Số Lượng
        col3.setPreferredWidth(100);
        TableColumn col4 = receiptTable.getColumnModel().getColumn(4); // Đơn giá
        col4.setPreferredWidth(120);
        TableColumn col5 = receiptTable.getColumnModel().getColumn(5); // Thành tiền
        col5.setPreferredWidth(140);
        TableColumn col6 = receiptTable.getColumnModel().getColumn(6); // Xóa
        col6.setPreferredWidth(80);
        
        // Center align specific columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        receiptTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        receiptTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        receiptTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        
        // Right align money columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        receiptTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        receiptTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        
        // Delete button column
        receiptTable.getColumnModel().getColumn(6).setCellRenderer(new DeleteButtonRenderer());
        receiptTable.getColumnModel().getColumn(6).setCellEditor(new DeleteButtonEditor());
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("PHIẾU NHẬP KHO", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(37, 47, 63));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Content area
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setOpaque(false);

        // Header info panel
        RoundedPanel headerPanel = createHeaderPanel();
        contentPanel.add(headerPanel, BorderLayout.NORTH);

        // Medicine items panel
        RoundedPanel itemsPanel = createItemsPanel();
        contentPanel.add(itemsPanel, BorderLayout.CENTER);

        // Footer with total and actions
        RoundedPanel footerPanel = createFooterPanel();
        contentPanel.add(footerPanel, BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private RoundedPanel createHeaderPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 15);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 1: Supplier
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nhà cung cấp:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(supplierCombo, gbc);
        
        // Row 2: Invoice number and date
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Số Hóa Đơn:"), gbc);
        gbc.gridx = 1;
        panel.add(invoiceNumberField, gbc);
        
        gbc.gridx = 2;
        panel.add(new JLabel("Ngày nhập:"), gbc);
        gbc.gridx = 3;
        panel.add(receiptDateField, gbc);
        
        return panel;
    }

    private RoundedPanel createItemsPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Simplified medicine search section
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Arial", Font.PLAIN, 16));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        
        medicineSearchField.setPreferredSize(new Dimension(0, 45));
        searchPanel.add(medicineSearchField, BorderLayout.CENTER);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Items table
        JScrollPane scrollPane = new JScrollPane(receiptTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            " Danh sách thuốc nhập kho ",
            0, 0, new Font("Arial", Font.BOLD, 14), new Color(37, 47, 63)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private RoundedPanel createFooterPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(20, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Total section
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setOpaque(false);
        
        JLabel totalLabel = new JLabel("TỔNG TIỀN NHẬP KHO:");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        totalLabel.setForeground(new Color(37, 47, 63));
        totalPanel.add(totalLabel, BorderLayout.WEST);
        totalPanel.add(totalAmountLabel, BorderLayout.EAST);
        
        // Actions panel with split layout (Cancel left, Print/Save right)
        JButton[] leftButtons = {cancelButton};
        JButton[] rightButtons = {printButton, saveReceiptButton};
        JPanel actionsPanel = InventoryButtonFactory.createSplitButtonGroup(leftButtons, rightButtons);
        
        panel.add(totalPanel, BorderLayout.WEST);
        panel.add(actionsPanel, BorderLayout.EAST);
        
        return panel;
    }

    private void setupEventListeners() {
        // Medicine search field - Enter key adds medicine
        medicineSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addMedicineToReceipt();
                }
            }
        });
        

        
        // Action buttons
        saveReceiptButton.addActionListener(e -> saveReceipt());
        cancelButton.addActionListener(e -> cancelReceipt());
        printButton.addActionListener(e -> printReceipt());
    }

    private void addMedicineToReceipt() {
        String searchText = medicineSearchField.getText().trim();
        if (searchText.isEmpty() || searchText.equals("Tìm thuốc theo tên hoặc quét mã vạch...")) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên thuốc hoặc quét mã vạch!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // TODO: Search medicine in database and show selection dialog
        showAddMedicineDialog(searchText);
    }

    private void showAddMedicineDialog(String searchText) {
        // Mock implementation - show a simple dialog for adding medicine details
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm Thuốc Vào Phiếu Nhập", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Medicine name (read-only, from search)
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Tên thuốc:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(searchText, 20);
        nameField.setEditable(false);
        formPanel.add(nameField, gbc);
        
        // Batch number
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Số lô:"), gbc);
        gbc.gridx = 1;
        JTextField batchField = new JTextField(20);
        formPanel.add(batchField, gbc);
        
        // Expiry date
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Hạn sử dụng:"), gbc);
        gbc.gridx = 1;
        JTextField expiryField = new JTextField("dd/MM/yyyy", 20);
        formPanel.add(expiryField, gbc);
        
        // Quantity
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Số lượng:"), gbc);
        gbc.gridx = 1;
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        formPanel.add(quantitySpinner, gbc);
        
        // Unit price
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Đơn giá nhập:"), gbc);
        gbc.gridx = 1;
        JTextField priceField = new JTextField("0", 20);
        formPanel.add(priceField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Thêm");
        JButton cancelBtn = new JButton("Hủy");
        
        addBtn.addActionListener(e -> {
            try {
                String medicineName = nameField.getText();
                String batch = batchField.getText();
                String expiry = expiryField.getText();
                int quantity = (Integer) quantitySpinner.getValue();
                double price = Double.parseDouble(priceField.getText().replace(",", ""));
                double lineTotal = quantity * price;
                
                // Add to table
                receiptTableModel.addRow(new Object[]{
                    medicineName,
                    batch,
                    expiry,
                    quantity,
                    currencyFormatter.format(price),
                    currencyFormatter.format(lineTotal),
                    "🗑️"
                });
                
                updateTotalAmount();
                medicineSearchField.setText("");
                dialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập đúng định dạng số!", 
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void updateTotalAmount() {
        totalAmount = 0.0;
        for (int i = 0; i < receiptTableModel.getRowCount(); i++) {
            String lineTotalStr = (String) receiptTableModel.getValueAt(i, 5);
            try {
                double lineTotal = Double.parseDouble(lineTotalStr.replace(",", ""));
                totalAmount += lineTotal;
            } catch (NumberFormatException e) {
                log.warn("Could not parse line total: " + lineTotalStr);
            }
        }
        totalAmountLabel.setText(currencyFormatter.format(totalAmount) + " VNĐ");
    }

    private void saveReceipt() {
        if (receiptTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng thêm ít nhất một loại thuốc!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (supplierCombo.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhà cung cấp!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // TODO: Save receipt to database
        JOptionPane.showMessageDialog(this, "Lưu phiếu nhập kho thành công!\nTổng tiền: " + totalAmountLabel.getText(), 
            "Thành công", JOptionPane.INFORMATION_MESSAGE);
        
        printButton.setEnabled(true);
        clearForm();
    }

    private void cancelReceipt() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc chắn muốn hủy phiếu nhập này?", 
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            clearForm();
        }
    }

    private void printReceipt() {
        // TODO: Implement print functionality
        JOptionPane.showMessageDialog(this, "Chức năng in phiếu nhập kho đang được phát triển!", 
            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm() {
        supplierCombo.setSelectedIndex(0);
        invoiceNumberField.setText("");
        receiptDateField.setText(dateFormatter.format(new Date()));
        
        // Reset search field to placeholder
        medicineSearchField.setText("Tìm thuốc theo tên hoặc quét mã vạch...");
        medicineSearchField.setFont(new Font("Arial", Font.ITALIC, 14));
        medicineSearchField.setForeground(InventoryColorScheme.TEXT_DISABLED);
        medicineSearchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(InventoryColorScheme.BORDER_LIGHT, 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        
        receiptTableModel.setRowCount(0);
        totalAmount = 0.0;
        totalAmountLabel.setText("0 VNĐ");
        printButton.setEnabled(false);
    }

    public void refreshData() {
        // TODO: Refresh supplier list and other master data
        log.info("Goods receipt panel data refreshed");
    }

    // Custom cell renderer for delete button
    private class DeleteButtonRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            return InventoryButtonFactory.createIconButton(
                InventoryButtonFactory.ICON_DELETE, 
                InventoryColorScheme.DANGER_RED, 
                InventoryColorScheme.darken(InventoryColorScheme.DANGER_RED, 0.2f),
                "Xóa dòng này"
            );
        }
    }

    // Custom cell editor for delete button
    private class DeleteButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int currentRow;

        public DeleteButtonEditor() {
            super(new JCheckBox());
            button = InventoryButtonFactory.createIconButton(
                InventoryButtonFactory.ICON_DELETE, 
                InventoryColorScheme.DANGER_RED, 
                InventoryColorScheme.darken(InventoryColorScheme.DANGER_RED, 0.2f),
                "Xóa dòng này"
            );
            button.addActionListener(e -> {
                receiptTableModel.removeRow(currentRow);
                updateTotalAmount();
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "🗑️";
        }
    }
} 