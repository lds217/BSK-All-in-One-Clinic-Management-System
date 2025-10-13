package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class PingRequest implements Packet {
    private long timestamp;
    
    public PingRequest() {
        this.timestamp = System.currentTimeMillis();
    }
}
