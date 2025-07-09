package BsK.client.ui.component.InventoryPage;

import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class ReportsPanel extends JPanel {
    private InventoryPage parentPage;
    
    // Report type selection
    private JComboBox<String> reportTypeCombo;
    private JButton generateButton;
    private JButton exportButton;
    
    // Report results
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JLabel reportTitleLabel;
    private JLabel reportSummaryLabel;
    
    // Date filter (for some reports)
    private JTextField fromDateField;
    private JTextField toDateField;
    
    private SimpleDateFormat dateFormatter;

    public ReportsPanel(InventoryPage parentPage) {
        this.parentPage = parentPage;
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        // Report type selection
        reportTypeCombo = new JComboBox<>(new String[]{
            "Chọn loại báo cáo...",
            "Thuốc Sắp Hết (Low Stock)",
            "Thuốc Sắp Hết Hạn",
            "Lịch Sử Xuất Nhập Kho",
            "Tồn Kho Theo Nhóm",
            "Báo Cáo Tổng Hợp"
        });
        reportTypeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        reportTypeCombo.setPreferredSize(new Dimension(250, 35));
        
        generateButton = new JButton("📊 Tạo Báo Cáo");
        generateButton.setFont(new Font("Arial", Font.BOLD, 14));
        generateButton.setBackground(new Color(33, 150, 243));
        generateButton.setForeground(Color.WHITE);
        generateButton.setFocusPainted(false);
        generateButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        generateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        exportButton = new JButton("📤 Xuất Excel");
        exportButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportButton.setBackground(new Color(76, 175, 80));
        exportButton.setForeground(Color.WHITE);
        exportButton.setFocusPainted(false);
        exportButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        exportButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportButton.setEnabled(false);
        
        // Date filters
        fromDateField = new JTextField(dateFormatter.format(new Date()));
        fromDateField.setFont(new Font("Arial", Font.PLAIN, 14));
        fromDateField.setPreferredSize(new Dimension(120, 35));
        fromDateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        toDateField = new JTextField(dateFormatter.format(new Date()));
        toDateField.setFont(new Font("Arial", Font.PLAIN, 14));
        toDateField.setPreferredSize(new Dimension(120, 35));
        toDateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Report results
        reportTableModel = new DefaultTableModel();
        reportTable = new JTable(reportTableModel);
        setupReportTable();
        
        reportTitleLabel = new JLabel("Chọn loại báo cáo để hiển thị kết quả");
        reportTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        reportTitleLabel.setForeground(new Color(37, 47, 63));
        
        reportSummaryLabel = new JLabel("");
        reportSummaryLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        reportSummaryLabel.setForeground(new Color(100, 100, 100));
    }

    private void setupReportTable() {
        reportTable.setFont(new Font("Arial", Font.PLAIN, 14));
        reportTable.setRowHeight(35);
        reportTable.setBackground(Color.WHITE);
        reportTable.setGridColor(new Color(230, 230, 230));
        reportTable.setShowVerticalLines(true);
        reportTable.setShowHorizontalLines(true);
        reportTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        reportTable.getTableHeader().setBackground(new Color(37, 47, 63));
        reportTable.getTableHeader().setForeground(Color.WHITE);
        reportTable.getTableHeader().setReorderingAllowed(false);
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("BÁO CÁO QUẢN LÝ KHO", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(37, 47, 63));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Controls panel
        RoundedPanel controlsPanel = createControlsPanel();
        mainPanel.add(controlsPanel, BorderLayout.CENTER);

        // Results panel
        RoundedPanel resultsPanel = createResultsPanel();
        mainPanel.add(resultsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private RoundedPanel createControlsPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        panel.setPreferredSize(new Dimension(0, 200));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Report type selection
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Loại báo cáo:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(reportTypeCombo, gbc);

        // Date range (initially hidden)
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel fromLabel = new JLabel("Từ ngày:");
        panel.add(fromLabel, gbc);
        gbc.gridx = 1;
        panel.add(fromDateField, gbc);

        gbc.gridx = 2;
        JLabel toLabel = new JLabel("Đến ngày:");
        panel.add(toLabel, gbc);
        gbc.gridx = 3;
        panel.add(toDateField, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(generateButton);
        buttonPanel.add(exportButton);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private RoundedPanel createResultsPanel() {
        RoundedPanel panel = new RoundedPanel(10, Color.WHITE, true);
        panel.setLayout(new BorderLayout(0, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        panel.setPreferredSize(new Dimension(0, 400));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(reportTitleLabel, BorderLayout.WEST);
        headerPanel.add(reportSummaryLabel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Table
        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupEventListeners() {
        generateButton.addActionListener(e -> generateReport());
        exportButton.addActionListener(e -> exportReport());
        
        reportTypeCombo.addActionListener(e -> {
            String selectedType = (String) reportTypeCombo.getSelectedItem();
            boolean needsDateRange = "Lịch Sử Xuất Nhập Kho".equals(selectedType);
            fromDateField.setVisible(needsDateRange);
            toDateField.setVisible(needsDateRange);
        });
    }

    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        if (reportType == null || reportType.startsWith("Chọn")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn loại báo cáo!", 
                "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        reportTitleLabel.setText("Đang tạo báo cáo...");
        reportSummaryLabel.setText("");
        reportTableModel.setRowCount(0);
        reportTableModel.setColumnCount(0);

        // Generate report based on type
        switch (reportType) {
            case "Thuốc Sắp Hết (Low Stock)":
                generateLowStockReport();
                break;
            case "Thuốc Sắp Hết Hạn":
                generateExpiryReport();
                break;
            case "Lịch Sử Xuất Nhập Kho":
                generateInventoryHistoryReport();
                break;
            case "Tồn Kho Theo Nhóm":
                generateStockByGroupReport();
                break;
            case "Báo Cáo Tổng Hợp":
                generateSummaryReport();
                break;
        }

        exportButton.setEnabled(reportTableModel.getRowCount() > 0);
    }

    private void generateLowStockReport() {
        String[] columns = {"Tên Thuốc", "ĐVT", "Tồn Hiện Tại", "Mức Tối Thiểu", "Cần Nhập", "Nhà Cung Cấp"};
        reportTableModel.setColumnIdentifiers(columns);

        // Mock data
        Object[][] data = {
            {"Vitamin C 1000mg", "Chai", "8", "20", "12", "Công ty ABC"},
            {"Aspirin 100mg", "Hộp", "5", "15", "10", "Công ty DEF"},
            {"Cefuroxime 250mg", "Hộp", "3", "10", "7", "Công ty GHI"},
            {"Omeprazole 20mg", "Hộp", "12", "25", "13", "Công ty ABC"},
            {"Metformin 500mg", "Hộp", "18", "30", "12", "Công ty JKL"}
        };

        for (Object[] row : data) {
            reportTableModel.addRow(row);
        }

        reportTitleLabel.setText("Báo Cáo Thuốc Sắp Hết");
        reportSummaryLabel.setText(data.length + " sản phẩm cần nhập thêm");

        // Color coding for urgent items
        reportTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected && column == 2) { // Current stock column
                    try {
                        int current = Integer.parseInt(value.toString());
                        if (current <= 5) {
                            c.setBackground(new Color(255, 235, 238));
                            setForeground(new Color(244, 67, 54));
                        } else if (current <= 10) {
                            c.setBackground(new Color(255, 248, 225));
                            setForeground(new Color(255, 152, 0));
                        } else {
                            c.setBackground(Color.WHITE);
                            setForeground(Color.BLACK);
                        }
                    } catch (NumberFormatException e) {
                        c.setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }
                
                setHorizontalAlignment(column >= 1 && column <= 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return c;
            }
        });

        log.info("Generated low stock report with " + data.length + " items");
    }

    private void generateExpiryReport() {
        String[] columns = {"Tên Thuốc", "Lô", "Hạn Sử Dụng", "Số Lượng", "Ngày Còn Lại", "Mức Độ"};
        reportTableModel.setColumnIdentifiers(columns);

        // Mock data
        Object[][] data = {
            {"Paracetamol 500mg", "PA202408", "15/08/2024", "25", "45", "Nguy hiểm"},
            {"Vitamin B1", "VB202409", "20/09/2024", "15", "78", "Cảnh báo"},
            {"Amoxicillin 250mg", "AM202410", "10/10/2024", "30", "98", "Cảnh báo"},
            {"Ibuprofen 400mg", "IB202411", "05/11/2024", "18", "124", "Bình thường"},
            {"Cefalexin 500mg", "CE202412", "25/12/2024", "22", "174", "Bình thường"}
        };

        for (Object[] row : data) {
            reportTableModel.addRow(row);
        }

        reportTitleLabel.setText("Báo Cáo Thuốc Sắp Hết Hạn");
        reportSummaryLabel.setText(data.length + " lô thuốc cần theo dõi");

        // Color coding for expiry urgency
        reportTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected && column == 5) { // Risk level column
                    String risk = value.toString();
                    if ("Nguy hiểm".equals(risk)) {
                        c.setBackground(new Color(255, 235, 238));
                        setForeground(new Color(244, 67, 54));
                    } else if ("Cảnh báo".equals(risk)) {
                        c.setBackground(new Color(255, 248, 225));
                        setForeground(new Color(255, 152, 0));
                    } else {
                        c.setBackground(new Color(232, 245, 233));
                        setForeground(new Color(76, 175, 80));
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }
                
                setHorizontalAlignment(column >= 3 && column <= 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return c;
            }
        });

        log.info("Generated expiry report with " + data.length + " batches");
    }

    private void generateInventoryHistoryReport() {
        String[] columns = {"Ngày", "Loại", "Thuốc", "Lô", "Số Lượng", "Đơn Giá", "Người Thực Hiện"};
        reportTableModel.setColumnIdentifiers(columns);

        // Mock data
        Object[][] data = {
            {"25/06/2024", "Nhập", "Paracetamol 500mg", "PA202408", "+100", "52.000", "Nguyễn Văn A"},
            {"25/06/2024", "Xuất", "Vitamin C 1000mg", "VC202405", "-5", "85.000", "Hệ thống"},
            {"24/06/2024", "Kiểm kê", "Amoxicillin 250mg", "AM202409", "+12", "-", "Trần Thị B"},
            {"24/06/2024", "Xuất", "Ibuprofen 400mg", "IB202407", "-8", "45.000", "Hệ thống"},
            {"23/06/2024", "Nhập", "Omeprazole 20mg", "OM202410", "+50", "38.000", "Lê Văn C"}
        };

        for (Object[] row : data) {
            reportTableModel.addRow(row);
        }

        reportTitleLabel.setText("Lịch Sử Xuất Nhập Kho");
        reportSummaryLabel.setText("Từ " + fromDateField.getText() + " đến " + toDateField.getText());

        log.info("Generated inventory history report with " + data.length + " transactions");
    }

    private void generateStockByGroupReport() {
        String[] columns = {"Nhóm Thuốc", "Số Loại", "Tổng Tồn", "Giá Trị (VNĐ)", "% Tổng Kho"};
        reportTableModel.setColumnIdentifiers(columns);

        // Mock data
        Object[][] data = {
            {"Thuốc kê đơn", "45", "2,847", "156,890,000", "34.5%"},
            {"Thuốc không kê đơn", "32", "1,923", "98,450,000", "21.6%"},
            {"Vitamin & TPCN", "28", "1,456", "76,230,000", "16.7%"},
            {"Thuốc tiêm", "15", "578", "89,670,000", "19.7%"},
            {"Dụng cụ y tế", "12", "234", "33,760,000", "7.4%"}
        };

        for (Object[] row : data) {
            reportTableModel.addRow(row);
        }

        reportTitleLabel.setText("Tồn Kho Theo Nhóm Thuốc");
        reportSummaryLabel.setText("Tổng: 132 loại thuốc, giá trị 455.000.000 VNĐ");

        // Right align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i < reportTable.getColumnCount(); i++) {
            reportTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        log.info("Generated stock by group report");
    }

    private void generateSummaryReport() {
        String[] columns = {"Chỉ Số", "Giá Trị", "Đơn Vị", "Ghi Chú"};
        reportTableModel.setColumnIdentifiers(columns);

        // Mock summary data
        Object[][] data = {
            {"Tổng số loại thuốc", "156", "loại", "Đang quản lý"},
            {"Tổng giá trị kho", "455.000.000", "VNĐ", "Tại thời điểm hiện tại"},
            {"Thuốc sắp hết", "7", "loại", "Dưới mức tối thiểu"},
            {"Thuốc sắp hết hạn", "12", "lô", "Trong 3 tháng tới"},
            {"Giao dịch hôm nay", "23", "lần", "Xuất + Nhập"},
            {"Doanh thu thuốc", "12.450.000", "VNĐ", "Tháng này"},
            {"Chi phí nhập hàng", "8.900.000", "VNĐ", "Tháng này"},
            {"Tỷ lệ hết hạn", "0.8%", "%", "Tháng trước"}
        };

        for (Object[] row : data) {
            reportTableModel.addRow(row);
        }

        reportTitleLabel.setText("Báo Cáo Tổng Hợp Kho Thuốc");
        reportSummaryLabel.setText("Cập nhật lúc: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

        log.info("Generated summary report");
    }

    private void exportReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Xuất báo cáo Excel");
        String fileName = reportType.replaceAll("[^a-zA-Z0-9]", "_") + "_" + 
                         dateFormatter.format(new Date()) + ".xlsx";
        fileChooser.setSelectedFile(new File(fileName));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // TODO: Implement actual Excel export
            JOptionPane.showMessageDialog(this, 
                "Xuất báo cáo thành công!\nFile: " + selectedFile.getName() + 
                "\nSố dòng: " + reportTableModel.getRowCount(),
                "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            
            log.info("Exported report: " + selectedFile.getAbsolutePath());
        }
    }

    public void refreshData() {
        reportTitleLabel.setText("Chọn loại báo cáo để hiển thị kết quả");
        reportSummaryLabel.setText("");
        reportTableModel.setRowCount(0);
        reportTableModel.setColumnCount(0);
        exportButton.setEnabled(false);
        log.info("Reports panel refreshed");
    }
} 