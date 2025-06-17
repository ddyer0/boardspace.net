#!/usr/bin/perl
#
# this script is invoked from qmail as a filter. It's purpose is to
# detect users whose email addresses are bouncing and flag them, both
# so they will see it and so we can skip them in mass mailings
#
# The general trick is to make a file
# /home/boardspa/var/boardspace.net/mail/.qmail-bouncedatabase
# which looks like this: 
# 
#	|cd /home/boardspa/cgi-bin/tlib; /usr/bin/perl procmail.pl
#	|/usr/bin/maildrop /home/interworx/lib/maildrop/spamfilter
#
# this processes the alias "bouncedatabase" which is a sink for bounced mail
# to bounce and forward, use the group alias "bounce-and-forward" which does both.
#
# NOTE!! This script is normally run under the uid of vpopmail, so it
# must run in a suitable environment.  This could be as 
# suid for boardspace so it will have write access
# to the log files it tries to write, but perl has problems as suid
# 
# second choice is to share a group with the vpopmail program (edit the /etc/group file)
# and set $::procmail_debug_log to a file whose group is that group.
#
# procmail.pl
use CGI qw(:standard);
use Debug;
use strict;

require "../include.pl";
require "gs_db.pl";
require "common.pl";

sub doit()
{  
 __dStart($::procmail_debug_log,"procmail $0");
 my $dbh = &connect();
 if($dbh) 
 {
  my $found = 0;
  my $msg = "";
  while(<STDIN>)
  {  $msg .= $_;
  }
  my $found = 0;
  my $target = "x-boardspace-user:\\s(.*)\\s";
  my $ind = $msg;
  if ($ind =~ m/$target/) { $found=1; }
  my $tar = $1;
  if( $found )
  {
        my ($user) = split(' ',$tar);
        my $qn = $dbh->quote($user);
	my $q = "update players set e_mail_bounce=e_mail_bounce+1,e_mail_bounce_date=CURRENT_TIMESTAMP where player_name=$qn limit 1";
	__d("bounce for $qn");
	&commandQuery($dbh,$q);
	&countEvent("bounced email",4,10);
  }
  else
  { my $smsg = (length($msg)>1000) ? substr($msg,0,1000) : $msg;
    __d("unexpected message\n$smsg");
   #__env();
    &countEvent($::procmail_debug_log,$::procmail_error_alert_level,$::procmail_error_panic_level);
  }
}
 __dEnd( "end!" );

 return(0);
}

&doit();
