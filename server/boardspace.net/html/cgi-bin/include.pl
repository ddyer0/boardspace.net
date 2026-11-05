#
# include.pl	- Global variables used by game server
#
$CGI::LIST_CONTEXT_WARN = 0;
$features="";
$class_dir = "v9.65";
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
$ip_name = "future.boardspace.net";
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
$tournament_log = "/home/boardspace/logs/tournament.log";
$perl_log = "/home/boardspace/logs/perl.log";
$translations_dir = "/home/boardspace/www/cgi-bin/tlib/translations/";
$language = 'english';
$db_host = "localhost";
$db_user = "root";
$db_type = "mysql";
$db_slow_query_time=3.0;
$db_very_slow_query_time=10.0;
$table    = "players";
$geocode=1; # don't geocode player locations for now
$debug_log = "/home/boardspace/logs/mysql_debug.log";
$procmail_debug_log = "/home/boardspace/logs/procmail.log";
$procmail_error_alert_level=10;
$procmail_error_panic_level=30;
$webroot = "/home/boardspace/www/";
$java_dir = "java";
$java_error_log = "/home/boardspace/logs/java_error.log";
$java_error_alert_level=3;
$java_error_panic_level=12;
$name_change_log = "/home/boardspace/logs/name-change.log";
$game_completed_log = "/home/boardspace/logs/game-results.log";
$server_logon_log = "/home/boardspace/logs/game-logon.log";
$contact_log = "/home/boardspace/logs/contact.log";
$banner_file = "/home/boardspace/htdocs/banner.txt";

# this makes a table appear on the login page
$top_players_per_row=5;
$top_players_columns=10;
@top_player_variations = 
    ('amazons','gomoku','hexade','bug','dameo','slither','bugs','pendulum','matrx', 'circle','epaminondas','manhattan','honeycomb','trench','trike','sprint','meridians','ordo','tumbleweed','crosswordle','viticulture','tamsk','havannah','iro','euphoria','kingscolor','dayandnight','mijnlieff','imagine','stymie','y','jumbulaya','wyps','crosswords','blackdeath','pushfight','kulami','mbrane','blooms','qe','twixt','barca','tintas','magnet','lyngk','hive', 'tammany','sixmaking','morris','stac','chess','checkers','proteus','majorities','carnac',
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
$server_start="/home/boardspace/bin/BoardSpaceServer.start";
$test_server_start="/home/boardspace/bin/TestServer.start";
$www_root="/home/boardspace/www";


#$cheerpj="https://cjrtnc.leaningtech.com/3.0/cj3loader.js";
#$cheerpj="https://cjrtnc.leaningtech.com/3_20240802_520/cj3loader.js";
$cheerpj="https://cjrtnc.leaningtech.com/3.1/cj3loader.js";
#
# misc parameters
$log_viewer_caption = "BoardSpace Log Viewer";
$log_viewer_dir = "/home/boardspace/logs/,/home/boardspace/wlogs/,/home/boardspace/wlogs/../4321/";
$temp_dir = "/home/boardspace/temp/";
#mailer info
$sendmail = "/usr/sbin/sendmail";
$supervisor_email= "gamemaster\@boardspace.net";
$announce_email = "boardspace-announcements\@boardspace.net";
$noreply_email = "boardspace-noreply\@boardspace.net";
$from_email = "registrationdesk\@boardspace.net";
$bonus_games=5;
$mobile_login_message = "none";
$enablescroll='true';
$mobile_version_info = "android_version 9.63 ios_version 9.63 igt_version 9.63 test_games xx enable_games xx disable_games xx java_version $class_dir ios_min_version 8.99 android_min_version 8.99 igt_min_version 8.99 android_reject_versions 8.87 ios_reject_versions 8.87 igt_reject_versions 8.87 ";
#
# crypto bits
#
$claudemodel="claude-sonnet-4-6";
$db_password = "xxxxx";
$stealth_password = "xxxxx";
$tournament_password = "xxxxx";
$sendmessage_password="xxxxx";
$proposal_password="xxxxx";
$changenews_password="xxxxx";
$log_viewer_password = "xxxxx";
$tea_key="xxxxxxxxxx";
$claudeapi="xxxxxxxxx";
$checksum_salt2 = 'some salt for version 2';
$checksum_version = 0;
$checksum_salt1 = some salt for version 1';
$checksum_saltx = 'the rest of them';
$webhook = "https://discord.com/xxxxx";
$turnhook = "https://discord.com/api/xxxx";

