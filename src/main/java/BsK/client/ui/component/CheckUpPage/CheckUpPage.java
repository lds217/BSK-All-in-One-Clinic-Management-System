package BsK.client.ui.component.CheckUpPage;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.common.packet.req.GetCheckUpQueueRequest;
import BsK.common.packet.res.ErrorResponse;
import BsK.common.packet.res.GetCheckUpQueueResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.DefaultTableModel;

import static BsK.client.network.handler.ClientHandler.frame;

@Slf4j
public class CheckUpPage extends JPanel {

    // update checkup queue
    private String[][] queue;

    public void updateQueue() {

        ClientHandler.addResponseListener(GetCheckUpQueueResponse.class, response -> {
            log.info("Received update queue: {}", frame.text());
        });

        ClientHandler.addResponseListener(ErrorResponse.class, response -> {
            log.error("Error response: {}", response.getError());
        });

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetCheckUpQueueRequest());


    }

    public CheckUpPage(MainFrame mainFrame) {

        updateQueue();

        setLayout(new BorderLayout());

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

        String[] sidebarItems = {"Thống kê","Thăm khám", "Dữ liệu bệnh nhân", "Kho", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage","CheckUpPage", "PatientDataPage", "InventoryPage", "UserPage", "InfoPage"};
        for(int i = 0; i < sidebarItems.length; i++) {
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
                    //  JOptionPane.showMessageDialog(null, item + " clicked!");
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

        RoundedPanel rightPanel = new RoundedPanel(20, Color.WHITE, false);

        JLabel titleText1 = new JLabel();
        titleText1.setText("Check Up Queue 1");
        titleText1.setFont(new Font("Arial", Font.BOLD, 16));
        titleText1.setBackground(Color.WHITE);
        titleText1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        JLabel titleText2 = new JLabel();
        titleText2.setText("Patient Info");
        titleText2.setFont(new Font("Arial", Font.BOLD, 16));
        titleText2.setBackground(Color.WHITE);
        titleText2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(titleText1, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10)); // Adjust padding for better appearance

        rightPanel.setLayout(new BorderLayout());
        rightPanel.add(titleText2, BorderLayout.NORTH);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        String[] columns = {"Name", "Gender", "Age"};
        String[][] data = {
                {"John", "Male", "20"},
                {"Dara", "Male", "20"},
                {"Bora", "Male", "21"},
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // All cells are non-editable
            }
        };
        JTable table1 = new JTable(model);

        // Set preferred size for the table
        table1.setPreferredScrollableViewportSize(new Dimension(400, 200));

        // Customize the font for the table header and cells
        table1.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table1.setFont(new Font("Arial", Font.PLAIN, 12));
        table1.setRowHeight(25);

        JScrollPane tableScroll1 = new JScrollPane(table1);
        tableScroll1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Inner padding
        leftPanel.add(tableScroll1, BorderLayout.CENTER);

        RoundedPanel inputPanel = new RoundedPanel(20, Color.WHITE, false);
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add some padding between components

        // Username label and text field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(10);
        inputPanel.add(usernameField, gbc);

        // Sex label and option field
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Sex:"), gbc);

        gbc.gridx = 1;
        String[] sexOptions = {"Male", "Female"};
        JComboBox<String> sexComboBox = new JComboBox<>(sexOptions);
        inputPanel.add(sexComboBox, gbc);

        // Age label and number field
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(new JLabel("Age:"), gbc);

        gbc.gridx = 1;
        JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 120, 1));
        inputPanel.add(ageSpinner, gbc);


        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table1.getSelectedRow();
                if (selectedRow != -1) {
                    // Get the data from the selected row
                    String name = (String) table1.getValueAt(selectedRow, 0);
                    String gender = (String) table1.getValueAt(selectedRow, 1);
                    String age = (String) table1.getValueAt(selectedRow, 2);

                    // Transfer the data to the inputPanel fields
                    usernameField.setText(name);
                    sexComboBox.setSelectedItem(gender);
                    ageSpinner.setValue(Integer.parseInt(age));
                }
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
                            int selectedRow = table1.getSelectedRow();

                            if (selectedRow != -1) {
                                // Get the data from the input fields
                                String name = usernameField.getText();
                                String gender = (String) sexComboBox.getSelectedItem();
                                int age = (Integer) ageSpinner.getValue();

                                // Update the data in the selected row
                                table1.setValueAt(name, selectedRow, 0);
                                table1.setValueAt(gender, selectedRow, 1);
                                table1.setValueAt(String.valueOf(age), selectedRow, 2);
                            } else
                                JOptionPane.showMessageDialog(null, "Please select a row to save changes");
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

        rightPanel.add(inputPanel, BorderLayout.CENTER);
        rightPanel.add(iconPanel, BorderLayout.SOUTH);

        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder()); // Remove border
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.5); // Split
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Remove border


        add(sidebarScrollPane, BorderLayout.WEST);
        add(topbar, BorderLayout.NORTH);
        add(splitPane , BorderLayout.CENTER);
    }



//    @Override
//    public void removeNotify() {
//        super.removeNotify();
//        // Remove listeners to avoid memory leaks
//        ClientHandler.removeResponseListener(GetCheckUpQueueResponse.class, this::handleGetCheckUpQueueResponse);
//        ClientHandler.removeResponseListener(ErrorResponse.class, this::handleErrorResponse);
//    }
//
//    private void handleGetCheckUpQueueResponse(GetCheckUpQueueResponse response) {
//        log.debug("Received checkup queue: {}", frame.text());
//    }
//
//    private void handleErrorResponse(ErrorResponse response) {
//        log.error("Error response: {}", response.getError());
//    }


}