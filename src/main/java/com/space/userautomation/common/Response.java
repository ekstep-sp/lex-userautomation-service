package com.space.userautomation.common;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.SimpleDateFormat;
import java.util.*;

public class Response {

    private String operation;
    private Map<String, Object> errorData;
    private Map<String, Object> successData;
    private HttpStatus status;
    private Integer statusCode;


    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Map<String, Object> getErrorData() {
        return errorData;
    }

    public void setErrorData(Map<String, Object> errorData) {
        this.errorData = errorData;
    }

    public Map<String, Object> getSuccessData() {
        return successData;
    }

    public void setSuccessData(Map<String, Object> successData) {
        this.successData = successData;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public Response() {
        errorData = new HashMap<>();
        successData = new HashMap<>();
        operation = "";
    }
    
    public void addErrorData(String key, Object data){
            errorData.put(key,data);
    }
    
    public ResponseEntity<JSONObject> getResponse(String message, HttpStatus status,Integer statusCode,String api,Object data) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date();
        JSONObject response = new JSONObject();
        response.put("STATUS", status);
        response.put("STATUS_CODE",statusCode);
        response.put("TIME_STAMP", formatter.format(date));
        response.put("API_ID",api);
        response.put("MESSAGE", message);
        response.put("DATA", data);
        ResponseEntity<JSONObject> responseData = new ResponseEntity<JSONObject>(response, status);
        return responseData;
    }


    public void addSuccessData(String key, Object data) {
        this.successData.put(key, data);
    }

    public ResponseEntity<JSONObject> getAllResponse() {
        SimpleDateFormat formatter = new SimpleDateFormat("DATE_TIME_FORMAT");
        Date date = new Date();
        JSONObject response = new JSONObject();
        response.put("TIME_STAMP", formatter.format(date));
        if (errorData.size() > 0) {
            response.put(status, "FAILURE");
            response.put("ERROR_DETAILS", errorData);
        }
        if (successData.size() > 0) {
            response.put(status, "SUCCESS");
            response.put("SUCCESS_DETAILS", successData);
        }
        ResponseEntity<JSONObject> responseData = new ResponseEntity<JSONObject>(response, status);
        return responseData;
    }
}