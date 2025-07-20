package BsK.client.ui.component.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.jdatepicker.*;

import javax.swing.*;

public class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {

    private String datePattern = "dd/MM/yyyy";
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern, Locale.ENGLISH);

    public DateLabelFormatter() {
        // Enable lenient parsing to be more flexible with user input
        dateFormatter.setLenient(false);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // Clean up the input text
        text = text.trim();
        java.util.Date parsedDate;

        // Handle different input formats that users might type
        if (text.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            // Standard format: dd/mm/yyyy or d/m/yyyy
            parsedDate = (java.util.Date) dateFormatter.parseObject(text);
        } else if (text.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
            // Handle dash format: dd-mm-yyyy
            text = text.replace("-", "/");
            parsedDate = (java.util.Date) dateFormatter.parseObject(text);
        } else if (text.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")) {
            // Handle dot format: dd.mm.yyyy
            text = text.replace(".", "/");
            parsedDate = (java.util.Date) dateFormatter.parseObject(text);
        } else if (text.matches("\\d{8}")) {
            // Handle compact format: ddmmyyyy
            String day = text.substring(0, 2);
            String month = text.substring(2, 4);
            String year = text.substring(4, 8);
            text = day + "/" + month + "/" + year;
            parsedDate = (java.util.Date) dateFormatter.parseObject(text);
        } else {
            // If none of the formats match, try default parsing
            parsedDate = (java.util.Date) dateFormatter.parseObject(text);
        }

        if (parsedDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsedDate);
            return cal;
        }
        return null;
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }

        return "";
    }
}