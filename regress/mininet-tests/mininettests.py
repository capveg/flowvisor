#!/usr/bin/env python
import unittest
import sys
import subprocess
from topology import *

if sys.version < 2.7 :
    print "These unittests require python 2.7 for the unittest.discover() to work"
    sys.exit(1)

class MininetTestsException(BaseException):
    pass

class MininetTests(unittest.TestCase):
    MN_bin = 'mn'
    mn_okay = False
    MN_stdout = 'mininet.stdout'
    MN_stderr = 'mininet.stderr'
    MN_switch_list = [ 'user', 'ovsk']


    def setUp(self):
        if MininetTests.mn_okay:
            return
        MininetTests.mn_okay = self.hasWorkingMininet()
        if not MininetTests.mn_okay:
            self.noWorkingMininet()

    def noWorkingMininet(self):
        raise MininetTestsException("\n\n\nMininet not working on this system: see %s and %s for details" % (
            MininetTests.MN_stdout, MininetTests.MN_stderr))

    def hasWorkingMininet(self):
        """ Probes different mn params and find a combo that works
            on this system """
        self.switch = None
        log_stdout = open(MininetTests.MN_stdout, "a")
        log_stderr = open(MininetTests.MN_stderr, "a")
        for switch in MininetTests.MN_switch_list :
            retcode = subprocess.call(["sudo", 
                            MininetTests.MN_bin, 
                            "--switch=%s"% switch,
                            "--test=pingall"],
                            stdout=log_stdout,
                            stderr=log_stderr,
                            )
            if retcode == 0 :
                self.switch = switch
                break
        log_stdout.close()
        log_stderr.close()
        return self.switch is not None

    def spawnMininet(self):
        pass

class MininetSelfTest(MininetTests):
    #@unittest.skipUnless(MininetTests.mn_okay, "mininet not configured on this system")
    def runTest(self):
        pass


        
if __name__ == '__main__':
    baseTest = MininetSelfTest()
    if baseTest.hasWorkingMininet():
        unittest.main()        
    else:
        baseTest.noWorkingMininet()
