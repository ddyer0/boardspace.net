#
# common subroutines for all rankings
#

# check to see if the server is up, and thinks this is a
# legitimate result.  log problems!
#
$'reject_line='';

sub check_server
{ # p3 p4 p5 p6 are optional
  my ($port,$session,$key,$p1,$p2,$p3,$p4,$p5,$p6)=@_;
  #print "not checking server\n";
  #return(1);
  unless (socket(SOCK, PF_INET, SOCK_STREAM, $'proto))
  { __d("socket failed  $!\n");
    return 0;
  }
  unless (connect(SOCK,sockaddr_in($port, inet_aton("localhost")))) {
    __d("connect($port,\"localhost\") failed: $!\n");
    return 0;
  }
  autoflush SOCK 1;
  my $msg = "218 $session $p1 $p2 $key";
  #
  # players 3 and above are optional
  #
  if($p3) { $msg .= " $p3"; }
  if($p4) { $msg .= " $p4"; }
  if($p5) { $msg .= " $p5"; }
  if($p6) { $msg .= " $p6"; }
  print SOCK "$msg\n";
  my $line = <SOCK>;
  $line = substr($line,1,5);
  my $ok=($line eq "219 1");						#209 1 means the server likes it
  #print "check $msg = ($line) ($ok)\n";
  $'reject_line = $line;
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
{ my($dbh,$u1,$game,$master)=@_;
  my $qgame = $dbh->quote($game);
  my $mstr = $master ? " AND ranking.is_master='yes'" : "AND ranking.is_master!='yes'";
  my $q = "SELECT player_name,value,max_rank,is_robot,fixed_rank,games_won,ladder_level,ladder_order,ladder_mentor "
    . " FROM players left join ranking on players.uid=ranking.uid "
    . " WHERE players.uid='$u1' $mstr AND ranking.variation=$qgame ";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);

	if($nr==0) 
	  {  # no hits, must be a new variation in the ranking table. try only the player table.
 		&finishQuery($sth);
		my $q = "SELECT player_name,0,0,is_robot,fixed_rank,0,0,0,0,0 FROM players WHERE uid='$u1'";
		$sth = &query($dbh,$q);
		$nr = &numRows($sth);
	  }

	if($nr != 1 )
	{ __dm("$ENV{'SCRIPT_NAME'} Line " .  __LINE__.": $nr players match for $u1\nquery: $q" ); 
		#this shouldn't happen but it did once with a very sick machine
    &finishQuery($sth);
		return(0,0,0,0,0,0, 0,0,0);
	}
	else
	{ my ($name,$oldrank,$oldmax,$is_robot,$fixed_rank,$games_won,$ladder_level,$ladder_order,$ladder_mentor) = &nextArrayRow($sth);
    &finishQuery($sth);
      if($ladder_level eq "") { $ladder_level=0; }
      if($ladder_order eq "") { $ladder_order=0; }
 	  if( $oldrank <= 0 ) 
       {  $oldrank = 1500; 
          # create the missing ranking table
		  my $mm = $master ? "'yes'" : "'no'";
          &commandQuery($dbh,"replace into ranking set is_master=$mm,uid='$u1',value='$oldrank',variation='$game'");          
       }
	  if($oldmax eq '') { $oldmax = '0'; }
    # is_robot is y for a real robot, g for a guest
    return($name,$oldrank,$oldmax,$is_robot,$fixed_rank,$games_won,
				$ladder_level,$ladder_order,$ladder_mentor);
	}
}

sub printrank()
{ my ($type,$p,$new_ranking,$oldrank,$newlevel) = @_;
	my $ll = ($newlevel eq 0) ? "" : " on ladder level $newlevel";
	if($oldrank eq $new_ranking)
	{print "${p}'s $type is $oldrank (unchanged)$ll\n";
    }
    else
   {my $dir = ($oldrank > $new_ranking) ? "decreased" : "increased";
	print "${p}'s $type $dir from $oldrank to $new_ranking$ll\n";
   }
}

my $handicap_change_rate=0.01;
my $confidence_change_rate=0.01;
my $max_confidence = 1.0;

sub next_handicap()
{	my ($draw,$p1_handicap,$p1_confidence,$p2_handicap,$p2_confidence,$score)=@_;
	my $handicap_spread = $p1_handicap - $p2_handicap;
	if($draw) { $score = $score/2; }
	my $spread = $score-$handicap_spread;
	my $rate = ($p2_confidence+(1.0-$p1_confidence))*$handicap_change_rate;

	my $new_handicap = $p1_handicap + $rate*$spread;
	my $new_confidence = $p1_confidence + $confidence_change_rate;

	#print "draw $draw $rate $p1_handicap $new_handicap score $score spread $spread\n";
	
	if ($new_confidence > $max_confidence) { $new_confidence=$max_confidence; }
	#print "old $p1_handicap($p1_confidence) $p2_handicap($p2_confidence) $score = $new_handicap\n";
	return(($new_handicap,$new_confidence));
}

sub next_rank()
{	my ($draw,$winner,$loser,$winner_games,$loser_games,$dir) = @_;
	$winner = ($winner-1500.0)/1000.0;
	$loser = ($loser-1500.0)/1000.0;
  my $wconf = ($winner_games+1)/20;  #confidence increases until you get 20 wins
  my $lconf = ($loser_games+1)/20;
  if($wconf>1) { $wconf=1; }
  if($lconf>1) { $lconf=1; }
  
  my ($nextr,$nextc) = &next_handicap($draw,$winner,$wconf,$loser,$lconf,1.1*$dir);

  return(1500 + int($nextr*1000.0));
}
#
# check a game digest against the database, looking for duplicate
# games, which are probably indicators of fraud.
#
sub checkdigest()
{  my ($dbh,$msg,$q) = @_;
   my $sth = &query($dbh,"$q LIMIT 10");
   my $nr = &numRows($sth);
   if($nr>0)
  { my $n = $nr;
    my $v = "";
    while($n-- > 0)
    { my ($a) = &nextArrayRow($sth);
      $v .= " $a";
    }
    my $qstr = $dbh->quote("Duplicate Digest at $msg: $nr games: $v");
    &commandQuery($dbh,"INSERT into messages SET type='alert',message=$qstr");
  }
  &finishQuery($sth);
  return($nr);
}
# return 1 to keep the loader happy
1;