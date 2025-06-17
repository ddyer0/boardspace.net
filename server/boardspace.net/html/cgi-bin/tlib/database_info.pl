#!/usr/bin/perl

require 5.001;
use strict;
require "../include.pl";
require "gs_db.pl";
require "common.pl";

use CGI qw(:standard);
use Mysql;


sub doit()
{ my $op = param('op');
  my $dbh = &connect();

  if($dbh)
  {
	if($op eq 'savegames')
	{# this is a service routine for the savegames_if_enough cron script
	 # it produces a list of the directory and prefixes that are needed
	 my $q = "select code,directory from variation where included=1  group by directory  order by directory_index";
	 my $sth = &query($dbh,$q);
	 my $nr = &numRows($sth);
	 while($nr-- > 0)
	 {my ($code,$dir) = &nextArrayRow($sth);
	  print " $::webroot$dir ${code}- ";
	 }
	 &finishQuery($sth);
	}
      elsif($op eq 'saverankings')
	{# this is a service routine for the savegames_if_enough cron script
	 # it produces a list of the directory and prefixes that are needed
	 my $q = "select name,directory,code from variation where included=1  group by name order by name";
	 my $sth = &query($dbh,$q);
	 my $nr = &numRows($sth);
	 while($nr-- > 0)
	 {my ($name,$dir,$key) = &nextArrayRow($sth);
	  print " $::webroot$dir $name $key ";
	 }
	 &finishQuery($sth);
	}
    else
	{ &log_error("operation $op unknown",$ENV{'SCRIPT_NAME'});
	}
  }
 }

&doit();
