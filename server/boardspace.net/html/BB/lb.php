<?php
/* PHP HTTP Tarpit
 * Purpose: Confuse and waste bot scanners time.
 * Use: Url rewrite unwanted bot traffic to this file. It is important you use
 *      Url rewrites not redirects as most bots ignore location headers.
 * Version: 1.3.7 (rewritten for PHP 5.2+ compatibility)
 */

// ---------------------------------------------------------------------------
// Settings (replaces namespace constants and const arrays)
// ---------------------------------------------------------------------------
define('LB_MIN_CONTENT',        2048);
define('LB_MAX_CONTENT',        10240);
define('LB_DEFENSE',            6);     // 1=Blinding, 2=Ninja, 3=Tarpit, 4=Chained Redirect,
                                        // 5=Bounceback, 6=Random per request, 7=Random by minute
define('LB_DELAY_MIN',          10000); // microseconds
define('LB_DELAY_MAX',          300000);
define('LB_MAX_REDIRECTS',      9);     // 0-9
define('LB_DEBUG',              false);

$LB_DEFENSE_SAMPLE = array(1, 2, 3, 3, 3, 3, 3, 4, 5);

$LB_REDIRECT_STATUSES = array(301, 302, 307);

// ---------------------------------------------------------------------------
// Helper: set HTTP response code (compatible with PHP < 5.4)
// ---------------------------------------------------------------------------
function lb_response_code($code)
{
    if (function_exists('http_response_code')) {
        http_response_code($code);
    } else {
        $statuses = array(
            100 => 'Continue',
            101 => 'Switching Protocols',
            102 => 'Processing',
            200 => 'OK',
            301 => 'Moved Permanently',
            302 => 'Found',
            307 => 'Temporary Redirect',
            401 => 'Unauthorized',
            404 => 'Not Found',
        );
        $text = isset($statuses[$code]) ? $statuses[$code] : 'Unknown';
        header("HTTP/1.1 $code $text", true, $code);
    }
}

// ---------------------------------------------------------------------------
// Helper: output random junk content
// ---------------------------------------------------------------------------
function lb_rand_content()
{
    $random_words = array(
        '',
        'Public Key:',
        'Private Key:',
        'Password',
        'Username',
        "\x03",
        "\x04",
        "\x07",
        "\x21",
        " | shutdown -r now ",
        "\" | shutdown -r now ",
        "' | shutdown -r now ",
        "\xe9\x65\n\xab\n",
        " | :(){ :|:& };: ",
        "\" | :(){ :|:& };: ",
        "' | :(){ :|:& };: ",
    );

    $chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\t\n\r\s";

    $random_content_length = mt_rand(0, LB_MAX_CONTENT - LB_MIN_CONTENT) + LB_MAX_CONTENT;
    if ($random_content_length > 0) {
        $size = strlen($chars);
        $random_word_point = mt_rand(0, $random_content_length - 1);
        for ($i = 0; $i < $random_content_length; $i++) {
            if ($i == $random_word_point)
                echo $random_words[mt_rand(0, count($random_words) - 1)];
            echo $chars[mt_rand(0, $size - 1)];
        }
    }
}

// ---------------------------------------------------------------------------
// Helper: random auth realm
// ---------------------------------------------------------------------------
function lb_rand_realm()
{
    $host = isset($_SERVER['HTTP_HOST']) ? $_SERVER['HTTP_HOST'] : '';
    $realms = array(
        'phpMyAdmin ' . $host,
        'GitLab Packages Registry',
    );

    $ip = (string) filter_var(
        isset($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : '',
        FILTER_VALIDATE_IP,
        FILTER_FLAG_IPV4
    );
    if (!empty($ip))
        mt_srand(ip2long($ip));

    $realm = $realms[mt_rand(0, count($realms) - 1)];

    if (!empty($ip))
        mt_srand();

    return $realm;
}

// ---------------------------------------------------------------------------
// Helper: build this server's base URL
// ---------------------------------------------------------------------------
function lb_self_url()
{
    if (empty($_SERVER['HTTP_HOST']))
        return false;

    $url = 'http';
    if ((isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] == 'on') ||
        (isset($_SERVER['SERVER_PORT']) && $_SERVER['SERVER_PORT'] == '443'))
        $url .= 's';

    $url .= '://';
    $port = isset($_SERVER['SERVER_PORT']) ? $_SERVER['SERVER_PORT'] : '80';
    if ($port != '80' && $port != '443')
        $url .= $_SERVER['HTTP_HOST'] . ':' . $port;
    else
        $url .= $_SERVER['HTTP_HOST'];

    return $url;
}

