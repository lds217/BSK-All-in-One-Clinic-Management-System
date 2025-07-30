package BsK.client.ui.component.common;

import BsK.client.LocalStorage;
import BsK.client.network.handler.ClientHandler;
import BsK.client.network.handler.ResponseListener;
import BsK.common.packet.req.SimpleMessageRequest;
import BsK.common.packet.res.SimpleMessageResponse;
import BsK.common.util.network.NetworkUtil;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SimpleChatDialog extends JDialog {

    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private final NavBar navBar; // <-- MODIFIED: This field is now used

    // Listener for incoming chat messages
    private final ResponseListener<SimpleMessageResponse> messageListener = this::handleNewMessage;

    // --- MODIFIED CONSTRUCTOR ---
    public SimpleChatDialog(Frame parent, NavBar navBar) {
        super(parent, "Tin nhắn Nội bộ", true);
        this.navBar = navBar; // Store the NavBar instance

        setSize(500, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // --- Disclaimer ---
        JLabel disclaimerLabel = new JLabel(
            "Lưu ý: Tin nhắn chỉ lưu cục bộ, không được lưu ở máy chủ và sẽ bị xoá khi tắt app.",
            SwingConstants.CENTER
        );
        disclaimerLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        disclaimerLabel.setForeground(Color.GRAY);
        disclaimerLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10)); // Top, Left, Bottom, Right
        add(disclaimerLabel, BorderLayout.NORTH);

        // --- UI Components ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setMargin(new Insets(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.addActionListener(e -> sendMessage());

        sendButton = new JButton("Gửi");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.addActionListener(e -> sendMessage());

        // --- Layout ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Networking and History ---
        setupNetworking();
        loadHistory();
    }

    // --- MODIFIED setupNetworking METHOD ---
    private void setupNetworking() {
        // Disable the NavBar's notification listener when this dialog opens
        if (this.navBar != null) {
            this.navBar.disableMessageListener();
        }

        // Register this dialog's listener to update the chat area
        ClientHandler.addResponseListener(SimpleMessageResponse.class, messageListener);

        // Re-enable the NavBar's listener when this dialog closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ClientHandler.deleteListener(SimpleMessageResponse.class, messageListener);
                if (navBar != null) {
                    navBar.enableMessageListener();
                }
            }
        });
    }

    private void loadHistory() {
        chatArea.setText(""); // Clear the area first
        for (String message : LocalStorage.chatHistory) {
            chatArea.append(message); // Append each saved message
        }
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
            SimpleMessageRequest request = new SimpleMessageRequest(LocalStorage.username, messageText);
            NetworkUtil.sendPacket(ClientHandler.ctx.channel(), request);
            messageField.setText("");
        }
    }

    // --- MODIFIED handleNewMessage METHOD ---
    private void handleNewMessage(SimpleMessageResponse response) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm").format(new Date());
            String formattedMessage = String.format("[%s] %s: %s\n",
                    timestamp,
                    response.getSenderName(),
                    response.getMessage()
            );

            LocalStorage.chatHistory.add(formattedMessage);
            chatArea.append(formattedMessage);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}