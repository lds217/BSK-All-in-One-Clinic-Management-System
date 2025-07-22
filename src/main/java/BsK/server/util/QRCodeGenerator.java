package BsK.server.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public class QRCodeGenerator {
    
    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    
    /**
     * Generate QR code for a given URL and save as image file
     */
    public static File generateQRCode(String url, String fileName) throws WriterException, IOException {
        return generateQRCode(url, fileName, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    /**
     * Generate QR code with custom dimensions
     */
    public static File generateQRCode(String url, String fileName, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);
        
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? BLACK : WHITE);
            }
        }
        
        File qrCodeFile = new File(fileName);
        ImageIO.write(bufferedImage, "PNG", qrCodeFile);
        
        log.info("QR code generated: {} for URL: {}", fileName, url);
        return qrCodeFile;
    }
    
    /**
     * Generate QR code as BufferedImage (for embedding in reports)
     */
    public static BufferedImage generateQRCodeImage(String url, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);
        
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? BLACK : WHITE);
            }
        }
        
        return bufferedImage;
    }
    
    /**
     * Generate QR code with custom colors
     */
    public static BufferedImage generateQRCodeImage(String url, int width, int height, Color foregroundColor, Color backgroundColor) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);
        
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int foregroundRGB = foregroundColor.getRGB();
        int backgroundRGB = backgroundColor.getRGB();
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? foregroundRGB : backgroundRGB);
            }
        }
        
        return bufferedImage;
    }
    
    /**
     * Test method to generate a sample QR code
     */
    public static void testQRGeneration() {
        try {
            String testUrl = "https://drive.google.com/drive/folders/test123";
            String fileName = "test-qr-code.png";
            
            File qrFile = generateQRCode(testUrl, fileName);
            log.info("✅ Test QR code generated successfully: {}", qrFile.getAbsolutePath());
            log.info("📱 QR code contains URL: {}", testUrl);
            log.info("💾 QR code saved as: {}", fileName);
            log.info("ℹ️  You can scan this QR code to test the functionality");
            
            // Keep the test file so user can see it
            log.info("🔍 Test QR code file kept for inspection");
            
        } catch (Exception e) {
            log.error("❌ QR code generation test failed", e);
        }
    }
} 