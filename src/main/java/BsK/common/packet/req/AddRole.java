package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import BsK.common.util.date.DateUtils;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AddRole implements Packet {

    private String roleName;
    private LocalDateTime lastUpdate;

    public AddRole(String roleName) {
        this.roleName = roleName;
        this.lastUpdate = LocalDateTime.now(DateUtils.VIETNAM_ZONE);
    }
}