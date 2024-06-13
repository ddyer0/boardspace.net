#!/usr/bin/perl
#
# edit.cgi
# view and edit player registration details
#
# 9/2005 added logic to allow uncomfirmed users to make changes, and to resend the
# the confirmation link if they make changes.
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use Debug;
use strict;
use CGI::Cookie;
use URI::Escape;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";
require "tlib/show_activity.pl";
require "tlib/favorite-games.pl";
require "tlib/password_tools.pl";

use HTML::Entities;

use Crypt::Tea;

sub init {
	$| = 1;				# force writes
  __dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
}

sub print_form_header()
{
	print "<form action=$ENV{'REQUEST_URI'} method=POST>\n";
	my $lan = &select_language(&param('language'));
	my $elan = &encode_entities($lan);
	my $test = &param('test');
	if($test) { print "<input type=hidden name=test value='$test'>\n"; }
}
sub alias_table()
{  my ($dbh,$uid,$identity,$supervisor) = @_;
    #
    # report possible aliases
    my $q1 = "SELECT sum(count) from  coincident_ip where uid1=$uid OR uid2=$uid";
	my $q2 = "SELECT player_name,uid from players WHERE identity!='0' AND uid!='$uid' AND identity='$identity'";
    my $sth1 = &query($dbh,$q1);
	my $sth2 = (($identity!=0)&&($identity!=-1))
				? &query($dbh,$q2)
				: 0;
    my $nr1 = &numRows($sth1);
	my $nr2 = &numRows($sth2);
    my $sum = 0;
    if($nr1>0) { ($sum) = &nextArrayRow($sth1); }
	
	if(($sum>0) || ($nr2>0))
	{
	print "<tr>";

    my $q = "SELECT uid1,uid2,pl1.player_name,pl2.player_name,type,count from coincident_ip "
      . " left join players as pl1 on pl1.uid=uid1 "
      . " left join players as pl2 on pl2.uid=uid2  "
      . " WHERE uid1=$uid or uid2=$uid order by count ";

     if($sum>0)
     { 
      my $sth = &query($dbh,$q);
	  my $rows = 0;
      my $nr = &numRows($sth);
      while ($nr-- > 0)
      {  my ($uid1,$uid2,$u1name,$u2name,$type,$count) = &nextArrayRow($sth);
          if((($count/$sum)> 0.01) && !(lc($type) eq 'nomatch'))
            {
			if($rows==0) {
	  print "<td><table><caption><b>" . &trans("possible aliases (same IP)") 
		. "</b></caption><tr><td>"
		. &trans("Alias")
		. "</td><td>"
		. &trans("alias type")
		. "</td><td>%</td>"; 
	  $rows++;
			}
            if($uid1 eq $uid) { $uid1 = $uid2; $u1name = $u2name; }
            my $quant = int(($count/$sum)*100.0+0.5);
			my $ss = $supervisor? "&super=$supervisor" : "";
            print "<tr><td><a href=$ENV{'SCRIPT_NAME'}?pname=$u1name>$u1name$ss</a></td><td>$type</td><td>$quant% (n=$count)</td></tr>"
            }
      }
	  if($rows>0)
	  {
      print "</table></td>";
	  }
	  &finishQuery($sth);
    }
	if($supervisor && ($nr2>0))
	{ my $comma = "";
	  print "<td><b>" . &trans("Same Identity Cookie") . " ($identity)</b>: ";
	  while($nr2>0)
	  {	my ($pn,$ui) = &nextArrayRow($sth2);
	    $nr2--;
	    print "$comma $pn";
		$comma = ",";
	  }
	}
	print "</tr>";
	}

	&finishQuery($sth1);
	&finishQuery($sth2);
 
}

# print a numeric cell, optionally with editing
sub pcell
 { my ($edit,$variation,$key,$val) = @_;
   if($edit)
   {print "<td><input type=hidden name='old-$variation-$key' value='$val'>";
    print "<input type=text size=5 name='new-$variation-$key' value='$val'></td>\n";
   }
   else
   { print "<td>$val</td>\n"; 
   }
 }
sub edit_gametable()
{ my ($dbh,$uid,$superedit) = @_;
  my $var;
  my $msg="";
  foreach $var (&all_variations($dbh))
  { my $key;
    my @cells = $superedit ? ('value','max_rank','games_won','games_lost','advocate') : ('advocate');
    my $comma = "";
    my $q = "";
    foreach $key (@cells)
    {  my $p1 = "old-$var-$key";
       my $p2 = "new-$var-$key";
       my $old = &param($p1);
       my $new = &param($p2);
       if(!($old eq $new))
      {  my $qnew = $dbh->quote($new);
         $q .= "$comma$key=$qnew";
         $comma = ",";
         $msg .= "$var $key changed from $old to $new<br>";
      }
    }
   if($q)
    {
     my $up = "UPDATE ranking SET $q WHERE uid='$uid' AND variation='$var'";
     &query($dbh,$up);
    }
  } 
  return($msg);
}
  
