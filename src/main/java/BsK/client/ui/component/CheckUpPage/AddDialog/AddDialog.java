package BsK.client.ui.component.CheckUpPage.AddDialog;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.common.packet.req.*;
import BsK.common.packet.res.*;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;


@Slf4j
public class AddDialog extends JDialog {
    private static final Logger log = LoggerFactory.getLogger(AddDialog.class);
    private JTextField patientNameField;
    private JTextField patientYearField;
    private JTextField patientIdField;
    private JTextField patientPhoneField;
    private JTextField cccdField;
    private JComboBox patientGenderField;
    private JComboBox wardComboBox, provinceComboBox;
    private JTextField customerAddressField;
    private DefaultTableModel patientTableModel;
    private JTable patientTable;
    private String[] patientColumns = {"Mã BN", "Tên Bệnh Nhân", "Năm sinh", "Số điện thoại" ,"Địa chỉ"};
    private String[][] patientData;
    private final ResponseListener<GetRecentPatientResponse> getRecentPatientResponseListener = this::getRecentPatientHandler;
    private final ResponseListener<GetWardResponse> wardResponseListener = this::handleGetWardResponse;
    private final ResponseListener<AddPatientResponse> addPatientResponseListener = this::handleAddPatientResponse;
    private final ResponseListener<AddCheckupResponse> addCheckupResponseListener = this::handleAddCheckupResponse;
    private JComboBox doctorComboBox;
    private JComboBox<String> checkupTypeComboBox;
    JButton saveButton;
    private DefaultComboBoxModel<String> wardModel;
    private JDatePickerImpl dobPicker;
    private JButton addPatientButton;
    private JButton clearButton;
    private String targetWard = null;
    
    // Search and pagination controls (removed search buttons)
    private JSpinner pageSpinner;
    private JLabel paginationLabel;
    private int currentPage = 1;
    private int totalPages = 1;
    private int totalCount = 0;
    private final int pageSize = 20;
    
    // Debounce search timers
    private Timer nameSearchTimer;
    private Timer phoneSearchTimer;

