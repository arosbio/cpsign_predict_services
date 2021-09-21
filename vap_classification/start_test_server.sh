#!/bin/bash

PWD=$(pwd)
PWD_RES=$PWD"/src/test/resources/resources"

export MODEL_FILE=$PWD_RES"/test-model.cpsign"
export LICENSE_FILE=$PWD_RES"/cpsign.license"
mvn clean package jetty:run-war -DskipTests