#!/bin/bash
if [ "$1" == "-h" ]; then
  echo "usage: $0 <repository path> <cpsign jar path>"
  exit 0
fi

REPOSITORY=$1 #$(pwd)/classification_eclipse-project/repo/
JAR_FILE=$2

GROUP_ID=com.genettasoft
ARTIFACT_ID=cpsign
VERSION=$(unzip -p $JAR_FILE META-INF/MANIFEST.MF | grep 'Implementation-Version' | head -1 | cut -d ':' -f 2 | tr -d ' ' | tr -d '\n' | tr -d '\r' )

echo $VERSION
mvn deploy:deploy-file \
  -Durl=file://$REPOSITORY \
  -Dfile=$JAR_FILE \
  -DgroupId=$GROUP_ID -D artifactId=$ARTIFACT_ID -Dversion=$VERSION
