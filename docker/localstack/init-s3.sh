#!/bin/bash
# Idempotente: crea el bucket de medios y activa versionado (D7). Se ejecuta en LocalStack ready.d.
set -e
BUCKET="${FINDU_MEDIA_BUCKET:-findu-media-assets-local}"
if awslocal s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; then
  echo "[init-s3] bucket $BUCKET ya existe"
else
  awslocal s3 mb "s3://$BUCKET"
  echo "[init-s3] bucket $BUCKET creado"
fi
awslocal s3api put-bucket-versioning --bucket "$BUCKET" --versioning-configuration Status=Enabled
echo "[init-s3] versionado Enabled en $BUCKET"
