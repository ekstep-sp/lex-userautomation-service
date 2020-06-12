package com.space.userautomation.services;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;
import java.time.LocalDateTime;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class EmailService {

    private static String email_from;
    private String platformLink = System.getenv("platformLink");
    
    private String domain = System.getenv("domain");

    public EmailService() {
    }
    public EmailService(String content) {
    }

    public void userCreationSuccessMail(String name, String email, String password, String organisation) {
        try {
            ProjectLogger.log("Received Request to send mail from the Email Executor.", LoggerEnum.INFO.name());
            String subject = domain + " New User Registration request.";
            TemplateParser parser = new TemplateParser(EmailTemplate.contentTemplate);
            String org = "";
            if(!organisation.isEmpty()) {
                org = "Organisation: " + organisation;
            }
            LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            LocalDate date = localDateTime.toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            formatter.withZone(ZoneId.of("Asia/Kolkata"));
            LocalTime time = localDateTime.toLocalTime();
            String formattedTime = formatter.format(time);
            String body = setArguments(parser.getContent(), domain, domain, name, email, org, date, formattedTime, "IST",domain);
            email = System.getenv("send_to");
            String emailTo[] = {email};
            sendMail(subject, body, emailTo, true);
        } catch (Exception e) {
            ProjectLogger.log("Failed to send mail in userCreationSuccessMail " + e, LoggerEnum.ERROR.name());
        }
    }

    public void userRegistrationDeclineMail(String emails[]) {
        try {
            ProjectLogger.log("Received Request to send mail for declining Registration.", LoggerEnum.INFO.name());
            String subject = domain+" Registration request declined.";
            TemplateParser parser = new TemplateParser(EmailTemplate.declineContentTemplate);
            String body = setArguments(parser.getContent(),domain ,domain);
            sendMail(subject, body, emails, false);
        } catch (Exception e) {
            ProjectLogger.log("Failed to send mail in userRegistrationDeclineMail " + e, LoggerEnum.ERROR.name());
        }
    }

    public void acceptmail(String name, String password, String organisation) {
        try {
            ProjectLogger.log("Received request to send mail for accepting registration.", LoggerEnum.INFO.name());
            String subject = "Welcome to "+ domain;
            String org = "";
            if(!organisation.isEmpty()) {
                org = "Organisation: " + organisation;
            }
            TemplateParser parser = new TemplateParser(EmailTemplate.acceptContentTemplate);
            String body = setArguments(parser.getContent(),platformLink,domain,name, password, domain);
            String email = name;
            String emailTo[] = {email};
            sendMail(subject, body, emailTo, true);
        } catch (Exception e) {
            ProjectLogger.log("Failed to send mail in acceptmail " + e, LoggerEnum.ERROR.name());
        }
    }
    
    public void sendMail(String subject, String body, String emailTo[], boolean replyToCheck) {

        try {
            // create email object for the sender email
            Email from = new Email(getEmailSenderFrom());
            from.setName(domain);
            // initialize the content with the message to send
            Content content = new Content();
            content.setType("text/html");
            content.setValue(body);
            Personalization personalization = new Personalization();

            // create email object for the each receiver email
            String[] to = emailTo;
            for (String toEach : to) {
                Email toEmail = new Email(toEach.trim());
                personalization.addTo(toEmail);
            }
            // create the mail object and initialize it
            Mail mail = new Mail();
            mail.setFrom(from);
            if(replyToCheck) {
                Email replyTo = new Email(System.getenv("reply_to"));
                mail.setReplyTo(replyTo);
            }

            // add subject to mail
            mail.setSubject(subject);

            // add content to mail
            mail.addContent(content);
            mail.addPersonalization(personalization);

            SendGrid sg = new SendGrid(System.getenv("sendgrid_api_key"));

            // initialize the request object and send the request
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if(response.getStatusCode() != 202) {
                throw new Exception(response.getBody());
            }
            ProjectLogger.log("Successfully sent the mail. Response Code : " + response.getStatusCode(), LoggerEnum.INFO.name());
        } catch (Exception ex) {
            ProjectLogger.log("Failed to send mail in sendMail method", ex, LoggerEnum.ERROR.name());
        }
    }

    private String getEmailSenderFrom() {
        if (email_from == null || email_from.isEmpty()) {
            email_from = System.getenv("send_from").trim();
        }
        return email_from;
    }

    public String setArguments(String EmailTemplate , Object... args) {
        String newContent = String.format(EmailTemplate, args);
        return newContent;
    }
}

