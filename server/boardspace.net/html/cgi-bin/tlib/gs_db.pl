use DBI;
use strict;
use Debug;
use Time::HiRes;
use HTML::Entities;
use CGI::Cookie;

#
# common database utilities
#
$::parent_script="gs_db.pl";

#
# scheme for penalizing abusive database users.  Keep two timers for "short time" and "long time"
# which are times in the future.  If "short time" hasn't arrived, delay immediately until it has.
# if "long time" builds up to a certain level, autoban the user.
# when performing queries, accumulate the time used and when disconnecting, set up short time
# and long time "for next time"
#
$::single_ip_ip;						# the ip address associated with this connection
$::standard_database_connection;		# a copy of the standard database connection
$::single_ip_uid=0;					# the ipinfo uid associated with this ip
$::single_ip_query_time=0;			# seconds spent executing queries for this connection
$::long_time_multiplier = 5;			# long term penalty accumulates this much faster than short time.
$::long_time_ban_threshold = 120;	# when long time exceeds now by this amount you get banned.
$::single_ip_penalty_time=0;			# "time" penalty for malformed parameters
$::single_ip_penalty_string='';		# the strings which caused the penalty
$::single_ip_penalty_threshold = 10;	# tolerance for bad parameters

#log errors as messages and in the database exception log

sub showForm()
{
	my @pars = param();
	my $par;
	__d("All parameter names: @pars\n");
	__d("Details:\n");
	foreach $par (@pars)
	{	my $val = param($par);
		if($par eq 'password') { $val = '<xxxx>'; }
		if($par eq 'password2') { $val = '<xxxx>'; }
		__d("$par\t$val");
	}
}
sub printForm()
{
	my @pars = param();
	my $par;
	print "All parameter names: @pars<br>\n";
	print "Details:<br>\n";
	foreach $par (@pars)
	{	my $val = param($par);
		if($par eq 'password') { $val = '<xxxx>'; }
		print "$par\t$val<br>\n";
	}
}
sub logError()
{ my ($context,$com)=@_;
	my $e = "($ENV{'REMOTE_ADDR'} $ENV{'REQUEST_URI'} : $context) $DBI::errstr";
  __dStart( "$::debug_log", $::parent_script );
  __d("Error $com:\n ->$e\n");
  my $trace = &stacktrace();
  __d("$trace");
  &showForm();
  __dEnd();
  #
  # print a good error message if debugging, but not on a live site
  # on a live site, this is probably a sql injection attack, so don't
  # give them a roadmap how to continue
  #
  if($::debug_mysql) 
  { print "Error $com:<br> $e<p>\n"; }
  else
  { print "Sorry, There was a problem processing your request<p>\n"; 
  }
}

sub logSilent()
{ my ($context,$com)=@_;
	my $e = "($ENV{'REMOTE_ADDR'} $ENV{'REQUEST_URI'} : $context) $DBI::errstr";
  __dStart( "$::debug_log", $::parent_script );
  __d("$com:\n ->$e\n");
  __dEnd();
}


sub logMessage()
{ my ($context,$com)=@_;
	my $e = "($ENV{'REMOTE_ADDR'} $ENV{'REQUEST_URI'} : $context)";
  __dStart( "$::debug_log", $::parent_script );
  __dm("$com:\n ->$e\n");
  &showForm();
  __dEnd();
}
sub logForm()
{ my ($context,$com)=@_;
	my $e = "($ENV{'REMOTE_ADDR'} $ENV{'REQUEST_URI'} : $context) $DBI::errstr";
  __dStart( "$::debug_log", $::parent_script );
  __d("$com:\n ->$e\n");
 &showForm();
  __dEnd();
}

sub bless_parameter_length()
{
	my ($str,$xlen) = @_;
	my $len = length($str);
	my $dif = $len-$xlen*2;
	if($dif>0)
	{	$::single_ip_penalty_time += $dif;
		$::single_ip_ip = &ip_to_int($ENV{'REMOTE_ADDR'});
		$::single_ip_penalty_string = ("" eq $::single_ip_penalty_string) ? $str : "$::single_ip_penalty_string\n$str";
	}
}

