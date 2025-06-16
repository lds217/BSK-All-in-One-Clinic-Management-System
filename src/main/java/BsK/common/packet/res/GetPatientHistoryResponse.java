package BsK.common.packet.res;

import BsK.common.entity.PatientHistory;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Slf4j
public class GetPatientHistoryResponse implements Packet {
    private String[][] history;
    private transient List<PatientHistory> patientHistoryList;
    
    public GetPatientHistoryResponse(String[][] history) {
        this.history = history;
        this.patientHistoryList = convertToPatientHistoryList(history);
    }
    
    /**
     * Converts the raw string array data to a list of PatientHistory objects
     * @param history Raw patient history data as string arrays
     * @return List of PatientHistory objects
     */
    private List<PatientHistory> convertToPatientHistoryList(String[][] history) {
        List<PatientHistory> result = new ArrayList<>();
        if (history != null) {
            for (String[] data : history) {
                if (data.length >= 6) {
                    result.add(new PatientHistory(data));
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the patient history list, converting from raw data if needed
     * @return List of PatientHistory objects
     */
    public List<PatientHistory> getPatientHistoryList() {
        if (patientHistoryList == null && history != null) {
            patientHistoryList = convertToPatientHistoryList(history);
        }
        return patientHistoryList;
    }
} 