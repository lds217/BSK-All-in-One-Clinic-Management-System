package BsK.client.ui.component.CheckUpPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.CheckUpPage.AddDialog.AddDialog;
import BsK.client.ui.component.CheckUpPage.MedicineDialog.MedicineDialog;
import BsK.client.ui.component.CheckUpPage.PrintDialog.MedicineInvoice;
import BsK.client.ui.component.CheckUpPage.ServiceDialog.ServiceDialog;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.QueueViewPage.QueueViewPage;
import BsK.common.entity.Patient;
import BsK.common.entity.Status;
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
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Slf4j
public class CheckUpPage extends JPanel {
    private List<Patient> patientQueue = new ArrayList<>();
    private String[][] rawQueueForTv = new String[][]{};
    private String[][] history;
    private DefaultTableModel model, historyModel;
    private JTable table1, historyTable;
    private final ResponseListener<GetCheckUpQueueUpdateResponse> checkUpQueueUpdateListener = this::handleGetCheckUpQueueUpdateResponse;
    private final ResponseListener<GetCheckUpQueueResponse> checkUpQueueListener = this::handleGetCheckUpQueueResponse;
    private final ResponseListener<GetCustomerHistoryResponse> customerHistoryListener = this::handleGetCustomerHistoryResponse;
    private final ResponseListener<GetDistrictResponse> districtResponseListener = this::handleGetDistrictResponse;
    private final ResponseListener<GetWardResponse> wardResponseListener = this::handleGetWardResponse;
    private final ResponseListener<CallPatientResponse> callPatientResponseListener = this::handleCallPatientResponse;
    private JTextField checkupIdField, customerLastNameField, customerFirstNameField,customerAddressField, customerPhoneField, customerIdField;
    private JTextArea symptomsField, diagnosisField, notesField;
    private JComboBox<String> doctorComboBox, statusComboBox, genderComboBox, provinceComboBox, districtComboBox, wardComboBox;
    private JSpinner customerWeightSpinner, customerHeightSpinner;
    private JDatePickerImpl datePicker, dobPicker;
    private String[][] medicinePrescription;
    private String[][] servicePrescription;
    private String[] doctorOptions;
    private MedicineDialog medDialog = null;
    private ServiceDialog serDialog = null;
    private AddDialog addDialog = null;
    private int previousSelectedRow = -1;
    private boolean saved = false;
    private DefaultComboBoxModel<String> districtModel, wardModel;
    private JComboBox<String> callRoomComboBox;
    private JButton callPatientButton;
    private JLabel callingStatusLabel;

    private QueueViewPage tvQueueFrame;

