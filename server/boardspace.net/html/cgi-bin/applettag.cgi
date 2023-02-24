#!/usr/bin/perl 
#
# get the location of the applet classes.  Used by ssi for a few pages.
#
use CGI qw(:standard);
use strict;
use CGI::Cookie;
use Debug;
require "include.pl";
require "tlib/common.pl";
require "tlib/gs_db.pl";
require "tlib/timestamp.pl";

$| = 1;				# force writes

# game is a game name, ie "zertz" or "yinsh"
sub print_offline_jnlp()
{	my ($dbh,$game) =@_;
	my $width = &param('width');
	my $height= &param('height');
	my $cgame = ucfirst($game);
	my $title = &trans("Review #1 games offline",$cgame);
	my $size = "";
	if($width) { $size .= "<argument>framewidth</argument> <argument>$width</argument>\n"; }
	if($height) { $size .= "<argument>frameheight</argument> <argument>$height</argument>\n"; }
	my $host = $ENV{'HTTP_HOST'};
	my $vname = "$cgame viewer.jnlp";
	my $proto = $ENV{'REQUEST_SCHEME'};
print <<ENDTAG;
Content-type: application/x-java-jnlp-file
Content-Disposition: inline; filename=${cgame} viewer.jnlp

<?xml version="1.0" encoding="UTF-8"?>
<jnlp spec="1.0+" codebase="${proto}://$host/java" >
    <information>
        <title>$title</title>
        <vendor>Boardspace.net</vendor>
        <description>$title using Java Web Start</description>
        <description kind="short">$cgame reviewer</description>
        <icon href="boardspace-icon.jpg" kind="default"/>
        <icon href="boardspace-splash.jpg" kind="splash"/>
        <shortcut online="true">
       	 <desktop/>
        </shortcut>
    </information>
    <security>
     <all-permissions/>
    </security>  
    <resources>
        <j2se version="1.5+"/>
        <jar href="jws/Launcher.jar" main="true" download="lazy"/>
        <!--#include virtual="/cgi-bin/applettag.cgi?tagname=appjars" -->
    </resources>
    <application-desc main-class="util.JWSApplication">
    <argument>servername</argument>
    <argument>$host</argument>
    $size
ENDTAG

   &print_jnlp_gameparams_tag($dbh,$game);

print <<ENDTAG2;
    </application-desc>
</jnlp>
ENDTAG2
	print "\n";
}


sub print_applet_tag()
{
	print " codebase=\"/$'java_dir/$'class_dir/\" ";
	print " archive=\"$'jar_collection\" code=\"$'root_applet\"";
}
sub print_class_dir()
{	print "$'class_dir";
}
sub print_gamedir()
{
	my ($dbh,$game) = @_;
	my $qgame = $dbh->quote($game);
	my $q = "select directory_index,name,directory from variation where included>0 order by directory_index";
	my $sth = &query($dbh,$q);
	my $num = &numRows($sth);
	while($num-- > 0)
	{
		my ($dirnum,$gamename,$dir) = &nextArrayRow($sth);
		if(($game eq '') || ($game eq $gamename))
		{
			print "$dirnum,$gamename,$dir\n";
		}
	}
	&finishQuery($sth);
}
sub print_gameparams_tag()
{	my ($dbh,$game) = @_;
	my $language = &select_language();
	my $qgame = $dbh->quote($game);
	my $q = "select directory_index,directory from variation where name=$qgame";
	my $sth = &query($dbh,$q);
	my ($num,$dir) = &nextArrayRow($sth);
	print "<param name='permissions' value='$'applet_permissions'>\n";
	print "<param name='gameindex' value='$num'>\n";
	print "<param name='reviewerdir$num' value='$dir'>\n";
	print "<param name='gamename' value='$game'>\n";
	print "<param name='rootname' value='"
		. &trans("Offline game reviewer for #1",ucfirst($game))
		. "'>\n";
	print "<param name='language' value='${language}Strings'>\n";

	# boilerplate we'll eliminate someday
	print "<param name='zertz.classname' value='L:Game'>\n";
	print "<param name='framewidth' value='700'>\n";
	print "<param name='reviewOnly' value='true'>\n";
	print "<param name='rootreviewer' value='true'>\n";

	&finishQuery($sth);
}
sub print_jnlp_gameparams_tag()
{	my ($dbh,$game) = @_;
	my $language = &select_language();
	my $qgame = $dbh->quote($game);
	my $q = "select directory_index,directory from variation where name=$qgame";
	my $sth = &query($dbh,$q);
	my ($num,$dir) = &nextArrayRow($sth);
	my $proto = $ENV{'REQUEST_SCHEME'};
	print "<argument>gameindex</argument><argument>$num</argument>\n";
	print "<argument>reviewerdir$num</argument><argument>$dir</argument>\n";
	print "<argument>gamename</argument><argument>$game</argument>\n";
	print "<argument>reviewonly</argument><argument>true</argument>\n";
	print "<argument>protocol</argument><argument>$proto</argument>\n";
	&finishQuery($sth);
}

