#!/bin/bash

# ===== Set environment variables =====

# DB Details.
export DB_URL="jdbc:oracle:thin:@10.16.3.43:1521:cbs"
export DB_USERNAME="CBS_APPS"
export DB_PASSWORD="Q0JTX0FQUFM=" # (CBS_APPS)

#export DB_URL="jdbc:oracle:thin:@172.20.27.122:1521/EMTELODC"
#export DB_PASSWORD="CBS_APPS"
#export DB_PASSWORD="Q0JTX0FQUFM=" # (CBS_APPS)

# export DB_URL="jdbc:oracle:thin:@172.26.64.235:1521/EMTELUAT"
# export DB_USERNAME="CRM_APPS_UAT"
# export DB_PASSWORD="Q1JNX0FwNjVfdSNUMzU=" # (CRM_Ap65_u#T35)

# Server port
export SERVER_PORT="8082"

# Log level.
export LOG_LEVEL="INFO"

# ===== Configuration =====
JAR_NAME="order-processor-1.0.0.jar"

# ===== Step 1: Validate JAR existence =====
if [[ ! -f "$JAR_NAME" ]]; then
  echo "ERROR: JAR file '$JAR_NAME' not found in current directory."
  exit 1
fi

# ===== Step 2: Start the Spring Boot application =====
echo "Starting application..."
java -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl -jar "$JAR_NAME"