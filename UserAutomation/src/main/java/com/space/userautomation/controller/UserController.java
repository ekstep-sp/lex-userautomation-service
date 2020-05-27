package com.space.userautomation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.model.User;
import com.space.userautomation.model.UserCredentials;
import com.space.userautomation.services.ExcelReader;
import com.space.userautomation.services.UserService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    public static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;
    ExcelReader excelReader;

    @RequestMapping(value = "/v1/token", method = RequestMethod.POST)
    public ResponseEntity<?> getTokenUsingCredentials(@RequestBody UserCredentials userCredentials) {


        String responseToken = null;
        try {
            System.out.println("token created:" +userCredentials );
            responseToken = userService.getToken(userCredentials);

        } catch (Exception e) {
            System.out.println("exception occured");

            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }


        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        return new ResponseEntity<>(responseToken, headers, HttpStatus.OK);

    }

    @RequestMapping(value = "/v1/create", method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@RequestBody User userDTO) {
        ProjectLogger.log("Create User Api called.", LoggerEnum.INFO.name());
        try {

            return userService.createUser(userDTO);
           // JSONObject response = userService.createUser(userDTO);
          //  return new ResponseEntity<JSONObject>(response,HttpStatus.OK);
//            return new ResponseEntity<>(HttpStatus.CREATED);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/uploadexcelfile", method = RequestMethod.POST, consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadExcelfile(@RequestBody File input) {
        try {
            Map<String,Object> allData = excelReader.readExcelSheet(input);
            List<String> headers = (List<String>)allData.get("header");
            List<List<String>> content = (List<List<String>>)allData.get("data");
            List<User> userList = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            for(List<String> contentEach : content) {
                Map<String, String> userData = new HashMap<>();
                for(int i=0; i<contentEach.size(); i++){
                    userData.put( headers.get(i), contentEach.get(i));
                }
                User user = mapper.convertValue(userData, User.class);
                userList.add(user);
            }

            for(User user: userList) {
                userService.createUser(user);
            }

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }
    }
}
