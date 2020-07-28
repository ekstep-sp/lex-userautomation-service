package com.space.userautomation.services;

import java.io.*;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.CallableStatement;
import java.util.*;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.database.cassandra.Cassandra;
import com.space.userautomation.database.postgresql.Postgresql;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
import io.jsonwebtoken.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.space.userautomation.common.ProjectLogger;
import org.json.simple.parser.JSONParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component

public class UserService {

//    @Value("${keycloak.credentials.secret}")
//    private String SECRETKEY;

//    @Value("${keycloak.resource}")
//   private String CLIENTID;

//    @Value("${keycloak.auth-server-url}")
//    private String AUTHURL;
//
//    @Value("${keycloak.realm}")
//    private String REALM;

    Response responses = new Response();

    String CLIENTID = System.getenv("keycloak_resource");
    private String AUTHURL = System.getenv("keycloak_auth_server_url");
    private String REALM = System.getenv("keycloak_realm");

    private String adminName = System.getenv("adminName");
    private String adminPassword = System.getenv("adminPassword");
    private String content_type = System.getenv("content_type");


    private String publicKey = System.getenv("public_key");

    Postgresql postgresql = new Postgresql();
    Cassandra cassandra = new Cassandra();
    String roleForAdminUser = "org-admin";

    public String getToken(UserCredentials userCredentials) {

        String responseToken = "";
        try {
            String username = userCredentials.getUsername();
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("grant_type", "password"));
            urlParameters.add(new BasicNameValuePair("client_id", CLIENTID));
            urlParameters.add(new BasicNameValuePair("username", username));
            urlParameters.add(new BasicNameValuePair("password", userCredentials.getPassword()));
            responseToken = sendPost(urlParameters);
        } catch (Exception e) {
            ProjectLogger.log("Exception occured in getToken method" + e.getMessage(), LoggerEnum.ERROR.name());
        }
        return responseToken;
    }

    public ResponseEntity<JSONObject> createNewUser(User user) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        Map<String, Object> responseData = new HashMap<>();
        try {
            validateUserDetails(user);
        } catch (Exception e) {
            ProjectLogger.log(e.getMessage(), LoggerEnum.ERROR.name());
            return responses.getResponse("", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, user.getApiId(), "");
        }
        try {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setUsername(adminName);
            userCredentials.setPassword(adminPassword);
            String token = getToken(userCredentials);
            JSONParser parser = new JSONParser();
            JSONObject tokenJson = (JSONObject) parser.parse(token);
            String accessToken = tokenJson.get("access_token").toString();
            JSONObject json = new JSONObject();
            json.put("email", user.getEmail());
            ProjectLogger.log("User Email : " + user.getEmail(), LoggerEnum.INFO.name());
            json.put("firstName", user.getFirstName());
            if (user.getLastName().length() > 0) {
                json.put("lastName", user.getLastName());
            }
            json.put("username", user.getEmail());
            json.put("emailVerified", false);
            json.put("enabled", false);
            Map<String, List<String>> attributes = user.getAttributes();
            JSONObject attr = new JSONObject();
            for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
                String key = entry.getKey();
                JSONArray value = new JSONArray();
                for (String list : entry.getValue()) {
                    value.add(list);
                }
                attr.put(key, value);
            }
            json.put("attributes", attr);
            HttpPost request = new HttpPost(System.getenv("productionUrl") + "auth/admin/realms/" + REALM + "/users");
            StringEntity params = new StringEntity(json.toString());
            ProjectLogger.log("User Create Request Body : " + json.toString(), LoggerEnum.INFO.name());
            request.addHeader("content-type", content_type);
            request.addHeader("Authorization", "Bearer " + accessToken);
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status Id for createNewUser method : " + statusId, LoggerEnum.INFO.name());
            String message = EntityUtils.toString(response.getEntity());
            ProjectLogger.log("Response for createNewUser method : " + message, LoggerEnum.INFO.name());
            if (statusId == UserAutomationEnum.CREATED) {
                Header[] headers = response.getAllHeaders();
                String userId = "";
                for (int i = 0; i < headers.length; i++) {
                    String ss = headers[i].toString();
                    if (ss.indexOf("Location") >= 0) {
                        String[] locationSplit = ss.split("/");
                        userId = locationSplit[locationSplit.length - 1];
                        break;
                    }
                }
                ProjectLogger.log("User created successfully in keycloak with userId : " + userId, LoggerEnum.INFO.name());
                TemplateParser parserEmailTemplate = new TemplateParser(EmailTemplate.contentTemplate);
                new EmailService(parserEmailTemplate.getContent()).userCreationSuccessMail(user.getName(), user.getEmail(), user.getPassword(), user.getOrganisation());
                responseData.put("User_id", userId);
                return responses.getResponse("user created successfully in keycloak with userId", HttpStatus.CREATED, UserAutomationEnum.CREATED, user.getApiId(), responseData);
            } else if (statusId == 409) {
                responseData.put("Email", user.getEmail());
                ProjectLogger.log("Email = " + user.getEmail() + " already present in keycloak", LoggerEnum.ERROR.name());
                return responses.getResponse("This Email is already registered", HttpStatus.NOT_IMPLEMENTED, UserAutomationEnum.NOT_IMPLEMENTED, user.getApiId(), responseData);
            } else {
                ProjectLogger.log("Failed to create user." + response, LoggerEnum.ERROR.name());
                return responses.getResponse("unable to create user now.Please check the logs", HttpStatus.INTERNAL_SERVER_ERROR, UserAutomationEnum.INTERNAL_SERVER_ERROR, user.getApiId(), "");
            }
        } catch (Exception ex) {
            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
            return getFailedResponse(ex.getMessage());
        } finally {
            httpClient.close();
        }
    }

    public void validateUserDetails(User user) throws Exception {
        if (StringUtils.isEmpty(user.getName())) {
            throw new Exception("Missing mandatory parameter: name.");
        }
        if (StringUtils.isEmpty(user.getEmail())) {
            throw new Exception("Missing mandatory parameter: email.");
        }
//        if(StringUtils.isEmpty(user.getPassword())) {
//            throw new Exception("Missing mandatory parameter: password.");
//        }
    }

    // Function to generate random alpha-numeric password of specific length
    public String generateRandomPassword(int len, int randNumOrigin, int randNumBound) {
        SecureRandom random = new SecureRandom();
        return random.ints(randNumOrigin, randNumBound + 1)
                .filter(i -> Character.isAlphabetic(i) || Character.isDigit(i))
                .limit(len)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                        StringBuilder::append)
                .toString();
    }

    private String sendPost(List<NameValuePair> urlParameters) throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(System.getenv("productionUrl") + "auth/realms/" + REALM + "/protocol/openid-connect/token");
        post.setEntity(new UrlEncodedFormEntity(urlParameters));

        HttpResponse response = client.execute(post);

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        return result.toString();

    }

    public ResponseEntity<JSONObject> getFailedResponse(String message) {
        ProjectLogger.log(message, LoggerEnum.ERROR.name());
        JSONObject response = new JSONObject();
        response.put("status", "failure");
        response.put("error", message);
        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        return failedResponse;
    }

    public ResponseEntity<JSONObject> getFailedResponse(String message, int statusCode) {
        JSONObject response = new JSONObject();
        response.put("status", "failure");
        response.put("error", message);
        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<JSONObject>(response, HttpStatus.valueOf(statusCode));
        return failedResponse;
    }

    public ResponseEntity<JSONObject> getSuccessResponse(String userId, String password) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("userId", userId);
