package BsK.server.network.handler;

import BsK.common.packet.Packet;
import BsK.common.packet.PacketSerializer;
import BsK.common.packet.req.*;
import BsK.common.packet.res.*;
import BsK.common.packet.res.ErrorResponse.Error;
import BsK.common.entity.Status;
import BsK.common.util.date.DateUtils;
import BsK.server.Server;
import BsK.server.ServerDashboard;
import BsK.server.network.manager.SessionManager;
import BsK.server.network.util.UserUtil;
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

import static BsK.server.Server.statement;


@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
    log.debug("Received message: {}", frame.text());
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
        SessionManager.onUserDisconnect(ctx.channel());
        // Optionally, you could send a LogoutResponse back to the client if needed,
        // but since the client navigates away, it might not be processed.
      } else {
        log.warn("Received LogoutRequest from a channel with no active user session: {}", ctx.channel().id().asLongText());
      }
    } else {
      // Check if user is authenticated
      var currentUser = SessionManager.getUserByChannel(ctx.channel().id().asLongText());
      if (!currentUser.isAuthenticated()) {
        log.warn("Received packet from unauthenticated user: {}", packet);
        return;
      }

      if (packet instanceof GetCheckUpQueueRequest) {
        log.debug("Received GetCheckUpQueueRequest");
        try {
          ResultSet rs = statement.executeQuery(
                  "select a.checkup_id, a.checkup_date, c.customer_last_name, c.customer_first_name,\n" +
                          "d.doctor_first_name, d.doctor_last_name, a.suggestion, a.diagnosis, a.notes, a.status, a.customer_id, \n" +
                          "c.customer_number, c.customer_address, a.customer_weight, a.customer_height, c.customer_gender, c.customer_dob,\n" +
                          "a.checkup_type, a.conclusion, a.reCheckupDate, c.cccd_ddcn, a.heart_beat, a.blood_pressure, c.drive_url, a.doctor_ultrasound_id, a.queue_number\n" +
                          "from checkup as a\n" +
                          "join customer as c on a.customer_id = c.customer_id\n" +
                          "join Doctor D on a.doctor_id = D.doctor_id\n" +
                          "where a.status = 'ĐANG KHÁM' or a.status = 'CHỜ KHÁM'"
          );

          if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the checkup table.");

          } else {
            ArrayList<String> resultList = new ArrayList<>();
            while (rs.next()) {
              String checkupId = rs.getString("checkup_id");
              String checkupDate = rs.getString("checkup_date");
              long checkupDateLong = Long.parseLong(checkupDate);
              Timestamp timestamp = new Timestamp(checkupDateLong);
              Date date = new Date(timestamp.getTime()); // Needed to recode
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
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetCheckUpQueueUpdateRequest) {
        log.debug("Received GetCheckUpQueueUpdateRequest");
        broadcastQueueUpdate();
      }

      // Get general doctor info
      if (packet instanceof GetDoctorGeneralInfo) {
        log.debug("Received GetDoctorGeneralInfo");
        try {
          ResultSet rs = statement.executeQuery(
                  "select concat(doctor_last_name, ' ', doctor_first_name), doctor_id from Doctor"
          );

            if (!rs.isBeforeFirst()) {
                System.out.println("No data found in the doctor table.");
            } else {
              ArrayList<String> resultList = new ArrayList<>();
              while (rs.next()) {
                String doctorName = rs.getString(1);
                String doctorId = rs.getString(2);
                resultList.add(doctorName + "|" + doctorId);
              }
              String[] resultString = resultList.toArray(new String[0]);
              String[][] resultArray = new String[resultString.length][];
              for (int i = 0; i < resultString.length; i++) {
                resultArray[i] = resultString[i].split("\\|");
              }
              UserUtil.sendPacket(currentUser.getSessionId(), new GetDoctorGeneralInfoResponse(resultArray));
              log.info("Send response to client GetDoctorGeneralInfo");
          }
        }
        catch (SQLException e) {
          throw new RuntimeException(e);
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
            FROM Checkup C JOIN Doctor D ON C.doctor_id = D.doctor_id  WHERE C.status = ? AND C.customer_id = ?
            ORDER BY C.checkup_date DESC;
        """;

        try (PreparedStatement historyStmt = Server.connection.prepareStatement(sql)) {
            
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
                            historyEntry[0] = checkupDateStr; // Fallback to raw string if not a timestamp
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
                    log.info("Sent patient history for patientId: {} the content: {}", getPatientHistoryRequest.getPatientId(), resultArray);
                }
            }
        } catch (SQLException e) {
          log.error("Error fetching patient history for patientId: " + getPatientHistoryRequest.getPatientId(), e);
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetMedInfoRequest getMedInfoRequest) {
        log.debug("Received GetMedInfoRequest");
        try {
          ResultSet rs = statement.executeQuery(
                  "select med_id, med_name, med_company, med_description, quantity, med_unit, med_selling_price, preference_note, supplement\n" +
                          "    from Medicine"
          );

          if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the medicine table.");
          } else {
            ArrayList<String> resultList = new ArrayList<>();
            while (rs.next()) {
                String medId = rs.getString("med_id");
                String medName = rs.getString("med_name");
                String medCompany = rs.getString("med_company");
                String medDescription = rs.getString("med_description");
                String quantity = rs.getString("quantity");
                String medUnit = rs.getString("med_unit");
                String medSellingPrice = rs.getString("med_selling_price");
                String preferenceNote = rs.getString("preference_note");
                String supplement = rs.getString("supplement");


                String result = String.join("|",medId, medName, medCompany, medDescription, quantity, medUnit,
                        medSellingPrice, preferenceNote != null ? preferenceNote : "", supplement != null ? supplement : "0");
                resultList.add(result);
            }

            String[] resultString = resultList.toArray(new String[0]);
            String[][] resultArray = new String[resultString.length][];
            for (int i = 0; i < resultString.length; i++) {
              resultArray[i] = resultString[i].split("\\|", -1);
            }

            UserUtil.sendPacket(currentUser.getSessionId(), new GetMedInfoResponse(resultArray));
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetSerInfoRequest getSerInfoRequest) {
        log.debug("Received GetSerInfoRequest");
        try {
          ResultSet rs = statement.executeQuery(
                  "select service_id, service_name, service_cost\n" +
                          "    from Service"
          );

          if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the service table.");
          } else {
            ArrayList<String> resultList = new ArrayList<>();
            while (rs.next()) {
              String serId = rs.getString("service_id");
              String serName = rs.getString("service_name");
              String serPrice = rs.getString("service_cost");

              String result = String.join("|", serId, serName, serPrice);
              resultList.add(result);
            }

            String[] resultString = resultList.toArray(new String[0]);
            String[][] resultArray = new String[resultString.length][];
            for (int i = 0; i < resultString.length; i++) {
              resultArray[i] = resultString[i].split("\\|");
            }

            UserUtil.sendPacket(currentUser.getSessionId(), new GetSerInfoResponse(resultArray));
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof ClinicInfoRequest clinicInfoRequest) {
        log.debug("Received ClinicInfoRequest");
        try {
          ResultSet rs = statement.executeQuery(
                  "select name, address, phone\n" +
                          "    from Clinic"
          );

            if (!rs.isBeforeFirst()) {
                System.out.println("No data found in the clinic table.");
            } else {
                String clinicName = rs.getString("name");
                String clinicAddress = rs.getString("address");
                String clinicPhone = rs.getString("phone");

                UserUtil.sendPacket(currentUser.getSessionId(), new ClinicInfoResponse(clinicName, clinicAddress, clinicPhone));
            }
        }
        catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetRecentPatientRequest getRecentPatientRequest) {
        log.debug("Received GetRecentPatientRequest");
        try {
          // Build the base query
          StringBuilder queryBuilder = new StringBuilder();
          queryBuilder.append("SELECT customer_id, customer_last_name, customer_first_name, customer_dob, customer_number, ")
                      .append("customer_address, customer_gender, cccd_ddcn FROM Customer");
          
          // Add WHERE clause for search parameters
          boolean hasNameSearch = getRecentPatientRequest.getSearchName() != null && !getRecentPatientRequest.getSearchName().trim().isEmpty();
          boolean hasPhoneSearch = getRecentPatientRequest.getSearchPhone() != null && !getRecentPatientRequest.getSearchPhone().trim().isEmpty();
          
          if (hasNameSearch || hasPhoneSearch) {
            queryBuilder.append(" WHERE ");
            if (hasNameSearch) {
              queryBuilder.append("(LOWER(customer_first_name) LIKE ? OR LOWER(customer_last_name) LIKE ? OR ")
                          .append("LOWER(CONCAT(customer_last_name, ' ', customer_first_name)) LIKE ?)");
            }
            if (hasNameSearch && hasPhoneSearch) {
              queryBuilder.append(" AND ");
            }
            if (hasPhoneSearch) {
              queryBuilder.append("customer_number LIKE ?");
            }
          }
          
          // Add ORDER BY
          queryBuilder.append(" ORDER BY customer_id DESC");
          
          // Count total records for pagination
          String countQuery = queryBuilder.toString().replace("SELECT customer_id, customer_last_name, customer_first_name, customer_dob, customer_number, customer_address, customer_gender, cccd_ddcn", "SELECT COUNT(*)");
          PreparedStatement countStmt = Server.connection.prepareStatement(countQuery);
          
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
          
          ResultSet countRs = countStmt.executeQuery();
          int totalCount = 0;
          if (countRs.next()) {
            totalCount = countRs.getInt(1);
          }
          countRs.close();
          countStmt.close();
          
          // Calculate pagination
          int pageSize = getRecentPatientRequest.getPageSize();
          int currentPage = getRecentPatientRequest.getPage();
          int totalPages = (int) Math.ceil((double) totalCount / pageSize);
          int offset = (currentPage - 1) * pageSize;
          
          // Add LIMIT and OFFSET for pagination
          queryBuilder.append(" LIMIT ? OFFSET ?");
          
          PreparedStatement stmt = Server.connection.prepareStatement(queryBuilder.toString());
          
          // Set parameters for the main query
          paramIndex = 1;
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
          stmt.setInt(paramIndex++, offset);
          
          ResultSet rs = stmt.executeQuery();
          
          if (!rs.isBeforeFirst()) {
            log.debug("No data found in the customer table for the given search criteria.");
            UserUtil.sendPacket(currentUser.getSessionId(), new GetRecentPatientResponse(new String[0][], totalCount, currentPage, totalPages, pageSize));
          } else {
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
          
          rs.close();
          stmt.close();
        } catch (SQLException e) {
          log.error("Error processing GetRecentPatientRequest", e);
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetProvinceRequest getProvinceRequest) {
        log.debug("Received GetProvinceRequest");
        try {
          ResultSet rs = statement.executeQuery(
                  "select provinces.code, provinces.name\n" +
                          "from provinces" +
                          " order by provinces.name"
          );

          if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the province table.");
          } else {
            ArrayList<String> resultList = new ArrayList<>();
            HashMap<String, String> provinceIdMap = new HashMap<>();
            resultList.add("Tỉnh/Thành phố");
            while (rs.next()) {
              String provinceId = rs.getString("code");
              String provinceName = rs.getString("name");
              provinceIdMap.put(provinceName, provinceId);
              resultList.add(provinceName);
            }

            String[] resultString = resultList.toArray(new String[0]);
            UserUtil.sendPacket(currentUser.getSessionId(), new GetProvinceResponse(resultString, provinceIdMap));
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetWardRequest getWardRequest) {
        log.debug("Received GetWardRequest for province {}", getWardRequest.getProvinceId());
        String sql = "SELECT wards.name FROM wards WHERE province_code = ? ORDER BY wards.name";

        // Use try-with-resources to automatically close resources
        try (PreparedStatement preparedStatement = Server.connection.prepareStatement(sql)) {
            preparedStatement.setString(1, getWardRequest.getProvinceId());
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("No data found in the ward table.");
                    // Send empty response
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetWardResponse(new String[]{"Phường/Xã"}));
                } else {
                    ArrayList<String> resultList = new ArrayList<>();
                    resultList.add("Phường/Xã");
                    while (rs.next()) {
                        resultList.add(rs.getString("name"));
                    }
                    String[] resultString = resultList.toArray(new String[0]);
                    UserUtil.sendPacket(currentUser.getSessionId(), new GetWardResponse(resultString));
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching wards for province {}", getWardRequest.getProvinceId(), e);
            throw new RuntimeException(e);
        }
      }
      if (packet instanceof AddPatientRequest addPatientRequest) {
        log.debug("Received AddPatientRequest");
        try {
          // Disable auto-commit for transaction
          Server.connection.setAutoCommit(false);
          
          PreparedStatement preparedStatement = Server.connection.prepareStatement(
                  "INSERT INTO Customer (customer_last_name, customer_first_name, customer_dob, customer_number, customer_address, customer_gender, cccd_ddcn) VALUES (?, ?, ?, ?, ?, ?, ?)",
                  PreparedStatement.RETURN_GENERATED_KEYS // Use this instead of separate query
          );
          preparedStatement.setString(1, addPatientRequest.getPatientLastName());
          preparedStatement.setString(2, addPatientRequest.getPatientFirstName());
          preparedStatement.setLong(3, addPatientRequest.getPatientDob());
          preparedStatement.setString(4, addPatientRequest.getPatientPhone());
          preparedStatement.setString(5, addPatientRequest.getPatientAddress());
          preparedStatement.setString(6, addPatientRequest.getPatientGender());
          preparedStatement.setString(7, addPatientRequest.getPatientCccdDdcn());
          preparedStatement.executeUpdate();

          // Get the generated key safely
          int customerId = 0;
          try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
              customerId = generatedKeys.getInt(1);
            } else {
              throw new SQLException("Creating patient failed, no ID obtained.");
            }
          }
          
          // Commit the transaction
          Server.connection.commit();
          
          // Close resources
          preparedStatement.close();

          // Send immediate response to client
          UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(true, customerId, "Thêm bệnh nhân thành công"));
          
          // Create Google Drive folder asynchronously (don't block the response)
          createPatientGoogleDriveFolderAsync(customerId, addPatientRequest.getPatientLastName(), addPatientRequest.getPatientFirstName());
        } catch (SQLException e) {
          try {
            // Rollback on error
            Server.connection.rollback();
          } catch (SQLException rollbackEx) {
            log.error("Error during rollback", rollbackEx);
          }
          String errorMessage = e.getMessage();
          UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(false, -1, "Lỗi: " + errorMessage));
          log.error("Error adding patient", e);
        } finally {
          try {
            // Reset auto-commit
            Server.connection.setAutoCommit(true);
          } catch (SQLException e) {
            log.error("Error resetting auto-commit", e);
          }
        }
      }

      if (packet instanceof AddCheckupRequest addCheckupRequest) {
        log.debug("Received AddCheckupRequest to add patient{}", addCheckupRequest.getCustomerId());
        try {
          // Disable auto-commit
          Server.connection.setAutoCommit(false);

          int queueNumber = 0;
          // Use a transaction to ensure atomic operations
          try {
              // First, try to update an existing counter for today
              try (PreparedStatement updateStmt = Server.connection.prepareStatement(
                      "UPDATE DailyQueueCounter SET current_count = current_count + 1 " +
                      "WHERE date = date('now', 'localtime') RETURNING current_count")) {
                  ResultSet rs = updateStmt.executeQuery();
                  if (rs.next()) {
                      // Update successful, get the new value
                      queueNumber = rs.getInt(1);
                  } else {
                      // No row for today exists yet, insert one
                      try (PreparedStatement insertStmt = Server.connection.prepareStatement(
                              "INSERT INTO DailyQueueCounter (date, current_count) VALUES (date('now', 'localtime'), 1) " +
                              "RETURNING current_count")) {
                          ResultSet insertRs = insertStmt.executeQuery();
                          if (insertRs.next()) {
                              queueNumber = insertRs.getInt(1); // Should be 1
                          } else {
                              throw new SQLException("Failed to insert new queue counter");
                          }
                      }
                  }
              }
          } catch (SQLException e) {
            log.error("Error updating queue counter", e);
            throw new RuntimeException(e);
          } 
          // First query: Insert Checkup
          PreparedStatement checkupStmt = Server.connection.prepareStatement(
                  "INSERT INTO Checkup (customer_id, doctor_id, checkup_type, status, queue_number) VALUES (?, ?, ?, ?, ?)");
          checkupStmt.setInt(1, addCheckupRequest.getCustomerId());
          checkupStmt.setInt(2, addCheckupRequest.getDoctorId());
          checkupStmt.setString(3, addCheckupRequest.getCheckupType());
          checkupStmt.setString(4, addCheckupRequest.getStatus());
          checkupStmt.setInt(5, queueNumber);
          checkupStmt.executeUpdate();

          // Second query: Insert MedicineOrder
          PreparedStatement medOrderStmt = Server.connection.prepareStatement(
                  "INSERT INTO MedicineOrder (checkup_id, customer_id, processed_by) VALUES (last_insert_rowid(), ?, ?)");
          medOrderStmt.setInt(1, addCheckupRequest.getCustomerId());
          medOrderStmt.setInt(2, addCheckupRequest.getProcessedById());
          medOrderStmt.executeUpdate();

          // Third query: Update Checkup

          PreparedStatement updateStmt = Server.connection.prepareStatement(
                  "UPDATE Checkup SET prescription_id = last_insert_rowid() WHERE checkup_id = (SELECT MAX(checkup_id) FROM Checkup WHERE customer_id = ?)");
          updateStmt.setInt(1, addCheckupRequest.getCustomerId());
          updateStmt.executeUpdate();

          // Get the generated checkup ID safely
          int generatedCheckupId = 0;
          PreparedStatement getCheckupIdStmt = Server.connection.prepareStatement(
                  "SELECT MAX(checkup_id) FROM Checkup WHERE customer_id = ?");
          getCheckupIdStmt.setInt(1, addCheckupRequest.getCustomerId());
          ResultSet checkupIdRs = getCheckupIdStmt.executeQuery();
          if (checkupIdRs.next()) {
            generatedCheckupId = checkupIdRs.getInt(1);
          }
          getCheckupIdStmt.close();
          checkupIdRs.close();

          // Commit the transaction
          Server.connection.commit();

          // Send immediate response to client
          UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(true, "Thêm bệnh nhân thành công", queueNumber));
          
          broadcastQueueUpdate();
          
          // Create Google Drive folder asynchronously (don't block the response)
          createCheckupGoogleDriveFolderAsync(generatedCheckupId, addCheckupRequest.getCustomerId());
        }
        catch (SQLException e) {
          try {
            // Rollback on error
            Server.connection.rollback();
          } catch (SQLException rollbackEx) {
            log.error("Error during rollback", rollbackEx);
          }
          String errorMessage = e.getMessage();
          UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(false, "Lỗi: " + errorMessage, -1));
          throw new RuntimeException(e);
        }
        finally {
          try {
            // Reset auto-commit
            Server.connection.setAutoCommit(true);
          } catch (SQLException e) {
            log.error("Error resetting auto-commit", e);
          }
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
        
        // The connection is managed by the try-catch-finally block below
        Connection conn = Server.connection; 
        try {
            conn.setAutoCommit(false); // Start transaction
            
            // 1. Insert/Update Customer (Now safe with try-with-resources)
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
                log.info("Customer saved successfully");
            }
            
            // This will hold the new ID generated by the database.
            Integer newPrescriptionId = null;
    
            // 2. Handle Medicine Prescriptions
            if (saveCheckupRequest.getMedicinePrescription() != null && 
                saveCheckupRequest.getMedicinePrescription().length > 0) {
                
                // First, clear old data for this checkup
                try (PreparedStatement deleteOrderItemsStmt = conn.prepareStatement("DELETE FROM OrderItem WHERE checkup_id = ?")) {
                    deleteOrderItemsStmt.setInt(1, saveCheckupRequest.getCheckupId());
                    deleteOrderItemsStmt.executeUpdate();
                }
                try (PreparedStatement deleteMedicineOrderStmt = conn.prepareStatement("DELETE FROM MedicineOrder WHERE checkup_id = ?")) {
                    deleteMedicineOrderStmt.setInt(1, saveCheckupRequest.getCheckupId());
                    deleteMedicineOrderStmt.executeUpdate();
                }
                log.info("Cleared previous medicine prescription for checkup_id: {}", saveCheckupRequest.getCheckupId());
    
                // A. Insert a new MedicineOrder record to get the auto-generated ID
                String medicineOrderSql = """
                    INSERT INTO MedicineOrder (
                        checkup_id, customer_id, total_amount, payment_method, 
                        status, payment_status, processed_by, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                // Use RETURN_GENERATED_KEYS to get the new ID back
                try (PreparedStatement medicineOrderStmt = conn.prepareStatement(medicineOrderSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    // ... (your existing totalMedicineAmount calculation) ...
                    double totalMedicineAmount = 0;
                    for (String[] medicine : saveCheckupRequest.getMedicinePrescription()) {
                        if (medicine.length > 8) { totalMedicineAmount += Double.parseDouble(medicine[8]); }
                    }
    
                    medicineOrderStmt.setInt(1, saveCheckupRequest.getCheckupId());
                    medicineOrderStmt.setInt(2, saveCheckupRequest.getCustomerId());
                    medicineOrderStmt.setDouble(3, totalMedicineAmount);
                    medicineOrderStmt.setString(4, ""); // payment_method
                    medicineOrderStmt.setString(5, "Pending"); // status
                    medicineOrderStmt.setString(6, "Unpaid"); // payment_status
                    medicineOrderStmt.setString(7, ""); // processed_by
                    medicineOrderStmt.setString(8, ""); // notes
                    medicineOrderStmt.executeUpdate();
                    
                    // B. Retrieve the generated key
                    try (ResultSet generatedKeys = medicineOrderStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            newPrescriptionId = generatedKeys.getInt(1);
                            log.info("New prescription_id generated: {}", newPrescriptionId);
                        } else {
                            throw new SQLException("Creating medicine order failed, no ID obtained.");
                        }
                    }
                }
    
                // C. Insert OrderItems using the new prescription_id
                String orderItemSql = """
                    INSERT INTO OrderItem (
                        prescription_id, med_id, quantity_ordered, dosage, duration, 
                        price_per_unit, total_price, checkup_id, notes
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                try (PreparedStatement orderItemStmt = conn.prepareStatement(orderItemSql)) {
                    for (String[] medicine : saveCheckupRequest.getMedicinePrescription()) {
                        if (medicine.length >= 10) {
                            orderItemStmt.setInt(1, newPrescriptionId); // Use the new ID
                            orderItemStmt.setString(2, medicine[0]); // med_id
                            orderItemStmt.setString(3, medicine[2]); // quantity
                            String dosage = String.format("Sáng %s, Trưa %s, Chiều %s", medicine[4], medicine[5], medicine[6]);
                            orderItemStmt.setString(4, dosage);
                            orderItemStmt.setString(5, ""); // duration
                            orderItemStmt.setString(6, medicine[7]); // unit_price
                            orderItemStmt.setString(7, medicine[8]); // total_price
                            orderItemStmt.setInt(8, saveCheckupRequest.getCheckupId());
                            orderItemStmt.setString(9, medicine[9]); // notes
                            orderItemStmt.addBatch();
                        }
                    }
                    orderItemStmt.executeBatch();
                    log.info("OrderItems saved for prescription_id: {}", newPrescriptionId);
                }
            }
    
            // 3. Insert/Update Checkup with the correct newPrescriptionId
            String checkupSql = """
                INSERT INTO Checkup (
                    checkup_id, customer_id, doctor_id, checkup_date, suggestion, diagnosis, 
                    prescription_id, notes, status, checkup_type, conclusion, reCheckupDate, 
                    customer_weight, customer_height, heart_beat, blood_pressure, doctor_ultrasound_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(checkup_id) DO UPDATE SET
                    suggestion = excluded.suggestion, diagnosis = excluded.diagnosis,
                    prescription_id = excluded.prescription_id, notes = excluded.notes,
                    status = excluded.status, checkup_type = excluded.checkup_type,
                    conclusion = excluded.conclusion, reCheckupDate = excluded.reCheckupDate,
                    customer_weight = excluded.customer_weight, customer_height = excluded.customer_height,
                    heart_beat = excluded.heart_beat, blood_pressure = excluded.blood_pressure,
                    doctor_ultrasound_id = excluded.doctor_ultrasound_id, doctor_id = excluded.doctor_id
                """;
            
            try (PreparedStatement checkupStmt = conn.prepareStatement(checkupSql)) {
                checkupStmt.setInt(1, saveCheckupRequest.getCheckupId());
                checkupStmt.setInt(2, saveCheckupRequest.getCustomerId());
                checkupStmt.setInt(3, saveCheckupRequest.getDoctorId());
                checkupStmt.setLong(4, saveCheckupRequest.getCheckupDate());
                checkupStmt.setString(5, saveCheckupRequest.getSuggestions());
                checkupStmt.setString(6, saveCheckupRequest.getDiagnosis());
                // Use setObject to handle the case where newPrescriptionId is null
                checkupStmt.setObject(7, newPrescriptionId); 
                checkupStmt.setString(8, saveCheckupRequest.getNotes());
                checkupStmt.setString(9, saveCheckupRequest.getStatus());
                checkupStmt.setString(10, saveCheckupRequest.getCheckupType());
                checkupStmt.setString(11, saveCheckupRequest.getConclusion());
                if (saveCheckupRequest.getReCheckupDate() != null) {
                    checkupStmt.setLong(12, saveCheckupRequest.getReCheckupDate());
                } else {
                    checkupStmt.setLong(12, 0);
                }
                checkupStmt.setDouble(13, saveCheckupRequest.getCustomerWeight());
                checkupStmt.setDouble(14, saveCheckupRequest.getCustomerHeight());
                checkupStmt.setInt(15, saveCheckupRequest.getHeartBeat());
                checkupStmt.setString(16, saveCheckupRequest.getBloodPressure());
                checkupStmt.setInt(17, saveCheckupRequest.getDoctorUltrasoundId());
                checkupStmt.executeUpdate();
                log.info("Checkup saved successfully");
            }
    
            // 4. Handle Service Prescriptions (Now safe with try-with-resources)
            // ... (your existing service handling logic is fine, just wrap it) ...
            
            conn.commit(); // Commit the entire transaction
            log.info("Successfully saved checkup {} with all related data", saveCheckupRequest.getCheckupId());
            UserUtil.sendPacket(currentUser.getSessionId(), new SaveCheckupRes(true, "Checkup saved successfully"));
            
        } catch (Exception e) {
            log.error("Error saving checkup transaction", e);
            if (conn != null) {
                try {
                    conn.rollback();
                    log.info("Transaction rolled back due to error");
                } catch (SQLException rollbackEx) {
                    log.error("Error rolling back transaction", rollbackEx);
                }
            }
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore auto-commit mode
                } catch (SQLException e) {
                    log.error("Error restoring auto-commit", e);
                }
            }
        }
    }
      
      if (packet instanceof GetOrderInfoByCheckupReq getOrderInfoByCheckupReq) {
        log.debug("Received GetOrderInfoByCheckupReq for checkupId: {}", getOrderInfoByCheckupReq.getCheckupId());
        
        String checkupId = getOrderInfoByCheckupReq.getCheckupId();
        String[][] medicinePrescription = null;
        String[][] servicePrescription = null;

        // Get Medicine Prescription
        try {
          PreparedStatement medStmt = Server.connection.prepareStatement(
              """
              SELECT
                  M.med_id,
                  M.med_name,
                  OI.quantity_ordered,
                  M.med_unit,
                  OI.dosage,
                  OI.price_per_unit,
                  OI.total_price,
                  OI.notes,
                  M.supplement
              FROM OrderItem OI
              JOIN Medicine M ON OI.med_id = M.med_id
              WHERE OI.checkup_id = ?
              """
          );
          medStmt.setString(1, checkupId);
          ResultSet medRs = medStmt.executeQuery();
          
          ArrayList<String[]> medList = new ArrayList<>();
          while(medRs.next()) {
            String[] med = new String[11];
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
            medList.add(med);
          }
          medicinePrescription = medList.toArray(new String[0][]);
          
        } catch (SQLException e) {
          log.error("Error getting medicine prescription for checkupId: " + checkupId, e);
        }
        
        // Get Service Prescription
        try {
          PreparedStatement serStmt = Server.connection.prepareStatement(
            """
            SELECT
                S.service_id,
                S.service_name,
                CS.quantity,
                S.service_cost,
                CS.total_cost
            FROM CheckupService CS
            JOIN Service S ON CS.service_id = S.service_id
            WHERE CS.checkup_id = ?
            """
          );
          serStmt.setString(1, checkupId);
          ResultSet serRs = serStmt.executeQuery();
          
          ArrayList<String[]> serList = new ArrayList<>();
          while(serRs.next()) {
            String[] ser = new String[6];
            ser[0] = serRs.getString("service_id");
            ser[1] = serRs.getString("service_name");
            ser[2] = serRs.getString("quantity");
            ser[3] = serRs.getString("service_cost");
            ser[4] = serRs.getString("total_cost");
            ser[5] = ""; // Notes - not available in CheckupService table
            serList.add(ser);
          }
          servicePrescription = serList.toArray(new String[0][]);

        } catch (SQLException e) {
          log.error("Error getting service prescription for checkupId: " + checkupId, e);
        }
        
        UserUtil.sendPacket(currentUser.getSessionId(), new GetOrderInfoByCheckupRes(medicinePrescription, servicePrescription));
        log.info("Sent order info for checkup {} to client", checkupId);
      }
      if (packet instanceof AddTemplateReq addTemplateReq) {
        log.debug("Received AddTemplateReq: {}", addTemplateReq);
        String templateName = addTemplateReq.getTemplateName();
        String templateTitle = addTemplateReq.getTemplateTitle();
        String templateDiagnosis = addTemplateReq.getTemplateDiagnosis();
        String templateConclusion = addTemplateReq.getTemplateConclusion();
        String templateSuggestion = addTemplateReq.getTemplateSuggestion();
        String templateImageCount = addTemplateReq.getTemplateImageCount();
        String templatePrintType = addTemplateReq.getTemplatePrintType();
        String templateGender = addTemplateReq.getTemplateGender();
        String templateContent = addTemplateReq.getTemplateContent();
        boolean templateVisible = addTemplateReq.isVisible();
        int templateStt = addTemplateReq.getStt();
        try {
          PreparedStatement templateStmt = Server.connection.prepareStatement(
            """
            INSERT INTO CheckupTemplate (template_gender, template_name, template_title, photo_num, print_type, content, conclusion, suggestion, diagnosis, visible, stt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """);
          templateStmt.setString(1, templateGender);
          templateStmt.setString(2, templateName);
          templateStmt.setString(3, templateTitle);
          templateStmt.setString(4, templateImageCount);
          templateStmt.setString(5, templatePrintType);
          templateStmt.setString(6, templateContent);
          templateStmt.setString(7, templateConclusion);
          templateStmt.setString(8, templateSuggestion);
          templateStmt.setString(9, templateDiagnosis);
          templateStmt.setBoolean(10, templateVisible);
          templateStmt.setInt(11, templateStt);
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
        try {
            PreparedStatement stmt = Server.connection.prepareStatement("SELECT * FROM CheckupTemplate");
            ResultSet rs = stmt.executeQuery();
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
        log.debug("Received EditTemplateReq: {}", editTemplateReq);
        Template template = editTemplateReq.getTemplate();
        try {
            PreparedStatement stmt = Server.connection.prepareStatement(
                """
                UPDATE CheckupTemplate SET template_gender = ?, template_name = ?, template_title = ?, photo_num = ?, print_type = ?, content = ?, conclusion = ?, suggestion = ?, diagnosis = ?, visible = ?, stt = ?
                WHERE template_id = ?
                """
            );
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
            stmt.executeUpdate();
            log.info("Template updated successfully");
            UserUtil.sendPacket(currentUser.getSessionId(), new EditTemplateRes(true, "Template updated successfully"));
        } catch (SQLException e) {
            log.error("Error updating template", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new EditTemplateRes(false, "Error updating template: " + e.getMessage()));
        }
      }
      if (packet instanceof DeleteTemplateReq deleteTemplateReq) {
        log.debug("Received DeleteTemplateReq: {}", deleteTemplateReq);
        try {
            PreparedStatement stmt = Server.connection.prepareStatement("DELETE FROM CheckupTemplate WHERE template_id = ?");
            stmt.setInt(1, deleteTemplateReq.getTemplateId());
            stmt.executeUpdate();
            log.info("Template deleted successfully");
            UserUtil.sendPacket(currentUser.getSessionId(), new DeleteTemplateRes(true, "Template deleted successfully"));
        } catch (SQLException e) {
            log.error("Error deleting template", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new DeleteTemplateRes(false, "Error deleting template: " + e.getMessage()));
        }
      }

      if (packet instanceof UploadCheckupImageRequest uploadCheckupImageRequest) {
        log.info("Received UploadCheckupImageRequest for checkupId: {}", uploadCheckupImageRequest.getCheckupId());

        String checkupId = uploadCheckupImageRequest.getCheckupId();
        String fileName = uploadCheckupImageRequest.getFileName();
        byte[] imageData = uploadCheckupImageRequest.getImageData();

        if (checkupId == null || checkupId.trim().isEmpty()) {
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "CheckupID is null or empty. Cannot save image.", fileName));
            return;
        }

        try {
            // 1. Define and create storage directory
            Path storageDir = Paths.get(Server.imageDbPath, checkupId.trim());
            Files.createDirectories(storageDir); // Create dirs if they don't exist

            // 2. Convert and save image as PNG for archival storage
            // Convert filename to PNG extension for server storage consistency
            String pngFileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".png";
            Path filePath = storageDir.resolve(pngFileName);
            
            // Convert image data to PNG format
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageData)) {
                BufferedImage image = ImageIO.read(bais);
                if (image != null) {
                    ImageIO.write(image, "PNG", filePath.toFile());
                    log.info("Successfully converted and saved image as PNG to {}", filePath);
                } else {
                    // Fallback: save as original format if conversion fails
                    try (FileOutputStream fos = new FileOutputStream(storageDir.resolve(fileName).toFile())) {
                        fos.write(imageData);
                    }
                    log.warn("Failed to convert image to PNG, saved as original format: {}", fileName);
                }
            }
            String savedPath = filePath.toString().replace("\\", "/");

            // 3. TODO: Store file path in database, linking it to the checkupId
            // A proper implementation would have a 'CheckupImages' table and an INSERT query here.

            // 4. Send immediate success response to client
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(true, "Image uploaded successfully to " + savedPath, fileName));

            // 5. Upload to Google Drive asynchronously (don't block the response)
            uploadCheckupImageToGoogleDriveAsync(checkupId, fileName, imageData);

        } catch (IOException e) {
            log.error("Failed to save uploaded image for checkupId {}: {}", checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "Server failed to save image: " + e.getMessage(), fileName));
        }
      }

      if (packet instanceof UploadCheckupPdfRequest uploadCheckupPdfRequest) {
        log.info("Received UploadCheckupPdfRequest for checkupId: {}, type: {}", uploadCheckupPdfRequest.getCheckupId(), uploadCheckupPdfRequest.getPdfType());

        String checkupId = uploadCheckupPdfRequest.getCheckupId();
        String fileName = uploadCheckupPdfRequest.getFileName();
        String pdfType = uploadCheckupPdfRequest.getPdfType();
        byte[] pdfData = uploadCheckupPdfRequest.getPdfData();

        if (checkupId == null || checkupId.trim().isEmpty()) {
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupPdfResponse(false, "CheckupID is null or empty. Cannot save PDF.", fileName, pdfType));
            return;
        }

        try {
            // 1. Define and create storage directory (use same img_db structure)
            Path storageDir = Paths.get(Server.imageDbPath, checkupId.trim());
            Files.createDirectories(storageDir); // Create dirs if they don't exist

            // 2. Save the PDF file (with override behavior - same name = override)
            Path filePath = storageDir.resolve(fileName);
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(pdfData);
            }
            String savedPath = filePath.toString().replace("\\", "/");
            log.info("Successfully saved PDF to {} (override mode)", savedPath);

            // 3. Send immediate success response to client
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupPdfResponse(true, "PDF uploaded successfully to " + savedPath, fileName, pdfType));

            // 4. Upload to Google Drive asynchronously (don't block the response)
            uploadCheckupPdfToGoogleDriveAsync(checkupId, fileName, pdfData, pdfType);

        } catch (IOException e) {
            log.error("Failed to save uploaded PDF for checkupId {}: {}", checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupPdfResponse(false, "Server failed to save PDF: " + e.getMessage(), fileName, pdfType));
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
        List<byte[]> imageDatas = new ArrayList<>();
        boolean success = false;
        String message = "";

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
                
                success = true;
                message = String.format("Successfully synced %d images for checkup %s", imageDatas.size(), checkupId);
                log.info("Successfully synced {} images for checkupId: {}", imageDatas.size(), checkupId);
                
            } catch (IOException e) {
                log.error("Failed to list images for checkupId: {}", checkupId, e);
                message = "Failed to read images from server: " + e.getMessage();
            }
        } else {
            log.warn("Image directory not found for checkupId: {}", checkupId);
            message = "No images found for this checkup on server";
            success = true; // Not finding images is still a successful response
        }

        UserUtil.sendPacket(currentUser.getSessionId(), new SyncCheckupImagesResponse(checkupId, imageNames, imageDatas, success, message));
        log.info("Sent sync response with {} images for checkupId: {}", imageDatas.size(), checkupId);
      }
      
      if (packet instanceof GetRecheckUpListRequest getRecheckUpListRequest) {
        log.debug("Received GetRecheckUpListRequest");
        getRecheckUpList(currentUser.getSessionId());
      }

      if (packet instanceof AddRemindDateRequest addRemindDateRequest) {
        log.debug("Received AddRemindDateRequest");
        try {
            String sql = "UPDATE Checkup SET remind_date = ? WHERE checkup_id = ?";
            PreparedStatement stmt = Server.connection.prepareStatement(sql);
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, addRemindDateRequest.getCheckupId());
            stmt.executeUpdate();
            log.info("Updated remind_date for checkup_id: {}", addRemindDateRequest.getCheckupId());
            getRecheckUpList(currentUser.getSessionId());
        } catch (SQLException e) {
            log.error("Error updating remind_date", e);
            UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
        }
      }
    }
  }

  private void getRecheckUpList(int sessionId) {
        try {
          String sql = "SELECT c.customer_last_name, c.customer_first_name, c.customer_number, ch.reCheckupDate, ch.remind_date, ch.checkup_id " +
                      "FROM Checkup ch " +
                      "JOIN Customer c ON ch.customer_id = c.customer_id " +
                      "WHERE ch.reCheckupDate BETWEEN ? AND ?";
          
          long fromDate = System.currentTimeMillis();
          long toDate = fromDate + (7L * 24 * 60 * 60 * 1000);

          PreparedStatement stmt = Server.connection.prepareStatement(sql);
          stmt.setLong(1, fromDate);
          stmt.setLong(2, toDate);
          
          ResultSet rs = stmt.executeQuery();
          
          List<String[]> results = new ArrayList<>();
          while (rs.next()) {
              String[] row = new String[5];
              row[0] = rs.getString("customer_last_name") + " " + rs.getString("customer_first_name");
              row[1] = rs.getString("customer_number");

              long recheckTimestamp = rs.getLong("reCheckupDate");
              long remindTimestamp = rs.getLong("remind_date");
              SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
              row[2] = sdf.format(new Date(recheckTimestamp));
              row[3] = sdf.format(new Date(remindTimestamp));
              row[4] = rs.getString("checkup_id");
              results.add(row);
          }
          
          UserUtil.sendPacket(sessionId, new GetRecheckUpListResponse(results.toArray(new String[0][])));
          
      } catch (SQLException e) {
          log.error("Error fetching recheck-up list", e);
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
  private void updatePatientDriveInfo(int patientId, String driveUrl, String driveFolderId) {
    try {
      PreparedStatement updateStmt = Server.connection.prepareStatement(
        "UPDATE Customer SET drive_url = ?, drive_folder_id = ? WHERE customer_id = ?"
      );
      updateStmt.setString(1, driveUrl);
      updateStmt.setString(2, driveFolderId);
      updateStmt.setInt(3, patientId);
      
      int rowsUpdated = updateStmt.executeUpdate();
      updateStmt.close();
      
      if (rowsUpdated > 0) {
        log.info("Updated Google Drive info for patient {}: URL={}, FolderID={}", patientId, driveUrl, driveFolderId);
      } else {
        log.warn("No rows updated for patient {} drive info", patientId);
      }
      
    } catch (SQLException e) {
      log.error("Failed to update Google Drive info for patient {}: {}", patientId, e.getMessage());
    }
  }

  /**
   * Creates a Google Drive folder for a checkup asynchronously and updates the checkup drive_url in database.
   * This method runs in a background thread to avoid blocking the checkup creation response.
   */
  private void createCheckupGoogleDriveFolderAsync(int checkupId, int customerId) {
    // Run in background thread to avoid blocking
    new Thread(() -> {
      try {
        // Check if Google Drive is connected
        if (!Server.isGoogleDriveConnected()) {
          log.info("Google Drive not connected - skipping checkup folder creation for checkup {}", checkupId);
          return;
        }

        // Get patient information from database
        PreparedStatement patientStmt = Server.connection.prepareStatement(
          "SELECT customer_last_name, customer_first_name, drive_folder_id FROM Customer WHERE customer_id = ?"
        );
        patientStmt.setInt(1, customerId);
        ResultSet patientRs = patientStmt.executeQuery();
        
        String patientLastName = "";
        String patientFirstName = "";
        String patientDriveFolderId = "";
        
        if (patientRs.next()) {
          patientLastName = patientRs.getString("customer_last_name");
          patientFirstName = patientRs.getString("customer_first_name");
          patientDriveFolderId = patientRs.getString("drive_folder_id");
        }
        patientStmt.close();
        patientRs.close();

        if (patientDriveFolderId == null || patientDriveFolderId.trim().isEmpty()) {
          log.warn("Patient {} has no Google Drive folder ID - cannot create checkup folder", customerId);
          return;
        }

        log.info("Creating Google Drive checkup folder for checkup {} (patient: {} {})", checkupId, patientLastName, patientFirstName);
        
        // Create checkup folder name using Vietnamese format
        String checkupFolderName = TextUtils.createCheckupFolderNameWithId(checkupId, patientLastName, patientFirstName);
        
        // Create checkup folder directly under patient folder using saved folder ID
        String checkupFolderId = createCheckupFolderDirectly(patientDriveFolderId, checkupFolderName);
        
        // Make the checkup folder public and get sharing URL
        String folderUrl = Server.getGoogleDriveService().getFolderSharingUrl(checkupFolderId);
        
        // Update the checkup drive_url and drive_folder_id in database
        updateCheckupDriveInfo(checkupId, folderUrl, checkupFolderId);
        
        log.info("Google Drive checkup folder created successfully for checkup {}: {}", checkupId, folderUrl);
        
        // Notify dashboard
        if (ServerDashboard.getInstance() != null) {
          ServerDashboard.getInstance().addLog(
            String.format("Created Google Drive checkup folder for checkup %d: %s", checkupId, checkupFolderName)
          );
        }
        
      } catch (Exception e) {
        log.error("Failed to create Google Drive checkup folder for checkup {}: {}", checkupId, e.getMessage());
        
        // Notify dashboard about the error
        if (ServerDashboard.getInstance() != null) {
          ServerDashboard.getInstance().addLog(
            String.format("Failed to create Google Drive checkup folder for checkup %d: %s", checkupId, e.getMessage())
          );
        }
      }
    }).start();
  }

  /**
   * Creates a checkup folder directly under patient folder using Google Drive API
   */
  private String createCheckupFolderDirectly(String parentFolderId, String folderName) throws Exception {
    return Server.getGoogleDriveService().createFolderUnderParent(parentFolderId, folderName);
  }

  /**
   * Updates both the drive_url and drive_folder_id columns for a checkup in the database.
   * Note: You need to add drive_url and drive_folder_id columns to the Checkup table.
   */
  private void updateCheckupDriveInfo(int checkupId, String driveUrl, String driveFolderId) {
    try {
      PreparedStatement updateStmt = Server.connection.prepareStatement(
        "UPDATE Checkup SET drive_url = ?, drive_folder_id = ? WHERE checkup_id = ?"
      );
      updateStmt.setString(1, driveUrl);
      updateStmt.setString(2, driveFolderId);
      updateStmt.setInt(3, checkupId);
      
      int rowsUpdated = updateStmt.executeUpdate();
      updateStmt.close();
      
      if (rowsUpdated > 0) {
        log.info("Updated Google Drive info for checkup {}: URL={}, FolderID={}", checkupId, driveUrl, driveFolderId);
      } else {
        log.warn("No rows updated for checkup {} drive info", checkupId);
      }
      
    } catch (SQLException e) {
      log.error("Failed to update Google Drive info for checkup {}: {}", checkupId, e.getMessage());
    }
  }

  /**
   * Uploads a checkup image to Google Drive asynchronously using the checkup's folder ID.
   * This method runs in a background thread to avoid blocking the image upload response.
   */
  private void uploadCheckupImageToGoogleDriveAsync(String checkupId, String fileName, byte[] imageData) {
    // Run in background thread to avoid blocking
    new Thread(() -> {
      try {
        // Check if Google Drive is connected
        if (!Server.isGoogleDriveConnected()) {
          log.info("Google Drive not connected - skipping image upload for checkup {}", checkupId);
          return;
        }

        // Get checkup's Google Drive folder ID from database
        PreparedStatement checkupStmt = Server.connection.prepareStatement(
          "SELECT drive_folder_id FROM Checkup WHERE checkup_id = ?");
        checkupStmt.setString(1, checkupId);
        ResultSet checkupRs = checkupStmt.executeQuery();
        
        String checkupDriveFolderId = "";
        if (checkupRs.next()) {
          checkupDriveFolderId = checkupRs.getString("drive_folder_id");
        }
        checkupStmt.close();
        checkupRs.close();

        if (checkupDriveFolderId == null || checkupDriveFolderId.trim().isEmpty()) {
          log.warn("Checkup {} has no Google Drive folder ID - cannot upload image", checkupId);
          return;
        }

        log.info("Uploading image {} to Google Drive for checkup {}", fileName, checkupId);
        
        // Create temporary file from byte array for Google Drive upload
        java.io.File tempFile = createTempFileFromBytes(imageData, fileName);
        
        try {
          // Upload file to Google Drive using checkup's folder ID
          String uploadedFileId = Server.getGoogleDriveService().uploadFileToFolder(
            checkupDriveFolderId, tempFile, fileName
          );
          
          log.info("Image uploaded successfully to Google Drive for checkup {}: FileID={}", checkupId, uploadedFileId);
          
          // Notify dashboard
          if (ServerDashboard.getInstance() != null) {
            ServerDashboard.getInstance().addLog(
              String.format("Uploaded image %s to Google Drive for checkup %s", fileName, checkupId)
            );
          }
          
        } finally {
          // Clean up temporary file
          if (tempFile.exists()) {
            tempFile.delete();
            log.debug("Cleaned up temporary file: {}", tempFile.getAbsolutePath());
          }
        }
        
      } catch (Exception e) {
        log.error("Failed to upload image to Google Drive for checkup {}: {}", checkupId, e.getMessage());
        
        // Notify dashboard about the error
        if (ServerDashboard.getInstance() != null) {
          ServerDashboard.getInstance().addLog(
            String.format("Failed to upload image %s to Google Drive for checkup %s: %s", fileName, checkupId, e.getMessage())
          );
        }
      }
    }).start();
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
   * Uploads a checkup PDF to Google Drive asynchronously using the checkup's folder ID.
   * This method runs in a background thread to avoid blocking the PDF upload response.
   * @param checkupId The checkup ID
   * @param fileName The PDF filename (e.g., "ultrasound_result.pdf")
   * @param pdfData The PDF file as byte array
   * @param pdfType The type of PDF ("ultrasound_result" or "medserinvoice")
   */
  private void uploadCheckupPdfToGoogleDriveAsync(String checkupId, String fileName, byte[] pdfData, String pdfType) {
    // Run in background thread to avoid blocking
    new Thread(() -> {
      try {
        // Check if Google Drive is connected
        if (!Server.isGoogleDriveConnected()) {
          log.info("Google Drive not connected - skipping PDF upload for checkup {}", checkupId);
          return;
        }

        // Get checkup's Google Drive folder ID from database
        PreparedStatement checkupStmt = Server.connection.prepareStatement(
          "SELECT drive_folder_id FROM Checkup WHERE checkup_id = ?");
        checkupStmt.setString(1, checkupId);
        ResultSet checkupRs = checkupStmt.executeQuery();
        
        String checkupDriveFolderId = "";
        if (checkupRs.next()) {
          checkupDriveFolderId = checkupRs.getString("drive_folder_id");
        }
        checkupStmt.close();
        checkupRs.close();

        if (checkupDriveFolderId == null || checkupDriveFolderId.trim().isEmpty()) {
          log.warn("Checkup {} has no Google Drive folder ID - cannot upload PDF", checkupId);
          return;
        }

        log.info("Uploading PDF {} ({}) to Google Drive for checkup {}", fileName, pdfType, checkupId);
        
        // Create temporary file from byte array for Google Drive upload
        java.io.File tempFile = createTempFileFromBytes(pdfData, fileName);
        
        try {
          // Upload file to Google Drive using checkup's folder ID (with override behavior)
          String uploadedFileId = Server.getGoogleDriveService().uploadFileToFolder(
            checkupDriveFolderId, tempFile, fileName
          );
          
          log.info("PDF uploaded successfully to Google Drive for checkup {}: FileID={}", checkupId, uploadedFileId);
          
          // Notify dashboard
          if (ServerDashboard.getInstance() != null) {
            ServerDashboard.getInstance().addLog(
              String.format("Uploaded PDF %s (%s) to Google Drive for checkup %s", fileName, pdfType, checkupId)
            );
          }
          
        } finally {
          // Clean up temporary file
          if (tempFile.exists()) {
            tempFile.delete();
            log.debug("Cleaned up temporary PDF file: {}", tempFile.getAbsolutePath());
          }
        }
        
      } catch (Exception e) {
        log.error("Failed to upload PDF to Google Drive for checkup {}: {}", checkupId, e.getMessage(), e);
        
        // Notify dashboard of error
        if (ServerDashboard.getInstance() != null) {
          ServerDashboard.getInstance().addLog(
            String.format("Failed to upload PDF %s (%s) to Google Drive for checkup %s: %s", fileName, pdfType, checkupId, e.getMessage())
          );
        }
      }
    }).start();
  }

  private void broadcastQueueUpdate() {
    try {
        // --- EXISTING LOGIC FOR THE QUEUE (UNCHANGED) ---
        ResultSet rs = statement.executeQuery(
                "select a.checkup_id, a.checkup_date, c.customer_last_name, c.customer_first_name,\n" +
                        "d.doctor_first_name, d.doctor_last_name, a.suggestion, a.diagnosis, a.notes, a.status, a.customer_id, \n" +
                        "c.customer_number, c.customer_address, a.customer_weight, a.customer_height, c.customer_gender, c.customer_dob, \n" +
                        "a.checkup_type, a.conclusion, a.reCheckupDate, c.cccd_ddcn, a.heart_beat, a.blood_pressure, c.drive_url, a.doctor_ultrasound_id, a.queue_number\n" +
                        "from checkup as a\n" +
                        "join customer as c on a.customer_id = c.customer_id\n" +
                        "join Doctor D on a.doctor_id = D.doctor_id\n" +
                        "where a.status = 'ĐANG KHÁM' or a.status = 'CHỜ KHÁM'"
        );

        String[][] resultArray;
        if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the checkup table.");
            resultArray = new String[0][0];
        } else {
            // (Your existing code to process the queue results remains here)
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
                if (driveUrl == null) { driveUrl = ""; }
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

        // --- EXISTING LOGIC FOR TODAY'S PATIENT COUNT ---
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate today = LocalDate.now(zoneId);
        long startOfTodayMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long endOfTodayMillis = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1;
        String countQuery = String.format(
            "SELECT COUNT(DISTINCT a.customer_id) as patient_count FROM checkup as a WHERE a.checkup_date >= %d AND a.checkup_date <= %d",
            startOfTodayMillis, endOfTodayMillis
        );
        int totalPatientsToday = 0;
        ResultSet countRs = statement.executeQuery(countQuery);
        if (countRs.next()) {
            totalPatientsToday = countRs.getInt("patient_count");
        }

        // --- ADDED LOGIC: QUERY FOR TODAY'S RE-CHECKUP COUNT ---
        String recheckQuery = String.format(
            "SELECT COUNT(*) as recheck_count FROM checkup WHERE reCheckupDate >= %d AND reCheckupDate <= %d",
            startOfTodayMillis, endOfTodayMillis
        );
        int totalRecheckToday = 0;
        ResultSet recheckRs = statement.executeQuery(recheckQuery);
        if (recheckRs.next()) {
            totalRecheckToday = recheckRs.getInt("recheck_count");
        }
        
        // --- SEND ALL RESPONSES TO ALL CLIENTS ---
        int maxCurId = SessionManager.getMaxSessionId();
        for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
            // Send the queue update response
            UserUtil.sendPacket(sessionId, new GetCheckUpQueueUpdateResponse(resultArray));
            
            // Send the patient count response
            UserUtil.sendPacket(sessionId, new TodayPatientCountResponse(totalPatientsToday));
            
            // Send the new re-checkup count response
            UserUtil.sendPacket(sessionId, new RecheckCountResponse(totalRecheckToday));
        }
        log.info("send checkup queue update response to all clients");
        log.info("send today patient count response to all clients");
        log.info("send recheck count response to all clients");

    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }

}
