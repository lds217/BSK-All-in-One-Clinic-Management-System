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
        ClientHandler.deleteListener(AddCheckupResponse.class);
        ClientHandler.deleteListener(AddPatientResponse.class);
        ClientHandler.deleteListener(GetRecentPatientResponse.class);
        ClientHandler.deleteListener(GetDistrictResponse.class);
        ClientHandler.deleteListener(GetWardResponse.class);
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

    public AddDialog(Frame parent) {
        super(parent, "Add Patient", true);

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
        patientTable.setFont(new Font("Serif", Font.BOLD, 20));
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
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;  // Fill both directions
        // Initial GridBagConstraints setup
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weighty = 0.0;

        // Patient Name row
        gbc.gridy = 0;

        // Label
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        inputPanel.add(new JLabel("Patient name:"), gbc);

        // Text field
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        patientNameField = new JTextField(8);
        inputPanel.add(patientNameField, gbc);

        // Add button
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        // This resolves paths relative to your class location
        ImageIcon originalIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/add.png");
        Image scaledImage = originalIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        ImageIcon addIcon = new ImageIcon(scaledImage);
        JButton addButton = new JButton(addIcon);
        inputPanel.add(addButton, gbc);

        // add event listener to the button
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

        // Doctor row
        gbc.gridy++;

        // Label
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Doctor:"), gbc);

        // Doctor combo box
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName);
        inputPanel.add(doctorComboBox, gbc);

        // Patient year and gender row
        gbc.gridy++;

        // Year label
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Patient year:"), gbc);

        // dob field
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        dobPicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        dobPicker.setPreferredSize(new Dimension(150, 30));
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        inputPanel.add(dobPicker, gbc);

        // Gender label
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Patient gender:"), gbc);

        // Gender combo
        gbc.gridx = 3;
        patientGenderField = new JComboBox<>(new String[]{"Nam", "Nữ"});
        inputPanel.add(patientGenderField, gbc);

        // Phone and ID row
        gbc.gridy++;

        // Phone label
        gbc.gridx = 0;
        inputPanel.add(new JLabel("Patient phone:"), gbc);

        // Phone field
        gbc.gridx = 1;
        patientPhoneField = new JTextField(8);
        inputPanel.add(patientPhoneField, gbc);

        // ID label
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Patient ID:"), gbc);

        // ID field
        gbc.gridx = 3;
        patientIdField = new JTextField(4);
        inputPanel.add(patientIdField, gbc);
        patientIdField.setEditable(false);
        // Address row
        gbc.gridy++;

        // Address label
        gbc.gridx = 0;
        inputPanel.add(new JLabel("Địa chỉ"), gbc);

        // Address field
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        customerAddressField = new JTextField(15);
        inputPanel.add(customerAddressField, gbc);

        // Province/District/Ward row
        gbc.gridy++;
        gbc.gridwidth = 1;

        // Province combo
        gbc.gridx = 1;
        provinceComboBox = new JComboBox<>(LocalStorage.provinces);
        inputPanel.add(provinceComboBox, gbc);

        // District combo
        gbc.gridx = 2;
        districtComboBox = new JComboBox<>(new String[]{"Huyện 1", "Huyện 2", "Huyện 3"});
        districtComboBox.setEnabled(false);
        inputPanel.add(districtComboBox, gbc);

        // Ward combo
        gbc.gridx = 3;
        wardComboBox = new JComboBox<>(new String[]{"Phường 1", "Phường 2", "Phường 3"});
        wardComboBox.setEnabled(false);
        inputPanel.add(wardComboBox, gbc);

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
                findAndSelectRow();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                findAndSelectRow();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                findAndSelectRow();
            }

            private void findAndSelectRow() {
                String searchText = patientNameField.getText().trim();
                if (searchText.isEmpty()) {
                    // Clear selection if search text is empty
                    saveButton.setEnabled(false);
                    patientTable.clearSelection();
                    return;
                }

                boolean found = false;
                for (int row = 0; row < patientTable.getRowCount(); row++) {
                    // Assuming the search is targeting the name column (column index 1)
                    String cellValue = patientTable.getValueAt(row, 1).toString();
                    if (TextUtils.removeAccents(cellValue.toLowerCase()).contains(TextUtils.removeAccents(searchText.toLowerCase()))) {
                        patientTable.setRowSelectionInterval(row, row);
                        patientTable.scrollRectToVisible(patientTable.getCellRect(row, 1, true));
                        found = true;
                        break; // Stop after selecting the first match
                    }
                }

                if (!found) {
                    patientTable.clearSelection();
                    saveButton.setEnabled(false);
                    patientIdField.setText("");
                }
                else {
                    saveButton.setEnabled(true);
                }
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
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, scrollPane);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);


        JPanel ButtonPanel = new JPanel();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            ClientHandler.deleteListener(AddCheckupResponse.class);
            ClientHandler.deleteListener(AddPatientResponse.class);
            ClientHandler.deleteListener(GetRecentPatientResponse.class);
            ClientHandler.deleteListener(GetDistrictResponse.class);
            ClientHandler.deleteListener(GetWardResponse.class);
            setVisible(false);
        });
        ButtonPanel.add(closeButton);

        saveButton = new JButton("Add to Checkup");
        saveButton.addActionListener(e -> {
            int patientId = Integer.parseInt(patientIdField.getText());
            int doctorId = doctorComboBox.getSelectedIndex()+ 1;
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddCheckupRequest(patientId, doctorId, LocalStorage.userId));

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
