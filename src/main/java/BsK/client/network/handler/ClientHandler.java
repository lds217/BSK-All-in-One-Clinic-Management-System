package BsK.client.network.handler;

import BsK.client.ui.handler.UIHandler;
import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.LoginRequest;
import BsK.common.packet.res.ErrorResponse;
import BsK.common.packet.res.HandshakeCompleteResponse;
import BsK.common.packet.res.LoginSuccessResponse;
import BsK.common.util.network.NetworkUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.IOException;
import java.util.Scanner;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
  public static final ClientHandler INSTANCE = new ClientHandler();

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    log.debug("Received message: {}", frame.text());
    var packet = PacketSerializer.GSON.fromJson(frame.text(), Packet.class);
    switch (packet) {
      case HandshakeCompleteResponse handshakeCompleteResponse -> {
        log.info("Handshake complete");
        UIHandler.INSTANCE.onPacket(packet);
        break;
      }
      case ErrorResponse response -> {
        log.error("Received error response: {}", response.getError());
        switch (response.getError()) {
          case USER_ALREADY_EXISTS -> log.error("User already exists");
          default -> log.error("Received error response: {}", response.getError());
        }
      }
      case LoginSuccessResponse loginSuccessResponse -> {
        UIHandler.INSTANCE.onPacket(packet);
        // int id = loginSuccessResponse.getId();
        // log.info("Received login success response", id);
      }
      case null, default -> log.warn("Unknown message: {}", frame.text());
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    log.info("Connected to server!");
    startUserMenu(ctx); // Start the user menu thread
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

  public void startUserMenu(ChannelHandlerContext ctx) {
    new Thread(() -> {
      Scanner scanner = new Scanner(System.in);
      while (true) {
        System.out.println("==== Menu ====");
        System.out.println("1. Login");
        System.out.println("2. Perform Action");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");

        int choice;
        try {
          choice = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
          System.out.println("Invalid input. Please enter a number.");
          continue;
        }

        switch (choice) {
          case 1 -> {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            // Create and send login request
            var loginRequest = new LoginRequest(username, password);
            NetworkUtil.sendPacket(ctx.channel(), loginRequest);
            System.out.println("Login request sent.");
          }
          case 2 -> {
            System.out.println("Performing some action...");
            // Send a packet to perform a specific action
            // Replace `ActionRequest` with your own packet type
            // NetworkUtil.sendPacket(ctx.channel(), new ActionRequest(...));
          }
          case 3 -> {
            System.out.println("Exiting...");
            ctx.close(); // Close the connection
            System.exit(0); // Exit the program
          }
          default -> System.out.println("Invalid option. Try again.");
        }
      }
    }).start();
  }
}
