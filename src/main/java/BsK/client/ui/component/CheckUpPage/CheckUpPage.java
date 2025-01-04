package BsK.client.ui.component.CheckUpPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
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
    private final ResponseListener<ErrorResponse> errorListener = this::handleErrorResponse;
    private final ResponseListener<GetCustomerHistoryResponse> customerHistoryListener = this::handleGetCustomerHistoryResponse;
    private JTextField checkupIdField, customerLastNameField, customerFirstNameField;
    private JTextArea symptomsField, diagnosisField, notesField;
    private JComboBox<String> doctorComboBox, statusComboBox;
    private JDatePickerImpl datePicker;

    public void updateQueue() {

        ClientHandler.addResponseListener(GetCheckUpQueueResponse.class, checkUpQueueListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueRequest());
    }

    private String[] doctorOptions;

    public void getDoctors() {
        // listener

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

        JLabel titleText2 = new JLabel();
        titleText2.setText("Patient Info");
        titleText2.setFont(new Font("Arial", Font.BOLD, 16));
        titleText2.setBackground(Color.WHITE);
        titleText2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(titleText1, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        rightBottomPanel.setLayout(new BorderLayout());
        rightBottomPanel.add(titleText2, BorderLayout.NORTH);
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
        // "Ngày Tháng", "Họ", "Tên", "Ten BS", "Họ BS", "Triệu chứng", "Chẩn đoán", "Ghi chú", "Trạng thái"
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add some padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        inputPanel.add(new JLabel("Mã khám bệnh:"), gbc);

        gbc.gridx = 1;
        checkupIdField = new JTextField();
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
        inputPanel.add(new JLabel("Ngày tháng:"), gbc);

        gbc.gridx = 3;
        inputPanel.add(datePicker, gbc);


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

        gbc.gridwidth = 4;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Bác Sĩ"), gbc);

        gbc.gridx = 1;
        doctorComboBox = new JComboBox<>(doctorOptions);
        inputPanel.add(doctorComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Triệu chứng"), gbc);

        gbc.gridx = 1;
        symptomsField = new JTextArea(3, 20);
        inputPanel.add(symptomsField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Chẩn đoán"), gbc);

        gbc.gridx = 1;
        diagnosisField = new JTextArea(3, 20);
        inputPanel.add(diagnosisField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Ghi chú"), gbc);

        gbc.gridx = 1;
        notesField = new JTextArea(3, 20);
        inputPanel.add(notesField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Trạng thái"), gbc);

        gbc.gridx = 1;
        String[] statusOptions = {"PROCESSING", "NOT", "DONE"};
        statusComboBox = new JComboBox<>(statusOptions);
        inputPanel.add(statusComboBox, gbc);

        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int selectedRow = table1.getSelectedRow();
                    if (selectedRow != -1) {
                        handleRowSelection(selectedRow);
                    }
                });
            }
        });


        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new GridLayout(1, 5));
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding
        iconPanel.setBackground(Color.WHITE);
        String[] iconName = {"add", "edit", "save", "delete"};
        for (String name : iconName) {
            ImageIcon originalIcon = new ImageIcon("src/main/resources/icon/" + name + ".png");
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
                        case "edit":
                            // Edit action
                            JOptionPane.showMessageDialog(null, "Edit action triggered");
                            break;
                        case "save": {
                            //Warning message
                            int option = JOptionPane.showOptionDialog(null, "Do you want to save changes?",
                                    "Save Changes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                    null, null, null);
                            if (option == JOptionPane.NO_OPTION) {
                                return;
                            }
                            break;
                        }
                        case "delete":
                            // Delete action
                            JOptionPane.showMessageDialog(null, "Delete action triggered");
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

        rightBottomPanel.add(inputPanel, BorderLayout.CENTER);
        rightBottomPanel.add(iconPanel, BorderLayout.SOUTH);


        // Right top panel
        JLabel titleText3 = new JLabel("History");
        titleText3.setFont(new Font("Arial", Font.BOLD, 16));

        rightTopPanel.setLayout(new BorderLayout());
        rightTopPanel.add(titleText3, BorderLayout.NORTH);
        rightTopPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        String historyColumns[] = {"Mã khám bệnh", "Ngày Tháng", "Bác Sĩ", "Triệu chứng", "Chẩn đoán", "Ghi chú", "Trạng thái"};

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
        log.info("Selected customer: {}", customerId);
        checkupIdField.setText(checkupId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date parsedDate = dateFormat.parse(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            datePicker.getModel().setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.getModel().setSelected(true);
        } catch (ParseException exception) {
            exception.printStackTrace();
        }

        customerLastNameField.setText(customerLastName);
        customerFirstNameField.setText(customerFirstName);
        doctorComboBox.setSelectedItem(doctor);
        symptomsField.setText(symptoms);
        diagnosisField.setText(diagnosis);
        notesField.setText(notes);
        statusComboBox.setSelectedItem(status);

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCustomerHistoryRequest(Integer.parseInt(queue[selectedRow][9])));
    }


    private void handleGetCustomerHistoryResponse(GetCustomerHistoryResponse response) {
        log.info("Received customer history");
        this.history = response.getHistory();
        historyModel.setDataVector(this.history, new String[]{"Mã khám bệnh", "Ngày Tháng", "Bác Sĩ", "Triệu chứng", "Chẩn đoán", "Ghi chú", "Trạng thái"});
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

