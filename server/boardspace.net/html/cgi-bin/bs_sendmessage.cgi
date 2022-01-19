#!/usr/bin/perl
#
# send a message to a player, to be received when he logs in
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
<TITLE>Send a Message to a Player</TITLE>

Head_end
}

sub active_tournaments()
{	my ($dbh) = @_;
	my @val;
	my $q = "SELECT uid,description from tournament where (status='signup' or status='active')";
	my $sth = &query($dbh,$q);
	my $numrows = &numRows($sth);
	while($numrows-- > 0)
	{
	my ($id,$desc) = &nextArrayRow($sth);
	push @val,$id;
	push @val,$desc;
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
  my $sth = &query($dbh,"SELECT uid,is_supervisor,is_tournament_manager FROM players WHERE (is_supervisor='yes') and player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid))");
  my $n = &numRows($sth);
  my ($uid,$super,$tournament) = &nextArrayRow($sth);
  &finishQuery($sth);
  if(($n == 1)
		&& &allow_ip_login($dbh,$myaddr)
		&& &allow_ip_login($dbh,$bannercookie))
	{	return($uid,$super,$tournament);
	}
	if($n==1)
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

sub isEmailVariation()
{	my ($method,@vars) = @_;
	my $mname = lc(substr($method,6));
	my $vv;
	foreach $vv (@vars)
	{ 
		if($mname eq $vv) 
		{ 
		  return(1); 
	        }
	}
	return(0);
}
sub do_gs_edit()
{
param();
  my $dbh = &connect();
  if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
  {
  &readtrans_db($dbh);
  my $unbounce=0;
  my $test = param('test');
  my $cctd = param('cctd');
	my $fromname = param('fromname');
	my $toname = param('toname');
	my $passwd = param('passwd');
	my $subject = param('subject');
	my $message = param('message');
	my $approve = param('approve');
	my $deliver = param('deliver');
	my $method = param('method');
	my $body = param('body');
	my $date = param('date');
	my $stamp = param('stamp');
	my $done = param('done');
	my $escaped = param('escaped');
	my $smpass = param('smpass');
	my @vars = &principle_variation_names($dbh);
	my @tournaments = &active_tournaments($dbh);
	my (@multi) = split(/[,]/,$toname);
  if($escaped) 
	{ 
	 $body = uri_unescape($body);
	 $subject = uri_unescape($subject); 
	}
	if($stamp && $toname) 
  	{	my $qstamp = $dbh->quote($stamp);
  		my $qname = $dbh->quote($toname);
  		my $sth = &query($dbh,"SELECT fromplayer,subject,datesent,body FROM email where toplayer=$qname AND stamp=$qstamp");
  		my $nr = &numRows($sth);
  		if($nr>0)
  		{
  			($fromname,$subject,$date,$body) = $sth->fetchrow;
  			$deliver = 0;
  			$approve = 0;
  		}
		&finishQuery($sth);

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

	# check recepients
	{
	foreach my $na (@multi)
	{
		if(!&findpname($dbh,&despace($na)))
		{ print "<b><p>Recepient '$na' is not in the database</b>";
		  $approve = 0;
		  $deliver = 0;
		}
	}
	}

	my $edit = !($deliver || $approve || $stamp);
	my $ttype = $edit ? "text" : "hidden";
	my $button = $stamp ? "Ok, On with the Game"
								  : $deliver ? "Send another Message"
									: $approve ? "Ok, send the message"
									: "Preview the Message before sending";
	my $bname = $stamp ? "done"
							: $deliver ? "start"
							: $approve ? "deliver"
							: "approve";
	if($done)
  	{
   	my $qto = $dbh->quote($toname);
 		my $qstamp = $dbh->quote($stamp);
 		my $cmd = "DELETE FROM email where toplayer=$qto and stamp=$qstamp";
 		&commandQuery($dbh,$cmd);
  	print "<p><b>Ok, you can log in again</b>";
  	}
  elsif ($deliver)
  	{ 
  		my $qsubj = $dbh->quote($subject);
  		my $qmsg = $dbh->quote($body);
  		my $qfrom = $dbh->quote($fromname);
  		my $qdate = $dbh->quote($date);
   		my $qto = $dbh->quote($toname);
  		my $qbody = $dbh->quote($body);
    if($method eq 'at-login')
      {	foreach my $player (@multi)
		{
		 $qto = $dbh->quote(&despace($player));
 		 my $query = "INSERT INTO email set toplayer=$qto,fromplayer=$qfrom,datesent=$qdate,subject=$qsubj,body=$qbody";
  		 &commandQuery($dbh,$query);
  		 print "<p>Message queued for delivery to $qto";
		}
      }
      else 
      { # some kind of email delivery
        my $addr="";
        my $q = "select player_name,e_mail,e_mail_bounce from players where  ";
        if ($method eq 'email-player') 
        {  $unbounce=1;
		    my $uu = "";
		    my $oldq = $q;
			 $q = "";
		     foreach my $na (@multi)
			 { $qto = $dbh->quote(&despace($na));
			   $q .= "$uu $oldq player_name=$qto";
			   $uu = " UNION ";
			 }
			 #print "$q<p>";
		   
       }
		elsif ($method eq 'email-bounce')
		{ $unbounce=1;
		   $q .= " (status='ok') AND  (no_email='' OR no_email is NULL)  AND (e_mail_bounce>0)";
		}
		elsif ($method eq 'email-translators')
		{  $q .= " (status='ok') AND  (is_translator ='Yes') ";
		}
        elsif ($method eq 'email-all')
        {  $q .= " (status='ok') AND  (no_email='' OR no_email is NULL)  AND (e_mail_bounce<2)";
        }
		elsif (&isEmailVariation($method,@vars))
		{  my $vv = $dbh->quote(substr($method,6));
		   $q = "SELECT player_name,e_mail,e_mail_bounce from players left join ranking on players.uid=ranking.uid "
                 . "WHERE variation=$vv AND status='ok' AND (no_email='' OR no_email is NULL) AND (e_mail_bounce<2)";
		}
		elsif (substr($method,0,11) eq "tournament-")
		{$unbounce=1;
		 my $tid0 = substr($method,11);
		my $np = (substr($tid0,0,2) eq "n-");
		my $tid = $np ? substr($tid0,2) : $tid0;
		my $qtid = $dbh->quote($tid);
		my $tv = $dbh->quote(&tournament_variation($dbh,$tid));
		my $npjoin = $np ? " left join ranking  on participant.pid = ranking.uid and ranking.variation=$tv  " : "";
		my $npcond = $np ? " and ranking.variation is null " : "";
		$q = "SELECT player_name,e_mail,e_mail_bounce from participant left join players "
				. " on participant.pid=players.uid $npjoin"
				. " where tid=$qtid $npcond";
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
  print "<title>Send a Message to a Player</title>\n";
	}

  print "<form action=$ENV{'REQUEST_URI'} method=POST>\n";
	print "<table><tr><td>";
	print "<b>From:</b></td><td>" ;
	print "<input type=hidden name=stamp value='$stamp'>";
	print "<input type=$ttype name=fromname value='$fromname' size=12>";
  if(!$edit)
  	{ print "$fromname";
  	}
  print "</td></tr>\n";
  
  if($edit)
	{
		print "<tr><td><b>your player password:</b></td><td><input type=password name=passwd value='$passwd' SIZE=20 MAXLENGTH=25>(not sent to the recepient)</td></tr>";
		print "<tr><td>note: this requires supervisor priv</td></tr>\n";
	}
	else
  { print "<input type=hidden name=passwd value=$passwd>\n";
    print "<input type=hidden name=smpass value=$smpass>\n";
  }
	
	print "<tr><td><b>To:</b></td><td>";
	print "<input type=$ttype name=toname value='$toname' size=12>";
	if(!$edit)
	{	print "$toname";
	}
  if(!$method) { $method = 'at-login'; }
  if ($edit)
      {  print "<select name=method>\n";
      &printOption($method,'at-login',"To Player at Login");
      &printOption($method,'email-player',"To player by email");

	  my $vv;
	  for $vv (@vars)
	  {
	   &printOption($method,"email-$vv","To $vv players by Email");
	  }
	  my @tes = @tournaments;
	  while( $#tes > 0)
	  {my $tdes = pop @tes;
	   my $tid = pop @tes;
	   &printOption($method,"tournament-$tid","To $tdes");
	   &printOption($method,"tournament-n-$tid","To $tdes (nonplaying players)");
	  }

      &printOption($method,'email-all',"To all players by Email");
	  &printOption($method,'email-bounce',"To players with bouncing Email");
	  &printOption($method,'email-translators',"To translators by Email");
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
	
	if($deliver != 0)
	{
    my $ctest = ($test) ? "checked" : "";
    print "<input type=checkbox name=test $ctest>test\n";
	my $ctd = $cctd ? "checked" : "";
	print "<input type=checkbox name=cctd $ctd>cc the td\n";
	}
	print "</form>\n";
  }
  &disconnect($dbh);
  &standard_footer();
  }
}

do_gs_edit();
