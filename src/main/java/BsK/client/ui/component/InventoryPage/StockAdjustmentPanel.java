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
import java.util.Date;

@Slf4j
public class StockAdjustmentPanel extends JPanel {
    private InventoryPage parentPage;
    
    // Search and selection
    private JTextField medicineSearchField;
    private JTable medicineTable;
    private DefaultTableModel medicineTableModel;
    private JTable batchTable;
    private DefaultTableModel batchTableModel;
    
    // Adjustment form
    private JLabel selectedMedicineLabel;
    private JLabel selectedBatchLabel;
    private JLabel currentQuantityLabel;
    private JSpinner actualQuantitySpinner;
    private JTextArea reasonTextArea;
    private JButton adjustButton;
    private JButton cancelButton;
    
    // Adjustment history
    private JTable historyTable;
    private DefaultTableModel historyTableModel;
    
    private SimpleDateFormat dateFormatter;

    public StockAdjustmentPanel(InventoryPage parentPage) {
        this.parentPage = parentPage;
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        // Medicine search
        medicineSearchField = new JTextField();
        medicineSearchField.setFont(new Font("Arial", Font.PLAIN, 14));
        medicineSearchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        // Medicine table
        String[] medicineColumns = {"Tên Thuốc", "Tổng Tồn", "Trạng Thái"};
        medicineTableModel = new DefaultTableModel(medicineColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        medicineTable = new JTable(medicineTableModel);
        setupMedicineTable();
        
        // Batch table
        String[] batchColumns = {"Lô", "HSD", "Tồn Hiện Tại"};
        batchTableModel = new DefaultTableModel(batchColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        batchTable = new JTable(batchTableModel);
        setupBatchTable();
        
        // Adjustment form components
        selectedMedicineLabel = new JLabel("Chưa chọn thuốc");
        selectedMedicineLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        selectedBatchLabel = new JLabel("Chưa chọn lô");
        selectedBatchLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        selectedBatchLabel.setForeground(new Color(100, 100, 100));
        
        currentQuantityLabel = new JLabel("0");
        currentQuantityLabel.setFont(new Font("Arial", Font.BOLD, 18));
        currentQuantityLabel.setForeground(new Color(63, 81, 181));
        
        actualQuantitySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
        actualQuantitySpinner.setFont(new Font("Arial", Font.PLAIN, 16));
        ((JSpinner.DefaultEditor) actualQuantitySpinner.getEditor()).getTextField().setHorizontalAlignment(JTextField.CENTER);
        
        reasonTextArea = new JTextArea(3, 30);
        reasonTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        reasonTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        reasonTextArea.setLineWrap(true);
        reasonTextArea.setWrapStyleWord(true);
        
        adjustButton = new JButton("⚙️ Thực Hiện Kiểm Kê");
        adjustButton.setFont(new Font("Arial", Font.BOLD, 16));
        adjustButton.setBackground(new Color(255, 152, 0));
        adjustButton.setForeground(Color.WHITE);
        adjustButton.setFocusPainted(false);
        adjustButton.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        adjustButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        adjustButton.setEnabled(false);
        
        cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 14));
        cancelButton.setBackground(new Color(158, 158, 158));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // History table
        String[] historyColumns = {"Ngày", "Thuốc", "Lô", "Trước", "Sau", "Chênh Lệch", "Lý Do"};
        historyTableModel = new DefaultTableModel(historyColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(historyTableModel);
        setupHistoryTable();
    }

    private void setupMedicineTable() {
        medicineTable.setFont(new Font("Arial", Font.PLAIN, 14));
        medicineTable.setRowHeight(35);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setBackground(Color.WHITE);
        medicineTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        medicineTable.getTableHeader().setBackground(new Color(37, 47, 63));
        medicineTable.getTableHeader().setForeground(Color.WHITE);
    }

    private void setupBatchTable() {
        batchTable.setFont(new Font("Arial", Font.PLAIN, 14));
        batchTable.setRowHeight(35);
        batchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        batchTable.setBackground(Color.WHITE);
        batchTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        batchTable.getTableHeader().setBackground(new Color(37, 47, 63));
        batchTable.getTableHeader().setForeground(Color.WHITE);
    }

    private void setupHistoryTable() {
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
        historyTable.setRowHeight(30);
        historyTable.setBackground(Color.WHITE);
        historyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        historyTable.getTableHeader().setBackground(new Color(37, 47, 63));
        historyTable.getTableHeader().setForeground(Color.WHITE);
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("KIỂM KÊ KHO THUỐC", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(37, 47, 63));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Main content split pane
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplit.setDividerLocation(400);
        mainSplit.setResizeWeight(0.6);

        // Top: Selection and adjustment
        JPanel topPanel = createSelectionAndAdjustmentPanel();
        mainSplit.setTopComponent(topPanel);

        // Bottom: History
        RoundedPanel historyPanel = createHistoryPanel();
        mainSplit.setBottomComponent(historyPanel);

        mainPanel.add(mainSplit, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createSelectionAndAdjustmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setOpaque(false);

        // Left: Medicine and batch selection
        JSplitPane selectionSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        selectionSplit.setDividerLocation(300);
        selectionSplit.setResizeWeight(0.5);

        RoundedPanel medicinePanel = createMedicineSelectionPanel();
        RoundedPanel batchPanel = createBatchSelectionPanel();

        selectionSplit.setLeftComponent(medicinePanel);
        selectionSplit.setRightComponent(batchPanel);

        // Right: Adjustment form
        RoundedPanel adjustmentPanel = createAdjustmentFormPanel();

        panel.add(selectionSplit, BorderLayout.CENTER);
        panel.add(adjustmentPanel, BorderLayout.EAST);

        return panel;
    }

    private RoundedPanel createMedicineSelectionPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("1. Chọn Thuốc");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Search
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("🔍"), BorderLayout.WEST);
        searchPanel.add(medicineSearchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.CENTER);

        // Table
        JScrollPane scrollPane = new JScrollPane(medicineTable);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private RoundedPanel createBatchSelectionPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("2. Chọn Lô Cần Kiểm Kê");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Table
        JScrollPane scrollPane = new JScrollPane(batchTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private RoundedPanel createAdjustmentFormPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setPreferredSize(new Dimension(350, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Header
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel headerLabel = new JLabel("3. Thực Hiện Kiểm Kê");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        panel.add(headerLabel, gbc);

        // Selected medicine
        gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Thuốc:"), gbc);
        gbc.gridx = 1;
        panel.add(selectedMedicineLabel, gbc);

        // Selected batch
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Lô:"), gbc);
        gbc.gridx = 1;
        panel.add(selectedBatchLabel, gbc);

        // Current quantity
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Tồn hiện tại:"), gbc);
        gbc.gridx = 1;
        panel.add(currentQuantityLabel, gbc);

        // Actual quantity
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(new JLabel("Số lượng thực tế:"), gbc);
        gbc.gridx = 1;
        panel.add(actualQuantitySpinner, gbc);

        // Reason
        gbc.gridx = 0; gbc.gridy = 5; gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Lý do:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH;
        JScrollPane reasonScroll = new JScrollPane(reasonTextArea);
        reasonScroll.setPreferredSize(new Dimension(200, 80));
        panel.add(reasonScroll, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(adjustButton);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private RoundedPanel createHistoryPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Lịch Sử Kiểm Kê");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(37, 47, 63));
        panel.add(headerLabel, BorderLayout.NORTH);

        // Table
        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupEventListeners() {
        // Medicine table selection
        medicineTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onMedicineSelected();
            }
        });

        // Batch table selection
        batchTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onBatchSelected();
            }
        });

