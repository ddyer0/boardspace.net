#!/usr/bin/perl
#
#
# logon     Validate user's Player Name and password CGI program!check
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
  	my $q = "SELECT uid,e_mail_bounce FROM players WHERE player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid)) AND status='ok' and is_robot is null";
	my $sth= &query($dbh,$q);
	my $nr = &numRows($sth);
	my $uid = 0;
	my $e_mail_bounce = 0;
	if($nr == 1)
	{
	($uid,$e_mail_bounce) = &nextArrayRow($sth);
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
	return ($uid,$e_mail_bounce);
}
#
# check that a player name exists, and is suitable to be an opponent
#
sub getPlayerUid()
{ 
	my ($dbh, $pname,$bounce) = @_;
 	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $slashpname = $dbh->quote($pname);

	my $isok = 0;
  	my $q = "SELECT uid,e_mail_bounce FROM players WHERE player_name=$slashpname AND status='ok' and is_robot is null";
	my $sth= &query($dbh,$q);
	my $nr = &numRows($sth);
	my $uid = 0;
	my $e_mail_bounce;
	if($nr == 1)
	{
	($uid,$e_mail_bounce) = &nextArrayRow($sth);
	}
	&finishQuery($sth);
	if($bounce)
	{
	  my $eb = $e_mail_bounce ? "true" : "false";
	  return "uidbounce $uid \"" . $pname . "\" \"$eb\"";
	}
	return "uid $uid\n";
}

