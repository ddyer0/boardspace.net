#!/usr/bin/perl
#
# generate player versus player results.  Based on gs_player_analysis by Ben Pollman
#
#

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
# use Debug;
use URI::Escape;
use strict;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";

#$database='test';

# print an underline/section separator in the main table
sub separator()
{
  print "<hr>";
}

sub print_game_selector()
{	my ($dbh,$game) = @_;
	my @variations = &all_variations($dbh);
    
	foreach my $var (@variations)
	{	my $sel = ($game eq $var) ? " selected" : "";
		print "<option value='$var'$sel>$var</option>";
	}
	
}

sub comparison_header()
{ my ($dbh,$game,$maxplayers,
		$player1,$player2,$player3,$player4,$player5,$player6,
		$uid1,$uid2,$uid3,$uid4,$uid5,$uid6,
		$start_date,$end_date)=@_;
  my $header=param('header');
  my $sorting=param('sort');
  my $master=param('master');
  my $delim=param('delim');
  my $tg = param('tg');
  my $str1 = "";
  my $str2 = "";
  my $str3 = "";
  my $str4 = "";
  my $str5 = "";
  my $str6 = "";
  
  my $ghead = &trans("Boardspace #1 Games Analysis",$game);
  print "<head><title>$ghead</title></head>\n";
  &standard_header();

  print "<form action=$ENV{'SCRIPT_NAME'}>\n";
#  my $name;
#  foreach $name (param())
#  { my $val = param($name);
#    if(!($name eq 'country') && !($name eq 'myname') && !($name eq 'player1') && !($name eq 'player2') &&
#       !($name eq 'start_date') && !($name eq 'end_date') && !($name eq 'fulltable') && 
#	!($name eq 'sort') && !($name eq 'delim') )
#    { print "<input type=hidden name=$name value=$val>\n";
#    }
#  }
  my ($nat) = &trans('Get Results');
  $str1 = &trans("Player unknown") if ( $player1 && ! $uid1 ) ;
  $str2 = &trans("Player unknown") if ( $player2 && ! $uid2 ) ;
  $str3 = &trans("Player unknown") if ( $player3 && ! $uid3 ) ;
  $str4 = &trans("Player unknown") if ( $player4 && ! $uid4 ) ;
  $str5 = &trans("Player unknown") if ( $player5 && ! $uid5 ) ;
  $str6 = &trans("Player unknown") if ( $player6 && ! $uid6 ) ;

  print &trans("Select Game")
	. ": <select name=game><option value=''>"
	. &trans("All Games") 
	. "</option>";
  &print_game_selector($dbh,$game);
  print "</select><br>";
  print "<p><table width=50%><tr>";
  {my $idx = 1;
   print "<tr>";
   while($idx<=$maxplayers)
   { print "<TD>Player $idx</TD>\n";
     $idx++;
   }
   print "</tr>\n";
  }
  
  print "<TD width=\"40\"><input type=text name=player1 value=$player1></TD>\n";
  print "<TD width=\"40\"><input type=text name=player2 value=$player2></TD>\n";
  if($maxplayers>2) {print "<TD width=\"40\"><input type=text name=player3 value=$player3></TD>\n";}
  if($maxplayers>3) {print "<TD width=\"40\"><input type=text name=player4 value=$player4></TD>\n";}
  if($maxplayers>4) {print "<TD width=\"40\"><input type=text name=player5 value=$player5></TD>\n";}
  if($maxplayers>5) {print "<TD width=\"40\"><input type=text name=player6 value=$player6></TD>\n";}

  print "<TD>&nbsp;&nbsp;<input type=submit value=\"$nat\"></TD></TR>\n";
  print "<TR><TD><font color=\"red\">$str1</font></TD><TD>"
	. "<font color=\"red\">$str2</font></TD></TR>\n" if ( $str1 || $str2 );   
  print "</table>";
  print "<p><table border=0>";
  print "<TR><TD>Start date</TD><TD>End date</TD></TR>\n";
  print "<TR><TD><input type=text name=start_date value=$start_date></TD>\n";
  print "<TD><input type=text name=end_date value=$end_date></TD><td>&nbsp;&nbsp;"
	. &trans("Date format: yyyy/mm/dd[:hh:mm]")
	. "</td></TR>\n";
  print "</table><br>";
  print "<BR>"
  . &trans("If only one player is entered, sort the opponents by")
  . "<select class='sml' name=\"sort\">";

  if ( $sorting eq 'gamesdown' || $sorting eq '' ) {
	print "<option value=\"gamesdown\" selected>"
		. &trans("Number of Games descending")
		. "\n";
  } else {
	print "<option value=\"gamesdown\">"
	. &trans("Number of Games descending")
	. "\n";
  }
  if ( $sorting eq 'gamesup' ) {
	print "<option value=\"gamesup\" selected>"
		. &trans("Number of Games ascending")
		. "\n";
  } else {
	print "<option value=\"gamesup\">"
	. &trans("Number of Games ascending")
	. "\n";
  }
  if ( $sorting eq 'win' ) {
	print "<option value=\"win\" selected>"
	. &trans("Win Percentage")
	. "\n";
  } else {
	print "<option value=\"win\">"
	. &trans("Win Percentage")
	. "\n";
  }
  if ( $sorting eq 'loss' ) {
	print "<option value=\"loss\" selected>"
		. &trans("Loss Percentage")
		. "\n";
  } else {
	print "<option value=\"loss\">"
		. &trans("Loss Percentage")
		. "\n";
  }
  print '</select>';
  print "<br><br><input type=\"checkbox\" name=\"fulltable\">"
	. &trans("Display all game details instead of only last 50 (but never more than 500).")
	. "\n";
  #print "<br><input type=\"checkbox\" name=\"master\"";
  #print " checked" if ( $master );
  #print ">Include only master games.\n";
  print "<br><input type=\"checkbox\" name=\"tg\"";
  print " checked" if ( $tg );
  print ">"
	. &trans("Include only tournament games.")
	. "\n";
  print "<br>"
   . &trans("Display only opponents against who at least #1 games were played.",
		"<input type=text size=\"1\" name=delim value=$delim>")
   . "</form><br><br>\n";
 
}

