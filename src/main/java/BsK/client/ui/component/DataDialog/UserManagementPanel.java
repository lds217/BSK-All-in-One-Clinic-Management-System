package BsK.client.ui.component.DataDialog;

import BsK.common.util.text.TextUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManagementPanel extends JPanel {

    // --- UI Components ---
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JTextField userSearchField;

    // --- Input Fields ---
    private JTextField userIdField;
    private JTextField usernameField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton btnAdd, btnEdit, btnDelete, btnClear;

    // --- Data & State ---
    private List<User> allUsers = new ArrayList<>();
    private User selectedUser = null;

    // A simple record to hold user data locally
    private record User(String id, String username, String password, String firstName, String lastName, String role) {}

    public UserManagementPanel() {
        super(new BorderLayout(10, 10));
        initComponents();
        createFakeData();
        filterUserTable(); // Initial population
    }

    private void initComponents() {
        this.setBorder(new EmptyBorder(15, 15, 15, 15));
        this.add(createUserInputPanel(), BorderLayout.WEST);
        this.add(createUserListPanel(), BorderLayout.CENTER);
    }

    private void createFakeData() {
        allUsers.add(new User(UUID.randomUUID().toString().substring(0, 8), "admin_user", "password123", "Admin", "Super", "ADMIN"));
        allUsers.add(new User(UUID.randomUUID().toString().substring(0, 8), "doctor_phil", "pass", "Phil", "McGraw", "USER"));
        allUsers.add(new User(UUID.randomUUID().toString().substring(0, 8), "nurse_jane", "pass", "Jane", "Doe", "USER"));
    }

    private JPanel createUserInputPanel() {
        JPanel mainInputPanel = new JPanel(new GridBagLayout());
        mainInputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.anchor = GridBagConstraints.NORTHWEST;
        mainGbc.insets = new Insets(5, 5, 5, 5);
        mainGbc.weightx = 1.0;
        mainGbc.gridx = 0;

        Font titleFont = new Font("Arial", Font.BOLD, 14);
        Font labelFont = new Font("Arial", Font.BOLD, 13);
        Font textFont = new Font("Arial", Font.PLAIN, 13);
        Dimension textFieldSize = new Dimension(100, 30);

        // --- User Details Panel ---
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Thông tin người dùng",
                TitledBorder.LEADING, TitledBorder.TOP, titleFont, new Color(50, 50, 50)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // User ID (Row 0)
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.2; // Label column width
        JLabel userIdLabel = new JLabel("UserID:");
        userIdLabel.setFont(labelFont);
        detailsPanel.add(userIdLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.8; // Field column width
        userIdField = new JTextField();
        userIdField.setEditable(false);
        userIdField.setFont(textFont);
        userIdField.setPreferredSize(textFieldSize);
        detailsPanel.add(userIdField, gbc);

        // Username (Row 1)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(labelFont);
        detailsPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        usernameField = new JTextField();
        usernameField.setFont(textFont);
        usernameField.setPreferredSize(textFieldSize);
        ((AbstractDocument) usernameField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, text.toLowerCase().replaceAll("[^a-z0-9_]", ""), attrs);
            }
        });
        detailsPanel.add(usernameField, gbc);

        // First Name (Row 2)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel firstNameLabel = new JLabel("Họ:");
        firstNameLabel.setFont(labelFont);
        detailsPanel.add(firstNameLabel, gbc);

        gbc.gridx = 1;
        firstNameField = new JTextField();
        firstNameField.setFont(textFont);
        firstNameField.setPreferredSize(textFieldSize);
        detailsPanel.add(firstNameField, gbc);

        // Last Name (Row 3)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel lastNameLabel = new JLabel("Tên:");
        lastNameLabel.setFont(labelFont);
        detailsPanel.add(lastNameLabel, gbc);
        
        gbc.gridx = 1;
        lastNameField = new JTextField();
        lastNameField.setFont(textFont);
        lastNameField.setPreferredSize(textFieldSize);
        detailsPanel.add(lastNameField, gbc);

        // Role (Row 4)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel roleLabel = new JLabel("Vai trò:");
        roleLabel.setFont(labelFont);
        detailsPanel.add(roleLabel, gbc);

        gbc.gridx = 1;
        roleComboBox = new JComboBox<>(new String[]{"USER", "ADMIN"});
        roleComboBox.setFont(textFont);
        roleComboBox.setPreferredSize(textFieldSize);
        detailsPanel.add(roleComboBox, gbc);

        // Password (Row 5)
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("Mật khẩu:");
        passwordLabel.setFont(labelFont);
        detailsPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        passwordField.setFont(textFont);
        passwordField.setPreferredSize(textFieldSize);
        detailsPanel.add(passwordField, gbc);

        mainGbc.gridy = 0;
        mainInputPanel.add(detailsPanel, mainGbc);

        mainGbc.gridy = 1;
        mainInputPanel.add(createButtonPanel(), mainGbc);

        mainGbc.gridy = 2;
        mainGbc.weighty = 1.0; // Filler to push content up
        mainInputPanel.add(new JPanel(), mainGbc);
        
        return mainInputPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnAdd = new JButton("Thêm mới");
        btnEdit = new JButton("Chỉnh sửa");
        btnDelete = new JButton("Xoá");
        btnClear = new JButton("Làm mới");

        Dimension btnSize = new Dimension(100, 35);
        btnAdd.setPreferredSize(btnSize);
        btnEdit.setPreferredSize(btnSize);
        btnDelete.setPreferredSize(btnSize);
        btnClear.setPreferredSize(btnSize);

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        btnClear.addActionListener(e -> clearUserFields());
        btnAdd.addActionListener(e -> handleAddUser());
        btnEdit.addActionListener(e -> handleEditUser());
        btnDelete.addActionListener(e -> handleDeleteUser());

        // Initial state
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);

        return buttonPanel;
    }

    private JPanel createUserListPanel() {
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Danh sách người dùng",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14)));

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Tìm theo username:"));
        userSearchField = new JTextField(25);
        userSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterUserTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterUserTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterUserTable(); }
        });
        searchPanel.add(userSearchField);
        listPanel.add(searchPanel, BorderLayout.NORTH);

        // Table
        String[] userColumns = {"UserID", "Username", "Họ", "Tên", "Vai trò"};
        userTableModel = new DefaultTableModel(userColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(28);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && userTable.getSelectedRow() != -1) {
                int viewRow = userTable.getSelectedRow();
                String userId = (String) userTableModel.getValueAt(viewRow, 0);
                selectedUser = allUsers.stream().filter(u -> u.id().equals(userId)).findFirst().orElse(null);
                if (selectedUser != null) {
                    populateUserFields(selectedUser);
                }
            }
        });
        listPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        return listPanel;
    }

    private void filterUserTable() {
        String filterText = userSearchField.getText().trim().toLowerCase();
        userTableModel.setRowCount(0);
        for (User user : allUsers) {
            if (filterText.isEmpty() || user.username().toLowerCase().contains(filterText)) {
                userTableModel.addRow(new Object[]{user.id(), user.username(), user.firstName(), user.lastName(), user.role()});
            }
        }
    }

    private void populateUserFields(User user) {
        userIdField.setText(user.id());
        usernameField.setText(user.username());
        firstNameField.setText(user.firstName());
        lastNameField.setText(user.lastName());
        roleComboBox.setSelectedItem(user.role());
        passwordField.setText("");
        passwordField.setEnabled(false);
        btnAdd.setEnabled(false);
        btnEdit.setEnabled(true);
        btnDelete.setEnabled(true);
    }

    private void clearUserFields() {
        selectedUser = null;
        userIdField.setText("(Tự động)");
        usernameField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        roleComboBox.setSelectedIndex(0);
        passwordField.setText("");
        passwordField.setEnabled(true);
        userTable.clearSelection();
        btnAdd.setEnabled(true);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);
        usernameField.requestFocusInWindow();
    }

    private void handleAddUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username và Mật khẩu không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (allUsers.stream().anyMatch(u -> u.username().equals(username))) {
            JOptionPane.showMessageDialog(this, "Username đã tồn tại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        User newUser = new User(
                UUID.randomUUID().toString().substring(0, 8),
                username,
                password, // In a real app, hash this!
                firstNameField.getText().trim(),
                lastNameField.getText().trim(),
                (String) roleComboBox.getSelectedItem()
        );
        allUsers.add(newUser);
        filterUserTable();
        clearUserFields();
        JOptionPane.showMessageDialog(this, "Thêm người dùng thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleEditUser() {
        if (selectedUser == null) return;
        // Find the user in the list and update them
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).id().equals(selectedUser.id())) {
                User updatedUser = new User(
                        selectedUser.id(),
                        usernameField.getText().trim(),
                        selectedUser.password(), // Keep original password
                        firstNameField.getText().trim(),
                        lastNameField.getText().trim(),
                        (String) roleComboBox.getSelectedItem()
                );
                allUsers.set(i, updatedUser);
                break;
            }
        }
        filterUserTable();
        clearUserFields();
        JOptionPane.showMessageDialog(this, "Cập nhật thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleDeleteUser() {
        if (selectedUser == null) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa người dùng '" + selectedUser.username() + "' không?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            allUsers.removeIf(u -> u.id().equals(selectedUser.id()));
            filterUserTable();
            clearUserFields();
        }
    }

    public void cleanup() {
        // No listeners to clean up yet, but good practice to have.
    }
}
