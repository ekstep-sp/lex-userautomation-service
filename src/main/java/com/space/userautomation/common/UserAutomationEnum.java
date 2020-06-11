package com.space.userautomation.common;

public enum UserAutomationEnum {
    
    SUCCESS_RESPONSEs("200");
    
    public static int SUCCESS_RESPONSE_STATUS_CODE = 200;
    public static int BAD_REQUEST_STATUS_CODE = 400;
    public static int FORBIDDEN = 403;
    public static int INTERNAL_SERVER_ERROR = 403;
    
    
    

    private  Object value;

    UserAutomationEnum(String value) {
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