sub timestr
{
  my $t = shift;
  my $min = int($t/60);
  my $sec = $t - $min*60;
  $sec = "0" . $sec if ( length($sec) == 1 );
  return("$min:$sec");
}

sub datestr
{
  my $date = shift;
  my $y = substr($date,0,4);
  my $mon = substr($date,4,2);
  my $day = substr($date,6,2);
  my $hh = substr($date,8,2);
  my $min = substr($date,10,2);
  return($y."-".$mon."-".$day.":$hh:$min");
}


sub convert_date
{
  my $date = shift;
  my ($mm,$hh,$d);
  my ($y,$m,$td) = split('/',$date);
  if ( $td =~ /:/ ) {
      ($d,$hh,$mm) = split(':',$td);
  } else {
      $mm = $hh = "00";
      $d = $td;
  }
  $hh = "0" . $hh if ( length($hh) == 1 );
  $mm = "0" . $mm if ( length($mm) == 1 );
  $mm = "00" if ( length($mm) == 0 );
  $d = "0" . $d if ( length($d) == 1 );
  $m = "0" . $m if ( length($m) == 1 );
  $y = "20" . $y if ( length($y) == 2 );
  my $str = $y . $m. $d . $hh . $mm . "00";
  
  $str = "" if ( length($str) != 14 );
  return($str);
}
   
sub get_uid
{ my ($dbh,$name) = @_;
  my $uid;
 
  if($name)
  {  my $qname = $dbh->quote($name);
     my $sth = &query($dbh,"SELECT uid from players where player_name=$qname");
     $uid = (&numRows($sth)>0) ? $sth->fetchrow() : "";
     &finishQuery($sth);
  }
  return($uid);
}

