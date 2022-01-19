#!/usr/bin/perl
#
#   CGIProxy 2.1beta10
#
#   nph-proxy.cgi-- CGIProxy 2.1: a proxy in the form of a CGI script.
#     Retrieves the resource at any HTTP or FTP URL, updating embedded URLs
#     in HTML and other resources to point back through this script.  By
#     default, no user info is sent to the server.  Options include
#     text-only proxying to save bandwidth, cookie filtering, ad filtering,
#     script removal, user-defined encoding of the target URL, and more.
#     Requires Perl 5.
#
#   Copyright (C) 1996, 1998-2005 by James Marshall, james@jmarshall.com
#   All rights reserved.  Free for non-commercial use; commercial use
#   requires a license.
#
#   For the latest, see http://www.jmarshall.com/tools/cgiproxy/
#
#
#   IMPORTANT NOTE ABOUT ANONYMOUS BROWSING:
#     CGIProxy was originally made for indirect browsing more than
#       anonymity, but since people are using it for anonymity, I've tried
#       to make it as anonymous as possible.  Suggestions welcome.  For best
#       anonymity, browse with JavaScript turned off.  In fact, that's the
#       only reliable way, in spite of what certain anonymity vendors claim.
#       That said, please notify me if you find any privacy holes, even when
#       using JavaScript.
#     Anonymity is good, but may not be bulletproof.  For example, if even
#       a single unchecked JavaScript statement can be run, your anonymity
#       can be compromised.  I've tried to handle JS in every place it can
#       exist, but please tell me if I missed any.  Also, browser plugins
#       or other executable extensions may be able to reveal you to a server.
#       Also, be aware that this script doesn't modify PDF files or other
#       third-party document formats that may contain linking ability, so
#       you will lose your anonymity if you follow links in such files.
#     If you find any other way your anonymity can be compromised, please let
#       me know.
#
#
#   CONFIGURATION:
#
#     None required in most situations.  On some servers, these might be
#       required (all in the "user configuration" section):
#       . If you're using another HTTP or SSL proxy, set $HTTP_PROXY,
#           $SSL_PROXY, and $NO_PROXY as needed.  If those proxies use
#           authentication, set $PROXY_AUTH and $SSL_PROXY_AUTH accordingly.
#       . If this is running on an SSL server that doesn't use port 443, set
#           $RUNNING_ON_SSL_SERVER=1 (otherwise, the default of '' is fine).
#
#     Options include:
#       . Set $TEXT_ONLY, $REMOVE_COOKIES, $REMOVE_SCRIPTS, $FILTER_ADS,
#           $HIDE_REFERER, and $INSERT_ENTRY_FORM as desired.  Set
#           $REMOVE_SCRIPTS if anonymity is important.
#       . To let the user choose all of those settings (except $TEXT_ONLY),
#           set $ALLOW_USER_CONFIG=1.
#       . To change the encoding format of the URL, modify the
#           proxy_encode() and proxy_decode() routines.  The default
#           routines are suitable for simple PATH_INFO compliance.
#       . To encode cookies, modify the cookie_encode() and cookie_decode()
#           routines.
#       . You can restrict which servers this proxy will access, with
#           @ALLOWED_SERVERS and @BANNED_SERVERS.
#       . Similarly, you can specify allowed and denied server lists for
#           both cookies and scripts.
#       . For security, you can ban access to private IP ranges, with
#           @BANNED_NETWORKS.
#       . If filtering ads, you can customize this with a few settings.
#       . To insert your own block of HTML into each page, set $INSERT_HTML
#           or $INSERT_FILE.
#       . As a last resort, if you really can't run this script as NPH,
#           you can try to run it as non-NPH by setting $NOT_RUNNING_AS_NPH=1.
#           BUT, read the notes and warnings above that line.  Caveat surfor.
#       . For crude load-balancing among a set of proxies, set @PROXY_GROUP.
#       . Other config is possible; see the user configuration section.
#       . If heavy use of this proxy puts a load on your server, see the
#           "NOTES ON PERFORMANCE" section below.
#
#     For more info, read the comments regarding any config options you set.
#
#     This script MUST be installed as a non-parsed header (NPH) script.
#       In Apache and many other servers, this is done by simply starting the
#       filename with "nph-".  It MAY be possible to fake it as a non-NPH
#       script, MOST of the time, by using the $NOT_RUNNING_AS_NPH feature.
#       This is not advised.  See the comments by that option for warnings.
#
#
#   TO USE:
#     Start a browsing session by calling the script with no parameters.
#       You can bookmark pages you browse to through the proxy, or link to
#       the URLs that are generated.
#
#
#   NOTES ON PERFORMANCE:
#     Unfortunately, this has gotten slower through the versions, mostly
#       because of optional new features.  Configured equally, version 1.3
#       takes 25% longer to run than 1.0 or 1.1 (based on *cough* highly
#       abbreviated testing).  Compiling takes about 50% longer.
#     Leaving $REMOVE_SCRIPTS=1 adds 25-50% to the running time.
#     Remember that we're talking about tenths of a second here.  Most of
#       the delay experienced by the user is from waiting on two network
#       connections.  These performance issues only matter if your server
#       CPU is getting overloaded.  Also, these only matter when retrieving
#       HTML, because it's the HTML modification that takes all the time.
#     If you can, use mod_perl.  Starting with version 1.3.1, this should
#       work under mod_perl, which requires Perl 5.004 or later.  If you use
#       mod_perl, be careful to install this as an NPH script, i.e. set the
#       "PerlSendHeader Off" configuration directive.  For more info, see the
#       mod_perl documentation.
#     If you use mod_perl and modify this script, see the note near the
#       "reset 'a-z'" line below, regarding UPPER_CASE and lower_case
#       variables.
#
#
#   TO DO:
#     What I want to hear about:
#       . Any HTML tags not being converted here.
#       . Any method of introducing JavaScript or other script, that's not
#           being handled here.
#       . Any script MIME types other than those already in @SCRIPT_MIME_TYPES.
#       . Any MIME types other than text/html that have links that need to
#           be converted.
#     plug any other script holes (e.g. MSIE-proprietary, other MIME types?)
#     This could use cleaner URL-encoding all over ($base_url, etc.)
#     more error checking?
#     find a simple encryption technique for proxy_encode()
#     support more protocols, like mailto: or gopher:
#     For ad filtering, add option to disable images from servers other than
#       that of the containing HTML page?  Is it worth it?
#
#
#   BUGS:
#     Anonymity may not not perfect.  In particular, there may be some remaining
#       JavaScript holes.  Please let me know if you find any.
#     Since ALL of your cookies are sent to this script (which then chooses
#       the relevant ones), some cookies could conceivably be dropped if
#       you accumulate a whole lot.  I haven't seen this happen yet.
#
#
#   I first wrote this in 1996 as an experiment to allow indirect browsing.
#     The original seed was a program I wrote for Rich Morin's article
#     in the June 1996 issue of Unix Review, online at
#     http://www.cfcl.com/tin/P/199606.shtml.
#
#   Confession: I didn't originally write this with the spec for HTTP
#     proxies in mind, and there are probably some violations of the protocol
#     (at least for proxies).  This whole thing is one big violation of the
#     proxy model anyway, so I hereby rationalize that the spec can be widely
#     interpreted here.  If there is demand, I can make it more conformant.
#     The HTTP client and server components should be fine; it's just the
#     special requirements for proxies that may not be followed.
#
#--------------------------------------------------------------------------

use strict ;
use Socket ;

# First block below is config variables, second block is sort-of config
#   variables, third block is persistent constants, fourth block is would-be
#   persistent constants (not set until needed), fifth block is constants for
#   JavaScript processing (mostly regular expressions), and last block is
#   variables.
use vars qw(
   $TEXT_ONLY
   $REMOVE_COOKIES  $REMOVE_SCRIPTS  $FILTER_ADS  $HIDE_REFERER
   $INSERT_ENTRY_FORM  $ALLOW_USER_CONFIG
   @ALLOWED_SERVERS  @BANNED_SERVERS  @BANNED_NETWORKS
   $NO_COOKIE_WITH_IMAGE  @ALLOWED_COOKIE_SERVERS  @BANNED_COOKIE_SERVERS
   @ALLOWED_SCRIPT_SERVERS  @BANNED_SCRIPT_SERVERS
   @BANNED_IMAGE_URL_PATTERNS  $RETURN_EMPTY_GIF
   $USER_IP_ADDRESS_TEST  $DESTINATION_SERVER_TEST
   $INSERT_HTML  $INSERT_FILE  $ANONYMIZE_INSERTION  $FORM_AFTER_INSERTION
   $INSERTION_FRAME_HEIGHT
   $RUNNING_ON_SSL_SERVER  $NOT_RUNNING_AS_NPH
   $HTTP_PROXY  $SSL_PROXY  $NO_PROXY  $PROXY_AUTH  $SSL_PROXY_AUTH
   $MINIMIZE_CACHING
   $SESSION_COOKIES_ONLY  $COOKIE_PATH_FOLLOWS_SPEC  $RESPECT_THREE_DOT_RULE
   @PROXY_GROUP
   $USER_AGENT  $USE_PASSIVE_FTP_MODE  $SHOW_FTP_WELCOME
   $PROXIFY_SCRIPTS  $ALLOW_UNPROXIFIED_SCRIPTS  $PROXIFY_COMMENTS
   $ENCODE_DECODE_BLOCK_IN_JS
   $USE_POST_ON_START  $ENCODE_URL_INPUT
   $REMOVE_TITLES  $NO_BROWSE_THROUGH_SELF  $NO_LINK_TO_START  $MAX_REQUEST_SIZE
   $QUIETLY_EXIT_PROXY_SESSION
   $OVERRIDE_SECURITY

   @SCRIPT_MIME_TYPES  @OTHER_TYPES_TO_REGISTER  @TYPES_TO_HANDLE
   $NON_TEXT_EXTENSIONS
   $PROXY_VERSION

   @MONTH  @WEEKDAY  %UN_MONTH
   @BANNED_NETWORK_ADDRS
   $USER_IP_ADDRESS_TEST_H  $DESTINATION_SERVER_TEST_H
   $RUNNING_ON_IIS
   @NO_PROXY
   $NO_CACHE_HEADERS
   @ALL_TYPES  %MIME_TYPE_ID  $SCRIPT_TYPE_REGEX  $TYPES_TO_HANDLE_REGEX
   $THIS_HOST  $ENV_SERVER_PORT  $ENV_SCRIPT_NAME  $THIS_SCRIPT_URL
   $HAS_BEGUN

   $CUSTOM_INSERTION  %IN_CUSTOM_INSERTION

   $RE_JS_WHITE_SPACE  $RE_JS_LINE_TERMINATOR  $RE_JS_COMMENT
   $RE_JS_IDENTIFIER_START  $RE_JS_IDENTIFIER_PART  $RE_JS_IDENTIFIER_NAME
   $RE_JS_PUNCTUATOR  $RE_JS_DIV_PUNCTUATOR
   $RE_JS_NUMERIC_LITERAL  $RE_JS_ESCAPE_SEQUENCE  $RE_JS_STRING_LITERAL
   $RE_JS_REGULAR_EXPRESSION_LITERAL
   $RE_JS_TOKEN  $RE_JS_INPUT_ELEMENT_DIV  $RE_JS_INPUT_ELEMENT_REG_EXP
   $RE_JS_SKIP  $RE_JS_SKIP_NO_LT
   $JSLIB_BODY

   $HTTP_VERSION  $HTTP_1_X
   $URL
   $now
   $packed_flags  $encoded_URL  $doing_insert_here  $env_accept
   $e_remove_cookies  $e_remove_scripts  $e_filter_ads  $e_insert_entry_form
   $e_hide_referer
   $images_are_banned_here  $scripts_are_banned_here  $cookies_are_banned_here
   $scheme  $authority  $path  $host  $port  $username  $password
   $cookie_to_server  %auth
   $script_url  $url_start  $url_start_inframe  $url_start_noframe
   $is_in_frame  $expected_type
   $base_url  $base_scheme  $base_host  $base_path  $base_file  $base_unframes
   $default_style_type  $default_script_type
   $status  $headers  $body  $is_html  $response_sent
   %in_mini_start_form
   $needs_jslib
   $debug ) ;


# Under mod_perl, persistent constants only need to be initialized once, so
#   use this one-time block to do so.
unless ($HAS_BEGUN) {

#--------------------------------------------------------------------------
#    user configuration
#--------------------------------------------------------------------------

# If set, then proxy traffic will be restricted to text data only, to save
#   bandwidth (though it can still be circumvented with uuencode, etc.).
# To replace images with a 1x1 transparent GIF, set $RETURN_EMPTY_GIF below.

$TEXT_ONLY= 0 ;      # set to 1 to allow only text data, 0 to allow all


# If set, then prevent all cookies from passing through the proxy.  To allow
#   cookies from some servers, set this to 0 and see @ALLOWED_COOKIE_SERVERS
#   and @BANNED_COOKIE_SERVERS below.  You can also prevent cookies with
#   images by setting $NO_COOKIE_WITH_IMAGE below.
# Note that this only affects cookies from the target server.   The proxy
#   script sends its own cookies for other reasons too, like to support
#   authentication.  This flag does not stop these cookies from being sent.

$REMOVE_COOKIES= 0 ;


# If set, then remove as much scripting as possible.  If anonymity is
#   important, this is strongly recommended!  Better yet, turn off script
#   support in your browser.
# On the HTTP level:
#   . prevent transmission of script MIME types (which only works if the server
#       marks them as such, so a malicious server could get around this, but
#       then the browser probably wouldn't execute the script).
#   . remove Link: headers that link to a resource of a script MIME type.
# Within HTML resources:
#   . remove <script>...</script> .
#   . remove intrinsic event attributes from tags, i.e. attributes whose names
#       begin with "on".
#   . remove <style>...</style> where "type" attribute is a script MIME type.
#   . remove various HTML tags that appear to link to a script MIME type.
#   . remove script macros (aka Netscape-specific "JavaScript entities"),
#       i.e. any attributes containing the string "&{" .
#   . remove "JavaScript conditional comments".
#   . remove MSIE-specific "dynamic properties".
# To allow scripts from some sites but not from others, set this to 0 and
#   see @ALLOWED_SCRIPT_SERVERS and @BANNED_SCRIPT_SERVERS below.
# See @SCRIPT_MIME_TYPES below for a list of which MIME types are filtered out.
# I do NOT know for certain that this removes all script content!  It removes
#   all that I know of, but I don't have a definitive list of places scripts
#   can exist.  If you do, please send it to me.  EVEN RUNNING A SINGLE
#   JAVASCRIPT STATEMENT CAN COMPROMISE YOUR ANONYMITY!  Just so you know.
# Richard Smith has a good test site for anonymizing proxies, at
#   http://users.rcn.com/rms2000/anon/test.htm
# Note that turning this on removes most popup ads!  :)

$REMOVE_SCRIPTS= 1 ;


# If set, then filter out images that match one of @BANNED_IMAGE_URL_PATTERNS,
#   below.  Also removes cookies attached to images, as if $NO_COOKIE_WITH_IMAGE
#   is set.
# To remove most popup advertisements, also set $REMOVE_SCRIPTS=1 above.

$FILTER_ADS= 0 ;


# If set, then don't send a Referer: [sic] header with each request
#   (i.e. something that tells the server which page you're coming from
#   that linked to it).  This is a minor privacy issue, but a few sites
#   won't send you pages or images if the Referer: is not what they're
#   expecting.  If a page is loading without images or a link seems to be
#   refused, then try turning this off, and a correct Referer: header will
#   be sent.
# This is only a problem in a VERY small percentage of sites, so few that
#   I'm kinda hesitant to put this in the entry form.  Other arrangements
#   have their own problems, though.

$HIDE_REFERER= 1 ;


# If set, insert a compact version of the URL entry form at the top of each
#   page.  This will also display the URL currently being viewed.
# When viewing a page with frames, then a new top frame is created and the
#   insertion goes there.
# If you want to customize the appearance of the form, modify the routine
#   mini_start_form() near the end of the script.
# If you want to insert something other than this form, see $INSERT_HTML and
#   $INSERT_FILE below.
# Users should realize that options changed via the form only take affect when
#   the form is submitted by entering a new URL or pressing the "Go" button.
#   Selecting an option, then following a link on the page, will not cause
#   the option to take effect.
# Users should also realize that anything inserted into a page may throw
#   off any precise layout.  The insertion will also be subject to
#   background colors and images, and any other page-wide settings.

$INSERT_ENTRY_FORM= 1 ;


# If set, then allow the user to control $REMOVE_COOKIES, $REMOVE_SCRIPTS,
#   $FILTER_ADS, $HIDE_REFERER, and $INSERT_ENTRY_FORM.  Note that they
#   can't fine-tune any related options, such as the various @ALLOWED... and
#   @BANNED... lists.

$ALLOW_USER_CONFIG= 1 ;



# If you want to encode the URLs of visited pages so that they don't show
#   up within the full URL in your browser bar, then use proxy_encode() and
#   proxy_decode().  These are Perl routines that transform the way the
#   destination URL is included in the full URL.  You can either use
#   some combination of the example encodings below, or you can program your
#   own routines.  The encoded form of URLs should only contain characters
#   that are legal in PATH_INFO.  This varies by server, but using only
#   printable chars and no "?" or "#" works on most servers.  Don't let
#   PATH_INFO contain the strings "./", "/.", "../", or "/..", or else it
#   may get compressed like a pathname somewhere.  Try not to make the
#   resulting string too long, either.
# Of course, proxy_decode() must exactly undo whatever proxy_encode() does.
# Make proxy_encode() as fast as possible-- it's a bottleneck for the whole
#   program.  The speed of proxy_decode() is not as important.
# If you're not a Perl programmer, you can use the example encodings that are
#   commented out, i.e. the lines beginning with "#".  To use them, merely
#   uncomment them, i.e. remove the "#" at the start of the line.  If you
#   uncomment a line in proxy_encode(), you MUST uncomment the corresponding
#   line in proxy_decode() (note that "corresponding lines" in
#   proxy_decode() are in reverse order of those in proxy_encode()).  You
#   can use one, two, or all three encodings at the same time, as long as
#   the correct lines are uncommented.
# Starting in version 2.1beta9, don't call these functions directly.  Rather,
#   call wrap_proxy_encode() and wrap_proxy_decode() instead, which handle
#   certain details that you shouldn't have to worry about in these functions.
# IMPORTANT: If you modify these routines, and if $PROXIFY_SCRIPTS is set
#   below (on by default), then you MUST modify $ENCODE_DECODE_BLOCK_IN_JS
#   below!!  (You'll need to write corresponding routines in JavaScript to do
#   the same as these routines in Perl, used when proxifying JavaScript.)
# Because of the simplified absolute URL resolution in full_url(), there may
#   be ".." segments in the default encoding here, notably in the first path
#   segment.  Normally, that's just an HTML mistake, but please tell me if
#   you see any privacy exploit with it.
# Note that a few sites have embedded applications (like applets or Shockwave)
#   that expect to access URLs relative to the page's URL.  This means they
#   may not work if the encoded target URL can't be treated like a base URL,
#   e.g. that it can't be appended with something like "../data/foo.data"
#   to get that expected data file.  In such cases, the default encoding below
#   should let these sites work fine, as should any other encoding that can
#   support URLs relative to it.

sub proxy_encode {
    my($URL)= @_ ;
    $URL=~ s#^([\w+.-]+)://#$1/# ;                 # http://xxx -> http/xxx
#    $URL=~ s/(.)/ sprintf('%02x',ord($1)) /ge ;   # each char -> 2-hex
#    $URL=~ tr/a-zA-Z/n-za-mN-ZA-M/ ;              # rot-13

    return $URL ;
}

sub proxy_decode {
    my($enc_URL)= @_ ;

#    $enc_URL=~ tr/a-zA-Z/n-za-mN-ZA-M/ ;        # rot-13
#    $enc_URL=~ s/([\da-fA-F]{2})/ sprintf("%c",hex($1)) /ge ;
    $enc_URL=~ s#^([\w+.-]+)/#$1://# ;           # http/xxx -> http://xxx
    return $enc_URL ;
}


# Encode cookies before they're sent back to the user.
# The return value must only contain characters that are legal in cookie
#   names and values, i.e. only printable characters, and no ";", ",", "=",
#   or white space.
# cookie_encode() is called twice for each cookie: once to encode the cookie
#   name, and once to encode the cookie value.  The two are then joined with
#   "=" and sent to the user.
# cookie_decode() must exactly undo whatever cookie_encode() does.
# Also, cookie_encode() must always encode a given input string into the
#   same output string.  This is because browsers need the cookie name to
#   identify and manage a cookie, so the name must be consistent.
# This is not a bottleneck like proxy_encode() is, so speed is not critical.
# IMPORTANT: If you modify these routines, and if $PROXIFY_SCRIPTS is set
#   below (on by default), then you MUST modify $ENCODE_DECODE_BLOCK_IN_JS
#   below!!  (You'll need to write corresponding routines in JavaScript to do
#   the same as these routines in Perl, used when proxifying JavaScript.)

sub cookie_encode {
    my($cookie)= @_ ;
#    $cookie=~ s/(.)/ sprintf('%02x',ord($1)) /ge ;   # each char -> 2-hex
#    $cookie=~ tr/a-zA-Z/n-za-mN-ZA-M/ ;              # rot-13
    $cookie=~ s/(\W)/ '%' . sprintf('%02x',ord($1)) /ge ; # simple URL-encoding
    return $cookie ;
}

sub cookie_decode {
    my($enc_cookie)= @_ ;
    $enc_cookie=~ s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;  # URL-decode
#    $enc_cookie=~ tr/a-zA-Z/n-za-mN-ZA-M/ ;          # rot-13
#    $enc_cookie=~ s/([\da-fA-F]{2})/ sprintf("%c",hex($1)) /ge ;
    return $enc_cookie ;
}


# If $PROXIFY_SCRIPTS is true, and if you modify the routines above that
#   encode cookies and URLs, then you need to modify $ENCODE_DECODE_BLOCK_IN_JS
#   here.  Explanation:  When proxifying JavaScript, a library of JavaScript
#   functions is used.  In that library are a few JavaScript routines that do
#   the same as their Perl counterparts in this script.  Four of those routines
#   are proxy_encode(), proxy_decode(), cookie_encode(), and cookie_decode().
#   Thus, unfortunately, when you write your own versions of those Perl routines
#   (or modify what's already there), you also need to write (or modify) these
#   corresponding JavaScript routines to do the same thing.  Put the routines in
#   this long variable $ENCODE_DECODE_BLOCK_IN_JS, and it will be included in
#   the JavaScript library when needed.  Prefix the function names with
#   "_proxy_jslib_", as below.
# The commented examples in the JavaScript routines below correspond exactly to
#   the commented examples in the Perl routines above.  Thus, if you modify the
#   Perl routines by merely uncommenting the examples, you can do the same in
#   these JavaScript routines.  (JavaScript comments begin with "//".)
# [If you don't know Perl:  Note that everything up until the line "EOB" is one
#   long string value, called a "here document".  $ENCODE_DECODE_BLOCK_IN_JS is
#   set to the whole thing.]
# jsm-- String.charCodeAt not in MSIE 5.0.
# jsm-- String.replace() with function doesn't work in MSIE 5.0.

$ENCODE_DECODE_BLOCK_IN_JS= <<'EOB' ;

function _proxy_jslib_proxy_encode(URL) {
    URL= URL.replace(/^([\w\+\.\-]+)\:\/\//, '$1/') ;
//    URL= URL.replace(/(.)/g, function (s,p1) { return p1.charCodeAt(0).toString(16) } ) ;
//    URL= URL.replace(/([a-mA-M])|[n-zN-Z]/g, function (s,p1) { return String.fromCharCode(s.charCodeAt(0)+(p1?13:-13)) }) ;

    return URL ;
}

function _proxy_jslib_proxy_decode(enc_URL) {
//    enc_URL= enc_URL.replace(/([a-mA-M])|[n-zN-Z]/g, function (s,p1) { return String.fromCharCode(s.charCodeAt(0)+(p1?13:-13)) }) ;
//    enc_URL= enc_URL.replace(/([\da-fA-F]{2})/g, function (s,p1) { return String.fromCharCode(eval('0x'+p1)) } ) ;
    enc_URL= enc_URL.replace(/^([\w\+\.\-]+)\//, '$1://') ;
    return enc_URL ;
}

function _proxy_jslib_cookie_encode(cookie) {
//    cookie= cookie.replace(/(.)/g, function (s,p1) { return p1.charCodeAt(0).toString(16) } ) ;
//    cookie= cookie.replace(/([a-mA-M])|[n-zN-Z]/g, function (s,p1) { return String.fromCharCode(s.charCodeAt(0)+(p1!=null?13:-13)) }) ;
    cookie= cookie.replace(/(\W)/g, function (s,p1) { return '%'+p1.charCodeAt(0).toString(16) } ) ;
    return cookie ;
}

function _proxy_jslib_cookie_decode(enc_cookie) {
    enc_cookie= enc_cookie.replace(/%([\da-fA-F]{2})/g, function (s,p1) { return String.fromCharCode(eval('0x'+p1)) } ) ;
//    enc_cookie= enc_cookie.replace(/([a-mA-M])|[n-zN-Z]/g, function (s,p1) { return String.fromCharCode(s.charCodeAt(0)+(p1!=null?13:-13)) }) ;
//    enc_cookie= enc_cookie.replace(/([\da-fA-F]{2})/g, function (s,p1) { return String.fromCharCode(eval('0x'+p1)) } ) ;
    return enc_cookie ;
}

EOB



# Use @ALLOWED_SERVERS and @BANNED_SERVERS to restrict which servers a user
#   can visit through this proxy.  Any URL at a host matching a pattern in
#   @BANNED_SERVERS will be forbidden.  In addition, if @ALLOWED_SERVERS is
#   not empty, then access is allowed *only* to servers that match a pattern
#   in it.  In other words, @BANNED_SERVERS means "ban these servers", and
#   @ALLOWED_SERVERS (if not empty) means "allow only these servers".  If a
#   server matches both lists, it is banned.
# These are each a list of Perl 5 regular expressions (aka patterns or
#   regexes), not literal host names.  To turn a hostname into a pattern,
#   replace every "." with "\.", add "^" to the beginning, and add "$" to the
#   end.  For example, "www.example.com" becomes "^www\.example\.com$".  To
#   match *every* host ending in something, leave out the "^".  For example,
#   "\.example\.com$" matches every host ending in ".example.com".  For more
#   details about Perl regular expressions, see the Perl documentation.  (They
#   may seem cryptic at first, but they're very powerful once you know how to
#   use them.)
@ALLOWED_SERVERS= () ;
@BANNED_SERVERS= () ;


# If @BANNED_NETWORKS is set, then forbid access to these hosts or networks.
# This is done by IP address, not name, so it provides more certain security
#   than @BANNED_SERVERS above.
# Specify each element as a decimal IP address-- all four integers for a host,
#   or one to three integers for a network.  For example, '127.0.0.1' bans
#   access to the local host, and '192.168' bans access to all IP addresses
#   in the 192.168 network.  Sorry, no banning yet for subnets other than
#   8, 16, or 24 bits.
# IF YOU'RE RUNNING THIS ON OR INSIDE A FIREWALL, THIS SETTING IS STRONGLY
#   RECOMMENDED!!  In particular, you should ban access to other machines
#   inside the firewall that the firewall machine itself may have access to.
#   Otherwise, external users will be able to access any internal hosts that
#   the firewall can access.  Even if that's what you intend, you should ban
#   access to any hosts that you don't explicitly want to expose to outside
#   users.
# In addition to the recommended defaults below, add all IP addresses of your
#   server machine if you want to protect it like this.
# After you set this, YOU SHOULD TEST to verify that the proxy can't access
#   the IP addresses you're banning!
# NOTE:  According to RFC 1918, network address ranges reserved for private
#   networks are 10.x.x.x, 192.168.x.x, and 172.16.x.x-172.31.x.x, i.e. with
#   respective subnet masks of 8, 16, and 12 bits.  Since we can't currently
#   do a 12-bit mask, we'll exclude the entire 172 network here.  If this
#   causes a problem, let me know and I'll add subnet masks down to 1-bit
#   resolution.
# Also included are 169.254.x.x (per RFC 3927) and 244.0.0.x (used for
#   routing), as recommended by Waldo Jaquith.
# On some systems, 127.x.x.x all point to localhost, so disallow all of "127".
# This feature is simple now but may be more complete in future releases.
#   How would you like this to be extended?  What would be useful to you?
@BANNED_NETWORKS= ('127', '192.168', '172', '10', '169.254', '244.0.0') ;


# Settings to fine-tune cookie filtering, if cookies are not banned altogether
#   (by user checkbox or $REMOVE_COOKIES above).
# Use @ALLOWED_COOKIE_SERVERS and @BANNED_COOKIE_SERVERS to restrict which
#   servers can send cookies through this proxy.  They work like
#   @ALLOWED_SERVERS and @BANNED_SERVERS above, both in how their precedence
#   works, and that they're lists of Perl 5 regular expressions.  See the
#   comments there for details.

# If non-empty, only allow cookies from servers matching one of these patterns.
# Comment this out to allow all cookies (subject to @BANNED_COOKIE_SERVERS).
#@ALLOWED_COOKIE_SERVERS= ('\bslashdot\.org$') ;

# Reject cookies from servers matching these patterns.
@BANNED_COOKIE_SERVERS= (
    '\.doubleclick\.net$',
    '\.preferences\.com$',
    '\.imgis\.com$',
    '\.adforce\.com$',
    '\.focalink\.com$',
    '\.flycast\.com$',
    '\.avenuea\.com$',
    '\.linkexchange\.com$',
    '\.pathfinder\.com$',
    '\.burstnet\.com$',
    '\btripod\.com$',
    '\bgeocities\.yahoo\.com$',
    '\.mediaplex\.com$',
    ) ;

# Set this to reject cookies returned with images.  This actually prevents
#   cookies returned with any non-text resource.
$NO_COOKIE_WITH_IMAGE= 1 ;


# Settings to fine-tune script filtering, if scripts are not banned altogether
#   (by user checkbox or $REMOVE_SCRIPTS above).
# Use @ALLOWED_SCRIPT_SERVERS and @BANNED_SCRIPT_SERVERS to restrict which
#   servers you'll allow scripts from.  They work like @ALLOWED_SERVERS and
#   @BANNED_SERVERS above, both in how their precedence works, and that
#   they're lists of Perl 5 regular expressions.  See the comments there for
#   details.
@ALLOWED_SCRIPT_SERVERS= () ;
@BANNED_SCRIPT_SERVERS= () ;



# Various options to help filter ads and stop cookie-based privacy invasion.
# These are only effective if $FILTER_ADS is set above.
# @BANNED_IMAGE_URL_PATTERNS uses Perl patterns.  If an image's URL
#   matches one of the patterns, it will not be downloaded (typically for
#   ad-filtering).  For more information on Perl regular expressions, see
#   the Perl documentation.
# Note that most popup ads will be removed if scripts are removed (see
#   $REMOVE_SCRIPTS above).
# If ad-filtering is your primary motive, consider using one of the many
#   proxies that specialize in that.  The classic is from JunkBusters, at
#   http://www.junkbusters.com .

# Reject images whose URL matches any of these patterns.  This is just a
#   sample list; add more depending on which sites you visit.
@BANNED_IMAGE_URL_PATTERNS= (
    'ad\.doubleclick\.net/ad/',
    '\b[a-z](\d+)?\.doubleclick\.net(:\d*)?/',
    '\.imgis\.com\b',
    '\.adforce\.com\b',
    '\.avenuea\.com\b',
    '\.go\.com(:\d*)?/ad/',
    '\.eimg\.com\b',
    '\bexcite\.netscape\.com(:\d*)?/.*/promo/',
    '/excitenetscapepromos/',
    '\.yimg\.com(:\d*)?.*/promo/',
    '\bus\.yimg\.com/[a-z]/(\w\w)/\1',
    '\bus\.yimg\.com/[a-z]/\d-/',
    '\bpromotions\.yahoo\.com(:\d*)?/promotions/',
    '\bcnn\.com(:\d*)?/ads/',
    'ads\.msn\.com\b',
    '\blinkexchange\.com\b',
    '\badknowledge\.com\b',
    '/SmartBanner/',
    '\bdeja\.com/ads/',
    '\bimage\.pathfinder\.com/sponsors',
    'ads\.tripod\.com',
    'ar\.atwola\.com/image/',
    '\brealcities\.com/ads/',
    '\bnytimes\.com/ad[sx]/',
    '\busatoday\.com/sponsors/',
    '\busatoday\.com/RealMedia/ads/',
    '\bmsads\.net/ads/',
    '\bmediaplex\.com/ads/',
    '\batdmt\.com/[a-z]/',
    '\bview\.atdmt\.com/',
    '\bADSAdClient31\.dll\b',
    ) ;

# If set, replace banned images with 1x1 transparent GIF.  This also replaces
#   all images with the same if $TEXT_ONLY is set.
$RETURN_EMPTY_GIF= 1 ;



# To use an external program to decide whether or not a user at a given IP
#   address may use this proxy (as opposed to using server configuration), set
#   $USER_IP_ADDRESS_TEST to either the name of a command-line program that
#   performs this test, or a queryable URL that performs this test (e.g. a CGI
#   script).
# For a command-line program:  The program should take a single argument, the
#   IP address of the user.  The output of the program is evaluated as a
#   number, and if the number is non-zero then the IP address of the user is
#   allowed; thus, the output is typically either "1" or "0".  Note that
#   depending on $ENV{PATH}, you may need to enter the path here explicitly.
# For a queryable URL:  Specify the start of the URL here (must begin with
#   "http://"), and the user's IP address will be appended.  For example, the
#   value here may contain a "?", thus putting the IP address in the
#   QUERY_STRING; it could also be in PATH_INFO.  The response body from the
#   URL should be a number like for a command line program, above.
$USER_IP_ADDRESS_TEST= '' ;


# To use an external program to decide whether or not a destination server is
#   allowed (as opposed to using @ALLOWED_SERVERS and @BANNED_SERVERS above),
#   set $DESTINATION_SERVER_TEST to either the name of a command-line program
#   that performs this test, or a queryable URL that performs this test (e.g. a
#   CGI script).
# For a command-line program: The program should take a single argument, the
#   destination server's name or IP address (depending on how the user enters
#   it).  The output of the program is evaluated as a number, and if the number
#   is non-zero then the destination server is allowed; thus, the output is
#   typically either "1" or "0".  Note that depending on $ENV{PATH}, you may
#   need to enter the path here explicitly.
# For a queryable URL: Specify the start of the URL here (must begin with
#   "http://"), and the destination server's name or IP address will be
#   appended.  For example, the value here may contain a "?", thus putting the
#   name or address in the QUERY_STRING; it could also be in PATH_INFO.  The
#   response body from the URL should be a number like for a command line
#   program, above.
$DESTINATION_SERVER_TEST= '' ;



# If either $INSERT_HTML or $INSERT_FILE is set, then that HTML text or the
#   contents of that named file (respectively) will be inserted into any HTML
#   page retrieved through this proxy.  $INSERT_HTML takes precedence over
#   $INSERT_FILE.
# When viewing a page with frames, a new top frame is created and the
#   insertions go there.
# NOTE:  Any HTML you insert should not have relative URLs in it!  The problem
#   is that there is no appropriate base URL to resolve them with.  So only use
#   absolute URLs in your insertion.  (If you use relative URLs anyway, then
#   a) if $ANONYMIZE_INSERTION is set, they'll be resolved relative to this
#   script's URL, which isn't great, or b) if $ANONYMIZE_INSERTION==0,
#   they'll be unchanged and the browser will simply resolve them relative
#   to the current page, which is usually worse.)
# The frame handling means that it's fairly easy for a surfer to bypass this
#   insertion, by pretending in effect to be in a frame.  There's not much we
#   can do about that, since a page is retrieved the same way regardless of
#   whether it's in a frame.  This script uses a parameter in the URL to
#   communicate to itself between calls, but the user can merely change that
#   URL to make the script think it's retrieving a page for a frame.  Also,
#   many browsers let the user expand a frame's contents into a full window.
# [The warning in earlier versions about setting $INSERT_HTML to '' when using
#   mod_perl and $INSERT_FILE no longer applies.  It's all handled elsewhere.]
# As with $INSERT_ENTRY_FORM, note that any insertion may throw off any
#   precise layout, and the insertion is subject to background colors and
#   other page-wide settings.

#$INSERT_HTML= "<h1>This is an inserted header</h1><hr>" ;
#$INSERT_FILE= 'insert_file_name' ;


# If your insertion has links that you want anonymized along with the rest
#   of the downloaded HTML, then set this to 1.  Otherwise leave it at 0.
$ANONYMIZE_INSERTION= 0 ;

# If there's both a URL entry form and an insertion via $INSERT_HTML or
#   $INSERT_FILE on the same page, the entry form normally goes at the top.
#   Set this to put it after the other insertion.
$FORM_AFTER_INSERTION= 0 ;


# If the insertion is put in a top frame, then this is how many pixels high
#   the frame is.  If the default of 80 or 50 pixels is too big or too small
#   for your insertion, change this.  You can use percentage of screen height
#   if you prefer, e.g. "20%".  (Unfortunately, you can't just tell the
#   browser to "make it as high as it needs to be", but at least the frame
#   will be resizable by the user.)
# This affects insertions by $INSERT_ENTRY_FORM, $INSERT_HTML, and $INSERT_FILE.
# The default here usually works for the inserted entry form, which varies in
#   size depending on $ALLOW_USER_CONFIG.  It also varies by browser.
$INSERTION_FRAME_HEIGHT= $ALLOW_USER_CONFIG   ? 80   : 50 ;



# Set this to 1 if the script is running on an SSL server, i.e. it is
#   accessed through a URL starting with "https:"; set this to 0 if it's not
#   running on an SSL server.  This is needed to know how to route URLs back
#   through the proxy.  Regrettably, standard CGI does not yet provide a way
#   for scripts to determine this without help.
# If this variable is set to '' or left undefined, then the program will
#   guess:  SSL is assumed if and only if SERVER_PORT is 443.  This fails
#   if SSL is used on another port, or (less commonly) a non-SSL server uses
#   port 443, but usually it works.  Besides being a good default, it lets
#   you install the script where both a secure server and a non-secure server
#   will serve it, and it will work correctly through either server.
# This has nothing to do with retrieving pages that are on SSL servers.
$RUNNING_ON_SSL_SERVER= '' ;


# If your server doesn't support NPH scripts, then set this variable to true
#   and try running the script as a normal non-NPH script.  HOWEVER, this
#   won't work as well as running it as NPH; there may be bugs, maybe some
#   privacy holes, and results may not be consistent.  It's a hack.
# Try to install the script as NPH before you use this option, because
#   this may not work.  NPH is supported on almost all servers, and it's
#   usually very easy to install a script as NPH (on Apache, for example,
#   you just need to name the script something starting with "nph-").
# One example of a problem is that Location: headers may get messed up,
#   because they mean different things in an NPH and a non-NPH script.
#   You have been warned.
# For this to work, your server MUST support the "Status:" CGI response
#   header.
$NOT_RUNNING_AS_NPH= 0 ;


# Set HTTP and SSL proxies if needed.  Also see $USE_PASSIVE_FTP_MODE below.
# The format of the first two variables is "host:port", with the port being
#   optional. The format of $NO_PROXY is a comma-separated list of hostnames
#   or domains:  any request for a hostname that ends in one of the strings in
#   $NO_PROXY will not use the HTTP or SSL proxy; e.g. use ".mycompany.com" to
#   avoid using the proxies to access any host in the mycompany.com domain.
# The environment variables in the examples below are appropriate defaults,
#   if they are available.  Note that earlier versions of this script used
#   the environment variables directly, instead of the $HTTP_PROXY and
#   $NO_PROXY variables we use now.
# Sometimes you can use the same proxy (like Squid) for both SSL and normal
#   HTTP, in which case $HTTP_PROXY and $SSL_PROXY will be the same.
# $NO_PROXY applies to both SSL and normal HTTP proxying, which is usually
#   appropriate.  If there's demand to differentiate those, it wouldn't be
#   hard to make a separate $SSL_NO_PROXY option.
#$HTTP_PROXY= $ENV{'http_proxy'} ;
#$SSL_PROXY= 'firewall.example.com:3128' ;
#$NO_PROXY= $ENV{'no_proxy'} ;


# If your HTTP and SSL proxies require authentication, this script supports
#   that in a limited way: you can have a single username/password pair per
#   proxy to authenticate with, regardless of realm.  In other words, multiple
#   realms aren't supported for proxy authentication (though they are for
#   normal server authentication, elsewhere).
# Set $PROXY_AUTH and $SSL_PROXY_AUTH either in the form of "username:password",
#   or to the actual base64 string that gets sent in the Proxy-Authorization:
#   header.  Often the two variables will be the same, when the same proxy is
#   used for both SSL and normal HTTP.
#$PROXY_AUTH= 'Aladdin:open sesame' ;
#$SSL_PROXY_AUTH= $PROXY_AUTH ;


# Here's an experimental feature that may or may not be useful.  It's trivial
#   to add, so I added it.  It was inspired in part by Mike Reiter's and Avi
#   Rubin's "Crowds", at http://www.research.att.com/projects/crowds/ .
#   Let me know if you find a use for it.
# The idea is that you have a number of mutually-trusting, cooperating
#   proxies that you list in @PROXY_GROUP().  If that is set, then instead
#   of rerouting all URLs back through this proxy, the script will choose
#   one of these proxies at random to reroute all URLs through, for each
#   run.  This could be used to balance the load among several proxies, for
#   example.  Under certain conditions it could conceivably help privacy by
#   making it harder to track a user's session, but under certain other
#   conditions it could make it easier, depending on how many people,
#   proxies, and proxy servers are involved.  For each page, both its
#   included images and followed links will go through the same proxy, so a
#   clever target server could determine which proxy servers are in each
#   group.
# proxy_encode() and proxy_decode() must be the same for all proxies in the
#   group.  Same goes for pack_flags() and unpack_flags() if you modified them,
#   and probably certain other routines and configuration options.
# Cookies and Basic authentication can't be supported with this, sorry, since
#   cookies can only be sent back to the proxy that created them.
# Set this to a list of absolute URLs of proxies, ending with "nph-proxy.cgi"
#   (or whatever you named the script).  Be sure to include the URL of this
#   proxy, or it will never redirect back through here.  Each proxy in the
#   group should have the same @PROXY_GROUP.
# Alternately, you could set each proxy's @PROXY_GROUP differently for more
#   creative configuration, such as to balance the load unevenly, or to send
#   users through a "round-robin" cycle of proxies.

#@PROXY_GROUP= ('http://www.example.com/~grommit/proxy/nph-proxy.cgi',
#	        'http://www.fnord.mil/langley/bavaria/atlantis/nph-proxy.cgi',
#	        'http://www.nothinghere.gov/No/Such/Agency/nph-proxy.cgi',
#	        ) ;


# Normally, your browser stores all pages you download in your computer's
#   hard drive and memory, in the "cache".  This saves a lot of time and
#   bandwidth the next time you view the page (especially with images, which
#   are bigger and may be shared among several pages).  However, in some
#   situations you may not want the pages you've visited to be stored.  If
#   $MINIMIZE_CACHING is set, then this proxy will try its best to prevent any
#   caching of anything retrieved through it.
# NOTE:  This cannot guarantee that no caching will happen.  All we can do is
#   instruct the browser not to cache anything.  A faulty or malicious browser
#   could cache things anyway if it chose to.
# NOTE:  This has nothing to do with your browser's "history list", which may
#   also store a list of URLs you've visited.
# NOTE:  If you use this, you will use a lot more bandwidth than without it,
#   and pages will seemingly load slower, because if a browser can't cache
#   anything locally then it has to load everything across the network every
#   time it needs something.
$MINIMIZE_CACHING= 0 ;


# Normally, each cookie includes an expiration time/date, and the cookie stays
#   in effect until then, even after you exit your browser and restart it
#   (which normally means the cookie is stored on the hard drive).  Any cookie
#   that has no explicit expiration date is a "session cookie", and stays in
#   effect only as long as the browser is running, and presumably is forgotten
#   after that.  If you set $SESSION_COOKIES_ONLY=1, then *all* cookies that
#   pass through this proxy will be changed to session cookies.  This is useful
#   at a public terminal, or wherever you don't want your cookies to remain
#   after you exit the browser.
# NOTE:  The clock on the server where this runs must be correct for this
#   option to work right!  It doesn't have to be exact, but don't have it off
#   by hours or anything like that.  The problem is that we must not alter any
#   cookies set to expire in the past, because that's how sites delete cookies.
#   If a cookie is being deleted, we DON'T want to turn it into a session
#   cookie.  So this script will not alter any cookies set to expire before the
#   current time according to the system clock.
$SESSION_COOKIES_ONLY= 0 ;


# Cookies have a URL path associated with them; it determines which URLs on a
#   server will receive the cookie in requests.  If the path is not specified
#   when the cookie is created, then the path is supposed to default to the
#   path of the URL that the cookie was retrieved with, according to the
#   cookie specification from Netscape.  Unfortunately, most browsers seem
#   to ignore the spec and instead give cookies a default path of "/", i.e.
#   "send this cookie with all requests to this server".  So, *sigh*, this
#   script uses "/" as the default path also.  If you want this script to
#   follow the specification instead, then set this variable to true.
$COOKIE_PATH_FOLLOWS_SPEC= 0 ;


# Technically, cookies must have a domain containing at least two dots if the
#   TLD is one of the main non-national TLD's (.com, .net, etc.), and three
#   dots otherwise.  This is to prevent malicious servers from setting cookies
#   for e.g. the entire ".co.uk" domain.  Unfortunately, this prescribed
#   behavior does not accommodate domains like ".google.de".  Thus, browsers
#   seem to not require three dots, and thus, this script will do the same by
#   default.  Set $RESPECT_THREE_DOT_RULE if you want the strictly correct
#   behavior instead.
$RESPECT_THREE_DOT_RULE= 0 ;


# Set $USER_AGENT to something generic like this if you want to be extra
#   careful.  Conceivably, revealing which browser you're using may be a
#   slight privacy or security risk.
# However, note that some URLs serve different pages depending on which
#   browser you're using, so some pages will change if you set this.
# This defaults to the user's HTTP_USER_AGENT.
#$USER_AGENT= 'Mozilla/4.05 [en] (X11; I; Linux 2.0.34 i586)' ;


# FTP transfers can happen in either passive or non-passive mode.  Passive
#   mode works better if the client (this script) is behind a firewall.  Some
#   people consider passive mode to be more secure, too.  But in certain
#   network configurations, if this script has trouble connecting to FTP
#   servers, you can turn this off to try non-passive mode.
# See http://cr.yp.to/ftp/security.html for a discussion of security issues
#   regarding passive and non-passive FTP.
$USE_PASSIVE_FTP_MODE= 1 ;


# Unlike a normal browser which can keep an FTP session open between requests,
#   this script must make a new connection with each request.  Thus, the
#   FTP welcome message (e.g. the README file) will be received every time;
#   there's no way for this script to know if you've been here before.  Set
#   $SHOW_FTP_WELCOME to true to always show the welcome message, or false
#   to never show it.
$SHOW_FTP_WELCOME= 1 ;


# If set, then modify script content (like JavaScript) as well as possible
#   such that network accesses go through this proxy script.  If not set, then
#   allow script content to pass unmodified, assuming it's not being removed.
# Currently, JavaScript is the only script content that's proxified.
# If this is set, and if you modify proxy_encode() and proxy_decode(), then
#   you MUST modify the JavaScript routines in $ENCODE_DECODE_BLOCK_IN_JS also.
# NOTE:  This proxification of script content may not be perfect.  It's pretty
#   good, but it may be possible to construct malicious JavaScript that reveals
#   your identity to the server.  The purpose of this feature is more to allow
#   scripts to function through the proxy, than to provide bulletproof
#   anonymity.
# The best advice remains:  FOR BEST ANONYMITY, BROWSE WITH SCRIPTS TURNED OFF.
$PROXIFY_SCRIPTS= 1 ;


# Though JavaScript is by far the most common kind of script, there are other
#   kinds too, such as Microsoft's VBScript.  This program proxifies JavaScript
#   content, but not other script content, which means those other scripts
#   could open privacy holes.  Thus, the default behavior of this program is
#   to remove those other scripts.  Set this variable to true if you'd rather
#   let those scripts through.
# How this works with $REMOVE_SCRIPTS and the "remove scripts" user checkbox:
#   If $ALLOW_UNPROXIFIED_SCRIPTS is false, then unsupported scripts will
#   always be removed.  If it is true, then it is subject to those other
#   settings, just like supported script types are.
$ALLOW_UNPROXIFIED_SCRIPTS= 0 ;


# Comments may contain HTML in them, which shouldn't be rendered but may be
#   relevant in some other way.  Set this flag if you want the contents of
#   comments to be proxified like the rest of the page, i.e. proxify URLs,
#   stylesheets, scripts, etc.
$PROXIFY_COMMENTS= 0 ;


# Apparently, some censoring filters search outgoing request URIs, but not
#   POST request bodies.  Set this to make the initial input form submit
#   using POST instead of GET.
$USE_POST_ON_START= 1 ;


# If this is set, then the URL the user enters in the start form or the top
#   form will be encoded by _proxy_jslib_proxy_encode() before it's submitted.
#   This can keep the URL the user visits private.
# Note that if you set this, you need to modify proxy_encode() above (along
#   with proxy_decode() and the two analogous JavaScript routines) if you
#   want the URL to actually be encoded to something non-obvious.
$ENCODE_URL_INPUT= 0 ;


# Apparently, some censoring filters look at titles on HTML pages.  Set this
#   to remove HTML page titles.
# Note that this does NOT remove titles that are generated by script content,
#   since those would have no effect on a filter.
$REMOVE_TITLES= 0 ;


# If set, this option prevents a user from calling the proxy through the
#   proxy itself, i.e. looping.  It's normally a mistake on the user's part,
#   and a waste of resources.
# This isn't foolproof; it just catches the obvious mistakes.  It's probably
#   pretty easy for a malicious user to make the script call itself, or s/he
#   can always use two proxies to call each other in a loop.  This doesn't
#   account for IP addresses or multiple hostnames for the same server.
$NO_BROWSE_THROUGH_SELF= 0 ;


# Set this to leave out the "Restart" link at the bottom of error pages, etc.
# In some situations this could make it harder for search engines to find the
#   start page.
$NO_LINK_TO_START= 0 ;


# For the obscure case when a POST must be repeated because of user
#   authentication, this is the max size of the request body that this
#   script will store locally.  If CONTENT_LENGTH is bigger than this,
#   the body's not saved at all-- the first POST will be correct, but
#   the second will not happen at all (since a partial POST is worse than
#   nothing).
$MAX_REQUEST_SIZE= 4194304 ;  # that's 4 Meg to you and me



# Normally, if a user tries to access a banned server or use an unsupported
#   scheme (protocol), this script will alert the user with a warning page, and
#   either allow the user to click through to the URL unprotected (i.e. without
#   using the proxy), or ban access altogether.  However, in some VPN-like
#   installations, it may more desirable to let users follow links from
#   protected pages (e.g. within an intranet) that lead to unprotected,
#   unproxified pages (e.g. pages outside of the intranet), with no breaks in
#   the browsing experience.  (This example assumes the proxy owner intends it
#   to be used for browsing only the intranet and not the Internet at large.)
#   Set $QUIETLY_EXIT_PROXY_SESSION to skip any warning message and let the
#   user surf directly to unproxified pages from proxified pages.  Note that
#   this somewhat changes the meaning of @ALLOWED_SERVERS and @BANNED_SERVERS--
#   they're not allowed or banned per se, it's just whether this proxy is
#   willing to handle their traffic.  @BANNED_NETWORKS is unaffected, however,
#   since the IP ranges it contains often make no sense outside of the LAN.
# WARNING:  DO *NOT* SET THIS FLAG IF ANONYMITY IS IMPORTANT AT ALL!!!  IT IS
#   NOT MEANT FOR THAT KIND OF INSTALLATION.  IF THIS IS SET, THEN USERS WILL
#   SURF INTO UNPROXIFIED, UNANONYMIZED PAGES WITH NO WARNING, AND THEIR
#   PRIVACY WILL BE COMPROMISED; THEY MAY NOT EVEN NOTICE FOR A LONG TIME.
#   THIS IS EXACTLY WHAT ANONYMIZING PROXIES ARE CREATED TO AVOID.

$QUIETLY_EXIT_PROXY_SESSION= 0 ;



# WARNING:
# EXCEPT UNDER RARE CIRCUMSTANCES, ANY PROXY WHICH HANDLES SSL REQUESTS
#   SHOULD *ONLY* RUN ON AN SSL SERVER!!!  OTHERWISE, YOU'RE RETRIEVING
#   PROTECTED PAGES BUT SENDING THEM BACK TO THE USER UNPROTECTED.  THIS
#   COULD EXPOSE ANY INFORMATION IN THOSE PAGES, OR ANY INFORMATION THE
#   USER SUBMITS TO A SECURE SERVER.  THIS COULD HAVE SERIOUS CONSEQUENCES,
#   EVEN LEGAL CONSEQUENCES.  IT UNDERMINES THE WHOLE PURPOSE OF SECURE
#   SERVERS.
# THE *ONLY* EXCEPTION IS WHEN YOU HAVE *COMPLETE* TRUST OF THE LINK
#   BETWEEN THE BROWSER AND THE SERVER THAT RUNS THE SSL-HANDLING PROXY,
#   SUCH AS ON A CLOSED LAN, OR IF THE PROXY RUNS ON THE SAME MACHINE AS
#   THE BROWSER.
# IF YOU ARE ABSOLUTELY SURE THAT YOU YOU TRUST THE USER-TO-PROXY LINK, YOU
#   CAN OVERRIDE THE AUTOMATIC SECURITY MEASURE BY SETTING THE FLAG BELOW.
#   CONSIDER THE CONSEQUENCES VERY CAREFULLY BEFORE YOU RUN THIS SSL-ACCESSING
#   PROXY ON AN INSECURE SERVER!!!

$OVERRIDE_SECURITY= 0 ;



# Stuff below here you probably shouldn't modify unless you're messing with
#   the code.


# This lists all MIME types that could identify a script, and which will be
#   filtered out as well as possible if removing scripts:  HTTP responses with
#   Content-Type: set to one of these will be nixed, certain HTML which links
#   to one of these types will be removed, style sheets with a type here will
#   be removed, and other odds and ends.
# These are used in matching, so can't contain special regex characters.
# This list is also used for the the $PROXIFY_SCRIPTS function.
# This list contains all script MIME types I know of, but I can't guarantee
#   it's a complete list.  It's largely taken from the examples at
#     http://www.robinlionheart.com/stds/html4/scripts.html
#   That page describes only the first four below as valid.
# The page at ftp://ftp.isi.edu/in-notes/iana/assignments/media-types/media-types
#   lists all media (MIME) types registered with the IANA, but unfortunately
#   many script types (especially proprietary ones) have not registered with
#   them, and that list doesn't specify which types are script content anyway.
@SCRIPT_MIME_TYPES= ('application/x-javascript', 'application/x-ecmascript',
		     'application/x-vbscript',   'application/x-perlscript',
		     'application/javascript',   'application/ecmascript',
		     'text/javascript',  'text/ecmascript', 'text/jscript',
		     'text/livescript',  'text/vbscript',   'text/vbs',
		     'text/perlscript',  'text/tcl',
		     'text/x-scriptlet', 'text/scriptlet',
		     'application/hta',
		    ) ;



# All MIME types in @SCRIPT_MIME_TYPES and @OTHER_TYPES_TO_REGISTER will be
#   "registered".  Registration helps the script remember which MIME type is
#   expected by a page when downloading embedded URLs, e.g. style sheets.  Any
#   MIME types that need special treatment should be listed here if they're not
#   already in @SCRIPT_MIME_TYPES.
# If you write a handler for a new MIME type in proxify_block(), and that type
#   isn't already listed in @SCRIPT_MIME_TYPES, then add it here.
# The Perl code in this program supports up to 64 registered MIME types, but
#   the JS _proxy_jslib_pack_flags() and _proxy_jslib_unpack_flags() routines
#   only support 26.  Thus, fix the JS code if there's ever more than 26 types.
@OTHER_TYPES_TO_REGISTER= ('text/css') ;


# These are MIME types that we *may* try to rewrite in proxify_block(), e.g.
#   to send all URLs back through this script.  If a type isn't on this list,
#   then we know for certain it should be sent back to the user unchanged,
#   which saves time.
# If you write a handler for a new MIME type in proxify_block(), then add the
#   type here.
# NOT all the types here are actually supported at this time!
# text/html is not on this list because currently it's handled specially.
@TYPES_TO_HANDLE= ('text/css',
		   'application/x-javascript', 'application/x-ecmascript',
		   'application/javascript',   'application/ecmascript',
		   'text/javascript',          'text/ecmascript',
		   'text/livescript',          'text/jscript',
		  ) ;


# This is a list of all file extensions that will be disallowed if
#   $TEXT_ONLY is set.  It's an inexact science.  If you want to ban
#   other file extensions, you can add more to this list.  Note that
#   removing extensions from this list won't necessarily allow those
#   files through, since there are other ways $TEXT_ONLY is implemented,
#   such as only allowing MIME types of text/* .
# The format of this list is one long string, with the extensions
#   separated by "|".  This is because the string is actually used as
#   a regular expression.  Don't worry if you don't know what that means.
# Extensions are roughly taken from Netscape's "Helper Preferences" screen
#   (but that was in 1996).  A more complete list might be made from a
#   mime.types file.
$NON_TEXT_EXTENSIONS=
	  'gif|jpeg|jpe|jpg|tiff|tif|png|bmp|xbm'   # images
	. '|mp2|mp3|wav|aif|aiff|au|snd'            # audios
	. '|avi|qt|mov|mpeg|mpg|mpe'                # videos
	. '|gz|Z|exe|gtar|tar|zip|sit|hqx|pdf'      # applications
	. '|ram|rm|ra|swf' ;                        # others


# This is now set directly in footer(), the only place it's used.
# $PROXY_VERSION= '2.1beta10' ;


#--------------------------------------------------------------------------
#   End of normal user configuration.
#   Now, set or adjust all globals that remain constant for all runs.
#--------------------------------------------------------------------------

# First, set various constants.

# These are used in rfc1123_date() and date_is_after().
@MONTH=   qw(Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec) ;
@WEEKDAY= qw(Sun Mon Tue Wed Thu Fri Sat Sun) ;
%UN_MONTH= map { lc($MONTH[$_]), $_ }  0..$#MONTH ;   # look up by month name

# Create the sets of regular expressions we'll need if we proxify scripts.
# So far, the only script type we proxify is JavaScript.
&set_RE_JS  if $PROXIFY_SCRIPTS ;


# Next, make copies of any constant environment variables, and fix as needed.

# SERVER_PORT and SCRIPT_NAME will be constant, and are used in several places.
#   Besides, we need SCRIPT_NAME fixed before setting $THIS_SCRIPT_URL.
# SCRIPT_NAME should have a leading slash, but the old CGI "standard" from
#   NCSA was unclear on that, so some servers didn't give it a leading
#   slash.  Here we ensure it has a leading slash.
# Apache has a bug where SCRIPT_NAME is wrong if the PATH_INFO has "//" in it;
#   it's set to the script name plus all of PATH_INFO up until its final "//".
#   To work around this, truncate SCRIPT_NAME at the first place it matches $0.
#   PATH_INFO is also changed to collapse all multiple slashes into a single
#   slash, which is not worked around here.  This bug should be fixed in
#   Apache 2.0.55 and later.
# Some servers provide $0 as a complete path rather than just the filename,
#   so extract the filename.
$ENV{SCRIPT_NAME}=~ s#^/?#/# ;
if ($ENV{SERVER_SOFTWARE}=~ /^Apache\b/i) {
    my($zero)= $0=~ m#([^/]*)$# ;
    ($ENV{SCRIPT_NAME})= $ENV{SCRIPT_NAME}=~ /^(.*?\Q$zero\E)/ if $zero ne '' ;
}
$ENV_SERVER_PORT= $ENV{SERVER_PORT} ;
$ENV_SCRIPT_NAME= $ENV{SCRIPT_NAME} ;



# Next, adjust config variables as needed, or create any needed constants from
#   them.

# Create @BANNED_NETWORK_ADDRS from @BANNED_NETWORKS.
# No error checking; assumes the proxy owner set @BANNED_NETWORKS correctly.
@BANNED_NETWORK_ADDRS= () ;
for (@BANNED_NETWORKS) {
    push(@BANNED_NETWORK_ADDRS, pack('C*', /(\d+)/g)) ;
}


# For the external tests, create hashes of parsed URLs if the tests are CGI calls.
# Note that the socket names must each be unique!
@{$USER_IP_ADDRESS_TEST_H}{qw(host port path socket open)}=
	(lc($1), ($2 eq '' ? 80 : $2), $3, 'S_USERTEST', 0)
    if ($USER_IP_ADDRESS_TEST=~ m#http://([^/?:]*):?(\d*)(.*)#i) ;
@{$DESTINATION_SERVER_TEST_H}{qw(host port path socket open)}=
	(lc($1), ($2 eq '' ? 80 : $2), $3, 'S_DESTTEST', 0)
    if ($DESTINATION_SERVER_TEST=~ m#http://([^/?:]*):?(\d*)(.*)#i) ;


# If $RUNNING_ON_SSL_SERVER is '', then guess based on SERVER_PORT.
$RUNNING_ON_SSL_SERVER= ($ENV_SERVER_PORT==443) if $RUNNING_ON_SSL_SERVER eq '' ;


# Set this constant based on whether the server is IIS, because we have to
#   test it later for every run to work around a bug in IIS.  A constant here
#   saves time when using mod_perl.
$RUNNING_ON_IIS= ($ENV{'SERVER_SOFTWARE'}=~ /IIS/) ;


# Create @NO_PROXY from $NO_PROXY for efficiency.
@NO_PROXY= split(/\s*,\s*/, $NO_PROXY) ;


# Base64-encode $PROXY_AUTH and $SSL_PROXY_AUTH if they're not encoded already.
$PROXY_AUTH=     &base64($PROXY_AUTH)      if $PROXY_AUTH=~ /:/ ;
$SSL_PROXY_AUTH= &base64($SSL_PROXY_AUTH)  if $SSL_PROXY_AUTH=~ /:/ ;


# Guarantee URLs in @PROXY_GROUP have no trailing slash.
foreach (@PROXY_GROUP) { s#/$## }


# Create $NO_CACHE_HEADERS depending on $MINIMIZE_CACHING setting; it is placed
#   in every response.  Note that in all the "here documents" we use for error
#   messages, it has to go on the same line as another header to avoid a blank
#   line in the response.
$NO_CACHE_HEADERS= $MINIMIZE_CACHING
    ? "Cache-Control: no-cache\015\012Pragma: no-cache\015\012"
    : '' ;


# Canonicalize all MIME types to lowercase.
for (@SCRIPT_MIME_TYPES)        { $_= lc }
for (@OTHER_TYPES_TO_REGISTER)  { $_= lc }

# Create @ALL_TYPES and %MIME_TYPE_ID, which are inverses of each other.
# This is useful e.g. to identify the MIME type expected in a given download,
#   in a one-character flag.  That's why we limit this to 64 types for now.
# $ALL_TYPES[0] is '', so we can test e.g. "if $MIME_TYPE_ID{$id} ..." .

@ALL_TYPES= ('', @SCRIPT_MIME_TYPES, @OTHER_TYPES_TO_REGISTER) ;
&HTMLdie("Too many MIME types to register.")  if @ALL_TYPES > 64 ;
@MIME_TYPE_ID{@ALL_TYPES}=  0..$#ALL_TYPES ;


# Regex that matches a script MIME type.
$SCRIPT_TYPE_REGEX= '(' . join("|", @SCRIPT_MIME_TYPES) . ')' ;

# Regex that tells us whether we handle a given MIME type.
$TYPES_TO_HANDLE_REGEX= '(' . join("|", @TYPES_TO_HANDLE) . ')' ;


# Set $THIS_HOST to the best guess how this script was called-- use the
#   Host: request header if available; otherwise, use SERVER_NAME.
# We don't bother with a $THIS_PORT, since it's more reliably set to the port
#   through which the script was called.  SERVER_NAME is much more likely to
#   be different from the hostname that the user sees, since one server may
#   handle many domains or have many hostnames.
if ($ENV{'HTTP_HOST'} ne '') {
    ($THIS_HOST)= $ENV{'HTTP_HOST'}=~ m#^(?:[\w+.-]+://)?([^:/?]*)# ;
    $THIS_HOST= $ENV{'SERVER_NAME'}   if $THIS_HOST eq '' ;
} else {
    $THIS_HOST= $ENV{'SERVER_NAME'} ;
}


# Build the constant $THIS_SCRIPT_URL from environment variables.  Only include
#   SERVER_PORT if it's not 80 (or 443 for SSL).
$THIS_SCRIPT_URL= $RUNNING_ON_SSL_SERVER
	    ? 'https://' . $THIS_HOST
	      . ($ENV_SERVER_PORT==443  ? ''  : ':' . $ENV_SERVER_PORT)
	      . $ENV_SCRIPT_NAME
	    : 'http://' . $THIS_HOST
	      . ($ENV_SERVER_PORT==80   ? ''  : ':' . $ENV_SERVER_PORT)
	      . $ENV_SCRIPT_NAME ;



# End of initialization of constants.
$HAS_BEGUN= 1 ;
}  # unless ($HAS_BEGUN)

#--------------------------------------------------------------------------
#   Global constants are now set.  Now do any initialization that is
#     required for every run.
#--------------------------------------------------------------------------

# OK, let's time this thing
#$starttime= time ;
#my($sutime,$sstime)= (times)[0,1] ;


# This is needed to run an NPH script under mod_perl.
# Other stuff needed for mod_perl:
#   must use at least Perl 5.004, or STDIN and STDOUT won't behave correctly;
#   cannot use exit();
#   must initialize or reset all vars;
#   regex's with /o option retain state between calls, so be careful;
#   typeglobbing of *STDIN doesn't work, so must pass filehandles as strings.
local($|)= 1 ;

# In mod_perl, global variables are retained between calls, so they must
#   be initialized correctly.  In this program, (most) UPPER_CASE variables
#   are persistent constants, i.e. they aren't changed after they're 
#   initialized above (in the $HAS_BEGUN block).  We also assume that no
#   lower_case variables are set before here.  It's a little hacky and possibly
#   error-prone if user customizations don't follow these conventions, but it's
#   fast and simple.
# So, if you're using mod_perl and you make changes to this script, don't
#   modify existing UPPER_CASE variables after the $HAS_BEGUN block above,
#   don't set lower_case variables before here, and don't use UPPER_CASE
#   variables for anything that will vary from run to run.
reset 'a-z' ;
$URL= '' ;     # (almost) only uppercase variable that varies from run to run


# Reset global flag $needs_jslib to false (not needed because of reset above).
#$needs_jslib= 0 ;


# Store $now rather than calling time() multiple times.
$now= time ;    # for (@goodmen)


# This script uses whatever version of HTTP the client is using.  So far
#   only 1.0 and 1.1 are supported.
($HTTP_VERSION)= $ENV{'SERVER_PROTOCOL'}=~ m#^HTTP/(\d+\.\d+)#i ;
$HTTP_VERSION= '1.0' unless $HTTP_VERSION=~ /^1\.[01]$/ ;


# Hack to support non-NPH installation-- luckily, the format of a
#   non-NPH response is almost exactly the same as an NPH response.
#   The main difference is the first word in the status line-- something
#   like "HTTP/1.x 200 OK" can be simulated with "Status: 200 OK", as
#   long as the server supports the Status: CGI response header.  So,
#   we set that first word to either "HTTP/1.x" or "Status:", and use
#   it for all responses throughout the script.
# NOTE:  This is not the only difference between an NPH and a non-NPH
#   response.  For example, the Location: header has different semantics
#   between the two types of responses.  This hack is only an approximation
#   that we hope works most of the time.  It's better to install the script
#   as an NPH script if possible (which it almost always is).
# Technically, the HTTP version in the response is supposed to be the highest
#   version supported by the server, even though the rest of the response may
#   be in the format of an earlier version.  Unfortunately, CGI scripts do
#   not have access to that value; it's a hole in the CGI standard.
$HTTP_1_X=  $NOT_RUNNING_AS_NPH   ? 'Status:'   : "HTTP/$HTTP_VERSION" ;


# Fix submitted by Alex Freed:  Under some unidentified conditions,
#   instances of nph-proxy.cgi can hang around for many hours and drag the
#   system.  So until we figure out why that is, here's a 10-minute timeout.
#   Please write me with any insight into this, since I can't reproduce the
#   problem.  Under what conditions, on what systems, does it happen?
# 9-9-1999: One theory is that it's a bug in older Apaches, and is fixed by
#   upgrading to Apache 1.3.6 or better.  Julian Haight reports seeing the
#   same problem with other scripts on Apache 1.3.3, and it cleared up when
#   he upgraded to Apache 1.3.6.  Let me know if you can confirm this.
# alarm() is missing on some systems (such as Windows), so use eval{} to
#   avoid failing when alarm() isn't available.
# As of version 2.1:  We now only do this if we're running on Apache that is
#   earlier than version 1.3.6, to allow large downloads for everyone else.

if ($ENV{'SERVER_SOFTWARE'}=~ m#^Apache/(\d+)\.(\d+)(?:\.(\d+))?#i) {
    if (($1<=>1 or $2<=>3 or $3<=>6) < 0) {
	$SIG{'ALRM'} = \&timeexit ;
	eval { alarm(600) } ;     # use where it works, ignore where it doesn't
    }
}

# Exit upon timeout.  If you wish, add code to clean up and log an error.
sub timeexit { $ENV{'MOD_PERL'}  ? goto EXIT  : exit 1 }


# Fix any environment variables that the server may have set wrong.
# Note that some constant environment variables are copied to variables above,
#   and fixed there.

# The IIS server doesn't set PATH_INFO correctly-- it sets it to the entire
#   request URI, rather than just the part after the script name.  So fix it
#   here if we're running on IIS.  Thanks to Dave Moscovitz for the info!
$ENV{'PATH_INFO'} =~ s/^$ENV_SCRIPT_NAME//   if $RUNNING_ON_IIS ;

# PATH_INFO may or may not be URL-encoded when we get it; it seems to vary
#   by server.  This script assumes it's still encoded.  Thus, if it's not,
#   we need to re-encode it.
# The only time this seems to come up is when spaces are in URLs, correctly
#   represented in the URL as %20 but decoded to " " in PATH_INFO.  Thus,
#   this hack only focuses on space characters.  It's a hack that I'm not at
#   all comfortable with.  :P
# Very yucky business, this encoding thing.
if ($ENV{'PATH_INFO'}=~ / /) {
    $ENV{'PATH_INFO'} =~ s/%/%25/g ;
    $ENV{'PATH_INFO'} =~ s/ /%20/g ;
}


# Copy often-used environment vars into scalars, for efficiency
$env_accept= $ENV{'HTTP_ACCEPT'} || '*/*' ;     # may be modified later


# PATH_INFO consists of a path segment of flags, followed by the encoded
#   target URL.  For example, PATH_INFO might be something like
#   "/010100A/http/www.example.com".  The actual format of the flag segment
#   is defined in the routine pack_flags().
# Thanks to Mike Harding for the idea of using another flag for the
#   $is_in_frame parameter, instead of using two parallel scripts.

# Extract flags and encoded URL from PATH_INFO.
($packed_flags, $encoded_URL)= $ENV{'PATH_INFO'}=~ m#/([^/]*)/?(.*)# ;

# Set all $e_xxx variables ("effective-xxx") and anything else from flag
#   segment of PATH_INFO.  If user config is not allowed or if flag segment
#   is not present, then set $e_xxx variables from hard-coded config variables
#   instead (but still set anything else as needed from PATH_INFO).
if ( $ALLOW_USER_CONFIG && ($packed_flags ne '') ) {
    ($e_remove_cookies, $e_remove_scripts, $e_filter_ads, $e_hide_referer,
     $e_insert_entry_form, $is_in_frame, $expected_type)=
	 &unpack_flags($packed_flags) ;

} else {
    # $is_in_frame is set in any case.  It indicates whether the current
    #   request will be placed in a frame.
    ($e_remove_cookies, $e_remove_scripts, $e_filter_ads, $e_hide_referer,
     $e_insert_entry_form, $is_in_frame, $expected_type)=
	 ($REMOVE_COOKIES, $REMOVE_SCRIPTS, $FILTER_ADS, $HIDE_REFERER,
	  $INSERT_ENTRY_FORM, (&unpack_flags($packed_flags))[5..6] ) ;
}

# Set any other $e_xxx variables not from flag segment [none currently].



# Flags are now set, and $encoded_URL now contains only the encoded target URL.



# Create a one-flag test for whether we're inserting anything into THIS page.
# This must happen after user flags are read, just above.
$doing_insert_here= !$is_in_frame && 
    ( $e_insert_entry_form || ($INSERT_FILE ne '') || ($INSERT_HTML ne '') ) ;


# One user reported problems with binary files on certain other OS's, and
#   this seemed to fix it.  Supposedly, either this or the "binmode S"
#   statements below the newsocketto() calls work, or all; I'm putting all in.
#   Tell me anything new you figure out about this.
binmode STDOUT ;


#--------------------------------------------------------------------------
#    parse URL, make checks, and set various globals
#--------------------------------------------------------------------------

# Calculate $url_start for use later in &full_url() and elsewhere.  It's an
#   integral part of &full_url(), placed here for speed, similar to the
#   variables set in &fix_base_vars.
# $url_start is the first part of every proxified URL.  A complete proxified
#   URL is made by appending &wrap_proxy_encode(URL) (and possibly a #fragment) to
#   $url_start.  $url_start normally consists of the current script's URL
#   (or one from @PROXY_GROUP), plus a flag segment in PATH_INFO, complete
#   with trailing slash.  For example, a complete $url_start might be
#   "http://www.example.com/path/nph-proxy.cgi/010110A/" .
# $url_start_inframe and $url_start_noframe are used to force the frame flag
#   on or off, for example when proxifying a link that causes frames to be
#   entered or exited.  Otherwise, most links inherit the current frame state.
# $script_url is used later for Referer: support, and whenever a temporary
#   copy of $url_start has to be generated.
# In earlier versions of CGIProxy, $url_start was called $this_url, which is
#   really what it was originally.  Its semantics had drifted somewhat since
#   then, so they have been cleaned up, and $url_start is now more descriptive.

# Set $url_start to a random element of @PROXY_GROUP, if that is set.
if (@PROXY_GROUP) {
    # srand is automatically called in Perl 5.004 and later.  It might be
    #   desirable to seed based on the URL, so that multiple requests for
    #   the same URL go through the same proxy, and may thus be cached.
    #srand( unpack('%32L*', $ENV{'PATH_INFO'}) ) ;  # seed with URL+flags
    $script_url= $PROXY_GROUP[ rand(scalar @PROXY_GROUP) ] ;
} else {
    $script_url= $THIS_SCRIPT_URL ;
}

# Create $url_start and any needed variants: "$script_url/flags/"
$url_start_inframe= $script_url . '/' .
    &pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
		$e_hide_referer, $e_insert_entry_form, 1, '') . '/' ;
$url_start_noframe= $script_url . '/' .
    &pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
		$e_hide_referer, $e_insert_entry_form, 0, '') . '/' ;
$url_start=  $is_in_frame   ? $url_start_inframe   : $url_start_noframe ;


# If there's no $encoded_URL, then start a browsing session.
&show_start_form() if $encoded_URL eq '' ;


# Decode the URL.
$URL= &wrap_proxy_decode($encoded_URL) ;


# Set the query string correctly, from either $ENV{QUERY_STRING} or what's
#   already in $URL.
# The query string may exist in either the encoded URL or in the containing
#   URL, as $ENV{QUERY_STRING}.  If the former, then the query string was
#   (definitely?) in a referenced URL, while the latter most likely implies a
#   GET form input.  Either query string is valid, but form input takes
#   precedence-- if $ENV{QUERY_STRING} exists, it should be used over any
#   query string in the encoded URL.
# Note that Netscape does not pass any query string data that is part of the
#   URL in the <form action> attribute, which is probably correct behaviour.
#   For this program to act exactly the same, it would need to strip the
#   query string when updating all <form action> URLs, way below.
# Question:  Is there ever a valid case when both QUERY_STRINGs exist??

$URL=~ s/(\?.*)?$/?$ENV{'QUERY_STRING'}/   if $ENV{'QUERY_STRING'} ne '' ;


# Parse the URL, using a regex modelled from the one in RFC 2396 (URI syntax),
#   appendix B.
# This assumes a hierarchical scheme; it won't work for e.g. mailto:
# "authority" is the combination of host, port, and possibly other info.
# Note that $path here will also contain any query component; it's more like
#   the request URI.
# Note that $URL is guaranteed to be an absolute URL with no "#" fragment,
#   though this does little error-checking.  Note also that the old ";"
#   parameters are now included in the path component.

($scheme, $authority, $path)= ($URL=~ m#^([\w+.-]+)://([^/?]*)(.*)$#i) ;
$scheme= lc($scheme) ;
$path= "/$path" if $path!~ m#^/# ;   # if path is '' or contains only query


# Magic here-- if $URL uses special scheme "x-proxy", immediately call the
#   general-purpose xproxy() routine.
&xproxy($URL) if $scheme eq 'x-proxy' ;


# Set $is_html if $path (minus query) ends in .htm or .html .
# MSIE has a bug (and privacy hole) whereby URLs with QUERY_STRING ending
#   in .htm or .html are mistakenly treated as HTML, and thus could have
#   untranslated links, <script> blocks, etc.  So for those cases, set
#   $is_html=true to make sure we later transform it as necessary.
if ($ENV{'HTTP_USER_AGENT'}=~ /MSIE/) {
    $is_html= 1  if $path=~ /\.html?(\?|$)/i ;
} else {
    $is_html= 1  if $path=~ /^[^?]*\.html?(\?|$)/i ;
}


# Alert the user to unsupported URL, with an intermediate page
&unsupported_warning($URL) unless ($scheme=~ /^(http|https|ftp)$/) ;

# Require a host to be present (for $base_url safety later)
# Testing for a valid hostname is more complex than just /\w/ , but this does
#   what we need here.
&HTMLdie('The target URL cannot contain an empty host name.')
    unless $authority=~ /^\w/ ;


# Parse $authority into $host, $port, and possibly others, depending on
#   which URL scheme is used.
# Since most URL schemes use the simple host:port, make that the default.
#   This may avoid oversight later when other URL schemes are added (though
#   be careful of username/password handling in that block below).
# Note that this does not set $port to a default.  In the interest of
#   encapsulation, the default $port should be set in the routine that
#   implements the protocol (i.e. http_get(), ftp_get(), etc.)

if ($scheme eq 'ftp') {
    # FTP authority can be username:password@host:port, with username,
    #   password, and port all optional.
    # Embedding your username/password in a URL is NOT RECOMMENDED!  Here,
    #   the second clause should almost always be used.
    if ($authority=~ /@/) {
	($username, $password, $host, $port)=
	    $authority=~ /([^:@]*):?([^@]*)@([^:]*):?(.*)/ ;
    } else {
	($username, $password)= ('anonymous', 'not@available.com') ;
	($host, $port)= $authority=~ /^([^:]*):?(.*)$/ ;
    }

# covers HTTP, etc.
} else {
    # Unlikely occurrence of username:password@host:port, but possible.
    #   Implies HTTP Basic authentication.  Not as much a security hole as
    #   doing the same in an FTP URL, above, but still not a great idea.
    if ($authority=~ /@/) {
	($username, $password, $host, $port)=
	    $authority=~ /([^:@]*):?([^@]*)@([^:]*):?(.*)/ ;
    } else {
	($host, $port)= $authority=~ /^([^:]*):?(.*)$/ ;
    }
}

$host= lc($host) ;      # hostnames are case-insensitive
$host=~ s/\.*$//g ;     # removes trailing dots to close a potential exploit


# If so configured, disallow browsing back through the script itself (looping).
# This assumes the script can only be called by an http:// or https:// URL.
# This could check SERVER_NAME in addition to $THIS_HOST, but that might
#   match falsely sometimes.  The way it is should still prevent deep loops.
if ($NO_BROWSE_THROUGH_SELF) {
    # Default $port's not set yet, so hack up an ad hoc version.
    my($port2)=  $port || ( $scheme eq 'https'  ? 443  : 80 ) ;
    &loop_disallowed_die($URL)
	if     ($scheme=~ /^https?/)
	    && ($host=~ /^$THIS_HOST$/i)
	    && ($port2 == $ENV_SERVER_PORT)
	    && ($path=~ /^$ENV_SCRIPT_NAME\b/) ;
}


# Die if the user's IP address is not allowed here.
if ($USER_IP_ADDRESS_TEST) {
    my($ok) ;
    if ($USER_IP_ADDRESS_TEST_H) {
	$ok= &http_get2($USER_IP_ADDRESS_TEST_H,
			$USER_IP_ADDRESS_TEST_H->{path} . $ENV{REMOTE_ADDR}) ;
    } else {
	$ok= `$USER_IP_ADDRESS_TEST $ENV{REMOTE_ADDR}` ;
    }
    &banned_user_die if $ok==0 ;
}


# Die if the target server is not allowed, according to $DESTINATION_SERVER_TEST.
if ($DESTINATION_SERVER_TEST) {
    my($ok) ;
    my($safehost)= $host ;
    if ($DESTINATION_SERVER_TEST_H) {
	$safehost=~ s/(\W)/ '%' . sprintf('%02x', ord($1)) /ge ;
	$ok= &http_get2($DESTINATION_SERVER_TEST_H,
			$DESTINATION_SERVER_TEST_H->{path} . $safehost) ;
    } else {
	$safehost=~ s/\\/\\\\/g ;
	$safehost=~ s/'/\\'/g ;
	$ok= `$DESTINATION_SERVER_TEST '$safehost'` ;
    }
    &banned_server_die($URL) if $ok==0 ;
}



# Die if the target server is not allowed, according to @ALLOWED_SERVERS and @BANNED_SERVERS.
if (@ALLOWED_SERVERS) {
    my($server_is_allowed) ;
    foreach (@ALLOWED_SERVERS) {
	$server_is_allowed= 1, last   if $host=~ /$_/ ;
    }
    &banned_server_die($URL) unless $server_is_allowed ;
}
foreach (@BANNED_SERVERS) {
    &banned_server_die($URL) if $host=~ /$_/ ;
}


# If we're filtering ads, set $images_are_banned_here appropriately.
if ($e_filter_ads) {
    foreach (@BANNED_IMAGE_URL_PATTERNS) {
	$images_are_banned_here= 1, last if $URL=~ /$_/ ;
    }
}


# Set $scripts_are_banned_here appropriately
$scripts_are_banned_here= $e_remove_scripts ;
unless ($scripts_are_banned_here) {
    if (@ALLOWED_SCRIPT_SERVERS) {
	$scripts_are_banned_here= 1 ;
	foreach (@ALLOWED_SCRIPT_SERVERS) {
	    $scripts_are_banned_here= 0, last   if $host=~ /$_/ ;
	}
    }
    unless ($scripts_are_banned_here) {
	foreach (@BANNED_SCRIPT_SERVERS) {
	    $scripts_are_banned_here= 1, last   if $host=~ /$_/ ;
	}
    }
}


# Set $cookies_are_banned_here appropriately
$cookies_are_banned_here= $e_remove_cookies ;
unless ($cookies_are_banned_here) {
    if (@ALLOWED_COOKIE_SERVERS) {
	$cookies_are_banned_here= 1 ;
	foreach (@ALLOWED_COOKIE_SERVERS) {
	    $cookies_are_banned_here= 0, last   if $host=~ /$_/ ;
	}
    }
    unless ($cookies_are_banned_here) {
	foreach (@BANNED_COOKIE_SERVERS) {
	    $cookies_are_banned_here= 1, last   if $host=~ /$_/ ;
	}
    }
}


# Disallow the retrieval if the expected MIME type is banned, because some
#   browsers erroneously give the advisory content-type precedence over
#   everything else.
if ($scripts_are_banned_here && $expected_type ne '') {
    &script_content_die if $expected_type=~ /^$SCRIPT_TYPE_REGEX$/io ;
}

# Exclude non-text if it's not allowed.  Err on the side of allowing too much.
if ($TEXT_ONLY) {
    # First, forbid requests for filenames with non-text-type extensions
    &non_text_die if ($path=~ /\.($NON_TEXT_EXTENSIONS)(;|\?|$)/i) ;

    # Then, filter the "Accept:" header to accept only text
    $env_accept=~ s#\*/\*#text/*#g ;    # not strictly perfect
    $env_accept= join(', ', grep(m#^text/#i, split(/\s*,\s*/, $env_accept)) ) ;
    &non_text_die unless $env_accept ne '' ;
}


# For a potential banner ad, intercept request if it looks like an image is
#   requested, i.e. unless the Accept: header allows either text/... or */... .
if ($images_are_banned_here) {
    &skip_image unless grep(m#^(text|\*)/#i, split(/\s*,\s*/, $env_accept) ) ;
}


# $base_url must be set correctly at any time &full_url() may be called.
#   &fix_base_vars() must be called as well, to set $base_scheme, $base_host,
#   $base_path, and $base_file.
# Unfortunately, the base URL may change over the course of this program.  We
#   will keep it set based on whatever info we have so far, i.e. request URI,
#   then e.g. HTTP response headers, then e.g. <base> tag (which happens to
#   be in the reverse order of the ultimate precedence).
$base_url= $URL ;
&fix_base_vars ;   # must be called whenever $base_url is set


# The next two variables $default_style_type and $default_script_type must be
#   kept up-to-date throughout the run of this program, just like $base_url
#   and its related variables.  They should always be canonicalized to
#   lowercase (MIME types are case-insensitive).
# Note that if these aren't handled carefully, then there could be a privacy
#   hole-- for example, style sheets of a script type could cause execution of
#   script content.

# Any style content ("style" attributes, <style> elements, or external
#   style sheets) that does not have a type defined uses the default style
#   sheet language. That should be specified in a Content-Style-Type: header
#   or equivalent <meta> tag, but if not then the default is text/css.
# This *should* only be needed for style attributes, but if the other two
#   forms of style content erroneously don't specify a type then it could be
#   used for them.
$default_style_type= 'text/css' ;

# Any script content (intrinsic events attributes (i.e. those named "on___")
#   or <script> elements) that does not have a type defined uses the default
#   script language.  That should be specified in a Content-Script-Type: header
#   or equivalent <meta> tag, but if not then the default is
#   application/x-javascript.
# This *should* only be needed for intrinsic event attributes, but if <script>
#   elements erroneously don't specify a type then it could be used for them.
# Unfortunately, MSIE doesn't always recognize "application/x-javascript",
#   even though that's the only strictly correct MIME type for JavaScript (it
#   does recognize the common "text/javascript").  However, $default_script_type
#   is only used to pass to proxify_block() or to compare to a script regex,
#   so we can keep it as the correct "application/x-javascript".
$default_script_type= 'application/x-javascript' ;


# Parse the cookie for real cookies and authentication information.
($cookie_to_server, %auth)=
    &parse_cookie($ENV{'HTTP_COOKIE'}, $path, $host, $scheme) ;


#--------------------------------------------------------------------------
#    Retrieve the resource into $body using the correct scheme,
#      also setting $status, $headers, and $is_html (all globals).
#      $is_html indicates whether the original resource is HTML, not
#      if a generated response is in HTML (e.g. an error message).
#    $response_sent might be set, indicating the response was sent by the
#      subroutine.  This is appropriate for streaming media, for example.
#--------------------------------------------------------------------------

if ($scheme eq 'http') {
    &http_get ;
} elsif ($scheme eq 'https') {
    &http_get ;
} elsif ($scheme eq 'ftp') {
    &ftp_get ;
}

#--------------------------------------------------------------------------
#    Modify entire response to point back through this script
#--------------------------------------------------------------------------

# If the resource is HTML (and not empty), update all URLs in all tags that
#   refer to URLs.  Plus a bunch of other stuff.

if ( $is_html  && ($body ne '') && !$response_sent ) {

    $body= &proxify_html(\$body, 1) ;

    # Change Content-Length header, since we changed the content
    $headers=~ s/^Content-Length:.*\012/
		 'Content-Length: ' . length($body) . "\015\012"/mie ;

}



#--------------------------------------------------------------------------
#    Send response back to user
#--------------------------------------------------------------------------

# If the response has not already been sent, print the status line, headers,
#   and the entire (possibly modified) resource.
# The $response_sent flag was added purely to support streaming media and
#   large files.
if (!$response_sent) {
    if ($ENV{'REQUEST_METHOD'} eq 'HEAD') {
	print $status, $headers ;
    } elsif ($is_html) {
	print $status, $headers, $body ;
	# print $debug ;   # handy for sprinkling checks throughout the code

    } else {
	print $status, $headers, $body ;
    }
}


# Put this back in to run speed trials
#if ($is_html) {
#    # OK, let's time this thing
#    my($eutime,$estime)= (times)[0,1] ;
#    open(LOG,">>proxy.log") ;
#    print LOG "full times: ", $eutime-$sutime, " ", $estime-$sstime,
#        " ", time-$starttime, "  URL: $URL\n" ;
#    close(LOG) ;
#}



EXIT:

# Catch-all-- if any handles are still open, close them here.  Some error
#   handling relies on this happening.  Also cancel existing alarm.
# These are basically for mod_perl, and unneeded if running as a CGI script.
close(S) ;
untie(*S) ;
eval { alarm(0) } ;   # use eval{} to avoid failing where alarm() is missing

exit unless $ENV{'MOD_PERL'} ;    # mod_perl scripts must not exit


#--------------------------------------------------------------------------
#   DONE!!
#--------------------------------------------------------------------------


#--------------------------------------------------------------------------
# proxify_html()-- Modify entire response to point back through this script
#--------------------------------------------------------------------------
#
# NOTE: IT IS IMPORTANT TO DO THIS AS COMPLETELY AS POSSIBLE!  IF A
# USER UNKNOWINGLY GOES TO A PAGE DIRECTLY AND NOT THROUGH THIS PROXY,
# HE/SHE MAY REVEAL HIM/HERSELF IN AN UNINTENDED WAY.
#
#--------------------------------------------------------------------------
# These were notes to myself from testing the speed of different methods.
#   Names like "nph-proxy2" refer to different modifications.  This version,
#   the fastest, was called nph-proxy2b.
#
# If YOU figure out a faster method, please tell me about it!
#
# It would certainly be faster if rewritten in C, because you could very
#   quickly read each character from the input and write it to the output,
#   maintaining state and altering the data stream as needed.  How much
#   faster is unclear, since the regular expression governing the main
#   while() loop below (as of version 2.0) is fairly efficient, and is
#   in effect handled in C by the Perl interpreter.
#
#--------------------------------------------------------------------------
#
# [This version is nph-proxy2b:  Break into @body array, do not use
#   %urlsin to test if tags should be updated, replacement strings
#   use (a|b|c) syntax.]
#
# [%urlsin was an associative array that listed tags and attributes that
#   may contain URLs, declared like:
#  %urlsin= ('a', 'href',
#            'applet', 'codebase',
#            'fig', 'src|imagemap',
#            'form', 'action|script',  ...
#           ) ;
# ]
#
# This is by far the most time-consuming part of the script, the updating
#   of all URLs in an HTML file.
#
# Results of informal speed testing (not what I expected):
#   Breaking into @body array instead of one big string definitely saves
#       significant time-- compare nph-proxy2 to nph-proxy1.  One test
#       showed a time saving of 1/3 to 1/2.
#   Using a %urlsin array does NOT seem to save time, even when only used
#       as boolean test to see if tag might contain URL-- compare nph-proxy2
#       to nph-proxy3, nph-proxy4, and (most similar) nph-proxy5.  Oh well.
#   It seems that reading one tag at a time, converting, and sending it
#       through does NOT save elapsed time over reading all tags before
#       converting, like I thought it would.  Both the CPU and elapsed
#       time are longer for one-tag-at-a-time approach-- compare
#       nph-proxy2 and nph-piper2.
#
#   Results of nph-proxy2 (blocks for each tag) to nph-proxy2b
#   (single "(att1|att2|att3)" style regex) testing:
#       Mixed results, but overall, using a single regex (nph-proxy2b)
#       takes less CPU "user time", about the same "system time", and
#       slightly more elapsed time than nph-proxy2; I don't know why.
#       The elapsed time is slightly more both within the script and at
#       the HTTP client's end.  All differences are less than 10% on
#       average, and nph-proxy2b occasionally shows LESS elapsed time
#       than nph-proxy2.  CPU time is always less for nph-proxy2b than
#       for nph-proxy2.
#   Since the bottleneck of CPU time is tighter than for elapsed time,
#       and since the elapsed-time loss is less than the CPU time gain,
#       let's go with nph-proxy2b (not that it really makes much difference).
#       Besides, the code is easier to read that way.
#
# So basically, breaking into @body array helps, but not much else does.
#
#--------------------------------------------------------------------------
# 8-4-98 JSM: Found a bug in the regex, so changed it.  It more resembles
#   nph-proxy2.cgi now.
#--------------------------------------------------------------------------


# This routine proxifies an entire block of HTML, which may in turn contain
#   scripts, stylesheets, comments, etc. that need to be proxified in their
#   own ways.
# The first parameter is either a scalar to be proxified, a reference to a
#   scalar (fastest for long strings), or a reference to a list of scalars
#   that need to be joined before proxifying.  The second parameter is a
#   flag that indicates whether this is a full page being proxified, or just
#   an HTML fragment (mostly to know whether we can insert something or not).
# The return value is one long scalar, the proxified HTML.
# This routine normally exits immediately if it finds a <frameset> tag.  This is
#   usually appropriate, but it can be avoided by setting the third parameter,
#   such as when calling this from inside a (normal, non-conditional) comment.
#   This flag should be preserved in nested calls when needed.  Rarely relevant.
# Note that since this routine calls full_url(), $url_start and the $base_ vars
#   must be set before calling this.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_proxify_html() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a (highly abbreviated) Perl-to-JavaScript translation of this routine.
#   Also note that parts of this routine are represented by other JavaScript
#   subroutines, namely _proxy_jslib_proxify_comment(),
#   _proxy_jslib_proxify_script_block(), _proxy_jslib_proxify_style_block()
#   _proxy_jslib_proxify_decl_bang(), _proxy_jslib_decl_question, and
#   _proxy_jslib_proxify_element() .

sub proxify_html {
    my($body_ref, $is_full_page, $no_exit_on_frameset)= @_ ;
    my(@out, $start, $comment, $script_block, $style_block, $decl_bang, $decl_question, $tag,
       $body_pos, $html_pos, $head_pos, $first_script_pos, $out_start,
       $has_content, $in_noscript, $in_title, $title, $full_insertion) ;
    my($ua_is_MSIE)= $ENV{'HTTP_USER_AGENT'}=~ /MSIE/ ;   # used in tight loops

    # Allow first parameter to be reference to list of values to be joined.
    $body_ref= \(join('', @$body_ref)) if ref($body_ref) eq 'ARRAY' ;

    # Allow first parameter to be string instead of reference, for convenience.
    $body_ref= \$body_ref unless ref($body_ref) ;

    # Read in the insertion if it will be needed.  Read it now instead of
    #   later, so certain JavaScript that depends on tag counts inside the
    #   insertion can be handled correctly.
    $full_insertion= &full_insertion($URL,0)   if $is_full_page ;


    # Iterate through $body, matching "chunks" as appropriate.  In HTML, the
    #   only sections that are interpreted as other than HTML are comments,
    #   within <script> blocks, within <style> blocks, and SGML declarations.
    #   Thus, match comments, <script> blocks, <style> blocks, and SGML
    #   declarations before matching any simple HTML tag.
    # Note that in a regex, alternatives are matched in order, which we take
    #   advantage of here-- the general case of a tag is matched after all
    #   others.
    # Comment formats have an unfortunate history.  Technically they're
    #   supposed to end on "--\s*>" (actually it's a bit more complicated than
    #   that), but many HTML authors merely end them on ">".  Browsers seem to
    #   first try to end a comment on "--\s*>", but if that's not available
    #   they end a comment on the first ">".  In addition, some browsers don't
    #   allow whitespace between "--" and ">".  Put that together and it's not
    #   easy to know where a browser will end comment processing and restart
    #   HTML processing.  In general, it's usually safer here to treat comment
    #   as HTML than HTML as comment, so here we end comments as early as the
    #   browser might end them: if a future "-->" exists, then that means end
    #   on "--\s*>", but if a future "-->" doesn't exist, then it means end on
    #   ">".  Thus the zero-width lookahead assertions in the regex below.  Not
    #   perfect.  :P  Would be better to tailor actions to browser types.  We
    #   could err towards longer comments for safety rather than shorter, if we
    #   removed comments entirely.
    # For info about non-HTML blocks in an HTML document, see
    #   http://www.w3.org/TR/html40/appendix/notes.html#h-B.3.2
    # Script and style blocks are supposed to end with the first "</" string,
    #   but in fact browsers seem to end those blocks at the actual </script>
    #   or </style> tags.  This is most likely what the HTML author expects
    #   anyway, though it violates the HTML spec.  In this script, we should
    #   over-proxify rather than under-proxify, so we'll end those blocks on
    #   those end tags as browsers (erroneously) do.
    # Worse, Konqueror allows the string "</script>" inside JS literal strings,
    #   i.e. doesn't end the script block on them.  Netscape does end the block
    #   there, and both browsers end style blocks on embedded </style> strings.
    # Because it's a given that we can't anonymize scripts completely, but
    #   we do want to anonymize HTML completely, we'd rather accidentally
    #   treat script content as HTML than the other way around.  So err on
    #   ending the <script> block too soon for some browsers, i.e. end on
    #   the first "</script>" regardless of whether it's in a string.
    #   (We'd end on "</", but *no* browser seems to do that.)
    # Script content can can exist in:  <script> blocks, conditional comments,
    #   intrinsic event attributes ("on___" attributes), script macros, and
    #   the MSIE-specific "dynamic properties".  These can be removed or
    #   proxified, depending on the settings of $scripts_are_banned_here and
    #   $PROXIFY_SCRIPTS.
    # Script content can also exist elsewhere when its MIME type is explicitly
    #   given (for example, in a <style> block); these cases will be handled
    #   (i.e. removed, proxified, or neither) when proxify_block() is called
    #   with those MIME types.


    # second line was: (?:(<!--.*?--\s*> | <!--.*?> )  # order is important
    while ( $$body_ref=~ m{([^<]*)
			   (?:(<!--(?=.*?-->).*?--\s*> | <!--(?!.*?-->).*?> )
			     |(<\s*script\b.*?<\s*/script\b.*?>)
			     |(<\s*style\b.*?<\s*/style\b.*?>)
			     |(<![^>]*>?)
			     |(<\?[^>]*>?)
			     |(<[^>]*>?)
			   )?
			  }sgix )
    {

	# Above regex must be in scalar context to work, so set vars here.
	($start, $comment, $script_block, $style_block, $decl_bang, $decl_question, $tag)=
	    ($1, $2, $3, $4, $5, $6, $7) ;


	# Handle page titles here.  This includes extracting one into $title,
	#   and clearing it if $REMOVE_TITLES is set.  Slightly hacky.
	# We assume the page title is the $start before a </title> end tag.
	#   HTML authors may erroneously put tags in there and mess up our
	#   assumption, but it will only affect display, not privacy.
	# To avoid checking this regex for every tag, stop after a <body> tag
	#   has been found.  This means any erroneous <title> blocks after
	#   that won't be removed.  If this ever matters, we can change it.
	if ($tag && !$body_pos && $tag=~ m#^<\s*/\s*title\b#i) {
	    $start= ''  if $REMOVE_TITLES ;
	    $title= $start ;
	}


	# Pass the text between tags through to the output.
	push(@out, $start) ;

	# Used when there is illegal early script content (see continue block).
	$out_start= @out ;

	# Don't insert anything into a document that has no text content.
	#   Otherwise, <frameset> tags (either hard-coded or written by JS)
	#   will not take effect--- any text content on the page disables
	#   framesets, in tested browsers.  A document with both text content
	#   and <frameset> tags is illegal anyway.
	$has_content||= $start=~ /\S/ unless $in_noscript || $in_title ;


	# Handle $tag match first, because it's the most common (though it's
	#   the last in the regex above).  Other cases to handle comments etc.
	#   follow this huge block, in "elsif {}" blocks.
	# NOTE: IF YOU MODIFY THIS BLOCK, then be sure to review and possibly
	#   modify the corresponding routine _proxy_jslib_proxify_element() in the
	#   JavaScript library, far below in the routine return_jslib().  It is
	#   a (highly abbreviated) Perl-to-JavaScript translation of this block.
	if ($tag) {

	    my($tag_name, $attrs, %attr, $name, $rebuild) ;

	    # Tag and attribute names match ([A-Za-z][\w.:-]*), I believe implied
	    #   by http://www.w3.org/TR/REC-html40/types.html#type-name .
	    ($tag_name, $attrs)= $tag=~ /^<\s*(\/?\s*[A-Za-z][\w.:-]*)\s*([^>]*)/ ;
	    $tag_name=~ s#^/\s*#/# ;
	    $tag_name= lc($tag_name) ;

	    # If scripts are removed, then we might as well display the blocks
	    #   within <noscript>.  Change <noscript> and </noscript> to <div>
	    #   and </div>, since <noscript> acts very close to <div> when it
	    #   is activated.  This preserves element attributes like lang, dir,
	    #   style, etc.
	    # Also use this block to handle $in_noscript and $in_title, which
	    #   are used to set $has_content correctly.
	    if ($tag_name eq 'noscript') {
		$in_noscript++ ;
		if ($scripts_are_banned_here) {
		    $tag=~ s/^<\s*noscript\b/<div/i ;
		    $tag_name= 'div' ;
		    $rebuild= 1 ;
		}
	    } elsif ($tag_name eq '/noscript') {
		$in_noscript-- if $in_noscript>0 ;
		push(@out, '</div>'), next if $scripts_are_banned_here ;
	    } elsif ($tag_name eq 'title') {
		$in_title++ ;
	    } elsif ($tag_name eq '/title') {
		$in_title-- ;
	    }

	    # Remember positions of first <html>, <head>, and <body> tags for
	    #   insertions later.
	    # Must do before big switch below, since a tag with no attributes
	    #   won't get that far.
	    $html_pos= @out+1  if !$html_pos && ($tag_name eq 'html') ;
	    $head_pos= @out+1  if !$head_pos && ($tag_name eq 'head') ;
	    $body_pos= @out+1  if !$body_pos && ($tag_name eq 'body') ;

	    # If it's a frame document, then call return_frame_doc().
	    # Only bother with this if we're doing an insertion.
	    # MUST be careful not to do this when $is_in_frame!  Else will recurse!
	    # This is the only exit point in proxify_html() other than the end.
	    &return_frame_doc(&wrap_proxy_encode($URL), $title)
		if ($tag_name eq 'frameset') && $doing_insert_here  && !$is_in_frame
		   && !$no_exit_on_frameset ;


	    # Pass tag through if it has no attributes, or if it doesn't parse
	    #   above (which would make $attrs undefined).  This includes end tags.
	    push(@out, $tag), next   if ($attrs eq '') ;


	    # Parse attributes into %attr.
	    # Regex below must be in scalar context for /g to work.
	    # In the case of duplicate attributes, browsers tend to use the first.
	    # A hack is here to handle erroneous HTML attributes that contain
	    #   ">" :  if an unclosed string is found in $attrs, then read up
	    #   to the next ">", add that to the tag, and restart the parsing.

	    PARSE_ATTRS: {
#		while ($attrs=~ /([A-Za-z][\w.:-]*)\s*(?:=\s*(?:"([^">]*)"?|'([^'>]*)'?|([^'"][^\s>]*)))?/g ) {
		while ($attrs=~ /([A-Za-z][\w.:-]*)\s*(?:=\s*(?:"([^"]*)"|'([^']*)'|([^'"][^\s>]*)|(['"])))?/g ) {
		    if (defined($5)) {
			# Again, next line only works in scalar context.
			$body=~ /\G([^>]*)(>?)/gc ;
			my($extra, $close)= ($1, $2) ;
			# exit loop if at end of string
			last if ($extra eq '') and ($close eq '') ;
			$attrs.= '>' . $extra ;
			$tag.=   $extra . $close ;
			redo PARSE_ATTRS ;
		    }

		    $name= lc($1) ;
		    $rebuild= 1, next if exists($attr{$name}) ; # duplicate attr
		    $attr{$name}= &HTMLunescape(defined($2) ? $2
					      : defined($3) ? $3
					      : defined($4) ? $4
					      : '' ) ;
		}
	    }


	    # Intrinsic event attributes, aka "(Java-)script attributes", are
	    #   assumed to be all attributes whose names start with "on".  This
	    #   is the case with HTML 4.01 as of late 1999.
	    # Script macros, aka the Netscape-specific "JavaScript entities",
	    #   have the form "&{ script-content };" and may appear within	
	    #   (and thus invoke JavaScript within) *any* HTML attribute.
	    #   Other browsers may emulate Netscape, so we handle these for
	    #   all browsers. 
	    # MSIE-specific "dynamic properties" can be in style attributes.
	    #   I can't find a definitive reference for their syntax (tell me
	    #   if you know of one), so in handling them we err on the safe
	    #   side and e.g. remove all style attributes that contain the
	    #   string "expression(".  I think I've seen "function()" used
	    #   too, so handle those.  But I'm not very familiar with these.
	    # As far as I can tell, the language used for "dynamic properties"
	    #   is text/jscript (Microsoft's JavaScript variant).
	    # There's an obscure problem with Netscape, script macros, and
	    #   character entities:  Netscape doesn't allow a block inside &{};
	    #   to contain character entities, though quoted strings inside
	    #   that block *can* (and sometimes *must*).  This seems to be in
	    #   violation of the HTML and SGML specs, as far as I can tell; see:
	    #     http://www.w3.org/TR/html40/types.html#h-6.14
	    #     http://www.w3.org/TR/html40/appendix/notes.html#h-B.7
	    #     http://www.w3.org/TR/html40/appendix/notes.html#h-B.3.2
	    #   Anyway, this messes with our HTMLunescape()'ing and
	    #   HTMLescape()'ing.  It will only matter when $PROXIFY_SCRIPT
	    #   is on (meaning bulletproof anonymity can't be too critical),
	    #   and rarely at that, so we'll let it slide for now.  We can
	    #   revisit this later if it becomes an issue.
	    # Netscape also allows quotes inside script macros that match the
	    #   outer enclosing quotes, but for now this script doesn't allow
	    #   those.  (I'm not sure Netscape should either.)
	    # Intrinsic event attributes may have character entities in them
	    #   (even though any contained script macros cannot, in Netscape's
	    #   implementation).


	    # If so configured, remove or proxify any script elements in each tag.
	    if ($scripts_are_banned_here) {

		# Remove intrinsic event attributes, which start with "on".
		# Also remove script macros, by removing all attributes with
		#   "&{" in them (which unfortunately removes any attributes
		#   that innocently contain that string too).  Remove the
		#   entire attribute.
		my(@remove_attrs)= grep(/^on/ || $attr{$_}=~ /&{/ , keys %attr) ;
		delete @attr{ @remove_attrs }, $rebuild=1   if @remove_attrs ;

	    } elsif ($PROXIFY_SCRIPTS) {

		# Proxify any script macros first.
		foreach (keys %attr) {
		    $attr{$_}=~ s/&{(.*?)};/
				 '&{' . &proxify_block($1, $default_script_type) . '};'
				 /sge
			&& ($rebuild= 1) ;
		}

		# Then, proxify all intrinsic event attributes.
		# This is imperfect but probably OK-- see notes above regarding
		#   Netscape, script macros, character entities, and our use
		#   of HTMLescape() and HTMLunescape().
		foreach (grep(/^on/, keys %attr)) {
		    $attr{$_}= &proxify_block($attr{$_}, $default_script_type) ;
		    $rebuild= 1 ;
		}
	    }


	    # Proxify style attribute, which could exist in almost any tag.
	    # Handle any MSIE-specific "dynamic properties" here instead of
	    #   above, to avoid extra work on every tag.
	    if (defined($attr{style})) {

		# Remove or proxify any "dynamic properties" in style
		#   attributes.  Only bother if user is using MSIE.
		if ($ua_is_MSIE) {
		    if ($scripts_are_banned_here) {
			delete($attr{style}), $rebuild=1
			    if $attr{style}=~ /(?:expression|function)\s*\(/i ;

		    } elsif ($PROXIFY_SCRIPTS) {
			# Proxify any strings inside "expression()" or "function()".
			$attr{style}=~ s#((?:expression|function)\s*\()([^)]*)#
					 $1 . &proxify_block($2, 'text/jscript')
					#gie
			    && ($rebuild= 1) ;
		    }
		}

		$attr{style}= &proxify_block($attr{style}, $default_style_type), $rebuild=1 ;
	    }



	    # Now, proxify the tag and its attributes based on which tag it is.
	    # This is a complete list of HTML tags/attributes that may include
	    #   a URL, to the best of my knowledge.  This list includes all
	    #   URL-type attributes defined in HTML 4.0 (as of 7-31-98), an
	    #   earlier HTML DTD as of 9-17-96, and any tags documented on
	    #   Michael Hannah's comprehensive HTML reference (as of 9-17-96;
	    #   Sandia has since forced him to remove the page).  The latter
	    #   included non-standard tags found to be used by Netscape or
	    #   Microsoft.
	    # If anyone knows of a well-maintained list of standard and
	    #   non-standard tags/attributes with URLs in them, please let me
	    #   know!!
	    # Tags are roughly in order from most-common to least common, for
	    #   speed.  Beyond that, they're roughly alphabetical.  Also,
	    #   they're roughly grouped as appropriate.  In the future, they
	    #   may be called instead via a hash of function references
	    #   (e.g. "&$do_tag{$tag_name}"), if we determine that the hash
	    #   lookup plus function call is faster than the current string
	    #   comparisons.
	    # We'll only get here for tags containing attributes.
	    # Note that most of these are very rarely used, if ever.  They're
	    #   included for safety, since we don't want an anonymous user
	    #   accidentally revealing themselves because of a non-anonymized
	    #   URL.
	    # Earlier versions of this script used a long regex to extract
	    #   and modify attributes.  Now, tags are fully parsed into
	    #   attributes, which takes a little longer but operates much more
	    #   cleanly and reliably.  The code is easier to work with too.
	    # Denoting which of these are for images/binaries might be helpful,
	    #   if we need more elaborate text-only support.

	    # Notes regarding frame support:
	    # One of the flags in PATH_INFO indicates whether the page will be
	    #   displayed in a frame, so we know whether or not we can insert a
	    #   header.  Most links keep the frame flag of their containing
	    #   page, but some links can change it-- it's set in <frame> tags,
	    #   and it's cleared in various links that exit a framed page.  For
	    #   both cases, we use the full_url_by_frame() routine instead of
	    #   full_url().  (You can think in terms of entering or leaving
	    #   "frame mode".)
	    # The links that set the frame flag are <frame> and <iframe>.  The
	    #   links that can clear it are <a>, <area>, <link>, and <form>,
	    #   when their target attribute is either "_top" or "_blank".  In
	    #   addition, the <base> tag can have a target attribute, which is
	    #   the default target for any of these tags lacking their own.  So
	    #   we maintain a variable $base_unframes that tells us whether the
	    #   current <base> target would make a link exit frames (i.e. it's
	    #   either "_top" or "_blank", or at least that's our best guess).
	    #   We check $base_unframes when handling <a>, <area>, <link>, and
	    #   <form> tags to set the frame flag correctly.
	    # Not all frame exits will be caught.  :(  This is because for any
	    #   given target attribute, we don't know whether it leads to a new
	    #   open window, or another frame in the existing window.  It could
	    #   hypothetically be fixed *somewhat* by maintaining some list of
	    #   which target names identify frames, based on the immediate
	    #   browsing history (i.e. record the frame name when a <frame> tag
	    #   is processed).  This would be rather elaborate, I think.
	    # This only matters in that if a link causes the user to leave
	    #   frames in a way we don't catch, then any HTML insertion may not
	    #   display properly.  This does NOT affect anonymity (whether the
	    #   user is still surfing through the proxy); it ONLY affects the
	    #   display of the inserted HTML.
	    # Apparently, Netscape only checks a matching prefix for _top and
	    #   _blank, i.e. "_topxx" and "_blankxx" act like _top and _blank.
	    #   MSIE works correctly.



		 #####   BEGIN TAG-SPECIFIC PROCESSING   #####



	    # Handle <a> tag, which only entails updating the href attribute,
	    #   but that includes deframing as needed (and *would* include
	    #   embedding the type code in the URL, but see next paragraph).
	    # Browsers are inconsistent in whether a tag's "type" attribute
	    #   takes precedence over actual Content-Type: header, or vice
	    #   versa.  It appears that for <link> tags, the type attribute
	    #   (erroneously) always takes precedence, while for the <a> tag
	    #   the type attribute is apparently ignored.  So to be consistent
	    #   with browsers, we need to IGNORE the expected type code for
	    #   the <a> tag.  In fact, we actually remove the type attribute
	    #   altogether to remove a privacy hole in any browsers that *do*
	    #   use it.  *sigh*  This wouldn't be a problem if <link>'s type
	    #   attribute was handled correctly by browsers, i.e. of lower
	    #   precedence than Content-Type:, but it's not. So the last part
	    #   of http_get() gets hacked a little, which leads to this hack.
	    #   (Another solution would be to add yet another flag into the
	    #   URL, a "linked-from-which-tag" flag.)
	    # Unlike other tags with type attribute, don't remove <a> tag
	    #   if it links to banned content.  It will only be activated
	    #   by user action, not automatically like the others.

	    if ($tag_name eq 'a') {
		# Remove type attribute altogether.
		delete $attr{type}, $rebuild=1   if defined($attr{type});

		if (defined($attr{href})) {

		    # If needed, detect if frame state might change.
		    # Deframe if (target unframes) or (no target and base target unframes)
		    if (   ($base_unframes && !defined($attr{target}))
			 || $attr{target}=~ /^_(top|blank)$/i         )
		    {
			$attr{href}= &full_url_by_frame($attr{href},0), $rebuild=1 ;
		    } else {
			$attr{href}= &full_url($attr{href}), $rebuild=1 ;
		    }


		    # If browsers were to handle all type attributes correctly
		    #   (see notes above), we'd use the block below to insert
		    #   the expected type into the linked-to URL.  Instead we
		    #   use the block above, because it's faster.

		    ## Could require $doing_insert_here here too to save a little
		    ##   time... may not keep frame state right, but wouldn't matter.
		    #my($link_unframe) ;
		    #$link_unframe=  ($base_unframes && !defined($attr{target}))
		    #              || $attr{target}=~ /^_(top|blank)$/i
		    #    if $is_in_frame ;

		    ## Use temporary copy of $url_start to call full_url() normally.
		    ## Only generate new value if is_in_frame flag has changed,
		    ##   or if type flag needs to be changed.
		    ## Verify that $attr{type} is a valid MIME type.
		    #local($url_start)= $url_start ;
		    #if ( ($attr{type} ne '') || $link_unframe ) {
		    #    ($attr{type})= $attr{type}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
		    #        if  defined($attr{type}) && $attr{type}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;
		    #    $url_start= $script_url . '/' .
		    #        &pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
		    #                    $e_hide_referer, $e_insert_entry_form,
		    #                    $link_unframe  ? 0  : $is_in_frame,
		    #                    lc($attr{type}))
		    #        . '/' ;
		    #}

		    #$attr{href}= &full_url($attr{href}), $rebuild=1 ;
		}



	    # Some browsers accept the faulty "<image>" tag instead of "<img>",
	    #   so handle that or else it's a privacy hole.  Changing <image>
	    #   tags to <img> works, plus lets such pages work in all browsers.
	    } elsif ($tag_name eq 'img' or $tag_name eq 'image') {
		$tag_name= 'img',                        $rebuild=1  if $tag_name eq 'image' ;
		$attr{src}=    &full_url($attr{src}),    $rebuild=1  if defined($attr{src}) ;
		$attr{lowsrc}= &full_url($attr{lowsrc}), $rebuild=1  if defined($attr{lowsrc}) ;
		$attr{longdesc}= &full_url($attr{longdesc}), $rebuild=1 if defined($attr{longdesc}) ;
		$attr{usemap}= &full_url($attr{usemap}), $rebuild=1  if defined($attr{usemap}) ;
		$attr{dynsrc}= &full_url($attr{dynsrc}), $rebuild=1  if defined($attr{dynsrc}) ;


	    } elsif ($tag_name eq 'body') {
		$attr{background}= &full_url($attr{background}), $rebuild=1 if defined($attr{background}) ;



	    # <base> has special significance.
	    # The base URL and target in the <base> tag are handled differently
	    #   by different browsers.  Netscape keeps running track of the two
	    #   values: when it finds a <base> tag, it remembers any base URL
	    #   or target, and uses it for subsequent links.  In other words,
	    #   at any point in the document, the base URL and base target that
	    #   are in effect are the ones from the most recent (previous)
	    #   <base href> and <base target> attributes.  Konqueror, however,
	    #   only honors the *final* <base> tag in the document, and uses it
	    #   for all links.  Here we go with Netscape's approach.  If we
	    #   were to do it like Konqueror, we'd scan the document for <base>
	    #   tags before converting any HTML (earlier versions of the script
	    #   did this).
	    # Even if we occasionally e.g. use the wrong base URL in certain
	    #   browsers, it's probably privacy-safe-- URLs will still always
	    #   be absolute and point through the proxy.  We might just access
	    #   the wrong URL.  It's only an obscure possibility, and would
	    #   only happen in faulty HTML anyway (multiple <base> tags aren't
	    #   allowed).
	    # In this script, the base URL and base target are stored in the
	    #   $base_ vars, and in the $base_unframes flag.

	    } elsif ($tag_name eq 'base') {
		# Remember what we need to from this <base> tag.  Only set
		#   $base_url etc. if $attr{href} looks like an absolute URL
		#   (which it always should, but some pages have errors).
		$base_url= $attr{href}, &fix_base_vars
		    if defined($attr{href}) && $attr{href}=~ m#^[\w+.-]+://# ;
		$base_unframes= $attr{target}=~ /^_(top|blank)$/i ;

		# Then convert any href attribute normally.
		$attr{href}= &full_url($attr{href}), $rebuild=1  if defined($attr{href}) ;



	    } elsif ($tag_name eq 'frame') {
		$attr{src}=      &full_url_by_frame($attr{src},1), $rebuild=1 if defined($attr{src}) ;
		$attr{longdesc}= &full_url($attr{longdesc}),       $rebuild=1 if defined($attr{longdesc}) ;

	    } elsif ($tag_name eq 'iframe') {
		$attr{src}=      &full_url_by_frame($attr{src},1), $rebuild=1 if defined($attr{src}) ;
		$attr{longdesc}= &full_url($attr{longdesc}),       $rebuild=1 if defined($attr{longdesc}) ;


	    # <head>'s profile attribute can be a space-separated list of URIs.
	    } elsif ($tag_name eq 'head') {
		$attr{profile}= join(' ', map {&full_url($_)} split(" ", $attr{profile})),
		    $rebuild=1  if defined($attr{profile}) ;

	    } elsif ($tag_name eq 'layer') {
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;



	    } elsif ($tag_name eq 'input') {
		$attr{src}=    &full_url($attr{src}),    $rebuild=1  if defined($attr{src}) ;
		$attr{usemap}= &full_url($attr{usemap}), $rebuild=1  if defined($attr{usemap}) ;


	    # <form> tag needs special attention, here and elsewhere.
	    # is <form script='...'> attribute ever used, or even recognized
	    #    by any browser?  It's not defined in any W3C DTD.
	    } elsif ($tag_name eq 'form') {
		# Deframe if (target unframes) or (no target and base target unframes)
		if (   ($base_unframes && !defined($attr{target}))
		     || $attr{target}=~ /^_(top|blank)$/i         )
		{
		    $attr{action}= &full_url_by_frame($attr{action},0), $rebuild=1 if defined($attr{action}) ;
		} else {
		    $attr{action}= &full_url($attr{action}),            $rebuild=1 if defined($attr{action}) ;
		}

		if ($scripts_are_banned_here) {
		    delete($attr{script}), $rebuild=1 if defined($attr{script}) ;
		} else {
		    $attr{script}= &full_url($attr{script}), $rebuild=1  if defined($attr{script}) ;
		}



	    # The only special handling for <area> is to handle any deframing.
	    } elsif ($tag_name eq 'area') {
		# Deframe if (target unframes) or (no target and base target unframes)
		if (   ($base_unframes && !defined($attr{target}))
		     || $attr{target}=~ /^_(top|blank)$/i         )
		{
		    $attr{href}= &full_url_by_frame($attr{href},0), $rebuild=1  if defined($attr{href}) ;
		} else {
		    $attr{href}= &full_url($attr{href}), $rebuild=1  if defined($attr{href}) ;
		}




	    # Handle <link> tag.  If type attribute exists, include correct
	    #   expected-type flag in updated links for other attributes, e.g.
	    #   to handle external style sheets correctly when downloaded
	    #   later.  Also handle deframing as needed.  Remove <link> tag
	    #   altogether if type is a script type and scripts are banned.
	    #   Note that the type attribute indicates an *advisory* *expected*
	    #   MIME type, not a required type, though some browsers seem to
	    #   treat it erroneously as the ultimate authority.
	    # In Netscape, the Content-Style-Type: header has no effect in the
	    #   interpretation of external style sheets.  This is probably correct.
	    #   Thus, $default_style_type is not used here.
	    # See http://www.w3.org/TR/html40/struct/links.html#edef-LINK  and
	    #     http://www.w3.org/TR/html40/types.html#type-links

	    } elsif ($tag_name eq 'link') {
		# Verify that $attr{type} is a valid MIME type.
		($attr{type})= $attr{type}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
		    if  defined($attr{type}) && $attr{type}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;

		my($type)= lc($attr{type}) ;

		# When a type attribute is not given, some browsers erroneously
		#   use a default type of "text/css" for any <link> tag indicating
		#   a stylesheet, even to the point of overriding a subsequent
		#   Content-Type: header.  So set that default type here if it's
		#   a stylesheet, as indicated by the rel attribute.
		$type= 'text/css' if ($type eq '') && $attr{rel}=~ /\bstylesheet\b/i ;

		# Remove tag if it links to a script type and scripts are banned.
		next if $scripts_are_banned_here && $type=~ /^$SCRIPT_TYPE_REGEX$/io ;

		# Deframe if (target unframes) or (no target and base target unframes)
		my($link_unframe) ;
		$link_unframe=  ($base_unframes && !defined($attr{target}))
			      || $attr{target}=~ /^_(top|blank)$/i
		    if $is_in_frame ;

		# Use temporary copy of $url_start to call full_url() normally.
		# Only generate new value if type flag has changed or we're deframing.
		local($url_start)= $url_start ;
		if ($type ne '') {
		    $url_start= $script_url . '/' .
			&pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
				    $e_hide_referer, $e_insert_entry_form,
				    $link_unframe  ? 0  : $is_in_frame,
				    $type)
			. '/' ;
		} elsif ($link_unframe) {
		    $url_start= $url_start_noframe ;
		}

		$attr{href}= &full_url($attr{href}), $rebuild=1  if defined($attr{href}) ;
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;   # Netscape?
		$attr{urn}=  &full_url($attr{urn}),  $rebuild=1  if defined($attr{urn}) ;




	    # Handle <meta http-equiv> tags like real HTTP headers (though the
	    #   Netscape-only "url" attribute can be handled normally).
	    # Remove http-equiv attribute if content is empty, else may generate
	    #   empty cookie.
	    # Note that nonstandard headers like Link: and URI: may contain
	    #   "<>", which should be correctly escaped and unescaped elsewhere.

	    } elsif ($tag_name eq 'meta') {
		$attr{url}= &full_url($attr{url}), $rebuild=1  if defined($attr{url}) ;   # Netscape

		if (defined($attr{'http-equiv'}) && defined($attr{content})) {
		    $attr{content}= &new_header_value(@attr{'http-equiv', 'content'}) ;
		    delete($attr{'http-equiv'}) unless defined($attr{content}) ;
		    $rebuild= 1 ;
		}





	    # The <param> tag is special-- if its valuetype attribute is "ref",
	    #   then the value attribute is a URI.  Also, in this case it has a
	    #   type attribute which indicates an expected MIME type.
	    # In http://www.w3.org/TR/html40/struct/objects.html#edef-PARAM ,
	    #   we're told not to resolve the value URI; however, not doing so
	    #   could open a privacy hole, normally only when it's an absolute
	    #   URI.  So based on our priorities, we update the value URI here
	    #   iff it's an absolute URI.

	    } elsif ($tag_name eq 'param') {
		if (lc($attr{valuetype}) eq 'ref') {
		    # Verify that $attr{type} is a valid MIME type.
		    ($attr{type})= $attr{type}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
			if  defined($attr{type}) && $attr{type}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;

		    my($type)= lc($attr{type}) ;

		    # Remove tag if it links to a script type and scripts are banned.
		    next if $scripts_are_banned_here && $type=~ /^$SCRIPT_TYPE_REGEX$/io ;

		    # Convert value attribute if needed.
		    if (defined($attr{value}) && ($attr{value}=~ /^[\w.+-]+:/)) {

			# Use a local copy of $url_start to call full_url() normally.
			# Only generate new $url_start if the type flag has changed.
			local($url_start)= $url_start ;
			if ($type ne '') {
			    $url_start= $script_url . '/' .
				&pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
					    $e_hide_referer, $e_insert_entry_form,
					    $is_in_frame, $type)
				. '/' ;
			}
			$attr{value}= &full_url($attr{value}) ;
			$rebuild= 1 ;
		    }
		}





	    # <applet> tags are handled much like <object> tags; see the
	    #   comments in that block to explain this block.  Also see
	    #     http://www.w3.org/TR/html40/struct/objects.html#edef-APPLET
	    # In <applet> tags we must convert the codebase, code, object, and
	    #   archive attributes.
	    # archive here is COMMA-separated list of URI's.
	    # archive, code, and object are all relative to codebase, which
	    #   may be relative to base URL.  Its default is the base URL.
	    #   Note that values of codebase are not supposed to depart from
	    #   dirs and subdirs of the base URL, because of security reasons,
	    #   but some do anyway.  See the above URL for details.
	    # This is untested with real applets.  It *has* been tested to
	    #   ensure that test HTML tags are converted as intended, so any
	    #   applet code that conforms to standards should work.
	    # jsm-- ugh, this seems to fail: some browsers don't handle the
	    #   "code" attribute right if it's an absolute URL.  One
	    #   interpretation of the spec would suggest that maybe setting
	    #   codebase to the nph-proxy.cgi/ path and code to the remainder
	    #   might work... but there may still be problems since the URL
	    #   path doesn't match the class name in the .class file that's
	    #   delivered.

	    } elsif ($tag_name eq 'applet') {
		my($codebase_url)= $attr{codebase} ;

		# Here is where we would guard against codebase leaving the
		#   directory: check for absolute path, absolute URL, or ".." .
		#next if $codebase_url=~ m#^/|^[\w+.-]*:|\.\.# ;

		# if $codebase_url is relative, then make it absolute based on
		#   current $base_ vars.  This is the quick method from full_url().
		# Only do this if $codebase_url is not empty.
		if ($codebase_url ne '') {
		    $codebase_url= 
			  $codebase_url=~ m#^[\w+.-]*:#i ? $codebase_url
			: $codebase_url=~ m#^//#         ? $base_scheme . $codebase_url
			: $codebase_url=~ m#^/#          ? $base_host . $codebase_url
			: $codebase_url=~ m#^\?#         ? $base_file . $codebase_url
			:                                  $base_path . $codebase_url ;
		}

		# codebase must be converted with normal $base_ vars first, but
		#   only after its original value is saved (above).
		$attr{codebase}= &full_url($attr{codebase}), $rebuild=1  if defined($attr{codebase}) ;

		# Use local() copies of $base_ vars, starting with current
		#   values as defaults.
		local($base_url, $base_scheme, $base_host, $base_path, $base_file)=
		    ($base_url, $base_scheme, $base_host, $base_path, $base_file) ;

		# Now set local $base_ vars if needed.
		$base_url= $codebase_url, &fix_base_vars  if $codebase_url ne '' ;

		# These two can now be converted normally, using new $base_ vars.
		$attr{code}=   &full_url($attr{code}),   $rebuild=1  if defined($attr{code}) ;
		$attr{object}= &full_url($attr{object}), $rebuild=1  if defined($attr{object}) ;

		# archive is a comma-separated list of URIs: split, convert, join.
		$attr{archive}= join(',', map {&full_url($_)} split(/\s*,\s*/, $attr{archive})),
		    $rebuild=1  if defined($attr{archive}) ;





	    # <object> tags need special treatment, particularly regarding which
	    #   attributes use which base URIs to resolve them when relative.
	    #   For details, see
	    #     http://www.w3.org/TR/html40/struct/objects.html#edef-OBJECT
	    # The <object> tag has five attributes that may contain URLs to be
	    #   converted:  usemap, codebase, classid, data, archive.  All must
	    #   be converted to absolute URLs, because a browser probably won't
	    #   resolve relative URLs correctly through this proxy.
	    # codebase is used as the base URL for classid, data, and archive;
	    #   it defaults to normal base URI.  In addition, data uses the
	    #   MIME type in "type", and classid uses the MIME type in codetype
	    #   if available, otherwise the one in "type".  Also, all URLs have
	    #   the frame flag set, to avoid inserting a page header into any
	    #   embedded objects (it's legal to embed HTML pages using the
	    #   <object> tag).
	    # Several places here, we use local copies of the $base_ vars and
	    #   $url_start to call full_url() with desired base URLs, types, etc.
	    # There are a few sites with embedded objects or applets that may not
	    #   work if the proxy_encode() routine is not the simple default.  See
	    #   the comments above that routine for details.  It has to do with
	    #   the objects trying to resolve relative URLs.

	    } elsif ($tag_name eq 'object') {
		# Verify that $attr{type} is a valid MIME type.
		($attr{type})= $attr{type}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
		    if  defined($attr{type}) && $attr{type}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;

		# Verify that $attr{codetype} is a valid MIME type.
		($attr{codetype})= $attr{codetype}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
		    if  defined($attr{codetype}) && $attr{codetype}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;

		my($type)=     lc($attr{type}) ;
		my($codetype)= lc($attr{codetype}) ;
		my($codebase_url)= $attr{codebase} ;

		# Remove tag if it links to a script type and scripts are banned.
		# Check both type and codetype.
		next if $scripts_are_banned_here &&
		    (   $type=~     /^$SCRIPT_TYPE_REGEX$/io
		     || $codetype=~ /^$SCRIPT_TYPE_REGEX$/io ) ;

		# if $codebase_url is relative, then make it absolute based on
		#   current $base_ vars.  This is the quick method from full_url().
		# Only do this if $codebase_url is not empty.
		if ($codebase_url ne '') {
		    $codebase_url= 
			  $codebase_url=~ m#^[\w+.-]*:#i ? $codebase_url
			: $codebase_url=~ m#^//#         ? $base_scheme . $codebase_url
			: $codebase_url=~ m#^/#          ? $base_host . $codebase_url
			: $codebase_url=~ m#^\?#          ? $base_file . $codebase_url
			:                                  $base_path . $codebase_url ;
		}

		# usemap is the only attribute converted normally.
		$attr{usemap}= &full_url($attr{usemap}), $rebuild=1  if defined($attr{usemap}) ;

		# codebase must be converted with normal $base_ vars first, but
		#   only after its original value is saved (above).
		$attr{codebase}= &full_url_by_frame($attr{codebase},1), $rebuild=1
		    if defined($attr{codebase}) ;

		# For remaining three attributes, use $base_ vars according to
		#   $codebase_url, without which default to original $base_ vars.
		local($base_url, $base_scheme, $base_host, $base_path, $base_file)=
		    ($base_url, $base_scheme, $base_host, $base_path, $base_file) ;
		$base_url= $codebase_url, &fix_base_vars  if $codebase_url ne '' ;

		# archive is a space-separated list of URIs: split, convert, join.
		# Do this before changing $url_start for data and classid handling.
		$attr{archive}= join(' ', map {&full_url_by_frame($_,1)} split(" ", $attr{archive})),
		    $rebuild=1  if defined($attr{archive}) ;

		# Convert data attribute if needed.
		# Note that $is_in_frame is set to 1 anyway, so go ahead and
		#   generate a new $url_start regardless.
		if (defined($attr{data})) {
		    local($url_start)= $script_url . '/' .
			&pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
				    $e_hide_referer, $e_insert_entry_form, 1, $type)
			. '/' ;
		    $attr{data}= &full_url($attr{data}) ;
		    $rebuild= 1 ;
		}


		# Convert classid attribute if needed.
		# Special case: Don't convert classid if it begins with
		#   "clsid:".  "clsid:" is a non-standard URL scheme that
		#   indicates the "UUID" of an object, and is sometimes used
		#   with embedded objects like Flash, etc.  It is described in
		#   a 1996 draft at http://www.w3.org/Addressing/clsid-scheme .

		if (defined($attr{classid}) && ($attr{classid}!~ /^clsid:/i)) {
		    local($url_start)= $script_url . '/' .
			&pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
				    $e_hide_referer, $e_insert_entry_form, 1,
				    ($codetype ne '')   ? $codetype   : $type )
			. '/' ;
		    $attr{classid}= &full_url($attr{classid}) ;
		    $rebuild= 1 ;
		}





	    # This will likely only be used when called recursively by the
	    #   block below that handles <script>...<script> blocks.

	    } elsif ($tag_name eq 'script') {

		# Probably won't get here, but catch in case one slips through.
		next if $scripts_are_banned_here ;

		# Verify that $attr{type} is a valid MIME type.
		($attr{type})= $attr{type}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
		    if  defined($attr{type}) && $attr{type}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;

		# Netscape apparently trusts expected type here, including the
		#   default to JavaScript, and completely ignores the Content-Type:
		#   header. The expected type first comes from "type" attribute,
		#   else "language" attribute, else default.
		# Konqueror, on the other hand, treats all external scripts as
		#   JavaScript, regardless of either expected type or Content-Type: .
		# Set the expected type in the URL flags, and the resource will be
		#   interpreted appropriately when downloaded in e.g. http_get().

		if (defined($attr{src})) {
		    my($type, $language) ;

		    $type= lc($attr{type}) ;

		    # If there's no type, but there's a language attribute, then
		    #   use that instead to guess the expected type.
		    if (!$type && ($language= $attr{language})) {
			$type= $language=~ /javascript|ecmascript|livescript|jscript/i
							 ? 'application/x-javascript'
			     : $language=~ /css/i        ? 'text/css'
			     : $language=~ /vbscript/i   ? 'application/x-vbscript'
			     : $language=~ /perl/i       ? 'application/x-perlscript'
			     : $language=~ /tcl/i        ? 'text/tcl'
			     :                             ''
		    }
		    $type||= $default_script_type ;

		    # Use a local copy of $url_start to call full_url() normally.
		    # Only generate new $url_start if the type flag has changed.
		    local($url_start)= $url_start ;
		    if ($type) {
			$url_start= $script_url . '/' .
			    &pack_flags($e_remove_cookies, $e_remove_scripts,
					$e_filter_ads, $e_hide_referer,
					$e_insert_entry_form, $is_in_frame, $type)
			    . '/' ;
		    }
		    $attr{src}= &full_url($attr{src}) ;
		    $rebuild= 1 ;

		    # Set $needs_jslib if we're loading an external JavaScript resource.
		    # If $type is empty, then check $default_script_type instead.
		    $needs_jslib= 1
			if ($type || $default_script_type)=~
			    m#^(?:application/x-javascript|application/x-ecmascript|application/javascript|application/ecmascript|text/javascript|text/ecmascript|text/livescript|text/jscript)$#i ;
		}



	    # This will likely only be used when called recursively by the
	    #   block below that handles <style>...<style> blocks.

	    } elsif ($tag_name eq 'style') {
		# Verify that $attr{type} is a valid MIME type.
		($attr{type})= $attr{type}=~ m#^\s*([\w.+\$-]*/[\w.+\$-]*)#, $rebuild=1
		    if  defined($attr{type}) && $attr{type}!~ m#^[\w.+\$-]+/[\w.+\$-]+$# ;





	    # These are seldom-used tags, or tags that seldom have URLs in them

	    } elsif ($tag_name eq 'select') {     # HTML 3.0
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;

	    } elsif ($tag_name eq 'hr') {         # HTML 3.0
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;

	    } elsif ($tag_name eq 'td') {         # Netscape extension?
		$attr{background}= &full_url($attr{background}), $rebuild=1 if defined($attr{background}) ;

	    } elsif ($tag_name eq 'th') {         # Netscape extension?
		$attr{background}= &full_url($attr{background}), $rebuild=1 if defined($attr{background}) ;

	    } elsif ($tag_name eq 'tr') {         # Netscape extension?
		$attr{background}= &full_url($attr{background}), $rebuild=1 if defined($attr{background}) ;

	    } elsif ($tag_name eq 'table') {      # Netscape extension?
		$attr{background}= &full_url($attr{background}), $rebuild=1 if defined($attr{background}) ;

	    } elsif ($tag_name eq 'bgsound') {    # Microsoft only
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;

	    } elsif ($tag_name eq 'blockquote') {
		$attr{cite}= &full_url($attr{cite}), $rebuild=1  if defined($attr{cite}) ;

	    } elsif ($tag_name eq 'del') {
		$attr{cite}= &full_url($attr{cite}), $rebuild=1  if defined($attr{cite}) ;

	    } elsif ($tag_name eq 'embed') {      # Netscape only
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;
		$attr{pluginspage}= &full_url($attr{pluginspage}),  $rebuild=1  if defined($attr{pluginspage}) ;

	    } elsif ($tag_name eq 'fig') {        # HTML 3.0
		$attr{src}=      &full_url($attr{src}),      $rebuild=1  if defined($attr{src}) ;
		$attr{imagemap}= &full_url($attr{imagemap}), $rebuild=1  if defined($attr{imagemap}) ;

	    } elsif ($tag_name=~ /^h[1-6]$/) {    # HTML 3.0
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;

	    } elsif ($tag_name eq 'ilayer') {
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;

	    } elsif ($tag_name eq 'ins') {
		$attr{cite}= &full_url($attr{cite}), $rebuild=1  if defined($attr{cite}) ;

	    } elsif ($tag_name eq 'note') {       # HTML 3.0
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;

	    } elsif ($tag_name eq 'overlay') {    # HTML 3.0
		$attr{src}=      &full_url($attr{src}),      $rebuild=1  if defined($attr{src}) ;
		$attr{imagemap}= &full_url($attr{imagemap}), $rebuild=1  if defined($attr{imagemap}) ;

	    } elsif ($tag_name eq 'q') {
		$attr{cite}= &full_url($attr{cite}), $rebuild=1  if defined($attr{cite}) ;

	    } elsif ($tag_name eq 'ul') {         # HTML 3.0
		$attr{src}=  &full_url($attr{src}),  $rebuild=1  if defined($attr{src}) ;



	    }   #####   END OF TAG-SPECIFIC PROCESSING   #####



	    # Rebuild the tag if it has been changed, as fast as possible.
	    # Attributes with value of '' are added without a value, like "selected".
	    # Undefined attributes are removed.
	    # Otherwise, use single quotes only if the values contain double
	    #   quotes and no single quotes, else use double quotes.  This
	    #   handles script-type attributes most cleanly.
	    # This is a bottleneck of the script, done for every rebuilt tag.
	    # The functionality of HTMLescape() is inlined here for speed.

	    if ($rebuild) {
		my($name, $value, $attrs, $end_slash) ;

		while (($name, $value)= each %attr) {
		    next unless defined($value) ;
		    $attrs.= (' ' . $name), next   if $value eq '' ;

		    $value=~ s/&/&amp;/g ;
		    $value=~ s/</&lt;/g ;
		    $value=~ s/>/&gt;/g ;
		    if ($value!~ /"/ || $value=~ /'/) {
			$value=~ s/"/&quot;/g ;  # only needed when using double quotes
			$attrs.= join('', ' ', $name, '="', $value, '"') ;
		    } else {
			$attrs.= join('', ' ', $name, "='", $value, "'") ;
		    }
		}

		$end_slash= $tag=~ m#/\s*>?$#   ? ' /'   : '' ;
		$tag= "<$tag_name$attrs$end_slash>" ;
	    }

	    push(@out, $tag) ;





	# $tag processing done.  Now, handle the other main cases-- comments,
	#   <script> blocks, <style> blocks, and <!...> declarations.


	# Handle comments of both the <!--...--> and <!--...> varieties.
	} elsif ($comment) {

	    # Handle "conditional comments", which begin with "<!--&{" and
	    #   end with "};".  They evaluate the initial expression, and
	    #   depending on that, include or exclude the rest of the comment.
	    if ( $comment=~ /^<!--\s*&{/ ) {

		# Remove the whole conditional comment if scripts are banned.
		next if $scripts_are_banned_here ;  # remove it by not doing push(@out)

		# Otherwise, proxify conditional comments as configured.  Proxify
		#   the HTML content in any case, since it could get rendered.
		my($condition, $contents, $end)=
		    $comment=~ /^<!--\s*&{(.*?)}\s*;(.*?)(--\s*)?>$/s ;
		$condition= &proxify_block($condition, $default_script_type)
		    if $PROXIFY_SCRIPTS ;
		$contents=  &proxify_html(\$contents, 0, $no_exit_on_frameset) ;
		$comment= join('', '<!--&{', $condition, '};', $contents, $end, '>') ;


	    # Handle MSIE's form of "conditional comments", which are cruder and
	    #   use a different syntax-- they are either
	    #     <!--[if IE]>...<![endif]-->     or
	    #     <![if ! IE]>...<![endif]>
	    # We can ignore the second form; they will be handled already.
	    } elsif ( $comment=~ /^<!--\s*\[\s*if/i ) {

		# Proxify the contents of the comment.
		my($start, $contents, $end)=
		    $comment=~ /^(<!--[^>]*?>)(.*?)(<!\s*\[\s*endif[^>]*?>)$/is ;
		$contents=  &proxify_html(\$contents, 0, 1) ;
		$comment= "$start$contents$end" ;


	    # Otherwise, for normal comments, proxify them if so configured.
	    # Note that here, we don't want to exit on a <frameset> tag, so
	    #   set the third parameter to proxify_html().
	    } elsif ($PROXIFY_COMMENTS) {
		my($contents, $end)= $comment=~ /^<!--(.*?)(--\s*)?>$/s ;
		$contents=  &proxify_html(\$contents, 0, 1) ;
		$comment= "<!--$contents$end>" ;
	    }

	    push(@out, $comment) ;




	# Handle <script> blocks, meaning either removal, or proxifying the
	#   <script> tag and/or the script content as needed.
	# NOTE: IF YOU MODIFY THIS BLOCK, then be sure to review and possibly
	#   modify the corresponding routine _proxy_jslib_proxify_script_block() in the
	#   JavaScript library, far below in the routine return_jslib().  It is
	#   a Perl-to-JavaScript translation of this block.
	} elsif ($script_block) {
	    my($tag, $script, $attrs, %attr, $type, $language, $name) ;

	    # Parse the <script> block.
	    ($tag, $script)=
		$script_block=~ m#^(<\s*script\b[^>]*>)(.*)<\s*/script\b.*?>\z#si ;

	    # Proxify <script> tag itself by calling proxify_html() on it.
	    # There is a block in the "if ($tag)" block above that handles
	    #   <script> tags and all relevant attributes in them.  This
	    #   includes fixing the type attribute if needed.
	    $tag= &proxify_html(\$tag, 0) ;

	    # Extract attributes into %attr.
	    # Regex below must be in scalar context for /g to work.
	    ($attrs)= $tag=~ /^<\s*script\b([^>]*)>/ ;
	    while ($attrs=~ /([A-Za-z][\w.:-]*)\s*(?:=\s*(?:"([^">]*)"?|'([^'>]*)'?|([^'"][^\s>]*)))?/g ) {
		$name= lc($1) ;
		next if exists($attr{$name}) ;   # duplicate attr
		$attr{$name}= &HTMLunescape(defined($2) ? $2
					  : defined($3) ? $3
					  : defined($4) ? $4
					  : '' ) ;
	    }

	    # Find script's MIME type: use type attribute if available,
	    #   else guess from language attribute, else use default
	    #   script type (even though it's not legal HTML).  See notes in
	    #   <script>-handling block far above, in "if ($tag)" block.
	    $type= lc($attr{type}) ;
	    if (!$type && ($language= $attr{language})) {
		$type= $language=~ /javascript|ecmascript|livescript|jscript/i
						 ? 'application/x-javascript'
		     : $language=~ /css/i        ? 'text/css'
		     : $language=~ /vbscript/i   ? 'application/x-vbscript'
		     : $language=~ /perl/i       ? 'application/x-perlscript'
		     : $language=~ /tcl/i        ? 'text/tcl'
		     :                             ''
	    }
	    $type||= $default_script_type ;


	    # If JS content (erroneously) contains the string "</script" (e.g. in
	    #   a string literal), then append to it everything up to the next
	    #   "</script".  Repeat as necessary.  Note that this affects
	    #   pos($$body_ref), the only place other than the main loop
	    #   condition to do so.
	    # When appending, use the correct "<\/script" instead of "</script".
	    #   Oddly, browsers (Firefox and Konqueror) seem to allow "</script" in
	    #   a string only when it's in a document.write() statement.  Thus,
	    #   without the "\", browsers would end the <script> on the "</script"
	    #   in our modified JS.
	    # All this requires first parsing the <script> element and calculating
	    #   its MIME type, unfortunately, even if we're just removing scripts.
	    # Currently, we only detect premature "</script" strings for
	    #   JavaScript scripts.
	    if ($type=~ m#^(application/x-javascript|application/x-ecmascript|application/javascript|application/ecmascript|text/javascript|text/ecmascript|text/livescript|text/jscript)$#i) {
		while (&js_script_ends_inside_string(\$script)) {
		    $$body_ref=~ m#\G(.*?)<\s*/script\b.*?>#sgci ;
		    $script.= "<\\/script>" . $1 ;
		}
	    }


	    # If needed, remove script altogether by not doing push(@out).
	    next if $scripts_are_banned_here ;

	    # Proxify the script content if needed.
	    $script= &proxify_block($script, $type) if $PROXIFY_SCRIPTS ;

	    push(@out, $tag, $script, '</script>') ;




	# Handle <style> blocks.
	# NOTE: IF YOU MODIFY THIS BLOCK, then be sure to review and possibly
	#   modify the corresponding routine _proxy_jslib_proxify_style_block() in the
	#   JavaScript library, far below in the routine return_jslib().  It is
	#   a Perl-to-JavaScript translation of this block.
	} elsif ($style_block) {
	    my($tag, $attrs, $stylesheet, $type) ;

	    ($tag, $stylesheet)=
		$style_block=~ m#^(<\s*style\b[^>]*>)(.*?)<\s*/style\b.*?>#si ;

	    # Proxify <style> tag itself by calling proxify_html() on it.
	    # This includes fixing the type attribute if needed.
	    $tag= &proxify_html(\$tag, 0) ;

	    # Abbreviated attribute extraction, only to find $type.
	    # Regex below must be in scalar context for /g to work.
	    ($attrs)= $tag=~ /^<\s*style\b([^>]*)>/ ;
	    while ($attrs=~ /([A-Za-z][\w.:-]*)\s*(?:=\s*(?:"([^">]*)"?|'([^'>]*)'?|([^'"][^\s>]*)))?/g ) {
		$type= lc(&HTMLunescape(defined($2) ? $2
				      : defined($3) ? $3
				      : defined($4) ? $4
				      : '' )), last
		    if lc($1) eq 'type' ;
	    }

	    $type||= $default_style_type ;

	    # Remove stylesheet if it's a script type and scripts are banned.
	    next if $scripts_are_banned_here && $type=~ /^$SCRIPT_TYPE_REGEX$/io ;

	    # Proxify the stylesheet.
	    $stylesheet= &proxify_block($stylesheet, $type) ;

	    push(@out, $tag, $stylesheet, '</style>') ;




	# Handle any <!...> declarations.
	# Declarations can contain URLs, such as for DTD's.  Most legitimate
	#   declarations would be safe if left unconverted, but if we don't
	#   convert URLs then a malicious document could use this mechanism
	#   to break privacy.  Here we use a simple method to handle virtually
	#   all existing cases and close all privacy holes.
	} elsif ($decl_bang) {
	    my($inside, @words, $q, $rebuild) ;
	    ($inside)= $decl_bang=~ /^<!([^>]*)/ ;
	    @words= $inside=~ /\s*("[^">]*"?|'[^'>]*'?|[^'"][^\s>]*)/g ;

	    # Instead of handling all SGML declarations, the quick hack here is
	    #   to convert any "word" in it that looks like an absolute URL.  It
	    #   handles virtually all existing cases well enough, and closes any
	    #   privacy hole regardless of the declaration.
	    foreach (@words) {
		if (m#^["']?[\w+.-]+://#) {
		    if    (/^"/)  { $q= '"' ; s/^"|"$//g }
		    elsif (/^'/)  { $q= "'" ; s/^'|'$//g }
		    else          { $q= '' }

		    $_= $q . &HTMLescape(&full_url(&HTMLunescape($_))) . $q ;
		    $rebuild= 1 ;
		}
	    }

	    $decl_bang= '<!' . join(' ', @words) . '>'   if $rebuild ;

	    push(@out, $decl_bang) ;




	# Handle any <?...?> declarations, such as XML declarations.
	} elsif ($decl_question) {

	    # Nothing needs to be done to these.
	    push(@out, $decl_question) ;




	}  # end of main if comment/script/style/declaration/tag block


    } continue {
	$first_script_pos= $out_start
	    if $needs_jslib && !defined($first_script_pos) ;

    }  # end of main while loop



    #   @out now has proxified HTML


    # Finally, a few things might be inserted into the page, if we're proxifying
    #   a full page and not just an HTML fragment.
    if ($is_full_page) {

	# Inserting anything (even a comment) before initial <!...> or <?...?>
	#   declarations confuses some browsers (like MSIE 6.0), so any
	#   insertion should go after initial declarations.  Thus, find
	#   the point right after any such declarations.
	# Note that comments may be included in an XML prolog, so they're
	#   matched here too.
	my($after_decl, $i) ;
	for ($i= 0; $i<@out; $i++) {
	    next unless $out[$i]=~ /^</ ;
	    $after_decl= $i+1, next if $out[$i]=~ /^<\s*(?:\?|!)/ ;
	    last ;   # if it's any other tag
	}


	# Insert form and/or other header as needed, if we're not in a frame.
	# Insert it right after the <body> tag if available, else right after
	#   the <html> tag, else at the beginning.
	# Don't insert anything if there was no (non-whitespace) content, or
	#   else <frameset> tags won't work.
	splice(@out, ($body_pos || $html_pos || $after_decl), 0, $full_insertion)
	    if $doing_insert_here && $has_content ;


	# If needed, insert <script src="...//scripts/jslib"> element to load
	#   the JavaScript library.  Put it right after the <head> tag if
	#   available, else right after the <html> tag, else at the beginning.
	# Also call _proxy_jslib_pass_vars().  It is a general mechanism to
	#   pass any needed values into the JS library.  As this script changes,
	#   this call to _proxy_jslib_pass_vars() may have new arguments added.
	#   Feel free to add your own arguments as needed to communicate from
	#   this Perl script to the JS library as it runs in the browser.  Be
	#   sure to update the _proxy_jslib_pass_vars() routine in the JS
	#   library, far below.
	# Set the base URL via a parameter to _proxy_jslib_pass_vars().  We
	#   don't track the base URL with every <base> tag like we do in the
	#   main body of this script; we just use the final base URL (which is
	#   like some browsers; see comments in proxify_html() by the <base> tag
	#   section for details).  To set it with every <base> tag would get
	#   messy, because proxify_html() also works on HTML fragments and
	#   doesn't know whether the enclosing page will use JS, and we don't
	#   want to insert unneeded JS.  This only matters for erroneous HTML
	#   anyway, because no more than one <base> tag is allowed; no privacy
	#   holes are opened or anything like that.
	# This won't work with the splice() just above if $body_pos is less than
	#   $head_pos.  That's invalid HTML, but if we ever need to handle it,
	#   then adjust $head_pos with the splice() above.
	# $needs_jslib is set when certain JavaScript is proxified in
	#   proxify_js(), or if an external (JavaScript) script is loaded with
	#   <script src=...>.  Conceivably, an external stylesheet could
	#   require the library too, but not likely.  We can fix that if
	#   needed.  Same for embedded objects that are script types.
	# Some pages might have script content before the <head> block, or
	#   otherwise placed illegally.  For this, we keep track of
	#   $first_script_pos, a slightly messy solution.
	# Brain-dead MSIE doesn't recognize "application/x-javascript", which
	#   is the only strictly correct MIME type for JavaScript.  Thus, we
	#   use the common and MSIE-recognized alternate, "text/javascript".
	if ($PROXIFY_SCRIPTS && $needs_jslib) {
	    my($base_url_jsq, $default_script_type_jsq, $default_style_type_jsq,
	       $p_cookies_are_banned_here, $p_doing_insert_here, $p_session_cookies_only,
	       $p_cookie_path_follows_spec, $p_respect_three_dot_rule, $insert_pos) ;
	    my($jslib_block)= '<script type="text/javascript" src="'
			    . &HTMLescape($url_start . &wrap_proxy_encode('x-proxy://scripts/jslib'))
			    . "\"></script>\n" ;
	    # Create JS double-quoted string of base URL and other vars.
	    ($base_url_jsq=            $base_url           )=~ s/(["\\])/\\$1/g ;
	    ($default_script_type_jsq= $default_script_type)=~ s/(["\\])/\\$1/g ;
	    ($default_style_type_jsq=  $default_style_type )=~ s/(["\\])/\\$1/g ;
	    $p_cookies_are_banned_here=  $cookies_are_banned_here   ? 'true'  : 'false' ;
	    $p_doing_insert_here=        $doing_insert_here         ? 'true'  : 'false' ;
	    $p_session_cookies_only=     $SESSION_COOKIES_ONLY      ? 'true'  : 'false' ;
	    $p_cookie_path_follows_spec= $COOKIE_PATH_FOLLOWS_SPEC  ? 'true'  : 'false' ;
	    $p_respect_three_dot_rule=   $RESPECT_THREE_DOT_RULE    ? 'true'  : 'false' ;
	    $insert_pos= $head_pos || $html_pos || $after_decl ;
	    $insert_pos= $first_script_pos
		if defined($first_script_pos) && $first_script_pos<$insert_pos ;
	    splice(@out, $insert_pos, 0, $jslib_block,
		   qq(<script type="text/javascript">_proxy_jslib_pass_vars("$base_url_jsq",$p_cookies_are_banned_here,$p_doing_insert_here,$p_session_cookies_only,$p_cookie_path_follows_spec,$p_respect_three_dot_rule,"$default_script_type_jsq","$default_style_type_jsq");</script>\n)
		  ) ;
	}

	# Prepend newline if it's after any declarations.
	splice(@out, $after_decl, 0, ($after_decl ? "\n" : ''),
	       "<!-- resource has been modified by proxy -->\n") ;

    }


    return join('', @out) ;

}  # sub proxify_html()



#--------------------------------------------------------------------------


# Returns the full absolute URL to query our script for the given URI
#   reference.  PATH_INFO will include the encoded absolute URL of the target,
#   but the fragment will be appended unencoded so browsers will resolve it
#   correctly.
# This is a major bottleneck for the whole program, so speed is important here.
# Note that the calculations of $url_start, $base_scheme, $base_host, 
#   $base_path, and $base_file throughout the program are an integral part of
#   this routine, placed elsewhere for speed.
# For HTTP, The URL to be encoded should include everything that is sent in
#   the request, including any query, but not any fragment.
# This only returns absolute URLs, though relative URLs would usually suffice.
#   If it matters, we could have a fullrelurl() and fullabsurl(), the latter
#   used for those HTML attributes that require an absolute URL (like <base>).
#
# The ?:?:?: statement resolves relative URLs to absolute URLs, given the
#   $base_{url,scheme,host,path} variables figured earlier.  It does it
#   simply and efficiently, and accurately enough; the full procedure is
#   described in RFC 2396 (URI syntax), section 5.2.
# RFC 2396, section 5 states that there are three types of relative URIs:
#   net_path (beginning with //, rarely used), abs_path (beginning with /),
#   and rel_path, any of which may be followed by a "?query"; the query must
#   be included in the result.  Thus, we only need to examine the start of
#   the relative URL.
# This ?:?:?: statement passes all test cases in RFC 2396 appendix C, except
#   for the following:  It does not reduce . and .. path segments (to do
#   so would take a lot more time), and it assumes $uri_ref has something
#   other than an empty fragment in it, i.e. that the URI is non-empty.
# This only works for hierarchical schemes, like HTTP or FTP.  Conceivably,
#   there's a problem if the base URL uses a non-hierarchical scheme, and
#   the document contains relative URLs.  Absolute URLs will be OK.
# Any HTML-escaping/unescaping should be done outside of this routine, since
#   it is used for any relative->absolute URL conversion, not just HTML.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_full_url() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.

sub full_url {
    my($uri_ref)= @_ ;

    $uri_ref=~ s/^\s+|\s+$//g ;  # remove leading/trailing whitespace

    # For now, prevent redirecting into x-proxy URLs.
    # This slows down the main tag-converting loop by 0-1%.
    return undef if $uri_ref=~ m#^x-proxy:#i ;

    # Handle "javascript:" URLs separately.  "livescript:" is an old synonym.
    if ($uri_ref=~ /^(?:javascript|livescript):/i) {
	return undef if $scripts_are_banned_here ;
	return $uri_ref unless $PROXIFY_SCRIPTS ;
	my($script)= $uri_ref=~ /^(?:javascript|livescript):(.*)$/si ;
	return 'javascript:' . &proxify_block($script, 'application/x-javascript') ;
    }

    # Separate fragment from URI
    my($uri,$frag)= $uri_ref=~ /^([^#]*)(#.*)?/ ;
    return $uri_ref if $uri eq '' ;  # allow bare fragments to pass unchanged

    # Hack here-- some sites (e.g. eBay) create erroneous URLs with linefeeds
    #   in them, which makes the links unusable if they are encoded here.
    #   So, here we strip CR and LF from $uri before proceeding.  :P
    $uri=~ s/[\015\012]//g ;

    # calculate absolute URL based on four possible cases
    my($absurl)=
	    $uri=~ m#^[\w+.-]*:#i   ?  $uri                 # absolute URL
	  : $uri=~ m#^//#           ?  $base_scheme . $uri  # net_path (rare)
	  : $uri=~ m#^/#            ?  $base_host . $uri    # abs_path, rel URL
	  : $uri=~ m#^\?#           ?  $base_file . $uri    # abs_path, rel URL
	  :                            $base_path . $uri ;  # relative path

    return $url_start . &wrap_proxy_encode($absurl) . $frag ;
}


# Identical to full_url(), except second parameter explicitly determines
#   whether we use $url_start_inframe or $url_start_noframe.
# This could be wrapped into the full_url() routine, but I'm guessing it
#   is more efficient to do it this way.  This won't be called often and
#   full_url() is called a lot.
# This uses a little trick with local() that lets us use full_url(), which
#   keeps the routines synchronized and reduces code size.  We set a local
#   version of $url_start, which is used by full_url() because it remains
#   in scope there, but when we exit this routine the scope closes and
#   the old $url_start is restored.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_full_url_by_frame() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.
sub full_url_by_frame {
    my($uri_ref, $is_frame)= @_ ;
    local($url_start)= $is_frame   ? $url_start_inframe  : $url_start_noframe ;
    return &full_url($uri_ref) ;
}


# Set globals $base_url, $base_scheme, $base_host, $base_path, and $base_file,
#   based on value of $base_url.  This must be called whenever $base_url is
#   set, which unfortunately may vary over the course of the program.
# These are an integral part of &full_url(), placed outside of that for speed.
# To specify:
#   $base_scheme is the scheme of the base URL, ending in ":", like "http:".
#   $base_host is the scheme/host/port of the base URL, with no final slash.
#   $base_path is the scheme/host/port/path, through final slash.
#   $base_file is the scheme/host/port/path, *including* file, but not query.
# These are only relevant (and accurate) for hierarchical "/"-using schemes,
#   like HTTP or FTP.
# Any HTML-escaping/unescaping should be done outside of this routine.
sub fix_base_vars {
    $base_url=~ s/\A\s+|\s+\Z//g ;  # remove leading/trailing spaces

    # Guarantee that $base_url has at least a path of '/', inserting before
    #   ?query if needed.
    $base_url=~ s#^([\w+.-]+://[^/?]+)/?#$1/# ;

    ($base_scheme)= $base_url=~ m#^([\w+.-]+:)//# ;
    ($base_host)=   $base_url=~ m#^([\w+.-]+://[^/?]+)# ; # no ending slash
    ($base_path)=   $base_url=~ m#^([^?]*/)# ;            # use greedy matching
    ($base_file)=   $base_url=~ m#^([^?]*)# ;
}


# Because encoding and decoding the URL requires some steps that are not
#   user-configurable, we "purify" the functions proxy_encode() and
#   proxy_decode() and move the extra steps to these wrapper functions.
# Don't encode the URI fragment.
# Note that we encode "?" to "=3f", and similar for "=" itself.  This is to
#   prevent "?" from being in the encoded URL, where it would prematurely
#   terminate PATH_INFO.
# Also, Apache has a bug where it compresses multiple "/" in PATH_INFO.  To
#   work around this, we encode all "//" to "/=2f", which will be unencoded
#   by proxy_decode() as described in the previous paragraph.  Same goes for
#   "%", since Apache has the same problem when "%2f%2f" is in PATH_INFO.
sub wrap_proxy_encode {
    my($URL)= @_ ;

    my($uri,$frag)= $URL=~ /^([^#]*)(#.*)?/ ;

    $uri= &proxy_encode($uri) ;

    # Encode ? so it doesn't prematurely end PATH_INFO.
    $uri=~ s/=/=3d/g ; $uri=~ s/\?/=3f/g ; $uri=~ s/%/=25/g ;
    1 while $uri=~ s#//#/=2f#g ;    # work around Apache PATH_INFO bug

    return $uri . $frag ;
}


sub wrap_proxy_decode {
    my($enc_URL)= @_ ;

    my($uri,$frag)= $enc_URL=~ /^([^#]*)(#.*)?/ ;

    # First, un-encode =xx chars.
    $uri=~ s/=(..)/chr(hex($1))/ge ;

    $uri= &proxy_decode($uri) ;

    return $uri . $frag ;
}



# Given a block of code, convert it to be "proxy-safe", depending on
#   the given content type (language).  Usually that conversion just means
#   updating any URLs in it.
# This is used for style sheets, scripts, etc.
# Preserve correct quotes.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_proxify_block() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.
# ALSO: Depending what you change here, the routine _proxy_jslib_proxify_css()
#   may be affected.
sub proxify_block {
    my($s, $type)= @_ ;

    if ($scripts_are_banned_here) {
	return undef if $type=~ /^$SCRIPT_TYPE_REGEX$/io ;
    }

    if ($type eq 'text/css') {
	# The only URIs in CSS2 are invoked with "url(...)" or "@import".
	#   (Are there any more?)
	# Ugly regex, but gets virtually all real matches and is privacy-safe.
	# Hard part is handling "\"-escaping.  See
	#   http://www.w3.org/TR/REC-CSS2/syndata.html#uri
	# Hopefully we'll use a whole different approach in the new rewrite.

	$s=~ s/url\s*\(\s*(([^)]*\\\))*[^)]*)(\)|$)/
	      'url(' . &css_full_url($1) . ')'     /gie ;

	$s=~ s#\@import\s*("[^"]*"|'[^']*'|(?!url\s*\()[^;\s<]*)#
	       '@import ' . &css_full_url($1)                   #gie ;

	return $s ;


    # JavaScript can be identified by any of these MIME types.  :P  The
    #   "ecma" ones are the standard, the "javascript" and "livescript" ones
    #   refer to Netscape's implementations, and the "jscript" one refers to
    #   Microsoft's implementation.  Until we need to differentiate, let's
    #   treat them all the same here.
    } elsif ($type=~ m#^(application/x-javascript|application/x-ecmascript|application/javascript|application/ecmascript|text/javascript|text/ecmascript|text/livescript|text/jscript)$#i) {

	# Slight hack-- verify $PROXIFY_SCRIPTS is true, since this may be
	#   called even when it's not true (e.g. style sheets of script type).
	return $s unless $PROXIFY_SCRIPTS ;

	return &proxify_js($s, 1) ;


    # For any non-supported script type, either remove it or pass it unchanged.
    } elsif ($type=~ /^$SCRIPT_TYPE_REGEX$/io) {
	return $ALLOW_UNPROXIFIED_SCRIPTS  ? $s  : '' ;


    } else {
	# If we don't understand the type, return the block unchanged.
	# This would be a privacy hole, if we didn't check for script types
	#   when $scripts_are_banned_here above.  If later we want the option
	#   of returning undef for an unknown type, we can add a parameter to
	#   specify that.

	return $s ;

    }

}



# For CSS only:  takes entire contents between parentheses in "url(...)",
#   extracts the URL therein (accounting for quotes, "\"-escaped chars, etc.),
#   and returns the full_url() of that, suitable for placing back inside
#   "url(...)", including all "\"-escaping, quotes, etc.  :P
# Preserve correct quotes, because this may be embedded in a larger quoted
#   context.
# In external style sheets, relative URLs are resolved relative to the style
#   sheet, not the source HTML document.  This makes it easy for us-- no
#   special $base_url handling.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_css_full_url() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   (almost) a Perl-to-JavaScript translation of this routine.
sub css_full_url {
    my($url)= @_ ;
    my($q) ;

    $url=~ s/\s+$// ;       # leading spaces already stripped above
    if    ($url=~ /^"/)  { $q= '"' ; $url=~ s/^"|"$//g }  # strip quotes
    elsif ($url=~ /^'/)  { $q= "'" ; $url=~ s/^'|'$//g }
    $url=~ s/\\(.)/$1/g ;   # "\"-unescape
    $url=~ s/^\s+|\s+$//g ; # finally, strip spaces once more

    $url= &full_url($url) ;

    $url=~ s/([(),\s'"\\])/\\$1/g ;    # put "\"-escaping back in

    return $q . $url . $q ;
}


#--------------------------------------------------------------------------
#    Scheme-specific routines
#--------------------------------------------------------------------------

#
# <scheme>_get: get resource at URL and set globals $status, $headers, $body,
#   and $is_html.  Optionally, set $response_sent to signal that the response
#   has already been sent.  These are all globals for speed, to prevent
#   unneeded copying of huge strings.
#

# http_get: actually supports both GET and POST.  Also, it is used for
#   https:// (SSL) URLs in addition to normal http:// URLs.

sub http_get {
    my($default_port, $portst, $realhost, $realport, $request_uri,
       $realm, $tried_realm, $auth,
       $proxy_auth_header, $content_type,
       $lefttoget, $postblock, @postbody, $body_too_big, $rin,
       $status_code, $footers) ;
    local($/)= "\012" ;

    # Localize filehandles-- safer for when using mod_perl, early exits, etc.
    # But unfortunately, it doesn't work well with tied variables.  :(
    local(*S, *S_PLAIN) ;

    # If using SSL, then verify that we're set up for it.
    if ($scheme eq 'https') {
	eval { require Net::SSLeay } ;  # don't check during compilation
	&no_SSL_warning($URL) if $@ ;

	# Fail if we're being asked to use SSL, and we're not on an SSL server.
	# Do NOT remove this code; instead, see note above where
	#   $OVERRIDE_SECURITY is set.
	&insecure_die  if !$RUNNING_ON_SSL_SERVER && !$OVERRIDE_SECURITY ;
    }


    $default_port= $scheme eq 'https'  ? 443  : 80 ;

    $port= $default_port if $port eq '' ;

    # Some servers don't like default port in a Host: header, so use $portst.
    $portst= ($port==$default_port)  ? ''  : ":$port" ;

    $realhost= $host ;
    $realport= $port ;
    $request_uri= $path ;

    # there must be a smoother way to handle proxies....
    if ($scheme eq 'http' && $HTTP_PROXY) {
	my($dont_proxy) ;
	foreach (@NO_PROXY) {
	    $dont_proxy= 1, last if $host=~ /$_$/i ;
	}
	unless ($dont_proxy) {
	    ($realhost, $realport)=
		$HTTP_PROXY=~ m#^(?:http://)?([^/?:]*):?([^/?]*)#i ;
	    $realport= 80 if $realport eq '' ;
	    $request_uri= $URL ;
	    $proxy_auth_header= "Proxy-Authorization: Basic $PROXY_AUTH\015\012"
	       if $PROXY_AUTH ne '' ;
	}
    }


    #------ Connect socket to host; send request; wait with select() ------

    # To be able to retry on a 401 Unauthorized response, put the whole thing
    #   in a labeled block.  Note that vars have to be reinitialized.
    HTTP_GET: {

	# Open socket(s) as needed, taking into account possible SSL, proxy, etc.
	# Whatever the situation, S will be the socket to handle the plaintext
	#   HTTP exchange (which may be encrypted by a lower level).

	# If using SSL, then open a plain socket S_PLAIN to the server and
	#   create an SSL socket handle S tied to the plain socket, such that
	#   whatever we write to S will be written encrypted to S_PLAIN (and
	#   similar for reads).  If using an SSL proxy, then connect to that
	#   instead and establish an encrypted tunnel to the destination server
	#   using the CONNECT method.
	if ($scheme eq 'https') {
	    my($dont_proxy) ;
	    if ($SSL_PROXY) {
		foreach (@NO_PROXY) {
		    $dont_proxy= 1, last if $host=~ /$_$/i ;
		}
	    }

	    # If using an SSL proxy, then connect to it and use the CONNECT
	    #   method to establish an encrypted tunnel.  The CONNECT method
	    #   is an HTTP extension, documented in RFC 2817.
	    # This block is modelled after code sent in by Grant DeGraw.
	    if ($SSL_PROXY && !$dont_proxy) {
		($realhost, $realport)=
		    $SSL_PROXY=~ m#^(?:http://)?([^/?:]*):?([^/?]*)#i ;
		$realport= 80 if $realport eq '' ;
		&newsocketto('S_PLAIN', $realhost, $realport) ;

		# Send CONNECT request.
		print S_PLAIN "CONNECT $host:$port HTTP/$HTTP_VERSION\015\012",
			      'Host: ', $host, $portst, "\015\012" ;
		print S_PLAIN "Proxy-Authorization: Basic $SSL_PROXY_AUTH\015\012"
		    if $SSL_PROXY_AUTH ne '' ;
		print S_PLAIN "\015\012" ;

		# Wait a minute for the response to start
		vec($rin= '', fileno(S_PLAIN), 1)= 1 ;
		select($rin, undef, undef, 60)
		    || &HTMLdie("No response from SSL proxy") ;

		# Read response to CONNECT.  All we care about is the status
		#   code, but we have to read the whole response.
		my($response, $status_code) ;
		do {
		    $response= '' ;
		    do {
			$response.= $_= <S_PLAIN> ;
		    } until (/^(\015\012|\012)$/) ; #lines end w/ LF or CRLF
		    ($status_code)= $response=~ m#^HTTP/\d+\.\d+\s+(\d+)# ;
		} until $status_code ne '100' ;

		# Any 200-level response is OK; fail otherwise.
		&HTMLdie("SSL proxy error; response was:<p><pre>$response</pre>")
		    unless $status_code=~ /^2/ ;

	    # If not using a proxy, then open a socket directly to the server.
	    } else {
		&newsocketto('S_PLAIN', $realhost, $realport) ;
	    }

	    # Either way, make an SSL socket S tied to the plain socket S_PLAIN.
	    tie(*S, 'SSL_Handle', \*S_PLAIN) ;


	# If not using SSL, then just open a normal socket.  Any proxy is
	#   already set in $realhost and $realport, above.
	} else {
	    &newsocketto('S', $realhost, $realport) ;
	}


	binmode S ;   # see note with "binmode STDOUT", above


	# Send the request.
	# The Host: header is required in HTTP 1.1 requests.  Also include
	#   Accept: and User-Agent: because they affect results.
	# We're anonymously browsing, so don't include the From: header.  The
	#   User-Agent: header is a very teensy privacy risk, but some pages
	#   load differently with different browsers.  Referer: is handled
	#   below, depending on the user option.  The blank Accept-Encoding:
	#   header indicates that we don't support any encoding (like gzip).
	#   Unfortunately, though, at least one server (Boa) chokes on an
	#   empty Accept-Encoding: header, so let's make it a "," here.  That
	#   effectively still means an empty value, according to the rules of
	#   HTTP header values.
	# Ultimately, we may want to check ALL possible request headers-- see
	#   if they're provided in $ENV{HTTP_xxx}, and include them in our
	#   request if appropriate as per the HTTP spec regarding proxies, and
	#   if they don't violate our goals here (e.g. privacy); some may need
	#   to be appropriately modified to pass through this proxy.  Each
	#   request header would have to be considered and handled individually.
	#   That's probably not all necessary, but we can take that approach as
	#   priorities dictate.
	# Note that servers are NOT required to provide request header values
	#   to CGI scripts!  Some do, but it must not be relied on.  Apache does
	#   provide them, and even provides unknown headers-- e.g. a "Foo: bar"
	#   request header will literally set HTTP_FOO to "bar".  (But some
	#   headers are explicitly discouraged from being given to CGI scripts,
	#   such as Authorization:, because that would be a security hole.)

	print S $ENV{'REQUEST_METHOD'}, ' ', $request_uri, " HTTP/$HTTP_VERSION\015\012",
		'Host: ', $host, $portst, "\015\012",    # needed for multi-homed servers
		'Accept: ', $env_accept, "\015\012",     # possibly modified
		'User-Agent: ', $USER_AGENT || $ENV{'HTTP_USER_AGENT'}, "\015\012",
		"Accept-Encoding: ,\015\012",
		"Accept-Language: $ENV{HTTP_ACCEPT_LANGUAGE}\015\012",
		$proxy_auth_header ;                     # empty if not needed


	# Create Referer: header if so configured.
	# Only include Referer: if we successfully remove $script_url+flags from
	#   start of referring URL.  Note that flags may not always be there.
	# If using @PROXY_GROUP, loop through them until one fits.  This could
	#   only be ambiguous if one proxy in @PROXY_GROUP is called through
	#   another proxy in @PROXY_GROUP, which you really shouldn't do anyway.
	if (!$e_hide_referer) {
	    my($referer)= $ENV{'HTTP_REFERER'} ;
	    if (@PROXY_GROUP) {
		foreach (@PROXY_GROUP) {
		    print(S 'Referer: ', &wrap_proxy_decode($referer), "\015\012"), last
			if  $referer=~ s#^$_(/[^/]*/?)?##  &&  ($referer ne '') ;
		    last if $referer eq '' ;
		}
	    } else {
		print S 'Referer: ', &wrap_proxy_decode($referer), "\015\012"
		    if  $referer=~ s#^$THIS_SCRIPT_URL(/[^/]*/?)?##  &&  ($referer ne '') ;
	    }
	}


	# Add "Connection: close" header if we're using HTTP 1.1.
	print S "Connection: close\015\012"  if $HTTP_VERSION eq '1.1' ;

	# Add the cookie if it exists and cookies aren't banned here.
	print S 'Cookie: ', $cookie_to_server, "\015\012"
	    if !$cookies_are_banned_here && ($cookie_to_server ne '') ;

	# Add Pragma: and Cache-Control: headers if they were given in the
	#   request, to allow caches to behave properly.  These two headers
	#   need no modification.
	# As explained above, we can't rely on request headers being provided
	#   to the script via environment variables.
	print S "Pragma: $ENV{HTTP_PRAGMA}\015\012" if $ENV{HTTP_PRAGMA} ne '' ;
	print S "Cache-Control: $ENV{HTTP_CACHE_CONTROL}\015\012"
	    if $ENV{HTTP_CACHE_CONTROL} ne '' ;


	# Add Authorization: header if we've had a challenge.
	if ($realm ne '') {
	    # If we get here, we know $realm has a defined $auth and has not
	    #   been tried.
	    print S 'Authorization: Basic ', $auth{$realm}, "\015\012" ;
	    $tried_realm= $realm ;

	} else {
	    # If we have auth information for this server, what the hey, let's
	    #   try one, it may save us a request/response cycle.
	    # First case is for rare case when auth info is in URL.  Related
	    #   block 100 lines down needs no changes.
	    if ($username ne '') {
		print S 'Authorization: Basic ',
			&base64($username . ':' . $password),
			"\015\012" ;
	    } elsif ( ($tried_realm,$auth)= each %auth ) {
		print S 'Authorization: Basic ', $auth, "\015\012" ;
	    }
	}


	# A little problem with authorization and POST requests: If auth
	#   is required, we won't know which realm until after we make the
	#   request and get part of the response.  But to make the request,
	#   we have to send the entire POST body, because some servers
	#   mistakenly require that before returning even an error response.
	#   So this means we have to send the entire POST body, and be
	#   prepared to send it a second time, thus we have to store it
	#   locally.  Either that, or fail to send the POST body a second
	#   time.  Here, we let the owner of this proxy set $MAX_REQUEST_SIZE:
	#   store and post a second time if a request is smaller, or else
	#   die with 413 the second time through.

	# If request method is POST, copy content headers and body to request.
	# The first time through here, save body to @postbody, if the body's
	#   not too big.
	if ($ENV{'REQUEST_METHOD'} eq 'POST') {

	    if ($body_too_big) {
		# Quick 'n' dirty response for an unlikely occurrence.
		# 413 is not actually an HTTP/1.0 response...
		&HTMLdie("Sorry, this proxy can't handle a request larger "
		       . "than $MAX_REQUEST_SIZE bytes at a password-protected"
		       . " URL.  Try reducing your submission size, or submit "
		       . "it to an unprotected URL.", 'Submission too large',
			 '413 Request Entity Too Large') ;
	    }

	    # Otherwise...
	    $lefttoget= $ENV{'CONTENT_LENGTH'} ;
	    print S 'Content-type: ', $ENV{'CONTENT_TYPE'}, "\015\012",
		    'Content-length: ', $lefttoget, "\015\012\015\012" ;

	    if (@postbody) {
		print S @postbody ;
	    } else {
		$body_too_big= ($lefttoget > $MAX_REQUEST_SIZE) ;

		# Loop to guarantee all is read from STDIN.
		do {
		    $lefttoget-= read(STDIN, $postblock, $lefttoget) ;
		    print S $postblock ;
		    # efficient-- only doing test when input is slow anyway.
		    push(@postbody, $postblock) unless $body_too_big ;
		} while $lefttoget && ($postblock ne '') ;
	    }

	# For GET or HEAD requests, just add extra blank line.
	} else {
	    print S "\015\012" ;
	}


	# Wait a minute for the response to start
	vec($rin= '', fileno(S), 1)= 1 ;
	select($rin, undef, undef, 60)
	    || &HTMLdie("No response from $realhost:$realport") ;


	#------ Read full response into $status, $headers, and $body ----

	# Support both HTTP 1.x and HTTP 0.9
	$status= <S> ;  # first line, which is the status line in HTTP 1.x


	# HTTP 0.9
	# Ignore possibility of HEAD, since it's not defined in HTTP 0.9.
	# Do any HTTP 0.9 servers really exist anymore?
	unless ($status=~ m#^HTTP/#) {
	    $is_html= 1 ;   # HTTP 0.9 by definition implies an HTML response
	    $content_type= 'text/html' ;
	    undef $/ ;
	    $body= $status . <S> ;
	    $status= '' ;

	    close(S) ;
	    untie(*S) if $scheme eq 'https' ;
	    return ;
	}


	# After here, we know we're using HTTP 1.x

	# Be sure to handle case when server doesn't send blank line!  It's
	#   rare and erroneous, but a couple servers out there do that when
	#   responding with a redirection.  This can cause some processes to
	#   linger and soak up resources, particularly under mod_perl.
	# To handle this, merely check for eof(S) in until clause below.
	# ... except that for some reason invoking eof() on a tied SSL_Handle
	#   makes later read()'s fail with unlikely error messages.  :(
	#   So instead of eof(S), test "$_ eq ''".

	# Loop to get $status and $headers until we get a non-100 response.
	do {
	    ($status_code)= $status=~ m#^HTTP/\d+\.\d+\s+(\d+)# ;

	    $headers= '' ;   # could have been set by first attempt
	    do {
		$headers.= $_= <S> ;    # $headers includes last blank line
#	    } until (/^(\015\012|\012)$/) || eof(S) ; # lines end w/ LF or CRLF
	    } until (/^(\015\012|\012)$/) || $_ eq '' ; #lines end w/ LF or CRLF

	    $status= <S> if $status_code == 100 ;  # re-read for next iteration
	} until $status_code != 100 ;

	# Unfold long header lines, a la RFC 822 section 3.1.1
	$headers=~ s/(\015\012|\012)[ \t]+/ /g ;


	# Check for 401 Unauthorized response
	if ($status=~ m#^HTTP/\d+\.\d+\s+401\b#) {
	    ($realm)=
		$headers=~ /^WWW-Authenticate:\s*Basic\s+realm="([^"\n]*)/mi ;
	    &HTMLdie("Error by target server: no WWW-Authenticate header.")
		unless $realm ne '' ;

	    if ($auth{$realm} eq '') {
		&get_auth_from_user($host, $realm, $URL) ;
	    } elsif ($realm eq $tried_realm) {
		&get_auth_from_user($host, $realm, $URL, 1) ;
	    }

	    # so now $realm exists, has defined $auth, and has not been tried
	    close(S) ;
	    untie(*S) if $scheme eq 'https' ;
	    redo HTTP_GET ;
	}


	# Extract $content_type, used in several places
	($content_type)= $headers=~ m#^Content-Type:\s*([\w/.+\$-]*)#mi ;
	$content_type= lc($content_type) ;

	# If we're text only, then cut off non-text responses (but allow
	#   unspecified types).
	if ($TEXT_ONLY) {
	    if ( ($content_type ne '') && ($content_type!~ m#^text/#i) ) {
		&non_text_die ;
	    }
	}

	# If we're removing scripts, then disallow script MIME types.
	if ($scripts_are_banned_here) {
	    &script_content_die  if $content_type=~ /^$SCRIPT_TYPE_REGEX$/io ;

	    # Note that the non-standard Link: header, which may link to a
	    #   style sheet, is handled in http_fix().
	}


	# If URL matches one of @BANNED_IMAGE_URL_PATTERNS, then skip the
	#   resource unless it's clearly a text type.
	if ($images_are_banned_here) {
	    &skip_image  unless $content_type=~ m#^text/#i ;
	}

	# Keeping $base_url and its related variables up-to-date is an
	#   ongoing job.  Here, we look in appropriate headers.  Note that if
	#   Content-Base: doesn't exist, Content-Location: is an absolute URL.
	if        ($headers=~ m#^Content-Base:\s*([\w+.-]+://\S+)#mi) {
	    $base_url= $1, &fix_base_vars ;
	} elsif   ($headers=~ m#^Content-Location:\s*([\w+.-]+://\S+)#mi) {
	    $base_url= $1, &fix_base_vars ;
	} elsif   ($headers=~ m#^Location:\s*([\w+.-]+://\S+)#mi) {
	    $base_url= $1, &fix_base_vars ;
	}

	# Now, fix the headers with &http_fix().  It uses &full_url(), and
	#   may modify the headers we just extracted the base URL from.
	# This also includes cookie support.
	&http_fix ;



	# If configured, make this response as non-cacheable as possible.
	#   This means remove any Expires: and Pragma: headers (the latter
	#   could be using extensions), strip Cache-Control: headers of any
	#   unwanted directives and add the "no-cache" directive, and add back
	#   to $headers the new Cache-Control: header and a "Pragma: no-cache"
	#   header.
	# A lot of this is documented in the HTTP 1.1 spec, sections 13 as a
	#   whole, 13.1.3, 13.4, 14.9, 14.21, and 14.32.  The Cache-Control:
	#   response header has eight possible directives, plus extensions;
	#   according to section 13.4, all except "no-cache", "no-store", and
	#   "no-transform" might indicate cacheability, so remove them.  Remove
	#   extensions for the same reason.  Remove any parameter from
	#   "no-cache", because that would limit its effect.  This effectively
	#   means preserve only "no-store" and "no-transform" if they exist
	#   (neither have parameters), and add "no-cache".
	# We use a quick method here that works for all but cases both faulty
	#   and obscure, but opens no privacy holes; in the future we may fully
	#   parse the header value(s) into its comma-separated list of
	#   directives.

	if ($MINIMIZE_CACHING) {
	    my($new_value)= 'no-cache' ;
	    $new_value.= ', no-store'
		if $headers=~ /^Cache-Control:.*?\bno-store\b/mi ;
	    $new_value.= ', no-transform'
	      if $headers=~ /^Cache-Control:.*?\bno-transform\b/mi ;

	    my($no_cache_headers)=
		"Cache-Control: $new_value\015\012Pragma: no-cache\015\012" ;

	    $headers=~ s/^Cache-Control:[^\012]*\012?//mig ;
	    $headers=~ s/^Pragma:[^\012]*\012?//mig ;
	    $headers=~ s/^Expires:[^\012]*\012?//mig ;

	    $headers= $no_cache_headers . $headers ;
	}



	# Set $is_html if headers indicate HTML response.
	# Question: are there any other HTML-like MIME types, including x-... ?
	$is_html= 1  if $content_type eq 'text/html' ;


	# Some servers return HTML content without the Content-Type: header.
	#   These MUST be caught, because Netscape displays them as HTML, and
	#   a user could lose their anonymity on these pages.
	# According to the HTTP 1.1 spec, section. 7.2.1, browsers can choose
	#   how to deal with HTTP bodies with no Content-Type: header.  See
	#       http://www.ietf.org/rfc/rfc2616.txt
	# In such a case, Netscape seems to always assume "text/html".
	#   Konqueror seems to guess the MIME type by using the Unix "file"
	#   utility on the first 1024 bytes, and possibly other clues (e.g.
	#   resource starts with "<h1>").
	# In any case, we must interpret as HTML anything that *may* be
	#   interpreted as HTML by the browser.  So if there is no
	#   Content-Type: header, set $is_html=1 .  The worst that would
	#   happen would be the occasional content mangled by modified URLs,
	#   which is better than a privacy hole.

	$is_html= 1  if ($content_type eq '') ;

	# To support non-NPH hack, replace first part of $status with
	#   "Status:" if needed.
	$status=~ s#^\S+#Status:#  if $NOT_RUNNING_AS_NPH ;

	# To support streaming media and large files, read the data from
	#   the server and send it immediately to the client.  The exception
	#   is HTML content, which still must be read fully to be converted
	#   in the main block.  HTML content is not normally streaming or
	#   very large.
	# This requires $status and $headers to be returned now, which is
	#   OK since headers have been completely cleaned up by now.  This
	#   also means that changes after this point to $body won't
	#   have any effect, which in fact is fine in the case of non-HTML
	#   resources.  Set $response_sent to prevent the main block from
	#   sending a response.
	# Also, handle any non-HTML types here which must be proxified.
	# This is a bit sloppy now, just a quick hack to get rudimentary
	#   handling of multiple types working and released.  It will be
	#   rewritten more cleanly at some point, when the whole proxifying
	#   of different types is modularized better.

	# Only read body if the request method is not HEAD
	if ($ENV{'REQUEST_METHOD'} ne 'HEAD') {

	    # Because of the erroneous way some browsers use the expected
	    #   MIME type instead of the actual Content-Type: header, check
	    #   $expected_type first.
	    # Since style sheets tend to be automatically loaded, whereas other
	    #   types (like scripts) are more user-selected, plus the fact that
	    #   CSS can be safely proxified and scripts cannot, we treat a
	    #   resource as CSS if it *may* be treated as CSS by the browser.
	    #   This is relevant when $expected_type and Content-Type: differ.

	    if (    ($expected_type=~ /^$TYPES_TO_HANDLE_REGEX$/io)
		 || ($content_type=~  /^$TYPES_TO_HANDLE_REGEX$/io)   ) {

		my($type) ;
		if ( ($expected_type eq 'text/css') || ($content_type eq 'text/css') ) {
		    $type= 'text/css' ;
		} elsif ($expected_type=~ /^$TYPES_TO_HANDLE_REGEX$/io) {
		    $type= $expected_type ;
		} else {
		    $type= $content_type ;
		}

		# If response is chunked, then dechunk it before processing.
		# Not perfect (it loses the benefit of chunked encoding), but it
		#   works and will seldom be a problem.  Chunked encoding won't
		#   often be used for the MIME types we're proxifying anyway.
		# Append $footers into $headers, and remove any Transfer-Encoding: header.
		if ($headers=~ /^Transfer-Encoding:[ \t]*chunked\b/mi) {
		    ($body, $footers)= &get_chunked_body('S') ;
		    &HTMLdie(&HTMLescape("Error reading chunked response from $URL ."))
			unless defined($body) ;
		    $headers=~ s/^Transfer-Encoding:[^\012]*\012?//mig ;
		    $headers=~ s/^(\015\012|\012)/$footers$1/m ;

		# Handle explicitly sized response.
		} elsif ($headers=~ /^Content-Length:[ \t]*(\d+)/mi) {
		    $body= &read_socket('S', $1) ;

		# If not chunked or sized, read entire input into $body.
		} else {
		    undef $/ ;
		    $body= <S> ;
		}

		# If $body looks like it's in UTF-16 encoding, then convert it
		#   to UTF-8 before proxifying.
		un_utf16(\$body) if ($body=~ /^(?:\376\377|\377\376)/) ;

		# If Content-Type: is "text/html" and body looks like HTML,
		#   then treat it as HTML.  This helps with sites that play
		#   fast and loose with MIME types (e.g. hotmail).  Hacky.
		if (($content_type eq 'text/html') and $body=~ /^\s*<(?:\!(?!--\s*\n)|html)/) {
		    $type= 'text/html' ;
		    $is_html= 1 ;

		} else {

		    $body= &proxify_block($body, $type) ;
		    $headers=~ s/^Content-Length:.*/
				 'Content-Length: ' . length($body) /mie ;

		    print $status, $headers, $body ;
		    $response_sent= 1 ;
		}


	    } elsif ($is_html) {
		# If response is chunked, handle as above; see comments there.
		if ($headers=~ /^Transfer-Encoding:[ \t]*chunked\b/mi) {
		    ($body, $footers)= &get_chunked_body('S') ;
		    &HTMLdie(&HTMLescape("Error reading chunked response from $URL ."))
			unless defined($body) ;
		    $headers=~ s/^Transfer-Encoding:[^\012]*\012?//mig ;
		    $headers=~ s/^(\015\012|\012)/$footers$1/m ;

		# Handle explicitly sized response.
		} elsif ($headers=~ /^Content-Length:[ \t]*(\d+)/mi) {
		    $body= &read_socket('S', $1) ;

		# If not chunked or sized, read entire input into $body.
		} else {
		    undef $/ ;
		    $body= <S> ;
		}

		# If $body looks like it's in UTF-16 encoding, then convert it
		#   to UTF-8 before proxifying.
		un_utf16(\$body) if ($body=~ /^(?:\376\377|\377\376)/) ;


	    # This is for when the resource is passed straight through without
	    #   modification.
	    # We don't care whether it's chunked or not here.
	    } else {
		my($buf) ;
		print $status, $headers ;

		# If using SSL, read() could return 0 and truncate data. :P
		print $buf while read(S, $buf, 16384) ;

		$response_sent= 1 ;

	    }

	} else {
	    $body= '' ;
	}

	close(S) ;
	untie(*S) if $scheme eq 'https' ;

    }  # HTTP_GET:

}  # sub http_get()




# This package defines a SSL filehandle, complete with all the functions
#   needed to tie a filehandle to.  This lets us use the routine http_get()
#   above for SSL (https) communication too, which means we only have one
#   routine to maintain instead of two-- big win.
# The idea was taken from Net::SSLeay::Handle, which is a great idea, but the
#   current implementation of that module isn't suitable for this application.
# This implementation uses an input buffer, which lets us write a moderately
#   efficient READLINE() routine here.  Net::SSLeay::ssl_read_until() would be
#   the natural function to use for that, but it reads and tests all input one
#   character at a time.
# This is in a BEGIN block to make sure any initialization is done.  "use"
#   would effectively do a BEGIN block too.

# These are all socket functions used by http_get():  print(), read(), <>,
#   close(), fileno() for select(), eof(), binmode()

BEGIN {
    package SSL_Handle ;

    use vars qw($SSL_CONTEXT  $DEFAULT_READ_SIZE) ;

    $DEFAULT_READ_SIZE= 512 ;


    # Create an SSL socket with e.g. "tie(*S_SSL, 'SSL_Handle', \*S_PLAIN)",
    #   where S_PLAIN is an existing open socket to be used by S_SSL.
    # S_PLAIN must remain in scope for the duration of the use of S_SSL, or
    #   else you'll get OpenSSL errors like "bad write retry".
    # If $unbuffered is set, then the socket input will be read one character
    #   at a time (probably slower).
    sub TIEHANDLE {
	my($class, $socket, $unbuffered)= @_ ;
	my($ssl) ;

	# $SSL_CONTEXT only needs to be created once (e.g. with mod_perl).
	unless ($SSL_CONTEXT) {
	    # load_error_strings() is only worth the effort when using mod_perl
	    Net::SSLeay::load_error_strings() if $ENV{'MOD_PERL'} ;
	    Net::SSLeay::SSLeay_add_ssl_algorithms() ;
	    Net::SSLeay::randomize() ;

	    # Create the reusable SSL context
	    $SSL_CONTEXT= Net::SSLeay::CTX_new()
		or &main::HTMLdie("Can't create SSL context: $!") ;

	    # Need this to cope with bugs in some other SSL implementations.
	    Net::SSLeay::CTX_set_options($SSL_CONTEXT, &Net::SSLeay::OP_ALL)
		and &main::HTMLdie("Can't set options on SSL context: $!");
	}

	$ssl = Net::SSLeay::new($SSL_CONTEXT)
	    or &main::HTMLdie("Can't create SSL connection: $!");
	Net::SSLeay::set_fd($ssl, fileno($socket))
	    or &main::HTMLdie("Can't set_fd: $!") ;
	Net::SSLeay::connect($ssl) or &main::HTMLdie("Can't SSL connect: $!") ;

	bless { SSL      => $ssl,
		socket   => $socket,
		readsize => ($unbuffered  ? 0  : $DEFAULT_READ_SIZE),
		buf      => '',
		eof      => '',
	      },
	    $class ;  # returns reference
    }


    # For the print() function.  Respect $, and $\ settings.
    sub PRINT {
	my($self)= shift ;
	my($written, $errs)=
	    Net::SSLeay::ssl_write_all($self->{SSL}, join($, , @_) . $\ ) ;
	&main::HTMLdie("Net::SSLeay::ssl_write_all error: $errs") if $errs ne '' ;
	return 1 ;   # to keep consistent with standard print()
    }


    # For read() and sysread() functions.
    # Note that unlike standard read() or sysread(), this function can return
    #   0 even when not at EOF, and when select() on the underlying socket
    #   indicates there is data to be read.  :(  This is because of SSL
    #   buffering issues: OpenSSL processes data in chunks (records), so a
    #   socket may have some data available but not enough for a full record,
    #   i.e. enough to release decrypted data to the reader.
    # So how can an application distinguish between an empty read() and EOF?
    #   Note that eof() is problematic too (see notes there).  :(
    # jsm-- may be possible to handle this by looking for SSL_ERROR_WANT_READ
    #   in the error code; http://www.openssl.org/docs/ssl/SSL_get_error.html
    #   has some info, then look in the source code of Net::SSLeay.
    sub READ {
	my($self)= shift ;
	return 0 if $self->{eof} ;

	# Can't use my(undef) in some old versions of Perl, so use $dummy.
	my($dummy, $len, $offset)= @_ ;   # $_[0] is handled explicitly below
	my($read, $errs) ;

	# this could be cleaned up....
	if ($len > length($self->{buf})) {
	    if ( $offset || ($self->{buf} ne '') ) {
		$len-= length($self->{buf}) ;
		#$read= Net::SSLeay::ssl_read_all($self->{SSL}, $len) ;
		($read, $errs)= &ssl_read_all_fixed($self->{SSL}, $len) ;
		&main::HTMLdie("ssl_read_all_fixed() error: $errs") if $errs ne '' ;
		return undef unless defined($read) ;
		$self->{eof}= 1  if length($read) < $len ;
		my($buflen)= length($_[0]) ;
		$_[0].= "\0" x ($offset-$buflen)  if $offset>$buflen ;
		substr($_[0], $offset)= $self->{buf} . $read ;
		$self->{buf}= '' ;
		return length($_[0])-$offset ;
	    } else {
		# Streamlined block for the most common case.
		#$_[0]= Net::SSLeay::ssl_read_all($self->{SSL}, $len) ;
		($_[0], $errs)= &ssl_read_all_fixed($self->{SSL}, $len) ;
		&main::HTMLdie("ssl_read_all_fixed() error: $errs") if $errs ne '' ;
		return undef unless defined($_[0]) ;
		$self->{eof}= 1  if length($_[0]) < $len ;
		return length($_[0]) ;
	    }
	} else {
	    # Here the ?: operator returns an lvar.
	    ($offset  ? substr($_[0], $offset)  : $_[0])=
		substr($self->{buf}, 0, $len) ;
	    substr($self->{buf}, 0, $len)= '' ;
	    return $len ;
	}
    }


    # For <> style input.
    # In Perl, $/ as the input delimiter can have two special values:  undef
    #   reads all input as one record, and "" means match on multiple blank
    #   lines, like the regex "\n{2,}".  Net::SSLeay doesn't support these,
    #   but here we support the undef value (though not the "" value).
    # See the note with READ(), above, about possible SSL buffering issues.
    #   It's not as big a problem here, since <> returns undef at EOF.  Note
    #   that ssl_read_all() blocks until all requested data is read.
    # Net::SSLeay::ssl_read_until() would normally be the natural function for
    #   this, but it reads and tests all input one character at a time, which
    #   is potentially very inefficient.  Thus we implement this package with
    #   an input buffer.
    sub READLINE {
	my($self)= shift ;
	my($read, $errs) ;
	if (defined($/)) {
	    if (wantarray) {
		return () if $self->{eof} ;
		($read, $errs)= &ssl_read_all_fixed($self->{SSL}) ;
		&main::HTMLdie("ssl_read_all_fixed() error: $errs") if $errs ne '' ;
		# Prepend current buffer, and split to end items on $/ or EOS;
		#   this regex prevents final '' element.
		$self->{eof}= 1 ;
		return ($self->{buf} . $read)=~ m#(.*?\Q$/\E|.+?\Z(?!\n))#sg ;
	    } else {
		return '' if $self->{eof} ;
		my($pos, $read, $ret) ;
		while ( ($pos= index($self->{buf}, $/)) == -1 ) {
		  $read= Net::SSLeay::read($self->{SSL}, $self->{readsize} || 1 ) ;
		  return undef if $errs = Net::SSLeay::print_errs('SSL_read') ;
		  $self->{eof}= 1, return $self->{buf}  if $read eq '' ;
		  $self->{buf}.= $read ;
		}
		$pos+= length($/) ;
		$ret= substr($self->{buf}, 0, $pos) ;
		substr($self->{buf}, 0, $pos)= '' ;
		return $ret ;
	    }
	} else {
	    return '' if $self->{eof} ;
	    ($read, $errs)= &ssl_read_all_fixed($self->{SSL}) ;
	    &main::HTMLdie("ssl_read_all_fixed() error: $errs") if $errs ne '' ;
	    $self->{eof}= 1 ;
	    return  $self->{buf} . $read ;
	}
    }


    # Used when closing socket, or from UNTIE() or DESTROY() if needed.
    #   Calling Net::SSLeay::free() twice on the same object causes a crash,
    #   so be careful not to do that.
    sub CLOSE {
	my($self)= shift ;
	my($errs) ;
	$self->{eof}= 1 ;
	$self->{buf}= '' ;
	if (defined($self->{SSL})) {
	    Net::SSLeay::free($self->{SSL}) ;
	    delete($self->{SSL}) ;  # to detect later if we've free'd it or not
	    &main::HTMLdie("Net::SSLeay::free error: $errs")
		if $errs= Net::SSLeay::print_errs('SSL_free') ;
	    close($self->{socket}) ;
	}
    }

    # In case the SSL filehandle is not closed correctly, this will deallocate
    #   as needed.  Without this, memory could be eaten up under mod_perl.
    # Some versions of Perl seem to have trouble with the scoping of tied
    #   variables and their objects, so define both UNTIE() and DESTROY() here.
    sub UNTIE {
	my($self)= shift ;
	$self->CLOSE ;
    }
    sub DESTROY {
	my($self)= shift ;
	$self->CLOSE ;
    }


    # FILENO we define to be the fileno() of the underlying socket.
    #   This is our best guess as to what will work with select(), which is
    #   the only thing fileno() is used for here.
    # See the note with READ(), above, about possible issues with select().
    sub FILENO {
	my($self)= shift ;
	return fileno($self->{socket}) ;
    }


    # For EOF we first check the fields we set ({eof} and {buf}), then test the
    #   eof() value of the underlying socket.
    # Note that there may still be data coming through the socket even
    #   though a read() returns nothing; see the note with READ() above.
    #   It may be more accurate here to try "Net::SSLeay::read($self->{SSL},1)"
    #   into {buf} before using eof().
    # This routine causes a weird problem:  If Perl's eof() is used on a tied
    #   SSL_Handle, it causes later read()'s on that filehandle to fail with
    #   "SSL3_GET_RECORD:wrong version number", which seems inappropriate.
    #   So, avoid use of eof().  :(  Maybe test a read result against ''.
    sub EOF {
	my($self)= shift ;
	return 1 if $self->{eof} ;        # overrides anything left in {buf}
	return 0 if $self->{buf} ne '' ;
	return eof($self->{socket}) ;
    }


    # BINMODE we define to be the same as binmode() on the underlying socket.
    # Only ever relevant on non-Unix machines.
    sub BINMODE {
	my($self)= shift ;
	binmode($self->{socket}) ;
    }


    # In older versions of Net::SSLeay, there was a bug in ssl_read_all()
    #   and ssl_read_until() where pages were truncated on any "0" character.
    #   To work with those versions, here we use a fixed copy of ssl_read_all().
    #   Earlier versions of CGIProxy had older copies of the two routines but
    #   fixed; now we just copy ssl_read_all() in from the new Net::SSLeay
    #   module and tweak it as needed.  (ssl_read_until() is no longer needed
    #   now that this package uses an input buffer.)

    sub ssl_read_all_fixed {
	my ($ssl,$how_much) = @_;
	$how_much = 2000000000 unless $how_much;
	my ($got, $errs);
	my $reply = '';

	while ($how_much > 0) {
	    $got = Net::SSLeay::read($ssl,$how_much);
	    last if $errs = Net::SSLeay::print_errs('SSL_read');
	    $how_much -= Net::SSLeay::blength($got);
	    last if $got eq '';  # EOF
	    $reply .= $got;
	}
	return wantarray ? ($reply, $errs) : $reply;
    }


    # end of package SSL_Handle
}




# ftp_get:

sub ftp_get {
    my($is_dir, $rcode, @r, $dataport, $remote_addr,
       $ext, $content_type, %content_type, $content_length, $enc_URL,
       @welcome, @cwdmsg) ;
    local($/)= "\012" ;

    $port= 21 if $port eq '' ;

    # List of file extensions and associated MIME types, or at least the ones
    #   a typical browser distinguishes from a nondescript file.
    # I'm open to suggestions for improving this.  One option is to read the
    #   file mime.types if it's available.
    %content_type=
	  ('txt',  'text/plain',
	   'text', 'text/plain',
	   'htm',  'text/html',
	   'html', 'text/html',
	   'css',  'text/css',
	   'png',  'image/png',
	   'jpg',  'image/jpeg',
	   'jpeg', 'image/jpeg',
	   'jpe',  'image/jpeg',
	   'gif',  'image/gif',
	   'xbm',  'image/x-bitmap',
	   'mpg',  'video/mpeg',
	   'mpeg', 'video/mpeg',
	   'mpe',  'video/mpeg',
	   'qt',   'video/quicktime',
	   'mov',  'video/quicktime',
	   'aiff', 'audio/aiff',
	   'aif',  'audio/aiff',
	   'au',   'audio/basic',
	   'snd',  'audio/basic',
	   'wav',  'audio/x-wav',
	   'mp2',  'audio/x-mpeg',
	   'mp3',  'audio/mpeg',
	   'ram',  'audio/x-pn-realaudio',
	   'rm',   'audio/x-pn-realaudio',
	   'ra',   'audio/x-pn-realaudio',
	   'gz',   'application/x-gzip',
	   'zip',  'application/zip',
	   ) ;


    $is_dir= $path=~ m#/$# ;
    $is_html= 0 if $is_dir ;   # for our purposes, do not treat dirs as HTML

    # Set $content_type based on file extension.
    # Hmm, still unsure how best to handle unknown file types.  This labels
    #   them as text/plain, so that README's, etc. will display right.
    ($ext)= $path=~ /\.(\w+)$/ ;  # works for FTP, not for URLs with query etc.
    $content_type= ($is_html || $is_dir)  ? 'text/html'
					  : $content_type{lc($ext)}
					    || 'text/plain' ;


    # If we're removing scripts, then disallow script MIME types.
    if ($scripts_are_banned_here) {
	&script_content_die if $content_type=~ /^$SCRIPT_TYPE_REGEX$/io ;
    }


    # Hack to help handle spaces in pathnames.  :P
    # $path should be delivered to us here with spaces encoded as "%20".
    #   But that's not what the FTP server wants (or what we should display),
    #   so translate them back to spaces in a temporary copy of $path.
    #   Hopefully the FTP server will allow spaces in the FTP commands below,
    #   like "CWD path with spaces".
    local($path)= $path ;
    $path=~ s/%20/ /g ;


    # Create $status and $headers, and leave $body and $is_html as is.
    # Directories use an HTML response, though $is_html is false when $is_dir.
    $status= "$HTTP_1_X 200 OK\015\012" ;
    $headers= $NO_CACHE_HEADERS . "Date: " . &rfc1123_date($now,0) . "\015\012"
	. ($content_type  ? "Content-type: $content_type\015\012"  : '') . "\015\012" ;


    # Open the control connection to the FTP server
    &newsocketto('S', $host, $port) ;
    binmode S ;   # see note with "binmode STDOUT", above

    # Luckily, RFC 959 (FTP) has a really good list of all possible response
    #   codes to all possible commands, on pages 50-53.

    # Connection establishment
    ($rcode)= &ftp_command('', '120|220') ;
    &ftp_command('', '220') if $rcode==120 ;

    # Login
    ($rcode, @welcome)= &ftp_command("USER $username\015\012", '230|331') ;
    ($rcode, @welcome)= &ftp_command("PASS $password\015\012", '230|202')
	if $rcode==331 ;

    # Set transfer parameters
    &ftp_command("TYPE I\015\012", '200') ;


    # If using passive FTP, send PASV command and parse response.  RFC 959
    #   isn't clear on the response format, but here we assume that the first
    #   six integers separated by commas are the host and port.
    if ($USE_PASSIVE_FTP_MODE) {
	my(@p) ;
	($rcode, @r)= &ftp_command("PASV\015\012", '227') ;
	@p= (join('',@r))=~
		/(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)/ ;
	$dataport= ($p[4]<<8) + $p[5] ;

	# Open the data socket to $dataport.  This is conceptually paired
	#   with the accept() for non-passive mode below, but we have to
	#   open the socket here first to allow for 125/150 responses to
	#   LIST and RETR commands in passive mode.
	&newsocketto('DATA_XFER', $host, $dataport) ;
	binmode DATA_XFER ;   # see note with "binmode STDOUT", above

    # If not using passive FTP, listen on open port and send PORT command.
    # See notes by newsocketto() about replacing pack('S n a4 x8') usage.
    } else {
	# Create and listen on data socket
	socket(DATA_LISTEN, AF_INET, SOCK_STREAM, (getprotobyname('tcp'))[2])
	    || &HTMLdie("Couldn't create FTP data socket: $!") ;
#	bind(DATA_LISTEN, pack('S n a4 x8', AF_INET, 0, "\0\0\0\0") )
	bind(DATA_LISTEN, pack_sockaddr_in(0, INADDR_ANY))
	    || &HTMLdie("Couldn't bind FTP data socket: $!") ;
#	$dataport= (unpack('S n a4 x8', getsockname(DATA_LISTEN)))[1] ;
	$dataport= (unpack_sockaddr_in(getsockname(DATA_LISTEN)))[0] ;
	listen(DATA_LISTEN,1)
	    || &HTMLdie("Couldn't listen on FTP data socket: $!") ;
	select((select(DATA_LISTEN), $|=1)[0]) ;    # unbuffer the socket

	# Tell FTP server which port to connect to
	&ftp_command( sprintf("PORT %d,%d,%d,%d,%d,%d\015\012",
			      unpack('C4', substr(getsockname(S),4,4)),
			      $dataport>>8, $dataport & 255),
		      '200') ;
    }


    # Do LIST for directories, RETR for files.
    # Unfortunately, the FTP spec in RFC 959 doesn't define a standard format
    #   for the response to LIST, but most servers use the equivalent of
    #   Unix's "ls -l".  Response to the NLST command is designed to be
    #   machine-readable, but it has nothing but file names.  So we use
    #   LIST and parse it as best we can later.
    if ($is_dir) {
	# If we don't CWD first, then symbolic links won't be followed.
	($rcode, @cwdmsg)= &ftp_command("CWD $path\015\012", '250') ;
	($rcode, @r)= &ftp_command("LIST\015\012", '125|150') ;
# was:  ($rcode, @r)= &ftp_command("LIST $path\015\012", '125|150') ;

    } else {
	($rcode, @r)= &ftp_command("RETR $path\015\012", '125|150|550') ;

	# If 550 response, it may be a symlink to a directory.
	# Try to CWD to it; if successful, do a redirect, else die with the
	#   original error response.  Note that CWD is required by RFC 1123
	#   (section 4.1.2.13), which updates RFC 959.
	if ($rcode==550) {
	    ($rcode)= &ftp_command("CWD $path\015\012", '') ;
	    &ftp_error(550,@r) unless $rcode==250 ;

	    ($enc_URL= $URL)=~ s/ /%20/g ;  # URL-encode any spaces

	    # Redirect the browser to the same URL with a trailing slash
	    print "$HTTP_1_X 301 Moved Permanently\015\012", $NO_CACHE_HEADERS,
		  "Date: ", &rfc1123_date($now,0), "\015\012",
		  "Location: ", $url_start, &wrap_proxy_encode($enc_URL . '/'),
		  "\015\012\015\012" ;
	    close(S) ; close(DATA_LISTEN) ; close(DATA_XFER) ;
	    goto EXIT ;
	}
    }


    # If not using passive FTP, accept the connection.
    if (!$USE_PASSIVE_FTP_MODE) {
	($remote_addr= accept(DATA_XFER, DATA_LISTEN))
	    || &HTMLdie("Error accepting FTP data socket: $!") ;
	select((select(DATA_XFER), $|=1)[0]) ;      # unbuffer the socket
	close(DATA_LISTEN) ;
	&HTMLdie("Intruder Alert!  Someone other than the server is trying "
	       . "to send you data.")
	    unless (substr($remote_addr,4,4) eq substr(getpeername(S),4,4)) ;
    }


    # Read the data into $body.
    # Streaming support added in 1.3.  For notes about streaming, look near
    #   the end of the http_get() routine.  Basically, as long as a resource
    #   isn't HTML (or a directory listing, in the case of FTP), we can pass
    #   the data immediately to the client, since it won't be modified.  Be
    #   sure to set $response_sent here.

    # This first block is for the rare case when an FTP resource is a special
    #   type that needs to be converted, e.g. a style sheet.  The block is
    #   copied in from http_get() and modified.  It will be cleaner and
    #   handled differently in a future version.

    if ( !$is_dir && !$is_html &&
	 (    ($expected_type=~ /^$TYPES_TO_HANDLE_REGEX$/io)
	   || ($content_type=~  /^$TYPES_TO_HANDLE_REGEX$/io)   ) ) {

	my($type) ;
	if ( ($expected_type eq 'text/css') || ($content_type eq 'text/css') ) {
	    $type= 'text/css' ;
	} elsif ($expected_type=~ /^$TYPES_TO_HANDLE_REGEX$/io) {
	    $type= $expected_type ;
	} else {
	    $type= $content_type ;
	}

	undef $/ ;
	$body= <DATA_XFER> ;
	$body= &proxify_block($body, $type) ;

	$headers= "Content-Length: " . length($body) . "\015\012" . $headers ;

	print $status, $headers, $body ;
	$response_sent= 1 ;


    } elsif ($is_html) {
	undef $/ ;
	$body= <DATA_XFER> ;

    } elsif ($is_dir) {
	undef $/ ;            # This was used for all non-HTML before streaming
	$body= <DATA_XFER> ;  #   was supported.

    } else {

	# Stick a Content-Length: header into the headers if appropriate (often
	#   there's a "(xxx bytes)" string in a 125 or 150 response line).
	# Be careful about respecting previous value of $headers, which may
	#   already end in a blank line.
	foreach (grep(/^(125|150)/, @r)) {
	    if ( ($content_length)= /\((\d+)[ \t]+bytes\)/ ) {
		$headers= "Content-Length: $content_length\015\012" .$headers ;
		last ;
	    }
	}

	# This is the primary change to support streaming media.
	my($buf) ;
	print $status, $headers ;
	print $buf while read(DATA_XFER, $buf, 16384) ;
	$response_sent= 1 ;

    }


    close(DATA_XFER) ;

    # Get the final completion response
    &ftp_command('', '226|250') ;

    &ftp_command("QUIT\015\012") ;   # don't care how they answer

    close(S) ;

    # Make a user-friendly directory listing.  Add Content-Length: header.
    if ($is_dir) {
	&ftp_dirfix(\@welcome, \@cwdmsg) ;
	$headers= "Content-Length: " . length($body) . "\015\012" . $headers ;
    }

}  # sub ftp_get()


# Send $cmd and return response code followed by full lines of  FTP response.
# Die if response doesn't match the regex $ok_response.
# Assumes the FTP control connection is in socket S.
sub ftp_command {
    my($cmd, $ok_response)= @_ ;
    my(@r, $rcode) ;
    local($/)= "\012" ;

    print S $cmd ;

    $_= $r[0]= <S> ;
    $rcode= substr($r[0],0,3) ;
    until (/^$rcode /) {      # this catches single- and multi-line responses
	push(@r, $_=<S>) ;
    }

    &ftp_error($rcode,@r) if $ok_response ne '' && $rcode!~ /$ok_response/ ;
    return $rcode, @r ;
}


# Convert a directory listing to user-friendly HTML.
# The text in $body is the output of the FTP LIST command, which is *usually*
#   the equivalent of Unix's "ls -l" command.  See notes in ftp_get() about
#   why we use LIST instead of NLST.
# A couple of tangles here to handle spaces in filenames.  We should probably
#   handle spaces in other protocols too, but URLs normally prohibit spaces--
#   it's only relative paths within a scheme (like FTP) that would have them.
sub ftp_dirfix {
    my($welcome_ref, $cwdmsg_ref)= @_ ;
    my($newbody, $parent_link, $max_namelen,
       @f, $is_dir, $is_link, $link, $name, $size, $size_type, $file_type,
       $welcome, $cwdmsg, $insertion, $enc_path) ;

    # Set minimum name column width; longer names will widen the column
    $max_namelen= 16 ;

    # each file should have name/, size, date
    my(@body)= split(/\015?\012/, $body) ;
    foreach (@body) {
	# Hack to handle leading spaces in filenames-- only allow a single
	#   space after the 8th field before filename starts.
#	@f= split(" ", $_, 9) ;   # Note special use of " " pattern.
#	next unless $#f>=8 ;
	@f= split(" ", $_, 8) ;   # Note special use of " " pattern.
	next unless $#f>=7 ;
	@f[7,8]= $f[7]=~ /^(\S*) (.*)/ ;  # handle leading spaces in filenames

	next if $f[8]=~ /^\.\.?$/ ;
	$file_type= '' ;
	$is_dir=  $f[0]=~ /^d/i ;
	$is_link= $f[0]=~ /^l/i ;
	$file_type= $is_dir     ? 'Directory'
		  : $is_link    ? 'Symbolic link'
		  :               '' ;
	$name= $f[8] ;
	$name=~ s/^(.*) ->.*$/$1/ if $is_link ;   # remove symlink's " -> xxx"
	$name.= '/' if $is_dir ;
	$max_namelen= length($name) if length($name)>$max_namelen ;
	if ($is_dir || $is_link) {
	    ($size, $size_type)= () ;
	} else {
	    ($size, $size_type)= ($f[4], 'bytes') ;
	    ($size, $size_type)= ($size>>10, 'Kb') if $size > 10240 ;
	}

	# Easy absolute URL calculation, because we know it's a relative path.
	($enc_path= $base_path . $name)=~ s/ /%20/g ;  # URL-encode any spaces
	$link=  &HTMLescape( $url_start . &wrap_proxy_encode($enc_path) ) ;

	$newbody.=
	    sprintf("  <a href=\"%s\">%s</a>%s %5s %-5s %3s %2s %5s  %s\012",
			   $link, $name, "\0".length($name),
			   $size, $size_type,
			   @f[5..7],
			   $file_type) ;
    }

    # A little hack to get filenames to line up right-- replace embedded
    #  "\0"-plus-length with correct number of spaces.
    $newbody=~ s/\0(\d+)/ ' ' x ($max_namelen-$1) /ge ;

    if ($path eq '/') {
	$parent_link= '' ;
    } else {
	($enc_path= $base_path)=~ s#[^/]*/$## ;
	$enc_path=~ s/ /%20/g ;  # URL-encode any spaces
	$link=  &HTMLescape( $url_start . &wrap_proxy_encode($enc_path) ) ;
	$parent_link= "<a href=\"$link\">Up to higher level directory</a>" ;
    }

    if ($SHOW_FTP_WELCOME && $welcome_ref) {
	$welcome= &HTMLescape(join('', grep(s/^230-//, @$welcome_ref))) ;
	# Make links of any URLs in $welcome.  Imperfect regex, but does OK.
	$welcome=~ s#\b([\w+.-]+://[^\s"']+[\w/])(\W)#
	    '<a href="' . &full_url($1) . "\">$1</a>$2" #ge ;
	$welcome.= "<hr>" if $welcome ne '' ;
    } else {
	$welcome= '' ;
    }

    # If CWD returned a message about this directory, display it.  Make links
    #   a la $welcome, above.
    if ($cwdmsg_ref) {
	$cwdmsg= &HTMLescape(join('', grep(s/^250-//, @$cwdmsg_ref))) ;
	$cwdmsg=~ s#\b([\w+.-]+://[^\s"']+[\w/])(\W)#
	    '<a href="' . &full_url($1) . "\">$1</a>$2" #ge ;
	$cwdmsg.= "<hr>" if $cwdmsg ne '' ;
    }


    # Create the top insertion if needed.
    $insertion= &full_insertion($URL,0)  if $doing_insert_here ;


    $body= <<EOS ;
<html>
<title>FTP directory of $URL</title>
<body>
$insertion
<h1>FTP server at $host</h1>
<h2>Current directory is $path</h2>
<hr>
<pre>
$welcome$cwdmsg
$parent_link
$newbody
</pre>
<hr>
</body>
</html>
EOS

}


# Return a generalized FTP error page.
# For now, respond with 200.  In the future, give more appropriate codes.
sub ftp_error {
    my($rcode,@r)= @_ ;

    close(S) ; close(DATA_LISTEN) ; close(DATA_XFER) ;

    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOH ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>FTP Error</title></head>
<body>
<h1>FTP Error</h1>
<h3>The FTP server at $host returned the following error response:</h3>
<pre>
EOH
    print @r, "</pre>\n" ;

    &footer ;
    goto EXIT ;
}


#--------------------------------------------------------------------------

#
# <scheme>_fix: modify response as appropriate for given protocol (scheme).
#

# http_fix: modify headers as needed, including cookie support.
# Note that headers have already been unfolded, when they were read in.
# Some HTTP headers are defined as comma-separated lists of values, and they
#   should be split before being processed.  According to the HTTP spec in
#   RFC 2616, such headers are:
#     Accept|Accept-Charset|Accept-Encoding|Accept-Language|Accept-Ranges|
#     Allow|Cache-Control|Connection|Content-Encoding|Content-Language|
#     If-Match|If-None-Match|Pragma|Public|Transfer-Encoding|Upgrade|Vary|
#     Via|Warning|WWW-Authenticate
#   As it turns out, none need to be handled in new_header_value().  Thus, we
#   don't need to split any standard headers before processing.  See section
#   4.2 of RFC 2616, plus the header definitions, for more info.

# Conceivably, Via: and Warning: could be exceptions to this, since they
#   do contain hostnames.  But a) these are primarily for diagnostic info and
#   not used to connect to those hosts, and b) we couldn't distinguish the
#   hostnames from pseudonyms anyway.
# Unfortunately, the non-standard Link: and URI: headers may be lists, and
#   we *do* have to process them.  Because of their unusual format and rarity,
#   these are handled as lists directly in new_header_value().
sub http_fix {
    my($name, $value, $new_value) ;
    my(@headers)= $headers=~ /^([^\012]*\012?)/mg ;  # split into lines

    foreach (@headers) {
	next unless ($name, $value)= /^([\w.-]+):\s*([^\015\012]*)/ ;
	$new_value= &new_header_value($name, $value) ;
	$_= defined($new_value)
	    ? "$name: $new_value\015\012"
	    : '' ;
    }

    $headers= join('', @headers) ;
}


# Returns the value of an updated header, e.g. with URLs transformed to point
#   back through this proxy.  Returns undef if the header should be removed.
# This is used to translate both real headers and <meta http-equiv> headers.
# Special case for URI: and Link: -- these headers can be lists of values
#   (see the HTTP spec, and comments above in http_fix()).  Thus, we must
#   process these headers as lists, i.e. transform each URL in the header.
sub new_header_value {
    my($name, $value)= @_ ;
    $name= lc($name) ;

    # sanity check
    return undef if $name eq '' ;

    # These headers consist simply of a URL.
    # Note that all these are absolute URIs, except possibly Content-Location:,
    #   which may be relative to Content-Base or the request URI-- notably, NOT
    #   relative to anything in the content, like a <base> tag.
    return &full_url($value)
	if    $name eq 'content-base'
	   || $name eq 'content-location' ;

    # Location: header should carry forward the expected type, since some sites
    #   (e.g.. hotmail) may 302 forward to another URL and use the wrong
    #   Content-Type:, and that retrieved resource may still be treated by the
    #   browser as of the expected type.  Here we just carry forward the entire
    #   flag segment.
    if ($name eq 'location') {
	local($url_start)= $script_url . '/' . $packed_flags . '/' ;
	return &full_url($value) ;
    }


    # Modify cookies to point back through the script, or they won't work.
    # If they're banned from this server, or if $NO_COOKIE_WITH_IMAGE or
    #   $e_filter_ads is set and the current resource isn't text, then filter
    #   them all out.
    # We guess whether the current resource is text or not by using both
    #   the Content-Type: response header and the Accept: header in the
    #   original request.  Content-Type: can be something text, something
    #   non-text, or it can be absent; Accept: can either accept something
    #   text or not.  Our test here is that the resource is non-text either
    #   if Accept: accepts no text, or if Content-Type: indicates non-text.
    #   Put another way, it's text if Accept: can accept text, and
    #   Content-Type: is either a text type, or is absent.
    # This test handles some cases that failed with earlier simpler tests.
    #   One site had a cookie in a 302 response for a text page that didn't
    #   include a Content-Type: header.  Another site was sneakier--
    #   http://zdnet.com returns an erroneous response that surgically
    #   bypassed an earlier text/no-text test here:  a redirection
    #   response to an image contains cookies along with a meaningless
    #   "Content-Type: text/plain" header.  They only do this on images that
    #   look like Web bugs.  (Hmm, what are the odds of THAT happening by
    #   accident, eh?)  So basically that means we can't trust Content-Type:
    #   alone, because a malicious server has full control over that header,
    #   whereas the Accept: header comes from the client.
    if ($name eq 'set-cookie') {
	return undef if $cookies_are_banned_here ;
	if ($NO_COOKIE_WITH_IMAGE || $e_filter_ads) {
	    return undef
		if ($headers=~ m#^Content-Type:\s*(\S*)#mi  &&  $1!~ m#^text/#i)
		   || ! grep(m#^(text|\*)/#i, split(/\s*,\s*/, $env_accept)) ;
	}

	return &cookie_to_client($value, $path, $host) ;
    }


    # Extract $default_style_type as needed.
    # Strictly speaking, a MIME type is "token/token", where token is
    #    ([^\x00-\x20\x7f-\xff()<>@,;:\\"/[\]?=]+)   (RFCs 1521 and 822),
    #   but this below covers all existing and likely future MIME types.
    if ($name eq 'content-style-type') {
	$default_style_type= lc($1)  if $value=~ m#^\s*([/\w.+\$-]+)# ;
	return $value ;
    }


    # Extract $default_script_type as needed.
    # Same deal about "token/token" as above.
    if ($name eq 'content-script-type') {
	$default_script_type= lc($1)  if $value=~ m#^\s*([/\w.+\$-]+)# ;
	return $value ;
    }


    # Handle P3P: header.  P3P info may also exist in a <link> tag (or
    #   conceivably a Link: header), but those are already handled correctly
    #   where <link> tags (or Link: headers) are handled.
    if ($name eq 'p3p') {
	$value=~ s/\bpolicyref\s*=\s*['"]?([^'"\s]*)['"]?/
		   'policyref="' . &full_url($1) . '"' /gie ;
	return $value ;
    }


    # And the non-standard Refresh: header... any others?
    $value=~ s/(;\s*URL\s*=)\s*(\S*)/ $1 . &full_url($2) /ie,   return $value
	if $name eq 'refresh' ;

    # The deprecated URI: header may contain several URI's, inside <> brackets.
    $value=~ s/<(\s*[^>\015\012]*)>/ '<'.&full_url($1).'>' /gie, return $value
	if $name eq 'uri' ;


    # The non-standard Link: header is a little problematic.  It's described
    #   in the HTTP 1.1 spec, section 19.6.2.4, but it is not standard.  Among
    #   other things, it can be used to link to style sheets, but the mechanism
    #   for indicating the style sheet type (=language, which could be a script
    #   MIME type) is not defined.
    # The HTML 4.0 spec (section 14.6) gives a little more detail regarding
    #   its use of the Link: header, but is still ambiguous-- e.g. their
    #   examples don't specify the type, though elsewhere it's implied that's
    #   required.
    # Generally speaking, we handle this like a <link> tag.  For notes about
    #   this block, see the block above that handles <link> tags.  For a
    #   description of the unusual format of this header, see the HTTP spec.
    # Note that this may be a list of values, and all URIs in it must be
    #   handled.  This gets a little messy, because we split on commas, but
    #   don't split on commas that are inside <> brackets, because that's
    #   the URL.
    if ($name eq 'link') {
	my($v, @new_values) ;

	my(@values)= $value=~ /(<[^>]*>[^,]*)/g ;
	foreach $v (@values) {
	    my($type)= $v=~ m#[^\w.\/?&-]type\s*=\s*["']?\s*([/\w.+\$-]+)#i ;
	    $type= lc($type) ;

	    if ($type eq '') {
		my($rel) ;
		$rel= $+
		    if $v=~ /[^\w.\/?&-]rel\s*=\s*("([^"]*)"|'([^']*)'|([^'"][^\s]*))/i ;
		$type= 'text/css' if $rel=~ /\bstylesheet\b/i ;
	    }

	    return undef
		if $scripts_are_banned_here && $type=~ /^$SCRIPT_TYPE_REGEX$/io ;

	    local($url_start)= $url_start ;
	    if ($type ne '') {
		$url_start= $script_url . '/' .
		    &pack_flags($e_remove_cookies, $e_remove_scripts, $e_filter_ads,
				$e_hide_referer, $e_insert_entry_form,
				$is_in_frame, $type)
			. '/' ;
	    }

	    $v=~ s/<(\s*[^>\015\012]*)>/ '<' . &full_url($1) . '>' /gie ;
	    push(@new_values, $v) ;
	}

	return join(', ', @new_values) ;
    }



    # For all non-special headers, return $value
    return $value ;

}


#--------------------------------------------------------------------------
#    Special admin routines, when called via the scheme type "x-proxy://"
#--------------------------------------------------------------------------

#--------------------------------------------------------------------------
#
#   I took the liberty of creating a general mechanism to let this proxy do
#   whatever tricks it needs to do, via the magic URL scheme "x-proxy://".
#   It was required to support HTTP Basic Authentication, and it's useful
#   for other things too.  The mechanism uses a heirarchical URL space: a
#   function family is in the normal "hostname" location, then the functions
#   and subfunctions are where the path segments would be.  A query string
#   is allowed on the end.
#
#   Don't add functions to this that may compromise security, since anyone
#   can request a URL beginning with x-proxy://.  For that matter, malicious
#   Web pages can automatically invoke these URLs, which could be annoying
#   if e.g. they clear your cookies without warning or other acts.
#
#   Which URLs map to which functions should really be documented here.  So,
#
#     //auth/make_auth_cookie
#         receives the authorization form data, sends a formatted auth
#         cookie to the user, and redirects the user to the desired URL.
#
#     //start
#         initiates a browsing session.
#
#     //cookies/clear
#         clears all of a user's cookies.
#
#     //cookies/manage
#         present the user with a page to manage her/his cookies
#
#     //cookies/update
#         process whatever actions are requested from the //cookies/manage
#         page (currently only deletion of cookies).
#
#     //frames/topframe
#         returns the special top frame with the entry form and/or the
#         other insertion.
#
#     //frames/framethis
#         given a URL, returns a page that frames that URL in the lower
#         frame with the top frame above (not currently used).
#
#     //scripts/jslib
#         returns the JavaScript library used when rewriting JavaScript.
#         Normally, this can be cached for efficiency.
#
#--------------------------------------------------------------------------

# A general-purpose routine to handle all x-proxy requests.
# This is expected to exit when completed, so make sure any called routines
#   exit if needed.  (By "exit", I mean "goto EXIT".)
sub xproxy {
    my($URL)= @_ ;
    $URL=~ s/^x-proxy://i ;

    # $qs will contain the query string in $URL, whether it was encoded with
    #   the URL or came from QUERY_STRING.
    my($family, $function, $qs)=  $URL=~ m#^//(\w+)(/?[^?]*)\??(.*)#i ;

    if ($family eq 'auth') {

	# For //auth/make_auth_cookie, return an auth cookie and redirect user
	#   to the desired URL.  The URL is already encoded in $in{'l'}.
	if ($function eq '/make_auth_cookie') {
	    my(%in)= &getformvars() ; # must use () or will pass current @_!
	    my($location)= $url_start . $in{'l'} ;  # was already encoded
	    my($cookie)= &auth_cookie(@in{'u', 'p', 'r', 's'}) ;

	    &redirect_to($location, "Set-Cookie: $cookie\015\012") ;
	}


    } elsif ($family eq 'start') {
	&startproxy ;


    } elsif ($family eq 'cookies') {

	# If pages could link to x-proxy:// URLs directly, this would be a
	#   security hole in that malicious pages could clear or update one's
	#   cookies.  But full_url() prevents that.  If that changes, then we
	#   should consider requiring POST in /cookie/clear and /cookie/update
	#   to minimize this risk.
	if ($function eq '/clear') {
	    my($location)=
		$url_start . &wrap_proxy_encode('x-proxy://cookies/manage') ;
	    $location.= '?' . $qs    if $qs ne '' ;

	    &redirect_to($location, &cookie_clearer($ENV{'HTTP_COOKIE'})) ;


	} elsif ($function eq '/manage') {
	    &manage_cookies($qs) ;


	# For //cookies/update, clear selected cookies and go to manage screen.
	} elsif ($function eq '/update') {
	    my(%in)= &getformvars() ; # must use () or will pass current @_!
	    my($location)=
		$url_start . &wrap_proxy_encode('x-proxy://cookies/manage') ;

	    # Add encoded "from" parameter to URL if available.
	    if ($in{'from'} ne '') {
		my($from_param)= $in{'from'} ;
		$from_param=~ s/([^\w.-])/ '%' . sprintf('%02x',ord($1)) /ge ;
		$location.=  '?from=' . $from_param ;
	    }

	    # "delete=" input fields are in form &base64(&cookie_encode($name)).
	    my(@cookies_to_delete) ;
	    foreach ( split(/\0/, $in{'delete'}) ) {
		push(@cookies_to_delete, &unbase64($_)) ; # use map{} in Perl 5
	    }

	    &redirect_to($location, &cookie_clearer(@cookies_to_delete)) ;
	}


    } elsif ($family eq 'frames') {
	my(%in)= &getformvars($qs) ;

	# Send the top proxy frame when a framed page is reframed.
	if ($function eq '/topframe') {
	    &return_top_frame($in{'URL'}) ;

	# Not currently used
	} elsif ($function eq '/framethis') {
	    &return_frame_doc($in{'URL'}, &HTMLescape(&wrap_proxy_decode($in{'URL'}))) ;
	}


    } elsif ($family eq 'scripts') {

	# Return the library needed for JavaScript rewriting.  Normally, this
	#   can be cached.
	if ($function eq '/jslib') {
	    &return_jslib ;
	}

    }


    &HTMLdie("Sorry, no such function as //". &HTMLescape("$family$function."),
	     '', '404 Not Found') ;

}


#--------------------------------------------------------------------------
#    Support routines for x-proxy
#--------------------------------------------------------------------------

# Initiate a browsing session. Formerly in the separate program startproxy.cgi.
sub startproxy {
    my(%in)= &getformvars() ;  # must use () or will pass current @_!

    # Decode URL if it was encoded before transmission.
    $in{'URL'}= &wrap_proxy_decode($in{'URL'})
	if $ENCODE_URL_INPUT && $in{'URL'}=~ s/^\x01// ;

    $in{'URL'}=~ s/^\s+|\s+$//g ;    # strip leading or trailing spaces

    &show_start_form('Enter the URL you wish to visit in the box below.')
	if $in{'URL'} eq '' ;

    # Handle (badly) the special case of "mailto:" URLs, which don't have "://".
    &unsupported_warning($in{URL}) if $in{URL}=~ /^mailto:/i ;

    # Parse input URI into components, using a regex similar to this one in
    #   RFC 2396:  ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
    # Here, $query and $fragment include their initial "?" and "#"  chars,
    #   and $scheme is undefined if there's no "://" .
    my($scheme, $authority, $path, $query, $fragment)=
	$in{URL}=~ m{^(?:([^:/?#]+)://)?([^/?#]*)([^?#]*)(\?[^#]*)?(#.*)?$} ;
    $scheme= lc($scheme) ;
    $path= '/' if $path eq '' ;

    # Parse $authority into username/password, hostname, and port-string.
    my($auth, $host, $portst)= $authority=~ /^([^@]*@)?([^:@]*)(:[^@]*)?$/ ;

    &show_start_form('The URL you entered has an invalid host name.', $in{URL})
	if !defined($host) ;

    $host= lc($host) ;   # must be after testing defined().

    &show_start_form('The URL must contain a valid host name.', $in{URL})
	if $host eq '' ;

    # Scheme defaults to FTP if host begins with "ftp.", else to HTTP.
    $scheme= ($host=~ /^ftp\./i)  ? 'ftp'  : 'http'   if $scheme eq '' ;

    &show_start_form('Sorry, only HTTP and FTP are currently supported.', $in{URL})
	unless $scheme=~ /^(http|https|ftp|x-proxy)$/ ;

    # Convert integer hostnames like 3467251275 to a.b.c.d format.
    # This is for big-endian; reverse the list for little-endian.
    $host= join('.', $host>>24 & 255, $host>>16 & 255, $host>>8 & 255, $host & 255)
	if $host=~ /^\d+$/ ;

    # Allow shorthand for hostnames-- if no "." is in it, then add "www"+"com"
    #   or "ftp"+"com".  Don't do it if the host already exists on the LAN.
    if ($scheme eq 'http') {
	$host= "www.$host.com"  if ($host!~ /\./) && !gethostbyname($host) ;
    } elsif ($scheme eq 'ftp') {
	# If there's username/password embedded (which you REALLY shouldn't do),
	#   then don't risk sending that to an unintended host.
	$host= "ftp.$host.com"
	    if ($auth eq '') && ($host!~ /\./) && !gethostbyname($host) ;
    }

    # Force $portst to ":" followed by digits, or ''.
    ($portst)= $portst=~ /^(:\d+)/ ;

    # Reassemble $authority after all changes are complete.
    $authority= $auth . $host . $portst ;

    # Prepend flag segment of PATH_INFO
    # This "erroneously" sets flags to "000000" when user config is not
    #   allowed, but it doesn't really affect anything.
    $url_start=~ s#[^/]*/$## ;   # remove old flag segment from $url_start
    $url_start.= &pack_flags(@in{'rc', 'rs', 'fa', 'br', 'if'}, $is_in_frame, '') . '/' ;

    &redirect_to( $url_start . &wrap_proxy_encode("$scheme://$authority$path$query") . $fragment ) ;
}



# Create the flag segment of PATH_INFO from the given flags, not including
#   slashes.  Result should be a valid path segment (i.e. alphanumeric and
#   certain punctuation OK, but no slashes or white space).
# This routine defines the structure of the flag segment.
# Note that an $expected_type of '' explicitly means that no type in particular
#   is expected, which will be the case for almost all resources.
# Note that any unrecognized MIME type (i.e. no element in %MIME_TYPE_ID)
#   is treated the same as '', i.e. element #0 -> "A" .
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_pack_flags() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   (almost) a Perl-to-JavaScript translation of this routine.
sub pack_flags {
    my($remove_cookies, $remove_scripts, $filter_ads, $hide_referer,
	  $insert_entry_form, $is_in_frame, $expected_type)= @_ ;
    my($flags) ;

    # Force all values to boolean for this format.
    $flags=   $remove_cookies     ? 1  : 0  ;
    $flags.=  $remove_scripts     ? 1  : 0  ;
    $flags.=  $filter_ads         ? 1  : 0  ;
    $flags.=  $hide_referer       ? 1  : 0  ;
    $flags.=  $insert_entry_form  ? 1  : 0  ;
    $flags.=  $is_in_frame        ? 1  : 0  ;

    # Add MIME type flag, packed into one character.
    $expected_type= pack('C', $MIME_TYPE_ID{lc($expected_type)}) ;
    $expected_type=~ tr#\x00-\x3f#A-Za-z0-9+-# ; # almost same as base64 chars

    $flags.=  $expected_type ;

    return $flags ;
}


# The reverse of pack_flags()-- given a flag segment from PATH_INFO, break
#   out all flag info.  The return list should match the input list for
#   pack_flags().
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_unpack_flags() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   (almost) a Perl-to-JavaScript translation of this routine.
sub unpack_flags {
    my($flags)= @_ ;
    my($remove_cookies, $remove_scripts, $filter_ads, $hide_referer,
       $insert_entry_form, $is_in_frame, $expected_type) ;

    ($remove_cookies, $remove_scripts, $filter_ads, $hide_referer,
     $insert_entry_form, $is_in_frame, $expected_type)=
	split(//, $flags) ;

    # Force all flags to valid values (currently all are 1 or 0).
    $remove_cookies=    $remove_cookies     ? 1  : 0  ;
    $remove_scripts=    $remove_scripts     ? 1  : 0  ;
    $filter_ads=        $filter_ads         ? 1  : 0  ;
    $hide_referer=      $hide_referer       ? 1  : 0  ;
    $insert_entry_form= $insert_entry_form  ? 1  : 0  ;
    $is_in_frame=       $is_in_frame        ? 1  : 0  ;

    # Extract expected MIME type from final one-character flag
    $expected_type=~ tr#A-Za-z0-9+-#\x00-\x3f# ;
    $expected_type= $ALL_TYPES[unpack('C', $expected_type)] ;

    return ($remove_cookies, $remove_scripts, $filter_ads, $hide_referer,
	    $insert_entry_form, $is_in_frame, $expected_type) ;
}


#--------------------------------------------------------------------------
#    Cookie routines
#--------------------------------------------------------------------------

# As of version 1.3, cookies are now a general mechanism for sending various
#   data to the proxy.  So far that's only authentication info and actual
#   cookies, but more functions could be added.  The new scheme essentially
#   divides up the cookie name space to accommodate many categories.
# Explanation: Normally, a cookie is uniquely identified ("keyed") by the
#   domain, path, and name, but for us the domain and path will always be
#   that of the proxy script, so we need to embed all "key" information into
#   the cookie's name.  Here, the general format for a cookie's name is
#   several fields, joined by ";".  The first field is always a cookie type
#   identifier, like "AUTH" or "COOKIE", and the remaining fields vary
#   according to cookie type.  This compound string is then URL-encoded as
#   necessary (cookie names and values can't contain semicolons, commas, or
#   white space).  The cookie's value contains whatever you need to store,
#   also URL-encoded as necessary.

# A general bug in cookie routines-- ports are not considered, which may
#   matter for both AUTH and COOKIE cookies.  It only matters when two ports
#   on the same server are being used.


# Returns all info we need from cookies.  Right now, that means one composite
#   cookie with all cookies that match the domain and path (and no others!),
#   and an %auth hash to look up auth info by server and realm.  Essentially,
#   this undoes the transformation done by the cookie creation routines.
# @auth is used instead of %auth for slight speedup.
# See notes where the various cookies are created for descriptions of their
#   format; currently, that's in cookie_to_client() and auth_cookie().
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_cookie_from_client() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of part of this routine.
sub parse_cookie {
    my($cookie, $target_path, $target_server, $target_scheme)= @_ ;
    my($name, $value, $type, @n,
       $cname, $path, $domain, $cvalue, $secure, @matches, %pathlen,
       $realm, $server, @auth) ;

    foreach ( split(/\s*;\s*/, $cookie) ) {
	($name, $value)= split(/=/, $_, 2) ;     # $value may contain "="
	$name= &cookie_decode($name) ;
	$value= &cookie_decode($value) ;
	($type, @n)= split(/;/, $name) ;
	if ($type eq 'COOKIE') {
	    ($cname, $path, $domain)= @n ;
	    $domain= lc($domain) ;
	    ($cvalue, $secure)= split(/;/, $value) ;
	    next if $secure && ($target_scheme ne 'https') ;

	    # According to the cookie spec, a cookie domain equal to a "."
	    #   plus the target domain should not match, but browsers treat
	    #   it as if it does, so we do the same here.
	    if ( ($target_server=~ /\Q$domain\E$/i or (lc('.'.$target_server) eq lc($domain)) )
		 && $target_path=~ /^\Q$path\E/ )
	    {
		# Cookies are always supposed to have a name, but some servers
		#   don't follow this. and at least one browser treats it as
		#   cookie with only "value" instead of "name=value".  So,
		#   we follow that here, for these errant cookies.
		push(@matches, ($cname ne '' ? $cname.'='.$cvalue : $cvalue)) ;
		$pathlen{$matches[$#matches]}= length($path) ;
	    }
	} elsif ($type eq 'AUTH') {
	    # format of auth cookie's name is AUTH;$enc_realm;$enc_server
	    ($realm, $server)= @n ;
	    $realm=~  s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;
	    $server=~ s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;
	    push(@auth, $realm, $value)  if  $server eq $target_server ;
	}
    }

    # More specific path mappings (i.e. longer paths) should be sent first.
    $cookie= join('; ', sort { $pathlen{$b} <=> $pathlen{$a} } @matches) ;

    return $cookie, @auth ;
}


# Old notes:
#
# Cookie support:  The trick is how to send a cookie back to the client that
#   it will return for appropriate hosts.  Given that the target URL may be
#   encoded, and the client can't always tell where the target URL is, the
#   only way to do that is to get *all* the cookies from the client and send
#   along the matching ones.  If the client has a lot of cookies through the
#   proxy, this could conceivably be a problem.  Oh well, it works for the
#   limited amount I've tested.
# Here, we transform the cookie from the server into something the client
#   will always send back to us, and embed the real server/path info in the
#   name of the name-value pair, since the cookie is uniquely identified by
#   the domain, path, and name.  Upon return from the client, we split the
#   name back into its original fields.
# One way to get around *some* of the all-cookies-all-the-time problem,
#   *sometimes*, may be possible to program with the following approach:
#   First, the target URL must be "encoded" (in proxy_encode()) in a way
#   that it resembles a path.  For example, the default "://" --> "/"
#   encoding does this.  Then, let the cookies go back to the client with
#   the target paths still intact.  This would only work when the cookie
#   domain is the default, i.e. the source host.  Check other possibilities
#   carefully, too, but I think you could get it to work somehow.
# Question-- is the port supposed to be used in the domain field?
#   Everything here assumes not, which is conceivably a security risk.

# Transform one cookie into something the client will send back through
#   the script, but still has all the needed info.  Returns a transformed
#   cookie, or undef if the cookie is invalid (e.g. comes from
#   the wrong host).
# A cookie is uniquely identified by the domain, path, and name, so this
#   transformation embeds the path and domain info into the "name".
# This doesn't handle multiple comma-separated cookies-- possible, but
#   which seems a slight contradiction between the HTTP spec (section 4.2
#   of both HTTP 1.0 and 1.1 specs) and the cookie spec at
#   http://www.netscape.com/newsref/std/cookie_spec.html.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_cookie_to_client() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.

sub cookie_to_client {
    my($cookie, $source_path, $source_server)= @_ ;
    my($name, $value, $expires_clause, $path, $domain, $secure_clause) ;
    my($new_name, $new_value, $new_cookie) ;

    # Start last four regexes with ";" to avoid extracting from name=value.
    # Cookie values aren't supposed to have commas, per the spec, but at least
    #   one site (go.com, using the Barista server) violates this.  So for now,
    #   allow commas in $value.
    #($name, $value)=   $cookie=~ /^\s*([^=;,\s]*)\s*=?\s*([^;,\s]*)/ ;
    ($name, $value)=   $cookie=~ /^\s*([^=;,\s]*)\s*=?\s*([^;\s]*)/ ;
    ($expires_clause)= $cookie=~ /;\s*(expires\s*=[^;]*)/i ;
    ($path)=     $cookie=~ /;\s*path\s*=\s*([^;,\s]*)/i ;  # clash w/ ;-params?
    ($domain)=         $cookie=~ /;\s*domain\s*=\s*([^;,\s]*)/i ;
    ($secure_clause)=  $cookie=~ /;\s*(secure\b)/i ;

    # Path defaults to either the path of the URL that sent the cookie, or '/'.
    #   See comments above $COOKIE_PATH_FOLLOWS_SPEC for more details.
    $path=  $COOKIE_PATH_FOLLOWS_SPEC  ? $source_path  : '/'  if $path eq '' ;

    # Domain must be checked for validity: defaults to the server that sent
    #   the cookie; otherwise, must match end of that server name, and must
    #   contain at least two dots if in one of these seven top-level domains,
    #   three dots otherwise.
    # As it turns out, hostnames ending in extraneous dots, like
    #   "slashdot.org.." resolve to the hostname without the dots.  So we
    #   need to guard against malicious cookie servers getting around the
    #   two/three-dot requirement this way.
    # Unfortunately, the three-dot rule is not always followed; consider
    #   for example the domain "google.de".  Probably because of such domains,
    #   browsers seem to only require two dots.  Thus, do the same here,
    #   unless $RESPECT_THREE_DOT_RULE is set.
    # Browsers also allow domains such as "example.com", i.e. missing the
    #   leading dot.  :P  So, prepend a dot in such situations; only do this
    #   if the 3-dot rule is already relaxed.
    if ($domain eq '') {
	$domain= $source_server ;
    } else {
	$domain=~ s/\.*$//g ;  # removes trailing dots!
	$domain=~ tr/././s ;   # ... and double dots for good measure.
	# Allow $domain to match domain-minus-leading-dot (erroneously),
	#   because that's how browsers do it.
	return undef
	    if ($source_server!~ /\Q$domain\E$/) and ('.'.$source_server ne $domain) ;
	if ($RESPECT_THREE_DOT_RULE) {
	    return(undef) unless
		( ( ($domain=~ tr/././) >= 3 ) ||
		  ( ($domain=~ tr/././) >= 2 &&
		    $domain=~ /\.(com|edu|net|org|gov|mil|int)$/i )
		) ;
	} else {
	    if (($domain=~ tr/././) < 2) {
		return undef  if $domain=~ /^\./ ;
		$domain= '.' . $domain ;
		return undef  if ($domain=~ tr/././) < 2 ;
	    }
	}
    }


    # This is hereby the transformed format: name is COOKIE;$name;$path;$domain
    #   (the three values won't already have semicolons in them); value is
    #   $value;$secure_clause .  Both name and value are then cookie_encode()'d.
    #   The name contains everything that identifies the cookie, and the value
    #   contains all info we might care about later.
    $new_name= &cookie_encode("COOKIE;$name;$path;$domain") ;

    # New value is "$value;$secure_clause", then cookie_encode()'d.
    $new_value= &cookie_encode("$value;$secure_clause") ;


    # Change $expires_clause to make it a session cookie if so configured.
    # Don't do so if the cookie expires in the past, which means a deleted cookie.
    if ($SESSION_COOKIES_ONLY && $expires_clause ne '') {
	my($expires_date)= $expires_clause=~ /^expires\s*=\s*(.*)$/i ;
	$expires_clause= ''  if &date_is_after($expires_date, $now) ;
    }


    # Create the new cookie from its components, removing the empty ones.
    # The new domain is this proxy server, which is the default if it is not
    #   specified.
    $new_cookie= join('; ', grep(length,
				 $new_name . '=' . $new_value,
				 $expires_clause,
				 'path=' . $ENV_SCRIPT_NAME . '/',
				 ($RUNNING_ON_SSL_SERVER ? ('secure') : () )
		     )) ;
    return $new_cookie ;

}



# Returns a cookie that contains authentication information for a particular
#   realm and server.  The format of the cookie is:  The name is
#   AUTH;$URL_encoded_realm;$URL_encoded_server, and the value is the
#   base64-encoded "$username:$password" needed for the Authorization: header.
#   On top of that, both name and value are cookie_encode()'d.
# Leave the "expires" clause out, which means the cookie lasts as long as
#   the session, which is what we want.
sub auth_cookie {
    my($username, $password, $realm, $server)= @_ ;

    $realm=~ s/(\W)/ '%' . sprintf('%02x',ord($1)) /ge ;
    $server=~ s/(\W)/ '%' . sprintf('%02x',ord($1)) /ge ;

    return join('', &cookie_encode("AUTH;$realm;$server"), '=',
		    &cookie_encode(&base64("$username:$password")),
		    '; path=' . $ENV_SCRIPT_NAME . '/' ) ;

}



# Generates a set of cookies that will delete the cookies contained in the
#   given cookie strings (e.g. from HTTP_COOKIE).  This is done by giving
#   each cookie an expiration time in the past, and setting their values
#   to "" for good measure.
# The input @cookies can each be a list of cookies separated by ";" .  The
#   cookies themselves can be either "name=value" or just "name".
# The return value is one long string of multiple "Set-Cookie:" headers.
# Slight quirk in Netscape and other browsers-- if cookie expiration is
#   set to the epoch time of "01-Jan-1970 00:00:00 GMT" (meaning second #0),
#   the cookie is treated as a session cookie instead of a deleted cookie.
#   Using second #1, i.e. "01-Jan-1970 00:00:01 GMT", causes the cookies to
#   be correctly deleted.

sub cookie_clearer {
    my(@cookies)= @_ ;   # may be one or more lists of cookies
    my($ret, $cname) ;

    foreach (@cookies) {
	foreach $cname ( split(/\s*;\s*/) ) {
	    $cname=~ s/=.*// ;      # change "name=value" to "name"
	    $ret.= "Set-Cookie: $cname=; expires=Thu, 01-Jan-1970 00:00:01 GMT; "
		 . "path=$ENV_SCRIPT_NAME/\015\012" ;
	}
    }
    return $ret ;
}


#--------------------------------------------------------------------------
#    Utility routines
#--------------------------------------------------------------------------

# The following subroutine looks messy, but can be used to open any
#   TCP/IP socket in any Perl program.  Except for the &HTMLdie() part.
# Typeglobbing has trouble with mod_perl and tied filehandles, so pass socket
#   handle as a string instead (e.g. 'S').
# Older versions created the packet structure with the old "pack('S n a4 x8')"
#   method.  However, some OS's (such as BSDI) vary from this, and it probably
#   won't work with IPv6 either.  So now we use the more general functions,
#   like pack_sockaddr_in() from Socket.pm.  (IPv6 support may require other
#   changes too.)
sub newsocketto {
    my($S, $host, $port)= @_ ;
    my($hostaddr, $remotehost) ;

    # If $host is long integer like 3467251275, break it into a.b.c.d format.
    # This is for big-endian; reverse the list for little-endian.
    $host= join('.', $host>>24 & 255, $host>>16 & 255, $host>>8 & 255,
		     $host & 255)
	if $host=~ /^\d+$/ ;

    # Create the remote host data structure, from host name or IP address.
    # Note that inet_aton() handles both alpha names and IP addresses.
    $hostaddr= inet_aton($host)
	|| &HTMLdie("Couldn't find address for $host: $!") ;
#    $remotehost= pack('S n a4 x8', AF_INET, $port, $hostaddr) ;
    $remotehost= pack_sockaddr_in($port, $hostaddr) ;

    # If the target IP address is a banned host or network, die appropriately.
    # This assumes that IP address structs have the most significant byte first.
    # This is a quick addition that will be fleshed out in a later version.
    # This may not work with IPv6, depending on what inet_aton() returns then.
    for (@BANNED_NETWORK_ADDRS) {
	&banned_server_die() if $hostaddr=~ /^$_/ ;   # No URL forces a die
    }

    # Create the socket and connect to the remote host
    no strict 'refs' ;   # needed to use $S as filehandle
    socket($S, AF_INET, SOCK_STREAM, (getprotobyname('tcp'))[2])
	|| &HTMLdie("Couldn't create socket: $!") ;
    connect($S, $remotehost)
	|| &HTMLdie("Couldn't connect to $host:$port: $!") ;
    select((select($S), $|=1)[0]) ;      # unbuffer the socket
}


# Read a specific number of bytes from a socket, looping if necessary.
# Returns all bytes read (possibly less than $length), or undef on error.
# Typeglobbing *STDIN into *S doesn't seem to work with mod_perl 1.21,
#   so pass socket handle as a string instead (e.g. 'STDIN').
# Using *S, the read() below immediately fails under mod_perl.
sub read_socket {
#    local(*S, $length)= @_ ;
    my($S, $length)= @_ ;
    my($ret, $numread, $thisread) ;

    #$numread= 0 ;
    no strict 'refs' ;   # needed to use $S as filehandle

    while (    ($numread<$length)
#	    && ($thisread= read(S, $ret, $length-$numread, $numread) ) )
	    && ($thisread= read($S, $ret, $length-$numread, $numread) ) )
    {
	$numread+= $thisread ;
    }
    return undef unless defined($thisread) ;

    return $ret ;
}


# Read a chunked body and footers from a socket; assumes that the
#   Transfer-Encoding: is indeed chunked.
# Returns the body and footers (which should then be appended to any
#   previous headers), or undef on error.
# For details of chunked encoding, see the HTTP 1.1 spec, e.g. RFC 2616
#   section 3.6.1 .
sub get_chunked_body {
    my($S)= @_ ;
    my($body, $footers, $chunk_size, $chunk) ;
    local($_) ;
    local($/)= "\012" ;

    # Read one chunk at a time and append to $body.
    # Note that hex() will automatically ignore a semicolon and beyond.
    no strict 'refs' ;     # needed to use $S as filehandle
    $body= '' ;            # to distinguish it from undef
    while ($chunk_size= hex(<$S>) ) {
	$body.= $chunk= &read_socket($S, $chunk_size) ;
	return undef unless length($chunk) == $chunk_size ;  # implies defined()
	$_= <$S> ;         # clear CRLF after chunk
    }

    # After all chunks, read any footers, NOT including the final blank line.
    while (<$S>) {
	last if /^(\015\012|\012)/  || $_ eq '' ;   # lines end w/ LF or CRLF
	$footers.= $_ ;
    }
    $footers=~ s/(\015\012|\012)[ \t]+/ /g ;       # unwrap long footer lines

    return wantarray  ? ($body, $footers)  : $body  ;
}



# This is a minimal routine that reads URL-encoded variables from a string,
#   presumably from something like QUERY_STRING.  If no string is passed,
#   it will read from either QUERY_STRING or STDIN, depending on
#   REQUEST_METHOD.  STDIN can't be read more than once for POST requests.
# It returns a hash.  In the event of multiple variables with the same name,
#   it concatenates the values into one hash element, delimiting with "\0".
# Returns undef on error.
sub getformvars {
    my($in)= @_ ;
    my(%in, $name, $value) ;

    # If no string is passed, read it from the usual channels.
    unless (defined($in)) {
	if ( ($ENV{'REQUEST_METHOD'} eq 'GET') ||
	     ($ENV{'REQUEST_METHOD'} eq 'HEAD') ) {
	    $in= $ENV{'QUERY_STRING'} ;
	} elsif ($ENV{'REQUEST_METHOD'} eq 'POST') {
	    return undef unless
		lc($ENV{'CONTENT_TYPE'}) eq 'application/x-www-form-urlencoded';
	    return undef unless defined($ENV{'CONTENT_LENGTH'}) ;
	    $in= &read_socket('STDIN', $ENV{'CONTENT_LENGTH'}) ;
	    # should we return undef if not all bytes were read?
	} else {
	    return undef ;   # unsupported REQUEST_METHOD
	}
    }

    foreach (split(/[&;]/, $in)) {
	s/\+/ /g ;
	($name, $value)= split('=', $_, 2) ;
	$name=~ s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;
	$value=~ s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;
	$in{$name}.= "\0" if defined($in{$name}) ;  # concatenate multiple vars
	$in{$name}.= $value ;
    }
    return %in ;
}



# For a given timestamp, returns a date in one of the following two forms,
#   depending on the setting of $use_dash:
#     "Wdy, DD Mon YYYY HH:MM:SS GMT"
#     "Wdy, DD-Mon-YYYY HH:MM:SS GMT"
# The first form is used in HTTP dates, and the second in Netscape's cookie
#   spec (although cookies sometimes use the first form, which seems to be
#   handled OK by most recipients).
# The first form is basically the date format in RFC 822 as updated in RFC
#   1123, except GMT is always used here.
sub rfc1123_date {
    my($time, $use_dash)= @_ ;
    my($s) =  $use_dash  ? '-'  : ' ' ;
    my(@t)= gmtime($time) ;

    return sprintf("%s, %02d$s%s$s%04d %02d:%02d:%02d GMT",
		   $WEEKDAY[$t[6]], $t[3], $MONTH[$t[4]], $t[5]+1900, $t[2], $t[1], $t[0] ) ;
}


# Returns true if $date1 is later than $date2.  Both parameters can be in
#   either rfc1123_date() format or the total-seconds format from time().
#   rfc1123_date() format is "Wdy, DD-Mon-YYYY HH:MM:SS GMT", possibly using
#   spaces instead of dashes.
# Returns undef if either date is invalid.
# A more general function would be un_rfc1123_date(), to take an RFC 1123 date
#   and return total seconds.
sub date_is_after {
    my($date1, $date2)= @_ ;
    my(@d1, @d2) ;

    # Trivial case when both are numeric.
    return ($date1>$date2)  if $date1=~ /^\d+$/ && $date2=~ /^\d+$/ ;

    # Get date components, depending on formats
    if ($date1=~ /^\d+$/) {
	@d1= (gmtime($date1))[3,4,5,2,1,0] ;
    } else {
	@d1= $date1=~ /^\w+,\s*(\d+)[ -](\w+)[ -](\d+)\s+(\d+):(\d+):(\d+)/ ;
	return undef unless @d1 ;
	$d1[1]= $UN_MONTH{lc($d1[1])} ;
	$d1[2]-= 1900 ;
    }
    if ($date2=~ /^\d+$/) {
	@d2= (gmtime($date2))[3,4,5,2,1,0] ;
    } else {
	@d2= $date2=~ /^\w+,\s*(\d+)[ -](\w+)[ -](\d+)\s+(\d+):(\d+):(\d+)/ ;
	return undef unless @d2 ;
	$d2[1]= $UN_MONTH{lc($d2[1])} ;
	$d2[2]-= 1900 ;
    }

    # Compare year, month, day, hour, minute, second in order.
    return ( ( $d1[2]<=>$d2[2] or $d1[1]<=>$d2[1] or $d1[0]<=>$d2[0] or
	       $d1[3]<=>$d2[3] or $d1[4]<=>$d2[4] or $d1[5]<=>$d2[5] )
	     > 0 ) ;
}



# Escape any &"<> chars to &xxx; and return resulting string.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_html_escape() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.
sub HTMLescape {
    my($s)= @_ ;
    $s=~ s/&/&amp;/g ;      # must be before all others
    $s=~ s/"/&quot;/g ;
    $s=~ s/</&lt;/g ;
    $s=~ s/>/&gt;/g ;
    return $s ;
}


# Unescape any &xxx; codes back to &"<> and return resulting string.
# Simplified version here; only includes &"<>.
# Some people accidentally leave off final ";", and some browsers support that
#   if the word ends there, so make the final ";" optional.
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_html_unescape() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.
sub HTMLunescape {
    my($s)= @_ ;
    $s=~ s/&quot\b;?/"/g ;
    $s=~ s/&lt\b;?/</g ;
    $s=~ s/&gt\b;?/>/g ;
    $s=~ s/&amp\b;?/&/g ;      # must be after all others
    return $s ;
}



# Base64-encode a string, except not inserting line breaks.
sub base64 {
    my($s)= @_ ;
    my($ret, $p, @c, $t) ;

    # Base64 padding is done with "=", but that's in the first 64 characters.
    #   So, use "@" as a placeholder for it until the tr/// statement.

    # For each 3 bytes, build a 24-bit integer and split it into 6-bit chunks.
    # Insert one or two padding chars if final substring is less than 3 bytes.
    while ($p<length($s)) {
	@c= unpack('C3', substr($s,$p,3)) ;
	$p+= 3 ;
	$t= ($c[0]<<16) + ($c[1]<<8) + $c[2] ;     # total 24-bit integer
	$ret.= pack('C4',     $t>>18,
			     ($t>>12)%64,
		    (@c>1) ? ($t>>6) %64 : 64,
		    (@c>2) ?  $t     %64 : 64 ) ;  # "@" is chr(64)
    }

    # Translate from bottom 64 chars into base64 chars, plus @ to = conversion.
    $ret=~ tr#\x00-\x3f@#A-Za-z0-9+/=# ;

    return $ret ;
}


# Opposite of base64() .
sub unbase64 {
    my($s)= @_ ;
    my($ret, $p, @c, $t, $pad) ;

    $pad++ if $s=~ /=$/ ;
    $pad++ if $s=~ /==$/ ;

    $s=~ tr#A-Za-z0-9+/##cd ;          # remove non-allowed characters
    $s=~ tr#A-Za-z0-9+/#\x00-\x3f# ;   # for speed, translate to \x00-\x3f

    # For each 4 chars, build a 24-bit integer and split it into 8-bit bytes.
    # Remove one or two chars from result if input had padding chars.
    while ($p<length($s)) {
	@c= unpack('C4', substr($s,$p,4)) ;
	$p+= 4 ;
	$t= ($c[0]<<18) + ($c[1]<<12) + ($c[2]<<6) + $c[3] ;
	$ret.= pack('C3', $t>>16, ($t>>8) % 256, $t % 256 ) ;
    }
    chop($ret) if $pad>=1 ;
    chop($ret) if $pad>=2 ;

    return $ret ;
}



# Convert a string from UTF-16 encoding to UTF-8.
sub un_utf16 {
    my($s)= @_ ;
    eval { require Encode ; import Encode 'from_to' } ;
    &no_Encode_die if $@ ;

    from_to($$s, "utf-16", "utf-8") ;  # converts in-place
}



# Read an entire file into a string and return it; return undef on error.
# Does NOT check for any security holes in $fname!
sub readfile {
    my($fname)= @_ ;
    my($ret) ;
    local(*F, $/) ;

    open(F, "<$fname") || return undef ;
    undef $/ ;
    $ret= <F> ;
    close(F) ;

    return $ret ;
}



# Simple, general-purpose HTTP client.  The HTTP client in http_get() is too
#   specialized and non-modular to use for anything but the primary resource.
# This leaves the connection open, i.e. a persistent connection, because that's
#   needed for the purpose this routine was written for (the external tests).
# This routine expects a pointer to a hash containing "host", "port", "socket",
#   and "open" elements, plus a $request_uri string.  In the hash, iff "open"
#   is false, then a new socket is opened, in the interest of persistent
#   connections.  "host", "port", and "socket" (a string name of a filehandle)
#   are assumed to be unchanging.
# Note that this HTTP client is missing many features, such as proxy support,
#   SSL support, and authentication.  Eventually, http_get() may be restructured
#   to be more modular and support what we need here.
# This is partially copied from http_get().  For more commenting, see that
#   routine, in the similar sections as below.
sub http_get2 {
    my($c, $request_uri)= @_ ;
    my($s, $status, $status_code, $headers, $body, $footers, $rin, $win, $num_tries) ;
    local($/)= "\012" ;

    no strict 'refs' ;    # needed for symbolic references

    # Using "$c->{socket}" causes syntax errors in some places, so alias it to $s.
    $s= $c->{socket} ;

    # For some reason, under mod_perl, occasionally the socket response is
    #   empty.  It may have something to do with the scope of the filehandles.
    #   Work around it with this hack-- if such occurs, retry the routine up
    #   to three times.
    RESTART: {
	# Create a new socket if a persistent one isn't lingering from last time.
	# Ideally we'd test eof() on the socket at the end of this routine, but
	#   that may only fail after many seconds.  So, here we assume the socket
	#   is still usable if it's not '' and if we can write to it.
	vec($win= '', fileno($s), 1)= 1 if defined(fileno($s)) ;
	if (!$c->{open} || !select(undef, $win, undef, 0)) {
	    &newsocketto($c->{socket}, $c->{host}, $c->{port}) ;
	    $c->{open}= 1 ;
	}

	# Print the simple request.
	print $s 'GET ', $request_uri, " HTTP/1.1\015\012",
		 'Host: ', $c->{host}, (($c->{port}==80)  ? ''  : ":$c->{port}"), "\015\012",
		 "\015\012" ;


	vec($rin= '', fileno($s), 1)= 1 ;
	select($rin, undef, undef, 60)
	    || &HTMLdie("No response from $c->{host}:$c->{port}") ;

	$status= <$s> ;

	# hack hack....
	unless ($status=~ m#^HTTP/#) {
	    $c->{open}= 0 ;
	    redo RESTART if ++$num_tries<3 ;
	    &HTMLdie("Invalid response from $c->{host}: [$status]") ;
	}
    }


    # Loop to get $status and $headers until we get a non-100 response.
    # See comments in http_get(), above the similar block.
    do {
	($status_code)= $status=~ m#^HTTP/\d+\.\d+\s+(\d+)# ;

	$headers= '' ;
	do {
	    $headers.= $_= <$s> ;    # $headers includes last blank line
	} until (/^(\015\012|\012)$/) || $_ eq '' ; #lines end w/ LF or CRLF

	$status= <$s> if $status_code == 100 ;  # re-read for next iteration
    } until $status_code != 100 ;

    # Unfold long header lines, a la RFC 822 section 3.1.1
    $headers=~ s/(\015\012|\012)[ \t]+/ /g ;

    # Read socket body depending on how length is determined; see RFC 2616 (the
    #   HTTP 1.1 spec), section 4.4.
    if ($headers=~ /^Transfer-Encoding:[ \t]*chunked\b/mi) {
	($body, $footers)= &get_chunked_body($s) ;
	&HTMLdie(&HTMLescape("Error reading chunked response from $c->{host} ."))
	    unless defined($body) ;
	$headers=~ s/^Transfer-Encoding:[^\012]*\012?//mig ;
	$headers=~ s/^(\015\012|\012)/$footers$1/m ;

    } elsif ($headers=~ /^Content-Length:[ \t]*(\d+)/mi) {
	$body= &read_socket($s, $1) ;

    } else {
	undef $/ ;
	$body= <$s> ;  # ergo won't be persistent connection
	close($s) ;
	$c->{open}= 0 ;
    }

    # If server doesn't support persistent connections, then close the socket.
    # We would test eof($s) here, but that causes a long wait.
    if ($headers=~ /^Connection:.*\bclose\b/mi || $status=~ m#^HTTP/1\.0#) {
	close($s) ;
	$c->{open}= 0 ;
    }

    return $body ;
}



#--------------------------------------------------------------------------
#    Output routines
#--------------------------------------------------------------------------


# Returns the complete HTML to be inserted at the top of a page, which may
#   consist of the URL entry form and/or a custom insertion in $INSERT_HTML
#   or $INSERT_FILE.
# As an important side effect, both %IN_CUSTOM_INSERTION and %in_mini_start_form
#   are set in set_custom_insertion() and mini_start_form(), respectively.
#   These are used later to handle certain JavaScript.
# Note that any insertion should not have any relative URLs in it, because
#   there's no good base URL to resolve them with.  See the comments where
#   $INSERT_HTML and $INSERT_FILE are set.
# Use the global, persistent variable $CUSTOM_INSERTION to hold the custom
#   insertion from $INSERT_HTML or $INSERT_FILE.  Set it the first time it's
#   needed (every time for a CGI script, once for a mod_perl script).  This
#   minimizes how often an inserted file is opened and read.
# $INSERT_HTML takes precedence over $INSERT_FILE.
# The inserted entry form is never anonymized.
sub full_insertion {
    my($URL, $in_top_frame)= @_ ;
    my($form, $insertion) ;
    $form= &mini_start_form($URL, $in_top_frame) if $e_insert_entry_form ;

    if (($INSERT_HTML ne '') || ($INSERT_FILE ne '')) {
	&set_custom_insertion if $CUSTOM_INSERTION eq '' ;

	# The insertion should not have relative URLs, but in case it does
	#   provide a base URL of this script for lack of anything better.
	#   It's erroneous, but it avoids unpredictable behavior.  $url_start
	#   is also required for proxify_html(), but it has already been set.
	# We can't do this only once to initialize, we must do this for each
	#   run, because user config flags might change from run to run.
	# NOTE!  If we don't use 0 in &proxify_html() here we'll recurse!
	if ($ANONYMIZE_INSERTION) {
	    local($base_url)= $script_url ;
	    &fix_base_vars ;
	    $insertion= &proxify_html(\$CUSTOM_INSERTION,0) ;
	} else {
	    $insertion= $CUSTOM_INSERTION ;
	}
    }

    return $FORM_AFTER_INSERTION   ? $insertion . $form   : $form . $insertion ;
}


# Set $CUSTOM_INSERTION from the correct source.  Also set %IN_CUSTOM_INSERTION
#   according to its contents.  This is needed for JavaScript handling, to
#   handle arrays like document.forms[] etc. that reference page elements in
#   order.  Insertions at the top of the page throw these arrays off, so we
#   must compensate by incrementing those subscripts by the number of forms,
#   links, etc. in the top insertion.  The counts in %IN_CUSTOM_INSERTION are
#   used for the custom insertion; elements in the inserted entry form are
#   handled elsewhere.
# The relevant arrays in the document object are applets[], embeds[], forms[],
#   ids[], layers[], anchors[], images[], and links[].  The first five
#   correspond directly to HTML tag names; the last three must be handled
#   individually.  The patterns below to detect <a href> and <a name> aren't
#   exact, but should work in almost all cases.  The pattern to detect tags
#   isn't even perfect-- it fails on script blocks, etc.  However, errors would
#   be rare and fairly harmless, and this whole situation is pretty rare anyway.
sub set_custom_insertion {
    return if $CUSTOM_INSERTION ne '' ;
    return unless ($INSERT_HTML ne '') || ($INSERT_FILE ne '') ;

    # Read $CUSTOM_INSERTION from the appropriate source.
    $CUSTOM_INSERTION= ($INSERT_HTML ne '')   ? $INSERT_HTML  : &readfile($INSERT_FILE) ;

    # Now, set counts in %IN_CUSTOM_INSERTION.
    %IN_CUSTOM_INSERTION= () ;
    foreach (qw(applet embed form id layer)) {
	$IN_CUSTOM_INSERTION{$_.'s'}++ while $CUSTOM_INSERTION=~ /<\s*$_\b/gi ;
    }
    $IN_CUSTOM_INSERTION{anchors}++ while $CUSTOM_INSERTION=~ /<\s*a\b[^>]*\bname\s*=/gi ;
    $IN_CUSTOM_INSERTION{links}++   while $CUSTOM_INSERTION=~ /<\s*a\b[^>]*\bhref\s*=/gi ;
    $IN_CUSTOM_INSERTION{images}++  while $CUSTOM_INSERTION=~ /<\s*img\b/gi ;
}



# Print the footer common to most error responses
sub footer {
    my($rightlink)= $NO_LINK_TO_START
	? ''
	: qq(<a href="$script_url"><i>Restart</i></a>) ;

    print <<EOF ;
<p>
<hr>
<table width="100%"><tr>
<td align=left>
<a href="http://www.jmarshall.com/tools/cgiproxy/"><i>CGIProxy 2.1beta10</i></a>
</td>
<td align=right>
$rightlink
</td>
</tr></table>
<p>
</body>
</html>
EOF
}



# Return the contents of the top frame, i.e. the one with whatever insertion
#   we have-- the entry form and/or the inserted HTML or file.
sub return_top_frame {
    my($enc_URL)= @_ ;
    my($body, $insertion) ;
    my($date_header)= &rfc1123_date($now, 0) ;

    # Redirect any links to the top frame.  Make sure any called routines know
    #   this by setting $base_unframes.  Also use $url_start_noframe to make
    #   sure any links with a "target" attribute that are followed from an
    #   anonymized insertion have the frame flag unset, and therefore have
    #   their own correct insertion.
    local($base_unframes)= 1 ;
    local($url_start)= $url_start_noframe ;

    $body= &full_insertion(&wrap_proxy_decode($enc_URL), 1) ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-Type: text/html

<html>
<head><base target="_top"></head>
<body>
$body
</body>
</html>
EOF

    goto EXIT ;
}


# Return a frame document that puts the insertion in the top frame and the
#   actual page in the lower frame.  Both of these will have the is_in_frame
#   flag set.
# MUST be careful to set $is_in_frame flag!  Else will recurse!
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_return_frame_doc() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   mostly a Perl-to-JavaScript translation of this routine.
sub return_frame_doc {
    my($enc_URL, $title)= @_ ;
    my($qs_URL, $top_URL, $page_URL) ;
    my($date_header)= &rfc1123_date($now, 0) ;

    ($qs_URL= $enc_URL) =~ s/([^\w.-])/ '%' . sprintf('%02x',ord($1)) /ge ;
    $top_URL= &HTMLescape($url_start_inframe
			. &wrap_proxy_encode('x-proxy://frames/topframe?URL=' . $qs_URL) ) ;
    $page_URL= &HTMLescape($url_start_inframe . $enc_URL) ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-Type: text/html

<html>
<head>
<title>$title</title>
</head>
<frameset rows="$INSERTION_FRAME_HEIGHT,*">
    <frame src="$top_URL">
    <frame src="$page_URL" name="_proxy_jslib_main_frame">
</frameset>
</html>
EOF

    goto EXIT ;
}



# When an image should be blanked, returns either a transparent 1x1 GIF or
#   a 406 result ("Not Acceptable").
sub skip_image {
    &return_empty_gif if $RETURN_EMPTY_GIF ;

    my($date_header)= &rfc1123_date($now, 0) ;
    print "$HTTP_1_X 406 Not Acceptable\015\012${NO_CACHE_HEADERS}Date: $date_header\015\012\015\012" ;
    goto EXIT ;
}


# Return a 1x1 transparent GIF.  Yes, that's an inlined 43-byte GIF.
sub return_empty_gif {
    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-Type: image/gif
Content-Length: 43

GIF89a\x01\0\x01\0\x80\0\0\0\0\0\xff\xff\xff\x21\xf9\x04\x01\0\0\0\0\x2c\0\0\0\0\x01\0\x01\0\x40\x02\x02\x44\x01\0\x3b
EOF

    goto EXIT ;
}



# Returns a 302 redirection response to $location, with optional extra headers.
# $other_headers must be complete with final "\015\12", etc.
sub redirect_to {
    my($location, $other_headers)= @_ ;
    print "$HTTP_1_X 302 Moved\015\012", $NO_CACHE_HEADERS,
	  "Date: ", &rfc1123_date($now,0), "\015\012",
	  $other_headers,
	  "Location: $location\015\012\015\012" ;

    goto EXIT ;
}



# Present the initial entry form
sub show_start_form {
    my($msg, $URL)= @_ ;
    my($method, $action, $flags, $cookies_url, $safe_URL, $jslib_block,
       $onsubmit, $onload) ;
    my($date_header)= &rfc1123_date($now, 0) ;

    $msg= "\n<h1><font color=green>$msg</font></h1>"  if $msg ne '' ;

    $method= $USE_POST_ON_START   ? 'post'   : 'get' ;

    $action=      &HTMLescape( $url_start . &wrap_proxy_encode('x-proxy://start') ) ;
    $cookies_url= &HTMLescape( $url_start . &wrap_proxy_encode('x-proxy://cookies/manage') ) ;
    $safe_URL= &HTMLescape($URL) ;

    # Encode the URL before submitting, if so configured.  Start it with "\x01"
    #   to indicate that it's encoded ("\0" isn't handled well by all browsers).
    if ($ENCODE_URL_INPUT) {
	$jslib_block= '<script src="'
		    . &HTMLescape($url_start . &wrap_proxy_encode('x-proxy://scripts/jslib'))
		    . "\"></script>\n" ;
	$onsubmit= q( onsubmit="if (!this.URL.value.match(/^\\x01/)) this.URL.value= '\x01'+_proxy_jslib_wrap_proxy_encode(this.URL.value) ; return true") ;
	$onload= q( onload="if (document.URLform.URL.value.match(/^\\x01/)) document.URLform.URL.value= _proxy_jslib_wrap_proxy_decode(document.URLform.URL.value.replace(/\\x01/, ''))") ;
    } else {
	$jslib_block= $onsubmit= $onload= '' ;
    }

    # Include checkboxes if user config is allowed.
    if ($ALLOW_USER_CONFIG) {
	my($rc_on)= $e_remove_cookies     ? ' checked'  : '' ;
	my($rs_on)= $e_remove_scripts     ? ' checked'  : '' ;
	my($fa_on)= $e_filter_ads         ? ' checked'  : '' ;
	my($br_on)= $e_hide_referer       ? ' checked'  : '' ;
	my($if_on)= $e_insert_entry_form  ? ' checked'  : '' ;
	$flags= <<EOF ;
<br><input type=checkbox id="rc" name="rc"$rc_on><label for="rc"> Remove all cookies (except certain proxy cookies)</label>
<br><input type=checkbox id="rs" name="rs"$rs_on><label for="rs"> Remove all scripts (recommended for anonymity)</label>
<br><input type=checkbox id="fa" name="fa"$fa_on><label for="fa"> Remove ads</label>
<br><input type=checkbox id="br" name="br"$br_on><label for="br"> Hide referrer information</label>
<br><input type=checkbox id="if" name="if"$if_on><label for="if"> Show URL entry form</label>
EOF
    }

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head>
$jslib_block
<title>Start Using CGIProxy</title>
</head>
<body$onload>
$msg
<h1>CGIProxy</h1>
<p>Start browsing through this CGI-based proxy by entering a URL below.
Only HTTP and FTP URLs are supported.  Not all functions will work
(e.g. some Java applets), but most pages will be fine.

<form name="URLform" action="$action" method=$method$onsubmit>
<input name="URL" size=66 value="$safe_URL">
$flags
<p><input type=submit value="   Begin browsing   ">
</form>

<h3><a href="$cookies_url">Manage cookies</a></h3>
EOF

    &footer ;
    goto EXIT ;
}



# Returns a mini version of the start form, as a string.  It requires
#   $url_start and $URL to be already set.
# To support this correctly in a frame, point it to target="_top" and use
#   $url_start_noframe in the action.
# Put the cookie management in the full window, and when the user "returns to
#   browsing" the frame flag will cause the frames to reload correctly.
sub mini_start_form {
    my($URL, $in_top_frame)= @_ ;
    my($method, $action, $flags, $table_open, $table_close,
       $cookies_url, $from_param, $safe_URL, $onsubmit, $onfocus) ;

    $method= $USE_POST_ON_START   ? 'post'   : 'get' ;
    $action= &HTMLescape( $url_start_noframe . &wrap_proxy_encode('x-proxy://start') ) ;
    $safe_URL= &HTMLescape($URL) ;

    # In "manage cookies" link, provide a way to return to page user came from.
    # Exclude certain characters from URL-encoding, to make URL more readable
    #   in the event it's not obscured.  Unfortunately, ":" and "/" are
    #   reserved in query component (RFC 2396), so we can't exclude them.
    # Don't confusing "URL-encoding" with the "encoding of the URL"!  The
    #   latter uses proxy_encode().  Unfortunate language.
    $from_param= &wrap_proxy_encode($URL) ;   # don't send unencoded URL
    $from_param=~ s/([^\w.-])/ '%' . sprintf('%02x',ord($1)) /ge ;
    $cookies_url= $url_start_noframe . &wrap_proxy_encode('x-proxy://cookies/manage')
		. '?from=' . $from_param ;
    $cookies_url= &HTMLescape($cookies_url) ;

    # Create "UP" link.
    my($scheme_authority, $up_path)= $URL=~ m{^([^:/?#]+://[^/?#]*)([^?#]*)} ;
    $up_path=~ s#[^/]*.$##s ;
    my($safe_up_URL)= &HTMLescape( $url_start_noframe . &wrap_proxy_encode("$scheme_authority$up_path") ) ;
    my($up_link)= $up_path ne ''
	? qq(&nbsp;&nbsp;<a href="$safe_up_URL" target="_top" style="color:#0000FF;">[&nbsp;UP&nbsp;]</a>)
	: '' ;

    # Alter various HTML depending on whether we're in the top frame or not.
    ($table_open, $table_close)= $in_top_frame
	? ('', '')
	: ('<table border=1 cellpadding=5><tr><td align=center bgcolor=white><font color=black>',
	   '</font></td></tr></table>') ;

    # Set global hash %in_mini_start_form according to how many each of applets,
    #   embeds, form, ids, layers, anchors, images, and links there are in this
    #   form.  It's used for handling certain JavaScript, later.
    # This isn't a persistent variable because it could vary from run to run.
    %in_mini_start_form= ('forms', 1, 'links', (($up_path ne '')  ? 2  : 1)) ;

    # Encode the URL before submitting, if so configured.  Start it with "\x01"
    #   to indicate that it's encoded ("\0" isn't handled well by all browsers).
    # Possible clash when a page has another element named "URL"; revisit if needed.
    if ($ENCODE_URL_INPUT) {
	$needs_jslib= 1 ;
	$onsubmit= q( onsubmit="if (!this.URL.value.match(/^\\x01/)) this.URL.value= '\x01'+_proxy_jslib_wrap_proxy_encode(this.URL.value) ; return true") ;
	$onfocus= q( onfocus="if (this.value.match(/^\\x01/)) this.value= _proxy_jslib_wrap_proxy_decode(this.value.replace(/\\x01/, ''))") ;
    } else {
	$onsubmit= '' ;
    }


    # Display one of two forms, depending on whether user config is allowed.
    if ($ALLOW_USER_CONFIG) {
	my($rc_on)= $e_remove_cookies     ? ' checked'  : '' ;
	my($rs_on)= $e_remove_scripts     ? ' checked'  : '' ;
	my($fa_on)= $e_filter_ads         ? ' checked'  : '' ;
	my($br_on)= $e_hide_referer       ? ' checked'  : '' ;
	my($if_on)= $e_insert_entry_form  ? ' checked'  : '' ;

# jsm-- remove for production release, plus in form below.
my($safe_URL2) ;
($safe_URL2= $URL)=~ s/([^\w.-])/ '%' . sprintf('%02x',ord($1)) /ge ;
$safe_URL2= "http://jmarshall.com/report.cgi?URL=$safe_URL2&version=2.1beta10" ;
$safe_URL2= &HTMLescape(&full_url($safe_URL2)) ;

	return <<EOF ;
<form name="URLform" action="$action" method=$method target="_top"$onsubmit>
<center>
$table_open
&nbsp;&nbsp;Location&nbsp;via&nbsp;proxy:<input name="URL" size=66 value="$safe_URL"$onfocus><input type=submit value="Go">
$up_link&nbsp;&nbsp;
<br><a href="$safe_URL2"><font color=red>[Report a bug]</font></a>
&nbsp;&nbsp;<a href="$cookies_url" target="_top" style="color:#0000FF;">[Manage&nbsp;cookies]</a>&nbsp;&nbsp;
<font size="-1"><input type=checkbox id="rc" name="rc"$rc_on><label for="rc">&nbsp;No&nbsp;cookies</label>
&nbsp;&nbsp;<input type=checkbox id="rs" name="rs"$rs_on><label for="rs">&nbsp;No&nbsp;scripts</label>
&nbsp;&nbsp;<input type=checkbox id="fa" name="fa"$fa_on><label for="fa">&nbsp;No&nbsp;ads</label>
&nbsp;&nbsp;<input type=checkbox id="br" name="br"$br_on><label for="br">&nbsp;No&nbsp;referrer</label>
&nbsp;&nbsp;<input type=checkbox id="if" name="if"$if_on><label for="if">&nbsp;Show&nbsp;this&nbsp;form</label>&nbsp;&nbsp;
</font>
$table_close
</center>
</form>
EOF

    # If user config isn't allowed, then show a different form.
    } else {
	return <<EOF ;
<form name="URLform" action="$action" method=$method target="_top"$onsubmit>
<center>
$table_open
Location&nbsp;via&nbsp;proxy:<input name="URL" size=66 value="$safe_URL"$onfocus><input type=submit value="Go">
$up_link
&nbsp;&nbsp;<a href="$cookies_url" target="_top">[Manage&nbsp;cookies]</a>
$table_close
</center>
</form>
EOF
    }

}



# Display cookies to the user and let user selectively delete them.
# No expiration date is displayed because to make that available would
#   require embedding it in every cookie.
sub manage_cookies {
    my($qs)= @_ ;
    my($return_url, $action, $clear_cookies_url, $cookie_rows, $auth_rows,
       $from_tag) ;
    my($name, $value, $type, @n, $delete_cb,
       $cname, $path, $domain, $cvalue, $secure,
       $realm, $server, $username) ;

    my($date_header)= &rfc1123_date($now, 0) ;

    my(%in)= &getformvars($qs) ;

    # $in{'from'} is already proxy_encoded
    $return_url= &HTMLescape( $url_start . $in{'from'} ) ;
    $action=     &HTMLescape( $url_start . &wrap_proxy_encode('x-proxy://cookies/update') ) ;

    # Create "clear cookies" link, preserving any query string.
    $clear_cookies_url= $url_start . &wrap_proxy_encode('x-proxy://cookies/clear') ;
    $clear_cookies_url.= '?' . $qs    if $qs ne '' ;
    $clear_cookies_url= &HTMLescape($clear_cookies_url) ;   # probably never necessary

    # Include from-URL in form if it's available.
    $from_tag= '<input type=hidden name="from" value="' . &HTMLescape($in{'from'}) . '">'
	if $in{'from'} ne '';

    # First, create $cookie_rows and $auth_rows from $ENV{'HTTP_COOKIE'}.
    # Note that the "delete" checkboxes use the encoded name as their identifier.
    # With minor rewriting, this could sort cookies e.g. by server.  Is that
    #   preferred?  Note that the order of cookies in $ENV{'HTTP_COOKIE'} has
    #   meaning.
    foreach ( split(/\s*;\s*/, $ENV{'HTTP_COOKIE'}) ) {
	($name, $value)= split(/=/, $_, 2) ;  # $value may contain "="
	$delete_cb= '<input type=checkbox name="delete" value="'
		  . &base64($name) . '">' ;
	$name= &cookie_decode($name) ;
	$value= &cookie_decode($value) ;
	($type, @n)= split(/;/, $name) ;
	if ($type eq 'COOKIE') {
	    ($cname, $path, $domain)= @n ;
	    ($cvalue, $secure)= split(/;/, $value) ;

	    $cookie_rows.= sprintf("<tr align=center><td>%s</td>\n<td>%s</td>\n<td>%s</td>\n<td>%s</td>\n<td>%s</td>\n<td>%s</td></tr>\n",
				   $delete_cb,
				   &HTMLescape($domain),
				   &HTMLescape($path),
				   &HTMLescape($cname),
				   &HTMLescape($cvalue),
				   $secure ? 'Yes' : 'No',
				  ) ;

	} elsif ($type eq 'AUTH') {
	    # format of auth cookie's name is AUTH;$enc_realm;$enc_server
	    ($realm, $server)= @n ;
	    $realm=~  s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;
	    $server=~ s/%([\da-fA-F]{2})/ pack('C', hex($1)) /ge ;
	    ($username)= split(/:/, &unbase64($value)) ;

	    $auth_rows.= sprintf("<tr align=center><td>%s</td>\n<td>%s</td>\n<td>%s</td>\n<td>%s</td></tr>\n",
				 $delete_cb,
				 &HTMLescape($server),
				 &HTMLescape($username),
				 &HTMLescape($realm),
				) ;

	}
    }


    # If either $cookie_rows or $auth_rows is empty, set appropriate messages.
    $cookie_rows= "<tr><td colspan=6 align=center>&nbsp;<br><b><font face=Verdana size=2>You are not currently sending any cookies through this proxy.</font></b><br>&nbsp;</td></tr>\n"
	if $cookie_rows eq '' ;

    $auth_rows= "<tr><td colspan=4 align=center>&nbsp;<br><b><font face=Verdana size=2>You are not currently authenticated to any sites through this proxy.</font></b><br>&nbsp;</td></tr>\n"
	if $auth_rows eq '' ;


    print <<EOF ;
$HTTP_1_X 200 OK
Cache-Control: no-cache
Pragma: no-cache
Date: $date_header
Content-type: text/html

<html>
<head>
<title>CGIProxy Cookie Management</title>
</head>
<body>
<h3><a href="$return_url">Return to browsing</a></h3>
<h1>Here are the cookies you're using through CGIProxy:</h1>

<form action="$action" method=post>
$from_tag

<table bgcolor="#ccffff" border=1>
<tr><th>Delete this cookie?</th>
    <th>For server names ending in:</th>
    <th>... and a path starting with:</th>
    <th>Cookie name</th>
    <th>Value</th>
    <th>Secure?</th>
</tr>
$cookie_rows
</table>

<h3>Authentication cookies:</h3>
<table bgcolor="#ccffcc" border=1>
<tr><th>Delete this cookie?</th>
    <th>Server</th>
    <th>User</th>
    <th>Realm</th>
</tr>
$auth_rows
</table>

<p><font color=red>
<input type=submit value="Delete selected cookies">
</font>
</form>

<h3><a href="$clear_cookies_url">Delete all cookies</a></h3>
EOF

    &footer ;
    goto EXIT ;
}



# Present the user with a special form that lets them enter authentication.
# The target URL is proxy_encoded in the form, for obscurity.
# Uses POST, because a GET request would show auth info in a logged URL.
sub get_auth_from_user {
    my($server, $realm, $URL, $tried)= @_ ;
    my($action, $msg) ;
    my($date_header)= &rfc1123_date($now, 0) ;

    $server= &HTMLescape($server) ;
    $realm=  &HTMLescape($realm) ;
    $URL=    &HTMLescape(&wrap_proxy_encode($URL)) ;

    $action= &HTMLescape( $url_start . &wrap_proxy_encode('x-proxy://auth/make_auth_cookie') ) ;

    $msg= "<h3><font color=red>Authorization failed.  Try again.</font></h3>"
	if $tried ;

    print <<EOF ;
$HTTP_1_X 200 OK
Cache-Control: no-cache
Pragma: no-cache
Date: $date_header
Content-type: text/html

<html>
<head><title>Enter username and password for $realm at $server</title></head>
<body>
<h1>Authorization Required</h1>
$msg

<form action="$action" method=post>
<input type=hidden name="s" value="$server">
<input type=hidden name="r" value="$realm">
<input type=hidden name="l" value="$URL">

<table border=1 cellpadding=5>
<tr><th bgcolor="#ff6666">
    Enter username and password for <nobr>$realm</nobr> at $server:</th></tr>
<tr><td bgcolor="#b0b0b0">
    <table cellpadding=0 cellspacing=0>
    <tr><td>Username:</td><td><input name="u" size=20></td></tr>
    <tr><td>Password:</td><td><input type=password name="p" size=20></td>
	<td>&nbsp;&nbsp;&nbsp;<input type=submit value="OK"></tr>
    </table>
</table>
</form>
<p>This requires cookie support turned on in your browser.
<p><i><b>Note:</b> Anytime you use a proxy, you're trusting the owner of that
proxy with all information you enter, including your name and password here.
This is true for <b>any</b> proxy, not just this one.
EOF

    &footer ;
    goto EXIT ;
}



# Alert the user to an unsupported URL, with this intermediate page.
sub unsupported_warning {
    my($URL)= @_ ;
    my($date_header)= &rfc1123_date($now, 0) ;

    &redirect_to($URL) if $QUIETLY_EXIT_PROXY_SESSION ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>WARNING: Entering non-anonymous area!</title></head>
<body>
<h1>WARNING: Entering non-anonymous area!</h1>
<h3>This proxy only supports HTTP and FTP.  Any browsing to another URL will
be directly from your browser, and no longer anonymous.</h3>
<h3>Follow the link below to exit your anonymous browsing session, and
continue to the URL non-anonymously.</h3>
<blockquote><tt><a href="$URL">$URL</a></tt></blockquote>
EOF

    &footer ;
    goto EXIT ;
}


# Alert the user that SSL is not supported, with this intermediate page.
sub no_SSL_warning {
    my($URL)= @_ ;
    my($date_header)= &rfc1123_date($now, 0) ;

    &redirect_to($URL) if $QUIETLY_EXIT_PROXY_SESSION ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>WARNING: SSL not supported, entering non-anonymous area!</title></head>
<body>
<h1>WARNING: SSL not supported, entering non-anonymous area!</h1>
<h3>This proxy as installed does not support SSL, i.e. URLs that start
with "https://".  To support SSL, the proxy administrator needs to install
the Net::SSLeay Perl module, and then this proxy will automatically
support SSL (the
<a href="http://www.jmarshall.com/tools/cgiproxy/">CGIProxy site</a>
has more info).  In the meantime, any browsing to an "https://" URL will
be directly from your browser, and no longer anonymous.</h3>
<h3>Follow the link below to exit your anonymous browsing session, and
continue to the URL non-anonymously.</h3>
<blockquote><tt><a href="$URL">$URL</a></tt></blockquote>
EOF

    &footer ;
    goto EXIT ;
}



# Return "403 Forbidden" message if the target server is forbidden.
sub banned_server_die {
    my($URL)= @_ ;
    my($date_header)= &rfc1123_date($now, 0) ;

    # Here, only quietly redirect out if we get a URL.  This allows calling
    #   routines to force an error, such as when using @BANNED_NETWORKS, or
    #   when a URL is not available.
    &redirect_to($URL) if $QUIETLY_EXIT_PROXY_SESSION && ($URL ne '') ;

    print <<EOF ;
$HTTP_1_X 403 Forbidden
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>The proxy can't access that server, sorry.</title></head>
<body>
<h1>The proxy can't access that server, sorry.</h1>
<p>The owner of this proxy has restricted which servers it can access,
presumably for security or bandwidth reasons.  The server you just tried
to access is not on the list of allowed servers.
EOF

    &footer ;
    goto EXIT ;
}



# Return "403 Forbidden" message if the user's IP address is disallowed.
sub banned_user_die {
    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 403 Forbidden
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>You are not allowed to use this proxy, sorry.</title></head>
<body>
<h1>You are not allowed to use this proxy, sorry.</h1>
<p>The owner of this proxy has restricted which users are allowed to use it.
Based on your IP address, you are not an authorized user.
EOF

    &footer ;
    goto EXIT ;
}



# If so configured, disallow browsing back through this same script.
sub loop_disallowed_die {
    my($URL)= @_ ;
    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 403 Forbidden
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>Proxy cannot loop back through itself</title></head>
<body>
<h1>Proxy cannot loop back through itself</h1>
<p>The URL you tried to access would cause this proxy to access itself,
which is redundant and probably a waste of resources.  The owner of this
proxy has configured it to disallow such looping.
<p>Rather than telling the proxy to access the proxy to access the desired
resource, try telling the proxy to access the resource directly.  The link
below <i>may</i> do this.
<blockquote><tt><a href="$URL">$URL</a></tt></blockquote>
EOF

    &footer ;
    goto EXIT ;
}



# Die if we try to retrieve a secure page while not running on a secure server,
#   because it's a security hole.
sub insecure_die {
    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>Retrieval of secure URLs through a non-secure proxy is forbidden.</title>
<body>
<h1>Retrieval of secure URLs through a non-secure proxy is forbidden.</h1>
<p>This proxy is running on a non-secure server, which means that retrieval
of pages from secure servers is not permitted.  The danger is that the user
and the end server may believe they have a secure connection between them,
while in fact the link between the user and this proxy is insecure and
eavesdropping may occur.  That's why we have secure servers, after all.
<p>This proxy must run on a secure server before being allowed to retrieve
pages from other secure servers.
EOF

    &footer ;
    goto EXIT ;
}



# Return "403 Forbidden" response for script content-type.
sub script_content_die {
    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 403 Forbidden
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>Script content blocked</title></head>
<body>
<h1>Script content blocked</h1>
<p>The resource you requested (or were redirected to without your knowledge)
is apparently an executable script.  Such resources have been blocked by this
proxy, presumably for your own protection.
<p>Even if you're sure you want the script, you can't get it through this
proxy the way it's configured.  If permitted, try browsing through this proxy
without removing scripts.  Otherwise, you'll need to reconfigure the proxy or
find another way to get the resource.
EOF

    &footer ;
    goto EXIT ;
}



# If images are forbidden, return either a "403 Forbidden" message or a
#   1x1 transparent GIF.
sub non_text_die {
    &return_empty_gif if $RETURN_EMPTY_GIF ;

    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 403 Forbidden
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>Proxy cannot forward non-text files</title></head>
<body>
<h1>Proxy cannot forward non-text files</h1>
<p>Due to bandwidth limitations, the owner of this particular proxy is
forwarding only text files.  For best results, turn off automatic image
loading if your browser lets you.
<p>If you need access to images or other binary data, route your browser
through another proxy (or install one yourself--
<a href="http://www.jmarshall.com/tools/cgiproxy/">it's easy</a>).
EOF

    &footer ;
    goto EXIT ;
}



# Die if the Encode module is needed but not found.
sub no_Encode_die {
    my($date_header)= &rfc1123_date($now, 0) ;

    print <<EOF ;
$HTTP_1_X 200 OK
${NO_CACHE_HEADERS}Date: $date_header
Content-type: text/html

<html>
<head><title>Page uses UTF-16 encoding, which is unsupported by this version
      of Perl</title></head>
<body>
<h1>Page uses UTF-16 encoding, which is unsupported by this version of Perl</h1>
<p>The page you requested appears to be in Unicode's UTF-16 format.  This is
not supported by the version of Perl running on this server (more exactly, the
"Encode" Perl module could not be found).
<p>To support UTF-16, please upgrade to Perl version 5.8.0 or later.
EOF

    &footer ;
    goto EXIT ;
}



# Die, outputting HTML error page, with optional response code and title.
sub HTMLdie {
    my($msg, $title, $status)= @_ ;
    $title= 'CGIProxy Error' if $title eq '' ;
    $status= '200 OK' if $status eq '' ;
    my($date_header)= &rfc1123_date($now, 0) ;

    # In case this is called early, set $HTTP_1_X to something that works.
    $HTTP_1_X=  $NOT_RUNNING_AS_NPH   ? 'Status:'   : "HTTP/1.0"
	if $HTTP_1_X eq '' ;

    print <<EOF ;
$HTTP_1_X $status
Cache-Control: no-cache
Pragma: no-cache
Date: $date_header
Content-Type: text/html

<html>
<head><title>$title</title></head>
<body>
<h1>$title</h1>
<h3>$msg</h3>
EOF

    &footer ;
    goto EXIT ;
}



#-----------------------------------------------------------------------
#  support for proxifying JavaScript
#-----------------------------------------------------------------------

# This routine modifies JavaScript code so that it works correctly through this
#   script.  This includes altering URL accesses to go through this script,
#   altering the reading and writing of cookies, and anthing else that's needed
#   to make script operation privacy-safe and transparent to the user.  The
#   $top_level parameter indicates whether this call is nested or not; it's
#   useful for only inserting a call to _proxy_jslib_flush_write_buffers() when
#   needed (slightly hacky).
# Elsewhere in this script, there is a library of JavaScript functions that are
#   called by code this routine produces.  If this routine generates any code
#   that uses that library, then it sets the global variable $needs_jslib=1 .
#   (It's not reset to false when the output code doesn't use the library,
#   since this routine may be called many times for one page.)
# THIS ROUTINE MAY NOT BE FOOLPROOF!!!  I can say that this script proxifies
#   JavaScript better than any similar software I've seen, and I know of no
#   privacy holes, but I can't guarantee there are none at this time.  If you
#   find a way to construct JavaScript that will not be correctly proxified
#   here, then please let me know.  If extreme privacy is critical to you,
#   then I recommend you turn off scripts in your browser.
# The current approach is to replace certain constructs with calls to
#   _proxy_jslib_handle(), _proxy_jslib_assign(), or _proxy_jslib_assign_rval()
#   in the JS library.  To do this: The input is read one token at a time (see
#   the routine set_RE_JS() below for details about tokenization), and when a
#   token is found that may need proxifying, it is replaced by a call to one of
#   those three functions, depending on whether it is being read/called or
#   assigned.  This requires, during tokenization, keeping track of the current
#   "term", by which I mean what the JS spec calls LeftHandSideExpression, one
#   value or variable, like one term if you consider an expression to be like a
#   polynomial, that one term which may have several object references or
#   method calls in it.  (Harder to explain than to understand.)  The term
#   (object) leading up to the token is passed to _proxy_jslib_handle() and
#   _proxy_jslib_assign() so they can test its type and access the property
#   through it.  Also passed to _proxy_jslib_handle() are the property name
#   (either a token or read from between "[]"), and the current value of the
#   property/variable (only needed when the object is null).  Passed to
#   _proxy_jslib_assign() are the "prefix" (i.e. "++", "--", or "delete"), the
#   leading term/object, the property name, the operator that causes the
#   assignment, and the right-hand expression it's being assigned to.  If there
#   is no leading term/object, then _proxy_jslib_assign_rval() is called
#   instead, with the prefix, the property name, the operator, the expression
#   it's being assigned to, and the property's current value.
# Also done in this routine are things like incrementing subscripts of e.g.
#   document.forms[] and other arrays, and changing references from
#   "_proxy_jslib_..." to "_proxy1_jslib_..." etc. to keep the libraries
#   separate when chaining proxies.  A lot of code just deals with keeping
#   $term_so_far accurate in different situations.
#
# Below are everything in core and client-side JavaScript that need to be
#   handled, according to a read of the reference sections of "JavaScript: The
#   Definitive Guide", 4th Edition, by David Flanagan, published by O'Reilly.
#   The only exceptions are minor and would not open privacy holes, such as
#   exact screen coordinates being off because of our insertions, or certain
#   DOM arrays being shifted because of our insertions (similar to the forms[]
#   etc. arrays we try to handle, as listed below).
#
# Here are the network-related things in JS that are handled by this script:
#   Window.open(), Document.write(), Document.writeln(), Document.close(),
#     Location.replace(), Layer.load(), Window.setInterval(),
#     Window.setTimeout(), HTMLElement.setAttribute(),
#     HTMLElement.setAttributeNode(), Node.appendChild(), Node.insertBefore(),
#     Node.replaceChild, CSS*.insertRule(), HTMLElement.insertAdjacentHTML()
#     (MSIE only), Window.navigate() (MSIE only), eval(), and many others are
#     handled in _proxy_jslib_handle().
#   All setting of src, href, background, lowsrc, action, useMap, longDesc,
#     cite, codeBase, profile, cssText, nodeValue, and location properties are
#     handled in _proxy_jslib_assign().  Also handled there are any setting of
#     innerHTML, outerHTML, or outerText properties, since they are defined by
#     HTMLElement and may be inherited by many different objects.  Also handled
#     in _proxy_jslib_assign() are any setting of the various read/write
#     properties of Link and Location objects that would cause a page to load.
#     There are several other things that _proxy_jslib_assign() handles.
#   If "location" is assigned without a leading object, then it is handled by
#     _proxy_jslib_assign_rval().
#   Cookies are handled-- reading of them in _proxy_jslib_handle(), and setting
#     of them in _proxy_jslib_assign().
#   "new Function(...)" is handled with _proxy_jslib_new_function(), since it
#     contains JS code that may need to be proxified.
#   These eight array properties of the document object are incremented as
#     needed, according to what's in the insertions:  applets[], embeds[],
#     forms[], ids[], layers[], anchors[], images[], and links[].
#   (.on* events don't need changing, since they're set to a function object,
#       unlike HTML event attributes, which are set to a string containing
#       JavaScript code.)
#
# NOTE: IF YOU MODIFY THIS ROUTINE, then be sure to review and possibly
#   modify the corresponding routine _proxy_jslib_proxify_js() in the
#   JavaScript library, far below in the routine return_jslib().  It is
#   a Perl-to-JavaScript translation of this routine.

sub proxify_js {
    my($in, $top_level)= @_ ;

    # Declaring variables here rather than in blocks below is a little faster.
    my(@out, $element, $token, $last_token, $new_last_token, $newline_since_last_token, $div_ok,
       $term_so_far, $prefix, $sub_expr, $op, $new_val, $cur_val_str, $inc_by,
       $does_write, $in_braces, $in_func, $expr, $next_expr,
       $var_decl, $var, $eq, $value, $skip1, $skip2, $funcname) ;

    # Gmail has a bug-- it uses "new Image().src = ..." where it should use
    #   "(new Image()).src = ..." .  So this is a workaround until they fix it.
    $in=~ s/new Image\(\)\.src =/(new Image()).src =/ ;


    # Note that these patterns contain an embedded set of parentheses that
    #   only match if the input element is a token.
  OUTER:
    while ($div_ok  ? $in=~ /\G($RE_JS_INPUT_ELEMENT_DIV)/gco
		    : $in=~ /\G($RE_JS_INPUT_ELEMENT_REG_EXP)/gco) {

	($element, $token)= ($1, $2) ;

	# If a token was gotten, then set $div_ok according to the token.
	#   Until we get a more complete parser, this is a pretty good guess.
	#   Note that here, "token" also includes DivPunctuator and
	#   RegularExpressionLiteral.
	# DivPunctuator may come after: certain reserved words, identifiers,
	#   the four punctuators ") ] ++ --", numeric and string literals,
	#   and regular expression literals.  To match identifiers but not
	#   the wrong reserved words, it's probably easier to include all
	#   identifiers, then just exclude those reserved words which may
	#   precede RegularExpressionLiteral.  The last line of the pattern
	#   below tests the start of the token for several possible token
	#   types, combined into one pattern.
	# Reserved words that may precede DivPunctuator are qw(this null true false);
	#   reserved words that may precede RegularExpressionLiteral are
	#   qw(case delete do else in instanceof new return throw typeof void).

	if (defined($token)) {
	    $div_ok= $token=~ m#^(?:\)|\]|\+\+|--)$|
				^(?!(?:case|delete|do|else|in|instanceof|new|return|throw|typeof|void)$)
				 (?:\pL|[\$_\\0-9'"]|\.\d|/..)#x ;
	}

	$newline_since_last_token= 1 if $element=~ /^$RE_JS_LINE_TERMINATOR$/o ;
	$new_last_token= '' ;

	# Keep track of whether we're in a function, to correctly handle returns.
	$in_braces++ if $token eq '{' ;
	$in_braces-- if $token eq '}' ;
	$in_func= 0 if $in_braces==0 ;


	# Now, handle cases depending on value of $token.


	# Whitespace and comments may be interspersed within a term.
	if ($token eq '') {
	    if ($term_so_far ne '') {
		$term_so_far.= $element ;
	    } else {
		push(@out, $element) ;
	    }


	# Increment identifiers from other libraries, to allow chaining of
	#   multiple proxies and to close a privacy hole.
	} elsif ($token=~ s/^_proxy(\d*)_jslib_/'_proxy'.($1+1).'_jslib_'/e) {
	    $term_so_far.= $token ;


	# Treat these as beginning a term.
	} elsif ($token=~ /^(?:$RE_JS_NUMERIC_LITERAL|$RE_JS_STRING_LITERAL|$RE_JS_REGULAR_EXPRESSION_LITERAL)$/o) {
	    push(@out, $prefix, $term_so_far) ;
	    $prefix= '' ;
	    $term_so_far= $token ;


	# Now all input elements are handled except identifiers (including
	#   reserved words) and all punctuators (including DivPunctuator).
	# All punctuators end a term except for .[(, which each need a special
	#   block here to handle them; all punctuators that are
	#   AssignmentOperator or ++/-- must also be handled specially.


	# Handle increment and decrement operators, and "delete", using this
	#   simplification:  ++/-- is post- if there's a term so far and
	#   not a newline since the last token, and pre- otherwise.
	#   Pre- operators become the "prefix" parameter in the call to
	#   _proxy_jslib_assign(); with post- operators, $prefix and
	#   $term_so_far are pushed onto @out, then the operator itself.
	#   Note that $term_so_far may have already been transformed during
	#   the processing of a previous token.
	} elsif ($token=~ /^(?:\+\+|--|delete)$/) {
	    # Handle "-->" instead of "--" if needed.
	    if ($token eq '--' and $in=~ /\G\s*>/gco) {
		push(@out, $prefix, $term_so_far, '-->') ;
		$prefix= $term_so_far= '' ;
	    } elsif (($term_so_far ne '') and !$newline_since_last_token) {
		push(@out, $prefix, $term_so_far, $token) ;
		$prefix= $term_so_far= '' ;
	    } else {
		push(@out, $prefix, $term_so_far) ;
		$prefix= $term_so_far= '' ;
		my($o, $p)= &get_next_js_term(\$in) ;
		last unless defined($p) ;
		if ($o ne '') {
		    push(@out, "_proxy_jslib_assign('$token', (" . &proxify_js($o) . "), (" . &proxify_js($p) . "), '')" ) ;
		} else {
		    # Note that $p is guaranteed to be a quoted identifier here.
		    $p=~ s/^'|'$//g ;
		    push(@out, "($p= _proxy_jslib_assign_rval('$token', '$p', '', '', $p))") ;
		}

#		$prefix= $token ;
	    }


	# eval() is a special case.  It should normally be followed by a
	#   parenthesis, in which case we transform "eval(expr)" into
	#   "eval(_proxy_jslib_proxify_js(expr))".
	# If it's not followed by a parenthesis, then that means the code
	#   is probably trying to assign something to the eval function itself.
	#   By spec, this may be treated as an error.  We handle it in the
	#   next block using _proxy_jslib_handle(), though imperfectly (e.g.
	#   when eval is replaced by a function, local variables are no longer
	#   in scope).
	# When its argument is not a primitive string, eval() returns its
	#   argument unchanged, which mucks this code up a bit.  As an imperfect
	#   solution, this is handled in _proxy_jslib_proxify_js(), by having it
	#   return its argument unchanged if it's not a string.
	} elsif (($token eq 'eval') && $in=~ /\G($RE_JS_SKIP*\()/gco) {
	    $needs_jslib= 1 ;
	    $term_so_far.= $token . $1 . '_proxy_jslib_proxify_js('
			 . &proxify_js(&get_next_js_expr(\$in,1)) . '))' ;
	    last unless $in=~ /\G\)/gc ;
	    $div_ok= 1 ;


	# Here, explicitly handle all properties and methods that need special
	#   treatment.  Property names only are listed, and sorted out in the
	#   all-purpose routines _proxy_jslib_handle(), _proxy_jslib_assign(),
	#   and _proxy_jslib_assign_rval().
	# For document.write() and document.writeln(), note that the writing of
	#   one e.g. tag can be split among several write() statements.  So for
	#   the parsing of its output to happen correctly, for each JS block we
	#   accumulate a buffer of all write() output and then flush it if
	#   needed at the end of the JS block's execution.  This more closely
	#   mimics the JS model anyway, where write() output is considered to
	#   immediately follow the closing </script> tag.  "If needed" means
	#   if we find an explicit write() or writeln() call, or if there are
	#   any parentheses :P, because any function call could do a write().
	# Element and Node have many properties which are inherited by many
	#   other objects.  Thus, their properties "innerHTML" through
	#   "nodeValue" below will be handled regardless of object type.
	} elsif ($token=~ /^(?:open|write|writeln|close|replace|load|eval
			       |setInterval|setTimeout|toString
			       |src|href|background|lowsrc|action|location
			       |useMap|longDesc|cite|codeBase|profile
			       |cssText|insertRule|setStringValue|setProperty
			       |backgroundImage|content|cursor|listStyleImage
			       |host|hostname|pathname|port|protocol|search
			       |insertNode|surroundContents|setNamedItem
			       |getElementById|getElementsByTagName
			       |innerHTML|outerHTML|outerText
			       |insertAdjacentHTML|setAttribute|setAttributeNode
			       |appendChild|insertBefore|replaceChild|nodeValue
			       |value|cookie|domain|frames|parent|top|opener
			       |execScript|navigate|showModalDialog|showModelessDialog
			    )$/x) {
	    $needs_jslib= 1 ;
	    $does_write||= ($token eq 'write') || ($token eq 'writeln') || ($token eq 'eval') ;

	    # Handle automatic semicolon insertion.  For more notes about
	    #   automatic semicolon insertion, see comments in
	    #   get_next_js_expr() below.
	    if ($newline_since_last_token
		and $last_token=~ m#^(?:\)|\]|\+\+|--)$|
				    ^(?!(?:case|delete|do|else|in|instanceof|new|typeof|void|function|var)$)
				     (?:\pL|[\$_\\0-9'"]|\.\d|/..)#x )
	    {
		push(@out, $prefix, $term_so_far) ;
		$prefix= $term_so_far= '' ;
	    }

	    $term_so_far=~ s/\.$RE_JS_SKIP*$// ;

	    # Transform to either _proxy_jslib_handle() or _proxy_jslib_assign() call.

	    # First, avoid modifying property names in object literals, which
	    #   are preceded by "{" or "," and followed by ":" .
	    # Not the cleanest here. but should work.
	    if ($last_token=~ /^[{,]$/ and $in=~ /\G($RE_JS_SKIP*:)/gco) {
		push(@out, $prefix, $term_so_far, $token, $1) ;
		$prefix= $term_so_far= '' ;
		$new_last_token= ':' ;
		$div_ok= 0 ;

	    } elsif ($prefix ne '') {
		if ($term_so_far eq '') {
		    push(@out, "$token= _proxy_jslib_assign_rval('$prefix', '$token', '$op', '', $token)") ;
		} else {
		    $term_so_far= "_proxy_jslib_assign('$prefix', $term_so_far, '$token', '', '')" ;
		}
		$prefix= '' ;
		$new_last_token= ')' ;
		$div_ok= 1 ;
	    } elsif ($in=~ /\G$RE_JS_SKIP_NO_LT*(\+\+|--)/gco) {
		$op= $1 ;
		if ($term_so_far eq '') {
		    push(@out, "$token= _proxy_jslib_assign_rval('', '$token', '$op', '', $token)") ;
		} else {
		    $term_so_far= "_proxy_jslib_assign('', $term_so_far, '$token', '$op', '')" ;
		}
		$new_last_token= ')' ;
		$div_ok= 1 ;
	    } elsif ($in=~ /\G$RE_JS_SKIP*(>>>=|<<=|>>=|[+*\/%&|^-]?=(?!=))/gco) {
		$op= $1 ;
		$new_val= &proxify_js(&get_next_js_expr(\$in)) ;
		if ($term_so_far eq '') {
		    push(@out, "$token= _proxy_jslib_assign_rval('', '$token', '$op', ($new_val), $token)") ;
		} else {
		    $term_so_far= "_proxy_jslib_assign('', $term_so_far, '$token', '$op', ($new_val))" ;
		}
		$new_last_token= ')' ;
		$div_ok= 0 ;
	    } else {
		# Pass object and name of property.  Only pass property's value
		#   if object is null, in which case it is needed for return
		#   value.  A little hacky.
		$cur_val_str= ($term_so_far eq '')  ? ", $token"  : '' ;
		$term_so_far= 'null' if $term_so_far eq '' ;
		$term_so_far= "_proxy_jslib_handle($term_so_far, '$token'$cur_val_str)" ;
		$new_last_token= ')' ;
		$div_ok= 1 ;
	    }


	# These eight arrays of the document object must have all subscripts
	#   incremented by the number of each type of element in the inserted
	#   HTML, so that the subscripts still refer to the intended page
	#   elements.
	# Here we assume the referring object is a document and don't check.
	# Also, it may refer to other documents' elements, but those also
	#   will probably need their subscripts incremented, so it's OK.
	# This is normally only needed for sloppy JS.  Better HTML/JS uses named
	#   elements, but some pages just use integer subscripts.
	# This errs when a non-numeric subscript is used that evaluates to a
	#   number.  It doesn't open a privacy hole.  If needed, we can revisit
	#   this.
	} elsif ($token=~ /^(?:applets|embeds|forms|ids|layers|anchors|images|links)$/) {
	    if ($doing_insert_here and $term_so_far ne '' and $in=~ /\G($RE_JS_SKIP*\[)/gco) {
		$skip1= $1 ;
		$next_expr= &get_next_js_expr(\$in,1) ;
		if ($next_expr=~ /^\s*\d+\s*$/) {
		    $inc_by= $IN_CUSTOM_INSERTION{$token}+$in_mini_start_form{$token} ;
		    $term_so_far.= $token . $skip1 . "$inc_by+(" . &proxify_js($next_expr) . ')]' ;
		} else {
		    $term_so_far.= $token . $skip1 . '(' . &proxify_js($next_expr) . ')]' ;
		}
		last unless $in=~ /\G\]/gc ;
		$new_last_token= ']' ;
	    } else {
		$term_so_far.= $token ;
	    }
	    $div_ok= 1 ;


	# These reserved words must have their following parenthesized
	#   expression read, or else it could be confused with the start of a
	#   term.  "catch" and "function" also use parentheses, but those are
	#   argument lists and shouldn't be proxified; they're handled below.
	} elsif ($token=~ /^(?:if|while|for|with|switch)$/) {
	    push(@out, $prefix, $term_so_far, $token) ;
	    $prefix= $term_so_far= '' ;
	    last unless $in=~ /\G($RE_JS_SKIP*\()/gco ;
	    push(@out, $1, &proxify_js(&get_next_js_expr(\$in,1)), ')') ;
	    last unless $in=~ /\G\)/gc ;
	    $div_ok= 0 ;

	# Parentheses after "catch" and "function" shouldn't be proxified.
	} elsif ($token eq 'catch') {
	    push(@out, $prefix, $term_so_far, $token) ;
	    $prefix= $term_so_far= '' ;
	    last unless $in=~ /\G($RE_JS_SKIP*\()/gco ;
	    push(@out, $1, &get_next_js_expr(\$in,1), ')') ;
	    last unless $in=~ /\G\)/gc ;
	    $div_ok= 0 ;

	# Contrary to the spec, MSIE allows function identifiers to be object
	#   properties in dot notation, so allow "identifier(.identifier)*" .
	} elsif ($token eq 'function') {
	    push(@out, $prefix, $term_so_far, $token) ;
	    $prefix= $term_so_far= '' ;
	    #last unless $in=~ /\G($RE_JS_SKIP*)($RE_JS_IDENTIFIER_NAME)?($RE_JS_SKIP*\()/gco ;   # by the spec
	    last unless $in=~ /\G($RE_JS_SKIP*)($RE_JS_IDENTIFIER_NAME(?:\.(?:$RE_JS_IDENTIFIER_NAME))*)?($RE_JS_SKIP*\()/gco ;
	    ($skip1, $funcname, $skip2)= ($1, $2, $3) ;
	    # Update function name if it's from another proxy's library.
	    $funcname=~ s/^_proxy(\d*)_jslib_/'_proxy'.($1+1).'_jslib_'/e ;
	    push(@out, $skip1, $funcname, $skip2, &get_next_js_expr(\$in,1), ') {') ;
	    last unless $in=~ /\G\)\s*\{/gc ;
	    $in_braces++ ;
	    $in_func= 1 ;
	    $div_ok= 0 ;


	# Handle "var" specially to avoid failing on e.g. "var open= 1 ;" .
	# "var ... in ..." clauses are handled by matching either "=" or "in"
	#   after the identifier name.
	} elsif ($token eq 'var') {
	    push(@out, $prefix, $term_so_far, $token) ;
	    $prefix= $term_so_far= '' ;
	    while (1) {
		$var_decl= &get_next_js_expr(\$in,0) ;
		( ($skip1, $var, $eq, $value)= $var_decl=~ /^($RE_JS_SKIP*)($RE_JS_IDENTIFIER_NAME$RE_JS_SKIP*)(=|in)?(.*)$/s )
		    || last OUTER ;
		# Update variable name if it's from another proxy's library.
		$var=~ s/^_proxy(\d*)_jslib_/'_proxy'.($1+1).'_jslib_'/e ;
		push(@out, $skip1, $var) ;
		push(@out, $eq, &proxify_js($value)) if $eq ne '' ;
		last unless $in=~ /\G,/gc ;
		push(@out, ',') ;
	    }
	    $div_ok= 0 ;


	# Handle "new Function(...)", which contains JS code.
	# This will matter extremely rarely, and is a messy fix, but if not
	#   handled is a privacy hole.
	} elsif ($token eq 'new') {
	    push(@out, $prefix, $term_so_far) ;
	    $prefix= $term_so_far= '' ;
	    if ($in=~ /\G$RE_JS_SKIP*Function\b/gco) {
		push(@out, '_proxy_jslib_new_function') ;
		# teeny bug-- should also push parens if $in is missing them
	    } else {
		push(@out, $token) ;
	    }


	# With every top-level "return", we must _proxy_jslib_flush_write_buffers()
	#   if anything's been written.  Transform "return expr" into
	#   "return ((_proxy_jslib_ret= (expr)), _proxy_jslib_flush_write_buffers(), _proxy_jslib_ret)"
	# Handle other returns simply in next block.
	# jsm-- what about "return foo()"?  $does_write is not set in time
	#   for that case.
	} elsif (($token eq 'return') and !$in_func and $top_level and $does_write) {
	    push(@out, $prefix, $term_so_far) ;
	    $prefix= $term_so_far= '' ;
	    $needs_jslib= 1 ;
	    # Allow commas, but not semicolons; perhaps $allow_multiple in
	    #   get_next_js_expr() should be 3-way.
	    $expr= &get_next_js_expr(\$in,0) ;
	    $expr.= ', ' . &get_next_js_expr(\$in,0) while $in=~ /\G$RE_JS_SKIP*,$RE_JS_SKIP*/gco ;
	    $expr= &proxify_js($expr, 0) ;
	    push(@out,
		 "return ((_proxy_jslib_ret= ($expr)), _proxy_jslib_flush_write_buffers(), _proxy_jslib_ret)") ;
	    $div_ok= 0 ;


	# This is all reserved words except "this", "super", "true", "false",
	#   and "null", which may be part of an object expression.  (Also
	#   missing are the nine reserved words handled directly above.)
	} elsif ($token=~ /^(?:abstract|boolean|break|byte|case|char|class|const|continue|debugger|default|delete|do|else|enum|export|extends|final|finally|float|goto|implements|in|instanceof|int|interface|long|native|package|private|protected|return|short|static|synchronized|throw|throws|transient|try|typeof|void|volatile)$/) {
	    push(@out, $prefix, $term_so_far, $token) ;
	    $prefix= $term_so_far= '' ;


	# This handles identifiers and a certain few reserved words, above.
	# Most reserved words must be handled separately from identifiers, or
	#   else there may be syntatic ambiguities, e.g. "if (foo) (...)".
	} elsif ($token=~ /^$RE_JS_IDENTIFIER_NAME$/o) {
	    # Handle automatic semicolon insertion.  For more notes about
	    #   automatic semicolon insertion, see comments in
	    #   get_next_js_expr() below.
	    if ($newline_since_last_token
		and $last_token=~ m#^(?:\)|\]|\+\+|--)$|
				    ^(?!(?:case|delete|do|else|in|instanceof|new|typeof|void|function|var)$)
				     (?:\pL|[\$_\\0-9'"]|\.\d|/..)#x )
	    {
		push(@out, $prefix, $term_so_far) ;
		$prefix= '' ;
		$term_so_far= $token ;
	    } else {
		$term_so_far.= $token ;
	    }


	} elsif ($token eq '.') {
	    $term_so_far.= '.' ;


	# For "(", get inside parens, proxify, and add to output.
	} elsif ($token eq '(') {
	    $does_write= 1 ;   # any function call could do a write()
	    $term_so_far.= '(' . &proxify_js(&get_next_js_expr(\$in,1)) . ')' ;
	    last unless $in=~ /\G\)/gc ;
	    $new_last_token= ')' ;
	    $div_ok= 1 ;


	# For "[", get inside brackets, proxify, and pass parenthesized as
	#   second parameter to _proxy_jslib_handle().  Or, start new term
	#   if it looks like an array literal instead.
	} elsif ($token eq '[') {
	    # Don't change it for simple integer subscripts.
	    if ($in=~ /\G($RE_JS_SKIP*\d+$RE_JS_SKIP*\])/gco) {
		$term_so_far.= '[' . $1 ;
		$new_last_token= ']' ;
		$div_ok= 1 ;
	    } else {
		$sub_expr= &proxify_js(&get_next_js_expr(\$in,1)) ;
		last unless $in=~ /\G\]/gc ;
		if ($term_so_far ne '') {
		    $needs_jslib= 1 ;
		    $new_last_token= ')' ;
		    if ($prefix ne '') {
			$term_so_far= "_proxy_jslib_assign('$prefix', $term_so_far, ($sub_expr), '', '')" ;
			$prefix= '' ;
			$div_ok= 0 ;
		    } elsif ($in=~ /\G$RE_JS_SKIP_NO_LT*(\+\+|--)/gco) {
			$op= $1 ;
			$term_so_far= "_proxy_jslib_assign('', $term_so_far, ($sub_expr), '$op', '')" ;
			$div_ok= 1 ;
		    } elsif ($in=~ /\G$RE_JS_SKIP*(>>>=|<<=|>>=|[+*\/%&|^-]?=(?!=))/gco) {
			$op= $1 ;
			$new_val= &proxify_js(&get_next_js_expr(\$in)) ;
			$term_so_far= "_proxy_jslib_assign('', $term_so_far, ($sub_expr), '$op', ($new_val))" ;
			$div_ok= 0 ;
		    } else {
			$term_so_far= "_proxy_jslib_handle($term_so_far, ($sub_expr))" ;
			$div_ok= 1 ;
		    }
		} else {
		    $term_so_far= "[$sub_expr]" ;
		    $new_last_token= ']' ;
		    $div_ok= 1 ;
		}
	    }


	# All other punctuators end a term.
	} elsif ($token=~ /^(?:$RE_JS_PUNCTUATOR|$RE_JS_DIV_PUNCTUATOR)$/o) {
	    push(@out, $prefix, $term_so_far, $token) ;
	    $prefix= $term_so_far= '' ;


	} else {
	    &HTMLdie("Shouldn't get here, token= [$token]") ;
	}

	if (defined($token)) {
	    $last_token= $new_last_token ne ''  ? $new_last_token  : $token ;
	    $newline_since_last_token= 0 ;
	}
    }


    push(@out, $prefix, $term_so_far) ;

    # If there's been a write or writeln, then insert a call to flush the
    #   output buffer.  A similar call is inserted into every appropriate
    #   "return" statement; see handling of that above.
    push(@out, " ;\n_proxy_jslib_flush_write_buffers() ;"), $needs_jslib= 1
	if $top_level && $does_write ;

    return join('', @out) ;
}




# Given a pointer to a string, return the longest complete JavaScript expression
#   starting at the string match pointer (pos), and update that string pointer.
# If $allow_multiple is set, then read multiple expressions/statements as
#   possible, only ending on an unmatched closing parenthesis (or error).
#   Otherwise, also end on a top-level comma or semicolon.
# The method here is to read in one token at a time, and compare it to various
#   possible tokens that could end the expression.  For this to work, we need
#   to keep a stack of various parenthesis characters which may nest; the
#   expression may only end when the parenthesis stack is empty.  Note that
#   the "?:" characters are treated like parentheses, to handle conditional
#   expressions.  The ":" needs special treatment, because it may also be used
#   in switch statements, labelled statements, and object literals.
# In this routine, all opening parentheses "([{" are treated the same; likewise
#   for all closing parentheses ")]}".  This is a shortcut that works for all
#   valid JavaScript, but errs on e.g. "( { ) }".  A browser wouldn't run that
#   anyway, so this shortcut seems safe.
# This routine is inefficient in that it tokenizes the JavaScript but doesn't
#   save that effort, thus the expression will require tokenizing again later.
#   This could be avoided if we had a good way of matching sequences of tokens
#   (a la regexes) in proxify_js().
sub get_next_js_expr {
    my($s, $allow_multiple)= @_ ;
    my(@out, @p, $element, $token, $div_ok, $last_token, $pos) ;

    while (1) {

	# Note that these patterns contain an embedded set of parentheses that
	#   only match if the input element is a token.
	last unless ($div_ok
		     ? $$s=~ /\G($RE_JS_INPUT_ELEMENT_DIV)/gco
		     : $$s=~ /\G($RE_JS_INPUT_ELEMENT_REG_EXP)/gco) ;
	($element, $token)= ($1, $2) ;

	# If $element is either ";" or "," , then end the expression if the
	#   parenthesis stack is empty.  Otherwise, continue.
	if ($element eq ';' or $element eq ',') {
	    pos($$s)-= 1, return join('', @out)  if !$allow_multiple and !@p ;

	# If it's a line terminator, then handle automatic semicolon insertion:
	#   if not allowing multiple statements, if the parenthesis stack is
	#   empty, if the previous token is not acceptable before an identifier
	#   or keyword, and if the next input is an identifier or keyword, then
	#   act as if a semicolon had been encountered, similar to above.
	# I'm not sure this is rigorous, but it should work for virtually all
	#   real-life situations.  Let me know if you find any privacy holes,
	#   or any actual sites it doesn't work with.
	# Testing the next input for an identifier requires saving and restoring
	#   pos($$s).
	# Tokens "not acceptable before an identifier or keyword" are identifiers
	#   and most keywords, numeric/string/regex literals, and the punctuators
	#   ")", "]", "++", and "--".  As it turns out, this is much the same
	#   regex as used in the setting of $div_ok above and below; the only
	#   difference is four keywords.
	# For more details, see the ECMAScript spec, section 7.9 .
	} elsif ($element=~ /^$RE_JS_LINE_TERMINATOR$/o) {
	    pos($$s)= $pos-length($element), return join('', @out)
		if !$allow_multiple and !@p 
		   and $last_token=~ m#^(?:\)|\]|\+\+|--)$|
				       ^(?!(?:case|delete|do|else|in|instanceof|new|typeof|void|function|var)$)
					(?:\pL|[\$_\\0-9'"]|\.\d|/..)#x
		   and ($pos= pos($$s), $$s=~ /\G$RE_JS_SKIP*$RE_JS_IDENTIFIER_NAME/gco) ;

	# If $element is an opening "parenthesis" (including "?"), then push it
	#   onto the parenthesis stack and continue.
	} elsif ($element=~ /^[(\[{?]$/) {
	    push(@p, $element) ;

	# If $element is a closing "parenthesis" (including ":"), then end the
	#   expression if the parenthesis stack is empty.  Otherwise, pop the
	#   parenthesis stack and continue.
	# If $element is ":", then only pop the parenthesis stack if the top
	#   item is a "?".  This prevents popping when the ":" is not part of
	#   a "?"...":" conditional (like in a switch statement, labelled
	#   statement, or object literal).  This is why we store the stack
	#   instead of using a simple counter.
	} elsif ($element=~ /^[)\]}:]$/) {
	    pos($$s)-= 1, return join('', @out)  unless @p ;
	    pop(@p)  unless ($element eq ':' and $p[$#p] ne '?') ;

	    # If it's a "}", then return if not $allow_multiple and
	    #   the parenthesis stack is empty.
	    return join('', @out, '}')
		if ($element eq '}' and !@p and !$allow_multiple) ;
	}

	# Whatever we got, add it to the output.
	push(@out, $element) ;

	# If a token was gotten, then set $div_ok according to the token.
	# See the comments in proxify_js() for details.
	if (defined($token)) {
	    $div_ok= $token=~ m#^(?:\)|\]|\+\+|--)$|
				^(?!(?:case|delete|do|else|in|instanceof|new|return|throw|typeof|void)$)
				 (?:\pL|[\$_\\0-9'"]|\.\d|/..)#x ;
	    $last_token= $token ;
	}
    }

    # If we got here, then $$s has no more tokens.  Either there's a syntax
    #   error, or the end of the string has been reached.  We'll *guess* that
    #   we have a valid expression if the parenthesis stack is empty, and
    #   return it; otherwise, return undef.  Either way, the pos($$s) doesn't
    #   change.
    return  @p  ? undef  : join('', @out) ;
}



# Given a reference to a string, return the next JavaScript term in it, split
#   up into the leading object and the final property (either the entire
#   contents between "[]" or a quoted identifier).  The string search pointer
#   is correctly updated.
# On error, return nothing.
# Note that if $o is empty, then $p is guaranteed to be a quoted identifier.
sub get_next_js_term {
    my($s)= @_ ;
    my($o, $p, $ofrag) ;

    $$s=~ /\G$RE_JS_SKIP*($RE_JS_IDENTIFIER_NAME)/gco or return ;
    $p= "'$1'" ;
    $ofrag= $1 ;

    while ($$s=~ /\G$RE_JS_SKIP*([.\[\(])/gco) {
	$o.= $ofrag ;
	if ($1 eq '.') {
	    $$s=~ /\G$RE_JS_SKIP*($RE_JS_IDENTIFIER_NAME)/gco  or return ;
	    $p= "'$1'" ;
	    $ofrag= '.' . $1 ;
	} elsif ($1 eq '[') {
	    $p= &get_next_js_expr($s, 1) ;
	    $ofrag= "[$p]" ;
	    $$s=~ /\G\]/gco  or return ;
	} elsif ($1 eq '(') {
	    $p= '' ;
	    $ofrag= '(' . &get_next_js_expr($s, 1) . ')' ;
	    $$s=~ /\G\)/gco  or return ;
	}
    }

    return ($o, $p) ;
}



# Used to detect the rare and erroneous case when "</script" is inside a
#   string literal.  Since JS string literals can't contain line terminators,
#   we only have to tokenize the last line.
# $s is a reference to a script block, not including <script> and </script>.
sub js_script_ends_inside_string {
    my($s)= @_ ;
    my($last_line, $token, $div_ok, $pos) ;

    $pos= pos($$s) ;
    ($last_line)= $$s=~ /(?:^|$RE_JS_LINE_TERMINATOR)([^\012\015]*)\z/o ;
    pos($$s)= $pos ;

    # Note that these patterns contain an embedded set of parentheses that
    #   only match if the input element is a token.
    while ($div_ok
	   ? $last_line=~ /\G$RE_JS_INPUT_ELEMENT_DIV/gco
	   : $last_line=~ /\G$RE_JS_INPUT_ELEMENT_REG_EXP/gco)
    {

	$token= $1 ;

	# If a token was gotten, then set $div_ok according to the token.
	# See the comments in proxify_js() for details.
	$div_ok= $token=~ m#^(?:\)|\]|\+\+|--)$|
			    ^(?!(?:case|delete|do|else|in|instanceof|new|return|throw|typeof|void)$)
			     (?:\pL|[\$_\\0-9'"]|\.\d|/..)#x
	    if (defined($token)) ;
    }

    # If we got here, then $last_line has no more tokens.  Either there's a
    #   syntax error, or the end of the string has been reached.  For our
    #   purposes, we return true iff the string remainder starts with ['"/].
    return $last_line=~ /\G['"\/]/  ? 1  : 0 ;
}



# Set the various regular expressions used in parsing JavaScript.
# These regular expressions are taken directly from the "productions" (rules of
#   grammar) of the ECMAScript specification, which is basically the JavaScript
#   spec.  The spec version followed below is the standard ECMA-262, published
#   in December 1999.  It's available at http://www.ecma.ch/ecma1/STAND/ECMA-262.HTM .
# For the most part, these patterns represent the grammar as strictly defined
#   in the ECMAScript spec.  For example, StringLiteral doesn't match '"\x"' or
#   '"\012"' and the pattern here reflects that, though other implementations
#   may be more permissive.  If needed, we can extend the patterns later to
#   cover common misuses.  Also, if we decide to support octal numeric literals
#   and octal escape sequences (as older implementations did), appendix B.1 of
#   the spec has the details.  In any case, when this program scans script
#   content and at some point fails to match a valid input element, it discards
#   the remainder of the script.  Thus, while the strictness may prevent sloppy
#   scripts from running, it gives maximum protection from privacy holes, etc.
# Some of the patterns here do not strictly follow the spec, for purposes of
#   multi-platform compatibility or performance.  To my knowledge, they work
#   fine for actual existing pages (as opposed to hypothetical cases), and they
#   do not open any privacy holes.  If you find otherwise, please let me know!
#   The various strictly conformant patterns are collected in comments at the
#   end of this routine.  Several have to do with the Unicode line terminators
#   \x{2028} and \x{2029}, which we ignore in the patterns here.
# We're not using the \x{unicode} construct, because it's not fully supported
#   yet, e.g. in character classes.
# Patterns use no-backtracking (the "(?>...)" construct) where possible for
#   speed; also, in some cases it prevents splitting tokens inappropriately.
#   No-backtracking patterns work here because the parsing and tokenizing is
#   pretty deterministic (i.e. unambiguous context of each input, which means
#   no backtracking is needed when parsing).  If we go with a more top-down
#   non-deterministic approach, we'd probably use fewer if any no-backtracking
#   patterns (though we'd still need to prevent splitting tokens).
# When these patterns are used elsewhere, don't forget they're no-backtracking!
sub set_RE_JS {

    # If we decide to support UTF-8, this allows multi-platform compatibility.
    #eval '/\x{2028}/' ;
    #my($utf8_OK)=  $@ eq '' ;


    $RE_JS_WHITE_SPACE= qr/[\x09\x0b\x0c \xa0]|\p{Zs}/ ;
    $RE_JS_LINE_TERMINATOR= qr/[\012\015]/ ;


    # Note that a single-line comment must not have a backtracking pattern, to
    #   force it to grab all characters up to a line terminator; multi-line
    #   comment must not backtrack either, to prevent it from grabbing beyond
    #   the first "*/".  So entire pattern is enclosed in (?>...) .
    # Technically, a "/*...*/" -style comment that contains a line terminator
    #   should be replaced by a line terminator during parsing, rather than
    #   be discarded entirely.  This may become relevant in the future if we
    #   parse syntax more rigorously, handle automatic semicolon insertion, etc.
    # Browsers also treat "<!--" as starting a one-line comment, so authors can
    #   use the old trick of an HTML comment to hide JS from non-JS browsers.
    #   This recognition of "<!--" is not part of the JS spec, but we handle
    #   it here.
    $RE_JS_COMMENT= qr#(?>/\*.*?\*/|//[^\012\015]*|<!--[^\012\015]*)#s ;


    # UnicodeLetter can be Unicode categories/properties of
    #   (Lu, Ll, Lt, Lm, Lo, Nl).  This can be condensed to (L, Nl).  Also,
    #   Nl doesn't seem to exist in Perl 5.6.0.  Thus, for now, use "\pL" to
    #   check for any letter.  Note that the "\pL" construct can't be used
    #   in character classes.
    # "\p{Pc}" doesn't exist in Perl 5.6.0.  So, don't use it either.
    # Eventually, we could set different values based on the Perl version, if
    #   there's demand.
    $RE_JS_IDENTIFIER_START= qr/\pL|[\$_]|\\u[0-9a-fA-F]{4}/ ;
    $RE_JS_IDENTIFIER_PART=  qr/$RE_JS_IDENTIFIER_START|\p{Mn}|\p{Mc}|\p{Nd}/ ;
    $RE_JS_IDENTIFIER_NAME=  qr/(?>$RE_JS_IDENTIFIER_START$RE_JS_IDENTIFIER_PART*)/ ;


    # Put the longest punctuators first in the list of alternatives.
    $RE_JS_PUNCTUATOR= qr/(?>>>>=?|===|!==|<<=|>>=|[<>=!+*%&|^-]=|\+\+|--|<<|>>|&&|\|\||[{}()[\].;,<>+*%&|^!~?:=-])/ ;
    $RE_JS_DIV_PUNCTUATOR= qr!(?>/=?)! ;


    # Hex literal must come before decimal, so that "0x..." is not parsed as "0"
    #   and a syntax error.  2nd and 3rd alternatives comprise DecimalLiteral
    #   plus the non-standard OctalIntegerLiteral, defined in section B.1 of the
    #   spec.
    $RE_JS_NUMERIC_LITERAL= qr/(?>0[xX][0-9a-fA-F]+|
				  [0-9]+(?:\.[0-9]*)?(?:[eE][+-]?[0-9]+)?|
				  \.[0-9]+(?:[eE][+-]?[0-9]+)?)
			       (?!$RE_JS_IDENTIFIER_START)
			      /x ;


    # The last alternative here represents CharacterEscapeSequence, fully expanded.
    # Note that this includes the non-standard OctalEscapeSequence, defined in
    #   section B.1 of the spec.
    # Unfortunately, some browsers allow a line terminator in the string if it's
    #   preceded by "\".  So, against the spec, allow line terminators in
    #   escape sequences.
    #$RE_JS_ESCAPE_SEQUENCE= qr/x[0-9a-fA-F]{2}|u[0-9a-fA-F]{4}|[0-7]{1,3}(?![0-9])|[^0-9xu\012\015]/ ;
    $RE_JS_ESCAPE_SEQUENCE= qr/x[0-9a-fA-F]{2}|u[0-9a-fA-F]{4}|[0-7]{1,3}(?![0-9])|[^0-9xu]/ ;
    $RE_JS_STRING_LITERAL= qr/"(?>(?:[^"\\\012\015]|\\$RE_JS_ESCAPE_SEQUENCE)*)"|
			      '(?>(?:[^'\\\012\015]|\\$RE_JS_ESCAPE_SEQUENCE)*)'/x ;

    $RE_JS_REGULAR_EXPRESSION_LITERAL= qr!/(?>(?:[^\012\015*\\/]|\\[^\012\015])
					      (?:[^\012\015\\/]|\\[^\012\015])*)
					  /(?>$RE_JS_IDENTIFIER_PART*)
					 !x ;


    # NumericLiteral should come before Punctuator, to avoid parsing e.g.
    #   ".4" as "." and "4".
    $RE_JS_TOKEN= qr/$RE_JS_IDENTIFIER_NAME|$RE_JS_NUMERIC_LITERAL|$RE_JS_PUNCTUATOR|$RE_JS_STRING_LITERAL/ ;


    # JavaScript has a parsing quirk-- to handle the ambiguity that "/" may
    #   start either a division operator or a regular expression literal, it's
    #   specified that the parser should match a division operator if it's
    #   allowed by the higher-level grammar, and otherwise match a regular
    #   expression literal.  So it provides the two goal productions below.
    #   When we use them, we'll try to guess from the context which to use.
    # These patterns aren't strictly correct, because each has the extra
    #   alternative at the end to match in case we guess wrong.  Also, we
    #   combine consecutive WhiteSpace input elements here.
    # These patterns have a quirk/hack that is important to be aware of:
    #   there's a set of parentheses surrounding the final three alternatives,
    #   and any time either pattern is used it will generate an extra
    #   backreference, and a $1 (or $2, or whatever).  This lets us know if
    #   the input element matched was a token (here counting division operators
    #   and regular expression literals as tokens), which aids our process of
    #   guessing whether a division operator is allowed as the next input (see
    #   above):  we guess based on which token it is, or leave the current
    #   guess unchanged if it's not a token.
    # Note that Comment has to come before DivPunctuator to correctly parse "//".

    $RE_JS_INPUT_ELEMENT_DIV= qr/(?>$RE_JS_WHITE_SPACE+)|$RE_JS_LINE_TERMINATOR|$RE_JS_COMMENT|
				 ($RE_JS_TOKEN|$RE_JS_DIV_PUNCTUATOR|$RE_JS_REGULAR_EXPRESSION_LITERAL)/x ;

    $RE_JS_INPUT_ELEMENT_REG_EXP= qr/(?>$RE_JS_WHITE_SPACE+)|$RE_JS_LINE_TERMINATOR|$RE_JS_COMMENT|
				     ($RE_JS_TOKEN|$RE_JS_REGULAR_EXPRESSION_LITERAL|$RE_JS_DIV_PUNCTUATOR)/x ;


    # These are pseudo-productions of those input elements that can come between
    #   tokens and are (pretty much) ignored.
    # Note that each represents one item and should normally be followed by "*".
    # $RE_JS_SKIP_NO_LT excludes line terminators for where those are not allowed.
    $RE_JS_SKIP= qr/(?>$RE_JS_WHITE_SPACE+)|$RE_JS_LINE_TERMINATOR|$RE_JS_COMMENT/ ;
    $RE_JS_SKIP_NO_LT= qr/(?>$RE_JS_WHITE_SPACE+)|$RE_JS_COMMENT/ ;



    #-------------------------------------------------------------------------
    #  expressions
    #-------------------------------------------------------------------------

    # This section is not actually used in the program.  The "patterns" here
    #   won't quite work; they're only for studying, to work out how expressions
    #   are parsed in JavaScript.  The whole grammar of expressions is a kind of
    #   recursive network of many different productions (rules).  The set of
    #   patterns here is greatly condensed from the grammar productions in the
    #   ECMAScript spec, but is still accurate and expresses the basic
    #   framework.  One basic recursion loop is that:  Expression contains
    #   LeftHandSideExpression, which contains PrimaryExpression and Arguments,
    #   both of which may contain Expression.  If you think of Expression as
    #   being like a polynomial, then LeftHandSideExpression is similar to one
    #   term in it, and PrimaryExpression is similar to the central piece of
    #   that (e.g. a variable name, a number, or a parenthesized subexpression);
    #   Arguments is a standard comma-separated list of arguments.  This
    #   recursion loop has many more intermediate steps in the actual spec; a
    #   few that simplify the other patterns, or are referred to in their own
    #   right, are articulated below.
    # A few extra pseudo-productions are created here to simplify the rest of
    #   the grammar.  For example, the list of binary operators lets us
    #   summarize two full pages of productions into a one-line pattern.
    # Again, these are NOT actual working patterns!  So don't use them in their
    #   current state.  To make them work, the subexpressions in them that match
    #   tokens would have to be interspersed with allowed whitespace, e.g. by
    #   using $RE_JS_SKIP.  Also, regular expression literals would have to be
    #   fit in appropriately.  They're a quirk in the language definition, not
    #   used in the syntax productions-- during *parsing*, they're replaced by
    #   RegExp objects.  See the spec, section 7.8.5, for details.  Finally, any
    #   patterns that contain variables set after them must use "postponed
    #   subexpressions", i.e. the "(??{ $PATTERN })" construct.  This is how
    #   recursive (and mutually-referencing) patterns are expressed in Perl.  A
    #   pretty neat feature, actually, if you don't know about it yet.
    # If used for real, some of these patterns might benefit from being
    #   non-backtracking.  In some cases, they might even require that to parse
    #   correctly.  See notes about backtracking at the start of this routine.
    # For LeftHandSideExpression, we use a pattern that is simpler to work with
    #   than the pattern in the spec, but is not strictly accurate.  It's a
    #   superset of the real LeftHandSideExpression, so it should cover all
    #   valid JavaScript expressions.  An example of an expression that is not
    #   strictly allowed but would match our pattern is "new new foo(a).b(c)".

    # The next two aren't in the spec, but are useful to us below.
    # $RE_JS_BINARY_OPERATOR=  qr/instanceof|in|>>>=?|===|!==|<<=|>>=|[<>=!+*%&|^\/-]=|<<|>>|&&|\|\||[*\/%+<>&^|?:=-]/ ;
    # $RE_JS_UNARY_OPERATOR=   qr/delete|void|typeof|\+\+|--|[+~!-]/ ;

    # $RE_JS_ARRAY_LITERAL= qr/\[,*(?:$RE_JS_ASSIGNMENT_EXPRESSION(?:,+$RE_JS_ASSIGNMENT_EXPRESSION)*)?,*\]/ ;
    # $RE_JS_PROPERTY_NAME= qr/$RE_JS_IDENTIFIER_NAME|$RE_JS_STRING_LITERAL|$RE_JS_NUMERIC_LITERAL/ ;
    # $RE_JS_OBJECT_LITERAL= qr/{(?:$RE_JS_PROPERTY_NAME:$RE_JS_ASSIGNMENT_EXPRESSION
    #                               (?:,$RE_JS_PROPERTY_NAME:$RE_JS_ASSIGNMENT_EXPRESSION)*)?}/x ;

    # $RE_JS_ARGUMENTS= qr/\((?:$RE_JS_ASSIGNMENT_EXPRESSION(?:,$RE_JS_ASSIGNMENT_EXPRESSION)*)?\)/ ;

    # $RE_JS_FUNCTION_EXPRESSION= qr/function$RE_JS_IDENTIFIER_NAME?
    #                                \((?:$RE_JS_IDENTIFIER_NAME(?:,$RE_JS_IDENTIFIER_NAME)*)\)
    #                                {$RE_JS_PROGRAM}
    #                               /x ;


    # $RE_JS_PRIMARY_EXPRESSION= qr/$RE_JS_IDENTIFIER_NAME|$RE_JS_NUMERIC_LITERAL|$RE_JS_STRING_LITERAL|
    #                               $RE_JS_ARRAY_LITERAL|$RE_JS_OBJECT_LITERAL|\($RE_JS_EXPRESSION\)/ ;

    # Here's the approximate simplification.
    # $RE_JS_LEFT_HAND_SIDE_EXPRESSION= qr/(?:new)*
    #                                      (?:$RE_JS_PRIMARY_EXPRESSION|$RE_JS_FUNCTION_EXPRESSION)
    #                                      (?:$RE_JS_ARGUMENTS|\[$RE_JS_EXPRESSION\]|\.$RE_JS_IDENTIFIER_NAME)*
    #                                     /x ;


    # $RE_JS_UNARY_EXPRESSION= qr/$RE_JS_UNARY_OPERATOR*$RE_JS_LEFT_HAND_SIDE_EXPRESSION(?:\+\+|--)?/ ;

    # $RE_JS_ASSIGNMENT_EXPRESSION= qr/$RE_JS_UNARY_EXPRESSION(?:$RE_JS_BINARY_OPERATOR$RE_JS_UNARY_EXPRESSION)*/ ;

    # $RE_JS_EXPRESSION= qr/$RE_JS_ASSIGNMENT_EXPRESSION(?:,$RE_JS_ASSIGNMENT_EXPRESSION)*/ ;


    #-------------------------------------------------------------------------
    # Below are the various patterns that would strictly follow the spec,
    #   collected from above.
    #-------------------------------------------------------------------------

#   $RE_JS_LINE_TERMINATOR= qr/[\012\015]|\x{2028}|\x{2029}/ ;
#   $RE_JS_COMMENT= qr!(?>/\*.*?\*/|//[^\012\015\x{2028}\x{2029}]*)!s ;
#
#   $RE_JS_IDENTIFIER_START= qr/\p{Lu}|\p{Ll}|\p{Lt}|\p{Lm}|\p{Lo}|\p{Nl}|[\$_]|\\u[0-9a-fA-F]{4}/ ;
#   $RE_JS_IDENTIFIER_PART=  qr/$RE_JS_IDENTIFIER_START|\p{Mn}|\p{Mc}|\p{Nd}|\p{Pc}/ ;
#
#   $RE_JS_ESCAPE_SEQUENCE= qr/x[0-9a-fA-F]{2}|u[0-9a-fA-F]{4}|0(?![0-9])|[^0-9xu\012\015\x{2028}\x{2029}]/ ;
#   $RE_JS_STRING_LITERAL= qr/"(?>(?:[^"\\\012\015\x{2028}\x{2029}]|\\$RE_JS_ESCAPE_SEQUENCE)*)"|
#			      '(?>(?:[^'\\\012\015\x{2028}\x{2029}]|\\$RE_JS_ESCAPE_SEQUENCE)*)'/x ;
#
#   $RE_JS_REGULAR_EXPRESSION_LITERAL= qr!/(?>(?:[^\012\015\x{2028}\x{2029}*\\/]|\\[^\012\015\x{2028}\x{2029}])
#					      (?:[^\012\015\x{2028}\x{2029}\\/]|\\[^\012\015\x{2028}\x{2029}])*)
#					  /(?>$RE_JS_IDENTIFIER_PART*)
#					 !x ;
#
#   $RE_JS_MEMBER_EXPRESSION= qr/(?:$RE_JS_PRIMARY_EXPRESSION|$RE_JS_FUNCTION_EXPRESSION)
#				   (?:\[$RE_JS_EXPRESSION\]|\.$RE_JS_IDENTIFIER_NAME)*
#				 |new$RE_JS_MEMBER_EXPRESSION$RE_JS_ARGUMENTS
#				/x ;
#   $RE_JS_LEFT_HAND_SIDE_EXPRESSION= qr/(?:new)*$RE_JS_MEMBER_EXPRESSION|
#					 $RE_JS_MEMBER_EXPRESSION$RE_JS_ARGUMENTS
#					 (?:$RE_JS_ARGUMENTS|\[$RE_JS_EXPRESSION\]|\.$RE_JS_IDENTIFIER_NAME)*
#					/x ;


}



#-----------------------------------------------------------------------
#  JavaScript library used when rewriting JavaScript
#-----------------------------------------------------------------------


# These are the functions included in the JavaScript library:
#   Initialization functions:
#     _proxy_jslib_init
#     _proxy_jslib_pass_vars (called from outside)
#   API functions:
#     _proxy_jslib_handle
#     _proxy_jslib_assign
#     _proxy_jslib_assign_rval
#   Secondary functions needed to support above functions:
#     _proxy_jslib_add_to_write_buffer
#     _proxy_jslib_flush_write_buffers
#     _proxy_jslib_new_function
#     _proxy_jslib_doc2win
#     _proxy_jslib_node_is_in_document
#     _proxy_jslib_init_domain
#     _proxy_jslib_full_url
#     _proxy_jslib_full_url_by_frame
#     _proxy_jslib_cookie_to_client
#     _proxy_jslib_cookie_from_client
#     _proxy_jslib_proxify_html
#     _proxy_jslib_proxify_js
#     _proxy_jslib_proxify_comment
#     _proxy_jslib_proxify_script_block
#     _proxy_jslib_proxify_style_block
#     _proxy_jslib_proxify_decl_bang
#     _proxy_jslib_proxify_decl_question
#     _proxy_jslib_proxify_element
#     _proxy_jslib_proxify_attribute
#     _proxy_jslib_proxify_block
#     _proxy_jslib_proxify_css
#     _proxy_jslib_css_full_url
#     _proxy_jslib_return_frame_doc
#     _proxy_jslib_proxy_encode, _proxy_jslib_proxy_decode
#     _proxy_jslib_cookie_encode, _proxy_jslib_cookie_decode
#     _proxy_jslib_wrap_proxy_encode, _proxy_jslib_wrap_proxy_decode
#   Utilities:
#     _proxy_jslib_object_type
#     _proxy_jslib_parse_url
#     _proxy_jslib_parse_full_url
#     _proxy_jslib_pack_flags
#     _proxy_jslib_unpack_flags
#     _proxy_jslib_html_escape
#     _proxy_jslib_html_unescape
#     _proxy_jslib_global_replace


# Returns the JavaScript library used to proxify JavaScript.  Normally, it
#   should be cachable.
# Some of these routines (the "API") are referenced from this Perl script.
#   Other routines are added to support the API functions.
# One important routine here is _proxy_jslib_pass_vars().  It's a general
#   mechanism used to pass any needed values from the Perl script into this
#   library.  One call to it is inserted in the HTML, right after this JS
#   library is loaded.  To pass more values into this library, add arguments
#   where it is called (two places), and modify the routine itself below.
# Many routines here are analogous to Perl routines in this script.  Some are
#   almost exact translations of the Perl routines into JavaScript (thus
#   implying that whenever those Perl routines are changed, these JS routines
#   must be changed too).
# To make this code run in MSIE 5.0, we must avoid a few JavaScript features
#   that are not supported in it.  :P  These include the boolean "in" operator,
#   Array.{pop,push,shift,unshift,splice}, Function.{apply,call},
#   String.replace() with a replacement function, certain regular expression
#   constructs, and (yes) the keyword-like global property "undefined" (though
#   the value exists and can be created with "void 0").  Also, instanceof
#   doesn't always work right in MSIE so avoid that too.
# This library contains very little commenting, to save bandwidth.  For those
#   routines with Perl analogs, see the comments accompanying the Perl routines.
sub return_jslib {
    my($date_header)=    &rfc1123_date($now, 0) ;
    my($expires_header)= &rfc1123_date($now+86400*7, 0) ;  # expires after a week

    # To save time, only set $JSLIB_BODY if it hasn't been set already.
    unless ($JSLIB_BODY) {

	# We must use single-quoted line delimiter ('EOF') to prevent variable
	#   interpolation, etc.  But we also have to pass some constants to it,
	#   so we concatenate a "variable" block and a "fixed" block.  The
	#   "variable" block is constant for each installation, so the library
	#   can still be cached.
	# Note that $ENCODE_DECODE_BLOCK_IN_JS is a user config setting, at top.
	my($script_name_jsq)= $ENV_SCRIPT_NAME ;
	$script_name_jsq=~ s/(["\\])/\\$1/g ;   # make safe for JS quoted string
	my($proxy_group_jsq, @pg, $all_types_js, $mime_type_id_js) ;
	@pg= @PROXY_GROUP ;
	foreach (@pg) { s/(["\\])/\\$1/g }
	$proxy_group_jsq= join(', ', map { "'$_'" } @pg) ;
	$all_types_js=    join(', ', map { "'$_'" } @ALL_TYPES) ;
	$mime_type_id_js= join(', ', map { "'$_':$MIME_TYPE_ID{$_}" } keys %MIME_TYPE_ID) ;

	$JSLIB_BODY= <<EOV . <<'EOF' ;

var _proxy_jslib_SCRIPT_NAME= "$script_name_jsq" ;
var _proxy_jslib_PROXY_GROUP= [$proxy_group_jsq] ;
var _proxy_jslib_ALL_TYPES= [$all_types_js] ;
var _proxy_jslib_MIME_TYPE_ID= {$mime_type_id_js} ;

$ENCODE_DECODE_BLOCK_IN_JS

EOV

var _proxy_jslib_RE_FULL_URL ;
var _proxy_jslib_url_start, _proxy_jslib_url_start_inframe, _proxy_jslib_url_start_noframe,
    _proxy_jslib_is_in_frame, _proxy_jslib_packed_flags, _proxy_jslib_URL ;
var _proxy_jslib_base_url, _proxy_jslib_base_scheme, _proxy_jslib_base_host, _proxy_jslib_base_path, _proxy_jslib_base_file ;
var _proxy_jslib_cookies_are_banned_here, _proxy_jslib_doing_insert_here, _proxy_jslib_SESSION_COOKIES_ONLY,
    _proxy_jslib_COOKIE_PATH_FOLLOWS_SPEC, _proxy_jslib_RESPECT_THREE_DOT_RULE,
    _proxy_jslib_default_script_type, _proxy_jslib_default_style_type ;
var _proxy_jslib_RE, _proxy_jslib_needs_jslib ;
var _proxy_jslib_write_buffers= [ {doc:document, has_js:true} ] ;
var _proxy_jslib_wins= new Array() ;
var _proxy_jslib_ret ;

_proxy_jslib_init() ;


//---- first, the two initialization functions -------------------------

// set _proxy_jslib_URL, _proxy_jslib_url_start, _proxy_jslib_packed_flags,
//   _proxy_jslib_is_in_frame, _proxy_jslib_url_start_inframe, _proxy_jslib_url_start_noframe
// _proxy_jslib_base_url must be set elsewhere, to handle possibly coming from
//   HTTP headers.
function _proxy_jslib_init() {
    // create global regex that matches a full URL, needed for _proxy_jslib_parse_full_url()
    var RE_SCRIPT_NAME= _proxy_jslib_global_replace(_proxy_jslib_SCRIPT_NAME, /(\W)/,
						    function (p) { return '\\'+p[1] } ) ;
    _proxy_jslib_RE_FULL_URL= new RegExp('^('+RE_SCRIPT_NAME+')\\/?([^\\/]*)\\/?(.*)') ;

    // Mozilla sometimes adds 'wyciwyg://' to the URL
    var URL= document.URL.replace(/^wyciwyg:\/\/\d+\//i, '') ;

    var u= _proxy_jslib_parse_full_url(URL) ;
    if (_proxy_jslib_PROXY_GROUP.length) {
	_proxy_jslib_url_start= _proxy_jslib_PROXY_GROUP[Math.floor(Math.random()*_proxy_jslib_PROXY_GROUP.length)]
				+'/'+u[1]+'/' ;
    } else {
	_proxy_jslib_url_start= u[0]+'/'+u[1]+'/' ;
    }
    var flags= _proxy_jslib_unpack_flags(u[1]) ;
    _proxy_jslib_is_in_frame= flags[5] ;
    flags[5]= 1 ;    // that's the frame flag
    _proxy_jslib_url_start_inframe= u[0]+'/'+_proxy_jslib_pack_flags(flags)+'/' ;
    flags[5]= 0 ;
    _proxy_jslib_url_start_noframe= u[0]+'/'+_proxy_jslib_pack_flags(flags)+'/' ;

    _proxy_jslib_packed_flags= u[1] ;
    _proxy_jslib_URL=          u[2] ;

    // this begins life as the hostname
    window._proxy_jslib_document_domain= _proxy_jslib_parse_url(_proxy_jslib_URL)[4] ;
    _proxy_jslib_wins[0]= window ;
//alert('end of init; _p_j_URL=\n['+_proxy_jslib_URL+']') ;
}


// set variables passed in from Perl program.
function _proxy_jslib_pass_vars(base_url, cookies_are_banned_here, doing_insert_here, SESSION_COOKIES_ONLY, COOKIE_PATH_FOLLOWS_SPEC, RESPECT_THREE_DOT_RULE, default_script_type, default_style_type) {
    // set base_ vars from base_url
    _proxy_jslib_base_url= base_url.replace(/^\s+|\s+$/g, '')
				   .replace(/^([\w\+\.\-]+\:\/\/[^\/\?]+)\/?/, "$1/") ;
    _proxy_jslib_base_scheme= _proxy_jslib_base_url.match(/^([\w\+\.\-]+\:)\/\//)[1] ;
    _proxy_jslib_base_host=   _proxy_jslib_base_url.match(/^([\w\+\.\-]+\:\/\/[^\/\?]+)/)[1] ;
    _proxy_jslib_base_path=   _proxy_jslib_base_url.match(/^([^\?]*\/)/)[1] ;
    _proxy_jslib_base_file=   _proxy_jslib_base_url.match(/^([^\?]*)/)[1] ;

    // other settings
    _proxy_jslib_cookies_are_banned_here=  cookies_are_banned_here ;
    _proxy_jslib_doing_insert_here=        doing_insert_here ;
    _proxy_jslib_SESSION_COOKIES_ONLY=     SESSION_COOKIES_ONLY ;
    _proxy_jslib_COOKIE_PATH_FOLLOWS_SPEC= COOKIE_PATH_FOLLOWS_SPEC ;
    _proxy_jslib_RESPECT_THREE_DOT_RULE=   RESPECT_THREE_DOT_RULE ;

    _proxy_jslib_default_script_type=      default_script_type.toLowerCase() ;
    _proxy_jslib_default_style_type=       default_style_type.toLowerCase() ;
}



//---- the general handler routines _proxy_jslib_handle() and _proxy_jslib_assign() ----

// This is used when the property in question IS NOT being assigned to.
function _proxy_jslib_handle (o, property, cur_val) {
//alert('starting handle; o, property, URL=['+o+']['+property+']\n['+_proxy_jslib_URL+']') ;
    // guess when the window object is implied; this only matters with Window's
    //   properties that we handle below
    if ((o===null)  && property.match(/^(location|open|setInterval|setTimeout|frames|parent|top|opener|execScript|navigate|showModalDialog|showModelessDialog|parentWindow)$/) && (window[property]===cur_val)) o= window ;

    // handle eval() specially-- it (oddly) can be a property of any object
    if (property=='eval')
	return function (code) {
		   if (o!=null) return o.eval(_proxy_jslib_proxify_js(code, 0)) ;
		   else         return   eval(_proxy_jslib_proxify_js(code, 0)) ;
	       } ;

    // if object is still null, merely return property value
    if (o==null) return cur_val ;

    var otype= _proxy_jslib_object_type(o) ;

    // handle any non-type-specific properties first, or properties that may
    //   apply to different object types (e.g. the properties of Node)
    switch (property) {
	case 'insertAdjacentHTML':
	    return function (where, text) {
		       if (_proxy_jslib_node_is_in_document(o))
			   return o.insertAdjacentHTML(where, _proxy_jslib_proxify_html(text, false, o.document)[0]) ;
		       return o.insertAdjacentHTML(where, text) ;
		   } ;
	case 'setAttribute':
	    return function (name, value) {
		       if (_proxy_jslib_node_is_in_document(o))
			   return o.setAttribute(name.toLowerCase(), _proxy_jslib_proxify_attribute(name, value)) ;
		       return o.setAttribute(name, value) ;
		   }
	case 'setAttributeNode':
	    return function (newAttr) {
		       if (_proxy_jslib_node_is_in_document(o))
			   newAttr.nodeValue= _proxy_jslib_proxify_attribute(newAttr.nodeName, newAttr.nodeValue) ;
		       return o.setAttributeNode(newAttr) ;
		   }
	case 'appendChild':
	    return function (newChild) {
		       if (_proxy_jslib_node_is_in_document(o) && !_proxy_jslib_node_is_in_document(newChild))
			   return o.appendChild(_proxy_jslib_proxify_element(newChild)) ;
		       return o.appendChild(newChild) ;
		   }
	case 'insertBefore':
	    return function (newChild, refChild) {
		       if (_proxy_jslib_node_is_in_document(o) && !_proxy_jslib_node_is_in_document(newChild))
			   return o.insertBefore(_proxy_jslib_proxify_element(newChild), refChild) ;
		       return o.insertBefore(newChild, refChild) ;
		   }
	case 'replaceChild':
	    return function (newChild, oldChild) {
		       if (_proxy_jslib_node_is_in_document(o) && !_proxy_jslib_node_is_in_document(newChild))
			   return o.replaceChild(_proxy_jslib_proxify_element(newChild), oldChild) ;
		       return o.replaceChild(newChild, oldChild) ;
		   }

	case 'insertRule':
	    return function (rule, index) {
		       return o.insertRule(_proxy_jslib_proxify_css(rule), index) ;
		   }

	// because some sites modify these in place, we must un-proxify these
	//   when retrieving the value.
	// for Location objects, return the object, but handle toString() below
	//   to unproxify it when needed.
	// jsm-- this will still leave Location proxified when toString() is
	//   called implicitly.
	case 'src':
	case 'href':
	case 'background':
	case 'lowsrc':
	case 'action':
	case 'useMap':
	case 'longDesc':
	case 'cite':
	case 'codeBase':
	case 'location':
	    var u= (o!=void 0) ? o[property] : cur_val ;
	    if (u==void 0) return void 0 ;
	    // return unchanged if u is a non-String object
	    if ((typeof u=='object') && (u.toLowerCase==void 0))
		return u ;
	    var pu= _proxy_jslib_parse_full_url(u) ;
	    if (pu==void 0) return u ;   // if it's not a URL
	    // if o is a node, then deproxify only if it's in doc or is an Image
	    if ((o.nodeName!==void 0) && (o.nodeType!==void 0) && (o.nodeValue!==void 0)) {
		if (_proxy_jslib_node_is_in_document(o) || (otype=='Image'))
		    return pu[2] ;
		else return u ;
	    }
//if (u=='') alert('in handle, first switch; typeof, o, property, u, caller=['+typeof(o)+']['+o+']['+property+']['+u+']\n['+arguments.callee.caller+']') ;
	    return pu[2] ;
    }


    // note use of closures to remember the object o
    switch (otype+'.'+property) {
	// Store new windows in a list so we can insert JS later if needed.
	// Store windows instead of documents, because docs may not be created yet.
	case 'Window.open':
	    return function (url, name, features, replace) {
		       var full_url= _proxy_jslib_full_url(url) ;
		       var win= o.open(full_url, name, features, replace) ;
		       if (url) win._proxy_jslib_document_domain=
			   _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(full_url)[2])[4] ;
		       _proxy_jslib_wins[_proxy_jslib_wins.length]= win ;
		       return win ;
		   } ;
	case 'Document.open':
	    return function(arg1, name, features, replace) {
		       if (arguments.length<=2) {
			   return o.open(arg1, name) ;
		       } else {
			   // MSIE-specific
			   return o.open(_proxy_jslib_full_url(arg1), name, features, replace) ;
		       }
		   }
	case 'Document.write':
	    // buffer the output by document
	    // no return value
	    return function () {
		       for (var i= 0 ; i<arguments.length ; i++)
//if (confirm('write ['+arguments[i]+']?'))
			   _proxy_jslib_add_to_write_buffer(o, arguments[i]) ;
		   } ;
	case 'Document.writeln':
	    return function () {
		       for (var i= 0 ; i<arguments.length ; i++)
			   _proxy_jslib_add_to_write_buffer(o, arguments[i]) ;
		       _proxy_jslib_add_to_write_buffer(o, '\n') ;
		   } ;
	case 'Document.close':
	    return function() {
//alert('starting Document.close(); # buffers='+_proxy_jslib_write_buffers.length) ;  //  jsm-- all alerts and confirms
		       var buf, i, p ;
		       for (i in _proxy_jslib_write_buffers) {
			   if (_proxy_jslib_write_buffers[i].doc===o) {
			       buf= _proxy_jslib_write_buffers[i] ;
			       if (buf.buf==void 0) break ;
			       p= _proxy_jslib_proxify_html(buf.buf, !buf.has_js, o) ;
			       if (p[2]) return ;   // found frame document
//if (confirm('flushing one buffer;\nhas_js=['+p[1]+']\nout=['+p[0]+']'))
			       o.write(p[0]) ;
			       buf.has_js= buf.has_js || p[1] ;
			       buf.buf= void 0 ;
			       break ;
			   }
		       }
//alert('about to o.close()') ;
		       // jsm-- upon next line, using gmail, Mozilla gives
		       //   "this page contains encrypted/unencrypted mix"
		       //   error.  It would seem to be something that's
		       //   previously written in Document.write() and
		       //   then flushed, but even if we skip the flushing,
		       //   it still happens.  The text that's written is
		       //   correctly proxified, and a Squid log shows there
		       //   is no insecure traffic from the browser.  ???
		       o.close() ;
//alert('ending Document.close()') ;
		   }
	case 'Location.replace':
	    return function (url) {
		       var u= _proxy_jslib_parse_full_url(o.toString()) ;
		       if (u) {
			   return o.replace(_proxy_jslib_full_url_by_frame(url, _proxy_jslib_unpack_flags(u[1])[5])) ;
		       } else {
			   return o.replace(_proxy_jslib_full_url(url)) ;
		       }
		   } ;
	case 'Location.toString':
	    return function () {
		       return _proxy_jslib_parse_full_url(o.toString())[2] ;
		   }
	case 'Window.setInterval':
	    return function (codefunc, interval) {
		       if (typeof(codefunc)=='function') {
			   // can't use apply() because of MSIE
			   // return o.setInterval.apply(o, arguments) ;
			   var a= new Array ;
			   for (var i= 0 ; i<arguments.length ; i++) a[i]= 'arguments['+i+']' ;
			   return eval('o.setInterval('+a.join()+')') ;
		       } else {
			   return o.setInterval(_proxy_jslib_proxify_js(codefunc), interval) ;
		       }
		   } ;
	case 'Window.setTimeout':
	    return function (codefunc, delay) {
		       if (typeof(codefunc)=='function') {
			   // can't use apply() because of MSIE
			   // return o.setTimeout.apply(o, arguments) ;
			   var a= new Array ;
			   for (var i= 0 ; i<arguments.length ; i++) a[i]= 'arguments['+i+']' ;
			   return eval('o.setTimeout('+a.join()+')') ;
		       } else {
			   return o.setTimeout(_proxy_jslib_proxify_js(codefunc), delay) ;
		       }
		   } ;
	case 'Document.cookie':
	    return _proxy_jslib_cookie_from_client() ;
	case 'Document.domain':
	    return _proxy_jslib_doc2win(o,_proxy_jslib_wins)._proxy_jslib_document_domain ;
	case 'Window.frames':
	    // jsm-- frames may not have _proxy_jslib_document_domain set, if
	    //   they don't have jslib inserted.  :(
	    var f ;
	    if (o._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(o) ;
	    for (f=0 ; f<o.frames.length ; f++) {
		if (o.frames[f]._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(o.frames[f]) ;
//if (o.frames[f]._proxy_jslib_document_domain==void 0) alert('o.frames[f]...domain is still undefined') ; // jsm
		if (o.frames[f]._proxy_jslib_document_domain!=o._proxy_jslib_document_domain) {
//		    alert('Tried to access o.frames, but prohibited; domains of o, o.frames[f]=['+o._proxy_jslib_document_domain+']['+o.frames[f]._proxy_jslib_document_domain+']') ;  // jsm-- test a bunch, then remove
		    return null ;
		}
	    }
	    return o.frames ;
	case 'Window.parent':
	    var w= (o.top._proxy_jslib_main_frame===o)  ? o  : o.parent ;
	    if (o._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(o) ;
	    if (w._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(w) ;
//alert('in parent; o.domain, w.domain=['+o._proxy_jslib_document_domain+']['+w._proxy_jslib_document_domain+']') ; // jsm-- remove
	    return (w._proxy_jslib_document_domain==o._proxy_jslib_document_domain)  ? w  : null ;
	case 'Window.top':
	    // if window uses frames, translate "top" to "top._proxy_jslib_main_frame".
	    var w= (o.top._proxy_jslib_main_frame!==void 0)  ? o.top._proxy_jslib_main_frame  : o.top ;
	    if (o._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(o) ;
	    if (w._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(w) ;
//alert('in Window.top; o, o.name, w, w.name=\n['+o+']['+o.name+']\n['+w+']['+w.name+']') ;  // jsm-- all alerts
//alert('in Window.top; o.domain, w.domain, caller=['+o._proxy_jslib_document_domain+']['+w._proxy_jslib_document_domain+']\n['+arguments.callee.caller+']') ; // jsm-- remove
return w ;  // jsm-- remove
	    return (w._proxy_jslib_document_domain==o._proxy_jslib_document_domain)  ? w  : null ;
	case 'Window.opener':
	    if (!o.opener) return null ;
	    if (o._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(o) ;
	    if (o.opener._proxy_jslib_document_domain==void 0) _proxy_jslib_init_domain(o.opener) ;
	    return (o.opener._proxy_jslib_document_domain==o._proxy_jslib_document_domain)
		?  o.opener  : null ;

	//  _proxy_jslib_parse_url() returns full_match, protocol, authentication, host, hostname, port, pathname, search, hash
	case 'Location.protocol':
	    return _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(o.href)[2])[1] ;
	case 'Location.host':
	    return _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(o.href)[2])[3] ;
	case 'Location.hostname':
	    return _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(o.href)[2])[4] ;
	case 'Location.port':
	    return _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(o.href)[2])[5] ;
	case 'Location.pathname':
	    return _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(o.href)[2])[6] ;
	case 'Location.search':
	    return _proxy_jslib_parse_url(_proxy_jslib_parse_full_url(o.href)[2])[7] ;


	// DOM methods in this block (and above, before this switch/case block)
	case 'Document.getElementById':
	    // Hack-- if element isn't in doc yet but is in output buffer, flush
	    //   buffer and try again.
	    return function (elementId) {
		       var e, i, buf, p ;
		       e= o.getElementById(elementId) ;
		       if (e!=null) return e ;
		       for (i= 0 ; i<_proxy_jslib_write_buffers.length ; i++)
			   if (_proxy_jslib_write_buffers[i].doc===o) break ;
		       if (i==_proxy_jslib_write_buffers.length) return null ;
		       buf= _proxy_jslib_write_buffers[i] ;
		       if (buf.buf==void 0) return null ;
		       if (buf.buf.match(new RegExp('id\\s*=\\s*[\'"]\\s*'+elementId+'\\s*[\'"]', 'i'))) {
			   p= _proxy_jslib_proxify_html(buf.buf, false, o) ;
			   if (p[2]) return ;   // found frame document
			   o.write(p[0]) ;
			   buf.has_js= buf.has_js || p[1] ;
			   buf.buf= void 0 ;
		       }
		       return o.getElementById(elementId) ;
		   }
	case 'Document.getElementsByTagName':
	    return function (tagname) {
		       var i, buf, p ;
		       for (i= 0 ; i<_proxy_jslib_write_buffers.length ; i++)
			   if (_proxy_jslib_write_buffers[i].doc===o) break ;
		       if (i==_proxy_jslib_write_buffers.length) return o.getElementsByTagName(tagname) ;
		       buf= _proxy_jslib_write_buffers[i] ;
		       if (buf.buf==void 0) return o.getElementsByTagName(tagname) ;
		       if (buf.buf.match(new RegExp('<\\s*'+tagname+'\\b', 'i'))) {
			   p= _proxy_jslib_proxify_html(buf.buf, false, o) ;
			   if (p[2]) return ;   // found frame document
			   o.write(p[0]) ;
			   buf.has_js= buf.has_js || p[1] ;
			   buf.buf= void 0 ;
		       }
		       return o.getElementsByTagName(tagname) ;
		   }

	case 'Range.insertNode':
	    return function (newNode) {
		       if (!_proxy_jslib_node_is_in_document(newNode))
			   return o.insertNode(_proxy_jslib_proxify_element(newNode)) ;
		       return o.insertNode(newNode) ;
		   }
	case 'Range.surroundContents':
	    return function (newParent) {
		       if (!_proxy_jslib_node_is_in_document(newParent))
			   return o.surroundContents(_proxy_jslib_proxify_element(newParent)) ;
		       return o.surroundContents(newParent) ;
		   }
	case 'NamedNodeMap.setNamedItem':
	    return function (node) {
		       node.nodeValue= _proxy_jslib_proxify_attribute(node.nodeName, node.nodeValue) ;
		       return o.setNamedItem(node) ;
		   }

	case 'CSSPrimitiveValue.setStringValue':
	    return function (type, value) {
		       if (type==CSSPrimitiveValue.CSS_URI)
			   return o.setStringValue(type, _proxy_jslib_full_url(value)) ;
		       return o.setStringValue(type, value) ;
		   }
	case 'CSSStyleDeclaration.setProperty':
	    return function (name, value, priority) {
		       return o.setProperty(name, _proxy_jslib_proxify_css(value), priority) ;
		   }


	// Netscape-specific in this block
	case 'Layer.load':
	    return function (url, width) {
		       return o.load(_proxy_jslib_full_url(url), width) ;
		   } ;


	// MSIE-specific in this block
	case 'Window.execScript':
	    return function(code, language) {
		       if (language.match(/^\s*(javascript|jscript|ecmascript|livescript|$)/i))
			   return o.execScript(_proxy_jslib_proxify_js(code), language) ;
		       // disallow scripts we don't support
		       return ;
		   }
	case 'Window.navigate':
	    return function (url) {
		       return o.navigate(_proxy_jslib_full_url(url)) ;
		   } ;
	case 'Window.showModalDialog':
	    return function(url, args, features) {
		       return o.showModalDialog(_proxy_jslib_full_url(url), args, features) ;
		   }
	case 'Window.showModelessDialog':
	    return function(url, args, features) {
		       return o.showModelessDialog(_proxy_jslib_full_url(url), args, features) ;
		   }
	// don't need to handle Document.parentWindow, do we?


	// non-standard but supported by both Mozilla and MSIE
	case 'XMLHttpRequest.open':
	    return function(method, url, asyncflag, username, password) {
		       // if absolute URL, verify domain is same as current page
		       if (url.match(/^[\w\+\.\-]*\:/)) {
			   var h1= (_proxy_jslib_parse_url(_proxy_jslib_URL))[4] ;
			   var h2= (_proxy_jslib_parse_url(url))[4] ;
//alert('h1,h2,url=['+h1+']['+h2+']\n['+url+']') ;
			   var d1= (h1.match(/(^|\.)(\w+\.\w+)$/))[2] ;
			   var d2= (h2.match(/(^|\.)(\w+\.\w+)$/))[2] ;
			   if (d1!=d2) return ;   // unallowed domain
		       }
		       // because of how this is used, don't insert the top form
		       return o.open(method, _proxy_jslib_full_url_by_frame(url,1), asyncflag, username, password) ;
		   }


	default:
	    if (typeof(o[property])=='function') {
		return function () {
			   // can't use apply() because of MSIE
			   // return o[property].apply(o, arguments) ;
			   var a= new Array ;
			   for (var i= 0 ; i<arguments.length ; i++) a[i]= 'arguments['+i+']' ;
			   return eval('o[property]('+a.join()+')') ;
		       } ;
	    } else {
		return o[property] ;
	    }

    }
}



// This is used when the property in question IS being assigned to, WITH an object.
function _proxy_jslib_assign (prefix, o, property, op, val) {
    var new_val, otype ;
//alert('starting assign; o, property, URL, old val, new val, caller=\n['+o+']\n['+property+']\n['+_proxy_jslib_URL+']\n['+((o!=void 0)?o[property]:'')+']\n['+val+']\n['+arguments.callee.caller+']') ;

    // guess when the window object is implied
//    if ((o===null)  && (window[property]!==void 0) && (window[property]===cur_val)) o= window ;

    // handle prefix
    if (prefix=='delete') return delete o[property] ;
    if (prefix=='++') {
	val= o[property]+1 ;
	op= '=' ;
    } else if (prefix=='--') {
	val= o[property]-1 ;
	op= '=' ;
    }

// sanity check
if (o==null) alert('in assign, o is null, property, caller=\n['+property+']\n['+arguments.callee.caller+']') ;

    otype= _proxy_jslib_object_type(o) ;

    // Simple variable assignment if o is a Node but not in a document yet.
    // Don't do this with Image objects, because they can load a URL even
    //   when not in a document.  Not very elegant here.
    if ((o.nodeName!==void 0) && (o.nodeType!==void 0) && (o.nodeValue!==void 0) && (otype!='Image')) {
	if (!_proxy_jslib_node_is_in_document(o))
	    return (op=='++' || op=='--')  ? eval('o[property]'+op)  : eval('o[property]'+op+'val') ;
    }


    var opmod= op.match(/=/)  ? op.replace(/=$/, '')  : '' ;

    // For unknown object types, transform common URL properties such as "src".
    //   It's better to proxify a property too much than to open a privacy hole,
    //   which is what happens if such a property is a URL that does not get
    //   proxified.  This also protects against when _proxy_jslib_object_type()
    //   doesn't ID an object correctly.
    // Don't do this if the value it's being assigned to is a non-String object.
    //   This helps when variables have the same name as properties.
    // We don't cover all combinations of properties and operators here; e.g.
    //   URL-like properties are unlikely to use ++ or --, and other
    //   combinations don't usually make sense.  We can revisit if needed.
    switch (property) {
	case 'src':
	case 'href':
	case 'background':
	case 'lowsrc':
	case 'action':
	case 'useMap':
	case 'longDesc':
	case 'cite':
	case 'codeBase':
	case 'location':
	    // test if val is a non-String object
	    if ((typeof val=='object') && (val.toLowerCase==void 0))
		return eval('o[property]'+op+'val') ;
	    if (opmod!='') {
		new_val= _proxy_jslib_parse_full_url(o[property])[2] ;
		eval('new_val' + op + 'val')
	    } else {
		new_val= val ;
	    }
//if (new_val.match(/[01]{6}[A-Z]/)) alert('in assign, about to call full_url; new_val, caller=\n['+new_val+']\n['+arguments.callee.caller+']') ;
	    // this won't catch e.g. "top.location.href=u"... :P
	    if ((property=='location') && (o.top===o)) {
		o[property]= _proxy_jslib_full_url_by_frame(new_val, false) ;
	    } else {
		o[property]= _proxy_jslib_full_url(new_val) ;
	    }
	    // return unproxified value
	    return new_val ;

	case 'profile':
	    var u= val.split(/\s+/) ;
	    for (var i= 0 ; i<u.length ; i++)
		u[i]= _proxy_jslib_full_url(u[i]) ;
	    o[property]= u.join(' ') ;
	    return val ;

	case 'cssText':
	    o[property]= _proxy_jslib_proxify_css(val) ;
	    return val ;

	// these are properties of HTMLElement, i.e. could be one of many object types
	case 'innerHTML':
	case 'outerHTML':
	case 'outerText':
	    if (opmod!='') { eval('new_val= o[property]' + opmod + 'val') }
	    else           { new_val= val }
	    o[property]= _proxy_jslib_proxify_html(new_val, false, (o.ownerDocument || o))[0] ;
	    return new_val ;

	// same for properties of Node
	case 'nodeValue':
	    if (opmod!='') { eval('new_val= o[property]' + opmod + 'val') }
	    else           { new_val= val }
	    o[property]= _proxy_jslib_proxify_attribute(property, new_val) ;
	    return new_val ;

	default:
	    var fu, u ;
	    if (otype=='Link' || otype=='Location') {
		fu= _proxy_jslib_parse_full_url(o.href) ;
		u=  _proxy_jslib_parse_url(fu[2]) ;
	    }
	    // u[] has full_match, protocol, authentication, host, hostname, port, pathname, search, hash
	    switch (otype+'.'+property) {
		// here we ignore case of "+=", etc.; revisit later if needed
		case 'Link.protocol':
		case 'Location.protocol':
		    val.toLowerCase() ;
		    o.href= _proxy_jslib_full_url(val+'//'+(u[2]!='' ? u[2]+'@' : '')+u[3]+u[6]+u[7]+u[8]) ;
		    return val ;
		case 'Link.host':
		case 'Location.host':
		    val.toLowerCase() ;
		    o.href= _proxy_jslib_full_url(u[1]+'//'+(u[2]!='' ? u[2]+'@' : '')+val+u[6]+u[7]+u[8]) ;
		    return val ;
		case 'Link.hostname':
		case 'Location.hostname':
		    o.href= _proxy_jslib_full_url(u[1]+'//'+(u[2]!='' ? u[2]+'@' : '')+val+(u[5]!='' ? ':'+u[5] : '')+u[6]+u[7]+u[8]) ;
		    return val ;
		case 'Link.port':
		case 'Location.port':
		    o.href= _proxy_jslib_full_url(u[1]+'//'+(u[2]!='' ? u[2]+'@' : '')+u[4]+(val!='' ? ':'+val : '')+u[6]+u[7]+u[8]) ;
		    return val ;
		case 'Link.pathname':
		case 'Location.pathname':
		    o.href= _proxy_jslib_full_url(u[1]+'//'+(u[2]!='' ? u[2]+'@' : '')+u[3]+val+u[7]+u[8]) ;
		    return val ;
		case 'Link.search':
		case 'Location.search':
		    o.href= _proxy_jslib_full_url(u[1]+'//'+(u[2]!='' ? u[2]+'@' : '')+u[3]+u[6]+val+u[8]) ;
		    return val ;
		case 'Document.cookie':
		    return (_proxy_jslib_cookies_are_banned_here
			    ? ''
			    : o.cookie= _proxy_jslib_cookie_to_client(val) ) ;
		case 'Document.domain':
		    var w= _proxy_jslib_doc2win(o,_proxy_jslib_wins) ;
		    // new domain must be suffix of old domain, must contain a
		    //   ".", and must be a complete domain suffix of old value
		    //   (tested here by prefixing with "." before suffix check,
		    //   but allowing if strings are equal).
		    if ( ( (('.'+val)==w._proxy_jslib_document_domain.slice(-val.length-1))
			   || (val==w._proxy_jslib_document_domain) )
			 && val.match(/\./) )
			return (w._proxy_jslib_document_domain= val) ;
//		    else alert('Warning: tried to set document.domain to illegal value: ['+val+'] old value: ['+w._proxy_jslib_document_domain+']') ;  // jsm
		    break ;
		case 'Attr.value':
		    o.value= _proxy_jslib_proxify_attribute(o.name, val) ;
		    return val ;
		case 'CSS2Properties.backgroundImage':
		case 'CSS2Properties.content':
		case 'CSS2Properties.cursor':
		case 'CSS2Properties.listStyleImage':
		    o[property]= _proxy_jslib_proxify_css(val) ;
		    return val ;

		default:
		    return (op=='++' || op=='--')  ? eval('o[property]'+op)  : eval('o[property]'+op+'val') ;
	    }
    }
}



// This is used when the property in question IS being assigned to, WITHOUT an object.
// The value returned is the value to set the variable to.
function _proxy_jslib_assign_rval (prefix, property, op, val, cur_val) {

    // handle prefix
    if (prefix=='delete') return undefined ;  // not quite the same as delete, but close enough
    if (prefix=='++') {
	val= 1 ;
	op= '+=' ;
    } else if (prefix=='--') {
	val=  1 ;
	op= '-=' ;
    }

    if ((typeof val=='object') && (val.toLowerCase==void 0))
	return val ;
    var new_val= cur_val ;
    eval('new_val' + op + 'val') ;

    switch (property) {
	// when there's no object, "location" is the only property that needs proxification
	// jsm-- what about "with" statements?
	case 'location':
	    return _proxy_jslib_full_url(new_val) ;
	default:
	    return new_val ;
    }
}




//---- below are used to support the API functions above ---------------


function _proxy_jslib_add_to_write_buffer(doc, html) {
    var i ;
    for (i= 0 ; i<_proxy_jslib_write_buffers.length ; i++) {
	if (_proxy_jslib_write_buffers[i].doc===doc) break ;
    }
    if (i==_proxy_jslib_write_buffers.length || _proxy_jslib_write_buffers[i].doc!==doc) {
	_proxy_jslib_write_buffers[_proxy_jslib_write_buffers.length]= { doc: doc, buf: html } ;
    } else {
	if (_proxy_jslib_write_buffers[i].buf==void 0) _proxy_jslib_write_buffers[i].buf= '' ;
	_proxy_jslib_write_buffers[i].buf+= html ;
    }
}


// careful-- output of document.write() may be (erroneously?) parsed and
//   executed immediately after document.write() statement.  To help with
//   that, we clear the buffer before calling document.write().
// Hack here for JS insertions-- if document was created and nothing written on
//   it yet, then insert the JS library if needed.
// Another hack-- since _proxy_jslib_write_buffers may be reset if what's
//   written includes jslib, we exit the loop if that happens.
function _proxy_jslib_flush_write_buffers() {
    var buf, i, p ;

    for (i= 0 ; (_proxy_jslib_write_buffers!=void 0) && (i<_proxy_jslib_write_buffers.length) ; i++) {
	buf= _proxy_jslib_write_buffers[i] ;
	if (buf.buf==void 0) continue ;

	p= _proxy_jslib_proxify_html(buf.buf, !buf.has_js, buf.doc) ;
	if (p[2]) return ;   // found frame document
//var w= _proxy_jslib_doc2win(buf.doc,_proxy_jslib_wins) ; if (w!=void 0) w= w.name ;
//if (confirm('flushing buffer;\nwin=['+w+']\nhas_js=['+p[1]+']\nout=['+p[0]+']'))
	buf.has_js= buf.has_js || p[1] ;
	buf.buf= void 0 ;
	buf.doc.write(p[0]) ;
    }
}



function _proxy_jslib_new_function() {
    arguments[arguments.length-1]= _proxy_jslib_proxify_js(arguments[arguments.length-1]) ;
    // can't use apply() because of MSIE
    // return Function.apply(null, arguments) ;  // Function() same w/ or w/o "new"
    var a= new Array ;
    for (var i= 0 ; i<arguments.length ; i++) a[i]= 'arguments['+i+']' ;
    return eval('Function('+a.join()+')') ;
}


function _proxy_jslib_doc2win(d, wins) {
    var f ;
    for (var i= 0 ; i<wins.length ; i++) {
	if (wins[i].document===d) return wins[i] ;
	if (wins[i].frames.length && (f= _proxy_jslib_doc2win(d, wins[i].frames))) return f ;
    }
    return null ;
}


function _proxy_jslib_node_is_in_document(node) {
    for ( ; (node!=null) && (node.nodeType!=9) ; node= node.parentNode) ;   // Node.DOCUMENT_NODE==9
    return node!=null ;
}



function _proxy_jslib_init_domain(w) {
    // jsm-- not a great solution
    if (w.document.URL=='about:blank') {
	w.setTimeout(function () { _proxy_jslib_init_domain(window) }, 50) ;
	return ;
    }
    var URL= w.document.URL.replace(/^wyciwyg:\/\/\d+\//i, '') ;
    URL= _proxy_jslib_parse_full_url(URL)[2] ;
    URL= decodeURIComponent(URL) ;
    if (URL=='about:blank') {
	w.setTimeout(function () { _proxy_jslib_init_domain(window) }, 200) ;
	return ;
    }
    w._proxy_jslib_document_domain= _proxy_jslib_parse_url(URL)[4] ;
}



function _proxy_jslib_full_url(uri_ref) {
    if (uri_ref==null) return '' ;
//if (uri_ref.match(/\/[01]{6}[A-Z]\//)) alert('in full_url, uri_ref, caller=\n['+uri_ref+']\n['+arguments.callee.caller+']') ;
//if (uri_ref.match(/_proxy_jslib_flush_write_buffers/)) alert('in full_url, uri_ref, caller=\n['+uri_ref+']\n['+arguments.callee.caller+']') ; 

    uri_ref= uri_ref.replace(/^\s+|\s+$/g, '') ;
    if (/^x\-proxy\:\/\//i.test(uri_ref))  return '' ;

    if (/^(javascript|livescript)\:/i.test(uri_ref)) {
	uri_ref= uri_ref.replace(/^(javascript|livescript)\:/i, '') ;
	return 'javascript:' + _proxy_jslib_proxify_js(uri_ref, 1) ;
    }

    var uf= uri_ref.match(/^([^\#]*)(\#.*)?/) ;
    var uri= uf[1] ;
    var frag=  uf[2]  ? uf[2]  : '' ;
    if (uri=='')  return uri_ref ;

    uri= uri.replace(/[\r\n]/g, '') ;

    var absurl ;
    if      (/^[\w\+\.\-]*\:/.test(uri))  { absurl= uri               }
    else if (/^\/\//.test(uri))           { absurl= _proxy_jslib_base_scheme + uri }
    else if (/^\//.test(uri))             { absurl= _proxy_jslib_base_host   + uri }
    else if (/^\?/.test(uri))             { absurl= _proxy_jslib_base_file   + uri }
    else                                  { absurl= _proxy_jslib_base_path   + uri }

    return _proxy_jslib_url_start + _proxy_jslib_wrap_proxy_encode(absurl) + frag ;
}


function _proxy_jslib_full_url_by_frame(uri_ref, is_frame) {
    var old_url_start= _proxy_jslib_url_start ;
    _proxy_jslib_url_start= is_frame  ? _proxy_jslib_url_start_inframe  : _proxy_jslib_url_start_noframe ;
    var ret= _proxy_jslib_full_url(uri_ref) ;
    _proxy_jslib_url_start= old_url_start ;
    return ret ;
}


function _proxy_jslib_wrap_proxy_encode(URL) {
    var uf= URL.match(/^([^\#]*)(\#.*)?/) ;
    var uri= uf[1] ;
    var frag=  uf[2]  ? uf[2]  : '' ;

    uri= _proxy_jslib_proxy_encode(uri) ;
    uri= uri.replace(/\=/g, '=3d').replace(/\?/g, '=3f').replace(/\#/g, '=23').replace(/\%/g, '=25') ;
    while (uri.match(/\/\//)) uri= uri.replace(/\/\//g, '/=2f') ;

    return uri + frag ;
}

function _proxy_jslib_wrap_proxy_decode(enc_URL) {
    var uf= enc_URL.match(/^([^\#]*)(\#.*)?/) ;
    var uri= uf[1] ;
    var frag=  uf[2]  ? uf[2]  : '' ;

    uri= uri.replace(/\=(..)/g, function (s,p1) { return String.fromCharCode(eval('0x'+p1)) } ) ;
    uri= _proxy_jslib_proxy_decode(uri) ;

    return uri + frag ;
}



function _proxy_jslib_cookie_to_client(cookie) {
    if (_proxy_jslib_cookies_are_banned_here) return '' ;

    var u= _proxy_jslib_parse_url(_proxy_jslib_URL) ;
    if (u==null) {
	alert("CGIProxy Error: Can't parse URL <"+_proxy_jslib_URL+">; not setting cookie.") ;
	return '' ;
    }
    var source_server= u[4] ;
    var source_path= u[6] ;
    if (source_path.substr(0,1)!='/') source_path= '/' + source_path ;

    var name, value, expires_clause, path, domain, secure_clause ;
    var new_name, new_value, new_cookie ;

    name= value= expires_clause= path= domain= secure_clause=
	new_name= new_value= new_cookie= '' ;

    if (/^\s*([^\=\;\,\s]*)\s*\=?\s*([^\;\s]*)/.test(cookie)) {
	name= RegExp.$1 ; value= RegExp.$2 ;
    }
    if (/\;\s*(expires\s*\=[^\;]*)/i.test(cookie))        expires_clause= RegExp.$1 ;
    if (/\;\s*path\s*\=\s*([^\;\,\s]*)/i.test(cookie))    path= RegExp.$1 ;
    if (/\;\s*domain\s*\=\s*([^\;\,\s]*)/i.test(cookie))  domain= RegExp.$1 ;
    if (/\;\s*(secure\b)/i.test(cookie))                  secure_clause= RegExp.$1 ;

    if (path=='') path= _proxy_jslib_COOKIE_PATH_FOLLOWS_SPEC  ? source_path  : '/' ;

    if (domain=='') {
	domain= source_server ;
    } else {
	domain= domain.replace(/\.+$/, '') ;
	domain= domain.replace(/\.{2,}/g, '.') ;
	if ( (source_server.substr(source_server.length-domain.length)!=domain.toLowerCase()) && ('.'+source_server!=domain) )
	    return '' ;
	var dots= domain.match(/\./g) ;
	if (_proxy_jslib_RESPECT_THREE_DOT_RULE) {
	    if (dots.length<3 && !( dots.length>=2 && /\.(com|edu|net|org|gov|mil|int)$/i.test(domain) ) )
		return '' ;
	} else {
	    if (dots.length<2) {
		if (domain.match(/^\./)) return '' ;
		domain= '.'+domain ;
		if (dots.length<1) return '' ;
	    }
	}
    }

    new_name=  _proxy_jslib_cookie_encode('COOKIE;'+name+';'+path+';'+domain) ;
    new_value= _proxy_jslib_cookie_encode(value+';'+secure_clause) ;

    if (_proxy_jslib_SESSION_COOKIES_ONLY && (expires_clause!='')) {
	/^expires\s*\=\s*(.*)$/i.test(expires_clause) ;
	var expires_date= RegExp.$1.replace(/\-/g, ' ') ;  // Date.parse() can't handle "-"
	if ( Date.parse(expires_date) > (new Date()).getTime() ) expires_clause= '' ;
    }

    new_cookie= new_name+'='+new_value ;
    if (expires_clause!='') new_cookie= new_cookie+'; '+expires_clause ;
    new_cookie= new_cookie+'; path='+_proxy_jslib_SCRIPT_NAME+'/' ;
    if (secure_clause!='')  new_cookie= new_cookie+'; '+secure_clause ;

    return new_cookie ;
}


function _proxy_jslib_cookie_from_client() {
    if (_proxy_jslib_cookies_are_banned_here) return '' ;
    if (!document.cookie) return '' ;

    var target_path, target_server, target_scheme ;
    var u= _proxy_jslib_parse_url(_proxy_jslib_URL) ;
    if (u==null) {
	alert("CGIProxy Error: Can't parse URL <"+_proxy_jslib_URL+">; not using cookie.") ;
	return ;
    }
    target_scheme= u[1] ;
    target_server= u[4] ;
    target_path= u[6] ;
    if (target_path.substr(0,1)!='/') target_path= '/' + target_path ;

    var matches= new Array() ;
    var pathlen= new Object() ;
    var cookies= document.cookie.split(/\s*;\s*/) ;
    for (var c in cookies) {
	var nv= cookies[c].split(/=/, 2) ;
	var name=  _proxy_jslib_cookie_decode(nv[0]) ;
	var value= _proxy_jslib_cookie_decode(nv[1]) ;
	var n= name.split(/;/) ;
	if (n[0]=='COOKIE') {
	    var cname, path, domain, cvalue, secure ;
	    cname= n[1] ; path= n[2] ; domain= n[3].toLowerCase() ;
	    var v= value.split(/;/) ;
	    cvalue= v[0] ; secure= v[1] ;
	    if (secure!='' && secure!=null && target_scheme!='https:') continue ;
	    if ( ((target_server.substr(target_server.length-domain.length)==domain)
		  || (domain=='.'+target_server))
		&& target_path.substr(0, path.length)==path )
	    {
		matches[matches.length]= cname  ? cname+'='+cvalue  : cvalue ;
		pathlen[cname+'='+cvalue]= path.length ;
	    }
	}
    }

    matches.sort(function (v1,v2) { return (pathlen[v2]-pathlen[v1]) } ) ;

    return matches.join('; ') ;
}




// returns [new_html, needs_jslib, found_frameset]
function _proxy_jslib_proxify_html(html, is_full_page, doc) {
    var out= '' ;
    var m1, m2, m3, rem ;
    var html_pos, head_pos ;

    if (is_full_page) _proxy_jslib_needs_jslib= false ;

    while (html!='') {
	var start= html.match(/^[^\<]*/) ;
	out+= start[0] ;
	html= html.substr(start[0].length) ;

	// because not all browsers support non-greedy matching, next six
	//   blocks emulate it.
	if (m1= html.match(/^\<\!\-\-/)) {
	    rem= html.substr(4) ;
	    if (rem.match(/\-\-\>/)) {
		m2= rem.match(/\-\-\s*\>/) ;
	    } else if (m2= rem.match(/\>/)) {
	    } else {
		html= '' ;
		break ;
	    }
	    var comment= html.substr(0, 4+m2.index+m2[0].length) ;
	    html= html.substr(comment.length) ;
	    out+= _proxy_jslib_proxify_comment(comment) ;
	    continue ;
	}

	if (m1= html.match(/^\<\s*script\b/i)) {
	    rem= html.substr(m1[0].length) ;
	    if (m2= rem.match(/\<\s*\/script\b/i)) {
		rem= html.substr(m1[0].length+m2.index+m2[0].length) ;
		if (m3= rem.match(/\>/)) {
		    var script_block= html.substr(0, m1[0].length+m2.index+m2[0].length+m3.index+1) ;
		    html= html.substr(script_block.length) ;
		    out+= _proxy_jslib_proxify_script_block(script_block) ;
		    continue ;
		}
	    }
	}

	if (m1= html.match(/^\<\s*style\b/i)) {
	    rem= html.substr(m1[0].length) ;
	    if (m2= rem.match(/\<\s*\/style\b/i)) {
		rem= html.substr(m1[0].length+m2.index+m2[0].length) ;
		if (m3= rem.match(/\>/)) {
		    var style_block= html.substr(0, m1[0].length+m2.index+m2[0].length+m3.index+1) ;
		    html= html.substr(style_block.length) ;
		    out+= _proxy_jslib_proxify_style_block(style_block) ;
		    continue ;
		}
	    }
	}

	if (m1= html.match(/^\<\![^\>]*\>?/)) {
	    out+= _proxy_jslib_proxify_decl_bang(m1[0]) ;
	    html= html.substr(m1[0].length) ;
	    continue ;
	}

	if (m1= html.match(/^\<\?[^\>]*\>?/)) {
	    out+= _proxy_jslib_proxify_decl_question(m1[0]) ;
	    html= html.substr(m1[0].length) ;
	    continue ;
	}

	// gmail has a bug where a <body> tag is missing the closing ">",
	//   so disallow "<" in tag here to avoid including following tag.
	//if (m1= html.match(/^\<[^\>]*\>?/)) {
	if (m1= html.match(/^\<[^\<>]*\>?/)) {
//if (m1[0].match(/[01]{6}[A-Z]/)) alert('in proxify_html; m1[0], caller=\n['+m1[0]+']\n'+arguments.callee.caller+']') ;
	    // For JS insertions, and <frameset> tags.  Ignore declarations for now.
	    if (m2= m1[0].match(/^\<\s*([A-Za-z][\w\.\:\-]*)/)) {
		var tag_name= m2[1].toLowerCase() ;
		if ((tag_name=='frameset') && _proxy_jslib_doing_insert_here && !_proxy_jslib_is_in_frame) {
		    _proxy_jslib_return_frame_doc(_proxy_jslib_wrap_proxy_encode(_proxy_jslib_URL), doc) ;
		    return ['', false, true] ;
		}
		if      (tag_name=='html') { html_pos= out.length }
		else if (tag_name=='head') { head_pos= out.length }
	    }

	    out+= _proxy_jslib_proxify_element(m1[0]) ;
	    html= html.substr(m1[0].length) ;

	    continue ;
	}

	break ;
    }

    // This handles rare case.  Don't worry about top insertion.  Hacky.
    if (is_full_page && _proxy_jslib_needs_jslib) {

	var jslib_block= '<script type="text/javascript" src="'
		       + _proxy_jslib_html_escape(_proxy_jslib_url_start+_proxy_jslib_wrap_proxy_encode('x-proxy://scripts/jslib'))
		       + '"><\/script>\n' ;
	var base_url_jsq= _proxy_jslib_global_replace(_proxy_jslib_base_url, /(["\\])/,
						      function (p) { return '\\'+p[1] } ) ;
	var insert_string= '<script type="text/javascript">_proxy_jslib_pass_vars("'
			 + base_url_jsq + '",'
			 + _proxy_jslib_cookies_are_banned_here + ','
			 + _proxy_jslib_doing_insert_here + ','
			 + _proxy_jslib_SESSION_COOKIES_ONLY + ','
			 + _proxy_jslib_COOKIE_PATH_FOLLOWS_SPEC + ','
			 + _proxy_jslib_RESPECT_THREE_DOT_RULE + ',"'
			 + _proxy_jslib_default_script_type + '","'
			 + _proxy_jslib_default_style_type + '");<\/script>\n' ;
	var insert_pos= head_pos || html_pos || 0 ;
	out= out.substring(0,insert_pos) + jslib_block + insert_string + out.substring(insert_pos) ;
    }

    return [out, _proxy_jslib_needs_jslib] ;
}



// For now, remove script-written comments and other SGML declarations.
function _proxy_jslib_proxify_comment(comment) {
    return '' ;
}

function _proxy_jslib_proxify_decl_bang(decl_bang) {
    return '' ;
}

function _proxy_jslib_proxify_decl_question(decl_question) {
    return '' ;
}


function _proxy_jslib_proxify_script_block(script_block) {
    var m1, m2, tag, script, attrs, attr, name ;
    attr= new Object() ;

    m1= script_block.match(/^(\<\s*script\b[^\>]*\>)((.|\s)*)\<\s*\/script\b[^\>]*\>$/i) ;
    var o_n_j= _proxy_jslib_needs_jslib ;   // hack hack
    tag= _proxy_jslib_proxify_html(m1[1])[0] ;
    _proxy_jslib_needs_jslib= o_n_j ;
    script= m1[2] ;
    attrs= tag.match(/^\<\s*script\b([^\>]*)\>/i)[1] ;

    while (m2= attrs.match(/([A-Za-z][\w\.\:\-]*)\s*(\=\s*(\"([^\"\>]*)\"?|\'([^\'\>]*)\'?|([^\'\"][^\s\>]*)))?/)) {
	attrs= attrs.substr(m2[0].length) ;
	name= m2[1].toLowerCase() ;
	if (attr[name]!=null) continue ;
	attr[name]= m2[4]  ? m2[4]  : m2[5]  ? m2[5]  : m2[6]  ? m2[6]  : '' ;
	attr[name]= _proxy_jslib_html_unescape(attr[name]) ;
    }
    if (attr.type!=null) attr.type= attr.type.toLowerCase() ;
    if (!attr.type && attr.language) {
	attr.type= attr.language.match(/javascript|ecmascript|livescript|jscript/i)
						     ? 'application/x-javascript'
		 : attr.language.match(/css/i)       ? 'text/css'
		 : attr.language.match(/vbscript/i)  ? 'application/x-vbscript'
		 : attr.language.match(/perl/i)      ? 'application/x-perlscript'
		 : attr.language.match(/tcl/i)       ? 'text/tcl'
		 : '' ;
    }
    if (!attr.type) attr.type= _proxy_jslib_default_script_type ;

    // For now, don't worry about "</script" inside JS-written scripts.

    script= _proxy_jslib_proxify_block(script, attr.type, 1) ;

    return tag+script+'<\/script>' ;
}


function _proxy_jslib_proxify_style_block(style_block) {
    var m1, m2, tag, stylesheet, attrs, type ;
    m1= style_block.match(/^(\<\s*style\b[^\>]*\>)((.|\s)*)\<\s*\/style\b[^\>]*\>$/i) ;
    var o_n_j= _proxy_jslib_needs_jslib ;   // hack hack
    tag= _proxy_jslib_proxify_html(m1[1])[0] ;
    _proxy_jslib_needs_jslib= o_n_j ;
    stylesheet= m1[2] ;
    attrs= tag.match(/^\<\s*style\b([^\>]*)\>/i)[1] ;

    while (m2= attrs.match(/([A-Za-z][\w\.\:\-]*)\s*(\=\s*(\"([^\"\>]*)\"?|\'([^\'\>]*)\'?|([^\'\"][^\s\>]*)))?/)) {
	attrs= attrs.substr(m2[0].length) ;
	if (m2[1].toLowerCase()=='type') {
	    type= m2[4]!=null  ? m2[4]  : m2[5]!=null  ? m2[5]  : m2[6]!=null  ? m2[6]  : '' ;
	    type= _proxy_jslib_html_unescape(type).toLowerCase() ;
	    break ;
	}
    }
    if (!type) type= _proxy_jslib_default_style_type ;
    stylesheet= _proxy_jslib_proxify_block(stylesheet, type, 1) ;

    return tag+stylesheet+'<\/style>' ;
}



// used for tags, element nodes (nodes+children are modified in place), and
//   DocumentFragment nodes.
function _proxy_jslib_proxify_element(element) {
    // Unfortunately, attr{} may have extra properties if a Web page changes
    //   anything in the Object prototype.  Thus, we use names[] to keep track
    //   of the tag's attributes.  We do this elsewhere too.
    var m1, m2, tag_name, attrs, attr, names, name, i, rebuild, end_slash,
	old_url_start, flags ;
    attr= new Object() ;
    names= new Array() ;

    // handle either tag strings or Node objects
    if (typeof element=='string') {
	if (!(m1= element.match(/^\<\s*([A-Za-z][\w\.\:\-]*)\s*([^\>]*)/))) return element ;
	tag_name= m1[1].toLowerCase() ;
	attrs= m1[2] ;
	// ignore possibility of <frameset> tag
	if (attrs=='') return element ;

	while (m2= attrs.match(/([A-Za-z][\w\.\:\-]*)\s*(\=\s*(\"([^\"\>]*)\"?|\'([^\'\>]*)\'?|([^\'\"][^\s\>]*)))?/)) {
	    attrs= attrs.substr(m2.index+m2[0].length) ;
	    name= m2[1].toLowerCase() ;
	    if (attr[name]!=null) { rebuild= 1 ; continue }
	    attr[name]= (m2[4]!='' && m2[4]!=void 0) ? m2[4]
		      : (m2[5]!='' && m2[5]!=void 0) ? m2[5]
		      : (m2[6]!='' && m2[6]!=void 0) ? m2[6]
		      : '' ;
	    attr[name]= _proxy_jslib_html_unescape(attr[name]) ;
	    names[names.length]= name ;
	}

    } else if (typeof element=='object') {
	// don't proxify Image objects, since they're already proxified
	if (_proxy_jslib_object_type(element)=='Image') return element ;
	
	if (element.nodeType==1) {           // Node.ELEMENT_NODE
	    tag_name= element.nodeName.toLowerCase() ;
	    for (i= 0 ; i<element.attributes.length ; i++) {
		name= element.attributes[i].nodeName.toLowerCase() ;
		attr[name]= element.attributes[i].nodeValue ;
		names[names.length]= name ;
	    }
	} else if (element.nodeType==11) {   // Node.DOCUMENT_FRAGMENT_NODE
	    for (i= 0 ; i<element.childNodes.length ; i++)
		_proxy_jslib_proxify_element(element.childNodes[i]) ;
	    return element ;
	} else if (element.nodeType==3) {     // Node.TEXT_NODE
	    return element ;
	} else {
	    alert('Error: unhandled node type==['+element.nodeType+']\ncaller=[\n'+arguments.callee.caller+']') ;
	    return ;
	}
    } else {
	alert('Error: unhandled typeof==['+(typeof element)+ ']') ;
	return ;
    }


    // Now we have tag_name, attr[], and names[] set.

//    for (name in attr) {
    for (i= 0 ; i<names.length ; i++) {
	name= names[i] ;
	// for now, simply delete attributes with script macros
	if (attr[name].match(/\&\{.*\}\;/)) { delete attr[name] ; rebuild= 1 ; continue }

	if (name.match(/^on/)) {
	    attr[name]= _proxy_jslib_proxify_block(attr[name], _proxy_jslib_default_script_type, 1) ;
	    rebuild= 1 ;
	}
    }

    if (attr.style!=null) {
	if (attr.style.match(/(expression|function)\s*\(/i ))
	    attr.style= _proxy_jslib_global_replace(attr.style, /\b((expression|function)\s*\()([^\)]*)/i,
						    function (p) { return p[1]+_proxy_jslib_proxify_js(p[3]) } ) ;

	attr.style= _proxy_jslib_proxify_block(attr.style, _proxy_jslib_default_style_type, 1) ;
	rebuild= 1 ;
    }

    // huge simplification of tag-specific block
    if (attr.href!==void 0)        { attr.href=        _proxy_jslib_full_url(attr.href) ;        rebuild= 1 }
    if (attr.src!==void 0)         {
	if (tag_name=='frame' || tag_name=='iframe') {
				     attr.src=         _proxy_jslib_full_url_by_frame(attr.src,1) ; rebuild= 1 ;
	} else if (tag_name=='script') {   // messy  :P
	    var old_url_start= _proxy_jslib_url_start ;
	    var flags= _proxy_jslib_unpack_flags(_proxy_jslib_packed_flags) ;
	    flags[6]= (attr.type!==void 0)  ? attr.type  : _proxy_jslib_default_script_type ;
	    _proxy_jslib_url_start= _proxy_jslib_SCRIPT_NAME + '/' + _proxy_jslib_pack_flags(flags) + '/' ;
				     attr.src=         _proxy_jslib_full_url(attr.src) ;         rebuild= 1 ;
	    _proxy_jslib_needs_jslib= true ;
	    _proxy_jslib_url_start= old_url_start ;
	} else                     { attr.src=         _proxy_jslib_full_url(attr.src) ;         rebuild= 1 }
    }
    if (attr.lowsrc!==void 0)      { attr.lowsrc=      _proxy_jslib_full_url(attr.lowsrc) ;      rebuild= 1 }
    if (attr.dynsrc!==void 0)      { attr.dynsrc=      _proxy_jslib_full_url(attr.dynsrc) ;      rebuild= 1 }
    if (attr.action!==void 0)      { attr.action=      _proxy_jslib_full_url(attr.action) ;      rebuild= 1 }
    if (attr.background!==void 0)  { attr.background=  _proxy_jslib_full_url(attr.background) ;  rebuild= 1 }
    if (attr.usemap!==void 0)      { attr.usemap=      _proxy_jslib_full_url(attr.usemap) ;      rebuild= 1 }
    if (attr.cite!==void 0)        { attr.cite=        _proxy_jslib_full_url(attr.cite) ;        rebuild= 1 }
    if (attr.longdesc!==void 0)    { attr.longdesc=    _proxy_jslib_full_url(attr.longdesc) ;    rebuild= 1 }
    if (attr.codebase!==void 0)    { attr.codebase=    _proxy_jslib_full_url(attr.codebase) ;    rebuild= 1 }
    if (attr.pluginspage!==void 0) { attr.pluginspage= _proxy_jslib_full_url(attr.pluginspage) ; rebuild= 1 }

    if ((tag_name=='meta') && attr['http-equiv'].match(/^\s*refresh\b/i)) {
	attr.content= _proxy_jslib_global_replace(
			  attr.content,
			  /(\;\s*URL\=)\s*(\S*)/i,
			  function (a) { return a[1] + _proxy_jslib_full_url(a[2]) } ) ;
	rebuild= 1 ;
    }

    // Now attr[] has been modified correctly.


    if (typeof element == 'object') {
	if (rebuild)
	    for (i= 0 ; i<element.attributes.length ; i++)
		element.attributes[i].nodeValue= attr[element.attributes[i].nodeName.toLowerCase()] ;
	for (i= 0 ; i<element.childNodes.length ; i++) {
	    if (element.childNodes[i].nodeType==1) {
		_proxy_jslib_proxify_element(element.childNodes[i]) ;
	    }
	}
	return element ;
    }

    if (!rebuild) return element ;

    attrs= '' ;
//    for (name in attr) {
    for (i= 0 ; i<names.length ; i++) {
	name= names[i] ;
	if (attr[name]==null) continue ;
	if (attr[name]=='')  { attrs+= ' '+name ; continue }
	if (!attr[name].match(/\"/) || attr[name].match(/\'/)) {
	    attrs+= ' '+name+'="'+_proxy_jslib_html_escape(attr[name])+'"' ;
	} else {
	    attrs+= ' '+name+"='"+_proxy_jslib_html_escape(attr[name])+"'" ;
	}
    }

    end_slash= element.match(/\/\s*>?$/)  ? ' /'  : '' ;
    return '<'+tag_name+attrs+end_slash+'>' ;
}


function _proxy_jslib_element2tag (e) {
    var ret= '', i ;
if (e.nodeType!=1) alert('in element2tag; nodeType=['+e.nodeType+']') ;
    for (i= 0 ; i<e.attributes.length ; i++)
	ret+= ' '+e.attributes[i].nodeName+'="'+e.attributes[i].nodeValue+'"' ;
    ret= '<'+e.tagName+ret+'>' ;
    for (i=0 ; i<e.childNodes.length ; i++)
	if      (e.childNodes[i].nodeType==1) ret+= '\n'+_proxy_jslib_element2tag(e.childNodes[i]) ;
	else if (e.childNodes[i].nodeType==3) ret+= '\n'+e.childNodes[i].nodeValue ;
    return ret ;
}



// This mimics much of _proxy_jslib_proxify_element(), above.
function _proxy_jslib_proxify_attribute(name, value) {
    if (/\&\{.*\}\;/.test(value)) return ;

    name= name.toLowerCase() ;

    // when proxifying URL, assume it's in a frame, since most of the time this
    //   routine is called it will be in a frame... not perfect....
    if (/^(href|src|lowsrc|dynsrc|action|background|usemap|cite|longdesc|codebase)$/i.test(name)) {
	//return _proxy_jslib_full_url(value) ;
	return _proxy_jslib_full_url_by_frame(value, true) ;
    } else if (/^on/i.test(name)) {
	return _proxy_jslib_proxify_block(value, _proxy_jslib_default_script_type, 1)
    } else if (/^style$/i.test(name)) {
	if (/(expression|function)\s*\(/i.test(value)) return ;
	else return value ;
    } else {
	return value ;
    }
}



function _proxy_jslib_proxify_block(s, type, unknown_type_ok) {
    type= type.toLowerCase() ;

    if (type=='text/css') {
	return _proxy_jslib_proxify_css(s) ;

    } else if (type.match(/^(application\/x\-javascript|application\/x\-ecmascript|application\/javascript|application\/ecmascript|text\/javascript|text\/ecmascript|text\/livescript|text\/jscript)$/)) {
	return _proxy_jslib_proxify_js(s, 1) ;

    } else {
	return unknown_type_ok ? s : '' ;
    }
}



function _proxy_jslib_proxify_css(css) {
    var out= '', m1 ;
    while (m1= css.match(/url\s*\(\s*(([^\)]*\\\))*[^\)]*)(\)|$)/i)) {
	out+= css.substr(0,m1.index) + 'url(' + _proxy_jslib_css_full_url(m1[1]) + ')' ;
	css= css.substr(m1.index+m1[0].length) ;
    }
    out+= css ;

    css= out ;
    out= '' ;
    while (m1=css.match(/\@import\s*(\"[^"]*\"|\'[^']*\'|[^\;\s\<]*)/i)) {
	if (!m1[1].match(/^url\s*\(/i)) {   // to avoid use of "(?!...)"
	    out+= css.substr(0,m1.index) + '@import ' + _proxy_jslib_css_full_url(m1[1]) ;
	} else {
	    out+= css.substr(0,m1.index) + m1[0] ;
	}
	css= css.substr(m1.index+m1[0].length) ;
    }
    out+= css ;

    return out ;
}


function _proxy_jslib_css_full_url(url) {
    var q= '' ;
    url= url.replace(/\s+$/, '') ;
    if      (url.match(/^\"/)) { q= '"' ; url= url.replace(/^\"|\"$/g, '') }
    else if (url.match(/^\'/)) { q= "'" ; url= url.replace(/^\'|\'$/g, '') }
    url= url.replace(/\\(.)/, "$1").replace(/^\s+|\s+$/g, '') ;
    url= _proxy_jslib_full_url(url) ;
    url= _proxy_jslib_global_replace(url, /([\(\)\,\s\'\"\\])/,
				     function (p) { return '\\'+p[1] } ) ;
    return q+url+q ;
}



function _proxy_jslib_return_frame_doc(enc_URL, doc) {
    var top_URL= _proxy_jslib_html_escape(_proxy_jslib_url_start_inframe
					  + _proxy_jslib_wrap_proxy_encode('x-proxy://frames/topframe?URL='
								      + encodeURIComponent(enc_URL) ) ) ;
    var page_URL= _proxy_jslib_html_escape(_proxy_jslib_url_start_inframe + enc_URL) ;
    doc.open();
    doc.write('<html>\n<frameset rows="80,*">\n'
	    + '<frame src="'+top_URL+'">\n<frame src="'+page_URL+'" name="_proxy_jslib_main_frame">\n'
	    + '</frameset>\n</html>') ;
    doc.close() ;
//alert('in return_frame_doc, after writing doc; top_URL, page_URL=\n['+top_URL+']\n['+page_URL+']') ;
}



//---- everything needed to handle proxify_js() ------------------------


function _proxy_jslib_proxify_js(s, top_level) {
    if ((s==void 0) || (s=='')) return s ;

    // hack for eval()-- return unchanged if it's not a string or String object
    if (!s.match) return s ;

    var RE ;

    if (!_proxy_jslib_RE) {  // saves time for multiple calls
	RE= new Object() ;

	// count embedded parentheses carefully when using all these in matches!
	RE.WhiteSpace= '[\x09\x0b\x0c \xa0]' ;
	RE.LineTerminator= '[\x0a\x0d]' ;

	// messy without non-greedy matching
	RE.Comment= '\\/\\*\\/*([^\\*]\\/|[^\\*\\/]*|\\**[^\\/])*\\*\\/|\\/\\/[^\\x0a\\x0d]*|\\<\\!\\-\\-[^\\x0a\\x0d]*' ;

	RE.IdentifierStart= '[a-zA-Z\\$\\_]|\\\\u[\da-fA-F]{4}' ;
	RE.IdentifierPart= RE.IdentifierStart+'|\\d' ;
	RE.IdentifierName= '('+RE.IdentifierStart+')('+RE.IdentifierPart+')*' ;

	RE.Punctuator= '\\>\\>\\>\\=?|\\=\\=\\=|\\!\\=\\=|\\<\\<\\=|\\>\\>\\=|[\\<\\>\\=\\!\\+\\*\\%\\&\\|\\^\\-]\\=|\\+\\+|\\-\\-|\\<\\<|\\>\\>|\\&\\&|\\|\\||[\\{\\}\\(\\)\\[\\]\\.\\;\\,\\<\\>\\+\\*\\%\\&\\|\\^\\!\\~\\?\\:\\=\\-]' ;
	RE.DivPunctuator= '\\/\\=?' ;

	RE.NumericLiteral= '0[xX][\\da-fA-F]+|(0|[1-9]\\d*)(\\.\\d*)?([eE][\\+\\-]?\\d+)?|\\.\\d+([eE][\\+\\-]?\\d+)?' ;
	RE.EscapeSequence= 'x[\\da-fA-F]{2}|u[\\da-fA-F]{4}|0|[^\\dxu]' ;
	RE.StringLiteral= '"([^\\"\\\\\\x0a\\x0d]|\\\\('+RE.EscapeSequence+'))*"|'
			+ "'([^\\'\\\\\\x0a\\x0d]|\\\\("+RE.EscapeSequence+"))*'" ;
	RE.RegularExpressionLiteral= '\\/([^\\x0a\\x0d\\*\\\\\\/]|\\\\[^\\x0a\\x0d])([^\\x0a\\x0d\\\\\\/]|\\\\[^\\x0a\\x0d])*\\/('+RE.IdentifierPart+')*' ;

	RE.Token= RE.IdentifierName+'|'+RE.NumericLiteral+'|'+RE.Punctuator+'|'+RE.StringLiteral ;
	RE.InputElementDiv= '^('+RE.WhiteSpace+'+|'+RE.LineTerminator+'|'+RE.Comment+
			    '|('+RE.Token+'|'+RE.DivPunctuator+'|'+RE.RegularExpressionLiteral+'))' ;
	RE.InputElementRegExp= '^('+RE.WhiteSpace+'+|'+RE.LineTerminator+'|'+RE.Comment+
			       '|('+RE.Token+'|'+RE.RegularExpressionLiteral+'|'+RE.DivPunctuator+'))' ;

	RE.SKIP= RE.WhiteSpace+'+|'+RE.LineTerminator+'|'+RE.Comment ;
	RE.SKIP_NO_LT= RE.WhiteSpace+'+|'+RE.Comment ;

	// make RegExp objects out of the ones we'll use
	RE.InputElementDiv= new RegExp(RE.InputElementDiv) ;
	RE.InputElementRegExp= new RegExp(RE.InputElementRegExp) ;
	RE.N_S_RE= new RegExp('^('+RE.NumericLiteral+'|'+RE.StringLiteral+'|'+RE.RegularExpressionLiteral+')$') ;
	RE.DOTSKIPEND= new RegExp('\\.('+RE.SKIP+')*$') ;
	// avoid (?!...) construct for older compatibility; thus needs extra check when using it
//	RE.NEXTISASSIGN= new RegExp('^('+RE.SKIP+')*(\\>\\>\\>\\=|\\<\\<\\=|\\>\\>\\=|[\\+\\*\\/\\%\\&\\|\\^\\-]?\\=(?!\\=))') ;
	RE.NEXTISASSIGN= new RegExp('^('+RE.SKIP+')*(\\>\\>\\>\\=|\\<\\<\\=|\\>\\>\\=|[\\+\\*\\/\\%\\&\\|\\^\\-]?\\=)') ;
	RE.NEXTISINCDEC= new RegExp('^('+RE.SKIP_NO_LT+')*(\\+\\+|\\-\\-)') ;
	RE.SKIPTOPAREN= new RegExp('^(('+RE.SKIP+')*\\()') ;
	RE.SKIPTOCOLON= new RegExp('^(('+RE.SKIP+')*\\:)') ;
	RE.SKIPTOCOMMASKIP= new RegExp('^(('+RE.SKIP+')*\\,('+RE.SKIP+')*)') ;
	RE.PUNCDIVPUNC= new RegExp('^('+RE.Punctuator+'|'+RE.DivPunctuator+')$') ;
	RE.SKIPTOIDENTIFIER= new RegExp('^('+RE.SKIP+')*('+RE.IdentifierName+')') ;
	RE.SKIPTOOFRAG= new RegExp('^('+RE.SKIP+')*([\\.\\[\\(])') ;

	_proxy_jslib_RE= RE ;

    } else {
	RE= _proxy_jslib_RE ;
    }


    var out, element, token, last_token, new_last_token, newline_since_last_token, div_ok,
	term_so_far= '', prefix= '', sub_expr, op, new_val, cur_val_str, inc_by,
	does_write, in_braces= 0, in_func= false, expr,
	var_decl, varname, eq, value, skip1, skip2, funcname,
	match, m2, o_p, o, p ;
    out= new Array() ;

    // s is constantly trimmed from the front
  OUTER:
    while(1) {
	if (div_ok) {
	    if (!(match= s.match(RE.InputElementDiv))) break ;
	} else {
	    if (!(match= s.match(RE.InputElementRegExp))) break ;
	}
	s= s.slice(match.index+match[0].length) ;

	element= match[0] ;
	token= match[3] ;

	if (token) {
	    div_ok= /^(\)|\]|\+\+|\-\-)$|^([a-zA-Z\$\_\\\d'"]|\.\d|\/..)/.test(token) ;
	    if (/^(case|delete|do|else|in|instanceof|new|return|throw|typeof|void)$/.test(token)) div_ok= false ;
	}

	if (/^[\x0a\x0d]$/.test(element)) newline_since_last_token= true ;
	new_last_token= '' ;

	if (token=='{') {
	    in_braces++ ;
	} else if (token=='}') {
	    if (--in_braces==0) in_func= false ;
	}

	if (!token) {
	    if (term_so_far) term_so_far+= element ;
	    else out[out.length]= element ;

	} else if (match= token.match(/^\_proxy(\d*)(\_jslib\_.*)/))   {
	    term_so_far+= '_proxy'+(match[1]+1)+match[2] ;

	} else if (RE.N_S_RE.test(token)) {
	    out[out.length]= prefix + term_so_far ;
	    prefix= '' ;
	    term_so_far= token ;


	} else if (/^(\+\+|\-\-|delete)$/.test(token)) {
	    if (token=='--' && (match= s.match(/^\s*\>/))) {
		s= s.slice(match.index+match[0].length) ;
		out[out.length]= prefix + term_so_far + '-->' ;
		prefix= term_so_far= '' ;
	    } else if (term_so_far!='' && !newline_since_last_token) {
		out[out.length]= prefix + term_so_far + token ;
		prefix= term_so_far= '' ;
	    } else {
		out[out.length]= prefix + term_so_far ;
		prefix= term_so_far= '' ;
		o_p= _proxy_jslib_get_next_js_term() ;
		o= o_p[0] ;
		p= o_p[1] ;
		if (p==void 0) break ;
		if (o!='') {
		    out[out.length]= "_proxy_jslib_assign('" + token + "', ("
				   + _proxy_jslib_proxify_js(o) + "), ("
				   + _proxy_jslib_proxify_js(p) + "), '')" ;
		} else {
		    p= p.match(/^'(.*)'$/)[1] ;
		    out[out.length]= "(" + p + "= _proxy_jslib_assign_rval('"
				   + token + "', '" + p + "', '', '', " + p + "))" ;
		}
	    }


	} else if (token=='eval' && (match= s.match(RE.SKIPTOPAREN))) {
	    s= s.slice(match.index+match[0].length) ;
	    _proxy_jslib_needs_jslib= true ;
	    term_so_far+= token + match[1] + '_proxy_jslib_proxify_js('
			+ _proxy_jslib_proxify_js(_proxy_jslib_get_next_js_expr(1))
			+ '))' ;
	    if (!(match= s.match(/^\)/))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    div_ok= true ;


	} else if (/^(open|write|writeln|close|replace|load|eval|setInterval|setTimeout|toString|src|href|background|lowsrc|action|location|useMap|longDesc|cite|codeBase|profile|cssText|insertRule|setStringValue|setProperty|backgroundImage|content|cursor|listStyleImage|host|hostname|pathname|port|protocol|search|insertNode|surroundContents|setNamedItem|getElementById|getElementsByTagName|innerHTML|outerHTML|outerText|insertAdjacentHTML|setAttribute|setAttributeNode|appendChild|insertBefore|replaceChild|nodeValue|value|cookie|domain|frames|parent|top|opener|execScript|navigate|showModalDialog|showModelessDialog)$/.test(token)) {
	    _proxy_jslib_needs_jslib= true ;
	    does_write= does_write || (token=='write') || (token=='writeln') || (token=='eval') ;
	    if ( newline_since_last_token
		 &&   /^(\)|\]|\+\+|\-\-)$|^([a-zA-Z\$\_\\\d'"]|\.\d|\/..)/.test(last_token)
		 && ! /^(case|delete|do|else|in|instanceof|new|typeof|void|function|var)$/.test(last_token) )
	    {
		out[out.length]= prefix + term_so_far ;
		prefix= term_so_far= '' ;
	    }
	    term_so_far= term_so_far.replace(RE.DOTSKIPEND, '') ;

	    if (/^[\{\,]/.test(last_token) && (match= s.match(RE.SKIPTOCOLON))) {
		s= s.slice(match.index+match[0].length) ;
		out[out.length]= prefix + term_so_far + token + match[1] ;
		prefix= term_so_far= '' ;
		new_last_token= ':' ;
		div_ok= false ;

	    } else if (prefix!='') {
		if (term_so_far=='') {
		    out[out.length]= token+"= _proxy_jslib_assign_rval('"+prefix+"', '"+token+"', '"+op+"', '', "+token+")" ;
		} else {
		    term_so_far= "_proxy_jslib_assign('"+prefix+"', "+term_so_far+", '"+token+"', '', '')" ;
		}
		prefix= '' ;
		new_last_token= ')' ;
		div_ok= true ;
	    } else if (match= s.match(RE.NEXTISINCDEC)) {
		s= s.slice(match.index+match[0].length) ;
		op= match[3] ;
		if (term_so_far=='') {
		    out[out.length]= token+"= _proxy_jslib_assign_rval('', '"+token+"', '"+op+"', '', "+token+")" ;
		} else {
		    term_so_far= "_proxy_jslib_assign('', "+term_so_far+", '"+token+"', '"+op+"', '')" ;
		}
		new_last_token= ')' ;
		div_ok= true ;
	    // use charAt() to avoid use of (?!...) construct in regex
	    } else if ((match= s.match(RE.NEXTISASSIGN)) && (s.charAt(match[0].length)!='=')) {
		s= s.slice(match.index+match[0].length) ;
		op= match[3] ;
		new_val= _proxy_jslib_proxify_js(_proxy_jslib_get_next_js_expr()) ;
		if (term_so_far=='') {
		    out[out.length]= token+"= _proxy_jslib_assign_rval('', '"+token+"', '"+op+"', ("+new_val+"), "+token+")" ;
		} else {
		    term_so_far= "_proxy_jslib_assign('', "+term_so_far+", '"+token+"', '"+op+"', ("+new_val+"))" ;
		}
		new_last_token= ')' ;
		div_ok= false ;
	    } else {
		cur_val_str= (term_so_far=='')  ? (', '+token)  : '' ;
		if (term_so_far=='') term_so_far= 'null' ;
		term_so_far= "_proxy_jslib_handle("+term_so_far+", '"+token+"'"+cur_val_str+")" ;
		new_last_token= ')' ;
		div_ok= true ;
	    }


	// Skip these for the JS version-- they require %IN_CUSTOM_INSERTION
	//   etc. and would be rare anyway.  Revisit later if needed.
	//} else if (/^(applets|embeds|forms|ids|layers|anchors|images|links)$/.test(token)) {

	} else if (/^(if|while|for|with|switch)$/.test(token)) {
	    out[out.length]= prefix + term_so_far + token ;
	    prefix= term_so_far= '' ;
	    if (!(match= s.match(RE.SKIPTOPAREN))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    out[out.length]= match[1] + _proxy_jslib_proxify_js(_proxy_jslib_get_next_js_expr(1)) + ')' ;
	    if (!(match= s.match(/^\)/))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    div_ok= false ;

	} else if (token=='catch') {
	    out[out.length]= prefix + term_so_far + token ;
	    prefix= term_so_far= '' ;
	    if (!(match= s.match(RE.SKIPTOPAREN))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    out[out.length]= match[1] + _proxy_jslib_get_next_js_expr(1) + ')' ;
	    if (!(match= s.match(/^\)/))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    div_ok= false ;

	} else if (token=='function') {
	    out[out.length]= prefix + term_so_far + token ;
	    prefix= term_so_far= '' ;
	    //if (!(match= s.match('^(('+RE.SKIP+')*)('+RE.IdentifierName+')?(('+RE.SKIP+')*\\()')))
	    if (!(match= s.match('^(('+RE.SKIP+')*)('+RE.IdentifierName+'(\\.('+RE.IdentifierName+'))*)?(('+RE.SKIP+')*\\()')))
		break ;
	    s= s.slice(match.index+match[0].length) ;
	    //skip1= match[1] ; funcname= match[4] ; skip2= match[7] ;
	    skip1= match[1] ; funcname= match[4] || '' ; skip2= match[11] ;
	    if (m2= funcname.match(/^_proxy(\d*)_jslib_/))
		funcname= '_proxy' + (m2[1]+1) + funcname.replace(/^_proxy(\d*)/, '') ;
	    out[out.length]= skip1 + funcname + skip2 + _proxy_jslib_get_next_js_expr(1) + ') {' ;
	    if (!(match= s.match(/^\)\s*\{/))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    in_braces++ ;
	    in_func= true ;
	    div_ok= false ;

	} else if (token=='var') {
	    out[out.length]= prefix + term_so_far + token ;
	    prefix= term_so_far= '' ;
	    while (1) {
		var_decl= _proxy_jslib_get_next_js_expr(0) ;
		if (!(match= var_decl.match('^(('+RE.SKIP+')*)('+RE.IdentifierName+'('+RE.SKIP+')*)(\\=|in)?((x|[^x])*)$')))
		    break OUTER ;
		skip1= match[1] ; varname= match[4] ; eq= match[9] ; value= match[10] ;
		if (m2= varname.match(/^_proxy(\d*)_jslib_/))
		    varname= '_proxy' + (m2[1]+1) + varname.replace(/^_proxy(\d*)/, '') ;
		out[out.length]= skip1 + varname ;
		if (eq) out[out.length]= eq + _proxy_jslib_proxify_js(value) ;
		if (!(match= s.match(/^\,/))) break ;
		s= s.slice(match.index+match[0].length) ;
		out[out.length]= ',' ;
	    }
	    div_ok= false ;

	} else if (token=='new') {
	    out[out.length]= prefix + term_so_far ;
	    prefix= term_so_far= '' ;
	    if (match= s.match('^('+RE.SKIP+')*Function\\b')) {
		s= s.slice(match.index+match[0].length) ;
		out[out.length]= '_proxy_jslib_new_function' ;
	    } else {
		out[out.length]= token ;
	    }

	} else if ((token=='return') && !in_func && top_level && does_write) {
	    out[out.length]= prefix + term_so_far ;
	    prefix= term_so_far= '' ;
	    _proxy_jslib_needs_jslib= true ;
	    expr= _proxy_jslib_get_next_js_expr(0) ;
	    while (match=s.match(RE.SKIPTOCOMMASKIP)) {
		s= s.slice(match.index+match[0].length) ;
		expr+= ', ' + _proxy_jslib_get_next_js_expr(0) ;
	    }
	    expr= _proxy_jslib_proxify_js(expr) ;
	    out[out.length]= 'return ((_proxy_jslib_ret= ('+expr+')), _proxy_jslib_flush_write_buffers(), _proxy_jslib_ret)' ;
	    div_ok= false ;


	} else if (/^(abstract|boolean|break|byte|case|char|class|const|continue|debugger|default|delete|do|else|enum|export|extends|final|finally|float|goto|implements|in|instanceof|int|interface|long|native|package|private|protected|return|short|static|synchronized|throw|throws|transient|try|typeof|void|volatile)$/.test(token)) {
	    out[out.length]= prefix + term_so_far + token ;
	    prefix= term_so_far= '' ;

	} else if (token.match('^('+RE.IdentifierName+')$')) {
	    if ( newline_since_last_token
		 &&   /^(\)|\]|\+\+|\-\-)$|^([a-zA-Z\$\_\\\d'"]|\.\d|\/..)/.test(last_token)
		 && ! /^(case|delete|do|else|in|instanceof|new|typeof|void|function|var)$/.test(last_token) )
	    {
		out[out.length]= prefix + term_so_far ;
		prefix= '' ;
		term_so_far= token ;
	    } else {
		term_so_far+= token ;
	    }

	} else if (token=='.') {
	    term_so_far+= '.' ;

	} else if (token=='(') {
	    does_write= 1 ;
	    term_so_far+= '(' + _proxy_jslib_proxify_js(_proxy_jslib_get_next_js_expr(1)) + ')' ;
	    if (!(match= s.match(/^\)/))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    new_last_token= ')' ;
	    div_ok= true ;


	} else if (token=='[') {
	    sub_expr= _proxy_jslib_proxify_js(_proxy_jslib_get_next_js_expr(1)) ;
	    if (!(match= s.match(/^\]/))) break ;
	    s= s.slice(match.index+match[0].length) ;
	    if (term_so_far) {
		_proxy_jslib_needs_jslib= true ;
		new_last_token= ')' ;
		if (prefix!='') {
		    term_so_far= "_proxy_jslib_assign('"+prefix+"', "+term_so_far+", ("+sub_expr+"), '', '')" ;
		    prefix= '' ;
		    div_ok= true ;
		} else if (match= s.match(RE.NEXTISINCDEC)) {
		    s= s.slice(match.index+match[0].length) ;
		    op= match[3] ;
		    term_so_far= "_proxy_jslib_assign('', "+term_so_far+", ("+sub_expr+"), '"+op+"', '')" ;
		    div_ok= true ;
		// use charAt() to avoid use of (?!...) construct in regex
		} else if ((match= s.match(RE.NEXTISASSIGN)) && (s.charAt(match[0].length)!='=')) {
		    s= s.slice(match.index+match[0].length) ;
		    op= match[3] ;
		    new_val= _proxy_jslib_proxify_js(_proxy_jslib_get_next_js_expr()) ;
		    term_so_far= "_proxy_jslib_assign('', "+term_so_far+", ("+sub_expr+"), '"+op+"', ("+new_val+"))" ;
		    div_ok= false ;
		} else {
		    term_so_far= "_proxy_jslib_handle("+term_so_far+", ("+sub_expr+"))" ;
		    div_ok= true ;
		}
	    } else {
		term_so_far= '['+sub_expr+']' ;
		new_last_token= ']' ;
		div_ok= true ;
	    }


	} else if (RE.PUNCDIVPUNC.test(token)) {
	    out[out.length]= prefix + term_so_far + token ;
	    prefix= term_so_far= '' ;

	} else {
	    // shouldn't get here
	}

	if (token) {
	    last_token= new_last_token  ? new_last_token  : token ;
	    newline_since_last_token= false ;
	}

    }

    out[out.length]= prefix + term_so_far ;

    if (top_level && does_write) {
	out[out.length]= ' ;\n_proxy_jslib_flush_write_buffers() ;' ;
	_proxy_jslib_needs_jslib= true ;
    }

    return out.join('') ;



    // nesting the function lets us access and modify s
    function _proxy_jslib_get_next_js_expr(allow_multiple) {
	var out= new Array(), p= new Array(), element, token, div_ok, last_token ;

	while (1) {
	    if (div_ok) {
		if (!(match= s.match(RE.InputElementDiv))) break ;
	    } else {
		if (!(match= s.match(RE.InputElementRegExp))) break ;
	    }
	    s= s.slice(match.index+match[0].length) ;
	    element= match[0] ;
	    token= match[3] ;

	    if (element==';' || element==',') {
		if (!allow_multiple && p.length==0) {
		    s= element+s ;
		    return out.join('') ;
		}

	    } else if (/^[\x0a\x0d]$/.test(element)) {
		if ( !allow_multiple && p.length==0
		     &&   /^(\)|\]|\+\+|\-\-)$|^([a-zA-Z\$\_\\\d'"]|\.\d|\/..)/.test(last_token)
		     && ! /^(case|delete|do|else|in|instanceof|new|typeof|void|function|var)$/.test(last_token)
		     &&   RE.SKIPTOIDENTIFIER.test(s) )
		{
		    s= element+s ;
		    return out.join('') ;
		}

	    } else if (/^[\(\[\{\?]$/.test(element)) {
		p[p.length]= element ;

	    } else if (/^[\)\]\}\:]$/.test(element)) {
		if (p.length==0) {
		    s=element+s ;
		    return out.join('') ;
		}
		if (p.length>0 && !(element==':' && p[p.length-1]!='?')) p.length-- ;
		if (element=='}' && p.length==0 && !allow_multiple) {
		    out[out.length]= element ;
		    return out.join('') ;
		}
	    }

	    out[out.length]= element ;

	    if (token) {
		div_ok= /^(\)|\]|\+\+|\-\-)$|^([a-zA-Z\$\_\\\d'"]|\.\d|\/..)/.test(token) ;
		if (/^(case|delete|do|else|in|instanceof|new|return|throw|typeof|void)$/.test(token)) div_ok= false ;
		last_token= token ;
	    }
	}

	return p.length==0  ? out.join('')  : void 0 ;
    }



    function _proxy_jslib_get_next_js_term() {
	var o= '', p, ofrag, match, m2 ;

	if (!(match= s.match(RE.SKIPTOIDENTIFIER))) return [void 0, void 0] ;
	s= s.slice(match.index+match[0].length) ;
	p= "'" + match[3] + "'" ;
	ofrag= match[3] ;

	while (match= s.match(RE.SKIPTOOFRAG)) {
	    s= s.slice(match.index+match[0].length) ;
	    o+= ofrag ;
	    if (match[3]=='.') {
		if (!(m2= s.match(RE.SKIPTOIDENTIFIER))) return [void 0, void 0] ;
		s= s.slice(m2.index+m2[0].length) ;
		p= "'" + m2[3] + "'" ;
		ofrag= '.' + m2[3] ;
	    } else if (match[3]=='[') {
		p= _proxy_jslib_get_next_js_expr(1) ;
		ofrag= '[' + p + ']' ;
		if (!s.match(/^\]/)) return [void 0, void 0] ;
		s= s.slice(1) ;
	    } else if (match[3]=='(') {
		p= '' ;
		ofrag= '(' + _proxy_jslib_get_next_js_expr(1) + ')' ;
		if (!s.match(/^\)/)) return [void 0, void 0] ;
		s= s.slice(1) ;
	    }
	}
	return [o, p] ;
    }

}



//---- utilities -------------------------------------------------------


// Take a heuristic guess at the type of an object, based on existing properties.
// This will only detect types that are specifically handled below.  For types
//   that are not, this returns ''.
// typeof(), instanceof, o.constructor, and o.toString() each have their problems.  :P
// Properties are carefully selected to test correctly in both Mozilla and MSIE;
//   objects and properties vary slightly by browser.
// Unfortunately, Mozilla has a bug such that it dies when HTMLAreaElement.pathname
//   is accessed.  Thus, we wrap this in a try/catch block.
// This approach will probably have to be replaced at some point.
// jsm-- use browser-dependent approach.
function _proxy_jslib_object_type(o) {
    if ((o==null) || (typeof(o)!='object')) return null ;   // can't use instanceof

    try {
	if ((o.navigator!==void 0) && (o.clearInterval!==void 0) && (o.moveBy!==void 0) && (o.self===o.window))
	    return 'Window' ;
	if ((o.alinkColor!==void 0) && (o.cookie!==void 0) && (o.writeln!==void 0))
	    return 'Document' ;
	if ((o.pathname!==void 0) && (o.protocol!==void 0) && (o.target!==void 0))
	    return 'Link' ;   // must do before 'Location'
	if ((o.pathname!==void 0) && (o.protocol!==void 0)  && (o.search!==void 0))
	    return 'Location' ;
	if ((o.background!==void 0) && (o.parentLayer!==void 0) && (o.moveAbove!==void 0))
	    return 'Layer' ;
	if ((o.hspace!==void 0) && (o.src!==void 0) && (o.border!==void 0))
	    return 'Image' ;
	if ((o.action!==void 0) && (o.encoding!==void 0) && (o.submit!==void 0))
	    return 'Form' ;
	if ((o.ownerElement!==void 0) && (o.specified!==void 0))
	    return 'Attr' ;
	if ((o.nodeName!==void 0) && (o.nodeType!==void 0) && (o.nodeValue!==void 0))
	    return 'Node' ;  // must be here after descendents of Node
	if ((o.getNamedItem!==void 0) && (o.removeNamedItem!==void 0) && (o.setNamedItem!==void 0))
	    return 'NamedNodeMap' ;
	if ((o.cloneRange!==void 0) && (o.compareBoundaryPoints!==void 0) && (o.surroundContents!==void 0))
	    return 'Range' ;
	if ((o.azimuth!==void 0) && (o.backgroundAttachment!==void 0) && (o.pageBreakInside!==void 0))
	    return 'CSS2Properties' ;
	if ((o.primitiveType!==void 0) && (o.getRectValue!==void 0) && (o.getCounterValue!==void 0))
	    return 'CSSPrimitiveValue' ;
	if ((o.getPropertyCSSValue!==void 0) && (o.getPropertyPriority!==void 0) && (o.removeProperty!==void 0))
	    return 'CSSStyleDeclaration' ;
	if ((o.getAllResponseHeaders!==void 0) && (o.getResponseHeader!==void 0) && (o.setRequestHeader!==void 0))
	    return 'XMLHttpRequest' ;   // non-standard but usually supported
    } catch(e) {
//	alert(e) ;
    }

    return '' ;
}



// Using JS (not RFC) terminology, this returns:
//   protocol, authentication, host, hostname, port, pathname, search, hash
function _proxy_jslib_parse_url(URL) {
    var u= URL.match(/^([\w\+\.\-]+\:)\/\/([^\/\?\#\@]*\@)?(([^\:\/\?\#]*)(\:[^\/\?\#]*)?)([^\?\#]*)([^#]*)(.*)$/) ;
    if (u==null) return ;   // if pattern doesn't match
    for (var i= 0 ; i<u.length ; i++)  if (u[i]==void 0) u[i]= '' ;
    u[1]= u[1].toLowerCase() ;
    u[2]= u[2].replace(/\@$/, '') ;
    u[3]= u[3].toLowerCase() ;
    u[3]= u[3].replace(/\.+(:|$)/, '$1') ;  // close potential exploit
    u[4]= u[4].toLowerCase() ;
    u[4]= u[4].replace(/\.+$/, '') ;      // close potential exploit
    u[5]= u[5].replace(/^\:/, '') ;
    return u ;
}


// returns url_start (NOT including packed flags), packed flags, and decoded target URL.
function _proxy_jslib_parse_full_url(URL) {
    if (URL=='about:blank') return ['', '', 'about:blank'] ;
    if (URL.match(/^javascript\:/i)) return ['', '', URL] ;
    if (URL=='') return ['', '', ''] ;

    var cmp, path_cmp ;

    if (_proxy_jslib_PROXY_GROUP.length) {
	for (var i in _proxy_jslib_PROXY_GROUP) {
	    if (URL.substring(0,_proxy_jslib_PROXY_GROUP[i].length)==_proxy_jslib_PROXY_GROUP[i]) {
		path_cmp= URL.substring(_proxy_jslib_PROXY_GROUP[i].length).match(/\/([^\/\?]*)\/?([^\?]*)(\??.*)/) ;
		if (path_cmp==null) alert("CGIProxy Error: Can't parse URL <"+URL+"> with PROXY_GROUP; not setting all variables correctly.") ;
		return [_proxy_jslib_PROXY_GROUP[i],
			path_cmp[1],
			_proxy_jslib_wrap_proxy_decode(path_cmp[2])+path_cmp[3]] ;
	    }
	}
    }

    cmp= URL.match(/^([\w\+\.\-]+)\:\/\/([^\/\?]*)([^\?]*)(\??.*)$/) ;
    if (cmp==null) return ;
    path_cmp= cmp[3].match(_proxy_jslib_RE_FULL_URL) ;
    if (cmp==null || path_cmp==null) alert("CGIProxy Error: Can't parse URL <"+URL+">; not setting all variables correctly.") ;

    return [cmp[1]+"://"+cmp[2]+path_cmp[1],
	    path_cmp[2],
	    _proxy_jslib_wrap_proxy_decode(path_cmp[3])+cmp[4]] ;
}


function _proxy_jslib_pack_flags(flags) {
    var pflags= new Array() ;
    for (var i= 0 ; i<6 ; i++) { pflags[i]= (flags[i]==1) ? '1' : '0' }
    pflags[6]= String.fromCharCode(_proxy_jslib_MIME_TYPE_ID[flags[6]]+65) ;  // only works through #26
    return pflags.join('') ;
}

function _proxy_jslib_unpack_flags(flagst) {
    var flags= flagst.split('') ;
    for (var i= 0 ; i<6 ; i++) { flags[i]= (flags[i]=='1') ? 1 : 0 }
    flags[6]= flags[6].charCodeAt(0)-65 ;     // only works through #26
    flags[6]= _proxy_jslib_ALL_TYPES[flags[6]] ;
    return flags ;
}



function _proxy_jslib_html_escape(s) {
    if (s==void 0) return '' ;
    return s.replace(/\&/g, '&amp;')
	    .replace(/\"/g, '&quot;')
	    .replace(/\</g, '&lt;')
	    .replace(/\>/g, '&gt;') ;
}

function _proxy_jslib_html_unescape(s) {
    if (s==void 0) return '' ;
    return s.replace(/\&quot\b\;?/g, '"')
	    .replace(/\&lt\b\;?/g,   '<')
	    .replace(/\&gt\b\;?/g,   '>')
	    .replace(/\&amp\b\;?/g,  '&') ;
}


// The replace() method in Netscape is broken, :( :( so we have to implement
//   our own.  The bug is that if a function is used as the replacement pattern
//   (needed for anything complex), then *any* replace() or match() (and others?)
//   within that function (or in called functions) will cause its $' to
//   be used in place of the calling replace()'s $' .  :P
// Call this function with a string, a NON-GLOBAL (!) pattern with possible
//   parentheses, and a callback function that takes one argument that is the
//   array resulting from s.match(pattern), and returns a replacement string.
// Because of how this is implemented, ^ in pattern works much like Perl's \G.
function _proxy_jslib_global_replace(s, pattern, replace_function) {
    var out= '' ;
    var m1 ;
    while ((m1=s.match(pattern))!=null) {
	out+= s.substr(0,m1.index) + replace_function(m1) ;
	s= s.substr(m1.index+m1[0].length) ;
    }
    return out+s ;
}


//----------------------------------------------------------------------


EOF
    } # end setting of $JSLIB_BODY


    print "$HTTP_1_X 200 OK\015\012",
	  "Expires: $expires_header\015\012",
	  "Date: $date_header\015\012",
	  "Content-Type: application/x-javascript\015\012",
	  "Content-Length: ", length($JSLIB_BODY), "\015\012",
	  "\015\012",
	  $JSLIB_BODY ;

    goto EXIT ;
}
