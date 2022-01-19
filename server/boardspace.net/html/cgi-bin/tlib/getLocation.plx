#This Perl script gets a geo block from the database
#see http://glrs.networldmap.com/
#
require 5.001;
use LWP::UserAgent;
use strict;

use JSON;

sub hitlocation()
{	my ($hit) = @_;
	return(get("http://button.geobutton.com/~$hit/1/1/showiframe?"));
}

sub getLocation()
{	my ($IpAddress,$detailed) = @_;
    my $ua = new LWP::UserAgent( timeout => 10); # 10 seconds
	if($detailed) { print "GetLocation $IpAddress\n"; }
	#Get the remote clients geographical location from the Geographical Location Resolution Server
	my $GlrsUrl="http://getcitydetails.geobytes.com/GetCityDetails?fqcn=$IpAddress&Template=IpLocation.html";
	if($detailed) { print "url= $GlrsUrl<br>\n"; }
	
	my $request = HTTP::Request->new(GET => $GlrsUrl);
    my $response = $ua->request($request);
    if(!$response->is_success) { return(0); }
    my $Buffer = $response->content;
	if($detailed) {print "buffer=$Buffer<br>\n";}
	# look to see if 'unknown' appears in buffer - indicates an unresolved IP address
	my %result;
	my $ResolveCheck = index($Buffer,"unknown");
	if ( $ResolveCheck > 0 )
        {
	if($detailed) { print "unknown address $IpAddress<br>"; }
        return(0);
        }
	else
        {   my $fooref = decode_json($Buffer) ;
	    my %foo = %$fooref;


	    my $City = $foo{'geobytescity'};
	    my $Region = $foo{'geobytesregion'};
	    if($Region eq $City) { $Region = ""; };

	    $result{'city'} = $City;
	    $result{'region'} = $Region;
	    $result{'country'} = $foo{'geobytescountry'};
	    $result{'latitude'} = $foo{'geobyteslatitude'};
	    $result{'longitude'} = $foo{'geobyteslongitude'};
	    $result{'certainty'} = $foo{'geobytescertainty'};

	    return(%result);
	}
        
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
