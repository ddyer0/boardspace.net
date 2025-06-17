#!/usr/bin/perl 
#
# generate regular, master and doubles rankings on demand.
#
#
# optional parameters:
#
# months=6                        number of months inactive before "retired"
# retired=0                       =1 for show retired players instead of active
# nitems=100                      =n to show top n players
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;

use strict;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/show_activity.pl";

#$database='test';

sub init 
{
   $| = 1;                         # force writes
}
sub rank_header()
{	my ($dbh,$nitems,$retired,$mode,$ccode,$variation,$myname,$order,$vname,$months)=@_;
	my $country = $ccode;
  my $header=param('header');
  my $altheader = "";
  my $pvar = &trans("${variation}-pretty-name");

  my $youcanget = &trans('youcanget');
  my $active = $retired
			?&trans("retired rankings",$nitems,$pvar)
			:&trans("active rankings",$nitems,$pvar);
  if(lc($ccode) eq "select country") { $ ccode = ""; }
	if($ccode eq "") { $ccode = "World Wide"; }
	$ccode = &trans($ccode);
  my $qvarname = $dbh->quote($variation);
  my $qvar = "select max_players from variation where variation.name=$qvarname";
  my $vquery = &query($dbh,$qvar);
  my ($nplayers) = &nextArrayRow($vquery);
  &finishQuery($vquery);
 
  my $normalstr = &trans(($retired==0)?"Player Rankings":"Retired Player Rankings",$ccode,$pvar);
  my $masterstr = &trans(($retired==0)?"Master Player Rankings":"Retired Master Player Rankings",$ccode,$pvar);
  my $turnstr =   &trans(($retired==0)?"#1 Turn Based #2 Player Rankings" : "Retired Turn Based Player Rankings",$ccode,$pvar);

  if($header eq "") 
  { 
     $header = ($mode eq 'turnbased') ? $turnstr : ($mode eq 'master') ? $masterstr : $normalstr;
   };
 
  my $link1 = ($mode eq '') ? "" : "<td>" . &get_link($vname,$country,$normalstr,$myname,'',$retired,$order) . "</td>";
  my $link2 = ($mode eq 'master') ? "" : "<td>" . &get_link($vname,$country,$masterstr,$myname,'master',$retired,$order) . "</td>";
  my $link3 = ($mode eq 'turnbased') ? "" : "<td>" . &get_link($vname,$country,$turnstr,$myname,'turnbased',$retired,$order) . "</td>";
  my $wmode = ($mode eq 'master') ? " Master" : ($mode eq 'turnbased') ? " Turn Based" : "";
  my $ladder = "<td>" . &getLadderLink($vname,&trans("#1$wmode Ranking Ladder",$pvar),$myname,$mode) . "</td>";
  my $dbmes=&trans("#1 game database",$pvar);
  my $dblink = "<td><a href='javascript:link(\"/cgi-bin/player_analysis.cgi?game=$variation\",0)'>$dbmes</a></td>";
   #http://boardspace.net/cgi-bin/player_analysis.cgi?game=hive
  #http://boardspace.net/hive/hive-viewer.shtml
&standard_header();
  
print <<Header;
<html>
<head>
    <title>$header</title>
</head>

<center>
<table width=850>
<tr>
<td align="left" valign="top">

<H2><center>$header</center></H2>
Header

&honeypot();
print "<table><tr><td width=50%>";
&print_country_form($country);
print "$active<p>$youcanget\n";

print "</td><td>";
&show_activity_table($dbh,0,$months,&timezoneCookie(),$variation);

print "</td></tr></table>\n";
print <<Header
<p>
<table border=1 cellpadding=3><tr>$link2$ladder$link1$link2$link3$dblink</tr></table>
<p>
<HR SIZE="4" WIDTH="40%">
Header

}
sub get_link()
{	my ($variation,$ccode,$pretty,$myname,$mode,$retired,$order) = @_;
	my $script = $ENV{"SCRIPT_NAME"};
	my $aux = "?game=$variation";
	if($myname) { $aux .= "&myname=$myname"; }
	if($order) { $aux .= "&order-by=$order"; }
	if($mode) { $aux .= "&mode=$mode"; }
	if($retired) { $aux .= "&retired=$retired"; }
	if($ccode) { $aux .= "&country=$ccode"; }
	return("<a href=\"javascript:link('$script$aux',0)\">$pretty</a>");
}
sub getLadderLink()
{	my ($variation,$pretty,$myname,$mode) = @_;
	my $script = '/cgi-bin/boardspace_ladder.cgi';
	my $aux = "?game=$variation&mode=$mode";
	if($myname) { $aux .= "&myname=$myname"; }
	return("<a href=\"javascript:link('$script$aux',0)\">$pretty</a>");
}
%'order_keys = 
	('GroupUp' => 'advocate asc',
	 'GroupDown' => 'advocate desc',
	 'RankUp' => 'value asc',
	 'RankDown' => 'value desc',
	 'PlayerUp' => 'player_name asc',
	 'PlayerDown' => 'player_name desc',
	 'RankingUp' => 'value asc',
	 'RankingDown' => 'value desc',
	 'LadderDown' => 'ladder_level asc',
	 'LadderUp' => 'ladder_level desc',
	 'LastPlayedUp' => 'last_played asc',
	 'LastPlayedDown' => 'last_played desc',
	 'CountryUp' => 'country asc,value desc',
	 'CountryDown' => 'country desc,value desc',
	 'MaxRankUp' => 'max_rank asc',
	 'MaxRankDown' => 'max_rank desc',
	 'WonUp' => 'games_won asc',
	 'WonDown' => 'games_won desc',
	 'LostUp' => 'games_lost asc',
	 'LostDown' => 'games_lost desc',
	 'PlayedUp' => 'played asc',
	 'PlayedDown' => 'played desc',
	 'WinpUp' => 'percent asc',
	 'WinpDown' => 'percent desc',

#	  my $MaxRank = &trans('Max Ranking');
#	  my $played = &trans('Games Played');
#	  my $Won = &trans('Won');
#	  my $Lost = &trans('Lost');
	 'name' => "player_name asc", 
	 'namedown' => "player_name desc",
	 'rank' => "value desc",
	 'rankup' => 'value asc'

	);

