#!/bin/sh

base=/home/capveg/git/proj/flowvisor/flowvisor-nwo-java

if [ -f $base/scripts/envs.sh ] ; then
    . $base/scripts/envs.sh
fi

echo Staring FlowVisor >&2 
exec java $sslopts -cp $classpath org.flowvisor.FlowVisor $@
