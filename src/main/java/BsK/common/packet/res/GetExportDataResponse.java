package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetExportDataResponse implements Packet {
    private boolean success;
    private String message;
    
    // Patient/Checkup data: each String[] represents one checkup record
    private String[][] patientData;
    
    // Medicine data: Map<CheckupId, List<MedicineItem>>
    // Each MedicineItem is a String[] with: [checkupId, medicineName, quantity, unit, unitPrice, totalPrice, dosage]
    private Map<String, List<String[]>> medicineData;
    
    // Service data: Map<CheckupId, List<ServiceItem>>
    // Each ServiceItem is a String[] with: [checkupId, serviceName, quantity, unitPrice, totalPrice]
    private Map<String, List<String[]>> serviceData;
}

