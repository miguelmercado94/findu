package com.findu.security.autorization_server_oauth2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AutorizationServerOauth2Application {

	public static void main(String[] args) {
		SpringApplication.run(AutorizationServerOauth2Application.class, args);
	}

}
