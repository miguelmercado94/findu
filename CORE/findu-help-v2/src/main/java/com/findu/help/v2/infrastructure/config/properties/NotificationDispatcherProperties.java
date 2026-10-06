package com.findu.help.v2.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "findu.notification.dispatcher")
public class NotificationDispatcherProperties {

    private String url = "http://localhost:9000";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 5000;
}
