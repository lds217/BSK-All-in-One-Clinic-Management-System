package BsK.client;

import BsK.server.network.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LocalStorage {
    public static String username;
    public static Role role;
    public static String ClinicName = "Phòng khám BSK";
    public static String ClinicAddress = "Số ABC, Đường XYZ, Quận 1, TP.HCM";
    public static String ClinicPhone = "0123456789";
}