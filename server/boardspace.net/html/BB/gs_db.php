<?php
/**
 * gs_db.php
 * PHP translation of gs_db.pl - common database utilities
 */

// ---------------------------------------------------------------------------
// Global state (replaces Perl package globals $::xxx)
// ---------------------------------------------------------------------------
$GLOBALS['parent_script']               = 'gs_db.php';
$GLOBALS['standard_database_connection'] = null;
$GLOBALS['single_ip_ip']                = null;
$GLOBALS['single_ip_uid']               = 0;
$GLOBALS['single_ip_query_time']        = 0.0;
$GLOBALS['long_time_multiplier']        = 5;
$GLOBALS['long_time_ban_threshold']     = 120;
$GLOBALS['single_ip_penalty_time']      = 0;
$GLOBALS['single_ip_penalty_string']    = '';
$GLOBALS['single_ip_penalty_threshold'] = 10;
$GLOBALS['single_ip_start_time']        = 0;

// Cache for game name/code lookups
$GLOBALS['gamename_to_code']    = [];
$GLOBALS['gamecode_to_name']    = [];
$GLOBALS['gamecode_to_viewer']  = [];
$GLOBALS['gamename_to_family']  = [];

// Query tracking (replaces %'lastquery / %'lastdbh)
$GLOBALS['lastquery'] = [];
$GLOBALS['lastdbh']   = [];

// ---------------------------------------------------------------------------
// Logging / debug helpers
// ---------------------------------------------------------------------------

function showForm(): void
{
    $params = $_REQUEST;
    error_log("All parameter names: " . implode(', ', array_keys($params)));
    error_log("Details:");
    foreach ($params as $par => $val) {
        if ($par === 'password' || $par === 'password2') { $val = '<xxxx>'; }
        error_log("$par\t$val");
    }
}

function printForm(): void
{
    $params = $_REQUEST;
    echo "All parameter names: " . implode(', ', array_keys($params)) . "<br>\n";
    echo "Details:<br>\n";
    foreach ($params as $par => $val) {
        if ($par === 'password') { $val = '<xxxx>'; }
        echo "$par\t$val<br>\n";
    }
}

function logError(string $context, string $com): void
{
    $addr = $_SERVER['REMOTE_ADDR']    ?? '';
    $uri  = $_SERVER['REQUEST_URI']    ?? '';
    $e    = "($addr $uri : $context)";
    error_log("Error $com:\n ->$e");

    if (!empty($GLOBALS['debug_mysql'])) {
        echo "Error $com:<br> $e<p>\n";
    } else {
        echo "Sorry, There was a problem processing your request<p>\n";
    }
}

function logSilent(string $context, string $com): void
{
    $addr = $_SERVER['REMOTE_ADDR'] ?? '';
    $uri  = $_SERVER['REQUEST_URI'] ?? '';
    $e    = "($addr $uri : $context)";
    error_log("$com:\n ->$e");
}

function logMessage(string $context, string $com): void
{
    $addr = $_SERVER['REMOTE_ADDR'] ?? '';
    $uri  = $_SERVER['REQUEST_URI'] ?? '';
    $e    = "($addr $uri : $context)";
    error_log("$com:\n ->$e");
    showForm();
}

function logForm(string $context, string $com): void
{
    $addr = $_SERVER['REMOTE_ADDR'] ?? '';
    $uri  = $_SERVER['REQUEST_URI'] ?? '';
    $e    = "($addr $uri : $context)";
    error_log("$com:\n ->$e");
    showForm();
}

// ---------------------------------------------------------------------------
// Parameter / input validation
// ---------------------------------------------------------------------------

/**
 * Penalise a connection if a parameter value exceeds the expected length.
 * Excess length is accumulated; if it breaches single_ip_penalty_threshold
 * the connection will be auto-banned on disconnect().
 */
function bless_parameter_length(string $str, int $xlen): void
{
    $len = strlen($str);
    $dif = $len - $xlen * 2;
    if ($dif > 0) {
        $GLOBALS['single_ip_penalty_time'] += $dif;
        $GLOBALS['single_ip_ip'] = ip_to_int($_SERVER['REMOTE_ADDR'] ?? '');
        $ps = $GLOBALS['single_ip_penalty_string'];
        $GLOBALS['single_ip_penalty_string'] = ($ps === '') ? $str : "$ps\n$str";
    }
}

