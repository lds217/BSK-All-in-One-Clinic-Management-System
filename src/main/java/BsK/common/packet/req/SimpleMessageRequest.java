package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class SimpleMessageRequest implements Packet {
    private String senderName;
    private String message;

}
