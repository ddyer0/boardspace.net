#!/usr/bin/perl 
#
# July 2003 gs_uidrank display rank of players retrieved by uid
#
use CGI qw(:standard);
use strict;

require "include.pl";
require "tlib/gs_db.pl";

sub init {
        $| = 1;                         # force writes
}

init();
{   print header;
		my $ok=0;
    if( param() ) 
    {
		my $dbh = &connect($ENV{'REQUEST_URI'});
    my $pstart = param("start");
	  my $i;
	  #
	  # get player score
  	#
	  if($dbh)
	 {
	  print "OK";
	  $ok = 1;
	  for($i=1;$i<=10;$i++)
	  { my $p=param("u$i");
	    if($p ne "") 
	    { my $qp = $dbh->quote($p);

				if($pstart) 
				{ &commandQuery($dbh,"UPDATE players SET games_started=games_started+1 WHERE uid=$qp");
				}
				else
	    	{
	      my $q = "SELECT ranking,is_master from players WHERE uid=$qp";
	      my $sth = &query($dbh,$q);
	      my $rr = &numRows($sth);
	      if($rr==1)
	    	{	my ($rank,$ismaster) = $sth->fetchrow();
          my $master = 0;  # no master rankings for now
	    	  if($rank<=0) { $rank='new'; }
		      if(!(lc($ismaster) eq 'y')) { $master="0"; }
	    	  print " $p $rank $master";
	    	}
	    	&finishQuery($sth);
	    	}
	  }}
	print "\n";
  }}
  
  if(!$ok)
    	{	print "NOT OK\n";
    	}
}