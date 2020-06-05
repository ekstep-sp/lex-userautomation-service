package com.space.userautomation.controller;


import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.model.User;
import com.space.userautomation.services.UserRoleService;
import com.space.userautomation.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usersubmission/user")
public class UserRoleController {

    @Autowired
    UserRoleService userRoleService;

    @RequestMapping(value = "/v1/create", headers={"root_org=space","org=space"}, method = RequestMethod.POST)
    public ResponseEntity<?> createUserRole(@RequestBody User userdata ,  @RequestHeader Map<String, String> header){
        ProjectLogger.log("Creating user role in cassandra", LoggerEnum.INFO.name());

        try {
            
            UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
            userdata.setApiId(builder.buildAndExpand().getPath());
            userdata.setRoot_org(header.get("root_org"));
            userdata.setOrganisation(header.get("org"));
            return userRoleService.createUserRole(userdata);
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
            UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequest();
            user.setApiId(builder.buildAndExpand().getPath());
            return userRoleService.getUserRoles(user);
        }
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
