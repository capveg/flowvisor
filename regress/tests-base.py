#!/usr/bin/python
from fvregress import *
import string     # really?  you have to do this?
import sys

if len(sys.argv) > 1 :
    wantPause = False
    timeout=5
    h = FvRegress.parseConfig(configDir='flowvisor-conf.d-base', alreadyRunning=True, port=int(sys.argv[1]))
else:
    wantPause = False
    timeout=5
    h = FvRegress.parseConfig(configDir='flowvisor-conf.d-base')

# start up a flowvisor with 1 switch (default) and two guests

#h= HyperTest(guests=[('localhost',54321),('localhost',54322)],
#    hyperargs=["-v0", "-a", "flowvisor-conf.d-base", "ptcp:%d"% HyperTest.OFPORT],valgrind=valgrindArgs)


if wantPause:
    doPause("start tests")
#################################### Start Tests
try:


    feature_request =     FvRegress.OFVERSION + '05 0008 2d47 c5eb'
    feature_request_after = FvRegress.OFVERSION + '05 0008 0000 0102'
    h.runTest(name="feature_request",timeout=timeout,  events= [
            TestEvent( "send","guest",'alice', feature_request),
            TestEvent( "recv","switch",'switch1', feature_request_after, strict=True),
            ])

    ############################################################
    feature_reply  =     FvRegress.OFVERSION + '''06 00e0 0000 0102 0000 76a9
                d40d 2548 0000 0100 0200 0000 0000 001f
                0000 03ff 0000 1ac1 51ff ef8a 7665 7468
                3100 0000 0000 0000 0000 0000 0000 0000
                0000 0000 0000 00c0 0000 0000 0000 0000
                0000 0000 0001 ce2f a287 f670 7665 7468
                3300 0000 0000 0000 0000 0000 0000 0000
                0000 0000 0000 00c0 0000 0000 0000 0000
                0000 0000 0002 ca8a 1ef3 77ef 7665 7468
                3500 0000 0000 0000 0000 0000 0000 0000
                0000 0000 0000 00c0 0000 0000 0000 0000
                0000 0000 0003 fabc 778d 7e0b 7665 7468
                3700 0000 0000 0000 0000 0000 0000 0000
                0000 0000 0000 00c0 0000 0000 0000 0000
                0000 0000'''
    # this reply should strip the STP bit, and trim the ports down to the allowable set
    feature_reply_after = FvRegress.OFVERSION + '''06 00 b0 2d 47 c5 eb 00 00 76 a9 d4 0d 25 48
        00 00 01 00 02 00 00 00 00 00 00 1f 00 00 03 ff
        00 00 1a c1 51 ff ef 8a 76 65 74 68 31 00 00 00
        00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        00 00 00 c0 00 00 00 00 00 00 00 00 00 00 00 00
        00 02 ca 8a 1e f3 77 ef 76 65 74 68 35 00 00 00
        00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        00 00 00 c0 00 00 00 00 00 00 00 00 00 00 00 00
        00 03 fa bc 77 8d 7e 0b 76 65 74 68 37 00 00 00
        00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
        00 00 00 c0 00 00 00 00 00 00 00 00 00 00 00 00'''



    h.runTest(name="feature_reply", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', feature_reply),
            TestEvent( "recv","guest",'alice', feature_reply_after),
            ])
    ############################################################
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
    h.runTest(name="switch2controller packet_in routing - by port", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', packet_to_g0_p0),
            TestEvent( "recv","guest",'alice', packet_to_g0_p0),
            TestEvent( "send","switch",'switch1', packet_to_g1_p0),
            TestEvent( "recv","guest",'bob', packet_to_g1_p0),
            ])
    ############################################################
    packet_to_g0_p3 =  FvRegress.OFVERSION + '''0a 0052 0000 0000 0000 0101
                0040 0003 0000 0000 0000 0001 0000 0000
                0002 0800 4500 0032 0000 0000 40ff f72c
                c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
                19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
                3354 51d5 0036'''
    packet_to_g1_p3 =  FvRegress.OFVERSION + '''0a 0052 0000 0002 0000 0101
                0040 0003 0000 0000 0000 0002 0000 0000
                0001 0800 4500 0032 0000 0000 40ff f72c
                c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
                19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
                3354 51d5 0036'''
    h.runTest(name="switch2controller packet_in routing - shared port", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', packet_to_g0_p3),
            TestEvent( "recv","guest",'alice', packet_to_g0_p3),
            TestEvent( "send","switch",'switch1', packet_to_g1_p3),
            TestEvent( "recv","guest",'bob', packet_to_g1_p3),
            ])
    #############################################################
    marco =     FvRegress.OFVERSION + '02 0008 2d47 c5eb'
    pollo =     FvRegress.OFVERSION + '03 0008 2d47 c5eb'
    h.runTest(name="echo/reply faking", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', marco),
            TestEvent( "recv","switch",'switch1', pollo),
            TestEvent( "send","guest",'bob', marco),
            TestEvent( "recv","guest",'bob', pollo),
            TestEvent( "send","guest",'alice', marco),
            TestEvent( "recv","guest",'alice', pollo),
            ])
    #############################################################
                # note; this packet is bogus! works just well enough for testing
    port_status_1 =    FvRegress.OFVERSION + '''0c 0040 2d47 c5ee 0000 0000 0000 0000
                0001 1234 5678 9abc def0 0000 0000 0000
                0000 0000 0000 0000 0000 0000 0000 0000
                0000 0000 0000 0000 0000 0000 0000 0000
                '''
    port_status_2 =    FvRegress.OFVERSION + '''0c 0040 2d47 c5ee 0000 0000 0000 0000
                0002 1234 5678 9abc def0 0000 0000 0000
                0000 0000 0000 0000 0000 0000 0000 0000
                0000 0000 0000 0000 0000 0000 0000 0000
                '''
    h.runTest(name="port status routing", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', port_status_1),    # switch sends port=0 event
            TestEvent( "recv","guest",'bob', port_status_1),    # only guest 0 should get it
            TestEvent( "send","switch",'switch1', port_status_2),    # switch sends port=1 event
            TestEvent( "recv","guest",'alice', port_status_2),    # only guest 1 should get it
            ])
    #############################################################
    packet_out_p0 = FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
                ffff 0008 0000 0008 0000 0080 0000 0000
                0001 0000 0000 0002 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
