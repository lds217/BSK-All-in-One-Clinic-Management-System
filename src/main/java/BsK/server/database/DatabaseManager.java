package BsK.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static void initialize() {
        log.info("Initializing database connection pool...");
        HikariConfig config = new HikariConfig();
        
        // Sử dụng đường dẫn tương đối đến file DB
        String dbPath = "database/BSK.db";
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        
        // CRITICAL: For SQLite, we need to apply PRAGMAs on EACH connection
        // Using HikariCP's connection init SQL with a single, simple PRAGMA
        // More complex PRAGMAs will be applied and verified separately
        config.setConnectionInitSql("PRAGMA busy_timeout=10000");
        config.setConnectionTestQuery("SELECT 1");

        // Cấu hình pool
        config.setPoolName("BSK-Clinic-Pool");
        config.setMaximumPoolSize(10); // 10 kết nối đồng thời là quá đủ cho ứng dụng phòng khám
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 giây
        config.setIdleTimeout(600000); // 10 phút
        config.setMaxLifetime(1800000); // 30 phút

        dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized.");
        
        // Apply and verify critical durability PRAGMAs on first connection
        applyDurabilitySettings();
        
        // Register shutdown hook for graceful database closure
        registerShutdownHook();
        
        // Start periodic WAL checkpoint (every 5 minutes) to reduce WAL file size
        // and ensure data is regularly merged into the main database
        startPeriodicCheckpoint();
    }
    
    private static java.util.concurrent.ScheduledExecutorService checkpointScheduler;
    
    /**
     * Starts a background task that performs periodic WAL checkpoints.
     * This reduces WAL file size and ensures data is regularly merged into the main database.
     * 
     * For a clinic system, this provides extra safety by reducing the amount of data
     * that could potentially be lost in a catastrophic failure.
     */
    private static void startPeriodicCheckpoint() {
        checkpointScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WAL-Checkpoint-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        // Run checkpoint every 5 minutes (300 seconds)
        // PASSIVE mode: doesn't block if there are active transactions
        checkpointScheduler.scheduleAtFixedRate(() -> {
            try {
                log.debug("Running periodic WAL checkpoint...");
                checkpointWAL("PASSIVE");
            } catch (Exception e) {
                log.warn("Periodic WAL checkpoint failed: {}", e.getMessage());
            }
        }, 5, 5, java.util.concurrent.TimeUnit.MINUTES);
        
        log.info("✅ Periodic WAL checkpoint scheduler started (every 5 minutes).");
    }
    
    /**
     * Apply critical durability settings for power outage protection.
     * These are database-level settings (not per-connection) and are verified.
     * 
     * PRAGMA journal_mode=WAL - Database-wide, persists after close
     * PRAGMA synchronous=EXTRA - Per-connection but we verify it works
     * 
     * synchronous levels in WAL mode:
     * - OFF (0): No syncs - DANGEROUS, data loss on crash
     * - NORMAL (1): Syncs only at checkpoint - RISKY for power outage
     * - FULL (2): Syncs WAL after each commit - SAFE
     * - EXTRA (3): Like FULL + syncs directory after unlink - SAFEST
     */
    private static void applyDurabilitySettings() {
        log.info("Applying SQLite durability settings for power outage protection...");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 1. Set WAL mode (this is a database-level setting, persists)
            ResultSet rs = stmt.executeQuery("PRAGMA journal_mode=WAL");
            String journalMode = rs.next() ? rs.getString(1) : "unknown";
            log.info("✅ Journal mode set to: {}", journalMode);
            if (!"wal".equalsIgnoreCase(journalMode)) {
                log.error("❌ CRITICAL: Failed to set WAL mode! Current mode: {}", journalMode);
                throw new RuntimeException("Failed to enable WAL mode for database durability");
            }
            
            // 2. Set synchronous=EXTRA (most paranoid mode)
            // EXTRA = syncs WAL on every commit AND syncs directory after file deletions
            stmt.execute("PRAGMA synchronous=EXTRA");
            rs = stmt.executeQuery("PRAGMA synchronous");
            int syncLevel = rs.next() ? rs.getInt(1) : -1;
            // 3 = EXTRA, 2 = FULL, 1 = NORMAL, 0 = OFF
            log.info("✅ Synchronous level set to: {} (3=EXTRA, 2=FULL)", syncLevel);
            if (syncLevel < 2) {
                log.error("❌ CRITICAL: Synchronous level too low! Level: {}", syncLevel);
                throw new RuntimeException("Failed to set safe synchronous level");
            }
            
            // 3. Increase busy timeout (wait longer if database is locked)
            stmt.execute("PRAGMA busy_timeout=10000");
            rs = stmt.executeQuery("PRAGMA busy_timeout");
            int busyTimeout = rs.next() ? rs.getInt(1) : -1;
            log.info("✅ Busy timeout set to: {} ms", busyTimeout);
            
            // 4. Enable full fsync for extra safety (macOS/iOS specific but harmless on other platforms)
            stmt.execute("PRAGMA fullfsync=ON");
            
            // 5. Perform initial checkpoint to ensure database is in good state
            rs = stmt.executeQuery("PRAGMA wal_checkpoint(TRUNCATE)");
            if (rs.next()) {
                int busy = rs.getInt(1);
                int logPages = rs.getInt(2);
                int checkpointed = rs.getInt(3);
                log.info("✅ Initial WAL checkpoint: busy={}, log_pages={}, checkpointed={}", 
                        busy, logPages, checkpointed);
            }
            
            log.info("✅ All durability settings applied successfully. Database is protected against power outage.");
            
        } catch (SQLException e) {
            log.error("❌ Failed to apply durability settings!", e);
            throw new RuntimeException("Failed to configure database for durability", e);
        }
    }
    
    /**
     * Gets a connection from the pool with durability settings applied.
     * CRITICAL: In SQLite WAL mode, these settings are per-connection:
     * - synchronous: Controls when data is synced to disk
     * - busy_timeout: How long to wait when database is locked by another writer
     * 
     * This ensures every connection has maximum data safety for concurrent writes.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        try (Statement stmt = conn.createStatement()) {
            // CRITICAL for power outage protection - syncs WAL on every commit
            stmt.execute("PRAGMA synchronous=EXTRA");
            // CRITICAL for concurrent writes - wait up to 10 seconds if database is locked
            stmt.execute("PRAGMA busy_timeout=10000");
        }
        return conn;
    }

    /**
     * Gracefully closes the database connection pool.
     * IMPORTANT: Performs a checkpoint before closing to ensure all WAL data
     * is merged into the main database file.
     * 
     * This method should be called during server shutdown.
     */
    public static void close() {
        // Stop periodic checkpoint scheduler first
        if (checkpointScheduler != null && !checkpointScheduler.isShutdown()) {
            log.info("Stopping periodic checkpoint scheduler...");
            checkpointScheduler.shutdown();
            try {
                if (!checkpointScheduler.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    checkpointScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                checkpointScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Shutting down database connection pool...");
            
            // Perform final checkpoint to merge all WAL data into main database
            // This is CRITICAL for data integrity before shutdown
            try {
                log.info("Performing final WAL checkpoint before shutdown...");
                boolean checkpointSuccess = checkpointWAL("TRUNCATE");
                if (checkpointSuccess) {
                    log.info("✅ Final WAL checkpoint completed successfully.");
                } else {
                    log.warn("⚠️ Final WAL checkpoint may be incomplete. Some transactions might still be in WAL.");
                }
            } catch (Exception e) {
                log.error("❌ Error during final WAL checkpoint", e);
            }
            
            dataSource.close();
            log.info("✅ Database connection pool closed.");
        }
    }
    
    /**
     * Registers a JVM shutdown hook to ensure graceful database shutdown.
     * This ensures WAL checkpoint is performed even if the server is terminated unexpectedly.
     * 
     * Called automatically during initialization.
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM shutdown detected - closing database gracefully...");
            close();
        }, "DatabaseShutdownHook"));
        log.info("✅ Database shutdown hook registered.");
    }

    /**
     * Performs a WAL checkpoint to merge all WAL data into the main database file.
     * This ensures the main .db file contains all recent data.
     * 
     * IMPORTANT: Call this before backing up the database!
     * 
     * Checkpoint modes:
     * - PASSIVE: Tries to checkpoint without blocking (default)
     * - FULL: Waits for all transactions to complete, then checkpoints
     * - RESTART: Like FULL, then restarts the WAL
     * - TRUNCATE: Like RESTART, then truncates WAL file to 0 bytes
     * 
     * @param mode Checkpoint mode: "PASSIVE", "FULL", "RESTART", or "TRUNCATE"
     * @return true if checkpoint succeeded, false otherwise
     */
    public static boolean checkpointWAL(String mode) {
        log.info("Starting WAL checkpoint with mode: {}", mode);
        
        try (Connection conn = getConnection();
             var stmt = conn.createStatement()) {
            
            // Execute checkpoint
            var rs = stmt.executeQuery("PRAGMA wal_checkpoint(" + mode + ")");
            
            if (rs.next()) {
                int busy = rs.getInt(1);  // 0 if successful, 1 if blocked
                int walPages = rs.getInt(2);   // Number of modified pages in WAL
                int checkpointed = rs.getInt(3); // Number of pages checkpointed
                
                if (busy == 0) {
                    log.info("✅ WAL checkpoint succeeded: {} pages checkpointed out of {} WAL pages", 
                            checkpointed, walPages);
                    return true;
                } else {
                    log.warn("⚠️ WAL checkpoint blocked by active transactions. " +
                            "Close all connections and try again.");
                    return false;
                }
            }
            
        } catch (SQLException e) {
            log.error("❌ Failed to checkpoint WAL", e);
            return false;
        }
        
        return false;
    }

    /**
     * Performs a FULL WAL checkpoint (recommended before backup).
     * This ensures all data is written to the main database file.
     * 
     * Usage example:
     * <pre>
     * // Before backing up database:
     * DatabaseManager.checkpointWAL();
     * // Now safe to copy BSK.db
     * </pre>
     * 
     * @return true if checkpoint succeeded, false otherwise
     */
    public static boolean checkpointWAL() {
        return checkpointWAL("TRUNCATE");
    }
}