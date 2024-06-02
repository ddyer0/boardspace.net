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
require "push/silentpush.pl";

sub init {
	$| = 1;				# force writes
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
	if($uid>0)
	{
	my $last_logon = time();	# num seconds since Jan 1, 1970
	my $qtime = $dbh->quote($last_logon);
	my $quid = $dbh->quote($uid);
	# update last login so player_analysis will notice games etc.
	&commandQuery($dbh,"Update players set last_logon=$qtime where uid=$quid limit 1");
	}
	return $uid;

}
#
# check that a player name exists, and is suitable to be an opponent
#
sub getPlayerUid()
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
sub recordgame()
{
	my ($dbh,$pname,$password) = @_;
	my $uid = &logon($dbh,$pname,$password);
	if($uid>0)
	{
	my $dirnum = &param('directory');
	my $qdirnum = $dbh->quote($dirnum);
	my $gamename = &param('gamename');
	my $body = &decode64(&param('body'));
	my $q = "select directory from variation where directory_index = $qdirnum";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr==1)
		{
		my ($dir) = &nextArrayRow($sth);
		my $root = $ENV{'document_root'};
		my $fullname = $root . $dir . $gamename . ".sgf";
		if(open (FH, "> $fullname"))
			{
			print FH $body;
			close(FH);
			my $uid = &param('gameuid');
			my $quid = $dbh->quote($uid);
			my $comments = &param('comments');
			my $qcomments = $dbh->quote($comments);
			if($uid)
			{
			my $q = "update offlinegame set comments=$qcomments where gameuid=$quid limit 1";
			&commandQuery($dbh,$q);
			}
			return "savedas \"$dir$gamename\"";
			}
			else 
			{
			return "error problem writing $fullname";
			}
		}
		else
		{
		return "error bad directory number";
		}
	}
	else
	{
	return "error bad player+password";
	}
}
#
# create or updare game
#
sub creategame()
{	my ($dbh, $pname, $password) = @_;

	my $uid = &logon($dbh,$pname,$password);

	if($uid>0)
	{
	my $owner = &param('owner');
	my $qowner = $dbh->quote($owner);

	my $gameuid = &param('gameuid');		#if gameuid is supplied, this is an update
	my $qgameuid = $dbh->quote($gameuid);

	my $status = &param('status');
	my $qstatus = $dbh->quote($status);

	my $allow = &param('allowotherplayers');
	my $qallow = $dbh->quote($allow);

	my $var = &param('variation');
	my $qvar = $dbh->quote($var);

	my $mode = &param('playmode');
	my $qmode = $dbh->quote($mode);

	my $invite = &param('invitedplayers');
	my $qinvite = $dbh->quote($invite);

	my $accept = &param('acceptedplayers');
	my $qaccept = $dbh->quote($accept);

	# parameters for the body
	my $comments = &param('comments');
	my $qcomments = $dbh->quote($comments);

	my $first = &param('firstplayer');
	my $qfirst = $dbh->quote($first);

	my $speed = &param('speed');
	my $qspeed = $dbh->quote($speed);

	my $body = &param('body');
	my $qbody = $dbh->quote($body);

	my $chat = &param('chat');
	my $qchat = $dbh->quote($chat);

	my $whoseturn = &param('whoseturn');
	my $qwhoseturn = $dbh->quote($whoseturn);

	my $nag = &param('nag');
	my $qnag = $dbh->quote($nag);
	my $nagtime = int(&param('nagtime'));
	my $nextnag = ($nag and ($nagtime>0) && ($nagtime<9)) ? "DATE_ADD(utc_timestamp(),INTERVAL $nagtime DAY)" : "NULL";
	my $gid = 0;
	# verify that the owner is the same as the login credentials
	if(($gameuid>0) || ($uid eq $owner))
	{
	my $preamble = $gameuid>0 
			? "update offlinegame set last=utc_timestamp()" 
			: "insert into offlinegame set last=utc_timestamp(),created=utc_timestamp()";
	my $postamble = $gameuid>0 
			? " where gameuid= $qgameuid"
			: "";
	my $q = "$preamble "
			. ($owner ? ",owner=$qowner" : "")
			. ($allow ? ",allowotherplayers=$qallow" : "")
			. ($invite ? ",invitedplayers=$qinvite" : "")
			. ($accept ? ",acceptedplayers=$qaccept" : "")
			. ($mode ? ",playmode=$qmode" : "")
			. ($comments ? ",comments=$qcomments" : "")
			. ($first ? ",firstplayer=$qfirst" : "")
			. ($speed ? ",speed=$qspeed" : "")
			. ($status ? ",status=$qstatus" : "")
			. ($whoseturn ? ",whoseturn=$qwhoseturn" : "")
			. ($body ? ",body=$qbody" : "")
			. ($chat ? ",chat=$qchat" : "")
			. ($var ? ",variation=$qvar" : "")
			. ($nag ? ",nag=$qnag,nagtime=$nextnag" : "")
			. $postamble;
	#print "\nQ: $q\n";
	my $sth = &commandQuery($dbh,$q);
	$gid = &last_insert_id($sth);
	&finishQuery($sth);
	}
	return $gameuid>0 
		? "gameuid $gameuid\n" 
		: "gameuid $gid\n";
	}
	else
	{
	return "error bad player+password";
	}
}
#
# users is a list of user ids separated by |
# result os a list of user + name pairs
#
sub getusers()
{	my ($dbh,$users) = @_;
	my @us = split /\|/,$users;
	my $ids = "where ";
	my $comma = "";
	foreach my $id (@us)
	{my $qid = $dbh->quote($id);
	 $ids .= "${comma}uid=$qid ";
 	 $comma = " or ";
	}
	my $q = "select player_name,uid from players $ids";
	#print "Q: $q\n";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	my $msg = "";
	while($n-- > 0)
	{	
	my ($u,$n) = &nextArrayRow($sth);
	$msg .= "$u \"$n\"\n";
	}
	&finishQuery($sth);
	#print "$msg<p>";
	return $msg;
}

