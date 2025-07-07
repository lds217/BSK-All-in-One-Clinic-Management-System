package BsK.client.ui.component.CheckUpPage.TemplateDialog;

import BsK.client.ui.component.MainFrame;
import BsK.common.packet.req.AddTemplateReq;
import BsK.common.packet.res.AddTemplateRes;
import BsK.common.packet.req.DeleteTemplateReq;
import BsK.common.packet.res.DeleteTemplateRes;
import BsK.common.packet.req.EditTemplateReq;
import BsK.common.packet.res.EditTemplateRes;
import BsK.common.packet.req.GetAllTemplatesReq;
import BsK.common.packet.res.GetAllTemplatesRes;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import BsK.common.entity.Template;

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import java.io.StringReader;
import java.util.List;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JColorChooser;

@Slf4j
public class TemplateDialog extends JDialog {
    private MainFrame mainFrame;
    private final ResponseListener<AddTemplateRes> addTemplateResListener;
    private final ResponseListener<GetAllTemplatesRes> getAllTemplatesResListener;
    private final ResponseListener<EditTemplateRes> editTemplateResListener;
    private final ResponseListener<DeleteTemplateRes> deleteTemplateResListener;
    private List<Template> templates;
    
    // Left panel components
    private JTextField idField;
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
        
        this.getAllTemplatesResListener = this::handleGetAllTemplatesResponse;
        this.addTemplateResListener = this::handleAddTemplateResponse;
        this.editTemplateResListener = this::handleEditTemplateResponse;
        this.deleteTemplateResListener = this::handleDeleteTemplateResponse;
        
        ClientHandler.addResponseListener(GetAllTemplatesRes.class, getAllTemplatesResListener);
        ClientHandler.addResponseListener(AddTemplateRes.class, addTemplateResListener);
        ClientHandler.addResponseListener(EditTemplateRes.class, editTemplateResListener);
        ClientHandler.addResponseListener(DeleteTemplateRes.class, deleteTemplateResListener);
        
        // Ensure modal behavior and proper parent relationship
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(false); // Don't force always on top, let modal behavior handle this
        
