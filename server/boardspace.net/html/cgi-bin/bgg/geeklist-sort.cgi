#!/usr/bin/perl
#
# geeklist-sort.cgi -- reorder a BoardGameGeek geeklist by thumbs received.
#
#   (no params)              form (or BGG login if no token yet)
#   action=login             POST username/password: sign in to BGG, keep the token in a cookie
#   action=logout            forget the token cookie
#   action=preview&listid=N  read the list + thumbs, show the proposed order
#   action=apply             re-check the list, then save the previewed order
#
# Uses BGG's undocumented web API (the calls BGG's own pages make):
#   POST  https://boardgamegeek.com/login/api/v1  {"credentials":{"username":..,"password":..}}
#         -> 2xx + session cookies, 400 {"errors":{"message":..}} on bad login
#   GET   https://boardgamegeek.com/api/accounts/current  (with those cookies) -> {"authToken":..}
#   GET   https://api.geekdo.com/api/listitems?listid=N&page=P         25 per page
#   GET   https://api.geekdo.com/api/listitem/ID/reaction?totalonly=1  {"thumbs":N}
#   PATCH https://api.geekdo.com/api/geeklist/N/listitem  {"data":[{"id":"..."},...]}  -> 204
#   Authorization: GeekAuth <authToken>
#
# Token sources, first found wins: ?token=xx, the login cookie, $AUTH_FILE.
# The password is used once to sign in and is never stored. The token cookie is
# HttpOnly, SameSite=Strict, Secure under https, and scoped to this script's path.
# Put this script behind Apache authentication and serve it over https.
#
# Needs only core Perl modules plus IO::Socket::SSL and Net::SSLeay for https
# (Debian: libio-socket-ssl-perl libnet-ssleay-perl).

use strict;
use warnings;
use HTTP::Tiny;
use JSON::PP;
use Time::HiRes qw(sleep);

my $AUTH_FILE    = $ENV{BGG_AUTH_FILE} || '/etc/boardspace/bgg-authtoken';
my $DEFAULT_LIST = '12240';
my $API          = 'https://api.geekdo.com/api';
my $BGG          = 'https://boardgamegeek.com';
my $COOKIE       = 'bgg_authtoken';
my $DELAY        = 0.15;   # seconds between BGG requests

$| = 1;
binmode STDOUT, ':encoding(UTF-8)';
my $json = JSON::PP->new->utf8;

my %q = read_params();
my $action = $q{action} || '';
my $listid = ($q{listid} || $DEFAULT_LIST);
$listid =~ /^\d+$/ or $listid = $DEFAULT_LIST;
my $is_post = ($ENV{REQUEST_METHOD} || '') eq 'POST';

my $param_token  = clean_token($q{token});
my $cookie_token = clean_token(read_cookies()->{$COOKIE});
my @set_cookie;
my $login_error;

