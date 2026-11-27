#!/usr/bin/perl

#
# (c) Copyright 2006 Software Garden, Inc.
# All Rights Reserved.
# Subject to Software License at the end of this file
#

#
# wikiCalc Program
#
# This is the interface for using the program
# by doing local HTTP serving on a Microsoft Windows
# system with a System Tray interface.
#
# Local HTTP serving on other systems, as well
# as Microsoft Windows, can be done through
# direct Perl execution with the wikicalc.pl
# program instead.
#
# Execute this file to start listening for
# a browser to access the UI.
#
# This program assumes that it is used with ActiveState
# Corporation's "Perl Dev Kit" PerlTray program.
# It assumes the following icons are available through
# PerlTray: "rootssmall16" and "rootssmallbw".
#

   use strict;

   use PerlTray; # From ActiveState Corporation and included automatically
                 # by the PerlTray program that bundles a Perl interpreter
                 # with the source code as an executable.

   use IO::Handle;

   use URI;
   use HTTP::Daemon;
   use HTTP::Status;
   use HTTP::Response;
   use Socket;
   use CGI::Cookie;

   use WKC;
   use WKCStrings;

#
# Get options.
#
# Start by going through each command line option and saving values.
#
# The short definition of the options is in the "--help" text
# here.
#

   my %commandmap = (s => "socket");

   while (@ARGV) {
      if ($ARGV[0] eq '--help' || $ARGV[0] eq '-h') {
         MessageBox (<<"EOF", $programname);
$WKCStrings{"wikicalcwinhelp1"} $0 $WKCStrings{"wikicalcwinhelp2"}$WKCStrings{"wikicalcwinhelp3"} $config_values{socket})
$WKCStrings{"wikicalcwinhelp4"}
EOF

         shift @ARGV;
         }

      elsif ($ARGV[0] eq '--browser' || $ARGV[0] eq '-b') {
         $config_values{browser} = 1;
         shift @ARGV;
         }

      elsif ($ARGV[0] =~ /^--(socket)=(.*)/) {
         $config_values{$1} = $2;
         shift @ARGV;
         }

      elsif ($ARGV[0] =~ /^-(s)$/) {
         shift @ARGV;
         $config_values{$commandmap{$1}} = shift @ARGV;
         }
      }

