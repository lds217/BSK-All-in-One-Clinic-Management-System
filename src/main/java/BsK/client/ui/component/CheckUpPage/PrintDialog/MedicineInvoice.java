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
    private String[][] med; // Name, Quantity, Price
    private String[][] services; // Name, Quantity, Price
    private String[][] supplements; // Name, Quantity, Price
    private static final DecimalFormat vndFormatter = new DecimalFormat("#,##0");

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Medicine Invoice Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);
            frame.setLayout(new GridLayout(3, 2, 10, 10));

            // Test Case 1: Only Medicines
            JButton medicineOnlyButton = new JButton("Only Medicines");
            medicineOnlyButton.addActionListener(e -> new MedicineInvoice("1001", "Nguyễn Văn A", "01/01/1990", "0123456789",
                    "Nam", "123 Đường ABC, Quận XYZ, TP HCM", "Bác sĩ XYZ", "Sốt cao",
                    "Nghỉ ngơi nhiều, uống nhiều nước", "https://example.com/drive1",
                    new String[][]{
                            {"1", "Paracetamol", "2 viên", "viên", "1", "2", "1", "5000", "10000", "Uống sau ăn"}
                    },
                    new String[][]{}, // No services
                    new String[][]{} // No supplements
            ).createDialog(frame));

            // Test Case 2: Only Services
            JButton serviceOnlyButton = new JButton("Only Services");
            serviceOnlyButton.addActionListener(e -> new MedicineInvoice("1002", "Trần Thị B", "15/05/1985", "0987654321",
                    "Nữ", "456 Đường DEF, Quận ABC, TP HCM", "Bác sĩ ABC", "Khám tổng quát",
                    "Theo dõi định kỳ", "https://example.com/drive2",
                    new String[][]{}, // No medicines
                    new String[][] {
                        {"1", "Khám tổng quát", "1", "150000", "150000", "Kiểm tra sức khỏe tổng quát"},
                        {"2", "Xét nghiệm máu", "1", "80000", "80000", "Xét nghiệm công thức máu"}
                    },
                    new String[][]{} // No supplements
            ).createDialog(frame));

            // Test Case 3: Only Supplements
            JButton supplementOnlyButton = new JButton("Only Supplements");
            supplementOnlyButton.addActionListener(e -> new MedicineInvoice("1003", "Lê Văn C", "20/12/1992", "0456789123",
                    "Nam", "789 Đường GHI, Quận DEF, TP HCM", "Bác sĩ DEF", "Tư vấn dinh dưỡng",
                    "Bổ sung vitamin và khoáng chất", "https://example.com/drive3",
                    new String[][]{}, // No medicines
                    new String[][]{}, // No services
                    new String[][] {
                        {"1", "Vitamin D3", "1", "viên", "1 viên/ngày sau ăn sáng", "30 viên", "2000", "60000", "Bổ sung vitamin D"},
                        {"2", "Omega 3", "2", "viên", "2 viên/ngày sau ăn", "60 viên", "1500", "90000", "Bổ sung dầu cá"}
                    }
            ).createDialog(frame));

            // Test Case 4: Medicines + Services (No Supplements)
            JButton medServiceButton = new JButton("Medicines + Services");
            medServiceButton.addActionListener(e -> new MedicineInvoice("1004", "Phạm Thị D", "10/03/1988", "0789123456",
                    "Nữ", "321 Đường JKL, Quận GHI, TP HCM", "Bác sĩ GHI", "Viêm họng",
                    "Điều trị kháng sinh và theo dõi", "https://example.com/drive4",
                    new String[][]{
                            {"1", "Amoxicillin", "1 vỉ", "viên", "1", "1", "0", "20000", "20000", "Uống trước ăn 30 phút"}
                    },
                    new String[][] {
                        {"1", "Khám tai mũi họng", "1", "100000", "100000", "Khám chuyên khoa"}
                    },
                    new String[][]{} // No supplements
            ).createDialog(frame));

            // Test Case 5: All Three Categories
            JButton allCategoriesButton = new JButton("All Categories");
            allCategoriesButton.addActionListener(e -> new MedicineInvoice("1005", "Hoàng Văn E", "25/07/1990", "0654321987",
                    "Nam", "654 Đường MNO, Quận JKL, TP HCM", "Bác sĩ JKL", "Kiểm tra sức khỏe định kỳ",
                    "Tổng quát + bổ sung dinh dưỡng", "https://example.com/drive5",
                    new String[][]{
                            {"1", "Vitamin C", "1 hộp", "viên", "1", "0", "0", "10000", "10000", "Uống buổi sáng"}
                    },
                    new String[][] {
                        {"1", "Khám tổng quát", "1", "150000", "150000", "Kiểm tra sức khỏe tổng quát"}
                    },
                    new String[][] {
                        {"1", "Calcium 500mg", "1 lọ", "viên", "1 viên/ngày sau ăn tối", "30 viên", "2500", "75000", "Bổ sung canxi"}
                    }
            ).createDialog(frame));

            // Test Case 6: Empty Invoice (No data)
            JButton emptyButton = new JButton("Empty Invoice");
            emptyButton.addActionListener(e -> new MedicineInvoice("1006", "Võ Thị F", "05/11/1995", "0321654987",
                    "Nữ", "987 Đường PQR, Quận MNO, TP HCM", "Bác sĩ MNO", "Tư vấn y tế",
                    "Chỉ tư vấn, không kê đơn", null,
                    new String[][]{}, // No medicines
                    new String[][]{}, // No services
                    new String[][]{} // No supplements
            ).createDialog(frame));

            frame.add(medicineOnlyButton);
            frame.add(serviceOnlyButton);
            frame.add(supplementOnlyButton);
            frame.add(medServiceButton);
            frame.add(allCategoriesButton);
            frame.add(emptyButton);

            frame.setVisible(true);
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


    /**
     * Shows the invoice directly in JasperViewer without creating a custom dialog
     */
    public void showDirectJasperViewer() {
        try {
            String pdfPath = "medical_invoice.pdf";
            generatePdf(pdfPath);
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

        String pdfPath = "medical_invoice.pdf";
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
                fileChooser.setSelectedFile(new File("medical_invoice.pdf"));
                int userSelection = fileChooser.showSaveDialog(dialog);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    File generatedFile = new File(pdfPath);
                    try (InputStream in = new FileInputStream(generatedFile);
                         OutputStream out = new FileOutputStream(fileToSave)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        JOptionPane.showMessageDialog(dialog, "PDF saved: " + fileToSave.getAbsolutePath());
                    }
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

    private JasperPrint jasperPrint; // Store JasperPrint for reuse

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
    private void fillJasperPrint() throws JRException, FileNotFoundException {
        // Only fill if it hasn't been filled already
        if (jasperPrint != null) {
            return;
        }
            // 1. Create data sources for medicines, services, and supplements
            java.util.List<InvoiceItem> medicines = new ArrayList<>();
            java.util.List<InvoiceItem> serviceItems = new ArrayList<>();
            java.util.List<InvoiceItem> supplementItems = new ArrayList<>();

            // Convert medicine data to InvoiceItem objects
        if (med != null) {
            for (String[] medicine : med) {
                    if (medicine != null && medicine.length >= 10) {
                        try {
                            String dosageInfo = String.format("S: %s, T: %s, C: %s", 
                                medicine[4] != null ? medicine[4] : "0", 
                                medicine[5] != null ? medicine[5] : "0", 
                                medicine[6] != null ? medicine[6] : "0");
                            String amount = medicine[2] + " " + medicine[3];
                            
                            medicines.add(InvoiceItem.createMedicine(
                                medicine[1] != null ? medicine[1] : "",  // medName
                                dosageInfo,  // dosage
                                amount  // amount
                            ));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing medicine data: " + e.getMessage());
                        }
                    }
                }
            }

            // Convert service data to InvoiceItem objects
            if (services != null) {
            for (String[] service : services) {
                    if (service != null && service.length >= 5) {
                        try {
                            serviceItems.add(InvoiceItem.createService(
                                service[1] != null ? service[1] : "",  // serName
                                service.length > 5 && service[5] != null ? service[5] : "",  // serNote
                                service[2] != null ? Integer.parseInt(service[2]) : 0,  // serAmount
                                service[3] != null ? Double.parseDouble(service[3]) : 0.0  // serUnitPrice
                            ));
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing service data: " + e.getMessage());
                        }
                    }
                }
            }

            // Convert supplement data to InvoiceItem objects
            if (supplements != null) {
                for (String[] supplement : supplements) {
                    if (supplement != null && supplement.length >= 10) {
                        try {
                            String dosageInfo = String.format("S: %s, T: %s, C: %s",
                                    supplement[4] != null ? supplement[4] : "0",
                                    supplement[5] != null ? supplement[5] : "0",
                                    supplement[6] != null ? supplement[6] : "0");

                            String amount = supplement[2] + " " + supplement[3]; // Quantity + Unit

                            supplementItems.add(InvoiceItem.createSupplement(
                                supplement[1] != null ? supplement[1] : "",  // supName
                                supplement[9] != null ? supplement[9] : "",  // supNote
                                dosageInfo,  // supDosage
                                amount,  // supAmount
                                0.0  // supUnitPrice, since it's not displayed for supplements
                            ));
                        } catch (Exception e) {
                            System.err.println("Error parsing supplement data: " + e.getMessage());
                        }
                    }
                }
            }

            // Create data sources
            JRBeanCollectionDataSource medicineDS = new JRBeanCollectionDataSource(medicines);
            JRBeanCollectionDataSource serviceDS = new JRBeanCollectionDataSource(serviceItems);
            JRBeanCollectionDataSource supplementDS = new JRBeanCollectionDataSource(supplementItems);

            // 2. Create parameters map
            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            
            // Data sources
            parameters.put("medicineDS", medicineDS);
            parameters.put("serviceDS", serviceDS);
            parameters.put("supplementDS", supplementDS);

            // Set locale for Vietnamese formatting
            parameters.put(JRParameter.REPORT_LOCALE, new java.util.Locale("vi", "VN"));

            // Patient information
            parameters.put("patientName", patientName != null ? patientName : "");
            parameters.put("patientDOB", patientDOB != null ? patientDOB : "");
            parameters.put("patientGender", patientGender != null ? patientGender : "");
            parameters.put("patientAddress", patientAddress != null ? patientAddress : "");

            // Clinic information
            parameters.put("clinicName", LocalStorage.ClinicName != null ? LocalStorage.ClinicName : "");
            parameters.put("clinicPhone", LocalStorage.ClinicPhone != null ? LocalStorage.ClinicPhone : "");
            parameters.put("clinicAddress", LocalStorage.ClinicAddress != null ? LocalStorage.ClinicAddress : "");

            // Doctor and medical information
            parameters.put("doctorName", doctorName != null ? doctorName : "");
            parameters.put("checkupDate", date != null ? date : "");
            parameters.put("patientDiagnos", diagnosis != null ? diagnosis : "");
            parameters.put("checkupNote", notes != null ? notes : "");

            // Barcode
            parameters.put("barcodeNumber", id != null ? id : "");
            
            // Drive URL for QR Code
            parameters.put("driveURL", driveURL != null ? driveURL : "");

            // Logo image path
            String logoPath = System.getProperty("user.dir") + "/src/main/java/BsK/client/ui/assets/icon/logo.jpg";
            parameters.put("logoImage", logoPath);

            // Add conditional parameters to check if data sources have data
            parameters.put("hasMedicines", medicines != null && !medicines.isEmpty());
            parameters.put("hasServices", serviceItems != null && !serviceItems.isEmpty());
            parameters.put("hasSupplements", supplementItems != null && !supplementItems.isEmpty());

            // 3. Load and compile the JRXML template
            String jrxmlPath = System.getProperty("user.dir") + "/src/main/java/BsK/client/ui/component/CheckUpPage/PrintDialog/print_forms/medserinvoice.jrxml";
            java.io.InputStream inputStream = new java.io.FileInputStream(new java.io.File(jrxmlPath));

            JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            // 4. Fill the report with data
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
    }

    private void displayPdfInLabel(String pdfPath, JPanel pdfPanel) throws Exception {
        PDDocument document = PDDocument.load(new File(pdfPath));
        PDFRenderer pdfRenderer = new PDFRenderer(document);

        // Create a panel to hold all pages
        pdfPanel.setLayout(new BoxLayout(pdfPanel, BoxLayout.Y_AXIS)); // Stack pages vertically

        // Render each page and add it to the pdfPanel
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(i, 150); // Adjust DPI as needed

            // Scale the image to a smaller size (e.g., 50% of the original size)
            int scaledWidth = image.getWidth() / 2;  // Make it half of the original width
            int scaledHeight = image.getHeight() / 2; // Make it half of the original height

            // Scale the image using getScaledInstance
            java.awt.Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH);

            // Create an ImageIcon from the scaled image
            ImageIcon icon = new ImageIcon(scaledImage);

            // Create a label for the image and add it to the panel
            JLabel pageLabel = new JLabel(icon);
            pdfPanel.add(pageLabel);
        }
        document.close();
        // Put the panel inside a JScrollPane to make it scrollable
        JScrollPane scrollPane = new JScrollPane(pdfPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Added for horizontal scroll if needed

        // Increase scroll sensitivity
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(20); // Increase for finer control, adjust as needed
        verticalScrollBar.setBlockIncrement(100); // Increase for page-like scrolling, adjust as needed

        // Assuming `dialog` is the parent component
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.validate();
        dialog.repaint();
    }


    public static void printPdf(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setJobName("Print Medicine Invoice");

            if (printerJob.printDialog()) {
                printerJob.setPrintable(new Printable() {
                    @Override
                    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                        if (pageIndex >= document.getNumberOfPages()) {
                            return NO_SUCH_PAGE;
                        }

                        Graphics2D g2 = (Graphics2D) graphics;
                        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        try {
                            PDFRenderer pdfRenderer = new PDFRenderer(document);
                            BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300); // Render at 300 DPI

                            double pageWidth = pageImage.getWidth();
                            double pageHeight = pageImage.getHeight();

                            double scaleX = pageFormat.getImageableWidth() / pageWidth;
                            double scaleY = pageFormat.getImageableHeight() / pageHeight;
                            double scale = Math.min(scaleX, scaleY);

                            g2.scale(scale, scale);
                            g2.drawImage(pageImage, 0, 0, null);

                        } catch (IOException e) {
                            e.printStackTrace();
                            return NO_SUCH_PAGE;
                        }

                        return PAGE_EXISTS;
                    }
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
