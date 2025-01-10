package BsK.client.ui.component.CheckUpPage.MedicineDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.GetMedInfoRequest;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

@Slf4j
public class MedicineDialog extends JDialog {
    private JTextField medicineNameField;
    private JTextField medicineCompanyField;
    private JTextArea medicineDescriptionField;
    private JSpinner quantitySpinner;
    private JTextField quantityLeftField;
    private JComboBox<String> UnitComboBox;
    private JTextField priceField;
    private JTextField totalField;
    private JTextArea noteField;
    private DefaultTableModel tableModel, selectedTableModel;
    private String[] medcineColumns = {"ID", "Medicine Name", "Medicine Company", "Description", "Stock", "Unit", "Price"};
    private String[][] medicineData;
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::getMedInfoHandler;
    private TableColumnModel columnModel;
    private HashMap<String, Boolean> selectedMedicine = new HashMap<>();
    private JTable medicineTable;
    private JTable selectedTable;

    private String[][] medicinePrescription;

    public String[][] getMedicinePrescription() {
        return medicinePrescription;
    }

    void getMedInfoHandler(GetMedInfoResponse response) {
        log.info("Received medicine data");
        medicineData = response.getMedInfo();
        tableModel.setDataVector(medicineData, medcineColumns);

        // resize column width
        columnModel = medicineTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(10);
        columnModel.getColumn(1).setPreferredWidth(100);
        columnModel.getColumn(2).setPreferredWidth(50);
        columnModel.getColumn(3).setPreferredWidth(200);
        columnModel.getColumn(4).setPreferredWidth(20);
        columnModel.getColumn(5).setPreferredWidth(20);
        columnModel.getColumn(6).setPreferredWidth(20);
    }

    void sendGetMedInfoRequest() {
        log.info("Sending GetMedInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
    }

    public MedicineDialog(Frame parent) {
        super(parent, "Enter Medicine Details", true);

        // Set the size of the dialog
        setSize(1100, 500);
        setResizable(true);

        ClientHandler.addResponseListener(GetMedInfoResponse.class, getMedInfoResponseListener);
        sendGetMedInfoRequest();

        setLayout(new BorderLayout());


        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.weightx = 1.0; // Components stretch horizontally
        gbc.weighty = 0.0; // Allow minor vertical flexibility
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.anchor = GridBagConstraints.WEST; // Align components to the  left
        gbc.gridx = 0;
        gbc.gridy = 0;

        inputPanel.add(new JLabel("Medicine Name:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        medicineNameField = new JTextField(15);
        inputPanel.add(medicineNameField, gbc);

        // Listen for changes in the text event
        tableModel = new DefaultTableModel(medicineData, medcineColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setFont(new Font("Serif", Font.BOLD, 20));
        medicineTable.setRowHeight(30);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(medicineTable);


        medicineTable.setPreferredScrollableViewportSize(new Dimension(625, 200));


        scrollPane.setPreferredSize(new Dimension(625, 200));


        medicineTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int selectedRow = medicineTable.getSelectedRow();
                    if (selectedRow != -1) {
                        handleRowSelection(selectedRow);
                    }
                });
            }
        });

        medicineNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleRowSelection(medicineTable.getSelectedRow());
                }
            }
        });


        medicineNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                findAndSelectRow();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                findAndSelectRow();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                findAndSelectRow();
            }

            private void findAndSelectRow() {
                String searchText = medicineNameField.getText().trim();
                if (searchText.isEmpty()) {
                    // Clear selection if search text is empty
                    medicineTable.clearSelection();
                    return;
                }

                boolean found = false;
                for (int row = 0; row < medicineTable.getRowCount(); row++) {
                    // Assuming the search is targeting the name column (column index 1)
                    String cellValue = medicineTable.getValueAt(row, 1).toString();
                    if (cellValue.toLowerCase().contains(searchText.toLowerCase())) {
                        medicineTable.setRowSelectionInterval(row, row);
                        medicineTable.scrollRectToVisible(medicineTable.getCellRect(row, 1, true));
                        found = true;
                        break; // Stop after selecting the first match
                    }
                }

                if (!found) {
                    medicineTable.clearSelection(); // No matches found
                }
            }
        });


        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Company:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        medicineCompanyField = new JTextField(15);
        inputPanel.add(medicineCompanyField, gbc);

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Description:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        medicineDescriptionField = new JTextArea(2, 15);
        inputPanel.add(medicineDescriptionField, gbc);
        gbc.anchor = GridBagConstraints.WEST;


        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Quantity:"), gbc);

        gbc.gridx = 1;
        // Create and set preferred size for the spinner
        quantitySpinner = new JSpinner();
        quantitySpinner.setPreferredSize(new Dimension(100, 25)); // Adjust width and height as needed
        inputPanel.add(quantitySpinner, gbc);


        gbc.gridx = 2;
        inputPanel.add(new JLabel("Left"), gbc);

        gbc.gridx = 3;
        quantityLeftField = new JTextField(5);
        quantityLeftField.setEditable(false);
        inputPanel.add(quantityLeftField, gbc);


        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Unit:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        String[] units = {"Viên", "Vỉ", "Hộp", "Ống"};
        UnitComboBox = new JComboBox<>(units);
        inputPanel.add(UnitComboBox, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Price:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        priceField = new JTextField(15);
        inputPanel.add(priceField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Total:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        totalField = new JTextField(15);
        inputPanel.add(totalField, gbc);

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Note:"), gbc);
        gbc.gridwidth = 3;
        gbc.gridx = 1;
        noteField = new JTextArea(2, 15);
        inputPanel.add(noteField, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        quantitySpinner.addChangeListener(e -> {
            int quantity = (int) quantitySpinner.getValue();
            if (quantity < 0) {
                quantity = 0;
                quantitySpinner.setValue(quantity);
            }
            else if (quantity > Integer.parseInt(quantityLeftField.getText())) {
                JOptionPane.showMessageDialog(this, "Quantity exceeds stock", "Error", JOptionPane.ERROR_MESSAGE);
                quantity = Integer.parseInt(quantityLeftField.getText());
                quantitySpinner.setValue(quantity);
            }
            double price = Double.parseDouble(priceField.getText());
            totalField.setText(String.valueOf(quantity * price));
        });



        // Set the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, scrollPane);
        splitPane.setResizeWeight(0.5); // Allocate more space to the inputPanel
        // Wrap the splitPane in a JPanel to control height
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(splitPane, BorderLayout.CENTER);


        // Create a table to display selected medicines
        String[] columnNames = {"ID", "Medicine Name", "Company", "Description", "Quantity", "Unit", "Single Price", "Total Price", "Note"};
        selectedTableModel = new DefaultTableModel(new String[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        selectedTable = new JTable(selectedTableModel);
        JScrollPane tableScrollPane = new JScrollPane(selectedTable);

        // Create a panel for add and remove buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        JButton submitButton = new JButton("Submit");
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(submitButton);

        addButton.addActionListener(e -> {
            int selectedRow = medicineTable.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = medicineTable.convertRowIndexToModel(selectedRow);
                if(selectedMedicine.containsKey(tableModel.getValueAt(modelRow, 0).toString())) {
                    JOptionPane.showMessageDialog(this, "Medicine already added", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selectedMedicine.put((String) tableModel.getValueAt(modelRow, 0), true);
                Object[] rowData = new Object[9];
                int columns = tableModel.getColumnCount();
                for (int i = 0; i < columns ; i++) {
                    rowData[i] = tableModel.getValueAt(modelRow, i);
                }

                rowData[4] = quantitySpinner.getValue();
                rowData[7] = totalField.getText();
                rowData[8] = noteField.getText();
                selectedTableModel.addRow(rowData);
                log.info("Added medicine with id: {}", tableModel.getValueAt(modelRow, 0));
            }
        });

        removeButton.addActionListener(e -> {
            int selectedRow = selectedTable.getSelectedRow();
            if (selectedRow != -1) {
                String id = selectedTableModel.getValueAt(selectedRow, 0).toString();
                selectedMedicine.remove(id);
                log.info("Removed medicine with id: {}", id);
                selectedTableModel.removeRow(selectedRow);
            }
        });

        submitButton.addActionListener(e -> {
            int rows = selectedTableModel.getRowCount();
            medicinePrescription = new String[rows][9];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < 9; j++) {
                    medicinePrescription[i][j] = selectedTableModel.getValueAt(i, j).toString();
                }
            }
            dispose();
        });

        // Add components to the dialog
        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(parent);
    }

    private void handleRowSelection(int selectedRow) {
        String id = (String) tableModel.getValueAt(selectedRow, 0);
        String name = (String) tableModel.getValueAt(selectedRow, 1);
        String company = (String) tableModel.getValueAt(selectedRow, 2);
        String description = (String) tableModel.getValueAt(selectedRow, 3);
        String stock = (String) tableModel.getValueAt(selectedRow, 4);
        String unit = (String) tableModel.getValueAt(selectedRow, 5);
        String price = (String) tableModel.getValueAt(selectedRow, 6);

        medicineNameField.setText(name);
        medicineCompanyField.setText(company);
        medicineDescriptionField.setText(description);
        quantitySpinner.setValue(0);
        quantityLeftField.setText(stock);
        UnitComboBox.setSelectedItem(unit);
        priceField.setText(price);
        totalField.setText("0");
        noteField.setText("");
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setVisible(true);

        JButton openDialogButton = new JButton("Open Medicine Dialog");
        openDialogButton.addActionListener(e -> {
            MedicineDialog dialog = new MedicineDialog(frame);
            dialog.setVisible(true);
        });

        frame.add(openDialogButton, BorderLayout.CENTER);
    }
}
