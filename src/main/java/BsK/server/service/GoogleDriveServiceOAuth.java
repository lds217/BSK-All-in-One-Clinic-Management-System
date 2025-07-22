package BsK.server.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GoogleDriveServiceOAuth {
    private static final String APPLICATION_NAME = "BSK Clinic Management System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    
    // Path to OAuth2 client credentials file 
    private static final String OAUTH_CREDENTIALS_PATH = "src/main/resources/google-oauth-credentials.json";
    // Directory to store authorization tokens
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    
    private Drive driveService;
    private String rootFolderId; // BSK Clinic root folder
    private String rootFolderName; // Configurable root folder name
    
    public GoogleDriveServiceOAuth() {
        this("BSK_Clinic_Patient_Files"); // Default folder name
    }
    
    public GoogleDriveServiceOAuth(String rootFolderName) {
        try {
            this.rootFolderName = rootFolderName != null ? rootFolderName : "BSK_Clinic_Patient_Files";
            this.driveService = buildDriveService();
            this.rootFolderId = getOrCreateRootFolder();
            log.info("Google Drive OAuth service initialized successfully with root folder: {}", this.rootFolderName);
        } catch (Exception e) {
            log.error("Failed to initialize Google Drive OAuth service", e);
            throw new RuntimeException("Google Drive OAuth service initialization failed", e);
        }
    }
    
    private Drive buildDriveService() throws GeneralSecurityException, IOException {
        // Load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY, 
                new InputStreamReader(new FileInputStream(OAUTH_CREDENTIALS_PATH))
        );
        
        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), 
                JSON_FACTORY, 
                clientSecrets, 
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        
        // Build a new authorized API client service
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Get or create the root folder for BSK Clinic
     */
    private String getOrCreateRootFolder() throws IOException {
        String folderName = this.rootFolderName;
        
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
     * Create a folder for a specific patient's checkup
     */
    public String createCheckupFolder(String patientId, String patientName, String checkupId) throws IOException {
        // First ensure patient folder exists
        String patientFolderId = createPatientFolder(patientId, patientName);
        
        String checkupFolderName = "Checkup_" + checkupId;
        
        // Check if checkup folder already exists
        Optional<String> existingCheckupFolderId = findCheckupFolder(patientFolderId, checkupId);
        if (existingCheckupFolderId.isPresent()) {
            log.info("Checkup folder already exists: {} (ID: {})", checkupFolderName, existingCheckupFolderId.get());
            return existingCheckupFolderId.get();
        }
        
        // Create new checkup folder
        File folderMetadata = new File();
        folderMetadata.setName(checkupFolderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(patientFolderId));
        
        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        
        log.info("Created checkup folder: {} (ID: {})", checkupFolderName, folder.getId());
        return folder.getId();
    }
    
    /**
     * Find existing checkup folder by checkup ID within a patient folder
     */
    private Optional<String> findCheckupFolder(String patientFolderId, String checkupId) throws IOException {
        String query = "name='Checkup_" + checkupId + "' and mimeType='application/vnd.google-apps.folder' and trashed=false and '" + patientFolderId + "' in parents";
        
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
     * Upload a file to a specific checkup folder
     */
    public String uploadFileToCheckup(String patientId, String patientName, String checkupId, java.io.File localFile, String fileName) throws IOException {
        String checkupFolderId = createCheckupFolder(patientId, patientName, checkupId);
        return uploadFile(checkupFolderId, localFile, fileName);
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
     * Get the folder URL (without making it public)
     */
    public String getFolderUrl(String folderId) throws IOException {
        File folder = driveService.files().get(folderId)
                .setFields("webViewLink")
                .execute();
        
        return folder.getWebViewLink();
    }

    /**
     * Get patient folder URL with QR code generation
     */
    public String getPatientFolderUrlWithQR(String patientId, String patientName) throws IOException {
        String patientFolderId = createPatientFolder(patientId, patientName);
        String folderUrl = getFolderUrl(patientFolderId);
        
        // Generate QR code for the folder URL
        try {
            String qrFileName = "patient_" + patientId + "_folder_qr.png";
            java.io.File qrFile = BsK.server.util.QRCodeGenerator.generateQRCode(folderUrl, qrFileName);
            log.info("📱 QR code generated for patient {} folder: {}", patientId, qrFile.getAbsolutePath());
            log.info("🔗 QR code contains URL: {}", folderUrl);
            return folderUrl;
        } catch (Exception e) {
            log.error("❌ Failed to generate QR code for patient folder", e);
            return folderUrl;
        }
    }
    
    /**
     * Get checkup folder URL with QR code generation
     */
    public String getCheckupFolderUrlWithQR(String patientId, String patientName, String checkupId) throws IOException {
        String checkupFolderId = createCheckupFolder(patientId, patientName, checkupId);
        String folderUrl = getFolderUrl(checkupFolderId);
        
        // Generate QR code for the folder URL
        try {
            String qrFileName = "patient_" + patientId + "_checkup_" + checkupId + "_qr.png";
            java.io.File qrFile = BsK.server.util.QRCodeGenerator.generateQRCode(folderUrl, qrFileName);
            log.info("📱 QR code generated for patient {} checkup {} folder: {}", patientId, checkupId, qrFile.getAbsolutePath());
            log.info("🔗 QR code contains URL: {}", folderUrl);
            return folderUrl;
        } catch (Exception e) {
            log.error("❌ Failed to generate QR code for checkup folder", e);
            return folderUrl;
        }
    }
    
    /**
     * Create a test folder and upload sample files
     */
    public void runConnectionTest() throws IOException {
        log.info("Starting Google Drive OAuth connection test...");
        
        // Create test folder
        String testFolderId = createPatientFolder("TEST002", "OAuth Test Patient");
        
        // Create a test text file
        java.io.File testFile = new java.io.File("test-oauth-connection.txt");
        try {
            java.io.FileWriter writer = new java.io.FileWriter(testFile);
            writer.write("BSK Clinic - Google Drive OAuth Connection Test\n");
            writer.write("Timestamp: " + java.time.LocalDateTime.now() + "\n");
            writer.write("This file verifies that the Google Drive OAuth integration is working correctly.\n");
            writer.write("Authentication method: OAuth2 with user consent\n");
            writer.close();
            
            // Upload the test file
            String fileId = uploadFile(testFolderId, testFile, "oauth-connection-test.txt");
            
            // Get sharing URL
            String folderUrl = getFolderSharingUrl(testFolderId);
            
            log.info("✅ Google Drive OAuth connection test successful!");
            log.info("📁 Test folder URL: {}", folderUrl);
            log.info("📄 Test file ID: {}", fileId);
            
            // Clean up test file
            testFile.delete();
            
        } catch (Exception e) {
            log.error("❌ Google Drive OAuth connection test failed", e);
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
    
    public String getRootFolderName() {
        return rootFolderName;
    }
    
    /**
     * Update the root folder name (creates new folder, doesn't affect existing folders)
     */
    public void updateRootFolderName(String newRootFolderName) throws IOException {
        if (newRootFolderName != null && !newRootFolderName.trim().isEmpty()) {
            this.rootFolderName = newRootFolderName.trim();
            this.rootFolderId = getOrCreateRootFolder();
            log.info("Root folder updated to: {} (ID: {})", this.rootFolderName, this.rootFolderId);
        }
    }

    /**
     * Creates a folder directly under a specific parent folder using the folder ID
     */
    public String createFolderUnderParent(String parentFolderId, String folderName) throws IOException {
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        folderMetadata.setParents(Collections.singletonList(parentFolderId));
        
        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();
        
        log.info("Created folder: {} under parent {} (ID: {})", folderName, parentFolderId, folder.getId());
        return folder.getId();
    }

    /**
     * Upload a file to a specific folder using the folder ID
     */
    public String uploadFileToFolder(String folderId, java.io.File localFile, String fileName) throws IOException {
        String mimeType = determineMimeType(fileName);

        // --- OVERRIDE LOGIC START ---
        // 1. Find any existing files with the same name in the target folder
        findFilesByName(folderId, fileName).forEach(file -> {
            try {
                log.info("Deleting existing file '{}' (ID: {}) to override.", file.getName(), file.getId());
                driveService.files().delete(file.getId()).execute();
            } catch (IOException e) {
                log.error("Failed to delete existing file (ID: {})", file.getId(), e);
                // We wrap in a RuntimeException because the lambda can't throw a checked IOException
                throw new RuntimeException("Failed to delete existing file for override", e);
            }
        });
        // --- OVERRIDE LOGIC END ---
        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));
        
        FileContent mediaContent = new FileContent(mimeType, localFile);
        
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id,webViewLink,webContentLink")
                .execute();
        
        log.info("Uploaded file: {} to folder {} (File ID: {})", fileName, folderId, uploadedFile.getId());
        return uploadedFile.getId();
    }

    /**
     * Helper method to find all files with a specific name inside a parent folder.
     * @param parentFolderId The ID of the folder to search within.
     * @param fileName The exact name of the files to find.
     * @return A list of File objects matching the name.
     * @throws IOException if the Google Drive API call fails.
     */
    private List<File> findFilesByName(String parentFolderId, String fileName) throws IOException {
        String query = String.format("name = '%s' and '%s' in parents and trashed = false",
                fileName.replace("'", "\\'"), // Escape single quotes in filename
                parentFolderId);

        FileList result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name)")
                .execute();
        
        return result.getFiles();
    }
} 