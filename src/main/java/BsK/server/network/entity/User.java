package BsK.server.network.entity;

import BsK.server.network.manager.SessionManager;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

import BsK.server.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Slf4j
@Data
public class User {

  int userId;
  Channel channel;
  Role role = Role.GUEST;
  int sessionId;

  public User(Channel channel, int sessionId) {
    this.channel = channel;
    this.sessionId = sessionId;
  }

    /**
   * Xác thực người dùng.
   * PHIÊN BẢN NÀY ĐÃ SỬA LỖI SQL INJECTION VÀ LỖI KỸ THUẬT,
   * NHƯNG VẪN CÓ RỦI RO BẢO MẬT VÌ DÙNG MẬT KHẨU PLAINTEXT.
   */
  public void authenticate(String username, String password) {
    // Sử dụng PreparedStatement để chống SQL Injection
    String sql = "SELECT user_id, role_name, deleted FROM User WHERE user_name = ? AND password = ?";
    
    // Sử dụng try-with-resources để quản lý kết nối và tài nguyên an toàn
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, username);
        stmt.setString(2, password); // Vẫn so sánh plaintext, cần được thay đổi
        
        try (ResultSet rs = stmt.executeQuery()) {
            // Tên đăng nhập là duy nhất, chỉ nên có 1 kết quả. Dùng if thay vì while.
            if (rs.next()) {
                boolean isDeleted = rs.getBoolean("deleted");
                if (isDeleted) {
                    log.warn("Login attempt for deleted user: {}", username);
                    // Không xác thực tài khoản đã bị xóa
                    this.role = Role.GUEST;
                    this.userId = -1;
                } else {
                    log.info("User '{}' logged in successfully.", username);
                    this.userId = rs.getInt("user_id");
                    this.role = Role.valueOf(rs.getString("role_name"));
                    // Việc cập nhật session/channel nên được thực hiện ở ServerHandler
                    // sau khi kiểm tra trạng thái xác thực của đối tượng User này.
                }
            } else {
                log.warn("Failed login attempt for username: {}", username);
                // Không tìm thấy user hoặc sai mật khẩu
                this.role = Role.GUEST;
                this.userId = -1;
            }
        }
        
    } catch (SQLException e) {
        log.error("Database error during authentication for user '{}'", username, e);
        // Đảm bảo người dùng không được xác thực nếu có lỗi
        this.role = Role.GUEST;
        this.userId = -1;
    }
  }

  public boolean isAuthenticated() {
    return role != Role.GUEST;
  }

  public void resetAuthentication() {
    this.role = Role.GUEST;
    this.userId = -1; // Or some other indicator of not being logged in
    SessionManager.updateUserRole(channel.id().asLongText(), this.role.toString(), this.userId);
    log.info("User session {} has been de-authenticated.", sessionId);
  }
}