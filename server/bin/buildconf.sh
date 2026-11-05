#!/bin/csh

source /home/boardspace/www/cgi-bin/include.csh

if (`lockfile -r 1 -l 60 /tmp/boardspaceserver_startup.lock`) then
echo "couldn't get lockfile /tmp/boardspaceserver_startup.lock"
exit 1
else

echo 'starting server'

echo root is $root

cd $root/www/cgi-bin/tlib

pwd

perl serverconf.pl > $root/bin/dynamic.conf

cd $root/bin

cat static.conf dynamic.conf > BoardSpaceServer.conf

rm -rf /tmp/boardspaceserver_startup.lock

endif

