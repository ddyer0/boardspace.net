#!/usr/bin/perl
# 
# from https://github.com/inutt/pushbullet-perl/blob/master/Net/PushBullet.pm
#
package Net::PushBullet;

#require "tlib/sense.pl";

use LWP;
use JSON;
use Encode qw/encode decode/;
use URI::Escape;
use Carp;
use MIME::Base64;
require "push/magic.pl";
use File::Basename;
require "push/slurp.pl";

sub new
{
	my $class = shift;
	my %options = @_; # api_key => <pushbullet API key>
	                  # key_file => <path to file containing api key>
					  #     - only one of api_key or key_file is required
	                  # device_id => <iden parameter of device to push to by default> (can be undef to push to all your devices)

	my $api_key;

	if ($options{'api_key'})
	{
		$api_key = $options{'api_key'};
	}
	elsif ($options{'key_file'})
	{
		$api_key = read_file($options{'key_file'}) || croak "Couldn't read API key from ".$options{'key_file'};
		chomp $api_key;
	}
	else
	{
		croak "Must provide an API key";
	};
	
	$api_key .= ":"; # To match proper HTTP basic auth for username but no password, as per pushbullet API change on 2013-09-27

	my $this = {
		'ua' => LWP::UserAgent->new(
			timeout => 20,
			protocols_allowed => ['https'],
		),
		'api_base_url' => "https://api.pushbullet.com/v2/",
	};

	bless $this,$class;
	$this->_ua->default_header("Authorization" => "Basic ".encode_base64($api_key));
	return $this;
};

sub _ua { my $this = shift; return $this->{'ua'}; };

sub _parse_response_code
{
	my $this = shift;
	my $response = shift;

	my $code = $response->code;
	my $message = $response->message;

	# Parse response codes as per https://www.pushbullet.com/api

	if ($code == 200) { return "Request succeeded" }; # Everything worked as expected
	if ($code == 400) { return "API request parameters incorrect" };
	if ($code == 401) { return "No valid API key provided" };
	if ($code == 402) { return "Parameters were valid but the request failed" };
	if ($code == 403) { return "API key is not valid for that request" };
	if ($code == 404) { return "Specified device/push ID or requested API url not found" };
	if ($code >= 500) { return "Server error, try again later ($code - $message)" };
	return "API returned unknown response code $code ($message)" ;

};

###
### Devices endpoint
###

sub get_devices
{
	my $this = shift;

	my $response = $this->_ua->get($this->{'api_base_url'}."devices");
	if ($response->is_success) { return decode_json($response->decoded_content)->{'devices'}; }
	else                       { croak($this->_parse_response_code($response));         };
};

sub delete_device
{
	my $this = shift;
	my $id = shift;

	my $response = $this->_ua->delete($this->{'api_base_url'}."devices/".$id);
	if ($response->is_success) { return decode_json($response->decoded_content);      }
	else                       { croak($this->_parse_response_code($response)); };
};

###
### Pushes endpoint
###

sub _send_push
{
	my $this = shift;
	my %data = @_;

	my $response = $this->_ua->post(
		$this->{'api_base_url'}."pushes",
		"Content-Type"=>"application/json",
		Content=>encode_json(\%data),
	);

	if ($response->is_success) { return decode_json($response->decoded_content);      }
	else                       { croak($this->_parse_response_code($response)); };
};

sub push_note
{
	my $this = shift;

	my ($title, $content) = @_;
	my %data = (
		device_iden => $this->{'device_id'},
		type => "note",
		title=>$title,
		body=>$content,
	);

	return $this->_send_push(%data);
};

sub push_link
{
	my $this = shift;
	my ($title, $url, $message) = @_; # message is optional
	my %data = (
		device_iden => $this->{'device_id'},
		type => "link",
		title => $title,
		url => $url,
		body => $message,
	);
	return $this->_send_push(%data);
};

sub push_address
{
	my $this = shift;
	my ($name, $address) = @_;
	my %data = (
		device_iden => $this->{'device_id'},
		type => "address",
		name => $name,
		address => $address,
	);
	return $this->_send_push(%data);
};

sub push_list
{
	my $this = shift;
	my ($title, @items) = @_;
	my %data = (
		device_iden => $this->{'device_id'},
		type => "list",
		title => $title,
		items => \@items,
	);
	return $this->_send_push(%data);
};

sub push_file
{
	my $this = shift;
	my ($filename, $message) = @_; # message is optional

	my $upload_request = $this->_upload_file($filename);
	my %data = (
		device_iden => $this->{'device_id'},
		type => "file",
		file_name => $upload_request->{'file_name'},
		file_type => $upload_request->{'file_type'},
		file_url => $upload_request->{'file_url'},
		body => $message,
	);
	return $this->_send_push(%data);
};

sub get_pushes
{
	my $this = shift;
	my $after_timestamp = shift // time()-86400; # default to last 24 hours onlu to prevent excessive server load

	my $response = $this->_ua->get($this->{'api_base_url'}."pushes?modified_after=".$after_timestamp); # Cursor seems unnecessary at the moment, and causes incorrect parameters error
	if ($response->is_success) { return @{decode_json($response->decoded_content)->{'pushes'}}; }
	else                       { croak($this->_parse_response_code($response));            };
};

sub delete_push
{
	my $this = shift;
	my $id = shift;

	my $response = $this->_ua->delete($this->{'api_base_url'}."pushes/".$id);
	if ($response->is_success) { return decode_json($response->decoded_content);      }
	else                       { croak($this->_parse_response_code($response)); };
};

###
### Contacts endpoint
###

sub get_contacts
{
	my $this = shift;

	my $response = $this->_ua->get($this->{'api_base_url'}."contacts");
	if ($response->is_success) { return decode_json($response->decoded_content)->{'contacts'}; }
	else                       { croak($this->_parse_response_code($response));         };
};

sub delete_contact
{
	my $this = shift;
	my $id = shift;

	my $response = $this->_ua->delete($this->{'api_base_url'}."contacts/".$id);
	if ($response->is_success) { return decode_json($response->decoded_content);      }
	else                       { croak($this->_parse_response_code($response)); };
};

###
### User endpoint
###

sub get_user
{
	my $this = shift;

	my $response = $this->_ua->get($this->{'api_base_url'}."users/me");
	if ($response->is_success) { return decode_json($response->decoded_content); }
	else                       { croak($this->_parse_response_code($response));         };
};

###
### File upload endpoint (used for file pushes)
###

sub _upload_file
{
	my $this = shift;
	my $filename = shift;

	croak("No filename provided") if ! $filename;
	croak("Can't read $filename") if ! -r $filename;
	my $mimetype = mimetype($filename);

	my $response = $this->_ua->get($this->{'api_base_url'}."upload-request?file_name=".uri_escape(basename($filename))."&file_type=".uri_escape($mimetype));
	if (!$response->is_success) { croak($this->_parse_response_code($response)); };
	my $upload_request = decode_json($response->decoded_content);

	my $upload_ua = LWP::UserAgent->new(timeout => 8, protocols_allowed => ['https']); # Need a UA without the authorization header for upload as it's not going to amazon servers not pushbullet ones

	my $response = $upload_ua->post(
		$upload_request->{'upload_url'},
		"Content-Type" => "form-data",
		Content=>[
			%{$upload_request->{'data'}},
			file => [ $filename ],
		],
	);

	if ($response->is_success) { return $upload_request; }
	else                       { croak("Got response code ".$response->code." from file upload attempt to ".$upload_request->{'upload_url'}); };
};

1;
