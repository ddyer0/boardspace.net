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
# 8/2015 participant tables added to support multiplayer games
# still todo: assign points to players
# minimal administrative functions if you specify ?admin=password
# 
# 1/2021 changed the authentication for admin mode to use an individual
# user permission in the database.
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
require "tlib/password_tools.pl";

use Crypt::Tea;

#
# fill the names, uids, and emails of the players in a partucular tournament
#
my %timezones;
my %names;
my %lcuids;
my %emails;
my %ranks;
my %notes;
my %hasmatch;		#has a match assigned in the current group

my @alluids;
my $adminKey = "";
my $admin_user = "";
my $admin_user_password = "";


#
# fill names and uids with the mappings for a given tournament
#
sub fill_player_info()
{	my ($dbh,$tournament,$variation,$forced) = @_;
	my $qvar = $dbh->quote($variation);
	$names{-1} = &trans("TBA");
	$names{-2} = &trans("[admin]");
	
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
	$lcuids{lc($name)} = $uid;
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
<link rel="stylesheet" href="/js/rome.css">
<script  type='text/javascript' src='/js/rome.js'></script>\n
</HEAD>
<center><H2>$tm</h2>
<br>$use
</center>
<p>
Head_end
}

#
# Log in an admin.  He has to know the secret password, and be designated
# as a tournament administrator in the player database.
#
sub logon_admin
{
	my ($dbh,$admin,$pname,$passwd) = @_;
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $bannercookie = &bannerCookie();
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $pw = "";
	my $val=0;
    $adminKey = "";
	if($passwd && $pname && ($admin eq $'tournament_password))
	{
	my $q = "SELECT uid FROM players WHERE player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid)) AND status='ok' AND is_tournament_manager='Yes' ";
	my $sth = &query($dbh,$q);
	#print "Q: $q<br>\n";
	my $n = &numRows($sth);
  	if($n == 1)
       { 
      	if((&allow_ip_access($dbh,$myaddr)>=0)
		 && (&allow_ip_access($dbh,$bannercookie)>=0))
		 {
		($val) = &nextArrayRow($sth);
		if($val) 
			{ $admin_user = $pname;
			  $admin_user_password = $passwd;
			  $adminKey = &makeAdminKey($admin,$pname,$passwd); 
			}
		}
	}
	else 
	{
	&note_failed_login($dbh,$myaddr,"IP: Tournament Manager as $pname");
    &note_failed_login($dbh,$bannercookie,"CK: Tournament Manager as $pname");
    
	&countEvent("failed tournament manager",10,100);
	}
	&finishQuery($sth);
	}
	return $val;
}

sub logon 
{
	my ($dbh,$pname,$passwd,$admin) = @_;
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $bannercookie = &bannerCookie();
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $pw = $admin ? "" : " AND pwhash=MD5(concat($slashpwd,uid)) ";
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
sub printHiddenAdmin()
{	my ($show) = @_;
	if($show && $adminKey)
	{
	print "<input type=hidden name=admin value='$adminKey'>\n";
	}
}

sub printEditLink()
{	my ($message,$thistid) = @_;
	my $formname = "edit${thistid}form";
	print "<font size=1><form method=post action=\"$ENV{'SCRIPT_NAME'}\" id='$formname'>\n";
	&printHiddenAdmin(1);
	print "<input type=hidden name=operation value='edittournament'>\n";
	print "<input type=hidden name=tournamentid value='$thistid'>\n";
	print "</form></font>\n";
	print "<a href='javascript:{}' onclick='document.getElementById(\"$formname\").submit(); return false;'>$message</a>\n";

}
sub printEditmatchLink()
{	my ($message,$thisid,$name) = @_;
	my $formname = "editmatch${thisid}${name}form";
	print "<font size=1><form method=post action=\"$ENV{'SCRIPT_NAME'}\" id='$formname'>\n";
	&printHiddenAdmin(1);
	print "<input type=hidden name=operation value='editmatch'>\n";
	print "<input type=hidden name=fromname value='$name'>\n";
	print "<input type=hidden name=matchid value='$thisid'>\n";
	print "</form></font>\n";
	print "<a href='javascript:{}' onclick='document.getElementById(\"$formname\").submit(); return false;'>$message</a>\n";
}

sub printTournamentLink()
{	my ($message,$tid,$admin,$past) = @_;
	my $formname = "showtournament$past$tid";
	my $pastclause = $past ? "1" : "0";
	my $arg = $tid>0 ? "?tournamentid=$tid" : "";
	print "<font size=1><form method=post action=\"$ENV{'SCRIPT_NAME'}$arg\" id='$formname'>\n";
	&printHiddenAdmin($admin);
	print "<input type=hidden name=operation value='showtournament'>\n";
	print "<input type=hidden name=tournamentid value='$tid'>\n";
	print "<input type=hidden name=past value='$pastclause'>\n";
	print "</form></font>\n";
	print "<a href='javascript:{}' onclick='document.getElementById(\"$formname\").submit(); return false;'>$message </a>\n";
}
#
# list the active or scheduled tournaments.  
# If a particular tournament id is supplied, list details
# if admin is supplied, add more editing options
#
sub show_tournaments
{ my ($dbh,$uid,$fromname,$passwd,$historic,$tid,$admin) = @_;
  print "<!-- show_tournaments() --!>\n";

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
	  my $msg = &trans('All Tournaments');
	  print "<td>\n";
	  &printTournamentLink($msg,-1,$admin,0);
	  print "</td>";
	  print "</tr><tr>";
	  print "<td>";
      print "<td align=center><b>"
	  . &trans("Players")
	  . "</b></td><td align=center>";
	  

	  print "</td><td><b>"
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
		 print "<td>";
		 &printEditLink("Edit",$thistid);
		 print "</td>";
	   }
       if($uid>0) { my $yes = ($myplayers==1) ? "Yes" : "";  print "<td>$yes</td>" }
       print"<td align=center>$nplayers</td><td>";
       my $qpname = encode_entities($fromname);
       my $qpasswd = encode_entities($passwd);
	   my $adm= $admin ? "&t2admin=$admin" : "";
	   &printTournamentLink($messid,$thistid,$admin,$historic);
       
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
	 print "<td colspan=2>";
	 if($historic)
	 {
	 my $pa = &trans('Current Tournaments');
	 &printTournamentLink($pa,-1,$admin,0);
	 }
	 else
	 {
	 my $pa = &trans('Past Tournaments');
	 &printTournamentLink($pa,-1,$admin,1);
	 }
 	 print "</td>";

	 if($admin && ($tid<=0))
	 { print "<td>";
	   &printEditLink("New",-1);
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
  print "<!-- show_tournament_selector() --!>\n";
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
{ my ($admin,$action,$name) = @_;
  if($action eq '') { $action = $ENV{'SCRIPT_NAME'}; }
  if($name) { $name="id='$name'"; }
  print "<form action='$action' method=post $name>\n";
  { my $test = &param('test');
    if($test) { print "<input type=hidden name=test value='$test>\n"; }
    &printHiddenAdmin($admin);
  }
}

#
# show the "add me" form
#
sub show_input_form()
{ my ($dbh,$fromname,$passwd,$default,$admin) = @_;
  $fromname = encode_entities($fromname);

  print "<!-- show_input_form() --!>\n";
  print "<input type=hidden name=operation value=''>\n";
  print "<p>"
	. &trans("Use this form to <b>join a tournament</b> (or remove yourself)")
	. "<br>\n";
  &print_form_header($admin);
  print "<table><tr><td>"
	. &trans("Player") . ": ";
  print "<input type=text name=fromname value='$fromname' size=20>\n";
  if(!$admin)
	{
	print "</td><td>" . &trans("Your Password") . ": ";
	print "<input type=password name=passwd value='$passwd' SIZE=20 MAXLENGTH=25>\n";
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
	my $q3 = "delete from matchparticipant where player=$quid and tournament=$qtid and matchstatus='notready'";
	 &commandQuery($dbh,$q3);
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
sub show_getadminpassword()
{
	my ($dbh,$admin) = @_;
	&print_form_header(0);
	print "<input type=hidden name='t2admin' value='$admin'>\n"; 
	print "<table><tr><td>";
	print &trans("your user name :");
	print "</td><td>";
	print "\n<input type=text value='' name=admin_user><br>";
	print "</td></tr>";
	print "<tr><td>";
	print &trans("Your password:");
	print "</td><td>";
	print "\n<input type=password value='' name=admin_user_password SIZE=20 MAXLENGTH=25><br>";
	print "</td><td>";
	print "\n<input type=submit>";
	print "</td></tr></table>";
	print("\n</form>");
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
	print "<input type=hidden name=from$sname value=$default>\n";
	print "</select>";
}

sub show_blank_match_form()
{	my ($row,$tid,$matchplayercount,$group,@ids) = @_;
	print "<tr>";
	my $pl = 1;
	while($pl <= $matchplayercount)
	{
	print "<td>";
	&print_player_selector("player${pl}_$row",0,"",@ids);
	print "</td>\n";
	$pl++;	
	}
	print "</tr>";
}


# this version fills a row for the matchparticipant table
sub show_filled_playerinfo()
{	my ($dbh,$admin,$closed,$row,$matchid,$matchplayerid,$pn,$pl1,$name1,@ids) = @_;

	my $en1 = &uri_escape($name1);
	my $plname1 = $names{$pl1};
	
	if($admin)
	{
	print "<td>$plname1<br>";	#this is to make browser page search find it
	my $qpl1 = $dbh->quote($pl1);
	my $qm = $dbh->quote($matchplayerid);
	&print_player_selector("match${pn}_$row",$pl1,$name1,@ids);
	print "<input type=hidden name=matchuid${pn}_$row value=$qm\n";
	&printEditmatchLink("edit",$matchid,"$plname1");
	print "</td>\n";
	}
	else
	{
	# print this as a GET link
	my $p1msg = ($closed || ($pl1 == -1)) ? "<td>$name1</td>"
		:"<td><a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname1&matchid=$matchid>$name1</td></a>\n";
	print $p1msg;
	}
	print "</td>";
}

sub show_notready_match_form()
{	my ($row,$matchid,$pl1,$comment1) = @_;
	
	print "<!-- show_notready_match_form() --!>\n";

	my $plname1 = $names{$pl1};
	if(!$plname1) {$plname1 = "no name for $pl1 in match $matchid";}
	my $en1 = &uri_escape($plname1);
	print "<tr><td>";
	print "<b>$plname1</b>";
	print "</td><td>\n";
	print "<input name=password_$row type=password SIZE=20 MAXLENGTH=25>";
	print "<input name=name_$row type=hidden value='$plname1'>";
	print "<input type=hidden name=match_$row value=$matchid>";
 	my $re = &trans('Ready to Play');
	print "</td><td><input type=submit name=doit value='$re'>";
	#	my $comm = encode_entities($comment1);
	#   print "</td><td><input type=text size=60 name=comment_$row value=$comm>";
	print "</td></tr>\n"
}
sub show_notready_matches()
{
	my ($admin,$dbh,$tournamentid,$group) = @_;
	
	print "<!-- show_notready_matches() --!>\n";


	my $qturn = $dbh->quote($tournamentid);
	my $qgroup = $dbh->quote($group);
	my $q = "select player,comment,matchid from matchparticipant"
		. " where (matchstatus='notready') and tournament=$qturn and tournament_group=$qgroup order by matchid";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $row = 0;
	if($nr>0)
	{
	my $firstrow=1;
	print "<p>";
	&print_form_header($admin);
	print &trans("confirm-tournament");
	print "<input type=hidden name=operation value='makeready'>";
	print "<table>";
	while($row < $nr)
	{
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
	 }}
 	#$found{$pl1}=$pl1;
	#$hasmatch{$pl1} = 1;
	&show_notready_match_form($row,$matchid,$pl1,$comment1);
	print "</form>\n";
	$row++;
	}
	}
	&finishQuery($sth);
	return($row);
}
sub show_matches_in_group()
{	my ($dbh,$tournamentid,$group,$type,$status,$mcomment,$sortkey,$admin) = @_;
	
	&show_notready_matches($admin,$dbh,$tournamentid,$group);

	print "<!-- show_matches_in_group() --!>\n";


	my ($variation) = &get_tournament_variation($dbh,$tournamentid);
	my $matchplayers = &getPlayersForVariation($dbh,$variation);
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
	my $or = "matchid";
	%hasmatch = ();
	my %found;

   	&print_form_header($admin);

	print "<table>\n";
	if($admin)
	{
	print "<input type=hidden name=operation value='creatematch'>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
	print "<input type=hidden name=firstmatch value=$nrows>\n";
	print "<input type=hidden name=group value='$egroup'>\n";
	}
	{
	#
	# the admin record comes from the matchrecord, whereas the player records 
	# come from matchparticipant records.  The 'A' in the admin select is a 
	# trick to make it sort last.
	#
	my $q = "select player,outcome,points,played,unix_timestamp(played),scheduled,comment,matchid,uid from matchparticipant"
		. " where (matchstatus!='notready') and tournament=$qturn and tournament_group=$qgroup "
		. " union select -2,if(admin!='winner',admin,admin_winner),0,played,unix_timestamp(played),scheduled,comment, matchid,'A' from matchrecord "
		. " where(matchstatus!='notready') and tournament=$qturn and tournament_group=$qgroup "
		. " order by $or,uid";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	#
	# show existing matches
	#
	#print "Q $q<p>";
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
	
	my $fullspan = $matchplayers+4+($admin?1:0);
	print "<tr><td colspan=$fullspan>";
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
	#
	#this papers over some damage ckeditor does
	#that inactivated the first subsequent form.
	print "<form></form>";

	
	my $first = 1;
	my $comm = "";
	my $change = "";
	my $changeint = 0;
	my $out = "";
	my $points = "";
	my $winner = "";
	my $scheduled = "";
	my $conflict = 0;
	my $pn = 0;
	while($nr-- > 0)
	{
	my ($pl1,$outcome,$player1_points,$played,$playedint,$scheduled,$comment,$matchid,$uid) = &nextArrayRow($sth);
	$pn++;
	# remove trailing nulls from colums damaged by the query
	$outcome =~ s/\x00*$//g;
	$uid =~ s/\x00*$//g;
	if($first || ($uid < 0))
	{	
	    print "<tr>";
	    $first=0;  
	    $pn = 1;
	    $nrows++;
	    $comm = "";
	    $out = "";
	    $winner = "";
	    $change = "";
	    $changeint = 0;
	    $conflict = 0;
	    $points = "";
		if($admin)
		{
		print "<td>";
		print "del <input type=checkbox name=delete_$nrows>\n";
		print "email <input type=checkbox name=email_$nrows>";
		print "<input type=hidden name=match_$nrows value='$matchid'>";
		print "<br>admin ";
		&printEditmatchLink("edit",$matchid,'');
		print "</td>";
		}	    
	}
	my $name1 = $names{$pl1};

	if($pl1!=0)
		{
		$hasmatch{$pl1} = 1;
		
		if($changeint eq 0 || ($played && ($playedint>$changeint))) { $change = substr($played,0,-3); $changeint=$playedint ; }
		
			#
			# combine outcome and comment sections
			#
			if($comment) { $comm .= "<b>$name1</b>: $comment<br>\n"; }
			
			# we reached the admin line, and there's a conflict about the outcome
			if($pl1==-2)
			{ if( $winner && !$conflict) { $out = &trans("#1 won",$winner) . "<br>\n"; }
			  # if outcome is a number
			  if($outcome =~/\d/) { $out = &trans("#1 won",$names{$outcome}) . "<br>\n"; }
			   elsif($outcome && !($outcome eq 'none')) { $out = $outcome; }
			}
			elsif ((($outcome eq 'none') || ($outcome eq 'scheduled')) && 
				!($scheduled eq '') && 
				!($scheduled eq '0000-00-00 00:00:00')
				)
			{	
				my $prop = $outcome;
				my $sched = substr($scheduled,0,-3);
				if($outcome eq 'none') { $prop = 'proposed'; }
				$out .=  "<b>$name1</b>:$prop $sched<br>";
			}
			elsif($outcome && !($outcome eq 'none')) 
				{ 
				# outcome for everyone except admin
				if($outcome eq 'win')
				{	if($winner=='') { $winner = $name1; } else { $conflict=1; };
				}
				$out .= "$name1 : $outcome<br>\n";  
				}
			if($player1_points!='') { $points .= "<br>$name1 : $player1_points\n"; }

		if($admin && $name1 && $found{$pl1}) 
			{ $name1 = "$name1 (duplicate)"; 
			}
		$found{$pl1}=$pl1;

		}

	if($pl1 == -2)
	{
	# supply the missing columns
	my $difplayers = $matchplayers+1-$pn;
	if($difplayers>0) 
		{ 
		  print "<td colspan=$difplayers></td>"; 
	    }
	print "<td colspan=2>$out$points</td>"; 
	print "<td>$change</td>";
	print "<td>$comm</td>"; 
	print "</tr>";
	$first = 1;
	}
	else
	{
	&show_filled_playerinfo($dbh,$admin,$closed,$nrows,$matchid,$uid,$pn,$pl1,$name1,@ids);
	}
	
	}

	 print "</tr>\n"; 

	}
	&finishQuery($sth);
	$nrows++;
	print "<input type=hidden name=lastmatch value=$nrows>\n";
	} # end of main match list


	
	print "</tr></table>";

	if($admin)
	{
	print "<input type=hidden name=firstnew value=$nrows>\n";

	#
	# show some blank matches
	#
	if(!$closed)
	{
	print "<table><caption>Create New matches for $group</caption>";
	my ($variation) = &get_tournament_variation($dbh,$tournamentid);
	my $matchplayercount = &getPlayersForVariation($dbh,$variation);
	&show_blank_match_form($nrows++,$tournamentid,$matchplayercount,$group,@ids);
	&show_blank_match_form($nrows++,$tournamentid,$matchplayercount,$group,@ids);
	&show_blank_match_form($nrows++,$tournamentid,$matchplayercount,$group,@ids);
	&show_blank_match_form($nrows++,$tournamentid,$matchplayercount,$group,@ids);
	print "</table>";
	print "<input type=hidden name=lastnew value=$nrows>\n";
	}
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

	print "<!-- show_current_matches() --!>\n";

	my $qtid = $dbh->quote($tid);
	my $q = "select name,type,status,comment,sortkey"
			. " from matchgroup"
			. " where uid=$qtid order by sortkey,status,name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0)
	{	my ($name,$type,$status,$comment,$sortkey) = &nextArrayRow($sth);	
		&show_matches_in_group($dbh,$tid,$name,$type,$status,$comment,$sortkey,$admin);
	}

	&finishQuery($sth);

	if($admin)
	{
	&print_form_header($admin);
	my $createop = 'creatematch';
	print "<table>\n";
 	print "<br>" . &trans("Create New Group") . ":";
	print "<input type=text name=creategroup value=''>\n";
	print &trans("sort key: #1","<input type=text name=sortkey value='MMM'>\n");
	print "<input type=hidden name=operation value=$createop>\n";
	print "<input type=hidden name=tournamentid value=$tid>\n";
	print "<input type=submit value='create group'>\n";
	print "</form>\n";
	}
}

sub creatematches()
{	my ($dbh,$tournamentid,$group,$sortkey,$admin)=@_;

	print "<!-- creatematches() --!>\n";
	#&printForm();
	my @ids = &fill_player_info($dbh,$tournamentid,'');
	my $qtour = $dbh->quote($tournamentid);
	my $qgroup = $dbh->quote($group);
	my $egroup = encode_entities($group);
	my $emailmsg = &param("emailgroup_$group");
	my $firstex = &param('firstmatch');
	my $lastex = &param('lastmatch');
	my $status = &param('round_status');
	my $typ = &param('round_type');
	my ($variation) =  &get_tournament_variation($dbh,$tournamentid);
	my $matchPlayers = &getPlayersForVariation($dbh,$variation);
	
	#&printForm();	
	# show the existing matches suitable for editing
	while($firstex < $lastex)
	{
		my $del = &param("delete_$firstex");
		my $email = &param("email_$firstex");
		my $edit = &param("edit_$firstex");
		my $match = &param("match_$firstex");
		my $qmatch = $dbh->quote($match);
		if($del) 
		{ my $q = "delete from matchrecord where matchid=$qmatch";
		  &commandQuery($dbh,$q); 
		  #print "Del $q<p>";
		  my $q2 = "delete from matchparticipant where matchid=$qmatch";
		  &commandQuery($dbh,$q2);
		}
		elsif($email)
		{	#send email to players
			#print "Sent notify $match<p>";
			&sendMatchNotify($dbh,$match,$emailmsg);
		}
		else
		{	
			# update players in existing matches
			my $pn = 0;
			while($pn++ < $matchPlayers)
			{
			my $paramname = "match${pn}_$firstex";
			my $idparamname = "from$paramname";
			my $pid = &param($paramname);
			if($pid)
			{
			my $fromuid = &param($idparamname);
			if(! ($fromuid eq $pid))
			{
			my $qpid = $dbh->quote($pid);
			my $qfromuid = $dbh->quote($fromuid);
			my $quid = $dbh->quote($fromuid);
			my $matchuid = &param("matchuid${pn}_$firstex");
			my $qmatchuid = $dbh->quote($matchuid);
			my $q = "update matchparticipant set player=$qpid where uid=$qmatchuid and player=$qfromuid and matchid=$qmatch and tournament=$qtour limit 1";
			#print "param $paramname $idparamname $fromuid pid $pid firstex $firstex<br>";
			#print "Q: $q<p>";
			&commandQuery($dbh,$q);
			}}}		
		}
		$firstex++;
	}

	#
	# create new matches
	#
	{
	my $first = &param('firstnew');
	my $last = &param('lastnew');
	while($first < $last)
	{
	my $pl1 = &param("player1_$first");
	if($pl1)
		{
		my $qpl1 = $dbh->quote($pl1);
		my $q = "INSERT INTO matchrecord SET tournament=$qtour,tournament_group=$qgroup";
		&commandQuery($dbh,$q);	
		my $q3 = "select last_insert_id() from matchrecord";
		my $sth = &query($dbh,$q3);
		my ($mid) = &nextArrayRow($sth);
		&finishQuery($sth);
		if($mid>0)
		{
		my $np = 1;
		while($np<=$matchPlayers)
		{
		my $pl = param("player${np}_$first");
		my $qpl = $dbh->quote($pl);
		my $qmid = $dbh->quote($mid);
		if($pl!=0)
			{
			my $q2 = "insert into matchparticipant set player=$qpl,tournament=$qtour,tournament_group=$qgroup,matchid=$qmid";
			#print "Q $q2<br>";
			&commandQuery($dbh,$q2);
			}
		$np++;
		}}
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
		&commandQuery($dbh,"delete from matchrecord where tournament=$qtour and tournament_group=$qgroup");
		&commandQuery($dbh,"delete from matchgroup where uid=$qtour and name=$qgroup");
	}
	elsif(param("email_$group"))
	{	#print "Email match $qtour group $egroup<br>\n";
		my $q = "select matchid from matchrecord where tournament=$qtour and tournament_group=$qgroup";
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

	print "<!-- show_admin_panel() --!>\n";

	print "<hr><br>"
		. &trans("Administrative Operations")
		. "<br>";
	&print_form_header($admin);
	print "<table>\n";
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
	print "</option>\n";
	
	print "<option value='sendemail'>";
	print &trans('send email');
	print "</option>\n";
	
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
sub getPlayersForVariation()
{	my ($dbh,$variation) = @_;
	my $qvar = $dbh->quote($variation);
	my $q = "select max_players from variation where name=$qvar";
	#print "Q: $q<p>";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $type = 'matchrecord';
	if($nr==1)
	{
		my ($players) = &nextArrayRow($sth);
		return($players);
	}
	return(2);
}
sub do_edit_tournament()
{	my ($dbh,$tournamentid,$admin) = @_;
	my $status = $dbh->quote(&param('tournament_status'));
	my $delete = &param('tournament_delete');
	my $qt = $dbh->quote($tournamentid);
	#&printForm();
	if($delete && ($tournamentid>0))
	{ &log_event($'tournament_log,$ENV{'SCRIPT_NAME'},"delete tournament $qt by $admin_user");
	  &commandQuery($dbh,"delete from tournament where uid=$qt");
	  &commandQuery($dbh,"delete from participant where tid=$qt"); 
	  &commandQuery($dbh,"delete from matchrecord where tournament=$qt");
	  &commandQuery($dbh,"delete from matchparticipant where tournament=$qt");
	  &commandQuery($dbh,"delete from matchgroup where uid=$qt");
	  &show_tournaments($dbh,0,'','',0,'',$admin);

	}
	else
	{
	my $longdescription = $dbh->quote(&param('tournament_long'));
	my $description = $dbh->quote(&param('tournament_short'));
	my $start = $dbh->quote(&param('tournament_start'));
	my $end = $dbh->quote(&param('tournament_end'));
	my $variation = &param('tournament_variation');
	my $matchplayercount = &getPlayersForVariation($dbh,$variation);
	my $qvariation = $dbh->quote($variation);
	my $format = $dbh->quote(&param('tournament_format'));
	my $qgame_threshold = $dbh->quote(&param('game_threshold'));
	my $sets = "set status=$status, longdescription=$longdescription,game_threshold=$qgame_threshold,"
		. "start=$start,end=$end,variation=$qvariation,"
		. "format=$format,description=$description";
	my $q = ($tournamentid<0)
			? "insert into tournament $sets"
			: "update tournament $sets where uid=$qt";
	if($tournamentid<0)
	{	&log_event($'tournament_log,$ENV{'SCRIPT_NAME'},"$admin_user created $description");
	}
	#print "Q: $q<p>";
	
	&commandQuery($dbh,$q);
	if($tournamentid<0) { $tournamentid=$dbh->{ q{mysql_insertid}}; }
	&show_edit_tournament($dbh,$tournamentid,$admin);
	}
}
sub show_sendemail()
{	my ($dbh,$tournamentid,$admin) = @_;
	print "<!-- show_sendemail() --!>\n";
    &print_form_header(0,"/cgi-bin/tournament-email.cgi","sendmailform");
    print "<input type=hidden name='tournamentid' value='$tournamentid'>\n";
	print "</form>\n";
	print "<script>document.forms['sendmailform'].submit();</script>\n";
}

sub show_edit_tournament()
{	my ($dbh,$tournamentid,$admin) = @_;

	print "<!-- show_edit_tournament() --!>\n";

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
	
    &print_form_header($admin);
	print "<input type=hidden name=operation value=doedittournament>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
		print "<table>";

		print "<tr><td>"
			. &trans("status")
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

#
# print a form to edit a match
#
sub show_edit_match()
{	my ($dbh,$matchid,$fromname,$admin) = @_;
	#&printForm();
	
	print "<!-- show_edit_match() --!>\n";

	my $qm = $dbh->quote($matchid);
	#
	# the admin record comes from the matchrecord, whereas the player records 
	# come from matchparticipant records.  The 'A' in the admin select is a 
	# trick to make it sort last.
	#
	my $q = "select matchgroup.status,player,points,tournament,outcome,played,scheduled,"
				. " matchparticipant.comment,tournament_group,matchparticipant.uid"
				. " from matchparticipant left join matchgroup "
				. " on matchparticipant.tournament = matchgroup.uid and matchgroup.name = matchparticipant.tournament_group "
				. " where matchid=$qm "
				# note that because of the particular structure of this join query, $outcome seems to be padded with a lot of nulls
				. " union select matchgroup.status,-2,0,tournament,if(admin='winner',admin_winner,admin),played,scheduled,"
				. " matchrecord.comment,tournament_group,'A'"
				. " from matchrecord left join matchgroup "
				. "	on matchrecord.tournament = matchgroup.uid and matchgroup.name = matchrecord.tournament_group "
				. " where matchid=$qm "
				. " order by uid ";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $filled = 0;
	my $fromuid;
	my $fromoutcome;
	my $fromcomment;
	my $fromtournament;
	my $fromscheduled;
	my $scheduled;
	my @playeruids;
	my $closed = 0;
	#print "Q : $q<p>";
	
	&print_form_header($admin);
	if($admin)
	{	my $nrm = $nr-1;
		print "<input type=hidden name=nplayers value='$nrm'>\n";
	}
	if($nr>0)
	{	print "<table border=1>";
		print "<tr><td>who</td><td>outcome</td><td>points</td><td>last change</td><td align=center colspan=2>Comments</td>";
		my $pn = 0;
		while($nr-- > 0)
		{
		my ($status,$player,$points,$tournament,$outcome,$played,$scheduled,$comment,$tournament_group,$uid,$matchid) = &nextArrayRow($sth);

		# note that because of the particular structure of the join query, $outcome and $uid seem to be padded with a lot of nulls
		# this removes the nulls
		$outcome =~ s/\x00*$//g;
		$uid =~ s/\x00*$//g;
		$pn++;
		$closed = ($status eq 'closed');
		$fromtournament = $tournament;
			
		if(!$filled)
		{	#once we have the tournament, fill the name table 
			&fill_player_info($dbh,$tournament,'');
			$fromuid = ($admin&&($fromname eq '')) ? -2 : $lcuids{lc($fromname)};
			$filled = 1;
		}

		if($player == $fromuid) 
			{ $fromoutcome = $outcome; 
			  $fromcomment = $comment;
			  $fromscheduled = substr($scheduled,0,-3);
			}
		my $name1 = $names{$player};
		if($player>0) { push(@playeruids,$player); }
		if($admin || ($player>0))
		{
		#
		# print the non-editable part
		#
		my $p1name = $fromname;
		my $tz1_raw = $timezones{$fromuid};
		my $p2name = $names{$player};
		my $tz2_raw = $timezones{$player};
		my $tz1 = $tz1_raw;
		my $tz2 = $tz2_raw;
		if($tz1!="") { my $off = &timezoneOffsetString($tz1); $tz1 = "(GMT$off)"; }
		if($tz2!="") { my $off = &timezoneOffsetString($tz2); $tz2 = "(GMT$off)"; }
		my $p1namelink = ($player==-2) ? "$p2name" : "<a href=\"javascript:editlink('$p2name',0)\">$p2name</a>&nbsp;$tz2 ";
			
		print "<tr><td>$p1namelink</td>";
				
		if(!$admin && (($outcome eq '' || $outcome eq 'none')) && !$closed && !($player eq $fromuid))
		{
		# print time finder
		my $p1 = $fromname;
		my $p2 = $names{$player};
		my $t1 = $tz1_raw;
		my $t2 = $tz2_raw;
		print "<td colspan=2>";
		my $tr = &trans("Find a good time for #1 vs #2",$p1,$p2);
		print "<a href=\"javascript:meetlink('$p1','$p2','$t1','$t2',1)\">$tr</a>";
		print "</td>";
		}
		else
		{	my $out = $outcome;
			# if the outcome is a number,it's a player id
		    if($outcome =~ /\d/) {$out = "$names{$outcome} won"; }
			my $sched = "";
			my $outname = $out;
			if(($out eq 'none') || ($out eq 'scheduled'))
				{ $sched = "<br>" . substr($scheduled,0,-3);
				  if($out eq 'none') { $outname = 'proposed'; }
				}
			print "<td>$outname$sched</td>";
			if($admin)
			{	
				my $epoints = &encode_entities($points);
				print "<td>";
				if($uid > 0)
				{
				print "<input type=hidden name='old_points_${pn}' value='$epoints'>";
				print "<input type=text size=5 value='$epoints' name='points_${pn}'>";
				print "<input type=hidden name=rowid_${pn} value='$uid'>";
				print "<input type=hidden name=player_${pn} value='$player'>";
				}
				print "</td>\n";
				}
			else
			{	print "<td>$points</td>";
			}	
		}
		
		print "<td>" . substr($played,0,-3) . "</td><td colspan=2>$comment</td>";

		print "</tr>\n";
		}	# end of if admin
		}	# end of while $nr loop
		
		print "</table>";					    # left table end

		if($admin || !$closed)
		{
		print "<table>";						# right table start
		my $aa = $admin ? "[admin] " : "";
		print "<tr><td>${aa}Edit by $fromname";
		my $pw = &trans("Your Password");
		if(!$admin) { print "</td><td>$pw <input type=password name=passwd value='' SIZE=20 MAXLENGTH=25>"; }
		print "</td></tr>\n";
		{
		print "<tr><td>status</td>";
		print "<td><select name=outcome>";
		&print_option("none",$fromoutcome,'proposed');
		&print_option('cancelled',$fromoutcome,'cancelled');
		&print_option('scheduled',$fromoutcome,'scheduled');
		&print_option('draw',$fromoutcome,'draw');
		if($admin)
		{
		 while($#playeruids>=0)
		 {	my $pl = pop(@playeruids);
			my $name = $names{$pl};
			&print_option($pl,$fromoutcome,"$name won");
		 }
		}
		else
		{
		&print_option('win',$fromoutcome,"$fromname won");
		&print_option('loss',$fromoutcome,"$fromname lost");
		}
		print "</select>\n";
		print "</td></tr>";
		}
		print "<tr><td>date/time (GMT) </td>";
		print "<td><div master>";
  		print "<input name='scheduled' value ='$fromscheduled' class=input ";
		 print "onclick=javascript:rome(scheduled,options={appendTo:'parent'})>";
		print "</div></td>";
		print "</tr>\n";

		my $ent = &encode_entities($fromcomment);

		print "<tr><td colspan=2>comments:<br>";
		print "<textarea name=comment cols=60 rows=4>";
		print "$ent</textarea></td></tr>\n";
		print "<script type='text/javascript'>CKEDITOR.replace( 'comment' );</script>";


		print "</td></tr></table>\n";		# right table end
		print "<input type=hidden name=operation value=doeditmatch>\n";
		print "<input type=hidden name=matchid value=$matchid>\n";
		print "<input type=hidden name=tournamentid value=$fromtournament>";
		print "<input type=hidden name=fromname value=$fromname>\n";
		my $qpl = $dbh->quote($fromname);
		print "<br><input type=checkbox name='send_email' value='yes' checked>" . &trans("Send Emails") . "\n";
		print "<br><input type=submit value='edit match'>";
		print "</form>\n";

		}			
		else { print &trans("Results for this group are closed"); }
		
		print "</td></tr></table>";		# outer table end

    &print_form_header($admin);
	print "<input type=hidden name=operation value=subscribe>";
	print "<input type=hidden name=tournamentid value=$fromtournament>";
	print "<input type=submit value='back to match view'>";
	print "</form>\n";

  } #end of $nr

}
#
# change an individual match record
#
sub do_edit_match()
{	my ($dbh,$matchid,$tid,$playerid,$email,$admin) = @_;
	# playerid is the authenticated player id
	#&printForm();
	my $outcome = &param('outcome');
	my $qoutcome = $dbh->quote($outcome);
	my $com = &param('comment');
	my $qcom = $dbh->quote($com);
	my $qmatch = $dbh->quote($matchid);
	my $qpl = $dbh->quote($playerid);
	my $qtid = $dbh->quote($tid);
	my $sched = &param('scheduled');
	my $qsched = $dbh->quote($sched);
	if($admin)
	{	my $npla = &param('nplayers');
		while($npla>0)
		{
		my $points = &param("points_$npla");
		my $oldval = &param("old_points_$npla");
		if(!($points eq $oldval))
		{
		my $uid = &param("player_$npla");
		if($uid>0)
		{
		my $quid = $dbh->quote($uid);
		my $rowid = &param("rowid_$npla");
		my $qrowid = $dbh->quote($rowid);
		my $qpoints = $dbh->quote($points);
		my $q = "update matchparticipant set points=$qpoints where uid=$qrowid and player=$quid and tournament=$qtid";
		#print "Q $npla uid=$uid points=$points $q<p>";
		&commandQuery($dbh,$q);
		}}
		$npla--;
		}
	}
	
	if($admin && ($playerid eq -2))
	{
	my $win = "";
	if($outcome =~/\d/)
		 { my $winid = $dbh->quote($outcome);
		   $win = "admin_winner=$winid,";
		   $qoutcome="'winner'"; 
		 }
	my $q = "update matchrecord set admin=$qoutcome,"
		. "comment=$qcom,"
		. "scheduled=$qsched,"
		. $win
		. "played=CURRENT_TIMESTAMP"
		. " where matchid=$qmatch and tournament=$qtid";
	&commandQuery($dbh,$q);
	}
	else 
	{
	my $q = "update matchparticipant set outcome=$qoutcome,"
		. "comment=$qcom,"
		. "scheduled=$qsched,"
		. "played=CURRENT_TIMESTAMP"
		. " where matchid=$qmatch and player=$qpl and tournament=$qtid";
	&commandQuery($dbh,$q);
	#print "q: $q<br>";
	}
	if($email) { &sendMatchNotify($dbh,$matchid); } else { print "No emails sent<br>"; }

}


sub sendMatchNotify()
{	my ($dbh,$matchid,$msg) = @_;

	if($matchid>0)
	{
	my $qmid = $dbh->quote($matchid);
	my $q = "select player_name,e_mail,language from matchparticipant "
				. " left join players on matchparticipant.player = players.uid"
				. " where matchparticipant.matchid = $qmid " ;
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $lan = $lib'language;
	#print "Q: $nr $q<p>";
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
	my $q = "update matchrecord set matchstatus='ready' where matchid = $qm";
	&commandQuery($dbh,$q);
	}

}

sub delete_makeready_match()
{
	my ($dbh,$matchid) = @_;
	if ($matchid)
	{	
	my $qm = $dbh->quote($matchid);
	my $q = "delete from matchrecord where matchid=$qm";
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
	print "<!-- standings_table() --!>\n";
	my $q = 
		  " select matchparticipant.uid,matchid,player as pid,points,tournament_group,matchgroup.sortkey,player_name"
		. " from matchparticipant "
		. " left join matchgroup on matchparticipant.tournament=matchgroup.uid and matchparticipant.tournament_group = matchgroup.name "
		. " left join players on players.uid = player"
		. " where matchparticipant.tournament=$qtid and matchgroup.type='swiss'";
	my %cells;
	my %total;
	my @cols;
	my @rows;
	#print "Q: $q<p>";
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
	my $q = "select status from matchgroup left join matchrecord "
		. " on matchgroup.uid = matchrecord.tournament"
		. " and matchgroup.name=matchrecord.tournament_group and matchgroup.uid=matchrecord.tournament"
		. " where matchrecord.matchid = $qmatch";
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
  #&printForm();
  my $fromname = param('fromname');
  	 $fromname = &despace(substr($fromname,0,25));
  my $passwd = param('passwd');
	 $passwd = &despace(substr($passwd,0,25));
  my $tournamentid = param('tournamentid') || "0";
  my $subscribe = param('subscribe');

  
  #
  # key the admin process by a t2admin parameter or
  # by invalid (usually by timestamp) previous admin info
  #
  my $admininfo = param("admin");
  my $admin = param('t2admin');	# the value of "admin=xx" is the admin password

  if($admin) 
	{
	$admin_user = &param('admin_user');
	$admin_user_password = &param('admin_user_password');
	}
	
  # if the key exists, validate and maybe cause rechecking
  if($admininfo)  
	{ ($admin,$admin_user,$admin_user_password) = &validateAdminKey($admininfo);
	}
 
  
  my $dbh = &connect();
  if($dbh)
  { 
  &readtrans_db($dbh); 
  
  my $uid = 0;
  my $operation = &param('operation');
  my $group = &param('group');
  my $past = &param('past');
  if(lc($operation) eq 'subscribe') { $operation=''; };

  if($admin)
	{
	my $isnew = !$admininfo;
	my $adminok = $'tournament_password
					&& ($admin eq $'tournament_password)
					&& (&logon_admin($dbh,$admin,$admin_user,$admin_user_password)!=0);
	if($admin_user && ($isnew || !$adminok))
	{	my $su = $adminok ? "successful " : "FAILED ";
		&log_event($'tournament_log,$ENV{'SCRIPT_NAME'},"$su admin login by $admin_user");
	}
	if(!$adminok)
		{ if($admin eq $'tournament_password)
			{ $operation = 'getadminpassword'; 
			}
			else { $admin = ""; }
		}
	}

   if(&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0)
    { 	
	&print_header($admin); 
	if(($operation eq '') || ($operation eq 'showtournament'))
		{if($fromname && ($passwd || $admin)) 
			{ $uid = &processInputForm($dbh,$fromname,$passwd,$tournamentid,$subscribe,$admin);
			}
		&show_tournaments($dbh,$uid,$fromname,$passwd,$past,$tournamentid,$admin);
		}
	   elsif($operation eq 'getadminpassword')
	   {
	    if($admininfo)
	     {	 my $time = &trans("time expired");
			 print "<b>$time</b><br>"; 
	     }

		&show_getadminpassword($dbh,$admin);
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
			my $editok = ($admin&&!$fromname) ? -2 : &logonOk($dbh,$fromname,$passwd,$admin);
			my $closed = !$admin && &matchclosed($dbh,$matchid);
			my $email = param('send_email');
			if($editok)
			{
			    if(!$closed)
			    {
				&do_edit_match($dbh,$matchid,$tournamentid,$editok,$email,$admin);
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
				my $editok = $passwd && ($admin	? -1 : &logonOk($dbh,$fromname,$passwd));
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
	   elsif($admin && ($operation eq 'sendemail'))
	   {	&show_sendemail($dbh,$tournamentid,$admin);
	   }
	   else
	   {	print "Operation $operation not implemented<br>\n";
	   }
	  if($admin && !($operation eq 'getadminpassword'))
	  {	&show_admin_panel($dbh,$admin,$tournamentid,$operation);
	  }
    }
  &tournament_footer();
  }
}


do_tournamentboard();