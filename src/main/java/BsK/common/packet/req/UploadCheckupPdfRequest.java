package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.Data;

@Data
public class UploadCheckupPdfRequest implements Packet {
    private String checkupId;
    private byte[] pdfData;
    private String fileName;
    private String pdfType; // "ultrasound_result" or "medserinvoice" (for future use)

    public UploadCheckupPdfRequest(String checkupId, byte[] pdfData, String fileName, String pdfType) {
        this.checkupId = checkupId;
        this.pdfData = pdfData;
        this.fileName = fileName;
        this.pdfType = pdfType;
    }
} 