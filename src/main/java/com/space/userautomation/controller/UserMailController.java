package com.space.userautomation.controller;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import com.space.userautomation.services.EmailService;
import com.space.userautomation.services.UserRoleService;
import com.space.userautomation.services.UserService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/usersubmission/user")
public class UserMailController {
        @Autowired
        UserService userService;
        EmailService emailService = new EmailService();
        Response responses = new Response();
        @RequestMapping(value = "/v1/decline",headers={"rootOrg","org","wid_OrgAdmin"}, method = RequestMethod.POST)
        public ResponseEntity<JSONObject> sendDeclineMailToUsers(User userData, @RequestHeader Map<Object, Object> header,@RequestBody JSONObject jsonObject) {
            try {
                Object emails = jsonObject.get("email");
                Object user_id = jsonObject.get("user_id");
                ProjectLogger.log("User registration Decline Api Hit.", LoggerEnum.INFO.name());
                userData.setApiId(responses.getApiId());
                userData.setRoot_org((String) header.get("rootorg"));
                userData.setOrganisation((String) header.get("org"));
                userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
                JSONObject jObj = new JSONObject((Map) new UserRoleService().getRoleForAdmin(userData).getBody().get("DATA"));
                Boolean isORG_ADMIN = (Boolean) jObj.get("ORG_ADMIN");
                if (isORG_ADMIN) {
                    if (userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getOrganisation().isEmpty()) && (!userData.getWid_OrgAdmin().isEmpty())) {
                        if (emails != null && emails instanceof String && user_id instanceof String) {
                            String email = (String) emails;
                            if (email.isEmpty()) {
                                ProjectLogger.log("Bad Request Error.", LoggerEnum.ERROR.name());
                                return responses.getResponse("Missing request param ", HttpStatus.BAD_REQUEST, 400, "", "");
                            } else {
                                String emailArr[] = {email};
                                userService.deleteUser(user_id.toString());
                                emailService.userRegistrationDeclineMail(emailArr);
                                return responses.getResponse("Success", HttpStatus.OK, 200, "", emailArr);
                            }
                        } else if (emails != null && emails instanceof ArrayList<?>) {
                            ProjectLogger.log("Emails : " + emails, LoggerEnum.INFO.name());
                            ArrayList<String> emailList = (ArrayList<String>) emails;
                            String[] emailsToSend = emailList.toArray(new String[emailList.size()]);
                            emailService.userRegistrationDeclineMail(emailsToSend);
                            return responses.getResponse("Success", HttpStatus.OK, 200, "", emailsToSend);
                        } else {
                            ProjectLogger.log("Bad Request Error.", LoggerEnum.ERROR.name());
                            return responses.getResponse("check the request params email,userId.", HttpStatus.BAD_REQUEST, 200, "", "");
                        }
                    } else {
                        ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                        return responses.getResponse("Please verify the headers before processing the request", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");
                    }
                } else {
                    return responses.getResponse("Permission denied,user role can be retireved by admin only", HttpStatus.FORBIDDEN, UserAutomationEnum.FORBIDDEN, "", "");
                }
            } catch (Exception ex) {
                ProjectLogger.log("Exception occured in acceptUser method", LoggerEnum.ERROR.name());
                return responses.getResponse("Please verify the headers before processing the request", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");
            }
        }
    }