#!/usr/bin/env bash

./mvnw deploy:deploy-file \
  -Dfile=rfid-chainway/lib/ReaderAPI20240822.jar \
  -DgroupId=com.rscja \
  -DartifactId=deviceapi \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local
