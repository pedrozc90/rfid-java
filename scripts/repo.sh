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

# Acura SDK
./mvnw deploy:deploy-file \
  -Dfile=rfid-acura/lib/mercuryapi.jar \
  -DgroupId=com.thingmagic \
  -DartifactId=thingmagic \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local

./mvnw deploy:deploy-file \
  -Dfile=rfid-acura/lib/jSerialComm-2.2.0.jar \
  -DgroupId=com.fazecast \
  -DartifactId=fazecast \
  -Dversion=2.2.0 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local

./mvnw deploy:deploy-file \
  -Dfile=rfid-acura/lib/ltkjava-1.0.0.6.jar \
  -DgroupId=org.ltkjava \
  -DartifactId=ltkjava \
  -Dversion=1.0.0.6 \
  -Dpackaging=jar \
  -DgeneratePom=true \
  -Durl=file://$PWD/.repo \
  -DrepositoryId=local
