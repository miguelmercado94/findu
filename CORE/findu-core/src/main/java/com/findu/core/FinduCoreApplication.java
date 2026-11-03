package com.findu.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinduCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinduCoreApplication.class, args);
    }
}
