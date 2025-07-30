package BsK.client.ui.component.DataDialog;

import BsK.common.entity.Patient;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

    public static void exportToExcel(List<Patient> patientList, File file) throws IOException {
        // Create a new Excel workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("DanhSachKhamBenh");

            // Define header titles
            String[] headers = {
                "Mã Khám", "Mã BN", "Họ", "Tên", "Năm Sinh", "Giới Tính", "Số Điện Thoại",
                "Địa Chỉ", "Ngày Khám", "Loại Khám", "Bác Sĩ Khám", "CCCD/DDCN", "Cân Nặng (kg)",
                "Chiều Cao (cm)", "Nhịp Tim (l/p)", "Huyết Áp (mmHg)", "Chẩn Đoán", "Kết Luận",
                "Đề Nghị", "Ghi Chú", "Ngày Tái Khám"
            };

            // Create the header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Fill data rows
            int rowNum = 1;
            for (Patient patient : patientList) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(patient.getCheckupId());
                row.createCell(1).setCellValue(patient.getCustomerId());
                row.createCell(2).setCellValue(patient.getCustomerLastName());
                row.createCell(3).setCellValue(patient.getCustomerFirstName());
                row.createCell(4).setCellValue(patient.getCustomerDob());
                row.createCell(5).setCellValue(patient.getCustomerGender());
                row.createCell(6).setCellValue(patient.getCustomerNumber());
                row.createCell(7).setCellValue(patient.getCustomerAddress());
                row.createCell(8).setCellValue(patient.getCheckupDate());
                row.createCell(9).setCellValue(patient.getCheckupType());
                row.createCell(10).setCellValue(patient.getDoctorName());
                row.createCell(11).setCellValue(patient.getCccdDdcn());
                row.createCell(12).setCellValue(patient.getCustomerWeight());
                row.createCell(13).setCellValue(patient.getCustomerHeight());
                row.createCell(14).setCellValue(patient.getHeartBeat());
                row.createCell(15).setCellValue(patient.getBloodPressure());
                row.createCell(16).setCellValue(patient.getDiagnosis());
                row.createCell(17).setCellValue(patient.getConclusion());
                row.createCell(18).setCellValue(patient.getSuggestion());
                row.createCell(19).setCellValue(patient.getNotes());
                row.createCell(20).setCellValue(patient.getReCheckupDate());
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }
}