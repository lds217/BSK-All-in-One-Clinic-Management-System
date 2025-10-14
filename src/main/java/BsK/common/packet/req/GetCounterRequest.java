package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GetCounterRequest implements Packet {
    private int shift; // 0 for morning, 1 for afternoon
}
