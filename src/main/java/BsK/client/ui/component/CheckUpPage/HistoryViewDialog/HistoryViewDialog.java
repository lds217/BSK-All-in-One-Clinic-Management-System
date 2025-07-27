package BsK.client.ui.component.CheckUpPage.HistoryViewDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.text.rtf.RTFEditorKit;
import lombok.extern.slf4j.Slf4j;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.GetImagesByCheckupIdReq;
import BsK.common.packet.req.GetOrderInfoByCheckupReq;
import BsK.common.packet.res.GetImagesByCheckupIdRes;
import BsK.common.packet.res.GetOrderInfoByCheckupRes;
import BsK.common.util.network.NetworkUtil;

@Slf4j
public class HistoryViewDialog extends JDialog {
    
    // Data models
    private String checkupId;
    private String patientName;
    private String checkupDate;
    private String content;
    private String suggestion;
    private String diagnosis;
    private String conclusion;
    private String reCheckupDate;
    private String doctorName;
    private String[][] medicinePrescription;
    private String[][] servicePrescription;
    
    // Vitals data
    private String customerHeight;
    private String customerWeight;
    private String heartRate;
    private String bloodPressure;
    
    // UI Components
    private JLabel imagePreviewLabel;
    private JPanel imageListPanel;
    private JScrollPane imageListScrollPane;
    private JTextPane contentArea;
    private JTextArea suggestionArea;
    private JTextArea diagnosisArea;
    private JTextArea conclusionArea;
    private JTree prescriptionTree;
    private DefaultTreeModel prescriptionTreeModel;
    private DefaultMutableTreeNode rootPrescriptionNode;
    private JLabel totalMedCostLabel;
    private JLabel totalSerCostLabel;
    private JLabel overallTotalCostLabel;
    
    // Constants
    private static final int THUMBNAIL_WIDTH = 80;
    private static final int THUMBNAIL_HEIGHT = 80;
    // Using LocalStorage.checkupMediaBaseDir which is loaded at startup
    private static final DecimalFormat df = new DecimalFormat("#,##0");
    
    // Current selected image
    private BufferedImage currentSelectedImage;
    private List<File> imageFiles;
    private final ResponseListener<GetOrderInfoByCheckupRes> orderInfoListener = this::handleGetOrderInfoResponse;
    private final ResponseListener<GetImagesByCheckupIdRes> imagesListener = this::handleGetImagesResponse;
    
    public HistoryViewDialog(Frame parent, String checkupId, String patientName, String checkupDate, String suggestion, String diagnosis, String conclusion, String notes, String reCheckupDate, String doctorName, String customerHeight, String customerWeight, String heartRate, String bloodPressure) {
        super(parent, "Xem chi tiết lịch sử khám - " + patientName, true);
        this.checkupId = checkupId;
        this.patientName = patientName;
        this.checkupDate = checkupDate;
        this.imageFiles = new ArrayList<>();
        
        // Set data from parameters
        this.content = notes; // This is the RTF content
        this.suggestion = suggestion;
        this.diagnosis = diagnosis;
        this.conclusion = conclusion;
        this.reCheckupDate = reCheckupDate;
        this.doctorName = doctorName;
        this.customerHeight = customerHeight;
        this.customerWeight = customerWeight;
        this.heartRate = heartRate;
        this.bloodPressure = bloodPressure;
        initializeDialog(parent);
        setupUI();
        
        // Add listeners
        ClientHandler.addResponseListener(GetOrderInfoByCheckupRes.class, orderInfoListener);
        ClientHandler.addResponseListener(GetImagesByCheckupIdRes.class, imagesListener);

        // Fetch real data from server
        fetchOrderInformation();
        fetchCheckupImages();
    }
    
