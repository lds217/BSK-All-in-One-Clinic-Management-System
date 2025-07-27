package BsK.client;

import BsK.server.network.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import BsK.client.ui.component.CheckUpPage.DoctorItem;

@AllArgsConstructor
@Data
public class LocalStorage {
    public static String sessionId;
    public static String username;
    public static Role role;
    public static int userId;
    public static String ClinicName = "Phòng khám BSK";
    public static String ClinicAddress = "Số ABC, Đường XYZ, Quận 1, TP.HCM";
    public static String ClinicPhone = "0123456789";

    public static List<DoctorItem> doctorsName = new ArrayList<>();
    public static String[] provinces = new String[]{};
    public static String[] wards = new String[]{};
    public static Map<String, String> provinceToId = new java.util.HashMap<>();
    public static HashMap<String, String[]> cachedWards; // cache local areas later will do
    public static HashMap<String, String[][]> medUnitConversion; // hash by med id inside is unit id and unit name and conversion rate and unit price

    // these variable can be changed by settings page
    // need restart
    public static String checkupMediaBaseDir = "src/main/resources/image/checkup_media";
    public static String ULTRASOUND_FOLDER_PATH = "ANH SIEU AM";

    // does not need restart
    public static Boolean autoChangeStatusToFinished = false; // turn this on will change status to finished when checkup is save by hitting print
    
}