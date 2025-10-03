#!/usr/bin/perl
#
# 1/2021 created from register.cgi, to use encrypted interface, and to handle ONLY app based registration
#
# 1/22/2009 added checks based on timestamps and additional &note_failed_login
# logic to thwart spam registration attempts for "sara1234" youtubegal1234@gmail
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;
use Debug;

require "include.pl";

require "tlib/common.pl";
require "tlib/timestamp.pl";
require "tlib/gs_db.pl";
require "tlib/params.pl";

#
# the actual information providers set error and result strings.
#
var $::error = '';
var $::result = '';

sub init {
	$| = 1;				# force writes
}


#
# some problem with registration
#
sub regError()
{	my ($msg) = @_;
	$::error = $::error . $msg . "\n";
}

# version that only works in unix
sub send_welcome()
{ my ($email,$pname,$uid) = @_;
  my $regsub = &trans("BoardSpace.net registration");
  my $language = &select_language();
  my $auth = &userAuthenticator($pname);
  my $regmsg = &trans("Thank you for registering at Boardspace.net.  To complete registration, click on this link:");

                open( SENDMAIL, "| $::sendmail -f $::from_email $email" );

#
# note, the exact form of the link is known to 
# the "$::bounce_targets list used by procmail
#
                print SENDMAIL <<Mail_header;
$auth
Sender: $::from_email
From: $::from_email
To: $email
Subject: $regsub

$regmsg

 http://www.boardspace.net/cgi-bin/confirm_register.cgi?pname=$pname&uid=$uid&language=$language

Mail_header
        close SENDMAIL;
  $::result = "ok\n";

}

#
# generally speaking, we do a one-time operation to create a user
# in the phpbb database with the same email and password.  After
# that, the two are mainted independently using their own mechanisms
# except that when boardspace players are renamed, the php name is changed
# too.  This detail is handled in the edit script.
#
sub setup_php()
{
	my ($user,$pass,$email,$date,$status) = @_;
	my $db_type = $::db_type;
	my $database= $::php_database;
	my $db_host = $::db_host;
	my $ok = 1;
	if($database)
	{
	my $dbh = DBI->connect("DBI:${db_type}:${database}:${db_host}",$::db_user,$::db_password);
	if($dbh)
	{

	my $qname = $dbh->quote($user);
	my $qpass = $dbh->quote($pass);
	my $qmail = $dbh->quote($email);
	my $qdate = $dbh->quote($date);
	my $que = "select user_id from phpbb_users where username=$qname ";
	my $sth = &query($dbh,$que);
	my $num = &numRows($sth);
	my $formsg = &trans("Use your boardspace username and password to access the #1 forums #2",
				"<a href=/BB/>","</a><p>");
	if($num==0)
	{	my $sth2 = &query($dbh,"select max(user_id) from phpbb_users");
		my $nr2 = &numRows($sth2);
		my ($max) = ($nr2==0) ? 0 : &nextArrayRow($sth2);
		&finishQuery($sth2);
		if($max>0)
		{
		my $quid = $dbh->quote($max+1);
		my $q = "insert into phpbb_users set username=$qname,"
				. "user_regdate=$qdate,"
				. "user_password=MD5($qpass),user_email=$qmail,user_id=$quid";
		&commandQuery($dbh,$q);
		if($status eq 'banned')
			{	&commandQuery($dbh,"insert into phpbb_banlist set ban_userid=$quid");
			}
		}
	} else
	{	#
		# note, we sometimes retire player names from the boardspace db
		# if these names are recycled, they get reused in the bb.  This shouldn't
		# be a big problem.  We deal with it here by giving the recycled user the new password
		# and email.  Most likely there were no posts under the old name anyway
		#
		my $quid = $dbh->quote($que);
		my $q = "update phpbb_users set user_regdate=$qdate,user_password=MD5($qpass),user_email=$qmail "
				. " where username=$qname";
		&commandQuery($dbh,$q);

		if($status eq 'banned')
			{	&commandQuery($dbh,"insert into phpbb_banlist set ban_userid=$quid");
			}
			else
			{	&commandQuery($dbh,"delete from phpbb_banlist where ban_userid=$quid");
			}

	}
	&finishQuery($sth);
	&disconnect($dbh);
	}
	else { $ok = 0; }
	
	} else { $ok = 0; }
	return($ok);
}

