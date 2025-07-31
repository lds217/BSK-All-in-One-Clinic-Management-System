package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddUserRequest implements Packet {
    private String userName;
    private String password;
    private String lastName;
    private String firstName;
    private String role;
    private Boolean deleted; // Typically false for new users
}