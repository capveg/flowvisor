#!/bin/sh

base=/home/capveg/git/proj/flowvisor/flowvisor-nwo-java

if [ -f $base/scripts/envs.sh ]; then
    . $base/scripts/envs.sh
fi


if [ -f $SSL_KEYSTORE ] ; then
    echo "SSL Server Keystore already exists ($SSL_KEYSTORE): not regenerating"
else
    echo "Trying to generate SSL Server Key with passwd from scripts/envs.sh" >&2
    keytool -genkey -keystore $SSL_KEYSTORE -storepass $SSL_KEYPASSWD -keyalg RSA
fi

echo "Generating default FlowVisor config file to $config"
java -cp $classpath org.flowvisor.config.FVConfig $config

echo "You will need to create new slices/flowvisor using the root account"
