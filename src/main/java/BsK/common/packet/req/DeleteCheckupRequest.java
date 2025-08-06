package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class DeleteCheckupRequest implements Packet {
    private long checkupId;

    public DeleteCheckupRequest(long checkupId) {
        this.checkupId = checkupId;
    }
    
}
