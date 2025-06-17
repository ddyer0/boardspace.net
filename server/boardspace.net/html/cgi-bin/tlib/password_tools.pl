
use strict;
use Digest::MD5 'md5_hex';

#
# tools to manipulate passwords and password verification
#
my $admin_key = "1357cfasasfvvujew5rgnwetysdfg03256t";
my $admin_salt = "dgwnwp58u5u;ldfnh";
#
# usage : see tournament manager. 
# when you have a valid user name and password, call makeAdminKey to make an
# encrypted, non-forgable token to pass around 
# when you receive a token, call validateAdminKey to extract and approve
# the original contents. Then proceed as though the token had never been
# received; ie if the key, user and password are valid, make a new token
# to be used next time.
# 
# admin key is ip:time:adminkey:user:password:checksum"  
# It's encrypted and passed around in all the forms
# with the timestamp updated. So to be hacked it has
# to both be correctly encrypted and hashed the same
# way as this does it.
#
# admin is an arbitrary string associated with the task
# name is a user name
# pass is a user password
#
# returns the encrypted key 
#
sub makeAdminKey()
{	my ($admin,$name,$pass) = @_;
	my $now = time();
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my $str = "$myaddr:$now:$admin:$name:$pass";
	my $sum = &md5_hex("${admin_salt}$str");
	my $final = "$str:$sum";
	my $enc = &encrypt($final,$admin_key);
	return( $enc );
}
#
# valid admin keys have to match the checksum, and the ip address,
# and be within 10 minutes of the time. returns three values,
# if everything is valid, the admin key, user name, and password
# if the time has expired but otherwise valid, just the admin key and "" ""
# if invalid, 3 empty strings.
#
sub validateAdminKey()
{	my ($fullkey) = @_;
	my $key = &decrypt($fullkey,$admin_key);
	my $myaddr = $ENV{'REMOTE_ADDR'};
	my ($addr,$time,$adminkey,$user,$pass,$vsum) = split(':',$key);
	my $str = "$myaddr:$time:$adminkey:$user:$pass";	
	my $sum = &md5_hex("${admin_salt}$str");
	my $now = time();
	my $timeok = ($time<=$now) && (($time+60*10)>$now);	# 10 minutes
	# ip address and overall hash must match
	if(($sum eq $vsum) && ($myaddr eq $addr))
	{	if($timeok)
			{
			return ($adminkey, $user, $pass);
			}
			else
			{
			 # this will force a new name and password to be provided
			 return($adminkey, "", "");
			}
	}

	return("","","");

}

#generate a rand string, signed by a md5 checksum
sub makeRandomKey()
{	my ($uid) = @_;
	my $r1 = int(rand(0x10000000));
	my $r2 = int(rand(0x10000000));
	my $r3 = int(rand(0x10000000));
	my $r4 = int(rand(0x10000000));
	my $rkey = "$r1$r2$r3$r4";
	# the md5 checksum makes it impossible for anyone who doesn't 
	# know this exact algorithm to make keys that will pass.
	my $mkey = &md5_hex($rkey . "${uid}keyrecord");
	my $key = "$rkey.$mkey";
	my $valid = &validateKey($uid,$key);
	if(!$valid) { die("generated an invalid key"); } 
	return($key);
}

#make a url
sub makeKeyUrl()
{	my ($script,$uid,$key) = @_;
	my $ukey = "?uid=$uid&key=$key"; 
	my $pwlink = "$script$ukey";
	return($pwlink)
}

# check the the alleged key looks like one
sub validateKey()
{	my ($uid,$key) = @_;
	my $mid = index($key,'.');
	my $part1 = substr($key,0,$mid);
	my $part2 = substr($key,$mid+1);
	my $m5 = &md5_hex($part1 . "${uid}keyrecord");
	return($m5 eq $part2);
}

sub registerKey()
{	my ($dbh,$uid,$key) = @_;
	my $quid = $dbh->quote($uid);
	my $qkey = $dbh->quote($key);
	my $qip = $dbh->quote($ENV{'REMOTE_ADDR'});
	my $q = "replace into passwordrecovery set uid=$quid,recoverytoken=$qkey,recoverydate=CURRENT_TIMESTAMP(),ipaddress=$qip";
	&commandQuery($dbh,$q);
}

sub send_password_real
{  my ($dbh,$uid,$pname,$old_email,$status) = @_;
   my $stalink = "";
   if(lc($status) eq 'unconfirmed')
    { my $url = "https://$ENV{'HTTP_HOST'}/cgi-bin/confirm_register.cgi?uid=$uid&pname=$pname";
      $stalink = &trans("Unconfirmed",$url) ;
   }
   my $key = &makeRandomKey($uid);
   my $pwlink = &makeKeyUrl("https://$ENV{'HTTP_HOST'}/cgi-bin/lost_password.cgi",$uid,$key);
   
   &registerKey($dbh,$uid,$key);
   #never expose this, it's a reset link anyone can use
   #print "link <a href='$pwlink'> click </a><p>";
   my $auth = &userAuthenticator($pname);   
   open( SENDMAIL, "| $::sendmail -f $::from_email $old_email" );
   my $eplate = &trans("EmailResetPassword",$stalink,$pname);
   #print "Pass $eplate<p>";
   print SENDMAIL <<Mail_header;
$auth
Sender: $::from_email
From: $::from_email
To: $old_email
$eplate
$pwlink
Mail_header
        
        close SENDMAIL;
}

sub send_password
{  my ($dbh,$uid) = @_;
    if($dbh)
    {
	&readtrans_db($dbh);
    my $quid = $dbh->quote($uid);
    my $sth = &query($dbh,"SELECT player_name,e_mail,status FROM players WHERE uid=$quid");
    if(&numRows($sth))
      { my ($pname,$email,$status) = &nextArrayRow($sth);
        print &trans("SendPassword",$pname,$email);
        print "<br>\n";
        &send_password_real($dbh,$uid,$pname,$email,$status);
      }
    &finishQuery($sth);
    }
}
1
