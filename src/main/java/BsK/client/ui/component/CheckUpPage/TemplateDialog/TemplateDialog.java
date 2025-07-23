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
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
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
import java.util.ArrayList;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.JColorChooser;
import java.awt.event.KeyEvent;
import javax.swing.text.Document;

@Slf4j
public class TemplateDialog extends JDialog {
    private MainFrame mainFrame;
    private final ResponseListener<AddTemplateRes> addTemplateResListener;
    private final ResponseListener<GetAllTemplatesRes> getAllTemplatesResListener;
    private final ResponseListener<EditTemplateRes> editTemplateResListener;
    private final ResponseListener<DeleteTemplateRes> deleteTemplateResListener;
    private List<Template> templates;
    
    // RTF Enhancement components
    private UndoManager undoManager;
    private static List<Color> recentColors = new ArrayList<>();
    private static final Color[] COMMON_COLORS = {
        Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
        Color.ORANGE, Color.MAGENTA, new Color(128, 0, 128), // Purple
        new Color(165, 42, 42), // Brown
        new Color(0, 128, 128), // Teal
        new Color(128, 128, 0), // Olive
        new Color(255, 20, 147), // Deep Pink
        new Color(30, 144, 255)  // Dodger Blue
    };
    
    // Left panel components
    private JTextField idField;
    private JComboBox<String> genderComboBox;
    private JTextField templateNameField;
    private JTextField titleField;
    private JTextArea diagnosisArea;
    private JComboBox<String> imageCountComboBox;
    private JComboBox<String> printTypeComboBox;
    private JTextField sttField;
    private JCheckBox visibleCheckBox;
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
        
        sttField = new JTextField(10);
        sttField.setToolTipText("Số thứ tự để sắp xếp mẫu (0 = mặc định)");
        
        visibleCheckBox = new JCheckBox("Ẩn");
        visibleCheckBox.setToolTipText("Đánh dấu để ẩn mẫu này khỏi danh sách hiển thị");
        
