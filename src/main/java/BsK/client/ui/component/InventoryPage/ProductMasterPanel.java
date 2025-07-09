package BsK.client.ui.component.InventoryPage;

import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
public class ProductMasterPanel extends JPanel {
    private InventoryPage parentPage;
    
    // Product list
    private JTextField searchField;
    private JTable productTable;
    private DefaultTableModel productTableModel;
    
    // Product form
    private JTextField nameField;
    private JTextField activeIngredientField;
    private JComboBox<String> categoryCombo;
    private JTextField manufacturerField;
    private JTextField descriptionField;
    
    // Units and pricing
    private JTextField unit1Field, unit2Field, unit3Field;
    private JTextField conversion1Field, conversion2Field;
    private JTextField price1Field, price2Field, price3Field;
    private JSpinner minStockSpinner;
    
    // Actions
    private JButton addButton;
    private JButton editButton;
    private JButton saveButton;
    private JButton cancelButton;
    private JButton deleteButton;
    
    private boolean isEditing = false;
    private int editingRow = -1;

    public ProductMasterPanel(InventoryPage parentPage) {
        this.parentPage = parentPage;
        
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
        
        // Product table
        String[] columns = {"Tên Thuốc", "Hoạt Chất", "Danh Mục", "ĐVT Chính", "Giá Bán", "Tồn Tối Thiểu"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        productTable = new JTable(productTableModel);
        setupProductTable();
        
        // Form fields
        nameField = createFormField();
        activeIngredientField = createFormField();
        manufacturerField = createFormField();
        descriptionField = createFormField();
        
        categoryCombo = new JComboBox<>(new String[]{
            "Thuốc kê đơn", "Thuốc không kê đơn", "Vitamin & TPCN", 
            "Thuốc tiêm", "Dụng cụ y tế", "Khác"
        });
        categoryCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Units and conversions
        unit1Field = createFormField(); // Main unit
        unit2Field = createFormField(); // Secondary unit
        unit3Field = createFormField(); // Tertiary unit
        
        conversion1Field = createFormField(); // 1 unit1 = ? unit2
        conversion2Field = createFormField(); // 1 unit2 = ? unit3
        
        // Pricing
        price1Field = createFormField();
        price2Field = createFormField();
        price3Field = createFormField();
        
        minStockSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 9999, 1));
        minStockSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Action buttons
        addButton = createActionButton("➕ Thêm Mới", new Color(76, 175, 80));
        editButton = createActionButton("✏️ Sửa", new Color(255, 152, 0));
        saveButton = createActionButton("💾 Lưu", new Color(33, 150, 243));
        cancelButton = createActionButton("❌ Hủy", new Color(158, 158, 158));
        deleteButton = createActionButton("🗑️ Xóa", new Color(244, 67, 54));
        
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
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

    private void setupProductTable() {
        productTable.setFont(new Font("Arial", Font.PLAIN, 14));
        productTable.setRowHeight(35);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.setBackground(Color.WHITE);
        productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        productTable.getTableHeader().setBackground(new Color(37, 47, 63));
        productTable.getTableHeader().setForeground(Color.WHITE);
        
        // Right align price column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        productTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        
        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        productTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        productTable.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("QUẢN LÝ DANH MỤC THUỐC", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(37, 47, 63));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Split pane: list + form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);

        // Left: Product list
        RoundedPanel listPanel = createProductListPanel();
        splitPane.setLeftComponent(listPanel);

