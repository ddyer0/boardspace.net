use strict;
use LWP::Simple ('getstore','get');   #import the head method only
use LWP::UserAgent;
use URI::Escape;
use CGI::Cookie;
use HTML::Entities;

require "tlib/xxtea.pl";

#
# 11/17/2020
# supporting code to allow combining and obfuscating parameters
# for important scripts.  Particularly login and score updates.
# 
# the general ideal is to take the old &par=val&par2=val paramerers
# passed as the payload of POST transactions, checksum it and convert
# the checksummed string to base64, and pass the new parameter as a
# single parameter.   At the receiving end (that's here) the parameter
# is un-base64d, validated, parsed, and the recovered parameters are
# stuffed into the &param database, so the existing code that expects
# cgi-parameters is unchanged.
#

 sub decode64 {
    my($d) = @_;
    $d =~ tr!A-Za-z0-9+/!!cd;
    $d =~ s/=+$//;
    $d =~ tr!A-Za-z0-9+/! -_!;
    my $r = '';
    while( $d =~ /(.{1,60})/gs ){
        my $len = chr(32 + length($1)*3/4);
        $r .= unpack("u", $len . $1 );
    }
    $r;
}
#	/**
#	  * Translates the specified byte array into Base64 string.
#	  * with \n every 100 input characters and = or == at the end to mark incomplete octets
#	  * @param buf the byte array (not null)
#	  * @return the translated Base64 string (not null)
#	  */
    sub encode64()
    {	my ($buf) = @_;
		return(&encode64_internal($buf,1));
	}
	  
#	/**
#	  * Translates the specified byte array into Base64 string.
#	  * and = or == at the end to mark incomplete octets
#	  * this does NOT add any \n for readability
#	  * @param buf the byte array (not null)
#	  * @return the translated Base64 string (not null)
#	  */
    sub encode64long()
    {
		my ($buf) = @_;
		return &encode64_internal($buf,0);
	}

	sub encode64_internal()
	{	my ($buf,$addcr) = @_;	
		my @ALPHABET = unpack("W*","ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");
		my $str = "";
        my $i=0;
        my $skip1=0;
        my $skip2=0;
        my @chars = unpack("W*",$buf);
        my $size = $#chars+1;
        #print "@ALPHABET\n";
        #print "$buf\n";
        #print "@chars\n";
        while($i < $size){
            if($addcr && $i>0 && ($i%99==0)) { $str.="\n"; }
	        my $b0 = $chars[$i++];
            my $b1 = 0;
             if($i < $size) { $b1 = $chars[$i++]; } else { $skip1=1; };
            my $b2 = 0;
             if($i < $size) { $b2 = $chars[$i++]; } else { $skip2=1; };
            my $mask = 0x3F;
            $str .= chr($ALPHABET[($b0 >> 2) & $mask]);
            $str .= chr($ALPHABET[(($b0 << 4) | (($b1 & 0xFF) >> 4)) & $mask]);
            $str .= $skip1 ? "=" : chr($ALPHABET[(($b1 << 2) | (($b2 & 0xFF) >> 6)) & $mask]);
            $str .= $skip2 ? "=" : chr($ALPHABET[$b2 & $mask]);
        }
        $str .= "\n";
       return $str;
    }
    
