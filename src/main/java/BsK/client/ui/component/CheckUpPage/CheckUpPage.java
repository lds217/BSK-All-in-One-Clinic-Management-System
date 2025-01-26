package BsK.client.ui.component.CheckUpPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.CheckUpPage.AddDialog.AddDialog;
import BsK.client.ui.component.CheckUpPage.MedicineDialog.MedicineDialog;
import BsK.client.ui.component.CheckUpPage.PrintDialog.MedicineInvoice;
import BsK.client.ui.component.CheckUpPage.ServiceDialog.ServiceDialog;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.DateLabelFormatter;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.common.packet.req.*;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private final ResponseListener<GetCheckUpQueueResponse> checkUpQueueListener = this::handleGetCheckUpQueueResponse;
    private final ResponseListener<GetCustomerHistoryResponse> customerHistoryListener = this::handleGetCustomerHistoryResponse;

    private JTextField checkupIdField, customerLastNameField, customerFirstNameField,customerAddressField, customerPhoneField, customerIdField;
    private JTextArea symptomsField, diagnosisField, notesField;
    private JComboBox<String> doctorComboBox, statusComboBox, genderComboBox, provinceComboBox, districtComboBox, wardComboBox;
    private JSpinner customerWeightSpinner, customerHeightSpinner;
    private JDatePickerImpl datePicker, dobPicker;
    private String[][] medicinePrescription;
    private String[][] servicePrescription;
    private String[] doctorOptions;
    private MedicineDialog medDialog = null;
    private ServiceDialog serDialog = null;
    private AddDialog addDialog = null;
    private int previousSelectedRow = -1;
    private boolean saved = false;


    boolean returnCell = false;
    public void updateQueue() {
        ClientHandler.addResponseListener(GetCheckUpQueueResponse.class, checkUpQueueListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueRequest());
    }


    public CheckUpPage(MainFrame mainFrame) {
        setLayout(new BorderLayout());

        // add history listener
        ClientHandler.addResponseListener(GetCustomerHistoryResponse.class, customerHistoryListener);
        updateQueue();

        // Navigation bar
        // navBar panel
        JPanel navBar = new JPanel();
        navBar.setBackground(new Color(63, 81, 181));
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS)); // Use BoxLayout for horizontal layout

        // Left-aligned navigation items (centered vertically)
        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(new Color(63, 81, 181));
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 10)); // Items on the left
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT); // Vertically center the panel in navBar

        // Add navigation items
        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Kho", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "UserPage", "InfoPage"};
        for (int i = 0; i < navBarItems.length; i++) {
            String item = navBarItems[i];
            String dest = destination[i];
            JLabel label = new JLabel(item);
            label.setForeground(Color.WHITE);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Change cursor to hand
            label.setFont(new Font("Arial", Font.BOLD, 14));

            // Add padding for visibility
            label.setBorder(BorderFactory.createEmptyBorder(7, 15, 10, 15));

            // Highlight the selected label
            if (item.equals("Thăm khám")) {
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 5, 0, new Color(33, 150, 243))); // Add bottom border
            }

            // Add a mouse listener to handle click events
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Handle the click event
                    mainFrame.showPage(dest);
                    // Reset background color for all labels
                    for (Component comp : navItemsPanel.getComponents()) {
                        JLabel lbl = (JLabel) comp;
                        lbl.setBackground(null); // Reset background
                        lbl.setOpaque(false); // Remove background color
                    }
                    // Set background for the clicked label
                    label.setBackground(new Color(33, 150, 243)); // Highlight clicked label
                    label.setOpaque(true);
                    label.setForeground(Color.WHITE); // Change text color to white
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    label.setForeground(new Color(200, 230, 255)); // Highlight on hover
                    label.setBackground(new Color(33, 150, 243)); // Highlight the button on hover
                    label.setOpaque(true); // Make the background visible on hover
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (label.getBackground() != new Color(33, 150, 243)) { // Only reset if it's not selected
                        label.setForeground(Color.WHITE); // Restore original color
                        label.setBackground(null); // Remove background color
                        label.setOpaque(false); // Make the background invisible
                    }
                }
            });

            navItemsPanel.add(label);
        }

        // Add a space between the navigation items and the "Welcome" label (this will push the welcome label to the far right)
        navBar.add(navItemsPanel);
        navBar.add(Box.createHorizontalGlue()); // This will push the welcome label to the right

        // Right-aligned "Welcome" label
        JLabel welcomeLabel = new JLabel("Welcome, " + LocalStorage.username + "            ");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // Make clickable
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // Add mouse interactions for the welcomeLabel
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) { // Check if it's a left-click
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem item1 = new JMenuItem("Option 1");
                    JMenuItem item2 = new JMenuItem("Option 2");
                    JMenuItem item3 = new JMenuItem("Option 3");

                    // Add action listeners to the menu items
                    item1.addActionListener(event -> System.out.println("Option 1 selected"));
                    item2.addActionListener(event -> System.out.println("Option 2 selected"));
                    item3.addActionListener(event -> System.out.println("Option 3 selected"));

                    // Add items to the popup menu
                    popupMenu.add(item1);
                    popupMenu.add(item2);
                    popupMenu.add(item3);

                    // Show the popup
                    popupMenu.show(welcomeLabel, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                welcomeLabel.setForeground(new Color(200, 230, 255)); // Highlight on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                welcomeLabel.setForeground(Color.WHITE); // Restore original color
            }
        });

        // Add the welcome label to the navBar (it will be on the far right due to the horizontal glue)
        navBar.add(welcomeLabel, BorderLayout.EAST);

        // Set preferred size for the navBar
        navBar.setPreferredSize(new Dimension(1200, 50));

        // Now the "Welcome" label is aligned to the right with space between the nav items



        // Data table inside a RoundedPanel
        RoundedPanel leftPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightTopPanel = new RoundedPanel(20, Color.WHITE, false);
        RoundedPanel rightBottomPanel = new RoundedPanel(20, Color.WHITE, false);

        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel titleText1 = new JLabel();
        titleText1.setText("Check Up Queue 1");
        titleText1.setFont(new Font("Arial", Font.BOLD, 16));
        titleText1.setBackground(Color.WHITE);
        titleText1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding


        JButton rightButton = new JButton("  ADD  ");
        rightButton.setBackground(new Color(63, 81, 181));
        rightButton.setForeground(Color.WHITE);
        rightButton.setFocusPainted(false);
        rightButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Padding
        rightButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        rightButton.addActionListener(e -> {
            if(addDialog == null) {
                addDialog = new AddDialog(mainFrame);
            }
            addDialog.setVisible(true);
        });

        topPanel.add(titleText1, BorderLayout.WEST);
        topPanel.add(rightButton, BorderLayout.EAST);

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(topPanel, BorderLayout.NORTH);
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

        gbc.gridy++;
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        provinceComboBox = new JComboBox<>(LocalStorage.provinces);
        inputPanel.add(provinceComboBox, gbc);


        gbc.gridx = 2;
        districtComboBox = new JComboBox<>(new String[]{"Huyện 1", "Huyện 2", "Huyện 3"});
        inputPanel.add(districtComboBox, gbc);
        // set not editable
        districtComboBox.setEnabled(false);


        gbc.gridx = 3;
        wardComboBox = new JComboBox<>(new String[]{"Phường 1", "Phường 2", "Phường 3"});
        inputPanel.add(wardComboBox, gbc);
        // set not editable
        wardComboBox.setEnabled(false);

        // Province ComboBox Listener
        provinceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = provinceComboBox.getSelectedIndex();
                DefaultComboBoxModel<String> districtModel = new DefaultComboBoxModel<>(new String[]{"Quận/Huyện"});
                districtComboBox.setModel(districtModel); // Set district combo box model
                if (selectedIndex != 0) { // If the selected index is not 0 (which corresponds to "Tỉnh/Thành phố")
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetDistrictRequest(LocalStorage.provinceToId
                            .get(provinceComboBox.getSelectedItem().toString())));
                    while (LocalStorage.districts == null || districtComboBox.getItemCount() <= 1) {
                        try {
                            Thread.sleep(100); // Wait for the district data to be fetched
                            if(LocalStorage.districts != null) {
                                districtModel = new DefaultComboBoxModel<>(LocalStorage.districts);
                                districtComboBox.setModel(districtModel);
                            }
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                     // Set district combo box model
                    districtComboBox.setEnabled(true); // Enable district combo box
                    wardComboBox.setSelectedItem("Xã/Phường"); // Set ward combo box to default value
                    wardComboBox.setEnabled(false);
                } else {
                    districtComboBox.setEnabled(false); // Disable district combo box if "Tỉnh/Thành phố" is selected
                    wardComboBox.setEnabled(false); // Disable ward combo box as well
                }
            }
        });


        // District ComboBox Listener
        districtComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = districtComboBox.getSelectedIndex();
                DefaultComboBoxModel<String> wardModel = new DefaultComboBoxModel<>(new String[]{"Xã/Phường"});
                wardComboBox.setModel(wardModel); // Set district combo box model
                if (selectedIndex != 0) { // If the selected index is not 0 (which corresponds to "Quận/Huyện")
                    NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetWardRequest(LocalStorage.districtToId
                            .get(districtComboBox.getSelectedItem().toString())));
                    log.info("Sending GetWardRequest with district ID: {}", LocalStorage.districtToId
                            .get(districtComboBox.getSelectedItem().toString()));
                    while (LocalStorage.wards == null || wardComboBox.getItemCount() <= 1) {
                        try {
                            Thread.sleep(100); // Wait for the district data to be fetched
                            if(LocalStorage.wards != null) {
                                wardModel = new DefaultComboBoxModel<>(LocalStorage.wards);
                                wardComboBox.setModel(wardModel);
                            }
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                    wardComboBox.setSelectedItem("Xã/Phường"); // Set ward combo box to default value
                    wardComboBox.setEnabled(true); // Enable ward combo box
                } else {
                    districtComboBox.setEnabled(false); // Disable district combo box if "Tỉnh/Thành phố" is selected
                    wardComboBox.setEnabled(false); // Disable ward combo box as well
                }
            }
        });


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
        doctorComboBox = new JComboBox<>(LocalStorage.doctorsName);
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
                    medDialog = null;
                    serDialog = null;
                    handleRowSelection(selectedRow);

                }
            }
        });

        JPanel iconPanel = new JPanel();
        iconPanel.setLayout(new GridLayout(1, 5));
        iconPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding
        iconPanel.setBackground(Color.WHITE);
        String[] iconName = {"save", "service", "medicine", "printer"};
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

        add(navBar, BorderLayout.NORTH);
        //add(topbar, BorderLayout.NORTH);
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
        LocalStorage.doctorsName = response.getDoctorsName();
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

