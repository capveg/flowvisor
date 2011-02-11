#!/usr/bin/python
from fvregress import *
import string     # really?  you have to do this?
import sys
import xmlrpclib


# start up a flowvisor with 1 switch (default) and two guests

#h= HyperTest(guests=[('localhost',54321),('localhost',54322)],
#    hyperargs=["-v0", "-a", "flowvisor-conf.d-base", "ptcp:%d"% HyperTest.OFPORT],valgrind=valgrindArgs)

wantPause = True

def test_failed(s):
    s = "TEST FAILED!!!: " + s
    print s
    raise Exception(s)



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
        h.spawnFlowVisor(configFile="tests-flowdb.xml")
    h.lamePause()
    h.addSwitch(name='switch1',port=port)
    h.addSwitch(name='switch2',port=port)

    user = "root"
    passwd = "0fw0rk"
    rpcport = 18080
    s = xmlrpclib.ServerProxy("https://" + user + ":" + passwd + "@localhost:" + str(rpcport) + "/xmlrpc")
    s.verbose=True

    if wantPause:
        doPause("start tests")
#################################### Start Tests
    
    flow_mod1 = FvRegress.OFVERSION + \
                          '''0e 00 50 40 00 90 b6 00 00 00 00 00 00 00 00
                          00 00 00 02 00 0c 29 c6 36 8d ff ff 00 00 08 06
                          00 02 00 00 c0 01 f9 7b c0 01 f9 79 00 00 00 00
                          00 00 00 00 00 00 00 00 00 00 00 05 00 00 80 00
                          00 00 01 6f 00 00 00 01 00 00 00 08 00 02 00 00'''
    flow_mod2 = FvRegress.OFVERSION + \
                          '''0e 00 50 40 00 90 b6 00 00 00 00 00 00 00 00
                          00 00 00 02 22 0c 29 c6 36 8d ff ff 00 00 08 06
                          00 02 00 00 c0 01 f9 7b c0 01 f9 79 00 00 00 00
                          00 00 00 00 00 00 00 00 00 00 00 05 00 00 80 00
                          00 00 01 6f 00 00 00 01 00 00 00 08 00 02 00 00'''
    flow_mod3 = FvRegress.OFVERSION + \
                          '''0e 00 50 40 00 90 b6 00 00 00 00 00 00 00 00
                          00 00 00 02 33 0c 29 c6 36 8d ff ff 00 00 08 06
                          00 02 00 00 c0 01 f9 7b c0 01 f9 79 00 00 00 00
                          00 00 00 00 00 00 00 00 00 00 00 05 00 00 80 00
                          00 00 01 6f 00 00 00 01 00 00 00 08 ff fc 00 00'''
    flow_mod3_after = FvRegress.OFVERSION + \
                    '''0e 00 58 00 00 01 04 00 00 00 00 00 00 00 00
                    00 00 00 02 33 0c 29 c6 36 8d ff ff 00 00 08 06
                    00 02 00 00 c0 01 f9 7b c0 01 f9 79 00 00 00 00
                    00 00 00 00 00 00 00 00 00 00 00 05 00 00 80 00
                    00 00 01 6f 00 00 00 01 00 00 00 08 00 02 00 00
                    00 00 00 08 00 03 00 00'''
    h.runTest(name="flowdb install",timeout=timeout,  events= [
          # send flow_mod, make sure it succeeds
          TestEvent( "send","guest",'alice', flow_mod1),
          TestEvent( "recv","switch",'switch1', flow_mod1), 
          TestEvent( "send","guest",'alice', flow_mod2),
          TestEvent( "recv","switch",'switch1', flow_mod2), 
          # this test sends an OFPP_ALL and make sures it expands
          # FIXME: fix API so we can test if the expand worked...
          # right now, this test is half-assed
          TestEvent( "send","guest",'alice', flow_mod3),
          TestEvent( "recv","switch",'switch1', flow_mod3_after), 
          ])

####################################
    print "Testing getSwitchFlowDB"            
    flows = s.api.getSwitchFlowDB("1")
    if len(flows) != 3:
        test_failed("Wanted 3 flows, got %d" % len(flows))
    else: 
        print "     SUCCESS"
    print "Got %d flows" % len(flows)
    for flow in flows:
        print "==== Got flow "
        for key,val in flow.iteritems():
            print "     %s=%s" % (key,val)
####################################
    print "Testing getSliceRewriteDB"            
    rewriteDB = s.api.getSliceRewriteDB("alice","1")
    if len(rewriteDB) != 3:
        test_failed("Wanted 3 rewrites, got %d" % len(rewriteDB))
    else: 
        print "     SUCCESS"
    for original, rewrites in rewriteDB.iteritems():
        print "========= Original: '%s'" % original
        print "========= " 
        for rewrite in rewrites:
            print "--------- Rewrite" 
            for key,val in rewrite.iteritems():
                print "     => %s=%s" % (key,val)
    #print "Sleeping!"
    #time.sleep(100000)

####################################
    flow_expire1 = FvRegress.OFVERSION + \
                '''0b 00 58 00 00 00 01 
                00 00 00 00 00 00 00 00 00 00 00 02 00 0c 29 c6 
                36 8d ff ff 00 00 08 06 00 02 00 00 c0 01 f9 7b 
                c0 01 f9 79 00 00 00 00
                00 00 00 00 00 00 00 00 80 00 00 00 00 00 00 00
                00 00 00 0b 00 00 00 00 00 00 00 00 00 00 01 e1
                00 00 00 00 00 00 02 58
                '''
    h.runTest(name="flowdb flow remove",timeout=timeout,  events= [
          # send flow_mod, make sure it succeeds
          TestEvent( "send","switch",'switch1', flow_expire1),
          TestEvent( "recv","guest",'alice', flow_expire1), 
          ])
#########################################
    print "Testing getSwitchFlowDB after remove"            
    flows = s.api.getSwitchFlowDB("1")
    if len(flows) != 2:
        test_failed("Wanted 2 flows, got %d" % len(flows))
    else: 
        print "     SUCCESS"
    print "Testing getSliceRewriteDB after remove"            
    rewriteDB = s.api.getSliceRewriteDB("alice","1")
    if len(rewriteDB) != 2:
        test_failed("Wanted 2 rewrites, got %d" % len(rewriteDB))
    else: 
        print "     SUCCESS"
#########################################
# more tests for this setup HERE
#################################### End Tests
finally:
    if wantPause:
        doPause("start cleanup")
    h.cleanup()

