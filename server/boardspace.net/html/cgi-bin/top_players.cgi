#!/usr/bin/perl
#
# nitems=nn
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use Debug;
use Time::Local;
use strict;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/top_players.pl";

sub rank_header 
{	my $t0 = &trans('Top Players');
  my $main = &trans('MainHeader.gif');
	print <<Header;
<html>
<head>
    <title>$t0</title>
</head>

<body background=/images/background1.jpg text="#000000" link="#0000EE" vlink="#551A8B" alink="#FF0000">

<center>
<img src="/images/$main" border=0>
<br><h2>$t0</h2>
<p>

Header

}

sub show_activity
{ my $dbh = &connect();
  if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {
  &readtrans_db($dbh);
  if(!param('embed')) { &standard_header(); }
  print "<center>\n";
  &top_players_table($dbh,$'top_players_per_row,'english','',@'top_player_variations);
  print "</center>\n";
  if(!param('embed')) { &standard_footer(); }
  }
  &disconnect($dbh);
}


print header;
param();
&show_activity();

