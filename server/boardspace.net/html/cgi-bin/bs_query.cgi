#!/usr/bin/perl
#
# the new "encrypted all the way" interface for queries from the app.
# incoming parameters are combined into a single params parameter, which is 
# base64, encrypted, and checksummed.  Replies are similarly packaged.
# theoretically, this makes them impossible to intercept or modify, 
# barring extreme reverse engineering.
#
use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use IO::File;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/params.pl";
require "tlib/timestamp.pl";

#
# the actual information providers set error and result strings.
#
var $'error = '';
var $'result = '';

#
# print the directories associated with the supported games. 
# this is the link used to browse directories using html
#
sub print_gamedir()
{
	my ($dbh,$game) = @_;
	&print_contact_log("offline");
	my $qgame = $dbh->quote($game);
	my $q = "select directory_index,name,directory from variation where included>0 order by directory_index";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	while($num-- > 0)
	{
		my ($dirnum,$gamename,$dir) = &nextArrayRow($sth);
		if(($game eq '') || ($game eq $gamename))
		{
			$'result = $'result . "$dirnum,$gamename,$dir\n";
		}
	}
	&finishQuery($sth);
}
sub print_country_list()
{	
	foreach my $val (@'countries) 
	{	$'result = $'result . "$val\n";
	}
}
sub print_timestamp_code()
{	my ($ts,$tss) = &timestamp(0);
	$'result = $'result . "$ts\n";
}

sub print_prereg_info()
{	&print_timestamp_code();
	&print_country_list();
}

sub make_contact_log()
{
 my ($msg ) = @_;
 my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
 open( F_OUT, ">>$'contact_log" );
 printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] %s\n",1900+$year,$mon+1,$mday,$hour,$min,$sec,$msg;
 close F_OUT;
}

sub print_contact_log()
{	my ($msg) = @_;
	my $platform = &param("platform");
	if(!($platform eq ''))
	{
	&make_contact_log("$msg platform $platform ip $ENV{'REMOTE_ADDR'}");
	}

}

#
# before login, supply a login message and the list of acceptable
# versions.
#
sub print_mobileinfo()
{	my ($dbh) = @_;
	my $val = "languages ";
	# no particular order is required, but start with languages
	my @ll;
	&print_contact_log("online");
	foreach my $key (keys(%'language_codes)) 
	{	my $val = $'language_codes{$key};
		if(!("" eq $val)) { push(@ll,$val); }
	}
	my $prev;
	my $llist = "";
	foreach my $l (sort @ll)
	{
	if(!($prev eq $l)) {	$llist .= "$l "; }
	$prev = $l;
	}
	$val .= &encode64long($llist) . "\n";	
	#
	# no extra crs in the base64!
	#
	$val .= "message " . &encode64long($'mobile_login_message) . "\n";
	$val .= "versions " . &encode64long($'mobile_version_info) . "\n";
	my $myuid = &param('myuid');
	if($myuid)
		{
		# if the caller provided his uid, provide the number of waiting games
		my $qmyuid = $dbh->quote($myuid);
		my $q = "select count(whoseturn) from offlinegame where whoseturn = $qmyuid and status='active'";
		my $sth = &query($dbh,$q);
		#print "Q: $q\n";
		my $numRows = &numRows($sth);
		if($numRows eq 1)
		{
		my ($n) = &nextArrayRow($sth);
		$val .= "turnbasedmoves " . &encode64long($n) ."\n";
		}
		&finishQuery($sth);
		}
	if($'checksum_version>0)
	{
	  $val .= "checksumversion " .&encode64long("$'checksum_version") . "\n";
	}
	$'result = $val;
}
#
#add an alert to the database.  This is used
#for severe and/or very unusual occurrences
#
sub postalert()
{	my ($dbh,$name,$data) = @_;
	my $ip = $ENV{'REMOTE_ADDR'};
	my $qstr = $dbh->quote("$name @ $ip: $data");
	&commandQuery($dbh,"INSERT into messages SET type='applet',message=$qstr");
	$'result = $qstr;
}

sub posterror()
{
	my ($dbh,$name,$data) = @_;
	my $caller = $ENV{'REMOTE_ADDR'};
	open(FILE,">>$'java_error_log");
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
	if($year<1900) { $year += 1900; }
	printf FILE "[%d/%02d/%02d %02d:%02d:%02d]", $year,$mon+1,$mday,$hour,$min,$sec;
	print FILE " log request from $name ($caller)\n";
	print FILE "data=$data\n";
	print FILE "\n";
	close(FILE);
	
	&countDBevent($dbh,$'java_error_log,$'java_error_alert_level,$'java_error_panic_level);
	
	$'result = "error logged";
}

sub print_messageboard
{ my ($dbh) = @_;
  my $q = "SELECT player_name,messageid,content,notes.uid from notes left join players on players.uid=notes.uid WHERE expires>current_date() ORDER BY expires";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  $'result = '';
  if($n>0)
    { $'result = "\n";
      while($n-->0)
      {my ($from,$messid,$content,$uid) = &nextArrayRow($sth);
       $'result = $'result . "${from}: $content\n\n";
	  }
    }
  &finishQuery($sth);
 
}

sub process_form()
{
	my ($dbh) = @_;
	my $tagname = param('tagname');
	if($tagname eq "gamedir")  { &print_gamedir($dbh,param('game')); }
	elsif($tagname eq 'messageboard') { &print_messageboard($dbh); }
	elsif($tagname eq 'prereginfo') { &print_prereg_info(); }
	elsif($tagname eq 'mobileinfo') { &print_mobileinfo($dbh); }
	elsif($tagname eq 'postalert') { &postalert($dbh,param('name'),param('data')); }
	elsif($tagname eq 'posterror') { &posterror($dbh,param('name'),param('data')); }
	else { $'error = "undefine request $tagname"; }	
	if(($'error eq '') && ($'result eq ''))
	{ 
		$'error = "tag $tagname produced nothing"; 
	}
}

#
# input is a single parameter "params" which is base64, encrypted, checksummed
#
sub doit()
{	__dStart( "$'debug_log",$ENV{'SCRIPT_NAME'});;

  	print header;

	if( param() ) 
	{
	my $ok = &useCombinedParams($'tea_key,1);
	if($ok)
	{
	my $dbh = &connect();
	if($dbh)
	{
		my $myaddr = $ENV{'REMOTE_ADDR'};	
		if( &allow_ip_login($dbh,$myaddr))
		{
		&process_form($dbh);	
		}
		else {
			$'error = "database connect refused";
		}
	 &disconnect($dbh);
	}
	else 
	{
	$'error = "database connect failed";
 	}}
 	
 	else { $'error = "invalid main input parameters"; }
 	}
 	else { $'error = "no input parameters"; }
 	 	
 	if($'error)
 	{	&printResult("Error",$'error);
 	}
 	else
 	{
 		&printResult("Ok",$'result);
 	}

  __dEnd( "end!" );
}


$| = 1;                         # force writes

&doit();
