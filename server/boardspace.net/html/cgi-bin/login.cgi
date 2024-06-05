#!/usr/bin/perl
#
#
# logon     Validate user's Player Name and password CGI program!
#
# major revision 1/2013 for java web start. The general philosophy is to
# keep exactly the same logic and flow, except that the various outcomes
# get printed in special ways for java web start.
#
# revision 11/2020 to add the option of passing all parameters as an
# encrypted package, and also to remove the obsolete login cookies
# and browser-based login, which is gone forever with java applets
# when client version 4.82 is obsolete, we should also change the
# insist on the packaged/encrypted login to prevent a security downgrade attack.
#
# sample param 
# params=rRPQ4eBxj8JC5Y2RPMNc20mquUwzMwpgWfKHQNy8zaUYS/Jwk39prylwj7eeqrmW4/jLJepgpsSgo253pLzlxjVkFbsM9oo/Z2vkV7baOcffQYJ6xP57rOmDaoEatVYgO+5AURcUloMczIB9WFQHS9MCWpJ3a64Q0lQKGj7UvLi+T02fhvG2N19vxjraGRAIq9rVS+RCaOMulfxu
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use HTML::Entities;
use URI::Escape;
use Mysql;
use Debug;
use Socket;
use strict;
require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/getLocation.pl";
require "tlib/lock.pl";
require "tlib/top_players.pl";
require "tlib/favorite-games.pl";
require "tlib/show-recent.pl";
require "tlib/messageboard.pl";
require "tlib/ordinal-rankings.pl";
require "tlib/params.pl";


sub init {
	$| = 1;				# force writes
}

sub make_log {
	my ( $msg ) = @_;
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
	open( F_OUT, ">>$'server_logon_log" );
 	printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] %s\n", 1900+$year,$mon+1,$mday,$hour,$min,$sec,$msg;
        close F_OUT;
}

sub check_and_start_server
{  my ($username,$usernum)=@_;
   my $server_status = &check_server($username,$usernum);
   if(!$server_status)
   {
       my $test = (param('test') eq 'true') && !($'class_dir eq $'test_class_dir);
       my $val = $test ? `$'test_server_start` : `$'server_start`;
       __dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
       __d("server down attempting restart: $'server_start = $? ($val)");
       sleep(2);
       $server_status = check_server($username,$usernum);
       if (! $server_status )
       { __dm( __LINE__." Server Down" );
       }
       else
       { __dm(__LINE__." Server restarted");
	   }
	  __dEnd();
	}
    return($server_status);
}

