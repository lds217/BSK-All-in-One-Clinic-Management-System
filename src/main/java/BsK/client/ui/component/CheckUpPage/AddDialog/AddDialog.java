package BsK.client.ui.component.CheckUpPage.AddDialog;

import javax.swing.*;
import java.awt.*;

public class AddDialog extends JDialog {
    private JTextField patientNameField;
    private JTextField patientYearField;
    private JTextField patientIdField;
    private JComboBox patientGenderField;
    private JComboBox villageComboBox, districtComboBox, provinceComboBox;
    private String[] villagesOptions, districtOptions, provinceOptions;

    public AddDialog(Frame parent) {
        super(parent, "Add Patient", true);

        // Set size of the dialog
        setSize(400, 300);
        setResizable(true);

        // Send request to get the first 20 patients in the database


        // Set layout of the dialog
        // left side is the text fields, right side is the patient list

        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();


        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.weightx = 1.0; // Components stretch horizontally
        gbc.weighty = 0.0; // Allow minor vertical flexibility
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.anchor = GridBagConstraints.WEST; // Align components to the  left
        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Patient name:"), gbc);

        gbc.gridwidth = 2;
        gbc.gridx = 1;
        patientNameField = new JTextField(15);
        inputPanel.add(patientNameField, gbc);

        gbc.gridx = 3;

        // Add add icon
        ImageIcon addIcon = new ImageIcon("src/main/java/BsK/client/ui/assets/icon/add.png");
        ImageIcon addIconScaled = new ImageIcon(addIcon.getImage().getScaledInstance(20, 20, Image.SCALE_DEFAULT));
        JButton addButton = new JButton(addIconScaled);
        addButton.addActionListener(e -> {
            // Send request to add patient
        });

        inputPanel.add(addButton, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        inputPanel.add(new JLabel("Patient year:"), gbc);

        gbc.gridx = 1;
        patientYearField = new JTextField(15);
        inputPanel.add(patientYearField, gbc);

        gbc.gridx = 2;
        inputPanel.add(new JLabel("Patient ID:"), gbc);

        gbc.gridx = 3;
        patientIdField = new JTextField(15);
        inputPanel.add(patientIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Address:"), gbc);

        gbc.gridx = 1;
        villagesOptions = new String[]{"Village 1", "Village 2", "Village 3"};
        villageComboBox = new JComboBox(villagesOptions);
        inputPanel.add(villageComboBox, gbc);

        gbc.gridx = 2;
        districtOptions = new String[]{"District 1", "District 2", "District 3"};
        districtComboBox = new JComboBox(districtOptions);
        inputPanel.add(districtComboBox, gbc);

        gbc.gridx = 3;
        provinceOptions = new String[]{"Province 1", "Province 2", "Province 3"};
        provinceComboBox = new JComboBox(provinceOptions);
        inputPanel.add(provinceComboBox, gbc);






        add(inputPanel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        // add a button to open the dialog
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);

        JButton openDialogButton = new JButton("Open Dialog");
        openDialogButton.addActionListener(e -> {
            AddDialog dialog = new AddDialog(frame);
            dialog.setVisible(true);
        });

        frame.add(openDialogButton, BorderLayout.CENTER);


    }
}
