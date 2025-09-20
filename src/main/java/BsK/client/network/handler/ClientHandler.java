package BsK.client.network.handler;


import BsK.client.LocalStorage;
import BsK.client.ui.handler.UIHandler;
import BsK.common.entity.DoctorItem;
import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.res.*;
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
      } else if (packet instanceof GetMedInfoResponse) {
          GetMedInfoResponse res = (GetMedInfoResponse) packet;
          log.debug("GetMedInfoResponse received");
      }

      if (packet instanceof GetProvinceResponse provinceResponse) {
        log.info("Province info received: {}", provinceResponse);
        LocalStorage.provinces = provinceResponse.getProvinces();
        LocalStorage.provinceToId = provinceResponse.getProvinceToId();
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
        // Send ping to keep connection alive
        log.debug("Sending ping to server");
        ctx.writeAndFlush(new PingWebSocketFrame());
      } else if (event.state() == IdleState.READER_IDLE) {
        // Server hasn't responded in a while - show internet issue dialog but don't close immediately
        log.warn("Server not responding - possible network issues");
        showInternetIssueDialog();
        // Try to send a ping to test connection
        ctx.writeAndFlush(new PingWebSocketFrame());
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Connection exception: {}", cause.getMessage());
    
    // Check if it's a network-related exception that might be temporary
    if (cause instanceof IOException || 
        cause instanceof java.nio.channels.ClosedChannelException ||
        cause instanceof java.net.SocketException) {
      
      // Show internet issue dialog instead of immediately closing
      showInternetIssueDialogBlocking();
      
      // Only close after multiple failures or if explicitly requested
      log.warn("Network exception occurred, showing warning dialog");
    } else {
      // For other exceptions, show the connection lost dialog and exit
      showConnectionLostDialog();
      System.exit(0);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    log.warn("Channel became inactive - connection lost");
    
    // Show internet issue dialog and give it time to display
    showInternetIssueDialogBlocking();
    
    // Add a small delay to ensure dialog shows before channel cleanup
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    super.channelInactive(ctx);
    // The dialog will handle the exit decision
  }

  private void showInternetIssueDialog() {
    long currentTime = System.currentTimeMillis();
    
    // Only show dialog if cooldown period has passed to avoid spam
    if (internetIssueDialogShown && (currentTime - lastNetworkIssueTime) < NETWORK_ISSUE_DIALOG_COOLDOWN) {
      return;
    }
    
    internetIssueDialogShown = true;
    lastNetworkIssueTime = currentTime;
    
    SwingUtilities.invokeLater(() -> {
      String message = """
          Phát hiện vấn đề kết nối mạng!
          
          Tình trạng:
          • Mạng internet chậm hoặc không ổn định
          • Một số yêu cầu có thể bị trễ hoặc thất bại
          • Ứng dụng sẽ tiếp tục hoạt động nhưng có thể chậm
          
          Nguyên nhân có thể:
          1. Kết nối WiFi/Internet không ổn định
          2. Tốc độ mạng chậm
          3. Gián đoạn mạng tạm thời
          
          Giải pháp:
          1. Kiểm tra kết nối internet/WiFi
          2. Thử thao tác lại nếu có lỗi
          3. Đợi mạng ổn định hơn
          4. Khởi động lại ứng dụng nếu cần thiết
          
          Ứng dụng sẽ tiếp tục hoạt động...
          """;
      
      int option = JOptionPane.showOptionDialog(
          null,
          message,
          "Cảnh Báo - Vấn Đề Mạng",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE,
          null,
          new String[]{"Tiếp Tục", "Đóng Ứng Dụng"},
          "Tiếp Tục"
      );
      
      if (option == 1) { // User chose to close application
        System.exit(0);
      }
      
      // Reset the flag after some time to allow showing the dialog again if needed
      new Thread(() -> {
        try {
          Thread.sleep(NETWORK_ISSUE_DIALOG_COOLDOWN);
          internetIssueDialogShown = false;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }).start();
    });
  }
  
  private void showInternetIssueDialogBlocking() {
    long currentTime = System.currentTimeMillis();
    
    // Only show dialog if cooldown period has passed to avoid spam
    if ((internetIssueDialogShown && (currentTime - lastNetworkIssueTime) < NETWORK_ISSUE_DIALOG_COOLDOWN) || dialogCurrentlyShowing) {
      return;
    }
    
    internetIssueDialogShown = true;
    dialogCurrentlyShowing = true;
    lastNetworkIssueTime = currentTime;
    
    // Show dialog directly on current thread to ensure it displays before app can exit
    try {
      // Force the dialog to show on the EDT and block until it's handled
      if (SwingUtilities.isEventDispatchThread()) {
        // If already on EDT, show directly
        showNetworkIssueDialog();
      } else {
        // If on another thread, switch to EDT and wait
        SwingUtilities.invokeAndWait(this::showNetworkIssueDialog);
      }
    } catch (InterruptedException | InvocationTargetException e) {
      dialogCurrentlyShowing = false;
      System.err.println("Error showing dialog: " + e.getMessage());
      // Show a simple message as fallback
      System.err.println("NETWORK ISSUE DETECTED - Please restart the application");
    }
    
    dialogCurrentlyShowing = false;
    
    // Reset the flag after some time to allow showing the dialog again if needed
    new Thread(() -> {
      try {
        Thread.sleep(NETWORK_ISSUE_DIALOG_COOLDOWN);
        internetIssueDialogShown = false;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }, "NetworkDialogReset").start();
  }
  
  private void showNetworkIssueDialog() {
    String message = """
        Phát hiện vấn đề kết nối mạng!
        
        Tình trạng:
        • Mạng internet chậm hoặc không ổn định
        • Một số yêu cầu có thể bị trễ hoặc thất bại
        • Ứng dụng sẽ tiếp tục hoạt động nhưng có thể chậm
        
        Nguyên nhân có thể:
        1. Kết nối WiFi/Internet không ổn định
        2. Tốc độ mạng chậm
        3. Gián đoạn mạng tạm thời
        
        Giải pháp:
        1. Kiểm tra kết nối internet/WiFi
        2. Thử thao tác lại nếu có lỗi
        3. Đợi mạng ổn định hơn
        4. Khởi động lại ứng dụng nếu cần thiết
        
        Ứng dụng sẽ tiếp tục hoạt động...
        """;
    
    int option = JOptionPane.showOptionDialog(
        null,
        message,
        "Cảnh Báo - Vấn Đề Mạng",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null,
        new String[]{"Tiếp Tục", "Đóng Ứng Dụng"},
        "Tiếp Tục"
    );
    
    if (option == 1) { // User chose to close application
      System.exit(0);
    }
  }

  private void showConnectionLostDialog() {
    SwingUtilities.invokeLater(() -> {
      String message = """
          Mất kết nối với máy chủ!
          
          Nguyên nhân có thể:
          1. Mạng internet bị ngắt kết nối
          2. WiFi bị mất kết nối
          3. Máy chủ đã ngừng hoạt động
          4. Lỗi mạng tạm thời
          
          Giải pháp:
          1. Kiểm tra kết nối internet/WiFi
          2. Khởi động lại ứng dụng
          3. Liên hệ quản trị viên nếu vấn đề tiếp tục
          
          Ứng dụng sẽ đóng sau khi nhấn OK.
          """;
      
      JOptionPane.showMessageDialog(
          null,
          message,
          "Cảnh Báo - Mất Kết Nối",
          JOptionPane.WARNING_MESSAGE
      );
    });
  }

  @SuppressWarnings("unchecked")
  private <T> void notifyListener(ResponseListener<?> listener, T response) {
    try {
      ((ResponseListener<T>) listener).onResponse(response);

    } catch (ClassCastException e) {
      log.error("Listener type mismatch for response: {}", response.getClass(), e);
    }
  }

}
