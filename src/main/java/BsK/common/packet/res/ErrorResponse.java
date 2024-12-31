package BsK.common.packet.res;

import BsK.common.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse implements Packet {

  Error error;

    public String getError() {
        return error.toString();
    }

    public enum Error {
    SUCCESS,
    UNKNOWN,
    USERNAME_NOT_FOUND,
    PASSWORD_INCORRECT,
    USER_ALREADY_EXISTS,INVALID_CREDENTIALS,
  }
}
