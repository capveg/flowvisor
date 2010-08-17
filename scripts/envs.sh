#!/bin/sh

SSL_KEYPASSWD=CHANGEME_PASSWD

#configbase=PREFIX

#install_root is for installing to a new directory, e.g., for chroot()

if [ -z $configbase ] ; then
    configbase=`dirname $0`/..
    install_dir=$configbase/dist
    jars=$configbase/lib
    config_dir=$configbase
    SSL_KEYSTORE=$configbase/mySSLKeyStore
else
    install_dir=$install_root$configbase/libexec/flowvisor
    jars=$install_dir
    config_dir=$install_root$configbase/etc/flowvisor
    SSL_KEYSTORE=$config_dir/mySSLKeyStore
fi


fv_defines="-Dorg.flowvisor.config_dir=$config_dir -Dorg.flowvisor.install_dir=$install_dir"

# Setup some environmental variables
classpath=$jars/openflow.jar:\
$jars/xmlrpc-client-3.1.3.jar:\
$jars/xmlrpc-common-3.1.3.jar:\
$jars/xmlrpc-server-3.1.3.jar:\
$jars/commons-logging-1.1.jar:\
$jars/ws-commons-util-1.0.2.jar:\
$jars/jsse.jar:\
$install_dir/flowvisor.jar

# ssl options for the jvm

sslopts="-Djavax.net.ssl.keyStore=$SSL_KEYSTORE -Djavax.net.ssl.keyStorePassword=$SSL_KEYPASSWD"

# for ssl debugging options
#sslopts="$sslopts -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol -Djavax.net.debug=ssl"




