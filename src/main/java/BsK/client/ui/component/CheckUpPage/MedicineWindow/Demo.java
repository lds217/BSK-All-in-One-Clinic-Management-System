package BsK.client.ui.component.CheckUpPage.MedicineWindow;

import javax.swing.*;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import java.awt.*;
public class Demo {

    JFrame frame = new JFrame("");
    AutoCompleteDecorator decorator;


    public Demo() {
       //JTable demo
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        JTable table = new JTable(new String[][] {
            { "1", "2", "3" },
            { "4", "5", "6" },
            { "7", "8", "9" },
            { "10", "11", "12" }
        }, new String[] { "A", "B", "C" });
        //set size of custom colum
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.NORTH);

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        Demo d = new Demo();
    }
}