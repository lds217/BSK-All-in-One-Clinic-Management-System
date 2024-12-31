package BsK.client;

import BsK.server.network.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LocalStorage {
    public static String username;
    public static Role role;
}