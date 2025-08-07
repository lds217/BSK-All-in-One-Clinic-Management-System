package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class GetCheckupImageResponse implements Packet {
    private String checkupId;
    private String imageName;
    private byte[] imageData;
    private boolean success;
    private String message;

    public GetCheckupImageResponse(String checkupId, String imageName, byte[] imageData, boolean success, String message) {
        this.checkupId = checkupId;
        this.imageName = imageName;
        this.imageData = imageData;
        this.success = success;
        this.message = message;
    }
}
