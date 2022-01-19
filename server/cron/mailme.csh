#!/bin/csh
#
# check for duplicate cron dups
#
~/cron/dtest.csh $0 60
set v = $?
#echo status $v
if ( $v == 0 ) then
 #echo ok to proceed
else
 #echo not ok
endif