    private void handleAddCheckupResponse(AddCheckupResponse response) {
        log.info("Received AddCheckupResponse");
        if (response.isSuccess()) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel messageLabel = new JLabel("Thêm bệnh nhân vào khám thành công");
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));

            JLabel queueLabel = new JLabel("Số thứ tự: " + response.getQueueNumber());
            queueLabel.setFont(new Font("Arial", Font.BOLD, 28));
            queueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(messageLabel);
            panel.add(Box.createVerticalStrut(10));
            panel.add(queueLabel);

            JOptionPane.showMessageDialog(this, panel, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, response.getError());
        }
        setVisible(false);
    }

    private void sendGetRecentPatientRequest() {
        sendPatientSearchRequest(null, null, 1);
    }
    
    private void sendPatientSearchRequest(String searchName, String searchPhone, int page) {
        log.info("Sending GetRecentPatientRequest with search: name='{}', phone='{}', page={}", searchName, searchPhone, page);
        ClientHandler.addResponseListener(GetRecentPatientResponse.class, getRecentPatientResponseListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetRecentPatientRequest(searchName, searchPhone, page, pageSize));
    }

    private void getRecentPatientHandler(GetRecentPatientResponse response) {
        log.info("Received GetRecentPatientResponse");
        patientData = response.getPatientData();
        
        // Update pagination info
        currentPage = response.getCurrentPage();
        totalPages = response.getTotalPages();
        totalCount = response.getTotalCount();
        
        // Update table data
        patientTableModel.setDataVector(patientData, patientColumns);
        // Set column widths: id, name, birthyear, sdt, address
        int[] columnWidths = {60, 180, 70, 120, 300};
        for (int i = 0; i < columnWidths.length; i++) {
            patientTable.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        
        // Update pagination controls
        if (pageSpinner != null) {
            SpinnerNumberModel model = (SpinnerNumberModel) pageSpinner.getModel();
            model.setMaximum(Math.max(1, totalPages));
            model.setValue(currentPage);
            paginationLabel.setText("/ " + totalPages + " (" + totalCount + " bệnh nhân)");
        }
        
        // Clear selection and update button states
        patientTable.clearSelection();
        saveButton.setEnabled(false);
        addPatientButton.setEnabled(true);
    }

    private int findProvinceIndex(String province) {
        for (int i = 0; i < LocalStorage.provinces.length; i++) {
            if (TextUtils.removeAccents(LocalStorage.provinces[i]).equals(TextUtils.removeAccents(province))) {
                return i;
            }
        }
        return -1;
    }

    private int findWardIndex(String ward) {
        for (int i = 0; i < LocalStorage.wards.length; i++) {
            if (TextUtils.removeAccents(LocalStorage.wards[i]).equals(TextUtils.removeAccents(ward))) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public void dispose() {
        // Clean up listeners
        log.info("Cleaning up listeners");
        ClientHandler.deleteListener(AddCheckupResponse.class, addCheckupResponseListener);
        ClientHandler.deleteListener(AddPatientResponse.class, addPatientResponseListener);
        ClientHandler.deleteListener(GetRecentPatientResponse.class, getRecentPatientResponseListener);
        ClientHandler.deleteListener(GetWardResponse.class, wardResponseListener);
        
        // Clean up timers
        if (nameSearchTimer != null && nameSearchTimer.isRunning()) {
            nameSearchTimer.stop();
        }
        if (phoneSearchTimer != null && phoneSearchTimer.isRunning()) {
            phoneSearchTimer.stop();
        }
        
        super.dispose();
    }



    public AddDialog(Frame parent) {
        super(parent, "Thêm Bệnh Nhân", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Set size of the dialog
        setSize(1200, 700);
        // Put in the middle of parent window
        setLocationRelativeTo(parent);
        setResizable(true);
        
        // UI Fonts and Dimensions
        Font labelFont = new Font("Arial", Font.BOLD, 15);
        Font textFont = new Font("Arial", Font.PLAIN, 15);
        Font titleFont = new Font("Arial", Font.BOLD, 16);
        Dimension textFieldSize = new Dimension(100, 30);
        Dimension comboBoxSize = new Dimension(100, 30);
        
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
        
        // Send request to get the latest 20 patients in the database
        sendGetRecentPatientRequest();
        ClientHandler.addResponseListener(GetWardResponse.class, wardResponseListener);
        ClientHandler.addResponseListener(AddPatientResponse.class, addPatientResponseListener);
        ClientHandler.addResponseListener(AddCheckupResponse.class, addCheckupResponseListener);

        // Add patent table on the right side
        // Add a scroll pane to the table
        patientTableModel = new DefaultTableModel(patientData, patientColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        patientTable = new JTable(patientTableModel);
        patientTable.setFont(textFont);
        patientTable.setRowHeight(35);
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Set column widths: id, name, birthyear, sdt, address
        int[] columnWidths = {60, 180, 70, 120, 300};
        for (int i = 0; i < columnWidths.length; i++) {
            patientTable.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }
        JScrollPane scrollPane = new JScrollPane(patientTable);

        patientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int selectedRow = patientTable.getSelectedRow();
                    if (selectedRow != -1) {
                        handleRowSelection(selectedRow);
                    }
                });
            }
        });

        // Set layout of the dialog
        // left side is the text fields, right side is the patient list

        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.insets = new Insets(5, 5, 5, 5);
        mainGbc.weightx = 1.0;

        // --- Patient Info Panel ---
        JPanel patientInfoPanel = new JPanel(new GridBagLayout());
        patientInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin bệnh nhân",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.gridwidth = 1;
        inputPanel.add(patientInfoPanel, mainGbc);

        // --- Address Info Panel ---
        JPanel addressInfoPanel = new JPanel(new GridBagLayout());
        addressInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Địa chỉ",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        inputPanel.add(addressInfoPanel, mainGbc);

        // --- Checkup Info Panel ---
        JPanel checkupInfoPanel = new JPanel(new GridBagLayout());
        checkupInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin đăng ký khám",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                titleFont, new Color(50, 50, 50)
        ));
        mainGbc.gridy = 2;
        inputPanel.add(checkupInfoPanel, mainGbc);
        
        // Placeholder for other components if any, or adjust weighting
        mainGbc.gridy = 3;
        mainGbc.weighty = 1.0; // Allow inputPanel to take remaining vertical space if needed
        inputPanel.add(new JPanel(), mainGbc); // Empty panel to push others up


        GridBagConstraints gbc = new GridBagConstraints(); // Re-use gbc for internal panel layouts
        gbc.fill = GridBagConstraints.HORIZONTAL; // Changed from BOTH to HORIZONTAL for most fields
        gbc.anchor = GridBagConstraints.WEST; // Align components to the left
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weighty = 0.0;

        // Populate Patient Info Panel
        // Patient Name (Row 0, Col 0-1) - now spans more columns since we removed the plus button
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JLabel nameLabel = new JLabel("Họ và tên:");
        nameLabel.setFont(labelFont);
        patientInfoPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3; // Span more columns since we removed the search button
        gbc.weightx = 1.0; // Allow patient name field to expand
        patientNameField = new JTextField(15);
        patientNameField.setFont(textFont);
        patientNameField.setPreferredSize(textFieldSize);
        patientInfoPanel.add(patientNameField, gbc);
        gbc.weightx = 0.0; // Reset
        gbc.gridwidth = 1; // Reset

        // DOB (Row 1, Col 0-1) & Gender (Row 1, Col 2-3)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JLabel dobLabel = new JLabel("Ngày sinh:");
        dobLabel.setFont(labelFont);
        patientInfoPanel.add(dobLabel, gbc);

        gbc.gridx = 1;
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.month", "Tháng");
        p.put("text.year", "Năm");
        // Remove "text.today" since DOB shouldn't default to today
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        dobPicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        dobPicker.getJFormattedTextField().setFont(textFont);
        dobPicker.getJFormattedTextField().setToolTipText("Nhập ngày theo định dạng dd/mm/yyyy");
        setupDateFieldForDirectInput(dobPicker);
        dobPicker.setPreferredSize(new Dimension(150, 30));
        patientInfoPanel.add(dobPicker, gbc);

        gbc.gridx = 2;
        JLabel genderLabel = new JLabel("Giới tính:");
        genderLabel.setFont(labelFont);
        patientInfoPanel.add(genderLabel, gbc);

        gbc.gridx = 3;
        patientGenderField = new JComboBox<>(new String[]{"Nam", "Nữ"});
        patientGenderField.setFont(textFont);
        patientGenderField.setPreferredSize(comboBoxSize);
        patientInfoPanel.add(patientGenderField, gbc);

        // Phone (Row 2, Col 0-2) & Patient ID (Row 3, Col 0-1)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel phoneLabel = new JLabel("Số điện thoại:");
        phoneLabel.setFont(labelFont);
        patientInfoPanel.add(phoneLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3; // Span more columns since we removed the search button
        patientPhoneField = new JTextField(10);
        patientPhoneField.setFont(textFont);
        patientPhoneField.setPreferredSize(textFieldSize);
        patientInfoPanel.add(patientPhoneField, gbc);
        gbc.gridwidth = 1; // Reset

        // Patient ID (Row 3, Col 0-1)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel patientIdLabel = new JLabel("Mã BN:");
        patientIdLabel.setFont(labelFont);
        patientInfoPanel.add(patientIdLabel, gbc);

        gbc.gridx = 1;
        patientIdField = new JTextField(5);
        patientIdField.setFont(textFont);
        patientIdField.setPreferredSize(textFieldSize);
        patientIdField.setEditable(false);
        patientInfoPanel.add(patientIdField, gbc);

        // CCCD (Row 4, Col 0-3)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        JLabel cccdLabel = new JLabel("CCCD/DDCN:");
        cccdLabel.setFont(labelFont);
        patientInfoPanel.add(cccdLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3; // Span across remaining columns
        cccdField = new JTextField(15);
        cccdField.setFont(textFont);
        cccdField.setPreferredSize(textFieldSize);
        patientInfoPanel.add(cccdField, gbc);
        gbc.gridwidth = 1; // Reset

        // Populate Checkup Info Panel
        gbc.gridy = 0; // Reset gridy for the new panel
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0; // Label doesn't expand
        JLabel doctorLabel = new JLabel("Bác sĩ khám:");
        doctorLabel.setFont(labelFont);
        checkupInfoPanel.add(doctorLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1; // Doctor ComboBox takes 1 column
        gbc.weightx = 0.5; // Doctor ComboBox takes some horizontal space
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName != null ? LocalStorage.doctorsName : new String[]{"Đang tải..."});
        doctorComboBox.setFont(textFont);
        doctorComboBox.setPreferredSize(comboBoxSize);
        checkupInfoPanel.add(doctorComboBox, gbc);

        // Checkup Type on the same row
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0; // Label doesn't expand
        JLabel checkupTypeLabel = new JLabel("Loại khám:");
        checkupTypeLabel.setFont(labelFont);
        checkupInfoPanel.add(checkupTypeLabel, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1; // Checkup Type ComboBox takes 1 column
        gbc.weightx = 0.5; // Checkup Type ComboBox takes some horizontal space
        String[] checkupTypeOptions = {"BỆNH", "THAI", "KHÁC"};
        checkupTypeComboBox = new JComboBox<>(checkupTypeOptions);
        checkupTypeComboBox.setFont(textFont);
        checkupTypeComboBox.setPreferredSize(comboBoxSize);
        checkupInfoPanel.add(checkupTypeComboBox, gbc);
        gbc.weightx = 0.0; // Reset weightx

        // Add new buttons row (Row 1)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        addPatientButton = new JButton("Thêm bệnh nhân mới");
        addPatientButton.setFont(textFont);
        addPatientButton.setEnabled(true); // Initially enabled since no patient is selected
        checkupInfoPanel.add(addPatientButton, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        clearButton = new JButton("Làm mới");
        clearButton.setFont(textFont);
        checkupInfoPanel.add(clearButton, gbc);
        gbc.weightx = 0.0; // Reset weightx
        gbc.gridwidth = 1; // Reset gridwidth


        // Populate Address Info Panel
        // Address (Street) (Row 0)
        gbc.gridy = 0; 
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        JLabel addressLabel = new JLabel("Địa chỉ cụ thể:");
        addressLabel.setFont(labelFont);
        addressInfoPanel.add(addressLabel, gbc); // Changed label slightly for clarity

        gbc.gridx = 1;
        gbc.gridwidth = 3; 
        gbc.weightx = 1.0; 
        customerAddressField = new JTextField(20);
        customerAddressField.setFont(textFont);
        customerAddressField.setPreferredSize(textFieldSize);
        addressInfoPanel.add(customerAddressField, gbc);
        gbc.weightx = 0.0; 

        // Province, Ward, Ward ComboBoxes on a single line (Row 1)
        gbc.gridy++;
        gbc.gridx = 0; // Start from the first column for the combo boxes
        gbc.gridwidth = 1; // Each combo box takes 1 logical column in this setup
        gbc.weightx = 0.33; // Distribute space among the three combo boxes
        provinceComboBox = new JComboBox<>(LocalStorage.provinces != null ? LocalStorage.provinces : new String[]{"Tỉnh/TP"});
        provinceComboBox.setFont(textFont);
        provinceComboBox.setPreferredSize(comboBoxSize);
        addressInfoPanel.add(provinceComboBox, gbc);

        gbc.gridx = 1;
        wardModel = new DefaultComboBoxModel<>(new String[]{"Xã/Phường"});
        wardComboBox = new JComboBox<>(wardModel);
        wardComboBox.setFont(textFont);
        wardComboBox.setPreferredSize(comboBoxSize);
        wardComboBox.setEnabled(false);
        addressInfoPanel.add(wardComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0; // Reset weightx for any subsequent components in this panel
        gbc.gridwidth = 1; // Reset gridwidth


        // Setup debounce search functionality
        setupDebounceSearch();

        // Province ComboBox Listener
        provinceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = provinceComboBox.getSelectedIndex();
                wardModel = new DefaultComboBoxModel<>(new String[]{"Xã/Phường"});
                wardComboBox.setModel(wardModel); // Set ward combo box model
                if (selectedIndex != 0) { // If the selected index is not 0 (which corresponds to "Tỉnh/Thành phố")
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(LocalStorage.provinceToId
                            .get(provinceComboBox.getSelectedItem().toString())));
                } else {
                    wardComboBox.setEnabled(false); // Disable ward combo box if "Tỉnh/Thành phố" is selected
                }
            }
        });


        // Ward ComboBox Listener
    
        inputPanel.setMinimumSize(new Dimension(400, 0));
        
        // Wrap inputPanel in a JScrollPane
        JScrollPane inputScrollPane = new JScrollPane(inputPanel);
        inputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Usually not needed for forms
        inputScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove scroll pane's own border

        // Create right panel for table and pagination
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Create pagination panel
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel prevLabel = new JLabel("Trang:");
        prevLabel.setFont(labelFont);
        paginationPanel.add(prevLabel);
        
        // Page spinner
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
        pageSpinner = new JSpinner(spinnerModel);
        pageSpinner.setPreferredSize(new Dimension(60, 25));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) pageSpinner.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        paginationPanel.add(pageSpinner);
        
        // Pagination info label
        paginationLabel = new JLabel("/ 1 (0 bệnh nhân)");
        paginationLabel.setFont(textFont);
        paginationPanel.add(paginationLabel);
        
        rightPanel.add(paginationPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScrollPane, rightPanel);
        splitPane.setResizeWeight(0.5); // Keep or adjust as needed
        add(splitPane, BorderLayout.CENTER);


        JPanel ButtonPanel = new JPanel();
        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(labelFont);
        closeButton.addActionListener(e -> {
            setVisible(false);
        });
        ButtonPanel.add(closeButton);

        saveButton = new JButton("Thêm vào khám");
        saveButton.setFont(labelFont);
        saveButton.addActionListener(e -> {
            int patientId = Integer.parseInt(patientIdField.getText());
            int doctorId = doctorComboBox.getSelectedIndex()+ 1;
            String selectedCheckupType = (String) checkupTypeComboBox.getSelectedItem();
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddCheckupRequest(patientId, doctorId, LocalStorage.userId, selectedCheckupType, "ĐANG KHÁM"));

        });
        saveButton.setEnabled(false);
        ButtonPanel.add(saveButton);

        add(ButtonPanel, BorderLayout.SOUTH);

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

        // Add event listeners for the new buttons
        addPatientButton.addActionListener(e -> {
            if (!patientIdField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bạn đã chọn bệnh nhân rồi");
                return;
            }
            String patientName = patientNameField.getText().trim();
            String patientPhone = patientPhoneField.getText().trim();
            String patientGender = (String) patientGenderField.getSelectedItem();
            String customerAddress = customerAddressField.getText().trim();
            String province = (String) provinceComboBox.getSelectedItem();
            String ward = (String) wardComboBox.getSelectedItem();
            String cccd = cccdField.getText().trim();

            // Validate required fields
            if (patientName.isEmpty() || patientPhone.isEmpty() || customerAddress.isEmpty() || 
                dobPicker.getJFormattedTextField().getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin");
                return;
            }
            
            // Check if province is selected
            if (provinceComboBox.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn tỉnh/thành phố");
                return;
            }
            
            // Check if ward is selected and enabled
            if (wardComboBox.isEnabled() && wardComboBox.getSelectedIndex() <= 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn xã/phường");
                return;
            }

            // Build the full address with proper format
            String address;
            if (wardComboBox.isEnabled() && wardComboBox.getSelectedIndex() > 0) {
                address = String.join(", ", customerAddress, ward, province);
            } else {
                address = String.join(", ", customerAddress, province);
            }

            //split name into first name and last name
            String[] nameParts = patientName.split(" ");
            String firstName = nameParts[nameParts.length - 1];
            StringBuilder lastName = new StringBuilder();
            for (int i = 0; i < nameParts.length - 1; i++) {
                lastName.append(nameParts[i]);
                if (i < nameParts.length - 2) {
                    lastName.append(" ");
                }
            }

            String lastNameStr = lastName.toString();
            String dateText = dobPicker.getJFormattedTextField().getText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            try {
                Date date = dateFormat.parse(dateText);
                long timestamp = date.getTime(); // This converts the Date to milliseconds since epoch (long)
                NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddPatientRequest(firstName, lastNameStr, timestamp,
                        patientPhone, address, patientGender, cccd));
            } catch (ParseException pe) {
                pe.printStackTrace();
                JOptionPane.showMessageDialog(null, "Định dạng ngày không hợp lệ: " + dateText);
            }
        });

        clearButton.addActionListener(e -> {
            // Stop any ongoing search timers
            if (nameSearchTimer != null && nameSearchTimer.isRunning()) {
                nameSearchTimer.stop();
            }
            if (phoneSearchTimer != null && phoneSearchTimer.isRunning()) {
                phoneSearchTimer.stop();
            }
            
            // Clear all fields
            patientNameField.setText("");
            patientPhoneField.setText("");
            patientIdField.setText("");
            cccdField.setText("");
            customerAddressField.setText("");
            
            // Reset combo boxes
            patientGenderField.setSelectedIndex(0);
            provinceComboBox.setSelectedIndex(0);
            wardComboBox.setSelectedIndex(0);
            wardComboBox.setEnabled(false);
            doctorComboBox.setSelectedIndex(0);
            checkupTypeComboBox.setSelectedIndex(0);
            
            // Reset targetWard
            targetWard = null;
            
            // Clear date picker
            dobPicker.getModel().setValue(null);
            dobPicker.getJFormattedTextField().setText("");
            
            // Clear table selection
            patientTable.clearSelection();
            
            // Update button states
            saveButton.setEnabled(false);
            addPatientButton.setEnabled(true);
            
            // Reset table to show all patients
            sendGetRecentPatientRequest();
    });
    

    
    // Add pagination spinner listener
    pageSpinner.addChangeListener(e -> {
        int selectedPage = (Integer) pageSpinner.getValue();
        if (selectedPage != currentPage && selectedPage >= 1 && selectedPage <= totalPages) {
            // Determine current search parameters
            String searchName = patientNameField.getText().trim();
            String searchPhone = patientPhoneField.getText().trim();
            searchName = searchName.isEmpty() ? null : searchName;
            searchPhone = searchPhone.isEmpty() ? null : searchPhone;
            
            sendPatientSearchRequest(searchName, searchPhone, selectedPage);
        }
    });
}

    private void setupDebounceSearch() {
        // Initialize timers for debounce search (1 second delay)
        nameSearchTimer = new Timer(1000, e -> {
            String searchName = patientNameField.getText().trim();
            String searchPhone = patientPhoneField.getText().trim();
            searchName = searchName.isEmpty() ? null : searchName;
            searchPhone = searchPhone.isEmpty() ? null : searchPhone;
            sendPatientSearchRequest(searchName, searchPhone, 1);
        });
        nameSearchTimer.setRepeats(false);

        phoneSearchTimer = new Timer(1000, e -> {
            String searchName = patientNameField.getText().trim();
            String searchPhone = patientPhoneField.getText().trim();
            searchName = searchName.isEmpty() ? null : searchName;
            searchPhone = searchPhone.isEmpty() ? null : searchPhone;
            sendPatientSearchRequest(searchName, searchPhone, 1);
        });
        phoneSearchTimer.setRepeats(false);

        // Add DocumentListener to patientNameField for debounced search
        patientNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetAndStartTimer(nameSearchTimer);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetAndStartTimer(nameSearchTimer);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetAndStartTimer(nameSearchTimer);
            }
        });

        // Add DocumentListener to patientPhoneField for debounced search
        patientPhoneField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetAndStartTimer(phoneSearchTimer);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                resetAndStartTimer(phoneSearchTimer);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                resetAndStartTimer(phoneSearchTimer);
            }
        });
    }

    private void resetAndStartTimer(Timer timer) {
        if (timer.isRunning()) {
            timer.stop();
        }
        timer.start();
    }

    private void handleRowSelection(int selectedRow) {
        if (selectedRow < 0) {
            return;
        }

        String patientId = patientTable.getValueAt(selectedRow, 0).toString();
        String patientName = patientTable.getValueAt(selectedRow, 1).toString();
        // String patientYear = patientTable.getValueAt(selectedRow, 2).toString();
        String patientPhone = patientTable.getValueAt(selectedRow, 3).toString();
        String patientAddress = patientTable.getValueAt(selectedRow, 4).toString();
        String patientGender = patientData[selectedRow][5];
        String patientDob = patientData[selectedRow][6];
        String patientCccd = patientData[selectedRow][7];
        patientIdField.setText(patientId);
        patientNameField.setText(patientName);
        patientPhoneField.setText(patientPhone);
        patientGenderField.setSelectedItem(patientGender);
        cccdField.setText(patientCccd);

        // Reset targetWard
        targetWard = null;

        // Extract province, ward, and detailed address from the full address
        if (patientAddress != null && !patientAddress.isEmpty()) {
            String[] addressParts = patientAddress.split(", ");
            
            if (addressParts.length >= 2) {
                // Last part is province, second last is ward, everything else is address
                String province = addressParts[addressParts.length - 1];
                targetWard = addressParts[addressParts.length - 2];
                
                // Combine remaining parts as the detailed address
                StringBuilder detailedAddress = new StringBuilder();
                for (int i = 0; i < addressParts.length - 2; i++) {
                    detailedAddress.append(addressParts[i]);
                    if (i < addressParts.length - 3) {
                        detailedAddress.append(", ");
                    }
                }
                
                customerAddressField.setText(detailedAddress.toString());
                provinceComboBox.setSelectedIndex(findProvinceIndex(province));
                // Ward will be set in the response handler using targetWard
            } else {
                // If address doesn't have enough parts, just use the whole address as detailed address
                customerAddressField.setText(patientAddress);
                provinceComboBox.setSelectedIndex(0); // Select default province
            }
        } else {
            // Clear address fields if no address is available
            customerAddressField.setText("");
            provinceComboBox.setSelectedIndex(0);
        }
        
        SimpleDateFormat dobFormat;
        try {
            Date parsedDate;
            if (patientDob.matches("\\d+")) {  // Check if the string is a timestamp
                long timestamp = Long.parseLong(patientDob); // Convert the string to a long
                parsedDate = new Date(timestamp); // Convert the timestamp to a Date
            } else {
                dobFormat = new SimpleDateFormat("dd/MM/yyyy");
                parsedDate = dobFormat.parse(patientDob); // Parse formatted date string
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);

            dobPicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dobPicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Định dạng ngày không hợp lệ: " + patientDob);
        }
        saveButton.setEnabled(true); // Enable button after successfully populating fields
        addPatientButton.setEnabled(false); // Disable add patient button when a patient is selected
    }

    private void handleAddPatientResponse(AddPatientResponse response) {
        log.info("Received AddPatientResponse");
        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Thêm bệnh nhân thành công");
            patientIdField.setText(String.valueOf(response.getPatientId()));
            addPatientButton.setEnabled(false); // Disable add patient button since a patient is now selected
            saveButton.setEnabled(true); // Enable save button since we now have a patient
            sendGetRecentPatientRequest();
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage());
        }
    }


    private void handleGetWardResponse(GetWardResponse response) {
        log.info("Received dialog ward data");

        LocalStorage.wards = response.getWards();
        wardModel = new DefaultComboBoxModel<>(LocalStorage.wards);
        wardComboBox.setModel(wardModel);
        wardComboBox.setEnabled(true); // Enable ward combo box

        // Set the ward after the ward data is loaded
        if (targetWard != null) {
            int wardIndex = findWardIndex(targetWard);
            if (wardIndex >= 0) {
                wardComboBox.setSelectedIndex(wardIndex);
            }
            // We've handled this targetWard, so clear it
            targetWard = null;
        }
    }


    private void setupDateFieldForDirectInput(JDatePickerImpl datePicker) {
        JFormattedTextField textField = datePicker.getJFormattedTextField();
        
        // Make sure the field is editable
        textField.setEditable(true);
        
        // Improve focus behavior for better tab navigation
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (textField.getText().isEmpty()) {
                        textField.setForeground(Color.BLACK);
                    }
                    textField.selectAll();
                });
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(Color.GRAY);
                }
            }
        });

        // Add key listener for better input handling
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Allow tab navigation
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    textField.transferFocus();
                }
            }
        });
    }


    public static void main(String[] args) {
        // add a button to open the dialog
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        JButton openDialogButton = new JButton("Mở Dialog");
        openDialogButton.addActionListener(e -> {
            AddDialog dialog = new AddDialog(frame);
            dialog.setVisible(true);
        });

        frame.add(openDialogButton, BorderLayout.CENTER);


    }
}
