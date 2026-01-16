#!/usr/bin/perl

use strict;
use Mysql;
use Debug;

require "tlib/offline_ops.pl";

&init();
&PrintHtmlHeader();
# print start_html('Logon');
if( param() ) 
{	#&logForm("offlineops");
	#__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
	# return true if we're not using combined params
	# or if the combined params were parsed and validated
	my $ok = &useCombinedParams($'tea_key,1);
	if($ok && checkChecksumVersion())
	{
	my $ip = $ENV{'REMOTE_ADDR'};
	my $dbh = &connect();
	if($dbh && (&allow_ip_access($dbh,$ip)>=0))
	{
	my $tagname = param('tagname');
 	 $tagname = &despace(substr($tagname,0,25));
	my $pname = param('pname');
	 $pname = &despace(substr($pname,0,25));
	my $version = param('version');
	my $passwd = param('password');
	$passwd = &despace(substr($passwd,0,25));
	my $msg = "";
	#__d("user $pname\n");
	#__d("pass $passwd");
	if('1' eq $version)
		{
		if('loginbounce' eq $tagname) 
			{ 
			  my ($uid,$bounce) = &logon($dbh,$pname,$passwd); 
			  if($uid>0) { &sendNags($dbh); }
		          my $bb = $bounce ? "true" : "false";
			  $msg = "uidbounce $uid \"$pname\" \"$bounce\"\n";
			}
		elsif('login' eq $tagname) # obsolete version, sunset with version 8.25
			{ my ($uid,$bounce) = &logon($dbh,$pname,$passwd); 
			  if($uid>0) { &sendNags($dbh); }
			  $msg = "uid $uid\n";
			}
		elsif('checkname' eq $tagname) { $msg = &getPlayerUid($dbh,$pname,0); }	# obsolete version, sunset with 8.25
		elsif('checknamebounce' eq $tagname) { $msg = &getPlayerUid($dbh,$pname,1); }
		elsif('creategame' eq $tagname) { $msg = &creategame($dbh,$pname,$passwd); }
		elsif('getinfo' eq $tagname) 
			{ my $own = &param('owner');
		          my $inv = &param('invitedplayers');
			  my $sta = &param('status');
			  my $var = &param('variation');
			  my $limit = &param('limit');
			  my $first = &param('first');
			  #print "\nget o=$own i=$inv s=$sta v=$var\n";
                          $msg = &getgameinfo($dbh,$own,$inv,$sta,$var,$first,$limit); 
			}
		elsif('recordgame' eq $tagname)
		{
		  $msg = &recordgame($dbh,$pname,$passwd);
		}
		elsif('getusers' eq $tagname) { $msg = &getusers($dbh,&param('users'),0); }	# obsolete version, sunset with 8.25
		elsif('getusersbounce' eq $tagname) { $msg = &getusers($dbh,&param('users'),1); }
		elsif('getbody' eq $tagname) { $msg = &getbody($dbh); }
		else { $msg = "error \"undefined request: $tagname\"" ; }
		
		if( index($msg,"error ") == 0)
			{
			&printResult("Error",substr($msg,6));
			}
			else
			{
			&sendNotifications($dbh);
			&printResult("Ok",$msg);
			}
		
	#	print "$msg<p>";
		}
	else {
		&printResult("Error","bad version, not 1");
	}
	}
	}
}
1
