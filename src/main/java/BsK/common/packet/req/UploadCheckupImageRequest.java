package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class UploadCheckupImageRequest implements Packet {
    private String checkupId;
    private byte[] imageData;
    private String fileName;

    public UploadCheckupImageRequest(String checkupId, byte[] imageData, String fileName) {
        this.checkupId = checkupId;
        this.imageData = imageData;
        this.fileName = fileName;
    }
} 