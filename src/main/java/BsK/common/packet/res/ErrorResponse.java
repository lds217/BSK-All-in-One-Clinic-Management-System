package BsK.common.packet.res;

import BsK.common.packet.Packet;
import BsK.common.Error;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse implements Packet {

  Error error;

    public String getError() {
      if (error.equals(Error.ACCESS_DENIED)) {
        return "Bạn không có quyền truy cập tài nguyên này";
      }
      if (error.equals(Error.SQL_EXCEPTION)) {
        return "Lỗi kết nối cơ sở dữ liệu, vui lòng kiểm tra lại kết nối và khởi động lại ứng dụng";
      }
      if (error.equals(Error.UNKNOWN)) {
        return "Lỗi không xác định, vui lòng liên hệ với quản trị viên";
      }
      if (error.equals(Error.USERNAME_NOT_FOUND)) {
        return "Tên đăng nhập không tồn tại";
      }
      if (error.equals(Error.PASSWORD_INCORRECT)) {
        return "Mật khẩu không chính xác, vui lòng kiểm tra lại mật khẩu";
      }
      if (error.equals(Error.USER_ALREADY_EXISTS)) {
        return "Tên đăng nhập đã tồn tại, vui lòng chọn tên đăng nhập khác";
      }
      if (error.equals(Error.INVALID_CREDENTIALS)) {
        return "Tên đăng nhập hoặc mật khẩu không chính xác, vui lòng kiểm tra lại";
      }
      return error.toString();
    }


}
