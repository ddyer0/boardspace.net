#!/bin/csh
#
set echo
# save a monthly copy of the database
# also make monthly alterations
#   remove "deadwood" players who have never played 
# depends on path set for mysql and database

source ~/cgi-bin/include.csh
set sunset=2
set now=`date`
set month=$now[2]"-"$now[6]
set monthname="$month.txt"


cd ~/cgi-bin/
set rname="$hive_rankingdir/Hive-Rankings-$month.html"
./boardspace_rankings.cgi "game=y" "n=99999" "header=Worldwide Hive Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
