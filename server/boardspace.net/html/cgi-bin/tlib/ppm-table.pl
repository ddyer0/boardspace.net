
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );
use Mysql;
use Debug;


sub prow_ppm()
{    my ($brief,$n,$country,$flag,$players,$sum,$pop) = @_;
     if($pop==0) { $pop = 10000000; }
     my $pm = int($pop / 100000);
     my $pms = "$pm";
     $pms = substr($pm,0,-1) . "." . substr($pm,-1) . "m";
     my $pc = int((100.0*1000000*$players)/$pop);
     if($pc<10) { $pc = "0$pc"; }
     $pc = substr($pc,0,-2) . "." . substr($pc,-2,2);
     my $line = "<TR>" 
				. ($brief ? "" :  "<TD align=center>$n</TD>")
				. "<TD ALIGN=left>$country</TD>"
				. "<td>$flag</td>"
				. "<td align=center>$pc"
				. ($brief ? "" :"(of $pms)</td>")
				. "<TD align=center>$players</td>"
				. ($brief ? "" : "<td align=center>$sum</td></tr>");
     print $line;
}

sub showtop_ppm()
{	my ($dbh,$country,$leaguesize,$whereclause,$master_clause) = @_;
	my $qcountry = $dbh->quote($country);
	my $subq = "SELECT player_name,ranking from players "
						. " $whereclause $master_clause AND country=$qcountry "
						. "ORDER BY ranking DESC "
						. "LIMIT $leaguesize";
	my $substh = &query($dbh,$subq);
	my $subn = &numRows($substh);
	my $n = 0;
	my $val = 0;
	my $str = "<table><tr>\n";
	while($subn>0)
		{	$subn--;
			$n++;
		  my ($pname,$prank) = $substh->fetchrow();
			my $comma = ($subn==0) ? "" : ",";
			$str .= "<td>$pname:&nbsp;</td><td>$prank$comma&nbsp;</td>\n";
			$val += $prank;
		}
	$str .= "</tr></table>\n";
	&finishQuery($substh);
	return($val,$str);
}

sub ppm_table()
{
	my ($dbh,$months,$nitems,$mode,$retired,$leaguesize,$brief) = @_;
	
	my $t = &current_time_string();
	if($months=='') { $months=$'retire_months; }
	if($nitems=='') { $nitems=100; }
	my $rtestdir = $retired ? "<" : ">=";
	my $league = param("league") eq "y";
	my $rtest = $retired ? " and status='retired'" : " and status='ok'";
	my $sunset = time()-60*60*24*30*$months; 
	my $last_played = "last_played $rtestdir $sunset";
	my $whereclause = "WHERE (games_played > 0) AND ($last_played)";
	my $master_clause = ($mode eq "master") ? " AND is_master='Y'" : "";
	my $query = "SELECT country,count(player_name) AS val "
					. ", sum(games_played) "
					. ", IFNULL(country_info.population,10000000000) AS pop"
					. ", (count(player_name)*1000000.0)/IFNULL(country_info.population,10000000000) as PPM "
					. " FROM players LEFT JOIN country_info ON country_info.name=players.country "
					. " $whereclause $master_clause GROUP BY country ORDER BY PPM DESC";
	my $sth =&query($dbh,$query );
	my $numr = &numRows($sth);
	my $ptype = $retired ? "Retired" : "Regular";
	my $showall=0;
#print "$query<br>\n";
	my $tit = &trans("Players per million of population, active over the previous #1 months",$months);
	print "<p><center><TABLE align=center BORDER=0 CELLPADDING=2 CELLSPACING=4 title=$tit";

        my $minp = ($mode eq 'master') ? 1 : 3;

	if($league eq 'y')
		{
		my %vac;
	  print "<TR>"
			. $brief ? "" : "<TD align=center><b>Rank</b></TD>"
			. "<TD align=left colspan=2><b>Country</b></TD>"
			. "<td align=center><b>Total Score</b></td>"
	  		. "<TD align=center><b>$ptype&nbsp;players</b></TD><td align=left><b>Top&nbsp;players</b></td></tr>";

		while ($numr>0 )
			{
			$numr--;
			my ($country, $players, $sum, $pop, $pc) = $sth->fetchrow();
			if(($players>=$minp)
			   && !($country eq "?") 
			   && !($country eq "")
			   && !($country eq "Select country") 
			   && !($country eq "Cyberspace"))
				{
		my ($val,$str) = &showtop_ppm($dbh,$country,$leaguesize,$whereclause,$master_clause);	
		my $cflag = &flag_image($country);
		my $tcountry = &trans($country);
		my $flag = &flag_image($country);
		my $tstr =  "<td>$tcountry</td><td>$flag</td><td align=center>$val</td><td align=center>$players</td>\n<td>$str</td></tr>\n";
		# retain the ordinal number in the key to disambiguate ties
                # offset by 10000000 so the sort order is dominated by the integer
		$val = 100000000 + $val * 1000 + $numr;
				$vac{$val} = $tstr;
				}
			}
		  my $n = 0;
		 	my $key;
			foreach $key(reverse(sort(keys(%vac))))
				{
				$n++;
				if(($n<$nitems) ||  ($showall>0))
					{	print "<tr><td align=center>$n</td>$vac{$key}";
					}
				}
		}
		else
		{
	
{	my $pos = &trans("Position");
	my $cou = &trans("Country");
	my $pp = &trans("Players per Million");
	my $pl = &trans("Players");
	my $ga = &trans("Games Played");

  print "<TR>"
		. ($brief ? "" : "<td align=center><b>$pos</b></TD>")
		. "<TD align=left colspan=2><b>$cou</b></TD>"
		."<td align=center><b>$pp</b></td>"
		."<TD align=center><b>$pl</b></TD>"
		. ($brief ? "" : "<TD align=center><b>$ga</b></td>");
	}
  print "<TR>";
	my $n = 0;
  my $otherplayers = 0;
  my $othersum = 0;
  my $otherpop = 0;
  my $allsum=0;
  my $allplayers=0;
  my $allpop=0;
	while ($numr>0 && (($n<$nitems) ||  ($showall>0)))
		{
		$numr--;
		my ($country, $players, $sum, $pop, $pc) = $sth->fetchrow();
		if ($pop eq "") { $pop = 10000000000.0; };
	        if(!($country eq "?") 
		       && !($country eq "")
			&& !($country eq "Select country") 
			&& !($country eq "Cyberspace"))
		{
		$allplayers+=$players;
		$allsum+=$sum;
		$allpop+=$pop;
		my $tcountry = &trans($country);
		if($players>=$minp)
		{	my $flag = &flag_image($country);
		    $n++;
		    &prow_ppm($brief,$n,$tcountry,"<img src=\"$flag\" width=33 height=22 style=\"padding:2px\">",$players,$sum,$pop);
	  }else
		{
		    $otherplayers += $players;
		    $othersum += $sum;
		    $otherpop += $pop;
		}
	    }
		}
	my $flag = &flag_image('');
	my $allpopq = "select sum(population) from country_info";
	my $qpop = &query($dbh,$allpopq);
	my ($world) = &nextArrayRow($qpop);
	&finishQuery($qpop);

	if(!$brief)
	{	&prow_ppm($brief,"--",&trans("Others"),"<img src=\"$flag\" width=33 height=22 style=\"padding:2px\">",
		$otherplayers,$othersum,($world-$allpop+$otherpop));
		&prow_ppm($brief,"--",&trans("Grand Total"),"",$allplayers,$allsum,$world);
	}
		}

	print "</TABLE><br>";

	
	&finishQuery($sth);

   }
   

1
