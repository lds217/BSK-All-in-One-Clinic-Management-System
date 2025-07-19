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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

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
    private String[] suggestionColumns = {"Tên thuốc", "Tồn kho", "ĐVT", "Giá"};
    private String[][] medicineData;
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::getMedInfoHandler;
    private TableColumnModel columnModel;
    private HashMap<String, Boolean> selectedMedicine = new HashMap<>();
    private JTable chosenMedicineTable;
    private JTable suggestionTable;
    private DefaultTableModel suggestionTableModel;
    private JPopupMenu suggestionPopup;
    private boolean isProgrammaticallySettingMedicineNameField = false;
    private boolean isSelectionLocked = false;

    private List<Medicine> medicines = new ArrayList<>();
    private List<Medicine> filteredMedicines = new ArrayList<>();
    private String[][] medicinePrescription;
    private String[][] originalPrescription; // Store original data for comparison
    private static final Logger logger = LoggerFactory.getLogger(MedicineDialog.class);

    public String[][] getMedicinePrescription() {
        return medicinePrescription;
    }

    void getMedInfoHandler(GetMedInfoResponse response) {
        logger.info("Received medicine data");
        medicineData = response.getMedInfo();
        
        // Get Medicine objects from the response
        medicines = response.getMedicines();
        
        // We don't need to populate a main table anymore, just have the data ready.
    }

    private void sendGetMedInfoRequest() {
        logger.info("Sending GetMedInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
    }

    public MedicineDialog(final Frame parent) {
        super(parent, "Đơn thuốc", true);
        this.medicinePrescription = new String[0][0];
        this.originalPrescription = new String[0][0];
        init(parent);
    }
    
    public MedicineDialog(final Frame parent, String[][] existingPrescription) {
        super(parent, "Đơn thuốc", true);
        this.medicinePrescription = existingPrescription != null ? existingPrescription : new String[0][0];
        this.originalPrescription = deepCopyArray(existingPrescription);
        init(parent);
        if (existingPrescription != null) {
            for (String[] row : existingPrescription) {
                selectedTableModel.addRow(row);
            }
        }
    }

    private void init(final Frame parent) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(parent);
        setResizable(true);
        
        // UI Fonts and Dimensions
        Font labelFont = new Font("Arial", Font.BOLD, 15);
        Font textFont = new Font("Arial", Font.PLAIN, 15);
        Font titleFont = new Font("Arial", Font.BOLD, 16);
        Dimension textFieldSize = new Dimension(100, 30);
        
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
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainInputPanel.add(medicineInfoPanel, mainGbc);

        JPanel quantityPricePanel = new JPanel(new GridBagLayout());
        quantityPricePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Số lượng & Giá",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        mainInputPanel.add(quantityPricePanel, mainGbc);

        JPanel dosagePanel = new JPanel(new GridBagLayout());
        dosagePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Liều dùng",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridy = 2;
        mainInputPanel.add(dosagePanel, mainGbc);

        JPanel notePanel = new JPanel(new GridBagLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Ghi chú",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridy = 3;
        mainInputPanel.add(notePanel, mainGbc);

        JPanel addMedicineButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addMedicineButton = new JButton("<html>Thêm thuốc <font color='red'>(F1)</font></html>");
        addMedicineButton.setFont(labelFont);
        addMedicineButton.setPreferredSize(new Dimension(200, 40));
        addMedicineButtonPanel.add(addMedicineButton);
        mainGbc.gridy = 4;
        mainInputPanel.add(addMedicineButtonPanel, mainGbc);

        mainGbc.gridy = 5;
        mainGbc.weighty = 1.0;
        mainInputPanel.add(new JPanel(), mainGbc);

        // Add Escape key listener to close dialog
        JRootPane rootPane = this.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE");
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

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

        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel quantityLabel = new JLabel("Số lượng:");
        quantityLabel.setFont(labelFont);
        quantityPricePanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
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
        quantityLeftField = new JTextField(5);
        quantityLeftField.setFont(textFont);
        quantityLeftField.setPreferredSize(textFieldSize);
        quantityLeftField.setEditable(false);
        quantityPricePanel.add(quantityLeftField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel unitLabel = new JLabel("ĐVT:");
        unitLabel.setFont(labelFont);
        quantityPricePanel.add(unitLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        String[] units = {"Viên", "Vỉ", "Hộp", "Chai", "Tuýp", "Gói"};
        UnitComboBox = new JComboBox<>(units);
        UnitComboBox.setFont(textFont);
        UnitComboBox.setPreferredSize(textFieldSize);
        UnitComboBox.setEnabled(false);
        quantityPricePanel.add(UnitComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JLabel priceLabel = new JLabel("Giá (VNĐ):");
        priceLabel.setFont(labelFont);
        quantityPricePanel.add(priceLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        priceField = new JTextField(10);
        priceField.setFont(textFont);
        priceField.setPreferredSize(textFieldSize);
        priceField.setEditable(false);
        quantityPricePanel.add(priceField, gbc);
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

        SpinnerNumberModel dosageModelTemplate = new SpinnerNumberModel(0, 0, 10, 1);
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

        suggestionTableModel = new DefaultTableModel(new String[0][0], suggestionColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        suggestionTable = new JTable(suggestionTableModel);
        suggestionTable.setFont(new Font("Arial", Font.PLAIN, 15));
        suggestionTable.setRowHeight(30);
        suggestionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionTable.setAutoCreateRowSorter(false); // We filter manually
        JScrollPane suggestionScrollPane = new JScrollPane(suggestionTable);

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setLayout(new BorderLayout());
        suggestionPopup.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionPopup.setFocusable(false);


        String[] selectedColumnNames = {"ID", "Tên thuốc", "SL", "ĐVT", "Sáng", "Trưa", "Chiều", "Đơn giá", "Thành tiền", "Ghi chú"};
        selectedTableModel = new DefaultTableModel(new String[][]{}, selectedColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 4 || column == 5 || column == 6 || column == 9;
            }
        };
        chosenMedicineTable = new JTable(selectedTableModel);
        chosenMedicineTable.setFont(new Font("Arial", Font.PLAIN, 15));
        chosenMedicineTable.setRowHeight(30);
        JScrollPane selectedTableScrollPane = new JScrollPane(chosenMedicineTable);
        selectedTableScrollPane.setPreferredSize(new Dimension(600, 150));

        JPanel removeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeMedicineButton = new JButton("Xóa thuốc đã chọn");
        removeMedicineButton.setFont(labelFont);
        removeButtonPanel.add(removeMedicineButton);

        JPanel chosenMedicinesPanel = new JPanel(new BorderLayout());
        chosenMedicinesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thuốc đã chọn",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(220, 20, 60)
        ));
        chosenMedicinesPanel.setBackground(new Color(255, 240, 240)); // Light red background
        selectedTableScrollPane.setBackground(new Color(255, 240, 240)); // Light red background
        chosenMedicinesPanel.add(selectedTableScrollPane, BorderLayout.CENTER);
        chosenMedicinesPanel.add(removeButtonPanel, BorderLayout.SOUTH);

        JScrollPane mainInputScrollPane = new JScrollPane(mainInputPanel);
        mainInputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainInputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainInputScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainInputScrollPane, chosenMedicinesPanel);
        splitPane.setResizeWeight(0.35);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Lưu");
        okButton.setFont(labelFont);
        JButton cancelButton = new JButton("Hủy");
        cancelButton.setFont(labelFont);
        bottomButtonPanel.add(okButton);
        bottomButtonPanel.add(cancelButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);

        // F1 Key binding for adding medicine
        Action addAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelectedMedicine();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "addMedicineAction");
        getRootPane().getActionMap().put("addMedicineAction", addAction);

        medicineNameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isSelectionLocked = false;
                showOrUpdateSuggestions();
            }
        });

        medicineNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingMedicineNameField) {
                    isSelectionLocked = false;
                    SwingUtilities.invokeLater(() -> showOrUpdateSuggestions()); 
                }
            }
            public void removeUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingMedicineNameField) {
                    isSelectionLocked = false;
                    SwingUtilities.invokeLater(() -> showOrUpdateSuggestions()); 
                }
            }
            public void insertUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingMedicineNameField) {
                    isSelectionLocked = false;
                    SwingUtilities.invokeLater(() -> showOrUpdateSuggestions()); 
                }
            }
        });

        suggestionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int selectedRow = suggestionTable.getSelectedRow();
                    if (selectedRow == -1) return;

                    handleMedicineTableRowSelection(selectedRow); // Populate fields on any click

                    if (e.getClickCount() == 2) {
                        addSelectedMedicine();
                        suggestionPopup.setVisible(false);
                    }
                });
            }
        });

        medicineNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (suggestionPopup.isVisible()) {
                    int selectedRow = suggestionTable.getSelectedRow();
                    int rowCount = suggestionTable.getRowCount();

                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            if (rowCount > 0) {
                                selectedRow = (selectedRow + 1) % rowCount;
                                suggestionTable.setRowSelectionInterval(selectedRow, selectedRow);
                                suggestionTable.scrollRectToVisible(suggestionTable.getCellRect(selectedRow, 0, true));
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_UP:
                            if (rowCount > 0) {
                                selectedRow = (selectedRow - 1 + rowCount) % rowCount;
                                suggestionTable.setRowSelectionInterval(selectedRow, selectedRow);
                                suggestionTable.scrollRectToVisible(suggestionTable.getCellRect(selectedRow, 0, true));
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ENTER:
                            if (selectedRow != -1) {
                                handleMedicineTableRowSelection(suggestionTable.getSelectedRow());
                                suggestionPopup.setVisible(false);
                                quantitySpinner.requestFocusInWindow();
                            }
                            e.consume();
                            break;
                        case KeyEvent.VK_ESCAPE:
                            suggestionPopup.setVisible(false);
                            e.consume();
                            break;
                    }
                } else {
                     if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        handleExactMatch();
                    }
                }
            }
        });

        quantitySpinner.addChangeListener(e -> updateTotalField());
        UnitComboBox.addActionListener(e -> updateTotalField());

        addMedicineButton.addActionListener(e -> addSelectedMedicine());
        removeMedicineButton.addActionListener(e -> removeSelectedMedicine());

        noteField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && e.getModifiersEx() == 0) {
                    e.consume();
                    addSelectedMedicine();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
                    e.consume();
                    eveningSpinner.requestFocusInWindow();
                }
            }
        });

        okButton.addActionListener(e -> {
            collectPrescriptionData();
            
            // Check if there are changes compared to the original prescription
            boolean hasChanges = hasDataChanged();
            
            if (!hasChanges) {
                // No changes made, allow closing without warning
                logger.info("No changes made to prescription.");
                dispose();
                return;
            }
            
            // If there are changes but no items selected, show a confirmation
            if (medicinePrescription == null || medicinePrescription.length == 0) {
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Bạn đã xóa tất cả thuốc khỏi đơn. Bạn có muốn lưu thay đổi này không?", 
                    "Xác nhận", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            logger.info("Prescription saved.");
            if (medicinePrescription != null) {
                for (String[] row : medicinePrescription) {
                    logger.info("Med: ID={}, Name={}, Qty={}, Unit={}, M={}, N={}, E={}, Price={}, Total={}, Note={}", 
                            row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8], row[9]);
                }
            }
            dispose();
        });

        cancelButton.addActionListener(e -> {
            dispose();
        });
    }

    private void showOrUpdateSuggestions() {
        if (isSelectionLocked) {
            return;
        }
        String filterText = medicineNameField.getText().trim();
        if (medicines == null || medicines.isEmpty()) {
            return;
        }

        if (filterText.isEmpty() && !medicineNameField.isFocusOwner()) {
            suggestionPopup.setVisible(false);
            return;
        }

        filteredMedicines.clear();
        List<String[]> filteredDisplayData = new ArrayList<>();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        for (Medicine med : medicines) {
            if (filterText.isEmpty() || TextUtils.removeAccents(med.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                filteredMedicines.add(med);
                filteredDisplayData.add(new String[]{
                        med.getName(),
                        med.getQuantity(),
                        med.getUnit(),
                        med.getSellingPrice()
                });
            }
        }

        suggestionTableModel.setDataVector(filteredDisplayData.toArray(new String[0][0]), suggestionColumns);
        resizeSuggestionTableColumns();

        if (!filteredDisplayData.isEmpty()) {
            suggestionTable.setRowSelectionInterval(0, 0);
            
            // Set preferred size for the popup's scroll pane
            JScrollPane scrollPane = (JScrollPane) suggestionPopup.getComponent(0);
            int headerHeight = suggestionTable.getTableHeader().getPreferredSize().height;
            int rowsHeight = suggestionTable.getRowCount() * suggestionTable.getRowHeight();
            int height = Math.min(rowsHeight, 200) + headerHeight;
            int width = medicineNameField.getWidth();
            scrollPane.setPreferredSize(new Dimension(width, height));
            
            suggestionPopup.pack();
            suggestionPopup.show(medicineNameField, 0, medicineNameField.getHeight());
            medicineNameField.requestFocusInWindow();
        } else {
            suggestionPopup.setVisible(false);
            // clearInputFields(); // Don't clear fields if the user is typing
        }
    }

    private void resizeSuggestionTableColumns() {
        columnModel = suggestionTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(200); // Tên thuốc
        columnModel.getColumn(1).setPreferredWidth(50);  // Tồn kho
        columnModel.getColumn(2).setPreferredWidth(50);  // ĐVT
        columnModel.getColumn(3).setPreferredWidth(80);  // Giá
    }
    
    private void handleMedicineTableRowSelection(int viewRow) {
        if (viewRow < 0 || viewRow >= filteredMedicines.size()) {
            clearInputFields();
            return;
        }
        Medicine selectedMedicine = filteredMedicines.get(viewRow);
        populateFieldsFromMedicine(selectedMedicine);
    }

    private void handleExactMatch() {
        String searchName = medicineNameField.getText().trim();
        if (searchName.isEmpty()) {
            return;
        }

        Medicine foundMedicine = null;
        String normalizedSearchName = TextUtils.removeAccents(searchName.toLowerCase());

        for (Medicine med : medicines) {
            if (TextUtils.removeAccents(med.getName().toLowerCase()).equals(normalizedSearchName)) {
                foundMedicine = med;
                break;
            }
        }

        if (foundMedicine != null) {
            populateFieldsFromMedicine(foundMedicine);
            quantitySpinner.requestFocusInWindow();
        }
    }

    private void populateFieldsFromMedicine(Medicine medicine) {
        if (medicine == null) {
            clearInputFields();
            return;
        }

        isProgrammaticallySettingMedicineNameField = true;
        medicineNameField.setText(medicine.getName());
        isProgrammaticallySettingMedicineNameField = false;

        medicineCompanyField.setText(medicine.getCompany());
        medicineDescriptionField.setText(medicine.getDescription());
        quantityLeftField.setText(medicine.getQuantity());
        UnitComboBox.setSelectedItem(medicine.getUnit());
        priceField.setText(medicine.getSellingPrice());

        quantitySpinner.setValue(1);

        String preferenceNote = medicine.getPreferenceNote();
        if (preferenceNote != null && !preferenceNote.trim().isEmpty()) {
            String[] parts = preferenceNote.split(",", 4);
            try {
                morningSpinner.setValue(parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0);
                noonSpinner.setValue(parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0);
                eveningSpinner.setValue(parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0);
                noteField.setText(parts.length > 3 ? parts[3].trim() : "");
            } catch (NumberFormatException e) {
                logger.error("Could not parse preference note: {}", preferenceNote, e);
                // Reset to default values in case of parsing error
                morningSpinner.setValue(0);
                noonSpinner.setValue(0);
                eveningSpinner.setValue(0);
                noteField.setText("");
            }
        } else {
            // No preference note, set default values
            morningSpinner.setValue(0);
            noonSpinner.setValue(0);
            eveningSpinner.setValue(0);
            noteField.setText("");
        }
        
        updateTotalField();
        isSelectionLocked = true;
    }

    private void clearInputFields() {
        // Don't clear the name field as the user is typing in it
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
        int selectedRowInSuggestionTable = suggestionTable.getSelectedRow();
        
        String medicineId, name, unit, currentPrice;
        String searchName = medicineNameField.getText().trim();

        if (searchName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một thuốc.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Prioritize selection from the popup table if it's open and has a selection
        if (suggestionPopup.isVisible() && selectedRowInSuggestionTable != -1) {
            Medicine selectedMedicine = filteredMedicines.get(suggestionTable.convertRowIndexToModel(selectedRowInSuggestionTable));
            medicineId = selectedMedicine.getId();
            name = selectedMedicine.getName();
            unit = selectedMedicine.getUnit();
            currentPrice = selectedMedicine.getSellingPrice();
        } else {
            // Otherwise, try to find an exact match from the text field
            Medicine foundMedicine = null;
            String normalizedSearchName = TextUtils.removeAccents(searchName.toLowerCase());

            for (Medicine med : medicines) {
                if (TextUtils.removeAccents(med.getName().toLowerCase()).equals(normalizedSearchName)) {
                    foundMedicine = med;
                    break;
                }
            }
            
            if (foundMedicine == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thuốc khớp với: '" + searchName + "'. Vui lòng chọn từ danh sách gợi ý.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
        
        // Clear fields for next entry
        isProgrammaticallySettingMedicineNameField = true;
        medicineNameField.setText("");
        isProgrammaticallySettingMedicineNameField = false;
        clearInputFields();
        medicineNameField.requestFocusInWindow();
    }

    private void removeSelectedMedicine() {
            int selectedRow = chosenMedicineTable.getSelectedRow();
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

    private String[][] deepCopyArray(String[][] original) {
        if (original == null) {
            return new String[0][0];
        }
        String[][] copy = new String[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) {
                copy[i] = new String[original[i].length];
                System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
            }
        }
        return copy;
    }

    private boolean hasDataChanged() {
        collectPrescriptionData();
        
        // If lengths are different, there's a change
        if (originalPrescription.length != medicinePrescription.length) {
            return true;
        }
        
        // If both are empty, no changes
        if (originalPrescription.length == 0 && medicinePrescription.length == 0) {
            return false;
        }
        
        // Compare each row
        for (int i = 0; i < originalPrescription.length; i++) {
            if (originalPrescription[i].length != medicinePrescription[i].length) {
                return true;
            }
            for (int j = 0; j < originalPrescription[i].length; j++) {
                String original = originalPrescription[i][j];
                String current = medicinePrescription[i][j];
                if (!java.util.Objects.equals(original, current)) {
                    return true;
                }
            }
        }
        
        return false;
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
