package BsK.client.ui.component.common;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class RoundedButtonUI extends BasicButtonUI {
    private final int cornerRadius;

    public RoundedButtonUI(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JButton button = (JButton) c;
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JButton button = (JButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = button.getWidth();
        int height = button.getHeight();
        g2.setColor(button.getBackground());
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
        g2.setColor(button.getForeground());
        g2.setFont(button.getFont());
        FontMetrics fm = g2.getFontMetrics();
        int stringWidth = fm.stringWidth(button.getText());
        int stringHeight = fm.getAscent();
        g2.drawString(button.getText(), (width - stringWidth) / 2, (height + stringHeight) / 2 - 2);
        g2.dispose();
    }
}