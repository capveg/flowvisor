#!/bin/sh

# base of source tree; needs rewrite for distribution
base=/home/capveg/git/proj/flowvisor/flowvisor-nwo-java
# Setup some environmental variables
classpath=$base/lib/openflow.jar:\
$base/lib/xmlrpc-client-3.1.3.jar:\
$base/lib/xmlrpc-common-3.1.3.jar:\
$base/lib/commons-logging-1.1.jar:\
$base/lib/ws-commons-util-1.0.2.jar:\
$base/dist/flowvisor.jar



