package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Data
public class AddPatientRequest implements Packet {
    String patientFirstName;
    String patientLastName;
    long patientDob;
    String patientPhone;
    String patientAddress;
    String patientGender;
    String patientCccdDdcn;
}
