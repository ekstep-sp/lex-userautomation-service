package com.space.userautomation.common;

public enum UserAutomationEnum {
    
    SUCCESS_RESPONSES("200");
    
    public static int SUCCESS_RESPONSE_STATUS_CODE = 200;
    public static int BAD_REQUEST_STATUS_CODE = 400;
    public static int FORBIDDEN = 403;
    public static int INTERNAL_SERVER_ERROR = 500;
    public static int NO_CONTENT = 204;
    public static int CREATED = 201;
    public static int NOT_IMPLEMENTED = 409;
    
    
    private  Object value;

    UserAutomationEnum(String value) {
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
