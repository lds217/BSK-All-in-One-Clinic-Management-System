package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class for Medicine
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Medicine {
    private String id;
    private String name;
    private String company;
    private String description;
    private String quantity;
    private String unit;
    private String sellingPrice;
    private String preferenceNote;
    private String supplement; // 0: no, 1: yes

    /**
     * Constructor to create Medicine from array data
     * @param data Array of medicine data from backend
     */
    public Medicine(String[] data) {
        if (data.length < 9) {
            throw new IllegalArgumentException("Medicine data array must contain at least 9 elements");
        }
        this.id = data[0];
        this.name = data[1];
        this.company = data[2];
        this.description = data[3];
        this.quantity = data[4];
        this.unit = data[5];
        this.sellingPrice = data[6];
        this.preferenceNote = data[7];
        this.supplement = data[8];
    }

    /**
     * Convert the Medicine entity to a String array
     * @return String array representation of Medicine
     */
    public String[] toStringArray() {
        return new String[]{id, name, company, description, quantity, unit, sellingPrice, preferenceNote, supplement};
    }
} 