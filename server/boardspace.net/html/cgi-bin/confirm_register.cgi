#!/usr/bin/perl
#
# confirm registration after the user receives an email from boardspace.date
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use strict;
use Debug;
use CGI::Cookie;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
use Crypt::Tea;

sub init {
	$| = 1;				# force writes
}


#
# here to register a new player in the database, and then log him in
#
sub confirm()
{
    my ($dbh,$pname, $uid) = @_;
	{
	&readtrans_db($dbh);
    my %cookies = fetch CGI::Cookie();
    my $bannercookie = $cookies{'client'};
 	my $slashpname = $dbh->quote($pname);

   if($bannercookie)
      {  $bannercookie = &decrypt($bannercookie->value,$'tea_key);
      } 

  if(&allow_ip_register($dbh,$ENV{'REMOTE_ADDR'})
     && &allow_ip_register($dbh,$bannercookie))
	{
	my $status = 'ok';
	my $badboy = &banned_from_registering($dbh,$uid);
	if($badboy)
		{
	    $status = 'banned';
		my $qbad = $dbh->quote($badboy);
		&commandQuery($dbh,"INSERT into messages SET type='alert',message=\"$pname registration banned by association with $badboy\"");
		}
	{
	my $quid = $dbh->quote($uid);
	my $qstat = $dbh->quote($status);
	my $sth = &query($dbh,"UPDATE players set status=$qstat WHERE player_name=$slashpname AND uid=$quid AND (status='unconfirmed' or status='ok')");
	my $n = &numRows($sth);
	&finishQuery($sth);
	if($n==1)
	{
	    print "<h2><center>"
		. &trans("Your Registration is confirmed")
		. "</center></h2>\n";
	    print "<br>"
			. &trans("What are you waiting for? #1 Go log in! #2",
						"<a href='/english/login.shtml'>","<a>\n");
		my $qq = "select player_name,e_mail,date_joined from players where uid=$quid";
	}
	else
	{
	    print &trans("Sorry, something went wrong.") . "<p>";
	}
	&disconnect($dbh);
	}

	}}
}

#
# the main program starts here
# --------------------------------------------
#

&init();
print header;
if( param() ) 
{   my $dbh = &connect("confirm");
	if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{
	&readtrans_db($dbh);
	&standard_header();
	print "<title>"
		.&trans("Registration Confirmation Page")
		. "</title>\n";
	my $pname = param('pname');
	my $uid = param('uid');
	&bless_parameter_length($pname,20);
	&bless_parameter_length($uid,20);
	$pname = &despace(substr($pname,0,25));
	

	if($pname && $uid)
		{
	    &confirm($dbh,$pname, $uid);
        }
	&standard_footer();
	}
	else
	{  print "<h2>Sorry, registration is temporarily unavailable</h2>\n";
	}
}
