package com.space.userautomation.database.postgresql;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import org.json.simple.JSONObject;
import org.postgresql.util.PSQLException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Postgresql {

    static Connection con;
    private static String schemaName_postgresql = System.getenv("schemaName_postgresql");
    private static String tableName_postgresql = System.getenv("tableName_postgresql");
    private static String databaseName_postgresql = System.getenv("databaseName_postgresql");
    private static String url = System.getenv("postgresql_url") ;
    private static String user = System.getenv("postgresql_name") ;
    private static String password = System.getenv("postgresql_password") ;
    Response response = new Response();

    static{
        try {
            con = DriverManager.getConnection(url, user, password);
            ProjectLogger.log("connecting to postgresql",LoggerEnum.INFO.name());
            shutDownHook();
        } catch (SQLException sqlException) {
            ProjectLogger.log("Failed to connect to Postgresql" + sqlException, LoggerEnum.FATAL.name());
            System.exit(-1);
        }
        catch(Exception exception){
            ProjectLogger.log("Failed to connect to Postgresql" + exception, LoggerEnum.FATAL.name());
            System.exit(-1);
        }
    }
    public static void shutDownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new PostgresqlConnection());
    }

    static class PostgresqlConnection extends Thread {
        @Override
        public void run() {
            ProjectLogger.log("Killing postgresql connection", LoggerEnum.INFO.name());
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
                ProjectLogger.log("failed to kill postgresql Connection.", e, LoggerEnum.FATAL.name());
            }
        }
    }

    public ResponseEntity<JSONObject> insertUserRoles(Map<String, Object> userData){
        ProjectLogger.log("Request recieved for insert user role with user id "+ userData.get("user_id"), LoggerEnum.INFO.name());
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(schemaName_postgresql + "." + tableName_postgresql + "(");
        query.append("root_org, user_id, role, updated_on, updated_by");
        query.append(")" + "VALUES" + "(");
        for (Map.Entry<String, Object> entry : userData.entrySet()) {
            query.append("?" + ",");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(");");
  
        try  {
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
            pst.setString(1, userData.get("root_org").toString());
            pst.setString(2,userData.get("user_id").toString());
            pst.setString(3, userData.get("role").toString());
            pst.setTimestamp(4, (Timestamp) userData.get("updated_on"));
            pst.setString(5,userData.get("updated_by").toString());
            pst.executeUpdate();
            return response.getResponse("User Role created successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
        }
        catch (PSQLException ex){
            ProjectLogger.log("User role already exists"+ ex, LoggerEnum.ERROR.name());
            return response.getResponse("User Role already exists", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while processing the data in postgresql", ex, LoggerEnum.ERROR.name());
            return response.getResponse("User Role cannot be updated", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }

    }

    public ResponseEntity<JSONObject> deleteUserRole(Map<String, Object> userData){
        ProjectLogger.log("Request recieved for deleting user role with user id "+ userData.get("user_id"), LoggerEnum.INFO.name());
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append(schemaName_postgresql + "." + tableName_postgresql );
        query.append(" WHERE user_id = '");
        query.append(userData.get("user_id"));
        query.append("' AND");
        query.append(" role = '");
        query.append(userData.get("role"));
        query.append("';");
        try  {
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
            pst.executeUpdate();
            return response.getResponse("User Role deleted successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
        }
        catch (PSQLException ex){
            ProjectLogger.log("PSQL exception while deleting user"+ ex, LoggerEnum.ERROR.name());
            return response.getResponse("PSQL exception ", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while deleting the data in postgresql", ex, LoggerEnum.ERROR.name());
            return response.getResponse("User Role cannot be deleted", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }
    
    
    public List<String> getUserRoles(Map<String, Object> userData) {
        List<String> role = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT role FROM ");
        query.append(schemaName_postgresql + "." + tableName_postgresql + " ");
        query.append("WHERE user_id = '");
        query.append(userData.get("user_id"));
        query.append("' AND root_org = '");
        query.append(userData.get("root_org"));
        query.append("';");
        try {
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
           ResultSet resultSet =  pst.executeQuery();
            while (resultSet.next()) {
                role.add(resultSet.getString("role"));
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured while retrieving user role for the given userId", ex, LoggerEnum.ERROR.name());
        }
        return role;
    }
    
    
    public Timestamp getTimestampValue(){
        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT CURRENT_TIMESTAMP;")) {
            if (rs.next()) {
                ProjectLogger.log("Connected to postgresql successfully with version" + rs.getString(1), LoggerEnum.INFO.name());
          return (rs.getTimestamp(1));
  
            }

        } catch (SQLException ex) {
            ProjectLogger.log("Failed to connect to postgresql"+ ex, LoggerEnum.ERROR.name());
        }
        return null;
    }
    
    public Boolean healthCheck() {
        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {
            if (rs.next()) {
                ProjectLogger.log("Connected to postgresql successfully with version" + rs.getString(1), LoggerEnum.INFO.name());
                return true;
            }
        } catch (SQLException ex) {
            ProjectLogger.log("Failed to connect to postgresql"+ ex, LoggerEnum.ERROR.name());
        }
        return  false;
    }
}
