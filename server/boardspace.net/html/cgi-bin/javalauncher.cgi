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

print "Content-type: application/x-java-jnlp-file\n\n";

print <<ENDTAG;
<jnlp spec="1.0+" codebase="http://boardspace.net/java" href="boardspace7.jnlp">
    <information>
        <title>Boardpace Login</title>
        <vendor>Boardspace.net</vendor>
        <description>Boardpace launcher using Java Web Start</description>
        <description kind="short">Boardspace Lobby</description>
        <icon href="boardspace-icon.jpg" kind="default"/>
        <icon href="boardspace-splash.jpg" kind="splash"/>
        <shortcut online="true">
       	 <desktop/>
        </shortcut>
    </information>
    <update check="timeout" policy="always"/>
    <security>
     <all-permissions/>
    </security>  
    <resources>
        <j2se version="1.5+"/>
        <jar href="jws/Launcher.jar" main="true" download="eager"/>
        <!--#include virtual="/cgi-bin/applettag.cgi?tagname=appjars" -->
    </resources>
    <application-desc main-class="util.JWSApplication">
    <argument>servername</argument>
    <argument>boardspace.net</argument>
    <!--#include virtual="/cgi-bin/applettag.cgi?tagname=appcookies" -->
    </application-desc>
</jnlp>
ENDTAG
