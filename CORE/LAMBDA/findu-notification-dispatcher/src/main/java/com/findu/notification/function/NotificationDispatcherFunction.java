package com.findu.notification.function;

import com.findu.notification.model.DispatchResult;
import com.findu.notification.model.NotificationRequest;
import com.findu.notification.service.NotificationDispatchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring Cloud Function que se expone como AWS Lambda.
 * Entrada: NotificationRequest (sin notification_id)
 * Salida: DispatchResult (con notification_id generado)
 */
@Configuration
public class NotificationDispatcherFunction {

    @Bean
    public Function<NotificationRequest, DispatchResult> dispatchNotification(
            NotificationDispatchService dispatchService) {
        return dispatchService::dispatch;
    }
}
