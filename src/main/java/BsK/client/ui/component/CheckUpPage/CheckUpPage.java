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
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
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
    private JComboBox<String> doctorComboBox, statusComboBox, genderComboBox, provinceComboBox, districtComboBox, wardComboBox, checkupTypeComboBox;
    private JSpinner customerWeightSpinner, customerHeightSpinner;
    private JDatePickerImpl datePicker, dobPicker;
    private String[][] medicinePrescription = new String[0][0]; // Initialize to empty
    private String[][] servicePrescription = new String[0][0]; // Initialize to empty
    private String[] doctorOptions;
    private MedicineDialog medDialog = null;
    private ServiceDialog serDialog = null;
    private AddDialog addDialog = null;
    private int previousSelectedRow = -1;
    private boolean saved = true; // Initially true, changed when patient selected or dialog opened.
    private DefaultComboBoxModel<String> districtModel, wardModel;
    private JComboBox<String> callRoomComboBox;
    private JButton callPatientButton;
    private JLabel callingStatusLabel;
    private JLabel activeNavItem = null; // To keep track of the active navigation item

    private QueueViewPage tvQueueFrame;

    // New UI components for prescription display
    private JPanel prescriptionDisplayPanel;
    private JTree prescriptionTree;
    private DefaultTreeModel prescriptionTreeModel;
    private DefaultMutableTreeNode rootPrescriptionNode;
    private JLabel totalMedCostLabel;
    private JLabel totalSerCostLabel;
    private JLabel overallTotalCostLabel;
    private static final DecimalFormat df = new DecimalFormat("#,##0");

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
            tableData[i][5] = p.getCheckupType();
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
        JPanel navBar = new JPanel();
        navBar.setBackground(new Color(63, 81, 181));
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS)); 

        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(new Color(63, 81, 181));
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 15)); // Increased vgap to 15
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT); 

        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Kho", "Thanh toán", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "CheckoutPage", "UserPage", "InfoPage"};
        String[] iconFiles = {"dashboard.png", "health-check.png", "database.png", "warehouse.png", "cashier-machine.png", "user.png", "info.png"}; // Icon files corresponding to items

        final Color defaultNavColor = new Color(63, 81, 181); // Base navbar color (transparent items)
        final Color hoverNavColor = new Color(50, 70, 170); // Darker for hover
        final Color activeNavColor = new Color(33, 150, 243); // Existing active/click color

        // Define borders
        final Border defaultNavItemBorder = BorderFactory.createEmptyBorder(12, 15, 12, 15); // Increased top/bottom padding to 12
        final Border activePageSpecificBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, activeNavColor), 
                BorderFactory.createEmptyBorder(12, 15, 9, 15) // Adjusted padding for the border (12 top, 9 bottom to account for 3px border)
        );

        for (int i = 0; i < navBarItems.length; i++) {
            final String itemText = navBarItems[i];
            final String dest = destination[i];
            String iconFileName = iconFiles[i];

            final JLabel label = new JLabel(itemText);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setBorder(defaultNavItemBorder); // Apply default padding
            label.setOpaque(false); // Default is transparent

            try {
                String iconPath = "src/main/java/BsK/client/ui/assets/icon/" + iconFileName;
                ImageIcon originalIcon = new ImageIcon(iconPath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(36, 36, Image.SCALE_SMOOTH); // Icon size 36x36
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                label.setIcon(scaledIcon);
            } catch (Exception e) {
                log.error("Error loading icon: {} for nav item: {}", iconFileName, itemText, e);
            }

            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setHorizontalTextPosition(SwingConstants.CENTER);

            // Initial active state for "Thăm khám"
            if (itemText.equals("Thăm khám")) {
                label.setBorder(activePageSpecificBorder);
                label.setBackground(activeNavColor); // Set background for active
                label.setOpaque(true);
                activeNavItem = label; // Set as initially active
            }

            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (activeNavItem != null && activeNavItem != label) {
                        // Reset previous active item
                        activeNavItem.setBackground(defaultNavColor); // Or null for transparent against navBar
                        activeNavItem.setOpaque(false);
                        activeNavItem.setForeground(Color.WHITE);
                        activeNavItem.setBorder(defaultNavItemBorder); // Reset to default border
                    }
                    
                    activeNavItem = label;
                    // Apply active styling
                    activeNavItem.setBackground(activeNavColor);
                    activeNavItem.setOpaque(true);
                    activeNavItem.setForeground(Color.WHITE); // Ensure text is white on active background
                    if (itemText.equals("Thăm khám")) { // Specific border for "Thăm khám" page
                        activeNavItem.setBorder(activePageSpecificBorder);
                    } else {
                        // For other items, active state might just be the background + default padding
                        // Or a generic active border if desired. For now, background is the main indicator.
                        activeNavItem.setBorder(defaultNavItemBorder); // Ensure it has correct padding
                    }
                    mainFrame.showPage(dest);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (label != activeNavItem) {
                        label.setForeground(new Color(200, 230, 255));
                        label.setBackground(hoverNavColor);
                        label.setOpaque(true);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (label != activeNavItem) {
                        label.setForeground(Color.WHITE);
                        label.setBackground(defaultNavColor); // Or null
                        label.setOpaque(false);
                    }
                }
            });
            navItemsPanel.add(label);
        }

        navBar.add(navItemsPanel);
        navBar.add(Box.createHorizontalGlue()); 

        JLabel welcomeLabel = new JLabel("Chào, " + LocalStorage.username + "            ");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) { 
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem profileItem = new JMenuItem("Hồ sơ cá nhân");
                    JMenuItem settingsItem = new JMenuItem("Cài đặt");
                    JMenuItem logoutItem = new JMenuItem("Đăng xuất");

                    profileItem.addActionListener(event -> {
                        JOptionPane.showMessageDialog(mainFrame, "Tính năng Hồ sơ cá nhân sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    });
                    settingsItem.addActionListener(event -> {
                        JOptionPane.showMessageDialog(mainFrame, "Tính năng Cài đặt sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    });
                    logoutItem.addActionListener(event -> {
                        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new LogoutRequest());
                        LocalStorage.username = null;
                        LocalStorage.userId = -1; 
                        mainFrame.showPage("LandingPage");
                    });
                    popupMenu.add(profileItem);
                    popupMenu.add(settingsItem);
                    popupMenu.addSeparator(); 
                    popupMenu.add(logoutItem);
                    int currentPreferredHeight = popupMenu.getPreferredSize().height;
                    popupMenu.setPreferredSize(new Dimension(150, currentPreferredHeight));
                    popupMenu.show(welcomeLabel, 0, welcomeLabel.getHeight());
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                welcomeLabel.setForeground(new Color(200, 230, 255)); 
            }
            @Override
            public void mouseExited(MouseEvent e) {
                welcomeLabel.setForeground(Color.WHITE); 
            }
        });
        navBar.add(welcomeLabel, BorderLayout.EAST);
        navBar.setPreferredSize(new Dimension(1200, 85)); // Increased preferred height to 85

        // Panels
        RoundedPanel leftPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightTopPanel = new RoundedPanel(20, Color.WHITE, false); // History Panel
        prescriptionDisplayPanel = new JPanel(new BorderLayout(5,5)); // New Prescription Panel
        RoundedPanel rightBottomPanel = new RoundedPanel(20, Color.WHITE, false); // Details/Actions Panel

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleText1 = new JLabel();
        titleText1.setText("Danh sách chờ khám");
        titleText1.setFont(new Font("Arial", Font.BOLD, 16));
        titleText1.setBackground(Color.WHITE);
        titleText1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        callingStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        callingStatusLabel.setFont(new Font("Arial", Font.BOLD, 15));
        callingStatusLabel.setForeground(new Color(0, 100, 0));
        callingStatusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        callingStatusLabel.setOpaque(true);
        callingStatusLabel.setBackground(new Color(230, 255, 230));

        JButton tvQueueButton = new JButton("Màn hình chờ TV");
        tvQueueButton.setBackground(new Color(0, 150, 136));
        tvQueueButton.setForeground(Color.WHITE);
        tvQueueButton.setFocusPainted(false);
        tvQueueButton.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        tvQueueButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tvQueueButton.addActionListener(e -> {
            if (tvQueueFrame == null || !tvQueueFrame.isDisplayable()) {
                tvQueueFrame = new QueueViewPage();
            } else {
                tvQueueFrame.toFront(); 
            }
            tvQueueFrame.updateQueueData(this.rawQueueForTv); 
            tvQueueFrame.setVisible(true);
        });

        JButton rightButton = new JButton("  THÊM BN  ");
        rightButton.setBackground(new Color(63, 81, 181));
        rightButton.setForeground(Color.WHITE);
        rightButton.setFocusPainted(false);
        rightButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); 
        rightButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rightButton.addActionListener(e -> {
            addDialog = new AddDialog(mainFrame);
            addDialog.setVisible(true);
            updateUpdateQueue();
        });

        topPanel.add(titleText1, BorderLayout.WEST);
        topPanel.add(callingStatusLabel, BorderLayout.CENTER);
        JPanel buttonPanelEast = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanelEast.setOpaque(false); 
        buttonPanelEast.add(tvQueueButton);
        buttonPanelEast.add(rightButton);
        topPanel.add(buttonPanelEast, BorderLayout.EAST);

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        String[] columns = {"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Loại khám"};
        model = new DefaultTableModel(preprocessPatientDataForTable(this.patientQueue), columns) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table1 = new JTable(model);
        table1.setPreferredScrollableViewportSize(new Dimension(400, 200));
        table1.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table1.setFont(new Font("Arial", Font.PLAIN, 12));
        table1.setRowHeight(25);
        JScrollPane tableScroll1 = new JScrollPane(table1);
        tableScroll1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        leftPanel.add(tableScroll1, BorderLayout.CENTER);

        // Right Bottom Panel (Details and Actions) - Setup remains similar
        rightBottomPanel.setLayout(new BorderLayout());
        rightBottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5), 
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Chi tiết và Thao tác",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
                )
        ));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel patientInfoInnerPanel = new JPanel(new GridBagLayout());
        patientInfoInnerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin bệnh nhân",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        GridBagConstraints gbcPatient = new GridBagConstraints();
        gbcPatient.insets = new Insets(3, 3, 3, 3);
        gbcPatient.fill = GridBagConstraints.HORIZONTAL;

        gbcPatient.gridwidth = 1; gbcPatient.gridx = 0; gbcPatient.gridy = 0; patientInfoInnerPanel.add(new JLabel("Họ"), gbcPatient);
        gbcPatient.gridx = 1; customerLastNameField = new JTextField(10); patientInfoInnerPanel.add(customerLastNameField, gbcPatient);
        gbcPatient.gridx = 2; patientInfoInnerPanel.add(new JLabel("Tên"), gbcPatient);
        gbcPatient.gridx = 3; customerFirstNameField = new JTextField(10); patientInfoInnerPanel.add(customerFirstNameField, gbcPatient);
        gbcPatient.gridx = 0; gbcPatient.gridy++; patientInfoInnerPanel.add(new JLabel("Giới tính"), gbcPatient);
        gbcPatient.gridx = 1; String[] genderOptions = {"Nam", "Nữ"}; genderComboBox = new JComboBox<>(genderOptions); patientInfoInnerPanel.add(genderComboBox, gbcPatient);
        gbcPatient.gridx = 2; patientInfoInnerPanel.add(new JLabel("Ngày sinh"), gbcPatient);
        gbcPatient.gridx = 3; UtilDateModel dobModel = new UtilDateModel(); Properties dobProperties = new Properties(); dobProperties.put("text.today", "Hôm nay"); dobProperties.put("text.month", "Tháng"); dobProperties.put("text.year", "Năm"); JDatePanelImpl dobPanel = new JDatePanelImpl(dobModel, dobProperties); dobPicker = new JDatePickerImpl(dobPanel, new DateLabelFormatter()); dobPicker.setPreferredSize(new Dimension(120, 30)); patientInfoInnerPanel.add(dobPicker, gbcPatient);
        gbcPatient.gridx = 0; gbcPatient.gridy++; patientInfoInnerPanel.add(new JLabel("Địa chỉ"), gbcPatient);
        gbcPatient.gridx = 1; gbcPatient.gridwidth = 3; customerAddressField = new JTextField(); patientInfoInnerPanel.add(customerAddressField, gbcPatient);
        gbcPatient.gridy++; gbcPatient.gridx = 1; gbcPatient.gridwidth = 1; provinceComboBox = new JComboBox<>(LocalStorage.provinces); patientInfoInnerPanel.add(provinceComboBox, gbcPatient);
        gbcPatient.gridx = 2; districtModel = new DefaultComboBoxModel<>(new String[]{"Huyện"}); districtComboBox = new JComboBox<>(districtModel); patientInfoInnerPanel.add(districtComboBox, gbcPatient); districtComboBox.setEnabled(false);
        gbcPatient.gridx = 3; wardModel = new DefaultComboBoxModel<>(new String[]{"Phường"}); wardComboBox = new JComboBox<>(wardModel); patientInfoInnerPanel.add(wardComboBox, gbcPatient); wardComboBox.setEnabled(false);

        provinceComboBox.addActionListener(e -> {
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            if (selectedProvince != null && !selectedProvince.equals("Tỉnh/Thành phố")) {
                String provinceId = LocalStorage.provinceToId.get(selectedProvince);
                if (provinceId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDistrictRequest(provinceId));
                    districtComboBox.setEnabled(false); districtModel.removeAllElements(); districtModel.addElement("Đang tải quận/huyện...");
                    wardComboBox.setEnabled(false); wardModel.removeAllElements(); wardModel.addElement("Phường"); 
                }
            } else {
                districtComboBox.setEnabled(false); districtModel.removeAllElements(); districtModel.addElement("Huyện");
                wardComboBox.setEnabled(false); wardModel.removeAllElements(); wardModel.addElement("Phường");
            }
        });
        districtComboBox.addActionListener(e -> {
            String selectedDistrict = (String) districtComboBox.getSelectedItem();
            if (selectedDistrict != null && LocalStorage.districtToId != null && LocalStorage.districtToId.containsKey(selectedDistrict)) {
                String districtId = LocalStorage.districtToId.get(selectedDistrict);
                if (districtId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(districtId));
                    wardComboBox.setEnabled(false); wardModel.removeAllElements(); wardModel.addElement("Đang tải phường/xã...");
                }
            } else {
                wardComboBox.setEnabled(false); wardModel.removeAllElements(); wardModel.addElement("Phường");
            }
        });

        gbcPatient.gridx = 0; gbcPatient.gridy++; patientInfoInnerPanel.add(new JLabel("Mã bệnh nhân"), gbcPatient); 
        gbcPatient.gridx = 1; customerIdField = new JTextField(10); customerIdField.setEditable(false); patientInfoInnerPanel.add(customerIdField, gbcPatient);
        gbcPatient.gridx = 2; patientInfoInnerPanel.add(new JLabel("SĐT"), gbcPatient); 
        gbcPatient.gridx = 3; customerPhoneField = new JTextField(10); patientInfoInnerPanel.add(customerPhoneField, gbcPatient);
        gbcPatient.gridx = 0; gbcPatient.gridy++; patientInfoInnerPanel.add(new JLabel("Cân nặng (kg)"), gbcPatient);
        gbcPatient.gridx = 1; SpinnerModel weightModel = new SpinnerNumberModel(60, 0, 300, 0.5); customerWeightSpinner = new JSpinner(weightModel); patientInfoInnerPanel.add(customerWeightSpinner, gbcPatient);
        gbcPatient.gridx = 2; patientInfoInnerPanel.add(new JLabel("Chiều cao (cm)"), gbcPatient);
        gbcPatient.gridx = 3; SpinnerModel heightModel = new SpinnerNumberModel(170, 0, 230, 0.5); customerHeightSpinner = new JSpinner(heightModel); patientInfoInnerPanel.add(customerHeightSpinner, gbcPatient);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4; gbc.weightx = 1.0; inputPanel.add(patientInfoInnerPanel, gbc); gbc.weightx = 0; 

        JPanel checkupInfoInnerPanel = new JPanel(new GridBagLayout());
        checkupInfoInnerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin khám bệnh",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        GridBagConstraints gbcCheckup = new GridBagConstraints();
        gbcCheckup.insets = new Insets(3, 3, 3, 3);
        gbcCheckup.fill = GridBagConstraints.HORIZONTAL;

        gbcCheckup.gridx = 0; gbcCheckup.gridy = 0; gbcCheckup.gridwidth = 1; gbcCheckup.anchor = GridBagConstraints.NORTHWEST; checkupInfoInnerPanel.add(new JLabel("Mã khám bệnh:"), gbcCheckup);
        gbcCheckup.gridx = 1; checkupIdField = new JTextField(10); checkupIdField.setEditable(false); checkupInfoInnerPanel.add(checkupIdField, gbcCheckup);
        UtilDateModel dateModel = new UtilDateModel(); Properties p = new Properties(); p.put("text.today", "Hôm nay"); p.put("text.month", "Tháng"); p.put("text.year", "Năm"); JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p); datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter()); datePicker.setPreferredSize(new Dimension(120, 30));
        gbcCheckup.gridx = 2; checkupInfoInnerPanel.add(new JLabel("Đơn Ngày:"), gbcCheckup);
        gbcCheckup.gridx = 3; checkupInfoInnerPanel.add(datePicker, gbcCheckup);
        gbcCheckup.gridwidth = 1; gbcCheckup.gridx = 0; gbcCheckup.gridy++; checkupInfoInnerPanel.add(new JLabel("Bác Sĩ"), gbcCheckup);
        gbcCheckup.gridwidth = 3; gbcCheckup.gridx = 1; doctorComboBox = new JComboBox<>(LocalStorage.doctorsName != null ? LocalStorage.doctorsName : new String[]{"Đang tải bác sĩ..."}); checkupInfoInnerPanel.add(doctorComboBox, gbcCheckup);
        gbcCheckup.gridwidth = 1; gbcCheckup.gridx = 0; gbcCheckup.gridy++; checkupInfoInnerPanel.add(new JLabel("Triệu chứng"), gbcCheckup);
        gbcCheckup.gridwidth = 3; gbcCheckup.gridx = 1; symptomsField = new JTextArea(3, 20); symptomsField.setLineWrap(true); symptomsField.setWrapStyleWord(true); gbcCheckup.weighty = 1.0; gbcCheckup.fill = GridBagConstraints.BOTH; checkupInfoInnerPanel.add(new JScrollPane(symptomsField), gbcCheckup); gbcCheckup.weighty = 0.0; gbcCheckup.fill = GridBagConstraints.HORIZONTAL; 
        gbcCheckup.gridwidth = 1; gbcCheckup.gridx = 0; gbcCheckup.gridy++; checkupInfoInnerPanel.add(new JLabel("Chẩn đoán"), gbcCheckup);
        gbcCheckup.gridwidth = 3; gbcCheckup.gridx = 1; diagnosisField = new JTextArea(3, 20); diagnosisField.setLineWrap(true); diagnosisField.setWrapStyleWord(true); gbcCheckup.weighty = 1.0; gbcCheckup.fill = GridBagConstraints.BOTH; checkupInfoInnerPanel.add(new JScrollPane(diagnosisField), gbcCheckup); gbcCheckup.weighty = 0.0; gbcCheckup.fill = GridBagConstraints.HORIZONTAL; 
        gbcCheckup.gridwidth = 1; gbcCheckup.gridx = 0; gbcCheckup.gridy++; checkupInfoInnerPanel.add(new JLabel("Ghi chú"), gbcCheckup);
        gbcCheckup.gridwidth = 3; gbcCheckup.gridx = 1; notesField = new JTextArea(3, 20); notesField.setLineWrap(true); notesField.setWrapStyleWord(true); gbcCheckup.weighty = 1.0; gbcCheckup.fill = GridBagConstraints.BOTH; checkupInfoInnerPanel.add(new JScrollPane(notesField), gbcCheckup); gbcCheckup.weighty = 0.0; gbcCheckup.fill = GridBagConstraints.HORIZONTAL; 
        gbcCheckup.gridwidth = 1; gbcCheckup.gridx = 0; gbcCheckup.gridy++; checkupInfoInnerPanel.add(new JLabel("Trạng thái"), gbcCheckup);
        gbcCheckup.gridwidth = 1; gbcCheckup.gridx = 1; String[] statusOptions = {"ĐANG KHÁM", "CHỜ KHÁM", "ĐÃ KHÁM"}; statusComboBox = new JComboBox<>(statusOptions); checkupInfoInnerPanel.add(statusComboBox, gbcCheckup);
        gbcCheckup.gridx = 2; checkupInfoInnerPanel.add(new JLabel("Loại khám"), gbcCheckup);
        gbcCheckup.gridx = 3; String[] checkupTypeOptions = {"BỆNH", "THAI", "KHÁC"}; checkupTypeComboBox = new JComboBox<>(checkupTypeOptions); checkupInfoInnerPanel.add(checkupTypeComboBox, gbcCheckup);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4; gbc.weightx = 1.0; gbc.weighty = 1.0; gbc.insets = new Insets(10, 0, 5, 0); inputPanel.add(checkupInfoInnerPanel, gbc); gbc.weightx = 0; gbc.weighty = 0; gbc.insets = new Insets(5,5,5,5); 
        
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(new JLabel("Gọi vào phòng:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; String[] roomOptions = {"Phòng 1", "Phòng 2", "Phòng 3"}; callRoomComboBox = new JComboBox<>(roomOptions); inputPanel.add(callRoomComboBox, gbc);
        gbc.gridx = 2; gbc.gridwidth = 2; callPatientButton = new JButton("Gọi bệnh nhân"); callPatientButton.setBackground(new Color(33, 150, 243)); callPatientButton.setForeground(Color.WHITE); callPatientButton.setFocusPainted(false); callPatientButton.addActionListener(e -> handleCallPatient()); inputPanel.add(callPatientButton, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; JButton freeRoomButton = new JButton("Đánh dấu phòng trống"); freeRoomButton.setBackground(new Color(46, 204, 113)); freeRoomButton.setForeground(Color.WHITE); freeRoomButton.setFocusPainted(false); freeRoomButton.addActionListener(e -> handleFreeRoom()); inputPanel.add(freeRoomButton, gbc);

        JScrollPane inputScroll = new JScrollPane(inputPanel);
        inputScroll.getVerticalScrollBar().setUnitIncrement(16); 
        inputScroll.getVerticalScrollBar().setBlockIncrement(64); 
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inputScroll.addMouseWheelListener(e -> {
            JScrollBar scrollBar = inputScroll.getVerticalScrollBar();
            int notches = e.getWheelRotation();
            int currentValue = scrollBar.getValue();
            int newValue = currentValue + (notches * scrollBar.getUnitIncrement() * 2);
            scrollBar.setValue(newValue);
        });
        rightBottomPanel.add(inputScroll, BorderLayout.CENTER);
        
        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new GridLayout(1, 5));
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        iconPanel.setBackground(Color.WHITE);
        String[] iconName = {"save", "service", "medicine", "printer"};
        for (String name : iconName) {
            ImageIcon originalIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/" + name + ".png");
            Image scaledImage = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH); 
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            JLabel iconLabel = new JLabel(scaledIcon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switch (name) {
                        case "service":
                            if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Vui lòng chọn một bệnh nhân từ hàng đợi", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE);
                                return;
                            }
                            if(serDialog == null) {
                                serDialog = new ServiceDialog(mainFrame);
                            }
                            serDialog.setVisible(true);
                            saved = false;
                            servicePrescription = serDialog.getServicePrescription();
                            updatePrescriptionTree(); 
                            log.info("Service prescription: {}", (Object) servicePrescription);
                            break;
                        case "save": {
                            int option = JOptionPane.showOptionDialog(null, "Bạn có muốn lưu các thay đổi?",
                                    "Lưu thay đổi", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                    null, null, null);
                            if (option == JOptionPane.NO_OPTION) {
                                return;
                            }
                            saved = true;
                            callingStatusLabel.setText(" ");
                            callingStatusLabel.setBackground(new Color(230, 255, 230));
                            callingStatusLabel.setForeground(new Color(0, 100, 0));
                            // TODO: Add logic to actually save medicinePrescription and servicePrescription with other checkup data
                            break;
                        }
                        case "medicine":
                            if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Vui lòng chọn một bệnh nhân từ hàng đợi", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE);
                                return;
                            }
                            if(medDialog == null) {
                                medDialog = new MedicineDialog(mainFrame);
                            }
                            medDialog.setVisible(true);
                            saved = false;
                            medicinePrescription = medDialog.getMedicinePrescription();
                            updatePrescriptionTree(); 
                            log.info("Medicine prescription: {}", (Object) medicinePrescription);
                            break;
                        case "printer":
                             if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Vui lòng chọn bệnh nhân trước.", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE); return;
                            }
                            if ((medicinePrescription == null || medicinePrescription.length == 0) && 
                                (servicePrescription == null || servicePrescription.length == 0)) {
                                JOptionPane.showMessageDialog(null, "Không có thuốc hoặc dịch vụ để in.", "Không có dữ liệu", JOptionPane.INFORMATION_MESSAGE); return;
                            }
                            MedicineInvoice medicineInvoice = new MedicineInvoice(checkupIdField.getText(),
                                    customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                                    dobPicker.getJFormattedTextField().getText(), customerPhoneField.getText(),
                                    genderComboBox.getSelectedItem().toString(), 
                                    customerAddressField.getText() + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (districtComboBox.getSelectedItem() != null ? districtComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                                    doctorComboBox.getSelectedItem().toString(), diagnosisField.getText(),
                                    notesField.getText(), medicinePrescription, servicePrescription); 
                            medicineInvoice.createDialog(mainFrame);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + name);
                    }
                }
                @Override public void mouseEntered(MouseEvent e) { iconLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2)); }
                @Override public void mouseExited(MouseEvent e) { iconLabel.setBorder(null); }
            });
            iconPanel.add(iconLabel);
        }
        rightBottomPanel.add(iconPanel, BorderLayout.SOUTH);

        // Right Top Panel (History)
        rightTopPanel.setLayout(new BorderLayout());
        rightTopPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 5, 10), 
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Lịch sử khám bệnh",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
                )
        ));
        String historyColumns[] = {"Ngày Tháng","Mã khám bệnh", "Triệu chứng", "Chẩn đoán", "Ghi chú"};
        historyModel = new DefaultTableModel(this.history, historyColumns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
        historyTable.setRowHeight(25); 
        historyTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { 
                    int selectedRow = historyTable.getSelectedRow();
                    // if (selectedRow != -1) { System.out.println("Clicked on history row: " + selectedRow); }
                }
            }
        });
        JScrollPane tableScroll2 = new JScrollPane(historyTable);
        tableScroll2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        rightTopPanel.add(tableScroll2, BorderLayout.CENTER);

        // Prescription Display Panel Setup
        prescriptionDisplayPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 10, 5, 10), 
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                "Đơn thuốc và Dịch vụ",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
            )
        ));
        rootPrescriptionNode = new DefaultMutableTreeNode("Đơn hiện tại");
        prescriptionTreeModel = new DefaultTreeModel(rootPrescriptionNode);
        prescriptionTree = new JTree(prescriptionTreeModel);
        prescriptionTree.setFont(new Font("Arial", Font.PLAIN, 13));
        prescriptionTree.setRowHeight(20);
        JScrollPane treeScrollPane = new JScrollPane(prescriptionTree);
        prescriptionDisplayPanel.add(treeScrollPane, BorderLayout.CENTER);
        
        JPanel totalCostPanel = new JPanel(new GridLayout(0,1, 5,5)); 
        totalCostPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        totalMedCostLabel = new JLabel("Tổng tiền thuốc: 0 VNĐ");
        totalMedCostLabel.setFont(new Font("Arial", Font.BOLD, 13));
        totalSerCostLabel = new JLabel("Tổng tiền dịch vụ: 0 VNĐ");
        totalSerCostLabel.setFont(new Font("Arial", Font.BOLD, 13));
        overallTotalCostLabel = new JLabel("TỔNG CỘNG: 0 VNĐ");
        overallTotalCostLabel.setFont(new Font("Arial", Font.BOLD, 14));
        overallTotalCostLabel.setForeground(Color.RED);
        totalCostPanel.add(totalMedCostLabel);
        totalCostPanel.add(totalSerCostLabel);
        totalCostPanel.add(new JSeparator());
        totalCostPanel.add(overallTotalCostLabel);
        prescriptionDisplayPanel.add(totalCostPanel, BorderLayout.SOUTH);
        updatePrescriptionTree(); // Initialize tree display

        // Split Panes Configuration
        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
        
        JSplitPane prescriptionAndDetailsPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, prescriptionDisplayPanel, rightBottomPanel);
        prescriptionAndDetailsPane.setResizeWeight(0.4); 
        prescriptionAndDetailsPane.setDividerSize(5);

        JSplitPane splitPaneRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightTopPanel, prescriptionAndDetailsPane);
        splitPaneRight.setResizeWeight(0.2);
        splitPaneRight.setDividerSize(5); 

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, splitPaneRight);
        splitPane.setResizeWeight(0.4); 
        splitPane.setDividerSize(5); 
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 

        add(navBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    SwingUtilities.invokeLater(() -> {
                        int selectedRow = table1.getSelectedRow();
                        if (selectedRow != -1) {
                            if (previousSelectedRow == selectedRow && !saved) {
                                int confirm = JOptionPane.showConfirmDialog(
                                        CheckUpPage.this,
                                        "Các thay đổi chưa được lưu. Bạn có chắc chắn muốn chuyển sang bệnh nhân khác không?",
                                        "Xác nhận chuyển bệnh nhân",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE);
                                if (confirm == JOptionPane.NO_OPTION) {
                                    return;
                                }
                            }
                            handleRowSelection(selectedRow);
                        }
                    });
                }
            }
        });
    }
    
    private void updatePrescriptionTree() {
        rootPrescriptionNode.removeAllChildren();
        double totalMedicineCost = 0;
        double totalServiceCost = 0;

        if (medicinePrescription != null && medicinePrescription.length > 0) {
            DefaultMutableTreeNode medicinesNode = new DefaultMutableTreeNode("Thuốc (" + medicinePrescription.length + ")");
            for (String[] med : medicinePrescription) {
                if (med == null || med.length < 10) continue; 
                String medDisplay = String.format("%s - SL: %s %s", med[1], med[2], med[3]);
                DefaultMutableTreeNode medNode = new DefaultMutableTreeNode(medDisplay);
                medNode.add(new DefaultMutableTreeNode("Liều dùng: Sáng " + med[4] + " / Trưa " + med[5] + " / Chiều " + med[6]));
                try {
                     medNode.add(new DefaultMutableTreeNode("Đơn giá: " + df.format(Double.parseDouble(med[7])) + " VNĐ"));
                     medNode.add(new DefaultMutableTreeNode("Thành tiền: " + df.format(Double.parseDouble(med[8])) + " VNĐ"));
                     totalMedicineCost += Double.parseDouble(med[8]);
                } catch (NumberFormatException e) { 
                    log.error("Error parsing medicine cost: {}", med[7] + " or " + med[8]); 
                    medNode.add(new DefaultMutableTreeNode("Đơn giá: Lỗi"));
                    medNode.add(new DefaultMutableTreeNode("Thành tiền: Lỗi"));
                }
                if (med[9] != null && !med[9].isEmpty()) {
                    medNode.add(new DefaultMutableTreeNode("Ghi chú: " + med[9]));
                }
                medicinesNode.add(medNode);
            }
            rootPrescriptionNode.add(medicinesNode);
        }

        if (servicePrescription != null && servicePrescription.length > 0) {
            DefaultMutableTreeNode servicesNode = new DefaultMutableTreeNode("Dịch vụ (" + servicePrescription.length + ")");
            for (String[] ser : servicePrescription) {
                 if (ser == null || ser.length < 6) continue; 
                String serDisplay = String.format("%s - SL: %s", ser[1], ser[2]);
                DefaultMutableTreeNode serNode = new DefaultMutableTreeNode(serDisplay);
                try {
                    serNode.add(new DefaultMutableTreeNode("Đơn giá: " + df.format(Double.parseDouble(ser[3])) + " VNĐ"));
                    serNode.add(new DefaultMutableTreeNode("Thành tiền: " + df.format(Double.parseDouble(ser[4])) + " VNĐ"));
                    totalServiceCost += Double.parseDouble(ser[4]); 
                } catch (NumberFormatException e) { 
                    log.error("Error parsing service cost: {}", ser[3] + " or " + ser[4]);
                    serNode.add(new DefaultMutableTreeNode("Đơn giá: Lỗi"));
                    serNode.add(new DefaultMutableTreeNode("Thành tiền: Lỗi"));
                }
                 if (ser[5] != null && !ser[5].isEmpty()) {
                    serNode.add(new DefaultMutableTreeNode("Ghi chú: " + ser[5]));
                }
                servicesNode.add(serNode);
            }
            rootPrescriptionNode.add(servicesNode);
        }
        
        totalMedCostLabel.setText("Tổng tiền thuốc: " + df.format(totalMedicineCost) + " VNĐ");
        totalSerCostLabel.setText("Tổng tiền dịch vụ: " + df.format(totalServiceCost) + " VNĐ");
        overallTotalCostLabel.setText("TỔNG CỘNG: " + df.format(totalMedicineCost + totalServiceCost) + " VNĐ");

        prescriptionTreeModel.reload();
        for (int i = 0; i < prescriptionTree.getRowCount(); i++) {
            prescriptionTree.expandRow(i);
        }
    }

    private void handleRowSelection(int selectedRowInQueue) {
        if (selectedRowInQueue < 0 || selectedRowInQueue >= patientQueue.size()) {
            log.warn("Selected row index out of bounds: {}", selectedRowInQueue);
            return;
        }
        Patient selectedPatient = patientQueue.get(selectedRowInQueue);

        medicinePrescription = new String[0][0];
        servicePrescription = new String[0][0];
        medDialog = null; 
        serDialog = null;
        updatePrescriptionTree(); 

        checkupIdField.setText(selectedPatient.getCheckupId());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date parsedDate; String dateStr = selectedPatient.getCheckupDate();
            if (dateStr.matches("\\d+")) parsedDate = new Date(Long.parseLong(dateStr));
            else parsedDate = dateFormat.parse(dateStr);
            Calendar calendar = Calendar.getInstance(); calendar.setTime(parsedDate);
            datePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
            log.error("Invalid date format for checkup date: {}", selectedPatient.getCheckupDate(), exception);
            JOptionPane.showMessageDialog(null, "Định dạng ngày hoặc dấu thời gian không hợp lệ: " + selectedPatient.getCheckupDate(), "Lỗi định dạng ngày", JOptionPane.ERROR_MESSAGE);
        }

        try {
            Date parsedDate; String dobStr = selectedPatient.getCustomerDob();
            if (dobStr.matches("\\d+")) parsedDate = new Date(Long.parseLong(dobStr));
            else parsedDate = new SimpleDateFormat("dd/MM/yyyy").parse(dobStr);
            Calendar calendar = Calendar.getInstance(); calendar.setTime(parsedDate);
            dobPicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dobPicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
             log.error("Invalid date format for DOB: {}", selectedPatient.getCustomerDob(), exception);
            JOptionPane.showMessageDialog(null, "Định dạng ngày không hợp lệ: " + selectedPatient.getCustomerDob(), "Lỗi định dạng ngày", JOptionPane.ERROR_MESSAGE);
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
        try { customerWeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerWeight())); } catch (NumberFormatException e) {log.warn("Invalid weight: {}", selectedPatient.getCustomerWeight()); customerWeightSpinner.setValue(0.0);}
        try { customerHeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerHeight())); } catch (NumberFormatException e) {log.warn("Invalid height: {}", selectedPatient.getCustomerHeight()); customerHeightSpinner.setValue(0.0);}
        genderComboBox.setSelectedItem(selectedPatient.getCustomerGender());
        checkupTypeComboBox.setSelectedItem(selectedPatient.getCheckupType());

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCustomerHistoryRequest(Integer.parseInt(selectedPatient.getCustomerId())));

        String fullAddress = selectedPatient.getCustomerAddress();
        String[] addressParts = fullAddress.split(", ");
        if (addressParts.length >= 1) customerAddressField.setText(addressParts[0]);
        else customerAddressField.setText(fullAddress);
        
        if (addressParts.length == 4) {
            String ward = addressParts[1]; String district = addressParts[2]; String province = addressParts[3];
            int provinceIdx = findProvinceIndex(province);
            if (provinceIdx != -1) {
                provinceComboBox.setSelectedIndex(provinceIdx); 
                 SwingUtilities.invokeLater(() -> { 
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {} 
                    int districtIdx = findDistrictIndex(district);
                    if (districtIdx != -1) {
                        districtComboBox.setSelectedIndex(districtIdx);
                         SwingUtilities.invokeLater(() -> {
                             try { Thread.sleep(250); } catch (InterruptedException ignored) {} 
                             int wardIdx = findWardIndex(ward);
                             if (wardIdx != -1) wardComboBox.setSelectedIndex(wardIdx);
                         });
                    } else {
                         wardModel.removeAllElements(); wardModel.addElement("Phường"); wardComboBox.setEnabled(false);
                    }
                 });
            } else {
                 districtModel.removeAllElements(); districtModel.addElement("Huyện"); districtComboBox.setEnabled(false);
                 wardModel.removeAllElements(); wardModel.addElement("Phường"); wardComboBox.setEnabled(false);
            }
        } else if (addressParts.length > 1) { // Handle cases with partial address (e.g. street, ward, district but no province)
            provinceComboBox.setSelectedIndex(0); // Default to placeholder
            districtModel.removeAllElements(); districtModel.addElement("Huyện"); districtComboBox.setEnabled(false);
            wardModel.removeAllElements(); wardModel.addElement("Phường"); wardComboBox.setEnabled(false);
            // Potentially try to parse known parts if format is somewhat consistent
            log.warn("Address format not fully standard ({} parts): {}", addressParts.length, fullAddress);
        } else {
            customerAddressField.setText(fullAddress); // If only street or unparseable
            provinceComboBox.setSelectedIndex(0);
            districtModel.removeAllElements(); districtModel.addElement("Huyện"); districtComboBox.setEnabled(false);
            wardModel.removeAllElements(); wardModel.addElement("Phường"); wardComboBox.setEnabled(false);
        }
        saved = true; 
    }

    private void handleGetDistrictResponse(GetDistrictResponse response) {
        log.info("Received district data");
        LocalStorage.districts = response.getDistricts();
        LocalStorage.districtToId = response.getDistrictToId();
        districtModel.removeAllElements(); 
        for (String district : LocalStorage.districts) { districtModel.addElement(district); }
        districtComboBox.setEnabled(true); 
        if (LocalStorage.districts.length > 0 && !LocalStorage.districts[0].startsWith("Đang tải")) {
             // No automatic selection, let user choose or handleRowSelection set it
        }
        wardModel.removeAllElements(); wardModel.addElement("Phường"); wardComboBox.setEnabled(false); 
    }

    private void handleGetWardResponse(GetWardResponse response) {
        log.info("Received ward data");
        LocalStorage.wards = response.getWards();
        wardModel.removeAllElements(); 
        for (String ward : LocalStorage.wards) { wardModel.addElement(ward); }
        wardComboBox.setEnabled(true); 
        if (LocalStorage.wards.length > 0 && !LocalStorage.wards[0].startsWith("Đang tải")) {
            // No automatic selection
        }
    }

    private void handleGetCustomerHistoryResponse(GetCustomerHistoryResponse response) {
        log.info("Received customer history");
        this.history = response.getHistory();
        historyModel.setDataVector(this.history, new String[]{"Ngày Tháng","Mã khám bệnh", "Triệu chứng", "Chẩn đoán", "Ghi chú"});
    }

    private  void handleGetDoctorGeneralInfoResponse(GetDoctorGeneralInfoResponse response) {
        log.info("Received doctor general info (though LocalStorage.doctorsName is typically used)");
    }

    private void handleGetCheckUpQueueUpdateResponse(GetCheckUpQueueUpdateResponse response) {
        log.info("Received checkup update queue");
        this.rawQueueForTv = response.getQueue(); 
        this.patientQueue.clear();
        if (this.rawQueueForTv != null) {
            for (String[] patientData : this.rawQueueForTv) {
                this.patientQueue.add(new Patient(patientData));
            }
        }
        model.setDataVector(preprocessPatientDataForTable(this.patientQueue), new String[]{"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Loại khám"});
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            tvQueueFrame.updateQueueData(this.rawQueueForTv);
        }
    }

    private void handleGetCheckUpQueueResponse(GetCheckUpQueueResponse response) {
        log.info("Received checkup queue");
        this.rawQueueForTv = response.getQueue(); 
        this.patientQueue.clear();
        if (this.rawQueueForTv != null) {
            for (String[] patientData : this.rawQueueForTv) {
                this.patientQueue.add(new Patient(patientData));
            }
        }
        model.setDataVector(preprocessPatientDataForTable(this.patientQueue), new String[]{"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Loại khám"});
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            tvQueueFrame.updateQueueData(this.rawQueueForTv);
        }
    }

    private void handleCallPatientResponse(CallPatientResponse response) {
        log.info("Received call patient response: Room {}, Patient ID {}, Status {}", response.getRoomId(), response.getPatientId(), response.getStatus());
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            if (response.getStatus() == Status.PROCESSING) {
                String patientIdToFind = String.valueOf(response.getPatientId());
                String patientDisplayInfo = patientIdToFind; 

                if (this.patientQueue != null) {
                    for (Patient patient : this.patientQueue) {
                        if (patient != null && patientIdToFind.equals(patient.getCheckupId())) {
                            String ho = patient.getCustomerLastName(); String ten = patient.getCustomerFirstName();
                            String customerDob = patient.getCustomerDob(); String namSinh = "N/A";
                            try {
                                Date parsedDate;
                                if (customerDob.matches("\\d+")) parsedDate = new Date(Long.parseLong(customerDob));
                                else parsedDate = new SimpleDateFormat("dd/MM/yyyy").parse(customerDob);
                                Calendar calendar = Calendar.getInstance(); calendar.setTime(parsedDate);
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
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng để đánh dấu là trống.", "Chưa chọn phòng", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedRoomName = callRoomComboBox.getSelectedItem().toString();
        int roomId = selectedRoomIndex + 1; 
        String freeText = "<html><b>Trạng thái phòng:</b> Phòng " + selectedRoomName + " hiện đang trống</html>";
        callingStatusLabel.setText(freeText);
        callingStatusLabel.setForeground(new Color(0, 100, 0));
        callingStatusLabel.setBackground(new Color(230, 255, 230)); 
        JOptionPane.showMessageDialog(this, "Phòng " + selectedRoomName + " đã được đánh dấu là trống.", "Cập nhật trạng thái phòng", JOptionPane.INFORMATION_MESSAGE);
        log.info("Room {} marked as free", selectedRoomName);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new CallPatientRequest(roomId, -1, Status.EMPTY));
    }

    private void handleCallPatient() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân từ hàng đợi để gọi.", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedRow < 0 || selectedRow >= patientQueue.size()){
             JOptionPane.showMessageDialog(this, "Lựa chọn bệnh nhân không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Patient selectedPatient = patientQueue.get(selectedRow);
        String maKhamBenh = selectedPatient.getCheckupId();
        String patientName = selectedPatient.getCustomerLastName() + " " + selectedPatient.getCustomerFirstName();
        int selectedRoom = callRoomComboBox.getSelectedIndex() + 1; 
        JOptionPane.showMessageDialog(this, "Đang gọi bệnh nhân " + patientName + " (Mã KB: " + maKhamBenh + ") đến " + callRoomComboBox.getSelectedItem().toString(), "Gọi bệnh nhân", JOptionPane.INFORMATION_MESSAGE);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new CallPatientRequest(selectedRoom, Integer.parseInt(maKhamBenh), Status.PROCESSING));
        log.info("Called patient {} (ID: {}) to room {}", patientName, maKhamBenh, selectedRoom);
        String callingText = "<html><b>Đang gọi:</b> BN " + patientName + " (Mã KB: " + maKhamBenh + ") đến " + callRoomComboBox.getSelectedItem().toString() + "</html>"; 
        callingStatusLabel.setText(callingText);
        callingStatusLabel.setForeground(Color.WHITE);
        callingStatusLabel.setBackground(new Color(217, 83, 79)); // Red background for calling
    }
}

