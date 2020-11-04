package com.space.userautomation.services;

public enum EmailTemplate {

    declineContentTemplate("<p>Hi,</p>" +
            "<p style=margin:0px >Thank you for your request for registration on the %s</p>" +
            "<p style=margin:0px >Unfortunately, owing to our current scope and policy, we are not able to accept your request.</p>" +
            "<p style=margin:0px >We wish you all the very best and hope to welcome you in the future phases of the platform.</p>" +
            "<p>Warm regards,<br/>%s Team</p>"),


    acceptContentTemplate("<p>Hi,</p>" +
            "<p>Please find below your credentials for the <a href = %s >%s Platform</a>.</p>" +
            "<p>Username:  %s <br/>Password: %s</p>" +
            "<p>Please login to the platform here and feel free to change the password using the 'Forgot Password' option. Please note that these are your personal credentials and should not be shared with anyone else.</p>"+
                                  "<p>Have a great day !<br/>Warm regards,<br/>%s Team</p>"),


    changeRoleTemplate1("<p>Hi %s,</p>" +
            "<p>%s has been designed with different roles to ensure quality of assets and standards for the community of people on %s.</p>"+
            "<p>There has been a change in the roles assigned to you on the platform.</p><p><u>Earlier you had the below roles:</u><br/>"),
    changeRoleTemplate2("</p><p><u>Now, you have the following roles assigned to you:</u><br/>"),
    roleDefinition("<p>Role %s: <b>%s</b> - %s</p>"),
    changeRoleTemplate3("</p><p>If you have any queries, please write back to us.</p>"+
            "<p><br/>Warm regards,<br/>%s Team</p>"),

    contentTemplate("Hi,<br/> <p>We have received a new request for registration on the %s  platform<br/></p>" +
                            "<p>The details submitted with the request are as below:<br/></p>" +
                            "<p>Here is a new request for registration on the %s platform. Please find below the details:<br/></p>" +
                            "<p>Name: %s<br/>Email: %s<br/>The request was submitted on %s and %s (%s).<br/></p>" +
                            "<p>Requesting you to please confirm accept or decline by responding to this email.<br/></p>" +
                            "<p>Have a great day!<br/>Warm regards,<br/>%s Team");


    private String content;
    EmailTemplate(String content) {
        this.content = content;
    }
    public String getContent() {
        return content;
    }
}