sub print_language_list()
{	my @ll;
	foreach my $key (keys(%'language_codes)) 
	{	my $val = $'language_codes{$key};
		if(!("" eq $val)) { push(@ll,$val); }
	}
	my $prev;
	foreach my $l (sort @ll)
	{
	if(!($prev eq $l)) {	print "$l\n"; }
	$prev = $l;
	}
}

#	/**
#	  * Translates the specified byte array into Base64 string.
#	  * and = or == at the end to mark incomplete octets
#	  * @param buf the byte array (not null)
#	  * @return the translated Base64 string (not null)
#	  */
    sub encode64long()
    {	# this version is obsolete, in the future use the version in params.pl
		my ($buf) = @_;
		my @ALPHABET = unpack("W*","ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
		my $str = "";
        my $i=0;
        my $skip1=0;
        my $skip2=0;
        my @chars = unpack("W*",$buf);
        my $size = $#chars+1;
        #print "@ALPHABET\n";
        #print "$buf\n";
        #print "@chars\n";
        while($i < $size){
            my $b0 = $chars[$i++];
            my $b1 = 0;
             if($i < $size) { $b1 = $chars[$i++]; } else { $skip1=1; };
            my $b2 = 0;
             if($i < $size) { $b2 = $chars[$i++]; } else { $skip2=1; };
            my $mask = 0x3F;
            $str .= chr($ALPHABET[($b0 >> 2) & $mask]);
            $str .= chr($ALPHABET[(($b0 << 4) | (($b1 & 0xFF) >> 4)) & $mask]);
            $str .= $skip1 ? "=" : chr($ALPHABET[(($b1 << 2) | (($b2 & 0xFF) >> 6)) & $mask]);
            $str .= $skip2 ? "=" : chr($ALPHABET[$b2 & $mask]);
        }
        $str .= "\n";
       return $str;
    }

sub make_log()
{
 my ($msg ) = @_;
 my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
 open( F_OUT, ">>$'contact_log" );
 printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] %s\n",1900+$year,$mon+1,$mday,$hour,$min,$sec,$msg;
 close F_OUT;
}

sub print_mobileinfo()
{	my $val = "languages ";
	my $platform = &param("platform");
	&make_log("platform $platform ip $ENV{'REMOTE_ADDR'}");
	# no particular order is required, but start with languages
	my @ll;
	foreach my $key (keys(%'language_codes)) 
	{	my $val = $'language_codes{$key};
		if(!("" eq $val)) { push(@ll,$val); }
	}
	my $prev;
	my $llist = "";
	foreach my $l (sort @ll)
	{
	if(!($prev eq $l)) {	$llist .= "$l "; }
	$prev = $l;
	}
	$val .= &encode64long($llist) . "\n";

	$val .= "message " . &encode64long($'mobile_login_message) . "\n";

	$val .= "versions " . &encode64long($'mobile_version_info) . "\n";

	print($val);

}

sub print_country_list()
{	
	foreach my $val (@'countries) 
	{	print "$val\n";
	}
}
sub print_timestamp_code()
{	
	&print_raw_timestamp();
}

sub print_prereg_info()
{	&print_timestamp_code();
	&print_country_list();
}

sub print_standard_reviewer()
{	my ($dbh) = @_;
	my $game = &param('game');
	my $width = &param('width');
	my $height= &param('height');
	my $cgame = ucfirst($game);
	my $title = &trans("Review #1 games offline",$cgame);
	print "<title>$title</title>\n";
	print "<h2>$title</h2><p>";
	print &trans("With this review applet, you can view the same games as in online Review rooms, but without connecting to the lobby.");

	print "<form method=post action='/cgi-bin/applettag.cgi'>\n";
	print "<input type=hidden name=tagname value=offlinejwsviewer>";
	print "<input type=hidden name=game value=$game>\n";
	print "<input type=hidden name=width value=$width>\n";
	print "<input type=hidden name=height value=$height>\n";
	print "<input type=submit value='Launch Java Viewer for $cgame'>\n";
	print "</form>";

}
sub print_appcookies()
{	my %cookies = fetch CGI::Cookie();
       __dStart( "$'debug_log", $ENV{'SCRIPT_NAME'} );
	foreach my $key (keys(%cookies))
	{ my $val = $cookies{$key};
	  my $ind = index($val,'=');
	  my $ind2 = index($val,';');
	  __d("Key $key '$val'");
	  if($ind>0 && $ind2 > $ind)
	   { $val = substr($val,$ind+1,$ind2-$ind-1); 
	     print "<argument>jws_$key</argument>\n<argument>$val</argument>\n";
		}
	 }
	__dEnd();
}
sub print_appletcookies()
{	my %cookies = fetch CGI::Cookie();
	my $parameters = "";
	my $space = "";
	foreach my $key (keys(%cookies))
	{ my $val = $cookies{$key};
	  my $ind = index($val,'=');
	  my $ind2 = index($val,';');
	  if($ind>0 && $ind2 > $ind)
	   { $val = substr($val,$ind+1,$ind2-$ind-1); 
	     print "<param name=\"jws_$key\" value=\"$val\" />\n";
	     $parameters .= $space . "jws_" . $key;
	     $space = " ";
		}
	 }
	 print "<param name=\"jws_parameters\" value=\"$parameters\" />\n";
	print "<param name=permissions value='$'applet_permissions'/>\n";
}

# print a list of the jars in the class directory.
sub print_appjars()
{	my $test = &param('test');
	my $dir = $test 
			? $ENV{'DOCUMENT_ROOT'} . "/$'java_dir/$'test_class_dir/" 
			: $ENV{'DOCUMENT_ROOT'} . "/$'java_dir/$'class_dir/";
	#print "Dir = $dir<p>";
	my $refdir = $test ? "test" : "jws";
	my (@jars) = &list_dir($dir);
	foreach my $jar (@jars)
	{ if( (index(lc($jar),'launcher.jar')<0)
			&& (index(lc($jar),".jar")>0)) { print "<jar href='$refdir/$jar' download='lazy'/>\n"; }
	 }
}

# print a list of the jars and their modification dates in the class directory.
sub print_appjarinfo()
{	my $test = &param('test');
	my $host = $test ? "/$'java_dir/$'test_class_dir/" : "/$'java_dir/$'class_dir/";
	my $dir = $ENV{'DOCUMENT_ROOT'} . $host;
	#print "Dir = $dir<p>";
	#my $par = &param('environment');
	#print "Env = $par\n";
	my (@jars) = &list_dir($dir);
	print "version,$'appjar_version,$host\n";
	foreach my $jar (@jars)
	{ if( (index(lc($jar),'launcher.jar')<0)
			&& (index(lc($jar),".jar")>0))
			{
			my $modtime = (stat("$dir$jar"))[9]; 
			print "$modtime,$host$jar\n"; 
			}
	 }
}

# print a list of the .res files and their modification dates in the data directory.
sub print_appdata()
{	my $test = &param('test');
	my $host = $test ? "/$'java_dir/testappdata/" : "/$'java_dir/appdata/";
	my $dir = $ENV{'DOCUMENT_ROOT'} . $host;
	#print "Dir = $dir<p>";
	my (@jars) = &list_dir($dir);
	print "version,1,$host\n";
	foreach my $jar (@jars)
	{ if( index(lc($jar),".res")>0)
		{
		my $modtime = (stat("$dir$jar"))[9]; 
		print "$modtime,$host$jar\n"; 
		}
	 }
}

# two start options, for regular jws and guest jws
sub print_jws()
{	my ($guest) = @_;
	if($'jws_link)
	{	my $link = ($guest eq 'true') ? "'$'jws_guest_link'" : "'$'jws_link'";
		my $message = &param('text');
		if ('' eq $message) { $message = "java web start message"; }
		my $jws_message = "<a href=$link>" . &trans("java web start") . "</a>";
		print &trans("$message #1",$jws_message);
	}
}


# two start options, for regular jws and guest jws
sub print_jws_raw()
{	my ($guest) = @_;
	if($'jws_link)
	{	my $link = ($guest eq 'true') ? "$'jws_guest_link" : "$'jws_link";
		print "$link";
	}
}

sub print_tag()
{	my ($tagname) = @_;

	if($tagname eq 'offlinejwsviewer')
		{
		}
		else 
		{
			print "Content-type: text/html\n\n"; 
		}

	if($tagname eq '')
	{	&print_applet_tag();
	}
	else
	{
	my $dbh = &connect();
	if($dbh && (&allow_ip_access($dbh,$ENV{'REMOTE_ADDR'})>=0))
		{
		&readtrans_db($dbh);
		my $game = &param('game');
		my $guest = &param('guest');
		if($tagname eq 'offlinejwsviewer') { &print_offline_jnlp($dbh,$game); }
		elsif($tagname eq 'offlinereviewer') { &print_standard_reviewer($dbh,$game); }
		elsif($tagname eq 'gameparams') { &print_gameparams_tag($dbh,$game); }
		elsif($tagname eq 'gamedir') { &print_gamedir($dbh,$game); }
		elsif($tagname eq 'languages') { &print_language_list(); }
		elsif($tagname eq 'countries') { &print_country_list(); }
		elsif($tagname eq 'timestamp') { &print_timestamp_code(); }
		elsif($tagname eq 'prereginfo') { &print_prereg_info(); }
		elsif($tagname eq 'classdir') { &print_class_dir(); }
		elsif($tagname eq 'appcookies') { &print_appcookies(); }
		elsif($tagname eq 'appletcookies') { &print_appletcookies(); }
		elsif($tagname eq 'appjars') { &print_appjars(); }
		elsif($tagname eq 'appjarinfo') { &print_appjarinfo(); }
		elsif($tagname eq 'appdata') { &print_appdata(); }
		elsif($tagname eq 'jwslink') { &print_jws($guest); }
		elsif($tagname eq 'jwsrawlink') { &print_jws_raw($guest); }
		elsif($tagname eq 'mobileinfo') { &print_mobileinfo(); }
		else
		{
		&log_error("applet tag $tagname is not defined",$ENV{'SCRIPT_NAME'});
		}
		}
	&disconnect($dbh);
	}
}

&print_tag(&param('tagname'));
