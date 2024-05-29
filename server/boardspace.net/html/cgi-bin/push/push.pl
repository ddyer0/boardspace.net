use strict;
use LWP::UserAgent;
use Mozilla::CA;
use LWP::Protocol::https ;

use Time::HiRes qw(time usleep);
require "tlib/params.pl";
require "push/webhook.pl";

#
# this uses a discord webhook to public a notification log
# and if the user has supplied their discord uid, arranges
# and @mention of them.
#
$'lastdiscord = 0;
sub send_discord_notification()
{
  my ($discord, $player, $subject, $email) = @_;

 #
  # a little dance to evade discord's rate limits, which seem to 
  # kick in after 3 messages and one message per 0.1 seconds
  #
  my $now = time();
  if($now<$'lastdiscord) 
	{ my $rem = ($'lastdiscord-$now);
          #print "sleep $rem\n";
          usleep($rem*1000); 
        }
  $'lastdiscord = $now+250;

  my $webhook = WebService::Discord::Webhook->new( $'webhook );
  $webhook->get();
  #
  #print "Webhook posting as '" . $webhook->{name} . "' in channel " . $webhook->{channel_id} . "\n";
  #<\@725833023119032460>
  my $user = $discord ? "<\@$discord> ($player) " : "\@$player ";
  my $content = "$subject $email";
  $webhook->execute( content => $user . $content );
  if($discord) { print "discord notification sent<br>"; }
}
#
# https://www.pushbullet.com
# ios android windows chrome firefox
# sample key o.exy4NfAMagNhfLkM5DjdFfVQ5AhpHrQb
# note; something like this ought to work, but this doesn't
#
sub send_pushbullet_notification()
{
  my ($discord, $player, $email) = @_;

  print "pushbullet send $discord ($email)<br>";
  my $header =  HTTP::Headers->new();
  my $response = LWP::UserAgent->new()->post(
    "http://api.pushbullet.com/v2/", 
	[
        "default_header" => "Access-Token: $discord",
	"body" => $email
    ]); 

  if ($response->is_success) {
    	print "pushbullet sent ($email)<br>";  # or whatever
		}
	else {
    	print "pushbullet fail " . $response->status_line . "<br>";
	}
  #my $p = Net::PushBullet->new($discord);
  #$p->push_note(&trans("Boardspace activity"),"tournament activity $email");
}


# https://alertzy.app/
# android or ios
# free 100 per day
# sample key i1vev2eynzh3786
sub send_alertz_notification()
{
  my ($discord, $player, $subject, $email) = @_;
  #print "alertz key $discord<br>title $subject<br>message $email<p>";
  my $response = LWP::UserAgent->new()->post(
    "https://alertzy.app/send", [
    "accountKey" => $discord,
    "title" => $subject,
    "message" => $email,
    "group" => "boardspace"
   ]); 

  if (!$response->is_success)
 	{
	# try http
	$response = LWP::UserAgent->new()->post(
    		"http://alertzy.app/send", [
    		"accountKey" => $discord,
    		"title" => "Boardspace activity",
    		"message" => $email,
    		"group" => "boardspace"
   	]);
	}
   if ($response->is_success) {
    	print "alertz sent<br>";  # or whatever
		}
	else {
    	print "alertz fail " . $response->status_line . "<br>";
	}
}

#
# this handles the notifications we support using a http:push api
# returns true if a personalized discord push was sent.
#
sub send_push_notification()
{	my ($dest,$player_name,$subject,$link) = @_;
	my $discord = 0;
	if (length($dest)>30)
		{ # looks like a pushbullet id
		&send_pushbullet_notification($dest,$player_name,$link);
		}
		elsif (length($dest)==15)
		{
		&send_alertz_notification($dest,$player_name,$subject,$link);
		}
		else
		{	
		#print "discord ($dest)<br>";
		&send_discord_notification($dest,$player_name,$subject, $link);
		$discord = 1;
		}
	return $discord;
}

sub send_notification()
{
   my ($player_name,$subject,$from,$e_mail,$discord,$link,$tmsg) = @_;
   if($player_name)
   {
   print "<br>mail to $player_name\n";
   my $notifyEmail;
   my $notifyDiscord;
   foreach my $dest (split(',',$discord))
	{
	my $ind = index($dest,'@');
	if($ind>0)
		{
		$notifyEmail = 1;
		# theoretically all users have a working email address.  The notifications field
		# can supply an alternate, which can be a real email address or an email gateway
		# for a notification service.  The notifications we know of that work this way
		# are pushover.net w4sb9biuis@pomail.net
		# and pushsafer.com JvEF3MqBGqd8h0TCfv7Y@pushsafer.com
		#
		print "email ..." . substr($dest,$ind) . "<br>";
		#print "<br>send from $from<br> to $dest<br> su $subject<br> body $link<p>";
		&send_mail($from,$dest,$subject,"\n$link\n" );
		}
		else
		{
		$notifyDiscord |= &send_push_notification($dest,$player_name,$subject,$link);
		}
	}
	if($notifyEmail==0)
	{
	#print "email default<br>";
	#print "send from $from<br> to $e_mail<br> su $subject<br> body $tmsg<p>";
	&send_mail($from,$e_mail,$subject,$tmsg );
	}
	if($notifyDiscord==0)
	{
	#print "discord blank<br>";
	&send_push_notification('',$player_name,$link);
	}
   }
}
1


