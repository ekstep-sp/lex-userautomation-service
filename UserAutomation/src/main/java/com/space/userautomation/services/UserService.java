package com.space.userautomation.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.json.simple.JSONObject;
import com.space.userautomation.common.ProjectLogger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
public class UserService  {

    @Value("${keycloak.credentials.secret}")
    private String SECRETKEY;

    @Value("${keycloak.resource}")
    private String CLIENTID;

    @Value("${keycloak.auth-server-url}")
    private String AUTHURL;

    @Value("${keycloak.realm}")
    private String REALM;


    public String getToken(UserCredentials userCredentials) {

        String responseToken = null;
        try {

            String username = userCredentials.getUsername();
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("grant_type", "password"));
            urlParameters.add(new BasicNameValuePair("client_id", CLIENTID));
            urlParameters.add(new BasicNameValuePair("username", username));
            urlParameters.add(new BasicNameValuePair("password", userCredentials.getPassword()));
            urlParameters.add(new BasicNameValuePair("client_secret", SECRETKEY));

            responseToken = sendPost(urlParameters);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return responseToken;

    }
    public ResponseEntity<JSONObject> createUser(User user) throws IOException{
   // public JSONObject createUser(User user) throws IOException{

        try {
            validateUserDetails(user);
        } catch (Exception e) {
            ProjectLogger.log(e.getMessage(), LoggerEnum.ERROR.name());
            return getFailedResponse(e.getMessage());
        }
        try {
            // Define password credential
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
//            String password = generateRandomPassword(16,22,122);
            passwordCred.setValue(user.getPassword());
           // System.out.println("password cred" + passwordCred);
//            ProjectLogger.log ("random generatedPaaword is  :"   +passwordCred.getValue(),LoggerEnum.INFO.name());


            UsersResource userResource = getKeycloakUserResource();

            // Define user details
            UserRepresentation userRep = new UserRepresentation();
//            userRep.setUsername(user.getUsername());
            userRep.setEmail(user.getEmail());
            userRep.setFirstName(user.getFirstName());
            userRep.setLastName(user.getLastName());
            userRep.setUsername(user.getEmail());
            userRep.setEnabled(false);
            userRep.setEmailVerified(false);
            userRep.setCredentials(Arrays.asList(passwordCred));

            Map<String, List<String>> attributes = user.getAttributes();
            if(!attributes.isEmpty() && attributes.size() > 0) {
                userRep.setAttributes(attributes);
            }

            ProjectLogger.log("User Details : " + user, LoggerEnum.INFO.name());
//            ProjectLogger.log("password generator" + Arrays.asList(passwordCred),LoggerEnum.INFO.name());


            // Create user
            Response result = userResource.create(userRep);

            int statusId = result.getStatus();

            ProjectLogger.log("Status Code of Keycloak Response : " + result.getStatus(),LoggerEnum.INFO.name());


            if (statusId == 201) {
                String userId = result.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                ProjectLogger.log("User created successfully in keycloak with userId : " + userId, LoggerEnum.INFO.name());
                new EmailService().userCreationSuccessMail(user.getEmail(), user.getPassword());
                return getSuccessResponse(userId);
//                // set role
//                RealmResource realmResource = getRealmResource();
//                RoleRepresentation savedRoleRepresentation = realmResource.roles().get("user").toRepresentation();
//                realmResource.users().get(userId).roles().realmLevel().add(Arrays.asList(savedRoleRepresentation));

//                ProjectLogger.log("Username==" + user.getUsername() + " created in keycloak successfully", LoggerEnum.INFO.name());
            }

                else if (statusId == 409) {
                ProjectLogger.log ("Email = " + user.getEmail() + " already present in keycloak",LoggerEnum.ERROR.name());
                return getFailedResponse("Eamil already present.", 409);
                } else {
                ProjectLogger.log("Failed to create user." + result,LoggerEnum.ERROR.name());
                return getFailedResponse("Unable to create user now. Please check the logs", 500);
            }

            } catch (Exception e) {
            ProjectLogger.log(e.getMessage(), LoggerEnum.ERROR.name());
            return getFailedResponse(e.getMessage());
            }
        }

    public void validateUserDetails(User user) throws Exception {
        if(StringUtils.isEmpty(user.getFirstName())) {
            throw new Exception("Missing mandatory parameter: firstname.");
        }
        if(StringUtils.isEmpty(user.getLastName())) {
            throw new Exception("Missing mandatory parameter: lastName.");
        }
        if(StringUtils.isEmpty(user.getEmail())) {
            throw new Exception("Missing mandatory parameter: email.");
        }
        if(StringUtils.isEmpty(user.getPassword())) {
            throw new Exception("Missing mandatory parameter: password.");
        }
    }

    private UsersResource getKeycloakUserResource() {
        Keycloak kc = KeycloakBuilder.builder().serverUrl(AUTHURL).realm("master").username("admin").password("admin")
                .clientId("admin-cli").resteasyClient((ResteasyClient)ClientBuilder.newClient()).build();
        RealmResource realmResource = kc.realm(REALM);
        UsersResource userRessource = realmResource.users();

        return userRessource;
    }

    private RealmResource getRealmResource() {

        Keycloak kc = KeycloakBuilder.builder().serverUrl(AUTHURL).realm("master").username("admin").password("admin")
                .clientId("admin-cli").resteasyClient((ResteasyClient)ClientBuilder.newClient()).build();

        RealmResource realmResource = kc.realm(REALM);

        return realmResource;

    }

    // Function to generate random alpha-numeric password of specific length
    public static String generateRandomPassword(int len, int randNumOrigin, int randNumBound)
    {
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
        response.put("status", "failed");
        response.put("error", message);
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

        return failedResponse;
    }

    public ResponseEntity<JSONObject> getFailedResponse(String message, int statusCode) {
        JSONObject response = new JSONObject();
        response.put("status", "failed");
        response.put("error", message);
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<JSONObject>(response, HttpStatus.valueOf(statusCode));

        return failedResponse;
    }

    public ResponseEntity<JSONObject> getSuccessResponse(String userId) {
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("userId", userId);
//        HttpHeaders headers = new HttpHeaders();
//        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        ResponseEntity<JSONObject> successReponse = new ResponseEntity<>(response, HttpStatus.OK);

        return successReponse;
    }


}
