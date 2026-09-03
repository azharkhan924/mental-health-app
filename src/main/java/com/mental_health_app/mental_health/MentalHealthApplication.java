package com.mental_health_app.mental_health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MentalHealthApplication {

	public static void main(String[] args) {
		// Set JVM timezone to IST so LocalDateTime.now() returns Indian time everywhere
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(MentalHealthApplication.class, args);
	}

}
