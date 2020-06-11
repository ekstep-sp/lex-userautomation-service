package com.space.userautomation.services;

//import com.google.gson.JsonObject;
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
import java.util.Locale;


public class EmailService {

    private static String email_from;
    private String platformLink = System.getenv("platformLink");
    
    private String domain = System.getenv("domain");
    
    
    //    private static String[] email_to;
//    private static String[] email_cc;
//    private static final String contentBody;
//    private static final String declineContentBody;
////    private String content;
//
//
//    static {
//        StringBuilder contentBodyData = new StringBuilder();
//        contentBodyData.append("<html><body><p>Hi,</p>");
//        contentBodyData.append("<p>We have received a new request for registration on the Action For Impact platform.</p>");
//        contentBodyData.append("<p><The details submitted with the request are as below:/p>");
////        contentBodyData.append("<p style='margin: 0'>Here is a new request for registration on the Action For Impact platform. Please find below the details:</p>");
//        contentBodyData.append("<p style='margin: 0'>Name: %s<br/>Email: %s<br/>");
//        // Next Line is for the conditional organisation value
//        contentBodyData.append("%s</p>");
//        contentBodyData.append("<p>The request was submitted on %s and %s (%s).</p>");
//        contentBodyData.append("<p>Requesting you to please confirm accept or decline by responding to this email.</p>");
//        contentBodyData.append("<p>Have a great day!<br/>Warm regards,<br/>Action for Impact Team</p></body></html>");
////        contentBodyData.append("<html><body>");
//////        contentBodyData.append("<img src=\"https://media-exp1.licdn.com/dms/image/C510BAQFNqeONOD68IQ/company-logo_200_200/0?e=2159024400&v=beta&t=vQWWXzhYRacpNVWLRs82z6ZOF-fYWh_CHPR32BuPy9g\" alt=\"Socion Logo\" width=\"50\" height=\"50\">");
////        contentBodyData.append("<p>Hi all</p>");
////        contentBodyData.append("<p>%s</p>");
////        contentBodyData.append("<p>Regards<br/>Socion Data Pipeline</p>");
//////        contentBodyData.append("<p></p>");
////        contentBodyData.append("</body></html>");
//        contentBody = contentBodyData.toString();
//
//        StringBuilder declineContent = new StringBuilder();
//        declineContent.append("<html><body><p>Hi,</p>");
//        declineContent.append("<p>Thank you for your request for registration on the Action For Impact platform.</p>");
//        declineContent.append("<p>Unfortunately, owing to our current scope and policy, we are not able to accept your request.</p>");
//        declineContent.append("<p>We wish you all the very best and hope to welcome you in the future phases of the platform.</p>");
//        declineContent.append("<p>Warm regards,<br/>Action for Impact Team</p></body></html>");
//
//        declineContentBody = declineContent.toString();
//    }


    public EmailService() {
//        String content = new String(contentBody);
//        this.content = String.format(content, message);
    }
    public EmailService(String content) {
//        String content = new String(contentBody);
//        this.content = String.format(content, message);
    }

    public void userCreationSuccessMail(String name, String email, String password, String organisation) {
        try {
            ProjectLogger.log("Received Request to send mail from the Email Executor.", LoggerEnum.INFO.name());
//            this.content = emailContent();
            String subject = "Action for Impact. New User Registration request.";
            // String emailTemplate = new String(contentBody);
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
            System.out.println("Error in run."+ e);
        }
    }

    public void userRegistrationDeclineMail(String emails[]) {
        try {
            ProjectLogger.log("Received Request to send mail for declining Registration.", LoggerEnum.INFO.name());
            String subject = "Action for Impact. Registration request declined.";
            //  String emailTemplate = new String(declineContentBody);
            TemplateParser parser = new TemplateParser(EmailTemplate.declineContentTemplate);
            String body = setArguments(parser.getContent(),domain ,domain);
            sendMail(subject, body, emails, false);
        } catch (Exception e) {
            System.out.println("Error in run."+ e);
        }
    }

