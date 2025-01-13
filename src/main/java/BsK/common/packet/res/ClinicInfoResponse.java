package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Data
@Slf4j
public class ClinicInfoResponse implements Packet {
    private String clinicName;
    private String clinicAddress;
    private String clinicPhone;
}
