package BsK.common.util.text;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtils {

    // Private constructor to prevent instantiation
    private TextUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Removes accents from a given string.
     *
     * @param text The input string.
     * @return A string without accents.
     */
    public static String removeAccents(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText =  Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        normalizedText = normalizedText.replaceAll("Đ", "D")
                .replaceAll("đ", "d");

        return normalizedText;
    }

    /**
     * Converts Vietnamese patient name to English format suitable for file/folder names.
     * Removes accents, special characters, and formats for use in Google Drive folder names.
     *
     * @param vietnameseName The Vietnamese patient name (e.g., "Trần Văn A", "Nguyễn Thị B")
     * @return A sanitized English name suitable for folder names (e.g., "Tran_Van_A", "Nguyen_Thi_B")
     */
    public static String vietnameseToEnglishName(String vietnameseName) {
        if (vietnameseName == null || vietnameseName.trim().isEmpty()) {
            return "Unknown_Patient";
        }

        // First remove accents
        String englishName = removeAccents(vietnameseName.trim());
        
        // Replace spaces with underscores
        englishName = englishName.replaceAll("\\s+", "_");
        
        // Remove any characters that are not letters, numbers, underscores, dots, or hyphens
        englishName = englishName.replaceAll("[^a-zA-Z0-9._-]", "");
        
        // Ensure it doesn't start or end with underscore
        englishName = englishName.replaceAll("^_+|_+$", "");
        
        // Replace multiple underscores with single underscore
        englishName = englishName.replaceAll("_+", "_");
        
        // If empty after sanitization, return default
        if (englishName.isEmpty()) {
            return "Unknown_Patient";
        }
        
        return englishName;
    }

    /**
     * Creates a standardized patient folder name for Google Drive.
     * Format: "Patient_[ID]_[EnglishName]"
     *
     * @param patientId The patient ID
     * @param patientLastName Patient's last name in Vietnamese
     * @param patientFirstName Patient's first name in Vietnamese
     * @return A standardized folder name (e.g., "Patient_123_Tran_Van_A")
     */
    public static String createPatientFolderName(int patientId, String patientLastName, String patientFirstName) {
        String fullName = (patientLastName + " " + patientFirstName).trim();
        String englishName = vietnameseToEnglishName(fullName);
        return "Patient_" + patientId + "_" + englishName;
    }

    /**
     * Creates a standardized checkup folder name for Google Drive.
     * Format: "Kham_benh_[YYYYMMDD]_[EnglishPatientName]"
     *
     * @param patientLastName Patient's last name in Vietnamese
     * @param patientFirstName Patient's first name in Vietnamese
     * @return A standardized checkup folder name (e.g., "Kham_benh_20241218_Tran_Van_A")
     */
    public static String createCheckupFolderName(String patientLastName, String patientFirstName) {
        // Get today's date in YYYYMMDD format
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Create English patient name
        String fullName = (patientLastName + " " + patientFirstName).trim();
        String englishName = vietnameseToEnglishName(fullName);
        
        return "Kham_benh_" + dateStr + "_" + englishName;
    }

    /**
     * Creates a checkup folder name with specific checkup ID.
     * Format: "Checkup_[ID]_[YYYYMMDD]_[EnglishPatientName]"
     *
     * @param checkupId The checkup ID
     * @param patientLastName Patient's last name in Vietnamese
     * @param patientFirstName Patient's first name in Vietnamese
     * @return A standardized checkup folder name (e.g., "Checkup_123_20241218_Tran_Van_A")
     */
    public static String createCheckupFolderNameWithId(int checkupId, String patientLastName, String patientFirstName) {
        // Get today's date in YYYYMMDD format
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Create English patient name
        String fullName = (patientLastName + " " + patientFirstName).trim();
        String englishName = vietnameseToEnglishName(fullName);
        
        return "Checkup_" + checkupId + "_" + dateStr + "_" + englishName;
    }

    /**
     * Scales down the font size definitions in an RTF string by half.
     * Finds all occurrences of \fsXX and replaces them with \fs(XX/2).
     * This is useful when JTextPane generates RTF with large font sizes
     * that need to be normalized for printing with JasperReports.
     *
     * @param rtfContent The input RTF string.
     * @return A new RTF string with font sizes scaled down.
     */
    public static String scaleRtfFontSize(String rtfContent) {
        if (rtfContent == null) {
            return null;
        }

        // Regex to find \fs followed by one or more digits.
        // The double backslash is needed to create a literal backslash in the regex string.
        Pattern pattern = Pattern.compile("\\\\fs(\\d+)");
        Matcher matcher = pattern.matcher(rtfContent);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            try {
                // Get the number string from the first capturing group.
                String sizeStr = matcher.group(1);
                int originalSize = Integer.parseInt(sizeStr);
                
                // The font size in RTF is in half-points. JTextPane seems to generate
                // sizes that are double what is visually represented. Dividing by 2
                // brings it to the expected point size for JasperReports.
                int newSize = originalSize / 2;
                
                // Append the replacement string. We need to escape the backslash for appendReplacement.
                matcher.appendReplacement(sb, "\\\\fs" + newSize);
            } catch (NumberFormatException e) {
                // This should not happen with the given regex, but it's safe to handle.
                // In case of an error, just append the original matched string.
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static void main(String[] args) {
        String[] testWords = {
                "Đạt", "đạt", "Ân", "Ấn", "Lê", "Hồ", "Trần", "Hoàng",
                "Quốc", "Phạm", "Dũng", "Bạch", "Hạnh", "Cường", "Vỹ"
        };

        for (String word : testWords) {
            System.out.println("Original: " + word + " → Without Accents: " + TextUtils.removeAccents(word));
        }

        // Test Vietnamese to English name conversion
        System.out.println("\n--- Testing Vietnamese Name Conversion ---");
        String[] testPatientNames = {
                "Trần Văn A", "Nguyễn Thị B", "Lê Hoàng C", "Phạm Quốc Dũng",
                "Bùi Thị Hạnh", "Đặng Minh Cường", "Võ Thế Vỹ", "Hoàng Gia Bảo"
        };

        for (String name : testPatientNames) {
            String englishName = vietnameseToEnglishName(name);
            String[] nameParts = name.split(" ", 2);
            String lastName = nameParts[0];
            String firstName = nameParts.length > 1 ? nameParts[1] : "";
            
            String patientFolderName = createPatientFolderName(123, lastName, firstName);
            String checkupFolderName = createCheckupFolderName(lastName, firstName);
            String checkupFolderWithId = createCheckupFolderNameWithId(456, lastName, firstName);
            
            System.out.println("Vietnamese: " + name);
            System.out.println("  → English: " + englishName);
            System.out.println("  → Patient Folder: " + patientFolderName);
            System.out.println("  → Checkup Folder: " + checkupFolderName);
            System.out.println("  → Checkup Folder (with ID): " + checkupFolderWithId);
            System.out.println();
        }

        // Test the new RTF scaling function
        System.out.println("\n--- Testing RTF Font Scaling ---");
        String rtfTestString = "{\\rtf1\\ansi{\\fonttbl{\\f0\\fnil Monospaced;}}{\\colortbl;\\red0\\green0\\blue0;}\\pard\\sa200\\sl276\\slmult1\\cf1\\b\\f0\\fs48 Hello World\\fs24 now small\\par}";
        System.out.println("Original RTF: " + rtfTestString);
        System.out.println("Scaled RTF:   " + scaleRtfFontSize(rtfTestString));
    }
}
