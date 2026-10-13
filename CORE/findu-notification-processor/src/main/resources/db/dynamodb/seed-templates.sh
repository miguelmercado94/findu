#!/bin/bash
# =====================================================================
# FINDU — Seed del motor de plantillas de notificación (DynamoDB / LocalStack)
# =====================================================================
# Este micro es un MOTOR DE PLANTILLAS dinámico, gestionable por un rol admin.
# Cada plantilla soporta 6 tipos funcionales: SMS, EMAIL, WHATSAPP, EVENTO,
# NOTIFICACION y ALERTA (diseño DISENO_MODELO_PLANTILLAS_DINAMICAS.md).
#
# Esquema versionado (diseño §9):
#   - PK  id = CODE#TIPO#LANG (ej. VERIFICACION_OTP#SMS#es)
#   - SK  sk:
#       * v#<n>     -> cada versión inmutable
#       * v#ACTIVE  -> puntero desnormalizado (active_version + copia de la activa)
#   Cada plantilla seedea DOS ítems: v#1 (versión inmutable) y v#ACTIVE (puntero).
#
# GSIs (diseño §6): GSI_tipo (tipo), GSI_pantalla (pantalla_destino), GSI_codigo (codigo).
#
# Ejecutar después de levantar docker compose:
#   bash CORE/findu-notification-processor/src/main/resources/db/dynamodb/seed-templates.sh
#
# Requiere: aws-cli configurado con endpoint http://localhost:4566
# =====================================================================

ENDPOINT="http://localhost:4566"
TABLE="notification_templates"
REGION="us-east-1"

AWS="env AWS_ACCESS_KEY_ID=mock AWS_SECRET_ACCESS_KEY=mock aws --endpoint-url=$ENDPOINT --region=$REGION"

echo "=== Creando tabla $TABLE (PK id + SK sk, versionada) ==="
$AWS dynamodb create-table \
  --table-name "$TABLE" \
  --attribute-definitions \
      AttributeName=id,AttributeType=S \
      AttributeName=sk,AttributeType=S \
      AttributeName=tipo,AttributeType=S \
      AttributeName=pantalla_destino,AttributeType=S \
      AttributeName=codigo,AttributeType=S \
  --key-schema \
      AttributeName=id,KeyType=HASH \
      AttributeName=sk,KeyType=RANGE \
  --global-secondary-indexes \
      'IndexName=GSI_tipo,KeySchema=[{AttributeName=tipo,KeyType=HASH}],Projection={ProjectionType=ALL}' \
      'IndexName=GSI_pantalla,KeySchema=[{AttributeName=pantalla_destino,KeyType=HASH}],Projection={ProjectionType=ALL}' \
      'IndexName=GSI_codigo,KeySchema=[{AttributeName=codigo,KeyType=HASH}],Projection={ProjectionType=ALL}' \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || echo "Tabla ya existe"

echo "=== Creando tabla findu_notifications ==="
$AWS dynamodb create-table \
  --table-name findu_notifications \
  --attribute-definitions AttributeName=notification_id,AttributeType=S \
  --key-schema AttributeName=notification_id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || echo "Tabla findu_notifications ya existe"

echo "=== Insertando plantillas (v#1 + v#ACTIVE por plantilla) ==="

# ---------------------------------------------------------------------
# Helper: escribe un ítem como versión inmutable (v#1) y, con los mismos
# atributos de contenido, como puntero v#ACTIVE (añadiendo active_version).
# $1 = JSON del cuerpo del ítem SIN "id"/"sk"/"version"/"active_version"
#      (deben venir id/codigo/tipo/language + atributos de contenido).
# Los atributos id, version e is_active ya vienen en el JSON base.
# Usamos jq para derivar las dos variantes de sk.
# ---------------------------------------------------------------------
seed_template() {
  local base_json="$1"

  # Versión inmutable v#1
  local v1_item
  v1_item=$(echo "$base_json" | jq -c '. + {"sk": {"S": "v#1"}}')
  $AWS dynamodb put-item --table-name "$TABLE" --item "$v1_item"

  # Puntero v#ACTIVE (copia desnormalizada + active_version)
  local active_item
  active_item=$(echo "$base_json" | jq -c '. + {"sk": {"S": "v#ACTIVE"}, "active_version": {"N": "1"}}')
  $AWS dynamodb put-item --table-name "$TABLE" --item "$active_item"
}

