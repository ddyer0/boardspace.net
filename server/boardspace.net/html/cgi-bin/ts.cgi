#!/usr/bin/perl
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
	my $jr = $variation ? " left join ranking  on participant.pid = ranking.uid and ranking.variation=$qvar  " : "";
	my $jo = $variation ? "rank desc, " : "";
	my $jw = $variation ? " and (ranking.variation=$qvar or ranking.variation is null) " : "";
	my $jv = $variation ? " ,ranking.value as rank " : "";
    my $q = "SELECT player_name,players.uid,e_mail,e_mail_bounce $jv from participant "
		. " left join players on participant.pid = players.uid "
		. " $jr where tid=$qt $jw"
		. " order by $jo player_name asc";
#   print "$tournament $variation $forced Q: $q<br>";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);

	while($nr-- > 0)
	{
	my ($name,$uid,$email,$bounce, $val) = &nextArrayRow($sth);
	my $textnotes = "";
	$names{$uid} = $name;
	$uids{$name} = $uid;
	$emails{$uid} = $email;
	if($variation) { if(!$val) { $val = "--"; } $ranks{$uid} = $val;}
	if($bounce>0) { $textnotes = "<b>(bounce)</b>"; }
	my $quid = $dbh->quote($uid);
	my $subq = "select variation,games_played,value from ranking where uid=$quid";
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
{	&standard_header();
	my $tmh = &trans("Boardspace.net Tournament Manager");
	my $tm = &trans("Tournament Manager");
	my $use = &trans("use this page to sign up for scheduled tournaments, check status of active tournaments, and report match results.");
	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>$tmh</TITLE>
<center><H2>$tm</h2>
<br>$use
</center>
<p>
Head_end
}

#
# this logon doesn't require a password if $admin is matches the tournament_admin password
#
sub logon 
{
	my ($dbh,$pname,$passwd,$admin) = @_;
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $bannercookie = &bannerCookie();
	if(&allow_ip_login($dbh,$myaddr)
     && &allow_ip_login($dbh,$bannercookie))
	{
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $pw = $admin ? "" : "AND password=$slashpwd ";
	my $sth = &query($dbh,"SELECT uid FROM players WHERE player_name=$slashpname $pw AND status='ok'");
	my $n = &numRows($sth);
    my $val=0;
  
	if($n == 1)
    { $val = &nextArrayRow($sth);
    }
	if(!$val)
		{
		&note_failed_login($dbh,$myaddr,"IP: Tournament as $pname");
        &note_failed_login($dbh,$bannercookie,"CK: Tournament as $pname");
		}
	&finishQuery($sth);
	return $val;
	}
	else
	{ return(0);
	}
}

#
# list the active or scheduled tournaments.  
# If a particular tournament id is supplied, list details
# if admin is supplied, add more editing options
#
sub show_tournaments
{ my ($dbh,$uid,$fromname,$passwd,$historic,$tid,$admin) = @_;
  my $qu = $uid>0 ? $uid : -1;
  my $tidclause = ($tid>0) ? " and tournament.uid=$tid " : "";
  my $cond = $historic ? "(status='finished')" : "(status='completed' or status='signup' or status='active')";
  my $q = "SELECT status,uid,description,count(pid),sum(if(pid=$qu,1,0)),start,longdescription from tournament left join participant "
              . " on uid=tid where $cond $tidclause group by uid order by start desc";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  my $activestatus = 0;
	#print $q;
  if($n>0)
    { print "<table>";
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
		 print "<td><a href=$ENV{'SCRIPT_NAME'}?admin=$admin&operation=edittournament&tournamentid=$thistid>Edit&nbsp;</a></td>";
	   }
       if($uid>0) { my $yes = ($myplayers==1) ? "Yes" : "";  print "<td>$yes</td>" }
       print"<td>$nplayers</td><td>";
       my $qpname = encode_entities($fromname);
       my $qpasswd = encode_entities($passwd);
	   my $adm= $admin ? "&admin=$admin" : "";
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
	 print "<td colspan=2><a href=$ENV{'SCRIPT_NAME'}?admin=$admin&operation=&tournamentid=-1>$pa</a></td>";

	 }
	 else
	 {
	 my $pa = &trans('Past Tournaments');
	 print "<td colspan=2><a href=$ENV{'SCRIPT_NAME'}?admin=$admin&operation=&tournamentid=-1&past=1>$pa</a></td>";
	 }
	 if($admin && ($tid<=0))
	 { print "<td>";
	   print "<td><a href=$ENV{'SCRIPT_NAME'}?admin=$admin&operation=edittournament&tournamentid=-1>New&nbsp;</a></td>";
	   print "</td>\n";
	 }
	 print "</tr>";
	}
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
  if($admin) { print "<input type=hidden name=admin value=$admin>\n"; }
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

