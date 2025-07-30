package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetCheckupDataResponse implements Packet {
    private String[][] checkupData;
    private int totalRecords;
    private int currentPage;
    private int totalPages;
    private int pageSize;
}