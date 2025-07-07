package BsK.client.ui.component.CheckUpPage.PrintDialog;

import BsK.client.LocalStorage;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import com.itextpdf.layout.element.AreaBreak;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

// --- JasperReports Imports ---
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import net.sf.jasperreports.engine.JRParameter;

@Slf4j
public class UltrasoundResult {

    private JDialog dialog;
    private final String checkupId;
    private final String patientName;
    private final String patientDOB;
    private final String patientGender;
    private final String patientAddress;
    private final String doctorName;
    private final String checkupDate;
    private final String rtfContent;
    private final String conclusion;
    private final String suggestion;
    private final List<File> selectedImages;
    private final String printType; // "ngang" or "dọc"
    private final String templateTitle;

    private static final String PDF_PATH = "ultrasound_result.pdf";

    public UltrasoundResult(String checkupId, String patientName, String patientDOB, String patientGender,
                            String patientAddress, String doctorName, String checkupDate,
                            String rtfContent, String conclusion, String suggestion,
                            List<File> selectedImages, String printType, String templateTitle) {
        this.checkupId = checkupId;
        this.patientName = patientName;
        this.patientDOB = patientDOB;
        this.patientGender = patientGender;
        this.patientAddress = patientAddress;
        this.doctorName = doctorName;
        this.checkupDate = checkupDate;
        this.rtfContent = rtfContent;
        this.conclusion = conclusion;
        this.suggestion = suggestion;
        this.selectedImages = selectedImages;
        this.printType = printType;
        this.templateTitle = templateTitle;
    }

