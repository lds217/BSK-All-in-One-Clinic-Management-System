package BsK.client.ui.component.CheckUpPage.PrintDialog;

import BsK.client.LocalStorage;
import BsK.client.ui.component.CheckUpPage.PrintDialog.InvoiceItem;
// JasperReports imports
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.MediaSizeName;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

@Slf4j

public class MedicineInvoice{
    
    /**
     * Load and compile Jasper report from JRXML following standard pattern: inputstream → load → compile
     */
    private static JasperReport loadCompiledReport(String reportName) {
        String jrxmlPath = "/print_forms/" + reportName + ".jrxml";
        
        try (InputStream inputStream = loadResourceAsStream(jrxmlPath)) {
            if (inputStream == null) {
                throw new RuntimeException("JRXML resource not found: " + jrxmlPath);
            }
            
            log.info("Loading and compiling JRXML: {}", jrxmlPath);
            
            // Standard JasperReports pattern: inputstream → load → compile
            JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            
            log.info("Successfully compiled report: {}", reportName);
            return jasperReport;
            
        } catch (Exception e) {
            log.error("Failed to load/compile JRXML report {}: {}", jrxmlPath, e.getMessage());
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
            inputStream = MedicineInvoice.class.getResourceAsStream(resourcePath);
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
        
        // Method 4: Try with different class as anchor
        try {
            inputStream = LocalStorage.class.getResourceAsStream(resourcePath);
            if (inputStream != null) {
                log.info("Successfully loaded resource using LocalStorage.class: {}", resourcePath);
                return inputStream;
            }
        } catch (Exception e) {
            log.warn("Failed to load resource using LocalStorage.class: {}", e.getMessage());
        }
        
        // Method 5: Try loading from external file system (fallback for JAR issues)
        try {
            String externalPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            java.io.File externalFile = new java.io.File(externalPath);
            if (externalFile.exists()) {
                inputStream = new FileInputStream(externalFile);
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
                inputStream = new FileInputStream(srcFile);
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
    private String patientName;
    private String patientDOB;
    private String patientPhone;
    private String patientGender;
    private String patientAddress;
    private String doctorName;
    private String diagnosis;
    private String notes;
    private String date;
    private String id;
    private String driveURL;
    private String[][] med; // Prescription data for medicines
    private String[][] services; // Prescription data for services
    private String[][] supplements; // Prescription data for supplements

    public CompletableFuture<byte[]> generatePdfBytesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generatePdfBytes();
            } catch (Exception e) {
                // Since this runs in a background thread, we can't show a dialog directly.
                // We'll wrap the exception and let the caller handle it.
                e.printStackTrace();
                throw new RuntimeException("Error generating PDF bytes for invoice: " + e.getMessage(), e);
            }
        });
    }

    public MedicineInvoice(String id, String patientName, String patientDOB, String patientPhone,
                           String patientGender, String patientAddress, String doctorName, String diagnosis,
                           String notes, String driveURL, String[][] med, String[][] services, String[][] supplements) {
        this.id = id;
        this.patientName = patientName;
        this.patientDOB = patientDOB;
        this.patientPhone = patientPhone;
        this.patientGender = patientGender;
        this.patientAddress = patientAddress;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.driveURL = driveURL;
        // Get today's date
        LocalDate today = LocalDate.now();

        // Format the date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = today.format(formatter);

        this.date = formattedDate;
        this.med = med;
        this.services = services;
        this.supplements = supplements;
    }


    private JasperPrint jasperPrint; // Store JasperPrint for reuse

    /**
     * Shows the invoice directly in JasperViewer without creating a custom dialog
     */
    public void showDirectJasperViewer() throws Exception {
        try {
            // This method now only fills the report and shows the viewer
            fillJasperPrint();
            
            if (jasperPrint == null) {
                throw new Exception("JasperPrint is null - report compilation failed");
            }
            
            // Use the constructor to prevent the application from closing
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setVisible(true);
        } catch (Exception e) {
            log.info("error showing the jasper viewer, {}", e.getMessage());
            throw e;
        }
    }