# =====================================================================
# OTP — SMS (VERIFICACION_OTP#SMS#es)  [requerido para que OTP resuelva]
# =====================================================================
seed_template '{
  "id": {"S": "VERIFICACION_OTP#SMS#es"},
  "codigo": {"S": "VERIFICACION_OTP"},
  "tipo": {"S": "SMS"},
  "language": {"S": "es"},
  "channel": {"S": "SMS"},
  "content_type": {"S": "TXT"},
  "body_template": {"S": "FINDU: Tu código es {{otp_code}}. Expira en {{expiration_minutes}} min."},
  "variables": {"L": [{"S": "otp_code"}, {"S": "expiration_minutes"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# =====================================================================
# OTP — EMAIL (VERIFICACION_EMAIL#EMAIL#es)  [OTP email]
# =====================================================================
seed_template '{
  "id": {"S": "VERIFICACION_EMAIL#EMAIL#es"},
  "codigo": {"S": "VERIFICACION_EMAIL"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Tu código de verificación FINDU"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tu código de verificación es: <strong style=\"font-size:24px\">{{otp_code}}</strong></p><p>Expira en {{expiration_minutes}} minutos.</p></body></html>"},
  "variables": {"L": [{"S": "user_name"}, {"S": "otp_code"}, {"S": "expiration_minutes"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# =====================================================================
# EMAIL — OFERTA_RECIBIDA#EMAIL#es   (diseño §7 EMAIL)
# =====================================================================
seed_template '{
  "id": {"S": "OFERTA_RECIBIDA#EMAIL#es"},
  "codigo": {"S": "OFERTA_RECIBIDA"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Nueva oferta para {{servicio_nombre}}"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tienes una nueva oferta de <strong>{{proveedor_nombre}}</strong> por <strong>{{valor_propuesto}}</strong> para tu solicitud de {{servicio_nombre}}.</p></body></html>"},
  "variables": {"L": [{"S": "user_name"}, {"S": "proveedor_nombre"}, {"S": "valor_propuesto"}, {"S": "servicio_nombre"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# =====================================================================
# WHATSAPP — OFERTA_RECIBIDA#WHATSAPP#es   (diseño §7 WHATSAPP)
# =====================================================================
seed_template '{
  "id": {"S": "OFERTA_RECIBIDA#WHATSAPP#es"},
  "codigo": {"S": "OFERTA_RECIBIDA"},
  "tipo": {"S": "WHATSAPP"},
  "language": {"S": "es"},
  "channel": {"S": "WHATSAPP"},
  "content_type": {"S": "TXT"},
  "wa_template_name": {"S": "oferta_recibida_v1"},
  "body_template": {"S": "Hola {{user_name}}, tienes una oferta de {{proveedor_nombre}} por {{valor_propuesto}} para {{servicio_nombre}}."},
  "variables": {"L": [{"S": "user_name"}, {"S": "proveedor_nombre"}, {"S": "valor_propuesto"}, {"S": "servicio_nombre"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# =====================================================================
# EVENTO — PAGO_COMPLETADO#EVENTO#es   (diseño §7 EVENTO)
# =====================================================================
seed_template '{
  "id": {"S": "PAGO_COMPLETADO#EVENTO#es"},
  "codigo": {"S": "PAGO_COMPLETADO"},
  "tipo": {"S": "EVENTO"},
  "language": {"S": "es"},
  "channel": {"S": "EVENTO"},
  "rabbit_exchange": {"S": "findu.notifications"},
  "rabbit_routing_key": {"S": "findu.notifications.push"},
  "rabbit_queue": {"S": "findu.notifications.push"},
  "event_type": {"S": "PAYMENT_COMPLETED"},
  "body_template": {"S": "{\"notificationId\":\"{{notification_id}}\",\"templateCode\":\"PAGO_COMPLETADO\"}"},
  "variables": {"L": [{"S": "notification_id"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# =====================================================================
# NOTIFICACION — OFERTA_ACEPTADA#NOTIFICACION#es   (diseño §7 NOTIFICACION)
#   routing.condicion = isUserActivo -> WEBSOCKET si activo / PUSH si no
# =====================================================================
seed_template '{
  "id": {"S": "OFERTA_ACEPTADA#NOTIFICACION#es"},
  "codigo": {"S": "OFERTA_ACEPTADA"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Oferta aceptada"},
  "body_template": {"S": "¡Tu oferta para {{servicio_nombre}} fue aceptada! Fecha: {{fecha_programada}}"},
  "variables": {"L": [{"S": "servicio_nombre"}, {"S": "fecha_programada"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# =====================================================================
# ALERTA — VERIFICA_CORREO#ALERTA#es   (diseño §7 ALERTA, SEGURIDAD)
# =====================================================================
seed_template '{
  "id": {"S": "VERIFICA_CORREO#ALERTA#es"},
  "codigo": {"S": "VERIFICA_CORREO"},
  "tipo": {"S": "ALERTA"},
  "language": {"S": "es"},
  "channel": {"S": "ALERTA"},
  "pantalla_destino": {"S": "perfil_proveedor"},
  "body_template": {"S": "Confirma tu correo y biometría para activar todas las funciones."},
  "variables": {"L": []},
  "alert_meta": {"M": {
    "categoria": {"S": "SEGURIDAD"},
    "pantalla_destino": {"S": "perfil_proveedor"},
    "titulo": {"S": "Verifica tu correo"},
    "cta_label": {"S": "Verificar ahora"},
    "cta_action": {"S": "findu://perfil/verificacion"},
    "cerrable": {"BOOL": false},
    "prioridad": {"N": "1"},
    "condicion_aparicion": {"S": "emailOk == false || biometriaOk == false"},
    "tiempo_desaparicion": {"N": "0"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "SEGMENTO"}, "filtros": {"M": {"rol": {"S": "PROVEEDOR"}}}}}
}'

# =====================================================================
# ALERTA — PROMO_NAVIDAD#ALERTA#es   (diseño §7 ALERTA, INFO + vigencia)
# =====================================================================
seed_template '{
  "id": {"S": "PROMO_NAVIDAD#ALERTA#es"},
  "codigo": {"S": "PROMO_NAVIDAD"},
  "tipo": {"S": "ALERTA"},
  "language": {"S": "es"},
  "channel": {"S": "ALERTA"},
  "pantalla_destino": {"S": "home"},
  "body_template": {"S": "20% en servicios del hogar hasta fin de mes."},
  "variables": {"L": []},
  "alert_meta": {"M": {
    "categoria": {"S": "INFO"},
    "pantalla_destino": {"S": "home"},
    "titulo": {"S": "Promo de diciembre"},
    "cta_label": {"S": "Conocer más"},
    "cta_action": {"S": "findu://promos/navidad"},
    "cerrable": {"BOOL": true},
    "prioridad": {"N": "2"},
    "vigencia_desde": {"S": "2026-12-01T00:00:00Z"},
    "vigencia_hasta": {"S": "2026-12-31T23:59:59Z"},
    "tiempo_desaparicion": {"N": "0"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# =====================================================================
# Plantillas funcionales legacy convertidas al nuevo formato CODE#TIPO#LANG
# (PUSH -> NOTIFICACION; EMAIL/SMS/WHATSAPP 1:1). Se conservan las vigentes.
# =====================================================================

# --- OFERTA_RECIBIDA push -> NOTIFICACION ---
seed_template '{
  "id": {"S": "OFERTA_RECIBIDA#NOTIFICACION#es"},
  "codigo": {"S": "OFERTA_RECIBIDA"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Nueva oferta"},
  "body_template": {"S": "{{proveedor_nombre}} te envió una oferta de {{valor_propuesto}} para {{servicio_nombre}}"},
  "variables": {"L": [{"S": "proveedor_nombre"}, {"S": "valor_propuesto"}, {"S": "servicio_nombre"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- OFERTA_RECIBIDA sms ---
seed_template '{
  "id": {"S": "OFERTA_RECIBIDA#SMS#es"},
  "codigo": {"S": "OFERTA_RECIBIDA"},
  "tipo": {"S": "SMS"},
  "language": {"S": "es"},
  "channel": {"S": "SMS"},
  "content_type": {"S": "TXT"},
  "body_template": {"S": "FINDU: {{proveedor_nombre}} ofertó {{valor_propuesto}} para {{servicio_nombre}}. Revisa tu app."},
  "variables": {"L": [{"S": "proveedor_nombre"}, {"S": "valor_propuesto"}, {"S": "servicio_nombre"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- OFERTA_ACEPTADA email ---
seed_template '{
  "id": {"S": "OFERTA_ACEPTADA#EMAIL#es"},
  "codigo": {"S": "OFERTA_ACEPTADA"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "¡Tu oferta fue aceptada!"},
  "body_template": {"S": "<html><body><h2>¡Felicidades {{user_name}}!</h2><p>Tu oferta para el servicio de <strong>{{servicio_nombre}}</strong> fue aceptada por el cliente.</p><p>Fecha programada: <strong>{{fecha_programada}}</strong></p></body></html>"},
  "variables": {"L": [{"S": "user_name"}, {"S": "servicio_nombre"}, {"S": "fecha_programada"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- SERVICIO_PROGRAMADO email ---
seed_template '{
  "id": {"S": "SERVICIO_PROGRAMADO#EMAIL#es"},
  "codigo": {"S": "SERVICIO_PROGRAMADO"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Servicio programado: {{servicio_nombre}}"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tu servicio de <strong>{{servicio_nombre}}</strong> está confirmado.</p><ul><li>Fecha: {{fecha_programada}}</li><li>Proveedor: {{proveedor_nombre}}</li></ul></body></html>"},
  "variables": {"L": [{"S": "user_name"}, {"S": "servicio_nombre"}, {"S": "fecha_programada"}, {"S": "proveedor_nombre"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- SERVICIO_PROGRAMADO whatsapp ---
seed_template '{
  "id": {"S": "SERVICIO_PROGRAMADO#WHATSAPP#es"},
  "codigo": {"S": "SERVICIO_PROGRAMADO"},
  "tipo": {"S": "WHATSAPP"},
  "language": {"S": "es"},
  "channel": {"S": "WHATSAPP"},
  "content_type": {"S": "TXT"},
  "body_template": {"S": "Hola {{user_name}}, tu servicio de {{servicio_nombre}} con {{proveedor_nombre}} está confirmado para el {{fecha_programada}}."},
  "variables": {"L": [{"S": "user_name"}, {"S": "servicio_nombre"}, {"S": "proveedor_nombre"}, {"S": "fecha_programada"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- RECUPERAR_PASSWORD email ---
seed_template '{
  "id": {"S": "RECUPERAR_PASSWORD#EMAIL#es"},
  "codigo": {"S": "RECUPERAR_PASSWORD"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Código de recuperación de contraseña"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tu código de recuperación es: <strong style=\"font-size:24px\">{{codigo}}</strong></p><p>Este código expira en {{expiracion_minutos}} minutos.</p></body></html>"},
  "variables": {"L": [{"S": "user_name"}, {"S": "codigo"}, {"S": "expiracion_minutos"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- RECUPERAR_PASSWORD sms ---
seed_template '{
  "id": {"S": "RECUPERAR_PASSWORD#SMS#es"},
  "codigo": {"S": "RECUPERAR_PASSWORD"},
  "tipo": {"S": "SMS"},
  "language": {"S": "es"},
  "channel": {"S": "SMS"},
  "content_type": {"S": "TXT"},
  "body_template": {"S": "FINDU: Tu código de recuperación es {{codigo}}. Expira en {{expiracion_minutos}} min."},
  "variables": {"L": [{"S": "codigo"}, {"S": "expiracion_minutos"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- BIENVENIDA email ---
seed_template '{
  "id": {"S": "BIENVENIDA#EMAIL#es"},
  "codigo": {"S": "BIENVENIDA"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "¡Bienvenido a FINDU!"},
  "body_template": {"S": "<html><body><h2>¡Hola {{user_name}}!</h2><p>Bienvenido a <strong>FINDU</strong>, tu plataforma de servicios profesionales bajo demanda.</p><a href=\"https://app.findu.co\">Ir a FINDU</a></body></html>"},
  "variables": {"L": [{"S": "user_name"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# =====================================================================
# ACTIVACIÓN DE PROVEEDOR — plantillas nuevas (diseño §7)
#   2 NOTIFICACION (PROVEEDOR_ACTIVADO / PROVEEDOR_RECHAZADO) con routing
#     condicion=isUserActivo -> WEBSOCKET si activo / PUSH si inactivo.
#   2 ALERTA (PERFIL_CORREO_PENDIENTE / PERFIL_BIOMETRIA_PENDIENTE) con
#     categoria INFO y todos los campos de UI bajo alert_meta (NO top-level
#     title en ALERTA). pantalla_destino=perfil_proveedor; tiempo_desaparicion=0
#     (sin auto-ocultado; visibilidad gobernada por condicion_aparicion).
# =====================================================================

# --- NOTIFICACION — PROVEEDOR_ACTIVADO (soporte aprobó la cuenta) ---
seed_template '{
  "id": {"S": "PROVEEDOR_ACTIVADO#NOTIFICACION#es"},
  "codigo": {"S": "PROVEEDOR_ACTIVADO"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Tu cuenta fue activada"},
  "body_template": {"S": "¡Felicidades {{provider_nombre}}! Tu cuenta de proveedor fue activada. Ya puedes recibir solicitudes."},
  "variables": {"L": [{"S": "provider_nombre"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- NOTIFICACION — PROVEEDOR_RECHAZADO (soporte rechazó la cuenta) ---
seed_template '{
  "id": {"S": "PROVEEDOR_RECHAZADO#NOTIFICACION#es"},
  "codigo": {"S": "PROVEEDOR_RECHAZADO"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "No pudimos activar tu cuenta"},
  "body_template": {"S": "Hola {{provider_nombre}}, no pudimos activar tu cuenta. Motivo: {{motivo}}. Corrige los datos y vuelve a intentarlo."},
  "variables": {"L": [{"S": "provider_nombre"}, {"S": "motivo"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- ALERTA — PERFIL_CORREO_PENDIENTE (INFO, perfil_proveedor, sin CTA) ---
seed_template '{
  "id": {"S": "PERFIL_CORREO_PENDIENTE#ALERTA#es"},
  "codigo": {"S": "PERFIL_CORREO_PENDIENTE"},
  "tipo": {"S": "ALERTA"},
  "language": {"S": "es"},
  "channel": {"S": "ALERTA"},
  "pantalla_destino": {"S": "perfil_proveedor"},
  "body_template": {"S": "Confirma tu correo para avanzar en la activación de tu cuenta."},
  "variables": {"L": []},
  "alert_meta": {"M": {
    "categoria": {"S": "INFO"},
    "pantalla_destino": {"S": "perfil_proveedor"},
    "titulo": {"S": "Confirma tu correo"},
    "cerrable": {"BOOL": false},
    "prioridad": {"N": "1"},
    "condicion_aparicion": {"S": "emailOk == false"},
    "tiempo_desaparicion": {"N": "0"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "SEGMENTO"}, "filtros": {"M": {"rol": {"S": "PROVEEDOR"}}}}}
}'

# --- ALERTA — PERFIL_BIOMETRIA_PENDIENTE (INFO, perfil_proveedor, con CTA) ---
seed_template '{
  "id": {"S": "PERFIL_BIOMETRIA_PENDIENTE#ALERTA#es"},
  "codigo": {"S": "PERFIL_BIOMETRIA_PENDIENTE"},
  "tipo": {"S": "ALERTA"},
  "language": {"S": "es"},
  "channel": {"S": "ALERTA"},
  "pantalla_destino": {"S": "perfil_proveedor"},
  "body_template": {"S": "Completa tu validación biométrica para activar tu cuenta."},
  "variables": {"L": []},
  "alert_meta": {"M": {
    "categoria": {"S": "INFO"},
    "pantalla_destino": {"S": "perfil_proveedor"},
    "titulo": {"S": "Completa tu validación biométrica"},
    "cta_label": {"S": "Validar ahora"},
    "cta_action": {"S": "findu://perfil/verificacion-biometrica"},
    "cerrable": {"BOOL": false},
    "prioridad": {"N": "2"},
    "condicion_aparicion": {"S": "biometriaOk == false"},
    "tiempo_desaparicion": {"N": "0"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "SEGMENTO"}, "filtros": {"M": {"rol": {"S": "PROVEEDOR"}}}}}
}'

# =====================================================================
# Plantillas legacy faltantes (diseño §7 "plantillas que usamos actualmente")
#   Seguridad (com.findu.security.util.NotificationTemplates):
#     CODIGO_VERIFICACION (EMAIL), PASSWORD_CAMBIADA (EMAIL).
#     (VERIFICACION_OTP, VERIFICACION_EMAIL y RECUPERAR_PASSWORD ya están arriba.)
#   Core (com.findu.core.domain.enums.NotificationTemplates), despachadas por el
#   observer con "PUSH" -> tipo NOTIFICACION; BIENVENIDA_* por EMAIL:
#     BIENVENIDA_CLIENTE, BIENVENIDA_PROVEEDOR, SOLICITUD_CREADA, SOLICITUD_CANCELADA,
#     SOLICITUD_ACTUALIZADA, CALIFICACION_RECIBIDA.
#     (OFERTA_RECIBIDA, OFERTA_ACEPTADA y SERVICIO_PROGRAMADO ya están arriba.)
# =====================================================================

# --- CODIGO_VERIFICACION email (seguridad) ---
seed_template '{
  "id": {"S": "CODIGO_VERIFICACION#EMAIL#es"},
  "codigo": {"S": "CODIGO_VERIFICACION"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Tu código de verificación FINDU"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tu código de verificación es: <strong style=\"font-size:24px\">{{codigo}}</strong></p><p>Expira en {{expiracion_minutos}} minutos.</p></body></html>"},
  "variables": {"L": [{"S": "user_name"}, {"S": "codigo"}, {"S": "expiracion_minutos"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- PASSWORD_CAMBIADA email (seguridad) ---
seed_template '{
  "id": {"S": "PASSWORD_CAMBIADA#EMAIL#es"},
  "codigo": {"S": "PASSWORD_CAMBIADA"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Tu contraseña fue actualizada"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Te confirmamos que tu contraseña de FINDU fue actualizada correctamente.</p><p>Si no fuiste tú, contacta a soporte de inmediato.</p></body></html>"},
  "variables": {"L": [{"S": "user_name"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- BIENVENIDA_CLIENTE email (core) ---
seed_template '{
  "id": {"S": "BIENVENIDA_CLIENTE#EMAIL#es"},
  "codigo": {"S": "BIENVENIDA_CLIENTE"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "¡Bienvenido a FINDU!"},
  "body_template": {"S": "<html><body><h2>¡Hola {{user_name}}!</h2><p>Bienvenido a <strong>FINDU</strong>. Ya puedes solicitar servicios profesionales a tu medida.</p><a href=\"https://app.findu.co\">Ir a FINDU</a></body></html>"},
  "variables": {"L": [{"S": "user_name"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- BIENVENIDA_PROVEEDOR email (core) ---
seed_template '{
  "id": {"S": "BIENVENIDA_PROVEEDOR#EMAIL#es"},
  "codigo": {"S": "BIENVENIDA_PROVEEDOR"},
  "tipo": {"S": "EMAIL"},
  "language": {"S": "es"},
  "channel": {"S": "EMAIL"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "¡Bienvenido a FINDU, proveedor!"},
  "body_template": {"S": "<html><body><h2>¡Hola {{user_name}}!</h2><p>Bienvenido a <strong>FINDU</strong> como proveedor. Completa tus validaciones para empezar a recibir solicitudes.</p><a href=\"https://app.findu.co\">Ir a FINDU</a></body></html>"},
  "variables": {"L": [{"S": "user_name"}]},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "USUARIO"}, "userIds": {"L": []}}}
}'

# --- SOLICITUD_CREADA push -> NOTIFICACION (core, a proveedores elegibles) ---
seed_template '{
  "id": {"S": "SOLICITUD_CREADA#NOTIFICACION#es"},
  "codigo": {"S": "SOLICITUD_CREADA"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Nueva solicitud disponible"},
  "body_template": {"S": "Hay una nueva solicitud: {{detalles}}. Presupuesto máximo: {{presupuestoMaximo}}."},
  "variables": {"L": [{"S": "solicitudId"}, {"S": "detalles"}, {"S": "presupuestoMaximo"}, {"S": "esPresupuestoEstricto"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- SOLICITUD_CANCELADA push -> NOTIFICACION (core) ---
seed_template '{
  "id": {"S": "SOLICITUD_CANCELADA#NOTIFICACION#es"},
  "codigo": {"S": "SOLICITUD_CANCELADA"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Solicitud cancelada"},
  "body_template": {"S": "La solicitud {{solicitudId}} fue cancelada. Motivo: {{motivo}}."},
  "variables": {"L": [{"S": "solicitudId"}, {"S": "motivo"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- SOLICITUD_ACTUALIZADA push -> NOTIFICACION (core) ---
seed_template '{
  "id": {"S": "SOLICITUD_ACTUALIZADA#NOTIFICACION#es"},
  "codigo": {"S": "SOLICITUD_ACTUALIZADA"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Solicitud actualizada"},
  "body_template": {"S": "La solicitud {{solicitudId}} fue actualizada: {{detalles}}. Fecha: {{fechaProgramada}}."},
  "variables": {"L": [{"S": "solicitudId"}, {"S": "detalles"}, {"S": "presupuestoMaximo"}, {"S": "esPresupuestoEstricto"}, {"S": "direccionId"}, {"S": "fechaProgramada"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

# --- CALIFICACION_RECIBIDA push -> NOTIFICACION (core) ---
seed_template '{
  "id": {"S": "CALIFICACION_RECIBIDA#NOTIFICACION#es"},
  "codigo": {"S": "CALIFICACION_RECIBIDA"},
  "tipo": {"S": "NOTIFICACION"},
  "language": {"S": "es"},
  "channel": {"S": "PUSH"},
  "title": {"S": "Nueva calificación"},
  "body_template": {"S": "Recibiste una nueva calificación de {{calificacion}} estrellas por tu servicio."},
  "variables": {"L": [{"S": "calificacion"}]},
  "routing": {"M": {
    "condicion": {"S": "isUserActivo"},
    "canal_si_activo": {"S": "WEBSOCKET"},
    "canal_si_inactivo": {"S": "PUSH"}
  }},
  "version": {"N": "1"},
  "is_active": {"BOOL": true},
  "audiencia": {"M": {"modo": {"S": "TODOS"}}}
}'

echo "=== Seed completado ==="
$AWS dynamodb scan --table-name "$TABLE" --select COUNT
