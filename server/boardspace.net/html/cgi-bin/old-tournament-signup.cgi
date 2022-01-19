#!/usr/bin/perl
#
# rechecked for query quoting 8/2010 after some glitches seen on gs_ips
#
# accept self signups and other data for a scheduled tournament.  
# administer the tournament system.  The same script serves both
# for administration and ordinary users, so some care is taken to
# present the right options depending on if we're the administrator
# or not.  
#
# this is work-in-progress linked to the boardspace database
# there are unique "tournament" records for each tournament,
# "participant" records for players who intend to play
# "match" records for two player matches scheduled to take place
# "group" records for groups of matches that go together
# 
# 
# minimal administrative functions if you specify ?admin=password
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
use Crypt::Tea;

#
# fill the names, uids, and emails of the players in a partucular tournament
#
my %timezones;
my %names;
my %uids;
my %emails;
my %ranks;
my %notes;
my %hasmatch;		#has a match assigned in the current group

my @alluids;
sub fill_player_info()
{	my ($dbh,$tournament,$variation,$forced) = @_;
	my $qvar = $dbh->quote($variation);
	if($forced || ($#alluids <= 0))
	{
	my @uids;
	my $qt = $dbh->quote($tournament);
	my $jr = $variation ? " left join ranking  on participant.pid = ranking.uid and ranking.variation=$qvar and ranking.is_master='No' " : "";
	my $jo = $variation ? "rank desc, " : "";
	my $jw = $variation ? " and (ranking.variation=$qvar or ranking.variation is null) " : "";
	my $jv = $variation ? " ,ranking.value as rank " : "";
    my $q = "SELECT player_name,timezone_offset,players.uid,e_mail,e_mail_bounce $jv from participant "
		. " left join players on participant.pid = players.uid "
		. " $jr where tid=$qt $jw"
		. " order by $jo player_name asc";
#print "$tournament $variation $forced Q: $q<br>";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);

	while($nr-- > 0)
	{
	my ($name,$timezone,$uid,$email,$bounce, $val) = &nextArrayRow($sth);
	my $textnotes = "";
	$names{$uid} = $name;
	$timezones{$uid} = $timezone;
	$uids{$name} = $uid;
	$emails{$uid} = $email;
	if($variation) { if(!$val) { $val = "--"; } $ranks{$uid} = $val;}
	if($bounce>0) { $textnotes = "<b>(bounce)</b>"; }
	my $quid = $dbh->quote($uid);
	my $subq = "select variation,games_played,value from ranking where uid=$quid";
#message print "q2: $subq<p>";
	my $sth2 = &query($dbh,$subq);
	my $nr2 = &numRows($sth2);
	while($nr2 > 0)
	{	$nr2--;
		my ($var,$num,$rank) = &nextArrayRow($sth2);
		$textnotes .= " $var($num)=$rank";

	}
	$notes{$uid}  = $textnotes;
	&finishQuery($sth2);

	push @uids,$uid;

	}

	&finishQuery($sth);
	@alluids = @uids;
	}
	return(@alluids);
}

sub init {
	$| = 1;				# force writes
	__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
}


sub print_header 
{	my ($admin) = @_;
	&standard_header();
	my $tmh = &trans("Boardspace.net Tournament Manager");
	my $tm = &trans("Tournament Manager");
	my $use = &trans("use this page to sign up for scheduled tournaments, check status of active tournaments, and report match results.");
	my $am = $admin
		? "<script type='text/javascript' src='/ckhtml/ckeditor.js'></script>\n"
		: "";
	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>$tmh</TITLE>
$am
</HEAD>
<center><H2>$tm</h2>
<br>$use
</center>
<p>
Head_end
}

#
# this logon doesn't require a password if $admin is matches the tournament_admin password
# also check for known-bouncing email
#
sub logon 
{
	my ($dbh,$pname,$passwd,$admin) = @_;
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $bannercookie = &bannerCookie();
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $pw = $admin ? "" : "AND password=$slashpwd ";
	my $q = "SELECT uid,e_mail_bounce FROM players WHERE player_name=$slashpname $pw AND status='ok'";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
    my $val=0;
    my $bounce=0;
  
	if(($n == 1)
		 && (&allow_ip_access($dbh,$myaddr)>=0)
	     && (&allow_ip_access($dbh,$bannercookie)>=0)
	     )
    { ($val,$bounce) = &nextArrayRow($sth);
    }
	if(($n==1) && !$val)
		{
		&note_failed_login($dbh,$myaddr,"IP: Tournament as $pname");
        &note_failed_login($dbh,$bannercookie,"CK: Tournament as $pname");
		}
	&finishQuery($sth);
	return ($val,$bounce);
}

sub logonOk()
{	my ($dbh,$pname,$passwd,$admin) = @_;
	my ($ok,$bounce) = &logon($dbh,$pname,$passwd,$admin);
	return($ok);
}
#
# list the active or scheduled tournaments.  
# If a particular tournament id is supplied, list details
# if admin is supplied, add more editing options
#
sub show_tournaments
{ my ($dbh,$uid,$fromname,$passwd,$historic,$tid,$admin) = @_;
  my $qu = $dbh->quote($uid>0 ? $uid : -1);
  my $qtid = $dbh->quote($tid);
  my $tidclause = ($tid>0) ? " and tournament.uid=$qtid " : "";
  my $cond = $historic ? "(status='finished')" : "(status='completed' or status='signup' or status='active')";
  my $q = "SELECT status,uid,description,count(pid),sum(if(pid=$qu,1,0)),start,longdescription from tournament left join participant "
              . " on uid=tid where $cond $tidclause group by uid order by start desc";
#message print "tournament $q<p>";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  my $activestatus = 0;
	#print $q;
  if($n>0)
    { print "<table><tr><td>";
    
      print "<table>";
      print "<tr>";
      if($uid>0) { print "<td><b>I'm Playing</b>&nbsp;</td>"; }
	  if($admin)
	  {
	  print "<td>&nbsp;</td>";
	  }
	  my $ct = ($tid>0) 
		? "<a href='$ENV{'SCRIPT_NAME'}'><b>"
				. &trans('All Tournaments')
				. "</b></a>" 
		: "<b>" . &trans('Current Tournaments') . "</b>";
      print "<td><b>"
	  . &trans("Players")
	  . "</b></td><td align=center>$ct</td><td><b>"
	  . &trans("status")
	  . "</b></td><td><b>"
	  . &trans("Starting Date")
	  . "</b></td></tr>\n";
    while($n-->0)
      {my ($thisstat,$thistid,$messid,$nplayers,$myplayers,$start,$longform) = &nextArrayRow($sth);
       print "<tr>";
	   $activestatus |= (lc($thisstat) eq 'signup');
	   if($admin)
	   {
		 print "<td><a href=$ENV{'SCRIPT_NAME'}?t2admin=$admin&operation=edittournament&tournamentid=$thistid>Edit&nbsp;</a></td>";
	   }
       if($uid>0) { my $yes = ($myplayers==1) ? "Yes" : "";  print "<td>$yes</td>" }
       print"<td>$nplayers</td><td>";
       my $qpname = encode_entities($fromname);
       my $qpasswd = encode_entities($passwd);
	   my $adm= $admin ? "&t2admin=$admin" : "";
	   my $pastclause = $historic ? "past=1&" : "";
       print "<a href=$ENV{'SCRIPT_NAME'}?${pastclause}tournamentid=$thistid&fromname=$qpname&password=$qpasswd$adm>";
       print "$messid</a>";
       print "</td><td>$thisstat</td><td>$start</td></tr>\n";
       
       if($tid>0)
        {print "<tr>";
         if($uid>0) { print "<td></td>"; }
         print "<td></td>";
         print "<td colspan=2>$longform</td>\n";
         print "</tr>";
         
		 my @ids = &fill_player_info($dbh,$tid,'');

		 print "<tr><td><b>"
			. &trans("Players")
			. ":</b><br>"
			. &trans("click for player details")
			. "</td><td colspan=2>";
		 for my $pl (@ids)
		 {	my $n = $names{$pl};
			print "<a href=\"javascript:editlink('$n',0)\">$n</a>&nbsp; ";
		 }
		 print "</td></tr>";
           
        }
      }
	 if($tid<=0)
	 {
	 print "<tr>";
	 if($historic)
	 {
	 my $pa = &trans('Current Tournaments');
	 print "<td colspan=2><a href=$ENV{'SCRIPT_NAME'}?t2admin=$admin&operation=&tournamentid=-1>$pa</a></td>";

	 }
	 else
	 {
	 my $pa = &trans('Past Tournaments');
	 print "<td colspan=2><a href=$ENV{'SCRIPT_NAME'}?t2admin=$admin&operation=&tournamentid=-1&past=1>$pa</a></td>";
	 }
	 if($admin && ($tid<=0))
	 { print "<td>";
	   print "<td><a href=$ENV{'SCRIPT_NAME'}?t2admin=$admin&operation=edittournament&tournamentid=-1>New&nbsp;</a></td>";
	   print "</td>\n";
	 }
	 print "</tr>";
	}
     print "</table>";
	 my $about = &trans('About Tournaments');
	 print "</td><td  bgcolor=#d0d0ff><a href='/english/about_tournaments.html'>$about</a></td></tr>";
	 print "</table>";
	 
	 if($tid>0)
	 {	# show details for a particular tournament
		 &show_current_matches($dbh,$tid,$admin);
	 }

	 #
	 # show the "add me" form if this is a signup tournament
	 #
	if($admin || $activestatus) 
		{   print "<hr>\n";
			&show_input_form($dbh,$fromname,$passwd,$tid,$admin); 
		}

  }else
  {  print "<p>No current tournaments<p>";
  }
}


sub show_tournament_selector()
{
  my ($dbh,$default,$include_active) = @_;
  my $act = $include_active ? " or status='active' or status='completed' " : "";
  my $q = "SELECT uid,description from tournament where status='signup' $act";
  my $sth = &query($dbh,$q);
  my $nr = &numRows($sth);
  print "<select name=tournamentid>\n";
  print "<option value=0 >"
	. &trans("Select a Tournament")
	. "</option>\n";
  while ($nr-- > 0)
  {  my ($tid,$desc) = &nextArrayRow($sth);
	 my $sel = ($default eq $tid) ? "selected" : "";
     print "<option value=$tid $sel>$desc</option>\n";
  }
  print "</select>";
}

sub print_form_header()
{
  print "<form action=$ENV{'SCRIPT_NAME'} method=post>\n";
  { my $test = &param('test');
    if($test) { print "<input type=hidden name=test value='$test>\n"; }
  }
}

#
# show the "add me" form
#
sub show_input_form
{ my ($dbh,$fromname,$passwd,$default,$admin) = @_;
  $fromname = encode_entities($fromname);
  print "<p>"
	. &trans("Use this form to <b>join a tournament</b> (or remove yourself)")
	. "<br>\n";
  &print_form_header();
  if($admin) { print "<input type=hidden name=t2admin value=$admin>\n"; }
  print "<table><tr><td>"
	. &trans("Player") . ": ";
  print "<input type=text name=fromname value='$fromname' size=12>\n";
  if(!$admin)
	{
	print "</td><td>" . &trans("Your Password") . ": ";
	print "<input type=password name=passwd value='$passwd'>\n";
	}
  print "</td>";
  print "<td>"
	. &trans("Not registered at Boardspace.net?")
	. "  <a href='/english/register.shtml'>"
	. &trans("Register")
	. "</a> "
	. &trans("here")
	. ".</td>";
  print "</tr>";
  print "<tr><td>";
  &show_tournament_selector($dbh,$default,$admin?1:0);
  print "<td><input type=checkbox name=subscribe checked>"
	. &trans("I plan to play")
	. "\n</td>";
  print "</td></tr></table>\n";
  print "<input type=submit name=doit value=\""
  . &trans('Sign Me Up')
  . "\">\n";
  print "</form>\n";
}
sub get_games_played()
{
	my ($dbh,$uid,$variation) = @_;
	my $quid = $dbh->quote($uid);
	my $qvar = $dbh->quote($variation);
	my $q = !($variation eq 'None')
		? "SELECT games_played from ranking where variation=$qvar and uid=$quid"
		: "SELECT games_played from players where uid=$quid";
	my $sth = &query($dbh,$q);
	if(&numRows($sth)>0)
	{	my ($num) = &nextArrayRow($sth);
		return($num);
	}
	return(0);
}
sub elgibleParticipant()
{	my ($dbh,$uid,$variation,$threshold) = @_;	
	my $played = &get_games_played($dbh,$uid,$variation);
	return($played>=$threshold)
}

sub processInputForm
{  my ($dbh,$fromname,$passwd,$tournamentid,$subscribe,$admin) = @_;
   my ($uid,$bounce) = &logon($dbh,$fromname,$passwd,$admin);
   if($uid>0)
   { if($tournamentid>0)
     {
     my $quid = $dbh->quote($uid);
     my $qtid = $dbh->quote($tournamentid);
     if($subscribe)
       {
        my ($variation,$threshold) = &get_tournament_variation($dbh,$tournamentid);
        my $elgible = $admin || &elgibleParticipant($dbh,$uid,$variation,$threshold);
        if($elgible)
        {
        if($bounce)
        {
        print "<b>";
        print &trans("Sorry, you can't sign up for a tournmant unless your email address is working.");
        my $ad = "<a href=\"javascript:edituser('$fromname',0)\">$fromname</a>";
        print " " . &trans("You can edit your email address at #1", $ad);
        print "</b><p>";
        return(0);
       }
        else
        {
        my $q = "replace into participant set tid=$qtid,pid=$quid";
        &commandQuery($dbh,$q);  
        }}
        else
        {
        print "<b>";
        print &trans("Sorry, you can't sign up for this tournament until you have played at least #1 games",$threshold);
        print "</b><p>";
        return(0);
        }
       }
       else
       {my $q = "delete from participant where tid=$qtid and pid=$quid";
         &commandQuery($dbh,$q);  
		my $q2 = "delete from oldmatchrecord where player1=$quid and tournament=$qtid and matchstatus='notready'";
		&commandQuery($dbh,$q2);
      }
     &commandQuery($dbh,"DELETE from notes where uid=$quid");
     }
     return($uid);
   }
   else
  {  print "<br><b>";
	 print &trans("Sorry, name and password not found in our player list");
	 print "</b><p>";
    return(0);
  } 
}

sub get_tournament_variation()
{	my ($dbh,$tournament) = @_;
	my $qt = $dbh->quote($tournament);
	my $q = "select variation,game_threshold from tournament where uid=$qt";
	my $sth = &query($dbh,$q);
	my ($var,$thresh) = &nextArrayRow($sth);
	&finishQuery($sth);
	return($var,$thresh);
}
sub show_player_info()
{	my ($dbh,$tournament) = @_;
	my ($var) = &get_tournament_variation($dbh,$tournament);
	my @ids = &fill_player_info($dbh,$tournament,$var);
	print "<table>";
	my $row=0;
	for my $id (@ids)
	{	my $name = $names{$id};
		my $email = $emails{$id};
		my $rank = $ranks{$id};
		my $note = $notes{$id};
		$row++;
		print "<tr><td>$row &nbsp;</td><td><a href=\"javascript:editlink('$name',1)\">$name</a></td><td>"
		. &obfuscateHTML($email)
		. "</td>";
		print "<td>$rank</td><td>$note</td></tr>\n";
	}

	print "</table>";
}

sub print_status_selector()
{	my ($nam,$value) = @_;
	my $nr = &trans("not ready to play");
	my $rr = &trans("ready to play");
	my $nrsel = ($value eq 'notready') ? "selected" : "";
	my $rrsel = ($value eq 'notready') ? "" : "selected";
	print "<select name=$nam>";
	print "<option value='notready' $nrsel>$nr</option>";
	print "<option value='ready' $rrsel>$rr</option>";
	print "</select>";
}
sub print_player_selector()
{	my ($sname,$default,$defaultname,@players) = @_;
	print "<select name=$sname>";
	print "selector $sname $default<br>\n";
	if(!$default)
	{	print "<option value=0 selected>Select Player</option>\n";
	}
	{
	my $sel = ($default eq "-2") ? "selected" : "";
	print "<option value=-2 $sel>Not Ready</option>\n";
	$sel = ($default eq "-1") ? "selected" : "";
	print "<option value=-1 $sel>TBA</option>\n";
	}
	for my $pl (@players)
	{
	my $nam = ($pl eq $default) ? $defaultname : $names{$pl};
	my $sel = ($pl eq $default) ? "selected" : "";
	if ($hasmatch{$pl}) { $nam = "[$nam]"; };
	print "<option value='$pl' $sel>$nam</option>\n";
	}
	print "</select>";
}

sub show_blank_match_form()
{	my ($row,$tid,$group,@ids) = @_;
	print "<tr>";
	print "<td>";
	&print_player_selector("player1_$row",0,"",@ids);
	print "</td><td>";
	&print_player_selector("player2_$row",0,"",@ids);
	print "</td>";
	print "</tr>";
}

sub show_auto_match_form()
{	my ($row,$tid,$group,$pl1,$pl2,@ids) = @_;
	print "<tr>";
	print "<td>";
	&print_player_selector("player1_$row",$pl1,$names{$pl1},@ids);
	print "</td><td>";
	&print_player_selector("player2_$row",$pl2,$names{$pl2},@ids);
	print "</td>";
	print "</tr>";
}

sub show_unready_match_form()
{	my ($row,$tid,$group,$pl1,@ids) = @_;
	my $nr = &trans("not ready to play");
	my $rr = &trans("ready to play");
	print "<tr>";
	print "<td>";
	&print_player_selector("playerready_$row",$pl1,$names{$pl1},@ids);
	print "</td>";
	print "</tr>";
}


sub resultString()
{	my ($str,$name1,$name2,$result1,$result2) = @_;
	my $details = "";
	if(length($result1) || length($result2))
	{	if(!$result1 ) { $result1 = "0"; }
		if(!$result2 ) { $result2 = "0"; }
		if(length($name1)) { $details .= "<br>$name1:$result1";  }
		if(length($name2)) { $details .= "<br>$name2:$result2"; }
	}
	my $main = ($str eq 'player1') ? "<b>$name1</b> won"
				: (($str eq 'player2') ? "<b>$name2</b> won"
					: $str);
	return("$main$details");
}

sub show_filled_match_form()
{	my ($admin,$closed,$row,$matchid,$pl1,$name1,$pl2,$name2,
		$outcome1,$outcome2,$player1_points,$player2_points,$final,$played,$comment,$comment1,$comment2,@ids) = @_;
	my $en1 = &uri_escape($name1);
	my $en2 = &uri_escape($name2);
	my $plname1 = $names{$pl1};
	my $plname2 = $names{$pl2};
	print "<tr><td>\n";
	if($admin)
	{
	print "del <input type=checkbox name=delete_$row>\n";
	print "<br>email <input type=checkbox name=email_$row>\n";
	print "</td><td>";
	print "<a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname1&t2admin=$admin&matchid=$matchid>edit</a>";	
	print "</td><td>";
	print "$plname1<br>";	#this is to make browser page search find it
	&print_player_selector("match1_$row",$pl1,$name1,@ids);
	print "</td><td>";
	print "$plname2<br>";	#this is to make browser page search find it
	&print_player_selector("match2_$row",$pl2,$name2,@ids);
	print "</td><td>";
	}
	else
	{
	my $p1msg = ($closed || ($pl1 == -1)) ? "$name1</td>"
		:"<a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname1&matchid=$matchid>$name1</td>";
	print $p1msg;

	my $p2msg = ($closed || ($pl2 == -1)) ? "<td>$name2</td>"
			: "<td><a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname2&matchid=$matchid>$name2</td>";
	print $p2msg;
	}
	{
	print "<td>\n";
	my $outcome1r = &resultString($outcome1,$name1,$name2);
	my $outcome2r = &resultString($outcome2,$name1,$name2);
	my $outcome = !($final eq 'none') ? &resultString($final,$name1,$name2,$player1_points,$player2_points)
					: ($outcome1 eq $outcome2) ? $outcome1r
					: ($outcome1 eq 'none') ? "$outcome2r (unofficial)"
					: ($outcome2 eq 'none') ? "$outcome1r (unofficial)"
					: ($outcome1 eq 'scheduled') ? "$outcome2r (unofficial)"
					: ($outcome2 eq 'scheduled') ? "$outcome1r (unofficial)"
					: "disputed";

	print "$outcome";
	print "</td><td>";
	print "$played";
	}
	print "</td><td>";
	if($comment1) { print "<b>$name1</b>: $comment1<br>"; };			
	if($comment2) { print "<b>$name2</b>: $comment2<br>"; };
	if($comment) { print "<b>admin</b>: $comment"; };
	print "<input type=hidden name=match_$row value=$matchid>";
	print "</td></tr>\n"
}

sub show_notready_match_form()
{	my ($admin,$row,$matchid,$pl1,$comment1) = @_;
	my $plname1 = $names{$pl1};
	if(!$plname1) {$plname1 = "no name for $pl1 in match $matchid";}
	my $en1 = &uri_escape($plname1);
	print "<tr><td>";
	if($admin)
	{
	print "del <input type=checkbox name=delete_$row>\n";
	print "</td><td>\n";
	}
	print "<b>$plname1</b></td><td>\n";
	print "<input name=password_$row type=password>";
	print "<input name=name_$row type=hidden value='$plname1'>";
	print "<input type=hidden name=match_$row value=$matchid>";
 	my $re = &trans('Ready to Play');
	print "</td><td><input type=submit name=doit value='$re'>";
	#	my $comm = encode_entities($comment1);
	#   print "</td><td><input type=text size=60 name=comment_$row value=$comm>";
	print "</td></tr>\n"
}

sub show_matches_in_group()
{	my ($dbh,$tournamentid,$group,$type,$status,$mcomment,$sortkey,$admin) = @_;
	my @ids = &fill_player_info($dbh,$tournamentid,'');
	my $nrows = 0;
	my $qturn = $dbh->quote($tournamentid);
	my $qgroup = $dbh->quote($group);
	my $egroup = encode_entities($group);
	my $qsort = $dbh->quote($sortkey);
	my $qstat = $dbh->quote($status);
	my $qtype = $dbh->quote($type);
	my $closed = ($status eq 'closed');
	my $bgcolor = $closed ? "#c0c0ff" : "#d0e0ff";
	my $or = $admin ? "played desc " : " matchid ";
	%hasmatch = ();
	my %found;


	{

	my $q = "select player1,comment1,matchid from oldmatchrecord"
		. " where (matchstatus='notready') and tournament=$qturn and tournament_group=$qgroup order by $or";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $row = 0;
	if($nr>0)
	{
	my $firstrow=1;
	print "<p>";
	print &trans("confirm-tournament");
    &print_form_header();
	print "<input type=hidden name=operation value='makeready'>";
	print "<table>";
	while($row < $nr)
	{
	 $row++;
	 my ($pl1,$comment1,$matchid) = &nextArrayRow($sth);
	 my $name1 = $names{$pl1};
	 if($pl1>0)
	 {
	 if($firstrow)
	 {
	 my $pl = &trans("Player");
	 my $pw = &trans("Your Password");
	 print "<tr><td><b>$pl</b></td><td><b>$pw</b></td><td></td></tr>\n"; 
	 $firstrow = 0;
	 }
 	$found{$pl1}=$pl1;
	$hasmatch{$pl1} = 1;
	&show_notready_match_form($admin,$row,$matchid,$pl1,$comment1);
	}}

	&finishQuery($sth);
	print "<input type=hidden name=lastnotready value=$row>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
	if($admin) { print "<input type=hidden name=t2admin value='$admin'>\n"; }
	print "</form>\n";
	print "</table>";
	}}

	if($admin)
	{
    &print_form_header();
	print "<table>\n";
	print "<input type=hidden name=operation value=creatematch>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
	print "<input type=hidden name=firstmatch value=$nrows>\n";
	print "<input type=hidden name=group value='$egroup'>\n";
	}
	{
	my $q = "select player1,player2,outcome1,outcome2,player1_points,player2_points,admin,played,comment,comment1,comment2,matchid from oldmatchrecord"
		. " where (matchstatus!='notready') and tournament=$qturn and tournament_group=$qgroup order by $or";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	#
	# show existing matches
	#
	if($nr>=0)
	{

	my $nored = $admin 
			? "" 
			: "<br>" . ($closed ? &trans("Matches for this round are closed") : &trans("Click on your player name to Edit"));
	my $sk = "";
	my $sta = "";
	my $typ = "";
	if($admin)
	{
	my @allstat = &get_enum_choices($dbh,'matchgroup','status');
	my @alltypes = &get_enum_choices($dbh,'matchgroup','type');
	$sk = &trans("sort key: #1","<input type=text name=sortkey value=$qsort size=8>");
	$sta = &get_selector_string('round_status',$status,@allstat);
	$typ = &get_selector_string('round_type',$type,@alltypes);
	}
	print "<p><table border=1 bgcolor=$bgcolor><caption><b>"
		. $sk
		. &trans($closed ? "Matches in group #1" : "Current Matches in group #1",$egroup)
		. $sta 
		. $typ
		. "</b>$nored</caption>\n";
	if(!$admin)
	{
	print "<tr><td colspan=2 align=center><b>"
		. &trans("Who")
		. "</b></td><td><b>"
		. &trans("Result")
		. "</b></td><td><b>"
		. &trans("last change")
		. "</b></td>";
	print "<td><b>"
	. &trans("comments")
	. "</b></td></tr>\n";
	}
	while($nr-- > 0)
	{
	my ($pl1,$pl2,$outcome1,$outcome2,$player1_points,$player2_points,$adminoutcome,$played,$comment,$comment1,$comment2,$matchid) = &nextArrayRow($sth);
	my $name1 = $names{$pl1};
	my $name2 = $names{$pl2};
	$hasmatch{$pl1} = 1;
	$hasmatch{$pl2} = 1;
	if($admin)
	{
	if($name1 && $found{$pl1}) { $name1 = "$name1 (duplicate)"; }
	if($name2 && $found{$pl2}) { $name2 = "$name2 (duplicate)"; }
	}
	if($pl1 == -1) { $name1 = "TBA"; }
	if($pl2 == -1) { $name2 = "TBA"; }
	
	$found{$pl1}=$pl1;
	$found{$pl2}=$pl2;
	&show_filled_match_form($admin,$closed,$nrows++,$matchid,$pl1,$name1,$pl2,$name2,$outcome1,
							$outcome2,$player1_points,$player2_points,$adminoutcome,$played,$comment,$comment1,$comment2,@ids);
	}

	print "<tr><td colspan=6>";
	if($admin)
	{	print "<textarea rows=4 cols=80 name='comment_$group'>";
		$mcomment = &encode_entities($mcomment);

	}
	print $mcomment;
	if($admin)
	{	print "</textarea>";
		print "<script type='text/javascript'>CKEDITOR.replace( 'comment_$group' );</script>";

	}
	print "</td></tr>";

	print "</table>\n";
	}
	&finishQuery($sth);
	print "<input type=hidden name=lastmatch value=$nrows>\n";
	} # end of main match list




	if($admin)
	{
	print "<input type=hidden name=firstnew value=$nrows>\n";

	#
	# show automatch proposals
	#
	{
	my $first_auto = param('first_auto');
	my $last_auto = param('last_auto');
	if(($first_auto eq '-1') && ($last_auto eq '-1'))
	{
	my ($variation) = &get_tournament_variation($dbh,$tournamentid);
	my @rankids = &fill_player_info($dbh,$tournamentid,0,1);
	print "<input type=hidden name=firstunready value=1>\n";

	print "Unready matches<p>";

	print "<table>";
	my $start;
	my $start_index = 0;
	my $end;
	my $end_index = $#rankids;
	print "end $end_index : @rankids<p>";
	while ($start_index < $end_index)
	{my $start = $rankids[$start_index];
	 $start_index++;
	 &show_unready_match_form($start_index,$tournamentid,$group,$start,@ids);
	};

	print "</table>";
	print "<input type=hidden name=lastunready value=$start_index>\n";
	print "<p>";
	}
	elsif($first_auto && $last_auto)
	{
	print "some matches from $first_auto with $last_auto<br>\n";
	print "<table>";
	my $start;
	my $start_index = $first_auto-1;
	my $end;
	my $end_index = $last_auto-1;
	my ($variation) = &get_tournament_variation($dbh,$tournamentid);
	my @rankids = &fill_player_info($dbh,$tournamentid,$variation,1);
	do {

	do { $start = $rankids[$start_index++];
		} while( ($start_index<$last_auto)  && $hasmatch{$start} && ($start_index<=$#rankids));
	do { $end = $rankids[$end_index++]; 
		} while( ($end_index<=$#rankids) && $hasmatch{$end});
	&show_auto_match_form($nrows++,$tournamentid,$group,$start,$end,@ids);

	} while($start_index+1 < $last_auto);
	print "</table>";
	}
	}
	#
	# show some blank matches
	#
	print "<table><tr><td>\n";
	print "<table>";

	#
	# if automatch is requested, select the possible names
	#

	&show_blank_match_form($nrows++,$tournamentid,$group,@ids);
	&show_blank_match_form($nrows++,$tournamentid,$group,@ids);
	&show_blank_match_form($nrows++,$tournamentid,$group,@ids);
	&show_blank_match_form($nrows++,$tournamentid,$group,@ids);
	print "</table>";
	print "</td><td>";
	print "auto-match first <input type=text name=first_auto value='' size=3>";
	print " starting with <input type=text name=last_auto value='' size=3>";
	print "from group.<br>From -1 to -1 for not-ready";
	# this should be from "winners of previous round" group
	print "</td></tr>";
	print "</table>";
	print "<input type=hidden name=lastnew value=$nrows>";
	print "</table>\n";

	  print "<input type=hidden name=t2admin value=$admin>\n";
	  print "<input type=checkbox name='delete_$egroup'>\n";
	  print "check to delete group $egroup<br>\n";
	  print "<br><input type=checkbox name='email_$egroup'>\n";
	  print "check to send notification email to group $egroup<br>\n";
	  print "<br>Email message<br><textarea rows=4 cols=80 name='emailgroup_$egroup'>";
	  print "</textarea>\n";
	  print "<br>";
	  print "<input type=submit value='Edit Matches'>\n";
	  print "</form>\n";
	}


}


sub show_current_matches()
{	my ($dbh,$tid,$admin) = @_;
	my $qtid = $dbh->quote($tid);
	my $q = "select name,type,status,comment,sortkey from matchgroup where uid=$qtid order by sortkey,status,name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0)
	{	my ($name,$type,$status,$comment,$sortkey) = &nextArrayRow($sth);
		&show_matches_in_group($dbh,$tid,$name,$type,$status,$comment,$sortkey,$admin);

	}
	&finishQuery($sth);

	if($admin)
	{
	&print_form_header();
	print "<table>\n";
    print "<input type=hidden name=t2admin value=$admin>\n";
    print "<br>" . &trans("Create New Group") . ":";
	print "<input type=text name=creategroup value=''>\n";
	print &trans("sort key: #1","<input type=text name=sortkey value='MMM'>\n");
	print "<input type=hidden name=operation value='creatematch'>\n";
	print "<input type=hidden name=tournamentid value=$tid>\n";
	print "<input type=submit value='create group'>\n";
	print "</form>\n";
	}
}

sub creatematches()
{	my ($dbh,$tournamentid,$group,$sortkey,$admin)=@_;
	my @ids = &fill_player_info($dbh,$tournamentid,'');
	my $qtour = $dbh->quote($tournamentid);
	my $qgroup = $dbh->quote($group);
	my $egroup = encode_entities($group);
	my $emailmsg = &param("emailgroup_$group");
	my $firstex = &param('firstmatch');
	my $lastex = &param('lastmatch');
	my $status = &param('round_status');
	my $typ = &param('round_type');
	while($firstex < $lastex)
	{
		my $del = &param("delete_$firstex");
		my $email = &param("email_$firstex");
		my $edit = &param("edit_$firstex");
		my $pl1 = &param("match1_$firstex");
		my $pl2 = &param("match2_$firstex");
		my $match = &param("match_$firstex");
		my $qmatch = $dbh->quote($match);
		my $qpl1 = $dbh->quote($pl1);
		my $qpl2 = $dbh->quote($pl2);
		if($del) 
		{ my $q = "delete from oldmatchrecord where matchid=$qmatch";
		  &commandQuery($dbh,$q); 
		  #print "del $q<p>";
		}
		elsif($email)
		{	#send email to players
			&sendMatchNotify($dbh,$match,$emailmsg);
		}
		else
		{	my $stat = (($pl1==-2)||($pl2==-2)) ? "'notready'" : "'ready'";
		    my $q = "update oldmatchrecord set player1=$qpl1,player2=$qpl2,matchstatus=$stat where matchid=$qmatch";
			&commandQuery($dbh,$q);
		}
		$firstex++;
	}

	#create "unready" matches
	{
	my $first = &param('firstunready');
	my $last = &param('lastunready'); 
	while($first < $last)
	{
	my $pl1 = &param("playerready_$first");
	my $qpl1 = $dbh->quote($pl1);

	my $q = "INSERT INTO oldmatchrecord SET player1=$qpl1,player2=-1, matchstatus='notready' ,tournament=$qtour,tournament_group=$qgroup";
	&commandQuery($dbh,$q);
	$first++;
	}

	}

	{
	my $first = &param('firstnew');
	my $last = &param('lastnew');
	while($first < $last)
	{
	my $pl1 = &param("player1_$first");
	my $pl2 = &param("player2_$first");
	if($pl1 && $pl2)
		{
		my $qpl1 = $dbh->quote($pl1);
		my $qpl2 = $dbh->quote($pl2);
		
		my $q = ($pl2 eq -2) 
				? "INSERT INTO oldmatchrecord SET player1=$qpl1,player2='-1', matchstatus='notready' ,tournament=$qtour,tournament_group=$qgroup"
				: "INSERT INTO oldmatchrecord SET player1=$qpl1,player2=$qpl2,tournament=$qtour,tournament_group=$qgroup";
		&commandQuery($dbh,$q);
		}
	$first++;
	}}
	my $mcomment = &param("comment_$group");
	if($mcomment || $sortkey || $status || $typ)
	{
	my $qm = $dbh->quote($mcomment);
	my $qsort = $dbh->quote($sortkey);
	my $qstatus = $dbh->quote($status);
	my $qtype = $dbh->quote($typ);
	my $q = "update matchgroup set status=$qstatus,comment=$qm,sortkey=$qsort,type=$qtype where uid=$qtour and name=$qgroup";
	#print "Q: $q<p>";
	&commandQuery($dbh,$q);
	}
	if(param("delete_$group")) 
	{	print "Delete match $qtour group $egroup<br>\n";
		&commandQuery($dbh,"delete from oldmatchrecord where tournament=$qtour and tournament_group=$qgroup");
		&commandQuery($dbh,"delete from matchgroup where uid=$qtour and name=$qgroup");
	}
	elsif(param("email_$group"))
	{	#print "Email match $qtour group $egroup<br>\n";
		my $q = "select matchid,player1,player2 from oldmatchrecord where tournament=$qtour and tournament_group=$qgroup";
		my $sth = &query($dbh,$q);
		my $nr = &numRows($sth);
		while($nr-- > 0)
		{	my ($matchid,$pl1,$pl2) = &nextArrayRow($sth);
			sendMatchNotify($dbh,$matchid,$emailmsg);
		}
		&finishQuery($sth);
	}
	my $gr = param('creategroup');
	if($gr)
	{	my $qgr = $dbh->quote($gr);
		my $qsort = $dbh->quote($sortkey);
		&commandQuery($dbh,"insert into matchgroup set uid=$qtour,name=$qgr,sortkey=$qsort");
	}
}

sub show_admin_panel()
{	my ($dbh,$admin,$tournamentid,$op) = @_;
	print "<hr><br>"
		. &trans("Administrative Operations")
		. "<br>";
	&print_form_header();
	print "<table>\n";
	print "<input type=hidden name=t2admin value=$admin>\n";
	print "<tr><td>";
	&show_tournament_selector($dbh,$tournamentid,1);
	print "</td><td>\n";
	print "<select name=operation>";
	print "<option value='subscribe' selected>"
		. &trans("Show tournaments")
		. "</option>\n";
	print "<option value='showinfo'>"
		. &trans("Show detailed player info")
		. "</option>\n";
	print "<option value='standings'>";
	print  &trans('standings table');
	print "</option>";
	
	print "</select>";
	print "</td></tr>\n";
	print "</table><input type=submit name=doit value='"
		. &trans("Administrate")
		. "'></form>\n";
}

sub print_option()
{	my ($value,$default,$pretty) = @_;
	my $def = ($value eq $default) ? "selected" : "";
	print "<option value='$value' $def>$pretty</option>\n";
}

sub do_edit_tournament()
{	my ($dbh,$tournamentid,$admin) = @_;
	my $status = $dbh->quote(&param('tournament_status'));
	my $delete = &param('tournament_delete');
	my $qt = $dbh->quote($tournamentid);

	if($delete && ($tournamentid>0))
	{ &commandQuery($dbh,"delete from tournament where uid=$qt");
	  &commandQuery($dbh,"delete from participant where tid=$qt"); 
	  &commandQuery($dbh,"delete from oldmatchrecord where tournament=$qt");
	  &commandQuery($dbh,"delete from matchgroup where uid=$qt");
	  &show_tournaments($dbh,0,'','',0,'',$admin);

	}
	else
	{
	my $longdescription = $dbh->quote(&param('tournament_long'));
	my $description = $dbh->quote(&param('tournament_short'));
	my $start = $dbh->quote(&param('tournament_start'));
	my $end = $dbh->quote(&param('tournament_end'));
	my $variation = $dbh->quote(&param('tournament_variation'));
	my $format = $dbh->quote(&param('tournament_format'));
	my $qgame_threshold = $dbh->quote(&param('game_threshold'));
	my $sets = "set status=$status, longdescription=$longdescription,game_threshold=$qgame_threshold,"
		. "start=$start,end=$end,variation=$variation,"
		. "format=$format,description=$description";
	my $q = ($tournamentid<0)
			? "insert into tournament $sets"
			: "update tournament $sets where uid=$qt";
	&commandQuery($dbh,$q);
	if($tournamentid<0) { $tournamentid=$dbh->{ q{mysql_insertid}}; }
	&show_edit_tournament($dbh,$tournamentid,$admin);
	}
}
sub show_edit_tournament()
{	my ($dbh,$tournamentid,$admin) = @_;
	my $qt = $dbh->quote($tournamentid);
	my $q = "select status,longdescription,start,end,variation,game_threshold,format,description "
		. " from tournament where uid=$qt";
	my @allstat = &get_enum_choices($dbh,'tournament','status');
	my @allformat = &get_enum_choices($dbh,'tournament','format');
	my @allvars = &all_variations($dbh);
	my $sth = &query($dbh,$q);
	if($sth)
	{
	my ($status,$longdescription,$start,$end,$variation,$game_threshold,$format,$description)
				= &nextArrayRow($sth);
	
    &print_form_header();
	print "<input type=hidden name=t2admin value=$admin>\n";
	print "<input type=hidden name=operation value=doedittournament>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
		print "<table>";

		print "<tr><td>"
			. &trans("Status")
			. ":</td><td>";
		print &get_selector_string('tournament_status',$status,@allstat);
		print "</td></tr>";

		print "<tr><td>"
			. &trans("Format")
			. ":</td><td>";
		print &get_selector_string('tournament_format',$format,@allformat);
		print "</td></tr>\n";

		print "<tr><td>Variation</td><td>";
		push(@allvars,"None");
		print &get_selector_string('tournament_variation',$variation,@allvars);
		print "</td></tr>\n";

		print "<tr><td>Registration Threshold</td><td>";
		print &get_selector_string('game_threshold',$game_threshold,('0','1','2','3','4','5','6','7','8','9','10'));
		print "</td></tr>";
		
		print "<tr><td>Start</td><td><input type=text name=tournament_start value='$start'></td></tr>\n";

		print "<tr><td>End:</td><td><input type=text name=tournament_end value='$end'></td></tr>\n";

		print "<tr><td>Short:</td><td>";
		my $sh = &encode_entities($description);
		print "<input name=tournament_short size=80 value='$sh'>";
		print "</td></tr>\n";

		print "<tr><td colspan=2>";
		print "<textarea rows=4 cols=80 name=tournament_long>";
		print &encode_entities($longdescription);
		print "</textarea></td></tr>";
		print "<script type='text/javascript'>CKEDITOR.replace( 'tournament_long' );</script>";
		print "</table>\n";
	print "<input type=checkbox name=tournament_delete>delete this tournament<br>";
	print "<input type=submit>";
	print "</form>";
	}
}
sub show_edit_match()
{	my ($dbh,$matchid,$fromname,$admin) = @_;
	my $qm = $dbh->quote($matchid);
	my $q = "select matchgroup.status,player1,player2,player1_points,player2_points,tournament,outcome1,outcome2,admin,played,"
		. " oldmatchrecord.comment,oldmatchrecord.comment1,oldmatchrecord.comment2,tournament_group"
		. " from oldmatchrecord left join matchgroup"
		. " on oldmatchrecord.tournament = matchgroup.uid and matchgroup.name = oldmatchrecord.tournament_group "
		. " where matchid=$qm";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr)
	{	my ($status,$player1,$player2,$player1_points,$player2_points,$tournament,$outcome1,$outcome2,$adminoutcome,$played,$comment,$comment1,$comment2,$tournament_group)
				= &nextArrayRow($sth);
		&fill_player_info($dbh,$tournament,'');
		my $closed = ($status eq 'closed');
		my $name1 = $names{$player1};
		my $name2 = $names{$player2};
		my $fromuid = $admin ? -1 : $uids{lc($fromname)};
		if($admin) { $fromname = '[admin]'; };
		#
		# print the non-editable part
		#
		my $outcome1res = &resultString($outcome1,$name1,$name2);
		my $outcome2res = &resultString($outcome2,$name1,$name2);
		my $adminstr = &resultString($adminoutcome,$name1,$name2,$player1_points,$player2_points);
		print "<table border=1>\n";			    # left table start
		my $p1name = $names{$player1};
		my $p2name = $names{$player2};
		my $tz1_raw = $timezones{$player1};
		my $tz2_raw = $timezones{$player2};
		my $tz1 = $tz1_raw;
		my $tz2 = $tz2_raw;
		if($tz1!="") { my $off = &timezoneOffsetString($tz1); $tz1 = "(GMT$off)"; }
		if($tz2!="") { my $off = &timezoneOffsetString($tz2); $tz2 = "(GMT$off)"; }
		my $p1namelink = "<a href=\"javascript:editlink('$p1name',0)\">$p1name</a>&nbsp;$tz1 ";
		my $p2namelink = "<a href=\"javascript:editlink('$p2name',0)\">$p2name</a>&nbsp;$tz2 ";
		
		print "<tr><td>who</td><td>$p1namelink vs $p2namelink</td><td align=center>Comments</td></tr>";
		print "<tr><td>result from $name1</td><td>$outcome1res</td><td>$comment1</td></tr>\n";
		print "<tr><td>result from $name2</td><td>$outcome2res</td><td>$comment2</td></tr>\n";
		print "<tr><td>result from [admin]</td><td>$adminstr</td><td>$comment</td></tr>\n";

		print "<tr><td>last edit</td><td>$played</td><td>";

		if($admin || !$closed)
		{
		my $p1 = $fromname;
		my $p2 = ($fromname eq $p1name) ? $p2name : $p1name;
		my $t1 = ($p1 eq $p1name) ? $tz1_raw : $tz2_raw;
		my $t2 = ($p1 eq $p1name) ? $tz2_raw : $tz1_raw;
		print "<form action='/cgi-bin/meetup.cgi' target=_new method=post>\n";
		print "<input type=hidden name=player1 value='$p1'>\n";
		print "<input type=hidden name=player2 value='$p2'>\n";
		print "<input type=hidden name=player1zone value='$t1'>\n";
		print "<input type=hidden name=player2zone value='$t2'>\n";
		print "<input type=submit name=doit value='find a good time to play'>\n";
		print "</form>\n";
		}
		print "</td></tr>\n";
		print "</table>";					    # left table end

		print "<table><tr><td align=top>";		# outer table start
		print "</td><td>";
		#
		# print the editable part
		#
		if($admin || !$closed)
		{
		&print_form_header();
		print "<table>";						# right table start
		print "<tr><td>Edit by $fromname";
		my $pw = &trans("Your Password");
		if(!$admin) { print "</td><td>$pw <input type=password name=passwd value=''>"; }
		print "</td></tr>\n";
		{
		my $outcome = $admin ? $adminoutcome 
							: ($fromuid eq $player1) ? $outcome1 
							: ($fromuid eq $player2) ? $outcome2 : "";
				
		print "<tr><td>outcome</td>";
		print "<td><select name=outcome>";
		&print_option("none",$outcome,'none');
		&print_option('cancelled',$outcome,'cancelled');
		&print_option('scheduled',$outcome,'scheduled');
		&print_option('draw',$outcome,'draw');
		&print_option('player1',$outcome,"$name1 won");
		&print_option('player2',$outcome,"$name2 won");
		print "</select>\n";
		print "</td></tr>";
		}
		my $editcomm = $admin ? $comment
							: ($fromuid eq $player1) ? $comment1 
							: ($fromuid eq $player2) ? $comment2 : "";
		my $ent = &encode_entities($editcomm);
		if($admin)
		{
			print "<tr><td>Match points for $name1</td><td><input name=player1_points type=text value='$player1_points'></td></tr>\n";
			print "<tr><td>Match points for $name2</td><td><input name=player2_points type=text value='$player2_points'></td></tr>\n";
		}
		print "<tr><td colspan=2>comments:<br>";
		print "<textarea name=comment cols=60 rows=4>";
		print "$ent</textarea></td></tr>\n";
		print "<script type='text/javascript'>CKEDITOR.replace( 'comment' );</script>";


		print "</td></tr></table>\n";		# right table end
		print "<input type=hidden name=operation value=doeditmatch>\n";
		print "<input type=hidden name=matchid value=$matchid>\n";
		print "<input type=hidden name=fromname value=$fromname>\n";
		my $qpl = $dbh->quote($fromname);
		if($admin)
		{
		print "<input type=hidden name=t2admin value=$admin>\n";
		}
		print "<br><input type=checkbox name='send_email' value='yes' checked>" . &trans("Send Emails") . "\n";
		print "<br><input type=submit value='edit match'>";
		print "</form>\n";

		}			
		else { print &trans("Results for this group are closed"); }
		
		print "</td></tr></table>";		# outer table end

    &print_form_header();
	print "<input type=hidden name=operation value=subscribe>";
	print "<input type=hidden name=tournamentid value=$tournament>";
	print "<input type=submit value='back to match view'>";
	if($admin) { print "<input type=hidden name=t2admin value=$admin>";}
	print "</form>\n";

	}


}

sub do_edit_match()
{	my ($dbh,$matchid,$playerid,$email,$admin) = @_;
	# playerid is the authenticated player id
	my $outcome = &param('outcome');
	my $qoutcome = $dbh->quote($outcome);
	my $com = &param('comment');
	my $qcom = $dbh->quote($com);
	my $qmatch = $dbh->quote($matchid);
	my $qpl = $dbh->quote($playerid);
	my $point1 = &param('player1_points');
	my $point2 = &param('player2_points');
	my $qp1 = "";
	my $qp2 = "";
	if(length($point1) || length($point2))
	{
		$qp1 = "player1_points=" . $dbh->quote("$point1") . "," ;
		$qp2 = "player2_points=" . $dbh->quote("$point2") . "," ;
	}
	my $out1 = $admin ? "outcome1" : "if(($qpl = player1),$qoutcome,outcome1)";
	my $out2 = $admin ? "outcome2" : "if(($qpl = player2),$qoutcome,outcome2)";
	my $out3 = $admin ? "admin=$qoutcome," : "";
	my $pmatch = $admin ? "" : "and ((player1 = $qpl) or (player2 = $qpl))";
	my $acom = $admin ? "comment=$qcom," : "";
	my $q = "update oldmatchrecord set outcome1=$out1,"
		. "outcome2= $out2,"
		. "$out3$qp1$qp2"
		. "comment1 = if(($qpl = player1),$qcom,comment1), "
		. "comment2 = if(($qpl = player2),$qcom,comment2), "
		. "$acom "
		. "played=CURRENT_TIMESTAMP"
		. " where matchid=$qmatch $pmatch";
	#print "Q: $q<br>";
	&commandQuery($dbh,$q);
	if($email) { &sendMatchNotify($dbh,$matchid); } else { print "No emails sent<br>"; }

}


sub sendMatchNotify()
{	my ($dbh,$matchid,$msg) = @_;

	if($matchid>0)
	{
	my $q = "select player_name,e_mail,language from oldmatchrecord "
				. " left join players on ((oldmatchrecord.player1 = players.uid) or (oldmatchrecord.player2 = players.uid))"
				. " where oldmatchrecord.matchid = '$matchid' " ;
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $lan = $lib'language;
	while($nr-- > 0)
	{
	my ($player_name,$e_mail,$language) = &nextArrayRow($sth);
	#try to send the message translated to the appropriate language
	if(!($lib'language eq $language)) { &readtrans_db($dbh,$language); }
	my $tmsg = &trans("There is new activity in your tournament match at boardspace.net\nSee #1",
						"http://$ENV{'HTTP_HOST'}/$ENV{'SCRIPT_NAME'}?matchid=$matchid&fromname=$player_name&operation=editmatch\n");
	if($msg)
	{ $tmsg = $tmsg . "\n\n" . $msg . "\n";
	}
	print "<br>mail to $player_name\n";
	&send_mail($'from_email,$e_mail,&trans("Boardspace.net tournament activity"),$tmsg );
	}
	if(!($lib'language eq $lan)) { &readtrans_db($dbh,$lan); }
	}
}
sub do_makeready_match()
{	my ($dbh,$matchid) = @_;
	if($matchid)
	{
	my $qm = $dbh->quote($matchid);
	my $q = "update oldmatchrecord set matchstatus='ready',player2='-1' where matchid = $qm";
	&commandQuery($dbh,$q);
	}

}

sub delete_makeready_match()
{
	my ($dbh,$matchid) = @_;
	if ($matchid)
	{	
	my $qm = $dbh->quote($matchid);
	my $q = "delete from oldmatchrecord where matchid=$qm";
	&commandQuery($dbh,$q);
	}
}
sub tournament_footer 
{	
	my $home = &trans('BoardSpace.net home page');
	my $goto = &trans('Go to');
	my $email = &trans('E-mail:');
	my $linkbody = "";
	my $emailout = &standard_email();
	print <<Footer;
<P>
<hr>
<center>
<table width=100%>
<tr><td align=left>
<font size=-1>
$email $emailout
</font>
</td>
<td align=center>$linkbody
</td>
<td align="right">
$goto <A HREF="/">$home</A>
</td></tr>
</table>
</center>

Footer
}
sub arrayContains()
{	my ($aref,$key) = @_;
	my @ar = @{$aref};
	for(my $i = 0; $i<=$#ar ; $i++) 
		{ if(@ar[$i] eq $key)  { return(1); }
		}
	return(0);
}

sub standings_table()
{
	my($dbh,$tid) = @_;
	my $qtid = $dbh->quote($tid);
	my $q = 
		  " select matchgroup.uid,matchid,player1 as pid,player1_points,matchgroup.name,matchgroup.sortkey,player_name"
		. " from participant left join oldmatchrecord"
		. " on oldmatchrecord.tournament = $qtid and oldmatchrecord.player1=participant.pid"
		. " left join matchgroup on matchgroup.type='swiss' and matchgroup.uid = $qtid and matchgroup.name = oldmatchrecord.tournament_group "
		. " left join players on players.uid = pid"
		. " union "
		. " select matchgroup.uid,matchid,player2 as pid,player2_points,matchgroup.name,matchgroup.sortkey,player_name"
		. " from participant left join oldmatchrecord"
		. " on oldmatchrecord.tournament = $qtid and oldmatchrecord.player2=participant.pid"
		. " left join matchgroup on matchgroup.type='swiss' and matchgroup.uid = $qtid and matchgroup.name = oldmatchrecord.tournament_group "
		. " left join players on players.uid = pid"
		. " order by player_name,sortkey desc";
	my %cells;
	my %total;
	my @cols;
	my @rows;
	my $sth = &query($dbh,$q);
	my $nrows = &numRows($sth);
	my $some = 0;
	
	while($nrows-- > 0)
	{
	my ($uid,$matchid,$player1,$points,$name,$key,$player_name) = &nextArrayRow($sth);
	

	if($uid)
	{
	if($name && !&arrayContains(\@cols,$name)) {  push(@cols,$name); }
	if($player_name && !&arrayContains(\@rows,$player_name)) { push(@rows,$player_name); }
	if(length($points)>0)
		{my $key = "$player_name,$name";
		 $cells{$key} = ($points);
		 my $val = $cells{$key};
		 $total{$player_name} += $points;
		 $some++;
		}
	   
	}
	
	}
	if($some==0) { return; }
	my $result = "<table><tr><td>" . &trans("Rank") . "</td><td>" . &trans("Player") . "</td>";
	foreach my $col (@cols) { $result .= "<td>$col</td>"; }
	$result .= "<td>" . &trans("Total") . "</td>";
	$result .= "</tr>\n";

	my @sorted;
	foreach my $row (@rows) { my @ar = ($total{$row}+0,$row); push(@sorted,\@ar); }
	@sorted = sort { my @aa = @{$a}; 
	                 my @bb = @{$b};
	                 return($bb[0] <=> $aa[0]);
	                 } @sorted;
	for(my $i =0 ; $i<=$#sorted; $i++)
	{	my @rowstuff = @{$sorted[$i]};
		my $row = $rowstuff[1];
		my $ip1 = $i+1;
	    $result .= "<tr><td>$ip1</td><td>$row</td>";
		foreach my $col (@cols)
		{	my $key = "$row,$col";	
			my ($val) = $cells{$key};

			if(length($val)==0) 
				{ $val = "-"; 
				}

			my $pts = "$val";
			$result .= "<td>$pts</td>";
		}
		$result .= "<td>$total{$row}</td>";
		$result .= "</tr>\n";
	}
	$result .= "</table>";

	print $result;

}
sub matchclosed()
{	my ($dbh,$matchid) = @_;
	my $qmatch = $dbh->quote($matchid);
	my $q = "select status from matchgroup left join oldmatchrecord "
		. " on matchgroup.uid = oldmatchrecord.tournament"
		. " and matchgroup.name=oldmatchrecord.tournament_group and matchgroup.uid=oldmatchrecord.tournament"
		. " where oldmatchrecord.matchid = $qmatch";
	my $sth = &query($dbh,$q);
	my ($status) = &nextArrayRow($sth);
	&finishQuery($sth);
	return($status eq 'closed'); 
}

# --------------------------------------------
&init();

print header;

#__d( "tournament...");

sub do_tournamentboard()
{
  my $fromname = param('fromname');
  	 $fromname = &despace(substr($fromname,0,25));
  my $passwd = param('passwd');
	 $passwd = &despace(substr($passwd,0,15));
  my $tournamentid = param('tournamentid') || "0";
  my $subscribe = param('subscribe');
  my $admin = param('t2admin');	# the value of "admin=xx" is the admin password
								# which is taken from include.pl.  All the local
								# functions are passed either this password or null
  my $dbh = &connect();
  my $uid = 0;
  my $operation = &param('operation');
  my $group = &param('group');
  my $past = &param('past');
  my $adbad = 0;
  if(lc($operation) eq 'subscribe') { $operation=''; };
  if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
    { &readtrans_db($dbh);
	  if($admin && (!$'tournament_password || !($admin eq $'tournament_password))) 
		{ $admin = ''; 
		  $adbad = 1;
		}
	  &print_header($admin); 
	if($adbad)
	{print "<b>Incorrect admin password<b><p>\n";
	}
	  if($operation eq '')
		{if($fromname && ($passwd || $admin)) 
			{ $uid = &processInputForm($dbh,$fromname,$passwd,$tournamentid,$subscribe,$admin);
			}
		&show_tournaments($dbh,$uid,$fromname,$passwd,$past,$tournamentid,$admin);
		}
	   elsif($operation eq 'showinfo')
	   {	&show_player_info($dbh,$tournamentid);
	   }
	   elsif($operation eq 'standings')
	   {	&standings_table($dbh,$tournamentid);
	   }
	   elsif ($operation eq 'creatematch')
	   {	my $sortkey = param('sortkey');
			&creatematches($dbh,$tournamentid,$group,$sortkey,$admin);
		    &show_tournaments($dbh,$uid,$fromname,$passwd,0,$tournamentid,$admin);
	   }
	   elsif ($operation eq 'doeditmatch')
	   {
			my $matchid = param('matchid');
			my $editok = $admin ?  -1 : &logonOk($dbh,$fromname,$passwd,$admin);
			my $closed = !$admin && &matchclosed($dbh,$matchid);
			my $email = param('send_email');
			if($editok)
			{
			    if(!$closed)
			    {
				&do_edit_match($dbh,$matchid,$editok,$email,$admin);
			    }
			}
			elsif($fromname && $passwd && !$admin)
			{	print "Sorry, wrong password<p>\n";
			}
			&show_edit_match($dbh,$matchid,$fromname,$admin);
	   }
	   elsif ($operation eq 'makeready')
	   {	my $last = param('lastnotready');
			my $idx = 1;
			while($idx <= $last)
			{	my $passwd = param("password_$idx");
				my $fromname = param("name_$idx");
				my $matchid = param("match_$idx");
				my $editok = $passwd && ($admin	? -1 : &logonOk($dbh,$fromname,$passwd,$admin));
				my $del = param("delete_$idx");
				if($admin && $del)
				{	&delete_makeready_match($dbh,$matchid);
				}
				elsif($editok)
				{ 
				  &do_makeready_match($dbh,$matchid);
				 }
			$idx++;
			}
			&show_tournaments($dbh,$uid,$fromname,$passwd,0,$tournamentid,$admin);
	   }
	   elsif ($operation eq 'editmatch')
	   {	my $matchid = param('matchid');
			&show_edit_match($dbh,$matchid,$fromname,$admin);
	   }
	   elsif ($admin && ($operation eq 'doedittournament'))
	   {
		 &do_edit_tournament($dbh,$tournamentid,$admin);
	   }
	   elsif($admin && ($operation eq 'edittournament'))
	   {	&show_edit_tournament($dbh,$tournamentid,$admin);
	   }
	   else
	   {	print "Operation $operation not implemented<br>\n";
	   }
	  if($admin)
	  {	&show_admin_panel($dbh,$admin,$tournamentid,$operation);
	  }
    }
  &tournament_footer();
}

do_tournamentboard();
