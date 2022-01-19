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
	 if( $dbh)
	 {
      my $game = &gamecode_to_gamename($dbh,param("game"));
      my $qgame = $dbh->quote($game);
	  print "OK";
	  $ok = 1;
	  for($i=1;$i<=10;$i++)
	  { my $p=param("u$i");
	    if($p ne "") 
	    { my $qp = $dbh->quote($p);

				if($game && $pstart) 
				{ &commandQuery($dbh,"UPDATE ranking SET games_started=games_started+1 WHERE uid=$qp and variation=$qgame");
				}
				else
	    	{
	      my $q = "SELECT value,variation from ranking WHERE uid=$qp";
	      my $sth = &query($dbh,$q);
	      my $rr = &numRows($sth);
	      while($rr-- > 0)
        {	my ($rank,$var) = $sth->fetchrow();
         my $nv = &gamename_to_gamecode($dbh,$var);
         if($nv)
	    	   {print " $p $nv $rank ";
          }
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