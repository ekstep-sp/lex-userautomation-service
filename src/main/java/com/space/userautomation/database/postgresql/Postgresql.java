package com.space.userautomation.database.postgresql;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.postgresql.util.PSQLException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static String tableName_userAutocomplete = System.getenv("tableName_userAutocomplete");
    private static String tableName_user = System.getenv("tableName_user");
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
        query.append("' AND root_org = '");
        query.append(userData.get("root_org"));
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
    
    public ResponseEntity<JSONObject> deleteUserDataFromUserAutocomplete(Map<String, Object> userData){
        ProjectLogger.log("Request recieved for deleting user data from user automation table "+ userData.get("user_id"), LoggerEnum.INFO.name());
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append( schemaName_postgresql + "." + tableName_userAutocomplete );
        query.append(" WHERE wid = '");
        query.append(userData.get("wid_user") + "'");
        query.append(" AND email = '");
        query.append(userData.get("email"));
        query.append("';");
        try  {
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
           int successcount = pst.executeUpdate();
            if(successcount > 0){
                return response.getResponse("User data deleted successfully from user automation ", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
            }
            else{
                return response.getResponse("User data  could not be deleted ", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
            }
        }
        catch (PSQLException ex){
            ProjectLogger.log("PSQL exception while deleting user from user automation"+ ex, LoggerEnum.ERROR.name());
            return response.getResponse("PSQL exception while deleting user from user automation", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while deleting the data in postgresql from user automation table", ex, LoggerEnum.ERROR.name());
            return response.getResponse("Failed to delete user data from user automation", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }
    //delete the user data from user table in postgresql
    public ResponseEntity<JSONObject> deleteUserDataFromUserTable(Map<String, Object> userData){
        ProjectLogger.log("Request recieved for deleting user data from user table with userid "+ userData.get("user_id"), LoggerEnum.INFO.name());
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append( schemaName_postgresql + "." + tableName_user );
        query.append(" WHERE email = '");
        query.append(userData.get("email") + "'");
        query.append(" AND kid = '" + userData.get("user_id") +"'");
        query.append(" AND root_org = '" + userData.get("root_org") + "'");
        query.append(" AND org = '" + userData.get("organisation") );
        query.append("';");
        try  {
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
           int successCount =  pst.executeUpdate();
           if(successCount > 0){
               return response.getResponse("User data deleted successfully from user table ", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
           }
           else{
               return response.getResponse("User data could not be deleted ", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);

           }
        }
        catch (PSQLException ex){
            ProjectLogger.log("PSQL exception while deleting user from user table "+ ex, LoggerEnum.ERROR.name());
            return response.getResponse("PSQL exception while deleting user from user table", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while deleting the data in postgresql from user table", ex, LoggerEnum.ERROR.name());
            return response.getResponse("Failed to delete user data from user table", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }
    //get email details of user for user details api.
    public Object  getUserDetails(User userData, String dataToBeRetrieved){
        Object responseData = new Object();
        ProjectLogger.log("Request recieved to get user details from table user.", LoggerEnum.ERROR.name());

        StringBuilder query = new StringBuilder();
        query.append("SELECT " );
        query.append(dataToBeRetrieved + " FROM ");
        query.append( tableName_user );
        query.append(" WHERE " + " wid = '" + userData.getWid_user() + "'");
        query.append(";");
        try{
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
            ResultSet resultSet =  pst.executeQuery();
            while (resultSet.next()) {
              responseData = resultSet.getString(dataToBeRetrieved);
            }

        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while updating organisation for user" + e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while updating organisation for user"+ ex, LoggerEnum.ERROR.name());
        }
        return responseData;
    }
//getting all users from userTable
    public Object  getAllUserList(User userData){
        JSONArray json = new JSONArray();
        ProjectLogger.log("Request recieved to get all user list  from table user.", LoggerEnum.ERROR.name());
        StringBuilder query = new StringBuilder();
        query.append("SELECT " );
        query.append( "*" + " FROM ");
//        query.append( tableName_user );
        query.append(schemaName_postgresql + "." + tableName_user );
    
        query.append(" WHERE " + " root_org = '" + userData.getRoot_org() + "'");
        query.append(" AND " + "org = '" + userData.getOrganisation() + "'");
        query.append(";");
        try{
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
            ResultSet resultSet =  pst.executeQuery();
    
            ResultSetMetaData rsmd = resultSet.getMetaData();
            while(resultSet.next()) {
                int numColumns = rsmd.getColumnCount();
                JSONObject obj = new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsmd.getColumnName(i);
                    obj.put(column_name, resultSet.getObject(column_name));
                }
                json.add(obj);
            }
            return json;
        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while retieving list of users from user table" + e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while retieving list of users from user table"+ ex, LoggerEnum.ERROR.name());
        }
        return json;
    }
    
    //update department_name for user table
    public int  updateUserDetails(User userData){
        ProjectLogger.log("Request recieved to update the user organisation details.", LoggerEnum.ERROR.name());
        int successcount = -1;
        StringBuilder query = new StringBuilder();
        query.append("UPDATE " );
        query.append( tableName_user );
        query.append(" SET " + " department_name = '" + userData.getOrganisation() + "'");
        query.append(" WHERE " + " wid = '" + userData.getWid_user() + "'");
        query.append(";");
        try{
            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
           successcount = pst.executeUpdate();
            return successcount;
        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while updating organisation for user" + e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while updating organisation for user" + ex, LoggerEnum.ERROR.name());
        }
        return successcount;
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
