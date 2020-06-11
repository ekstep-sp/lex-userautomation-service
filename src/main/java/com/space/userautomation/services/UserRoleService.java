package com.space.userautomation.services;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.database.cassandra.Cassandra;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
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

    private String REALM = System.getenv("keycloak.realm");
    private String root_org = System.getenv("rootOrg");
    private String org = System.getenv("org");
    private String locale = System.getenv("locale");

    public ResponseEntity<JSONObject> createUserRole(User userData) throws IOException {
        String userId = userData.getUser_id();
        try {
            JSONObject jObj = new JSONObject((Map) getRoleForAdmin(userData).getBody().get("DATA"));
            Boolean isORG_ADMIN = (Boolean) jObj.get("ORG_ADMIN");
            if (isORG_ADMIN) {
                Map<String, Object> userDetails = new HashMap<>();
                String token = new String();
                UserCredentials userCredentials = new UserCredentials();
                userCredentials.setUsername(userId);
                userCredentials.setPassword(userData.getPassword());
                token =  new UserService().getToken(userCredentials);
                System.out.println("token" + token);
                JSONParser parser = new JSONParser();
                JSONObject tokenJson = (JSONObject) parser.parse(token);
                String accessToken = tokenJson.get("access_token").toString();
                System.out.println("accessToken" + accessToken);
                String wid =  getWidIdFromWToken(accessToken,userData);
                userData.setUser_id(wid);
                cassandra.insertUser(userData.toMapUserRole());
                userDetails.put("WID" ,userData.getUser_id() );
                userDetails.put("Root_Org",userData.getRoot_org());
                userDetails.put("User_Roles" , userData.getRoles());
                return response.getResponse("User Role assigned successfully", HttpStatus.OK, 200, userData.getApiId(), userDetails);
            }
            else{
                return response.getResponse("User role cannot be assigned", HttpStatus.OK, 404, userData.getApiId(), "");
            }
        }
        catch(Exception ex){
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, 404, userData.getApiId(),"");
        }
    }

    public String getWidIdFromWToken(String token, User user) throws IOException {
        String userId = new String();
        try {

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            String accessToken = token;
            HttpGet request = new HttpGet(System.getenv("productionUrl")+"apis/protected/v8/user/details/wtoken");
            request.setHeader("Authorization", "bearer " + accessToken);
            request.setHeader("rootOrg", root_org);
            request.setHeader("org", org);
            request.setHeader("locale", locale);
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status Id : " + statusId, LoggerEnum.INFO.name());
            String responseData = EntityUtils.toString(response.getEntity());
            ProjectLogger.log("Response : " + responseData, LoggerEnum.INFO.name());
            if (statusId == 200) {
                JSONParser parser = new JSONParser();
                JSONObject jsonObj = (JSONObject) parser.parse(responseData);
                JSONObject userDetails = (JSONObject) jsonObj.get("user");
                userId = userDetails.get("wid").toString();
                ProjectLogger.log("UserId retrieved from WToken " + userId, LoggerEnum.INFO.name());
                return userId;
            }
            else {
                ProjectLogger.log("Could not retrieve UserId from WToken ", LoggerEnum.INFO.name());
                return userId;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception " + ex + "while retieving userId from wtoken api ", LoggerEnum.INFO.name());

        }
        return userId;
    }

    public String getAccessToken() {
        String accessToken = new String();
        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(System.getenv("productionUrl")+"auth/realms/"+REALM+"/protocol/openid-connect/token");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("client_id", "portal"));
            params.add(new BasicNameValuePair("grant_type", "password"));
            params.add(new BasicNameValuePair("scope", "openid"));
            params.add(new BasicNameValuePair("username", "deepak"));
            params.add(new BasicNameValuePair("password", "Apr@2020"));
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status Id : " + statusId, LoggerEnum.INFO.name());
            if (statusId == 200) {
                String message = EntityUtils.toString(response.getEntity());
                ProjectLogger.log("Responses : " + response, LoggerEnum.INFO.name());
                JSONParser parser = new JSONParser();
                JSONObject tokenJson = (JSONObject) parser.parse(message);
                accessToken = tokenJson.get("access_token").toString();
                System.out.println("accessToken" + accessToken);
                return accessToken;
            } else {
                ProjectLogger.log("Failed to retrieve access token ", LoggerEnum.ERROR.name());
            }
        }
        catch(IOException  ioException){
            ProjectLogger.log("IO Exception occured while getting access token"+ ioException, LoggerEnum.ERROR.name());
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured while getting access token "+ ex , LoggerEnum.ERROR.name());
        }
        return accessToken;
    }

    public ResponseEntity<JSONObject> getRoleForAdmin(User userDetailsForRoles){
        try {
            List<String> userRoles = new ArrayList<>();
            Map<String,Boolean> roles = new HashMap<String, Boolean>();
            System.out.println("wid"+userDetailsForRoles.getWid_OrgAdmin() );
            userDetailsForRoles.setUser_id(userDetailsForRoles.getWid_OrgAdmin());
            List<User> userList = cassandra.getUserRoles(userDetailsForRoles.toMapUserRole());
            for(User user: userList){
                userRoles = user.getRoles();
            }
            if(getSpecificRole(userRoles)){
                roles.put("ORG_ADMIN",true);
                return response.getResponse("", HttpStatus.FOUND, 200, userDetailsForRoles.getApiId(),roles);
            }
            else{
                roles.put("ORG_ADMIN",false);
                return response.getResponse("", HttpStatus.NOT_FOUND, 404, userDetailsForRoles.getApiId(),roles);
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured "+ ex , LoggerEnum.ERROR.name());
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, 500, userDetailsForRoles.getApiId(),"");
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

        try{
            Map<String, Object> userResponse = new HashMap<>();
            //check if user is enabled
            //check if user password is set.
           ResponseEntity<JSONObject> responseEntity =  userInformation.intializationRequest(userDetails);
           System.out.println("Data"+responseEntity);
           JSONObject jsonData = new JSONObject((Map) responseEntity.getBody().get("DATA"));
           JSONObject enabeldetails = (JSONObject) jsonData.get("enableDetails");
            Boolean isEnable = (Boolean)enabeldetails.get("Enabled");
            JSONObject passwordDetails= (JSONObject) jsonData.get("passwordDetails");
            String updatedPassword = (String) passwordDetails.get("password");
            Boolean isPasswordSet = (Boolean)passwordDetails.get("updatedPassword");
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
                    String widForNewUser =  getWidIdFromWToken(accessToken,userDetails);
                    


                    //assign roles for the new user from requested role to cassandra
                    userDetails.setUser_id(widForNewUser);
                    ResponseEntity<JSONObject> responseData = cassandra.insertUser(userDetails.toMapUserRole());
                    Integer statusCode = (Integer) responseData.getBody().get("STATUS_CODE");
                    if (statusCode == 200) {
                        
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
                } else {
                    return response.getResponse("Permission denaid, user role can be assigned by admin user only.", HttpStatus.FORBIDDEN, UserAutomationEnum.FORBIDDEN, apiId, "");
                }
            }
            else{
                return response.getResponse("User is not enabled and password is not updated", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, apiId, "");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Internal Server Exception "+ ex , LoggerEnum.ERROR.name());
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, apiId,"");
        }
    }

}
