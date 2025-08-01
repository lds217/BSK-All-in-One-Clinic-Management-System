package BsK.client.ui.component.DataDialog;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Medicine;
import BsK.common.packet.req.AddMedicineRequest;
import BsK.common.packet.req.EditMedicineRequest;
import BsK.common.packet.req.GetMedInfoRequest;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A JPanel dedicated to managing medicine inventory (CRUD operations).
 * It handles its own UI, data fetching, and event handling.
 */
@Slf4j
public class MedicineManagementPanel extends JPanel {

    // --- UI Components ---
    private JTable medicineTable;
    private DefaultTableModel medicineTableModel;
    private JTextField medicineSearchField;

    // --- Input Fields ---
    private JTextField medicineNameField;
    private JTextField medicineCompanyField;
    private JTextArea medicineDescriptionField;
    private JTextField routeField; // --- ADDED --- Field for Route of Administration
    private JTextField medicineUnitField;
    private JTextField medicinePriceField;
    private JSpinner morningSpinner, noonSpinner, eveningSpinner;
    private JTextArea noteField;
    private JCheckBox chkIsDeleted;      // Checkbox for soft delete
    private JCheckBox chkIsSupplement;   // Checkbox for supplement status

    // --- Action Buttons ---
    private JButton btnAdd, btnEdit, btnClear;

    // --- Data & State ---
    private List<Medicine> allMedicines = new ArrayList<>();
    private String selectedMedicineId = null;

    // --- Networking ---
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
        JScrollPane descriptionScrollPane = new JScrollPane(medicineDescriptionField);
        descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        medicineInfoPanel.add(descriptionScrollPane, gbc);
        gbc.weightx = 0.0;

