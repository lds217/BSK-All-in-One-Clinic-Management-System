package BsK.client.ui.component.QueueViewPage;

import BsK.client.LocalStorage;
import BsK.common.util.date.DateUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class QueueViewPage extends JFrame {
    private DefaultTableModel tvQueueTableModel;
    private JTable tvQueueTable;
    private final String[] tvQueueColumns = {"STT", "Họ và Tên", "Năm sinh"};

    private JLabel clinicNameLabel;
    private JLabel clinicAddressLabel;
    private JLabel clinicPhoneLabel;

    private JLabel room1StatusLabel;
    private JLabel room2StatusLabel;
    private JPanel room1Panel;
    private JPanel room2Panel;

    private JPanel callingPatientDisplayPanel;
    private JLabel nowCallingTextLabel;
    private JLabel nowCallingInfoLine1Label;
    private JLabel nowCallingInfoLine2Label;

    private String room1PatientInfo = null; // Stores "Name (Year)"
    private String room2PatientInfo = null; // Stores "Name (Year)"

    public QueueViewPage() {
        setTitle("Màn hình chờ TV");
        setSize(1280, 720); // Common 720p resolution for TVs
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 10)); // Main layout with vertical gap
        getContentPane().setBackground(Color.DARK_GRAY); // Dark background for TV

        // --- Top Panel --- 
        JPanel topPanel = new JPanel(new BorderLayout(20, 0)); // Hgap for spacing
        topPanel.setOpaque(false); // Transparent to show frame background
        topPanel.setBorder(new EmptyBorder(20, 20, 10, 20)); // Padding

        // Clinic Info Panel (Top-Left)
        JPanel clinicInfoPanel = new JPanel();
        clinicInfoPanel.setLayout(new BoxLayout(clinicInfoPanel, BoxLayout.Y_AXIS));
        clinicInfoPanel.setOpaque(false);

        clinicNameLabel = new JLabel("Phòng khám: Đang tải...");
        styleLabelForTv(clinicNameLabel, 28, Font.BOLD, Color.WHITE);
        clinicAddressLabel = new JLabel("Địa chỉ: Đang tải...");
        styleLabelForTv(clinicAddressLabel, 20, Font.PLAIN, Color.LIGHT_GRAY);
        clinicPhoneLabel = new JLabel("Điện thoại: Đang tải...");
        styleLabelForTv(clinicPhoneLabel, 20, Font.PLAIN, Color.LIGHT_GRAY);

        clinicInfoPanel.add(clinicNameLabel);
        clinicInfoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        clinicInfoPanel.add(clinicAddressLabel);
        clinicInfoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        clinicInfoPanel.add(clinicPhoneLabel);
        loadClinicInfo(); // Load data from LocalStorage

        topPanel.add(clinicInfoPanel, BorderLayout.WEST);

        // Room Status Panel (Top-Right)
        JPanel roomsOuterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); // Align to right
        roomsOuterPanel.setOpaque(false);
        JPanel roomsPanel = new JPanel(new GridLayout(1, 2, 20, 0)); // 1 row, 2 cols, hgap
        roomsPanel.setOpaque(false);

        room1Panel = new JPanel(new BorderLayout());
        room1StatusLabel = new JLabel("PHÒNG 1", SwingConstants.CENTER);
        styleRoomLabel(room1StatusLabel, room1Panel, Color.GREEN.darker(), "TRỐNG"); // Initial state: Room 1 Empty
        room1Panel.add(room1StatusLabel, BorderLayout.CENTER);
        room1Panel.setPreferredSize(new Dimension(220, 100));

        room2Panel = new JPanel(new BorderLayout());
        room2StatusLabel = new JLabel("PHÒNG 2", SwingConstants.CENTER);
        styleRoomLabel(room2StatusLabel, room2Panel, Color.GREEN.darker(), "TRỐNG"); // Initial state: Room 2 Empty
        room2Panel.add(room2StatusLabel, BorderLayout.CENTER);
        room2Panel.setPreferredSize(new Dimension(220, 100));

        roomsPanel.add(room1Panel);
        roomsPanel.add(room2Panel);
        roomsOuterPanel.add(roomsPanel);
        topPanel.add(roomsOuterPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- "Now Calling" Display Panel (NEW) ---
        callingPatientDisplayPanel = new JPanel();
        callingPatientDisplayPanel.setLayout(new BoxLayout(callingPatientDisplayPanel, BoxLayout.Y_AXIS)); // Vertical alignment
        callingPatientDisplayPanel.setOpaque(false); // Transparent background
        callingPatientDisplayPanel.setBorder(new EmptyBorder(15, 20, 15, 20)); // Padding

        nowCallingTextLabel = new JLabel("ĐANG GỌI:");
        styleLabelForTv(nowCallingTextLabel, 38, Font.BOLD, new Color(255, 215, 0)); // Gold color
        nowCallingTextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nowCallingTextLabel.setVisible(false); // Initially hidden

        nowCallingInfoLine1Label = new JLabel("Phòng 1: TRỐNG"); // Default message
        styleLabelForTv(nowCallingInfoLine1Label, 32, Font.BOLD, Color.WHITE);
        nowCallingInfoLine1Label.setAlignmentX(Component.CENTER_ALIGNMENT);

        nowCallingInfoLine2Label = new JLabel("Phòng 2: TRỐNG"); // Default message
        styleLabelForTv(nowCallingInfoLine2Label, 32, Font.BOLD, Color.WHITE);
        nowCallingInfoLine2Label.setAlignmentX(Component.CENTER_ALIGNMENT);

        callingPatientDisplayPanel.add(nowCallingTextLabel);
        callingPatientDisplayPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
        callingPatientDisplayPanel.add(nowCallingInfoLine1Label);
        callingPatientDisplayPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
        callingPatientDisplayPanel.add(nowCallingInfoLine2Label);

        // --- Center Panel to hold "Now Calling" and Table ---
        JPanel centerContentPanel = new JPanel(new BorderLayout(0, 0));
        centerContentPanel.setOpaque(false);
        centerContentPanel.add(callingPatientDisplayPanel, BorderLayout.NORTH);

        // --- Queue Table Panel (Center/Bottom) ---
        tvQueueTableModel = new DefaultTableModel(new Object[][]{}, tvQueueColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tvQueueTable = new JTable(tvQueueTableModel);

        tvQueueTable.setFont(new Font("Arial", Font.BOLD, 26));
        tvQueueTable.setRowHeight(45);
        tvQueueTable.setFillsViewportHeight(true);
        tvQueueTable.setBackground(new Color(220, 220, 220)); // Light gray for table background
        tvQueueTable.setGridColor(Color.GRAY);

        JTableHeader tableHeader = tvQueueTable.getTableHeader();
        tableHeader.setFont(new Font("Arial", Font.BOLD, 28));
        tableHeader.setBackground(new Color(63, 81, 181));
        tableHeader.setForeground(Color.WHITE);
        tableHeader.setPreferredSize(new Dimension(0, 50)); // Header height

        JScrollPane scrollPane = new JScrollPane(tvQueueTable);
        scrollPane.setBorder(new EmptyBorder(10, 20, 20, 20)); // Padding around table
        scrollPane.getViewport().setBackground(Color.DARK_GRAY); // Match frame background
        centerContentPanel.add(scrollPane, BorderLayout.CENTER); // Add table to center of centerContentPanel
        add(centerContentPanel, BorderLayout.CENTER); // Add centerContentPanel to main frame's center
    }

    private void styleLabelForTv(JLabel label, int size, int style, Color color) {
        label.setFont(new Font("Segoe UI", style, size)); // Segoe UI is a common nice font
        label.setForeground(color);
    }

    private void styleRoomLabel(JLabel label, JPanel panel, Color bgColor, String statusText) {
        // The label's main text (PHÒNG 1 or PHÒNG 2) is set at initialization and doesn't change.
        // This method now only updates the status part of the text and background color.
        String baseRoomText = label.getText().split("<br>")[0]; // Get "PHÒNG X"
        if (baseRoomText.contains("html")) baseRoomText = (label == room1StatusLabel ? "PHÒNG 1" : "PHÒNG 2"); // Fallback if parsing fails
        
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setForeground(Color.WHITE);
        label.setText("<html><div style='text-align: center;'>" + baseRoomText + "<br>" + statusText + "</div></html>");
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
    }

    private void loadClinicInfo() {
        if (LocalStorage.ClinicName != null) {
            clinicNameLabel.setText("Phòng khám: " + LocalStorage.ClinicName);
            clinicAddressLabel.setText("Địa chỉ: " + LocalStorage.ClinicAddress);
            clinicPhoneLabel.setText("Điện thoại: " + LocalStorage.ClinicPhone);
        } else {
            clinicNameLabel.setText("Phòng khám: Đang tải...");
            clinicAddressLabel.setText("Địa chỉ: Đang tải...");
            clinicPhoneLabel.setText("Điện thoại: Đang tải...");
        }
    }

    private void updateNowCallingDisplay() {
        boolean room1Busy = room1PatientInfo != null;
        boolean room2Busy = room2PatientInfo != null;

        if (room1Busy) {
            nowCallingInfoLine1Label.setText("Phòng 1: " + room1PatientInfo);
            styleLabelForTv(nowCallingInfoLine1Label, 32, Font.BOLD, Color.WHITE);
        } else {
            nowCallingInfoLine1Label.setText("Phòng 1: TRỐNG");
            styleLabelForTv(nowCallingInfoLine1Label, 32, Font.ITALIC, Color.LIGHT_GRAY);
        }

        if (room2Busy) {
            nowCallingInfoLine2Label.setText("Phòng 2: " + room2PatientInfo);
            styleLabelForTv(nowCallingInfoLine2Label, 32, Font.BOLD, Color.WHITE);
        } else {
            nowCallingInfoLine2Label.setText("Phòng 2: TRỐNG");
            styleLabelForTv(nowCallingInfoLine2Label, 32, Font.ITALIC, Color.LIGHT_GRAY);
        }

        if (room1Busy || room2Busy) {
            nowCallingTextLabel.setVisible(true);
        } else {
            nowCallingTextLabel.setVisible(false);
        }
    }

    public void updateSpecificRoomStatus(int roomId, String patientIdForRoomBox, String queueNumberForRoomBox, String fullPatientInfoForCentralDisplay, BsK.common.entity.Status status) {
        JLabel targetLabel;
        JPanel targetPanel;
        String roomName;

        if (roomId == 1) {
            targetLabel = room1StatusLabel;
            targetPanel = room1Panel;
            roomName = "PHÒNG 1";
        } else if (roomId == 2) {
            targetLabel = room2StatusLabel;
            targetPanel = room2Panel;
            roomName = "PHÒNG 2";
        } else {
            System.err.println("QueueViewPage: Invalid roomId for status update: " + roomId);
            return;
        }

        if (status == BsK.common.entity.Status.PROCESSING) {
            styleRoomLabel(targetLabel, targetPanel, Color.RED.darker(), "STT: " + queueNumberForRoomBox);
            if (roomId == 1) {
                room1PatientInfo = fullPatientInfoForCentralDisplay;
            } else {
                room2PatientInfo = fullPatientInfoForCentralDisplay;
            }
        } else { // EMPTY, DONE, etc.
            styleRoomLabel(targetLabel, targetPanel, Color.GREEN.darker(), "TRỐNG");
            if (roomId == 1) {
                room1PatientInfo = null;
            } else {
                room2PatientInfo = null;
            }
        }
        updateNowCallingDisplay();
    }

    public void markRoomAsFree(int roomId) {
        JLabel targetLabel;
        JPanel targetPanel;
        String roomName; // Not strictly needed here if styleRoomLabel handles base text

        if (roomId == 1) {
            targetLabel = room1StatusLabel;
            targetPanel = room1Panel;
            // roomName = "PHÒNG 1"; // Base text is already PHÒNG 1
        } else if (roomId == 2) {
            targetLabel = room2StatusLabel;
            targetPanel = room2Panel;
            // roomName = "PHÒNG 2"; // Base text is already PHÒNG 2
        } else {
            System.err.println("QueueViewPage: Invalid roomId for marking as free: " + roomId);
            return;
        }
        styleRoomLabel(targetLabel, targetPanel, Color.GREEN.darker(), "TRỐNG");
        if (roomId == 1) {
            room1PatientInfo = null;
        } else {
            room2PatientInfo = null;
        }
        updateNowCallingDisplay();
    }

    public void updateQueueData(String[][] fullQueueData) {
        if (fullQueueData == null) {
            tvQueueTableModel.setRowCount(0);
            // Reset room labels to their base text + TRỐNG
            styleRoomLabel(room1StatusLabel, room1Panel, Color.GREEN.darker(), "TRỐNG");
            styleRoomLabel(room2StatusLabel, room2Panel, Color.GREEN.darker(), "TRỐNG");
            room1PatientInfo = null;
            room2PatientInfo = null;
            updateNowCallingDisplay();
            return;
        }

        List<Object[]> tvDataList = new ArrayList<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy");

        for (String[] row : fullQueueData) {
            if (row.length > 24) { // Ensure we have all data including queue number
                String queueNumber = row[24]; // Use the new queue number from index 24
                String ho = row[2];
                String ten = row[3];
                String hoVaTen = ho + " " + ten;
                String customerDob = row[15]; // Use customer DOB instead of checkup date
                String namSinh = "N/A"; 

                if (customerDob != null && !customerDob.isEmpty()) {
                    // Extract year from DOB using DateUtils for consistency
                    int year = DateUtils.extractYearFromTimestamp(customerDob);
                    if (year != -1) {
                        namSinh = String.valueOf(year);
                    } else {
                        // Fallback: try to parse as date string and extract year
                        try {
                            Date dobDate = DateUtils.convertToDate(customerDob);
                            if (dobDate != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(dobDate);
                                namSinh = String.valueOf(calendar.get(Calendar.YEAR));
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi phân tích ngày sinh cho năm trên màn hình chờ TV: " + customerDob + " - " + e.getMessage());
                        }
                    }
                }
                tvDataList.add(new Object[]{queueNumber, hoVaTen, namSinh});
            }
        }
        tvQueueTableModel.setDataVector(tvDataList.toArray(new Object[0][0]), tvQueueColumns);

        // The specific room status (patient ID in box) is now handled by updateSpecificRoomStatus
        // and markRoomAsFree. The general updateQueueData should not override this detailed status.
        // If no patients are called, updateNowCallingDisplay will reflect that rooms are free if their info is null.
    }

    @Override
    public void setVisible(boolean b) {
        if (b) { 
            loadClinicInfo();
        }
        super.setVisible(b);
    }

    public void optimizeForTv() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        // setUndecorated(true); // Optional: removes window borders, use with caution
    }
} 