        // Adjust button
        adjustButton.addActionListener(e -> performStockAdjustment());

        // Cancel button
        cancelButton.addActionListener(e -> clearSelection());
    }

    private void onMedicineSelected() {
        int row = medicineTable.getSelectedRow();
        if (row >= 0) {
            String medicineName = (String) medicineTableModel.getValueAt(row, 0);
            selectedMedicineLabel.setText(medicineName);
            loadBatchesForMedicine(medicineName);
            clearBatchSelection();
        }
    }

    private void onBatchSelected() {
        int row = batchTable.getSelectedRow();
        if (row >= 0) {
            String batch = (String) batchTableModel.getValueAt(row, 0);
            String currentQty = (String) batchTableModel.getValueAt(row, 2);
            
            selectedBatchLabel.setText("Lô: " + batch);
            currentQuantityLabel.setText(currentQty);
            
            try {
                int qty = Integer.parseInt(currentQty);
                actualQuantitySpinner.setValue(qty);
            } catch (NumberFormatException e) {
                actualQuantitySpinner.setValue(0);
            }
            
            adjustButton.setEnabled(true);
        }
    }

    private void loadBatchesForMedicine(String medicineName) {
        batchTableModel.setRowCount(0);
        
        // TODO: Load actual batch data from database
        // Mock data
        Object[][] mockBatches = {
            {"PA202408", "15/08/2025", "45"},
            {"PA202411", "30/11/2025", "18"},
            {"PA202501", "15/02/2026", "30"}
        };
        
        for (Object[] batch : mockBatches) {
            batchTableModel.addRow(batch);
        }
    }

    private void performStockAdjustment() {
        String medicineName = selectedMedicineLabel.getText();
        String batch = selectedBatchLabel.getText();
        String currentQtyStr = currentQuantityLabel.getText();
        int actualQty = (Integer) actualQuantitySpinner.getValue();
        String reason = reasonTextArea.getText().trim();
        
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập lý do kiểm kê!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int currentQty = Integer.parseInt(currentQtyStr);
            int difference = actualQty - currentQty;
            
            // Add to history
            historyTableModel.insertRow(0, new Object[]{
                dateFormatter.format(new Date()),
                medicineName,
                batch,
                currentQty,
                actualQty,
                (difference >= 0 ? "+" : "") + difference,
                reason
            });
            
            // TODO: Update database
            JOptionPane.showMessageDialog(this, 
                "Kiểm kê thành công!\nChênh lệch: " + difference + " đơn vị", 
                "Thành công", JOptionPane.INFORMATION_MESSAGE);
            
            // Update the batch table
            int selectedRow = batchTable.getSelectedRow();
            if (selectedRow >= 0) {
                batchTableModel.setValueAt(String.valueOf(actualQty), selectedRow, 2);
            }
            
            clearSelection();
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Lỗi dữ liệu số lượng!", 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearSelection() {
        medicineTable.clearSelection();
        batchTable.clearSelection();
        clearBatchSelection();
    }

    private void clearBatchSelection() {
        selectedMedicineLabel.setText("Chưa chọn thuốc");
        selectedBatchLabel.setText("Chưa chọn lô");
        currentQuantityLabel.setText("0");
        actualQuantitySpinner.setValue(0);
        reasonTextArea.setText("");
        adjustButton.setEnabled(false);
    }

    public void refreshData() {
        // TODO: Load actual data from database
        loadMockMedicineData();
        loadMockHistoryData();
    }

    private void loadMockMedicineData() {
        medicineTableModel.setRowCount(0);
        Object[][] mockData = {
            {"Paracetamol 500mg", "63", "Bình thường"},
            {"Amoxicillin 250mg", "112", "Bình thường"},
            {"Vitamin C 1000mg", "8", "Sắp hết"},
            {"Ibuprofen 400mg", "45", "Bình thường"}
        };
        
        for (Object[] row : mockData) {
            medicineTableModel.addRow(row);
        }
    }

    private void loadMockHistoryData() {
        historyTableModel.setRowCount(0);
        Object[][] mockHistory = {
            {"20/06/2024 14:30", "Paracetamol 500mg", "PA202408", "50", "45", "-5", "Hỏng do ẩm"},
            {"18/06/2024 09:15", "Vitamin C 1000mg", "VC202405", "15", "12", "-3", "Hết hạn"},
            {"15/06/2024 16:45", "Amoxicillin 250mg", "AM202409", "100", "112", "+12", "Tìm thấy thêm trong kho"}
        };
        
        for (Object[] row : mockHistory) {
            historyTableModel.addRow(row);
        }
    }
} 