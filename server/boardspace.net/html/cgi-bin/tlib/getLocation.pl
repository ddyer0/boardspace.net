#This Perl script gets a geo block from the database
#see http://glrs.networldmap.com/
#
require 5.001;
use LWP::UserAgent;
use strict;
use JSON;
use utf8;

sub hitlocation()
{	my ($hit) = @_;
	return(get("http://button.geobutton.com/~$hit/1/1/showiframe?"));
}

sub getLocation()
{	my ($ip,$detailed) = @_;
	my %res = &getLocation_ipapi($ip,$detailed);
	if(!%res) { &getLocation_freegeoip($ip,$detailed); }
	return %res;
}

sub getLocation_ipapi()
{
	my ($IpAddress,$detailed) = @_;
	my $ua = new LWP::UserAgent( timeout => 10); # 10 seconds
	# see ip-api.com
	my $GlrsUrl="http://ip-api.com/json/$IpAddress";
	if($detailed) { print "url= $GlrsUrl<br>\n"; }
	my $request = HTTP::Request->new(GET => $GlrsUrl);
   	my $response = $ua->request($request);
    	if(!$response->is_success) { return(0); }
	my $Buffer = $response->content; 
	if($detailed) { print "$Buffer<br>\n"; }

 	my $fooref = decode_json($Buffer) ;
	my %foo = %$fooref;

	my $City = $foo{'city'};
	my $Region = $foo{'regionName'};
	if($Region eq $City) { $Region = ""; };
	if($City eq '') 
		{ $City = $foo{'time_zone'}; 
		  my ($reg,$cit) = split(/\//,$City);
                  if($reg && $cit) { $Region = $reg; $City = $cit; }
		}
	my %result;
	# the names of these keys are different from freegeoip
	my $lat = $foo{'lat'};
	my $lon = $foo{'lon'};
	$result{'city'} = $City;
	$result{'region'} = $Region;
	$result{'country'} = $foo{'country'};
	$result{'latitude'} = $lat;
	$result{'longitude'} = $lon;
	$result{'certainty'} = ($lat && $lon) ? 100 : 0;

	return(%result);
}

# this service is deprecated because they've imposed really low usage limits
sub getLocation_freegeoip()
{
	my ($IpAddress,$detailed) = @_;
	my $ua = new LWP::UserAgent( timeout => 10); # 10 seconds
	# was 	"http://freegeoip.net/json/$IpAddress";
	# see api.ipstack.com
	my $GlrsUrl= "http://api.ipstack.com/$IpAddress?access_key=e150299ed229ba69034aea6f6cc8ceef";
	if($detailed) { print "url= $GlrsUrl<br>\n"; }
	my $request = HTTP::Request->new(GET => $GlrsUrl);
   	my $response = $ua->request($request);
    	if(!$response->is_success) { return(0); }
	my $Buffer = $response->content; 
	if($detailed) { print "$Buffer<br>\n"; }

 	my $fooref = decode_json($Buffer) ;
	my %foo = %$fooref;

	my $City = $foo{'city'};
	my $Region = $foo{'region_name'};
	if($Region eq $City) { $Region = ""; };
	if($City eq '') 
		{ $City = $foo{'time_zone'}; 
		  my ($reg,$cit) = split(/\//,$City);
                  if($reg && $cit) { $Region = $reg; $City = $cit; }
		}
	my %result;
	my $lat = $foo{'latitude'};
	my $lon = $foo{'longitude'};
	$result{'city'} = $City;
	$result{'region'} = $Region;
	$result{'country'} = $foo{'country_name'};
	$result{'latitude'} = $lat;
	$result{'longitude'} = $lon;
	$result{'certainty'} = ($lat && $lon) ? 100 : 0;

	return(%result);
}



sub geobutton()
{	my ($id)=@_;#id is the networldmap id
	my $now=time();
	my %location = &getLocation($ENV{'REMOTE_ADDR'});
	my $myplace;
	if(%location && ($location{'certainty'}>20))
	 	{ my $City = $location{'city'};
	 		my $Region = $location{'region'};
	 		my $Country = $location{'country'};
	 		$myplace = "$Region, $Country";
	 		if(!($City eq $Region)) { $myplace = "$City, $myplace"; }
	 		$myplace = "We think you are located in $myplace.  If that is not correct, please tell us by clicking on this button: ";
	 	}else 
	 	{ $myplace = "We Can't tell where you are, <em>please tell us by clicking on this button</em>: "; 
	 	}
	 $myplace .= "<a href='http://www.networldmap.com/'><img height=12 width=67 alt='the NetworldMap site' src='/images/geobutton.gif'></a> ";

	print <<BUTTONEND;
<script language=javascript>
 var im = new Image;
 im.src="http://button.geobutton.com/~$id/1/1/showbutton?$now";
</script>
<noscript>
<img ISMAP SRC="http://button.geobutton.com/~$id/1/1/showbutton?$now" NOSAVE BORDER=0 height=1 width=1>
</noscript>
BUTTONEND
return($myplace);
}


1
