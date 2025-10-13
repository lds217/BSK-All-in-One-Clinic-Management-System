package BsK.client.network.handler;


import BsK.client.LocalStorage;
import BsK.client.ui.handler.UIHandler;
import BsK.common.entity.DoctorItem;
import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.PingRequest;
import BsK.common.packet.res.*;
import BsK.common.util.network.NetworkUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
  public static ChannelHandlerContext ctx;
  public static TextWebSocketFrame frame;
  public static final ClientHandler INSTANCE = new ClientHandler();
  
  // Network resilience tracking
  private static volatile boolean internetIssueDialogShown = false;
  private static volatile long lastNetworkIssueTime = 0;
  private static final long NETWORK_ISSUE_DIALOG_COOLDOWN = 30000; // 30 seconds
  private static volatile boolean dialogCurrentlyShowing = false;
  
  // Ping-pong tracking
  private static volatile int missedPongCount = 0;
  private static final int MAX_MISSED_PONGS = 3;
  private static volatile long lastPingRequestTime = 0;
  private static volatile boolean awaitingPongResponse = false;
  
  // Disconnection type tracking
  public enum DisconnectionType {
    TIMEOUT,           // Server not responding (READER_IDLE)
    CONNECTION_LOST,   // Abrupt connection loss
    NETWORK_ERROR,     // Network-related IOException
    SERVER_SHUTDOWN,   // Clean server shutdown
    UNKNOWN            // Other/unknown causes
  }
  
  private static volatile DisconnectionType lastDisconnectionType = DisconnectionType.UNKNOWN;
  private static volatile boolean wasTimeoutDetected = false;
  private static volatile long lastPingTime = 0;
  private static volatile long lastPongTime = 0;
  private static volatile boolean awaitingPong = false;
  
  /**
   * Get a human-readable description of the disconnection type
   */
  public static String getDisconnectionTypeDescription(DisconnectionType type) {
    return switch (type) {
      case TIMEOUT -> "Server Timeout - Server stopped responding";
      case CONNECTION_LOST -> "Connection Lost - Network disconnected abruptly";
      case NETWORK_ERROR -> "Network Error - Data transmission issues";
      case SERVER_SHUTDOWN -> "Server Shutdown - Server is not available";
      case UNKNOWN -> "Unknown - Unspecified disconnection cause";
    };
  }
  
  /**
   * Get the current disconnection type (for debugging/logging)
   */
  public static DisconnectionType getCurrentDisconnectionType() {
    return lastDisconnectionType;
  }

  private static final Map<Class<?>, List<ResponseListener<?>>> listeners = new ConcurrentHashMap<>();

  public static <T> void addResponseListener(Class<T> responseType, ResponseListener<T> listener) {
    listeners.computeIfAbsent(responseType, k -> new ArrayList<>()).add(listener);
  }

  public static void clearListeners() {
    listeners.clear();
  }

  public static void deleteListener(Class<?> responseType, ResponseListener<?> listener) {
    List<ResponseListener<?>> responseListeners = listeners.get(responseType);
    if (responseListeners != null) {
      responseListeners.remove(listener);
    }
  }

  public static void deleteListener(Class<?> responseType) {
    listeners.remove(responseType);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    var packet = PacketSerializer.GSON.fromJson(frame.text(), Packet.class);

    if (packet != null) {
      // Handle HandshakeCompleteResponse to initialize ctx and frame
      if (packet instanceof HandshakeCompleteResponse) {
        log.info("Handshake complete");
        ClientHandler.ctx = ctx;
        ClientHandler.frame = frame;
        UIHandler.INSTANCE.showUI();
        return; 
      }

      if (packet instanceof ClinicInfoResponse clinicInfoResponse) {
        log.info("Clinic info received: {}", clinicInfoResponse);
        LocalStorage.ClinicName = clinicInfoResponse.getClinicName();
        LocalStorage.ClinicAddress = clinicInfoResponse.getClinicAddress();
        LocalStorage.ClinicPhone = clinicInfoResponse.getClinicPhone();
        LocalStorage.ClinicPrefix = clinicInfoResponse.getClinicPrefix();
        return;
      }

      if (packet instanceof GetDoctorGeneralInfoResponse) {
        GetDoctorGeneralInfoResponse res = (GetDoctorGeneralInfoResponse) packet;
        LocalStorage.doctorsName.clear();
        if (res.getDoctorsName() != null) {
            for (String[] doctorData : res.getDoctorsName()) {
                if (doctorData != null && doctorData.length >= 2) {
                    LocalStorage.doctorsName.add(new DoctorItem(doctorData[1], doctorData[0])); // id, name
                }
            }
        }
        log.info("Updated doctors list in LocalStorage. Total doctors: {}", LocalStorage.doctorsName.size());
      } else if (packet instanceof GetMedInfoResponse res) {
          log.debug("GetMedInfoResponse received: {}", res.getClass().getSimpleName());
      }

      if (packet instanceof GetProvinceResponse provinceResponse) {
        log.info("Province info received: {}", provinceResponse);
        LocalStorage.provinces = provinceResponse.getProvinces();
        LocalStorage.provinceToId = provinceResponse.getProvinceToId();
        return;
      }
      
      // Handle PongResponse
      if (packet instanceof PongResponse pongResponse) {
        long roundTripTime = System.currentTimeMillis() - pongResponse.getTimestamp();
        log.debug("Received PongResponse (round trip: {}ms)", roundTripTime);
        
        // Reset missed pong counter on successful pong
        missedPongCount = 0;
        awaitingPongResponse = false;
        wasTimeoutDetected = false;
        
        return;
      }

      // Dispatch to registered listeners
      List<ResponseListener<?>> responseListeners = listeners.get(packet.getClass());
      if (responseListeners != null && !responseListeners.isEmpty()) {
        for (ResponseListener<?> listener : new ArrayList<>(responseListeners)) {
          notifyListener(listener, packet);
        }
      } else {
        log.debug("No listeners registered for response type: {}", packet.getClass().getName());
      }
    } else {
      log.warn("Unknown or null packet received: {}", frame.text());
    }
  }


  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      if (event.state() == IdleState.WRITER_IDLE) {
        // Send PingRequest packet to keep connection alive
        if (awaitingPongResponse) {
          // Previous ping not answered - increment missed counter
          missedPongCount++;
          log.warn("Missed pong #{} - no response to previous ping", missedPongCount);
          
          if (missedPongCount >= MAX_MISSED_PONGS) {
            log.error("Missed {} consecutive pongs - disconnecting", missedPongCount);
            wasTimeoutDetected = true;
            lastDisconnectionType = DisconnectionType.TIMEOUT;
            ctx.close(); // Force disconnect
            return;
          }
        }
        
        // Send new ping packet
        log.debug("Sending PingRequest to server (missed count: {})", missedPongCount);
        lastPingRequestTime = System.currentTimeMillis();
        awaitingPongResponse = true;
        
        PingRequest pingRequest = new PingRequest();
        NetworkUtil.sendPacket(ctx.channel(), pingRequest);
        
      } else if (event.state() == IdleState.READER_IDLE) {
        // Server hasn't sent any data (including pong) for 5 minutes
        log.warn("Server not responding - no data received for 5 minutes (READER_IDLE)");
        wasTimeoutDetected = true;
        lastDisconnectionType = DisconnectionType.TIMEOUT;
        // Let channelInactive handle the disconnect
      }
    }
    
    // IMPORTANT: Call super to ensure event propagation continues
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Connection exception: {}", cause.getMessage());
    
    // Determine disconnection type based on exception
    if (cause instanceof java.nio.channels.ClosedChannelException) {
      lastDisconnectionType = DisconnectionType.CONNECTION_LOST;
      log.warn("Channel closed abruptly - connection lost");
    } else if (cause instanceof java.net.ConnectException) {
      lastDisconnectionType = DisconnectionType.SERVER_SHUTDOWN;
      log.warn("Cannot connect to server - server may be down");
    } else if (cause instanceof IOException || cause instanceof java.net.SocketException) {
      lastDisconnectionType = DisconnectionType.NETWORK_ERROR;
      log.warn("Network-related exception occurred");
    } else {
      lastDisconnectionType = DisconnectionType.UNKNOWN;
      log.error("Unknown exception type: {}", cause.getClass().getName());
    }
    
    // Handle based on exception type
    if (cause instanceof IOException || 
        cause instanceof java.nio.channels.ClosedChannelException ||
        cause instanceof java.net.SocketException) {
      
      // Don't show dialog here - let channelInactive handle it to avoid double dialogs
      log.warn("Network exception occurred: {}", lastDisconnectionType);
    } else {
      // For other exceptions, show the connection lost dialog and exit
      showDisconnectionDialogFinal(DisconnectionType.UNKNOWN);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.warn("Channel became inactive - connection lost");
    
    // Determine disconnection type if not already set
    if (lastDisconnectionType == DisconnectionType.UNKNOWN) {
      if (wasTimeoutDetected || missedPongCount >= MAX_MISSED_PONGS) {
        lastDisconnectionType = DisconnectionType.TIMEOUT;
        log.info("Channel inactive due to timeout (missed {} pongs)", missedPongCount);
      } else if (awaitingPongResponse && (System.currentTimeMillis() - lastPingRequestTime) > 30000) {
        lastDisconnectionType = DisconnectionType.TIMEOUT;
        log.info("Channel inactive - server stopped responding to pings");
      } else {
        lastDisconnectionType = DisconnectionType.CONNECTION_LOST;
        log.info("Channel inactive - abrupt connection loss");
      }
    }
    
    // Show appropriate dialog based on disconnection type - this forces restart
    showDisconnectionDialogFinal(lastDisconnectionType);
    
    // Add a small delay to ensure dialog shows before channel cleanup
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    super.channelInactive(ctx);
    // Reset state for potential reconnection
    resetConnectionState();
  }

  // Removed showInternetIssueDialog - consolidated into single dialog system
  
  

  // Removed showConnectionLostDialog - consolidated into single dialog system
  
  private void resetConnectionState() {
    lastDisconnectionType = DisconnectionType.UNKNOWN;
    wasTimeoutDetected = false;
    awaitingPong = false;
    lastPingTime = 0;
    lastPongTime = 0;
    missedPongCount = 0;
    awaitingPongResponse = false;
    lastPingRequestTime = 0;
  }
  
  /**
   * Shows final disconnection dialog that forces application restart
   */
  private void showDisconnectionDialogFinal(DisconnectionType type) {
    // Only show dialog if cooldown period has passed to avoid spam
    long currentTime = System.currentTimeMillis();
    if ((internetIssueDialogShown && (currentTime - lastNetworkIssueTime) < NETWORK_ISSUE_DIALOG_COOLDOWN) || dialogCurrentlyShowing) {
      // If dialog already shown recently, just exit
      System.exit(0);
      return;
    }
    
    internetIssueDialogShown = true;
    dialogCurrentlyShowing = true;
    lastNetworkIssueTime = currentTime;
    
    try {
      // Force the dialog to show on the EDT and block until it's handled
      if (SwingUtilities.isEventDispatchThread()) {
        showFinalDisconnectionDialog(type);
      } else {
        SwingUtilities.invokeAndWait(() -> showFinalDisconnectionDialog(type));
      }
    } catch (InterruptedException | InvocationTargetException e) {
      System.err.println("Error showing disconnection dialog: " + e.getMessage());
    }
    
    // Always exit after showing dialog
    System.exit(0);
  }
  
  private void showFinalDisconnectionDialog(DisconnectionType type) {
    String title;
    String message;
    
    switch (type) {
      case TIMEOUT -> {
        title = "Cảnh Báo - Hết Thời Gian Chờ";
        message = """
            Máy chủ không phản hồi!
            
            Tình trạng:
            • Máy chủ đã dừng phản hồi các yêu cầu
            • Kết nối vẫn còn nhưng không có dữ liệu
            • Có thể do máy chủ quá tải hoặc đã treo
            
            Nguyên nhân có thể:
            1. Máy chủ đang gặp sự cố hoặc quá tải
            2. Mạng chậm gây ra timeout
            3. Máy chủ đã dừng hoạt động
            
            Giải pháp:
            1. Kiểm tra kết nối internet
            2. Liên hệ quản trị viên máy chủ
            3. Thử lại sau vài phút
            
            Ứng dụng sẽ đóng để đảm bảo tính toàn vẹn dữ liệu.
            """;
      }
      case CONNECTION_LOST -> {
        title = "Cảnh Báo - Mất Kết Nối";
        message = """
            Mất kết nối với máy chủ!
            
            Tình trạng:
            • Kết nối bị ngắt đột ngột
            • Không thể giao tiếp với máy chủ
            
            Nguyên nhân có thể:
            1. Mạng internet bị ngắt kết nối
            2. WiFi bị mất kết nối
            3. Cáp mạng bị rút
            4. Router/modem gặp sự cố
            
            Giải pháp:
            1. Kiểm tra kết nối internet/WiFi
            2. Kiểm tra cáp mạng
            3. Khởi động lại router nếu cần
            4. Khởi động lại ứng dụng
            
            Ứng dụng sẽ đóng để đảm bảo tính toàn vẹn dữ liệu.
            """;
      }
      case NETWORK_ERROR -> {
        title = "Cảnh Báo - Lỗi Mạng";
        message = """
            Phát hiện lỗi mạng!
            
            Tình trạng:
            • Gặp lỗi trong quá trình truyền dữ liệu
            • Kết nối không ổn định
            
            Nguyên nhân có thể:
            1. Mạng không ổn định
            2. Gói tin bị mất
            3. Sự cố mạng tạm thời
            
            Giải pháp:
            1. Kiểm tra chất lượng mạng
            2. Thử kết nối mạng khác
            3. Đợi mạng ổn định hơn
            4. Khởi động lại ứng dụng
            
            Ứng dụng sẽ đóng để đảm bảo tính toàn vẹn dữ liệu.
            """;
      }
      case SERVER_SHUTDOWN -> {
        title = "Cảnh Báo - Máy Chủ Ngừng Hoạt Động";
        message = """
            Máy chủ đã ngừng hoạt động!
            
            Tình trạng:
            • Không thể kết nối đến máy chủ
            • Máy chủ có thể đã tắt hoặc khởi động lại
            
            Nguyên nhân có thể:
            1. Máy chủ đang bảo trì
            2. Máy chủ gặp sự cố
            3. Máy chủ đã tắt
            
            Giải pháp:
            1. Liên hệ quản trị viên máy chủ
            2. Đợi máy chủ hoạt động trở lại
            3. Kiểm tra thông báo bảo trì
            
            Ứng dụng sẽ đóng để bảo vệ dữ liệu.
            """;
      }
      default -> {
        title = "Cảnh Báo - Mất Kết Nối";
        message = """
            Mất kết nối với máy chủ!
            
            Nguyên nhân không xác định.
            
            Giải pháp:
            1. Kiểm tra kết nối internet
            2. Khởi động lại ứng dụng
            3. Liên hệ hỗ trợ kỹ thuật
            
            Ứng dụng sẽ đóng để đảm bảo an toàn dữ liệu.
            """;
      }
    }
    
    // Only show OK button - no continue option
    JOptionPane.showMessageDialog(
        null,
        message,
        title,
        JOptionPane.ERROR_MESSAGE
    );
  }
  
  // Removed old dialog methods - now using single showDisconnectionDialogFinal

  @SuppressWarnings("unchecked")
  private <T> void notifyListener(ResponseListener<?> listener, T response) {
    try {
      ((ResponseListener<T>) listener).onResponse(response);

    } catch (ClassCastException e) {
      log.error("Listener type mismatch for response: {}", response.getClass(), e);
    }
  }

}
