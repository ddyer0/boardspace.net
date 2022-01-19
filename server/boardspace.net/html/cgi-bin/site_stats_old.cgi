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
   my $q = "SELECT count(gmtdate),sum(if(variation='zertz',1,0)),"
       . " sum(if(variation='zertz+11',1,0)),"
       . " sum(if(variation='zertz+24',1,0)),"
       . " sum(if(variation='loa',1,0)),"
       . " sum(if(variation='loap',1,0)),"
       . " sum(if(variation='plateau',1,0)),"  
       . " sum(if(variation='yinsh',1,0)),"
       . " sum(if(variation='yinsh-blitz',1,0)),"
       . " sum(if(variation='hex',1,0)),"
       . " sum(if(variation='hex-15',1,0)),"
       . " sum(if(variation='hex-19',1,0)),"
       . " sum(if(variation='trax',1,0)),"
       . " sum(if(variation='punct',1,0)),"
       . " sum(if(variation='gobblet',1,0)),"
       . " sum(if(variation='gobbletm',1,0)),"
	   . " sum(if(variation='exxit',1,0)),"
	   . " sum(if(variation='tablut',1,0)),"
	   . " sum(if(variation='hive',1,0)),"
       . " sum(if(mode='master',1,0)),sum(if(mode='unranked',1,0)),"
       . " sum(if(pl1.is_robot='y' or pl2.is_robot='y',1,0))"
       . " from zertz_gamerecord as gr"
       . " left join players as pl1 on pl1.uid=gr.player1 "
       . " left join players as pl2 on pl2.uid=gr.player2 "
       . " where $dates";
   my $sth = &query($dbh,$q);
   #print "Q: $q<br>";
   my ($itot,$ztot,$z11tot,$z24tot,$loatot,$loaptot,$platot,$yintot,$ybtot,$hextot,$h15tot,$h19tot,
    $traxtot,$puncttot,$gobtot,$gobmtot,$exxittot,$tabtot,$hivetot,$mtot,$utot,$irobo) = &nextArrayRow($sth);
  &finishQuery($sth);
  if($itot>=0)
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
   my $m = "replace into site_stats set "
     . "month_recorded='$recordas',"
     . "games_played='$itot',"
     . "robot_games_played='$irobo',"
     . "master_games_played='$mtot',"
     . "unranked_games_played='$utot',"
     . "zertz_games_played='$ztot',"
     . "zertz11_games_played='$z11tot',"
     . "zertz24_games_played='$z24tot',"
     . "plateau_games_played='$platot',"
     . "yinsh_games_played='$yintot',"
     . "yinsh_blitz_games_played='$ybtot',"
     . "hex_games_played='$hextot',"
     . "hex15_games_played='$h15tot',"
     . "hex19_games_played='$h19tot',"
     . "trax_games_played='$traxtot',"
     . "punct_games_played='$puncttot',"
     . "gobblet_games_played='$gobtot',"  
     . "gobbletm_games_played='$gobmtot',"
	 . "exxit_games_played='$exxittot',"
	 . "tablut_games_played='$tabtot',"
	 . "hive_games_played='$hivetot',"   
     . "loa_games_played='$loatot',"
     . "loap_games_played='$loaptot',"
     . "active_players='$act'";
   &commandQuery($dbh,$m);
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
  
