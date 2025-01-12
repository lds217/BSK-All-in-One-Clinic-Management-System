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

public class MedicalInvoiceWithGUI {

    private JDialog dialog;
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JButton openDialogButton = new JButton("Open Medical Invoice");
            openDialogButton.addActionListener(e -> new MedicalInvoiceWithGUI().createDialog(null));
            JFrame frame = new JFrame("Main Application");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);
            frame.setLayout(new FlowLayout());
            frame.add(openDialogButton);
            frame.setVisible(true);
        });
    }

    private void createDialog(JFrame parent) {
        dialog = new JDialog(parent, "Medical Invoice", true);
        dialog.setSize(600, 600);
        dialog.setResizable(false);
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
        String clinicInfo = String.format("%s\n%s\nSố điện thoại: %s\nEmail: %s",
                "Clinic Name", "123 Clinic Street",
                "0123456789", "test@#mail.com");
        Paragraph clinicInfoParagraph = new Paragraph(clinicInfo)
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT)
                .setFont(font);
        document.add(clinicInfoParagraph);

        // Add title
        String title = "Toa thuốc\n";
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
                .add(new Paragraph("Phái: Nam\nNăm sinh: 2500\n" +
                        "Bác sĩ: Nguyễn Văn B"))
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.LEFT)
                .setBorder(Border.NO_BORDER)
                .setFont(font);
        patientDoctorTable.addCell(moreInfoCell);

        // Add the table to the document
        document.add(patientDoctorTable);

        // Diagnosis and notes
        String diagnosisNotes = "Chẩn đoán: Cảm cúmm m mm m m m  mm mm mm m mm m " +
                "m m m mm m m m m m m mm mm m mm m mm mm mm nm nm nm nmn m nm nm" +
                " mmn mm nm nm nmn mn mn mn mn mn mn mn mn mn mnm nm nm nmn mn mn \nGhi chú: " +
                "Nghỉ ngơi và uống nhiều nước\n\n\n\n\n\n\n\n\n\n";
        Paragraph diagnosisNotesParagraph = new Paragraph(diagnosisNotes).setFont(font);
        document.add(diagnosisNotesParagraph);


        // Add services and medicine table
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2}))
                .setFont(font)
                .setWidth(UnitValue.createPercentValue(100));
        table.addHeaderCell("Service/Medicine");
        table.addHeaderCell("Quantity");
        table.addHeaderCell("Price");
        table.addCell("Consultation\n Ngày uống 100 viên, sáng trưa chiều");
        table.addCell("1");
        table.addCell("$50");
        table.addCell("Aspirin\n Ngày uống 100 viên, sáng trưa chiều");
        table.addCell("2");
        table.addCell("$10");
        table.addCell("Paracetamol") ;
        table.addCell("1");
        table.addCell("$5");
        table.addCell("Vitamin C");
        table.addCell("1");
        table.addCell("$7");
        table.addCell("Antibiotics");
        table.addCell("1");
        table.addCell("$20");
        table.addCell("Echinacea");
        table.addCell("1");
        table.addCell("$15");
        table.addCell("Total");
        table.addCell("");
        table.addCell("$107");

        document.add(table);


        // Add signature area
        Paragraph signatureArea = new Paragraph("\n\n\nChữ Ký: ________________________")
                .setFont(font)
                .setTextAlignment(com.itextpdf.layout.property.TextAlignment.RIGHT);
        document.add(signatureArea);

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

        // Assuming `dialog` is the parent component
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.validate();
        dialog.repaint();
    }


    public static void printPdf(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setJobName("Print Medical Invoice");

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
