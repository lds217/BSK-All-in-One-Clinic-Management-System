package BsK.client.ui.component.DataDialog;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Service;
import BsK.common.packet.req.AddServiceRequest;
import BsK.common.packet.req.EditServiceRequest;
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
    private JTextField priceField;
    private JCheckBox chkIsDeleted; // Checkbox for soft delete

    // --- Action Buttons ---
    private JButton btnAdd, btnEdit, btnClear;

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

        mainGbc.gridy = 1;
        mainInputPanel.add(createServiceButtonPanel(), mainGbc);

        mainGbc.gridy = 2;
        mainGbc.weighty = 1.0; // Pushes content to the top
        mainInputPanel.add(new JPanel(), mainGbc);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        // --- Tên dịch vụ ---
        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel nameLabel = new JLabel("Tên dịch vụ:");
        nameLabel.setFont(labelFont);
        serviceInfoPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        serviceNameField = new JTextField(20);
        serviceNameField.setFont(textFont);
        serviceNameField.setPreferredSize(textFieldSize);
        serviceInfoPanel.add(serviceNameField, gbc);

        // --- Đơn giá ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel priceLabel = new JLabel("Đơn giá (VNĐ):");
        priceLabel.setFont(labelFont);
        serviceInfoPanel.add(priceLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        priceField = new JTextField(10);
        priceField.setFont(textFont);
        priceField.setPreferredSize(textFieldSize);
        serviceInfoPanel.add(priceField, gbc);

        return mainInputPanel;
    }

    /**
     * Creates the panel containing the action buttons and checkboxes.
     */
    private JPanel createServiceButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnAdd = new JButton("Thêm mới");
        btnEdit = new JButton("Chỉnh sửa");
        btnClear = new JButton("Làm mới");
        chkIsDeleted = new JCheckBox("Ẩn (Xoá)");
        chkIsDeleted.setFont(new Font("Arial", Font.BOLD, 13));

        Dimension btnSize = new Dimension(100, 35);
        btnAdd.setPreferredSize(btnSize);
        btnEdit.setPreferredSize(btnSize);
        btnClear.setPreferredSize(btnSize);

        // --- Component Order Changed Here ---
        buttonPanel.add(chkIsDeleted);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnClear);

        // Button Actions
        btnClear.addActionListener(e -> clearServiceFields());

        btnAdd.addActionListener(e -> {
            String name = serviceNameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên dịch vụ không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                serviceNameField.requestFocusInWindow();
                return;
            }
            String priceText = priceField.getText().trim();
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Giá dịch vụ không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                priceField.requestFocusInWindow();
                return;
            }

            Double price;
            try {
                price = Double.parseDouble(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Giá dịch vụ không được là số âm.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Giá dịch vụ phải là một con số hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Boolean deleted = false; // New services are active by default
            AddServiceRequest request = new AddServiceRequest(name, price, deleted);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

            JOptionPane.showMessageDialog(this, "Yêu cầu thêm dịch vụ '" + name + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearServiceFields();
        });

        btnEdit.addActionListener(e -> {
            if (selectedServiceId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một dịch vụ để chỉnh sửa.", "Chưa chọn dịch vụ", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String name = serviceNameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên dịch vụ không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String priceText = priceField.getText().trim();
            if (priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Giá dịch vụ không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Double price;
            try {
                price = Double.parseDouble(priceText);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, "Giá dịch vụ không được là số âm.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Giá dịch vụ phải là một con số hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Boolean deleted = chkIsDeleted.isSelected();
            EditServiceRequest request = new EditServiceRequest(selectedServiceId, name, price, deleted);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

            JOptionPane.showMessageDialog(this, "Yêu cầu chỉnh sửa dịch vụ '" + name + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearServiceFields();
        });

        // Initial button states
        btnEdit.setEnabled(false);
        chkIsDeleted.setEnabled(false);

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
        String[] serviceColumns = {"ID", "Tên dịch vụ", "Đơn giá (VNĐ)", "Trạng thái"};
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

        // Update checkbox state
        chkIsDeleted.setSelected("1".equals(service.getDeleted()));

        // Update button states
        btnAdd.setEnabled(false);
        btnEdit.setEnabled(true);
        chkIsDeleted.setEnabled(true);
    }

    private void clearServiceFields() {
        selectedServiceId = null;
        serviceNameField.setText("");
        priceField.setText("");
        chkIsDeleted.setSelected(false);
        serviceTable.clearSelection();
        serviceNameField.requestFocusInWindow();

        // Reset button states
        btnAdd.setEnabled(true);
        btnEdit.setEnabled(false);
        chkIsDeleted.setEnabled(false);
    }

    private void filterServiceTable() {
        String filterText = serviceSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        serviceTableModel.setRowCount(0); // Clear the table before adding filtered results

        for (Service service : allServices) {
            if (filterText.isEmpty() || TextUtils.removeAccents(service.getName().toLowerCase()).contains(lowerCaseFilterText)) {
                String status = "0".equals(service.getDeleted()) ? "Đang bán" : "Đã ẩn";
                serviceTableModel.addRow(new Object[]{
                        service.getId(),
                        service.getName(),
                        service.getCost(),
                        status
                });
            }
        }
    }

    public void cleanup() {
        ClientHandler.deleteListener(GetSerInfoResponse.class, getSerInfoResponseListener);
    }
}