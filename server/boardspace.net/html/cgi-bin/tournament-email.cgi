#!/usr/bin/perl
#
# send a message to a tournament player, to be received when he logs in
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Mysql;
use Debug;
use Socket;
use strict;
use URI::Escape;
use HTML::Entities;
use CGI::Cookie;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
use Crypt::Tea;

sub init {
	$| = 1;				# force writes
	__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
}


sub print_header 
{	&standard_header();

	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>Send a Message to a Tournament's players</TITLE>

Head_end
}

sub active_tournaments()
{	my ($dbh) = @_;
	my @val;
	my $q = "SELECT uid,description,teams from tournament where (status='signup' or status='active')";
	my $sth = &query($dbh,$q);
	my $numrows = &numRows($sth);
	while($numrows-- > 0)
	{
	my ($id,$desc,$team) = &nextArrayRow($sth);
	push @val,$id;
	push @val,$desc;
	push @val,$team
	}
	&finishQuery($sth);
	return(@val);
}

sub active_teams()
{	my ($dbh,$tid) = @_;
	my @val;
	my $qtid = $dbh->quote($tid);
	my $q = "SELECT uid,name from team where tournamentid=$qtid";
	my $sth = &query($dbh,$q);
	my $numrows = &numRows($sth);
	while($numrows-- > 0)
	{
	my ($uid,$name) = &nextArrayRow($sth);
	push @val,$uid;
	push @val,$name;
	}
	&finishQuery($sth);
	return(@val);
}

sub tournament_variation()
{	my ($dbh,$tid) = @_;
	my $qtid = $dbh->quote($tid);
	my $q = "select variation from tournament where uid=$qtid";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	my $val = "";
	if($num==1) { ($val) = &nextArrayRow($sth); }
	&finishQuery($sth);
	return($val);
}


sub findpname()
{ my ($dbh,$plname) = @_;
	my $slashname = $dbh->quote($plname);
	my $sth = &query($dbh,"SELECT * FROM players WHERE player_name=$slashname");
	my $n = &numRows($sth);
	&finishQuery($sth);
  return($n>=1);
}

sub logon 
{
  my ($dbh,$pname,$passwd) = @_;
  my $myaddr = $ENV{'REMOTE_ADDR'};
  my $bannercookie = &bannerCookie();
  my $slashpname = $dbh->quote($pname);
  my $slashpwd = $dbh->quote($passwd);
  my $q = "SELECT * FROM players WHERE (is_tournament_manager='yes') and player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid))";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  &finishQuery($sth);
  if(($n == 1)
		&& &allow_ip_login($dbh,$myaddr)
		&& &allow_ip_login($dbh,$bannercookie))
	{	return(1);
	}
  else
	{
        &note_failed_login($dbh,$myaddr,"IP: Send Message as $pname");
	&note_failed_login($dbh,$bannercookie,"CK: Send Message as $pname");
	}
	return 0;
}

sub printOption()
  {  my ($sel,$val,$name) = @_;
     my $issel == ($val eq $sel) ? "selected" : "";
     print "<option value='$val' $issel>$name</option>\n";
  }

sub send_email()
{  my ($toemail,$toname,$to,$from,$date,$subj,$body) = @_;
   my $editlink = edit_user_info_message($toname);
   $body =~ s/\r\n/\n/g;	# trim crlf to just lf
   my $msg = "$body\n\n$editlink\n";

   &send_mail_to($toname,$from,$toemail,$subj,$msg);

}

# --------------------------------------------
&init();

print header;

__d( "sendmessage...");


