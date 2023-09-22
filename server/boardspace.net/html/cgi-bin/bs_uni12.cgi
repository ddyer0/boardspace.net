#!/usr/bin/perl
#
# 9/2023 record results of game with up to 12 players
# 9/2009 record results of game with up to 6 players
# 1/2014 record results in the ranking ladder
# sample param for 3p game
# params=IQJehgmtwmpQpDKDAuveCwz+ln23Fz1HPLFucb2LZtHicEDh9C57V+FoRg/Xr7UYZhJsfTUbIbIsKJtjJI5DeyTjdMWntv1U9+NAI210gHedThzReMvB5BL9oKN7dbEoe6JPO76s3zet34ILZplzt9gYTeef08VleVjBdlZ+aFGRcpbJOi9Ef7k1kMXnWO9B4QPqnr96SZvNX61nBrRRpwHy29Qhozw5J0CJqF4JBQYynQ/0uhvciG1L2FFc3ZiNuPMczGS7u4qphwnucqtyJ+r8A0aKn0cQPCpNDw==
# sample for 8p game
# Score Url &de=1799655051&dm=-174572645&game=IM&u1=40008&s1=0&fch1=11&t1=365&u2=40006&s2=1&t2=361&u3=40003&s3=0&t3=448&u4=40001&s4=-1&t4=219&u5=40004&s5=5&t5=346&u6=40007&s6=1&t6=321&u7=40005&s7=-5&t7=372&u8=40002&s8=-1&t8=424&key=80.80.132.192&session=1&sock=2255&mode=&sameclock=1&fname=IM-b8-b6-b3-b1-b4-b7-b5-b2-2023-09-20-2255&xx=xx
# Game b4 19+97.216 postURLStream /cgi-bin/bs_uni12.cgi?params=OWvS6Lta8fnScESnKMXLdOekXyVmZYe6oCstgZnqX8kOve7evzZSCTTg3WcT0OWxmyKLdK0aG69UpmERCSsmv8xgjEEjbLryN0YN0gnyVEnyDGaTdizgQXZCD7jlRrxVMPrDyQXZ4PLuLte6Qw5QouF+nxRkNmm7OjiBW1659jzewmHkUyYV280Dt3sOusakM0oGw4evFf/yThZGBQnl5vQejBGmxYGp6Z4LOli330JlLfUK9kFgT+eggquXiU1gzEZXNazN556Lgw8LJvaeBM6fh8IZo6/6nCjW+X6H4FiaLVSA2wGtLq0470YOis/GuAnXcJfiIvyxYD7hjbQ6IBhLcZKMpWRW15vtqdb5Ke6BUyTRqtLTzN+75Gw7/ZahUnzlOYwE+6y5RKyBotjk7jvEwr3VxRl3NhCKaExpuSUiyzDs4dKdwYjkTiGl47fW0vlre32QzZOfk/TRPLcqXg==


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

