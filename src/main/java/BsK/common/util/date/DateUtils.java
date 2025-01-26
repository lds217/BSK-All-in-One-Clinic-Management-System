package BsK.common.util.date;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtils {
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

    public static void main(String[] args) {
        // Test the method with valid and invalid input
        String validTimestamp = "1085875200000";
        String invalidTimestamp = "invalid";

        int validYear = DateUtils.extractYearFromTimestamp(validTimestamp);
        System.out.println("Year: " + validYear); // Output: Year: 2004

        int invalidYear = DateUtils.extractYearFromTimestamp(invalidTimestamp);
        System.out.println("Year: " + invalidYear); // Output: Year: -1
    }
}
