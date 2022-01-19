#This Perl script gets a geo block from the database
#see http://glrs.networldmap.com/
#
require 5.001;
use LWP::Simple (get);   #import the head method only
use strict;


sub getLocation()
{	my ($IpAddress,$detailed) = @_;
	if($detailed) { print "GetLocation $IpAddress\n"; }
	#Get the remote clients geographical location from the Geographical Location Resolution Server
	my $GlrsUrl="http://glrs.networldmap.com/scripts/glrs.dll?Getlocation&IpAddress=$IpAddress&MaxRecords=1&Template=IpLocation.html";
	if($detailed) { print "url= $GlrsUrl<br>\n"; }
	my $Buffer=get($GlrsUrl);

	if($Buffer eq "")
	{ if($detailed) { print "retry<br>\n"; $Buffer=get($GlrsUrl); }
      }
	if($Buffer eq "")
	{ if($detailed) { print "retry<br>\n"; $Buffer=get($GlrsUrl); }
      }
	if($Buffer eq "")
	{ if($detailed) { print "retry<br>\n"; $Buffer=get($GlrsUrl); }
      }

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

1