sub processInputForm
{  my ($dbh,$fromname,$passwd,$tournamentid,$subscribe,$admin) = @_;
   my $uid = &logon($dbh,$fromname,$passwd,$admin);
   if($uid>0)
   { if($tournamentid>0)
     {if($subscribe)
       {
        my $q = "replace into participant set tid=$tournamentid,pid=$uid";
        &commandQuery($dbh,$q);  
       }
       else
       {my $q = "delete from participant where tid=$tournamentid and pid=$uid";
         &commandQuery($dbh,$q);  
      }
     &commandQuery($dbh,"DELETE from notes where uid=$uid");
     }
     return($uid);
   }
   else
  {  print "<b>Sorry, name and password not found in our player list</b><p>";
    return(0);
  } 
}

sub get_tournament_variation()
{	my ($dbh,$tournament) = @_;
	my $qt = $dbh->quote($tournament);
	my $q = "select variation from tournament where uid=$qt";
	my $sth = &query($dbh,$q);
	my ($var) = &nextArrayRow($sth);
	&finishQuery($sth);
	return($var);
}
sub show_player_info()
{	my ($dbh,$tournament) = @_;
	my $var = &get_tournament_variation($dbh,$tournament);
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
	print "<option value=-1>TBA</option>\n";
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
{	my ($str,$name1,$name2) = @_;
	return(($str eq 'player1') ? "<b>$name1</b> won"
		: ($str eq 'player2') ? "<b>$name2</b> won"
		: $str);
}

sub show_filled_match_form()
{	my ($admin,$row,$matchid,$pl1,$name1,$pl2,$name2,
		$outcome1,$outcome2,$final,$played,$comment,$comment1,$comment2,@ids) = @_;
	my $en1 = &uri_escape($name1);
	my $en2 = &uri_escape($name2);
	my $plname1 = $names{$pl1};
	my $plname2 = $names{$pl2};
	print "<tr><td>\n";
	if($admin)
	{
	print "del <input type=checkbox name=delete_$row>\n";
	print "</td><td>";
	print "<a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname1&admin=$admin&matchid=$matchid>edit</a>";	
	print "</td><td>";
	&print_player_selector("match1_$row",$pl1,$name1,@ids);
	print "</td><td>";
	&print_player_selector("match2_$row",$pl2,$name2,@ids);
	print "</td><td>";
	}
	else
	{
	my $p1msg = ($pl1 == -1) ? "$name1</td>"
		:"<a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname1&matchid=$matchid>$name1</td>";
	print $p1msg;

	my $p2msg = ($pl2 == -1) ? "<td>$name2</td>"
			: "<td><a href=$ENV{'SCRIPT_NAME'}?operation=editmatch&fromname=$plname2&matchid=$matchid>$name2</td>";
	print $p2msg;
	}
	{
	print "<td>\n";
	my $outcome1r = &resultString($outcome1,$name1,$name2);
	my $outcome2r = &resultString($outcome2,$name1,$name2);
	my $outcome = !($final eq 'none') ? &resultString($final,$name1,$name2)
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
	my $en1 = &uri_escape($plname1);
	print "<tr><td>";
	if($admin)
	{
	print "del <input type=checkbox name=delete_$row>\n";
	print "</td><td>";
	}
	print "<b>$plname1</b></td><td>\n";
	print "<input name=password_$row type=password>";
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
	my $qsort = $dbh->quote($sortkey);
	%hasmatch = ();
	if($admin)
	{
    &print_form_header();
	print "<table>\n";
	print "<input type=hidden name=operation value=creatematch>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
	print "<input type=hidden name=firstmatch value=$nrows>\n";
	print "<input type=hidden name=group value=$qgroup>\n";
	}
	my $or = $admin ? "played desc " : " matchid ";
	{
	my $q = "select player1,player2,outcome1,outcome2,admin,played,comment,comment1,comment2,matchid from matchrecord"
		. " where (matchstatus!='notready') and tournament=$qturn and tournament_group=$qgroup order by $or";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my %found;
	#
	# show existing matches
	#
	if($nr>0)
	{

	my $nored = $admin 
			? "" 
			: "<br>" . &trans("Click on your player name to Edit");
	my $gg = $qgroup;
	my $sk = "";
	if($admin)
	{
	$sk = &trans("sort key: #1","<input type=text name=sortkey value=$qsort size=8>");
	}
	print "<p><table border=1><caption><b>"
		. $sk
		. &trans("Current Matches in group #1",$gg)
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
	my ($pl1,$pl2,$outcome1,$outcome2,$adminoutcome,$played,$comment,$comment1,$comment2,$matchid) = &nextArrayRow($sth);
	my $name1 = $names{$pl1};
	my $name2 = $names{$pl2};
	$hasmatch{$pl1} = 1;
	$hasmatch{$pl2} = 1;
	if($name1 && $found{$pl1}) { $name1 = "$name1 (duplicate)"; }
	if($name2 && $found{$pl2}) { $name2 = "$name2 (duplicate)"; }
	if($pl1 == -1) { $name1 = "TBA"; }
	if($pl2 == -1) { $name2 = "TBA"; }
	$found{$pl1}=$pl1;
	$found{$pl2}=$pl2;
	&show_filled_match_form($admin,$nrows++,$matchid,$pl1,$name1,$pl2,$name2,$outcome1,
							$outcome2,$adminoutcome,$played,$comment,$comment1,$comment2,@ids);
	}

	print "<tr><td colspan=6>";
	if($admin)
	{	print "<textarea rows=4 cols=80 name='comment_$group'>";
		$mcomment = &encode_entities($mcomment);
	}
	print $mcomment;
	if($admin)
	{	print "</textarea>";
	}
	print "</td></tr>";

	print "</table>\n";
	}
	&finishQuery($sth);
	print "<input type=hidden name=lastmatch value=$nrows>\n";
	} # end of main match list


	{

	my $q = "select player1,comment1,matchid from matchrecord"
		. " where (matchstatus='notready') and tournament=$qturn and tournament_group=$qgroup order by $or";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $row = 0;
	my %found;
	if($nr>0)
	{
	print "<p>";
	print &trans("confirm-tournament");
	print "<table>";
	while($row < $nr)
	{
	 $row++;
	 my ($pl1,$comment1,$matchid) = &nextArrayRow($sth);
	 my $name1 = $names{$pl1};
	 if($row==1)
	 {
	 my $pl = &trans("Player");
	 my $pw = &trans("Your Password");
	 print "<tr><td><b>$pl</b></td><td><b>$pw</b></td><td></td></tr>\n"; 

	 }
	 &show_notready_match_form($admin,$row,$matchid,$pl1,$comment1);
	}
	&finishQuery($sth);
	print "</table>";
	print "<input type=hidden name=lastnotready value=$row>\n";
	}}

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
	my $variation = &get_tournament_variation($dbh,$tournamentid);
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
	my $variation = &get_tournament_variation($dbh,$tournamentid);
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

	  print "<input type=hidden name=admin value=$admin>\n";
	  print "<input type=checkbox name='delete_$group'>\n";
	  print "check to delete group $qgroup<br>\n";

	  print "<input type=submit value='Edit Matches'>\n";
	  print "</form>\n";
	}
}


