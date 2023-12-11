#!/usr/bin/perl
#
# record results of 1-player game (ie; crosswordle)
#
#CREATE TABLE `sp_record` (
#  `player1` int(11) NOT NULL,
#  `variation` enum('crosswordle_55') NOT NULL,
#  `score1` int(11) NOT NULL,
#  `gamename` char(64) NOT NULL,
#  `puzzleid` varchar(45) NOT NULL,
#  `puzzledate` int(11) NOT NULL,
#  `gmtdate` datetime NOT NULL,
#  `time1` int(11) NOT NULL,
#  `mode` enum('normal','hard') NOT NULL,
#  `uid` int(11) NOT NULL AUTO_INCREMENT,
#  `deviceid` char(16),
#  `nickname` char(16),
#  PRIMARY KEY (`uid`)
#) ENGINE=InnoDB DEFAULT CHARSET=utf8;
#

use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use IO::File;

require "include.pl";
require "tlib/common.pl";
require "tlib/ranking.pl";
require "tlib/gs_db.pl";
require "tlib/params.pl";

$| = 1;                         # force writes

print header;
my $ok = 0;
	
__dStart( "$'debug_log",$ENV{'SCRIPT_NAME'});;

sub timestr()
{
  my ($t) = @_;
  my $min = int($t/60);
  my $sec = int($t - $min*60);
  $sec = "0" . $sec if ( length($sec) == 1 );
  return("$min:$sec");
}

sub personalSummary()
{
    my ($dbh,$offline,$u1,$variation,$mode) = @_;
    my $selector = $offline ? "deviceid=$u1" : "player1=$u1" ;
    my $counter = $offline ? "count(deviceid)" : "count(uid)";
    my $q = "select $counter,sum(time1),sum(score1) from sp_record where $selector and variation=$variation and mode=$mode";
    my $sth = &query($dbh,$q);
    my ($count,$sumtime,$sumscore) = &nextArrayRow($sth);
    &finishQuery($sth);
    my $ts = $count==0 ? 0 : &timestr($sumtime/$count);    

    print "personalsolved $count personaltime $ts\n";

    my $q = "select count(score1),score1 from sp_record where player1=$u1 and variation=$variation and mode=$mode and score1>0 group by score1 order by score1 asc";
    my $sth = &query($dbh,$q);
    my $nr = &numRows($sth);
    my $last = 3;
    my $restcount = 0;


    while($nr-- > 0)
    {
    my ($lcount,$score) = &nextArrayRow($sth);
    while($last<$score)
    {   
	print "count$last 0\n"; 
        $last++;
    }
    my $pc = int(($lcount*100)/$count);
    if($score<15)
    {
     print "count$score $lcount\n";
    }
    else { $restcount += $lcount; } 
    $last = $score+1;
    }
    &finishQuery($sth);
    if($restcount>0)
    {
	print "count15+    $restcount\n";
    }
    print "end true\n";     
}
sub puzzleSummary()
{   my ($dbh,$offline,$puzzleid) = @_;

    my $q = "select count(score1),sum(score1),sum(time1) from sp_record where puzzleid=$puzzleid and score1>0";
    my $sth = &query($dbh,$q);
    my ($count,$sum,$sumtime) = &nextArrayRow($sth);
    &finishQuery($sth);
    my $ts = $count==0 ? 0 : &timestr($sumtime/$count);
    my $last = 3;
    my $restcount = 0;

    my $q = "select count(score1),score1 from sp_record where puzzleid=$puzzleid and score1>0 group by score1 order by score1 asc";
    my $sth = &query($dbh,$q);
    my $nr = &numRows($sth);
    print "everyonesolved $count everyonetime $ts\n";
    while($nr-- > 0)
    {
    my ($lcount,$score) = &nextArrayRow($sth);
    while($last<$score)
    {   
	print "count$last 0\n"; 
	$last++;
    }
    if($score<15)
    {
     print "count$score $lcount\n";
    }
    else { $restcount += $lcount; } 
    $last = $score+1;
    }
    &finishQuery($sth);
    if($restcount>0)
    {
	print "count15+    $restcount\n";
    }
    print "end true\n";
}
sub getUid()
{
    my ($dbh,$p) = @_;
    my $q = "select uid from players where player_name=$p";
    my $sth = &query($dbh,$q);
    my ($u) = &nextArrayRow($sth);
    &finishQuery($sth);
    return $u;
}

sub doit()
{
  $ok = &useCombinedParams($'tea_key,1);
  if($ok)
  {
  __d("from $ENV{'REMOTE_ADDR'} $ENV{'QUERY_STRING'}" );
 
  my $dbh = &connect();              # connect to local mysqld
  if($dbh)
  {
  my $p1 = $dbh->quote(param("p1"));
  my $u1 = &param("u1");		# uid
  my $variation = $dbh->quote(param("variation"));		# puzzle type
  my $puzzleid = $dbh->quote(param("puzzleid"));	# the actual puzzle text
  my $mode = (param("hard") eq ('true')) ? "'hard'" : "'normal'";		# if hard puzzle group
  my $offline = &param('offline') eq 'true';
 
  if($u1 eq 'null')
  { $u1 = &getUid($dbh,$p1);
  }
  $u1 = $dbh->quote($u1);

  my $q = "select uid,gmtdate,time1,score1 from sp_record where player1=$u1 and puzzleid=$puzzleid and mode=$mode";
  my $sth = &query($dbh,$q);
  my $nr = &numRows($sth);
  print "version 1\n";
  if($nr>0)
  {
      my ($uid,$gmtdate,$time,$score) = &nextArrayRow($sth);
      my $date = substr($gmtdate,0,10);
      my $ts = &timestr($time);
      print "solveddate $date solvedtime $ts solvedscore $score\n";
  }
  &finishQuery($sth);

  &puzzleSummary($dbh,$offline,$puzzleid);
  &personalSummary($dbh,$offline,$u1,$variation,$mode);

  &disconnect($dbh);

  } # end of if ($dbh)
 } # end of if ($ok)
  
} #end of &doit() 

if(&param()) { doit(); }
	else {
  	__d( "No update parameters parameter found..." );
	}
 
if(!$ok) 
	{  print "puzzle stats failed\n";
  	}

__dEnd( "end!" );



