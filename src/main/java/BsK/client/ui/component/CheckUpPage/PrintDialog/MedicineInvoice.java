package BsK.client.ui.component.CheckUpPage.PrintDialog;

import BsK.client.LocalStorage;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.property.UnitValue;

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
    private String[][] med; // Name, Quantity, Price
    private String[][] services; // Name, Quantity, Price
    private static final DecimalFormat vndFormatter = new DecimalFormat("#,##0");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JButton openDialogButton = new JButton("Open Medicine Invoice");

            openDialogButton.addActionListener(e -> new MedicineInvoice("1212", "Nguyễn Văn A", "01/01/1990", "0123456789",
                    "Nam", "123 Đường ABC, Quận XYZ, TP HCM", "Bác sĩ XYZ", "Sốt cao",
                    "Nghỉ ngơi nhiều, uống nhiều nước",
                    new String[][]{
                            {"Paracetamol", "2", "5000"},
                            {"Vitamin C", "1", "10000"},
                            {"Amoxicillin", "1", "20000"}
                    },
                    new String[][] {
                        {"Khám tổng quát", "1", "150000"},
                        {"Xét nghiệm máu", "1", "80000"}
                    }
            ).createDialog(null));
            JFrame frame = new JFrame("Main Application");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new FlowLayout());
            frame.add(openDialogButton);
            frame.setVisible(true);
        });
    }

    public MedicineInvoice(String id, String patientName, String patientDOB, String patientPhone,
                               String patientGender, String patientAddress, String doctorName, String diagnosis,
                               String notes, String[][] med, String[][] services) {
        this.id = id;
        this.patientName = patientName;
        this.patientDOB = patientDOB;
        this.patientPhone = patientPhone;
        this.patientGender = patientGender;
        this.patientAddress = patientAddress;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.notes = notes;
        // Get today's date
        LocalDate today = LocalDate.now();

        // Format the date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = today.format(formatter);

        this.date = formattedDate;
        this.med = med;
        this.services = services;
    }


    public void createDialog(JFrame parent) {
        dialog = new JDialog(parent, "Medicine Invoice", true);
        dialog.setSize(600, 600);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

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

        // Print PDF Action
        printButton.addActionListener(e -> {
            try {
                File pdfFile = new File(pdfPath);
                printPdf(pdfFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error printing PDF: " + ex.getMessage());
            }
        });

        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void generatePdf(String pdfPath) throws Exception {
        PdfWriter writer = new PdfWriter(pdfPath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc); // Default A4 size

        // Set document margins - REDUCED
        document.setMargins(20, 25, 20, 25); // Smaller margins (top, right, bottom, left)

        String boldFontPath = "src/main/java/BsK/client/ui/assets/font/SVN-Arial Bold.ttf";
        PdfFont boldFont = PdfFontFactory.createFont(boldFontPath, "Identity-H", true);

        String fontPath = "src/main/java/BsK/client/ui/assets/font/SVN-Arial Regular.ttf";
        PdfFont font = PdfFontFactory.createFont(fontPath, "Identity-H", true);

        // --- HEADER SECTION ---
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 3.5f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(5); // Reduced margin bottom

        try {
            String logoPath = "src/main/java/BsK/client/ui/assets/icon/clinic_logo.png";
            ImageData imageData = ImageDataFactory.create(logoPath);
            Image logo = new Image(imageData).scaleToFit(60, 60).setMarginRight(15); // Slightly smaller logo & margin
            headerTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE));
        } catch (Exception e) {
            headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));
            System.err.println("Logo image not found or error loading: " + e.getMessage());
        }
        
        Paragraph clinicInfoParagraph = new Paragraph()
                .add(new Text(LocalStorage.ClinicName + "\n").setFont(boldFont).setFontSize(12)) // Reduced font
                .add(new Text("Địa chỉ: " + LocalStorage.ClinicAddress + "\n").setFontSize(9)) // Reduced font
                .add(new Text("Điện thoại: " + LocalStorage.ClinicPhone).setFontSize(9)) // Reduced font
                .setFont(font);
        headerTable.addCell(new Cell().add(clinicInfoParagraph)
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.LEFT)
                .setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER).setPaddingLeft(5)); // Reduced padding
        
        document.add(headerTable);

        // --- INVOICE TITLE ---
        Paragraph titleParagraph = new Paragraph("TOA THUỐC")
                .setFontSize(18).setFont(boldFont) // Reduced font size
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER)
                .setMarginBottom(8); // Reduced margin
        document.add(titleParagraph);

        // --- PATIENT AND INVOICE DETAILS SECTION ---
        Table patientDoctorTable = new Table(UnitValue.createPercentArray(new float[]{55, 45})) // Adjusted column widths
                .setWidth(UnitValue.createPercentValue(100))
                .setFontSize(9).setFont(font) // Reduced font size
                .setMarginBottom(8); // Reduced margin

        Cell patientInfoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(3); // Reduced padding
        Paragraph pName = new Paragraph().add(new Text("Họ và tên: ").setFont(boldFont)).add(patientName);
        pName.setMultipliedLeading(1.0f);
        patientInfoCell.add(pName);
        Paragraph pDOB = new Paragraph().add(new Text("Ngày sinh: ").setFont(boldFont)).add(patientDOB);
        pDOB.setMultipliedLeading(1.0f);
        patientInfoCell.add(pDOB);
        Paragraph pGender = new Paragraph().add(new Text("Giới tính: ").setFont(boldFont)).add(patientGender);
        pGender.setMultipliedLeading(1.0f);
        patientInfoCell.add(pGender);
        Paragraph pPhone = new Paragraph().add(new Text("Số điện thoại: ").setFont(boldFont)).add(patientPhone);
        pPhone.setMultipliedLeading(1.0f);
        patientInfoCell.add(pPhone);
        Paragraph pAddress = new Paragraph().add(new Text("Địa chỉ: ").setFont(boldFont)).add(patientAddress);
        pAddress.setMultipliedLeading(1.0f);
        patientInfoCell.add(pAddress);
        patientDoctorTable.addCell(patientInfoCell);

        Cell doctorInvoiceCell = new Cell().setBorder(Border.NO_BORDER).setPadding(3); // Reduced padding
        Paragraph pDoctor = new Paragraph().add(new Text("Bác sĩ: ").setFont(boldFont)).add(doctorName);
        pDoctor.setMultipliedLeading(1.0f);
        doctorInvoiceCell.add(pDoctor);
        Paragraph pDate = new Paragraph().add(new Text("Ngày lập: ").setFont(boldFont)).add(date);
        pDate.setMultipliedLeading(1.0f);
        doctorInvoiceCell.add(pDate);
        Paragraph pInvoiceId = new Paragraph().add(new Text("Đơn thuốc số: ").setFont(boldFont)).add(id);
        pInvoiceId.setMultipliedLeading(1.0f);
        doctorInvoiceCell.add(pInvoiceId);
        Paragraph pSpace1 = new Paragraph(" ");
        pSpace1.setMultipliedLeading(1.0f);
        doctorInvoiceCell.add(pSpace1); 
        Paragraph pSpace2 = new Paragraph(" ");
        pSpace2.setMultipliedLeading(1.0f);
        doctorInvoiceCell.add(pSpace2); 
        patientDoctorTable.addCell(doctorInvoiceCell);
        
        document.add(patientDoctorTable);

        // --- DIAGNOSIS AND NOTES ---
        Paragraph diagnosisParagraph = new Paragraph();
        diagnosisParagraph.add(new Text("Chẩn đoán: ").setFont(boldFont)).add(diagnosis);
        diagnosisParagraph.setFont(font).setFontSize(9).setMarginBottom(2).setMultipliedLeading(1.0f);
        document.add(diagnosisParagraph);

        if (notes != null && !notes.trim().isEmpty()) {
            Paragraph notesParagraph = new Paragraph();
            notesParagraph.add(new Text("Ghi chú (chung): ").setFont(boldFont)).add(notes);
            notesParagraph.setFont(font).setFontSize(9).setMarginBottom(5).setMultipliedLeading(1.0f);
            document.add(notesParagraph);
        } else {
            document.add(new Paragraph().setMarginBottom(5)); 
        }

        // --- MEDICINE TABLE ---
        Paragraph medicineSectionTitle = new Paragraph("CHI TIẾT THUỐC")
            .setFont(boldFont).setFontSize(11) // Reduced font size
            .setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER)
            .setMarginTop(3).setMarginBottom(3); // Reduced margins
        document.add(medicineSectionTitle);
        
        Table medTable = new Table(UnitValue.createPercentArray(new float[]{3.8f, 1.7f, 1.5f, 1.5f, 2.5f})) // Adjusted column widths
                .setWidth(UnitValue.createPercentValue(100))
                .setFontSize(8).setFont(font) // Reduced font size for table content
                .setMarginBottom(5); // Reduced margin

        medTable.addHeaderCell(new Cell().add(new Paragraph("Tên thuốc")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
        medTable.addHeaderCell(new Cell().add(new Paragraph("Liều dùng")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
        medTable.addHeaderCell(new Cell().add(new Paragraph("Số lượng")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
        medTable.addHeaderCell(new Cell().add(new Paragraph("Đơn giá (VNĐ)")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
        medTable.addHeaderCell(new Cell().add(new Paragraph("Thành tiền (VNĐ)")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));

        long medicineTotal = 0;
        if (med != null) {
            for (String[] medicine : med) {
                if (medicine == null || medicine.length < 10) {
                    medTable.addCell(new Cell(1, 5).add(new Paragraph("Thông tin thuốc không đầy đủ").setItalic().setFontSize(8)).setPadding(2));
                    continue;
                }
                Paragraph medNameAndNote = new Paragraph(); 
                medNameAndNote.add(new Text(medicine[1]).setFontSize(8)); 
                if (medicine[9] != null && !medicine[9].isEmpty()) {
                     medNameAndNote.add(new Text("\nGhi chú: " + medicine[9]).setFontSize(7).setItalic()); 
                }
                medNameAndNote.setMultipliedLeading(1.0f); 
                medTable.addCell(new Cell().add(medNameAndNote).setPadding(2));

                String dosageInfo = String.format("S: %s, T: %s, C: %s", medicine[4], medicine[5], medicine[6]);
                medTable.addCell(new Cell().add(new Paragraph(dosageInfo).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                medTable.addCell(new Cell().add(new Paragraph(medicine[2] + " " + medicine[3]).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                try {
                    medTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(Long.parseLong(medicine[7]))).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                    medTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(Long.parseLong(medicine[8]))).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                    medicineTotal += Long.parseLong(medicine[8]); 
                } catch (NumberFormatException e) { 
                    System.err.println("Error parsing medicine price/total: " + medicine[7] + " or " + medicine[8]);
                    medTable.addCell(new Cell().add(new Paragraph("Lỗi giá")).setFontSize(8).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE));
                    medTable.addCell(new Cell().add(new Paragraph("Lỗi giá")).setFontSize(8).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE));
                }
            }
        }
        document.add(medTable);
        
        Table medTotalTable = new Table(UnitValue.createPercentArray(new float[]{7.5f, 2.5f}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER).setMarginBottom(3); 
        medTotalTable.addCell(new Cell().add(new Paragraph("Tổng tiền thuốc:")).setFont(boldFont).setFontSize(9).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(1));
        medTotalTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(medicineTotal) + " VNĐ")).setFont(boldFont).setFontSize(9).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(1));
        document.add(medTotalTable);

        long overallTotal = medicineTotal;

        // --- SERVICES TABLE ---
        if (services != null && services.length > 0) {
            Paragraph serviceSectionTitle = new Paragraph("CHI TIẾT DỊCH VỤ")
                .setFont(boldFont).setFontSize(11) // Reduced font size
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER)
                .setMarginTop(5).setMarginBottom(3); // Reduced margins
            document.add(serviceSectionTitle);

            Table serviceTable = new Table(UnitValue.createPercentArray(new float[]{4.5f, 1.5f, 2f, 2.5f})) // Adjusted column widths
                    .setWidth(UnitValue.createPercentValue(100))
                    .setFontSize(8).setFont(font) // Reduced font size for table content
                    .setMarginBottom(5); // Reduced margin

            serviceTable.addHeaderCell(new Cell().add(new Paragraph("Tên dịch vụ")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
            serviceTable.addHeaderCell(new Cell().add(new Paragraph("Số lượng")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
            serviceTable.addHeaderCell(new Cell().add(new Paragraph("Đơn giá (VNĐ)")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));
            serviceTable.addHeaderCell(new Cell().add(new Paragraph("Thành tiền (VNĐ)")).setFont(boldFont).setFontSize(9).setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2));

            long serviceTotal = 0;
            for (String[] service : services) {
                if (service == null || service.length < 5) {
                    serviceTable.addCell(new Cell(1, 4).add(new Paragraph("Thông tin dịch vụ không đầy đủ").setItalic().setFontSize(8)).setPadding(2));
                    continue;
                }
                Paragraph serNameAndNote = new Paragraph(); 
                serNameAndNote.add(new Text(service[1]).setFontSize(8)); 
                if (service.length > 5 && service[5] != null && !service[5].isEmpty()) {
                     serNameAndNote.add(new Text("\nGhi chú: " + service[5]).setFontSize(7).setItalic()); 
                }
                serNameAndNote.setMultipliedLeading(1.0f); 
                serviceTable.addCell(new Cell().add(serNameAndNote).setPadding(2));
                serviceTable.addCell(new Cell().add(new Paragraph(service[2]).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                try {
                    serviceTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(Long.parseLong(service[3]))).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                    serviceTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(Long.parseLong(service[4]))).setFontSize(8)).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE)); 
                    serviceTotal += Long.parseLong(service[4]); 
                } catch (NumberFormatException e) { 
                    System.err.println("Error parsing service price/total: " + service[3] + " or " + service[4]);
                    serviceTable.addCell(new Cell().add(new Paragraph("Lỗi giá")).setFontSize(8).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE));
                    serviceTable.addCell(new Cell().add(new Paragraph("Lỗi giá")).setFontSize(8).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setPadding(2).setVerticalAlignment(com.itextpdf.layout.property.VerticalAlignment.MIDDLE));
                }
            }
            document.add(serviceTable);
            overallTotal += serviceTotal;

            Table serTotalTable = new Table(UnitValue.createPercentArray(new float[]{7.5f, 2.5f}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER).setMarginBottom(3); 
            serTotalTable.addCell(new Cell().add(new Paragraph("Tổng tiền dịch vụ:")).setFont(boldFont).setFontSize(9).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(1));
            serTotalTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(serviceTotal) + " VNĐ")).setFont(boldFont).setFontSize(9).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(1));
            document.add(serTotalTable);
        }

        // --- OVERALL TOTAL ---
        document.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginTop(5).setMarginBottom(3)); 
        Table overallTotalTable = new Table(UnitValue.createPercentArray(new float[]{7.5f, 2.5f}))
            .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER).setMarginBottom(10); 
        overallTotalTable.addCell(new Cell().add(new Paragraph("TỔNG CỘNG THANH TOÁN:")).setFont(boldFont).setFontSize(10).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(1)); 
        overallTotalTable.addCell(new Cell().add(new Paragraph(vndFormatter.format(overallTotal) + " VNĐ")).setFont(boldFont).setFontSize(10).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT).setBorder(Border.NO_BORDER).setPadding(1)); 
        document.add(overallTotalTable);

        // --- SIGNATURE AREA ---
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
            .setWidth(UnitValue.createPercentValue(100))
            .setBorder(Border.NO_BORDER)
            .setMarginTop(15); // Reduced margin
        
        signatureTable.addCell(new Cell().setBorder(Border.NO_BORDER)); 

        Paragraph signatureParagraph = new Paragraph(); // Initialize paragraph
        signatureParagraph.add(new Text("Ngày " + date.substring(0,2) + " tháng " + date.substring(3,5) + " năm " + date.substring(6) + "\n").setFontSize(9));
        signatureParagraph.add(new Text("Bác sĩ điều trị\n\n\n").setFontSize(9)); // Reduced newlines for space
        signatureParagraph.add(new Text("(Ký, ghi rõ họ tên)").setFontSize(8).setItalic());
        signatureParagraph.setFont(font)
            .setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER)
            .setMultipliedLeading(1.0f); // Applied to the Paragraph
        
        signatureTable.addCell(new Cell().add(signatureParagraph).setBorder(Border.NO_BORDER));
        document.add(signatureTable);

        document.close();
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
