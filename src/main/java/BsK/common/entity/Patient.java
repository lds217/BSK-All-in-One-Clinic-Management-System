package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO for patient
@Data
@AllArgsConstructor
public class Patient {
    private String checkupId;
    private String checkupDate;
    private String customerLastName;
    private String customerFirstName;
    private String doctorName;
    private String suggestion;
    private String diagnosis;
    private String notes;
    private String status;
    private String customerId;
    private String customerNumber;
    private String customerAddress;
    private String customerWeight;
    private String customerHeight;
    private String customerGender;
    private String customerDob;
    private String checkupType;
    private String conclusion;

    public Patient(String[][] array) {
        this.checkupId = array[0][0];
        this.checkupDate = array[0][1];
        this.customerLastName = array[0][2];
        this.customerFirstName = array[0][3];
        this.doctorName = array[0][4];
        this.suggestion = array[0][5];
        this.diagnosis = array[0][6];
        this.notes = array[0][7];
        this.status = array[0][8];
        this.customerId = array[0][9];
        this.customerNumber = array[0][10];
        this.customerAddress = array[0][11];
        this.customerWeight = array[0][12];
        this.customerHeight = array[0][13];
        this.customerGender = array[0][14];
        this.customerDob = array[0][15];
        this.checkupType = array[0][16];
        this.conclusion = array[0][17];
    }

    // New constructor for a single patient data row
    public Patient(String[] data) {
        if (data.length < 18) {
            throw new IllegalArgumentException("Patient data array must contain at least 18 elements to include a conclusion.");
        }
        this.checkupId = data[0];
        this.checkupDate = data[1];
        this.customerLastName = data[2];
        this.customerFirstName = data[3];
        this.doctorName = data[4];
        this.suggestion = data[5];
        this.diagnosis = data[6];
        this.notes = data[7];
        this.status = data[8];
        this.customerId = data[9];
        this.customerNumber = data[10];
        this.customerAddress = data[11];
        this.customerWeight = data[12];
        this.customerHeight = data[13];
        this.customerGender = data[14];
        this.customerDob = data[15];
        this.checkupType = data[16];
        this.conclusion = data[17];
    }
}
