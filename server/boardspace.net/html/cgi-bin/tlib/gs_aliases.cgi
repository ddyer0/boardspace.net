#!/usr/bin/perl
# Copyright (c) April 2003 
#
# monitor players who may be using aliases, as measured by using the
# same ip address within a short period of time.
#
# the basic data element is an "comcident_ip" table, which records a pair of player
# uids which were seen used together.  These are maintained by the gs_logon.cgi script,
# and also displayed by the edit.cgi script.
#
require 5.001;
use strict;
require "../include.pl";
require "gs_db.pl";
require "common.pl";

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);

#
# display a summary of interesting activity.
#
sub normal_form($dbh)
{	my ($dbh) = @_;
	my $months = &param('months');
  if($months == '') { $months=$::retire_months; };
  my $sunset = int(time() - $months * 60 * 60 * 24 * 31);
	my $q = "select p1.player_name,p2.player_name,UNIX_TIMESTAMP(cip0.last_date),cip0.count,cip0.type,cip1.count,cip2.count"
		. " from coincident_ip as cip0"
		. " left join players as p1 on p1.uid=cip0.uid1 "
		. " left join players as p2 on p2.uid=cip0.uid2 "
		. " left join coincident_ip as cip1 on cip1.uid1=cip0.uid1 and cip1.type='NoMatch' "
    . " left join coincident_ip as cip2 on cip2.uid1=cip0.uid2 and cip2.type='NoMatch' "
    . " where (cip0.last_date > $sunset) and (cip0.type != 'NoMatch') and (cip0.count>1) "
    . " order by cip0.last_date desc";
  my $sth = &query($dbh,$q);
  my $nr = &numRows($sth);
  print "<table><caption><b>Possible Aliases</b></caption>\n";
  while($nr-- > 0)
	{
		my ($p1,$p2,$date,$count,$type,$c1,$c2) = &nextArrayRow($sth);
		$c1 = int(100.0*$count/($count+$c1)+0.5);
		$c2 = int(100.0*$count/($count+$c2)+0.5);
		my $date = &timeago($date);
		print "<tr><td>$p1</td> <td>$c1%</td> <td>$p2</td> <td>$c2%</td><td>$type</td><td>(n=$count)</td>"
		 . "<td><b>$date</b></td></tr>\n";
	}
	print "</table>\n";

}

#
# display info from the coincident_ip table
#
sub doit()
{ my $password = &param('password');

  print header;

  print "<title>$ENV{'HTTP_HOST'} Alias Monitor</title><h2>$ENV{'HTTP_HOST'} Alias Monitor</h2>\n";
  

  if($::sendmessage_password 
  	&& ($password eq $::sendmessage_password))
  {
   my $dbh = &connect();
	 if($dbh) { &normal_form($dbh); }
	 else { print "Connecting to the database failed<br>"; }
  }
  else
  {
  print "<form action=$ENV{'REQUEST_URI'} method=post>\n";
  print "<br>password: <input name=password value=''>\n";
  print "<br><input type=submit>\n";
  print "</form>\n";
  }

}

&doit();