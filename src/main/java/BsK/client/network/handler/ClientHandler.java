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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
  public static ChannelHandlerContext ctx;
  public static TextWebSocketFrame frame;
  public static final ClientHandler INSTANCE = new ClientHandler();

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
        ctx.writeAndFlush(new PingWebSocketFrame());
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof IOException) {
      log.error("Connection closed");
      System.exit(0);
    }
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
