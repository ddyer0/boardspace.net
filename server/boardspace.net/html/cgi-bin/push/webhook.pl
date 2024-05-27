package WebService::Discord::Webhook;

# from https://metacpan.org/dist/WebService-Discord-Webhook/source/lib/WebService/Discord/Webhook.pm

use strict;
use warnings;
 
# Module for interacting with the REST service
use HTTP::Tiny;
 
# JSON decode
use JSON;
 
# Base64 encode for avatar images
use MIME::Base64 qw(encode_base64);
 
# Parse filename from filepath
use File::Spec;
 
# better error messages
# use Carp qw(croak);
 
# PACKAGE VARS
our $VERSION = '1.10';
 
# Base URL for all API requests
our $BASE_URL = 'https://discord.com/api';
 
sub log_error()
{
	my ($msg) = @_;
	&::log_error($msg);
}
##################################################
 
# Create a new Webhook object.
#  Pass a hash containing parameters
#  Requires:
#   url, or
#   token and id
#  Optional:
#   wait
#   timeout
#   verify_SSL
#  A single scalar is treated as a URL
sub new {
  my $class = shift;
 
  my %params;
  if ( @_ > 1 ) {
    %params = @_;
  } else {
    $params{url} = shift;
  }
 
  # check parameters
  my ( $id, $token );
  if ( $params{url} ) {
    if ( $params{url} =~ m{discord(?:app)?\.com/api/webhooks/(\d+)/([^/?]+)}i ) {
      $id    = $1;
      $token = $2;
    } else {
      &log_error("PARAMETER ERROR: Failed to parse ID and Token from URL");
    }
  } elsif ( $params{id} && $params{token} ) {
    if ( $params{id} =~ m/^\d+$/ && $params{token} =~ m{^[^/?]+$} ) {
      $id    = $params{id};
      $token = $params{token};
    } else {
      &log_error("PARAMETER ERROR: Failed to validate ID and Token");
    }
  } else {
    &log_error("PARAMETER ERROR: Must provide either URL, or ID and Token");
  }
 
  # Create an LWP UserAgent for REST requests
  my %attributes = (
    agent => "p5-WebService-Discord-Webhook (https://github.com/greg-kennedy/p5-WebService-Discord-Webhook, $VERSION)"
  );
  if ( $params{timeout} )    { $attributes{timeout}    = $params{timeout} }
  if ( $params{verify_SSL} ) { $attributes{verify_SSL} = $params{verify_SSL} }
 
  my $http = HTTP::Tiny->new(%attributes);
 
  # create class with some params
  my $self = bless { id => $id, token => $token, http => $http }, $class;
  if ( $params{wait} ) { $self->{wait} = 1 }
 
  # call get to populate additional details
  #$self->get();
 
  return $self;
}
 
# updates internal structures after a webhook request
sub _parse_response {
  my $self = shift;
  my $json = shift;
 
  my $response = decode_json($json);
 
  # sanity
  if ( $self->{id} ne $response->{id} ) {
    &log_error("SERVICE ERROR: get() returned ID='"
      . $response->{id}
      . "', expected ID='"
      . $self->{id} . "'");
  }
  if ( $self->{token} ne $response->{token} ) {
    &log_error("SERVICE ERROR: get() returned Token='"
      . $response->{token}
      . "', expected Token='"
      . $self->{token} . "'");
  }
 
  # store / update details
  if ( $response->{guild_id} ) {
    $self->{guild_id} = $response->{guild_id};
  } else {
    delete $self->{guild_id};
  }
  $self->{channel_id} = $response->{channel_id};
  $self->{name}       = $response->{name};
  $self->{avatar}     = $response->{avatar};
 
  return $response;
}
 
