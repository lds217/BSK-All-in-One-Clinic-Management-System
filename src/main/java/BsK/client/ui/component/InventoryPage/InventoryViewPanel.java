package BsK.client.ui.component.InventoryPage;

import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.InventoryPage.common.InventoryButtonFactory;
import BsK.client.ui.component.InventoryPage.common.InventoryColorScheme;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class InventoryViewPanel extends JPanel {
    private InventoryPage parentPage;
    
    // Left panel components
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private JTable medicineTable;
    private DefaultTableModel medicineTableModel;
    
    // Right panel components
    private JLabel selectedMedicineLabel;
    private JTable batchTable;
    private DefaultTableModel batchTableModel;
    private JButton adjustStockButton;
    private JButton exportButton;
    
    // Current selection
    private String selectedMedicineName = "";
    private int selectedBatchRow = -1;
    
    // Date formatter
    private SimpleDateFormat dateFormatter;

    public InventoryViewPanel(InventoryPage parentPage) {
        this.parentPage = parentPage;
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        // Search and filter components
        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        categoryFilter = new JComboBox<>(new String[]{
            "Tất cả danh mục", "Thuốc kê đơn", "Thuốc không kê đơn", 
            "Vitamin & Thực phẩm chức năng", "Dụng cụ y tế"
        });
        categoryFilter.setFont(new Font("Arial", Font.PLAIN, 14));
        
        exportButton = InventoryButtonFactory.createSpecialButton("📤 Xuất Excel");
        
        // Medicine table (left panel)
        String[] medicineColumns = {"Tên Thuốc", "ĐVT", "Tổng Tồn", "Trạng Thái"};
        medicineTableModel = new DefaultTableModel(medicineColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        medicineTable = new JTable(medicineTableModel);
        setupMedicineTable();
        
        // Batch table (right panel)
        String[] batchColumns = {"Lô", "Hạn Sử Dụng", "Số Lượng", "Giá Nhập", "Ngày Nhập"};
        batchTableModel = new DefaultTableModel(batchColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        batchTable = new JTable(batchTableModel);
        setupBatchTable();
        
        // Selected medicine label
        selectedMedicineLabel = new JLabel("Chọn một loại thuốc để xem chi tiết lô");
        selectedMedicineLabel.setFont(new Font("Arial", Font.BOLD, 16));
        selectedMedicineLabel.setForeground(new Color(100, 100, 100));
        
        // Adjust stock button
        adjustStockButton = InventoryButtonFactory.createSecondaryButton("⚙️ Kiểm Kê");
        adjustStockButton.setEnabled(false);
    }

    private void setupMedicineTable() {
        medicineTable.setFont(new Font("Arial", Font.PLAIN, 14));
        medicineTable.setRowHeight(35);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setBackground(Color.WHITE);
        medicineTable.setGridColor(new Color(230, 230, 230));
        medicineTable.setShowVerticalLines(true);
        medicineTable.setShowHorizontalLines(true);
        medicineTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        medicineTable.getTableHeader().setBackground(InventoryColorScheme.TABLE_HEADER_BG);
        medicineTable.getTableHeader().setForeground(InventoryColorScheme.TABLE_HEADER_TEXT);
        medicineTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        TableColumn col0 = medicineTable.getColumnModel().getColumn(0); // Tên Thuốc
        col0.setPreferredWidth(250);
        TableColumn col1 = medicineTable.getColumnModel().getColumn(1); // ĐVT
        col1.setPreferredWidth(80);
        TableColumn col2 = medicineTable.getColumnModel().getColumn(2); // Tổng Tồn
        col2.setPreferredWidth(100);
        TableColumn col3 = medicineTable.getColumnModel().getColumn(3); // Trạng Thái
        col3.setPreferredWidth(120);
        
        // Custom renderer for status column
        medicineTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    String status = (String) value;
                    c.setBackground(InventoryColorScheme.getStatusBackgroundColor(status));
                    setForeground(InventoryColorScheme.getStatusColor(status));
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
        
        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        medicineTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        medicineTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    }

    private void setupBatchTable() {
        batchTable.setFont(new Font("Arial", Font.PLAIN, 14));
        batchTable.setRowHeight(35);
        batchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        batchTable.setBackground(Color.WHITE);
        batchTable.setGridColor(new Color(230, 230, 230));
        batchTable.setShowVerticalLines(true);
        batchTable.setShowHorizontalLines(true);
        batchTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        batchTable.getTableHeader().setBackground(InventoryColorScheme.TABLE_HEADER_BG);
        batchTable.getTableHeader().setForeground(InventoryColorScheme.TABLE_HEADER_TEXT);
        batchTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        TableColumn col0 = batchTable.getColumnModel().getColumn(0); // Lô
        col0.setPreferredWidth(120);
        TableColumn col1 = batchTable.getColumnModel().getColumn(1); // HSD
        col1.setPreferredWidth(100);
        TableColumn col2 = batchTable.getColumnModel().getColumn(2); // Số Lượng
        col2.setPreferredWidth(100);
        TableColumn col3 = batchTable.getColumnModel().getColumn(3); // Giá Nhập
        col3.setPreferredWidth(120);
        TableColumn col4 = batchTable.getColumnModel().getColumn(4); // Ngày Nhập
        col4.setPreferredWidth(100);
        
        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < batchTable.getColumnCount(); i++) {
            batchTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Right align price column
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        batchTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
    }

    private void layoutComponents() {
        // Top panel with search and filters
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // Main content: split pane with medicine list and batch details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);
        
        // Left panel: Medicine list
        JPanel leftPanel = createMedicineListPanel();
        splitPane.setLeftComponent(leftPanel);
        
        // Right panel: Batch details
        JPanel rightPanel = createBatchDetailsPanel();
        splitPane.setRightComponent(rightPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createTopPanel() {
        RoundedPanel topPanel = new RoundedPanel(10, Color.WHITE, true);
        topPanel.setLayout(new BorderLayout(15, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        // Left side: Search and filter
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftControls.setOpaque(false);
        
        // Search field with placeholder effect
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setOpaque(false);
        searchPanel.setPreferredSize(new Dimension(300, 35));
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        categoryFilter.setPreferredSize(new Dimension(200, 35));
        
        leftControls.add(new JLabel("Tìm kiếm:"));
        leftControls.add(searchPanel);
        leftControls.add(new JLabel("Danh mục:"));
        leftControls.add(categoryFilter);
        
        // Right side: Grouped action buttons
        JButton printReportButton = InventoryButtonFactory.createSecondaryButton("📊 In Báo Cáo");
        JPanel rightControls = InventoryButtonFactory.createButtonGroup(FlowLayout.RIGHT, exportButton, printReportButton);
        
        topPanel.add(leftControls, BorderLayout.WEST);
        topPanel.add(rightControls, BorderLayout.EAST);
        
        return topPanel;
    }

    private JPanel createMedicineListPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel titleLabel = new JLabel("DANH SÁCH THUỐC TRONG KHO");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(37, 47, 63));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(medicineTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createBatchDetailsPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header with medicine name and actions
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        selectedMedicineLabel.setText("CHI TIẾT LÔ THUỐC");
        headerPanel.add(selectedMedicineLabel, BorderLayout.WEST);
        headerPanel.add(adjustStockButton, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Table with scroll
        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private void setupEventListeners() {
        // Search field
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterMedicineList();
            }
        });
        
        // Category filter
        categoryFilter.addActionListener(e -> filterMedicineList());
        
        // Medicine table selection
        medicineTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onMedicineSelected();
                }
            }
        });
        
        // Batch table selection
        batchTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onBatchSelected();
                }
            }
        });
        
        // Export button
        exportButton.addActionListener(e -> exportToExcel());
        
        // Adjust stock button
        adjustStockButton.addActionListener(e -> openStockAdjustment());
    }

    private void filterMedicineList() {
        // TODO: Implement filtering logic
        log.info("Filtering medicine list with search: '" + searchField.getText() + 
                "' and category: '" + categoryFilter.getSelectedItem() + "'");
    }

    private void onMedicineSelected() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow >= 0) {
            selectedMedicineName = (String) medicineTableModel.getValueAt(selectedRow, 0);
            selectedMedicineLabel.setText("CHI TIẾT LÔ: " + selectedMedicineName);
            loadBatchDetails(selectedMedicineName);
            adjustStockButton.setEnabled(false); // Will be enabled when batch is selected
        }
    }

    private void onBatchSelected() {
        selectedBatchRow = batchTable.getSelectedRow();
        adjustStockButton.setEnabled(selectedBatchRow >= 0);
    }

    private void loadBatchDetails(String medicineName) {
        // Clear existing data
        batchTableModel.setRowCount(0);
        
        // TODO: Load actual batch data from database
        loadMockBatchData(medicineName);
    }

    private void loadMockBatchData(String medicineName) {
        // Mock batch data
        Object[][] batchData = {
            {medicineName.substring(0, 2).toUpperCase() + "202408", "15/08/2025", "45", "52.000", "10/01/2024"},
            {medicineName.substring(0, 2).toUpperCase() + "202411", "30/11/2025", "18", "55.000", "15/03/2024"},
            {medicineName.substring(0, 2).toUpperCase() + "202501", "15/02/2026", "30", "58.000", "20/05/2024"}
        };
        
        for (Object[] row : batchData) {
            batchTableModel.addRow(row);
        }
        
        log.info("Loaded batch details for: " + medicineName);
    }

    private void exportToExcel() {
        // TODO: Implement Excel export functionality
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất danh sách tồn kho");
        fileChooser.setSelectedFile(new File("TonKho_" + dateFormatter.format(new Date()) + ".xlsx"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // TODO: Export logic here
            JOptionPane.showMessageDialog(this, 
                "Xuất file thành công!\nĐường dẫn: " + selectedFile.getAbsolutePath(),
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openStockAdjustment() {
        if (selectedBatchRow >= 0) {
            String batchNumber = (String) batchTableModel.getValueAt(selectedBatchRow, 0);
            String currentQuantity = (String) batchTableModel.getValueAt(selectedBatchRow, 2);
            
            // TODO: Open stock adjustment dialog
            log.info("Opening stock adjustment for batch: " + batchNumber + " (current: " + currentQuantity + ")");
            
            parentPage.navigateToStockAdjustment();
        }
    }

    public void refreshData() {
        // TODO: Load actual data from database
        loadMockMedicineData();
    }

    private void loadMockMedicineData() {
        // Clear existing data
        medicineTableModel.setRowCount(0);
        
        // Mock medicine data with enhanced status showing quantity ratios
        Object[][] medicineData = {
            {"Paracetamol 500mg", "Hộp", "63", "63/20 - Bình thường"},
            {"Amoxicillin 250mg", "Hộp", "112", "112/30 - Bình thường"},
            {"Vitamin C 1000mg", "Chai", "8", "8/20 - Sắp hết"},
            {"Ibuprofen 400mg", "Hộp", "45", "45/25 - Bình thường"},
            {"Aspirin 100mg", "Hộp", "5", "5/15 - Sắp hết"},
            {"Omeprazole 20mg", "Hộp", "78", "78/30 - Bình thường"},
            {"Metformin 500mg", "Hộp", "92", "92/40 - Bình thường"},
            {"Atorvastatin 20mg", "Hộp", "34", "34/25 - Bình thường"},
            {"Amlodipine 5mg", "Hộp", "67", "67/30 - Bình thường"},
            {"Losartan 50mg", "Hộp", "12", "12/25 - Sắp hết"}
        };
        
        for (Object[] row : medicineData) {
            medicineTableModel.addRow(row);
        }
        
        log.info("Medicine inventory data refreshed - " + medicineData.length + " items loaded");
    }
} 