//        response.put("password", password);
        ResponseEntity<JSONObject> successReponse = new ResponseEntity<>(response, HttpStatus.OK);
        return successReponse;
    }

    public static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

    public ResponseEntity<JSONObject> userList(String filter, User userData) {
        Map<String, Object> hideUsers = new HashMap<>();
        hideUsers.put("username1", "spaceadmin");
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            JSONObject jObj = new JSONObject((Map) new UserRoleService().getRoleForAdmin(userData).getBody().get("DATA"));
            Boolean isORG_ADMIN = (Boolean) jObj.get(roleForAdminUser);
            if (isORG_ADMIN) {
                UserCredentials userCredentials = new UserCredentials();
                userCredentials.setUsername(adminName);
                userCredentials.setPassword(adminPassword);
                String token = getToken(userCredentials);
                ProjectLogger.log("Token generated : " + token, LoggerEnum.INFO.name());
                JSONParser parser = new JSONParser();
                JSONObject tokenJson = (JSONObject) parser.parse(token);
                String accessToken = tokenJson.get("access_token").toString();
                HttpGet request = new HttpGet(System.getenv("productionUrl") + "auth/admin/realms/" + REALM + "/users");
                request.addHeader("content-type", "application/json");
                request.addHeader("Authorization", "Bearer " + accessToken);
                HttpResponse response = httpClient.execute(request);
                int statusId = response.getStatusLine().getStatusCode();
                ProjectLogger.log("Status Id : " + statusId, LoggerEnum.INFO.name());
                String message = EntityUtils.toString(response.getEntity());
                JSONArray userList = (JSONArray) parser.parse(message);
                List user = new ArrayList();
                Boolean isEnabled;
                if (filter == null) {
                    for (int i = 0; i < userList.size(); i++) {
                        JSONObject obj = (JSONObject) userList.get(i);
                        if (!obj.get("username").equals(hideUsers.get("username1"))) {
                            user.add(obj);
                        }
                    }
                    return responses.getResponse("userList", HttpStatus.OK, 200, userData.getApiId(), user);
                }
                if (filter.equals("enabled")) {
                    isEnabled = true;
                    for (int i = 0; i < userList.size(); i++) {
                        JSONObject obj = (JSONObject) userList.get(i);
                        if (obj.get("enabled") == isEnabled) {
                            if (!obj.get("username").equals(hideUsers.get("username1"))) {
                                user.add(obj);
                            }
                        }
                    }
                    ProjectLogger.log("List of users are enabled : " + user, LoggerEnum.INFO.name());
                    return responses.getResponse("userList of enabled users", HttpStatus.OK, 200, userData.getApiId(), user);
                } else if (filter.equals("disabled")) {
                    isEnabled = false;
                    for (int i = 0; i < userList.size(); i++) {
                        JSONObject obj = (JSONObject) userList.get(i);
                        if (obj.get("enabled") == isEnabled) {
                            if (!obj.get("username").equals(hideUsers.get("username1"))) {
                                user.add(obj);
                            }
                        }
                    }
                    ProjectLogger.log("List of users are disabled : " + user, LoggerEnum.INFO.name());
                    return responses.getResponse("userList of disabled users", HttpStatus.OK, 200, userData.getApiId(), user);
                } else if (filter.equals("all")) {
                    for (int i = 0; i < userList.size(); i++) {
                        JSONObject obj = (JSONObject) userList.get(i);
                        if (!obj.get("username").equals(hideUsers.get("username1"))) {
                            user.add(obj);
                        }
                    }
                    ProjectLogger.log("List of users  : " + user, LoggerEnum.INFO.name());
                    return responses.getResponse("userList of all users", HttpStatus.OK, 200, userData.getApiId(), user);
                } else if (filter == null) {
                    return responses.getResponse("userList", HttpStatus.OK, 200, userData.getApiId(), userList);
                } else {
                    return responses.getResponse("userList", HttpStatus.OK, 200, userData.getApiId(), userList);
                }
            } else {
                return responses.getResponse("Permission denied,user list can be retireved by admin only", HttpStatus.FORBIDDEN, UserAutomationEnum.FORBIDDEN, userData.getApiId(), "");
            }
        } catch (Exception ex) {
            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
            return responses.getResponse(" ", HttpStatus.BAD_REQUEST, 400, userData.getApiId(), "");
        }
    }

    
    
    public ResponseEntity<JSONObject> userListFromUserTable(String filter, User userData){
        try{
            JSONArray userList = (JSONArray) postgresql.getAllUserList(userData);
            List filteredUserList = new ArrayList();
                for (int i = 0; i < userList.size(); i++) {
                    JSONObject obj = (JSONObject) userList.get(i);
                    filteredUserList.add(obj); 
                }
            return responses.getResponse("userList of all users", HttpStatus.OK, 200, userData.getApiId(), filteredUserList);
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in retrieving user list from user table " + ex.getMessage(), LoggerEnum.ERROR.name());
            return responses.getResponse(" ", HttpStatus.BAD_REQUEST, 400, userData.getApiId(), "");
        }
    }
    
    
    public JSONObject deleteUser(String user_id) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        JSONObject jsonobject = new JSONObject();
        try {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setUsername(adminName);
            userCredentials.setPassword(adminPassword);
            String token = getToken(userCredentials);
            ProjectLogger.log("Token generated : " + token, LoggerEnum.INFO.name());
            JSONParser parser = new JSONParser();
            JSONObject tokenJson = (JSONObject) parser.parse(token);
            String accessToken = tokenJson.get("access_token").toString();
            HttpDelete request = new HttpDelete(System.getenv("productionUrl") + "auth/admin/realms/" + REALM + "/users/" + user_id);
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "Bearer " + accessToken);
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status id data : " + statusId, LoggerEnum.INFO.name());
            if (statusId == 204) {
                ProjectLogger.log("user id is deleted succesfully.", LoggerEnum.INFO.name());
                jsonobject.put("statusCode", statusId);
                return jsonobject;
            } else if (statusId == 404) {
                ProjectLogger.log("user id is not existed", LoggerEnum.ERROR.name());
                jsonobject.put("statusCode", statusId);
                return jsonobject;
            }
        } catch (Exception ex) {
            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
        }
        return jsonobject;
    }

    public ResponseEntity<JSONObject> userDetails(User userData) {
        try {
            String newToken = getModifiedToken(userData.getTokenForUserDetails());
            Map<String, Object> user = new HashMap<>();
            Claims claimData = getDataFromToken(newToken);
            String organisation = (String) claimData.get("organisation");
            String emailFromToken = (String) claimData.get("email");
            if (validateWidWithToken(userData, emailFromToken)) {
                if (!organisation.isEmpty() && (organisation != null)) {
                    userData.setOrganisation(organisation);
//     DecodedJWT jwt = JWT.decode(userData.getTokenForUserDetails());
//     String org = jwt.getClaim("organisation").asString();
                    int successCountForUpdate = postgresql.updateUserDetails(userData);
                    user.put("wid", userData.getWid_user());
                    if (successCountForUpdate > 0) {
                        ProjectLogger.log("Successfully updated the user data.", LoggerEnum.ERROR.name());
                        return responses.getResponse("Successfully updated the user data.", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, userData.getApiId(), user);
                    } else {
                        ProjectLogger.log("Failed to update the user data.", LoggerEnum.ERROR.name());
                        return responses.getResponse("Failed to update the user data.", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), user);
                    }
                } else {
                    ProjectLogger.log("No organisation data was found", LoggerEnum.ERROR.name());
                    return responses.getResponse("No organisation data was found.", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, userData.getApiId(), user);
                }
            } else {
                ProjectLogger.log("Mismatched wid and token, please verify and try again.", LoggerEnum.ERROR.name());
                return responses.getResponse("Mismatched wid and token, please verify and try again.", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, userData.getApiId(), user);
            }
        } catch (ExpiredJwtException expiredJwtException) {
            ProjectLogger.log("Jwt token expired.Please try again" + expiredJwtException, LoggerEnum.INFO.name());
            return responses.getResponse("JWT token expired, Please try again", HttpStatus.FORBIDDEN, UserAutomationEnum.FORBIDDEN, userData.getApiId(), "");
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured in userDetails method. " + ex, LoggerEnum.INFO.name());
            return responses.getResponse("Something went wrong !!!.", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");
        }
    }

    public String getModifiedToken(String oldtoken) {
        Boolean containsBearer = oldtoken.startsWith("bearer");
        if (containsBearer) {
            String newToken = oldtoken.replace("bearer", "");
            return newToken;
        } else {
            return oldtoken;
        }
    }

    public Claims getDataFromToken(String token) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(keySpecX509);
        Jws<Claims> claims = Jwts.parser().setSigningKey(pubKey).parseClaimsJws(token);
