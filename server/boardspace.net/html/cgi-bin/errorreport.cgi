#!/usr/bin/perl 
#
# create a log file entry for a java error
# this simply dumps all the data from a web post into
# a log file
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;

#
# main program
#
$::java_error_log = "g:/temp/logs/miniloader_error.txt";

$| = 1;                         # force writes
sub doit()
{
print header;
my %in;
my @k=keys(%in);
my $e=param();
my $name=param('name');
my $data=param('data');
my $caller = $ENV{'REMOTE_ADDR'};
open(FILE,">>$::java_error_log");
my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
if($year<1900) { $year += 1900; }
printf FILE "[%d/%02d/%02d %02d:%02d:%02d]", $year,$mon+1,$mday,$hour,$min,$sec;
print FILE " log request from $name ($caller)\n";
print FILE "data=$data\n";
print FILE "\n";
close(FILE);

print "Ok\n";

}
&doit();
