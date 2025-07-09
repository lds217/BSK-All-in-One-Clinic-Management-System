package BsK.client.ui.component.InventoryPage.common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Centralized button factory for consistent UI/UX across the Inventory Management module.
 * Defines standardized button styles for different action types.
 */
public class InventoryButtonFactory {
    
    // Brand color palette (softer, professional tones)
    public static final Color PRIMARY_COLOR = new Color(63, 81, 181);      // Professional blue
    public static final Color PRIMARY_HOVER = new Color(48, 63, 159);      // Darker blue on hover
    public static final Color SECONDARY_COLOR = new Color(158, 158, 158);   // Neutral grey
    public static final Color SECONDARY_HOVER = new Color(117, 117, 117);   // Darker grey on hover
    public static final Color DESTRUCTIVE_COLOR = new Color(229, 57, 53);   // Softer red
    public static final Color DESTRUCTIVE_HOVER = new Color(198, 40, 40);   // Darker red on hover
    public static final Color SPECIAL_COLOR = new Color(97, 97, 97);        // Dark grey
    public static final Color SPECIAL_HOVER = new Color(66, 66, 66);        // Darker grey on hover
    public static final Color SUCCESS_COLOR = new Color(67, 160, 71);       // Softer green
    public static final Color SUCCESS_HOVER = new Color(56, 142, 60);       // Darker green on hover
    
    // Standard button dimensions
    public static final Dimension STANDARD_BUTTON_SIZE = new Dimension(140, 40);
    public static final Dimension LARGE_BUTTON_SIZE = new Dimension(180, 45);
    public static final Dimension SMALL_BUTTON_SIZE = new Dimension(100, 35);
    
    // Font configurations
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    public static final Font LARGE_BUTTON_FONT = new Font("Arial", Font.BOLD, 16);
    public static final Font SMALL_BUTTON_FONT = new Font("Arial", Font.BOLD, 12);

    /**
     * Creates a primary action button (Save, Create, Confirm)
     * Solid background with white text
     */
    public static JButton createPrimaryButton(String text) {
        return createStandardButton(text, PRIMARY_COLOR, PRIMARY_HOVER, Color.WHITE, BUTTON_FONT, STANDARD_BUTTON_SIZE);
    }
    
    public static JButton createPrimaryButton(String text, Dimension size) {
        Font font = size.equals(LARGE_BUTTON_SIZE) ? LARGE_BUTTON_FONT : 
                   size.equals(SMALL_BUTTON_SIZE) ? SMALL_BUTTON_FONT : BUTTON_FONT;
        return createStandardButton(text, PRIMARY_COLOR, PRIMARY_HOVER, Color.WHITE, font, size);
    }

    /**
     * Creates a secondary action button (Cancel, Close)
     * Grey background or outline with colored text
     */
    public static JButton createSecondaryButton(String text) {
        return createStandardButton(text, SECONDARY_COLOR, SECONDARY_HOVER, Color.WHITE, BUTTON_FONT, STANDARD_BUTTON_SIZE);
    }
    
    public static JButton createSecondaryButton(String text, Dimension size) {
        Font font = size.equals(LARGE_BUTTON_SIZE) ? LARGE_BUTTON_FONT : 
                   size.equals(SMALL_BUTTON_SIZE) ? SMALL_BUTTON_FONT : BUTTON_FONT;
        return createStandardButton(text, SECONDARY_COLOR, SECONDARY_HOVER, Color.WHITE, font, size);
    }

    /**
     * Creates a destructive action button (Delete, Remove)
     * Solid red background with white text
     */
    public static JButton createDestructiveButton(String text) {
        return createStandardButton(text, DESTRUCTIVE_COLOR, DESTRUCTIVE_HOVER, Color.WHITE, BUTTON_FONT, STANDARD_BUTTON_SIZE);
    }
    
    public static JButton createDestructiveButton(String text, Dimension size) {
        Font font = size.equals(LARGE_BUTTON_SIZE) ? LARGE_BUTTON_FONT : 
                   size.equals(SMALL_BUTTON_SIZE) ? SMALL_BUTTON_FONT : BUTTON_FONT;
        return createStandardButton(text, DESTRUCTIVE_COLOR, DESTRUCTIVE_HOVER, Color.WHITE, font, size);
    }

    /**
     * Creates a special action button (Print, Export)
     * Dark grey background with white text
     */
    public static JButton createSpecialButton(String text) {
        return createStandardButton(text, SPECIAL_COLOR, SPECIAL_HOVER, Color.WHITE, BUTTON_FONT, STANDARD_BUTTON_SIZE);
    }
    
