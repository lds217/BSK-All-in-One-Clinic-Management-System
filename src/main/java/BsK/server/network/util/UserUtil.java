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
    var user = SessionManager.getUserByChannel(channel);
    if (user == null) return;

    NetworkUtil.sendPacket(user.getChannel(), packet);
  }
}
