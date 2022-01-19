#!/usr/bin/perl

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;

require "config.pl";
require "../include.pl";
require "../$lib'lib/common.pl";

$| = 1;				# force writes

sub doit()
{
    my $log = $'perl_log;
    open(FILE,">>$log");
print <<HEADER;
Content-type: text/html

<html>
<head>
<title>Reveived Form</title></head>
<body bgcolor="#ffffff">
<h3>Received Form</h3>
HEADER

print "Logged to $'perl_log<p>";
print FILE "\n---\nReceived Form:\n";

{ my @pars = param();
	my $par;
	print "All parameter names: @pars<p>\n";
	print FILE "All parameter names: @pars\n";
	print "Details:<br><table>\n";
	print FILE "Details:\n";
	foreach $par (@pars)
	{	my $val = param($par);
		if($par eq 'password') { $val = '<xxxx>'; }
		print "<tr><td>$par</td><td>$val</td></tr>\n";
		print FILE "$par -> $val\n"
	}
	print "</table>";
}
close(FILE);
}

&doit();
1
