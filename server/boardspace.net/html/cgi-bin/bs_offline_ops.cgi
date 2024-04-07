#!/usr/bin/perl
#
#
# logon     Validate user's Player Name and password CGI program!
#
# major revision 1/2013 for java web start. The general philosophy is to
# keep exactly the same logic and flow, except that the various outcomes
# get printed in special ways for java web start.
#
# revision 11/2020 to add the option of passing all parameters as an
# encrypted package, and also to remove the obsolete login cookies
# and browser-based login, which is gone forever with java applets
# when client version 4.82 is obsolete, we should also change the
# insist on the packaged/encrypted login to prevent a security downgrade attack.
#
# sample param 
# params=rRPQ4eBxj8JC5Y2RPMNc20mquUwzMwpgWfKHQNy8zaUYS/Jwk39prylwj7eeqrmW4/jLJepgpsSgo253pLzlxjVkFbsM9oo/Z2vkV7baOcffQYJ6xP57rOmDaoEatVYgO+5AURcUloMczIB9WFQHS9MCWpJ3a64Q0lQKGj7UvLi+T02fhvG2N19vxjraGRAIq9rVS+RCaOMulfxu
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use HTML::Entities;
use URI::Escape;

use Mysql;
use Debug;
use Socket;
use strict;
require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/getLocation.pl";
require "tlib/lock.pl";
require "tlib/top_players.pl";
require "tlib/favorite-games.pl";
require "tlib/show-recent.pl";
require "tlib/messageboard.pl";
require "tlib/ordinal-rankings.pl";
require "tlib/params.pl";


sub init {
	$| = 1;				# force writes
}

sub make_log {
	my ( $msg ) = @_;
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
	open( F_OUT, ">>$'server_logon_log" );
 	printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] %s\n", 1900+$year,$mon+1,$mday,$hour,$min,$sec,$msg;
        close F_OUT;
}
#
# check the player name and password
#
sub logon()
{ 
	my ($dbh, $pname, $clearpasswd) = @_;
 	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($clearpasswd);

	my $isok = 0;
  	my $q = "SELECT uid FROM players WHERE player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid)) AND status='ok' and is_robot is null";
	my $sth= &query($dbh,$q);
	my $nr = &numRows($sth);
	my $uid = 0;
	if($nr == 1)
	{
	($uid) = &nextArrayRow($sth);
	}
	&finishQuery($sth);

	return $uid;

}
#
# check that a player name exists, and is suitable to be an opponent
#
sub checkplayer()
{ 
	my ($dbh, $pname) = @_;
 	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $slashpname = $dbh->quote($pname);

	my $isok = 0;
  	my $q = "SELECT uid FROM players WHERE player_name=$slashpname AND status='ok' and is_robot is null";
	my $sth= &query($dbh,$q);
	my $nr = &numRows($sth);
	my $uid = 0;
	if($nr == 1)
	{
	($uid) = &nextArrayRow($sth);
	}
	&finishQuery($sth);

	return "uid $uid\n";

}
sub creategame()
{	my ($dbh, $pname) = @_;
	my $password = &param('password');

	my $owner = &param('owner');

	my $qowner = $dbh->quote($owner);

	my $allowother = &param('allowother');
	my $qallow = $dbh->quote($allowother);

	my $variation = &param('variation');
	my $qvar = $dbh->quote($variation);

	my $invitedplayers = &param('invitedplayers');
	my $qinvite = $dbh->quote($invitedplayers);

	# parameters for the body
	my $comments = &param('comments');
	my $qcomments = $dbh->quote($comments);

	my $firstplayer = &param('firstplayer');
	my $qfirst = $dbh->quote($firstplayer);

	my $speed = &param('speed');
	my $qspeed = $dbh->quote($speed);

	my $body = "comments $qcomments\n"
			. "firstplayer $qfirst\n"
			. "speed $qspeed\n";
	my $qbody = $dbh->quote($body);

	my $uid = &logon($dbh,$pname,$password);
	my $gid = 0;
	# verify that the owner is the same as the login credentials
	if(($uid eq $owner) && $owner>0)
	{
	my $q = "insert into offlinegame set owner=$qowner"
			. ",allowotherplayers=$qallow"
			. ",invitedplayers=$qinvite"
			. ",body=$qbody"
			. ",variation=$qvar";

	my $sth = &commandQuery($dbh,$q);
	$gid = &last_insert_id($sth);
	&finishQuery($sth);
	}
	return "gameuid $gid\n";
}
#
# the main program starts here
# --------------------------------------------
#
&init();
print header;
# print start_html('Logon');
if( param() ) 
{	&logForm("offlineops");
    #__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
	# return true if we're not using combined params
	# or if the combined params were parsed and validated
	my $ok = &useCombinedParams($'tea_key);
	if($ok && checkChecksumVersion())
	{
	my $ip = $ENV{'REMOTE_ADDR'};
	my $dbh = &connect();
	if($dbh && (&allow_ip_access($dbh,$ip)>=0))
	{
	my $tagname = param('tagname');
 	 $tagname = &despace(substr($tagname,0,25));
	my $pname = param('pname');
	 $pname = &despace(substr($pname,0,25));
	my $version = param('version');
	my $passwd = param('password');
	$passwd = &despace(substr($passwd,0,25));
	my $msg = "";
	__d("user $pname\n");
	#__d("pass $passwd");
	if('1' eq $version)
		{
		if('login' eq $tagname) 
			{ my $uid = &logon($dbh,$pname,$passwd); 
			  $msg = "uid $uid\n";
			}
		elsif('checkname' eq $tagname) { $msg = &checkplayer($dbh,$pname); }
		elsif('creategame' eq $tagname) { $msg = &creategame($dbh,$pname); }
		else { $msg = "error \"undefined request: $tagname\"" ; }
		#print "$val<p>";
	
		&printResult("Ok",$msg);
		}
	else {
		&printResult("Error","bad version, not 1");
	}
	}
	}
}
