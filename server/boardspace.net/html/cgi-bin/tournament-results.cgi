#!/usr/bin/perl
#
# this is a form for extracting tournament results as tables or csv
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

print header;


sub print_tournament_selector()
{
	my ($dbh) = @_;
	my $q = "SELECT status,uid,description from tournament "
              . " where (status='completed' or status='signup' or status='active')"
	      . " order by uid desc";	
	my $sth = &query($dbh,$q);
  	my $n = &numRows($sth);
  	my $activestatus = 0;
	print &trans("select which tournament");
	print "<select name=tournamentid >";
	while($n-- > 0)
	{
	my ($status,$uid,$description) = &nextArrayRow($sth);
	print "<option value=$uid>$description ($status)</option>\n";
	}
	print "</select>";
	&finishQuery($sth);
}

sub print_tournament_results()
{
	my ($dbh,$tournamentid,$raw) = @_;
	my $qtid = $dbh->quote($tournamentid);
	my $q = "select player_name,players.uid,team,team.name,tournament_group,outcome,points from matchparticipant"
			. " left join participant on participant.pid = matchparticipant.player and participant.tid=matchparticipant.tournament"
			. " left join players on participant.pid = players.uid "
			. " left join team on team.uid=participant.team"
			. " where tid=$qtid"
			. " order by tournament_group,team"
		;
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	if(!$raw)
	{
	print "<table><tr><td>name</td><td>uid</td><td>team</td><td>group</td><td>outcome</td><td>points</td></tr>";
	}
	while ($n-- > 0)
	{
	my ($pname,$uid,$team,$teamname,$group,$outcome,$points) = &nextArrayRow($sth);
	if($raw) {  print "$pname,$uid,$teamname,$group,$outcome,$points<br>"; }
	else {
	 print "<tr><td>$pname</td><td>$uid</td><td>$teamname</td><td>$group</td><td>$outcome</td><td>$points</td></tr>\n";
	}
	}
	&finishQuery($sth);
	if(!$raw) { print "</table>"; }
}
sub print_header 
{	my () = @_;
	&standard_header();
	my $tmh = &trans("Boardspace.net Tournament Results");
	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>$tmh</TITLE>
</HEAD>
<center><H2>$tmh</h2>
</center>
<p>
Head_end
}

#__d( "show tournament results...");

sub do_tournamentboard()
{ 
  #&printForm();
  my $tournamentid = param('tournamentid') || "0";
  my $raw = param('rawformat');
  my $myaddr = $ENV{'REMOTE_ADDR'};
  my $dbh = &connect();
  if($dbh && &allow_ip_access($dbh,$myaddr)>=0)
  { 
  &readtrans_db($dbh); 

  if(!$raw) { &print_header(); }

  if($tournamentid eq "0")
	 {
	 print "<form>";
	 &print_tournament_selector($dbh);
	 print "<br><input type=checkbox name=rawformat>raw csv format";
	 print "<br><input type=submit value='show results' >\n";
	 print "</form>";
	 }
  	else {
	 &print_tournament_results($dbh,$tournamentid,$raw);
	}
  }
}


do_tournamentboard();
