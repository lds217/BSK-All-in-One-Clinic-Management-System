package BsK.server.test;

import BsK.server.service.GoogleDriveServiceOAuth;
import BsK.server.util.QRCodeGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class GoogleDriveOAuthTest {
    
    public static void main(String[] args) {
        log.info("🚀 Starting BSK Google Drive OAuth Integration Test");
        
        try {
            // Initialize Google Drive OAuth service
            log.info("🔐 This will open a browser window for Google authentication...");
            GoogleDriveServiceOAuth driveService = new GoogleDriveServiceOAuth();
            
            // Run basic connection test
            driveService.runConnectionTest();
            
            // Test patient folder creation and file upload
            testPatientFileOperations(driveService);
            
            // Test QR code generation
            testQRCodeGeneration();
            
            log.info("🎉 All Google Drive OAuth tests completed successfully!");
            
        } catch (Exception e) {
            log.error("❌ Google Drive OAuth test failed", e);
            System.exit(1);
        }
    }
    
    private static void testPatientFileOperations(GoogleDriveServiceOAuth driveService) throws IOException {
        log.info("🧪 Testing patient file operations...");
        
        // Test patient folder creation
        String patientId = "P002";
        String patientName = "Trần Thị B";
        String patientFolderId = driveService.createPatientFolder(patientId, patientName);
        
        // Create sample PDF file (mock)
        File testPdf = createSamplePDF("medical_report_" + patientId + ".pdf");
        
        // Create sample image file (mock)
        File testImage = createSampleTextFile("xray_" + patientId + ".txt", "Sample X-ray data for patient " + patientId);
        
        try {
            // Upload files
            String pdfFileId = driveService.uploadFile(patientFolderId, testPdf, testPdf.getName());
            String imageFileId = driveService.uploadFile(patientFolderId, testImage, "xray_" + patientId + ".jpg");
            
            // Get folder sharing URL
            String folderUrl = driveService.getFolderSharingUrl(patientFolderId);
            
            log.info("✅ Patient folder created successfully");
            log.info("📁 Patient folder URL: {}", folderUrl);
            log.info("📄 PDF file ID: {}", pdfFileId);
            log.info("🖼️ Image file ID: {}", imageFileId);
            
            // Test QR code generation for this folder
            testQRCodeForFolder(folderUrl, patientId);
            
        } finally {
            // Clean up test files
            if (testPdf.exists()) testPdf.delete();
            if (testImage.exists()) testImage.delete();
        }
    }
    
    private static void testQRCodeGeneration() {
        log.info("🧪 Testing QR code generation...");
        QRCodeGenerator.testQRGeneration();
    }
    
    private static void testQRCodeForFolder(String folderUrl, String patientId) {
        try {
            log.info("🧪 Testing QR code generation for patient folder...");
            String qrFileName = "patient_" + patientId + "_qr.png";
            File qrFile = QRCodeGenerator.generateQRCode(folderUrl, qrFileName);
            
            log.info("✅ QR code generated for patient folder: {}", qrFile.getAbsolutePath());
            log.info("📱 QR code contains URL: {}", folderUrl);
            
            // Clean up
            if (qrFile.exists()) {
                qrFile.delete();
                log.info("🧹 QR code file cleaned up");
            }
            
        } catch (Exception e) {
            log.error("❌ QR code generation failed for patient folder", e);
        }
    }
    
    private static File createSamplePDF(String fileName) throws IOException {
        File file = new File(fileName);
        FileWriter writer = new FileWriter(file);
        writer.write("BSK Clinic - Sample Medical Report (OAuth Test)\n");
        writer.write("==========================================\n\n");
        writer.write("Patient ID: P002\n");
        writer.write("Patient Name: Trần Thị B\n");
        writer.write("Date: " + java.time.LocalDate.now() + "\n\n");
        writer.write("This is a sample medical report for testing Google Drive OAuth integration.\n");
        writer.write("In production, this would be a properly formatted PDF report.\n");
        writer.write("Authentication method: OAuth2 with user consent\n");
        writer.close();
        return file;
    }
    
    private static File createSampleTextFile(String fileName, String content) throws IOException {
        File file = new File(fileName);
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.write("\nCreated: " + java.time.LocalDateTime.now());
        writer.write("\nAuthentication method: OAuth2 with user consent");
        writer.close();
        return file;
    }
} 