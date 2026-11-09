package com.findu.notification.processor.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseCredentialResolverTest {

    /** Construye un JSON de service account válido (con private_key PEM real) para achambear-5fae3. */
    private static String fakeServiceAccountJson() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        byte[] pkcs8 = gen.generateKeyPair().getPrivate().getEncoded();
        // Base64 PEM con saltos de línea reales (separador = '\n' real).
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(pkcs8);
        String pem = "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
        // Para embeberlo en el string JSON, escapamos los saltos de línea a \n (literal).
        String pemEscaped = pem.replace("\n", "\\n");
        return "{\n"
                + "  \"type\": \"service_account\",\n"
                + "  \"project_id\": \"achambear-5fae3\",\n"
                + "  \"private_key_id\": \"test-key-id\",\n"
                + "  \"private_key\": \"" + pemEscaped + "\",\n"
                + "  \"client_email\": \"test@achambear-5fae3.iam.gserviceaccount.com\",\n"
                + "  \"client_id\": \"1234567890\",\n"
                + "  \"token_uri\": \"https://oauth2.googleapis.com/token\"\n"
                + "}";
    }

    @Test
    @DisplayName("JSON inline válido -> resuelve credencial")
    void resolve_jsonInline_present() throws Exception {
        FirebaseCredentialResolver resolver =
                new FirebaseCredentialResolver(fakeServiceAccountJson(), "");

        Optional<GoogleCredentials> result = resolver.resolve();

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Ruta a archivo existente -> resuelve credencial")
    void resolve_pathExisting_present(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("firebase-service-account.json");
        Files.writeString(file, fakeServiceAccountJson());

        FirebaseCredentialResolver resolver =
                new FirebaseCredentialResolver("", file.toString());

        Optional<GoogleCredentials> result = resolver.resolve();

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Sin json ni ruta ni default -> deshabilitado (empty)")
    void resolve_absent_disabled() {
        FirebaseCredentialResolver resolver =
                new FirebaseCredentialResolver("", "/ruta/que/no/existe/firebase.json");

        Optional<GoogleCredentials> result = resolver.resolve();

        assertThat(result).isEmpty();
    }
}
