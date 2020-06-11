package com.space.userautomation.controller;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.model.UserCredentials;
import com.space.userautomation.services.UserService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usersubmission")
public class HealthController {

    @Autowired
    UserService userService;
    
    private String adminName = System.getenv("adminName");
    private String adminPassword = System.getenv("adminPassword");

    @RequestMapping(value = "/v1/health", method = RequestMethod.GET)
    public ResponseEntity<?> health() {
        ProjectLogger.log("Health Api hit : ", LoggerEnum.INFO.name());
        JSONObject response = new JSONObject();
        response.put("status", "success");
        response.put("spring_services", "UP");

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername(adminName);
        userCredentials.setPassword(adminPassword);
        String token = userService.getToken(userCredentials);

        ProjectLogger.log("Token : "+token, LoggerEnum.INFO.name());
        if(token.indexOf("error") > 0) {
            ProjectLogger.log("Keycloak is DOWN", LoggerEnum.WARN.name());
            response.put("keycloak_services", "DOWN");
        } else {
            ProjectLogger.log("Keycloak is UP", LoggerEnum.INFO.name());
            response.put("keycloak_services", "UP");
        }

        ResponseEntity<JSONObject> successReponse = new ResponseEntity<>(response, HttpStatus.OK);
        return successReponse;

    }
}
