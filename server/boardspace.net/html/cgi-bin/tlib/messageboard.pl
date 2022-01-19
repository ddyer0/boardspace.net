sub print_messageboard_header 
{	&standard_header();
	my $msg1 = &trans("Public Notices and Messages");
	my $msg2 = &trans("use this space to post information about when you will be available to play");
	my $msg3 = "<h3><a href=/cgi-bin/rss2.cgi type='location/rss+xml' target=_blank ><img src=/images/feed-icon-14x14.png></a>"
				. "<a href='/cgi-bin/messageboard.cgi'>$msg1</a></h3>";
	print<<Head_end;
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0//EN">
<HTML>
<HEAD>
<TITLE>$msg1</TITLE>
<center><H1>$msg3</h1>
<br>$msg2</center>
<p>
Head_end
}

#logon, return uid and supervisor status
sub messageboard_logon 
{
  my ($dbh,$pname,$passwd) = @_;
  my $myaddr = $ENV{'REMOTE_ADDR'};
  my $bannercookie = &bannerCookie();
  my $slashpname = $dbh->quote($pname);
  my $slashpwd = $dbh->quote($passwd);
  my $q = "SELECT uid,is_supervisor FROM players WHERE player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid)) AND status='ok' and is_robot is null";
  #print "Q: $q<br>";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  my $val=0;
  my $is_supervisor='';
  if($n == 1)
    { ($val,$is_supervisor) = &nextArrayRow($sth);
    }
  &finishQuery($sth);

  if(!val)
  {	$is_supervisor = "";
	 &note_failed_login($dbh,$myaddr,"IP: Messageboard as $pname");
	 &note_failed_login($dbh,$bannercookie,"CK: Messageboard as $pname");
  }
  elsif ( &allow_ip_login($dbh,$myaddr)
			&& &allow_ip_login($dbh,$bannercookie))
	{
	}
  else
	{
	 $val = 0;
	 $is_supervisor = "";
	}
  return ($val,(lc($is_supervisor) eq 'yes'));
}

sub show_messages
{ my ($dbh,$link,$recycle) = @_;
  my $q = "SELECT player_name,messageid,content,notes.uid from notes left join players on players.uid=notes.uid WHERE expires>current_date() ORDER BY expires";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  if($n>0)
    { print "<table width=100% border=1>";
	  my $deloption = $recycle ? "<td><b>delete&nbsp;</b></td>" : "";
      if(!$link) 
	  { print "<tr>$deloption<td><b>From</b></td><td><b>Message</b></td></tr>\n";
	  }
      while($n-->0)
      {my ($from,$messid,$content,$uid) = &nextArrayRow($sth);
	   if($recycle && &param("delete$messid"))
	   {
	    &commandQuery($dbh,"delete from notes where messageid='$messid' and uid='$uid'");
	   }
	   else
	   {
	   my $deloption = ($recycle && (lc($uid) eq lc($recycle)))
		 ? "<td width=5%><input type=checkbox name='delete$messid' ></td>" 
		 : $recycle? "<td></td>" : "";
	   $content  = &encode_entities(&utfDecode($content));
       print "<tr>$deloption<td width=10%><b>${from}</b>:&nbsp;</td><td>$content</td></tr>\n";
	   }
      }
     print "</table>";
  }else
  {  my $msg = &trans("No Messages");
	 print "<p>$msg<p>";
  }
  &finishQuery($sth);
  print "<br><br>\n"
}

sub show_input_form
{ my ($fromname,$passwd,$expires,$subject) = @_;
  $fromname = encode_entities($fromname);
  $subject = encode_entities($subject);
  my $msg = &trans("Add a Message");
  print "<p>$msg<br>\n";
  my $msg = &trans("Player:");
  print "<table><tr><td>$msg ";
  print "<input type=text name=fromname value='$fromname' size=12>\n";
  my $msg = &trans("Your Password:");
  print "</td><td>$msg ";
  print "<input type=password name=passwd value='$passwd' SIZE=20 MAXLENGTH=25>\n";
  my $msg = &trans("Expires in");
  print "</td><td>$msg ";
  print "<select name=expires>\n";
  my $msg2 = &trans("1 day");
  print "<option value='1'>$msg2</option>\n";
  my $msg3 = &trans("#1 days",2);
  print "<option value='2' selected>$msg3</option>\n";
  my $msg3 = &trans("#1 days",3);
  print "<option value='3'>$msg3</option>\n";
  my $msg3 = &trans("#1 days",4);
  print "<option value='4'>$msg3</option>\n";
  my $msg3 = &trans("#1 days",5);
  print "<option value='5'>$msg3</option>\n";
  my $msg3 = &trans("#1 days",6);
  print "<option value='6'>$msg3</option>\n";
  my $msg3 = &trans("#1 days",7);
  print "<option value='7'>$msg3</option>\n";

  print "</td></tr>";
  my $msg = &trans("message:");
  print "<tr><td colspan=3>$msg <input name=subject type=text value='$subject' size=100>\n";
  print "</td></tr></table>\n";
  my $msg = &trans("to delete a message, replace it with a blank message");
  my $msg2 = &trans("Add a Message");
  print "<input type=submit name=doit value='$msg2'> $msg\n";
}

