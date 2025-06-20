#!/usr/bin/perl
#
#
# logon     Validate user's Player Name and password CGI program!
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


use Crypt::Tea;

sub init {
	$| = 1;				# force writes
}

#
# check to see if the server is accepting connections
# and the database is accepting connections and can do
# a minimal query.
# 
# this is designed to be called from /uptime-probe.shtml
# and is monitored from uptime_robot
sub check_server()
{ my ($username,$usernum)=@_;
  unless (socket(SOCK, PF_INET, SOCK_STREAM, $::proto))
  {
    return 0;
  }
  return(1);
}

sub check_database()
{	my $dbh = &connect();
	if($dbh)
	{
	my $query = "select uid from players where player_name='guest'";
	my $sth = &query($dbh,$query);
	my $num = &numRows($dbh);
	if($num==1)
	{ my ($uid) = &nextArrayRow($sth);
	}
	&finishQuery($sth);
	&disconnect($dbh);
	return(1);
	}
	return(0);
}
sub check_server_and_db()
{	my ($username,$usernum)=@_;
   	my $server_status = &check_server($username,$usernum);
	my $db_status = &check_database();
   	if(!$server_status)
   	{	print "server down\n";
	}
	elsif (!$db_status)
	{	print "database down\n";
	}
	else
	{ print "xyzzy it's magic!\n";
	}
}

print header;
&check_server_and_db();
