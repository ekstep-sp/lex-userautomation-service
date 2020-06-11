package com.space.userautomation.controller;


import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import com.space.userautomation.services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/usersubmission/user")
public class UserRoleController {

    @Autowired
    UserRoleService userRoleService;
    Response response = new Response();
    
    
    @RequestMapping(value = "/v1/create", headers={"rootOrg","org"}, method = RequestMethod.POST)
    public ResponseEntity<?> createUserRole(@RequestBody User userData ,  @RequestHeader Map<String, String> header){
        ProjectLogger.log("Creating user role in cassandra", LoggerEnum.INFO.name());

        try {
            
            userData.setApiId(response.getApiId());
            userData.setRoot_org(header.get("root_org"));
            userData.setOrganisation(header.get("org"));
            return userRoleService.createUserRole(userData);
        }
        catch (Exception ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/v1/checkorgadmin", method = RequestMethod.GET)
    public ResponseEntity<?> getUserRole(){
        ProjectLogger.log("Fetching user role from cassandra", LoggerEnum.INFO.name());
        try{
            User user = new User();
            user.setApiId(response.getApiId());
            return userRoleService.getRoleForAdmin(user);
        }
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping(value = "/v1/acceptuser", headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.POST)
    public ResponseEntity<?> acceptUser(@RequestBody User userData, @RequestHeader Map<Object, Object> header){
        ProjectLogger.log("Accepting user role", LoggerEnum.INFO.name());
        try
        {
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
            if(userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getOrganisation().isEmpty()) && (!userData.getWid_OrgAdmin().isEmpty())){
      
                return userRoleService.getAcceptedUser(userData);
            }
            else{
                return response.getResponse("PLz verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        } 
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