sub show_activity()
{
	my $dbh = &connect();
	if($dbh)
	{
      my ($sec,$min,$hour,$mday,$thismonth,$thisyear) = localtime(time);
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
      my $monthago = "${lastyear}${lastmonth}${mday}000000";

      # recalculate from the beginning
      # &update_stats($dbh,"20040301000000","20040401000000","20040301000000");
      # &update_stats($dbh,"20040401000000","20040501000000","20040401000000");

      &update_stats($dbh,$startthismonth,$startnextmonth,$startthismonth);
      &update_stats($dbh,$startlastmonth,$startthismonth,$startlastmonth);
      &update_stats($dbh,$monthago,$startnextmonth,$startnextmonth);
  my ($sth) = &query($dbh,
          "SELECT extract(year from month_recorded),extract(month from month_recorded),games_played,robot_games_played,"
        . " active_players,master_games_played,unranked_games_played,"
        . " zertz_games_played,zertz11_games_played,zertz24_games_played,"
        . " loa_games_played,loap_games_played,plateau_games_played,"
        . " yinsh_games_played,yinsh_blitz_games_played,"
        . " hex_games_played,hex15_games_played,hex19_games_played,"
        . " trax_games_played,punct_games_played,gobblet_games_played,gobbletm_games_played,"
		. " exxit_games_played,tablut_games_played,hive_games_played"
        . " FROM site_stats ORDER BY month_recorded");
  my ($n) = &numRows($sth);
	{
  my $yr=&trans('Year');
	my $mo=&trans('Month');
	my $ag=&trans('All Games');
	my $r1g = &trans('Robot Games');
      my $masg = &trans('Master Games');
      my $unh = &trans('Unranked');
      my $act = &trans('Active Players');
      my $ztot = &trans('Zertz Games');
      my $z11tot = &trans('Zertz+11 Games');
      my $z24tot = &trans('Zertz+24 Games');
     my $loatot = &trans('LOA Games');
     my $loaptot = &trans('LOAPS Games');
     my $platot = &trans('Plateau Games');
	 my $yintot = &trans('Yinsh Games');
     my $ybtot = &trans('Yinsh-Blitz Games');
	 my $hextot = &trans('Hex Games');
     my $h15tot = &trans('Hex-15 Games');
     my $h19tot = &trans('Hex-19 Games');
     my $traxtot = &trans('Trax Games');
     my $puncttot = &trans('Punct Games');
     my $gobtot = &trans('Gobblet Games');
	 my $gobmtot = &trans('GobbletM Games');
	 my $exxittot = &trans('Exxit Games');
	 my $tabtot = &trans('Tablut Games');
	 my $hivetot = &trans('Hive Games');

	print "<table width=550>\n";
	&printRow("<b>$yr</b>",
		  "<b>$mo</b>",
		  "<b>$act</b>",
		  "<b>$ag</b>",
 		  "<b>$masg</b>",
		  "<b>$r1g</b>",
		  "<b>$unh</b>",
         "<b>$ztot</b>",
          "<b>$z11tot</b>",
          "<b>$z24tot</b>",
          "<b>$loatot</b>",
          "<b>$loaptot</b>",
          "<b>$platot</b>",
		  "<b>$yintot</b>",
          "<b>$ybtot</b>",
		  "<b>$hextot</b>",
          "<b>$h15tot</b>",
          "<b>$h19tot</b>",
          "<b>$traxtot</b>",
          "<b>$puncttot</b>",
          "<b>$gobtot</b>",
          "<b>$gobmtot</b>",
		  "<b>$hivetot</b>",
		  "<b>$exxittot</b>",
		  "<b>$tabtot</b>",
          "");
      }
	while($n>0)
	{ $n--;
	  my ($year,$month,$itotal,$irobo,$num,$imaster,$iunranked,$ztotal,$z11total,$z24total,$loatotal,$loaptotal,
	              $platot,$yintot,$ybtot,
                  $hextot,$h15tot,$h19tot,
                  $traxtot,$puncttot,$gobtot,$gobmtot,$exxittot,$tabtot,$hivetot) = &nextArrayRow($sth);
        my $robopc = ($itotal==0)?0:int($irobo*100/$itotal);
        my $unrpc = ($itotal==0)?0:int($iunranked*100/$itotal);
    if($n==0) { $year="30"; $month="days"; }
	  &printRow($year,
			 $month,
			 "$num",
			 $itotal,
			 $imaster,
			 "$robopc %",
             "$unrpc %",
             $ztotal,$z11total,$z24total,$loatotal,$loaptotal,$platot,
             $yintot,$ybtot,
             $hextot,$h15tot,$h19tot,
             $traxtot,$puncttot,$gobtot,$gobmtot,
			 $hivetot,$exxittot,$tabtot,
             
                   "" );
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
