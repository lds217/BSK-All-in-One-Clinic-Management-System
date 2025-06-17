package BsK.client.ui.component.CheckUpPage.ServiceDialog;

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
    private JTable serviceTable;
    private JTable selectedTable;
    private boolean isProgrammaticallySettingNameField = false;

    private List<Service> services = new ArrayList<>();
    private String[][] servicePrescription;
    private static final Logger logger = LoggerFactory.getLogger(ServiceDialog.class);

    public String[][] getServicePrescription() {
        return servicePrescription;
    }

    void getSerInfoHandler(GetSerInfoResponse response) {
        logger.info("Received service data");
        serviceData = response.getSerInfo();
        
        // Get Service objects from the response
        services = response.getServices();
        
        // Set the data vector with the raw string arrays for backward compatibility
        tableModel.setDataVector(serviceData, serviceColumns);
        resizeServiceTableColumns();
    }

    void sendGetSerInfoRequest() {
        logger.info("Sending GetSerInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetSerInfoRequest());
    }

    public ServiceDialog(Frame parent) {
        super(parent, "Thêm dịch vụ", true);
        this.servicePrescription = new String[0][0];
        initUI(parent);
    }
    
    public ServiceDialog(Frame parent, String[][] existingPrescription) {
        super(parent, "Thêm dịch vụ", true);
        this.servicePrescription = existingPrescription != null ? existingPrescription : new String[0][0];
        initUI(parent);
        if (existingPrescription != null) {
            for (String[] row : existingPrescription) {
                selectedTableModel.addRow(row);
            }
        }
    }

    private void initUI(Frame parent) {
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
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainInputPanel.add(serviceInfoPanel, mainGbc);

        JPanel notePanel = new JPanel(new GridBagLayout());
        notePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Ghi chú",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        mainInputPanel.add(notePanel, mainGbc);

        JPanel addServiceButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton addServiceButton = new JButton("Thêm dịch vụ");
        addServiceButtonPanel.add(addServiceButton);
        mainGbc.gridy = 2;
        mainInputPanel.add(addServiceButtonPanel, mainGbc);

        mainGbc.gridy = 3;
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
        serviceInfoPanel.add(new JLabel("Tên dịch vụ:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        serviceNameField = new JTextField(20);
        serviceInfoPanel.add(serviceNameField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        serviceInfoPanel.add(new JLabel("Số lượng:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        quantitySpinner.setPreferredSize(new Dimension(80, 25));
        serviceInfoPanel.add(quantitySpinner, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 2;
        serviceInfoPanel.add(new JLabel("Đơn giá (VNĐ):"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        priceField = new JTextField(10);
        priceField.setEditable(false);
        serviceInfoPanel.add(priceField, gbc);
        gbc.weightx = 0.0;

        gbc.gridy++;
        gbc.gridx = 0;
        serviceInfoPanel.add(new JLabel("Thành tiền (VNĐ):"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        totalField = new JTextField(10);
        totalField.setEditable(false);
        serviceInfoPanel.add(totalField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        notePanel.add(new JLabel("Ghi chú:"), gbc);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        noteField = new JTextArea(3, 20);
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
        serviceTable = new JTable(tableModel);
        serviceTable.setFont(new Font("Arial", Font.PLAIN, 14));
        serviceTable.setRowHeight(25);
        serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTable.setAutoCreateRowSorter(true);
        JScrollPane serviceTableScrollPane = new JScrollPane(serviceTable);
        serviceTableScrollPane.setPreferredSize(new Dimension(550, 250));

        String[] selectedColumnNames = {"ID", "Tên dịch vụ", "Số lượng", "Đơn giá", "Thành tiền", "Ghi chú"};
        selectedTableModel = new DefaultTableModel(new String[][]{}, selectedColumnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 5;
            }
        };
        selectedTable = new JTable(selectedTableModel);
        selectedTable.setFont(new Font("Arial", Font.PLAIN, 14));
        selectedTable.setRowHeight(25);
        JScrollPane selectedTableScrollPane = new JScrollPane(selectedTable);
        selectedTableScrollPane.setPreferredSize(new Dimension(550, 150));

        JPanel removeServiceButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeServiceButton = new JButton("Xóa dịch vụ đã chọn");
        removeServiceButtonPanel.add(removeServiceButton);

        // Create titled panels for right side sections
        JPanel availableServicesPanel = new JPanel(new BorderLayout());
        availableServicesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Dịch vụ có",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(63, 81, 181)
        ));
        availableServicesPanel.add(serviceTableScrollPane, BorderLayout.CENTER);
        
        JPanel chosenServicesPanel = new JPanel(new BorderLayout());
        chosenServicesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Dịch vụ đã chọn",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(220, 20, 60)
        ));
        chosenServicesPanel.setBackground(new Color(255, 240, 240)); // Light red background
        selectedTableScrollPane.setBackground(new Color(255, 240, 240)); // Light red background
        chosenServicesPanel.add(selectedTableScrollPane, BorderLayout.CENTER);
        chosenServicesPanel.add(removeServiceButtonPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(0,5));
        rightPanel.add(availableServicesPanel, BorderLayout.CENTER);
        rightPanel.add(chosenServicesPanel, BorderLayout.SOUTH);

        JScrollPane mainInputScrollPane = new JScrollPane(mainInputPanel);
        mainInputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainInputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainInputScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainInputScrollPane, rightPanel);
        splitPane.setResizeWeight(0.4);
        add(splitPane, BorderLayout.CENTER);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("Lưu");
        JButton cancelButton = new JButton("Hủy");
        bottomButtonPanel.add(okButton);
        bottomButtonPanel.add(cancelButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);

        serviceNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingNameField) {
                    SwingUtilities.invokeLater(() -> filterServiceTable()); 
                }
            }
            public void removeUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingNameField) {
                    SwingUtilities.invokeLater(() -> filterServiceTable()); 
                }
            }
            public void insertUpdate(DocumentEvent e) { 
                if (!isProgrammaticallySettingNameField) {
                    SwingUtilities.invokeLater(() -> filterServiceTable()); 
                }
            }
        });

        serviceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    SwingUtilities.invokeLater(() -> {
                        int selectedRow = serviceTable.getSelectedRow();
                        if (selectedRow != -1) {
                            handleServiceTableRowSelection(selectedRow);
                        }
                    });
                }
            }
        });

        serviceNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (serviceTable.getSelectedRow() != -1) {
                        addSelectedService();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (serviceTable.getRowCount() > 0) {
                        int selectedRow = serviceTable.getSelectedRow();
                        if (selectedRow < serviceTable.getRowCount() - 1) {
                            serviceTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                            serviceTable.scrollRectToVisible(serviceTable.getCellRect(selectedRow + 1, 0, true));
                            handleServiceTableRowSelection(selectedRow+1);
                        } else {
                            serviceTable.setRowSelectionInterval(0,0);
                            serviceTable.scrollRectToVisible(serviceTable.getCellRect(0,0,true));
                            handleServiceTableRowSelection(0);
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (serviceTable.getRowCount() > 0) {
                        int selectedRow = serviceTable.getSelectedRow();
                        if (selectedRow > 0) {
                            serviceTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                            serviceTable.scrollRectToVisible(serviceTable.getCellRect(selectedRow - 1, 0, true));
                             handleServiceTableRowSelection(selectedRow - 1);
                        } else {
                            serviceTable.setRowSelectionInterval(serviceTable.getRowCount()-1, serviceTable.getRowCount()-1);
                            serviceTable.scrollRectToVisible(serviceTable.getCellRect(serviceTable.getRowCount()-1,0,true));
                            handleServiceTableRowSelection(serviceTable.getRowCount()-1);
                        }
                    }
                }
            }
        });

        quantitySpinner.addChangeListener(e -> updateTotalField());

        addServiceButton.addActionListener(e -> addSelectedService());
        removeServiceButton.addActionListener(e -> removeSelectedService());

        okButton.addActionListener(e -> {
            collectPrescriptionData();
            if (servicePrescription == null || servicePrescription.length == 0) {
                JOptionPane.showMessageDialog(this, "Chưa có dịch vụ nào được chọn.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            logger.info("Service prescription saved.");
            dispose();
        });

        cancelButton.addActionListener(e -> {
            dispose();
        });
    }

    private void filterServiceTable() {
        String filterText = serviceNameField.getText().trim();
        if (serviceData == null) return;
        if (filterText.isEmpty()) {
            tableModel.setDataVector(serviceData, serviceColumns);
            resizeServiceTableColumns();
            serviceTable.clearSelection();
        } else {
            List<String[]> filteredData = new ArrayList<>();
            List<Service> filteredServices = new ArrayList<>();
            String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());
            
            for (Service service : services) {
                if (TextUtils.removeAccents(service.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                    filteredServices.add(service);
                    filteredData.add(service.toStringArray());
                }
            }
            
            tableModel.setDataVector(filteredData.toArray(new String[0][0]), serviceColumns);
            resizeServiceTableColumns();
            if (!filteredData.isEmpty()) {
                serviceTable.setRowSelectionInterval(0,0);
                handleServiceTableRowSelection(0);
            } else {
                clearInputFields();
            }
        }
    }

    private void resizeServiceTableColumns() {
        columnModel = serviceTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(30);
        columnModel.getColumn(1).setPreferredWidth(300);
        columnModel.getColumn(2).setPreferredWidth(100);
    }

    private void handleServiceTableRowSelection(int viewRow) {
        if (viewRow < 0 || viewRow >= serviceTable.getRowCount()) {
            clearInputFields();
            return;
        }
        int modelRow = serviceTable.convertRowIndexToModel(viewRow);
        
        isProgrammaticallySettingNameField = true;
        serviceNameField.setText(tableModel.getValueAt(modelRow, 1).toString());
        isProgrammaticallySettingNameField = false;
        
        priceField.setText(tableModel.getValueAt(modelRow, 2).toString());
        quantitySpinner.setValue(1);
        noteField.setText("");
        updateTotalField();
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
        int selectedRowInServiceTable = serviceTable.getSelectedRow();
        if (selectedRowInServiceTable == -1 && serviceNameField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ từ danh sách hoặc tìm kiếm.", "Chưa chọn dịch vụ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String serviceId, name, currentPrice;

        if (selectedRowInServiceTable != -1) {
            int modelRow = serviceTable.convertRowIndexToModel(selectedRowInServiceTable);
            serviceId = tableModel.getValueAt(modelRow, 0).toString();
            name = tableModel.getValueAt(modelRow, 1).toString();
            currentPrice = tableModel.getValueAt(modelRow, 2).toString();
        } else {
            String searchName = serviceNameField.getText().trim();
            Service foundService = null;
            
            for (Service service : services) {
                if (TextUtils.removeAccents(service.getName().toLowerCase()).equals(TextUtils.removeAccents(searchName.toLowerCase()))) {
                    foundService = service;
                    break;
                }
            }
            
            if (foundService == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy ID cho dịch vụ: " + searchName + ". Vui lòng chọn từ bảng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
                return;
            }
        }
        selectedTableModel.addRow(new Object[]{serviceId, name, quantity, currentPrice, totalAmount, note});
    }

    private void removeSelectedService() {
        int selectedRow = selectedTable.getSelectedRow();
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
