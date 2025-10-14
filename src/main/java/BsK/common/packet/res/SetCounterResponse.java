package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SetCounterResponse implements Packet{
    private int counter;
    private int shift; // 0 for morning, 1 for afternoon
}
