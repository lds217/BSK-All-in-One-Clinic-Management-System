package BsK.client.ui.component.QueueViewPage;

import BsK.client.LocalStorage;

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
    private final String[] tvQueueColumns = {"Mã khám bệnh", "Họ và Tên", "Năm sinh"};

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
        setTitle("TV Queue Display");
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

        clinicNameLabel = new JLabel("Clinic: Loading...");
        styleLabelForTv(clinicNameLabel, 28, Font.BOLD, Color.WHITE);
        clinicAddressLabel = new JLabel("Address: Loading...");
        styleLabelForTv(clinicAddressLabel, 20, Font.PLAIN, Color.LIGHT_GRAY);
        clinicPhoneLabel = new JLabel("Phone: Loading...");
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
        room1StatusLabel = new JLabel("ROOM 1", SwingConstants.CENTER);
        styleRoomLabel(room1StatusLabel, room1Panel, Color.GREEN.darker(), "EMPTY");
        room1Panel.add(room1StatusLabel, BorderLayout.CENTER);
        room1Panel.setPreferredSize(new Dimension(220, 100));

        room2Panel = new JPanel(new BorderLayout());
        room2StatusLabel = new JLabel("ROOM 2", SwingConstants.CENTER);
        styleRoomLabel(room2StatusLabel, room2Panel, Color.GREEN.darker(), "EMPTY");
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

        nowCallingTextLabel = new JLabel("NOW CALLING:");
        styleLabelForTv(nowCallingTextLabel, 38, Font.BOLD, new Color(255, 215, 0)); // Gold color
        nowCallingTextLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nowCallingTextLabel.setVisible(false); // Initially hidden

        nowCallingInfoLine1Label = new JLabel("Room 1: FREE"); // Default message
        styleLabelForTv(nowCallingInfoLine1Label, 32, Font.BOLD, Color.WHITE);
        nowCallingInfoLine1Label.setAlignmentX(Component.CENTER_ALIGNMENT);

        nowCallingInfoLine2Label = new JLabel("Room 2: FREE"); // Default message
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

    private void styleRoomLabel(JLabel label, JPanel panel, Color bgColor, String text) {
        label.setFont(new Font("Segoe UI", Font.BOLD, 22));
        label.setForeground(Color.WHITE);
        label.setText("<html><div style='text-align: center;'>" + text.replace(" ", "<br>") + "</div></html>");
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
    }

    private void loadClinicInfo() {
        if (LocalStorage.ClinicName != null) {
            clinicNameLabel.setText("Clinic: " + LocalStorage.ClinicName);
            clinicAddressLabel.setText("Address: " + LocalStorage.ClinicAddress);
            clinicPhoneLabel.setText("Phone: " + LocalStorage.ClinicPhone);
        }
    }

    private void updateNowCallingDisplay() {
        boolean room1Busy = room1PatientInfo != null;
        boolean room2Busy = room2PatientInfo != null;

        if (room1Busy) {
            nowCallingInfoLine1Label.setText("Room 1: " + room1PatientInfo);
            styleLabelForTv(nowCallingInfoLine1Label, 32, Font.BOLD, Color.WHITE);
        } else {
            nowCallingInfoLine1Label.setText("Room 1: FREE");
            styleLabelForTv(nowCallingInfoLine1Label, 32, Font.ITALIC, Color.LIGHT_GRAY);
        }

        if (room2Busy) {
            nowCallingInfoLine2Label.setText("Room 2: " + room2PatientInfo);
            styleLabelForTv(nowCallingInfoLine2Label, 32, Font.BOLD, Color.WHITE);
        } else {
            nowCallingInfoLine2Label.setText("Room 2: FREE");
            styleLabelForTv(nowCallingInfoLine2Label, 32, Font.ITALIC, Color.LIGHT_GRAY);
        }

        if (room1Busy || room2Busy) {
            nowCallingTextLabel.setVisible(true);
        } else {
            nowCallingTextLabel.setVisible(false);
            // Optional: could make one line say "All rooms free" and hide the other
            // For now, both lines will show "Room X: FREE" and title is hidden.
        }
    }

    public void updateSpecificRoomStatus(int roomId, String patientIdForRoomBox, String fullPatientInfoForCentralDisplay, BsK.common.entity.Status status) {
        JLabel targetLabel;
        JPanel targetPanel;

        if (roomId == 1) {
            targetLabel = room1StatusLabel;
            targetPanel = room1Panel;
        } else if (roomId == 2) {
            targetLabel = room2StatusLabel;
            targetPanel = room2Panel;
        } else {
            System.err.println("QueueViewPage: Invalid roomId for status update: " + roomId);
            return;
        }

        if (status == BsK.common.entity.Status.PROCESSING) {
            // Use patientIdForRoomBox for the small room panel
            styleRoomLabel(targetLabel, targetPanel, Color.RED.darker(), "P" + roomId + "\n" + patientIdForRoomBox);
            // Store fullPatientInfoForCentralDisplay for the main "Now Calling" display
            if (roomId == 1) {
                room1PatientInfo = fullPatientInfoForCentralDisplay;
            } else {
                room2PatientInfo = fullPatientInfoForCentralDisplay;
            }
        } else {
            // Room is not PROCESSING (e.g., EMPTY, DONE)
            styleRoomLabel(targetLabel, targetPanel, Color.GREEN.darker(), "ROOM " + roomId + "\nEMPTY");
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

        if (roomId == 1) {
            targetLabel = room1StatusLabel;
            targetPanel = room1Panel;
        } else if (roomId == 2) {
            targetLabel = room2StatusLabel;
            targetPanel = room2Panel;
        } else {
            System.err.println("QueueViewPage: Invalid roomId for marking as free: " + roomId);
            return;
        }
        styleRoomLabel(targetLabel, targetPanel, Color.GREEN.darker(), "ROOM " + roomId + "\nEMPTY");
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
            styleRoomLabel(room1StatusLabel, room1Panel, Color.GREEN.darker(), "ROOM 1 EMPTY");
            styleRoomLabel(room2StatusLabel, room2Panel, Color.GREEN.darker(), "ROOM 2 EMPTY");
            return;
        }

        List<Object[]> tvDataList = new ArrayList<>();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yyyy");

        for (String[] row : fullQueueData) {
            // Assuming row[0] is MaKhamBenh, row[1] is Date for year, row[2] is Ho, row[3] is Ten
            // As per user: "Mã khám bệnh (the first collum and last and first nam (concate index 2 and 3 (3rd and 4th collumn)) and take the year out from the second column))"
            if (row.length > 3) { 
                String maKhamBenh = row[0];
                String ho = row[2];
                String ten = row[3];
                String hoVaTen = ho + " " + ten;
                String dateStr = row[1]; // Second column for date for year extraction
                String namSinh = "N/A"; // Or Nam Kham as discussed

                try {
                    Date parsedDate;
                    if (dateStr.matches("\\d+")) { // Unix timestamp
                        parsedDate = new Date(Long.parseLong(dateStr));
                    } else { // dd/MM/yyyy format
                        parsedDate = inputDateFormat.parse(dateStr);
                    }
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(parsedDate);
                    namSinh = String.valueOf(calendar.get(Calendar.YEAR));
                } catch (ParseException | NumberFormatException e) {
                    System.err.println("Error parsing date for TV Queue year: " + dateStr + " - " + e.getMessage());
                }
                tvDataList.add(new Object[]{maKhamBenh, hoVaTen, namSinh});
            }
        }
        tvQueueTableModel.setDataVector(tvDataList.toArray(new Object[0][0]), tvQueueColumns);

        // Update Room 1 Status - This part will be superseded by updateSpecificRoomStatus or needs to be re-evaluated
        // if (fullQueueData.length > 0 && fullQueueData[0] != null && fullQueueData[0].length > 0) {
        //     styleRoomLabel(room1StatusLabel, room1Panel, Color.RED.darker(), "ROOM 1 " + fullQueueData[0][0]);
        // } else {
        //     styleRoomLabel(room1StatusLabel, room1Panel, Color.GREEN.darker(), "ROOM 1 EMPTY");
        // }

        // Update Room 2 Status - This part will be superseded by updateSpecificRoomStatus or needs to be re-evaluated
        // if (fullQueueData.length > 1 && fullQueueData[1] != null && fullQueueData[1].length > 0) {
        //     styleRoomLabel(room2StatusLabel, room2Panel, Color.RED.darker(), "ROOM 2 " + fullQueueData[1][0]);
        // } else {
        //     styleRoomLabel(room2SṭatusLabel, room2Panel, Color.GREEN.darker(), "ROOM 2 EMPTY");
        // }
    }

    @Override
    public void setVisible(boolean b) {
        if (b) { // When making visible, reload clinic info in case it changed or was not available at init
            loadClinicInfo();
        }
        super.setVisible(b);
    }

    // Optional: A method to make the frame more TV-friendly (e.g., full screen)
    public void optimizeForTv() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        // setUndecorated(true); // Optional: removes window borders, use with caution
    }
} 