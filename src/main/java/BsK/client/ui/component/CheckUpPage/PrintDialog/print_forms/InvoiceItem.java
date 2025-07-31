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
    private Double unitPrice;
    private String medNote;

    // --- Service Fields ---
    private String serName;
    private String serNote;
    private Double serUnitPrice;
    private Integer serAmount;

    // --- Supplement Fields ---
    private String supName;
    private String supNote;
    private String supDosage;
    private String supAmount;
    private Double supUnitPrice;

    // Private constructor to be used by factory methods
    private InvoiceItem() {}

    // --- Factory Methods ---

    public static InvoiceItem createMedicine(String medName, String dosage, String amount) {
        InvoiceItem item = new InvoiceItem();
        item.type = "MED";
        item.medName = medName;
        item.dosage = dosage;
        item.amount = amount;
        item.unitPrice = 0.0;
        item.medNote = "";
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

    public static InvoiceItem createSupplement(String supName, String supNote, String supDosage, String supAmount, Double supUnitPrice) {
        InvoiceItem item = new InvoiceItem();
        item.type = "SUP";
        item.supName = supName;
        item.supNote = supNote;
        item.supDosage = supDosage;
        item.supAmount = supAmount;
        item.supUnitPrice = supUnitPrice;
        return item;
    }


    // --- Getters ---

    public String getType() { return type; }
    public String getMedName() { return medName; }
    public String getDosage() { return dosage; }
    public String getAmount() { return amount; }
    public Double getUnitPrice() { return unitPrice; }
    public String getMedNote() { return medNote; }
    public String getSerName() { return serName; }
    public String getSerNote() { return serNote; }
    public Double getSerUnitPrice() { return serUnitPrice; }
    public Integer getSerAmount() { return serAmount; }
    public String getSupName() { return supName; }
    public String getSupNote() { return supNote; }
    public String getSupDosage() { return supDosage; }
    public String getSupAmount() { return supAmount; }
    public Double getSupUnitPrice() { return supUnitPrice; }
} 