package BsK.client.ui.component.DataDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataDialog extends JDialog {
    
    private JTextField searchField;
    private JComboBox<String> doctorComboBox;
    private JSpinner fromDateSpinner;
    private JSpinner toDateSpinner;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JLabel resultCountLabel;
    private JButton prevPageButton, nextPageButton, firstPageButton, lastPageButton;
    private JLabel currentPageLabel;
    
    private int currentPage = 1;
    private int totalPages = 8;
    private int recordsPerPage = 20;
    private int totalRecords = 157;
    
    public DataDialog(JFrame parent) {
        super(parent, "Quản Lý Dữ Liệu Khám Bệnh", true);
        initializeDialog();
        loadFakeData();
    }
    
    private void initializeDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        
        // Create main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Add components
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(createDataGridPanel(), BorderLayout.CENTER);
        mainPanel.add(createPaginationPanel(), BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Tìm kiếm và Lọc dữ liệu"));
        
        // Top row with search and main action buttons
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        // Search field
        searchField = new JTextField("Tìm theo tên bệnh nhân, mã bệnh nhân...", 25);
        searchField.setForeground(Color.GRAY);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals("Tìm theo tên bệnh nhân, mã bệnh nhân...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        
        JButton filterButton = new JButton("Lọc");
        filterButton.setBackground(new Color(51, 135, 204));
        filterButton.setForeground(Color.WHITE);
        filterButton.setPreferredSize(new Dimension(80, 30));
        
        JButton clearFilterButton = new JButton("Xóa bộ lọc");
        clearFilterButton.setPreferredSize(new Dimension(100, 30));
        
        JButton exportExcelButton = new JButton("Xuất Excel");
        exportExcelButton.setBackground(new Color(66, 157, 21));
        exportExcelButton.setForeground(Color.WHITE);
        exportExcelButton.setPreferredSize(new Dimension(100, 30));
        
        JButton addNewButton = new JButton("+ Thêm mới");
        addNewButton.setBackground(new Color(200, 138, 16));
        addNewButton.setForeground(Color.WHITE);
        addNewButton.setPreferredSize(new Dimension(120, 30));
        
        topRow.add(new JLabel("🔍"));
        topRow.add(searchField);
        topRow.add(filterButton);
        topRow.add(clearFilterButton);
        topRow.add(Box.createHorizontalStrut(20));
        topRow.add(exportExcelButton);
        topRow.add(addNewButton);
        
        // Bottom row with advanced filters
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        bottomRow.add(new JLabel("Từ ngày:"));
        fromDateSpinner = new JSpinner(new SpinnerDateModel());
        fromDateSpinner.setEditor(new JSpinner.DateEditor(fromDateSpinner, "dd/MM/yyyy"));
        fromDateSpinner.setPreferredSize(new Dimension(100, 25));
        bottomRow.add(fromDateSpinner);
        
        bottomRow.add(new JLabel("Đến ngày:"));
        toDateSpinner = new JSpinner(new SpinnerDateModel());
        toDateSpinner.setEditor(new JSpinner.DateEditor(toDateSpinner, "dd/MM/yyyy"));
        toDateSpinner.setPreferredSize(new Dimension(100, 25));
        bottomRow.add(toDateSpinner);
        
        bottomRow.add(Box.createHorizontalStrut(20));
        bottomRow.add(new JLabel("Bác sĩ:"));
        String[] doctors = {"Tất cả", "BS. Nguyễn Thiên Phúc", "BS. Trần Minh Anh", "BS. Lê Hoàng Nam", "BS. Phạm Thị Lan"};
        doctorComboBox = new JComboBox<>(doctors);
        doctorComboBox.setPreferredSize(new Dimension(150, 25));
        bottomRow.add(doctorComboBox);
        
        controlPanel.add(topRow, BorderLayout.NORTH);
        controlPanel.add(bottomRow, BorderLayout.SOUTH);
        
        // Add action listeners
        addNewButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Chức năng thêm mới sẽ chuyển đến trang Thăm khám với form trống", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        
        exportExcelButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Chức năng xuất Excel đang được phát triển", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
        
        filterButton.addActionListener(e -> applyFilters());
        clearFilterButton.addActionListener(e -> clearFilters());
        
        return controlPanel;
    }
    
    private JPanel createDataGridPanel() {
        JPanel gridPanel = new JPanel(new BorderLayout());
        
        // Result count label
        resultCountLabel = new JLabel("Hiển thị 1 đến 20 của 157 kết quả");
        resultCountLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        resultCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gridPanel.add(resultCountLabel, BorderLayout.NORTH);
        
        // Create table
        String[] columnNames = {"STT", "Mã BN", "Họ và Tên", "Ngày khám", "Bác sĩ khám", "Kết luận", "Hành động"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only actions column is editable
            }
        };
        
        dataTable = new JTable(tableModel);
        dataTable.setRowHeight(35);
        dataTable.setFont(new Font("Arial", Font.PLAIN, 12));
        dataTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        dataTable.getTableHeader().setBackground(new Color(240, 240, 240));
        dataTable.setSelectionBackground(new Color(230, 240, 255));
        
        // Set column widths
        TableColumn column;
        int[] columnWidths = {50, 80, 150, 100, 130, 200, 120};
        for (int i = 0; i < columnWidths.length; i++) {
            column = dataTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(columnWidths[i]);
        }
        
        // Custom renderer for actions column
        dataTable.getColumn("Hành động").setCellRenderer(new ActionButtonRenderer());
        dataTable.getColumn("Hành động").setCellEditor(new ActionButtonEditor());
        
        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.setBorder(BorderFactory.createLoweredBevelBorder());
        gridPanel.add(scrollPane, BorderLayout.CENTER);
        
        return gridPanel;
    }
    
    private JPanel createPaginationPanel() {
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        firstPageButton = new JButton("<<");
        prevPageButton = new JButton("<");
        nextPageButton = new JButton(">");
        lastPageButton = new JButton(">>");
        
        currentPageLabel = new JLabel("1");
        
        firstPageButton.setPreferredSize(new Dimension(40, 30));
        prevPageButton.setPreferredSize(new Dimension(40, 30));
        nextPageButton.setPreferredSize(new Dimension(40, 30));
        lastPageButton.setPreferredSize(new Dimension(40, 30));
        
        paginationPanel.add(firstPageButton);
        paginationPanel.add(prevPageButton);
        
        // Add page numbers
        for (int i = 1; i <= Math.min(5, totalPages); i++) {
            JButton pageButton = new JButton(String.valueOf(i));
            pageButton.setPreferredSize(new Dimension(40, 30));
            if (i == currentPage) {
                pageButton.setBackground(new Color(51, 135, 204));
                pageButton.setForeground(Color.WHITE);
            }
            final int pageNum = i;
            pageButton.addActionListener(e -> goToPage(pageNum));
            paginationPanel.add(pageButton);
        }
        
        if (totalPages > 5) {
            paginationPanel.add(new JLabel("..."));
            JButton lastPageNumButton = new JButton(String.valueOf(totalPages));
            lastPageNumButton.setPreferredSize(new Dimension(40, 30));
            final int lastPage = totalPages;
            lastPageNumButton.addActionListener(e -> goToPage(lastPage));
            paginationPanel.add(lastPageNumButton);
        }
        
        paginationPanel.add(nextPageButton);
        paginationPanel.add(lastPageButton);
        
        // Add navigation listeners
        firstPageButton.addActionListener(e -> goToPage(1));
        prevPageButton.addActionListener(e -> goToPage(Math.max(1, currentPage - 1)));
        nextPageButton.addActionListener(e -> goToPage(Math.min(totalPages, currentPage + 1)));
        lastPageButton.addActionListener(e -> goToPage(totalPages));
        
        return paginationPanel;
    }
    
    private void loadFakeData() {
        String[][] fakeData = {
            {"1", "BN-00123", "Nguyễn Văn An", "28/07/2025", "BS. Thiên Phúc", "Viêm họng", ""},
            {"2", "BN-00122", "Trần Thị Bình", "27/07/2025", "BS. Minh Anh", "Sốt siêu vi", ""},
            {"3", "BN-00121", "Lê Hoàng Cường", "27/07/2025", "BS. Thiên Phúc", "Đau dạ dày", ""},
            {"4", "BN-00120", "Phạm Thị Dung", "26/07/2025", "BS. Minh Anh", "Khám tổng quát", ""},
            {"5", "BN-00119", "Hoàng Văn Em", "26/07/2025", "BS. Thiên Phúc", "Viêm phế quản", ""},
            {"6", "BN-00118", "Ngô Thị Phương", "25/07/2025", "BS. Hoàng Nam", "Đau đầu mãn tính", ""},
            {"7", "BN-00117", "Võ Minh Quang", "25/07/2025", "BS. Minh Anh", "Tăng huyết áp", ""},
            {"8", "BN-00116", "Đặng Thị Hoa", "24/07/2025", "BS. Thiên Phúc", "Tiểu đường type 2", ""},
            {"9", "BN-00115", "Bùi Văn Inh", "24/07/2025", "BS. Thị Lan", "Viêm gan B", ""},
            {"10", "BN-00114", "Lý Thị Kim", "23/07/2025", "BS. Minh Anh", "Khám thai 20 tuần", ""},
            {"11", "BN-00113", "Trương Văn Long", "23/07/2025", "BS. Thiên Phúc", "Viêm dạ dày", ""},
            {"12", "BN-00112", "Phan Thị Mai", "22/07/2025", "BS. Hoàng Nam", "Rối loạn lipid máu", ""},
            {"13", "BN-00111", "Nguyễn Minh Nam", "22/07/2025", "BS. Thị Lan", "Viêm khớp", ""},
            {"14", "BN-00110", "Cao Thị Oanh", "21/07/2025", "BS. Minh Anh", "Bệnh tim mạch", ""},
            {"15", "BN-00109", "Đinh Văn Phúc", "21/07/2025", "BS. Thiên Phúc", "Hen suyễn", ""},
            {"16", "BN-00108", "Lưu Thị Quỳnh", "20/07/2025", "BS. Hoàng Nam", "Viêm amidan", ""},
            {"17", "BN-00107", "Tô Văn Rạng", "20/07/2025", "BS. Thị Lan", "Đau lưng mãn tính", ""},
            {"18", "BN-00106", "Huỳnh Thị Sang", "19/07/2025", "BS. Minh Anh", "Viêm ruột thừa", ""},
            {"19", "BN-00105", "Vũ Văn Tâm", "19/07/2025", "BS. Thiên Phúc", "Suy thận mãn", ""},
            {"20", "BN-00104", "Phạm Gia Hân", "22/07/2025", "BS. Minh Anh", "Khám tổng quát", ""}
        };
        
        for (String[] row : fakeData) {
            tableModel.addRow(row);
        }
    }
    
    private void applyFilters() {
        JOptionPane.showMessageDialog(this, "Chức năng lọc dữ liệu đang được phát triển", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void clearFilters() {
        searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
        searchField.setForeground(Color.GRAY);
        doctorComboBox.setSelectedIndex(0);
        fromDateSpinner.setValue(new Date());
        toDateSpinner.setValue(new Date());
    }
    
    private void goToPage(int page) {
        currentPage = page;
        // Update result count label
        int startRecord = (currentPage - 1) * recordsPerPage + 1;
        int endRecord = Math.min(currentPage * recordsPerPage, totalRecords);
        resultCountLabel.setText(String.format("Hiển thị %d đến %d của %d kết quả", startRecord, endRecord, totalRecords));
        
        // Update pagination buttons
        Component parent = firstPageButton.getParent();
        parent.removeAll();
        parent.add(createPaginationPanel());
        parent.revalidate();
        parent.repaint();
    }
    
    // Custom renderer for action buttons
    class ActionButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton viewButton, editButton, deleteButton;
        
        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));
            viewButton = new JButton("👁️");
            editButton = new JButton("✏️");
            deleteButton = new JButton("🗑️");
            
            viewButton.setPreferredSize(new Dimension(30, 25));
            editButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setPreferredSize(new Dimension(30, 25));
            
            viewButton.setToolTipText("Xem");
            editButton.setToolTipText("Sửa");
            deleteButton.setToolTipText("Xóa");
            
            add(viewButton);
            add(editButton);
            add(deleteButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }
    
    // Custom editor for action buttons
    class ActionButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton viewButton, editButton, deleteButton;
        
        public ActionButtonEditor() {
            super(new JCheckBox());
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
            
            viewButton = new JButton("👁️");
            editButton = new JButton("✏️");
            deleteButton = new JButton("🗑️");
            
            viewButton.setPreferredSize(new Dimension(30, 25));
            editButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setPreferredSize(new Dimension(30, 25));
            
            viewButton.addActionListener(e -> {
                int row = dataTable.getSelectedRow();
                String patientId = (String) tableModel.getValueAt(row, 1);
                JOptionPane.showMessageDialog(DataDialog.this, "Xem thông tin bệnh nhân: " + patientId, "Xem", JOptionPane.INFORMATION_MESSAGE);
                fireEditingStopped();
            });
            
            editButton.addActionListener(e -> {
                int row = dataTable.getSelectedRow();
                String patientId = (String) tableModel.getValueAt(row, 1);
                JOptionPane.showMessageDialog(DataDialog.this, "Chỉnh sửa thông tin bệnh nhân: " + patientId, "Sửa", JOptionPane.INFORMATION_MESSAGE);
                fireEditingStopped();
            });
            
            deleteButton.addActionListener(e -> {
                int row = dataTable.getSelectedRow();
                String patientId = (String) tableModel.getValueAt(row, 1);
                int result = JOptionPane.showConfirmDialog(DataDialog.this, 
                    "Bạn có chắc chắn muốn xóa phiếu khám của bệnh nhân " + patientId + " không?", 
                    "Xác nhận xóa", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    tableModel.removeRow(row);
                    JOptionPane.showMessageDialog(DataDialog.this, "Đã xóa phiếu khám thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
                fireEditingStopped();
            });
            
            panel.add(viewButton);
            panel.add(editButton);
            panel.add(deleteButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }
}
