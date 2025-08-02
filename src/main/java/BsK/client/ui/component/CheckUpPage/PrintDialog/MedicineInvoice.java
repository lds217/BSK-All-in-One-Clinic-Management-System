package BsK.client.ui.component.CheckUpPage.PrintDialog;

import BsK.client.LocalStorage;
import BsK.client.ui.component.CheckUpPage.PrintDialog.print_forms.InvoiceItem;
// JasperReports imports
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;

public class MedicineInvoice{

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
    public void showDirectJasperViewer() {
        try {
            // This method now only fills the report and shows the viewer
            fillJasperPrint();
            // Use the constructor to prevent the application from closing
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error generating PDF: " + e.getMessage());
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
            displayPdfInLabel(pdfPath, pdfViewer);
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
                        String dosageInfo = String.format("S: %s, T: %s, C: %s", medicine[4], medicine[5], medicine[6]);
                        String note = medicine[9];
                        String route = medicine[11];
                        
                        // Call signature: (Tên, Ghi chú, Liều dùng, Đường dùng, Số lượng)
                        medicineItems.add(InvoiceItem.createMedicine(medName, note, dosageInfo, route, amount));
                    } catch (Exception e) {
                        System.err.println("Error parsing medicine data row: " + java.util.Arrays.toString(medicine) + " | Error: " + e.getMessage());
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
                        System.err.println("Error parsing service data: " + e.getMessage());
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
                        String dosageInfo = String.format("S: %s, T: %s, C: %s", supplement[4], supplement[5], supplement[6]);
                        String supNote = supplement[9];
                        String supRoute = supplement[11];
                        
                        // --- CORRECTED THIS LINE ---
                        // Swapped 'amount' and 'supRoute' to match the method signature in InvoiceItem.java
                        // Call signature: (Tên, Ghi chú, Liều dùng, Số lượng, Đường dùng)
                        supplementItems.add(InvoiceItem.createSupplement(supName, supNote, dosageInfo, supRoute, amount));
                        
                    } catch (Exception e) {
                         System.err.println("Error parsing supplement data row: " + java.util.Arrays.toString(supplement) + " | Error: " + e.getMessage());
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
        parameters.put(JRParameter.REPORT_LOCALE, new java.util.Locale("vi", "VN"));
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
        parameters.put("barcodeNumber", id != null ? id : "");
        parameters.put("driveURL", driveURL != null ? driveURL : "");
        parameters.put("hasMedicines", !medicineItems.isEmpty());
        parameters.put("hasServices", !serviceItems.isEmpty());
        parameters.put("hasSupplements", !supplementItems.isEmpty());

        String logoPath = "/BsK/client/ui/assets/icon/logo.jpg";
        // Load the logo as an InputStream
        InputStream logoStream = MedicineInvoice.class.getResourceAsStream(logoPath);
        if (logoStream == null) {
            System.err.println("ERROR: Logo image not found on classpath: " + logoPath);
            parameters.put("logoImage", null);
        } else {
            // Pass the InputStream to the report parameter
            parameters.put("logoImage", logoStream);
        }
        String resourcePath = "/BsK/client/ui/component/CheckUpPage/PrintDialog/print_forms/medserinvoice.jrxml";
        try (InputStream inputStream = MedicineInvoice.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {

                throw new FileNotFoundException("Không thể tìm thấy tệp mẫu Jasper report trong classpath: " + resourcePath);
            }
            JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
        }
    }

    private void displayPdfInLabel(String pdfPath, JPanel pdfPanel) throws Exception {
        PDDocument document = PDDocument.load(new File(pdfPath));
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        pdfPanel.setLayout(new BoxLayout(pdfPanel, BoxLayout.Y_AXIS));

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(i, 150);
            ImageIcon icon = new ImageIcon(image);
            JLabel pageLabel = new JLabel(icon);
            pdfPanel.add(pageLabel);
        }
        document.close();
        
        dialog.revalidate();
        dialog.repaint();
    }


    public static void printPdf(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setJobName("Print Medicine Invoice");

            if (printerJob.printDialog()) {
                printerJob.setPrintable((graphics, pageFormat, pageIndex) -> {
                    if (pageIndex >= document.getNumberOfPages()) {
                        return Printable.NO_SUCH_PAGE;
                    }

                    Graphics2D g2 = (Graphics2D) graphics;
                    g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    try {
                        PDFRenderer pdfRenderer = new PDFRenderer(document);
                        BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300);

                        double scale = Math.min(pageFormat.getImageableWidth() / pageImage.getWidth(),
                                                pageFormat.getImageableHeight() / pageImage.getHeight());
                        
                        g2.scale(scale, scale);
                        g2.drawImage(pageImage, 0, 0, null);

                    } catch (IOException e) {
                        e.printStackTrace();
                        return Printable.NO_SUCH_PAGE;
                    }

                    return Printable.PAGE_EXISTS;
                });

                printerJob.print();
                System.out.println("Printing completed.");
            } else {
                System.out.println("Print job canceled.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error printing PDF: " + e.getMessage());
        }
    }

}