    public static JButton createSpecialButton(String text, Dimension size) {
        Font font = size.equals(LARGE_BUTTON_SIZE) ? LARGE_BUTTON_FONT : 
                   size.equals(SMALL_BUTTON_SIZE) ? SMALL_BUTTON_FONT : BUTTON_FONT;
        return createStandardButton(text, SPECIAL_COLOR, SPECIAL_HOVER, Color.WHITE, font, size);
    }

    /**
     * Creates a success action button (Add, Create New)
     * Solid green background with white text
     */
    public static JButton createSuccessButton(String text) {
        return createStandardButton(text, SUCCESS_COLOR, SUCCESS_HOVER, Color.WHITE, BUTTON_FONT, STANDARD_BUTTON_SIZE);
    }
    
    public static JButton createSuccessButton(String text, Dimension size) {
        Font font = size.equals(LARGE_BUTTON_SIZE) ? LARGE_BUTTON_FONT : 
                   size.equals(SMALL_BUTTON_SIZE) ? SMALL_BUTTON_FONT : BUTTON_FONT;
        return createStandardButton(text, SUCCESS_COLOR, SUCCESS_HOVER, Color.WHITE, font, size);
    }

    /**
     * Creates an outline button (secondary style with border)
     * Transparent background with colored border and text
     */
    public static JButton createOutlineButton(String text, Color borderColor) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(borderColor);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(STANDARD_BUTTON_SIZE);
        button.setMinimumSize(STANDARD_BUTTON_SIZE);
        button.setMaximumSize(STANDARD_BUTTON_SIZE);
        
        // Hover effect for outline buttons
        Color hoverBackground = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 20);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverBackground);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(Color.WHITE);
            }
        });
        
        return button;
    }

    /**
     * Creates an icon-only button for use in tables and compact areas
     */
    public static JButton createIconButton(String icon, Color backgroundColor, Color hoverColor, String tooltip) {
        JButton button = new JButton(icon);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(35, 30));
        button.setMinimumSize(new Dimension(35, 30));
        button.setMaximumSize(new Dimension(35, 30));
        
        if (tooltip != null && !tooltip.isEmpty()) {
            button.setToolTipText(tooltip);
        }
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(backgroundColor);
                }
            }
        });
        
        return button;
    }

    /**
     * Core method for creating standardized buttons
     */
    private static JButton createStandardButton(String text, Color backgroundColor, Color hoverColor, 
                                              Color textColor, Font font, Dimension size) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(backgroundColor);
        button.setForeground(textColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(backgroundColor);
                } else {
                    button.setBackground(new Color(200, 200, 200)); // Disabled color
                }
            }
        });
        
        // Disabled state styling
        button.addPropertyChangeListener("enabled", evt -> {
            if ((Boolean) evt.getNewValue()) {
                button.setBackground(backgroundColor);
                button.setForeground(textColor);
            } else {
                button.setBackground(new Color(200, 200, 200));
                button.setForeground(new Color(150, 150, 150));
            }
        });
        
        return button;
    }

    /**
     * Creates a button group panel with standardized spacing
     */
    public static JPanel createButtonGroup(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.setOpaque(false);
        
        for (JButton button : buttons) {
            panel.add(button);
        }
        
        return panel;
    }

    /**
     * Creates a button group panel with custom alignment
     */
    public static JPanel createButtonGroup(int alignment, JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(alignment, 10, 0));
        panel.setOpaque(false);
        
        for (JButton button : buttons) {
            panel.add(button);
        }
        
        return panel;
    }

    /**
     * Creates a split button group (left and right aligned)
     */
    public static JPanel createSplitButtonGroup(JButton[] leftButtons, JButton[] rightButtons) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        if (leftButtons != null && leftButtons.length > 0) {
            JPanel leftPanel = createButtonGroup(FlowLayout.LEFT, leftButtons);
            panel.add(leftPanel, BorderLayout.WEST);
        }
        
        if (rightButtons != null && rightButtons.length > 0) {
            JPanel rightPanel = createButtonGroup(FlowLayout.RIGHT, rightButtons);
            panel.add(rightPanel, BorderLayout.EAST);
        }
        
        return panel;
    }

    // Standard icon constants for common actions
    public static final String ICON_DELETE = "🗑️";
    public static final String ICON_EDIT = "✏️";
    public static final String ICON_VIEW = "👁️";
    public static final String ICON_PRINT = "🖨️";
    public static final String ICON_EXPORT = "📤";
    public static final String ICON_SEARCH = "🔍";
    public static final String ICON_ADD = "➕";
    public static final String ICON_SAVE = "💾";
    public static final String ICON_CANCEL = "❌";
    public static final String ICON_REFRESH = "🔄";
    public static final String ICON_SETTINGS = "⚙️";
    public static final String ICON_REPORT = "📊";
    public static final String ICON_WARNING = "⚠️";
    public static final String ICON_SUCCESS = "✅";
    public static final String ICON_INFO = "ℹ️";
} 