# # # #
#
# Fork to let PerlTray continue
#
# # # #

   my $pid;

   my ($readpipe1, $writepipe1); # Pipes to communicate with forked pseudo-server
   my ($readpipe2, $writepipe2); # Any message to the other means "quit"

   pipe $readpipe1, $writepipe1;
   pipe $readpipe2, $writepipe2;

   if ($pid = fork) {  # In parent -- continue with PerlTray
      close $readpipe1; # will write to other to stop it later
      $writepipe1->autoflush(1);
      close $writepipe2;
      }
   elsif (!defined $pid) { # Failed fork
      $pid = "FAILED";
      close $readpipe1;
      close $writepipe1;
      close $readpipe2;
      close $writepipe2;
      }

   # In child -- drop through to normal handling of HTTP requests

   else {
      close $writepipe1;
      $writepipe2->autoflush(1);
      close $readpipe2;

      # # # # # # # # # #
      #
      # Main program to act as a simple local web server
      # It calls process_request in the ListGarden package to do the work.
      #
      # # # # # # # # # #

      my $quit;

      my $d = HTTP::Daemon->new (
                    LocalPort => $config_values{socket},
                    Reuse => 1, Timeout => 5);
      if (!$d) {
         close $readpipe1;
         print $writepipe2 <<"EOF"; # Tell parent can't start Daemon
$WKCStrings{"wikicalcwinnolistener1"} $config_values{socket}.
$WKCStrings{"wikicalcwinnolistener2"}
EOF
         close $writepipe2;
         exit;
         }

      while (1) {
         my $c = $d->accept; # Could return undef if timeout

         # Make sure the request is from our machine

         if ($c) {
            my ($port, $host) = sockaddr_in(getpeername($c));
            if ($host ne inet_aton("127.0.0.1")) {
               $c->close;  # no - ignore request completely
               undef($c);
               next;
               }
            }

         # Process the request

         while ((defined $c) && (my $r = $c->get_request)) {
            if ($r->method eq 'POST' || $r->method eq 'GET') {
               $c->force_last_request;
               if ($r->uri =~ /favicon/) {   # if this is a request for favicon.ico, ignore
                  $c->send_error(RC_NOT_FOUND);
                  next;
                  }

               my %cookies = parse CGI::Cookie(scalar $r->header("Cookie"));

               my $cookievalue = $cookies{wkcdata}->{value}[0]; # Get our cookie to pass to request processing

               my $res = new HTTP::Response(200);

               my %responsedata = (); # start empty each time

               if ($r->method eq 'POST') {
                  process_request($r->content(), $cookievalue, \%responsedata);
                  }
               else {
                  process_request($r->uri->query(), $cookievalue, \%responsedata);
                  }

               if ($responsedata{content}) {
                  $res->content($responsedata{content});
                  }
               else {
                  $res->content_type("text/html; charset=UTF-8");
                  $res->expires("-1d");
                  $res->content($WKCStrings{"wikicalcquitpage"});
                  $quit = 1;
                  }

               $res->content_type($responsedata{contenttype} || "text/html; charset=UTF-8");

               if ($responsedata{cookie}) {
                  my $fullcookie = new CGI::Cookie(-name => "wkcdata", -value => $responsedata{cookie},
                                                   -path => '/'); # make path explicit
                  $fullcookie->expires($responsedata{cookieexpires}) if $responsedata{cookieexpires};

                  # Need to convert $fullcookie to string in case it is a reference, which sometimes happens
                  $res->push_header("Set-Cookie" => "$fullcookie");
                  }

               $res->expires($responsedata{contentexpires} || "-1d");

               $c->send_response($res);

               }

            else {
               $c->send_error(RC_FORBIDDEN);
               }

            $quit = 1 if ((-s $readpipe1) > 0); # Stop if get a message from parent

            if ($quit) {
               $c->close;
               undef($c);
               close $readpipe1;
               print $writepipe2 "Stopping!"; # Tell parent we're stopping
               close $writepipe2;
               exit;
               }
            }

         $c->close if defined $c;
         undef($c);

         if ((-s $readpipe1) > 0) { # Stop if message comes after timeout
            close $readpipe1;
            print $writepipe2 "Stopping!";
            close $writepipe2;
            exit;
            }
         }

      exit; # just in case...

      }

# # # #
#
# PerlTray stuff
#
# # # #

my $quitting;
my $dobrowser;

sub PopupMenu { # Menu to display if tray icon clicked

   return [["*$WKCStrings{wikicalcwinpopup1} (http://127.0.0.1:$config_values{socket}/ $WKCStrings{wikicalcwinpopup2})", "$WKCStrings{wikicalcwinpopup3} 'http://127.0.0.1:$config_values{socket}/'"],
           ["$WKCStrings{wikicalcwinpopup4}", \&stopChild],
          ];
}

sub ToolTip { # Show name when cursor is over icon

   return "$programname";
}

sub Singleton { # Another instance is attempted to be started

   Execute("http://127.0.0.1:$config_values{socket}/"); # Popup window instead

}

sub stopChild { # Exit command

   print $writepipe1 "Stop!"; # Quit the child which should signal us
   $quitting = 1; # just in case we never hear from the child, set our timer

}

