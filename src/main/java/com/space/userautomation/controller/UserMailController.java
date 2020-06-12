package com.space.userautomation.controller;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.services.EmailService;
import com.space.userautomation.services.UserService;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;

@RestController
@RequestMapping("/usersubmission/user")
public class UserMailController {
    
    EmailService emailService = new EmailService();
    Response responses = new Response();

    @RequestMapping(value = "/v1/decline", method = RequestMethod.POST)
    public ResponseEntity<?> sendDeclineMailToUsers(@RequestBody JSONObject jsonObject) {
        ProjectLogger.log("User registration Decline Api Hit.", LoggerEnum.INFO.name());
        Object emails = jsonObject.get("email");
        if(emails!=null && emails instanceof String) {
            String email = (String) emails;
            if(email.isEmpty()) {
                ProjectLogger.log("Bad Request Error.", LoggerEnum.ERROR.name());
                return responses.getResponse("Missing request param email",HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE,"","");
            } else {
                String emailArr[] = {email};
                emailService.userRegistrationDeclineMail(emailArr);
                return responses.getResponse("Success", HttpStatus.OK,UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE,"",emailArr);
            }
        } else if(emails!=null && emails instanceof ArrayList<?>) {
            ProjectLogger.log("Emails : "+emails, LoggerEnum.INFO.name());
            ArrayList<String> emailList = (ArrayList<String>) emails;
            String[] emailsToSend = emailList.toArray(new String[emailList.size()]);
            emailService.userRegistrationDeclineMail(emailsToSend);
            return responses.getResponse("Success", HttpStatus.OK,UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE,"",emailsToSend);
        } else {
            ProjectLogger.log("Bad Request Error.", LoggerEnum.ERROR.name());
            return responses.getResponse("Missing request param 'email'.", HttpStatus.BAD_REQUEST,UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE,"","");
        }
    }
}