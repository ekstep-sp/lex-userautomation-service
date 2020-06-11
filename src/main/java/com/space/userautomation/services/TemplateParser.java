package com.space.userautomation.services;


public class TemplateParser {
    private String content;
    
    public TemplateParser(EmailTemplate template) {
        this.content = template.getContent();
    }
    
    public String getContent() {
        return content;
    }
    
    public void setArguments(Object... args) {
        System.out.println("template.getContent() : " + content);
        String newContent = String.format(content, args);
        this.content = newContent;
    }
    
}