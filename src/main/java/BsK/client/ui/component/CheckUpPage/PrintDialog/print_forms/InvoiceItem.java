package BsK.client.ui.component.CheckUpPage.PrintDialog.print_forms;

/**
 * Represents a single item on the invoice, which can be a medicine, a service, or a supplement.
 * This class matches the fields defined in the 'med_pre' dataset in the JRXML.
 */
public class InvoiceItem {
    // --- Common Fields ---
    private String type; // "MED", "SER", "SUP"

    // --- Medicine Fields ---
    private String medName;
    private String dosage;
    private String amount;
    private String medRoute;
    private String medNote;

    // --- Service Fields ---
    private String serName;
    private String serNote;
    private Double serUnitPrice;
    private Integer serAmount;

    // --- Supplement Fields ---
    private String supName;
    private String supDosage;
    private String supAmount;
    private String supRoute;
    private String supNote;

    

    
    // Private constructor to be used by factory methods
    private InvoiceItem() {}

    // --- Factory Methods ---

    public static InvoiceItem createMedicine(String medName, String medNote, String dosage, String route, String amount) {
        InvoiceItem item = new InvoiceItem();
        item.type = "MED";
        item.medName = medName;
        item.dosage = dosage;
        item.medRoute = route;
        item.amount = amount;
        item.medNote = medNote;
        return item;
    }

    public static InvoiceItem createService(String serName, String serNote, Integer serAmount, Double serUnitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.type = "SER";
        item.serName = serName;
        item.serNote = serNote;
        item.serAmount = serAmount;
        item.serUnitPrice = serUnitPrice;
        return item;
    }

    public static InvoiceItem createSupplement(String supName, String supNote, String supDosage, String supRoute,  String supAmount) {
        InvoiceItem item = new InvoiceItem();
        item.type = "SUP";
        item.supName = supName;
        item.supDosage = supDosage;
        item.supRoute = supRoute;
        item.supAmount = supAmount;
        item.supNote = supNote;
        return item;
    }


    // --- Getters ---

    public String getType() { return type; }
    public String getMedName() { return medName; }
    public String getDosage() { return dosage; }
    public String getMedRoute() { return medRoute; }
    public String getAmount() { return amount; }
    public String getMedNote() { return medNote; }
    public String getSerName() { return serName; }
    public String getSerNote() { return serNote; }
    public Double getSerUnitPrice() { return serUnitPrice; }
    public Integer getSerAmount() { return serAmount; }
    public String getSupName() { return supName; }
    public String getSupNote() { return supNote; }
    public String getSupDosage() { return supDosage; }
    public String getSupAmount() { return supAmount; }
    public String getSupRoute() { return supRoute; }
} 