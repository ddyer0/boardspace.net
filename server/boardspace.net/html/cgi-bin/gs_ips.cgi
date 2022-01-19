#!/usr/bin/perl
# Copyright (c) April 2003 
#
# monitor ip addresses connecting, or failing to connect
# ban ip address ranges from connecting
# report instances of banning via the event database
# this file contains the setup and reporting.  The actual logic is in gs_db.pl
#
# the basic data element is an "ipinfo" table, which records a range of ip addresses
# and counts some related events.
#CREATE TABLE ipinfo (
#  uid int(11)          unique int for this entry
#  status enum
#   ('auto'             automaticly created after a login failure                  
#    'normal'           no actions, just here to keep counts of some IP address
#    'noguest'          no guest logins are permitted (unimplemented)
#    'confirm'          registrations must be confirmed by email (unimplemented)
#    'noregister'       registrations are not permitted
#    'autobanned'       was auto, banned after too many failed login attempts
#    'banned'           manually banned
#    'wasbanned')       archival note that was once "autobanned" but ban has expired
#  min                  min IP address of range
#  max                  max IP address of range
#  logincount           number of logins for this address
#  badlogincount        number of failed logins for this address
#  rejectcount          number of logins rejected by autoban or banned status
#  regcount             number of registrations rejected by noregister status
#  changed timestamp(14)last actvity
#  comment text,        has info about failed login attempts for auto/autobanned/wasbanned
#
require 5.001;
use strict;
require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use CGI::Cookie;
use Mysql;
use Debug;
use Crypt::Tea;
use HTML::Entities;

