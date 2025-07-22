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
        log.info("📁 Testing folder structure: BSK App → Patient Folders → Checkup Folders → Files");
        
        try {
            // Initialize Google Drive OAuth service
            log.info("🔐 This will open a browser window for Google authentication...");
            GoogleDriveServiceOAuth driveService = new GoogleDriveServiceOAuth();
            
            // Run basic connection test
            driveService.runConnectionTest();
            
            // Test new folder structure and file operations
            testNewFolderStructure(driveService);
            
            // Test standalone QR code generation
            testQRCodeGeneration();
            
            log.info("🎉 All Google Drive OAuth tests completed successfully!");
            log.info("📱 Check the generated QR code files in your project directory!");
            
        } catch (Exception e) {
            log.error("❌ Google Drive OAuth test failed", e);
            System.exit(1);
        }
    }
    
    private static void testNewFolderStructure(GoogleDriveServiceOAuth driveService) throws IOException {
        log.info("🧪 Testing BSK folder structure: App → Patient → Checkup → Files...");
        
        // Test data
        String patientId = "P002";
        String patientName = "Trần Thị B";
        String checkupId1 = "CHK001";
        String checkupId2 = "CHK002";
        
        log.info("👤 Patient: {} (ID: {})", patientName, patientId);
        
        // Test Checkup 1: Medical examination with PDF report
        log.info("🏥 Testing Checkup 1: Medical Examination (ID: {})", checkupId1);
        
        // Create sample files for checkup 1
        File medicalReport = createMedicalReportPDF(patientId, checkupId1);
        File bloodTestImage = createBloodTestImage(patientId, checkupId1);
        
        try {
            // Upload files to checkup 1 folder
            String reportFileId = driveService.uploadFileToCheckup(patientId, patientName, checkupId1, medicalReport, "medical_report_" + checkupId1 + ".pdf");
            String imageFileId = driveService.uploadFileToCheckup(patientId, patientName, checkupId1, bloodTestImage, "blood_test_" + checkupId1 + ".jpg");
            
            // Get checkup folder URL and generate QR code
            String checkup1Url = driveService.getCheckupFolderUrlWithQR(patientId, patientName, checkupId1);
            
            log.info("✅ Checkup 1 completed successfully");
            log.info("📄 Medical report file ID: {}", reportFileId);
            log.info("🩸 Blood test image file ID: {}", imageFileId);
            log.info("🔗 Checkup 1 folder URL: {}", checkup1Url);
            
        } finally {
            // Clean up local test files
            if (medicalReport.exists()) medicalReport.delete();
            if (bloodTestImage.exists()) bloodTestImage.delete();
        }
        
        // Test Checkup 2: Ultrasound examination
        log.info("🏥 Testing Checkup 2: Ultrasound Examination (ID: {})", checkupId2);
        
        // Create sample files for checkup 2
        File ultrasoundReport = createUltrasoundReportPDF(patientId, checkupId2);
        File ultrasoundImage1 = createUltrasoundImage(patientId, checkupId2, "IMG001");
        File ultrasoundImage2 = createUltrasoundImage(patientId, checkupId2, "IMG002");
        
        try {
            // Upload files to checkup 2 folder
            String reportFileId = driveService.uploadFileToCheckup(patientId, patientName, checkupId2, ultrasoundReport, "ultrasound_report_" + checkupId2 + ".pdf");
            String image1FileId = driveService.uploadFileToCheckup(patientId, patientName, checkupId2, ultrasoundImage1, "ultrasound_" + checkupId2 + "_IMG001.jpg");
            String image2FileId = driveService.uploadFileToCheckup(patientId, patientName, checkupId2, ultrasoundImage2, "ultrasound_" + checkupId2 + "_IMG002.jpg");
            
            // Get checkup folder URL and generate QR code
            String checkup2Url = driveService.getCheckupFolderUrlWithQR(patientId, patientName, checkupId2);
            
            log.info("✅ Checkup 2 completed successfully");
            log.info("📄 Ultrasound report file ID: {}", reportFileId);
            log.info("🖼️ Ultrasound image 1 file ID: {}", image1FileId);
            log.info("🖼️ Ultrasound image 2 file ID: {}", image2FileId);
            log.info("🔗 Checkup 2 folder URL: {}", checkup2Url);
            
        } finally {
            // Clean up local test files
            if (ultrasoundReport.exists()) ultrasoundReport.delete();
            if (ultrasoundImage1.exists()) ultrasoundImage1.delete();
            if (ultrasoundImage2.exists()) ultrasoundImage2.delete();
        }
        
        // Get patient main folder URL and generate QR code
        log.info("🧪 Testing patient main folder access...");
        String patientFolderUrl = driveService.getPatientFolderUrlWithQR(patientId, patientName);
        log.info("👤 Patient main folder URL: {}", patientFolderUrl);
        
        // Summary
        log.info("📊 Test Summary:");
        log.info("   📁 BSK App Folder: Created/Verified");
        log.info("   👤 Patient Folder: Patient_{}_{}",patientId, patientName.replace(" ", "_"));
        log.info("   🏥 Checkup Folders: Checkup_{} and Checkup_{}", checkupId1, checkupId2);
        log.info("   📄 Files uploaded: 5 files (2 PDFs + 3 images)");
        log.info("   📱 QR codes generated: 3 codes (patient + 2 checkups)");
    }
    
    private static void testQRCodeGeneration() {
        log.info("🧪 Testing standalone QR code generation...");
        QRCodeGenerator.testQRGeneration();
        log.info("✅ Standalone QR code test completed");
    }
    
    private static File createMedicalReportPDF(String patientId, String checkupId) throws IOException {
        String fileName = "medical_report_" + patientId + "_" + checkupId + ".pdf";
        File file = new File(fileName);
        FileWriter writer = new FileWriter(file);
        
        writer.write("BSK CLINIC - MEDICAL EXAMINATION REPORT\n");
        writer.write("=========================================\n\n");
        writer.write("Patient ID: " + patientId + "\n");
        writer.write("Checkup ID: " + checkupId + "\n");
        writer.write("Examination Date: " + java.time.LocalDate.now() + "\n");
        writer.write("Examination Time: " + java.time.LocalTime.now() + "\n\n");
        writer.write("EXAMINATION RESULTS:\n");
        writer.write("- Blood Pressure: 120/80 mmHg\n");
        writer.write("- Heart Rate: 72 bpm\n");
        writer.write("- Temperature: 36.5°C\n");
        writer.write("- Weight: 65 kg\n");
        writer.write("- Height: 165 cm\n\n");
        writer.write("DIAGNOSIS:\n");
        writer.write("Patient is in good health condition.\n\n");
        writer.write("RECOMMENDATIONS:\n");
        writer.write("- Regular exercise\n");
        writer.write("- Balanced diet\n");
        writer.write("- Follow-up in 6 months\n\n");
        writer.write("Doctor: Dr. Nguyen Van A\n");
        writer.write("Signature: _______________\n");
        writer.close();
        return file;
    }
    
    private static File createUltrasoundReportPDF(String patientId, String checkupId) throws IOException {
        String fileName = "ultrasound_report_" + patientId + "_" + checkupId + ".pdf";
        File file = new File(fileName);
        FileWriter writer = new FileWriter(file);
        
        writer.write("BSK CLINIC - ULTRASOUND EXAMINATION REPORT\n");
        writer.write("==========================================\n\n");
        writer.write("Patient ID: " + patientId + "\n");
        writer.write("Checkup ID: " + checkupId + "\n");
        writer.write("Examination Date: " + java.time.LocalDate.now() + "\n");
        writer.write("Examination Time: " + java.time.LocalTime.now() + "\n\n");
        writer.write("ULTRASOUND FINDINGS:\n");
        writer.write("- Liver: Normal size and echogenicity\n");
        writer.write("- Gallbladder: Normal\n");
        writer.write("- Kidneys: Both kidneys normal\n");
        writer.write("- Spleen: Normal\n");
        writer.write("- Pancreas: Visualized portions normal\n\n");
        writer.write("CONCLUSION:\n");
        writer.write("Normal abdominal ultrasound examination.\n\n");
        writer.write("Images captured: 2 files\n");
        writer.write("Radiologist: Dr. Le Thi C\n");
        writer.write("Signature: _______________\n");
        writer.close();
        return file;
    }
    
    private static File createBloodTestImage(String patientId, String checkupId) throws IOException {
        String fileName = "blood_test_" + patientId + "_" + checkupId + ".txt";
        File file = new File(fileName);
        FileWriter writer = new FileWriter(file);
        
        writer.write("BSK CLINIC - BLOOD TEST IMAGE DATA\n");
        writer.write("==================================\n\n");
        writer.write("Patient ID: " + patientId + "\n");
        writer.write("Checkup ID: " + checkupId + "\n");
        writer.write("Image Type: Blood test microscopy\n");
        writer.write("Capture Date: " + java.time.LocalDateTime.now() + "\n\n");
        writer.write("Note: This is a mock text file representing blood test image data.\n");
        writer.write("In production, this would be actual medical image files (JPG/PNG).\n");
        writer.close();
        return file;
    }
    
    private static File createUltrasoundImage(String patientId, String checkupId, String imageId) throws IOException {
        String fileName = "ultrasound_" + patientId + "_" + checkupId + "_" + imageId + ".txt";
        File file = new File(fileName);
        FileWriter writer = new FileWriter(file);
        
        writer.write("BSK CLINIC - ULTRASOUND IMAGE DATA\n");
        writer.write("==================================\n\n");
        writer.write("Patient ID: " + patientId + "\n");
        writer.write("Checkup ID: " + checkupId + "\n");
        writer.write("Image ID: " + imageId + "\n");
        writer.write("Image Type: Abdominal ultrasound\n");
        writer.write("Capture Date: " + java.time.LocalDateTime.now() + "\n\n");
        writer.write("Note: This is a mock text file representing ultrasound image data.\n");
        writer.write("In production, this would be actual medical image files (JPG/PNG/DICOM).\n");
        writer.close();
        return file;
    }
} 