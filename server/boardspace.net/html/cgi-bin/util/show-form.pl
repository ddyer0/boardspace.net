#!/usr/bin/perl

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;

$| = 1;				# force writes

print <<HEADER;
Content-type: text/html

<html>
<head>
<title>Reveived Form</title></head>
<body bgcolor="#ffffff">
<h3>Received Form</h3>
HEADER

{ my @pars = param();
	my $par;
	print "All parameter names: @pars<p>\n";
	print "Details:<br><table>\n";
	foreach $par (@pars)
	{	my $val = param($par);
		if($par eq 'password') { $val = '<xxxx>'; }
		print "<tr><td>$par</td><td>$val</td></tr>\n";
	}
	print "</table>";
}
1
