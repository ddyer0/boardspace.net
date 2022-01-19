#!/usr/bin/perl

#   PROGRAM: env -- report CGI environment
#   PURPOSE: To debug CGI scripts
#   PROJECT: CGI application development
#    AUTHOR: Neill A. Kipp
#   REVISED: January 25, 1998

################
# Print the required header for CGI/HTML.

print <<HEADER;
Content-type: text/html

<html>
<head>
<title>ENV</title></head>
<body bgcolor="#ffffff">
<h3>ENV: Report CGI Script Environment---by Neill A. Kipp</h3>
HEADER

################
# Print the table of environment values

print "<h3>Environment</h3>\n";

print "<table border=1>\n";
foreach ( sort keys( %ENV)) {
    print "<tr><td><i>$_:</i><td>$ENV{ $_}</tr>\n";
}
print "</table>\n";

################
# Check menu buttons

# regular post data
if ( $ENV{'REQUEST_METHOD'} eq "GET" ) {
    $request = $ENV{ 'QUERY_STRING'};
}

elsif ( $ENV{ 'REQUEST_METHOD'} eq "POST") {
    while ( <STDIN>) {
	push( @lines, $_);
    }
} 

$multipart = "multipart/form-data";

# special upload data
if ( index( $ENV{ 'CONTENT_TYPE'}, $multipart) == 0) {
    ($xxx, $boundary) = split( /boundary=/, $ENV{ 'CONTENT_TYPE'});

    print "<h3>Upload Data</h3>\n";
    print "<pre>\n";
    foreach ( @lines) {
	tr/\011\012\015\040-\176/\./c;
	s/\&/\&amp;/g;
	s/\</&lt;/g;
	s+$boundary+<b>$boundary</b>+g;
	print "$_";
    }
    print "</pre>\n";
} else {
# regular query or form data
    print "<h3>Form Data</h3>\n";

    if ( @lines) {
	$request = join( "", @lines);
    }

    print "<table border=1>\n";
    @fields = split( /\&/, $request);
    foreach ( sort @fields) {
	($key, $value) = split( /=/, $_);
	$value =~ s/\+/ /g;
	$value =~ s/%(\w\w)/pack(C,hex($1))/eg;
	print "<tr><td><i>$key</i><td>$value</tr>\n";
    }
    print "</table>\n";
}

# ////////////////////////////////////////////////////////////////

print <<MINIFORM;
<hr>
<table>
<tr>
<td><b>GET:</b>
<td><a href="/nkipp-cgi/env/path?query">/nkipp-cgi/env/path?query</a>
</tr>
</table>
<form method=POST action="/nkipp-cgi/env/path?query">
<table border=1>
<tr valign=top>
<td><b>POST:</b>
<td>Checkbox<br>
<input type=checkbox name=Checkbox1 checked> checkbox1
<br>
<input type=checkbox name=Checkbox2> checkbox2
<td>Default<br>
<input name=Default size=10 maxlength=200 value="default">
<td>Hidden<br>
<input type=hidden name=Hidden value=hidden>
<td>Image1<br>
<input type=image name=Image1 alt="Image1" src="/~nkipp/go.gif" border=0>
<td>Image2<br>
<input type=image name=Image2 alt="Image2" src="/~nkipp/go.gif" border=0>
<td>Radio<br>
low
<input type=radio name=Radio value=low >
<input type=radio name=Radio value=middle checked>
<input type=radio name=Radio value=high >
high
<td>Select<br>
<select name=Select>
<option value=select1 selected>select 1
<option value=select2 >select 2
<option value=select3 >select 3
</select>
<td>Submit<br>
<input type=submit name=submitbutton1 value=" Submit1 ">
<input type=submit name=submitbutton2 value=" Submit2 ">
</tr>
</table>
</form>
MINIFORM

# ////////////////////////////////////////////////////////////////
$ENCTYPE="multipart/form-data";

print <<UPLOADFORM;
<form method=POST action="/nkipp-cgi/env/path?query" enctype="$ENCTYPE">
<table>
<tr>
<td><b>UPLOAD:</b>
<td>File:
<td><input type=file name=filename1 value="" size=30 maxlength=256 >
<td><input type=submit name=Upload value="Upload">
</tr>
</table>
</form>
UPLOADFORM
# ////////////////////////////////////////////////////////////////

print qq@<hr><a href="/~nkipp/env.txt">Perl source: env</a>\n@;
