package BsK.client.ui.component.CheckUpPage.PrintDialog;

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

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class MedicalInvoiceWithGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MedicalInvoiceWithGUI().createGUI());
    }

    private void createGUI() {
        JFrame frame = new JFrame("Medical Invoice");
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3));
        JButton previewButton = new JButton("Preview PDF");
        JButton saveButton = new JButton("Save PDF");
        JButton printButton = new JButton("Print PDF");

        buttonPanel.add(previewButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);

        JLabel pdfViewer = new JLabel();
        JScrollPane scrollPane = new JScrollPane(pdfViewer);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        String pdfPath = "medical_invoice.pdf";

        // Preview PDF Action
        previewButton.addActionListener(e -> {
            try {
                generatePdf(pdfPath);
                displayPdfInLabel(pdfPath, pdfViewer);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error displaying PDF: " + ex.getMessage());
            }
        });

        // Save PDF Action
        saveButton.addActionListener(e -> {
            try {
                generatePdf(pdfPath);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save PDF As");
                fileChooser.setSelectedFile(new File("medical_invoice.pdf"));
                int userSelection = fileChooser.showSaveDialog(frame);

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
                        JOptionPane.showMessageDialog(frame, "PDF saved: " + fileToSave.getAbsolutePath());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error saving PDF: " + ex.getMessage());
            }
        });

        // Print PDF Action
        printButton.addActionListener(e -> {
            try {
                generatePdf(pdfPath);
                File pdfFile = new File(pdfPath);
                printPdf(pdfFile);
                JOptionPane.showMessageDialog(frame, "Printing triggered.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error printing PDF: " + ex.getMessage());
            }
        });

        frame.setVisible(true);
    }

    private void generatePdf(String pdfPath) throws Exception {
        PdfWriter writer = new PdfWriter(pdfPath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);


        String boldFontPath = "src/main/java/BsK/client/ui/assets/font/SVN-Arial Bold.ttf"; // Update with your font path
        PdfFont boldFont = PdfFontFactory.createFont(boldFontPath, "Identity-H", true);

        String fontPath = "src/main/java/BsK/client/ui/assets/font/SVN-Arial Regular.ttf"; // Update with your font path
        PdfFont font = PdfFontFactory.createFont(fontPath, "Identity-H", true);

        // Add logo
        String logoPath = "/Users/lethanhdat/Documents/GitHub/ldS_BsK/src/main/java/BsK/client/ui/assets/icon/add.png"; // Path to your logo
        ImageData imageData = ImageDataFactory.create(logoPath);
        Image logo = new Image(imageData).scaleToFit(75, 75);
        logo.setFixedPosition(50, 750);
        document.add(logo);

        // Add clinic information
        String clinicInfo = "Phòng khám BSK\nĐức Hòa Long An\nPhone: (123) 456-7890\nNgày khám: 30 tháng 2 năm 2025\n\n";
        Paragraph clinicInfoParagraph = new Paragraph(clinicInfo)
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT)
                .setFont(font);
        document.add(clinicInfoParagraph);

        // Add title
        String title = "Toa thuốc\n\n";
        Paragraph titleParagraph = new Paragraph(title)
                .setFontSize(24).setFont(boldFont)
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.CENTER);
        document.add(titleParagraph);

        // Create a table with 2 columns, each taking 50% of the width
        Table patientDoctorTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));

// Add patient and doctor information in the first column (left-aligned)
        Cell patientDoctorCell = new Cell()
                .add(new Paragraph("Họ tên bệnh nhân: Lê Nguyễn A\nSố điện thoại: 0123456789\n" +
                        "Địa chỉ: 123 Patient Street"))
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setFont(font);
        patientDoctorTable.addCell(patientDoctorCell);

// Add date and invoice information in the second column (right-aligned)
        Cell moreInfoCell = new Cell()
                .add(new Paragraph("Phái: Nam\nTuổi: 25\nNgày sinh: 01/01/2000\n" +
                        "Bác sĩ: Nguyễn Văn B"))
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setFont(font);
        patientDoctorTable.addCell(moreInfoCell);

// Add the table to the document
        document.add(patientDoctorTable);

        // Diagnosis and notes
        String diagnosisNotes = "Chẩn đoán: Cảm cúm\nGhi chú: Nghỉ ngơi và uống nhiều nước";
        Paragraph diagnosisNotesParagraph = new Paragraph(diagnosisNotes).setFont(font);
        document.add(diagnosisNotesParagraph);


        // Add services and medicine table
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2}))
                .setWidth(UnitValue.createPercentValue(100));
        table.addHeaderCell("Service/Medicine");
        table.addHeaderCell("Quantity");
        table.addHeaderCell("Price");
        table.addCell("Consultation");
        table.addCell("1");
        table.addCell("$50");
        table.addCell("Aspirin");
        table.addCell("2");
        table.addCell("$10");
        document.add(table);


        // Add signature area
        Paragraph signatureArea = new Paragraph("\n\n\nChữ Ký: ________________________")
                .setFont(font)
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT);
        document.add(signatureArea);

        document.close();
    }

    private void displayPdfInLabel(String pdfPath, JLabel label) throws IOException {
        PDDocument document = PDDocument.load(new File(pdfPath));
        PDFRenderer renderer = new PDFRenderer(document);

        // Render the first page
        BufferedImage pageImage = renderer.renderImageWithDPI(0, 100); // 100 DPI for reasonable quality
        ImageIcon icon = new ImageIcon(pageImage);
        label.setIcon(icon);

        document.close();
    }

    public static void printPdf(File pdfFile) {
        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService != null) {
                DocPrintJob printJob = printService.createPrintJob();
                Doc pdfDoc = new SimpleDoc(pdfFile.toURI().toURL().openStream(),
                        javax.print.DocFlavor.INPUT_STREAM.PDF, null);
                PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
                printJob.print(pdfDoc, attributes);
                System.out.println("PDF sent to the printer.");
            } else {
                System.out.println("No default print service found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
