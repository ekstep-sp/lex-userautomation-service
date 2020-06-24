package com.space.userautomation.services;

import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
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
    private String content_type = System.getenv("content_type");

    public ResponseEntity<JSONObject> intializationRequest(User userData) {
        try{
            ProjectLogger.log("IntializationRequest method is called" , LoggerEnum.INFO.name());
            JSONObject jobj = new JSONObject();
            JSONObject jsonObject = new JSONObject(enableUser(userData));
            jobj.put("enableDetails",jsonObject);
            String token = (String) jsonObject.get("token");
            JSONObject setPassword = new JSONObject(updatePassword(userData,token));
            jobj.put("passwordDetails",setPassword);
            return response.getResponse("",HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE,userData.getApiId(),jobj);
        }
        catch(Exception e){
            ProjectLogger.log("Exception occured in intializationRequest method" , LoggerEnum.ERROR.name());
            return response.getResponse("",HttpStatus.BAD_REQUEST,UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
        }
    }

    public JSONObject enableUser(User user){
        JSONObject jobj = new JSONObject();
        try {
            ProjectLogger.log("EnableUser method is called" , LoggerEnum.INFO.name());
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
            ProjectLogger.log("EnableUser method is called2" , LoggerEnum.INFO.name());

//            String generatePassword = new UserService().generateRandomPassword(16, 22, 122);
            HttpPut request = new HttpPut(System.getenv("productionUrl")+"auth/admin/realms/"+System.getenv("keycloak_realm")+"/users/"+user.getUser_id());
            ProjectLogger.log("EnableUser method is called31" , LoggerEnum.INFO.name());

            request.setHeader("content-type", content_type);
            ProjectLogger.log("EnableUser method is called32" , LoggerEnum.INFO.name());

            request.setHeader("Authorization" ,"bearer "+accessToken);
            request.setEntity(params);
            HttpResponse responses = httpClient.execute(request);
            ProjectLogger.log("EnableUser method is called33" , LoggerEnum.INFO.name());

            int statusId = responses.getStatusLine().getStatusCode();
            ProjectLogger.log("EnableUser method is called3" , LoggerEnum.INFO.name());

            if(statusId == UserAutomationEnum.NO_CONTENT){
                ProjectLogger.log("EnableUser method is called4" , LoggerEnum.INFO.name());

                jobj.put("Enabled", true);
                jobj.put("token", accessToken);
//                jobj.put("Password", generatePassword);
//                jobj.put("IsPasswordSet", true);
                return jobj;
            }
            else{
                jobj.put("Enabled", false);
//                jobj.put("Password","");
//                jobj.put("IsPasswordSet", false);
                return jobj;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured in enableUser method ", LoggerEnum.ERROR.name());
        }
        return jobj;
    }

    public JSONObject updatePassword(User userData, String accessToken) throws IOException {
        ProjectLogger.log("updatePassword method called", LoggerEnum.INFO.name());
        JSONObject jobj = new JSONObject();
        String generatePassword = new UserService().generateRandomPassword(16, 22, 122);
        HttpPut request = new HttpPut(System.getenv("productionUrl")+"auth/admin/realms/"+System.getenv("keycloak_realm")+"/users/"+userData.getUser_id()+"/reset-password");
        request.setHeader("content-type", content_type);
        request.setHeader("Authorization" ,"bearer "+accessToken);
        JSONObject jParams = new JSONObject();
        jParams.put("type","password");
        jParams.put("value",generatePassword);
        jParams.put("temporary",false);
        StringEntity params = new StringEntity(jParams.toString());
        request.setEntity(params);
        HttpResponse responses = httpClient.execute(request);
        int statusId = responses.getStatusLine().getStatusCode();
        if(statusId == UserAutomationEnum.NO_CONTENT){
            ProjectLogger.log("Password generated successfully", LoggerEnum.ERROR.name());
            jobj.put("password",generatePassword);
            jobj.put("updatedPassword",true);
            return jobj;
        }
        else{
            ProjectLogger.log("Password could not be generated", LoggerEnum.ERROR.name());
            jobj.put("password","");
            jobj.put("updatedPassword",false);
            return jobj;
        }
    }
}
