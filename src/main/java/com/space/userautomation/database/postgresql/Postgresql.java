package com.space.userautomation.database.postgresql;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import org.json.simple.JSONObject;
import org.postgresql.util.PSQLException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

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
        ProjectLogger.log("Request recieved to get user details from table wingspan_user.", LoggerEnum.INFO.name());

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

    public List<Map<String, Object>> getAllUserList(String rootOrg, String org) {
        return getAllUserList(rootOrg, org, "*", null, null, null, 0, 0);
    }

        //new method for getting all users from userTable
    public List<Map<String, Object>> getAllUserList(String rootOrg, String org, String fields, Timestamp startDate, Timestamp endDate, String searchQuery, int searchSize, int offSet) {
        List<Map<String, Object>> userList = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append(fields).append(" FROM ");
        query.append(tableName_user);
        query.append(" WHERE root_org = ? ");
        query.append(" AND org = ? ");
        if (startDate != null && endDate != null) {
            query.append(" AND time_inserted >= ? AND time_inserted <= ? ");
        }
        if (StringUtils.hasText(searchQuery)) {
            query.append(" AND (concat(lower(first_name), ' ', lower(middle_name), ' ',lower(last_name))  like ? OR concat(lower(first_name), ' ',lower(last_name))  like ? OR lower(department_name)  like ?) ");
        }
        query.append(" ORDER BY first_name, last_name");
        if (searchSize != 0) {
            query.append(" LIMIT ").append(searchSize);
            query.append(" OFFSET ").append(offSet);
        }
        query.append(";");
        try (Connection conn = connect();
             PreparedStatement pst = conn.prepareStatement(String.valueOf(query))) {
            int params = 0;
            pst.setString(++params, rootOrg);
            pst.setString(++params, org);
            if (startDate != null && endDate != null) {
                pst.setTimestamp(++params, startDate);
                pst.setTimestamp(++params, endDate);
            }
            if (StringUtils.hasText(searchQuery)) {
                searchQuery = "%" + searchQuery.toLowerCase() + "%";
                pst.setString(++params, searchQuery); // for first name + middle name + last name
                pst.setString(++params, searchQuery); // for first name + last name
                pst.setString(++params, searchQuery); // for department name
            }
            ResultSet resultSet = pst.executeQuery();
            ResultSetMetaData rsmd = resultSet.getMetaData();
            while (resultSet.next()) {
                Map<String, Object> obj = new HashMap<>();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    obj.put(rsmd.getColumnName(i), resultSet.getObject(i));
                }
                userList.add(obj);
            }
            return userList;
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured while retieving list of users" + Arrays.toString(ex.getStackTrace()) + ex, LoggerEnum.ERROR.name());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    public int getUserCount(Timestamp startDate, Timestamp endDate, String rootOrg, String org) {
        String query = "SELECT " +
                "count(*) as total" + " FROM " +
                tableName_user +
                " WHERE root_org = ? " +
                " AND org = ? ";
        if (startDate != null && endDate != null) {
            query += " AND time_inserted >= ? AND time_inserted <= ? ";
        }
        try (Connection conn = connect();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, rootOrg);
            pst.setString(2, org);
            if (startDate != null && endDate != null) {
                pst.setTimestamp(3, startDate);
                pst.setTimestamp(4, endDate);
            }
            ResultSet resultSet = pst.executeQuery();
            if (resultSet.next()) return resultSet.getInt("total");
            return 0;
        } catch (Exception ex) {
            ProjectLogger.log("Exception occurred while retrieving count of users" + Arrays.toString(ex.getStackTrace()) + ex, LoggerEnum.ERROR.name());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public List<HashMap<String, Object>> getUserStatsByField(String rootOrg, String fieldName, Timestamp startDate, Timestamp endDate) {
        String query = "SELECT " + fieldName + "," +
                "count(*) as count" + " FROM " +
                tableName_user +
                " WHERE root_org = ? ";
        if (startDate != null) {
            query += " AND time_inserted >= ?";
        }
        if (endDate != null) {
            query += " AND time_inserted <= ? ";
        }
        query += " group by " + fieldName;
        try (Connection conn = connect();
             PreparedStatement pst = conn.prepareStatement(query)) {
            int i = 1;
            pst.setString(i, rootOrg);
            if (startDate != null) {
                pst.setTimestamp(++i, startDate);
            }
            if (endDate != null) {
                pst.setTimestamp(++i, endDate);
            }
            ResultSet resultSet = pst.executeQuery();
            Map<String, Integer> userStats = new HashMap<>();
            while (resultSet.next()) {
                String key = resultSet.getString(fieldName);
                if (!StringUtils.hasText(key)){
                    key = "No Data";
                } else {
                    char[] charArray = key.toCharArray();
                    StringBuilder newKey = new StringBuilder();
                    char prevChar = ' ';
                    for (char ch:charArray) {
                        if (!Character.isLetterOrDigit(ch)) {
                            ch = ' ';
                        }
                        if (Character.isLetterOrDigit(ch) || (Character.isWhitespace(ch) && !Character.isWhitespace(prevChar))) {
                            newKey.append(Character.toLowerCase(ch));
                            prevChar = ch;
                        }
                    }
                    if (newKey.length() == 0) {
                        newKey.append("No Data");
                    }
                    key = StringUtils.capitalize(newKey.toString().trim());
                }
                userStats.merge(key, resultSet.getInt("count"), Integer::sum);
            }
            return userStats.entrySet().stream()
                    .sorted(Comparator.comparingInt((ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue).reversed())
                    .map(entry -> {
                        HashMap<String, Object> temp = new HashMap<>();
                        temp.put("key", entry.getKey());
                        temp.put("value", entry.getValue());
                        return temp;
                    })
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            ProjectLogger.log("Exception occurred while retrieving count of users" + Arrays.toString(ex.getStackTrace()) + ex, LoggerEnum.ERROR.name());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    //update department_name for user table
    public int  updateUserDetails(User userData){
        ProjectLogger.log("Request recieved to update the user organisation details.", LoggerEnum.INFO.name());
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
        ProjectLogger.log("Request recieved to get all user roles.", LoggerEnum.INFO.name());
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

    public List<String> getUserIdsByRole(String rootOrg, String role) {
        ProjectLogger.log("Request recieved to get all user roles.", LoggerEnum.INFO.name());
        List<String> userIds = new ArrayList<>();
        String query = "SELECT user_id FROM " +
                schemaName_postgresql + "." + tableName_postgresql + " " +
                "WHERE role = '" +
                role +
                "' AND root_org = '" +
                rootOrg +
                "';";
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(query);
            ResultSet resultSet =  pst.executeQuery()){
            while (resultSet.next()) {
                String userId = resultSet.getString("user_id");
                if (userId != null && userId.split("-").length == 5) {
                    userIds.add(resultSet.getString("user_id"));
                }
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured while retrieving user role for the given userId" + Arrays.toString(ex.getStackTrace()), ex, LoggerEnum.ERROR.name());
        }
        return userIds;
    }

    public int  updateUserProfile(User user, Map<String ,Object> userMap){
        ProjectLogger.log("Request recieved to update the user data.", LoggerEnum.INFO.name());
        int successcount = -1;
        StringBuilder query = new StringBuilder();
        query.append("UPDATE " );
        query.append(tableName_user);
        query.append(" SET " );
        for (Map.Entry<String, Object> entry : userMap.entrySet()) {
            if(entry.getValue() != null){
                query.append(entry.getKey() + " = '" + entry.getValue() + "',");
            }
        }
        query.deleteCharAt(query.length() - 1);
        query.append(" WHERE " + " wid = '" + user.getWid() + "'");
        query.append(" AND " + " root_org = '" + user.getRoot_org() + "'");
        query.append(" AND " + "org = '" + user.getOrganisation() + "'");
        query.append(";");
        try(Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(String.valueOf(query))){
            successcount = pst.executeUpdate();
            return successcount;
        } catch (SQLException e) {
            ProjectLogger.log("SQL Exception occured while updating user data" + Arrays.toString(e.getStackTrace()) + " exception: " + e, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while updating user data" + Arrays.toString(ex.getStackTrace()) + " exception: " + ex, LoggerEnum.ERROR.name());
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
