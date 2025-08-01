package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EditMedicineRequest implements Packet {
    private String id;
    private String name;
    private String company;
    private String description;
    private String unit;
    private Double price;
    private String preferredNote;
    private Boolean supplement;
    private Boolean deleted;
}
