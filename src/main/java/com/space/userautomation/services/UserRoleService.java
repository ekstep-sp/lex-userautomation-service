package com.space.userautomation.services;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.database.cassandra.Cassandra;
import com.space.userautomation.model.User;
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


    public ResponseEntity<JSONObject> createUserRole(User userData) throws IOException {
        String userId = userData.getUser_id();
        try {
            JSONObject jObj = new JSONObject((Map) getUserRoles(userData).getBody().get("DATA"));
            Boolean isORG_ADMIN = (Boolean) jObj.get("ORG_ADMIN");
            if (isORG_ADMIN) {
                Map<String, Object> userDetails = new HashMap<>();
//                String accessToken = getAccessToken();
//                String userId = getUserIdFromWToken(accessToken);
                userData.setUser_id(userId);
                cassandra.insertUser(userData.toMapUserRole());
                userDetails.put("User_Id" ,userData.getUser_id() );
                userDetails.put("Root_Org",userData.getRoot_org());
                userDetails.put("User_Roles" , userData.getRoles());
                return response.getResponse("User Role assigned successfully", HttpStatus.OK, 200, userData.getApiId(), userDetails);
//                /*  this code below is used to create token for new user */
                
//                String token = new String();
//                UserCredentials userCredentials = new UserCredentials();
//                userCredentials.setUsername(userData.getName());
//                userCredentials.setPassword(userData.getPassword());
//                token =  userService.getToken(userCredentials);
//                JSONParser parser = new JSONParser();
//                JSONObject tokenJson = (JSONObject) parser.parse(token);
//                String accessToken = tokenJson.get("access_token").toString();
//                System.out.println("accessToken" + accessToken);
//                String userId =  getUserIdFromWToken(accessToken);
//                userData.setUser_id(userId);
//                cassandra.insertUser(userData.toMapUserRole());
//                return response.getResponse("User Role assigned successfully", HttpStatus.OK, 200, userData.getApiId(), userData);
            }
            else{
                return response.getResponse("User role cannot be assigned", HttpStatus.OK, 404, userData.getApiId(), "");
            }
        }
        catch(Exception ex){
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, 404, userData.getApiId(),"");
        }
    }

    public String getUserIdFromWToken(String token) throws IOException {
        String userId = new String();
        try {

            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            String accessToken = token;
            HttpGet request = new HttpGet("https://www.actionforimpact.io/apis/protected/v8/user/details/wtoken");
            request.setHeader("Authorization", "bearer " + accessToken);
            request.setHeader("rootOrg", "space");
            request.setHeader("org", "space");
            request.setHeader("locale", "en");
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
//                JSONObject jsonObj = new JSONObject(responseData);
//                userId = jsonObj.getJSONObject("user").getString("wid");
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
            HttpPost request = new HttpPost("https://www.actionforimpact.io/auth/realms/wingspan/protocol/openid-connect/token");
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

    public ResponseEntity<JSONObject> getUserRoles(User userDetails){
        try {
            List<String> userRoles = new ArrayList<>();
            Map<String,Boolean> roles = new HashMap<String, Boolean>();
            String accessToken = getAccessToken();
            String userId = getUserIdFromWToken(accessToken);
            userDetails.setUser_id(userId);
            List<User> userList = cassandra.getUserRoles(userDetails.toMapUserRole());
            for(User user: userList){
                userRoles = user.getRoles();
            }
            if(getSpecificRole(userRoles)){
                roles.put("ORG_ADMIN",true);
                return response.getResponse("", HttpStatus.FOUND, 200, userDetails.getApiId(),roles);
            }
            else{
                roles.put("ORG_ADMIN",false);
                return response.getResponse("", HttpStatus.NOT_FOUND, 404, userDetails.getApiId(),roles);
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured "+ ex , LoggerEnum.ERROR.name());
            return response.getResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, 500, userDetails.getApiId(),"");
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
}
