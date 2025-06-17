#!/usr/bin/perl
  
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/show_activity.pl";

print header;
my $dbh = &connect();
&show_activity_table($dbh,&param('uid'),0);

