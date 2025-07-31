package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddServiceRequest implements Packet{
    private String name;
    private Double price;
    private Boolean deleted;
}
