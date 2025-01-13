package BsK.client.ui.component.CheckUpPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.CheckUpPage.MedicineDialog.MedicineDialog;
import BsK.client.ui.component.CheckUpPage.PrintDialog.MedicineInvoice;
import BsK.client.ui.component.CheckUpPage.ServiceDialog.ServiceDialog;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.common.packet.req.GetCheckUpQueueRequest;
import BsK.common.packet.req.GetCustomerHistoryRequest;
import BsK.common.packet.req.GetDoctorGeneralInfo;
import BsK.common.packet.res.ErrorResponse;
import BsK.common.packet.res.GetCheckUpQueueResponse;
import BsK.common.packet.res.GetCustomerHistoryResponse;
import BsK.common.packet.res.GetDoctorGeneralInfoResponse;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

@Slf4j
public class CheckUpPage extends JPanel {
    private String[][] queue;
    private String[][] history;
    private DefaultTableModel model, historyModel;
    private JTable table1, historyTable;
    private final ResponseListener<GetDoctorGeneralInfoResponse> doctorGeneralInfoListener = this::handleGetDoctorGeneralInfoResponse;
    private final ResponseListener<GetCheckUpQueueResponse> checkUpQueueListener = this::handleGetCheckUpQueueResponse;
    private final ResponseListener<GetCustomerHistoryResponse> customerHistoryListener = this::handleGetCustomerHistoryResponse;
    private JTextField checkupIdField, customerLastNameField, customerFirstNameField,customerAddressField, customerPhoneField, customerIdField;
    private JTextArea symptomsField, diagnosisField, notesField;
    private JComboBox<String> doctorComboBox, statusComboBox, genderComboBox;
    private JSpinner customerWeightSpinner, customerHeightSpinner;
    private JDatePickerImpl datePicker, dobPicker;
    private String[][] medicinePrescription;
    private String[][] servicePrescription;
    private String[] doctorOptions;
    private MedicineDialog medDialog = null;
    private ServiceDialog serDialog = null;
    private int previousSelectedRow = -1;
    private boolean saved = false;
    boolean returnCell = false;
    public void updateQueue() {
        ClientHandler.addResponseListener(GetCheckUpQueueResponse.class, checkUpQueueListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueRequest());
    }

    public void getDoctors() {
        ClientHandler.addResponseListener(GetDoctorGeneralInfoResponse.class, doctorGeneralInfoListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDoctorGeneralInfo());
    }

    public CheckUpPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        // add history listener
        ClientHandler.addResponseListener(GetCustomerHistoryResponse.class, customerHistoryListener);

