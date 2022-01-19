#!/bin/csh
#set echo
#
# script to watch for low or rapidly declining disk space
#
~/cron/dtest.csh $0 10
source ~/cgi-bin/include.csh
set alarm = 500000
set rate = 100000
#
set space = `df | grep "/dev/xv"`
set space = $space[3]
set oldspace = 0
if(-e oldspace) set oldspace = `cat oldspace`
@ decline = $oldspace - $space
set msg = ""
if($space < $alarm) then
 set msg = "Space on home is $space"
endif

if($decline > $rate) then
 set msg = "Space on home declined from $oldspace to $space"
endif

if("$msg" != "") then
 mail  -s "Disk space low on Boardspace.net"  gamemaster\@boardspace.net << endofline
  $msg
endofline
#else
# mail -s "Disk space ok on Boardspace.net"  gamemaster\@boardspace.net <<EOF
#   all ok, it ran
#EOF
endif






