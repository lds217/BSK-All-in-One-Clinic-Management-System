package BsK.common.packet.res;

import BsK.common.entity.Service;
import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@AllArgsConstructor
public class GetSerInfoResponse implements Packet {
    private String[][] serInfo;
    private transient List<Service> services;
    
    public GetSerInfoResponse(String[][] serInfo) {
        this.serInfo = serInfo;
        this.services = convertToServiceList(serInfo);
    }
    
    /**
     * Converts the raw string array data to a list of Service objects
     * @param serInfo Raw service data as string arrays
     * @return List of Service objects
     */
    private List<Service> convertToServiceList(String[][] serInfo) {
        List<Service> result = new ArrayList<>();
        if (serInfo != null) {
            for (String[] data : serInfo) {
                if (data.length >= 3) {
                    result.add(new Service(data));
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the service list, converting from raw data if needed
     * @return List of Service objects
     */
    public List<Service> getServices() {
        if (services == null && serInfo != null) {
            services = convertToServiceList(serInfo);
        }
        return services;
    }
}
