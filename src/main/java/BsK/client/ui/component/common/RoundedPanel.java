package BsK.client.ui.component.common;

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {
    private int cornerRadius;
    private Color fillColor;
    private boolean gradient;

    public RoundedPanel(int radius, Color fillColor, boolean gradient) {
        super();
        this.cornerRadius = radius;
        this.fillColor = fillColor;
        this.gradient = gradient;
        setOpaque(false); // Ensures background is transparent outside the rounded area
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Create a gradient paint from white to the fillColor
        GradientPaint gradientColor = new GradientPaint(
                -100, -100, Color.WHITE, width, height, fillColor
        );

        // Apply the gradient as the fill paint
        if(gradient)
            graphics.setPaint(gradientColor);
        else
            graphics.setColor(fillColor);
        graphics.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // Draw the rounded border
       // graphics.setColor(getForeground());
       // graphics.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
    }
}
