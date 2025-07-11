package BsK.client.ui.component.CheckUpPage;


// TO DO add backend logic to heart beat and blood pressure and weight height


import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.CheckUpPage.AddDialog.AddDialog;
import BsK.client.ui.component.CheckUpPage.HistoryViewDialog.HistoryViewDialog;
import BsK.client.ui.component.CheckUpPage.MedicineDialog.MedicineDialog;
import BsK.client.ui.component.CheckUpPage.PrintDialog.MedicineInvoice;
import BsK.client.ui.component.CheckUpPage.PrintDialog.UltrasoundResult;
import BsK.client.ui.component.CheckUpPage.ServiceDialog.ServiceDialog;
import BsK.client.ui.component.CheckUpPage.TemplateDialog.TemplateDialog;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.QueueViewPage.QueueViewPage;
import BsK.common.entity.Patient;
import BsK.common.entity.Status;
import BsK.common.entity.Template;
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
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.datatransfer.*;
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
import javax.swing.JColorChooser;

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

// Java imports for concurrent tasks
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.InputStream;

// --- JasperReports Imports ---
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.view.JasperViewer;
// --- End JasperReports Imports ---

// Add new imports
import BsK.common.entity.PatientHistory;
import BsK.common.packet.req.GetPatientHistoryRequest;
import BsK.common.packet.res.GetPatientHistoryResponse;
import BsK.common.packet.req.SaveCheckupRequest;
import BsK.common.packet.req.SearchAllRole;
import BsK.common.packet.req.SearchAllUser;
import BsK.common.packet.req.UploadCheckupImageRequest;
import BsK.common.packet.res.AddCheckupResponse;
import BsK.common.packet.res.AddPatientResponse;
import BsK.common.packet.res.RegisterSuccessResponse;
import BsK.common.packet.res.SaveCheckupRes;
import BsK.common.packet.res.UploadCheckupImageResponse;
import BsK.common.util.date.DateUtils;

@Slf4j
public class CheckUpPage extends JPanel {
    private MainFrame mainFrame;
    private List<Patient> patientQueue = new ArrayList<>();
    private String[][] rawQueueForTv = new String[][]{};
    private String[][] history;
    private DefaultTableModel historyModel;
    private JTable historyTable;
    private final ResponseListener<GetCheckUpQueueUpdateResponse> checkUpQueueUpdateListener = this::handleGetCheckUpQueueUpdateResponse;
    private final ResponseListener<GetCheckUpQueueResponse> checkUpQueueListener = this::handleGetCheckUpQueueResponse;
    private final ResponseListener<GetPatientHistoryResponse> patientHistoryListener = this::handleGetPatientHistoryResponse;
    private final ResponseListener<GetDistrictResponse> districtResponseListener = this::handleGetDistrictResponse;
    private final ResponseListener<GetWardResponse> wardResponseListener = this::handleGetWardResponse;
    private final ResponseListener<CallPatientResponse> callPatientResponseListener = this::handleCallPatientResponse;
    private final ResponseListener<GetOrderInfoByCheckupRes> orderInfoByCheckupListener = this::handleGetOrderInfoByCheckupResponse;
    private final ResponseListener<GetAllTemplatesRes> getAllTemplatesListener = this::handleGetAllTemplatesResponse;
    private final ResponseListener<UploadCheckupImageResponse> uploadImageResponseListener = this::handleUploadImageResponse;
    private JTextField checkupIdField, customerLastNameField, customerFirstNameField,customerAddressField, customerPhoneField, customerIdField, customerCccdDdcnField;
    private JTextArea suggestionField, diagnosisField, conclusionField; // Changed symptomsField to suggestionField
    private JTextPane notesField;
    private JComboBox<String> doctorComboBox, statusComboBox, genderComboBox, provinceComboBox, districtComboBox, wardComboBox, checkupTypeComboBox, templateComboBox;
    private JSpinner customerWeightSpinner, customerHeightSpinner, patientHeartRateSpinner, bloodPressureSystolicSpinner, bloodPressureDiastolicSpinner;
    private JDatePickerImpl datePicker, dobPicker, recheckupDatePicker;
    private String[][] medicinePrescription = new String[0][0]; // Initialize to empty
    private String[][] servicePrescription = new String[0][0]; // Initialize to empty
    private String[] doctorOptions;
    private MedicineDialog medDialog = null;
    private ServiceDialog serDialog = null;
    private AddDialog addDialog = null;
    private String selectedCheckupId = null; // Use checkupId to track selection instead of row index
    private boolean saved = true; // Initially true, changed when patient selected or dialog opened.
    private DefaultComboBoxModel<String> districtModel, wardModel;
    private JComboBox<String> callRoomComboBox;
    private JButton callPatientButton;
    private JLabel callingStatusLabel;
    private JPanel rightContainer; // Add this field
    private List<Template> allTemplates;
    private JButton openQueueButton;
    private JButton addPatientButton;
    private JButton[] actionButtons;

    private QueueViewPage tvQueueFrame;
    private QueueManagementPage queueManagementPage; // The new queue window

    // New UI components for prescription display
    private JPanel prescriptionDisplayPanel;
    private JTree prescriptionTree;
    private DefaultTreeModel prescriptionTreeModel;
    private DefaultMutableTreeNode rootPrescriptionNode;
    private JLabel totalMedCostLabel;
    private JLabel totalSerCostLabel;
    private JLabel overallTotalCostLabel;
    private static final DecimalFormat df = new DecimalFormat("#,##0");

    private List<File> selectedImagesForPrint = new ArrayList<>();

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
    private boolean isWebcamInitialized = false;
    private JPanel webcamContainer;
    private final ExecutorService imageUploadExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Future<?>> uploadTimeoutTasks = new ConcurrentHashMap<>();

    // Video recording components
    private FFmpegFrameRecorder recorder;
    private Java2DFrameConverter frameConverter;

    boolean returnCell = false;

    // Template print setting
    private int photoNum = 0;
    private String printType = "";
    private String templateName = "";
    private String templateTitle = "";
    private String templateGender = "";
    private String templateContent = "";
    private String templateConclusion = "";
    private String templateSuggestion = "";
    

