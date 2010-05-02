#!/bin/sh

base=`dirname $0`/..

if [ -f $base/scripts/envs.sh ] ; then
    . $base/scripts/envs.sh
else
    echo "Could not find envs.sh: dying..." >&2
    exit 1
fi

java -cp $classpath org.flowvisor.config.DefaultConfig $@

