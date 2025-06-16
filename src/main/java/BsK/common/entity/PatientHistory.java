package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class for Patient History
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientHistory {
    private String checkupDate;
    private String checkupId;
    private String suggestion;
    private String diagnosis;
    private String prescriptionId;
    private String notes;

    /**
     * Constructor to create PatientHistory from array data
     * @param data Array of patient history data from backend
     */
    public PatientHistory(String[] data) {
        if (data.length < 6) {
            throw new IllegalArgumentException("PatientHistory data array must contain at least 6 elements");
        }
        this.checkupDate = data[0];
        this.checkupId = data[1];
        this.suggestion = data[2];
        this.diagnosis = data[3];
        this.prescriptionId = data[4];
        this.notes = data[5];
    }

    /**
     * Convert the PatientHistory entity to a String array
     * @return String array representation of PatientHistory
     */
    public String[] toStringArray() {
        return new String[]{checkupDate, checkupId, suggestion, diagnosis, prescriptionId, notes};
    }
} 