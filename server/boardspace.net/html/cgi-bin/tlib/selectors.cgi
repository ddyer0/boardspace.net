#!/usr/bin/perl
#
use strict;
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );

require "../include.pl";
require "common.pl";
require "gs_db.pl";
print "Content-type: text/html\n\n";

my $what = param('what');
my $default = param('default');

if($what eq 'language')
{	my $dbh = &connect();
	if($dbh)
	{&readtrans_db($dbh);
	 &select_language_menu($default,0);
	 &disconnect($dbh);
	}
}
elsif ($what eq 'country')
{	my $dbh = &connect();
	if($dbh)
	{ &readtrans_db($dbh);
	  &print_country_selector($default);
	  &disconnect($dbh);
	}
}
