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
    sys.exit(0)

try:

    h= FvRegress()
    port=16633
    rpcport=18080
    h.addController("alice",    54321)
    h.addController("bob",      54322)
    wantPause = False

    if len(sys.argv) > 1 :
        port=int(sys.argv[1])
        timeout=60
        h.useAlreadyRunningFlowVisor(port)
    else:
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
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:" + str(rpcport) + "/xmlrpc")
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
    x =  s.api.listDevices()
    valid_len = 2
    if len(x) != valid_len :
        print "Got " + str(len(x)) + " entries but wanted " + str(valid_len)
        test_failed("listDevices root test1")
    valid = "00:00:00:00:00:00:00:01"
    if x[0] != valid:
        print "Got " + x[0] + " but wanted " + valid
        test_failed("listDevices root test2")
    valid = "00:00:00:00:00:00:00:02"
    if x[1] != valid:
        print "Got " + x[1] + " but wanted " + valid
        test_failed("listDevices root test3")
    x = s.api.getDeviceInfo("00:00:00:00:00:00:00:01")
    for key,val in  x.iteritems():
        print "                 "+ key + "="  + val
    print "     passed"
    print "Root getLinks test"
    x = s.api.getLinks()
    linkcount=0
    valid_len = 2
    if len(x) != valid_len: 
        print "Got " + str(len(x)) + " links but wanted " + str(valid_len)
        test_failed("getLinks root test")
    for link in x:
        print "             Link " + str(linkcount) + ":"
        linkcount+=1
        for key,val in link.iteritems():
            print "                 " + key  + "=" + val 
    print "     passed"
    print "Slice creation: Cathy"
    lame_email = "cathy@foo.com"
    if not s.api.createSlice("cathy","cathyPass","tcp:localhost:54323",lame_email) :
        print "Got false from creating slice for cathy"
        test_failed("slice creation")
    x = s.api.getConfig("slices!cathy!contact_email")
    if (len(x) < 1) or x[0] != lame_email: 
        print "Failed to get correct email for cathy: wanted " + lame_email + " but got " + str(x)
        test_failed("slice creation")
    print "     passed"
    print "Slice creation: Doug (with periods)"
    cool_email = "laudi@daudi.com"
    if not s.api.createSlice("doug.e.fresh", "theOriginal", "tcp:localhost:54324", cool_email) :
        print "Got false from creating slice for doug (with periods)"
        test_failed("slice creation with periods")
    x = s.api.getConfig("slices!doug.e.fresh!contact_email")
    if (len(x) < 1)  or x[0] != cool_email: 
        print "Failed to get correct email for doug: wanted " + cool_email + " but got " + str(x)
        test_failed("slice creation with periods")
    print "     passed"


#################################### Start Alice Tests
    user="alice"
    passwd="alicePass"
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:" + str(rpcport) + "/xmlrpc")
    print "Alice ping test"
    x= s.api.ping("Joe mama")
    valid = "PONG(alice): Joe mama"
    if(x != valid) : 
        print "Got '"+ x + "' but wanted '" + valid + "'"
        test_failed("ping test")
    print "     passed"
    print "Alice listFlowSpace test"
    x = s.api.listFlowSpace()
    valid_len = 6 
    if len(x) != valid_len: 
        print "Got " + str(len(x)) + " entries but wanted " + str(valid_len)
        test_failed("listFlowSpace alice test")
    print "     passed"
    ## FIXME!
    #print s.api.change_passwd("alice","foo")
#################################### Start Alice Tests
    user="bob"
    passwd="bobPass"
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:" + str(rpcport) + "/xmlrpc")
    print "Bob ping test"
    x= s.api.ping("Joe mama")
    valid = "PONG(bob): Joe mama"
    if(x != valid) : 
        print "Got '"+ x + "' but wanted '" + valid + "'"
        test_failed("ping test")
    print "     passed"
    print "Bob listFlowSpace test"
    x = s.api.listFlowSpace()
    valid_len = 4 
    if len(x) != valid_len: 
        print "Got " + str(len(x)) + " entries but wanted " + str(valid_len)
        test_failed("listFlowSpace bob test")
    print "     passed"


#################################### Start Tests
# more tests for this setup HERE
#################################### End Tests
finally:
    h.cleanup()

