#  #not !/usr/bin/perl 
#
# cross link tournament games into a separate hierarchy.  Note that this is NOT
# executed from the web, but from a cron script.  Depends on the format of
# names of master games in several ways
#
#
#
#use CGI qw(-debug :standard);
use CGI qw(:standard);
require "common.pl";
require "../include.pl";
require "gs_db.pl";
use strict;

$'recurse = 1;

var @'months = ("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

sub link_tournament()
{  my ($gamedir,$dir,$file,$doubles) = @_;
   my @parts = split(/-/,$file);
   {
   my $master = &dircat(&dircat($'webroot,$gamedir),"tournament") ;
   if(! -e $master) { mkdir($master,0755); }
   my $yr = $parts[3];
   if(0+$yr>1900)
   {
   my $myr = &dircat($master,$yr);
   my $mmo = &dircat($myr,$'months[$parts[4]]);
   my $mfi = &dircat($mmo,$file);
   if( ! -e $myr) { mkdir($myr,0755); }
   if( ! -e $mmo) { mkdir($mmo,0755); }
   if( ! -e $mfi) 
     { my $v = link(&dircat($dir,$file),$mfi); 
       #print "link $mfi = $v\n";
   }
   }
  }
}

sub match_dir
{	my ($gamedir,$dir) = @_;
	my @files = &list_dir($dir);
	my $file;
	foreach $file (@files)
	{my $cf = &dircat($dir,$file);
         if("." eq $file) {}
	 elsif (".." eq $file) {}
	 elsif ("challenge" eq $file) {}
	 elsif ("master" eq $file) {}
	 elsif (-d $cf)
	 { if($'recurse)
		{&match_dir($gamedir,$cf);
	     }
	 }
	 elsif ( (lc(substr($file,0,3)) eq "tm!")
                   || (lc(substr($file,0,3)) eq "tu!") 
                   || (lc(substr($file,0,3)) eq "td!")
                   || (lc(substr($file,0,3)) eq "tr!") 
                   || (lc(substr($file,0,2)) eq "t!") )
	 {  my $db = (lc(substr($file,0,3)) eq "td!");
            &link_tournament($gamedir,$dir,$file,$db);
	 }

     }
}

{	print "Running tournament game collection\n";
	my $dir;
	foreach $dir (@'game_dirs)
            {
            my $gdir = &dircat($'webroot,$dir);
	    &match_dir($dir, $gdir);
            #onetime match from unofficial archives
	    #&match_dir($dir,"$gdir/../xGames/");
            }
}


