#This Perl script gets a geo block from the database
#see http://glrs.networldmap.com/
#
use strict;
use CGI qw(:standard);

# and the libwww-perl bundle
use LWP::Simple;

sub getLocation()
{	my ($IpAddress) = @_;
	#Get the remote clients geographical location from the Geographical Location Resolution Server

	my $GlrsUrl="http://getcitydetails.geobytes.com/GetCityDetails?fqcn=$IpAddress&Template=IpLocation.html";
	
	print "Q: $GlrsUrl\n";
	#http://glrs.geoup.com/scripts/glrs.dll?Getlocation&IpAddress=$IpAddress&MaxRecords=1&Template=IpLocation.html
	
	my $Buffer=get($GlrsUrl);
	# look to see if 'unknown' appears in buffer - indicates an unresolved IP address
	my %result;
	my $ResolveCheck = index($Buffer,"unknown");
	print "B: $Buffer\n";

	if ( $ResolveCheck > 0 )
        {
        return(0);
        }
				else
        {
        #Extract required Information from Meta tags of the returned page.
        require HTML::HeadParser;
        my $p = HTML::HeadParser->new;
        $p->parse($Buffer);
		
        my $City = $p->header('city1');
	my $Region = $p->header('region1');
	if($Region eq $City) { $Region = ""; }
	$result{'city'} = $City;
        $result{'region'} = $Region;
        $result{'country'} = $p->header('country1');
        $result{'latitude'} = $p->header('latitude1');
        $result{'longitude'} = $p->header('longitude1');
        $result{'certainty'} = $p->header('certainty1');

	#my @keys = keys(%result);
	#my $key;
	#foreach $key(@keys) { print "$key = $result{$key}\n"; }
      return(%result);
      }
        
}


sub prlocation()
{ my ($name,$ip) = @_;
  my %res = &getLocation($ip);
  if(%res)
	{ my $City = $res{'city'};
	  my $Region = $res{'region'};
	  my $Country = $res{'country'};
	  my $Lat = $res{'latitude'};
	  my $Lon = $res{'longitude'};
	  my $Cert = $res{'certainty'};
	  my $res = 
	  print "$name	$ip	$Cert%	$City, $Region, $Country	$Lat,$Lon\n";
	}
}

1
