package BsK.server;

import BsK.server.network.handler.ServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {
  private static final int PORT = 1999;
  public static Connection connection;

    static {
        try {
            // Get the database from resources and copy to a temp file
            String dbPath = extractDatabaseFile();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // Delete the temp file when the JVM exits
            new File(dbPath).deleteOnExit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private static String extractDatabaseFile() throws IOException {
        // Create a temp file for the database
        Path tempFile = Files.createTempFile("BSK", ".db");
        
        // Copy the database from resources to the temp file
        try (InputStream in = Server.class.getResourceAsStream("/database/BSK.db")) {
            if (in == null) {
                throw new IOException("Database file not found in resources");
            }
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        return tempFile.toString();
    }

    public static Statement statement;

    static {
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Server() throws SQLException {
    }

    public static void main(String[] args) throws InterruptedException, SQLException {
        EventLoopGroup parentGroup =
            Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        EventLoopGroup childGroup =
            Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        try {
          ServerBootstrap bootstrap =
              new ServerBootstrap()
                  .group(parentGroup, childGroup)
                  .channel(
                      Epoll.isAvailable()
                          ? EpollServerSocketChannel.class
                          : NioServerSocketChannel.class)
                  .localAddress(new InetSocketAddress(PORT))
                  .childHandler(
                      new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                          ch.pipeline()
                              .addLast(new IdleStateHandler(60 * 30, 0, 0, TimeUnit.SECONDS))
                              .addLast(new HttpServerCodec())
                              .addLast(new ChunkedWriteHandler())
                              .addLast(new HttpObjectAggregator(8192))
                              .addLast("ws", new WebSocketServerProtocolHandler("/"))
                              .addLast(new ServerHandler());
                        }
                      });

          ChannelFuture f = bootstrap.bind().sync();

          log.info("Server started on port {}", PORT);
          f.channel().closeFuture().sync();
        } finally {
          parentGroup.shutdownGracefully();
          childGroup.shutdownGracefully();
        }
      }
    }
