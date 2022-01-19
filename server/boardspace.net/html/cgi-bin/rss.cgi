#!/usr/bin/perl
#
#
# logon     rss feed for boardspace forums
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use CGI::Cookie;
use HTML::Entities;

use Mysql;
use Debug;
use Socket;
use strict;
require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/getLocation.pl";
require "tlib/lock.pl";
require "tlib/top_players.pl";
require "tlib/favorite-games.pl";
require "tlib/show-recent.pl";
require "tlib/messageboard.pl";

use Crypt::Tea;

sub init {
	$| = 1;				# force writes
}



sub do_rss()
{	&init();

	print header;
	
	{
		my $dbh = &connect();
		if($dbh)
			{
			&readtrans_db($dbh); 
			&disconnect($dbh);
			}
	}

	print "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n";
	print "<rss version=\"2.0\">\n";
	&show_recent_rss(5);
	print "</rss>\n";
}

&do_rss();
