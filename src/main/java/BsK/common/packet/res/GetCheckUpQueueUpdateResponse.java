package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class GetCheckUpQueueUpdateResponse implements Packet {
    private String[][] queue;
}