        // --- ADDED: Route of Administration Field ---
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel routeLabel = new JLabel("Đường dùng:");
        routeLabel.setFont(labelFont);
        medicineInfoPanel.add(routeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        routeField = new JTextField(20);
        routeField.setFont(textFont);
        routeField.setPreferredSize(textFieldSize);
        medicineInfoPanel.add(routeField, gbc);
        gbc.weightx = 0.0;
        // --- END ADDED ---

        mainGbc.gridx = 0; mainGbc.gridy = 0;
        mainInputPanel.add(medicineInfoPanel, mainGbc);

        // --- Sub-Panel 2: Unit & Price (Simplified) ---
        JPanel quantityPricePanel = new JPanel(new GridBagLayout());
        quantityPricePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Đơn vị & Giá",
                TitledBorder.LEADING, TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));

        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel unitLabel = new JLabel("ĐVT:");
        unitLabel.setFont(labelFont);
        quantityPricePanel.add(unitLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        medicineUnitField = new JTextField(10);
        medicineUnitField.setFont(textFont);
        medicineUnitField.setPreferredSize(textFieldSize);
        quantityPricePanel.add(medicineUnitField, gbc);

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
        quantityPricePanel.add(medicinePriceField, gbc);
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
        btnAdd = new JButton("Thêm mới");
        btnEdit = new JButton("Chỉnh sửa");
        btnClear = new JButton("Làm mới");

        Font checkboxFont = new Font("Arial", Font.BOLD, 13);
        chkIsSupplement = new JCheckBox("Thực phẩm bổ sung");
        chkIsSupplement.setFont(checkboxFont);
        chkIsDeleted = new JCheckBox("Ẩn (Xoá)");
        chkIsDeleted.setFont(checkboxFont);

        Dimension btnSize = new Dimension(100, 35);
        btnAdd.setPreferredSize(btnSize);
        btnEdit.setPreferredSize(btnSize);
        btnClear.setPreferredSize(btnSize);

        // --- Component Order Changed Here ---
        buttonPanel.add(chkIsSupplement);
        buttonPanel.add(chkIsDeleted);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnClear);

        // --- Button Actions ---
        btnClear.addActionListener(e -> clearMedicineFields());

        // --- ADD MEDICINE ACTION ---
        btnAdd.addActionListener(e -> {
            // 1. Validate required fields
            String name = medicineNameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên thuốc không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                medicineNameField.requestFocusInWindow();
                return;
            }

            String priceText = medicinePriceField.getText().trim();
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Giá thuốc không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                medicinePriceField.requestFocusInWindow();
                return;
            }

            // 2. Gather data from all other fields
            String company = medicineCompanyField.getText().trim();
            String description = medicineDescriptionField.getText().trim();
            String route = routeField.getText().trim(); // --- ADDED ---
            String unit = medicineUnitField.getText().trim();
            Boolean isSupplement = chkIsSupplement.isSelected();
            Boolean isDeleted = false; // New medicines are active by default

            // 3. Parse price and handle potential errors
            double price;
            try {
                price = Double.parseDouble(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Giá thuốc không được là số âm.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    medicinePriceField.requestFocusInWindow();
                    return;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Giá thuốc phải là một con số hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                medicinePriceField.requestFocusInWindow();
                return;
            }

            // 4. Construct the special 'preferredNote' string
            String preferredNote = morningSpinner.getValue().toString() + ","
                                 + noonSpinner.getValue().toString() + ","
                                 + eveningSpinner.getValue().toString() + ","
                                 + noteField.getText().trim();

            // 5. Create the request packet and send it to the server
            // --- UPDATED ---: Added 'route' to the request constructor
            AddMedicineRequest request = new AddMedicineRequest(name, company, description, unit, price, preferredNote, isSupplement, isDeleted, route);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

            // 6. Provide feedback and reset the form
            JOptionPane.showMessageDialog(this, "Yêu cầu thêm thuốc '" + name + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearMedicineFields();
        });

        // --- EDIT MEDICINE ACTION ---
        btnEdit.addActionListener(e -> {
            // 1. Check if a medicine has been selected
            if (selectedMedicineId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một loại thuốc để chỉnh sửa.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Validate required fields
            String name = medicineNameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên thuốc không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                medicineNameField.requestFocusInWindow();
                return;
            }

            String priceText = medicinePriceField.getText().trim();
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Giá thuốc không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                medicinePriceField.requestFocusInWindow();
                return;
            }

            // 3. Gather data from all fields
            String id = selectedMedicineId;
            String company = medicineCompanyField.getText().trim();
            String description = medicineDescriptionField.getText().trim();
            String route = routeField.getText().trim(); // --- ADDED ---
            String unit = medicineUnitField.getText().trim();
            Boolean isSupplement = chkIsSupplement.isSelected();
            Boolean isDeleted = chkIsDeleted.isSelected();

            // 4. Parse price and handle errors
            Double price;
            try {
                price = Double.parseDouble(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Giá thuốc không được là số âm.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    medicinePriceField.requestFocusInWindow();
                    return;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Giá thuốc phải là một con số hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                medicinePriceField.requestFocusInWindow();
                return;
            }

            // 5. Construct the 'preferredNote' string
            String preferredNote = morningSpinner.getValue().toString() + ","
                                 + noonSpinner.getValue().toString() + ","
                                 + eveningSpinner.getValue().toString() + ","
                                 + noteField.getText().trim();

            // 6. Create the request packet and send it
            // --- UPDATED ---: Added 'route' to the request constructor
            EditMedicineRequest request = new EditMedicineRequest(id, name, company, description, unit, price, preferredNote, isSupplement, isDeleted, route);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

            // 7. Provide user feedback and clear the form
            JOptionPane.showMessageDialog(this, "Yêu cầu chỉnh sửa thuốc '" + name + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearMedicineFields();
        });

        // Initial button states
        btnEdit.setEnabled(false);
        chkIsSupplement.setEnabled(true); // Can set for new items
        chkIsDeleted.setEnabled(false);   // Can only set for existing items

        return buttonPanel;
    }

    private JPanel createMedicineListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Danh Sách Thuốc",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)));

        // --- Table Setup ---
        // --- BUG FIX: Corrected column names to match the data being added ---
        String[] medicineColumns = {"ID", "Tên thuốc", "Bổ sung", "Trạng thái"};
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
        // FIX: Add the text field to the panel, not the panel to itself
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
        routeField.setText(med.getRoute()); // --- ADDED ---
        medicineUnitField.setText(med.getUnit());
        medicinePriceField.setText(med.getSellingPrice());

        // Set checkboxes based on the medicine's status
        chkIsSupplement.setSelected("1".equals(med.getSupplement()));
        chkIsDeleted.setSelected("1".equals(med.getDeleted()));

        // --- PARSE PREFERRED NOTE ---
        String preferredNote = med.getPreferredNote();
        if (preferredNote != null && !preferredNote.trim().isEmpty()) {
            String[] parts = preferredNote.split(",", 4);
            try {
                morningSpinner.setValue(parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0);
                noonSpinner.setValue(parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0);
                eveningSpinner.setValue(parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0);
                noteField.setText(parts.length > 3 ? parts[3].trim() : "");
            } catch (NumberFormatException e) {
                log.error("Could not parse preferred note: {}", preferredNote, e);
                // Reset to default values in case of parsing error
                morningSpinner.setValue(0);
                noonSpinner.setValue(0);
                eveningSpinner.setValue(0);
                noteField.setText("");
            }
        } else {
            // No preferred note, set default values
            morningSpinner.setValue(0);
            noonSpinner.setValue(0);
            eveningSpinner.setValue(0);
            noteField.setText("");
        }
        // --- END PARSING ---

        // Update button states
        btnAdd.setEnabled(false);
        btnEdit.setEnabled(true);
        chkIsSupplement.setEnabled(true);
        chkIsDeleted.setEnabled(true);
    }

    private void clearMedicineFields() {
        selectedMedicineId = null;
        medicineNameField.setText("");
        medicineCompanyField.setText("");
        medicineDescriptionField.setText("");
        routeField.setText(""); // --- ADDED ---
        medicineUnitField.setText("");
        medicinePriceField.setText("");
        
        // --- ADDED RESET LOGIC ---
        morningSpinner.setValue(0);
        noonSpinner.setValue(0);
        eveningSpinner.setValue(0);
        noteField.setText("");
        // --- END ADD ---

        chkIsSupplement.setSelected(false);
        chkIsDeleted.setSelected(false);
        medicineTable.clearSelection();
        medicineNameField.requestFocusInWindow();

        // Reset button states
        btnAdd.setEnabled(true);
        btnEdit.setEnabled(false);
        chkIsSupplement.setEnabled(true);
        chkIsDeleted.setEnabled(false);
    }

    private void filterMedicineTable() {
        String filterText = medicineSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        medicineTableModel.setRowCount(0); // Clear the table before adding filtered results

        for (Medicine med : allMedicines) {
            // Check if name matches filter text
            boolean nameMatches = filterText.isEmpty() || TextUtils.removeAccents(med.getName().toLowerCase()).contains(lowerCaseFilterText);

            if (nameMatches) {
                // Determine the status text based on the flags
                String supplementStatus = "1".equals(med.getSupplement()) ? "Có" : "Không";
                String deletedStatus = "0".equals(med.getDeleted()) ? "Đang bán" : "Đã ẩn";

                medicineTableModel.addRow(new Object[]{
                        med.getId(),
                        med.getName(),
                        supplementStatus,
                        deletedStatus
                });
            }
        }
    }

    public void cleanup() {
        ClientHandler.deleteListener(GetMedInfoResponse.class, getMedInfoResponseListener);
    }
}