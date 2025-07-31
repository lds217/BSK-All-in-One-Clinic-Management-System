package BsK.client;

import BsK.client.network.handler.ClientHandler;
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
    private static Properties config = new Properties();
    
    public static Properties getConfig() {
        return config;
    }

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
        log.info("\n____   _____ _  __\n|  _ \\ / ____| |/\n| |_) | (___ | ' /\n|  _ < \\___ \\|  <\n| |_) |____) | . \\\n|____/|_____/|_|\\_\n");

        var thread = new Thread(() -> {
            // First try to load from external config file
            File externalConfig = new File("config/config.properties");
            if (externalConfig.exists()) {
                try (var input = Files.newInputStream(externalConfig.toPath())) {
                    config.load(input);
                    log.info("Loaded configuration from external file: {}", externalConfig.getAbsolutePath());
                } catch (IOException e) {
                    log.error("Error loading external config file", e);
                }
            }
            
            // If external config failed or doesn't exist, try internal config
            if (config.isEmpty()) {
                try (InputStream input = Client.class.getClassLoader().getResourceAsStream("config.properties")) {
                    if (input == null) {
                        log.error("Unable to find config.properties");
                        showConnectionErrorDialog("localhost", 1999, 
                            new Exception("Không tìm thấy file cấu hình config.properties"));
                        return;
                    }
                    config.load(input);
                    log.info("Loaded configuration from internal resources");
                } catch (IOException e) {
                    log.error("Error loading internal config file", e);
                    showConnectionErrorDialog("localhost", 1999, e);
                    return;
                }
            }

            // Get server configuration
            String serverAddress = config.getProperty("server.address", "localhost");
            String portStr = config.getProperty("server.port");
            
            // Load media directory setting into LocalStorage
            String mediaDir = config.getProperty("storage.checkup_media_base_dir");
            if (mediaDir != null && !mediaDir.trim().isEmpty()) {
                LocalStorage.checkupMediaBaseDir = mediaDir.trim();
                log.info("Using configured media directory: {}", mediaDir);
            } else {
                log.info("Using default media directory: {}", LocalStorage.checkupMediaBaseDir);
            }
            
            // Load ultrasound folder setting into LocalStorage
            String ultrasoundFolder = config.getProperty("storage.ultrasound_folder_path");
            if (ultrasoundFolder != null && !ultrasoundFolder.trim().isEmpty()) {
                LocalStorage.ULTRASOUND_FOLDER_PATH = ultrasoundFolder.trim();
                log.info("Using configured ultrasound folder: {}", ultrasoundFolder);
            } else {
                log.info("Using default ultrasound folder: {}", LocalStorage.ULTRASOUND_FOLDER_PATH);
            }
            
            // Load auto change status setting into LocalStorage
            String autoChangeStatus = config.getProperty("app.auto_change_status_to_finished");
            if (autoChangeStatus != null && !autoChangeStatus.trim().isEmpty()) {
                LocalStorage.autoChangeStatusToFinished = Boolean.parseBoolean(autoChangeStatus.trim());
                log.info("Auto change status to finished: {}", LocalStorage.autoChangeStatusToFinished);
            } else {
                log.info("Using default auto change status: {}", LocalStorage.autoChangeStatusToFinished);
            }
            
            // Use defaults if not configured
            if (portStr != null) {
                int port = Integer.parseInt(portStr);
                log.info("Using configured port: {}", port);
            } else {
                log.info("Using default port: {}", 1999);
            }

            log.info("Using server address: {}", serverAddress);
            if ("127.0.0.1".equals(serverAddress)) {
                log.info("Connecting to localhost");
            }
            
            URI uri = null;
            try {
                uri = new URI("ws://" + serverAddress + ":" + 1999 + "/");
            } catch (URISyntaxException e) {
                showConnectionErrorDialog(serverAddress, 1999, e);
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
                                                        .addLast(new HttpObjectAggregator(50 * 1024 * 1024))
                                                        .addLast(
                                                                new WebSocketClientProtocolHandler(
                                                                        finalUri,
                                                                        WebSocketVersion.V13,
                                                                        null,
                                                                        true,
                                                                        new DefaultHttpHeaders(),
                                                                        50 * 1024 * 1024))
                                                        .addLast("ws", ClientHandler.INSTANCE);
                                            }
                                        });
                log.info("Connecting to {}:{}", serverAddress, 1999);
                Channel channel = bootstrap.connect(serverAddress, 1999).sync().channel();
                channel.closeFuture().sync();

            } catch (Exception e) {
                log.error("Connection error", e);
                showConnectionErrorDialog(serverAddress, 1999, e);
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