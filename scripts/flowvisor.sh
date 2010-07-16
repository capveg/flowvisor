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
if [ -z $DEBUG ] ; then 
    exec java $sslopts -cp $classpath org.flowvisor.FlowVisor "$@" 
else
    exec java $sslopts -cp $classpath org.flowvisor.FlowVisor "$@" 2>&1 | tee /tmp/flowvisor-$$.log
fi
