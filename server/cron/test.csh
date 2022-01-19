#!/bin/csh
#
#set echo
# save a monthly copy of the database
# also make monthly alterations
#   remove "deadwood" players who have never played 
# depends on path set for mysql and database
#
~/cron/dtest.csh $0 600
set v = $?
#
# dtest is a kludge to prevent scripts running twice
#
if ( $v == 0 ) then
source ~/cgi-bin/include.csh
set sunset=2
set now=`date`
set month=$now[2]"-"$now[6]
set monthname="$month.txt"


set rname="$rankingdir/Zertz-Rankings-$month.html"
cd ~/cgi-bin/
./boardspace_rankings.cgi "game=z" "n=99999" "header=Worldwide Zertz Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#
set rname="$rankingdir/Zertz+11-Rankings-$month.html"
./boardspace_rankings.cgi "game=z11" "n=99999" "header=Worldwide Zertz+11 Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#
#
set rname="$rankingdir/Zertz+24-Rankings-$month.html"
./boardspace_rankings.cgi "game=z24" "n=99999" "header=Worldwide Zertz+24 Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$loa_rankingdir/LOA-Rankings-$month.html"
./boardspace_rankings.cgi "game=l" "n=99999" "header=Worldwide Lines of Action Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$loa_rankingdir/LOAPS-Rankings-$month.html"
./boardspace_rankings.cgi "game=lp" "n=99999" "header=Worldwide LOAPS Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#


set rname="$plateau_rankingdir/Plateau-Rankings-$month.html"
./boardspace_rankings.cgi "game=p" "n=99999" "header=Worldwide Plateau Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$yinsh_rankingdir/Yinsh-Rankings-$month.html"
./boardspace_rankings.cgi "game=y" "n=99999" "header=Worldwide Yinsh Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$dvonn_rankingdir/Dvonn-Rankings-$month.html"
./boardspace_rankings.cgi "game=d" "n=99999" "header=Worldwide Dvonn Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$gipf_rankingdir/Gipf-Rankings-$month.html"
./boardspace_rankings.cgi "game=g" "n=99999" "header=Worldwide Gipf Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$tamsk_rankingdir/Tamsk-Rankings-$month.html"
./boardspace_rankings.cgi "game=t" "n=99999" "header=Worldwide Tamsk Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$hex_rankingdir/Hex-Rankings-$month.html"
./boardspace_rankings.cgi "game=h" "n=99999" "header=Worldwide Hex Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#


set rname="$trax_rankingdir/Trax-Rankings-$month.html"
./boardspace_rankings.cgi "game=tr" "n=99999" "header=Worldwide Trax Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#


set rname="$punct_rankingdir/Punct-Rankings-$month.html"
./boardspace_rankings.cgi "game=pt" "n=99999" "header=Worldwide Punct Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$gobbletm_rankingdir/GobbletM-Rankings-$month.html"
./boardspace_rankings.cgi "game=gm" "n=99999" "header=Worldwide GobbletM Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$gobblet_rankingdir/Gobblet-Rankings-$month.html"
./boardspace_rankings.cgi "game=gb" "n=99999" "header=Worldwide Gobblet Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname
#

set rname="$hive_rankingdir/Hive-Rankings-$month.html"
./boardspace_rankings.cgi "game=hv" "n=99999" "header=Worldwide Hive Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname

set rname="$exxit_rankingdir/Exxit-Rankings-$month.html"
./boardspace_rankings.cgi "game=ex" "n=99999" "header=Worldwide Exxit Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname

set rname="$tablut_rankingdir/Tablut-Rankings-$month.html"
./boardspace_rankings.cgi "game=tb" "n=99999" "header=Worldwide Tablut Rankings as of $month" > tempfile
sed 1D tempfile > $rname
rm tempfile
set xname=$rname:t
if(-e $web_mirror/$xname) rm $web_mirror/$xname
ln $rname $web_mirror/$xname


mysql --password=$db_password --user=$db_user $database << UPDATESTEPS
DELETE FROM players WHERE last_logon < (UNIX_TIMESTAMP()-(60*60*$sunset*30*24)) AND locked!='y' AND (status='unconfirmed' OR  games_played=0 <= 0)\g
UPDATESTEPS
#
#
# save the map data
#
set rname2="$rankingdir/PlayerMap-$month.txt"
cd ~/cgi-bin/tlib
./gs_locations.cgi "header=Tantrix Player Map as of $month" > tempfile
#remove content_type line
sed 1D tempfile > $rname2
rm tempfile
#save for mirroring
set mname=$rname2:t
if(-e $web_mirror/$mname) rm $web_mirror/$mname
ln $rname2 $web_mirror/$mname

# end or master if for the whole file
endif
endif