# GET request
#  Retrieves some info about the webhook setup
#  No parameters
sub get {
  my $self = shift;
 
  my $url = $BASE_URL . '/webhooks/' . $self->{id} . '/' . $self->{token};
 
  my $response = $self->{http}->get($url);
  if ( !$response->{success} ) {
 
    # non-200 code returned
    &log_error("HTTP ERROR: HTTP::Tiny->get($url) returned error\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason} . "\n"
      . "\tcontent: " . $response->{content});
  } elsif ( !$response->{content} ) {
 
    # empty result
    &log_error("HTTP ERROR: HTTP::Tiny->get($url) returned empty response\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason});
  }
 
  # update internal structs and return
  return $self->_parse_response( $response->{content} );
}
 
# PATCH request
#  Allows webhook to alter its Name or Avatar
sub modify {
  my $self = shift;
 
  my %params;
  if ( @_ > 1 ) {
    %params = @_;
  } else {
    $params{name} = shift;
  }
 
  # check params
  if ( !( $params{name} || exists $params{avatar} ) ) {
    &log_error("PARAMETER ERROR: Modify request with no valid parameters");
  }
 
  my %request;
 
  # retrieve the two allowed params and place in request if needed
  if ( $params{name} ) { $request{name} = $params{name} }
 
  if ( exists $params{avatar} ) {
    if ( $params{avatar} ) {
 
      # try to infer type from data string
      my $type;
      if (
        substr( $params{avatar}, 0, 8 ) eq "\x89\x50\x4e\x47\x0d\x0a\x1a\x0a" )
      {
        $type = 'image/png';
      } elsif ( substr( $params{avatar}, 0, 2 ) eq "\xff\xd8"
        && substr( $params{avatar}, -2 ) eq "\xff\xd9" )
      {
        $type = 'image/jpeg';
      } elsif ( substr( $params{avatar}, 0, 4 ) eq 'GIF8' ) {
        $type = 'image/gif';
      } else {
        &log_error("PARAMETER ERROR: Could not determine image type from data (not a valid png, jpeg or gif image)");
      }
 
      $request{avatar} =
        'data:' . $type . ';base64,' . encode_base64( $params{avatar} );
    } else {
      $request{avatar} = undef;
    }
  }
 
  my $url = $BASE_URL . '/webhooks/' . $self->{id} . '/' . $self->{token};
 
  # PATCH method not yet built-in as of 0.076
  #my $response = $self->{http}->patch($url, \%request);
  my $response = $self->{http}->request(
    'PATCH', $url,
    {
      headers => { 'Content-Type' => 'application/json' },
      content => encode_json( \%request )
    }
  );
  if ( !$response->{success} ) {
 
    # non-200 code returned
    &log_error("HTTP ERROR: HTTP::Tiny->patch($url) returned error\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason} . "\n"
      . "\tcontent: " . $response->{content});
  } elsif ( !$response->{content} ) {
 
    # empty result
    &log_error("HTTP ERROR: HTTP::Tiny->patch($url) returned empty response\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason});
  }
 
  # update internal structs and return
  return $self->_parse_response( $response->{content} );
}
 
# DELETE request - deletes the webhook
sub destroy {
  my $self = shift;
 
  my $url = $BASE_URL . '/webhooks/' . $self->{id} . '/' . $self->{token};
 
  my $response = $self->{http}->delete($url);
  if ( !$response->{success} ) {
    &log_error("HTTP ERROR: HTTP::Tiny->delete($url) returned error\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason} . "\n"
      . "\tcontent: " . $response->{content});
  }
 
  # DELETE response is 204 NO CONTENT, so there is no return code.
}
 
# EXECUTE - posts the message.
# Required parameters: one of
#  content
#  files
#  embeds
# Optional paremeters:
#  username
#  avatar_url
#  tts
#  allowed_mentions
sub execute {
  my $self = shift;
 
  # extract params
  my %params;
  if ( @_ > 1 ) {
    %params = @_;
  } else {
    $params{content} = shift;
  }
 
  # convenience params
  if ( $params{file} )  { $params{files}  = [ delete $params{file} ] }
  if ( $params{embed} ) { $params{embeds} = [ delete $params{embed} ] }
 
  # test required fields
  if ( !( $params{content} || $params{files} || $params{embeds} ) ) {
    &log_error("PARAMETER ERROR: Execute request missing required parameters (must have at least content, embed, or file)");
  } elsif ( $params{embeds} && $params{files} ) {
    &log_error("PARAMETER ERROR: Execute request cannot combine file and embed request in one call.");
  }
 
  # construct JSON request
  my %request;
 
  # all messages types may have these params
  if ( $params{content} ) { $request{content} = $params{content} }
 
  if ( $params{username} )   { $request{username}   = $params{username} }
  if ( $params{avatar_url} ) { $request{avatar_url} = $params{avatar_url} }
  if ( $params{tts} )        { $request{tts}        = JSON::PP::true }
  if ( $params{allowed_mentions} ) { $request{allowed_mentions} = $params{allowed_mentions} }
 
  # compose URL
  my $url = $BASE_URL . '/webhooks/' . $self->{id} . '/' . $self->{token};
  if ( $self->{wait} ) { $url .= '?wait=true' }
 
  # switch mode for request based on file upload or no
  my $response;
  if ( !$params{files} ) {
 
    # This is a regular, no-fuss JSON request
    if ( $params{embeds} ) { $request{embeds} = $params{embeds} }
 
    $response = $self->{http}->post(
      $url,
      {
        headers => { 'Content-Type' => 'application/json' },
        content => encode_json( \%request )
      }
    );
  } else {
 
    # File upload, construct a multipart/form-data message
    #  32 random chars to make a boundary
    my @chars    = ( 'A' .. 'Z', 'a' .. 'z', '0' .. '9' );
    my $boundary = '';
    for ( 0 .. 31 ) {
      $boundary .= $chars[ rand @chars ];
    }
 
    # Build request body
    my $content = '';
 
    for my $i ( 0 .. $#{ $params{files} } ) {
      my $file = $params{files}[$i];
      $content .= "\r\n--$boundary\r\n";
      $content .=
          "Content-Disposition: form-data; name=\"file$i\"; filename=\""
        . $file->{name}
        . "\"\r\n";
 
      # Discord ignores content-type, just put octet-stream for everything
      $content .= "Content-Type: application/octet-stream\r\n";
      $content .= "\r\n";
      $content .= $file->{data} . "\r\n";
    }
 
    # add the json payload for the rest of the message
    $content .= "\r\n--$boundary\r\n";
    $content .= "Content-Disposition: form-data; name=\"payload_json\";\r\n";
    $content .= "Content-Type: application/json\r\n";
    $content .= "\r\n";
    $content .= encode_json( \%request ) . "\r\n";
 
    $content .= "\r\n--$boundary--\r\n";
 
    $response = $self->{http}->post(
      $url,
      {
        headers =>
          { 'Content-Type' => "multipart/form-data; boundary=$boundary" },
        content => $content
      }
    );
  }
 
  if ( !$response->{success} ) {
    &log_error("HTTP ERROR: HTTP::Tiny->post($url) returned error\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason} . "\n"
      . "\tcontent: " . $response->{content});
  }
 
  # return details, or just true if content is empty (wait=0)
  if ( $response->{content} ) { return decode_json( $response->{content} ) }
}
 
sub execute_slack {
  my $self = shift;
 
  my $json;
  if ( @_ > 1 ) {
    my %params = @_;
    $json = encode_json( \%params );
  } else {
    $json = shift;
  }
 
  # create a slack-format post url
  my $url =
    $BASE_URL . '/webhooks/' . $self->{id} . '/' . $self->{token} . '/slack';
  if ( $self->{wait} ) { $url .= '?wait=true' }
 
  my $response = $self->{http}->post( $url,
    { headers => { 'Content-Type' => 'application/json' }, content => $json } );
  if ( !$response->{success} ) {
    &log_error("HTTP ERROR: HTTP::Tiny->post($url) returned error\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason} . "\n"
      . "\tcontent: " . $response->{content});
  }
 
  # return details, or just undef if content is empty (wait=0)
  #  Slack request usually returns the string "ok"
  if ( $response->{content} ) { return $response->{content} }
}
 
sub execute_github {
  my $self = shift;
 
  my %params = @_;
 
  # check params
  if ( !( $params{event} && $params{json} ) ) {
    &log_error(    "PARAMETER ERROR: execute_github missing required event and json parameters");
  }
 
  # create a github-format post url
  my $url =
    $BASE_URL . '/webhooks/' . $self->{id} . '/' . $self->{token} . '/github';
  if ( $self->{wait} ) { $url .= '?wait=true' }
 
  my $response = $self->{http}->post(
    $url,
    {
      headers => {
        'Content-Type'   => 'application/json',
        'X-GitHub-Event' => $params{event}
      },
      content => $params{json}
    }
  );
  if ( !$response->{success} ) {
    &log_error("HTTP ERROR: HTTP::Tiny->post($url) returned error\n"
      . "\tcode: " . $response->{status} . " " . $response->{reason} . "\n"
      . "\tcontent: " . $response->{content});
  }
 
  # return details, or just undef if content is empty (wait=0)
  #  github request usually has no response
  if ( $response->{content} ) { return $response->{content} }
}
 
1;
 
__END__
