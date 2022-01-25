use strict;
use LWP::Simple ('getstore','get');   #import the head method only
use LWP::UserAgent;
use URI::Escape;
use CGI::Cookie;
use HTML::Entities;
use Digest::MD5 'md5_hex';
#
# note that when adding languages, it's also necessary
# to change the enum in the translations table
#
%'language_codes = 
	( 
	#"" => "english",			#default language
	"en"=>"english",
	"zn"=>"chinese",
	"fr"=>"french",
	"cs"=>"czech",
	"ro"=>"romanian",
        "sv"=>"swedish",
#	   "sk"=>"slovak",
#		"hu"=>"hungarian",
		"de"=>"german",
#		,"da"=>"danish",
"eo"=>"esperanto",
#   "fi"=>"finnish",
    "es"=>"spanish",
#     "ms"=>"bahasa",
     "pl"=>"polish",
     "pt"=>"portuguese",
     "jp"=>"japanese",
     "it"=>"italian",
     "no"=>"norwegian",
	"ru"=>"russian",
     "ca"=>"catala",
     "nl"=>"dutch",
	);
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

sub list_dirs()
{
  my ($dirlist) = @_;
  my @res;
  my $n;
  my @sub = split(/[,]/,$dirlist);
  my $fil;
  foreach $fil(@sub)
  {  foreach $n(&list_dir($fil)) { push(@res,&dircat($fil,$n)); }
  }
  return(@res);
}
#
# call with pushnew(\@arr,val)
#
sub pushnew()
{	my ($ar,$val) = @_;
	my @arr = @$ar;
	my $idx = $#arr;
	while($idx >= 0)
	{	if($arr[$idx]==$val) { return(0); }
		$idx--;
	}
	push(@$ar,$val);
	return(1);
}

sub select_option()
{	my ($name,$value,$default) = @_;
	my $issel = (lc($value) eq lc($default)) ? "selected" : "";
	return "<option value='$value' $issel>$name</option>\n";
}

