package com.space.userautomation.model;

import org.keycloak.representations.idm.CredentialRepresentation;

import java.io.Serializable;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

  private String password;

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
}
