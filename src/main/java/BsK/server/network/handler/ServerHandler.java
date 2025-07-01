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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.Utf8FrameValidator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import lombok.extern.slf4j.Slf4j;

import BsK.common.entity.Medicine;
import BsK.common.entity.Service;
import BsK.common.entity.PatientHistory;
import BsK.common.entity.Template;

import static BsK.server.Server.statement;


@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private static final String IMAGE_DB_PATH = "img_db";

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
                          "c.customer_number, c.customer_address, c.customer_weight, c.customer_height, c.customer_gender, c.customer_dob, a.checkup_type, a.conclusion\n" +
                          "from checkup as a\n" +
                          "join customer as c on a.customer_id = c.customer_id\n" +
                          "join Doctor D on a.doctor_id = D.doctor_id\n" +
                          "where a.status = 'ĐANG KHÁM'"
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

              String result = String.join("|", checkupId,
                      checkupDate, customerLastName, customerFirstName,
                      doctorLastName + " " + doctorFirstName, suggestion,
                      diagnosis, notes, status, customerId, customerNumber, customerAddress, customerWeight, customerHeight,
                      customerGender, customerDob, checkupType, conclusion
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
        try {
          ResultSet rs = statement.executeQuery(
                  "select a.checkup_id, a.checkup_date, c.customer_last_name, c.customer_first_name,\n" +
                          "d.doctor_first_name, d.doctor_last_name, a.suggestion, a.diagnosis, a.notes, a.status, a.customer_id, \n" +
                          "c.customer_number, c.customer_address, c.customer_weight, c.customer_height, c.customer_gender, c.customer_dob, a.checkup_type\n" +
                          "from checkup as a\n" +
                          "join customer as c on a.customer_id = c.customer_id\n" +
                          "join Doctor D on a.doctor_id = D.doctor_id\n" +
                          "where a.status = 'ĐANG KHÁM'"
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


              String result = String.join("|", checkupId,
                      checkupDate, customerLastName, customerFirstName,
                      doctorLastName + " " + doctorFirstName, suggestion,
                      diagnosis, notes, status, customerId, customerNumber, customerAddress, customerWeight, customerHeight,
                      customerGender, customerDob, checkupType
              );

              resultList.add(result);

              // log.info(result);
            }

            String[] resultString = resultList.toArray(new String[0]);
            String[][] resultArray = new String[resultString.length][];
            for (int i = 0; i < resultString.length; i++) {
              resultArray[i] = resultString[i].split("\\|");
            }
            //send to all
            int maxCurId = SessionManager.getMaxSessionId();
            for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
              UserUtil.sendPacket(sessionId, new GetCheckUpQueueUpdateResponse(resultArray));
            }
          }
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }


      // Get general doctor info
      if (packet instanceof GetDoctorGeneralInfo) {
        log.debug("Received GetDoctorGeneralInfo");
        try {
          ResultSet rs = statement.executeQuery(
                  "select concat(doctor_last_name, ' ', doctor_first_name) from Doctor"
          );

            if (!rs.isBeforeFirst()) {
                System.out.println("No data found in the doctor table.");
            } else {
              ArrayList<String> resultList = new ArrayList<>();
              while (rs.next()) {
                String doctorName = rs.getString(1);
                resultList.add(doctorName);
              }
              String[] resultString = resultList.toArray(new String[0]);
              UserUtil.sendPacket(currentUser.getSessionId(), new GetDoctorGeneralInfoResponse(resultString));
              log.info("Send response to client GetDoctorGeneralInfo");
          }
        }
        catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetPatientHistoryRequest getPatientHistoryRequest) {
        log.debug("Received GetPatientHistoryRequest");
        try {
            ResultSet rs = statement.executeQuery(
                    "select Checkup.checkup_date, Checkup.checkup_id, Checkup.suggestion, Checkup.diagnosis, Checkup.prescription_id, Checkup.notes\n" +
                            "from Customer\n" +
                            "join Checkup on Customer.customer_id = Checkup.customer_id\n" +
                            "where Checkup.status = \"DONE\" and Customer.customer_id = " +
                            getPatientHistoryRequest.getPatientId() +
                            " order by checkup_date"
            );

            if (!rs.isBeforeFirst()) {
                System.out.println("No history data found in the checkup table.");
                UserUtil.sendPacket(currentUser.getSessionId(), new GetPatientHistoryResponse(new String[0][0]));
            } else {
                ArrayList<String> resultList = new ArrayList<>();
                while (rs.next()) {
                    String checkupDate = rs.getString("checkup_date");
                    long checkupDateLong = Long.parseLong(checkupDate);
                    Timestamp timestamp = new Timestamp(checkupDateLong);
                    Date date = new Date(timestamp.getTime()); // Needed to recode
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    checkupDate = sdf.format(date);
                    String checkupId = rs.getString("checkup_id");
                    String suggestion = rs.getString("suggestion");
                    String diagnosis = rs.getString("diagnosis");
                    String prescriptionId = rs.getString("prescription_id");
                    String notes = rs.getString("notes");
                    String result = String.join("|", checkupDate, checkupId, suggestion, diagnosis, prescriptionId, notes);
                    resultList.add(result);
                    // log.info(result);
                }

                String[] resultString = resultList.toArray(new String[0]);
                String[][] resultArray = new String[resultString.length][];
                for (int i = 0; i < resultString.length; i++) {
                    resultArray[i] = resultString[i].split("\\|");
                }

                UserUtil.sendPacket(currentUser.getSessionId(), new GetPatientHistoryResponse(resultArray));
            }

        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetMedInfoRequest getMedInfoRequest) {
        log.debug("Received GetMedInfoRequest");
        try {
          ResultSet rs = statement.executeQuery(
                  "select med_id, med_name, med_company, med_description, quantity, med_unit, med_selling_price\n" +
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


                String result = String.join("|",medId, medName, medCompany, medDescription, quantity, medUnit,
                        medSellingPrice);
                resultList.add(result);
            }

            String[] resultString = resultList.toArray(new String[0]);
            String[][] resultArray = new String[resultString.length][];
            for (int i = 0; i < resultString.length; i++) {
              resultArray[i] = resultString[i].split("\\|");
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
          ResultSet rs = statement.executeQuery(
                  "SELECT customer_id, customer_last_name, customer_first_name, customer_dob, customer_number, " +
                          "customer_address, customer_address, customer_gender\n" +
                          "FROM Customer\n" +
                          "ORDER BY customer_id DESC\n" +
                          "LIMIT 20;");
          if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the customer table.");
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

              String result = String.join("|", customerId, customerLastName + " " + customerFirstName,
                      year, customerNumber, customerAddress, customerGender, customerDob);
              resultList.add(result);
            }


            String[] resultString = resultList.toArray(new String[0]);
            String[][] resultArray = new String[resultString.length][];
            for (int i = 0; i < resultString.length; i++) {
              resultArray[i] = resultString[i].split("\\|");
            }

            UserUtil.sendPacket(currentUser.getSessionId(), new GetRecentPatientResponse(resultArray));
          }
        } catch (SQLException e) {
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

      if (packet instanceof GetDistrictRequest getDistrictRequest) {
        log.debug("Received GetDistrictRequest");
        try{

          PreparedStatement preparedStatement = Server.connection.prepareStatement(
                  "SELECT districts.code, districts.name " +
                          "                          FROM districts " +
                          "                          WHERE province_code = ? " +
                          "                          ORDER BY districts.name"
          );
          preparedStatement.setString(1, getDistrictRequest.getProvinceId());

          ResultSet rs = preparedStatement.executeQuery();


          if (!rs.isBeforeFirst()) {
                System.out.println("No data found in the district table.");
            } else {
                ArrayList<String> resultList = new ArrayList<>();
                HashMap<String, String> districtIdMap = new HashMap<>();
                resultList.add("Quận/Huyện");
                while (rs.next()) {
                String districtId = rs.getString("code");
                String districtName = rs.getString("name");
                districtIdMap.put(districtName, districtId);
                resultList.add(districtName);
                }

                String[] resultString = resultList.toArray(new String[0]);

                UserUtil.sendPacket(currentUser.getSessionId(), new GetDistrictResponse(resultString, districtIdMap));
            }
            } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof GetWardRequest getWardRequest) {
        log.debug("Received GetWardRequest");

        try{
          PreparedStatement preparedStatement = Server.connection.prepareStatement(
                  "SELECT wards.code, wards.name \n" +
                          "                          FROM wards\n" +
                          "                          WHERE district_code = ? " +
                          "                          ORDER BY wards.name"
          );
          preparedStatement.setString(1, getWardRequest.getDistrictId());

          ResultSet rs = preparedStatement.executeQuery();


          if (!rs.isBeforeFirst()) {
            System.out.println("No data found in the district table.");
          } else {
            ArrayList<String> resultList = new ArrayList<>();
            HashMap<String, String> districtIdMap = new HashMap<>();
            resultList.add("Xã/Phường");
            while (rs.next()) {
              String districtId = rs.getString("code");
              String districtName = rs.getString("name");
              districtIdMap.put(districtName, districtId);
              resultList.add(districtName);
            }

            String[] resultString = resultList.toArray(new String[0]);
            UserUtil.sendPacket(currentUser.getSessionId(), new GetWardResponse(resultString));
          }
        } catch (SQLException e) {
          UserUtil.sendPacket(currentUser.getSessionId(), new ErrorResponse(Error.SQL_EXCEPTION));
          throw new RuntimeException(e);
        }
      }

      if (packet instanceof AddPatientRequest addPatientRequest) {
        log.debug("Received AddPatientRequest");
        try {
          // Disable auto-commit for transaction
          Server.connection.setAutoCommit(false);
          
          PreparedStatement preparedStatement = Server.connection.prepareStatement(
                  "INSERT INTO Customer (customer_last_name, customer_first_name, customer_dob, customer_number, customer_address, customer_gender) VALUES (?, ?, ?, ?, ?, ?)",
                  PreparedStatement.RETURN_GENERATED_KEYS // Use this instead of separate query
          );
          preparedStatement.setString(1, addPatientRequest.getPatientLastName());
          preparedStatement.setString(2, addPatientRequest.getPatientFirstName());
          preparedStatement.setLong(3, addPatientRequest.getPatientDob());
          preparedStatement.setString(4, addPatientRequest.getPatientPhone());
          preparedStatement.setString(5, addPatientRequest.getPatientAddress());
          preparedStatement.setString(6, addPatientRequest.getPatientGender());
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

          UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(true, customerId, "Thêm bệnh nhân thành công"));
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

          // First query: Insert Checkup
          PreparedStatement checkupStmt = Server.connection.prepareStatement(
                  "INSERT INTO Checkup (customer_id, doctor_id, checkup_type) VALUES (?, ?, ?)");
          checkupStmt.setInt(1, addCheckupRequest.getCustomerId());
          checkupStmt.setInt(2, addCheckupRequest.getDoctorId());
          checkupStmt.setString(3, addCheckupRequest.getCheckupType());
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

          // Commit the transaction
          Server.connection.commit();

          UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(true, "Thêm bệnh nhân thành công"));
        }
        catch (SQLException e) {
          try {
            // Rollback on error
            Server.connection.rollback();
          } catch (SQLException rollbackEx) {
            log.error("Error during rollback", rollbackEx);
          }
          String errorMessage = e.getMessage();
          UserUtil.sendPacket(currentUser.getSessionId(), new AddCheckupResponse(false, "Lỗi: " + errorMessage));
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
        log.debug("Received CallPatientRequest to call patient{}", callPatientRequest.getPatientId());
        int patientId = callPatientRequest.getPatientId();
        int roomId = callPatientRequest.getRoomId();
        Status status = callPatientRequest.getStatus();
        // send to all clients
        int maxCurId = SessionManager.getMaxSessionId();
        for (int sessionId = 1; sessionId <= maxCurId; sessionId++) {
          UserUtil.sendPacket(sessionId, new CallPatientResponse(patientId, roomId, status));
        }
      }

      if (packet instanceof SaveCheckupRequest saveCheckupRequest) {
        log.debug("Received SaveCheckupRequest to save checkup {}", saveCheckupRequest.getCheckupId());
        
        Connection conn = null;
        try {
          conn = Server.connection;
          // Start transaction
          conn.setAutoCommit(false);
          
          // 1. Insert/Update Customer
          String customerSql = """
            INSERT INTO Customer (
                customer_id, customer_first_name, customer_last_name, customer_dob,
                customer_gender, customer_address, customer_number,
                customer_weight, customer_height
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(customer_id) DO UPDATE SET
                customer_first_name = excluded.customer_first_name,
                customer_last_name = excluded.customer_last_name,
                customer_dob = excluded.customer_dob,
                customer_gender = excluded.customer_gender,
                customer_address = excluded.customer_address,
                customer_number = excluded.customer_number,
                customer_weight = excluded.customer_weight,
                customer_height = excluded.customer_height
            """;
          
          PreparedStatement customerStmt = conn.prepareStatement(customerSql);
          customerStmt.setString(1, saveCheckupRequest.getCustomerId());
          customerStmt.setString(2, saveCheckupRequest.getCustomerFirstName());
          customerStmt.setString(3, saveCheckupRequest.getCustomerLastName());
          customerStmt.setString(4, saveCheckupRequest.getCustomerDob());
          customerStmt.setString(5, saveCheckupRequest.getCustomerGender());
          customerStmt.setString(6, saveCheckupRequest.getCustomerAddress());
          customerStmt.setString(7, saveCheckupRequest.getCustomerNumber());
          customerStmt.setString(8, saveCheckupRequest.getCustomerWeight());
          customerStmt.setString(9, saveCheckupRequest.getCustomerHeight());
          customerStmt.executeUpdate();
          log.info("Customer saved successfully");
          // 2. Generate prescription_id if we have medicine prescriptions
          String prescriptionId = null;
          if (saveCheckupRequest.getMedicinePrescription() != null && 
              saveCheckupRequest.getMedicinePrescription().length > 0) {
            
            // Get next prescription_id
            String getNextIdSql = "SELECT COALESCE(MAX(prescription_id), 0) + 1 as next_id FROM MedicineOrder";
            PreparedStatement nextIdStmt = conn.prepareStatement(getNextIdSql);
            ResultSet rs = nextIdStmt.executeQuery();
            if (rs.next()) {
              prescriptionId = rs.getString("next_id");
            }
            rs.close();
            nextIdStmt.close();
          }

          // 3. Insert/Update Checkup
          String checkupSql = """
            INSERT INTO Checkup (
                checkup_id, customer_id, doctor_id, checkup_date,
                suggestion, diagnosis, prescription_id, notes, status, checkup_type, conclusion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(checkup_id) DO UPDATE SET
                suggestion = excluded.suggestion,
                diagnosis = excluded.diagnosis,
                prescription_id = excluded.prescription_id,
                notes = excluded.notes,
                status = excluded.status,
                checkup_type = excluded.checkup_type,
                conclusion = excluded.conclusion
            """;
          
          PreparedStatement checkupStmt = conn.prepareStatement(checkupSql);
          checkupStmt.setString(1, saveCheckupRequest.getCheckupId());
          checkupStmt.setString(2, saveCheckupRequest.getCustomerId());
          checkupStmt.setString(3, saveCheckupRequest.getDoctorId());
          checkupStmt.setString(4, saveCheckupRequest.getCheckupDate());
          checkupStmt.setString(5, saveCheckupRequest.getSuggestions());
          checkupStmt.setString(6, saveCheckupRequest.getDiagnosis());
          checkupStmt.setString(7, prescriptionId);
          checkupStmt.setString(8, saveCheckupRequest.getNotes());
          checkupStmt.setString(9, saveCheckupRequest.getStatus());
          checkupStmt.setString(10, saveCheckupRequest.getCheckupType());
          checkupStmt.setString(11, saveCheckupRequest.getConclusion());
          checkupStmt.executeUpdate();
          log.info("Prescription ID generated or edited: {}", prescriptionId);
          log.info("Checkup saved successfully");

          // 4. Handle Medicine Prescriptions
          // First, delete all existing items for this checkup to handle edits/removals
          try (PreparedStatement deleteOrderItemsStmt = conn.prepareStatement("DELETE FROM OrderItem WHERE checkup_id = ?")) {
              deleteOrderItemsStmt.setString(1, saveCheckupRequest.getCheckupId());
              deleteOrderItemsStmt.executeUpdate();
          }
          try (PreparedStatement deleteMedicineOrderStmt = conn.prepareStatement("DELETE FROM MedicineOrder WHERE checkup_id = ?")) {
              deleteMedicineOrderStmt.setString(1, saveCheckupRequest.getCheckupId());
              deleteMedicineOrderStmt.executeUpdate();
          }
          log.info("Cleared previous medicine prescription for checkup_id: {}", saveCheckupRequest.getCheckupId());

          if (saveCheckupRequest.getMedicinePrescription() != null && 
              saveCheckupRequest.getMedicinePrescription().length > 0 && prescriptionId != null) {
            
            // Calculate total amount for medicine order
            double totalMedicineAmount = 0;
            for (String[] medicine : saveCheckupRequest.getMedicinePrescription()) {
              if (medicine.length > 8) {
                try {
                  totalMedicineAmount += Double.parseDouble(medicine[8]); // total_price
                } catch (NumberFormatException e) {
                  log.warn("Invalid medicine total price: {}", medicine[8]);
                }
              }
            }
            
            // Insert/Update MedicineOrder
            String medicineOrderSql = """
              INSERT INTO MedicineOrder (
                  prescription_id, checkup_id, customer_id,
                  total_amount, payment_method, status,
                  payment_status, processed_by, notes, datetime
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
              ON CONFLICT(prescription_id, checkup_id) DO UPDATE SET
                  customer_id = excluded.customer_id,
                  total_amount = excluded.total_amount,
                  payment_method = excluded.payment_method,
                  status = excluded.status,
                  payment_status = excluded.payment_status,
                  processed_by = excluded.processed_by,
                  notes = excluded.notes,
                  datetime = CURRENT_DATE
              """;
            
            PreparedStatement medicineOrderStmt = conn.prepareStatement(medicineOrderSql);
            medicineOrderStmt.setString(1, prescriptionId);
            medicineOrderStmt.setString(2, saveCheckupRequest.getCheckupId());
            medicineOrderStmt.setString(3, saveCheckupRequest.getCustomerId());
            medicineOrderStmt.setDouble(4, totalMedicineAmount);
            medicineOrderStmt.setString(5, ""); // payment_method
            medicineOrderStmt.setString(6, "Pending"); // status
            medicineOrderStmt.setString(7, "Unpaid"); // payment_status
            medicineOrderStmt.setString(8, ""); // processed_by
            medicineOrderStmt.setString(9, ""); // notes
            medicineOrderStmt.executeUpdate();
            log.info("MedicineOrder saved successfully");
            // Insert OrderItems
            String orderItemSql = """
              INSERT INTO OrderItem (
                  prescription_id, med_id, quantity_ordered,
                  dosage, duration, price_per_unit, total_price, checkup_id, notes
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
              ON CONFLICT(order_item_id) DO UPDATE SET
                  quantity_ordered = excluded.quantity_ordered,
                  dosage = excluded.dosage,
                  duration = excluded.duration,
                  price_per_unit = excluded.price_per_unit,
                  total_price = excluded.total_price,
                  checkup_id = excluded.checkup_id,
                  notes = excluded.notes
              """;
            log.info("OrderItem saved successfully");
            PreparedStatement orderItemStmt = conn.prepareStatement(orderItemSql);
            
            for (String[] medicine : saveCheckupRequest.getMedicinePrescription()) {
              if (medicine.length >= 10) {
                orderItemStmt.setString(1, prescriptionId);
                orderItemStmt.setString(2, medicine[0]); // med_id
                orderItemStmt.setString(3, medicine[2]); // quantity
                
                // Build dosage string from morning, noon, evening
                String dosage = String.format("Sáng %s, Trưa %s, Chiều %s", 
                    medicine[4], medicine[5], medicine[6]);
                orderItemStmt.setString(4, dosage);
                orderItemStmt.setString(5, ""); // duration - could be calculated or added to request
                orderItemStmt.setString(6, medicine[7]); // unit_price
                orderItemStmt.setString(7, medicine[8]); // total_price
                orderItemStmt.setString(8, saveCheckupRequest.getCheckupId());
                orderItemStmt.setString(9, medicine[9]); // notes
                orderItemStmt.addBatch();
              }
            }
            orderItemStmt.executeBatch();
          }
          log.info("Medicine prescriptions saved successfully");

          // 5. Handle Service Prescriptions
          // First, delete all existing services for this checkup
          try (PreparedStatement deleteServiceStmt = conn.prepareStatement("DELETE FROM CheckupService WHERE checkup_id = ?")) {
              deleteServiceStmt.setString(1, saveCheckupRequest.getCheckupId());
              deleteServiceStmt.executeUpdate();
          }
          log.info("Cleared previous service prescription for checkup_id: {}", saveCheckupRequest.getCheckupId());

          if (saveCheckupRequest.getServicePrescription() != null && 
              saveCheckupRequest.getServicePrescription().length > 0) {
            
            String serviceSql = """
              INSERT INTO CheckupService (
                  checkup_id, service_id, quantity,
                  total_cost, status, checkup_date
              ) VALUES (?, ?, ?, ?, ?, CURRENT_DATE)
              ON CONFLICT(order_id) DO UPDATE SET
                  quantity = excluded.quantity,
                  total_cost = excluded.total_cost,
                  status = excluded.status
              """;
            
            PreparedStatement serviceStmt = conn.prepareStatement(serviceSql);
            
            for (String[] service : saveCheckupRequest.getServicePrescription()) {
              if (service.length >= 6) {
                serviceStmt.setString(1, saveCheckupRequest.getCheckupId());
                serviceStmt.setString(2, service[0]); // service_id
                serviceStmt.setString(3, service[2]); // quantity
                serviceStmt.setString(4, service[4]); // total_cost
                serviceStmt.setString(5, "PENDING"); // status
                
                serviceStmt.addBatch();
              }
            }
            serviceStmt.executeBatch();
          }
          log.info("Service prescriptions saved successfully");
          // Commit transaction
          conn.commit();
          log.info("Successfully saved checkup {} with all related data", saveCheckupRequest.getCheckupId());
          
          UserUtil.sendPacket(currentUser.getSessionId(), new SaveCheckupRes(true, "Checkup saved successfully"));
          
        } catch (Exception e) {
          log.error("Error saving checkup transaction", e);
          // Rollback transaction on error
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
          // Restore auto-commit
          if (conn != null) {
            try {
              conn.setAutoCommit(true);
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
                  OI.notes
              FROM OrderItem OI
              JOIN Medicine M ON OI.med_id = M.med_id
              WHERE OI.checkup_id = ?
              """
          );
          medStmt.setString(1, checkupId);
          ResultSet medRs = medStmt.executeQuery();
          
          ArrayList<String[]> medList = new ArrayList<>();
          while(medRs.next()) {
            String[] med = new String[10];
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
        try {
          PreparedStatement templateStmt = Server.connection.prepareStatement(
            """
            INSERT INTO CheckupTemplate (template_gender, template_name, template_title, photo_num, print_type, content, conclusion, suggestion, diagnosis)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    rs.getString("diagnosis")
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
                UPDATE CheckupTemplate SET template_gender = ?, template_name = ?, template_title = ?, photo_num = ?, print_type = ?, content = ?, conclusion = ?, suggestion = ?, diagnosis = ?
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
            stmt.setInt(10, template.getTemplateId());
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

      if (packet instanceof UploadCheckupImageRequest request) {
        log.info("Received UploadCheckupImageRequest for checkupId: {}", request.getCheckupId());

        String checkupId = request.getCheckupId();
        String fileName = request.getFileName();
        byte[] imageData = request.getImageData();

        if (checkupId == null || checkupId.trim().isEmpty()) {
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "CheckupID is null or empty. Cannot save image.", fileName));
            return;
        }

        try {
            // 1. Define and create storage directory
            Path storageDir = Paths.get(IMAGE_DB_PATH, checkupId.trim());
            Files.createDirectories(storageDir); // Create dirs if they don't exist

            // 2. Save the file
            Path filePath = storageDir.resolve(fileName);
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(imageData);
            }
            String savedPath = filePath.toString().replace("\\", "/");
            log.info("Successfully saved image to {}", savedPath);

            // 3. TODO: Store file path in database, linking it to the checkupId
            // A proper implementation would have a 'CheckupImages' table and an INSERT query here.

            // 4. Send success response
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(true, "Image uploaded successfully to " + savedPath, fileName));

        } catch (IOException e) {
            log.error("Failed to save uploaded image for checkupId {}: {}", checkupId, e.getMessage());
            UserUtil.sendPacket(currentUser.getSessionId(), new UploadCheckupImageResponse(false, "Server failed to save image: " + e.getMessage(), fileName));
        }
      }
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
}
