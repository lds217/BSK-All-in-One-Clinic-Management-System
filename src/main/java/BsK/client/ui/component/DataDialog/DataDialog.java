package BsK.client.ui.component.DataDialog;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.common.AddDialog.AddDialog;
import BsK.common.entity.DoctorItem;
import BsK.common.entity.Patient;
import BsK.common.packet.req.DeleteCheckupRequest;
import BsK.common.packet.req.GetCheckupDataRequest;
import BsK.common.packet.res.DeleteCheckupResponse;
import BsK.common.packet.res.GetCheckupDataResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.client.ui.component.CheckUpPage.CheckUpPage;
import BsK.common.entity.CheckupData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataDialog extends JDialog {

    //<editor-fold desc="Properties for Checkup Data Tab">
    private JTextField searchField;
    private JTextField idSearchField;
    private JComboBox<String> doctorComboBox;
    private JSpinner fromDateSpinner;
    private JSpinner toDateSpinner;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JLabel resultCountLabel;
    private final ResponseListener<GetCheckupDataResponse> dataResponseListener = this::handleGetCheckupDataResponse;
    private final ResponseListener<DeleteCheckupResponse> deleteResponseListener = this::handleDeleteCheckupResponse; // --- NEW ---
    private int currentPage = 1;
    private int totalPages = 1;
    private int recordsPerPage = 20;
    private int totalRecords = 0;
    private boolean isExporting = false;
    private File fileForExport = null;
    private JPanel mainPanel;
    private JPanel paginationPanel;
    private CheckUpPage checkUpPageInstance;
    private String[][] currentCheckupData;
    //</editor-fold>

    // --- Panels for the other tabs
    private MedicineManagementPanel medicinePanel;
    private ServiceManagementPanel servicePanel;
    private DoctorManagementPanel doctorPanel;
    private UserManagementPanel userPanel;

    public DataDialog(JFrame parent, CheckUpPage checkUpPage) {
        super(parent, "Quản Lý Dữ Liệu", true);
        this.checkUpPageInstance = checkUpPage;

        initializeDialog();
        setupNetworking();

        // Initial data fetch for checkup data tab
        fetchData(1);
    }

    private void initializeDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        // --- Tab 1: Patient Checkup Data (Existing UI) ---
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(createControlPanel(), BorderLayout.NORTH);
        mainPanel.add(createDataGridPanel(), BorderLayout.CENTER);
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        mainPanel.add(paginationPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("<html><body style='padding: 5px 10px;'>Dữ Liệu Khám Bệnh</body></html>", mainPanel);

        // --- Tab 2: Medicine Management (Instantiate new panel) ---
        medicinePanel = new MedicineManagementPanel();
        tabbedPane.addTab("<html><body style='padding: 5px 10px;'>Quản Lý Thuốc</body></html>", medicinePanel);

        // --- Tab 3: Service Management ---
        servicePanel = new ServiceManagementPanel();
        tabbedPane.addTab("<html><body style='padding: 5px 10px;'>Quản Lý Dịch Vụ</body></html>", servicePanel);

        // --- Tab 4: User Management ---
        userPanel = new UserManagementPanel();
        tabbedPane.addTab("<html><body style='padding: 5px 10px;'>Quản Lý Người Dùng</body></html>", userPanel);

        // --- Tab 5: Doctor Management ---
        doctorPanel = new DoctorManagementPanel();
        tabbedPane.addTab("<html><body style='padding: 5px 10px;'>Quản Lý Bác Sĩ</body></html>", doctorPanel);

        add(tabbedPane, BorderLayout.CENTER);

        loadFiltersFromLocalStorage();
    }

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetCheckupDataResponse.class, dataResponseListener);
        ClientHandler.addResponseListener(DeleteCheckupResponse.class, deleteResponseListener); // --- NEW ---

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Clean up all listeners
                ClientHandler.deleteListener(GetCheckupDataResponse.class, dataResponseListener);
                ClientHandler.deleteListener(DeleteCheckupResponse.class, deleteResponseListener); // --- NEW ---
                if (medicinePanel != null) {
                    medicinePanel.cleanup();
                }
                if (servicePanel != null) {
                    servicePanel.cleanup();
                }
                if (userPanel != null) {
                    userPanel.cleanup();
                }
                if (doctorPanel != null) {
                    doctorPanel.cleanup();
                }
                super.windowClosing(e);
            }
        });
    }

    //<editor-fold desc="Checkup Data Tab Methods">
    private void loadFiltersFromLocalStorage() {
        if (LocalStorage.dataDialogSearchTerm == null || LocalStorage.dataDialogSearchTerm.isEmpty()) {
            searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
            searchField.setForeground(Color.GRAY);
        } else {
            searchField.setText(LocalStorage.dataDialogSearchTerm);
            searchField.setForeground(Color.BLACK);
        }

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

        if (LocalStorage.dataDialogDoctorName != null) {
            doctorComboBox.setSelectedItem(LocalStorage.dataDialogDoctorName);
        } else {
            doctorComboBox.setSelectedIndex(0);
        }

        if (LocalStorage.dataDialogIdSearchTerm != null && !LocalStorage.dataDialogIdSearchTerm.isEmpty()) {
            idSearchField.setText(LocalStorage.dataDialogIdSearchTerm);
            idSearchField.setForeground(Color.BLACK);
        } else {
            idSearchField.setText("Tìm theo mã phiếu khám...");
            idSearchField.setForeground(Color.GRAY);
        }
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

        JButton getAllButton = new JButton("Lấy tất cả");
        getAllButton.setPreferredSize(new Dimension(100, 30));

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
        topRow.add(getAllButton);
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

        doctorComboBox = new JComboBox<>();
        doctorComboBox.addItem("Tất cả");
        if (LocalStorage.doctorsName != null) {
            for (DoctorItem doctor : LocalStorage.doctorsName) {
                doctorComboBox.addItem(doctor.getName());
            }
        }
        doctorComboBox.setSelectedIndex(0);
        doctorComboBox.setPreferredSize(new Dimension(150, 25));
        bottomRow.add(doctorComboBox);

        bottomRow.add(Box.createHorizontalStrut(15));
        bottomRow.add(new JLabel("Mã Phiếu:"));
        idSearchField = new JTextField("Tìm theo mã phiếu khám...", 12);
        idSearchField.setForeground(Color.GRAY);
        idSearchField.setPreferredSize(new Dimension(150, 25));
        idSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (idSearchField.getText().equals("Tìm theo mã phiếu khám...")) {
                    idSearchField.setText("");
                    idSearchField.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (idSearchField.getText().isEmpty()) {
                    idSearchField.setText("Tìm theo mã phiếu khám...");
                    idSearchField.setForeground(Color.GRAY);
                }
            }
        });
        bottomRow.add(idSearchField);

        controlPanel.add(topRow, BorderLayout.NORTH);
        controlPanel.add(bottomRow, BorderLayout.SOUTH);

        addNewButton.addActionListener(e -> {
            AddDialog addDialog = new AddDialog((Frame) getParent());
            addDialog.setVisible(true);
        });
        exportExcelButton.addActionListener(e -> handleExportToExcel());

        filterButton.addActionListener(e -> {
            saveFiltersToLocalStorage();
            fetchData(1);
        });
        clearFilterButton.addActionListener(e -> {
            clearFilters();
            fetchData(1);
        });

        getAllButton.addActionListener(e -> {
            GetCheckupDataRequest request = new GetCheckupDataRequest(null, null,null, null, null, 1, recordsPerPage, LocalStorage.currentShift);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
            resultCountLabel.setText("Đang tải tất cả dữ liệu...");
        });

        return controlPanel;
    }

    private JPanel createDataGridPanel() {
        JPanel gridPanel = new JPanel(new BorderLayout());
        resultCountLabel = new JLabel("Đang tải dữ liệu...");
        resultCountLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        resultCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gridPanel.add(resultCountLabel, BorderLayout.NORTH);

        String[] columnNames = {"STT", "Mã khám", "Họ và Tên", "Năm sinh", "Giới tính", "Ngày khám", "Bác sĩ khám", "Chẩn đoán", "Kết luận", "Hành động"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 9; // Only the "Hành động" column is editable
            }
        };
        dataTable = new JTable(tableModel);
        dataTable.setRowHeight(35);
        dataTable.setFont(new Font("Arial", Font.PLAIN, 12));
        dataTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        dataTable.getTableHeader().setBackground(new Color(240, 240, 240));
        dataTable.setSelectionBackground(new Color(230, 240, 255));

        TableColumn column;
        int[] columnWidths = {40, 80, 150, 80, 60, 100, 130, 150, 150, 120}; // Adjusted widths
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

        String checkupIdSearch = idSearchField.getText();
        if (checkupIdSearch == null || checkupIdSearch.equals("Tìm theo mã phiếu khám...") || checkupIdSearch.trim().isEmpty()) {
            checkupIdSearch = null;
        }

        if (checkupIdSearch != null) {
            searchTerm = null;
            searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
            searchField.setForeground(Color.GRAY);
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

        if (selectedDoctor != null && !selectedDoctor.equals("Tất cả")) {
            if (LocalStorage.doctorsName != null) {
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
        }

        GetCheckupDataRequest request = new GetCheckupDataRequest(searchTerm, checkupIdSearch, fromTimestamp, toTimestamp, doctorId, page, recordsPerPage, LocalStorage.currentShift);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

        resultCountLabel.setText("Đang tải dữ liệu cho trang " + page + "...");
    }

    private void handleGetCheckupDataResponse(GetCheckupDataResponse response) {
        if (isExporting) {
            List<Patient> patientsToExport = new ArrayList<>();
            if (response.getCheckupData() != null) {
                for (String[] rowData : response.getCheckupData()) {
                    patientsToExport.add(new Patient(rowData));
                }
            }

            try {
                ExcelExporter.exportToExcel(patientsToExport, LocalStorage.doctorsName, this.fileForExport);
                JOptionPane.showMessageDialog(this, "Xuất file Excel thành công!\n" + this.fileForExport.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi ghi file Excel: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } finally {
                isExporting = false;
                fileForExport = null;
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            this.currentPage = response.getCurrentPage();
            this.totalPages = response.getTotalPages();
            this.totalRecords = response.getTotalRecords();
            this.recordsPerPage = response.getPageSize();
            this.currentCheckupData = response.getCheckupData();

            tableModel.setRowCount(0);
            if (this.currentCheckupData != null) {
                int stt = (currentPage - 1) * recordsPerPage + 1;
                for (String[] rowData : this.currentCheckupData) {
                    try {
                        Patient patient = new Patient(rowData);
                        String[] tableRow = {
                                String.valueOf(stt++),
                                patient.getCheckupId(), // Changed from getCustomerId() to getCheckupId()
                                patient.getCustomerLastName() + " " + patient.getCustomerFirstName(),
                                patient.getCustomerDob(),
                                patient.getCustomerGender(),
                                patient.getCheckupDate(),
                                patient.getDoctorName(),
                                patient.getDiagnosis(),
                                patient.getConclusion(),
                                "" // Placeholder for action buttons
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
    
    /**
     * --- NEW ---
     * Handles the response from the server after a delete request.
     * @param response The response packet from the server.
     */
    private void handleDeleteCheckupResponse(DeleteCheckupResponse response) {
        SwingUtilities.invokeLater(() -> {
            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(this, "Xóa phiếu khám thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                // Refresh the table to show the change.
                fetchData(currentPage);
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa phiếu khám: " + response.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
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

    private void handleExportToExcel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu file Excel");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new File("DanhSachKhamBenh.xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.fileForExport = fileChooser.getSelectedFile();
            if (!this.fileForExport.getName().toLowerCase().endsWith(".xlsx")) {
                this.fileForExport = new File(this.fileForExport.getParentFile(), this.fileForExport.getName() + ".xlsx");
            }

            this.isExporting = true;
            JOptionPane.showMessageDialog(this, "Đang chuẩn bị dữ liệu để xuất file...", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            fetchData(-1); // Fetch all data for export
        }
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

    private void saveFiltersToLocalStorage() {
        String searchTerm = searchField.getText();
        LocalStorage.dataDialogSearchTerm = searchTerm.equals("Tìm theo tên bệnh nhân, mã bệnh nhân...") ? "" : searchTerm;

        String idSearchTerm = idSearchField.getText();
        LocalStorage.dataDialogIdSearchTerm = idSearchTerm.equals("Tìm theo mã phiếu khám...") ? "" : idSearchTerm;

        LocalStorage.dataDialogFromDate = (Date) fromDateSpinner.getValue();
        LocalStorage.dataDialogToDate = (Date) toDateSpinner.getValue();
        LocalStorage.dataDialogDoctorName = (String) doctorComboBox.getSelectedItem();
    }

    private void clearFilters() {
        searchField.setText("Tìm theo tên bệnh nhân, mã bệnh nhân...");
        searchField.setForeground(Color.GRAY);
        idSearchField.setText("Tìm theo mã phiếu khám...");
        idSearchField.setForeground(Color.GRAY);
        doctorComboBox.setSelectedIndex(0);

        Date today = new Date();
        fromDateSpinner.setValue(today);
        toDateSpinner.setValue(today);

        LocalStorage.dataDialogSearchTerm = "";
        LocalStorage.dataDialogIdSearchTerm = "";
        LocalStorage.dataDialogFromDate = today;
        LocalStorage.dataDialogToDate = today;
        LocalStorage.dataDialogDoctorName = "Tất cả";
    }

    //</editor-fold>

    //<editor-fold desc="Action Button Inner Classes">

    /**
     * --- MODIFIED ---
     * Renders both an Edit and a Delete button in the "Hành động" column.
     */
    class ActionButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton editButton;
        private final JButton deleteButton;

        public ActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));

            // Edit Button
            ImageIcon editIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/edit.png");
            editIcon.setImage(editIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            editButton = new JButton(editIcon);
            editButton.setPreferredSize(new Dimension(30, 25));
            editButton.setToolTipText("Sửa");

            // Delete Button
            ImageIcon deleteIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/delete.png");
            deleteIcon.setImage(deleteIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            deleteButton = new JButton(deleteIcon);
            deleteButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setToolTipText("Xóa");

            add(editButton);
            add(deleteButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    /**
     * --- MODIFIED ---
     * Cell editor for the "Hành động" column, handling clicks for both Edit and Delete buttons.
     */
    class ActionButtonEditor extends DefaultCellEditor {
        private final JPanel panel;
        private final JButton editButton;
        private final JButton deleteButton;

        public ActionButtonEditor(JDialog parentDialog) {
            super(new JCheckBox());
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));

            // Edit Button Setup
            ImageIcon editIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/edit.png");
            editIcon.setImage(editIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            editButton = new JButton(editIcon);
            editButton.setPreferredSize(new Dimension(30, 25));
            editButton.setToolTipText("Sửa");

            // Delete Button Setup
            ImageIcon deleteIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/delete.png");
            deleteIcon.setImage(deleteIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            deleteButton = new JButton(deleteIcon);
            deleteButton.setPreferredSize(new Dimension(30, 25));
            deleteButton.setToolTipText("Xóa");

            // Action Listener for Edit Button
            editButton.addActionListener(e -> {
                fireEditingStopped();
                int selectedViewRow = dataTable.getSelectedRow();
                if (selectedViewRow < 0) return;
                int modelRow = dataTable.convertRowIndexToModel(selectedViewRow);

                if (currentCheckupData != null && modelRow >= 0 && modelRow < currentCheckupData.length) {
                    String[] rowData = currentCheckupData[modelRow];
                    try {
                        Patient selectedPatient = new Patient(rowData);
                        if (checkUpPageInstance != null) {
                            checkUpPageInstance.loadPatientByCheckupId(selectedPatient);
                        }
                        parentDialog.dispose();
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(parentDialog, "Không thể tải dữ liệu bệnh nhân. Dữ liệu không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            // Action Listener for Delete Button
            deleteButton.addActionListener(e -> {
                fireEditingStopped();
                int selectedViewRow = dataTable.getSelectedRow();
                if (selectedViewRow < 0) return;
                int modelRow = dataTable.convertRowIndexToModel(selectedViewRow);

                String checkupIdStr = (String) tableModel.getValueAt(modelRow, 1);
                String patientName = (String) tableModel.getValueAt(modelRow, 2);

                int confirmation = JOptionPane.showConfirmDialog(
                        parentDialog,
                        "Bạn có chắc chắn muốn xóa phiếu khám của bệnh nhân:\n" + patientName + " (Mã khám: " + checkupIdStr + ")?\n\nHành động này sẽ xóa toàn bộ dữ liệu liên quan và không thể hoàn tác.",
                        "Xác nhận xóa",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (confirmation == JOptionPane.YES_OPTION) {
                    try {
                        long checkupId = Long.parseLong(checkupIdStr);
                        DeleteCheckupRequest deleteRequest = new DeleteCheckupRequest(checkupId, LocalStorage.currentShift);
                        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), deleteRequest);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(parentDialog, "Mã khám không hợp lệ: " + checkupIdStr, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
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
    //</editor-fold>
}
