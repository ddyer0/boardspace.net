#!/bin/csh

source /home/boardspace/www/cgi-bin/include.csh

set now=`date`
set suffix = "$now[2]-$now[3]-$now[6]";

if((! -e "$debug_log-$suffix") && -e $debug_log) mv "$debug_log" "$debug_log-$suffix"
if((! -e "$java_error_log-$suffix") && -e $java_error_log) mv "$java_error_log" "$java_error_log-$suffix"

limit coredumpsize u
./BoardSpaceServer BoardSpaceServer.conf
if($status != 0) then
 echo "start failed"
 exit 1
 endif

 echo "started"
 exit 0
