#!/bin/bash

PWD=$(pwd)
PWD_RES=$PWD"/src/test/resources"

export MODEL_FILE=$PWD_RES"/test-model-tcp.cpsign"

mvn verify 
