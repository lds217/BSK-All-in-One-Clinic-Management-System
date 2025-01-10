package BsK.client.ui.component.CheckUpPage.PrintDialog;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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

        // Add logo
        String logoPath = "/Users/lethanhdat/Documents/GitHub/ldS_BsK/src/main/java/BsK/client/ui/assets/icon/add.png"; // Path to your logo
        ImageData imageData = ImageDataFactory.create(logoPath);
        Image logo = new Image(imageData).scaleToFit(100, 100);
        logo.setFixedPosition(50, 750);
        document.add(logo);

        // Add clinic information
        String clinicInfo = "Your Clinic Name\n123 Clinic Street\nCity, State, ZIP\nPhone: (123) 456-7890\nEmail: clinic@example.com";
        Paragraph clinicInfoParagraph = new Paragraph(clinicInfo).setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT);
        document.add(clinicInfoParagraph);

        // Add patient and doctor names
        String patientDoctorInfo = "Patient: John Doe\nDoctor: Dr. Jane Smith";
        Paragraph patientDoctorParagraph = new Paragraph(patientDoctorInfo);
        document.add(patientDoctorParagraph);

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

        // Add diagnosis and notes
        String diagnosisNotes = "Diagnosis: Flu\nNotes: Rest and hydration recommended";
        Paragraph diagnosisNotesParagraph = new Paragraph(diagnosisNotes);
        document.add(diagnosisNotesParagraph);

        // Add signature area
        Paragraph signatureArea = new Paragraph("\n\n\nSignature: ________________________");
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
