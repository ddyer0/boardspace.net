#!/usr/bin/perl
#
# edit.cgi
# view and edit player registration details
#
# 9/2005 added logic to allow uncomfirmed users to make changes, and to resend the
# the confirmation link if they make changes.
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Debug;
use strict;
use URI::Escape;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";


#
# this is a "honeypot" script to notice and ban anyone just clicking on links
#
sub doit()
{	my $ip = $ENV{'REMOTE_ADDR'};
	my $from = &param('from');
	my $dbh = &connect();
	my $look = &param('translate');
	print header;
	if($dbh && (&allow_ip_access($dbh,$ip)>=0))
	{
	&readtrans_db($dbh);
	my $tr = &trans($look);
	print "Translate $look to $tr<p>";
	}

}
&doit();



