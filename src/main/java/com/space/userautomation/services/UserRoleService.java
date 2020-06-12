package com.space.userautomation.services;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.database.cassandra.Cassandra;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserRoleService {
    
    Cassandra cassandra = new Cassandra();
    Response response = new Response();
    EmailService emailService = new EmailService();
    UpdateUserInformation userInformation = new UpdateUserInformation();

    
    private String root_org = System.getenv("rootOrg");
    private String org = System.getenv("org");
    private String locale = System.getenv("locale");

    public ResponseEntity<JSONObject> createUserRole(User userData) throws IOException {
        String userId = userData.getUser_id();
        try {
            ProjectLogger.log("createUserRole method is called" , LoggerEnum.INFO.name());
            JSONObject jObj = new JSONObject((Map) getRoleForAdmin(userData).getBody().get("DATA"));
            Boolean isORG_ADMIN = (Boolean) jObj.get("ORG_ADMIN");
            if (isORG_ADMIN) {
                Map<String, Object> userDetails = new HashMap<>();
                String token = new String();
                UserCredentials userCredentials = new UserCredentials();
                userCredentials.setUsername(userId);
                userCredentials.setPassword(userData.getPassword());
                token =  new UserService().getToken(userCredentials);
                JSONParser parser = new JSONParser();
                JSONObject tokenJson = (JSONObject) parser.parse(token);
                String accessToken = tokenJson.get("access_token").toString();
                String wid =  getWidIdFromWToken(accessToken).get("wid").toString();
                userData.setUser_id(wid);
                cassandra.insertUser(userData.toMapUserRole());
                userDetails.put("WID" ,userData.getUser_id() );
                userDetails.put("Root_Org",userData.getRoot_org());
                userDetails.put("User_Roles" , userData.getRoles());
                return response.getResponse("User Role assigned successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, userData.getApiId(), userDetails);
            }
            else{
                return response.getResponse("User role cannot be assigned", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");
            }
        }
        catch(Exception ex){
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, userData.getApiId(),"");
        }
    }

    public Map<String, Object> getWidIdFromWToken(String accessToken) throws IOException {
        Map<String, Object>  responseFromWid = new HashMap<>();
         String userId = new String();
        try {
            ProjectLogger.log("getWidIdFromWToken method is called" , LoggerEnum.INFO.name());
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(System.getenv("productionUrl")+"apis/protected/v8/user/details/wtoken");
            request.setHeader("Authorization", "bearer " + accessToken);
            request.setHeader("rootOrg", root_org);
            request.setHeader("org", org);
            request.setHeader("locale", locale);
            ProjectLogger.log("Request sent for wtoken api" + userId, LoggerEnum.INFO.name());
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status Id for wtoken api : " + statusId, LoggerEnum.INFO.name());
            String responseData = EntityUtils.toString(response.getEntity());
            ProjectLogger.log("Response from wtoken api : " + responseData, LoggerEnum.INFO.name());
            if (statusId == UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE) {
                JSONParser parser = new JSONParser();
                JSONObject jsonObj = (JSONObject) parser.parse(responseData);
                JSONObject userDetails = (JSONObject) jsonObj.get("user");
                userId = userDetails.get("wid").toString();
                ProjectLogger.log("UserId retrieved from WToken " + userId, LoggerEnum.INFO.name());
                responseFromWid.put("wid", userId);
                responseFromWid.put("status_code",statusId);
                return responseFromWid;
            }
            if(statusId == UserAutomationEnum.INTERNAL_SERVER_ERROR){
                ProjectLogger.log("Internal server error from wtoken api ", LoggerEnum.ERROR.name());
                responseFromWid.put("wid", "");
                responseFromWid.put("status_code",statusId);
                return responseFromWid;
            }
            else {
                ProjectLogger.log("Failed to fetch wid from wtoken api. ", LoggerEnum.ERROR.name());
                responseFromWid.put("wid", "");
                responseFromWid.put("status_code",statusId);
                return responseFromWid;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception " + ex + "while retieving userId from wtoken api ", LoggerEnum.INFO.name());
        }
        return responseFromWid;
    }

    public ResponseEntity<JSONObject> getRoleForAdmin(User userDetailsForRoles){
        try {
            List<String> userRoles = new ArrayList<>();
            Map<String,Boolean> roles = new HashMap<String, Boolean>();
            ProjectLogger.log("wid for admin role" + userDetailsForRoles.getWid_OrgAdmin(), LoggerEnum.INFO.name());
            userDetailsForRoles.setUser_id(userDetailsForRoles.getWid_OrgAdmin());
            List<User> userList = cassandra.getUserRoles(userDetailsForRoles.toMapUserRole());
            for(User user: userList){
                userRoles = user.getRoles();
            }
            if(getSpecificRole(userRoles)){
                roles.put("ORG_ADMIN",true);
                return response.getResponse("org admin role", HttpStatus.FOUND, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, userDetailsForRoles.getApiId(),roles);
            }
            else{
                roles.put("ORG_ADMIN",false);
                return response.getResponse("org admin role not found ", HttpStatus.NOT_FOUND, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userDetailsForRoles.getApiId(),roles);
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured  in getRoleForAdmin method "+ ex , LoggerEnum.ERROR.name());
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, userDetailsForRoles.getApiId(),"");
        }
    }

    public boolean getSpecificRole(List<String> userRoles){
        List<String> roleToBeSearched  = new ArrayList<>();
        roleToBeSearched.add("ORG_ADMIN");
        if(userRoles.equals(roleToBeSearched)){
            return true;
        }
        else{
            return false;
        }
    }

    public ResponseEntity<JSONObject> getAcceptedUser(User userDetails){
        //Fetching user data
        String apiId = userDetails.getApiId();
        String userName = userDetails.getUsername();
//        String updatedPassword = userDetails.getPassword();

        try{
            Map<String, Object> userResponse = new HashMap<>();
            ResponseEntity<JSONObject> responseEntity = userInformation.intializationRequest(userDetails);
            ProjectLogger.log("Data from getAcceptedUser user from  intializationRequest method"+ responseEntity , LoggerEnum.ERROR.name());
            JSONObject jsonData = new JSONObject((Map) responseEntity.getBody().get("DATA"));
            JSONObject enabeldetails = (JSONObject) jsonData.get("enableDetails");
            Boolean isEnable = (Boolean) enabeldetails.get("Enabled");
            JSONObject passwordDetails = (JSONObject) jsonData.get("passwordDetails");
            String updatedPassword = (String) passwordDetails.get("password");
            Boolean isPasswordSet = (Boolean) passwordDetails.get("updatedPassword");
            if(isEnable && isPasswordSet) {

                //Retreive the user roles of admin user.
                JSONObject jObjForUserRole = new JSONObject((Map) getRoleForAdmin(userDetails).getBody().get("DATA"));

                //Check if role of ADMIN user is ORG_ADMIN.
                Boolean isORG_ADMIN = (Boolean) jObjForUserRole.get("ORG_ADMIN");
                if (isORG_ADMIN) {

                    //Create token for  new user.
                    String token = new String();
                    UserCredentials userCredentials = new UserCredentials();
                    userCredentials.setUsername(userName);
                    userCredentials.setPassword(updatedPassword);
                    token = new UserService().getToken(userCredentials);
                    JSONParser parser = new JSONParser();
                    JSONObject tokenJson = (JSONObject) parser.parse(token);
                    String accessToken = tokenJson.get("access_token").toString();

                    //retrieve wid of the new  user from token genserated.
                    Map<String,Object> responseFromWtoken = getWidIdFromWToken(accessToken);
                    
                    String widForNewUser = responseFromWtoken.get("wid").toString();
                    Integer responseStatusCode = (Integer) responseFromWtoken.get("status_code");
                    if(!widForNewUser.isEmpty() && (responseStatusCode == UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE)) {
                        //assign roles for the new user from requested role to cassandra
                        userDetails.setUser_id(widForNewUser);
                        ResponseEntity<JSONObject> responseData = cassandra.insertUser(userDetails.toMapUserRole());
                        Integer statusCode = (Integer) responseData.getBody().get("STATUS_CODE");
                        if (statusCode == UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE) {
                            //send email to new user with user credentials of user and platform link.
                            emailService.acceptmail(userName, updatedPassword, userDetails.getOrganisation());
                            
                            //return the response for the user role assigned.
                            userResponse.put("WID", widForNewUser);
                            userResponse.put("Root_Org", userDetails.getRoot_org());
                            userResponse.put("User_Roles", userDetails.getRoles());
                            return response.getResponse("User Role assigned successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, apiId, userResponse);
                        } else {
                            return response.getResponse("User could not be accepted", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, apiId, userResponse);
                        }
                    }else{
                        ProjectLogger.log("Wid was not fetched from wtoken api ", LoggerEnum.INFO.name());
                        return response.getResponse("User role couldn't be assigned, failed to fetch wid from wtoken.Please try again", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, apiId, userResponse);
                    }
                } else {
                    return response.getResponse("Permission denaid, user role can be assigned by admin user only.", HttpStatus.FORBIDDEN, UserAutomationEnum.FORBIDDEN, apiId, "");
                }
            } else{
                return response.getResponse("User is not enabled and password is not updated", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, apiId, "");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Internal Server Exception for accepting user roles "+ ex , LoggerEnum.ERROR.name());
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, apiId,"");
        }
    }
}
