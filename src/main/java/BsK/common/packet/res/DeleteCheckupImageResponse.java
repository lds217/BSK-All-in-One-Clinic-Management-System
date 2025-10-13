package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class DeleteCheckupImageResponse implements Packet {
    private boolean success;
    private String message;
    private String fileName;

    public DeleteCheckupImageResponse(boolean success, String message, String fileName) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
    }
}

