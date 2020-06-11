package com.space.userautomation.services;
import java.io.*;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.*;
import com.space.userautomation.common.LoggerEnum;

import com.space.userautomation.common.Response;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
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

    String SECRETKEY = System.getenv("keycloak.credentials.secret");
    String CLIENTID = System.getenv("keycloak.resource");
    private String AUTHURL = System.getenv("keycloak.auth-server-url");
    private String REALM = System.getenv("keycloak.realm");

    private String adminName = System.getenv("adminName");
    private String adminPassword = System.getenv("adminPassword");

    public String getToken(UserCredentials userCredentials) {

        String responseToken = "";
        try {

            String username = userCredentials.getUsername();
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("grant_type", "password"));
            urlParameters.add(new BasicNameValuePair("client_id", CLIENTID));
            urlParameters.add(new BasicNameValuePair("username", username));
            urlParameters.add(new BasicNameValuePair("password", userCredentials.getPassword()));
//            urlParameters.add(new BasicNameValuePair("client_secret", SECRETKEY));

            responseToken = sendPost(urlParameters);
            System.out.println("response token" + responseToken);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return responseToken;

    }

    public ResponseEntity<JSONObject> createNewUser(User user) throws IOException {

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            validateUserDetails(user);
        } catch (Exception e) {
            ProjectLogger.log(e.getMessage(), LoggerEnum.ERROR.name());
            return responses.getResponse("",HttpStatus.BAD_REQUEST,400,"","");
        }

        try {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setUsername(adminName);
            userCredentials.setPassword(adminPassword);
            String token = getToken(userCredentials);
            ProjectLogger.log("Token generated : " + token, LoggerEnum.INFO.name());
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

//            JSONArray credentails = new JSONArray();
//            JSONObject password = new JSONObject();
////            String generatePassword = generateRandomPassword(16, 22, 122);
//            password.put("type", "password");
//            password.put("value", generatePassword);
//            password.put("temporary", false);
//            credentails.add(password);
//            json.put("credentials", credentails);

            HttpPost request = new HttpPost(System.getenv("productionUrl")+"auth/admin/realms/"+REALM+"/users");
            StringEntity params = new StringEntity(json.toString());
            ProjectLogger.log("User Create Request Body : " + json.toString(), LoggerEnum.INFO.name());
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "Bearer " + accessToken);
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status Id : " + statusId, LoggerEnum.INFO.name());
            String message = EntityUtils.toString(response.getEntity());
            ProjectLogger.log("Response : " + message, LoggerEnum.INFO.name());
            if (statusId == 201) {
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

                //    return getSuccessResponse(userId, generatePassword);
                return responses.getResponse("user created successfully in keycloak with userId",HttpStatus.CREATED,201,"", userId);
            } else if (statusId == 409) {
                ProjectLogger.log("Email = " + user.getEmail() + " already present in keycloak", LoggerEnum.ERROR.name());
                //return getFailedResponse("This Email is already registered.", 409);
                return responses.getResponse("This Email is already registered",HttpStatus.NOT_IMPLEMENTED,409,"",user.getEmail());
            } else {
                ProjectLogger.log("Failed to create user." + response, LoggerEnum.ERROR.name());
                // return getFailedResponse("Unable to create user now. Please check the logs", 500);
                return responses.getResponse("unable to create user now.Please check the logs",HttpStatus.INTERNAL_SERVER_ERROR,500,"","");
            }
        } catch (Exception ex) {
            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
            return getFailedResponse(ex.getMessage());
        } finally {
            httpClient.close();
        }
    }

