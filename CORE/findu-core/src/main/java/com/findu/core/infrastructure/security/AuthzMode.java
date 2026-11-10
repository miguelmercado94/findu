package com.findu.core.infrastructure.security;

/** Modo de la autorizacion por propietario: OFF no hace nada, LOG solo registra, ENFORCE bloquea. */
public enum AuthzMode {
    OFF, LOG, ENFORCE
}
