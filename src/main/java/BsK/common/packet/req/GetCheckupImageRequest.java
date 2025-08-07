package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class GetCheckupImageRequest implements Packet {
    private String checkupId;
    private String imageName;

    public GetCheckupImageRequest(String checkupId, String imageName) {
        this.checkupId = checkupId;
        this.imageName = imageName;
    }
}
