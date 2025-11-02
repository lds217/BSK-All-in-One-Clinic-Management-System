package BsK.common.packet.res;

import BsK.common.entity.Doctor;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class GetDoctorInfoResponse implements Packet {
    private String[][] doctorInfo;
    private transient List<Doctor> doctors;
    
    public GetDoctorInfoResponse(String[][] doctorInfo) {
        this.doctorInfo = doctorInfo;
        this.doctors = convertToDoctorList(doctorInfo);
    }
    
    /**
     * Converts the raw string array data to a list of Doctor objects
     * @param doctorInfo Raw doctor data as string arrays
     * @return List of Doctor objects
     */
    private List<Doctor> convertToDoctorList(String[][] doctorInfo) {
        List<Doctor> result = new ArrayList<>();
        if (doctorInfo != null) {
            for (String[] data : doctorInfo) {
                if (data.length >= 4) {
                    result.add(new Doctor(data));
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the doctor list, converting from raw data if needed
     * @return List of Doctor objects
     */
    public List<Doctor> getDoctors() {
        if (doctors == null && doctorInfo != null) {
            doctors = convertToDoctorList(doctorInfo);
        }
        return doctors;
    }
}

