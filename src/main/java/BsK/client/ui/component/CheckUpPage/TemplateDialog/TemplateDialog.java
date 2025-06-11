package BsK.client.ui.component.CheckUpPage.TemplateDialog;

import BsK.client.ui.component.MainFrame;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.rtf.RTFEditorKit;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
public class TemplateDialog extends JDialog {
    private MainFrame mainFrame;
    
    // Left panel components
    private JTextField idField;
    private JSpinner sttSpinner;
    private JComboBox<String> genderComboBox;
    private JTextField templateNameField;
    private JTextField titleField;
    private JTextArea diagnosisArea;
    private JComboBox<String> imageCountComboBox;
    private JComboBox<String> printTypeComboBox;
    private JTable templateTable;
    private DefaultTableModel tableModel;
    
    // Right panel components
    private JTextPane contentRTFField;
    private JTextArea conclusionField;
    private JTextArea suggestionField;
    
    // Buttons
    private JButton addNewButton;
    private JButton cancelButton;
    private JButton saveButton;
    private JButton deleteButton;

    public TemplateDialog(MainFrame mainFrame) {
        super(mainFrame, "Quản lý mẫu", true);
        this.mainFrame = mainFrame;
        
        initializeComponents();
        setupLayout();
        setupListeners();
        
        setSize(900, 600);
        setLocationRelativeTo(mainFrame);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }
    
