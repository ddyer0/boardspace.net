#
# include.pl	- Global variables used by game server
#
$features="";
$class_dir = "v5.98";
$test_class_dir = "test";
#$test_class_dir = "v4.99a";
#$applet_permissions='sandbox';
$applet_permissions='all-permissions';
$test_applet_permissions='all-permissions';
$jdk="new";
$jws_link='/java/boardspace7.jnlp';
$jws_guest_link='/java/boardspace7guest.jnlp';
#$jws_message = "Java web start was broken by some recent java update.  Log in the regular way instead";
$jws_message='';
# caution, just "boardspace.net" seems to go to any of the ips available.
#$ip_name = "67.202.75.9"; # this is the official ip
$ip_name = "boardspace.net";
#$ip_name = "50.31.1.13";
$game_server_port = 2255;
# developer server on 2256
$test_server_port = 12257;
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
$tournament_log = "/home/boardspa/logs/tournament.log";
$perl_log = "/home/boardspa/logs/perl.log";
$translations_dir = "/home/boardspa/www/cgi-bin/tlib/translations/";
$language = 'english';
$db_host = "localhost";
$db_user = "root";
$db_type = "mysql";
$db_slow_query_time=3.0;
$db_very_slow_query_time=10.0;
$table    = "players";
$geocode=1; # don't geocode player locations for now
$debug_log = "/home/boardspa/logs/mysql_debug.log";
$procmail_debug_log = "/home/boardspa/logs/procmail.log";
$procmail_error_alert_level=10;
$procmail_error_panic_level=30;
$webroot = "/home/boardspa/www/";
$java_dir = "java";
$java_error_log = "/home/boardspa/logs/java_error.log";
$java_error_alert_level=3;
$java_error_panic_level=12;
$name_change_log = "/home/boardspa/logs/name-change.log";
$game_completed_log = "/home/boardspa/logs/game-results.log";
$server_logon_log = "/home/boardspa/logs/game-logon.log";
$contact_log = "/home/boardspa/logs/contact.log";
$banner_file = "/home/boardspa/htdocs/banner.txt";

# this makes a table appear on the login page
$top_players_per_row=5;
$top_players_columns=10;
@top_player_variations = 
    ('viticulture','kingscolor','dayandnight','mijnlieff','imagine','stymie','y','jumbulaya','wyps','crosswords','blackdeath','pushfight','kulami','mbrane','blooms','qe','twixt','barca','tintas','magnet','lyngk','euphoria','hive', 'tammany','sixmaking','morris','stac','chess','checkers','proteus','majorities','carnac',
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
$server_start="/home/boardspa/bin/BoardSpaceServer.start";
$test_server_start="/home/boardspa/bin/TestServer.start";
$www_root="/home/boardspa/www";

#
# misc parameters
$log_viewer_caption = "BoardSpace Log Viewer";

$log_viewer_dir = "/home/boardspa/logs/,/home/boardspa/wlogs/,/home/boardspa/wlogs/../4321/";
$temp_dir = "/home/boardspa/temp/";
#mailer info
$sendmail = "/usr/sbin/sendmail";
$supervisor_email= "gamemaster\@boardspace.net";
$announce_email = "boardspace-announcements\@boardspace.net";
$noreply_email = "boardspace-noreply\@boardspace.net";
$from_email = "registrationdesk\@boardspace.net";
$bonus_games=5;
$mobile_login_message = "none";
$mobile_version_info = "test_games xx enable_games xx disable_games xx java_version $class_dir ios_min_version 5.40 android_min_version 5.40 android_version 5.98 ios_version 5.98 android_reject_versions 0 ios_reject_versions 0 ";

$db_password = "xxxxx";
$stealth_password = "xxxxx";
$tournament_password = "xxxxx";
$sendmessage_password="xxxxx";
$proposal_password="xxxxx";
$changenews_password="xxxxx";
$log_viewer_password = "xxxxx";
$tea_key="xxxxxxxxxx";
$checksum_salt2 = 'some salt for version 2';
$checksum_version = 0;
$checksum_salt1 = some salt for version 1';
$checksum_saltx = 'the rest of them';

