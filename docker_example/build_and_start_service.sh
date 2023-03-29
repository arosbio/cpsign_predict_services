#!/bin/bash

# === Build the regression WAR file ===

# Build _with_ Swagger UI and /draw GUI
mvn -f ../cp_regression clean package -DskipTests 
# Or, build _without_ those resources
# mvn -f ../cp_regression clean package -DskipTests -P thin

# Copy the WAR file to the current directory and rename to ROOT.war
cp ../cp_regression/target/cp_reg*.war ROOT.war


# === Copy the test-model to deploy (assuming you have one in this location) ===
cp ../cp_regression/src/test/resources/test-model.cpsign .

# Build the docker image, assuming you have a test-model (see Dockerfile)
docker build -t regression-server .

docker run -p 80:8080 regression-server:latest

