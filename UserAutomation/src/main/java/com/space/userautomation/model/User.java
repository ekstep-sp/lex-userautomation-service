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

    private String name;

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


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

        String firstName = name.split(" ")[0];
        this.firstName = firstName;
        return firstName;
    }

//    public void setFirstName(String firstName) {
//        this.firstName = firstName;
//    }

    public String getLastName() {
        try {
            if(name.indexOf(" ") > 0) {
                String lastName = name.substring(name.indexOf(" ") + 1);
                if (lastName.length() > 0) {
                    this.lastName = lastName;
                } else {
                    this.lastName = "";
                }
                return this.lastName;
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

//    public void setLastName(String lastName) {
//        this.lastName = lastName;
//    }

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
        userDetails.put("name", name);
        userDetails.put("firstName", this.getFirstName());
        userDetails.put("lastName", this.getLastName());
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
