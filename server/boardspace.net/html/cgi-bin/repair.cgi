#!/usr/bin/perl
#
# one time program to repair the "max ever" ranking field
#

use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use IO::File;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";

sub doit()
{        

  my $dbh = &connect();              # connect to local mysqld
  if($dbh)
	{
	my $q = "select uid,max_rank,variation from ranking";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	print "$num rows\n";
	while($num-- > 0)
	{
		my ($uid,$rank,$variation) = &nextArrayRow($sth);
		my $quid = $dbh->quote($uid);
		my $qvar = $dbh->quote($variation);
		my $newmax = 0;
		my $newmax2 = 0;
		my $q2 = "select max(rank1) from zertz_gamerecord where player1=$quid and variation=$qvar";
		my $q3 = "select max(rank2) from zertz_gamerecord where player2=$quid and variation=$qvar";
		my $sth2 = &query($dbh,$q2);
		my $sth3 = &query($dbh,$q3);
		if(&numRows($sth2)>0) { ($newmax) = &nextArrayRow($sth2); }
		if(&numRows($sth3)>0) { ($newmax2) = &nextArrayRow($sth3); }
		&finishQuery($sth2);
		&finishQuery($sth3);
		if($newmax2>$newmax) { $newmax = $newmax2; }
		my $qnew = $dbh->quote($newmax);
		$q3 = "update ranking set max_rank=$qnew where uid=$quid and variation=$qvar limit 1\n";
		if($newmax!=$rank)
		{	&commandQuery($dbh,$q3);
			print "$q3";
		}

	}
	&finishQuery($sth);
	}

}

&doit();

