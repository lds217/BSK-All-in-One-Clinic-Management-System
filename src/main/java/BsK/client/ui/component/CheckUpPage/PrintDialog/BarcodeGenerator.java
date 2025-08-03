package BsK.client.ui.component.CheckUpPage.PrintDialog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
public class BarcodeGenerator {

    // Fixed paths for saving images
    private static final String BARCODE_DIRECTORY = "temp/barcodes";
    private static final String QRCODE_DIRECTORY = "temp/qrcodes";
    
    // Ensure directories exist
    static {
        try {
            Files.createDirectories(Paths.get(BARCODE_DIRECTORY));
            Files.createDirectories(Paths.get(QRCODE_DIRECTORY));
            log.info("Created barcode directories: {} and {}", BARCODE_DIRECTORY, QRCODE_DIRECTORY);
        } catch (IOException e) {
            log.error("Failed to create barcode directories", e);
        }
    }

    /**
     * Generates a Code 128 barcode, saves it to a fixed path, and returns the file path.
     * File is saved as: temp/barcodes/barcode_{text}_{timestamp}.png
     */
    public static String generateCode128AndSave(String text, int width, int height) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode text cannot be null or empty");
        }
        
        log.info("Generating Code 128 barcode and saving to file for text: {}", text);
        
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Create filename with timestamp to avoid conflicts
        String sanitizedText = sanitizeFileName(text);
        String filename = String.format("barcode_%s_%d.png", sanitizedText, System.currentTimeMillis());
        String filePath = Paths.get(BARCODE_DIRECTORY, filename).toString();
        
        // Save image to file
        File outputFile = new File(filePath);
        ImageIO.write(image, "PNG", outputFile);
        
        log.info("Successfully saved Code 128 barcode to: {}", filePath);
        return filePath;
    }

    /**
     * Generates a QR Code, saves it to a fixed path, and returns the file path.
     * File is saved as: temp/qrcodes/qrcode_{text}_{timestamp}.png
     */
    public static String generateQRCodeAndSave(String text, int width, int height) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code text cannot be null or empty");
        }
        
        log.info("Generating QR Code and saving to file for text: {}", text);
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Create filename with timestamp to avoid conflicts
        String sanitizedText = sanitizeFileName(text);
        String filename = String.format("qrcode_%s_%d.png", sanitizedText, System.currentTimeMillis());
        String filePath = Paths.get(QRCODE_DIRECTORY, filename).toString();
        
        // Save image to file
        File outputFile = new File(filePath);
        ImageIO.write(image, "PNG", outputFile);
        
        log.info("Successfully saved QR Code to: {}", filePath);
        return filePath;
    }

    /**
     * Generates a Code 128 barcode with custom directory, saves it, and returns the file path.
     */
    public static String generateCode128AndSave(String text, int width, int height, String customDirectory) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode text cannot be null or empty");
        }
        
        // Ensure custom directory exists
        Files.createDirectories(Paths.get(customDirectory));
        
        log.info("Generating Code 128 barcode and saving to custom directory: {}", customDirectory);
        
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Create filename
        String sanitizedText = sanitizeFileName(text);
        String filename = String.format("barcode_%s_%d.png", sanitizedText, System.currentTimeMillis());
        String filePath = Paths.get(customDirectory, filename).toString();
        
        // Save image to file
        File outputFile = new File(filePath);
        ImageIO.write(image, "PNG", outputFile);
        
        log.info("Successfully saved Code 128 barcode to: {}", filePath);
        return filePath;
    }

    /**
     * Generates a QR Code with custom directory, saves it, and returns the file path.
     */
    public static String generateQRCodeAndSave(String text, int width, int height, String customDirectory) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code text cannot be null or empty");
        }
        
        // Ensure custom directory exists
        Files.createDirectories(Paths.get(customDirectory));
        
        log.info("Generating QR Code and saving to custom directory: {}", customDirectory);
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Create filename
        String sanitizedText = sanitizeFileName(text);
        String filename = String.format("qrcode_%s_%d.png", sanitizedText, System.currentTimeMillis());
        String filePath = Paths.get(customDirectory, filename).toString();
        
        // Save image to file
        File outputFile = new File(filePath);
        ImageIO.write(image, "PNG", outputFile);
        
        log.info("Successfully saved QR Code to: {}", filePath);
        return filePath;
    }

    /**
     * Generates a Code 128 barcode with fixed filename (overwrites existing), saves it, and returns the file path.
     * Useful when you want to reuse the same filename for reports.
     */
    public static String generateCode128WithFixedName(String text, int width, int height, String filename) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode text cannot be null or empty");
        }
        
        log.info("Generating Code 128 barcode with fixed filename: {}", filename);
        
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Ensure filename has .png extension
        if (!filename.toLowerCase().endsWith(".png")) {
            filename += ".png";
        }
        
        String filePath = Paths.get(BARCODE_DIRECTORY, filename).toString();
        
        // Save image to file (will overwrite if exists)
        File outputFile = new File(filePath);
        ImageIO.write(image, "PNG", outputFile);
        
        log.info("Successfully saved Code 128 barcode to: {}", filePath);
        return filePath;
    }

    /**
     * Generates a QR Code with fixed filename (overwrites existing), saves it, and returns the file path.
     * Useful when you want to reuse the same filename for reports.
     */
    public static String generateQRCodeWithFixedName(String text, int width, int height, String filename) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code text cannot be null or empty");
        }
        
        log.info("Generating QR Code with fixed filename: {}", filename);
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Ensure filename has .png extension
        if (!filename.toLowerCase().endsWith(".png")) {
            filename += ".png";
        }
        
        String filePath = Paths.get(QRCODE_DIRECTORY, filename).toString();
        
        // Save image to file (will overwrite if exists)
        File outputFile = new File(filePath);
        ImageIO.write(image, "PNG", outputFile);
        
        log.info("Successfully saved QR Code to: {}", filePath);
        return filePath;
    }

    /**
     * Utility method to clean up temporary barcode files older than specified minutes
     */
    public static void cleanupOldBarcodeFiles(int olderThanMinutes) {
        cleanupOldFiles(BARCODE_DIRECTORY, olderThanMinutes);
        cleanupOldFiles(QRCODE_DIRECTORY, olderThanMinutes);
    }

    /**
     * Helper method to clean up old files in a directory
     */
    private static void cleanupOldFiles(String directory, int olderThanMinutes) {
        try {
            File dir = new File(directory);
            if (!dir.exists()) return;
            
            long cutoffTime = System.currentTimeMillis() - (olderThanMinutes * 60 * 1000L);
            
            File[] files = dir.listFiles((file) -> 
                file.isFile() && 
                file.getName().toLowerCase().endsWith(".png") && 
                file.lastModified() < cutoffTime
            );
            
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        log.debug("Deleted old barcode file: {}", file.getName());
                    }
                }
                log.info("Cleaned up {} old files from {}", files.length, directory);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup old files in directory: {}", directory, e);
        }
    }

    /**
     * Helper method to sanitize text for use in filenames
     */
    private static String sanitizeFileName(String text) {
        if (text == null) return "empty";
        
        // Replace invalid filename characters and limit length
        String sanitized = text.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return sanitized;
    }

    // ============ ORIGINAL METHODS KEPT FOR BACKWARD COMPATIBILITY ============

    /**
     * Generates a Code 128 barcode and returns it as a Base64 encoded string.
     * Format: data:image/png;base64,{base64_data}
     */
    public static String generateCode128Base64String(String text, int width, int height) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode text cannot be null or empty");
        }
        
        log.info("Generating Code 128 barcode as Base64 string for text: {}", text);
        
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        String base64String = bufferedImageToBase64String(image);
        
        log.info("Successfully generated Code 128 barcode Base64 string");
        return base64String;
    }

    /**
     * Generates a QR Code and returns it as a Base64 encoded string.
     * Format: data:image/png;base64,{base64_data}
     */
    public static String generateQRCodeBase64String(String text, int width, int height) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code text cannot be null or empty");
        }
        
        log.info("Generating QR Code as Base64 string for text: {}", text);
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        String base64String = bufferedImageToBase64String(image);
        
        log.info("Successfully generated QR Code Base64 string");
        return base64String;
    }

    /**
     * Generates a Code 128 barcode and returns it as a BufferedImage.
     * This method is JAR-compatible and works better with JasperReports.
     */
    public static BufferedImage generateCode128BufferedImage(String text, int width, int height) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode text cannot be null or empty");
        }
        
        log.info("Generating Code 128 barcode as BufferedImage for text: {}", text);
        
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height);
        
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        log.info("Successfully generated Code 128 barcode BufferedImage");
        
        return image;
    }

    /**
     * Generates a QR Code and returns it as a BufferedImage.
     * This method is JAR-compatible and works better with JasperReports.
     */
    public static BufferedImage generateQRCodeBufferedImage(String text, int width, int height) throws WriterException, IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QR Code text cannot be null or empty");
        }
        
        log.info("Generating QR Code as BufferedImage for text: {}", text);
        
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        log.info("Successfully generated QR Code BufferedImage");
        
        return image;
    }

    /**
     * Legacy method: Generates a Code 128 barcode and returns it as an InputStream.
     * Keep this for backward compatibility, but prefer BufferedImage methods for JasperReports.
     */
    public static InputStream generateCode128Stream(String text, int width, int height) throws WriterException, IOException {
        BufferedImage image = generateCode128BufferedImage(text, width, height);
        return bufferedImageToInputStream(image);
    }

    /**
     * Legacy method: Generates a QR Code and returns it as an InputStream.
     * Keep this for backward compatibility, but prefer BufferedImage methods for JasperReports.
     */
    public static InputStream generateQRCodeStream(String text, int width, int height) throws WriterException, IOException {
        BufferedImage image = generateQRCodeBufferedImage(text, width, height);
        return bufferedImageToInputStream(image);
    }

    /**
     * Helper method to convert BufferedImage to Base64 encoded string
     * Returns format: data:image/png;base64,{base64_data}
     */
    private static String bufferedImageToBase64String(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        String base64String = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64String;
    }

    /**
     * Helper method to convert BufferedImage to InputStream
     */
    private static InputStream bufferedImageToInputStream(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        return new ByteArrayInputStream(imageBytes);
    }

    /**
     * Helper method to convert Base64 string back to BufferedImage if needed
     * Accepts both formats: "data:image/png;base64,{data}" or just "{data}"
     */
    public static BufferedImage base64StringToBufferedImage(String base64String) throws IOException {
        if (base64String == null || base64String.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 string cannot be null or empty");
        }
        
        // Remove data URL prefix if present
        String base64Data = base64String;
        if (base64String.startsWith("data:image/")) {
            int commaIndex = base64String.indexOf(',');
            if (commaIndex != -1) {
                base64Data = base64String.substring(commaIndex + 1);
            }
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        return ImageIO.read(bais);
    }
}