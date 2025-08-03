package BsK.client.ui.component.CheckUpPage.PrintDialog;

import BsK.client.LocalStorage;


import lombok.extern.slf4j.Slf4j;


// --- JasperReports Imports ---
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.JasperPrintManager;
// --- End JasperReports Imports ---

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRException;
import java.util.ArrayList;


@Slf4j
public class UltrasoundResult {
    
    /**
     * Load and compile Jasper report from JRXML with robust error handling for JAR environment
     */
    private static JasperReport loadCompiledReport(String reportName) {
        String jrxmlPath = "/print_forms/" + reportName + ".jrxml";
        
        try (InputStream jrxmlStream = loadResourceAsStream(jrxmlPath)) {
            if (jrxmlStream == null) {
                throw new RuntimeException("JRXML resource not found: " + jrxmlPath);
            }
            
            log.info("Compiling JRXML at runtime: {}", jrxmlPath);
            
            // Read all bytes to ensure complete stream reading in JAR environment
            byte[] jrxmlContent = jrxmlStream.readAllBytes();
            log.info("JRXML content size: {} bytes", jrxmlContent.length);
            
            if (jrxmlContent.length == 0) {
                throw new RuntimeException("JRXML file is empty: " + jrxmlPath);
            }
            
            // Use ByteArrayInputStream for reliable reading in JAR environment
            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(jrxmlContent)) {
                log.info("Loading JRXML design...");
                JasperDesign jasperDesign = JRXmlLoader.load(byteStream);
                log.info("Compiling Jasper report...");
                JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
                log.info("Successfully compiled report: {}", reportName);
                return jasperReport;
            }
        } catch (Exception e) {
            log.error("Failed to compile JRXML report {}: {}", jrxmlPath, e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not load or compile report: " + reportName, e);
        }
    }
    
    /**
     * Robust resource loading method that works in both development and JAR environments
     */
    private static InputStream loadResourceAsStream(String resourcePath) {
        // Try multiple approaches to load the resource
        InputStream inputStream = null;
        
        // Method 1: Using current class classloader
        try {
            inputStream = UltrasoundResult.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                log.info("Successfully loaded resource using class.getResourceAsStream: {}", resourcePath);
                return inputStream;
            }
        } catch (Exception e) {
            log.warn("Failed to load resource using class.getResourceAsStream: {}", e.getMessage());
        }
        
        // Method 2: Using thread context classloader (without leading slash)
        try {
            String pathWithoutSlash = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            inputStream = contextClassLoader.getResourceAsStream(pathWithoutSlash);
            if (inputStream != null) {
                log.info("Successfully loaded resource using context classloader: {}", pathWithoutSlash);
                return inputStream;
            }
        } catch (Exception e) {
            log.warn("Failed to load resource using context classloader: {}", e.getMessage());
        }
        
        // Method 3: Using system classloader
        try {
            String pathWithoutSlash = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            inputStream = ClassLoader.getSystemResourceAsStream(pathWithoutSlash);
            if (inputStream != null) {
                log.info("Successfully loaded resource using system classloader: {}", pathWithoutSlash);
                return inputStream;
            }
        } catch (Exception e) {
            log.warn("Failed to load resource using system classloader: {}", e.getMessage());
        }
        
        // Method 5: Try loading from external file system (fallback for JAR issues)
        try {
            String externalPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            java.io.File externalFile = new java.io.File(externalPath);
            if (externalFile.exists()) {
                inputStream = new java.io.FileInputStream(externalFile);
                log.info("Successfully loaded resource from external file: {}", externalFile.getAbsolutePath());
                return inputStream;
            }
        } catch (Exception e) {
            log.warn("Failed to load resource from external file: {}", e.getMessage());
        }
        
        // Method 6: Try loading from src/main/resources (development mode)
        try {
            String srcPath = "src/main/resources" + (resourcePath.startsWith("/") ? "" : "/") + resourcePath;
            java.io.File srcFile = new java.io.File(srcPath);
            if (srcFile.exists()) {
                inputStream = new java.io.FileInputStream(srcFile);
                log.info("Successfully loaded resource from source directory: {}", srcFile.getAbsolutePath());
                return inputStream;
            }
        } catch (Exception e) {
            log.warn("Failed to load resource from source directory: {}", e.getMessage());
        }
        
