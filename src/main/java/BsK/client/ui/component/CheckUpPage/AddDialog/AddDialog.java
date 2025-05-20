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
    private JComboBox patientGenderField;
    private JComboBox wardComboBox, districtComboBox, provinceComboBox;
    private JTextField customerAddressField;
    private DefaultTableModel patientTableModel;
    private JTable patientTable;
    private String[] patientColumns = {"Patient ID", "Patient Name", "Patient Year", "Patient Phone" ,"Patient Address"};
    private String[][] patientData;
    private final ResponseListener<GetRecentPatientResponse> getRecentPatientResponseListener = this::getRecentPatientHandler;
    private final ResponseListener<GetDistrictResponse> districtResponseListener = this::handleGetDistrictResponse;
    private final ResponseListener<GetWardResponse> wardResponseListener = this::handleGetWardResponse;
    private final ResponseListener<AddPatientResponse> addPatientResponseListener = this::handleAddPatientResponse;
    private final ResponseListener<AddCheckupResponse> addCheckupResponseListener = this::handleAddCheckupResponse;
    private JComboBox doctorComboBox;
    private JComboBox<String> checkupTypeComboBox;
    JButton saveButton;
    private DefaultComboBoxModel<String> districtModel, wardModel;
    private JDatePickerImpl dobPicker;

    private void handleAddCheckupResponse(AddCheckupResponse response) {
        log.info("Received AddCheckupResponse");
        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Thêm bệnh nhân vào khám thành công");
        } else {
            JOptionPane.showMessageDialog(this, response.getError());
        }
        setVisible(false);
    }

    private void sendGetRecentPatientRequest() {
        log.info("Sending GetRecentPatientRequest");
        ClientHandler.addResponseListener(GetRecentPatientResponse.class, getRecentPatientResponseListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetRecentPatientRequest());
    }

    private void getRecentPatientHandler(GetRecentPatientResponse response) {
        log.info("Received GetRecentPatientResponse");
        patientData = response.getPatientData();

        patientTableModel.setDataVector(patientData, patientColumns);
    }

    private int findProvinceIndex(String province) {
        for (int i = 0; i < LocalStorage.provinces.length; i++) {
            if (TextUtils.removeAccents(LocalStorage.provinces[i]).equals(TextUtils.removeAccents(province))) {
                return i;
            }
        }
        return -1;
    }

    private int findDistrictIndex(String district) {
        for (int i = 0; i < LocalStorage.districts.length; i++) {
            if (TextUtils.removeAccents(LocalStorage.districts[i]).equals(TextUtils.removeAccents(district))) {
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
        ClientHandler.deleteListener(AddCheckupResponse.class);
        ClientHandler.deleteListener(AddPatientResponse.class);
        ClientHandler.deleteListener(GetRecentPatientResponse.class);
        super.dispose();
    }

    private void filterPatientTableByPhone() {
        String filterText = patientPhoneField.getText().toLowerCase();
        if (patientData == null) {
            return;
        }
        if (filterText.isEmpty()) {
            patientTableModel.setDataVector(patientData, patientColumns);
        } else {
            java.util.List<String[]> filteredData = new java.util.ArrayList<>();
            for (String[] row : patientData) {
                // Assuming phone number is at index 3, based on patientColumns definition
                // {"Patient ID", "Patient Name", "Patient Year", "Patient Phone" ,"Patient Address"}
                if (row.length > 3 && row[3] != null && row[3].toLowerCase().startsWith(filterText)) {
                    filteredData.add(row);
                }
            }
            patientTableModel.setDataVector(filteredData.toArray(new String[0][0]), patientColumns);
        }
    }

    private void filterPatientTableByName() {
        String filterText = patientNameField.getText().trim();
        if (patientData == null) {
            return;
        }
        if (filterText.isEmpty()) {
            patientTableModel.setDataVector(patientData, patientColumns);
            patientTable.clearSelection(); // Clear selection when text is empty
            saveButton.setEnabled(false); // Disable save button
        } else {
            java.util.List<String[]> filteredData = new java.util.ArrayList<>();
            String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());
            for (String[] row : patientData) {
                // Assuming patient name is at index 1
                // {"Patient ID", "Patient Name", "Patient Year", "Patient Phone" ,"Patient Address"}
                if (row.length > 1 && row[1] != null) {
                    String cellValue = TextUtils.removeAccents(row[1].toLowerCase());
                    if (cellValue.contains(lowerCaseFilterText)) {
                        filteredData.add(row);
                    }
                }
            }
            patientTableModel.setDataVector(filteredData.toArray(new String[0][0]), patientColumns);
            // If you want to auto-select the first match after filtering, you can add:
            // if (!filteredData.isEmpty()) {
            //     patientTable.setRowSelectionInterval(0, 0);
            //     saveButton.setEnabled(true);
            // } else {
            //     patientTable.clearSelection();
            //     saveButton.setEnabled(false);
            // }
        }
    }

    public AddDialog(Frame parent) {
        super(parent, "Add Patient", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Set size of the dialog
        setSize(1000, 400);
        // Put in the middle
        setLocationRelativeTo(null);
        setResizable(true);

        // Send request to get the latest 20 patients in the database
        sendGetRecentPatientRequest();
        ClientHandler.addResponseListener(GetDistrictResponse.class, districtResponseListener);
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
        patientTable.setFont(new Font("Serif", Font.PLAIN, 14));
        patientTable.setRowHeight(30);
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
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
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        mainGbc.gridy = 1;
        inputPanel.add(addressInfoPanel, mainGbc);

        // --- Checkup Info Panel ---
        JPanel checkupInfoPanel = new JPanel(new GridBagLayout());
        checkupInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin đăng ký khám",
                javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
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
        // Patient Name (Row 0, Col 0-1) & Add Button (Row 0, Col 2)
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        patientInfoPanel.add(new JLabel("Họ và tên:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0; // Allow patient name field to expand
        patientNameField = new JTextField(15);
        patientInfoPanel.add(patientNameField, gbc);
        gbc.weightx = 0.0; // Reset

        gbc.gridx = 2;
        gbc.gridwidth = 1;
        ImageIcon originalIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/add.png");
        Image scaledImage = originalIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        ImageIcon addIcon = new ImageIcon(scaledImage);
        JButton addButton = new JButton(addIcon);
        addButton.setToolTipText("Thêm bệnh nhân mới (nếu không tìm thấy)");
        patientInfoPanel.add(addButton, gbc);

        // DOB (Row 1, Col 0-1) & Gender (Row 1, Col 2-3)
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        patientInfoPanel.add(new JLabel("Ngày sinh:"), gbc);

        gbc.gridx = 1;
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Hôm nay");
        p.put("text.month", "Tháng");
        p.put("text.year", "Năm");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        dobPicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        dobPicker.setPreferredSize(new Dimension(150, 30));
        patientInfoPanel.add(dobPicker, gbc);

        gbc.gridx = 2;
        patientInfoPanel.add(new JLabel("Giới tính:"), gbc);

        gbc.gridx = 3;
        patientGenderField = new JComboBox<>(new String[]{"Nam", "Nữ"});
        patientInfoPanel.add(patientGenderField, gbc);

        // Phone (Row 2, Col 0-1) & Patient ID (Row 2, Col 2-3)
        gbc.gridy++;
        gbc.gridx = 0;
        patientInfoPanel.add(new JLabel("Số điện thoại:"), gbc);

        gbc.gridx = 1;
        patientPhoneField = new JTextField(10);
        patientInfoPanel.add(patientPhoneField, gbc);

        gbc.gridx = 2;
        patientInfoPanel.add(new JLabel("Mã BN:"), gbc);

        gbc.gridx = 3;
        patientIdField = new JTextField(5);
        patientIdField.setEditable(false);
        patientInfoPanel.add(patientIdField, gbc);

        // Add event listener to the addButton (moved here as it's part of this panel now)
        addButton.addActionListener(e -> {
            if (!patientIdField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Bạn đã chọn bệnh nhân rồi");
                return;
            }
            String patientName = patientNameField.getText();
            String patientPhone = patientPhoneField.getText();
            String patientGender = (String) patientGenderField.getSelectedItem();
            String customerAddress = customerAddressField.getText();
            String province = (String) provinceComboBox.getSelectedItem();
            String district = (String) districtComboBox.getSelectedItem();
            String ward = (String) wardComboBox.getSelectedItem();
            String address = String.join(", ", customerAddress, ward, district, province);

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
            // check if dobPicker is not null
            if (patientName.isEmpty()  || patientPhone.isEmpty() || customerAddress.isEmpty() || dobPicker.getJFormattedTextField().getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields");
                return;
            }

            String dateText = dobPicker.getJFormattedTextField().getText();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            try {
                Date date = dateFormat.parse(dateText);
                long timestamp = date.getTime(); // This converts the Date to milliseconds since epoch (long)
                NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddPatientRequest(firstName, lastNameStr, timestamp,
                        patientPhone, address, patientGender));
            } catch (ParseException pe) {
                pe.printStackTrace();
                JOptionPane.showMessageDialog(null, "Invalid date format: " + dateText);
            }

        });

        // Populate Checkup Info Panel
        gbc.gridy = 0; // Reset gridy for the new panel
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0; // Label doesn't expand
        checkupInfoPanel.add(new JLabel("Bác sĩ khám:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1; // Doctor ComboBox takes 1 column
        gbc.weightx = 0.5; // Doctor ComboBox takes some horizontal space
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName != null ? LocalStorage.doctorsName : new String[]{"Đang tải..."});
        checkupInfoPanel.add(doctorComboBox, gbc);

        // Checkup Type on the same row
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0; // Label doesn't expand
        checkupInfoPanel.add(new JLabel("Loại khám:"), gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1; // Checkup Type ComboBox takes 1 column
        gbc.weightx = 0.5; // Checkup Type ComboBox takes some horizontal space
        String[] checkupTypeOptions = {"BENH", "THAI", "KHAC"};
        checkupTypeComboBox = new JComboBox<>(checkupTypeOptions);
        checkupInfoPanel.add(checkupTypeComboBox, gbc);
        gbc.weightx = 0.0; // Reset weightx


        // Populate Address Info Panel
        // Address (Street) (Row 0)
        gbc.gridy = 0; 
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0; 
        addressInfoPanel.add(new JLabel("Địa chỉ cụ thể:"), gbc); // Changed label slightly for clarity

        gbc.gridx = 1;
        gbc.gridwidth = 3; 
        gbc.weightx = 1.0; 
        customerAddressField = new JTextField(20);
        addressInfoPanel.add(customerAddressField, gbc);
        gbc.weightx = 0.0; 

        // Province, District, Ward ComboBoxes on a single line (Row 1)
        gbc.gridy++;
        gbc.gridx = 0; // Start from the first column for the combo boxes
        gbc.gridwidth = 1; // Each combo box takes 1 logical column in this setup
        gbc.weightx = 0.33; // Distribute space among the three combo boxes
        provinceComboBox = new JComboBox<>(LocalStorage.provinces != null ? LocalStorage.provinces : new String[]{"Tỉnh/TP"});
        addressInfoPanel.add(provinceComboBox, gbc);

        gbc.gridx = 1;
        districtModel = new DefaultComboBoxModel<>(new String[]{"Quận/Huyện"});
        districtComboBox = new JComboBox<>(districtModel);
        districtComboBox.setEnabled(false);
        addressInfoPanel.add(districtComboBox, gbc);

        gbc.gridx = 2;
        wardModel = new DefaultComboBoxModel<>(new String[]{"Phường/Xã"});
        wardComboBox = new JComboBox<>(wardModel);
        wardComboBox.setEnabled(false);
        addressInfoPanel.add(wardComboBox, gbc);
        gbc.weightx = 0.0; // Reset weightx for any subsequent components in this panel
        gbc.gridwidth = 1; // Reset gridwidth


        // Listeners that were previously in the flat layout
        patientPhoneField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                filterPatientTableByPhone();
            }
            public void removeUpdate(DocumentEvent e) {
                filterPatientTableByPhone();
            }
            public void insertUpdate(DocumentEvent e) {
                filterPatientTableByPhone();
            }
        });

        patientNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleRowSelection(patientTable.getSelectedRow());
                }
            }
        });

        patientNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterPatientTableByName();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterPatientTableByName();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterPatientTableByName();
            }
        });

        // Province ComboBox Listener
        provinceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = provinceComboBox.getSelectedIndex();
                districtModel = new DefaultComboBoxModel<>(new String[]{"Quận/Huyện"});
                districtComboBox.setModel(districtModel); // Set district combo box model
                if (selectedIndex != 0) { // If the selected index is not 0 (which corresponds to "Tỉnh/Thành phố")
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDistrictRequest(LocalStorage.provinceToId
                            .get(provinceComboBox.getSelectedItem().toString())));
                } else {
                    districtComboBox.setEnabled(false); // Disable district combo box if "Tỉnh/Thành phố" is selected
                    wardComboBox.setEnabled(false); // Disable ward combo box as well
                }
            }
        });


        // District ComboBox Listener
        districtComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = districtComboBox.getSelectedIndex();
                wardModel = new DefaultComboBoxModel<>(new String[]{"Xã/Phường"});
                wardComboBox.setModel(wardModel); // Set district combo box model
                if (selectedIndex != 0) { // If the selected index is not 0 (which corresponds to "Quận/Huyện")
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(LocalStorage.districtToId
                            .get(districtComboBox.getSelectedItem().toString())));
                } else {
                    districtComboBox.setEnabled(false); // Disable district combo box if "Tỉnh/Thành phố" is selected
                    wardComboBox.setEnabled(false); // Disable ward combo box as well
                }
            }
        });
        inputPanel.setMinimumSize(new Dimension(400, 0));
        
        // Wrap inputPanel in a JScrollPane
        JScrollPane inputScrollPane = new JScrollPane(inputPanel);
        inputScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // Usually not needed for forms
        inputScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove scroll pane's own border

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScrollPane, scrollPane);
        splitPane.setResizeWeight(0.6); // Keep or adjust as needed
        add(splitPane, BorderLayout.CENTER);


        JPanel ButtonPanel = new JPanel();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            setVisible(false);
        });
        ButtonPanel.add(closeButton);

        saveButton = new JButton("Add to Checkup");
        saveButton.addActionListener(e -> {
            int patientId = Integer.parseInt(patientIdField.getText());
            int doctorId = doctorComboBox.getSelectedIndex()+ 1;
            String selectedCheckupType = (String) checkupTypeComboBox.getSelectedItem();
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddCheckupRequest(patientId, doctorId, LocalStorage.userId, selectedCheckupType));

        });
        saveButton.setEnabled(false);
        ButtonPanel.add(saveButton);

        add(ButtonPanel, BorderLayout.SOUTH);
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
        patientIdField.setText(patientId);
        patientNameField.setText(patientName);
        patientPhoneField.setText(patientPhone);
        patientGenderField.setSelectedItem(patientGender);

        // Extract province, district, ward from the address
        String[] addressParts = patientAddress.split(", ");
        if (addressParts.length == 4) {
            String address = addressParts[0];
            String ward = addressParts[1];
            String district = addressParts[2];
            String province = addressParts[3];
            customerAddressField.setText(address);
            provinceComboBox.setSelectedIndex(findProvinceIndex(province));
            try {
                Thread.sleep(100);
                while(!districtComboBox.isEditable()) {
                    districtComboBox.setSelectedIndex(findDistrictIndex(district));
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(100);
                while(!wardComboBox.isEditable()) {
                    wardComboBox.setSelectedIndex(findWardIndex(ward));
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
            JOptionPane.showMessageDialog(null, "Invalid date format: " + patientDob);
        }
        saveButton.setEnabled(true); // Enable button after successfully populating fields
    }

    private void handleAddPatientResponse(AddPatientResponse response) {
        log.info("Received AddPatientResponse");
        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Thêm bệnh nhân thành công");
            patientIdField.setText(String.valueOf(response.getPatientId()));
            sendGetRecentPatientRequest();
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage());
        }
    }


    private void handleGetDistrictResponse(GetDistrictResponse response) {
        log.info("Received dialog district data");

        LocalStorage.districts = response.getDistricts();
        LocalStorage.districtToId = response.getDistrictToId();
        districtModel = new DefaultComboBoxModel<>(LocalStorage.districts);
        districtComboBox.setModel(districtModel);
        districtComboBox.setEnabled(true); // Enable district combo box
    }

    private void handleGetWardResponse(GetWardResponse response) {
        log.info("Received dialog ward data");
        LocalStorage.wards = response.getWards();
        wardModel = new DefaultComboBoxModel<>(LocalStorage.wards);
        wardComboBox.setModel(wardModel);
        wardComboBox.setEnabled(true); // Enable ward combo box
    }


    public static void main(String[] args) {
        // add a button to open the dialog
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        JButton openDialogButton = new JButton("Open Dialog");
        openDialogButton.addActionListener(e -> {
            AddDialog dialog = new AddDialog(frame);
            dialog.setVisible(true);
        });

        frame.add(openDialogButton, BorderLayout.CENTER);


    }
}
