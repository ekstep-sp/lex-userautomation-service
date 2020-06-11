package com.space.userautomation.controller;


import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.model.User;
import com.space.userautomation.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usersubmission/user")
public class UserAutomaticController {

    @Autowired
    UserService userService;

    @RequestMapping(value = "/v2/create", method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@RequestBody User userDTO) {
        ProjectLogger.log("UserAutomation Create User Api called.", LoggerEnum.INFO.name());
        try {
            return userService.createNewUser(userDTO);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    //Retrieve All Users
    @RequestMapping(value = "/v1/users", method = RequestMethod.GET)
    public ResponseEntity<?> listAllUsers() {
        ProjectLogger.log("UserAutomation getUsers Api called.", LoggerEnum.INFO.name());
        //User user = new User();

            return userService.userList();


       // ProjectLogger.log("list of users : "+users, LoggerEnum.INFO.name());
       // return new ResponseEntity<List<User>>(users, HttpStatus.OK);
    }
//
//    @RequestMapping (value = "/v1/updatepassword/{id}",method = RequestMethod.PUT)
//    public ResponseEntity<?> updatePassword(@RequestBody User userDTO){
//        ProjectLogger.log("update password api called:",LoggerEnum.INFO.name());
//        return userService.enableUserWithPassword(userDTO);
//    }

//    public ResponseEntity<JSONObject> getResponse(String message, HttpStatus status) {
//        ProjectLogger.log(message, LoggerEnum.ERROR.name());
//        JSONObject response = new JSONObject();
//        response.put("message", message);
//        ResponseEntity<JSONObject> failedResponse = new ResponseEntity<>(response, status);
//
//        return failedResponse;
//    }
}
