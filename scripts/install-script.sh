#!/bin/sh

# sadly hacked together install script; really only used to bootstrap rpm or debs
# apologies in advance to anyone who has to debug this

prefix_default=/usr/local
fvuser_default=flowvisor
fvgroup_default=flowvisor
root_default=""

install=install
base=`dirname $0`/..
scriptd=$base/scripts
libs=$base/lib
dist=$base/dist
config=flowvisor-config.xml
#verbose=-v

usage="$0 [-p prefix_dir] [-u flowvisor_user] [-g flowvisor_group] [-r root_dir]"
usage_str="p:u:g:r:"

while getopts $usage_str opt; do
    case $opt in
    p)
        prefix=$OPTARG
        echo "Set prefix to '$prefix'" >&2
    ;;
    u)
        fvuser=$OPTARG
        echo "Set fvuser to '$fvuser'" >&2
    ;;
    g)
        fvgroup=$OPTARG
        echo "Set fvgroup to '$fvgroup'" >&2
    ;;
    r)
        root=$OPTARG
        echo "Set root to '$root'" >&2
    ;;
    \?)
        echo "Invalid option: -$OPTARG" >&2
        cat << EOF  >&2
        Usage:
        $usage
            defaults:
                prefix_dir=$prefix_default
                fvuser=$fvuser_default
                fvgroup=$fvgroup_default
                root=$root_default
EOF
        exit 1
    ;;
esac
done

echo "Using source dir: $base"

test -z "$prefix" && read -p "Installation prefix ($prefix_default): " prefix
if [ "X$prefix" == "X" ] ; then
    prefix=$prefix_default
fi

test -z "$fvuser" && read -p "FlowVisor User ($fvuser_default): " fvuser
if [ "X$fvuser" == "X" ] ; then
    fvuser=$fvuser_default
fi

test -z "$fvgroup" && read -p "FlowVisor Group ($fvgroup_default): " fvgroup
if [ "X$fvgroup" == "X" ] ; then
    fvgroup=$group_default
fi

test -z "$root" && read -p "Install to different root directory ($root_default) " root
if [ "X$root" == "X" ] ; then
    root=$root_default
fi


echo Installing FlowVisor into $root$prefix with prefix=$prefix as user/group ${fvuser}:${fvgroup}

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
    $install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -d $root$prefix/$d
done

echo Installing scripts
$install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -D $bin_SCRIPTS $root$prefix/bin
$install $verbose --owner=$fvuser --group=$fvgroup --mode=755 -D $sbin_SCRIPTS $root$prefix/sbin


echo Installing jars
cd $owd
cd $libs
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 -D $LIBS $root$prefix/libexec/flowvisor/.

echo Installing flowvisor.jar
cd $owd
cd $dist
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 -D flowvisor.jar  $root$prefix/libexec/flowvisor/.

echo Installing configs
cd $owd
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $scriptd/envs $root$prefix/etc/flowvisor/envs.sh
$install $verbose --owner=$fvuser --group=$fvgroup --mode=644 $base/mySSLKeyStore $root$prefix/etc/flowvisor
install_root=$root $root$prefix/sbin/config-gen-default $root$prefix/etc/flowvisor/$config
