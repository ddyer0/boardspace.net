#!/usr/bin/perl
#
#
# logon     Validate user's Player Name and password for java web start
#
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use CGI::Cookie;
use HTML::Entities;

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

use Crypt::Tea;

sub init {
	$| = 1;				# force writes
}

sub make_log {
	my ( $msg ) = @_;
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
	open( F_OUT, ">>$::server_logon_log" );
 	printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] %s\n", 1900+$year,$mon+1,$mday,$hour,$min,$sec,$msg;
        close F_OUT;
}

sub check_and_start_server
{	my ($username,$usernum,$forjws)=@_;
   my $server_status = &check_server($username,$usernum);
   if(!$server_status)
   {
       my $test = (param('test') eq 'true') && !($::class_dir eq $::test_class_dir);
       my $val = $test ? `$::test_server_start` : `$::server_start`;
       __dStart( "$::debug_log", $ENV{'SCRIPT_NAME'} );
       __d("server down attempting restart: $::server_start = $? ($val)");
       sleep(2);
       $server_status = check_server($username,$usernum);
       if (! $server_status )
       { __dm( __LINE__." Server Down" );

	if($forjws)
	{ print "<param name=failed value='Lobby Unavailable'>\n";
	}
	else
	{
	print <<Unavailable;
	<html>
	<title>
	Lobby Unavailable
        </title>
        <h1>Lobby Unavailable<hr>
        </h1>
       The Lobby is currently unavailable. Please try again in a little while.<p>
       [ <a href="javascript:window.history.go(-1)">Return to previous
       page.</a> ]
Unavailable
     }}
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
  unless (socket(SOCK, PF_INET, SOCK_STREAM, $::proto))
  {
    my $msg = scalar(localtime) . ": socket: $!\n";
    open LOG, ">>$::perl_log";
    print LOG $msg;
    close LOG;
    return 0;
  }
  my $test = (param('test') eq 'true') && !($::class_dir eq $::test_class_dir);
  my $num = inet_aton($::ip_name);
  my $port = $test ? $::test_server_port : $::game_server_port;
  unless (connect(SOCK,sockaddr_in($port, $num))) 
    {
    my $numx = ord(substr($num,0,1)) . "." . ord(substr($num,1,1)) 
        . "." . ord(substr($num,2,1)) .  "." . ord(substr($num,3,1));
    my $msg = scalar(localtime) . ": connect: port $::ip_name($numx):$port $!\n";
    open LOG, ">>$::perl_log";
    print LOG $msg;
    close LOG;
    return 0;
  }
	srand();
	my $rand1=int(rand(255.99));
	my $rand2=int(rand(255.99));
	my $rand3=int(rand(255.99));
	my $rand4=int(rand(255.99));
	$::serverKey="$rand1.$rand2.$rand3.$rand4";
  print SOCK "200 -1 $username#$usernum $::serverKey\n";
  close(SOCK);
  return 1;
}

sub print_applet()
{ my ($dbh,$fav,$pname,$uid,$languageName,$uidrank,$haspicture,$country,$latitude,$logitude,
      $pclass,$played,$timec,$banner,$bannermode,$forjws) = @_;
  my $language=$languageName."Strings";
  my $use_class_dir = param('classes');
  my $use_jdk = param('jdk');
  my $test = (param('test') eq 'true') && !($::class_dir eq $::test_class_dir);
  my $testserver = $test ? "<param name=testserver value=true>\n" : "";
  my $stealth = (($bannermode eq 'S')&&(&param('stealth') eq 'true'))
	? "<param name=stealth value=true>\n"
	: "";

   if($use_jdk eq "") { $use_jdk = $::jdk; }
   if($use_class_dir eq "") { $use_class_dir = $::class_dir; }
   if($test) { $use_class_dir = $::test_class_dir; }

 my $rootclass = $::root_applet;
 my $jarclass = $::jar_collection;
 my $java = param('java');
  my $challenge = $::challengevalue;
  my $extra = $::extramouse;
  if(param('extra')) { $extra='true'; }
  if($challenge) { $challenge = &challengeValue(); }
  if(!($java eq "")) { $java = "<param name=jdk value=$java>\n"; }
  if(!($banner eq ""))
  {  $banner = "<param name=bannerString value=\"$banner\">\n";
  }
  if(!($bannermode eq ""))
  {  $banner .="<param name=bannerMode value='$bannermode'>\n";
  }
  my $dd = ($pname eq 'ddyer') 
      ? "<param name=extraactions value=true>"
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
	$gameparams .= "<param name='reviewerdir$idx' value='$dir'>\n"
  }
  &finishQuery($sth);
  }
	if($fav) { $fav = "<param name=favorites value='$fav'>"; }
   my $port = $test ? $::test_server_port : $::game_server_port;
   my $endpart = $forjws ? "" : "<h1>Sorry, you don't have java!</h1>\n"
		. "<p><b>Boardspace requires java. Follow <a href='needsjava.html'>this link</a> for advice on how to get\n"
		. "java set up in your browser.</b><p></applet>\n";
		
	my $firstpart = $forjws ? "" : "	<applet codebase=http://$ENV{'HTTP_HOST'}/$::java_dir/$use_class_dir archive=$jarclass code=$rootclass width=1 height=1>\n"
		. "	<!-- *** -->\n";
   print <<End;
	$firstpart
	<param name=servername value="$ENV{'HTTP_HOST'}">
	<param name=localIP value=$ENV{'REMOTE_ADDR'}>
	<param name=serverKey value="$::serverKey">
	$java 
	$banner 
	$stealth 
	$fav 
	$testserver
	<param name=lobbyportnumber value="$port">
	<param name=robotlobby value="solo">
	<param name=username value="$pname">
	<param name=time-correction value="$timec">
	<param name=chatpercent value=30>
	<param name=uid value=$uid>
	<param name=uidranking value="$uidrank">
	<param name=picture value="$haspicture">
	<param name=country value="$country">
	<param name=playerclass value=$pclass>
	<param name=gamesplayed value=$played>
	<param name=latitude value=$latitude>
	<param name=logitude value=$logitude>
	<param name=language value="$language">
	$gameparams
  $dd
  <param name=extramouse value="$extra" comment="extra mouse actions in the lobby">
  <param name=final value=true>
  $endpart