sub do_gs_edit()
{
param();
  my $dbh = &connect();
  if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {
  &readtrans_db($dbh);
  my $unbounce=0;
  my $test = param('test');
  my $cctd = param('cctd');
	my $fromname = param('fromname');
	my $passwd = param('passwd');
	my $subject = param('subject');
	my $message = param('message');
	my $approve = param('approve');
	my $deliver = param('deliver');
	my $method = param('method');
	my $body = param('body');
	my $date = param('date');
	my $done = param('done');
	my $escaped = param('escaped');
	my $smpass = param('smpass');
	my @tournaments = &active_tournaments($dbh);
  if($escaped) 
	{ 
	 $body = uri_unescape($body);
	 $subject = uri_unescape($subject); 
	}

	if($date eq "") { $date = &date_string(time()); }
	

	&print_header();

	
	if($approve)
  	{	if(!&logon($dbh,$fromname,$passwd)) 
  		{ print "<p><b>Your name or password is incorrect</b><p>"; 
  		  $approve = 0;
  		  $deliver = 0;
  		}
  	}

	my $edit = !($deliver || $approve );
	my $ttype = $edit ? "text" : "hidden";
	my $button = $deliver ? "Send another Message"
									: $approve ? "Ok, send the message"
									: "Preview the Message before sending";
	my $bname = $deliver ? "start"
							: $approve ? "deliver"
							: "approve";
	if ($deliver)
  	{ 
  		my $qsubj = $dbh->quote($subject);
  		my $qmsg = $dbh->quote($body);
  		my $qfrom = $dbh->quote($fromname);
  		my $qdate = $dbh->quote($date);
  		my $qbody = $dbh->quote($body);
 
      { # some kind of email delivery
        my $addr="";
        my $q = "select player_name,e_mail,e_mail_bounce from players where  ";

		if (substr($method,0,11) eq "tournament-")
		{$unbounce=1;
		my ($tt,$tid,$team) = split '-',$method;
		my $qtid = $dbh->quote($tid);
		my $qteam = $dbh->quote($team);
		my $tv = $dbh->quote(&tournament_variation($dbh,$tid));
		my $teamsel = ($team eq 'x') ? "" : "and team=$qteam";
		$q = "SELECT player_name,e_mail,e_mail_bounce from participant left join players "
				. " on participant.pid=players.uid"
				. " where tid=$qtid $teamsel";
	#print "Q: $q<p>";
		}
        else { &die("No delivery code for $method<br>"); }
  
         my $sth = &query($dbh,$q);
         my $nr = &numRows($sth);
         my $te = $test ? " test " : "";
         print "<blockquote>$nr rows$te\n";
         while ($nr-- >= 0)
           { my ($na,$v,$bounce);
		     if($nr<0)
			 { $na = "gamemaster";
			   $v = $'supervisor_email
			 } else
			 { ($na,$v,$bounce) = &nextArrayRow($sth); 
			 }
             my $addr = "\"$na\" <$v>";
             my $addr1 = encode_entities($addr);
	     my $ub = ($bounce>0 && $unbounce) ? " and unbouncing" : "";
             print "<p>Sending$ub to $addr1";
             if(!$test)
	     { &send_email($v,$na,$addr,$'announce_email,$date,$subject,$body); 
	       if($bounce>0 && $unbounce)
	       { my $qname = $dbh->quote($na);
		 &commandQuery($dbh,"update players set e_mail_bounce='0',e_mail_bounce_date=null"
			       . " where player_name=$qname");
	       }
	      }
         }
		  &finishQuery($sth);
          print "</blockquote>";
          }
      }
 	else
  	{

	if(!$edit)
	{

	 print "<FONT FACE=\"Futura Bk BT\"><H1>A Message For You</H1></font>\n";
 	 print "<title>A Message for You</title>\n";

	}else
	{
  print "<title>Send a Message to a Tournament's players</title>\n";
	}

  print "<form action=$ENV{'REQUEST_URI'} method=POST>\n";
	print "<table><tr><td>";
	print "<b>From:</b></td><td>" ;
	print "<input type=$ttype name=fromname value='$fromname' size=12>";
  if(!$edit)
  	{ print "$fromname";
  	}
  print "</td></tr>\n";
  
  if($edit)
	{
		print "<tr><td><b>your player password:</b></td><td><input type=password name=passwd value='$passwd' SIZE=20 MAXLENGTH=25>(not sent to the recepient)</td></tr>";
		print "<tr><td>note: this requires the tournament manager priv</td></tr>\n";
	}
	else
  { print "<input type=hidden name=passwd value=$passwd SIZE=20 MAXLENGTH=25>\n";
    print "<input type=hidden name=smpass value=$smpass SIZE=20 MAXLENGTH=25>\n";
  }
	
  print "<tr><td><b>To:</b></td><td>";

  if ($edit)
      {  print "<select name=method>\n";


	  my @tes = @tournaments;
	  while( $#tes > 0)
	  {
           my $teams = pop @tes;
	   my $tdes = pop @tes;
	   my $tid = pop @tes;
 	   &printOption($method,"tournament-$tid-x","To $tdes");
	   if($teams)
		{
		my @teamoptions =  &active_teams($dbh,$tid);
		while( $#teamoptions > 0)
		{
		  my $tname = pop @teamoptions;
		  my $uid = pop @teamoptions;
		  &printOption($method,"tournament-$tid-$uid","To $tdes Team $tname");
		}
             }
	  }

      print "</select>";
      }
   else 
      {  print "<input type=hidden name=method value='$method'>\n";
         print "method: $method";
      }

  print "</td></tr>\n";
	
	print "<tr><td><b>Date:</b></td><td>$date</td></tr>\n";
	
	print "<tr><td><b>Subject:</b></td><td>";
	my $s = encode_entities($subject);
	print "<input type=$ttype name=subject value=\"$s\" size=80>";
	if(!$edit)
		{	print $s;
    }
	print "</td></tr>\n";
	
	print "<tr><td colspan=2><p><pre>";
	if($edit)
		{print "<textarea name=body  cols=80 rows=20>$body</textarea>";
		}
		else
	{ my $b = uri_escape($body);
		print "<input type=hidden name=escaped value=1>\n";
		print "<input type=hidden name=body value=\"$b\">";
		print $body;
	}
	print "</pre></td></tr>\n";
	print "</table>";
	
	print "<input type=submit name=$bname value='$button'>\n";
    my $ctest = ($test) ? "checked" : "";
    print "<input type=checkbox name=test $ctest>test\n";
	my $ctd = $cctd ? "checked" : "";
	print "<input type=checkbox name=cctd $ctd>cc the td\n";
	print "</form>\n";
  }
  &standard_footer();
  }
  &disconnect($dbh);
}

do_gs_edit();
