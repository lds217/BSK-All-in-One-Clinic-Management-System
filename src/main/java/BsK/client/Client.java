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

@Slf4j
public class Client {
    public static void main(String[] args) throws Exception {
        var thread = new Thread(() -> {
            var serverAddress = "localhost";
            var port = 1999;
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