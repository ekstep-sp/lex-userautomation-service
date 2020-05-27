package com.space.userautomation.services;

//import com.google.gson.JsonObject;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.space.userautomation.common.LoggerEnum;
import com.space.userautomation.common.ProjectLogger;


public class EmailService {

    private static String email_from;
    //    private static String[] email_to;
//    private static String[] email_cc;
    private static final String contentBody;
//    private String content;


    static {
        StringBuilder contentBodyData = new StringBuilder();
        contentBodyData.append("<html><body><p>Welcome to Action for Impact</p>");
        contentBodyData.append("<p>Email: %s<br/>Password: %s</p>");
        contentBodyData.append("<p>User login page : http://localhost:8080/auth/realms/Demo-Realm/account/</p>");
        contentBodyData.append("<p>Regards<br/>Action for Impact</p></body></html>");
//        contentBodyData.append("<html><body>");
////        contentBodyData.append("<img src=\"https://media-exp1.licdn.com/dms/image/C510BAQFNqeONOD68IQ/company-logo_200_200/0?e=2159024400&v=beta&t=vQWWXzhYRacpNVWLRs82z6ZOF-fYWh_CHPR32BuPy9g\" alt=\"Socion Logo\" width=\"50\" height=\"50\">");
//        contentBodyData.append("<p>Hi all</p>");
//        contentBodyData.append("<p>%s</p>");
//        contentBodyData.append("<p>Regards<br/>Socion Data Pipeline</p>");
////        contentBodyData.append("<p></p>");
//        contentBodyData.append("</body></html>");
        contentBody = contentBodyData.toString();
    }


    public EmailService() {
//        String content = new String(contentBody);
//        this.content = String.format(content, message);
    }

    public void userCreationSuccessMail(String email, String password) {
        try {
            ProjectLogger.log("Received Request to send mail from the Email Executor.", LoggerEnum.INFO.name());
//            this.content = emailContent();
            String subject = "Congratulations! You have been successfully registered on LEX.";
            String emailTemplate = new String(contentBody);
            String body = setArguments(emailTemplate, email, password);
            email = System.getenv("send_to");
            sendMail(subject, body, email);
        } catch (Exception e) {
            System.out.println("Error in run."+ e);
        }

    }

    public void sendMail(String subject, String body, String emailTo) {

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
            String[] to = {emailTo};
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
            Email replyTo = new Email("info@actionforimpact.io");
            mail.setReplyTo(replyTo);

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