    boolean returnCell = false;
    public void updateQueue() {
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueRequest());
    }

    public void updateUpdateQueue() {
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueUpdateRequest());
    }

    private String[][] preprocessPatientDataForTable(List<Patient> patients) {
        if (patients == null) {
            return new String[][]{};
        }
        String[][] tableData = new String[patients.size()][8];
        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            tableData[i][0] = p.getCheckupId();
            tableData[i][1] = p.getCheckupDate();
            tableData[i][2] = p.getCustomerLastName();
            tableData[i][3] = p.getCustomerFirstName();
            tableData[i][4] = p.getDoctorName();
            //tableData[i][5] = p.getDiagnosis();
            //tableData[i][6] = p.getNotes();
            tableData[i][5] = p.getStatus();
        }
        return tableData;
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

    public CheckUpPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        ClientHandler.addResponseListener(GetCheckUpQueueResponse.class, checkUpQueueListener);
        ClientHandler.addResponseListener(GetCheckUpQueueUpdateResponse.class, checkUpQueueUpdateListener);
        ClientHandler.addResponseListener(GetCustomerHistoryResponse.class, customerHistoryListener);
        ClientHandler.addResponseListener(GetDistrictResponse.class, districtResponseListener);
        ClientHandler.addResponseListener(GetWardResponse.class, wardResponseListener);
        ClientHandler.addResponseListener(CallPatientResponse.class, callPatientResponseListener);
        updateQueue();

        // Navigation bar
        // navBar panel
        JPanel navBar = new JPanel();
        navBar.setBackground(new Color(63, 81, 181));
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS)); // Use BoxLayout for horizontal layout

        // Left-aligned navigation items (centered vertically)
        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(new Color(63, 81, 181));
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Items on the left
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT); // Vertically center the panel in navBar

        // Add navigation items
        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Kho", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "UserPage", "InfoPage"};
        for (int i = 0; i < navBarItems.length; i++) {
            String item = navBarItems[i];
            String dest = destination[i];
            JLabel label = new JLabel(item);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Change cursor to hand
            label.setFont(new Font("Arial", Font.BOLD, 14));

            // Add padding for visibility
            label.setBorder(BorderFactory.createEmptyBorder(7, 15, 10, 15));

            // Highlight the selected label
            if (item.equals("Thăm khám")) {
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, new Color(33, 150, 243))); // Add bottom border
            }

            // Add a mouse listener to handle click events
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Handle the click event
                    mainFrame.showPage(dest);
                    // Reset background color for all labels
                    for (Component comp : navItemsPanel.getComponents()) {
                        JLabel lbl = (JLabel) comp;
                        lbl.setBackground(null); // Reset background
                        lbl.setOpaque(false); // Remove background color
                    }
                    // Set background for the clicked label
                    label.setBackground(new Color(33, 150, 243)); // Highlight clicked label
                    label.setOpaque(true);
                    label.setForeground(Color.WHITE); // Change text color to white
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    label.setForeground(new Color(200, 230, 255)); // Highlight on hover
                    label.setBackground(new Color(33, 150, 243)); // Highlight the button on hover
                    label.setOpaque(true); // Make the background visible on hover
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (label.getBackground() != new Color(33, 150, 243)) { // Only reset if it's not selected
                        label.setForeground(Color.WHITE); // Restore original color
                        label.setBackground(null); // Remove background color
                        label.setOpaque(false); // Make the background invisible
                    }
                }
            });

            navItemsPanel.add(label);
        }

        // Add a space between the navigation items and the "Welcome" label (this will push the welcome label to the far right)
        navBar.add(navItemsPanel);
        navBar.add(Box.createHorizontalGlue()); // This will push the welcome label to the right

        // Right-aligned "Welcome" label
        JLabel welcomeLabel = new JLabel("Welcome, " + LocalStorage.username + "            ");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Make clickable
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Add mouse interactions for the welcomeLabel
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) { // Check if it's a left-click
                    JPopupMenu popupMenu = new JPopupMenu();

                    JMenuItem profileItem = new JMenuItem("User Profile");
                    JMenuItem settingsItem = new JMenuItem("Settings");
                    JMenuItem logoutItem = new JMenuItem("Logout");

                    profileItem.addActionListener(event -> {
                        System.out.println("User Profile selected");
                        // Placeholder for profile action
                        JOptionPane.showMessageDialog(mainFrame, "User Profile feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
                    });

                    settingsItem.addActionListener(event -> {
                        System.out.println("Settings selected");
                        // Placeholder for settings action
                        JOptionPane.showMessageDialog(mainFrame, "Settings feature coming soon!", "Info", JOptionPane.INFORMATION_MESSAGE);
                    });

                    logoutItem.addActionListener(event -> {
                        System.out.println("Logout selected");
                        // Send LogoutRequest to server
                        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new LogoutRequest());

                        // Clear local storage
                        LocalStorage.username = null;
                        LocalStorage.userId = -1; // Or whatever your default/unauthenticated user ID is
                        // Clear any other sensitive session data from LocalStorage

                        // Navigate to LandingPage
                        mainFrame.showPage("LandingPage");
                    });

                    // Add items to the popup menu
                    popupMenu.add(profileItem);
                    popupMenu.add(settingsItem);
                    popupMenu.addSeparator(); // Adds a visual separator before logout
                    popupMenu.add(logoutItem);

                    // Set preferred width after items are added, height will be based on content
                    int currentPreferredHeight = popupMenu.getPreferredSize().height;
                    popupMenu.setPreferredSize(new Dimension(150, currentPreferredHeight));

                    // Show the popup
                    popupMenu.show(welcomeLabel, 0, welcomeLabel.getHeight());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                welcomeLabel.setForeground(new Color(200, 230, 255)); // Highlight on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                welcomeLabel.setForeground(Color.WHITE); // Restore original color
            }
        });

        // Add the welcome label to the navBar (it will be on the far right due to the horizontal glue)
        navBar.add(welcomeLabel, BorderLayout.EAST);

        // Set preferred size for the navBar
        navBar.setPreferredSize(new Dimension(1200, 50));

        // Now the "Welcome" label is aligned to the right with space between the nav items



        // Data table inside a RoundedPanel
        RoundedPanel leftPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightTopPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightBottomPanel = new RoundedPanel(20, Color.WHITE, false);

        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel titleText1 = new JLabel();
        titleText1.setText("Check Up Queue 1");
        titleText1.setFont(new Font("Arial", Font.BOLD, 16));
        titleText1.setBackground(Color.WHITE);
        titleText1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        callingStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        callingStatusLabel.setFont(new Font("Arial", Font.BOLD, 15));
        callingStatusLabel.setForeground(new Color(0, 100, 0));
        callingStatusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        callingStatusLabel.setOpaque(true);
        callingStatusLabel.setBackground(new Color(230, 255, 230));

        JButton tvQueueButton = new JButton("TV Queue Display");
        tvQueueButton.setBackground(new Color(0, 150, 136)); // A teal color for distinction
        tvQueueButton.setForeground(Color.WHITE);
        tvQueueButton.setFocusPainted(false);
        tvQueueButton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        tvQueueButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        tvQueueButton.addActionListener(e -> {
            if (tvQueueFrame == null || !tvQueueFrame.isDisplayable()) {
                tvQueueFrame = new QueueViewPage();
            } else {
                tvQueueFrame.toFront(); // Bring to front if already open
            }
            tvQueueFrame.updateQueueData(this.rawQueueForTv); // Use rawQueueForTv for TV display
            tvQueueFrame.setVisible(true);
            // tvQueueFrame.optimizeForTv(); // Optional: Call if you want it to try to go fullscreen
        });

        JButton rightButton = new JButton("  ADD  ");
        rightButton.setBackground(new Color(63, 81, 181));
        rightButton.setForeground(Color.WHITE);
        rightButton.setFocusPainted(false);
        rightButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding
        rightButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        rightButton.addActionListener(e -> {
            addDialog = new AddDialog(mainFrame);
            addDialog.setVisible(true);
            updateUpdateQueue();
        });

        topPanel.add(titleText1, BorderLayout.WEST);
        topPanel.add(callingStatusLabel, BorderLayout.CENTER);
        JPanel buttonPanelEast = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanelEast.setOpaque(false); // Make panel transparent
        buttonPanelEast.add(tvQueueButton);
        buttonPanelEast.add(rightButton);
        topPanel.add(buttonPanelEast, BorderLayout.EAST);

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));


        rightBottomPanel.setLayout(new BorderLayout());
        rightBottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        String[] columns = {"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Trạng thái"};
        model = new DefaultTableModel(preprocessPatientDataForTable(this.patientQueue), columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // All cells are non-editable
            }
        };
        table1 = new JTable(model);

        // Set preferred size for the table
        table1.setPreferredScrollableViewportSize(new Dimension(400, 200));

        // Customize the font for the table header and cells
        table1.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table1.setFont(new Font("Arial", Font.PLAIN, 12));
        table1.setRowHeight(25);

        JScrollPane tableScroll1 = new JScrollPane(table1);
        tableScroll1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        leftPanel.add(tableScroll1, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add some padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Patient Info Sub-Panel with TitledBorder ---
        JPanel patientInfoInnerPanel = new JPanel(new GridBagLayout());
        patientInfoInnerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin bệnh nhân",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        GridBagConstraints gbcPatient = new GridBagConstraints();
        gbcPatient.insets = new Insets(3, 3, 3, 3);
        gbcPatient.fill = GridBagConstraints.HORIZONTAL;

        gbcPatient.gridwidth = 1;
        gbcPatient.gridx = 0;
        gbcPatient.gridy = 0;
        patientInfoInnerPanel.add(new JLabel("Họ"), gbcPatient);
        gbcPatient.gridx = 1;
        customerLastNameField = new JTextField(10); // Adjusted preferred size
        patientInfoInnerPanel.add(customerLastNameField, gbcPatient);
        gbcPatient.gridx = 2;
        patientInfoInnerPanel.add(new JLabel("Tên"), gbcPatient);
        gbcPatient.gridx = 3;
        customerFirstNameField = new JTextField(10); // Adjusted preferred size
        patientInfoInnerPanel.add(customerFirstNameField, gbcPatient);

        gbcPatient.gridx = 0;
        gbcPatient.gridy++;
        patientInfoInnerPanel.add(new JLabel("Giới tính"), gbcPatient);
        gbcPatient.gridx = 1;
        String[] genderOptions = {"Nam", "Nữ"};
        genderComboBox = new JComboBox<>(genderOptions);
        patientInfoInnerPanel.add(genderComboBox, gbcPatient);
        gbcPatient.gridx = 2;
        patientInfoInnerPanel.add(new JLabel("Ngày sinh"), gbcPatient);
        gbcPatient.gridx = 3;
        UtilDateModel dobModel = new UtilDateModel();
        Properties dobProperties = new Properties();
        dobProperties.put("text.today", "Today");
        dobProperties.put("text.month", "Month");
        dobProperties.put("text.year", "Year");
        JDatePanelImpl dobPanel = new JDatePanelImpl(dobModel, dobProperties);
        dobPicker = new JDatePickerImpl(dobPanel, new DateLabelFormatter());
        dobPicker.setPreferredSize(new Dimension(120, 30)); // Adjusted preferred size
        patientInfoInnerPanel.add(dobPicker, gbcPatient);

        gbcPatient.gridx = 0;
        gbcPatient.gridy++;
        patientInfoInnerPanel.add(new JLabel("Địa chỉ"), gbcPatient);
        gbcPatient.gridx = 1;
        gbcPatient.gridwidth = 3;
        customerAddressField = new JTextField();
        patientInfoInnerPanel.add(customerAddressField, gbcPatient);

        gbcPatient.gridy++;
        gbcPatient.gridx = 1; // Aligned with address field above
        gbcPatient.gridwidth = 1;
        provinceComboBox = new JComboBox<>(LocalStorage.provinces);
        patientInfoInnerPanel.add(provinceComboBox, gbcPatient);
        gbcPatient.gridx = 2;
        districtModel = new DefaultComboBoxModel<>(new String[]{"Huyện"}); // Placeholder for district
        districtComboBox = new JComboBox<>(districtModel);
        patientInfoInnerPanel.add(districtComboBox, gbcPatient);
        districtComboBox.setEnabled(false);
        gbcPatient.gridx = 3;
        wardModel = new DefaultComboBoxModel<>(new String[]{"Phường"}); // Placeholder for ward
        wardComboBox = new JComboBox<>(wardModel);
        patientInfoInnerPanel.add(wardComboBox, gbcPatient);
        wardComboBox.setEnabled(false);

        provinceComboBox.addActionListener(e -> {
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            if (selectedProvince != null && !selectedProvince.equals("Tỉnh/Thành phố")) {
                String provinceId = LocalStorage.provinceToId.get(selectedProvince);
                if (provinceId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDistrictRequest(provinceId));
                    districtComboBox.setEnabled(false);
                    districtModel.removeAllElements();
                    districtModel.addElement("Loading districts...");
                    wardComboBox.setEnabled(false);
                    wardModel.removeAllElements();
                    wardModel.addElement("Phường"); // Reset ward
                }
            } else {
                districtComboBox.setEnabled(false);
                districtModel.removeAllElements();
                districtModel.addElement("Huyện");
                wardComboBox.setEnabled(false);
                wardModel.removeAllElements();
                wardModel.addElement("Phường");
            }
        });

        districtComboBox.addActionListener(e -> {
            String selectedDistrict = (String) districtComboBox.getSelectedItem();
            // Check if selectedDistrict is not null and not the placeholder/loading message
            if (selectedDistrict != null && LocalStorage.districtToId != null && LocalStorage.districtToId.containsKey(selectedDistrict)) {
                String districtId = LocalStorage.districtToId.get(selectedDistrict);
                if (districtId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(districtId));
                    wardComboBox.setEnabled(false);
                    wardModel.removeAllElements();
                    wardModel.addElement("Loading wards...");
                }
            } else {
                 // If no valid district is selected (or district data is not yet loaded), disable and reset wardComboBox
                wardComboBox.setEnabled(false);
                wardModel.removeAllElements();
                wardModel.addElement("Phường");
            }
        });

        gbcPatient.gridx = 0;
        gbcPatient.gridy++;
        patientInfoInnerPanel.add(new JLabel("Mã số BN"), gbcPatient); // Shortened label
        gbcPatient.gridx = 1;
        customerIdField = new JTextField(10);
        customerIdField.setEditable(false);
        patientInfoInnerPanel.add(customerIdField, gbcPatient);
        gbcPatient.gridx = 2;
        patientInfoInnerPanel.add(new JLabel("SĐT"), gbcPatient); // Shortened label
        gbcPatient.gridx = 3;
        customerPhoneField = new JTextField(10);
        patientInfoInnerPanel.add(customerPhoneField, gbcPatient);

        gbcPatient.gridx = 0;
        gbcPatient.gridy++;
        patientInfoInnerPanel.add(new JLabel("Cân nặng (kg)"), gbcPatient);
        gbcPatient.gridx = 1;
        SpinnerModel weightModel = new SpinnerNumberModel(60, 0, 300, 0.5);
        customerWeightSpinner = new JSpinner(weightModel);
        patientInfoInnerPanel.add(customerWeightSpinner, gbcPatient);
        gbcPatient.gridx = 2;
        patientInfoInnerPanel.add(new JLabel("Chiều cao (cm)"), gbcPatient);
        gbcPatient.gridx = 3;
        SpinnerModel heightModel = new SpinnerNumberModel(170, 0, 230, 0.5);
        customerHeightSpinner = new JSpinner(heightModel);
        patientInfoInnerPanel.add(customerHeightSpinner, gbcPatient);

        // Add patientInfoInnerPanel to the main inputPanel
        gbc.gridx = 0;
        gbc.gridy = 0; // First element in the main inputPanel
        gbc.gridwidth = 4;
        gbc.weightx = 1.0; // Allow horizontal fill
        inputPanel.add(patientInfoInnerPanel, gbc);
        gbc.weightx = 0; // Reset


        // --- Checkup Info Sub-Panel with TitledBorder ---
        JPanel checkupInfoInnerPanel = new JPanel(new GridBagLayout());
        checkupInfoInnerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin khám bệnh",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        GridBagConstraints gbcCheckup = new GridBagConstraints();
        gbcCheckup.insets = new Insets(3, 3, 3, 3);
        gbcCheckup.fill = GridBagConstraints.HORIZONTAL;

        gbcCheckup.gridx = 0;
        gbcCheckup.gridy = 0;
        gbcCheckup.gridwidth = 1;
        gbcCheckup.anchor = GridBagConstraints.NORTHWEST; // Default anchor
        checkupInfoInnerPanel.add(new JLabel("Mã khám bệnh:"), gbcCheckup);
        gbcCheckup.gridx = 1;
        checkupIdField = new JTextField(10);
        checkupIdField.setEditable(false);
        checkupInfoInnerPanel.add(checkupIdField, gbcCheckup);

        UtilDateModel dateModel = new UtilDateModel(); // Renamed from 'model' to avoid conflict
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.setPreferredSize(new Dimension(120, 30));

        gbcCheckup.gridx = 2;
        checkupInfoInnerPanel.add(new JLabel("Đơn Ngày:"), gbcCheckup);
        gbcCheckup.gridx = 3; // Adjusted gridx for datePicker
        checkupInfoInnerPanel.add(datePicker, gbcCheckup);

        gbcCheckup.gridwidth = 1;
        gbcCheckup.gridx = 0;
        gbcCheckup.gridy++;
        checkupInfoInnerPanel.add(new JLabel("Bác Sĩ"), gbcCheckup);
        gbcCheckup.gridwidth = 3;
        gbcCheckup.gridx = 1;
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName != null ? LocalStorage.doctorsName : new String[]{"Loading..."});
        checkupInfoInnerPanel.add(doctorComboBox, gbcCheckup);

        gbcCheckup.gridwidth = 1;
        gbcCheckup.gridx = 0;
        gbcCheckup.gridy++;
        checkupInfoInnerPanel.add(new JLabel("Triệu chứng"), gbcCheckup);
        gbcCheckup.gridwidth = 3;
        gbcCheckup.gridx = 1;
        symptomsField = new JTextArea(3, 20);
        symptomsField.setLineWrap(true);
        symptomsField.setWrapStyleWord(true);
        gbcCheckup.weighty = 1.0; // Allow vertical expansion
        gbcCheckup.fill = GridBagConstraints.BOTH; // Allow expansion in both directions
        checkupInfoInnerPanel.add(new JScrollPane(symptomsField), gbcCheckup); 
        gbcCheckup.weighty = 0.0; // Reset for next components
        gbcCheckup.fill = GridBagConstraints.HORIZONTAL; // Reset fill

        gbcCheckup.gridwidth = 1;
        gbcCheckup.gridx = 0;
        gbcCheckup.gridy++;
        checkupInfoInnerPanel.add(new JLabel("Chẩn đoán"), gbcCheckup);
        gbcCheckup.gridwidth = 3;
        gbcCheckup.gridx = 1;
        diagnosisField = new JTextArea(3, 20);
        diagnosisField.setLineWrap(true);
        diagnosisField.setWrapStyleWord(true);
        gbcCheckup.weighty = 1.0; // Allow vertical expansion
        gbcCheckup.fill = GridBagConstraints.BOTH; // Allow expansion in both directions
        checkupInfoInnerPanel.add(new JScrollPane(diagnosisField), gbcCheckup); 
        gbcCheckup.weighty = 0.0; // Reset for next components
        gbcCheckup.fill = GridBagConstraints.HORIZONTAL; // Reset fill

        gbcCheckup.gridwidth = 1;
        gbcCheckup.gridx = 0;
        gbcCheckup.gridy++;
        checkupInfoInnerPanel.add(new JLabel("Ghi chú"), gbcCheckup);
        gbcCheckup.gridwidth = 3;
        gbcCheckup.gridx = 1;
        notesField = new JTextArea(3, 20);
        notesField.setLineWrap(true);
        notesField.setWrapStyleWord(true);
        gbcCheckup.weighty = 1.0; // Allow vertical expansion
        gbcCheckup.fill = GridBagConstraints.BOTH; // Allow expansion in both directions
        checkupInfoInnerPanel.add(new JScrollPane(notesField), gbcCheckup); 
        gbcCheckup.weighty = 0.0; // Reset for next components
        gbcCheckup.fill = GridBagConstraints.HORIZONTAL; // Reset fill

        gbcCheckup.gridwidth = 1;
        gbcCheckup.gridx = 0;
        gbcCheckup.gridy++;
        checkupInfoInnerPanel.add(new JLabel("Trạng thái"), gbcCheckup);
        gbcCheckup.gridwidth = 3;
        gbcCheckup.gridx = 1;
        String[] statusOptions = {"PROCESSING", "NOT", "DONE"};
        statusComboBox = new JComboBox<>(statusOptions);
        checkupInfoInnerPanel.add(statusComboBox, gbcCheckup);

        // Add checkupInfoInnerPanel to the main inputPanel
        gbc.gridx = 0;
        gbc.gridy = 1; // Second element in the main inputPanel
        gbc.gridwidth = 4;
        gbc.weightx = 1.0; // Allow horizontal fill
        gbc.weighty = 1.0; // Allow checkupInfoInnerPanel to take vertical space
        gbc.insets = new Insets(10, 0, 5, 0); // Add some top margin before this panel
        inputPanel.add(checkupInfoInnerPanel, gbc);
        gbc.weightx = 0; // Reset
        gbc.weighty = 0; // Reset for subsequent components (like the call panel row)
        gbc.insets = new Insets(5,5,5,5); // Reset insets for subsequent components

        // --- Call Patient components --- (now added after the two sub-panels)
        gbc.gridwidth = 1; // Reset gridwidth
        gbc.gridx = 0;
        gbc.gridy = 2; // This is now the third major "row" in inputPanel
        inputPanel.add(new JLabel("Call to Room:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 1;
        String[] roomOptions = {"Room 1", "Room 2", "Room 3"}; 
        callRoomComboBox = new JComboBox<>(roomOptions);
        inputPanel.add(callRoomComboBox, gbc);

        gbc.gridx = 2; 
        gbc.gridwidth = 2; 
        callPatientButton = new JButton("Call Patient");
        callPatientButton.setBackground(new Color(33, 150, 243)); 
        callPatientButton.setForeground(Color.WHITE);
        callPatientButton.setFocusPainted(false);
        callPatientButton.addActionListener(e -> handleCallPatient());
        inputPanel.add(callPatientButton, gbc);

        // Add Free Room button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        JButton freeRoomButton = new JButton("Mark Room as Free");
        freeRoomButton.setBackground(new Color(46, 204, 113)); // Green color
        freeRoomButton.setForeground(Color.WHITE);
        freeRoomButton.setFocusPainted(false);
        freeRoomButton.addActionListener(e -> handleFreeRoom());
        inputPanel.add(freeRoomButton, gbc);

        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Ensure final selection event
                int selectedRow = table1.getSelectedRow();
                if (selectedRow != -1) {
                    if (previousSelectedRow != -1) {
                        if(previousSelectedRow != selectedRow) {
                            if(!saved) {
                                JOptionPane.showMessageDialog(null, "Please save changes before selecting another patient");
                                returnCell = true;
                                table1.setRowSelectionInterval(previousSelectedRow, previousSelectedRow);
                                return;
                            }
                        }
                    }
                    // reset information

                    if (returnCell) {
                        returnCell = false;
                        return;
                    }
                    medicinePrescription = new String[][]{};
                    servicePrescription = new String[][]{};
                    medDialog = null;
                    serDialog = null;
                    handleRowSelection(selectedRow);

                }
            }
        });

        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new GridLayout(1, 5));
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding
        iconPanel.setBackground(Color.WHITE);
        String[] iconName = {"save", "service", "medicine", "printer"};
        for (String name : iconName) {
            ImageIcon originalIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/" + name + ".png");
            Image scaledImage = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH); // Resize to 32x32
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            JLabel iconLabel = new JLabel(scaledIcon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switch (name) {
                        case "service":
                            // If there is no user selected, show a warning message
                            if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Please select a patient from the queue");
                                return;
                            }
                            // Open service dialog
                            if(serDialog == null) {
                                serDialog = new ServiceDialog(mainFrame);
                                previousSelectedRow = table1.getSelectedRow();
                            }
                            serDialog.setVisible(true);
                            saved = false;
                            servicePrescription = serDialog.getServicePrescription();

                            log.info("Service prescription: {}", servicePrescription);
                            break;
                        case "save": {
                            //Warning message
                            int option = JOptionPane.showOptionDialog(null, "Do you want to save changes?",
                                    "Save Changes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                    null, null, null);
                            if (option == JOptionPane.NO_OPTION) {
                                return;
                            }
                            // Save action
                            saved = true;
                            callingStatusLabel.setText(" ");
                            callingStatusLabel.setBackground(new Color(230, 255, 230));
                            callingStatusLabel.setForeground(new Color(0, 100, 0));
                            break;
                        }
                        case "medicine":
                            // If there is no user selected, show a warning message
                            if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Please select a patient from the queue");
                                return;
                            }
                            // Open medicine dialog
                            if(medDialog == null) {
                                medDialog = new MedicineDialog(mainFrame);
                                previousSelectedRow = table1.getSelectedRow();
                            }
                            medDialog.setVisible(true);
                            saved = false;
                            medicinePrescription = medDialog.getMedicinePrescription();

                            log.info("Medicine prescription: {}", medicinePrescription);

                            break;
                        case "printer":
                            // Print action


                            MedicineInvoice medicineInvoice = new MedicineInvoice(checkupIdField.getText(),
                                    customerLastNameField.getText() + customerFirstNameField.getText(),
                                    dobPicker.getJFormattedTextField().getText(), customerPhoneField.getText(),
                                    genderComboBox.getSelectedItem().toString(), customerAddressField.getText(),
                                    doctorComboBox.getSelectedItem().toString(), diagnosisField.getText(),
                                    notesField.getText(), medicinePrescription);

                            medicineInvoice.createDialog(mainFrame);

                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + name);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    iconLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    iconLabel.setBorder(null);
                }
            });
            iconPanel.add(iconLabel);
        }

        JScrollPane inputScroll = new JScrollPane(inputPanel);
        // Configure scroll settings for smoother scrolling
        inputScroll.getVerticalScrollBar().setUnitIncrement(16); // Increase scroll speed
        inputScroll.getVerticalScrollBar().setBlockIncrement(64); // Increase block increment
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // Add mouse wheel listener for smoother scrolling
        inputScroll.addMouseWheelListener(e -> {
            JScrollBar scrollBar = inputScroll.getVerticalScrollBar();
            int notches = e.getWheelRotation();
            int currentValue = scrollBar.getValue();
            int newValue = currentValue + (notches * scrollBar.getUnitIncrement() * 2);
            scrollBar.setValue(newValue);
        });

        rightBottomPanel.add(inputScroll, BorderLayout.CENTER);
        rightBottomPanel.add(iconPanel, BorderLayout.SOUTH);

        // Apply TitledBorder to rightBottomPanel
        rightBottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10), // Outer padding
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Chi tiết và Thao tác",
                        TitledBorder.LEADING,
                        TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16),
                        new Color(63, 81, 181)
                )
        ));

        // Right top panel
        rightTopPanel.setLayout(new BorderLayout());
        // Apply TitledBorder to rightTopPanel
        rightTopPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10), // Outer padding to keep content away from split pane edge
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Lịch sử khám bệnh",
                        TitledBorder.LEADING,
                        TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16),
                        new Color(63, 81, 181)
                )
        ));

        String historyColumns[] = {"Ngày Tháng","Mã khám bệnh", "Triệu chứng", "Chẩn đoán", "Ghi chú"};

        historyModel = new DefaultTableModel(this.history, historyColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // All cells are non-editable
            }
        };
        historyTable = new JTable(historyModel);

        // Customize the font for the table header and cells
        historyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
        historyTable.setRowHeight(25); // Increased row height

        // Add a mouse listener for future click functionality
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { // Single click
                    int selectedRow = historyTable.getSelectedRow();
                    if (selectedRow != -1) {
                        // Placeholder for showing more details
                        System.out.println("Clicked on history row: " + selectedRow);
                        // String checkupId = (String) historyTable.getValueAt(selectedRow, 1); // Assuming Mã khám bệnh is at index 1
                        // JOptionPane.showMessageDialog(CheckUpPage.this, "Details for Checkup ID: " + checkupId + " will be shown here.", "History Details", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        JScrollPane tableScroll2 = new JScrollPane(historyTable);
        tableScroll2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        rightTopPanel.add(tableScroll2, BorderLayout.CENTER);


        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder()); // Remove border
        JSplitPane splitPaneRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightTopPanel, rightBottomPanel);
        splitPaneRight.setResizeWeight(0.5); // Split
        splitPaneRight.setDividerSize(5); // Set divider size

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, splitPaneRight);
        splitPane.setResizeWeight(0.8); // Split
        splitPane.setDividerSize(5); // Set divider size

        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Remove border

        add(navBar, BorderLayout.NORTH);
        //add(topbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void handleRowSelection(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= patientQueue.size()) {
            log.warn("Selected row index out of bounds: {}", selectedRow);
            return;
        }
        Patient selectedPatient = patientQueue.get(selectedRow);

        checkupIdField.setText(selectedPatient.getCheckupId());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date parsedDate;
            String dateStr = selectedPatient.getCheckupDate();
            if (dateStr.matches("\\d+")) {
                long timestamp = Long.parseLong(dateStr);
                parsedDate = new Date(timestamp);
            } else {
                parsedDate = dateFormat.parse(dateStr);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            datePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Invalid date format or timestamp: " + selectedPatient.getCheckupDate());
        }

        SimpleDateFormat dobFormat;
        try {
            Date parsedDate;
            String dobStr = selectedPatient.getCustomerDob();
            if (dobStr.matches("\\d+")) {
                long timestamp = Long.parseLong(dobStr);
                parsedDate = new Date(timestamp);
            } else {
                dobFormat = new SimpleDateFormat("dd/MM/yyyy");
                parsedDate = dobFormat.parse(dobStr);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            dobPicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dobPicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Invalid date format: " + selectedPatient.getCustomerDob());
        }

        customerLastNameField.setText(selectedPatient.getCustomerLastName());
        customerFirstNameField.setText(selectedPatient.getCustomerFirstName());
        doctorComboBox.setSelectedItem(selectedPatient.getDoctorName());
        symptomsField.setText(selectedPatient.getSymptoms());
        diagnosisField.setText(selectedPatient.getDiagnosis());
        notesField.setText(selectedPatient.getNotes());
        statusComboBox.setSelectedItem(selectedPatient.getStatus());
        customerIdField.setText(selectedPatient.getCustomerId());
        customerPhoneField.setText(selectedPatient.getCustomerNumber());
        customerAddressField.setText(selectedPatient.getCustomerAddress());
        customerWeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerWeight()));
        customerHeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerHeight()));
        genderComboBox.setSelectedItem(selectedPatient.getCustomerGender());

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCustomerHistoryRequest(Integer.parseInt(selectedPatient.getCustomerId())));

        // Extract province, district, ward from the address
        String fullAddress = selectedPatient.getCustomerAddress();
        String[] addressParts = fullAddress.split(", ");
        if (addressParts.length == 4) {
            String address = addressParts[0];
            String ward = addressParts[1];
            String district = addressParts[2];
            String province = addressParts[3];
            customerAddressField.setText(address);
            int provinceIdx = findProvinceIndex(province);
            if (provinceIdx != -1) provinceComboBox.setSelectedIndex(provinceIdx);
            try {
                Thread.sleep(100);
                int districtIdx = findDistrictIndex(district);
                if (districtIdx != -1) districtComboBox.setSelectedIndex(districtIdx);

                Thread.sleep(100);
                int wardIdx = findWardIndex(ward);
                if (wardIdx != -1) wardComboBox.setSelectedIndex(wardIdx);
            } catch (InterruptedException e) {
                log.error("Interrupted during address component selection", e);
                Thread.currentThread().interrupt(); // Restore interruption status
            }
        } else {
            customerAddressField.setText(fullAddress);
            // Reset dependent combo boxes
            provinceComboBox.setSelectedIndex(0);
            districtModel.removeAllElements();
            districtModel.addElement("Huyện");
            districtComboBox.setEnabled(false);
            wardModel.removeAllElements();
            wardModel.addElement("Phường");
            wardComboBox.setEnabled(false);
        }
    }

    private void handleGetDistrictResponse(GetDistrictResponse response) {
        log.info("Received district data");

        LocalStorage.districts = response.getDistricts();
        LocalStorage.districtToId = response.getDistrictToId();
        districtModel.removeAllElements(); // Clear previous items
        for (String district : LocalStorage.districts) {
            districtModel.addElement(district);
        }
        districtComboBox.setModel(districtModel);
        districtComboBox.setEnabled(true); // Enable district combo box
        // Automatically select the first actual district if available, or keep placeholder
        if (LocalStorage.districts.length > 1) { // more than just the placeholder
            districtComboBox.setSelectedIndex(0); // Should be the placeholder "Huyện" or "Quận/Huyện"
        } else if (LocalStorage.districts.length == 1 && !LocalStorage.districts[0].startsWith("Loading")){
             districtComboBox.setSelectedIndex(0); // Only one actual district
        }

        // Reset ward combo box as district has changed
        wardModel.removeAllElements();
        wardModel.addElement("Phường");
        wardComboBox.setModel(wardModel);
        wardComboBox.setEnabled(false); // Ward should be fetched after district selection
    }

    private void handleGetWardResponse(GetWardResponse response) {
        log.info("Received ward data");
        LocalStorage.wards = response.getWards();
        // Assuming wardToId is not strictly needed on client unless for specific reverse lookups
        // If it is, it should be part of GetWardResponse and stored in LocalStorage
        wardModel.removeAllElements(); // Clear previous items
        for (String ward : LocalStorage.wards) {
            wardModel.addElement(ward);
        }
        wardComboBox.setModel(wardModel);
        wardComboBox.setEnabled(true); // Enable ward combo box
        if (LocalStorage.wards.length > 0 && !LocalStorage.wards[0].startsWith("Loading")) {
            wardComboBox.setSelectedIndex(0); // Select the placeholder or first ward
        }
    }

    private void handleGetCustomerHistoryResponse(GetCustomerHistoryResponse response) {
        log.info("Received customer history");
        this.history = response.getHistory();
        historyModel.setDataVector(this.history, new String[]{"Ngày Tháng","Mã khám bệnh", "Triệu chứng", "Chẩn đoán", "Ghi chú"});
    }

    private  void handleGetDoctorGeneralInfoResponse(GetDoctorGeneralInfoResponse response) {
        log.info("Received doctor general info");
        this.doctorOptions = response.getDoctorsName();
        LocalStorage.doctorsName = response.getDoctorsName();
    }

    private void handleGetCheckUpQueueUpdateResponse(GetCheckUpQueueUpdateResponse response) {
        log.info("Received checkup update queue");
        this.rawQueueForTv = response.getQueue(); // Store raw data for TV
        this.patientQueue.clear();
        if (this.rawQueueForTv != null) {
            for (String[] patientData : this.rawQueueForTv) {
                this.patientQueue.add(new Patient(patientData));
            }
        }
        model.setDataVector(preprocessPatientDataForTable(this.patientQueue), new String[]{"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Trạng thái"});
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            tvQueueFrame.updateQueueData(this.rawQueueForTv);
        }
    }

    private void handleGetCheckUpQueueResponse(GetCheckUpQueueResponse response) {
        log.info("Received checkup queue");
        this.rawQueueForTv = response.getQueue(); // Store raw data for TV
        this.patientQueue.clear();
        if (this.rawQueueForTv != null) {
            for (String[] patientData : this.rawQueueForTv) {
                this.patientQueue.add(new Patient(patientData));
            }
        }
        model.setDataVector(preprocessPatientDataForTable(this.patientQueue), new String[]{"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Trạng thái"});
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            tvQueueFrame.updateQueueData(this.rawQueueForTv);
        }
    }

    private void handleCallPatientResponse(CallPatientResponse response) {
        log.info("Received call patient response: Room {}, Patient ID {}, Status {}", response.getRoomId(), response.getPatientId(), response.getStatus());
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            if (response.getStatus() == Status.PROCESSING) {
                String patientIdToFind = String.valueOf(response.getPatientId());
                String patientDisplayInfo = patientIdToFind; // Default to ID if not found

                if (this.patientQueue != null) {
                    for (Patient patient : this.patientQueue) {
                        if (patient != null && patientIdToFind.equals(patient.getCheckupId())) {
                            String ho = patient.getCustomerLastName();
                            String ten = patient.getCustomerFirstName();
                            String customerDob = patient.getCustomerDob();
                            String namSinh = "N/A";

                            try {
                                SimpleDateFormat dobFormat;
                                Date parsedDate;
                                if (customerDob.matches("\\d+")) {
                                    long timestamp = Long.parseLong(customerDob);
                                    parsedDate = new Date(timestamp);
                                } else {
                                    dobFormat = new SimpleDateFormat("dd/MM/yyyy");
                                    parsedDate = dobFormat.parse(customerDob);
                                }
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(parsedDate);
                                namSinh = String.valueOf(calendar.get(Calendar.YEAR));
                            } catch (ParseException | NumberFormatException e) {
                                log.error("Error parsing DoB for TV display: {} for patient ID {}", customerDob, patientIdToFind, e);
                            }
                            patientDisplayInfo = ho + " " + ten + " (" + namSinh + ")";
                            break; 
                        }
                    }
                }
                tvQueueFrame.updateSpecificRoomStatus(response.getRoomId(), String.valueOf(response.getPatientId()), patientDisplayInfo, response.getStatus());
            } else if (response.getStatus() == Status.EMPTY) {
                tvQueueFrame.markRoomAsFree(response.getRoomId());
            }
        }
    }

    private void handleErrorResponse(ErrorResponse response) {
        log.error("Error response: {}", response.getError());
    }

    private void handleFreeRoom() {
        int selectedRoomIndex = callRoomComboBox.getSelectedIndex();
        if (selectedRoomIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to mark as free.", "No Room Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedRoomName = callRoomComboBox.getSelectedItem().toString();
        int roomId = selectedRoomIndex + 1; // 1-based room ID

        // Update the calling status label to show room is free
        String freeText = "<html><b>Room Status:</b> " + selectedRoomName + " is now free</html>";
        callingStatusLabel.setText(freeText);
        callingStatusLabel.setForeground(new Color(0, 100, 0));
        callingStatusLabel.setBackground(new Color(230, 255, 230)); // Light green background

        // Show confirmation message
        JOptionPane.showMessageDialog(this,
                "Room " + selectedRoomName + " has been marked as free.",
                "Room Status Updated",
                JOptionPane.INFORMATION_MESSAGE);

        log.info("Room {} marked as free", selectedRoomName);

        // Update the TV Queue display as well
        // if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
        //     tvQueueFrame.markRoomAsFree(roomId);
        // }
        // TODO: Consider sending a packet to the server to notify other clients/instances about the room being free.
        // For example: NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new UpdateRoomStatusRequest(roomId, Status.FREE));
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new CallPatientRequest(roomId, -1, Status.EMPTY));
    }

    private void handleCallPatient() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a patient from the queue to call.", "No Patient Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (selectedRow < 0 || selectedRow >= patientQueue.size()){
             JOptionPane.showMessageDialog(this, "Invalid patient selection.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Patient selectedPatient = patientQueue.get(selectedRow);
        String maKhamBenh = selectedPatient.getCheckupId();
        String patientName = selectedPatient.getCustomerLastName() + " " + selectedPatient.getCustomerFirstName();

        int selectedRoom = callRoomComboBox.getSelectedIndex() + 1; 

        JOptionPane.showMessageDialog(this,
                "Calling patient " + patientName + " (ID: " + maKhamBenh + ") to Room " + selectedRoom, // Corrected room display
                "Calling Patient",
                JOptionPane.INFORMATION_MESSAGE);

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new CallPatientRequest(selectedRoom, Integer.parseInt(maKhamBenh),
                                                                                    Status.PROCESSING));
        log.info("Called patient {} (ID: {}) to room {}", patientName, maKhamBenh, selectedRoom);

        String callingText = "<html><b>Calling:</b> Patient " + patientName + " (ID: " + maKhamBenh + ") to Room " + selectedRoom + "</html>"; // Corrected room display
        callingStatusLabel.setText(callingText);
        callingStatusLabel.setForeground(Color.WHITE);
        callingStatusLabel.setBackground(new Color(217, 83, 79));
    }
}

