#!/usr/bin/perl 
#
# Copyright (c) 1996 InterNetivity, Inc.
#
# debug.pm     Fred's perl debugging module
#
# Modified     By      Desc
# =========    ===     ==================================================
# Aug-05-96    FFD     Inital implementation.
# Sep-19-96    FFD     Similar interface to __debug.h

package Debug;
require 5.001;
require Exporter;

@ISA       = qw(Exporter);
@EXPORT    = qw( dStart __dStart __dEnd __d __dm __env __dmsg);

BEGIN {
	#print "Initalizing debug....\n\n";

	$log_file = "/home/boardspa/logs/debug.log";
	$log_name = "no _dStart $0";
	$operator = "gamemaster\@boardspace.net";
	$debug = 1;                                   # Debugging flag
	$first_run = 1;
        $no_start = 1;
}

sub __dStart {
	( $log_file, $log_name ) = @_ ;
        $no_start=0;
	my $who = `whoami`;
	__d("# Start running as $who");
}

sub __dEnd {
	__d("# End ");
}

sub __dm {
	( $msg ) = @_;
	__d("*** failed $msg");
	__dmsg($msg);
    }
sub __dmsg
{  ($msg,$env) = @_;
   my $smsg = substr($msg,0,60); 
   $sendmail = "/usr/lib/sendmail";
   open( SENDMAIL, "| $sendmail $operator" );
   print SENDMAIL <<Mail_contents;
To: $operator
Subject: ** Boardspace.net exception - $smsg

The following exception has occurred:


	         Program: $log_name

	         Message: $msg
	

See $log_file for details.

$env

regards, Boardspace Console
Mail_contents

	close SENDMAIL;
}

sub stacktrace()
{
	my $i = 1;
	my $msg = "Stack Trace:<br>\n";
	while ( (my @call_details = (caller($i++))) ){
		my $detail = $call_details[1].":".$call_details[2]." in function ".$call_details[3]."<br>\n";
		$msg = $msg . $detail;
	}
	return $msg;
}


sub showEnv()
{ my $who = `whoami`;
  my $val = "$0\nshowEnv running as $who\n";
  #$val .= &stacktrace();
  foreach ( sort keys( %ENV)) {
       $val .= "$_ : $ENV{ $_}\n";
     }
  return($val);
}
sub __env {
    __d(&showEnv());
}
sub __d {                               #  &log
  ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
  my $string = pop(@_);
  if($year<1000) { $year +='1900'; } 
  if ($debug ) {
        #print "[$year/$mon/$mday $hour:$min:$sec]  $string \n";
         if ( $first_run ) {
                 open(F_OUT,">>$log_file") || __dmsg("open log file $log_file failed",&showEnv());
#                 print F_OUT "\n                  $log_name";
                 print F_OUT "\n";
 		$first_run = 0;
        } else {
                 open(F_OUT,">>$log_file");
         }
 
        printf F_OUT "[%d/%02d/%02d %02d:%02d:%02d] (%s): %s\n", $year,$mon+1,$mday,$hour,$min,$sec,$log_name,$string;

	if($no_start)
	{$no_start=0;

         print F_OUT &showEnv(); 
        }
        close F_OUT;
  }
}

1;

