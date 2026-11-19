package BsK.common.util.text;

/**
 * Helpers for the CCCD/DDCN (citizen ID) field.
 * The field is optional: a blank value or a placeholder such as "0" / "000000000000"
 * means "unknown" and is stored as NULL instead of a fake number.
 */
public final class CccdUtils {

    private CccdUtils() {
    }

    /** Returns the trimmed value, or null when it is blank or only zeros (a placeholder). */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.matches("0+")) {
            return null;
        }
        return trimmed;
    }

    /** A real ID is 12 digits (CCCD) or 9 digits (old CMND) and not a single repeated digit. */
    public static boolean isValid(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        return normalized.matches("\\d{9}|\\d{12}") && !normalized.matches("(\\d)\\1+");
    }
}
