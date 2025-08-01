package BsK.server.network.manager;

import BsK.server.network.entity.User;
import BsK.server.network.entity.ClientConnection;
import io.netty.channel.Channel;
import java.util.Collection;
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

  private static final Map<String, ClientConnection> CHANNEL_TO_CONNECTION_MAP = new ConcurrentHashMap<>();

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

  public static Collection<ClientConnection> getAllConnections() {
    return CHANNEL_TO_CONNECTION_MAP.values();
  }

  public static int onUserLogin(Channel channel) {
    int sessionId = SESSION_ID.incrementAndGet();
    String channelId = channel.id().asLongText();
    var user = CHANNEL_TO_USER_MAP.get(channelId);
    if(user == null) {
      user = new User(channel, sessionId);
      CHANNEL_TO_USER_MAP.put(channelId, user);
    } else {
      user.setSessionId(sessionId);
      user.setChannel(channel);
    } 
    var connection = new ClientConnection(channel, sessionId);
    
    SESSION_ID_MAP.put(sessionId, channelId);
    CHANNEL_SESSION_ID_MAP.put(channelId, sessionId);
    CHANNEL_TO_CONNECTION_MAP.put(channelId, connection);
    
    return sessionId;
  }

  public static void onUserDisconnect(Channel channel) {
    String channelId = channel.id().asLongText();
    var user = CHANNEL_TO_USER_MAP.get(channelId);
    if(user != null && user.getUserId() != -1) {
      int oldSessionId = CHANNEL_SESSION_ID_MAP.remove(channelId);
      SESSION_ID_MAP.remove(oldSessionId);
      CHANNEL_TO_CONNECTION_MAP.remove(channelId);
    }
  }

  public static void updateUserRole(String channelId, String role, int userId) {
    ClientConnection conn = CHANNEL_TO_CONNECTION_MAP.get(channelId);
    if (conn != null) {
      conn.setUserRole(role);
      conn.setUserId(userId);
    }
  }
}
