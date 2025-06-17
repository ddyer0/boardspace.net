#!/usr/bin/perl

require 5.001;
use strict;
require "../include.pl";
require "gs_db.pl";
require "common.pl";

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use Debug;

sub doit()
{ my $file;
  my $dbh = &connect();
  my $q = "SELECT country,count(country) as cc from players group by country order by cc desc";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
	print header;
	print "<h1>flag test</h1>\n";
	print "<table>\n";
	while($n-- >=0 )
	{	my ($file,$cc) = $sth->fetchrow();
	  my $flagim = &flag_image($file);
    my $miss = "";
    if(!-e "$ENV{'DOCUMENT_ROOT'}$flagim")
	 { $miss = "missing and used";
	 }
		print "<tr><td>$cc</td><td>$file</td><td><img width=50 height=33 alt=\"$file\" src=\"$flagim\"></td><td>$miss</td>\n";
	}
	print "</table><table>\n";
	foreach $file (@'countries)
	{	my $miss = "";
    my $flagim = &flag_image($file);
		if(!-e "$ENV{'DOCUMENT_ROOT'}$flagim")
			 { $miss = "missing";
			 }
		print "<tr><td>$file</td><td><img width=50 height=33 alt=\"$file\" src=\"$flagim\"></td><td>$miss</td></tr>\n";
	}
	print "</table>\n";
}
&doit();