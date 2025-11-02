package BsK.client.ui.component.DataDialog;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.Doctor;
import BsK.common.entity.DoctorItem;
import BsK.common.packet.req.AddDoctorRequest;
import BsK.common.packet.req.EditDoctorRequest;
import BsK.common.packet.req.GetDoctorInfoRequest;
import BsK.common.packet.res.GetDoctorInfoResponse;
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


@Slf4j
public class DoctorManagementPanel extends JPanel {

    private JTable doctorTable;
    private DefaultTableModel doctorTableModel;
    private JTextField doctorSearchField;

    private JTextField hoField;
    private JTextField tenField;
    private JCheckBox chkIsDeleted;

    private JButton btnAdd, btnEdit, btnClear;

    private List<Doctor> allDoctors = new ArrayList<>();
    private String selectedDoctorId = null;

    // --- Networking ---
    private final ResponseListener<GetDoctorInfoResponse> getDoctorInfoResponseListener = this::handleGetDoctorInfoResponse;

    public DoctorManagementPanel() {
        super(new BorderLayout(10, 10));
        initComponents();
        setupNetworking();
    }

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetDoctorInfoResponse.class, getDoctorInfoResponseListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDoctorInfoRequest());
    }

    private void initComponents() {
        this.setBorder(new EmptyBorder(15, 15, 15, 15));
        this.add(createDoctorInputPanel(), BorderLayout.WEST);
        this.add(createDoctorListPanel(), BorderLayout.CENTER);
    }

    private void handleGetDoctorInfoResponse(GetDoctorInfoResponse response) {
        SwingUtilities.invokeLater(() -> {
            if (response != null && response.getDoctors() != null) {
                allDoctors = response.getDoctors();
                populateDoctorTable();
                
                // Update LocalStorage.doctorsName with only non-deleted doctors
                LocalStorage.doctorsName.clear();
                for (Doctor doctor : allDoctors) {
                    if ("0".equals(doctor.getDeleted())) {
                        LocalStorage.doctorsName.add(new DoctorItem(
                            doctor.getId(), 
                            doctor.getFullName()
                        ));
                    }
                }
                log.info("Updated LocalStorage.doctorsName with {} active doctors", LocalStorage.doctorsName.size());
            }
        });
    }

    private JPanel createDoctorInputPanel() {
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

        JPanel doctorInfoPanel = new JPanel(new GridBagLayout());
        doctorInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin bác sĩ",
                TitledBorder.LEADING, TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainInputPanel.add(doctorInfoPanel, mainGbc);

        mainGbc.gridy = 1;
        mainInputPanel.add(createDoctorButtonPanel(), mainGbc);

        mainGbc.gridy = 2;
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
        JLabel hoLabel = new JLabel("Họ:");
        hoLabel.setFont(labelFont);
        doctorInfoPanel.add(hoLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        hoField = new JTextField(20);
        hoField.setFont(textFont);
        hoField.setPreferredSize(textFieldSize);
        doctorInfoPanel.add(hoField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel tenLabel = new JLabel("Tên:");
        tenLabel.setFont(labelFont);
        doctorInfoPanel.add(tenLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        tenField = new JTextField(10);
        tenField.setFont(textFont);
        tenField.setPreferredSize(textFieldSize);
        doctorInfoPanel.add(tenField, gbc);

        return mainInputPanel;
    }

    private JPanel createDoctorButtonPanel() {
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

        buttonPanel.add(chkIsDeleted);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnClear);

        btnClear.addActionListener(e -> clearDoctorFields());

        // --- ADD DOCTOR ACTION ---
        btnAdd.addActionListener(e -> {
            // 1. Validate required fields
            String ho = hoField.getText().trim();
            String ten = tenField.getText().trim();
            
            if (ho.isEmpty() || ten.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Họ và tên không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                if (ho.isEmpty()) {
                    hoField.requestFocusInWindow();
                } else {
                    tenField.requestFocusInWindow();
                }
                return;
            }

            // 2. Create the request packet and send it to the server
            AddDoctorRequest request = new AddDoctorRequest(ho, ten, false);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

            // 3. Provide feedback and reset the form
            JOptionPane.showMessageDialog(this, "Yêu cầu thêm bác sĩ '" + ho + " " + ten + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearDoctorFields();
        });

        // --- EDIT DOCTOR ACTION ---
        btnEdit.addActionListener(e -> {
            // 1. Check if a doctor has been selected
            if (selectedDoctorId == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một bác sĩ để chỉnh sửa.", "Chưa chọn bác sĩ", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Validate required fields
            String ho = hoField.getText().trim();
            String ten = tenField.getText().trim();
            
            if (ho.isEmpty() || ten.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Họ và tên không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                if (ho.isEmpty()) {
                    hoField.requestFocusInWindow();
                } else {
                    tenField.requestFocusInWindow();
                }
                return;
            }

            // 3. Gather data from all fields
            String id = selectedDoctorId;
            Boolean isDeleted = chkIsDeleted.isSelected();

            // 4. Create the request packet and send it
            EditDoctorRequest request = new EditDoctorRequest(id, ho, ten, isDeleted);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);

            // 5. Provide user feedback and clear the form
            JOptionPane.showMessageDialog(this, "Yêu cầu chỉnh sửa bác sĩ '" + ho + " " + ten + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearDoctorFields();
        });

        btnEdit.setEnabled(false);
        chkIsDeleted.setEnabled(false);

        return buttonPanel;
    }

    private JPanel createDoctorListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Danh Sách Bác Sĩ",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)));

        String[] doctorColumns = {"ID", "Họ", "Tên", "Trạng thái"};
        doctorTableModel = new DefaultTableModel(doctorColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        doctorTable = new JTable(doctorTableModel);
        doctorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        doctorTable.setFont(new Font("Arial", Font.PLAIN, 12));
        doctorTable.setRowHeight(28);

        doctorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && doctorTable.getSelectedRow() != -1) {
                int viewRow = doctorTable.getSelectedRow();
                String doctorId = doctorTableModel.getValueAt(viewRow, 0).toString();

                allDoctors.stream()
                        .filter(doctor -> doctor.getId().equals(doctorId))
                        .findFirst()
                        .ifPresent(this::populateDoctorFields);
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm bác sĩ:"));
        doctorSearchField = new JTextField(25);
        searchPanel.add(doctorSearchField);

        doctorSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterDoctorTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterDoctorTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterDoctorTable(); }
        });

        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(doctorTable), BorderLayout.CENTER);

        return listPanel;
    }

    private void populateDoctorTable() {
        filterDoctorTable();
    }

    private void populateDoctorFields(Doctor doctor) {
        if (doctor == null) return;
        selectedDoctorId = doctor.getId();
        
        hoField.setText(doctor.getLastName());
        tenField.setText(doctor.getFirstName());
        chkIsDeleted.setSelected("1".equals(doctor.getDeleted()));

        btnAdd.setEnabled(false);
        btnEdit.setEnabled(true);
        chkIsDeleted.setEnabled(true);
    }

    private void clearDoctorFields() {
        selectedDoctorId = null;
        hoField.setText("");
        tenField.setText("");
        chkIsDeleted.setSelected(false);
        doctorTable.clearSelection();
        hoField.requestFocusInWindow();

        btnAdd.setEnabled(true);
        btnEdit.setEnabled(false);
        chkIsDeleted.setEnabled(false);
    }

    private void filterDoctorTable() {
        String filterText = doctorSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());

        doctorTableModel.setRowCount(0);

        for (Doctor doctor : allDoctors) {
            String fullName = doctor.getFullName();
            if (filterText.isEmpty() || TextUtils.removeAccents(fullName.toLowerCase()).contains(lowerCaseFilterText)) {
                String status = "0".equals(doctor.getDeleted()) ? "Hoạt động" : "Đã ẩn";

                doctorTableModel.addRow(new Object[]{
                        doctor.getId(),
                        doctor.getLastName(),
                        doctor.getFirstName(),
                        status
                });
            }
        }
    }

    public void cleanup() {
        ClientHandler.deleteListener(GetDoctorInfoResponse.class, getDoctorInfoResponseListener);
    }
}

