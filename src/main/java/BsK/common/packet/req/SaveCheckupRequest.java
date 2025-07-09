package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
public class SaveCheckupRequest implements Packet {
    String checkupId;
    String customerId;
    String doctorId;
    String checkupDate;

    // Checkup Details
    String suggestions;
    String diagnosis;
    String notes;
    String status;
    String checkupType;
    String conclusion;
    String reCheckupDate;

    // Patient Details
    String customerFirstName;
    String customerLastName;
    String customerDob;
    String customerGender;
    String customerAddress;
    String customerNumber;
    String customerWeight;
    String customerHeight;
    String customerCccdDdcn;

    // Prescriptions
    String[][] medicinePrescription;
    String[][] servicePrescription;
} 