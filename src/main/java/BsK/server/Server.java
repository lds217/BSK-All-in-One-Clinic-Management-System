package BsK.server;

import BsK.server.network.handler.ServerHandler;
import BsK.server.service.GoogleDriveServiceOAuth;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Server {
  private static int PORT;
  public static Connection connection;
  public static GoogleDriveServiceOAuth googleDriveService;
  private static boolean googleDriveEnabled = true; // Can be configured
  private static String googleDriveRootFolderName = "BSK_Clinic_Patient_Files"; // Default
  public static String imageDbPath = "img_db"; // Default
  public static String checkupMediaBaseDir = "image/checkup_media"; // Default

    static {
        try {
            // Load configuration
            Properties props = new Properties();
            
            // First try to load from external config file
            File externalConfig = new File("config/config.properties");
            if (externalConfig.exists()) {
                try (var input = Files.newInputStream(externalConfig.toPath())) {
                    props.load(input);
                    log.info("Loaded configuration from external file: {}", externalConfig.getAbsolutePath());
                }
            } else {
                // Fall back to internal config
                try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
                    if (input == null) {
                        log.error("Unable to find config.properties");
                        throw new RuntimeException("config.properties not found");
                    }
                    props.load(input);
                    log.info("Loaded configuration from internal resources");
                }
            }
            
            // Get port configuration
            String portStr = props.getProperty("server.port");
            String addressStr = props.getProperty("server.address");
            
            // Log address configuration
            if (addressStr != null) {
                log.info("Server address configured as: {}", addressStr);
            } else {
                log.info("No server address configured, clients will need to use the correct address to connect");
            }
            
            // Get port configuration
            if (portStr != null) {
                PORT = Integer.parseInt(portStr);
                log.info("Using configured port: {}", PORT);
            } else {
                PORT = 1999;
                log.info("Using default port: {}", PORT);
            }

            // Get Google Drive configuration
            String driveEnabledStr = props.getProperty("google.drive.enabled");
            if (driveEnabledStr != null) {
                googleDriveEnabled = Boolean.parseBoolean(driveEnabledStr);
            }
            
            String driveRootFolderStr = props.getProperty("google.drive.root.folder");
            if (driveRootFolderStr != null && !driveRootFolderStr.trim().isEmpty()) {
                googleDriveRootFolderName = driveRootFolderStr.trim();
            }
            
            log.info("Google Drive integration enabled: {}", googleDriveEnabled);
            log.info("Google Drive root folder: {}", googleDriveRootFolderName);

            // Get Storage paths configuration
            String imageDbPathStr = props.getProperty("storage.image_db_path");
            if (imageDbPathStr != null && !imageDbPathStr.trim().isEmpty()) {
                imageDbPath = imageDbPathStr.trim();
            }

            String checkupMediaBaseDirStr = props.getProperty("storage.checkup_media_base_dir");
            if (checkupMediaBaseDirStr != null && !checkupMediaBaseDirStr.trim().isEmpty()) {
                checkupMediaBaseDir = checkupMediaBaseDirStr.trim();
            }

            log.info("Image DB path: {}", imageDbPath);
            log.info("Checkup media base dir: {}", checkupMediaBaseDir);

            // Directly use the provided database path
            String dbPath = "database/BSK.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            // Initialize Google Drive service if enabled
            initializeGoogleDriveService();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize server", e);
        }
    }

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
            
            // Update dashboard if it exists
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().updateGoogleDriveStatus(true, "Connected");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize Google Drive OAuth service: {}", e.getMessage());
            log.info("💡 Server will continue without Google Drive integration");
            log.info("🔧 To fix this, ensure google-oauth-credentials.json is properly configured");
            googleDriveService = null;
            
            // Update dashboard if it exists
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().updateGoogleDriveStatus(false, "Failed: " + e.getMessage());
            }
        }
    }

    /**
     * Manually retry Google Drive connection
     */
    public static void retryGoogleDriveConnection() {
        log.info("🔄 Retrying Google Drive connection...");
        initializeGoogleDriveService();
    }

    /**
     * Check if Google Drive is connected
     */
    public static boolean isGoogleDriveConnected() {
        return googleDriveService != null;
    }

    /**
     * Get Google Drive service instance
     */
    public static GoogleDriveServiceOAuth getGoogleDriveService() {
        return googleDriveService;
    }

    /**
     * Get the current Google Drive root folder name
     */
    public static String getGoogleDriveRootFolderName() {
        return googleDriveRootFolderName;
    }

    /**
     * Update Google Drive root folder name and reinitialize service
     */
    public static void updateGoogleDriveRootFolder(String newRootFolderName) throws Exception {
        if (newRootFolderName == null || newRootFolderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Root folder name cannot be empty");
        }
        
        googleDriveRootFolderName = newRootFolderName.trim();
        log.info("🔄 Updating Google Drive root folder to: {}", googleDriveRootFolderName);
        
        // Save to configuration file
        saveGoogleDriveRootFolderToConfig(googleDriveRootFolderName);
        
        // Reinitialize Google Drive service with new root folder
        if (googleDriveEnabled) {
            initializeGoogleDriveService();
        }
    }

    /**
     * Save Google Drive root folder name to config file
     */
    private static void saveGoogleDriveRootFolderToConfig(String rootFolderName) {
        try {
            Properties props = new Properties();
            
            // Load existing properties
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
            
            // Update the root folder property
            props.setProperty("google.drive.root.folder", rootFolderName);
            
            // Create config directory if it doesn't exist
            File configDir = new File("config");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // Save to external config file
            try (var output = Files.newOutputStream(externalConfig.toPath())) {
                props.store(output, "BSK Server Configuration - Updated by Dashboard");
                log.info("💾 Saved Google Drive root folder setting to config file");
            }
            
        } catch (Exception e) {
            log.warn("⚠️  Could not save Google Drive root folder to config file: {}", e.getMessage());
        }
    }

    private static String extractDatabaseFile() throws IOException {
        throw new UnsupportedOperationException("extractDatabaseFile is not used anymore. Database is read directly from the specified path.");
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
        log.info("\n____   _____ _  __\n|  _ \\ / ____| |/\n| |_) | (___ | ' /\n|  _ < \\___ \\|  <\n| |_) |____) | . \\\n|____/|_____/|_|\\_\n");
        // Initialize and show server dashboard
        ServerDashboard dashboard = ServerDashboard.getInstance();
        dashboard.setVisible(true);
        dashboard.updateStatus("Starting...", Color.ORANGE);
        dashboard.updatePort(PORT);
        
        // Update Google Drive status on dashboard
        dashboard.updateGoogleDriveStatus(isGoogleDriveConnected(), 
            isGoogleDriveConnected() ? "Connected" : "Disconnected");

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
                              .addLast(new HttpObjectAggregator(50 * 1024 * 1024))
                              .addLast(new WebSocketServerProtocolHandler("/", null, true, 50 * 1024 * 1024))
                              .addLast(new ServerHandler());
                        }
                      });

          ChannelFuture f = bootstrap.bind().sync();

          log.info("Server started on port {}", PORT);
          log.info("To connect, clients should use:");
          log.info(" - If on same machine: ws://localhost:{} or ws://127.0.0.1:{}", PORT, PORT);
          log.info(" - If on network: ws://<this-computer's-ip>:{}", PORT);
          
          // Update dashboard status to running
          dashboard.updateStatus("Running", Color.GREEN);
          dashboard.addLog("Server started successfully on port " + PORT);
          if (isGoogleDriveConnected()) {
              dashboard.addLog("Google Drive integration: Connected ✅");
          } else {
              dashboard.addLog("Google Drive integration: Disconnected ❌");
          }
          
          f.channel().closeFuture().sync();
        } catch (Exception e) {
          dashboard.updateStatus("Error", Color.RED);
          dashboard.addLog("Server error: " + e.getMessage());
          throw e;
        } finally {
          dashboard.updateStatus("Shutting down", Color.ORANGE);
          dashboard.addLog("Server shutting down...");
          parentGroup.shutdownGracefully();
          childGroup.shutdownGracefully();
        }
    }

    /**
     * Initiates the backup of the main database file to Google Drive.
     * This is intended to be called from the ServerDashboard.
     * @throws IOException if the file cannot be found or the upload fails.
     */
    public static void backupDatabaseToDrive() throws IOException {
        ServerDashboard dashboard = ServerDashboard.getInstance();

        if (!isGoogleDriveConnected() || googleDriveService == null) {
            dashboard.addLog("❌ Cannot backup database: Google Drive is not connected.");
            throw new IOException("Google Drive service is not available.");
        }

        // Define the path to the database
        String dbPath = "database/BSK.db";
        java.io.File dbFile = new java.io.File(dbPath);

        if (!dbFile.exists()) {
            dashboard.addLog("❌ Cannot backup database: File not found at " + dbPath);
            throw new IOException("Database file not found: " + dbPath);
        }

        // Call the service to perform the backup
        googleDriveService.backupDatabaseFile(dbFile);
    }   
}