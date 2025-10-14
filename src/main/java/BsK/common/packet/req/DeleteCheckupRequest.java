package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class DeleteCheckupRequest implements Packet {
    private long checkupId;
    private int shift;

    public DeleteCheckupRequest(long checkupId, int shift) {
        this.checkupId = checkupId;
        this.shift = shift;
    }
    
}
