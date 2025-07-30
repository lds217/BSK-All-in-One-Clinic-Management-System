package BsK.client.ui.component.DataDialog;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.common.AddDialog.AddDialog;
import BsK.common.entity.DoctorItem;
import BsK.common.entity.Patient;
import BsK.common.packet.req.GetCheckupDataRequest;
import BsK.common.packet.res.GetCheckupDataResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.client.ui.component.CheckUpPage.CheckUpPage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Date;

public class DataDialog extends JDialog {

    private JTextField searchField;
    private JComboBox<String> doctorComboBox;
    private JSpinner fromDateSpinner;
    private JSpinner toDateSpinner;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JLabel resultCountLabel;

    private final ResponseListener<GetCheckupDataResponse> dataResponseListener = this::handleGetCheckupDataResponse;

    private int currentPage = 1;
    private int totalPages = 1;
    private int recordsPerPage = 20;
    private int totalRecords = 0;

    private JPanel mainPanel;
    private JPanel paginationPanel;
    private CheckUpPage checkUpPageInstance; // <-- ADD THIS FIELD

    // --- MODIFIED CONSTRUCTOR ---
    public DataDialog(JFrame parent, CheckUpPage checkUpPage) {
        super(parent, "Quản Lý Dữ Liệu Khám Bệnh", true);
        this.checkUpPageInstance = checkUpPage; // Store the instance

        initializeDialog();
        setupNetworking();
        fetchData(1);
    }

    private void initializeDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(createDataGridPanel(), BorderLayout.CENTER);

        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                mainPanel.add(paginationPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        loadFiltersFromLocalStorage(); // <-- CALL THE NEW METHOD HERE
    }
    
