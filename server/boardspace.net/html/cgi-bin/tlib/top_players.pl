

sub top_players_table
{ my ($dbh,$number,$lan,$myname,@games) = @_;
  if($dbh)
  {
  my $language = &select_language($lan);
  my $idx = 0;
  my $months = $::retire_months;
  my $sunset = time()-60*60*24*30*$months; 

  my $t1 = ($number>0) 
	? &trans('Top players at Boardspace.net')
	: &trans('Games at Boardspace.net');
  if($number<=0) { @games = sort(@games); }
  my $fulllist = &trans('full_list');
  print "<b>$t1</b>";
  print "<table border=1 caption='$t1'><tr>";
  foreach my $game (@games)
  {
  my $qvar = $dbh->quote($game);
  my $vlink = "about_${game}.html";
  my $jname = &gamename_to_gamecode($dbh,$game);
  my $variation = &gamecode_to_gamename($dbh,$jname);
  my $vtr = &trans("${variation}-pretty-name");
  my $ncols = $::top_players_columns;  # new rows every 5 cols
  if((($idx>0)&&(($idx%$ncols)==0))) { print "</tr><tr>"; }
  print "<td valign=top>";
  print "<b><center><a target=_new href='$vlink'>$vtr</a></center></b>";

  if($number>0)
  {
  my $q = "SELECT player_name, value, country "
			. " FROM players left join ranking on players.uid=ranking.uid "
		    . " WHERE ranking.variation=$qvar AND (value > 0) "
				. " AND ((status='ok') or (status='nologin'))"
				. " AND (ranking.last_played > $sunset) "
				. " ORDER BY value DESC LIMIT $number";
  my $sth = &query($dbh,$q);
  my $nr = &numRows($sth);
  print "<table>\n";
  while($nr>0)
  {	$nr--;
	my ($player,$value,$country) = &nextArrayRow($sth);
	my $cflag = &flag_image($country);
	my $playerlink = "<a href=\"javascript:editlink('$player',1)\">$player</a>";
	print "<tr><td>$playerlink</td><td>$value</td><td>";
	print "<img width=33 height=22 src='$cflag' alt='$country'>";
	print "</td></tr>\n";
  }
  print "<tr><td colspan=3 align=center><a href='/cgi-bin/boardspace_rankings.cgi?game=$jname&language=$language&myname=$myname' target=_new>$fulllist</a></td></tr>\n";
  print "</table>\n";
  &finishQuery($sth);
  }

  print "</td>\n";
  $idx++;
  }
  print "</tr>";
  print "</table>\n";
  }
}

1
