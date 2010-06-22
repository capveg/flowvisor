#!/bin/sh

# sadly hacked together install script; really only used to bootstrap rpm or debs
# apologies in advance to anyone who has to debug this

prefix=/usr/local
fvuser=flowvisor
fvgroup=flowvisor
install=install
base=`dirname $0`/..
scriptd=$base/scripts
libs=$base/lib
dist=$base/dist
config=flowvisor-config.xml
#verbose=-v

echo "Using source dir: $base"
read -p "Installation prefix ($prefix): " prefix
read -p "FlowVisor User ($fvuser): " fvuser
read -p "FlowVisor Group ($fvgroup): " fvgroup


echo Installing FlowVisor into $prefix as user/group ${fvuser}:${fvgroup}

bin_SCRIPTS="\
    fvctl \
    "

sbin_SCRIPTS="\
    config-dump \
    config-gen-default \
    config-query \
    flowvisor \
    "

LIBS="\
    commons-logging-1.1.jar \
    openflow.jar \
    jsse.jar \
    ws-commons-util-1.0.2.jar \
    xmlrpc-client-3.1.3.jar \
    xmlrpc-common-3.1.3.jar \
    xmlrpc-server-3.1.3.jar \
    "
owd=`pwd`
cd $scriptd

for script in $bin_SCRIPTS $sbin_SCRIPTS envs ; do 
    echo Updating $script.sh to $script
    sed -e "s!#base=PREFIX!base=$prefix!" -e "s!#configbase=PREFIX!configbase=$prefix!"< $script.sh > $script
done

echo Creating directories
for d in bin sbin libexec/flowvisor etc/flowvisor ; do 
    echo Creating $prefix/$d
    $install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -d $prefix/$d
done

echo Installing scripts
$install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -D $bin_SCRIPTS $prefix/bin
$install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -D $sbin_SCRIPTS $prefix/sbin


echo Installing jars
cd $owd
cd $libs
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 -D $LIBS $prefix/libexec/flowvisor/.

echo Installing flowvisor.jar
cd $owd
cd $dist
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 -D flowvisor.jar  $prefix/libexec/flowvisor/.

echo Installing configs
cd $owd
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $scriptd/envs $prefix/etc/flowvisor/envs.sh
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $base/mySSLKeyStore $prefix/etc/flowvisor
$prefix/sbin/config-gen-default $prefix/etc/flowvisor/$config