sub show_current_matches()
{	my ($dbh,$tid,$admin) = @_;
	my $qtid = $dbh->quote($tid);
	my $q = "select name,type,status,comment,sortkey from matchgroup where uid=$qtid order by status,name";
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
    print "<input type=hidden name=admin value=$admin>\n";
    print "<br>" . &trans("Create New Group") . ":";
	print "<input type=text name=creategroup value=''>\n";
	print &trans("sort key: #1","<input type=text name=sortkey value='MMM'>\n");
	print "<input type=hidden name=operation value='creatematch'>\n";
	print "<input type=hidden name=tournamentid value=$tid>\n";
	print "<input type=submit value='create group'>\n";
	}
}

sub creatematches()
{	my ($dbh,$tournamentid,$group,$sortkey,$admin)=@_;
	my @ids = &fill_player_info($dbh,$tournamentid,'');
	my $qtour = $dbh->quote($tournamentid);
	my $qgroup = $dbh->quote($group);

	my $firstex = &param('firstmatch');
	my $lastex = &param('lastmatch');
	while($firstex < $lastex)
	{
		my $del = &param("delete_$firstex");
		my $edit = &param("edit_$firstex");
		my $pl1 = &param("match1_$firstex");
		my $pl2 = &param("match2_$firstex");
		my $match = &param("match_$firstex");
		my $qmatch = $dbh->quote($match);
		my $qpl1 = $dbh->quote($pl1);
		my $qpl2 = $dbh->quote($pl2);
		if($del) 
		{ my $q = "delete from matchrecord where matchid=$qmatch";
		  &commandQuery($dbh,$q); 
		  #print "del $q<p>";
		}
		else
		{	my $q = "update matchrecord set player1=$qpl1,player2=$qpl2 where matchid=$qmatch";
			&commandQuery($dbh,$q);
			#print "upd $q<br>";
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

	my $q = "INSERT INTO matchrecord SET player1=$qpl1,player2=-1, matchstatus='notready' ,tournament=$qtour,tournament_group=$qgroup";
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
		my $q = "INSERT INTO matchrecord SET player1=$qpl1,player2=$qpl2,tournament=$qtour,tournament_group=$qgroup";
		&commandQuery($dbh,$q);
		}
	$first++;
	}}
	my $mcomment = &param("comment_$group");
	if($mcomment || $sortkey)
	{
	my $qm = $dbh->quote($mcomment);
	my $qsort = $dbh->quote($sortkey);
	my $q = "update matchgroup set comment=$qm,sortkey=$qsort where uid=$qtour and name=$qgroup";
	#print "Q: $q<p>";
	&commandQuery($dbh,$q);
	}
	if(param("delete_$group")) 
	{	print "Delete match $qtour group qgroup<br>\n";
		&commandQuery($dbh,"delete from matchrecord where tournament=$qtour and tournament_group=$qgroup");
		&commandQuery($dbh,"delete from matchgroup where uid=$qtour and name=$qgroup");
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
	print "<input type=hidden name=admin value=$admin>\n";
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
	  &commandQuery($dbh,"delete from matchrecord where tournament=$qt");
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
	my $sets = "set status=$status, longdescription=$longdescription,"
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
	my $q = "select status,longdescription,start,end,variation,format,description "
		. " from tournament where uid=$qt";
	my @allstat = &get_enum_choices($dbh,'tournament','status');
	my @allformat = &get_enum_choices($dbh,'tournament','format');
	my @allvars = &all_variations($dbh);
	my $sth = &query($dbh,$q);
	if($sth)
	{
	my ($status,$longdescription,$start,$end,$variation,$format,$description)
				= &nextArrayRow($sth);
	
    &print_form_header();
	print "<input type=hidden name=admin value=$admin>\n";
	print "<input type=hidden name=operation value=doedittournament>\n";
	print "<input type=hidden name=tournamentid value=$tournamentid>\n";
		print "<table>";

		print "<tr><td>"
			. &trans("Status")
			. ":</td><td>";
		&print_selector('tournament_status',$status,@allstat);
		print "</td></tr>";

		print "<tr><td>"
			. &trans("Format")
			. ":</td><td>";
		&print_selector('tournament_format',$format,@allformat);
		print "</td></tr>\n";

		print "<tr><td>Variation</td><td>";
		&print_selector('tournament_variation',$variation,@allvars);
		print "</td></tr>\n";

		print "<tr><td>Start</td><td><input type=text name=tournament_start value='$start'></td></tr>\n";

		print "<tr><td>End:</td><td><input type=text name=tournament_end value='$end'></td></tr>\n";

		print "<tr><td>Short:</td><td>";
		my $sh = &encode_entities($description);
		print "<input name=tournament_short size=80 value='$sh'>";
		print "</td></tr>\n";

		print "<tr><td colspan=2><textarea rows=4 cols=80 name=tournament_long>";
		print &encode_entities($longdescription);
		print "</textarea></td></tr>";
	
		print "</table>\n";
	print "<input type=checkbox name=tournament_delete>delete this tournament<br>";
	print "<input type=submit>";
	print "</form>";
	}
}

