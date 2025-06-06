package BsK.client.ui.component.CheckoutPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.CheckoutPage.StandaloneMedicineDialog;
import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.ActionListener;

@Slf4j
public class CheckoutPage extends JPanel {
    private JLabel activeNavItem = null;
    private MainFrame mainFrame;

    // Left Panel Components
    private JTable finishedCheckupTable;
    private DefaultTableModel finishedCheckupTableModel;
    private JButton newStandaloneBillButton;

    // Right Panel Components (Bill Details)
    private JLabel billIdLabel;
    private JLabel billDateLabel;
    private JTextField customerNameField; // For standalone bills
    private JTextField customerPhoneField; // For standalone bills
    private JTextField barcodeField;
    private JButton scanBarcodeButton;
    private JButton addMedicineItemButton;
    private JButton removeMedicineItemButton;
    private JTable billItemsTable;
    private DefaultTableModel billItemsTableModel;
    private JLabel subtotalAmountLabel;
    private JTextField discountField;
    private JLabel grandTotalAmountLabel;
    private JButton processPaymentButton;
    private JButton printBillButton;
    
    private StandaloneMedicineDialog standaloneMedicineDialog;


    public CheckoutPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));

        // --- Navigation Bar ---
        JPanel navBar = new JPanel();
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS));
        navBar.setBackground(new Color(240, 240, 240));
        navBar.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        // Left section with navigation items
        JPanel navItemsPanel = new JPanel();
        navItemsPanel.setBackground(navBar.getBackground());
        navItemsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));
        navItemsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        navItemsPanel.setPreferredSize(new Dimension(750, 70));

        // Center section with current page title
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setBackground(navBar.getBackground());
        titlePanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        JLabel titleLabel = new JLabel("Thanh toán");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(60, 60, 60));
        titlePanel.add(titleLabel, new GridBagConstraints());

        // Right section with user info
        JPanel userPanel = new JPanel(new GridBagLayout());
        userPanel.setBackground(navBar.getBackground());
        userPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        userPanel.setPreferredSize(new Dimension(200, 70));

        // Add welcome label with user info
        JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        welcomePanel.setBackground(navBar.getBackground());
        JLabel welcomeLabel = new JLabel("Chào, " + LocalStorage.username);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        welcomeLabel.setForeground(new Color(60, 60, 60));
        welcomeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        welcomeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserMenu(welcomeLabel);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                welcomeLabel.setForeground(new Color(30, 30, 30));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                welcomeLabel.setForeground(new Color(60, 60, 60));
            }
        });
        welcomePanel.add(welcomeLabel);
        userPanel.add(welcomePanel, new GridBagConstraints());

        String[] navBarItems = {"Thống kê", "Thăm khám", "Dữ liệu", "Kho", "Thanh toán", "Người dùng", "Thông tin"};
        String[] destination = {"DashboardPage", "CheckUpPage", "PatientDataPage", "InventoryPage", "CheckoutPage", "UserPage", "InfoPage"};
        String[] iconFiles = {"dashboard.png", "health-check.png", "database.png", "warehouse.png", "cashier-machine.png", "user.png", "info.png"};
        Color[] navItemColors = {
            new Color(51, 135, 204),    // Darker Blue
            new Color(66, 157, 21),     // Darker Green
            new Color(200, 138, 16),    // Darker Orange
            new Color(204, 62, 63),     // Darker Red
            new Color(91, 37, 167),     // Darker Purple
            new Color(15, 155, 155),    // Darker Cyan
            new Color(196, 27, 36)      // Darker Red-Orange
        };

        final Color defaultTextColor = new Color(50, 50, 50);
        final Color activeTextColor = Color.WHITE;
        final Color shadowColor = new Color(0, 0, 0, 50);

        for (int i = 0; i < navBarItems.length; i++) {
            final String itemText = navBarItems[i];
            final String dest = destination[i];
            String iconFileName = iconFiles[i];
            final Color itemColor = navItemColors[i];
            
            RoundedPanel itemPanel = new RoundedPanel(15, itemColor.brighter(), true);
            itemPanel.setLayout(new BorderLayout(5, 5));
            itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            itemPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            itemPanel.setPreferredSize(new Dimension(130, 70));

            final JLabel label = new JLabel(itemText);
            label.setForeground(defaultTextColor);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setOpaque(false);

            try {
                String iconPath = "src/main/java/BsK/client/ui/assets/icon/" + iconFileName;
                ImageIcon originalIcon = new ImageIcon(iconPath);
                Image scaledImage = originalIcon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                label.setIcon(scaledIcon);
            } catch (Exception e) {
                log.error("Error loading icon: {} for nav item: {}", iconFileName, itemText, e);
            }

            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            itemPanel.add(label, BorderLayout.CENTER);

            if (itemText.equals("Thanh toán")) {
                itemPanel.setBackground(itemColor);
                label.setForeground(activeTextColor);
                itemPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(2, 2, 2, 2),
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2),
                        BorderFactory.createEmptyBorder(8, 13, 8, 13)
                    )
                ));
                activeNavItem = label;
            }

            final int index = i;
            itemPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (activeNavItem != null && activeNavItem != label) {
                        JPanel prevPanel = (JPanel) activeNavItem.getParent();
                        prevPanel.setBackground(navItemColors[findNavItemIndex(activeNavItem.getText(), navBarItems)].brighter());
                        prevPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 2, 2),
                            BorderFactory.createEmptyBorder(10, 15, 10, 15)
                        ));
                        activeNavItem.setForeground(defaultTextColor);
                    }
                    activeNavItem = label;
                    itemPanel.setBackground(itemColor);
                    label.setForeground(activeTextColor);
                    itemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(2, 2, 2, 2),
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2),
                            BorderFactory.createEmptyBorder(8, 13, 8, 13)
                        )
                    ));
                    mainFrame.showPage(dest);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor);
                        label.setForeground(activeTextColor);
                        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 2, 2),
                            BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 1),
                                BorderFactory.createEmptyBorder(9, 14, 9, 14)
                            )
                        ));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (label != activeNavItem) {
                        itemPanel.setBackground(itemColor.brighter());
                        label.setForeground(defaultTextColor);
                        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 2, 2),
                            BorderFactory.createEmptyBorder(10, 15, 10, 15)
                        ));
                    }
                }
            });
            navItemsPanel.add(itemPanel);
        }

        navBar.add(navItemsPanel);
        navBar.add(Box.createHorizontalGlue());
        navBar.add(titlePanel);
        navBar.add(Box.createHorizontalGlue());
        navBar.add(userPanel);
        navBar.setPreferredSize(new Dimension(1200, 90));

        add(navBar, BorderLayout.NORTH);

        // --- Main Content Area ---
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.35); // Give more space to bill details initially
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // --- Left Panel: Queues and New Bill ---
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));

        // Finished Checkup Queue Panel
        RoundedPanel finishedQueuePanel = new RoundedPanel(15, Color.WHITE, false);
        finishedQueuePanel.setLayout(new BorderLayout());
        finishedQueuePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
            "Chờ thanh toán (đã khám)",
            javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
        ));
        
        String[] finishedColumns = {"Mã Bill", "Tên BN", "Tổng tiền", "Trạng thái"};
        finishedCheckupTableModel = new DefaultTableModel(new Object[][]{}, finishedColumns) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        finishedCheckupTable = new JTable(finishedCheckupTableModel);
        finishedCheckupTable.setRowHeight(25);
        finishedCheckupTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        finishedCheckupTable.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane finishedTableScroll = new JScrollPane(finishedCheckupTable);
        finishedQueuePanel.add(finishedTableScroll, BorderLayout.CENTER);
        // TODO: Populate this table with patients who have Status.COMPLETED_CHECKUP or similar
        // TODO: Add MouseListener to finishedCheckupTable to load bill details on selection

        leftPanel.add(finishedQueuePanel, BorderLayout.CENTER);

        newStandaloneBillButton = new JButton("Tạo hóa đơn mới (khách lẻ)");
        newStandaloneBillButton.setFont(new Font("Arial", Font.BOLD, 14));
        newStandaloneBillButton.setBackground(new Color(0, 150, 136));
        newStandaloneBillButton.setForeground(Color.WHITE);
        newStandaloneBillButton.setFocusPainted(false);
        newStandaloneBillButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newStandaloneBillButton.setBorder(BorderFactory.createEmptyBorder(10,15,10,15));
        newStandaloneBillButton.addActionListener(e -> createNewStandaloneBill());
        JPanel newBillButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        newBillButtonPanel.setOpaque(false);
        newBillButtonPanel.add(newStandaloneBillButton);
        leftPanel.add(newBillButtonPanel, BorderLayout.SOUTH);

        mainSplitPane.setLeftComponent(leftPanel);

        // --- Right Panel: Bill Details ---
        RoundedPanel rightPanel = new RoundedPanel(15, Color.WHITE, false);
        rightPanel.setLayout(new BorderLayout(10,10));
         rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 10, 0, 0), 
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(63, 81, 181), 1, true),
                        "Chi tiết hóa đơn",
                        javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP,
                        new Font("Arial", Font.BOLD, 16), new Color(63, 81, 181)
                )
        ));

        JPanel billInfoAndActionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Bill Info (ID, Date, Customer for standalone)
        gbc.gridx = 0; gbc.gridy = 0; billInfoAndActionsPanel.add(new JLabel("Mã HĐ:"), gbc);
        gbc.gridx = 1; billIdLabel = new JLabel("N/A"); billInfoAndActionsPanel.add(billIdLabel, gbc);
        gbc.gridx = 2; billInfoAndActionsPanel.add(new JLabel("Ngày:"), gbc);
        gbc.gridx = 3; billDateLabel = new JLabel(new SimpleDateFormat("dd/MM/yyyy").format(new Date())); billInfoAndActionsPanel.add(billDateLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; billInfoAndActionsPanel.add(new JLabel("Tên KH (lẻ):"), gbc);
        gbc.gridx = 1; gbc.gridwidth=3; customerNameField = new JTextField(20); billInfoAndActionsPanel.add(customerNameField, gbc); gbc.gridwidth=1;
        gbc.gridx = 0; gbc.gridy = 2; billInfoAndActionsPanel.add(new JLabel("SĐT KH (lẻ):"), gbc);
        gbc.gridx = 1; gbc.gridwidth=3; customerPhoneField = new JTextField(15); billInfoAndActionsPanel.add(customerPhoneField, gbc); gbc.gridwidth=1;
        
        // Barcode Scan
        gbc.gridx = 0; gbc.gridy = 3; billInfoAndActionsPanel.add(new JLabel("Barcode:"), gbc);
        gbc.gridx = 1; gbc.gridwidth=2; barcodeField = new JTextField(20); billInfoAndActionsPanel.add(barcodeField, gbc);
        gbc.gridx = 3; gbc.gridwidth=1; scanBarcodeButton = new JButton("Quét"); billInfoAndActionsPanel.add(scanBarcodeButton, gbc);
        scanBarcodeButton.addActionListener(e -> scanBarcodeAction());
        gbc.gridwidth=1;

        // Bill Items Table
        String[] billItemCols = {"Mã Thuốc", "Tên thuốc", "SL", "ĐVT", "Đơn giá", "Thành tiền"};
        billItemsTableModel = new DefaultTableModel(new Object[][]{}, billItemCols) {
            public boolean isCellEditable(int row, int column) { return false; } // Or allow SL edit
        };
        billItemsTable = new JTable(billItemsTableModel);
        billItemsTable.setRowHeight(25);
        billItemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        billItemsTable.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane billItemsScroll = new JScrollPane(billItemsTable);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        billInfoAndActionsPanel.add(billItemsScroll, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0.0;

        // Add/Remove Medicine Buttons for Bill Items
        JPanel itemActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addMedicineItemButton = new JButton("Thêm thuốc vào HĐ");
        addMedicineItemButton.addActionListener(e -> openStandaloneMedicineDialog());
        removeMedicineItemButton = new JButton("Xóa thuốc khỏi HĐ");
        removeMedicineItemButton.addActionListener(e -> removeMedicineFromBillAction());
        itemActionPanel.add(addMedicineItemButton);
        itemActionPanel.add(removeMedicineItemButton);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; billInfoAndActionsPanel.add(itemActionPanel, gbc);


        // Totals Panel
        JPanel totalsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints totalGbc = new GridBagConstraints();
        totalGbc.insets = new Insets(3,10,3,10);
        totalGbc.anchor = GridBagConstraints.EAST;
        
        totalGbc.gridx=0; totalGbc.gridy=0; totalsPanel.add(new JLabel("Tổng phụ:"), totalGbc);
        totalGbc.gridx=1; subtotalAmountLabel = new JLabel("0 VNĐ"); totalsPanel.add(subtotalAmountLabel, totalGbc);
        totalGbc.gridx=0; totalGbc.gridy++; totalsPanel.add(new JLabel("Giảm giá:"), totalGbc);
        totalGbc.gridx=1; discountField = new JTextField("0", 8); discountField.setHorizontalAlignment(JTextField.RIGHT); totalsPanel.add(discountField, totalGbc);
        // TODO: Add listener to discountField to update grand total
        totalGbc.gridx=0; totalGbc.gridy++; totalsPanel.add(new JLabel("TỔNG CỘNG:"), totalGbc);
        totalGbc.gridx=1; grandTotalAmountLabel = new JLabel("0 VNĐ"); 
        grandTotalAmountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        grandTotalAmountLabel.setForeground(Color.RED);
        totalsPanel.add(grandTotalAmountLabel, totalGbc);
        
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 4; gbc.anchor = GridBagConstraints.EAST;
        billInfoAndActionsPanel.add(totalsPanel, gbc);


        // Main Action Buttons (Payment, Print)
        JPanel mainActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        processPaymentButton = new JButton("Thanh Toán");
        processPaymentButton.setFont(new Font("Arial", Font.BOLD, 16));
        processPaymentButton.setBackground(new Color(76, 175, 80));
        processPaymentButton.setForeground(Color.WHITE);
        processPaymentButton.addActionListener(e -> processPaymentAction());
        
        printBillButton = new JButton("In Hóa Đơn");
        printBillButton.setFont(new Font("Arial", Font.BOLD, 16));
        printBillButton.setBackground(new Color(33, 150, 243));
        printBillButton.setForeground(Color.WHITE);
        printBillButton.addActionListener(e -> printBillAction());

        mainActionsPanel.add(processPaymentButton);
        mainActionsPanel.add(printBillButton);

        rightPanel.add(billInfoAndActionsPanel, BorderLayout.CENTER);
        rightPanel.add(mainActionsPanel, BorderLayout.SOUTH);
        
        mainSplitPane.setRightComponent(new JScrollPane(rightPanel)); // Make right panel scrollable too

        add(mainSplitPane, BorderLayout.CENTER);
        
        // TODO: Load initial finished checkup queue
        loadFinishedCheckupQueue();
        updateBillDetailsPanel(null); // Initially no bill selected or new bill
    }

    private void loadFinishedCheckupQueue() {
        // TODO: Network request to get patients with status like "ĐÃ KHÁM XONG" or "CHỜ THANH TOÁN"
        // For now, add dummy data
        finishedCheckupTableModel.addRow(new Object[]{"CK001", "Nguyễn Văn A", "250,000 VNĐ", "Chờ TT"});
        finishedCheckupTableModel.addRow(new Object[]{"CK002", "Trần Thị B", "180,000 VNĐ", "Chờ TT"});
        log.info("Dummy finished checkup queue loaded.");
    }

    private void createNewStandaloneBill() {
        log.info("Creating new standalone bill.");
        // TODO: Generate a new Bill ID (e.g., "BL" + timestamp or sequence)
        billIdLabel.setText("BL" + System.currentTimeMillis()%10000); 
        billDateLabel.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        customerNameField.setText("");
        customerPhoneField.setText("");
        customerNameField.setEditable(true);
        customerPhoneField.setEditable(true);
        barcodeField.setText("");
        billItemsTableModel.setRowCount(0); // Clear items table
        subtotalAmountLabel.setText("0 VNĐ");
        discountField.setText("0");
        grandTotalAmountLabel.setText("0 VNĐ");
        // TODO: Clear/reset any other relevant fields for a new bill
        // TODO: Focus on customer name or barcode field
    }
    
    private void updateBillDetailsPanel(/* BillObject bill/Patient patient */ Object selectedBillData) {
        if (selectedBillData == null) { // For new bill or initial state
            billIdLabel.setText("N/A");
            billDateLabel.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            customerNameField.setText(""); customerNameField.setEditable(true);
            customerPhoneField.setText(""); customerPhoneField.setEditable(true);
            barcodeField.setText("");
            billItemsTableModel.setRowCount(0);
             updateTotals();
        } else {
            // TODO: Populate right panel based on selectedBillData (from finished queue)
            // Example:
            // billIdLabel.setText(bill.getId());
            // billDateLabel.setText(bill.getDate());
            // customerNameField.setText(patient.getName()); customerNameField.setEditable(false);
            // customerPhoneField.setText(patient.getPhone()); customerPhoneField.setEditable(false);
            // billItemsTableModel.setDataVector(bill.getItems(), billItemCols);
            // updateTotals();
             log.info("Loading bill details for: " + selectedBillData.toString());
        }
    }


    private void scanBarcodeAction() {
        String barcode = barcodeField.getText().trim();
        if (barcode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập barcode.", "Barcode trống", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // TODO: Network request to find medicine by barcode
        // If found, add to billItemsTable or open dialog with prefilled info
        log.info("Barcode scanned (mock): " + barcode);
        JOptionPane.showMessageDialog(this, "Đã quét barcode (mock): " + barcode + "\nTODO: Tìm thuốc và thêm vào HĐ.", "Quét Barcode", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void openStandaloneMedicineDialog() {
        if (standaloneMedicineDialog == null) {
            standaloneMedicineDialog = new StandaloneMedicineDialog(mainFrame);
        }
        standaloneMedicineDialog.setVisible(true);
        String[][] selectedMeds = standaloneMedicineDialog.getMedicinePrescription();
        if (selectedMeds != null) {
            for (String[] med : selectedMeds) {
                // med: ID, Tên thuốc, SL, ĐVT, Sáng, Trưa, Chiều, Đơn giá, Thành tiền, Ghi chú
                // For bill, we might only need: ID, Tên thuốc, SL, ĐVT, Đơn giá, Thành tiền
                 if (med.length >= 9) {
                    billItemsTableModel.addRow(new Object[]{
                        med[0], // ID
                        med[1], // Tên thuốc
                        med[2], // SL
                        med[3], // ĐVT
                        med[7], // Đơn giá
                        med[8]  // Thành tiền
                    });
                }
            }
            updateTotals();
        }
    }

    private void removeMedicineFromBillAction() {
        int selectedRow = billItemsTable.getSelectedRow();
        if (selectedRow != -1) {
            billItemsTableModel.removeRow(selectedRow);
            updateTotals();
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một thuốc để xóa khỏi hóa đơn.", "Chưa chọn thuốc", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void updateTotals() {
        double subtotal = 0;
        for (int i = 0; i < billItemsTableModel.getRowCount(); i++) {
            try {
                // Assuming "Thành tiền" is at column 5 and is a string like "120,000"
                String totalStr = billItemsTableModel.getValueAt(i, 5).toString().replace(",", "");
                subtotal += Double.parseDouble(totalStr);
            } catch (Exception e) {
                log.error("Error calculating subtotal for row " + i, e);
            }
        }
        subtotalAmountLabel.setText(String.format("%,.0f VNĐ", subtotal));
        
        double discount = 0;
        try {
            discount = Double.parseDouble(discountField.getText().replace(",", ""));
        } catch (NumberFormatException e) {
            // ignore, keep discount 0
        }
        
        double grandTotal = subtotal - discount;
        grandTotalAmountLabel.setText(String.format("%,.0f VNĐ", grandTotal));
    }

    private void processPaymentAction() {
        // TODO: Implement payment processing logic
        // - Validate bill (items, total > 0)
        // - Send request to backend to mark bill as paid, update inventory
        // - Show success/failure message
        // - Potentially clear the bill or offer to print
        String billId = billIdLabel.getText();
        String total = grandTotalAmountLabel.getText();
        log.info("Processing payment for Bill ID: " + billId + ", Amount: " + total);
        JOptionPane.showMessageDialog(this, "THANH TOÁN THÀNH CÔNG (mock)\nBill ID: "+billId+"\nTổng tiền: "+total+"\nTODO: Lưu HĐ, cập nhật kho...", "Xác nhận thanh toán", JOptionPane.INFORMATION_MESSAGE);
        createNewStandaloneBill(); // Clear for next bill
    }

    private void printBillAction() {
        // TODO: Implement bill printing
        // - Gather all necessary bill data (customer, items, totals)
        // - Create a new Invoice/Bill printing class (similar to MedicineInvoice but for general bills)
        // - Generate and show/print the PDF
        log.info("Printing bill (mock) for Bill ID: " + billIdLabel.getText());
         JOptionPane.showMessageDialog(this, "Đang chuẩn bị in hóa đơn (mock)...\nTODO: Tạo file PDF hóa đơn.", "In Hóa Đơn", JOptionPane.INFORMATION_MESSAGE);
    }

    private int findNavItemIndex(String text, String[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(text)) return i;
        }
        return 0;
    }

    private void showUserMenu(JLabel welcomeLabel) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(BorderFactory.createEmptyBorder());
        
        JMenuItem profileItem = createMenuItem("Hồ sơ cá nhân", e -> 
            JOptionPane.showMessageDialog(mainFrame, "Tính năng Hồ sơ cá nhân sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        
        JMenuItem settingsItem = createMenuItem("Cài đặt", e -> 
            JOptionPane.showMessageDialog(mainFrame, "Tính năng Cài đặt sắp ra mắt!", "Thông báo", JOptionPane.INFORMATION_MESSAGE));
        
        JMenuItem logoutItem = createMenuItem("Đăng xuất", e -> {
            LocalStorage.username = null;
            LocalStorage.userId = -1;
            mainFrame.showPage("LandingPage");
        });

        popupMenu.add(profileItem);
        popupMenu.add(settingsItem);
        popupMenu.addSeparator();
        popupMenu.add(logoutItem);
        
        popupMenu.setPreferredSize(new Dimension(180, popupMenu.getPreferredSize().height));
        popupMenu.show(welcomeLabel, 0, welcomeLabel.getHeight());
    }

    private JMenuItem createMenuItem(String text, ActionListener action) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Arial", Font.PLAIN, 12));
        item.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        item.addActionListener(action);
        return item;
    }
} 