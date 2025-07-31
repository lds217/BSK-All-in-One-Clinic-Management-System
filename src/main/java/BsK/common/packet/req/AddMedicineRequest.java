package BsK.common.packet.req;

import BsK.common.packet.Packet;

public class AddMedicineRequest implements Packet{
    private String name;
    private String company;
    private String description;
    private String unit;
    private double price;
    private String preferedNote;
}