    private void fetchOrderInformation() {
        try {
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetOrderInfoByCheckupReq(this.checkupId));
            log.info("Sent GetOrderInfoByCheckupReq for checkupId: {}", this.checkupId);
        } catch (Exception e) {
            log.error("Error sending GetOrderInfoByCheckupReq for checkupId: {}", this.checkupId, e);
        }
    }
    
    private void fetchCheckupImages() {
        try {
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetImagesByCheckupIdReq(this.checkupId));
            log.info("Sent GetImagesByCheckupIdReq for checkupId: {}", this.checkupId);
        } catch (Exception e) {
            log.error("Error sending GetImagesByCheckupIdReq for checkupId: {}", this.checkupId, e);
        }
    }
    
    private void handleGetOrderInfoResponse(GetOrderInfoByCheckupRes response) {
        SwingUtilities.invokeLater(() -> {
            setMedicinePrescription(response.getMedicinePrescription());
            setServicePrescription(response.getServicePrescription());
            log.info("Successfully updated history order tree for checkupId: {}", checkupId);
        });
    }

    private void handleGetImagesResponse(GetImagesByCheckupIdRes response) {
        log.info("Client received GetImagesByCheckupIdRes for checkupId: {}. Image count: {}", 
            response.getCheckupId(), response.getImageDatas() != null ? response.getImageDatas().size() : 0);
        if (checkupId.equals(response.getCheckupId())) {
            SwingUtilities.invokeLater(() -> {
                updateImageList(response.getImageNames(), response.getImageDatas());
            });
        }
    }
    
    private void initializeDialog(Frame parent) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(parent);
        setResizable(true);
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        
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

            @Override
            public void windowClosed(WindowEvent e) {
                // Clean up listeners when dialog is closed
                ClientHandler.deleteListener(GetOrderInfoByCheckupRes.class, orderInfoListener);
                ClientHandler.deleteListener(GetImagesByCheckupIdRes.class, imagesListener);
            }
        });
    }
    
    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Create main content panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create center panel with main content
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);

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
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(63, 81, 181)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        headerPanel.setBackground(new Color(248, 249, 250));
        
        // Patient Info Panel
        JPanel patientInfoPanel = new JPanel(new GridBagLayout());
        patientInfoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 20); // Add spacing between components

        JLabel titleLabel = new JLabel("THÔNG TIN KHÁM BỆNH");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20)); // Increased title font
        titleLabel.setForeground(new Color(63, 81, 181));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4; // Span across all columns
        gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 0, 10, 20); // More bottom margin for title
        patientInfoPanel.add(titleLabel, gbc);

        // Reset constraints
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(4, 5, 4, 15);

        // Define fonts for reuse
        Font labelFont = new Font("Arial", Font.BOLD, 14);
        Font valueFont = new Font("Arial", Font.PLAIN, 14);

        // --- ROW 1 ---
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel patientLabel = new JLabel("Bệnh nhân:");
        patientLabel.setFont(labelFont);
        patientInfoPanel.add(patientLabel, gbc);

        gbc.gridx = 1;
        JLabel patientNameLabel = new JLabel(patientName);
        patientNameLabel.setFont(valueFont);
        patientInfoPanel.add(patientNameLabel, gbc);

        gbc.gridx = 2;
        JLabel doctorLabel = new JLabel("Bác sĩ khám:");
        doctorLabel.setFont(labelFont);
        patientInfoPanel.add(doctorLabel, gbc);

        gbc.gridx = 3;
        JLabel doctorNameLabel = new JLabel(getDisplayableString(this.doctorName));
        doctorNameLabel.setFont(valueFont);
        patientInfoPanel.add(doctorNameLabel, gbc);

        // --- ROW 2 ---
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel dateLabel = new JLabel("Ngày khám:");
        dateLabel.setFont(labelFont);
        patientInfoPanel.add(dateLabel, gbc);

        gbc.gridx = 1;
        JLabel checkupDateLabel = new JLabel(formatDate(checkupDate));
        checkupDateLabel.setFont(valueFont);
        patientInfoPanel.add(checkupDateLabel, gbc);

        gbc.gridx = 2;
        JLabel recheckupDateLabel = new JLabel("Ngày tái khám:");
        recheckupDateLabel.setFont(labelFont);
        patientInfoPanel.add(recheckupDateLabel, gbc);

        gbc.gridx = 3;
        JLabel recheckupValueLabel = new JLabel(getDisplayableString(this.reCheckupDate));
        recheckupValueLabel.setFont(valueFont);
        patientInfoPanel.add(recheckupValueLabel, gbc);

        // --- ROW 3 ---
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel idLabel = new JLabel("Mã khám:");
        idLabel.setFont(labelFont);
        patientInfoPanel.add(idLabel, gbc);

        gbc.gridx = 1;
        JLabel checkupIdLabel = new JLabel(checkupId);
        checkupIdLabel.setFont(valueFont);
        patientInfoPanel.add(checkupIdLabel, gbc);

        // --- ROW 4 - Vitals Information ---
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel heightLabel = new JLabel("Chiều cao:");
        heightLabel.setFont(labelFont);
        patientInfoPanel.add(heightLabel, gbc);

        gbc.gridx = 1;
        JLabel heightValueLabel = new JLabel(getDisplayableString(this.customerHeight) + " cm");
        heightValueLabel.setFont(valueFont);
        patientInfoPanel.add(heightValueLabel, gbc);

        gbc.gridx = 2;
        JLabel weightLabel = new JLabel("Cân nặng:");
        weightLabel.setFont(labelFont);
        patientInfoPanel.add(weightLabel, gbc);

        gbc.gridx = 3;
        JLabel weightValueLabel = new JLabel(getDisplayableString(this.customerWeight) + " kg");
        weightValueLabel.setFont(valueFont);
        patientInfoPanel.add(weightValueLabel, gbc);

        // --- ROW 5 - Heart Rate and Blood Pressure ---
        gbc.gridy = 5;
        gbc.gridx = 0;
        JLabel heartRateLabel = new JLabel("Nhịp tim:");
        heartRateLabel.setFont(labelFont);
        patientInfoPanel.add(heartRateLabel, gbc);

        gbc.gridx = 1;
        JLabel heartRateValueLabel = new JLabel(getDisplayableString(this.heartRate) + " bpm");
        heartRateValueLabel.setFont(valueFont);
        patientInfoPanel.add(heartRateValueLabel, gbc);

        gbc.gridx = 2;
        JLabel bloodPressureLabel = new JLabel("Huyết áp:");
        bloodPressureLabel.setFont(labelFont);
        patientInfoPanel.add(bloodPressureLabel, gbc);

        gbc.gridx = 3;
        JLabel bloodPressureValueLabel = new JLabel(getDisplayableString(this.bloodPressure) + " mmHg");
        bloodPressureValueLabel.setFont(valueFont);
        patientInfoPanel.add(bloodPressureValueLabel, gbc);
        
        headerPanel.add(patientInfoPanel, BorderLayout.CENTER);
        
        // Right side - Close button
        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Arial", Font.BOLD, 12));
        closeButton.setBackground(new Color(244, 67, 54));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(closeButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        // Create main split pane (horizontal)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // Left side gets 60%
        mainSplitPane.setDividerSize(8);
        mainSplitPane.setContinuousLayout(true);
        
        // Left side - Image section (split vertically)
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setResizeWeight(0.75); // Top gets 75%
        leftSplitPane.setDividerSize(6);
        leftSplitPane.setContinuousLayout(true);
        
        // Top left - Image preview (biggest area)
        JPanel imagePreviewPanel = createImagePreviewPanel();
        leftSplitPane.setTopComponent(imagePreviewPanel);
        
        // Bottom left - Image list
        JPanel imageListPanelContainer = createImageListPanel();
        leftSplitPane.setBottomComponent(imageListPanelContainer);
        
        mainSplitPane.setLeftComponent(leftSplitPane);
        
        // Right side (split vertically)
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setResizeWeight(0.5); // Equal split
        rightSplitPane.setDividerSize(6);
        rightSplitPane.setContinuousLayout(true);
        
        // Top right - Information content
        JPanel infoPanel = createInformationPanel();
        rightSplitPane.setTopComponent(infoPanel);
        
        // Bottom right - Prescription tree
        JPanel prescriptionPanel = createPrescriptionPanel();
        rightSplitPane.setBottomComponent(prescriptionPanel);
        
        mainSplitPane.setRightComponent(rightSplitPane);
        
        centerPanel.add(mainSplitPane, BorderLayout.CENTER);
        
        return centerPanel;
    }
    
    private JPanel createImagePreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            "Xem trước hình ảnh",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14), new Color(76, 175, 80)
        ));
        
        // Create image preview label
        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setBackground(new Color(250, 250, 250));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        // Default message
        imagePreviewLabel.setText("<html><center><div style='color: #999; font-size: 14px;'>" +
                                 "<p>Chọn hình ảnh bên dưới để xem</p>" +
                                 "<p>hoặc không có hình ảnh nào</p></div></center></html>");
        
        JScrollPane scrollPane = new JScrollPane(imagePreviewLabel);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createImageListPanel() {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 152, 0), 2),
            "Danh sách hình ảnh",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14), new Color(255, 152, 0)
        ));
        
        // Create image list panel with flow layout
        imageListPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        imageListPanel.setBackground(Color.WHITE);
        
        // Create scroll pane for image list
        imageListScrollPane = new JScrollPane(imageListPanel);
        imageListScrollPane.setPreferredSize(new Dimension(0, 120));
        imageListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        imageListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        containerPanel.add(imageListScrollPane, BorderLayout.CENTER);
        
        return containerPanel;
    }
    
    private JPanel createInformationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(63, 81, 181), 2),
            "Thông tin khám bệnh",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14), new Color(63, 81, 181)
        ));
        
        // Create tabbed pane for different information sections
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 12));
        
        // Content tab
        contentArea = new JTextPane();
        contentArea.setEditable(false);
        contentArea.setFont(new Font("Arial", Font.PLAIN, 12));
        contentArea.setBackground(new Color(248, 249, 250));
        contentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setRtfContent(contentArea, this.content);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        tabbedPane.addTab("Nội dung khám", contentScroll);
        
        // Suggestion tab
        suggestionArea = new JTextArea(getDisplayableString(this.suggestion));
        suggestionArea.setEditable(false);
        suggestionArea.setLineWrap(true);
        suggestionArea.setWrapStyleWord(true);
        suggestionArea.setFont(new Font("Arial", Font.PLAIN, 12));
        suggestionArea.setBackground(new Color(248, 249, 250));
        suggestionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane suggestionScroll = new JScrollPane(suggestionArea);
        tabbedPane.addTab("Đề nghị", suggestionScroll);
        
        // Diagnosis tab
        diagnosisArea = new JTextArea(getDisplayableString(this.diagnosis));
        diagnosisArea.setEditable(false);
        diagnosisArea.setLineWrap(true);
        diagnosisArea.setWrapStyleWord(true);
        diagnosisArea.setFont(new Font("Arial", Font.PLAIN, 12));
        diagnosisArea.setBackground(new Color(248, 249, 250));
        diagnosisArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane diagnosisScroll = new JScrollPane(diagnosisArea);
        tabbedPane.addTab("Chẩn đoán", diagnosisScroll);
        
        // Conclusion tab
        conclusionArea = new JTextArea(getDisplayableString(this.conclusion));
        conclusionArea.setEditable(false);
        conclusionArea.setLineWrap(true);
        conclusionArea.setWrapStyleWord(true);
        conclusionArea.setFont(new Font("Arial", Font.PLAIN, 12));
        conclusionArea.setBackground(new Color(248, 249, 250));
        conclusionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane conclusionScroll = new JScrollPane(conclusionArea);
        tabbedPane.addTab("Kết luận", conclusionScroll);
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createPrescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(156, 39, 176), 2),
            "Đơn thuốc & Dịch vụ",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14), new Color(156, 39, 176)
        ));
        
        // Create prescription tree
        rootPrescriptionNode = new DefaultMutableTreeNode("Đơn thuốc & Dịch vụ");
        prescriptionTreeModel = new DefaultTreeModel(rootPrescriptionNode);
        prescriptionTree = new JTree(prescriptionTreeModel);
        prescriptionTree.setFont(new Font("Arial", Font.PLAIN, 12));
        prescriptionTree.setRowHeight(22);
        prescriptionTree.setShowsRootHandles(true);
        prescriptionTree.setRootVisible(false);
        
        JScrollPane treeScrollPane = new JScrollPane(prescriptionTree);
        treeScrollPane.setPreferredSize(new Dimension(0, 200));
        
        // Create cost summary panel
        JPanel costSummaryPanel = new JPanel(new GridBagLayout());
        costSummaryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        costSummaryPanel.setBackground(new Color(248, 249, 250));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);
        
        totalMedCostLabel = new JLabel("Tổng tiền thuốc: 0 VNĐ");
        totalMedCostLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridx = 0; gbc.gridy = 0;
        costSummaryPanel.add(totalMedCostLabel, gbc);
        
        totalSerCostLabel = new JLabel("Tổng tiền dịch vụ: 0 VNĐ");
        totalSerCostLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridy = 1;
        costSummaryPanel.add(totalSerCostLabel, gbc);
        
        overallTotalCostLabel = new JLabel("TỔNG CỘNG: 0 VNĐ");
        overallTotalCostLabel.setFont(new Font("Arial", Font.BOLD, 14));
        overallTotalCostLabel.setForeground(new Color(244, 67, 54));
        gbc.gridy = 2;
        costSummaryPanel.add(overallTotalCostLabel, gbc);
        
        panel.add(treeScrollPane, BorderLayout.CENTER);
        panel.add(costSummaryPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateImageList(List<String> imageNames, List<byte[]> imageDatas) {
        SwingUtilities.invokeLater(() -> {
            imageListPanel.removeAll();
            
            if (imageNames != null && !imageNames.isEmpty()) {
                log.info("Populating image list with {} images for checkupId: {}", imageNames.size(), checkupId);
                for (int i = 0; i < imageNames.size(); i++) {
                    byte[] imageData = imageDatas.get(i);
                    String imageName = imageNames.get(i);
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
                        BufferedImage image = ImageIO.read(bais);
                        if (image != null) {
                            JButton thumbnailButton = createThumbnailButton(image, imageName);
                            imageListPanel.add(thumbnailButton);
                        } else {
                            log.warn("Failed to decode image data for '{}'. ImageIO.read returned null. Data length: {} bytes.", imageName, imageData.length);
                        }
                    } catch (IOException e) {
                        log.error("Error decoding image data for {}", imageName, e);
                    }
                }
                // Display the first image automatically
                if (!imageDatas.isEmpty()) {
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(imageDatas.get(0));
                        BufferedImage firstImage = ImageIO.read(bais);
                        if (firstImage != null) {
                             displayFullImage(firstImage, imageNames.get(0));
                        }
                    } catch (IOException e) {
                        log.error("Error decoding first image", e);
                    }
                }
            } else {
                log.info("No images found for checkupId: {}. Displaying 'No images' label.", checkupId);
                JLabel noImagesLabel = new JLabel("Không có hình ảnh nào");
                noImagesLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noImagesLabel.setForeground(Color.GRAY);
                imageListPanel.add(noImagesLabel);
            }
            
            imageListPanel.revalidate();
            imageListPanel.repaint();
        });
    }
    
    private JButton createThumbnailButton(BufferedImage image, String imageName) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (image != null) {
            Image scaledImage = image.getScaledInstance(
                THUMBNAIL_WIDTH - 4, THUMBNAIL_HEIGHT - 4, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledImage));
            
            // Add click listener to show full image
            button.addActionListener(e -> displayFullImage(image, imageName));
        } else {
             button.setText("Error");
        }
        
        // Add hover effects
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBorder(BorderFactory.createRaisedBevelBorder());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBorder(BorderFactory.createLoweredBevelBorder());
            }
        });
        
        return button;
    }
    
    private void displayFullImage(BufferedImage image, String imageName) {
        currentSelectedImage = image;

        // Calculate the best fit for the preview area.
        // Use the container's size (the JScrollPane's viewport) instead of the label's own size
        // to avoid a feedback loop where the scaled image size influences the next scaling operation.
        Container previewContainer = imagePreviewLabel.getParent();
        Dimension previewSize = (previewContainer != null && previewContainer.getWidth() > 0)
            ? previewContainer.getSize()
            : new Dimension(400, 300); // Fallback size

        double scaleX = (double) previewSize.width / image.getWidth();
        double scaleY = (double) previewSize.height / image.getHeight();
        double scale = Math.min(scaleX, scaleY);

        int scaledWidth = (int) (image.getWidth() * scale);
        int scaledHeight = (int) (image.getHeight() * scale);

        Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaledImage);

        imagePreviewLabel.setText(""); // Clear text
        imagePreviewLabel.setIcon(icon);
        imagePreviewLabel.setToolTipText("Hình ảnh: " + imageName + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }
    
    private void updatePrescriptionTree() {
        rootPrescriptionNode.removeAllChildren();
        double totalMedicineCost = 0;
        double totalServiceCost = 0;

        // Add medicines
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

        // Add services
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
        
        // Update cost labels
        totalMedCostLabel.setText("Tổng tiền thuốc: " + df.format(totalMedicineCost) + " VNĐ");
        totalSerCostLabel.setText("Tổng tiền dịch vụ: " + df.format(totalServiceCost) + " VNĐ");
        overallTotalCostLabel.setText("TỔNG CỘNG: " + df.format(totalMedicineCost + totalServiceCost) + " VNĐ");

        // Refresh tree
        prescriptionTreeModel.reload();
        
        // Expand all nodes
        for (int i = 0; i < prescriptionTree.getRowCount(); i++) {
            prescriptionTree.expandRow(i);
        }
    }
    
    private boolean isImageFile(String filename) {
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp"};
        String lowerFilename = filename.toLowerCase();
        for (String ext : extensions) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    private String formatDate(String dateStr) {
        try {
            if (dateStr != null && dateStr.matches("\\d+")) {
                // Timestamp
                Date date = new Date(Long.parseLong(dateStr));
                return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
            } else if (dateStr != null && !dateStr.trim().isEmpty()) {
                // Already formatted date
                return dateStr;
            }
        } catch (NumberFormatException e) {
            log.warn("Error formatting date: {}", dateStr, e);
        }
        return dateStr != null ? dateStr : "Không rõ";
    }
    
    // Setters for updating data from external sources
    public void setContent(String content) {
        this.content = content;
        if (contentArea != null) {
            setRtfContent(contentArea, content);
        }
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
        if (suggestionArea != null) {
            suggestionArea.setText(getDisplayableString(suggestion));
        }
    }
    
    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
        if (diagnosisArea != null) {
            diagnosisArea.setText(getDisplayableString(diagnosis));
        }
    }
    
    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
        if (conclusionArea != null) {
            conclusionArea.setText(getDisplayableString(conclusion));
        }
    }
    
    public void setMedicinePrescription(String[][] medicinePrescription) {
        this.medicinePrescription = medicinePrescription;
        updatePrescriptionTree();
    }
    
    public void setServicePrescription(String[][] servicePrescription) {
        this.servicePrescription = servicePrescription;
        updatePrescriptionTree();
    }
    
    private void setRtfContent(JTextPane textPane, String rtfContent) {
        textPane.setContentType("text/rtf");
        // Check for null, empty, or placeholder RTF content
        if (rtfContent == null || rtfContent.isBlank() || rtfContent.contains("\\cf0 null\\par")) {
            rtfContent = "{\\rtf1\\ansi\\deff0 {\\fonttbl{\\f0 Arial;}}\\fs24\\i\\cf1 Không có thông tin.\\par}"; // Italic "No information"
        }
        try {
            // The RTF data from the database might be encoded in a specific way.
            // Using a stream helps handle character encoding correctly.
            textPane.getEditorKit().read(new java.io.ByteArrayInputStream(rtfContent.getBytes()), textPane.getDocument(), 0);
        } catch (Exception e) {
            log.error("Failed to set RTF content for checkupId {}", checkupId, e);
            // Fallback to plain text if RTF parsing fails
            textPane.setContentType("text/plain");
            textPane.setText("Không thể hiển thị nội dung chi tiết (lỗi định dạng).");
        }
    }
    
    private String getDisplayableString(String input) {
        if (input == null || input.isBlank() || "null".equalsIgnoreCase(input.trim())) {
            return "Không có thông tin";
        }
        return input;
    }
} 