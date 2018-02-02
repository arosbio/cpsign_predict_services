#!/bin/bash

oc delete all -l app=cpsign-classification
oc delete all -l app=cpsign-classification-20

sleep 2

oc process -f classification_build.yaml -p GIT_SOURCESECRET=deploymentkey | oc create -f -

RESULT=`curl --data "grant_type=password&client_id=modelingweb&username=test&password=test" http://keycloak.130.238.55.60.xip.io/auth/realms/toxhq/protocol/openid-connect/token`
TOKEN=`echo $RESULT | sed 's/.*access_token":"//g' | sed 's/".*//g'`

oc process -f classification_deploy.yaml \
    -p JAR_URL=http://modelingweb.130.238.55.60.xip.io/api/v1/models/20 \
    -p LICENSE_URL=http://modelingweb.130.238.55.60.xip.io/api/v1/licenses/1 \
    -p TOKEN=$TOKEN \
    -p MODEL_ID=20 | oc create -f -

exit 0



oc delete all -l app=cpsign-regression
oc delete all -l app=cpsign-regression-20

sleep 2

oc process -f regression_build.yaml -p GIT_SOURCESECRET=deploymentkey | oc create -f -

RESULT=`curl --data "grant_type=password&client_id=modelingweb&username=test&password=test" http://keycloak.130.238.55.60.xip.io/auth/realms/toxhq/protocol/openid-connect/token`
TOKEN=`echo $RESULT | sed 's/.*access_token":"//g' | sed 's/".*//g'`

oc process -f classification_deploy.yaml \
    -p JAR_URL=http://modelingweb.130.238.55.60.xip.io/api/v1/models/20 \
    -p LICENSE_URL=http://modelingweb.130.238.55.60.xip.io/api/v1/licenses/1 \
    -p TOKEN=$TOKEN \
    -p MODEL_ID=20 \
    -p MODEL_TYPE=regression \
    | oc create -f -
