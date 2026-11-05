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

#$database='test';

sub init 
{
   $| = 1;                         # force writes
}

sub rank_header()
{	my ($nitems,$retired,$mode,$ccode)=@_;
	my $country = $ccode;
  my $header=param('header');
  my $youcanget = &trans('youcanget');
	my $active = $retired
			?&trans("retired rankings",$nitems)
			:&trans("active rankings",$nitems);
  if(lc($ccode) eq "select country") { $ ccode = ""; }
	if($ccode eq "") { $ccode = "World Wide"; }
	$ccode = &trans($ccode);
	$ccode =~ s/ /&nbsp;/g;
	
  if($header eq "") 
  {   my $str = 
    ($retired==0)
   			? (($mode eq "master") 
					? "Master Player Rankings"
					: "Player Rankings")
			: (($mode eq "master") 
					? "Retired Master Player Rankings"
					: "Retired Player Rankings");
	$header = &trans($str,$ccode);
  };

&standard_header();
  
print <<Header;
<html>
<head>
    <title>$header</title>
</head>

<center>

<table width=650>
<tr>
<td align="left" valign="top">

<H2>$header</H2>
Header

&print_country_form($country);
	
print <<Header
$active
<p>$youcanget
<p>
<HR SIZE="4" WIDTH="40%">
Header

}

sub update_rankings 
{ 			
  my $myname=param('myname');
  my $realplayers = param('realplayers');
	my $nitems=param('n');
	my $mode = param('mode');
  my $order=param('order-by');
  my $retired = param('retired');
	my $country = param('country');
	my $country_clause="";
  my $months=param('months');
  my $t = &current_time_string();

  my $useemail = param('email');
	my $dbh = &connect();              # connect to local mysqld

	if($dbh)
	{
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
  if($months=='') { $months=$'retire_months; }
	if($nitems=='') { $nitems=100; }
	if($order eq "") 
		{ $order="value"; 
		}
	my $rtest = '>';
	my $retired_test = "AND status='ok'";
	if($retired) { $rtest='<='; $retired_test="OR status='retired' "; }
        my $sunset = time()-60*60*24*30*$months; 
        my $lp = "ranking.last_played";
	my $real_clause = $realplayers ? " AND (is_robot is NULL) " : " or (is_robot='y') ";
	my $query = "SELECT player_name, e_mail, $lp, value, country, "
				. "max_rank,games_won,games_lost "
				. " FROM players left join ranking on players.uid=ranking.uid "
         . " WHERE ranking.variation='zertz' AND (value > 0) "
				. " AND (($lp $rtest $sunset $retired_test) "
				. $country_clause . " ) " . $real_clause
				. " ORDER BY $order DESC";
  my $sth =&query($dbh,$query );
	print "Q: $query\n";
	&rank_header($nitems,$retired,$mode,$country);

	print "<TABLE BORDER=0 CELLPADDING=2 WIDTH=\"100%\">";
	{ my $Rank = &trans('Rank');
	  my $Player = &trans('Player');
	  my $Ranking = &trans('Ranking');
	  my $winp = &trans('Win%');
	  my $lastp = &trans('Last played');
	  my $Country = &trans('Country');
	  my $MaxRank = &trans('Max Ranking');
	  my $played = &trans('Games played');
	  my $Won = &trans('Won');
	  my $Lost = &trans('Lost');

    		print "<TR><td></td><TD><b><P align=left>$Rank</b></TD><TD><p align=left><b>$Player</b></TD>"
    				. "<TD><p align=left><b>$Ranking</b></TD><td><b>$winp</b></td><TD><b>$lastp</b></td>"
    				. "<TD><p align=left><b>$Country</b></TD><TD align=center><b>$MaxRank</b></td>"
    				. "<td><b>$played</b></td><td align=right><b>$Won</b></td><td align=right><b>$Lost</b></td>"
    				. "<td align=right></td>";
    	}

    if($useemail)
    { print "<td><b>email</b></td>";
    }
       print "</td><TR>";
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
		
		my ($curname, $email , $pdate, $ranking, $country, $max_rank,$rgames_won,$rgames_lost) = $sth->fetchrow();
		
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
		my $played = $rgames_won+$rgames_lost;
		my $percent = ($played>20)?int(0.5+100.0*($rgames_won)/$played):"";
		my $line = "<TR><td></td><TD>$n</TD><TD><P ALIGN=left><A HREF=\"/cgi-bin/edit.cgi?pname=$curname\">$bold$curname$nobold</A></TD>"
					. "<TD><P ALIGN=left>$ranking</TD><td>$percent</td><TD align=left>$daysago</TD>"
					. "<TD><P ALIGN=center><img width=33 height=22 alt=\"$country\" src=\"$cflag\"></TD><TD align=center>$max_rank</td><td align=center>$played</td>"
					. "<td align=right>$rgames_won</td><td align=right>$rgames_lost</td>";
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
	&disconnect($dbh);
}}

print header;
&init();
&update_rankings();
&standard_footer();
	