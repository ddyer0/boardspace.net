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
$cgi_lib::maxdata = 150000;

# headers for every page this script produces.  The result must never be
# cached, otherwise the browser can show a stale result page or picture.
$html_header = "Content-type: text/html\r\n"
             . "Cache-Control: no-cache, no-store, must-revalidate\r\n"
             . "Pragma: no-cache\r\n"
             . "Expires: 0\r\n\r\n";

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

# minimal html escaping for values echoed back to the browser
sub esc()
{  my ($s) = @_;
   $s = "" unless defined $s;
   $s =~ s/&/&amp;/g;
   $s =~ s/</&lt;/g;
   $s =~ s/>/&gt;/g;
   $s =~ s/"/&quot;/g;
   return($s);
}

# true if the file starts with the JPEG signature (FF D8 FF)
sub looks_like_jpeg()
{  my ($file) = @_;
   my ($head) = "";
   open(my $fh, "<", $file) or return(0);
   binmode($fh);
   read($fh, $head, 3);
   close($fh);

   return(($head eq "\xFF\xD8\xFF") ? 1 : 0);
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

  # Check the declared size BEFORE cgi-lib sees the request.  If it is too
  # big, read and discard the body so the browser can finish sending and is
  # able to display our message instead of "connection reset", then reply
  # with a proper header and a clean page.
  my $len = $ENV{'CONTENT_LENGTH'} || 0;
  if ($len > $cgi_lib::maxdata)
  {
    my ($left, $junk) = ($len < 20_000_000 ? $len : 20_000_000);
    while ($left > 0)
    {  my $n = read(STDIN, $junk, ($left > 8192) ? 8192 : $left);
       last unless $n;
       $left -= $n;
    }
    print $html_header;
    print &HtmlTop("File Upload Results");
    print "<b>Upload failed:</b> the picture is too large ",
          "($len bytes). The limit is $cgi_lib::maxdata bytes. ",
          "Please resize it and try again.\n";
    print &HtmlBot;
    exit 0;
  }

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
  print $html_header;
  print &HtmlTop("File Upload Results");

  print "The file's reported name on your machine was:\n",
        " <i>".&esc($cgi_cfn{'upfile'})."</i><br>\n",
        "The file's reported Content-type (possibly none) was:\n",
        " <i>".&esc($cgi_ct{'upfile'})."</i><br>\n";

    my (@name) = split("/",$cgi_data{'upfile'});
    my ($nameonly) =$name[$#name];
    my (@type) = &filename_split($nameonly);
    my ($typeonly) = lc($type[$#type]);
    my ($realname) = lc($username.$typeonly);
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
      if(&looks_like_jpeg($from))
      {
    if(-e $toname) { unlink($toname); }
    rename($from, $toname);
        # the time() suffix makes the browser fetch the new picture
        # instead of showing its cached copy of the old one
        print "<p>It's an image!<br><img src=\"".&esc($pubname.$realname)."?t=".time()."\"><br>\n";
      }
      else
      {
        unlink($from);
        print "<p><b>That file is not a valid JPEG image.</b>\n";
      }
    }
    else
  {
      unlink($from);
      print "<p><b>Only JPEG (.jpg) pictures are accepted.</b>\n";
    }
  }
  else
  {
    unlink($from);
  print "<b>Bad user name or password</b>\n";
      }

  print &HtmlBot;


  # The following lines are solely to suppress 'only used once' warnings
  $cgi_lib::writefiles = $cgi_lib::writefiles;
  $cgi_lib::maxdata    = $cgi_lib::maxdata;

}
