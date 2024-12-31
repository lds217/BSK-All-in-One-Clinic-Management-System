package BsK.server.network.handler;

import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.GetPatientInfoReq;
import BsK.common.packet.req.LoginRequest;
import BsK.common.packet.req.RegisterRequest;
import BsK.common.packet.res.ErrorResponse;
import BsK.common.packet.res.ErrorResponse.Error;
import BsK.common.packet.res.GetPatientInfoRes;
import BsK.common.packet.res.HandshakeCompleteResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;
import BsK.server.database.entity.Patient;
import BsK.server.database.entity.Patient.Sex;
import BsK.server.network.manager.SessionManager;
import BsK.server.network.util.UserUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.IOException;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    log.debug("Received message: {}", frame.text());
    Packet packet = PacketSerializer.GSON.fromJson(frame.text(), Packet.class);


    if (packet instanceof LoginRequest loginRequest) {
      log.debug(
          "Received login request: {}, {}", loginRequest.getUsername(), loginRequest.getPassword());
      var user = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
      user.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

      if (user.isAuthenticated()) {
        UserUtil.sendPacket(user.getSessionId(), new LoginSuccessResponse(user.getUserId(), user.getRole()));
        log.info("Send response to client User {} authenticated, role {}, session {}", user.getUserId(), user.getRole(), user.getSessionId());
      } else {
        log.info("User {} failed to authenticate", user.getUserId());
        UserUtil.sendPacket(user.getSessionId(), new ErrorResponse(Error.INVALID_CREDENTIALS));
      }
    } else if (packet instanceof RegisterRequest registerRequest) {
      log.debug(
          "Received register request: {}, {}",
          registerRequest.getUsername(),
          registerRequest.getPassword());
      // Tạo user trong database hoặc check exist
      boolean isUserExist = false;
      if (isUserExist) {
        var errorResponse = new ErrorResponse(Error.USER_ALREADY_EXISTS);
        NetworkUtil.sendPacket(ctx.channel(), errorResponse);
      }
    } else {
      // Gia su packet nay can verify la user da login
      var user = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
      if (!user.isAuthenticated()) {
        log.warn("Received packet from unauthenticated user: {}", packet);
        return;
      }


      switch (packet) {
        case GetPatientInfoReq getPatientInfoReq -> {
          var patientInfo =
              new Patient(getPatientInfoReq.getPatientId(), new Date(1999, 01, 01), "test",
                  Sex.MALE);
          var response = new GetPatientInfoRes(patientInfo);
          UserUtil.sendPacket(user.getUserId(), response);
        }
        case null, default -> log.warn("Received unknown packet: {}", packet);
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof IOException) {
      SessionManager.onUserDisconnect(ctx.channel());
    } else {
      log.error("ERROR: ", cause);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent event) {
      if (event.state() == IdleState.READER_IDLE) {
        try {
          SessionManager.onUserDisconnect(ctx.channel());
          ctx.channel().close();
        } catch (Exception e) {
        }
      }
    } else if (evt instanceof HandshakeComplete) {
      int SessionId = SessionManager.onUserLogin(ctx.channel());
      log.info("Session {} logged in", SessionId);

      UserUtil.sendPacket(SessionId, new HandshakeCompleteResponse());
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }
}
