#!/usr/bin/perl
use strict;
require "../include.pl";
require "check-time.pl";
require "gs_db.pl";
my $correction = &get_time_correction(1);	#get the correction quietly
my $dbh = &connect();
if($dbh) 
{	&commandQuery($dbh,"REPLACE INTO facts SET name='time-correction',value='$correction'");
	&disconnect($dbh);
}