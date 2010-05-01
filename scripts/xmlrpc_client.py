#!/usr/bin/python
import xmlrpclib

user="root"
passwd="0fw0rk"
s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
print s.api.ping("Joe mama")
print s.api.listFlowSpace()

# Print list of available methods
#print s.system.listMethods()
