#!/usr/bin/perl
#
# geeklist_check.cgi -- "is game X on geeklist Y?"
#
# Parameters (GET or POST):
#   game   a BGG game id, a boardgamegeek.com/boardgame/... URL, or a name
#   list   a geeklist id or a boardgamegeek.com/geeklist/... URL
#          (default 12240, "Proposed BoardSpace.net games")
#   format "text" for a one-line plain-text answer (yes/no/error), for scripts
#
# BoardGameGeek's XML API requires an application token (since fall 2025).
# Register at https://boardgamegeek.com/applications, then either
#   SetEnv BGG_TOKEN <token>            in the Apache vhost, or
#   $::bgg_token = "<token>";           in include.pl
#
# Geeklists are fetched from /xmlapi/geeklist/<id> and cached for
# $CACHE_SECONDS in the temp directory, so repeated queries against the same
# list cost one BGG request.  For testing without the network, set
# GEEKLIST_XML_FILE to a saved copy of a geeklist's XML.
#
use strict;
use warnings;
use CGI::Carp qw( fatalsToBrowser );
use CGI qw(:standard -utf8);
use LWP::UserAgent;
use File::Spec;
use Encode qw(decode);

binmode(STDOUT, ':encoding(UTF-8)');

use FindBin;
# optional: may define $::bgg_token.  This script lives in cgi-bin/bgg/,
# include.pl in cgi-bin/.
eval { require "$FindBin::Bin/../include.pl"; };

my $DEFAULT_LIST = 12240;
my @PRESETS = ([12240, 'Proposed BoardSpace.net games'],
               [21458, 'Current Boardspace.net Games']);
my $CACHE_SECONDS = 600;
my $BGG = 'https://boardgamegeek.com';

sub esc
{
    my ($s) = @_;
    $s = '' unless defined $s;
    $s =~ s/&/&amp;/g; $s =~ s/</&lt;/g; $s =~ s/>/&gt;/g; $s =~ s/"/&quot;/g;
    return $s;
}

# XML attribute and text values arrive entity-encoded
sub unxml
{
    my ($s) = @_;
    $s = '' unless defined $s;
    $s =~ s/&#(\d+);/chr($1)/ge;
    $s =~ s/&#x([0-9a-fA-F]+);/chr(hex($1))/ge;
    $s =~ s/&quot;/"/g; $s =~ s/&apos;/'/g; $s =~ s/&lt;/</g; $s =~ s/&gt;/>/g; $s =~ s/&amp;/&/g;
    return $s;
}

sub list_id
{
    my ($s) = @_;
    $s = '' unless defined $s;
    return $1 if $s =~ m{geeklist/(\d+)};
    return $1 if $s =~ m{^\s*(\d+)\s*$};
    return undef;
}

# returns (xml, error)
sub fetch_geeklist
{
    my ($id) = @_;
    if ($ENV{GEEKLIST_XML_FILE})
    {
        open(my $fh, '<:raw', $ENV{GEEKLIST_XML_FILE}) or return (undef, "can't read $ENV{GEEKLIST_XML_FILE}");
        local $/; my $x = <$fh>; close $fh;
        return ($x, undef);
    }
    my $cache = File::Spec->catfile(File::Spec->tmpdir(), "bgg_geeklist_$id.xml");
    if (-f $cache && (time() - (stat($cache))[9]) < $CACHE_SECONDS)
    {
        if (open(my $fh, '<:raw', $cache)) { local $/; my $x = <$fh>; close $fh; return ($x, undef); }
    }
    my $token = $ENV{BGG_TOKEN} || do { no warnings 'once'; $::bgg_token };
    return (undef, "no BGG API token configured (set BGG_TOKEN or \$::bgg_token)") unless $token;

    my $ua = LWP::UserAgent->new(timeout => 30, agent => 'boardspace.net geeklist_check/1.0');
    my $url = "$BGG/xmlapi/geeklist/$id";
    for my $attempt (1 .. 6)
    {
        my $res = $ua->get($url, 'Authorization' => "Bearer $token");
        if ($res->code == 202)                 # BGG queued the request; try again shortly
        {
            sleep(2);
            next;
        }
        if ($res->code == 429)                 # rate limited
        {
            sleep(5);
            next;
        }
        return (undef, "BGG answered " . $res->status_line) unless $res->is_success;
        my $x = $res->decoded_content(charset => 'none');
        return (undef, "geeklist $id not found") if $x =~ /<error\b/i && $x !~ /<item\b/;
        if (open(my $fh, '>:raw', "$cache.$$"))
        {
            print $fh $x; close $fh;
            rename("$cache.$$", $cache);
        }
        return ($x, undef);
    }
    return (undef, "BGG is still preparing geeklist $id; try again in a minute");
}

# returns (title, [items]) where each item is a hashref of the item's attributes
sub parse_geeklist
{
    my ($xml) = @_;
    my $title = ($xml =~ m{<title>(.*?)</title>}s) ? unxml($1) : '';
    my @items;
    while ($xml =~ m{<item\s+([^>]*?)/?>}gs)
    {
        my $attrs = $1;
        my %a;
        while ($attrs =~ m{(\w+)\s*=\s*"([^"]*)"}g) { $a{$1} = unxml($2); }
        push @items, \%a if defined $a{objectid};
    }
    return ($title, \@items);
}

# returns a list of matching items, best matches first
sub find_game
{
    my ($items, $game) = @_;
    my $q = $game;
    $q =~ s/^\s+|\s+$//g;
    if ($q =~ m{boardgame(?:expansion)?/(\d+)} || $q =~ m{^(\d+)$})
    {
        my $id = $1;
        return grep { $_->{objectid} eq $id } @$items;
    }
    my $lq = lc($q);
    my @exact = grep { lc($_->{objectname}) eq $lq } @$items;
    return @exact if @exact;
    return grep { index(lc($_->{objectname}), $lq) >= 0 } @$items;
}

