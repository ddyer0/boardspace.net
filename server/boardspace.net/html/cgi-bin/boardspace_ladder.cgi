#!/usr/bin/perl 
#
# generate regular, master and doubles rankings on demand.
#
#
# optional parameters:
#
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;

use strict;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/ladder.pl";

#$database='test';

sub init 
{
   $| = 1;                         # force writes
}

sub rank_header()
{
  my ($dbh,$variation,$myname,$vname)=@_;
  my $pvar = &trans("${variation}-pretty-name");
  my $header=param('header');
  my $altheader = &trans("Player Rankings","",$pvar);
  my $stdheader = &trans("Master Player Rankings","",$pvar);

  my $youcanget = &trans('youcanget');
	
  if($header eq "") 
  {  
	 $header = &trans("#1 Ranking Ladder",$pvar);
  };
  my $link = &get_link($vname,$stdheader,$myname,"master");	
  my $link1 = &get_link($vname,$altheader,$myname,'');	
  my $dbmes=&trans("#1 game results database",$pvar);
  my $dblink = "<a href='javascript:link(\"/cgi-bin/player_analysis.cgi?game=$variation\",0)'>$dbmes</a>";
  my $vlink = &gamecode_to_gameviewer($dbh,$vname);
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

print <<Header
<p>
<table border=1 cellpadding=3><tr><td>$link1</td><td>$link</td><td>$dblink</td></tr></table>
<p>
<HR SIZE="4" WIDTH="40%">
Header

}
sub get_link()
{	my ($variation,$pretty,$myname,$mode) = @_;
	my $script = '/cgi-bin/boardspace_rankings.cgi';
	my $aux = "?game=$variation";
	if($myname) { $aux .= "&myname=$myname"; }
	if($mode) { $aux .= "&mode=$mode"; }
	return("<a href=\"javascript:link('$script$aux',0)\">$pretty</a>");
}
 

sub update_rankings 
{ 		
	my $myname=param('myname');
    my $vname = &param('game');

    &bless_parameter_length($myname,20);
    &bless_parameter_length($vname,20);


    my $dbh = &connect();              # connect to local mysqld
	if($dbh)
	{
	&readtrans_db($dbh);
  
	if(&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0)
	{
	my $variation = &gamecode_to_gamename($dbh,$vname ? $vname : "Z");
	my $t = &current_time_string();
    my $qvar = $dbh->quote($variation);
    
	&rank_header($dbh,$variation,$myname,$vname);
	
	print "<b>";
	print &trans("howtheladderworks");
	print "</b>";
	my $maxlvl = &max_ladder_level($dbh,'ranking',$dbh->quote($variation));
	my $currlvl = 0;
	print "<table><p>";
	print "<tr><td><b>" . &trans("Ladder Level") . "</b></td>" . "<td align=center><b>" . &trans("Players") . "</b></td></tr>\n";
	while($currlvl++ < $maxlvl)
	{
	print "<tr>\n";
	my $qlvl = $dbh->quote($currlvl);
	my $qvar = $dbh->quote($variation);
	my $q = "select ranking.uid,player_name,ladder_level,ladder_order from ranking left join players"
		. " on players.uid=ranking.uid where variation=$qvar and ladder_level=$qlvl"
		. " order by ladder_order";
	#print "Q: $q";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	my $co = (($currlvl&1)==0) ? "#b0d0ff" : "#CCCCFF";
	print "<td>Level $currlvl</td>";
	print "<td align=center><table><tr><td  bgcolor=$co>";
	while($nr-- > 0)
	{	my ($uid,$pname,$ladder_level,$ladder_order) = &nextArrayRow($sth);
		print " <A HREF=\"javascript:editlink('$pname',0)\">$pname</a> ";
	}
	&finishQuery($sth);
	print "</td></tr></table></td>";
	
	print "</tr>";
	}
	print "</table>";
	}
	&disconnect($dbh);
	
	
  &honeypot();
 
}}

print header;
&init();
&update_rankings();
&standard_footer();
	
