#!/usr/bin/perl
#
# send a message to a player, to be received when he logs in
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/messageboard.pl";

use Crypt::Tea;

sub init {
	$| = 1;				# force writes
	__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
}

# --------------------------------------------
&init();

print header;

__d( "messageboard...");

{
	my $fromname = param('fromname');
	
	&bless_parameter_length($fromname,20);

	$fromname = &despace(substr($fromname,0,25));
	my $passwd = param('passwd');

	&bless_parameter_length($passwd,30);

	$passwd = &despace(substr($passwd,0,25));

	my $subject = param('subject');
	my $expires = param('expires');
	
	&bless_parameter_length($expires,3);

	my $link = param('link');
	my $dbh = &connect();
	if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{ 
	my $sub = decode_entities($subject);
	do_messageboard($dbh,$fromname,$passwd,$sub,$expires,$link);
	}
}
