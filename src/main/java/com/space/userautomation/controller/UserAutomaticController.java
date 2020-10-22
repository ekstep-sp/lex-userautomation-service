package com.space.userautomation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import com.space.userautomation.services.ExcelReader;
import com.space.userautomation.services.UserService;
import jnr.ffi.Struct;
import org.apache.tomcat.jni.Time;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.print.DocFlavor;
import javax.validation.constraints.NotBlank;
import java.sql.Timestamp;
import java.util.*;

@RestController
@Validated
@RequestMapping("/usersubmission/user")
public class UserAutomaticController {

    @Autowired
    UserService userService;
    ExcelReader excelReader;
    Response response = new Response();

    @RequestMapping(value = "/v2/create", method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@RequestBody User userDTO) {
        ProjectLogger.log("UserAutomation Create User Api called.", LoggerEnum.INFO.name());
        try {
            userDTO.setApiId(response.getApiId());
            return userService.createNewUser(userDTO);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    //Retrieve All Users
    @RequestMapping(value = "/v1/users",headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.GET)
    public ResponseEntity<JSONObject> listAllUsers(@RequestParam(required = false) String filter, User userData, @RequestHeader Map<Object, Object> header) {
        ProjectLogger.log("UserAutomation getUsers Api called.", LoggerEnum.INFO.name());
        try
        {
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
            if(userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getOrganisation().isEmpty()) && (!userData.getWid_OrgAdmin().isEmpty())){
                return userService.userList(filter,userData);
            }
            else{
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in listAllUsers method", LoggerEnum.ERROR.name());
            return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
        }
    }
    @RequestMapping(value = "/v1/uploadFile", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<JSONObject> uploadFile(@RequestParam("DataSheet") MultipartFile file) {
        ProjectLogger.log("upload the file : ", LoggerEnum.INFO.name());
        try {
            if(!file.getOriginalFilename().isEmpty()){
                Map<String, Object> allData = excelReader.readExcelSheet(file.getInputStream(), file.getOriginalFilename());
                ProjectLogger.log("read the file : " + file.getOriginalFilename(), LoggerEnum.INFO.name());
                List<String> headers = (List<String>) allData.get("header");
                List<List<String>> content = (List<List<String>>) allData.get("data");
                List<User> userList = new ArrayList<>();
                ObjectMapper mapper = new ObjectMapper();
                for (List<String> contentEach : content) {
                    Map<String, String> userData = new HashMap<>();
                    for (int i = 0; i < contentEach.size(); i++) {
                        userData.put(headers.get(i), contentEach.get(i));
                    }
                    User user = mapper.convertValue(userData, User.class);
                    userList.add(user);
                }
                for (User user : userList) {
                    ResponseEntity<JSONObject> response = userService.createNewUser(user);
                }
                return response.getResponse("User details", HttpStatus.OK, 200, "", userList);
            }else{
                return response.getResponse("File not found", HttpStatus.BAD_REQUEST, 400, "", "");
            }
        }
        catch (Exception ex) {
            ProjectLogger.log("Exception occured in uploadFile method", LoggerEnum.INFO.name());
            return response.getResponse("Users could not be uploaded", HttpStatus.BAD_REQUEST, 400, "", "");
        }
    }
    @RequestMapping(value = "/v2/userdetails", headers={"rootOrg","Org","authorization"},  method = RequestMethod.PATCH)
    public ResponseEntity<JSONObject> userDetails(@RequestParam(required = true) String wid,  User userData, @RequestHeader Map<Object, Object> header){
        ProjectLogger.log("User Details api is called.", LoggerEnum.INFO.name());
        try {
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setTokenForUserDetails((String) header.get("authorization"));
            userData.setWid_user(wid);
            if((!userData.getTokenForUserDetails().isEmpty()) && (!userData.getWid_user().isEmpty())){
                return userService.updateUserDetails(userData);
            }
            else{
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in userDetails", LoggerEnum.ERROR.name());
            return response.getResponse("User details cannot be updated ",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
        }
    }

    @RequestMapping(value = "/v2/users",headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.GET)
    public ResponseEntity<?> listAllUsersFromUserTable(User userData, @RequestHeader Map<Object, Object> header) {
        ProjectLogger.log("UserAutomation getUsers Api called.", LoggerEnum.INFO.name());
        try
        {
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
            if(userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getOrganisation().isEmpty()) && (!userData.getWid_OrgAdmin().isEmpty())){
                return userService.userListFromUserTable(userData);
            }
            else{
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in listAllUsers method", LoggerEnum.ERROR.name());
            return response.getResponse("Users details could not be retrieved",HttpStatus.INTERNAL_SERVER_ERROR, UserAutomationEnum.INTERNAL_SERVER_ERROR,userData.getApiId(),"");
        }
    }

    @GetMapping(value = "/v2/users/taguser")
    public ResponseEntity<?> getUserListForTag(@RequestParam(required = false) Timestamp startDate,
                                               @RequestParam(required = false) Timestamp endDate,
                                               @RequestHeader(value = "rootOrg") String rootOrg,
                                               @RequestHeader(value = "org") String org) {
        ProjectLogger.log("UserAutomation getUserListForTag Api called.", LoggerEnum.INFO.name());

        if (Objects.equals(rootOrg, System.getenv("rootOrg")) && StringUtils.hasText(org)) {
            return userService.getUsersListForTaggingUsers(rootOrg, org, startDate, endDate);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect Headers");
        }
    }

    @RequestMapping(value = "/v1/editprofile",headers={"rootOrg","Org"}, method = RequestMethod.PATCH)
    public ResponseEntity<JSONObject> editProfile(@RequestBody User userData, @RequestHeader Map<Object, Object> header) {
        ProjectLogger.log("UserAutomation editProfile Api called.", LoggerEnum.INFO.name());
        try
        {
            userData.setApiId(response.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            if(userData.getRoot_org().equals(System.getenv("rootOrg")) && (userData.getOrganisation().equals(System.getenv("org")))){
                return userService.editUserProfile(userData);
            }
            else{
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return response.getResponse("Please verify the headers before processing the request",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured in edit profile method", LoggerEnum.ERROR.name());
            return response.getResponse("Exception occured in edit profile method",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,userData.getApiId(),"");
        }
    }
    
    @GetMapping(value = "/v2/users/count")
    public ResponseEntity<?> getUserCount(@RequestParam(required = false) Timestamp startDate,
                                          @RequestParam(required = false) Timestamp endDate,
                                          @RequestHeader(value = "rootOrg") String rootOrg,
                                          @RequestHeader(value = "org")  String org) {
        if (Objects.equals(rootOrg, System.getenv("rootOrg")) && StringUtils.hasText(org)) {
            return userService.getUserCount(startDate, endDate, rootOrg, org);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect Headers");
        }
    }
    
    
}
