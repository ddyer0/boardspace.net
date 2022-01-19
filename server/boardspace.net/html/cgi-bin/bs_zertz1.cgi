#!/usr/bin/perl
#
# record results of game
#

use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use IO::File;

require "include.pl";
require "tlib/gs_db.pl";

#
# check to see if the server is up, and thinks this is a
# legitimate result.  log problems!
#
sub check_server
{ my ($port,$session,$key,$p1,$p2)=@_;
  unless (socket(SOCK, PF_INET, SOCK_STREAM, $'proto))
  { __d("socket failed  $!\n");
    return 0;
  }
  unless (connect(SOCK,sockaddr_in($port, inet_aton($'ip_name)))) {
    __d("connect($port,$'ip_name) failed: $!\n");
    return 0;
  }
  autoflush SOCK 1;
  my $msg = "218 $session $p1 $p2 $key";
  print SOCK "$msg\n";
  my $line = <SOCK>;
  $line = substr($line,1,5);
  my $ok=($line eq "219 1");						#209 1 means the server likes it
  #print "check $msg = ($line) ($ok)\n";
  close(SOCK);
  return ($ok);
}

sub make_log()
{
 my ($msg ) = @_;
 my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
 open( F_OUT, ">>$'game_completed_log" );
 printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] %s\n",1900+$year,$mon+1,$mday,$hour,$min,$sec,$msg;
 close F_OUT;
}

sub getrank()
{ my($dbh,$u1)=@_;
  my $q = "SELECT player_name,ranking,max_rank,is_robot,fixed_rank,games_won FROM players WHERE uid='$u1'";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	if($nr != 1 )
	{ __dm( __LINE__."$nr players match for $u1" ); 
		#this shouldn't happen but it did once with a very sick machine
    &finishQuery($sth);
		return(0,0,0,0,0,0);
	}
	else
	{ my ($name,$oldrank,$oldmax,$is_robot,$fixed_rank,$games_won) = &nextArrayRow($sth);
    &finishQuery($sth);
 	  if( $oldrank <= 0 ) {  $oldrank = 1500; }
    return($name,$oldrank,$oldmax,$is_robot,$fixed_rank,$games_won);
	}
}

my $handicap_change_rate=0.01;
my $confidence_change_rate=0.01;
my $max_confidence = 1.0;

sub next_handicap()
{	my ($p1_handicap,$p1_confidence,$p2_handicap,$p2_confidence,$score)=@_;
	my $handicap_spread = $p1_handicap - $p2_handicap;
	my $spread = $score-$handicap_spread;
	my $rate = ($p2_confidence+(1.0-$p1_confidence))*$handicap_change_rate;
	my $new_handicap = $p1_handicap + $rate*$spread;
	my $new_confidence = $p1_confidence + $confidence_change_rate;
	
	if ($new_confidence > $max_confidence) { $new_confidence=$max_confidence; }
	#print "old $p1_handicap($p1_confidence) $p2_handicap($p2_confidence) $score = $new_handicap\n";
	return(($new_handicap,$new_confidence));
}
sub next_rank()
{	my ($winner,$loser,$winner_games,$loser_games,$dir) = @_;
	$winner = ($winner-1500.0)/1000.0;
	$loser = ($loser-1500.0)/1000.0;
  my $wconf = ($winner_games+1)/20;  #confidence increases until you get 20 wins
  my $lconf = ($loser_games+1)/20;
  if($wconf>1) { $wconf=1; }
  if($lconf>1) { $lconf=1; }
  
	my ($nextr,$nextc) = &next_handicap($winner,$wconf,$loser,$lconf,1.1*$dir);
	return(1500 + int($nextr*1000.0));
}

sub printrank()
{ my ($type,$p,$new_ranking,$oldrank) = @_;
	if($oldrank eq $new_ranking)
		{print "${p}'s $type is $oldrank (unchanged)\n";
    }
    else
   {my $dir = ($oldrank > $new_ranking) ? "decreased" : "increased";
		print "${p}'s $type $dir from $oldrank to $new_ranking\n";
   }
}


$| = 1;                         # force writes
print header;
	
__dStart( "$'debug_log",$ENV{'SCRIPT_NAME'});;
if( param() ) 
{
  $| = 1;                         # force writes
  __d("from $ENV{'REMOTE_ADDR'} $ENV{'QUERY_STRING'}" );
  my ($game) = param('game');
  my $fname = param('fname');
  my $tourney = param('tournament') ? 'Yes' : 'No';

  {
  my $dbh = &connect();              # connect to local mysqld
  if($dbh)
	{
  my $error = 0;
  my (@pname,@s,@nr,@u,@t,@c,@fch);
  my (@oldrank,@maxrank,@robo,@fixedrank,@new_ranking,@gameswon);
  my $i;
  for($i=1;$i<=2;$i++)
	{
  $u[$i] = param("u$i");		# uid
  $t[$i] = param("t$i");		# time for player
	$s[$i] = param("s$i");    # 1 for winner, 0 for loser
	$fch[$i] = param("fch$i");# fraud check
	$nr[$i] = param("nr$i");  # unranked request
	if($u[$i] ne "") 
		{ ($pname[$i],$oldrank[$i],$maxrank[$i],$robo[$i],$fixedrank[$i],$gameswon[$i])=&getrank($dbh,$u[$i]); 
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
 if(($error==0) && ($robo[1] ne "g") && ($robo[2] ne "g"))
	{
		my $session=param('session');
		my $key = param('key');
		my $port = param('sock');
		if($port<2240) { $port=$'game_server_port}

		if( !&check_server($port,$session,$key,$u[1],$u[2]))
		{
		 __dm( __LINE__." Session or Key Mismatch: $ENV{'QUERY_STRING'}" );
		 print "\n** Ranking update was rejected by the server **\n";
		}
		else
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
		
		if( $s[1] > $s[2] ) {
			$winner = 1;		
			$loser = 2;
		} else {
			$winner = 2;
			$loser = 1;
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
	  if(($focus>0) || ($adv>0) || ($fch1 || $fch2))
	  { my $s1=param('s1');
	    my $s2=param('s2');
	    if($fch1) { $fch1 = " focus-$pname[$winner]=$fch1"; }
	    if($fch2) { $fch2 = " focus-$pname[$loser]=$fch2"; }

	    if($adv) { $adv =  " advantage=$adv"; }
	    if($focus) { $focus = " focus=$focus"; }
	    if($sam) { $focus .= " same-public-ip"; }
	    if($samm) { $focus .= " same-browser"; }
	    if($samh) { $focus .= " same-local-ip"; }
	    if($samc) { $focus .= " same-clock"; }
	    $focus .= $fch1 . $fch2;
	    my $msg = "$pname[1]=$s1 $pname[2]=$s2$adv$focus";
	    my $qstr=$dbh->quote($msg);
		&commandQuery($dbh,"INSERT into messages SET type='alert',message=$qstr");
		}
		}
			
  # now the actual ranking calculation
	{
   	my $bonus = &check_bonus($dbh,$game);
   	$new_ranking[$winner] = &next_rank($oldrank[$winner],$oldrank[$loser],$gameswon[$winner],$gameswon[$loser],1) + $bonus;
   	$new_ranking[$loser] = &next_rank($oldrank[$loser],$oldrank[$winner],$gameswon[$loser],$gameswon[$winner],-1);
 		

	my $newmax = $maxrank[$winner];
	my $newmax2 = $maxrank[$loser];

	if($new_ranking[$winner]>$newmax) { $newmax= $new_ranking[$winner]; }
	     
	if(!$unranked1)
    {my $command="UPDATE players 
	   				  SET last_played=$last_played,
	    						ranking=$new_ranking[ $winner ],
		    					games_won=games_won+$newwon,
		    					max_rank = $newmax
		    					WHERE uid='$u[ $winner ]'";
				&commandQuery($dbh,$command);
    }
    else { $new_ranking[$winner]=$oldrank[$winner]; }
    &printrank("ranking",$pname[$winner],$new_ranking[$winner],$oldrank[$winner]);
	  
	if(!$unranked2)
   {my $command="UPDATE players 
	   				  SET last_played=$last_played,
	    						ranking=$new_ranking[ $loser ],
		    					games_lost=games_lost+$newlost,
                  max_rank = $newmax2
		    					WHERE uid='$u[ $loser ]'";
			 &commandQuery($dbh,$command);
	  } 
    else { $new_ranking[$loser]=$oldrank[$loser]; }
    &printrank("ranking",$pname[$loser],$new_ranking[$loser],$oldrank[$loser]);

    &make_log("$pname[1]($new_ranking[1])\t$s[1]\t$pname[2]($new_ranking[2])\t$s[2]\t$fname");
				
	} 

	# if we have uids, record a game record
	{
	my $mode = ($unranked1 && $unranked2) ? 'Unranked' :'Normal';
  my $var = ($game eq 'Z11') ? "variation='Zertz+11',"
              : ($game eq 'Z') ? "variation='Zertz'," : "";
  my $winp = ($winner==1) ? "player1" : "player2";
	my $q = "INSERT INTO zertz_gamerecord SET player1='$u[$winner]',player2='$u[$loser]',"
		. " time1='$t[$winner]',time2='$t[$loser]',"
		. " rank1='$new_ranking[$winner]',rank2='$new_ranking[$loser]',"
		. " $var mode='$mode',tournament='$tourney',"
    . " winner='$winp',"
		. " gamename='$fname'";
	&commandQuery($dbh,$q);
	}
	
	} #end of ranking update
	
	
	
	}
 	&disconnect($dbh);

	}
    }
  
  }
  else 
  {
  __d( "No update parameters parameter found..." );
  print "score update failed\n";
  }
__dEnd( "end!" );