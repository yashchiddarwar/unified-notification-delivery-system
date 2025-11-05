package com.Portfolio.Notifire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NotifireApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotifireApplication.class, args);
	}

}
