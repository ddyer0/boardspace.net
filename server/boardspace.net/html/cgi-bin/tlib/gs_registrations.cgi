#!/usr/bin/perl 
#
#
# optional parameters:
# nitems=100                      =n to show top n players
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
require "../include.pl";
require "common.pl";
require "gs_db.pl";
use strict;

sub init()
{
        $| = 1;                         # force writes
}

sub rank_header()
{ my ($header,$nitems)=@_;
	my $t = &current_time_string();
  &standard_header();

print <<Header;

<td align="left" valign="top">

<H2>$header</H2>

The following table shows the $nitems most recently registered players
<p>You can get more
information about a player by clicking on their Player Name.
<p>Last updated: <i>$t</i>
<p>
<HR SIZE="4" WIDTH="40%">
Header
}


sub show_registrations()
{ my ($dbh,$nrows)=@_;
	my $sth = &query($dbh, "SELECT player_name,e_mail,e_mail_bounce,note_sent,country,date_joined,games_played,last_played,status,num_logon FROM players ORDER BY date_joined DESC" );
	my ($nitems) = &numRows($sth);
	if($nrows<$nitems) { $nitems=$nrows; }
	print "<TABLE BORDER=0 CELLPADDING=2 WIDTH=\"100%\">";
	print "<TR><TD><p align=left><b>Player Name</b></TD>";
	print "<TD><p align=left><b>Email</b></TD>";
	print "<TD><p align=left ><b>Country</b></TD>";
	print "<td><b>Registered on</b></td>";
    print "<td><b>status</b></td>";
	print "<td># Logon</td>";
	print "<td>Played</td>";
	print "<TR>";
	my ($n) = 0;
	while ($n < $nitems)
		{my ($player_name,$email,$bounce,$note_sent,$country,$date_joined,$games_played,$last_played,$status,$num_logon) = &nextArrayRow($sth);
		 my $finished = $games_played;
		 if(($finished == 0) && ($last_played!=0)) { $finished = "U"; }
		 my $pdate = &date_string($date_joined);
	  $n++;
		print "<TR><TD><P ALIGN=left><A HREF=\"javascript:editlink('$player_name',0)\">$player_name</A></TD>";
		my $hemail = &obfuscateHTML($email);
		my $bb = ($bounce>0) ? " <b>(bouncing)</b>" : "";
		my $nn = ($note_sent) ? "<b>(note)</n>" : "";
		print "<TD><P ALIGN=left>$hemail$bb$nn</TD>";
		print "<TD><P ALIGN=left>$country</TD>";
		print "<TD>$pdate</TD>";
    print "<td>$status</td>";
	  print "<td align=right>$num_logon</td>";
	  print "<td align=right>$finished</td>";
		print "</TR>\n";
	}
	print "</TABLE></P>";
	&finishQuery($sth);
	&standard_footer();

}
print header;
init();
param();
  my ($dbh) = &connect();
  if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {
  &readtrans_db($dbh);
  my $nitems=param('n');
  if($nitems=='') { $nitems=100; }
  my $header=param('header');
  if($header eq "") {  $header="World-Wide Registrations"; };
	&rank_header($header,$nitems);

  &show_registrations($dbh,$nitems);
  &disconnect($dbh);
  }