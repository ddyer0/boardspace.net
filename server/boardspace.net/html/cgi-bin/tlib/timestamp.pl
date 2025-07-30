#!/usr/bin/perl

use strict;
require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
use Crypt::Tea;
use HTML::Entities;
#
# generate a timestamp for the current hour (or n hours ago)
# which is really just the encrypted time and date
#
sub timestamp_date()
{
	my ($offset) = @_;
	my $when = time()-$offset*60*24;
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime($when);
	my $month = ( "January", "February", "March", "April", "May",
				"June", "July", "August", "September", "October", "November", "December")
				 [ $mon ];
	if($year<1000) { $year += 1900; }

	return("$hour:$mday-$month-$year");
}

sub timestamp_date_seed()
{
  my $ts = &timestamp_date();
  my $seed = int(rand(2**32));
  return "$ts+$seed";
}

sub timestamp()
{	my ($offset) = @_;
	my $val = &timestamp_date_seed($offset);
	return(&encode_entities(&encrypt($val,$::tea_key)),$val);

}
#
# limit 1 will add a check that timestamps are used only once
# limit 2 will add a rate limit so the form can't be used too often
#
sub check_timestamp()
{	my ($var,$limit) = @_;
	my $dv0 = &decrypt(&decode_entities($var),$::tea_key);
        #print "check ($limit) $dv0";
	my ($dv) = split(/\+/,$dv0);
	my $maybe = ($dv eq &timestamp_date(0)) || ($dv eq &timestamp_date(1));
	if($maybe && $limit>0)
	{
	my $dbh = &connect();
	my $myaddr = $ENV{'REMOTE_ADDR'};
	if($dbh)
	{ if(&allow_ip_access($dbh,$myaddr))
		{
		# limit 2 also introduces a "fail" which will rate limit the use of the form
		if($limit>1) { &note_failed_login($dbh,$myaddr,"timestamped"); }
		my $qt = $dbh->quote($dv0);
		my $q = "insert ignore into timestamps set data=$qt";
		my $sth = &query($dbh,$q);
		my $uid = &last_insert_id($sth);
		if(!$uid) 
			{ #print "failed, duplicate<p>";
			 $maybe = 0;
			} 
		&finishQuery($sth);
		&commandQuery($dbh,"delete from timestamps where date < now()-interval 1 day ");
		}
		else { $maybe = 0; }
		}
	  
	  &disconnect($dbh);
	}
	return $maybe;
}

sub print_timestamp()
{ my ($ts,$tss) = &timestamp(0);
  print &obfuscateHTML("<input type=hidden name=timestamp value='$ts'>\n");
}

sub print_raw_timestamp()
{	my ($ts,$tss) = &timestamp(0);
	print "$ts\n";
}