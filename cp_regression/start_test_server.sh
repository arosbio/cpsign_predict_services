#!/bin/bash

PWD=$(pwd)
PWD_RES=$PWD"/src/test/resources"

export MODEL_FILE=$PWD_RES"/test-model.cpsign"

mvn clean package jetty:run-war -DskipTests