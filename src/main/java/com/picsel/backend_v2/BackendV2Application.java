package com.picsel.backend_v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendV2Application {

	public static void main(String[] args) {
		SpringApplication.run(BackendV2Application.class, args);
	}
}
