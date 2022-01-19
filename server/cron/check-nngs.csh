#!/bin/tcsh
#set echo
#
# [2/2006] this version uses gnu netcat, which returns a nonzero status if
# the connection fails.
#
###############################################################
#	050218
# cgknngs:
# check if nngs is still running
# also check if it is responding
#
#	If not responsive: kill it off
#	If not running: start it
#
#	To be run from cron.
#	NB: uses the nc (netcat) program, present in linux
#
#	HTH,
#	AvK
###############################################################
set THE_HOST=localhost
set THE_PORT=6969
set WHAT_TO_SEARCH="nngssrv"
set WHAT_TO_START=${HOME}/bin/Nngs.start

nc -w 10 -z ${THE_HOST} ${THE_PORT}
if($status != 0) then
set psline=`ps x | grep ${WHAT_TO_SEARCH} | grep -v grep `
set thepid=0
if($#thepid > 1) then
 # try to kill the old one
 set thepid=$psline[1]
 echo "kill pid $thepid" >>& nngs_ok_time
 kill ${thepid}
endif

cd ${HOME}/bin
echo "starting ${WHAT_TO_START} " >>& nngs_ok_time
${WHAT_TO_START} &

else
 date >> & nngs_ok_time
endif

