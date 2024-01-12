# syntax=docker/dockerfile:1

ARG PORT=8080
# chose to use `thin` or `full` profile 
ARG SERVICE_TYPE=thin

FROM ubuntu:20.04 AS war-builder
ENV TZ=Europe/Stockholm
ARG SERVICE_TYPE

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN /bin/bash -c "apt-get update && apt-get install openjdk-11-jdk-headless maven -y"

COPY . /app
RUN /bin/bash -c "cd /app && /usr/bin/mvn clean -DskipTests -DfinalName=root -P ${SERVICE_TYPE} package && cd .."


# Base of the services
FROM jetty:11.0.18-jre17 AS base-service
ARG PORT

# Labels
LABEL org.opencontainers.image.source=https://github.com/arosbio/cpsign_predict_services
LABEL org.opencontainers.image.licenses=GPL-3.0-only

# Expose the chosen port
EXPOSE ${PORT}

# Create directory for where models should be placed (as default)
RUN mkdir /var/lib/jetty/models/



# ===========================================================
# CLF server
# ===========================================================
FROM base-service AS cpsign-cp-clf-server

LABEL org.opencontainers.image.description="REST server for conformal classification QSAR models built using CPSign"
# Copy the cp-classification war
COPY --from=war-builder /app/cp_classification/target/root.war /var/lib/jetty/webapps/ROOT.war

# ===========================================================
# REG server
# ===========================================================
FROM base-service AS cpsign-cp-reg-server

LABEL org.opencontainers.image.description="REST server for conformal regression QSAR models built using CPSign"
# Copy the cp-regression war
COPY --from=war-builder /app/cp_regression/target/root.war /var/lib/jetty/webapps/ROOT.war

# ===========================================================
# VAP server
# ===========================================================
FROM base-service AS cpsign-vap-clf-server

LABEL org.opencontainers.image.description="REST server for Venn-ABERS probabilistic classification QSAR models built using CPSign"
# Copy the cp-classification war
COPY --from=war-builder /app/vap_classification/target/root.war /var/lib/jetty/webapps/ROOT.war