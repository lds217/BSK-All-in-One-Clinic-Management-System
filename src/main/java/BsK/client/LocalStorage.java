package BsK.client;

import BsK.common.entity.DoctorItem;
import BsK.server.network.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date; 

@AllArgsConstructor
@Data
public class LocalStorage {
    public static String sessionId;
    public static String username;
    public static Role role;
    public static int userId;
    
    public static List<String> chatHistory = new ArrayList<>();
    public static String pathToProject = "";
    
    public static String dataDialogSearchTerm = "";
    public static Date dataDialogFromDate = new Date();
    public static Date dataDialogToDate = new Date();
    public static String dataDialogDoctorName = "Tất cả";

    public static List<DoctorItem> doctorsName = new ArrayList<>();
    public static String[] provinces = new String[]{};
    public static String[] wards = new String[]{};
    public static Map<String, String> provinceToId = new java.util.HashMap<>();
    public static HashMap<String, String[]> cachedWards;
    public static HashMap<String, String[][]> medUnitConversion;

    // --- Settings managed in the SettingsDialog ---

    // these variable can be changed by settings page
    // need restart
    public static String checkupMediaBaseDir = "image/checkup_media";
    public static String ULTRASOUND_FOLDER_PATH = "ANH SIEU AM";
    public static String ClinicName = "Phòng khám BSK";
    public static String ClinicAddress = "Số ABC, Đường XYZ, Quận 1, TP.HCM";
    public static String ClinicPhone = "0123456789";
    public static String ClinicPrefix = "BSK";
    public static String serverAddress = "127.0.0.1";
    public static String serverPort = "1999";
    public static boolean googleDriveEnabled = true;
    public static String googleDriveRootFolder = "BSK_Clinic_Patient_Files";
    
    // does not need restart
    public static Boolean autoChangeStatusToFinished = false;
    
}