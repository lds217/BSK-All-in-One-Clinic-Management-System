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
import java.awt.image.BufferedImage;

// Imports for new functionality
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

// Webcam imports
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

// JavaCV imports for video recording
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.IplImage;
import static org.bytedeco.opencv.global.opencv_core.cvFlip;

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

    // New member variables for Supersonic View
    private JPanel supersonicViewPanelGlobal;
    private JPanel imageGalleryPanel; // Displays thumbnails
    private JScrollPane imageGalleryScrollPane;
    private JLabel webcamFeedLabel; // Placeholder for webcam feed
    private JComboBox<String> webcamDeviceComboBox;
    private JButton takePictureButton;
    private JButton recordVideoButton;
    private String currentCheckupIdForMedia;
    private Path currentCheckupMediaPath;
    private javax.swing.Timer imageRefreshTimer;
    private static final String CHECKUP_MEDIA_BASE_DIR = "src/main/resources/image/checkup_media"; // Updated media directory path
    private static final int THUMBNAIL_WIDTH = 100;
    private static final int THUMBNAIL_HEIGHT = 100;

    // Webcam-specific variables
    private Webcam selectedWebcam;
    private WebcamPanel webcamPanel;
    private volatile boolean isRecording = false;
    private Thread recordingThread;
    private final Object webcamLock = new Object();
    private JLabel recordingTimeLabel;
    private long recordingStartTime;
    private javax.swing.Timer recordingTimer;

    // Video recording components
    private FFmpegFrameRecorder recorder;
    private Java2DFrameConverter frameConverter;

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

        // Ensure the base media directory exists
        try {
            Files.createDirectories(Paths.get(CHECKUP_MEDIA_BASE_DIR));
            log.info("Created or verified media directory at: {}", CHECKUP_MEDIA_BASE_DIR);
        } catch (IOException e) {
            log.error("Failed to create media directory: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                "Không thể tạo thư mục lưu trữ media: " + e.getMessage(),
                "Lỗi Khởi Tạo",
                JOptionPane.ERROR_MESSAGE);
        }

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

        // New structure for leftPanel to accommodate Supersonic View
        leftPanel.setLayout(new BorderLayout()); 

        JPanel queueSectionPanel = new JPanel(new BorderLayout());
        queueSectionPanel.setOpaque(false); 
        queueSectionPanel.add(topPanel, BorderLayout.NORTH);
        queueSectionPanel.add(tableScroll1, BorderLayout.CENTER);

        supersonicViewPanelGlobal = createSupersonicViewPanel(); 

        JSplitPane verticalSplitLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queueSectionPanel, supersonicViewPanelGlobal);
        verticalSplitLeft.setResizeWeight(0.65); // Queue gets 65% of space initially
        verticalSplitLeft.setDividerSize(8);
        verticalSplitLeft.setBorder(BorderFactory.createEmptyBorder()); 
        verticalSplitLeft.setContinuousLayout(true);

        leftPanel.add(verticalSplitLeft, BorderLayout.CENTER);

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
        splitPane.setResizeWeight(0.35); // Adjust overall horizontal split
        splitPane.setDividerSize(8); // Main divider a bit thicker
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        splitPane.setContinuousLayout(true);

        add(navBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
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

        // == Supersonic View Reset & Setup ==
        if (imageRefreshTimer != null && imageRefreshTimer.isRunning()) {
            imageRefreshTimer.stop();
        }
        
        // Clean up webcam resources before switching patients
        cleanupWebcam();
        
        currentCheckupIdForMedia = null;
        currentCheckupMediaPath = null;
        if (imageGalleryPanel != null) { // Clear previous images
            imageGalleryPanel.removeAll();
            JLabel loadingMsg = new JLabel("Đang tải hình ảnh (nếu có)...");
            loadingMsg.setFont(new Font("Arial", Font.ITALIC, 12));
            imageGalleryPanel.add(loadingMsg);
            imageGalleryPanel.revalidate();
            imageGalleryPanel.repaint();
        }
        
        // Enable webcam controls for the new patient
        if (takePictureButton != null) takePictureButton.setEnabled(false);
        if (recordVideoButton != null) recordVideoButton.setEnabled(false);
        if (webcamDeviceComboBox != null) webcamDeviceComboBox.setEnabled(false);
        if (webcamPanel != null) {
            webcamPanel.start(); // Start webcam preview for new patient
        }
        // == End Supersonic View Reset ==

        medicinePrescription = new String[0][0];
        servicePrescription = new String[0][0];
        medDialog = null; 
        serDialog = null;
        updatePrescriptionTree(); 

        checkupIdField.setText(selectedPatient.getCheckupId());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date parsedDate; String dateStr = selectedPatient.getCheckupDate();
            if (dateStr != null && dateStr.matches("\\d+")) { // Check if string is all digits (timestamp)
                 parsedDate = new Date(Long.parseLong(dateStr));
            } else if (dateStr != null && !dateStr.trim().isEmpty()) { // Attempt to parse as dd/MM/yyyy
                 parsedDate = dateFormat.parse(dateStr);
            } else {
                log.error("Invalid date format for checkup date: {}", selectedPatient.getCheckupDate());
                return;
            }
            Calendar calendar = Calendar.getInstance(); calendar.setTime(parsedDate);
            datePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
            log.error("Invalid date format for checkup date: {}", selectedPatient.getCheckupDate(), exception);
            JOptionPane.showMessageDialog(null, "Định dạng ngày hoặc dấu thời gian không hợp lệ: " + selectedPatient.getCheckupDate(), "Lỗi định dạng ngày", JOptionPane.ERROR_MESSAGE);
        }

        try {
            Date parsedDate; String dobStr = selectedPatient.getCustomerDob();
            if (dobStr != null && dobStr.matches("\\d+")) { // Timestamp
                parsedDate = new Date(Long.parseLong(dobStr));
            } else if (dobStr != null && !dobStr.trim().isEmpty()){ // Date string
                parsedDate = new SimpleDateFormat("dd/MM/yyyy").parse(dobStr);
            } else {
                log.error("Invalid date format for DOB: {}", selectedPatient.getCustomerDob());
                return;
            }
            Calendar calendar = Calendar.getInstance(); calendar.setTime(parsedDate);
            dobPicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dobPicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
             log.error("Invalid date format for DOB: {}", selectedPatient.getCustomerDob(), exception);
            JOptionPane.showMessageDialog(null, "Định dạng ngày không hợp lệ cho ngày sinh: " + selectedPatient.getCustomerDob(), "Lỗi định dạng ngày", JOptionPane.ERROR_MESSAGE);
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
        if (addressParts.length > 0) customerAddressField.setText(addressParts[0]);
        else if (fullAddress != null) customerAddressField.setText(fullAddress);
        
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

        // Setup for Supersonic View media for the NEWLY selected patient
        currentCheckupIdForMedia = selectedPatient.getCheckupId();
        if (currentCheckupIdForMedia != null && !currentCheckupIdForMedia.trim().isEmpty()) {
            ensureMediaDirectoryExists(currentCheckupIdForMedia);
            currentCheckupMediaPath = Paths.get(CHECKUP_MEDIA_BASE_DIR, currentCheckupIdForMedia.trim());
            
            if (Files.exists(currentCheckupMediaPath)) {
                loadAndDisplayImages(currentCheckupMediaPath); // Load initial images for this patient

                if (imageRefreshTimer != null && !imageRefreshTimer.isRunning()) {
                    imageRefreshTimer.start(); // Start/restart timer for this patient's media
                }
                if (takePictureButton != null) takePictureButton.setEnabled(true);
                if (recordVideoButton != null) recordVideoButton.setEnabled(true);
                if (webcamDeviceComboBox != null) webcamDeviceComboBox.setEnabled(true);
            } else {
                log.error("Media directory does not exist after creation attempt: {}", currentCheckupMediaPath);
                JOptionPane.showMessageDialog(this, 
                    "Không thể truy cập thư mục media cho bệnh nhân này.", 
                    "Lỗi Thư Mục", 
                    JOptionPane.ERROR_MESSAGE);
                currentCheckupMediaPath = null;
                if (takePictureButton != null) takePictureButton.setEnabled(false);
                if (recordVideoButton != null) recordVideoButton.setEnabled(false);
                if (webcamDeviceComboBox != null) webcamDeviceComboBox.setEnabled(false);
            }
        } else {
            log.warn("No Checkup ID available (or it is empty) for media operations for selected patient (Customer ID: {}). Disabling media features.", selectedPatient.getCustomerId());
            if (takePictureButton != null) takePictureButton.setEnabled(false);
            if (recordVideoButton != null) recordVideoButton.setEnabled(false);
            if (webcamDeviceComboBox != null) webcamDeviceComboBox.setEnabled(false);
            if(imageGalleryPanel != null) {
                imageGalleryPanel.removeAll();
                imageGalleryPanel.add(new JLabel("Không có ID khám để hiển thị media."));
                imageGalleryPanel.revalidate();
                imageGalleryPanel.repaint();
            }
        }

        saved = false; // Data loaded, any change from now on is "unsaved" until save button.
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
        log.info("Received doctor general info (though LocalStorage.doctorsName is typically used directly)");
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
                            if (customerDob != null && !customerDob.isEmpty()) {
                                try {
                                    Date parsedDate;
                                    if (customerDob.matches("\\d+")) parsedDate = new Date(Long.parseLong(customerDob)); // Timestamp
                                    else parsedDate = new SimpleDateFormat("dd/MM/yyyy").parse(customerDob); // Date string
                                    Calendar calendar = Calendar.getInstance(); calendar.setTime(parsedDate);
                                    namSinh = String.valueOf(calendar.get(Calendar.YEAR));
                                } catch (ParseException | NumberFormatException e) {
                                    log.error("Error parsing DoB for TV display: {} for patient ID {}", customerDob, patientIdToFind, e);
                                }
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

    private JPanel createSupersonicViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 0, 0, 0), // Top margin for this section
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Siêu âm & Hình ảnh",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181))));
        panel.setMinimumSize(new Dimension(300, 250)); // Ensure it has some minimum height
        panel.setPreferredSize(new Dimension(450, 300)); // Give it a decent preferred size

        JPanel imageDisplayContainer = createImageDisplayPanel(); // Left side: Image gallery
        JPanel webcamControlContainer = createWebcamControlPanel(); // Right side: Webcam controls

        JSplitPane supersonicSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imageDisplayContainer, webcamControlContainer);
        supersonicSplitPane.setResizeWeight(0.60); // Image gallery gets 60% of the space
        supersonicSplitPane.setDividerSize(6);
        supersonicSplitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Inner padding
        supersonicSplitPane.setContinuousLayout(true);

        panel.add(supersonicSplitPane, BorderLayout.CENTER);

        // Initialize image refresh timer (if not already)
        if (imageRefreshTimer == null) {
            imageRefreshTimer = new javax.swing.Timer(5000, e -> { // Refresh every 5 seconds
                if (currentCheckupMediaPath != null && Files.exists(currentCheckupMediaPath)) {
                    loadAndDisplayImages(currentCheckupMediaPath);
                }
            });
            imageRefreshTimer.setRepeats(true);
        }
        return panel;
    }

    private JPanel createImageDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thư viện Hình ảnh",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.ITALIC, 14), new Color(50, 50, 50)));

        imageGalleryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10)); // Added more spacing
        imageGalleryPanel.setBackground(Color.WHITE); // Set a background for the gallery

        imageGalleryScrollPane = new JScrollPane(imageGalleryPanel);
        imageGalleryScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        imageGalleryScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        imageGalleryScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        imageGalleryScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Border for scroll pane

        panel.add(imageGalleryScrollPane, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(250, 0)); // Width preference, height will be determined by split
        return panel;
    }

    private JPanel createWebcamControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Điều khiển Webcam",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.ITALIC, 14), new Color(50, 50, 50)));
        panel.setBackground(Color.WHITE);

        // Initialize webcam components - only scan for devices once
        Webcam.getDiscoveryService().stop(); // Stop automatic discovery
        
        List<Webcam> webcams = Webcam.getWebcams();
        String[] webcamNames = webcams.stream()
                .map(Webcam::getName)
                .toArray(String[]::new);

        // Panel for device selection
        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        devicePanel.setOpaque(false);
        devicePanel.add(new JLabel("Thiết bị:"));
        webcamDeviceComboBox = new JComboBox<>(webcamNames.length > 0 ? webcamNames : new String[]{"Không tìm thấy webcam"});
        webcamDeviceComboBox.addActionListener(e -> {
            String selectedDevice = (String) webcamDeviceComboBox.getSelectedItem();
            if (selectedDevice != null && !selectedDevice.equals("Không tìm thấy webcam")) {
                switchWebcam(selectedDevice);
            }
        });
        devicePanel.add(webcamDeviceComboBox);

        // Create webcam panel with default webcam
        if (!webcams.isEmpty()) {
            selectedWebcam = webcams.get(0);
            // Try to set a higher custom resolution
            Dimension[] resolutions = selectedWebcam.getViewSizes();
            Dimension bestResolution = WebcamResolution.VGA.getSize();
            
            // Find the best resolution that's at least VGA but not too high
            for (Dimension resolution : resolutions) {
                if (resolution.width >= 640 && resolution.height >= 480 &&
                    resolution.width <= 1280 && resolution.height <= 720) {
                    bestResolution = resolution;
                    break;
                }
            }
            
            selectedWebcam.setViewSize(bestResolution);
            webcamPanel = new WebcamPanel(selectedWebcam, false); // false = don't start automatically
            webcamPanel.setFPSDisplayed(true);
            webcamPanel.setPreferredSize(new Dimension(180, 140));
            webcamPanel.setFitArea(true);
            webcamPanel.setFPSLimit(30); // Limit FPS to reduce unnecessary updates
            webcamPanel.start();
        } else {
            webcamPanel = null;
            JLabel noWebcamLabel = new JLabel("Không tìm thấy webcam", SwingConstants.CENTER);
            noWebcamLabel.setPreferredSize(new Dimension(180, 140));
            panel.add(noWebcamLabel, BorderLayout.CENTER);
        }

        if (webcamPanel != null) {
            panel.add(webcamPanel, BorderLayout.CENTER);
        }

        // Recording time label
        recordingTimeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        recordingTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        recordingTimeLabel.setForeground(Color.RED);
        recordingTimeLabel.setVisible(false);

        // Initialize recording timer
        recordingTimer = new javax.swing.Timer(1000, e -> updateRecordingTime());

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5)); // Changed to 1 row, 2 columns
        buttonPanel.setOpaque(false);
        
        takePictureButton = new JButton("Chụp ảnh");
        takePictureButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/camera.png"));
        takePictureButton.addActionListener(e -> handleTakePicture());
        
        recordVideoButton = new JButton("Quay video");
        recordVideoButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/video-camera.png"));
        recordVideoButton.addActionListener(e -> handleRecordVideo());
        
        buttonPanel.add(takePictureButton);
        buttonPanel.add(recordVideoButton);

        // Layout for webcam controls
        JPanel southControls = new JPanel(new BorderLayout(5,5));
        southControls.setOpaque(false);
        southControls.add(devicePanel, BorderLayout.NORTH);
        southControls.add(recordingTimeLabel, BorderLayout.CENTER);
        southControls.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(southControls, BorderLayout.SOUTH);

        // Initially disable buttons until a patient is selected
        takePictureButton.setEnabled(false);
        recordVideoButton.setEnabled(false);
        webcamDeviceComboBox.setEnabled(false);

        panel.setPreferredSize(new Dimension(200,0));
        return panel;
    }

    private void updateRecordingTime() {
        if (!isRecording) return;
        
        long elapsedTime = System.currentTimeMillis() - recordingStartTime;
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60)) % 24;
        
        recordingTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void switchWebcam(String deviceName) {
        synchronized (webcamLock) {
            if (selectedWebcam != null && selectedWebcam.isOpen()) {
                webcamPanel.stop();
                selectedWebcam.close();
            }

            // Find webcam from the existing list without triggering new discovery
            Webcam newWebcam = Webcam.getWebcams().stream()
                    .filter(webcam -> webcam.getName().equals(deviceName))
                    .findFirst()
                    .orElse(null);

            if (newWebcam != null) {
                selectedWebcam = newWebcam;
                // Try to set a higher custom resolution
                Dimension[] resolutions = selectedWebcam.getViewSizes();
                Dimension bestResolution = WebcamResolution.VGA.getSize();
                
                // Find the best resolution that's at least VGA but not too high
                for (Dimension resolution : resolutions) {
                    if (resolution.width >= 640 && resolution.height >= 480 &&
                        resolution.width <= 1280 && resolution.height <= 720) {
                        bestResolution = resolution;
                        break;
                    }
                }
                
                selectedWebcam.setViewSize(bestResolution);
                
                // Remove old webcam panel
                if (webcamPanel != null && webcamPanel.getParent() != null) {
                    Container parent = webcamPanel.getParent();
                    parent.remove(webcamPanel);
                    
                    // Create and add new webcam panel
                    webcamPanel = new WebcamPanel(selectedWebcam, false); // false = don't start automatically
                    webcamPanel.setFPSDisplayed(true);
                    webcamPanel.setPreferredSize(new Dimension(180, 140));
                    webcamPanel.setFitArea(true);
                    parent.add(webcamPanel, BorderLayout.CENTER);
                    parent.revalidate();
                    parent.repaint();
                    webcamPanel.start();
                }
            }
        }
    }

    private void handleTakePicture() {
        if (currentCheckupMediaPath == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một lượt khám để lưu ảnh.", "Chưa chọn lượt khám", JOptionPane.WARNING_MESSAGE);
            return;
        }

        synchronized (webcamLock) {
            if (selectedWebcam == null || !selectedWebcam.isOpen()) {
                JOptionPane.showMessageDialog(this, "Webcam không khả dụng.", "Lỗi Webcam", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "IMG_" + currentCheckupIdForMedia + "_" + timestamp + ".png";
            Path filePath = currentCheckupMediaPath.resolve(fileName);

            try {
                BufferedImage image = selectedWebcam.getImage();
                if (image != null) {
                    ImageIO.write(image, "PNG", filePath.toFile());
                    log.info("Picture taken and saved at: {}", filePath);
                    JOptionPane.showMessageDialog(this,
                            "Đã chụp và lưu ảnh thành công tại:\n" + filePath.toString(),
                            "Chụp ảnh thành công", JOptionPane.INFORMATION_MESSAGE);
                    loadAndDisplayImages(currentCheckupMediaPath);
                } else {
                    throw new IOException("Không thể lấy ảnh từ webcam");
                }
            } catch (IOException ex) {
                log.error("Error capturing/saving image: {}", filePath, ex);
                JOptionPane.showMessageDialog(this, 
                    "Lỗi khi chụp/lưu ảnh: " + ex.getMessage(), 
                    "Lỗi Chụp Ảnh", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleRecordVideo() {
        if (currentCheckupMediaPath == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một lượt khám để lưu video.", "Chưa chọn lượt khám", JOptionPane.WARNING_MESSAGE);
            return;
        }

        synchronized (webcamLock) {
            if (selectedWebcam == null || !selectedWebcam.isOpen()) {
                JOptionPane.showMessageDialog(this, "Webcam không khả dụng.", "Lỗi Webcam", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!isRecording) {
                // Start recording
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String fileName = "VID_" + currentCheckupIdForMedia + "_" + timestamp + ".mp4";
                Path filePath = currentCheckupMediaPath.resolve(fileName);

                try {
                    // Check if JavaCV classes are available
                    try {
                        Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
                        Class.forName("org.bytedeco.javacv.FFmpegFrameRecorder");
                    } catch (ClassNotFoundException e) {
                        throw new Exception("Không tìm thấy thư viện JavaCV cần thiết. Vui lòng kiểm tra cài đặt Maven.", e);
                    }

                    // Initialize frame converter
                    frameConverter = new Java2DFrameConverter();

                    // Get webcam's actual frame rate
                    double webcamFPS = selectedWebcam.getFPS();
                    // If webcam doesn't report FPS or reports an invalid value, default to 15
                    if (webcamFPS <= 0 || Double.isNaN(webcamFPS)) {
                        webcamFPS = 15.0;
                    }
                    
                    // Set output FPS to match capture rate but use PTS multiplier for slower playback
                    final double captureRate = webcamFPS;

                    // Initialize the recorder with optimized settings
                    try {
                        Dimension size = selectedWebcam.getViewSize();
                        recorder = new FFmpegFrameRecorder(filePath.toString(), size.width, size.height);
                        
                        // Video format and codec settings
                        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
                        recorder.setFormat("mp4");
                        recorder.setFrameRate(captureRate);
                        recorder.setVideoQuality(1);
                        
                        // Pixel format and color space settings
                        recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
                        
                        // Encoding settings for better performance
                        recorder.setVideoOption("preset", "medium");
                        recorder.setVideoOption("tune", "zerolatency");
                        recorder.setVideoOption("crf", "23");
                        
                        // Buffer size settings - increased for smoother recording
                        recorder.setVideoOption("bufsize", "5000k");
                        recorder.setVideoOption("maxrate", "5000k");
                        
                        // Add filter to slow down playback by 3x
                        recorder.setVideoOption("vf", "setpts=3.0*PTS");
                        
                        recorder.start();
                    } catch (Exception e) {
                        throw new Exception("Lỗi khởi tạo FFmpeg recorder: " + e.getMessage(), e);
                    }

                    recordingThread = new Thread(() -> {
                        try {
                            recordingStartTime = System.currentTimeMillis();
                            long frameInterval = (long) (1000.0 / captureRate); // Time between frames in ms
                            long lastFrameTime = System.currentTimeMillis();
                            long frameCount = 0;
                            long startTime = System.currentTimeMillis();

                            while (isRecording) {
                                if (selectedWebcam.isOpen()) {
                                    long currentTime = System.currentTimeMillis();
                                    long elapsedTime = currentTime - lastFrameTime;

                                    if (elapsedTime >= frameInterval) {
                                        BufferedImage image = selectedWebcam.getImage();
                                        if (image != null) {
                                            try {
                                                // Convert color space from BGR to RGB
                                                BufferedImage rgbImage = new BufferedImage(
                                                    image.getWidth(), image.getHeight(), 
                                                    BufferedImage.TYPE_3BYTE_BGR);
                                                
                                                Graphics2D g = rgbImage.createGraphics();
                                                g.drawImage(image, 0, 0, null);
                                                g.dispose();

                                                Frame frame = frameConverter.convert(rgbImage);
                                                recorder.record(frame);
                                                
                                                frameCount++;
                                                lastFrameTime = currentTime;

                                                // Calculate actual FPS every 30 frames
                                                if (frameCount % 30 == 0) {
                                                    long duration = System.currentTimeMillis() - startTime;
                                                    double actualFPS = (frameCount * 1000.0) / duration;
                                                    log.debug("Actual recording FPS: {}", String.format("%.2f", actualFPS));
                                                }
                                            } catch (Exception e) {
                                                throw new Exception("Lỗi ghi frame: " + e.getMessage(), e);
                                            }
                                        }
                                    } else {
                                        // Sleep for a shorter time to be more responsive
                                        Thread.sleep(Math.max(1, frameInterval - elapsedTime));
                                    }
                                } else {
                                    Thread.sleep(100);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error during video recording: {}", e.getMessage(), e);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(CheckUpPage.this,
                                    "Lỗi trong quá trình ghi video: " + e.getMessage(),
                                    "Lỗi Ghi Video",
                                    JOptionPane.ERROR_MESSAGE);
                                stopRecording();
                            });
                        }
                    }, "VideoRecordingThread");

                    recordingThread.setPriority(Thread.MAX_PRIORITY);

                    isRecording = true;
                    recordVideoButton.setText("Dừng");
                    recordVideoButton.setBackground(Color.RED);
                    recordingTimeLabel.setVisible(true);
                    recordingTimeLabel.setForeground(Color.RED);
                    recordingTimer.start();
                    recordingThread.start();

                    log.info("Started video recording to: {}", filePath);
                } catch (Exception e) {
                    log.error("Error initializing video recording: {}", e.getMessage(), e);
                    String errorMessage = e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += "\n\nChi tiết lỗi: " + e.getCause().getMessage();
                    }
                    JOptionPane.showMessageDialog(this,
                        "Lỗi khởi tạo ghi video:\n" + errorMessage,
                        "Lỗi Ghi Video",
                        JOptionPane.ERROR_MESSAGE);
                    stopRecording();
                }
            } else {
                stopRecording();
            }
        }
    }

    private void stopRecording() {
        isRecording = false;
        
        if (recordingThread != null) {
            try {
                recordingThread.join(1000); // Wait for recording thread to finish
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for recording thread to finish");
            }
            recordingThread = null;
        }

        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Exception e) {
                log.error("Error stopping recorder: {}", e.getMessage(), e);
            }
            recorder = null;
        }

        if (frameConverter != null) {
            frameConverter = null;
        }

        recordVideoButton.setText("Quay video");
        recordVideoButton.setBackground(null);
        recordingTimeLabel.setVisible(false);
        recordingTimer.stop();

        log.info("Stopped video recording");
    }

    // Add cleanup method for webcam resources
    private void cleanupWebcam() {
        synchronized (webcamLock) {
            if (webcamPanel != null) {
                webcamPanel.stop();
            }
            if (selectedWebcam != null && selectedWebcam.isOpen()) {
                selectedWebcam.close();
            }
            if (isRecording) {
                isRecording = false;
                if (recordingThread != null) {
                    recordingThread.interrupt();
                    recordingThread = null;
                }
            }
        }
    }

    private void loadAndDisplayImages(Path mediaPath) {
        if (imageGalleryPanel == null) {
            log.warn("imageGalleryPanel is null, cannot load images.");
            return;
        }
        // Ensure this runs on the EDT
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> loadAndDisplayImages(mediaPath));
            return;
        }

        imageGalleryPanel.removeAll(); // Clear existing images

        File mediaFolder = mediaPath.toFile();
        if (mediaFolder.exists() && mediaFolder.isDirectory()) {
            File[] files = mediaFolder.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".png") ||
                       lowerName.endsWith(".jpg") ||
                       lowerName.endsWith(".jpeg") ||
                       lowerName.endsWith(".gif");
            });

            if (files != null && files.length > 0) {
                for (File file : files) {
                    try {
                        ImageIcon originalIcon = new ImageIcon(file.toURI().toURL()); // Use URL to avoid caching issues
                        Image image = originalIcon.getImage();

                        int originalWidth = originalIcon.getIconWidth();
                        int originalHeight = originalIcon.getIconHeight();
                        int newWidth = THUMBNAIL_WIDTH;
                        int newHeight = THUMBNAIL_HEIGHT;

                        if (originalWidth > 0 && originalHeight > 0) {
                            double aspectRatio = (double) originalWidth / originalHeight;
                            if (originalWidth > originalHeight) { // Landscape or square
                                newHeight = (int) (THUMBNAIL_WIDTH / aspectRatio);
                            } else { // Portrait
                                newWidth = (int) (THUMBNAIL_HEIGHT * aspectRatio);
                            }
                             // Ensure new dimensions are at least 1x1
                            newWidth = Math.max(1, newWidth);
                            newHeight = Math.max(1, newHeight);
                        } else {
                             // Fallback for invalid image dimensions
                            log.warn("Image {} has invalid dimensions ({}x{})", file.getName(), originalWidth, originalHeight);
                            newWidth = THUMBNAIL_WIDTH / 2; newHeight = THUMBNAIL_HEIGHT / 2;
                        }


                        Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                        JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                        imageLabel.setToolTipText(file.getName() + " (" + originalWidth + "x" + originalHeight + ")");
                        imageLabel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                        // Set fixed size for the label to ensure FlowLayout behaves
                        imageLabel.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

                        imageGalleryPanel.add(imageLabel);
                    } catch (Exception ex) {
                        log.error("Error loading image thumbnail: {}", file.getAbsolutePath(), ex);
                        JLabel errorLabel = new JLabel("<html><center>Lỗi ảnh<br>" + file.getName().substring(0, Math.min(file.getName().length(),10)) + "...</center></html>");
                        errorLabel.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
                        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        errorLabel.setOpaque(true);
                        errorLabel.setForeground(Color.RED);
                        errorLabel.setBackground(Color.LIGHT_GRAY);
                        imageGalleryPanel.add(errorLabel);
                    }
                }
            } else {
                JLabel noImagesLabel = new JLabel("Không có hình ảnh trong thư mục.");
                noImagesLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noImagesLabel.setHorizontalAlignment(SwingConstants.CENTER);
                // Make label take up some space if gallery is empty
                noImagesLabel.setPreferredSize(new Dimension(imageGalleryScrollPane.getViewport().getWidth() - 20 > 0 ? imageGalleryScrollPane.getViewport().getWidth() - 20 : 150, THUMBNAIL_HEIGHT));
                imageGalleryPanel.add(noImagesLabel);
            }
        } else {
            JLabel noFolderLabel = new JLabel("Thư mục media không tồn tại hoặc không thể truy cập.");
            noFolderLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            noFolderLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noFolderLabel.setPreferredSize(new Dimension(imageGalleryScrollPane.getViewport().getWidth() - 20 > 0 ? imageGalleryScrollPane.getViewport().getWidth() - 20 : 200, THUMBNAIL_HEIGHT));
            imageGalleryPanel.add(noFolderLabel);
            log.warn("Media path does not exist or is not a directory: {}", mediaPath);
        }
        imageGalleryPanel.revalidate();
        imageGalleryPanel.repaint();
    }

    public void cleanup() {
        // Stop image refresh timer
        if (imageRefreshTimer != null && imageRefreshTimer.isRunning()) {
            imageRefreshTimer.stop();
        }
        
        // Clean up webcam resources
        cleanupWebcam();
        
        // Stop webcam discovery service
        Webcam.getDiscoveryService().stop();
    }

    @Override
    public void removeNotify() {
        stopRecording(); // Ensure recording is stopped when component is removed
        cleanup();
        super.removeNotify();
    }

    private void ensureMediaDirectoryExists(String checkupId) {
        try {
            // First ensure base directory exists
            Path baseDir = Paths.get(CHECKUP_MEDIA_BASE_DIR);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                log.info("Created base media directory at: {}", baseDir);
            }

            // Then create patient-specific directory
            if (checkupId != null && !checkupId.trim().isEmpty()) {
                Path patientDir = baseDir.resolve(checkupId.trim());
                if (!Files.exists(patientDir)) {
                    Files.createDirectories(patientDir);
                    log.info("Created patient media directory at: {}", patientDir);
                }
                return;
            }
        } catch (IOException e) {
            log.error("Error creating media directories for checkup {}: {}", checkupId, e.getMessage(), e);
            JOptionPane.showMessageDialog(null,
                "Không thể tạo thư mục lưu trữ media: " + e.getMessage(),
                "Lỗi Thư Mục",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}

