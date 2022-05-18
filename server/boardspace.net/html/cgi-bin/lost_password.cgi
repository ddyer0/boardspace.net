#!/usr/bin/perl 
#
# 2/2010 fix sql injection vulnerability and install &allow_ip_access
# 2/2017 change to using a recovery token instead of sending passwords
#
# optional parameters:
# nitems=100                      =n to show top n players
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/password_tools.pl";

use strict;
use Crypt::Tea;

$'minPasswordLength = 6;
$'maxPasswordLength = 25;

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

<H2><center>$header</center></H2>

Header
}

sub show_registrations()
{ my ($dbh,$nrows,$name,$email)=@_;
  my $qname = $dbh->quote("%$name%");
  my $nameclause = ($name eq '') ? '' : "player_name like $qname";
  my $qmail = $dbh->quote($email);
  my $mailclause = $email ? "e_mail=$qmail " . ($nameclause ? " and " : "") : '';
  my $q = "SELECT uid,player_name,e_mail,country,date_joined FROM players where $mailclause $nameclause ORDER BY date_joined DESC";
  my $sth = &query($dbh, $q );
	my ($nitems) = &numRows($sth);
	if($nrows<$nitems) { $nitems=$nrows; }
  #print "Q: $q<p>";
  if($nitems>0)
  {
	print "<TABLE BORDER=0 CELLPADDING=2 WIDTH=\"100%\">";
	print "<TR><td></td><TD><p align=left><b>Player Name</b></TD>";
	print "<TD><p align=left><b>Email</b></TD>";
	print "<TD><p align=left ><b>Country</b></TD>";
	print "<td><b>Registered on</b></td>";
	print "<TR>";
	my ($n) = 0;
	while ($n < $nitems)
		{my ($uid,$player_name,$email,$country,$date_joined) = &nextArrayRow($sth);
		 my $pdate = &date_string($date_joined);
	  $n++;
    my $sel = (lc($name) eq lc($player_name)) ? "checked" : "";
		print "<TR>";
     print "<td><input type=radio name=uid value=$uid $sel></td>";
     print "<TD><P ALIGN=left><A HREF=\"javascript:editlink('$player_name',0)\">$player_name</A></TD>";
     # don't give the full email here
     my $idx = index($email,'@');
     if($idx>0) { $email = substr($email,0,$idx+1) . "..." ; }
     my $em = &obfuscateHTML("$email");
		print "<TD><P ALIGN=left>$em</TD>";
		print "<TD><P ALIGN=left>$country</TD>";
		print "<TD>$pdate</TD>";
		print "</TR>\n";
	}
	print "</TABLE></P>";
  }
  &finishQuery($sth);
  &disconnect($dbh);

  return($nitems);
}

sub doacquire()
{ my ($dbh) = @_;
  my $nitems=param('n');
  my $language = param('language');
  my $name = param('pname');
  my $email = param('email');
  &bless_parameter_length($name,20);
  &bless_parameter_length($nitems,10);
  &bless_parameter_length($language,15);
	
  my $name = &despace(substr($name,0,25));
  my $uid = param('uid');
  if($nitems=='') { $nitems=100; }

  if($uid) 
    { #actually sending a password
	&note_failed_login($dbh,$ENV{'REMOTE_ADDR'},"IP: send password change $name");
	&send_password($dbh,$uid);
    $name="";
    }
  print "<form action=$ENV{'REQUEST_URI'} method=post>\n";
  print "<input type=hidden name=language value='$language'>\n";
  print &trans("My login id is");
  print "<input name=pname value='$name' width=12>";
  print &trans(" .. or something containing this string");
  print &trans("<br>(at least 3 characters)");
  print "</form><form action=$ENV{'REQUEST_URI'} method=post>";
  print &trans("<br>..or the email address I registered with was");
  print "<br><input name=email value='$email' SIZE=50 MAXLENGTH=50 chars>";
  print "<br>\n";
  if( ($name && (length($name)>=3)) || ($email && (length($email)>=5)) )
    { &note_failed_login($dbh,$ENV{'REMOTE_ADDR'},"IP: password change $name");
      my $ns = &show_registrations($dbh,$nitems,$name,$email); 
      if($ns>0)
        {print &trans("selectone");
         print "<br>";
         my $doit = &trans("Send Password Change");
         print "<input type=submit value='$doit'>\n";
        }
        else
      {  print &trans("SorryNoMatch");
         print "<br>";
      }
   }

  print "</form>";
}

