#!/usr/bin/python
import xmlrpclib

user="root"
passwd="0fw0rk"
s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
print "=============== Root's view ================"
print s.api.ping("Joe mama")
print s.api.listFlowSpace()

user="alice"
passwd="alicePass"
s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
print "=============== Alice's view ================"
print s.api.ping("Joe mama")
print s.api.listFlowSpace()
user="bob"
passwd="bobPass"
s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
print "=============== Bob's view ================"
print s.api.ping("Joe mama")
print s.api.listFlowSpace()

#### FIXME
#print "=============== available methods ============"
# Print list of available methods
#print s.system.listMethods()
