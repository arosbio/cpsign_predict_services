#!/bin/bash

mvn deploy:deploy-file \
    -Durl=file://$(pwd)/local_mvn_repo/ \
    -Dfile=lib/encryption-impl-1.0.0-SNAPSHOT.jar \
    -DgroupId=com.arosbio \
    -DartifactId=encryption-impl \
    -Dpackaging=jar \
    -Dversion=1.0.0
