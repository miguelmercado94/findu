#!/bin/bash
# =====================================================================
# FINDU — Seed de plantillas de notificación para DynamoDB (LocalStack)
# Ejecutar después de levantar docker compose:
#   bash CORE/findu-notification-processor/src/main/resources/db/dynamodb/seed-templates.sh
#
# Requiere: aws-cli configurado con endpoint http://localhost:4566
# =====================================================================

ENDPOINT="http://localhost:4566"
TABLE="notification_templates"
REGION="us-east-1"

AWS="env AWS_ACCESS_KEY_ID=mock AWS_SECRET_ACCESS_KEY=mock aws --endpoint-url=$ENDPOINT --region=$REGION"

echo "=== Creando tabla $TABLE ==="
$AWS dynamodb create-table \
  --table-name $TABLE \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || echo "Tabla ya existe"

echo "=== Creando tabla findu_notifications ==="
$AWS dynamodb create-table \
  --table-name findu_notifications \
  --attribute-definitions AttributeName=notification_id,AttributeType=S \
  --key-schema AttributeName=notification_id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || echo "Tabla findu_notifications ya existe"

echo "=== Insertando plantillas de ejemplo ==="

# ─── OFERTA RECIBIDA ───────────────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "OFERTA_RECIBIDA#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Nueva oferta para tu solicitud de {{servicio_nombre}}"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tienes una nueva oferta de <strong>{{proveedor_nombre}}</strong> por <strong>{{valor_propuesto}}</strong> para tu solicitud de {{servicio_nombre}}.</p><p>Tiempo estimado: {{tiempo_estimado}}</p><p><em>\"{{mensaje}}\"</em></p><a href=\"https://app.findu.co/solicitudes/{{solicitud_id}}/ofertas\">Ver ofertas</a></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "OFERTA_RECIBIDA#PUSH#es"},
  "channel": {"S": "PUSH"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": "Nueva oferta"},
  "body_template": {"S": "{{proveedor_nombre}} te envió una oferta de {{valor_propuesto}} para {{servicio_nombre}}"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "OFERTA_RECIBIDA#SMS#es"},
  "channel": {"S": "SMS"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": ""},
  "body_template": {"S": "FINDU: {{proveedor_nombre}} ofertó {{valor_propuesto}} para {{servicio_nombre}}. Revisa tu app."},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

# ─── OFERTA ACEPTADA (al proveedor) ───────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "OFERTA_ACEPTADA#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "¡Tu oferta fue aceptada!"},
  "body_template": {"S": "<html><body><h2>¡Felicidades {{user_name}}!</h2><p>Tu oferta para el servicio de <strong>{{servicio_nombre}}</strong> fue aceptada por el cliente.</p><p>Fecha programada: <strong>{{fecha_programada}}</strong></p><p>Dirección: {{direccion}}</p><a href=\"https://app.findu.co/mis-servicios\">Ver mis servicios</a></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "OFERTA_ACEPTADA#PUSH#es"},
  "channel": {"S": "PUSH"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": "Oferta aceptada"},
  "body_template": {"S": "¡Tu oferta para {{servicio_nombre}} fue aceptada! Fecha: {{fecha_programada}}"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

# ─── SOLICITUD CANCELADA ───────────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "SOLICITUD_CANCELADA#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Solicitud cancelada"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>La solicitud de <strong>{{servicio_nombre}}</strong> programada para {{fecha_programada}} ha sido cancelada.</p><p>Motivo: {{motivo}}</p></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "SOLICITUD_CANCELADA#PUSH#es"},
  "channel": {"S": "PUSH"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": "Solicitud cancelada"},
  "body_template": {"S": "La solicitud de {{servicio_nombre}} del {{fecha_programada}} fue cancelada."},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

# ─── SERVICIO PROGRAMADO ───────────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "SERVICIO_PROGRAMADO#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Servicio programado: {{servicio_nombre}}"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tu servicio de <strong>{{servicio_nombre}}</strong> está confirmado.</p><ul><li>Fecha: {{fecha_programada}}</li><li>Proveedor: {{proveedor_nombre}}</li><li>Dirección: {{direccion}}</li></ul><p>El proveedor llegará a la hora acordada.</p></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "SERVICIO_PROGRAMADO#PUSH#es"},
  "channel": {"S": "PUSH"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": "Servicio confirmado"},
  "body_template": {"S": "Tu {{servicio_nombre}} con {{proveedor_nombre}} está confirmado para el {{fecha_programada}}"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

# ─── CALIFICACIÓN RECIBIDA ─────────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "CALIFICACION_RECIBIDA#PUSH#es"},
  "channel": {"S": "PUSH"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": "Nueva calificación"},
  "body_template": {"S": "Recibiste {{puntaje}} estrellas por tu servicio de {{servicio_nombre}}"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "CALIFICACION_RECIBIDA#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Recibiste una calificación"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Recibiste una calificación de <strong>{{puntaje}} ⭐</strong> por tu servicio de {{servicio_nombre}}.</p><p><em>\"{{comentario}}\"</em></p></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

# ─── RECUPERACIÓN DE CONTRASEÑA ────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "RECUPERAR_PASSWORD#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "Código de recuperación de contraseña"},
  "body_template": {"S": "<html><body><h2>Hola {{user_name}}</h2><p>Tu código de recuperación es: <strong style=\"font-size:24px\">{{codigo}}</strong></p><p>Este código expira en {{expiracion_minutos}} minutos.</p><p>Si no solicitaste este cambio, ignora este correo.</p></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "RECUPERAR_PASSWORD#SMS#es"},
  "channel": {"S": "SMS"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": ""},
  "body_template": {"S": "FINDU: Tu código de recuperación es {{codigo}}. Expira en {{expiracion_minutos}} min."},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

# ─── BIENVENIDA ────────────────────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "BIENVENIDA#EMAIL#es"},
  "channel": {"S": "EMAIL"},
  "language": {"S": "es"},
  "content_type": {"S": "HTML"},
  "subject": {"S": "¡Bienvenido a FINDU!"},
  "body_template": {"S": "<html><body><h2>¡Hola {{user_name}}!</h2><p>Bienvenido a <strong>FINDU</strong>, tu plataforma de servicios profesionales bajo demanda.</p><p>Ya puedes buscar proveedores cerca de ti y solicitar servicios de:</p><ul><li>Salud y Bienestar</li><li>Hogar</li><li>Belleza</li><li>Cuidado Personal</li><li>Tecnología</li></ul><a href=\"https://app.findu.co\">Ir a FINDU</a></body></html>"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

echo "=== Seed completado. Templates insertados ==="

# ─── WHATSAPP TEMPLATES ────────────────────────────────────────────

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "OFERTA_RECIBIDA#WHATSAPP#es"},
  "channel": {"S": "WHATSAPP"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": ""},
  "body_template": {"S": "Hola {{user_name}}, tienes una nueva oferta de {{proveedor_nombre}} por {{valor_propuesto}} para {{servicio_nombre}}. Revisa tu app FINDU."},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

$AWS dynamodb put-item --table-name $TABLE --item '{
  "id": {"S": "SERVICIO_PROGRAMADO#WHATSAPP#es"},
  "channel": {"S": "WHATSAPP"},
  "language": {"S": "es"},
  "content_type": {"S": "TXT"},
  "subject": {"S": ""},
  "body_template": {"S": "Hola {{user_name}}, tu servicio de {{servicio_nombre}} con {{proveedor_nombre}} está confirmado para el {{fecha_programada}}. Dirección: {{direccion}}"},
  "version": {"N": "1"},
  "is_active": {"BOOL": true}
}'

echo "=== Seed completado ==="
$AWS dynamodb scan --table-name $TABLE --select COUNT
