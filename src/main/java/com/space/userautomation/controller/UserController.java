package com.space.userautomation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    public static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;


    Response response = new Response();

    @RequestMapping(value = "/v1/token", method = RequestMethod.POST)
    public ResponseEntity<?> getTokenUsingCredentials(@RequestBody UserCredentials userCredentials) {
        String responseToken = null;
        try {
            ProjectLogger.log("token created:" + userCredentials, LoggerEnum.INFO.name());
            responseToken = userService.getToken(userCredentials);
        } catch (Exception e) {
            ProjectLogger.log("Exception occured in gettokencedentials", LoggerEnum.INFO.name());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.put("Content-Type", Arrays.asList("application/json; charset=utf-8"));
        return new ResponseEntity<>(responseToken, headers, HttpStatus.OK);
    }
}
