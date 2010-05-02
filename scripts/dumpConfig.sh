#!/bin/sh

base=/home/capveg/git/proj/flowvisor/flowvisor-nwo-java

if [ -f $base/scripts/envs.sh ]; then
    . $base/scripts/envs.sh
fi


java -cp $classpath org.flowvisor.config.DefaultConfig $@
