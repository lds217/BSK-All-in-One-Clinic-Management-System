package BsK.client.ui.component.CheckUpPage;

import java.awt.Desktop;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.common.AddDialog.AddDialog;
import BsK.client.ui.component.common.HistoryViewDialog.HistoryViewDialog;
import BsK.client.ui.component.common.MedicineDialog.MedicineDialog;
import BsK.client.ui.component.common.ServiceDialog.ServiceDialog;
import BsK.client.ui.component.common.TemplateDialog.TemplateDialog;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.CheckUpPage.PrintDialog.MedicineInvoice;
import BsK.client.ui.component.CheckUpPage.PrintDialog.UltrasoundResult;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.QueueViewPage.QueueViewPage;
import BsK.common.entity.DoctorItem;
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

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;
import java.util.Comparator;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.datatransfer.*;
import java.text.DecimalFormat;
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
import java.nio.file.WatchService;
import java.nio.file.WatchKey;
import java.nio.file.WatchEvent;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.FileSystems;
import javax.swing.JColorChooser;

// Webcam imports
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

// JavaCV imports for video recording
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

// Java imports for concurrent tasks
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.Document;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;

// --- JasperReports Imports ---
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.view.JasperViewer;
// --- End JasperReports Imports ---

// Add new imports
import BsK.common.packet.req.GetPatientHistoryRequest;
import BsK.common.packet.res.GetPatientHistoryResponse;
import BsK.common.packet.req.SaveCheckupRequest;
import BsK.common.packet.req.UploadCheckupImageRequest;
import BsK.common.packet.req.UploadCheckupPdfRequest;
import BsK.common.packet.res.UploadCheckupImageResponse;
import BsK.common.packet.res.UploadCheckupPdfResponse;
import BsK.common.util.date.DateUtils;
import javax.swing.text.JTextComponent;

import java.nio.file.DirectoryStream;
import java.util.Collections;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;


@Slf4j
public class CheckUpPage extends JPanel {

    private ScheduledExecutorService folderScanExecutor;
    private volatile boolean isScanning = false;
    private static final int SCAN_INTERVAL_MILLISECONDS = 500; // Check every 1 
    private static final int MAX_FOLDERS_TO_SCAN = 12;

    private final Set<Path> processingFiles = ConcurrentHashMap.newKeySet();
    private final Map<Path, FileAttributes> lastSeenFiles = new ConcurrentHashMap<>();

    private static class FileAttributes {
        final long size;
        final FileTime lastModified;
        final boolean isStable;
        
        FileAttributes(long size, FileTime lastModified, boolean isStable) {
            this.size = size;
            this.lastModified = lastModified;
            this.isStable = isStable;
        }
    }

    private JPanel room1StatusPanel;
    private JLabel room1StatusLabel;
    private JPanel room2StatusPanel;
    private JLabel room2StatusLabel;

    private MainFrame mainFrame;
    private List<Patient> patientQueue = new ArrayList<>();
    private String[][] rawQueueForTv = new String[][]{};
    private String[][] history;
    private DefaultTableModel historyModel;
    private JTable historyTable;
    private final ResponseListener<GetCheckUpQueueUpdateResponse> checkUpQueueUpdateListener = this::handleGetCheckUpQueueUpdateResponse;
    private final ResponseListener<GetCheckUpQueueResponse> checkUpQueueListener = this::handleGetCheckUpQueueResponse;
    private final ResponseListener<GetPatientHistoryResponse> patientHistoryListener = this::handleGetPatientHistoryResponse;
    private final ResponseListener<GetWardResponse> wardResponseListener = this::handleGetWardResponse;
    private final ResponseListener<CallPatientResponse> callPatientResponseListener = this::handleCallPatientResponse;
    private final ResponseListener<GetOrderInfoByCheckupRes> orderInfoByCheckupListener = this::handleGetOrderInfoByCheckupResponse;
    private final ResponseListener<GetAllTemplatesRes> getAllTemplatesListener = this::handleGetAllTemplatesResponse;
    private final ResponseListener<UploadCheckupImageResponse> uploadImageResponseListener = this::handleUploadImageResponse;
    private final ResponseListener<UploadCheckupPdfResponse> uploadPdfResponseListener = this::handleUploadPdfResponse;
    private final ResponseListener<SyncCheckupImagesResponse> syncImagesResponseListener = this::handleSyncImagesResponse;
    private final ResponseListener<GetCheckupImageResponse> getImageResponseListener = this::handleGetCheckupImageResponse;
    private final ResponseListener<DeleteCheckupImageResponse> deleteImageResponseListener = this::handleDeleteImageResponse;
    private JTextField checkupIdField, customerLastNameField, customerFirstNameField,customerAddressField, customerPhoneField, customerIdField, customerCccdDdcnField;
    private JTextArea suggestionField, diagnosisField, conclusionField; // Changed symptomsField to suggestionField
    private JTextPane notesField;
    private JComboBox<DoctorItem> doctorComboBox, ultrasoundDoctorComboBox;
    private JComboBox<String> statusComboBox, genderComboBox, provinceComboBox, wardComboBox, checkupTypeComboBox, templateComboBox, orientationComboBox; // Added orientationComboBox
    private JCheckBox needRecheckupCheckbox; // Checkbox to indicate if re-checkup is needed
    private JSpinner customerWeightSpinner, customerHeightSpinner, patientHeartRateSpinner, bloodPressureSystolicSpinner, bloodPressureDiastolicSpinner;
    private JDatePickerImpl datePicker, dobPicker, recheckupDatePicker;
    private JButton recheckupDatePickerButton;
    private String[][] medicinePrescription = new String[0][0]; // Initialize to empty
    private String[][] servicePrescription = new String[0][0]; // Initialize to empty
    private MedicineDialog medDialog = null;
    private ServiceDialog serDialog = null;
    private AddDialog addDialog = null;
    private String selectedCheckupId = null; // Use checkupId to track selection instead of row index
    private boolean saved = true; // Initially true, changed when patient selected or dialog opened.
    private DefaultComboBoxModel<String> wardModel;
    
    // Variables to store target ward and ward when loading patient address
    private String targetWard = null;
    private JComboBox<String> callRoomComboBox;
    private JButton callPatientButton;
    private JLabel callingStatusLabel;
    private JPanel rightContainer; // Add this field
    private List<Template> allTemplates;
    private JButton openQueueButton;
    private JButton addPatientButton;
    private JButton[] actionButtons;
    private JButton driveButton;
    
    // Template info labels
    private JLabel imageCountValueLabel;
    private JLabel genderValueLabel;

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
    private JPanel imageGalleryPanel; // Displays thumbnails
    private JScrollPane imageGalleryScrollPane;
    private JComboBox<String> webcamDeviceComboBox;
    private JButton webcamRefreshButton;
    private JButton takePictureButton;
    private JButton openFolderButton;
    private JButton recordVideoButton;
    private volatile String currentCheckupIdForMedia;
    private volatile Path currentCheckupMediaPath;
    private javax.swing.Timer imageRefreshTimer;
    // Using LocalStorage.checkupMediaBaseDir which is loaded at startup
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
    private final Object mediaStateLock = new Object();
    private final ExecutorService imageUploadExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Future<?>> uploadTimeoutTasks = new ConcurrentHashMap<>();
    
    // Ultrasound folder monitoring
    private static final String ULTRASOUND_FOLDER_PATH = LocalStorage.ULTRASOUND_FOLDER_PATH;
    private WatchService watchService;
    private ExecutorService folderWatchExecutor;
    private volatile boolean isWatchingFolder = false;

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
        String[][] tableData = new String[patients.size()][7];
        
