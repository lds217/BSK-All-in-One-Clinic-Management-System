package BsK.client.ui.component.CheckUpPage.ServiceDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.GetSerInfoRequest;
import BsK.common.packet.res.GetSerInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
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
public class ServiceDialog extends JDialog {
    private JTextField serviceNameField;
    private JSpinner quantitySpinner;
    private JTextField priceField;
    private JTextField totalField;
    private JTextArea noteField;
    private DefaultTableModel tableModel, selectedTableModel;
    private String[] serviceColumns = {"ID", "Service Name", "Price"};
    private String[][] serviceData;
    private final ResponseListener<GetSerInfoResponse> getSerInfoResponseListener = this::getSerInfoHandler;
    private TableColumnModel columnModel;
    private HashMap<String, Boolean> selectedService = new HashMap<>();
    private JTable serviceTable;
    private JTable selectedTable;

    private String[][] servicePrescription;

    public String[][] getServicePrescription() {
        return servicePrescription;
    }

    void getSerInfoHandler(GetSerInfoResponse response) {
        log.info("Received service data");
        serviceData = response.getSerInfo();
        tableModel.setDataVector(serviceData, serviceColumns);

        // resize column width
        columnModel = serviceTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(10);
        columnModel.getColumn(1).setPreferredWidth(200);
        columnModel.getColumn(2).setPreferredWidth(50);
    }

    void sendGetSerInfoRequest() {
        log.info("Sending GetSerInfoRequest");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetSerInfoRequest());
    }

    public ServiceDialog(Frame parent) {
        super(parent, "Enter Service Details", true);

        // Set the size of the dialog
        setSize(1100, 500);
        setResizable(true);

        ClientHandler.addResponseListener(GetSerInfoResponse.class, getSerInfoResponseListener);
        sendGetSerInfoRequest();

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

        inputPanel.add(new JLabel("Service Name:"), gbc);
        gbc.gridx = 1;
        serviceNameField = new JTextField(15);
        inputPanel.add(serviceNameField, gbc);

        // Listen for changes in the text event
        tableModel = new DefaultTableModel(serviceData, serviceColumns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serviceTable = new JTable(tableModel);
        serviceTable.setFont(new Font("Serif", Font.BOLD, 20));
        serviceTable.setRowHeight(30);
        serviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(serviceTable);


        serviceTable.setPreferredScrollableViewportSize(new Dimension(625, 200));


        scrollPane.setPreferredSize(new Dimension(625, 200));


        serviceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    int selectedRow = serviceTable.getSelectedRow();
                    if (selectedRow != -1) {
                        handleRowSelection(selectedRow);
                    }
                });
            }
        });

        serviceNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleRowSelection(serviceTable.getSelectedRow());
                }
            }
        });


        serviceNameField.getDocument().addDocumentListener(new DocumentListener() {
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
                String searchText = serviceNameField.getText().trim();
                if (searchText.isEmpty()) {
                    // Clear selection if search text is empty
                    serviceTable.clearSelection();
                    return;
                }

                boolean found = false;
                for (int row = 0; row < serviceTable.getRowCount(); row++) {
                    // Assuming the search is targeting the name column (column index 1)
                    String cellValue = serviceTable.getValueAt(row, 1).toString();
                    if (TextUtils.removeAccents(cellValue.toLowerCase()).contains(TextUtils.removeAccents(searchText
                            .toLowerCase()))) {
                        serviceTable.setRowSelectionInterval(row, row);
                        serviceTable.scrollRectToVisible(serviceTable.getCellRect(row, 1, true));
                        found = true;
                        break; // Stop after selecting the first match
                    }
                }

                if (!found) {
                    serviceTable.clearSelection(); // No matches found
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Quantity:"), gbc);

        gbc.gridx = 1;
        // Create and set preferred size for the spinner
        quantitySpinner = new JSpinner();
        quantitySpinner.setPreferredSize(new Dimension(100, 25)); // Adjust width and height as needed
        inputPanel.add(quantitySpinner, gbc);


        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        priceField = new JTextField(15);
        inputPanel.add(priceField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Total:"), gbc);
        gbc.gridx = 1;
        totalField = new JTextField(15);
        inputPanel.add(totalField, gbc);

        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy++;
        inputPanel.add(new JLabel("Note:"), gbc);
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

            double price = Double.parseDouble(priceField.getText());
            totalField.setText(String.valueOf(quantity * price));
        });



        // Set the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, scrollPane);
        splitPane.setResizeWeight(0.5); // Allocate more space to the inputPanel
        // Wrap the splitPane in a JPanel to control height
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(splitPane, BorderLayout.CENTER);


        // Create a table to display selected services
        String[] columnNames = {"ID", "Service Name", "Quantity", "Single Price", "Total Price", "Note"};
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
            int selectedRow = serviceTable.getSelectedRow();
            if (selectedRow != -1) {
                int modelRow = serviceTable.convertRowIndexToModel(selectedRow);
                if(selectedService.containsKey(tableModel.getValueAt(modelRow, 0).toString())) {
                    JOptionPane.showMessageDialog(this, "Service already added", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selectedService.put((String) tableModel.getValueAt(modelRow, 0), true);
                Object[] rowData = new Object[6];

                rowData[0] = tableModel.getValueAt(modelRow, 0);
                rowData[1] = tableModel.getValueAt(modelRow, 1);
                rowData[2] = quantitySpinner.getValue();
                rowData[3] = priceField.getText();
                rowData[4] = totalField.getText();
                rowData[5] = noteField.getText();
                selectedTableModel.addRow(rowData);
                log.info("Added service with id: {}", tableModel.getValueAt(modelRow, 0));
            }
        });

        removeButton.addActionListener(e -> {
            int selectedRow = selectedTable.getSelectedRow();
            if (selectedRow != -1) {
                String id = selectedTableModel.getValueAt(selectedRow, 0).toString();
                selectedService.remove(id);
                log.info("Removed service with id: {}", id);
                selectedTableModel.removeRow(selectedRow);
            }
        });

        submitButton.addActionListener(e -> {
            int rows = selectedTableModel.getRowCount();
            servicePrescription = new String[rows][6];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < 6; j++) {
                    servicePrescription[i][j] = selectedTableModel.getValueAt(i, j).toString();
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
        String price = (String) tableModel.getValueAt(selectedRow, 2);

        serviceNameField.setText(name);
        quantitySpinner.setValue(0);
        priceField.setText(price);
        totalField.setText("0");
        noteField.setText("");
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setVisible(true);

        JButton openDialogButton = new JButton("Open Service Dialog");
        openDialogButton.addActionListener(e -> {
            ServiceDialog dialog = new ServiceDialog(frame);
            dialog.setVisible(true);
        });

        frame.add(openDialogButton, BorderLayout.CENTER);
    }
}
