package BsK.client;

import BsK.server.network.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;

@AllArgsConstructor
@Data
public class LocalStorage {
    public static String username;
    public static Role role;
    public static int userId;
    public static String ClinicName = "Phòng khám BSK";
    public static String ClinicAddress = "Số ABC, Đường XYZ, Quận 1, TP.HCM";
    public static String ClinicPhone = "0123456789";
    public static String[] doctorsName;
    public static String[] provinces;
    public static String[] wards;
    public static HashMap<String, String> provinceToId;
    public static HashMap<String, String[]> cachedWards; // cache local areas later will do
}