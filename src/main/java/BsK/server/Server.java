package BsK.server;

import BsK.server.network.handler.ServerHandler;
import BsK.server.service.GoogleDriveServiceOAuth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {
    private static int PORT;
    
    // --- REFACTORED DATABASE CONNECTION ---
    // The single, static connection pool (DataSource). This is thread-safe.
    private static HikariDataSource dataSource;
    
    // These static fields are still okay as they are configured once at startup.
    public static GoogleDriveServiceOAuth googleDriveService;
    private static boolean googleDriveEnabled = true;
    private static String googleDriveRootFolderName = "BSK_Clinic_Patient_Files";
    public static String imageDbPath = "img_db";
    public static String checkupMediaBaseDir = "src/main/resources/image/checkup_media";

    // Static initializer block to set up the server configuration and connection pool
    static {
        try {
            // --- Load configuration ---
            Properties props = new Properties();
            File externalConfig = new File("config/config.properties");
            if (externalConfig.exists()) {
                try (var input = Files.newInputStream(externalConfig.toPath())) {
                    props.load(input);
                    log.info("Loaded configuration from external file: {}", externalConfig.getAbsolutePath());
                }
            } else {
                try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
                    if (input == null) {
                        log.error("Unable to find config.properties");
                        throw new RuntimeException("config.properties not found");
                    }
                    props.load(input);
                    log.info("Loaded configuration from internal resources");
                }
            }

            PORT = Integer.parseInt(props.getProperty("server.port", "1999"));
            log.info("Using port: {}", PORT);

            // Load other properties
            googleDriveEnabled = Boolean.parseBoolean(props.getProperty("google.drive.enabled", "true"));
            googleDriveRootFolderName = props.getProperty("google.drive.root.folder", "BSK_Clinic_Patient_Files").trim();
            imageDbPath = props.getProperty("storage.image_db_path", "img_db").trim();
            checkupMediaBaseDir = props.getProperty("storage.checkup_media_base_dir", "src/main/resources/image/checkup_media").trim();
            log.info("Google Drive enabled: {}", googleDriveEnabled);
            log.info("Google Drive root folder: {}", googleDriveRootFolderName);
            log.info("Image DB path: {}", imageDbPath);
            log.info("Checkup media base dir: {}", checkupMediaBaseDir);


            // --- DATABASE POOL INITIALIZATION ---
            String dbPath = "src/main/resources/database/BSK.db";
            
            // 1. Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setConnectionTestQuery("SELECT 1");
            config.setMaximumPoolSize(10); // Max 10 concurrent connections, perfect for your use case
            config.setPoolName("BsK-DB-Pool");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // 2. Create the DataSource (the pool itself)
            dataSource = new HikariDataSource(config);
            log.info("✅ Database connection pool initialized successfully for {}", dbPath);
            
            // Initialize Google Drive service if enabled
            initializeGoogleDriveService();
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize server", e);
            throw new RuntimeException("Failed to initialize server", e);
        }
    }

    /**
     * This is the new, safe way for handlers to get a database connection.
     * @return A database connection from the pool.
     * @throws SQLException if a connection cannot be obtained.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("\n____  _____ _  __\n|  _ \\ / ____| |/ \n| |_) | (___ | ' / \n|  _ < \\___ \\|  <\n| |_) |____) | . \\\n|____/|_____/|_|\\_\n");
        ServerDashboard dashboard = ServerDashboard.getInstance();
        dashboard.setVisible(true);
        dashboard.updateStatus("Starting...", Color.ORANGE);
        dashboard.updatePort(PORT);
        dashboard.updateGoogleDriveStatus(isGoogleDriveConnected(), isGoogleDriveConnected() ? "Connected" : "Disconnected");

        EventLoopGroup parentGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        EventLoopGroup childGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(PORT))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                            .addLast(new IdleStateHandler(60 * 30, 0, 0, TimeUnit.SECONDS))
                            .addLast(new HttpServerCodec())
                            .addLast(new ChunkedWriteHandler())
                            .addLast(new HttpObjectAggregator(50 * 1024 * 1024))
                            .addLast(new WebSocketServerProtocolHandler("/", null, true, 50 * 1024 * 1024))
                            .addLast(new ServerHandler()); // A new handler for each client
                    }
                });

            ChannelFuture f = bootstrap.bind().sync();
            log.info("Server started on port {}", PORT);
            dashboard.updateStatus("Running", Color.GREEN);
            dashboard.addLog("Server started successfully on port " + PORT);
            
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            dashboard.updateStatus("Error", Color.RED);
            dashboard.addLog("Server error: " + e.getMessage());
            log.error("Server crashed", e);
        } finally {
            dashboard.updateStatus("Shutting down", Color.ORANGE);
            dashboard.addLog("Server shutting down...");
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
            if (dataSource != null) {
                dataSource.close(); // Close the connection pool on shutdown
                log.info("Database connection pool closed.");
            }
        }
    }

    // --- Google Drive and other helper methods ---

    private static void initializeGoogleDriveService() {
        if (!googleDriveEnabled) {
            log.info("Google Drive integration is disabled");
            googleDriveService = null;
            return;
        }

        try {
            log.info("🔄 Initializing Google Drive OAuth service...");
            googleDriveService = new GoogleDriveServiceOAuth(googleDriveRootFolderName);
            log.info("✅ Google Drive OAuth service initialized successfully");
            
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().updateGoogleDriveStatus(true, "Connected");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize Google Drive OAuth service: {}", e.getMessage());
            log.info("💡 Server will continue without Google Drive integration");
            googleDriveService = null;
            
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().updateGoogleDriveStatus(false, "Failed: " + e.getMessage());
            }
        }
    }

    public static void retryGoogleDriveConnection() {
        log.info("🔄 Retrying Google Drive connection...");
        initializeGoogleDriveService();
    }

    public static boolean isGoogleDriveConnected() {
        return googleDriveService != null;
    }

    public static GoogleDriveServiceOAuth getGoogleDriveService() {
        return googleDriveService;
    }

    public static String getGoogleDriveRootFolderName() {
        return googleDriveRootFolderName;
    }

    public static void updateGoogleDriveRootFolder(String newRootFolderName) throws Exception {
        if (newRootFolderName == null || newRootFolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Root folder name cannot be empty");
        }
        
        googleDriveRootFolderName = newRootFolderName.trim();
        log.info("🔄 Updating Google Drive root folder to: {}", googleDriveRootFolderName);
        
        saveGoogleDriveRootFolderToConfig(googleDriveRootFolderName);
        
        if (googleDriveEnabled) {
            initializeGoogleDriveService();
        }
    }

    private static void saveGoogleDriveRootFolderToConfig(String rootFolderName) {
        try {
            Properties props = new Properties();
            
            File externalConfig = new File("config/config.properties");
            if (externalConfig.exists()) {
                try (var input = Files.newInputStream(externalConfig.toPath())) {
                    props.load(input);
                }
            } else {
                try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
                    if (input != null) {
                        props.load(input);
                    }
                }
            }
            
            props.setProperty("google.drive.root.folder", rootFolderName);
            
            File configDir = new File("config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            try (var output = Files.newOutputStream(externalConfig.toPath())) {
                props.store(output, "BSK Server Configuration - Updated by Dashboard");
                log.info("💾 Saved Google Drive root folder setting to config file");
            }
            
        } catch (Exception e) {
            log.warn("⚠️  Could not save Google Drive root folder to config file: {}", e.getMessage());
        }
    }

    public static void backupDatabaseToDrive() throws IOException {
        ServerDashboard dashboard = ServerDashboard.getInstance();

        if (!isGoogleDriveConnected() || googleDriveService == null) {
            dashboard.addLog("❌ Cannot backup database: Google Drive is not connected.");
            throw new IOException("Google Drive service is not available.");
        }

        String dbPath = "src/main/resources/database/BSK.db";
        java.io.File dbFile = new java.io.File(dbPath);

        if (!dbFile.exists()) {
            dashboard.addLog("❌ Cannot backup database: File not found at " + dbPath);
            throw new IOException("Database file not found: " + dbPath);
        }

        googleDriveService.backupDatabaseFile(dbFile);
    }
}
