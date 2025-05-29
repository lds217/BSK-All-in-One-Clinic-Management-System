package BsK.client.ui.component.CheckoutPage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.GetMedInfoRequest;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class StandaloneMedicineDialog extends JDialog {
    private JTextField medicineNameField;
    private JTextField barcodeField; // New field for barcode
    private JTextField medicineCompanyField;
    private JTextArea medicineDescriptionField;
    private JSpinner quantitySpinner;
    private JTextField quantityLeftField;
    private JComboBox<String> UnitComboBox;
    private JTextField priceField;
    private JTextField totalField;
    private JSpinner morningSpinner, noonSpinner, eveningSpinner;
    private JTextArea noteField;
    private DefaultTableModel tableModel, selectedTableModel;
    private String[] medcineColumns = {"ID", "Tên thuốc", "Barcode", "Công ty", "Mô tả", "Tồn kho", "ĐVT", "Giá"}; // Added Barcode Column
    private String[][] medicineData;
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::getMedInfoHandler;
    private TableColumnModel columnModel;
    private HashMap<String, Boolean> selectedMedicine = new HashMap<>();
    private JTable medicineTable;
    private JTable selectedTable;
    private boolean isProgrammaticallySettingMedicineNameField = false;

    private String[][] medicinePrescription;
    private static final Logger logger = LoggerFactory.getLogger(StandaloneMedicineDialog.class);

    public String[][] getMedicinePrescription() {
        return medicinePrescription;
    }

    void getMedInfoHandler(GetMedInfoResponse response) {
        logger.info("Received medicine data for Standalone Dialog");
        medicineData = response.getMedInfo(); // Assuming GetMedInfoResponse also returns barcode in medInfo array
        tableModel.setDataVector(medicineData, medcineColumns);
        resizeMedicineTableColumns();
    }

    private void sendGetMedInfoRequest() {
        logger.info("Sending GetMedInfoRequest for Standalone Dialog");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
    }

    public StandaloneMedicineDialog(Frame parent) {
        super(parent, "Thêm thuốc vào hóa đơn (khách lẻ)", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1250, 800); 
        setLocationRelativeTo(parent);
        setResizable(true);

        ClientHandler.addResponseListener(GetMedInfoResponse.class, getMedInfoResponseListener);
        sendGetMedInfoRequest();

        setLayout(new BorderLayout(10, 10));

        JPanel mainInputPanel = new JPanel(new GridBagLayout());
        mainInputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.insets = new Insets(5, 5, 5, 5);
        mainGbc.weightx = 1.0;

        // Search Panel (Name and Barcode)
        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Tìm kiếm thuốc",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0; mainGbc.gridy = 0;
        mainInputPanel.add(searchPanel, mainGbc);

        JPanel medicineInfoPanel = new JPanel(new GridBagLayout());
        medicineInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin thuốc",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        mainInputPanel.add(medicineInfoPanel, mainGbc);

        JPanel quantityPricePanel = new JPanel(new GridBagLayout());
        quantityPricePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Số lượng & Giá",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 2;
        mainInputPanel.add(quantityPricePanel, mainGbc);

        JPanel dosagePanel = new JPanel(new GridBagLayout());
        dosagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Liều dùng (nếu có)", // Dosage might be optional for direct sale
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 3;
        mainInputPanel.add(dosagePanel, mainGbc);

        JPanel notePanel = new JPanel(new GridBagLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Ghi chú (nếu có)",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 4;
        mainInputPanel.add(notePanel, mainGbc);

        JPanel addMedicineButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addMedicineButton = new JButton("Thêm thuốc vào HĐ");
        addMedicineButtonPanel.add(addMedicineButton);
        mainGbc.gridy = 5;
        mainInputPanel.add(addMedicineButtonPanel, mainGbc);

        mainGbc.gridy = 6; mainGbc.weighty = 1.0;
        mainInputPanel.add(new JPanel(), mainGbc); // Filler panel

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(3, 5, 3, 5); // Reduced insets slightly
        gbc.weightx = 0.0; gbc.weighty = 0.0;

        // Search Panel Content
        gbc.gridy = 0; gbc.gridx = 0;
        searchPanel.add(new JLabel("Tên thuốc:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        medicineNameField = new JTextField(20);
        searchPanel.add(medicineNameField, gbc);
        gbc.weightx = 0.0;
        gbc.gridx = 2; gbc.insets = new Insets(3, 15, 3, 5); // More left inset for barcode label
        searchPanel.add(new JLabel("Barcode:"), gbc);
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.gridx = 3; gbc.weightx = 1.0;
        barcodeField = new JTextField(15);
        searchPanel.add(barcodeField, gbc);
        gbc.weightx = 0.0;

        // Medicine Info Panel Content
        gbc.gridy = 0; gbc.gridx = 0;
        medicineInfoPanel.add(new JLabel("Công ty:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        medicineCompanyField = new JTextField(20);
        medicineCompanyField.setEditable(false);
        medicineInfoPanel.add(medicineCompanyField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        medicineInfoPanel.add(new JLabel("Mô tả:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1; gbc.weightx = 1.0;
        medicineDescriptionField = new JTextArea(2, 20); // Reduced rows
        medicineDescriptionField.setLineWrap(true);
        medicineDescriptionField.setWrapStyleWord(true);
        medicineDescriptionField.setEditable(false);
        JScrollPane descriptionScrollPane = new JScrollPane(medicineDescriptionField);
        descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        medicineInfoPanel.add(descriptionScrollPane, gbc);
        gbc.weightx = 0.0;

        // Quantity & Price Panel Content (similar to MedicineDialog)
        gbc.gridy = 0; gbc.gridx = 0;
        quantityPricePanel.add(new JLabel("Số lượng:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1)); // Increased max quantity
        quantitySpinner.setPreferredSize(new Dimension(80, 25));
        quantityPricePanel.add(quantitySpinner, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        quantityPricePanel.add(new JLabel("Còn lại:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        quantityLeftField = new JTextField(5);
        quantityLeftField.setEditable(false);
        quantityPricePanel.add(quantityLeftField, gbc);
        gbc.weightx = 0.0;
        gbc.gridy++; gbc.gridx = 0;
        quantityPricePanel.add(new JLabel("ĐVT:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        String[] units = {"Viên", "Vỉ", "Hộp", "Chai", "Tuýp", "Gói"};
        UnitComboBox = new JComboBox<>(units);
        UnitComboBox.setEnabled(false);
        quantityPricePanel.add(UnitComboBox, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        quantityPricePanel.add(new JLabel("Giá (VNĐ):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        priceField = new JTextField(10);
        priceField.setEditable(false);
        quantityPricePanel.add(priceField, gbc);
        gbc.weightx = 0.0;
        gbc.gridy++; gbc.gridx = 0;
        quantityPricePanel.add(new JLabel("Thành tiền (VNĐ):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        totalField = new JTextField(10);
        totalField.setEditable(false);
        quantityPricePanel.add(totalField, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0.0;

        // Dosage Panel Content
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.0;
        dosagePanel.add(new JLabel("Sáng:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        morningSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        morningSpinner.setPreferredSize(new Dimension(60,25));
        dosagePanel.add(morningSpinner, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        dosagePanel.add(new JLabel("Trưa:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.3;
        noonSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        noonSpinner.setPreferredSize(new Dimension(60,25));
        dosagePanel.add(noonSpinner, gbc);
        gbc.gridx = 4; gbc.weightx = 0.0;
        dosagePanel.add(new JLabel("Chiều:"), gbc);
        gbc.gridx = 5; gbc.weightx = 0.3;
        eveningSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        eveningSpinner.setPreferredSize(new Dimension(60,25));
        dosagePanel.add(eveningSpinner, gbc);
        gbc.weightx = 0.0;

        // Note Panel Content
        gbc.gridy = 0; gbc.gridx = 0;
        notePanel.add(new JLabel("Ghi chú:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        noteField = new JTextArea(2, 20); // Reduced rows
        noteField.setLineWrap(true);
        noteField.setWrapStyleWord(true);
        JScrollPane noteScrollPane = new JScrollPane(noteField);
        noteScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        notePanel.add(noteScrollPane, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.0; gbc.weighty = 0.0;

        // Medicine Table (Right Side)
        tableModel = new DefaultTableModel(medicineData, medcineColumns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setFont(new Font("Arial", Font.PLAIN, 13));
        medicineTable.setRowHeight(22);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setAutoCreateRowSorter(true);
        JScrollPane medicineTableScrollPane = new JScrollPane(medicineTable);
        medicineTableScrollPane.setPreferredSize(new Dimension(650, 250)); // Wider table

        // Selected Medicine Table (Right Side, Bottom)
        String[] selectedColumnNames = {"ID", "Tên thuốc", "SL", "ĐVT", "Sáng", "Trưa", "Chiều", "Đơn giá", "Thành tiền", "Ghi chú"};
        selectedTableModel = new DefaultTableModel(new String[][]{}, selectedColumnNames) {
            @Override public boolean isCellEditable(int row, int column) {
                 // Allow editing for SL, Dosage, Note in selected table
                return column == 2 || column == 4 || column == 5 || column == 6 || column == 9;
            }
        };
        selectedTable = new JTable(selectedTableModel);
        selectedTable.setFont(new Font("Arial", Font.PLAIN, 13));
        selectedTable.setRowHeight(22);
        JScrollPane selectedTableScrollPane = new JScrollPane(selectedTable);
        selectedTableScrollPane.setPreferredSize(new Dimension(650, 180)); // Wider table

        JPanel removeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeMedicineButton = new JButton("Xóa thuốc đã chọn");
        removeButtonPanel.add(removeMedicineButton);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.add(medicineTableScrollPane, BorderLayout.CENTER);
        JPanel bottomRightsPanel = new JPanel(new BorderLayout(0,5));
        bottomRightsPanel.add(selectedTableScrollPane, BorderLayout.CENTER);
        bottomRightsPanel.add(removeButtonPanel, BorderLayout.SOUTH);
        rightPanel.add(bottomRightsPanel, BorderLayout.SOUTH);

        JScrollPane mainInputScrollPane = new JScrollPane(mainInputPanel);
        mainInputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainInputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainInputScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainInputScrollPane, rightPanel);
        splitPane.setResizeWeight(0.42); // Adjusted resize weight slightly
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Lưu & Đóng");
        JButton cancelButton = new JButton("Hủy");
        bottomButtonPanel.add(okButton);
        bottomButtonPanel.add(cancelButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);

        // Event Listeners
        medicineNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { if (!isProgrammaticallySettingMedicineNameField) SwingUtilities.invokeLater(() -> filterMedicineTableByName()); }
            public void removeUpdate(DocumentEvent e) { if (!isProgrammaticallySettingMedicineNameField) SwingUtilities.invokeLater(() -> filterMedicineTableByName()); }
            public void insertUpdate(DocumentEvent e) { if (!isProgrammaticallySettingMedicineNameField) SwingUtilities.invokeLater(() -> filterMedicineTableByName()); }
        });

        barcodeField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    filterMedicineTableByBarcode();
                }
            }
        });

        medicineTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    SwingUtilities.invokeLater(() -> {
                        int selectedRow = medicineTable.getSelectedRow();
                        if (selectedRow != -1) handleMedicineTableRowSelection(selectedRow);
                    });
                }
            }
        });

        medicineNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleTableNavigationKeys(e, medicineTable);
            }
        });
        barcodeField.addKeyListener(new KeyAdapter() { // Allow table nav from barcode field too
             @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    filterMedicineTableByBarcode();
                } else {
                    handleTableNavigationKeys(e, medicineTable);
                }
            }
        });

        quantitySpinner.addChangeListener(e -> updateTotalField());
        UnitComboBox.addActionListener(e -> updateTotalField()); // Though disabled, good practice

        addMedicineButton.addActionListener(e -> addSelectedMedicine());
        removeMedicineButton.addActionListener(e -> removeSelectedMedicine());

        okButton.addActionListener(e -> {
            collectPrescriptionData();
            if (medicinePrescription == null || medicinePrescription.length == 0) {
                // For standalone sale, it's okay to have no prescription if just closing
                // But if they intend to add items, perhaps a different message or behavior
            }
            logger.info("Standalone prescription/items saved.");
            // Log for debugging
            for (String[] row : medicinePrescription) {
                 logger.info("Med: ID={}, Name={}, Qty={}, Unit={}, M={}, N={}, E={}, Price={}, Total={}, Note={}", 
                        (Object[]) row);
            }
            dispose();
        });

        cancelButton.addActionListener(e -> {
            medicinePrescription = null; // Clear any collected data if cancelled
            dispose();
        });
    }
    
    private void handleTableNavigationKeys(KeyEvent e, JTable targetTable) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (targetTable.getSelectedRow() != -1) addSelectedMedicine();
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            if (targetTable.getRowCount() > 0) {
                int selectedRow = targetTable.getSelectedRow();
                selectedRow = (selectedRow < targetTable.getRowCount() - 1) ? selectedRow + 1 : 0;
                targetTable.setRowSelectionInterval(selectedRow, selectedRow);
                targetTable.scrollRectToVisible(targetTable.getCellRect(selectedRow, 0, true));
                handleMedicineTableRowSelection(selectedRow);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            if (targetTable.getRowCount() > 0) {
                int selectedRow = targetTable.getSelectedRow();
                selectedRow = (selectedRow > 0) ? selectedRow - 1 : targetTable.getRowCount() - 1;
                targetTable.setRowSelectionInterval(selectedRow, selectedRow);
                targetTable.scrollRectToVisible(targetTable.getCellRect(selectedRow, 0, true));
                handleMedicineTableRowSelection(selectedRow);
            }
        }
    }

    private void filterMedicineTableByName() {
        filterMedicineTable(medicineNameField.getText().trim(), null);
    }

    private void filterMedicineTableByBarcode() {
        filterMedicineTable(null, barcodeField.getText().trim());
         if (medicineTable.getRowCount() == 1) { // If unique match by barcode
            medicineTable.setRowSelectionInterval(0,0);
            handleMedicineTableRowSelection(0);
            addSelectedMedicine(); // Optionally auto-add if unique and quantity is set
            barcodeField.setText(""); // Clear barcode field after processing
            medicineNameField.requestFocusInWindow(); // Or quantitySpinner
        }
    }

    private void filterMedicineTable(String nameFilter, String barcodeFilter) {
        if (medicineData == null) return;
        
        boolean nameIsEmpty = (nameFilter == null || nameFilter.isEmpty());
        boolean barcodeIsEmpty = (barcodeFilter == null || barcodeFilter.isEmpty());

        if (nameIsEmpty && barcodeIsEmpty) {
            tableModel.setDataVector(medicineData, medcineColumns);
            resizeMedicineTableColumns();
            medicineTable.clearSelection();
            clearInputFields(); // Also clear input fields when filter is cleared
        } else {
            List<String[]> filteredData = new ArrayList<>();
            String lowerCaseNameFilter = nameIsEmpty ? "" : TextUtils.removeAccents(nameFilter.toLowerCase());
            // Barcode is usually exact match or startsWith, not contains typically
            String lowerCaseBarcodeFilter = barcodeIsEmpty ? "" : barcodeFilter.toLowerCase(); 

            for (String[] row : medicineData) {
                boolean nameMatch = nameIsEmpty; // True if no name filter
                boolean barcodeMatch = barcodeIsEmpty; // True if no barcode filter

                if (!nameIsEmpty && row.length > 1 && row[1] != null) {
                    nameMatch = TextUtils.removeAccents(row[1].toLowerCase()).contains(lowerCaseNameFilter);
                }
                 // Assuming barcode is at index 2 in medicineData from server (ID, Name, Barcode, Company...)
                if (!barcodeIsEmpty && row.length > 2 && row[2] != null) {
                    barcodeMatch = row[2].toLowerCase().equals(lowerCaseBarcodeFilter); // Exact match for barcode
                }

                if (nameMatch && barcodeMatch) { // If name filter provided, must match name. If barcode provided, must match barcode.
                    filteredData.add(row);
                }
            }
            tableModel.setDataVector(filteredData.toArray(new String[0][0]), medcineColumns);
            resizeMedicineTableColumns();
            if (!filteredData.isEmpty()) {
                medicineTable.setRowSelectionInterval(0,0);
                handleMedicineTableRowSelection(0);
            } else {
                clearInputFields();
            }
        }
    }

    private void resizeMedicineTableColumns() {
        if (medicineTable.getColumnCount() == 0) return;
        columnModel = medicineTable.getColumnModel();
        // ID, Tên thuốc, Barcode, Công ty, Mô tả, Tồn kho, ĐVT, Giá
        columnModel.getColumn(0).setPreferredWidth(30);  // ID
        columnModel.getColumn(1).setPreferredWidth(150); // Tên thuốc
        columnModel.getColumn(2).setPreferredWidth(100); // Barcode
        columnModel.getColumn(3).setPreferredWidth(100); // Công ty
        columnModel.getColumn(4).setPreferredWidth(180); // Mô tả
        columnModel.getColumn(5).setPreferredWidth(50);  // Tồn kho
        columnModel.getColumn(6).setPreferredWidth(40);  // ĐVT
        columnModel.getColumn(7).setPreferredWidth(60);  // Giá
    }

    private void handleMedicineTableRowSelection(int viewRow) {
        if (viewRow < 0 || viewRow >= medicineTable.getRowCount()) {
            clearInputFields();
            return;
        }
        int modelRow = medicineTable.convertRowIndexToModel(viewRow);

        isProgrammaticallySettingMedicineNameField = true;
        medicineNameField.setText(tableModel.getValueAt(modelRow, 1).toString());
        isProgrammaticallySettingMedicineNameField = false;
        // Assuming barcode is column 2. If not present in tableModel directly, might need to fetch from medicineData using ID
        if (tableModel.getColumnCount() > 2 && tableModel.getValueAt(modelRow, 2) != null) {
             barcodeField.setText(tableModel.getValueAt(modelRow, 2).toString());
        }
        medicineCompanyField.setText(tableModel.getValueAt(modelRow, 3).toString());
        medicineDescriptionField.setText(tableModel.getValueAt(modelRow, 4).toString());
        quantityLeftField.setText(tableModel.getValueAt(modelRow, 5).toString());
        UnitComboBox.setSelectedItem(tableModel.getValueAt(modelRow, 6).toString());
        priceField.setText(tableModel.getValueAt(modelRow, 7).toString());
        
        quantitySpinner.setValue(1);
        morningSpinner.setValue(0); // Reset dosage for standalone sale, or make it optional
        noonSpinner.setValue(0);
        eveningSpinner.setValue(0);
        noteField.setText("");
        updateTotalField();
    }

    private void clearInputFields() {
        // Don't clear medicineNameField or barcodeField as they are used for filtering
        medicineCompanyField.setText("");
        medicineDescriptionField.setText("");
        quantityLeftField.setText("");
        UnitComboBox.setSelectedIndex(0); // Or set to a default/blank if applicable
        priceField.setText("");
        totalField.setText("");
        quantitySpinner.setValue(1);
        morningSpinner.setValue(0);
        noonSpinner.setValue(0);
        eveningSpinner.setValue(0);
        noteField.setText("");
    }

    private void updateTotalField() {
        try {
            int quantity = (Integer) quantitySpinner.getValue();
            if (quantity < 0) { quantitySpinner.setValue(0); quantity = 0; }
            double price = priceField.getText().isEmpty() ? 0 : Double.parseDouble(priceField.getText().replace(",",""));
            totalField.setText(String.format("%,.0f", quantity * price)); // Format with commas
        } catch (NumberFormatException e) {
            totalField.setText("Giá trị không hợp lệ");
            logger.error("Error parsing price for total calculation: " + priceField.getText(), e);
        }
    }

    private void addSelectedMedicine() {
        int selectedRowInMedicineTable = medicineTable.getSelectedRow();
        String medicineId, name, unit, currentPrice, barcodeVal;

        if (selectedRowInMedicineTable != -1) {
            int modelRow = medicineTable.convertRowIndexToModel(selectedRowInMedicineTable);
            medicineId = tableModel.getValueAt(modelRow, 0).toString();
            name = tableModel.getValueAt(modelRow, 1).toString();
            barcodeVal = tableModel.getColumnCount() > 2 && tableModel.getValueAt(modelRow, 2) != null ? tableModel.getValueAt(modelRow, 2).toString() : "N/A";
            unit = tableModel.getValueAt(modelRow, 6).toString(); // ĐVT is at 6 (0-indexed)
            currentPrice = tableModel.getValueAt(modelRow, 7).toString(); // Giá is at 7
        } else if (!medicineNameField.getText().isEmpty() || !barcodeField.getText().isEmpty()){
            // Attempt to find by name or barcode if no table selection but fields have text
            String searchName = medicineNameField.getText().trim();
            String searchBarcode = barcodeField.getText().trim();
            String foundId = null, foundUnit = null, foundPrice = null, actualName = null, actualBarcode = null;

            for (String[] med : medicineData) { // medicineData: ID, Tên thuốc, Barcode, Công ty, Mô tả, Tồn kho, ĐVT, Giá
                boolean nameMatches = !searchName.isEmpty() && TextUtils.removeAccents(med[1].toLowerCase()).contains(TextUtils.removeAccents(searchName.toLowerCase()));
                boolean barcodeMatches = !searchBarcode.isEmpty() && med.length > 2 && med[2] != null && med[2].equalsIgnoreCase(searchBarcode);
                if (nameMatches || barcodeMatches) {
                    foundId = med[0]; actualName = med[1]; actualBarcode = med[2]; 
                    foundUnit = med[6]; foundPrice = med[7];
                    break; // Take first match
                }
            }
             if (foundId == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thuốc khớp với tên hoặc barcode đã nhập.", "Không tìm thấy thuốc", JOptionPane.WARNING_MESSAGE);
                return;
            }
            medicineId = foundId; name = actualName; unit = foundUnit; currentPrice = foundPrice; barcodeVal = actualBarcode;
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một thuốc từ danh sách hoặc nhập tên/barcode.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int quantity = (Integer) quantitySpinner.getValue();
        int morningVal = (Integer) morningSpinner.getValue();
        int noonVal = (Integer) noonSpinner.getValue();
        int eveningVal = (Integer) eveningSpinner.getValue();
        String note = noteField.getText();
        String totalAmount = totalField.getText().replace(",",""); // Remove comma for internal storage if needed

        if (quantity <= 0) {
            JOptionPane.showMessageDialog(this, "Số lượng phải lớn hơn 0.", "Số lượng không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedTableModel.addRow(new Object[]{medicineId, name, quantity, unit, morningVal, noonVal, eveningVal, currentPrice, totalAmount, note});
    }

    private void removeSelectedMedicine() {
        int selectedRow = selectedTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedTableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một thuốc từ bảng dưới để xóa.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void collectPrescriptionData() {
        int rowCount = selectedTableModel.getRowCount();
        if (rowCount == 0) {
            medicinePrescription = new String[0][0];
            return;
        }
        medicinePrescription = new String[rowCount][10];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < 10; j++) {
                 medicinePrescription[i][j] = selectedTableModel.getValueAt(i, j) != null ? selectedTableModel.getValueAt(i, j).toString() : "";
            }
        }
    }

    @Override
    public void dispose() {
        logger.info("Cleaning up StandaloneMedicineDialog listeners");
        ClientHandler.deleteListener(GetMedInfoResponse.class); // Ensure this matches the listener added
        super.dispose();
    }

    // Main method for testing this dialog independently
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Standalone Medicine Dialog");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            JButton openDialogButton = new JButton("Open Standalone Medicine Dialog");
            openDialogButton.addActionListener(e -> {
                StandaloneMedicineDialog dialog = new StandaloneMedicineDialog(frame);
                dialog.setVisible(true);
                String[][] prescription = dialog.getMedicinePrescription();
                if (prescription != null && prescription.length > 0) {
                    System.out.println("Standalone Items obtained:");
                    for (String[] item : prescription) {
                        // Ensure correct number of items for format string
                        if (item.length == 10) {
                             System.out.println(String.format("ID:%s, Name:%s, Qty:%s, Unit:%s, M:%s, N:%s, E:%s, Price:%s, Total:%s, Note:%s", 
                                (Object[]) item));
                        } else {
                            System.out.println("Item with incorrect number of fields: " + java.util.Arrays.toString(item));
                        }
                    }
                } else {
                    System.out.println("No items or dialog was cancelled.");
                }
            });
            frame.setLayout(new FlowLayout());
            frame.add(openDialogButton);
            frame.setVisible(true);
        });
    }
} 