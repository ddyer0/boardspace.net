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
require "tlib/show_activity.pl";
use Crypt::Tea;


# --------------------------------------------

print header;

#__d( "meetup...");


sub print_header 
{	
	&standard_header();
	my $tmh = &trans("Boardspace.net Game Time Finder");
	my $tm = &trans("Meeting time finder");
	my $p1 = &param('player1');
	my $p2 = &param('player2'); 
	my $use = ($p1 && $p2) 
		? &trans("use this page to find a time for #1 and #2 to meet and play",$p1,$p2) 
		: &trans("use this page to find a time to play");
	my $am = "";
	my $use2 = &trans("when you have found a time, email your opponent with a proposal");
	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>$tmh</TITLE>
$am
</HEAD>
<center><H2>$tm</h2>
<br>$use
<br>$use2
</center>
<p>
Head_end
}
sub print_timezone_selector()
{	my ($name,$zone) = @_;
	print "<select name='$name'>\n";
	print &select_option(&trans('use best guess'),'',$zone);
	for(my $off = -12; $off<=12; $off++)
	{	my $val =  - $off*60;
		my $str = &timezoneOffsetString($val);
		print &select_option("GMT$str",$val,$zone);
	}
	print "</select>\n";

}
sub print_meetup_form()
{
	my ($dbh,$player1,$player2,$player1zone,$player2zone,$language) = @_;

	if($player1zone eq '' && $player1)
	{my ($p1uid,$p1zone) = &getPlayerInfo($dbh,$player1);
	 $player1zone = $p1zone;
	}
	if($player2zone eq '' && $player2)
	{
	 my ($p2uid,$p2zone) = &getPlayerInfo($dbh,$player2);
	 $player2zone = $p2zone;
	}

	print "<form $ENV{'SCRIPT_NAME'} method=POST>\n";
	print "<input type=hidden name=language value='$language'>\n";
	print "<table><tr><td>";
	print &trans("Your player name");
	print "</td><td>";
	print "<input type = text name = 'player1' value='$player1'><br>\n";
	print "<input type=hidden name = 'originalplayer1' value='$player1'>\n";
	print "</td><td>";
	print &trans("Your timezone");
	print "</td><td>";
	&print_timezone_selector('player1zone',$player1zone);
	print "<input type=hidden name='originalplayer1zone' value='$player1zone'>\n";
	print "</td></tr><tr><td>";
	
	print &trans("Opponent's player name");
	print "</td><td>";
	print "<input type = text name = 'player2' value='$player2'><br>\n";
	print "<input type=hidden name = 'originalplayer2' value='$player2'>\n";

	print "</td><td>";
	print &trans("Opponent's timezone");
	print "</td><td>";
	&print_timezone_selector('player2zone',$player2zone);
	print "<input type=hidden name='originalplayer2zone' value='$player2zone'>\n";
	print "</td></tr></table>";
	print "<input type = submit name='do it'>\n";
	
	print "</form>";
	
}
#
# get a players uid and last known timezone
#
sub getPlayerInfo()
{
	my ($dbh,$player1) = @_;
	my $qp1 = $dbh->quote($player1);
	my $q = "select uid,timezone_offset from players where player_name = $qp1";
	my $sth = &query($dbh,$q);
	my ($uid,$zone) = &nextArrayRow($sth);
	if(!$uid) { print &trans("Player #1 doesn't exist",$player1) . "<br>\n"; }
	return($uid,$zone);
}

sub show_meetup_charts()
{
	my ($dbh,$nmonths,$player1,$player2,$p1zone,$p2zone) = @_;
	my ($p1uid,$dbzone1) = &getPlayerInfo($dbh,$player1);
	my ($p2uid,$dbzone2) = &getPlayerInfo($dbh,$player2);
	if($p1zone eq '') { $p1zone = $dbzone1; }
	if($p2zone eq '') { $p2zone = $dbzone2; }
	if($p1uid && $p2uid)
	{	
    my $minus1 = &timezoneOffsetString($p1zone);
    my $minus2 = &timezoneOffsetString($p2zone);
	my ($stack1,$max1,$tot1) = &get_activity_table($dbh,$p1uid,$nmonths,$p1zone,0);
	my $cap1 = &trans("Games played by #1, local time is GMT#2",$player1,$minus1);
	my ($stack2,$max2,$tot2) = &get_activity_table($dbh,$p2uid,$nmonths,$p1zone,0);
	my $cap2 = &trans("Games played by #2 in GMT#3, shown as local time for #1",$player1,$player2,$minus2);
	my ($stack3,$max3,$tot3) = &get_combined_tables($stack1,$max1,$tot1,$stack2,$max2,$tot2,$p1zone);
	my $cap3 = &trans("Shared times for #1 and #2 as local time for #3",$player1,$player2,$player1);
	my $local1 = &trans("local time for #1",$player1);
	my $local2 = &trans("local time for #1",$player2);
	print "<table>";
	print "<tr><td>";
	print &getTimeTable($stack3,$max3,$p1zone,$cap3,0,$tot3,$local1,'12hour',$p2zone,$local2);
	print "</td><td>";
	print &trans("Hover over a square to see the times that square represents");
	print "<br>";
	print &trans("Click to get an editable text window");
	print "</td></tr><tr><td>";
	print "</tr><tr>";
	print "<td>";
	print &getTimeTable($stack1,$max1,$p1zone,$cap1,0,$tot1,$local1,'hour',$p2zone,$local2);
	print "</td><td>";
	print &getTimeTable($stack2,$max2,$p1zone,$cap2,0,$tot2,$local1,'hour',$p2zone,$local2);
	print "</td></tr>";
	print "</table>";

	}
}
sub do_meetup()
{ my $language = &select_language(&param('language'));
  my $player1 = param('player1');
  my $player2 = param('player2');
  my $player1zone = param('player1zone');
  my $player2zone = param('player2zone');
  my $originalPlayer1 = param('originalplayer1');
  my $originalPlayer2 = param('originalplayer2');
  my $originalPlayer1zone = param('originalplayer1zone');
  my $originalPlayer2zone = param('originalplayer2zone');
  
  if( !($player1 eq $originalPlayer1) && ($player1zone eq $originalPlayer1zone)) { $player1zone = ''; }
  if( !($player2 eq $originalPlayer2) && ($player2zone eq $originalPlayer2zone)) { $player2zone = ''; }
  
  my $months = param('months');
  my $dbh = &connect();
  if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  { 
  &readtrans_db($dbh,$language);
      
  &print_header();
  
  if($player1 && $player2)
  {
  &show_meetup_charts($dbh,$months,$player1,$player2,$player1zone,$player2zone);
  }
  &print_meetup_form($dbh,$player1,$player2,$player1zone,$player2zone,$language);
  &standard_footer();
  }
  &disconnect($dbh);
}
 
#__dend();
  

do_meetup();