sub processInputForm
{  my ($dbh,$uid,$super,$expires,$subject) = @_;
   if($expires>21) { $expires=21; }
   if($expires<1) { $expires=1; }
   if($uid>0)
   { if(!$super)
       { &commandQuery($dbh,"DELETE from notes where uid=$uid"); 
	   }
     if(length($subject)>5)
      {
      my $enc = $dbh->quote(&utfEncode($subject));
      my $q = "INSERT INTO notes SET uid=$uid,expires=DATE_ADD(CURRENT_DATE,INTERVAL $expires DAY),content=$enc";
      &commandQuery($dbh,$q);
      }
     return(1);
   }
}


sub do_messageboard()
{
  my ($dbh,$fromname,$passwd,$subject,$expires,$link) = @_;

  if($dbh) { &readtrans_db($dbh); }
  
  if($link)
  {
  my $myaddr = $ENV{'REMOTE_ADDR'};
  my $msg = &trans("Public Notices and Messages");
  print "<h3><a href=/cgi-bin/rss2.cgi type='location/rss+xml' target=_blank ><img src=/images/feed-icon-14x14.png></a>";
  print "<a href='/cgi-bin/messageboard.cgi'>$msg</a></h3>";
  }
  else 
  {&print_messageboard_header(); 
  }
  if($dbh)
    {
	 my ($uid,$super);
	 if($fromname && $passwd)
	 {
	  ($uid,$super) = &messageboard_logon($dbh,$fromname,$passwd);
	  if($uid>0) 
      { if(&processInputForm($dbh,$uid,$super,$expires,$subject))
        {  $expires=0;
           $subject='';
        }
      }
	  elsif($fromname)
	 {  print "<b>Sorry, name '$fromname' and that password are not found in our player list.</b><p>";
        return(0);
     } 
	 }
	  if(!$link)
	   { print "<form action='/cgi-bin/messageboard.cgi' method=post>\n"; 
	     my $lan = &select_language($language);
		 my $elan = &encode_entities($lan);
		 print "<input type=hidden name=language value='$elan'>";
	   }

      &show_messages($dbh,$link,$super?$uid:0);
	  if($link)
	  {
	  my $msg = &trans("Add a Message");
	  print "<center><a target=_new href='/cgi-bin/messageboard.cgi?fromname=$fromname'>$msg</a></center>";
	  }
	  else
	  {
      &show_input_form($fromname,$passwd,$expires,$subject);
	  print "</form>\n"; 
	  &standard_footer();
	  }
    }
}

sub show_messages_rss
{ my ($dbh,$number) = @_;
  my $q = "SELECT player_name,messageid,content,notes.uid,expires from notes left join players on players.uid=notes.uid WHERE expires>current_date() ORDER BY expires limit $number";
  my $sth = &query($dbh,$q);
  my $n = &numRows($sth);
  if($n>0)
    {
      while($n-->0)
      {my ($from,$messid,$content,$uid,$expires) = &nextArrayRow($sth);
       $content = &encode_entities($content);
       print "\n<item>\n<title>${from}: $content</title>\n";
       print "<pubDate>01 Jan 2012 00:00:00 GMT</pubDate>\n";
       print "<description>$content</description>\n</item>\n";
      }
  }else
  {  my $msg = &trans("No Messages");
	 print "<item><title>$msg</title><item>";
  }
  &finishQuery($sth);
}
sub show_messageboard_rss()
{
  my ($dbh,$number) = @_;

  if($dbh)
  {
  &readtrans_db($dbh);

  &show_messages_rss($dbh,$number);

  }
}

1
