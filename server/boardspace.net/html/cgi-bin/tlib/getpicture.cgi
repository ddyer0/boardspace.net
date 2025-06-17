#!/usr/bin/perl
#
# this is a simple script that delivers a picture from the player
# pictures directory, or not if there is none.  This is in effect
# the same as <img xx> but doesn't cause an error log entry when
# the picture is missing, and also doesn't require the applet to
# know where the pictures are located.
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;
require "../include.pl";

sub hashKey()
{	my ($name) = @_;
	my $hash = 0;
	for my $ch (unpack("W*",$name))
	{	
	$hash = $hash*3+$ch;
	}	
	return($hash);
}

{ my $player = lc(param('pname'));
  my $name = $ENV{'DOCUMENT_ROOT'}.$::image_dir.$player.".jpg";
  #$| = 1;				# force writes
  print "Content-type: image/jpeg\n\n";
  if(open(IMAGE, $name))
  { binmode(IMAGE);
  	while (<IMAGE>){print $_;}
  	close(IMAGE);
  } 
  else 
  { my $hash = &hashKey($player) % 30;
    if($hash < 10) { $hash = "p0$hash.jpg"; } else { $hash = "p$hash.jpg"; } 
	my $stockName = $ENV{'DOCUMENT_ROOT'} . "/images/portraits/$hash";
	if(open(IMAGE, $stockName))
	{ binmode(IMAGE);
  	  while (<IMAGE>){print $_;}
  	  close(IMAGE);
    } 
  }
}