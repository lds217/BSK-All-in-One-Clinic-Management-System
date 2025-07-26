package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetRecentPatientResponse implements Packet {
    String[][] patientData;
    int totalCount;
    int currentPage;
    int totalPages;
    int pageSize;
    
    // Constructor for backward compatibility
    public GetRecentPatientResponse(String[][] patientData) {
        this.patientData = patientData;
        this.totalCount = patientData != null ? patientData.length : 0;
        this.currentPage = 1;
        this.totalPages = 1;
        this.pageSize = this.totalCount;
    }
}
