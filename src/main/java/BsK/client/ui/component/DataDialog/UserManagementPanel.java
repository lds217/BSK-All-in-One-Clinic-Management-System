package BsK.client.ui.component.DataDialog;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.entity.User;
import BsK.common.packet.req.AddUserRequest;
import BsK.common.packet.req.EditUserRequest;
import BsK.common.packet.req.GetAllUserInfoRequest;
import BsK.common.packet.res.GetAllUserInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UserManagementPanel extends JPanel {

    // --- UI Components ---
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTextField userSearchField;

    // --- Input Fields ---
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField lastNameField;
    private JTextField firstNameField;
    private JComboBox<String> roleComboBox;
    private JCheckBox chkIsDeleted;
    private boolean isPasswordVisible = false; // State for password visibility

    // --- Action Buttons ---
    private JButton btnAdd, btnEdit, btnClear;

    // --- Data & State ---
    private List<User> allUsers = new ArrayList<>();
    private String selectedUserId = null;

    // --- Networking ---
    private final ResponseListener<GetAllUserInfoResponse> getAllUserInfoListener = this::handleGetAllUserInfoResponse;

    public UserManagementPanel() {
        super(new BorderLayout(10, 10));
        initComponents();
        setupNetworking();
    }

    private void initComponents() {
        this.setBorder(new EmptyBorder(15, 15, 15, 15));
        this.add(createUserInputPanel(), BorderLayout.WEST);
        this.add(createUserListPanel(), BorderLayout.CENTER);
    }

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetAllUserInfoResponse.class, getAllUserInfoListener);
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetAllUserInfoRequest());
    }

    private JPanel createUserInputPanel() {
        JPanel mainInputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.weightx = 1.0;

        // --- User Info Panel ---
        JPanel userInfoPanel = new JPanel(new GridBagLayout());
        userInfoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin người dùng",
                TitledBorder.LEADING, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14), new Color(50, 50, 50)
        ));
        addFormFields(userInfoPanel);
        gbc.gridy = 0;
        mainInputPanel.add(userInfoPanel, gbc);

        // --- Button Panel ---
        gbc.gridy = 1;
        mainInputPanel.add(createButtonPanel(), gbc);

        // --- Filler ---
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        mainInputPanel.add(new JPanel(), gbc);

        return mainInputPanel;
    }

    private void addFormFields(JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        // --- Row 0: Username ---
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        usernameField = new JTextField(15);
        panel.add(usernameField, gbc);

        // --- Row 1: Password with View Button ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(createPasswordPanel(), gbc); // Use a helper method to create the composite panel

        // --- Row 2: Last Name ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Họ:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        lastNameField = new JTextField(15);
        panel.add(lastNameField, gbc);

        // --- Row 3: First Name ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Tên:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        firstNameField = new JTextField(15);
        panel.add(firstNameField, gbc);

        // --- Row 4: Role ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Vai trò:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        roleComboBox = new JComboBox<>(new String[]{"ADMIN", "USER"});
        panel.add(roleComboBox, gbc);
    }

    private JPanel createPasswordPanel() {
        JPanel passwordPanel = new JPanel(new BorderLayout(5, 0));
        passwordField = new JPasswordField(15);
        
        JButton viewPasswordButton = new JButton();
        try {
            ImageIcon viewIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/view.png");
            Image scaledImage = viewIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            viewPasswordButton.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            log.error("Could not load view icon", e);
            viewPasswordButton.setText("👁️"); // Fallback emoji
        }

        viewPasswordButton.setPreferredSize(new Dimension(30, 30));
        viewPasswordButton.setOpaque(false);
        viewPasswordButton.setContentAreaFilled(false);
        viewPasswordButton.setBorderPainted(false);
        viewPasswordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        viewPasswordButton.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible; // Toggle the state
            if (isPasswordVisible) {
                passwordField.setEchoChar((char) 0); // Show password
            } else {
                passwordField.setEchoChar('•'); // Hide password
            }
        });

        passwordPanel.add(passwordField, BorderLayout.CENTER);
        passwordPanel.add(viewPasswordButton, BorderLayout.EAST);
        return passwordPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnAdd = new JButton("Thêm mới");
        btnEdit = new JButton("Chỉnh sửa");
        btnClear = new JButton("Làm mới");
        chkIsDeleted = new JCheckBox("Xoá (Ẩn)");
        chkIsDeleted.setFont(new Font("Arial", Font.BOLD, 13));

        Dimension btnSize = new Dimension(100, 35);
        btnAdd.setPreferredSize(btnSize);
        btnEdit.setPreferredSize(btnSize);
        btnClear.setPreferredSize(btnSize);

        buttonPanel.add(chkIsDeleted);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnClear);

        // Button Actions
        btnClear.addActionListener(e -> clearUserFields());
        btnAdd.addActionListener(e -> addUser());
        btnEdit.addActionListener(e -> editUser());

        btnEdit.setEnabled(false);
        chkIsDeleted.setEnabled(false);

        return buttonPanel;
    }

    private JPanel createUserListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Danh Sách Người Dùng",
            TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14))
        );

        String[] userColumns = {"ID", "Tên đăng nhập", "Họ và Tên", "Vai trò", "Trạng thái"};
        userTableModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setFont(new Font("Arial", Font.PLAIN, 12));
        userTable.setRowHeight(28);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userTable.getSelectedRow() != -1) {
                int viewRow = userTable.getSelectedRow();
                String userId = (String) userTableModel.getValueAt(viewRow, 0);
                allUsers.stream()
                        .filter(user -> user.getId().equals(userId))
                        .findFirst()
                        .ifPresent(this::populateUserFields);
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm kiếm người dùng:"));
        userSearchField = new JTextField(25);
        searchPanel.add(userSearchField);
        userSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterUserTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterUserTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterUserTable(); }
        });

        listPanel.add(searchPanel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        return listPanel;
    }

    private void handleGetAllUserInfoResponse(GetAllUserInfoResponse response) {
        SwingUtilities.invokeLater(() -> {
            allUsers.clear();
            if (response != null && response.getUserInfo() != null) {
                for (String[] userData : response.getUserInfo()) {
                    allUsers.add(new User(userData));
                }
            }
            filterUserTable();
        });
    }

    private void filterUserTable() {
        String filterText = userSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());
        userTableModel.setRowCount(0);

        for (User user : allUsers) {
            String fullName = user.getLastName() + " " + user.getFirstName();
            if (filterText.isEmpty() ||
                TextUtils.removeAccents(user.getUserName().toLowerCase()).contains(lowerCaseFilterText) ||
                TextUtils.removeAccents(fullName.toLowerCase()).contains(lowerCaseFilterText))
            {
                String status = "0".equals(user.getDeleted()) ? "Hoạt động" : "Đã ẩn";
                userTableModel.addRow(new Object[]{
                        user.getId(),
                        user.getUserName(),
                        fullName,
                        user.getRole(),
                        status
                });
            }
        }
    }

    private void populateUserFields(User user) {
        if (user == null) return;
        selectedUserId = user.getId();
        usernameField.setText(user.getUserName());
        lastNameField.setText(user.getLastName());
        firstNameField.setText(user.getFirstName());
        roleComboBox.setSelectedItem(user.getRole());
        chkIsDeleted.setSelected("1".equals(user.getDeleted()));

        // --- Password field population ---
        isPasswordVisible = false; // Reset visibility state
        passwordField.setEchoChar('•'); // Ensure it's hidden
        passwordField.setText(user.getPassword()); // Populate with actual password

        btnAdd.setEnabled(false);
        btnEdit.setEnabled(true);
        chkIsDeleted.setEnabled(true);
    }

    private void clearUserFields() {
        selectedUserId = null;
        usernameField.setText("");
        lastNameField.setText("");
        firstNameField.setText("");
        roleComboBox.setSelectedIndex(0);
        chkIsDeleted.setSelected(false);
        
        // --- Clear password and reset visibility ---
        isPasswordVisible = false;
        passwordField.setEchoChar('•');
        passwordField.setText("");
        
        userTable.clearSelection();
        usernameField.requestFocusInWindow();

        btnAdd.setEnabled(true);
        btnEdit.setEnabled(false);
        chkIsDeleted.setEnabled(false);
    }

    private void addUser() {
        String userName = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (userName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập và mật khẩu không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AddUserRequest request = new AddUserRequest(
                userName,
                password,
                lastNameField.getText().trim(),
                firstNameField.getText().trim(),
                (String) roleComboBox.getSelectedItem(),
                false // New users are never deleted by default
        );
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
        JOptionPane.showMessageDialog(this, "Yêu cầu thêm người dùng '" + userName + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        clearUserFields();
    }

    private void editUser() {
        if (selectedUserId == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn người dùng để chỉnh sửa.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String userName = usernameField.getText().trim();
        if (userName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên đăng nhập không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Only send a password if the field is not empty, otherwise send empty string
        String password = new String(passwordField.getPassword());
        
        EditUserRequest request = new EditUserRequest(
                selectedUserId,
                userName,
                password, // Server should handle if this is empty (i.e., don't change password)
                lastNameField.getText().trim(),
                firstNameField.getText().trim(),
                (String) roleComboBox.getSelectedItem(),
                chkIsDeleted.isSelected()
        );
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
        JOptionPane.showMessageDialog(this, "Yêu cầu cập nhật người dùng '" + userName + "' đã được gửi.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        clearUserFields();
    }
    
    public void cleanup() {
        ClientHandler.deleteListener(GetAllUserInfoResponse.class, getAllUserInfoListener);
    }
}