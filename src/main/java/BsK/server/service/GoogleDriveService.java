package BsK.server.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GoogleDriveService {
    private static final String APPLICATION_NAME = "BSK Clinic Management System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    
    // Path to service account key file (will be configured)
    private static final String SERVICE_ACCOUNT_KEY_PATH = "src/main/resources/google-drive-service-account.json";
    
    private Drive driveService;
    private String rootFolderId; // BSK Clinic root folder
    
    public GoogleDriveService() {
        try {
            this.driveService = buildDriveService();
            this.rootFolderId = getOrCreateRootFolder();
            log.info("Google Drive service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Google Drive service", e);
            throw new RuntimeException("Google Drive service initialization failed", e);
        }
    }
    
    private Drive buildDriveService() throws GeneralSecurityException, IOException {
        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(new FileInputStream(SERVICE_ACCOUNT_KEY_PATH))
                .createScoped(SCOPES);
        
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Get or create the root folder for BSK Clinic
     */
    private String getOrCreateRootFolder() throws IOException {
        String folderName = "BSK_Clinic_Patient_Files";
        
        // Search for existing root folder
        String query = "name='" + folderName + "' and mimeType='application/vnd.google-apps.folder' and trashed=false";
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .execute();
        
        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            String folderId = files.get(0).getId();
            log.info("Found existing root folder: {} (ID: {})", folderName, folderId);
            return folderId;
        }
        
        // Create root folder if it doesn't exist
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        
        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        
        log.info("Created new root folder: {} (ID: {})", folderName, folder.getId());
        return folder.getId();
    }
    
    /**
     * Create a folder for a specific patient
     */
    public String createPatientFolder(String patientId, String patientName) throws IOException {
        String folderName = "Patient_" + patientId + "_" + sanitizeFileName(patientName);
        
        // Check if folder already exists
        Optional<String> existingFolderId = findPatientFolder(patientId);
        if (existingFolderId.isPresent()) {
            log.info("Patient folder already exists: {} (ID: {})", folderName, existingFolderId.get());
            return existingFolderId.get();
        }
        
        // Create new patient folder
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(rootFolderId));
        
        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        
        log.info("Created patient folder: {} (ID: {})", folderName, folder.getId());
        return folder.getId();
    }
    
    /**
     * Find existing patient folder by patient ID
     */
    private Optional<String> findPatientFolder(String patientId) throws IOException {
        String query = "name contains 'Patient_" + patientId + "_' and mimeType='application/vnd.google-apps.folder' and trashed=false and '" + rootFolderId + "' in parents";
        
        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .execute();
        
        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return Optional.of(files.get(0).getId());
        }
        
        return Optional.empty();
    }
    
    /**
     * Upload a file to a patient's folder
     */
    public String uploadFile(String patientFolderId, java.io.File localFile, String fileName) throws IOException {
        String mimeType = determineMimeType(fileName);
        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(patientFolderId));
        
        FileContent mediaContent = new FileContent(mimeType, localFile);
        
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id,webViewLink,webContentLink")
                .execute();
        
        log.info("Uploaded file: {} (ID: {})", fileName, uploadedFile.getId());
        return uploadedFile.getId();
    }
    
    /**
     * Get the public sharing URL for a folder
     */
    public String getFolderSharingUrl(String folderId) throws IOException {
        // Make the folder publicly viewable
        com.google.api.services.drive.model.Permission permission = new com.google.api.services.drive.model.Permission();
        permission.setType("anyone");
        permission.setRole("reader");
        
        driveService.permissions().create(folderId, permission).execute();
        
        // Get the folder details with web view link
        File folder = driveService.files().get(folderId)
                .setFields("webViewLink")
                .execute();
        
        return folder.getWebViewLink();
    }
    
    /**
     * Create a test folder and upload sample files
     */
    public void runConnectionTest() throws IOException {
        log.info("Starting Google Drive connection test...");
        
        // Create test folder
        String testFolderId = createPatientFolder("TEST001", "Test Patient");
        
        // Create a test text file
        java.io.File testFile = new java.io.File("test-connection.txt");
        try {
            java.io.FileWriter writer = new java.io.FileWriter(testFile);
            writer.write("BSK Clinic - Google Drive Connection Test\n");
            writer.write("Timestamp: " + java.time.LocalDateTime.now() + "\n");
            writer.write("This file verifies that the Google Drive integration is working correctly.\n");
            writer.close();
            
            // Upload the test file
            String fileId = uploadFile(testFolderId, testFile, "connection-test.txt");
            
            // Get sharing URL
            String folderUrl = getFolderSharingUrl(testFolderId);
            
            log.info("✅ Google Drive connection test successful!");
            log.info("📁 Test folder URL: {}", folderUrl);
            log.info("📄 Test file ID: {}", fileId);
            
            // Clean up test file
            testFile.delete();
            
        } catch (Exception e) {
            log.error("❌ Google Drive connection test failed", e);
            if (testFile.exists()) {
                testFile.delete();
            }
            throw e;
        }
    }
    
    private String sanitizeFileName(String fileName) {
        // Remove or replace invalid characters for file names
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    private String determineMimeType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFileName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream"; // Default binary
    }
    
    public String getRootFolderId() {
        return rootFolderId;
    }
} 