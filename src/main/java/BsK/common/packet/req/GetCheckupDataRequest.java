package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetCheckupDataRequest implements Packet {
    private String searchTerm;
    private Long fromDate; // Use Long for timestamp
    private Long toDate;   // Use Long for timestamp
    private Integer doctorId;
    private int page = 1;
    private int pageSize = 20;
}