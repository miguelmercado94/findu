package com.findu.security;

import com.findu.security.config.DotEnvBootstrap;
import com.findu.security.config.properties.DynamoDbProperties;
import com.findu.security.config.properties.RedisCacheProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({DynamoDbProperties.class, RedisCacheProperties.class})
public class FinduSpringSecurityApplication {

    public static void main(String[] args) {
        DotEnvBootstrap.load();
        SpringApplication.run(FinduSpringSecurityApplication.class, args);
    }
}