#
# this is a simple nonscientific checksum intended to validate
# strings sent between server and client.  In addition to validating
# them for accuracy, it makes them hard to modify.
#
sub simplecs()
{	my ($str) = @_;
	my @chrs = unpack("W*",$str);
	my $cs = $#chrs+1;
	my $ch;
	my $i=0;
	foreach $ch (@chrs)
	{ 
	  $cs = ((($cs+$ch)*((($i&1)==0)?13:17))) % ((($cs&5)==0)?0x1256235:0x5030322);
	  $i++;
	}
	return($cs);
}
#
# this is cribbed from CGI.pm.
# parse the &par=val&par2=val2 string, and stuff the
# recovered values back into the param data
#
sub parse_params {
    my($tosplit) = @_;
    my(@pairs) = split(/[&;]/,$tosplit);
    my($param,$value);
    my $debug = &debugging();
    for (@pairs) {
	if(!($_ eq ''))
	{
	($param,$value) = split('=',$_,2);
	$param = uri_unescape($param);
	$value = uri_unescape($value);
	param($param,$value);
	if($debug)  { print "($_) set $param = $value\n"; }
	}
   }
}
#
# validate the incoming string, and if valid stuff
# the parameters back into the cgi params.
# There should be 
# len=xxx
# calc=yyy
# followed by all the rest that is checksummed
# 
sub validate()
{
  my ($params) = @_;
  #print "Val $params\n\n";
  my $calc = index($params,"calc=");
  my $calcEnd = index($params,"\n",$calc+4);
  my $checksum = substr($params,$calc+5,$calcEnd-$calc-5);
  my $lenidx = index($params,"len=");
  my $lenend = index($params,"\n",$lenidx+4);
  my $len = substr($params,$lenidx+4,$lenend-$lenidx-4);
  my $rest = substr($params,$calcEnd+1);
  # check the checksum according to the declared version
  my $checkv = index($params,"checksumversion=");
  my $checkve = $checkv>=0 ? index($params,"&",$checkv+16) : -1;
  my $checktype = $checkve>$checkv ? substr($params,$checkv+16,$checkve-$checkv-16) : "";
  if($checktype eq '1') { $rest = $'checksum_salt1 . $rest; }
  elsif($checktype eq '2') { $rest = $'checksum_salt2 . $rest; }
  elsif($checktype eq "") {}
  else { $rest = $'checksum_saltx . $checktype . $rest; }
  my $restlen = length($rest);
  my $cs = &simplecs($rest);
  my $ok = $cs eq $checksum;
  #print "Checksum ($checkv $checkve $checktype) is $checksum\n len is $len\n restlen is $restlen\nrest is $rest\n"; 
  #print "cs $cs $ok\n";
  if($ok) { 
	#__d("params Checksum is $checksum\n len is $len\n restlen is $restlen\nrest is $rest\n$params\n");
	parse_params($rest); 
	}
  else { __d("params invalid Checksum is $checksum\n len is $len\n restlen is $restlen\nrest is $rest\n$params\n"); }
  return $ok;
}

sub removeParams()
{
	my (@all) = &param();
	foreach my $p (@all) { param($p,''); }
	my (@all2) = &param();
	#print "add (@all) -> (@all2)<p>\n";
}

sub useCombinedParams()
{ my ($key,$remove) = @_;
  my $combined = param('params');
  if($remove) { &removeParams(); }

  #print "par\n$combined\n";
  if(!($combined eq ''))
	{ 
	# i'm not sure where the + get turned into spaces in the transportation
	# but it's easy to fix them and hard to figure out why they need to be fixed
	$combined =~ s/ /+/g;
	my $params = &xxtea_decrypt(&decode64($combined),$key);
	#print "dec\n($params)\n";
	 my $valid = &validate($params);
         if(!$valid) { 
		
		my $dbh = &connect($ENV{'REQUEST_URI'});
		my $myaddr = $ENV{'REMOTE_ADDR'};
		&note_failed_login($dbh,$myaddr,"IP: game login parameter validation failed");
		&countEvent("failed login",100,200);
		&disconnect($dbh);
		return(0); 
		}
	}
  return(1);
}
#
# sensitive scripts require that the checksum used is the one specified to the login process
#
sub checkChecksumVersion()
{	if($'checksum_version>0)
	{
	my $vers = param('checksumversion');
	if(!($vers eq $'checksum_version))
		{
		my $from = $ENV{'SCRIPT_NAME'};
		__d("user banned from $from\n");
		&banmenow("$from not following checksum protocol");
		return 0;
		}
	}
	return 1;
}

#
# result is checksumed, encrypted, and base64'd
#
sub printResult()
{
	my ($code,$val) = @_;
	my $msg	= "$code\n$val\n";
	my $cs = &simplecs($msg);
	my $ll = length($msg);
	$msg = "len=$ll\ncalc=$cs\n" . $msg  ;
	my $encr = &xxtea_encrypt($msg,$'tea_key);
	my $base64 = &encode64($encr);
	# encrypted, base64'd
	print $base64;
	if(&debugging())
	{
	my $dec64 = &decode64($base64);
	my $dec = &xxtea_decrypt($dec64,$'tea_key);
	print "\n$dec\n";
	}
}
1
