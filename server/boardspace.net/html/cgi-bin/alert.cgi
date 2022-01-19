#!/usr/bin/perl 
#
# create a log file entry for a java error
#

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;

require "include.pl";
require "tlib/gs_db.pl";

#
# main program
#

$| = 1;                         # force writes
print header;
#
# this script is obsolete and should be removed
# after version 4.96 is extinct
#
my %in;
my @k=keys(%in);
my $e=param();
my $name=param('name');
my $data=param('data');
my $caller = $ENV{'REMOTE_ADDR'};
my $dbh = &connect();
my $ip = $ENV{'REMOTE_ADDR'};
my $qstr = $dbh->quote("$name @ $ip: $data");
&commandQuery($dbh,"INSERT into messages SET type='applet',message=$qstr");
&disconnect($dbh);
print "Ok\n";