//        String organisation = (String) claims.getBody().get("organisation"); 
        Claims getBody = claims.getBody();
        return getBody;
    }

    public Boolean validateWidWithToken(User user, String emailFromToken) {
        String emailResponse = (String) postgresql.getUserDetails(user, "email");
        if (emailFromToken.equals(emailResponse)) {
            return true;
        }
        return false;
    }


    public JSONObject deleteUserFromUserAutoComplete(String email, String wid) {
        JSONObject jsonObject = new JSONObject();
        try {
            User userData = new User();
            userData.setEmail(email);
            userData.setWid_user(wid);
            ResponseEntity<JSONObject> responseData = postgresql.deleteUserDataFromUserAutocomplete(userData.toMapUserDataForUserAutoComplete());
            Integer statusCode = (Integer) responseData.getBody().get("STATUS_CODE");
            if (statusCode == UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE) {
                jsonObject.put("wid",wid);
                jsonObject.put("status","true");
                ProjectLogger.log("User data deleted  successfully from user autocomplete table with email: " + userData.getEmail(), LoggerEnum.INFO.name());
                return jsonObject;
            } else {
                jsonObject.put("email",userData.getEmail());
                jsonObject.put("status","false");
                ProjectLogger.log("Failed to delete user data from user autocomplete table with email: " + userData.getEmail(), LoggerEnum.ERROR.name());
                return jsonObject;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured in deleteUserFromUserAutoComplete,  " + ex.getMessage(), LoggerEnum.ERROR.name());
        }
        return jsonObject;
    }

    public JSONObject deleteUserFromUserTable(String email, String userId, User user) {
        JSONObject jsonObject = new JSONObject();
        try {
            User userData = new User();
            userData.setEmail(email);
            userData.setUser_id(userId);
            userData.setRoot_org(user.getRoot_org());
            userData.setOrganisation(user.getOrganisation());
            ResponseEntity<JSONObject> responseData = postgresql.deleteUserDataFromUserTable(userData.toMapUserDataForUserAutoComplete());
            Integer statusCode = (Integer) responseData.getBody().get("STATUS_CODE");
            if (statusCode == UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE) {
                jsonObject.put("email",userData.getEmail());
                jsonObject.put("status","true");
                ProjectLogger.log("User data deleted successfully from user table with email: " + userData.getEmail(), LoggerEnum.INFO.name());
                return jsonObject;
            } else {
                jsonObject.put("email",userData.getEmail());
                jsonObject.put("status","false");
                ProjectLogger.log("Failed to delete user data from user table with email: " + userData.getEmail(), LoggerEnum.ERROR.name());
                return jsonObject;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured in deleteUserFromUserTable,  " + ex.getMessage(), LoggerEnum.ERROR.name());
        }
        return jsonObject;
    }
    
    
    //delete the user from user terms and condition table in cassandra db.
    public JSONObject deleteUserFromUserTncTable(String userId, User user) {
        JSONObject jsonObject = new JSONObject();
        try {
            User userData = new User();
            userData.setUser_id(userId);
            userData.setRoot_org(user.getRoot_org());
            userData.setApiId(user.getApiId());
            ResponseEntity<JSONObject> responseData = cassandra.deleteUserDataFromUserTncTable(userData.toMapUserDataForUserAutoComplete());
            Integer statusCode = (Integer) responseData.getBody().get("STATUS_CODE");
            if (statusCode == UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE) {
                jsonObject.put("userId",userId);
                jsonObject.put("status","true");
                ProjectLogger.log("User data deleted successfully from user tnc table with userId : " + userId , LoggerEnum.INFO.name());
                return jsonObject;
            } else {
                jsonObject.put("userId",userId);
                jsonObject.put("status","false");
                ProjectLogger.log("Failed to delete user data from user tnc table with userId: " + userId, LoggerEnum.ERROR.name());
                return jsonObject;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured in deleteUserFromUserTable,  " + ex.getMessage(), LoggerEnum.ERROR.name());
        }
        return jsonObject;
    }
}
    