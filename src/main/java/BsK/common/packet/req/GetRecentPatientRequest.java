package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetRecentPatientRequest implements Packet {
    private String searchName;
    private String searchPhone;
    private int page = 1;
    private int pageSize = 20;
    
    // Constructor for backward compatibility (gets first page with default page size)
    public GetRecentPatientRequest(int page, int pageSize) {
        this(null, null, page, pageSize);
    }
    
    // Constructor for search without pagination
    public GetRecentPatientRequest(String searchName, String searchPhone) {
        this(searchName, searchPhone, 1, 20);
    }
}
