package BsK.common.packet.res;

import BsK.common.entity.Medicine;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@AllArgsConstructor
public class GetMedInfoResponse implements Packet {
    private String[][] medInfo;
    private transient List<Medicine> medicines;
    
    public GetMedInfoResponse(String[][] medInfo) {
        this.medInfo = medInfo;
        this.medicines = convertToMedicineList(medInfo);
    }
    
    /**
     * Converts the raw string array data to a list of Medicine objects
     * @param medInfo Raw medicine data as string arrays
     * @return List of Medicine objects
     */
    private List<Medicine> convertToMedicineList(String[][] medInfo) {
        List<Medicine> result = new ArrayList<>();
        if (medInfo != null) {
            for (String[] data : medInfo) {
                if (data.length >= 7) {
                    result.add(new Medicine(data));
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the medicine list, converting from raw data if needed
     * @return List of Medicine objects
     */
    public List<Medicine> getMedicines() {
        if (medicines == null && medInfo != null) {
            medicines = convertToMedicineList(medInfo);
        }
        return medicines;
    }
}