// ---------------------------------------------------------------------------
// Helper: redirect URL pointing back at the scanner
// ---------------------------------------------------------------------------
function lb_remote_redirect_url()
{
    $ports = array(80, 443, 8080, 8443, 8888);
    return 'http://' . $_SERVER['REMOTE_ADDR'] . ':' . $ports[array_rand($ports)];
}

// ---------------------------------------------------------------------------
// Helper: validate that a string is a non-negative integer
// ---------------------------------------------------------------------------
function lb_validate_integer($s)
{
    return preg_match('/^(\d+)$/', $s);
}

// ---------------------------------------------------------------------------
// Main logic
// ---------------------------------------------------------------------------

// Random delay before responding
usleep(mt_rand(LB_DELAY_MIN, LB_DELAY_MAX));

// HEAD requests: entice scanner to follow up with a GET
if (isset($_SERVER['REQUEST_METHOD']) && 'HEAD' == $_SERVER['REQUEST_METHOD']) {
    lb_response_code(200);
    header("Content-Length: " . (mt_rand(0, LB_MAX_CONTENT - LB_MIN_CONTENT) + LB_MAX_CONTENT));
    die();
}

$defense_number = LB_DEFENSE;

// Enforce chained redirection if query string matches our pattern
$times_redirected = 0;
if (!empty($_SERVER['QUERY_STRING'])) {
    $refered_page = $_SERVER['QUERY_STRING'];
    if (!empty($refered_page) && lb_validate_integer($refered_page)) {
        $key_number = substr($refered_page, 0, strlen($refered_page) - 1);
        if (!empty($refered_page) && 0 == $key_number % 4242) {
            $times_redirected = substr($refered_page, -1);
            if (lb_validate_integer($times_redirected)) {
                if ($times_redirected < LB_MAX_REDIRECTS)
                    $defense_number = 4;
                else
                    $defense_number = mt_rand(0, 1) ? 5 : 1;
            } elseif ($defense_number == 4) {
                $times_redirected = 0;
            }
        }
    }
}

// Randomize defense selection
if (6 == $defense_number) {
    $defense_number = $LB_DEFENSE_SAMPLE[array_rand($LB_DEFENSE_SAMPLE)];
} elseif (7 == $defense_number) {
    mt_srand((int)date('i'));
    $defense_number = mt_rand(1, 5);
    mt_srand();
}

$redirect_url = '';

switch ($defense_number) {

    // Blinding Mode: flood scanner logs with false positives
    case 1:
        lb_response_code(200);
        lb_rand_content();
        break;

    // Ninja Mode: false negatives
    case 2:
        lb_response_code(404);
        echo '404 Not Found';
        if (mt_rand(0, 1))
            lb_rand_content();
        break;

    // HTTP Tarpit: slow the scanner down
    case 3:
        $rand_num = mt_rand(0, 3);
        if (2 == $rand_num) {
            lb_response_code(401);
            header('WWW-Authenticate: Basic realm="' . lb_rand_realm() . '"');
            echo "401 Not Authorized\n";
            lb_rand_content();
            break;
        }
        if (!LB_DEBUG) {
            lb_response_code(100 + $rand_num);
            if (1 == $rand_num)
                header("Upgrade: HTTP/2.0");
        } else {
            echo "HTTP/1.1 10$rand_num";
        }
        break;

    // Endless Redirect: down the rabbit hole
    case 4:
        if ($times_redirected >= LB_MAX_REDIRECTS || !lb_validate_integer($times_redirected))
            $times_redirected = 0;
        $times_redirected++;

        $base = lb_self_url();
        if ($base)
            $redirect_url = $base
                . (isset($_SERVER['PHP_SELF']) ? $_SERVER['PHP_SELF'] : '')
                . '?' . (mt_rand(1, 1000) * 4242) . $times_redirected;
        // fall through to case 5 intentionally

    // Bounceback Redirect: throw the scanner back at itself
    case 5:
        if (empty($redirect_url))
            $redirect_url = lb_remote_redirect_url();

        lb_response_code($LB_REDIRECT_STATUSES[array_rand($LB_REDIRECT_STATUSES)]);
        header('Location: ' . $redirect_url);
        if (mt_rand(0, 1))
            lb_rand_content();
        break;
}

die();
