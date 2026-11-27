#!/usr/bin/perl
#
# accept a connection report
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
use Crypt::Tea;

sub init {
	$| = 1;				# force writes
	__dStart( "$'debug_log", $ENV{'REQUEST_URI'} );
}


sub print_header 
{	&standard_header();

	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>Public Notices and Messages</TITLE>
<center><H1>Public Notices and Messages</h1>
<br>use this space to post information about when you will be available to play</center>
<p>
Head_end
}

#logon, return uid and supervisor status
sub logon 
{
  my ($dbh,$pname,$passwd) = @_;
  my $myaddr = $ENV{'REMOTE_ADDR'};
  my $bannercookie = &bannerCookie();
  if(&allow_ip_login($dbh,$myaddr)
     && &allow_ip_login($dbh,$bannercookie))
	{
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $sth = &query($dbh,"SELECT uid,is_supervisor FROM players WHERE player_name=$slashpname AND password=$slashpwd AND status='ok'");
	my $n = &numRows($sth);
  my $val=0;
  my $is_supervisor='';
	if($n == 1)
    { ($val,$is_supervisor) = &nextArrayRow($sth);
    }
	if(!$val)
		{
		&note_failed_login($dbh,$myaddr,"IP: report as $pname");
        &note_failed_login($dbh,$bannercookie,"CK: report as $pname");
		}
  &finishQuery($sth);
	return ($val,(lc($is_supervisor) eq 'yes'));
	}
	else
	{ return(0,"");
	}
}

sub show_messages
{ my ($dbh,$recycle) = @_;
  my $q = "SELECT player_name,messageid,content,notes.uid from notes left join players on players.uid=notes.uid WHERE expires>current_date() ORDER BY expires";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  if($n>0)
    { print "<table width=100%>";
	  my $deloption = $recycle ? "<td><b>delete&nbsp;</b></td>" : "";
      print "<tr>$deloption<td><b>From</b></td><td><b>Message</b></td></tr>\n";
      while($n-->0)
      {my ($from,$messid,$content,$uid) = &nextArrayRow($sth);
	   if($recycle && &param("delete$messid"))
	   {
	    &commandQuery($dbh,"delete from notes where messageid='$messid' and uid='$uid'");
	   }
	   else
	   {
	   my $deloption = ($recycle && (lc($uid) eq lc($recycle)))
		 ? "<td width=5%><input type=checkbox name='delete$messid' ></td>" 
		 : $recycle? "<td></td>" : "";
       print "<tr>$deloption<td width=10%><b>${from}</b>:&nbsp;</td><td>$content</td></tr>\n";
	   }
      }
     print "</table>";
  }else
  {  print "<p>No Messages<p>";
  }
  print "<br><br>\n"
}

sub show_input_form
{ my ($fromname,$passwd,$expires,$subject) = @_;
  $fromname = encode_entities($fromname);
  $subject = encode_entities($subject);
  print "<p>Add a Message<br>\n";
  print "<table><tr><td>Player: ";
  print "<input type=text name=fromname value='$fromname' size=12>\n";
  print "</td><td>Your Password: ";
  print "<input type=password name=passwd value='$passwd'>\n";
  print "</td><td>Expires in ";
  print "<select name=expires>\n";
  print "<option value='1'>1 day</option>\n";
  print "<option value='2' selected>2 days</option>\n";
  print "<option value='3'>3 days</option>\n";
  print "<option value='4'>4 days</option>\n";
  print "<option value='5'>5 days</option>\n";
  print "<option value='6'>6 days</option>\n";
  print "<option value='7'>7 days</option>\n";
  print "</td></tr>";
  print "<tr><td colspan=3>message: <input name=subject type=text value='$subject' size=100>\n";
  print "</td></tr></table>\n";
  print "<input type=submit name=doit value='add a message'>  to delete a message, replace it with a blank message\n";
}

sub processInputForm
{  my ($dbh,$uid,$super,$expires,$subject) = @_;
   if($expires>21) { $expires=21; }
   if($expires<1) { $expires=1; }
   my $sub = $dbh->quote($subject);
   if($uid>0)
   { if(!$super)
       { &commandQuery($dbh,"DELETE from notes where uid=$uid"); 
	   }
     if(length($sub)>5)
      {my $q = "INSERT INTO notes SET uid=$uid,expires=DATE_ADD(CURRENT_DATE,INTERVAL $expires DAY),content=$sub";
      &commandQuery($dbh,$q);
      }
     return(1);
   }
}


# --------------------------------------------
&init();

print header;

__d( "messageboard...");

sub do_connection()
{
	my $fromname = param('fromname');
	my $passwd = param('passwd');
	my $subject = param('subject');
	my $expires = param('expires');
  my $dbh = &connect();
  
  &print_header(); 
  if($dbh)
    {
	 &readtrans_db($dbh);
	 my ($uid,$super) = &logon($dbh,$fromname,$passwd);
	  if($uid>0) 
      { if(&processInputForm($dbh,$uid,$super,$expires,$subject))
        {  $expires=0;

           $subject='';
        }
      }
	  elsif($fromname)
	 {  print "<b>Sorry, name ($fromname,$passwd) and password not found in our player list.</b><p>";
        return(0);
     } 
	  print "<form action=$ENV{'REQUEST_URI'} method=post>\n";
      &show_messages($dbh,$super?$uid:0);
      &show_input_form($fromname,$passwd,$expires,$subject);
	  print "</form>\n";
    }
  &standard_footer();
}

do_connection();