sub show_edit_match()
{	my ($dbh,$matchid,$fromname,$admin) = @_;
	my $qm = $dbh->quote($matchid);
	my $q = "select player1,player2,tournament,outcome1,outcome2,admin,played,comment,comment1,comment2,tournament_group"
		. " from matchrecord where matchid=$qm";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr)
	{	my ($player1,$player2,$tournament,$outcome1,$outcome2,$adminoutcome,$played,$comment,$comment1,$comment2,$tournament_group)
				= &nextArrayRow($sth);
		&fill_player_info($dbh,$tournament,'');
		my $name1 = $names{$player1};
		my $name2 = $names{$player2};
		my $fromuid = $admin ? -1 : $uids{lc($fromname)};
		if($admin) { $fromname = '[admin]'; };

		#
		# print the non-editable part
		#
		my $outcome1res = &resultString($outcome1,$name1,$name2);
		my $outcome2res = &resultString($outcome2,$name1,$name2);
		my $adminstr = &resultString($adminoutcome,$name1,$name2);
		print "<table border=1>\n";			    # left table start
		print "<tr><td>who</td><td>$names{$player1} vs $names{$player2}</td><td align=center>Comments</td></tr>";
		print "<tr><td>result from $name1</td><td>$outcome1res</td><td>$comment1</td></tr>\n";
		print "<tr><td>result from $name2</td><td>$outcome2res</td><td>$comment2</td></tr>\n";
		print "<tr><td>result from [admin]</td><td>$adminstr</td><td>$comment</td></tr>\n";
		print "<tr><td>last edit</td><td>$played</td></tr>\n";
		print "</table>";					    # left table end

		print "<table><tr><td align=top>";		# outer table start
		print "</td><td>";
		#
		# print the editable part
		#
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
		print "<tr><td colspan=2>comments:<br>";
		print "<textarea name=comment cols=60 rows=4>";
		print "$ent</textarea></td></tr>\n";


		print "</td></tr></table>\n";		# right table end
		print "<input type=hidden name=operation value=doeditmatch>\n";
		print "<input type=hidden name=matchid value=$matchid>\n";
		print "<input type=hidden name=fromname value=$fromname>\n";
		my $qpl = $dbh->quote($fromname);
		if($admin)
		{
		print "<input type=hidden name=admin value=$admin>\n";
		}
		print "<input type=submit value='edit match'>";
		print "</form>\n";

		print "</td></tr></table>";		# outer table end

    &print_form_header();
	print "<input type=hidden name=operation value=subscribe>";
	print "<input type=hidden name=tournamentid value=$tournament>";
	print "<input type=submit value='back to match view'>";
	if($admin) { print "<input type=hidden name=admin value=$admin>";}
	print "</form>\n";

	}


}