    public void createDialog(JFrame parent) {
        dialog = new JDialog(parent, "Medicine Invoice", true);
        dialog.setSize(600, 600);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        
        // Ensure modal behavior and proper parent relationship
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setAlwaysOnTop(false); // Don't force always on top, let modal behavior handle this
        
        // Add window listener to handle minimize/restore events with parent
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                if (parent != null) {
                    parent.setState(Frame.ICONIFIED);
                }
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
                if (parent != null && parent.getState() == Frame.ICONIFIED) {
                    parent.setState(Frame.NORMAL);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        JButton saveButton = new JButton("Save PDF");
        JButton printButton = new JButton("Print PDF");

        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);

        JPanel pdfViewer = new JPanel();
        JScrollPane scrollPane = new JScrollPane(pdfViewer);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        String pdfPath = "temp_medical_invoice.pdf"; // Use a temporary file name
        try {
            generatePdf(pdfPath);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialog, "Error generating PDF: " + e.getMessage());
        }

        // Save PDF Action
        saveButton.addActionListener(e -> {
            try {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save PDF As");
                // Suggest a file name based on patient name and date
                fileChooser.setSelectedFile(new File(patientName.replace(" ", "_") + "_invoice_" + date.replace("/", "-") + ".pdf"));
                int userSelection = fileChooser.showSaveDialog(dialog);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    // Ensure the file has a .pdf extension
                    if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                        fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
                    }
                    
                    // Export the already generated JasperPrint object to the chosen file
                    JasperExportManager.exportReportToPdfFile(jasperPrint, fileToSave.getAbsolutePath());
                    JOptionPane.showMessageDialog(dialog, "PDF saved: " + fileToSave.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error saving PDF: " + ex.getMessage());
            }
        });

