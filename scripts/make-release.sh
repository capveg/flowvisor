#!/bin/sh

fv_main=src/org/flowvisor/FlowVisor.java

if [ "X$1" = "X" ] ; then
    echo "Usage: $0 release-name" >&2
    exit 1
fi

release=$1
sed -i "s/FLOWVISOR_VERSION = \"([^\"])\"/FLOWVISOR_VERSION = \"$release\"/" $fv_main
git diff

