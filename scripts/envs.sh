#!/bin/sh

SSL_KEYPASSWD=CHANGEME_PASSWD

# base of source tree; needs rewrite for distribution
base=/home/capveg/git/proj/flowvisor/flowvisor-nwo-java

SSL_KEYSTORE=$base/mySSLKeyStore

config=$base/default-config.xml

# Setup some environmental variables
classpath=$base/lib/openflow.jar:\
$base/lib/xmlrpc-client-3.1.3.jar:\
$base/lib/xmlrpc-common-3.1.3.jar:\
$base/lib/xmlrpc-server-3.1.3.jar:\
$base/lib/commons-logging-1.1.jar:\
$base/lib/ws-commons-util-1.0.2.jar:\
$base/dist/flowvisor.jar

# ssl options for the jvm

sslopts="-Djavax.net.ssl.keyStore=$SSL_KEYSTORE -Djavax.net.ssl.keyStorePassword=$SSL_KEYPASSWD"

# for ssl debugging options
sslopts="$sslopts -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl"