983f 2b71 c2e4 1b6f bc11 8250'''
            # note, the xid here is a function of the order of the tests;
            #    DO NOT CHANGE test order
    packet_out_p0_aftr = FvRegress.OFVERSION + '''0d 0058 0101 0000 ffff ffff
                ffff 0008 0000 0008 0000 0080 0000 0000
                0001 0000 0000 0002 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
983f 2b71 c2e4 1b6f bc11 8250'''
    packet_out_p1 = FvRegress.OFVERSION + '''0d 0058 0000 abce ffff ffff
                ffff 0008 0000 0008 0001 0080 0000 0000
                0001 0000 0000 0000 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
983f 2b71 c2e4 1b6f bc11 8250'''
            # note, the xid here is a function of the order of the tests;
            #    DO NOT CHANGE test order
    packet_out_p1_aftr = FvRegress.OFVERSION + '''01 000c 0000 abce 0004 0000'''    # NOT correct output -- yet; need to append bad message
    h.runTest(name="packet_out; valid", timeout=timeout, events= [
            TestEvent( "send","guest",'alice', packet_out_p0),
            TestEvent( "recv","switch",'switch1', packet_out_p0_aftr),
            TestEvent( "send","guest",'alice', packet_out_p1),
            TestEvent( "recv","guest",'alice', packet_out_p1_aftr),
            ])
    ################################################################
        # poke the switch again, to make sure it survived
    marco =     FvRegress.OFVERSION + '02 0008 2d47 c5eb'
    pollo =     FvRegress.OFVERSION + '03 0008 2d47 c5eb'
    h.runTest(name="echo/reply faking (again)", timeout=timeout, events= [
            TestEvent( "send","switch",'switch1', marco),
            TestEvent( "recv","switch",'switch1', pollo),
            TestEvent( "send","guest",'bob', marco),
            TestEvent( "recv","guest",'bob', pollo),
            TestEvent( "send","guest",'alice', marco),
            TestEvent( "recv","guest",'alice', pollo),
            ])
    ################################################################
    lldp_out =     FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
                ffff 0008 0000 0008 0001 0080 0123 2000
                0001 0000 0000 0000 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
983f 2b71 c2e4 1b6f bc11 8250'''
            # note, the xid here is a function of the order of the tests;
            #    DO NOT CHANGE test order
    lldp_out_after = FvRegress.OFVERSION + '''0d 0058 0301 0000 ffff ffff
                ffff 0008 0000 0008 0001 0080 0123 20ff
                0002 0000 0000 0000 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
983f 2b71 c2e4 1b6f bc11 8250'''
    lldp_in =      FvRegress.OFVERSION + '''0a 0052 0000 0000 0000 0101
                0040 0003 0000 0123 20ff 0002 0000 0000
                0002 0800 4500 0032 0000 0000 40ff f72c
                c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
                19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
                3354 51d5 0036'''
    lldp_in_after =  FvRegress.OFVERSION + '''0a 0052 0000 0000 0000 0101
                0040 0003 0000 0123 2000 0001 0000 0000
                0002 0800 4500 0032 0000 0000 40ff f72c
                c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
                19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
                3354 51d5 0036'''
    h.runTest(name="lldp hack", timeout=timeout, events= [
            TestEvent( "send","guest",'bob', lldp_out),
            TestEvent( "recv","switch",'switch1', lldp_out_after),
            TestEvent( "send","switch",'switch1', lldp_in),
            TestEvent( "recv","guest",'bob', lldp_in_after),
            ])
    ################################################################
    stats_request_desc =         FvRegress.OFVERSION + '''10 000c 0000 0000 0000 0000'''
    stats_request_desc_after =     FvRegress.OFVERSION + '''10 000c 0801 0000 0000 0000'''
    stats_reply_desc = FvRegress.OFVERSION + '''11 03 2c 08 01 00 00 00 00 00 00 4e 45 43 20
            43 6f 72 70 6f 72 61 74 69 6f 6e 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 52 65 66 65
            72 65 6e 63 65 20 55 73 65 72 2d 53 70 61 63 65
            20 53 77 69 74 63 68 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00 4e 6f 6e 65
            00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
            00 00 00 00 00 00 00 00 00 00 00 00            '''
    stats_reply_desc_after = FvRegress.OFVERSION + '''11032c00000000000000004e454320436f72706f726174696f6e20283132372e302e302e3129000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005265666572656e636520557365722d53706163652053776974636820287377697463683129000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004e6f6e6500000000000000000000000000000000000000000000000000000000'''
    h.runTest(name="stats description", timeout=timeout, events= [
            TestEvent( "send","guest",'bob', stats_request_desc),
            TestEvent( "recv","switch",'switch1', stats_request_desc_after, strict=True),
            TestEvent( "send","switch",'switch1', stats_reply_desc, strict=True),
            TestEvent( "recv","guest",'bob', stats_reply_desc_after),
            ])

    #############################################################
    packet_out_flood = FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
                ffff 0008 0000 0008 fffb 0080 0000 0000
                0001 0000 0000 0002 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
                973f 2b71 c2e4 1b6f bc11 8250'''
            # note, the xid here is a function of the order of the tests;
            #    DO NOT CHANGE test order
    packet_out_flood_aftr = FvRegress.OFVERSION + '''0d 00 68 09 01 00 00 ff ff ff ff ff ff 00 18
                00 00 00 08 00 00 00 80 00 00 00 08 00 02 00 80
                00 00 00 08 00 03 00 80 00 00 00 00 00 01 00 00
                00 00 00 02 08 00 45 00 00 32 00 00 40 00 40 11
                28 68 c0 a8 c8 00 c0 a8 c9 01 00 01 00 00 00 1e
                d7 c3 cd c0 25 1b e6 dc ea 0c 72 6d 97 3f 2b 71
                c2 e4 1b 6f bc 11 82 50'''
    packet_out2_flood = FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
                ffff 0008 0000 0008 fffb 0080 0000 0000
                0002 0000 0000 0001 0800 4500 0032 0000
                4000 4011 2868 c0a8 c800 c0a8 c901 0001
                0000 001e d7c3 cdc0 251b e6dc ea0c 726d
                973f 2b71 c2e4 1b6f bc11 8250'''
            # note, the xid here is a function of the order of the tests;
            #    DO NOT CHANGE test order
    packet_out2_flood_aftr = FvRegress.OFVERSION + '''0d 00 60 06 01 00 00 ff ff ff ff ff ff 00 10
                00 00 00 08 00 01 00 80 00 00 00 08 00 03 00 80
                00 00 00 00 00 02 00 00 00 00 00 01 08 00 45 00
                00 32 00 00 40 00 40 11 28 68 c0 a8 c8 00 c0 a8
                c9 01 00 01 00 00 00 1e d7 c3 cd c0 25 1b e6 dc
                ea 0c 72 6d 97 3f 2b 71 c2 e4 1b 6f bc 11 82 50
                '''

    h.runTest(name="packet_out flood ", timeout=timeout, events= [
            TestEvent( "send","guest",'alice', packet_out_flood),
            TestEvent( "recv","switch",'switch1', packet_out_flood_aftr),
            TestEvent( "send","guest",'bob', packet_out2_flood),
            TestEvent( "recv","switch",'switch1', packet_out2_flood_aftr),
            ])
#########################################
# more tests for this setup HERE
#################################### End Tests
finally:
    if wantPause:
        doPause("start cleanup")
    h.cleanup()

