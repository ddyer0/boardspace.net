use strict;
use LWP::UserAgent;
use Mozilla::CA;
use LWP::Protocol::https ;


require "tlib/params.pl";
require "push/webhook.pl";

#
# this uses a discord webhook to public a notification log
# and if the user has supplied their discord uid, arranges
# and @mention of them.
#
sub send_discord_notification()
{
  my ($discord, $player, $subject, $email) = @_;
  #   
  # silent notifications use the hook for the turn based notification channel
  my $webhook = WebService::Discord::Webhook->new($'turnhook );
  $webhook->get();
  #
  #print "Webhook posting as '" . $webhook->{name} . "' in channel " . $webhook->{channel_id} . "\n";
  #<\@725833023119032460>
  my $user = $discord ? "<\@$discord> ($player) " : "\@$player ";
  my $content = "$subject $email";
  $webhook->execute( content => $user . $content );
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

  my $header =  HTTP::Headers->new();
  my $response = LWP::UserAgent->new()->post(
    "http://api.pushbullet.com/v2/", 
	[
        "default_header" => "Access-Token: $discord",
	"body" => $email
    ]); 

  return $response->is_success;
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
   if (!$response->is_success)
	{
    	$'error = "alertz fail " . $response->status_line . "<br>";
	}
}

#
# this handles the notifications we support using a http:push api
# returns true if a personalized discord push was sent.
#
sub send_push_notification()
{	my ($dest,$player_name,$subject,$link) = @_;
	my $discord = 0;
	#print "push d=$dest p=$player_name s=$subject l=$link\n";
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

sub send_silent_notification()
{
   my ($player_name,$subject,$from,$e_mail,$discord,$link,$tmsg) = @_;
   my $notifyEmail;
   my $notifyDiscord;
   #print "silent p=$player_name s=$subject f=$from t=$e_mail l=$link m=$tmsg\n";
   if(!$tmsg) { $tmsg = $link; }
   elsif(!$link) { $link = $tmsg; }
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
		#print "<br>send from $from<br> to $dest<br> su $subject<br> body $link<p>";
		&send_mail_to($player_name,$from,$dest,$subject,"\n$link\n" );
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
	&send_mail_to($player_name,$from,$e_mail,$subject,$tmsg );
	}
	if($notifyDiscord==0)
	{
	#print "discord blank<br>";
	&send_push_notification('',$player_name,$subject,$link);
	}
}

1

