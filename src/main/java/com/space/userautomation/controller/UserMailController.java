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
    String roleForAdminUser = "org-admin";

    private String allowSendMail = System.getenv("allowSendMail");

    @RequestMapping(value = "/v1/decline", headers = {"rootOrg", "org", "wid_OrgAdmin"}, method = RequestMethod.POST)
    public ResponseEntity<JSONObject> sendDeclineMailToUsers(User userData, @RequestHeader Map<Object, Object> header, @RequestBody JSONObject jsonObject) {
        try {

            userData.setApiId(responses.getApiId());
            userData.setRoot_org((String) header.get("rootorg"));
            userData.setOrganisation((String) header.get("org"));
            userData.setWid_OrgAdmin((String) header.get("wid_orgadmin"));
            if ((userData.getRoot_org().equals(System.getenv("rootOrg")) && (!userData.getRoot_org().isEmpty())) && (userData.getOrganisation().equals(System.getenv("org")) && (!userData.getOrganisation().isEmpty())) && (!userData.getWid_OrgAdmin().isEmpty())) {
                Object emails = jsonObject.get("email");
                Object user_id = jsonObject.get("user_id");
                ProjectLogger.log("User registration Decline Api Hit.", LoggerEnum.INFO.name());
                userData.setWid_user((String)jsonObject.get("wid"));
                JSONObject jObj = new JSONObject((Map) new UserRoleService().getRoleForAdmin(userData).getBody().get("DATA"));
                Boolean isORG_ADMIN = (Boolean) jObj.get(roleForAdminUser);
                if (isORG_ADMIN) {
                    if (emails != null && emails instanceof String && user_id instanceof String) {
                        String email = (String) emails;
                        if (email.isEmpty() && userData.getWid_user().isEmpty() && ((String) user_id).isEmpty()) {
                            ProjectLogger.log("Bad Request Error.", LoggerEnum.ERROR.name());
                            return responses.getResponse("Missing request param ", HttpStatus.BAD_REQUEST, 400, "", "");
                        } else {
                            String emailArr[] = {email};
                            JSONObject job = new JSONObject(userService.deleteUser(user_id.toString()));
                            Integer statusCode = (Integer) job.get("statusCode");
                            if (statusCode == 204) {
                                
                                //delete the user data from uer autocomplete table in postgresql
//                                JSONObject jsonObject_userautocomplete = userService.deleteUserFromUserAutoComplete(emails.toString(), userData.getWid_user());
//                                String userAutocompleteStatus = (String) jsonObject_userautocomplete.get("status");
                                //delete the user data from user table in postgresql
                                JSONObject jsonObject_user = userService.deleteUserFromUserTable(emails.toString(),user_id.toString(), userData);
                                String userStatus = (String) jsonObject_user.get("status");
                                
                                //delete the user data from user tnc in cassandra
                                JSONObject jsonObject_userTncTable = userService.deleteUserFromUserTncTable(user_id.toString(), userData );
                                String userTncTable = (String) jsonObject_userTncTable.get("status");
                                if(userStatus.equals("true") && userTncTable.equals("true")){
                                    if (allowSendMail.equals("true")) {
                                        emailService.userRegistrationDeclineMail(emailArr);
                                    }
                                    ProjectLogger.log("User deleted successfully", LoggerEnum.ERROR.name());
                                    return responses.getResponse("User deleted successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", emailArr);
                                }
                                else{
                                    ProjectLogger.log("Failed to delete user from table.", LoggerEnum.ERROR.name());
                                    return responses.getResponse("User could not be deleted.", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, "", "");
                                }

                            } else {
                                ProjectLogger.log("Failed to delete user.", LoggerEnum.ERROR.name());
                                return responses.getResponse("User could not be deleted.", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, "", "");
                            }
                        }
                    } else if (emails != null && emails instanceof ArrayList<?>) {
                        ProjectLogger.log("Emails : " + emails, LoggerEnum.INFO.name());
                        ArrayList<String> emailList = (ArrayList<String>) emails;
                        String[] emailsToSend = emailList.toArray(new String[emailList.size()]);
                        JSONObject job = new JSONObject(userService.deleteUser(user_id.toString()));
                        Integer statusCode = (Integer) job.get("statusCode");
                        if (statusCode == 204) {
//                            //delete the user data from uer autocomplete table in postgresql
//                            userService.deleteUserFromUserAutoComplete(emails.toString(), user_id.toString());

                            JSONObject jsonObject_user = userService.deleteUserFromUserTable(emails.toString(),user_id.toString(), userData);
                            String userStatus = (String) jsonObject_user.get("status");

                            //delete the user data from user tnc in cassandra
                            JSONObject jsonObject_userTncTable = userService.deleteUserFromUserTncTable(user_id.toString(), userData );
                            String userTncTable = (String) jsonObject_userTncTable.get("status");
                            if(userStatus.equals("true") && userTncTable.equals("true")){
                                if (allowSendMail.equals("true")) {
                                    emailService.userRegistrationDeclineMail(emailsToSend);
                                }
                                ProjectLogger.log("User deleted successfully", LoggerEnum.ERROR.name());
                                return responses.getResponse("User deleted successfully", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", emailsToSend);
                            }
                            else{
                                ProjectLogger.log("Failed to delete user from table.", LoggerEnum.ERROR.name());
                                return responses.getResponse("User could not be deleted.", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, "", "");
                            }
//                        if (allowSendMail.equals("true")) {
//                            emailService.userRegistrationDeclineMail(emailsToSend);
//                        }
//                        return responses.getResponse("Success", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, userData.getApiId(), emailsToSend);
                        } else {
                            ProjectLogger.log("Failed to delete user.", LoggerEnum.ERROR.name());
                            return responses.getResponse("User could not be deleted.", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, "", "");
                        }
                    } else {
                        ProjectLogger.log("User cannot be deleted, please provide appropriate params.", LoggerEnum.ERROR.name());
                        return responses.getResponse("User cannot be deleted, please provide appropriate params", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");

                    }
                } else {
                    return responses.getResponse("Permission denied,user role can be deleted by admin only", HttpStatus.FORBIDDEN, UserAutomationEnum.FORBIDDEN, userData.getApiId(), "");
                }
            }
            else {
                ProjectLogger.log("Inapproriate headers in request.", LoggerEnum.ERROR.name());
                return responses.getResponse("Please verify the headers before processing the request", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured in decline user method", LoggerEnum.ERROR.name());
            return responses.getResponse("Internal Server Error", HttpStatus.BAD_REQUEST, UserAutomationEnum.BAD_REQUEST_STATUS_CODE, userData.getApiId(), "");
        }
    }
    }