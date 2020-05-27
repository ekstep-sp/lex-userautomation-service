package com.space.userautomation.model;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String password;

    private String organisation;

    private String appleId;

    private String[] roles;

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public String getAppleId() {
        return appleId;
    }

    public void setAppleId(String appleId) {
        this.appleId = appleId;
    }

//    private List<String> credentials;
//
//    public List<String> getCredentials() {
//        return credentials;
//    }
//
//    public void setCredentials(List<String> credentials) {
//        this.credentials = credentials;
//    }


    public List<CredentialRepresentation> credentials;

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public User()

    {

    }



    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }


    public Map<String, String> toMap() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("email", email);
        userDetails.put("firstName", firstName);
        userDetails.put("lastName", lastName);
        userDetails.put("password", password);

        return userDetails;
    }

    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> userMap = new HashMap<>();
        if(!StringUtils.isEmpty(organisation)) {
            userMap.put("organisation", Arrays.asList(organisation));
        }
        if(!StringUtils.isEmpty(appleId)) {
            userMap.put("appleID", Arrays.asList(appleId));
        }

        return userMap;
    }
}
