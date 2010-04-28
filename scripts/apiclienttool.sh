#!/bin/sh

classpath=lib/openflow.jar:\
lib/xmlrpc-client-3.1.3.jar:\
lib/xmlrpc-common-3.1.3.jar:\
lib/commons-logging-1.1.jar:\
lib/ws-commons-util-1.0.2.jar:\
dist/flowvisor.jar

exec java -cp $classpath org.flowvisor.api.APIClientTool $@
