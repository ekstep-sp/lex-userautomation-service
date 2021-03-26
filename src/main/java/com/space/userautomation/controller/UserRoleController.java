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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.BadRequestException;
import java.util.Collections;
import java.util.HashMap;
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
            ProjectLogger.log("Exception occured in create user role", LoggerEnum.ERROR.name());
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/v1/check/admin", headers = {"rootOrg", "wid"}, method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Boolean> checkOrgAdmin(@RequestHeader Map<String, Object> header) {
        ProjectLogger.log("Fetching user role from postgres table", LoggerEnum.INFO.name());
        Map<String, Object> userRole = new HashMap<>();

        if(header.isEmpty()){
            throw new BadRequestException("Header should not be empty");
        }
        if (header.get("rootorg") == null || StringUtils.isEmpty(header.get("rootorg"))) {
            throw new BadRequestException("Pass rootOrg in headers");
        }
        if (header.get("wid") == null || StringUtils.isEmpty(header.get("wid"))) {
            throw new BadRequestException("Pass userId in headers");
        }
        userRole.put("root_org", header.get("rootorg"));
        userRole.put("user_id", header.get("wid"));
        return Collections.singletonMap("isAdmin", userRoleService.checkOrgAdmin(userRole));
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
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        } 
        catch(Exception ex){
            ProjectLogger.log("Exception occured in acceptUser method", LoggerEnum.ERROR.name());
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @RequestMapping(value = "/v1/getallroles", headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.GET)
    public ResponseEntity<?> getAllRole(User userData, @RequestHeader Map<Object, Object> header){
        ProjectLogger.log("Accepting user role", LoggerEnum.INFO.name());
        try
        {
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
            if((userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getRoot_org().isEmpty())) && ( userData.getOrganisation().equals(System.getenv("org")) && (!userData.getOrganisation().isEmpty())) && (!userData.getWid_OrgAdmin().isEmpty())){
                return userRoleService.getAllRoles(userData);
            }
            else{
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in acceptUser method", LoggerEnum.ERROR.name());
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    @RequestMapping(value = "/v1/changerole", headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.PUT)
    public ResponseEntity<?> changeRole(@RequestBody User userData, @RequestHeader Map<Object, Object> header){
        ProjectLogger.log(" Request recieved for change role api", LoggerEnum.INFO.name());
        try
        {
            User user = new User();
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
            userData.setEmail(userData.getEmail());
            userData.setName(userData.getName());
            if((userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getRoot_org().isEmpty())) && ( userData.getOrganisation().equals(System.getenv("org")) && (!userData.getOrganisation().isEmpty())) && (!userData.getWid_OrgAdmin().isEmpty())){
                return  userRoleService.changeRole(userData);
            }
            else{
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in changerole method", LoggerEnum.ERROR.name());
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
//    @RequestMapping(value = "/v1/getRoles/{id}", headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.GET)
//    public ResponseEntity<JSONObject> getRole(@PathVariable("id") String user_id,
//                                              User userData, @RequestHeader Map<Object, Object> header) {
//        ProjectLogger.log("Fetching user role from cassandra", LoggerEnum.INFO.name());
//        try
//        {
//            userData.setApiId(response.getApiId());
//            userData.setRoot_org((String) header.get("rootorg"));
//            userData.setOrganisation((String) header.get("org"));
//            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
//            if(userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getOrganisation().isEmpty()) && (!userData.getWid_OrgAdmin().isEmpty())){
//                return userRoleService.getRoles(user_id,userData);
//            }
//            else{
//                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
//                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
//            }
//        }
//        catch(Exception ex){
//            ProjectLogger.log("Exception occured in acceptUser method", LoggerEnum.ERROR.name());
//            return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
//        }
//    }
}
