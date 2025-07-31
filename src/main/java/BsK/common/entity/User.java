package BsK.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
    private String id;
    private String userName;
    private String lastName;
    private String firstName;
    private String password;
    private String role;
    private String deleted; // "0" or "1"

    /**
     * Constructor to create a User from a String array from the server.
     */
    public User(String[] data) {
        if (data.length < 7) {
            throw new IllegalArgumentException("User data array must contain at least 7 elements");
        }
        this.id = data[0];
        this.userName = data[1];
        this.lastName = data[2];
        this.firstName = data[3];
        this.password = data[4];
        this.role = data[5];
        this.deleted = data[6];
    }
}