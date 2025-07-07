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

        // Test the new RTF scaling function
        System.out.println("\n--- Testing RTF Font Scaling ---");
        String rtfTestString = "{\\rtf1\\ansi{\\fonttbl{\\f0\\fnil Monospaced;}}{\\colortbl;\\red0\\green0\\blue0;}\\pard\\sa200\\sl276\\slmult1\\cf1\\b\\f0\\fs48 Hello World\\fs24 now small\\par}";
        System.out.println("Original RTF: " + rtfTestString);
        System.out.println("Scaled RTF:   " + scaleRtfFontSize(rtfTestString));
    }
}