sub gametable()
{  my ($dbh,$uid,$edit,$playeredit,$player) = @_;
   my $var;
   print "<table border=1>";
   my $eplayer = uri_escape($player);
   my $q = "select variation,is_master,advocate,value,games_won,games_lost,max_rank,last_played"
     . " from ranking where uid=$uid and variation is not null and variation!='' order by last_played desc";
   my $sth = &query($dbh,$q);
   my $n = &numRows($sth);
   print "<tr>";
     print "<td>" . &trans("Game") . "</td>\n";
     print "<td>" . &trans("Current Rank") . "</td>\n";
     print "<td>" . &trans("Maximum Rank") . "</td>\n";
     print "<td>" . &trans("Games Won") . "</td>\n";
     print "<td>" . &trans("Games Lost") . "</td>\n";
     print "<td>" . &trans("Last Played") . "</td>\n";
     print "<td>" . &trans("Group") . "</td>\n";
     print "</tr>";
   while($n-- > 0)
    {  print "<tr>";
     my ($variation,$is_master,
         $advocate,
         $value,$games_won,$games_lost,$max_rank,$last_played) = &nextArrayRow($sth);
     my $evar = uri_escape($variation);
	 if($is_master eq 'Yes') { $variation = &trans("#1 Master",$variation); }
     $last_played = &date_string($last_played,1);
     print "<td><a href=\"/cgi-bin/player_analysis.cgi?game=$evar&player1=$eplayer\">$variation</a></td>\n";
     &pcell($edit,$variation,"value",$value);
     &pcell($edit,$variation,"max_rank",$max_rank);
     &pcell($edit,$variation,"games_won",$games_won);
     &pcell($edit,$variation,"games_lost",$games_lost);
     print "<td>$last_played</td>\n";

     if( ($games_won+$games_lost)>10)
     {	print "<td>";
        my $mas = (($is_master eq 'Yes')||($max_rank >=2000)) ? &trans("Master") : "";
        my $end = "";
        if ($playeredit)
			{ print "<input type=hidden name = 'old-${variation}-advocate' value='$advocate'>\n";
			  my @choices = &get_enum_choices($dbh,'ranking','advocate');
			  $end = &get_translated_selector_string("new-${variation}-advocate","#1-group",$advocate, @choices);
			}
			elsif($advocate)
			{ 
			$end = &trans("${advocate}-group"); 
			}
		my $msg = $end;
		if($mas) { if($end) { $msg .= ", "; } $msg .= $mas; }
		print "$msg</td>";
     }else
     { print "<td></td>"; 
     }


     print "</tr>";
  }
  &finishQuery($sth);
  print "</table>";
}

sub print_header
{my ($readonly,$super) = @_;
 my $foc = $readonly ? "" : 'onLoad="document.forms[0].pname.focus()"';
 &standard_header();
 
if(!$readonly)
	{
	 my $msg = &trans("BoardSpace Player details");
	 print "<blockquote><FONT FACE=\"Futura Bk BT\"><H1>${super}$msg</H1></font>\n";
	 my $msg = &trans("Registration information change");
 	 print "<title>Boardspace.net - ${super}$msg</title>\n";
	}else
	{
	my $msg = &trans("Boardspace.net - player details");
	print "<title>$msg</title>\n";
	}
	 &honeypot();

}


sub findpname()
{	my ($dbh,$plname) = @_;
	my $slashname = $dbh->quote($plname);
	my $sth = &query($dbh,"SELECT full_name FROM players WHERE player_name=$slashname");
	my $n = &numRows($sth);
	&finishQuery($sth);
  return($n>=1);
}
#
# require the "supervisor" be a supervisor according to the database, and 
# that his player password is correct, and that the supervisor password is correct.
#
sub is_valid_supervisor()
{ my ($dbh,$supervisor,$ppas,$passwd,$myaddr) = @_;
  if($supervisor)
  	{my $qsup = $dbh->quote($supervisor);
	 my $qpas = $dbh->quote($ppas);
	 my $supq = "select is_supervisor from players where player_name=$qsup and pwhash=MD5(concat($qpas,uid)) and is_supervisor='Yes'";
	 my $sth = &query($dbh,$supq);
	 my $issup = &numRows($sth)>=1;
     &finishQuery($sth);
	 my $val = !($'sendmessage_password eq "") && ($passwd eq $'sendmessage_password) && $issup;
	 my $bannercookie = &bannerCookie();
	 if($val && &allow_ip_login($dbh,$myaddr)
	  && &allow_ip_login($dbh,$bannercookie))
	  {
	   return(1);
	  }
	  elsif($val)
  		{
			&note_failed_login($dbh,$myaddr,"IP: edit as supervisor $supervisor");
 			&note_failed_login($dbh,$bannercookie,"CK: edit as supervisor $supervisor");
 		}
  	}
	return(0);
}

sub logon 
{ my ($dbh,$pname,$passwd,$supervisor,$spassword) = @_;
  my $myaddr = $ENV{'REMOTE_ADDR'};
  my $bannercookie = &bannerCookie();

  if(&is_valid_supervisor($dbh,$supervisor,$passwd,$spassword,$myaddr))
	{ 
   	  return(&findpname($dbh,$pname));
	}
	elsif($pname && $passwd)
	{
	# looking to make changes
	my $slashpname = $dbh->quote($pname);
	my $slashpwd = $dbh->quote($passwd);
	my $q = "SELECT player_name,status FROM players WHERE locked is null AND player_name=$slashpname AND pwhash=MD5(concat($slashpwd,uid))";
	#print "Q: $q<br>";
	my $sth = &query($dbh,$q);
	my $n = &numRows($sth);
	my ($pn,$status) = ($n == 1)? $sth->fetchrow() : "";
	&finishQuery($sth);
	#
	# CHECK that SELECT matched only ONE player
	#
	my $val = ($n== 1) && (($status=='unconfirmed') || ($status eq 'ok'));
	if(!$val) 
		{ &note_failed_login($dbh,$myaddr,"IP: edit as $pname ($passwd)"); 
		  &note_failed_login($dbh,$bannercookie,"CK: edit as $pname ($passwd)"); 
		}
	return($val);
	}
	return(0); 
}


