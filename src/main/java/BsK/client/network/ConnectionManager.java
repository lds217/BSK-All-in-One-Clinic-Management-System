package BsK.client.network;

import BsK.client.network.handler.ClientHandler;
import BsK.common.exception.NetworkException;
import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the WebSocket connection lifecycle with automatic reconnection support.
 * This singleton handles connection, disconnection, and reconnection with exponential backoff.
 */
@Slf4j
public class ConnectionManager {
    
    private static final ConnectionManager INSTANCE = new ConnectionManager();
    
    public static ConnectionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Connection states
     */
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }
    
    /**
     * Listener interface for connection state changes
     */
    public interface ConnectionStateListener {
        void onStateChanged(ConnectionState newState, String message);
        void onReconnectCountdown(int secondsRemaining, int attemptNumber);
    }
    
    // Connection configuration
    private String serverAddress = "localhost";
    private int serverPort = 1999;
    private URI serverUri;
    
    // Connection state
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private volatile Channel channel;
    private EventLoopGroup eventLoopGroup;
    
    // Reconnection settings
    private static final int INITIAL_RETRY_DELAY_SECONDS = 1;
    private static final int MAX_RETRY_DELAY_SECONDS = 30;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    
    // Ping/Pong timeout settings - MORE TOLERANT (1 minute total)
    // Send ping every 15 seconds, allow 4 missed pongs = 60 seconds tolerance
    private static final int WRITER_IDLE_SECONDS = 15;  // Send ping every 15 seconds when idle
    private static final int READER_IDLE_SECONDS = 90;  // Fallback: no data for 90 seconds = disconnect
    
    private final AtomicInteger currentRetryDelay = new AtomicInteger(INITIAL_RETRY_DELAY_SECONDS);
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ConnectionManager-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> reconnectFuture;
    private ScheduledFuture<?> countdownFuture;
    
    // Listeners
    private final List<ConnectionStateListener> listeners = new CopyOnWriteArrayList<>();
    
    // Track if handshake is complete
    private volatile boolean handshakeComplete = false;
    
    private ConnectionManager() {
        // Private constructor for singleton
    }
    
    /**
     * Initialize connection parameters from config
     */
    public void initialize(Properties config) {
        this.serverAddress = config.getProperty("server.address", "localhost");
        String portStr = config.getProperty("server.port", "1999");
        this.serverPort = Integer.parseInt(portStr);
        
        try {
            this.serverUri = new URI("ws://" + serverAddress + ":" + serverPort + "/");
            log.info("ConnectionManager initialized with server: {}:{}", serverAddress, serverPort);
        } catch (URISyntaxException e) {
            log.error("Invalid server URI", e);
            throw new RuntimeException("Invalid server URI configuration", e);
        }
    }
    
    /**
     * Add a connection state listener
     */
    public void addListener(ConnectionStateListener listener) {
        listeners.add(listener);
        // Notify immediately of current state
        listener.onStateChanged(state.get(), getStateMessage(state.get()));
    }
    
    /**
     * Remove a connection state listener
     */
    public void removeListener(ConnectionStateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Start the connection process
     */
    public void startConnection() {
        if (state.get() == ConnectionState.CONNECTING || state.get() == ConnectionState.CONNECTED) {
            log.info("Connection already in progress or established");
            return;
        }
        
        setState(ConnectionState.CONNECTING, "Đang kết nối đến máy chủ...");
        connectAsync();
    }
    
    /**
     * Manual retry triggered by user
     */
    public void retryNow() {
        log.info("Manual retry requested");
        
        // Cancel any pending automatic retry
        cancelPendingReconnect();
        
        // Reset retry delay for manual retry
        currentRetryDelay.set(INITIAL_RETRY_DELAY_SECONDS);
        
        // Start connection
        setState(ConnectionState.RECONNECTING, "Đang thử kết nối lại...");
        connectAsync();
    }
    
    /**
     * Called when connection is established and handshake is complete
     */
    public void onConnected() {
        log.info("Connection established and handshake complete");
        handshakeComplete = true;
        reconnectAttempt.set(0);
        currentRetryDelay.set(INITIAL_RETRY_DELAY_SECONDS);
        setState(ConnectionState.CONNECTED, "Đã kết nối");
    }
    
    /**
     * Called when connection is lost
     */
    public void onDisconnected(String reason) {
        log.warn("Connection lost: {}", reason);
        handshakeComplete = false;
        channel = null;
        
        // Don't reconnect if we're already disconnected (intentional disconnect)
        if (state.get() == ConnectionState.DISCONNECTED) {
            return;
        }
        
        // Start reconnection process
        scheduleReconnect();
    }
    
    /**
     * Get current connection state
     */
    public ConnectionState getState() {
        return state.get();
    }
    
    /**
     * Check if connected and ready to send packets
     */
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED && 
               channel != null && 
               channel.isActive() && 
               handshakeComplete;
    }
    
    /**
     * Get the current channel for sending packets
     */
    public Channel getChannel() {
        return channel;
    }
    
    /**
     * Send a packet, throwing NetworkException if not connected
     */
    public void sendPacket(Packet packet) throws NetworkException {
        if (!isConnected()) {
            throw new NetworkException("Không có kết nối đến máy chủ. Vui lòng đợi kết nối được khôi phục.");
        }
        
        try {
            String text = PacketSerializer.GSON.toJson(packet);
            channel.writeAndFlush(new TextWebSocketFrame(text));
        } catch (Exception e) {
            log.error("Failed to send packet", e);
            throw new NetworkException("Lỗi gửi dữ liệu: " + e.getMessage());
        }
    }
    
    /**
     * Get the current server address
     */
    public String getServerAddress() {
        return serverAddress;
    }
    
    /**
     * Get the current server port
     */
    public int getServerPort() {
        return serverPort;
    }
    
    // ==================== Private Methods ====================
    
    private void connectAsync() {
        new Thread(() -> {
            try {
                doConnect();
            } catch (Exception e) {
                log.error("Connection attempt failed", e);
                scheduleReconnect();
            }
        }, "ConnectionManager-Connect").start();
    }
    
    private void doConnect() throws InterruptedException {
        log.info("Attempting to connect to {}:{}", serverAddress, serverPort);
        
        // Cleanup previous connection if any
        if (eventLoopGroup != null && !eventLoopGroup.isShutdown()) {
            eventLoopGroup.shutdownGracefully().sync();
        }
        
        eventLoopGroup = new NioEventLoopGroup();
        
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new HttpClientCodec())
                            .addLast(new HttpObjectAggregator(50 * 1024 * 1024))
                            .addLast(new WebSocketClientProtocolHandler(
                                serverUri,
                                WebSocketVersion.V13,
                                null,
                                true,
                                new DefaultHttpHeaders(),
                                50 * 1024 * 1024))
                            // MORE TOLERANT TIMEOUT SETTINGS:
                            // Reader timeout: 90 seconds - fallback disconnect if no data at all
                            // Writer timeout: 15 seconds - send ping every 15 seconds when idle
                            // Combined with MAX_MISSED_PONGS=4, this gives ~60 seconds tolerance
                            .addLast(new IdleStateHandler(READER_IDLE_SECONDS, WRITER_IDLE_SECONDS, 0))
                            .addLast("ws", ClientHandler.INSTANCE);
                    }
                });
            
            ChannelFuture connectFuture = bootstrap.connect(serverAddress, serverPort);
            
            // Add listener for connection result
            connectFuture.addListener(future -> {
                if (future.isSuccess()) {
                    log.info("TCP connection established, waiting for WebSocket handshake...");
                    channel = connectFuture.channel();
                    
                    // Add close listener
                    channel.closeFuture().addListener(closeFuture -> {
                        log.info("Channel closed");
                        if (state.get() != ConnectionState.DISCONNECTED) {
                            onDisconnected("Kết nối bị đóng");
                        }
                    });
                } else {
                    log.error("Connection failed: {}", future.cause().getMessage());
                    scheduleReconnect();
                }
            });
            
        } catch (Exception e) {
            log.error("Error during connection setup", e);
            scheduleReconnect();
        }
    }
    
    private void scheduleReconnect() {
        int attempt = reconnectAttempt.incrementAndGet();
        int delay = currentRetryDelay.get();
        
        log.info("Scheduling reconnect attempt {} in {} seconds", attempt, delay);
        setState(ConnectionState.RECONNECTING, 
            String.format("Đang chờ kết nối lại (lần thử %d)...", attempt));
        
        // Start countdown notifications
        startCountdown(delay, attempt);
        
        // Schedule the actual reconnect
        reconnectFuture = scheduler.schedule(() -> {
            cancelCountdown();
            setState(ConnectionState.RECONNECTING, "Đang kết nối lại...");
            connectAsync();
        }, delay, TimeUnit.SECONDS);
        
        // Calculate next delay with exponential backoff
        int nextDelay = (int) Math.min(delay * BACKOFF_MULTIPLIER, MAX_RETRY_DELAY_SECONDS);
        currentRetryDelay.set(nextDelay);
    }
    
    private void startCountdown(int seconds, int attemptNumber) {
        final AtomicInteger remaining = new AtomicInteger(seconds);
        
        countdownFuture = scheduler.scheduleAtFixedRate(() -> {
            int r = remaining.getAndDecrement();
            if (r > 0) {
                notifyCountdown(r, attemptNumber);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    private void cancelCountdown() {
        if (countdownFuture != null && !countdownFuture.isDone()) {
            countdownFuture.cancel(false);
        }
    }
    
    private void cancelPendingReconnect() {
        cancelCountdown();
        if (reconnectFuture != null && !reconnectFuture.isDone()) {
            reconnectFuture.cancel(false);
        }
    }
    
    private void setState(ConnectionState newState, String message) {
        ConnectionState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            log.info("Connection state changed: {} -> {} ({})", oldState, newState, message);
            notifyStateChange(newState, message);
        }
    }
    
    private void notifyStateChange(ConnectionState newState, String message) {
        SwingUtilities.invokeLater(() -> {
            for (ConnectionStateListener listener : listeners) {
                try {
                    listener.onStateChanged(newState, message);
                } catch (Exception e) {
                    log.error("Error notifying listener", e);
                }
            }
        });
    }
    
    private void notifyCountdown(int secondsRemaining, int attemptNumber) {
        SwingUtilities.invokeLater(() -> {
            for (ConnectionStateListener listener : listeners) {
                try {
                    listener.onReconnectCountdown(secondsRemaining, attemptNumber);
                } catch (Exception e) {
                    log.error("Error notifying countdown", e);
                }
            }
        });
    }
    
    private String getStateMessage(ConnectionState state) {
        return switch (state) {
            case DISCONNECTED -> "Chưa kết nối";
            case CONNECTING -> "Đang kết nối...";
            case CONNECTED -> "Đã kết nối";
            case RECONNECTING -> "Đang kết nối lại...";
        };
    }
    
    /**
     * Shutdown the connection manager (for application exit)
     * Sends a proper WebSocket close frame so the server can clean up the session
     */
    public void shutdown() {
        log.info("Shutting down ConnectionManager");
        state.set(ConnectionState.DISCONNECTED);
        cancelPendingReconnect();
        
        if (channel != null && channel.isActive()) {
            try {
                // Send WebSocket close frame for proper server-side cleanup
                log.info("Sending WebSocket close frame to server...");
                channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.CloseWebSocketFrame())
                    .sync();  // Wait for close frame to be sent
                
                // Give server a moment to process the close
                Thread.sleep(100);
                
                // Now close the channel
                channel.close().sync();
                log.info("Channel closed cleanly");
            } catch (InterruptedException e) {
                log.warn("Interrupted while closing channel", e);
                Thread.currentThread().interrupt();
                // Force close if interrupted
                channel.close();
            } catch (Exception e) {
                log.warn("Error during graceful close, forcing close", e);
                channel.close();
            }
        }
        
        if (eventLoopGroup != null && !eventLoopGroup.isShutdown()) {
            try {
                eventLoopGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down event loop", e);
                Thread.currentThread().interrupt();
            }
        }
        
        scheduler.shutdown();
    }
}
