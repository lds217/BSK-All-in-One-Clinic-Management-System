package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class UploadCheckupPdfResponse implements Packet {
    private boolean success;
    private String message;
    private String fileName;
    private String pdfType;

    public UploadCheckupPdfResponse(boolean success, String message, String fileName, String pdfType) {
        this.success = success;
        this.message = message;
        this.fileName = fileName;
        this.pdfType = pdfType;
    }
} 