// ---------------------------------------------------------------------------
// Database connection
// ---------------------------------------------------------------------------

/**
 * Connect to the database and return a PDO handle.
 */
function gs_connect(string $script = ''): ?PDO
{
    $db_type  = $GLOBALS['db_type']     ?? 'mysql';
    $database = $GLOBALS['database']    ?? '';
    $db_host  = $GLOBALS['db_host']     ?? 'localhost';
    $db_user  = $GLOBALS['db_user']     ?? '';
    $db_pass  = $GLOBALS['db_password'] ?? '';

    if ($script !== '') {
        $GLOBALS['parent_script'] = $script;
    }

    try {
        $dsn = "$db_type:dbname=$database;host=$db_host";
        $dbh = new PDO($dsn, $db_user, $db_pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        ]);
    } catch (PDOException $ex) {
        logError("Trying to connect", "db: {$database}@{$db_host} user:{$db_user} " . $ex->getMessage());
        return null;
    }

    $GLOBALS['standard_database_connection'] = $dbh;
    return $dbh;
}

// ---------------------------------------------------------------------------
// Date helper
// ---------------------------------------------------------------------------

function datestring(): string
{
    return date('j/n/Y:');
}

// ---------------------------------------------------------------------------
// Query helpers  (thin wrappers that mirror the Perl originals)
// ---------------------------------------------------------------------------

/**
 * Safe quote wrapper — returns '' if value is empty.
 */
function gs_quote(PDO $dbh, $item): string
{
    if ($dbh && $item !== null && $item !== '') {
        return $dbh->quote($item);
    }
    return "''";
}

/**
 * Prepare and execute a SELECT; returns a PDOStatement or null on error.
 */
function gs_query(PDO $dbh, string $com): ?PDOStatement
{
    $start = microtime(true);

    try {
        $sth = $dbh->prepare($com);
        $sth->execute();
    } catch (PDOException $ex) {
        logError("execute", $com);
        if (empty($GLOBALS['debug_mysql'])) {
            $prev = $GLOBALS['debug_mysql'];
            $GLOBALS['debug_mysql'] = 1;    // prevent recursive errors
            note_failed_login($dbh, $_SERVER['REMOTE_ADDR'] ?? '', "IP: failed query - see log");
            $GLOBALS['debug_mysql'] = $prev;
        }
        return null;
    }

    $elapsed = microtime(true) - $start;
    $GLOBALS['single_ip_query_time'] += $elapsed;

    $slow      = $GLOBALS['db_slow_query_time']      ?? 2.0;
    $very_slow = $GLOBALS['db_very_slow_query_time'] ?? 5.0;

    if ($elapsed > $slow) {
        $msg = "slow query: {$elapsed} seconds: $com";
        if ($elapsed > $very_slow) {
            logMessage("slow query", $msg);
        } else {
            logForm("slow query", $msg);
        }
    }

    // Track open queries (keyed by object id)
    $id = spl_object_id($sth);
    $GLOBALS['lastquery'][$id] = $com;
    $GLOBALS['lastdbh'][$id]   = $dbh;

    return $sth;
}

/**
 * Row count for a statement handle.
 */
function numRows(?PDOStatement $sth): int
{
    return $sth ? $sth->rowCount() : 0;
}

/**
 * Fetch the next row as a plain array.
 */
function nextArrayRow(?PDOStatement $sth): array
{
    if (!$sth) {
        logError("nextArrayRow on null", "");
        return [];
    }
    $row = $sth->fetch(PDO::FETCH_NUM);
    if ($sth->errorCode() !== '00000') {
        logError("in nextArrayRow", $sth->errorInfo()[2] ?? '');
        return [];
    }
    return $row ?: [];
}

/**
 * Finish / free a statement handle.
 */
function finishQuery(?PDOStatement $sth): ?PDOStatement
{
    if ($sth) {
        $id = spl_object_id($sth);
        $GLOBALS['lastquery'][$id] = null;
        $GLOBALS['lastdbh'][$id]   = null;
        $sth->closeCursor();
    }
    return $sth;
}

/**
 * Return the last auto-increment id from a statement.
 */
function last_insert_id(?PDOStatement $sth): int
{
    // In PDO the insert id lives on the connection, not the statement.
    // Callers should use $dbh->lastInsertId() directly; this shim is provided
    // for compatibility.
    return 0;
}

