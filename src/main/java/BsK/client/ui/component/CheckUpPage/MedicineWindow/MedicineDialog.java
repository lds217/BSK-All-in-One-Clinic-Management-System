package BsK.client.ui.component.CheckUpPage.MedicineWindow;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.GetDoctorGeneralInfo;
import BsK.common.packet.req.GetMedInfoRequest;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.server.network.util.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import java.awt.*;
import java.util.List;

@Slf4j
public class MedicineDialog extends JDialog {
    private JTextField medicineNameField;
    private JTextField dosageField;
    private JTextField frequencyField;
    private JTextField quantityField;
    private JTextField priceField;
    private DefaultTableModel tableModel;
    private String[] medcineColumns = {"Medicine Name", "Medicine Company", "Description"};
    private String[][] medicineData;
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::getMedInfoHandler;

    void getMedInfoHandler(GetMedInfoResponse response) {
        log.info("Received medicine data");
        medicineData = response.getMedInfo();
        tableModel.setDataVector(medicineData, medcineColumns);
    }

    void sendGetMedInfoRequest() {
        log.info("Sending GetMedInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
    }

    public MedicineDialog(Frame parent) {
        super(parent, "Enter Medicine Details", true);

        // Set the size of the dialog
        setSize(800, 300);

        ClientHandler.addResponseListener(GetMedInfoResponse.class, getMedInfoResponseListener);
        sendGetMedInfoRequest();

        setLayout(new BorderLayout());


        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch horizontally only
        gbc.gridx = 0;
        gbc.gridy = 0;

        inputPanel.add(new JLabel("Medicine Name:"), gbc);
        gbc.gridx = 1;
        medicineNameField = new JTextField(15);
        inputPanel.add(medicineNameField, gbc);

        // Listen for changes in the text event
        tableModel = new DefaultTableModel(medicineData, medcineColumns);
        JTable medicineTable = new JTable(tableModel);
        TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(tableModel);
        medicineTable.setRowSorter(rowSorter);
        medicineTable.setPreferredSize(new Dimension(400, 100));
        JScrollPane scrollPane = new JScrollPane(medicineTable);

        medicineNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterRows();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterRows();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterRows();
            }

            private void filterRows() {
                String searchText = medicineNameField.getText().trim();
                if (searchText.isEmpty()) {
                    rowSorter.setRowFilter(null);
                } else {
                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, 0));
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Dosage:"), gbc);
        gbc.gridx = 1;
        dosageField = new JTextField(15);
        inputPanel.add(dosageField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Frequency:"), gbc);
        gbc.gridx = 1;
        frequencyField = new JTextField(15);
        inputPanel.add(frequencyField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        quantityField = new JTextField(15);
        inputPanel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        priceField = new JTextField(15);
        inputPanel.add(priceField, gbc);


        scrollPane.setPreferredSize(new Dimension(400, 200)); // 200px height

        // Set the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, scrollPane);
        splitPane.setResizeWeight(0.45); // Allocate more space to the inputPanel
        // Wrap the splitPane in a JPanel to control height
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(splitPane, BorderLayout.CENTER);
        topPanel.setPreferredSize(new Dimension(600, 250));


        // Create a table to display selected medicines
        String[] columnNames = {"Medicine Name", "Dosage", "Frequency", "Quantity", "Price"};
        DefaultTableModel selectedTableModel = new DefaultTableModel(columnNames, 0);
        JTable selectedTable = new JTable(selectedTableModel);
        JScrollPane tableScrollPane = new JScrollPane(selectedTable);

        // Create a panel for add and remove buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        addButton.addActionListener(e -> {
            int selectedRow = medicineTable.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = medicineTable.convertRowIndexToModel(selectedRow);
                Object[] rowData = new Object[tableModel.getColumnCount()];
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    rowData[i] = tableModel.getValueAt(modelRow, i);
                }
                selectedTableModel.addRow(rowData);
            }
        });

        removeButton.addActionListener(e -> {
            int selectedRow = selectedTable.getSelectedRow();
            if (selectedRow != -1) {
                selectedTableModel.removeRow(selectedRow);
            }
        });

        // Add components to the dialog
        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set dialog properties
        pack();
        setLocationRelativeTo(parent);
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