sub select_language_menu()
{	my ($def,$auto) = @_;
	my $key;
	my $issel = ($def eq '') ? "selected" : "";
	my $ac = $auto ? " onchange=this.form.submit()" : "";
	print "<select name='language' $ac>\n";
	print "<OPTION VALUE='' $issel>"
	. &trans("Select Language")
	. "</OPTION>\n";
	foreach $key (keys(%'language_codes)) 
	{	my $val = $'language_codes{$key};
	    my $tval = &trans($val);
		print &select_option($val,$tval,$def);
	}
	print "</select>\n";
}

sub month_string()
{ my ($when) = @_;
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime($when);
	my $month = ( "January", "February", "March", "April", "May",
				"June", "July", "August", "September", "October", "November", "December")[ $mon ];
	return sprintf "%s %d", $month, $year+1900 ;
}

# current time as a pure string, no month names
sub ctime()
  { 
   my ($sec,$min,$hr,$da,$mo,$yr) = gmtime(time());
   if($yr<1000) { $yr+=1900; }
   $mo++;
   my $val = sprintf "%d-%02d-%02d %02d:%02d:%02d",$yr,$mo,$da,$hr,$min,$sec;
   return($val);
 }
 
sub date_string()
{ my ($when,$notime) = @_;
	if($when==0)
	{ return(&trans("NeverHappened"));
	}else
	{
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime($when);
	my $month = &trans(( "January", "February", "March", "April", "May",
				"June", "July", "August", "September", "October", "November", "December")[ $mon ]);
	if($notime)
	{	return(&trans('datestring',$year+1900,$month,$mday));
	}
	else
	{
	 my $timestring = sprintf "%02d:%02d:%02d GMT", $hour, $min, $sec;
	 return(&trans('date and time',$year+1900,$month,$mday,$timestring));
	}
	}
}

sub challengeValue()
{
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = localtime(time);
	$mon++;
	if($year<1000) { $year += 1900; }
	if( ($year>2003) || ($mon>1))
	{		#after feb 2003
			if($mon<10) { $mon = "0$mon"; }
			return("$year$mon");
	}
	else
	{	#before feb 2003
		return("1");
	}
}

sub current_time_string()
{	return(&date_string(time));
}

#
# concatinate two directory elements, so there is exactly one / between them
#
sub dircat()
{my ($one,$two) = @_;
 my $d1 = (substr($one,-1) eq "/");
 my $d2 = (substr($two,0,1) eq "/");
 #print "dx s1=$one s2=$two = $d1 $d2<br>\n";
 if($d1 && $d2) {return($one.substr($two,1));}
 if($d1 || $d2) {return($one.$two); }
 return($one."/".$two);
}


#
#insert a text file into the output stream
#
sub insert_file()
{my ($path) = @_;
 if(open(IMAGE, "$path"))
 {while (<IMAGE>){print $_;}
  close(IMAGE);
}
}
#
#insert a text file into the output stream
#
sub read_file()
{my ($path) = @_;
 my $val;
 if(open(IMAGE, $path))
 {while (<IMAGE>){$val .= "$_";}
  close(IMAGE);
	}
return($val);
}

sub write_file()
{	my ($path,$val) = @_;
	if(open(FILE,">$path")) { print FILE $val;	close(FILE); }
	else { print "open $path failed<p>\n";}

}

# return the unix date at which players are considered retired
# due to inactity
#
sub player_retired_time()
{	my $months=&param('months');
	if($months=='') { $months=$'retire_months; }
	my $sunset = time()-60*60*24*30*$months; 
	return($sunset);
}

sub timeago()
{	my ($pdate) = @_;
	my $ago = time()-$pdate;
	my $daysago = int($ago/(60*60*24));
		
	if($daysago==0) 
		{ my $hrsandminutes = $ago-$daysago*60*60*24;
			my $hrsago = int($hrsandminutes/(60*60));
			if($hrsago==0)
			{ $daysago = &trans("minutes ago");
			}else
			{	$daysago = ($hrsago==1) 
									? &trans("1 hour ago")
									: &trans("N1 hours ago",$hrsago);
			}
		}
		elsif($daysago==1) { $daysago=&trans("yesterday");}
		else 
		{
		my $yearsago = int($daysago/365); 
		$daysago -=($yearsago*365);
		my $monthsago = int($daysago/30);
		if(($yearsago==1) && ($monthsago<=1)) { $yearsago = 0; $monthsago += 12; }
		my $weeksago =int($daysago/7);
		$daysago -= ($monthsago*30);
		if($yearsago>1) { $daysago = &trans("N1 years ago",$yearsago); }
		  elsif($yearsago>0 && $monthsago>1) { $daysago = &trans("1 year+#1 months ago",$monthsago); }
		  elsif ($monthsago>3) { $daysago = &trans("N1 months ago",$monthsago); }
		  elsif ($weeksago>1) { $daysago = &trans("N1 weeks ago",$weeksago); }
		  else { $daysago = &trans("N1 days ago",$daysago); }
	   }
	$daysago =~ s/ /&nbsp;/g;       
	return($daysago);
}

# concatinate two directory elements, so there is exactly one / between them
#
sub dircat()
{	my ($one,$two) = @_;
	my $d1 = (substr($one,-1) eq "/");
	my $d2 = (substr($two,0,1) eq "/");
	#print "dx s1=$one s2=$two = $d1 $d2<br>\n";
	if($d1 && $d2) {return($one.substr($two,1));}
	if($d1 || $d2) {return($one.$two); }
	return($one."/".$two);
}
#
# split a file name (containing / for file separator) into name,path, and type
# INPUT: a path name
# OUTPUT: three strings for name,path,type
#
sub filename_split()
{  my($name) = @_;
   my($path,$type);
   $name =~ y/\\/\//;		#standardize on / in filenames
   $path="";
   $type="";

   if("/" eq substr($name,-1)) 
   	{ $name = substr($name,0,-1); 
   	}
   
   my($slash) = rindex($name,"/");
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

# standard anti-spam email
sub standard_email()
{  my ($em) = @_;
   if($em eq '') { $em = $'supervisor_email;  }
    &obfuscateHTML("<a href='mailto:$em'>$em</a>");
}

sub standard_footer 
{	
	my $home = &trans('BoardSpace.net home page');
	my $goto = &trans('Go to');
	my $email = &trans('E-mail:');
	my $emailout = &standard_email();
	print <<Footer;
<P>
<hr>
<center>
<table width=100%>
<tr><td align=left>
<font size=-1>
$email $emailout
</font>
</td><td align="right">
$goto <A HREF="/">$home</A>
</td></tr>
</table>
</center>

Footer
&honeypot();
}


@'countries = (
	"Select country",
	"Afghanistan",
	"Albania",
	"Algeria",
	"Andorra",
	"Angola",
	"Antarctica",
	"Antigua and Barbuda",
	"Argentina",
	"Armenia",
	"Australia",
	"Austria",
	"Azerbaijan",
	"Bahamas",
	"Bahrain",
	"Bangladesh",
	"Barbados",
	"Belarus",
	"Belgium",
	"Belize",
	"Benin",
	"Bermuda",
	"Bhutan",
	"Bolivia",
	"Bosnia Hercegovina",
	"Botswana",
	"Brazil",
	"Brunei Darussalam",
	"Bulgaria",
	"Burkina Faso",
	"Burundi",
	"Cambodia",
	"Cameroon",
	"Canada",
	"Cape Verde",
	"Central African Republic",
	"Chad",
	"Chile",
	"China",
	"Colombia",
	"Congo",
	"Costa Rica",
	"Cote d'Ivoire",
	"Croatia",
	"Cuba",
	"Cyprus",
	"Czech Republic",
	"Denmark",
	"Djibouti",
	"Dominica",
	"Dominican Republic",
	"East Timor",
	"Ecuador",
	"Egypt",
	"El Salvador",
	"Equatorial Guinea",
	"Estonia",
	"Ethiopia",
	"Fiji",
	"Finland",
	"France",
	"French Guiana",
	"Gabon",
	"Gambia",
	"Georgia",
	"Germany",
	"Ghana",
	"Greece",
	"Grenada",
	"Guadeloupe",
	"Guam",
	"Guatemala",
	"Guinea",
	"Guinea Bissau",
	"Guyana",
	"Haiti",
	"Honduras",
	"Hong Kong",
	"Hungary",
	"Iceland",
	"India",
	"Indonesia",
	"Iran",
	"Iraq",
	"Ireland",
	"Israel",
	"Italy",
	"Jamaica",
	"Japan",
	"Jordan",
	"Kazakhstan",
	"Kenya",
	"Kiribati",
	"Korea North",
	"Korea South",
	"Kuwait",
	"Kyrgyzstan",
	"Laos",
	"Latvia",
	"Lebanon",
	"Lesotho",
	"Liberia",
	"Libya",
	"Liechtenstein",
	"Lithuania",
	"Luxembourg",
	"Macau",
	"Madagascar",
	"Malawi",
	"Malaysia",
	"Maldives",
	"Mali",
	"Malta",
	"Mauritania",
	"Mauritius",
	"Mexico",
	"Micronesia",
	"Moldova",
	"Monaco",
	"Mongolia",
	"Morocco",
	"Mozambique",
	"Myanmar",
	"Namibia",
	"Nauru",
	"Nepal",
	"Netherlands",
	"New Caledonia",
	"New Zealand",
	"Nicaragua",
	"Niger",
	"Nigeria",
	"Niue",
	"Norway",
	"Oman",
	"Pakistan",
	"Palau",
	"Panama",
	"Papua New Guinea",
	"Paraguay",
	"Peru",
	"Philippines",
        "Planet Earth",
	"Poland",
	"Portugal",
	"Puerto Rico",
	"Qatar",
	"Romania",
	"Russian Federation",
	"Rwanda",
	"Saint Helena",
	"Saint Kitts and Nevis",
	"Saint Lucia",
	"Saint Pierre and Miquelon",
	"Saint Vincent and the Grenadines",
	"Samoa",
	"San Marino",
	"Sao Tome and Principe",
	"Saudi Arabia",
	"Senegal",
	"Seychelles",
	"Sierra Leone",
	"Singapore",
	"Slovenia",
	"Slovak Republic",
	"Solomon Islands",
	"Somalia",
	"South Africa",
	"Spain",
	"Sri Lanka",
	"Sudan",
	"Suriname",
	"Swaziland",
	"Sweden",
	"Switzerland",
	"Syria",
	"Taiwan",
	"Tajikistan",
	"Tanzania",
	"Thailand",
	"Togo",
	"Tokelau",
	"Tonga",
	"Trinidad and Tobago",
	"Tunisia",
	"Turkey",
	"Turkmenistan",
	"Tuvalu",
	"Uganda",
	"Ukraine",
	"United Arab Emirates",
	"UK",
	"USA",
	"Uruguay",
	"Uzbekistan",
	"Vanuatu",
	"Vatican",
	"Venezuela",
	"Vietnam",
	"Western Sahara",
	"Yemen",
	"Yugoslavia",
	"Zaire",
	"Zambia",
	 "Zimbabwe"
);

sub print_country_selector
{  my ($current) = @_;
   my $selectedindex = 0;
   my @cc = @'countries;
   my $c = 0;

   $current = lc($current);
   while ($c < $#cc) 
     { if( lc($cc[$c]) eq $current) 
       { $selectedindex=$c; 
         $c = $#cc; 
       }
       $c++;
     }
   $c = 0;
   print "<select name=country>\n";
   while ($c <= $#cc) 
     { my $sel = ($c == $selectedindex) ? "selected" : "";
       my $tc = &trans($'countries[$c]);
       print "<option $sel value=\"$cc[$c]\">$tc</option>\n";
      $c++;
     }
   print "</select>\n";
}

sub print_country_form()
{	my ($country) = @_;
	print "<form action=$ENV{'SCRIPT_NAME'}>\n";
	my $name;
	foreach $name (param())
	{ my $val = param($name);
	  if(!($name eq 'country') && !($name eq 'myname'))
	  	{print "<input type=hidden name=$name value=\"$val\">\n";
	  	}
	}
  &print_country_selector($country); 
  my ($nat) = &trans('View National Table');
  print "<input type=submit value=\"$nat\">\n";
	print "</form>\n";
	
}

sub timestamp_to_date()
{	my ($stamp) = @_;
	my $year = substr($stamp,0,4);
	my $mo = substr($stamp,4,2);
	my $da = substr($stamp,6,2);
	my $hr = substr($stamp,8,2);
	my $mm = substr($stamp,10,2);
	return("$year-$mo-$da $hr:$mm");
}

# print a row for a table, as many columns as are supplied
sub print_row()
{	my @r = @_;
	my $i=0;
	my $n=$#r;
	print "<tr>\n";
	while($i<=$n)
	{ $i++;
	  my $item=shift(@r);
	  print "<td>$item</td>\n";
	}
	print "</tr>\n";
	#print "@r<p>\n";
}

# remove leading and trailing spaces
sub despace()
{	my ($str) = @_;
	while ((length($str)>=1) && (substr($str,0,1) eq ' '))
	{ $str = substr($str,1);
	}
	while((length($str)>=1) && (substr($str,length($str)-1) eq ' '))
	{ $str = substr($str,0,length($str)-1);
	}
	return($str);
}

# true if name is letters and numbers only
sub validname()
{	my ($str) = @_;
  my $valid = "abcdefghijklmnopqrstuvwxyz0123456789";
  my $i=0;
  my $len = length($str);
  while ($i<$len)
	{ my $ch = lc(substr($str,$i,1));
	  my $ind = index($valid,$ch);
	  if($ind<0) { return(0); }
		$i++;
	}
	return(1);
}

sub print_option_list()
{	my ($current,@option_list) = @_;
	my $op;
	foreach $op (@option_list)
	{	my $sel = ($current eq $op) ? "selected" : "";
		print "<OPTION $sel value='$op'>$op</OPTION>\n";
	}
	#for testing
	#print "<OPTION value='bad'>bad</OPTION>\n";
}
# make a silent log entry
sub log_event()
{	my ($logfile,$name,$message) = @_;
	my $uri = $ENV{'REQUEST_URI'};
	my $caller = $ENV{'REMOTE_ADDR'};
	my $root = $ENV{'DOCUMENT_ROOT'};
	if($logfile eq "") { $logfile = $'perl_log; };
	my $filename = ">>$logfile";
	open(FILE,$filename);
	my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdast) = gmtime(time);
	printf FILE "[%d/%02d/%02d %02d:%02d:%02d]", (1900+$year),$mon+1,$mday,$hour,$min,$sec;
	print FILE " log request from $name ($uri) for $caller\n";
	print FILE "$message\n\n";
	close(FILE);
}
# make a noisey log entry
sub log_error_event()
{	my ($logfile,$name,$message) = @_;
	&log_event($logfile,$name,$message);
	print "<br>\n<b>error from $name</b>:<blockquote> $message</blockquote><br>\n";
	&countEvent($logfile,$'perl_error_alert_level,$'perl_error_panic_level);
}
sub log_error()
{	my ($message,$source)=@_;
	my ($package, $filename, $line) = caller;
	if($source eq "") { $source = "$filename line $line"; }
	if($'debug_mysql)
	{
		$source = $source . &stacktrace();
	}
	&log_error_event($'perl_log,$source,$message);
}


# CgiError
# Prints out an error message which which containes appropriate headers,
# markup, etcetera.
# Parameters:
#  If no parameters, gives a generic error message
#  Otherwise, the first parameter will be the title and the rest will 
#  be given as different paragraphs of the body

sub CgiError {
  my (@msg) = @_;
  my ($i,$name);

  if (!@msg) {
    $name = &MyFullUrl;
    @msg = ("Error: script $name encountered fatal error\n");
  };

  if (!$cgi_lib'headerout) { #')
    &PrintHtmlHeader();  
  }
  print "<br><b>$msg[0]</b>\n";
  foreach $i (1 .. $#msg) {
    print "<br>$msg[$i]\n";
  }
  print "<br>\n";
  
  $cgi_lib'headerout++;
}
$cgi_lib'header_printed=0;
sub PrintHeader {
  return "Content-type: text/html\n\n";
}

sub PrintHtmlHeader()
{  my ($batch) = @_;
  if(!$cgi_lib'header_printed)
  {  if(!$batch) { print &PrintHeader(); }
    $cgi_lib'header_printed=1;
  }
}

# CgiDie
# Identical to CgiError, but also quits with the passed error message.

sub CgiDie {
  my (@msg) = @_;
  &CgiError (@msg);
  die "fatal error (@msg)";
}

#
# get a mandatory non-blank constant
# use this for internal non-blank constant that should
# be set up, and for which it is a serious problem if they
# are not.
#
sub xconst()
{ my($val) = @_;
  if(!$val) 
    {  my ($package, $filename, $line) = caller;
      &CgiDie("missing parameter in $filename at line $line"); 
    }
  return($val);
}
#
# get a mandatory non-blank parameter
# use this for internal non-blank parameters that should
# be passed around ie; job number etc.
# not for user input parameters
#
sub xparam()
{ my($name) = @_;
  my $val = param($name);
  if(!$val)
    { my ($package, $filename, $line) = caller;
     &CgiDie("missing parameter: $name in $filename at $line"); 
    }
  return($val);
  
}

sub readconfigfile()
{	my ($file) = @_;
	my (%info) = ();
	$lib'nullval = "(null)";
	if(!$file) { $file = "missing.config"; }
	if(open(IMAGE, $file))
  {	my $var='';
        my $loop=0;
    while (<IMAGE>)
  		{ my $line = $_;
                  $loop++;
  			#substitute our special null value for ""
  			#
  			if((length($line)>2) && !(substr($line,0,1) eq "#"))
  				{
  				my $com = index($line,",");
  				if($com==0)
  				{ # lines that begin with a comma are continuation lines
  				  my $sub = substr($line,1);
  				  $info{$var}= "$info{$var}\n$sub";
  				} 
  				elsif($com>0)
  					{ $var = substr($line,0,$com);
  						my $val = substr($line,$com+1,-1);
 		 				if(!$val) { $val = $lib'nullval; }
 						my $len  = length($line);
   					#print "$com $len $var = ($val)<br>\n";
   					my $oldval=$info{$var};
		   			if($oldval) 
		   				{ &log_error_event($lib'logfile,"readconfig",
		   							"config var $var already has value $oldval, new value is $val");
		   				}
		 				$info{$var}=$val;
		  				}
		  				else
  					{	&log_error_event($lib'logfile,"readconfig","no comma in log line:$loop: $line");
  					}
  				}
  		}
  	close(IMAGE);
  }else
	{ 
		&log_error_event($lib'logfile,"readconfig","missing config file: $file");
	}
	return(%info);
}

sub addNewTrans()
{	my ($newk,$cxt) = @_;
	if($lib'translations_loaded)
        {
	my $dbh = &connect();
	if($dbh)
	{
	my $qnew = $dbh->quote($newk);
	my $qc = $dbh->quote($cxt);
	&commandQuery($dbh,"replace into translation set keystring=$qnew,translation=$qnew,context=$qc,language='english',collection='web'");
	}
	&disconnect($dbh);
}}
sub trans()
{	# this magic call to encode entities will encode nonstandard ascii (ie; chinese)
	# but leave regular ascii untouched.  This allows simple html tags to be embedded
	# in the translation, which still protecting the unioode chartacters.
	my (@args) = @_;
	my ($package,$file,$line) = caller;
	return(&rawtrans_caller(1,$package,$file,$line,@args));
}

sub rawtrans()
{	my (@args) = @_;
	my ($package,$file,$line) = caller;
	return(0,&rawtrans_caller($package,$file,$line,@args));
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
#
# translate with caller id in case we start something new
#
sub rawtrans_caller()
{	my ($enc,$package,$file,$line,$varname,@args) = @_;
    if(!$lib'language) 
		{ my ($package,$file,$line) = caller;
		  &log_error("translations for \"$varname\" not set up yet,called from $file:$line"); 
		  }
	my $val = $lib'translations{$varname};
	if($val eq "") 
	{ 
		my $vn = $varname;
		$vn =~ s/</&lt;/g;
		#&log_error_event("","$file line $line","Missing $'language translation key: $vn val=$val");
		my ($na,$pa,$ty) = &filename_split($file);
		&addNewTrans($varname,"$na$ty : $line");
		$val = "<font color='red'>$varname</font>";
		#my $key;
		#foreach $key(keys(%lib'translations))
		#{print "key : $key val = $lib'translations{$key}<br>";
		#}
	}
	elsif($val eq $lib'nullval) { $val = "" };
  if($enc) 
	{ # this protects standard ascii characters from encoding, including cr, lf and tab 
	  $val = encode_entities($val,'^\n\r\t\x20-\x25\x27-\x7e'); 
	}
  {	
  my $i = $#args+1;
  while($i>0)
  { 
    my $ss = "#$i";
    my $ll = index($val,$ss);
    $i--;
    while(($ll>0) && (substr($val,$ll-1,1) eq '&')) { $ll = index($val,$ss,$ll+1); }
    if($ll>=0)
	{	substr($val,$ll,length($ss),$args[$i]);
	}
	else
	{	if($'debug_mysql)
			{ 
			&log_error("missing $'language translation key $ss for $varname in value $val");
			}
			else
			{
			&log_event("missing $'language translation key $ss for $varname in value $val");
			}
	}
   }
  }
	return($val);
}
sub flag_image($file)
{	my ($file)=@_;
  if(($file eq '') || ($file eq '?') || ($file eq '--') || (lc($file) eq 'select country'))
		{ $file = 'Planet Earth';
		}
	return("/images/flags/${file}.gif");
}
# a more severe form of uri_escape that makes the argument safe to use
# in GET style urls 
sub url_escape()
{  my ($arg) = @_;
  return(uri_escape($arg,"^A-Za-z0-9"));
}
#
# format a URL with arguments escaped properly
#
sub get_url()
{  my ($url,%args)=@_;
    my $argstring="";
    my $key;
    foreach $key (keys(%args))
    { my $prefix = ($argstring eq "") ? "?" : "&";
      $argstring .= $prefix.&url_escape($key)."=".&url_escape($args{$key});
    }
  return($url.$argstring);
}
sub loadSiteString()
{	return "<script src='/js/site.js?1'></script>";
}

sub standard_header
{	my $site = &loadSiteString();
    print <<info
$site
<body style="background-image: url(/images/background-image.jpg);">
<center><img src="/images/master-logo.jpg"></center>
<br>

info

}

sub get_selector_string()
{
	my ($name,$default,@choices) = @_;
	my $val =  "<select name=$name>\n";
	foreach my $ch (@choices)
	{	my $sel = (lc($default) eq lc($ch)) ? "selected" : "";
		$val .= "<option value='$ch' $sel>$ch</option>\n";
	}
	$val .= "</select>\n";
	return($val);
}
#
# like trans, but no translation
#
sub noTrans()
{	my ($val,@args) = @_;	
	my $i = $#args+1;
    while($i>0)
   { 
    my $ss = "#$i";
    my $ll = index($val,$ss);
    $i--;
    if($ll>=0)
	{	substr($val,$ll,length($ss),$args[$i]);
	}
	else
	{	&log_error("missing substitution key $ss in $val");
	}
  }
  return($val);
}
sub get_translated_selector_string()
{
	my ($name,$trans,$default,@choices) = @_;
	my $val =  "<select name=$name>\n";
	foreach my $ch (@choices)
	{	my $sel = (lc($default) eq lc($ch)) ? "selected" : "";
		my $tch = $ch ? &trans(&noTrans($trans,$ch)) : "";
		$val .= "<option value='$ch' $sel>$tch</option>\n";
	}
	$val .= "</select>\n";
	return($val);
}
sub hexEncode()
{	my ($str) = @_;
	my $res = "";
	my $len = length($str);
	my $idx=0;
	while($idx<$len)
	{	$res .= sprintf("%2.2X",ord(substr($str,$idx++,1)));
	}
	return($res);
}

sub hexDecode($)
    {
    	## Convert each two-digit hex number back to an ASCII character.
    	(my $str = shift) =~ s/([a-fA-F0-9]{2})/chr(hex $1)/eg;
    	return $str;
    }

sub obfuscateHTML()
{	my ($str) = @_;
	my $hestr = lc(&hexEncode($str));
	my $part1 = substr($hestr,5);
	my $part2 = substr($hestr,0,5);
	return("<script>obPrint('$part1','$part2')</script>");
}
#
# encode an internal string as a pure ascii, with unicode codepoints encoded as \uxxxx
#
sub utfEncode()
{	my ($str) = @_;
	my $len = length($str);
	my $res = "";
	my $idx = 0;
	while($idx<$len)
	{	my $c = substr($str,$idx++,1);
	    my $ch = ord($c);
		if($ch>=128) { $ch = sprintf("\\u%04x",$ch); }
		else { $ch = chr($ch); }
		$res .= $ch;
	}
	return($res);
}
# convert a hex charactger code to a number
sub hexValue()
{	my ($ch) = @_;
	if(($ch>=ord('0')) && ($ch<=ord('9'))) { return($ch-ord('0')); }
	if(($ch>=ord('a')) && ($ch<=ord('f'))) { return(($ch-ord('a'))+10); }
	if(($ch>=ord('A')) && ($ch<=ord('F'))) { return(($ch-ord('A'))+10); }
	return(0);
}
#
# decode a string with embeddd \uxxxx to an internal string
#
sub utfDecode()
{	my ($str) = @_;
	my $len = length($str);
	my $out = "";
	my $idx = 0;
	while($idx<$len)
	{	my $ch = substr($str,$idx,1);
		$idx++;
		if(($ch eq '\\') 
			&& ($idx<$len) 
			&& ((substr($str,$idx,1) eq 'u')))
		{	my $v = 0;
			$idx++;
			my $lim = ($len < ($idx+4)) ? $len : ($idx+4);
			while($idx<$lim) 
			{ $v = $v*16 + &hexValue(ord(substr($str,$idx,1)));
			   $idx++; 
			}
			$ch = chr($v);
		}
		$out .= $ch;
	}
	return($out);
}

#
# this header is added to outgoing emails so the bounce dector can
# identify the intended used
#
sub userAuthenticator()
{
    my ($to,$pass) = @_;
    my $salt = int(rand(999999999));
    my $passon = $pass ? "true" : "false";
    my $cs = &md5_hex("$to$passon$salt");
    return("x-boardspace-user: $to $passon $salt $cs ");
}

sub send_mail_to()
{	my ($touser,$from,$to,$sub,$body) = @_;
	my $auth = &userAuthenticator($touser,1);
	my $msg = 	"$auth\nSender: $from\nFrom: $from\nTo: \"$touser\" <$to>\nSubject: $sub\n\n$body\n";
	open( SENDMAIL, "| $'sendmail -f $from $to" );
	print SENDMAIL $msg;
    close SENDMAIL;
}

sub send_mail()
{	my ($from,$to,$sub,$body) = @_;
	my $msg = "Sender: $from\nFrom: $from\nTo: $to\nSubject: $sub\n$body\n";
	open( SENDMAIL, "| $'sendmail -f $from $to" );
	print SENDMAIL $msg;
    close SENDMAIL;
}

sub edit_user_info_message()
{	my ($toname) = @_;
	my $editlink = &trans("edit_user_information_note",$toname,$toname);
	return($editlink);
}
sub bannerCookie()
{
  my %cookies = fetch CGI::Cookie();
  my $bannercookie = $cookies{'client'};
  if($bannercookie)
    {  $bannercookie = &decrypt($bannercookie->value,$'tea_key);
       #partial defense against junk cookies.  
       if(length($bannercookie)<4) { $bannercookie=0; }
    } 
  return($bannercookie+0);
}
sub timezoneCookie()
{
  my %cookies = fetch CGI::Cookie();
  my $cook = $cookies{'timezoneoffset'};
  my $tzcookie = $cook ? $cook->value : 0;
  return($tzcookie);
}

sub timezoneOffsetString()
{	my ($timezoneoffset) = @_;
	my $neg = "-";
	if($timezoneoffset eq "") { $timezoneoffset = 0; }
    if($timezoneoffset<0) { $timezoneoffset = -$timezoneoffset; $neg = "+"; }
    my $frac = int((($timezoneoffset % 60)/60)*10);
    my $hrs = int($timezoneoffset/60);
    my $minus = ($timezoneoffset==0) ? "" : $neg . "$hrs" . (($frac == 0) ? "" : ".$frac");
	return($minus);
}


# return only acceptable languages. 
sub select_language()
{ my ($languageName) = @_;

  # supply language from cookies

  if($languageName eq "")
  {
   my %cookies = fetch CGI::Cookie();
   my $lcookie = $cookies{'language'};
   if($lcookie) { $languageName=$lcookie->value; }
  }

  {
  # reject languages not in the list
  my $val;
  my $found = 0;
  $languageName=lc($languageName);
  foreach $val (values(%'language_codes))
   { if($val eq $languageName) {$found=1; }
   }
  if(!$found) { $languageName = ""; } 
  }

  # supply language from browser
  if($languageName eq "")
	{
    my ($lang) = split(/[,-]/,$ENV{'HTTP_ACCEPT_LANGUAGE'}."-a" );
	$lang = lc($lang);
	$languageName = $'language_codes{$lang};
	if($languageName eq "") { $languageName=$'language_codes{""}};
	}
  if("" eq $languageName) { $languageName="english"; }
  return($languageName);
}

%lib'translations=();
$lib'nullval="";
$lib'language='';
$lib'translations_loaded=0;

#
# read the translations from the database
#
sub readtrans_db()
{	my ($dbh,$ll) = @_;
	my $test = &param('test');
	if($ll eq '') { $ll = param('language'); }
	my $lan = &select_language($ll);
	if($lan eq '') { $lan = 'english'; }
	$'language = $lan;
	if(!($lib'language eq $lan))
	{
	$lib'language = $lan;
	my %tr;
	#print "reading db translations for $lan<p>";
	my $qlang = $dbh->quote($lan);
	my $q = "select keystring,translation,language from translation "
			 . " where (language='english' or language=$qlang) and (collection='web') "
			 . " order by language";
	#
	# query gets all the english first, then the other language if it exists
	#
	my $sth = &query($dbh,$q);
	my $nr = &numRows($sth);
	while($nr-- > 0)
	{
		my ($key,$trans,$lang) = &nextArrayRow($sth);
		$trans = &utfDecode($trans);
		$key = &utfDecode($key);
		#print "$lang $key<br>";
		if($test) { $trans = "<font color=blue>$trans</font>"; }
		$tr{$key}=$trans;
	}
	&finishQuery($sth);
	%lib'translations = %tr;
    $lib'translations_loaded=1;
	}
	return(%lib'translations);
}
sub debugging()
{
	return(($ENV{'REQUEST_URI'} eq ''));
}
1
