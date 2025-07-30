package BsK.client.ui.component.DataDialog;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Medicine;
import BsK.common.packet.req.GetMedInfoRequest;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A JPanel dedicated to managing medicine inventory (CRUD operations).
 * It handles its own UI, data fetching, and event handling.
 */
public class MedicineManagementPanel extends JPanel {

    // --- UI Components
    private JTable medicineTable;
    private DefaultTableModel medicineTableModel;
    private JTextField medicineSearchField;

    // --- Input Fields
    private JTextField medicineNameField;
    private JTextField medicineCompanyField;
    private JTextArea medicineDescriptionField;
    private JTextField medicineQuantityField;
    private JComboBox<String> medicineUnitComboBox;
    private JTextField medicinePriceField;
    private JTextField totalField;
    private JSpinner morningSpinner, noonSpinner, eveningSpinner;
    private JTextArea noteField;

    // --- Data & State
    private List<Medicine> allMedicines = new ArrayList<>();
    private String selectedMedicineId = null;

    // --- Networking
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::handleGetMedInfoResponse;

    public MedicineManagementPanel() {
        super(new BorderLayout(10, 10));
        initComponents();
        setupNetworking();
    }

    private void initComponents() {
        this.setBorder(new EmptyBorder(15, 15, 15, 15));
        this.add(createMedicineInputPanel(), BorderLayout.WEST); // Input form
        this.add(createMedicineListPanel(), BorderLayout.CENTER); // Searchable table
    }

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetMedInfoResponse.class, getMedInfoResponseListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
    }

    /**
     * Creates the left panel with input fields, structured into sub-panels.
     */
    private JPanel createMedicineInputPanel() {
        // --- Main container for the left side ---
        JPanel mainInputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.insets = new Insets(0, 0, 10, 0); // Padding between sub-panels
        mainGbc.weightx = 1.0;

        // --- Define fonts ---
        Font titleFont = new Font("Arial", Font.BOLD, 14);
        Font labelFont = new Font("Arial", Font.BOLD, 13);
        Font textFont = new Font("Arial", Font.PLAIN, 13);
        Dimension textFieldSize = new Dimension(100, 30);

        // --- Sub-Panel 1: Medicine Info ---
        JPanel medicineInfoPanel = new JPanel(new GridBagLayout());
        medicineInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin thuốc",
                TitledBorder.LEADING, TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel nameLabel = new JLabel("Tên thuốc:");
        nameLabel.setFont(labelFont);
        medicineInfoPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        medicineNameField = new JTextField(20);
        medicineNameField.setFont(textFont);
        medicineNameField.setPreferredSize(textFieldSize);
        medicineInfoPanel.add(medicineNameField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel companyLabel = new JLabel("Công ty:");
        companyLabel.setFont(labelFont);
        medicineInfoPanel.add(companyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        medicineCompanyField = new JTextField(20);
        medicineCompanyField.setFont(textFont);
        medicineCompanyField.setPreferredSize(textFieldSize);
        medicineCompanyField.setEditable(false);
        medicineInfoPanel.add(medicineCompanyField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel descriptionLabel = new JLabel("Mô tả:");
        descriptionLabel.setFont(labelFont);
        medicineInfoPanel.add(descriptionLabel, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        medicineDescriptionField = new JTextArea(3, 20);
        medicineDescriptionField.setFont(textFont);
        medicineDescriptionField.setLineWrap(true);
        medicineDescriptionField.setWrapStyleWord(true);
        medicineDescriptionField.setEditable(false);
        JScrollPane descriptionScrollPane = new JScrollPane(medicineDescriptionField);
        descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        medicineInfoPanel.add(descriptionScrollPane, gbc);
        gbc.weightx = 0.0;

        mainGbc.gridx = 0; mainGbc.gridy = 0;
        mainInputPanel.add(medicineInfoPanel, mainGbc);

        // --- Sub-Panel 2: Quantity & Price ---
        JPanel quantityPricePanel = new JPanel(new GridBagLayout());
        quantityPricePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Số lượng & Giá",
                TitledBorder.LEADING, TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));

        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel quantityLabel = new JLabel("Số lượng:");
        quantityLabel.setFont(labelFont);
        quantityPricePanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setFont(textFont);
        quantitySpinner.setPreferredSize(new Dimension(80, 30));
        quantityPricePanel.add(quantitySpinner, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JLabel quantityLeftLabel = new JLabel("Còn lại:");
        quantityLeftLabel.setFont(labelFont);
        quantityPricePanel.add(quantityLeftLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        medicineQuantityField = new JTextField(5);
        medicineQuantityField.setFont(textFont);
        medicineQuantityField.setPreferredSize(textFieldSize);
        medicineQuantityField.setEditable(false);
        quantityPricePanel.add(medicineQuantityField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel unitLabel = new JLabel("ĐVT:");
        unitLabel.setFont(labelFont);
        quantityPricePanel.add(unitLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        String[] units = {"Viên", "Vỉ", "Hộp", "Chai", "Tuýp", "Gói"};
        medicineUnitComboBox = new JComboBox<>(units);
        medicineUnitComboBox.setFont(textFont);
        medicineUnitComboBox.setPreferredSize(textFieldSize);
        medicineUnitComboBox.setEnabled(false);
        quantityPricePanel.add(medicineUnitComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JLabel priceLabel = new JLabel("Giá (VNĐ):");
        priceLabel.setFont(labelFont);
        quantityPricePanel.add(priceLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        medicinePriceField = new JTextField(10);
        medicinePriceField.setFont(textFont);
        medicinePriceField.setPreferredSize(textFieldSize);
        medicinePriceField.setEditable(false);
        quantityPricePanel.add(medicinePriceField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel totalLabel = new JLabel("Thành tiền (VNĐ):");
        totalLabel.setFont(labelFont);
        quantityPricePanel.add(totalLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        totalField = new JTextField(10);
        totalField.setFont(textFont);
        totalField.setPreferredSize(textFieldSize);
        totalField.setEditable(false);
        quantityPricePanel.add(totalField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        mainGbc.gridx = 0; mainGbc.gridy = 1;
        mainInputPanel.add(quantityPricePanel, mainGbc);

        // --- Sub-Panel 3: Dosage ---
        JPanel dosagePanel = new JPanel(new GridBagLayout());
        dosagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Liều dùng",
                TitledBorder.LEADING, TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel morningLabel = new JLabel("Sáng:");
        morningLabel.setFont(labelFont);
        dosagePanel.add(morningLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        morningSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        morningSpinner.setFont(textFont);
        morningSpinner.setPreferredSize(new Dimension(60,30));
        dosagePanel.add(morningSpinner, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JLabel noonLabel = new JLabel("Trưa:");
        noonLabel.setFont(labelFont);
        dosagePanel.add(noonLabel, gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.3;
        noonSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        noonSpinner.setFont(textFont);
        noonSpinner.setPreferredSize(new Dimension(60,30));
        dosagePanel.add(noonSpinner, gbc);
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        JLabel eveningLabel = new JLabel("Chiều:");
        eveningLabel.setFont(labelFont);
        dosagePanel.add(eveningLabel, gbc);
        gbc.gridx = 5;
        gbc.weightx = 0.3;
        eveningSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        eveningSpinner.setFont(textFont);
        eveningSpinner.setPreferredSize(new Dimension(60,30));
        dosagePanel.add(eveningSpinner, gbc);
        gbc.weightx = 0.0;
        
        mainGbc.gridx = 0; mainGbc.gridy = 2;
        mainInputPanel.add(dosagePanel, mainGbc);

        // --- Sub-Panel 4: Note ---
        JPanel notePanel = new JPanel(new GridBagLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Ghi chú",
                TitledBorder.LEADING, TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        
        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel noteLabel = new JLabel("Ghi chú:");
        noteLabel.setFont(labelFont);
        notePanel.add(noteLabel, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        noteField = new JTextArea(3, 20);
        noteField.setFont(textFont);
        noteField.setLineWrap(true);
        noteField.setWrapStyleWord(true);
        JScrollPane noteScrollPane = new JScrollPane(noteField);
        noteScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        notePanel.add(noteScrollPane, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        mainGbc.gridx = 0; mainGbc.gridy = 3;
        mainInputPanel.add(notePanel, mainGbc);
        
        // --- Sub-Panel 5: Action Buttons ---
        mainGbc.gridx = 0; mainGbc.gridy = 4; mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainInputPanel.add(createMedicineButtonPanel(), mainGbc);

        // --- Filler Panel to push everything up ---
        mainGbc.gridx = 0; mainGbc.gridy = 5; mainGbc.weighty = 1.0;
        mainInputPanel.add(new JPanel(), mainGbc);

        return mainInputPanel;
    }


    private JPanel createMedicineButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton btnAdd = new JButton("Thêm mới");
        JButton btnEdit = new JButton("Chỉnh sửa");
        JButton btnDelete = new JButton("Xoá");
        JButton btnClear = new JButton("Làm mới");

        Dimension btnSize = new Dimension(100, 35);
        btnAdd.setPreferredSize(btnSize);
        btnEdit.setPreferredSize(btnSize);
        btnDelete.setPreferredSize(btnSize);
        btnClear.setPreferredSize(btnSize);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        // --- Button Actions ---
        btnClear.addActionListener(e -> clearMedicineFields());

        btnAdd.addActionListener(e -> {
            String name = medicineNameField.getText();
            if (name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên thuốc không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // TODO: Implement Add Medicine network request
            JOptionPane.showMessageDialog(this, "Chức năng 'Thêm mới' cho thuốc '" + name + "' sẽ được thực hiện tại đây.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        btnEdit.addActionListener(e -> {
            if (selectedMedicineId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một loại thuốc để chỉnh sửa.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // TODO: Implement Edit Medicine network request
            String name = medicineNameField.getText();
            JOptionPane.showMessageDialog(this, "Chức năng 'Chỉnh sửa' cho thuốc ID: " + selectedMedicineId + " (" + name + ") sẽ được thực hiện tại đây.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        btnDelete.addActionListener(e -> {
            if (selectedMedicineId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một loại thuốc để xóa.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn xóa thuốc '" + medicineNameField.getText() + "' không?",
                    "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                // TODO: Implement Delete Medicine network request
                JOptionPane.showMessageDialog(this, "Chức năng 'Xoá' cho thuốc ID: " + selectedMedicineId + " sẽ được thực hiện tại đây.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return buttonPanel;
    }

    private JPanel createMedicineListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Danh Sách Thuốc",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)));

        // --- Table Setup ---
        String[] medicineColumns = {"ID", "Tên thuốc", "Công ty", "Tồn kho", "ĐVT", "Đơn giá"};
        medicineTableModel = new DefaultTableModel(medicineColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        medicineTable = new JTable(medicineTableModel);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setFont(new Font("Arial", Font.PLAIN, 12));
        medicineTable.setRowHeight(28);

        medicineTable.setRowSorter(null);

        medicineTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && medicineTable.getSelectedRow() != -1) {
                int viewRow = medicineTable.getSelectedRow();
                String medicineId = (String) medicineTableModel.getValueAt(viewRow, 0);

                allMedicines.stream()
                        .filter(med -> med.getId().equals(medicineId))
                        .findFirst()
                        .ifPresent(this::populateMedicineFields);
            }
        });

        // --- Search Panel ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm thuốc:"));
        medicineSearchField = new JTextField(25);
        searchPanel.add(medicineSearchField);

        medicineSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterMedicineTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterMedicineTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterMedicineTable(); }
        });

        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(medicineTable), BorderLayout.CENTER);

        return listPanel;
    }

    private void handleGetMedInfoResponse(GetMedInfoResponse response) {
        SwingUtilities.invokeLater(() -> {
            if (response != null && response.getMedicines() != null) {
                allMedicines = response.getMedicines();
                populateMedicineTable();
            }
        });
    }

    private void populateMedicineTable() {
        // Just call the filter method, which will show all items if search is empty
        filterMedicineTable();
    }

    private void populateMedicineFields(Medicine med) {
        if (med == null) return;
        selectedMedicineId = med.getId();
        medicineNameField.setText(med.getName());
        medicineCompanyField.setText(med.getCompany());
        medicineDescriptionField.setText(med.getDescription());
        medicineQuantityField.setText(med.getQuantity());
        medicineUnitComboBox.setSelectedItem(med.getUnit());
        medicinePriceField.setText(med.getSellingPrice());
    }

    private void clearMedicineFields() {
        selectedMedicineId = null;
        medicineNameField.setText("");
        medicineCompanyField.setText("");
        medicineDescriptionField.setText("");
        medicineQuantityField.setText("");
        medicineUnitComboBox.setSelectedIndex(0);
        medicinePriceField.setText("");
        medicineTable.clearSelection();
        medicineNameField.requestFocusInWindow();
    }

    private void filterMedicineTable() {
        String filterText = medicineSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        medicineTableModel.setRowCount(0); // Clear the table before adding filtered results

        for (Medicine med : allMedicines) {
            if (filterText.isEmpty() || TextUtils.removeAccents(med.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                medicineTableModel.addRow(new Object[]{
                        med.getId(),
                        med.getName(),
                        med.getCompany(),
                        med.getQuantity(),
                        med.getUnit(),
                        med.getSellingPrice()
                });
            }
        }
    }

    public void cleanup() {
        ClientHandler.deleteListener(GetMedInfoResponse.class, getMedInfoResponseListener);
    }
}