#
# check to see if the server is up, and try to launch it if not
# log problems!
#
sub check_server
{ my ($username,$usernum)=@_;
  unless (socket(SOCK, PF_INET, SOCK_STREAM, $'proto))
  {
    my $msg = scalar(localtime) . ": socket: $!\n";
    open LOG, ">>$'perl_log";
    print LOG $msg;
    close LOG;
    return 0;
  }
  my $test = (param('test') eq 'true') && !($'class_dir eq $'test_class_dir);
  my $num = inet_aton("localhost");
  my $port = $test ? $'test_server_port : $'game_server_port;
  unless (connect(SOCK,sockaddr_in($port, $num))) 
    {
    my $numx = ord(substr($num,0,1)) . "." . ord(substr($num,1,1)) 
        . "." . ord(substr($num,2,1)) .  "." . ord(substr($num,3,1));
    my $msg = scalar(localtime) . ": connect: port $'ip_name($numx):$port $!\n";
    open LOG, ">>$'perl_log";
    print LOG $msg;
    close LOG;
    return 0;
  }
	srand();
	my $rand1=int(rand(255.99));
	my $rand2=int(rand(255.99));
	my $rand3=int(rand(255.99));
	my $rand4=int(rand(255.99));
	$'serverKey="$rand1.$rand2.$rand3.$rand4";
  print SOCK "200 -1 $username#$usernum $'serverKey\n";
  # this is an attempt to force the socket data to actually
  # be received before we leave.  Under some odd circumstances
  # the output-only socket isn't received in a timely fashion
  # and the user login can get there first!
  flush SOCK;
  my $data = {0};
  my $n = '0'+sysread SOCK,$data,1;
  close(SOCK);

  return 1;
}
sub print_jws_applet()
{ my ($dbh,$fav,$pname,$uid,$languageName,$uidrank,$haspicture,$country,$latitude,$logitude,
      $pclass,$played,$timec,$bannermode) = @_;
  my $language=$languageName."Strings";
  my $use_class_dir = param('classes');
  my $cheerpj = lc(param('cheerpj')) eq 'true';
  my $guestnamepar = param('guestname');
  my ($guestname) = split(' ',$guestnamepar);
  my $test = (param('test') eq 'true') && !($'class_dir eq $'test_class_dir);
  my $testserver = $test ? "testserver=true\n" : "";
  my $stealth = (($bannermode eq 'S')&&(param('stealth') eq 'true'))
	? "<param name=stealth value=true>\n"
	: "";

   if($use_class_dir eq "") { $use_class_dir = $'class_dir; }
   if($test) { $use_class_dir = $'test_class_dir; }

  my $extra = $'extramouse;
  my $feat = $'features;
  if(param('extra')) { $extra='true'; }
  my $banner="";  # this used to be part of a cookie based protol to exclude undesirables
  if(!($bannermode eq ""))
  {  $banner .="bannerMode=$bannermode\n";
  }
  my $dd = ($pname eq 'ddyer') 
      ? "extraactions=true\n"
       : "";
  
  # 
  # get the game directories from the database variations table
  #
  my $gameparams="";
  {
  my $q = "select directory_index,directory from variation where included=1 "
			. " group by directory_index order by directory_index ";
  my $sth = &query($dbh,$q);
  my $num = &numRows($sth);
  while($num -- > 0)
  {
	my ($idx,$dir) = &nextArrayRow($sth);
	$gameparams .= "reviewerdir$idx=$dir\n"
  }
  &finishQuery($sth);
  }
	if($fav) { $fav = "favorites=$fav\n"; }
   my $actualPort = ($test ? $'test_server_port : $'game_server_port);
   my $port = $cheerpj
		? ($test ? $'cheerpj_test_server_port : $'cheerpj_game_server_port)
		: $actualPort;
   my $guestnameout = (($pname eq 'guest') && $guestname) ? "guestname=$guestname\n" : "";
   my $msg = utfEncode("$banner$fav$testserver$dd
codebase=/$'java_dir/$use_class_dir/
documentbase=/$languageName/
servername=$ENV{'HTTP_HOST'}
gameservername=$'ip_name
localIP=$ENV{'REMOTE_ADDR'}
serverKey=$'serverKey
lobbyportnumber=$port
reallobbyportnumber=$actualPort
robotlobby=solo
username=$pname
time-correction=$timec
chatpercent=30
uid=$uid
uidranking=$uidrank
picture=$haspicture
country=$country
playerclass=$pclass
gamesplayed=$played
latitude=$latitude
logitude=$logitude
language=$language
$gameparams
extramouse=$extra
$guestnameout$feat
final=true
");
	my $cs = &simplecs($msg);
	my $ll = length($msg);
	$msg = "len=$ll\ncalc=$cs\n" . $msg  ;
	print &encode64($msg);
}

#
# record coincidencident logins using the same ip address.  These
# might be real coincidences, or two people sharing a session, or
# two people sharing a machine.  Or they might be evidence of one
# person with two identities playing themselves.
#
sub record_coincidences()
{  my ($dbh,$uid,$last_logon,$ip) = @_;      
   my $rind = rindex($ip,'.');
   my $rip = $rind>0 ? substr($ip,0,$rind+1) : $ip;
   my $qrip = $dbh->quote("$rip%");
   my $qip = $dbh->quote($ip);
   #
   # detect login within an hour from a similar ip
   #
   my $q = "SELECT uid,last_ip FROM players "
   	   	      . " WHERE uid!=$uid "
        		. " AND (last_logon>($last_logon-60*60)) "
        		. " AND (last_ip like $qrip)";
   my $sth = &query($dbh,$q);
   my $nr = &numRows($sth);
   if($nr == 0)
   { # no similar logins, so count a non-coincident login
     my $q = "UPDATE coincident_ip set count=count+1,last_ip=$qip,last_date=CURRENT_TIMESTAMP"
   	. " WHERE uid1=$uid AND uid2=0 and type='NoMatch'" ;
     my $sth2 = &query($dbh,$q);
     my $nr = &numRows($sth2);
     if($nr==0)
     { my $q = "INSERT INTO coincident_ip set count=1,type='NoMatch',last_ip=$qip,last_date=CURRENT_TIMESTAMP,uid1=$uid,uid2=0";
       &commandQuery($dbh,$q);
     }
     &finishQuery($sth2);
   }
   else
   {
   # coincidence detected
   while($nr-- > 0)
   {	my ($co_uid, $co_ip) = &nextArrayRow($sth);
   	my $type = ($co_ip eq $ip) ? "Exact" : "Close";
   	my $q = "UPDATE coincident_ip SET last_date=CURRENT_TIMESTAMP,count=count+1,last_ip=$qip "
		. " WHERE type='$type' and ((uid1=$uid and  uid2=$co_uid) OR (uid2=$uid AND uid1=$co_uid))";
   	my $sth2 = &query($dbh,$q);
   	if(&numRows($sth2) == 0)
   	{ my $q = "INSERT into coincident_ip set last_ip='$ip',uid1=$uid,uid2=$co_uid,count=1,last_date=CURRENT_TIMESTAMP,type='$type'";
   	  &commandQuery($dbh,$q);
   	}
   	&finishQuery($sth2);
   }
   }
   &finishQuery($sth);
}
#
# record coincidences based on machine cookies.  These are
# defininite clues that more than one person is using the
# same browser at some time.  We also try to propogate the
# identity so it sticks even if they later change browsers 
# of machines.
#
sub record_cookie_coincidences()
{  my ($dbh,$uid,$last_login,$cookie) = @_;      
   #
   # detect login with the same cookie
   #
   my $qc = $dbh->quote($cookie);
   my $q = "SELECT uid,identity FROM players "
                . " WHERE uid!=$uid "
            . " AND (last_logon>($last_login-24*60*60)) "
            . " AND is_robot is NULL"
            . " AND (identity=$qc)";
   my $sth = &query($dbh,$q);
   my $nr = &numRows($sth);
   if($nr == 0)
   { # no similar logins, so count a non-coincident login
     my $q = "UPDATE coincident_ip set count=count+1,last_ip=$qc,last_date=CURRENT_TIMESTAMP"
     . " WHERE uid1=$uid AND uid2=0 and type='NoMatch'" ;
     my $sth2 = &query($dbh,$q);
     my $nr = &numRows($sth2);
     if($nr==0)
     { my $q = "INSERT INTO coincident_ip set count=1,type='NoMatch',last_ip=$qc,last_date=CURRENT_TIMESTAMP,uid1=$uid,uid2=0";
       &commandQuery($dbh,$q);
     }
     &finishQuery($sth2);
   }
   else
   {
   # coincidence detected
   while($nr-- > 0)
   {  my ($co_uid, $co_ip) = &nextArrayRow($sth);
     my $type = "Exact";
     my $q = "UPDATE coincident_ip SET last_date=CURRENT_TIMESTAMP,count=count+1,last_ip=$qc "
    . " WHERE type='$type' and ((uid1=$uid and  uid2=$co_uid) OR (uid2=$uid AND uid1=$co_uid))";
     my $sth2 = &query($dbh,$q);
     if(&numRows($sth2) == 0)
     { my $q = "INSERT into coincident_ip set last_ip=$qc,uid1=$uid,uid2=$co_uid,count=1,last_date=CURRENT_TIMESTAMP,type='$type'";
       &commandQuery($dbh,$q);
     }
     &finishQuery($sth2);
   }
   }
   &finishQuery($sth);
}




#
# print the "unavailable" message after trying and failing
# to restart the server.
#
sub doServerDown()
{
	print "unavailable\n";
}
sub timezoneParameter()
{
	my $tz = &param('timezone');
	if( (length($tz)>1) && (substr($tz,0,1)=='x')) { return(substr($tz,1)); }
	return('');
}
sub identityParameter()
{	my $id = &param('identity');
	if( (length($id)>1) && (substr($id,0,1)=='x')) { return(substr($id,1)); }
	return('');
}
sub logon()
{ 
 my ($pname, $clearpasswd,$cookie,$language) = @_;
 my $languageName = &select_language($language);
 my $dbh = &connect($ENV{'REQUEST_URI'});
 if($dbh)
    {
	&readtrans_db($dbh);
	&bless_parameter_length($pname,20);
	&bless_parameter_length($clearpasswd,20);
	&bless_parameter_length($language,20);
	my $slashpname = $dbh->quote($pname);
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $isok = 0;
	my $bannercookie = &identityParameter();	

	#__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
  
	{
	my $qpass = $dbh->quote($clearpasswd);
	my $q = "SELECT is_supervisor,locked,pwhash,MD5(concat($qpass,uid)),identity,is_robot,uid,num_logon,country,latitude,logitude,games_played,is_master,status "
										. " FROM players WHERE (status='ok' or status='banned') and player_name=$slashpname";
	#__d("Q $clearpasswd $q");
	my $sth0 = &query($dbh,$q);
	my $n = numRows($sth0);
	#
	# CHECK that SELECT matched only ONE player
	#
	if (($n == 1) && &allow_ip_login($dbh,$myaddr) && &allow_ip_login($dbh,$bannercookie))
	{#
   my ($is_supervisor,$locked,$pass,$passwd,$identity,$robot,$uid,$num_logon,$country,$latitude,$logitude,$games_played,$is_master,$status) = nextArrayRow($sth0);
   # set this again as we may not have read it yet
   #
   # note, if he is banned let him log in anyway, so the banning contagion will
   # spread to any other identity he tries to log in on
   #
   my $bannermode = (lc($is_supervisor) eq 'yes') 
			? "S"
			: ($status eq 'banned') ? "Y" : "N";
   # supply password from cookie
   if($robot eq 'g')
   {	$passwd = $pass;	#guarantee login for the guest
   }

   
   #if the cookie got lost, try to give him back the same one
   if($identity==-1) 
	{ # fresh identity
	  $bannercookie = $identity = 0; 
	}
   if(!$robot && !$bannercookie)
    { $bannercookie = $identity;
    }
   # if no cookie assign a new one
   if(!$bannercookie) 
     { # assign them so if printed as an IP address, they will be 0.xx.xx.xx;
      $bannercookie = time & 0xffffff; 
     }

   my $pstr = substr(time,0,10) . $clearpasswd;

   # check password;
   if( ($pass eq '') || !(lc($passwd) eq lc($pass)))
    { $n=-1; # password mismatch
    }
    else
    {
	 print header; 

 	 #check for email
	 #
	 $isok = 1;

	 my $mq = "SELECT stamp from email where toplayer=$slashpname order by stamp";
	 my $sth2 = &query($dbh,$mq);
	 my $nr2 = &numRows($sth2);
	 if($'jws_message)
	 { print "message\n$'jws_message\n"
	 }
	 else
	 {
	 if($nr2>0)
		{
		 my ($stamp)=$sth2->fetchrow;
		 my $estamp = uri_escape($stamp);
		 my $epname = uri_escape($pname);
		 my $url = "http://$ENV{'HTTP_HOST'}/cgi-bin/bs_sendmessage.cgi?toname=$epname&stamp=$estamp";
		 print "message\n$url\n";
		}
		else
		{
	 #
	 #
	 my $uidrank = "";
   my $latupdate = "";
   my ($pclass) = ($is_master eq 'y')?2 : ((0>=900) ? 1 : ($games_played>100?0:-1));
   if($'geocode)
   {
   my %location = &getLocation($ENV{'REMOTE_ADDR'});
    my $myplace;
	 if(%location && ($location{'certainty'}>20))
	 	{ $latitude = $location{'latitude'};
	 		$logitude = $location{'longitude'};
	 		if($latitude && $logitude)
	 		{ $latitude = $dbh->quote($latitude);
	 			$logitude = $dbh->quote($logitude);
	 			$latupdate = ", latitude=$latitude, logitude=$logitude";
	 		}
	 	}
	}

	#
	# ALL ready to LOGON!
	#
	$num_logon++;
    my $last_logon = time();	# num seconds since Jan 1, 1970
	my $fav = &favorite_games($dbh,$uid,90,3);
    #add the robots' rank
    my ($sth2) = &query($dbh,"SELECT players.uid,value,variation FROM players left join ranking "
          . " on ranking.uid=players.uid WHERE is_robot='y' OR players.uid=$uid ");
    {my $nrows = &numRows($sth2);
     my $i=0;
     while($i<$nrows)
	  	{ my ($pl,$ra,$va) = &nextArrayRow($sth2);
	  	  if(($ra eq "") || ($ra eq "0") ) { $ra = "New"; }
      $va = &gamename_to_gamecode($dbh,$va);
      if($va) { $uidrank .= "$pl $va $ra "; }
	  	  $i++;
	  	}
	}
    &finishQuery($sth2);


	my $server_status = &check_and_start_server($pname,$uid);

	if(!$server_status)
	{ &doServerDown();
	}
	else
      { my $root = $ENV{'DOCUMENT_ROOT'};
	  my $stealth = &param('stealth');
	  if($stealth) { $stealth="&stealth=$stealth"; }
	my $args = "&pname=$pname&password=$clearpasswd$stealth";

	# other params
	my $jdk = param('jdk');
	my $classes = param('classes');
	my $java = param('java');
	if($java) { $args .="&java=$java"; }
	if($classes) { $args .="&classes=$classes"; }
	if($jdk){ $args .= "&jdk=$jdk"; }
	if(param('extra')) { $args .="&extra=1"; }
	if($languageName) { $args .="&language=$languageName" }

	my $timec = 0;
	{ my $sth = &query($dbh,"SELECT value from facts where name='time-correction'");
	  if($sth) { ($timec) = &nextArrayRow($sth); &finishQuery($sth); }
    }
	
	my $imagename=$ENV{'DOCUMENT_ROOT'}.$'image_dir.lc($pname).".jpg";
	my $haspicture;
		if(-r $imagename) { $haspicture="true"; }
		else { $haspicture="false"; }

	{

        my $host=$ENV{'REMOTE_ADDR'};
        my $hostname = gethostbyaddr(inet_aton($host),AF_INET);
        my $qb = (lc($locked) eq 'y') ? 'identity' : $dbh->quote($bannercookie);
        my $jw = "";
        my $qlang = $dbh->quote($languageName);
        my $timezone = &timezoneParameter();
        my $qtz = $dbh->quote($timezone);
        my $tz = $timezone ? "timezone_offset=$qtz," : "";
	my $plat = param('platform');
	if(!($plat eq "")) { $plat = $plat . " "; };
        &make_log( $pname."\t$jw$ENV{'HTTP_ACCEPT_LANGUAGE'}($languageName)\t$num_logon\t"."${hostname}($host)"."\t". "$plat" . $ENV{'HTTP_USER_AGENT'} );
        &record_coincidences($dbh,$uid,$last_logon,$host);
        &record_cookie_coincidences($dbh,$uid,$last_logon,$bannercookie); 
        my $q = "UPDATE players SET language=$qlang,$tz identity=$qb,num_logon=$num_logon, last_ip='$host',last_logon=$last_logon$latupdate "
										. "WHERE uid=$uid";
	      &commandQuery($dbh,$q);
#__dStart( "$'debug_log", $'parent_script );
#__d($q);
#__dEnd();
 
    {
    #noframes version.  
    print "applet\n";
      &print_jws_applet($dbh,$fav,$pname,$uid,$languageName,$uidrank,$haspicture,$country,$latitude,
                       $logitude,$pclass,$games_played,$timec,$bannermode);
    }

  } 
  
   }

    }
    }
    &finishQuery($sth2);
  }}
  
  if($n!=1)  
	{ print header;
		&note_failed_login($dbh,$myaddr,"IP: game login as $pname");
		&note_failed_login($dbh,$bannercookie,"CK: game login as $pname"); 
		&countEvent("failed login",100,200);
	}
	
	&finishQuery($sth0);
	}
	 
	if(!$isok)
	{
	print "failed\n";
	}

	&disconnect($dbh);
	}
}



#
# the main program starts here
# --------------------------------------------
#
&init();
#print header;
# print start_html('Logon');
if( param() ) 
{	&logForm("login");
    #__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
	# return true if we're not using combined params
	# or if the combined params were parsed and validated
	my $ok = &useCombinedParams($'tea_key);
	if($ok && checkChecksumVersion())
	{
	my $pname = param('pname');
	my $forjws = param('jws');
	$pname = &despace(substr($pname,0,25));
	my $passwd = param('password');
	$passwd = &despace(substr($passwd,0,25));
	__d("user $pname\n");
	#__d("pass $passwd");
        my $cookie = param('cookie');
	my $language = &select_language(param('language'));
	&logon($pname,$passwd,$cookie,$language);
	}
}
