package BsK.common.packet.res;

import BsK.common.entity.Status;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallPatientResponse implements Packet {
    int patientId;
    int roomId;
    Status status;
}
