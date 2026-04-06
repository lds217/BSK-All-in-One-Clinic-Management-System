package BsK.client.ui.component.common;

import BsK.client.network.ConnectionManager;
import BsK.client.network.ConnectionManager.ConnectionState;
import BsK.client.network.ConnectionManager.ConnectionStateListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;

/**
 * Glass pane overlay that blocks UI interaction when connection is lost.
 * Shows connection status, retry countdown, and manual retry button.
 */
public class ConnectionStatusOverlay extends JPanel implements ConnectionStateListener {
    
    private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 180);
    private static final Color CARD_BACKGROUND = new Color(45, 45, 48);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color ACCENT_COLOR = new Color(0, 122, 204);
    private static final Color BUTTON_HOVER_COLOR = new Color(0, 150, 230);
    
    private final JLabel statusLabel;
    private final JLabel messageLabel;
    private final JLabel countdownLabel;
    private final JButton retryButton;
    private final SpinnerPanel spinnerPanel;
    
    private volatile boolean showSpinner = true;
    private volatile int countdownSeconds = 0;
    private volatile int attemptNumber = 0;
    
    public ConnectionStatusOverlay() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        
        // Block all mouse events on the overlay
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                e.consume();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                e.consume();
            }
        });
        addMouseMotionListener(new MouseAdapter() {});
        
        // Create center card panel
        JPanel cardPanel = createCardPanel();
        add(cardPanel);
        
        // Initialize components
        spinnerPanel = new SpinnerPanel();
        statusLabel = createLabel("Đang kết nối...", 18, Font.BOLD);
        messageLabel = createLabel("Vui lòng đợi trong giây lát", 14, Font.PLAIN);
        countdownLabel = createLabel("", 12, Font.PLAIN);
        countdownLabel.setForeground(new Color(180, 180, 180));
        
        retryButton = createRetryButton();
        
        // Layout components in card
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 20, 10, 20);
        
        cardPanel.add(spinnerPanel, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(15, 20, 5, 20);
        cardPanel.add(statusLabel, gbc);
        
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 20, 5, 20);
        cardPanel.add(messageLabel, gbc);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 20, 10, 20);
        cardPanel.add(countdownLabel, gbc);
        
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 20, 20, 20);
        cardPanel.add(retryButton, gbc);
        
        // Initially hidden until state is set
        retryButton.setVisible(false);
        countdownLabel.setVisible(false);
        
        // Register as listener
        ConnectionManager.getInstance().addListener(this);
    }
    
    private JPanel createCardPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(400, 280));
        return card;
    }
    
    private JLabel createLabel(String text, int size, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(TEXT_COLOR);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
    
    private JButton createRetryButton() {
        JButton button = new JButton("Thử lại ngay") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(ACCENT_COLOR.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(BUTTON_HOVER_COLOR);
                } else {
                    g2.setColor(ACCENT_COLOR);
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                
                g2.dispose();
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(150, 40));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addActionListener(e -> {
            ConnectionManager.getInstance().retryNow();
        });
        
        return button;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(OVERLAY_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
    
    @Override
    public void onStateChanged(ConnectionState newState, String message) {
        SwingUtilities.invokeLater(() -> {
            switch (newState) {
                case DISCONNECTED -> {
                    setVisible(true);
                    statusLabel.setText("Chưa kết nối");
                    messageLabel.setText("Ứng dụng đang chờ kết nối đến máy chủ");
                    showSpinner = false;
                    retryButton.setVisible(true);
                    countdownLabel.setVisible(false);
                }
                case CONNECTING -> {
                    setVisible(true);
                    statusLabel.setText("Đang kết nối...");
                    messageLabel.setText(message);
                    showSpinner = true;
                    retryButton.setVisible(false);
                    countdownLabel.setVisible(false);
                }
                case CONNECTED -> {
                    setVisible(false);
                    showSpinner = false;
                }
                case RECONNECTING -> {
                    setVisible(true);
                    statusLabel.setText("Đang kết nối lại...");
                    messageLabel.setText(message);
                    showSpinner = true;
                    retryButton.setVisible(true);
                    countdownLabel.setVisible(true);
                }
            }
            spinnerPanel.setSpinning(showSpinner);
            repaint();
        });
    }
    
    @Override
    public void onReconnectCountdown(int secondsRemaining, int attemptNumber) {
        SwingUtilities.invokeLater(() -> {
            this.countdownSeconds = secondsRemaining;
            this.attemptNumber = attemptNumber;
            countdownLabel.setText(String.format("Thử lại sau %d giây (lần thử %d)", 
                secondsRemaining, attemptNumber));
            countdownLabel.setVisible(true);
        });
    }
    
    /**
     * Inner panel that shows a spinning animation
     */
    private class SpinnerPanel extends JPanel {
        private volatile boolean spinning = true;
        private int angle = 0;
        private Timer timer;
        
        public SpinnerPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(60, 60));
            
            timer = new Timer(30, e -> {
                if (spinning) {
                    angle = (angle + 10) % 360;
                    repaint();
                }
            });
            timer.start();
        }
        
        public void setSpinning(boolean spinning) {
            this.spinning = spinning;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int size = 50;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            
            if (spinning) {
                // Draw spinning arc
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(80, 80, 85));
                g2.drawOval(x, y, size, size);
                
                g2.setColor(ACCENT_COLOR);
                g2.draw(new Arc2D.Double(x, y, size, size, angle, 90, Arc2D.OPEN));
            } else {
                // Draw static circle with X
                g2.setStroke(new BasicStroke(3));
                g2.setColor(new Color(200, 80, 80));
                g2.drawOval(x, y, size, size);
                
                int padding = 15;
                g2.drawLine(x + padding, y + padding, x + size - padding, y + size - padding);
                g2.drawLine(x + size - padding, y + padding, x + padding, y + size - padding);
            }
            
            g2.dispose();
        }
    }
    
    /**
     * Cleanup when overlay is no longer needed
     */
    public void cleanup() {
        ConnectionManager.getInstance().removeListener(this);
    }
}

