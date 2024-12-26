package BsK.client.ui.handler;

import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.LoginRequest;
import BsK.common.packet.req.PingRequest;
import BsK.common.packet.res.HandshakeCompleteResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;

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
        System.out.println("Received ping request");
        id = response.getUserId();
        break;
      default:
        return;
    }
  }

  public void showUI() {
    while (true) {
      if (id != -1) {

      }
    }
  }

  public void onClickButtonLogin() {
    if (!readyToLogin) {
      // TODO: hiển thị lỗi "Chưa kết nối tới server"
      return;
    }
    var username = "username"; // Đọc từ đâu đó
    var password = "password"; // Đọc từ đâu đó
    var loginRequest = new LoginRequest(username, password);
    NetworkUtil.sendPacket(loginRequest);
  }
}
