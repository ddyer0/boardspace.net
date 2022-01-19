#!/bin/csh
#
#set echo
setenv now `date`
mail -s "mail test for real-me.net" test@real-me.net <<EOF
test from boardspace
sent $now
EOF
