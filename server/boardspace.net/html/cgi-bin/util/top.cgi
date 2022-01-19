#!/usr/bin/perl

require 5.001;
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );
use strict;


sub doit()
{  print "Content-type: text/html\n\n"; 
   print "<html><head><META HTTP-EQUIV=REFRESH CONTENT=30></head><title>Server Status on boardspace.net</title><h1>Process load on $ENV{'HTTP_HOST'}</h1>\n";
   my $tm = `top -b -n1 `;
   print "<pre>$tm</pre>\n";
}

&doit();
