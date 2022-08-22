#!/usr/bin/perl
#
# generate rankings on demand.  Extracted from gs_74a2d.cgi on nov 23 1998 -ddyer
# added current games played statistics 15 apr 99
# added multiplayer support 2/2010
#
#
# optional parameters:
# nitems=nn
#
use CGI qw(:standard);
use Mysql;
use Debug;
use Time::Local;
use strict;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";


{ my $zoom_threshold=50;
  my $master_zoom_threshold=100;
  my $rank_threshold=825;
  my $mailto=$'supervisor_email;
  my $dbh = &connect();
  my $target_date = time()-60*60*12;
  my $sth2 = &query($dbh,"SELECT date+0,message,type FROM messages WHERE UNIX_TIMESTAMP(date)>$target_date ORDER BY date");
  my $num2 = &numRows($sth2);
  my $sth = &query($dbh,"SELECT player_name,value,prev_value_1,prev_value_2,variation,ranking.uid from ranking "
       . " LEFT JOIN players ON players.uid=ranking.uid "
		   . " WHERE (value>$rank_threshold) AND (prev_value_2>0) AND (ABS(value-prev_value_1)=$zoom_threshold OR ABS(value-prev_value_2)>=$zoom_threshold)"
		   . " ORDER BY value");
  my $num = &numRows($sth);
  my $sth3 = &query($dbh,"select player_name,e_mail from ranking "
    . " LEFT JOIN players ON players.uid=ranking.uid where max_rank>=2000 and players.is_master='n' ");
  my $num3 = &numRows($sth3);

  my $sth4 = &query($dbh,"select count(changed),language from translation where changed>date_sub(current_date(),interval 1 day) group by language"); 
  my $num4 = &numRows($sth4);
  
  if( ($num>0) || ($num2>0) || ($num3>0) || ($num4>0) )
  {
  my $sumtrans = 0;
  my $sumtransstr = "";
  
  while($num4-->0)
  {	my ($n,$lang) = &nextArrayRow($sth4);
    $sumtrans += $n;
    $sumtransstr .=" $n $lang";
  }
  
  my $trans = ($sumtrans>0) ? " $sumtrans translations" : "";
  my $master = ($num3>0)?"$num3 new potential masters":"";
  my $alert = ($num2>0)?" $num2 alerts":"";
  my $zoom = ($num>0) ? " $num players zoom in the rankings":"";
  open( SENDMAIL, "| $'sendmail $mailto" );
  print SENDMAIL "subject: boardspace.net:$master$alert$zoom$trans\n";
  if($sumtrans>0) { print SENDMAIL "\ntranslations: $sumtransstr\n"; }
  if($num3>0)
  { 
    print SENDMAIL "\nNew Master Candidates:";
    while ($num3>0)
    {
	$num3--;
	my ($player,$email) = $sth3->fetchrow();
	print SENDMAIL " $player($email) ";
    }
    print SENDMAIL "\n\n";
  }
  while($num2>0)
  { $num2--;
    my ($date,$message) = $sth2->fetchrow();
    my $yr = substr($date,0,4);
    my $mo = substr($date,4,2);
    my $da = substr($date,6,2);
    my $hr = substr($date,8,2);
    my $mn = substr($date,10,2);
    my $msg = "$da-$mo-$yr $hr:$mn: $message";
    print SENDMAIL "$msg\n";
  }
  while ($num>0)
  { $num--;
    my ($id,$r1,$r2,$r3,$var,$uid) = $sth->fetchrow();
	my $qid = $dbh->quote($uid);
	my $qvar = $dbh->quote($var);
    print SENDMAIL "$id $var\t$r1 from $r2 or $r3\n";

	# get the names of the most recent games
	my $qz = "(select gamename,gmtdate from zertz_gamerecord "
		. " where (gmtdate > date_sub(curdate(),interval 1 day))"
		. "    and variation=$qvar" 
		. "    and (player1=$qid or player2=$qid))"
		. " UNION (select gamename,gmtdate from mp_gamerecord "
		. " where (gmtdate > date_sub(curdate(),interval 1 day))"
		. "    and variation=$qvar" 
		. "    and (player1=$qid or player2=$qid or player3=$qid or player4=$qid or player5=$qid or player6=$qid))"
		. " order by gmtdate desc limit 10";
	my $sthz = &query($dbh,$qz);
	my $nz = &numRows($sthz);
	while($nz>0)
	{
	$nz--;
	my ($gm) = &nextArrayRow($sthz);
	print SENDMAIL "\t\t$gm\n";
	}
	&finishQuery($sthz);
	#the old version that grepped the log
	#my $msg = `grep -i $id $'game_completed_log | tail`;
    #print SENDMAIL "$msg\n";

  }
  close SENDMAIL;
  
  }
  &finishQuery($sth4);
  &finishQuery($sth3);
  &finishQuery($sth2);
  &finishQuery($sth);
  # cleanup messages more than a year old
  &commandQuery($dbh,"DELETE from messages WHERE (UNIX_TIMESTAMP(date)+60*60*24*365)<UNIX_TIMESTAMP()");
  &commandQuery($dbh,"UPDATE ranking SET prev_value_2=prev_value_1, prev_value_1=value");

  &disconnect($dbh);
}
