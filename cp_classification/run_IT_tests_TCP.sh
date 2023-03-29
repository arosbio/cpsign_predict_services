#!/bin/bash

export MODEL_FILE=$(pwd)"/src/test/resources/test-model-tcp.cpsign"

mvn verify 

unset MODEL_FILE