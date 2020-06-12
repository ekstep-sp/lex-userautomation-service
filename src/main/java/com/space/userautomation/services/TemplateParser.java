package com.space.userautomation.services;

import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;

public class TemplateParser {
    
    private String content;
    
    public TemplateParser(EmailTemplate template) {
        this.content = template.getContent();
    }
    
    public String getContent() {
        return content;
    }
    
    public void setArguments(Object... args) {
        ProjectLogger.log("Template content from setArguments" + content, LoggerEnum.INFO.name());
        String newContent = String.format(content, args);
        this.content = newContent;
    }
}