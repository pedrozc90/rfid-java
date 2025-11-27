#!/usr/bin/env bash

# Chainway SDK
./mvnw deploy:deploy-file \
  -Dfile=rfid-chainway/lib/ReaderAPI20240822.jar \
  -DgroupId=com.rscja \
  -DartifactId=deviceapi \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local

# Zebra SDK
./mvnw deploy:deploy-file \
  -Dfile=rfid-zebra/lib/Symbol_RFID_API3.jar \
  -DgroupId=com.mot.rfid \
  -DartifactId=api3-sdk \
  -Dversion=2.0.0 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local

# Impinj SDK
./mvnw deploy:deploy-file \
  -Dfile=rfid-impinj/lib/OctaneSDKJava-5.0.0.0-jar-with-dependencies.jar \
  -DgroupId=com.impinj \
  -DartifactId=octane \
  -Dversion=5.0.0.0 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local
