#!/usr/bin/perl

#
# WKCcallableUtilities.pl -- CGI-callable functions
#
# (c) Copyright 2006 Software Garden, Inc.
# All Rights Reserved.
# Subject to Software License included with WKC.pm
#

# # # # # # # #
#
# This is simple sample code to show the processing of a wikiCalc wkcHTTP function request
#
# SECURITY WARNING!
#
#   NOTE: This code is provided for testing and teaching purposes. Normally it is not
#   made available to users because it can open up security issues with the
#   regexget function. On a server-based system, it should NOT be given execute permission
#   without thinking this through.
#
# # # # # # # #

#
# To be invoked by CGI in response to a wikiCalc wkcHTTP function request
#
# Arguments are as follows:
#
# V1=function
#
# Functions:
#
#    echo - echo back the other parameters as "1(type)=text1, 2(type)=text2..." for V2, V3, etc.
#
#    rand - return a random integer from 1 to V2 inclusive (default V2 is 100)
#
#    regexget - Get another web page and return a portion of it as follows:
#
#       regexget, url, regex, returntype
#          regex is a Perl regular expression with at least one parenthesis value
#          The value of $1 will be returned
#          "returntype" and "errortype" are: N, T, or H for number, text, or HTML
#          Example: (e.g., HTTP("url-to-here", 10, "Error",
#                               "regexget", "http://finance.yahoo.com/q?s=msft", "Last Trade:.+?<b>(.+?)<","N")
#
#    graph - Return the HTML for a graph of values that follow with optional text labels after them
#       graph, totalheight, totalwidth, color, value1, value2..., label1, label2...
#

   use strict;
   use CGI qw(:standard);
   use LWP::UserAgent;
   use utf8;


   # Get CGI object to get parameters:

   my $q = new CGI;

   my %params = $q->Vars; # get all parameters

   if (!$params{V1}) {
      %params = (V1 => "Tregexget", V2 => "Thttp://finance.yahoo.com/q?s=msft", V3 => "TLast Trade:.+?<b>(.+?)<");
      %params = (V1 => "Trand", V2 => "N100");
      %params = (V1 => "Tgraph", V2 => "N100", V3 => "N400", V4 => "Tred", V5 => "N5", V6 => "N9", V7 => "N2", V8 => "N3");
      %params = (V1 => "Techo", V2 => "TWKCcallableUtilities.pl, see source for documentation.");
      }

   print "Content-type:text/plain; charset=UTF-8\nExpires: Thu, 01 Jan 1970 00:00:00 GMT\n\n";

   my (@argvals, @argtypes, $returnstr);

   foreach my $param (sort keys %params) {
      $argvals[substr($param,1)+0] = substr($params{$param},1);
      $argtypes[substr($param,1)+0] = substr($params{$param},0,1);
      }

   if ($argvals[1] eq "echo") {
      for (my $i=1; $i < scalar(@argvals)-1; $i++) {
         $returnstr .= (($returnstr ? ", $i(" : "T$i(") . $argtypes[$i+1] . ")=" . $argvals[$i+1]);
         }
      print $returnstr;
      }

   if ($argvals[1] eq "rand") {
      my $maxv = $argvals[2]+0;
      $returnstr = "N" . (int(rand($maxv > 0 ? $maxv : 100)) + 1);
      print $returnstr;
      }

   elsif ($argvals[1] eq "regexget") {
      my $ua = LWP::UserAgent->new; 
      $ua->agent("WKCcallableUtilities");
      $ua->timeout(60);
      my $req = HTTP::Request->new("GET", $argvals[2]);
      $req->header('Accept' => '*/*');
      my $res = $ua->request($req);
      if ($res->is_success) {
          my $returnedhtml = $res->content;
          $returnstr = $1 if ($returnedhtml =~ m/$argvals[3]/s);
          $returnstr = ($argvals[4] || "T") . $returnstr;
          }
      else {
          $returnstr = "TUnable to do regexget HTTP request: " . $res->status_line;
          }
      print "$returnstr\n";
      }

   elsif ($argvals[1] eq "graph") {
      my $maxheight = $argvals[2] || 100;
      my $totalwidth = $argvals[3] || 400;
      my $color = $argvals[4] || "black";
      my ($maxval, $minval);
      my (@values, @labels);
      for (my $i=4; $i < scalar(@argvals)-1; $i++) {
         if ($argtypes[$i+1] eq "T") {
            push @labels, special_chars($argvals[$i+1]);
            }
         elsif ($argtypes[$i+1] eq "H") {
            push @labels, $argvals[$i+1];
            }
         else {
            $maxval = $argvals[$i+1] if (!defined $maxval || $maxval < $argvals[$i+1]);
            $minval = $argvals[$i+1] if (!defined $minval || $minval > $argvals[$i+1]);
            push @values, $argvals[$i+1];
            }
         }
      my $extra = ($maxval-$minval)*0.1;
      $minval = $minval - ($extra || 1);
      my $eachwidth = int($totalwidth / (scalar(@values) || 1)) || 1;
      $returnstr = qq!H<table width="$totalwidth"><tr>!;
      for (my $i=0; $i < (scalar @values); $i++) {
         my $thisbar = int(($values[$i]-$minval)*$maxheight/($maxval-$minval || 1))+1;
         $returnstr .= qq!<td valign="bottom"><table cellspacing="0" cellpadding="0" width="$eachwidth">!;
         $returnstr .= qq!<tr><td align="center" style="font-size:8pt;font-weight:bold;">$values[$i]</td></tr>!;
         $returnstr .= qq!<tr><td><div style="height:${thisbar}px;background-color:$color;width:100%">&nbsp;</div></td></tr>!;
         $returnstr .= qq!</table></td>!;
         }
      $returnstr .= "</tr><tr>";
      for (my $i=0; $i < scalar @values; $i++) {
         $returnstr .= qq!<td align="center" valign="top" style="font-size:8pt;font-weight:bold;">$labels[$i]</td>!;
         }
      $returnstr .= "</tr></table>";

      print $returnstr;
      }


# # # # # # # # # #
# special_chars($string)
#
# Returns $estring where &, <, >, " are HTML escaped
# 

sub special_chars {
   my $string = shift @_;

   $string =~ s/&/&amp;/g;
   $string =~ s/</&lt;/g;
   $string =~ s/>/&gt;/g;
   $string =~ s/"/&quot;/g;

   return $string;
}