//    public ResponseEntity<JSONObject> createUser(User user) throws IOException {
//        // public JSONObject createUser(User user) throws IOException{
//
//        try {
//            validateUserDetails(user);
//        } catch (Exception e) {
//            ProjectLogger.log(e.getMessage(), LoggerEnum.ERROR.name());
//            return getFailedResponse(e.getMessage());
//        }
//        try {
//            // Define password credential
//            CredentialRepresentation passwordCred = new CredentialRepresentation();
//            passwordCred.setTemporary(false);
//            passwordCred.setType(CredentialRepresentation.PASSWORD);
//            String password = generateRandomPassword(16, 22, 122);
//            passwordCred.setValue(password);
//            user.setPassword(password);
//            // System.out.println("password cred" + passwordCred);
////            ProjectLogger.log ("random generatedPaaword is  :"   +passwordCred.getValue(),LoggerEnum.INFO.name());
//
//
//            UsersResource userResource = getKeycloakUserResource();
//
//            // Define user details
//            UserRepresentation userRep = new UserRepresentation();
////            userRep.setUsername(user.getUsername());
//            userRep.setEmail(user.getEmail());
//            userRep.setFirstName(user.getFirstName());
//            if (user.getLastName().length() > 0) {
//                userRep.setLastName(user.getLastName());
//            }
//            userRep.setUsername(user.getEmail());
//            userRep.setEnabled(false);
//            userRep.setEmailVerified(false);
//            userRep.setCredentials(Arrays.asList(passwordCred));
//
//            Map<String, List<String>> attributes = user.getAttributes();
//            if (!attributes.isEmpty() && attributes.size() > 0) {
//                userRep.setAttributes(attributes);
//            }
//
//            ProjectLogger.log("User Details : " + user, LoggerEnum.INFO.name());
////            ProjectLogger.log("password generator" + Arrays.asList(passwordCred),LoggerEnum.INFO.name());
//
//
//            // Create user
//            Response result = userResource.create(userRep);
//            int statusId = result.getStatus();
//
//            ProjectLogger.log("Status Code of Keycloak Response : " + result.getStatus(), LoggerEnum.INFO.name());
//
//
//            if (statusId == 201) {
//                String userId = result.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
//                ProjectLogger.log("User created successfully in keycloak with userId : " + userId, LoggerEnum.INFO.name());
//                // TemplateParser parser = new TemplateParser(EmailTemplates.contentTemplate);
//                //  TemplateParser parsertemplate = new TemplateParser(EmailTemplates.declineContentTemplate);
//
//                new EmailService().userCreationSuccessMail(user.getName(), user.getEmail(), user.getPassword(), user.getOrganisation());
//                return getSuccessResponse(userId, password);
////                // set role
////                RealmResource realmResource = getRealmResource();
////                RoleRepresentation savedRoleRepresentation = realmResource.roles().get("user").toRepresentation();
////                realmResource.users().get(userId).roles().realmLevel().add(Arrays.asList(savedRoleRepresentation));
//
////                ProjectLogger.log("Username==" + user.getUsername() + " created in keycloak successfully", LoggerEnum.INFO.name());
//            } else if (statusId == 409) {
//                ProjectLogger.log("Email = " + user.getEmail() + " already present in keycloak", LoggerEnum.ERROR.name());
//                return getFailedResponse("Eamil already present.", 409);
//            } else {
//                ProjectLogger.log("Failed to create user." + result, LoggerEnum.ERROR.name());
//                return getFailedResponse("Unable to create user now. Please check the logs", 500);
//            }
//
//        } catch (Exception e) {
//            ProjectLogger.log(e.getMessage(), LoggerEnum.ERROR.name());
//            return getFailedResponse(e.getMessage());
//        }
//    }

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

//    private UsersResource getKeycloakUserResource() {
//        Keycloak kc = KeycloakBuilder.builder().serverUrl("http://127.0.0.1:8080/auth/").realm("Demo-Realm").username("anjitha").password("anjitha")
//                .clientId("clientid-03").resteasyClient((ResteasyClient) ClientBuilder.newClient()).build();
//        RealmResource realmResource = kc.realm("Demo-Realm");
//        UsersResource userRessource = realmResource.users();
//
//        return userRessource;
//    }
//
//    private RealmResource getRealmResource() {
//
//        Keycloak kc = KeycloakBuilder.builder().serverUrl("https://www.actionforimpact.io/auth").realm("wingspan").username("Actionforimpact_admin").password("XhF6w6zm24DSqQa4")
//                .clientId("portal").resteasyClient((ResteasyClient) ClientBuilder.newClient()).build();
//        RealmResource realmResource = kc.realm("wingspan");
//
//        return realmResource;
//
//    }

    // Function to generate random alpha-numeric password of specific length
    public  String generateRandomPassword(int len, int randNumOrigin, int randNumBound) {
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
        HttpPost post = new HttpPost(AUTHURL + "/realms/" + REALM + "/protocol/openid-connect/token");

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
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        return failedResponse;
    }

    public ResponseEntity<JSONObject> getFailedResponse(String message, int statusCode) {
        JSONObject response = new JSONObject();
        response.put("status", "failure");
        response.put("error", message);
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<JSONObject>(response, HttpStatus.valueOf(statusCode));

        return failedResponse;
    }

    public ResponseEntity<JSONObject> getSuccessResponse(String userId, String password) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("userId", userId);
//        response.put("password", password);
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
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
    
