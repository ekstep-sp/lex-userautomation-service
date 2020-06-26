package com.space.userautomation.database.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.Response;
import com.space.userautomation.common.UserAutomationEnum;
import com.space.userautomation.model.User;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Cassandra {

    static Cluster cluster;

    private static String tableName = System.getenv("tableName");
    private static String keyspaceName = System.getenv("keyspaceName");
    Response response = new Response();


    static {
        try {
//            cluster = Cluster.builder().addContactPoint("127.0.0.1").withPort(9042).withoutJMXReporting().build();
            cluster = Cluster.builder()
                    .addContactPointsWithPorts(Arrays.asList(
                            new InetSocketAddress(System.getenv("cassandra_host"), Integer.parseInt(System.getenv("cassandra_port")))))
                    .withoutJMXReporting()
                    .build();
            ProjectLogger.log("Connecting to cassandra db", LoggerEnum.INFO.name());
            shutDownHook();
        } catch (NoHostAvailableException noHostAvailableException) {
            ProjectLogger.log("Exception occured while connecting to the host", noHostAvailableException, LoggerEnum.ERROR.name());
            System.exit(-1);
        } catch (Exception ex) {
            ProjectLogger.log("Failed to connect to cassandra", ex, LoggerEnum.FATAL.name());
            System.exit(-1);
        }
    }

    public static void shutDownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new CassandraConnection());
    }


    public ResponseEntity<JSONObject> insertUser(Map<String, Object> userData) {
        System.out.println("userID" + userData.get("user_id"));
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(keyspaceName + "." + tableName + "(");
        for (Map.Entry<String, Object> entry : userData.entrySet()) {
            query.append(entry.getKey() + ",");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(")" + "VALUES" + "(");
        for (Map.Entry<String, Object> entry : userData.entrySet()) {
            query.append("?" + ",");
        }
        query.deleteCharAt(query.length() - 1);
        query.append(");");
//        query.append(") IF NOT EXISTS;");
        try {
            Session session = cluster.connect();
            PreparedStatement prepared = session.prepare(
                    String.valueOf(query));
            BoundStatement bound = prepared.bind().setString("user_id", (String) userData.get("user_id")).setString("root_org", (String) userData.get("root_org"))
                    .setList("roles", (List<String>) userData.get("roles"));
            session.execute(bound);
            session.close();
            return response.getResponse("User Role created", HttpStatus.OK, UserAutomationEnum.SUCCESS_RESPONSE_STATUS_CODE, "", userData);

        } catch (WriteTimeoutException writeTimeOutException) {
            ProjectLogger.log("Exception occured while inserting to cassandra", writeTimeOutException, LoggerEnum.ERROR.name());
            return response.getResponse("User Role created", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        } catch (InvalidQueryException invalidQueryException) {
            ProjectLogger.log("Exception occured while inserting to cassandra", invalidQueryException, LoggerEnum.ERROR.name());
            return response.getResponse("User Role created", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured while processing the data to Cassandra", ex, LoggerEnum.ERROR.name());
            return response.getResponse("User Role created", HttpStatus.BAD_REQUEST, UserAutomationEnum.INTERNAL_SERVER_ERROR, "", userData);
        }
    }

    public List<User> getUserRoles(Map<String, Object> userData) {
        List<User> userDetails = new ArrayList<User>();
        System.out.println("userID" + userData.get("user_id"));
        StringBuilder query = new StringBuilder();
        query.append("SELECT roles FROM ");
        query.append(keyspaceName + "." + tableName + " ");
        query.append("WHERE user_id = '");
        query.append(userData.get("user_id"));
        query.append("' AND root_org = '");
        query.append(userData.get("root_org"));
        query.append("' ALLOW FILTERING;");
        try {
            Session session = cluster.connect();
            PreparedStatement prepared = session.prepare(String.valueOf(query));
            BoundStatement bound = prepared.bind();
            ResultSet resultSet = session.execute(bound);
            if (!resultSet.isExhausted()) {
                Row row = resultSet.one();
//                userDetails.add(new User(row.getList("roles", String.class)));
                return userDetails;
            }
        } catch (Exception ex) {
            ProjectLogger.log("Exception occured while retrieving user role for the given userId", ex, LoggerEnum.ERROR.name());
        }
        return userDetails;
    }

    static class CassandraConnection extends Thread {
        @Override
        public void run() {
            ProjectLogger.log("Killing Cassandra Connection.", LoggerEnum.INFO.name());
            try {
                if (cluster != null) {
                    cluster.close();
                }
            } catch (Exception e) {
                ProjectLogger.log("failed to kill Cassandra Connection.", e, LoggerEnum.FATAL.name());
            }
        }
    }

    public static String healthCheck() {
        String returnStatus = new String();
        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM ");
            query.append(keyspaceName+"."+tableName +";");
            Session session = cluster.connect();
            PreparedStatement prepared = session.prepare(String.valueOf(query));
            BoundStatement bound = prepared.bind();
            ResultSet resultSet = session.execute(bound);
            if (!resultSet.isExhausted()) {
                returnStatus = "UP";
                ProjectLogger.log("Cassandra cluster connected " + returnStatus, LoggerEnum.INFO.name());
                return returnStatus;
            }
        } catch (Exception ex) {
            returnStatus = "DOWN";
            ProjectLogger.log("Exception" + ex + "occured while connecting to cassnadra.", LoggerEnum.INFO.name());
            return returnStatus;
        }
        return returnStatus;
    }
}