sub send_changes 
{ my ($old_email,$email,$pname,$name,$country,$city, $super,$stuff,$bcc,$nomail,$status,$uid) = @_;
  my $news = ($nomail eq 'y') ? "You will not receive any newsletters\n" : "";
  my $stames = "";
  if($status eq 'unconfirmed')
	{
	my $ll = "http://$ENV{'HTTP_HOST'}/cgi-bin/confirm_register.cgi?uid=$uid&pname=$pname";
	$stames = "your registration has not been confirmed.  Click on this link\n$ll\nto complete your registration.\n\n";
	if(!($old_email eq $email)) { $old_email .= ",$email"; }
	}

  my $auth = &userAuthenticator($pname);
  open( SENDMAIL, "| $'sendmail $old_email" );
  print SENDMAIL <<Mail_header;
$auth
Sender: $'from_email
From: $'from_email
To: $old_email
${bcc}Subject: ${super}Changes to your registration at Boardspace.net
$stames
Changes have been made to your registration information at Boardspace.net
Probably it was you who made the changes, if not: at least you know.

Your user name is $pname.
Your full name is $name.
Your country is $country.
Your city is $city.
Your email address is $email (this was sent to your old email address).
$news$stuff
Mail_header
        close SENDMAIL;

}


# --------------------------------------------
&init();

print header;

#print start_html('Logon');

__d( "main...");

