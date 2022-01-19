#!/usr/bin/perl
# Copyright (c) 1996 InterNetivity, Inc.
#
# gs_editnews     Validate user's Player Name and password CGI program!
#
#
# To Do: Add subscribing to the list
#
# Modified     By      Desc
# =========    ===     ==================================================
# Mar-05-99    FFD     Inital implementation.

use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use strict;
use Debug;
use URI::Escape;

require "include.pl";
require "tlib/gs_db.pl";
require "tlib/common.pl";

sub init {
	$| = 1;				# force writes
	__dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
}


# --------------------------------------------
&init();

__d( "editnews...");

sub do_gs_edit()
{
  print header;

  param();
  {
	my $fromname = param('language');
	my $passwd = param('password');
	my $escaped = param('escaped');
	my $body = param('body');
	my $written=param('written');
	my $dbh = &connect();
	my $myaddr = $ENV{'REMOTE_ADDR'};
	if($dbh)
  	{
	&readtrans_db($dbh);
	my $languagename = &select_language(param('language'));
	my $newsname = "$ENV{'DOCUMENT_ROOT'}/$languagename/news.txt";
	$written=0;
	if($languagename 
		&& ($'changenews_password && ($passwd eq $'changenews_password)) )
		{
		if(&allow_ip_login($dbh,$myaddr))
		{
			if(!($body eq ''))
			{
				write_file(${newsname},$body);
				print "<b>news changed</b><br>\n";
				$written=1;
			}
			$body = read_file( $newsname); 
			print "<pre>$body</pre><p>";
		}}
		else
		{
		&note_failed_login($dbh,$myaddr,"edit news $fromname");
		}

	&standard_header();

	print "<form action=$ENV{'REQUEST_URI'} method=post>\n";
	
	&select_language_menu($languagename,1);
	print "<br>Password: <input type=password name='password' value='' SIZE=20 MAXLENGTH=25> (special password to change news)<br>";
	print "<p>";
	print "<textarea name=body  cols=80 rows=6 scrolling=no>$body</textarea>\n";
    print "<input type=hidden name=written value=$written>\n";
	print "<p><input type=submit value='Change News'>\n";
	print "</form>\n";
	
    &disconnect($dbh);
	}		
	
  &standard_footer();


  }
}

do_gs_edit();

