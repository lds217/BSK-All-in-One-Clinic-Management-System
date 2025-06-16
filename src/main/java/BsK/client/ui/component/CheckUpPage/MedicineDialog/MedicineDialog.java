package BsK.client.ui.component.CheckUpPage.MedicineDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Medicine;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class MedicineDialog extends JDialog {
    private JTextField medicineNameField;
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
    private String[] medcineColumns = {"ID", "Tên thuốc", "Công ty", "Mô tả", "Tồn kho", "ĐVT", "Giá"};
    private String[][] medicineData;
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::getMedInfoHandler;
    private TableColumnModel columnModel;
    private HashMap<String, Boolean> selectedMedicine = new HashMap<>();
    private JTable medicineTable;
    private JTable selectedTable;
    private boolean isProgrammaticallySettingMedicineNameField = false;

    private List<Medicine> medicines = new ArrayList<>();
    private String[][] medicinePrescription;
    private static final Logger logger = LoggerFactory.getLogger(MedicineDialog.class);

    public String[][] getMedicinePrescription() {
        return medicinePrescription;
    }

    void getMedInfoHandler(GetMedInfoResponse response) {
        logger.info("Received medicine data");
        medicineData = response.getMedInfo();
        
        // Get Medicine objects from the response
        medicines = response.getMedicines();
        
        // Set the data vector with the raw string arrays for backward compatibility
        tableModel.setDataVector(medicineData, medcineColumns);

        // resize column width
        columnModel = medicineTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(20); // ID
        columnModel.getColumn(1).setPreferredWidth(150); // Name
        columnModel.getColumn(2).setPreferredWidth(100); // Company
        columnModel.getColumn(3).setPreferredWidth(200); // Description
        columnModel.getColumn(4).setPreferredWidth(40); // Stock
        columnModel.getColumn(5).setPreferredWidth(40); // Unit
        columnModel.getColumn(6).setPreferredWidth(50); // Price
    }

    private void sendGetMedInfoRequest() {
        logger.info("Sending GetMedInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
    }

    public MedicineDialog(Frame parent) {
        super(parent, "Thêm thuốc", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(parent);
        setResizable(true);
        
        // Ensure modal behavior and proper parent relationship
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(false); // Don't force always on top, let modal behavior handle this
        
        // Add window listener to handle minimize/restore events with parent
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                if (parent != null) {
                    parent.setState(Frame.ICONIFIED);
                }
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
                if (parent != null && parent.getState() == Frame.ICONIFIED) {
                    parent.setState(Frame.NORMAL);
                }
            }
        });

        // Initialize medicinePrescription as empty array
        medicinePrescription = new String[0][0];

        // Add response listener and send request for medicine data
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

        JPanel medicineInfoPanel = new JPanel(new GridBagLayout());
        medicineInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin thuốc",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainInputPanel.add(medicineInfoPanel, mainGbc);

        JPanel quantityPricePanel = new JPanel(new GridBagLayout());
        quantityPricePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Số lượng & Giá",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        mainInputPanel.add(quantityPricePanel, mainGbc);

        JPanel dosagePanel = new JPanel(new GridBagLayout());
        dosagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Liều dùng",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 2;
        mainInputPanel.add(dosagePanel, mainGbc);

        JPanel notePanel = new JPanel(new GridBagLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Ghi chú",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 3;
        mainInputPanel.add(notePanel, mainGbc);

        JPanel addMedicineButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addMedicineButton = new JButton("Thêm thuốc");
        addMedicineButtonPanel.add(addMedicineButton);
        mainGbc.gridy = 4;
        mainInputPanel.add(addMedicineButtonPanel, mainGbc);

        mainGbc.gridy = 5;
        mainGbc.weighty = 1.0;
        mainInputPanel.add(new JPanel(), mainGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        medicineInfoPanel.add(new JLabel("Tên thuốc:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        medicineNameField = new JTextField(20);
        medicineInfoPanel.add(medicineNameField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        medicineInfoPanel.add(new JLabel("Công ty:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        medicineCompanyField = new JTextField(20);
        medicineCompanyField.setEditable(false);
        medicineInfoPanel.add(medicineCompanyField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        medicineInfoPanel.add(new JLabel("Mô tả:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        medicineDescriptionField = new JTextArea(3, 20);
        medicineDescriptionField.setLineWrap(true);
        medicineDescriptionField.setWrapStyleWord(true);
        medicineDescriptionField.setEditable(false);
        JScrollPane descriptionScrollPane = new JScrollPane(medicineDescriptionField);
        descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        medicineInfoPanel.add(descriptionScrollPane, gbc);
        gbc.weightx = 0.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        quantityPricePanel.add(new JLabel("Số lượng:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setPreferredSize(new Dimension(80, 25));
        quantityPricePanel.add(quantitySpinner, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        quantityPricePanel.add(new JLabel("Còn lại:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        quantityLeftField = new JTextField(5);
        quantityLeftField.setEditable(false);
        quantityPricePanel.add(quantityLeftField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        quantityPricePanel.add(new JLabel("ĐVT:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        String[] units = {"Viên", "Vỉ", "Hộp", "Chai", "Tuýp", "Gói"};
        UnitComboBox = new JComboBox<>(units);
        UnitComboBox.setEnabled(false);
        quantityPricePanel.add(UnitComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        quantityPricePanel.add(new JLabel("Giá (VNĐ):"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        priceField = new JTextField(10);
        priceField.setEditable(false);
        quantityPricePanel.add(priceField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        quantityPricePanel.add(new JLabel("Thành tiền (VNĐ):"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        totalField = new JTextField(10);
        totalField.setEditable(false);
        quantityPricePanel.add(totalField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        SpinnerNumberModel dosageModelTemplate = new SpinnerNumberModel(0, 0, 10, 1);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        dosagePanel.add(new JLabel("Sáng:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        morningSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        morningSpinner.setPreferredSize(new Dimension(60,25));
        dosagePanel.add(morningSpinner, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        dosagePanel.add(new JLabel("Trưa:"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 0.3;
        noonSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        noonSpinner.setPreferredSize(new Dimension(60,25));
        dosagePanel.add(noonSpinner, gbc);
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        dosagePanel.add(new JLabel("Chiều:"), gbc);
        gbc.gridx = 5;
        gbc.weightx = 0.3;
        eveningSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        eveningSpinner.setPreferredSize(new Dimension(60,25));
        dosagePanel.add(eveningSpinner, gbc);
        gbc.weightx = 0.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        notePanel.add(new JLabel("Ghi chú:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        noteField = new JTextArea(3, 20);
        noteField.setLineWrap(true);
        noteField.setWrapStyleWord(true);
        JScrollPane noteScrollPane = new JScrollPane(noteField);
        noteScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        notePanel.add(noteScrollPane, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        tableModel = new DefaultTableModel(medicineData, medcineColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setFont(new Font("Arial", Font.PLAIN, 14));
        medicineTable.setRowHeight(25);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.setAutoCreateRowSorter(true);
        JScrollPane medicineTableScrollPane = new JScrollPane(medicineTable);
        medicineTableScrollPane.setPreferredSize(new Dimension(600, 250));

        String[] selectedColumnNames = {"ID", "Tên thuốc", "SL", "ĐVT", "Sáng", "Trưa", "Chiều", "Đơn giá", "Thành tiền", "Ghi chú"};
        selectedTableModel = new DefaultTableModel(new String[][]{}, selectedColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 4 || column == 5 || column == 6 || column == 9;
            }
        };
        selectedTable = new JTable(selectedTableModel);
        selectedTable.setFont(new Font("Arial", Font.PLAIN, 14));
        selectedTable.setRowHeight(25);
        JScrollPane selectedTableScrollPane = new JScrollPane(selectedTable);
        selectedTableScrollPane.setPreferredSize(new Dimension(600, 150));

        JPanel removeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeMedicineButton = new JButton("Xóa thuốc đã chọn");
        removeButtonPanel.add(removeMedicineButton);

        // Create titled panels for right side sections
        JPanel availableMedicinesPanel = new JPanel(new BorderLayout());
        availableMedicinesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thuốc trong kho",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(63, 81, 181)
        ));
        availableMedicinesPanel.add(medicineTableScrollPane, BorderLayout.CENTER);
        
        JPanel chosenMedicinesPanel = new JPanel(new BorderLayout());
        chosenMedicinesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thuốc đã chọn",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(220, 20, 60)
        ));
        chosenMedicinesPanel.setBackground(new Color(255, 240, 240)); // Light red background
        selectedTableScrollPane.setBackground(new Color(255, 240, 240)); // Light red background
        chosenMedicinesPanel.add(selectedTableScrollPane, BorderLayout.CENTER);
        chosenMedicinesPanel.add(removeButtonPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 5));
        rightPanel.add(availableMedicinesPanel, BorderLayout.CENTER);
        rightPanel.add(chosenMedicinesPanel, BorderLayout.SOUTH);

        JScrollPane mainInputScrollPane = new JScrollPane(mainInputPanel);
        mainInputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainInputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainInputScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainInputScrollPane, rightPanel);
        splitPane.setResizeWeight(0.45);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        bottomButtonPanel.add(okButton);
        bottomButtonPanel.add(cancelButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);

        medicineNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingMedicineNameField) {
                    SwingUtilities.invokeLater(() -> filterMedicineTable()); 
                }
            }
            public void removeUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingMedicineNameField) {
                    SwingUtilities.invokeLater(() -> filterMedicineTable()); 
                }
            }
            public void insertUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingMedicineNameField) {
                    SwingUtilities.invokeLater(() -> filterMedicineTable()); 
                }
            }
        });

        medicineTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    SwingUtilities.invokeLater(() -> {
                        int selectedRow = medicineTable.getSelectedRow();
                        if (selectedRow != -1) {
                            handleMedicineTableRowSelection(selectedRow);
                        }
                    });
                }
            }
        });
        medicineNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (medicineTable.getSelectedRow() != -1) addSelectedMedicine();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (medicineTable.getRowCount() > 0) {
                        int selectedRow = medicineTable.getSelectedRow();
                        selectedRow = (selectedRow < medicineTable.getRowCount() - 1) ? selectedRow + 1 : 0;
                        medicineTable.setRowSelectionInterval(selectedRow, selectedRow);
                        medicineTable.scrollRectToVisible(medicineTable.getCellRect(selectedRow, 0, true));
                        handleMedicineTableRowSelection(selectedRow);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (medicineTable.getRowCount() > 0) {
                        int selectedRow = medicineTable.getSelectedRow();
                        selectedRow = (selectedRow > 0) ? selectedRow - 1 : medicineTable.getRowCount() - 1;
                        medicineTable.setRowSelectionInterval(selectedRow, selectedRow);
                        medicineTable.scrollRectToVisible(medicineTable.getCellRect(selectedRow, 0, true));
                        handleMedicineTableRowSelection(selectedRow);
                    }
                }
            }
        });

        quantitySpinner.addChangeListener(e -> updateTotalField());
        UnitComboBox.addActionListener(e -> updateTotalField());

        addMedicineButton.addActionListener(e -> addSelectedMedicine());
        removeMedicineButton.addActionListener(e -> removeSelectedMedicine());

        okButton.addActionListener(e -> {
            collectPrescriptionData();
            if (medicinePrescription == null || medicinePrescription.length == 0) {
                JOptionPane.showMessageDialog(this, "Chưa có thuốc nào được chọn.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            logger.info("Prescription saved.");
            for (String[] row : medicinePrescription) {
                logger.info("Med: ID={}, Name={}, Qty={}, Unit={}, M={}, N={}, E={}, Price={}, Total={}, Note={}", 
                        row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9]);
            }
            dispose();
        });

        cancelButton.addActionListener(e -> {
            medicinePrescription = null;
            dispose();
        });
    }

    private void filterMedicineTable() {
        String filterText = medicineNameField.getText().trim();
        if (medicineData == null) return;
        if (filterText.isEmpty()) {
            tableModel.setDataVector(medicineData, medcineColumns);
            resizeMedicineTableColumns();
                    medicineTable.clearSelection();
        } else {
            List<String[]> filteredData = new ArrayList<>();
            List<Medicine> filteredMedicines = new ArrayList<>();
            String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());
            
            for (Medicine med : medicines) {
                if (TextUtils.removeAccents(med.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                    filteredMedicines.add(med);
                    filteredData.add(med.toStringArray());
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
        columnModel = medicineTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(20);
        columnModel.getColumn(1).setPreferredWidth(150);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(200);
        columnModel.getColumn(4).setPreferredWidth(40);
        columnModel.getColumn(5).setPreferredWidth(40);
        columnModel.getColumn(6).setPreferredWidth(50);
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

        medicineCompanyField.setText(tableModel.getValueAt(modelRow, 2).toString());
        medicineDescriptionField.setText(tableModel.getValueAt(modelRow, 3).toString());
        quantityLeftField.setText(tableModel.getValueAt(modelRow, 4).toString());
        UnitComboBox.setSelectedItem(tableModel.getValueAt(modelRow, 5).toString());
        priceField.setText(tableModel.getValueAt(modelRow, 6).toString());
        
        quantitySpinner.setValue(1);
        morningSpinner.setValue(0);
        noonSpinner.setValue(0);
        eveningSpinner.setValue(0);
        noteField.setText("");
        updateTotalField();
    }

    private void clearInputFields() {
        medicineCompanyField.setText("");
        medicineDescriptionField.setText("");
        quantityLeftField.setText("");
        UnitComboBox.setSelectedIndex(0);
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
            double price = priceField.getText().isEmpty() ? 0 : Double.parseDouble(priceField.getText());
            totalField.setText(String.format("%.0f", quantity * price));
        } catch (NumberFormatException e) {
            totalField.setText("Giá trị không hợp lệ");
            logger.error("Error parsing price for total calculation: " + priceField.getText(), e);
        }
    }

    private void addSelectedMedicine() {
        int selectedRowInMedicineTable = medicineTable.getSelectedRow();
        if (selectedRowInMedicineTable == -1 && medicineNameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một thuốc từ danh sách hoặc tìm kiếm.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String medicineId, name, unit, currentPrice;

        if (selectedRowInMedicineTable != -1) {
            int modelRow = medicineTable.convertRowIndexToModel(selectedRowInMedicineTable);
            medicineId = tableModel.getValueAt(modelRow, 0).toString();
            name = tableModel.getValueAt(modelRow, 1).toString();
            unit = tableModel.getValueAt(modelRow, 5).toString();
            currentPrice = tableModel.getValueAt(modelRow, 6).toString();
        } else {
            String searchName = medicineNameField.getText().trim();
            Medicine foundMedicine = null;
            
            for (Medicine med : medicines) {
                if (TextUtils.removeAccents(med.getName().toLowerCase()).equals(TextUtils.removeAccents(searchName.toLowerCase()))) {
                    foundMedicine = med;
                    break;
                }
            }
            
            if (foundMedicine == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy ID cho thuốc: " + searchName + ". Vui lòng chọn từ bảng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            medicineId = foundMedicine.getId();
            name = foundMedicine.getName(); 
            unit = foundMedicine.getUnit(); 
            currentPrice = foundMedicine.getSellingPrice();
        }

        int quantity = (Integer) quantitySpinner.getValue();
        int morningVal = (Integer) morningSpinner.getValue();
        int noonVal = (Integer) noonSpinner.getValue();
        int eveningVal = (Integer) eveningSpinner.getValue();
        String note = noteField.getText();
        String totalAmount = totalField.getText();

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
            medicinePrescription[i][0] = selectedTableModel.getValueAt(i, 0).toString();
            medicinePrescription[i][1] = selectedTableModel.getValueAt(i, 1).toString();
            medicinePrescription[i][2] = selectedTableModel.getValueAt(i, 2).toString();
            medicinePrescription[i][3] = selectedTableModel.getValueAt(i, 3).toString();
            medicinePrescription[i][4] = selectedTableModel.getValueAt(i, 4).toString();
            medicinePrescription[i][5] = selectedTableModel.getValueAt(i, 5).toString();
            medicinePrescription[i][6] = selectedTableModel.getValueAt(i, 6).toString();
            medicinePrescription[i][7] = selectedTableModel.getValueAt(i, 7).toString();
            medicinePrescription[i][8] = selectedTableModel.getValueAt(i, 8).toString();
            medicinePrescription[i][9] = selectedTableModel.getValueAt(i, 9).toString();
        }
    }

    @Override
    public void dispose() {
        logger.info("Cleaning up MedicineDialog listeners");
        ClientHandler.deleteListener(GetMedInfoResponse.class);
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Medicine Dialog");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
        JButton openDialogButton = new JButton("Open Medicine Dialog");
        openDialogButton.addActionListener(e -> {
            MedicineDialog dialog = new MedicineDialog(frame);
            dialog.setVisible(true);
                String[][] prescription = dialog.getMedicinePrescription();
                if (prescription != null && prescription.length > 0) {
                    System.out.println("Prescription obtained:");
                    for (String[] item : prescription) {
                        System.out.println(String.format("ID:%s, Name:%s, Qty:%s, Unit:%s, M:%s, N:%s, E:%s, Price:%s, Total:%s, Note:%s", 
                                (Object[]) item));
                    }
                } else {
                    System.out.println("No prescription or dialog was cancelled.");
                }
            });
            frame.setLayout(new FlowLayout());
            frame.add(openDialogButton);
            frame.setVisible(true);
        });
    }
}
