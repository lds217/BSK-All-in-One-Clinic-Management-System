package BsK.client.ui.component.CheckUpPage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DoctorItem {
    private String id;
    private String name;

    /**
     * This is the crucial part. JComboBox will use this method to display the
     * item in the dropdown.
     */
    @Override
    public String toString() {
        return name;
    }
} 