/**
 * Execute an UPDATE/INSERT/DELETE with no result set.
 */
function commandQuery(PDO $dbh, string $com): ?PDOStatement
{
    $sth = gs_query($dbh, $com);
    return finishQuery($sth);
}

// ---------------------------------------------------------------------------
// Disconnect — applies accumulated penalties before closing
// ---------------------------------------------------------------------------

function gs_disconnect(?PDO $dbh): void
{
    if (!$dbh) { return; }

    if ($dbh === $GLOBALS['standard_database_connection']) {

        // ---- Penalty for bad input parameters ----
        if ($GLOBALS['single_ip_penalty_time'] > $GLOBALS['single_ip_penalty_threshold']) {
            $script = $_SERVER['SCRIPT_NAME'] ?? '';
            $ps     = $GLOBALS['single_ip_penalty_string'];
            $qc     = $dbh->quote("bad input {$script}: $ps");
            $da     = $dbh->quote(datestring());
            $comm   = ($ps !== '') ? ",comment=concat(comment,'\n',$da,$qc)" : "";

            if ($GLOBALS['single_ip_uid']) {
                $quid = $GLOBALS['single_ip_uid'];
                $q = "UPDATE ipinfo SET status='autobanned',rejectcount=rejectcount+1$comm WHERE uid=$quid";
                commandQuery($dbh, $q);
            } elseif ($GLOBALS['single_ip_ip']) {
                $qip = $dbh->quote($GLOBALS['single_ip_ip']);
                $q = "INSERT INTO ipinfo SET status='autobanned',min=$qip,max=$qip,comment=$qc"
                   . " ON DUPLICATE KEY UPDATE status='autobanned',rejectcount=rejectcount+1$comm";
                commandQuery($dbh, $q);
            }
        }

        // ---- Penalty for excess query time ----
        if (($GLOBALS['single_ip_ip'] != 0) && ($GLOBALS['single_ip_query_time'] > 1.0)) {
            $itime   = (int)$GLOBALS['single_ip_query_time'];
            $ltime   = $itime * $GLOBALS['long_time_multiplier'];
            $newtime = "short_time=UNIX_TIMESTAMP()+$itime,"
                     . "long_time=GREATEST(long_time+$ltime,UNIX_TIMESTAMP()+$ltime)";
            $script  = $_SERVER['SCRIPT_NAME'] ?? '';
            $qtime   = $dbh->quote("excess time={$itime} from {$script}");
            $da      = $dbh->quote(datestring());

            if ($GLOBALS['single_ip_uid']) {
                $quid = $GLOBALS['single_ip_uid'];
                commandQuery($dbh,
                    "UPDATE ipinfo SET $newtime,"
                    . "comment=concat(comment,'\n',$da,$qtime,'(',long_time-UNIX_TIMESTAMP(),')')"
                    . " WHERE uid=$quid"
                );
            } else {
                $qip = $dbh->quote($GLOBALS['single_ip_ip']);
                commandQuery($dbh,
                    "INSERT INTO ipinfo SET status='auto',min=$qip,comment=$qtime,max=$qip,"
                    . "short_time=UNIX_TIMESTAMP()+$itime,long_time=UNIX_TIMESTAMP()+$ltime"
                    . " ON DUPLICATE KEY UPDATE $newtime,"
                    . "comment=concat(comment,'\n',$da,$qtime,'(',long_time-UNIX_TIMESTAMP(),')')"
                );
            }
        }

        $GLOBALS['standard_database_connection'] = null;
    }

    // Warn about any queries still open against this connection
    foreach ($GLOBALS['lastquery'] as $id => $key) {
        if ($key && isset($GLOBALS['lastdbh'][$id]) && $GLOBALS['lastdbh'][$id] === $dbh) {
            logError("query not finished at disconnect:", $key);
        }
    }

    $dbh = null; // PDO closes on last reference
}

// ---------------------------------------------------------------------------
// Login / registration checks
// ---------------------------------------------------------------------------

function check_logon(string $pname, string $passwd): bool
{
    $dbh = gs_connect("check_logon");
    if (!$dbh) { return false; }

    $slashpname = $dbh->quote($pname);
    $slashpwd   = $dbh->quote($passwd);
    $sth = gs_query($dbh,
        "SELECT num_logon FROM players "
        . "WHERE player_name=$slashpname AND pshash=MD5(concat($slashpwd,uid))"
    );
    $n = numRows($sth);
    finishQuery($sth);
    gs_disconnect($dbh);
    return ($n === 1);
}

