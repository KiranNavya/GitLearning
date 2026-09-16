######################################################################
#                                                                    #
# Maintainer  : EIA                                                  #
# Date        : 31-07-2025                                           #
# Description : Dockerfile for orch-order-processor project          #
#                                                                    #
######################################################################

FROM dev-docker-registry.tecnotree.com/mirror/registry.docker.io/eclipse-temurin:21-jre-latest

# Install required dependencies
RUN apk add --no-cache \
    tini \
    bash \
    curl \
    openssl \
    wget \
    vim \
    iputils \
    mysql-client \
    tcpdump \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=Asia/Kolkata

# Create a dedicated non-root user and group for the application
RUN addgroup --system eiaGroup && adduser --system --ingroup eiaGroup eiaUser

# Create application and log directories, setting ownership and permissions
RUN mkdir -p /opt/app /opt/logs && \
    chown -R eiaUser:eiaGroup /opt/app /opt/logs && \
    chmod -R 755 /opt/app /opt/logs

# Set the user for running the application
USER eiaUser

# Set working directory
WORKDIR /opt/app

# Copy the application JAR and configuration file
COPY --chown=eiaUser:eiaGroup target/orch-order-processor.jar /opt/app/app.jar
COPY --chown=eiaUser:eiaGroup orch-order-processor/orch-business-config.yml /opt/app

# Expose application port
EXPOSE 8081

# Set the entrypoint to run the application
# Use -Djava.security.egd for faster startup with randomness
ENTRYPOINT ["tini", "--", "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/app/app.jar"]