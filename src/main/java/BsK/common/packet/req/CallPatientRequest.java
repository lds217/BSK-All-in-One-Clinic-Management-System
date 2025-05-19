package BsK.common.packet.req;

import BsK.common.entity.Status;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallPatientRequest implements Packet {
    int roomId;
    int patientId;
    Status status;
}
