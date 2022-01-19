#!/usr/bin/perl 
#
# generate regular, master and doubles rankings on demand.
#
#
# optional parameters:
#
# months=6                        number of months inactive before "retired"
# retired=0                       =1 for show retired players instead of active
# nitems=100                      =n to show top n players
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;

use strict;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/ordinal-rankings.pl";

#$database='test';

sub init 
{
   $| = 1;                         # force writes
}

sub doit()
{	my $num = &param('nitems');
	my $months = &param('months');
	&init();
	print header;
	my $dbh = &connect();
	if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{
	&standard_header();
	if($num==0) { $num=40; }
	if($months==0) { $months = $'retire_months; }
	&readtrans_db($dbh);

	&show_ordinal_rankings($dbh,$num,$months,-1);
	&standard_footer();
	}
}

&doit();
	
