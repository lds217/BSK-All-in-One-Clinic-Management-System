package BsK.client.ui.component.DataDialog;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.common.entity.DoctorItem;
import BsK.common.packet.req.GetExportDataRequest;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.date.DateUtils; // Added import
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class ExcelExportDialog extends JDialog {

    private JDatePickerImpl fromDatePicker;
    private JDatePickerImpl toDatePicker;
    private JComboBox<String> doctorComboBox;
    private JCheckBox includeMedicineCheckbox;
    private JCheckBox includeServiceCheckbox;
    private JButton exportButton;
    private JButton cancelButton;

    // Callback interface for when export is confirmed
    public interface ExportCallback {
        void onExportConfirmed(long fromTimestamp, long toTimestamp, Integer doctorId, 
                               boolean includeMedicine, boolean includeService, File exportFile);
    }

    private ExportCallback callback;

    public ExcelExportDialog(JDialog parent, ExportCallback callback) {
        super(parent, "Cấu Hình Xuất Excel", true);
        this.callback = callback;
        initializeDialog();
    }

    private void initializeDialog() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(500, 460); // Increased height slightly
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("Cấu Hình Xuất Dữ Liệu Excel");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = createFormPanel();
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Tùy Chọn Xuất"),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5); // Increased vertical spacing
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Arial", Font.BOLD, 13);
        Font textFont = new Font("Arial", Font.PLAIN, 13);

        // --- Row 0: From Date ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel fromLabel = new JLabel("Từ ngày:");
        fromLabel.setFont(labelFont);
        formPanel.add(fromLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        fromDatePicker = createDatePicker();
        // Remove fixed preferred size to let layout manager handle it
        // Set default to first day of current month
        Calendar cal = Calendar.getInstance(DateUtils.VIETNAM_TIMEZONE);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        fromDatePicker.getModel().setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        fromDatePicker.getModel().setSelected(true);
        formPanel.add(fromDatePicker, gbc);

        // --- Row 1: To Date ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel toLabel = new JLabel("Đến ngày:");
        toLabel.setFont(labelFont);
        formPanel.add(toLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        toDatePicker = createDatePicker();
        // Set default to today
        Calendar today = Calendar.getInstance(DateUtils.VIETNAM_TIMEZONE);
        toDatePicker.getModel().setDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        toDatePicker.getModel().setSelected(true);
        formPanel.add(toDatePicker, gbc);

        // --- Add Date Validation Logic ---
        fromDatePicker.getModel().addChangeListener(e -> {
            Date fromDate = (Date) fromDatePicker.getModel().getValue();
            Date toDate = (Date) toDatePicker.getModel().getValue();
            
            if (fromDate != null && toDate != null && fromDate.after(toDate)) {
                // If From Date > To Date, set To Date = From Date
                Calendar c = Calendar.getInstance(DateUtils.VIETNAM_TIMEZONE);
                c.setTime(fromDate);
                toDatePicker.getModel().setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            }
        });

        toDatePicker.getModel().addChangeListener(e -> {
            Date fromDate = (Date) fromDatePicker.getModel().getValue();
            Date toDate = (Date) toDatePicker.getModel().getValue();
            
            if (fromDate != null && toDate != null && toDate.before(fromDate)) {
                // If To Date < From Date, set From Date = To Date
                Calendar c = Calendar.getInstance(DateUtils.VIETNAM_TIMEZONE);
                c.setTime(toDate);
                fromDatePicker.getModel().setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            }
        });

        // --- Row 2: Doctor Filter ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel doctorLabel = new JLabel("Bác sĩ:");
        doctorLabel.setFont(labelFont);
        formPanel.add(doctorLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        doctorComboBox = new JComboBox<>();
        doctorComboBox.setFont(textFont);
        doctorComboBox.addItem("Tất cả");
        if (LocalStorage.doctorsName != null) {
            for (DoctorItem doctor : LocalStorage.doctorsName) {
                doctorComboBox.addItem(doctor.getName());
            }
        }
        // Consistent height with date picker
        doctorComboBox.setPreferredSize(new Dimension(doctorComboBox.getPreferredSize().width, 30)); 
        formPanel.add(doctorComboBox, gbc);

        // --- Row 3: Include Medicine Checkbox ---
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5); // Add extra space before checkboxes
        includeMedicineCheckbox = new JCheckBox("Bao gồm thông tin thuốc (đơn thuốc)");
        includeMedicineCheckbox.setFont(textFont);
        includeMedicineCheckbox.setSelected(false);
        formPanel.add(includeMedicineCheckbox, gbc);

        // --- Row 4: Include Service Checkbox ---
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 5, 5, 5);
        includeServiceCheckbox = new JCheckBox("Bao gồm thông tin dịch vụ");
        includeServiceCheckbox.setFont(textFont);
        includeServiceCheckbox.setSelected(false);
        formPanel.add(includeServiceCheckbox, gbc);

        return formPanel;
    }

    private JDatePickerImpl createDatePicker() {
        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.month", "Tháng");
        p.put("text.year", "Năm");
        p.put("text.today", "Hôm nay");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        
        // Improve styling of the text field inside date picker
        JFormattedTextField textField = datePicker.getJFormattedTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 13));
        textField.setPreferredSize(new Dimension(150, 30)); // Consistent height
        
        setupDateFieldForDirectInput(datePicker);
        return datePicker;
    }

    private void setupDateFieldForDirectInput(JDatePickerImpl datePicker) {
        JFormattedTextField textField = datePicker.getJFormattedTextField();
        textField.setEditable(true);

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
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        exportButton = new JButton("Xuất Excel");
        exportButton.setFont(new Font("Arial", Font.BOLD, 14));
        exportButton.setBackground(new Color(66, 157, 21));
        exportButton.setForeground(Color.WHITE);
        exportButton.setPreferredSize(new Dimension(140, 40));
        exportButton.setFocusPainted(false);
        exportButton.addActionListener(e -> handleExport());

        cancelButton = new JButton("Hủy");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(exportButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    private void handleExport() {
        // Validate dates
        Date fromDate = (Date) fromDatePicker.getModel().getValue();
        Date toDate = (Date) toDatePicker.getModel().getValue();

        if (fromDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày bắt đầu.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (toDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày kết thúc.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (fromDate.after(toDate)) {
            JOptionPane.showMessageDialog(this, "Ngày bắt đầu không thể sau ngày kết thúc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Calculate timestamps
        Calendar cal = Calendar.getInstance(DateUtils.VIETNAM_TIMEZONE);
        cal.setTime(fromDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long fromTimestamp = cal.getTimeInMillis();

        cal.setTime(toDate);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long toTimestamp = cal.getTimeInMillis();

        // Get doctor ID
        String selectedDoctor = (String) doctorComboBox.getSelectedItem();
        Integer doctorId = null;

        if (selectedDoctor != null && !selectedDoctor.equals("Tất cả")) {
            if (LocalStorage.doctorsName != null) {
                for (DoctorItem doctor : LocalStorage.doctorsName) {
                    if (doctor.getName().equals(selectedDoctor)) {
                        try {
                            doctorId = Integer.parseInt(doctor.getId());
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid doctor ID format: " + doctor.getId());
                        }
                        break;
                    }
                }
            }
        }

        // Show file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu file Excel");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        
        // Generate default filename with date range
        String fromDateStr = String.format("%td%tm%tY", fromDate, fromDate, fromDate);
        String toDateStr = String.format("%td%tm%tY", toDate, toDate, toDate);
        fileChooser.setSelectedFile(new File("DanhSachKhamBenh_" + fromDateStr + "_" + toDateStr + ".xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File exportFile = fileChooser.getSelectedFile();
            if (!exportFile.getName().toLowerCase().endsWith(".xlsx")) {
                exportFile = new File(exportFile.getParentFile(), exportFile.getName() + ".xlsx");
            }

            boolean includeMedicine = includeMedicineCheckbox.isSelected();
            boolean includeService = includeServiceCheckbox.isSelected();

            // Store export settings via callback
            if (callback != null) {
                callback.onExportConfirmed(fromTimestamp, toTimestamp, doctorId, includeMedicine, includeService, exportFile);
            }

            // Send request to backend
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetExportDataRequest(fromTimestamp, toTimestamp, doctorId, includeMedicine, includeService));
            
            // Show loading message
            JOptionPane.showMessageDialog(this, 
                "Đang tải dữ liệu từ máy chủ...\nVui lòng đợi.", 
                "Thông báo", 
                JOptionPane.INFORMATION_MESSAGE);

            dispose();
        }
    }
}
