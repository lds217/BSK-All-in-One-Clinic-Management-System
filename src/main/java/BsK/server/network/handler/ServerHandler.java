package BsK.server.network.handler;

import BsK.common.packet.Packet;
import BsK.server.database.DatabaseManager;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.*;
import BsK.common.packet.res.*;
import BsK.common.Error;
import BsK.common.entity.Status;
import BsK.common.util.date.DateUtils;
import BsK.common.util.network.NetworkUtil;
import BsK.server.Server;
import BsK.server.ServerDashboard;
import BsK.server.network.manager.SessionManager;
import BsK.server.network.util.UserUtil;
import BsK.server.network.entity.Role;
import BsK.server.network.entity.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.Utf8FrameValidator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import BsK.common.entity.Medicine;
import BsK.common.entity.Service;
import BsK.common.entity.PatientHistory;
import BsK.common.entity.Template;
import BsK.common.util.text.TextUtils;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.Comparator;
import java.util.stream.Stream;




@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    // Update last activity time for the client
    var connectedUser = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
    if (connectedUser != null) {
      ServerDashboard.getInstance().refreshNetworkTable();
    }
    
    Packet packet = PacketSerializer.GSON.fromJson(frame.text(), Packet.class);


    if (packet instanceof LoginRequest loginRequest) {
      log.debug(
          "Received login request: {}, {}", loginRequest.getUsername(), loginRequest.getPassword());
      var user = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
      user.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

      if (user.isAuthenticated()) {
        UserUtil.sendPacket(user.getSessionId(), new LoginSuccessResponse(user.getUserId(), user.getRole()));
        log.info("Send response to client User {} authenticated, role {}, session {}", user.getUserId(), user.getRole(), user.getSessionId());
        // Update client connection info
        SessionManager.updateUserRole(ctx.channel().id().asLongText(), user.getRole().toString(), user.getUserId());
      } else {
        log.info("User {} failed to authenticate", user.getUserId());
        UserUtil.sendPacket(user.getSessionId(), new ErrorResponse(Error.INVALID_CREDENTIALS));
      }
    } else if (packet instanceof RegisterRequest registerRequest) {
      log.debug(
          "Received register request: {}, {}",
          registerRequest.getUsername(),
          registerRequest.getPassword());
      // Tạo user trong database hoặc check exist
      boolean isUserExist = false;
    } else if (packet instanceof LogoutRequest) {
            var logoutUser = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
      if (logoutUser != null) {
        log.info("User {} (Session {}) requested logout.", logoutUser.getUserId(), logoutUser.getSessionId());
        logoutUser.resetAuthentication();
      } else {
        log.warn("Received LogoutRequest from a channel with no active user session: {}", ctx.channel().id().asLongText());
      }
      
    } else {
      // Check if user is authenticated
      var currentUser = SessionManager.getUserByChannel(ctx.channel().id().asLongText());

      if (packet instanceof PingRequest pingRequest) {
        // Respond immediately to ping with pong
       // log.debug("Received PingRequest from session {}, responding with PongResponse", currentUser.getSessionId());
        UserUtil.sendPacket(currentUser.getSessionId(), new PongResponse(pingRequest.getTimestamp()));
      }
      if (currentUser == null || !currentUser.isAuthenticated()) {
        log.warn("Received packet from unauthenticated user: {}", packet);
        return;
      }

      if (packet instanceof GetCheckUpQueueRequest getCheckUpQueueRequest) {
        log.debug("Received GetCheckUpQueueRequest");
        int shift = getCheckUpQueueRequest.getShift();
        String sql = """
            SELECT
                a.checkup_id, a.checkup_date, c.customer_last_name, c.customer_first_name,
                d.doctor_first_name, d.doctor_last_name, a.suggestion, a.diagnosis, a.notes,
                a.status, a.customer_id, c.customer_number, c.customer_address,
                a.customer_weight, a.customer_height, c.customer_gender, c.customer_dob,
                a.checkup_type, a.conclusion, a.reCheckupDate, c.cccd_ddcn, a.heart_beat,
                a.blood_pressure, c.drive_url, a.doctor_ultrasound_id, a.queue_number
            FROM
                checkup AS a
            JOIN
                customer AS c ON a.customer_id = c.customer_id
            JOIN
                Doctor D ON a.doctor_id = D.doctor_id
            WHERE
                date(a.checkup_date / 1000, 'unixepoch', '+7 hours') = date('now', '+7 hours') AND a.shift = ?""";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, shift);
            ResultSet rs = stmt.executeQuery();
    
            if (!rs.isBeforeFirst()) {
                log.warn("No data found in the checkup table for today.");
                // Gửi về một mảng rỗng thay vì không gửi gì
                UserUtil.sendPacket(currentUser.getSessionId(), new GetCheckUpQueueResponse(new String[0][0]));
            } else {
                // Phần code xử lý logic bên trong không thay đổi
                ArrayList<String> resultList = new ArrayList<>();
                while (rs.next()) {
                    String checkupId = rs.getString("checkup_id");
                    String checkupDate = rs.getString("checkup_date");
                    long checkupDateLong = Long.parseLong(checkupDate);
                    Timestamp timestamp = new Timestamp(checkupDateLong);
                    Date date = new Date(timestamp.getTime());
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    checkupDate = sdf.format(date);
                    String customerLastName = rs.getString("customer_last_name");
                    String customerFirstName = rs.getString("customer_first_name");
                    String doctorFirstName = rs.getString("doctor_first_name");
                    String doctorLastName = rs.getString("doctor_last_name");
                    String suggestion = rs.getString("suggestion");
                    String diagnosis = rs.getString("diagnosis");
                    String notes = rs.getString("notes");
                    String status = rs.getString("status");
                    String customerId = rs.getString("customer_id");
                    String customerNumber = rs.getString("customer_number");
                    String customerAddress = rs.getString("customer_address");
                    String customerWeight = rs.getString("customer_weight");
                    String customerHeight = rs.getString("customer_height");
                    String customerGender = rs.getString("customer_gender");
                    String customerDob = rs.getString("customer_dob");
                    String checkupType = rs.getString("checkup_type");
                    String conclusion = rs.getString("conclusion");
                    String reCheckupDate = rs.getString("reCheckupDate");
                    String cccdDdcn = rs.getString("cccd_ddcn");
                    String heartBeat = rs.getString("heart_beat");
                    String bloodPressure = rs.getString("blood_pressure");
                    String driveUrl = rs.getString("drive_url");
                    String doctorUltrasoundId = rs.getString("doctor_ultrasound_id");
                    String queueNumber = String.format("%02d", rs.getInt("queue_number"));
                    if (driveUrl == null) {
                        driveUrl = "";
                    }
                    String result = String.join("|", checkupId,
                            checkupDate, customerLastName, customerFirstName,
                            doctorLastName + " " + doctorFirstName, suggestion,
                            diagnosis, notes, status, customerId, customerNumber, customerAddress, customerWeight, customerHeight,
                            customerGender, customerDob, checkupType, conclusion, reCheckupDate, cccdDdcn, heartBeat, bloodPressure,
                            driveUrl, doctorUltrasoundId, queueNumber
                    );
                    resultList.add(result);
                }
    
                String[] resultString = resultList.toArray(new String[0]);
                String[][] resultArray = new String[resultString.length][];
                for (int i = 0; i < resultString.length; i++) {
                    resultArray[i] = resultString[i].split("\\|");
                }
                UserUtil.sendPacket(currentUser.getSessionId(), new GetCheckUpQueueResponse(resultArray));
            }
        } catch (SQLException e) {
        
            log.error("Error processing GetCheckUpQueueRequest", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
    }

      if (packet instanceof GetCheckUpQueueUpdateRequest getCheckUpQueueUpdateRequest) {
        log.debug("Received GetCheckUpQueueUpdateRequest");
        broadcastQueueUpdate(getCheckUpQueueUpdateRequest.getShift());
      }

      if (packet instanceof GetDoctorGeneralInfo) {
        log.debug("Received GetDoctorGeneralInfo");
        String sql = "SELECT doctor_last_name || ' ' || doctor_first_name, doctor_id FROM Doctor";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (!rs.isBeforeFirst()) {
                log.warn("No data found in the doctor table.");
                UserUtil.sendPacket(currentUser.getSessionId(), new GetDoctorGeneralInfoResponse(new String[0][0]));
            } else {
                ArrayList<String> resultList = new ArrayList<>();
                while (rs.next()) {
                    String doctorName = rs.getString(1); // Lấy theo chỉ số cột
                    String doctorId = rs.getString(2);   // Lấy theo chỉ số cột
                    resultList.add(doctorName + "|" + doctorId);
                }
                
                String[] resultString = resultList.toArray(new String[0]);
                String[][] resultArray = new String[resultString.length][];
                for (int i = 0; i < resultString.length; i++) {
                    resultArray[i] = resultString[i].split("\\|");
                }
                
                UserUtil.sendPacket(currentUser.getSessionId(), new GetDoctorGeneralInfoResponse(resultArray));
                log.info("Sent response to client GetDoctorGeneralInfo");
            }
        } catch (SQLException e) {
            log.error("Error processing GetDoctorGeneralInfo", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetPatientHistoryRequest getPatientHistoryRequest) {
        log.debug("Received GetPatientHistoryRequest for patientId: {}", getPatientHistoryRequest.getPatientId());
        
        String sql = """
            SELECT
                C.checkup_date,
                C.checkup_id,
                C.suggestion,
                C.diagnosis,
                C.prescription_id,
                C.notes,
                C.checkup_type,
                C.conclusion,
                C.reCheckupDate,
                D.doctor_last_name,
                D.doctor_first_name,
                C.customer_height,
                C.customer_weight,
                C.heart_beat,
                C.blood_pressure
            FROM Checkup C JOIN Doctor D ON C.doctor_id = D.doctor_id
            WHERE C.status = ? AND C.customer_id = ?
            ORDER BY C.checkup_date DESC;
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement historyStmt = conn.prepareStatement(sql)) {
            
            historyStmt.setString(1, "ĐÃ KHÁM");
            historyStmt.setInt(2, getPatientHistoryRequest.getPatientId());

            try (ResultSet rs = historyStmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    log.info("No history data found for patientId: {}", getPatientHistoryRequest.getPatientId());
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetPatientHistoryResponse(new String[0][0]));
                } else {
                    ArrayList<String[]> resultList = new ArrayList<>();
                    while (rs.next()) {
                        String[] historyEntry = new String[15];
                        
                        String checkupDateStr = rs.getString("checkup_date");
                        try {
                            long checkupDateLong = Long.parseLong(checkupDateStr);
                            Timestamp timestamp = new Timestamp(checkupDateLong);
                            Date date = new Date(timestamp.getTime());
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                            historyEntry[0] = sdf.format(date);
                        } catch (Exception e) {
                            historyEntry[0] = checkupDateStr;
                        }
    
                        historyEntry[1] = rs.getString("checkup_id");
                        historyEntry[2] = rs.getString("suggestion");
                        historyEntry[3] = rs.getString("diagnosis");
                        historyEntry[4] = rs.getString("prescription_id");
                        historyEntry[5] = rs.getString("notes");
                        historyEntry[6] = rs.getString("checkup_type");
                        historyEntry[7] = rs.getString("conclusion");
                        historyEntry[8] = rs.getString("reCheckupDate");   
                        historyEntry[9] = rs.getString("doctor_last_name");
                        historyEntry[10] = rs.getString("doctor_first_name");
                        historyEntry[11] = rs.getString("customer_height");
                        historyEntry[12] = rs.getString("customer_weight");
                        historyEntry[13] = rs.getString("heart_beat");
                        historyEntry[14] = rs.getString("blood_pressure");
                        resultList.add(historyEntry);
                    }
    
                    String[][] resultArray = resultList.toArray(new String[0][]);
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetPatientHistoryResponse(resultArray));
                    log.info("Sent patient history for patientId: {}", getPatientHistoryRequest.getPatientId());
                }
            }
        } catch (SQLException e) {
  
            log.error("Error fetching patient history for patientId: {}", getPatientHistoryRequest.getPatientId(), e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetMedInfoRequest getMedInfoRequest) {
        log.debug("Received GetMedInfoRequest");
        getMedInfo(currentUser.getSessionId());
      }

      if (packet instanceof GetSerInfoRequest getSerInfoRequest) {
        log.debug("Received GetSerInfoRequest");
        getSerInfo(currentUser.getSessionId());
      }

      if (packet instanceof ClinicInfoRequest clinicInfoRequest) {
        log.debug("Received ClinicInfoRequest");
        
        // Thêm LIMIT 1 vì ta chỉ mong đợi 1 dòng kết quả
        String sql = "SELECT name, address, phone, prefix FROM Clinic LIMIT 1";
    
        // SỬA ĐỔI: Sử dụng try-with-resources
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
    
            // SỬA ĐỔI: Dùng if (rs.next()) là cách làm chuẩn và an toàn hơn
            if (rs.next()) {
                String clinicName = rs.getString("name");
                String clinicAddress = rs.getString("address");
                String clinicPhone = rs.getString("phone");
                String clinicPrefix = rs.getString("prefix");
    
                UserUtil.sendPacket(currentUser.getSessionId(), new ClinicInfoResponse(clinicName, clinicAddress, clinicPhone, clinicPrefix));
            } else {
                // Ghi log nếu không tìm thấy thông tin phòng khám
                log.warn("No data found in the Clinic table. Cannot send clinic info.");
                // Bạn có thể gửi một response lỗi ở đây nếu cần, nhưng thường thì client có thể xử lý thông tin rỗng.
            }
        } catch (SQLException e) {
            // SỬA ĐỔI: Xử lý lỗi an toàn
            log.error("Error processing ClinicInfoRequest", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetRecentPatientRequest getRecentPatientRequest) {
        log.debug("Received GetRecentPatientRequest");
    
        // Sử dụng try-with-resources cho Connection để quản lý toàn bộ thao tác
        try (Connection conn = DatabaseManager.getConnection()) {
            // --- 1. Xây dựng câu truy vấn động ---
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT customer_id, customer_last_name, customer_first_name, customer_dob, customer_number, ")
                        .append("customer_address, customer_gender, cccd_ddcn FROM Customer");
            
            boolean hasNameSearch = getRecentPatientRequest.getSearchName() != null && !getRecentPatientRequest.getSearchName().trim().isEmpty();
            boolean hasPhoneSearch = getRecentPatientRequest.getSearchPhone() != null && !getRecentPatientRequest.getSearchPhone().trim().isEmpty();
            
            if (hasNameSearch || hasPhoneSearch) {
                queryBuilder.append(" WHERE ");
                if (hasNameSearch) {
                    // SỬA LỖI SQL: Dùng || thay vì CONCAT cho SQLite
                    queryBuilder.append("(LOWER(customer_first_name) LIKE ? OR LOWER(customer_last_name) LIKE ? OR ")
                                .append("LOWER(customer_last_name || ' ' || customer_first_name) LIKE ?)");
                }
                if (hasNameSearch && hasPhoneSearch) {
                    queryBuilder.append(" AND ");
                }
                if (hasPhoneSearch) {
                    queryBuilder.append("customer_number LIKE ?");
                }
            }
            
            queryBuilder.append(" ORDER BY customer_id DESC");
            
            // --- 2. Đếm tổng số bản ghi để phân trang ---
            int totalCount = 0;
            String countQuery = queryBuilder.toString().replace(
                "SELECT customer_id, customer_last_name, customer_first_name, customer_dob, customer_number, customer_address, customer_gender, cccd_ddcn", 
                "SELECT COUNT(*)"
            );
            
            try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
                int paramIndex = 1;
                if (hasNameSearch) {
                    String searchName = "%" + getRecentPatientRequest.getSearchName().toLowerCase().trim() + "%";
                    countStmt.setString(paramIndex++, searchName);
                    countStmt.setString(paramIndex++, searchName);
                    countStmt.setString(paramIndex++, searchName);
                }
                if (hasPhoneSearch) {
                    String searchPhone = "%" + getRecentPatientRequest.getSearchPhone().trim() + "%";
                    countStmt.setString(paramIndex++, searchPhone);
                }
                
                try (ResultSet countRs = countStmt.executeQuery()) {
                    if (countRs.next()) {
                        totalCount = countRs.getInt(1);
                    }
                }
            }
    
            // --- 3. Tính toán thông tin phân trang ---
            int pageSize = getRecentPatientRequest.getPageSize();
            int currentPage = getRecentPatientRequest.getPage();
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            int offset = (currentPage - 1) * pageSize;
            
            // --- 4. Truy vấn lấy dữ liệu của trang hiện tại ---
            queryBuilder.append(" LIMIT ? OFFSET ?");
            
            try (PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
                int paramIndex = 1;
                if (hasNameSearch) {
                    String searchName = "%" + getRecentPatientRequest.getSearchName().toLowerCase().trim() + "%";
                    stmt.setString(paramIndex++, searchName);
                    stmt.setString(paramIndex++, searchName);
                    stmt.setString(paramIndex++, searchName);
                }
                if (hasPhoneSearch) {
                    String searchPhone = "%" + getRecentPatientRequest.getSearchPhone().trim() + "%";
                    stmt.setString(paramIndex++, searchPhone);
                }
                stmt.setInt(paramIndex++, pageSize);
                stmt.setInt(paramIndex, offset); // Sửa lại: Dùng paramIndex thay vì paramIndex++
                
                try (ResultSet rs = stmt.executeQuery()) {
                    // Xử lý kết quả (phần này không thay đổi)
                    ArrayList<String> resultList = new ArrayList<>();
                    while (rs.next()) {
                        String customerId = rs.getString("customer_id");
                        String customerLastName = rs.getString("customer_last_name");
                        String customerFirstName = rs.getString("customer_first_name");
                        String customerDob = rs.getString("customer_dob");
                        String year = Integer.toString(DateUtils.extractYearFromTimestamp(customerDob));
                        String customerNumber = rs.getString("customer_number");
                        String customerAddress = rs.getString("customer_address");
                        String customerGender = rs.getString("customer_gender");
                        String cccdDdcn = rs.getString("cccd_ddcn");
    
                        String result = String.join("|", customerId, customerLastName + " " + customerFirstName,
                                year, customerNumber, customerAddress, customerGender, customerDob, cccdDdcn);
                        resultList.add(result);
                    }
    
                    String[] resultString = resultList.toArray(new String[0]);
                    String[][] resultArray = new String[resultString.length][];
                    for (int i = 0; i < resultString.length; i++) {
                        resultArray[i] = resultString[i].split("\\|");
                    }
                    
   
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetRecentPatientResponse(resultArray, totalCount, currentPage, totalPages, pageSize));
                }
            }
        } catch (SQLException e) {
            // Xử lý lỗi an toàn
            log.error("Error processing GetRecentPatientRequest", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetProvinceRequest getProvinceRequest) {
        log.debug("Received GetProvinceRequest");
    
        String sql = "SELECT code, name FROM provinces ORDER BY name";
    
        // SỬA ĐỔI: Sử dụng try-with-resources để quản lý kết nối và tài nguyên
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            ArrayList<String> resultList = new ArrayList<>();
            HashMap<String, String> provinceIdMap = new HashMap<>();
            
            // Luôn thêm giá trị mặc định cho ComboBox ở client
            resultList.add("Tỉnh/Thành phố");
            
            // Vòng lặp while sẽ tự xử lý trường hợp không có dữ liệu
            while (rs.next()) {
                String provinceId = rs.getString("code");
                String provinceName = rs.getString("name");
                provinceIdMap.put(provinceName, provinceId);
                resultList.add(provinceName);
            }
    
            if (resultList.size() <= 1) {
                log.warn("No data found in the province table.");
            }
            
            String[] resultString = resultList.toArray(new String[0]);
            UserUtil.sendPacket(currentUser.getSessionId(), new GetProvinceResponse(resultString, provinceIdMap));
            
        } catch (SQLException e) {
            // SỬA ĐỔI: Xử lý lỗi an toàn
            log.error("Error processing GetProvinceRequest", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetWardRequest getWardRequest) {
        log.debug("Received GetWardRequest for province {}", getWardRequest.getProvinceId());
        String sql = "SELECT name FROM wards WHERE province_code = ? ORDER BY name";
    
        // SỬA ĐỔI: Sử dụng try-with-resources cho Connection và đổi 'Server.connection' thành 'conn'
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            
            preparedStatement.setString(1, getWardRequest.getProvinceId());
            
            try (ResultSet rs = preparedStatement.executeQuery()) {
                ArrayList<String> resultList = new ArrayList<>();
                // Luôn thêm giá trị mặc định cho ComboBox ở client
                resultList.add("Phường/Xã");
                
                // Vòng lặp while sẽ tự xử lý trường hợp không có dữ liệu
                while (rs.next()) {
                    resultList.add(rs.getString("name"));
                }
    
                if (resultList.size() <= 1) {
                    log.warn("No data found in the ward table for province_code: {}", getWardRequest.getProvinceId());
                }
    
                String[] resultString = resultList.toArray(new String[0]);
                UserUtil.sendPacket(currentUser.getSessionId(), new GetWardResponse(resultString));
            }
        } catch (SQLException e) {
            // SỬA ĐỔI: Xử lý lỗi an toàn
            log.error("Error fetching wards for province {}: {}", getWardRequest.getProvinceId(), e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
      if (packet instanceof AddPatientRequest addPatientRequest) {
        log.debug("Received AddPatientRequest");
        
        String sql = "INSERT INTO Customer (customer_last_name, customer_first_name, customer_dob, customer_number, customer_address, customer_gender, cccd_ddcn) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        // SỬA ĐỔI: Bắt đầu bằng try-with-resources để lấy Connection từ pool
        try (Connection conn = DatabaseManager.getConnection()) {
            
            // 1. Bắt đầu transaction trên kết nối vừa mượn
            conn.setAutoCommit(false);
            
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                
                preparedStatement.setString(1, addPatientRequest.getPatientLastName());
                preparedStatement.setString(2, addPatientRequest.getPatientFirstName());
                preparedStatement.setLong(3, addPatientRequest.getPatientDob());
                preparedStatement.setString(4, addPatientRequest.getPatientPhone());
                preparedStatement.setString(5, addPatientRequest.getPatientAddress());
                preparedStatement.setString(6, addPatientRequest.getPatientGender());
                preparedStatement.setString(7, addPatientRequest.getPatientCccdDdcn());
                preparedStatement.executeUpdate();
    
                // Lấy customerId vừa được tạo ra
                int customerId = 0;
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        customerId = generatedKeys.getInt(1);
                    } else {
                        // Nếu không lấy được ID, ném lỗi để kích hoạt rollback
                        throw new SQLException("Creating patient failed, no ID obtained.");
                    }
                }
                
                // 2. Nếu mọi thứ thành công, commit transaction
                conn.commit();
                
                // 3. Gửi phản hồi thành công và thực hiện các tác vụ bất đồng bộ
                UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(true, customerId, "Thêm bệnh nhân thành công"));
                createPatientGoogleDriveFolderAsync(customerId, addPatientRequest.getPatientLastName(), addPatientRequest.getPatientFirstName());
    
            } catch (SQLException e) {
                // 4. Nếu có bất kỳ lỗi nào xảy ra trong khối try bên trong, rollback lại
                log.error("Error during AddPatient transaction, rolling back.", e);
                conn.rollback();
                
                // Gửi phản hồi lỗi về client
                String errorMessage = e.getMessage();
                UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(false, -1, "Lỗi: " + errorMessage));
            } finally {
                // 5. Rất quan trọng: Luôn trả lại trạng thái auto-commit về true
                // trước khi kết nối được trả về pool bởi try-with-resources
                conn.setAutoCommit(true);
            }
    
        } catch (SQLException e) {
            // Khối catch này chỉ bắt lỗi khi không thể lấy được kết nối từ pool
            log.error("Failed to get DB connection for AddPatientRequest", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(false, -1, "Lỗi: Không thể kết nối CSDL."));
        }
      }

      if (packet instanceof AddCheckupRequest addCheckupRequest) {
        log.debug("Received AddCheckupRequest to add for customer {}", addCheckupRequest.getCustomerId());
    
        // --- SỬA ĐỔI QUAN TRỌNG: Sử dụng một kết nối riêng từ pool cho toàn bộ giao dịch ---
        // try-with-resources sẽ đảm bảo kết nối được trả về pool ngay cả khi có lỗi.
    
        try (Connection conn = DatabaseManager.getConnection()) {
            int generatedCheckupId = 0;
            int queueNumber = addCheckupRequest.getQueueNumber();
            int shift = addCheckupRequest.getShift();
            boolean shouldBroadcastCounter = false;
            try {
                // 1. Bắt đầu một transaction trên kết nối CỤC BỘ (conn) này
                conn.setAutoCommit(false);
    
                // 2. LẤY SỐ THỨ TỰ (QUEUE NUMBER)
                if (queueNumber == -1) { // if queue number is -1, means the queue number is not provided, so we need to get the queue number from the database
                    String queueSql = """
                            INSERT INTO DailyQueueCounter (date, shift, current_count) 
                            VALUES (date('now', 'localtime'), ?, 1) 
                            ON CONFLICT(date, shift) DO UPDATE SET current_count = current_count + 1 
                            RETURNING current_count
                            """;
                    
                    try (PreparedStatement queueStmt = conn.prepareStatement(queueSql)) {
                        queueStmt.setInt(1, shift);
                        ResultSet rs = queueStmt.executeQuery();
                        if (rs.next()) {
                            queueNumber = rs.getInt(1);
                            shouldBroadcastCounter = true; // Auto-increment always gives new max
                        } else {
                            throw new SQLException("Failed to get or update queue number.");
                        }
                    }
                } else { // Custom queue number provided
                    int currentMax = 0;
                    String getMaxSql = "SELECT current_count FROM DailyQueueCounter WHERE date = date('now', 'localtime') AND shift = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(getMaxSql)) {
                        stmt.setInt(1, shift);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            currentMax = rs.getInt(1);
                        }
                    }

                    if (queueNumber > currentMax) {
                        String upsertSql = """
                            INSERT INTO DailyQueueCounter (date, shift, current_count) 
                            VALUES (date('now', 'localtime'), ?, ?) 
                            ON CONFLICT(date, shift) DO UPDATE SET current_count = ?
                            """;
                        try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
                            stmt.setInt(1, shift);
                            stmt.setInt(2, queueNumber);
                            stmt.setInt(3, queueNumber);
                            stmt.executeUpdate();
                        }
                        shouldBroadcastCounter = true;
                    }
                }
    
                // 3. CHÈN VÀO BẢNG CHECKUP VÀ LẤY LẠI CHECKUP_ID
                String checkupSql = "INSERT INTO Checkup (customer_id, doctor_id, checkup_type, status, queue_number, shift) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement checkupStmt = conn.prepareStatement(checkupSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    checkupStmt.setInt(1, addCheckupRequest.getCustomerId());
                    checkupStmt.setInt(2, addCheckupRequest.getDoctorId());
                    checkupStmt.setString(3, addCheckupRequest.getCheckupType());
                    checkupStmt.setString(4, addCheckupRequest.getStatus());
                    checkupStmt.setInt(5, queueNumber);
                    checkupStmt.setInt(6, shift);
                    checkupStmt.executeUpdate();
    
                    try (ResultSet generatedKeys = checkupStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            generatedCheckupId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating checkup failed, no ID obtained.");
                        }
                    }
                }
    
                // 4. CHÈN VÀO BẢNG MEDICINEORDER VÀ LẤY LẠI PRESCRIPTION_ID
                int generatedPrescriptionId = 0;
                String medOrderSql = "INSERT INTO MedicineOrder (checkup_id, customer_id, processed_by) VALUES (?, ?, ?)";
                try (PreparedStatement medOrderStmt = conn.prepareStatement(medOrderSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    medOrderStmt.setInt(1, generatedCheckupId);
                    medOrderStmt.setInt(2, addCheckupRequest.getCustomerId());
                    medOrderStmt.setInt(3, addCheckupRequest.getProcessedById());
                    medOrderStmt.executeUpdate();
    
                    try (ResultSet generatedKeys = medOrderStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            generatedPrescriptionId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating medicine order failed, no ID obtained.");
                        }
                    }
                }
    
                // 5. CẬP NHẬT BẢNG CHECKUP VỚI PRESCRIPTION_ID
                String updateSql = "UPDATE Checkup SET prescription_id = ? WHERE checkup_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, generatedPrescriptionId);
                    updateStmt.setInt(2, generatedCheckupId);
                    updateStmt.executeUpdate();
                }
    
                // 6. Nếu tất cả các bước thành công, commit transaction
                conn.commit();
    
                // Gửi phản hồi thành công và broadcast cập nhật
                UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(true, "Thêm bệnh nhân thành công", queueNumber));
                broadcastQueueUpdate(shift);
                if (shouldBroadcastCounter) { // Nếu STT không chỉ định, thì không cần gửi cho các client
                    int maxCurId = SessionManager.getMaxSessionId();
                    for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
                        UserUtil.sendPacket(sessionId, new SetCounterResponse(queueNumber, shift));
                    }
                }
                // Tạo thư mục Google Drive bất đồng bộ
                createCheckupGoogleDriveFolderAsync(generatedCheckupId, addCheckupRequest.getCustomerId());
    
            } catch (SQLException e) {
                // Nếu có bất kỳ lỗi nào trong khối try ở trên, rollback transaction
                log.error("SQLException during AddCheckupRequest, rolling back transaction.", e);
                conn.rollback(); // Quan trọng: rollback trên kết nối cục bộ
                
                // Gửi phản hồi lỗi về client
                String errorMessage = e.getMessage();
                UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(false, "Lỗi Server: " + errorMessage, -1));
            }
            // KHÔNG CẦN khối `finally` để `setAutoCommit(true)`.
            // HikariCP sẽ tự động reset trạng thái của kết nối khi nó được trả về pool.
    
        } catch (SQLException e) {
            // Lỗi này xảy ra nếu không thể lấy kết nối từ pool, hoặc có lỗi nghiêm trọng khi rollback
            log.error("Critical error during database transaction management for AddCheckupRequest.", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(false, "Lỗi hệ thống: Không thể xử lý giao dịch CSDL.", -1));
        }
      }

      if (packet instanceof SetCounterRequest setCounterRequest) {
        log.debug("Received SetCounterRequest to set counter to {}", setCounterRequest.getCounter());
        int shift = setCounterRequest.getShift();
    
        try (Connection conn = DatabaseManager.getConnection()) {
            int currentMax = 0;
            String getMaxSql = "SELECT current_count FROM DailyQueueCounter WHERE date = date('now', 'localtime') AND shift = ?";
            try (PreparedStatement stmt = conn.prepareStatement(getMaxSql)) {
                stmt.setInt(1, shift);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    currentMax = rs.getInt(1);
                }
            }

            int newCounter = setCounterRequest.getCounter();
            if (newCounter > currentMax) {
                String upsertSql = """
                    INSERT INTO DailyQueueCounter (date, shift, current_count) 
                    VALUES (date('now', 'localtime'), ?, ?) 
                    ON CONFLICT(date, shift) DO UPDATE SET current_count = ?
                    """;
                try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
                    stmt.setInt(1, shift);
                    stmt.setInt(2, newCounter);
                    stmt.setInt(3, newCounter);
                    stmt.executeUpdate();
                }

                int maxCurId = SessionManager.getMaxSessionId();
                for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
                    UserUtil.sendPacket(sessionId, new SetCounterResponse(newCounter, shift));
                }
            }
        } catch (SQLException e) {
            log.error("Error during SetCounterRequest.", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetCounterRequest getCounterRequest) {
        log.debug("Received GetCounterRequest");
        int shift = getCounterRequest.getShift();
        String sql = "SELECT current_count FROM DailyQueueCounter WHERE date = date('now', 'localtime') AND shift = ?;";
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, shift);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    int counter = rs.getInt(1);
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetCounterResponse(counter, shift));
                } else {
                    // No counter for this shift today, so send 0
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetCounterResponse(0, shift));
                }
            }
        } catch (SQLException e) {
            log.error("Error during GetCounterRequest, rolling back transaction.", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof CallPatientRequest callPatientRequest) {
        log.debug("Received CallPatientRequest to call patient checkup_id: {}", callPatientRequest.getPatientId());
        int checkupId = callPatientRequest.getPatientId(); // This is the checkup_id
        int roomId = callPatientRequest.getRoomId();
        Status status = callPatientRequest.getStatus();
        String queueNumber = callPatientRequest.getQueueNumber();
        // send to all clients with the queue number included
        int maxCurId = SessionManager.getMaxSessionId();
        for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
            UserUtil.sendPacket(sessionId, new CallPatientResponse(checkupId, roomId, queueNumber, status));
        }
      }

      if (packet instanceof SaveCheckupRequest saveCheckupRequest) {
        log.debug("Received SaveCheckupRequest to save checkup {}", saveCheckupRequest.getCheckupId());
    
        // --- SỬA ĐỔI QUAN TRỌNG: Lấy một kết nối riêng từ pool cho toàn bộ giao dịch ---
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                // 1. Bắt đầu transaction trên kết nối CỤC BỘ (conn)
                conn.setAutoCommit(false);
    
                // 2. CẬP NHẬT THÔNG TIN KHÁCH HÀNG (CUSTOMER)
                String customerSql = """
                    INSERT INTO Customer (
                        customer_id, customer_first_name, customer_last_name, customer_dob,
                        customer_gender, customer_address, customer_number, cccd_ddcn
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(customer_id) DO UPDATE SET
                        customer_first_name = excluded.customer_first_name,
                        customer_last_name = excluded.customer_last_name,
                        customer_dob = excluded.customer_dob,
                        customer_gender = excluded.customer_gender,
                        customer_address = excluded.customer_address,
                        customer_number = excluded.customer_number,
                        cccd_ddcn = excluded.cccd_ddcn
                    """;
                try (PreparedStatement customerStmt = conn.prepareStatement(customerSql)) {
                    customerStmt.setInt(1, saveCheckupRequest.getCustomerId());
                    customerStmt.setString(2, saveCheckupRequest.getCustomerFirstName());
                    customerStmt.setString(3, saveCheckupRequest.getCustomerLastName());
                    customerStmt.setLong(4, saveCheckupRequest.getCustomerDob());
                    customerStmt.setString(5, saveCheckupRequest.getCustomerGender());
                    customerStmt.setString(6, saveCheckupRequest.getCustomerAddress());
                    customerStmt.setString(7, saveCheckupRequest.getCustomerNumber());
                    customerStmt.setString(8, saveCheckupRequest.getCustomerCccdDdcn());
                    customerStmt.executeUpdate();
                }
    
                // 3. XỬ LÝ ĐƠN THUỐC (MEDICINE PRESCRIPTION)
                Integer newPrescriptionId = null;
    
                // Xóa dữ liệu cũ của đơn thuốc liên quan đến lần khám này
                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM OrderItem WHERE checkup_id = ?")) {
                    deleteStmt.setInt(1, saveCheckupRequest.getCheckupId());
                    deleteStmt.executeUpdate();
                }
                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM MedicineOrder WHERE checkup_id = ?")) {
                    deleteStmt.setInt(1, saveCheckupRequest.getCheckupId());
                    deleteStmt.executeUpdate();
                }
                log.info("Cleared previous medicine prescription for checkup_id: {}", saveCheckupRequest.getCheckupId());
    
                // Nếu có đơn thuốc mới, tạo lại từ đầu
                if (saveCheckupRequest.getMedicinePrescription() != null && saveCheckupRequest.getMedicinePrescription().length > 0) {
                    // A. Chèn vào MedicineOrder để lấy prescription_id mới
                    String medicineOrderSql = "INSERT INTO MedicineOrder (checkup_id, customer_id, total_amount, status, payment_status) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement medicineOrderStmt = conn.prepareStatement(medicineOrderSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        double totalMedicineAmount = 0;
                        for (String[] medicine : saveCheckupRequest.getMedicinePrescription()) {
                            if (medicine.length > 8) totalMedicineAmount += Double.parseDouble(medicine[8]);
                        }
                        medicineOrderStmt.setInt(1, saveCheckupRequest.getCheckupId());
                        medicineOrderStmt.setInt(2, saveCheckupRequest.getCustomerId());
                        medicineOrderStmt.setDouble(3, totalMedicineAmount);
                        medicineOrderStmt.setString(4, "Pending"); // status
                        medicineOrderStmt.setString(5, "Unpaid");  // payment_status
                        medicineOrderStmt.executeUpdate();
    
                        try (ResultSet generatedKeys = medicineOrderStmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                newPrescriptionId = generatedKeys.getInt(1);
                            } else {
                                throw new SQLException("Creating medicine order failed, no ID obtained.");
                            }
                        }
                    }
    
                    // B. Chèn các chi tiết thuốc vào OrderItem bằng batch
                    String orderItemSql = "INSERT INTO OrderItem (prescription_id, med_id, quantity_ordered, dosage, price_per_unit, total_price, checkup_id, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement orderItemStmt = conn.prepareStatement(orderItemSql)) {
                        for (String[] medicine : saveCheckupRequest.getMedicinePrescription()) {
                            if (medicine.length >= 10) {
                                orderItemStmt.setInt(1, newPrescriptionId);
                                orderItemStmt.setString(2, medicine[0]); // med_id
                                orderItemStmt.setString(3, medicine[2]); // quantity
                                String dosage = String.format("Sáng %s, Trưa %s, Chiều %s", medicine[4], medicine[5], medicine[6]);
                                orderItemStmt.setString(4, dosage);
                                orderItemStmt.setString(5, medicine[7]); // unit_price
                                orderItemStmt.setString(6, medicine[8]); // total_price
                                orderItemStmt.setInt(7, saveCheckupRequest.getCheckupId());
                                orderItemStmt.setString(8, medicine[9]); // notes
                                orderItemStmt.addBatch();
                            }
                        }
                        orderItemStmt.executeBatch();
                    }
                }
    
                // 4. CẬP NHẬT THÔNG TIN LẦN KHÁM (CHECKUP)
                String checkupSql = """
                    UPDATE Checkup SET 
                        suggestion = ?, diagnosis = ?, prescription_id = ?, notes = ?, status = ?,
                        checkup_type = ?, conclusion = ?, reCheckupDate = ?, customer_weight = ?,
                        customer_height = ?, heart_beat = ?, blood_pressure = ?, 
                        doctor_ultrasound_id = ?, doctor_id = ?, checkup_date = ?
                    WHERE checkup_id = ?
                    """;
                try (PreparedStatement checkupStmt = conn.prepareStatement(checkupSql)) {
                    checkupStmt.setString(1, saveCheckupRequest.getSuggestions());
                    checkupStmt.setString(2, saveCheckupRequest.getDiagnosis());
                    checkupStmt.setObject(3, newPrescriptionId); // setObject an toàn cho giá trị null
                    checkupStmt.setString(4, saveCheckupRequest.getNotes());
                    checkupStmt.setString(5, saveCheckupRequest.getStatus());
                    checkupStmt.setString(6, saveCheckupRequest.getCheckupType());
                    checkupStmt.setString(7, saveCheckupRequest.getConclusion());
                    checkupStmt.setObject(8, saveCheckupRequest.getReCheckupDate()); // setObject an toàn cho null
                    checkupStmt.setDouble(9, saveCheckupRequest.getCustomerWeight());
                    checkupStmt.setDouble(10, saveCheckupRequest.getCustomerHeight());
                    checkupStmt.setInt(11, saveCheckupRequest.getHeartBeat());
                    checkupStmt.setString(12, saveCheckupRequest.getBloodPressure());
                    checkupStmt.setInt(13, saveCheckupRequest.getDoctorUltrasoundId());
                    checkupStmt.setInt(14, saveCheckupRequest.getDoctorId());
                    checkupStmt.setLong(15, saveCheckupRequest.getCheckupDate());
                    checkupStmt.setInt(16, saveCheckupRequest.getCheckupId()); // WHERE clause
                    checkupStmt.executeUpdate();
                }
    
                // 5. XỬ LÝ DỊCH VỤ (SERVICES)
                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM CheckupService WHERE checkup_id = ?")) {
                    deleteStmt.setInt(1, saveCheckupRequest.getCheckupId());
                    deleteStmt.executeUpdate();
                }
                log.info("Cleared previous services for checkup_id: {}", saveCheckupRequest.getCheckupId());
    
                // TỐI ƯU: Dùng batch để chèn dịch vụ
                if (saveCheckupRequest.getServicePrescription() != null && saveCheckupRequest.getServicePrescription().length > 0) {
                    String serviceSql = "INSERT INTO CheckupService (checkup_id, service_id, quantity, total_cost, notes) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement serviceStmt = conn.prepareStatement(serviceSql)) {
                        for (String[] service : saveCheckupRequest.getServicePrescription()) {
                            serviceStmt.setInt(1, saveCheckupRequest.getCheckupId());
                            serviceStmt.setString(2, service[0]);
                            serviceStmt.setInt(3, Integer.parseInt(service[2]));
                            serviceStmt.setDouble(4, Double.parseDouble(service[4]));
                            serviceStmt.setString(5, service[5]);
                            serviceStmt.addBatch();
                        }
                        serviceStmt.executeBatch();
                    }
                }
    
                // 6. Hoàn tất và COMMIT giao dịch
                conn.commit();
                log.info("Successfully saved checkup transaction for checkup_id: {}", saveCheckupRequest.getCheckupId());
                broadcastQueueUpdate(saveCheckupRequest.getShift());
                UserUtil.sendPacket(currentUser.getSessionId(), new SaveCheckupRes(true, "Lưu thông tin khám bệnh thành công."));
    
            } catch (SQLException e) {
                // Nếu có lỗi, ROLLBACK toàn bộ giao dịch
                log.error("Error during save checkup transaction, rolling back.", e);
                conn.rollback();
                UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
            }
            // Không cần khối `finally` để reset auto-commit. HikariCP sẽ tự lo việc này.
    
        } catch (SQLException e) {
            // Lỗi này xảy ra nếu không thể lấy kết nối từ pool hoặc có lỗi khi rollback
            log.error("Critical error during database transaction management for SaveCheckupRequest.", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
      
      if (packet instanceof GetOrderInfoByCheckupReq getOrderInfoByCheckupReq) {
        log.debug("Received GetOrderInfoByCheckupReq for checkupId: {}", getOrderInfoByCheckupReq.getCheckupId());
        
        String checkupId = getOrderInfoByCheckupReq.getCheckupId();
        String[][] medicinePrescription = null;
        String[][] servicePrescription = null;
    
        // --- SỬA ĐỔI: Lấy thông tin đơn thuốc (Medicine Prescription) ---
        // Sử dụng try-with-resources để quản lý TẤT CẢ tài nguyên JDBC (Connection, PreparedStatement, ResultSet)
        String medSql = """
            SELECT M.med_id, M.med_name, OI.quantity_ordered, M.med_unit, OI.dosage,
                   OI.price_per_unit, OI.total_price, OI.notes, M.supplement, M.route
            FROM OrderItem OI
            JOIN Medicine M ON OI.med_id = M.med_id
            WHERE OI.checkup_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement medStmt = conn.prepareStatement(medSql)) {
            
            medStmt.setString(1, checkupId);
            try (ResultSet medRs = medStmt.executeQuery()) {
                ArrayList<String[]> medList = new ArrayList<>();
                while(medRs.next()) {
                    String[] med = new String[12];
                    med[0] = medRs.getString("med_id");
                    med[1] = medRs.getString("med_name");
                    med[2] = medRs.getString("quantity_ordered");
                    med[3] = medRs.getString("med_unit");
                    
                    // Parse dosage: "Sáng 1, Trưa 1, Chiều 1"
                    String dosage = medRs.getString("dosage");
                    String morning = "0", noon = "0", evening = "0";
                    if (dosage != null && !dosage.isEmpty()) {
                        String[] parts = dosage.split(", ");
                        for (String part : parts) {
                            String[] dosagePart = part.split(" ");
                            if (dosagePart.length == 2) {
                                if ("Sáng".equals(dosagePart[0])) morning = dosagePart[1];
                                else if ("Trưa".equals(dosagePart[0])) noon = dosagePart[1];
                                else if ("Chiều".equals(dosagePart[0])) evening = dosagePart[1];
                            }
                        }
                    }
                    med[4] = morning;
                    med[5] = noon;
                    med[6] = evening;
    
                    med[7] = medRs.getString("price_per_unit");
                    med[8] = medRs.getString("total_price");
                    med[9] = medRs.getString("notes");
                    med[10] = medRs.getString("supplement");
                    med[11] = medRs.getString("route");
                    medList.add(med);
                }
                medicinePrescription = medList.toArray(new String[0][]);
            }
        } catch (SQLException e) {
            log.error("Error getting medicine prescription for checkupId: " + checkupId, e);
            // Nếu có lỗi, medicinePrescription sẽ vẫn là null
        }
        
        // --- SỬA ĐỔI: Lấy thông tin dịch vụ (Service Prescription) ---
        String serSql = """
            SELECT S.service_id, S.service_name, CS.quantity,
                   S.service_cost, CS.total_cost, CS.notes
            FROM CheckupService CS
            JOIN Service S ON CS.service_id = S.service_id
            WHERE CS.checkup_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement serStmt = conn.prepareStatement(serSql)) {
    
            serStmt.setString(1, checkupId);
            try (ResultSet serRs = serStmt.executeQuery()) {
                ArrayList<String[]> serList = new ArrayList<>();
                while(serRs.next()) {
                    String[] ser = new String[6];
                    ser[0] = serRs.getString("service_id");
                    ser[1] = serRs.getString("service_name");
                    ser[2] = serRs.getString("quantity");
                    ser[3] = serRs.getString("service_cost");
                    ser[4] = serRs.getString("total_cost");
                    ser[5] = serRs.getString("notes");
                    serList.add(ser);
                }
                servicePrescription = serList.toArray(new String[0][]);
            }
        } catch (SQLException e) {
            log.error("Error getting service prescription for checkupId: " + checkupId, e);
            // Nếu có lỗi, servicePrescription sẽ vẫn là null
        }
        
        UserUtil.sendPacket(currentUser.getSessionId(), new GetOrderInfoByCheckupRes(medicinePrescription, servicePrescription));
        log.info("Sent order info for checkup {} to client", checkupId);
    }
    
    if (packet instanceof AddTemplateReq addTemplateReq) {
        log.debug("Received AddTemplateReq: {}", addTemplateReq);
        
        // --- SỬA ĐỔI: Sử dụng try-with-resources để đảm bảo an toàn và tự động đóng tài nguyên ---
        String sql = """
            INSERT INTO CheckupTemplate (template_gender, template_name, template_title, photo_num, 
                                         print_type, content, conclusion, suggestion, diagnosis, 
                                         visible, stt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement templateStmt = conn.prepareStatement(sql)) {
    
            templateStmt.setString(1, addTemplateReq.getTemplateGender());
            templateStmt.setString(2, addTemplateReq.getTemplateName());
            templateStmt.setString(3, addTemplateReq.getTemplateTitle());
            templateStmt.setString(4, addTemplateReq.getTemplateImageCount());
            templateStmt.setString(5, addTemplateReq.getTemplatePrintType());
            templateStmt.setString(6, addTemplateReq.getTemplateContent());
            templateStmt.setString(7, addTemplateReq.getTemplateConclusion());
            templateStmt.setString(8, addTemplateReq.getTemplateSuggestion());
            templateStmt.setString(9, addTemplateReq.getTemplateDiagnosis());
            templateStmt.setBoolean(10, addTemplateReq.isVisible());
            templateStmt.setInt(11, addTemplateReq.getStt());
            templateStmt.executeUpdate();
            
            log.info("Template saved successfully");
            UserUtil.sendPacket(currentUser.getSessionId(), new AddTemplateRes(true, "Template saved successfully"));
        } catch (SQLException e) {
            log.error("Error saving template", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
      if (packet instanceof GetAllTemplatesReq) {
        log.debug("Received GetAllTemplatesReq");
        
        // --- SỬA ĐỔI: Sử dụng try-with-resources để quản lý TẤT CẢ tài nguyên ---
        String sql = "SELECT * FROM CheckupTemplate ORDER BY stt"; // Thêm ORDER BY để sắp xếp
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            List<Template> templates = new ArrayList<>();
            while (rs.next()) {
                templates.add(new Template(
                    rs.getInt("template_id"),
                    rs.getString("template_gender"),
                    rs.getString("template_name"),
                    rs.getString("template_title"),
                    rs.getString("photo_num"),
                    rs.getString("print_type"),
                    rs.getString("content"),
                    rs.getString("conclusion"),
                    rs.getString("suggestion"),
                    rs.getString("diagnosis"),
                    rs.getBoolean("visible"),
                    rs.getInt("stt")
                ));
            }
            UserUtil.sendPacket(currentUser.getSessionId(), new GetAllTemplatesRes(templates));
    
        } catch (SQLException e) {
            log.error("Error fetching templates", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
    }
    
    if (packet instanceof EditTemplateReq editTemplateReq) {
        log.debug("Received EditTemplateReq for templateId: {}", editTemplateReq.getTemplate().getTemplateId());
        Template template = editTemplateReq.getTemplate();
        
        // --- SỬA ĐỔI: Sử dụng try-with-resources ---
        String sql = """
            UPDATE CheckupTemplate 
            SET template_gender = ?, template_name = ?, template_title = ?, 
                photo_num = ?, print_type = ?, content = ?, conclusion = ?, 
                suggestion = ?, diagnosis = ?, visible = ?, stt = ?
            WHERE template_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, template.getTemplateGender());
            stmt.setString(2, template.getTemplateName());
            stmt.setString(3, template.getTemplateTitle());
            stmt.setString(4, template.getPhotoNum());
            stmt.setString(5, template.getPrintType());
            stmt.setString(6, template.getContent());
            stmt.setString(7, template.getConclusion());
            stmt.setString(8, template.getSuggestion());
            stmt.setString(9, template.getDiagnosis());
            stmt.setBoolean(10, template.isVisible());
            stmt.setInt(11, template.getStt());
            stmt.setInt(12, template.getTemplateId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                log.info("Template {} updated successfully", template.getTemplateId());
                UserUtil.sendPacket(currentUser.getSessionId(), new EditTemplateRes(true, "Cập nhật mẫu thành công."));
            } else {
                log.warn("Template {} not found for update.", template.getTemplateId());
                UserUtil.sendPacket(currentUser.getSessionId(), new EditTemplateRes(false, "Không tìm thấy mẫu để cập nhật."));
            }
    
        } catch (SQLException e) {
            log.error("Error updating template {}", template.getTemplateId(), e);
            UserUtil.sendPacket(currentUser.getSessionId(), new EditTemplateRes(false, "Lỗi khi cập nhật mẫu: " + e.getMessage()));
        }
    }
    
    if (packet instanceof DeleteTemplateReq deleteTemplateReq) {
        log.debug("Received DeleteTemplateReq for templateId: {}", deleteTemplateReq.getTemplateId());
        
        // --- SỬA ĐỔI: Sử dụng try-with-resources ---
        String sql = "DELETE FROM CheckupTemplate WHERE template_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, deleteTemplateReq.getTemplateId());
            int affectedRows = stmt.executeUpdate();
    
            if (affectedRows > 0) {
                log.info("Template {} deleted successfully", deleteTemplateReq.getTemplateId());
                UserUtil.sendPacket(currentUser.getSessionId(), new DeleteTemplateRes(true, "Xóa mẫu thành công."));
            } else {
                log.warn("Template {} not found for deletion.", deleteTemplateReq.getTemplateId());
                UserUtil.sendPacket(currentUser.getSessionId(), new DeleteTemplateRes(false, "Không tìm thấy mẫu để xóa."));
            }
    
        } catch (SQLException e) {
            log.error("Error deleting template {}", deleteTemplateReq.getTemplateId(), e);
            UserUtil.sendPacket(currentUser.getSessionId(), new DeleteTemplateRes(false, "Lỗi khi xóa mẫu: " + e.getMessage()));
        }
    }
    
    if (packet instanceof UploadCheckupImageRequest uploadCheckupImageRequest) {
        log.info("Received UploadCheckupImageRequest for checkupId: {}", uploadCheckupImageRequest.getCheckupId());
    
        String checkupId = uploadCheckupImageRequest.getCheckupId();
        String originalFileName = uploadCheckupImageRequest.getFileName(); // Use the original name
        byte[] imageData = uploadCheckupImageRequest.getImageData();
    
        if (checkupId == null || checkupId.trim().isEmpty()) {
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "CheckupID không hợp lệ.", originalFileName));
            return;
        }
    
        try {
            // 1. Define the storage directory
            Path storageDir = Paths.get(Server.imageDbPath, checkupId.trim());
            Files.createDirectories(storageDir);
    
            // 2. Define the final file path using the original file name.
            // This preserves the original extension (jpg, png, etc.).
            Path filePath = storageDir.resolve(originalFileName);
    
            // 3. Write the raw bytes directly to the file.
            // This is much faster and safer than decoding and re-encoding.
            Files.write(filePath, imageData);
            log.info("Successfully saved original image to {}", filePath);
    
            // 4. Send a success response back to the client
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(true, "Tải ảnh lên thành công.", originalFileName));
    
            // 5. Asynchronously upload to Google Drive (your existing logic)
            uploadCheckupImageToGoogleDriveAsync(checkupId, originalFileName, imageData);
    
        } catch (IOException e) {
            log.error("Failed to save uploaded image for checkupId {}: {}", checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "Lỗi máy chủ khi lưu ảnh: " + e.getMessage(), originalFileName));
        } catch (Exception e) { // Catch any other unexpected errors
            log.error("An unexpected error occurred during image upload for checkupId {}: {}", checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "Lỗi không xác định: " + e.getMessage(), originalFileName));
        }
    }

    if (packet instanceof DeleteCheckupImageRequest deleteCheckupImageRequest) {
        log.info("Received DeleteCheckupImageRequest for checkupId: {}, fileName: {}", 
                deleteCheckupImageRequest.getCheckupId(), deleteCheckupImageRequest.getFileName());
    
        String checkupId = deleteCheckupImageRequest.getCheckupId();
        String fileName = deleteCheckupImageRequest.getFileName();
    
        if (checkupId == null || checkupId.trim().isEmpty()) {
            UserUtil.sendPacket(currentUser.getSessionId(), 
                new DeleteCheckupImageResponse(false, "CheckupID không hợp lệ.", fileName));
            return;
        }
    
        if (fileName == null || fileName.trim().isEmpty()) {
            UserUtil.sendPacket(currentUser.getSessionId(), 
                new DeleteCheckupImageResponse(false, "Tên file không hợp lệ.", fileName));
            return;
        }
    
        try {
            // 1. Define the local file path
            Path storageDir = Paths.get(Server.imageDbPath, checkupId.trim());
            Path filePath = storageDir.resolve(fileName);
    
            // 2. Delete the local file if it exists
            boolean localDeleted = false;
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                localDeleted = true;
                log.info("Successfully deleted local image: {}", filePath);
            } else {
                log.warn("Local file not found: {}", filePath);
            }
    
            // 3. Send success response to client immediately
            UserUtil.sendPacket(currentUser.getSessionId(), 
                new DeleteCheckupImageResponse(true, "Xóa ảnh thành công.", fileName));
    
            // 4. Asynchronously delete from Google Drive
            deleteCheckupImageFromGoogleDriveAsync(checkupId, fileName);
    
        } catch (IOException e) {
            log.error("Failed to delete image for checkupId {}: {}", checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), 
                new DeleteCheckupImageResponse(false, "Lỗi máy chủ khi xóa ảnh: " + e.getMessage(), fileName));
        } catch (Exception e) {
            log.error("An unexpected error occurred during image deletion for checkupId {}: {}", 
                    checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), 
                new DeleteCheckupImageResponse(false, "Lỗi không xác định: " + e.getMessage(), fileName));
        }
    }

      if (packet instanceof DeleteCheckupRequest deleteCheckupRequest) {
        log.info("Recieved delete checkup request for checkupId: {}", deleteCheckupRequest.getCheckupId());
    
        long checkupId = deleteCheckupRequest.getCheckupId();
        String checkupIdStr = String.valueOf(checkupId);

        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);

            // 1. Get drive_folder_id before deleting the checkup record
            String driveFolderId = null;
            try (PreparedStatement ps = connection.prepareStatement("SELECT drive_folder_id FROM main.Checkup WHERE checkup_id = ?")) {
                ps.setLong(1, checkupId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        driveFolderId = rs.getString("drive_folder_id");
                    }
                }
            }

            // 2. Delete local and cloud images/folders
            if (driveFolderId != null && !driveFolderId.trim().isEmpty()) {
                deleteCheckupImagesFromGoogleDriveAsync(driveFolderId);
            }
            deleteLocalCheckupImageFolder(checkupIdStr);
            
            // 3. Define the SQL DELETE statements in the correct order
            String deleteOrderItemsSQL = "DELETE FROM main.OrderItem WHERE prescription_id IN (SELECT prescription_id FROM main.MedicineOrder WHERE checkup_id = ?)";
            String deleteMedicineOrderSQL = "DELETE FROM main.MedicineOrder WHERE checkup_id = ?";
            String deleteCheckupServiceSQL = "DELETE FROM main.CheckupService WHERE checkup_id = ?";
            String deleteCheckupSQL = "DELETE FROM main.Checkup WHERE checkup_id = ?";

            // 4. Execute deletes in order
            try (PreparedStatement psOrderItems = connection.prepareStatement(deleteOrderItemsSQL)) {
                psOrderItems.setLong(1, checkupId);
                psOrderItems.executeUpdate();
            }

            try (PreparedStatement psMedicineOrder = connection.prepareStatement(deleteMedicineOrderSQL)) {
                psMedicineOrder.setLong(1, checkupId);
                psMedicineOrder.executeUpdate();
            }

            try (PreparedStatement psCheckupService = connection.prepareStatement(deleteCheckupServiceSQL)) {
                psCheckupService.setLong(1, checkupId);
                psCheckupService.executeUpdate();
            }

            try (PreparedStatement psCheckup = connection.prepareStatement(deleteCheckupSQL)) {
                psCheckup.setLong(1, checkupId);
                int rowsAffected = psCheckup.executeUpdate();
                if (rowsAffected == 0) {
                    log.warn("Warning: Checkup with ID " + checkupId + " not found.");
                }
            }

            // 5. If all statements executed without error, commit the transaction
            connection.commit();
            log.info("Successfully deleted checkup " + checkupId + " and all associated data.");
            broadcastQueueUpdate(deleteCheckupRequest.getShift());
            UserUtil.sendPacket(currentUser.getSessionId(), new DeleteCheckupResponse(true, "Xóa phiếu khám thành công."));

        } catch (SQLException e) {
            // 6. If any SQL error occurs, roll back the entire transaction
            log.info("SQL error during deletion. Rolling back transaction for checkup ID: " + checkupId);
            e.printStackTrace();
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    System.err.println("Failed to rollback transaction.");
                    ex.printStackTrace();
                }
            }
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        
        } catch (IOException e) {
            log.error("Failed to delete local image folder for checkupId {}: {}", checkupId, e.getMessage());
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    log.error("Failed to rollback transaction after IOException", ex);
                }
            }
            UserUtil.sendPacket(currentUser.getSessionId(), 
                new DeleteCheckupResponse(false, "Lỗi khi xóa thư mục ảnh: " + e.getMessage()));

        } finally {
            // 7. Always return the connection to the pool
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


      if (packet instanceof GetImagesByCheckupIdReq getImagesByCheckupIdReq) {
        String checkupId = getImagesByCheckupIdReq.getCheckupId();
        log.info("Received GetImagesByCheckupIdReq for checkupId: {}", checkupId);

        List<String> imageNames = new ArrayList<>();
        List<byte[]> imageDatas = new ArrayList<>();

        Path checkupDir = Paths.get(Server.imageDbPath, checkupId);
        if (Files.exists(checkupDir) && Files.isDirectory(checkupDir)) {
            try {
                List<Path> imagePaths = Files.list(checkupDir)
                    .filter(path -> {
                        String filename = path.toString().toLowerCase();
                        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".gif") || filename.endsWith(".bmp");
                    })
                    .collect(Collectors.toList());

                for (Path imagePath : imagePaths) {
                    try {
                        imageDatas.add(Files.readAllBytes(imagePath));
                        imageNames.add(imagePath.getFileName().toString());
                    } catch (IOException e) {
                        log.error("Failed to read image file: {}", imagePath, e);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to list images for checkupId: {}", checkupId, e);
            }
        } else {
            log.warn("Image directory not found for checkupId: {}", checkupId);
        }

        UserUtil.sendPacket(currentUser.getSessionId(), new GetImagesByCheckupIdRes(checkupId, imageNames, imageDatas));
        log.info("Sent {} images for checkupId: {}", imageDatas.size(), checkupId);
      }

      if (packet instanceof SyncCheckupImagesRequest syncCheckupImagesRequest) {
        String checkupId = syncCheckupImagesRequest.getCheckupId();
        log.info("Received SyncCheckupImagesRequest for checkupId: {}", checkupId);
    
        List<String> imageNames = new ArrayList<>();
        // No longer create a list for byte data: List<byte[]> imageDatas = new ArrayList<>();
        boolean success = false;
        String message = "";
    
        Path checkupDir = Paths.get(Server.imageDbPath, checkupId);
        if (Files.exists(checkupDir) && Files.isDirectory(checkupDir)) {
            try {
                // We ONLY collect the names, not the file content
                imageNames = Files.list(checkupDir)
                    .filter(path -> {
                        String filename = path.toString().toLowerCase();
                        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png") || filename.endsWith(".gif") || filename.endsWith(".bmp");
                    })
                    .map(path -> path.getFileName().toString()) // Get just the filename string
                    .collect(Collectors.toList());
    
                success = true;
                message = String.format("Successfully found %d image names for checkup %s", imageNames.size(), checkupId);
                log.info(message);
                
            } catch (IOException e) {
                log.error("Failed to list images for checkupId: {}", checkupId, e);
                message = "Failed to read image directory on server: " + e.getMessage();
            }
        } else {
            log.warn("Image directory not found for checkupId: {}", checkupId);
            message = "No images found for this checkup on server";
            success = true; // Not finding a directory is a successful "empty" response
        }
        
        // Note: imageDatas has been removed from the constructor
        UserUtil.sendPacket(currentUser.getSessionId(), new SyncCheckupImagesResponse(checkupId, imageNames, success, message));
        log.info("Sent sync response with {} image names for checkupId: {}", imageNames.size(), checkupId);
    }
    if (packet instanceof GetCheckupImageRequest getImageRequest) {
        String checkupId = getImageRequest.getCheckupId();
        String imageName = getImageRequest.getImageName();
        log.info("Received GetCheckupImageRequest for image '{}' in checkup {}", imageName, checkupId);
    
        byte[] imageData = null;
        boolean success = false;
        String message = "";
    
        try {
            Path checkupDir = Paths.get(Server.imageDbPath, checkupId);
            Path targetPath = checkupDir.resolve(imageName).normalize();
    
            // CRITICAL SECURITY CHECK: Prevent directory traversal attacks (e.g., "../../../etc/passwd")
            if (!targetPath.startsWith(checkupDir)) {
                throw new SecurityException("Attempted directory traversal attack: " + imageName);
            }
    
            if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
                // The potentially dangerous operation is now safely inside a try-catch block
                imageData = Files.readAllBytes(targetPath);
                success = true;
                message = "Successfully read image data.";
            } else {
                message = "Image not found on server: " + imageName;
                log.warn(message);
            }
        } catch (IOException e) {
            message = "IO error reading image file: " + e.getMessage();
            log.error("Failed to read image file '{}': {}", imageName, e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            // THIS IS THE KEY! We catch the crash and turn it into a safe error response.
            message = "Server ran out of memory trying to read image. File may be corrupted: " + imageName;
            log.error("OutOfMemoryError while reading image file '{}'. It is likely corrupted.", imageName, e);
        } catch (Exception e) {
            message = "An unexpected error occurred: " + e.getMessage();
            log.error("Unexpected error reading image file '{}': {}", imageName, e.getMessage(), e);
        }
    
        UserUtil.sendPacket(currentUser.getSessionId(), new GetCheckupImageResponse(checkupId, imageName, imageData, success, message));
    }
      if (packet instanceof GetRecheckUpListRequest getRecheckUpListRequest) {
        log.debug("Received GetRecheckUpListRequest");
        getRecheckUpList(currentUser.getSessionId());
      }

      if (packet instanceof AddRemindDateRequest addRemindDateRequest) {
        log.debug("Received AddRemindDateRequest for checkupId: {}", addRemindDateRequest.getCheckupId());
        
        // SỬA ĐỔI: Sử dụng try-with-resources để quản lý kết nối và statement an toàn
        String sql = "UPDATE Checkup SET remind_date = ? WHERE checkup_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, addRemindDateRequest.getCheckupId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                log.info("Updated remind_date for checkup_id: {}", addRemindDateRequest.getCheckupId());
                // Giả sử hàm getRecheckUpList đã được triển khai an toàn
                getRecheckUpList(currentUser.getSessionId()); 
            } else {
                log.warn("No checkup found with id {} to update remind_date.", addRemindDateRequest.getCheckupId());
            }
    
        } catch (SQLException e) {
            log.error("Error updating remind_date", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
      if (packet instanceof EmergencyRequest emergencyRequest) {
        log.info("Received EmergencyRequest from user_id: {}", currentUser.getUserId());
        String senderName = "Không xác định";

        // SỬA ĐỔI: Lấy tên người gửi một cách an toàn với try-with-resources
        String sql = "SELECT user_name FROM User WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, currentUser.getUserId());
            try (ResultSet rs = stmt.executeQuery()) {
          
                if (rs.next()) { 
                    senderName = rs.getString("user_name");
                } else {
                    log.warn("Could not find user_name for user_id: {}", currentUser.getUserId());
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching sender name for emergency, will use default.", e);
        }
        int maxCurId = SessionManager.getMaxSessionId();
        for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
          UserUtil.sendPacket(sessionId, new EmergencyResponse(senderName));
        }
      }
      if (packet instanceof GetCheckupDataRequest getCheckupDataRequest) {
        if (currentUser.getRole() != Role.ADMIN) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Processing GetCheckupDataRequest for page {} with all info.", getCheckupDataRequest.getPage());
    
        try (Connection conn = DatabaseManager.getConnection()) {
            String baseSelect = "SELECT a.checkup_id, a.checkup_date, c.customer_last_name, c.customer_first_name, " +
                    "d.doctor_first_name, d.doctor_last_name, a.suggestion, a.diagnosis, a.notes, a.status, a.customer_id, " +
                    "c.customer_number, c.customer_address, a.customer_weight, a.customer_height, c.customer_gender, c.customer_dob, " +
                    "a.checkup_type, a.conclusion, a.reCheckupDate, c.cccd_ddcn, a.heart_beat, a.blood_pressure, c.drive_url, a.doctor_ultrasound_id, a.queue_number ";
            
            String fromAndJoin = "FROM checkup AS a " +
                                 "JOIN customer AS c ON a.customer_id = c.customer_id " +
                                 "JOIN Doctor AS d ON a.doctor_id = d.doctor_id ";
    
            StringBuilder whereClause = new StringBuilder();
            java.util.List<Object> params = new java.util.ArrayList<>();
            boolean hasWhere = false;
    
            String checkupIdSearch = getCheckupDataRequest.getCheckupIdSearch();
            if (checkupIdSearch != null && !checkupIdSearch.trim().isEmpty()) {
                whereClause.append("WHERE a.checkup_id = ?");
                try {
                    params.add(Integer.parseInt(checkupIdSearch.trim()));
                } catch (NumberFormatException e) {
                    params.add(-1); 
                }
                hasWhere = true;
            } 
            else if (getCheckupDataRequest.getSearchTerm() != null && !getCheckupDataRequest.getSearchTerm().trim().isEmpty()) {
                // SỬA LỖI: SQLite dùng || để nối chuỗi, không phải CONCAT()
                whereClause.append("WHERE (LOWER(c.customer_first_name) LIKE ? OR LOWER(c.customer_last_name) LIKE ? OR " +
                                   "LOWER(c.customer_last_name || ' ' || c.customer_first_name) LIKE ? OR c.customer_number LIKE ?)");
                String searchTerm = "%" + getCheckupDataRequest.getSearchTerm().toLowerCase().trim() + "%";
                params.add(searchTerm); 
                params.add(searchTerm); 
                params.add(searchTerm); 
                params.add(searchTerm);
                hasWhere = true;
            }
    
            if (getCheckupDataRequest.getFromDate() != null && getCheckupDataRequest.getToDate() != null) {
                whereClause.append(hasWhere ? " AND " : "WHERE ").append("a.checkup_date BETWEEN ? AND ?");
                params.add(getCheckupDataRequest.getFromDate());
                params.add(getCheckupDataRequest.getToDate());
                hasWhere = true;
            }
    
            if (getCheckupDataRequest.getDoctorId() != null && getCheckupDataRequest.getDoctorId() > 0) {
                whereClause.append(hasWhere ? " AND " : "WHERE ").append("a.doctor_id = ?");
                params.add(getCheckupDataRequest.getDoctorId());
            }
    
            int totalRecords = 0;
            String countQuery = "SELECT COUNT(*) " + fromAndJoin + whereClause;
            try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
                for (int i = 0; i < params.size(); i++) {
                    countStmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet countRs = countStmt.executeQuery()) {
                    if (countRs.next()) {
                        totalRecords = countRs.getInt(1);
                    }
                }
            }
    
            int pageSize = getCheckupDataRequest.getPageSize();
            int currentPage = getCheckupDataRequest.getPage();
            int totalPages = (totalRecords == 0) ? 0 : (int) Math.ceil((double) totalRecords / pageSize);
            int offset = (currentPage - 1) * pageSize;
    
            String finalQuery = baseSelect + fromAndJoin + whereClause + " ORDER BY a.checkup_date DESC, a.checkup_id DESC LIMIT ? OFFSET ?";
            java.util.List<String[]> resultList = new java.util.ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(finalQuery)) {
                int paramIndex = 1;
                for (Object param : params) {
                    stmt.setObject(paramIndex++, param);
                }
                stmt.setInt(paramIndex++, pageSize);
                stmt.setInt(paramIndex, offset);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String checkupId = rs.getString("checkup_id");
                        String checkupDate = String.valueOf(rs.getLong("checkup_date"));
                        String customerLastName = rs.getString("customer_last_name");
                        String customerFirstName = rs.getString("customer_first_name");
                        String doctorName = "BS. " + rs.getString("doctor_last_name") + " " + rs.getString("doctor_first_name");
                        String suggestion = rs.getString("suggestion");
                        String diagnosis = rs.getString("diagnosis");
                        String notes = rs.getString("notes");
                        String status = rs.getString("status");
                        String customerId = rs.getString("customer_id");
                        String customerNumber = rs.getString("customer_number");
                        String customerAddress = rs.getString("customer_address");
                        String customerWeight = String.valueOf(rs.getBigDecimal("customer_weight"));
                        String customerHeight = String.valueOf(rs.getBigDecimal("customer_height"));
                        String customerGender = rs.getString("customer_gender");
                        String customerDob = String.valueOf(rs.getLong("customer_dob"));
                        String checkupType = rs.getString("checkup_type");
                        String conclusion = rs.getString("conclusion");
                        String reCheckupDate = String.valueOf(rs.getLong("reCheckupDate"));
                        String cccdDdcn = rs.getString("cccd_ddcn");
                        String heartBeat = String.valueOf(rs.getInt("heart_beat"));
                        String bloodPressure = rs.getString("blood_pressure");
                        String driveUrl = rs.getString("drive_url");
                        String doctorUltrasoundId = String.valueOf(rs.getInt("doctor_ultrasound_id"));
                        String queueNumber = String.valueOf(rs.getInt("queue_number"));
    
                        resultList.add(new String[]{
                            checkupId != null ? checkupId : "",
                            checkupDate != null ? checkupDate : "0",
                            customerLastName != null ? customerLastName : "",
                            customerFirstName != null ? customerFirstName : "",
                            doctorName,
                            suggestion != null ? suggestion : "",
                            diagnosis != null ? diagnosis : "",
                            notes != null ? notes : "",
                            status != null ? status : "",
                            customerId != null ? customerId : "",
                            customerNumber != null ? customerNumber : "",
                            customerAddress != null ? customerAddress : "",
                            customerWeight != null ? customerWeight : "0.0",
                            customerHeight != null ? customerHeight : "0.0",
                            customerGender != null ? customerGender : "",
                            customerDob != null ? customerDob : "0",
                            checkupType != null ? checkupType : "",
                            conclusion != null ? conclusion : "",
                            reCheckupDate != null ? reCheckupDate : "0",
                            cccdDdcn != null ? cccdDdcn : "",
                            heartBeat != null ? heartBeat : "0",
                            bloodPressure != null ? bloodPressure : "0/0",
                            driveUrl != null ? driveUrl : "",
                            doctorUltrasoundId != null ? doctorUltrasoundId : "0",
                            queueNumber != null ? queueNumber : "0"
                        });
                    }
                }
            }
            
            String[][] resultArray = resultList.toArray(new String[0][]);
            UserUtil.sendPacket(currentUser.getSessionId(), new GetCheckupDataResponse(resultArray, totalRecords, currentPage, totalPages, pageSize));
            
        } catch (SQLException e) {
            log.error("Error processing GetCheckupDataRequest", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new GetCheckupDataResponse(new String[0][0], 0, 1, 0, getCheckupDataRequest.getPageSize()));
        }
      }
      
      
      if (packet instanceof SimpleMessageRequest simpleMessageRequest) {
        log.info("Received SimpleMessageRequest from {}", simpleMessageRequest.getSenderName());
        SimpleMessageResponse response = new SimpleMessageResponse(simpleMessageRequest.getSenderName(), simpleMessageRequest.getMessage());
        int maxCurId = SessionManager.getMaxSessionId();
        for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
          UserUtil.sendPacket(sessionId, response);
        }
        log.info("Sent SimpleMessageResponse to all sessions");
      }
      if (packet instanceof AddMedicineRequest addMedicineRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received AddMedicineRequest for medicine: {}", addMedicineRequest.getName());
    
        String sql = """
            INSERT INTO Medicine (med_name, med_company, med_description, med_unit, 
                                  med_selling_price, preferred_note, supplement, deleted, route) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, addMedicineRequest.getName());
            stmt.setString(2, addMedicineRequest.getCompany());
            stmt.setString(3, addMedicineRequest.getDescription());
            stmt.setString(4, addMedicineRequest.getUnit());
            stmt.setDouble(5, addMedicineRequest.getPrice());
            stmt.setString(6, addMedicineRequest.getPreferredNote());
            stmt.setBoolean(7, addMedicineRequest.getSupplement());
            stmt.setBoolean(8, addMedicineRequest.getDeleted());
            stmt.setString(9, addMedicineRequest.getRoute());
            
            stmt.executeUpdate();
            log.info("Added medicine successfully: {}", addMedicineRequest.getName());
            getMedInfo(currentUser.getSessionId());
    
        } catch (SQLException e) {
            log.error("Error adding medicine", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
      if (packet instanceof EditMedicineRequest editMedicineRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received EditMedicineRequest for medicine ID: {}", editMedicineRequest.getId());
    
        // SỬA ĐỔI: Sử dụng try-with-resources để quản lý kết nối và statement an toàn
        String sql = """
            UPDATE Medicine 
            SET med_name = ?, med_company = ?, med_description = ?, med_unit = ?, 
                med_selling_price = ?, preferred_note = ?, supplement = ?, deleted = ?, route = ? 
            WHERE med_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, editMedicineRequest.getName());
            stmt.setString(2, editMedicineRequest.getCompany());
            stmt.setString(3, editMedicineRequest.getDescription());
            stmt.setString(4, editMedicineRequest.getUnit());
            stmt.setDouble(5, editMedicineRequest.getPrice());
            stmt.setString(6, editMedicineRequest.getPreferredNote());
            stmt.setBoolean(7, editMedicineRequest.getSupplement());
            stmt.setBoolean(8, editMedicineRequest.getDeleted());
            stmt.setString(9, editMedicineRequest.getRoute());
            stmt.setString(10, editMedicineRequest.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                log.info("Updated medicine successfully: {}", editMedicineRequest.getName());
                // Giả sử hàm getMedInfo đã được triển khai an toàn
                getMedInfo(currentUser.getSessionId());
            } else {
                log.warn("No medicine found with ID {} to update.", editMedicineRequest.getId());
            }
    
        } catch (SQLException e) {
            log.error("Error updating medicine", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
      if (packet instanceof AddServiceRequest addServiceRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received AddServiceRequest for service: {}", addServiceRequest.getName());
    
        String sql = "INSERT INTO Service (service_name, service_cost, deleted) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, addServiceRequest.getName());
            stmt.setDouble(2, addServiceRequest.getPrice());
            stmt.setBoolean(3, addServiceRequest.getDeleted());
            stmt.executeUpdate();
            
            log.info("Added service successfully: {}", addServiceRequest.getName());
            // Giả sử hàm getSerInfo đã được triển khai an toàn
            getSerInfo(currentUser.getSessionId());
    
        } catch (SQLException e) {
            log.error("Error adding service", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
    }
    
    if (packet instanceof EditServiceRequest editServiceRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received EditServiceRequest for service ID: {}", editServiceRequest.getId());
    
        String sql = "UPDATE Service SET service_name = ?, service_cost = ?, deleted = ? WHERE service_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, editServiceRequest.getName());
            stmt.setDouble(2, editServiceRequest.getPrice());
            stmt.setBoolean(3, editServiceRequest.getDeleted());
            stmt.setString(4, editServiceRequest.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                log.info("Updated service successfully: {}", editServiceRequest.getName());
                getSerInfo(currentUser.getSessionId());
            } else {
                log.warn("No service found with ID {} to update.", editServiceRequest.getId());
            }
    
        } catch (SQLException e) {
            log.error("Error updating service", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }

      if (packet instanceof GetAllUserInfoRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received GetAllUserInfoRequest");
        // Gọi hàm trợ giúp đã được viết lại một cách an toàn
        getAllUser(currentUser.getSessionId());
    }
    
    if (packet instanceof AddUserRequest addUserRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received AddUserRequest for username: {}", addUserRequest.getUserName());
    
        String sql = "INSERT INTO User (user_name, password, last_name, first_name, role_name, deleted) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, addUserRequest.getUserName());
            stmt.setString(2, addUserRequest.getPassword()); // Cân nhắc mã hóa mật khẩu trước khi lưu
            stmt.setString(3, addUserRequest.getLastName());
            stmt.setString(4, addUserRequest.getFirstName());
            stmt.setString(5, addUserRequest.getRole());
            stmt.setBoolean(6, addUserRequest.getDeleted());
            
            stmt.executeUpdate();
            
            log.info("Added user successfully: {}", addUserRequest.getUserName());
            getAllUser(currentUser.getSessionId());
    
        } catch (SQLException e) {
            // Xử lý lỗi nếu username đã tồn tại
            if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: User.user_name")) {
                 log.warn("Attempted to add a user with a duplicate username: {}", addUserRequest.getUserName());
                 // Bạn có thể gửi một gói tin lỗi cụ thể hơn ở đây
                 UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.USERNAME_EXISTS));
            } else {
                log.error("Error adding user", e);
                UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
            }
        }
      }

      if (packet instanceof EditUserRequest editUserRequest) {
        if (!currentUser.getRole().equals(Role.ADMIN)) {
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.ACCESS_DENIED));
            return;
        }
        log.info("Received EditUserRequest for user ID: {}", editUserRequest.getId());
    
        // SỬA ĐỔI: Sử dụng try-with-resources cho kết nối và statement
        String sql;
        boolean isPasswordIncluded;
    
        // Kiểm tra xem client có gửi mật khẩu mới không. Nếu không, không cập nhật mật khẩu.
        if (editUserRequest.getPassword() != null && !editUserRequest.getPassword().isEmpty()) {
            sql = "UPDATE User SET user_name = ?, password = ?, last_name = ?, first_name = ?, role_name = ?, deleted = ? WHERE user_id = ?";
            isPasswordIncluded = true;
        } else {
            sql = "UPDATE User SET user_name = ?, last_name = ?, first_name = ?, role_name = ?, deleted = ? WHERE user_id = ?";
            isPasswordIncluded = false;
        }
    
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
    
            if (isPasswordIncluded) {
                stmt.setString(1, editUserRequest.getUserName());
                stmt.setString(2, editUserRequest.getPassword()); // Cân nhắc mã hóa mật khẩu
                stmt.setString(3, editUserRequest.getLastName());
                stmt.setString(4, editUserRequest.getFirstName());
                stmt.setString(5, editUserRequest.getRole());
                stmt.setBoolean(6, editUserRequest.getDeleted());
                stmt.setInt(7, Integer.parseInt(editUserRequest.getId())); // SỬA LỖI: Chuyển ID sang kiểu int
            } else {
                stmt.setString(1, editUserRequest.getUserName());
                stmt.setString(2, editUserRequest.getLastName());
                stmt.setString(3, editUserRequest.getFirstName());
                stmt.setString(4, editUserRequest.getRole());
                stmt.setBoolean(5, editUserRequest.getDeleted());
                stmt.setInt(6, Integer.parseInt(editUserRequest.getId())); // SỬA LỖI: Chuyển ID sang kiểu int
            }
            
            int affectedRows = stmt.executeUpdate();
    
            if (affectedRows > 0) {
                log.info("Updated user successfully: {}", editUserRequest.getUserName());
                getAllUser(currentUser.getSessionId());
            } else {
                log.warn("No user found with ID {} to update.", editUserRequest.getId());
            }
    
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format for EditUserRequest: {}", editUserRequest.getId());
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.INVALID_INPUT));
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: User.user_name")) {
                 log.warn("Attempted to update a user with a duplicate username: {}", editUserRequest.getUserName());
                 UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.USERNAME_EXISTS));
            } else {
                log.error("Error updating user", e);
                UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
            }
        }
      }
  }
}


  private void getAllUser(int sessionId) {
    log.debug("Fetching all users for session: {}", sessionId);
    // SỬA LỖI BẢO MẬT: Đã loại bỏ cột 'password' khỏi câu truy vấn
    String sql = "SELECT user_id, user_name, last_name, password, first_name, role_name, deleted FROM User ORDER BY user_id";
    
    // SỬA LỖI QUAN TRỌNG: Sử dụng try-with-resources để quản lý tài nguyên an toàn
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
        
        // GIỮ NGUYÊN LOGIC GỐC: Xây dựng mảng String[][] qua các bước join và split
        ArrayList<String> resultList = new ArrayList<>();
        while (rs.next()) {
            String userId = rs.getString("user_id");
            String userName = rs.getString("user_name");
            String password = rs.getString("password");
            String lastName = rs.getString("last_name");
            String firstName = rs.getString("first_name");
            String role = rs.getString("role_name");
            String deleted = String.valueOf(rs.getBoolean("deleted")); // Lấy giá trị boolean an toàn

            // Mật khẩu đã được cố ý loại bỏ khỏi câu truy vấn để bảo mật
            String result = String.join("|", userId, userName, lastName, firstName, password, role, deleted);
            resultList.add(result);
        }

        String[] resultString = resultList.toArray(new String[0]);
        String[][] resultArray = new String[resultString.length][];
        for (int i = 0; i < resultString.length; i++) {
            resultArray[i] = resultString[i].split("\\|");
        }
        
        UserUtil.sendPacket(sessionId, new GetAllUserInfoResponse(resultArray));
        
    } catch (SQLException e) {
        log.error("Error fetching all users", e);
        // SỬA LỖI: Gửi gói tin lỗi thay vì làm sập server
        UserUtil.sendPacket(sessionId, new ErrorResponse(Error.SQL_EXCEPTION));
    }
  }

    /**
   * Lấy danh sách tất cả dịch vụ và gửi cho một session cụ thể.
   *
   * @param sessionId ID của session sẽ nhận dữ liệu.
   */
  private void getSerInfo(int sessionId) {
    log.debug("Fetching all services for session: {}", sessionId);
    String sql = "SELECT service_id, service_name, service_cost, deleted FROM Service";

    // SỬA LỖI QUAN TRỌNG: Sử dụng try-with-resources để quản lý tài nguyên an toàn
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

        // GIỮ NGUYÊN LOGIC GỐC: Xây dựng mảng String[][] qua các bước join và split
        ArrayList<String> resultList = new ArrayList<>();
        while (rs.next()) {
            String serId = rs.getString("service_id");
            String serName = rs.getString("service_name");
            String serPrice = rs.getString("service_cost");
            // Lấy giá trị boolean một cách an toàn và chuyển thành String
            String deleted = rs.getBoolean("deleted") ? "1" : "0";

            String result = String.join("|", serId, serName, serPrice, deleted);
            resultList.add(result);
        }

        String[] resultString = resultList.toArray(new String[0]);
        String[][] resultArray = new String[resultString.length][];
        for (int i = 0; i < resultString.length; i++) {
            resultArray[i] = resultString[i].split("\\|");
        }
        log.debug("service info: {}", resultArray);
        UserUtil.sendPacket(sessionId, new GetSerInfoResponse(resultArray));

    } catch (SQLException e) {
        log.error("Error fetching services", e);
        // SỬA LỖI: Gửi gói tin lỗi thay vì làm sập server
        UserUtil.sendPacket(sessionId, new ErrorResponse(Error.SQL_EXCEPTION));
    }
  }

  /**
   * Lấy danh sách tất cả các loại thuốc và gửi cho một session cụ thể.
   *
   * @param sessionId ID của session sẽ nhận dữ liệu.
   */
  private void getMedInfo(int sessionId) {
    log.debug("Fetching all medicines for session: {}", sessionId);
    String sql = """
        SELECT med_id, med_name, med_company, med_description, med_unit, 
              med_selling_price, preferred_note, supplement, deleted, route 
        FROM Medicine 
        ORDER BY med_id
        """;
    
    // SỬA LỖI QUAN TRỌNG: Sử dụng try-with-resources để quản lý tài nguyên an toàn
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
        
        // GIỮ NGUYÊN LOGIC GỐC: Xây dựng mảng String[][] qua các bước join và split
        ArrayList<String> resultList = new ArrayList<>();
        while (rs.next()) {
            String medId = rs.getString("med_id");
            String medName = rs.getString("med_name");
            String medCompany = rs.getString("med_company");
            String medDescription = rs.getString("med_description");
            String medUnit = rs.getString("med_unit");
            String medSellingPrice = rs.getString("med_selling_price");
            String preferredNote = rs.getString("preferred_note");
            String route = rs.getString("route");

            // Lấy giá trị boolean một cách an toàn và chuyển thành String "1" hoặc "0"
            String supplement = rs.getBoolean("supplement") ? "1" : "0";
            String deleted = rs.getBoolean("deleted") ? "1" : "0";

            String result = String.join("|", 
                medId, 
                medName, 
                medCompany != null ? medCompany : "", 
                medDescription != null ? medDescription : "", 
                medUnit,
                medSellingPrice, 
                preferredNote != null ? preferredNote : "", 
                supplement, 
                deleted, 
                route != null ? route : ""
            );
            resultList.add(result);
        }

        String[] resultString = resultList.toArray(new String[0]);
        String[][] resultArray = new String[resultString.length][];
        for (int i = 0; i < resultString.length; i++) {
            // Giữ nguyên tham số -1 để đảm bảo số cột chính xác
            resultArray[i] = resultString[i].split("\\|", -1);
        }

        UserUtil.sendPacket(sessionId, new GetMedInfoResponse(resultArray));

    } catch (SQLException e) {
        log.error("Error fetching medicines", e);
        // SỬA LỖI: Gửi gói tin lỗi thay vì làm sập server
        UserUtil.sendPacket(sessionId, new ErrorResponse(Error.SQL_EXCEPTION));
    }
  }

  /**
   * Lấy danh sách bệnh nhân cần tái khám trong vòng 7 ngày tới và gửi cho một session cụ thể.
   *
   * @param sessionId ID của session sẽ nhận dữ liệu.
   */
  private void getRecheckUpList(int sessionId) {
    log.debug("Fetching re-checkup list for session: {}", sessionId);

    // CẢI TIẾN: Sử dụng múi giờ cụ thể ("Asia/Ho_Chi_Minh") thay vì múi giờ mặc định của server.
    // Điều này đảm bảo tính nhất quán dù server chạy ở bất kỳ đâu.
    ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
    
    // Sử dụng java.time API để tính toán ngày tháng một cách rõ ràng và an toàn.
    long fromDate = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli();
    // Lấy đến cuối ngày thứ 7 kể từ hôm nay để bao trọn 7 ngày.
    long toDate = LocalDate.now(zoneId).plusDays(7).atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli();

    String sql = """
        SELECT c.customer_last_name, c.customer_first_name, c.customer_number, 
              ch.reCheckupDate, ch.remind_date, ch.checkup_id
        FROM Checkup ch
        JOIN Customer c ON ch.customer_id = c.customer_id
        WHERE ch.reCheckupDate IS NOT NULL AND ch.reCheckupDate BETWEEN ? AND ?
        ORDER BY ch.reCheckupDate ASC
        """;

    // SỬA LỖI QUAN TRỌNG: Sử dụng try-with-resources để quản lý tài nguyên an toàn
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setLong(1, fromDate);
        stmt.setLong(2, toDate);
        
        try (ResultSet rs = stmt.executeQuery()) {
            List<String[]> results = new ArrayList<>();
            
            // CẢI TIẾN: Tạo đối tượng formatter một lần ngoài vòng lặp để tăng hiệu năng.
            // Sử dụng DateTimeFormatter (thread-safe) thay cho SimpleDateFormat.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(zoneId);

            while (rs.next()) {
                String[] row = new String[5];
                row[0] = rs.getString("customer_last_name") + " " + rs.getString("customer_first_name");
                row[1] = rs.getString("customer_number");

                // Câu lệnh SQL đã đảm bảo reCheckupDate không null
                long recheckTimestamp = rs.getLong("reCheckupDate");
                row[2] = formatter.format(Instant.ofEpochMilli(recheckTimestamp));

                // Xử lý an toàn cho trường remind_date có thể là null
                long remindTimestamp = rs.getLong("remind_date");
                if (rs.wasNull() || remindTimestamp == 0) {
                    row[3] = "Chưa đặt"; // Hoặc có thể để là ""
                } else {
                    row[3] = formatter.format(Instant.ofEpochMilli(remindTimestamp));
                }
                
                row[4] = rs.getString("checkup_id");
                results.add(row);
            }
            
            UserUtil.sendPacket(sessionId, new GetRecheckUpListResponse(results.toArray(new String[0][])));
        }
        
    } catch (SQLException e) {
        log.error("Error fetching recheck-up list", e);
        // Giữ nguyên logic xử lý lỗi đã tốt của bạn
        UserUtil.sendPacket(sessionId, new ErrorResponse(Error.SQL_EXCEPTION));
    }
  }
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof IOException) {
      var user = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
      if (user != null) {
        ServerDashboard.getInstance().addLog("Client disconnected: Session " + user.getSessionId());
      }
      SessionManager.onUserDisconnect(ctx.channel());
    } else {
      log.error("ERROR: ", cause);
      ServerDashboard.getInstance().addLog("Error: " + cause.getMessage());
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent event) {
      if (event.state() == IdleState.READER_IDLE) {
        try {
          var user = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
          if (user != null) {
            ServerDashboard.getInstance().addLog("Client timed out: Session " + user.getSessionId());
          }
          SessionManager.onUserDisconnect(ctx.channel());
              if (user != null) {
        user.resetAuthentication();
      }
          ctx.channel().close();
        } catch (Exception e) {
          ServerDashboard.getInstance().addLog("Error during client timeout: " + e.getMessage());
        }
      }
    } else if (evt instanceof HandshakeComplete) {
      int SessionId = SessionManager.onUserLogin(ctx.channel());
      log.info("Session {} logged in", SessionId);
      ServerDashboard.getInstance().addLog("New client connected: Session " + SessionId);
      ServerDashboard.incrementClients();

      UserUtil.sendPacket(SessionId, new HandshakeCompleteResponse());
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  /**
   * Creates a Google Drive folder for a patient asynchronously and updates the drive_url in database.
   * This method runs in a background thread to avoid blocking the patient creation response.
   */
  private void createPatientGoogleDriveFolderAsync(int patientId, String patientLastName, String patientFirstName) {
    // Run in background thread to avoid blocking
    new Thread(() -> {
      try {
        // Check if Google Drive is connected
        if (!Server.isGoogleDriveConnected()) {
          log.info("Google Drive not connected - skipping folder creation for patient {}", patientId);
          return;
        }

        log.info("Creating Google Drive folder for patient {} ({} {})", patientId, patientLastName, patientFirstName);
        
        // Create English name (without Patient_ prefix since createPatientFolder will add it)
        String fullName = (patientLastName + " " + patientFirstName).trim();
        String englishPatientName = TextUtils.vietnameseToEnglishName(fullName);
        
        // Create patient folder in Google Drive
        String patientFolderId = Server.getGoogleDriveService().createPatientFolder(
            String.valueOf(patientId), 
            englishPatientName
        );
        
        // Make the folder public and get sharing URL
        String folderUrl = Server.getGoogleDriveService().getFolderSharingUrl(patientFolderId);
        
        // Update both drive_url and drive_folder_id in database
        updatePatientDriveInfo(patientId, folderUrl, patientFolderId);
        
        log.info("Google Drive folder created successfully for patient {}: {}", patientId, folderUrl);
        
        // Notify dashboard
        if (ServerDashboard.getInstance() != null) {
          ServerDashboard.getInstance().addLog(
            String.format("Created Google Drive folder for patient %d: %s", patientId, englishPatientName)
          );
        }
        
      } catch (Exception e) {
        log.error("Failed to create Google Drive folder for patient {}: {}", patientId, e.getMessage());
        
        // Notify dashboard about the error
        if (ServerDashboard.getInstance() != null) {
          ServerDashboard.getInstance().addLog(
            String.format("Failed to create Google Drive folder for patient %d: %s", patientId, e.getMessage())
          );
        }
      }
    }).start();
  }

  /**
   * Updates both the drive_url and drive_folder_id columns for a patient in the database.
   */
  private void updatePatientDriveInfo(int patientId, String driveUrl, String driveFolderId) throws SQLException {
    String sql = "UPDATE Customer SET drive_url = ?, drive_folder_id = ? WHERE customer_id = ?";

    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement updateStmt = conn.prepareStatement(sql)) {

        updateStmt.setString(1, driveUrl);
        updateStmt.setString(2, driveFolderId);
        updateStmt.setInt(3, patientId);

        int rowsUpdated = updateStmt.executeUpdate();

        if (rowsUpdated > 0) {
            log.info("Updated Google Drive info for patient {}: URL={}, FolderID={}", patientId, driveUrl, driveFolderId);
        } else {
            log.warn("Could not find patient with ID {} to update Google Drive info.", patientId);
        }

    } catch (SQLException e) {
        log.error("Failed to update Google Drive info for patient {}: {}", patientId, e.getMessage());
        throw e;
    }
  }

  /**
   * Tạo thư mục Google Drive cho một lần khám trong một luồng nền (bất đồng bộ).
   * Phiên bản này sử dụng CompletableFuture cho xử lý bất đồng bộ hiện đại và JDBC an toàn.
   *
   * @param checkupId  ID của lần khám.
   * @param customerId ID của bệnh nhân liên quan.
   */
  private void createCheckupGoogleDriveFolderAsync(int checkupId, int customerId) {
    // Sử dụng CompletableFuture để xử lý bất đồng bộ, hiện đại hơn new Thread().
    CompletableFuture.runAsync(() -> {
        try {
            if (!Server.isGoogleDriveConnected()) {
                log.info("Google Drive not connected - skipping checkup folder creation for checkup {}", checkupId);
                return;
            }

            // Lấy thông tin bệnh nhân từ CSDL một cách an toàn
            String patientLastName = "";
            String patientFirstName = "";
            String patientDriveFolderId = null;

            String sql = "SELECT customer_last_name, customer_first_name, drive_folder_id FROM Customer WHERE customer_id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, customerId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        patientLastName = rs.getString("customer_last_name");
                        patientFirstName = rs.getString("customer_first_name");
                        patientDriveFolderId = rs.getString("drive_folder_id");
                    }
                }
            }

            if (patientDriveFolderId == null || patientDriveFolderId.trim().isEmpty()) {
                log.warn("Patient {} has no Google Drive folder ID - cannot create checkup folder", customerId);
                return;
            }

            log.info("Creating Google Drive checkup folder for checkup {} (patient: {} {})", checkupId, patientLastName, patientFirstName);
            
            // Giả sử các hàm này đã được triển khai và hoạt động đúng
            String checkupFolderName = TextUtils.createCheckupFolderNameWithId(checkupId, patientLastName, patientFirstName);
            String checkupFolderId = createCheckupFolderDirectly(patientDriveFolderId, checkupFolderName);
            String folderUrl = Server.getGoogleDriveService().getFolderSharingUrl(checkupFolderId);
            
            // Gọi hàm cập nhật an toàn
            updateCheckupDriveInfo(checkupId, folderUrl, checkupFolderId);
            
            log.info("Google Drive checkup folder created successfully for checkup {}: {}", checkupId, folderUrl);
            
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().addLog(
                    String.format("Created Google Drive checkup folder for checkup %d: %s", checkupId, checkupFolderName)
                );
            }

        } catch (Exception e) {
            log.error("Failed to create Google Drive checkup folder for checkup {}: {}", checkupId, e.getMessage(), e);
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().addLog(
                    String.format("Failed to create Google Drive checkup folder for checkup %d: %s", checkupId, e.getMessage())
                );
            }
        }
    });
  }

  /**
   * Creates a checkup folder directly under patient folder using Google Drive API
   */
  private String createCheckupFolderDirectly(String parentFolderId, String folderName) throws Exception {
    return Server.getGoogleDriveService().createFolderUnderParent(parentFolderId, folderName);
  }

  /**
   * Cập nhật thông tin Google Drive (drive_url và drive_folder_id) cho một lần khám trong CSDL.
   *
   * @param checkupId     ID của lần khám cần cập nhật.
   * @param driveUrl      URL của thư mục Google Drive.
   * @param driveFolderId ID của thư mục Google Drive.
   * @throws SQLException nếu có lỗi truy cập cơ sở dữ liệu xảy ra.
   */
  private void updateCheckupDriveInfo(int checkupId, String driveUrl, String driveFolderId) throws SQLException {
    String sql = "UPDATE Checkup SET drive_url = ?, drive_folder_id = ? WHERE checkup_id = ?";
    
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, driveUrl);
        stmt.setString(2, driveFolderId);
        stmt.setInt(3, checkupId);
        
        int rowsUpdated = stmt.executeUpdate();
        if (rowsUpdated == 0) {
            log.warn("Could not find checkup with ID {} to update Google Drive info.", checkupId);
        }
    }
  }

  /**
   * Tải ảnh của lần khám lên Google Drive một cách bất đồng bộ.
   *
   * @param checkupId  ID của lần khám (dưới dạng String).
   * @param fileName   Tên của tệp ảnh.
   * @param imageData  Dữ liệu của tệp ảnh.
   */
  private void uploadCheckupImageToGoogleDriveAsync(String checkupId, String fileName, byte[] imageData) {
    // CẢI TIẾN: Sử dụng CompletableFuture để xử lý bất đồng bộ, hiệu quả hơn new Thread().
    log.info("uploadCheckupImageToGoogleDriveAsync");
    CompletableFuture.runAsync(() -> {
        try {
            if (!Server.isGoogleDriveConnected()) {
                log.info("Google Drive not connected - skipping image upload for checkup {}", checkupId);
                return;
            }

            // --- SỬA LỖI: Lấy thông tin drive_folder_id từ CSDL một cách an toàn ---
            String checkupDriveFolderId = null;
            String sql = "SELECT drive_folder_id FROM Checkup WHERE checkup_id = ?";
            
            try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                // Giả định checkup_id trong CSDL là kiểu số (integer), an toàn hơn là String.
                stmt.setInt(1, Integer.parseInt(checkupId));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        checkupDriveFolderId = rs.getString("drive_folder_id");
                    }
                }
            }

            if (checkupDriveFolderId == null || checkupDriveFolderId.trim().isEmpty()) {
                log.warn("Checkup {} has no Google Drive folder ID - cannot upload image", checkupId);
                return;
            }

            log.info("Uploading image {} to Google Drive for checkup {}", fileName, checkupId);
            
            // Logic tạo file tạm và xóa sau khi dùng của bạn đã tốt, chúng ta sẽ giữ lại.
            java.io.File tempFile = null;
            try {
                tempFile = createTempFileFromBytes(imageData, fileName);
                
                String uploadedFileId = Server.getGoogleDriveService().uploadFileToFolder(
                    checkupDriveFolderId, tempFile, fileName
                );
                
                log.info("Image uploaded successfully to Google Drive for checkup {}: FileID={}", checkupId, uploadedFileId);
                
                if (ServerDashboard.getInstance() != null) {
                    ServerDashboard.getInstance().addLog(
                        String.format("Uploaded image %s to Google Drive for checkup %s", fileName, checkupId)
                    );
                }
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    if (tempFile.delete()) {
                        log.debug("Cleaned up temporary file: {}", tempFile.getAbsolutePath());
                    } else {
                        log.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath());
                    }
                }
            }
            
        } catch (NumberFormatException e) {
            log.error("Invalid checkupId format '{}' for Google Drive upload.", checkupId);
        } catch (Exception e) {
            log.error("Failed to upload image to Google Drive for checkup {}: {}", checkupId, e.getMessage(), e);
            if (ServerDashboard.getInstance() != null) {
                ServerDashboard.getInstance().addLog(
                    String.format("Failed to upload image %s to Google Drive for checkup %s: %s", fileName, checkupId, e.getMessage())
                );
            }
        }
    });
  }

  /**
   * Creates a temporary file from byte array for Google Drive upload
   */
  private java.io.File createTempFileFromBytes(byte[] data, String originalFileName) throws IOException {
    // Get file extension
    String extension = "";
    int dotIndex = originalFileName.lastIndexOf('.');
    if (dotIndex > 0) {
      extension = originalFileName.substring(dotIndex);
    }
    
    // Create temporary file
    java.io.File tempFile = java.io.File.createTempFile("checkup_image_", extension);
    
    // Write byte data to temporary file
    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
      fos.write(data);
    }
    
    return tempFile;
  }

  /**
   * Delete an image from Google Drive asynchronously.
   *
   * @param checkupId  ID of the checkup (as String).
   * @param fileName   Name of the image file to delete.
   */
  private void deleteCheckupImageFromGoogleDriveAsync(String checkupId, String fileName) {
    log.info("deleteCheckupImageFromGoogleDriveAsync - Checkup: {}, File: {}", checkupId, fileName);
    CompletableFuture.runAsync(() -> {
        try {
            if (!Server.isGoogleDriveConnected()) {
                log.info("Google Drive not connected - skipping image deletion for checkup {}", checkupId);
                return;
            }

            // Get the checkup's Google Drive folder ID from database
            String checkupDriveFolderId = null;
            String sql = "SELECT drive_folder_id FROM Checkup WHERE checkup_id = ?";
            
            try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, Integer.parseInt(checkupId));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        checkupDriveFolderId = rs.getString("drive_folder_id");
                    }
                }
            }

            if (checkupDriveFolderId == null || checkupDriveFolderId.trim().isEmpty()) {
                log.warn("Checkup {} has no Google Drive folder ID - cannot delete image", checkupId);
                return;
            }

            log.info("Deleting image {} from Google Drive for checkup {}", fileName, checkupId);
            
            // Find and delete the file from Google Drive
            List<com.google.api.services.drive.model.File> files = 
                Server.getGoogleDriveService().findFilesByName(checkupDriveFolderId, fileName);
            
            if (files.isEmpty()) {
                log.warn("File {} not found in Google Drive folder for checkup {}", fileName, checkupId);
                return;
            }
            
            // Delete all matching files (there should typically be only one)
            for (com.google.api.services.drive.model.File file : files) {
                Server.getGoogleDriveService().deleteFile(file.getId());
                log.info("Successfully deleted file {} (ID: {}) from Google Drive for checkup {}", 
                        fileName, file.getId(), checkupId);
                
                if (ServerDashboard.getInstance() != null) {
                    ServerDashboard.getInstance().addLog(
                        String.format("Deleted image %s from Google Drive for checkup %s", fileName, checkupId)
                    );
                }
            }
            
        } catch (Exception e) {
            log.error("Error deleting image {} from Google Drive for checkup {}", fileName, checkupId, e);
        }
    });
  }

  private void deleteCheckupImagesFromGoogleDriveAsync(String folderId) {
    log.info("deleteCheckupImagesFromGoogleDriveAsync - Folder: {}", folderId);
    CompletableFuture.runAsync(() -> {
        try {
            if (!Server.isGoogleDriveConnected()) {
                log.warn("Google Drive not connected - skipping folder deletion.");
                return;
            }
            Server.getGoogleDriveService().deleteFile(folderId);
            log.info("Successfully deleted folder with ID: {} from Google Drive.", folderId);
        } catch (Exception e) {
            log.error("Error deleting folder {} from Google Drive", folderId, e);
        }
    });
  }

  private void deleteLocalCheckupImageFolder(String checkupId) throws IOException {
    Path directory = Paths.get(Server.imageDbPath, checkupId);
    if (Files.exists(directory)) {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("Deleted local file/folder: {}", path);
                    } catch (IOException e) {
                        log.error("Failed to delete path: {}", path, e);
                    }
                });
        }
    } else {
        log.warn("Local image folder not found for checkupId {}: {}", checkupId, directory);
    }
  }

 
  private void broadcastQueueUpdate(int shift) {
    String sql = """
        SELECT
            a.checkup_id, a.checkup_date, c.customer_last_name, c.customer_first_name,
            d.doctor_first_name, d.doctor_last_name, a.suggestion, a.diagnosis, a.notes,
            a.status, a.customer_id, c.customer_number, c.customer_address,
            a.customer_weight, a.customer_height, c.customer_gender, c.customer_dob,
            a.checkup_type, a.conclusion, a.reCheckupDate, c.cccd_ddcn, a.heart_beat,
            a.blood_pressure, c.drive_url, a.doctor_ultrasound_id, a.queue_number
        FROM
            checkup AS a
        JOIN
            customer AS c ON a.customer_id = c.customer_id
        JOIN
            Doctor D ON a.doctor_id = D.doctor_id
        WHERE
            date(a.checkup_date / 1000, 'unixepoch', '+7 hours') = date('now', '+7 hours') AND a.shift = ?""";
    
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, shift);
        ResultSet rs = stmt.executeQuery();
        
        String[][] resultArray;
        if (!rs.isBeforeFirst()) {
            log.warn("No data found in the checkup table for today's shift {}.", shift);
            resultArray = new String[0][0];
        } else {
            // ... (rest of the logic is the same)
            ArrayList<String> resultList = new ArrayList<>();
            while (rs.next()) {
                String checkupId = rs.getString("checkup_id");
                String checkupDate = rs.getString("checkup_date");
                long checkupDateLong = Long.parseLong(checkupDate);
                Timestamp timestamp = new Timestamp(checkupDateLong);
                Date date = new Date(timestamp.getTime());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                checkupDate = sdf.format(date);
                String customerLastName = rs.getString("customer_last_name");
                String customerFirstName = rs.getString("customer_first_name");
                String doctorFirstName = rs.getString("doctor_first_name");
                String doctorLastName = rs.getString("doctor_last_name");
                String suggestion = rs.getString("suggestion");
                String diagnosis = rs.getString("diagnosis");
                String notes = rs.getString("notes");
                String status = rs.getString("status");
                String customerId = rs.getString("customer_id");
                String customerNumber = rs.getString("customer_number");
                String customerAddress = rs.getString("customer_address");
                String customerWeight = rs.getString("customer_weight");
                String customerHeight = rs.getString("customer_height");
                String customerGender = rs.getString("customer_gender");
                String customerDob = rs.getString("customer_dob");
                String checkupType = rs.getString("checkup_type");
                String conclusion = rs.getString("conclusion");
                String reCheckupDate = rs.getString("reCheckupDate");
                String cccdDdcn = rs.getString("cccd_ddcn");
                String heartBeat = rs.getString("heart_beat");
                String bloodPressure = rs.getString("blood_pressure");
                String driveUrl = rs.getString("drive_url");
                String doctorUltrasoundId = rs.getString("doctor_ultrasound_id");
                String queueNumber = String.format("%02d", rs.getInt("queue_number"));
                if (driveUrl == null) {
                    driveUrl = "";
                }
                String result = String.join("|", checkupId, checkupDate, customerLastName, customerFirstName,
                        doctorLastName + " " + doctorFirstName, suggestion, diagnosis, notes, status, customerId, customerNumber, customerAddress, customerWeight, customerHeight,
                        customerGender, customerDob, checkupType, conclusion, reCheckupDate, cccdDdcn, heartBeat, bloodPressure,
                        driveUrl, doctorUltrasoundId, queueNumber
                );
                resultList.add(result);
            }
            String[] resultString = resultList.toArray(new String[0]);
            resultArray = new String[resultString.length][];
            for (int i = 0; i < resultString.length; i++) {
                resultArray[i] = resultString[i].split("\\|");
            }
        }
        
        // --- LOGIC GỐC ĐẾM SỐ BỆNH NHÂN TRONG NGÀY ---
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);
        long startOfTodayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long endOfTodayMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1;
        
        int totalPatientsToday = 0;
        String countQuery = "SELECT COUNT(DISTINCT a.customer_id) as patient_count FROM checkup as a WHERE a.checkup_date >= ? AND a.checkup_date <= ?";
        
        try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
            countStmt.setLong(1, startOfTodayMillis);
            countStmt.setLong(2, endOfTodayMillis);
            try (ResultSet countRs = countStmt.executeQuery()) {
                if (countRs.next()) {
                    totalPatientsToday = countRs.getInt("patient_count");
                }
            }
        }

        // --- LOGIC GỐC ĐẾM SỐ LƯỢT TÁI KHÁM ---
        int totalRecheckToday = 0;
        String recheckQuery = "SELECT COUNT(*) as recheck_count FROM checkup WHERE reCheckupDate >= ? AND reCheckupDate <= ?";
        
        try (PreparedStatement recheckStmt = conn.prepareStatement(recheckQuery)) {
            recheckStmt.setLong(1, startOfTodayMillis);
            recheckStmt.setLong(2, endOfTodayMillis);
            try (ResultSet recheckRs = recheckStmt.executeQuery()) {
                if (recheckRs.next()) {
                    totalRecheckToday = recheckRs.getInt("recheck_count");
                }
            }
        }

        int maxCurId = SessionManager.getMaxSessionId();
        for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
            UserUtil.sendPacket(sessionId, new GetCheckUpQueueUpdateResponse(resultArray, shift));
            UserUtil.sendPacket(sessionId, new TodayPatientCountResponse(totalPatientsToday));
            UserUtil.sendPacket(sessionId, new RecheckCountResponse(totalRecheckToday));
        }
        log.info("send checkup queue update response to all clients");
        log.info("send today patient count response to all clients");
        log.info("send recheck count response to all clients");

    } catch (SQLException e) {
        log.error("Error during broadcastQueueUpdate", e);
    }
}

}