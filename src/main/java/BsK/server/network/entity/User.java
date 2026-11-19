package BsK.server.network.entity;

import BsK.server.network.manager.SessionManager;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

import BsK.server.database.DatabaseManager;
import BsK.server.security.PasswordHasher;
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
   * Xác thực người dùng bằng cách so sánh mật khẩu với mã băm đã lưu.
   */
  public void authenticate(String username, String password) {
    // Tên đăng nhập không có ràng buộc UNIQUE, nên phải kiểm tra mọi dòng trùng tên.
    // Tài khoản đang hoạt động được kiểm tra trước.
    String sql = "SELECT user_id, role_name, deleted, password FROM User WHERE user_name = ? "
        + "ORDER BY deleted ASC, user_id ASC";

    // Sử dụng try-with-resources để quản lý kết nối và tài nguyên an toàn
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, username);

        this.role = Role.GUEST;
        this.userId = -1;

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                if (!PasswordHasher.verify(password, rs.getString("password"))) {
                    continue;
                }
                if (rs.getBoolean("deleted")) {
                    // Không xác thực tài khoản đã bị xóa
                    log.warn("Login attempt for deleted user: {}", username);
                    return;
                }
                log.info("User '{}' logged in successfully.", username);
                this.userId = rs.getInt("user_id");
                this.role = Role.valueOf(rs.getString("role_name"));
                // Việc cập nhật session/channel nên được thực hiện ở ServerHandler
                // sau khi kiểm tra trạng thái xác thực của đối tượng User này.
                return;
            }
        }
        log.warn("Failed login attempt for username: {}", username);

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