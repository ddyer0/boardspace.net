#  #not !/usr/bin/perl 
#
# cross link master games into a separate hierarchy.  Note that this is NOT
# executed from the web, but from a cron script.  Depends on the format of
# names of master games in several ways
#
#
# optional parameters:
# nitems=100                      =n to show top n players
#
use CGI qw(-debug :standard);
require "common.pl";
require "../include.pl";
require "../gs_db.pl";
use strict;

$::recurse = 1;

var @'months = ("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

sub link_master()
{  my ($dir,$file) = @_;
   my @parts = split(/-/,$file);
   if($#parts>6)
   {
   # parts>6 is to distinguish new format games with scores from old format games without.
   # we presume that all the old format games have been collected already
   my $master = &dircat(&dircat($::webroot,$::game_dir),"master") ;
   my $myr = &dircat($master,$parts[4]);
   my $mmo = &dircat($myr,$::months[$parts[5]]);
   my $mfi = &dircat($mmo,$file);
   if( ! -e $myr) { mkdir($myr,0755); }
   if( ! -e $mmo) { mkdir($mmo,0755); }
   if( ! -e $mfi) 
     { my $v = link(&dircat($dir,$file),$mfi); 
       print "link $mfi = $v\n";
   }
   }
}

sub match_dir
{	my ($dir) = @_;
	my @files = &list_dir($dir);
	my $file;
	foreach $file (@files)
	{my $cf = &dircat($dir,$file);
         if("." eq $file) {}
	 elsif (".." eq $file) {}
	 elsif ("challenge" eq $file) {}
	 elsif ("master" eq $file) {}
	 elsif (-d $cf)
	 { if($::recurse)
		{&match_dir($cf);
	     }
	 }
	 elsif ( (lc(substr($file,0,3)) eq "tm!") || (lc(substr($file,0,2)) eq "m!") )
	 { &link_master($dir,$file);
	 }

     }
}

{	print "Running master game collection\n";
	my $gdir = &dircat($::webroot,$::game_dir);
	&match_dir($gdir);
}