        // Table for template list
        String[] columnNames = {"ID", "STT", "Tên mẫu"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        templateTable = new JTable(tableModel);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set bigger font for better visibility
        templateTable.setFont(new Font("Arial", Font.PLAIN, 14));
        templateTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        templateTable.setRowHeight(25); // Increase row height for better readability
        
        // Initialize recent colors if empty
        if (recentColors.isEmpty()) {
            recentColors.add(Color.BLACK);
            recentColors.add(Color.RED);
            recentColors.add(Color.BLUE);
        }
        
        // Right panel components
        contentRTFField = new JTextPane();
        contentRTFField.setContentType("text/rtf");
        contentRTFField.setEditorKit(new RTFEditorKit());
        contentRTFField.setFont(new Font("Times New Roman", Font.PLAIN, 16)); // Set default font
        
        // Set up undo manager
        undoManager = new UndoManager();
        Document doc = contentRTFField.getDocument();
        doc.addUndoableEditListener(undoManager);
        
        // Add keyboard shortcuts for formatting and undo/redo
        setupRTFKeyboardShortcuts();
        
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

        // Add Escape key listener to close dialog
        JRootPane rootPane = this.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE");
        ActionMap actionMap = rootPane.getActionMap();
        actionMap.put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
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
        
        // Row 7: STT and Visible checkbox
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1;
        infoPanel.add(new JLabel("STT:"), gbc);
        gbc.gridx = 1;
        infoPanel.add(sttField, gbc);
        
        gbc.gridx = 2; gbc.gridwidth = 2;
        infoPanel.add(visibleCheckBox, gbc);
        
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
        
        // Add recommendation label
        JLabel recommendLabel = new JLabel("Chú thích: Nên dùng font từ 20 đến 24, font chữ Times New Roman.");
        recommendLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        recommendLabel.setForeground(new Color(200, 60, 60));
        recommendLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        contentPanel.add(recommendLabel, BorderLayout.SOUTH);

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

        // Undo and Redo buttons
        JButton undoButton = new JButton("↶");
        undoButton.setFont(new Font("Arial", Font.BOLD, 14));
        undoButton.setFocusPainted(false);
        undoButton.setToolTipText("Hoàn tác (Ctrl+Z)");
        undoButton.setPreferredSize(new Dimension(30, 25));
        undoButton.addActionListener(e -> {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        });

        JButton redoButton = new JButton("↷");
        redoButton.setFont(new Font("Arial", Font.BOLD, 14));
        redoButton.setFocusPainted(false);
        redoButton.setToolTipText("Làm lại (Ctrl+Y)");
        redoButton.setPreferredSize(new Dimension(30, 25));
        redoButton.addActionListener(e -> {
            if (undoManager.canRedo()) {
                undoManager.redo();
            }
        });

        // Color chooser button
        JButton colorButton = new JButton("Màu");
        colorButton.setFont(new Font("Arial", Font.PLAIN, 11));
        colorButton.setFocusPainted(false);
        colorButton.setToolTipText("Chọn màu chữ");
        colorButton.setPreferredSize(new Dimension(50, 25));
        colorButton.addActionListener(e -> {
            Color newColor = showCustomColorChooser();
            if (newColor != null) {
                MutableAttributeSet attr = new SimpleAttributeSet();
                StyleConstants.setForeground(attr, newColor);
                contentRTFField.setCharacterAttributes(attr, false);
                addToRecentColors(newColor);
            }
        });

        // Add components to toolbar with spacing
        contentToolbar.add(Box.createHorizontalStrut(3));
        contentToolbar.add(undoButton);
        contentToolbar.addSeparator(new Dimension(3, 0));
        contentToolbar.add(redoButton);
        contentToolbar.addSeparator(new Dimension(8, 0));
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
    
    private void setupRTFKeyboardShortcuts() {
        InputMap inputMap = contentRTFField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = contentRTFField.getActionMap();
        
        // Undo/Redo shortcuts
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
        
        // Format shortcuts - Bold, Italic, Underline
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK), "bold");
        actionMap.put("bold", new StyledEditorKit.BoldAction());
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK), "italic");
        actionMap.put("italic", new StyledEditorKit.ItalicAction());
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK), "underline");
        actionMap.put("underline", new StyledEditorKit.UnderlineAction());
    }
    
    private Color showCustomColorChooser() {
        JDialog colorDialog = new JDialog(this, "Chọn màu chữ", true);
        colorDialog.setSize(400, 300);
        colorDialog.setLocationRelativeTo(this);
        colorDialog.setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Recent colors panel
        JPanel recentPanel = new JPanel(new GridLayout(1, Math.min(recentColors.size(), 6), 5, 5));
        recentPanel.setBorder(BorderFactory.createTitledBorder("Màu gần đây"));
        
        final Color[] selectedColor = new Color[1];
        
        for (Color color : recentColors) {
            JButton colorBtn = createColorButton(color, selectedColor, colorDialog);
            recentPanel.add(colorBtn);
        }
        
        // Common colors panel
        JPanel commonPanel = new JPanel(new GridLayout(3, 4, 5, 5));
        commonPanel.setBorder(BorderFactory.createTitledBorder("Màu thông dụng"));
        
        for (Color color : COMMON_COLORS) {
            JButton colorBtn = createColorButton(color, selectedColor, colorDialog);
            commonPanel.add(colorBtn);
        }
        
        // More colors button
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton moreColorsBtn = new JButton("Màu khác...");
        moreColorsBtn.addActionListener(e -> {
            Color customColor = JColorChooser.showDialog(colorDialog, "Chọn màu tùy chỉnh", Color.BLACK);
            if (customColor != null) {
                selectedColor[0] = customColor;
                colorDialog.dispose();
            }
        });
        buttonPanel.add(moreColorsBtn);
        
        JButton cancelBtn = new JButton("Hủy");
        cancelBtn.addActionListener(e -> {
            selectedColor[0] = null;
            colorDialog.dispose();
        });
        buttonPanel.add(cancelBtn);
        
        mainPanel.add(recentPanel, BorderLayout.NORTH);
        mainPanel.add(commonPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        colorDialog.add(mainPanel);
        colorDialog.setVisible(true);
        
        return selectedColor[0];
    }
    
    private JButton createColorButton(Color color, Color[] selectedColor, JDialog parentDialog) {
        JButton colorBtn = new JButton();
        colorBtn.setBackground(color);
        colorBtn.setPreferredSize(new Dimension(40, 30));
        colorBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        colorBtn.setToolTipText(getColorName(color));
        colorBtn.addActionListener(e -> {
            selectedColor[0] = color;
            parentDialog.dispose();
        });
        return colorBtn;
    }
    
    private String getColorName(Color color) {
        if (color.equals(Color.BLACK)) return "Đen";
        if (color.equals(Color.RED)) return "Đỏ";
        if (color.equals(Color.BLUE)) return "Xanh dương";
        if (color.equals(Color.GREEN)) return "Xanh lá";
        if (color.equals(Color.ORANGE)) return "Cam";
        if (color.equals(Color.MAGENTA)) return "Tím hồng";
        return String.format("RGB(%d,%d,%d)", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private void addToRecentColors(Color newColor) {
        // Remove if already exists
        recentColors.remove(newColor);
        // Add to front
        recentColors.add(0, newColor);
        // Keep only last 6 colors
        if (recentColors.size() > 6) {
            recentColors.remove(recentColors.size() - 1);
        }
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
            int templateStt = Integer.parseInt(sttField.getText().trim().isEmpty() ? "0" : sttField.getText().trim());
            log.info("Template name: {}", templateName);
            log.info("Template title: {}", templateTitle);
            log.info("Template diagnosis: {}", templateDiagnosis);
            log.info("Template conclusion: {}", templateConclusion);
            log.info("Template suggestion: {}", templateSuggestion);
            log.info("Template image count: {}", templateImageCount);
            log.info("Template print type: {}", templatePrintType);
            log.info("Template gender: {}", templateGender);
            log.info("Template content: {}", templateContent);
            log.info("Template STT: {}", templateStt);

            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new AddTemplateReq(templateName, templateTitle, templateDiagnosis, templateConclusion,
                        templateSuggestion, templateImageCount, templatePrintType, templateGender, templateContent, !visibleCheckBox.isSelected(), templateStt));
            
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
                tableModel.addRow(new Object[]{template.getTemplateId(), template.getStt(), template.getTemplateName()});
            }
            
            // Set column widths after data is loaded - ID and STT much smaller
            if (templateTable.getColumnCount() >= 3) {
                templateTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID column - much smaller
                templateTable.getColumnModel().getColumn(0).setMaxWidth(60);
                templateTable.getColumnModel().getColumn(0).setMinWidth(40);
                
                templateTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // STT column - much smaller  
                templateTable.getColumnModel().getColumn(1).setMaxWidth(60);
                templateTable.getColumnModel().getColumn(1).setMinWidth(40);
                
                templateTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Template name - larger
                
                // Center align ID and STT columns for better appearance
                javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
                centerRenderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                templateTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
                templateTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // STT
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
        sttField.setText(String.valueOf(template.getStt()));
        visibleCheckBox.setSelected(!template.isVisible()); // Checkbox is "Ẩn" so inverted logic
        conclusionField.setText(template.getConclusion());
        suggestionField.setText(template.getSuggestion());
        
        try {
            contentRTFField.setText(""); // Clear previous content
            RTFEditorKit rtfEditorKit = (RTFEditorKit) contentRTFField.getEditorKit();
            StringReader reader = new StringReader(template.getContent());
            rtfEditorKit.read(reader, contentRTFField.getDocument(), 0);
            
            // Reset undo manager after loading new content
            undoManager.discardAllEdits();
        } catch (Exception ex) {
            log.error("Failed to load RTF content for template ID {}", template.getTemplateId(), ex);
            contentRTFField.setText(template.getContent()); // Fallback to plain text
            
            // Reset undo manager even in fallback case
            undoManager.discardAllEdits();
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

        int templateStt = Integer.parseInt(sttField.getText().trim().isEmpty() ? "0" : sttField.getText().trim());
        
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
            diagnosisArea.getText(),
            !visibleCheckBox.isSelected(),  // Checkbox is "Ẩn" so inverted logic
            templateStt
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
        sttField.setText("0"); // Default STT to 0
        visibleCheckBox.setSelected(false); // Default to visible (not hidden)
        contentRTFField.setText("");
        conclusionField.setText("");
        suggestionField.setText("");
        
        // Reset undo manager when clearing fields
        if (undoManager != null) {
            undoManager.discardAllEdits();
        }
    }
} 