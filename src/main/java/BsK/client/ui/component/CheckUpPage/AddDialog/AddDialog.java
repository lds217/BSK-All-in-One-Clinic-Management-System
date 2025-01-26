package BsK.client.ui.component.CheckUpPage.AddDialog;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.GetDoctorGeneralInfo;
import BsK.common.packet.req.GetRecentPatientRequest;
import BsK.common.packet.res.GetDoctorGeneralInfoResponse;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.packet.res.GetRecentPatientResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


@Slf4j
public class AddDialog extends JDialog {
    private JTextField patientNameField;
    private JTextField patientYearField;
    private JTextField patientIdField;
    private JTextField patientPhoneField;
    private JComboBox patientGenderField;
    private JComboBox villageComboBox, districtComboBox, provinceComboBox;
    private String[] villagesOptions, districtOptions, provinceOptions;
    private DefaultTableModel patientTableModel;
    private JTable patientTable;
    private String[] patientColumns = {"Patient ID", "Patient Name", "Patient Year", "Patient Phone" ,"Patient Address"};
    private String[][] patientData;
    private final ResponseListener<GetRecentPatientResponse> getRecentPatientResponseListener = this::getRecentPatientHandler;
    private JComboBox doctorComboBox;

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


    public AddDialog(Frame parent) {
        super(parent, "Add Patient", true);

        // Set size of the dialog
        setSize(1000, 400);
        // Put in the middle
        setLocationRelativeTo(null);
        setResizable(true);

        // Send request to get the latest 20 patients in the database
        sendGetRecentPatientRequest();


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


        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.weightx = 1.0; // Components stretch horizontally
        gbc.weighty = 0.0; // Allow minor vertical flexibility


        gbc.anchor = GridBagConstraints.WEST; // Align components to the  left
        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Patient name:"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;
        patientNameField = new JTextField(20);
        inputPanel.add(patientNameField, gbc);

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
                    patientTable.clearSelection(); // No matches found
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Doctor:"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;

        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName);
        inputPanel.add(doctorComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Patient year:"), gbc);

        gbc.gridx = 1;
        patientYearField = new JTextField(8);
        inputPanel.add(patientYearField, gbc);

        gbc.gridx = 2;
        inputPanel.add(new JLabel("Patient gender:"), gbc);

        gbc.gridx = 3;
        patientGenderField = new JComboBox<>(new String[]{"Nam", "Nữ"});
        inputPanel.add(patientGenderField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Patient phone:"), gbc);

        gbc.gridx = 1;
        patientPhoneField = new JTextField(8);
        inputPanel.add(patientPhoneField, gbc);


        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        inputPanel.add(new JLabel("Patient ID:"), gbc);


        gbc.gridx = 3;
        gbc.anchor = GridBagConstraints.WEST;
        patientIdField = new JTextField(4);
        inputPanel.add(patientIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Address:"), gbc);

        gbc.gridx = 1;
        provinceOptions = new String[]{"Province 1", "Province 2", "Province 3"};
        provinceComboBox = new JComboBox(provinceOptions);
        inputPanel.add(provinceComboBox, gbc);

        gbc.gridx = 2;
        districtOptions = new String[]{"District 1", "District 2", "District 3"};
        districtComboBox = new JComboBox(districtOptions);
        inputPanel.add(districtComboBox, gbc);

        gbc.gridx = 3;
        villagesOptions = new String[]{"Village 1", "Village 2", "Village 3"};
        villageComboBox = new JComboBox(villagesOptions);
        inputPanel.add(villageComboBox, gbc);



        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, scrollPane);
        splitPane.setResizeWeight(0.1);
        add(splitPane, BorderLayout.CENTER);


        JPanel ButtonPanel = new JPanel();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            setVisible(false);
        });
        ButtonPanel.add(closeButton);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            // Send request to save the patient
        });
        ButtonPanel.add(saveButton);

        add(ButtonPanel, BorderLayout.SOUTH);
    }

    private void handleRowSelection(int selectedRow) {
        if (selectedRow < 0) {
            return;
        }

        String patientId = patientTable.getValueAt(selectedRow, 0).toString();
        String patientName = patientTable.getValueAt(selectedRow, 1).toString();
        String patientYear = patientTable.getValueAt(selectedRow, 2).toString();
        String patientPhone = patientTable.getValueAt(selectedRow, 3).toString();
        String patientAddress = patientTable.getValueAt(selectedRow, 4).toString();
        String patientGender = patientData[selectedRow][5];
        patientIdField.setText(patientId);
        patientNameField.setText(patientName);
        patientYearField.setText(patientYear);
        patientPhoneField.setText(patientPhone);
        patientGenderField.setSelectedItem(patientGender);

        // Extract province, district, village from the address
        String[] addressParts = patientAddress.split(", ");
        if (addressParts.length == 3) {
            String village = addressParts[0];
            String district = addressParts[1];
            String province = addressParts[2];

            villageComboBox.setSelectedItem(village);
            districtComboBox.setSelectedItem(district);
            provinceComboBox.setSelectedItem(province);
        }
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
