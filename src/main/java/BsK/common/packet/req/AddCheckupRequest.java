package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class AddCheckupRequest implements Packet {
    int customerId;
    int doctorId;
    int processedById;
}
