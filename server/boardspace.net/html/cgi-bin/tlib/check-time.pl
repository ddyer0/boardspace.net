#!/usr/bin/perl
use LWP::UserAgent;
use HTTP::Request;
# use ppm install timedate to get the following
use Date::Parse qw(str2time);
use strict;
#
# this function makes a good guess at the accuracy of the local
# gmt clock by asking a selection of internet hosts what time it
# is, and taking the median (not average!) value.  Rather than
# a formal, high precision time protocol, we use the time header
# from ordinary http servers.  No special protocols are required.
#
# usage:
# require "check-time.pl";
# my $correction = &get_time_correction(1);	#get the correction quietly
#
#
# these more-or-less randomly selected hosts can be changed at will,
# but the theory is that these should be official time sites or 
# big public sites that are likely to be well maintained and available
#
my @hosts = 
	( "http://nist.time.gov/",
	  "http://tycho.usno.navy.mil/" ,
	  "http://www.worldtimeserver.com/",
	  "http://www.weather.gov.hk/",
	  "http://www.worldtime.com/",
	  "http://www.time.gov/",
	  "http://wwp.greenwichmeantime.com/",
	  "http://www.cnn.com/",
	  "http://www.google.com/",
	  "http://www.yahoo.com/",
	);

sub get_time_correction()
{	my ($quiet) = @_;
	my %results;
	foreach my $host (@hosts)
	{
	my $ua = LWP::UserAgent->new;
	my $request = HTTP::Request->new(HEAD => $host);
	$request->push_header(Pragma => "no-cache");
	$request->push_header("Cache-Control" => "no-cache");
	$ua->env_proxy();
	my $response = $ua->request($request);
	if ($response->is_success) 
	{
 		my $r=$response->header("Date");
		my $rt = str2time($r);
		if (undef != $rt)
			{ $rt -= time(); 
			   $results{$host} = $rt;
  			   if(!$quiet) { print "host: $host time: $r diff: $rt\n"; }
			}
			else 
			{	if(!$quiet) { print "$host bad time header (=$r)\n"; }
			}
	}
	else { if(!$quiet) { print STDERR "$host failed"; } }
	}
	my @val = values(%results);
	# insist that we got at least 5 good results so the median is not some random
	# success in a sea of failures.
	if($#val>=5) 
		{ my @res = sort {$::a <=> $::b} @val; 
	        my $correction = $res[int($#res/2)];
		  if(!$quiet) { print "Median correction (@res) is $correction\n";  }
		  return($correction);
		}
		else
		{
		return(0);
		}
}

1