    public void updateQueue() {
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueRequest());
    }


    public void getAllTemplates() {
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetAllTemplatesReq());
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
            
            // Convert DOB to display format using utility function
            tableData[i][1] = DateUtils.convertToDisplayFormat(p.getCustomerDob());
            
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
        ClientHandler.addResponseListener(GetPatientHistoryResponse.class, patientHistoryListener);
        ClientHandler.addResponseListener(GetOrderInfoByCheckupRes.class, orderInfoByCheckupListener);
        ClientHandler.addResponseListener(GetDistrictResponse.class, districtResponseListener);
        ClientHandler.addResponseListener(GetWardResponse.class, wardResponseListener);
        ClientHandler.addResponseListener(CallPatientResponse.class, callPatientResponseListener);
        ClientHandler.addResponseListener(GetAllTemplatesRes.class, getAllTemplatesListener);
        ClientHandler.addResponseListener(UploadCheckupImageResponse.class, uploadImageResponseListener);

        // Instantiate the new queue page but don't show it yet
        queueManagementPage = new QueueManagementPage();

        updateQueue();
        getAllTemplates();

        // --- Navigation Bar ---
        NavBar navBar = new NavBar(mainFrame, "Thăm khám");
        add(navBar, BorderLayout.NORTH);

        // --- Central Control Panel (New) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5)); // Reduced vertical gap from 10 to 5
        controlPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5)); // Reduced top/bottom from 5 to 3
        controlPanel.setBackground(Color.WHITE);

        openQueueButton = new JButton("<html>Danh sách chờ <font color='red'><b>(F1)</b></font></html>");
        openQueueButton.setBackground(new Color(255, 152, 0)); // Amber
        openQueueButton.setForeground(Color.WHITE);
        openQueueButton.setFocusPainted(false);
        openQueueButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); // Reduced vertical padding from 10 to 8
        openQueueButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openQueueButton.addActionListener(e -> {
            updateQueue();
            queueManagementPage.setVisible(true);
            queueManagementPage.toFront();
        });

        JButton tvQueueButton = new JButton("Màn hình chờ TV");
        tvQueueButton.setBackground(new Color(0, 150, 136));
        tvQueueButton.setForeground(Color.WHITE);
        tvQueueButton.setFocusPainted(false);
        tvQueueButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); // Reduced vertical padding from 10 to 8
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

        addPatientButton = new JButton("<html>THÊM BN <font color='red'><b>(F2)</b></font></html>");
        addPatientButton.setBackground(new Color(63, 81, 181));
        addPatientButton.setForeground(Color.WHITE);
        addPatientButton.setFocusPainted(false);
        addPatientButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20)); // Reduced vertical padding from 10 to 8
        addPatientButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addPatientButton.addActionListener(e -> {
            addDialog = new AddDialog(mainFrame);
            addDialog.setVisible(true);
            updateUpdateQueue();
        });

        controlPanel.add(openQueueButton);
        controlPanel.add(tvQueueButton);
        controlPanel.add(addPatientButton);

        // --- Status Label ---
        callingStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        callingStatusLabel.setFont(new Font("Arial", Font.BOLD, 13));
        callingStatusLabel.setForeground(new Color(0, 100, 0));
        callingStatusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(2, 10, 2, 10)
        ));
        callingStatusLabel.setOpaque(true);
        callingStatusLabel.setBackground(new Color(230, 255, 230));

        // --- Main UI Panels ---
        // These are the main building blocks for the new layout
        RoundedPanel rightTopPanel = new RoundedPanel(20, Color.WHITE, false); // History Panel
        prescriptionDisplayPanel = new JPanel(new BorderLayout(5,5)); // New Prescription Panel
        RoundedPanel rightBottomPanel = new RoundedPanel(20, Color.WHITE, false); // Details/Actions Panel
        JPanel imageGalleryViewPanel = createImageGalleryViewPanel(); // Image Gallery View Panel
        JPanel webcamControlContainer = createWebcamControlPanel(); // Webcam controls are now separate

        // Configure Details/Actions Panel (now on the left)
        rightBottomPanel.setLayout(new BorderLayout());
        rightBottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 5, 3, 5), // Reduced top/bottom from 5 to 3
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Chi tiết và Thao tác",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
                )
        ));

        // Configure rightTopPanel for gallery
        rightTopPanel.setLayout(new BorderLayout());
        rightTopPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 3, 10), // Reduced bottom from 5 to 3
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Thư viện Hình ảnh",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
                )
        ));
        
        // Add image gallery to rightTopPanel
        JPanel imageDisplayPanel = createImageDisplayPanel();
        rightTopPanel.add(imageDisplayPanel, BorderLayout.CENTER);

        // Configure Prescription Display Panel (prescriptionDisplayPanel)
        prescriptionDisplayPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(3, 10, 3, 10), // Reduced top/bottom from 5 to 3
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


        // Configure the Details/Actions Panel (rightBottomPanel)
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

        // Main input panel with BorderLayout
        JPanel inputPanel = new JPanel(new BorderLayout(10, 5)); // Reduced vertical gap from 10 to 5
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Reduced top/bottom from 10 to 5

        // Patient Info Section (Top)
        JPanel patientInfoInnerPanel = new JPanel(new GridBagLayout());
        patientInfoInnerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin bệnh nhân",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16), new Color(50, 50, 50)
        ));

        // Set up a larger, more readable font for labels and fields
        Font labelFont = new Font("Arial", Font.BOLD, 18); // Increased from 14 to 18
        Font fieldFont = new Font("Arial", Font.BOLD, 16); // Increased from 14 to 16

        GridBagConstraints gbcPatient = new GridBagConstraints();
        gbcPatient.insets = new Insets(5, 8, 5, 8); // Reduced vertical insets from 8 to 5
        gbcPatient.fill = GridBagConstraints.HORIZONTAL;
        gbcPatient.anchor = GridBagConstraints.WEST;

        // Row 0: CCCD/DDCN
        gbcPatient.gridx = 0; gbcPatient.gridy = 0; gbcPatient.weightx = 0.1;
        JLabel cccdLabel = new JLabel("CCCD/DDCN", SwingConstants.RIGHT);
        cccdLabel.setFont(labelFont);
        patientInfoInnerPanel.add(cccdLabel, gbcPatient);

        gbcPatient.gridx = 1; gbcPatient.weightx = 0.4; gbcPatient.gridwidth = 3;
        customerCccdDdcnField = new JTextField(15);
        customerCccdDdcnField.setFont(fieldFont);
        patientInfoInnerPanel.add(customerCccdDdcnField, gbcPatient);
        gbcPatient.gridwidth = 1; // Reset gridwidth

        // Row 1: Name
        gbcPatient.gridx = 0; gbcPatient.gridy = 1; gbcPatient.weightx = 0.1;
        JLabel hoLabel = new JLabel("Họ", SwingConstants.RIGHT);
        hoLabel.setFont(labelFont);
        patientInfoInnerPanel.add(hoLabel, gbcPatient);

        gbcPatient.gridx = 1; gbcPatient.weightx = 0.4;
        customerLastNameField = new JTextField(20);
        customerLastNameField.setFont(fieldFont);
        patientInfoInnerPanel.add(customerLastNameField, gbcPatient);

        gbcPatient.gridx = 2; gbcPatient.weightx = 0.1;
        JLabel tenLabel = new JLabel("Tên", SwingConstants.RIGHT);
        tenLabel.setFont(labelFont);
        patientInfoInnerPanel.add(tenLabel, gbcPatient);

        gbcPatient.gridx = 3; gbcPatient.weightx = 0.4;
        customerFirstNameField = new JTextField(15);
        customerFirstNameField.setFont(fieldFont);
        patientInfoInnerPanel.add(customerFirstNameField, gbcPatient);

        // Row 2: ID and Phone
        gbcPatient.gridx = 0; gbcPatient.gridy = 2; gbcPatient.weightx = 0.1;
        JLabel idLabel = new JLabel("Mã BN", SwingConstants.RIGHT);
        idLabel.setFont(labelFont);
        patientInfoInnerPanel.add(idLabel, gbcPatient);

        gbcPatient.gridx = 1; gbcPatient.weightx = 0.4;
        customerIdField = new JTextField(15);
        customerIdField.setFont(fieldFont);
        customerIdField.setEditable(false);
        patientInfoInnerPanel.add(customerIdField, gbcPatient);

        // Initialize checkupIdField
        checkupIdField = new JTextField(15);
        checkupIdField.setFont(fieldFont);
        checkupIdField.setEditable(false);

        gbcPatient.gridx = 2; gbcPatient.weightx = 0.1;
        JLabel phoneLabel = new JLabel("SĐT", SwingConstants.RIGHT);
        phoneLabel.setFont(labelFont);
        patientInfoInnerPanel.add(phoneLabel, gbcPatient);

        gbcPatient.gridx = 3; gbcPatient.weightx = 0.4;
        customerPhoneField = new JTextField(15);
        customerPhoneField.setFont(fieldFont);
        patientInfoInnerPanel.add(customerPhoneField, gbcPatient);

        // Row 3: Gender and DOB
        gbcPatient.gridx = 0; gbcPatient.gridy = 3;
        JLabel genderLabel = new JLabel("Giới tính", SwingConstants.RIGHT);
        genderLabel.setFont(labelFont);
        patientInfoInnerPanel.add(genderLabel, gbcPatient);

        gbcPatient.gridx = 1;
        String[] genderOptions = {"Nam", "Nữ"};
        genderComboBox = new JComboBox<>(genderOptions);
        genderComboBox.setFont(fieldFont);
        patientInfoInnerPanel.add(genderComboBox, gbcPatient);

        gbcPatient.gridx = 2;
        JLabel dobLabel = new JLabel("Ngày sinh", SwingConstants.RIGHT);
        dobLabel.setFont(labelFont);
        patientInfoInnerPanel.add(dobLabel, gbcPatient);

        gbcPatient.gridx = 3;
        UtilDateModel dobModel = new UtilDateModel();
        Properties dobProperties = new Properties();
        dobProperties.put("text.today", "Hôm nay");
        dobProperties.put("text.month", "Tháng");
        dobProperties.put("text.year", "Năm");
        JDatePanelImpl dobPanel = new JDatePanelImpl(dobModel, dobProperties);
        dobPicker = new JDatePickerImpl(dobPanel, new DateLabelFormatter());
        dobPicker.getJFormattedTextField().setFont(fieldFont);
        patientInfoInnerPanel.add(dobPicker, gbcPatient);

        // Row 4: Weight and Height
        gbcPatient.gridx = 0; gbcPatient.gridy = 4;
        JLabel weightLabel = new JLabel("Cân nặng (kg)", SwingConstants.RIGHT);
        weightLabel.setFont(labelFont);
        patientInfoInnerPanel.add(weightLabel, gbcPatient);

        gbcPatient.gridx = 1;
        SpinnerModel weightModel = new SpinnerNumberModel(60, 0, 300, 0.5);
        customerWeightSpinner = new JSpinner(weightModel);
        customerWeightSpinner.setFont(fieldFont);
        JComponent weightEditor = customerWeightSpinner.getEditor();
        if (weightEditor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)weightEditor).getTextField().setFont(fieldFont);
        }
        patientInfoInnerPanel.add(customerWeightSpinner, gbcPatient);

        gbcPatient.gridx = 2;
        JLabel heightLabel = new JLabel("Chiều cao (cm)", SwingConstants.RIGHT);
        heightLabel.setFont(labelFont);
        patientInfoInnerPanel.add(heightLabel, gbcPatient);

        gbcPatient.gridx = 3;
        SpinnerModel heightModel = new SpinnerNumberModel(170, 0, 230, 0.5);
        customerHeightSpinner = new JSpinner(heightModel);
        customerHeightSpinner.setFont(fieldFont);
        JComponent heightEditor = customerHeightSpinner.getEditor();
        if (heightEditor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)heightEditor).getTextField().setFont(fieldFont);
        }
        patientInfoInnerPanel.add(customerHeightSpinner, gbcPatient);

        // Row 5: Heart Rate and Blood Pressure
        gbcPatient.gridy++;
        gbcPatient.gridx = 0;
        JLabel heartRateLabel = new JLabel("Nhịp tim (l/p)", SwingConstants.RIGHT);
        heartRateLabel.setFont(labelFont);
        patientInfoInnerPanel.add(heartRateLabel, gbcPatient);

        gbcPatient.gridx = 1;
        SpinnerModel heartRateModel = new SpinnerNumberModel(80, 30, 250, 1);
        patientHeartRateSpinner = new JSpinner(heartRateModel);
        patientHeartRateSpinner.setFont(fieldFont);
        JComponent heartRateEditor = patientHeartRateSpinner.getEditor();
        if (heartRateEditor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)heartRateEditor).getTextField().setFont(fieldFont);
        }
        patientInfoInnerPanel.add(patientHeartRateSpinner, gbcPatient);

        gbcPatient.gridx = 2;
        JLabel bloodPressureLabel = new JLabel("Huyết áp (mmHg)", SwingConstants.RIGHT);
        bloodPressureLabel.setFont(labelFont);
        patientInfoInnerPanel.add(bloodPressureLabel, gbcPatient);

        gbcPatient.gridx = 3;
        JPanel bloodPressurePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bloodPressurePanel.setOpaque(false);

        SpinnerModel systolicModel = new SpinnerNumberModel(120, 50, 300, 1);
        bloodPressureSystolicSpinner = new JSpinner(systolicModel);
        bloodPressureSystolicSpinner.setFont(fieldFont);
        JComponent systolicEditor = bloodPressureSystolicSpinner.getEditor();
        if (systolicEditor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)systolicEditor).getTextField().setFont(fieldFont);
        }

        SpinnerModel diastolicModel = new SpinnerNumberModel(80, 30, 200, 1);
        bloodPressureDiastolicSpinner = new JSpinner(diastolicModel);
        bloodPressureDiastolicSpinner.setFont(fieldFont);
        JComponent diastolicEditor = bloodPressureDiastolicSpinner.getEditor();
        if (diastolicEditor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor)diastolicEditor).getTextField().setFont(fieldFont);
        }

        JLabel slashLabel = new JLabel(" / ");
        slashLabel.setFont(fieldFont);

        bloodPressurePanel.add(bloodPressureSystolicSpinner);
        bloodPressurePanel.add(slashLabel);
        bloodPressurePanel.add(bloodPressureDiastolicSpinner);
        patientInfoInnerPanel.add(bloodPressurePanel, gbcPatient);

        // Row 6: Address (full width)
        gbcPatient.gridx = 0; gbcPatient.gridy = 6;
        JLabel addressLabel = new JLabel("Địa chỉ", SwingConstants.RIGHT);
        addressLabel.setFont(labelFont);
        patientInfoInnerPanel.add(addressLabel, gbcPatient);

        gbcPatient.gridx = 1; gbcPatient.gridwidth = 3;
        customerAddressField = new JTextField(40);
        customerAddressField.setFont(fieldFont);
        patientInfoInnerPanel.add(customerAddressField, gbcPatient);

        // Row 7: Location dropdowns
        gbcPatient.gridwidth = 1; // Reset gridwidth
        gbcPatient.gridx = 1; gbcPatient.gridy = 7;
        provinceComboBox = new JComboBox<>(LocalStorage.provinces);
        provinceComboBox.setFont(fieldFont);
        patientInfoInnerPanel.add(provinceComboBox, gbcPatient);

        gbcPatient.gridx = 2;
        districtModel = new DefaultComboBoxModel<>(new String[]{"Huyện"});
        districtComboBox = new JComboBox<>(districtModel);
        districtComboBox.setFont(fieldFont);
        districtComboBox.setEnabled(false);
        patientInfoInnerPanel.add(districtComboBox, gbcPatient);

        gbcPatient.gridx = 3;
        wardModel = new DefaultComboBoxModel<>(new String[]{"Phường"});
        wardComboBox = new JComboBox<>(wardModel);
        wardComboBox.setFont(fieldFont);
        wardComboBox.setEnabled(false);
        patientInfoInnerPanel.add(wardComboBox, gbcPatient);

        // Add Room Selection Panel
        JPanel roomControlPanel = new JPanel(new GridBagLayout());
        roomControlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Điều khiển phòng khám",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(50, 50, 50)
        ));

        GridBagConstraints gbcRoom = new GridBagConstraints();
        gbcRoom.insets = new Insets(5, 8, 5, 8); // Reduced vertical insets from 8 to 5
        gbcRoom.fill = GridBagConstraints.HORIZONTAL;

        // Room Selection
        gbcRoom.gridx = 0; gbcRoom.gridy = 0;
        JLabel roomLabel = new JLabel("Phòng khám:", SwingConstants.RIGHT);
        roomLabel.setFont(labelFont);
        roomControlPanel.add(roomLabel, gbcRoom);

        gbcRoom.gridx = 1;
        String[] roomOptions = {"Phòng 1", "Phòng 2", "Phòng 3"};
        callRoomComboBox = new JComboBox<>(roomOptions);
        callRoomComboBox.setFont(fieldFont);
        roomControlPanel.add(callRoomComboBox, gbcRoom);

        // Call Patient Button
        gbcRoom.gridx = 2;
        callPatientButton = new JButton("Gọi bệnh nhân");
        callPatientButton.setFont(fieldFont);
        callPatientButton.setBackground(new Color(33, 150, 243));
        callPatientButton.setForeground(Color.WHITE);
        callPatientButton.setFocusPainted(false);
        callPatientButton.addActionListener(e -> handleCallPatient());
        roomControlPanel.add(callPatientButton, gbcRoom);

        // Free Room Button
        gbcRoom.gridx = 0; gbcRoom.gridy = 1; gbcRoom.gridwidth = 3;
        JButton freeRoomButton = new JButton("Đánh dấu phòng trống");
        freeRoomButton.setFont(fieldFont);
        freeRoomButton.setBackground(new Color(46, 204, 113));
        freeRoomButton.setForeground(Color.WHITE);
        freeRoomButton.setFocusPainted(false);
        freeRoomButton.addActionListener(e -> handleFreeRoom());
        roomControlPanel.add(freeRoomButton, gbcRoom);

        // Add room control panel to the bottom of patient info panel
        gbcPatient.gridx = 0; gbcPatient.gridy = 8; gbcPatient.gridwidth = 4;
        patientInfoInnerPanel.add(roomControlPanel, gbcPatient);

        // Add back the event listeners
        provinceComboBox.addActionListener(e -> {
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            if (selectedProvince != null && !selectedProvince.equals("Tỉnh/Thành phố")) {
                String provinceId = LocalStorage.provinceToId.get(selectedProvince);
                if (provinceId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDistrictRequest(provinceId));
                    districtComboBox.setEnabled(false); 
                    districtModel.removeAllElements(); 
                    districtModel.addElement("Đang tải quận/huyện...");
                    wardComboBox.setEnabled(false); 
                    wardModel.removeAllElements(); 
                    wardModel.addElement("Phường"); 
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
            if (selectedDistrict != null && LocalStorage.districtToId != null && LocalStorage.districtToId.containsKey(selectedDistrict)) {
                String districtId = LocalStorage.districtToId.get(selectedDistrict);
                if (districtId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(districtId));
                    wardComboBox.setEnabled(false); 
                    wardModel.removeAllElements(); 
                    wardModel.addElement("Đang tải phường/xã...");
                }
            } else {
                wardComboBox.setEnabled(false); 
                wardModel.removeAllElements(); 
                wardModel.addElement("Phường");
            }
        });

        // Checkup Info Section
        JPanel checkupInfoPanel = new JPanel(new BorderLayout(10, 5)); // Reduced vertical gap from 10 to 5
        checkupInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin khám bệnh",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));

        // Top Row Panel (Doctor, Status, Type)
        JPanel topRowPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTop = new GridBagConstraints();
        gbcTop.insets = new Insets(3, 5, 3, 5); // Reduced vertical insets from 5 to 3
        gbcTop.fill = GridBagConstraints.HORIZONTAL;

        // Initialize datePicker
        UtilDateModel dateModel = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Hôm nay");
        p.put("text.month", "Tháng");
        p.put("text.year", "Năm");
        JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.setPreferredSize(new Dimension(120, 30));
        datePicker.getJFormattedTextField().setFont(fieldFont);

        gbcTop.gridx = 0; gbcTop.gridy = 0;
        JLabel doctorLabel = new JLabel("Bác Sĩ");
        doctorLabel.setFont(labelFont);
        topRowPanel.add(doctorLabel, gbcTop);

        gbcTop.gridx = 1; gbcTop.weightx = 0.4;
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName != null ? LocalStorage.doctorsName : new String[]{"Đang tải bác sĩ..."});
        doctorComboBox.setFont(fieldFont);
        topRowPanel.add(doctorComboBox, gbcTop);

        gbcTop.gridx = 2; gbcTop.weightx = 0;
        JLabel dateLabel = new JLabel("Đơn Ngày:");
        dateLabel.setFont(labelFont);
        topRowPanel.add(dateLabel, gbcTop);

        gbcTop.gridx = 3; gbcTop.weightx = 0.3;
        topRowPanel.add(datePicker, gbcTop);

        gbcTop.gridx = 4; gbcTop.weightx = 0;
        JLabel statusLabel = new JLabel("Trạng thái");
        statusLabel.setFont(labelFont);
        topRowPanel.add(statusLabel, gbcTop);

        gbcTop.gridx = 5; gbcTop.weightx = 0.3;
        statusComboBox = new JComboBox<>(new String[]{"ĐANG KHÁM", "CHỜ KHÁM", "ĐÃ KHÁM"});
        statusComboBox.setFont(fieldFont);
        topRowPanel.add(statusComboBox, gbcTop);

        gbcTop.gridx = 6; gbcTop.weightx = 0;
        JLabel typeLabel = new JLabel("Loại khám");
        typeLabel.setFont(labelFont);
        topRowPanel.add(typeLabel, gbcTop);

        gbcTop.gridx = 7; gbcTop.weightx = 0.3;
        checkupTypeComboBox = new JComboBox<>(new String[]{"BỆNH", "THAI", "KHÁC"});
        checkupTypeComboBox.setFont(fieldFont);
        topRowPanel.add(checkupTypeComboBox, gbcTop);

        // Template Selection Row
        JPanel templatePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTemplate = new GridBagConstraints();
        gbcTemplate.insets = new Insets(3, 5, 3, 5); // Reduced vertical insets from 5 to 3
        gbcTemplate.fill = GridBagConstraints.HORIZONTAL;

        gbcTemplate.gridx = 0; gbcTemplate.gridy = 0;
        JLabel templateLabel = new JLabel("Chọn mẫu:");
        templateLabel.setFont(labelFont);
        templatePanel.add(templateLabel, gbcTemplate);

        gbcTemplate.gridx = 1; gbcTemplate.weightx = 1.0;
        templateComboBox = new JComboBox<>(new String[]{
            "Không sử dụng mẫu",
            "Mẫu khám tổng quát",
            "Mẫu khám thai",
            "Mẫu khám nhi",
            "Mẫu khám tim mạch"
        });
        templateComboBox.setFont(fieldFont);
        templateComboBox.addActionListener(e -> handleTemplateSelection());
        templatePanel.add(templateComboBox, gbcTemplate);

        
        // Add "Thêm mẫu" button next to template combobox
        gbcTemplate.gridx = 2; gbcTemplate.weightx = 0.0;
        JButton addTemplateButton = new JButton("Thêm mẫu");
        addTemplateButton.setFont(fieldFont);
        addTemplateButton.setBackground(new Color(63, 81, 181));
        addTemplateButton.setForeground(Color.WHITE);
        addTemplateButton.setFocusPainted(false);
        addTemplateButton.setPreferredSize(new Dimension(100, 25));
        addTemplateButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(45, 63, 163)),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        addTemplateButton.addActionListener(e -> {
            // TODO: Open template dialog
            log.info("Opening template dialog");
            TemplateDialog templateDialog = new TemplateDialog(mainFrame);
            templateDialog.setVisible(true);
        });
        templatePanel.add(addTemplateButton, gbcTemplate);

        // Main Content Panel with split layout
        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 5)); // Reduced vertical gap from 10 to 5
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 3, 5)); // Reduced top from 10 to 5, bottom from 5 to 3

        // Create left panel for Nội dung (larger)
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Nội dung (chú thích)",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 18), // Increased font size for title
            new Color(63, 81, 181) // Match the blue theme
        ));

        notesField = new JTextPane();
        notesField.setContentType("text/rtf");
        
        // Set default font and size
        notesField.setFont(new Font("Times New Roman", Font.PLAIN, 16));
        
        // Set up RTF editor kit with proper character encoding
        RTFEditorKit rtfKit = new RTFEditorKit();
        notesField.setEditorKit(rtfKit);
        
        // Set up document properties
        Document doc = notesField.getDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        
        // Set up empty default template
        String defaultTemplate = "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Times New Roman;}}\n" +
                               "{\\colortbl ;\\red0\\green0\\blue0;}\n" +
                               "\\viewkind4\\uc1\\pard\\cf1\\f0\\fs32\\par}";
        setRtfContentFromString(defaultTemplate);
        
        // Add tab support - 4 spaces per tab
        notesField.getInputMap().put(KeyStroke.getKeyStroke("TAB"), "insertTab");
        notesField.getActionMap().put("insertTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                notesField.replaceSelection("    ");
            }
        });

        // Set default RTF template with proper Vietnamese character support
        String defaultRtfTemplate = "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Times New Roman;}}\n" +
                                  "{\\colortbl ;\\red0\\green0\\blue0;}\n" +
                                  "\\viewkind4\\uc1\\pard\\cf1\\f0\\fs32\\par}";
        try {
            notesField.getDocument().remove(0, notesField.getDocument().getLength());
            new RTFEditorKit().read(new ByteArrayInputStream(defaultRtfTemplate.getBytes("UTF-8")), 
                                  notesField.getDocument(), 0);
        } catch (Exception e) {
            log.error("Error setting default RTF template", e);
        }
        
        // Set preferred size for notes field
        notesField.setPreferredSize(new Dimension(0, 400)); // Make it tall

        // Enhanced Toolbar for notes with more formatting options
        JToolBar notesToolbar = new JToolBar(JToolBar.HORIZONTAL);
        notesToolbar.setFloatable(false);
        notesToolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        notesToolbar.setBackground(new Color(245, 245, 245));

        // Style buttons with better visual appearance
        JButton boldButton = new JButton(new StyledEditorKit.BoldAction());
        boldButton.setText("B");
        boldButton.setFont(new Font("Arial", Font.BOLD, 14));
        boldButton.setFocusPainted(false);
        boldButton.setToolTipText("In đậm (Ctrl+B)");
        
        JButton italicButton = new JButton(new StyledEditorKit.ItalicAction());
        italicButton.setText("I");
        italicButton.setFont(new Font("Arial", Font.ITALIC, 14));
        italicButton.setFocusPainted(false);
        italicButton.setToolTipText("In nghiêng (Ctrl+I)");
        
        JButton underlineButton = new JButton(new StyledEditorKit.UnderlineAction());
        underlineButton.setText("U");
        underlineButton.setFont(new Font("Arial", Font.PLAIN, 14));
        underlineButton.setFocusPainted(false);
        underlineButton.setToolTipText("Gạch chân (Ctrl+U)");

        // Font family selector
        String[] fontFamilies = {"Arial", "Times New Roman", "Verdana", "Courier New", "Tahoma", "Calibri"};
        JComboBox<String> fontFamilyComboBox = new JComboBox<>(fontFamilies);
        fontFamilyComboBox.setSelectedItem("Times New Roman"); // Match default font of notesField
        fontFamilyComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        fontFamilyComboBox.setToolTipText("Phông chữ");
        fontFamilyComboBox.setPreferredSize(new Dimension(140, 25));
        fontFamilyComboBox.addActionListener(e -> {
            String fontFamily = (String) fontFamilyComboBox.getSelectedItem();
            if (fontFamily != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setFontFamily(attr, fontFamily);
                notesField.getStyledDocument().setCharacterAttributes(
                    notesField.getSelectionStart(),
                    notesField.getSelectionEnd() - notesField.getSelectionStart(),
                    attr, false);
            }
        });

        // Font size spinner
        SpinnerModel sizeModel = new SpinnerNumberModel(20, 8, 72, 2); // Default 20, min 8, max 72, step 2
        JSpinner sizeSpinner = new JSpinner(sizeModel);
        sizeSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        sizeSpinner.setToolTipText("Cỡ chữ (Cỡ chữ JasperReport = giá trị / 2)");
        sizeSpinner.setPreferredSize(new Dimension(60, 25));
        sizeSpinner.addChangeListener(e -> {
            int size = (int) sizeSpinner.getValue();
            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setFontSize(attr, size);
            notesField.getStyledDocument().setCharacterAttributes(
                notesField.getSelectionStart(),
                notesField.getSelectionEnd() - notesField.getSelectionStart(),
                attr, false);
        });

        // Color chooser button
        JButton colorButton = new JButton("Màu");
        colorButton.setFont(new Font("Arial", Font.PLAIN, 14));
        colorButton.setFocusPainted(false);
        colorButton.setToolTipText("Chọn màu chữ");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(CheckUpPage.this, "Chọn màu chữ", notesField.getForeground());
            if (newColor != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, newColor);
                notesField.setCharacterAttributes(attr, false);
            }
        });

        // Add buttons to toolbar with separators and styling
        notesToolbar.add(Box.createHorizontalStrut(5));
        notesToolbar.add(fontFamilyComboBox);
        notesToolbar.addSeparator(new Dimension(5, 0));
        notesToolbar.add(sizeSpinner);
        notesToolbar.addSeparator(new Dimension(10, 0));
        notesToolbar.add(boldButton);
        notesToolbar.addSeparator(new Dimension(5, 0));
        notesToolbar.add(italicButton);
        notesToolbar.addSeparator(new Dimension(5, 0));
        notesToolbar.add(underlineButton);
        notesToolbar.addSeparator(new Dimension(10, 0));
        notesToolbar.add(colorButton);
        notesToolbar.add(Box.createHorizontalStrut(5));

        // Style the toolbar buttons
        for (Component c : notesToolbar.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setBackground(new Color(250, 250, 250));
                b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
            }
        }

        leftPanel.add(notesToolbar, BorderLayout.NORTH);
        JScrollPane notesScrollPane = new JScrollPane(notesField);
        notesScrollPane.setPreferredSize(new Dimension(0, 400)); // Set scroll pane size too
        leftPanel.add(notesScrollPane, BorderLayout.CENTER);

        // Create right panel for Triệu chứng and Chẩn đoán
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setPreferredSize(new Dimension(250, 0)); // Set a smaller fixed width for right panel
        rightPanel.setMinimumSize(new Dimension(200, 0)); // Set minimum size to prevent it from shrinking too much
        rightPanel.setMaximumSize(new Dimension(280, Integer.MAX_VALUE)); // Set maximum width to limit expansion

        // Suggestion Panel
        JPanel suggestionPanel = new JPanel(new BorderLayout(5, 5));
        suggestionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Đề nghị",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(50, 50, 50)
        ));
        suggestionField = new JTextArea(5, 20); // Adjusted rows for better layout
        suggestionField.setFont(fieldFont);
        suggestionField.setLineWrap(true);
        suggestionField.setWrapStyleWord(true);
        JScrollPane suggestionScrollPane = new JScrollPane(suggestionField);
        suggestionScrollPane.setPreferredSize(new Dimension(0, 200)); // Set fixed height
        suggestionPanel.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionPanel.setMinimumSize(new Dimension(200, 200));
        suggestionPanel.setPreferredSize(new Dimension(250, 200));

        // Add Re-checkup date picker
        JPanel recheckupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        recheckupPanel.setBackground(Color.WHITE);
        JLabel recheckupLabel = new JLabel("Tái khám:");
        recheckupLabel.setFont(labelFont);
        recheckupPanel.add(recheckupLabel);
        
        UtilDateModel recheckupModel = new UtilDateModel();
        Properties recheckupProps = new Properties();
        recheckupProps.put("text.today", "Hôm nay");
        recheckupProps.put("text.month", "Tháng");
        recheckupProps.put("text.year", "Năm");
        JDatePanelImpl recheckupDatePanel = new JDatePanelImpl(recheckupModel, recheckupProps);
        recheckupDatePicker = new JDatePickerImpl(recheckupDatePanel, new DateLabelFormatter());
        recheckupDatePicker.getJFormattedTextField().setFont(fieldFont);
        recheckupDatePicker.setPreferredSize(new Dimension(140, 30)); // Make it a bit smaller
        recheckupPanel.add(recheckupDatePicker);
        suggestionPanel.add(recheckupPanel, BorderLayout.SOUTH);

        suggestionPanel.setMinimumSize(new Dimension(200, 200));
        suggestionPanel.setPreferredSize(new Dimension(250, 200));

        // Diagnosis Panel
        JPanel diagnosisPanel = new JPanel(new BorderLayout(5, 5));
        diagnosisPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Chẩn đoán",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(50, 50, 50)
        ));
        diagnosisField = new JTextArea(4, 20);
        diagnosisField.setFont(fieldFont);
        diagnosisField.setLineWrap(true);
        diagnosisField.setWrapStyleWord(true);
        diagnosisPanel.add(new JScrollPane(diagnosisField), BorderLayout.CENTER);

        // Conclusion Panel
        JPanel conclusionPanel = new JPanel(new BorderLayout(5, 5));
        conclusionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Kết luận",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(50, 50, 50)
        ));
        conclusionField = new JTextArea(4, 20);
        conclusionField.setFont(fieldFont);
        conclusionField.setLineWrap(true);
        conclusionField.setWrapStyleWord(true);
        conclusionPanel.add(new JScrollPane(conclusionField), BorderLayout.CENTER);

        rightPanel.add(suggestionPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(diagnosisPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        rightPanel.add(conclusionPanel);

        // Create split pane for left and right panels
        JSplitPane contentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        contentSplitPane.setResizeWeight(1.0); // Give maximum weight to the left panel
        contentSplitPane.setDividerSize(3); // Make divider even smaller
        contentSplitPane.setBorder(null);
        contentSplitPane.setOneTouchExpandable(true); // Add one-touch expand/collapse buttons

        // Add to main content panel
        mainContentPanel.add(templatePanel, BorderLayout.NORTH);
        mainContentPanel.add(contentSplitPane, BorderLayout.CENTER);

        // Assemble checkup info panel
        checkupInfoPanel.add(topRowPanel, BorderLayout.NORTH);
        checkupInfoPanel.add(mainContentPanel, BorderLayout.CENTER);

                // Create tabbed pane for patient info and checkup info
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(new Color(63, 81, 181));
        
        // Add tabs with icons
        ImageIcon patientIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/user.png");
        ImageIcon checkupIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/health-check.png");
        
        // Scale icons if needed
        if (patientIcon.getIconWidth() > 20) {
            Image scaledImage = patientIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            patientIcon = new ImageIcon(scaledImage);
        }
        if (checkupIcon.getIconWidth() > 20) {
            Image scaledImage = checkupIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            checkupIcon = new ImageIcon(scaledImage);
        }

        // Create a container for patient info and history
        JPanel patientInfoContainer = new JPanel(new BorderLayout(0, 10));
        patientInfoContainer.add(patientInfoInnerPanel, BorderLayout.CENTER);

        // Add history panel to the bottom of patient info
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
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
                if (e.getClickCount() == 1) { // Single click is enough
                    int selectedRow = historyTable.getSelectedRow();
                    // Check if the row index is valid for our data array
                    if (selectedRow >= 0 && history != null && selectedRow < history.length) {
                        String[] selectedHistory = history[selectedRow];
                        // Ensure the selected record has enough data fields
                        if (selectedHistory != null && selectedHistory.length >= 11) {
                            String checkupId = selectedHistory[1];
                            String patientName = customerFirstNameField.getText() + " " + customerLastNameField.getText();
                            String checkupDate = selectedHistory[0];
                            String suggestion = selectedHistory[2];
                            String diagnosis = selectedHistory[3];
                            String notes = selectedHistory[5];
                            String conclusion = selectedHistory[7];
                            String reCheckupDate = selectedHistory[8];
                            String doctorName = selectedHistory[9] + " " + selectedHistory[10];
                             
                            openHistoryViewDialog(checkupId, patientName, checkupDate, suggestion, diagnosis, conclusion, notes, reCheckupDate, doctorName);
                        } else {
                            log.warn("Selected history record has incomplete data. Length: {}", (selectedHistory != null ? selectedHistory.length : "null"));
                        }
                    }
                }
            }
        });
        JScrollPane tableScroll2 = new JScrollPane(historyTable);
        tableScroll2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        historyPanel.add(tableScroll2, BorderLayout.CENTER);
        historyPanel.setPreferredSize(new Dimension(0, 200)); // Set fixed height for history panel
        patientInfoContainer.add(historyPanel, BorderLayout.SOUTH);

        // Create scroll panes for each panel
        JScrollPane patientScrollPane = new JScrollPane(patientInfoContainer);
        patientScrollPane.setBorder(BorderFactory.createEmptyBorder());
        patientScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        JScrollPane checkupScrollPane = new JScrollPane(checkupInfoPanel);
        checkupScrollPane.setBorder(BorderFactory.createEmptyBorder());
        checkupScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Add tabs with icons
        tabbedPane.addTab("Thông tin bệnh nhân", patientIcon, patientScrollPane);
        tabbedPane.addTab("Thông tin khám bệnh", checkupIcon, checkupScrollPane);

        // Style the tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setBackgroundAt(i, new Color(245, 245, 245));
        }

        // Add some padding around the tabbed pane
        JPanel tabbedPaneContainer = new JPanel(new BorderLayout());
        tabbedPaneContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPaneContainer.add(tabbedPane, BorderLayout.CENTER);

        // New panel to hold controls at the top of the details panel
        JPanel topActionPanel = new JPanel(new BorderLayout(20, 0)); // Horizontal gap
        topActionPanel.add(controlPanel, BorderLayout.WEST);
        topActionPanel.add(callingStatusLabel, BorderLayout.CENTER);
        
        rightBottomPanel.add(topActionPanel, BorderLayout.NORTH);
        rightBottomPanel.add(tabbedPaneContainer, BorderLayout.CENTER);
        
        // --- Assemble New Layout ---
        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());

        // Main Split Pane (Horizontal): Details on left, Right Panel on right
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rightBottomPanel, rightContainer);
        mainSplitPane.setResizeWeight(0.7); // Left side gets 70% of space
        mainSplitPane.setDividerSize(5); // Reduced from 8 to 5
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Configure webcam container
        webcamControlContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 3, 5), // Reduced top from 10 to 5, bottom from 5 to 3
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Webcam",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
                )
        ));
        webcamControlContainer.setPreferredSize(new Dimension(0, 280)); // Reduced height from 300 to 280

        // New Right Split Pane (Vertical): Webcam on top, Gallery in middle, Prescription on bottom
        JSplitPane topRightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, webcamControlContainer, rightTopPanel);
        topRightSplitPane.setResizeWeight(0.4); // Webcam gets 40% of space
        topRightSplitPane.setDividerSize(3); // Reduced from 5 to 3
        
        JSplitPane newRightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topRightSplitPane, prescriptionDisplayPanel);
        newRightSplitPane.setResizeWeight(0.7); // Top part gets 70% of space
        newRightSplitPane.setDividerSize(3); // Reduced from 5 to 3

        // Create a container for the right side that includes the split pane and action buttons
        rightContainer = new JPanel(new BorderLayout());
        rightContainer.add(newRightSplitPane, BorderLayout.CENTER);

        // Create a more modern action panel
        JPanel iconPanel = new JPanel(new GridLayout(2, 3, 10, 10)); // 2x3 grid with 10px gaps
        iconPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Reduced top/bottom from 10 to 5
        iconPanel.setBackground(Color.WHITE);

        // Define button properties
        Object[][] buttonData = {
            {"medicine", "Thêm thuốc", new Color(0, 150, 136)},
            {"service", "Thêm DV", new Color(255, 152, 0)},
            {"printer", "<html><center>In toa thuốc<br><font color='red'>(F7)</font></center></html>", new Color(156, 39, 176)},
            {"save", "<html><center>Lưu<br><font color='red'>(F8)</font></center></html>", new Color(63, 81, 181)},
            {"loupe", "<html><center>Lưu & Xem KQ<br><font color='red'>(F9)</font></center></html>", new Color(0, 172, 193)},
            {"ultrasound", "<html><center>Lưu & In KQ<br><font color='red'>(F10)</font></center></html>", new Color(21, 101, 192)}
        };

        // Create array to store action buttons for later access
        actionButtons = new JButton[buttonData.length];

        for (int i = 0; i < buttonData.length; i++) {
            String name = (String) buttonData[i][0];
            String text = (String) buttonData[i][1];
            Color color = (Color) buttonData[i][2];

            JButton button = createActionButton(name, text, color);
            button.setEnabled(false); // Disabled by default
            button.addActionListener(e -> handleActionPanelClick(name));
            iconPanel.add(button);
            actionButtons[i] = button;
        }

        rightContainer.add(iconPanel, BorderLayout.SOUTH);

        // Main Split Pane (Horizontal)
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rightBottomPanel, rightContainer);
        mainSplitPane.setResizeWeight(0.7); // Left side gets 70% of space
        mainSplitPane.setDividerSize(5); // Reduced from 8 to 5
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // --- Add components to the main panel ---
        JPanel centerContentPanel = new JPanel(new BorderLayout(0, 3)); // Reduced vertical gap from 5 to 3
        centerContentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10)); // Reduced bottom from 10 to 5

        centerContentPanel.add(mainSplitPane, BorderLayout.CENTER);

        add(centerContentPanel, BorderLayout.CENTER);

        // In the constructor, after creating notesField:
        setupNotesPasteHandler();
        setupShortcuts();
    }

    private JButton createActionButton(String iconName, String text, Color bgColor) {
        JButton button = new JButton(text);
        try {
            String iconPath = "src/main/java/BsK/client/ui/assets/icon/" + iconName + ".png";
            File iconFile = new File(iconPath);
            if (!iconFile.exists()) {
                 log.warn("Icon not found for button: {}, path: {}", iconName, iconPath);
                 // Create a placeholder icon if the file is missing
                 BufferedImage placeholder = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
                 Graphics2D g = placeholder.createGraphics();
                 g.setPaint(Color.GRAY);
                 g.fillRect(0, 0, 24, 24);
                 g.dispose();
                 button.setIcon(new ImageIcon(placeholder));
            } else {
                 ImageIcon icon = new ImageIcon(iconPath);
                 Image scaledImg = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                 button.setIcon(new ImageIcon(scaledImg));
            }
        } catch (Exception e) {
            log.error("Failed to load icon for button: {}", iconName, e);
        }
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Non-flickering hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });

        return button;
    }

    private void handleActionPanelClick(String name) {
        switch (name) {
            case "service":
                serDialog = new ServiceDialog(mainFrame, this.servicePrescription);
                serDialog.setVisible(true);
                saved = false;
                servicePrescription = serDialog.getServicePrescription();
                updatePrescriptionTree(); 
                log.info("Service prescription: {}", (Object) servicePrescription);
                break;
            case "save":
                int option = JOptionPane.showOptionDialog(null, "Bạn có muốn lưu các thay đổi?",
                        "Lưu thay đổi", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, null, null);
                if (option == JOptionPane.NO_OPTION) {
                    return;
                }

                String statusToSave = (String) statusComboBox.getSelectedItem();
                handleSave();
                afterSaveActions(statusToSave, "Đã lưu thành công!");
                break;
            case "medicine":
                medDialog = new MedicineDialog(mainFrame, this.medicinePrescription);
                medDialog.setVisible(true);
                saved = false;
                medicinePrescription = medDialog.getMedicinePrescription();
                updatePrescriptionTree(); 
                log.info("Medicine prescription: {}", (Object) medicinePrescription);
                break;
            case "printer":
                if ((medicinePrescription == null || medicinePrescription.length == 0) && 
                    (servicePrescription == null || servicePrescription.length == 0)) {
                    return; // Just return silently if there's nothing to print
                }
                MedicineInvoice medicineInvoice = new MedicineInvoice(checkupIdField.getText(),
                        customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                        dobPicker.getJFormattedTextField().getText(), customerPhoneField.getText(),
                        genderComboBox.getSelectedItem().toString(), 
                        customerAddressField.getText() + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (districtComboBox.getSelectedItem() != null ? districtComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                        doctorComboBox.getSelectedItem().toString(), diagnosisField.getText(),
                        conclusionField.getText(), medicinePrescription, servicePrescription, 
                        // fake supplements
                        new String[][] {
                            {"1", "Vitamin D3", "1", "viên", "1 viên/ngày sau ăn sáng", "30", "2000", "60000", "Bổ sung vitamin D"},
                            {"2", "Omega 3", "2", "viên", "2 viên/ngày sau ăn", "60", "1500", "90000", "Bổ sung dầu cá"},
                            {"3", "Calcium 500mg", "1", "viên", "1 viên/ngày sau ăn tối", "30", "2500", "75000", "Bổ sung canxi"}
                        }
                        ); 
                medicineInvoice.showDirectJasperViewer(); // Use the direct JasperViewer method
                break;
            case "ultrasound": // This case now handles "Lưu & In"
                // Step 1: Validate conditions first.
                if (selectedImagesForPrint.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Vui lòng chọn ít nhất một hình ảnh để in kết quả.",
                            "Chưa chọn ảnh", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                 if (photoNum != 0 && selectedImagesForPrint.size() > photoNum) {
                    JOptionPane.showMessageDialog(this,
                            "Số lượng ảnh đã chọn (" + selectedImagesForPrint.size() + ") nhiều hơn số lượng cho phép trong mẫu (" + photoNum + ").",
                            "Quá nhiều ảnh", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Step 2: Attempt to create the report object BEFORE saving anything.
                UltrasoundResult ultrasoundResultPrint = new UltrasoundResult(
                        checkupIdField.getText(),
                        customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                        dobPicker.getJFormattedTextField().getText(),
                        (String) genderComboBox.getSelectedItem(),
                        customerAddressField.getText() + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (districtComboBox.getSelectedItem() != null ? districtComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                        (String) doctorComboBox.getSelectedItem(),
                        datePicker.getJFormattedTextField().getText(),
                        TextUtils.scaleRtfFontSize(getRtfContentAsString()),
                        conclusionField.getText(),
                        suggestionField.getText(),
                        recheckupDatePicker.getJFormattedTextField().getText(),
                        selectedImagesForPrint,
                        printType,
                        templateTitle
                );
                
                JasperPrint jasperPrintToPrint;
                try {
                    jasperPrintToPrint = ultrasoundResultPrint.createJasperPrint();
                    if (jasperPrintToPrint == null) {
                        throw new Exception("Báo cáo được tạo là null.");
                    }
                } catch (Exception ex) {
                    log.error("Failed to create JasperPrint for printing", ex);
                    JOptionPane.showMessageDialog(this, "Không thể tạo báo cáo để in: " + ex.getMessage(), "Lỗi Báo Cáo", JOptionPane.ERROR_MESSAGE);
                    return; // HALT if report creation fails.
                }

                // Step 3: If report creation is successful, commit by saving and printing.
                if ("ĐANG KHÁM".equals(statusComboBox.getSelectedItem())) {
                    statusComboBox.setSelectedItem("ĐÃ KHÁM");
                }
                String statusToSavePrint = (String) statusComboBox.getSelectedItem();
                handleSave();
                afterSaveActions(statusToSavePrint, "Đã lưu. Đang gửi lệnh in...");

                // Step 4: Finally, print using the pre-made object.
                try {
                    JasperPrintManager.printReport(jasperPrintToPrint, false);
                } catch (JRException e) {
                    log.error("Error printing report directly", e);
                    JOptionPane.showMessageDialog(this, "Không thể in trực tiếp: " + e.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
                }
                break;
            case "loupe": // This case now handles "Lưu & Xem"
                // Step 1: Validate conditions first.
                if (selectedImagesForPrint.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Vui lòng chọn ít nhất một hình ảnh để xem kết quả.",
                            "Chưa chọn ảnh", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                 if (photoNum != 0 && selectedImagesForPrint.size() > photoNum) {
                    JOptionPane.showMessageDialog(this,
                            "Số lượng ảnh đã chọn (" + selectedImagesForPrint.size() + ") nhiều hơn số lượng cho phép trong mẫu (" + photoNum + ").",
                            "Quá nhiều ảnh", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Step 2: Attempt to create the report object BEFORE saving anything.
                UltrasoundResult ultrasoundResultView = new UltrasoundResult(
                        checkupIdField.getText(),
                        customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                        dobPicker.getJFormattedTextField().getText(),
                        (String) genderComboBox.getSelectedItem(),
                        customerAddressField.getText() + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (districtComboBox.getSelectedItem() != null ? districtComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                        (String) doctorComboBox.getSelectedItem(),
                        datePicker.getJFormattedTextField().getText(),
                        TextUtils.scaleRtfFontSize(getRtfContentAsString()),
                        conclusionField.getText(),
                        suggestionField.getText(),
                        recheckupDatePicker.getJFormattedTextField().getText(),
                        selectedImagesForPrint,
                        printType,
                        templateTitle
                );

                JasperPrint jasperPrintToView;
                try {
                    jasperPrintToView = ultrasoundResultView.createJasperPrint();
                    if (jasperPrintToView == null) {
                        throw new Exception("Báo cáo được tạo là null.");
                    }
                } catch (Exception ex) {
                    log.error("Failed to create JasperPrint for viewing", ex);
                    JOptionPane.showMessageDialog(this, "Không thể tạo báo cáo xem trước: " + ex.getMessage(), "Lỗi Báo Cáo", JOptionPane.ERROR_MESSAGE);
                    return; // HALT if report creation fails.
                }

                // Step 3: If report creation is successful, commit by saving and showing the view.
                // NO automatic status change for "Lưu & Xem"
                String statusToSaveView = (String) statusComboBox.getSelectedItem();
                handleSave();
                afterSaveActions(statusToSaveView, "Đã lưu. Đang tạo bản xem trước...");

                // Step 4: Finally, show the viewer with the pre-made object.
                JasperViewer.viewReport(jasperPrintToView, false);
                break;
        }
    }
    
    /**
     * Helper method to perform common actions after any save operation.
     * @param statusToSave The patient status that was saved.
     * @param message The message to display in the status label.
     */
    private void afterSaveActions(String statusToSave, String message) {
        callingStatusLabel.setText(message);
        callingStatusLabel.setBackground(new Color(230, 255, 230));
        callingStatusLabel.setForeground(new Color(0, 100, 0));
        updateUpdateQueue();

        // If patient is marked as "ĐÃ KHÁM", clear the selection and details immediately.
        if ("ĐÃ KHÁM".equals(statusToSave)) {
            clearPatientDetails();
            // Visually update the queue table to remove the selection highlight.
            queueManagementPage.updateQueueTable();
        }
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
        this.selectedCheckupId = selectedPatient.getCheckupId(); // Track selection by ID

        // Enable all action buttons when a patient is selected
        for (Component comp : ((JPanel)((JPanel)rightContainer.getComponent(1))).getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(true);
            }
        }

        // Reset saved flag for new patient
        saved = true;

        // Set checkupId field
        checkupIdField.setText(selectedPatient.getCheckupId());

        // == Supersonic View Reset & Setup ==
        if (imageRefreshTimer != null && imageRefreshTimer.isRunning()) {
            imageRefreshTimer.stop();
        }
        
        selectedImagesForPrint.clear();
        
        // Only stop recording if it's active, don't cleanup webcam
        if (isRecording) {
            stopRecording();
        }
        
        currentCheckupIdForMedia = null;
        currentCheckupMediaPath = null;
        if (imageGalleryPanel != null) {
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

        // Set checkup date using utility function
        Date checkupDate = DateUtils.convertToDate(selectedPatient.getCheckupDate());
        if (checkupDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(checkupDate);
            datePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getModel().setSelected(true);
        } else {
            log.error("Invalid date format for checkup date: {}", selectedPatient.getCheckupDate());
            JOptionPane.showMessageDialog(null, "Định dạng ngày hoặc dấu thời gian không hợp lệ: " + selectedPatient.getCheckupDate(), "Lỗi định dạng ngày", JOptionPane.ERROR_MESSAGE);
        }

        // Set DOB using utility function
        Date dobDate = DateUtils.convertToDate(selectedPatient.getCustomerDob());
        if (dobDate != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dobDate);
            dobPicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dobPicker.getModel().setSelected(true);
        } else {
            log.error("Invalid date format for DOB: {}", selectedPatient.getCustomerDob());
            JOptionPane.showMessageDialog(null, "Định dạng ngày không hợp lệ cho ngày sinh: " + selectedPatient.getCustomerDob(), "Lỗi định dạng ngày", JOptionPane.ERROR_MESSAGE);
        }

        customerLastNameField.setText(selectedPatient.getCustomerLastName());
        customerFirstNameField.setText(selectedPatient.getCustomerFirstName());
        doctorComboBox.setSelectedItem(selectedPatient.getDoctorName());
        suggestionField.setText(selectedPatient.getSuggestion()); // De nghi
        diagnosisField.setText(selectedPatient.getDiagnosis()); // Chuan doan
        conclusionField.setText(selectedPatient.getConclusion()); // Ket luan
        recheckupDatePicker.getJFormattedTextField().setText(selectedPatient.getReCheckupDate()); // Ngày khám lại

        // Handle RTF notes content
        String notes = selectedPatient.getNotes();
        if (notes != null && !notes.isEmpty()) {
            if (isValidRtfContent(notes)) {
                // If it's valid RTF, load it directly
                setRtfContentFromString(notes);
            } else {
                // If it's plain text, convert to RTF
                StringBuilder rtfContent = new StringBuilder();
                rtfContent.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Times New Roman;}}\n");
                rtfContent.append("{\\colortbl ;\\red0\\green0\\blue0;}\n");
                rtfContent.append("\\viewkind4\\uc1\\pard\\cf1\\f0\\fs32 ");
                rtfContent.append(notes.replace("\n", "\\par "));
                rtfContent.append("\\par}");
                setRtfContentFromString(rtfContent.toString());
            }
        } else {
            // Clear the notes field
            setRtfContentFromString("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Times New Roman;}}\\viewkind4\\uc1\\pard\\f0\\fs32\\par}");
        }
        
        statusComboBox.setSelectedItem(selectedPatient.getStatus());
        customerIdField.setText(selectedPatient.getCustomerId());
        customerPhoneField.setText(selectedPatient.getCustomerNumber());
        try { customerWeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerWeight())); } catch (NumberFormatException e) {log.warn("Invalid weight: {}", selectedPatient.getCustomerWeight()); customerWeightSpinner.setValue(0.0);}
        try { customerHeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerHeight())); } catch (NumberFormatException e) {log.warn("Invalid height: {}", selectedPatient.getCustomerHeight()); customerHeightSpinner.setValue(0.0);}
        genderComboBox.setSelectedItem(selectedPatient.getCustomerGender());
        checkupTypeComboBox.setSelectedItem(selectedPatient.getCheckupType());
        customerCccdDdcnField.setText(selectedPatient.getCccdDdcn() != null ? selectedPatient.getCccdDdcn() : "");

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetPatientHistoryRequest(Integer.parseInt(selectedPatient.getCustomerId())));
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetOrderInfoByCheckupReq(selectedPatient.getCheckupId()));
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
        SwingUtilities.invokeLater(() -> {
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
        });
    }

    private void handleGetWardResponse(GetWardResponse response) {
        SwingUtilities.invokeLater(() -> {
        log.info("Received ward data");
        LocalStorage.wards = response.getWards();
        wardModel.removeAllElements(); 
        for (String ward : LocalStorage.wards) { wardModel.addElement(ward); }
        wardComboBox.setEnabled(true); 
        if (LocalStorage.wards.length > 0 && !LocalStorage.wards[0].startsWith("Đang tải")) {
            // No automatic selection
        }
        });
    }

    private void handleGetPatientHistoryResponse(GetPatientHistoryResponse response) {
        SwingUtilities.invokeLater(() -> {
        log.info("Received patient history");
            this.history = response.getHistory(); // Keep original 6-column data for the dialog
            
            // Prepare data specifically for the 5-column table view
            String[][] tableData = new String[this.history.length][5];
            for (int i = 0; i < this.history.length; i++) {
                if (this.history[i] != null && this.history[i].length >= 6) {
                    // 0: checkup_date, 1: checkup_id, 2: suggestion, 3: diagnosis, 4: prescription_id, 5: notes, 6: checkup_type, 7: conclusion
                    // display 0, 1, 6, 7
                    tableData[i][0] = this.history[i][0];
                    tableData[i][1] = this.history[i][1];
                    tableData[i][2] = this.history[i][6];
                    tableData[i][3] = this.history[i][7];
                }
            }
            
            historyModel.setDataVector(tableData, new String[]{"Ngày Tháng","Mã khám bệnh", "Loại khám", "Kết luận"});
        });
    }

    private  void handleGetDoctorGeneralInfoResponse(GetDoctorGeneralInfoResponse response) {
        log.info("Received doctor general info (though LocalStorage.doctorsName is typically used directly)");
    }

    private void handleGetCheckUpQueueUpdateResponse(GetCheckUpQueueUpdateResponse response) {
        SwingUtilities.invokeLater(() -> {
        log.info("Received checkup update queue");
        this.rawQueueForTv = response.getQueue(); 
        this.patientQueue.clear();
        if (this.rawQueueForTv != null) {
            for (String[] patientData : this.rawQueueForTv) {
                this.patientQueue.add(new Patient(patientData));
            }
        }
        if (queueManagementPage != null) {
            queueManagementPage.updateQueueTable();
        }
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            tvQueueFrame.updateQueueData(this.rawQueueForTv);
        }
        });
    }

    private void handleGetCheckUpQueueResponse(GetCheckUpQueueResponse response) {
        SwingUtilities.invokeLater(() -> {
        log.info("Received checkup queue");
        this.rawQueueForTv = response.getQueue(); 
        this.patientQueue.clear();
        if (this.rawQueueForTv != null) {
            for (String[] patientData : this.rawQueueForTv) {
                this.patientQueue.add(new Patient(patientData));
            }
        }
        if (queueManagementPage != null) {
            queueManagementPage.updateQueueTable();
        }
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            tvQueueFrame.updateQueueData(this.rawQueueForTv);
        }
        });
    }

    private void handleCallPatientResponse(CallPatientResponse response) {
        SwingUtilities.invokeLater(() -> {
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
                                // Extract year from DOB for display
                                int year = DateUtils.extractYearFromTimestamp(customerDob);
                                if (year != -1) {
                                    namSinh = String.valueOf(year);
                                } else {
                                    // Try to parse as date string and extract year
                                    Date dobDate = DateUtils.convertToDate(customerDob);
                                    if (dobDate != null) {
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.setTime(dobDate);
                                        namSinh = String.valueOf(calendar.get(Calendar.YEAR));
                                    }
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
        });
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
        int selectedRow = queueManagementPage.getSelectedRow(); // Get from the queue window now
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

    private void handleGetOrderInfoByCheckupResponse(GetOrderInfoByCheckupRes response) {
        log.info("Received order info for checkup.");
        
        // Ensure UI updates are on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            this.medicinePrescription = response.getMedicinePrescription() != null ? response.getMedicinePrescription() : new String[0][0];
            this.servicePrescription = response.getServicePrescription() != null ? response.getServicePrescription() : new String[0][0];
            
            // Pass the updated prescriptions to the dialogs if they exist
            if (medDialog != null) {
                // Assuming MedicineDialog has a method to update its data
                // medDialog.setPrescription(this.medicinePrescription);
            }
            if (serDialog != null) {
                // Assuming ServiceDialog has a method to update its data
                // serDialog.setPrescription(this.servicePrescription);
            }
            
            // Update the UI tree display
            updatePrescriptionTree();
        });
    }

    private JPanel createImageGalleryViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 0, 0, 0), // Top margin for this section
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Thư viện Hình ảnh & Video",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181))));
        panel.setMinimumSize(new Dimension(300, 200)); // Ensure it has some minimum height
        panel.setPreferredSize(new Dimension(450, 200)); // Give it a decent preferred size

        JPanel imageDisplayContainer = createImageDisplayPanel();

        panel.add(imageDisplayContainer, BorderLayout.CENTER);

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

        // Initialize webcam device list - but don't start webcam yet
        List<Webcam> webcams = Webcam.getWebcams();
        String[] webcamNames = new String[webcams.size() + 1];
        webcamNames[0] = "Chọn thiết bị...";
        for (int i = 0; i < webcams.size(); i++) {
            webcamNames[i + 1] = webcams.get(i).getName();
        }

        // Panel for device selection
        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        devicePanel.setOpaque(false);
        devicePanel.add(new JLabel("Thiết bị:"));
        webcamDeviceComboBox = new JComboBox<>(webcamNames);
        webcamDeviceComboBox.addActionListener(e -> {
            String selectedDevice = (String) webcamDeviceComboBox.getSelectedItem();
            if (selectedDevice != null && !selectedDevice.equals("Chọn thiết bị...")) {
                initializeWebcam(selectedDevice);
            } else {
                cleanupWebcam();
                if (webcamContainer != null) {
                    webcamContainer.removeAll();
                    JLabel noWebcamLabel = new JLabel("Chọn thiết bị webcam", SwingConstants.CENTER);
                    noWebcamLabel.setPreferredSize(new Dimension(180, 140));
                    webcamContainer.add(noWebcamLabel);
                    webcamContainer.revalidate();
                    webcamContainer.repaint();
                }
            }
        });
        devicePanel.add(webcamDeviceComboBox);

        // Create webcam container panel
        webcamContainer = new JPanel(new BorderLayout());
        webcamContainer.setPreferredSize(new Dimension(180, 140));
        JLabel initialLabel = new JLabel("Chọn thiết bị webcam", SwingConstants.CENTER);
        initialLabel.setPreferredSize(new Dimension(180, 140));
        webcamContainer.add(initialLabel, BorderLayout.CENTER);
        panel.add(webcamContainer, BorderLayout.CENTER);

        // Recording time label
        recordingTimeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        recordingTimeLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        recordingTimeLabel.setForeground(Color.RED);
        recordingTimeLabel.setVisible(false);

        // Initialize recording timer
        recordingTimer = new javax.swing.Timer(1000, e -> updateRecordingTime());

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        buttonPanel.setOpaque(false);
        
        takePictureButton = new JButton("<html>Chụp ảnh <font color='red'><b>(F5)</b></font></html>");
        takePictureButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/camera.png"));
        takePictureButton.addActionListener(e -> handleTakePicture());
        takePictureButton.setEnabled(false);
        
        recordVideoButton = new JButton("<html>Quay video <font color='red'><b>(F6)</b></font></html>");
        recordVideoButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/video-camera.png"));
        recordVideoButton.addActionListener(e -> handleRecordVideo());
        recordVideoButton.setEnabled(false);
        
        buttonPanel.add(takePictureButton);
        buttonPanel.add(recordVideoButton);

        // Layout for webcam controls
        JPanel southControls = new JPanel(new BorderLayout(5,5));
        southControls.setOpaque(false);
        southControls.add(devicePanel, BorderLayout.NORTH);
        southControls.add(recordingTimeLabel, BorderLayout.CENTER);
        southControls.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(southControls, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(200,0));
        return panel;
    }

    private ExecutorService cleanupExecutor = Executors.newSingleThreadExecutor();
    private Future<?> cleanupTask;
    private volatile boolean isCleaningUp = false;

    private void cleanupWebcam() {
        if (isCleaningUp) return; // Prevent multiple cleanup calls
        
        isCleaningUp = true;
        
        // Stop recording immediately if active
        if (isRecording) {
            stopRecording();
        }

        // Cancel any existing cleanup task
        if (cleanupTask != null && !cleanupTask.isDone()) {
            cleanupTask.cancel(true);
        }

        // Submit non-blocking cleanup
        cleanupTask = cleanupExecutor.submit(() -> {
            try {
                // Quick cleanup without waiting
                if (webcamPanel != null) {
                    SwingUtilities.invokeLater(() -> {
                        if (webcamContainer != null) {
                            webcamContainer.removeAll();
                            JLabel placeholderLabel = new JLabel("Webcam đã tắt", SwingConstants.CENTER);
                            placeholderLabel.setPreferredSize(new Dimension(180, 140));
                            webcamContainer.add(placeholderLabel);
                            webcamContainer.revalidate();
                            webcamContainer.repaint();
                        }
                    });
                    
                    // Stop webcam panel in background
                    try {
                        webcamPanel.stop();
                    } catch (Exception e) {
                        log.debug("Webcam panel stop error (non-critical): {}", e.getMessage());
                    }
                }
                
                // Close webcam in background without blocking
                if (selectedWebcam != null && selectedWebcam.isOpen()) {
                    try {
                        selectedWebcam.close();
                    } catch (Exception e) {
                        log.debug("Webcam close error (non-critical): {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("Non-critical cleanup error: {}", e.getMessage());
            } finally {
                isWebcamInitialized = false;
                selectedWebcam = null;
                webcamPanel = null;
                isCleaningUp = false;
            }
        });
    }

    // Fast, non-blocking cleanup for page switching
    public void fastCleanup() {
        // Stop timers immediately
        if (imageRefreshTimer != null && imageRefreshTimer.isRunning()) {
            imageRefreshTimer.stop();
        }
        if (recordingTimer != null && recordingTimer.isRunning()) {
            recordingTimer.stop();
        }
        
        // Stop recording immediately if active
        if (isRecording) {
            isRecording = false; // Stop recording flag immediately
        }
        
        // Quick webcam cleanup without blocking
        cleanupWebcam();
        
        // Don't stop webcam discovery service here - let it run for other instances
    }

    // Full cleanup only when application is closing
    public void fullCleanup() {
        fastCleanup();
        
        // Only stop discovery service on full shutdown
        try {
            Webcam.getDiscoveryService().stop();
        } catch (Exception e) {
            log.debug("Discovery service stop error (non-critical): {}", e.getMessage());
        }

        // Shutdown cleanup executor
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            // Don't wait for termination - let it finish in background
        }
    }

    @Override
    public void removeNotify() {
        // Use fast cleanup instead of full cleanup
        fastCleanup();
        super.removeNotify();
    }

    private void initializeWebcam(String deviceName) {
        // Don't block if already cleaning up
        if (isCleaningUp) return;
        
        // Quick cleanup of existing webcam without waiting
        if (selectedWebcam != null && selectedWebcam.isOpen()) {
            cleanupWebcam();
        }

        // Initialize in background to avoid blocking UI
        cleanupExecutor.submit(() -> {
            try {
                // Find the selected webcam
                Webcam newWebcam = Webcam.getWebcams().stream()
                        .filter(webcam -> webcam.getName().equals(deviceName))
                        .findFirst()
                        .orElse(null);

                if (newWebcam != null && !isCleaningUp) {
                    selectedWebcam = newWebcam;
                    
                    // Set resolution quickly
                    Dimension bestResolution = WebcamResolution.VGA.getSize();
                    
                    try {
                        selectedWebcam.setViewSize(bestResolution);
                        selectedWebcam.open(true); // Non-blocking open

                        SwingUtilities.invokeLater(() -> {
                            if (!isCleaningUp && webcamContainer != null) {
                                // Create and add new webcam panel
                                webcamPanel = new WebcamPanel(selectedWebcam, false);
                                webcamPanel.setFPSDisplayed(false); // Disable FPS display for better performance
                                webcamPanel.setPreferredSize(new Dimension(180, 140));
                                webcamPanel.setFitArea(true);
                                webcamPanel.setFPSLimit(15); // Lower FPS for better performance

                                webcamContainer.removeAll();
                                webcamContainer.add(webcamPanel, BorderLayout.CENTER);
                                webcamContainer.revalidate();
                                webcamContainer.repaint();

                                webcamPanel.start();
                                isWebcamInitialized = true;

                                // Enable buttons
                                if (takePictureButton != null) takePictureButton.setEnabled(true);
                                if (recordVideoButton != null) recordVideoButton.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        log.error("Error initializing webcam: ", e);
                        SwingUtilities.invokeLater(() -> {
                            if (!isCleaningUp) {
                                JOptionPane.showMessageDialog(CheckUpPage.this,
                                    "Error initializing webcam: " + e.getMessage(),
                                    "Webcam Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                log.error("Error in webcam initialization thread: ", e);
            }
        });
    }

    private void updateRecordingTime() {
        if (!isRecording) return;
        
        long elapsedTime = System.currentTimeMillis() - recordingStartTime;
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60)) % 24;
        
        recordingTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
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
                    // Save locally first for immediate UI feedback (PNG for high quality local cache)
                    ImageIO.write(image, "PNG", filePath.toFile());
                    log.info("Picture taken and saved locally at: {}", filePath);

                    // Refresh UI to show the new image immediately
                    loadAndDisplayImages(currentCheckupMediaPath);

                    // Now, upload in the background (will be compressed to JPG)
                    uploadImageInBackground(filePath.getFileName().toString(), image);

                } else {
                    throw new IOException("Không thể lấy ảnh từ webcam");
                }
            } catch (IOException ex) {
                log.error("Error capturing/saving image locally: {}", filePath, ex);
                JOptionPane.showMessageDialog(this,
                    "Lỗi khi lưu ảnh cục bộ: " + ex.getMessage(),
                    "Lỗi Lưu Ảnh",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void uploadImageInBackground(String fileName, BufferedImage image) {
        imageUploadExecutor.submit(() -> {
            try {
                // Compress the image to JPEG for smaller size
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", baos);
                byte[] compressedImageData = baos.toByteArray();

                // Create and send the request
                String checkupId = currentCheckupIdForMedia;

                // Adjust file name to have .jpg extension for the server
                String jpgFileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".jpg";

                // Schedule a timeout task for this upload
                Future<?> timeoutTask = timeoutExecutor.schedule(() -> {
                    // If the task is still in the map, it means we timed out
                    if (uploadTimeoutTasks.remove(jpgFileName) != null) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(CheckUpPage.this,
                                "Không nhận được phản hồi từ máy chủ khi tải lên ảnh: " + jpgFileName + "\n" +
                                "Vui lòng kiểm tra lại kết nối mạng (Wi-Fi/Internet).",
                                "Lỗi Mạng",
                                JOptionPane.WARNING_MESSAGE);
                        });
                    }
                }, 15, TimeUnit.SECONDS); // 15-second timeout

                // Store the task so we can cancel it if we get a response
                uploadTimeoutTasks.put(jpgFileName, timeoutTask);

                UploadCheckupImageRequest request = new UploadCheckupImageRequest(checkupId, compressedImageData, jpgFileName);
                NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
                log.info("Sent UploadCheckupImageRequest for {}", jpgFileName);

            } catch (IOException e) {
                log.error("Failed to compress image for upload: {}", fileName, e);
                // The timeout mechanism will handle notifying the user of a potential network issue.
            }
        });
    }

    private void handleUploadImageResponse(UploadCheckupImageResponse response) {
        String fileName = response.getFileName();
        
        // A response was received, so find and cancel the timeout task.
        Future<?> timeoutTask = uploadTimeoutTasks.remove(fileName);
        if (timeoutTask != null) {
            timeoutTask.cancel(false); // Cancel the scheduled timeout
        }
        
        if (response.isSuccess()) {
            log.info("Successfully uploaded image: {}", response.getMessage());
        } else {
            log.error("Failed to upload image: {}", response.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi tải ảnh lên server: " + response.getMessage(),
                    "Lỗi Tải Ảnh",
                    JOptionPane.ERROR_MESSAGE);
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
                    
                    // captureRate is used by the capture loop logic for polling interval
                    final double captureRate = webcamFPS; 

                    // If observed playback is 2x faster, the actual sustainable FPS for the recorder
                    // should be half the reported/assumed FPS.
                    double recorderFrameRate = webcamFPS / 2.0;
                    if (recorderFrameRate < 1.0) { // Ensure a minimum frame rate of 1.0
                        recorderFrameRate = 1.0;
                    }
                    log.info("Setting recorder frame rate to: {} (derived from webcam FPS: {})", String.format("%.2f", recorderFrameRate), String.format("%.2f", webcamFPS));

                    // Initialize the recorder with optimized settings
                    try {
                        Dimension size = selectedWebcam.getViewSize();
                        recorder = new FFmpegFrameRecorder(filePath.toString(), size.width, size.height);
                        
                        // Video format and codec settings
                        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
                        recorder.setFormat("mp4");
                        // Use the adjusted effective frame rate for the recorder
                        recorder.setFrameRate(recorderFrameRate); 
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
                        
                        // Remove the speed filter and start recorder
                        recorder.start();
                    } catch (Exception e) {
                        throw new Exception("Lỗi khởi tạo FFmpeg recorder: " + e.getMessage(), e);
                    }

                    recordingThread = new Thread(() -> {
                        try {
                            recordingStartTime = System.currentTimeMillis();
                            final long videoStartTimeNanos = System.nanoTime(); // For precise frame timestamps
                            // Use captureRate (original webcamFPS) for the loop's polling interval calculation
                            long frameInterval = (long) (1000.0 / captureRate); 
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
                                                // Set explicit timestamp in microseconds
                                                frame.timestamp = (System.nanoTime() - videoStartTimeNanos) / 1000L;
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
                    recordVideoButton.setText("<html>Dừng <font color='red'><b>(F6)</b></font></html>");
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

        recordVideoButton.setText("<html>Quay video <font color='red'><b>(F6)</b></font></html>");
        recordVideoButton.setBackground(null);
        recordingTimeLabel.setVisible(false);
        recordingTimer.stop();

        log.info("Stopped video recording");
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
                        imageLabel.setToolTipText(file.getName());
                        // These are important to center the image if it's smaller than the thumbnail bounds
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

                        JCheckBox selectCheckBox = new JCheckBox(""); // "box only"
                        selectCheckBox.setToolTipText("Chọn để in");
                        selectCheckBox.setOpaque(false); // Make it transparent to see the image behind
                        
                        // Check if the file is already in the list to maintain state on refresh
                        if (selectedImagesForPrint.contains(file)) {
                            selectCheckBox.setSelected(true);
                        }

                        final File imageFile = file;
                        selectCheckBox.addActionListener(e -> {
                            if (selectCheckBox.isSelected()) {
                                if (!selectedImagesForPrint.contains(imageFile)) {
                                    selectedImagesForPrint.add(imageFile);
                                }
                            } else {
                                selectedImagesForPrint.remove(imageFile);
                            }
                            if (log.isInfoEnabled()) {
                                log.info("Selected images for printing: {}", selectedImagesForPrint.stream().map(File::getName).collect(Collectors.toList()));
                            }
                        });
                        
                        // Use a JLayeredPane to overlay checkbox on the image
                        JLayeredPane layeredPane = new JLayeredPane();
                        layeredPane.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
                        layeredPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        layeredPane.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Set the hand cursor

                        // Add the click listener directly to the image label
                        imageLabel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                showFullImageDialog(imageFile);
                            }
                        });

                        // The label will fill the entire pane
                        imageLabel.setBounds(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

                        // Position the checkbox at the top-right corner
                        int checkboxSize = 20;
                        selectCheckBox.setBounds(THUMBNAIL_WIDTH - checkboxSize - 2, 2, checkboxSize, checkboxSize); // 2px padding

                        layeredPane.add(imageLabel, JLayeredPane.DEFAULT_LAYER);
                        layeredPane.add(selectCheckBox, JLayeredPane.PALETTE_LAYER); // PALETTE_LAYER is on top of DEFAULT_LAYER
                        
                        imageGalleryPanel.add(layeredPane);
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

    private void showFullImageDialog(File imageFile) {
        try {
            BufferedImage fullImage = ImageIO.read(imageFile);
            if (fullImage == null) {
                JOptionPane.showMessageDialog(this, "Không thể tải hình ảnh.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
    
            // Create a dialog to show the image
            JDialog imageDialog = new JDialog(mainFrame, "Xem ảnh - " + imageFile.getName(), true);
            imageDialog.setLayout(new BorderLayout());
            
            // Scale image to fit screen dimensions
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int maxWidth = (int) (screenSize.width * 0.9);
            int maxHeight = (int) (screenSize.height * 0.9);
    
            int imgWidth = fullImage.getWidth();
            int imgHeight = fullImage.getHeight();
            
            double scale = Math.min(1.0, Math.min((double) maxWidth / imgWidth, (double) maxHeight / imgHeight));
            
            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);
            
            Image scaledImage = fullImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            
            // Add label to a scroll pane in case it's still large
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            imageDialog.add(scrollPane, BorderLayout.CENTER);
            
            // Set dialog size and position
            imageDialog.setSize(scaledWidth + 20, scaledHeight + 45); // Add padding for borders and title bar
            imageDialog.setLocationRelativeTo(this);
            imageDialog.setVisible(true);
            
        } catch (IOException e) {
            log.error("Error opening full image view for {}", imageFile.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this, "Lỗi khi mở ảnh: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
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

    /**
     * A separate window (JFrame) to manage and display the patient queue.
     * It communicates with the main CheckUpPage to handle patient selection.
     */
    private class QueueManagementPage extends JDialog {
        private JTable queueTable;
        private DefaultTableModel queueTableModel;

        public QueueManagementPage() {
            super(mainFrame, "Danh sách chờ khám", true); // Set as modal dialog
            
            setIconImage(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/database.png").getImage());
            setSize(800, 600);
            setLayout(new BorderLayout());
            
            // Add window listener to handle minimize/restore events with parent
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowIconified(WindowEvent e) {
                    if (mainFrame != null) {
                        mainFrame.setState(java.awt.Frame.ICONIFIED);
                    }
                }
                
                @Override
                public void windowDeiconified(WindowEvent e) {
                    if (mainFrame != null && mainFrame.getState() == java.awt.Frame.ICONIFIED) {
                        mainFrame.setState(java.awt.Frame.NORMAL);
                    }
                }
                
                @Override
                public void windowClosing(WindowEvent e) {
                    // Hide instead of closing to maintain state
                    setVisible(false);
                }
            });

            String[] columns = {"Mã khám bệnh", "Ngày sinh", "Họ", "Tên", "Bác Sĩ", "Loại khám"};
            queueTableModel = new DefaultTableModel(preprocessPatientDataForTable(patientQueue), columns) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            queueTable = new JTable(queueTableModel);
            queueTable.setPreferredScrollableViewportSize(new Dimension(750, 400));
            queueTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
            queueTable.setFont(new Font("Arial", Font.PLAIN, 12));
            queueTable.setRowHeight(25);
            queueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            queueTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        SwingUtilities.invokeLater(() -> {
                            int selectedRow = queueTable.getSelectedRow();
                            if (selectedRow != -1) {
                                String newlySelectedCheckupId = (String) queueTableModel.getValueAt(selectedRow, 0);

                                // Check for unsaved changes before switching
                                if (selectedCheckupId != null && !selectedCheckupId.equals(newlySelectedCheckupId) && !saved) {
                                    int confirm = JOptionPane.showConfirmDialog(
                                            QueueManagementPage.this,
                                            "Các thay đổi chưa được lưu. Bạn có chắc chắn muốn chuyển sang bệnh nhân khác không?",
                                            "Xác nhận chuyển bệnh nhân",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.WARNING_MESSAGE);
                                    if (confirm == JOptionPane.NO_OPTION) {
                                        // Reselect the previous row visually to avoid confusion
                                        int previousRow = findRowByCheckupId(selectedCheckupId);
                                        if (previousRow != -1) {
                                            queueTable.setRowSelectionInterval(previousRow, previousRow);
                                        }
                                        return;
                                    }
                                }
                                // Call the parent class's method to handle the selection
                                CheckUpPage.this.handleRowSelection(selectedRow);
                                // Hide the dialog after selection
                                QueueManagementPage.this.setVisible(false);
                            }
                        });
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(queueTable);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            add(scrollPane, BorderLayout.CENTER);
            setLocationRelativeTo(mainFrame);
            setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        }

        private int findRowByCheckupId(String checkupId) {
            if (checkupId == null) return -1;
            for (int i = 0; i < queueTableModel.getRowCount(); i++) {
                if (checkupId.equals(queueTableModel.getValueAt(i, 0))) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Updates the data in the queue table. This should be called when the patient queue changes.
         */
        public void updateQueueTable() {
            SwingUtilities.invokeLater(() -> {
                String[] columns = {"Mã khám bệnh", "Ngày sinh", "Họ", "Tên", "Bác Sĩ", "Loại khám"};
                String previouslySelectedId = CheckUpPage.this.selectedCheckupId;

                queueTableModel.setDataVector(preprocessPatientDataForTable(patientQueue), columns);

                if (previouslySelectedId != null) {
                    int rowToSelect = findRowByCheckupId(previouslySelectedId);
                    if (rowToSelect != -1) {
                        queueTable.setRowSelectionInterval(rowToSelect, rowToSelect);
                        queueTable.scrollRectToVisible(queueTable.getCellRect(rowToSelect, 0, true));
                    } else {
                        // The previously selected patient is no longer in the queue (e.g., status changed to "ĐÃ KHÁM")
                        // Clear the details panel to avoid confusion
                        clearPatientDetails();
                    }
                }
            });
        }

        /**
         * Gets the currently selected row from the queue table.
         * @return the selected row index, or -1 if no row is selected.
         */
        public int getSelectedRow() {
            return queueTable.getSelectedRow();
        }
    }

    private void handleRtfPaste() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);
            
            if (content != null) {
                // Try to get RTF data first
                if (content.isDataFlavorSupported(new DataFlavor("text/rtf"))) {
                    Object rtfData = content.getTransferData(new DataFlavor("text/rtf"));
                    if (rtfData instanceof InputStream) {
                        RTFEditorKit kit = (RTFEditorKit) notesField.getEditorKit();
                        kit.read((InputStream) rtfData, notesField.getDocument(), notesField.getCaretPosition());
                        return;
                    }
                }
                
                // Fall back to plain text if RTF is not available
                if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String text = (String) content.getTransferData(DataFlavor.stringFlavor);
                    
                    // Convert plain text to RTF with proper Vietnamese encoding
                    StringBuilder rtfContent = new StringBuilder();
                    rtfContent.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Times New Roman;}}\n");
                    rtfContent.append("{\\colortbl ;\\red0\\green0\\blue0;}\n");
                    rtfContent.append("\\viewkind4\\uc1\\pard\\cf1\\f0\\fs32 ");
                    
                    // Convert Vietnamese characters to RTF Unicode escape sequences
                    for (char c : text.toCharArray()) {
                        if (c < 128) {
                            rtfContent.append(c);
                        } else {
                            rtfContent.append("\\u").append((int) c).append("?");
                        }
                    }
                    
                    rtfContent.append("\\par}");
                    
                    // Insert the RTF content
                    ByteArrayInputStream in = new ByteArrayInputStream(rtfContent.toString().getBytes("ISO-8859-1"));
                    RTFEditorKit kit = (RTFEditorKit) notesField.getEditorKit();
                    kit.read(in, notesField.getDocument(), notesField.getCaretPosition());
                }
            }
        } catch (Exception e) {
            log.error("Error handling RTF paste", e);
        }
    }

    // Add paste key binding to the notes field
    private void setupNotesPasteHandler() {
        notesField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "customPaste");
        notesField.getActionMap().put("customPaste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRtfPaste();
            }
        });
    }

    /**
     * Converts the current RTF content in the notesField to a string for database storage
     * @return String representation of the RTF content
     */
    private String getRtfContentAsString() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            notesField.getEditorKit().write(out, notesField.getDocument(), 0, notesField.getDocument().getLength());
            // Return raw RTF content
            return out.toString("ISO-8859-1");
        } catch (Exception e) {
            log.error("Error getting RTF content", e);
            return "";
        }
    }

    /**
     * Sets the RTF content from a string (loaded from database)
     * @param rtfContent The RTF content as string
     */
    private void setRtfContentFromString(String rtfContent) {
        if (rtfContent == null || rtfContent.isEmpty()) {
            return;
        }

        try {
            // Clear existing content
            notesField.getDocument().remove(0, notesField.getDocument().getLength());
            
            // Load RTF content directly without any conversion
            notesField.getEditorKit().read(new ByteArrayInputStream(rtfContent.getBytes("ISO-8859-1")), 
                                         notesField.getDocument(), 0);
        } catch (Exception e) {
            log.error("Error setting RTF content from string", e);
        }
    }

    /**
     * Gets plain text content from the RTF editor
     * @return Plain text content without RTF formatting
     */
    private String getPlainTextContent() {
        try {
            return notesField.getDocument().getText(0, notesField.getDocument().getLength());
        } catch (Exception e) {
            log.error("Error getting plain text content", e);
            return "";
        }
    }

    /**
     * Checks if the RTF content is valid
     * @param rtfContent The RTF content to validate
     * @return true if content is valid RTF
     */
    private boolean isValidRtfContent(String rtfContent) {
        return rtfContent != null && rtfContent.startsWith("{\\rtf");
    }



    // Update the save method to get RTF content
    private void handleSave() {
        // Get all the field values
        String rtfContent = getRtfContentAsString();
        String checkupId = checkupIdField.getText();
        String checkupDate = datePicker.getJFormattedTextField().getText();
        String customerId = customerIdField.getText();
        String customerLastName = customerLastNameField.getText();
        String customerFirstName = customerFirstNameField.getText();
        String customerDob = dobPicker.getJFormattedTextField().getText();
        String customerGender = (String) genderComboBox.getSelectedItem();
        
        // Construct full address
        String address = customerAddressField.getText();
        String ward = (String) wardComboBox.getSelectedItem();
        String district = (String) districtComboBox.getSelectedItem();
        String province = (String) provinceComboBox.getSelectedItem();
        String fullAddress = String.format("%s, %s, %s, %s", 
            address,
            ward != null && !ward.equals("Phường") ? ward : "",
            district != null && !district.equals("Huyện") ? district : "",
            province != null && !province.equals("Tỉnh/Thành phố") ? province : ""
        ).replaceAll(", ,", ",").trim().replaceAll(",$", "");

        String customerNumber = customerPhoneField.getText();
        String customerWeight = customerWeightSpinner.getValue().toString();
        String customerHeight = customerHeightSpinner.getValue().toString();
        String doctorId = String.valueOf(doctorComboBox.getSelectedIndex() + 1);
        String suggestions = suggestionField.getText();
        String diagnosis = diagnosisField.getText();
        String conclusion = conclusionField.getText();
        String status = (String) statusComboBox.getSelectedItem();
        String checkupType = (String) checkupTypeComboBox.getSelectedItem();
        String reCheckupDate = recheckupDatePicker.getJFormattedTextField().getText();
        String customerCccdDdcn = customerCccdDdcnField.getText();

        // Log all fields that will be sent to backend
        log.info("=== SaveCheckupRequest Data ===");
        log.info("checkupId: {}", checkupId);
        log.info("checkupDate: {}", checkupDate);
        log.info("customerId: {}", customerId);
        log.info("customerLastName: {}", customerLastName);
        log.info("customerFirstName: {}", customerFirstName);
        log.info("customerDob: {}", customerDob);
        log.info("customerGender: {}", customerGender);
        log.info("customerAddress: {}", fullAddress);
        log.info("customerNumber: {}", customerNumber);
        log.info("customerWeight: {}", customerWeight);
        log.info("customerHeight: {}", customerHeight);
        log.info("doctorId: {}", doctorId);
        log.info("suggestions: {}", suggestions);
        log.info("diagnosis: {}", diagnosis);
        log.info("conclusion: {}", conclusion);
        log.info("notes (RTF): {}", rtfContent);
        log.info("status: {}", status);
        log.info("checkupType: {}", checkupType);
        log.info("medicinePrescription: {}", (Object) medicinePrescription);
        log.info("servicePrescription: {}", (Object) servicePrescription);
        log.info("reCheckupDate: {}", reCheckupDate);
        log.info("customerCccdDdcn: {}", customerCccdDdcn);
        log.info("=== End SaveCheckupRequest Data ===");

        
        Long customerDobLong = DateUtils.convertToDate(customerDob).getTime();
        Long reCheckupDateLong = DateUtils.convertToDate(reCheckupDate).getTime();
        Long checkupDateLong = DateUtils.convertToDate(checkupDate).getTime();
        SaveCheckupRequest request = new SaveCheckupRequest(
            Integer.parseInt(checkupId),
            Integer.parseInt(customerId),
            Integer.parseInt(doctorId),
            checkupDateLong,
            suggestions,
            diagnosis,
            rtfContent,
            status,
            checkupType,
            conclusion,
            reCheckupDateLong,
            customerFirstName,
            customerLastName,
            customerDobLong,
            customerGender,
            fullAddress,
            customerNumber,
            Double.parseDouble(customerWeight),
            Double.parseDouble(customerHeight),
            customerCccdDdcn,
            medicinePrescription,
            servicePrescription
        );

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
        saved = true;
    }

    private void handleTemplateSelection() {
        String selectedTemplateName = (String) templateComboBox.getSelectedItem();

        if (selectedTemplateName == null || allTemplates == null || selectedTemplateName.equals("Không sử dụng mẫu")) {
            return;
        }

        allTemplates.stream()
            .filter(t -> t.getTemplateName().equals(selectedTemplateName))
            .findFirst()
            .ifPresent(this::applyTemplate);
    }

    private void applyTemplate(Template template) {
        diagnosisField.setText(template.getDiagnosis());
        conclusionField.setText(template.getConclusion());
        suggestionField.setText(template.getSuggestion());
        
        String rtfContent = template.getContent();
        if (rtfContent != null && !rtfContent.isEmpty()) {
            setRtfContentFromString(rtfContent);
        } else {
            setRtfContentFromString(""); // Clear if content is empty
        }
        // Set template print setting
        photoNum = Integer.parseInt(template.getPhotoNum());
        printType = template.getPrintType();
        templateName = template.getTemplateName();
        templateTitle = template.getTemplateTitle();
        templateGender = template.getTemplateGender();
        templateContent = template.getContent();
        templateConclusion = template.getConclusion();
        templateSuggestion = template.getSuggestion();
    }

    private void handleGetAllTemplatesResponse(GetAllTemplatesRes response) {
        log.info("Get all templates response");
        this.allTemplates = response.getTemplates();
        List<Template> templates = response.getTemplates();
        SwingUtilities.invokeLater(() -> {
            templateComboBox.removeAllItems();
            templateComboBox.addItem("Không sử dụng mẫu");
            for (Template template : templates) {
                templateComboBox.addItem(template.getTemplateName());
            }
        });

        // No need to delete listener here because it will be called again when user click refresh button
    }

    /**
     * Opens the history view dialog for the selected checkup
     */
    private void openHistoryViewDialog(String checkupId, String patientName, String checkupDate, String suggestion, String diagnosis, String conclusion, String notes, String reCheckupDate, String doctorName) {
        try {
            // Pass all the necessary details to the dialog
            HistoryViewDialog historyDialog = new HistoryViewDialog(mainFrame, checkupId, patientName, checkupDate, suggestion, diagnosis, conclusion, notes, reCheckupDate, doctorName);
            historyDialog.setVisible(true);
        } catch (Exception e) {
            log.error("Error opening history view dialog", e);
            JOptionPane.showMessageDialog(this, 
                "Không thể mở chi tiết lịch sử khám: " + e.getMessage(), 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearPatientDetails() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Reset all fields to default values
            checkupIdField.setText("");
            customerLastNameField.setText("");
            customerFirstNameField.setText("");
            customerAddressField.setText("");
            customerPhoneField.setText("");
            customerIdField.setText("");
            suggestionField.setText("");
            diagnosisField.setText("");
            conclusionField.setText("");
            setRtfContentFromString("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Times New Roman;}}\\viewkind4\\uc1\\pard\\f0\\fs32\\par}"); // Clear notes
            customerCccdDdcnField.setText("");

            if (doctorComboBox.getItemCount() > 0) doctorComboBox.setSelectedIndex(0);
            statusComboBox.setSelectedIndex(0);
            genderComboBox.setSelectedIndex(0);
            if (provinceComboBox.getItemCount() > 0) provinceComboBox.setSelectedIndex(0);
            // district and ward will be cleared by province selection listener
            checkupTypeComboBox.setSelectedIndex(0);
            if (templateComboBox.getItemCount() > 0) templateComboBox.setSelectedIndex(0);

            customerWeightSpinner.setValue(0);
            customerHeightSpinner.setValue(0);
            patientHeartRateSpinner.setValue(80);
            bloodPressureSystolicSpinner.setValue(120);
            bloodPressureDiastolicSpinner.setValue(80);

            datePicker.getModel().setValue(null);
            dobPicker.getModel().setValue(null);
            recheckupDatePicker.getModel().setValue(null);

            medicinePrescription = new String[0][0];
            servicePrescription = new String[0][0];
            updatePrescriptionTree();

            historyModel.setRowCount(0); // Clear history table

            // Disable all action buttons
            for (Component comp : ((JPanel)((JPanel)rightContainer.getComponent(1))).getComponents()) {
                if (comp instanceof JButton) {
                    comp.setEnabled(false);
                }
            }

            // Clear media view
            if (imageGalleryPanel != null) {
                imageGalleryPanel.removeAll();
                imageGalleryPanel.add(new JLabel("Chọn bệnh nhân để xem media."));
                imageGalleryPanel.revalidate();
                imageGalleryPanel.repaint();
            }

            selectedCheckupId = null;
            saved = true; // No patient loaded, so it's in a "saved" state.
        }
    }

    /**
     * Sets up keyboard shortcuts for the entire page.
     */
    private void setupShortcuts() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        // F1: Open Queue
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "openQueue");
        actionMap.put("openQueue", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (openQueueButton != null && openQueueButton.isEnabled()) {
                    openQueueButton.doClick();
                }
            }
        });

        // F2: Add New Patient
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "addPatient");
        actionMap.put("addPatient", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (addPatientButton != null && addPatientButton.isEnabled()) {
                    addPatientButton.doClick();
                }
            }
        });

        // F5: Take Picture
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "takePicture");
        actionMap.put("takePicture", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (takePictureButton != null && takePictureButton.isEnabled()) {
                    takePictureButton.doClick();
                }
            }
        });

        // F6: Record Video
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "recordVideo");
        actionMap.put("recordVideo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (recordVideoButton != null && recordVideoButton.isEnabled()) {
                    recordVideoButton.doClick();
                }
            }
        });

        // F7: Print Invoice
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "printInvoice");
        actionMap.put("printInvoice", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionButtons[2] != null && actionButtons[2].isEnabled()) {
                    handleActionPanelClick("printer");
                }
            }
        });

        // F8: Save
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "saveAction");
        actionMap.put("saveAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionButtons[3] != null && actionButtons[3].isEnabled()) {
                    // Bypass confirmation dialog for shortcut
                    String statusToSave = (String) statusComboBox.getSelectedItem();
                    handleSave();
                    afterSaveActions(statusToSave, "Đã lưu thành công (F8)");
                }
            }
        });

        // F9: Save and View
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "saveAndView");
        actionMap.put("saveAndView", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionButtons[4] != null && actionButtons[4].isEnabled()) {
                    handleActionPanelClick("loupe");
                }
            }
        });

        // F10: Save and Print
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), "saveAndPrint");
        actionMap.put("saveAndPrint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionButtons[5] != null && actionButtons[5].isEnabled()) {
                    handleActionPanelClick("ultrasound");
                }
            }
        });
    }
}


