package BsK.client.ui.component.DashboardPage.DataViewerDialog;

import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.client.ui.component.common.RoundedPanel;
import BsK.common.entity.Medicine;
import BsK.common.entity.Service;
import BsK.common.packet.req.GetMedInfoRequest;
import BsK.common.packet.req.GetSerInfoRequest;
import BsK.common.packet.res.GetMedInfoResponse;
import BsK.common.packet.res.GetSerInfoResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.common.util.text.TextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DataViewerDialog extends JDialog {

    // --- UI Components ---
    private JTable medicineTable;
    private DefaultTableModel medicineTableModel;
    private JTextField medicineSearchField;
    private JTable serviceTable;
    private DefaultTableModel serviceTableModel;
    private JTextField serviceSearchField;

    // --- Data Lists ---
    private List<Medicine> allMedicines = new ArrayList<>();
    private List<Service> allServices = new ArrayList<>();

    // --- Networking ---
    private final ResponseListener<GetMedInfoResponse> getMedInfoResponseListener = this::handleGetMedInfoResponse;
    private final ResponseListener<GetSerInfoResponse> getSerInfoResponseListener = this::handleGetSerInfoResponse;


    public DataViewerDialog(Frame parent) {
        super(parent, "Danh sách Thuốc & Dịch vụ", true);
        initUI();
        setupNetworking();
    }

    private void initUI() {
        setSize(1200, 700);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        // Create the two main panels
        JPanel medicinePanel = createListPanel("DANH SÁCH THUỐC", true);
        JPanel servicePanel = createListPanel("DANH SÁCH DỊCH VỤ", false);

        // Create and configure the split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, medicinePanel, servicePanel);
        splitPane.setResizeWeight(0.5); // Equal initial size
        splitPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        splitPane.setOpaque(false);

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createListPanel(String title, boolean isMedicine) {
        RoundedPanel panel = new RoundedPanel(15, Color.WHITE, false);
        panel.setLayout(new BorderLayout(10, 10));

        // Title and Search Bar Panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        
        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        topPanel.add(searchIcon, BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        
        // Table Setup
        DefaultTableModel tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        setupTableStyle(table);

        if (isMedicine) {
            this.medicineTable = table;
            this.medicineTableModel = tableModel;
            this.medicineSearchField = searchField;
            medicineTableModel.setColumnIdentifiers(new String[]{"Mã", "Tên thuốc", "ĐVT", "Đơn giá"});
            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(1).setPreferredWidth(350);
        } else {
            this.serviceTable = table;
            this.serviceTableModel = tableModel;
            this.serviceSearchField = searchField;
            serviceTableModel.setColumnIdentifiers(new String[]{"Mã", "Tên dịch vụ", "Đơn giá"});
            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(1).setPreferredWidth(350);
        }

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (isMedicine) filterMedicineTable(); else filterServiceTable();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (isMedicine) filterMedicineTable(); else filterServiceTable();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                if (isMedicine) filterMedicineTable(); else filterServiceTable();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10),
            title,
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 18),
            new Color(44, 82, 130)
        ));

        return panel;
    }

    private void setupTableStyle(JTable table) {
        table.setFont(new Font("Segoe UI", Font.BOLD, 16));
        table.setRowHeight(35);
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setGridColor(new Color(229, 231, 235));
        table.setIntercellSpacing(new Dimension(0, 1));
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.setBackground(new Color(243, 244, 246));
        header.setForeground(new Color(55, 65, 81));
        header.setReorderingAllowed(false);
    }
    
    // --- Networking and Data Handling ---

    private void setupNetworking() {
        ClientHandler.addResponseListener(GetMedInfoResponse.class, getMedInfoResponseListener);
        ClientHandler.addResponseListener(GetSerInfoResponse.class, getSerInfoResponseListener);

        log.info("Requesting medicine and service lists for viewer.");
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetMedInfoRequest());
        NetworkUtil.sendPacket(ClientHandler.ctx.channel(), new GetSerInfoRequest());
    }
    
    private void handleGetMedInfoResponse(GetMedInfoResponse response) {
        SwingUtilities.invokeLater(() -> {
            allMedicines = response.getMedicines();
            filterMedicineTable();
        });
    }

    private void handleGetSerInfoResponse(GetSerInfoResponse response) {
        SwingUtilities.invokeLater(() -> {
            allServices = response.getServices();
            filterServiceTable();
        });
    }

    private void filterMedicineTable() {
        String filterText = medicineSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());
        medicineTableModel.setRowCount(0);

        if (allMedicines == null) return;

        for (Medicine med : allMedicines) {
            boolean isDeleted = "1".equals(med.getDeleted());
            boolean nameMatches = filterText.isEmpty() || TextUtils.removeAccents(med.getName().toLowerCase()).contains(lowerCaseFilterText);

            if (!isDeleted && nameMatches) {
                medicineTableModel.addRow(new Object[]{
                    med.getId(),
                    med.getName(),
                    med.getUnit(),
                    med.getSellingPrice()
                });
            }
        }
    }

    private void filterServiceTable() {
        String filterText = serviceSearchField.getText().trim();
        String lowerCaseFilterText = TextUtils.removeAccents(filterText.toLowerCase());
        serviceTableModel.setRowCount(0);

        if (allServices == null) return;

        for (Service service : allServices) {
            boolean isDeleted = "1".equals(service.getDeleted());
            boolean nameMatches = filterText.isEmpty() || TextUtils.removeAccents(service.getName().toLowerCase()).contains(lowerCaseFilterText);
            
            if (!isDeleted && nameMatches) {
                serviceTableModel.addRow(new Object[]{
                    service.getId(),
                    service.getName(),
                    service.getCost()
                });
            }
        }
    }

    @Override
    public void dispose() {
        log.info("Closing DataViewerDialog, removing listeners.");
        ClientHandler.deleteListener(GetMedInfoResponse.class, getMedInfoResponseListener);
        ClientHandler.deleteListener(GetSerInfoResponse.class, getSerInfoResponseListener);
        super.dispose();
    }
}