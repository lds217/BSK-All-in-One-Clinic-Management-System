package BsK.client;

import BsK.client.network.ClientTls;
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
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import java.net.URI;
import java.net.URISyntaxException;
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

    private static void showTlsErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, "Lỗi Kết Nối Bảo Mật", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        });
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
                4. Mạng internet/WiFi bị ngắt kết nối
                
                Giải pháp:
                1. Kiểm tra kết nối internet/WiFi
                2. Kiểm tra xem máy chủ đã được khởi động chưa
                3. Kiểm tra lại cấu hình trong file config.properties
                4. Tắt ứng dụng và khởi động lại
                
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
            applyConfigToLocalStorage(config);
            String serverAddress = LocalStorage.serverAddress;
            int serverPort;
            try {
                serverPort = Integer.parseInt(LocalStorage.serverPort);
            } catch (NumberFormatException e) {
                log.warn("Invalid server.port '{}', using 1999", LocalStorage.serverPort);
                serverPort = 1999;
                LocalStorage.serverPort = "1999";
            }
            
            // Load media directory setting into LocalStorage (also handled in applyConfigToLocalStorage)
            String mediaDir = config.getProperty("storage.checkup_media_base_dir");
            if (mediaDir != null && !mediaDir.trim().isEmpty()) {
                log.info("Using configured media directory: {}", mediaDir);
            } else {
                log.info("Using default media directory: {}", LocalStorage.checkupMediaBaseDir);
            }
            
            // Load ultrasound folder setting into LocalStorage
            String ultrasoundFolder = config.getProperty("storage.ultrasound_folder_path");
            if (ultrasoundFolder != null && !ultrasoundFolder.trim().isEmpty()) {
                log.info("Using configured ultrasound folder: {}", ultrasoundFolder);
            } else {
                log.info("Using default ultrasound folder: {}", LocalStorage.ULTRASOUND_FOLDER_PATH);
            }
            
            // Load auto change status setting into LocalStorage
            if (config.getProperty("app.auto_change_status_to_finished") != null) {
                log.info("Auto change status to finished: {}", LocalStorage.autoChangeStatusToFinished);
            } else {
                log.info("Using default auto change status: {}", LocalStorage.autoChangeStatusToFinished);
            }
            
            //load projectPath
            String projectPath = config.getProperty("app.project_path");
            if (projectPath != null && !projectPath.trim().isEmpty()) {
                log.info("Using configured project path: {}", projectPath);
            } else {
                log.info("Using default project path: {}", LocalStorage.pathToProject);
            }

            log.info("Using server address: {}:{}", serverAddress, serverPort);
            if ("127.0.0.1".equals(serverAddress) || "localhost".equalsIgnoreCase(serverAddress)) {
                log.info("Connecting to localhost");
            }
            
            URI uri = null;
            try {
                uri = new URI(ClientTls.webSocketScheme(config) + "://" + serverAddress + ":" + serverPort + "/");
            } catch (URISyntaxException e) {
                showConnectionErrorDialog(serverAddress, serverPort, e);
                return;
            }

            EventLoopGroup group = new NioEventLoopGroup();
            try {
                URI finalUri = uri;
                String tlsHost = serverAddress;
                int tlsPort = serverPort;
                Bootstrap bootstrap =
                        new Bootstrap()
                                .group(group)
                                .channel(NioSocketChannel.class)
                                .handler(
                                        new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            protected void initChannel(SocketChannel ch) throws Exception {
                                                ClientTls.addTo(ch.pipeline(), config, tlsHost, tlsPort);
                                                ch.pipeline()
                                                        .addLast(new HttpClientCodec())
                                                        .addLast(new HttpObjectAggregator(50  * 1024 * 1024))
                                                        .addLast(
                                                                new WebSocketClientProtocolHandler(
                                                                        finalUri,
                                                                        WebSocketVersion.V13,
                                                                        null,
                                                                        true,
                                                                        new DefaultHttpHeaders(),
                                                                        50  * 1024 * 1024))
                                                        .addLast("ws", ClientHandler.INSTANCE);
                                            }
                                        });
                log.info("Connecting to {}:{} ({})", serverAddress, serverPort,
                        ClientTls.isEnabled(config) ? "encrypted" : "NOT encrypted");
                Channel channel = bootstrap.connect(serverAddress, serverPort).sync().channel();

                SslHandler tls = channel.pipeline().get(SslHandler.class);
                if (tls != null) {
                    Future<Channel> handshake = tls.handshakeFuture().awaitUninterruptibly();
                    if (!handshake.isSuccess()) {
                        log.error("TLS handshake with server failed", handshake.cause());
                        channel.close();
                        showTlsErrorDialog(ClientTls.describeHandshakeFailure(handshake.cause()));
                        return;
                    }
                    log.info("Encrypted connection established ({})", tls.engine().getSession().getProtocol());
                }
                
                // Store the channel for potential reconnection
                ClientHandler.ctx = channel.pipeline().get(ClientHandler.class).ctx;
                
                // Wait for channel to close, but handle it gracefully
                channel.closeFuture().sync();

            } catch (Exception e) {
                log.error("Connection error", e);
                
                // Show more specific error message for different types of connection failures
                if (e instanceof java.net.ConnectException) {
                    showConnectionErrorDialog(serverAddress, serverPort, 
                        new Exception("Không thể kết nối đến máy chủ. Máy chủ có thể chưa được khởi động hoặc địa chỉ/cổng không đúng."));
                } else if (e instanceof java.net.UnknownHostException) {
                    showConnectionErrorDialog(serverAddress, serverPort, 
                        new Exception("Không tìm thấy máy chủ. Kiểm tra lại địa chỉ máy chủ và kết nối mạng."));
                } else if (e instanceof java.net.SocketTimeoutException) {
                    showConnectionErrorDialog(serverAddress, serverPort, 
                        new Exception("Hết thời gian chờ kết nối. Kiểm tra kết nối mạng và thử lại."));
                } else {
                    showConnectionErrorDialog(serverAddress, serverPort, e);
                }
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

    /** Loads client settings from config.properties into LocalStorage for UI and connection. */
    public static void applyConfigToLocalStorage(Properties config) {
        setConfigString(config, "server.address", "server_address", v -> LocalStorage.serverAddress = v, LocalStorage.serverAddress);
        setConfigString(config, "server.port", null, v -> LocalStorage.serverPort = v, LocalStorage.serverPort);

        setConfigString(config, "clinic.name", null, v -> LocalStorage.ClinicName = v, LocalStorage.ClinicName);
        setConfigString(config, "clinic.address", null, v -> LocalStorage.ClinicAddress = v, LocalStorage.ClinicAddress);
        setConfigString(config, "clinic.phone", null, v -> LocalStorage.ClinicPhone = v, LocalStorage.ClinicPhone);
        setConfigString(config, "clinic.prefix", null, v -> LocalStorage.ClinicPrefix = v, LocalStorage.ClinicPrefix);

        setConfigString(config, "storage.checkup_media_base_dir", null, v -> LocalStorage.checkupMediaBaseDir = v, LocalStorage.checkupMediaBaseDir);
        setConfigString(config, "storage.ultrasound_folder_path", null, v -> LocalStorage.ULTRASOUND_FOLDER_PATH = v, LocalStorage.ULTRASOUND_FOLDER_PATH);

        String googleDrive = config.getProperty("google.drive.enabled");
        if (googleDrive != null && !googleDrive.isBlank()) {
            LocalStorage.googleDriveEnabled = Boolean.parseBoolean(googleDrive.trim());
        }
        setConfigString(config, "google.drive.root.folder", null, v -> LocalStorage.googleDriveRootFolder = v, LocalStorage.googleDriveRootFolder);

        String autoStatus = config.getProperty("app.auto_change_status_to_finished");
        if (autoStatus != null && !autoStatus.isBlank()) {
            LocalStorage.autoChangeStatusToFinished = Boolean.parseBoolean(autoStatus.trim());
        }
        setConfigString(config, "app.project_path", null, v -> LocalStorage.pathToProject = v, LocalStorage.pathToProject);
    }

    private static void setConfigString(Properties config, String primaryKey, String aliasKey,
                                        java.util.function.Consumer<String> setter, String defaultValue) {
        String value = config.getProperty(primaryKey);
        if ((value == null || value.isBlank()) && aliasKey != null) {
            value = config.getProperty(aliasKey);
        }
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        } else if (defaultValue != null) {
            setter.accept(defaultValue);
        }
    }
}