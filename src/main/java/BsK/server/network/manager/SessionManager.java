package BsK.server.network.manager;

import BsK.server.network.entity.User;
import io.netty.channel.Channel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionManager {
  private static final Map<Integer, String> SESSION_ID_MAP = new ConcurrentSkipListMap<>();

  private static final Map<String, Integer> CHANNEL_SESSION_ID_MAP = new ConcurrentHashMap<>();

  private static final Map<String, User> CHANNEL_TO_USER_MAP = new ConcurrentHashMap<>();

  private static final AtomicInteger SESSION_ID = new AtomicInteger(0);


  public static User getUserByChannel(String channel) {
    return CHANNEL_TO_USER_MAP.get(channel);
  }

  public static int getSessionByChannelId(String channelId) {
    return CHANNEL_SESSION_ID_MAP.get(channelId);
  }

  public static String getChannelBySessionId(int sessionId) {
    return SESSION_ID_MAP.get(sessionId);
  }

  public static int getMaxSessionId() {
    return SESSION_ID.get();
  }

  public static int onUserLogin(Channel channel) {
    int sessionId = SESSION_ID.incrementAndGet();
    var user = new User(channel, sessionId);
    SESSION_ID_MAP.put(sessionId, channel.id().asLongText());
    CHANNEL_SESSION_ID_MAP.put(channel.id().asLongText(), sessionId);
    CHANNEL_TO_USER_MAP.put(channel.id().asLongText(), user);
    return sessionId;
  }

  public static void onUserDisconnect(Channel channel) {
    int sessionId = CHANNEL_SESSION_ID_MAP.remove(channel.id().asLongText());
    SESSION_ID_MAP.remove(sessionId);
  }
}
