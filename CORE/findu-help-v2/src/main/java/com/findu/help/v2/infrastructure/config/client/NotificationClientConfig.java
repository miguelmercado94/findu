package com.findu.help.v2.infrastructure.config.client;

import com.findu.help.v2.infrastructure.config.properties.NotificationDispatcherProperties;
import com.findu.help.v2.infrastructure.config.properties.NotificationProcessorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        NotificationDispatcherProperties.class,
        NotificationProcessorProperties.class
})
public class NotificationClientConfig {

    @Bean
    public RestClient dispatcherRestClient(NotificationDispatcherProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .build();
    }

    @Bean
    public RestClient processorRestClient(NotificationProcessorProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .build();
    }
}
