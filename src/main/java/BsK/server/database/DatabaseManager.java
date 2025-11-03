package BsK.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class DatabaseManager {

    private static HikariDataSource dataSource;

    public static void initialize() {
        log.info("Initializing database connection pool...");
        HikariConfig config = new HikariConfig();
        
        // Sử dụng đường dẫn tương đối đến file DB
        String dbPath = "database/BSK.db";
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        
        // Cấu hình cho SQLite
        config.addDataSourceProperty("journal_mode", "WAL"); // Rất quan trọng để tăng khả năng ghi đồng thời
        config.addDataSourceProperty("busy_timeout", "5000"); // Chờ 5 giây nếu DB bị lock
        config.setConnectionTestQuery("SELECT 1");

        // Cấu hình pool
        config.setPoolName("BSK-Clinic-Pool");
        config.setMaximumPoolSize(10); // 10 kết nối đồng thời là quá đủ cho ứng dụng phòng khám
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 giây
        config.setIdleTimeout(600000); // 10 phút
        config.setMaxLifetime(1800000); // 30 phút

        dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized successfully.");
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            log.info("Database connection pool closed.");
        }
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