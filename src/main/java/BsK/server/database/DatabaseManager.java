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
}