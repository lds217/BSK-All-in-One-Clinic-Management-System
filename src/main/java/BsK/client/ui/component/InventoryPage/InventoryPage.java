package BsK.client.ui.component.InventoryPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
public class InventoryPage extends JPanel {
    private MainFrame mainFrame;
    
    // Navigation components
    private JPanel sideNavPanel;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    
    // Navigation buttons
    private JButton dashboardButton;
    private JButton inventoryViewButton;
    private JButton goodsReceiptButton;
    private JButton stockAdjustmentButton;
    private JButton reportsButton;
    private JButton productMasterButton;
    private JButton supplierMasterButton;
    
    // Sub-panels for each section
    private InventoryDashboard dashboardPanel;
    private InventoryViewPanel inventoryViewPanel;
    private GoodsReceiptPanel goodsReceiptPanel;
    private StockAdjustmentPanel stockAdjustmentPanel;
    private ReportsPanel reportsPanel;
    private ProductMasterPanel productMasterPanel;
    private SupplierMasterPanel supplierMasterPanel;
    
    // Common utilities
    private NumberFormat currencyFormatter;
    private SimpleDateFormat dateFormatter;
    
    // Current selected section
    private String currentSection = "dashboard";

    public InventoryPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.currencyFormatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        this.dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
        
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // Navigation Bar
        NavBar navBar = new NavBar(mainFrame, "Quản Lý Kho Thuốc");
        add(navBar, BorderLayout.NORTH);

        initializeComponents();
        layoutComponents();
        setupEventListeners();
        
