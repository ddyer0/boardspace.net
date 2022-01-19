
sub favorite_games()
{	my ($dbh,$uid,$days,$lim) = @_;
	my $qid = $dbh->quote($uid);
	my $timeclause ="(gmtdate > date_sub(current_timestamp(),interval $days day))";
	my $zpart = "(SELECT count(gamename) as gname ,variation from zertz_gamerecord where  $timeclause and (player1=$qid or player2=$qid ) group by variation) ";
	my $mpart = "(SELECT count(gamename) as gname ,variation from mp_gamerecord where  $timeclause and (player1=$qid or player2=$qid or player3=$qid or player4=$qid or player5=$qid or player6=$qid) group by variation) ";
	my $q = "$zpart UNION $mpart order by gname desc limit $lim";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	my $val="";
	my $space = "";
	while($num-- > 0)
	{
	my ($n,$var) = &nextArrayRow($sth);
	$val.= "$space$var";
	$space = " ";
	}
	&finishQuery($sth);
	return($val);
}
1
