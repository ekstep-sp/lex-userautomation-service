package com.space.userautomation.services;

import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.space.userautomation.common.LoggerEnum;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class UpdateUserInformation {

    Response response = new Response();
    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    private String adminName = System.getenv("adminName");
    private String adminPassword = System.getenv("adminPassword");
    
    public ResponseEntity<JSONObject> intializationRequest(User userData) {
        try{
            JSONObject jobj = new JSONObject();
            JSONObject jsonObject = new JSONObject(enableUserWithPassword(userData));
            jobj.put("enableDetails",jsonObject);
            String token = (String) jsonObject.get("token");
            
            JSONObject setPassword = new JSONObject(updatePassword(userData,token));
            jobj.put("passwordDetails",setPassword);
            
            return response.getResponse("",HttpStatus.OK,200,userData.getApiId(),jobj);    
        }
        catch(Exception e){
            return response.getResponse("",HttpStatus.BAD_REQUEST,500,userData.getApiId(),"");
        }
    }
    
    public JSONObject enableUserWithPassword(User user){
        JSONObject jobj = new JSONObject();
        try {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setUsername(adminName);
            userCredentials.setPassword(adminPassword);
            String token = new UserService().getToken(userCredentials);
            JSONParser parser = new JSONParser();
            JSONObject tokenJson = (JSONObject) parser.parse(token);
            String accessToken = tokenJson.get("access_token").toString();
            JSONObject jsonData = new JSONObject();
            jsonData.put("enabled", true);
            StringEntity params = new StringEntity(jsonData.toString());
            HttpPut request = new HttpPut(System.getenv("productionUrl")+"auth/admin/realms/"+System.getenv("keycloak.realm")+"/users/"+user.getUser_id());
            request.setHeader("content-type", "application/json");
            request.setHeader("Authorization" ,"bearer "+accessToken);
            request.setEntity(params);
            HttpResponse responses = httpClient.execute(request);
            int statusId = responses.getStatusLine().getStatusCode();
            if(statusId == 204){
                jobj.put("Enabled", true);
                jobj.put("token", accessToken);
//                jobj.put("Password", generatePassword);
//                jobj.put("IsPasswordSet", true);
                return jobj;
//                return response.getResponse("user account enabled", HttpStatus.OK, 200, "", responseDataMap);
            }
            else{
                jobj.put("Enabled", false);
//                jobj.put("Password","");
//                jobj.put("IsPasswordSet", false);
                return jobj;
//                return response.getResponse("user could not be enabled ", HttpStatus.BAD_REQUEST, 404, "",responseDataMap);
            }
        } catch (Exception ex) {
            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
//            return response.getResponse("user account diabled", HttpStatus.BAD_REQUEST, 400, "", "");
        }
        return jobj;
    }
    
    
    public JSONObject updatePassword(User userData, String accessToken) throws IOException {
        JSONObject jobj = new JSONObject();
        String generatePassword = new UserService().generateRandomPassword(16, 22, 122);
        HttpPut request = new HttpPut(System.getenv("productionUrl")+"auth/admin/realms/"+System.getenv("keycloak.realm")+"/users/"+userData.getUser_id()+"/reset-password");
        request.setHeader("content-type", "application/json");
        request.setHeader("Authorization" ,"bearer "+accessToken);
        JSONObject jParams = new JSONObject();
        jParams.put("type","password");
        jParams.put("value",generatePassword);
        jParams.put("temporary",false);
        StringEntity params = new StringEntity(jParams.toString());
        request.setEntity(params);
        HttpResponse responses = httpClient.execute(request);
        int statusId = responses.getStatusLine().getStatusCode();
        if(statusId == 204){
            jobj.put("password",generatePassword);
            jobj.put("updatedPassword",true);
            return jobj;
        }
        else{
            jobj.put("password","");
            jobj.put("updatedPassword",false);
            return jobj;
        }
    }
}
