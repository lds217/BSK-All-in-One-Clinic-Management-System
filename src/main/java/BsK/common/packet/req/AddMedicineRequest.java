package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddMedicineRequest implements Packet{
    private String name;
    private String company;
    private String description;
    private String unit;
    private Double price;
    private String preferedNote;
    private Boolean supplement;
    private Boolean deleted;
}
