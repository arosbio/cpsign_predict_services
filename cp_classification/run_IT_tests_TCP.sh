#!/bin/bash

export MODEL_FILE=$(pwd)"/src/test/resources/test-model-tcp.cpsign"

mvn verify -DskipUnitTests

exit_status=$?

unset MODEL_FILE

if [ $exit_status -ne 0 ]; then
    echo "Error: IT Test for - TCP - CP Classification server failed."
    exit 1
fi