FROM jetty:11.0.14-jre17

# Copy the service application
COPY ROOT.war /var/lib/jetty/webapps/ROOT.war

# Copy the model to the default location
COPY test-model.cpsign /var/lib/jetty/model.jar
