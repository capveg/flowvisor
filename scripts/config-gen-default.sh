#!/bin/sh

#base=PREFIX

if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
else
    envs=$install_root$base/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

if [ "X$1" = "X" ] ; then
    echo "$0 newconfig.xml [SSLinfo]" >&2
    echo "e.g., $0 /tmp/new-config.xml \"cn=Mark Jones, ou=JavaSoft, o=Sun, c=US\"" >&2
    exit 1
fi


if [ -f $SSL_KEYSTORE ] ; then
    echo "SSL Server Keystore already exists ($SSL_KEYSTORE): not regenerating"
else
    echo "Trying to generate SSL Server Key with passwd from scripts/envs.sh" >&2
    if [ "X$2" != "X" ] ; then
        dname="-dname $2"
    fi
    keytool -genkey -keystore $SSL_KEYSTORE -storepass $SSL_KEYPASSWD -keyalg RSA $dname
fi

echo "Generating default FlowVisor config file to $1"
java -cp $classpath org.flowvisor.config.FVConfig $1

echo "You will need to create new slices using the root account"
