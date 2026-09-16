#!/bin/bash
# ==== CONFIGURATION ====
export ORCH_DB_URL="jdbc:oracle:thin:@10.16.3.43:1521:cbs"
export ORCH_DB_USERNAME="CBS_APPS"
export ORCH_DB_PASSWORD="CBS_APPS"
export LOG_LEVEL="INFO"

JAR_NAME="orch-order-processor.jar"
#TRUSTSTORE="sso_truststore.jks"
#TRUSTSTORE_PASSWORD="changeit"   # replace with actual password if different

# ==== VALIDATE ====
if [ ! -f "$JAR_NAME" ]; then
  echo "❌ JAR file not found: $JAR_NAME"
  exit 1
fi

#if [ ! -f "$TRUSTSTORE" ]; then
#  echo "❌ Truststore file not found: $TRUSTSTORE"
#  exit 1
#fi

echo "Using DB URL: $ORCH_DB_URL"
echo "Using DB Username: $ORCH_DB_USERNAME"

# ==== START APPLICATION ====
echo "🚀 Starting $JAR_NAME with external config $CONFIG_FILE"
#java \
#  -Djavax.net.ssl.trustStore="$TRUSTSTORE" \
#  -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWORD" \
#  -jar "$JAR_NAME"

java -DLOG_LEVEL="${LOG_LEVEL}" -jar "$JAR_NAME"

exit 0