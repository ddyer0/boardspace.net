#!/usr/bin/perl
#
#
# logon     Validate user's Player Name and password CGI program!
#
# major revision 1/2013 for java web start. The general philosophy is to
# keep exactly the same logic and flow, except that the various outcomes
# get printed in special ways for java web start.
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use CGI::Cookie;
use HTML::Entities;
use URI::Escape;

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
require "tlib/ordinal-rankings.pl";

use Crypt::Tea;

sub init {
	$| = 1;				# force writes
}



sub standard_stuff()
{  my ($dbh,$pname,$language)=@_;
   my $languageName = &select_language($language);
   my $root = $ENV{'DOCUMENT_ROOT'};
   my $top = param("notop") eq '';
   print "<center>";
   &readtrans_db($dbh);

   print "<table CELLSPACING=2 align=top><tr>";
  
   print "<td align=left valign=top>"; 
    &do_messageboard($dbh,$pname,'','','',1); 
   print "<br>";
     &show_recent(5);
   print "</td>";
   print "<td>";
   &show_ordinal_rankings($dbh,10,$'retire_months,2);
   print "</td>";
   print "</tr></table>";
   if($top) { &top_players_table($dbh,0,$language,$pname,@'top_player_variations); }
  
}

{	my $dbh = &connect();
	print header;
	if($dbh)
	{
	if(&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0)
	{
	my %cookies = fetch CGI::Cookie();
	my $pname = $cookies{'nickname'};
	my $language = $cookies{'language'};
	if($pname) { $pname = $pname->value;}
	if($language) { $language = $language->value;}
	&standard_stuff($dbh,$pname,$language);
	}
	else {
	  print "<h1>database access blocked<h1>";
	}
	}
	else
	{
	 print "<h1>mysql not connecting</h1>";
	}
}