// ---------------------------------------------------------------------------
// IP address utilities
// ---------------------------------------------------------------------------

/**
 * Convert a dotted IPv4 address (or large integer string) to an integer.
 * Non-dotted "fake" addresses longer than 4 chars are passed through as-is.
 */
function ip_to_int(string $in): int|string
{
    if (!$in) { $in = "0"; }

    $parts = explode('.', $in);
    [$s1, $s2, $s3, $s4] = array_pad($parts, 4, null);

    if (!$s2 && !$s3 && !$s4) {
        // Fake/non-dotted IP — pass through if it looks big enough to be real
        return (strlen($in) > 4) ? $in : 0;
    }

    return ((int)$s1 << 24) | ((int)$s2 << 16) | ((int)$s3 << 8) | (int)$s4;
}

/**
 * Convert an integer back to a dotted IPv4 string.
 */
function int_to_ip(int $in): string
{
    $s4 = $in & 0xff;  $in >>= 8;
    $s3 = $in & 0xff;  $in >>= 8;
    $s2 = $in & 0xff;
    $s1 = $in >> 8;
    return "$s1.$s2.$s3.$s4";
}

// ---------------------------------------------------------------------------
// IP access / ban logic
// ---------------------------------------------------------------------------

/**
 * Return the ipinfo uid (>0) if access is allowed, 1 if no record exists (ok),
 * or -1 if the IP is banned.
 */
function allow_ip_access(PDO $dbh, string $ip): int
{
    $intip = ip_to_int($ip);
    if (!$intip) { return 0; }  // no IP specified

    $loginok = true;
    $ii      = $dbh->quote($intip);
    $bantime = ($GLOBALS['bad_login_time'] ?? 60) * 60;
    $range   = "($ii>=min AND $ii<=max)";
    $query   = "SELECT status, uid,"
             . " if((UNIX_TIMESTAMP(changed)+$bantime)>UNIX_TIMESTAMP(),1,0),"
             . " min, max, UNIX_TIMESTAMP(changed), UNIX_TIMESTAMP(),"
             . " short_time, long_time"
             . " FROM ipinfo WHERE $range";

    $sth     = gs_query($dbh, $query);
    $numrows = numRows($sth);
    $gooduid = ($numrows > 0) ? 0 : 1;  // no record = ok

    if (strpos($ip, '.') !== false) {
        $GLOBALS['single_ip_ip'] = $intip;
    }

    while ($numrows > 0) {
        $numrows--;
        [$st, $uid, $changed, $min, $max, $changedtime, $now, $short_time, $long_time]
            = nextArrayRow($sth);

        if ((($st === 'autobanned') && $changed) || ($st === 'banned')) {
            $dt = $now - $changedtime;
            if ($dt > 60) {
                // Only update once per minute to avoid floods
                $uu = $dbh->quote($uid);
                commandQuery($dbh, "UPDATE ipinfo SET rejectcount=rejectcount+1 WHERE $uu=uid");
            } else {
                $GLOBALS['single_ip_penalty_string'] = '';
            }
            $loginok = false;

        } elseif ($min == $max) {
            if ($st === 'autobanned' || $st === 'auto') {
                $gooduid = $uid;
            }

            $penalty      = $short_time - $now;
            $long_penalty = $long_time  - $now;
            $da           = $dbh->quote(datestring());

            if ($long_penalty > $GLOBALS['long_time_ban_threshold']) {
                $bancom  = $dbh->quote("excess long penalty=$long_penalty");
                $quid    = $dbh->quote($uid);
                commandQuery($dbh,
                    "UPDATE ipinfo SET status='autobanned',long_time=0,short_time=0,"
                    . "comment=concat(comment,'\n',$da,$bancom) WHERE uid=$quid"
                );
                $gooduid = 0;
            }

            $GLOBALS['single_ip_uid'] = $uid;

            if ($penalty > 0) {
                // short_time is the earliest the IP may use the database again
                sleep((int)$penalty);
                $GLOBALS['single_ip_start_time'] = $now + $penalty;
            }
        }
    }

    finishQuery($sth);
    return $loginok ? (int)$gooduid : -1;
}