sub update_rankings 
{ 		
  my $myname=param('myname');

 &bless_parameter_length($myname,20);

  my $realplayers = param('realplayers');
  my $nitems=param('n');
   &bless_parameter_length($nitems,10);
  my $mode = param('mode');
   &bless_parameter_length($mode,10);
  my $order=param('order-by');
   &bless_parameter_length($order,10);
  my $retired = param('retired');
   &bless_parameter_length($retired,10);
	my $country = param('country');
   &bless_parameter_length($country,30);
   $country = &validate_country($country);
	my $country_clause="";
  my $months=param('months');
    &bless_parameter_length($months,5);
 my $vname = &param('game');
    &bless_parameter_length($vname,20);
  my $useemail = param('email');
	my $dbh = &connect();              # connect to local mysqld
	if($dbh)
	{
  if(&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0)
  {
  &readtrans_db($dbh);
  my $variation = &gamecode_to_gamename($dbh,$vname ? $vname : "Z");
  my $t = &current_time_string();
  if(lc($country) eq "select country") { $country = ""; }
  if($country)
	{ 
	  if($country eq 'y')
		{ if(!($myname eq ""))
			{my $qname = $dbh->quote($myname);
		   my $sth = &query($dbh,"SELECT country from players where player_name=$qname");
		   if(&numRows($sth)>0) { ($country) = $sth->fetchrow();}
			}else
			{$country="";
			}
		}
		my $qcountry = $dbh->quote($country);
		$country_clause = " AND country=$qcountry";
	}
  if($months=='') { $months=$::retire_months; }
	if($nitems=='') { $nitems=100; }
	my $order_key = $::order_keys{$order};
	if($order_key eq "") 
		{ $order_key="value desc"; 
		}
	my $rtest = '>';
	my $retired_test = "AND status='ok'";
	if($retired) { $rtest='<='; $retired_test="OR status='retired' "; }
        my $sunset = time()-60*60*24*30*$months; 
        my $lp = "ranking.last_played";
	my $real_clause = $realplayers ? " AND (is_robot is NULL) " : " or (is_robot='y') ";
    my $qvar = $dbh->quote($variation);
	my $mstat = (($mode eq 'turnbased') ? 'turnbased' : ($mode eq 'master') ? 'yes' : 'no');
	my $ladder_clause =  ""; #" AND ladder_level is not null "  this has the effect of excluding players who only play robots
	my $query = "SELECT player_name, e_mail, $lp, value, country, "
				. "max_rank,ranking.games_played as played, "
				. "advocate, "
				. "if(ranking.games_played>20,(100.5+(100*games_won)/(games_won+games_lost)),'') as percent, "
				. "ladder_level"
				. " FROM players left join ranking "
				. " on players.uid=ranking.uid "
         . " WHERE ranking.variation=$qvar AND (value > 0) "
				. " AND ranking.is_master='$mstat'"
				. $ladder_clause
				. " AND (($lp $rtest $sunset $retired_test $country_clause) "
				. $real_clause . " ) "
				. " ORDER BY $order_key";
	#print "Q: $query";
  my $sth =&query($dbh,$query );
	#print "Q: $query\n";
	&rank_header($dbh,$nitems,$retired,$mode,$country,$variation,$myname,$order,$vname,$months);

	print "<TABLE BORDER=0 CELLPADDING=2 WIDTH=\"100%\">";
	{ my $Rank = &trans('Rank');
	  my $Player = &trans('Player');
	  my $Ranking = &trans('Ranking');
	  my $Group = &trans('Group');
	  my $winp = &trans('Win%');
	  my $lastp = &trans('Last Played');
	  my $Country = &trans('Country');
	  my $MaxRank = &trans('Max Ranking');
	  my $Ladder = &trans('Ladder');
	  my $played = &trans('Games Played');
	  $Group = &get_link($vname,$country,$Group,$myname,$mode,$retired,
				(($order eq 'GroupDown')?'GroupUp':'GroupDown'));
				
	  $Ranking = &get_link($vname,$country,$Ranking,$myname,$mode,$retired,
				(($order eq 'RankingDown')?'RankingUp':'RankingDown'));
				
	  $Ladder = &get_link($vname,$country,$Ladder,$myname,$mode,$retired,
				(($order eq 'LadderDown')?'LadderUp':'LadderDown'));
				
	  $Player = &get_link($vname,$country,$Player,$myname,$mode,$retired,
				(($order eq 'PlayerUp')?'PlayerDown':'PlayerUp'));

	  $lastp = &get_link($vname,$country,$lastp,$myname,$mode,$retired,
				(($order eq 'LastPlayedDown')?'LastPlayedUp':'LastPlayedDown'));

	  $Country = &get_link($vname,$country,$Country,$myname,$mode,$retired,
				(($order eq 'CountryDown')?'CountryUp':'CountryDown'));

	  $MaxRank = &get_link($vname,$country,$MaxRank,$myname,$mode,$retired,
				(($order eq 'MaxRankDown')?'MaxRankUp':'MaxRankDown'));

	  $played = &get_link($vname,$country,$played,$myname,$mode,$retired,
				(($order eq 'PlayedDown')?'PlayedUp':'PlayedDown'));

	  $winp = &get_link($vname,$country,$winp,$myname,$mode,$retired,
				(($order eq 'WinpDown')?'WinpUp':'WinpDown'));

# my ($variation,$pretty,$myname,$mode,$retired,$order) = @_;
  	  print "<TR>"
		. "<TD align=left><b>$Rank</b></TD>"
	   	. "<TD align=left><b>$Player</b></TD>"
    		. "<TD align=left><b>$Ranking</b></TD>"    
    		. "<TD align=left><b>$Ladder</b></TD>"
     		. "<TD align=left><b>$Group</b></TD>" #advocate etc
    		. "<TD><b>$winp</b></td>"
		. "<TD><b>$lastp</b></TD>"
    		. "<TD align=left><b>$Country</b></TD>"
		. "<TD align=center><b>$MaxRank</b></TD>"
    		. "<td><b>$played</b></td>";
   	}

    if($useemail)
    { print "<td><b>email</b></td>";
    }
       print "</TR>\n";
	my $n = 0;
	my $showall=0;
	my @showback;
	my $imseen = ($myname eq "");
	my $numr = &numRows($sth);

  if ($numr>10000) { __dm("mysql returned $numr, an unreasonable number of rows"); return; }

	while ($numr>0 && (($n<$nitems) || !$imseen || ($showall>0)))
		{
		$n++;
		$numr--;
		
		my ($curname, $email , $pdate, $ranking, $country, $max_rank,$played,$group,$pcent, $ladder) = &nextArrayRow($sth);
		
		my $bold = "";
		my $nobold="";
		my $daysago = &timeago($pdate);
		
		if(lc($curname) eq lc($myname)) 
				{ $bold="<b>"; $nobold="</b>";
					$imseen = 1;
				  $showall = 5;
				  if($n>$nitems)
					{
					if($showback[0] ne "") { print "<tr><td colspan=3><br><b>... you are here ...</b><p></td></tr>";}
				  print "$showback[1]";
				  print "$showback[2]";
				  print "$showback[3]";
				  print "$showback[4]";
					}
				}
		my $cflag = &flag_image(${country});
		$country =~ s/ /&nbsp;/g;
		if($pcent) {  $pcent = int($pcent)-100; };
		my $group_description;
		if($group)
		{ $group_description = "title='" . &trans("${group}-description") . "' ";
		  $group = &trans("$group-group");
		}

		my $line = "<TR>"
				. "<TD ALIGN=center>$n</TD>"
				. "<TD ALIGN=left><A HREF=\"javascript:editlink('$curname',0)\">$bold$curname$nobold</A></TD>"
				. "<TD ALIGN=left>$ranking</TD>"
				. "<td align=left>$ladder</td>"
				. "<td $group_description>$group</td>"
				. "<td>$pcent</td>"
				. "<TD align=left>$daysago</TD>"
				. "<TD ALIGN=center><img width=33 height=22 alt=\"$country\" src=\"$cflag\"></TD>"
				. "<TD align=center>$max_rank</td>"
				. "<td align=center>$played</td>";
		if($useemail) { $line .= "<td>$email</td>"; }
		$line .= "</TR>\n";
		if(($n<=$nitems) || ($bold ne "") || ($showall>0))
			{ $showall--;
		    print $line;
			} else
			{ $showback[0]=$showback[1];
			  $showback[1]=$showback[2];
			  $showback[2]=$showback[3];
			  $showback[3]=$showback[4];
			  $showback[4]=$line;
			}
	}
	print "</TABLE></P>";
	&finishQuery($sth);
    &standard_footer();
	}
	&disconnect($dbh);
}}

print header;
&init();
&update_rankings();
	
