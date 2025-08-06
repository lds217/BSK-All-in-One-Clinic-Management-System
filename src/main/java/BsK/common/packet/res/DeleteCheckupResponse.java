package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class DeleteCheckupResponse implements Packet{
    private boolean success;
    private String message;

}