/**
 * Return true if the IP is allowed to log in.
 * A successful check clears any watch-list entry for the IP.
 */
function allow_ip_login(PDO $dbh, string $ip): bool
{
    $loginok = allow_ip_access($dbh, $ip);
    if ($loginok > 0) {
        $good = $dbh->quote($loginok);
        commandQuery($dbh, "DELETE FROM ipinfo WHERE $good=uid");
    }
    return ($loginok >= 0);
}

/**
 * Return true if the IP is allowed to register.
 */
function allow_ip_register(PDO $dbh, string $ip): bool
{
    $intip = ip_to_int($ip);
    if (!$intip) { return true; }

    $bantime = ($GLOBALS['bad_login_time'] ?? 60) * 60;
    $cond    = "( ((status='autobanned') AND ((UNIX_TIMESTAMP(changed)+$bantime)>UNIX_TIMESTAMP()))"
             . " OR (status='banned') OR (status='noregister') )";
    $ii      = $dbh->quote($intip);
    $range   = "($ii>=min AND $ii<=max)";
    $query   = "SELECT status, uid FROM ipinfo WHERE $range AND $cond";

    $sth     = gs_query($dbh, $query);
    $numrows = numRows($sth);
    $loginok = true;

    while ($numrows > 0) {
        $numrows--;
        [$st, $uid] = nextArrayRow($sth);
        commandQuery($dbh, "UPDATE ipinfo SET rejectcount=rejectcount+1 WHERE uid='$uid'");
        $loginok = false;
    }

    finishQuery($sth);
    return $loginok;
}

/**
 * Return the name of any banned user sharing this uid's email address, or ''.
 */
function banned_from_registering(PDO $dbh, int $uid, int $ident): string
{
    $quid = $dbh->quote($uid);
    $sth  = gs_query($dbh, "SELECT e_mail FROM players WHERE uid=$quid");
    $val  = '';

    if (numRows($sth) > 0) {
        [$email] = nextArrayRow($sth);
        $qemail     = $dbh->quote($email);
        $qident     = $dbh->quote($ident);
        $uidclause  = ($ident == 0) ? "" : " OR identity=$qident";
        $sth2 = gs_query($dbh,
            "SELECT player_name FROM players WHERE status='banned' AND (e_mail=$qemail $uidclause)"
        );
        if (numRows($sth2) > 0) {
            [$val] = nextArrayRow($sth2);
        }
        finishQuery($sth2);
    }

    finishQuery($sth);
    return (string)$val;
}

/**
 * Note a failed login attempt; auto-ban after too many failures.
 */
function note_failed_login(PDO $dbh, string $ip, string $info): void
{
    $intip = ip_to_int($ip);
    if (!$intip) { return; }

    $ii = $dbh->quote($intip);
    $exquery = "SELECT status, uid, unix_timestamp(changed), badlogincount, unix_timestamp(), comment"
             . " FROM ipinfo"
             . " WHERE (min=$ii AND max=min) AND (status='auto' OR status='autobanned')";
    $sth = gs_query($dbh, $exquery);
    $nr  = numRows($sth);

    $query = "UPDATE ipinfo SET badlogincount=badlogincount+1, logincount=GREATEST(0,logincount-1)"
           . " WHERE ($ii>=min AND $ii<=max)";
    commandQuery($dbh, $query);

    if ($nr == 0) {
        $da   = datestring();
        $comm = $dbh->quote("$da$info");
        $q = "INSERT INTO ipinfo SET status='auto',min=$ii,max=$ii,badlogincount=1,comment=$comm"
           . " ON DUPLICATE KEY UPDATE badlogincount=badlogincount+1,comment=concat(comment,'\n',$comm)";
        commandQuery($dbh, $q);

        // Clean up temporary data more than a month old
        $cleanup = "DELETE FROM ipinfo"
                 . " WHERE ((UNIX_TIMESTAMP(changed)+60*60*24*30)<UNIX_TIMESTAMP())"
                 . "   AND (long_time < UNIX_TIMESTAMP())"
                 . "   AND ((status='auto') OR (status='autobanned') OR (status='wasbanned'))";
        commandQuery($dbh, $cleanup);
    } else {
        [$status, $uid, $changed, $badlogin, $now, $oldcom] = nextArrayRow($sth);
        $da   = datestring();
        $comm = $dbh->quote("$da$info");

        $bad_login_interval = $GLOBALS['bad_login_interval'] ?? 60;
        if (($now - 60 * $bad_login_interval) > $changed) {
            // Old news — start a new sequence
            if (strtolower($status) === 'autobanned') {
                commandQuery($dbh, "UPDATE ipinfo SET status='wasbanned',changed=changed WHERE uid=$uid");
            } else {
                commandQuery($dbh, "DELETE FROM ipinfo WHERE uid=$uid");
            }
        } else {
            $newcom = "$oldcom\n$da$info";
            if (strlen($newcom) > 500) {
                $newcom = "..." . substr($newcom, strpos($newcom, "\n", 10));
            }
            $newcom = $dbh->quote($newcom);
            commandQuery($dbh, "UPDATE ipinfo SET comment=$newcom WHERE uid=$uid");

            $bad_login_count = $GLOBALS['bad_login_count'] ?? 5;
            if (($badlogin + 1) >= $bad_login_count && strtolower($status) === 'auto') {
                commandQuery($dbh, "UPDATE ipinfo SET status='autobanned' WHERE uid=$uid");
                $qmsg = $dbh->quote("auto-banned ip=$ip for excess failed logins: $info");
                commandQuery($dbh, "INSERT INTO messages SET type='alert',message=$qmsg");
            }
        }
    }

    finishQuery($sth);
}

