package BsK.server.network.util;

import BsK.common.packet.Packet;
import BsK.common.util.network.NetworkUtil;
import BsK.server.network.manager.SessionManager;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserUtil {
  public static void sendPacket(int sessionId, Packet packet) {
    String channel = SessionManager.getChannelBySessionId(sessionId);
    if (channel == null) return;
    var user = SessionManager.getUserByChannel(channel);
    if (user == null) return;

    NetworkUtil.sendPacket(user.getChannel(), packet);
  }

  /**
   * Sends a packet to every session that has logged in. Connections that have not logged in
   * (or have logged out) are skipped, because broadcasts can carry patient details.
   */
  public static void broadcastToAuthenticated(Packet packet) {
    int maxSessionId = SessionManager.getMaxSessionId();
    for (int sessionId = 1; sessionId <= maxSessionId; sessionId++) {
      String channel = SessionManager.getChannelBySessionId(sessionId);
      if (channel == null) continue;
      var user = SessionManager.getUserByChannel(channel);
      if (user == null || !user.isAuthenticated()) continue;

      NetworkUtil.sendPacket(user.getChannel(), packet);
    }
  }
}
