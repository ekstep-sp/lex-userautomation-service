package com.space.userautomation.controller;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.database.cassandra.Cassandra;
import com.space.userautomation.model.UserCredentials;
import com.space.userautomation.services.UserService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

@RestController
@RequestMapping("/usersubmission")
public class HealthController {

    @Autowired
    UserService userService;

//    private final String url = "jdbc:postgresql://localhost:5432/keycloak";
//    private final String user = "root";
//    private final String password = "password";

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
        ProjectLogger.log("Token : " + token, LoggerEnum.INFO.name());
        if (token.indexOf("error") > 0) {
            ProjectLogger.log("Keycloak is DOWN", LoggerEnum.WARN.name());
            response.put("keycloak_services", "DOWN");
        } else {
            ProjectLogger.log("Keycloak is UP", LoggerEnum.INFO.name());
            response.put("keycloak_services", "UP");
        }
        response.put("cassandra", checkCassandraConnection());
//        response.put("postgresql", checkPostgresqlConnection());
        ResponseEntity<JSONObject> successReponse = new ResponseEntity<>(response, HttpStatus.OK);
        return successReponse;
    }

    public String checkCassandraConnection() {
        try {
            Cluster cluster = Cluster.builder()
                    .addContactPointsWithPorts(Arrays.asList(
                            new InetSocketAddress(System.getenv("cassandra.host"), Integer.parseInt(System.getenv("cassandra.port")))))
                    .withoutJMXReporting()
                    .build();
            if (!cluster.getMetadata().getAllHosts().isEmpty() && !(cluster.isClosed())) {
                String status = "UP";
                cluster.close();
                return status;
//                Cassandra.shutDownHook();
            } else {
                String status = "DOWN";
                return status;
            }
        } catch (NoHostAvailableException ex) {
            ProjectLogger.log("Cassandra cannot be connected,no host available", LoggerEnum.INFO.name());
            String status = "DOWN";
            return status;
        }
    }

//    public String checkPostgresqlConnection(){
//        try {
//            String url = "jdbc:postgresql://localhost:5432/keycloak";
//            Properties props = new Properties();
//            props.setProperty("user", "root");
//            props.setProperty("password", "password");
//            props.setProperty("ssl", "true");
//            Connection conn = DriverManager.getConnection(url, props);
//            String status = "UP";
//            return status;
//        } catch (SQLException e) {
//            ProjectLogger.log("Postgresql cannot be connected.", LoggerEnum.INFO.name());
//            String status = "DOWN";
//            return status;
//        }
//    }
}


