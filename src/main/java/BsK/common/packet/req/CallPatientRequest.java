package BsK.common.packet.req;

import BsK.common.entity.Status;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallPatientRequest implements Packet {
    private int roomId;
    private int patientId;
    private String queueNumber;
    private Status status;
}