//    public ResponseEntity<JSONObject> deleteUser() {
//        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
//        try {
////            UserCredentials userCredentials = new UserCredentials();
////            userCredentials.setUsername("anjitha");
////            userCredentials.setPassword("anjitha");
////            String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJEZVdwNkc0b0FocWFxUDdCaTlKWEpnajRRNGxiY3RsUFdHc2xaQk56XzBZIn0.eyJleHAiOjE1OTE3MTY0MzksImlhdCI6MTU5MTY4MDQzOSwianRpIjoiNjhmZmYzNjEtOTRiNy00NzMwLWE0ODYtZmUwYzM3MzA5MGEzIiwiaXNzIjoiaHR0cDovLzEyNy4wLjAuMTo4MDgwL2F1dGgvcmVhbG1zL0RlbW8tUmVhbG0iLCJhdWQiOlsicmVhbG0tbWFuYWdlbWVudCIsImFjY291bnQiXSwic3ViIjoiNDI2MDMzMTktYjdhNS00NDc4LTgyZTktMGRlZGIxNGIzM2VhIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiY2xpZW50aWQtMDMiLCJzZXNzaW9uX3N0YXRlIjoiODQ1MDMyMmEtYzllNS00Njc0LTkzNjQtMjVjZTk4ZmQyNGY5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjgwODAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJyZWFsbS1tYW5hZ2VtZW50Ijp7InJvbGVzIjpbInZpZXctcmVhbG0iLCJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycyIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwicmVhbG0tYWRtaW4iLCJjcmVhdGUtY2xpZW50IiwibWFuYWdlLXVzZXJzIiwicXVlcnktcmVhbG1zIiwidmlldy1hdXRob3JpemF0aW9uIiwicXVlcnktY2xpZW50cyIsInF1ZXJ5LXVzZXJzIiwibWFuYWdlLWV2ZW50cyIsIm1hbmFnZS1yZWFsbSIsInZpZXctZXZlbnRzIiwidmlldy11c2VycyIsInZpZXctY2xpZW50cyIsIm1hbmFnZS1hdXRob3JpemF0aW9uIiwibWFuYWdlLWNsaWVudHMiLCJxdWVyeS1ncm91cHMiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IkFuaml0aGEgUiIsInByZWZlcnJlZF91c2VybmFtZSI6ImFuaml0aGEiLCJnaXZlbl9uYW1lIjoiQW5qaXRoYSIsImZhbWlseV9uYW1lIjoiUiIsImVtYWlsIjoiYW5qaXRoYS5yOThAZ21haWwuY29tIn0.HrWWKa4lWGNtdchmsiqxZQOphnkpYQyNd4Ua90Vg0mZWvqyHEj5bOSV_tCyjIFMqJqKRxFj5_c3nimQHzZkoliNvdPnY-QqfxgCU6xlWyp_oGbI12o5mX8XBfwwSNoQk2OmAoADpc6u8cneHwmKHBHVpkDJQm17TTPIDsvhRo2-NqMjirZUh70HT2Z141K7wjdudSgCgEHSW33aiG568JgPiFnU7_q6_1W4wrA0PBGQTgOpR19j-LxhqAWCtB9rfG1KflER1J9PhqK6WDFehlQVBLKLMpsBllvB3fE7gl-qAQ4MVSAguxU4vzFYUBQb1RcGMPzkQwRXoL-AaEsc4Yw";
//
//            UserCredentials userCredentials = new UserCredentials();
//            userCredentials.setUsername("anjitha");
//            userCredentials.setPassword("anjitha");
//            String token = getToken(userCredentials);
//
////            String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI1bTEwTGFCenFEemZCc2R6Q2U3SGdYRWFVZmJXMk1SLTRIdmxTeG5fcG53In0.eyJleHAiOjE1OTE2Nzg2NzksImlhdCI6MTU5MTY3ODM3OSwianRpIjoiMjYwNzNiOTYtYWZkYy00OTNlLWI3NzEtZTIxMjU3YWQwNjMxIiwiaXNzIjoiaHR0cDovLzEyNy4wLjAuMTo4MDgwL2F1dGgvcmVhbG1zL3dpbmdzcGFuIiwiYXVkIjpbInJlYWxtLW1hbmFnZW1lbnQiLCJhY2NvdW50Il0sInN1YiI6IjFhOGJhZjdhLWY2OTQtNGI1OS04ODlmLWVhOWI0NmRlZDNkZCIsInR5cCI6IkJlYXJlciIsImF6cCI6InBvcnRhbCIsInNlc3Npb25fc3RhdGUiOiI2NGMxNmQ1Mi00MDVmLTQyNTYtOWE0My1mMDY4MmZiZTE2NDAiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InJlYWxtLW1hbmFnZW1lbnQiOnsicm9sZXMiOlsidmlldy1yZWFsbSIsInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwibWFuYWdlLWlkZW50aXR5LXByb3ZpZGVycyIsImltcGVyc29uYXRpb24iLCJyZWFsbS1hZG1pbiIsImNyZWF0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJxdWVyeS1yZWFsbXMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJxdWVyeS1jbGllbnRzIiwicXVlcnktdXNlcnMiLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidmlldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6InVtYSBkZXZpIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidW1hZGV2aSIsImdpdmVuX25hbWUiOiJ1bWEiLCJmYW1pbHlfbmFtZSI6ImRldmkiLCJlbWFpbCI6InVtYXBhbWlzZXR0eTEyM0BnbWFpbC5jb20ifQ.DURqnEortWe5D4bOEW1A8vWTaFXAmwjG5d1OlQV3I0KGAoLR0Dquzx4nm9WhfmNuvUtg-M4ufRoJxaO7ixs6S9RpvTTLKpKdF7FtZdMrz9CnqCilkEwEAUct27VIUXr8k7g0UHYSh8zcPl-5NiEUs1AK3lxpfhHSM3LfJE_sp8eX_C1ICgi_fKGpDgjkHVDkD_g7x7w6HGUO2UyVs-qdXKk8QOBEhn0wgYfW2RWL_qjqnT3rW6oAlw86Ki2cjLMT3e91h876izGXCH9YeTtB1jHw65mTwVZIOrbnjTLm2j1GjE3EfCIi9fcyX0qII1mbBwDKe97TppeO2Su6Ws8Nzg";
//            ProjectLogger.log("Token generated : " + token, LoggerEnum.INFO.name());
//            JSONParser parser = new JSONParser();
//            JSONObject tokenJson = (JSONObject) parser.parse(token);
//            String accessToken = tokenJson.get("access_token").toString();
//            HttpDelete request = new HttpDelete("http://127.0.0.1:8080/auth/admin/realms/Demo-Realm/users/299237ce-7b98-49c0-9a67-8a92bb6f9361");
//            request.addHeader("content-type", "application/json");
//            request.addHeader("Authorization", "Bearer " + accessToken);
//            HttpResponse response = httpClient.execute(request);
//            int statusId = response.getStatusLine().getStatusCode();
//            ProjectLogger.log("Status Id : " + statusId, LoggerEnum.INFO.name());
//            //   String message = EntityUtils.toString(response.getEntity());
//            //   JSONArray delUser = (JSONArray) parser.parse(message);
//            ProjectLogger.log("user is deleted : " + statusId, LoggerEnum.INFO.name());
//            return responses.getResponse("delUser", HttpStatus.OK, 200, "", statusId);
//        } catch (Exception ex) {
//            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
//            return responses.getResponse("no user with id", HttpStatus.BAD_REQUEST, 400, "", "");
//        }
//    }

    // getting the userlist
    public ResponseEntity<JSONObject> userList() {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {

            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setUsername(adminName);
            userCredentials.setPassword(adminPassword);
            String token = getToken(userCredentials);
            ProjectLogger.log("Token generated : " + token, LoggerEnum.INFO.name());
            JSONParser parser = new JSONParser();
            JSONObject tokenJson = (JSONObject) parser.parse(token);
            String accessToken = tokenJson.get("access_token").toString();
            HttpGet request = new HttpGet(System.getenv("productionUrl")+"auth/admin/realms/"+REALM+"/users");
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "Bearer " + accessToken);
            HttpResponse response = httpClient.execute(request);
            int statusId = response.getStatusLine().getStatusCode();
            ProjectLogger.log("Status Id : " + statusId, LoggerEnum.INFO.name());
            String message = EntityUtils.toString(response.getEntity());
            //   String jsonFormattedString = message.replaceAll("\\\\", "");
            //response.getContent().
            //JSONObject messaagenew = new JSONObject(messag);
            JSONArray userList = (JSONArray) parser.parse(message);
            // JSONObject m[] = new (JSONObject) messagenew.get(0);
//            int size = messagenew.size();
//            JSONObject ob[] = new JSONObject[size];
//            for(int i=0; i<size; i++) {
//                ob[i] = (JSONObject) messagenew.get(i);
//            }

//            List<Object> userlist = new ArrayList<>();
//            userlist.add(message);
            
            ProjectLogger.log("List of users : " + userList, LoggerEnum.INFO.name());
            return responses.getResponse("userlist", HttpStatus.OK, 200, "", userList);
            //  ProjectLogger.log("List of users : " + jsonFormattedString, LoggerEnum.INFO.name());
            // return responses.getResponse("List of users", HttpStatus.OK, 200,"", jsonFormattedString);

        } catch (Exception ex) {
            ProjectLogger.log(ex.getMessage(), LoggerEnum.ERROR.name());
            return responses.getResponse("Users list is empty", HttpStatus.BAD_REQUEST, 400, "", "");
        }
    }
}
