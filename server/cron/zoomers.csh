#!/bin/csh
#
# check for zoomers and save previous rankings
# this script is designed to be run exactly twice a day
#
~/cron/dtest.csh $0 60
set v = $?
if ( $v == 0 ) then
cd ~/cgi-bin/
/usr/bin/perl zoomers.pl
cd ~/cgi-bin/tlib
/usr/bin/perl set-time-correction.pl
endif


