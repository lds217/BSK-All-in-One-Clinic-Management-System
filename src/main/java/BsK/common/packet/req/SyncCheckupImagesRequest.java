package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class SyncCheckupImagesRequest implements Packet {
    private String checkupId;

    public SyncCheckupImagesRequest(String checkupId) {
        this.checkupId = checkupId;
    }
} 