#
# here to register a new player in the database, and then log him in
#
sub register()
{
  my ($dbh,$pname, $passwd, $passwd2, $name, $email, $country) = @_;
	my %cookies = fetch CGI::Cookie();
	my $bannercookie = $cookies{'client'};
	my $myaddr = $ENV{'REMOTE_ADDR'};
	if($bannercookie)
    {  $bannercookie = &decrypt($bannercookie->value,$::tea_key);
    } 
	if(&allow_ip_register($dbh,$myaddr)
       && &allow_ip_register($dbh,$bannercookie))
	{
	my $slashpname = $dbh->quote($pname);
	my $q = "SELECT full_name FROM players WHERE player_name=$slashpname";
	my $sth = &query($dbh,$q);
	if($sth)
	{
	my $numrows = &numRows($sth);

	my ($full) = &nextArrayRow($sth);
	#
	# front load these error message lookups so the all get into the database
	#
	my $invalid =  "nouser";
	my $nameinuse =  "nouser" ;
	my $blankpw = "nopassword";
	my $difpw = "nopassword";

	&finishQuery($sth);

	if( (length($pname)>10) || (length($pname)<3) || !&validname($pname) )
	{ #tell them it's taken if the name is invalid.  This can only
	  #happen if someone is trying to cheat
	  &note_failed_login($dbh,$myaddr,"IP: Registration invalid name '$pname'");
	  &note_failed_login($dbh,$bannercookie,"CK: Registration invalid name '$pname'");
	  &regError($invalid);
	}
	elsif( $numrows > 0 )
	{
	 &regError($nameinuse);
	}
	elsif( $passwd eq "" )
	{ &regError($blankpw);
	}
	elsif( $passwd  ne $passwd2  )
	{ &regError($difpw);
	}
	else
	{
	#
	# Add new user
	#
	my $date_joined = time();	# num seconds since Jan 1, 1970

	my $slashname = $dbh->quote(&utfEncode($name));
	my $slashpname = $dbh->quote($pname);
	my $slashemail = $dbh->quote($email);
	my $slashpasswd = $dbh->quote($passwd);
	my $slashcountry = $dbh->quote($country);
	#obsolete password=
	my $q = "INSERT INTO players SET full_name=$slashname,player_name=$slashpname,"
    . "e_mail=$slashemail,"
    #. "password=$slashpasswd,"
    . "date_joined=$date_joined,comment='',"
    . "country=$slashcountry";
	my $success=&commandQuery($dbh,$q)
				 &&	&setup_php($pname,$passwd,$email,$date_joined,'');
	if($success)
	{	
	    my $sth = &query($dbh,"select uid from players where player_name=$slashpname");
	    if(&numRows($sth)==1)
	    { my ($uid) = &nextArrayRow($sth);
	      my $quid = $dbh->quote($uid);
	      &finishQuery($sth);
	      &commandQuery($dbh,"update players set pwhash=MD5(concat($slashpasswd,uid)) where uid=$quid");
	      &send_welcome($email,$pname,$uid);
	    }
	}
	else
	{
	&regError("Registration failed!");
	}
    }}
}
}


#
# the main program starts here
# --------------------------------------------
#

sub doRegister()
{	my ($dbh) = @_;

	my $fullname = param('fullname');
	 if('' eq $fullname) { $fullname = param('realName'); }
	$fullname = &decode_entities($fullname);
	my $pname = param('pname');
		$pname = &despace(substr($pname,0,25));
	my $password = param('password');
		$password = &despace(substr($password,0,25));
	my $password2 = param('password2');
		$password2 = &despace(substr($password2,0,25));
	my $email = param('email');
	my $country = param('country');
	
	&bless_parameter_length($fullname,60);	#assess for penalty
	&bless_parameter_length($pname,20);		#assess for penalty
	&bless_parameter_length($password,20);	#assess for penalty
	&bless_parameter_length($password2,20);	#assess for penalty
	&bless_parameter_length($email,80);		#assess for penalty
	&bless_parameter_length($country,40);	#assess for penalty
	my $l1 = length($password);
	my $l2 = length($password2);
	__d("reg $ENV{'REMOTE_ADDR'} name='$pname' pw='<$l1 chars>' pw2='<$l2 chars>' full='$fullname' em='$email' country='$country'");

	&readtrans_db($dbh);

	my $stamp = param('timestamp');
	&bless_parameter_length($stamp,40);	#assess for penalty
	
	# note, this is used by the apps, which don't refresh their timestamps after
	# every interaction, so it's not suitable to update to the stronger checks
	if(&check_timestamp($stamp))
	{
	if ( $email && $pname && $password && ($password eq $password2))
		{# registration
		&register($dbh,$pname, $password, $password2, $fullname, $email, $country);
		&countEvent("registration",50,100);
        }
	}
	else
	{
	my %cookies = fetch CGI::Cookie();
	my $bannercookie = $cookies{'client'};
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $qpname = $dbh->quote($pname);
	__d("failed bad timestamp");
	&note_failed_login($dbh,$myaddr,"IP: bad register timestamp '$qpname' ");
	&note_failed_login($dbh,$bannercookie,"CK: bad register timestamp '$qpname'");
	&countEvent("spam registration attempt",50,200);
	&regError("Registration failed (code 2)!");
	}
}

#
# input is a single parameter "params" which is base64, encrypted, checksummed
#
sub doit()
{	__dStart( "$::debug_log",$ENV{'SCRIPT_NAME'});;

  	print header;

	if( param() ) 
	{
	my $ok = &useCombinedParams($::tea_key,1);
	if($ok)
	{
	my $dbh = &connect();
	if($dbh)
	{
		my $myaddr = $ENV{'REMOTE_ADDR'};	
		if( &allow_ip_access($dbh,$myaddr))
		{
		&doRegister($dbh);	
		}
		else {
			$::error = "database connect refused";
		}
		&disconnect($dbh);
	}
	else 
	{
	$::error = "database connect failed";
 	}}
 	
 	else { $::error = "invalid main input parameters"; }
 	}
 	else { $::error = "no input parameters"; }
 	 	
 	if($::error)
 	{	&printResult("Error",$::error);
 	}
 	else
 	{
 		&printResult("Ok",$::result);
 	}

  __dEnd( "end!" );
}


$| = 1;                         # force writes

&doit();
