#!/bin/bash
# =====================================================================
# FINDU — DDL del store de device tokens (DynamoDB / LocalStack)
# =====================================================================
# Tabla que guarda los device tokens de Firebase que la app móvil registra.
# El micro findu-notification-processor la consulta para enviar push:
#   - La app registra por 'username' (String).
#   - En el registro se resuelve una vez el recipient canónico
#     'PROVEEDOR_<proveedorId>' vía lookup interno a security y se guarda en
#     'canonical_recipient', para que el fallback de push lo consulte por GSI
#     en el hot path (1 Query, sin cross-service).
#
# Esquema:
#   - PK  device_token (S)          -> upsert idempotente por token
#   - GSI_username           (PK username)            -> tokens por usuario
#   - GSI_canonical_recipient (PK canonical_recipient) -> tokens por recipient canónico
#   Atributos adicionales: platform, created_at, updated_at.
#
# Ejecutar después de levantar docker compose:
#   bash CORE/findu-notification-processor/src/main/resources/db/dynamodb/create-device-tokens-table.sh
#
# Requiere: aws-cli configurado con endpoint http://localhost:4566
# =====================================================================

ENDPOINT="http://localhost:4566"
TABLE="findu_device_tokens"
REGION="us-east-1"

AWS="env AWS_ACCESS_KEY_ID=mock AWS_SECRET_ACCESS_KEY=mock aws --endpoint-url=$ENDPOINT --region=$REGION"

echo "=== Creando tabla $TABLE (PK device_token + GSI_username + GSI_canonical_recipient) ==="
$AWS dynamodb create-table \
  --table-name "$TABLE" \
  --attribute-definitions \
      AttributeName=device_token,AttributeType=S \
      AttributeName=username,AttributeType=S \
      AttributeName=canonical_recipient,AttributeType=S \
  --key-schema \
      AttributeName=device_token,KeyType=HASH \
  --global-secondary-indexes \
      'IndexName=GSI_username,KeySchema=[{AttributeName=username,KeyType=HASH}],Projection={ProjectionType=ALL}' \
      'IndexName=GSI_canonical_recipient,KeySchema=[{AttributeName=canonical_recipient,KeyType=HASH}],Projection={ProjectionType=ALL}' \
  --billing-mode PAY_PER_REQUEST \
  2>/dev/null || echo "Tabla $TABLE ya existe"

echo "=== Tabla $TABLE lista ==="
