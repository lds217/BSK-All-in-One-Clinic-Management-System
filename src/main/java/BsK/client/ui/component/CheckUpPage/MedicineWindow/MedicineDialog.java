package BsK.client.ui.component.CheckUpPage.MedicineWindow;//package BsK.client.ui.component.CheckUpPage.MedicineWindow;
//
//import javax.swing.*;
//import javax.swing.table.DefaultTableModel;
//import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
//import java.awt.*;
//import java.util.List;
//
//public class MedicineDialog extends JDialog {
//    private JComboBox<String> medicineNameComboBox;
//    private JTextField dosageField;
//    private JTextField frequencyField;
//    private JTextField quantityField;
//    private JTextField priceField;
//    private DefaultTableModel tableModel;
//
//    // Example list of medicine names for autocomplete
//    private List<String> medicineList = List.of(
//            "Paracetamol|20|4", "Ibuprofen|20|4", "Aspirin|400|2", "Amoxicillin|32|4",
//            "Ciprofloxacin|323|4", "Doxycycline|43|43", "Metformin|332|23", "Atorvastatin|232|32"
//    );
//
//    public MedicineDialog(Frame parent) {
//        super(parent, "Enter Medicine Details", true);
//        setLayout(new BorderLayout());
//
//        // Create a panel to hold the input fields
//        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
//        inputPanel.add(new JLabel("Medicine Name:"));
//        medicineNameComboBox = new JComboBox<>(medicineList.toArray(new String[0]));
//        medicineNameComboBox.setEditable(true);
//        AutoCompleteDecorator.decorate(medicineNameComboBox);
//        inputPanel.add(medicineNameComboBox);
//
//        inputPanel.add(new JLabel("Dosage:"));
//        dosageField = new JTextField();
//        inputPanel.add(dosageField);
//        inputPanel.add(new JLabel("Frequency:"));
//        frequencyField = new JTextField();
//        inputPanel.add(frequencyField);
//        inputPanel.add(new JLabel("Quantity:"));
//        quantityField = new JTextField();
//        inputPanel.add(quantityField);
//        inputPanel.add(new JLabel("Price:"));
//        priceField = new JTextField();
//        inputPanel.add(priceField);
//
//        // Create a table to display selected medicines
//        String[] columnNames = {"Medicine Name", "Dosage", "Frequency", "Quantity", "Price"};
//        tableModel = new DefaultTableModel(columnNames, 0);
//        JTable table = new JTable(tableModel);
//        JScrollPane tableScrollPane = new JScrollPane(table);
//
//        // Create a panel for add and remove buttons
//        JPanel buttonPanel = new JPanel();
//        JButton addButton = new JButton("Add");
//        JButton removeButton = new JButton("Remove");
//        buttonPanel.add(addButton);
//        buttonPanel.add(removeButton);
//
//        // Add action listeners for buttons
//        addButton.addActionListener(e -> {
//            String medicineName = (String) medicineNameComboBox.getSelectedItem();
//            String dosage = dosageField.getText();
//            String frequency = frequencyField.getText();
//            String quantity = quantityField.getText();
//            String price = priceField.getText();
//            tableModel.addRow(new Object[]{medicineName, dosage, frequency, quantity, price});
//        });
//
//        removeButton.addActionListener(e -> {
//            int selectedRow = table.getSelectedRow();
//            if (selectedRow != -1) {
//                tableModel.removeRow(selectedRow);
//            }
//        });
//
//        // Add components to the dialog
//        add(inputPanel, BorderLayout.NORTH);
//        add(tableScrollPane, BorderLayout.CENTER);
//        add(buttonPanel, BorderLayout.SOUTH);
//
//        // Set dialog properties
//        pack();
//        setLocationRelativeTo(parent);
//    }
//
//    public static void main(String[] args) {
//        JFrame frame = new JFrame();
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(400, 300);
//        frame.setVisible(true);
//
//        JButton openDialogButton = new JButton("Open Medicine Dialog");
//        openDialogButton.addActionListener(e -> {
//            MedicineDialog dialog = new MedicineDialog(frame);
//            dialog.setVisible(true);
//        });
//
//        frame.add(openDialogButton, BorderLayout.CENTER);
//    }
//}

