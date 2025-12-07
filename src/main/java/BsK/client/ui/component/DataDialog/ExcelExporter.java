package BsK.client.ui.component.DataDialog;

import BsK.common.entity.DoctorItem;
import BsK.common.entity.Patient;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelExporter {

    /**
     * Finds a doctor's name from a list based on their ID.
     * @param id The doctor ID to look for.
     * @param doctorList The list of all available doctors.
     * @return The doctor's name, or an empty string if not found.
     */
    private static String getDoctorNameById(String id, List<DoctorItem> doctorList) {
        if (id == null || id.isEmpty() || id.equals("0") || doctorList == null) {
            return "";
        }
        for (DoctorItem doctor : doctorList) {
            if (doctor.getId().equals(id)) {
                return doctor.getName();
            }
        }
        return ""; // Return empty if no match is found
    }

    // <<< MODIFIED: Method signature now accepts the list of doctors
    public static void exportToExcel(List<Patient> patientList, List<DoctorItem> doctorList, File file) throws IOException {
        // Create a new Excel workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("DanhSachKhamBenh");

            // <<< MODIFIED: Added "Bác Sĩ Siêu Âm" column
            String[] headers = {
                "Mã Khám", "Mã BN", "Họ", "Tên", "Năm Sinh", "Giới Tính", "Số Điện Thoại",
                "Địa Chỉ", "Ngày Khám", "Loại Khám", "Bác Sĩ Khám", "Bác Sĩ Siêu Âm", "CCCD/DDCN", "Cân Nặng (kg)",
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
                String ultrasoundDoctorName = getDoctorNameById(patient.getDoctorUltrasoundId(), doctorList);
                row.createCell(11).setCellValue(ultrasoundDoctorName);
                row.createCell(12).setCellValue(patient.getCccdDdcn());
                row.createCell(13).setCellValue(patient.getCustomerWeight());
                row.createCell(14).setCellValue(patient.getCustomerHeight());
                row.createCell(15).setCellValue(patient.getHeartBeat());
                row.createCell(16).setCellValue(patient.getBloodPressure());
                row.createCell(17).setCellValue(patient.getDiagnosis());
                row.createCell(18).setCellValue(patient.getConclusion());
                row.createCell(19).setCellValue(patient.getSuggestion());
                row.createCell(20).setCellValue(patient.getNotes());
                row.createCell(21).setCellValue(patient.getReCheckupDate());
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * Creates a multi-sheet Excel template with headers only (blank data).
     * Sheet 1: Patient/Checkup Info (always included)
     * Sheet 2: Services Used (if includeService is true)
     * Sheet 3: Medicines Used (if includeMedicine is true)
     * 
     * @param file The output file to write to.
     * @param includeMedicine Whether to include the medicine sheet.
     * @param includeService Whether to include the service sheet.
     */
    public static void exportTemplateMultiSheet(File file, boolean includeMedicine, boolean includeService) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // ========== Sheet 1: Thông Tin Bệnh Nhân (Always included) ==========
            XSSFSheet patientSheet = workbook.createSheet("ThongTinBenhNhan");
            String[] patientHeaders = {
                "Mã Khám",           // Checkup ID - for linking with other sheets
                "Họ Tên",
                "Ngày Tháng Năm Sinh",
                "Địa Chỉ",
                "SĐT",
                "Ngày Khám",
                "Chuẩn Đoán",
                "Kết Luận Siêu Âm",
                "Đề Nghị",
                
            };
            Row patientHeaderRow = patientSheet.createRow(0);
            for (int i = 0; i < patientHeaders.length; i++) {
                Cell cell = patientHeaderRow.createCell(i);
                cell.setCellValue(patientHeaders[i]);
            }
            // Auto-size columns for better readability
            for (int i = 0; i < patientHeaders.length; i++) {
                patientSheet.setColumnWidth(i, 5000); // ~17 characters wide
            }

            // ========== Sheet 2: Dịch Vụ Sử Dụng (if includeService) ==========
            if (includeService) {
                XSSFSheet serviceSheet = workbook.createSheet("DichVuSuDung");
                String[] serviceHeaders = {
                    "Mã Khám",           // Checkup ID - links to patient sheet
                    "Tên Dịch Vụ",
                    "Số Lượng",
                    "Đơn Giá",
                    "Thành Tiền"
                };
                Row serviceHeaderRow = serviceSheet.createRow(0);
                for (int i = 0; i < serviceHeaders.length; i++) {
                    Cell cell = serviceHeaderRow.createCell(i);
                    cell.setCellValue(serviceHeaders[i]);
                }
                for (int i = 0; i < serviceHeaders.length; i++) {
                    serviceSheet.setColumnWidth(i, 5000);
                }
            }

            // ========== Sheet 3: Thuốc Sử Dụng (if includeMedicine) ==========
            if (includeMedicine) {
                XSSFSheet medicineSheet = workbook.createSheet("ThuocSuDung");
                String[] medicineHeaders = {
                    "Mã Khám",           // Checkup ID - links to patient sheet
                    "Tên Thuốc",
                    "Số Lượng",
                    "Đơn Vị",
                    "Đơn Giá",
                    "Thành Tiền",
                    "Liều Dùng"
                };
                Row medicineHeaderRow = medicineSheet.createRow(0);
                for (int i = 0; i < medicineHeaders.length; i++) {
                    Cell cell = medicineHeaderRow.createCell(i);
                    cell.setCellValue(medicineHeaders[i]);
                }
                for (int i = 0; i < medicineHeaders.length; i++) {
                    medicineSheet.setColumnWidth(i, 5000);
                }
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * Exports data from backend response to a multi-sheet Excel file.
     * Sheet 1: Patient/Checkup Info (always included)
     * Sheet 2: Services Used (if includeService is true and data exists)
     * Sheet 3: Medicines Used (if includeMedicine is true and data exists)
     * 
     * @param patientData The patient/checkup data from backend (each row: [checkupId, fullName, dob, address, phone, checkupDate, diagnosis, conclusion, suggestion])
     * @param medicineData Map of checkupId -> list of medicine items (each: [checkupId, medName, quantity, unit, unitPrice, totalPrice, dosage])
     * @param serviceData Map of checkupId -> list of service items (each: [checkupId, serviceName, quantity, unitPrice, totalPrice])
     * @param includeMedicine Whether medicine data was requested
     * @param includeService Whether service data was requested
     * @param file The output file to write to.
     */
    public static void exportWithData(
            String[][] patientData,
            Map<String, List<String[]>> medicineData,
            Map<String, List<String[]>> serviceData,
            boolean includeMedicine,
            boolean includeService,
            File file) throws IOException {
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // ========== Sheet 1: Thông Tin Bệnh Nhân ==========
            XSSFSheet patientSheet = workbook.createSheet("ThongTinBenhNhan");
            String[] patientHeaders = {
                "Mã Khám",
                "Họ Tên",
                "Ngày Tháng Năm Sinh",
                "Địa Chỉ",
                "SĐT",
                "Ngày Khám",
                "Chuẩn Đoán",
                "Kết Luận Siêu Âm",
                "Đề Nghị"
            };
            
            // Create header row
            Row patientHeaderRow = patientSheet.createRow(0);
            for (int i = 0; i < patientHeaders.length; i++) {
                Cell cell = patientHeaderRow.createCell(i);
                cell.setCellValue(patientHeaders[i]);
            }
            
            // Fill patient data
            if (patientData != null) {
                int rowNum = 1;
                for (String[] row : patientData) {
                    Row dataRow = patientSheet.createRow(rowNum++);
                    for (int i = 0; i < row.length && i < patientHeaders.length; i++) {
                        dataRow.createCell(i).setCellValue(row[i] != null ? row[i] : "");
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < patientHeaders.length; i++) {
                patientSheet.setColumnWidth(i, 5000);
            }

            // ========== Sheet 2: Dịch Vụ Sử Dụng ==========
            if (includeService) {
                XSSFSheet serviceSheet = workbook.createSheet("DichVuSuDung");
                String[] serviceHeaders = {
                    "Mã Khám",
                    "Tên Dịch Vụ",
                    "Số Lượng",
                    "Đơn Giá",
                    "Thành Tiền"
                };
                
                Row serviceHeaderRow = serviceSheet.createRow(0);
                for (int i = 0; i < serviceHeaders.length; i++) {
                    Cell cell = serviceHeaderRow.createCell(i);
                    cell.setCellValue(serviceHeaders[i]);
                }
                
                // Fill service data
                if (serviceData != null) {
                    int rowNum = 1;
                    for (Map.Entry<String, List<String[]>> entry : serviceData.entrySet()) {
                        for (String[] svcRow : entry.getValue()) {
                            Row dataRow = serviceSheet.createRow(rowNum++);
                            for (int i = 0; i < svcRow.length && i < serviceHeaders.length; i++) {
                                dataRow.createCell(i).setCellValue(svcRow[i] != null ? svcRow[i] : "");
                            }
                        }
                    }
                }
                
                for (int i = 0; i < serviceHeaders.length; i++) {
                    serviceSheet.setColumnWidth(i, 5000);
                }
            }

            // ========== Sheet 3: Thuốc Sử Dụng ==========
            if (includeMedicine) {
                XSSFSheet medicineSheet = workbook.createSheet("ThuocSuDung");
                String[] medicineHeaders = {
                    "Mã Khám",
                    "Tên Thuốc",
                    "Số Lượng",
                    "Đơn Vị",
                    "Đơn Giá",
                    "Thành Tiền",
                    "Liều Dùng"
                };
                
                Row medicineHeaderRow = medicineSheet.createRow(0);
                for (int i = 0; i < medicineHeaders.length; i++) {
                    Cell cell = medicineHeaderRow.createCell(i);
                    cell.setCellValue(medicineHeaders[i]);
                }
                
                // Fill medicine data
                if (medicineData != null) {
                    int rowNum = 1;
                    for (Map.Entry<String, List<String[]>> entry : medicineData.entrySet()) {
                        for (String[] medRow : entry.getValue()) {
                            Row dataRow = medicineSheet.createRow(rowNum++);
                            for (int i = 0; i < medRow.length && i < medicineHeaders.length; i++) {
                                dataRow.createCell(i).setCellValue(medRow[i] != null ? medRow[i] : "");
                            }
                        }
                    }
                }
                
                for (int i = 0; i < medicineHeaders.length; i++) {
                    medicineSheet.setColumnWidth(i, 5000);
                }
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }
        }
    }
}