sub recordgame()
{
	my ($dbh,$pname,$password) = @_;
	my ($uid) = &logon($dbh,$pname,$password);
	if($uid>0)
	{
	my $dirnum = &param('directory');
	my $qdirnum = $dbh->quote($dirnum);
	my $gamename = &param('gamename');
	my $body = &decode64(&param('body'));
	my $q = "select directory from variation where directory_index = $qdirnum limit 1";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr==1)
		{
		my ($dir) = &nextArrayRow($sth);
		my $root = $ENV{'DOCUMENT_ROOT'};
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
                         #__dStart( "$::debug_log", $ENV{'SCRIPT_NAME'} );
			 #__d("saving file ($fullname)");
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
# this is for the tournament system, to mark an offline game as deleted when the
# corresponding tournament match is deleted
#
sub deleteOfflineGame()
{	my ($dbh,$gameid) = @_;
	my $qgameuid = $dbh->quote($gameid);
	my $q2 = "update offlinegame set status='canceled',marked='expired' where gameuid=$qgameuid limit 1";
	#print "q: $q2<br>";
	&commandQuery($dbh,$q2);
}
#
# this is for the use of the tournament system
#
sub create2PlayerGame()
{	my ($dbh,$player1,$player2,$variation,$comment) = @_;
	my $qp1 = $dbh->quote($player1);
	# create a match
	my $qup = $dbh->quote("|$player1|$player2|");
	my $qvariation = $dbh->quote($variation);
	my $qcomment = $dbh->quote(&encode64($comment));
	my $qm = "insert into offlinegame set acceptedplayers=$qup,invitedplayers=$qup,variation=$qvariation,whoseturn=$qp1,comments=$qcomment,"
			. "status='active',speed='day2',allowotherplayers='false',created=utc_timestamp(),last=utc_timestamp()";
	my $sth = &commandQuery($dbh,$qm);
	my $gid = &last_insert_id($sth);
}
sub update2PlayerNag()
{
   my ($dbh,$game,$player,$message,$time) = @_;
   my $nag = encode64("$player,$message");
   my $quid = $dbh->quote($game);
   my $qnag = $dbh->quote($nag);
   my $qtime = "DATE_ADD(utc_timestamp(),INTERVAL $time DAY)";
   my $q = "update offlinegame set nag=$qnag,nagtime=$qtime where gameuid=$quid limit 1"; 
   &commandQuery($dbh,$q);
}
#
# create or updare game
#
sub creategame()
{	my ($dbh, $pname, $password) = @_;

	my ($uid) = &logon($dbh,$pname,$password);

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

	my $lastchange = &param('lastknownchange');
	my $qlastchange = $dbh->quote($lastchange);

	my $lastsequence = &param('lastknownsequence');
	my $qlastsequence = $dbh->quote($lastsequence);

	my $sequence = &param('sequence');
	my $qsequence = $dbh->quote($sequence);

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
	my $andchange = $lastchange ? " and last=$qlastchange" : "";
	my $andsequence = $lastsequence ? " and sequence=$qlastsequence" : "";
	my $postamble = $gameuid>0 
			? " where gameuid= $qgameuid" . $andchange
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
			. ($sequence ? ",sequence=$qsequence" : "")
			. $postamble;
	#print "\nQ: $q\n";
	my $sth = &commandQuery($dbh,$q);

	if($gameuid>0)
	{
	  my $nr = &numRows($sth);
	  if ($nr == 0)
		{
		# failed, most likely because the timestamp doesn't match
		my $q2 = "select last,sequence from offlinegame where gameuid=$qgameuid";
		my $sth2 = &query($dbh,$q2);
		my $nr2 = &numRows($sth2);
		if($nr2 eq 0)
			{
			return "error game $qgameuid doesn't exist";
			}
			else
			{
			my ($last,$seq) = &nextArrayRow($sth2);
			my $qlast = $dbh->quote($last);
			my $qseq = $dbh->quote($seq);

			my $msg = "";
			if($lastsequence && !($lastsequence eq $seq)) { $msg .= " last sequence is $qseq not $qlastsequence"; }
			if($lastchange && !($lastchange eq $last)) { $msg .= " last change is $qlast not $qlastchange"; }
			if($msg eq "") { $msg = " failed for some unexpected reason"; }		
			return "error$msg";
			}
		}
	}
	else {
	  $gid = &last_insert_id($sth);
	}
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
{	my ($dbh,$users,$bounce) = @_;
	my @us = split /\|/,$users;
	my $ids = "where ";
	my $comma = "";
	foreach my $id (@us)
	{my $qid = $dbh->quote($id);
	 $ids .= "${comma}uid=$qid ";
 	 $comma = " or ";
	}
	my $q = "select player_name,uid,e_mail_bounce from players $ids";
	#print "Q: $q\n";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	my $msg = "";
	while($n-- > 0)
	{	
	my ($u,$n, $b) = &nextArrayRow($sth);
	my $bt = $b ? "true" : "false";
	my $bb = $bounce ? " \"$bt\"" : "";
	$msg .= "$u \"$n\" $bb\n";
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

	my $q = "select body,chat,sequence from offlinegame where gameuid=$quid";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr==1)
	{
	my ($body,$chat,$sequence) = &nextArrayRow($sth);
	return "sequence \"$sequence\"\n body \"$body\"\nchat \"$chat\"\n";
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
	$cond = "where $cond $comma marked is null ";
	my $index = ($status && $invited ) ? "use index (status)" : "";
	my $q = "select owner,whoseturn,gameuid,sequence,status,variation,playmode,"
		."comments,firstplayer,speed,"
		."invitedplayers,acceptedplayers,allowotherplayers,created,last "
		."from offlinegame $index $cond order by last desc,status limit $limit offset $first";
	#print "\nQ: $q\n";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	my $msg = "";

	while($n-- > 0)
	{
	#
	# note that these field names must match the expectations of the AsyncGameStack class
	#
	my ($owner,$whoseturn,$gameuid,$sequence,$status,$variation,$playmode,
		$comments,$firstplayer,$speed,
		$invitedplayers,$acceptedplayers,$allow,$created,$last) = &nextArrayRow($sth);
	$msg .= "gameuid \"$gameuid\"\n"
		. "whoseturn \"$whoseturn\"\n"
		. "sequence \"$sequence\"\n"
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
	  &send_silent_notification($player_name,$type,$::from_email,$e_mail,$discord,$message);
	  }
   &finishQuery($sth);
}

sub sendNags()
{	my ($dbh) = @_;
	my $q = "select nag,nagtime,utc_timestamp() from offlinegame where nag is not null and nagtime<utc_timestamp() and (status='active' or status='setup') and marked is null";
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
	&commandQuery($dbh,"update offlinegame set nagtime=date_add(utc_timestamp(),interval 1 day) where nag is not null and nagtime<$qrestamp and (status='active' or status='setup') and marked is null limit $orows");
	}
}

sub sendNotifications()
{	my ($dbh) = @_;
	my $idx = 0;

	while(1)
	 {
	  my $notification = &param("notification$idx");
	  if(!$notification) { return; }
	  $idx++;
	  my ($uid,$message) = split ",",&decode64($notification);
	  &sendNotification($dbh,"Boardspace.net game activity",$uid,$message);
	}
}

1
