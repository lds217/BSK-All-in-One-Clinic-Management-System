package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class PongResponse implements Packet {
    private long timestamp;
    
    public PongResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public PongResponse(long clientTimestamp) {
        this.timestamp = clientTimestamp;
    }
}

