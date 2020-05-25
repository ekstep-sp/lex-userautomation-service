package com.space.userautomation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class UserAutomationApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserAutomationApplication.class, args);
	}

}
