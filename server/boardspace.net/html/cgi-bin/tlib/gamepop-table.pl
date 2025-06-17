
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );
use Mysql;
use Debug;
use strict;

#
# most popular games table
#
sub showtop_gamepop()
{	my ($dbh,$months,$leaguesize) = @_;
	my $dd = $months * 30;
	my $languagename = &select_language();
	my $subq = "SELECT count(variation)as vcount,variation from zertz_gamerecord"
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day)"
			. " GROUP BY variation"
			. " UNION"
			. " SELECT count(variation)as vcount,variation from mp_gamerecord"
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day)"
			. " GROUP BY variation"
			. " ORDER BY vcount asc"

			;
	my $substh = &query($dbh,$subq);
	my $subn = &numRows($substh);
	my $n = 0;
	my $val = 0;
	my @rows;
	#print "Q: $subq";
	my $str = "<table><tr>\n";
	while($subn>0)
		{	$subn--;
			$n++;
		  my ($prank,$pname) = $substh->fetchrow();
			my $comma = ($subn==0) ? "" : ",";
			my @ar = ($prank,$pname);
			$str .= push(@rows,\@ar);
			$val += $prank;
		}
	$str .= "</tr></table>\n";
	&finishQuery($substh);
	my $msg =  &trans("Most Popular Games");
	my $title = ($leaguesize<40) 
		? "'" . &trans('Link to a longer list') . "' target=_new"
		: "'" . &trans("Played in the last #1 months",$months) . "'";
    $msg = "<a href='/cgi-bin/boardspace_olympics.cgi' title=$title >$msg</a>";

	print "<h3><center>" . $msg . "</center></h3>";
	print "<table>";
	while($#rows > 0 && ($leaguesize-- > 0))
	{	my $arr = pop(@rows);
		my ($prank,$pname) = @{$arr};
		my $percent = int((($prank*100)/$val)+0.5);
		if($percent>0)
		{
		my $ppname = &trans("${pname}-pretty-name");
		my $fam = &gamename_to_family($dbh,$pname);
		my $plink = "<a href='/${languagename}/about_${fam}.html'>$ppname</a>";
		print "<tr><td>${percent}%</td><td>$plink</td></tr>\n";
		}
	}
	print "</table>\n";

	return($val,$str);
}

#
# friendliest players (players with the most opponents)
#
sub showtop_active()
{	my ($dbh,$months,$leaguesize) = @_;
	my $dd = $months * 30;
	my $subq = "SELECT p1info.player_name as name,count(player1) as count from zertz_gamerecord "
			. " left join players as p1info on p1info.uid=player1 "
			. " left join players as p2info on p2info.uid=player2 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) and p1info.is_robot IS NULL and p2info.is_robot IS NULL"
			. " GROUP BY player1 "
			
			. " UNION ALL"
			. " SELECT p2info.player_name as name,count(player2) as count from zertz_gamerecord "
			. " left join players as p1info on p1info.uid=player1 "
			. " left join players as p2info on p2info.uid=player2 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) and p1info.is_robot IS NULL and p2info.is_robot IS NULL"
			. " GROUP BY player2 "
			
			. " UNION ALL"			
			. " SELECT p1info.player_name as name,count(player1) as count from mp_gamerecord "
			. " left join players as p1info on p1info.uid=player1 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) "
			. " and p1info.uid is NOT NULL and p1info.is_robot IS NULL"
			. " GROUP BY player1 "
			
			. " UNION ALL"			
			. " SELECT p1info.player_name as name,count(player2) as count from mp_gamerecord "
			. " left join players as p1info on p1info.uid=player2 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) "
			. " and p1info.uid is NOT NULL and p1info.is_robot IS NULL"
			. " GROUP BY player2 "
			
			. " UNION ALL"			
			. " SELECT p1info.player_name as name,count(player3) as count from mp_gamerecord "
			. " left join players as p1info on p1info.uid=player3 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) "
			. " and p1info.uid is NOT NULL and p1info.is_robot IS NULL"
			. " GROUP BY player3 "
			
			. " UNION ALL"			
			. " SELECT p1info.player_name as name,count(player4) as count from mp_gamerecord "
			. " left join players as p1info on p1info.uid=player4 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) "
			. " and p1info.uid is NOT NULL and p1info.is_robot IS NULL"
			. " GROUP BY player4 "
			
			. " UNION ALL"			
			. " SELECT p1info.player_name as name,count(player5) as count from mp_gamerecord "
			. " left join players as p1info on p1info.uid=player5 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) "
			. " and p1info.uid is NOT NULL and p1info.is_robot IS NULL"
			. " GROUP BY player5 "
			
			. " UNION ALL"			
			. " SELECT p1info.player_name as name,count(player6) as count from mp_gamerecord "
			. " left join players as p1info on p1info.uid=player6 "
			. " WHERE gmtdate>date_sub(NOW(),interval $dd day) "
			. " and p1info.uid is NOT NULL and p1info.is_robot IS NULL"
			. " GROUP BY player6 "
			
			. " ORDER BY name desc"
			;
	my $substh = &query($dbh,$subq);
	my $subn = &numRows($substh);
	my %sums;
	while($subn-- > 0)
	{
	 my ($pname,$pcount) = $substh->fetchrow();
	 $sums{$pname} += $pcount;
	}
	my @rows;
	foreach my $key (keys(%sums))
	{ my @row = ($key, $sums{$key});
	  push(@rows,\@row);
	}
	@rows = sort { my @aa = @{$a}; 
	                 my @bb = @{$b};
	                 return($aa[1] <=> $bb[1]);
	                 } @rows;
	my $n = 0;
	my $msg =  &trans("Most Games Played");
	my $title = ($leaguesize<40) 
		? "'" . &trans('Link to a longer list') . "' target=_new"
		: "'" . &trans("Played against other players, in the last #1 months",$months) . "'";
    $msg = "<a href='/cgi-bin/boardspace_olympics.cgi' title=$title>$msg</a>";
	print "<h3><center>" . $msg . "</center></h3>";
	print "<table>";
	while($#rows>=0 && $leaguesize-- > 0)
	{   my $row = pop(@rows);
		my $name = @$row[0];
		my $val = @$row[1];
		my $ename = "<a href=javascript:editlink('$name',0)>$name</a>";
		print "<tr><td>$ename</td><td>$val</td></tr>\n";
	}
	&finishQuery($substh);
	print "</table>\n";

}

   

1
