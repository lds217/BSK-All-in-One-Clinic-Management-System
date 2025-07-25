package BsK.server.network.entity;

import BsK.server.network.manager.SessionManager;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

import static BsK.server.Server.statement;

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

  public void authenticate(String username, String password) {
      try {
          ResultSet rs = statement.executeQuery("select * from User where user_name = '" + username + "' and password = '" + password + "'");
          if (!rs.isBeforeFirst()) {
              System.out.println("No data found in the User table.");
          } else {
              while(rs.next())
              {
                  log.info(rs.getString("user_name") + " logged in");
                  role = Role.valueOf(rs.getString("role_name"));
                  userId = rs.getInt("user_id");
                  // Update the client connection with the new role
                  SessionManager.updateUserRole(channel.id().asLongText(), role.toString(), userId);
              }
          }
      } catch (SQLException e) {
          throw new RuntimeException(e);
      }
  }

  public boolean isAuthenticated() {
    return role != Role.GUEST;
  }
}
