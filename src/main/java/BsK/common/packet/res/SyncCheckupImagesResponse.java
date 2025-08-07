package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.Data;

import java.util.List;

@Data
public class SyncCheckupImagesResponse implements Packet {
    private String checkupId;
    private List<String> imageNames;
    private boolean success;
    private String message;

    public SyncCheckupImagesResponse(String checkupId, List<String> imageNames, boolean success, String message) {
        this.checkupId = checkupId;
        this.imageNames = imageNames;
        this.success = success;
        this.message = message;
    }
} 