#connect to the database
sub connect()
{ my ($script)=@_;
	my $db_type = $::db_type;
	my $database= $::database;
	my $db_host = $::db_host;
	if($script!="") {$::parent_script=$script; }
	my $dbh = DBI->connect("DBI:${db_type}:${database}:${db_host};mysql_multi_statements=1",$::db_user,$::db_password);
	if(!$dbh)
	{ &logError("Trying to connect","db: ${database}\@${db_host} user:$::db_user");
	}

	$::standard_database_connection = $dbh;
	return($dbh);
}

%'lastquery='';
%'lastdbh='';
sub datestring()
{
	my (undef,undef,undef,$day,$month,$year) = localtime();
	$month++;
	$year+=1900;
	return "$day/$month/$year:";
}
sub quote()
{
  my ($dbh,$item) = @_;
  if($dbh && $item) { return $dbh->quote($item); }
  return "''";
}
# disconnect from the database
sub disconnect()
{	my ($dbh) = @_;
	if($dbh)
	{
	my $kk;
	if($dbh == $::standard_database_connection)
	{
	#
	# if too much junk was detected in the parameter stream, 
	# just ban this guy.
	if($::single_ip_penalty_time>$::single_ip_penalty_threshold)
	{	# bad guy feeding stuff to the queries
	my $qc = $dbh->quote("bad input $ENV{'SCRIPT_NAME'}: $::single_ip_penalty_string" );
	my $da = $dbh->quote(&datestring());
	if($::single_ip_uid)
		{
		my $quid = $::single_ip_uid;
		my $q = "update ipinfo set status='autobanned',comment=concat(comment,'\n',$da,$qc) WHERE uid=$quid";
		&commandQuery($dbh,$q);
		}
		elsif($::single_ip_ip)
		{
		my $qip = $dbh->quote($::single_ip_ip);
		my $q = "Insert into ipinfo set status='autobanned',min=$qip,max=$qip,comment=$qc"
				. " ON DUPLICATE KEY UPDATE status='autobanned',comment=concat(comment,'\n',$da,$qc)";
		&commandQuery($dbh,$q);
		}	
	}
	
	# if he accumulated enough time to incur a penalty
	# print "Disconnect times $::single_ip_query_time for $::single_ip_ip ($::single_ip_uid)<p>";
	if(($::single_ip_ip!=0) && ($::single_ip_query_time>1.0))
	{
	my $itime = int($::single_ip_query_time);
	my $ltime = $itime*$::long_time_multiplier;
	my $newtime = "short_time=UNIX_TIMESTAMP()+$itime,long_time=GREATEST(long_time+$ltime,UNIX_TIMESTAMP()+$ltime)";
	my $qtime = $dbh->quote("excess time=$itime from $ENV{'SCRIPT_NAME'}");
	my $da = $dbh->quote(&datestring());
	
	if($::single_ip_uid)
		{
		my $quid = $::single_ip_uid;
		&commandQuery($dbh,"update ipinfo set $newtime,comment=concat(comment,'\n',$da,$qtime,'(',long_time-UNIX_TIMESTAMP(),')') WHERE uid=$quid");
		}
		else
		{
		my $qip = $dbh->quote($::single_ip_ip);
		&commandQuery($dbh,"Insert into ipinfo set status='auto',min=$qip,comment=$qtime,max=$qip,short_time=UNIX_TIMESTAMP()+$itime,long_time=UNIX_TIMESTAMP()+$ltime"
				. " ON DUPLICATE KEY UPDATE $newtime,comment=concat(comment,'\n',$da,$qtime,'(',long_time-UNIX_TIMESTAMP(),')')");
		}
	}
	$::standard_database_connection = 0;
	}
	
    for $kk (keys(%'lastquery))
        {
		my $db = $::lastdbh{$kk};
		my $key = $::lastquery{$kk};
        if(($db eq $dbh) && $key) { &logError("query not finished at disconnect:",$key); }
        }
	     $dbh->disconnect();
    }
}

# return an executed query, ready to be interrogated, or null
sub query()
{ my ($dbh,$com) = @_;
  #print "Q: $com\n";
  my $start = Time::HiRes::time();
  my $sth = $dbh->prepare($com);
  $::lastquery{$sth} = $com;
  $::lastdbh{$sth} = $dbh;
  if(!$sth->execute) 
	{ &logError("execute",$com);
	 if(! $::debug_mysql)
	 {
	 my $x = $::debug_mysql;
	 $::debug_mysql = 1;	#prevent recursive errors
	 &note_failed_login($dbh,$ENV{'REMOTE_ADDR'},"IP: failed query - see log");
	 $::debug_mysql = $x;
	 }
	 &finishQuery($sth); 
	 $sth=0;
  }
  my $end = Time::HiRes::time();
  my $tot = ($end-$start);
  $::single_ip_query_time += $tot;
  if($tot>$::db_slow_query_time) 
	{  my $msg = "slow query: ${tot} seconds: $com";
	   if($tot>$::db_very_slow_query_time) { &logMessage("slow query",$msg); }
	   else { &logForm("slow query",$msg); }
	}
  return($sth);
}

sub numRows()
{	my ($sth)=@_;
	return($sth ? $sth->rows : 0);
}
sub nextArrayRow()
{	my ($sth)=@_;
	if(!$sth)
	{
	    &logError("nextArrayRow on null");
	    &CgiDie("nextArrayRow on null");
	}
	else
	{
	my @val = $sth->fetchrow_array; 
	if($sth->err)
	{   my $err = $sth->errstr; 
	    &logError("in nextArrayRow");
	    &CgiDie("in nextArrayRow");
	}
	else
	{
	    return @val;
	}}
}
	
#return true for successful cleanup of a successful command
sub finishQuery()
{	my ($sth)=@_;
	if($sth) 
		{ $::lastquery{$sth} = 0; 
		  $::lastdbh{$sth}= 0;
		  $sth->finish() || &logError("finish","");
		}
	# return the actual $sth, which can still be used to
	# retrieve the last uid created by auto increment
	return($sth);
}

sub last_insert_id()
{ my ($sth) = @_;
  return $sth->{'mysql_insertid'};
}
	
# return true for a sucessful command
sub commandQuery()
{	my ($dbh,$com) = @_;
	my $sth=&query($dbh,$com);
	return &finishQuery($sth);
}
#
# validate a user name and password.  This is the simple version, some places
# use a more complex version that returns more values.
#
sub check_logon()
{ my ($pname, $passwd) = @_;
  my $dbh = &connect("check_logon");              # connect to local mysqld
  my $slashpname = $dbh->quote($pname);
  my $slashpwd = $dbh->quote($passwd);
  my $sth = &query($dbh,"SELECT num_logon FROM players WHERE player_name=$slashpname AND pshash=MD5(concat($slashpwd,uid))");
  my $n = &numRows($sth);
  &finishQuery($sth);
  &disconnect($dbh);
  return($n==1);
}

#
# keep the master system balanced at 1500 by awarding bonus points or deficit points
# which are awarded to the winner of a master game.
#
sub check_bonus()
{	my ($dbh,$game) = @_;
	my $active = time()-60*60*24*30*($::retire_months);
	my $query = "select 1500-sum(value)/count(value) from players left join ranking "
       . " on players.uid=ranking.uid and ranking.variation='$game'"
			. " where (games_won>=$::bonus_games) AND (status='ok') AND (ranking.last_played>$active)";
	my $sth = &query($dbh,$query);
	my ($deficit) = &nextArrayRow($sth);
	&finishQuery($sth);
	#print "Query $query\n Deficit $deficit\n";
	if($deficit>=2) { $deficit=2; }
	elsif ($deficit<=-2) { $deficit=-2; }
	else { $deficit=int($deficit); }
	return($deficit);
}
sub banmenow()
{	my ($from) = @_;
	my $dbh = &connect();
	if($dbh)
	{
	my $ip = $ENV{'REMOTE_ADDR'};
	&banme($dbh,$ip,$from);
	&disconnect($dbh);
	}
}
sub banme()
{	my ($dbh,$ip,$from) = @_;
	my $qip = $dbh->quote(&ip_to_int($ip));
	my $msg1 = "auto-banned ip=$ip for tripping the robot alarm from $from";
	my $da = &datestring();
	my $msg2 = "${da}alarm from $from";
	my $qmsg2 = $dbh->quote($msg2);
	my $q = "INSERT INTO ipinfo SET status='autobanned',rejectcount=1,min=$qip,max=$qip,comment=$qmsg2 "
		. " ON DUPLICATE KEY UPDATE badlogincount=badlogincount+1,rejectcount=rejectcount+1,status='autobanned',comment=concat(comment,'\n',$qmsg2)";
	#print "$q<p>";
	&commandQuery($dbh,$q);

	my $q2 = "SELECT uid,comment from ipinfo where min=$qip and max=$qip ";
	my $sth = &query($dbh,$q2);
	my ($uid,$comm) = &nextArrayRow($sth);
	if(length($comm)>500)
	{	$comm = "..." . substr($comm,index($comm,"\n",10));
		my $quid = $dbh->quote($uid);
		my $qcomm = $dbh->quote($comm);
		&commandQuery($dbh,"update ipinfo set comment = $qcomm where uid=$quid");
	}
	&finishQuery($sth);
	# add an alert which will be sent with email
	# my $qmsg1 = $dbh->quote($msg1);
	# &commandQuery($dbh,"INSERT into messages SET type='alert',message=$qmsg1");
}
sub honeypot()
{	my ($from,$text) = @_;
	if(!$from) { $from=$ENV{'SCRIPT_NAME'}; }
	$from=&encode_entities($from);
	print("<a href='/cgi-bin/noticeme.cgi?from=$from'>$text</a>");
}


#
# convert an ip address to an integer
#
sub ip_to_int()
{ my ($in) = @_;
  if (!$in) { $in = "0"; }
  my ($s1,$s2,$s3,$s4) = split(/\./,$in);
  if(!$s2 && !$s3 && !$s4) 
	{ # our fake ip addresses have to be big.  defend against junk
          return((length($in)>4) ? $in : 0); 
	}  # no dots

  my $val = ($s1+0)<<24 | ($s2+0)<<16 | ($s3+0)<<8 | $s4;
  return($val);
}

#
# convert an integer to an ip address
#
sub int_to_ip()
{ my ($in) = @_;
  my $s4 = $in&0xff;
  $in = $in >> 8;
  my $s3 = $in&0xff;
  $in = $in >> 8;
  my $s2 = $in&0xff;
  my $s1 = $in >> 8;
  my $val = "$s1.$s2.$s3.$s4";
  return($val);
}

#
# return uid of ipinfo row or 0 if access ok
# return -1 if not ok
#

sub allow_ip_access()
{  my ($dbh,$ip) = @_;
   my ($intip) = &ip_to_int($ip);
   if($intip)
   {
   # don't do this with no IP, as happens for the "cookie" pass if there is no cookie
   my $loginok = 1;
   my $ii = $dbh->quote($intip);
   my $bantime = $::bad_login_time*60;
   my $range = "($ii>=min and $ii<=max)";
   my $query = "SELECT status,uid,if((UNIX_TIMESTAMP(changed)+$bantime)>UNIX_TIMESTAMP(),1,0),min,max,UNIX_TIMESTAMP(changed),UNIX_TIMESTAMP(),short_time,long_time from ipinfo where $range";
   my $sth = &query($dbh,$query);
   my $numrows = &numRows($sth);
   my $gooduid = 0;
   
   if(index($ip,'.')>0) { $::single_ip_ip = $intip; }

   while ($numrows>0)
   {   $numrows--;
       my ($st,$uid,$changed,$min,$max,$changedtime,$now,$short_time,$long_time) = &nextArrayRow($sth);
       #print "Ban $st,$uid,$changed ($min - $max)<br>\n";
       if( (($st eq 'autobanned') && $changed) || ($st eq 'banned') )
       {my $dt = $now-$changedtime;
	#&logSilent("reject","reject $changedtime $now $dt");
	if($dt>60)
	{
	#only make new database entries once per minute to avoid floods
	my $uu = $dbh->quote($uid);
        &commandQuery($dbh,"update ipinfo SET rejectcount=rejectcount+1 where $uu=uid");
	}
        $loginok = 0;
       }
       elsif($min eq $max)
       {
       if( ($st eq 'autobanned') || ($st eq 'auto'))
       {
        $gooduid = $uid;
        }
       my $penalty = $short_time - $now;
       my $long_penalty = $long_time - $now;
       
       #print "Short $penalty Long $long_penalty / $short_time $long_time <br>";
       my $da = $dbh->quote(&datestring());

       if($long_penalty > $::long_time_ban_threshold)	# if he accumulates enough penalties, ban him
		{
		my $bancom = $dbh->quote("excess long penalty=$long_penalty");
		my $quid = $dbh->quote($uid);
		&commandQuery($dbh,"update ipinfo set status='autobanned',long_time=0,short_time=0,comment=concat(comment,'\n',$da,$bancom) where uid=$quid");
		my $gooduid = 0;
        }
       
       $::single_ip_uid = $uid;
       if($penalty>0)
		{
		# short time is the earliest time when the guy is allowed to
		# use the database again.  If it's too soon, just delay a while.
		sleep($penalty);
		$::single_ip_start_time = $now+$penalty;
		}
       }
   }
   &finishQuery($sth);
   return($loginok ? $gooduid : -1);
   }
   return(0);	# no ip specified
}
#
# return 1 if this ip address is allowed to login.  
# Consider banned and temporarily banned ip addresses.
# increment counters for acceptable or unacceptable logins
#
sub allow_ip_login()
{  my ($dbh,$ip) = @_;
   my ($intip) = &ip_to_int($ip);
   my ($loginok) = &allow_ip_access($dbh,$ip);
   #print "Allow $ip $loginok<p>";
   if($loginok>0)
   { # count logins in static entries
     my $good = $dbh->quote($loginok);
	 # he was banned or on the watch list
	 # clear him with this successful login
	 #print "q: L delete from ipinfo where $good=uid<p>";
	 &commandQuery($dbh,"delete from ipinfo where $good=uid ");
   }
   return($loginok>=0);
}

#
# return 1 if this ip address is allowed to login.  
# Consider banned and temporarily banned ip addresses.
# increment counters for acceptable or unacceptable logins
#
sub allow_ip_register()
{  my ($dbh,$ip) = @_;
   my ($intip) = &ip_to_int($ip);
   if(!$intip) { return(1); }
   my $bantime = $::bad_login_time*60;
   my $cond = "( ((status='autobanned') and ((UNIX_TIMESTAMP(changed)+$bantime)>UNIX_TIMESTAMP()))"
           . " or (status='banned') or (status='noregister') )";
   my $ii = $dbh->quote($intip);
   my $range = "($ii>=min and $ii<=max) ";
   my $query = "SELECT status,uid from ipinfo where $range AND $cond";
   my $sth = &query($dbh,$query);
   my $numrows = &numRows($sth);
   my $loginok = 1;
   #print "ip=$ip rows = $numrows\n";
   while ($numrows>0)
   {   $numrows--;
       my ($st,$uid) = &nextArrayRow($sth);
       #print "Noregister by $st row $uid\n";
       &commandQuery($dbh,"update ipinfo set rejectcount=rejectcount+1 where uid='$uid'");
       $loginok = 0;
   }
   &finishQuery($sth);
   return($loginok);
}

#
# given a registeree uid and his id cookie, return the name of
# any banned user associated with his email 
#
sub banned_from_registering()
{  my ($dbh,$uid,$ident) = @_;
   my $quid = $dbh->quote($uid);
   my $q = "SELECT e_mail from players where uid=$quid";
   my $sth = &query($dbh,$q);
   my $nsth = &numRows($sth);
   my $val = "";
   if($nsth>0)
   {
	my ($email) = &nextArrayRow($sth);
	my $qemail = $dbh->quote($email);
	my $qident = $dbh->quote($ident);
	my $uidclause = ($ident==0) ? "" : " OR identity=$qident";
	my $q2 = "SELECT player_name from players where status='banned' and (e_mail=$qemail $uidclause)";
	my $sth2 = &query($dbh,$q2);
	if(&numRows($sth2)>0)
	{	($val) = &nextArrayRow($sth2);
	}
	&finishQuery($sth2);
   }
   &finishQuery($sth);
   return($val);
 }



#
#
# when a complete login check from an acceptable IP fails, note the failure.
# when lots of failures occur, temporarily ban the offending address and 
# set an alert, which will be seen in daily emails.
#
sub note_failed_login()
{	my ($dbh,$ip,$info) = @_;
  my ($intip) = &ip_to_int($ip);
  if($intip)
  {
  my $ii = $dbh->quote($intip);
  my $exquery = "SELECT status,uid,unix_timestamp(changed),badlogincount,unix_timestamp(),comment from ipinfo"
			. " WHERE (min=$ii and max=$ii) AND (status='auto' or status='autobanned')";
  my $sth = &query($dbh,$exquery);
  my $nr = &numRows($sth);

  my $query = "UPDATE ipinfo set badlogincount=badlogincount+1,logincount=GREATEST(0,logincount-1)"
				. " WHERE ($ii>=min AND $ii<=max)";
  #print "Q: $nr $query<br>";
  &commandQuery($dbh,$query);

  if($nr==0) 
  	{ # note that a new particular IP failed, distinguished by the status=auto flag.
	my $da = &datestring();
	my $comm = $dbh->quote("$da$info");
	my $q = "INSERT INTO ipinfo SET status='auto',min=$ii,max=$ii,badlogincount=1,comment=$comm"
	. " ON DUPLICATE KEY UPDATE badlogincount=badlogincount+1,comment=concat(comment,'\n',$comm)";
	#print "Q: $q<br>";
	&commandQuery($dbh,$q);
		# clean up temporary data more than a month old
	my $q = "DELETE FROM ipinfo "
				. " WHERE ((UNIX_TIMESTAMP(changed)+60*60*24*30)<UNIX_TIMESTAMP())"
				. " AND (long_time < UNIX_TIMESTAMP())"
				. "  AND ((status='auto') or (status='autobanned') or (status='wasbanned'))";
	#print "Q: $q<br>";
	&commandQuery($dbh,$q);
  	}
  	else
	{
	my ($status,$uid,$changed,$badlogin,$now,$oldcom) = &nextArrayRow($sth);
	my $da = &datestring();
	my $comm = $dbh->quote("$da$info");

	#print "Ban $uid $oldcom () $info<p>";

	# if within the last hour, accumulate and possibly ban this guy
	if(($now-60*$::bad_login_interval)>$changed)
	{	#old news, start a new sequence
		if(lc($status) eq 'autobanned')
        { # remember past bannings by converting to "wasbanned"
          &commandQuery($dbh,"UPDATE ipinfo SET status='wasbanned',changed=changed WHERE uid=$uid");}
        else
        { # delete the small change
          &commandQuery($dbh,"DELETE FROM ipinfo WHERE uid=$uid");
        }
	}
	else
	{
	my $da = &datestring();
	my $comm = "$oldcom\n$da$info";
	my $len = length($comm);
	if($len>500)
	{	$comm = "..." . substr($comm,index($comm,"\n",10));
	}
	$comm = $dbh->quote($comm);
	&commandQuery($dbh,"UPDATE ipinfo SET comment=$comm WHERE uid=$uid");
	if(($badlogin+1)>=$::bad_login_count && (lc($status) eq 'auto'))
	{	# convert to a ban.  The ban only lasts an hour or so
		&commandQuery($dbh,"UPDATE ipinfo SET status='autobanned' WHERE uid=$uid");
		my $qmsg = $dbh->quote("auto-banned ip=$ip for excess failed logins: $info");
		# add an alert which will be sent with email
		&commandQuery($dbh,"INSERT into messages SET type='alert',message=$qmsg");
	}
    }
  }	
  &finishQuery($sth);
}
}

var %'gamename_to_code;
var %'gamecode_to_name;
var %'gamecode_to_viewer;
var %'gamename_to_family;

sub load_game_to_code()
{	my ($dbh) = @_;
	#
	# load all of them into a  hashtable, callers tend to need more than one
	#
	my $q = "select name,code,viewer,familyname from variation";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0)
		{
		my ($un,$cd,$vv,$ff) = &nextArrayRow($sth);
		$::gamename_to_code{lc($un)} = lc($cd);
		$::gamecode_to_name{lc($cd)} = lc($un);
		$::gamecode_to_viewer{lc($cd)} = $vv;
		$::gamename_to_family{lc($un)} = $ff;
		}
	&finishQuery($sth);

}
sub gamename_to_family()
{	my ($dbh,$name) = @_;
	$name = lc($name);
	my $fa = $::gamename_to_family{$name};
	if(!$fa) { &load_game_to_code($dbh); $fa = $::gamename_to_family{$name}; }
	return($fa ? $fa : $name);
}
sub gamename_to_gamecode()
{	my ($dbh,$name,$master) = @_;
	$name = lc($name);
	my $gc = $::gamename_to_code{$name};
	if(!$gc)
	{
	&load_game_to_code($dbh);
	$gc = $::gamename_to_code{$name};
	if($master eq 'Yes') { $gc = "M$gc"; }
	}
	if(!$gc) 
		{ # used to include $name, but that led to junk in the database due to some
		  # bizarre URL injection attack
		  $gc="xx-unknown-gamename"; 
		}
	return($gc);
}
sub gamecode_to_gamename()
{
	my ($dbh,$name) = @_;
	$name = lc($name);
	my $gc = $::gamecode_to_name{$name};
	if(!$gc)
	{
	&load_game_to_code($dbh);
	$gc = $::gamecode_to_name{$name};
	}
	if(!$gc) 
		{ # used to include $name, but that led to junk in the database due to some
		  # bizarre URL injection attack
		  $gc="xx-unknown-gamecode"; 
		}	
	return($gc);
}
sub gamecode_to_gameviewer()
{
	my ($dbh,$name) = @_;
	$name = lc($name);
	my $gc = $::gamecode_to_viewer{$name};
	if(!$gc)
	{
	&load_game_to_code($dbh);
	$gc = $::gamecode_to_viewer{$name};
	}
	if(!$gc) 
		{ # used to include $name, but that led to junk in the database due to some
		  # bizarre URL injection attack
		  $gc="xx-unknown-gameviewer"; 
		}	
	return($gc);
}
sub get_column_definition
{
  my ($dbh,$table,$column)=@_;
  my $sth = &query($dbh,"describe $table");
  my $num = &numRows($sth);
  while($num>0)
  { $num--;
    my $def = "";
    my ($name,$type,$nullable,$uni,$defval) = $sth->fetchrow_array();
    
    if($type eq "inttime") { $type="datetime"; }
    
    if(lc($name) eq lc($column)) {&finishQuery($sth); return($type) };

  }
  &finishQuery($sth);
  my ($package, $filename, $line) = caller;
  &log_error("column $column table $table not found","get_column_definition from $filename line $line");
}
sub get_enum_choices
{ my ($dbh,$table,$column)=@_;
  my $def = &get_column_definition($dbh,$table,$column);
  if(index($def,"enum(")==0)
  {  my @str = split(/[,\)]/,substr($def,5));
    my $items=$#str;
    while ($items>=0) 
      { $str[$items] = substr($str[$items],1,-1);
       $items--;
      }
    return(@str);
  }
  else 
  {my ($package, $filename, $line) = caller;
   &log_error("table $table column $column is not recognised as an 'enum': $def","get_enum_choices from $filename line $line");
  }
  return();
}
sub get_enum_index
{  my ($dbh,$table,$column,$value)=@_;
   my $lcc = lc($value);
   my @def = &get_enum_choices($dbh,$table,$column);
   my $idx = $#def;
   while($idx>=0) 
     { if(lc($def[$idx]) eq $lcc) 
       {
         return($idx+1); 
       }
       $idx--;
     }
   my ($package, $filename, $line) = caller;
   &log_error("can't find index for $table $column $value in @def","get_enum_index from $filename line $line");

   return(0);
}

