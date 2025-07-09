package BsK.client.ui.component.InventoryPage;

import BsK.client.ui.component.common.RoundedPanel;
import BsK.client.ui.component.InventoryPage.common.InventoryButtonFactory;
import BsK.client.ui.component.InventoryPage.common.InventoryColorScheme;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Slf4j
public class InventoryDashboard extends JPanel {
    private InventoryPage parentPage;
    
    // Dashboard metrics
    private JLabel lowStockCountLabel;
    private JLabel expiringCountLabel;
    private JLabel totalValueLabel;
    private JLabel totalItemsLabel;
    
    // Quick action buttons
    private JButton newReceiptButton;
    private JButton viewInventoryButton;
    private JButton lowStockReportButton;
    private JButton expiryReportButton;

    public InventoryDashboard(InventoryPage parentPage) {
        this.parentPage = parentPage;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));
        
        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        // Initialize metric labels with refined colors
        lowStockCountLabel = new JLabel("0");
        lowStockCountLabel.setFont(new Font("Arial", Font.BOLD, 42));
        lowStockCountLabel.setForeground(InventoryColorScheme.DANGER_RED);
        lowStockCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        expiringCountLabel = new JLabel("0");
        expiringCountLabel.setFont(new Font("Arial", Font.BOLD, 42));
        expiringCountLabel.setForeground(InventoryColorScheme.WARNING_ORANGE);
        expiringCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        totalValueLabel = new JLabel("0 VNĐ");
        totalValueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        totalValueLabel.setForeground(InventoryColorScheme.SUCCESS_GREEN);
        totalValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        totalItemsLabel = new JLabel("0");
        totalItemsLabel.setFont(new Font("Arial", Font.BOLD, 42));
        totalItemsLabel.setForeground(InventoryColorScheme.PRIMARY_BLUE);
        totalItemsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Initialize quick action buttons using standardized factory
        newReceiptButton = InventoryButtonFactory.createSuccessButton("📥 Nhập Kho Mới", InventoryButtonFactory.LARGE_BUTTON_SIZE);
        viewInventoryButton = InventoryButtonFactory.createPrimaryButton("📦 Xem Tồn Kho", InventoryButtonFactory.LARGE_BUTTON_SIZE);
        lowStockReportButton = InventoryButtonFactory.createDestructiveButton("⚠️ Thuốc Sắp Hết", InventoryButtonFactory.LARGE_BUTTON_SIZE);
        expiryReportButton = InventoryButtonFactory.createSpecialButton("📅 Sắp Hết Hạn", InventoryButtonFactory.LARGE_BUTTON_SIZE);
    }



    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        mainPanel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("Tổng Quan Kho Thuốc", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(37, 47, 63));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Metrics cards
        JPanel metricsPanel = createMetricsPanel();
        mainPanel.add(metricsPanel, BorderLayout.CENTER);

        // Quick actions
        JPanel actionsPanel = createActionsPanel();
        mainPanel.add(actionsPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 20, 20));
        panel.setOpaque(false);

        // Low Stock Card
        RoundedPanel lowStockCard = createMetricCard(
            "Thuốc Sắp Hết",
            lowStockCountLabel,
            "sản phẩm dưới mức tối thiểu",
            InventoryColorScheme.CARD_LOW_STOCK_BORDER,
            InventoryColorScheme.CARD_LOW_STOCK_BG
        );

        // Expiring Soon Card
        RoundedPanel expiringCard = createMetricCard(
            "Sắp Hết Hạn",
            expiringCountLabel,
            "lô thuốc trong 3 tháng tới",
            InventoryColorScheme.CARD_EXPIRING_BORDER,
            InventoryColorScheme.CARD_EXPIRING_BG
        );

        // Total Value Card
        RoundedPanel valueCard = createMetricCard(
            "Tổng Giá Trị Kho",
            totalValueLabel,
            "tại thời điểm hiện tại",
            InventoryColorScheme.CARD_VALUE_BORDER,
            InventoryColorScheme.CARD_VALUE_BG
        );

        // Total Items Card
        RoundedPanel itemsCard = createMetricCard(
            "Tổng Số Mặt Hàng",
            totalItemsLabel,
            "loại thuốc đang quản lý",
            InventoryColorScheme.CARD_ITEMS_BORDER,
            InventoryColorScheme.CARD_ITEMS_BG
        );

        panel.add(lowStockCard);
        panel.add(expiringCard);
        panel.add(valueCard);
        panel.add(itemsCard);

        return panel;
    }

    private RoundedPanel createMetricCard(String title, JLabel valueLabel, String subtitle, Color accentColor, Color bgColor) {
        RoundedPanel card = new RoundedPanel(15, bgColor, true);
        card.setLayout(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            BorderFactory.createEmptyBorder(25, 20, 25, 20)
        ));
        card.setPreferredSize(new Dimension(280, 160));

        // Title
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(accentColor);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(titleLabel, BorderLayout.NORTH);

        // Value (centered with more space)
        JPanel valuePanel = new JPanel(new GridBagLayout());
        valuePanel.setOpaque(false);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setVerticalAlignment(SwingConstants.CENTER);
        valuePanel.add(valueLabel);
        card.add(valuePanel, BorderLayout.CENTER);

        // Subtitle
        JLabel subtitleLabel = new JLabel(subtitle, SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(InventoryColorScheme.TEXT_SECONDARY);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        card.add(subtitleLabel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        // Actions title
        JLabel actionsTitle = new JLabel("Thao Tác Nhanh", SwingConstants.CENTER);
        actionsTitle.setFont(new Font("Arial", Font.BOLD, 20));
        actionsTitle.setForeground(new Color(37, 47, 63));
        panel.add(actionsTitle, BorderLayout.NORTH);

        // Action buttons with improved layout
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        buttonsPanel.setOpaque(false);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        buttonsPanel.add(newReceiptButton);
        buttonsPanel.add(viewInventoryButton);
        buttonsPanel.add(lowStockReportButton);
        buttonsPanel.add(expiryReportButton);

        panel.add(buttonsPanel, BorderLayout.CENTER);

        return panel;
    }

    private void setupEventListeners() {
        newReceiptButton.addActionListener(e -> parentPage.navigateToGoodsReceipt());
        viewInventoryButton.addActionListener(e -> parentPage.navigateToInventoryView());
        lowStockReportButton.addActionListener(e -> {
            parentPage.navigateToReports();
            // TODO: Auto-generate low stock report
        });
        expiryReportButton.addActionListener(e -> {
            parentPage.navigateToReports();
            // TODO: Auto-generate expiry report
        });
    }

    public void refreshData() {
        // TODO: Load actual data from database
        loadMockDashboardData();
    }

    private void loadMockDashboardData() {
        // Mock data for demonstration
        lowStockCountLabel.setText("7");
        expiringCountLabel.setText("12");
        totalValueLabel.setText(parentPage.getCurrencyFormatter().format(45600000) + " VNĐ");
        totalItemsLabel.setText("156");
        
        log.info("Dashboard metrics refreshed");
    }
} 