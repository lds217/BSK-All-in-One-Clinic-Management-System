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
import java.awt.image.BufferedImage;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HistoryViewDialog extends JDialog {
    
    // Data models
    private String checkupId;
    private String patientName;
    private String checkupDate;
    private String content;
    private String suggestion;
    private String conclusion;
    private String[][] medicinePrescription;
    private String[][] servicePrescription;
    
    // UI Components
    private JLabel imagePreviewLabel;
    private JPanel imageListPanel;
    private JScrollPane imageListScrollPane;
    private JTextArea contentArea;
    private JTextArea suggestionArea;
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
    private static final String CHECKUP_MEDIA_BASE_DIR = "src/main/resources/image/checkup_media";
    private static final DecimalFormat df = new DecimalFormat("#,##0");
    
    // Current selected image
    private BufferedImage currentSelectedImage;
    private List<File> imageFiles;
    
    public HistoryViewDialog(Frame parent, String checkupId, String patientName, String checkupDate) {
        super(parent, "Xem chi tiết lịch sử khám - " + patientName, true);
        this.checkupId = checkupId;
        this.patientName = patientName;
        this.checkupDate = checkupDate;
        this.imageFiles = new ArrayList<>();
        
        // Initialize sample data for demonstration
        initializeSampleData();
        
        initializeDialog(parent);
        setupUI();
        loadImagesForCheckup();
        updatePrescriptionTree();
    }
    
    private void initializeSampleData() {
        // Sample data - in real implementation, this would come from database
        this.content = "Bệnh nhân có triệu chứng đau đầu, sốt nhẹ. Khám tổng quát không có bất thường đáng kể.";
        this.suggestion = "Nghỉ ngơi đầy đủ, uống nhiều nước, theo dõi thêm 2-3 ngày.";
        this.conclusion = "Cảm cúm thông thường, không có biến chứng.";
        
        // Sample medicine prescription
        this.medicinePrescription = new String[][] {
            {"1", "Paracetamol 500mg", "2", "viên", "1", "1", "1", "5000", "30000", "Uống sau ăn"},
            {"2", "Vitamin C", "1", "lọ", "0", "1", "0", "15000", "15000", "Uống trước ăn"}
        };
        
        // Sample service prescription
        this.servicePrescription = new String[][] {
            {"1", "Xét nghiệm máu", "1", "100000", "100000", "Xét nghiệm tổng quát"},
            {"2", "Siêu âm", "1", "200000", "200000", "Siêu âm bụng"}
        };
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
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(63, 81, 181)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        headerPanel.setBackground(new Color(248, 249, 250));
        
        // Left side - Patient info
        JPanel patientInfoPanel = new JPanel(new GridBagLayout());
        patientInfoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 20);
        
        JLabel titleLabel = new JLabel("THÔNG TIN KHÁM BỆNH");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(63, 81, 181));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        patientInfoPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel patientLabel = new JLabel("Bệnh nhân:");
        patientLabel.setFont(new Font("Arial", Font.BOLD, 12));
        patientInfoPanel.add(patientLabel, gbc);
        
        gbc.gridx = 1;
        JLabel patientNameLabel = new JLabel(patientName);
        patientNameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        patientInfoPanel.add(patientNameLabel, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel dateLabel = new JLabel("Ngày khám:");
        dateLabel.setFont(new Font("Arial", Font.BOLD, 12));
        patientInfoPanel.add(dateLabel, gbc);
        
        gbc.gridx = 1;
        JLabel checkupDateLabel = new JLabel(formatDate(checkupDate));
        checkupDateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        patientInfoPanel.add(checkupDateLabel, gbc);
        
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel idLabel = new JLabel("Mã khám:");
        idLabel.setFont(new Font("Arial", Font.BOLD, 12));
        patientInfoPanel.add(idLabel, gbc);
        
        gbc.gridx = 1;
        JLabel checkupIdLabel = new JLabel(checkupId);
        checkupIdLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        patientInfoPanel.add(checkupIdLabel, gbc);
        
        headerPanel.add(patientInfoPanel, BorderLayout.WEST);
        
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
        contentArea = new JTextArea(content);
        contentArea.setEditable(false);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setFont(new Font("Arial", Font.PLAIN, 12));
        contentArea.setBackground(new Color(248, 249, 250));
        contentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane contentScroll = new JScrollPane(contentArea);
        tabbedPane.addTab("Nội dung khám", contentScroll);
        
        // Suggestion tab
        suggestionArea = new JTextArea(suggestion);
        suggestionArea.setEditable(false);
        suggestionArea.setLineWrap(true);
        suggestionArea.setWrapStyleWord(true);
        suggestionArea.setFont(new Font("Arial", Font.PLAIN, 12));
        suggestionArea.setBackground(new Color(248, 249, 250));
        suggestionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane suggestionScroll = new JScrollPane(suggestionArea);
        tabbedPane.addTab("Đề nghị", suggestionScroll);
        
        // Conclusion tab
        conclusionArea = new JTextArea(conclusion);
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
    
    private void loadImagesForCheckup() {
        SwingUtilities.invokeLater(() -> {
            imageFiles.clear();
            imageListPanel.removeAll();
            
            // Load images from checkup media directory
            Path mediaPath = Paths.get(CHECKUP_MEDIA_BASE_DIR, checkupId);
            
            if (Files.exists(mediaPath) && Files.isDirectory(mediaPath)) {
                try {
                    Files.list(mediaPath)
                         .filter(path -> isImageFile(path.toString()))
                         .forEach(path -> {
                             File imageFile = path.toFile();
                             imageFiles.add(imageFile);
                             
                             // Create thumbnail button
                             JButton thumbnailButton = createThumbnailButton(imageFile);
                             imageListPanel.add(thumbnailButton);
                         });
                } catch (IOException e) {
                    log.error("Error loading images for checkup {}", checkupId, e);
                }
            }
            
            if (imageFiles.isEmpty()) {
                JLabel noImagesLabel = new JLabel("Không có hình ảnh nào");
                noImagesLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noImagesLabel.setForeground(Color.GRAY);
                imageListPanel.add(noImagesLabel);
            }
            
            imageListPanel.revalidate();
            imageListPanel.repaint();
        });
    }
    
    private JButton createThumbnailButton(File imageFile) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        try {
            BufferedImage originalImage = ImageIO.read(imageFile);
            if (originalImage != null) {
                Image scaledImage = originalImage.getScaledInstance(
                    THUMBNAIL_WIDTH - 4, THUMBNAIL_HEIGHT - 4, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaledImage));
                
                // Add click listener to show full image
                button.addActionListener(e -> displayFullImage(originalImage, imageFile.getName()));
            }
        } catch (IOException e) {
            log.error("Error creating thumbnail for image {}", imageFile.getName(), e);
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
        
        // Calculate the best fit for the preview area
        Dimension previewSize = imagePreviewLabel.getSize();
        if (previewSize.width <= 0 || previewSize.height <= 0) {
            previewSize = new Dimension(400, 300); // Default size
        }
        
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
            contentArea.setText(content);
        }
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
        if (suggestionArea != null) {
            suggestionArea.setText(suggestion);
        }
    }
    
    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
        if (conclusionArea != null) {
            conclusionArea.setText(conclusion);
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
} 