if ($action eq 'login' && $is_post)
{
    my ($tok, $err) = bgg_login($q{username} // '', $q{password} // '');
    if ($tok)
    {
        $cookie_token = $tok;
        push @set_cookie, cookie_header($tok, 0);
        $action = '';
    }
    else
    {
        $login_error = $err;
    }
}
elsif ($action eq 'logout')
{
    undef $cookie_token;
    push @set_cookie, cookie_header('', 1);
    $action = '';
}

my $auth = $param_token // $cookie_token // read_auth();

print "Set-Cookie: $_\n" for @set_cookie;
print "Content-Type: text/html; charset=utf-8\n";
print "Cache-Control: no-store\n\n";
page_top();

if (!defined $auth || defined $login_error)
{
    show_login();
    page_bottom();
    exit;
}

my $http = HTTP::Tiny->new(
    agent   => 'boardspace-geeklist-sorter',
    timeout => 30,
    default_headers => { 'Authorization' => "GeekAuth $auth", 'Accept' => 'application/json' },
);

if ($action eq 'preview')       { do_preview(); }
elsif ($action eq 'apply')      { do_apply(); }
else                            { show_form(); }

page_bottom();
exit;

# ---------------------------------------------------------------------------

# sign in to BGG; returns (authToken) or (undef, message)
sub bgg_login
{
    my ($user, $pass) = @_;
    return (undef, 'Enter your BGG username and password.') unless length $user && length $pass;
    utf8::decode($user);
    utf8::decode($pass);
    my $ua = HTTP::Tiny->new(agent => 'boardspace-geeklist-sorter', timeout => 30);
    my $r = $ua->request('POST', "$BGG/login/api/v1",
        {
            headers => { 'Content-Type' => 'application/json', 'Accept' => 'application/json' },
            content => $json->encode({ credentials => { username => $user, password => $pass } }),
        });
    if (!$r->{success})
    {
        my $msg = eval { $json->decode($r->{content})->{errors}{message} };
        return (undef, $msg ? "BGG: $msg" : "BGG login failed ($r->{status} $r->{reason})");
    }
    my $sc = $r->{headers}{'set-cookie'} // [];
    my @jar = map { (split /;/, $_, 2)[0] } (ref $sc ? @$sc : ($sc));
    return (undef, 'BGG accepted the login but sent no session cookie.') unless @jar;

    my $a = $ua->get("$BGG/api/accounts/current",
        { headers => { 'Cookie' => join('; ', @jar), 'Accept' => 'application/json' } });
    my $tok = $a->{success} ? eval { $json->decode($a->{content})->{authToken} } : undef;
    return (undef, "Logged in, but couldn't read the authToken ($a->{status}).") unless clean_token($tok);
    return clean_token($tok);
}

sub cookie_header
{
    my ($value, $expire) = @_;
    my $path = $ENV{SCRIPT_NAME} || '/';
    my $c = "$COOKIE=$value; Path=$path; HttpOnly; SameSite=Strict";
    $c .= '; Secure' if ($ENV{HTTPS} || '') =~ /^on$/i;
    $c .= $expire ? '; Max-Age=0' : '; Max-Age=2592000';   # 30 days
    return $c;
}

sub read_cookies
{
    my %c;
    for my $pair (split /;\s*/, $ENV{HTTP_COOKIE} // '')
    {
        my ($k, $v) = split /=/, $pair, 2;
        $c{$k} = $v if defined $v;
    }
    return \%c;
}

sub clean_token
{
    my ($t) = @_;
    return (defined $t && $t =~ /^\s*([A-Za-z0-9]+)\s*$/) ? $1 : undef;
}

# carry a token that came in as a parameter through forms and links
sub token_field
{
    return defined $param_token ? qq{<input type=hidden name=token value="$param_token">\n} : '';
}

sub token_query
{
    return defined $param_token ? "&amp;token=$param_token" : '';
}

sub show_login
{
    print "<p class=err>", h($login_error), "</p>\n" if defined $login_error;
    print qq{<h2>Sign in to BoardGameGeek</h2>
<form method=post>
<input type=hidden name=action value=login>
<p>Username <input name=username autocomplete=username></p>
<p>Password <input type=password name=password autocomplete=current-password></p>
<button>Sign in</button>
</form>
<p>Your password goes to BGG once and isn't saved; the script keeps only BGG's auth token,
in a cookie for this page.</p>};
}

sub show_form
{
    my $logout = (defined $cookie_token && !defined $param_token)
        ? qq{<p><a href="?action=logout">forget login</a></p>} : '';
    print qq{<form method=get>
<input type=hidden name=action value=preview>
${\ token_field()}GeekList id <input name=listid value="$listid" size=8>
<button>Preview</button>
</form>
<p>Preview reads every item's thumbs (a few hundred requests, about a minute for a big list)
and shows the new order. Nothing changes on BGG until you press Apply on the preview page.</p>
$logout};
}

sub do_preview
{
    my ($name, $items, $creator) = read_list($listid);
    return unless $items;
    my $n = @$items;
    print "<h2>", h($name), " <small>(list $listid, $n items)</small></h2>\n";
    my ($owned, $why) = ownership($creator);
    print "<p>$why</p>\n";
    print "<p id=prog>Reading thumbs: ";
    for my $i (0 .. $#$items)
    {
        my $r = get_json("$API/listitem/$items->[$i]{id}/reaction?totalonly=1");
        if (!$r) { print "</p>"; return; }
        $items->[$i]{thumbs} = $r->{thumbs} || 0;
        print(($i + 1) % 25 == 0 ? ($i + 1) . ' ' : '');
        sleep $DELAY;
    }
    print "done.</p>\n";

    my @sorted = sort { $b->{thumbs} <=> $a->{thumbs} || $a->{pos} <=> $b->{pos} } @$items;
    my $moved = grep { $sorted[$_]{pos} != $_ } 0 .. $#sorted;

    print "<p><b>$moved</b> of $n items change position.</p>\n";
    if ($moved && !$owned)
    {
        print "<p class=ro>Read-only preview: only the list's owner can save a new order.</p>\n";
    }
    elsif ($moved)
    {
        my $ids = join(',', map { $_->{id} } @sorted);
        print qq{<form method=post>
<input type=hidden name=action value=apply>
<input type=hidden name=listid value="$listid">
<input type=hidden name=order value="$ids">
${\ token_field()}<button class=apply>Apply this order</button> &nbsp; <a href="?listid=$listid${\ token_query()}">cancel</a>
</form>\n};
    }
    else
    {
        print "<p>Already in thumbs order; nothing to do.</p>\n";
    }

    print "<table><tr><th>New</th><th>Thumbs</th><th>Was</th><th>Change</th><th>Game</th></tr>\n";
    for my $i (0 .. $#sorted)
    {
        my $e = $sorted[$i];
        my $d = $e->{pos} - $i;
        my $chg = $d > 0 ? "<span class=up>&#9650;$d</span>"
                : $d < 0 ? "<span class=dn>&#9660;" . (-$d) . "</span>" : '';
        printf "<tr%s><td>%d</td><td>%d</td><td>%d</td><td>%s</td><td><a href=\"https://boardgamegeek.com%s\">%s</a></td></tr>\n",
            ($d ? ' class=mv' : ''), $i + 1, $e->{thumbs}, $e->{pos} + 1, $chg, h($e->{href}), h($e->{name});
    }
    print "</table>\n";
}

sub do_apply
{
    if (!$is_post)
    {
        print "<p class=err>Apply must be posted from the preview page.</p>";
        return;
    }
    my @order = grep { /^\d+$/ } split /,/, ($q{order} || '');

    # make sure the list still holds exactly the previewed items
    my ($name, $items, $creator) = read_list($listid);
    return unless $items;
    my ($owned, $why) = ownership($creator);
    if (!$owned)
    {
        print "<p class=err>$why. Nothing was saved.</p>";
        return;
    }
    my %now  = map { $_->{id} => 1 } @$items;
    my %seen;
    my @dups = grep { $seen{$_}++ } @order;
    my @gone = grep { !$now{$_} } @order;
    my @new  = grep { !$seen{$_->{id}} } @$items;
    if (@dups || @gone || @new || @order != @$items)
    {
        print "<p class=err>The list changed since the preview (",
            scalar(@new), " new, ", scalar(@gone), " removed). Nothing was saved. ",
            "<a href=\"?action=preview&amp;listid=$listid", token_query(), "\">Preview again</a>.</p>";
        return;
    }

    my $body = $json->encode({ data => [ map { { id => "$_" } } @order ] });
    my $r = $http->request('PATCH', "$API/geeklist/$listid/listitem",
        { headers => { 'Content-Type' => 'application/json' }, content => $body });
    if ($r->{success})
    {
        print "<p class=ok>Saved the new order of <b>", h($name), "</b> ($r->{status}). ",
            "<a href=\"https://boardgamegeek.com/geeklist/$listid\">View the list</a></p>";
    }
    else
    {
        print "<p class=err>BGG refused the update: $r->{status} ", h($r->{reason}), "<br>",
            h(substr($r->{content} // '', 0, 500)), "</p>";
    }
}

# who owns the list, relative to the signed-in user: (owned, html description)
sub ownership
{
    my ($creator) = @_;
    my $me = current_user();
    my $owner = defined $creator ? user_label($creator) : 'an unknown user';
    if (!$me)
    {
        return (0, "GeekList owned by $owner; couldn't identify the signed-in user, "
            . "so the token may have expired");
    }
    my $owned = defined($creator) && $creator == $me->{user};
    return (1, "GeekList owned by $owner (you)") if $owned;
    return (0, "GeekList owned by $owner; you are signed in as <b>" . h($me->{username}) . "</b>");
}

my $current_user;
sub current_user
{
    $current_user //= get_json("$API/accounts/current", 1) // 0;
    return ($current_user && defined $current_user->{user}) ? $current_user : undef;
}

# "username (First Last)" linked to the BGG profile; falls back to the number
sub user_label
{
    my ($id) = @_;
    my $u = $id =~ /^\d+$/ ? get_json("$API/user/$id", 1) : undef;
    return "user #" . h($id) unless $u && defined $u->{username};
    my $real = join(' ', grep { defined && length } $u->{firstname}, $u->{lastname});
    return sprintf '<a href="%s/profile/%s"><b>%s</b></a>%s', $BGG, h($u->{username}),
        h($u->{username}), length $real ? ' (' . h($real) . ')' : '';
}

# returns (listname, [ {id,name,href,pos}, ... ], creator id) or () after printing an error
sub read_list
{
    my ($id) = @_;
    my $meta = get_json("$API/geeklists/$id") or return;
    my @items;
    my $total;
    for (my $page = 1; !defined($total) || @items < $total; $page++)
    {
        my $j = get_json("$API/listitems?listid=$id&page=$page") or return;
        $total //= $j->{pagination}{total};
        my $data = $j->{data} || [];
        last unless @$data;
        for my $d (@$data)
        {
            push @items, { id => $d->{id}, name => $d->{item}{name} // '?',
                           href => $d->{item}{href} // '', pos => scalar @items };
        }
        sleep $DELAY;
    }
    my %u = map { $_->{id} => 1 } @items;
    if (!defined($total) || @items != $total || keys(%u) != $total)
    {
        print "<p class=err>Read ", scalar(@items), " items but the list reports ",
            ($total // '?'), "; stopping without changing anything.</p>";
        return;
    }
    return ($meta->{name} // "list $id", \@items, $meta->{creator});
}

sub get_json
{
    my ($url, $quiet) = @_;
    for my $attempt (1 .. 4)
    {
        my $r = $http->get($url);
        if ($r->{success})
        {
            my $v = eval { $json->decode($r->{content}) };
            return $v if $v;
            last;
        }
        last if $attempt == 4 || ($r->{status} != 429 && $r->{status} < 500);
        sleep 5 * $attempt;
    }
    print "<p class=err>Request failed: ", h($url), "</p>" unless $quiet;
    return;
}

sub read_auth
{
    open(my $fh, '<', $AUTH_FILE) or return;
    my $t = <$fh>;
    close $fh;
    return unless defined $t;
    $t =~ s/\s+//g;
    return length($t) ? $t : undef;
}

sub read_params
{
    my $s = $ENV{QUERY_STRING} // '';
    if (($ENV{REQUEST_METHOD} || '') eq 'POST' && ($ENV{CONTENT_LENGTH} || 0) > 0)
    {
        my $len = $ENV{CONTENT_LENGTH};
        $len = 1_000_000 if $len > 1_000_000;
        read(STDIN, my $body, $len);
        $s .= '&' . $body;
    }
    my %p;
    for my $pair (split /[&;]/, $s)
    {
        my ($k, $v) = split /=/, $pair, 2;
        next unless defined $k && length $k;
        for ($k, $v) { next unless defined; tr/+/ /; s/%([0-9A-Fa-f]{2})/chr hex $1/ge; }
        $p{$k} = $v // '';
    }
    return %p;
}

sub h
{
    my ($s) = @_;
    $s //= '';
    $s =~ s/&/&amp;/g; $s =~ s/</&lt;/g; $s =~ s/>/&gt;/g; $s =~ s/"/&quot;/g;
    return $s;
}

sub page_top
{
    print <<'HTML';
<!DOCTYPE html><html><head><meta charset="utf-8"><script src="/js/site.js"></script>
<title>GeekList thumbs sort</title>
<meta name="referrer" content="no-referrer">
<style>
main{font-family:sans-serif;color:#222;max-width:60em;margin:10px auto 30px;padding:14px 22px;background:rgba(255,255,255,.92);border-radius:8px}
main h1{margin-top:.2em}
table{border-collapse:collapse;margin-top:1em}
td,th{padding:2px 8px;border-bottom:1px solid #ddd;text-align:right}
td:last-child,th:last-child{text-align:left}
tr.mv{background:#fffbe6}
.up{color:#080}.dn{color:#b00}
.err{color:#b00;font-weight:bold}.ro{color:#666;font-style:italic}.ok{color:#080;font-weight:bold}
button.apply{font-size:1.1em;padding:4px 14px}
</style></head>
<body style="background-image: url(/images/background-image.jpg);">
<center>
<table style="text-align: left; width: 100%;" border="1" cellpadding="2" cellspacing="2">
<tbody>
<tr>
<td><h1 style="text-align: center;"><a href="/"><img alt="boardspace.net" src="/images/master-logo.jpg" style="width: 600px; height: 100px; border: 0;"></a></h1></td>
<td>
<div id="google_translate_element"></div>
<script type="text/javascript">
function googleTranslateElementInit() {
new google.translate.TranslateElement({pageLanguage: 'en', layout: google.translate.TranslateElement.InlineLayout.SIMPLE}, 'google_translate_element');
}
</script>
<script type="text/javascript" src="//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit">
</script>
</td>
</tr>
</tbody>
</table>
<h1 style="text-align: center;">A Place for Board Games <a href="https://www.facebook.com/pages/Boardspacenet/147924481942900" target="_new"><img src="/images/fb-like.jpg" alt="Like our facebook page" title="Like our facebook page" height="25" width="50"></a>
</h1>
</center>
<main><h1>GeekList thumbs sort</h1>
HTML
}

sub page_bottom
{
    print "</main></body></html>\n";
}
