package com.space.userautomation.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

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
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


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
    public int createUser(User userDTO) throws IOException{
   // public JSONObject createUser(User userDTO) throws IOException{

        int statusId = 0;



        try {
            // Define password credential
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            String password = generateRandomPassword(16,22,122);
            passwordCred.setValue(password);
            System.out.println("password cred" + passwordCred);
            System.out.println(passwordCred.getValue());


            UsersResource userRessource = getKeycloakUserResource();

            // Define user details
            UserRepresentation user = new UserRepresentation();
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setFirstName(userDTO.getFirstName());
            user.setLastName(userDTO.getLastName());
            user.setEnabled(true);
            user.setEmailVerified(false);
            user.setCredentials(Arrays.asList(passwordCred));
            System.out.println("password generator" + Arrays.asList(passwordCred));


            // Create user
            Response result = userRessource.create(user);
            System.out.println("Keycloak create user response code>>>>" + result.getStatus());

            statusId = result.getStatus();

            if (statusId == 201) {

                String userId = result.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                System.out.println("User created with userId:" + userId);

                // set role
                RealmResource realmResource = getRealmResource();
                RoleRepresentation savedRoleRepresentation = realmResource.roles().get("user").toRepresentation();
                realmResource.users().get(userId).roles().realmLevel().add(Arrays.asList(savedRoleRepresentation));

                System.out.println("Username==" + userDTO.getUsername() + " created in keycloak successfully");

                new EmailService().userCreationSuccessMail(user.getUsername(), user.getEmail(), password);
            }

                else if (statusId == 409) {
                    System.out.println("Username==" + userDTO.getUsername() + " already present in keycloak");

                } else {
                    System.out.println("Username==" + userDTO.getUsername() + " could not be created in keycloak");

                }

            } catch (Exception e) {
                e.printStackTrace();

            }

            return statusId;

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


}
