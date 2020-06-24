package com.space.userautomation.model;

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
    
    private String root_org;
    
    private String user_id;
    
    private String wid_OrgAdmin;


    public String getWid_OrgAdmin() {
        return wid_OrgAdmin;
    }

    public void setWid_OrgAdmin(String wid_OrgAdmin) {
        this.wid_OrgAdmin = wid_OrgAdmin;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    private String apiId;
    public String getRoot_org() {
        return root_org;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void setRoot_org(String root_org) {
        this.root_org = root_org;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    private List<String> roles;



    public String getAppleId() {
        return appleId;
    }

    public void setAppleId(String appleId) {
        this.appleId = appleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
     public User(List<String> roles)
{
    this.roles = roles;
}


    public String getFirstName() {

        String firstName = name.split(" ")[0];
        this.firstName = firstName;
        return firstName;
    }
    
    public String wid;

    public String getWid() {
        return wid;
    }

    public void setWid(String wid) {
        this.wid = wid;
    }

    public String getWid_user() {
        return wid_user;
    }
    public void setWid_user(String wid_user) {
        this.wid_user = wid_user;
    }
    private String wid_user;

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
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrganisation() {
        if(StringUtils.isEmpty(organisation) || organisation.trim().isEmpty()) {
            return "";
        } else {
            return organisation;
        }
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
    
    public Map<String,Object> toMapUserRole(){
        Map<String,Object> userData = new HashMap<String, Object>();
        userData.put("root_org",this.getRoot_org());
        userData.put("user_id", this.getUser_id());
        userData.put("roles", this.getRoles());
        return userData;
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
