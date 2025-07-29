package BsK.client.ui.component.DashboardPage.RecheckUpDialog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
// NEW: Imports for modern date/time handling
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.*;
import BsK.client.network.handler.ClientHandler;
import BsK.common.util.network.NetworkUtil;
import BsK.common.packet.req.GetRecheckUpListRequest;
import BsK.common.packet.res.GetRecheckUpListResponse;
import BsK.client.network.handler.ResponseListener;

import BsK.common.packet.req.AddRemindDateRequest;

public class RecheckUpDialog extends JDialog {

    private JTable recheckTable;
    private DefaultTableModel tableModel;
    private JLabel qrCodeLabel;
    private JTextField sdtDisplayField;
    private JTextArea templateArea;
    private JTextArea contentInputArea;
    private JTextArea previewMessageArea;
    private JCheckBox includeTemplateCheckbox;
    private JTextArea deepLinkTextArea;

    private int currentCheckUpId = 0;
    
    // NEW: Fields for filtering functionality
    private JComboBox<String> dateFilterComboBox;
    private String[][] allRecheckUpData; // Stores the original, unfiltered list from the server

    private final ResponseListener<GetRecheckUpListResponse> getRecheckUpListResponseListener = this::handleGetRecheckUpListResponse;

    public RecheckUpDialog(Frame parent) {
        super(parent, "Quản Lý Tái Khám", true);
        initializeDialog();
        
        // Add the response listener
        ClientHandler.addResponseListener(GetRecheckUpListResponse.class, getRecheckUpListResponseListener);
        
        // Send the initial request
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetRecheckUpListRequest());
    }
    
    @Override
    public void dispose() {
        // Clean up listeners when the dialog is closed
        ClientHandler.deleteListener(GetRecheckUpListResponse.class, getRecheckUpListResponseListener);
        super.dispose();
    }
    
    // MODIFIED: This method now stores the master data list and calls the filter method
    private void handleGetRecheckUpListResponse(GetRecheckUpListResponse response) {
        SwingUtilities.invokeLater(() -> {
            this.allRecheckUpData = response.getRecheckUpList();
            // Initially populate the table using the filter (which defaults to "All")
            filterAndDisplayTableData();
        });
    }

    private void initializeDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        splitPane.setDividerLocation(750);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);
    }

    // MODIFIED: The left panel now includes a filter panel at the top
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        leftPanel.setOpaque(false);

        // NEW: Panel for the date filter ComboBox
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(new EmptyBorder(0, 0, 10, 0)); // Bottom padding

        JLabel filterLabel = new JLabel("Lọc theo ngày tái khám: ");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        String[] filterOptions = {"Hôm nay", "Ngày mai", "Trong 2 ngày tới", "Trong 3 ngày tới", "Trong 1 tuần tới"};
        dateFilterComboBox = new JComboBox<>(filterOptions);
        dateFilterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateFilterComboBox.addActionListener(e -> filterAndDisplayTableData());

        filterPanel.add(filterLabel);
        filterPanel.add(dateFilterComboBox);
        
        leftPanel.add(filterPanel, BorderLayout.NORTH); // Add filter to the top
        leftPanel.add(createTablePanel(), BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        String[] columnNames = {"Họ và tên", "Số điện thoại", "Ngày tái khám", "Ngày nhắc", "ID"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        recheckTable = new JTable(tableModel);
        recheckTable.setRowSorter(new TableRowSorter<>(tableModel));
        
        recheckTable.getColumnModel().getColumn(4).setMinWidth(0);
        recheckTable.getColumnModel().getColumn(4).setMaxWidth(0);
        recheckTable.getColumnModel().getColumn(4).setWidth(0);

        recheckTable.setRowHeight(30);
        recheckTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        recheckTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        recheckTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recheckTable.getTableHeader().setReorderingAllowed(false);

        recheckTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleTableSelectionChange();
            }
        });

        JScrollPane scrollPane = new JScrollPane(recheckTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        return tablePanel;
    }

    // MODIFIED: Updated the "Làm mới" button's action listener to be a full refresh
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(10, 15));
        rightPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        rightPanel.setOpaque(false);

        JPanel qrPanel = new JPanel(new GridBagLayout());
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createTitledBorder("Mã QR"));
        qrPanel.setPreferredSize(new Dimension(300, 300));
        qrCodeLabel = new JLabel("Chọn bệnh nhân để tạo tin nhắn", SwingConstants.CENTER);
        qrCodeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        qrPanel.add(qrCodeLabel);
        rightPanel.add(qrPanel, BorderLayout.NORTH);

        JPanel centerContentPanel = new JPanel(new GridBagLayout());
        centerContentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        sdtDisplayField = new JTextField();
        sdtDisplayField.setEditable(false);
        sdtDisplayField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sdtDisplayField.setBackground(new Color(235, 235, 235));
        sdtDisplayField.setBorder(createTitledCompoundBorder("SĐT (từ bảng)"));
        centerContentPanel.add(sdtDisplayField, gbc);

        gbc.gridy = 1; gbc.ipady = 0;
        includeTemplateCheckbox = new JCheckBox("Bao gồm mẫu tin nhắn", true);
        includeTemplateCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        includeTemplateCheckbox.setOpaque(false);
        includeTemplateCheckbox.addActionListener(e -> updatePreview());
        centerContentPanel.add(includeTemplateCheckbox, gbc);

        gbc.gridy = 2; gbc.ipady = 20;
        templateArea = new JTextArea();
        templateArea.setEditable(false);
        templateArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        templateArea.setBackground(new Color(235, 235, 235));
        templateArea.setLineWrap(true);
        templateArea.setWrapStyleWord(true);
        JScrollPane templateScrollPane = new JScrollPane(templateArea);
        templateScrollPane.setBorder(createTitledCompoundBorder("Mẫu tin nhắn (tự động)"));
        centerContentPanel.add(templateScrollPane, gbc);

        gbc.gridy = 3; gbc.ipady = 40;
        contentInputArea = new JTextArea();
        contentInputArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        contentInputArea.setLineWrap(true);
        contentInputArea.setWrapStyleWord(true);
        contentInputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePreview(); }
        });
        JScrollPane contentScrollPane = new JScrollPane(contentInputArea);
        contentScrollPane.setBorder(createTitledCompoundBorder("Nội dung tin nhắn (tùy chọn)"));
        centerContentPanel.add(contentScrollPane, gbc);

        gbc.gridy = 4; gbc.ipady = 40;
        previewMessageArea = new JTextArea();
        previewMessageArea.setEditable(false);
        previewMessageArea.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        previewMessageArea.setBackground(new Color(240, 240, 240));
        previewMessageArea.setLineWrap(true);
        previewMessageArea.setWrapStyleWord(true);
        JScrollPane previewScrollPane = new JScrollPane(previewMessageArea);
        previewScrollPane.setBorder(createTitledCompoundBorder("Xem trước tin nhắn (tổng hợp)"));
        centerContentPanel.add(previewScrollPane, gbc);

        rightPanel.add(centerContentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        JButton resetButton = new JButton("Làm mới");
        resetButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resetButton.addActionListener(e -> {
            // This action now fully refreshes the view and data
            dateFilterComboBox.setSelectedIndex(0); // Reset filter to "All"
            recheckTable.clearSelection();
            clearRightPanel();
            // Request latest data from the server
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetRecheckUpListRequest());
        });
        JButton generateButton = new JButton("Tạo Tin & QR");
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        generateButton.addActionListener(e -> {
            generateMessageAndQr();
        });
        JButton addRemindDateButton = new JButton("Lưu ngày nhắc");
        addRemindDateButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addRemindDateButton.addActionListener(e -> {
            if (currentCheckUpId != 0) {
                NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddRemindDateRequest(String.valueOf(currentCheckUpId)));
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân.", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE);
            }
        });

        buttonPanel.add(resetButton);
        buttonPanel.add(generateButton);
        buttonPanel.add(addRemindDateButton);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        return rightPanel;
    }

    // NEW: This method filters the master data list and populates the table
    private void filterAndDisplayTableData() {
        tableModel.setRowCount(0); // Clear the visible table
        clearRightPanel();         // Clear the details panel on the right

        if (allRecheckUpData == null) {
            return; // Exit if there's no data to filter
        }

        String selectedFilter = (String) dateFilterComboBox.getSelectedItem();
        if (selectedFilter == null) {
             selectedFilter = "Hôm nay"; // Default case
        }

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (String[] row : allRecheckUpData) {
            String dateString = row[2];
            try {
                LocalDate recheckDate = LocalDate.parse(dateString, formatter);
                boolean shouldAddRow = false;

                switch (selectedFilter) {
                    case "Hôm nay":
                        shouldAddRow = recheckDate.isEqual(today);
                        break;
                    case "Ngày mai":
                        shouldAddRow = recheckDate.isEqual(today.plusDays(1));
                        break;
                    case "Trong 2 ngày tới": // includes today and tomorrow
                        shouldAddRow = !recheckDate.isBefore(today) && recheckDate.isBefore(today.plusDays(2));
                        break;
                    case "Trong 3 ngày tới":
                        shouldAddRow = !recheckDate.isBefore(today) && recheckDate.isBefore(today.plusDays(3));
                        break;
                    case "Trong 1 tuần tới":
                        shouldAddRow = !recheckDate.isBefore(today) && recheckDate.isBefore(today.plusDays(7));
                        break;
                }

                if (shouldAddRow) {
                    tableModel.addRow(row);
                }

            } catch (DateTimeParseException e) {
                System.err.println("Could not parse date string: " + dateString);
                // If the filter is "All", add rows with unparseable dates so they are not lost
                if ("Hôm nay".equals(selectedFilter)) {
                    tableModel.addRow(row);
                }
            }
        }
    }

    private int getSelectedModelRow() {
        int viewRow = recheckTable.getSelectedRow();
        if (viewRow != -1) {
            return recheckTable.convertRowIndexToModel(viewRow);
        }
        return -1;
    }

    private void handleTableSelectionChange() {
        clearRightPanel();
        int modelRow = getSelectedModelRow();

        if (modelRow != -1) {
            currentCheckUpId = Integer.parseInt((String) tableModel.getValueAt(modelRow, 4));
            String sdt = (String) tableModel.getValueAt(modelRow, 1);
            sdtDisplayField.setText(sdt);
            updatePreview();
        } else {
            currentCheckUpId = 0;
        }
    }
    
    private void updatePreview() {
        int modelRow = getSelectedModelRow();
        if (modelRow == -1) {
            templateArea.setText("");
            previewMessageArea.setText("");
            return;
        }

        String patientName = (String) tableModel.getValueAt(modelRow, 0);
        String recheckDate = (String) tableModel.getValueAt(modelRow, 2);
        String customContent = contentInputArea.getText();

        String templateText = String.format("Phòng khám Bs.Khắp xin thông báo: Anh/Chị %s có lịch tái khám vào ngày %s.", patientName, recheckDate);
        templateArea.setText(templateText);

        String fullMessage;
        if (includeTemplateCheckbox.isSelected()) {
            fullMessage = templateText + " " + customContent;
        } else {
            fullMessage = customContent;
        }
        previewMessageArea.setText(fullMessage.trim());
    }

    private void generateMessageAndQr() {
        int modelRow = getSelectedModelRow();
        if (modelRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân.", "Chưa chọn bệnh nhân", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sdt = (String) tableModel.getValueAt(modelRow, 1);
        String messageBody = previewMessageArea.getText();

        if (messageBody.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nội dung tin nhắn trống.", "Nội dung trống", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String formattedSdt = formatPhoneNumberForSms(sdt);
            String encodedBody = URLEncoder.encode(messageBody, StandardCharsets.UTF_8).replace("+", "%20");
            
            String smsDeepLink = "sms:" + formattedSdt + "?body=" + encodedBody;

            BufferedImage qrImage = generateQrCodeImage(smsDeepLink);
            Image scaledQr = qrImage.getScaledInstance(280, 280, Image.SCALE_SMOOTH);
            qrCodeLabel.setIcon(new ImageIcon(scaledQr));
            qrCodeLabel.setText("");

        } catch (WriterException e) {
            qrCodeLabel.setIcon(null);
            qrCodeLabel.setText("Lỗi tạo QR");
            JOptionPane.showMessageDialog(this, "Lỗi khi tạo mã QR.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private String formatPhoneNumberForSms(String localNumber) {
        if (localNumber != null && !localNumber.trim().isEmpty()) {
            if (localNumber.startsWith("0")) {
                return "+84" + localNumber.substring(1);
            }
        }
        return localNumber;
    }

    private BufferedImage generateQrCodeImage(String text) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300, hints);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.createGraphics();

        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (bitMatrix.get(i, j)) {
                    graphics.fillRect(i, j, 1, 1);
                }
            }
        }
        return image;
    }
    
    private void clearRightPanel() {
        qrCodeLabel.setIcon(null);
        qrCodeLabel.setText("Chọn bệnh nhân để tạo tin nhắn");
        sdtDisplayField.setText("");
        includeTemplateCheckbox.setSelected(true);
        templateArea.setText("");
        contentInputArea.setText("");
        previewMessageArea.setText("");
    }

    private CompoundBorder createTitledCompoundBorder(String title) {
        return new CompoundBorder(
                BorderFactory.createTitledBorder(null, title, 0, 0,
                        new Font("Segoe UI", Font.PLAIN, 12), new Color(60, 60, 60)),
                new EmptyBorder(2, 5, 2, 5)
        );
    }
}