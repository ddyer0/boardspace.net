#!/usr/bin/perl 
#
# generate rankings on demand.  Extracted from gs_74a2d.cgi on nov 23 1998 -ddyer
#
#
# optional parameters:
# nitems=100                      =n to show top n players
#
use CGI qw(:standard);
require "common.pl";
require "../include.pl";
require "../gs_db.pl";
use strict;


#
# the standard retirement clause, based on number of months since played
# the "retired" flag, and the desire to see retired or active players
#
sub retirement_clause()
{ my ($months,$retired) = @_;
  my $rtest = ">=";
  my $retired_test = 'AND is_retired IS NULL';

  if($months<=0) { $months=$'retire_months; };

  if($retired) 
	{ $rtest='<='; 
	  $retired_test='OR is_retired IS NOT  NULL'; 
	}

  my $sunset = time()-60*60*24*30*$months; # about number of seconds in that many months

  return( "AND last_played $rtest $sunset $retired_test" );
}

#
# main program
#
print header;
my @parameters = param();

{	my $retired = param('retired');
	my $months = param('months');	
 	my $retirement_test = &retirement_clause($months,$retired);

	my $dbh = &connect();

	# 
	# number of matching players for whome no location is known
	#
	my $nullquery = "SELECT COUNT(*) from players WHERE logitude IS NULL $retirement_test";
	my $sth = &query($dbh,$nullquery);
	my ($numnull) = &nextArrayRow($sth);
	&finishQuery($sth);
	print "nodata: $numnull\n";

	my $query = "SELECT player_name,latitude,logitude FROM players "
				. " WHERE logitude IS NOT NULL $retirement_test";

	my $sth = &query($dbh,$query);
	my $numr = &numRows($sth);

	print "rows: $numr\n";
	while($numr-->0)
	{ my ($who,$lat,$lon) = &nextArrayRow($sth);
	  print "$lat	$lon	$who\n";
	}
	&finishQuery($sth);
	&disconnect($dbh);
}