sub print_summary
{ my ($titel,$games,$won,$lost,$tied,$t1,$t2) = @_;

   my $percentage = ($games>0) ? sprintf ' (%1.1f%)',100*$won/($games) : "n/a";
   my $opp_percentage = ($games>0) ? sprintf ' (%1.1f%)',100*$lost/($games) : "n/a";
   my $tie_percentage = ($games > 0) ? sprintf ' (%1.1f%)',100*$tied/($games) : "n/a";

   $t1 = int($t1/$games);
   $t2 = int($t2/$games);
   my $str1 = timestr($t1);
   my $str2 = timestr($t2);

   print "<TR><TD align=left><b>$titel</b></TD>";
   print "<TD align=left><b>$won</b>$percentage</TD>";
   print "<TD align=left><b>$lost</b>$opp_percentage</TD>";
   print "<TD align=left><b>$tied</b>$tie_percentage</TD>";
  
   print "<TD><b>$games</b></TD><td>$str1</td><td>$str2</td></TR>\n";

}
sub playerclause()
{	my ($multi,$uid) = @_;
	my $res = "(player1=$uid) or (player2=$uid)";
	if($multi) { $res .= "or (player3=$uid) or (player4=$uid)" }
	return("($res)");
}
sub getGameInfo()
{	my ($dbh,$game) = @_;
	my $qgame = $dbh->quote($game);
	my $q = "select max_players from variation where name = $qgame";
	my $sth = &query($dbh,$q);
	my ($max) = &nextArrayRow($sth);
	if(!$max) { $max = 2; }
	&finishQuery($sth);
	return($max);
}
sub compare_players 
{ my $game = param('game');		
  my $player1=param('player1');
   $player1 = &despace(substr($player1,0,25));
  my $player2=param('player2');
   $player2 = &despace(substr($player2,0,25));
  my $player3=param('player3');
   $player3 = &despace(substr($player3,0,25));
  my $player4=param('player4');
   $player4 = &despace(substr($player4,0,25));
  my $player5=param('player5');
   $player5 = &despace(substr($player5,0,25));
  my $player6=param('player6');
   $player6 = &despace(substr($player6,0,25));
  my ($robot, $robot2, $robot3, $goodbot);
  my $robtot = -1;
  my $start_date=param('start_date');
  my $end_date=param('end_date');
  my $fulltable=param('fulltable');
  my $sorting=param('sort');
  my $delim=param('delim');
  my $master=param('master');
  my $tg=param('tg');
  my $uid1=0; 
  my $uid2=0;
  my $uid3=0;
  my $uid4=0;
  my $uid5=0;
  my $uid6=0;

  my $dbh = &connect();              # connect to local mysqld

  if($dbh)
  {	 
	&readtrans_db($dbh);
    my $t = &current_time_string();
	my $start_str = convert_date($start_date);
	my $end_str = convert_date($end_date);

 	$uid1 = get_uid($dbh,$player1);
	$uid2 = get_uid($dbh,$player2);
	$uid3 = get_uid($dbh,$player3);
	$uid4 = get_uid($dbh,$player4);
	$uid5 = get_uid($dbh,$player5);
	$uid6 = get_uid($dbh,$player6);

	$robot = get_uid($dbh,"dumbot");
	$robot2 = get_uid($dbh,"smartbot");
	$robot3 = get_uid($dbh,"bestbot");

	my ($maxplayers) = &getGameInfo($dbh,$game);

	&comparison_header($dbh,$game,$maxplayers,
				$player1,$player2,$player3,$player4,$player5,$player6,
				$uid1,$uid2,$uid3,$uid4,$uid5,$uid6,
				$start_date,$end_date);
	#
    {   if ( !$uid1 ) 
		{
		$uid1 = $uid2;
		$uid2 = 0;
		$player1 = $player2;
		$player2 = "";
		} 
	my $multiplayer = ($maxplayers > 2);
	my $subquery = " gmtdate >= $start_str " if ( length($start_str) == 14 );
	$subquery = $subquery . " AND gmtdate <= $end_str " if ( length($end_str) == 14 );
	#$subquery = $subquery . " AND mode = 'MASTER' " if ($master);
	$subquery = $subquery . " AND tournament = 'yes' " if ($tg);

	#
	# the general strategy is to extend the query to 6 players, and specifically
	# construct it for 2 zertz-gamerecord or 4 mp_gamerecord.  The two extra are 
	# forseeable expansions of mp_gamerecord.
	#
	my $bselect = "";

	if($multiplayer)
	{
	my $fields = "player1,player2,player3,player4,0,0,variation,"
				. "score1,score2,score3,score4,0,0,"
				. "time1,time2,time3,time4,0,0,"
				. "gamename,mode,tournament,gmtdate+0 as da,"
				. "players.player_name,players2.player_name,players3.player_name,players4.player_name,'',''";

	$bselect = "SELECT $fields FROM mp_gamerecord "
		. " left join players on players.uid=mp_gamerecord.player1 "
		. " left join players as players2 on players2.uid=mp_gamerecord.player2 " 
		. " left join players as players3 on players3.uid=mp_gamerecord.player3 "
		. " left join players as players4 on players4.uid=mp_gamerecord.player4 "
		;
	}
	else
	{
	my $fields = "player1,player2,0,0,0,0,variation,"
				. "winner,0,0,0,0,0,"
				. "time1,time2,0,0,0,0,"
				. "gamename,mode,tournament,gmtdate+0 as da,"
				. "players.player_name,players2.player_name,'','','',''";
	$bselect = "SELECT $fields FROM zertz_gamerecord "
		. " left join players on players.uid=zertz_gamerecord.player1 "
		. " left join players as players2 on players2.uid=zertz_gamerecord.player2 " ;
	}

	my $and = "WHERE";
	if($game) { $bselect .= "$and variation='$game' "; $and=" AND "; }
	my $query = "($bselect";
	if($subquery)
	{
		$query .= $and . $subquery ;
		$and = " AND ";
	}
	if ($uid1>0)
	{ $query .= $and . &playerclause($multiplayer,$uid1);
	  $and = " AND "
	}
	if ($uid2>0)
	{ $query .= $and . &playerclause($multiplayer,$uid2);
	  $and = " AND "
	}
	if ($uid3>0)
	{ $query .= $and . &playerclause($multiplayer,$uid3);
	  $and = " AND "
	}
	if ($uid4>0)
	{ $query .= $and . &playerclause($multiplayer,$uid4);
	  $and = " AND "
	}
	if ($uid5>0)
	{ $query .= $and . &playerclause($multiplayer,$uid5);
	  $and = " AND "
	}
	if ($uid6>0)
	{ $query .= $and . &playerclause($multiplayer,$uid6);
	  $and = " AND "
	}


	{ $query = $query . " LIMIT 500)";
	}

	$query = $query . " ORDER by da desc";
	my $sth =&query($dbh,$query );
	#print "<P>Q: $query\n";

	my $won = 0; my $lost = 0; my $tied = 0;
	my $twon = 0; my $tlost = 0; my $ttied = 0;
	my $uwon = 0; my $ulost = 0; my $utied = 0;
	my $tbwon = 0; my $tblost = 0; my $tbtied = 0;
	my $mwon = 0; my $mlost = 0; my $mtied = 0;
	my $tott1 = 0;  my $tott2 = 0;  my $tott3 = 0;  my $tott4 = 0; my $tott5 = 0;  my $tott6 = 0;
	my $mtott1 = 0; my $mtott2 = 0;my $mtott3 = 0; my $mtott4 = 0;my $mtott5 = 0; my $mtott6 = 0;
	my $utott1 = 0; my $utott2 = 0;my $utott3 = 0; my $utott4 = 0;my $utott5 = 0; my $utott6 = 0;
	my $ttott1 = 0; my $ttott2 = 0;my $ttott3 = 0; my $ttott4 = 0;my $ttott5 = 0; my $ttott6 = 0;
	my $numr = &numRows($sth);
	my $games = $numr; my $tgames = 0; my $mgames = 0; my $ugames = 0;
	my @table; my $row; my $numrows = 0;
	my (%wins, %loss, %ties, %games, %times, %opptimes, %percentages, %losspercentages,%names);
	$wins{$robtot} = 0;
	$loss{$robtot} = 0;
	$ties{$robtot} = 0;

	while ($numr>0 )
	{	$numr--;
		my ($p1,$p2,$p3,$p4,$p5,$p6,$variation,
			$score1,$score2,$score3,$score4,$score5,$score6,
			$time1,$time2,$time3,$time4,$time5,$time6,
			$gamename,$mode,$tournament,$date,
			$p1name,$p2name,$p3name,$p4name,$p5name,$p6name)
		 = nextArrayRow($sth);

		if($p1name) { $names{$p1} = $p1name; }
		if($p2name) { $names{$p2} = $p2name; }
		if($p3name) { $names{$p3} = $p3name; }
		if($p4name) { $names{$p4} = $p4name; }
		if($p5name) { $names{$p5} = $p5name; }
		if($p6name) { $names{$p6} = $p6name; }

		my $winnermoving = $score1;
		my $winner = ($winnermoving eq 'draw') ? 'draw' : 'player1';
		
		# normalize player1 (the player who was named) as the winner
		if ( ($p1 == $uid2) || ($p2 == $uid1) ) 
		{
			my $temp = $time2; $time2=$time1; $time1=$temp;
			$temp = $p2; $p2=$p1; $p1=$p2;
			$temp = $p1name;
			if($winner eq 'player1') { $winner = "player2"; }
			$p1name = $p2name;
			$p2name = $temp;
		}
		if ( $numrows < 50 || ($fulltable eq 'on' &&  $numrows < 500) ) {
		  $numrows++;
		  push @table, [$p1, $p2 , $p3, $p4, $p5, $p6,
		                $time1, $time2, $time3, $time4, $time5, $time6,
						$score1,$score2,$score3,$score4,$score5,$score6,
						$winner,$winnermoving,$gamename, $mode,$tournament,$date,
						$p1name,$p2name,$p3name,$p4name,$p5name, $p6name];
		}

		if ( $uid1 || $uid2 || $uid3 || $uid4 || $uid5 || $uid6) 
		{
			$tott1+= $time1;
			$tott2+= $time2;
			$tott3+= $time3;
			$tott4+= $time4;
			$tott5+= $time5;
			$tott6+= $time6;

			#
			# interpretation of the zertz_gamerecord "winner" field is as follows.
			# the actual winner of the game is always player1
			# the winner field tells us if the winner (player1) moved first or second
			#
			if ( $mode eq 'master' ) 
			{
				$mgames++;
				$mtott1+= $time1;
				$mtott2+= $time2;
				$mtott3+= $time3;
				$mtott4+= $time4;
				$mtott5+= $time5;
				$mtott6+= $time6;

				if($winner eq 'player1') { $mwon++; }
				elsif($winner eq 'player2' ) { $mlost++; }
				else { $mtied++; }
			}
			if ( $mode eq 'unranked' ) {
				$ugames++;
				$utott1+= $time1;
				$utott2+= $time2;
				$utott3+= $time3;
				$utott4+= $time4;
				$utott5+= $time5;
				$utott6+= $time6;
				if($winner eq 'player1') { $uwon++; }
				elsif($winner eq 'player2') { $ulost++; }
				else { $mtied++; }
			}
			if ( $tournament eq 'yes' ) 
			{
				$tgames++;
				$ttott1+= $time1;
				$ttott2+= $time2;
				$ttott3+= $time3;
				$ttott4+= $time4;
				$ttott5+= $time5;
				$ttott6+= $time6;
				if($winner eq 'player1') { $twon++; }
				elsif($winner eq 'player2') { $tlost++; }
				else { $ttied++; }
			}
			if($winner eq 'player1') { $won++; }
			elsif($winner eq 'player2') { $lost++; }
			else { $tied++; }
		}
		if(!$uid1 && !$uid2)
		{	# no user specified
		$games{$p2}++;
		$games{$p1}++;
		$names{$p1}=$p1name;
		$names{$p2}=$p2name;
		$times{$p1} += $time1;
		$times{$p2} += $time2;
		$opptimes{$p1} += $time2;
		$opptimes{$p2} += $time1;
		if($winner eq 'draw') { $ties{$p1}++; $ties{$p2}++; $wins{$p1}+=0; $loss{$p1}+=0; $wins{$p2}+=0; $loss{$p2}+=0; }
		else { $loss{$p1}+=0; $wins{$p1}++; $wins{$p2}+=0; $loss{$p2}++; $ties{$p1}+=0; $ties{$p2}+=0;}
		}
		elsif ( !$uid1 || !$uid2 ) 
		{	# one user specified
			if ( !$games{$p2} ) 
			{
				$wins{$p2}=0;
				$ties{$p2}=0; 
				$loss{$p2}=0;
			}
			$games{$p2} += 1;
			$names{$p2} = $p2name;
			$times{$p2}+= $time1;
			$opptimes{$p2}+= $time2;
			if($winner eq 'player1') { $wins{$p2}++; }
			elsif($winner eq 'player2') { $loss{$p2}++; }
			else { $ties{$p2}++; }
		}

#   throw all robot games together
			if ( $p2 == $robot || $p2 == $robot2 || $p2 == $robot3 ) 
			{
				my $p = $robtot;
				$games{$p} += 1;
				$times{$p}+= $time1;
				$opptimes{$p}+= $time2;
				if($winner eq 'player1') { $wins{$p}++; }
				elsif ($winner eq 'player2') { $loss{$p}++; }
				else { $ties{$p}++ }
			}

		}
	&finishQuery($sth);
	if($uid1 || $uid2)
	{
	print "<P><TABLE BORDER=0 CELLPADDING=5 CELLSPACING=5 WIDTH=\"70%\">";
 	my $opponent_tp;
	my $percentage; my $opp_percentage; my $tie_percentage;
	print "<TR><TD align=left WIDTH=\"30%\"><b></b></TD>";
	print "<TD WIDTH=\"15%\"></TD><TD WIDTH=\"20%\"></TD></TR>\n";
	print "<TR><TD align=left WIDTH=\"30%\"><b></b></TD>"
                . "<TD WIDTH=\"15%\">"
		. &trans("#1 Win",$player1)
		. "</TD>"
		. "<TD WIDTH=\"15%\">"
		. &trans("#1 Loss",$player1)
		. "</TD>"
		. "<TD WIDTH=\"15%\">"
		. &trans("Tied")
		. "</TD><TD WIDTH=\"20%\">"
		. &trans("Games Played")
		. "</TD>"
		. "<td>"
		. &trans("#1 Avg Time",$player1)
		. "</td><td>"
		. &trans("Opponent Avg Time")
		. "</td></TR>\n";
	print_summary("All Games", $games,$won,$lost,$tied,$tott1,$tott2) if ( $games );
	print_summary("Robot Games", $games{$robtot},$wins{$robtot},$loss{$robtot},$ties{$robtot},$times{$robtot},$opptimes{$robtot}) if($games{$robtot});
	print_summary("Unranked Games", $ugames,$uwon,$ulost,$utied,$utott1,$utott2) if ( $ugames );
	#print_summary("Master Games", $mgames,$mwon,$mlost,$mtied,$mtott1,$mtott2) if ( $mgames );
	print_summary("Tournament Games", $tgames,$twon,$tlost,$ttied,$ttott1,$ttott2) if ( $tgames );
	
    print "</b></TABLE>\n";
	&separator();  # underline/separator
	}
	
	if ( !$uid2 ) 
	{
		print "<TABLE BORDER=0 CELLPADDING=5 CELLSPACING=5 WIDTH=\"70%\">";
		if($uid1 ) 
		{ print 
		   "<CAPTION><A HREF=\"edit.cgi?pname=$player1\"><b>$player1</b></A><span class=\"sml\">\'s results against</span></CAPTION>"; 
		}
		print "<TR><TD>&nbsp;</TD></TR>\n";
		print "<TR><TD align=left WIDTH=\"30%\"><b></b></TD>"
       	    . "<TD WIDTH=\"15%\">"
			. &trans("#1 Win",$player1)
			. "</TD>"
			. "<TD WIDTH=\"15%\">"
			. &trans("#1 Loss",$player1)
			. "</TD>"
			. "<TD WIDTH=\"15%\">"
			. &trans("Tie")
			. "</TD><TD WIDTH=\"20%\">"
			. &trans("Games Played")
			. "</TD>"
			. "<td>"
			. &trans("#1 Avg Time",$player1)
			. "</td><td>"
			. &trans("Opponent Avg Time")
			. "</td></TR>\n";
		foreach my $key ( keys %games  ) {
		   $percentages{$key} = sprintf '%1.1f',100*$wins{$key}/($games{$key});
		   $losspercentages{$key} = sprintf '%1.1f',100*$loss{$key}/($games{$key});
		}
		my @sorted;
		if ( $sorting eq 'gamesdown' || $sorting eq '') {
			@sorted = sort { $games{$b} <=> $games{$a} } keys(%games);
		} elsif ( $sorting eq 'gamesup' ) {
                        @sorted = sort { $games{$a} <=> $games{$b} } keys(%games);
                } elsif ( $sorting eq 'win' ) {
                        @sorted = sort { $percentages{$b} <=> $percentages{$a} } keys(%games);
                } else {
                        @sorted = sort { $losspercentages{$b} <=> $losspercentages{$a} } keys(%games);
                }
		my $num=0;
		foreach my $key ( @sorted  ) {
		   my $qname = $dbh->quote($key);
		   next if ($key == $robtot );     # skip the thrown together robots
		   next if ($games{$key} < $delim );
		   $player2 = $names{$key};
		   next if ( ! $player2 );  # skip players that have stopped, i.e. empty name
		   $num++;
		   my $ep1 = uri_escape($player1);
		   my $ep2 = uri_escape($player2);
		   my $evar = uri_escape($game);
		   print_summary("$num: <A HREF=\"$ENV{'SCRIPT_NAME'}?player1=$ep1&player2=$ep2&game=$evar\">$player2",
		   $games{$key},$wins{$key},$loss{$key},$ties{$key},$times{$key},$opptimes{$key});
		}
		print "</b></TABLE>\n";
	&separator();  # underline/separator
	}

	print "<p>\n";

	print "<TABLE BORDER=0 CELLPADDING=2 CELLSPACING=4 WIDTH=\"100%\">";
	my $str1 = ( $uid1 ? $player1 : "Opponent" );
	my $str2 = ( $uid2 ? $player2 : "Opponent" );
	print "<TR>"
		. "<TD align=left><b>Game-saved line</b></TD>"
		. "<TD align=left><b>Winner</b></TD>"
    	. "<TD align=center><b>Date</b></TD>"
		. "<TD align=center><b>$str1</b></TD>"
		. "<TD align=center><b>$str2</b></TD>"
 		. "<TD align=center><b>Mode</b></TD>"
    	. "<TD align=center><b>Tournament</b></TD>"
		. "<TR>";

	for $row ( @table) {
		my ($p1, $p2 , $p3, $p4, $p5, $p6,
			$time1, $time2, $time3, $time4, $time5, $time6,
		    $score1, $score2, $score3, $score4, $score5, $score6,
			$winner, $winnermoving, $gamename, $mode,$tournament,$date,
			$p1name,$p2name,$p3name,$p4name,$p5name,$p6name) = @$row;
		my $str1 = timestr($time1);
		my $str2 = timestr($time2);
		my $datestr = datestr($date);
		my $winstr = ($winner eq 'draw')
			? "draw" 
			: (($winner eq 'player1') ? $p1name : $p2name);
		if(!($winnermoving eq 'draw'))
			{ $winstr = $winstr . " moving " . (($winnermoving eq 'player1') ? "first" : "second");
			}
		print "<TR>"
 			. "<TD align=left><b>$gamename</b></TD>"
			. "<TD align=left><b>$winstr</b></TD>"
    		. "<TD align=center><b>$datestr</b></TD>"
	   		. "<TD align=center><b>$str1</b></TD>"
			. "<TD align=center><b>$str2</b></TD>"
			. "<TD align=center><b>$mode</b></TD>"
    		. "<TD align=center><b>$tournament</b></TD>"
			. "</TR>\n";
		}
	print "</TABLE><br>\n";


#	&finishQuery($sth);
  }
  &disconnect($dbh);

  &standard_footer();
}}

print header;
print "<HTML>";
&compare_players();


