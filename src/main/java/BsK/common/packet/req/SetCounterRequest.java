package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SetCounterRequest implements Packet {
    private int counter;
    private int shift; // 0 for morning, 1 for afternoon
}