        for (int i = 0; i < patients.size(); i++) {
            Patient p = patients.get(i);
            tableData[i][0] = p.getQueueNumber(); // STT
            tableData[i][1] = p.getCheckupId(); // Mã Khám
            tableData[i][2] = p.getCustomerLastName() + " " + p.getCustomerFirstName(); // Họ và Tên
            tableData[i][3] = String.valueOf(DateUtils.extractYearFromTimestamp(p.getCustomerDob())); // Năm sinh
            tableData[i][4] = p.getDoctorName(); // Bác Sĩ
            tableData[i][5] = p.getCheckupType(); // Loại khám
            tableData[i][6] = p.getStatus(); // Trạng thái
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
            Files.createDirectories(Paths.get(LocalStorage.checkupMediaBaseDir));
            log.info("Created or verified media directory at: {}", LocalStorage.checkupMediaBaseDir);
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
        ClientHandler.addResponseListener(GetWardResponse.class, wardResponseListener);
        ClientHandler.addResponseListener(CallPatientResponse.class, callPatientResponseListener);
        ClientHandler.addResponseListener(GetAllTemplatesRes.class, getAllTemplatesListener);
        ClientHandler.addResponseListener(UploadCheckupImageResponse.class, uploadImageResponseListener);
        ClientHandler.addResponseListener(UploadCheckupPdfResponse.class, uploadPdfResponseListener);
        ClientHandler.addResponseListener(SyncCheckupImagesResponse.class, syncImagesResponseListener);
        ClientHandler.addResponseListener(GetCheckupImageResponse.class, getImageResponseListener);
        ClientHandler.addResponseListener(DeleteCheckupImageResponse.class, deleteImageResponseListener);

        // Instantiate the new queue page but don't show it yet
        queueManagementPage = new QueueManagementPage();
        
        // Initialize ultrasound folder monitoring
        initializeUltrasoundFolderScanner();
        
        // Stop automatic webcam discovery to prevent background scanning
        try {
            Webcam.getDiscoveryService().stop();
            log.info("Stopped automatic webcam discovery service on startup");
        } catch (Exception e) {
            log.debug("Webcam discovery service stop on startup (non-critical): {}", e.getMessage());
        }

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
            // Always fetch the latest queue to avoid cross-client mismatch
            updateQueue();
            // Reset filters to "Tất cả" to avoid index/filter/sort mismatch on open
            if (queueManagementPage != null) {
                queueManagementPage.checkupTypeFilter.setSelectedItem("Tất cả");

                queueManagementPage.applyFilters();
            }
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
            // Unregister the listener from the parent page to avoid conflicts
            ClientHandler.deleteListener(GetWardResponse.class, wardResponseListener);

            // Dispose any existing dialog to ensure clean listener removal
            if (addDialog != null) {
                addDialog.dispose();
            }
            addDialog = new AddDialog(mainFrame) {
                @Override
                public void dispose() {
                    super.dispose();
                    addDialog = null;  // Clear the reference when dialog is disposed
                    // Re-register the listener after the dialog is actually disposed
                    ClientHandler.addResponseListener(GetWardResponse.class, wardResponseListener);
                    log.info("AddDialog disposed, re-registered ward listener for CheckUpPage.");
                }
            };
            
            // Show the dialog without blocking the main thread
            SwingUtilities.invokeLater(() -> {
                addDialog.setVisible(true);
                // The re-registration will now happen inside the dispose() override
            });

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
                "Đơn thuốc và Dịch vụ (tham khảo)",
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

        gbcPatient.gridx = 1; gbcPatient.weightx = 0.4; gbcPatient.gridwidth = 2; // <-- MODIFIED from 3 to 2
        customerCccdDdcnField = new JTextField(15);
        customerCccdDdcnField.setFont(fieldFont);
        addSelectAllOnFocus(customerCccdDdcnField);
        patientInfoInnerPanel.add(customerCccdDdcnField, gbcPatient);
        gbcPatient.gridwidth = 1; // Reset gridwidth

        // --- START: ADD THE NEW DRIVE BUTTON HERE ---
        gbcPatient.gridx = 3; gbcPatient.weightx = 0.2; // Add to the right
        driveButton = new JButton("Drive Bệnh nhân");
        driveButton.setFont(new Font("Arial", Font.BOLD, 14));
        driveButton.setBackground(new Color(26, 115, 232)); // Google Blue color
        driveButton.setForeground(Color.WHITE);
        driveButton.setFocusPainted(false);
        driveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        driveButton.setEnabled(false); // Disabled by default
        driveButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        // Load and set the icon
        try {
            String iconPath = "src/main/java/BsK/client/ui/assets/icon/google-drive.png";
            ImageIcon driveIcon = new ImageIcon(new ImageIcon(iconPath).getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
            driveButton.setIcon(driveIcon);
        } catch (Exception e) {
            log.error("Failed to load Google Drive icon", e);
        }

        // Add action listener to open the URL
        driveButton.addActionListener(e -> {
            Patient currentPatient = getCurrentSelectedPatient();
            if (currentPatient != null && currentPatient.getDriveUrl() != null && !currentPatient.getDriveUrl().isEmpty()) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI(currentPatient.getDriveUrl()));
                } catch (Exception ex) {
                    log.error("Could not open patient drive URL: {}", currentPatient.getDriveUrl(), ex);
                    JOptionPane.showMessageDialog(this, "Không thể mở liên kết Drive:\n" + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Bệnh nhân này không có liên kết Google Drive.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        patientInfoInnerPanel.add(driveButton, gbcPatient);
        // --- END: NEW DRIVE BUTTON CODE ---

        // Row 1: Name
        gbcPatient.gridx = 0; gbcPatient.gridy = 1; gbcPatient.weightx = 0.1;
        JLabel hoLabel = new JLabel("Họ", SwingConstants.RIGHT);
        hoLabel.setFont(labelFont);
        patientInfoInnerPanel.add(hoLabel, gbcPatient);

        gbcPatient.gridx = 1; gbcPatient.weightx = 0.4;
        customerLastNameField = new JTextField(20);
        customerLastNameField.setFont(fieldFont);
        addSelectAllOnFocus(customerLastNameField);
        patientInfoInnerPanel.add(customerLastNameField, gbcPatient);

        gbcPatient.gridx = 2; gbcPatient.weightx = 0.1;
        JLabel tenLabel = new JLabel("Tên", SwingConstants.RIGHT);
        tenLabel.setFont(labelFont);
        patientInfoInnerPanel.add(tenLabel, gbcPatient);

        gbcPatient.gridx = 3; gbcPatient.weightx = 0.4;
        customerFirstNameField = new JTextField(15);
        customerFirstNameField.setFont(fieldFont);
        addSelectAllOnFocus(customerFirstNameField);
        patientInfoInnerPanel.add(customerFirstNameField, gbcPatient);

        // Row 2: ID and Phone
        gbcPatient.gridx = 0; gbcPatient.gridy = 2; gbcPatient.weightx = 0.1;
        JLabel idLabel = new JLabel("Mã Khám", SwingConstants.RIGHT);
        idLabel.setFont(labelFont);
        patientInfoInnerPanel.add(idLabel, gbcPatient);

        gbcPatient.gridx = 1; gbcPatient.weightx = 0.4;
        checkupIdField = new JTextField(15);
        checkupIdField.setFont(fieldFont);
        checkupIdField.setEditable(false);
        patientInfoInnerPanel.add(checkupIdField, gbcPatient);

        customerIdField = new JTextField(15);
        customerIdField.setFont(fieldFont);
        customerIdField.setEditable(false);
        

        // Initialize checkupIdField
        

        gbcPatient.gridx = 2; gbcPatient.weightx = 0.1;
        JLabel phoneLabel = new JLabel("SĐT", SwingConstants.RIGHT);
        phoneLabel.setFont(labelFont);
        patientInfoInnerPanel.add(phoneLabel, gbcPatient);

        gbcPatient.gridx = 3; gbcPatient.weightx = 0.4;
        customerPhoneField = new JTextField(15);
        customerPhoneField.setFont(fieldFont);
        addSelectAllOnFocus(customerPhoneField);
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
        dobProperties.put("text.month", "Tháng");
        dobProperties.put("text.year", "Năm");
        // Remove "text.today" since these dates shouldn't default to today
        JDatePanelImpl dobPanel = new JDatePanelImpl(dobModel, dobProperties);
        dobPicker = new JDatePickerImpl(dobPanel, new DateLabelFormatter());
        dobPicker.getJFormattedTextField().setFont(fieldFont);
        dobPicker.getJFormattedTextField().setToolTipText("Nhập ngày theo định dạng dd/mm/yyyy");
        setupDateFieldForDirectInput(dobPicker);
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
            JFormattedTextField tf = ((JSpinner.DefaultEditor) weightEditor).getTextField();
            tf.setFont(fieldFont);
            addSelectAllOnFocus(tf);
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
            JFormattedTextField tf = ((JSpinner.DefaultEditor) heightEditor).getTextField();
            tf.setFont(fieldFont);
            addSelectAllOnFocus(tf);
        }
        patientInfoInnerPanel.add(customerHeightSpinner, gbcPatient);

        // Row 5: Heart Rate and Blood Pressure
        gbcPatient.gridy++;
        gbcPatient.gridx = 0;
        JLabel heartRateLabel = new JLabel("Nhịp tim (l/p)", SwingConstants.RIGHT);
        heartRateLabel.setFont(labelFont);
        patientInfoInnerPanel.add(heartRateLabel, gbcPatient);

        gbcPatient.gridx = 1;
        SpinnerModel heartRateModel = new SpinnerNumberModel(80, 0, 250, 1);
        patientHeartRateSpinner = new JSpinner(heartRateModel);
        patientHeartRateSpinner.setFont(fieldFont);
        JComponent heartRateEditor = patientHeartRateSpinner.getEditor();
        if (heartRateEditor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) heartRateEditor).getTextField();
            tf.setFont(fieldFont);
            addSelectAllOnFocus(tf);
        }
        patientInfoInnerPanel.add(patientHeartRateSpinner, gbcPatient);

        gbcPatient.gridx = 2;
        JLabel bloodPressureLabel = new JLabel("Huyết áp (mmHg)", SwingConstants.RIGHT);
        bloodPressureLabel.setFont(labelFont);
        patientInfoInnerPanel.add(bloodPressureLabel, gbcPatient);

        gbcPatient.gridx = 3;
        JPanel bloodPressurePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bloodPressurePanel.setOpaque(false);

        SpinnerModel systolicModel = new SpinnerNumberModel(120, 0, 300, 1);
        bloodPressureSystolicSpinner = new JSpinner(systolicModel);
        bloodPressureSystolicSpinner.setFont(fieldFont);
        JComponent systolicEditor = bloodPressureSystolicSpinner.getEditor();
        if (systolicEditor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) systolicEditor).getTextField();
            tf.setFont(fieldFont);
            addSelectAllOnFocus(tf);
        }

        SpinnerModel diastolicModel = new SpinnerNumberModel(80, 0, 200, 1);
        bloodPressureDiastolicSpinner = new JSpinner(diastolicModel);
        bloodPressureDiastolicSpinner.setFont(fieldFont);
        JComponent diastolicEditor = bloodPressureDiastolicSpinner.getEditor();
        if (diastolicEditor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) diastolicEditor).getTextField();
            tf.setFont(fieldFont);
            addSelectAllOnFocus(tf);
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
        addSelectAllOnFocus(customerAddressField);
        patientInfoInnerPanel.add(customerAddressField, gbcPatient);

        // Row 7: Location dropdowns
        gbcPatient.gridx = 0; gbcPatient.gridy = 7;
        gbcPatient.gridwidth = 1; // Reset gridwidth
        JLabel provinceLabel = new JLabel("Tỉnh/Thành phố", SwingConstants.RIGHT);
        provinceLabel.setFont(labelFont);
        patientInfoInnerPanel.add(provinceLabel, gbcPatient);

        gbcPatient.gridwidth = 1; // Reset gridwidth
        gbcPatient.gridx = 1; gbcPatient.gridy = 7;
        provinceComboBox = new JComboBox<>(LocalStorage.provinces);
        provinceComboBox.setFont(fieldFont);
        patientInfoInnerPanel.add(provinceComboBox, gbcPatient);
        
        // Add Xã/Phường label before wardComboBox
        gbcPatient.gridx = 2; gbcPatient.gridy = 7;
        JLabel wardLabel = new JLabel("Xã/Phường", SwingConstants.RIGHT);
        wardLabel.setFont(labelFont);
        patientInfoInnerPanel.add(wardLabel, gbcPatient);

        gbcPatient.gridx = 3;
        wardModel = new DefaultComboBoxModel<>(new String[]{"Xã/Phường"});
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
        gbcRoom.insets = new Insets(5, 8, 5, 8);

        // --- Left Side: Controls ---

        // Room Selection Label
        gbcRoom.fill = GridBagConstraints.HORIZONTAL;
        gbcRoom.gridx = 0; gbcRoom.gridy = 0;
        JLabel roomLabel = new JLabel("Phòng khám:", SwingConstants.RIGHT);
        roomLabel.setFont(labelFont);
        roomControlPanel.add(roomLabel, gbcRoom);

        // Room Selection ComboBox
        gbcRoom.gridx = 1;
        String[] roomOptions = {"Phòng 1", "Phòng 2"};
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
        callPatientButton.addActionListener(e -> handleCallPatient()); // Ensure this method updates the panels
        roomControlPanel.add(callPatientButton, gbcRoom);

        // Free Room Button (spans the width of the controls)
        gbcRoom.gridx = 0; gbcRoom.gridy = 1; gbcRoom.gridwidth = 3;
        JButton freeRoomButton = new JButton("Đánh dấu phòng trống");
        freeRoomButton.setFont(fieldFont);
        freeRoomButton.setBackground(new Color(46, 204, 113));
        freeRoomButton.setForeground(Color.WHITE);
        freeRoomButton.setFocusPainted(false);
        freeRoomButton.addActionListener(e -> handleFreeRoom()); // Ensure this method updates the panels
        roomControlPanel.add(freeRoomButton, gbcRoom);


        // --- Right Side: Status Panels ---

        // Reset constraints for status panels
        gbcRoom.gridwidth = 1;
        gbcRoom.gridheight = 2; // Make panels span 2 rows vertically
        gbcRoom.fill = GridBagConstraints.BOTH; // Fill available space

        // Room 1 Status Panel
        gbcRoom.gridx = 3; gbcRoom.gridy = 0;
        room1StatusPanel = new JPanel(new BorderLayout());
        room1StatusPanel.setPreferredSize(new Dimension(110, 0)); // Set width, height is controlled by layout
        room1StatusLabel = new JLabel("", SwingConstants.CENTER);
        room1StatusLabel.putClientProperty("baseText", "PHÒNG 1"); // Store base text for reuse
        styleRoomStatusPanel(room1StatusPanel, room1StatusLabel, Color.GREEN.darker(), "TRỐNG"); // Initial state
        room1StatusPanel.add(room1StatusLabel, BorderLayout.CENTER);
        roomControlPanel.add(room1StatusPanel, gbcRoom);

        // Room 2 Status Panel
        gbcRoom.gridx = 4; gbcRoom.gridy = 0;
        room2StatusPanel = new JPanel(new BorderLayout());
        room2StatusPanel.setPreferredSize(new Dimension(110, 0)); // Set width, height is controlled by layout
        room2StatusLabel = new JLabel("", SwingConstants.CENTER);
        room2StatusLabel.putClientProperty("baseText", "PHÒNG 2"); // Store base text for reuse
        styleRoomStatusPanel(room2StatusPanel, room2StatusLabel, Color.GREEN.darker(), "TRỐNG"); // Initial state
        room2StatusPanel.add(room2StatusLabel, BorderLayout.CENTER);
        roomControlPanel.add(room2StatusPanel, gbcRoom);


        // --- Add the completed panel to your main layout ---
        // This part remains the same
        gbcPatient.gridx = 0; gbcPatient.gridy = 8; gbcPatient.gridwidth = 4; // Adjust gridwidth if needed
        patientInfoInnerPanel.add(roomControlPanel, gbcPatient);

        // Add back the event listeners
        provinceComboBox.addActionListener(e -> {
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            if (selectedProvince != null && !selectedProvince.equals("Tỉnh/Thành phố")) {
                String provinceId = LocalStorage.provinceToId.get(selectedProvince);
                if (provinceId != null) {
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(provinceId));
                    wardComboBox.setEnabled(false); 
                    wardModel.removeAllElements(); 
                    wardModel.addElement("Đang tải xã/phường...");
                }
            } else {
                wardComboBox.setEnabled(false); 
                wardModel.removeAllElements(); 
                wardModel.addElement("Xã/Phường");
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
        gbcTop.insets = new Insets(1, 5, 1, 5); // Reduced vertical insets for density
        gbcTop.fill = GridBagConstraints.HORIZONTAL;

        // Initialize datePicker
        UtilDateModel dateModel = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.month", "Tháng");
        p.put("text.year", "Năm");
        // Remove "text.today" since these dates shouldn't default to today
        JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.setPreferredSize(new Dimension(120, 30));
        datePicker.getJFormattedTextField().setFont(fieldFont);
        datePicker.getJFormattedTextField().setToolTipText("Nhập ngày theo định dạng dd/mm/yyyy");
        setupDateFieldForDirectInput(datePicker);

        gbcTop.gridx = 0; gbcTop.gridy = 0;
        JLabel dateLabel = new JLabel("Đơn Ngày:");
        dateLabel.setFont(labelFont);
        topRowPanel.add(dateLabel, gbcTop);

        gbcTop.gridx = 1; gbcTop.weightx = 0.3;
        topRowPanel.add(datePicker, gbcTop);

        gbcTop.gridx = 2; gbcTop.weightx = 0;
        JLabel statusLabel = new JLabel("Trạng thái");
        statusLabel.setFont(labelFont);
        topRowPanel.add(statusLabel, gbcTop);

        gbcTop.gridx = 3; gbcTop.weightx = 0.3;
        statusComboBox = new JComboBox<>(new String[]{"ĐANG KHÁM", "CHỜ KHÁM", "ĐÃ KHÁM"});
        statusComboBox.setFont(fieldFont);
        topRowPanel.add(statusComboBox, gbcTop);

        gbcTop.gridx = 4; gbcTop.weightx = 0;
        JLabel typeLabel = new JLabel("Loại khám");
        typeLabel.setFont(labelFont);
        topRowPanel.add(typeLabel, gbcTop);

        gbcTop.gridx = 5; gbcTop.weightx = 0.3;
        checkupTypeComboBox = new JComboBox<>(new String[]{"PHỤ KHOA", "THAI", "KHÁC"});
        checkupTypeComboBox.setFont(fieldFont);
        topRowPanel.add(checkupTypeComboBox, gbcTop);

        // Doctor Selection Row
        JPanel doctorPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcDoctor = new GridBagConstraints();
        gbcDoctor.insets = new Insets(1, 5, 1, 5); // Reduced vertical insets for density
        gbcDoctor.fill = GridBagConstraints.HORIZONTAL;

        gbcDoctor.gridx = 0; gbcDoctor.gridy = 0;
        JLabel doctorLabel = new JLabel("Bác sĩ chỉ định");
        doctorLabel.setFont(labelFont);
        doctorPanel.add(doctorLabel, gbcDoctor);

        gbcDoctor.gridx = 1; gbcDoctor.weightx = 0.4;
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName.toArray(new DoctorItem[0]));
        doctorComboBox.setFont(fieldFont);
        doctorPanel.add(doctorComboBox, gbcDoctor);

        gbcDoctor.gridx = 2; gbcDoctor.weightx = 0;
        JLabel ultrasoundDoctorLabel = new JLabel("Bác sĩ siêu âm");
        ultrasoundDoctorLabel.setFont(labelFont);
        doctorPanel.add(ultrasoundDoctorLabel, gbcDoctor);

        gbcDoctor.gridx = 3; gbcDoctor.weightx = 0.4;
        ultrasoundDoctorComboBox = new JComboBox<>(LocalStorage.doctorsName.toArray(new DoctorItem[0]));
        ultrasoundDoctorComboBox.setFont(fieldFont);
        doctorPanel.add(ultrasoundDoctorComboBox, gbcDoctor);

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

        // Add template info labels between template selection and orientation
        gbcTemplate.gridx = 2; gbcTemplate.weightx = 0.0;
        JLabel imageCountInfoLabel = new JLabel("SL ảnh:");
        imageCountInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        imageCountInfoLabel.setForeground(new Color(100, 100, 100));
        templatePanel.add(imageCountInfoLabel, gbcTemplate);

        gbcTemplate.gridx = 3;
        imageCountValueLabel = new JLabel("-");
        imageCountValueLabel.setFont(new Font("Arial", Font.BOLD, 12));
        imageCountValueLabel.setForeground(new Color(63, 81, 181));
        templatePanel.add(imageCountValueLabel, gbcTemplate);

        gbcTemplate.gridx = 4;
        JLabel genderInfoLabel = new JLabel("Giới:");
        genderInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        genderInfoLabel.setForeground(new Color(100, 100, 100));
        templatePanel.add(genderInfoLabel, gbcTemplate);

        gbcTemplate.gridx = 5;
        genderValueLabel = new JLabel("-");
        genderValueLabel.setFont(new Font("Arial", Font.BOLD, 12));
        genderValueLabel.setForeground(new Color(63, 81, 181));
        templatePanel.add(genderValueLabel, gbcTemplate);

        
        // Add "Thêm mẫu" button next to template combobox
        // Add orientation dropdown
        gbcTemplate.gridx = 6; gbcTemplate.weightx = 0.0;
        JLabel orientationLabel = new JLabel("Hướng in:");
        orientationLabel.setFont(labelFont);
        templatePanel.add(orientationLabel, gbcTemplate);

        gbcTemplate.gridx = 7;
        orientationComboBox = new JComboBox<>(new String[]{"Ngang", "Dọc"});
        orientationComboBox.setFont(fieldFont);
        orientationComboBox.setPreferredSize(new Dimension(80, 25));
        orientationComboBox.addActionListener(e -> {
            printType = (String) orientationComboBox.getSelectedItem();
        });
        templatePanel.add(orientationComboBox, gbcTemplate);

        // Add template button
        gbcTemplate.gridx = 8; gbcTemplate.weightx = 0.0;
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
        // addSelectAllOnFocus(notesField); // Do NOT select all for notes field
        
        // Set default font and size
        notesField.setFont(new Font("Arial", Font.PLAIN, 16));
        
        // Set up RTF editor kit with proper character encoding
        RTFEditorKit rtfKit = new RTFEditorKit();
        notesField.setEditorKit(rtfKit);
        
        // Set up document properties
        Document doc = notesField.getDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        
        // Set up empty default template
        String defaultTemplate = "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Arial;}}\n" +
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
        String defaultRtfTemplate = "{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Arial;}}\n" +
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
        String[] fontFamilies = {"Arial", "Arial", "Verdana", "Courier New", "Tahoma", "Calibri"};
        JComboBox<String> fontFamilyComboBox = new JComboBox<>(fontFamilies);
        fontFamilyComboBox.setSelectedItem("Arial"); // Match default font of notesField
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

        // Suggestion Panel - more compact
        JPanel suggestionPanel = new JPanel(new BorderLayout(3, 3)); // Reduced spacing
        suggestionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Đề nghị",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16), 
            new Color(50, 50, 50)
        ));
        suggestionField = new JTextArea(2, 10); // Further reduced to 2 rows
        suggestionField.setFont(fieldFont);
        suggestionField.setLineWrap(true);
        suggestionField.setWrapStyleWord(true);
        // addSelectAllOnFocus(suggestionField);
        JScrollPane suggestionScrollPane = new JScrollPane(suggestionField);
        suggestionScrollPane.setPreferredSize(new Dimension(0, 50)); // Reduced height
        suggestionPanel.add(suggestionScrollPane, BorderLayout.CENTER);
        suggestionPanel.setMinimumSize(new Dimension(200, 90)); // Reduced height
        suggestionPanel.setPreferredSize(new Dimension(280, 90)); // Reduced height

        // Add Re-checkup date picker - even more compact layout
        JPanel recheckupPanel = new JPanel();
        recheckupPanel.setLayout(new BoxLayout(recheckupPanel, BoxLayout.X_AXIS)); // More compact layout
        recheckupPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // No border
        recheckupPanel.setBackground(Color.WHITE);
        JLabel recheckupLabel = new JLabel("Tái khám:");
        recheckupLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Even smaller font
        recheckupPanel.add(recheckupLabel);
        recheckupPanel.add(Box.createHorizontalStrut(2)); // Minimal spacing
        
        // Add checkbox for re-checkup needed - ultra compact
        needRecheckupCheckbox = new JCheckBox();
        needRecheckupCheckbox.setFont(new Font("Arial", Font.PLAIN, 11)); // Even smaller font
        needRecheckupCheckbox.setBackground(Color.WHITE);
        needRecheckupCheckbox.setSelected(false);
        needRecheckupCheckbox.setMargin(new Insets(0, 0, 0, 0)); // Remove margin
        needRecheckupCheckbox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // No border
        needRecheckupCheckbox.addActionListener(e -> {
            boolean selected = needRecheckupCheckbox.isSelected();
            recheckupDatePicker.setEnabled(selected);
            recheckupDatePicker.getJFormattedTextField().setEditable(selected);
            if(recheckupDatePickerButton != null) {
                recheckupDatePickerButton.setEnabled(selected);
            }
            if (!selected) {
                // Clear the date if checkbox is unchecked
                recheckupDatePicker.getModel().setValue(null);
            }
        });
        recheckupPanel.add(needRecheckupCheckbox);
        
        UtilDateModel recheckupModel = new UtilDateModel();
        Properties recheckupProps = new Properties();
        recheckupProps.put("text.month", "Tháng");
        recheckupProps.put("text.year", "Năm");
        // Remove "text.today" since these dates shouldn't default to today
        JDatePanelImpl recheckupDatePanel = new JDatePanelImpl(recheckupModel, recheckupProps);
        recheckupDatePicker = new JDatePickerImpl(recheckupDatePanel, new DateLabelFormatter());
        // Find and store the button component
        for (Component comp : recheckupDatePicker.getComponents()) {
            if (comp instanceof JButton) {
                recheckupDatePickerButton = (JButton) comp;
                break;
            }
        }
        recheckupDatePicker.getJFormattedTextField().setFont(new Font("Arial", Font.PLAIN, 12)); // Smaller font
        recheckupDatePicker.getJFormattedTextField().setToolTipText("Nhập ngày theo định dạng dd/mm/yyyy");
        setupDateFieldForDirectInput(recheckupDatePicker);
        recheckupDatePicker.setPreferredSize(new Dimension(120, 25)); // Even smaller dimensions
        recheckupDatePicker.setEnabled(false); // Initially disabled until checkbox is checked
        recheckupDatePicker.getJFormattedTextField().setEditable(false);
        if (recheckupDatePickerButton != null) {
            recheckupDatePickerButton.setEnabled(false);
        }
        recheckupPanel.add(recheckupDatePicker);
        
        // Adjust the layout to make room for the new checkbox
        suggestionPanel.setPreferredSize(new Dimension(280, 200)); // Increased width from 250 to 280
        rightPanel.setPreferredSize(new Dimension(280, 0)); // Increased width from 250 to 280
        rightPanel.setMinimumSize(new Dimension(230, 0)); // Increased minimum width from 200 to 230
        
        suggestionPanel.add(recheckupPanel, BorderLayout.SOUTH);

        suggestionPanel.setMinimumSize(new Dimension(200, 200));
        suggestionPanel.setPreferredSize(new Dimension(250, 200));

        // Diagnosis Panel - more compact
        JPanel diagnosisPanel = new JPanel(new BorderLayout(3, 3)); // Reduced spacing
        diagnosisPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Chẩn đoán",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16), 
            new Color(50, 50, 50)
        ));
        diagnosisField = new JTextArea(3, 20); // Reduced rows
        diagnosisField.setFont(new Font("Arial", Font.BOLD, 16)); // Slightly smaller font
        diagnosisField.setLineWrap(true);
        diagnosisField.setWrapStyleWord(true);
        // addSelectAllOnFocus(diagnosisField);
        diagnosisPanel.add(new JScrollPane(diagnosisField), BorderLayout.CENTER);

        // Conclusion Panel - more compact
        JPanel conclusionPanel = new JPanel(new BorderLayout(3, 3)); // Reduced spacing
        conclusionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Kết luận",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16), 
            new Color(50, 50, 50)
        ));
        conclusionField = new JTextArea(3, 20); // Reduced rows
        conclusionField.setFont(new Font("Arial", Font.BOLD, 16)); // Slightly smaller font
        conclusionField.setLineWrap(true);
        conclusionField.setWrapStyleWord(true);
        // addSelectAllOnFocus(conclusionField);
        conclusionPanel.add(new JScrollPane(conclusionField), BorderLayout.CENTER);

        rightPanel.add(suggestionPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Reduced from 10 to 5
        rightPanel.add(diagnosisPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Reduced from 10 to 5
        rightPanel.add(conclusionPanel);

        // Create split pane for left and right panels
        JSplitPane contentSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        contentSplitPane.setResizeWeight(0.85); // Give maximum weight to the left panel
        contentSplitPane.setDividerSize(3); // Make divider even smaller
        contentSplitPane.setBorder(null);
        contentSplitPane.setOneTouchExpandable(true); // Add one-touch expand/collapse buttons

        // Add to main content panel
        mainContentPanel.add(templatePanel, BorderLayout.NORTH);
        mainContentPanel.add(contentSplitPane, BorderLayout.CENTER);

        // Create a container panel to hold the top rows with vertical layout
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(topRowPanel);
        topContainer.add(doctorPanel);

        // Assemble checkup info panel
        checkupInfoPanel.add(topContainer, BorderLayout.NORTH);
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
        String historyColumns[] = {"Ngày khám", "Loại khám", "Kết luận", "Cao (cm)", "Nặng (kg)", "Nhịp tim", "Huyết áp"};
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
                        // Ensure the selected record has enough data fields (now need 15 for vitals)
                        if (selectedHistory != null && selectedHistory.length >= 15) {
                            String checkupId = selectedHistory[1];
                            String patientName = customerFirstNameField.getText() + " " + customerLastNameField.getText();
                            String checkupDate = selectedHistory[0];
                            String suggestion = selectedHistory[2];
                            String diagnosis = selectedHistory[3];
                            String notes = selectedHistory[5];
                            String conclusion = selectedHistory[7];
                            String reCheckupDate = selectedHistory[8];
                            String doctorName = selectedHistory[9] + " " + selectedHistory[10];
                            String customerHeight = selectedHistory[11]; // customer_height
                            String customerWeight = selectedHistory[12]; // customer_weight
                            String heartRate = selectedHistory[13]; // heart_beat
                            String bloodPressure = selectedHistory[14]; // blood_pressure

                            openHistoryViewDialog(checkupId, patientName, checkupDate, suggestion, diagnosis, conclusion, notes, reCheckupDate, doctorName, customerHeight, customerWeight, heartRate, bloodPressure);
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

        // Listen for CallPatientResponse to update the TV queue display
        ClientHandler.addResponseListener(CallPatientResponse.class, response -> {
            log.info("Received CallPatientResponse: PatientId={}, RoomId={}, Status={}, QueueNumber={}",
                    response.getPatientId(), response.getRoomId(), response.getStatus(), response.getQueueNumber());

            if (tvQueueFrame != null && tvQueueFrame.isShowing()) {
                // Find the full patient name and birth year from patient queue
                String patientName = "";
                String birthYear = "";
                
                // Look up the patient in our patient queue by checkup ID
                for (Patient patient : patientQueue) {
                    if (patient.getCheckupId().equals(String.valueOf(response.getPatientId()))) {
                        patientName = patient.getCustomerLastName() + " " + patient.getCustomerFirstName();
                        
                        // Extract year from DOB
                        if (patient.getCustomerDob() != null && !patient.getCustomerDob().isEmpty()) {
                            int year = DateUtils.extractYearFromTimestamp(patient.getCustomerDob());
                            if (year != -1) {
                                birthYear = String.valueOf(year);
                            }
                        }
                        break;
                    }
                }

                String fullPatientInfo = patientName + " (" + birthYear + ")";

                // Pass the QUEUE NUMBER to be displayed in the small room status box
                tvQueueFrame.updateSpecificRoomStatus(
                        response.getRoomId(),
                        String.valueOf(response.getPatientId()),
                        response.getQueueNumber(), // Use the queue number here
                        fullPatientInfo,           // Full info for the central display
                        response.getStatus()
                );
            }
        });
    }

    private void addSelectAllOnFocus(JTextComponent textComponent) {
        textComponent.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(textComponent::selectAll);
            }
        });
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

    /**
     * Creates a date comparator for table sorting that properly handles dd/MM/yyyy format dates
     * @return Comparator for date strings in dd/MM/yyyy format
     */
    private Comparator<String> createDateComparator() {
        return new Comparator<String>() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            
            @Override
            public int compare(String date1, String date2) {
                try {
                    if (date1 == null || date1.trim().isEmpty()) {
                        return date2 == null || date2.trim().isEmpty() ? 0 : 1;
                    }
                    if (date2 == null || date2.trim().isEmpty()) {
                        return -1;
                    }
                    
                    Date d1 = dateFormat.parse(date1.trim());
                    Date d2 = dateFormat.parse(date2.trim());
                    return d1.compareTo(d2);
                } catch (ParseException e) {
                    // If parsing fails, fall back to string comparison
                    return date1.compareTo(date2);
                }
            }
        };
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

    /**
     * Helper method to get the currently selected patient from the queue
     * @return The currently selected Patient object, or null if none selected
     */
    private Patient getCurrentSelectedPatient() {
        if (selectedCheckupId == null) {
            return null;
        }
        
        // Find the patient in the queue by checkupId
        for (Patient patient : patientQueue) {
            if (patient != null && selectedCheckupId.equals(patient.getCheckupId())) {
                return patient;
            }
        }
        return null;
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
                    // make a dialog saying that there are no medicine or service
                    JOptionPane.showMessageDialog(this, "Không có thuốc hoặc dịch vụ để in.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Separate regular medications and supplements
                List<String[]> regularMeds = new ArrayList<>();
                List<String[]> supplements = new ArrayList<>();
                
                if (medicinePrescription != null) {
                    for (String[] med : medicinePrescription) {
                        // The supplement flag is at index 10 (after returned from the med dialog)
                        if ("1".equals(med[10])) {
                            log.info("Supplement: {}", med[1]);
                            supplements.add(med);
                        } else {
                            log.info("Regular medicine: {}", med[1]);
                            regularMeds.add(med);
                        }
                    }
                }
                
                Patient printerPatient = getCurrentSelectedPatient();
                String printerPatientDriveUrl = (printerPatient != null) ? printerPatient.getDriveUrl() : "";

                DoctorItem selectedDoctor = (DoctorItem) doctorComboBox.getSelectedItem();
                String doctorName = (selectedDoctor != null) ? selectedDoctor.getName() : "";
                
                // Show papaer size dialog 
                String paperSize = (String) JOptionPane.showInputDialog(this, "Chọn kích thước giấy in:", "Chọn kích thước giấy in", JOptionPane.QUESTION_MESSAGE, null, new Object[] {"A4", "A5"}, "A4");
                if (paperSize == null) {
                    return;
                }

                MedicineInvoice medicineInvoice = new MedicineInvoice(checkupIdField.getText(),
                        customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                        dobPicker.getJFormattedTextField().getText(), customerPhoneField.getText(),
                        genderComboBox.getSelectedItem().toString(),
                        customerAddressField.getText()  + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                        doctorName.toUpperCase(), 
                        diagnosisField.getText(),
                        conclusionField.getText(),
                        printerPatientDriveUrl,
                        regularMeds.toArray(new String[0][]),
                        servicePrescription,
                        supplements.toArray(new String[0][]), // Pass supplements
                        paperSize
                );
                // First, show the print preview to the user
                try {
                    medicineInvoice.showDirectJasperViewer(); 
                    medicineInvoice.generatePdfBytesAsync().thenAccept(pdfBytes -> {
                        if (pdfBytes != null && pdfBytes.length > 0) {
                            String checkupId = checkupIdField.getText();
                            String fileName = "medserinvoice.pdf";
                            String pdfType = "medserinvoice";

                            log.info("Uploading {} ({}) for checkupId: {}", fileName, pdfType, checkupId);

                            UploadCheckupPdfRequest request = new UploadCheckupPdfRequest(checkupId, pdfBytes, fileName, pdfType);
                            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
                        }
                    }).exceptionally(ex -> {
                        log.error("Failed to generate or upload medicine invoice PDF", ex);
                        // Show error in the UI thread
                        SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, "Lỗi khi tạo hoặc tải lên file PDF hóa đơn: " + ex.getMessage(), "Lỗi PDF", JOptionPane.ERROR_MESSAGE)
                        );
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed to show direct JasperViewer", e);
                    JOptionPane.showMessageDialog(this, "Lỗi khi hiển thị hộp thoại in: " + e.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
                }

                
                break;
            case "ultrasound": // This case now handles "Lưu & In"

                 if (photoNum != 0 && selectedImagesForPrint.size() > photoNum) {
                    JOptionPane.showMessageDialog(this,
                            "Số lượng ảnh đã chọn (" + selectedImagesForPrint.size() + ") nhiều hơn số lượng cho phép trong mẫu (" + photoNum + ").",
                            "Quá nhiều ảnh", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Step 2: Attempt to create the report object BEFORE saving anything.
                // Get current patient's Google Drive URL for QR code
                Patient currentPatient = getCurrentSelectedPatient();
                String currentPatientDriveUrl = (currentPatient != null) ? currentPatient.getDriveUrl() : "";
                
                DoctorItem selectedDoctorForPrint = (DoctorItem) doctorComboBox.getSelectedItem();
                String doctorNameForPrint = (selectedDoctorForPrint != null) ? selectedDoctorForPrint.getName() : "";
                
                DoctorItem selectedUltrasoundDoctorForPrint = (DoctorItem) ultrasoundDoctorComboBox.getSelectedItem();
                String ultrasoundDoctorNameForPrint = (selectedUltrasoundDoctorForPrint != null) ? selectedUltrasoundDoctorForPrint.getName() : "";
                
                UltrasoundResult ultrasoundResultPrint = new UltrasoundResult(
                        checkupIdField.getText(),
                        customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                        dobPicker.getJFormattedTextField().getText(),
                        (String) genderComboBox.getSelectedItem(),
                        customerAddressField.getText() +  ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                        doctorNameForPrint,
                        ultrasoundDoctorNameForPrint.toUpperCase(),
                        datePicker.getJFormattedTextField().getText(),
                        TextUtils.scaleRtfFontSize(getRtfContentAsString()),
                        conclusionField.getText() == null ? "" : conclusionField.getText(),
                        suggestionField.getText() == null ? "" : suggestionField.getText(),
                        diagnosisField.getText() == null ? "" : diagnosisField.getText(),
                        recheckupDatePicker.getJFormattedTextField().getText(),
                        selectedImagesForPrint,
                        (String) orientationComboBox.getSelectedItem(), // Use selected orientation
                        templateTitle,
                        customerHeightSpinner.getValue().toString(),
                        customerWeightSpinner.getValue().toString(),
                        patientHeartRateSpinner.getValue().toString(),
                        bloodPressureSystolicSpinner.getValue() + "/" + bloodPressureDiastolicSpinner.getValue(),
                        currentPatientDriveUrl // Google Drive URL for QR code
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
                // This will be turn on later
                // if ("ĐANG KHÁM".equals(statusComboBox.getSelectedItem())) {
                //     statusComboBox.setSelectedItem("ĐÃ KHÁM");
                // }
                String statusToSavePrint = (String) statusComboBox.getSelectedItem();
                handleSave();
                

                // Step 4: Finally, show print dialog with the pre-made object.
                try {
                    JasperPrintManager.printReport(jasperPrintToPrint, true); // true = show print dialog
                } catch (JRException e) {
                    log.error("Error showing print dialog", e);
                    JOptionPane.showMessageDialog(this, "Không thể hiển thị hộp thoại in: " + e.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
                }

                afterSaveActions(statusToSavePrint, "Đã lưu. Đang gửi lệnh in...");
                break;
            case "loupe": // This case now handles "Lưu & Xem"

                 if (photoNum != 0 && selectedImagesForPrint.size() > photoNum) {
                    JOptionPane.showMessageDialog(this,
                            "Số lượng ảnh đã chọn (" + selectedImagesForPrint.size() + ") nhiều hơn số lượng cho phép trong mẫu (" + photoNum + ").",
                            "Quá nhiều ảnh", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Step 2: Attempt to create the report object BEFORE saving anything.
                // Get current patient's Google Drive URL for QR code  
                Patient viewPatient = getCurrentSelectedPatient();
                String viewPatientDriveUrl = (viewPatient != null) ? viewPatient.getDriveUrl() : "";
                
                DoctorItem selectedDoctorForView = (DoctorItem) doctorComboBox.getSelectedItem();
                String doctorNameForView = (selectedDoctorForView != null) ? selectedDoctorForView.getName() : "";
                
                DoctorItem selectedUltrasoundDoctorForView = (DoctorItem) ultrasoundDoctorComboBox.getSelectedItem();
                String ultrasoundDoctorNameForView = (selectedUltrasoundDoctorForView != null) ? selectedUltrasoundDoctorForView.getName() : "";
                
                UltrasoundResult ultrasoundResultView = new UltrasoundResult(
                        checkupIdField.getText(),
                        customerLastNameField.getText() + " " + customerFirstNameField.getText(),
                        dobPicker.getJFormattedTextField().getText(),
                        (String) genderComboBox.getSelectedItem(),
                        customerAddressField.getText() + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                        doctorNameForView,
                        ultrasoundDoctorNameForView.toUpperCase(),
                        datePicker.getJFormattedTextField().getText(),
                        TextUtils.scaleRtfFontSize(getRtfContentAsString()),
                        conclusionField.getText() == null ? "" : conclusionField.getText(),
                        suggestionField.getText() == null ? "" : suggestionField.getText(),
                        diagnosisField.getText() == null ? "" : diagnosisField.getText(),
                        recheckupDatePicker.getJFormattedTextField().getText(),
                        selectedImagesForPrint,
                        (String) orientationComboBox.getSelectedItem(), // Use selected orientation
                        templateTitle,
                        customerHeightSpinner.getValue().toString(),
                        customerWeightSpinner.getValue().toString(),
                        patientHeartRateSpinner.getValue().toString(),
                        bloodPressureSystolicSpinner.getValue() + "/" + bloodPressureDiastolicSpinner.getValue(),
                        viewPatientDriveUrl // Google Drive URL for QR code
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
                

                // Step 4: Finally, show the viewer with the pre-made object.
                JasperViewer.viewReport(jasperPrintToView, false);

                afterSaveActions(statusToSaveView, "Đã lưu. Đang tạo bản xem trước...");
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
        // updateUpdateQueue();

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

    private void handleRowSelection(int selectedRowInQueue, Patient patient) {
        
        if ((selectedRowInQueue < 0 || selectedRowInQueue >= patientQueue.size()) && patient == null) {
            log.warn("Selected row index out of bounds: {}", selectedRowInQueue);
            return;
        }
        Patient selectedPatient;
        if (patient != null) {
            selectedPatient = patient;
        } else {
            selectedPatient = patientQueue.get(selectedRowInQueue);
        }

        
        // SAFETY: Clear previous patient IDs IMMEDIATELY when switching patients
        String previousPatientId = currentCheckupIdForMedia;
        log.info("=== SWITCHING PATIENT - SAFETY CLEARING ===");
        log.info("Previous patient ID: {}", previousPatientId);
        log.info("New patient ID: {}", selectedPatient.getCheckupId());
        
        // Clear immediately to prevent any ultrasound image mix-ups (atomically)
        synchronized (mediaStateLock) {
            currentCheckupIdForMedia = null;
            currentCheckupMediaPath = null;
        }
        this.selectedCheckupId = null;
        
        // Now set the new patient ID
        this.selectedCheckupId = selectedPatient.getCheckupId(); // Track selection by ID

        // Enable all action buttons when a patient is selected
        for (Component comp : ((JPanel)((JPanel)rightContainer.getComponent(1))).getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(true);
            }
        }
        driveButton.setEnabled(true); // <-- ADD THIS LINE

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
        
        synchronized (mediaStateLock) {
            currentCheckupIdForMedia = null;
            currentCheckupMediaPath = null;
        }
        if (imageGalleryPanel != null) {
            imageGalleryPanel.removeAll();
            JLabel loadingMsg = new JLabel("Đang tải hình ảnh (nếu có)...");
            loadingMsg.setFont(new Font("Arial", Font.ITALIC, 12));
            imageGalleryPanel.add(loadingMsg);
            imageGalleryPanel.revalidate();
            imageGalleryPanel.repaint();
        }
        
        // Enable webcam controls for the new patient
        if (takePictureButton != null) takePictureButton.setEnabled(true);
        if (recordVideoButton != null) recordVideoButton.setEnabled(true);
        if (webcamDeviceComboBox != null) webcamDeviceComboBox.setEnabled(true);
        if (openFolderButton != null) openFolderButton.setEnabled(true);
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
        
        // Check if re-checkup date is present and valid
        if (selectedPatient.getReCheckupDate() != null && !selectedPatient.getReCheckupDate().isEmpty() && 
            !selectedPatient.getReCheckupDate().equals("0") && !selectedPatient.getReCheckupDate().equals("null")) {
            needRecheckupCheckbox.setSelected(true);
            recheckupDatePicker.setEnabled(true);
            recheckupDatePicker.getJFormattedTextField().setEditable(true);
            if (recheckupDatePickerButton != null) {
                recheckupDatePickerButton.setEnabled(true);
            }
            // Parse and set the re-checkup date
            Date recheckupDate = DateUtils.convertToDate(selectedPatient.getReCheckupDate());
            if (recheckupDate != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(recheckupDate);
                recheckupDatePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                recheckupDatePicker.getModel().setSelected(true);
            } else {
                // If parsing fails, log it and keep the field clear
                log.warn("Could not parse re-checkup date: {}", selectedPatient.getReCheckupDate());
                recheckupDatePicker.getModel().setValue(null);
            }
        } else {
            needRecheckupCheckbox.setSelected(false);
            recheckupDatePicker.setEnabled(false);
            recheckupDatePicker.getJFormattedTextField().setEditable(false);
            if (recheckupDatePickerButton != null) {
                recheckupDatePickerButton.setEnabled(false);
            }
            recheckupDatePicker.getModel().setValue(null);
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

        // Select doctor by name
        log.info("Selected patient's doctor name: {}", selectedPatient.getDoctorName());
        DoctorItem doctorToSelect = findDoctorByName(selectedPatient.getDoctorName());
        if (doctorToSelect != null) {
            doctorComboBox.setSelectedItem(doctorToSelect);
        } else if (doctorComboBox.getItemCount() > 0) {
            doctorComboBox.setSelectedIndex(0);
            log.warn("Doctor '{}' not found in the list for patient selection.", selectedPatient.getDoctorName());
        }

        DoctorItem ultrasoundDoctorToSelect = findDoctorById(selectedPatient.getDoctorUltrasoundId());
        if (ultrasoundDoctorToSelect != null) {
            ultrasoundDoctorComboBox.setSelectedItem(ultrasoundDoctorToSelect);
        } else if (ultrasoundDoctorComboBox.getItemCount() > 0) {
            // if no ultrasound doctor by default it se to the same doctor as the main doctor
            ultrasoundDoctorComboBox.setSelectedItem(doctorToSelect);
            log.warn("Ultrasound Doctor '{}' not found in the list for patient selection.", selectedPatient.getDoctorUltrasoundId());
        }

        suggestionField.setText(selectedPatient.getSuggestion()); // De nghi
        diagnosisField.setText(selectedPatient.getDiagnosis()); // Chuan doan
        conclusionField.setText(selectedPatient.getConclusion()); // Ket luan
        if (selectedPatient.getReCheckupDate() != null) {
            recheckupDatePicker.getJFormattedTextField().setText(DateUtils.convertToDisplayFormat(selectedPatient.getReCheckupDate()));
        } else {
            recheckupDatePicker.getJFormattedTextField().setText("");
        }

        // Handle RTF notes content
        String notes = selectedPatient.getNotes();
        if (notes != null && !notes.isEmpty()) {
            if (isValidRtfContent(notes)) {
                // If it's valid RTF, load it directly
                setRtfContentFromString(notes);
            } else {
                // If it's plain text, convert to RTF
                StringBuilder rtfContent = new StringBuilder();
                rtfContent.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Arial;}}\n");
                rtfContent.append("{\\colortbl ;\\red0\\green0\\blue0;}\n");
                rtfContent.append("\\viewkind4\\uc1\\pard\\cf1\\f0\\fs32 ");
                rtfContent.append(notes.replace("\n", "\\par "));
                rtfContent.append("\\par}");
                setRtfContentFromString(rtfContent.toString());
            }
        } else {
            // Clear the notes field
            setRtfContentFromString("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Arial;}}\\viewkind4\\uc1\\pard\\f0\\fs32\\par}");
        }
        
        statusComboBox.setSelectedItem(selectedPatient.getStatus());
        customerIdField.setText(selectedPatient.getCustomerId());
        customerPhoneField.setText(selectedPatient.getCustomerNumber());
        try { customerWeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerWeight())); } catch (NumberFormatException e) {log.warn("Invalid weight: {}", selectedPatient.getCustomerWeight()); customerWeightSpinner.setValue(0.0);}
        try { customerHeightSpinner.setValue(Double.parseDouble(selectedPatient.getCustomerHeight())); } catch (NumberFormatException e) {log.warn("Invalid height: {}", selectedPatient.getCustomerHeight()); customerHeightSpinner.setValue(0.0);}
        try { patientHeartRateSpinner.setValue(Integer.parseInt(selectedPatient.getHeartBeat())); } catch (NumberFormatException e) {log.warn("Invalid heart beat: {}", selectedPatient.getHeartBeat()); patientHeartRateSpinner.setValue(0);}
        //blood pressure is format as string 0/0 so we will need to split it first
        String bloodPressure = selectedPatient.getBloodPressure();
        if (bloodPressure != null && bloodPressure.contains("/")) {
            String[] bloodPressureParts = bloodPressure.split("/");
            if (bloodPressureParts.length == 2) {
                try {
                    bloodPressureSystolicSpinner.setValue(Integer.parseInt(bloodPressureParts[0]));
                } catch (NumberFormatException e) {
                    log.warn("Invalid systolic blood pressure: {}", bloodPressureParts[0]);
                    bloodPressureSystolicSpinner.setValue(0);
                }
                try {
                    bloodPressureDiastolicSpinner.setValue(Integer.parseInt(bloodPressureParts[1]));
                } catch (NumberFormatException e) {
                    log.warn("Invalid diastolic blood pressure: {}", bloodPressureParts[1]);
                    bloodPressureDiastolicSpinner.setValue(0);
                }
            } else {
                 log.warn("Invalid blood pressure format: {}", bloodPressure);
                 bloodPressureSystolicSpinner.setValue(0);
                 bloodPressureDiastolicSpinner.setValue(0);
            }
        } else {
            log.warn("Blood pressure data is missing or in wrong format: {}", bloodPressure);
            bloodPressureSystolicSpinner.setValue(0);
            bloodPressureDiastolicSpinner.setValue(0);
        }
        genderComboBox.setSelectedItem(selectedPatient.getCustomerGender());
        checkupTypeComboBox.setSelectedItem(selectedPatient.getCheckupType());
        customerCccdDdcnField.setText(selectedPatient.getCccdDdcn() != null ? selectedPatient.getCccdDdcn() : "");


        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetPatientHistoryRequest(Integer.parseInt(selectedPatient.getCustomerId())));
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetOrderInfoByCheckupReq(selectedPatient.getCheckupId()));
        // Sync images from server to client for consistency
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new SyncCheckupImagesRequest(selectedPatient.getCheckupId()));
        String fullAddress = selectedPatient.getCustomerAddress();
        String[] addressParts = fullAddress.split(", ");
        
        // Reset target ward
        targetWard = null;
        
        if (addressParts.length >= 2) {
            // Last part is province, second last is ward, everything else is address
            String province = addressParts[addressParts.length - 1];
            targetWard = addressParts[addressParts.length - 2];
            log.info("Province: {}", province);
            log.info("Target ward: {}", targetWard);
            // Combine remaining parts as the detailed address
            StringBuilder detailedAddress = new StringBuilder();
            for (int i = 0; i < addressParts.length - 2; i++) {
                detailedAddress.append(addressParts[i]);
                if (i < addressParts.length - 3) {
                    detailedAddress.append(", ");
                }
            }
            
            customerAddressField.setText(detailedAddress.toString());
            
            int provinceIdx = findProvinceIndex(province);
            if (provinceIdx != -1) {
                provinceComboBox.setSelectedIndex(provinceIdx);
                // The province selection will trigger ward loading automatically
                // Ward will be set in the response handler using targetWard
            } else {
                log.warn("Province not found: {}", province);
                wardModel.removeAllElements(); wardModel.addElement("Xã/Phường"); wardComboBox.setEnabled(false);
                targetWard = null;
            }
        } else if (addressParts.length == 1) {
            // Handle case with only one part
            customerAddressField.setText(fullAddress);
            provinceComboBox.setSelectedIndex(0);
            wardModel.removeAllElements(); wardModel.addElement("Xã/Phường"); wardComboBox.setEnabled(false);
        } else {
            // Empty address case
            customerAddressField.setText("");
            provinceComboBox.setSelectedIndex(0);
            wardModel.removeAllElements(); wardModel.addElement("Xã/Phường"); wardComboBox.setEnabled(false);
        }

        // Setup for Supersonic View media for the NEWLY selected patient
        currentCheckupIdForMedia = selectedPatient.getCheckupId();
        if (selectedPatient.getCheckupId() != null && !selectedPatient.getCheckupId().trim().isEmpty()) {
            String newId = selectedPatient.getCheckupId().trim();
            ensureMediaDirectoryExists(newId);
            Path newPath = Paths.get(LocalStorage.checkupMediaBaseDir, newId);
            synchronized (mediaStateLock) {
                currentCheckupIdForMedia = newId;
                currentCheckupMediaPath = newPath;
            }
            
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
                if (openFolderButton != null) openFolderButton.setEnabled(false);
                if (webcamDeviceComboBox != null) webcamDeviceComboBox.setEnabled(false);
            }
        } else {
            log.warn("No Checkup ID available (or it is empty) for media operations for selected patient (Customer ID: {}). Disabling media features.", selectedPatient.getCustomerId());
            if (takePictureButton != null) takePictureButton.setEnabled(false);
            if (recordVideoButton != null) recordVideoButton.setEnabled(false);
            if (openFolderButton != null) openFolderButton.setEnabled(false);
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

    private void handleGetWardResponse(GetWardResponse response) {
        SwingUtilities.invokeLater(() -> {
        log.info("Received ward data");
        LocalStorage.wards = response.getWards();
        wardModel.removeAllElements(); 
        for (String ward : LocalStorage.wards) { wardModel.addElement(ward); }
        wardComboBox.setEnabled(true); 
        
        // If we have a target ward to set, try to set it now
        if (targetWard != null) {
            int wardIdx = findWardIndex(targetWard);
            if (wardIdx != -1) {
                log.info("Setting target ward: {}", targetWard);
                wardComboBox.setSelectedIndex(wardIdx);
                // The ward selection will trigger ward loading automatically
            } else {
                log.warn("Target ward not found in loaded data: {}", targetWard);
                targetWard = null;
            }
        }
        });
    }


    private void handleGetPatientHistoryResponse(GetPatientHistoryResponse response) {
        SwingUtilities.invokeLater(() -> {
            log.info("Received patient history");
            this.history = response.getHistory(); // Keep original data for the dialog

            if (this.history != null && this.history.length > 0) {
                // Prepare data specifically for the 7-column table view
                String[] historyColumns = {"Ngày khám", "Loại khám", "Kết luận", "Cao (cm)", "Nặng (kg)", "Nhịp tim", "Huyết áp"};
                String[][] tableData = new String[this.history.length][7];

                for (int i = 0; i < this.history.length; i++) {
                    if (this.history[i] != null && this.history[i].length >= 15) {
                        // 0: checkup_date, 1: checkup_id, 2: suggestion, 3: diagnosis, 4: prescription_id, 5: notes, 6: checkup_type, 
                        // 7: conclusion, 8: reCheckupDate, 9: doctor_last_name, 10: doctor_first_name, 11: customer_height, 12: customer_weight, 
                        // 13: heart_beat, 14: blood_pressure
                        tableData[i][0] = this.history[i][0];  // Ngày khám
                        tableData[i][1] = this.history[i][6];  // Loại khám
                        tableData[i][2] = this.history[i][7];  // Kết luận
                        tableData[i][3] = this.history[i][11]; // Cao (cm)
                        tableData[i][4] = this.history[i][12]; // Nặng (kg)
                        tableData[i][5] = this.history[i][13]; // Nhịp tim
                        tableData[i][6] = this.history[i][14]; // Huyết áp
                    }
                }
                historyModel.setDataVector(tableData, historyColumns);
            } else {
                // Clear the table if there is no history
                historyModel.setRowCount(0);
            }
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
        log.info("Received call patient response: Room {}, Patient ID {}, Queue Number {}, Status {}", 
               response.getRoomId(), response.getPatientId(), response.getQueueNumber(), response.getStatus());
        if (tvQueueFrame != null && tvQueueFrame.isVisible()) {
            if (response.getStatus() == Status.PROCESSING) {
                String patientIdToFind = String.valueOf(response.getPatientId());
                String queueNumber = response.getQueueNumber();
                String patientDisplayInfo = queueNumber; // Use queue number instead of patient ID

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
                tvQueueFrame.updateSpecificRoomStatus(response.getRoomId(), String.valueOf(response.getPatientId()), queueNumber, patientDisplayInfo, response.getStatus());
            } else if (response.getStatus() == Status.EMPTY) {
                tvQueueFrame.markRoomAsFree(response.getRoomId());
            }
        }
        });

        if (response.getRoomId() == 1 && response.getStatus() == Status.PROCESSING) {
            styleRoomStatusPanel(room1StatusPanel, room1StatusLabel, Color.RED.darker(), "STT: " + response.getQueueNumber());
        } else if (response.getRoomId() == 2 && response.getStatus() == Status.PROCESSING) {
            styleRoomStatusPanel(room2StatusPanel, room2StatusLabel, Color.RED.darker(), "STT: " + response.getQueueNumber());
        } else if (response.getRoomId() == 1 && response.getStatus() == Status.EMPTY) {
            styleRoomStatusPanel(room1StatusPanel, room1StatusLabel, Color.GREEN.darker(), "TRỐNG");
        } else if (response.getRoomId() == 2 && response.getStatus() == Status.EMPTY) {
            styleRoomStatusPanel(room2StatusPanel, room2StatusLabel, Color.GREEN.darker(), "TRỐNG");
        }
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
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new CallPatientRequest(roomId, -1, "", Status.EMPTY));
    }

    private void handleCallPatient() {
        if (selectedCheckupId == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân từ danh sách chờ.", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Patient selectedPatient = null;
        for (Patient patient : patientQueue) {
            if (selectedCheckupId.equals(patient.getCheckupId())) {
                selectedPatient = patient;
                break;
            }
        }

        if (selectedPatient == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy thông tin chi tiết cho bệnh nhân đã chọn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int roomId = callRoomComboBox.getSelectedIndex() + 1;
        int patientId = Integer.parseInt(selectedCheckupId);
        
        // Get queue number from patient DTO and format it to 2 digits with leading zero
        String queueNumber = selectedPatient.getQueueNumber();

        // Set status to PROCESSING when calling
        statusComboBox.setSelectedItem("ĐANG KHÁM");
        Status status = Status.PROCESSING;

        callingStatusLabel.setText("Đang gọi bệnh nhân " + selectedPatient.getCustomerFirstName() + " (STT: " + queueNumber + ") vào phòng " + roomId);
        callingStatusLabel.setForeground(Color.BLUE);
        callingStatusLabel.setBackground(new Color(220, 230, 255));
        
        CallPatientRequest request = new CallPatientRequest(roomId, patientId, queueNumber, status);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
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
            imageRefreshTimer = new javax.swing.Timer(2000, e -> { // Refresh every 2 seconds
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
        // Remove inner title to avoid duplicate "Thư viện Hình ảnh" header and save vertical space
        panel.setBorder(BorderFactory.createEmptyBorder());

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

        // Initialize with placeholder - no automatic device search
        String[] initialWebcamNames = {"Chọn thiết bị..."};

        // Panel for device selection
        JPanel devicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        devicePanel.setOpaque(false);
        devicePanel.add(new JLabel("Thiết bị:"));
        
        webcamDeviceComboBox = new JComboBox<>(initialWebcamNames);
        webcamDeviceComboBox.setPreferredSize(new Dimension(150, 25));
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
        
        // Add refresh button with reload icon
        ImageIcon reloadIcon = getReloadIcon();
        if (reloadIcon != null) {
            webcamRefreshButton = new JButton(reloadIcon);
        } else {
            // Fallback to emoji if icon fails to load
            webcamRefreshButton = new JButton("🔄");
            log.warn("Failed to load reload icon, using emoji fallback");
        }
        webcamRefreshButton.setPreferredSize(new Dimension(30, 25));
        webcamRefreshButton.setToolTipText("Tìm kiếm thiết bị webcam (thay vì tự động tìm)");
        webcamRefreshButton.setFocusPainted(false);
        webcamRefreshButton.addActionListener(e -> refreshWebcamDevices());
        devicePanel.add(webcamRefreshButton);

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
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        buttonPanel.setOpaque(false);
        
        takePictureButton = new JButton("<html>Chụp ảnh <font color='red'><b>(F5)</b></font></html>");
        takePictureButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/camera.png"));
        takePictureButton.addActionListener(e -> handleTakePicture());
        takePictureButton.setEnabled(false);
        
        recordVideoButton = new JButton("<html>Quay video <font color='red'><b>(F6)</b></font></html>");
        recordVideoButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/video-camera.png"));
        recordVideoButton.addActionListener(e -> handleRecordVideo());
        recordVideoButton.setEnabled(false);

        openFolderButton = new JButton("Mở thư mục ảnh");
        openFolderButton.setIcon(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/folder.png"));
        openFolderButton.setEnabled(false); // Initially disabled
        openFolderButton.addActionListener(e -> {
            if (currentCheckupMediaPath != null && Files.exists(currentCheckupMediaPath)) {
                try {
                    // This opens the folder in the system's file explorer
                    Desktop.getDesktop().open(currentCheckupMediaPath.toFile());
                } catch (IOException ex) {
                    log.error("Failed to open media folder: {}", currentCheckupMediaPath, ex);
                    JOptionPane.showMessageDialog(this,
                        "Không thể mở thư mục: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Không có thư mục media cho bệnh nhân này hoặc chưa chọn bệnh nhân.",
                    "Thông báo",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        buttonPanel.add(takePictureButton);
        buttonPanel.add(recordVideoButton);
        buttonPanel.add(openFolderButton);

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
    
    // Helper method to load and scale the reload icon
    private ImageIcon getReloadIcon() {
        try {
            ImageIcon reloadIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/reload.png");
            Image scaledImage = reloadIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } catch (Exception e) {
            log.debug("Could not load reload icon: {}", e.getMessage());
            return null;
        }
    }
    
    // Manual webcam device refresh
    private void refreshWebcamDevices() {
        log.info("Manual webcam device refresh requested");
        
        // Disable button and show searching state
        webcamRefreshButton.setEnabled(false);
        webcamRefreshButton.setText("⏳");
        webcamRefreshButton.setIcon(null); // Remove icon during search
        webcamDeviceComboBox.removeAllItems();
        webcamDeviceComboBox.addItem("Đang tìm thiết bị...");
        
        // Search for devices in background thread
        cleanupExecutor.submit(() -> {
            try {
                log.info("Searching for webcam devices...");
                
                // Manually trigger device discovery without continuous scanning
                final List<Webcam> webcams;
                try {
                    // Get devices list (this may start discovery service temporarily)
                    webcams = Webcam.getWebcams();
                    log.info("Found {} webcam devices", webcams.size());
                    
                    // Stop the discovery service immediately after getting the list
                    // to prevent continuous background scanning
                    try {
                        Webcam.getDiscoveryService().stop();
                        log.info("Stopped webcam discovery service to prevent background scanning");
                    } catch (Exception stopEx) {
                        log.debug("Discovery service stop during refresh (non-critical): {}", stopEx.getMessage());
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to get webcam list: {}", e.getMessage());
                    throw e; // Re-throw to be caught by outer catch block
                }
                
                SwingUtilities.invokeLater(() -> {
                    // Update combobox with found devices
                    webcamDeviceComboBox.removeAllItems();
                    webcamDeviceComboBox.addItem("Chọn thiết bị...");
                    
                    for (Webcam webcam : webcams) {
                        webcamDeviceComboBox.addItem(webcam.getName());
                        log.info("Added webcam: {}", webcam.getName());
                    }
                    
                    // Restore button with icon
                    webcamRefreshButton.setEnabled(true);
                    ImageIcon reloadIcon = getReloadIcon();
                    if (reloadIcon != null) {
                        webcamRefreshButton.setText("");
                        webcamRefreshButton.setIcon(reloadIcon);
                    } else {
                        webcamRefreshButton.setText("🔄"); // Fallback to emoji
                        webcamRefreshButton.setIcon(null);
                        log.warn("Failed to restore reload icon, using emoji");
                    }
                    
                    if (webcams.isEmpty()) {
                        log.warn("No webcam devices found");
                    } else {
                        log.info("Webcam device refresh completed successfully");
                    }
                });
                
            } catch (Exception e) {
                log.error("Error refreshing webcam devices: {}", e.getMessage(), e);
                
                SwingUtilities.invokeLater(() -> {
                    webcamDeviceComboBox.removeAllItems();
                    webcamDeviceComboBox.addItem("Lỗi tìm thiết bị");
                    webcamRefreshButton.setEnabled(true);
                    // Restore reload icon on error
                    ImageIcon reloadIcon = getReloadIcon();
                    if (reloadIcon != null) {
                        webcamRefreshButton.setText("");
                        webcamRefreshButton.setIcon(reloadIcon);
                    } else {
                        webcamRefreshButton.setText("🔄"); // Fallback to emoji
                        webcamRefreshButton.setIcon(null);
                        log.warn("Failed to restore reload icon after error, using emoji");
                    }
                });
            }
        });
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
        
        // Note: We don't stop the ultrasound folder watcher here since it should 
        // continue running even when switching pages. It will only be stopped
        // during fullCleanup() when the application is closing.
        
        // Don't stop webcam discovery service here - let it run for other instances
    }

    // Full cleanup only when application is closing
    public void fullCleanup() {
        fastCleanup();
        
        // Stop ultrasound folder watcher
        stopUltrasoundFolderScanner();
        
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
    
    // === Ultrasound Folder Monitoring Methods ===
    // --- NEW METHOD: Replaces initializeUltrasoundFolderWatcher() ---
    private void initializeUltrasoundFolderScanner() {
        try {
            // Create the ultrasound folder if it doesn't exist
            Path ultrasoundPath = Paths.get(ULTRASOUND_FOLDER_PATH);
            log.info("Initializing ultrasound folder scanner for path: {}", ultrasoundPath.toAbsolutePath());
            
            if (!Files.exists(ultrasoundPath)) {
                Files.createDirectories(ultrasoundPath);
                log.info("Created ultrasound folder: {}", ultrasoundPath.toAbsolutePath());
            }
            
            // Clear tracking maps
            processingFiles.clear();
            lastSeenFiles.clear();
            
            // Start scanning in a scheduled thread
            folderScanExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "UltrasoundScanner");
                t.setDaemon(true);
                return t;
            });
            isScanning = true;
            
            folderScanExecutor.scheduleWithFixedDelay(
                this::scanUltrasoundFolders, 
                0, // Initial delay
                SCAN_INTERVAL_MILLISECONDS, 
                TimeUnit.MILLISECONDS
            );
            
            log.info("Started ultrasound folder scanner with {} milliseconds intervals", SCAN_INTERVAL_MILLISECONDS);
            
        } catch (Exception e) {
            log.error("Failed to initialize ultrasound folder scanner: {}", e.getMessage(), e);
        }
    }
    
    private void scanUltrasoundFolders() {
        if (!isScanning) {
            return;
        }
        
        // Capture current patient ID once to avoid changes during scan
        final String currentPatientId = currentCheckupIdForMedia;
        
        try {
            Path ultrasoundPath = Paths.get(ULTRASOUND_FOLDER_PATH);
            
            if (!Files.exists(ultrasoundPath)) {
                return;
            }
            
            // Clean up tracking for files that no longer exist
            cleanupStaleFileTracking(ultrasoundPath);
            
            // LEVEL 0: Check for images directly in the root "ANH SIEU AM" folder
            scanDirectoryForImages(ultrasoundPath, currentPatientId);
            
            // Get all first-level directories and sort by creation time (newest first)
            List<Path> firstLevelFolders = getFirstLevelFoldersSortedByDate(ultrasoundPath);
            
            // Take only the latest N folders for performance
            List<Path> foldersToScan = firstLevelFolders.stream()
                .limit(MAX_FOLDERS_TO_SCAN)
                .collect(Collectors.toList());
        
            // LEVEL 1: Check for images directly in each of the newest first-level folders
            for (Path folder : foldersToScan) {
                if (!isScanning) break;
                scanDirectoryForImages(folder, currentPatientId);
            }
            
            // LEVEL 2+: Scan each of the newest folders deeply for images in subdirectories
            for (Path folder : foldersToScan) {
                if (!isScanning) break;
                scanSubdirectoriesForImages(folder, currentPatientId);
            }
            
        } catch (Exception e) {
            log.error("Error during ultrasound folder scan: {}", e.getMessage(), e);
        }
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String lowercaseName = fileName.toLowerCase();
        return lowercaseName.endsWith(".jpg") || 
               lowercaseName.endsWith(".jpeg") || 
               lowercaseName.endsWith(".png") || 
               lowercaseName.endsWith(".bmp") || 
               lowercaseName.endsWith(".tiff") || 
               lowercaseName.endsWith(".tif");
    }

    private List<Path> getFirstLevelFoldersSortedByDate(Path parentPath) {
        List<Path> folders = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentPath, Files::isDirectory)) {
            for (Path folder : stream) {
                folders.add(folder);
            }
        } catch (Exception e) {
            log.error("Error listing first-level folders in {}: {}", parentPath, e.getMessage(), e);
            return Collections.emptyList();
        }
        
        // Sort by last modified time (newest first)
        folders.sort((path1, path2) -> {
            try {
                FileTime time1 = Files.getLastModifiedTime(path1);
                FileTime time2 = Files.getLastModifiedTime(path2);
                return time2.compareTo(time1);
            } catch (Exception e) {
                log.warn("Error comparing last modified times for {} and {}: {}", path1, path2, e.getMessage());
                return 0;
            }
        });
        
        return folders;
    }
    
    private void cleanupStaleFileTracking(Path basePath) {
        lastSeenFiles.entrySet().removeIf(entry -> {
            Path filePath = entry.getKey();
            try {
                return !Files.exists(filePath) || !filePath.startsWith(basePath);
            } catch (Exception e) {
                return true; // Remove on error
            }
        });
        
        processingFiles.removeIf(filePath -> {
            try {
                return !Files.exists(filePath) || !filePath.startsWith(basePath);
            } catch (Exception e) {
                return true; // Remove on error
            }
        });
    }
    
    private void scanDirectoryForImages(Path directory, String currentPatientId) {
        if (!Files.exists(directory) || !Files.isDirectory(directory) || !isScanning) {
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (!isScanning) break;
                
                // Only check files, not subdirectories in this method
                if (Files.isRegularFile(entry) && isImageFile(entry.getFileName().toString())) {
                    processImageFileCandidate(entry, currentPatientId);
                }
            }
        } catch (Exception e) {
            log.error("Error scanning directory for images {}: {}", directory, e.getMessage(), e);
        }
    }
    
    private void scanSubdirectoriesForImages(Path directory, String currentPatientId) {
        if (!Files.exists(directory) || !Files.isDirectory(directory) || !isScanning) {
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (!isScanning) break;
                
                // Only process subdirectories, skip files (already handled by scanDirectoryForImages)
                if (Files.isDirectory(entry)) {
                    scanFolderRecursively(entry, currentPatientId);
                }
            }
        } catch (Exception e) {
            log.error("Error scanning subdirectories {}: {}", directory, e.getMessage(), e);
        }
    }
    
    private void scanFolderRecursively(Path directory, String currentPatientId) {
        if (!Files.exists(directory) || !Files.isDirectory(directory) || !isScanning) {
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (!isScanning) break;
                
                if (Files.isDirectory(entry)) {
                    // Recursively scan subdirectories
                    scanFolderRecursively(entry, currentPatientId);
                } else if (Files.isRegularFile(entry) && isImageFile(entry.getFileName().toString())) {
                    // Found an image file
                    processImageFileCandidate(entry, currentPatientId);
                }
            }
        } catch (Exception e) {
            log.error("Error scanning directory {}: {}", directory, e.getMessage(), e);
        }
    }
    
    private void processImageFileCandidate(Path imagePath, String currentPatientId) {
        // Skip if already being processed
        if (processingFiles.contains(imagePath)) {
            return;
        }
        
        try {
            // Check if file exists and get its attributes atomically
            BasicFileAttributes attrs = Files.readAttributes(imagePath, BasicFileAttributes.class);
            FileTime lastModified = attrs.lastModifiedTime();
            long fileSize = attrs.size();
            
            // Skip empty files or files that are likely still being written
            if (fileSize == 0) {
                log.debug("Skipping empty file: {}", imagePath);
                return;
            }
            
            FileAttributes lastSeen = lastSeenFiles.get(imagePath);
            
            if (lastSeen == null) {
                // First time seeing this file - track it but don't process yet
                lastSeenFiles.put(imagePath, new FileAttributes(fileSize, lastModified, false));
                log.debug("First time seeing file, tracking: {}", imagePath);
                return;
            }
            
            // Check if file has stabilized (no changes since last scan)
            boolean hasChanged = lastSeen.size != fileSize || !lastSeen.lastModified.equals(lastModified);
            
            if (hasChanged) {
                // File is still changing - update tracking
                lastSeenFiles.put(imagePath, new FileAttributes(fileSize, lastModified, false));
                log.debug("File still changing, updating tracking: {}", imagePath);
                return;
            }
            
            if (!lastSeen.isStable) {
                // File hasn't changed this cycle - mark as stable
                lastSeenFiles.put(imagePath, new FileAttributes(fileSize, lastModified, true));
                log.debug("File stabilized, ready for processing: {}", imagePath);
            }
            
            // File is stable and ready for processing
            if (lastSeen.isStable) {
                processStableImageFile(imagePath, currentPatientId);
            }
            
        } catch (NoSuchFileException e) {
            // File was deleted between directory scan and attribute read
            lastSeenFiles.remove(imagePath);
            log.debug("File disappeared during scan: {}", imagePath);
        } catch (Exception e) {
            log.error("Error checking file attributes for {}: {}", imagePath, e.getMessage(), e);
        }
    }
    
    private void processStableImageFile(Path imagePath, String currentPatientId) {
        // Double-check the file still exists and add to processing set atomically
        if (!processingFiles.add(imagePath)) {
            return; // Already being processed
        }
        
        try {
            // Final existence and patient validation
            if (!Files.exists(imagePath)) {
                log.debug("File no longer exists when ready to process: {}", imagePath);
                return;
            }
            
            // Use the captured patient ID to avoid race conditions
            handleUltrasoundImageDetected(imagePath, currentPatientId);
            
            // Remove from tracking since it's been processed
            lastSeenFiles.remove(imagePath);
            
        } finally {
            // Always remove from processing set
            processingFiles.remove(imagePath);
        }
    }
    
    // Modified to accept patient ID parameter to avoid race conditions
    private void handleUltrasoundImageDetected(Path imagePath, String patientId) {
        log.info("=== HANDLING ULTRASOUND IMAGE: {} ===", imagePath.toAbsolutePath());
        log.info("Patient ID for this operation: {}", patientId);
        log.info("Current media path: {}", currentCheckupMediaPath);
        
        // Snapshot the current media state atomically to avoid races
        final String capturedPatientId;
        final Path capturedMediaPath;
        synchronized (mediaStateLock) {
            capturedPatientId = (patientId == null) ? currentCheckupIdForMedia : patientId;
            capturedMediaPath = currentCheckupMediaPath;
        }
        if (capturedPatientId == null || capturedPatientId.trim().isEmpty() || capturedMediaPath == null) {
            log.warn("Media state not ready (patientId or mediaPath null). Skipping: {}", imagePath);
            return;
        }
        
        try {
            // Final safety check - ensure file still exists before processing
            if (!Files.exists(imagePath)) {
                log.warn("Ultrasound image file no longer exists: {}", imagePath);
                return;
            }
            
            // Additional safety: check current state matches our captured state
            if (!capturedPatientId.equals(currentCheckupIdForMedia)) {
                log.warn("Patient changed during processing. Captured: {}, Current: {}. Skipping.", 
                    capturedPatientId, currentCheckupIdForMedia);
                return;
            }
            
            if (capturedMediaPath == null) {
                log.error("SAFETY ERROR: Media path is null during processing! Skipping.");
                return;
            }
            
            String originalFileName = imagePath.getFileName().toString();
            String fileExtension = "";
            int lastDotIndex = originalFileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                fileExtension = originalFileName.substring(lastDotIndex);
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String newFileName = "ultrasound_" + capturedPatientId + "_" + timestamp + fileExtension;
            
            Path targetPath = capturedMediaPath.resolve(newFileName);
            
            // Robust move with retries/fallback
            safeMoveWithRetry(imagePath, targetPath);
            
            log.info("Moved ultrasound image from {} to {}", imagePath, targetPath);
            
            // Offload image decoding & upload
            imageUploadExecutor.submit(() -> {
                try {
                    BufferedImage image = ImageIO.read(targetPath.toFile());
                    if (image != null) {
                        uploadImageInBackground(newFileName, image);
                        log.info("Started uploading ultrasound image to server: {}", newFileName);
                    } else {
                        log.warn("Could not read ultrasound image for upload: {}", targetPath);
                    }
                } catch (Exception uploadEx) {
                    log.error("Error reading ultrasound image for upload: {}", uploadEx.getMessage(), uploadEx);
                }
            });
            
            SwingUtilities.invokeLater(() -> {
                // Use the current state for UI updates, but log the operation with captured state
                if (capturedMediaPath != null && Files.exists(capturedMediaPath)) {
                    loadAndDisplayImages(capturedMediaPath);
                }
                log.info("✅ SUCCESS: Ultrasound image processed for patient {} - File: {}", capturedPatientId, newFileName);
            });
            
        } catch (Exception e) {
            log.error("Error handling ultrasound image: {}", e.getMessage(), e);
        }
    }
    
    private void stopUltrasoundFolderScanner() {
        log.info("Stopping ultrasound folder scanner...");
        isScanning = false;
        
        if (folderScanExecutor != null) {
            folderScanExecutor.shutdown();
            try {
                if (!folderScanExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    folderScanExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                folderScanExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Clear tracking data
        processingFiles.clear();
        lastSeenFiles.clear();
        
        log.info("Ultrasound folder scanner stopped");
    }

    // Robust move with retry to avoid transient AccessDenied on Windows
    private void safeMoveWithRetry(Path source, Path target) throws IOException {
        int attempts = 0;
        while (true) {
            try {
                try {
                    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (java.nio.file.FileSystemException e) {
                attempts++;
                if (attempts > 6) {
                    throw e;
                }
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during move retry", ie);
                }
            }
        }
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

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String fileName = "IMG_" + currentCheckupIdForMedia + "_" + timestamp + ".jpg";
            Path filePath = currentCheckupMediaPath.resolve(fileName);

            try {
                BufferedImage image = selectedWebcam.getImage();
                if (image != null) {
                    // Save locally as JPG for consistency (compressed format for client storage)
                    ImageIO.write(image, "JPG", filePath.toFile());
                    log.info("Picture taken and saved locally as JPG at: {}", filePath);

                    // Refresh UI to show the new image immediately
                    loadAndDisplayImages(currentCheckupMediaPath);

                    // Now, upload in the background (server will convert to PNG for archival)
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
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                // CHANGE 1: Write the image as a JPG instead of PNG.
                // This creates compressed JPG data, which is smaller and faster to upload.
                ImageIO.write(image, "jpg", baos);
                
                byte[] imageData = baos.toByteArray();
                baos.close();
    
                String checkupId = currentCheckupIdForMedia;
    
                // CHANGE 2: Use the original fileName (which should already be .jpg).
                // No need to rename it to .png anymore.
                UploadCheckupImageRequest request = new UploadCheckupImageRequest(checkupId, imageData, fileName);
                
                NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
                log.info("Sent UploadCheckupImageRequest for {}", fileName);
    
            } catch (IOException e) {
                log.error("Failed to convert image to JPG for upload: {}", fileName, e);
            }
        });
    }

    private void deleteImage(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            log.warn("Cannot delete image - file does not exist or is null");
            return;
        }

        String fileName = imageFile.getName();
        String checkupId = currentCheckupIdForMedia;

        if (checkupId == null || checkupId.trim().isEmpty()) {
            log.error("Cannot delete image - no checkup ID available");
            JOptionPane.showMessageDialog(this,
                    "Không thể xóa ảnh - không có mã khám.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Send delete request to server
        DeleteCheckupImageRequest request = new DeleteCheckupImageRequest(checkupId, fileName);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
        log.info("Sent DeleteCheckupImageRequest for file: {} in checkup: {}", fileName, checkupId);

        // Delete local file immediately
        try {
            if (imageFile.delete()) {
                log.info("Successfully deleted local image file: {}", fileName);
                // Refresh the gallery
                if (currentCheckupMediaPath != null && Files.exists(currentCheckupMediaPath)) {
                    loadAndDisplayImages(currentCheckupMediaPath);
                }
            } else {
                log.warn("Failed to delete local image file: {}", fileName);
            }
        } catch (Exception e) {
            log.error("Error deleting local image file: {}", fileName, e);
        }
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

    private void handleDeleteImageResponse(DeleteCheckupImageResponse response) {
        String fileName = response.getFileName();
        
        if (response.isSuccess()) {
            log.info("Successfully deleted image from server: {} - {}", fileName, response.getMessage());
            SwingUtilities.invokeLater(() -> {
                // Image already deleted locally, just log success
                if (callingStatusLabel != null) {
                    callingStatusLabel.setText("Đã xóa ảnh: " + fileName);
                    callingStatusLabel.setForeground(new Color(0, 150, 0)); // Green color
                }
            });
        } else {
            log.error("Failed to delete image from server: {} - {}", fileName, response.getMessage());
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi xóa ảnh trên server: " + response.getMessage(),
                        "Lỗi Xóa Ảnh",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void handleUploadPdfResponse(UploadCheckupPdfResponse response) {
        if (response.isSuccess()) {
            log.info("Successfully uploaded PDF: {} - {}", response.getPdfType(), response.getMessage());
            SwingUtilities.invokeLater(() -> {
                callingStatusLabel.setText("Đã lưu PDF thành công: " + response.getPdfType());
                callingStatusLabel.setForeground(new Color(0, 100, 0));
            });
        } else {
            log.error("Failed to upload PDF: {} - {}", response.getPdfType(), response.getMessage());
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "Lỗi khi tải PDF lên server: " + response.getMessage(),
                        "Lỗi Tải PDF",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void handleSyncImagesResponse(SyncCheckupImagesResponse response) {
        if (response.isSuccess()) {
            String checkupId = response.getCheckupId();
            int imageCount = response.getImageNames() != null ? response.getImageNames().size() : 0;
            log.info("Received image manifest for checkup {}. Found {} images. Requesting data individually.", checkupId, imageCount);
    
            SwingUtilities.invokeLater(() -> {
                try {
                    // Step 1: Create the local directory for this checkup
                    Path mediaDir = Paths.get(LocalStorage.checkupMediaBaseDir, checkupId);
                    if (!Files.exists(mediaDir)) {
                        Files.createDirectories(mediaDir);
                    }
                    
                    // Step 2 (Optional but Recommended): Clear the local directory to ensure it perfectly matches the server.
                    // This prevents images deleted on the server from lingering on the client.
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(mediaDir)) {
                        for (Path entry : stream) {
                            Files.delete(entry);
                        }
                    }
                    log.info("Cleared local media directory for checkup {}", checkupId);
    
                    // Step 3: Request each image from the server one by one
                    if (imageCount > 0) {
                        for (String imageName : response.getImageNames()) {
                            log.info("Requesting image data for: {}", imageName);
                            GetCheckupImageRequest request = new GetCheckupImageRequest(checkupId, imageName);
                            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
                        }
                    } else {
                        // If there are no images on the server, ensure the gallery is empty
                        if (checkupId.equals(currentCheckupIdForMedia)) {
                            loadAndDisplayImages(mediaDir);
                        }
                    }
                    
                    // Update UI status
                    if (callingStatusLabel != null) {
                        callingStatusLabel.setText("Đang tải " + imageCount + " hình ảnh từ server...");
                        callingStatusLabel.setForeground(new Color(0, 100, 0));
                    }
    
                } catch (Exception e) {
                    log.error("Failed to process synced image manifest: {}", e.getMessage());
                    if (callingStatusLabel != null) {
                        callingStatusLabel.setText("Lỗi khi xử lý danh sách hình ảnh: " + e.getMessage());
                        callingStatusLabel.setForeground(Color.RED);
                    }
                }
            });
        } else {
            log.warn("Image sync failed for checkup {}: {}", response.getCheckupId(), response.getMessage());
            SwingUtilities.invokeLater(() -> {
                if (callingStatusLabel != null) {
                    callingStatusLabel.setText("Đồng bộ hình ảnh: " + response.getMessage());
                    callingStatusLabel.setForeground(new Color(200, 100, 0)); // Orange for warning
                }
            });
        }
    }
    
    
    // ----------------- NEW METHOD -----------------
    // ADD this new method to your CheckUpPage class.
    private void handleGetCheckupImageResponse(GetCheckupImageResponse response) {
        if (response.isSuccess() && response.getImageData() != null) {
            log.info("Received image data for {} from checkup {}", response.getImageName(), response.getCheckupId());
    
            SwingUtilities.invokeLater(() -> {
                try {
                    Path mediaDir = Paths.get(LocalStorage.checkupMediaBaseDir, response.getCheckupId());
                    if (!Files.exists(mediaDir)) {
                        Files.createDirectories(mediaDir);
                    }
    
                    // For client-side consistency, we'll save everything as a JPG.
                    String jpgImageName = response.getImageName().replaceAll("\\.\\w+$", ".jpg");
                    java.io.File imageFile = new java.io.File(mediaDir.toFile(), jpgImageName);
    
                    // Try to read the byte array as a BufferedImage and write it as a JPG.
                    // This handles conversion from PNG, BMP, etc.
                    try (java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(response.getImageData())) {
                        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bais);
                        if (image != null) {
                            javax.imageio.ImageIO.write(image, "JPG", imageFile);
                        } else {
                            throw new IOException("ImageIO.read returned null; could not decode image format.");
                        }
                    }
    
                    log.debug("Saved synced image as JPG: {}", imageFile.getAbsolutePath());
    
                    // If this image belongs to the currently selected patient, refresh the gallery to show it.
                    if (response.getCheckupId().equals(currentCheckupIdForMedia)) {
                        loadAndDisplayImages(currentCheckupMediaPath);
                    }
    
                } catch (IOException e) {
                    log.error("Failed to save synced image {} locally: {}", response.getImageName(), e.getMessage());
                    // As a fallback, you could write the raw bytes if conversion fails, but logging the error is often enough.
                }
            });
        } else {
            log.error("Failed to get image {} for checkup {}: {}", response.getImageName(), response.getCheckupId(), response.getMessage());
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
                    // If webcam doesn't report FPS or reports an invalid value, default to a base value like 15
                    if (webcamFPS <= 0 || Double.isNaN(webcamFPS)) {
                        webcamFPS = 15.0;
                    }
                    
                    // FIX FOR TIMING ISSUE: We double the reported FPS to match the actual frame delivery speed.
                    final double recorderFrameRate = webcamFPS * 2.0;
                
                    // The capture loop will now also target this corrected frame rate to avoid missing frames.
                    final double captureRate = recorderFrameRate;
                
                    log.info("Setting recorder frame rate to: {} (Corrected from webcam's reported FPS of {})", String.format("%.2f", recorderFrameRate), String.format("%.2f", webcamFPS));
                
                    // Initialize the recorder with optimized settings
                    try {
                        Dimension size = selectedWebcam.getViewSize();
                        recorder = new FFmpegFrameRecorder(filePath.toString(), size.width, size.height);
                        
                        // Video format and codec settings
                        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
                        recorder.setFormat("mp4");
                        
                        // Set the corrected frame rate
                        recorder.setFrameRate(recorderFrameRate); 
                        
                        // Pixel format
                        recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
                        
                        // --- QUALITY SETTINGS ---
                        // Using CRF is the best way to control quality for H.264. 
                        // The setVideoQuality(0) call is removed to avoid conflicts.
                        recorder.setVideoOption("crf", "18"); // High quality
                        recorder.setVideoOption("preset", "slow"); // Better compression
                        recorder.setVideoOption("tune", "zerolatency");
                        
                        // Buffer size settings
                        recorder.setVideoOption("bufsize", "20000k");
                        recorder.setVideoOption("maxrate", "20000k");
                        
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

                        // Create popup menu for right-click
                        JPopupMenu imagePopupMenu = new JPopupMenu();
                        JMenuItem deleteMenuItem = new JMenuItem("Xoá");
                        deleteMenuItem.setIcon(UIManager.getIcon("FileView.fileIcon")); // Optional icon
                        deleteMenuItem.addActionListener(ev -> {
                            int confirm = JOptionPane.showConfirmDialog(
                                CheckUpPage.this,
                                "Bạn có chắc chắn muốn xoá ảnh này không?\n" + imageFile.getName(),
                                "Xác nhận xoá",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                            
                            if (confirm == JOptionPane.YES_OPTION) {
                                deleteImage(imageFile);
                            }
                        });
                        imagePopupMenu.add(deleteMenuItem);

                        // Add the click listener directly to the image label
                        imageLabel.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                if (SwingUtilities.isRightMouseButton(e)) {
                                    imagePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                                } else if (SwingUtilities.isLeftMouseButton(e)) {
                                    showFullImageDialog(imageFile);
                                }
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
            
            // Add Escape key listener to close dialog
            JRootPane rootPane = imageDialog.getRootPane();
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE");
            ActionMap actionMap = rootPane.getActionMap();
            actionMap.put("ESCAPE", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    imageDialog.dispose();
                }
            });
            
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
            Path baseDir = Paths.get(LocalStorage.checkupMediaBaseDir);
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
        private JComboBox<String> checkupTypeFilter;
        private List<Patient> filteredPatients;

        public QueueManagementPage() {
            super(mainFrame, "Danh sách chờ khám", true); // Set as modal dialog
            
            setIconImage(new ImageIcon("src/main/java/BsK/client/ui/assets/icon/database.png").getImage());
            setSize(1200, 700); // Increased size to accommodate filters and new column
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
                    applyFilters();
                    // Hide instead of closing to maintain state
                    setVisible(false);
                }
            });

            // Initialize filtered patients list
            filteredPatients = new ArrayList<>(patientQueue);

            // Create filter panel
            JPanel filterPanel = createFilterPanel();
            add(filterPanel, BorderLayout.NORTH);

            String[] columns = {"STT", "Mã Khám", "Họ và Tên", "Năm sinh", "Bác Sĩ", "Loại khám", "Trạng thái"};
            queueTableModel = new DefaultTableModel(preprocessPatientDataForTable(filteredPatients), columns) {
                @Override
                public boolean isCellEditable(int row, int column) { return false; }
            };
            
            queueTable = new JTable(queueTableModel);
            queueTable.setPreferredScrollableViewportSize(new Dimension(1000, 400)); // Increased width for new column
            queueTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 18));
            queueTable.setFont(new Font("Arial", Font.BOLD, 20));
            queueTable.setRowHeight(35);
            queueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            

            queueTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            queueTable.getColumnModel().getColumn(1).setPreferredWidth(90);
            queueTable.getColumnModel().getColumn(2).setPreferredWidth(250);
            queueTable.getColumnModel().getColumn(3).setPreferredWidth(80);
            queueTable.getColumnModel().getColumn(4).setPreferredWidth(220);
            queueTable.getColumnModel().getColumn(5).setPreferredWidth(120);
            queueTable.getColumnModel().getColumn(6).setPreferredWidth(120);
            // Create a row sorter for the table
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(queueTableModel);
            queueTable.setRowSorter(sorter);
            
            // Set custom comparator for STT column to sort as integers
            sorter.setComparator(0, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    try {
                        Integer i1 = Integer.parseInt(s1);
                        Integer i2 = Integer.parseInt(s2);
                        return i1.compareTo(i2);
                    } catch (NumberFormatException e) {
                        return s1.compareTo(s2); // Fallback to string comparison
                    }
                }
            });
            
            // Sort by "STT" (queue number) column in ascending order
            List<RowSorter.SortKey> sortKeys = new ArrayList<>();
            sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
            sorter.setSortKeys(sortKeys);

            queueTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        SwingUtilities.invokeLater(() -> {
                            int selectedRow = queueTable.getSelectedRow();
                            if (selectedRow != -1) {
                                // Convert view index to model index to handle sorting
                                int modelRow = queueTable.convertRowIndexToModel(selectedRow);
                                String newlySelectedCheckupId = (String) queueTableModel.getValueAt(modelRow, 1); // Column 1 is Checkup ID

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
                                // Resolve the selected patient by Checkup ID to avoid index mismatch after filtering/sorting
                                Patient selected = patientQueue.stream()
                                        .filter(p -> newlySelectedCheckupId.equals(p.getCheckupId()))
                                        .findFirst().orElse(null);
                                if (selected != null) {
                                    CheckUpPage.this.handleRowSelection(-1, selected);
                                }
                                applyFilters();
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
            
            // Add key bindings for Enter key to select patient
            queueTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "selectPatient");
            queueTable.getActionMap().put("selectPatient", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRow = queueTable.getSelectedRow();
                    if (selectedRow != -1) {
                        // Simulate the mouse click logic
                        SwingUtilities.invokeLater(() -> {
                            // Convert view index to model index to handle sorting
                            int modelRow = queueTable.convertRowIndexToModel(selectedRow);
                            String newlySelectedCheckupId = (String) queueTableModel.getValueAt(modelRow, 1); // Column 1 is Checkup ID

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
                            // Resolve patient by Checkup ID to avoid index mismatch
                            Patient selected = patientQueue.stream()
                                    .filter(p -> newlySelectedCheckupId.equals(p.getCheckupId()))
                                    .findFirst().orElse(null);
                            if (selected != null) {
                                CheckUpPage.this.handleRowSelection(-1, selected);
                            }
                            applyFilters();
                            // Hide the dialog after selection
                            QueueManagementPage.this.setVisible(false);
                        });
                    }
                }
            });

            // Add Escape key listener to close dialog
            JRootPane rootPane = this.getRootPane();
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE");
            ActionMap actionMap = rootPane.getActionMap();
            actionMap.put("ESCAPE", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyFilters();
                    setVisible(false);
                }
            });
        }

        

        private JPanel createFilterPanel() {
            JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            filterPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Bộ lọc",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16),
                new Color(63, 81, 181)
            ));
            filterPanel.setBackground(Color.WHITE);

            // Checkup Type Filter
            JLabel checkupTypeLabel = new JLabel("Loại khám:");
            checkupTypeLabel.setFont(new Font("Arial", Font.BOLD, 14));
            filterPanel.add(checkupTypeLabel);

            checkupTypeFilter = new JComboBox<>(new String[]{"Tất cả", "PHỤ KHOA", "THAI", "KHÁC"});
            checkupTypeFilter.setFont(new Font("Arial", Font.PLAIN, 14));
            checkupTypeFilter.setPreferredSize(new Dimension(120, 30));
            checkupTypeFilter.addActionListener(e -> applyFilters());
            filterPanel.add(checkupTypeFilter);

            // Separator
            filterPanel.add(Box.createHorizontalStrut(20));

            // Clear Filter Button
            filterPanel.add(Box.createHorizontalStrut(20));

            return filterPanel;
        }

        private void applyFilters() {
            String selectedCheckupType = (String) checkupTypeFilter.getSelectedItem();
        
            // 1. Filter your master list into a temporary list
            filteredPatients = patientQueue.stream()
                .filter(patient -> {
                    // Checkup type filter with normalized comparison (accent-insensitive, case-insensitive)
                    if (!"Tất cả".equals(selectedCheckupType)) {
                        String normalizedSelected = TextUtils.removeAccents(selectedCheckupType).trim().toUpperCase();
                        String patientType = patient.getCheckupType();
                        String normalizedPatientType = TextUtils.removeAccents(patientType == null ? "" : patientType).trim().toUpperCase();
                        if (!normalizedSelected.equals(normalizedPatientType)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
        
            // 2. Clear the table model's rows (this doesn't affect columns)
            queueTableModel.setRowCount(0);
        
            // 3. Add the new, filtered data row by row
            Object[][] filteredData = preprocessPatientDataForTable(filteredPatients);
            for (Object[] rowData : filteredData) {
                queueTableModel.addRow(rowData);
            }
            
            // NOTE: You don't need to call `setRowSorter` again if you're just updating rows.
            // The existing sorter will work correctly.
        
            // Auto-select first row after applying filters
            if (queueTable.getRowCount() > 0) {
                queueTable.setRowSelectionInterval(0, 0);
                queueTable.requestFocusInWindow();
            }
        }

        private int findRowByCheckupId(String checkupId) {
            // Find the patient in the patientQueue
            if (checkupId == null) return -1;
            for (int i = 0; i < queueTableModel.getRowCount(); i++) {
                if (checkupId.equals(queueTableModel.getValueAt(i, 1))) {
                    return i;
                }
            }
            return -1; // Not found
        }

        /**
         * Updates the data in the queue table. This should be called when the patient queue changes.
         */
        public void updateQueueTable() {
            SwingUtilities.invokeLater(() -> {
                String previouslySelectedId = CheckUpPage.this.selectedCheckupId;

                // Update filtered patients list and apply current filters
                filteredPatients = new ArrayList<>(patientQueue);
                applyFilters(); // This will update the table with filtered data and apply sorting

                if (previouslySelectedId != null) {
                    int rowToSelect = findRowByCheckupId(previouslySelectedId);
                    
                    if (rowToSelect != -1) {
                        int viewRow = queueTable.convertRowIndexToView(rowToSelect);
                        if (viewRow != -1) {
                            queueTable.setRowSelectionInterval(viewRow, viewRow);
                            queueTable.scrollRectToVisible(queueTable.getCellRect(viewRow, 0, true));
                        }
                    } else {
                        // Not visible under current filters. Check if the patient still exists in the full queue.
                        Patient target = patientQueue.stream()
                                .filter(p -> previouslySelectedId.equals(p.getCheckupId()))
                                .findFirst().orElse(null);
                        if (target != null) {
                            // Adjust filters to include the target patient's type and re-apply
                            if (checkupTypeFilter != null) {
                                String type = target.getCheckupType();
                                checkupTypeFilter.setSelectedItem(type == null || type.isEmpty() ? "Tất cả" : type);
                            }

                            applyFilters();
                            int rowAgain = findRowByCheckupId(previouslySelectedId);
                            if (rowAgain != -1) {
                                int viewRow = queueTable.convertRowIndexToView(rowAgain);
                                if (viewRow != -1) {
                                    queueTable.setRowSelectionInterval(viewRow, viewRow);
                                    queueTable.scrollRectToVisible(queueTable.getCellRect(viewRow, 0, true));
                                }
                            } else if (queueTable.getRowCount() > 0) {
                                // Fall back to first row without showing a misleading status-change message
                                queueTable.setRowSelectionInterval(0, 0);
                            }
                        } else {
                            // Patient truly no longer in queue; show message and clear
                            JOptionPane.showMessageDialog(
                                    mainFrame,
                                    "Trạng thái của bệnh nhân bạn đang chọn đã thay đổi và không còn trong hàng chờ.\n" +
                                    "Giao diện của bạn sẽ được làm mới.",
                                    "Cập Nhật Trạng Thái Bệnh Nhân",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            
                            clearPatientDetails(); // Now clear the details
                            if (queueTable.getRowCount() > 0) {
                                queueTable.setRowSelectionInterval(0, 0);
                            }
                        }
                    }
                } else {
                    // No previously selected patient, auto-select first row if available
                    if (queueTable.getRowCount() > 0) {
                        queueTable.setRowSelectionInterval(0, 0);
                    }
                }
                
                // Ensure table gets focus for keyboard navigation
                queueTable.requestFocusInWindow();
            });
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
                    rtfContent.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Arial;}}\n");
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
        if (checkupIdField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chưa chọn bệnh nhân.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DoctorItem selectedDoctor = (DoctorItem) doctorComboBox.getSelectedItem();
        String doctorIdStr = (selectedDoctor != null) ? selectedDoctor.getId() : null;
        DoctorItem selectedUltrasoundDoctor = (DoctorItem) ultrasoundDoctorComboBox.getSelectedItem();
        String ultrasoundDoctorIdStr = (selectedUltrasoundDoctor != null) ? selectedUltrasoundDoctor.getId() : null;

        if (doctorIdStr == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn bác sĩ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date checkupDate = (Date) datePicker.getModel().getValue();
        Date dob = (Date) dobPicker.getModel().getValue();
        Date recheckupDate = (Date) recheckupDatePicker.getModel().getValue();
    
        // Add validation for the required dates
        if (checkupDate == null) {
            JOptionPane.showMessageDialog(this, "Ngày khám không được để trống.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dob == null) {
            JOptionPane.showMessageDialog(this, "Ngày sinh không được để trống.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (needRecheckupCheckbox.isSelected() && recheckupDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày tái khám.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
        SaveCheckupRequest request = new SaveCheckupRequest(
                Integer.parseInt(checkupIdField.getText()),
                Integer.parseInt(customerIdField.getText()),
                Integer.parseInt(doctorIdStr),
                ultrasoundDoctorIdStr != null ? Integer.parseInt(ultrasoundDoctorIdStr) : null,
                checkupDate.getTime(),
                suggestionField.getText(),
                diagnosisField.getText(),
                getRtfContentAsString(),
                (String) statusComboBox.getSelectedItem(),
                (String) checkupTypeComboBox.getSelectedItem(),
                conclusionField.getText(),
                recheckupDate != null ? recheckupDate.getTime() : null,
                needRecheckupCheckbox.isSelected(),
                customerFirstNameField.getText(),
                customerLastNameField.getText(),
                dob.getTime(),
                (String) genderComboBox.getSelectedItem(),
                customerAddressField.getText() + ", " + (wardComboBox.getSelectedItem() != null ? wardComboBox.getSelectedItem().toString() : "") + ", " + (provinceComboBox.getSelectedItem() != null ? provinceComboBox.getSelectedItem().toString() : ""),
                customerPhoneField.getText(),
                (Double) customerWeightSpinner.getValue(),
                (Double) customerHeightSpinner.getValue(),
                customerCccdDdcnField.getText(),
                (Integer) patientHeartRateSpinner.getValue(),
                bloodPressureSystolicSpinner.getValue() + "/" + bloodPressureDiastolicSpinner.getValue(),
            medicinePrescription,
            servicePrescription
        );
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
        saved = true;
        } catch (Exception e) {
            log.error("Failed to create or send SaveCheckupRequest", e);
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleTemplateSelection() {
        String selectedTemplateName = (String) templateComboBox.getSelectedItem();

        if (selectedTemplateName == null || allTemplates == null || selectedTemplateName.equals("Không sử dụng mẫu")) {
            // Reset orientation to landscape when no template is selected
            orientationComboBox.setSelectedItem("ngang");
            // Clear template info when no template is selected
            imageCountValueLabel.setText("-");
            genderValueLabel.setText("-");
            return;
        }

        allTemplates.stream()
            .filter(t -> t.getTemplateName().equals(selectedTemplateName))
            .findFirst()
            .ifPresent(this::applyTemplate);
    }

    private void applyTemplate(Template template) {
        if (template == null) return;
        
        // Set the print orientation from the template
        if (template.getPrintType() != null) {
            orientationComboBox.setSelectedItem(template.getPrintType());
        }

        templateTitle = template.getTemplateTitle();
        photoNum = Integer.parseInt(template.getPhotoNum());
        
        // Use a try-catch for the RTF content to prevent application crashes
        try {
            RTFEditorKit rtfEditorKit = (RTFEditorKit) notesField.getEditorKit();
            // First clear the document to ensure we're not appending content
            notesField.getDocument().remove(0, notesField.getDocument().getLength());
            
            if (template.getContent() != null && !template.getContent().isEmpty()) {
                rtfEditorKit.read(new StringReader(template.getContent()), notesField.getDocument(), 0);
            }
        } catch (Exception e) {
            log.error("Failed to apply RTF content from template '{}'", template.getTemplateName(), e);
            // Fallback to plain text or show an error
            notesField.setText("Lỗi khi tải nội dung mẫu.");
        }

        // Only set fields if they have content in the template
        if (template.getDiagnosis() != null && !template.getDiagnosis().isEmpty()) {
            diagnosisField.setText(template.getDiagnosis());
        }
        
        if (template.getConclusion() != null && !template.getConclusion().isEmpty()) {
            conclusionField.setText(template.getConclusion());
        }
        
        if (template.getSuggestion() != null && !template.getSuggestion().isEmpty()) {
            suggestionField.setText(template.getSuggestion());
        }
        
        callingStatusLabel.setText("Đã áp dụng mẫu '" + template.getTemplateName() + "'.");
        callingStatusLabel.setBackground(new Color(220, 230, 255));
        callingStatusLabel.setForeground(new Color(0, 0, 139));
    }

    private void handleGetAllTemplatesResponse(GetAllTemplatesRes response) {
        log.info("Get all templates response");
        this.allTemplates = response.getTemplates();
        List<Template> templates = response.getTemplates();
        
        // Sort templates by STT first (ascending), then by name (alphabetical)
        templates.sort((t1, t2) -> {
            // First compare by STT (smaller numbers first)
            int sttCompare = Integer.compare(t1.getStt(), t2.getStt());
            if (sttCompare != 0) {
                return sttCompare;
            }
            // If STT is the same, compare by template name alphabetically
            return t1.getTemplateName().compareToIgnoreCase(t2.getTemplateName());
        });
        
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
    private void openHistoryViewDialog(String checkupId, String patientName, String checkupDate, String suggestion, String diagnosis, String conclusion, String notes, String reCheckupDate, String doctorName, String customerHeight, String customerWeight, String heartRate, String bloodPressure) {
        try {
            // Pass all the necessary details to the dialog including vitals
            HistoryViewDialog historyDialog = new HistoryViewDialog(mainFrame, checkupId, patientName, checkupDate, suggestion, diagnosis, conclusion, notes, reCheckupDate, doctorName, customerHeight, customerWeight, heartRate, bloodPressure);
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
            log.info("=== CLEARING PATIENT DETAILS FOR SAFETY ===");
            
            // SAFETY: Clear patient IDs FIRST to prevent ultrasound image mismatches
            String previousPatientId = currentCheckupIdForMedia;
            currentCheckupIdForMedia = null;
            currentCheckupMediaPath = null;
            selectedCheckupId = null;
            
            log.info("Cleared patient IDs - Previous ID: {}, Now: NULL", previousPatientId);
            
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
            setRtfContentFromString("{\\rtf1\\ansi\\ansicpg1252\\deff0\\deflang1033{\\fonttbl{\\f0\\fnil\\fcharset163 Arial;}}\\viewkind4\\uc1\\pard\\f0\\fs32\\par}"); // Clear notes
            customerCccdDdcnField.setText("");
            
            // Clear target address values
            targetWard = null;

            // if (doctorComboBox.getItemCount() > 0) doctorComboBox.setSelectedIndex(0);
            statusComboBox.setSelectedIndex(0);
            genderComboBox.setSelectedIndex(0);
            if (provinceComboBox.getItemCount() > 0) provinceComboBox.setSelectedIndex(0);
            // ward and ward will be cleared by province selection listener
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
            needRecheckupCheckbox.setSelected(false);
            recheckupDatePicker.setEnabled(false);
            recheckupDatePicker.getJFormattedTextField().setEditable(false);
            if (recheckupDatePickerButton != null) {
                recheckupDatePickerButton.setEnabled(false);
            }

            templateTitle = null;
            photoNum = 0;
            orientationComboBox.setSelectedItem("ngang");

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
        driveButton.setEnabled(false); // <-- ADD THIS LINE
        if (takePictureButton != null) takePictureButton.setEnabled(false);
        if (recordVideoButton != null) recordVideoButton.setEnabled(false);
        if (openFolderButton != null) openFolderButton.setEnabled(false);
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

    private DoctorItem findDoctorByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (DoctorItem item : LocalStorage.doctorsName) {
            if (item.getName().equalsIgnoreCase(name.trim())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Finds a DoctorItem in the LocalStorage list by their ID.
     * @param id The ID of the doctor to find.
     * @return The matching DoctorItem, or null if not found.
     */
    private DoctorItem findDoctorById(String id) {
        // Return null if the ID is invalid or the doctor list is not available
        if (id == null || id.isEmpty() || id.equals("0") || LocalStorage.doctorsName == null) {
            return null;
        }
        // Loop through the doctors and compare by ID
        for (DoctorItem doctor : LocalStorage.doctorsName) {
            if (doctor.getId().equals(id)) {
                return doctor; // Found a match
            }
        }
        return null; // No match found
    }

    public void loadPatientByCheckupId(Patient patient) {
        handleRowSelection(-1, patient);
    }

    /**
     * Styles the room status panels with appropriate background color, border, and text.
     * @param panel The JPanel to style.
     * @param label The JLabel inside the panel.
     * @param bgColor The background color (e.g., red for busy, green for free).
     * @param statusText The text to display below the room name (e.g., "TRỐNG" or "STT: 123").
     */
    private void styleRoomStatusPanel(JPanel panel, JLabel label, Color bgColor, String statusText) {
        // Retrieves the base room name (e.g., "PHÒNG 1") stored in the label's properties
        String baseRoomText = (String) label.getClientProperty("baseText");

        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(Color.WHITE);
        // Uses HTML to center text and create a line break
        label.setText("<html><div style='text-align: center;'>" + baseRoomText + "<br>" + statusText + "</div></html>");
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
    }
}