        // Right: Product form
        RoundedPanel formPanel = createProductFormPanel();
        splitPane.setRightComponent(formPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private RoundedPanel createProductListPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Header with search
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        
        JLabel headerLabel = new JLabel("DANH SÁCH THUỐC");
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
        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private RoundedPanel createProductFormPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Header
        JLabel headerLabel = new JLabel("THÔNG TIN THUỐC");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Basic info
        addFormRow(formPanel, gbc, 0, "Tên thuốc:", nameField);
        addFormRow(formPanel, gbc, 1, "Hoạt chất:", activeIngredientField);
        addFormRow(formPanel, gbc, 2, "Danh mục:", categoryCombo);
        addFormRow(formPanel, gbc, 3, "Nhà sản xuất:", manufacturerField);
        addFormRow(formPanel, gbc, 4, "Mô tả:", descriptionField);

        // Separator
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator1 = new JSeparator();
        separator1.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        formPanel.add(separator1, gbc);

        // Units section
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        JLabel unitsLabel = new JLabel("ĐƠNN VỊ & QUY ĐỔI");
        unitsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        unitsLabel.setForeground(new Color(37, 47, 63));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        formPanel.add(unitsLabel, gbc);

        gbc.gridwidth = 1;
        addFormRow(formPanel, gbc, 7, "ĐVT chính:", unit1Field);
        addFormRow(formPanel, gbc, 8, "ĐVT phụ:", unit2Field);
        addFormRow(formPanel, gbc, 9, "ĐVT nhỏ:", unit3Field);
        addFormRow(formPanel, gbc, 10, "1 chính = ? phụ:", conversion1Field);
        addFormRow(formPanel, gbc, 11, "1 phụ = ? nhỏ:", conversion2Field);

        // Separator
        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JSeparator separator2 = new JSeparator();
        separator2.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        formPanel.add(separator2, gbc);

        // Pricing section
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        JLabel pricingLabel = new JLabel("GIÁ BÁN & TỒN KHO");
        pricingLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pricingLabel.setForeground(new Color(37, 47, 63));
        gbc.gridx = 0; gbc.gridy = 13; gbc.gridwidth = 2;
        formPanel.add(pricingLabel, gbc);

        gbc.gridwidth = 1;
        addFormRow(formPanel, gbc, 14, "Giá bán chính:", price1Field);
        addFormRow(formPanel, gbc, 15, "Giá bán phụ:", price2Field);
        addFormRow(formPanel, gbc, 16, "Giá bán nhỏ:", price3Field);
        addFormRow(formPanel, gbc, 17, "Tồn tối thiểu:", minStockSpinner);

        panel.add(formPanel, BorderLayout.CENTER);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(deleteButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addFormRow(JPanel parent, GridBagConstraints gbc, int row, String label, JComponent component) {
        gbc.gridx = 0; gbc.gridy = row;
        parent.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        component.setPreferredSize(new Dimension(200, 30));
        parent.add(component, gbc);
    }

    private void setupEventListeners() {
        // Table selection
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onProductSelected();
            }
        });

