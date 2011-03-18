#!/bin/bash

### usage: fvSwitchStats <DPID>
# Contributed by Christopher J. Tengi <tengi@cs.princeton.edu>

SED=/bin/sed
GREP=/bin/grep
FVCTL=/var/local/bin/fvctl

dpid=$1

host=`hostname`
uptime=`uptime | sed -e 's/, .*$//'`

stats=`$FVCTL --passwd-file=/mrtg/.fvp getSwitchStats $dpid | $GREP Total`

packetIn=`echo $stats | $SED -e 's/^.*PACKET_IN=//' -e 's/,.*$//'`
packetOut=`echo $stats | $SED -e 's/^.*PACKET_OUT=//' -e 's/,.*$//'`

echo $packetIn
echo $packetOut
echo $uptime
echo $host