    private void initializeComponents() {
        // Left panel components
        idField = new JTextField(10);
        idField.setEditable(false);
        
        sttSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        
        genderComboBox = new JComboBox<>(new String[]{"Chung", "Nam", "Nữ"});
        
        templateNameField = new JTextField(20);
        titleField = new JTextField(20);
        
        diagnosisArea = new JTextArea(3, 20);
        diagnosisArea.setLineWrap(true);
        diagnosisArea.setWrapStyleWord(true);
        
        imageCountComboBox = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6"});
        printTypeComboBox = new JComboBox<>(new String[]{"Ngang", "Dọc"});
        
        // Table for template list
        String[] columnNames = {"STT", "Tên mẫu"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        templateTable = new JTable(tableModel);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Right panel components
        contentRTFField = new JTextPane();
        contentRTFField.setContentType("text/rtf");
        contentRTFField.setEditorKit(new RTFEditorKit());
        contentRTFField.setFont(new Font("Times New Roman", Font.PLAIN, 16)); // Set default font
        
        conclusionField = new JTextArea(5, 20);
        conclusionField.setLineWrap(true);
        conclusionField.setWrapStyleWord(true);
        
        suggestionField = new JTextArea(5, 20);
        suggestionField.setLineWrap(true);
        suggestionField.setWrapStyleWord(true);
        
        // Buttons
        addNewButton = new JButton("Thêm mới");
        cancelButton = new JButton("Huỷ");
        saveButton = new JButton("Lưu");
        deleteButton = new JButton("Xoá");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Create main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.3);
        mainSplitPane.setDividerSize(3);
        
        // Left panel
        JPanel leftPanel = createLeftPanel();
        mainSplitPane.setLeftComponent(leftPanel);
        
        // Right panel
        JPanel rightPanel = createRightPanel();
        mainSplitPane.setRightComponent(rightPanel);
        
        add(mainSplitPane, BorderLayout.CENTER);
    }
    
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(3, 3));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Template info panel
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Thông tin mẫu",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(63, 81, 181)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Row 1: Buttons
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttonPanel.add(addNewButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(deleteButton);
        infoPanel.add(buttonPanel, gbc);
        
        // Row 2: Id, STT
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        infoPanel.add(new JLabel("Id:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(idField, gbc);
        
        gbc.gridx = 2;
        infoPanel.add(new JLabel("STT:"), gbc);
        gbc.gridx = 3;
        infoPanel.add(sttSpinner, gbc);
        
        // Row 3: Giới
        gbc.gridx = 0; gbc.gridy = 2;
        infoPanel.add(new JLabel("Giới:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        infoPanel.add(genderComboBox, gbc);
        
        // Row 4: Tên mẫu
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 3;
        infoPanel.add(new JLabel("Tên mẫu:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(templateNameField, gbc);
        
        // Row 5: Tiêu đề
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 4;
        infoPanel.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(titleField, gbc);
        
        // Row 6: Chẩn đoán (2 row span instead of 3)
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 5;
        infoPanel.add(new JLabel("Chẩn đoán:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        infoPanel.add(new JScrollPane(diagnosisArea), gbc);
        
        // Row 7: Số lượng hình, Kiểu in (fixed positioning)
        gbc.gridheight = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 7;
        infoPanel.add(new JLabel("Số lượng hình:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(imageCountComboBox, gbc);
        
        gbc.gridx = 2;
        infoPanel.add(new JLabel("Kiểu in:"), gbc);
        gbc.gridx = 3;
        infoPanel.add(printTypeComboBox, gbc);
        
        leftPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Template table at bottom
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Danh sách mẫu",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(63, 81, 181)
        ));
        
        JScrollPane tableScrollPane = new JScrollPane(templateTable);
        tableScrollPane.setPreferredSize(new Dimension(0, 150));
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        
        leftPanel.add(tablePanel, BorderLayout.SOUTH);
        
        return leftPanel;
    }
    
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(3, 3));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Top: RTF Content with toolbar
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Nội dung",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(63, 81, 181)
        ));
        
        // Create formatting toolbar for RTF content
        JToolBar contentToolbar = createContentToolbar();
        contentPanel.add(contentToolbar, BorderLayout.NORTH);
        
        JScrollPane contentScrollPane = new JScrollPane(contentRTFField);
        contentScrollPane.setPreferredSize(new Dimension(0, 250));
        contentPanel.add(contentScrollPane, BorderLayout.CENTER);
        
        rightPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Bottom: Conclusion and Suggestion side by side
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        
        // Conclusion panel
        JPanel conclusionPanel = new JPanel(new BorderLayout());
        conclusionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Kết luận",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(50, 50, 50)
        ));
        conclusionPanel.add(new JScrollPane(conclusionField), BorderLayout.CENTER);
        
        // Suggestion panel
        JPanel suggestionPanel = new JPanel(new BorderLayout());
        suggestionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Đề nghị",
            TitledBorder.LEADING, TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(50, 50, 50)
        ));
        suggestionPanel.add(new JScrollPane(suggestionField), BorderLayout.CENTER);
        
        bottomPanel.add(conclusionPanel);
        bottomPanel.add(suggestionPanel);
        bottomPanel.setPreferredSize(new Dimension(0, 150));
        
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        return rightPanel;
    }
    
    private JToolBar createContentToolbar() {
        JToolBar contentToolbar = new JToolBar(JToolBar.HORIZONTAL);
        contentToolbar.setFloatable(false);
        contentToolbar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        contentToolbar.setBackground(new Color(245, 245, 245));

        // Font size combobox
        String[] sizes = {"12", "14", "16", "18", "20", "24"};
        JComboBox<String> sizeComboBox = new JComboBox<>(sizes);
        sizeComboBox.setSelectedItem("16");
        sizeComboBox.setFont(new Font("Arial", Font.PLAIN, 12));
        sizeComboBox.setToolTipText("Cỡ chữ");
        sizeComboBox.setPreferredSize(new Dimension(60, 25));
        sizeComboBox.addActionListener(e -> {
            String size = (String) sizeComboBox.getSelectedItem();
            if (size != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setFontSize(attr, Integer.parseInt(size));
                contentRTFField.getStyledDocument().setCharacterAttributes(
                    contentRTFField.getSelectionStart(),
                    contentRTFField.getSelectionEnd() - contentRTFField.getSelectionStart(),
                    attr, false);
            }
        });

        // Style buttons
        JButton boldButton = new JButton(new StyledEditorKit.BoldAction());
        boldButton.setText("B");
        boldButton.setFont(new Font("Arial", Font.BOLD, 12));
        boldButton.setFocusPainted(false);
        boldButton.setToolTipText("In đậm (Ctrl+B)");
        boldButton.setPreferredSize(new Dimension(30, 25));
        
        JButton italicButton = new JButton(new StyledEditorKit.ItalicAction());
        italicButton.setText("I");
        italicButton.setFont(new Font("Arial", Font.ITALIC, 12));
        italicButton.setFocusPainted(false);
        italicButton.setToolTipText("In nghiêng (Ctrl+I)");
        italicButton.setPreferredSize(new Dimension(30, 25));
        
        JButton underlineButton = new JButton(new StyledEditorKit.UnderlineAction());
        underlineButton.setText("U");
        underlineButton.setFont(new Font("Arial", Font.PLAIN, 12));
        underlineButton.setFocusPainted(false);
        underlineButton.setToolTipText("Gạch chân (Ctrl+U)");
        underlineButton.setPreferredSize(new Dimension(30, 25));

        // Color chooser button
        JButton colorButton = new JButton("Màu");
        colorButton.setFont(new Font("Arial", Font.PLAIN, 11));
        colorButton.setFocusPainted(false);
        colorButton.setToolTipText("Chọn màu chữ");
        colorButton.setPreferredSize(new Dimension(50, 25));
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(TemplateDialog.this, "Chọn màu chữ", contentRTFField.getForeground());
            if (newColor != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, newColor);
                contentRTFField.setCharacterAttributes(attr, false);
            }
        });

        // Add components to toolbar with spacing
        contentToolbar.add(Box.createHorizontalStrut(3));
        contentToolbar.add(sizeComboBox);
        contentToolbar.addSeparator(new Dimension(8, 0));
        contentToolbar.add(boldButton);
        contentToolbar.addSeparator(new Dimension(3, 0));
        contentToolbar.add(italicButton);
        contentToolbar.addSeparator(new Dimension(3, 0));
        contentToolbar.add(underlineButton);
        contentToolbar.addSeparator(new Dimension(8, 0));
        contentToolbar.add(colorButton);
        contentToolbar.add(Box.createHorizontalStrut(3));

        // Style the toolbar buttons
        for (Component c : contentToolbar.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                b.setBackground(new Color(250, 250, 250));
                b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(2, 4, 2, 4)
                ));
            }
        }

        return contentToolbar;
    }
    
    private void setupListeners() {
        // TODO: Add listeners for all buttons and table selection
        
        addNewButton.addActionListener(e -> {
            // TODO: Implement add new template functionality
            log.info("Add new template button clicked");
        });
        
        cancelButton.addActionListener(e -> {
            // TODO: Implement cancel functionality
            log.info("Cancel button clicked");
            dispose();
        });
        
        saveButton.addActionListener(e -> {
            // TODO: Implement save template functionality
            log.info("Save template button clicked");
        });
        
        deleteButton.addActionListener(e -> {
            // TODO: Implement delete template functionality
            log.info("Delete template button clicked");
        });
        
        templateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                // TODO: Implement template selection functionality
                int selectedRow = templateTable.getSelectedRow();
                if (selectedRow >= 0) {
                    log.info("Template selected: row {}", selectedRow);
                }
            }
        });
    }
    
    // TODO: Add methods for backend communication
    private void loadTemplates() {
        // TODO: Load templates from backend
    }
    
    private void saveTemplate() {
        // TODO: Save template to backend
    }
    
    private void deleteTemplate() {
        // TODO: Delete template from backend
    }
    
    private void clearFields() {
        // TODO: Clear all input fields
    }
} 