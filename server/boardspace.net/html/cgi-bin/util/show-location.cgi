#!/usr/bin/perl
#
require 5.001;
use CGI qw(:standard);

require "getLocation.pl";


sub init {
	$| = 1;				# force writes
}

init();
param();			#parse parameters, if any 
print header;
{
  my $ip = param('ip');
  my $detailed = (param('detailed') eq "y");
  if($ip == "") { $ip = $ENV{'REMOTE_ADDR'} ; }
  my %location = &getLocation($ip,$detailed);
  my $myplace;
  if(%location && ($location{'certainty'}>20))
    { my $City = $location{'city'};
      my $Region = $location{'region'};
      my $Country = $location{'country'};
      $myplace = "$Region, $Country";
      if(!($City eq $Region))  { $myplace = "$City, $myplace"; }
      $myplace = "The IP locator thinks you are located in:<br>$myplace";
    }
    else 
    { $myplace = "The IP locator can't tell where you (ip=$ip) are, please tell it!"; 
    }
   print "$myplace";
   if($detailed)
    { my $key ; 
      print "<br>";
      foreach $key(keys(%location))
	{ my $val = $location{$key};
	  print "$key = $val<br>\n";
	}
    }
}