        // Action buttons
        addButton.addActionListener(e -> startAdd());
        editButton.addActionListener(e -> startEdit());
        saveButton.addActionListener(e -> saveProduct());
        cancelButton.addActionListener(e -> cancelOperation());
        deleteButton.addActionListener(e -> deleteProduct());
    }

    private void onProductSelected() {
        int row = productTable.getSelectedRow();
        boolean hasSelection = row >= 0;
        
        editButton.setEnabled(hasSelection && !isEditing);
        deleteButton.setEnabled(hasSelection && !isEditing);
        
        if (hasSelection) {
            loadProductToForm(row);
        }
    }

    private void loadProductToForm(int row) {
        // TODO: Load actual product data
        nameField.setText((String) productTableModel.getValueAt(row, 0));
        activeIngredientField.setText("Paracetamol"); // Mock data
        categoryCombo.setSelectedItem(productTableModel.getValueAt(row, 2));
        manufacturerField.setText("Công ty ABC");
        descriptionField.setText("Thuốc giảm đau, hạ sốt");
        
        unit1Field.setText("Hộp");
        unit2Field.setText("Vỉ");
        unit3Field.setText("Viên");
        conversion1Field.setText("10");
        conversion2Field.setText("10");
        
        price1Field.setText("100000");
        price2Field.setText("10000");
        price3Field.setText("1000");
        minStockSpinner.setValue(20);
    }

    private void startAdd() {
        isEditing = true;
        editingRow = -1;
        enableFormFields(true);
        clearForm();
        
        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    private void startEdit() {
        int row = productTable.getSelectedRow();
        if (row < 0) return;
        
        isEditing = true;
        editingRow = row;
        enableFormFields(true);
        
        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    private void saveProduct() {
        if (!validateForm()) return;
        
        Object[] rowData = {
            nameField.getText(),
            activeIngredientField.getText(),
            categoryCombo.getSelectedItem(),
            unit1Field.getText(),
            parentPage.getCurrencyFormatter().format(Double.parseDouble(price1Field.getText())),
            minStockSpinner.getValue().toString()
        };
        
        if (editingRow >= 0) {
            // Update existing
            for (int i = 0; i < rowData.length; i++) {
                productTableModel.setValueAt(rowData[i], editingRow, i);
            }
            JOptionPane.showMessageDialog(this, "Cập nhật thuốc thành công!", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // Add new
            productTableModel.addRow(rowData);
            JOptionPane.showMessageDialog(this, "Thêm thuốc mới thành công!", 
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
        onProductSelected(); // Refresh button states
    }

    private void deleteProduct() {
        int row = productTable.getSelectedRow();
        if (row < 0) return;
        
        String productName = (String) productTableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa thuốc '" + productName + "'?",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            productTableModel.removeRow(row);
            clearForm();
            JOptionPane.showMessageDialog(this, "Xóa thuốc thành công!", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
            // TODO: Delete from database
        }
    }

    private void enableFormFields(boolean enabled) {
        nameField.setEnabled(enabled);
        activeIngredientField.setEnabled(enabled);
        categoryCombo.setEnabled(enabled);
        manufacturerField.setEnabled(enabled);
        descriptionField.setEnabled(enabled);
        unit1Field.setEnabled(enabled);
        unit2Field.setEnabled(enabled);
        unit3Field.setEnabled(enabled);
        conversion1Field.setEnabled(enabled);
        conversion2Field.setEnabled(enabled);
        price1Field.setEnabled(enabled);
        price2Field.setEnabled(enabled);
        price3Field.setEnabled(enabled);
        minStockSpinner.setEnabled(enabled);
    }

    private void clearForm() {
        nameField.setText("");
        activeIngredientField.setText("");
        categoryCombo.setSelectedIndex(0);
        manufacturerField.setText("");
        descriptionField.setText("");
        unit1Field.setText("");
        unit2Field.setText("");
        unit3Field.setText("");
        conversion1Field.setText("");
        conversion2Field.setText("");
        price1Field.setText("");
        price2Field.setText("");
        price3Field.setText("");
        minStockSpinner.setValue(10);
    }

    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên thuốc!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return false;
        }
        
        if (unit1Field.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đơn vị chính!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            unit1Field.requestFocus();
            return false;
        }
        
        try {
            Double.parseDouble(price1Field.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập giá bán hợp lệ!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            price1Field.requestFocus();
            return false;
        }
        
        return true;
    }

    public void refreshData() {
        // TODO: Load actual data from database
        loadMockProductData();
    }

    private void loadMockProductData() {
        productTableModel.setRowCount(0);
        Object[][] mockData = {
            {"Paracetamol 500mg", "Paracetamol", "Thuốc không kê đơn", "Hộp", "100.000", "20"},
            {"Amoxicillin 250mg", "Amoxicillin", "Thuốc kê đơn", "Hộp", "150.000", "15"},
            {"Vitamin C 1000mg", "Acid Ascorbic", "Vitamin & TPCN", "Chai", "85.000", "10"},
            {"Ibuprofen 400mg", "Ibuprofen", "Thuốc không kê đơn", "Hộp", "120.000", "25"},
            {"Omeprazole 20mg", "Omeprazole", "Thuốc kê đơn", "Hộp", "200.000", "30"}
        };
        
        for (Object[] row : mockData) {
            productTableModel.addRow(row);
        }
        
        log.info("Product master data loaded - " + mockData.length + " products");
    }
} 