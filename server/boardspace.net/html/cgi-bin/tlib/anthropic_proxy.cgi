#!/usr/bin/env perl
use strict;
use warnings;

# anthropic_proxy.cgi — Anthropic API proxy for Apache CGI / mod_cgi / mod_cgid
#
# Apache vhost config (place the script in your cgi-bin, or use ScriptAlias):
#
#   ScriptAlias /anthropic/ /var/www/cgi-bin/anthropic_proxy.cgi/
#
#   <Directory /var/www/cgi-bin>
#       Options +ExecCGI
#       AddHandler cgi-script .cgi
#   </Directory>
#
# Then browser scripts hit:
#   POST https://yoursite.com/anthropic/v1/messages
#
# API key resolution order (first non-empty wins):
#   1. x-api-key request header
#   2. Authorization: Bearer <key> request header
#   3. ANTHROPIC_API_KEY environment variable (set in Apache config via SetEnv)
#
# Apache SetEnv example (httpd.conf or vhost):
#   SetEnv ANTHROPIC_API_KEY sk-ant-...
#   SetEnv ALLOWED_ORIGINS   https://yoursite.com,https://app.yoursite.com

use CGI qw();
use LWP::UserAgent;
use HTTP::Request;

# ── Configuration ─────────────────────────────────────────────────────────────

my $ANTHROPIC_BASE    = 'https://api.anthropic.com';
my $ANTHROPIC_VERSION = '2023-06-01';

my @ALLOWED_ORIGINS = split /\s*,\s*/,
    ($ENV{ALLOWED_ORIGINS} || 'http://localhost:3000,http://localhost:5173');

# Headers we never forward in either direction
my %HOP_BY_HOP = map { lc($_) => 1 } qw(
    host connection keep-alive proxy-authenticate proxy-authorization
    te trailers transfer-encoding upgrade
);

# ── Helpers ───────────────────────────────────────────────────────────────────

sub cors_headers {
    my ($origin) = @_;
    my $allowed = (grep { $_ eq '*' || $_ eq $origin } @ALLOWED_ORIGINS)
                  ? ($origin || '*')
                  : $ALLOWED_ORIGINS[0];
    return (
        'Access-Control-Allow-Origin'      => $allowed,
        'Access-Control-Allow-Methods'     => 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers'     =>
            'Content-Type, x-api-key, Authorization, anthropic-beta, anthropic-dangerous-direct-browser-access',
        'Access-Control-Allow-Credentials' => 'true',
        'Vary'                             => 'Origin',
    );
}

sub emit {
    # Print a raw HTTP response to stdout (CGI output)
    my (%args) = @_;
    my $status  = $args{status}  // '200 OK';
    my $ctype   = $args{ctype}   // 'application/json';
    my $body    = $args{body}    // '';
    my %extra   = %{ $args{headers} // {} };

    print "Status: $status\r\n";
    print "Content-Type: $ctype\r\n";
    for my $k (sort keys %extra) {
        print "$k: $extra{$k}\r\n";
    }
    print "\r\n";
    print $body;
}

# ── CGI environment ───────────────────────────────────────────────────────────

my $method  = $ENV{REQUEST_METHOD} // 'GET';
my $origin  = $ENV{HTTP_ORIGIN}    // '';

my %cors = cors_headers($origin);

# ── CORS preflight ────────────────────────────────────────────────────────────

if ($method eq 'OPTIONS') {
    emit(status => '204 No Content', body => '', headers => \%cors);
    exit 0;
}

# ── Reconstruct the upstream path from PATH_INFO ──────────────────────────────
# With ScriptAlias /anthropic/ .../anthropic_proxy.cgi/
# a request to /anthropic/v1/messages arrives with PATH_INFO = /v1/messages

my $path_info = "/v1/messages";

my $upstream_url = $ANTHROPIC_BASE . $path_info;
if (my $qs = $ENV{QUERY_STRING}) {
    $upstream_url .= "?$qs";
}

# ── Resolve API key ───────────────────────────────────────────────────────────

my $api_key = $ENV{HTTP_X_API_KEY} // '';

unless ($api_key) {
    my $auth = $ENV{HTTP_AUTHORIZATION} // '';
    ($api_key = $auth) =~ s/^Bearer\s+//i;
}

unless ($api_key) {
    emit(
        status  => '401 Unauthorized',
        body    => '{"error":"No API key — send x-api-key header or set ANTHROPIC_API_KEY on the server"}',
        headers => \%cors,
    );
    exit 0;
}

# ── Read request body ─────────────────────────────────────────────────────────

my $body = '';
if ($method eq 'POST' || $method eq 'PUT' || $method eq 'PATCH') {
    my $len = $ENV{CONTENT_LENGTH} // 0;
    read(STDIN, $body, $len) if $len > 0;
}

# ── Build upstream request ────────────────────────────────────────────────────

my $upstream_req = HTTP::Request->new($method, $upstream_url);

# Forward safe request headers (CGI exposes them as HTTP_* env vars)
for my $key (sort keys %ENV) {
    next unless $key =~ /^HTTP_(.+)$/;
    my $name = lc($1);
    $name =~ s/_/-/g;
    next if $HOP_BY_HOP{$name};
    next if $name eq 'x-api-key';      # will be set from resolved key below
    next if $name eq 'authorization';
    next if $name eq 'origin';         # not useful upstream
    $upstream_req->header($name => $ENV{$key});
}

# Forward Content-Type (CGI keeps it in CONTENT_TYPE, not HTTP_CONTENT_TYPE)
my $ctype = $ENV{CONTENT_TYPE} // 'application/json';
$upstream_req->header('content-type' => $ctype);

# Inject auth
$upstream_req->header('x-api-key'        => $api_key);
$upstream_req->header('anthropic-version' => $ANTHROPIC_VERSION);

$upstream_req->content($body) if length $body;

# ── Forward to Anthropic ──────────────────────────────────────────────────────

my $ua = LWP::UserAgent->new(agent => 'AnthropicCGIProxy/1.0', timeout => 120);
my $resp = $ua->request($upstream_req);

# ── Stream response back through CGI stdout ───────────────────────────────────

my $status_line = $resp->code . ' ' . $resp->message;

print "Status: $status_line\r\n";

# Forward upstream response headers (skip hop-by-hop)
$resp->headers->scan(sub {
    my ($name, $value) = @_;
    return if $HOP_BY_HOP{lc $name};
    print "$name: $value\r\n";
});

# CORS headers
for my $k (sort keys %cors) {
    print "$k: $cors{$k}\r\n";
}

print "\r\n";

# Body — binmode so binary/SSE content is not mangled
binmode STDOUT;
print $resp->content;
