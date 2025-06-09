package BsK.client.ui.component.DashboardPage;

import BsK.client.LocalStorage;
import BsK.client.ui.component.MainFrame;
import BsK.client.ui.component.common.NavBar;
import BsK.client.ui.component.common.RoundedButtonUI;
import BsK.client.ui.component.common.RoundedPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;

@Slf4j
public class DashboardPage extends JPanel {
    private final MainFrame mainFrame;

    public DashboardPage(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240)); // A light background for the page

        // --- Navigation Bar ---
        NavBar navBar = new NavBar(mainFrame, "Thống kê");
        add(navBar, BorderLayout.NORTH);

        // --- Main Content Panel (from existing DashboardPage) ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainContent.setOpaque(false); // Make it transparent to see page background

        // Top cards
        JPanel topCards = new JPanel();
        topCards.setOpaque(false); // Make transparent
        topCards.setLayout(new GridLayout(1, 4, 20, 20));
        String[] cardTitles = {"Add Check", "Income", "Expense", "Other Income"};
        // Using slightly more professional/subtle colors
        Color[] cardColors = {new Color(100, 181, 246), new Color(77, 208, 225), new Color(255, 183, 77), new Color(129, 199, 132)};

        for (int i = 0; i < cardTitles.length; i++) {
            RoundedPanel card = new RoundedPanel(15, cardColors[i], true); // Slightly smaller radius
            card.setBackground(cardColors[i]); // Ensure background is set
            card.setLayout(new BorderLayout(10,10));
            card.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15)); // Adjusted padding

            JLabel cardTitleLabel = new JLabel(cardTitles[i]);
            cardTitleLabel.setForeground(Color.WHITE);
            cardTitleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Larger title
            cardTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(cardTitleLabel, BorderLayout.NORTH);

            JLabel cardValueLabel = new JLabel("N/A"); // Placeholder value
            cardValueLabel.setForeground(Color.WHITE);
            cardValueLabel.setFont(new Font("Arial", Font.PLAIN, 24)); // Larger value text
            cardValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(cardValueLabel, BorderLayout.CENTER);

            topCards.add(card);
        }
        mainContent.add(topCards, BorderLayout.NORTH);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        wrapperPanel.setOpaque(false);

        RoundedPanel dataTablePanel = new RoundedPanel(15, Color.WHITE, false);
        dataTablePanel.setLayout(new BorderLayout());
        dataTablePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {"Name", "Gender", "Course", "Fees", "Action"};
        String[][] data = {
                {"John", "Male", "Java", "$300", "Edit/Delete"},
                {"Dara", "Male", "C++", "$300", "Edit/Delete"},
                {"Bora", "Male", "C#", "$300", "Edit/Delete"},
        };
        JTable table = new JTable(data, columns);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(10,0,0,0)); // Top padding for header
        dataTablePanel.add(tableScroll, BorderLayout.CENTER);

        wrapperPanel.add(dataTablePanel, BorderLayout.CENTER);
        mainContent.add(wrapperPanel, BorderLayout.CENTER);

        // Notice board (keeping it commented as in original)
        /*
        JPanel noticeBoard = new RoundedPanel(20, Color.WHITE, false);
        noticeBoard.setLayout(new BorderLayout());
        noticeBoard.setBorder(BorderFactory.createTitledBorder("Notice Board"));
        noticeBoard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea noticeText = new JTextArea("04/10/2021\nHidemode Now\n\n03/10/2021\nFurther Reading\n");
        noticeText.setEditable(false);
        noticeBoard.add(new JScrollPane(noticeText), BorderLayout.CENTER);
        mainContent.add(noticeBoard, BorderLayout.EAST);
        */

        add(mainContent, BorderLayout.CENTER);
    }

    // Main method (usually for testing, can be kept or removed)
    public static void main(String[] args) {
//        JFrame frame = new JFrame("Dashboard");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(1200, 800);
//        DashboardPage d = new DashboardPage(null); // Pass null or a mock MainFrame for testing
//        frame.add(d);
//        frame.setVisible(true);
    }
}