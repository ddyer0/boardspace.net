#!/bin/csh -f
set echo
pwd
#
set target = $1
set lastrun = $2
#
# check for duplicate scripts.  We look for a "lastrun" file
# and want it to be nonexistant, or to be older than $lastrun seconds
#
#
# default exit code
#
set code = 1
#
#
# first get the lock
#
if (`lockfile -r 1 -l 60 "${target}.lock"`) then
echo "couldn't get lockfile ${target}.lock"
else
#
if (-e ${target}.lastrun) then
 # file exits, make sure it is old
 set runon = `filetest -C "${target}.lastrun"`
 set now = `filetest -C "${target}.lock"`
 @ runon = $runon + $lastrun
 if ( $now < $runon ) then
    echo `date` "${target}.lastrun exists and is too new" >>& cronlog.txt
  set code = 1
 else
  echo `date` "${target}.lastrun exists and is old enough" >>& cronlog.txt
  set code = 0
 endif

else

 echo `date` "${target}.lastrun does not exist, that's ok" >>& cronlog.txt
 set code = 0
endif


rm -rf " ${target}.lastrun"
touch "${target}.lastrun"
rm -rf "${target}.lock"
endif
#
# exit with 1 if error, 0 if ok to proceed
#
exit $code











