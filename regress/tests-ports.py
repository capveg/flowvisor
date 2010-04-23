#!/usr/bin/python
from fvregress import *
import string 	# really?  you have to do this?

test="ports"

if len(sys.argv) > 1 :
    wantPause = False
    timeout=60
    h = FvRegress.parseConfig(configDir='flowvisor-conf.d-'+test, alreadyRunning=True, port=int(sys.argv[1]))
else:
    wantPause = False
    timeout=5
    h = FvRegress.parseConfig(configDir='flowvisor-conf.d-'+test)

# start up a flowvisor with 1 switch and two guests

switch_features= FvRegress.OFVERSION + '''06 00 e0 ef be ad de 00 00 00 00 00 00 00 02
                    00 00 00 80 02 00 00 00 00 00 00 00 00 00 00 00
                    00 99 32 30 00 00 00 00 70 6f 72 74 20 31 35 33
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 01 32 31 30 30 30 30 70 6f 72 74 20 31 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 02 32 32 30 30 30 30 70 6f 72 74 20 32 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 03 32 33 30 30 30 30 70 6f 72 74 20 33 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                    00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00'''

if wantPause:
	doPause("before adding switch1")
h.addSwitch(name='switch1',dpid=2, switch_features=switch_features)	


if wantPause:
	doPause("start tests")
#################################### Start Tests
try:
#    # send a packet in from a port that was not listed in switch_features
#	packet_to_g0_p0 =  FvRegress.OFVERSION + '''0a 0052 0000 0000 0000 0101
#				0040 0000 0000 0000 0000 0001 0000 0000
#				0002 0800 4500 0032 0000 0000 40ff f72c
#				c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
#				19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
#				3354 51d5 0036'''
#    # should dynamically grow for alice
#	packet_to_g0_p0 =  FvRegress.OFVERSION + '''0a 0052 0000 0001 0000 0101
#				0040 0001 0000 0000 0000 0002 0000 0000
#				0001 0800 4500 0032 0000 0000 40ff f72c
#				c0a8 0028 c0a8 0128 7a18 586b 1108 97f5
#				19e2 657e 07cc 31c3 11c7 c40c 8b95 5151
#				3354 51d5 0036'''
#    # should not be sent to bob (explicitly lists ports)
#	h.runTest(name="switch1controller packet_in routing - by port", timeout=timeout, events= [
#			TestEvent( "send","switch","switch1", packet=packet_to_g0_p0),
#			TestEvent( "recv","guest","alice", packet=packet_to_g0_p0),
#			TestEvent( "clear?","guest","bob", packet=""),
#			])
	############################################################
	packet_out_pAll = FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
            ffff 0008 0000 0008 fffb 0080 0000 0000
            0001 0000 0000 0002 0800 4500 0032 0000
            4000 4011 2868 c0a8 c800 c0a8 c901 0001
            0000 001e d7c3 cdc0 251b e6dc ea0c 726d
            973f 2b71 c2e4 1b6f bc11 8250'''
    # note, the xid here is a function of the order of the tests;
    #   DO NOT CHANGE test order
	packet_out_p0_aftr_port0 = FvRegress.OFVERSION+ '''0d 00 70 02 01 00 00 ff ff ff ff ff ff 00 20
            00 00 00 08 00 99 00 80 00 00 00 08 00 01 00 80
            00 00 00 08 00 02 00 80 00 00 00 08 00 03 00 80
            00 00 00 00 00 01 00 00 00 00 00 02 08 00 45 00
            00 32 00 00 40 00 40 11 28 68 c0 a8 c8 00 c0 a8
            c9 01 00 01 00 00 00 1e d7 c3 cd c0 25 1b e6 dc
            ea 0c 72 6d 97 3f 2b 71 c2 e4 1b 6f bc 11 82 50'''
	packet_out_pAll_bob = FvRegress.OFVERSION + '''0d 0058 0000 abcd ffff ffff
            ffff 0008 0000 0008 fffb 0080 0000 0000
            0002 0000 0000 0001 0800 4500 0032 0000
            4000 4011 2868 c0a8 c800 c0a8 c901 0001
            0000 001e d7c3 cdc0 251b e6dc ea0c 726d
            973f 2b71 c2e4 1b6f bc11 8250'''
	packet_out_pAll_bob_aftr = FvRegress.OFVERSION+ '''0d 00 70 03 01 00 00 ff ff ff ff ff ff 00 20
            00 00 00 08 00 99 00 80 00 00 00 08 00 01 00 80
            00 00 00 08 00 02 00 80 00 00 00 08 00 03 00 80
            00 00 00 00 00 02 00 00 00 00 00 01 08 00 45 00
            00 32 00 00 40 00 40 11 28 68 c0 a8 c8 00 c0 a8
            c9 01 00 01 00 00 00 1e d7 c3 cd c0 25 1b e6 dc
            ea 0c 72 6d 97 3f 2b 71 c2 e4 1b 6f bc 11 82 50'''
	h.runTest(name="packet_out; valid", timeout=timeout, events= [
            TestEvent( "send","guest","alice", packet=packet_out_pAll),
            TestEvent( "recv","switch","switch1", packet=packet_out_p0_aftr_port0),
            TestEvent( "send","guest","bob", packet=packet_out_pAll_bob),
            TestEvent( "recv","switch","switch1", packet=packet_out_pAll_bob_aftr),
            ])


	############################################################
	packet_out_pInPort = FvRegress.OFVERSION + '''0d 0058 0000 abff ffff ffff
            0001 0008 0000 0008 fffb 0080 0000 0000
            0001 0000 0000 0002 0800 4500 0032 0000
            4000 4011 2868 c0a8 c800 c0a8 c901 0001
            0000 001e d7c3 cdc0 251b e6dc ea0c 726d
            973f 2b71 c2e4 1b6f bc11 8250'''
	packet_out_pInPort_aftr = FvRegress.OFVERSION+ '''0d 00 68 04 01 00 00 ff ff ff ff 00 01 00 18
            00 00 00 08 00 99 00 80 00 00 00 08 00 02 00 80
            00 00 00 08 00 03 00 80 00 00 00 00 00 01 00 00
            00 00 00 02 08 00 45 00 00 32 00 00 40 00 40 11
            28 68 c0 a8 c8 00 c0 a8 c9 01 00 01 00 00 00 1e
            d7 c3 cd c0 25 1b e6 dc ea 0c 72 6d 97 3f 2b 71
            c2 e4 1b 6f bc 11 82 50'''
	h.runTest(name="dont flood in_port; valid", timeout=timeout, events= [
            TestEvent( "send","guest","alice", packet=packet_out_pInPort),
            TestEvent( "recv","switch","switch1", packet=packet_out_pInPort_aftr),
            ])



#########################################
# more tests for this setup HERE
#################################### End Tests
finally:
	if wantPause:
		doPause("start cleanup")
	h.cleanup()

