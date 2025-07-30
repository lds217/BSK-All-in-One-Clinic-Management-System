package BsK.common.packet.res;

import BsK.common.packet.Packet;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmergencyResponse implements Packet {
    String senderName;
}
