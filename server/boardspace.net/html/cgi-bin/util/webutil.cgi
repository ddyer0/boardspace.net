#!/usr/bin/perl
# Program: WebUtil - A collection of net commands 
# Author: The Puppet Master
# Created: 12/13/1998

use CGI::Carp qw( fatalsToBrowser );

$version = "2.0";
$history = "webutil.history";
$show_hist = "1";          # 0=No | 1=Yes

# $cgi_url is the location of this script.
$cgi_url = "http://www.boardspace.net/cgi-bin/util/webutil.cgi";

# $pingnum is the number of packets to send on ping. 
$pingnum = 5;

# $examplesite is the site address to use in the Examples.
$examplesite = $ENV{'REMOTE_ADDR'};;

# $exampleserv is the DNS Server to use for NSLookup
$exampleserv = $ENV{'REMOTE_ADDR'};

# $exampleip is the IP Address To Lookup for NSLookup.
$exampleip = $ENV{'REMOTE_ADDR'};

# $examplemail is the email address to us in the Finger Example.
# NOTE: THE \ IN FRONT OF THE @ IS VERY IMPORTANT!!!
$examplemail = "yourname\@domain.com";

# Done With Configuration.

require 'parsform.cgi';
require 'error.cgi';
require 'formdate.cgi';

$timeformat = "<timesec>";
$dmonthformat = "<m>";
$yearformat = "<year>";

if ($ENV{'QUERY_STRING'} ne '') {
	$command = "$ENV{'QUERY_STRING'}";
}
else {
	&parse_form;
}


print "Content-type: text/html\n\n";

