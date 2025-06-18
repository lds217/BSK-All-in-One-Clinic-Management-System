package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class UploadCheckupImageResponse implements Packet {
    private boolean success;
    private String message;
    private String fileName;

    public UploadCheckupImageResponse(boolean success, String message, String fileName) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
    }
} 