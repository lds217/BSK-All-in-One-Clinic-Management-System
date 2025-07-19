package BsK.common.util.date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class DateUtils {
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    
    /**
     * Extracts the year from a given timestamp string in milliseconds.
     *
     * @param timestampStr the timestamp as a string (e.g., "1085875200000")
     * @return the year as an integer, or -1 if the input is invalid
     */
    public static int extractYearFromTimestamp(String timestampStr) {
        try {
            // Parse the string to a long value
            long timestamp = Long.parseLong(timestampStr);
            // Convert the timestamp to a LocalDateTime
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            return dateTime.getYear(); // Return the year
        } catch (NumberFormatException e) {
            // Handle invalid input
            System.err.println("Invalid timestamp: " + timestampStr);
            return -1; // Return -1 to indicate an error
        }
    }

    /**
     * Converts a date input (timestamp string or dd/MM/yyyy string) to display format (dd/MM/yyyy).
     *
     * @param dateInput the date input - can be a timestamp string or already formatted date string
     * @return the date in dd/MM/yyyy format, or the original string if conversion fails
     */
    public static String convertToDisplayFormat(String dateInput) {
        if (dateInput == null || dateInput.trim().isEmpty()) {
            return "";
        }
        
        try {
            // Check if it's a timestamp (all digits)
            if (dateInput.matches("\\d+")) {
                Date date = new Date(Long.parseLong(dateInput));
                return DISPLAY_FORMAT.format(date);
            } else {
                // If it's already in date format, try to parse and reformat to ensure consistency
                Date date = DISPLAY_FORMAT.parse(dateInput);
                return DISPLAY_FORMAT.format(date);
            }
        } catch (NumberFormatException | ParseException e) {
            // If parsing fails, return the original string
            System.err.println("Invalid date format: " + dateInput);
            return dateInput;
        }
    }

    /**
     * Converts a date input (timestamp string or dd/MM/yyyy string) to Date object.
     *
     * @param dateInput the date input - can be a timestamp string or dd/MM/yyyy formatted string
     * @return the Date object, or null if conversion fails
     */
    public static Date convertToDate(String dateInput) {
        if (dateInput == null || dateInput.trim().isEmpty()) {
            // return plain date
            return new Date();
        }
        
        try {
            // Check if it's a timestamp (all digits)
            if (dateInput.matches("\\d+")) {
                return new Date(Long.parseLong(dateInput));
            } else {
                // Try to parse as dd/MM/yyyy format
                return DISPLAY_FORMAT.parse(dateInput);
                
            }
        } catch (NumberFormatException | ParseException e) {
            System.err.println("Invalid date format: " + dateInput);
            return new Date();
        }
    }

    /**
     * Converts a Date object to display format (dd/MM/yyyy).
     *
     * @param date the Date object to convert
     * @return the date in dd/MM/yyyy format, or empty string if date is null
     */
    public static String formatToDisplay(Date date) {
        if (date == null) {
            return "";
        }
        return DISPLAY_FORMAT.format(date);
    }

    public static void main(String[] args) {
        // Test the method with valid and invalid input
        String validTimestamp = "1085875200000";
        String invalidTimestamp = "invalid";

        int validYear = DateUtils.extractYearFromTimestamp(validTimestamp);
        System.out.println("Year: " + validYear); // Output: Year: 2004

        int invalidYear = DateUtils.extractYearFromTimestamp(invalidTimestamp);
        System.out.println("Year: " + invalidYear); // Output: Year: -1
        
        // Test new utility methods
        System.out.println("Display format: " + convertToDisplayFormat(validTimestamp));
        System.out.println("Display format: " + convertToDisplayFormat("15/06/2023"));
        System.out.println("Date object: " + convertToDate(validTimestamp));
        System.out.println("Date object: " + convertToDate("15-06-2023"));

    }
}
