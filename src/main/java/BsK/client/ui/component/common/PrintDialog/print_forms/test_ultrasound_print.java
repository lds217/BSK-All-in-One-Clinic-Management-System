package BsK.client.ui.component.common.PrintDialog.print_forms;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.JRParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class test_ultrasound_print {
    public static void main(String[] args) {
        try {
            // 1. Put the data sources into the parameters map
            Map<String, Object> parameters = new HashMap<>();

            // Set locale to Vietnamese for number formatting and dates
            parameters.put(JRParameter.REPORT_LOCALE, new Locale("vi", "VN"));

            // 2. Sample data parameters
            parameters.put("clinicName", "BSK CLINIC");
            parameters.put("clinicAddress", "123 Main Street, City, Country");
            parameters.put("clinicPhone", "0123-456-789");

            parameters.put("patientName", "Nguyễn Thị B");
            parameters.put("patientDOB", "20/07/1985");
            parameters.put("patientGender", "Nữ");
            parameters.put("patientAddress", "456 Đường DEF, Phường UVW, Quận 2, TP.HCM");
            parameters.put("patientDiagnos", "Theo dõi thai kỳ tuần 12");

            parameters.put("doctorName", "BS. Trần Văn C");
            parameters.put("checkupDate", "22/12/2024");
            
            // 3. Add the RTF content for the notes, with corrected font sizes.
            // RTF font sizes are in "half-points", so \fs24 = 12pt.
            String rtfContent = "{\\rtf1\\ansi\n" +
                "{\\fonttbl\\f0\\fnil Monospaced;\\f1\\fnil Times New Roman;}\n" +
                "{\\colortbl\\red0\\green0\\blue0;\\red51\\green51\\blue51;\\red0\\green0\\blue255;}\n" +
                "\n" +
                "\\li0\\ri0\\fi0\\f1\\fs28\\i0\\b\\ul0\\cf2 Test\\fs24\\cf1\\par\n" + // Heading at 14pt, main text at 12pt
                "\\par\n" +
                "dawdawd\\par\n" +
                "ad\\par\n" +
                " aw\\par\n" +
                "daw da\\b0 daw dad aw\\ul d awd awd \\par\n" +
                "\\ul0\\tab\\tab\\ul awdawd\\ul0\\par\n" +
                "\\li0\\ri0\\fi0\\ul0\\par\n" +
                "}";
            parameters.put("checkupNote", rtfContent);
            parameters.put("checkupConclusion", "Thai nhi phát triển bình thường, dự kiến sinh trong 3 tháng tới."); // Normal string
            parameters.put("checkupSuggestion", "Bổ sung sắt và canxi, tái khám sau 4 tuần."); // Normal string


            // 4. Add the re-checkup date
            parameters.put("reCheckupDate", "ngày 29 tháng 12 năm 2024");
            
            // 5. Add barcode number
            parameters.put("barcodeNumber", "2024001235");

            // 6. Set the number of images to display
            int numberOfImages = 4;
            parameters.put("numberImage", numberOfImages);

            // 7. Add logo image path
            String projectDir = System.getProperty("user.dir");
            String logoPath = projectDir + "/src/main/java/BsK/client/ui/assets/icon/logo.jpg";
            parameters.put("logoImage", logoPath);

            // 8. Add paths for the 6 ultrasound images
            String imageDir = projectDir + "/img_db/8/";
            String[] imageFiles = {
                "IMG_8_20250619_033320.jpg",
                "IMG_8_20250619_033621.jpg",
                "IMG_8_20250619_033622.jpg",
                "IMG_8_20250619_033623.jpg",
                "IMG_8_20250628_220921.jpg",
                "IMG_9_20250619_034013.jpg"
            };

            for (int i = 0; i < 6; i++) {
                if (i < numberOfImages) {
                    parameters.put("image" + (i + 1), imageDir + imageFiles[i]);
                } else {
                    parameters.put("image" + (i + 1), null); // Pass null for unused image slots
                }
            }
            
            // 9. Load and compile the report
            String jrxmlPath = projectDir + "/src/main/java/BsK/client/ui/component/CheckUpPage/PrintDialog/print_forms/ultrasoundresult.jrxml";
            InputStream inputStream = new FileInputStream(new File(jrxmlPath));

            JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            // 10. Fill the report. Since there's no table, we use an JREmptyDataSource.
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // 11. View the report
            JasperViewer.viewReport(jasperPrint, false); // `false` to prevent app exit on close

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 