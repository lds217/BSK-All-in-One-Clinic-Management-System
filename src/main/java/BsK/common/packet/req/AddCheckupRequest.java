package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class AddCheckupRequest implements Packet {
    private int customerId;
    private int doctorId;
    private int processedById;
    private String checkupType;
    private String status;
}
