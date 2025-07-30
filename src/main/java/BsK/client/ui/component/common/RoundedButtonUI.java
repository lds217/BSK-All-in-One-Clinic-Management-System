package BsK.client.ui.component.common;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RoundedButtonUI extends BasicButtonUI {
    private final int cornerRadius;
    private final Color backgroundColor;
    private final Color foregroundColor;
    private final Color borderColor;

    public RoundedButtonUI(Color backgroundColor, Color foregroundColor, int cornerRadius) {
        this(backgroundColor, foregroundColor, null, cornerRadius);
    }

    public RoundedButtonUI(Color backgroundColor, Color foregroundColor, Color borderColor, int cornerRadius) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.borderColor = borderColor;
        this.cornerRadius = cornerRadius;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JButton button = (JButton) c;
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(backgroundColor);
        button.setForeground(foregroundColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JButton button = (JButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bgColor = button.getBackground();
        if (button.getModel().isRollover()) {
            if (bgColor.getAlpha() == 0) {
                bgColor = new Color(255, 255, 255, 50);
            } else {
                bgColor = bgColor.darker();
            }
        }

        if (bgColor.getAlpha() > 0) {
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), cornerRadius, cornerRadius);
        }

        if (borderColor != null) {
            Color actualBorderColor = borderColor;
            if (button.getModel().isRollover()) {
                actualBorderColor = borderColor.brighter();
            }
            g2.setColor(actualBorderColor);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, cornerRadius, cornerRadius);
        }

        g2.setColor(button.getForeground());
        g2.setFont(button.getFont());
        FontMetrics fm = g2.getFontMetrics();
        int stringWidth = fm.stringWidth(button.getText());
        int stringHeight = fm.getAscent();
        g2.drawString(button.getText(), (stringWidth > 0 ? (c.getWidth() - stringWidth) / 2 : 0), (c.getHeight() + fm.getAscent()) / 2 - 2);

        g2.dispose();
    }
}
