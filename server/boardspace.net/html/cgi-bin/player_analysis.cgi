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
require "tlib/timestamp.pl";

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
  my $norobot = param('norobot');
  my $tg = param('tg');
  my $str1 = "";
  my $str2 = "";
  my $str3 = "";
  my $str4 = "";
  my $str5 = "";
  my $str6 = "";
  my $selgamecode = &gamename_to_gamecode($dbh,$game);
  my $ghead = &trans("Boardspace #1 Games Analysis",$game);
  print "<head><title>$ghead</title></head>\n";
  &standard_header();
  print "<form action=$ENV{'SCRIPT_NAME'} method=post>\n";
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
  print "<input type=hidden name=submitted value=true>\n";
  &print_timestamp();
  print &trans("Select Game")
	. ": <select name=game><option value=''>"
	. &trans("All Games") 
	. "</option>";
  &print_game_selector($dbh,$game);
  print "</select><br>";
  {
  # print a link to the rankings for the selected game
  my $trank = &trans("rankings for #1",$game);
  print "<a href=\"javascript:link('boardspace_rankings.cgi?game=$selgamecode')\">$trank</a>\n";
  }
  print "<p><table width=50%><tr>";
  {my $idx = 1;
   print "<tr>";
   while($idx<=$maxplayers)
   { my $pn = &trans("Player #1",$idx);
     print "<TD>$pn</TD>\n";
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
  my $sd = &trans("Start date");
  my $ed = &trans("End Date");
  print "<TR><TD>$sd</TD><TD>$ed</TD></TR>\n";
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
  print "<br>";
  print "<input type=checkbox name=norobot ";
  print " checked " if ($norobot);
  print ">" . &trans("Exclude robot games") . "\n";
  print "<br>" . &trans("Display only opponents against who at least #1 games were played.",
		"<input type=text size=\"1\" name=delim value=$delim>")
   . "</form><br>\n";
 
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
   
sub get_uid_and_dates
{ my ($dbh,$name) = @_;
  my ($uid,$first,$last);
 
  if($name)
  {  my $qname = $dbh->quote($name);
     my $sth = &query($dbh,"SELECT uid,date_joined,if(is_robot='y',UNIX_TIMESTAMP(CURRENT_TIMESTAMP),last_logon) from players where player_name=$qname");
     my $nr = &numRows($sth);
     if($nr>0)
     {
     ($uid,$first,$last) = $sth->fetchrow();
     }
     &finishQuery($sth);
  }
  return($uid,$first,$last);
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
   if($games eq "") { $games = "0"; }
   if($won eq "") {$won = "0"; }
   if($lost eq "") { $lost = "0"; }
   if($tied eq "") { $tied = "0"; }

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

sub average_opponent_time()
{
  my ($index,$p1,$p2,$p3,$p4,$p5,$p6,
             $t1,$t2,$t3,$t4,$t5,$t6) = @_;
  my $no=0;
  my $tt=0;
  if (!($index eq 1) && !($p1 eq 0)) { $no++; $tt+=$t1; }
  if (!($index eq 2) && !($p2 eq 0)) { $no++; $tt+=$t2; }
  if (!($index eq 3) && !($p3 eq 0)) { $no++; $tt+=$t3; }
  if (!($index eq 4) && !($p4 eq 0)) { $no++; $tt+=$t4; }
  if (!($index eq 5) && !($p5 eq 0)) { $no++; $tt+=$t5; }
  if (!($index eq 6) && !($p6 eq 0)) { $no++; $tt+=$t6; }
  if($no>0) { return($tt/$no); }
  return(0);
}

# score as winner only if we're the best, not tied
sub winner_among()
{
  my ($p1,$p2,$p3,$p4,$p5,$p6,
      $t1,$t2,$t3,$t4,$t5,$t6) = @_;
  my $best = 0;
  my $second = 0;
  my $bests = -1;
  my $seconds = -1;
  if ($p1 > 0)
    { if($t1>$bests) { $second = $best; $seconds = $bests; $best=$p1; $bests=$t1; }
      elsif ($t1>$seconds) { $seconds=$t1; $second=$p1; }
    }
  if ($p2 > 0)
    { if($t2>$bests) { $second = $best; $seconds = $bests; $best=$p2; $bests=$t2; }
      elsif ($t2>$seconds) { $seconds=$t2; $second=$p2; }
    }
  if ($p3 > 0)
    { if($t3>$bests) { $second = $best; $seconds = $bests; $best=$p3; $bests=$t3; }
      elsif ($t3>$seconds) { $seconds=$t3; $second=$p3; }
    }
  if ($p4 > 0)
    { if($t4>$bests) { $second = $best; $seconds = $bests; $best=$p4; $bests=$t4; }
      elsif ($t4>$seconds) { $seconds=$t4; $second=$p4; }
    }
  if ($p5 > 0)
    { if($t5>$bests) { $second = $best; $seconds = $bests; $best=$p5; $bests=$t5; }
      elsif ($t5>$seconds) { $seconds=$t5; $second=$p5; }
    }
  if ($p6 > 0)
    { if($t6>$bests) { $second = $best; $seconds = $bests; $best=$p6; $bests=$t6; }
      elsif ($t6>$seconds) { $seconds=$t6; $second=$p6; }
    }
  if($bests > $seconds) { return($best); }
  return(0);
}

# score as draw only if we're tied for the best, not tied
sub draw_among()
{
  my ($p1,$p2,$p3,$p4,$p5,$p6,
      $t1,$t2,$t3,$t4,$t5,$t6) = @_;
  my $best = 0;
  my $second = 0;
  my $bests = -1;
  my $seconds = -1;

  if ($p1 > 0)
    { if($t1>$bests) { $second = $best; $seconds = $bests; $best=$p1; $bests=$t1; }
      elsif ($t1>$seconds) { $seconds=$t1; $second=$p1; }
    }
  if ($p2 > 0)
    { if($t2>$bests) { $second = $best; $seconds = $bests; $best=$p2; $bests=$t2; }
      elsif ($t2>$seconds) { $seconds=$t2; $second=$p2; }
    }
  if ($p3 > 0)
    { if($t3>$bests) { $second = $best; $seconds = $bests; $best=$p3; $bests=$t3; }
      elsif ($t3>$seconds) { $seconds=$t3; $second=$p3; }
    }
  if ($p4 > 0)
    { if($t4>$bests) { $second = $best; $seconds = $bests; $best=$p4; $bests=$t4; }
      elsif ($t4>$seconds) { $seconds=$t4; $second=$p4; }
    }
  if ($p5 > 0)
    { if($t5>$bests) { $second = $best; $seconds = $bests; $best=$p5; $bests=$t5; }
      elsif ($t5>$seconds) { $seconds=$t5; $second=$p5; }
    }
  if ($p6 > 0)
    { if($t6>$bests) { $second = $best; $seconds = $bests; $best=$p6; $bests=$t6; }
      elsif ($t6>$seconds) { $seconds=$t6; $second=$p6; }
    }
  if($bests eq $seconds) 
   { return($best,$second); 
   }
}

# score as loser only if we're the worst, not tied
sub loser_among()
{
  my ($p1,$p2,$p3,$p4,$p5,$p6,
      $t1,$t2,$t3,$t4,$t5,$t6) = @_;
  my $best = 0;
  my $second = 0;
  my $bests = 999999;
  my $seconds = 999999;

  if ($p1 > 0)
    { if($t1<$bests) { $second = $best; $seconds = $bests; $best=$p1; $bests=$t1; }
      elsif ($t1<$seconds) { $seconds=$t1; $second=$p1; }
    }
  if ($p2 > 0)
    { if($t2<$bests) { $second = $best; $seconds = $bests; $best=$p2; $bests=$t2; }
      elsif ($t2<$seconds) { $seconds=$t2; $second=$p2; }
    }
  if ($p3 > 0)
    { if($t3<$bests) { $second = $best; $seconds = $bests; $best=$p3; $bests=$t3; }
      elsif ($t3<$seconds) { $seconds=$t3; $second=$p3; }
    }
  if ($p4 > 0)
    { if($t4>$bests) { $second = $best; $seconds = $bests; $best=$p4; $bests=$t4; }
      elsif ($t4<$seconds) { $seconds=$t4; $second=$p4; }
    }
  if ($p5 > 0)
    { if($t5>$bests) { $second = $best; $seconds = $bests; $best=$p5; $bests=$t5; }
      elsif ($t5<$seconds) { $seconds=$t5; $second=$p5; }
    }
  if ($p6 > 0)
    { if($t6<$bests) { $second = $best; $seconds = $bests; $best=$p6; $bests=$t6; }
      elsif ($t6<$seconds) { $seconds=$t6; $second=$p6; }
    }
  if($bests < $seconds) { return($best); }
  return(0);
}
# score as draw only if we're tied for the best, not tied
sub position_among()
{
  my ($index,$p1,$p2,$p3,$p4,$p5,$p6,
      $t1,$t2,$t3,$t4,$t5,$t6) = @_;
  my $greater = 0;
  my $lesser = 0;
  my $same = 0;

  if($p1>0) {if($t1 > $index) { $greater++; } elsif($t1<$index) { $lesser++; } else {$same++};}
  if($p2>0) {if($t2 > $index) { $greater++; } elsif($t2<$index) { $lesser++; } else {$same++};}
  if($p3>0) {if($t3 > $index) { $greater++; } elsif($t3<$index) { $lesser++; } else {$same++};}
  if($p4>0) {if($t4 > $index) { $greater++; } elsif($t4<$index) { $lesser++; } else {$same++};}
  if($p5>0) {if($t5 > $index) { $greater++; } elsif($t5<$index) { $lesser++; } else {$same++};}
  if($p5>0) {if($t6 > $index) { $greater++; } elsif($t6<$index) { $lesser++; } else {$same++};}
  return(($greater+$same/2)/($greater+$same+$lesser));
}


sub compare_players 
{ my $game = param('game');	

  my $robtot = -1;
  my $start_date=param('start_date');
  my $end_date=param('end_date');
  my $fulltable=param('fulltable');
  my $norobot = param('norobot');
  my $sorting=param('sort');
  my $delim=param('delim');
  my $master=param('master');
  my $tg=param('tg');
  my $allgames = ($game eq '');
  my $limit = 0+param('limit');
  my $sub = param('submitted');
  my $stamp = param('timestamp');

  &bless_parameter_length($start_date,20);	#assess for penalty
  &bless_parameter_length($end_date,20);	#assess for penalty
  &bless_parameter_length($sorting,20);	#assess for penalty
  &bless_parameter_length($allgames,20);	#assess for penalty
  &bless_parameter_length($master,10);	#assess for penalty
  &bless_parameter_length($limit,20);	#assess for penalty
  
  if($limit<=0) { $limit = 500; }
  if(!$sub || &check_timestamp($stamp))
  {
  my $dbh = &connect();              # connect to local mysqld

  if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {	 

	&readtrans_db($dbh);
	{
    my $t = &current_time_string();
	my ($maxplayers) = &getGameInfo($dbh,$game);
	my $multiplayer = ($maxplayers > 2);
	my @players;				# player names used in the form
	my @uids;					# uids for players used in the form
	my $sunrise_date = 	0;		# player registration date
	my $sunset_date = 0;		# player last login date

	#
	# collect player names and uids from the form
	#
	{
	my %playernames;			# player names index by parameter name
	my %playeruids;				# player uids index by parameter name
	&honeypot();
	for(my $i=1; $i<=6; $i++)
	{
	my $pn = "player$i";
    my $pla = param($pn);

   	&bless_parameter_length($pla,20);	#assess for penalty

	my $pl = &despace(substr($pla,0,25));
	if($pl)
	{
	  my ($uu,$ff,$ll) = &get_uid_and_dates($dbh,$pl);
	  $playernames{$pn} = $pl;
	  $playeruids{$pn} = $uu;
	  if($ff > 0 && (($sunrise_date==0) || ($ff>$sunrise_date)) ) { $sunrise_date = $ff; }
	  if($ll > 0 && (($sunset_date==0) || ($ll<$sunset_date))) { $sunset_date = $ll; };
 
	  if($uu) 
		{ push(@players,$pl); 
		  push(@uids,$uu);
		}
	}}
	
	# print the header for the next round, including the player names
	&comparison_header($dbh,$game,$maxplayers,
				$playernames{'player1'},
				$playernames{'player2'},
				$playernames{'player3'},
				$playernames{'player4'},
				$playernames{'player5'},
				$playernames{'player6'},

				$playeruids{'player1'},
				$playeruids{'player2'},
				$playeruids{'player3'},
				$playeruids{'player4'},
				$playeruids{'player5'},
				$playeruids{'player6'},

				$start_date,$end_date);
	}
	#
	if($sub eq 'true')
    {  

	#
	# the general strategy is to extend the query to 6 players, and specifically
	# construct it for 2 zertz-gamerecord or 4 mp_gamerecord.  The two extra are 
	# forseeable expansions of mp_gamerecord.
	#
	my $query = "";

	my $bselect = "";	# generic conditions for both selects

	{
	#build the generic "where" clause
	my $and = "WHERE";
	if($game) { $bselect .= "$and variation='$game' "; $and=" AND "; }

	for(my $i=0;$i<=$#uids;$i++)
	{
	 $bselect.= $and . &playerclause($multiplayer,$uids[$i]);
	 $and = " AND ";
	}

	{
	my $start_str = convert_date($start_date);
	if ( length($start_str) == 14 ) 
	{
	  $bselect .= " $and gmtdate >= $start_str ";
	  $and = " AND ";
	}}
	{
	my $end_str = convert_date($end_date);
	if ( length($end_str) == 14 )
	{
	  $bselect .= " $and gmtdate <= $end_str ";
	  $and = " AND ";
	}}
	if($sunrise_date>0)
	{	$bselect .= $and . "gmtdate >= FROM_UNIXTIME($sunrise_date)";
		$and = " AND ";
	}
	if($sunset_date>0)
	{	$bselect .= $and . "gmtdate <= DATE_ADD(FROM_UNIXTIME($sunset_date),interval 1 day)";
		$and = " AND ";
	}


	if($master)
	{ $bselect .= " $and mode = 'MASTER' ";
	  $and = " AND ";
	}
	if($tg)
	{
	  $bselect .= "$and tournament = 'yes' " ;
	  $and = " AND ";
	}
	}


	if($multiplayer || $allgames)
	{
	my $mpfields = "player1,player2,player3,player4,player5,player6,variation,"
				. "score1,score2,score3,score4,score5,score6,"
				. "time1,time2,time3,time4,time5,time6,"
				. "gamename,mode,tournament,gmtdate+0 as da,"
				. "players.player_name,players2.player_name,players3.player_name,"
				. "players4.player_name,players5.player_name,players6.player_name,"
				. "players.is_robot,players2.is_robot,players3.is_robot,"
				. "players4.is_robot,players5.is_robot,players6.is_robot"
				. ",0"
				;

	my $mpselect = "SELECT $mpfields FROM mp_gamerecord "
		. " left join players on players.uid=mp_gamerecord.player1 "
		. " left join players as players2 on players2.uid=mp_gamerecord.player2 " 
		. " left join players as players3 on players3.uid=mp_gamerecord.player3 "
		. " left join players as players4 on players4.uid=mp_gamerecord.player4 "
		. " left join players as players5 on players5.uid=mp_gamerecord.player5 "
		. " left join players as players6 on players6.uid=mp_gamerecord.player6 "
		;
	$query .= "($mpselect $bselect ORDER by gmtdate desc LIMIT $limit  )";
	}

	if(!$multiplayer || $allgames)
	{
	my $union = $allgames ? " UNION " : "";
	my $tpfields = "player1,player2,0,0,0,0,variation,"
				. "winner,0,0,0,0,0,"
				. "time1,time2,0,0,0,0,"
				. "gamename,mode,tournament,gmtdate+0 as da,"
				. "players.player_name,players2.player_name,'','','','',"
				. "players.is_robot,players2.is_robot,'','','','',1"
				;
	my $tpselect = "SELECT $tpfields FROM zertz_gamerecord "
		. " left join players on players.uid=zertz_gamerecord.player1 "
		. " left join players as players2 on players2.uid=zertz_gamerecord.player2 " ;
	my $lim = $union ? " ORDER by da desc LIMIT $limit" : "";
	$query .= "$union ($tpselect $bselect ORDER by gmtdate desc LIMIT $limit ) $lim ";
	}

	#print "<P>Q: $query\n<p>";

	my $sth =&query($dbh,$query );
  my %games;    # games played index by UID
  my %tottime;  # player total time index by UID
  my %opptime;  # average opponent time index by UID
  my %wins;     # wins index by UID
  my %loss;     # losses index by UID
  my %ties;     # ties index by UID
  my %position; # finish position index by UID
  my %percentages;  # win percentages index by UID
  my %losspercentages;  #loss percentages undex by UID
  
  my %mgames;    # games played index by UID
  my %mtottime; # master game total time index by UID
  my %mopptime; # master game opponent times, index by UID
  my %mwins;     # wins index by UID
  my %mloss;     # losses index by UID
  my %mties;     # ties index by UID
  my %mposition; # finish position index by UID

  my %ugames;    # games played index by UID
  my %utottime; # unranked game total time undex by UID
  my %uopptime; # unranked game opponent time, index by UID
  my %uwins;     # wins index by UID
  my %uloss;     # losses index by UID
  my %uties;     # ties index by UID
  my %uposition; # finish position index by UID

  my %tgames;    # games played index by UID
  my %ttottime; # tournament game total time index by UID
  my %topptime; # tournament opponent total time, index by UID
  my %twins;     # wins index by UID
  my %tloss;     # losses index by UID
  my %tties;     # ties index by UID
  my %tposition; # finish position index by UID

  my %names;    # player names index by UID
  my %robots;	# player robot status index by UID

	my $numr = &numRows($sth);

	if($numr >= $limit) 
	{ my $lim = &trans("These results are limited to a subset of #1 games",$limit);
	  print "<b>$lim<b><br>";
	}
  my $allgames = $numr;
  my $alltgames = 0;
  my $allmgames = 0;
  my $allugames = 0;
  
  $names{-1} = "All Robots";
  $robots{-1} = 'y';

	my @table; # data passed through to the individual game report section
  my $row;
  my $numrows = 0;
  my $maxplayerresults = 2;
  $wins{$robtot} = 0;
	$loss{$robtot} = 0;
	$ties{$robtot} = 0;

	while ($numr>0 )
	{	$numr--;
		my ($p1,$p2,$p3,$p4,$p5,$p6,$variation,
			$score1,$score2,$score3,$score4,$score5,$score6,
			$time1,$time2,$time3,$time4,$time5,$time6,
			$gamename,$mode,$tournament,$date,
			$p1name,$p2name,$p3name,$p4name,$p5name,$p6name,
			$p1rob,$p2rob,$p3rob,$p4rob,$p5rob,$p6rob,$is2p)
		 = nextArrayRow($sth);
   
 		if($p1name) { $names{$p1} = $p1name; }
		if($p2name) { $names{$p2} = $p2name; }
		if($p3name) { $names{$p3} = $p3name; if(3>$maxplayerresults) { $maxplayerresults=3; } }
		if($p4name) { $names{$p4} = $p4name; if(4>$maxplayerresults) { $maxplayerresults=4; }}
		if($p5name) { $names{$p5} = $p5name; if(5>$maxplayerresults) { $maxplayerresults=5; }}
		if($p6name) { $names{$p6} = $p6name; if(6>$maxplayerresults) { $maxplayerresults=6; }}
		my $robogame = 0;
		if($p1rob eq 'y') { $robots{$p1}='y' ; $robogame=1; }
		if($p2rob eq 'y') { $robots{$p2}='y' ; $robogame=1;  }
		if($p3rob eq 'y') { $robots{$p3}='y' ; $robogame=1; }
		if($p4rob eq 'y') { $robots{$p4}='y' ; $robogame=1; }
		if($p5rob eq 'y') { $robots{$p5}='y' ; $robogame=1;  }
		if($p6rob eq 'y') { $robots{$p6}='y' ; $robogame=1; }

    # in 2 player games from zertz_gamerecord, the score1 field is
    # actually the table's winner field
	my $winnermoving="";

    if($is2p)
    {
	 #
	 # in zertz_gamerecords, the winner is always the first of the two players listed.
	 # and the winner field we read as score1 indicates if he played first or second
	 #
	  #print "$p1 $names{$p1} $p2 $names{$p2} $score1<br>";

     if($score1 eq 'draw') { $score1 = 1; $score2 = 1; }
	 elsif($score1 eq 'player1') { $score1 = 1; $score2=0; $winnermoving=" moving first"; }
     else { $score1 = 1; $score2 = 0; $winnermoving = " moving second"; }
	 if($#uids>=0)
	 {	if(!($uids[0] eq $p1))
			{
			 # swap the times to match the expected names
			 my $tt = $time1;
			 my $pla = $p1;
			 my $s1 = $score1;
			 $score1 = $score2;
			 $score2 = $s1;
			 $p1 = $p2;
			 $p2 = $pla;
			 $time1 = $time2;
			 $time2 = $tt;
			 
			}
	 }
    }
    my $useThisGame = ($norobot ? !$robogame : 1);

    if($useThisGame)
    {
    my $ww = &winner_among($p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    my $ll = &loser_among( $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    my ($tt1,$tt2) = &draw_among($p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    $wins{$ww}++;
    $loss{$ll}++;
    # count ties for both players
    if($tt1) { $ties{$tt1}++; }
    if($tt2) { $ties{$tt2}++; }
    
    $position{$p1}+= &position_among($score1, $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    $position{$p2}+= &position_among($score2, $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    $position{$p3}+= &position_among($score3, $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    $position{$p4}+= &position_among($score4, $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    $position{$p5}+= &position_among($score5, $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    $position{$p6}+= &position_among($score6, $p1,$p2,$p3,$p4,$p5,$p6,$score1,$score2,$score3,$score4,$score5,$score6);
    
    # save a specific game row for later
    if ( $numrows < 50 || ($fulltable eq 'on' &&  $numrows < 500) )
     {
		  $numrows++;
		  push @table, [$p1, $p2 , $p3, $p4, $p5, $p6,
		                $time1, $time2, $time3, $time4, $time5, $time6,
						$score1,$score2,$score3,$score4,$score5,$score6,
						$ww,$ll,$tt1,$tt2,$winnermoving,$gamename, $mode,$tournament,$date,
						$p1name,$p2name,$p3name,$p4name,$p5name, $p6name];
		  }
    
      # accumulate time totals per uid
      $tottime{$p1} += $time1;
      $tottime{$p2} += $time2;
      $tottime{$p3} += $time3;
      $tottime{$p4} += $time4;
      $tottime{$p5} += $time5;
      $tottime{$p6} += $time6;
	
      my $ao1 = &average_opponent_time(1,$p1,$p2,$p3,$p4,$p5,$p6,
                        $time1,$time2,$time3,$time4,$time5,$time6);
      my $ao2 = &average_opponent_time(2,$p1,$p2,$p3,$p4,$p5,$p6,
                        $time1,$time2,$time3,$time4,$time5,$time6);
      my $ao3 = &average_opponent_time(3,$p1,$p2,$p3,$p4,$p5,$p6,
                        $time1,$time2,$time3,$time4,$time5,$time6);
      my $ao4 = &average_opponent_time(4,$p1,$p2,$p3,$p4,$p5,$p6,
                        $time1,$time2,$time3,$time4,$time5,$time6);
      my $ao5 = &average_opponent_time(5,$p1,$p2,$p3,$p4,$p5,$p6,
                        $time1,$time2,$time3,$time4,$time5,$time6);
      my $ao6 = &average_opponent_time(6,$p1,$p2,$p3,$p4,$p5,$p6,
                        $time1,$time2,$time3,$time4,$time5,$time6);
      $opptime{$p1} += $ao1;
      $opptime{$p2} += $ao2;
      $opptime{$p3} += $ao3;
      $opptime{$p4} += $ao4;
      $opptime{$p5} += $ao5;
      $opptime{$p6} += $ao6;

    $games{$p1}++;
    $games{$p2}++;
    $games{$p3}++;
    $games{$p4}++;
    $games{$p5}++;
    $games{$p6}++;


	if ( $mode eq 'master' )
			{
		$allmgames++;
        $mtottime{$p1} += $time1;
        $mtottime{$p2} += $time2;
        $mtottime{$p3} += $time3;
        $mtottime{$p4} += $time4;
        $mtottime{$p5} += $time5;
        $mtottime{$p6} += $time6;
        $mopptime{$p1} += $ao1;
        $mopptime{$p2} += $ao2;
        $mopptime{$p3} += $ao3;
        $mopptime{$p4} += $ao4;
        $mopptime{$p5} += $ao5;
        $mopptime{$p6} += $ao6;

        $mwins{$ww}++;
        $mloss{$ll}++;
        if($tt1) { $mties{$tt1}++; }
		if($tt2) { $mties{$tt2}++; }
      }
  	if ( $mode eq 'unranked' )
      {
		$allugames++;

        $utottime{$p1} += $time1;
        $utottime{$p2} += $time2;
        $utottime{$p3} += $time3;
        $utottime{$p4} += $time4;
        $utottime{$p5} += $time5;
        $utottime{$p6} += $time6;
        $uopptime{$p1} += $ao1;
        $uopptime{$p2} += $ao2;
        $uopptime{$p3} += $ao3;
        $uopptime{$p4} += $ao4;
        $uopptime{$p5} += $ao5;
        $uopptime{$p6} += $ao6;
        
        $uwins{$ww}++;
        $uloss{$ll}++;
        if($tt1) { $uties{$tt1}++; }
        if($tt2) { $uties{$tt2}++; }
			}

    if ( $tournament eq 'yes' )
			{
		$alltgames++;
        $ttottime{$p1} += $time1;
        $ttottime{$p2} += $time2;
        $ttottime{$p3} += $time3;
        $ttottime{$p4} += $time4;
        $ttottime{$p5} += $time5;
        $ttottime{$p6} += $time6;
        $topptime{$p1} += $ao1;
        $topptime{$p2} += $ao2;
        $topptime{$p3} += $ao3;
        $topptime{$p4} += $ao4;
        $topptime{$p5} += $ao5;
        $topptime{$p6} += $ao6;
	    $twins{$ww}++;
        $tloss{$ll}++;
        if($tt1) { $tties{$tt1}++; }
        if($tt2) { $tties{$tt2}++; }
			}
   } # end of processing rows
   }
  &finishQuery($sth);

{ # print the opponent list as a list of table column headers
  my $oppstr = "";
	for(my $i=0;$i<1 ; $i++)
	{ my $uu = ($i<=$#players) ? $players[$i] : "Opponent";
	  $uu = &trans("#1 Avg Time",$uu);
	  $oppstr .= "<TD align=center>$uu</TD>";
	}
  if($#uids==0) # one player named
  {
  for(my $idx = 0; $idx<=$#uids; $idx++)
  {
  my $player1 = $uids[$idx];
	print "<P><TABLE BORDER=0 CELLPADDING=5 CELLSPACING=5 WIDTH=\"70%\">";
 	my $opponent_tp;
	my $percentage; my $opp_percentage; my $tie_percentage;
	print "<TR><TD align=left WIDTH=\"30%\"><b></b></TD>";
	print "<TD WIDTH=\"15%\"></TD><TD WIDTH=\"20%\"></TD></TR>\n";
	print "<TR><TD align=left WIDTH=\"30%\"><b></b></TD>"
                . "<TD WIDTH=\"15%\">"
		. &trans("#1 Win",$names{$player1})
		. "</TD>"
		. "<TD WIDTH=\"15%\">"
		. &trans("#1 Loss",$names{$player1})
		. "</TD>"
		. "<TD WIDTH=\"15%\">"
		. &trans("Tied")
		. "</TD><TD WIDTH=\"20%\">"
		. &trans("Games Played")
		. "</TD>"
		. $oppstr
		. "<td>"
		. &trans("Opponent Avg Time")
		. "</td></TR>\n";
	print_summary("All Games", $games{$player1},$wins{$player1},$loss{$player1},$ties{$player1},$tottime{$player1},$opptime{$player1}) if ( $allgames>0 );
	#print_summary("Robot Games", $games{-1},$wins{-1},$loss{-1},$ties{-1},$tottime{-1},$opptime{-1}) if($games{-1});
	print_summary("Unranked Games", $allugames ,$uwins{$player1},$uloss{$player1},$uties{$player1},$utottime{$player1},$uopptime{$player1}) if ( $allugames>0 );
#print_summary("Master Games", $allmgames,$mwons{$player1},$mlosss{$player1},$mtieds{$player1},$mtottimes{$player1},$mopptimes{$player1} if ( $allmgames>0 );

	print_summary("Tournament Games", $alltgames,$twins{$player1},$tloss{$player1},$tties{$player1},$ttottime{$player1},$topptime{$player1}) if ( $alltgames>0 );
	
    print "</b></TABLE>\n";
	&separator();  # underline/separator
	}
}}

	#if ( $#uids eq 0 )	# exactly one player named
	{	my $uid1 = $uids[0];
		my $player1 = $players[0];

  my $oppstr = "";
	for(my $i=0;$i<2 ; $i++)
	{ my $uu = ($i<=$#players) ? $players[$i] : (($i eq 0) ? "" : "Opponent");
	  $uu = &trans("#1 Avg Time",$uu);
	  $oppstr .= "<TD align=center>$uu</TD>";
	}

		print "<TABLE BORDER=0 CELLPADDING=5 CELLSPACING=5 WIDTH=\"70%\">";
		if($uid1 ) 
		{ print 
		   "<CAPTION><A HREF=\"javascript:editlink('$player1')\"><b>$player1</b></A><span class=\"sml\">\'s results against</span></CAPTION>"; 
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
			. $oppstr
			. "</TR>\n";
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
		   next if ($key == $robtot );     # skip the thrown together robots
		   next if ($games{$key} < $delim );
		   next if ($key eq $uid1);
		   my $player2 = $names{$key};
		   next if ( ! $player2 );  # skip players that have stopped, i.e. empty name
		   $num++;
		   my $ep1 = uri_escape($player1);
		   my $ep2 = uri_escape($player2);
		   my $evar = uri_escape($game);
		   
		   if($#uids<0) 
		   {
		   #no players named
		   print_summary("$num: <A HREF=\"javascript:link('$ENV{'SCRIPT_NAME'}?player1=$ep1&player2=$ep2&game=$evar')\">$player2",
		  		   $games{$key},$wins{$key},$loss{$key},$ties{$key},$tottime{$key},$opptime{$key});
			}
			else
			{
		   # total times are nonsense
		   print_summary("$num: <A HREF=\"javascript:link('$ENV{'SCRIPT_NAME'}?player1=$ep1&player2=$ep2&game=$evar')\">$player2",
		   $games{$key},$loss{$key},$wins{$key},$ties{$key},$opptime{$key},$tottime{$key});
		   }
		}
		print "</b></TABLE>\n";
	&separator();  # underline/separator
	}

	print "<p>\n";
	my $vlink = &trans("#1 game archives",$game);
	my $selgamecode = &gamename_to_gamecode($dbh,$game);
	my $selgamelink = &gamecode_to_gameviewer($dbh,$selgamecode);
	print "<a href='$selgamelink'>$vlink</a><br>";
	print "<TABLE BORDER=0 CELLPADDING=2 CELLSPACING=4 WIDTH=\"100%\">";

	my $oppstr = "";
	for(my $i=1;$i<=$maxplayerresults ; $i++)
	{
	  $oppstr .= "<TD align=center><b>player $i Time</b></TD>";
	}

	print "<TR>"
		. "<TD align=left><b>Game-saved line</b></TD>"
		. "<TD align=left><b>Winner</b></TD>"
    	. "<TD align=center><b>Date</b></TD>"
		. $oppstr
 		. "<TD align=center><b>Mode</b></TD>"
    	. "<TD align=center><b>Tournament</b></TD>"
		. "<TR>";
	my %times;
	my %scores;

	for $row ( @table) {
		my ($p1, $p2 , $p3, $p4, $p5, $p6,
			$time1, $time2, $time3, $time4, $time5, $time6,
		    $score1, $score2, $score3, $score4, $score5, $score6,
			$winner, $loser, $tied1, $tied2, $winnermoving, $gamename, $mode,$tournament,$date,
			$p1name,$p2name,$p3name,$p4name,$p5name,$p6name) = @$row;
		$times{$p1} = timestr($time1);
		$times{$p2} = timestr($time2);
		$times{$p3} = timestr($time3);
		$times{$p4} = timestr($time4);
		$times{$p5} = timestr($time5);
		$times{$p6} = timestr($time6);
		$scores{$p1} = $score1;
		$scores{$p2} = $score2;
		$scores{$p3} = $score3;
		$scores{$p4} = $score4;
		$scores{$p5} = $score5;
		$scores{$p6} = $score6;

		my $timestr = "";
		my $datestr = datestr($date);
		my $winstr = ($tied1>0) ? "draw" : "$names{$winner}$winnermoving";
		my $cols = 0;
		if($p1) { my $ns = "$names{$p1} ";
				  $cols++;
		          $timestr .= "<TD align=right><b>$ns$times{$p1}</b></TD>" }
		if($p2) { my $ns = "$names{$p2} ";
				  $cols++;
				  $timestr .= "<TD align=right><b>$ns$times{$p2}</b></TD>" }
		if($p3) { my $ns = "$names{$p3} ";
				  $cols++;
				  $timestr .= "<TD align=right><b>$ns$times{$p3}</b></TD>" }
		if($p4) { my $ns = "$names{$p4} ";
				  $cols++;
				  $timestr .= "<TD align=right><b>$ns$times{$p4}</b></TD>" }
		if($p5) { my $ns = "$names{$p5} ";
				  $cols++;
				  $timestr .= "<TD align=right><b>$ns$times{$p5}</b></TD>" }
		if($p6) { my $ns = "$names{$p6} ";
				  $cols++;
				  $timestr .= "<TD align=right><b>$ns$times{$p6}</b></TD>" }

	    while ($cols<$maxplayerresults) { $timestr .= "<TD></TD>"; $cols++; }

		print "<TR>"
 			. "<TD align=left><b>$gamename</b></TD>"
			. "<TD align=left><b>$winstr</b></TD>"
    		. "<TD align=center><b>$datestr</b></TD>"
	   		. $timestr
			. "<TD align=center><b>$mode</b></TD>"
    		. "<TD align=center><b>$tournament</b></TD>"
			. "</TR>\n";
		}
	print "</TABLE><br>\n";


#	&finishQuery($sth);
  }}
  &disconnect($dbh);

  &standard_footer();
}}
}

print header;
print "<HTML>";
&compare_players();


