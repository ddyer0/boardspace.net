#!/bin/sh
# restart the two test servers used by compare.py (C on 3001/3002/3003, Java on 4001/4002/4003)
B=/tmp/mt
pkill -f "cmigs[_a-z]* $B" ; pkill -f "migs.jar $B"; sleep 1
for d in c j; do rm -rf $B/$d; done
"$(dirname "$0")/mkconf.sh" $B/c 3001 3002 ${1:-0} ${2:-0} 3003
"$(dirname "$0")/mkconf.sh" $B/j 4001 4002 ${1:-0} ${2:-0} 4003
# the C server has no SO_REUSEADDR, so retry until TIME_WAIT connections on its port clear
( cd $B/c; for i in $(seq 1 40); do ${CMIGS:-/tmp/cmigs} $B/c/test.conf > $B/c/out.txt 2>&1 && break; grep -q "Failed bind" $B/c/out.txt || break; sleep 3; done ) &
cd $B/j && setsid java -jar /home/claude/jmigs/migs.jar $B/j/test.conf > $B/j/out.txt 2>&1 &
for i in $(seq 1 60); do grep -q "main process started" $B/c/main.log 2>/dev/null && break; sleep 2; done
sleep 1
