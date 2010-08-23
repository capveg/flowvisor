#!/bin/sh

#base=PREFIX

usage() {
    cat << "EOF"  
USAGE: $0 cmd config.xml [options]
    match config.xml <dpid> <match>

EOF
    exit 1
}

if [ -z $base ] ; then
    envs=`dirname $0`/../scripts/envs.sh
else 
    envs=$base/etc/flowvisor/envs.sh
fi

if [ -f $envs ] ; then
    . $envs
else
    echo "Could not find $envs: dying..." >&2
    exit 1
fi

case "X$1" in 
    Xmatch)
        shift
        java -cp $classpath $fv_defines org.flowvisor.message.FVPacketIn "$@"
    ;;
    X)
        usage
    ;;
    *)
    echo "Unknown command '$1'" >&2
    exit 1
    ;;
esac