sub item_link
{
    my ($list, $it) = @_;
    return "$BGG/geeklist/$list?itemid=$it->{id}#$it->{id}";
}

sub game_link
{
    my ($it) = @_;
    my $kind = $it->{subtype} || 'boardgame';
    return "$BGG/$kind/$it->{objectid}";
}

sub page
{
    my ($game, $listarg, $body) = @_;
    my $opts = join('', map
    {
        my ($id, $name) = @$_;
        "<option value=\"$id\">" . esc($name) . " ($id)</option>"
    } @PRESETS);
    my $g = esc($game);
    my $l = esc($listarg);
    print header(-type => 'text/html', -charset => 'utf-8');
    print <<HTML;
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<script src="/js/site.js"></script>
<title>Is it on the geeklist?</title>
<style>
main{font:16px/1.5 system-ui,-apple-system,"Segoe UI",sans-serif;color:#222;max-width:720px;margin:10px auto 30px;padding:22px 18px;background:rgba(255,255,255,.88);border-radius:8px}
main h1{font-size:28px;line-height:1.15;margin:0 0 18px}
form{display:grid;gap:12px;background:#fff;border:1px solid #ddd6c8;border-radius:8px;padding:18px}
label{font-weight:600;font-size:14px}
input,select,button{font:inherit;padding:9px 10px;border:1px solid #bbb;border-radius:6px;width:100%;box-sizing:border-box}
.row{display:grid;gap:6px}
button{background:#2d5d8a;color:#fff;border:0;font-weight:600;cursor:pointer;min-height:44px}
button:focus,input:focus,select:focus{outline:3px solid #9cc3e6;outline-offset:1px}
.ans{margin-top:22px;padding:16px 18px;border-radius:8px;background:#fff;border-left:6px solid #999}
.yes{border-color:#2e8540}.no{border-color:#b23a2e}.err{border-color:#c98a00}
.ans h2{margin:0 0 8px;font-size:22px}
.ans ul{margin:8px 0 0;padding-left:20px}
small{color:#666}
a{color:#1f4f7a}
</style>
</head>
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
<main>
<h1>Is game X on geeklist Y?</h1>
<form method="get" action="">
 <div class="row"><label for="game">Game: name, BGG id, or BGG link</label>
  <input id="game" name="game" value="$g" placeholder="game name or board game numer or https://boardgamegeek.com/boardgame/nnn" required></div>
 <div class="row"><label for="list">Geeklist: id or BGG link</label>
  <input id="list" name="list" value="$l" list="presets" placeholder="$DEFAULT_LIST"></div>
 <datalist id="presets">$opts</datalist>
 <button type="submit">Check</button>
</form>
$body
<p><small>Data from <a href="https://boardgamegeek.com">BoardGameGeek</a>. Lists are cached for @{[ int($CACHE_SECONDS/60) ]} minutes.</small></p>
<p><a href="https://boardgamegeek.com"><img src="/images/powered_by_logo_01_SM.jpg"></img></a>
</main>
</body>
</html>
HTML
}

sub main
{
    my $game = param('game');
    my $listarg = param('list');
    $listarg = $DEFAULT_LIST unless defined $listarg && $listarg =~ /\S/;
    my $text = (param('format') || '') eq 'text';

    unless (defined $game && $game =~ /\S/)
    {
        page('', $listarg, '');
        return;
    }
    my $list = list_id($listarg);
    my ($verdict, $body);
    if (!defined $list)
    {
        ($verdict, $body) = ('error: bad geeklist id', "<div class=\"ans err\"><h2>Which geeklist?</h2>"
            . "Give a geeklist number or a boardgamegeek.com/geeklist/... link.</div>");
    }
    else
    {
        my ($xml, $err) = fetch_geeklist($list);
        if ($err)
        {
            ($verdict, $body) = ("error: $err", "<div class=\"ans err\"><h2>Couldn't read the geeklist</h2>" . esc($err) . "</div>");
        }
        else
        {
            my ($title, $items) = parse_geeklist(decode('UTF-8', $xml));
            my $lt = "<a href=\"$BGG/geeklist/$list\">" . esc($title || "geeklist $list") . "</a>";
            my @hits = find_game($items, $game);
            if (@hits)
            {
                my $lis = join('', map
                {
                    my $it = $_;
                    "<li><a href=\"" . esc(game_link($it)) . "\">" . esc($it->{objectname}) . "</a>"
                    . " &mdash; <a href=\"" . esc(item_link($list, $it)) . "\">list entry</a>"
                    . " by " . esc($it->{username})
                    . (defined $it->{postdate} ? ", " . esc($it->{postdate}) : '')
                    . (defined $it->{thumbs} ? ", $it->{thumbs} thumbs" : '')
                    . "</li>"
                } @hits);
                my $what = @hits == 1 ? 'is on' : scalar(@hits) . ' matching games are on';
                ($verdict, $body) = ('yes ' . join(', ', map { $_->{objectname} } @hits),
                    "<div class=\"ans yes\"><h2>Yes</h2>" . esc($game) . " $what $lt.<ul>$lis</ul></div>");
            }
            else
            {
                ($verdict, $body) = ('no', "<div class=\"ans no\"><h2>No</h2>"
                    . esc($game) . " is not among the " . scalar(@$items) . " games on $lt.</div>");
            }
        }
    }
    if ($text)
    {
        print header(-type => 'text/plain', -charset => 'utf-8');
        print "$verdict\n";
        return;
    }
    page($game, $listarg, $body);
}

main();
