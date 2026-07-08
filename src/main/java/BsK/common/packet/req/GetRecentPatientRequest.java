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
    private String searchId;
    private int page = 1;
    private int pageSize = 20;
    
    // Constructor for backward compatibility (gets first page with default page size)
    public GetRecentPatientRequest(int page, int pageSize) {
        this(null, null, null, page, pageSize);
    }
    
    // Constructor for search without pagination
    public GetRecentPatientRequest(String searchName, String searchPhone) {
        this(searchName, searchPhone, null, 1, 20);
    }
    
    // Constructor for name/phone search with pagination (no ID search)
    public GetRecentPatientRequest(String searchName, String searchPhone, int page, int pageSize) {
        this(searchName, searchPhone, null, page, pageSize);
    }
}
