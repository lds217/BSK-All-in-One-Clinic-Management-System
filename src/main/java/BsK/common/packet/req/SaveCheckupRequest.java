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
    Long checkupDate;

    // Checkup Details
    String suggestions;
    String diagnosis;
    String notes;
    String status;
    String checkupType;
    String conclusion;
    Long reCheckupDate;

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

    // Prescriptions
    String[][] medicinePrescription;
    String[][] servicePrescription;
} 