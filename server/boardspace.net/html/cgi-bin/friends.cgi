#!/usr/bin/perl
#
use CGI qw(:standard);
use Mysql;
use Debug;
use Time::Local;
use strict;
require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/getLocation.pl";
require "tlib/favorite-games.pl";

sub hstring()
{my ($center) = @_;
 my $tit = $center?
    &trans("Find Players who may be near #1 in real life",$center)
    : &trans("Find nearby players");
 return($tit);
}

sub friend_header 
{ my ($center,$months,$dist) = @_;
  &standard_header();
  my $tit = &hstring($center);
  my $language = &select_language(&param('language'));
  my $mbox = &trans("List players who have played within the last #1 Months",
		    "<input type=text size=3 value=$months name=months>");
  my $lbox = &trans("and are located within approximately #1 Kilometers.",
		    "<input type=text size=5 value=$dist name=klicks>");
  my $nbox = &trans("Type in a player's nickname.");
  my $bbox = &trans("Find nearby players");
  my $expbox = &trans("map explanation message #1 url",
		      "<a href=/$language/playermap.shtml>Player World Map</a>");

	print <<Header;
<html>
<head>
<title>$tit</title>
</head>

<p>
<blockquote>
<form action=$ENV{'SCRIPT_NAME'}>
<br><FONT FACE="Futura Bk BT"><H1>$tit</H1></font>
<input type=hidden value=$language name=language>
<input type=text value="$center" name=pname> &nbsp $nbox
<br>
$mbox,
<br>
$lbox
<br><br><br>
<input type=submit value="$bbox" name=doit><br>
</form>
<p>
<font -1>
$expbox
</font><br><br>&nbsp
<p>
</blockquote>

Header

}
sub printRow()
{ my @args = @_;
	print "<tr>";
	while($#args>=0)
		{ my $a = shift(@args);
		  if($#args < 0) { print "<td align=left>$a</td>"; }
		  else { print "<td align=right>$a</td>"; }
		}
	print "</tr>\n";
}
  
sub show_friends()
{ my ($pname,$months,$klicks) = @_;
	my $dbh = &connect();
    if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
	{
	&readtrans_db($dbh);
	&friend_header($pname,$months,$klicks);
	if($pname)
	{
	my $mylat;
	my $mylon;
	my $qpname = $dbh->quote($pname);
	my $sunset = time()-60*60*24*30*$months; 
	my $degrees = $klicks/(1.6*50);
	my $p2mes = &trans("Players who may be near #1 in real life",$pname);
	print "<center><h2>$p2mes</h2>";
  
	if($pname=~/[0-9]*\.[0-9]*\.[0-9]*\.[0-9]*/)	# looks like an IP address
		{
		my %result = &getLocation($pname,0);
		my @re = %result;
		$mylat = $result{'latitude'};
		$mylon = $result{'longitude'};
		
		my $City = $result{'city'};
		my $Region = $result{'region'};
		my $Country = $result{'country'};
		my $myplace = "$Region, $Country";
		if(!($City eq $Region)) { $myplace = "$City, $myplace"; }
		print "<center>we think $pname is located near $myplace</center>\n";
		}else
		{
		my $sth = &query($dbh,"SELECT latitude,logitude FROM players WHERE player_name=$qpname");
		my $nr = &numRows($sth);
		($mylat,$mylon) = &nextArrayRow($sth);
		&finishQuery($sth);
      		}
				
	if($mylat && $mylon)
		{
		 my $sth = &query($dbh,"SELECT latitude,logitude,player_name,full_name,e_mail,last_played,uid FROM players "
														. "where (abs(logitude-$mylon)+abs(latitude-$mylat))<$degrees "
																	. " and player_name!=$qpname"
																	. " and last_played > $sunset "
														. "order by (abs(logitude-$mylon)+abs(latitude-$mylat))");
		  my $numr = &numRows($sth);
		 my $apmessage = &trans("#1 active players have been spotted nearby",
					"<b>$numr</b>");
		print "$apmessage<p>";
		  if($numr>0)
			{ print "<table>" ;
			  my $pltrans = &trans("Player");
			  my $rntrans = &trans("Real Name");
			  my $emtrans = &trans("E-Mail");
			  my $kmtrans = &trans("Kilometers");
			  my $lptrans = &trans("Last Played");
			  my $favs = &trans("Most Played Games");
			  &printRow("<b>$pltrans</b>" , "<b>$rntrans</b>" ,
				    "<b><center>$emtrans</center></b>" ,
				    "<b>&nbsp;$kmtrans&nbsp;</b>","<b> $lptrans</b>","&nbsp;<b>$favs</b>");
				while($numr>0)
			{ my ($lat,$lon,$who,$full,$em,$last,$uid) = &nextArrayRow($sth);
			my $lodis = $lon-$mylon;
			my $ladis = $lat-$mylat;
			my $dis = sqrt($ladis*$ladis+$lodis*$lodis);	#degrees away
			my $kdis = int($dis*60*1.6);			#approx 60 miles per degree, 1.6 klicks / mile
			my $ago = &timeago($last);
			my $wholink = "<a href=\"javascript:editlink('$who',0)\">$who</a>";
			my $elink = &obfuscateHTML("<a href=\"mailto:$em\">$em</a>");
			my $fav = &favorite_games($dbh,$uid,$months*30,3);
			$numr--;
			&printRow("$wholink",&encode_entities(&utfDecode($full)),"$elink","$kdis",$ago,"&nbsp;$fav");
			}
			print "</table></center>";
			}
	&finishQuery($sth);
		}
	my $wherem = &trans("#1 was last located at latitude #2, longitude #3",
			    $pname,$mylat, $mylon);

	print "<p>";
	print "<br>$wherem;<br>";
	print &geobutton(730);	# 938 for testing also prints some stuff
	}
	&standard_footer();
	}
	&disconnect($dbh);
}


print header;
param();
my ($pname) = param('pname');
 $pname = &despace(substr($pname,0,25));
my $months = param('months');
my $klicks = param('klicks');
if($klicks <=0 ) { $klicks = 100; }
if($months<=0) { $months = $'retire_months; }
&show_friends($pname,$months,$klicks);
