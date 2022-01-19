#!/usr/bin/perl
#
# 1/22/2009 added checks based on timestamps and additional &note_failed_login
# logic to thwart spam registration attempts for "sara1234" youtubegal1234@gmail
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;
use Debug;
use CGI::Cookie;

require "include.pl";


require "tlib/common.pl";
require "tlib/timestamp.pl";
require "tlib/gs_db.pl";
require "tlib/show-recent.pl"

use Crypt::Tea;


sub init {
	$| = 1;				# force writes
}



print header;
{	my $n = &param('n');
	if($n<1) { $n = 5; }
	if($n>100) { $n= 100; }
	&show_recent($n);
}