sub Timer { # Executed every second to listen to child (the HTTP listener)

   if ($dobrowser) {
      $dobrowser = 0;
      Execute("http://127.0.0.1:$config_values{socket}/");
      }

   if ($config_values{browser}) { # if flag on command line, bring up browser (after waiting a second)
      $dobrowser = 1;
      delete $config_values{browser};
      }

   my $rp2status = (-s $readpipe2); # See if there is anything waiting in the pipe for forked HTTP listener
   my $pipestr;

   if ($rp2status > 0) {
      read($readpipe2, $pipestr, $rp2status); # Yes - read it
      Balloon($pipestr, $programname, "info", 10) if ($pipestr=~m/Error/); # If message, display it
      }

   if ($quitting > 10) {
      wait; # Clean up children
      exit; # Done
      }
   elsif ($quitting) {
      $quitting++;
      SetIcon("rootssmall16") if $quitting % 2; # Flash icon while waiting to quit
      SetIcon("rootssmallbw") unless $quitting % 2;
      }

   if ((!defined $rp2status) || ($pipestr=~m/Stopping!/)) {
      $quitting = 11; # If text from child is "Stopping!", we should quit soon
      }

   SetTimer(":01"); # Check back in a second
}

SetIcon("rootssmall16"); # use this to start
Timer(); # Start the timer waiting for the child to quit


__END__

=head1 NAME

wikicalcwin.pl

=head1 VERSION

This is wikicalcwin.pl v0.92

=head1 AUTHOR

Dan Bricklin, Software Garden, Inc.

=head1 COPYRIGHT

(c) Copyright 2006 Software Garden, Inc.
All Rights Reserved.

See Software License in the program file.

=cut

#
# HISTORY
#
# Version 0.1
# $Date: 2005/11/08 15:59:57 $
# $Revision: 0.1 $
#
# Version 1.00 08 Nov 2005 15:59:50 EST
#   Dan Bricklin, Software Garden, Inc. (http://www.softwaregarden.com/)
#   -Intitial version, based on listgardenwin.pl
#
#
# TODO:
#
#

=begin license

SOFTWARE LICENSE

This software and documentation is
Copyright (c) 2006 Software Garden, Inc.
All rights reserved. 

1. The source code of this program is made available as free software;
you can redistribute it and/or modify it under the terms of the GNU
General Public License, version 2, as published by the Free Software
Foundation.

2. This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details. You should have received a
copy of the GNU General Public License along with this program; if not,
write to the Free Software Foundation, Inc., 59 Temple Place - Suite
330, Boston, MA  02111-1307, USA.

3. If the GNU General Public License is restrictive in a way that does
not meet your needs, contact the copyright holder (Software Garden,
Inc.) to inquire about the availability of other licenses, such as
traditional commercial licenses. 

4. The right to distribute this software or to use it for any purpose
does not give you the right to use Servicemarks or Trademarks of
Software Garden, Inc., including Garden, Software Garden, ListGarden, 
and wikiCalc.

5. An appropriate copyright notice will include the Software Garden,
Inc., copyright, and a prominent change notice will include a
reference to Software Garden, Inc., as the originator of the code
to which the changes were made.

Exception for Executable Bundle 

In some cases this program is distributed together with programs and
libraries of ActiveState Corporation as a single executable file (an
"Executable Bundle") produced using ActiveState Corporation's "Perl Dev
Kit" PerlTray program ("PDK PerlTray"). This free software license does
not apply to those programs and libraries of ActiveState Corporation
that are part of the Executable Bundle. You only have a license to use
those programs and libraries of ActiveState Corporation for runtime
purposes in order to execute this software of Software Garden, Inc. In
order to create and distribute similar executable files from modified
source files, you will need to license your own copy of PDK PerlTray. 

As a specific exception for this product to the terms and conditions of
the GNU General Public License version 2, you are free to distribute
this software (modified or unmodified) in an Executable Bundle created
with PDK PerlTray as long as you adhere to the GNU General Public
License in all respects for all software components except for those of
PDK PerlTray added by that program when used to create the Executable
Bundle. 

Disclaimer 

THIS SOFTWARE IS PROVIDED BY SOFTWARE GARDEN, INC., "AS IS" AND ANY
EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
WARRANTIES OF INFRINGEMENT AND THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL SOFTWARE GARDEN, INC. NOR ITS EMPLOYEES AND OFFICERS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE DISTRIBUTION OR USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 

Software Garden, Inc.
PO Box 610369
Newton Highlands, MA 02461 USA
www.softwaregarden.com

License version: 1.3/2005-09-05

=end

=cut