sub getbody()
{
	my ($dbh) = @_;
	my $uid = &param('gameuid');
	my $quid = $dbh->quote($uid);

	my $q = "select body,chat from offlinegame where gameuid=$quid";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr==1)
	{
	my ($body,$chat) = &nextArrayRow($sth);
	return "body \"$body\"\nchat \"$chat\"\n";
	}
	else
	{
	return "error game $quid not found";
	}
}
#
# get info for games in progress
# return format is <field> <value>"
# repeated as necessary, but gameuid starts each block
#
sub getgameinfo()
{	my ($dbh, $owner, $invited, $status, $variation,$first,$limit) = @_;

	#print "\n o=$owner i=$invited s=$status\n";
	#
	# construct the condition selector
	#
	my $cond = '';
	my $comma = '';
	# make damn sure any supplied parameters are integers
        if (!$first) { $first = 0; } 
        if (!$limit) { $limit = 100; };
	$first = int($first);
        $limit = int($limit);
	if (!($owner eq '') && ($owner>0))
		{
		my $quid = $dbh->quote($owner);
		my $cc = "${comma}owner=$owner";
		
		if($invited)
			{
 			my $qinvite = $dbh->quote("%|$invited|%");
			my $sta = (($status eq '') || ($status eq 'setup')) 
			 ? " or invitedplayers like $qinvite "
			 : "";
			$cc = "($cc or acceptedplayers like $qinvite $sta)";
			}
		$cond .= $cc;
		$comma = " and ";
		}

	if (!($status eq ''))
		{
		#
		# note that the status names in the database enum must be the same
		# as the java AsyncStatus table
		#
		my $qstat = $dbh->quote($status);
		$cond .= "${comma}status=$qstat";
		$comma = " and ";	
		}
	if(!($variation eq ''))
		{
		my $qvariation = $dbh->quote($variation);
		$cond .= "${comma}variation=$qvariation";
		$comma = " and ";
		}
	if(!($cond eq '')) { $cond = "where $cond" ; }
	my $index = ($status && $invited ) ? "use index (status)" : "";
	my $q = "select owner,whoseturn,gameuid,status,variation,playmode,"
		."comments,firstplayer,speed,"
		."invitedplayers,acceptedplayers,allowotherplayers,created,last "
		."from offlinegame $index $cond order by status,last desc limit $limit offset $first";
	#print "\nQ: $q\n";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	my $msg = "";

	while($n-- > 0)
	{
	#
	# note that these field names must match the expectations of the AsyncGameStack class
	#
	my ($owner,$whoseturn,$gameuid,$status,$variation,$playmode,
		$comments,$firstplayer,$speed,
		$invitedplayers,$acceptedplayers,$allow,$created,$last) = &nextArrayRow($sth);
	$msg .= "gameuid \"$gameuid\"\n"
		. "whoseturn \"$whoseturn\"\n"
		. "owner \"$owner\"\n"
		. "status \"$status\"\n"
		. "speed \"$speed\"\n"
		. "variation \"$variation\"\n"
		. "playmode \"$playmode\"\n"
		. "comments \"$comments\"\n"
		. "firstplayer \"$firstplayer\"\n"
		. "invitedplayers \"$invitedplayers\"\n"
		. "acceptedplayers \"$acceptedplayers\""
		. "allowotherplayers \"$allow\"\n"
		. "created \"$created\"\n"
		. "last \"$last\"\n";
	
	}
	&finishQuery($sth);

	return $msg;
}
sub sendNotification()
{
   my ($dbh,$type,$uid,$message) = @_;

   #print "not u=$uid m=$message\n";
   my $quid = $dbh->quote($uid);
   my $q = "select player_name,e_mail,discorduid from players where uid=$quid";
   my $sth = &query($dbh,$q);
   my $nr = &numRows($sth);
   #print "Q $nr $q\n";
   if($nr==1)
	  {
	   my ($player_name,$e_mail,$discord) = &nextArrayRow($sth);
	  &send_silent_notification($player_name,$type,$'from_email,$e_mail,$discord,$message);
	  }
   &finishQuery($sth);
}

sub sendNags()
{	my ($dbh) = @_;
	my $q = "select nag,nagtime,utc_timestamp() from offlinegame where nag is not null and nagtime<utc_timestamp() and status='active'";
	my $sth = &query($dbh,$q);
	my $nrows = &numRows($sth);
	if($nrows > 0)
	{
	my $orows = $nrows;
	my $restamp;
	while ($nrows-- > 0)
	{
	my ($nag,$nagtime,$stamp) = &nextArrayRow($sth);
	$restamp = $stamp;
	my ($uid,$message) = split ",",&decode64($nag);
	&sendNotification($dbh,"Boardspace.net game reminder",$uid,$message);
	}
	&finishQuery($sth);
	my $qrestamp = $dbh->quote($restamp);
	&commandQuery($dbh,"update offlinegame set nagtime=date_add(nagtime,interval 1 day) where nag is not null and nagtime<$qrestamp and status='active' limit $orows");
	}
}

sub sendNotifications()
{	my ($dbh) = @_;
	my $idx = 0;

	#print "send not\n";

	while(1)
	 {
	  my $notification = &param("notification$idx");
	  #print "n=$notification\n";
	  if(!$notification) { return; }
	  $idx++;
	  my ($uid,$message) = split ",",&decode64($notification);
	  &sendNotification($dbh,"Boardspace.net game activity",$uid,$message);
	}
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
	my $ok = &useCombinedParams($'tea_key,1);
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
			  if($uid>0) { &sendNags($dbh); }
			  $msg = "uid $uid\n";
			}
		elsif('checkname' eq $tagname) { $msg = &getPlayerUid($dbh,$pname); }
		elsif('creategame' eq $tagname) { $msg = &creategame($dbh,$pname,$passwd); }
		elsif('getinfo' eq $tagname) 
			{ my $own = &param('owner');
		          my $inv = &param('invitedplayers');
			  my $sta = &param('status');
			  my $var = &param('variation');
			  my $limit = &param('limit');
			  my $first = &param('first');
			  #print "\nget o=$own i=$inv s=$sta v=$var\n";
                          $msg = &getgameinfo($dbh,$own,$inv,$sta,$var,$first,$limit); 
			}
		elsif('recordgame' eq $tagname)
		{
		  $msg = &recordgame($dbh,$pname,$passwd);
		}
		elsif('getusers' eq $tagname) { $msg = &getusers($dbh,&param('users')); }
		elsif('getbody' eq $tagname) { $msg = &getbody($dbh); }
		else { $msg = "error \"undefined request: $tagname\"" ; }
		
		if( index($msg,"error ") == 0)
			{
			&printResult("Error",substr($msg,6));
			}
			else
			{
			&sendNotifications($dbh);
			&printResult("Ok",$msg);
			}
		
	#	print "$msg<p>";
		}
	else {
		&printResult("Error","bad version, not 1");
	}
	}
	}
}
