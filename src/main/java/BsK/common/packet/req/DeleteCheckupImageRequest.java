package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class DeleteCheckupImageRequest implements Packet {
    private String checkupId;
    private String fileName;

    public DeleteCheckupImageRequest(String checkupId, String fileName) {
        this.checkupId = checkupId;
        this.fileName = fileName;
    }
}

