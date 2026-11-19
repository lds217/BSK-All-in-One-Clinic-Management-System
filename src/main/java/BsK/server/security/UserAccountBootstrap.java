package BsK.server.security;

import BsK.server.database.DatabaseManager;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Prepares user accounts at server startup: converts any plain-text passwords to hashes, then
 * creates an admin account if no active admin exists. Safe to run on every startup.
 */
@Slf4j
public final class UserAccountBootstrap {

  public static final String DEFAULT_ADMIN_USERNAME = "admin";

  // Leaves out characters that are easy to confuse when read from the screen (0/O, 1/l/I).
  private static final String PASSWORD_ALPHABET =
      "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int GENERATED_PASSWORD_LENGTH = 16;

  private UserAccountBootstrap() {}

  /**
   * @param showGeneratedAdminPassword receives a newly generated admin password so it can be shown
   *     once. It is called before the account is saved and the password is never logged.
   */
  public static void run(Consumer<String> showGeneratedAdminPassword) throws SQLException {
    try (Connection conn = DatabaseManager.getConnection()) {
      hashPlainTextPasswords(conn);
      seedAdminIfMissing(conn, showGeneratedAdminPassword);
    }
  }

  private static void hashPlainTextPasswords(Connection conn) throws SQLException {
    Map<Integer, String> plainTextPasswords = new LinkedHashMap<>();
    try (PreparedStatement stmt = conn.prepareStatement("SELECT user_id, password FROM User");
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String password = rs.getString("password");
        if (!PasswordHasher.isHash(password)) {
          plainTextPasswords.put(rs.getInt("user_id"), password);
        }
      }
    }
    if (plainTextPasswords.isEmpty()) {
      return;
    }

    // Hash before opening the transaction so the database is not locked during the slow work.
    Map<Integer, String> hashes = new LinkedHashMap<>();
    plainTextPasswords.forEach((id, password) -> hashes.put(id, PasswordHasher.hash(password)));

    boolean originalAutoCommit = conn.getAutoCommit();
    conn.setAutoCommit(false);
    try (PreparedStatement stmt =
        conn.prepareStatement("UPDATE User SET password = ? WHERE user_id = ? AND password = ?")) {
      for (Map.Entry<Integer, String> entry : hashes.entrySet()) {
        stmt.setString(1, entry.getValue());
        stmt.setInt(2, entry.getKey());
        stmt.setString(3, plainTextPasswords.get(entry.getKey()));
        stmt.executeUpdate();
      }
      conn.commit();
      log.info("Converted {} plain-text password(s) to hashes.", hashes.size());
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    } finally {
      conn.setAutoCommit(originalAutoCommit);
    }
  }

  private static void seedAdminIfMissing(Connection conn, Consumer<String> showPassword)
      throws SQLException {
    if (hasActiveAdmin(conn)) {
      return;
    }

    String password = generatePassword();
    String hash = PasswordHasher.hash(password);
    showPassword.accept(password);

    // Usernames are not unique in the schema, so reuse the oldest matching row if there is one.
    String restoreSql =
        "UPDATE User SET password = ?, role_name = 'ADMIN', deleted = 0 WHERE user_id = "
            + "(SELECT MIN(user_id) FROM User WHERE user_name = ?)";
    try (PreparedStatement restore = conn.prepareStatement(restoreSql)) {
      restore.setString(1, hash);
      restore.setString(2, DEFAULT_ADMIN_USERNAME);
      if (restore.executeUpdate() > 0) {
        log.warn("No active admin found. Restored the '{}' account.", DEFAULT_ADMIN_USERNAME);
        return;
      }
    }

    String insertSql =
        "INSERT INTO User (user_name, last_name, first_name, password, role_name, deleted) "
            + "VALUES (?, '', 'Admin', ?, 'ADMIN', 0)";
    try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
      insert.setString(1, DEFAULT_ADMIN_USERNAME);
      insert.setString(2, hash);
      insert.executeUpdate();
    }
    log.warn("No active admin found. Created the '{}' account.", DEFAULT_ADMIN_USERNAME);
  }

  private static boolean hasActiveAdmin(Connection conn) throws SQLException {
    String sql = "SELECT deleted FROM User WHERE role_name = 'ADMIN'";
    try (PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        if (!rs.getBoolean("deleted")) {
          return true;
        }
      }
    }
    return false;
  }

  private static String generatePassword() {
    SecureRandom random = new SecureRandom();
    StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
    for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
      password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
    }
    return password.toString();
  }
}
