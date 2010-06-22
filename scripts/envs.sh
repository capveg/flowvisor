#!/bin/sh

SSL_KEYPASSWD=CHANGEME_PASSWD

#configbase=PREFIX

if [ -z $configbase ] ; then
    configbase=`dirname $0`/..
    jars=$configbase/lib
    dist=$configbase/dist
    SSL_KEYSTORE=$configbase/mySSLKeyStore
else
    jars=$configbase/libexec/flowvisor
    dist=$configbase/libexec/flowvisor
    SSL_KEYSTORE=$configbase/etc/flowvisor/mySSLKeyStore
fi


# Setup some environmental variables
classpath=$jars/openflow.jar:\
$jars/xmlrpc-client-3.1.3.jar:\
$jars/xmlrpc-common-3.1.3.jar:\
$jars/xmlrpc-server-3.1.3.jar:\
$jars/commons-logging-1.1.jar:\
$jars/ws-commons-util-1.0.2.jar:\
$jars/jsse.jar:\
$dist/flowvisor.jar

# ssl options for the jvm

sslopts="-Djavax.net.ssl.keyStore=$SSL_KEYSTORE -Djavax.net.ssl.keyStorePassword=$SSL_KEYPASSWD"

# for ssl debugging options
#sslopts="$sslopts -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl"