#
# display a summary of interesting activity.
#
sub normal_form()
{ my ($dbh,$delete,$recent,$banned,$forip,$forcomments,$ip) = @_; 
  my $ipint = &ip_to_int($ip);
  my $whereclause = 
		$banned ? "WHERE status='banned' ORDER by changed "
		 : $recent ? " ORDER BY changed DESC LIMIT 30 "
				:  $forcomments
						? sprintf("WHERE comment like %s ORDER BY changed DESC LIMIT 30",$dbh->quote("%$ip%"))
						: $forip ? "WHERE min<='$ipint' and max>='$ipint' ORDER BY changed DESC LIMIT 30" 
							: "WHERE (badlogincount>2) or (status!='auto') ORDER BY changed DESC LIMIT 30";
  my $q = "select uid,status,min,max,logincount,badlogincount,rejectcount,changed+0,comment from ipinfo "
	    . "$whereclause";
  #print "Q: $q<p>";
  my $sth = &query($dbh,$q);
  my $num = &numRows($sth);
  
  print "<input type=submit name='delete selected' value='delete selected'>\n";
  print "<input type=submit name='add new' value='add new'>\n";
  print "<input type=submit name='banned' value='banned'>\n";
  print "<input type=submit name='show recent' value='show recent'>\n";
  print "<input type=submit name='show interesting' value = 'show interesting'>\n";
  print "<input type=submit name='show for IP' value='show for IP'>\n";
  print "<input type=submit name='search comments' value='search comments'>\n";

  print "<input type=text name='ip' value=''>\n";
  print "<input type=hidden name=prev_up value='$forip'>\n";
  print "<input type=hidden name=prev_recent value='$recent'>\n";
  print "<input type=hidden name=prev_banned value='$banned'>\n";
  print "<table border=1>\n";
  &print_row("delete","edit","status","min","max","login","badlogin","reject","changed","comment");
  while($num>0)
  { my ($uid,$status,$min,$max,$login,$badlogin,$reject,$changed,$comment) = &nextArrayRow($sth);
    if($delete && param("select-$uid"))
  	{ #delete this row
  		&commandQuery($dbh,"delete from ipinfo where uid=$uid");
  	}
  	else
  	{	my $ecomment = &encode_entities($comment);
		&print_row("<input type=checkbox name='select-$uid'>",
						  "<input type=submit name=edit value=' $uid '>",
							$status,
							&int_to_ip($min),
							&int_to_ip($max),
							$login,
							$badlogin,
							$reject,
							&timestamp_to_date($changed),
							"<pre>$ecomment</pre>");


  	}
    $num--;
  }
	print "</table>";
	&finishQuery($sth);
}
#
# display a single entry for editing.  Also used to create new entries.
#
sub edit_form()
{	my ($dbh,$edit) = @_;

	my ($uid,$status,$min,$max,$login,$badlogin,$reject,$changed,$comment);

	my @options = &get_enum_choices($dbh,'ipinfo','status');
	
	if($edit)
		{ my $sth = &query($dbh,"select uid,status,min,max,logincount,badlogincount,rejectcount,changed,comment from ipinfo where uid=$edit");
		  ($uid,$status,$min,$max,$login,$badlogin,$reject,$changed,$comment) = &nextArrayRow($sth);
			print "<h1>Edit Ip Range $edit</h1>\n";
		  &finishQuery($sth);
		}
		else
		{	print "<h1>New Ip Range</h1>\n";
		}
		
	print "<input type=hidden name=uid value=$uid>\n";
	print "<table border=1>\n";
	$min = &int_to_ip($min);
	$max = &int_to_ip($max);
	
	&print_row("Column","Value");

	print "<tr><td>status</td><td><select name=status>";
	&print_option_list($status,@options);
	print "</select></td></tr>\n";
	
	&print_row("min ip","<input type=text name=min_ip value='$min'>\n");
	&print_row("max ip","<input type=text name=max_ip value='$max'>\n");
	&print_row("logins","<input type=int name=logins value=$login>\n");
	&print_row("badlogins","<input type=int name=badlogins value=$badlogin>\n");
	&print_row("rejected","<input type=int name=rejected value=$reject>\n");
	&print_row("comment","<input type=text name=comment value='$comment'>");
  
  print "</table>\n";
 	print "<input type=submit name='edited' value='edited'>\n";
	print "<input type=submit name='continue' value='no changes'>\n";


  print "<p><ul><li><b>Normal</b> means just collect statistics\n";
  print "<li><b>noguest</b> means guest logins are not permitted\n";
  print "<li><b>confirm</b> means new registrations must be confirmed by email\n";
  print "<li><b>noregister</b> means new registrations are not permitted\n";
  print "<li><b>autobanned</b> means no logins are permitted due to recent hacking attempt\n";
  print "<li><b>banned</b> means no logins are permitted permanantly\n";
  print "<li><b>wasbanned</b> means an autoban existed but has expired";
  print "</ul>";

}
#
# finalize the edit of an entry
#	
sub finish_edit()
{	my ($dbh) = @_;
	my $uid = param('uid');
	my $minip = param('min_ip');
	my $maxip = param('max_ip');
	my $min = &ip_to_int($minip);
	my $max = &ip_to_int($maxip);

	my $login = param('logins');
	my $badlogins = param('badlogins');
	my $reject = param('rejected');

	if ($login eq '') { $login = "0";};
	if ($badlogins eq '') { $badlogins="0"; }
	if ($reject eq '') { $reject="0"; }

	my $qlogin = $dbh->quote($login);
	my $qbadlogins = $dbh->quote($badlogins);
	my $qreject = $dbh->quote($reject);
	
	my $status = param('status');
	my $qstatus = $dbh->quote($status);
	my $comment = $dbh->quote(param('comment'));
	my $v1 = &int_to_ip($min);
	my $v2 = &int_to_ip($max);
	my $valuestring = "comment=$comment,status=$qstatus,min='$min',max='$max',logincount=$qlogin,rejectcount=$qreject,badlogincount=$qbadlogins";

	if($uid==0)
	{
		&commandQuery($dbh,"insert into ipinfo set $valuestring");
	}
	else
	{ &commandQuery($dbh,"update ipinfo set $valuestring where uid=$uid");
	}
}
#
# display info from the ipinfo table
#
sub doit()
{ my $password = &param('password');
	my $delete = &param('delete selected');
	my $forip = &param($delete ? 'prev_ip' : 'show for IP');
	my $forcomments = &param('search comments');
	my $recent = &param($delete ? 'prev_recent' : 'show recent');
	my $banned = &param($delete ? 'prev_banned' : 'banned');
	my $ip = &param('ip');
	my $add = &param('add new');
	my $edit = &param('edit');
	my $oldselect = &param('selected');
	my $edited = &param('edited');
	my $myaddr = $ENV{'REMOTE_ADDR'};
    my $bannercookie = &bannerCookie();

  print header;
  print "<title>$ENV{'HTTP_HOST'} Ip Monitor</title><h2>$ENV{'HTTP_HOST'} Ip Monitor</h2>\n";
  print "<form method=post action=$ENV{'REQUEST_URI'}>\n";
  my $dbh = &connect();

  if($dbh 
  	&& $'sendmessage_password 
  	&& ($password eq $'sendmessage_password)
  	&& (&allow_ip_access($dbh,$myaddr)>=0)
  	&& (&allow_ip_access($dbh,$bannercookie)>=0))
  { print "<input type=hidden name=password value='$password'>\n";
	if($edited) { &finish_edit($dbh); }
 	if($add) { &edit_form($dbh,0); }
 		elsif ($edit) { &edit_form($dbh,$edit); }
 		else 
 			{
 			 &normal_form($dbh,$delete,$recent,$banned,$forip,$forcomments,$ip); 
 			 }
  }
  else
  {
  if($dbh)
  	{
  	if($password) 
  		{
  		&note_failed_login($dbh,$myaddr,"IP: view '$ENV{'REQUEST_URI'}");
        &note_failed_login($dbh,$bannercookie,"CK: view '$ENV{'REQUEST_URI'}");
 		}
  	}
  print "<br>password: ";
  print "<input type=password name=password value='' SIZE=20 MAXLENGTH=25>\n";
  }
  if($dbh) {  	&disconnect($dbh); }
  print "</form>\n"

 
}

&doit();