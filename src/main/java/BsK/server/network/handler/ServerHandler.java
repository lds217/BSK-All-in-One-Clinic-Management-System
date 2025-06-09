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
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;


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
                          "d.doctor_first_name, d.doctor_last_name, a.symptoms, a.diagnosis, a.notes, a.status, a.customer_id, \n" +
                          "c.customer_number, c.customer_address, c.customer_weight, c.customer_height, c.customer_gender, c.customer_dob, a.checkup_type\n" +
                          "from checkup as a\n" +
                          "join customer as c on a.customer_id = c.customer_id\n" +
                          "join Doctor D on a.doctor_id = D.doctor_id\n" +
                          "where a.status = 'PROCESSING'"
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
              String symptoms = rs.getString("symptoms");
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
                      doctorLastName + " " + doctorFirstName, symptoms,
                      diagnosis, notes, status, customerId, customerNumber, customerAddress, customerWeight, customerHeight,
                      customerGender, customerDob, checkupType
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
                          "d.doctor_first_name, d.doctor_last_name, a.symptoms, a.diagnosis, a.notes, a.status, a.customer_id, \n" +
                          "c.customer_number, c.customer_address, c.customer_weight, c.customer_height, c.customer_gender, c.customer_dob, a.checkup_type\n" +
                          "from checkup as a\n" +
                          "join customer as c on a.customer_id = c.customer_id\n" +
                          "join Doctor D on a.doctor_id = D.doctor_id\n" +
                          "where a.status = 'PROCESSING'"
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
              String symptoms = rs.getString("symptoms");
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
                      doctorLastName + " " + doctorFirstName, symptoms,
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

      if (packet instanceof GetCustomerHistoryRequest getCustomerHistoryRequest) {
        log.debug("Received GetCustomerHistoryRequest");
        try {
            ResultSet rs = statement.executeQuery(
                    "select Checkup.checkup_date, Checkup.checkup_id, Checkup.symptoms, Checkup.diagnosis, Checkup.prescription_id, Checkup.notes\n" +
                            "from Customer\n" +
                            "join Checkup on Customer.customer_id = Checkup.customer_id\n" +
                            "where Checkup.status = \"DONE\" and Customer.customer_id = " +
                            getCustomerHistoryRequest.getCustomerId() +
                            " order by checkup_date"
            );

            if (!rs.isBeforeFirst()) {
                System.out.println("No history data found in the checkup table.");
                UserUtil.sendPacket(currentUser.getSessionId(), new GetCustomerHistoryResponse(new String[0][7]));
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
                    String symptoms = rs.getString("symptoms");
                    String diagnosis = rs.getString("diagnosis");
                    String prescriptionId = rs.getString("prescription_id");
                    String notes = rs.getString("notes");
                    String result = String.join("|", checkupDate, checkupId, symptoms, diagnosis, prescriptionId, notes);
                    resultList.add(result);
                    // log.info(result);
                }

                String[] resultString = resultList.toArray(new String[0]);
                String[][] resultArray = new String[resultString.length][];
                for (int i = 0; i < resultString.length; i++) {
                    resultArray[i] = resultString[i].split("\\|");
                }

                UserUtil.sendPacket(currentUser.getSessionId(), new GetCustomerHistoryResponse(resultArray));
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
          PreparedStatement preparedStatement = Server.connection.prepareStatement(
                  "INSERT INTO Customer (customer_last_name, customer_first_name, customer_dob, customer_number, customer_address, customer_gender) VALUES (?, ?, ?, ?, ?, ?)"
          );
          preparedStatement.setString(1, addPatientRequest.getPatientLastName());
          preparedStatement.setString(2, addPatientRequest.getPatientFirstName());
          preparedStatement.setLong(3, addPatientRequest.getPatientDob());
          preparedStatement.setString(4, addPatientRequest.getPatientPhone());
          preparedStatement.setString(5, addPatientRequest.getPatientAddress());
          preparedStatement.setString(6, addPatientRequest.getPatientGender());
          preparedStatement.executeUpdate();

          // Get the last inserted ID
          PreparedStatement getIdStmt = Server.connection.prepareStatement("SELECT last_insert_rowid()");
          ResultSet rs = getIdStmt.executeQuery();
          int customerId = 0;
          if (rs.next()) {
            customerId = rs.getInt(1);
          }

          UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(true, customerId, "Thêm bệnh nhân thành công"));
        } catch (SQLException e) {
          String errorMessage = e.getMessage();
          UserUtil.sendPacket(currentUser.getSessionId(), new AddPatientResponse(false,-1, "Lỗi: " + errorMessage));
          throw new RuntimeException(e);
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
