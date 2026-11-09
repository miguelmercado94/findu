package com.findu.notification.processor.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resuelve la credencial de Firebase (service account) de forma flexible y degradable,
 * en el orden definido por el plan (D5):
 * <ol>
 *   <li>JSON inline por {@code findu.notification.firebase.credentials-json} (env {@code FIREBASE_CREDENTIALS_JSON}).</li>
 *   <li>Ruta a archivo por {@code findu.notification.firebase.credentials-path} (env {@code FIREBASE_CREDENTIALS_PATH}).</li>
 *   <li>Default local {@code secrets/firebase-service-account.json} relativo a la raíz del repo.</li>
 *   <li>Si nada aplica, devuelve {@link Optional#empty()} (Firebase deshabilitado, no se crea FirebaseApp).</li>
 * </ol>
 * NUNCA loguea el contenido de la credencial.
 */
public class FirebaseCredentialResolver {

    private static final Logger log = LoggerFactory.getLogger(FirebaseCredentialResolver.class);

    static final String DEFAULT_LOCAL_PATH = "secrets/firebase-service-account.json";

    private final String credentialsJson;
    private final String credentialsPath;

    public FirebaseCredentialResolver(String credentialsJson, String credentialsPath) {
        this.credentialsJson = credentialsJson;
        this.credentialsPath = credentialsPath;
    }

    /**
     * Resuelve las credenciales. Devuelve {@link Optional#empty()} cuando ninguna fuente aplica
     * o cuando la lectura falla (degradación: Firebase queda deshabilitado).
     */
    public Optional<GoogleCredentials> resolve() {
        // 1) JSON inline (env/propiedad), p.ej. Railway FIREBASE_CREDENTIALS_JSON
        if (credentialsJson != null && !credentialsJson.isBlank()) {
            try (InputStream in = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
                log.info("[FCM] Credencial Firebase resuelta desde JSON inline (FIREBASE_CREDENTIALS_JSON).");
                return Optional.of(GoogleCredentials.fromStream(in));
            } catch (Exception e) {
                log.warn("[FCM] No se pudo leer la credencial Firebase desde JSON inline: {}", e.getMessage());
                return Optional.empty();
            }
        }

        // 2) Ruta a archivo (env/propiedad)
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            Path path = Path.of(credentialsPath);
            if (Files.isRegularFile(path)) {
                try (InputStream in = new FileInputStream(path.toFile())) {
                    log.info("[FCM] Credencial Firebase resuelta desde ruta FIREBASE_CREDENTIALS_PATH.");
                    return Optional.of(GoogleCredentials.fromStream(in));
                } catch (Exception e) {
                    log.warn("[FCM] No se pudo leer la credencial Firebase desde la ruta configurada: {}", e.getMessage());
                    return Optional.empty();
                }
            }
            log.warn("[FCM] FIREBASE_CREDENTIALS_PATH configurada pero el archivo no existe.");
        }

        // 3) Default local secrets/firebase-service-account.json (relativo a la raíz del repo)
        Path defaultPath = Path.of(DEFAULT_LOCAL_PATH);
        if (Files.isRegularFile(defaultPath)) {
            try (InputStream in = new FileInputStream(defaultPath.toFile())) {
                log.info("[FCM] Credencial Firebase resuelta desde default local {}.", DEFAULT_LOCAL_PATH);
                return Optional.of(GoogleCredentials.fromStream(in));
            } catch (Exception e) {
                log.warn("[FCM] No se pudo leer la credencial Firebase desde el default local: {}", e.getMessage());
                return Optional.empty();
            }
        }

        // 4) Nada aplica -> deshabilitado
        log.warn("[FCM] Sin credencial Firebase (JSON inline, ruta ni default local). Push FCM DESHABILITADO (no-op).");
        return Optional.empty();
    }
}
