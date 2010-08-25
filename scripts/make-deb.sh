#!/bin/sh

if [ $# -ne 1 ] ; then
    echo "Usage: $0 version-string" >&2
    exit 1
fi
if [ ! -f Makefile ] ; then 
    echo "Run me in the base of the source directory; no Makefile found" >&2
    exit 1
fi
version=$1

base=`pwd`
sudo rm -rf $base/pkgbuild/root
make prefix=/usr root=$base/pkgbuild/root fvuser=flowvisor fvgroup=flowvisor install

mkdir -p $base/pkgbuild/root/etc/init.d
cp fv-startup.sh $base/pkgbuild/root/etc/init.d/flowvisor
chmod 755 $base/pkgbuild/root/etc/init.d/flowvisor

mkdir -p $base/pkgbuild/root/DEBIAN
cd $base/pkgbuild/root/DEBIAN
cat > control << EOF
Package: flowvisor
Version: $version
Architecture: i386
Maintainer: Rob Sherwood <rob.sherwood@stanford.edu>
Section: misc
Priority: optional
Description: The OpenFlow FlowVisor
Depends: sun-java6-jdk
EOF

cd ..
#sudo chown -Rh root .
#sudo chgrp -Rh root .
#sudo find . \( -type f -o -type d \) -exec chmod ugo-w {} \;
#sudo chown -Rh openflow:openflow ./usr/etc/flowvisor
#sudo chmod 2775 ./usr/etc/flowvisor
#sudo chmod u+w ./usr/etc/flowvisor/*
#sudo chmod u+w DEBIAN

cd ..
ctlfile="root/DEBIAN/control"
pkgname=$(grep "^Package:" ${ctlfile} | awk '{print $2}')
arch=$(grep "^Architecture:" ${ctlfile} | awk '{print $2}')
tgtfile="${pkgname}_${version}_${arch}.deb"
dpkg -b root $tgtfile
