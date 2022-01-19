#!/usr/bin/perl
#
# nitems=nn
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use Debug;
use Time::Local;
use strict;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";


sub this_month()
{  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time());
  return(timegm(0,0,0,1,$mon,$year));
}

sub prev_month()
{  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time());
  $mon--;
  if($mon<0) { $mon=11; $year--; }
  return(timegm(0,0,0,1,$mon,$year));
}

#
# query for a particular month, save the result in the stats table
#
sub update_stats()
{  my ($dbh,$start,$end,$recordas) = @_;
   my $dates = "(gmtdate>=$start and gmtdate<=$end)";

   {
   #
   # collect 2 player stats
   #
   my @variations = &all_2_variations($dbh);
   my @names = ("games_played", "master_games_played", "unranked_games_played" , "robot_games_played");
   my $q = "SELECT count(gmtdate) as games_played"
	. ",sum(if(mode='master',1,0)) as master_games_played"
	. ",sum(if(mode='unranked',1,0)) as unranked_games_played"
	. ",sum(if(pl1.is_robot='y' or pl2.is_robot='y',1,0)) as robot_games_played";

   foreach my $vv (@variations)
	{	$q .= ",sum(if(variation='$vv',1,0)) ";
		push @names,$vv;
	}

	$q .= " from zertz_gamerecord  as gr "
	    . " left join players as pl1 on pl1.uid=gr.player1 "
        . " left join players as pl2 on pl2.uid=gr.player2 "
		. " where $dates";

   my $sth = &query($dbh,$q);

   my @results = &nextArrayRow($sth);

   &finishQuery($sth);

   while($#names >= 0)
   {
	my $thisname = pop @names;
	my $thisval = pop @results;
	if($thisval > 0)
	{	my $qname = $dbh->quote($thisname);
		my $qval = $dbh->quote($thisval);
		my $v = "replace into single_site_stat set month_recorded = '$recordas',"
			. "stat_name=$qname, stat_value=$qval";
		&commandQuery($dbh,$v);
	}
   }}

   {
   #
   # collect 4 player stats
   #
   my @variations = &all_4_variations($dbh);
   my @names = ("games_played", "master_games_played", "unranked_games_played" , "robot_games_played");
   my $q = "SELECT count(gmtdate) as games_played"
	. ",sum(if(mode='master',1,0)) as master_games_played"
	. ",sum(if(mode='unranked',1,0)) as unranked_games_played"
	. ",sum(if(pl1.is_robot='y' or pl2.is_robot='y'  or pl3.is_robot='y'  or pl4.is_robot='y',1,0)) as robot_games_played";

   my $nVariations = $#variations+1;

   foreach my $vv (@variations)
	{	$q .= ",sum(if(variation='$vv',1,0)) ";
		push @names,$vv;
	}

	$q .= " from mp_gamerecord as gr "
	    . " left join players as pl1 on pl1.uid=gr.player1 "
        . " left join players as pl2 on pl2.uid=gr.player2 " 
		. " left join players as pl3 on pl3.uid=gr.player3 "
		. " left join players as pl4 on pl4.uid=gr.player4 "	;

   my $sth = &query($dbh,$q);

   my @results = &nextArrayRow($sth);

   &finishQuery($sth);

   while($#names >= 0)
   {
	my $thisname = pop @names;
	my $thisval = pop @results;
	$nVariations--;

	if($thisval > 0)
	{	my $qname = $dbh->quote($thisname);
		my $qval = $dbh->quote($thisval);
		if($nVariations<0)
		{
		my $v = "update single_site_stat set stat_value=stat_value+$qval "
			. " where stat_name=$qname and month_recorded='$recordas'";
		&commandQuery($dbh,$v);
		}
		else
		{
		my $v = "replace into single_site_stat set month_recorded = '$recordas',"
			. "stat_name=$qname, stat_value=$qval";
		&commandQuery($dbh,$v);
		}
	}
   }}

   {
   my $aq = "create temporary table ap select distinct player1  as someone from zertz_gamerecord WHERE $dates";
   my $aq2 = "insert into ap select distinct player2  as someone from zertz_gamerecord WHERE $dates";
   my $aq3 = "select count(distinct someone) from ap";
   &commandQuery($dbh,$aq);
   &commandQuery($dbh,$aq2);
   my $sthz = &query($dbh,$aq3);
   my ($act) = &nextArrayRow($sthz);
   &finishQuery($sthz);
   &commandQuery($dbh,"DROP TABLE ap");

   my $v = "replace into single_site_stat set month_recorded = '$recordas',"
			. "stat_name='active_players', stat_value='$act'";

   &commandQuery($dbh,$v);

   }

 
}


sub printRow()
{ my @args = @_;
	print "<tr>";
	while($#args>0)
		{ my $a = shift(@args);
		  print "<td align=right>$a</td>";
		}
	print "</tr>\n";
}

sub update_all_stats()
{	my ($dbh) = @_;
	my $oldest = "select distinct date_format(gmtdate,'%Y%m01') from zertz_gamerecord where gmtdate+0>0 order by gmtdate asc";
	my $sth = &query($dbh,$oldest);
	my $numr = &numRows($sth);
	my ($startdate) = &nextArrayRow($sth);
	$numr--;
	&commandQuery($dbh,"delete from single_site_stat ");

	while($numr-- > 0)
	{
	my ($enddate) = &nextArrayRow($sth);
	#print "from $startdate to $enddate<p>";
	&update_stats($dbh,$startdate,$enddate,$startdate);
	$startdate = $enddate;	
	}
	
	&finishQuery($sth);


}
sub btrans()
{	my ($in) = @_;
	return("<b>" . &trans($in) . "</b>");
}
sub show_activity()
{
	my $dbh = &connect();
	if($dbh)
	{ &readtrans_db($dbh);
      my ($sec,$min,$hour,$mday,$thismonth,$thisyear) = localtime(time);
      my @variations = &all_variations($dbh);
      if($thisyear<1900) { $thisyear+=1900; }
      $thismonth++;
      my $lastmonth = $thismonth-1;
      my $lastyear=$thisyear;
      my $nextmonth = $thismonth+1;
      my $nextyear = $thisyear;
      if($nextmonth==13) { $nextmonth=1; $nextyear++; }
      if($lastmonth==0) { $lastmonth=12; $lastyear--; };
      if($mday==31) { $mday=30; }
      if(($lastmonth==2) && ($mday>28)) { $mday=28; };
      if($mday<10) { $mday="0$mday"; }
      if($thismonth<10) { $thismonth="0$thismonth";  }
      if($nextmonth<10) { $nextmonth="0$nextmonth"; }
      if($lastmonth<10) { $lastmonth="0$lastmonth"; }
      my $tr = &trans("Site Usage Statistics");

      my $startthismonth = "${thisyear}${thismonth}01000000";
      my $startlastmonth = "${lastyear}${lastmonth}01000000";
      my $startnextmonth = "${nextyear}${nextmonth}01000000";
      my $currentmonth = "${nextyear}-${nextmonth}-01";
      my $monthago = "${lastyear}${lastmonth}${mday}000000";

      # recalculate from the beginning
	  #&update_all_stats($dbh);
       #&update_stats($dbh,"20040301000000","20040401000000","20040301000000");
       #&update_stats($dbh,"20040401000000","20040501000000","20040401000000");

      &update_stats($dbh,$startthismonth,$startnextmonth,$startthismonth);
      &update_stats($dbh,$startlastmonth,$startthismonth,$startlastmonth);
      &update_stats($dbh,$monthago,$startnextmonth,$startnextmonth);

	  my ($sth) = &query($dbh,
          "SELECT month_recorded,stat_name,stat_value from single_site_stat order by month_recorded desc");

	my ($n) = &numRows($sth);

	{
	my @row;

    push @row,&btrans('Year');
	push @row,&btrans('Month');
    push @row,&btrans('Active Players');
	push @row,&btrans('All Games');
	push @row,&btrans('Robot Games');
    push @row,&btrans('Unranked');

	foreach my $na (@variations)
	{	my $gp = "$na games";
		push @row,&btrans($gp);
	}
	print "<table width=550>\n";

	push @row,"";
	&printRow(@row);

   } # end of header row

	my $olddate;
	my %pairs;

	while($n-- >=0)
	{ my ($date,$stat,$val) = &nextArrayRow($sth);
	  if($olddate && !($olddate eq $date))
	  {
	  my @row;
	  my $tot = $pairs{'games_played'};
	  my $rob = $pairs{'robot_games_played'};
	  my $unr = $pairs{'unranked_games_played'};
	  my $robopc = ($tot==0)?0:int($rob*100/$tot);
      my $unrpc = ($tot==0)?0:int($unr*100/$tot);
	  my $yr = substr($olddate,0,4);
	  my $mo = substr($olddate,5,2);
	  if($olddate eq $currentmonth) { $yr = '30'; $mo = 'days'; }
	  push @row, $yr;
	  push @row, $mo;
	  push @row, $pairs{'active_players'};
	  push @row, $tot;
	  push @row, "$robopc%";
	  push @row, "$unrpc%";
	  
	  foreach my $na (@variations)
	  {	push @row,$pairs{$na};
	  }

	  push @row, "";
	  &printRow(@row);

	  %pairs = ();
	  }
	  $pairs{$stat} = $val;
	  $olddate = $date;
	}

	print "</table>\n";
	&finishQuery($sth);


	&disconnect($dbh);
	}
}


print header;
param();
&standard_header();
&show_activity();

&standard_footer();
