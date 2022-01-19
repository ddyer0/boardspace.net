#-------------------------------------------------
# lock.pl
# Copyright (C) 1995 Jonathan A. Lewis
#
# Permission to use, copy, modify, and distribute this include file and its 
# documentation for any purpose without fee is granted
# provided that the above copyright notice appears in all copies.
# 
# This, and the accompanying scripts, is provided "as is" without any express 
# or implied warranty.
#
# Use this script at your own risk.  It's guaranteed to do nothing but
# occupy space...unless your disk crashes, in which case it does nothing
# at all.
#------------------------------------------------


$LOCK_SH = 1;
$LOCK_EX = 2;
$LOCK_NB = 4;
$LOCK_UN = 8;

sub lock {
  #pass in a file handle to be locked
  #flock will wait until it can get a lock
	
    	flock($_[0], $LOCK_EX);
    	# seek to whence just incase we were waiting for a lock after opening
      	seek($_[0], 0, $_[1]);
}

sub unlock {
        flock($_[0], $LOCK_UN);
}
1; #return true        