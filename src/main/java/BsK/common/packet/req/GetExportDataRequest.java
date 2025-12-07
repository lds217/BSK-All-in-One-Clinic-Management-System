package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetExportDataRequest implements Packet {
    private Long fromDate;      // Start timestamp
    private Long toDate;        // End timestamp
    private Integer doctorId;   // Optional doctor filter (null = all doctors)
    private boolean includeMedicine;  // Include medicine/prescription data
    private boolean includeService;   // Include service data
}

