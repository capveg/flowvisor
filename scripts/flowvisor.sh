#!/bin/sh

#base=PREFIX

if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
    DEBUG=yes
else
    envs=$base/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

echo Staring FlowVisor >&2 
exec java -server -Xms100M -Xmx1000M $sslopts -cp $classpath org.flowvisor.FlowVisor "$@" 