// ---------------------------------------------------------------------------
// Banning helpers / honeypot
// ---------------------------------------------------------------------------

function banmenow(string $from): void
{
    $dbh = gs_connect();
    if ($dbh) {
        $ip = $_SERVER['REMOTE_ADDR'] ?? '';
        banme($dbh, $ip, $from);
        gs_disconnect($dbh);
    }
}

function banme(PDO $dbh, string $ip, string $from, string $also = ''): void
{
    $qip   = $dbh->quote(ip_to_int($ip));
    $valid = md5("{$from}honey");
    $da    = datestring();
    $match = ($valid === $also) ? "robot" : "fuzzing";
    $msg2  = "$da $match alarm from $from";
    $qmsg2 = $dbh->quote($msg2);

    $q = "INSERT INTO ipinfo SET status='autobanned',rejectcount=1,min=$qip,max=$qip,comment=$qmsg2"
       . " ON DUPLICATE KEY UPDATE badlogincount=badlogincount+1,rejectcount=rejectcount+1,"
       . "status='autobanned',comment=concat(comment,'\n',$qmsg2)";
    commandQuery($dbh, $q);

    $q2  = "SELECT uid, comment FROM ipinfo WHERE min=$qip AND max=$qip";
    $sth = gs_query($dbh, $q2);
    [$uid, $comm] = nextArrayRow($sth);

    if (strlen((string)$comm) > 500) {
        $comm  = "..." . substr($comm, strpos($comm, "\n", 10));
        $quid  = $dbh->quote($uid);
        $qcomm = $dbh->quote($comm);
        commandQuery($dbh, "UPDATE ipinfo SET comment=$qcomm WHERE uid=$quid");
    }
    finishQuery($sth);
}

/**
 * Emit an invisible honeypot anchor. Any bot that follows it gets auto-banned.
 */
function honeypot(string $from = '', string $text = ''): void
{
    if (!$from) { $from = $_SERVER['SCRIPT_NAME'] ?? ''; }
    $from = htmlspecialchars($from, ENT_QUOTES, 'UTF-8');
    $sum  = md5("{$from}honey");
    echo "<a href='/cgi-bin/noticeme.cgi?data=$from&also=$sum'>$text</a>";
}

// ---------------------------------------------------------------------------
// Rating / bonus
// ---------------------------------------------------------------------------

function check_bonus(PDO $dbh, string $game): int
{
    $retire_months = $GLOBALS['retire_months'] ?? 6;
    $bonus_games   = $GLOBALS['bonus_games']   ?? 10;
    $active        = time() - 60 * 60 * 24 * 30 * $retire_months;

    $query = "SELECT 1500-sum(value)/count(value) FROM players"
           . " LEFT JOIN ranking ON players.uid=ranking.uid AND ranking.variation='$game'"
           . " WHERE (games_won>=$bonus_games) AND (status='ok') AND (ranking.last_played>$active)";

    $sth = gs_query($dbh, $query);
    [$deficit] = nextArrayRow($sth);
    finishQuery($sth);

    $deficit = (int)$deficit;
    if ($deficit >= 2)       { $deficit =  2; }
    elseif ($deficit <= -2)  { $deficit = -2; }

    return $deficit;
}

