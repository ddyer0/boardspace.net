#!/usr/bin/perl
#
#
use CGI qw(:standard);
use CGI::Carp qw( fatalsToBrowser );
use Mysql;
use Debug;

require "include.pl";
require "tlib/common.pl";

#
# cgi-lib and configuration for it
#
#
require "tlib/cgi-lib.pl";
# temp directory.  Note that making these "var $pubname" breaks upload
# for obscure reasons - it causes the input to be absorbed resulting in
# "expected n got 0 messages
$pubname =$image_dir;
$basename = $ENV{'DOCUMENT_ROOT'}.$pubname;
$cgi_lib::writefiles = $basename."upload/";
# Limit upload size to avoid using too much memory
$cgi_lib::maxdata = 50000;


sub init {
	$| = 1;				# force writes
	__dStart( "$debug_log", $ENV{'SCRIPT_NAME'} );
}

#note that this is hard to modernize because loading tlib/gs_db.pl arms "strict"
#which breaks things in this old code.
sub check_password()
{   my ($name,$pass) = @_;
    my ($dbh) = Mysql->Connect($db_host,$database,$db_user,$db_password);              # connect to local mysqld
    my ($slashname) = $dbh->quote($name);
    my ($slashpass) = $dbh->quote($pass);
    my ($sth) = $dbh->query("SELECT * FROM players WHERE locked is null AND player_name=$slashname AND pwhash=MD5(concat($slashpass,uid))");
    my ($n) = $sth->numrows;
#    print "checking $name $slashname $pass $slashpass\n";
    return(($n==1)?1:0);
}

# split a file name (containing / for file separator) into name,path, and type
# INPUT: a path name
# OUTPUT: three strings for name,path,type
#
sub filename_split()
{  my($str) = @_;
   my($path,$name,$type);
   $str =~ y/\\/\//;#standardize on / in filenames
       $name=$str;
   $path="";
   $type="";

   my($slash) = rindex($str,"/");
   if($slash >=0) 
   {   $slash++;
       $path = substr($name,0,$slash); 
       $name = substr($name,$slash);
   }

   my($typeindex) = rindex($name,"\.");
   if($typeindex >= 0)
   {
       $type = substr($name,$typeindex);
       $name = substr($name,0,$typeindex);
   }
   return($name,$path,$type);
}

MAIN: 
{
  my (%cgi_data,  # The form data
      %cgi_cfn,   # The uploaded file(s) client-provided name(s)
      %cgi_ct,    # The uploaded file(s) content-type(s).  These are
                  #   set by the user's browser and may be unreliable
      %cgi_sfn,   # The uploaded file(s) name(s) on the server (this machine)
      $ret,       # Return value of the ReadParse call.       
     );
  &init();


  # Start off by reading and parsing the data.  Save the return value.
  # Pass references to retreive the data, the filenames, and the content-type
  #

  $ret = &ReadParse(\%cgi_data,\%cgi_cfn,\%cgi_ct,\%cgi_sfn);

  # A bit of error checking never hurt anyone
  if (!defined $ret) {
    &CgiDie("Error in reading and parsing of CGI input");
    } elsif (!$ret) {
    &CgiDie("Missing parameters\n",
	    "Please complete the form \n");
    } elsif (!defined $cgi_data{'upfile'}) 
    {
    &CgiDie("Data missing\n",
	    "No picture uploaded: please complete the form</a>.\n");
    }

  my ($username) = $cgi_data{'userid'};
  my ($password) = $cgi_data{'pass'};

	$username = &despace(substr($username,0,25));
	$password = &despace(substr($password,0,25));

  # Now print the page for the user to see...
  print &PrintHeader;
  print &HtmlTop("File Upload Results");

  print <<EOT;
The file's reported name on your machine was:
 <i>$cgi_cfn{'upfile'}</i><br>
The file's reported Content-type (possibly none) was:
 <i>$cgi_ct{'upfile'}</i><br>
EOT

    my (@name) = split("/",$cgi_data{'upfile'});
    my ($nameonly) =$name[$#name];
    my ($name,$path,$typeonly) = &filename_split($nameonly);
    my ($realname) = lc($username.".jpg");
    my ($from) = $basename."upload/".$nameonly;
    my ($toname) = $basename.$realname;
  if(&check_password($username,$password))
   {
  if (( $cgi_ct{'upfile'} eq "image/pjpeg")
      || ( $cgi_ct{'upfile'} eq "image/jpeg") 
      || ( lc($typeonly) eq ".jpg")  
	  || ( lc($typeonly) eq ".jpeg")    	
#  ||       ($cgi_ct{'upfile'} eq "image/gif")
   )
  {
    if(-e $toname) { unlink($toname); }
    rename($from, $toname);
    print "from $from to $toname\n";
    print "<p>It's an image!<br><img src=\"$pubname$realname\"><br>\n";
  }}else
  {
  print "<b>Bad user name or password</b>\n";
      }
#<hr>
#The contents of $cgi_data{'upfile'} are as follows:<br>
#<pre>

  # Print the contents of the uploaded file
#  open (UPFILE, $cgi_sfn{'upfile'}) or 
#    &CgiError("Error: Unable to open file $cgi_sfn{'upfile'}: $!\n");
#  $buf = "";    # avoid annoying warning message
#  while (read (UPFILE, $buf, 8192)) {
#    # Munge the uploaded text so that it doesn't contain HTML elements
#    # This munging isn't complete -- lots of illegal characters are left
#    # However, it takes care of the most common culprits.  
#    $buf =~ s/</&lt;/g;
#    $buf =~ s/>/&gt;/g;
#    print $buf;
#  }
#  close (UPFILE);
#  print "</pre>\n";

#  $cvt = `gs/gs /i$cgi_data{'upfile'} /o$cgi_data{'upfile'} /pcmulti.004 /fcmulti.8bf`;

#  print "<pre>\n$cvt\n</pre>\n";
#  my (@parts) = split(/\//,$cgi_data{'upfile'});
#  my ($name) = $parts[$#parts];
#  my ($dlname) = "/out/$name";
#  print "<table><td><tr>\n";
#  print "\n<hr><bre><img src=\"$dlname\" WIDTH=130>\n";
#  print "</tr><tr>Use your browser's <b>save as</b> option to <a href=\"$dlname\">Download the result</a></tr></td></table>\n";
#  print "<p>\n";
 

  #unlink ($cgi_sfn{'upfile'}) or
  # &CgiError("Error: Unable to delete file",
  #           "Error: Unable to delete file $cgi_sfn{'upfile'}: $!\n");
  # cleanup - delete the uploaded file
  # Note that when using spooling of files to disk, the uploaded file's
  # name on the server machine is in both %cgi_data and %cgi_sfn
  # (that is, the first and fourth parameters to ReadParse).  However,
  # for technical reasons, the data in %cgi_data are tainted.  The data in
  # %cgi_sfn are not tainted, but the keys can contain only a limited
  # set of characters ([-\w] in cgi-lib 2.8)
  #  print "<hr>File $cgi_data{'upfile'} has now been removed\n";


  print &HtmlBot;


  # The following lines are solely to suppress 'only used once' warnings
  $cgi_lib::writefiles = $cgi_lib::writefiles;
  $cgi_lib::maxdata    = $cgi_lib::maxdata;

}