sub print_capability()
{	my ($dbh,$form,$caption,$field,$cval) = @_;
	{
 	print "<tr><td>" . &trans($caption) . " :</td><td>";
    my $mc;
    print "<select name=$form>\n";
    foreach $mc(&get_enum_choices($dbh,"players","$field"))
      {  my $selected = ($mc eq $cval) ? "selected" : "";
        print "<option value='$mc' $selected>$mc</option>\n";
      }
      print "</select>\n";
    print "</td></tr>\n";
    }
}
sub do_gs_edit()
{
param();
  {
  __dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
  __d( "Checking Parameters...");
  __d( "REMOTE_HOST ".$ENV{'REMOTE_ADDR'});
	my $dbh = &connect();
    if(!$dbh || ! (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0)) { return; }
	&readtrans_db($dbh);
	my $language = &select_language(&param('language'));
	my $pname = param('pname');
	
	 &bless_parameter_length($pname,20);

	 $pname = &despace(substr($pname,0,25));
	my $newpname = param('newpname');
	 &bless_parameter_length($newpname,20);
		$newpname = &despace(substr($newpname,0,25));
	my $passwd = param('passwd');
	 &bless_parameter_length($passwd,25);
	 $passwd = &despace(substr($passwd,0,25));
	my $newpasswd = param('newpasswd');
 	 &bless_parameter_length($newpasswd,25);
     $newpasswd = &despace(substr($newpasswd,0,25));
 	my $newpasswd2 = param('newpasswd2');
 	&bless_parameter_length($newpasswd2,25);
	$newpasswd2 = &despace(substr($newpasswd2,0,25));

	my $newcountry = &despace(param('country'));
	my $newcity = &despace(param('city'));
	my $editable = param('editable');
	my $readonly = ($editable eq "");
	my $newname = &despace(&decode_entities(param('fullname')));
	my $newemail = param('email');
	my $show = param('show');
	my $update_pname=$pname;
	my $newhighranking = param('newhighranking');
	my $newranking = param('newranking');
	my $newmaster = param('newmaster');
	my $newidentity = param('newidentity');
	my $newstatus = param('newstatus');
	my $newretired = param('newretired');
	my $supervisor=param('super');
	my $spassword = param('spassword');
	my $newtournament = param('newtournament');
	my $newsuper = param('newsuper');
	my $newtranslator = param('newtranslator');
	my $newdiscord = &despace(param('discorduid'));
	my $newlanguage = &despace(param('language'));

    my $hasNewStatus = 0;
  my $newnomail = param('nomail');
  if($newnomail eq 'on') { $newnomail='y'; } else { $newnomail=''; }
	if(($passwd eq "") && !($newpname eq "") && !($newpname eq $pname)) { $pname = $newpname; $newpname=""; }
 
	if(($newcountry eq "No Change")||($newcountry eq "Select Country")) { $newcountry = ""; }
	if($editable eq "") { $editable=($pname eq "") ? "yes" : "no"; }

 	my $slashname = $dbh->quote($pname);
	my $q = "SELECT  timezone_offset,language,identity,players.comment,proposals.title,proposal,message,players.uid,locked,e_mail,e_mail_bounce,e_mail_bounce_date,full_name,country,city,date_joined,"
					. "last_played,last_logon,"
          . "games_played,is_master,is_translator,is_tournament_manager,is_supervisor,"
					. "players.status,last_ip,no_email,discorduid "
          . "FROM players left join proposals on players.proposal=proposals.uid "
          . "WHERE player_name=$slashname";
	my $sth = &query($dbh,$q);
	my $nr = ($pname eq '') ? 0 : &numRows($sth);
	#print "Q: $nr $q<p>";
	my ($timezone_offset,$lang,$identity,$comment,$proposal_title,$proposal,$message,$uid,$locked,$email,$bounce,$bounce_date,$name,$country,$city,$date_joined,
      $last_played,$last_logon,
			$games_played,$master,$translator,$istm,$issuper,$status,$last_ip,$nomail,$disid)
			= $sth->fetchrow();
	&finishQuery($sth);
	$name = &utfDecode($name);
	if($locked && ($supervisor eq '')) { $readonly = 1; }

	&print_header($readonly,($supervisor ? "Supervisor ($supervisor)":""));

  __d( "Inspecting user " . $pname );

  if($nr>0)
  {
  my $old_email=$email;
  my $newcomment = &param('newcomment');
  my $newmessage=&param('newmessage');

  if((param('mailpassword') eq "on"))
 	{ &send_password($dbh,$uid);
	   print "<br><b>password sent to $email</b>\n";
  	}
  {
  # logged in segment,   
  if(&logon($dbh,$pname,$passwd,$supervisor,$spassword))
  {   #trying to make changes, supplied the correct password
     my $opstr = "UPDATE players SET ";
     my $comma = "";
     my $superedit = ($readonly || !$supervisor)?0:1;
	 if((param('unbounce') eq 'on'))
     { $opstr .= "$comma e_mail_bounce='0',e_mail_bounce_date=null";
       $comma = ","; 
       print "<br>email bounce count reset\n";
     }
     if(!($passwd eq $newpasswd) && !($newpasswd2 eq "") && ($newpasswd eq $newpasswd2))
       { print "<br>changed password\n"; # from $passwd to $newpasswd\n";
        my $slashname = $dbh->quote($newpasswd);
        $opstr = $opstr . $comma . " pwhash=MD5(concat($slashname,uid))";
        $comma = ", ";
        #obsolete password=
        #$opstr = $opstr . $comma . " password=$slashname";
        $passwd=$newpasswd;
        &changePhp($uid,$pname,$passwd);	
      }
  {
	  #print "Password $passwd<br>";
	  #get the rest of the interesting data from the database record for this user  
	  #my $lmsg = '';
	  # police name length and content
	  my $lname = length($newpname);
	  if($lname>0)
		{if(($lname<3) || ($lname>10) || !&validname($newpname))
		 { # don't print anything, just let them wonder why it didn't work
		   # print "<b>Naughty $lname $newpname Naughty!<br></b>"; 
		   $newpname=''; 
		 }
	  }}

    
      if(!($name eq $newname) && !($newname eq ""))
            { 
            my $ename = &encode_entities(&utfDecode($name));
            my $enew = &encode_entities($newname);
            print "<br>changed name from $ename to $enew\n";
			my $slashname = $dbh->quote(&utfEncode($newname));
        $opstr = $opstr . $comma . " full_name=$slashname";
        $comma = ", ";
        $name=$newname;
      }
      if(!($country eq $newcountry) && !($newcountry eq ""))
            { print "<br>changed country from $country to $newcountry\n";
        my $slashname = $dbh->quote($newcountry);
        $opstr = $opstr . $comma . " country=$slashname";
        $comma = ", ";
        $country=$newcountry;
      }
      if(!($newlanguage eq "") && !($newlanguage eq $lang)) 
	{
	print "<br>changed language from $lang to $newlanguage\n";
	my $slanguage = $dbh->quote($newlanguage); 
	$opstr = $opstr . $comma . "language=$slanguage";
	$comma = ", ";
	$lang = $newlanguage;
	}
      
      if(!($newdiscord eq $disid))
	{
	print "<br>changed discord id from $disid to $newdiscord\n";
	my $sdis = $dbh->quote($newdiscord);
	$disid = $newdiscord;
	$opstr = $opstr . $comma . "discorduid=$sdis" ;
	$comma = ", ";
      }
      if(!($city eq $newcity) && !($newcity eq ""))
            { print "<br>changed city from $city to $newcity\n";
        my $slashname = $dbh->quote($newcity);
        $opstr = $opstr . $comma . " city=$slashname";
        $comma = ", ";
        $city=$newcity;
      }
      if(!($email eq $newemail) && !($newemail eq ""))
       { print "<br>changed email from $email to $newemail\n";
        my $slashname = $dbh->quote($newemail);
        $opstr = $opstr . $comma . " e_mail=$slashname,e_mail_bounce='0',e_mail_bounce_date=null";
        $comma = ", ";
        $email=$newemail;
      }
      if(!($newnomail eq $nomail))
      {
        $nomail=$newnomail;
        my $nv = ($nomail eq 'y') ? 'y' : 'n';
        $opstr = $opstr . $comma . " no_email='$nomail'";
        $comma = ", ";
       print "No email flag set to $nv\n";
      }
      if(!($newpname eq $pname) && !($newpname eq ""))
       { if(&findpname($dbh,$newpname))
         {
         print "<br><b>Sorry, the login name $newpname is already in use</b><br>\n";
         }
         else
         {
         print "<br>changed login name from $pname to $newpname<br>\n";
         my $slashname = $dbh->quote($newpname);
         $opstr = $opstr . $comma . "player_name=$slashname" ;
         if(!(lc($newpname) eq lc($pname)))
           { my $slashold=$dbh->quote($pname);
             &commandQuery($dbh,"INSERT into name_change SET uid=$uid,oldname=$slashold,newname=$slashname,time=UNIX_TIMESTAMP()");
           }
         $comma = ", ";
         $update_pname = $newpname;
         }
       }
       
       my $msg = &edit_gametable($dbh,$uid,$superedit);
       if($msg) { print $msg; }
       
     if(!($comment eq $newcomment))
      {print "Changed comment to: $newcomment<br>";
       my $slm = $dbh->quote($newcomment);
       $opstr .= $comma . "comment=$slm";
       $comma = ",";
       $comment = $newcomment;   
       # note that this preceeds "$stuff" and will not trigger an email
      }

     if(!($message eq $newmessage))
      {print "Changed message to: $newmessage<br>";
       my $slm = $dbh->quote($newmessage);
       $opstr .= $comma . "message=$slm";
       $comma = ",";
       $message = $newmessage;
       # note that this preceeds "$stuff" and will not trigger an email
      }

      my $stuff="";
      my $deleted = 0;
      if($supervisor && !($newidentity eq $identity) && !($newidentity eq ''))
	  {
	   print "<br>identity changed from $identity to $newidentity<br>\n";
	   $opstr = $opstr . $comma . "identity='$newidentity'";
	   $comma = ", ";
	   $identity = $newidentity;
	  }
      if($supervisor && !($newmaster eq $master) && !($newmaster eq ""))
      {  print "<br>master changed from $master to $newmaster<br>\n";
        $opstr = $opstr . $comma . "is_master='$newmaster'" ;
        $comma = ", ";
        $stuff .="New master: $newmaster (was $master)\n";
        $master = $newmaster;
      }
	
    if($supervisor && !($status eq $newstatus) && ($newstatus ne ''))
    {  print "<br>status changed from $status to $newstatus<br>\n";
        $opstr = $opstr . $comma . "status='$newstatus'" ;
            $comma = ", ";
        $stuff .="New Status: $newstatus (was $status)\n";
        $status = $newstatus;
		$hasNewStatus=1;
    }
    
    if($supervisor && !($istm eq $newtournament) && ($newtournament ne ''))
    {  print "<br>tournament manager changed from $istm to $newtournament<br>\n";
        $opstr = $opstr . $comma . "is_tournament_manager='$newtournament'" ;
            $comma = ", ";
        $stuff .="New Tournament Manager: $newtournament (was $istm)\n";
        $istm = $newtournament;
    }
     if($supervisor && !($translator eq $newtranslator) && ($newtranslator ne ''))
    {  print "<br>Translator manager changed from $translator to $newtranslator<br>\n";
        $opstr = $opstr . $comma . "is_translator='$newtranslator'" ;
            $comma = ", ";
        $stuff .="New Translator: $newtranslator (was $translator)\n";
        $translator = $newtranslator;
    }
     if($supervisor && !($issuper eq $newsuper) && ($newsuper ne ''))
    {  print "<br>Supervisor changed from $issuper to $newsuper<br>\n";
        $opstr = $opstr . $comma . "is_supervisor='$newsuper'" ;
        $comma = ", ";
        $stuff .="New Supervisor: $newsuper (was $issuper)\n";
        $issuper = $newsuper;
    }
    	 if((param('deleterequest') eq 'on') && !(lc($pname) eq 'guest'))
	 {	my $quid = $dbh->quote($uid);
	    my $cmess = $dbh->quote("deleted by user $pname at $email");
	    $opstr .= "$comma status='deleted', player_name=$quid, comment = $cmess, e_mail = ''";
	    $comma = ", ";
	    $stuff .= "account for $pname at $email deleted\n";
	    print "<br>account for $pname at $email deleted<br>";
	    $deleted = 1;
	 }

    # ready to make changes
      if(!($comma eq ""))
      {  $opstr = $opstr . " WHERE player_name='$pname'";
     # print "Q: $opstr<br>";
     &commandQuery($dbh,$opstr);
     if(!(lc($pname) eq lc($update_pname)))
       { __d("name change from $pname to $update_pname");
         open( F_OUT, ">>$'name_change_log" );
         printf F_OUT "%s %s\n", &date_string(time()),"name change from $pname to $update_pname";
         close F_OUT;
      }
    if($deleted) { $newpname = "$uid"; }
    
    # if name changed, change the picture file too
    if(!($pname eq $update_pname))
     { my ($picname) = $ENV{'DOCUMENT_ROOT'}.$'image_dir.lc($pname).".jpg";
       my ($newpicname) = $ENV{'DOCUMENT_ROOT'}.$'image_dir.lc($update_pname).".jpg";
    if(-e $newpicname) { unlink($newpicname); print "remove $newpicname<br>\n"; }
    if(-e $picname) { rename($picname,$newpicname); print "rename $picname to $newpicname<br>\n"; }
	if($'php_database)
	{ # if we have a phpbb, try to keep the user names in sync
	  my $qp = $dbh->quote($pname);
	  my $qn = $dbh->quote($update_pname);
	  my $db = $'php_database;
	   my $q = "update ${db}.phpbb_users set username=$qn where username=$qp";
	  if($deleted==0) { print "Your forum ID also changed from $pname to $update_pname<br>"; }
	  else { $newpname = ""; }
	  &commandQuery($dbh,$q);
	 }

	}

	  if($hasNewStatus && $'php_database)
	  {	my $db = $'php_database;
	    my $qn = $dbh->quote($update_pname);
		my $sth = &query($dbh,"select user_id from ${db}.phpbb_users where username=$qn");
	    # 
		# attempt to keep banned users out of the forum too
		#
	    if(&numRows($sth)>0)
		{
		my $phu = &nextArrayRow($sth);
		if($phu)
		{
		my $qphu = $dbh->quote($phu);
		if($newstatus eq 'banned')
		{
		&commandQuery($dbh,"insert into ${db}.phpbb_banlist set ban_userid=$qphu");
		}
		else
		{
		&commandQuery($dbh,"delete from ${db}.phpbb_banlist where ban_userid=$qphu");
		}
		}
		}
		
		&finishQuery($sth);

	  }

    $pname=$update_pname;
    my $bcc = (($supervisor eq "")&&(lc($master) eq 'y')) ? "bcc: $'supervisor_email\n" : "";

    if($stuff || ( !($comma eq '') && ($status eq 'unconfirmed')))
      { &send_changes(($supervisor?$'supervisor_email:$old_email),$email,$pname,$name,$country,$city,
         ($supervisor?"Supervisor $supervisor ":""),$stuff,$bcc,$nomail,$status,$uid);
      }
      } # end of got interesting data
     }  # end of login valid
  else
  { #login invalid
   if(!($passwd eq ""))
        {  print "<p>Sorry!  The password you supplied is not valid<br>\n";
      print "Use your browser's <b>BACK</b> to correct your entry and try again<p>\n";
     return;
      }
    }
     
    # end of logged in segment 
    }

  if(!$readonly)
	{ &print_form_header();
	}
    print "<input type=hidden name=editable value=$editable>\n";
    print "<h2>" . &trans("Details for #1",$pname) . "</h2><p>\n";
    print "<input TYPE=hidden name=pname value='$pname'>\n";
    print "<table>\n";
    if(!$readonly)
     { my $msg = &trans("Player name:");
	   print "<tr><td>$msg</td>\n";

   	 my $nameeditable =  (lc($master) eq 'y') ? $supervisor : 1;
   	 if($nameeditable)
     	{ 
		 my $msg = &trans("You can change your player name (you will keep your ranking)");
         print "<td><input TYPE=text NAME=newpname VALUE=\"$pname\" SIZE=10 MAXLENGTH=10></td>\n";
 	   		 print "<td>$msg.</td></tr>\n";
      	}
     	else
     	{	print "<td><b>$pname</b>";
         print "<input type=hidden name=newpname value=\"$pname\">\n";
		 my $msg = &trans("Masters cannot change their nicknames.");
     		print "<td>$msg</td></tr>\n";
     		
     	}
	 my $msg  = &trans("New Password:");
     print "<tr><td>$msg</td>\n";
     print "<td><input TYPE=password NAME=newpasswd VALUE=\"\" SIZE=20 MAXLENGTH=25></td>\n";
	 my $msg = &trans("Type in a new password to change your current one.");
	 print "<td>$msg</td></tr>\n";
	 my $msg = &trans("Confirm New Password:");
     print "<tr><td>$msg</td>\n";
     print "<td><input TYPE=password NAME=newpasswd2 VALUE=\"\" SIZE=20 MAXLENGTH=25></td>\n";

	     }
       
       
       
     print "<tr>\n";
     print "<td >\n";
	 print &trans("Full Name");
	 print ":</td>\n";
	 { my $ename = &encode_entities($name);
	   if($readonly)
	   	{print "<td>$ename</td>\n";
	   	}else
	   	{print "<td><input TYPE=text NAME=fullname VALUE=\"$ename\" SIZE=20 MAXLENGTH=25></td>\n";
	   	}
	 }

     my $flagim = &flag_image($country);
     print "<td align=left rowspan=3><img src=\"$flagim\" alt=\"$country\"></td>";
    
     print "</tr><tr>";
     
     print "<td>" . &trans("City") . ":</td>\n";
     if($readonly)
        	{
        	 print "<td>$city</td>\n";
        	}else
        	{
		      print "<td><input TYPE=text NAME=city VALUE=\"$city\" SIZE=20 MAXLENGTH=25></td>\n";
        	}

     print "</tr><tr><td>" . &trans("Country") . ":</td>\n";
     if($readonly)
        	{
        	 print "<td>$country</td>";
        	}else
        	{
		   print "<td >";
		   &print_country_selector($country);
		   print "</td>\n";
        	}

      my $tlang = &trans($lang);
      print "<tr><td>" . &trans("Language:") . "</td>";
      if($readonly)
	{ print "<td>$tlang</td></tr>\n"; }
	else
	{ print "<td>" ;
          &select_language_menu($lang,0) ;
	  print "</td></tr>\n";
	}

   print "<tr><td>" . &trans("Email address") . ":</td>\n";
     
     my $bouncemess = $bounce_date
						? &trans("Emails to this address are bouncing (at #1)",$bounce_date)
						: &trans("Emails to this address are bouncing");
     my $bb = ($bounce>0)
		? "<br><b>" . $bouncemess . "</b>" 
		: "";

     if($readonly)
       {my $nom = ($nomail eq 'y') ? "&nbsp;No newsletter emails will be sent" : "";
	    my $hemail = &obfuscateHTML("$email");
        print "<td colspan=2>$hemail$nom$bb<td></tr>";
  	my $tdis = ($disid eq "") ? &trans('regular email') : &trans('somewhere else');
	print "<tr><td>" . &trans("Notifications to:") . "</td><td>$tdis <a href=/english/about_discord.html>" 
			. &trans("about notifications") . "</a></td></tr>";

  
	print "<tr><td colspan=2>";
	if(!$locked)
	{
	&print_form_header();
	print "<input type=hidden name=pname value='$pname'>\n";
        print &trans("Forgot your password? Tick this box #1 and press this #2 button to reset it",
		"<input TYPE=checkbox NAME=mailpassword>",
		"<input type=submit value='"
		. &trans("Change Password")
		. "'>");

	print "</form>";
	}

       }else
       {
        my $chk = ($nomail eq 'y') ? "checked" : "";
        print "<td colspan=2>"
			. &obfuscateHTML("<input TYPE=text NAME=email VALUE='$email' SIZE=50 MAXLENGTH=50>")
			. "$bb";
		if($bounce>0)
		{ print "<br><input type=checkbox name=unbounce>unbounce"
		}
        print "<input type=checkbox name=nomail $chk> If checked, send no announcements";
       print "</td></tr>\n";
   
	print "<tr><td>" . &trans("Notifications to:") . "</td><td>"
			 . "<input type=text size=20 name=discorduid value='$disid'>" 
			 . "<a href=/english/about_discord.html> " 
			 . &trans("Notifications") . "</a></td></tr>";
 
	
     print "<tr><td>"
	 . &trans("Current Password")
	 . ":</td>\n";
     print "<td><input TYPE=password NAME=passwd VALUE=\"\" SIZE=20 MAXLENGTH=25></td>\n";
     print "<td>"
	 . &trans("You must supply your password to make changes!")
	 . "<br>";
       print &trans("Forgot your password? Tick this box #1 and press this #2 button to change it",
		"<input TYPE=checkbox NAME=mailpassword>",
		"<input type=submit value='"
		. &trans("Change Password")
		. "'>");
     print "</td></tr>\n";
     
     print "<tr>";
     my $delmsg = &trans("this can't be undone, so you had better mean it");
     print "<td></td><td></td><td><i>$delmsg";
     my $delreq = &trans("delete my account");
     print "<input type=checkbox name=deleterequest>$delreq</i></td>";
	 print "</tr>\n";
     if($supervisor)
         {
           print "<tr><td>Supervisor Password</td><td><input name=spassword type=password value='' SIZE=20 MAXLENGTH=25></td></tr>\n";
		   print "<tr><td>note: also supply supervisor's personal password</td></tr>";
         }
     	}

    {
    # tell about proposals
    if($readonly)
    { 
      if($message)
      {
      my $umessage = &escapeHTML($message);
      print "<tr><td>&nbsp;</td></tr><tr><td >" . &trans("Player's Message") . ":</td>";
      print "<td align=left colspan=2>$umessage</td>\n";
      print "</tr>\n";
      }
      else
      { print "<tr><td>"
		. &trans("No Player Message")
		. "</td></tr>\n";
      }
    }
    else
    { $message =~ s/\"/\\\"/g;
	  my $msg = &trans("Leave a message for the world:");
      print "<tr><td>&nbsp;</td></tr><tr><td >$msg</td>";
      print "<td align=left colspan=2><textarea name=newmessage rows=3 cols=50>$message</textarea></td>\n";
      print "</tr>\n";
     }

   if($supervisor)    # present the supervisor comment
      { 
        if($readonly)
        { print "<tr><td align=left colspan=3><br><b>"
			. &trans("Supervisor comment") 
		. ":</b>";
		 my $ucomment = &escapeHTML($comment);
          print "<br>$ucomment</td>\n";
        }
        else
        {print "<tr><td align=left colspan=3><br><b>"
		. &trans("leave a supervisor comment")
		. ":</b>";
		 $comment =~ s/\"/\\\"/g;
          print "<br><textarea name=newcomment rows=3 cols=43>$comment</textarea></td>\n";
        }    
        print "</tr>\n";
     }


    print "<tr><td>&nbsp;</td></tr><tr><td>" 
			. &trans("Supported Proposal") 
			. " :</td><td colspan=2>";
    if($proposal>0)
      { my $pt = &escapeHTML($proposal_title);
        print "<a href=/cgi-bin/proposals.cgi?view=$proposal>#${proposal}: $pt</a>";
      }
    else { print "None - \t"; print "<a href=/cgi-bin/proposals.cgi>Add your support to a proposal</a>";
	  }}
 
   # tell about name changes in the past
   { my $sth = &query($dbh,"SELECT oldname,newname,time from name_change where uid=$uid order by time desc");
         my $nr = &numRows($sth);
         if($nr>0)
         {my $msg="Past Names:";
          while(-- $nr >= 0)
           { print "<tr><td>$msg</td>";
             my ($old,$new,$time) = &nextArrayRow($sth);
             my $dtime = &date_string($time,1);
             print "<td>from $old to $new on $dtime</td>\n";
             $msg = "";
           }
         print "</tr>\n"
         }
       &finishQuery($sth);
       }

	  print "</table>\n";
	if(!$readonly)
    {
	print "<P>";  
	my $msg = &trans("Process request");
	print "<input TYPE=button NAME=name VALUE=\"$msg\" onClick=\"checkEditForm(this.form)\">\n";
    }
    my $superedit = ($readonly || !$supervisor)?0:1;
    if(!$readonly)
	{
	  if($supervisor)
	  	{ print "<input type=hidden name=super value='$supervisor'>";
  		}
		 my $msg = &trans("Other Statistics for #1",$pname);
		 print "<p><h2>$msg</h2>\n";
	   print "<table>\n";
		}
     print "<tr><td>\n";
     print "<table>\n";
	my $date = &date_string($date_joined,1);
	print "<tr><td>"
		. &trans("Registered on") 
		. ":</td><td>$date</td></tr>\n";
	$date = &date_string($last_played,1);
	my $dago = &timeago($last_played);
	my $ldate = &date_string($last_logon,1);
	my $ltago = &timeago($last_logon);
	my $notimezone = $timezone_offset eq "";
	my $minus = &timezoneOffsetString($timezone_offset);

	my $tzdesc = $notimezone ? &trans("unknown") : "GMT$minus";
	print "<tr><td>" . &trans("Last Seen") . ":</td><td>$ldate ($ltago)</td></tr>\n";
	print "<tr><td>" . &trans("Last Played") . ":</td><td>$date ($dago)</td></tr>\n";
	print "<tr><td>" . &trans("Last Timezone") . ":</td><td>$tzdesc</td></tr>\n";
	print "<tr><td>" . &trans("Games Played") . ":</td><td>$games_played</td></tr>\n";
	print "<tr><td>" . &trans("UID") . "</td><td>$uid</td></tr>\n";
	if(!$superedit)
  	{
 	if($supervisor)
	 { print "<tr><td>" . &trans("Identity") . "</td><td>$identity</td></tr>\n";
	 }
 	my $m = (lc($master) eq 'y') ? 'Y' : 'N';
	  print "<tr><td>" . &trans("Master?") . " :</td><td>$m</td></tr>\n";
 	}
  	else
  	{
	print "<tr><td>Identity (use -1 to reset)</td><td><input type=text name=newidentity value='$identity'></td></tr>\n";

    &print_capability($dbh,"newmaster","Master?","is_master",$master); 
	&print_capability($dbh,"newtranslator","Translator?","is_translator",$translator);
	&print_capability($dbh,"newtournament","Tournament Manager?","is_tournament_manager",$istm);
	&print_capability($dbh,"newsuper","Supervisor?","is_supervisor",$issuper);  
	&print_capability($dbh,"newstatus","status","status",$status);

   }
   
  &alias_table($dbh,$uid,$identity,$supervisor);
  print "<tr><td colspan=2>";
  {
	my $fav = &favorite_games($dbh,$uid,90,4); 
	if($fav)
	{
	my $msg = &trans("Recently favorite games:");
	print "$msg";
	my $key;
	my @words = split(/ /,$fav);
	foreach $key (@words)
	{
	print " ";
	print &trans($key);
	}
	print "<br>";
	}
  }
  &gametable($dbh,$uid,$superedit,!$readonly,$pname);
  my $cook = &timezoneCookie();
  &show_activity_table($dbh,$uid,0,&timezoneCookie());
  print "</td></tr>";



   print  "</table>\n";

	my ($lcname) = lc($pname);
	
  if(!$readonly)
  	{print "</form>\n";
  	}

	print "<table><tr><td>";
	my $root=$ENV{'DOCUMENT_ROOT'};
 
  # picture this
	if(-e "$root/$'image_dir$lcname.jpg")
	 { print "<img src=\"$'image_dir$lcname.jpg\">";
    if(!$readonly)
	{print "<br><a href=/$language/pictureupload.html>"
		. &trans("Click here to upload a new picture")
		. "</a>";
	}
  }else
   { print &trans("No Picture");
   print "<br><a href=/$language/pictureupload.html>"
		. &trans("Click here to upload your picture")
		. "</a>";
   }
  
  print "</td></tr></table>\n";

  } # end of name valid
  else
  { #print the entry form
  if(!($pname eq ""))
  {print "<br><b>"
	. &trans("User #1 doesn't exist.",&encode_entities($pname))
	. "</b>\n";
  }
  my $msg = &trans("Please type in your player name or the name you'd like to inspect.");
  print "<p>" . $msg . "<p>\n";
  }  # end of readonly segement
  &print_form_header();

  if($supervisor)
  	{ print "<input type=hidden name=super value='$supervisor'>";
	  }
   print "<table>\n";

  print "<tr><td>" . &trans("Player Nickname") . " :</td>\n";
   print "<td><input TYPE=text NAME=pname VALUE=\"$pname\" SIZE=10 MAXLENGTH=10></td></tr>\n";
   print "</table>\n";

  print "<p><input TYPE=submit NAME=name VALUE=\"" . &trans("Get Info") . "\">\n";
  print "<input type=checkbox name=editable>" . &trans(" Edit") . "\n";
  print "</form>\n";
   &disconnect($dbh);
}}

do_gs_edit();

__dEnd( "end!" );
