#!/usr/bin/perl 
#
# generate rankings on demand.  Extracted from gs_74a2d.cgi on nov 23 1998 -ddyer
# http://www.tantrix.com/cgi-bin/tlib/gs_duplicate_games.cgi?collect=y&password=prettyplease&randomseed=27854394
#
# optional parameters:
# nitems=100                      =n to show top n players
#
use CGI qw(-debug :standard);
require "common.pl";
require "../include.pl";
require "../gs_db.pl";
use strict;

var %'games;

sub match_dir
{	my ($rootdir,$dir) = @_;
	my @files = &list_dir($dir);
	my $file;
	print "<br>Search $rootdir ($dir)\n<br>";
	
	foreach $file (@files)
	{	my $cf = &dircat($dir,$file);
		if("." eq $file) {}
		elsif (".." eq $file) {}
		elsif ("challenge" eq $file) {}
		elsif ($'games{$file})
		{ my $of = &dircat($rootdir,$file);
		  if(! -e $of)
		   {print "<br>found $of";
		       link($cf,$of);
		   }
		}
		elsif (-d $cf)
		{
			match_dir($rootdir,$cf);
		}
	}
	print "<br>done";
}

#
# main program
#
print header;
my @parameters = param();

{	my $randomseed = param('randomseed');
	my $getseeds = param('getseeds');
	my $collect = param('collect');
	my $password = param('password');
	my $dupemode = param('dupemode');
	my $table = $dupemode ? 'duplicate_games' : 'challenge_games';
	my $dbh = &connect();
	print "Running request for $randomseed ($collect) ($password)\n";
	if($getseeds>0)
	{ my $query = "SELECT DISTINCT randomseed FROM $table";
		my $sth = &query($dbh,$query);
		my $num = &numRows($sth);
		print "rows: $num\n";
		while($num>0)
		{	$num--;
			my ($seed) = $sth->fetchrow();
			print "$seed\n";
		}
		&finishQuery($sth);		
	}
	if($collect && $randomseed && ($password eq "prettyplease"))
	{ my $query = "SELECT name FROM $table WHERE randomseed=$randomseed ORDER BY seedset";
		my $sth = &query($dbh,$query);
		my $num = &numRows($sth);
		while($num>0)
		{	$num--;
			my ($name) = $sth->fetchrow();
			$'games{"${name}.sgf"}=1;
		}
		my $r = $ENV{'DOCUMENT_ROOT'};
		my $gdir = &dircat($r,$'game_dir);
		my $cdir = &dircat($gdir,"/challenge/$randomseed");
		if(! -e $cdir) { mkdir($cdir,755); }
		&match_dir($cdir,$gdir);
	        &match_dir($cdir,&dircat($gdir,"../xGames/"));
	}
	elsif($randomseed!=0)
	{	my $query = "SELECT seedset,p0,p1,s0,s1,name FROM $table WHERE randomseed=$randomseed ORDER BY seedset";
		my $sth = &query($dbh,$query);
		my $num = &numRows($sth);
		print "rows: $num\n";
		while($num>0)
		{	$num--;
			my ($set,$p0,$p1,$s0,$s1,$name) = $sth->fetchrow();
			print "$set $p0 $p1 $s0 $s1 $name\n";
		}
		&finishQuery($sth);		
	}

	&disconnect($dbh);
}