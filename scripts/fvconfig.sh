#!/bin/sh

#base=PREFIX

usage() {
    cat << "EOF"  
USAGE: fvconfig cmd config.xml [options]
    match config.xml <dpid> <match>
    dump config.xml
    chpasswd config.xml <slicename>
    query config.xml <dpid> [slicename]
    generate newconfig.xml [hostname] [admin_passwd] [of_port] [api_port]
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

if [ "x$1" = "x" ] ; then
    usage
    exit 1
fi

cmd=$1
shift
case "X$cmd" in 
    Xmatch)
        exec java -cp $classpath $fv_defines org.flowvisor.message.FVPacketIn "$@"
    ;;
    Xdump)
        exec java -cp $classpath $fv_defines org.flowvisor.config.DefaultConfig "$@"
    ;;
    Xchpasswd)
        exec java -cp $classpath $fv_defines org.flowvisor.api.APIAuth "$@"
    ;;
    Xquery)
        exec java -cp $classpath $fv_defines org.flowvisor.flows.FlowSpaceUtil "$@"
    ;;
    Xgenerate)
        echo "Trying to generate SSL Server Key with passwd from scripts/envs.sh" >&2
        if [ "X$2" != "X" ] ; then
            cn=$2
        else
            cn=`hostname`
        fi
        echo Generating cert with common name == $cn
        dname="-dname cn=$cn"
        keytool -genkey -keystore $SSL_KEYSTORE -storepass $SSL_KEYPASSWD -keypass $SSL_KEYPASSWD -keyalg RSA $dname
        exec java -cp $classpath $fv_defines org.flowvisor.config.FVConfig $1 $3 $4 $5
    ;;
    X)
        usage
    ;;
    *)
        echo "Unknown command '$1'" >&2
        usage
    ;;
esac