// ---------------------------------------------------------------------------
// Game name / code lookup tables
// ---------------------------------------------------------------------------

function load_game_to_code(PDO $dbh): void
{
    $q   = "SELECT name, code, viewer, familyname FROM variation";
    $sth = gs_query($dbh, $q);
    $nr  = numRows($sth);

    while ($nr-- > 0) {
        [$un, $cd, $vv, $ff] = nextArrayRow($sth);
        $GLOBALS['gamename_to_code'][strtolower($un)]   = strtolower($cd);
        $GLOBALS['gamecode_to_name'][strtolower($cd)]   = strtolower($un);
        $GLOBALS['gamecode_to_viewer'][strtolower($cd)] = $vv;
        $GLOBALS['gamename_to_family'][strtolower($un)] = $ff;
    }
    finishQuery($sth);
}

function gamename_to_family(PDO $dbh, string $name): string
{
    $name = strtolower($name);
    $fa   = $GLOBALS['gamename_to_family'][$name] ?? null;
    if (!$fa) { load_game_to_code($dbh); $fa = $GLOBALS['gamename_to_family'][$name] ?? null; }
    return $fa ?: $name;
}

function gamename_to_gamecode(PDO $dbh, string $name, string $master = ''): string
{
    $name = strtolower($name);
    $gc   = $GLOBALS['gamename_to_code'][$name] ?? null;
    if (!$gc) {
        load_game_to_code($dbh);
        $gc = $GLOBALS['gamename_to_code'][$name] ?? null;
        if ($master === 'Yes') { $gc = "M$gc"; }
    }
    return $gc ?: 'xx-unknown-gamename';
}

function gamecode_to_gamename(PDO $dbh, string $name): string
{
    $name = strtolower($name);
    $gc   = $GLOBALS['gamecode_to_name'][$name] ?? null;
    if (!$gc) { load_game_to_code($dbh); $gc = $GLOBALS['gamecode_to_name'][$name] ?? null; }
    return $gc ?: 'xx-unknown-gamecode';
}

function gamecode_to_gameviewer(PDO $dbh, string $name): string
{
    $name = strtolower($name);
    $gc   = $GLOBALS['gamecode_to_viewer'][$name] ?? null;
    if (!$gc) { load_game_to_code($dbh); $gc = $GLOBALS['gamecode_to_viewer'][$name] ?? null; }
    return $gc ?: 'xx-unknown-gameviewer';
}

// ---------------------------------------------------------------------------
// Schema introspection
// ---------------------------------------------------------------------------

function get_column_definition(PDO $dbh, string $table, string $column): string
{
    $sth = gs_query($dbh, "DESCRIBE $table");
    $num = numRows($sth);

    while ($num > 0) {
        $num--;
        [$name, $type] = nextArrayRow($sth);
        if ($type === 'inttime') { $type = 'datetime'; }
        if (strtolower($name) === strtolower($column)) {
            finishQuery($sth);
            return $type;
        }
    }

    finishQuery($sth);
    $bt = debug_backtrace(DEBUG_BACKTRACE_IGNORE_ARGS, 1)[0];
    logError("column $column table $table not found",
             "get_column_definition from {$bt['file']} line {$bt['line']}");
    return '';
}

function get_enum_choices(PDO $dbh, string $table, string $column): array
{
    $def = get_column_definition($dbh, $table, $column);
    if (strpos($def, 'enum(') === 0) {
        // parse enum('a','b','c') -> ['a','b','c']
        preg_match_all("/'([^']*)'/", $def, $matches);
        return $matches[1];
    }
    $bt = debug_backtrace(DEBUG_BACKTRACE_IGNORE_ARGS, 1)[0];
    logError("table $table column $column is not recognised as an 'enum': $def",
             "get_enum_choices from {$bt['file']} line {$bt['line']}");
    return [];
}

function get_enum_index(PDO $dbh, string $table, string $column, string $value): int
{
    $lcc     = strtolower($value);
    $choices = get_enum_choices($dbh, $table, $column);

    foreach ($choices as $idx => $choice) {
        if (strtolower($choice) === $lcc) {
            return $idx + 1;  // MySQL enum index is 1-based
        }
    }

    $bt = debug_backtrace(DEBUG_BACKTRACE_IGNORE_ARGS, 1)[0];
    logError("can't find index for $table $column $value in " . implode(',', $choices),
             "get_enum_index from {$bt['file']} line {$bt['line']}");
    return 0;
}

