package BsK.client.ui.component.InventoryPage.common;

import java.awt.*;

/**
 * Centralized color scheme for the Inventory Management module.
 * Uses softer, professional tones for better readability and visual appeal.
 */
public class InventoryColorScheme {
    
    // Primary brand colors (softer, professional tones)
    public static final Color PRIMARY_BLUE = new Color(63, 81, 181);
    public static final Color PRIMARY_BLUE_LIGHT = new Color(92, 107, 192);
    public static final Color PRIMARY_BLUE_DARK = new Color(48, 63, 159);
    
    // Status colors (less saturated for easier viewing)
    public static final Color SUCCESS_GREEN = new Color(67, 160, 71);
    public static final Color SUCCESS_GREEN_LIGHT = new Color(232, 245, 233);
    public static final Color WARNING_ORANGE = new Color(255, 167, 38);
    public static final Color WARNING_ORANGE_LIGHT = new Color(255, 248, 225);
    public static final Color DANGER_RED = new Color(229, 57, 53);
    public static final Color DANGER_RED_LIGHT = new Color(255, 235, 238);
    public static final Color INFO_BLUE = new Color(33, 150, 243);
    public static final Color INFO_BLUE_LIGHT = new Color(227, 242, 253);
    
    // Neutral colors
    public static final Color GREY_50 = new Color(250, 250, 250);
    public static final Color GREY_100 = new Color(245, 245, 245);
    public static final Color GREY_200 = new Color(238, 238, 238);
    public static final Color GREY_300 = new Color(224, 224, 224);
    public static final Color GREY_400 = new Color(189, 189, 189);
    public static final Color GREY_500 = new Color(158, 158, 158);
    public static final Color GREY_600 = new Color(117, 117, 117);
    public static final Color GREY_700 = new Color(97, 97, 97);
    public static final Color GREY_800 = new Color(66, 66, 66);
    public static final Color GREY_900 = new Color(33, 33, 33);
    
    // Background colors
    public static final Color BACKGROUND_PRIMARY = Color.WHITE;
    public static final Color BACKGROUND_SECONDARY = GREY_50;
    public static final Color BACKGROUND_TERTIARY = GREY_100;
    
    // Text colors
    public static final Color TEXT_PRIMARY = new Color(37, 47, 63);
    public static final Color TEXT_SECONDARY = GREY_600;
    public static final Color TEXT_DISABLED = GREY_400;
    public static final Color TEXT_ON_DARK = Color.WHITE;
    
    // Border colors
    public static final Color BORDER_LIGHT = GREY_200;
    public static final Color BORDER_MEDIUM = GREY_300;
    public static final Color BORDER_DARK = GREY_400;
    
    // Dashboard card colors (softer backgrounds)
    public static final Color CARD_LOW_STOCK_BG = new Color(255, 242, 242);
    public static final Color CARD_LOW_STOCK_BORDER = new Color(229, 115, 115);
    public static final Color CARD_EXPIRING_BG = new Color(255, 248, 225);
    public static final Color CARD_EXPIRING_BORDER = new Color(255, 193, 7);
    public static final Color CARD_VALUE_BG = new Color(232, 245, 233);
    public static final Color CARD_VALUE_BORDER = new Color(102, 187, 106);
    public static final Color CARD_ITEMS_BG = new Color(232, 234, 246);
    public static final Color CARD_ITEMS_BORDER = new Color(92, 107, 192);
    
    // Table colors
    public static final Color TABLE_HEADER_BG = new Color(37, 47, 63);
    public static final Color TABLE_HEADER_TEXT = Color.WHITE;
    public static final Color TABLE_ROW_EVEN = Color.WHITE;
    public static final Color TABLE_ROW_ODD = new Color(248, 249, 250);
    public static final Color TABLE_ROW_SELECTED = new Color(232, 234, 246);
    public static final Color TABLE_ROW_HOVER = new Color(245, 245, 245);
    public static final Color TABLE_BORDER = GREY_200;
    
    // Status indicator colors in tables
    public static final Color STATUS_ACTIVE_BG = SUCCESS_GREEN_LIGHT;
    public static final Color STATUS_ACTIVE_TEXT = SUCCESS_GREEN;
    public static final Color STATUS_WARNING_BG = WARNING_ORANGE_LIGHT;
    public static final Color STATUS_WARNING_TEXT = WARNING_ORANGE;
    public static final Color STATUS_DANGER_BG = DANGER_RED_LIGHT;
    public static final Color STATUS_DANGER_TEXT = DANGER_RED;
    public static final Color STATUS_INFO_BG = INFO_BLUE_LIGHT;
    public static final Color STATUS_INFO_TEXT = INFO_BLUE;
    
    // Form colors
    public static final Color FORM_FIELD_BG = Color.WHITE;
    public static final Color FORM_FIELD_BORDER = BORDER_LIGHT;
    public static final Color FORM_FIELD_BORDER_FOCUS = PRIMARY_BLUE;
    public static final Color FORM_FIELD_DISABLED_BG = GREY_100;
    public static final Color FORM_FIELD_DISABLED_BORDER = GREY_300;
    
    // Navigation colors
    public static final Color NAV_BACKGROUND = new Color(37, 47, 63);
    public static final Color NAV_TEXT = Color.WHITE;
    public static final Color NAV_TEXT_INACTIVE = new Color(150, 150, 150);
    public static final Color NAV_HOVER = new Color(52, 62, 78);
    public static final Color NAV_ACTIVE = PRIMARY_BLUE;
    
    // Utility methods for creating variations
    
    /**
     * Creates a lighter version of the given color
     */
    public static Color lighten(Color color, float factor) {
        int red = Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor));
        int green = Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor));
        int blue = Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor));
        return new Color(red, green, blue, color.getAlpha());
    }
    
    /**
     * Creates a darker version of the given color
     */
    public static Color darken(Color color, float factor) {
        int red = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int green = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int blue = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(red, green, blue, color.getAlpha());
    }
    
    /**
     * Creates a color with specified opacity
     */
    public static Color withOpacity(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    
    /**
     * Gets status color based on string value
     */
    public static Color getStatusColor(String status) {
        if (status == null) return TEXT_SECONDARY;
        
        switch (status.toLowerCase()) {
            case "hoạt động":
            case "active":
            case "bình thường":
            case "normal":
                return STATUS_ACTIVE_TEXT;
            case "sắp hết":
            case "low":
            case "warning":
            case "cảnh báo":
                return STATUS_WARNING_TEXT;
            case "hết hàng":
            case "out":
            case "danger":
            case "nguy hiểm":
                return STATUS_DANGER_TEXT;
            case "tạm dừng":
            case "paused":
            case "info":
                return STATUS_INFO_TEXT;
            default:
                return TEXT_SECONDARY;
        }
    }
    
    /**
     * Gets status background color based on string value
     */
    public static Color getStatusBackgroundColor(String status) {
        if (status == null) return BACKGROUND_PRIMARY;
        
        switch (status.toLowerCase()) {
            case "hoạt động":
            case "active":
            case "bình thường":
            case "normal":
                return STATUS_ACTIVE_BG;
            case "sắp hết":
            case "low":
            case "warning":
            case "cảnh báo":
                return STATUS_WARNING_BG;
            case "hết hàng":
            case "out":
            case "danger":
            case "nguy hiểm":
                return STATUS_DANGER_BG;
            case "tạm dừng":
            case "paused":
            case "info":
                return STATUS_INFO_BG;
            default:
                return BACKGROUND_PRIMARY;
        }
    }
} 