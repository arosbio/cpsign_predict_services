#!/bin/bash

export MODEL_FILE=$(pwd)"/src/test/resources/test-model.cpsign"

#clean package 
mvn clean package jetty:run-war -DskipTests