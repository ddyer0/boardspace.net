
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;

use strict;

require "tlib/ppm-table.pl";
require "tlib/gamepop-table.pl";

my %num1;
my %person;

#
# add the top 3 players for a particular variation
#
sub addRows()
{
	my ($dbh,$where,$months) = @_;
	my $sunset = time()-60*60*24*30*$months; 
	my $rtest = '>';
	my $lp = "ranking.last_played";
	my $q = "SELECT variation,ranking.uid,player_name,country,value "
			. " FROM ranking LEFT JOIN players ON ranking.uid=players.uid "
			. " $where AND (status='ok') and ($lp $rtest $sunset) ORDER BY value desc LIMIT 3";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	my $n = 0;
	while($num>0)
	{
		$num--;
		my ($var,$uid,$name,$country,$value) = &nextArrayRow($sth);
		
		{
		my $counts = $num1{$country};
		if ($#$counts==0) { $counts = [7]; }
		$$counts[$n]++;
		$$counts[$n+3] .= "$var ";	# save details
		$$counts[6] = $country;
		$num1{$country}=  $counts;
		#print "C $country @$counts<br>\n";
		}
		
		{
		my $counts = $person{$name};
		if ($#$counts==0) { $counts = [0,0,0]; }
		$$counts[$n]++;
		$$counts[$n+3] .= "$var ";
		$$counts[6] = $country;
		$person{$name}= $counts;
		#print "P $name @$counts<br>\n";
		}

		$n++;
	}
	&finishQuery($sth);
}
sub build_ppm_table()
{	my($dbh,$months,$nitems) = @_;
	my $msg = "<h3><center>" . &trans("National Popularity") . "</center></h3>";
	my $tit = ($nitems<40)
		? "'" . &trans('Link to a longer list') . "'  target=_new"
		: "'" . &trans("Players per million of population, active over the previous #1 months",$months) . "'";
	$msg = "<a href='/cgi-bin/boardspace_olympics.cgi' title=$tit>$msg</a>";
	print $msg;
	&ppm_table($dbh,$months,$nitems,'','','',1);
}

#
# fill the temp table with data from a hash containing the
# accumulated counts and details.
#
sub buildFromHash()
{	my ($dbh,$ttype,$num1) = @_;

	for my $key (keys %$num1)
	{	my $val = $$num1{$key};
		my $qc = $dbh->quote($key);
	    my $dt1 = $dbh->quote($$val[3]);
	    my $dt2 = $dbh->quote($$val[4]);
	    my $dt3 = $dbh->quote($$val[5]);
	    my $country = $dbh->quote($$val[6]);
	    my $qt = $dbh->quote($ttype);
	  my $q = sprintf("INSERT INTO rankstats SET ctype=$qt,ordinal1='%d',ordinal2='%d',ordinal3='%d',country=$country"
						. ",details1=$dt1,details2=$dt2,details3=$dt3"
						. ",name=$qc",
						$$val[0],$$val[1],$$val[2]);
	  &commandQuery($dbh,$q);
	}

}
sub buildTempTable()
{	my ($dbh,$months) = @_;
	my @player2 = &all_2_variations($dbh);
	for my $pl (@player2) 
	{  my $qn = $dbh->quote($pl);
	   &addRows($dbh,"where variation=$qn",$months);
	 }
	 
	my @player4 = &all_4_variations($dbh);
	for my $pl (@player4)
	{  my $qn = $dbh->quote($pl);
	   &addRows($dbh,"where variation=$qn",$months);
	 }
	my $q = "CREATE TEMPORARY TABLE rankstats (ctype text,name text,details1 text,details2 text,details3 text,ordinal1 int,ordinal2 int,ordinal3 int, country text)";
	&commandQuery($dbh,$q);
	
	&buildFromHash($dbh,"country",\%num1);
	&buildFromHash($dbh,"person",\%person);
	
}
sub showTable()
{	my ($dbh,$lim,$ctype,$months) = @_;
	my $q = "SELECT name,country,ordinal1,ordinal2,ordinal3,details1,details2,details3 from rankstats where ctype='$ctype' ORDER BY ordinal1*1.2+ordinal2*1.1+ordinal3 desc limit $lim";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	my $gold = &trans("Gold");
	my $silver = &trans("Silver");
	my $bronze = &trans("Bronze");
	my $msg = ($ctype eq 'country') ? &trans("National Standings")
					: &trans("Top Players");
	my $tit = ($lim<40)
		? "'" . &trans('Link to a longer list') . "' target=_new"
		: "'" . &trans(($ctype eq 'country')
						? "Countries whose players rank first second or third in any game, active in the last #1 months"
						: "Players who rank first, second or third, active in the last #1 months",$months) . "'";
	
    $msg = "<a href='/cgi-bin/boardspace_olympics.cgi' title=$tit>$msg</a>";
	print "<h3>$msg</h3>";
	print "<table cellpadding=2";
	my $gim = "<img src='/images/stock/gold.png' alt='$gold'>";
	my $sim = "<img src='/images/stock/silver.png' alt='$silver'>";
	my $bim = "<img src='/images/stock/bronze.png' alt='$bronze'>";

	print "<tr><td>&nbsp;</td><td>&nbsp;</td><td><b>$gim</b></td><td><b>$sim</b></td><td><b>$bim</b></td></tr>";
	while($num-- > 0)
	{
		my ($name,$country,$order1,$order2,$order3,$details1,$details2,$details3) = &nextArrayRow($sth);
		my $flag = &flag_image($country);
		print "<tr>";
		my $ename = ($ctype eq 'person')
			 ? "<a href=javascript:editlink('$name',0)>$name</a>"
			 : &trans($name);

		print "<td>$ename</td><td><img src='$flag' width=33 height=22>"
			. "</td><td align=center title='$details1'>$order1</td>"
			. "<td align=center title='$details2'>$order2</td>"
			. "<td align=center title='$details3'>$order3</td>";
		print "</tr>\n";
	}
	print "</table>";
	&finishQuery($sth);
}

sub showVersatilityTable()
{	my ($dbh,$lim,$months) = @_;
	my $sunset = time()-60*60*24*30*$months; 
	my $q = "SELECT player_name,count(value) as vc,country "
			. " FROM ranking left join players on players.uid=ranking.uid "
		    . " WHERE (value > 0) "
				. " AND ((status='ok') or (status='nologin'))"
				. " AND (players.is_robot is null)"
				. " AND (ranking.last_played > $sunset) "
			. " GROUP BY ranking.uid "
			. " ORDER BY vc DESC LIMIT $lim";	
	#print "Q: $q<p>";			
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	my $msg = &trans("Most Versatile Players");
	my $title = ($lim<40)
		? "'" . &trans('Link to a longer list') . "' target=_new"
		: "'" . &trans("Played the most different games over the last #1 months",$months) . "'";
    $msg = "<a href='/cgi-bin/boardspace_olympics.cgi' title=$title>$msg</a>";
	print "<h3>$msg</h3>";
	print "<table cellpadding=2";
	my $ngames = &trans("Different games");
	print "<tr><td>&nbsp;</td><td  colspan=2><b>$ngames</b></td></tr>";
	while($num-- > 0)
	{
		my ($name,$value,$country) = &nextArrayRow($sth);
		my $gvalue = &trans("#1 games",$value);
		my $flag = &flag_image($country);
		print "<tr>";
		my $ename = "<a href=javascript:editlink('$name',0)>$name</a>";
		print "<td>$ename</td><td><img src='$flag' width=33 height=22>"
			. "</td><td align=center >$gvalue</td>";
		print "</tr>\n";
	}
	print "</table>";
	&finishQuery($sth);
}
sub selector1()
{	my ($key,$pal,$sunset,$tab) = @_;
	return "( "
			." SELECT distinct $key as p1,$pal as p2 "
			. " FROM $tab "
			. " USE INDEX (dateindex) "
			. " WHERE $key!=0 "
				. " AND $pal!=0 "
			# note that this is easier for mysql to optimize than unix_timestamp(gmtdate)>$sunset
			. " AND gmtdate > FROM_UNIXTIME($sunset) "
			. " )\n";
			
}
sub selector()
{	my ($key,$sun) = @_;
	my $un = "";
	my $res = "";
	for(my $val = 1; $val<7; $val++)
	{	if($val != $key)
		{
		$res .= $un . &selector1("player$key","player$val",$sun,"mp_gamerecord") ;
		$un = " UNION ";
		}
	}
	return($res);
}
sub zselector()
{	my ($key,$sun) = @_;
	my $un = "";
	my $res = "";
	for(my $val = 1; $val<3; $val++)
	{	if($val != $key)
		{
		$res .= $un . &selector1("player$key","player$val",$sun,"zertz_gamerecord") ;
		$un = " UNION ";
		}
	}
	return($res);
}
sub showFriendlyTable()
{	my ($dbh,$lim,$months) = @_;
	my $sunset = time()-60*60*24*30*$months; 
	my $thr = 4;
	my $q = "CREATE TEMPORARY TABLE PAIRS "
			. &selector(1,$sunset)
			. " UNION " . &selector(2,$sunset)
			. " UNION " . &selector(3,$sunset)
			. " UNION " . &selector(4,$sunset)
			. " UNION " . &selector(5,$sunset)
			. " UNION " . &selector(6,$sunset)
			. " UNION " . &zselector(1,$sunset)
			. " UNION " . &zselector(2,$sunset)
		    ;	

	#print "Q: $q<p>";			
	&commandQuery($dbh,$q);
	my $q2 = "Select player_name,count(distinct(p2))as value, country from PAIRS "
		. " left join players on p1=uid "
		. " where is_robot is null "
		. " group by p1 "
		. " order by value desc"
		. " limit $lim";
	#print "Q2 $q2<p>";
	my $sth = &query($dbh,$q2);
	my $num = &numRows($sth);
	my $msg = &trans("Friendliest Players");
	my $title = ($lim<40)
		? "'" . &trans('Link to a longer list') . "' target=_new"
		: "'" . &trans("Played the most different games over the last #1 months",$months) . "'";
    $msg = "<a href='/cgi-bin/boardspace_olympics.cgi' title=$title>$msg</a>";
	print "<h3>$msg</h3>";
	print "<table cellpadding=2";
	my $ngames = &trans("Different Opponents");
	print "<tr><td>&nbsp;</td><td  colspan=2><b>$ngames</b></td></tr>";
	while($num-- > 0)
	{
		my ($name,$value,$country) = &nextArrayRow($sth);
		my $gvalue = &trans("#1 players",$value);
		my $flag = &flag_image($country);
		print "<tr>";
		my $ename = "<a href=javascript:editlink('$name',0)>$name</a>";
		print "<td>$ename</td><td><img src='$flag' width=33 height=22>"
			. "</td><td align=center >$gvalue</td>";
		print "</tr>\n";
	}
	print "</table>";
	&finishQuery($sth);
}

sub show_ordinal_rankings() 
{ 	my ($dbh,$nitems,$months,$show) = @_;
	if($dbh)
	{
	if($show>0)
	{#show some number of adjacent items
	 my $maxcols = 8;
	 my $bit = 1<<$maxcols;
	 my $randomizer = (time()/111)%($maxcols);
	 my $mask = ((1 << $show) -1);
	 $show = $mask << $randomizer;
	 if($show & $bit) { $show ^= ($bit|1); }
	}
	print "<table border=2 cellpadding=2><tr align=center>";
	
	&buildTempTable($dbh,$months);

	if($show&1)
	{ 
	  print "<td valign=top>";
	  &build_ppm_table($dbh,($months<3?3:$months),$nitems);
	  print "</td>";
	}

	if($show & 2)
	{
	print "<td valign=top>";
	&showTable($dbh,$nitems,'country',$months);
	print "</td>";
	}

	if($show & 4)
	{
	print "<td valign=top>";
	&showTable($dbh,$nitems,'person',$months);
	print "</td>";
	}
	if($show & 0x8)
	{
	print "<td valign=top>";
	&showVersatilityTable($dbh,$nitems,$months);
	print "</td>";
	}

	if($show & 0x10)
	{
	print "<td valign=top>";
	&showtop_active($dbh,($months<3?3:$months),$nitems+2);
	print "</td>";
	
	}
	
	if($show & 0x20)
	{
	print "<td valign=top>";
	&showFriendlyTable($dbh,$nitems,$months);
	print "</td>";
	}
	

	if($show & 0x40)
	{
	print "<td valign=top>";
	&showtop_gamepop($dbh,($months<3?3:$months),$nitems+2);
	print "</td>";
	}


	if($show>0 && ($show & 0x80))
	{
	 print "<td valign=center>";
	 print "<a href='/english/playermap.shtml'>";
	 print "<center>Boardspace player map</center><br>";
	 print "<image src='/images/community-map.jpg'>";
	 print "</a>";
	 print "</td>";
	}
		
	print "</tr></table>";
	
	}
}
1