        updateQueue();
        getDoctors();
        // Sidebar panel
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                int width = getWidth();
                int height = getHeight();
                Color color1 = new Color(63, 81, 181);
                Color color2 = new Color(33, 150, 243);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, width, height);
            }
        };
        sidebar.setBackground(new Color(63, 81, 181));
        sidebar.setLayout(new GridLayout(15, 1));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] sidebarItems = {"Thống kê", "Thăm khám", "Dữ liệu bệnh nhân", "Kho", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "UserPage", "InfoPage"};
        for (int i = 0; i < sidebarItems.length; i++) {
            String item = sidebarItems[i];
            String dest = destination[i];
            JLabel label = new JLabel(item);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Change cursor to hand
            label.setFont(new Font("Arial", Font.BOLD, 14));

            // Add a mouse listener to handle click events
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Handle the click event
                    mainFrame.showPage(dest);
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    label.setForeground(new Color(200, 230, 255)); // Highlight on hover
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    label.setForeground(Color.WHITE); // Restore original color
                }
            });

            sidebar.add(label);
        }



        // Make the sidebar scrollable
        JScrollPane sidebarScrollPane = new JScrollPane(sidebar);
        sidebarScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        sidebarScrollPane.getViewport().setOpaque(false);
        sidebarScrollPane.setOpaque(false);

        // Topbar panel
        JPanel topbar = new JPanel();
        topbar.setLayout(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Check Up");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        topbar.add(title, BorderLayout.WEST);

        JLabel userInfo = new JLabel("Welcome, " + LocalStorage.username);
        userInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        topbar.add(userInfo, BorderLayout.EAST);

        // Data table inside a RoundedPanel
        RoundedPanel leftPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightTopPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightBottomPanel = new RoundedPanel(20, Color.WHITE, false);


        JLabel titleText1 = new JLabel();
        titleText1.setText("Check Up Queue 1");
        titleText1.setFont(new Font("Arial", Font.BOLD, 16));
        titleText1.setBackground(Color.WHITE);
        titleText1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding


        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(titleText1, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        rightBottomPanel.setLayout(new BorderLayout());
        rightBottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        String[] columns = {"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Triệu chứng", "Chẩn đoán", "Ghi chú", "Trạng thái"};
        this.queue = new String[][]{}; // Initialize with empty data

        model = new DefaultTableModel(this.queue, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // All cells are non-editable
            }
        };
        table1 = new JTable(model);

        // Set preferred size for the table
        table1.setPreferredScrollableViewportSize(new Dimension(400, 200));

        // Customize the font for the table header and cells
        table1.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table1.setFont(new Font("Arial", Font.PLAIN, 12));
        table1.setRowHeight(25);

        JScrollPane tableScroll1 = new JScrollPane(table1);
        tableScroll1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        leftPanel.add(tableScroll1, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add some padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;


        JLabel patientInfoLabel = new JLabel("Thông tin bệnh nhân");
        patientInfoLabel.setFont(patientInfoLabel.getFont().deriveFont(Font.BOLD, 16)); // Bold, size 16
        patientInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridwidth = 4;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        inputPanel.add(patientInfoLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Họ"), gbc);
        gbc.gridx = 1;
        customerLastNameField = new JTextField(5);
        inputPanel.add(customerLastNameField, gbc);
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Tên"), gbc);
        gbc.gridx = 3;
        customerFirstNameField = new JTextField(5);
        inputPanel.add(customerFirstNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Giới tính"), gbc);
        gbc.gridx = 1;
        String[] genderOptions = {"Nam", "Nữ"};
        genderComboBox = new JComboBox<>(genderOptions);
        inputPanel.add(genderComboBox, gbc);
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Ngày sinh"), gbc);
        gbc.gridx = 3;
        UtilDateModel dobModel = new UtilDateModel();
        Properties dobProperties = new Properties();
        dobProperties.put("text.today", "Today");
        dobProperties.put("text.month", "Month");
        dobProperties.put("text.year", "Year");
        JDatePanelImpl dobPanel = new JDatePanelImpl(dobModel, dobProperties);
        dobPicker = new JDatePickerImpl(dobPanel, new DateLabelFormatter());
        dobPicker.setPreferredSize(new Dimension(100, 30));
        inputPanel.add(dobPicker, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Địa chỉ"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        customerAddressField = new JTextField();
        inputPanel.add(customerAddressField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Mã số bệnh nhân"), gbc);
        customerIdField = new JTextField();

        gbc.gridx = 1;
        inputPanel.add(customerIdField, gbc);

        gbc.gridx = 2;
        inputPanel.add(new JLabel("Số điện thoại"), gbc);

        gbc.gridx = 3;
        customerPhoneField = new JTextField();
        inputPanel.add(customerPhoneField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Cân nặng"), gbc);
        gbc.gridx = 1;
        SpinnerModel weightModel = new SpinnerNumberModel(60, 0, 300, 0.5);
        customerWeightSpinner = new JSpinner(weightModel);
        inputPanel.add(customerWeightSpinner, gbc);
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Chiều cao"), gbc);
        gbc.gridx = 3;
        SpinnerModel heightModel = new SpinnerNumberModel(170, 0, 230, 0.5);
        customerHeightSpinner = new JSpinner(heightModel);
        inputPanel.add(customerHeightSpinner, gbc);



        JLabel checkupInfoLabel = new JLabel("Thông tin khám bệnh");
        checkupInfoLabel.setFont(checkupInfoLabel.getFont().deriveFont(Font.BOLD, 16)); // Bold, size 16
        checkupInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridwidth = 4;
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        inputPanel.add(checkupInfoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        inputPanel.add(new JLabel("Mã khám bệnh:"), gbc);

        gbc.gridx = 1;
        checkupIdField = new JTextField(5);
        checkupIdField.setEditable(false);
        inputPanel.add(checkupIdField, gbc);

        UtilDateModel model = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());
        datePicker.setPreferredSize(new Dimension(150, 30));

        gbc.gridx = 2;
        inputPanel.add(new JLabel("Đơn Ngày:"), gbc);
        gbc.gridwidth = 0;
        gbc.gridx = 3;
        inputPanel.add(datePicker, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Bác Sĩ"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;
        doctorComboBox = new JComboBox<>(doctorOptions);
        inputPanel.add(doctorComboBox, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Triệu chứng"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;
        symptomsField = new JTextArea(3, 20);
        inputPanel.add(symptomsField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Chẩn đoán"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;
        diagnosisField = new JTextArea(3, 20);
        inputPanel.add(diagnosisField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Ghi chú"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;
        notesField = new JTextArea(3, 20);
        inputPanel.add(notesField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Trạng thái"), gbc);

        gbc.gridwidth = 3;
        gbc.gridx = 1;
        String[] statusOptions = {"PROCESSING", "NOT", "DONE"};
        statusComboBox = new JComboBox<>(statusOptions);
        inputPanel.add(statusComboBox, gbc);
        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Ensure final selection event
                int selectedRow = table1.getSelectedRow();
                if (selectedRow != -1) {
                    if (previousSelectedRow != -1) {
                        if(previousSelectedRow != selectedRow) {
                            if(!saved) {
                                JOptionPane.showMessageDialog(null, "Please save changes before selecting another patient");
                                returnCell = true;
                                table1.setRowSelectionInterval(previousSelectedRow, previousSelectedRow);
                                return;
                            }
                        }
                    }
                    // reset information

                    if (returnCell) {
                        returnCell = false;
                        return;
                    }
                    medicinePrescription = new String[][]{};
                    servicePrescription = new String[][]{};
                    handleRowSelection(selectedRow);

                }
            }
        });

        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new GridLayout(1, 5));
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding
        iconPanel.setBackground(Color.WHITE);
        String[] iconName = {"add", "save", "service", "medicine", "printer"};
        for (String name : iconName) {
            ImageIcon originalIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/" + name + ".png");
            Image scaledImage = originalIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH); // Resize to 32x32
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            JLabel iconLabel = new JLabel(scaledIcon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switch (name) {
                        case "add":
                            // Add action
                            JOptionPane.showMessageDialog(null, "Add action triggered");
                            break;
                        case "service":
                            // If there is no user selected, show a warning message
                            if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Please select a patient from the queue");
                                return;
                            }
                            // Open service dialog
                            if(serDialog == null) {
                                serDialog = new ServiceDialog(mainFrame);
                                previousSelectedRow = table1.getSelectedRow();
                            }
                            serDialog.setVisible(true);
                            saved = false;
                            servicePrescription = serDialog.getServicePrescription();

                            log.info("Service prescription: {}", servicePrescription);
                            break;
                        case "save": {
                            //Warning message
                            int option = JOptionPane.showOptionDialog(null, "Do you want to save changes?",
                                    "Save Changes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                    null, null, null);
                            if (option == JOptionPane.NO_OPTION) {
                                return;
                            }
                            // Save action
                            saved = true;
                            break;
                        }
                        case "medicine":
                            // If there is no user selected, show a warning message
                            if (checkupIdField.getText().isEmpty()) {
                                JOptionPane.showMessageDialog(null, "Please select a patient from the queue");
                                return;
                            }
                            // Open medicine dialog
                            if(medDialog == null) {
                                medDialog = new MedicineDialog(mainFrame);
                                previousSelectedRow = table1.getSelectedRow();
                            }
                            medDialog.setVisible(true);
                            saved = false;
                            medicinePrescription = medDialog.getMedicinePrescription();

                            log.info("Medicine prescription: {}", medicinePrescription);

                            break;
                        case "printer":
                            // Print action


                            MedicineInvoice medicineInvoice = new MedicineInvoice(checkupIdField.getText(),
                                    customerLastNameField.getText() + customerFirstNameField.getText(),
                                    dobPicker.getJFormattedTextField().getText(), customerPhoneField.getText(),
                                    genderComboBox.getSelectedItem().toString(), customerAddressField.getText(),
                                    doctorComboBox.getSelectedItem().toString(), diagnosisField.getText(),
                                    notesField.getText(), medicinePrescription);

                            medicineInvoice.createDialog(mainFrame);

                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + name);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    iconLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    iconLabel.setBorder(null);
                }
            });
            iconPanel.add(iconLabel);
        }

        JScrollPane inputScroll = new JScrollPane(inputPanel);

        rightBottomPanel.add(inputScroll, BorderLayout.CENTER);
        rightBottomPanel.add(iconPanel, BorderLayout.SOUTH);


        // Right top panel
        JLabel titleText3 = new JLabel("History");
        titleText3.setFont(new Font("Arial", Font.BOLD, 16));

        rightTopPanel.setLayout(new BorderLayout());
        rightTopPanel.add(titleText3, BorderLayout.NORTH);
        rightTopPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        String historyColumns[] = {"Ngày Tháng","Mã khám bệnh", "Triệu chứng", "Chẩn đoán", "Ghi chú"};

        historyModel = new DefaultTableModel(this.history, historyColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // All cells are non-editable
            }
        };
        historyTable = new JTable(historyModel);
        JScrollPane tableScroll2 = new JScrollPane(historyTable);
        tableScroll2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        rightTopPanel.add(tableScroll2, BorderLayout.CENTER);


        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder()); // Remove border
        JSplitPane splitPaneRight = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightTopPanel, rightBottomPanel);
        splitPaneRight.setResizeWeight(0.5); // Split
        splitPaneRight.setDividerSize(5); // Set divider size

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, splitPaneRight);
        splitPane.setResizeWeight(0.8); // Split
        splitPane.setDividerSize(5); // Set divider size

        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Remove border

        add(sidebarScrollPane, BorderLayout.WEST);
        add(topbar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void handleRowSelection(int selectedRow) {
        String checkupId = (String) table1.getValueAt(selectedRow, 0);
        String date = (String) table1.getValueAt(selectedRow, 1);
        String customerLastName = (String) table1.getValueAt(selectedRow, 2);
        String customerFirstName = (String) table1.getValueAt(selectedRow, 3);
        String doctor = (String) table1.getValueAt(selectedRow, 4);
        String symptoms = (String) table1.getValueAt(selectedRow, 5);
        String diagnosis = (String) table1.getValueAt(selectedRow, 6);
        String notes = (String) table1.getValueAt(selectedRow, 7);
        String status = (String) table1.getValueAt(selectedRow, 8);
        String customerId = queue[selectedRow][9];
        String cutomerPhone = queue[selectedRow][10];
        String customerAddress = queue[selectedRow][11];
        String customerWeight = queue[selectedRow][12];
        String customerHeight = queue[selectedRow][13];
        String customerGender = queue[selectedRow][14];
        String customerDob = queue[selectedRow][15];


        log.info("Selected customer: {}", customerId);
        checkupIdField.setText(checkupId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date parsedDate;

            // Check if the input is a Unix timestamp
            if (date.matches("\\d+")) { // Matches numeric strings (Unix timestamps)
                long timestamp = Long.parseLong(date); // Parse the timestamp
                parsedDate = new Date(timestamp); // Convert the timestamp to a Date
            } else {
                parsedDate = dateFormat.parse(date); // Parse formatted date strings
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);

            datePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getModel().setSelected(true);

        } catch (ParseException | NumberFormatException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Invalid date format or timestamp: " + date);
        }


        SimpleDateFormat dobFormat;
        try {
            Date parsedDate;
            if (customerDob.matches("\\d+")) {  // Check if the string is a timestamp
                long timestamp = Long.parseLong(customerDob); // Convert the string to a long
                parsedDate = new Date(timestamp); // Convert the timestamp to a Date
            } else {
                dobFormat = new SimpleDateFormat("dd/MM/yyyy");
                parsedDate = dobFormat.parse(customerDob); // Parse formatted date string
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);

            dobPicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dobPicker.getModel().setSelected(true);
        } catch (ParseException | NumberFormatException exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(null, "Invalid date format: " + customerDob);
        }


        customerLastNameField.setText(customerLastName);
        customerFirstNameField.setText(customerFirstName);
        doctorComboBox.setSelectedItem(doctor);
        symptomsField.setText(symptoms);
        diagnosisField.setText(diagnosis);
        notesField.setText(notes);
        statusComboBox.setSelectedItem(status);
        customerIdField.setText(customerId);
        customerPhoneField.setText(cutomerPhone);
        customerAddressField.setText(customerAddress);
        customerWeightSpinner.setValue(Double.parseDouble(customerWeight));
        customerHeightSpinner.setValue(Double.parseDouble(customerHeight));



        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCustomerHistoryRequest(Integer.parseInt(queue[selectedRow][9])));
    }


    private void handleGetCustomerHistoryResponse(GetCustomerHistoryResponse response) {
        log.info("Received customer history");
        this.history = response.getHistory();
        historyModel.setDataVector(this.history, new String[]{"Ngày Tháng","Mã khám bệnh", "Triệu chứng", "Chẩn đoán", "Ghi chú"});
    }

    private  void handleGetDoctorGeneralInfoResponse(GetDoctorGeneralInfoResponse response) {
        log.info("Received doctor general info");
        this.doctorOptions = response.getDoctorsName();
    }

    private void handleGetCheckUpQueueResponse(GetCheckUpQueueResponse response) {
        log.info("Received checkup queue");
        this.queue = response.getQueue();
        model.setDataVector(this.queue, new String[]{"Mã khám bệnh", "Ngày Tháng", "Họ", "Tên", "Bác Sĩ", "Triệu chứng", "Chẩn đoán", "Ghi chú", "Trạng thái"});
    }

    private void handleErrorResponse(ErrorResponse response) {
        log.error("Error response: {}", response.getError());
    }
}

