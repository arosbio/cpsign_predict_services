FROM ghcr.io/arosbio/predictservice:latest


# Copy the model to the default location
COPY YOUR-MODEL.cpsign /var/lib/jetty/model.jar
# tell the service where to find the model
ENV MODEL_FILE=/var/lib/jetty/model.jar