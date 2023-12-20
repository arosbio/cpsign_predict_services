# syntax=docker/dockerfile:1
FROM ubuntu:20.04 AS war-builder
ENV TZ=Europe/Stockholm
ENV DEBUG='True'

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN /bin/bash -c "apt-get update && apt-get install openjdk-11-jdk-headless maven -y"

COPY . /app
RUN /bin/bash -c "cd /app && /usr/bin/mvn clean -DskipTests -DfinalName=root -P thin package && cd .."


# Base of the services
ARG PORT=8080

FROM jetty:11.0.18-jre17 AS base-service

# TODO: sett correct user

# Expose the chosen port
EXPOSE ${PORT}

# Create directory for where models should be placed (as default)
RUN mkdir /var/lib/jetty/models/



# ===========================================================
# CLF server
# ===========================================================
FROM base-service AS cpsign-cp-clf-server

# Copy the cp-classification war
COPY --from=war-builder /app/cp_classification/target/root.war /var/lib/jetty/webapps/ROOT.war

# ===========================================================
# REG server
# ===========================================================
FROM base-service AS cpsign-cp-reg-server

# Copy the cp-regression war
COPY --from=war-builder /app/cp_regression/target/root.war /var/lib/jetty/webapps/ROOT.war

# ===========================================================
# VAP server
# ===========================================================
FROM base-service AS cpsign-vap-clf-server

# Copy the cp-classification war
COPY --from=war-builder /app/vap_classification/target/root.war /var/lib/jetty/webapps/ROOT.war