// ---------------------------------------------------------------------------
// Variation listing helpers
// ---------------------------------------------------------------------------

function principle_variations(PDO $dbh): array
{
    return _fetch_column($dbh,
        "SELECT code FROM variation WHERE included=1 GROUP BY directory_index ORDER BY name");
}

function principle_variation_names(PDO $dbh): array
{
    return _fetch_column($dbh,
        "SELECT name FROM variation WHERE included=1 GROUP BY directory_index ORDER BY name");
}

function all_variations(PDO $dbh): array
{
    return _fetch_column($dbh,
        "SELECT DISTINCT name FROM variation WHERE included='1' ORDER BY name");
}

function all_2_variations(PDO $dbh): array
{
    return _fetch_column($dbh,
        "SELECT DISTINCT name FROM variation WHERE included='1' AND max_players<=2 ORDER BY name");
}

function all_4_variations(PDO $dbh): array
{
    return _fetch_column($dbh,
        "SELECT DISTINCT name FROM variation WHERE included='1' AND max_players>2 ORDER BY name");
}

/** Internal helper: fetch a single column from a query into a flat array. */
function _fetch_column(PDO $dbh, string $query): array
{
    $sth  = gs_query($dbh, $query);
    $nr   = numRows($sth);
    $vals = [];
    while ($nr-- > 0) {
        [$var] = nextArrayRow($sth);
        $vals[] = $var;
    }
    finishQuery($sth);
    return $vals;
}

// ---------------------------------------------------------------------------
// Event counting / alerting
// ---------------------------------------------------------------------------

function countEvent(string $eventname, int $high_alert = 0, int $panic_alert = 0): void
{
    $dbh = gs_connect();
    if ($dbh) {
        countDBevent($dbh, $eventname, $high_alert, $panic_alert);
    }
}

function countDBevent(PDO $dbh, string $eventname, int $high_alert, int $panic_alert): void
{
    $qname = $dbh->quote($eventname);
    $q = "INSERT INTO eventlog SET day_recorded=current_date(),name=$qname,count=1"
       . " ON DUPLICATE KEY UPDATE count=count+1";
    commandQuery($dbh, $q);

    if ($high_alert > 0) {
        $whereclause = " WHERE day_recorded=current_date() AND name=$qname";
        $sel         = "SELECT count, alert FROM eventlog $whereclause";
        $sth         = gs_query($dbh, $sel);
        [$nn, $alert] = nextArrayRow($sth);
        finishQuery($sth);

        if ($nn >= $high_alert && !$alert) {
            $msg = $dbh->quote("reached $high_alert items for event $eventname");
            commandQuery($dbh, "INSERT INTO messages SET type='alert',message=$msg");
            commandQuery($dbh, "UPDATE eventlog SET alert=true $whereclause");
        }

        if (($nn == $panic_alert) && ($panic_alert > 0)) {
            $msg = "panic: reached $panic_alert items for event $eventname";
            error_log($msg);
        }
    }
}

// ---------------------------------------------------------------------------
// phpBB sync
// ---------------------------------------------------------------------------

function changePhp(int $uid, string $user, string $pass): void
{
    $db_type      = $GLOBALS['db_type']      ?? 'mysql';
    $php_database = $GLOBALS['php_database'] ?? '';
    $db_host      = $GLOBALS['db_host']      ?? 'localhost';
    $db_user      = $GLOBALS['db_user']      ?? '';
    $db_password  = $GLOBALS['db_password']  ?? '';

    if (!$php_database) { return; }

    try {
        $dbh = new PDO(
            "$db_type:dbname=$php_database;host=$db_host",
            $db_user, $db_password,
            [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
        );
        $quid      = $dbh->quote($uid);
        $qname     = $dbh->quote($user);
        $qpassword = $dbh->quote($pass);
        $php = "UPDATE phpbb_users SET user_password=MD5($qpassword)"
             . " WHERE username=$qname LIMIT 1";
        commandQuery($dbh, $php);
    } catch (PDOException $ex) {
        logError("changePhp", $ex->getMessage());
    }
}