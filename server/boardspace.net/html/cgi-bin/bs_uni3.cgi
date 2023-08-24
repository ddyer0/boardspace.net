#!/usr/bin/perl
#
# record results of 2 player game, and also update the ladder
#

use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use IO::File;

require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/ranking.pl";
require "tlib/ladder.pl";
require "tlib/params.pl";

$| = 1;                         # force writes
	
__dStart( "$'debug_log",$ENV{'SCRIPT_NAME'});;
if( param() ) 
{
  $| = 1;                         # force writes
  my $ok = &useCombinedParams($'tea_key,1);
  if($ok)
  {
  print header;

  __d("from $ENV{'REMOTE_ADDR'} $ENV{'QUERY_STRING'}" );
  my $game0 = param('game');
  my $fname = param('fname');
  my $tourney = param('tournament') ? 'Yes' : 'No';
  my $digest_mid = param('dm');
  my $digest_end = param('de');
  my $master = (param('mm') eq 'true');

  if($game0)
 {
  my $dbh = &connect();              # connect to local mysqld
  if($dbh)
	{
  my $game = &gamecode_to_gamename($dbh,$game0);
  my $error = 0;
  my (@pname,@s,@nr,@u,@t,@c,@fch);
  my (@oldrank,@maxrank,@robo,@fixedrank,@new_ranking,@gameswon,@ladder_level,@ladder_order,@ladder_mentor);
  my $i;
  my $fsw = &param('fsw');	# follower state warning
  for($i=1;$i<=2;$i++)
	{
  $u[$i] = param("u$i");		# uid
  $t[$i] = param("t$i");		# time for player
	$s[$i] = param("s$i");    # 1 for winner, 0 for loser
	$fch[$i] = param("fch$i");# fraud check
	$nr[$i] = param("nr$i");  # unranked request
	if($u[$i] ne "") 
		{ ($pname[$i],$oldrank[$i],$maxrank[$i],$robo[$i],$fixedrank[$i],
		   $gameswon[$i],
		   $ladder_level[$i],$ladder_order[$i],$ladder_mentor[$i])
						= &getrank($dbh,$u[$i],$game,$master); 
						
	      if($ladder_level[$i] eq "") { $ladder_level[$i]=0; }
	      if($ladder_order[$i] eq "") { $ladder_order[$i]=0; }

		   if($oldrank[$i] == 0) 
				{ # watch out that sick puppies don't corrupt the database
					$error++; 
		    }
		};
		$new_ranking[$i]=$oldrank[$i];
	}
  
 my $es = "";
 if( $u[1] eq "" ) { $error++; $es .= "No Player 1\n"; };
 if( $u[2] eq "" ) { $error++; $es .= "No Player 2\n"; };
 if(( $u[1] eq $u[2]) 
       && !($robo[1] eq "g"))
     { # players playing with themselves, except guests
     $error++;
     $es .= "Player 1 = Player 2 (=$pname[1])\n"; 
     }

 if($error) { __dm($es); }
 if($error==0)
	{
		my $session=param('session');
		my $key = param('key');
		my $port = param('sock');
		if($port<2240) { $port=$'game_server_port}

		if( !&check_server($port,$session,$key,$u[1],$u[2]))
		{
		 __dm( __LINE__." scoring rejected: \"$'reject_line\" $ENV{'QUERY_STRING'}" );
		 print "\n** Ranking update was rejected by the server **\n";
		}
		elsif (($robo[1] ne "g") && ($robo[2] ne "g"))
		{
		#
		# Only record rankings for two player game, not including a guest
		#
		my $newwon = 0;
		my $newtie = 0;
		my $newlost = 0;
		my $last_played = time(); #seconds since 1970
		if($s[1]==$s[2])   { $newtie++;  }else { $newwon++;$newlost++ }
		
	#
	# Compute changes in rankings
	# 
	my $m = sqrt( abs( $s[1] - $s[2] ));
	my $winner;
	my $loser;
	my $draw = 0;

	if( $s[1] > $s[2] ) {
		$winner = 1;		
		$loser = 2;
		} else {
		$winner = 2;
		$loser = 1;
		if($s[1]==$s[2]) 
		{ $draw=1; 
		  $winner = ($oldrank[1]>$oldrank[2]) ? 2 : 1;
		  $loser = ($winner==2) ? 1 : 2;
		}
		}

	my $unranked1 = $nr[$winner] eq "true";
	my $unranked2 = $nr[$loser] eq "true";

	# register alerts if any
	if(!$unranked1 && !$unranked2)
	{ my $focus = param('focus');
	  my $adv = param('advantage');
	  my $sam = param('samepublicip');
	  my $samm = param('samebrowser');
	  my $samh = param('samelocalip');
	  my $samc = param('sameclock');
	  my $fch1 = $fch[$winner];
	  my $fch2 = $fch[$loser];
    my $s1=param('s1');
    my $s2=param('s2');
    my $msgid = "$fname $pname[1]=$s1 $pname[2]=$s2";
    
    #
    # note, don't combine two queries with OR as that forces mysql to scan all rows
    #
    if($digest_end!=0)
      { my $n = &checkdigest($dbh,"Endgame $msgid",
                    "SELECT gamename from zertz_gamerecord WHERE digest_end='$digest_end'");
        if(($n==0) && ($digest_mid!=0))
        { &checkdigest($dbh,"Midgame $msgid","SELECT gamename from zertz_gamerecord WHERE digest_mid='$digest_mid'");
        }
      }
    
	  if(($focus>0) || ($adv>0) || ($fch1 || $fch2) || ($fsw>1))
	  { 
	    if($fch1) { $fch1 = " focus-$pname[$winner]=$fch1"; }
	    if($fch2) { $fch2 = " focus-$pname[$loser]=$fch2"; }

	    if($adv) { $adv =  " advantage=$adv"; }
	    if($focus) { $focus = " focus=$focus"; }
	    if($sam) { $focus .= " same-public-ip"; }
	    if($samm) { $focus .= " same-browser"; }
	    if($samh) { $focus .= " same-local-ip"; }
	    if($samc) { $focus .= " same-clock"; }
		if($fsw > 1) { $focus .= " follower=$fsw"; }
	    $focus .= $fch1 . $fch2;
	    my $msg = "$msgid $adv$focus";
	    my $qstr=$dbh->quote($msg);
		&commandQuery($dbh,"INSERT into messages SET type='alert',message=$qstr");
		}
		}
			
  # now the actual ranking calculation
	{
   	$new_ranking[$winner] = &next_rank($draw,$oldrank[$winner],$oldrank[$loser],$gameswon[$winner],$gameswon[$loser],1);
   	$new_ranking[$loser] = &next_rank($draw,$oldrank[$loser],$oldrank[$winner],$gameswon[$loser],$gameswon[$winner],-1);
	if(!$draw)
	{
	 # don't attempt system adjustment in drawn games, since it will probably result in both
	 # players ranks moving in the same direction, which is hard to explain.
   	 my $bonus = &check_bonus($dbh,$game);
	 my $bl = $bonus/2;
	 my $bw = $bonus-$bl;
	 $new_ranking[$winner] += $bw;
	 $new_ranking[$loser] += $bl;
	 #print "Bonus $bonus\n";
     # if the ranking of the winner decreases, restore him.  This slows down the correction
     # of the system, but saves a lot of explaining.
     if($new_ranking[$winner]<$oldrank[$winner]) { $new_ranking[$winner]=$oldrank[$winner]; }
	 # similarly, if the rank of the loser increases
	 if($new_ranking[$loser]>$oldrank[$loser]) { $new_ranking[$loser]=$oldrank[$loser]; }
	}
	my $newmax = $maxrank[$winner];
	my $newmax2 = $maxrank[$loser];
    my $qgame = $dbh->quote($game);
    my $ism = $master ? 'yes' : 'no';
	my $mmstr = $master ? "Master " : "";
	my $plaism = $master ? "AND players.is_master='y'" : "";
	if($new_ranking[$winner]>$newmax) { $newmax= $new_ranking[$winner]; }

	#note that the loser can gain points too, especially if this is a tie game
	if($new_ranking[$loser]>$newmax2) { $newmax2=$new_ranking[$loser]; }  
	$newmax = $dbh->quote($newmax);
	$newmax2 = $dbh->quote($newmax2);
	
	my $loserLadder="";
	my $winnerLadder="";
	my $ladderUpdate1 = "";
	my $newWin = 0;
	my $hasLadder = 0;
	if(!$unranked1 && !$unranked2 && !$robo[$winner] && !$robo[$loser])
	{
	# figure the ladder updates
	$hasLadder = 1;
	if($ladder_level[$winner] <= 0)
		 { my $ll = &entry_ladder_level($dbh,'ranking',$qgame);
		   $ladder_level[$winner] = $ll;
		   $winnerLadder = "ladder_level = $ll,";  
		   $newWin = 1;
		 }
	if($ladder_level[$loser] <= 0) 
		{ my $ll = &entry_ladder_level($dbh,'ranking',$qgame);
		  $ladder_level[$loser] = $ll;
		  $loserLadder = "ladder_level = $ll,"; 
		}
	#
	# add some components to the main update
	#
	if( $ladder_level[$winner] >= $ladder_level[$loser])	   # winner at same or a higher level
			{
			#move the winner up and the loser left
			my $oldWinLevel = $ladder_level[$winner];
			my $newWinLevel = $oldWinLevel;
			my $movedup=0;
			# winner will be moved up a level by the primary update
			if(($oldWinLevel>1) 
				&& !$newWin 
				&& !($ladder_mentor[$winner] eq $u[$loser])	# dont allow the same loser to advance you twice
				)
			{ $winnerLadder .= "ladder_level=ladder_level-1,ladder_mentor='$u[$loser]',";
			  $ladder_level[$winner] -= 1;
			  $newWinLevel -= 1;
			  $movedup=1;
			}
			if(($robo[$winner] eq '') || $movedup)
			{
			#robot doesn't move just for playing
			$winnerLadder .= "ladder_order=-1,";	# move up and left
			}
			# loser will be repositioned to 0 by the primary update
			$loserLadder .= "ladder_order=0,";
			$ladderUpdate1 = &reorder_ladder_query('ranking',$qgame,$newWinLevel);
			if($newWinLevel!=$oldWinLevel)
				{	$ladderUpdate1 .= &reorder_ladder_query('ranking',$qgame,$oldWinLevel);
				}
			if(($ladder_level[$loser] != $oldWinLevel) && ($ladder_level[$loser]!=$newWinLevel))
				{	$ladderUpdate1 .= &reorder_ladder_query('ranking',$qgame,$ladder_level[$loser]);
				}
			}
			else
			{
			#move both left
			my $oldWinLevel = $ladder_level[$winner];
			my $oldLoseLevel = $ladder_level[$loser];
			# both players move left
			if($robo[$winner] eq '')
			{
			$winnerLadder .= "ladder_order=0,";	# move up and left, leave room for 1
			}
			if($robo[$loser] eq '')
			{
			$loserLadder .= "ladder_order=0,";
			}
			$ladderUpdate1 = &reorder_ladder_query('ranking',$qgame,$oldWinLevel)
					. &reorder_ladder_query('ranking',$qgame,$oldLoseLevel);
			}
	}
	
	if(!$unranked1)
    {my $command="UPDATE players,ranking
	   				SET players.last_played=$last_played,
                    ranking.last_played=$last_played,
	    			value=$new_ranking[ $winner ],
	    			$winnerLadder
		    		games_won=games_won+$newwon,
                    players.games_played=players.games_played+1,
                    ranking.games_played=ranking.games_played+1,
		    		max_rank = $newmax
		    		WHERE players.uid='$u[ $winner ]' $plaism AND ranking.is_master='$ism' AND ranking.uid='$u[ $winner ]' AND variation=$qgame";
				#print "$command\n";
				&commandQuery($dbh,$command);
    }
    else 
    { $new_ranking[$winner]=$oldrank[$winner]; 
      my $command="UPDATE players 
	   				SET players.last_played=$last_played
		    		WHERE players.uid='$u[ $winner ]'";
	  &commandQuery($dbh,$command);
    }
    &printrank("${mmstr}ranking",$pname[$winner],$new_ranking[$winner],$oldrank[$winner],
		$hasLadder ? $ladder_level[$winner] : 0);
	  
	if(!$unranked2)
   {my $command="UPDATE players,ranking
	   			   SET players.last_played=$last_played,
                   ranking.last_played=$last_played,
	    		   value=$new_ranking[ $loser ],
		    	   games_lost=games_lost+$newlost,
		    	   $loserLadder
                   players.games_played=players.games_played+1,
                   ranking.games_played=ranking.games_played+1,
                   max_rank = $newmax2
		    	   WHERE players.uid='$u[ $loser ]' $plaism  AND ranking.is_master='$ism'  AND ranking.uid='$u[ $loser ]' AND variation=$qgame";
	#print "$command\n";
			 &commandQuery($dbh,$command);
	  } 
    else 
    { $new_ranking[$loser]=$oldrank[$loser]; 
    my $command="UPDATE players 
	   				SET players.last_played=$last_played
		    		WHERE players.uid='$u[ $loser ]'";
	#print "$command\n";
	&commandQuery($dbh,$command);
    }
    &printrank("${mmstr}ranking",$pname[$loser],$new_ranking[$loser],$oldrank[$loser],
		$hasLadder ? $ladder_level[$loser] : 0);

	#print "ladder $ladderUpdate1\n";
	if(!($ladderUpdate1 eq ""))
	{
		#print "$ladderUpdate1\n";
		&commandQuery($dbh,$ladderUpdate1);
	}
	
    &make_log("$pname[1]($new_ranking[1])\t$s[1]\t$pname[2]($new_ranking[2])\t$s[2]\t$fname");
				
	} 
  #update zertz_gamerecord set date=date,gmtdate=reverse(substring(reverse(gamename),1,15)) 
	# if we have uids, record a game record
	{
	my $mode = $master ? 'master' 
				:($unranked1 && $unranked2) 
					? 'Unranked' : 'Normal';
    my $winp = $draw ? 'draw' : (($winner==1) ? "player1" : "player2");
    my $now = $dbh->quote(&ctime());
    my $q = "INSERT INTO zertz_gamerecord SET player1='$u[$winner]',player2='$u[$loser]',"
     . " time1='$t[$winner]',time2='$t[$loser]',"
     . " rank1='$new_ranking[$winner]',rank2='$new_ranking[$loser]',"
     . " variation='$game',mode='$mode',tournament='$tourney',"
     . " digest_end='$digest_end',digest_mid='$digest_mid',"
     . " winner='$winp',"
     . " gmtdate=$now,"
     . " gamename='$fname'";
	#print "Q: $q\n";
	&commandQuery($dbh,$q);
	}
	
	} #end of ranking update
	
	
	
	}
 	&disconnect($dbh);

	}
    }
    else
    { print "Unknown game type: $game0\n";
    }
  }
  }
  else 
  {
  print header;

  __d( "No update parameters parameter found..." );
  print "score update failed\n";
  }
__dEnd( "end!" );
