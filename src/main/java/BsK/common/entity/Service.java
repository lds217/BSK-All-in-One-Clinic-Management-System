package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class for Service
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service {
    private String id;
    private String name;
    private String cost;

    /**
     * Constructor to create Service from array data
     * @param data Array of service data from backend
     */
    public Service(String[] data) {
        if (data.length < 3) {
            throw new IllegalArgumentException("Service data array must contain at least 3 elements");
        }
        this.id = data[0];
        this.name = data[1];
        this.cost = data[2];
    }

    /**
     * Convert the Service entity to a String array
     * @return String array representation of Service
     */
    public String[] toStringArray() {
        return new String[]{id, name, cost};
    }
} 