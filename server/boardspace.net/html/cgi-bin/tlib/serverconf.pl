#!/usr/bin/perl

# 
# this generates the list of game directories and game names from the database
# in a format that the server can use as part of its init file
#
require 5.001;
use strict;
require "../include.pl";
require "gs_db.pl";
require "common.pl";
use File::Copy;
use CGI qw(:standard);
use Mysql;


{# this is a service routine 
 # it produces a list of the directory and names that are needed or projected
 my $dbh = &connect();
 if($dbh)
 {
 my $q = "select name,directory from variation where directory_index is not null and directory is not null group by directory order by directory_index";
 my $sth = &query($dbh,$q);
 my $nr = &numRows($sth);
 my $nd = $nr;
 my $gamestr = "ngamedirs,$nd\n";
 my $namestr = "ngametypes,$nd\n";
 my $idx = 0;
 my $xhta = "";
 while($nr-- > 0)
 {my ($name,$dir) = &nextArrayRow($sth);
  my $gamedir  = "$::www_root$dir";
  my $hta = "$gamedir/.htaccess";
  $namestr .= "gametype$idx,$name\n";
  $gamestr .= "gamedir$idx,$gamedir\n";

  # also make sure the game dir exists and will be acessable
  # to the web indexer
  my ($name,$maindir,$ext) = &filename_split($gamedir);
  if(! -e $maindir) { print "creating $maindir\n"; mkdir($maindir); }
  if(! -e $gamedir) { print "creating $gamedir\n"; mkdir($gamedir); }
  if( -e "$hta" ) { $xhta = "$gamedir/.htaccess" }
  elsif ($xhta) 
  { print "Copying $xhta to $hta\n";
    copy($xhta,$hta); 
  }

  $idx++;
 }
 &finishQuery($sth);
 print "#list produded by database_info.pl\n$namestr$gamestr";
}}

