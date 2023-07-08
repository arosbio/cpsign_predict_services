#!/bin/bash

# Export the model
export MODEL_FILE=$(pwd)"/src/test/resources/encryptedACPmodel.jar"
# Export the encryption key (here as plain text)
export ENCRYPTION_KEY=Kt6gqo8lds7mVyYr0gOdpg==

# Go into service_utils and install a new version (required during development and testing, when updated continuously)
cd ../service_utils
mvn clean install -DskipTests

# This is a classification model, need to go to cp_classification to start that one
cd ../cp_classification/

#clean package 
mvn clean package jetty:run-war -U -DskipTests -P encrypt -DENCRYPTION_KEY=Kt6gqo8lds7mVyYr0gOdpg==