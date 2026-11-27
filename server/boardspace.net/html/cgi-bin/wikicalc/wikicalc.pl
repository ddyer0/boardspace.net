#!/usr/bin/perl

#
# (c) Copyright 2006 Software Garden, Inc.
# All Rights Reserved.
# Subject to Software License at the end of this file
#

#
# wikiCalc
#
# This is the interface for using the program
# by doing local HTTP serving
#
# Execute this file to start listening for
# a browser to access the UI.
#

   use strict;

   use URI;
   use HTTP::Daemon;
   use HTTP::Status;
   use HTTP::Response;
   use Socket;
   use CGI::Cookie;

   use WKC;
   use WKCStrings;

#
# Common variables
#

#
# Load plugins
#

#!!! not now!!!   load_plugins();

#
# Need to make options plugin driven
#

# # # # # # # # # #
#
# Main program to act as a simple local web server
# It calls process_request to do the work.
#
# # # # # # # # # #

   my $quit;

   my $d = HTTP::Daemon->new (
                    LocalPort => $config_values{socket},
                    Reuse => 1);

   if (!$d) {
      print <<"EOF";
$programname
$WKCStrings{"wikicalcnolistener"} $config_values{socket}.
EOF

      exit;
      }

   print "$programname\n$WKCStrings{wikicalcaccessui}: http://127.0.0.1:$config_values{socket}/\n";

   while (my $c = $d->accept) {

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

         if ($quit) {
            $c->close;
            undef($c);
            exit;
            }
         }

      $c->close;
      undef($c);
      }


__END__

=head1 NAME

wikicalc.pl

=head1 VERSION

This is wikicalc.pl v0.92

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
# $Date: 2005/09/05 13:02:00 $
# $Revision: 0.1 $
#
# Version 0.1 05 Sep 2005 13:04:00 EDT
#   Dan Bricklin, Software Garden, Inc. (http://www.softwaregarden.com/)
#   -Intitial version
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
