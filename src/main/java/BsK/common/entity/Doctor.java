package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class for Doctor
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Doctor {
    private String id;
    private String lastName;
    private String firstName;
    private String deleted; // 0: active, 1: deleted

    /**
     * Constructor to create Doctor from array data
     * @param data Array of doctor data from backend [id, lastName, firstName, deleted]
     */
    public Doctor(String[] data) {
        if (data.length < 4) {
            throw new IllegalArgumentException("Doctor data array must contain at least 4 elements");
        }
        this.id = data[0];
        this.lastName = data[1];
        this.firstName = data[2];
        this.deleted = data[3];
    }

    /**
     * Get the full name of the doctor
     * @return Full name (lastName + firstName)
     */
    public String getFullName() {
        return (lastName + " " + firstName).trim();
    }

    /**
     * Convert the Doctor entity to a String array
     * @return String array representation of Doctor
     */
    public String[] toStringArray() {
        return new String[]{id, lastName, firstName, deleted};
    }
}

