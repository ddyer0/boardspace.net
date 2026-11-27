#
# secrets.pl -- site-specific paths and passwords for the boardspace cgi scripts (this machine).
# loaded by include.pl from www/../secrets/secrets.pl.  Keep it outside the apache
# document root, readable only by the user the cgi scripts run as, and out of CVS/git.
#
# paths
$www_root = "g:\\share\\projects\\boardspace-html\\htdocs\\";
$webroot = "g:/share/projects/boardspace-html/htdocs/";
$translations_dir = "g:/share/projects/boardspace-html/htdocs/cgi-bin/tlib/translations/";
$temp_dir = "g:/share/projects/boardspace.net/temp/";
$banner_file = "g:/share/projects/boardspace-html/htdocs/banner.txt";
$server_start="g:/share/projects/tantrix.com/bin/bin/tantrixServer.bat";
$test_server_start = "g:/share/projects/boardspace.net/bin/TestServer.start";
$sendmail = "/usr/sbin/sendmail";
$tournament_log = "g:/share/projects/boardspace.net/logs/tournament.log";
$perl_log = "g:/share/projects/boardspace.net/logs/perl.log";
$debug_log = "g:/share/projects/boardspace.net/logs/mysql_debug.log";
$procmail_debug_log = "g:/share/projects/boardspace.net/logs/procmail.log";
$java_error_log = "g:/share/projects/boardspace.net/logs/java_error.log";
$name_change_log = "g:/share/projects/boardspace.net/logs/name.change";
$game_completed_log = "g:/share/projects/boardspace.net/logs/game.results";
$server_logon_log = "g:/share/projects/boardspace.net/logs/game.logon";
$contact_log = "g:/share/projects/boardspace.net/logs/contact.log";
$log_viewer_dir='g:/share/projects/boardspace.net/logs/';
#
# passwords and keys
$db_password = "xxxxxxxxxxxxxxxxxx";
$stealth_password = "xxxxxxxxxxxxxxxxxxx";
$tournament_password = "xxxxxxxxxxxxx";
$sendmessage_password="xxxxxxxxxxxxxxxxxx";
$proposal_password="xxxxxxxxxxxxxxx";
$changenews_password="xxxxxxxxxxxxxxxx";
$log_viewer_password='xxxxxxxxxxxxxxxxx';
$tea_key = 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';
$checksum_salt1 = 'xxxxxxxxxxxxxxxxxxxxxxxxxxx';
$checksum_salt2 = 'xxxxxxxxxxxxxxxxxxxxxxx';
$checksum_saltx = 'xxxxxxxxxxxxxxxxxxxx';
$claudeapi= "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
$webhook = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
$bgg_token="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
$turnhook = $webhook;
$jws_key = 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx';
$file_upload_secret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
1;
