package BsK.client;

import BsK.client.network.handler.ClientHandler;
import BsK.client.ui.handler.UIHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

@Slf4j
public class Client {
    private static void showConnectionErrorDialog(String serverAddress, int port, Exception error) {
        SwingUtilities.invokeLater(() -> {
            String message = String.format(
                """
                Không thể kết nối đến máy chủ!
                
                Địa chỉ máy chủ: %s
                Cổng: %d
                
                Nguyên nhân có thể:
                1. Máy chủ chưa được khởi động
                2. Địa chỉ hoặc cổng không chính xác
                3. Tường lửa đang chặn kết nối
                
                Giải pháp:
                1. Kiểm tra xem máy chủ đã được khởi động chưa
                2. Kiểm tra lại cấu hình trong file config.properties
                3. Tắt ứng dụng và khởi động lại
                
                Chi tiết lỗi: %s
                """,
                serverAddress,
                port,
                error.getMessage()
            );
            
            JOptionPane.showMessageDialog(
                null,
                message,
                "Lỗi Kết Nối",
                JOptionPane.ERROR_MESSAGE
            );
            
            // Exit the application after showing the error
            System.exit(1);
        });
    }

    public static void main(String[] args) throws Exception {
        var thread = new Thread(() -> {
            Properties props = new Properties();
            String serverAddress = "localhost";
            int port = 1999;
            
            // First try to load from external config file
            File externalConfig = new File("config/config.properties");
            if (externalConfig.exists()) {
                try (var input = Files.newInputStream(externalConfig.toPath())) {
                    props.load(input);
                    log.info("Loaded configuration from external file: {}", externalConfig.getAbsolutePath());
                } catch (IOException e) {
                    log.error("Error loading external config file", e);
                }
            }
            
            // If external config failed or doesn't exist, try internal config
            if (props.isEmpty()) {
                try (InputStream input = Client.class.getClassLoader().getResourceAsStream("config.properties")) {
                    if (input == null) {
                        log.error("Unable to find config.properties");
                        showConnectionErrorDialog(serverAddress, port, 
                            new Exception("Không tìm thấy file cấu hình config.properties"));
                        return;
                    }
                    props.load(input);
                    log.info("Loaded configuration from internal resources");
                } catch (IOException e) {
                    log.error("Error loading internal config file", e);
                    showConnectionErrorDialog(serverAddress, port, e);
                    return;
                }
            }

            // Get server configuration
            serverAddress = props.getProperty("server.address", "localhost");
            String portStr = props.getProperty("server.port");
            
            // Use defaults if not configured
            if (portStr != null) {
                port = Integer.parseInt(portStr);
                log.info("Using configured port: {}", port);
            } else {
                log.info("Using default port: {}", port);
            }

            log.info("Using server address: {}", serverAddress);
            if ("127.0.0.1".equals(serverAddress)) {
                log.info("Connecting to localhost");
            }
            
            URI uri = null;
            try {
                uri = new URI("ws://" + serverAddress + ":" + port + "/");
            } catch (URISyntaxException e) {
                showConnectionErrorDialog(serverAddress, port, e);
                return;
            }

            EventLoopGroup group = new NioEventLoopGroup();
            try {
                URI finalUri = uri;
                Bootstrap bootstrap =
                        new Bootstrap()
                                .group(group)
                                .channel(NioSocketChannel.class)
                                .handler(
                                        new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) {
                                                ch.pipeline()
                                                        .addLast(new IdleStateHandler(60 * 30,
                                                                0, 0, TimeUnit.SECONDS))
                                                        .addLast(new HttpClientCodec())
                                                        .addLast(new HttpObjectAggregator(8192))
                                                        .addLast(
                                                                new WebSocketClientProtocolHandler(
                                                                        finalUri,
                                                                        WebSocketVersion.V13,
                                                                        null,
                                                                        true,
                                                                        new DefaultHttpHeaders(),
                                                                        100000))
                                                        .addLast("ws", ClientHandler.INSTANCE);
                                            }
                                        });
                log.info("Connecting to {}:{}", serverAddress, port);
                Channel channel = bootstrap.connect(serverAddress, port).sync().channel();
                channel.closeFuture().sync();

            } catch (Exception e) {
                log.error("Connection error", e);
                showConnectionErrorDialog(serverAddress, port, e);
                return;
            } finally {
                try {
                    group.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    log.error("Error shutting down", e);
                }
            }
        });
        thread.start();
    }
}