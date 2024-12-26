package BsK.common.packet.req;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class RegisterRequest implements Packet {

  String username;
  String password;
  String name;
  String role;
  LocalDateTime timestamp;

  public RegisterRequest(String username, String password, String name, String role) {
    this.username = username;
    this.password = password;
    this.name = name;
    this.role = role;
    this.timestamp = LocalDateTime.now(); // Automatically sets the current time
  }
}