package BsK.common.util.text;

import java.text.Normalizer;

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

    public static void main(String[] args) {
        String[] testWords = {
                "Đạt", "đạt", "Ân", "Ấn", "Lê", "Hồ", "Trần", "Hoàng",
                "Quốc", "Phạm", "Dũng", "Bạch", "Hạnh", "Cường", "Vỹ"
        };



        for (String word : testWords) {
            System.out.println("Original: " + word + " → Without Accents: " + TextUtils.removeAccents(word));
        }
    }
}
