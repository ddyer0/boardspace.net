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
	
__dStart( "$::debug_log",$ENV{'SCRIPT_NAME'});;

sub checkServer()
{ my ($u) = @_;
  my $session=param('session');
  my $key = param('key');
  my $port = param('sock');
  my $alscored = (param('first') eq 'true');
  if($port<2240) { $port=$::game_server_port}
  if($alscored && !&check_server($port,$session,$key,$u,$u))
      {
       __dm( __LINE__." scoring rejected: \"$::reject_line\" $ENV{'QUERY_STRING'}" );
       print "\n** Ranking update was rejected by the server **\n";
       return 0;
      }
  return 1;
}
# 
# unused at the moment
#
sub getPname()
{
	my ($dbh,$user) = @_;
	my $q = "select player_name from players where uid=$user";
	my $sth = &query($dbh,$q);
	my $pname;
	if(&numRows($sth)>0)
	{
	($pname) = &nextArrayRow($sth);
	}
	&finishQuery($sth);
	return $pname;
}
sub timestr()
{
  my ($t) = @_;
  my $min = int($t/60);
  my $sec = int($t - $min*60);
  $sec = "0" . $sec if ( length($sec) == 1 );
  return("$min:$sec");
}

sub doit()
{
  $ok = &useCombinedParams($::tea_key,1);
  if($ok)
  {
  __d("from $ENV{'REMOTE_ADDR'} $ENV{'QUERY_STRING'}" );
 
  my $dbh = &connect();              # connect to local mysqld
  if($dbh)
  {
  my $serverless = &param('serverless') eq 'true';
  my $game0 = param('game');
  my $game = &gamecode_to_gamename($dbh,$game0);
  my $fname =$dbh->quote(&param('fname'));		# game saved as
  my $error = 0;
  #
  # if offline is true, the p1 parameter is a device id, not a player uid
  #
  my $offline = &param("offline") eq 'true';
  my $rawuser = $dbh->quote(&param("u1"));
  my $deviceid = $offline ? $rawuser : "null";
  my $u1 = $offline ? "null" : $rawuser;

  my $t1 = $dbh->quote(&param("t1"));		# time for player
  my $sl0 = param("s1");
  my $pname = $dbh->quote(&param("p1"));
  my $s1 = $dbh->quote($sl0);   		# score for player
  my $unranked = param("nr1") eq 'true';
  my $puzzleid = $dbh->quote(param("puzzleid"));	# the actual puzzle text
  my $puzzledate = $dbh->quote(param("puzzledate"));	# epoch date for this puzzle
  my $var0 = param("variation");
  my $variation = $dbh->quote($var0);		# puzzle type
  my $mode = (param("hard") eq ('true')) ? "'hard'" : "'normal'";		# if hard puzzle group
  my $now = $dbh->quote(&ctime());
  if($unranked || $serverless || $offline || &checkServer(param('u1')))
  {	my $nr =0;
	my $selector = $offline ? "deviceid=$deviceid" : "player1=$u1";
 	my $q = "select gmtdate from sp_record where $selector and puzzleid=$puzzleid and mode=$mode";
	my $sth = &query($dbh,$q);
  	my $nr = &numRows($sth);
	#print "q: $q\n";
	if($nr>0)
	{
	my ($gmtdate) = &nextArrayRow($sth);
	&finishQuery($sth);
 	print "You already solved this puzzle on $gmtdate\n";
	}
	else
	{
	&finishQuery($sth);
	if(!$unranked || $offline)
		{
  		my $q = "insert into sp_record set player1=$u1,deviceid=$deviceid,nickname=$pname,"
			. "time1=$t1,score1=$s1,gamename=$fname,puzzleid=$puzzleid,puzzledate=$puzzledate,variation=$variation,mode=$mode,"
       	  		. "gmtdate=$now";
  		#print "Q: $q\n";
    		&commandQuery($dbh,$q);
    		&make_log("$pname\t$sl0\t$var0\t$fname");		
		}
	}	

  }

  &disconnect($dbh);

  } # end of if ($dbh)
 } # end of if ($ok)
  
} #end of &doit() 

if(&param()) { doit(); }
	else {
  	__d( "No update parameters parameter found..." );
	}
 
if(!$ok) 
	{  print "score update failed\n";
  	}

__dEnd( "end!" );



