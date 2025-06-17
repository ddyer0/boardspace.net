#!/usr/bin/perl
#
# send a message to a tournament player, to be received when he logs in
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/timestamp.pl";

sub init {
	$| = 1;				# force writes
	__dStart( "$::debug_log", $ENV{'SCRIPT_NAME'} );
}


sub print_header 
{	&standard_header();

	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>Send a Message to gamemaster at boardspace.net</TITLE>

Head_end
}

sub send_email()
{  my ($toemail,$toname,$from,$subj,$body) = @_;
 
   $body =~ s/\r\n/\n/g;	# trim crlf to just lf
   my $msg = "$body\n\n";
  
   &send_mail_to($toname,$from,$toemail,$subj,$msg);

}

# --------------------------------------------
&init();

print header;

__d( "sendmessage...");


sub do_gs_edit()
{
param();
  my $dbh = &connect();
  if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {
  &readtrans_db($dbh);
  my $sendit = param('sendit');
  my $info = param('info');

  &print_header();

  if($sendit)
  {
  my $fromname = param('fromname');
  my $subject = param('subject');
  my $message = param('body');
  my $email = param('email');
  my $stamp = param('timestamp');
  &bless_parameter_length($stamp,40);	#assess for penalty

  if(&check_timestamp($stamp))
  {
  $message = "from: $fromname\nfrom email: $email\ninfo: $info\n\n$message";

  #($toemail,$toname,$to,$from,$date,$subj,$body)
  &send_email($::supervisor_email,"feedback form",$::supervisor_email,$subject,$message);
  print &trans("Message sent"); 
  print "<br>";
  }
  else 
  { __d("failed bad timestamp");
    my $myaddr = $ENV{'REMOTE_ADDR'};
    &note_failed_login($dbh,$myaddr,"IP: bad feedback timestamp");
    print &trans("Message not sent"); print "<br>";; }
  }
  else
  {
  my $unbounce=0;

  my $subject = &param('subject');

  print "<form action=$ENV{'REQUEST_URI'} method=POST>\n";
  print "<center>";
  print "<table><tr><td>";
  print "<b>From:</b></td><td>" ;
  print "<input type=text name=fromname size=30>";
  print "</td></tr><tr>";
  print "<td><b>Email:</b></td>";
  print "<td><input type=text name=email size=30></td>";
  print "</tr>\n";
  
  $subject = &encode_entities($subject);
	
  print "<tr><td><b>Subject:</b></td><td>";
  print "<input type=text name=subject value='$subject' size=80>";
  print "</td></tr>\n";
	
  print "<tr><td colspan=2><p><pre>";
  print "<textarea name=body  cols=90 rows=20></textarea>";

  print "</pre></td></tr>\n";
  print "<tr><td><input type=submit value='Send Feedback'></td></tr>\n";
  print "<input type=hidden name=sendit value=true>";
  print "<input type=hidden name=info value='$subject'>";
  &print_timestamp();
  print "</table>";
  print "</center>";
  print "</form>";
  }
  &standard_footer();
  }
  &disconnect($dbh);
}

do_gs_edit();
