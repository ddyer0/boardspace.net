#
# include.pl	- Global variables used by game server
#
$CGI::LIST_CONTEXT_WARN = 0;
#
# site-specific paths and passwords live outside the web tree, in www/../secrets/secrets.pl,
# where www is the directory that contains cgi-bin.  On the server: /home/boardspace/secrets/secrets.pl
#
use File::Basename ();
use File::Spec ();
{ my $secrets = File::Spec->catfile(File::Basename::dirname(Cwd::abs_path(__FILE__)),File::Spec->updir,File::Spec->updir,'secrets','secrets.pl');
  my $ok = do $secrets;
  die "can't load $secrets: ".($@ || $!) unless $ok;
}
$features="";
$class_dir = "v9.71a";
$test_class_dir = "vtest";
#$test_class_dir = "v4.99a";
#$applet_permissions='sandbox';
$appjar_version="1";
$applet_permissions='all-permissions';
$test_applet_permissions='all-permissions';
$jdk="new";
$jws_link='/java/boardspace7.jnlp';
$jws_guest_link='/java/boardspace7guest.jnlp';
#$jws_message = "Java web start was broken by some recent java update.  Log in the regular way instead";
$jws_message='';
# caution, just "boardspace.net" seems to go to any of the ips available.
$ip_name = "boardspace.net";
#$ip_name = "50.31.1.13"; 
$game_server_port = 2255;
# developer server on 2256
$test_server_port = 2255;
#$test_server_port = 12257;
# 12345 was configured to use the websockify proxy
# 12346 uses direct access
$cheerpj_game_server_port = 12346;
# 12347 is the proxy port 12357 is the direct wss capable port
$cheerpj_test_server_port = 12357;
$database = "boardspace";
$php_database = "phpbb";
$debug_mysql=0;
#
$retire_months=3;

# the effect of this is that if you make 10 mistakes login in in 1 hour,
# you are banned for 24 hours.  On the other hand, if your're trying to
# guess passwords, after 10 attempts you are banned until you have stopped
# guessing for 24 hours.
$bad_login_count=10;		# tries before bad login gets you banned
$bad_login_interval=60;	# minutes for $bad_login_count attempts
$bad_login_time=60*2;	# minutes you stay banned (2 hours)
#
$language = 'english';
$db_host = "localhost";
$db_user = "root";
$db_type = "mysql";
$db_slow_query_time=3.0;
$db_very_slow_query_time=10.0;
$table    = "players";
$geocode=1; # don't geocode player locations for now
$procmail_error_alert_level=10;
$procmail_error_panic_level=30;
$java_dir = "java";
$java_error_alert_level=3;
$java_error_panic_level=12;

# this makes a table appear on the login page
$top_players_per_row=5;
$top_players_columns=10;
@top_player_variations = 
    ('breakthrough','breakthru','amazons','gomoku','hexade','bug','dameo','slither','bugs','pendulum','matrx', 'circle','epaminondas','manhattan','honeycomb','trench','trike','sprint','meridians','ordo','tumbleweed','crosswordle','viticulture','tamsk','havannah','iro','euphoria','kingscolor','dayandnight','mijnlieff','imagine','stymie','y','jumbulaya','wyps','crosswords','blackdeath','pushfight','kulami','mbrane','blooms','qe','twixt','barca','tintas','magnet','lyngk','hive', 'tammany','sixmaking','morris','stac','chess','checkers','proteus','majorities','carnac',
     'modx','veletas','ponte','morelli','gounki','gyges','zertz','yinsh','dvonn', 'tzaar', 'gipf',
     'go','shogi','xiangqi','tablut','rithmomachy','fanorona',
     'punct','oneday','yspahan','raj','mogul','container',
  'frogs','che', 'micropul','palago','spangles','trax',
'santorini',
    'exxit',
     'gobblet','loa','plateau',
     'hex'
     ,'tumblingdown'
     ,'dipole','truchet','volcano','kuba','qyshinsu','knockabout','warp6','breakingaway','cannon','triad','mutton','octiles','colorito','arimaa','crossfire'
	,'entrapment','quinamid','volo','cookie-disco','syzygy','universe','phlip','diagonal-blocks','khet','takojudo'
	);

$image_dir =  "/players/";


#$cheerpj="https://cjrtnc.leaningtech.com/3.0/cj3loader.js";
#$cheerpj="https://cjrtnc.leaningtech.com/3_20240802_520/cj3loader.js";
$cheerpj="https://cjrtnc.leaningtech.com/3.1/cj3loader.js";
#
# misc parameters
$log_viewer_caption = "BoardSpace Log Viewer";
#mailer info
$supervisor_email= "gamemaster\@boardspace.net";
$announce_email = "boardspace-announcements\@boardspace.net";
$noreply_email = "boardspace-noreply\@boardspace.net";
$from_email = "registrationdesk\@boardspace.net";
$bonus_games=5;
$mobile_login_message = "none";
$enablescroll='true';
$mobile_version_info = "android_version 9.69 ios_version 9.68 igt_version 9.69 test_games xx enable_games xx disable_games xx java_version $class_dir ios_min_version 8.99 android_min_version 8.99 igt_min_version 8.99 android_reject_versions 8.87 ios_reject_versions 8.87 igt_reject_versions 8.87 ";
#
# crypto bits
#
$checksum_version = 0;
$includessl='true';
$claudemodel="claude-sonnet-4-6";