        // Show dashboard by default
        showSection("dashboard");
    }

    private void initializeComponents() {
        // Initialize side navigation
        createSideNavigation();
        
        // Initialize content area with card layout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);
        
        // Initialize all panels
        dashboardPanel = new InventoryDashboard(this);
        inventoryViewPanel = new InventoryViewPanel(this);
        goodsReceiptPanel = new GoodsReceiptPanel(this);
        stockAdjustmentPanel = new StockAdjustmentPanel(this);
        reportsPanel = new ReportsPanel(this);
        productMasterPanel = new ProductMasterPanel(this);
        supplierMasterPanel = new SupplierMasterPanel(this);
        
        // Add panels to card layout
        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(inventoryViewPanel, "inventory");
        contentPanel.add(goodsReceiptPanel, "receipt");
        contentPanel.add(stockAdjustmentPanel, "adjustment");
        contentPanel.add(reportsPanel, "reports");
        contentPanel.add(productMasterPanel, "products");
        contentPanel.add(supplierMasterPanel, "suppliers");
    }

    private void createSideNavigation() {
        sideNavPanel = new JPanel();
        sideNavPanel.setLayout(new BoxLayout(sideNavPanel, BoxLayout.Y_AXIS));
        sideNavPanel.setBackground(new Color(37, 47, 63));
        sideNavPanel.setPreferredSize(new Dimension(220, 0));
        sideNavPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Navigation title
        JLabel titleLabel = new JLabel("QUẢN LÝ KHO");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        sideNavPanel.add(titleLabel);

        // Create navigation buttons
        dashboardButton = createNavButton("📊 Tổng Quan", "dashboard");
        inventoryViewButton = createNavButton("📦 Tồn Kho", "inventory");
        goodsReceiptButton = createNavButton("📥 Nhập Kho", "receipt");
        stockAdjustmentButton = createNavButton("📋 Kiểm Kê Kho", "adjustment");
        reportsButton = createNavButton("📈 Báo Cáo", "reports");
        
        // Add separator
        sideNavPanel.add(Box.createVerticalStrut(15));
        JLabel masterDataLabel = new JLabel("DANH MỤC");
        masterDataLabel.setFont(new Font("Arial", Font.BOLD, 12));
        masterDataLabel.setForeground(new Color(150, 150, 150));
        masterDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sideNavPanel.add(masterDataLabel);
        sideNavPanel.add(Box.createVerticalStrut(10));
        
        productMasterButton = createNavButton("💊 Thuốc", "products");
        supplierMasterButton = createNavButton("🏢 Nhà Cung Cấp", "suppliers");

        // Add buttons to panel
        sideNavPanel.add(dashboardButton);
        sideNavPanel.add(inventoryViewButton);
        sideNavPanel.add(goodsReceiptButton);
        sideNavPanel.add(stockAdjustmentButton);
        sideNavPanel.add(reportsButton);
        sideNavPanel.add(Box.createVerticalStrut(10));
        sideNavPanel.add(productMasterButton);
        sideNavPanel.add(supplierMasterButton);
        
        // Add flexible space at bottom
        sideNavPanel.add(Box.createVerticalGlue());
    }

    private JButton createNavButton(String text, String section) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(37, 47, 63));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(220, 45));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!section.equals(currentSection)) {
                    button.setBackground(new Color(52, 62, 78));
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!section.equals(currentSection)) {
                    button.setBackground(new Color(37, 47, 63));
                }
            }
        });
        
        return button;
    }

    private void layoutComponents() {
        // Main layout: side navigation + content
        add(sideNavPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void setupEventListeners() {
        dashboardButton.addActionListener(e -> showSection("dashboard"));
        inventoryViewButton.addActionListener(e -> showSection("inventory"));
        goodsReceiptButton.addActionListener(e -> showSection("receipt"));
        stockAdjustmentButton.addActionListener(e -> showSection("adjustment"));
        reportsButton.addActionListener(e -> showSection("reports"));
        productMasterButton.addActionListener(e -> showSection("products"));
        supplierMasterButton.addActionListener(e -> showSection("suppliers"));
    }

    public void showSection(String section) {
        // Update button states
        resetNavButtonColors();
        currentSection = section;
        
        // Highlight active button
        JButton activeButton = getButtonForSection(section);
        if (activeButton != null) {
            activeButton.setBackground(new Color(63, 81, 181));
        }
        
        // Show corresponding panel
        cardLayout.show(contentPanel, section);
        
        // Refresh panel data if needed
        refreshCurrentPanel(section);
        
        log.info("Switched to inventory section: " + section);
    }

    private void resetNavButtonColors() {
        Color defaultColor = new Color(37, 47, 63);
        dashboardButton.setBackground(defaultColor);
        inventoryViewButton.setBackground(defaultColor);
        goodsReceiptButton.setBackground(defaultColor);
        stockAdjustmentButton.setBackground(defaultColor);
        reportsButton.setBackground(defaultColor);
        productMasterButton.setBackground(defaultColor);
        supplierMasterButton.setBackground(defaultColor);
    }

    private JButton getButtonForSection(String section) {
        switch (section) {
            case "dashboard": return dashboardButton;
            case "inventory": return inventoryViewButton;
            case "receipt": return goodsReceiptButton;
            case "adjustment": return stockAdjustmentButton;
            case "reports": return reportsButton;
            case "products": return productMasterButton;
            case "suppliers": return supplierMasterButton;
            default: return null;
        }
    }

    private void refreshCurrentPanel(String section) {
        // TODO: Implement panel refresh logic
        switch (section) {
            case "dashboard":
                dashboardPanel.refreshData();
                break;
            case "inventory":
                inventoryViewPanel.refreshData();
                break;
            case "receipt":
                goodsReceiptPanel.refreshData();
                break;
            case "adjustment":
                stockAdjustmentPanel.refreshData();
                break;
            case "reports":
                reportsPanel.refreshData();
                break;
            case "products":
                productMasterPanel.refreshData();
                break;
            case "suppliers":
                supplierMasterPanel.refreshData();
                break;
        }
    }

    // Getters for child panels
    public MainFrame getMainFrame() { return mainFrame; }
    public NumberFormat getCurrencyFormatter() { return currencyFormatter; }
    public SimpleDateFormat getDateFormatter() { return dateFormatter; }
    
    // Navigation methods for child panels
    public void navigateToInventoryView() { showSection("inventory"); }
    public void navigateToGoodsReceipt() { showSection("receipt"); }
    public void navigateToStockAdjustment() { showSection("adjustment"); }
    public void navigateToReports() { showSection("reports"); }
    public void navigateToProductMaster() { showSection("products"); }
} 