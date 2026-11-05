

function allow_ip_access($dbh, $ip) {
    $intip = ip_to_int($ip);
    
    if (!$intip) {
        return 0; // no ip specified
    }

    // don't do this with no IP, as happens for the "cookie" pass if there is no cookie
    $loginok = true;
    $ii = $dbh->quote($intip);
    $bantime = $GLOBALS['bad_login_time'] * 60;
    $range = "($ii>=min AND $ii<=max)";
    $query = "SELECT status, uid, if((UNIX_TIMESTAMP(changed)+$bantime)>UNIX_TIMESTAMP(),1,0),
                     min, max, UNIX_TIMESTAMP(changed), UNIX_TIMESTAMP(),
                     short_time, long_time
              FROM ipinfo
              WHERE $range";

    $sth = query($dbh, $query);
    $numrows = numRows($sth);
    $gooduid = ($numrows > 0) ? 0 : 1; // no info is ok

    if (strpos($ip, '.') !== false) {
        $GLOBALS['single_ip_ip'] = $intip;
    }

    while ($numrows > 0) {
        $numrows--;
        [$st, $uid, $changed, $min, $max, $changedtime, $now, $short_time, $long_time] = nextArrayRow($sth);

        if ((($st === 'autobanned') && $changed) || ($st === 'banned')) {
            $dt = $now - $changedtime;

            if ($dt > 60) {
                // only make new database entries once per minute to avoid floods
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
                // if he accumulates enough penalties, ban him
                $bancom = $dbh->quote("excess long penalty=$long_penalty");
                $quid   = $dbh->quote($uid);
                commandQuery($dbh,
                    "UPDATE ipinfo
                     SET status='autobanned', long_time=0, short_time=0,
                         comment=CONCAT(comment, '\n', $da, $bancom)
                     WHERE uid=$quid"
                );
                $gooduid = 0;
            }

            $GLOBALS['single_ip_uid'] = $uid;

            if ($penalty > 0) {
                // short_time is the earliest time the user is allowed to
                // use the database again. If it's too soon, delay a while.
                sleep($penalty);
                $GLOBALS['single_ip_start_time'] = $now + $penalty;
            }
        }
    }

    finishQuery($sth);
    return $loginok ? $gooduid : -1;
}