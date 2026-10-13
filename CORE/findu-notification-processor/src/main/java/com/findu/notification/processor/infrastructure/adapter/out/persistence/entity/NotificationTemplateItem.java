package com.findu.notification.processor.infrastructure.adapter.out.persistence.entity;

/**
 * Representa el ítem crudo de DynamoDB para la tabla {@code notification_templates}
 * (esquema versionado PK {@code id} + SK {@code sk}, diseño §3 / §9).
 *
 * <p>Esta clase centraliza los <b>nombres de atributo</b> del ítem DynamoDB. El
 * {@code NotificationTemplateItemMapper} es el único código que convierte entre
 * este contrato de atributos y el modelo de dominio {@code NotificationTemplate};
 * la capa application/domain nunca ve estos nombres.</p>
 */
public final class NotificationTemplateItem {

    private NotificationTemplateItem() {
    }

    // ---- Claves ----
    public static final String ID = "id";                       // PK: CODE#TIPO#LANG
    public static final String SK = "sk";                       // SK: v#<n> | v#ACTIVE

    // ---- Comunes (§3.1) ----
    public static final String CODIGO = "codigo";
    public static final String TIPO = "tipo";
    public static final String LANGUAGE = "language";
    public static final String BODY_TEMPLATE = "body_template";
    public static final String VARIABLES = "variables";         // L<S>
    public static final String VERSION = "version";             // N
    public static final String IS_ACTIVE = "is_active";         // BOOL
    public static final String AUDIENCIA = "audiencia";         // M
    public static final String CREATED_AT = "created_at";
    public static final String CREATED_BY = "created_by";
    public static final String UPDATED_AT = "updated_at";
    public static final String UPDATED_BY = "updated_by";

    // Puntero v#ACTIVE: número de versión activa (desnormalizado)
    public static final String ACTIVE_VERSION = "active_version"; // N

    // ---- Legacy / canal técnico ----
    public static final String CHANNEL = "channel";

    // ---- EMAIL / WHATSAPP ----
    public static final String SUBJECT = "subject";
    public static final String CONTENT_TYPE = "content_type";
    public static final String WA_TEMPLATE_NAME = "wa_template_name";

    // ---- EVENTO ----
    public static final String RABBIT_EXCHANGE = "rabbit_exchange";
    public static final String RABBIT_ROUTING_KEY = "rabbit_routing_key";
    public static final String RABBIT_QUEUE = "rabbit_queue";
    public static final String EVENT_TYPE = "event_type";

    // ---- NOTIFICACION ----
    public static final String TITLE = "title";
    public static final String ROUTING = "routing";            // M
    public static final String ROUTING_CONDICION = "condicion";
    public static final String ROUTING_CANAL_ACTIVO = "canal_si_activo";
    public static final String ROUTING_CANAL_INACTIVO = "canal_si_inactivo";

    // ---- ALERTA (AlertMeta, §3.2) ----
    public static final String ALERT_META = "alert_meta";       // M
    public static final String CATEGORIA = "categoria";
    public static final String PANTALLA_DESTINO = "pantalla_destino";
    public static final String TITULO = "titulo";
    public static final String CTA_LABEL = "cta_label";
    public static final String CTA_ACTION = "cta_action";
    public static final String CERRABLE = "cerrable";           // BOOL
    public static final String PRIORIDAD = "prioridad";         // N
    public static final String VIGENCIA_DESDE = "vigencia_desde";
    public static final String VIGENCIA_HASTA = "vigencia_hasta";
    public static final String CONDICION_APARICION = "condicion_aparicion";
    public static final String TIEMPO_DESAPARICION = "tiempo_desaparicion"; // N

    // ---- Valores de SK ----
    public static final String SK_PREFIX = "v#";
    public static final String SK_ACTIVE = "v#ACTIVE";

    /** Construye la PK lógica {@code CODE#TIPO#LANG}. */
    public static String buildId(String codigo, String tipo, String language) {
        return codigo.toUpperCase() + "#" + tipo.toUpperCase() + "#" + language.toLowerCase();
    }

    /** Construye el SK de una versión concreta ({@code v#<n>}). */
    public static String versionSk(int version) {
        return SK_PREFIX + version;
    }
}
