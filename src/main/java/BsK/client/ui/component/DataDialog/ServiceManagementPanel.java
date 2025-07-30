package BsK.client.ui.component.DataDialog;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Service;
import BsK.common.packet.req.GetSerInfoRequest;
import BsK.common.packet.res.GetSerInfoResponse;
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
 * A JPanel dedicated to managing services (CRUD operations).
 * It handles its own UI, data fetching, and event handling.
 */
public class ServiceManagementPanel extends JPanel {

    // --- UI Components ---
    private JTable serviceTable;
    private DefaultTableModel serviceTableModel;
    private JTextField serviceSearchField;

    // --- Input Fields (Left Side) ---
    private JTextField serviceNameField;
    private JSpinner quantitySpinner;
    private JTextField priceField;
    private JTextField totalField;
    private JTextArea noteField;

    // --- Data & State ---
    private List<Service> allServices = new ArrayList<>();
    private String selectedServiceId = null;

    // --- Networking ---
    private final ResponseListener<GetSerInfoResponse> getSerInfoResponseListener = this::handleGetSerInfoResponse;

    public ServiceManagementPanel() {
        super(new BorderLayout(10, 10));
        initComponents();
        setupNetworking();
    }

    private void initComponents() {
        this.setBorder(new EmptyBorder(15, 15, 15, 15));
        this.add(createServiceInputPanel(), BorderLayout.WEST);
        this.add(createServiceListPanel(), BorderLayout.CENTER);
    }

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetSerInfoResponse.class, getSerInfoResponseListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetSerInfoRequest());
    }

    /**
     * Creates the left panel with input fields for service details and action buttons.
     */
    private JPanel createServiceInputPanel() {
        JPanel mainInputPanel = new JPanel(new GridBagLayout());
        mainInputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.insets = new Insets(5, 5, 5, 5);
        mainGbc.weightx = 1.0;

        Font titleFont = new Font("Arial", Font.BOLD, 16);
        Font labelFont = new Font("Arial", Font.BOLD, 15);
        Font textFont = new Font("Arial", Font.PLAIN, 15);
        Dimension textFieldSize = new Dimension(100, 30);

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

        mainGbc.gridy = 2;
        mainInputPanel.add(createServiceButtonPanel(), mainGbc);

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

        return mainInputPanel;
    }

    /**
     * Creates the panel containing the "Thêm", "Sửa", "Xóa", "Làm mới" buttons.
     */
    private JPanel createServiceButtonPanel() {
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

        // Button Actions
        btnClear.addActionListener(e -> clearServiceFields());

        btnAdd.addActionListener(e -> {
            String name = serviceNameField.getText();
            if (name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên dịch vụ không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // TODO: Implement Add Service network request
            JOptionPane.showMessageDialog(this, "Chức năng 'Thêm mới' cho dịch vụ '" + name + "' sẽ được thực hiện tại đây.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        btnEdit.addActionListener(e -> {
            if (selectedServiceId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ để chỉnh sửa.", "Chưa chọn dịch vụ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // TODO: Implement Edit Service network request
            String name = serviceNameField.getText();
            JOptionPane.showMessageDialog(this, "Chức năng 'Chỉnh sửa' cho dịch vụ ID: " + selectedServiceId + " (" + name + ") sẽ được thực hiện tại đây.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        btnDelete.addActionListener(e -> {
            if (selectedServiceId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ để xóa.", "Chưa chọn dịch vụ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn xóa dịch vụ '" + serviceNameField.getText() + "' không?",
                    "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                // TODO: Implement Delete Service network request
                JOptionPane.showMessageDialog(this, "Chức năng 'Xoá' cho dịch vụ ID: " + selectedServiceId + " sẽ được thực hiện tại đây.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return buttonPanel;
    }

    /**
     * Creates the right panel with the search field and the table of all services.
     */
    private JPanel createServiceListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Danh Sách Dịch Vụ",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)));

        // Table
        String[] serviceColumns = {"ID", "Tên dịch vụ", "Đơn giá (VNĐ)"};
        serviceTableModel = new DefaultTableModel(serviceColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        serviceTable = new JTable(serviceTableModel);
        serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serviceTable.setFont(new Font("Arial", Font.PLAIN, 12));
        serviceTable.setRowHeight(28);

        serviceTable.setRowSorter(null);

        serviceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && serviceTable.getSelectedRow() != -1) {
                // IMPORTANT: We need to find the original index from the 'allServices' list
                int viewRow = serviceTable.getSelectedRow();
                String serviceId = (String) serviceTableModel.getValueAt(viewRow, 0);
                
                allServices.stream()
                    .filter(service -> service.getId().equals(serviceId))
                    .findFirst()
                    .ifPresent(this::populateServiceFields);
            }
        });

        // Search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm dịch vụ:"));
        serviceSearchField = new JTextField(25);
        searchPanel.add(serviceSearchField);

        serviceSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterServiceTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterServiceTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterServiceTable(); }
        });

        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(serviceTable), BorderLayout.CENTER);

        return listPanel;
    }

    private void handleGetSerInfoResponse(GetSerInfoResponse response) {
        SwingUtilities.invokeLater(() -> {
            if (response != null && response.getServices() != null) {
                allServices = response.getServices();
                populateServiceTable();
            }
        });
    }

    private void populateServiceTable() {
        // Just call the filter method, which will show all items if search is empty
        filterServiceTable();
    }

    private void populateServiceFields(Service service) {
        if (service == null) return;
        selectedServiceId = service.getId();
        serviceNameField.setText(service.getName());
        priceField.setText(service.getCost());
        noteField.setText(""); // Assuming notes are not stored in the main service entity
        quantitySpinner.setValue(1);
    }

    private void clearServiceFields() {
        selectedServiceId = null;
        serviceNameField.setText("");
        priceField.setText("");
        totalField.setText("");
        noteField.setText("");
        quantitySpinner.setValue(1);
        serviceTable.clearSelection();
        serviceNameField.requestFocusInWindow();
    }

    private void filterServiceTable() {
        String filterText = serviceSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        serviceTableModel.setRowCount(0); // Clear the table before adding filtered results

        for (Service service : allServices) {
            if (filterText.isEmpty() || TextUtils.removeAccents(service.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                serviceTableModel.addRow(new Object[]{
                        service.getId(),
                        service.getName(),
                        service.getCost()
                });
            }
        }
    }

    public void cleanup() {
        ClientHandler.deleteListener(GetSerInfoResponse.class, getSerInfoResponseListener);
    }
}