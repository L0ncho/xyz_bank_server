#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT="${1:-$ROOT/platform/shared-security/src/main/resources/keystore.p12}"
PASSWORD="${BFF_KEYSTORE_PASSWORD:-changeit}"
ALIAS="xyzbank"

mkdir -p "$(dirname "$OUTPUT")"
rm -f "$OUTPUT"

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore "$OUTPUT" \
  -validity 3650 \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dname "CN=localhost,OU=XYZBank,O=Duoc,C=CL" \
  -noprompt

echo "Wrote $OUTPUT"