    // --- ADD THIS NEW METHOD ---
    private void loadFiltersFromLocalStorage() {
        // Load search term, handling placeholder text
        if (LocalStorage.dataDialogSearchTerm == null || LocalStorage.dataDialogSearchTerm.isEmpty()) {
            searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
            searchField.setForeground(Color.GRAY);
        } else {
            searchField.setText(LocalStorage.dataDialogSearchTerm);
            searchField.setForeground(Color.BLACK);
        }

        // Load dates, providing a default if null
        if (LocalStorage.dataDialogFromDate != null) {
            fromDateSpinner.setValue(LocalStorage.dataDialogFromDate);
        } else {
            fromDateSpinner.setValue(new Date());
        }

        if (LocalStorage.dataDialogToDate != null) {
            toDateSpinner.setValue(LocalStorage.dataDialogToDate);
        } else {
            toDateSpinner.setValue(new Date());
        }
        
        // Load doctor selection
        if (LocalStorage.dataDialogDoctorName != null) {
            doctorComboBox.setSelectedItem(LocalStorage.dataDialogDoctorName);
        } else {
            doctorComboBox.setSelectedIndex(0); // Default to "Tất cả"
        }
    }

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetCheckupDataResponse.class, dataResponseListener);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // --- MODIFIED: Changed to deleteListener as requested ---
                ClientHandler.deleteListener(GetCheckupDataResponse.class, dataResponseListener);
                super.windowClosing(e);
            }
        });
    }



    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Tìm kiếm và Lọc dữ liệu"));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
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
        
        // --- MODIFIED: Use DoctorItem from LocalStorage ---
        doctorComboBox = new JComboBox<>();
        // Add "Tất cả" option first
        doctorComboBox.addItem("Tất cả");
        // Add doctors from LocalStorage
        for (DoctorItem doctor : LocalStorage.doctorsName) {
            doctorComboBox.addItem(doctor.getName());
        }
        doctorComboBox.setSelectedIndex(0); // Default to "Tất cả"
        doctorComboBox.setPreferredSize(new Dimension(150, 25));
        bottomRow.add(doctorComboBox);

        controlPanel.add(topRow, BorderLayout.NORTH);
        controlPanel.add(bottomRow, BorderLayout.SOUTH);

        addNewButton.addActionListener(e -> {
            AddDialog addDialog = new AddDialog((Frame) getParent());
            addDialog.setVisible(true);
        });
        exportExcelButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Chức năng xuất Excel đang được phát triển", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        
        filterButton.addActionListener(e -> {
            saveFiltersToLocalStorage(); // <-- SAVE STATE BEFORE FETCHING
            fetchData(1);
        });
        clearFilterButton.addActionListener(e -> {
            clearFilters();
            fetchData(1);
        });

        return controlPanel;
    }

    private JPanel createDataGridPanel() {
        JPanel gridPanel = new JPanel(new BorderLayout());
        resultCountLabel = new JLabel("Đang tải dữ liệu...");
        resultCountLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        resultCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gridPanel.add(resultCountLabel, BorderLayout.NORTH);

        // --- MODIFIED: Added more columns for better data visibility ---
        String[] columnNames = {"STT", "Mã BN", "Họ và Tên", "Năm sinh", "Giới tính", "Ngày khám", "Bác sĩ khám", "Chẩn đoán", "Kết luận", "Hành động"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 9; // Action column is now at index 9
            }
        };
        dataTable = new JTable(tableModel);
        dataTable.setRowHeight(35);
        dataTable.setFont(new Font("Arial", Font.PLAIN, 12));
        dataTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        dataTable.getTableHeader().setBackground(new Color(240, 240, 240));
        dataTable.setSelectionBackground(new Color(230, 240, 255));
        
        // --- MODIFIED: Adjusted column widths for the new layout ---
        TableColumn column;
        int[] columnWidths = {40, 80, 150, 80, 60, 100, 130, 150, 200, 120};
        for (int i = 0; i < columnWidths.length; i++) {
            column = dataTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(columnWidths[i]);
        }

        dataTable.getColumn("Hành động").setCellRenderer(new ActionButtonRenderer());
        dataTable.getColumn("Hành động").setCellEditor(new ActionButtonEditor(this));
        
        JScrollPane scrollPane = new JScrollPane(dataTable);
        gridPanel.add(scrollPane, BorderLayout.CENTER);
        return gridPanel;
    }

    private void fetchData(int page) {
        String searchTerm = searchField.getText();
        if (searchTerm.equals("Tìm theo tên bệnh nhân, mã bệnh nhân...")) {
            searchTerm = null;
        }

        Date fromDate = (Date) fromDateSpinner.getValue();
        Date toDate = (Date) toDateSpinner.getValue();

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long fromTimestamp = cal.getTimeInMillis();

        cal.setTime(toDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long toTimestamp = cal.getTimeInMillis();

        String selectedDoctor = (String) doctorComboBox.getSelectedItem();
        Integer doctorId = null;
        
        // --- MODIFIED: Get correct doctor ID from LocalStorage ---
        if (selectedDoctor != null && !selectedDoctor.equals("Tất cả")) {
            for (DoctorItem doctor : LocalStorage.doctorsName) {
                if (doctor.getName().equals(selectedDoctor)) {
                    try {
                        doctorId = Integer.parseInt(doctor.getId());
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid doctor ID format: " + doctor.getId());
                    }
                    break;
                }
            }
        }

        GetCheckupDataRequest request = new GetCheckupDataRequest(searchTerm, fromTimestamp, toTimestamp, doctorId, page, recordsPerPage);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
        
        resultCountLabel.setText("Đang tải dữ liệu cho trang " + page + "...");
    }

    // --- MODIFIED: Process raw data into Patient DTOs and populate table correctly ---
    private void handleGetCheckupDataResponse(GetCheckupDataResponse response) {
        SwingUtilities.invokeLater(() -> {
            this.currentPage = response.getCurrentPage();
            this.totalPages = response.getTotalPages();
            this.totalRecords = response.getTotalRecords();
            this.recordsPerPage = response.getPageSize();

            tableModel.setRowCount(0);
            if (response.getCheckupData() != null) {
                int stt = (currentPage - 1) * recordsPerPage + 1;
                for (String[] rowData : response.getCheckupData()) {
                    try {
                        Patient patient = new Patient(rowData);
                        String[] tableRow = {
                            String.valueOf(stt++),
                            patient.getCustomerId(), // Correctly use customerId for "Mã BN"
                            patient.getCustomerLastName() + " " + patient.getCustomerFirstName(),
                            patient.getCustomerDob(),
                            patient.getCustomerGender(),
                            patient.getCheckupDate(),
                            patient.getDoctorName(),
                            patient.getDiagnosis(),
                            patient.getConclusion(),
                            "" // Action column placeholder
                        };
                        tableModel.addRow(tableRow);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Skipping row due to invalid data: " + e.getMessage());
                    }
                }
            }
            
            updateResultCountLabel();
            updatePaginationControls();
        });
    }

    private void updateResultCountLabel() {
        if (totalRecords == 0) {
            resultCountLabel.setText("Không tìm thấy kết quả nào.");
            return;
        }
        int startRecord = (currentPage - 1) * recordsPerPage + 1;
        int endRecord = Math.min(startRecord + recordsPerPage - 1, totalRecords);
        resultCountLabel.setText(String.format("Hiển thị %d đến %d của %d kết quả", startRecord, endRecord, totalRecords));
    }

    private void updatePaginationControls() {
        paginationPanel.removeAll();
        
        JButton firstPageButton = new JButton("<<");
        firstPageButton.addActionListener(e -> fetchData(1));
        firstPageButton.setEnabled(currentPage > 1);

        JButton prevPageButton = new JButton("<");
        prevPageButton.addActionListener(e -> fetchData(currentPage - 1));
        prevPageButton.setEnabled(currentPage > 1);
        
        paginationPanel.add(firstPageButton);
        paginationPanel.add(prevPageButton);

        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        if (startPage > 1) {
            paginationPanel.add(new JLabel("..."));
        }
        
        for (int i = startPage; i <= endPage; i++) {
            JButton pageButton = new JButton(String.valueOf(i));
            if (i == currentPage) {
                pageButton.setBackground(new Color(51, 135, 204));
                pageButton.setForeground(Color.WHITE);
            }
            final int pageNum = i;
            pageButton.addActionListener(e -> fetchData(pageNum));
            paginationPanel.add(pageButton);
        }

        if (endPage < totalPages) {
             paginationPanel.add(new JLabel("..."));
        }
        
        JButton nextPageButton = new JButton(">");
        nextPageButton.addActionListener(e -> fetchData(currentPage + 1));
        nextPageButton.setEnabled(currentPage < totalPages);
        
        JButton lastPageButton = new JButton(">>");
        lastPageButton.addActionListener(e -> fetchData(totalPages));
        lastPageButton.setEnabled(currentPage < totalPages);
        
        paginationPanel.add(nextPageButton);
        paginationPanel.add(lastPageButton);

        paginationPanel.revalidate();
        paginationPanel.repaint();
    }
    
    // --- ADD THIS NEW METHOD TO THE DataDialog CLASS ---
    private void saveFiltersToLocalStorage() {
        String searchTerm = searchField.getText();
        // Don't save the placeholder text
        if (searchTerm.equals("Tìm theo tên bệnh nhân, mã bệnh nhân...")) {
            LocalStorage.dataDialogSearchTerm = "";
        } else {
            LocalStorage.dataDialogSearchTerm = searchTerm;
        }
        
        LocalStorage.dataDialogFromDate = (Date) fromDateSpinner.getValue();
        LocalStorage.dataDialogToDate = (Date) toDateSpinner.getValue();
        LocalStorage.dataDialogDoctorName = (String) doctorComboBox.getSelectedItem();
    }

    private void clearFilters() {
        // Reset UI components
        searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
        searchField.setForeground(Color.GRAY);
        doctorComboBox.setSelectedIndex(0); // Defaults to "Tất cả"
        
        Date today = new Date();
        fromDateSpinner.setValue(today);
        toDateSpinner.setValue(today);

        // --- ADD THIS PART TO RESET LOCALSTORAGE ---
        LocalStorage.dataDialogSearchTerm = "";
        LocalStorage.dataDialogFromDate = today;
        LocalStorage.dataDialogToDate = today;
        LocalStorage.dataDialogDoctorName = "Tất cả";
    }

    private ImageIcon createIcon(String path, int width, int height) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(new ImageIcon(imgURL).getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
        }
        System.err.println("Couldn't find file: " + path);
        return null;
    }
    
    class ActionButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton, deleteButton;

        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 2, 2));


            ImageIcon editIcon = createIcon("/BsK/client/ui/assets/icon/edit.png", 16, 16);
            editButton = new JButton(editIcon);
            ImageIcon deleteIcon = createIcon("/BsK/client/ui/assets/icon/delete.png", 16, 16);
            deleteButton = new JButton(deleteIcon);

            editButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setPreferredSize(new Dimension(30, 25));

            editButton.setToolTipText("Sửa");
            deleteButton.setToolTipText("Xóa");

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
    
    class ActionButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editButton, deleteButton;

        public ActionButtonEditor(JDialog parentDialog) {
            super(new JCheckBox());
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));


            ImageIcon editIcon = createIcon("/BsK/client/ui/assets/icon/edit.png", 16, 16);
            editButton = new JButton(editIcon);
            ImageIcon deleteIcon = createIcon("/BsK/client/ui/assets/icon/delete.png", 16, 16);
            deleteButton = new JButton(deleteIcon);

            editButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setPreferredSize(new Dimension(30, 25));

            editButton.addActionListener(e -> {
                int row = dataTable.convertRowIndexToModel(dataTable.getSelectedRow());
                String checkupId = (String) tableModel.getValueAt(row, 1);

                if (checkUpPageInstance != null) {
                    checkUpPageInstance.loadPatientByCheckupId(checkupId);
                }
                
                parentDialog.dispose();
                fireEditingStopped();
            });

            deleteButton.addActionListener(e -> {
                int row = dataTable.getSelectedRow();
                String patientId = (String) tableModel.getValueAt(row, 1);
                int result = JOptionPane.showConfirmDialog(parentDialog,
                        "Bạn có chắc chắn muốn xóa phiếu khám của bệnh nhân " + patientId + " không?",
                        "Xác nhận xóa",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    tableModel.removeRow(row);
                    JOptionPane.showMessageDialog(parentDialog, "Đã xóa phiếu khám thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                }
                fireEditingStopped();
            });

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
