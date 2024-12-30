package BsK.client.ui.handler;

import BsK.client.network.handler.ClientHandler;
import BsK.client.ui.component.MainFrame;
import BsK.common.packet.Packet;
import BsK.common.packet.req.LoginRequest;
import BsK.common.packet.res.HandshakeCompleteResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import javax.swing.*;

@Slf4j
public class UIHandler {
    public static final UIHandler INSTANCE = new UIHandler();
    boolean isLoggedIn = false;
    int id = -1;
    boolean readyToLogin = false;

    public void onPacket(Packet packet) {
        switch (packet) {
            case HandshakeCompleteResponse response:
                System.out.println("Received handshake complete response");
                readyToLogin = true;
                break;
            case LoginSuccessResponse response:
                log.info("Received login success response: " + response.getUserId() + " " + response.getRole());
                break;
            default:
                return;
        }
    }

    public void showUI() {
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }

}