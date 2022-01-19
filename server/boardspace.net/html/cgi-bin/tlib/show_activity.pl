use strict;
#
# modified 12/2006 to autoscale to the max number played in any box
# modified 4/2013 to offset by time zone
# modified 4/2013 to support "meetup.cgi"
#
sub formattedTime()
{	my ($time,$zone,$style) = @_;
	my @weekday = ("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat");
	my @month = ("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec");
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = gmtime($time);
	my $ap = ($hour <12) ? "am" : "pm";
	my $hh = ($hour <=12) ? $hour : $hour-12;
	#
	# 24 hour style typically used for gmt
	#
	if($style eq 'GMT_12hour')
	{
	return( sprintf("%s %s %d %d:00 $zone",&trans($weekday[$wday]),&trans($month[$mon]),$mday,$hour));
	}
	elsif($style eq 'GMT_hour')
	{
	return( sprintf("%s %d:00 $zone",&trans($weekday[$wday]),$hour));	
	}
	#
	# 12 hour style typically used for local times with specific dates.
	#
	elsif ($style eq '12hour')
	{
	return( sprintf("%s %s %d %d $ap $zone",&trans($weekday[$wday]),&trans($month[$mon]),$mday,$hh));
	}
	#
	# hour time gives generic day/hour without dates
	#
	elsif($style eq 'hour')
	{
	return( sprintf("%s %d $ap $zone",&trans($weekday[$wday]),$hh));	
	}
}
#
# getTimeTable returns a string which is a complete table for 7 days/24 hours
#
sub getTimeTable()
{	my ($stack0,$maxcount,$timezoneoffset,$caption,$ndays,$totcount,$timename,$style,$alttimezoneoffset,$alttimename) = @_;
	my @stack = @{$stack0};
    my $nn=0;
    my $scale = 10/$maxcount;
    my $daynumber=1;
    my $table = "";
    $table .=  "<table border=1 cellspacing=0>\n";
    my $minus = &timezoneOffsetString($timezoneoffset);
    my $cap = $caption ? $caption : &trans("Games finished at time")  . " (GMT$minus)";
    $table .= "<caption>$cap</caption>";
    my $weekday;
    my @days = ("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
    my $step=1;
    my $dbcount=0;
    my $dbday=0;
    my $dbhour=0;
    my $daynum=0;
    my $now = time();
    my ($sec,$cmin,$chour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime($now-(60*$timezoneoffset));
    my $current_hour = $wday * 24 + $chour;

    my $ctime = sprintf "%02d:%02d",$chour,$cmin;
    $wday++;
    $table .= "<tr><td>" 
		. &trans("Now: #1",$ctime) 
		. "</td>";
    for(my $hour=0; $hour<24; $hour++) 
      { my $bg=(($chour+0)==$hour) ? "bgcolor=red" : ""; 
        my $ap = ($hour<12) ? "<br>am" : "<br>pm";
        my $hh = ($hour<13) ? $hour : ($hour-12);
        $table .= sprintf "<td $bg align=center><font face=courier size=1>%s</font></td>",
			"$hh$ap"; 
      }
    $table .= "</tr>";
	my $hournum = 0;
    for $weekday (@days)
    { $daynum++;
      $weekday = &trans($weekday);
      my $bg = ($daynum==$wday) ? "bgcolor=red" : "";
      $table .= "<tr>";
      $table .= "<td $bg>";
      $table .= "<p font-size=8pt>$weekday</p>";
      $table .= "</td>\n";
      my $hour;
	
      for($hour=0; $hour<24; $hour++)
      { $hournum++;
		my $bgcolor="black";
        my $txt = "&nbsp;";
        if($step)
        {  $dbcount = @stack[$nn++];
	   $dbday = @stack[$nn++];
	   $dbhour = @stack[$nn++];
           $step=0;
        }
        if((($dbday+0) == $daynum)&&(($dbhour+0) == $hour))
        {  $step=1;
	   $dbcount=int($dbcount*$scale);
           if($dbcount>10) { $dbcount=10; }
           my $sp = ($dbcount==0) ? 0 : 64+19*$dbcount;
           $bgcolor = sprintf "%2x%2x%2x",$sp,$sp,$sp;
        }
        my $gmhour = $hournum - 1 - $current_hour;
        if($gmhour<0) { $gmhour += 24*7; }
        my $tip ="";
        my $click = "";
        
        {
        my $gmttime = &formattedTime($now+(60*60*$gmhour),"GMT","GMT_$style");
        my $localtime = &formattedTime($now+(60*60*$gmhour)-60*$timezoneoffset,$timename,$style);
        my $msg = "$localtime\n$gmttime";
        my $alert = "$localtime<br>$gmttime";
        if($alttimename)
        {
         my $alttime = &formattedTime($now+(60*60*$gmhour)-60*$alttimezoneoffset,$alttimename,$style);
         $msg .= "\n$alttime";
         $alert .= "<br>$alttime";
        }
        $tip = "title = '$msg'";
        $click = "onclick=\"openpop('$alert','Timezone times');\"";
        }

        $table .= "<td $tip $click align=center bgcolor=$bgcolor color=0xffffff>$txt</td>";
      }
      $table .= "</tr>\n";
    }
	if($ndays>0)
	{
	my $tot = $ndays > 59 
	? &trans("total of #2 games in the previous #1 months",int($ndays/30),$totcount)
	: &trans("total of #2 games in the previous #1 days",$ndays,$totcount);
    $table .=  "<tr><td colspan=25><center>"
	. $tot
	. "</center></td></tr>";
	}
    $table .= "</table>\n";
	return($table);
}
#
# return an array of triples, each triple is day/hour/count of games played.
# also returns the max count and total count of the triples.
#
sub get_activity_table()
{ my ($dbh,$uid,$nmonths,$timezoneoffset,$gametype) = @_;
  #
  # timezoneoffset if minutes from gmt
  # gametype is a game variation or '' for all games
  # uid is a player name or '' for all players
  #
  my $ndays = $nmonths ? int($nmonths*30) : ($uid ? 120 : 30) ;
  my $qid = $dbh->quote($uid);
  if($timezoneoffset eq "") { $timezoneoffset = 0; }
  my $uclause = ($uid==0) ? "" : " AND ((player1=$qid)OR(player2=$qid))";
  my $muclause = ($uid==0) ? "" : " AND ((player1=$qid)OR(player2=$qid)OR(player3=$qid)OR(player4=$qid)OR(player5=$qid)OR(player6=$qid))";
  my $qgame = $dbh->quote($gametype);
  my $gameclause = $gametype ? " and variation=$qgame " : "";
  if($dbh)
  {	&readtrans_db($dbh);
    my @stack;
    my $maxcount = 1;
    my $totcount = 0;
    my $q = "(SELECT count(gamename),"
      . " dayofweek(date_sub(gmtdate,interval $timezoneoffset minute)) as day,"
      . " hour(date_sub(gmtdate,interval $timezoneoffset minute)) as hhour"
      . " FROM zertz_gamerecord "
      . " WHERE gmtdate > date_sub(current_timestamp(),interval $ndays day) $uclause$gameclause"
      . " GROUP BY day,hhour)"
      . " UNION " 
	  . "(SELECT count(gamename),"
      . " dayofweek(date_sub(gmtdate,interval $timezoneoffset minute)) as day,"
      . " hour(date_sub(gmtdate,interval $timezoneoffset minute)) as hour"
      . " FROM mp_gamerecord "
      . " WHERE gmtdate > date_sub(current_timestamp(),interval $ndays day) $muclause$gameclause"
      . " GROUP BY day,hour)"
      . " ORDER BY day,hhour ";
    my $sth = &query($dbh,$q);
    my $nn = &numRows($sth);
	my $prevd;
	my $prevh;
	my $prevc;
    while($nn-- > 0)
	{ my ($dcount, $dday, $dhour) = &nextArrayRow($sth);
	  
	  $totcount += $dcount;
	  #
	  # combine adjacent elements which may be there because of the union
	  #
	  if(($prevd eq $dday) && ($prevh eq $dhour))
	  {	pop(@stack);
		pop(@stack);
		pop(@stack);
		$dcount += $prevc;
	  }
	  $prevd = $dday;
	  $prevh = $dhour;
	  $prevc = $dcount;
	  
	  if($dcount > $maxcount) { $maxcount = $dcount; }
	  push(@stack,$dcount);
	  push(@stack,$dday);
	  push(@stack,$dhour);
	}

    &finishQuery($sth);
	return(\@stack,$maxcount,$totcount);
   }
}

sub show_activity_table
{ my ($dbh,$uid,$nmonths,$timezoneoffset,$gametype,$caption) = @_;
  my ($stackref,$maxcount,$totcount) = &get_activity_table($dbh,$uid,$nmonths,$timezoneoffset,$gametype);
  my $ndays = $nmonths ? int($nmonths*30) : ($uid ? 120 : 30) ;

  print &getTimeTable($stackref,$maxcount,$timezoneoffset,$caption,$ndays,$totcount,&trans("Your local time"),'hour');
}

#
# unpack the stack into a hash array
# where the key is day*100+hour
# and the values are counts scaled to 0-1.0
#
sub unpackStack()
{	my ($stackref,$scale) = @_;
	my @stack = @{$stackref};
	my %result;
	while($#stack>0)
	{	my $hour = pop(@stack);
		my $day = pop(@stack);
		my $count = pop(@stack);
		my $val = $count/$scale;
		$result{$day*100 + $hour} = $val;
	}
	return(%result);
}
sub get_combined_tables()
{	my ($stack1ref,$max1,$tot1,$stack2ref,$max2,$tot2,$timezoneoffset) = @_;
	my @stack1 = @{$stack1ref};
	my @stack2 = @{$stack2ref};
	my %total1 = &unpackStack(\@stack1,$max1+1);
	my %total2 = &unpackStack(\@stack2,$max2+1);
	my @stack3 ;
	my $max3 = 1;
	my $tot3 = 1;
	for(my $day = 0; $day<32; $day++)
	{	
		for(my $hour = 0; $hour<25; $hour++)
		{
		my $t1 = $total1{$day*100+$hour};
		my $t2 = $total2{$day*100+$hour};
		if($t1>0 && $t2>0)
			{	
				my $count = ($t1*$t2)*10000;
				push(@stack3,$count);
				push(@stack3,$day);
				push(@stack3,$hour);
				if($count>$max3) { $max3 = $count; }
				$tot3 += $count;

			}
		}
	}
	return(\@stack3,$max3,$tot3);
	
}
1
