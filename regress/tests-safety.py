#!/usr/bin/python
from fvregress import *
import string     # really?  you have to do this?
import sys
import xmlrpclib



user="root"
passwd="0fw0rk"
rpcport=18080
# start up a flowvisor with 1 switch (default) and two guests


try:

    h= FvRegress()
    port=16633
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
        h.spawnFlowVisor(configFile="tests-safety.xml")
    h.lamePause()
    h.addSwitch(name='switch1',port=port)
    h.addSwitch(name='switch2',port=port)


    if wantPause:
        doPause("start tests")
#################################### Start Tests
    # initialize, then delete slice alice, then send to it
    # bug #92; would cause a NPE
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:" + str(rpcport) + "/xmlrpc")
    s.api.ping('alice')
    s.api.deleteSlice('alice')
    slices = s.api.listSlices()
    if 'alice' in slices :
        raise "Deleting alice's slice failed!!!!"       # need to come up with a real regression testing framework
    # now try to send something to alice, and make sure she doesn't get it
    packet_to_g0_p0 =  FvRegress.OFVERSION + '''0a 0052 0000 0000 0000 0101
                0040 0000 0000 0000 0000 0001 0000 0000
                0002 0800 4500 0032 0000 0000 40ff f72c
                c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
                19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
                3354 51d5 0036'''
    packet_to_g1_p0 =  FvRegress.OFVERSION + '''0a 0052 0000 0001 0000 0101
                0040 0001 0000 0000 0000 0002 0000 0000
                0001 0800 4500 0032 0000 0000 40ff f72c
                c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
                19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
                3354 51d5 0036'''
    h.runTest(name="packet_in to non-existant slice", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', packet_to_g0_p0),
            TestEvent( "clear?","guest","alice", packet=""),
            TestEvent( "send","switch",'switch1', packet_to_g1_p0),
            TestEvent( "recv","guest",'bob', packet_to_g1_p0),
            ])
#########################################
# more tests for this setup HERE
#################################### End Tests
finally:
    if wantPause:
        doPause("start cleanup")
    h.cleanup()

