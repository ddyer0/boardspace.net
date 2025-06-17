#!/usr/bin/perl

use strict;
require "include.pl";
require "tlib/common.pl";
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

sub timestamp()
{	my ($offset) = @_;
	my $val = &timestamp_date($offset);
	return(&encode_entities(&encrypt($val,$::tea_key)),$val);

}

sub check_timestamp()
{	my ($var) = @_;
	my $dv = &decrypt(&decode_entities($var),$::tea_key);
	return( ($dv eq &timestamp_date(0)) || ($dv eq &timestamp_date(1)));
}

sub print_timestamp()
{ my ($ts,$tss) = &timestamp(0);
  print &obfuscateHTML("<input type=hidden name=timestamp value='$ts'>\n");
}

sub print_raw_timestamp()
{	my ($ts,$tss) = &timestamp(0);
	print "$ts\n";
}