sub main_score_routine()
{ my ($dbh,$game) = @_;
  my $fname = param('fname');
  my $tourney = param('tournament') ? 'Yes' : 'No';
  my $digest_mid = param('dm');
  my $digest_end = param('de');
  my $master = (param('mm') eq 'true');

  my $hasGuest = 0;
  my $hasUnranked = 0;
  my $numPlayers = 0;
  my $blank = 0;
  my $error = 0;
  my (@pname,@s,@nr,@u,@t,@c,@fch,@change,@wins,@losses,@newrank);
  my (@oldrank,@maxrank,@robo,@fixedrank,@gameswon,@ladder_level,@ladder_order);
  my @ladder_resort;
  my $fsw = &param('fsw');	# follower state warning
  my $es = "";
  for(my $low=1;$low<=12;$low++)
	{
	$change[$low] = 0;					# no change in score
	$wins[$low] = 0;
	$losses[$low] = 0;
        $u[$low] = param("u$low");		# uid
        $t[$low] = param("t$low");		# time for player
	$s[$low] = param("s$low");    # score for each player
	$fch[$low] = param("fch$low");# fraud check
	$nr[$low] = param("nr$low");  # unranked request
	if($nr[$low] eq 'true') { $hasUnranked = 1; }

	if($u[$low] ne "")
		{ ($pname[$low],$oldrank[$low],$maxrank[$low],$robo[$low],$fixedrank[$low],
		   $gameswon[$low],
		   $ladder_level[$low],$ladder_order[$low])=&getrank($dbh,$u[$low],$game,$master);
		   $numPlayers++;
		   if($blank)
		   		{ $error++;
		   		  $es .= "Non Sequential player before $low\n";
		   		}
		   if($oldrank[$low] == 0)
				{ # watch out that sick puppies don't corrupt the database
					$error++;
          $es .= "user $pname[$low] $u[$low] no rank\n";
		    }
		}
		else { $blank = 1; };
	}

 if( $u[1] eq "" ) { $error++; $es .= "No Player 1\n"; };
 if( $u[2] eq "" ) { $error++; $es .= "No Player 2\n"; };


 # check for duplicate players
 for(my $low = 1; $low<=$numPlayers; $low++)
		{ if($robo[$low] eq "g") { $hasGuest++; }
			for(my $high = $low+1; $high<=$numPlayers; $high++)
			{
	  if(($u[$low] eq $u[$high])
       	&& !($robo[$low] eq "g"))
	 {
	 $error++;
	 $es .= "Player $low = Player $high (=$pname[$low])\n";
			}
			}
		}

 if($error==0)
	{
		my $session=param('session');
		my $key = param('key');
		my $port = param('sock');
		if($port<2240) { $port=$'game_server_port}

		if(!&check_server($port,$session,$key,$u[1],$u[2],$u[3],$u[4],$u[5],$u[6],$u[7],$u[8],$u[9],$u[10],$u[11],$u[12]))
		{
		 __dm( __LINE__." scoring rejected: \"$'reject_line\" $ENV{'QUERY_STRING'}" );
		 print "\n** Ranking update was rejected by the server **\n";
		 return;
		}
		elsif (!$hasGuest)
		{
	#
	#Only record rankings and so on for games, not including a guest
	#
	my $last_played = time(); #seconds since 1970
 	my $bonus = &check_bonus($dbh,$game);
	my $qgame = $dbh->quote($game);

	if($hasUnranked==0)
	{
	# check the fraud parameters and issue alerts
	 my $focus = param('focus');
	 my $adv = param('advantage');
	 my $sam = param('samepublicip');
	 my $samm = param('samebrowser');
	 my $samh = param('samelocalip');
	 my $samc = param('sameclock');
	 my $msgid = "$fname ";
	 my $fchsum = 0;
	 my $fchmsg = "";
	 #add names and scores
	 print "Bonus $bonus\n";
  
	 for(my $low=1;$low<=$numPlayers;$low++)
		{ $msgid .= " $pname[$low]=$s[$low]";
		  my $fc = $fch[$low];
		  $fchsum += $fc;
		  if($fc>0) { $fchmsg .= " focus-$pname[$low]=$fc"; }
		}

	 #
       # note, don't combine two queries with OR as that forces mysql to scan all rows
       #
      if($digest_end!=0)
      { my $qdend = $dbh->quote($digest_end);
        my $n = &checkdigest($dbh,"Endgame $msgid",
                    "SELECT gamename from mp_gamerecord WHERE digest_end=$qdend");
        if(($n==0) && ($digest_mid!=0))
        { my $qdmid = $dbh->quote($digest_mid);
	  &checkdigest($dbh,"Midgame $msgid","SELECT gamename from mp_gamerecord WHERE digest_mid=$qdmid");
        }
      }

	if(($focus>0) || ($adv>0) || ($fchsum>0) || ($fsw>1))
	  {
	    if($adv) { $adv =  " advantage=$adv"; }
	    if($focus) { $focus = " focus=$focus"; }
	    if($sam) { $focus .= " same-public-ip"; }
	    if($samm) { $focus .= " same-browser"; }
	    if($samh) { $focus .= " same-local-ip"; }
	    if($samc) { $focus .= " same-clock"; }
		if($fsw > 1) { $focus .= " follower=$fsw"; }
	    $focus .= $fchmsg;
	    my $msg = "$msgid $adv$focus";
	    my $qstr=$dbh->quote($msg);
		&commandQuery($dbh,"INSERT into messages SET type='alert',message=$qstr");
		}
		}

		# the actual ranking calculations
		for(my $low = 1; $low<$numPlayers; $low++)
			{
			for(my $high = $low+1; $high<=$numPlayers; $high++)
				{
		#
		# Compute changes in rankings
		#
		my $m = sqrt( abs( $s[$low] - $s[$high] ));
		my $winner;
		my $loser;
		my $draw = 0;
		#
		# assign winner and loser
		#
		if( $s[$low] > $s[$high] )
			{
			$winner = $low;
			$loser = $high;
			$wins[$winner]++;
			$losses[ $loser]++;

			}
			else
			{
			$winner = $high;
			$loser = $low;
			if($s[$low]==$s[$high])
				{ $draw=1;
		  		$winner = ($oldrank[$low]>$oldrank[$high]) ? $high : $low;
		  		$loser = ($winner==$high) ? $low : $high;
				}
				else
				{
				$wins[$winner]++;
				$losses[ $loser]++;
				}
			}

  	# now the actual ranking calculation
    my $wchange =  &next_rank($draw,$oldrank[$winner],$oldrank[$loser],$gameswon[$winner],$gameswon[$loser],1)
   										- $oldrank[$winner];
    my $lchange =  &next_rank($draw,$oldrank[$loser],$oldrank[$winner],$gameswon[$loser],$gameswon[$winner],-1)
   										- $oldrank[$loser];
   	# detailed scoring info
    my $l1 = $draw ? "tie" : "winner";
    my $l2 = $draw ? "" : "loser";

	if(!$draw)
	{
	 # don't attempt system adjustment in drawn games, since it will probably result in both
	 # players ranks moving in the same direction, which is hard to explain.
	 my $bl = $bonus/2;
	 my $bw = $bonus-$bl;
	 $wchange += $bw;
	 $lchange += $bl;
	 #print "Bonus $bonus\n";
   # if the ranking of the winner decreases, restore him.  This slows down the correction
   # of the system, but saves a lot of explaining.
 	 if($wchange<0) { $wchange=0; }
	 # similarly, if the rank of the loser increases
	 if($lchange>0) { $lchange=0; }
	}


    my $msg = "pair $l1 $pname[$winner] $s[$winner] : $wchange  - $l2 $pname[$loser] $s[$loser] : $lchange\n";
    print $msg;
    __d($msg);
   	$change[$winner] += $wchange;
   	$change[$loser] += $lchange;
				}
			}

	{
	my $logstr = "";
	my @mostwins;
	
	{
	# sort the player indexes by most wins, so the ladder updates
	# will do the ultimate winner last, leaving him on top relative 
	# to the other players.
	#
	for(my $idx = 0; $idx<=$numPlayers; $idx++)
	{	my @ww = ($wins[$idx],$idx);
		push(@mostwins,\@ww);
	}
	@mostwins = sort { @$a[0] <=> @$b[0] } @mostwins;
	}
	for(my $mw = 1; $mw <=$numPlayers; $mw++)
	{	my $wins = $mostwins[$mw];
		my $low = @$wins[1];
		my $newmax = $maxrank[$low];
		my $lowsm = $master ? 'yes' : 'no';
    	my $mmstr = $master ? "Master " : "";
	    my $plaism = $master ? "AND players.is_master='y'" : "";
		  $newrank[$low] = $oldrank[$low]+$change[$low];
	  if( $nr[$low] )
	  {
	  $newrank[$low] = $oldrank[$low];
	  my $qlow = $dbh->quote($u[ $low ]);
	  my $command = "UPDATE players,ranking
	   	SET players.last_played=$last_played
    		WHERE players.uid=$qlow ";
	  &commandQuery($dbh,$command);
	  }
	  else
	  {
	  # $mw orders by the sort order, not the player number
	  my $ladder_update = "ladder_order='-$mw',";
	  if($ladder_level[$low]<=0) 
		{	$ladder_level[$low] = &entry_ladder_level($dbh,'ranking',$qgame);
			 my $qladlow = $dbh->quote($ladder_level[$low]);
			$ladder_update .= "ladder_level = $qladlow,";
		}
		elsif($wins[$low]>0 && ($ladder_level[$low]>1))
		{	$ladder_update .= "ladder_level = ladder_level-1,";
			&pushnew(\@ladder_resort,$ladder_level[$low]);
			$ladder_level[$low]--;
		}
	
	  &pushnew(\@ladder_resort,$ladder_level[$low]);
	  #lowest levels are very volatile, one new game can upset them all.
	  if($ladder_level[$low]<=2)
		{ &pushnew(\@ladder_resort,$ladder_level[$low]+1);	
		  &pushnew(\@ladder_resort,$ladder_level[$low]+2);
		}
	  
	  if($newrank[$low]>$newmax) { $newmax= $newrank[$low]; }
  	  $newmax = $dbh->quote($newmax);
	  my $qlow = $dbh->quote($u[ $low ]);
	  my $qlowsm = $dbh->quote($lowsm);
	  my $command=
	   "UPDATE players,ranking
	   	SET players.last_played=$last_played, ranking.last_played=$last_played,
	    	value=$newrank[$low],
    		games_won=games_won+$wins[$low],
    		games_lost=games_lost+$losses[$low],
    		$ladder_update
            players.games_played=players.games_played+1,
            ranking.games_played=ranking.games_played+1,
   		    max_rank = $newmax
    		WHERE players.uid=$qlow $plaism AND ranking.is_master=$qlowsm
    		      AND ranking.uid=$qlow AND variation=$qgame";
		#print "Q: $command\n";
		&commandQuery($dbh,$command);
		}
			&printrank("${mmstr}ranking",$pname[$low],$newrank[$low],$oldrank[$low],$ladder_level[$low]);
			$logstr .= "$pname[$low]($newrank[$low])\t";
			}


    &make_log($logstr . $fname);
	}

	}


  #update mp_gamerecord set date=date,gmtdate=reverse(substring(reverse(gamename),1,15))
	# if we have uids, record a game record
	{
	my $mode = $master ? 'master'
				: ($hasUnranked? 'Unranked' : 'Normal');
  my $now = $dbh->quote(&ctime());
  my $qpl = "" ;
  for(my $low = 1; $low<=$numPlayers; $low++)
		{	my $uv = $dbh->quote($u[$low]);
      my $tv = $dbh->quote($t[$low]);
      my $sv = $dbh->quote($s[$low]);
      my $rv = $dbh->quote($newrank[$low]);
      $qpl .=
          "player${low}=$uv,time${low}=$tv,score${low}=$sv,rank${low}=$rv,";
		}
    my $qgame = $dbh->quote($game);
    my $qmode = $dbh->quote($mode);
    my $tmode = $dbh->quote($tourney);
    my $dend = $dbh->quote($digest_end);
    my $dmid = $dbh->quote($digest_mid);
    my $gname = $dbh->quote($fname);
    
    my $q = "INSERT INTO mp_gamerecord SET $qpl "
     . " variation=$qgame,mode=$qmode,tournament=$tmode,"
     . " digest_end=$dend,digest_mid=$dmid,"
     . " gmtdate=$now,"
     . " gamename=$gname";
	#print "Q: $q\n";
	&commandQuery($dbh,$q);
 
	# reorder the ranking ladders
	#print "Ladder resort $#ladder_resort \n";
 	if($#ladder_resort>=0)
	{
	@ladder_resort = reverse sort(@ladder_resort);	# put them in order
	#print "L : @ladder_resort\n";
	my $ladder_update_query = "";
	while($#ladder_resort>=0)
	{	my $lvl = pop(@ladder_resort);
		$ladder_update_query .= &reorder_ladder_query('ranking',$qgame,$lvl) . "\n";
	}
	#print "Q: $ladder_update_query\n";
	&commandQuery($dbh,$ladder_update_query);
	}
	}
	} #end of ranking update
  else
  {  print "Rejected: $es\n";
  }

}

sub doit()
{	__dStart( "$'debug_log",$ENV{'SCRIPT_NAME'});;

	if( param() ) 
	{
	my $ok = &useCombinedParams($'tea_key);
	if($ok)
	{
  	print header;
	__d("from $ENV{'REMOTE_ADDR'} $ENV{'QUERY_STRING'}" );
  	my $game0 = param('game');
  	if($game0)
	 {
	  my $dbh = &connect();              # connect to local mysqld
	  if($dbh)
	 	{
	  	my $game = &gamecode_to_gamename($dbh,$game0);
		&main_score_routine($dbh,$game);
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
}


$| = 1;                         # force writes

&doit();