sub principle_variations()
{	my ($dbh) = @_;
	my @val;
	my $q = "SELECT code FROM variation where included=1 group by directory_index order by name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0) 
	{	my ($var) = &nextArrayRow($sth);
		push(@val,$var);
	}
	&finishQuery($sth);
	return(@val);
}

sub principle_variation_names()
{	my ($dbh) = @_;
	my @val;
	my $q = "SELECT name FROM variation where included=1 group by directory_index order by name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0) 
	{	my ($var) = &nextArrayRow($sth);
		push(@val,$var);
	}
	&finishQuery($sth);
	return(@val);
}

sub all_variations()
{	my ($dbh) = @_;
	my @val;
	my $q = "select distinct name from variation where included='1' order by name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0) 
	{	my ($var) = &nextArrayRow($sth);
		push(@val,$var);
	}
	&finishQuery($sth);
	return(@val);
}
#
# variations of 2 player games, found in rankings table
#
sub all_2_variations()
{	my ($dbh) = @_;
	my @val;
	my $q = "select distinct name from variation where included='1' and max_players<=2 order by name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0) 
	{	my ($var) = &nextArrayRow($sth);
		push(@val,$var);
	}
	&finishQuery($sth);
	return(@val);
}

#
# rankings in multplayer games, found in mp_rankings table
#
sub all_4_variations()
{	my ($dbh) = @_;
	my @val;
	my $q = "select distinct name from variation where included='1' and max_players>2 order by name";
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0) 
	{	my ($var) = &nextArrayRow($sth);
		push(@val,$var);
	}
	&finishQuery($sth);
	return(@val);
}
# count events of whatever type.  If a threshhold is exceeded, set the "alert" flag,
# and add an entry to the daily message list
# and if a higher threshold is exceeded, send an email immediately.
sub countEvent()
{
	my ($eventname,$high_alert,$panic_alert) = @_;
	my $dbh = &connect();
	if($dbh)
	{
	&countDBevent($dbh,$eventname,$high_alert,$panic_alert);
	}
}
sub countDBevent()
{	my ($dbh,$eventname,$high_alert,$panic_alert) = @_;
	my $qname = $dbh->quote($eventname);
	my $q = "insert into eventlog set day_recorded=current_date(),name=$qname,count=1 "
			. " on duplicate key update count=count+1";
	
	&commandQuery($dbh,$q);		# count the event

	if($high_alert>0)
	{
	#
	# if we've exceeded the alert level, set the alert which will
	# trigger notice in the daily email
	#
	my $whereclause = " where day_recorded=current_date() and name=$qname";
	my $sel = "select count,alert from eventlog $whereclause";
	my $sth = &query($dbh,$sel);
	my ($nn,$alert) = &nextArrayRow($sth,$sel);
	&finishQuery($sth);
	if($nn>=$high_alert && !$alert)
	{
	my $msg = $dbh->quote("reached $high_alert items for event $eventname");
	&commandQuery($dbh,"insert into messages SET type='alert',message=$msg");
	&commandQuery($dbh,"update eventlog set alert=true $whereclause");
	#
	# if we reach the panic level, send an email now
	#
	}
	if(($nn eq $panic_alert) && ($panic_alert>0))
	 {
		my $msg = "panic: reached $panic_alert items for event $eventname";
		__dStart($::debug_log,$ENV{'SCRIPT_NAME'});
		__dmsg($msg);
		__dEnd();
	 }
	} # end of high alert possible
}
# keep the forums database in sync
sub changePhp()
{
	my ($uid,$user,$pass) = @_;
	my $db_type = $::db_type;
	my $database= $::php_database;
	my $db_host = $::db_host;
	if($database)
	{
	my $dbh = DBI->connect("DBI:${db_type}:${database}:${db_host}",$::db_user,$::db_password);
	if($dbh)
		{
		my $quid = $dbh->quote($uid);
		my $qname = $dbh->quote($user);
		my $qpassword = $dbh->quote($pass);
		my $php = "update phpbb_users set user_password=MD5($qpassword) "
					. "where username=$qname limit 1";
		#print "$php<br>\n";
		&commandQuery($dbh,$php);

		}
	}
}


1;
