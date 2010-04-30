#!/usr/bin/python
import xmlrpclib

user="pyroot"
passwd="grr"
s = xmlrpclib.ServerProxy("http://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
print s.api.ping("Joe mama")
print s.api.listFlowSpace()

# Print list of available methods
#print s.system.listMethods()
