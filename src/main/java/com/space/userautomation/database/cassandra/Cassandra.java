package com.space.userautomation.database.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.WriteTimeoutException;
import com.space.userautomation.common.ProjectLogger;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.model.User;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Cassandra {

    static Cluster cluster;

    private String tableName = System.getenv("tableName");
    private String keyspaceName = System.getenv("keyspaceName");

    static {
        try {
            cluster = Cluster.builder()
                    .addContactPointsWithPorts(Arrays.asList(
                            new InetSocketAddress(System.getenv("cassandra.host"), Integer.parseInt(System.getenv("cassandra.port")))))
                    .withoutJMXReporting()
                    .build();
            ProjectLogger.log("Connecting to cassandra db", LoggerEnum.INFO.name());
            shutDownHook();
        }
        catch(NoHostAvailableException noHostAvailableException){
            ProjectLogger.log("Exception occured while connecting to the host", noHostAvailableException, LoggerEnum.ERROR.name());
            System.exit(-1);
        }
        catch (Exception ex) {
            ProjectLogger.log("Failed to connect to cassandra", ex, LoggerEnum.FATAL.name());
            System.exit(-1);
        }
    }

    public static void shutDownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new CassandraConnection());
    }


    public void insertUser(Map<String, Object> userData) {
        System.out.println("userID" + userData.get("user_id"));
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ");
        query.append(keyspaceName +"."+tableName +"(");
        for(Map.Entry<String, Object> entry :userData.entrySet()) {
            query.append(entry.getKey() + ",");
        }
        query.deleteCharAt(query.length()-1);
        query.append(")"+"VALUES"+"(");
        for(Map.Entry<String, Object> entry :userData.entrySet()) {
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
                    .setList("roles",(List<String>) userData.get("roles"));
            session.execute(bound);
            session.close();
        }
        catch (WriteTimeoutException writeTimeOutException){
            ProjectLogger.log("Exception occured while inserting to cassandra", writeTimeOutException, LoggerEnum.ERROR.name());
        }
//        catch(QueryValidationException queryValidationException){
//            ProjectLogger.log("Exception occured while inserting to cassandra", queryValidationException, LoggerEnum.ERROR.name());
//        }
        catch(InvalidQueryException invalidQueryException){
            ProjectLogger.log("Exception occured while inserting to cassandra", invalidQueryException, LoggerEnum.ERROR.name());
        }
        catch(Exception ex) {
            ProjectLogger.log("Exception occured while processing the data to Cassandra", ex, LoggerEnum.ERROR.name());
        }
    }

    public List<User> getUserRoles(Map<String, Object> userData){
        List<User> userDetails = new ArrayList<User>();
        System.out.println("userID" + userData.get("user_id"));
        StringBuilder query = new StringBuilder();
        query.append("SELECT roles FROM ");
        query.append(keyspaceName +"."+tableName + " " );
        query.append("WHERE user_id = '");
        query.append(userData.get("user_id"));
        query.append("';");
        try{
            Session session = cluster.connect();
            PreparedStatement prepared = session.prepare( String.valueOf(query));
            BoundStatement bound = prepared.bind();
            ResultSet resultSet = session.execute(bound);
            if (!resultSet.isExhausted()) {
                Row row = resultSet.one();
                userDetails.add(new User(row.getList("roles",String.class)));
                return  userDetails;
            }
        }
        catch(Exception ex){
            ProjectLogger.log("Exception occured while retrieving user role for the given userId", ex, LoggerEnum.ERROR.name());
        }
        return userDetails;
    }

    static class CassandraConnection extends Thread {
        @Override
        public void run() {
            ProjectLogger.log("Killing Cassandra Connection.", LoggerEnum.INFO.name());
            try {
                if(cluster!= null){
                    cluster.close();
                }
            } catch (Exception e) {
                ProjectLogger.log("failed to kill Cassandra Connection.", e, LoggerEnum.FATAL.name());
            }
        }
    }
}