End

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


sub standard_stuff()
{  my ($dbh,$pname,$language)=@_;
   &standard_header();
   my $languageName = &select_language($language);
   my $root = $ENV{'DOCUMENT_ROOT'};
   print "<HTML><BASE HREF=\"http://$ENV{'HTTP_HOST'}/$languageName/login.shtml\">\n";
   my $myplace = &geobutton(730);  # 938 for testing also prints some stuff
   print "<script language=javascript>var \$myname='$pname';var \$myplace=\"$myplace\";</script>\n";
#   print "<applet width=1 height=1><h1>Sorry, you don't have java!</h1><p>"
#         . "<b>Boardspace requires java."
#         . "Follow <a href='needsjava.html'>this link</a>"
#         . " for advice on how to get java set up in your browser.</b><p></applet>";
   my $localpage = &dircat($root,"$languageName/background-page.html");
   if(!(-e $localpage)) { $localpage = &dircat($root,"english/background-page.html") };
   if(-e $localpage) 
    {
    &insert_file($localpage); 
    }
   print "<center>";
   &readtrans_db($dbh);

   print "<table CELLSPACING=2 align=top><tr>";
  
   print "<td align=left valign=top>"; 
    &show_recent(5);
   print "<br>";
    &do_messageboard($dbh,$pname,'','','',1); 
   print "</td>";
  
   print "<td>";
     &show_ordinal_rankings($dbh,10,$::retire_months);
   print "</td>";
   print "</tr></table>";

   &top_players_table($dbh,0,$language,$pname,@'top_player_variations);
   print "</center>";
   &standard_footer();
}



sub logon_frame()
{
	my ($pname, $passwd,$cookie,$language,$forjws) = @_;
	my $frame = param('frame');
	if($forjws || ($frame eq '')) { $frame='no'; }
	if($frame eq "main")
	{
	print header;
	my $dbh = &connect($ENV{'REQUEST_URI'});
    &standard_stuff($dbh,$pname,$language);
	&disconnect($dbh);
	}else
	{
	&logon($pname,$passwd,$frame,$cookie,$language,$forjws);
	}
}

sub logon()
{ 
 my ($pname, $passwd,$frame,$cookie,$language,$forjws) = @_;
 my $languageName = &select_language($language);
 my $dbh = &connect($ENV{'REQUEST_URI'});
 if($dbh)
    {
	&readtrans_db($dbh);
	my $slashpname = $dbh->quote($pname);
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $isok = 0;
	my $bannercookie = &bannerCookie();	
	{
	my $sth0 = &query($dbh,"SELECT is_supervisor,locked,password,identity,is_robot,uid,num_logon,country,latitude,logitude,games_played,is_master,status "
										. " FROM players WHERE (status='ok' or status='banned') and player_name=$slashpname");
	my $n = numRows($sth0);
	#
	# CHECK that SELECT matched only ONE player
	#
	if (($n == 1) && &allow_ip_login($dbh,$myaddr) && &allow_ip_login($dbh,$bannercookie))
	{#
   my ($is_supervisor,$locked,$pass,$identity,$robot,$uid,$num_logon,$country,$latitude,$logitude,$games_played,$is_master,$status) = nextArrayRow($sth0);
   my $cookiename = &encode_entities("login$uid");
   my $cookieval = '';
   #
   # note, if he is banned let him log in anyway, so the banning contagion will
   # spread to any other identity he tries to log in on
   #
   my $bannermode = (lc($is_supervisor) eq 'yes') 
			? "S"
			: ($status eq 'banned') ? "Y" : "N";
   # supply password from cookie
   if(!$passwd)
    {  my %cookies = fetch CGI::Cookie();
       my $pcook = $cookies{$cookiename}; 
       if($pcook) 
         { $cookieval = $pcook->value;
           $passwd = substr(&decrypt($cookieval,$::tea_key),10); 
         }
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

   my $pstr = substr(time,0,10) . $passwd;
   my $cpass = &encode_entities(&encrypt($pstr, $::tea_key));
   my $rpass = &decrypt($cpass,$::tea_key);
   # check password;
   if( ($pass eq '') || !(lc($passwd) eq lc($pass)))
    { $n=-1; # password mismatch
     #print header; 
     #print "set cookie uid=$uid cookie = $cookiename - $cpass<br>\n";
     #print " found cookie : $cookieval<br>\n";
     #print " src $pstr<br> enc $cpass<br>dec $rpass<br>\n";
    }
    else
    {
     
     # set new cookie with password and name. 
     my $bpass = encode_entities(&encrypt($bannercookie,$::tea_key));
     my $bcookie = new CGI::Cookie(-name=>'client',-value=>$bpass,-expires=>'+8d');
     if(!(lc($locked) eq 'y'))
      {
      print "Set-Cookie: $bcookie\n";
      if($cookie) 
       { my $pwcookie = new CGI::Cookie(-name=>$cookiename,-value=>$cpass,-expires=>'+10y');
         print "Set-Cookie: $pwcookie\n"; 
       }
      }
	 #
	 # set username and language cookies
	 #
	 my $ncookie = new CGI::Cookie(-name=>'nickname',-value=>$pname,-expires=>'+1d');
	 my $lcookie = new CGI::Cookie(-name=>'language',-value=>$language,-expires=>'+1d');
	 print "Set-Cookie: $ncookie\n";
	 print "Set-Cookie: $lcookie\n";
     print header; 
     #print "set cookie uid=$uid cookie = $cookiename - $cpass<br>\n";
     #print " found cookie : $cookieval<br>\n";
     #print " enc $cpass<br>dec $rpass<br>\n";
	 #check for email
	 #
	 $isok = 1;

	 my $mq = "SELECT stamp from email where toplayer=$slashpname order by stamp";
	 my $sth2 = &query($dbh,$mq);
	 my $nr2 = &numRows($sth2);
	 if($nr2>0)
		{ my ($stamp)=$sth2->fetchrow;
			my $url = "'/cgi-bin/bs_sendmessage.cgi?toname=$pname&stamp=$stamp'";
		  print "<META HTTP-EQUIV=REFRESH CONTENT=\"3; url=$url\">";
			print "We interrupt this program for an important message from the management...<p>";
			print "Go to <a href=$url>this link</a><p>";
			&finishQuery($sth2);
		}
		else
		{
     &finishQuery($sth2);
	 #
	 #
	 my $uidrank = "";
   my $latupdate = "";
   my ($pclass) = ($is_master eq 'y')?2 : ((0>=900) ? 1 : ($games_played>100?0:-1));
  
   if($::geocode)
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


	my $server_status = &check_and_start_server($pname,$uid,$forjws);

	if($server_status)
      { my $root = $ENV{'DOCUMENT_ROOT'};
	  my $stealth = &param('stealth');
	  if($stealth) { $stealth="&stealth=$stealth"; }
	my $args = "&pname=$pname&password=$passwd$stealth";

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
	
	my $imagename=$ENV{'DOCUMENT_ROOT'}.$::image_dir.lc($pname).".jpg";
	my $haspicture;
		if(-r $imagename) { $haspicture="true"; }
		else { $haspicture="false"; }
	if(!$forjws) { print "<HTML><BASE HREF=\"http://$ENV{'HTTP_HOST'}/$languageName/login.shtml\">\n"; }
	if(($frame eq "yes") || ($frame eq "no"))
	{

        my $host=$ENV{'REMOTE_ADDR'};
        my $hostname = gethostbyaddr(inet_aton($host),AF_INET);
        my $qb = (lc($locked) eq 'y') ? 'identity' : $dbh->quote($bannercookie);
        &make_log( $pname."\t$ENV{'HTTP_ACCEPT_LANGUAGE'}($languageName)\t$num_logon\t"."${hostname}($host)"."\t".$ENV{'HTTP_USER_AGENT'} );
        &record_coincidences($dbh,$uid,$last_logon,$host);
        &record_cookie_coincidences($dbh,$uid,$last_logon,$bannercookie);  
	      &commandQuery($dbh,"UPDATE players SET identity=$qb,num_logon=$num_logon, last_ip='$host',last_logon=$last_logon$latupdate "
										. "WHERE uid=$uid");

       if($frame eq "yes")
       {my $now = time();
       	my $some_args = "&pname=$pname&language=$language";
        # adding uid=$time is an attempt to thwart caching of the applet pane
        print "<frameset rows='100%,0' border=0 framespacing=0>\n";
        print "<frame name=main src=\"http://$ENV{'HTTP_HOST'}$ENV{'SCRIPT_NAME'}?frame=main$some_args\">\n";
        print "<frame name=applet src=\"http://$ENV{'HTTP_HOST'}$ENV{'SCRIPT_NAME'}?frame=applet&uid=$uid$args\" scrolling=no noresize>\n" ;
	#
	#redirect browsers without frames to a noframes page
	#
	print "<noframes>";
	print "<META HTTP-EQUIV=REFRESH CONTENT=\"0; url='$ENV{'SCRIPT_URI'}?frame=no$args'\">\n";
	print "</noframes>";

	print "</frameset>\n"; 

       } elsif($frame eq "no")
    {
    #noframes version.  
    if(!$forjws)
    {
    print "<script language=javascript>var \$myname='$pname';</script>\n";
    &standard_stuff($dbh,$pname,$language);
    }
    &print_applet($dbh,$fav,$pname,$uid,$languageName,$uidrank,$haspicture,$country,
          $latitude,$logitude,$pclass,$games_played,$timec,$bannercookie,$bannermode,$forjws);
    }

  } 
   elsif ($frame eq "applet")
	{
	 #print "<META HTTP-EQUIV=Pragma CONTENT=no-cache>\n";
	 print "<body background=/images/background.jpg>\n";
   &print_applet($dbh,$fav,$pname,$uid,$languageName,$uidrank,$haspicture,$country,$latitude,
                       $logitude,$pclass,$games_played,$timec,$bannercookie,$bannermode,$forjws);
	} 
   }

    }
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
	if($forjws)
	{
	my $msg = &trans("Unrecognized player name or password");
	print "<param name=failed value='$msg'>\n";
	}
	else
	{
	print header;
    &standard_header();

	print "<h2>"
		. &trans("Login Failed")
		. "</hr><p>";

	if($pname && !$passwd)
	{
		print &trans("The login cookie for \"#1\" has expired.",
			     &encode_entities($pname));
	}
	else
	{
		print &trans("NoLoginMessage",&encode_entities($pname),&encode_entities($passwd));
	}
	print "<br><a href='/$language/login.shtml'>"
		. &trans("Log in")
		. "</a>";
	print "<br><a href='/$language/register.shtml'>"
		. &trans("Register")
		. "</a>";
	&standard_footer();
	}}

	&disconnect($dbh);
	}
}

#
# the main program starts here
# --------------------------------------------
#
&init();

# print start_html('Logon');
if( param() ) 
{	my $forjws = param('jws');
	my $pname = param('pname');
	 $pname = &despace(substr($pname,0,25));
	my $passwd = param('password');
	 $passwd = &despace(substr($passwd,0,15));
    my $cookie = param('cookie');
	my $language = &select_language(param('language'));
	&logon_frame($pname, $passwd, $cookie,$language,$forjws);
}
