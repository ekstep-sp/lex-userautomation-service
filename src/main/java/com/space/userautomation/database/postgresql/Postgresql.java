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
import java.util.*;

public class Postgresql {
    
    private static String schemaName_postgresql = System.getenv("schemaName_postgresql");
    private static String tableName_postgresql = System.getenv("tableName_postgresql");
    private static String databaseName_postgresql = System.getenv("databaseName_postgresql");
    private static String url = System.getenv("postgresql_url") ;
    private static String user = System.getenv("postgresql_name") ;
    private static String password = System.getenv("postgresql_password") ;
    private static String tableName_userAutocomplete = System.getenv("tableName_userAutocomplete");
    private static String tableName_user = System.getenv("tableName_user");
    Response response = new Response();
    
    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            ProjectLogger.log("connected to postgresql succesfully",LoggerEnum.INFO.name());
        } catch (SQLException sqlException) {
            ProjectLogger.log("Failed to connect to Postgresql" + sqlException, LoggerEnum.FATAL.name());
            System.exit(-1);
        }
        catch(Exception exception){
            ProjectLogger.log("Failed to connect to Postgresql" + exception, LoggerEnum.FATAL.name());
            System.exit(-1);
        }
        return conn;
    }
    
    public ResponseEntity<JSONObject> insertUserRoles(Map<String, Object> userData) throws SQLException {
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
        try (Connection conn = connect();
             PreparedStatement pst = conn.prepareStatement(String.valueOf(query))) { 
//            PreparedStatement pst = con.prepareStatement(String.valueOf(query));
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
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query))){
            pst.executeUpdate();
            return response.getResponse("User Role deleted successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
        }
        catch (PSQLException ex){
            ProjectLogger.log("PSQL exception while deleting user"+ ex, LoggerEnum.ERROR.name());
            return response.getResponse("PSQL exception ", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while deleting the data in postgresql"+ Arrays.toString(ex.getStackTrace()) + ex, LoggerEnum.ERROR.name());
            return response.getResponse("User Role cannot be deleted", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }
    
    public ResponseEntity<JSONObject> deleteUserDataFromUserAutocomplete(Map<String, Object> userData){
        ProjectLogger.log("Request recieved for deleting user data from user automation table "+ userData.get("user_id"), LoggerEnum.INFO.name());
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append( tableName_userAutocomplete );
        query.append(" WHERE wid = '");
        query.append(userData.get("wid_user") + "'");
        query.append(" AND email = '");
        query.append(userData.get("email"));
        query.append("';");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query))){
           int successcount = pst.executeUpdate();
            if(successcount > 0){
                return response.getResponse("User data deleted successfully from user automation ", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
            }
            else{
                return response.getResponse("User data  could not be deleted ", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
            }
        }
        catch (PSQLException ex){
            ProjectLogger.log("PSQL exception while deleting user from user automation"+ Arrays.toString(ex.getStackTrace()) +  ex, LoggerEnum.ERROR.name());
            return response.getResponse("PSQL exception while deleting user from user automation", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while deleting the data in postgresql from user automation table"  + Arrays.toString(ex.getStackTrace()) , ex, LoggerEnum.ERROR.name());
            return response.getResponse("Failed to delete user data from user automation", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }
    //delete the user data from user table in postgresql
    public ResponseEntity<JSONObject> deleteUserDataFromUserTable(Map<String, Object> userData){
        ProjectLogger.log("Request recieved for deleting user data from user table with userid "+ userData.get("user_id"), LoggerEnum.INFO.name());
        StringBuilder query = new StringBuilder();
        query.append("DELETE FROM ");
        query.append( tableName_user );
        query.append(" WHERE wid = '");
        query.append(userData.get("wid_user") + "'");
        query.append(" AND root_org = '" + userData.get("root_org") + "'");
        query.append(" AND org = '" + userData.get("organisation") );
        query.append("';");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query))){
           int successCount =  pst.executeUpdate();
           if(successCount > 0){
               return response.getResponse("User data deleted successfully from user table ", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);
           }
           else{
               return response.getResponse("User data could not be deleted ", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);

           }
        }
        catch (PSQLException ex){
            ProjectLogger.log("PSQL exception while deleting user from user table "+ Arrays.toString(ex.getStackTrace())+  ex, LoggerEnum.ERROR.name());
            return response.getResponse("PSQL exception while deleting user from user table", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured while deleting the data in postgresql from user table" + Arrays.toString(ex.getStackTrace()) + ex, LoggerEnum.ERROR.name());
            return response.getResponse("Failed to delete user data from user table", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }
    //get email details of user for user details api.
    public Object  getUserDetails(User userData, String dataToBeRetrieved){
        String emailResponse = new String();
        ProjectLogger.log("Request recieved to get user details from table wingspan_user.", LoggerEnum.ERROR.name());

        StringBuilder query = new StringBuilder();
        query.append("SELECT " );
        query.append(dataToBeRetrieved + " FROM ");
        query.append( tableName_user );
        
        query.append(" WHERE " + " wid = '" + userData.getWid_user() + "'");
        query.append(";");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query));
            ResultSet resultSet =  pst.executeQuery()){
            while (resultSet.next()) {
                emailResponse = resultSet.getString(dataToBeRetrieved);
            }

        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while updating user data in table wingspan_user" + e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while updating user data in table  wingspan_user" + Arrays.toString(ex.getStackTrace())+ ex, LoggerEnum.ERROR.name());
        }
        return emailResponse;
    }
//getting all users from userTable
    public ResponseEntity<JSONObject>  getAllUserList(User userData){
        JSONArray json = new JSONArray();
        ProjectLogger.log("Request recieved to get all user list  from table user.", LoggerEnum.ERROR.name());
        StringBuilder query = new StringBuilder();
        query.append("SELECT " );
        query.append( "*" + " FROM ");
        query.append( tableName_user );
        query.append(" WHERE " + " root_org = '" + userData.getRoot_org() + "'");
        query.append(" AND " + "org = '" + userData.getOrganisation() + "'");
        query.append(";");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query));
            ResultSet resultSet =  pst.executeQuery()){
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
            ProjectLogger.log("User list retieved successfully from wingspan_user table", LoggerEnum.INFO.name());
            return response.getResponse("User list retieved successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", json);
//            return json;
        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while retieving list of users from wingspan_user table" + Arrays.toString(e.getStackTrace())+ e, LoggerEnum.ERROR.name());
            return response.getResponse("Internal Server error", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", "");
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while retieving list of users from wingspan_user table"+ Arrays.toString(ex.getStackTrace()) +  ex, LoggerEnum.ERROR.name());
            return response.getResponse("Internal Server error", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", "");
        }
//        return json;
    }
    
    //update department_name for user table
    public int  updateUserDetails(User userData){
        ProjectLogger.log("Request recieved to update the user organisation details.", LoggerEnum.ERROR.name());
        int successcount = -1;
        StringBuilder query = new StringBuilder();
        query.append("UPDATE " );
        query.append(tableName_user);
        query.append(" SET " + " department_name = '" + userData.getOrganisation() + "'");
        query.append(" WHERE " + " wid = '" + userData.getWid_user() + "'");
        query.append(";");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query))){
           successcount = pst.executeUpdate();
            return successcount;
        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while updating organisation for user" + Arrays.toString(e.getStackTrace()) +  e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while updating organisation for user" + Arrays.toString(ex.getStackTrace()) + ex, LoggerEnum.ERROR.name());
        }
        return successcount;
    }
    
    public List<String> getUserRoles(Map<String, Object> userData) {
        ProjectLogger.log("Request recieved to get all user roles.", LoggerEnum.ERROR.name());
        List<String> role = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT role FROM ");
        query.append(schemaName_postgresql + "." + tableName_postgresql + " ");
        query.append("WHERE user_id = '");
        query.append(userData.get("user_id"));
        query.append("' AND root_org = '");
        query.append(userData.get("root_org"));
        query.append("';");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query));
            ResultSet resultSet =  pst.executeQuery()){
            while (resultSet.next()) {
                role.add(resultSet.getString("role"));
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured while retrieving user role for the given userId" + Arrays.toString(ex.getStackTrace()), ex, LoggerEnum.ERROR.name());
        }
        return role;
    }

    public int  updateUserProfile(String wid, Map<String ,Object> userMap){
        ProjectLogger.log("Request recieved to update the user profile details.", LoggerEnum.ERROR.name());
        int successcount = -1;
        StringBuilder query = new StringBuilder();
        query.append("UPDATE " );
        query.append(tableName_user);
        query.append(" SET " );
        for (Map.Entry<String, Object> entry : userMap.entrySet()) {
            query.append(entry.getKey() + " = '" + entry.getValue() + "',");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(" WHERE " + " wid = '" + wid + "'");
        query.append(";");
        System.out.println("query"+ query);
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query))){
            successcount = pst.executeUpdate();
            return successcount;
        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while updating user profile" + Arrays.toString(e.getStackTrace()) + " exception: " + e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while updating user profile" + Arrays.toString(ex.getStackTrace()) + " exception: " + ex, LoggerEnum.ERROR.name());
        }
        return successcount;
    }
    
    public Timestamp getTimestampValue(){
        try (Connection con = connect();
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
        try (Connection con = connect();
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