sub do_edit_match()
{	my ($dbh,$matchid,$playerid,$admin) = @_;
	# playerid is the authenticated player id
	my $outcome = &param('outcome');
	my $qoutcome = $dbh->quote($outcome);
	my $com = &param('comment');
	my $qcom = $dbh->quote($com);
	my $qmatch = $dbh->quote($matchid);
	my $qpl = $dbh->quote($playerid);
	my $out1 = $admin ? "outcome1" : "if(($qpl = player1),$qoutcome,outcome1)";
	my $out2 = $admin ? "outcome2" : "if(($qpl = player2),$qoutcome,outcome2)";
	my $out3 = $admin ? "admin=$qoutcome," : "";
	my $pmatch = $admin ? "" : "and ((player1 = $qpl) or (player2 = $qpl))";
	my $acom = $admin ? "comment=$qcom," : "";
	my $q = "update matchrecord set outcome1=$out1,"
		. "outcome2= $out2,"
		. "$out3"
		. "comment1 = if(($qpl = player1),$qcom,comment1), "
		. "comment2 = if(($qpl = player2),$qcom,comment2), "
		. "$acom "
		. "played=CURRENT_TIMESTAMP"
		. " where matchid=$qmatch $pmatch";
	&commandQuery($dbh,$q);


}

