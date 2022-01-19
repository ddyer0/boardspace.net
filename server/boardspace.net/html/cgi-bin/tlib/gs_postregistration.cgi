#!/usr/bin/perl 
#
#
# optional parameters:
# nitems=100                      =n to show top n players
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
require "../include.pl";
require "common.pl";
require "gs_db.pl";
use strict;


  sub doit()
  {
	my $dbh = &connect();
	my $to = param('to');
	my $test = param('test');
	if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{	my $qtest = $dbh->quote($to);
		my $cond =  $to ? "where player_name=$qtest" 
				: " where note_sent is null and games_played=0 and last_played=0 and status='ok' and e_mail_bounce=0"
				. " and (no_email='' OR no_email is NULL)"
				. " and date_joined<unix_timestamp(date_sub(now(),interval 7 day))"
				. " and date_joined>unix_timestamp(date_sub(now(),interval 30 day))";
				
		my $q = "SELECT player_name,e_mail,language,from_unixtime(date_joined) "
			. " FROM players $cond"
			. " ORDER BY language,date_joined DESC" ;
		print "Q: $q<p>";
		my $sth = &query($dbh,$q);
		my $nr = &numRows($sth);
		while($nr-- > 0)
		{
		my ($player,$e_mail,$language,$date) = &nextArrayRow($sth);
		&readtrans_db($dbh,$language);
		my $msg = edit_user_info_message($player);
		my $qplay = $dbh->quote($player);
		my $su = &trans("Playing at boardspace.net");
		my $mm = &trans('recent_registration_message') . "\n" . $msg;
		if($test)
		{
		print "from: $'from_email to: $e_mail su: $su<br>$mm<br>";
		}
		else
		{
		my $q = "update players set note_sent='yes' where player_name = $qplay";
		print "to: $player<br>";
		&commandQuery($dbh,$q);
		&send_mail($'supervisor_email,$e_mail,$su,$mm);
		}
		}
	}
	my $q = "select count(note_sent),sum(if(games_played>0,1,0)) from players where note_sent='Yes'";
	my $sth = &query($dbh,$q);
	my ($tot,$rec) = &nextArrayRow($sth);
	my $pc = int(($rec*100)/($tot+1));
	print "<br>Recovered $rec $pc%<br>";

  }
  print header;
  if(param())
  {
	doit()
  }
  