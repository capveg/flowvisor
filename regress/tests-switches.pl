#!/usr/bin/python
from fvregress import *
import string 	# really?  you have to do this?
import sys

if len(sys.argv) > 1 :
	wantPause = True
	timeout=9999999
	valgrindArgs= []
else:
	wantPause = False
	timeout=5
	valgrindArgs= None

h = FvRegress.parseConfig(configDir='flowvisor-conf.d-switches', valgrind=valgrindArgs)

if wantPause:
	doPause("start tests")
#################################### Start Tests
try:

    feature_request =   FvRegress.OFVERSION + '05 0008 2d47 c5eb'
    feature_request_after = FvRegress.OFVERSION + '05 0008 0101 0000'
    h.runTest(name="feature_request from alice to switch1",timeout=timeout,  events= [
            TestEvent( "send","guest",'alice', feature_request),
            TestEvent( "recv","switch",'switch1', feature_request_after, strict=True),
            ])

    feature_request =   FvRegress.OFVERSION + '05 0008 2d47 c5eb'
    feature_request_after = FvRegress.OFVERSION + '05 0008 0101 0000'
    h.runTest(name="feature_request from bob to switch2",timeout=timeout,  events= [
            TestEvent( "send","guest",'bob', feature_request,switch2),
            TestEvent( "recv","switch",'switch2', feature_request_after, strict=True),
            ])


#########################################
# more tests for this setup HERE
#################################### End Tests
finally:
	if wantPause:
		doPause("start cleanup")
	h.cleanup()

