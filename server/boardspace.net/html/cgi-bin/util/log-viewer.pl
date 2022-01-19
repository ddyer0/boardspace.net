#!/usr/bin/perl
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard);
use HTML::Entities;
use strict;

require "../include.pl";
require "../tlib/common.pl";
require "../tlib/service-runner.pl";

sub print_frameset()
{	my ($urlbase,%args) = @_;
	my $caption = $'log_viewer_caption;
	my $url = &get_url($urlbase,%args);
	print "<html>\n";
	print "<head>\n";
	print "<title>$caption</title>\n";
	print "</head>\n";
	print "<frameset rows='120,*'>\n";
	print "<frame name=top src='$url&frame=top' scrolling=no marginwidth=0 marginheight=0>\n";
	print "<frame name=bottom src='$url&frame=bottom' marginwidth=0 marginheight=0>\n";
	print "</frameset>\n";
	print "</html>\n";
}

sub printlines()
{	my ($txt) = @_;
	my @lines = split(/[\r\n]/,$txt);
	my $line;
	foreach $line(@lines) { if($line ne "") { print encode_entities("$line\n"); }}
}

sub do_it()
{
	my $password = param('password');
	my $filter = param('filter');
	my $casesensitive = param('casesensitive');
	my $clines = param('contextlines');
	my $lines = &param('lines');
	my $caption = $'log_viewer_caption;
	my $lpw = $'log_viewer_password;
	if(!$clines<0) { $clines=5; }
	if(!($lpw && ($password eq $lpw)))
	{ print "<title>$caption</title>\n";
		print "<h3>$caption</h3>\n";
		print "<br>Password: ";
		print "<form name=logviewer action=$ENV{'SCRIPT_NAME'} METHOD=POST >";
		print "<input name=password type=password SIZE=20 MAXLENGTH=25>";
		print "<input type=submit value='View Logs'>";
		print "</form>";
	}
	else
	{
	my $frame = param('frame');
	my $base = $ENV{'SCRIPT_NAME'};
	my $selected = param('selected');	if($lines == 0) { $lines=25; }
	my %arglist = (
		'lines',$lines,
		'selected',$selected,
		'password',$password
	);
	if($frame eq "")
	{ &print_frameset($base,%arglist);
	}
	elsif ($frame eq 'top')
	{ 
		print "<h3>$caption</h3>";
		my @dir =sort(&list_dirs($'log_viewer_dir));
		my $l25 = ($lines == 25) ? 'checked' : '';
		my $l500 = ($lines == 500) ? 'checked' : '';
		my $l100 = ($lines == 100) ? 'checked' : '';
		my $file;
		print "<table><tr><td>\n";
		print "<form name=viewform action =$ENV{'SCRIPT_NAME'} METHOD=POST target='bottom'>\n";
		print "<input name=password value='$password' type=hidden>";
		print "<input name=lines value=25 onclick='this.form.submit()' type=radio $l25>25 Lines";
		print "<input name=lines value=100 onclick='this.form.submit()' type=radio $l100>100 Lines";
		print "<input name=lines value=500 onclick='this.form.submit()' type=radio $l500>500 Lines";
		print "  select file: ";
		print "<select name=selected onchange='this.form.submit()'>";
		foreach $file(@dir)
		{  my $sfile = $file;
		   $sfile =~ y/_/./;
		    my ($name,$fdir,$type) = &filename_split($sfile);
			if(lc($type) eq '.log')
				{	print "<option value='$file'>$name</option>\n";
				}
		}
		print "</select>";
		print "<input type=hidden name=frame value=bottom>\n";
		my $cs = $casesensitive ? "checked" : "";
		print "<br>Case Sensitive: <input type=checkbox name=casesensitive $cs>";
		print "match: <input type=text name=filter value='$filter' onchange='this.form.submit()'>\n";
		print "<input type=text size=2 name='contextlines' value='5' onchange='this.form.submit()'>context lines\n";
		print "</form>";
		print "</td><td align=right>";
		{my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
		printf "current GMTIME: [%d/%02d/%02d %02d:%02d:%02d]<br>", (1900+$year),$mon+1,$mday,$hour,$min,$sec;
		}
		{my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = localtime(time);
		printf "server localtime: [%d/%02d/%02d %02d:%02d:%02d]", (1900+$year),$mon+1,$mday,$hour,$min,$sec;
		}

		print "</td></tr></table>";
		print "<script language=javascript>document.forms.viewform.submit();</script>\n";
	}
	elsif ($frame eq 'bottom')
	{ 
		if($selected eq "") { print "blank<br>"; }
	  else 
    { my $file;
    	{
	    	my $f;
	    	#set file to the selected file, but check that it is in the list
	    	#of sanctioned log files.  This prevents an unfriendly respondant
	    	#from viewing any file on the system by hacking the form.
		foreach $f(&list_dirs($'log_viewer_dir))
		{ if($f eq $selected) { $file = $f; }
		}
	}
	  	print "<pre>";
	    if($filter)
	  	{ 	my ($name,$dir,$typ) = &filename_split($file);
	  	    my $temp = &dircat($'temp_dir,"temp$$.tmp");
	        my $cs = $casesensitive ? "" : "-i";
	        my $c1 = "grep -A $clines $cs \"$filter\" \"$file\"";
	        my $c2 = "tail -$lines \"$temp\"";
	        print "cmd: $c1\n$c2\n";
	        backquote($c1,$temp);
	        my ($val) = backquote($c2);
	        unlink($temp);
	  			&printlines($val);
	  	}else
	  	{ my ($val) = backquote("tail -$lines \"$file\"");
		  	&printlines($val);
  		}
	  	print "</pre>";
	  	print "<br>done<p>";
	  }
	}else
	{ &log_error_event("","log-viewer","unknown frame $frame");
	}
}}

print header;

&do_it();
