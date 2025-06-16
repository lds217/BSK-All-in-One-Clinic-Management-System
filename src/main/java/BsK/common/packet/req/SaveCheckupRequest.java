package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode//(callSuper = true) // Cannot call super on an interface implementation
@Data
@AllArgsConstructor
public class SaveCheckupRequest implements Packet {
    private String checkupId;
    private String checkupDate; // Consider sending as long (timestamp) if consistency is needed
    private String customerId;
    private String customerLastName;
    private String customerFirstName;
    private String customerDob; // Consider sending as long (timestamp)
    private String customerGender;
    private String customerAddress; // Full address string: "Street, Ward, District, Province"
    private String customerNumber; // Phone number
    private String customerWeight;
    private String customerHeight;
    private String doctorId; // Or doctorId if preferred for backend processing
    private String suggestions;
    private String diagnosis;
    private String notes;
    private String status;
    private String checkupType;
    private String conclusion;  
    private String[][] medicinePrescription; // Structure: [med_id, name, quantity, unit, dose_morning, dose_noon, dose_evening, unit_price, total_price, notes]
    private String[][] servicePrescription;  // Structure: [ser_id, name, quantity, unit_price, total_price, notes]
} 