        log.error("All resource loading methods failed for: {}", resourcePath);
        return null;
    }

    private JDialog dialog;
    private final String checkupId;
    private final String patientName;
    private final String patientDOB;
    private final String patientGender;
    private final String patientAddress;
    private final String doctorName;
    private final String ultrasoundDoctorName;
    private final String checkupDate;
    private final String rtfContent;
    private final String conclusion;
    private final String suggestion;
    private final String recheckupDate;
    private final List<File> selectedImages;
    private final String printType; // "ngang" or "dọc"
    private final String templateTitle;
    private final String driveUrl; // Google Drive URL for QR code
    
    // Vitals data
    private final String customerHeight;
    private final String customerWeight;
    private final String heartRate;
    private final String bloodPressure;

    private static final String PDF_PATH = "ultrasound_result.pdf";

    public UltrasoundResult(String checkupId, String patientName, String patientDOB, String patientGender,
                            String patientAddress, String doctorName, String ultrasoundDoctorName, String checkupDate,
                            String rtfContent, String conclusion, String suggestion,
                            String recheckupDate, List<File> selectedImages, 
                            String printType, String templateTitle, String customerHeight, 
                            String customerWeight, String heartRate, String bloodPressure, String driveUrl) {

        // Defensive null checks to prevent JasperReports from crashing on null parameters
        // Also handle "null" string values by converting them to empty strings
        this.checkupId = convertNullToEmpty(checkupId);
        this.patientName = convertNullToEmpty(patientName);
        this.patientDOB = convertNullToEmpty(patientDOB);
        this.patientGender = convertNullToEmpty(patientGender);
        this.patientAddress = convertNullToEmpty(patientAddress);
        this.doctorName = convertNullToEmpty(doctorName);
        this.ultrasoundDoctorName = convertNullToEmpty(ultrasoundDoctorName);
        this.checkupDate = convertNullToEmpty(checkupDate);
        this.rtfContent = convertNullToEmpty(rtfContent);
        this.conclusion = convertNullToEmpty(conclusion);
        this.suggestion = convertNullToEmpty(suggestion);
        this.recheckupDate = convertNullToEmpty(recheckupDate);
        this.selectedImages = selectedImages != null ? selectedImages : new ArrayList<>();
        this.printType = convertNullToEmpty(printType);
        this.templateTitle = convertNullToEmpty(templateTitle);
        this.customerHeight = convertNullToEmpty(customerHeight);
        this.customerWeight = convertNullToEmpty(customerWeight);
        this.heartRate = convertNullToEmpty(heartRate);
        this.bloodPressure = convertNullToEmpty(bloodPressure);
        this.driveUrl = convertNullToEmpty(driveUrl); // Google Drive URL
    }
    
    /**
     * Helper method to convert null or "null" string values to empty strings
     * @param value The string to check
     * @return Empty string if value is null or equals "null", otherwise the original value
     */
    private String convertNullToEmpty(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        return value;
    }
    
    /**
     * Helper method to convert null or "null" string values to a default value
     * @param value The string to check
     * @param defaultValue The default value to use if value is null or "null"
     * @return defaultValue if value is null or equals "null", otherwise the original value
     */
    private String convertNullToEmpty(String value, String defaultValue) {
        if (value == null || "null".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    public void showDirectJasperViewer() {
        try {
            JasperPrint jasperPrint = createJasperPrint();
            if (jasperPrint != null) {
                JasperViewer.viewReport(jasperPrint, false);
            }
        } catch (Exception e) {
            log.error("Error displaying JasperViewer for Ultrasound Result", e);
            JOptionPane.showMessageDialog(null, "Không thể hiển thị bản xem trước: " + e.getMessage(), "Lỗi Jasper", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void printDirectly() {
        try {
            JasperPrint jasperPrint = createJasperPrint();
            if (jasperPrint != null) {
                // The 'false' argument means no print dialog will be shown.
                JasperPrintManager.printReport(jasperPrint, false);
            }
        } catch (Exception e) {
            log.error("Error printing report directly for Ultrasound Result", e);
            JOptionPane.showMessageDialog(null, "Không thể in trực tiếp: " + e.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JasperPrint createJasperPrint() throws JRException, IOException {
        String reportName;
        if ("Dọc".equalsIgnoreCase(this.printType)) {
            reportName = "ultrasoundresult_potrait";
        } else {
            reportName = "ultrasoundresult";
        }
        
        log.info("Using Jasper report template: {}", reportName);

        try {
            Map<String, Object> parameters = new HashMap<>();

            // Set locale for date/number formatting
            parameters.put(JRParameter.REPORT_LOCALE, Locale.of("vi", "VN"));
            
            // Populate all the string parameters
            parameters.put("clinicPrefix", LocalStorage.ClinicPrefix != null ? LocalStorage.ClinicPrefix : "");
            parameters.put("clinicName", convertNullToEmpty(LocalStorage.ClinicName, "Phòng khám BSK"));
            parameters.put("clinicAddress", convertNullToEmpty(LocalStorage.ClinicAddress, "Địa chỉ phòng khám"));
            parameters.put("clinicPhone", convertNullToEmpty(LocalStorage.ClinicPhone, "SĐT phòng khám"));
            parameters.put("patientName", this.patientName);
            parameters.put("patientDOB", this.patientDOB);
            parameters.put("patientGender", this.patientGender);
            parameters.put("patientAddress", this.patientAddress);
            parameters.put("doctorName", this.doctorName);
            parameters.put("ultrasoundDoctorName", this.ultrasoundDoctorName);
            parameters.put("checkupDate", this.checkupDate);
            parameters.put("barcodeNumber", this.checkupId);

            // RTF and plain text content
            parameters.put("checkupNote", this.rtfContent != null ? this.rtfContent : "");
            parameters.put("checkupConclusion", this.conclusion != null ? this.conclusion : "");
            parameters.put("checkupSuggestion", this.suggestion != null ? this.suggestion : "");
            parameters.put("reCheckupDate", this.recheckupDate != null ? this.recheckupDate : "");
            
            // Vitals data
            parameters.put("checkupHeight", this.customerHeight != null ? this.customerHeight : "");
            parameters.put("checkupWeight", this.customerWeight != null ? this.customerWeight : "");
            parameters.put("checkupHeartBeat", this.heartRate);
            parameters.put("checkupBloodPressure", this.bloodPressure);

            // Template title
            parameters.put("templateTitle", this.templateTitle);
            
            // Extra safety: Ensure no parameter is null or "null" string
            for (String key : parameters.keySet()) {
                Object value = parameters.get(key);
                if (value instanceof String && ("null".equals(value) || value == null)) {
                    parameters.put(key, "");
                }
            }

            // Handle images
            int numberOfImages = this.selectedImages.size();
            parameters.put("numberImage", numberOfImages);
            
            parameters.put("logoImage", System.getProperty("user.dir") + "/src/main/java/BsK/client/ui/assets/icon/logo.jpg");

            for (int i = 0; i < 6; i++) {
                if (i < numberOfImages) {
                    parameters.put("image" + (i + 1), this.selectedImages.get(i).getAbsolutePath());
                } else {
                    parameters.put("image" + (i + 1), null); // Pass null for unused image slots
                }
            }

            // Google Drive URL for QR code generation
            parameters.put("driveURL", this.driveUrl);

            // Load pre-compiled report or compile JRXML as fallback
            JasperReport jasperReport = loadCompiledReport(reportName);

            // Fill the report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            return jasperPrint;
        } catch (Exception e) {
            log.error("Error creating JasperPrint for report {}: {}", reportName, e.getMessage());
            throw new JRException("Failed to create ultrasound report", e);
        }
    }

    /**
     * Exports the ultrasound result as PDF bytes for backend upload
     * @return byte array of the PDF file
     * @throws JRException if there's an error during PDF export
     * @throws IOException if there's an error during file operations
     */
    public byte[] exportToPdfBytes() throws JRException, IOException {
        // First create the JasperPrint object
        JasperPrint jasperPrint = createJasperPrint();
        
        // Export to PDF bytes
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

} 