        // Print PDF Action - Show JasperViewer
        printButton.addActionListener(e -> {
            try {
                if (jasperPrint != null) {
                    // Close our custom dialog first
                    dialog.dispose();
                    
                    // Use the constructor to prevent the application from closing
                    JasperViewer viewer = new JasperViewer(jasperPrint, false);
                    viewer.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(dialog, "Report not generated yet. Please try again.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error opening print viewer: " + ex.getMessage());
            }
        });

        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private byte[] generatePdfBytes() throws Exception {
        try {
            // This method is almost identical to generatePdf, but exports to a byte array
            fillJasperPrint(); // Ensures jasperPrint is filled
            
            if (jasperPrint == null) {
                throw new Exception("JasperPrint is null - report compilation failed");
            }
            
            log.info("Exporting JasperPrint to PDF bytes...");
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error generating PDF byte array with JasperReports: " + e.getMessage(), e);
        }
    }

    private void generatePdf(String pdfPath) throws Exception {
        try {
            // Refactored to use a common method to fill the report
            fillJasperPrint();
            
            if (jasperPrint == null) {
                throw new Exception("JasperPrint is null - report compilation failed");
            }
            
            // Export to PDF file
            JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Error generating PDF with JasperReports: " + e.getMessage(), e);
        }
    }

    /**
     * Common method to fill the JasperPrint object with data and parameters.
     * This avoids code duplication between generating a file and a byte array.
     */
    private void fillJasperPrint() throws JRException, IOException {
        if (jasperPrint != null) {
            return;
        }

        java.util.List<InvoiceItem> medicineItems = new ArrayList<>();
        java.util.List<InvoiceItem> serviceItems = new ArrayList<>();
        java.util.List<InvoiceItem> supplementItems = new ArrayList<>();

        if (med != null) {
            for (String[] medicine : med) {
                if (medicine != null && medicine.length >= 12) {
                    try {
                        String medName = medicine[1];
                        String amount = medicine[2] + " " + medicine[3];
                        String dosageInfo = String.format("Sáng: %s, Trưa: %s, Chiều: %s", medicine[4], medicine[5], medicine[6]);
                        String note = medicine[9];
                        String route = medicine[11];
                        
                        // Call signature: (Tên, Ghi chú, Liều dùng, Đường dùng, Số lượng)
                        medicineItems.add(InvoiceItem.createMedicine(medName, note, dosageInfo, route, amount));
                    } catch (Exception e) {
                        log.info("Error parsing medicine data row: " + java.util.Arrays.toString(medicine) + " | Error: " + e.getMessage());
                    }
                }
            }
        }

        if (services != null) {
            for (String[] service : services) {
                if (service != null && service.length >= 5) {
                    try {
                        serviceItems.add(InvoiceItem.createService(
                            service[1], 
                            service.length > 5 ? service[5] : "", 
                            Integer.parseInt(service[2]), 
                            Double.parseDouble(service[3])
                        ));
                    } catch (NumberFormatException e) {
                        log.info("Error parsing service data: " + e.getMessage());
                    }
                }
            }
        }

        if (supplements != null) {
            for (String[] supplement : supplements) {
                if (supplement != null && supplement.length >= 12) {
                    try {
                        String supName = supplement[1];
                        String amount = supplement[2] + " " + supplement[3];
                        String dosageInfo = String.format("Sáng: %s, Trưa: %s, Chiều: %s", supplement[4], supplement[5], supplement[6]);
                        String supNote = supplement[9];
                        String supRoute = supplement[11];
                        
                        // --- CORRECTED THIS LINE ---
                        // Swapped 'amount' and 'supRoute' to match the method signature in InvoiceItem.java
                        // Call signature: (Tên, Ghi chú, Liều dùng, Số lượng, Đường dùng)
                        supplementItems.add(InvoiceItem.createSupplement(supName, supNote, dosageInfo, supRoute, amount));
                        
                    } catch (Exception e) {
                        log.info("Error parsing supplement data row: " + java.util.Arrays.toString(supplement) + " | Error: " + e.getMessage());
                    }
                }
            }
        }

        JRBeanCollectionDataSource medicineDS = new JRBeanCollectionDataSource(medicineItems);
        JRBeanCollectionDataSource serviceDS = new JRBeanCollectionDataSource(serviceItems);
        JRBeanCollectionDataSource supplementDS = new JRBeanCollectionDataSource(supplementItems);

        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        
        parameters.put("medicineDS", medicineDS);
        parameters.put("serviceDS", serviceDS);
        parameters.put("supplementDS", supplementDS);
        parameters.put(JRParameter.REPORT_LOCALE, java.util.Locale.of("vi", "VN"));
        parameters.put("patientName", patientName != null ? patientName : "");
        parameters.put("patientDOB", patientDOB != null ? patientDOB : "");
        parameters.put("patientGender", patientGender != null ? patientGender : "");
        parameters.put("patientAddress", patientAddress != null ? patientAddress : "");
        parameters.put("clinicPrefix", LocalStorage.ClinicPrefix != null ? LocalStorage.ClinicPrefix : "");
        parameters.put("clinicName", LocalStorage.ClinicName != null ? LocalStorage.ClinicName : "");
        parameters.put("clinicPhone", LocalStorage.ClinicPhone != null ? LocalStorage.ClinicPhone : "");
        parameters.put("clinicAddress", LocalStorage.ClinicAddress != null ? LocalStorage.ClinicAddress : "");
        parameters.put("doctorName", doctorName != null ? doctorName : "");
        parameters.put("checkupDate", date != null ? date : "");
        parameters.put("patientDiagnos", diagnosis != null ? diagnosis : "");
        parameters.put("checkupNote", notes != null ? notes : "");
        parameters.put("hasMedicines", !medicineItems.isEmpty());
        parameters.put("hasServices", !serviceItems.isEmpty());
        parameters.put("hasSupplements", !supplementItems.isEmpty());
        parameters.put("id", id);

        // FIXED: Generate barcode and QR code as BufferedImage instead of InputStream
        try {
            // Generate barcode and get file path
            String barcodePath = BarcodeGenerator.generateCode128WithFixedName(id, 200, 50, "invoice_barcode");
            parameters.put("barcodeNumber", barcodePath);
            
            // Generate QR code for the driveURL
            String qrcodePath = BarcodeGenerator.generateQRCodeWithFixedName(driveURL, 220, 220, "invoice_qrcode");
            parameters.put("driveURL", qrcodePath);
            
        } catch (Exception e) {
            log.error("Error generating barcode/QR code images: {}", e.getMessage());
            // Set fallback parameters as null or empty if image generation fails
            parameters.put("barcodeNumber", null);
            parameters.put("driveURL", null);
        }

        try {
            // Use the new method that tries pre-compiled .jasper files first
            JasperReport jasperReport = loadCompiledReport("medserinvoice");
            log.info("Successfully loaded/compiled jasper report");
            
            log.info("Attempting to fill report with parameters...");
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            log.info("Successfully filled jasper print");
            
            // Verify jasperPrint is not null and has content
            if (jasperPrint != null) {
                log.info("JasperPrint created successfully with {} pages", jasperPrint.getPages().size());
            } else {
                log.error("JasperPrint is null after fill operation!");
            }
        }
        catch (Exception e) {
            log.info("error loading the input stream, {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows a dialog to select paper size (A4/A5) and then opens the system print dialog.
     * @param parent The parent frame for the dialogs.
     */
    public void showPrintDialogWithOptions(JFrame parent) {
        try {
            // Step 1: Ensure the report data is compiled and filled.
            // This populates the 'jasperPrint' object.
            log.info("Preparing report data for printing...");
            fillJasperPrint();
            if (jasperPrint == null) {
                JOptionPane.showMessageDialog(parent, "Could not generate the report data.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Step 2: Show the paper size selection dialog.
            Object[] options = {"A4", "A5"};
            int choice = JOptionPane.showOptionDialog(parent,
                    "Chọn khổ giấy để in:",
                    "Print Options",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            // Step 3: If the user cancels, do nothing.
            if (choice == JOptionPane.CLOSED_OPTION) {
                log.info("User cancelled the print operation.");
                return;
            }

            // Step 4: Determine the selected paper size.
            MediaSizeName selectedPaperSize = (choice == 0) ? MediaSizeName.ISO_A4 : MediaSizeName.ISO_A5;
            log.info("User selected paper size: {}", selectedPaperSize);

            // Step 5: Call the direct printing method with the chosen size.
            printDirectly(selectedPaperSize);

        } catch (Exception e) {
            log.error("Failed during the print process with options", e);
            JOptionPane.showMessageDialog(parent, "Lỗi khi chuẩn bị in: " + e.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Private helper method to print the report directly using JRPrintServiceExporter.
     * @param paperSize The MediaSizeName (e.g., A4 or A5) to use for printing.
     * @throws JRException
     */
    private void printDirectly(MediaSizeName paperSize) throws JRException {
        // This set of instructions tells the printer to use the selected paper size
        // and allows it to scale the content down if necessary.
        PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
        printRequestAttributeSet.add(paperSize);
        printRequestAttributeSet.add(Fidelity.FIDELITY_FALSE); // CRITICAL: This allows scaling!

        // Configure the exporter with our printing instructions
        JRPrintServiceExporter exporter = new JRPrintServiceExporter();
        SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
        configuration.setPrintRequestAttributeSet(printRequestAttributeSet);
        configuration.setDisplayPageDialog(false); // Optional: Do not show page setup dialog
        configuration.setDisplayPrintDialog(true);  // Show the main system print dialog

        // Export the report to the printer
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setConfiguration(configuration);
        
        log.info("Exporting report to printer with configuration...");
        exporter.exportReport();
        log.info("Print job sent to the system.");
    }

}