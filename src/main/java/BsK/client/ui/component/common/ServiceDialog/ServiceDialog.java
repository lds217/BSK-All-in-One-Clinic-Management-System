package BsK.client.ui.component.common.ServiceDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Service;
import BsK.common.packet.req.GetSerInfoRequest;
import BsK.common.packet.res.GetSerInfoResponse;
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
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class ServiceDialog extends JDialog {
    private JTextField serviceNameField;
    private JSpinner quantitySpinner;
    private JTextField priceField;
    private JTextField totalField;
    private JTextArea noteField;
    private DefaultTableModel tableModel, selectedTableModel;
    private String[] serviceColumns = {"ID", "Tên dịch vụ", "Giá (VNĐ)"};
    private String[][] serviceData;
    private final ResponseListener<GetSerInfoResponse> getSerInfoResponseListener = this::getSerInfoHandler;
    private TableColumnModel columnModel;
    private JTable chosenServiceTable;
    private JTable suggestionTable;
    private DefaultTableModel suggestionTableModel;
    private JPopupMenu suggestionPopup;
    private boolean isProgrammaticallySettingNameField = false;
    private boolean isSelectionLocked = false;

    private List<Service> services = new ArrayList<>();
    private List<Service> filteredServices = new ArrayList<>();
    private String[][] servicePrescription;
    private String[][] originalPrescription; // Store original data for comparison
    private static final Logger logger = LoggerFactory.getLogger(ServiceDialog.class);

    public String[][] getServicePrescription() {
        return servicePrescription;
    }

    void getSerInfoHandler(GetSerInfoResponse response) {
        logger.info("Received service data");
        serviceData = response.getSerInfo();
        
        // Get Service objects from the response
        services = response.getServices();
        
        // We don't need to populate a main table anymore, just have the data ready.
    }

    void sendGetSerInfoRequest() {
        logger.info("Sending GetSerInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetSerInfoRequest());
    }

    public ServiceDialog(Frame parent) {
        super(parent, "Thêm dịch vụ", true);
        this.servicePrescription = new String[0][0];
        this.originalPrescription = new String[0][0];
        initUI(parent);
    }
    
    public ServiceDialog(Frame parent, String[][] existingPrescription) {
        super(parent, "Thêm dịch vụ", true);
        this.servicePrescription = existingPrescription != null ? existingPrescription : new String[0][0];
        this.originalPrescription = deepCopyArray(existingPrescription);
        initUI(parent);
        if (existingPrescription != null) {
            for (String[] row : existingPrescription) {
                selectedTableModel.addRow(row);
            }
        }
    }

    private void initUI(Frame parent) {
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

        // Add response listener and send request for service data
        ClientHandler.addResponseListener(GetSerInfoResponse.class, getSerInfoResponseListener);
        sendGetSerInfoRequest();

        setLayout(new BorderLayout(10, 10));

        JPanel mainInputPanel = new JPanel(new GridBagLayout());
        mainInputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.insets = new Insets(5, 5, 5, 5);
        mainGbc.weightx = 1.0;

        JPanel serviceInfoPanel = new JPanel(new GridBagLayout());
        serviceInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin dịch vụ",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainInputPanel.add(serviceInfoPanel, mainGbc);

        JPanel notePanel = new JPanel(new GridBagLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Ghi chú",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        mainInputPanel.add(notePanel, mainGbc);

        JPanel addServiceButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addServiceButton = new JButton("<html>Thêm dịch vụ <font color='red'>(F1)</font></html>");
        addServiceButton.setFont(labelFont);
        addServiceButton.setPreferredSize(new Dimension(220, 40));
        addServiceButtonPanel.add(addServiceButton);
        mainGbc.gridy = 2;
        mainInputPanel.add(addServiceButtonPanel, mainGbc);

        mainGbc.gridy = 3;
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
        JLabel nameLabel = new JLabel("Tên dịch vụ:");
        nameLabel.setFont(labelFont);
        serviceInfoPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        serviceNameField = new JTextField(20);
        serviceNameField.setFont(textFont);
        serviceNameField.setPreferredSize(textFieldSize);
        serviceInfoPanel.add(serviceNameField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel quantityLabel = new JLabel("Số lượng:");
        quantityLabel.setFont(labelFont);
        serviceInfoPanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setFont(textFont);
        quantitySpinner.setPreferredSize(new Dimension(80, 30));
        serviceInfoPanel.add(quantitySpinner, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 2;
        JLabel priceLabel = new JLabel("Đơn giá (VNĐ):");
        priceLabel.setFont(labelFont);
        serviceInfoPanel.add(priceLabel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        priceField = new JTextField(10);
        priceField.setFont(textFont);
        priceField.setPreferredSize(textFieldSize);
        priceField.setEditable(false);
        serviceInfoPanel.add(priceField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        JLabel totalLabel = new JLabel("Thành tiền (VNĐ):");
        totalLabel.setFont(labelFont);
        serviceInfoPanel.add(totalLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        totalField = new JTextField(10);
        totalField.setFont(textFont);
        totalField.setPreferredSize(textFieldSize);
        totalField.setEditable(false);
        serviceInfoPanel.add(totalField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel noteLabel = new JLabel("Ghi chú:");
        noteLabel.setFont(labelFont);
        notePanel.add(noteLabel, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
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

        tableModel = new DefaultTableModel(serviceData, serviceColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        // serviceTable = new JTable(tableModel); // This line is removed
        // serviceTable.setFont(new Font("Arial", Font.PLAIN, 14)); // This line is removed
        // serviceTable.setRowHeight(25); // This line is removed
        // serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // This line is removed
        // serviceTable.setAutoCreateRowSorter(true); // This line is removed
        // JScrollPane serviceTableScrollPane = new JScrollPane(serviceTable); // This line is removed
        // serviceTableScrollPane.setPreferredSize(new Dimension(550, 250)); // This line is removed

        suggestionTableModel = new DefaultTableModel(new String[0][0], serviceColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        suggestionTable = new JTable(suggestionTableModel);
        suggestionTable.setFont(textFont);
        suggestionTable.setRowHeight(30);
        suggestionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionTable.setAutoCreateRowSorter(false); // We filter manually
        JScrollPane suggestionScrollPane = new JScrollPane(suggestionTable);

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setLayout(new BorderLayout());
        suggestionPopup.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionPopup.setFocusable(false);

        String[] selectedColumnNames = {"ID", "Tên dịch vụ", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"};
        selectedTableModel = new DefaultTableModel(new String[][]{}, selectedColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 5;
            }
        };
        chosenServiceTable = new JTable(selectedTableModel);
        chosenServiceTable.setFont(textFont);
        chosenServiceTable.setRowHeight(30);
        JScrollPane selectedTableScrollPane = new JScrollPane(chosenServiceTable);
        selectedTableScrollPane.setPreferredSize(new Dimension(550, 150));

        JPanel removeServiceButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeServiceButton = new JButton("Xóa dịch vụ đã chọn");
        removeServiceButton.setFont(labelFont);
        removeServiceButtonPanel.add(removeServiceButton);

        JPanel chosenServicesPanel = new JPanel(new BorderLayout());
        chosenServicesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Dịch vụ đã chọn",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(220, 20, 60)
        ));
        chosenServicesPanel.setBackground(new Color(255, 240, 240)); // Light red background
        selectedTableScrollPane.setBackground(new Color(255, 240, 240)); // Light red background
        chosenServicesPanel.add(selectedTableScrollPane, BorderLayout.CENTER);
        chosenServicesPanel.add(removeServiceButtonPanel, BorderLayout.SOUTH);

        JScrollPane mainInputScrollPane = new JScrollPane(mainInputPanel);
        mainInputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainInputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainInputScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainInputScrollPane, chosenServicesPanel);
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

        // F1 Key binding for adding service
        Action addAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addSelectedService();
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "addServiceAction");
        getRootPane().getActionMap().put("addServiceAction", addAction);

        serviceNameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isSelectionLocked = false;
                showOrUpdateSuggestions();
            }
        });

        serviceNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingNameField) {
                    isSelectionLocked = false;
                    SwingUtilities.invokeLater(() -> showOrUpdateSuggestions()); 
                }
            }
            public void removeUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingNameField) {
                    isSelectionLocked = false;
                    SwingUtilities.invokeLater(() -> showOrUpdateSuggestions()); 
                }
            }
            public void insertUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingNameField) {
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

                    handleServiceTableRowSelection(selectedRow); // Populate fields on any click
                    suggestionPopup.setVisible(false); // Close popup on single click
                    
                    if (e.getClickCount() == 2) {
                        addSelectedService();
                    }
                });
            }
        });

        serviceNameField.addKeyListener(new KeyAdapter() {
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
                                handleServiceTableRowSelection(suggestionTable.getSelectedRow());
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

        addServiceButton.addActionListener(e -> addSelectedService());
        removeServiceButton.addActionListener(e -> removeSelectedService());

        noteField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && e.getModifiersEx() == 0) {
                    e.consume();
                    addSelectedService();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
                    e.consume();
                    quantitySpinner.requestFocusInWindow();
                }
            }
        });

        okButton.addActionListener(e -> {
            collectPrescriptionData();
            
            // Check if there are changes compared to the original prescription
            boolean hasChanges = hasDataChanged();
            
            if (!hasChanges) {
                // No changes made, allow closing without warning
                logger.info("No changes made to service prescription.");
                dispose();
                return;
            }
            
            // If there are changes but no items selected, show a confirmation
            if (servicePrescription == null || servicePrescription.length == 0) {
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Bạn đã xóa tất cả dịch vụ khỏi đơn. Bạn có muốn lưu thay đổi này không?", 
                    "Xác nhận", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            logger.info("Service prescription saved.");
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
        String filterText = serviceNameField.getText().trim();
        if (services == null || services.isEmpty()) {
            return;
        }

        if (filterText.isEmpty() && !serviceNameField.isFocusOwner()) {
            suggestionPopup.setVisible(false);
            return;
        }

        filteredServices.clear();
        List<String[]> filteredDisplayData = new ArrayList<>();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        for (Service service : services) {
            if (filterText.isEmpty() || TextUtils.removeAccents(service.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                filteredServices.add(service);
                filteredDisplayData.add(service.toStringArray());
            }
        }

        suggestionTableModel.setDataVector(filteredDisplayData.toArray(new String[0][0]), serviceColumns);
        resizeSuggestionTableColumns();

        if (!filteredDisplayData.isEmpty()) {
            suggestionTable.setRowSelectionInterval(0, 0);
            
            // Set preferred size for the popup's scroll pane
            JScrollPane scrollPane = (JScrollPane) suggestionPopup.getComponent(0);
            int headerHeight = suggestionTable.getTableHeader().getPreferredSize().height;
            int rowsHeight = suggestionTable.getRowCount() * suggestionTable.getRowHeight();
            int height = Math.min(rowsHeight, 200) + headerHeight;
            int width = serviceNameField.getWidth();
            scrollPane.setPreferredSize(new Dimension(width, height));
            
            suggestionPopup.pack();
            suggestionPopup.show(serviceNameField, 0, serviceNameField.getHeight());
            serviceNameField.requestFocusInWindow();
        } else {
            suggestionPopup.setVisible(false);
        }
    }

    private void resizeSuggestionTableColumns() {
        columnModel = suggestionTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(100);
    }

    private void handleServiceTableRowSelection(int viewRow) {
        if (viewRow < 0 || viewRow >= filteredServices.size()) {
            clearInputFields();
            return;
        }
        Service selectedService = filteredServices.get(viewRow);
        populateFieldsFromService(selectedService);
    }

    private void handleExactMatch() {
        String searchName = serviceNameField.getText().trim();
        if (searchName.isEmpty()) {
            return;
        }

        Service foundService = null;
        String normalizedSearchName = TextUtils.removeAccents(searchName.toLowerCase());

        for (Service service : services) {
            if (TextUtils.removeAccents(service.getName().toLowerCase()).equals(normalizedSearchName)) {
                foundService = service;
                break;
            }
        }

        if (foundService != null) {
            populateFieldsFromService(foundService);
            quantitySpinner.requestFocusInWindow();
        }
    }

    private void populateFieldsFromService(Service service) {
        if (service == null) {
            clearInputFields();
            return;
        }

        isProgrammaticallySettingNameField = true;
        serviceNameField.setText(service.getName());
        isProgrammaticallySettingNameField = false;

        priceField.setText(service.getCost());
        quantitySpinner.setValue(1);
        noteField.setText("");
        updateTotalField();
        isSelectionLocked = true;
    }

    private void clearInputFields(){
        priceField.setText("");
        totalField.setText("");
        quantitySpinner.setValue(1);
        noteField.setText("");
    }

    private void updateTotalField() {
        try {
            int quantity = (Integer) quantitySpinner.getValue();
            if (quantity < 0) {
                quantitySpinner.setValue(0);
                quantity = 0;
            }
            double price = 0;
            if (!priceField.getText().isEmpty()) {
                price = Double.parseDouble(priceField.getText());
            }
            totalField.setText(String.format("%.0f", quantity * price));
        } catch (NumberFormatException e) {
            totalField.setText("Giá không hợp lệ");
            logger.error("Error parsing price for total: " + priceField.getText(), e);
        }
    }

    private void addSelectedService() {
        int selectedRowInSuggestionTable = suggestionTable.getSelectedRow();
        String searchName = serviceNameField.getText().trim();

        if (searchName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ.", "Chưa chọn dịch vụ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serviceId, name, currentPrice;

        if (suggestionPopup.isVisible() && selectedRowInSuggestionTable != -1) {
            Service selectedService = filteredServices.get(suggestionTable.convertRowIndexToModel(selectedRowInSuggestionTable));
            serviceId = selectedService.getId();
            name = selectedService.getName();
            currentPrice = selectedService.getCost();
        } else {
            Service foundService = null;
            String normalizedSearchName = TextUtils.removeAccents(searchName.toLowerCase());

            for (Service service : services) {
                if (TextUtils.removeAccents(service.getName().toLowerCase()).equals(normalizedSearchName)) {
                    foundService = service;
                    break;
                }
            }
            
            if (foundService == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy dịch vụ khớp với: '" + searchName + "'. Vui lòng chọn từ danh sách gợi ý.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            serviceId = foundService.getId();
            name = foundService.getName(); 
            currentPrice = foundService.getCost();
        }

        int quantity = (Integer) quantitySpinner.getValue();
        String note = noteField.getText();
        String totalAmount = totalField.getText();

        if (quantity <= 0) {
            JOptionPane.showMessageDialog(this, "Số lượng phải lớn hơn 0.", "Số lượng không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        for (int i = 0; i < selectedTableModel.getRowCount(); i++) {
            if (selectedTableModel.getValueAt(i, 0).equals(serviceId)) {
                int existingQuantity = Integer.parseInt(selectedTableModel.getValueAt(i, 2).toString());
                int newQuantity = existingQuantity + quantity;
                double singlePrice = Double.parseDouble(selectedTableModel.getValueAt(i, 3).toString());
                selectedTableModel.setValueAt(newQuantity, i, 2);
                selectedTableModel.setValueAt(String.format("%.0f", newQuantity * singlePrice), i, 4);
                
                // Clear fields for next entry
                isProgrammaticallySettingNameField = true;
                serviceNameField.setText("");
                isProgrammaticallySettingNameField = false;
                clearInputFields();
                serviceNameField.requestFocusInWindow();
                return;
            }
        }
        selectedTableModel.addRow(new Object[]{serviceId, name, quantity, currentPrice, totalAmount, note});
        
        // Clear fields for next entry
        isProgrammaticallySettingNameField = true;
        serviceNameField.setText("");
        isProgrammaticallySettingNameField = false;
        clearInputFields();
        serviceNameField.requestFocusInWindow();
    }

    private void removeSelectedService() {
        int selectedRow = chosenServiceTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedTableModel.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ từ bảng dưới để xóa.", "Chưa chọn dịch vụ", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void collectPrescriptionData() {
        int rowCount = selectedTableModel.getRowCount();
        List<String[]> prescriptionList = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            String[] rowData = new String[selectedTableModel.getColumnCount()];
            for (int j = 0; j < selectedTableModel.getColumnCount(); j++) {
                Object value = selectedTableModel.getValueAt(i, j);
                rowData[j] = (value != null) ? value.toString() : "";
            }
            prescriptionList.add(rowData);
        }
        this.servicePrescription = prescriptionList.toArray(new String[0][]);
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
        if (originalPrescription.length != servicePrescription.length) {
            return true;
        }
        
        // If both are empty, no changes
        if (originalPrescription.length == 0 && servicePrescription.length == 0) {
            return false;
        }
        
        // Compare each row
        for (int i = 0; i < originalPrescription.length; i++) {
            if (originalPrescription[i].length != servicePrescription[i].length) {
                return true;
            }
            for (int j = 0; j < originalPrescription[i].length; j++) {
                String original = originalPrescription[i][j];
                String current = servicePrescription[i][j];
                if (!java.util.Objects.equals(original, current)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public void dispose() {
        logger.info("Cleaning up ServiceDialog listeners");
        ClientHandler.deleteListener(GetSerInfoResponse.class);
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Service Dialog");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);

            JButton openDialogButton = new JButton("Open Service Dialog");
            openDialogButton.addActionListener(e -> {
                ServiceDialog dialog = new ServiceDialog(frame);
                dialog.setVisible(true);
                String[][] prescription = dialog.getServicePrescription();
                if (prescription != null && prescription.length > 0) {
                    System.out.println("Service Prescription obtained:");
                    for (String[] item : prescription) {
                        System.out.println(String.join(", ", item));
                    }
                } else {
                    System.out.println("No service prescription or dialog was cancelled.");
                }
            });

            frame.setLayout(new FlowLayout());
            frame.add(openDialogButton);
            frame.setVisible(true);
        });
    }
}
