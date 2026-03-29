package com.findu.security.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Carga variables desde el archivo {@code .env} en el directorio de trabajo (módulo del proyecto).
 * No sobrescribe variables ya definidas en el SO ni en {@code -D}.
 */
public final class DotEnvBootstrap {

    private DotEnvBootstrap() {
    }

    public static void load() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e -> {
            String key = e.getKey();
            if (System.getenv(key) != null) {
                return;
            }
            if (System.getProperty(key) != null) {
                return;
            }
            System.setProperty(key, e.getValue());
        });
    }
}