sub tournament_footer 
{	
	my $home = &trans('BoardSpace.net home page');
	my $goto = &trans('Go to');
	my $email = &trans('E-mail:');
	my $linklink = &trans('IAGO World Tour');
	my $link = "<a href='http://abstractgamers.org/iwt.htm'>$linklink</a>";
	my $linkbody = &trans('IAGO World Tour #1 Description',$link);
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

# --------------------------------------------
&init();

print header;

#__d( "tournament...");

sub do_tournamentboard()
{
  my $fromname = param('fromname');
  my $passwd = param('passwd');
  my $tournamentid = param('tournamentid') || "0";
  my $subscribe = param('subscribe');
  my $admin = param('admin');	# the value of "admin=xx" is the admin password
								# which is taken from include.pl.  All the local
								# functions are passed either this password or null
  my $dbh = &connect();
  my $uid = 0;
  my $operation = &param('operation');
  my $group = &param('group');
  my $past = &param('past');
  if(lc($operation) eq 'subscribe') { $operation=''; };
  if($dbh)
    { &readtrans_db($dbh);
	  &print_header(); 
	 if($admin && (!$'tournament_password || !($admin eq $'tournament_password))) 
		{ $admin = ''; 
		  print "<b>Incorrect admin password<b><p>\n";
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
	   elsif ($operation eq 'creatematch')
	   {	my $sortkey = param('sortkey');
			&creatematches($dbh,$tournamentid,$group,$sortkey,$admin);
		    &show_tournaments($dbh,$uid,$fromname,$passwd,0,$tournamentid,$admin);
	   }
	   elsif ($operation eq 'doeditmatch')
	   {
			my $editok = $admin	? -1 : &logon($dbh,$fromname,$passwd,$admin);
			my $matchid = param('matchid');
			if($editok)
			{
			&do_edit_match($dbh,$matchid,$editok,$admin);
			}
			elsif($fromname && $passwd && !$admin)
			{	print "Sorry, wrong password<p>\n";
			}
			&show_edit_match($dbh,$matchid,$fromname,$admin);
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
