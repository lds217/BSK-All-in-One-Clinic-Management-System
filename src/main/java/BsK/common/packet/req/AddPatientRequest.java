package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Data
public class AddPatientRequest implements Packet {
    private String patientFirstName;
    private String patientLastName;
    private long patientDob;
    private String patientPhone;
    private String patientAddress;
    private String patientGender;
    private String patientCccdDdcn;
}