    public void acceptmail(String name, String password, String organisation) {
        try {
            ProjectLogger.log("Received request to send mail for accepting registration.", LoggerEnum.INFO.name());
            String subject = "Approved registration request.";
            String org = "";
            if(!organisation.isEmpty()) {
                org = "Organisation: " + organisation;
            }
            TemplateParser parser = new TemplateParser(EmailTemplate.acceptContentTemplate);
            String body = setArguments(parser.getContent(),name, password, platformLink, domain);
//            String email = "";
//            email = System.getenv("send_to");
            String email = name;
//            email = System.getenv("send_to");
            String emailTo[] = {email};
            sendMail(subject, body, emailTo, true);
        } catch (Exception e) {
            System.out.println("Error in run."+ e);
        }
    }
    public void sendMail(String subject, String body, String emailTo[], boolean replyToCheck) {

        try {
            // create email object for the sender email
            Email from = new Email(getEmailSenderFrom());
            from.setName("Action for Impact");
            // initialize the content with the message to send
            Content content = new Content();
            content.setType("text/html");
            content.setValue(body);
//                Content content = new Content("text/plain", this.content);
//        Mail mail = new Mail(from, this.template.getSubject(), to, content);


            Personalization personalization = new Personalization();

            // create email object for the each receiver email
            String[] to = emailTo;
//            System.out.println(Arrays.toString(to));
            for (String toEach : to) {
                Email toEmail = new Email(toEach.trim());
                personalization.addTo(toEmail);
            }

            // create email object for the each receiver CC email
//                String[] cc = getEmailReceiverCC();
//            System.out.println(Arrays.toString(cc));
//                if (cc != null || cc.length > 0) {
//                    for (String ccEach : cc) {
////                    System.out.println(ccEach);
//                        Email ccEmail = new Email(ccEach.trim());
//                        personalization.addCc(ccEmail);
//                    }
//                }


            // creae the mail object and initialize it
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
//            System.out.println(response.getBody());
        } catch (Exception ex) {
            ProjectLogger.log("Failed to send mail. ", ex, LoggerEnum.ERROR.name());
        }
    }

    private String getEmailSenderFrom() {
        if (email_from == null || email_from.isEmpty()) {
            email_from = System.getenv("send_from").trim();
        }
        return email_from;
    }

    public String setArguments(String EmailTemplate , Object... args) {
//        System.out.println("template.getContent() : " + content);
        String newContent = String.format(EmailTemplate, args);
        return newContent;
    }

//    private String[] getEmailReceiverTo() {
//        if (email_to == null || email_to.length == 0) {
//            String to = prop.getProperty("send_to");
//            email_to = to.trim().split(",");
//        }
//        return email_to;
//    }

//    private String[] getEmailReceiverCC() {
//        if (email_cc == null || email_cc.length == 0) {
//            PropertiesCache prop = PropertiesCache.getInstance();
//            String cc = prop.getProperty("send_cc");
//            if (cc == null) {
//                email_cc = new String[0];
//            } else {
//                email_cc = cc.trim().split(",");
//            }
//        }
//        return email_cc;
//    }

//    private String emailContent() {
//
//        String content = new String(contentBody);
////        return String.format(content, message);
//
//        parser = new TemplateParser(EmailTemplates.finalEmailWithAllDataTemplate);
//        JsonObject count = new PostgresImpl().getData();
//        long dataCount = count.get(Constants.TOTAL).getAsInt();
//        long successCount = count.get(Constants.SUCCESS).getAsInt();
//        long failureCount = count.get(Constants.FAILURE).getAsInt();
//
//        LocalDate currentDate = LocalDate.now();
//        parser.setArguments(dataCount, currentDate, successCount, failureCount);
//        String message = parser.getContent();
//
//        return String.format(content, message);
//    }
}

