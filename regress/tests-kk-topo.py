#!/usr/bin/python
from fvregress import *
import string     # really?  you have to do this?
import sys


# start up a flowvisor with 1 switch (default) and two guests

#h= HyperTest(guests=[('localhost',54321),('localhost',54322)],
#    hyperargs=["-v0", "-a", "flowvisor-conf.d-base", "ptcp:%d"% HyperTest.OFPORT],valgrind=valgrindArgs)

wantPause = True

try:

    h= FvRegress()
    port=16633
    h.addController("ncast",    54321)
    h.addController("prod",      54322)

    if len(sys.argv) > 1 :
        wantPause = False
        port=int(sys.argv[1])
        timeout=60
        h.useAlreadyRunningFlowVisor(port)
    else:
        wantPause = False
        timeout=5
        h.spawnFlowVisor(configFile="tests-kk-topo.xml")
    h.lamePause()
    h.addSwitch(name='switch1',port=port)


    if wantPause:
        doPause("start tests")
    ################################################################
    lldp_out =     FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
                ffff 0008 0000 0008 0001 0080 0123 2000
                0001 0000 0000 0000 88cc 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
                983f 2b71 c2e4 1b6f bc11 8250'''
    lldp_out_after_prod = FvRegress.OFVERSION + \
                '''0d 00 7b 00 00 ab cd ff ff ff ff ff ff 00 08
                00 00 00 08 00 01 00 80 01 23 20 00 00 01 00 00
                00 00 00 00 88 cc 45 00 00 32 00 00 40 00 40 11
                28 68 c0 a8 c8 00 c0 a8 c9 01 00 01 00 00 00 1e
                d7 c3 cd c0 25 1b e6 dc ea 0c 72 6d 98 3f 2b 71
                c2 e4 1b 6f bc 11 82 50 11 81 07 70 72 6f 64 00
                20 20 20 20 6d 61 67 69 63 20 66 6c 6f 77 76 69
                73 6f 72 31 00 05 15 de ad ca fe'''
    lldp_out_after_ncast = FvRegress.OFVERSION + \
                '''0d 00 7c 00 00 ab cd ff ff ff ff ff ff 00 08
                00 00 00 08 00 01 00 80 01 23 20 00 00 01 00 00
                00 00 00 00 88 cc 45 00 00 32 00 00 40 00 40 11
                28 68 c0 a8 c8 00 c0 a8 c9 01 00 01 00 00 00 1e
                d7 c3 cd c0 25 1b e6 dc ea 0c 72 6d 98 3f 2b 71
                c2 e4 1b 6f bc 11 82 50 12 01 07 6e 63 61 73 74
                00 20 20 20 20 6d 61 67 69 63 20 66 6c 6f 77 76
                69 73 6f 72 31 00 06 15 de ad ca fe'''
    lldp_in_prod =      FvRegress.OFVERSION + \
                    '''0a 00 64 00 00 00 00 00 00 01 01
                    00 40 00 03 00 00 01 23 20 00 00 01 00 00
                    00 00 00 00 88 cc 45 00 00 32 00 00 40 00 40 11
                    28 68 c0 a8 c8 00 c0 a8 c9 01 00 01 00 00 00 1e
                    d7 c3 cd c0 25 1b e6 dc ea 0c 72 6d 98 3f 2b 71
                    c2 e4 1b 6f bc 11 82 50 08 81 07 
                    70 72 6f 64 00
                    66 76 31 00 
                    05 04 de ad ca fe'''
    lldp_in_ncast =      FvRegress.OFVERSION + \
                    '''0a 00 65 00 00 00 00 00 00 01 01
                    00 40 00 03 00 00 01 23 20 00 00 01 00 00
                    00 00 00 00 88 cc 45 00 00 32 00 00 40 00 40 11
                    28 68 c0 a8 c8 00 c0 a8 c9 01 00 01 00 00 00 1e
                    d7 c3 cd c0 25 1b e6 dc ea 0c 72 6d 98 3f 2b 71
                    c2 e4 1b 6f bc 11 82 50 08 81 07 
                    6e 63 61 73 74 00
                    66 76 31 00 
                    06 04 de ad ca fe'''
                    
    lldp_in_after =  FvRegress.OFVERSION + \
                '''0a 00 52 00 00 00 00 00 00 01 01 00 40 00 03
                00 00 01 23 20 00 00 01 00 00 00 00 00 00 88 cc
                45 00 00 32 00 00 40 00 40 11 28 68 c0 a8 c8 00
                c0 a8 c9 01 00 01 00 00 00 1e d7 c3 cd c0 25 1b
                e6 dc ea 0c 72 6d 98 3f 2b 71 c2 e4 1b 6f bc 11
                82 50'''
    h.runTest(name="lldp hack", timeout=timeout, events= [
            TestEvent( "send","guest",'prod', lldp_out),
            TestEvent( "recv","switch",'switch1', lldp_out_after_prod),
            TestEvent( "send","guest",'ncast', lldp_out),
            TestEvent( "recv","switch",'switch1', lldp_out_after_ncast),
            TestEvent( "send","switch",'switch1', lldp_in_prod),
            TestEvent( "recv","guest",'prod', lldp_in_after),
            TestEvent( "send","switch",'switch1', lldp_in_ncast),
            TestEvent( "recv","guest",'ncast', lldp_in_after),
            ])
#########################################
    arp_in = FvRegress.OFVERSION + '''0a 00 4e 00 00 00 00 00 10 2e fc 00 3c 00 01
                    00 00 ff ff ff ff ff ff 00 23 ae 35 fd f3 08 06
                    00 01 08 00 06 04 00 01 00 23 ae 35 fd f3 0a 4f
                    01 69 00 00 00 00 00 00 0a 4f 01 9f 00 00 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00'''
    h.runTest(name="kk's arp", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', arp_in),
            TestEvent( "recv","guest",'prod', arp_in),
            ])
#########################################
# more tests for this setup HERE
#################################### End Tests
finally:
    if wantPause:
        doPause("start cleanup")
    h.cleanup()

