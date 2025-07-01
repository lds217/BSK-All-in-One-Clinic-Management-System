package BsK.client.ui.component.CheckUpPage.PrintDialog.print_forms;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JRParameter;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Collectors;


public class test_print {
    public static void main(String[] args) {
        try {
            // 1. Create a master list with all item types
            List<InvoiceItem> allItems = new ArrayList<>();
            // Medicines
            allItems.add(InvoiceItem.createMedicine("Paracetamol", "2 viên/lần, 3 lần/ngày", 10, 10000.0, "Uống sau ăn"));
            allItems.add(InvoiceItem.createMedicine("Amoxicillin", "1 viên/lần, 2 lần/ngày", 20, 5000.0, "Uống trước ăn 30 phút"));
            // Services
            allItems.add(InvoiceItem.createService("Khám tổng quát", "Kiểm tra sức khoẻ định kỳ", 1, 200000.0));
            allItems.add(InvoiceItem.createService("Siêu âm ổ bụng", "", 1, 150000.0));

            // Supplements
            allItems.add(InvoiceItem.createSupplement("Vitamin C", "Uống sau ăn sáng", "1 viên/lần, 1 lần/ngày", 30, 2000.0));
            allItems.add(InvoiceItem.createSupplement("Vitamin C", "Uống sau ăn sáng", "1 viên/lần, 1 lần/ngày", 30, 2000.0));
            // 2. Filter the master list into separate lists for each type
            List<InvoiceItem> medicines = allItems.stream().filter(item -> "MED".equals(item.getType())).collect(Collectors.toList());
            List<InvoiceItem> services = allItems.stream().filter(item -> "SER".equals(item.getType())).collect(Collectors.toList());
            List<InvoiceItem> supplements = allItems.stream().filter(item -> "SUP".equals(item.getType())).collect(Collectors.toList());

            // 3. Create a JRBeanCollectionDataSource for each list
            JRBeanCollectionDataSource medicineDS = new JRBeanCollectionDataSource(medicines);
            JRBeanCollectionDataSource serviceDS = new JRBeanCollectionDataSource(services);
            JRBeanCollectionDataSource supplementDS = new JRBeanCollectionDataSource(supplements);


            // 4. Put the data sources into the parameters map
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("medicineDS", medicineDS);
            parameters.put("serviceDS", serviceDS);
            parameters.put("supplementDS", supplementDS);
            
            // Set locale to Vietnamese for number formatting
            parameters.put(JRParameter.REPORT_LOCALE, new Locale("vi", "VN"));

            // Sample data parameters
            parameters.put("name", "John Doe");
            parameters.put("clinicName", " BSK CLINIC");
            parameters.put("clinicPhone", "0123-456-789");
            parameters.put("patientName", "Nguyen Van A");
            parameters.put("patientDOB", "15/03/1990");
            parameters.put("patientGender", "Nam");
            parameters.put("patientAddress", "123 Đường ABC, Phường XYZ, Quận 1, TP.HCM");
            parameters.put("clinicAddress", "123 Main Street, City, Country");
            parameters.put("doctorName", "BS. Nguyen Van B");
            parameters.put("checkupDate", "20/12/2024");
            parameters.put("checkupNote", "Bệnh nhân cần nghỉ ngơi và tái khám sau 1 tuần");
            parameters.put("patientDiagnos", "Viêm họng cấp tính, sốt nhẹ");
            
            // Add logo image path
            String logoPath = System.getProperty("user.dir") + "/src/main/java/BsK/client/ui/assets/icon/logo.jpg";
            parameters.put("logoImage", logoPath);
            
            // Add barcode number
            parameters.put("barcodeNumber", "2024001234");

            // Use relative path for jrxml file
            String jrxmlPath = System.getProperty("user.dir") + "/src/main/java/BsK/client/ui/component/CheckUpPage/PrintDialog/print_forms/medserinvoice.jrxml";
            InputStream inputStream = new FileInputStream(new File(jrxmlPath));

            JasperDesign jasperDesign = JRXmlLoader.load(inputStream);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // view the report
            JasperViewer.viewReport(jasperPrint);

         //JasperExportManager.exportReportToPdfFile(jasperPrint, "D:\\Github Clones\\BSK-All-in-One-Clinic-Management-System\\src\\main\\resources\\print_forms\\medserinvoice.pdf");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}   
