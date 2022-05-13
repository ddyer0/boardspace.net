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
require "tlib/show_activity.pl";

sub rank_header 
{	my $t0 = &trans('Site Usage Statistics');
  my $t1 = &trans('The following table shows the Site activity since the beginning');
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
$t1
<p>
Header

}
sub show_activity
{
  my $dbh = &connect();
  if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {
  my $uid = &param('uid');
  my $ndays = &param('months');
  &show_activity_table($dbh,$uid,$ndays,&timezoneCookie());
  }
  &disconnect($dbh);
}


print header;
param();
&standard_header();
&show_activity();

&standard_footer();
