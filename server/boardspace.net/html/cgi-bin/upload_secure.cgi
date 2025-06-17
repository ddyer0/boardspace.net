#!/usr/bin/env perl
use strict;
use warnings;
use CGI;
use MIME::Base64;
use Digest::MD5 qw(md5_hex);
use File::Path qw(make_path);
use File::Slurp qw(read_file);

# Shared secret for HMAC-style authentication
my $secret = "xyzzy";
my $upload_dir = "g:/temp/uploads/";

# Setup CGI and headers
my $cgi = CGI->new;
print $cgi->header('text/plain');

# Extract parameters
my $file     = $cgi->param('file')     or die "Missing 'file' parameter\n";
my $data     = $cgi->param('data')     or die "Missing 'data' parameter\n";
my $part     = $cgi->param('part')     || 1;
my $total    = $cgi->param('total')    || 1;
my $checksum = $cgi->param('checksum') or die "Missing 'checksum' parameter\n";
my $finalsum = $cgi->param('finalsum');  # Optional, only required on final part

# Recompute expected checksum with parameter names
my $expected = md5_hex(
    $secret .
    "file=$file" .
    "part=$part" .
    "total=$total" .
    "data=$data"
);

# Validate checksum
if ($checksum ne $expected) {
    die "Invalid checksum for chunk $part\n";
}

# Save the chunk
make_path($upload_dir) unless -d $upload_dir;
my $partfile = "$upload_dir/part$part";
open(my $fh, '>', $partfile) or die "Cannot write to $partfile: $!";
print $fh $data;
close($fh);
print "Saved part $part of $total for file '$file'\n";

# If this is the last part, verify full file
my @parts = map { "$upload_dir/part$_" } (1 .. $total);
if ($part == $total && @parts == grep { -e $_ } @parts) {
    my $outfile = "$upload_dir/$file.final";
    open(my $out, '>', $outfile) or die "Cannot write to $outfile: $!";
    binmode($out);
    foreach my $p (@parts) {
        open(my $in, '<', $p) or die "Cannot read $p: $!";
        local $/; my $chunk = <$in>;
        print $out decode_base64($chunk);
        close($in);
	unlink($in);
    }
    close($out);

    if ($finalsum) {
	# Read the file and compute its final checksum
	open(my $fh, '<', $outfile) or die "Cannot open $file: $!";
	binmode($fh);
	my $raw = do { local $/; <$fh> };
	close($fh);
	my $digest = md5_hex($raw);

        if ($digest eq $finalsum) {
            print "✅ Final file checksum verified successfully.\n";
        } else {
            #unlink $outfile;
           die "Final file checksum mismatch. $outfile preserved ($digest != $finalsum ) deleted.\n";
        }
    } else {
        print "⚠️ No final checksum provided, file assembled but not verified.\n";
    }
}
