package com.findu.notification.processor.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * Inicialización del Firebase Admin SDK, flexible y degradable.
 *
 * <p>La credencial se resuelve con {@link FirebaseCredentialResolver} (JSON inline -> ruta ->
 * default local {@code secrets/firebase-service-account.json}). Si ninguna fuente aplica, NO se
 * crea {@link FirebaseApp} ni el bean {@link FirebaseMessaging}: el envío push degrada a no-op.</p>
 *
 * <p>Proyecto Firebase: {@code achambear-5fae3}. NUNCA se loguea el contenido de la credencial.</p>
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${findu.notification.firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${findu.notification.firebase.credentials-path:}")
    private String credentialsPath;

    /**
     * Expone {@link FirebaseMessaging} SOLO si hay credencial resoluble; devuelve {@code null}
     * (bean ausente de funcionalidad) cuando Firebase está deshabilitado, para que el sender
     * degrade a no-op sin romper el arranque.
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        Optional<GoogleCredentials> credentials =
                new FirebaseCredentialResolver(credentialsJson, credentialsPath).resolve();

        if (credentials.isEmpty()) {
            log.warn("[FCM] Firebase Admin SDK NO inicializado (sin credencial). Push degradado a no-op.");
            return null;
        }

        try {
            FirebaseApp app;
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials.get())
                        .build();
                app = FirebaseApp.initializeApp(options);
                log.info("[FCM] Firebase Admin SDK inicializado correctamente.");
            } else {
                app = FirebaseApp.getInstance();
                log.info("[FCM] Firebase Admin SDK ya estaba inicializado; se reutiliza la instancia.");
            }
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.warn("[FCM] Error al inicializar Firebase Admin SDK: {}. Push degradado a no-op.", e.getMessage());
            return null;
        }
    }
}