        // Add window listener to handle minimize/restore events with parent
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                if (mainFrame != null) {
                    mainFrame.setState(Frame.ICONIFIED);
                }
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
                if (mainFrame != null && mainFrame.getState() == Frame.ICONIFIED) {
                    mainFrame.setState(Frame.NORMAL);
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                loadTemplates();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                ClientHandler.deleteListener(AddTemplateRes.class, addTemplateResListener);
                ClientHandler.deleteListener(GetAllTemplatesRes.class, getAllTemplatesResListener);
                ClientHandler.deleteListener(EditTemplateRes.class, editTemplateResListener);
                ClientHandler.deleteListener(DeleteTemplateRes.class, deleteTemplateResListener);
            }
        });
        
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

        
        genderComboBox = new JComboBox<>(new String[]{"Chung", "Nam", "Nữ"});
        
        templateNameField = new JTextField(20);
        titleField = new JTextField(20);
        
        diagnosisArea = new JTextArea(3, 20);
        diagnosisArea.setLineWrap(true);
        diagnosisArea.setWrapStyleWord(true);
        
        imageCountComboBox = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6"});
        printTypeComboBox = new JComboBox<>(new String[]{"Ngang", "Dọc"});
        
        // Table for template list
        String[] columnNames = {"ID", "Tên mẫu"};
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
        
        // Row 2: Id and Gender
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        infoPanel.add(new JLabel("Id:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(idField, gbc);

        gbc.gridx = 2;
        infoPanel.add(new JLabel("Giới:"), gbc);
        gbc.gridx = 3;
        infoPanel.add(genderComboBox, gbc);

        // Row 3: Tên mẫu
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        infoPanel.add(new JLabel("Tên mẫu:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(templateNameField, gbc);
        
        // Row 4: Tiêu đề
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 3;
        infoPanel.add(new JLabel("Tiêu đề:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(titleField, gbc);
        
        // Row 5: Chẩn đoán
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 4;
        infoPanel.add(new JLabel("Chẩn đoán:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        infoPanel.add(new JScrollPane(diagnosisArea), gbc);
        
        // Row 6: Số lượng hình, Kiểu in
        gbc.gridheight = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 6;
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

        // Font family selector
        String[] fontFamilies = {"Arial", "Times New Roman", "Verdana", "Courier New", "Tahoma", "Calibri"};
        JComboBox<String> fontFamilyComboBox = new JComboBox<>(fontFamilies);
        fontFamilyComboBox.setSelectedItem("Times New Roman");
        fontFamilyComboBox.setFont(new Font("Arial", Font.PLAIN, 12));
        fontFamilyComboBox.setToolTipText("Phông chữ");
        fontFamilyComboBox.setPreferredSize(new Dimension(120, 25));
        fontFamilyComboBox.addActionListener(e -> {
            String fontFamily = (String) fontFamilyComboBox.getSelectedItem();
            if (fontFamily != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setFontFamily(attr, fontFamily);
                contentRTFField.getStyledDocument().setCharacterAttributes(
                    contentRTFField.getSelectionStart(),
                    contentRTFField.getSelectionEnd() - contentRTFField.getSelectionStart(),
                    attr, false);
            }
        });

        // Font size spinner
        SpinnerModel sizeModel = new SpinnerNumberModel(20, 8, 72, 2); // Default 20, min 8, max 72, step 2
        JSpinner sizeSpinner = new JSpinner(sizeModel);
        sizeSpinner.setFont(new Font("Arial", Font.PLAIN, 12));
        sizeSpinner.setToolTipText("Cỡ chữ (Cỡ chữ JasperReport = giá trị / 2)");
        sizeSpinner.setPreferredSize(new Dimension(60, 25));
        sizeSpinner.addChangeListener(e -> {
            int size = (int) sizeSpinner.getValue();
            MutableAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setFontSize(attr, size);
            contentRTFField.getStyledDocument().setCharacterAttributes(
                contentRTFField.getSelectionStart(),
                contentRTFField.getSelectionEnd() - contentRTFField.getSelectionStart(),
                attr, false);
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
        contentToolbar.add(fontFamilyComboBox);
        contentToolbar.addSeparator(new Dimension(5, 0));
        contentToolbar.add(sizeSpinner);
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
            String templateName = templateNameField.getText();
            String templateTitle = titleField.getText();
            String templateDiagnosis = diagnosisArea.getText();
            String templateConclusion = conclusionField.getText();
            String templateSuggestion = suggestionField.getText();
            String templateImageCount = imageCountComboBox.getSelectedItem().toString();
            String templateContent = "";
            try {
                RTFEditorKit rtfEditorKit = (RTFEditorKit) contentRTFField.getEditorKit();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                rtfEditorKit.write(baos, contentRTFField.getDocument(), 0, contentRTFField.getDocument().getLength());
                templateContent = baos.toString();
            } catch (Exception ex) {
                log.error("Could not get RTF text from contentRTFField", ex);
            }
            String templatePrintType = printTypeComboBox.getSelectedItem().toString();
            String templateGender = genderComboBox.getSelectedItem().toString();
            log.info("Template name: {}", templateName);
            log.info("Template title: {}", templateTitle);
            log.info("Template diagnosis: {}", templateDiagnosis);
            log.info("Template conclusion: {}", templateConclusion);
            log.info("Template suggestion: {}", templateSuggestion);
            log.info("Template image count: {}", templateImageCount);
            log.info("Template print type: {}", templatePrintType);
            log.info("Template gender: {}", templateGender);
            log.info("Template content: {}", templateContent);

            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddTemplateReq(templateName, templateTitle, templateDiagnosis, templateConclusion,
                        templateSuggestion, templateImageCount, templatePrintType, templateGender, templateContent));
            
        });
        
        cancelButton.addActionListener(e -> {
            // TODO: Implement cancel functionality
            log.info("Cancel button clicked");
            dispose();
        });
        
        saveButton.addActionListener(e -> {
            saveTemplate();
        });
        
        deleteButton.addActionListener(e -> {
            deleteTemplate();
        });
        
        templateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                // TODO: Implement template selection functionality
                int selectedRow = templateTable.getSelectedRow();
                if (selectedRow >= 0) {
                    log.info("Template selected: row {}", selectedRow);
                    int templateId = (int) templateTable.getValueAt(selectedRow, 0);
                    templates.stream()
                        .filter(t -> t.getTemplateId() == templateId)
                        .findFirst()
                        .ifPresent(this::populateFieldsFromTemplate);
                }
            }
        });
    }
    
    // TODO: Add methods for backend communication
    private void loadTemplates() {
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetAllTemplatesReq());
    }

    private void handleGetAllTemplatesResponse(GetAllTemplatesRes response) {
        this.templates = response.getTemplates();
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Template template : templates) {
                tableModel.addRow(new Object[]{template.getTemplateId(), template.getTemplateName()});
            }
        });
    }

    private void populateFieldsFromTemplate(Template template) {
        idField.setText(String.valueOf(template.getTemplateId()));
        genderComboBox.setSelectedItem(template.getTemplateGender());
        templateNameField.setText(template.getTemplateName());
        titleField.setText(template.getTemplateTitle());
        diagnosisArea.setText(template.getDiagnosis());
        imageCountComboBox.setSelectedItem(template.getPhotoNum());
        printTypeComboBox.setSelectedItem(template.getPrintType());
        conclusionField.setText(template.getConclusion());
        suggestionField.setText(template.getSuggestion());
        
        try {
            contentRTFField.setText(""); // Clear previous content
            RTFEditorKit rtfEditorKit = (RTFEditorKit) contentRTFField.getEditorKit();
            StringReader reader = new StringReader(template.getContent());
            rtfEditorKit.read(reader, contentRTFField.getDocument(), 0);
        } catch (Exception ex) {
            log.error("Failed to load RTF content for template ID {}", template.getTemplateId(), ex);
            contentRTFField.setText(template.getContent()); // Fallback to plain text
        }
    }
    
    private void saveTemplate() {
        if (idField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a template to edit.", "No Template Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String templateContent = "";
        try {
            RTFEditorKit rtfEditorKit = (RTFEditorKit) contentRTFField.getEditorKit();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            rtfEditorKit.write(baos, contentRTFField.getDocument(), 0, contentRTFField.getDocument().getLength());
            templateContent = baos.toString();
        } catch (Exception ex) {
            log.error("Could not get RTF text from contentRTFField", ex);
        }

        Template template = new Template(
            Integer.parseInt(idField.getText()),
            genderComboBox.getSelectedItem().toString(),
            templateNameField.getText(),
            titleField.getText(),
            imageCountComboBox.getSelectedItem().toString(),
            printTypeComboBox.getSelectedItem().toString(),
            templateContent,
            conclusionField.getText(),
            suggestionField.getText(),
            diagnosisArea.getText()
        );

        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new EditTemplateReq(template));
    }
    
    private void deleteTemplate() {
        if (idField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a template to delete.", "No Template Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this template?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (response == JOptionPane.YES_OPTION) {
            int templateId = Integer.parseInt(idField.getText());

            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new DeleteTemplateReq(templateId));
        }
    }
    
    private void handleAddTemplateResponse(AddTemplateRes response) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, response.getMessage(), response.isSuccess() ? "Success" : "Error", response.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            if (response.isSuccess()) {
                loadTemplates();
            }
        });
    }

    private void handleEditTemplateResponse(EditTemplateRes response) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, response.getMessage(), response.isSuccess() ? "Success" : "Error", response.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            if (response.isSuccess()) {
                loadTemplates();
            }
        });
    }

    private void handleDeleteTemplateResponse(DeleteTemplateRes response) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, response.getMessage(), response.isSuccess() ? "Success" : "Error", response.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            if (response.isSuccess()) {
                loadTemplates();
                clearFields();
            }
        });
    }

    private void clearFields() {
        idField.setText("");
        genderComboBox.setSelectedIndex(0);
        templateNameField.setText("");
        titleField.setText("");
        diagnosisArea.setText("");
        imageCountComboBox.setSelectedIndex(0);
        printTypeComboBox.setSelectedIndex(0);
        contentRTFField.setText("");
        conclusionField.setText("");
        suggestionField.setText("");
    }
} 