    public void showDirectJasperViewer() {
        try {
            Map<String, Object> parameters = new HashMap<>();

            // Set locale for date/number formatting
            parameters.put(JRParameter.REPORT_LOCALE, new Locale("vi", "VN"));
            
            // Populate all the string parameters
            parameters.put("clinicName", LocalStorage.ClinicName != null ? LocalStorage.ClinicName : "Phòng khám BSK");
            parameters.put("clinicAddress", LocalStorage.ClinicAddress != null ? LocalStorage.ClinicAddress : "Địa chỉ phòng khám");
            parameters.put("clinicPhone", LocalStorage.ClinicPhone != null ? LocalStorage.ClinicPhone : "SĐT phòng khám");
            parameters.put("patientName", this.patientName);
            parameters.put("patientDOB", this.patientDOB);
            parameters.put("patientGender", this.patientGender);
            parameters.put("patientAddress", this.patientAddress);
            parameters.put("doctorName", this.doctorName);
            parameters.put("checkupDate", this.checkupDate);
            parameters.put("barcodeNumber", this.checkupId);

            // RTF and plain text content
            parameters.put("checkupNote", this.rtfContent);
            parameters.put("checkupConclusion", this.conclusion);
            parameters.put("checkupSuggestion", this.suggestion);
            parameters.put("reCheckupDate", "Tái khám theo lịch hẹn"); // Placeholder, adjust as needed

            // Handle images
            int numberOfImages = this.selectedImages.size();
            parameters.put("numberImage", numberOfImages);
            
            String projectDir = System.getProperty("user.dir");
            parameters.put("logoImage", projectDir + "/src/main/java/BsK/client/ui/assets/icon/logo.jpg");

            for (int i = 0; i < 6; i++) {
                if (i < numberOfImages) {
                    parameters.put("image" + (i + 1), this.selectedImages.get(i).getAbsolutePath());
                } else {
                    parameters.put("image" + (i + 1), null); // Pass null for unused image slots
                }
            }

            // Load and compile the report
            String jrxmlPath = projectDir + "/src/main/java/BsK/client/ui/component/CheckUpPage/PrintDialog/print_forms/ultrasoundresult.jrxml";
            InputStream inputStream = new FileInputStream(new File(jrxmlPath));

            JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            // Fill the report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // View the report
            JasperViewer.viewReport(jasperPrint, false);

        } catch (Exception e) {
            log.error("Error generating or showing JasperReport for Ultrasound", e);
            JOptionPane.showMessageDialog(null, "Lỗi khi tạo báo cáo siêu âm: " + e.getMessage(), "Lỗi JasperReport", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void createDialog(JFrame parent) {
        dialog = new JDialog(parent, "Kết quả siêu âm", true);
        dialog.setSize(800, 700);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                if (parent != null) parent.setState(Frame.ICONIFIED);
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                if (parent != null && parent.getState() == Frame.ICONIFIED) parent.setState(Frame.NORMAL);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Lưu PDF");
        JButton printButton = new JButton("In PDF");
        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);

        JPanel pdfViewer = new JPanel();
        JScrollPane scrollPane = new JScrollPane(pdfViewer);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        try {
            generatePdf(PDF_PATH);
            displayPdfInLabel(PDF_PATH, pdfViewer);
        } catch (Exception e) {
            log.error("Error generating ultrasound PDF", e);
            JOptionPane.showMessageDialog(dialog, "Lỗi khi tạo PDF: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        saveButton.addActionListener(e -> savePdf());
        printButton.addActionListener(e -> printPdf());

        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private void generatePdf(String pdfPath) throws Exception {
        PdfWriter writer = new PdfWriter(pdfPath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document;

        boolean isLandscape = "ngang".equalsIgnoreCase(this.printType);
        if (isLandscape) {
            document = new Document(pdfDoc, PageSize.A4.rotate());
        } else {
            document = new Document(pdfDoc, PageSize.A4);
        }
        document.setMargins(20, 20, 20, 20);

        String boldFontPath = "src/main/java/BsK/client/ui/assets/font/SVN-Arial Bold.ttf";
        PdfFont boldFont = PdfFontFactory.createFont(boldFontPath, "Identity-H", true);
        String fontPath = "src/main/java/BsK/client/ui/assets/font/SVN-Arial Regular.ttf";
        PdfFont font = PdfFontFactory.createFont(fontPath, "Identity-H", true);

        String title = templateTitle != null ? templateTitle.toUpperCase() : "KẾT QUẢ SIÊU ÂM";

        if (isLandscape) {
            generateLandscapeLayout(document, boldFont, font, title);
        } else {
            addHeader(document, boldFont, font);
            document.add(new Paragraph(title)
                    .setFont(boldFont).setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));
            generatePortraitLayout(document, boldFont, font);
        }

        document.close();
    }

    private Cell createHeaderBlock(PdfFont boldFont, PdfFont font) {
        Cell headerContainer = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{0.3f, 0.7f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(2);
        try {
            String logoPath = "src/main/java/BsK/client/ui/assets/icon/clinic_logo.png";
            Image logo = new Image(ImageDataFactory.create(logoPath)).scaleToFit(50, 50);
            headerTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        } catch (Exception e) {
            headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));
            log.warn("Clinic logo not found", e);
        }

        Paragraph clinicInfo = new Paragraph()
                .add(new Text(LocalStorage.ClinicName + "\n").setFont(boldFont).setFontSize(10))
                .add(new Text("Địa chỉ: " + LocalStorage.ClinicAddress + "\n").setFontSize(8))
                .add(new Text("Điện thoại: " + LocalStorage.ClinicPhone).setFontSize(8))
                .setFont(font);
        headerTable.addCell(new Cell().add(clinicInfo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        
        headerContainer.add(headerTable);
        return headerContainer;
    }

    private void generateLandscapeLayout(Document document, PdfFont boldFont, PdfFont font, String title) throws Exception {
        Table titleTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(2);

        Paragraph titleParagraph = new Paragraph(title)
                .setFont(boldFont).setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2);

        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{1.2f, 0.8f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(0);

        Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPadding(1).setVerticalAlignment(VerticalAlignment.TOP);
        
        leftCell.add(createHeaderBlock(boldFont, font));

        leftCell.add(new Paragraph(title)
                .setFont(boldFont).setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2));
        
        addPatientInfo(leftCell, boldFont, font, 8);
        leftCell.add(new Paragraph("NỘI DUNG").setFont(boldFont).setFontSize(10).setMarginTop(2)
                .setTextAlignment(TextAlignment.LEFT).setUnderline());
        
        Image rtfImage = createRtfImage(this.rtfContent, PageSize.A4.rotate().getWidth() * 0.58f); 
        if (rtfImage != null) {
            // Set maximum height to prevent page overflow - ensure single page layout
            rtfImage.setMaxHeight(320); // Fixed max height to ensure single page
            rtfImage.setAutoScale(false);
            leftCell.add(rtfImage);
        }

        // Add conclusion and suggestion to left column
        leftCell.add(new Paragraph("KẾT LUẬN").setFont(boldFont).setFontSize(10).setMarginTop(3)
                .setTextAlignment(TextAlignment.LEFT).setUnderline());
        leftCell.add(new Paragraph(this.conclusion).setFont(font).setFontSize(8));
        
        leftCell.add(new Paragraph("ĐỀ NGHỊ").setFont(boldFont).setFontSize(10).setMarginTop(2)
                .setTextAlignment(TextAlignment.LEFT).setUnderline());
        leftCell.add(new Paragraph(this.suggestion).setFont(font).setFontSize(8));

        Cell rightCell = new Cell().setBorder(Border.NO_BORDER).setPadding(1).setVerticalAlignment(VerticalAlignment.TOP);
        rightCell.add(new Paragraph("HÌNH ẢNH SIÊU ÂM").setFont(boldFont).setFontSize(10)
                .setTextAlignment(TextAlignment.LEFT).setUnderline().setMarginBottom(1));
        Table imageTable = createImageTable(selectedImages);
        rightCell.add(imageTable);

        rightCell.add(createSignatureBlock(font, boldFont));

        mainTable.addCell(leftCell);
        mainTable.addCell(rightCell);
        document.add(mainTable);
    }

    private void generatePortraitLayout(Document document, PdfFont boldFont, PdfFont font) throws Exception {
        boolean multiPage = selectedImages.size() > 4;
        addPatientInfo(document, boldFont, font, 8);

        if (multiPage) {
            Table contentTable = new Table(1).setWidth(UnitValue.createPercentValue(100));
            Cell contentCell = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
            
            contentCell.add(new Paragraph("NỘI DUNG").setFont(boldFont).setFontSize(10).setMarginTop(5)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            Image rtfImage = createRtfImage(this.rtfContent, PageSize.A4.getWidth() - 60);
            if (rtfImage != null) {
                contentCell.add(rtfImage);
            }

            contentCell.add(new Paragraph("KẾT LUẬN").setFont(boldFont).setFontSize(10).setMarginTop(5)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            contentCell.add(new Paragraph(this.conclusion).setFont(font).setFontSize(8));
            contentCell.add(new Paragraph("ĐỀ NGHỊ").setFont(boldFont).setFontSize(10).setMarginTop(5)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            contentCell.add(new Paragraph(this.suggestion).setFont(font).setFontSize(8));
            
            contentTable.addCell(contentCell);
            document.add(contentTable);
            addSignature(document, font, boldFont);

            document.add(new AreaBreak());
            addHeader(document, boldFont, font);
            document.add(new Paragraph("HÌNH ẢNH SIÊU ÂM (tt)").setFont(boldFont).setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            Table imageTable = createImageTable(selectedImages);
            document.add(imageTable);
        } else {
            Table mainTable = new Table(UnitValue.createPercentArray(new float[]{1.2f, 0.8f}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginTop(5);
            
            Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(5);
            leftCell.add(new Paragraph("NỘI DUNG").setFont(boldFont).setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            Image rtfImage = createRtfImage(this.rtfContent, PageSize.A4.getWidth() * 0.55f - 40);
            if (rtfImage != null) {
                leftCell.add(rtfImage);
            }

            Cell rightCell = new Cell().setBorder(Border.NO_BORDER);
            rightCell.add(new Paragraph("HÌNH ẢNH").setFont(boldFont).setFontSize(10)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            Table imageTable = createImageTable(selectedImages);
            rightCell.add(imageTable);

            mainTable.addCell(leftCell);
            mainTable.addCell(rightCell);
            document.add(mainTable);
            
            document.add(new Paragraph("KẾT LUẬN").setFont(boldFont).setFontSize(10).setMarginTop(5)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            document.add(new Paragraph(this.conclusion).setFont(font).setFontSize(8));
            document.add(new Paragraph("ĐỀ NGHỊ").setFont(boldFont).setFontSize(10).setMarginTop(5)
                    .setTextAlignment(TextAlignment.LEFT).setUnderline());
            document.add(new Paragraph(this.suggestion).setFont(font).setFontSize(8));

            addSignature(document, font, boldFont);
        }
    }

    private Table createImageTable(List<File> images) throws IOException {
        int numImages = images.size();
        int numCols = numImages == 1 ? 1 : 2;
        
        Table imageTable = new Table(numCols)
                .useAllAvailableWidth()
                .setMarginTop(1).setMarginBottom(1);
        
        for(File imgFile : images) {
            Image img = new Image(ImageDataFactory.create(imgFile.getAbsolutePath()));
            
            img.setAutoScale(true);
            img.setMaxWidth(110f);
            img.setMaxHeight(85f);
            
            Cell imgCell = new Cell()
                    .add(img)
                    .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                    .setPadding(1)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            imageTable.addCell(imgCell);
        }
        
        int remainder = numImages % numCols;
        if (remainder != 0 && numImages > 1) {
            imageTable.addCell(new Cell().setBorder(Border.NO_BORDER));
        }
        return imageTable;
    }

    private Image createRtfImage(String rtf, float availableWidth) {
        if (rtf == null || rtf.trim().isEmpty()) {
            return null;
        }
        try {
            JTextPane pane = new JTextPane();
            pane.setEditorKit(new RTFEditorKit());
            pane.setBackground(Color.WHITE);
            pane.setOpaque(true);

            pane.read(new ByteArrayInputStream(rtf.getBytes("ISO-8859-1")), null);

            pane.setSize(new Dimension((int) availableWidth, Integer.MAX_VALUE));
            Dimension prefSize = pane.getPreferredSize();
            pane.setSize(prefSize);

            int renderDPI = 240; // Further reduced DPI to make text appear even smaller
            float scale = renderDPI / 72f;
            
            int imgWidth = (int) (prefSize.width * scale);
            int imgHeight = (int) (prefSize.height * scale);
            
            if (imgWidth <= 0 || imgHeight <= 0) {
                log.warn("Invalid image dimensions for RTF rendering: {}x{}", imgWidth, imgHeight);
                return null;
            }

            BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            
            g2d.scale(scale, scale);
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            pane.paint(g2d);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            Image itextImage = new Image(ImageDataFactory.create(baos.toByteArray()));
            
            // Set width directly. The reduced DPI has already made the content smaller.
            itextImage.setWidth(UnitValue.createPointValue(prefSize.width));
            
            return itextImage;

        } catch (Exception e) {
            log.error("Failed to convert RTF to Image", e);
            return null;
        }
    }

    private void addHeader(Document document, PdfFont boldFont, PdfFont font) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1.2f, 3.8f, 2f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(5);
        try {
            String logoPath = "src/main/java/BsK/client/ui/assets/icon/clinic_logo.png";
            Image logo = new Image(ImageDataFactory.create(logoPath)).scaleToFit(50, 50);
            headerTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        } catch (Exception e) {
            headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));
            log.warn("Clinic logo not found", e);
        }
        
        Paragraph clinicInfo = new Paragraph()
                .add(new Text(LocalStorage.ClinicName + "\n").setFont(boldFont).setFontSize(10))
                .add(new Text("Địa chỉ: " + LocalStorage.ClinicAddress + "\n").setFontSize(8))
                .add(new Text("Điện thoại: " + LocalStorage.ClinicPhone).setFontSize(8))
                .setFont(font);
        headerTable.addCell(new Cell().add(clinicInfo).setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE));
        
        headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));
        document.add(headerTable);
    }
    
    private void addPatientInfo(Object container, PdfFont boldFont, PdfFont font, int fontSize) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1,1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setFontSize(fontSize).setFont(font)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f))
                .setMarginBottom(1); // Reduced margin for compactness
                
        Cell left = new Cell().setBorder(Border.NO_BORDER).setPadding(1);
        left.add(new Paragraph().add(new Text("Họ tên: ").setFont(boldFont)).add(patientName));
        left.add(new Paragraph().add(new Text("Năm sinh: ").setFont(boldFont)).add(patientDOB));
        left.add(new Paragraph().add(new Text("Giới tính: ").setFont(boldFont)).add(patientGender));

        Cell right = new Cell().setBorder(Border.NO_BORDER).setPadding(1);
        right.add(new Paragraph().add(new Text("Địa chỉ: ").setFont(boldFont)).add(patientAddress));
        right.add(new Paragraph().add(new Text("Ngày khám: ").setFont(boldFont)).add(checkupDate));
        right.add(new Paragraph().add(new Text("Mã khám: ").setFont(boldFont)).add(checkupId));

        table.addCell(left);
        table.addCell(right);
        
        if (container instanceof Document) {
            ((Document) container).add(table);
        } else if (container instanceof Cell) {
            ((Cell) container).add(table);
        }
    }
    
    private void addSignature(Document document, PdfFont font, PdfFont boldFont) {
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginTop(5);
            
        signatureTable.addCell(new Cell().setBorder(Border.NO_BORDER)); // empty cell for alignment

        Paragraph signParagraph = new Paragraph()
            .add("Ngày " + checkupDate.substring(0,2) + " tháng " + checkupDate.substring(3,5) + " năm " + checkupDate.substring(6) + "\n")
            .add(new Text("BÁC SĨ ĐIỀU TRỊ\n").setFont(boldFont))
            .add(new Text("(Ký và ghi rõ họ tên)\n\n").setFontSize(9).setItalic()) // Reduced newlines for signature space
            .add(doctorName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFont(font).setFontSize(10);

        signatureTable.addCell(new Cell().add(signParagraph).setBorder(Border.NO_BORDER));
        document.add(signatureTable);
    }

    private void displayPdfInLabel(String pdfPath, JPanel pdfPanel) {
        pdfPanel.removeAll();
        pdfPanel.setLayout(new BoxLayout(pdfPanel, BoxLayout.Y_AXIS));

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            float scale = 0.6f;
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(i, 120);
                int newWidth = (int) (image.getWidth() * scale);
                int newHeight = (int) (image.getHeight() * scale);
                ImageIcon icon = new ImageIcon(image.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH));
                JLabel pageLabel = new JLabel(icon);
                pageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                pageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                pdfPanel.add(pageLabel);
            }
        } catch (IOException e) {
            log.error("Error displaying PDF", e);
            pdfPanel.add(new JLabel("Lỗi khi hiển thị PDF: " + e.getMessage()));
        }
        pdfPanel.revalidate();
        pdfPanel.repaint();
    }

    private void savePdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu file PDF");
        fileChooser.setSelectedFile(new File("ultrasound_" + checkupId + ".pdf"));
        int userSelection = fileChooser.showSaveDialog(dialog);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                generatePdf(fileToSave.getAbsolutePath());
                JOptionPane.showMessageDialog(dialog, "PDF đã được lưu: " + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                log.error("Error saving PDF", ex);
                JOptionPane.showMessageDialog(dialog, "Lỗi khi lưu PDF: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void printPdf() {
        File pdfFile = new File(PDF_PATH);
         try (PDDocument document = PDDocument.load(pdfFile)) {
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            printerJob.setJobName("In kết quả siêu âm - " + patientName);

            if (printerJob.printDialog()) {
                printerJob.setPrintable(new Printable() {
                    @Override
                    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                        if (pageIndex >= document.getNumberOfPages()) {
                            return NO_SUCH_PAGE;
                        }
                        try {
                            PDFRenderer pdfRenderer = new PDFRenderer(document);
                            BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300);

                            Graphics2D g2d = (Graphics2D) graphics;
                            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                            
                            double scale = Math.min(pageFormat.getImageableWidth() / pageImage.getWidth(), pageFormat.getImageableHeight() / pageImage.getHeight());
                            g2d.drawImage(pageImage, 0, 0, (int) (pageImage.getWidth() * scale), (int) (pageImage.getHeight() * scale), null);
                            
                            return PAGE_EXISTS;
                        } catch (IOException e) {
                            log.error("Error rendering page for printing", e);
                            throw new PrinterException(e.getMessage());
                        }
                    }
                });
                printerJob.print();
            }
        } catch (Exception e) {
            log.error("Error printing PDF", e);
            JOptionPane.showMessageDialog(dialog, "Lỗi khi in PDF: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Table createSignatureBlock(PdfFont font, PdfFont boldFont) {
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginTop(2); // Reduced margin for compactness
            
        signatureTable.addCell(new Cell().setBorder(Border.NO_BORDER)); // empty cell for alignment

        Paragraph signParagraph = new Paragraph()
            .add("Ngày " + checkupDate.substring(0,2) + " tháng " + checkupDate.substring(3,5) + " năm " + checkupDate.substring(6) + "\n")
            .add(new Text("BÁC SĨ ĐIỀU TRỊ\n").setFont(boldFont))
            .add(new Text("(Ký và ghi rõ họ tên)\n\n").setFontSize(9).setItalic()) // Reduced newlines for signature space
            .add(doctorName)
            .setTextAlignment(TextAlignment.CENTER)
            .setFont(font).setFontSize(10);

        signatureTable.addCell(new Cell().add(signParagraph).setBorder(Border.NO_BORDER));
        return signatureTable;
    }


} 