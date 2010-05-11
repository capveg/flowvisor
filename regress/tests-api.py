#!/usr/bin/python
from fvregress import *
import string     # really?  you have to do this?
import sys
import xmlrpclib



# start up a flowvisor with 1 switch (default) and two guests

#h= HyperTest(guests=[('localhost',54321),('localhost',54322)],
#    hyperargs=["-v0", "-a", "flowvisor-conf.d-base", "ptcp:%d"% HyperTest.OFPORT],valgrind=valgrindArgs)

def test_failed(str):
    print "TEST FAILED!!!: " + str
    sys.exit(1)

try:

    h= FvRegress()
    port=16633
    rpcport=8080
    h.addController("alice",    54321)
    h.addController("bob",      54322)

    if len(sys.argv) > 1 :
        wantPause = False
        port=int(sys.argv[1])
        timeout=60
        h.useAlreadyRunningFlowVisor(port)
    else:
        wantPause = False
        timeout=5
        h.spawnFlowVisor(configFile="tests-base.xml")
    h.lamePause()
    h.addSwitch(name='switch1',port=port)
    h.addSwitch(name='switch2',port=port)
    h.lamePause()


    if wantPause:
        doPause("start tests")
#################################### Start Root Tests
    user="root"
    passwd="0fw0rk"
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
    print "Root ping test"
    x= s.api.ping("Joe mama")
    valid = "PONG(root): Joe mama"
    if(x != valid) : 
        print "Got '"+ x + "' but wanted '" + valid + "'"
        test_failed("ping test")
    print "     passed"
    print "Root listFlowSpace test"
    x = s.api.listFlowSpace()
    #for x in  s.api.listFlowSpace():
    #        print x
    valid_len = 10
    if len(x) != valid_len: 
        print "Got " + str(len(x)) + " entries but wanted " + str(valid_len)
        test_failed("listFlowSpace root test")
    print "     passed"
    print "GetDevices Test"
    x =  s.api.getDevices()
    valid_len = 2
    if len(x) != valid_len :
        print "Got " + str(len(x)) + " entries but wanted " + str(valid_len)
        test_failed("getDevices root test")
    print "     passed"
    for d in x: 
        print d

#################################### Start Alice Tests
    user="alice"
    passwd="alicePass"
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
    print "Root ping test"
    x= s.api.ping("Joe mama")
    valid = "PONG(alice): Joe mama"
    if(x != valid) : 
        print "Got '"+ x + "' but wanted '" + valid + "'"
        test_failed("ping test")
    print "     passed"
    print "Root listFlowSpace test"
    x = s.api.listFlowSpace()
    valid_len = 7 
    if len(x) != valid_len: 
        print "Got " + len(x) + " entries but wanted " + valid_len
        test_failed("listFlowSpace alice test")
    print "     passed"
    #print s.api.change_passwd("alice","foo")
    user="bob"
    passwd="bobPass"
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:8080/xmlrpc")
    print "=============== Bob's view ================"
    print s.api.ping("Joe mama")
    for x in  s.api.listFlowSpace():
        print x


#################################### Start Tests
# more tests for this setup HERE
#################################### End Tests
finally:
    if wantPause:
        doPause("start cleanup")
    h.cleanup()
