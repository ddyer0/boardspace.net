#!/usr/bin/perl 
#
# get the location of the applet classes.  Used by ssi for a few pages.
#
use strict;
use Debug;

$| = 1;				# force writes

$'javadir = "miniloader";
$'jardir = "$ENV{'DOCUMENT_ROOT'}/$'javadir";
$'host = $'ENV{'HTTP_HOST'};
#
# list the files in a directory, given a fullpath.  The returned list
# is names-only, but includes . and ..
#
sub list_dir()
{
   my ($directory) = @_;
    opendir (DIR, $directory);#or &cgierr ("Can't open dir: '$directory'.\nReason: $!");
    my @ls = readdir(DIR);
    closedir (DIR);
    return(@ls);
}

# print a list of the jars and their modification dates in the class directory.
sub print_appjarinfo()
{	my $dir = $ENV{'DOCUMENT_ROOT'} . "/$'java_dir/$'class_dir/";
	#print "Dir = $dir<p>";
	my $dir = $'jardir;
	my (@jars) = &list_dir($dir);
	my $host = "/$'javadir";
	print "version,1,$'host\n";
	print "# following the version, lines contain the unix date and file name of jar files\n";
	foreach my $jar (@jars)
	{ if( (index(lc($jar),'launcher.jar')<0)
			&& (index(lc($jar),".jar")>0))
			{
			my $modtime = (stat("$dir/$jar"))[9]; 
			print "$modtime,$host/$jar\n"; 
			}
	 }
}


print "Content-type: text/html\n\n";;
&print_appjarinfo(); 
