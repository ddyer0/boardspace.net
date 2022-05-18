#!/usr/bin/perl 
#
# tantrix popularity by country
#
#
# optional parameters:
#
# months=6                        number of months inactive before "retired"
# retired=0                       =1 for show retired players instead of active
# nitems=100                      =n to show top n players
#
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );
use Mysql;
use Debug;

use strict;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/ppm-table.pl";
sub init 
{
   $| = 1;                         # force writes
}

sub rank_header()
{	my ($nitems,$retired,$league)=@_;
	my $ccode ="";
 	my $off = ($retired==1) ?"Emeritus" :""; 
	my $retired_league = &trans("The following table shows the ranking of retired players by country, ie players who have not played withing the past three months.");
	my $active_league = &trans("The following table shows the ranking of players by country.");
	my $retired_normal = &trans("The following table shows the number of <i>retired</i> players by country.");
	my $active_normal = &trans("The following table compares the per capita % of Boardspace players for countries with at least three active players. This percentage is indicative of Boardspace popularity in the appropriate country.");
	my $active = $league 
			? (($retired==1) ? $retired_league : $active_league)
			: (($retired==1) ? $retired_normal : $active_normal);
	
#  if($header eq "") 
#  	{  $header= $league ? "$ccode $off ${mm}${modetype} ranking by Country"
#  				: "$ccode $off $modetype popularity"; 
#  	};
	my $rb = &trans("Ranking by Country");
 

print <<Header;
<html>
<head>
    <title>$rb</title>
</head>
Header

&standard_header();

print "$active<br>\n";


}
sub update_rankings 
{ 			
	my $nitems=param('n');
	my $retired = param('retired');
	my $months=param('months');
	my $mode = param('mode');
	my $league = param('league');
	my $top = param('top');
	my $leaguesize = ($top eq "") ? 3 : $top;
	my $prefix = (($mode eq 'master') ? "master_" : "");	

	my $dbh = &connect();              # connect to local mysqld

	if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{
	&readtrans_db($dbh);
	
	&rank_header($nitems,$retired,$league);

	&ppm_table($dbh,$months,$nitems,$mode,$retired,$leaguesize,0);

	&standard_footer();

 
   }
	&disconnect($dbh);
}
print header;
&init();
&update_rankings();
	
