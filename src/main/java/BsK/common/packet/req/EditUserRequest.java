package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EditUserRequest implements Packet {
    private String id;
    private String userName;
    private String password; // Can be empty/null if not changing
    private String lastName;
    private String firstName;
    private String role;
    private Boolean deleted;
}