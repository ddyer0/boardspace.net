#!/usr/bin/perl
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );
use strict;

require "../include.pl";
require "gs_db.pl";


#
# keep the master system balanced at 1500 by awarding bonus points or deficit points
# which are awarded to the winner of a master game.
#
sub lcheck_bonus()
{	my ($dbh,$game) = @_;
	my $active = time()-60*60*24*30*($'retire_months);
  my $query = "select 1500-sum(value)/count(value),sum(value),count(value) from players left join ranking\n "
        . " on players.uid=ranking.uid and ranking.variation='$game'"
	. "  where ranking.variation='$game' AND games_won>=$'bonus_games AND status='ok' AND (ranking.last_played>$active)";

	my $sth = &query($dbh,$query);
	my ($deficit,$sum,$count) = &nextArrayRow($sth);
  if($count==0) { $count=1; }
	my $avg = $sum/$count;
	print "Query $query\n Deficit $deficit sum=$sum n=$count avg=$avg\n";

	if($deficit>4) { $deficit=4; }
	elsif($deficit>3) { $deficit=3; }
	elsif($deficit>2) { $deficit=2; }
	elsif($deficit>1) { $deficit=1; }
	elsif($deficit<-3) { $deficit=-3; }
	elsif($deficit<-2) { $deficit=-2; }
	elsif($deficit<-1) { $deficit=-1; }
	else { $deficit=0; }
	 
	 &finishQuery($sth);
	 return($deficit);
}
print header;
print "<pre>";

my $dbh = &connect();

if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
{
my @choices = &all_variations($dbh);
for my $choice (@choices)
{	my $b = &lcheck_bonus($dbh,$choice);
	print "<b>$choice</b> bonus is $b\n";
}}

print "</pre>";
&disconnect($dbh);
#
