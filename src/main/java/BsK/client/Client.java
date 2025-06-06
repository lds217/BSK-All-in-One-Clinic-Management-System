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

@Slf4j
public class Client {
    public static void main(String[] args) throws Exception {
        var thread = new Thread(() -> {
            Properties props = new Properties();
            
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
                        return;
                    }
                    props.load(input);
                    log.info("Loaded configuration from internal resources");
                } catch (IOException e) {
                    log.error("Error loading internal config file", e);
                    return;
                }
            }

            // Get server configuration
            var serverAddress = props.getProperty("server.address");
            var portStr = props.getProperty("server.port");
            
            // Use defaults if not configured
            if (serverAddress == null) {
                serverAddress = "localhost";
                log.info("Using default server address: {}", serverAddress);
            } else {
                log.info("Using configured server address: {}", serverAddress);
            }
            
            int port;
            if (portStr != null) {
                port = Integer.parseInt(portStr);
                log.info("Using configured port: {}", port);
            } else {
                port = 1999;
                log.info("Using default port: {}", port);
            }

            if ("127.0.0.1".equals(serverAddress)) {
                log.info("Connecting to localhost");
            }
            
            URI uri = null;
            try {
                uri = new URI("ws://" + serverAddress + ":" + port + "/");
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
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

                throw new RuntimeException(e);
            } finally {
                try {
                    group.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();


    }
}