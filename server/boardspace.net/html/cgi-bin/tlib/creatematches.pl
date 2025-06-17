use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;

require "../include.pl";
require "gs_db.pl";
require "common.pl";
#
# this creates matchparticipant records for all the individual matchrecord
# everything is direct except "outcome" field is translated to "win/loss"
#
sub createMatches()
{	my $dbh = &connect();
	my $q = "SELECT player1 as player,tournament,"
		. " if(outcome1='player1','win',if(outcome1='player2','loss',outcome1)) as outcome, played,"
		. " comment1 as comment,matchstatus,matchid, tournament_group,player1_points as points from matchrecord "
		. " union select player2 as player,tournament,"
		. " if(outcome2='player2','win',if(outcome2='player1','loss',outcome2)) as outcome, played, "
		. " comment2 as comment,matchstatus,matchid, tournament_group,player2_points as points from matchrecord ";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0)
	{
		my ($pl,$tid,$out,$played,$comm,$matchstatus,$matchid,$group,$points) = &nextArrayRow($sth);
		my $qpl = $dbh->quote($pl);
		my $qtid = $dbh->quote($tid);
		my $qout = $dbh->quote($out);
		my $qplayed = $dbh->quote($played);
		my $qcomm = $dbh->quote($comm);
		my $qsta = $dbh->quote($matchstatus);
		my $qmid = $dbh->quote($matchid);
		my $qgroup = $dbh->quote($group);
		my $qpoints = $dbh->quote($points);
		my $q1 = "insert into matchparticipant set player=$qpl,tournament=$qtid,outcome=$qout,played=$qplayed,"
			. " comment=$qcomm,matchstatus=$qsta,matchid=$qmid,tournament_group=$qgroup,points = $qpoints";
		&commandQuery($dbh,$q1);
	}
	&finishQuery($sth);
}
&createMatches();