sub blesspassword()
{	my ($jws,$password,$password2) = @_;
	my $message = '';
	my $blankpw = $jws ? "nopassword" : &trans("You have entered a blank password.  Please enter a password (up to ten characters).");
	my $difpw = $jws ? "nopassword" : &trans("You must enter the same password twice.  Please try again.");
	my $plen = $jws ? "nopassword" : &trans("passwords must be $'minPasswordLength to $'maxPasswordLength characters");
	my $line = $jws ? "\n" : "<br>\n";
	if(('' eq $password) && ('' eq $password2)) { return(0); }
	if('' eq $password) { print "$blankpw$line"; return(0); }
	if(! ($password eq $password2)) { print "$difpw$line"; return(0); }
	if( (length($password)<$'minPasswordLength) || (length($password)>$'maxPasswordLength)) { print "$plen$line"; return(0); }
	return(1);
}


sub doreset()
{	my ($dbh,$key) = @_;
	my $uid = param('uid');
	my $name = param('pname');
  
    &bless_parameter_length($name,20);
    my $name = &despace(substr($name,0,25));

	my $problem = '';
	my $password = param('password');
	my $password2 = param('password2');
	
 	$password = &despace(substr($password,0,$'maxPasswordLength+10));
	$password2 = &despace(substr($password2,0,$'maxPasswordLength+10));

	my $valid = &validateKey($uid,$key);
	if(!$valid) { $problem = 'key not valid'; }
	else {
		my $quid = $dbh->quote($uid);
		my $qkey = $dbh->quote($key);
		my $qname = $dbh->quote($name);
		my $q = "select player_name,full_name from passwordrecovery "
				  . "left join players on players.uid=passwordrecovery.uid "
				  . "where passwordrecovery.uid=$quid and recoverytoken=$qkey and recoverydate>date_sub(NOW(),interval 1 day)";
		#print "$q<br>";
		my $sth = &query($dbh,$q);
		my $nr = &numRows($sth);
		if($nr!=1) { $problem = 'key not found or expired'; }
		else {
			my ($pname,$fullname) = &nextArrayRow($sth);
			my $bless = &blesspassword(0,$password,$password2);
			if($bless)
			{	
				#actually changing a password, record a ding to keep it from happening a lot
				&note_failed_login($dbh,$ENV{'REMOTE_ADDR'},"IP: use recovery token for $name uid=$uid");
 
				my $qpassword = $dbh->quote($password);
				# ready to roll
				my $del = "delete from passwordrecovery where uid=$quid limit 1";
				#print "$del<br>\n";
				&commandQuery($dbh,$del);
				# obsolete password
				my $upd = "update players set "
				# . "password = $qpassword ,"
				  . " pwhash=MD5(concat($qpassword,uid)) where uid=$quid limit 1";
				#print "$upd<br>\n";
				&commandQuery($dbh,$upd);
				&changePhp($uid,$pname,$password);		
				print "Password changed<p>\n";
			}
			else
			{	my $scr = $ENV{'REQUEST_URI'};
				print "<form method=post action='$scr'>\n";
				print "<input type=hidden name='uid' value='$uid'>\n";
				print "<input type=hidden name='key' value='$key'>\n";
				print "<input type=hidden name='pname' value='$name'>\n";
				print &trans("$'minPasswordLength to $'maxPasswordLength characters, letters and numbers only");
				print "<br>\n";
				print &trans("new password for #1 (#2)",$pname,&encode_entities(&utfDecode($fullname)));
				print " ";
				print "<input type=password name='password' SIZE=20 MAXLENGTH=25><br>\n";
				print &trans("the same password again:");
				print " ";
				print "<input type=password name=password2 SIZE=20 MAXLENGTH=25><br>\n";
				print "<input type=submit value='change password'><br>\n";
				print "</form>";
			}
		}
	}
	if($problem) { print "Problem: $problem<br>"; }
}

{
print header;
init();
param();
  my $dbh = &connect();
 if($dbh  && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
 {
  &readtrans_db($dbh);
  
  my $header=&trans("Password Change");
  &rank_header($header,0);
 
  print "<center><p>";
  print &trans("We don't remember your password either, but you can change it");
  print "</center>";
  my $key = param('key');
  if('' eq $key) { &doacquire($dbh); }
  else { &doreset($dbh,$key); }
 
    &standard_footer();
  }
  &disconnect($dbh);
  }


