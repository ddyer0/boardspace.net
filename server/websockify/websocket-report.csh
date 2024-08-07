#!/bin/csh
#
# this script has to run as root, not as boardspa.  Put it in the root cron jobs
#
cd ~boardspa/logs
grep websock /var/log/messages | tail -20 >> websocket.log
ps -eal | grep websock >> websocket.log
