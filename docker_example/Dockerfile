# Replace [SERVER-TYPE] with one of; cpsign-cp-clf-server, cpsign-cp-reg-server or cpsign-vap-clf-server
# Update the [TAG] based on your requirements.
# Replace [YOUR-MODEL.jar] to the model you wish to deploy (must match against the SERVER-TYPE)

FROM ghcr.io/arosbio/[SERVER-TYPE]:[TAG]

# Copy the model to the default location
COPY [YOUR-MODEL.jar] /var/lib/jetty/model.jar
