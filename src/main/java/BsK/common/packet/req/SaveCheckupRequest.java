package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
public class SaveCheckupRequest implements Packet {
    Integer checkupId;
    Integer customerId;
    Integer doctorId;
    Integer doctorUltrasoundId;
    Long checkupDate;

    // Checkup Details
    String suggestions;
    String diagnosis;
    String notes;
    String status;
    String checkupType;
    String conclusion;
    Long reCheckupDate;
    Boolean needsRecheckup; // Flag to indicate if re-checkup is needed

    // Patient Details
    String customerFirstName;
    String customerLastName;
    Long customerDob;
    String customerGender;
    String customerAddress;
    String customerNumber;
    Double customerWeight;
    Double customerHeight;
    String customerCccdDdcn;
    Integer heartBeat;
    String bloodPressure; // format as string 0/0

    // Prescriptions
    String[][] medicinePrescription;
    String[][] servicePrescription;
} 