# Ping
if ($command eq 'ping') {
	print "<html><head><title>WebUtil Ping</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Ping</b> A default of $pingnum packets will be sent.\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"ping\">\n";
	print "Web Name or IP Address: <input type=text name=\"webaddress\" value=\"$examplesite\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$examplesite</i><p>\n";
	print "<input type=submit value=\" Ping \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}

# Traceroute
elsif ($command eq 'traceroute') {
	print "<html><head><title>WebUtil Traceroute</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Traceroute</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"traceroute\">\n";
	print "Web Name or IP Address: <input type=text name=\"webaddress\" value=\"$examplesite\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$examplesite</i><p>\n";
	print "<input type=submit value=\" Traceroute \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}
	
# Whois
elsif ($command eq 'whois') {
	print "<html><head><title>WebUtil Whois</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Whois</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"whois\">\n";
	print "Web Name or IP Address: <input type=text name=\"webaddress\" value=\"$examplesite\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$examplesite</i><p>\n";
	print "<input type=submit value=\" Whois \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}	

# Finger
elsif ($command eq 'finger') {
	print "<html><head><title>WebUtil Finger</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Finger</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"finger\">\n";
	print "Email Address: <input type=text name=\"emailaddress\" value=\"$examplemail\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$examplemail</i><p>\n";
	print "<input type=submit value=\" Finger \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}	

# NSLOOKUP
elsif ($command eq 'nslookup') {
	print "<html><head><title>WebUtil NS-Lookup</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil NS-Lookup</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"nslookup\">\n";
	print "Server To Use: <input type=text name=\"server\" value=\"$exampleserv\" size=\"30\"><p>\n";
	print "Example: <i>$exampleserv</i><p>\n";
	print "Web Address: <input type=text name=\"webaddress\" value=\"$exampleip\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$exampleip</i><p>\n";
	print "<input type=submit value=\" NSLOOKUP \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}	


# HOST
elsif ($command eq 'host') {
	print "<html><head><title>WebUtil Host</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Host</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"host\">\n";
	print "Web Address: <input type=text name=\"webaddress\" value=\"$examplesite\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$examplesite</i><p>\n";
	print "<input type=submit value=\" HOST \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}	


# DNSQuery
elsif ($command eq 'dnsquery') {
	print "<html><head><title>WebUtil DNS-Query</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil DNS-Query</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"dnsquery\">\n";
	print "Web Address: <input type=text name=\"webaddress\" value=\"$examplesite\" size=\"30\">\n";
	print "<p>\n";
	print "Example: <i>$examplesite</i><p>\n";
	print "<input type=submit value=\" DNSQuery \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}	


# Calendar
elsif ($command eq 'calendar') {
	$dmonth = &format_date(time,$dmonthformat);
	$year = &format_date(time,$yearformat);
	print "<html><head><title>WebUtil Calendar</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Calendar</b>\n";
	print "<form method=\"POST\" action=\"$cgi_url\">\n";
	print "<input type=\"hidden\" name=\"action\" value=\"calendar\">\n";
	print "Month (1-12 or blank for entire year): <input type=text name=\"month\" value=\"$dmonth\" size=\"2\">\n";
	print "Year (4 digits): <input type=text name=\"year\" value=\"$year\" size=\"4\">\n";
	print "<p>\n";
	print "Example: <i>$dmonth $year</i><p>\n";
	print "<input type=submit value=\" Calendar \"> <input type=reset>\n";
	print "</form>\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}	


# The Ping Routine
elsif ($FORM{'action'} eq 'ping') {
	&data_entry unless $FORM{'webaddress'};
	$starttime=&format_date(time,$timeformat);
	for($i=0;$i<$pingnum;$i++) {  $ping .= `ping -v $FORM{'webaddress'}`; }
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil Ping</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Ping</b><p>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "Ping Results For: $FORM{'webaddress'}</b><p>\n";
	print "<pre>$ping</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}

# The Traceroute Routine
elsif ($FORM{'action'} eq 'traceroute') {
	&data_entry unless $FORM{'webaddress'};
	$starttime=&format_date(time,$timeformat);
	$traceroute = `traceroute $FORM{'webaddress'}`;
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil Traceroute</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Traceroute</b>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "Traceroute Results For: $FORM{'webaddress'}</b><p>\n";
	print "<pre>$traceroute</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}

# The Whois Routine
elsif ($FORM{'action'} eq 'whois') {
	&data_entry unless $FORM{'webaddress'};
	$starttime=&format_date(time,$timeformat);
	$whois = `whois $FORM{'webaddress'}`;
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil Whois</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Whois</b>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "Whois Results For: $FORM{'webaddress'}</b><p>\n";
	print "<pre>$whois</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}

# The Finger Routine
elsif ($FORM{'action'} eq 'finger') {
	&data_entry unless $FORM{'emailaddress'};
	$starttime=&format_date(time,$timeformat);
	$finger = `finger $FORM{'emailaddress'}`;
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil Finger</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Finger</b>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "Finger Results For: $FORM{'emailaddress'}</b><p>\n";
	print "<pre>$finger</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}

# The NS-Lookup Routine
elsif ($FORM{'action'} eq 'nslookup') {
	&data_entry unless $FORM{'webaddress'};
	$starttime=&format_date(time,$timeformat);
	$nslookup = `nslookup $FORM{'webaddress'} $FORM{'server'}`;
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil NS-Lookup</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil NS-Lookup</b>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "NS-Lookup Results For: $FORM{'webaddress'}</b><p>\n";
	print "<pre>$nslookup</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}


# The Host Routine
elsif ($FORM{'action'} eq 'host') {
	&data_entry unless $FORM{'webaddress'};
	$starttime=&format_date(time,$timeformat);
	$host = `host -v $FORM{'webaddress'}`;
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil Host</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Host</b>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "Host Results For: $FORM{'webaddress'}</b><p>\n";
	print "<pre>$host</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}


# The DNSQuery Routine
elsif ($FORM{'action'} eq 'dnsquery') {
	&data_entry unless $FORM{'webaddress'};
	$starttime=&format_date(time,$timeformat);
	$dnsquery = `dnsquery -v $FORM{'webaddress'}`;
	$endtime=&format_date(time,$timeformat);
	print "<html><head><title>WebUtil DNSQuery</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil DNSQuery</b>\n";
	print "<table border=0>\n";
	print "<tr><td>Time Started:</td><td>$starttime</td></tr>\n";
	print "<tr><td>Time Finished:</td><td>$endtime</td></tr>\n";
	print "</table>\n";
	print "<p><b>\n";
	print "DNSQuery Results For: $FORM{'webaddress'}</b><p>\n";
	print "<pre>$dnsquery</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}


# The Calendar Routine
elsif ($FORM{'action'} eq 'calendar') {
	&data_entry unless $FORM{'year'};
	$calendar = `cal $FORM{'month'} $FORM{'year'}`;
	print "<html><head><title>WebUtil Calendar</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Calendar\n";
	print "<p>\n";
	print "Calendar Results</b><p>\n";
	print "<pre>$calendar</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}

# The uptime routine
#elsif ($FORM{'action'} eq 'uptime') {
elsif ($command eq 'uptime') {
	$uptime = `uptime`;
	print "<html><head><title>WebUtil Uptime</title></head>\n";
	print "<body>\n";
	print "<b>WebUtil Uptime\n";
	print "<p>\n";
	print "Uptime Results</b><p>\n";
	print "<pre>$uptime</pre>\n";
	print "<p>Done!\n";
	print "<p><a href=\"$cgi_url\">RETURN TO MAIN MENU</a>\n";
	print "</body></html>\n";
}
	

# The Main Menu!
else {
	print "<html><head><title>WebUtil</title></head>\n";
	print "<body><center><h1>WebUtil Version: $version</h1>\n";
	if ($show_hist eq "1") {
		print "<font size=-1><a href=\"$history\">View History</a></font><p>\n";
	}
	print "<!-- ****************************************\n";
	print "<!-- Webutil.pl by The Puppet Master\n";
	print "<!-- Copyright 1998 The Puppet Master\n";
	print "<!-- ****************************************\n";
	print "<h6>By The Puppet Master</h6>\n";
	print "<h3>Make Your Choice Below</h3></center>\n";
	print "<hr><center>\n";
	print "<table border=0 width=600>\n";
	print "<tr><td>\n";
	print "Note: Depending on present net traffic and the speed of your link,\n";
	print "Some of these commands may take a long time.  Patience is\n";
	print "a virtue.  This script will run on Unix only!\n";
	print "I will not be responsible for any information retrieved\n";
	print "from these commands.\n";
	print "<ul>\n";
	print "<li><a href=\"$cgi_url?ping\">ping</a></li>\n";
	print "<li><a href=\"$cgi_url?traceroute\">traceroute</a></li>\n";
	print "<li><a href=\"$cgi_url?whois\">whois</a></li>\n";
	print "<li><a href=\"$cgi_url?finger\">finger</a></li>\n";
	print "<li><a href=\"$cgi_url?nslookup\">nslookup</a></li>\n";
	print "<li><a href=\"$cgi_url?host\">host</a></li>\n";
	print "<li><a href=\"$cgi_url?dnsquery\">dns query</a></li>\n";
	print "<li><a href=\"$cgi_url?calendar\">calendar</a></li>\n";
	print "<li><a href=\"$cgi_url?uptime\">uptime</a></li>\n";
	print "</ul>\n";
	print "</td></tr></table>\n";
	print "<p>\n";
	print "<font size=\"-1\">WebUtil \&copy; Copyright 1998 The Puppet Master<br>\n";
	print "Formdate.pl \&amp; Parsform.pl are all \&copy; Copyright <a href=\"http://www.cgi-perl.com\">The CGI/Perl Cookbook</a><br>by Matt Wright \&amp; Craig Patchett\n";
	print "</font></center>\n";
	print "</body></html>\n";
}


# Missing Fields
sub data_entry {
	print "<html><head><title>Data Entry Error</title></head>\n";
	print "<body><center><h1>Data Entry Error</h1>\n";
	print "You didn't enter the data you were looking for!\n";
	print "<p>Use your Browsers back button to try again.\n";
	print "</body></html>\n";
	exit;
}
	
# END OF WEBUTIL
